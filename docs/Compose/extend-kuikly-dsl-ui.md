# 扩展 Kuikly DSL UI 组件

本页说明在「Kuikly DSL UI 组件已经存在」的前提下，如何通过 `MakeKuiklyComposeNode` 将这些组件包装为 Compose 侧可直接使用的 Composable。

## API 函数：MakeKuiklyComposeNode

**`MakeKuiklyComposeNode` 是将 Kuikly 原生视图嵌入 Compose 渲染树的统一桥接入口。** 根据组件是否需要挂载 Compose 子节点，提供两个重载：

### 无子节点（原子组件）

用于自身完整渲染、不接受外部子视图的组件：

```kotlin
@Composable
@UiComposable
fun <T : DeclarativeBaseView<*, *>> MakeKuiklyComposeNode(
    factory: () -> T,
    modifier: Modifier,
    viewInit: T.() -> Unit = {},
    viewUpdate: (T) -> Unit = {},
    measurePolicy: MeasurePolicy = KuiklyDefaultMeasurePolicy
)
```

### 有子节点（容器组件）

用于需要将 Compose 子节点挂载到当前视图下的组件：

```kotlin
@Composable
@UiComposable
fun <T : DeclarativeBaseView<*, *>> MakeKuiklyComposeNode(
    factory: () -> T,
    modifier: Modifier,
    content: @Composable () -> Unit,
    viewInit: T.() -> Unit = {},
    viewUpdate: (T) -> Unit = {},
    measurePolicy: MeasurePolicy = DefaultColumnMeasurePolicy
)
```

## 参数解析

- **`factory: () -> T`**：创建 Kuikly 视图实例的工厂函数。
- **`modifier: Modifier`**：Compose 侧传入的 `Modifier`，内部会同步到 Kuikly 节点，用于尺寸、布局、点击等。
- **`content: @Composable () -> Unit`（仅容器重载）**：子内容插槽，Compose 子节点会被挂载到当前 Kuikly 容器节点下。
- **`viewInit: T.() -> Unit`**：视图创建时调用一次，用于一次性初始化，如设置背景、注册事件、构建内部子视图等。
- **`viewUpdate: (T) -> Unit`**：每次重组时调用，用于根据最新的 Compose 参数更新 Kuikly 视图属性。
- **`measurePolicy: MeasurePolicy`**：Compose 布局测量策略：
  - 无子节点默认为 `KuiklyDefaultMeasurePolicy`（占满父约束大小）
  - 有子节点默认为 `DefaultColumnMeasurePolicy`，可按需自定义。

## 两种使用场景

### 场景一：原子组件

**适用**：组件自身完整渲染（如视频、图片、地图等），**不接受外部子视图**，Kotlin 侧只负责桥接属性和事件到原生层。

Kuikly DSL 侧继承 `DeclarativeBaseView`，实现 `viewName()` 返回原生 view 类型名，属性/事件通过 `getViewAttr()` / `getViewEvent()` 桥接：

```kotlin
// Kuikly DSL 侧（VideoView 为例，原生层负责实际渲染）
// core/src/commonMain/kotlin/com/tencent/kuikly/core/views/VideoView.kt

// Compose 侧包装
@Composable
fun Video(
    src: String,
    playControl: VideoPlayControl,
    playTimeDidChanged: (curTime: Int, totalTime: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    MakeKuiklyComposeNode<VideoView>(
        factory = { VideoView() },
        modifier = modifier,
        viewInit = {
            getViewAttr().run {
                playControl(VideoPlayControl.PLAY)
                src(src)
            }
        },
        viewUpdate = {
            it.getViewAttr().run {
                src(src)
                playControl(playControl)
            }
            it.getViewEvent().run {
                playTimeDidChanged(handlerFn = playTimeDidChanged)
            }
        }
    )
}

// 调用
Video(
    src = "https://example.com/video.mp4",
    playControl = VideoPlayControl.PLAY,
    playTimeDidChanged = { cur, total -> },
    modifier = Modifier.size(320.dp, 180.dp)
)
```

### 场景二：容器组件

**适用**：组件作为布局容器，**可挂载任意 Compose 子节点**（通过 `content` 传入），自身负责容器样式（背景、圆角、点击等）。

Kuikly DSL 侧继承 `ViewContainer`，实现 `createAttr()`、`createEvent()`、`viewName()`，在 `viewInit` 里只设置自身样式，子视图交给 `content`：

```kotlin
// Kuikly DSL 侧
class KuiklyCard : ViewContainer<ContainerAttr, Event>() {
    override fun createAttr() = ContainerAttr()
    override fun createEvent() = Event()
    override fun viewName() = ViewConst.TYPE_VIEW

    fun setup(onClick: (() -> Unit)? = null) {
        attr {
            backgroundColor(0xFFE3F2FDL)
            borderRadius(12f)
        }
        onClick?.let {
            event { click { onClick() } }
        }
    }
}

// Compose 侧包装（使用容器重载，传入 content）
@Composable
fun KuiklyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    MakeKuiklyComposeNode<KuiklyCard>(
        factory = { KuiklyCard() },
        modifier = modifier,
        viewInit = { setup(onClick) },
        viewUpdate = { it.setup(onClick) },
        content = content,
    )
}

// 调用
KuiklyCard(
    modifier = Modifier.fillMaxWidth().height(120.dp),
    onClick = { /* handle click */ }
) {
    Text("卡片标题")
    Text("卡片描述")
}
```

## 注意事项

`factory` 中传入的视图类型需根据场景选择正确基类：

- **原子组件**：继承 `DeclarativeBaseView`，`viewName()` 返回原生 view 类型名，原生层负责渲染
- **容器组件**：继承 `ViewContainer`，可挂载 Compose 子节点，`isRenderView()` 始终返回 `true`

**不能**将 `ComposeView` 子类传入 `factory`——`ComposeView` 的扁平化优化会导致 Compose 计算出的布局 frame 无法传递给实际渲染层，视图将不可见。

```kotlin
// ❌ 错误：ComposeView 子类，编译通过但运行时不可见
class MyView : ComposeView<ComposeAttr, ComposeEvent>() { ... }
MakeKuiklyComposeNode<MyView>(factory = { MyView() }, ...)

// ✅ 正确（原子组件）：DeclarativeBaseView 子类
class MyView : DeclarativeBaseView<MyAttr, MyEvent>() {
    override fun viewName() = "MyNativeView"
}
MakeKuiklyComposeNode<MyView>(factory = { MyView() }, ...)

// ✅ 正确（容器组件）：ViewContainer 子类
class MyView : ViewContainer<ContainerAttr, Event>() {
    override fun createAttr() = ContainerAttr()
    override fun createEvent() = Event()
    override fun viewName() = ViewConst.TYPE_VIEW
}
MakeKuiklyComposeNode<MyView>(factory = { MyView() }, content = { ... }, ...)
```

## Demo 示例

- 原子组件示例（VideoView）：[`VideoView.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/VideoView.kt)
