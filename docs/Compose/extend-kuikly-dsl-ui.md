# 扩展 Kuikly DSL UI 组件

本页说明在「Kuikly DSL UI 组件已经存在」的前提下，如何通过 `MakeKuiklyComposeNode` 将这些组件包装为 Compose 侧可直接使用的 Composable。

## API 函数：MakeKuiklyComposeNode

`MakeKuiklyComposeNode` 是 Kuikly Compose 用来把 **Kuikly 视图（`DeclarativeBaseView`）挂到 Compose 渲染树** 的统一入口。

### 叶子节点重载（无子内容）

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

### 容器重载（有子内容）

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

- **`factory: () -> T`**：创建具体 Kuikly 视图实例的工厂函数，例如 `VideoView()`、`VirtualNodeView()`。  
- **`modifier: Modifier`**：Compose 侧传入的 `Modifier`，内部会同步到 Kuikly 节点，用于尺寸、布局、点击等。  
- **`content: @Composable () -> Unit`（仅容器重载）**：子内容插槽，Compose 子节点会被挂载到当前 Kuikly 容器节点下。  
- **`viewInit: T.() -> Unit`**：视图创建时调用一次，用于做一次性初始化，如注册监听、设置默认属性等。  
- **`viewUpdate: (T) -> Unit`**：每次重组时调用，用于根据最新的 Compose 参数更新 Kuikly 视图属性。  
- **`measurePolicy: MeasurePolicy`**：Compose 布局测量策略：
  - 叶子节点默认为 `KuiklyDefaultMeasurePolicy`（占满父约束大小）  
  - 容器重载默认为 `DefaultColumnMeasurePolicy`，可以按需要自定义布局测量。  

## 使用示例：叶子节点组件（Video）

- 封装 Kuikly DSL 为 Composable：[`VideoView.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/VideoView.kt)  
- Compose Demo 页面：[`ComposeVideoDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeVideoDemo.kt)

典型封装模式（简化示意）：

```kotlin
@Composable
fun VideoView(
    url: String,
    modifier: Modifier = Modifier,
) {
    MakeKuiklyComposeNode<VideoView>(
        factory = { VideoView() },
        modifier = modifier,
        viewInit = {
            // 一次性初始化，例如绑定控制器
        },
        viewUpdate = { view ->
            // 根据最新的 url 等参数更新底层 Kuikly Video 视图
        }
    )
}
```

调用方可以像使用普通 Compose 组件一样使用：

```kotlin
VideoView(
    url = "https://example.com/video.mp4",
    modifier = Modifier.fillMaxWidth()
)
```

## 使用示例：容器组件（如通用卡片/布局容器）

容器组件需要同时处理「自身属性」和「子内容插槽」，典型封装模式：

```kotlin
@Composable
fun DslCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    MakeKuiklyComposeNode<VirtualNodeView>(
        factory = { VirtualNodeView() },
        modifier = modifier,
        content = content,
        viewInit = {
            // 初始化容器属性，例如点击事件、背景、圆角等
        },
        viewUpdate = { view ->
            // 根据最新参数更新容器
        }
    )
}
```

业务侧使用示例：

```kotlin
DslCard {
    Text("标题")
    Text("描述文案")
    VideoView(url = ...)
}
```

完整的容器封装与使用示例，可参考 Demo 中的 DSL 容器组件实践（后续会补充到本页，持续更新中）。




