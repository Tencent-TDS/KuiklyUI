# Native 属性动画

本文说明 Kuikly Compose Native 属性动画的适用范围、API 使用方式、回退规则和当前限制。

Native 属性动画是现有 Compose 动画系统的一条可选执行路径。它不会替换原有的 Compose
逐帧插值实现，也不会自动改变已有业务的动画行为。只有动画规格显式调用
`preferNative()`，并且整个动画组都满足能力要求时，动画才会交给 Native View 执行。

## 为什么需要 Native 属性动画

Kuikly Compose 的重组、布局和动画插值通常运行在 Kuikly 线程。当页面正在进行复杂重组
或计算时，Compose 动画的插值也可能被延迟，表现为弹窗、内容切换等动画出现掉帧或停顿。

Native 属性动画会在 Kuikly 侧完成一次目标状态计算和属性提交，然后由 Android、iOS 或
HarmonyOS 的原生 View 动画系统独立完成后续插值。即使 Kuikly 线程暂时繁忙，已经提交的
Native 动画仍能继续播放。

当前实现接入 alpha、transform 和纯色背景。更本质的判断标准不是“是否属于布局 API”，
而是起止状态能否一次计算完成，后续是否可以由 Native View 独立插值。需要 Kuikly 每一帧
重新重组、测量、计算兄弟布局或执行自定义绘制的动画仍由 Compose 执行。

## 使用前先理解运行时语义

Native 动画与 Compose 逐帧动画最重要的差异是：**Compose 保存的是目标状态，屏幕上正在
显示的中间状态由 Native View 保存。**

动画提交后：

- `value` / `targetValue` 在 Compose 逻辑层成为终值。
- View 在屏幕上当前可见的 alpha、transform 等中间值由 Native 平台继续插值。
- `isRunning`、Transition 的 `currentState` / `isIdle`、协程完成等状态仍等待 Native
  完成回调。
- `animate*AsState` 的 `finishedListener` 在最终动画正常完成时调用。
- 与原 Compose 语义一致，被新目标取消的旧 `animate*AsState` 动画不会调用旧的
  `finishedListener`。

因此，业务不能在 Native 动画播放期间读取 `value` 来获取屏幕上正在显示的中间进度。
Native 路径不向 Compose 提供逐帧屏幕值或速度。如果业务需要逐帧回调、根据中间值
触发逻辑，或者要求测试时钟逐帧推进，应继续使用 Compose 动画。

### 动画中断

同一个 View、同一个属性上的新动画会替换旧动画：

- 新动画尽量从 Native View 当前显示状态继续，避免先跳到逻辑终值再重新开始。
- 旧动画协程按取消语义结束。
- 同一 View 上的新 Native 动画会替换旧 Native 动画；首期不承诺不同 descriptor 的属性级并行。
- 延迟中的 Snap 会被同属性的新动画替换，不会在旧 deadline 再写入旧目标。

`AnimatedVisibility` 退出期间会保留节点直到 Native 完成回调。快速
show/hide/show 会从当前显示状态反向继续。

### stop、取消和生命周期

调用 `stop()`、协程取消、View 移除或 Scene 销毁时，会清理对应 Native animator 和
Coordinator 记录。逻辑状态以最后一次已经提交的目标状态为准，避免 Compose 状态与 Native
View 最终状态长期分离。

## 基本原则

### 显式启用

Native 动画是实验性、显式 opt-in 能力：

```kotlin
fun <T> AnimationSpec<T>.preferNative(): AnimationSpec<T>

fun <T> FiniteAnimationSpec<T>.preferNative(): FiniteAnimationSpec<T>
```

未调用 `preferNative()` 的动画始终走原有 Compose 路径。

`preferNative()` 不改变原始 `AnimationSpec` 的插值逻辑。包装对象保留原始 spec；当 Native
能力检查失败时，会使用原始 spec 完整回退 Compose。

### 整组使用 Native 或整组使用 Compose

同一个逻辑动画组不能同时使用 Compose 时钟和 Native 时钟。例如一个
`AnimatedVisibility` 同时包含 fade、scale 和 slide 时，三个效果都必须满足：

- 每个活跃动画的 spec 都调用了 `preferNative()`。
- 每个 spec 类型都受支持。
- 所有活跃属性必须使用相同的动画 descriptor（曲线、duration 和 delay 一致）。
- 每个动画值最终只驱动受支持的 Native 视觉属性。
- 动画组中没有当前 Native 路径暂不支持的参与者。

任意一个条件不满足，整个动画组都会回退 Compose，避免不同属性使用不同时间轴而出现
撕裂、错位或完成状态不一致。

### 默认行为不变

Native 动画不会自动迁移现有业务：

```kotlin
// 仍然使用 Compose 插帧。
tween<Float>(durationMillis = 300)

// 优先使用 Native；能力检查失败时回退 Compose。
tween<Float>(durationMillis = 300).preferNative()
```

因此，Core、Compose 和三端 Render 升级后，未修改的业务动画行为保持不变。

## 平台范围

当前 Native 属性动画链路覆盖：

- Android
- iOS
- HarmonyOS

Compose/Core 和对应平台的 Native Render 需要同步升级。当前版本没有单独的
capability/version 握手，不建议新 Compose/Core 搭配不包含 Native 动画协议的旧 Render。

## 支持状态速览

状态含义：

- **支持**：已接入并经过 Native 路径验证。
- **条件支持**：当前版本可以使用 Native，但必须满足属性、spec 或生命周期条件。
- **未接入**：基础属性动画能力可能存在，但高层组件当前没有启用 Native。
- **当前未支持**：当前版本会回退 Compose，但不代表技术上无法扩展。
- **不适用**：依赖逐帧业务逻辑或 Compose 时钟，原则上应继续使用 Compose。

### 动画 API

| API | 状态 | 条件或限制 |
| --- | --- | --- |
| `animate*AsState` | 条件支持 | 动画值必须只驱动 alpha、transform 或纯色背景 |
| `Animatable.animateTo` | 条件支持 | 不能有逐帧 block、显式 bounds 或非零初速度 |
| `updateTransition` / `Transition.animate*` | 条件支持 | 所有活跃子动画必须全部 opt-in 且可由 Native 执行 |
| `AnimatedVisibility` | 支持 | 支持 fade、scale、slide；expand/shrink 当前未支持 |
| `Crossfade` | 支持 | Native 执行内容节点的 alpha 动画 |
| `AnimatedContent` | 当前未支持 | 默认涉及双内容、尺寸动画和复杂退出生命周期 |
| `animateContentSize` | 当前未支持 | 属于测量和布局动画 |
| `rememberInfiniteTransition` | 当前未支持 | 当前协议未支持无限循环 |
| `animateDecay` / fling | 不适用 | 依赖手势速度和逐帧位置 |
| Transition seek/scrub | 不适用 | 需要外部时钟逐帧控制 |
| 测试时钟手动推进 | 不适用 | Native 平台时钟不受 Compose 测试时钟控制 |

### 高层组件和常用场景

下面的 `pageName` 可以直接在 Demo 中打开。`NativeAnimationDemo` 后面的编号是页面内的
具体用例；标为“未接入”或“当前未支持”的页面用于观察现有 Compose 行为，不会执行 Native
动画。

| 组件或场景 | 当前状态 | Demo pageName | 说明 |
| --- | --- | --- | --- |
| `Dialog` 容器 | 未接入 | `DialogDemo` | Dialog 本身没有出现/退出动画 API |
| Dialog 内容 fade + scale | 条件支持 | `NativeAnimationDemo`（用例 12） | 使用 opt-in 的 `AnimatedVisibility`，并保留退出节点 |
| `Popup` 容器 | 未接入 | `NativeAnimationDemo`（用例 13） | Popup 只负责覆盖层和静态锚点位置 |
| Popup 内容 fade + scale | 条件支持 | `NativeAnimationDemo`（用例 13） | 使用 opt-in 的 `AnimatedVisibility` |
| `ModalBottomSheet` | 支持 | `BottomSheetDemo1` | show/hide 位移默认 Native，三种样式均可切换 Compose / Native；拖拽跟手仍由 Compose 驱动 |
| `SnackbarHost` | 未接入 | `ScaffoldDemo` | 当前页面展示 Compose 行为；内部 alpha + scale 适合后续增加 Native 开关 |
| `Crossfade` 内容切换 | 支持 | `NativeAnimationDemo`（用例 5） | Compose / Native 对照，Native spec 调用 `preferNative()` |
| `AnimatedContent` | 当前未支持 | `AnimatedContentDemo` | 默认包含尺寸和多内容生命周期管理 |
| `NavHost` 页面转场 | 条件支持 | `NativeNavHostDemo` | 固定容器的纯 slide 或 slide + fade 默认自动使用 Native；不支持手势返回 |

### Native 属性

| 最终渲染属性 | 状态 | 典型 Compose 写法 |
| --- | --- | --- |
| alpha | 支持 | `Modifier.graphicsLayer { alpha = value }` |
| translationX/Y | 支持 | `Modifier.graphicsLayer { translationX = value }` |
| scaleX/Y | 支持 | `Modifier.graphicsLayer { scaleX = value }` |
| rotationX/Y/Z | 支持 | `Modifier.graphicsLayer { rotationZ = value }` |
| transformOrigin | 支持 | `Modifier.graphicsLayer { transformOrigin = value }` |
| 纯色背景 | 支持 | `Modifier.background(animatedColor)` |
| `Modifier.offset` 位置 | 当前不支持 | 即使 spec 调用 `preferNative()`，也会整体回退 Compose |
| 数值圆角 | 当前未支持 | 使用 `preferNative()` 时能力检查失败，整组回退 Compose |
| 结构化阴影/高程 | 当前未支持，可扩展 | 需要三端补充 shadow 参数插值 handler |
| 矩形/圆角矩形 clip bounds | 当前未支持，可扩展 | 可插值 rect/radius；布尔 clip 本身没有连续动画 |
| 文字颜色 | 当前未支持 | 当前只识别 View 的纯色背景属性 |
| width/height/size | 当前未支持 | 会改变测量结果和可能影响兄弟布局 |
| padding | 当前未支持 | 会改变测量和子节点布局 |
| 通用 frame/测量结果 | 当前未支持 | 需要收集受影响节点和兄弟布局结果 |
| 渐变/Brush | 当前未支持 | 需要结构化渐变协议和三端插值 |
| Canvas/自定义绘制 | 不适用 | 绘制逻辑只存在于 Compose 侧 |
| 任意 clip path morph | 当前未支持 | 只有兼容路径拓扑才可能安全插值 |

需要特别注意：是否支持取决于动画值最终被用在哪里，而不是取决于动画值的 Kotlin 类型。

例如，`animateDpAsState()` 本身可以进入 Native 候选流程，但如果结果被用于 `width()`，
就会整组回退 Compose；`animateFloatAsState()` 的结果用于 `graphicsLayer.alpha` 时则可以
由 Native 执行。

这里的“当前不支持”描述的是本版本能力，不表示这些属性永远不能由 Native 动画。offset、
结构化阴影和规则 clip bounds 可以作为后续扩展方向，详见
[可扩展属性的边界](#可扩展属性的边界)。

### AnimationSpec

| AnimationSpec | 状态 | 说明 |
| --- | --- | --- |
| `TweenSpec` | 支持 | 支持 duration 和 delay |
| `LinearEasing` | 支持 | 映射为线性 cubic |
| `CubicBezierEasing` | 支持 | 下发四个贝塞尔控制点 |
| Compose 内置 cubic easing | 支持 | 例如 `FastOutSlowInEasing` |
| `SpringSpec` | 当前未支持 | 首期继续使用 Compose 插帧 |
| `SnapSpec` | 支持 | 支持 delay |
| 自定义 lambda `Easing` | 当前未支持 | 无法可靠序列化到 Native |
| keyframes | 当前未支持 | 回退 Compose |
| keyframes with spline/arc | 当前未支持 | 回退 Compose |
| repeat/reverse | 当前未支持 | 回退 Compose |
| infinite repeat | 当前未支持 | 回退 Compose |
| decay/fling | 不适用 | 依赖手势速度和逐帧位置 |

Spring 的停止阈值和中断速度在三端并不完全一致。首期不改造现有 Render 的 spring
实现，因此即使调用 `preferNative()` 也会整体回退到 Compose 插帧。

## 可扩展属性的边界

当前 Coordinator 正式接收的属性包括 `opacity`、`transform` 和 `backgroundColor`。这是
当前实现范围，不是 Native View 动画能力的上限。

### Modifier.offset（当前不支持）

`Modifier.offset` 会改变子节点的 placement，但不会改变自身测量尺寸，也不会要求兄弟节点
重新排列。不过当前版本没有为 offset placement 建立属性归属，也不会把 frame 更新提交为
Native 动画。因此，无论 `animateOffsetAsState`、`animateIntOffsetAsState`、
`Transition.animateOffset` 还是 `Transition.animateIntOffset`，只要动画值最终用于
`Modifier.offset`，都会使用原有 Compose 插帧链路。

后续若要支持，不会根据“某个 frame 恰好变化了”来猜测它是 offset 动画。更可靠的方式是在
`Modifier.offset` 生成 placement 时登记属性归属：只有 x/y 确实由当前
`preferNative()` 动画端点驱动，且 width/height 不变，Coordinator 才接管这次位置更新。
普通布局、滚动或父节点变化造成的位置变化不会因此被误判成 Native offset 动画。

可行方案有两种：

1. 将起止位置转换为 Native transform translation。
2. 使用三端 Render 已有的 frame 动画能力，但只允许 x/y 变化，width/height 必须相同。

第二种更贴合 `Modifier.offset` 的最终 frame，但需要补充以下语义：

- 中断时从 Native View 当前显示位置继续。
- 动画期间 Compose 的逻辑坐标已是终点，需要明确点击命中和无障碍坐标如何与屏幕位置对齐。
- 只允许位置变化；一旦 width/height 或兄弟布局同时变化，整组回退 Compose。

上述内容仅记录后续可能的实现方向，不代表当前版本已经支持。

### 圆角

圆角动画当前不进入 Native 路径。即使数值动画 spec 调用了 `preferNative()`，只要该值最终
驱动 `RoundedCornerShape`、clip 或 `borderRadius`，能力检查就会失败，整个逻辑动画组使用
原有 Compose 逐帧动画。

原因是三端现有圆角实现不具备一致、可靠的原生插值语义，特别是 mask/clip 的创建、移除、
组合裁剪和动画中断。后续如果重新接入，需要先统一这些 Render 行为，再开放能力，不能仅把
圆角属性加入 Native 支持列表。

### 阴影

结构化阴影也可以扩展为 Native 动画，但比圆角复杂。建议协议明确携带：

- offsetX / offsetY
- blur 或 shadow radius
- spread（平台支持时）
- shadow color
- elevation（使用平台 elevation 模型时）

三端当前都有静态阴影属性，但 Android、iOS、HarmonyOS 还需要各自补充动画 handler，并
验证圆角、clip、wrapper view 与阴影同时存在时的行为。首期可只支持一组阴影参数，不承诺
多重阴影数组和平台完全一致的模糊效果。

### clip bounds

clip 需要按数据类型区分：

- `clipToBounds = true/false` 是布尔开关，本身没有连续插值过程。
- 矩形 clip bounds 可以插值 left/top/right/bottom。
- 圆角矩形 clip 可以与 corner radius 一起插值。
- 任意 Path 只有在起止路径拓扑兼容时才可能插值，不能作为通用跨平台能力。

`expandIn` / `shrinkOut` 也不能简单等同于 clip bounds 动画。它们除了 clip，还可能改变
容器尺寸、对齐偏移和兄弟布局，因此仍属于后续布局动画能力。

## API 使用方式

以下示例均需要：

```kotlin
import com.tencent.kuikly.compose.animation.core.preferNative
```

### animateFloatAsState：透明度

```kotlin
@Composable
fun NativeAlphaExample(visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween<Float>(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ).preferNative(),
        label = "cardAlpha"
    )

    Box(
        Modifier
            .size(120.dp)
            .graphicsLayer {
                this.alpha = alpha
            }
            .background(Color.Blue)
    )
}
```

这里的 Float 最终写入 `graphicsLayer.alpha`，因此动画可以由 Native View 执行。

下面的写法虽然同样使用 `animateDpAsState`，但会回退 Compose：

```kotlin
val width by animateDpAsState(
    targetValue = if (expanded) 240.dp else 120.dp,
    animationSpec = tween<Dp>(300).preferNative(),
    label = "width"
)

// width 会改变测量和布局，因此不能由 Native View 独立插值。
Box(Modifier.width(width))
```

### animateColorAsState：纯色背景

```kotlin
@Composable
fun NativeBackgroundColorExample(active: Boolean) {
    val color by animateColorAsState(
        targetValue = if (active) Color(0xFF4CAF50) else Color(0xFF1565C0),
        animationSpec = tween<Color>(400).preferNative(),
        label = "backgroundColor"
    )

    Box(
        Modifier
            .size(120.dp)
            .background(color)
    )
}
```

当前只支持可映射为 sRGB 整色值的背景色。相同的 `color` 如果用于 `Text(color = color)`，
会回退 Compose；用于渐变 Brush 也会回退。

### Animatable.animateTo：位移

```kotlin
@Composable
fun NativeAnimatableExample(move: Boolean) {
    val translation = remember { Animatable(0f) }

    LaunchedEffect(move) {
        translation.animateTo(
            targetValue = if (move) 160f else 0f,
            animationSpec = tween<Float>(350).preferNative()
        )
    }

    Box(
        Modifier
            .size(80.dp)
            .graphicsLayer {
                translationX = translation.value
            }
            .background(Color.Green)
    )
}
```

不能为 `animateTo` 传入逐帧 block：

```kotlin
translation.animateTo(
    targetValue = 160f,
    animationSpec = tween<Float>(350).preferNative()
) {
    // 存在逐帧 block 时必须回退 Compose。
    reportProgress(value)
}
```

同样，调用过 `updateBounds()`、显式传入非零 `initialVelocity`，或者使用 `animateDecay()`
时不会进入 Native。

### updateTransition：组合属性

```kotlin
private enum class CardState { Hidden, Visible }

@Composable
fun NativeTransitionExample(state: CardState) {
    val transition = updateTransition(state, label = "card")
    val spec = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    ).preferNative()

    val alpha by transition.animateFloat(
        transitionSpec = { spec },
        label = "cardAlpha"
    ) {
        if (it == CardState.Visible) 1f else 0f
    }
    val scale by transition.animateFloat(
        transitionSpec = { spec },
        label = "cardScale"
    ) {
        if (it == CardState.Visible) 1f else 0.85f
    }
    val translationY by transition.animateFloat(
        transitionSpec = { spec },
        label = "cardTranslationY"
    ) {
        if (it == CardState.Visible) 0f else 40f
    }

    Box(
        Modifier
            .size(120.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                this.translationY = translationY
            }
            .background(Color.Blue)
    )
}
```

`updateTransition` 采用整组能力检查。如果再增加一个使用未包装 spec 的活跃子动画，或者
增加 width、文字颜色等当前 Native 路径未支持的属性，整组会回退 Compose。

### AnimatedVisibility：fade、scale 和 slide

```kotlin
@Composable
fun NativeAnimatedVisibilityExample(visible: Boolean) {
    val enterVisualSpec =
        tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing).preferNative()
    val exitVisualSpec =
        tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing).preferNative()
    val enterSlideSpec =
        tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing).preferNative()
    val exitSlideSpec =
        tween<IntOffset>(durationMillis = 220, easing = FastOutSlowInEasing).preferNative()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(enterVisualSpec) +
            scaleIn(enterVisualSpec, initialScale = 0.85f) +
            slideInVertically(enterSlideSpec) { fullHeight -> fullHeight / 3 },
        exit = fadeOut(exitVisualSpec) +
            scaleOut(exitVisualSpec, targetScale = 0.85f) +
            slideOutVertically(exitSlideSpec) { fullHeight -> fullHeight / 3 }
    ) {
        Box(
            Modifier
                .size(160.dp)
                .background(Color.Green)
        )
    }
}
```

Native slide 不会动画 View 的 frame。Compose 会计算一次最终尺寸和 slide 起止偏移，然后
将 slide 转换为 Native graphics transform translation。

以下组合会整体回退：

```kotlin
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween<Float>(300).preferNative()) +
        expandVertically(), // 当前 Native 路径未支持布局尺寸动画
    exit = fadeOut(tween<Float>(300).preferNative()) +
        shrinkVertically()
) {
    content()
}
```

`Modifier.animateEnterExit()` 添加的子级效果也属于同一个逻辑动画组。只要其中存在未 opt-in
或当前 Native 路径未支持的活跃效果，整个组都会回退。

### Crossfade

```kotlin
@Composable
fun NativeCrossfadeExample(page: String) {
    Crossfade(
        targetState = page,
        animationSpec = tween<Float>(300).preferNative(),
        label = "pageCrossfade"
    ) { targetPage ->
        when (targetPage) {
            "home" -> HomeContent()
            else -> DetailContent()
        }
    }
}
```

Crossfade 只让 Native 执行旧内容和新内容的 alpha 动画。两份内容本身的测量、布局和业务
重组仍由 Compose 完成。如果内容切换同时依赖容器尺寸动画，应使用 Compose 路径。

## 常用弹窗和转场场景

### 居中 Dialog

`Dialog` 本身是覆盖层容器，没有内置出现/退出动画，也没有 Native 动画开关。常见实现是：

- Dialog 遮罩淡入淡出。
- 内容使用 `AnimatedVisibility` 的 fade + scale。
- 退出动画完成后再把 Dialog 从 Composition 中移除。

内容的 fade + scale 可以使用 Native。不能在点击关闭时立即执行：

```kotlin
if (showDialog) {
    Dialog(...) {
        content()
    }
}
```

如果关闭操作立刻把 `showDialog` 设为 `false`，整个 Dialog 会被移除，Compose 和 Native 都
没有机会播放退出动画。业务需要用 `MutableTransitionState` 保留退出节点：

```kotlin
@Composable
fun NativeAnimatedDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit
) {
    val visibility = remember { MutableTransitionState(false) }
    var exitRequested by remember { mutableStateOf(false) }
    val visualSpec = tween<Float>(260).preferNative()

    LaunchedEffect(show) {
        if (show) {
            exitRequested = false
            visibility.targetState = true
        } else if (visibility.currentState || visibility.targetState) {
            exitRequested = true
            visibility.targetState = false
        }
    }

    LaunchedEffect(
        visibility.isIdle,
        visibility.currentState,
        exitRequested
    ) {
        if (exitRequested && visibility.isIdle && !visibility.currentState) {
            exitRequested = false
            onDismissed()
        }
    }

    if (show || visibility.currentState || !visibility.isIdle) {
        Dialog(
            onDismissRequest = onDismissRequest
        ) {
            AnimatedVisibility(
                visibleState = visibility,
                enter = fadeIn(visualSpec) +
                    scaleIn(visualSpec, initialScale = 0.88f),
                exit = fadeOut(visualSpec) +
                    scaleOut(visualSpec, targetScale = 0.88f)
            ) {
                content()
            }
        }
    }
}
```

如果需要遮罩也走 Native，建议将 `DialogProperties.scrimColor` 设置为透明，然后在 Dialog
内容中放置一个全屏、纯色背景节点，并用 `animateColorAsState(...preferNative())` 动画其
背景色。不要使用 Float 动画后在业务层间接计算文字色、渐变或其他绘制对象。

### Popup、下拉菜单和 Tooltip

`Popup` 当前负责创建覆盖层和计算静态锚点位置，本身没有出现/退出动画。

以下效果可以通过内部 `AnimatedVisibility` 交给 Native View 执行：

- fade
- scale
- transformOrigin
- 静态锚点基础上的轻量 slide

以下效果当前仍由 Compose 执行：

- 动态修改 `Popup.offset`
- 锚点 View 移动时逐帧重算 Popup 位置
- Popup 尺寸变化
- 依赖测量结果的展开/收缩

常见菜单的“从锚点缩放并淡入”属于支持范围；但 Popup 必须像 Dialog 一样保留到 exit
完成，不能在点击关闭时立即从 Composition 移除。

### ModalBottomSheet

当前 `ModalBottomSheet` 的非手势 show/hide 位移默认使用 Native。它内部包含三类动画：

1. 遮罩：继续使用原有 Compose `animateFloatAsState`。
2. 自动出现/退出：`AnimatedVisibility` + `slideInVertically` /
   `slideOutVertically`。
3. 手势拖拽和回弹：`Modifier.offset` + 逐帧 `animate`。

默认情况下，只有非手势的 show/hide slide 交给 Native View 执行。遮罩、手势拖拽、
布局、生命周期和回调都保持原来的 Compose 行为。

#### 组件 API

不建议通过 `Modifier` 切换动画执行方式。Modifier 只作用于 Sheet 内容节点，无法同时控制
Dialog 遮罩、退出节点生命周期、完成回调和拖拽状态。

现有不带开关的 `ModalBottomSheet` 函数签名保持不变，并默认使用 Native show/hide 位移。
另提供实验性 overload，用于灰度或强制回到 Compose：

```kotlin
@Composable
fun ModalBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    preferNativeAnimation: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dismissOnDrag: Boolean = false,
    dismissThreshold: Float = 0.25f,
    animationDurationMillis: Int = 250,
    content: @Composable ColumnScope.() -> Unit
)
```

使用方式：

```kotlin
@Composable
fun BottomSheetExample(show: Boolean) {
    ModalBottomSheet(
        visible = show,
        onDismissRequest = { /* 更新 show */ }
    ) {
        SheetContent()
    }
}
```

需要强制使用 Compose 时显式传入 `preferNativeAnimation = false`。该 overload 复用同一个
状态实现：

1. 默认 slide 的 `FiniteAnimationSpec<IntOffset>` 在组件内部调用 `preferNative()`。
2. 遮罩继续使用原来的 Compose Float 动画。
3. slide 不满足能力时回退 Compose。
4. `dismissOnDrag` 的按手指移动阶段继续由 Compose 驱动；只在稳定状态执行 Native
   show/hide。

### Snackbar 和 Toast

`SnackbarHost` 当前使用两个 `Animatable` 驱动 `graphicsLayer` 的 alpha 和 scale，这两个
属性属于 Native 支持范围。

但组件内部 animation spec 目前没有调用 `preferNative()`，所以现有 `SnackbarHost` 仍走
Compose。后续可以通过高层组件 opt-in 参数增量接入，不需要改变 Snackbar 的布局结构。

### 页面转场和 NavHost

`NavHost` 当前通过 `AnimatedContent` 管理页面切换。`AnimatedContent` 除进入/退出的
fade、scale、slide 外，还负责：

- 同时保留旧页面和新页面。
- 计算两份内容的尺寸和对齐。
- 默认执行 `SizeTransform`。
- 维护退出页面的生命周期和销毁时机。
- 处理中途再次导航时的多内容状态。

当前已支持一个专门面向 `NavHost` 的受限 Native 子集：

- NavHost 容器尺寸固定，页面先完成一次终态测量。
- Enter/Exit 只包含 slide，或者 slide + fade；不混入 scale、expand/shrink 或自定义效果。
- 业务传入普通 animation spec 即可，不需要调用 `preferNative()`；NavHost 会在内部将符合条件的
  slide 和 fade 一起切换到 Native。
- 同一次 push/pop 中所有 slide 使用相同的 duration、delay 和 easing。
- 不包含共享元素、手势返回或 seek/scrub。

满足这些条件时，NavHost 会自动关闭该次 `AnimatedContent` 的默认 `SizeTransform`。新旧
页面属于同一个 Native 动画组：push 时新页面滑入、旧页面视差滑出；pop 时执行相反方向。
两边 Native 回调全部完成后，才释放退出页面并完成 Navigation 生命周期切换。

使用方式：

```kotlin
val slideSpec = tween<IntOffset>(
    durationMillis = 300,
    easing = FastOutSlowInEasing
)

NavHost(
    navController = navController,
    startDestination = "home",
    enterTransition = {
        slideInHorizontally(slideSpec) { it }
    },
    exitTransition = {
        slideOutHorizontally(slideSpec) { -it / 3 }
    },
    popEnterTransition = {
        slideInHorizontally(slideSpec) { -it / 3 }
    },
    popExitTransition = {
        slideOutHorizontally(slideSpec) { it }
    }
) {
    // destinations
}
```

`preferNativeSlideAnimation` 默认是 `true`，因此正常业务不需要设置；传入 `false` 可以强制
回到 Compose，方便灰度或对照。纯 slide 和 slide + fade 会自动使用 Native；包含 scale、
尺寸变化或其他混合效果时仍整体使用 Compose。通用 `AnimatedContent` 的行为不变。

## 回退规则

以下任意情况会触发 Compose 回退：

- spec 未调用 `preferNative()`。
- spec 类型或 easing 当前未支持。
- 动画组中出现当前 Native 路径未支持的属性。
- 同一 Transition 中有部分活跃子动画没有 opt-in。
- 动画值驱动测量、布局、文字、Canvas 或业务分支。
- Animatable 存在逐帧 block、bounds 或非零初速度。
- Transition 处于 seek/scrub 状态。
- Native 动画事务没有收集到合法的目标属性。

回退发生在目标属性正式提交 Native 前。Coordinator 会丢弃暂存的目标操作、恢复属性缓存，
再启动原有 Compose 插帧链路，避免目标状态在回退前闪烁一次。

回退不是错误。业务可以安全地对动画 spec 调用 `preferNative()`，能力不满足时仍会保持
原 Compose 动画结果。

## 常见误用

### 只给组合动画中的一个 spec 调用 preferNative

```kotlin
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween<Float>(300).preferNative()) +
        scaleIn(tween(300)), // 未 opt-in
    exit = fadeOut(tween<Float>(300).preferNative())
) {
    content()
}
```

结果：整组回退 Compose。应为所有活跃效果提供调用过 `preferNative()` 的 spec。

### 动画值同时驱动视觉属性和布局

```kotlin
val progress by animateFloatAsState(
    targetValue = target,
    animationSpec = tween<Float>(300).preferNative()
)

Box(
    Modifier
        .width((100 + progress * 100).dp)
        .graphicsLayer { alpha = progress }
)
```

结果：整组回退，因为 width 需要逐帧重新测量。

### 用逻辑 value 获取 Native 中间进度

```kotlin
LaunchedEffect(alpha) {
    reportVisibleProgress(alpha)
}
```

Native 模式下 `alpha` 很快成为逻辑终值，不代表平台正在显示的中间 alpha。需要逐帧业务
回调时，应使用 Compose 动画，不应启用 Native。

### 立即移除退出节点

```kotlin
if (visible) {
    Dialog { content() }
}
```

结果：`visible = false` 时节点立即销毁，没有退出动画。应使用
`MutableTransitionState` 或其他状态保留策略，等待 `isIdle && !currentState` 后再移除。

### 用 Native slide 期待兄弟布局跟随

Native slide 是 graphics transform，只改变 View 的视觉位置，不改变 Compose 布局位置。
如果动画要求兄弟节点逐帧让位、重新排列或跟随移动，必须使用 Compose 布局动画。

## 调试和验证

框架开发阶段可以临时将 `NativeAnimationTrace.enabled` 设为 `true`，再在控制台过滤：

```text
[NativeAnimation]
```

常见 Compose 日志含义：

- `candidate accepted`：spec 和基础条件通过，进入 Native 候选。
- `create group`：创建页面级动画事务。
- `stage`：目标属性已暂存，尚未提交 Native。
- `commit`：动画组通过检查并提交 Native。
- `fallback`：动画组回退 Compose。
- `rollback`：丢弃目标态暂存操作并恢复缓存。
- `descriptor rejected reason=not-preferred`：spec 未调用 `preferNative()`。
- `descriptor rejected reason=non-zero-velocity`：逻辑初速度非零。
- `mark unsupported`：目标态发现布局、文字或其他不支持属性。

验证 Native 动画是否真正独立运行时，可以在动画期间人为增加 Kuikly 线程重组负载。已经
提交的 Native alpha/transform/backgroundColor 应继续播放，而 Compose 插帧动画可能受到
线程繁忙影响。

## 当前能力边界总结

适合优先使用 Native 的场景：

- 弹窗内容 fade + scale。
- 静态锚点菜单 fade + scale。
- BottomSheet 非手势 show/hide 的 transform slide。
- 固定容器 NavHost 的纯 slide 或 slide + fade push/pop。
- 简单提示条 alpha + scale。
- 不改变布局的卡片 alpha、translation、scale、rotation。
- Crossfade。
- 纯色背景切换。

应继续使用 Compose 的场景：

- width、height、padding 等会改变测量或兄弟布局的动画。
- expand/shrink、`animateContentSize`。
- 通用 AnimatedContent，以及包含 scale、尺寸变化或手势返回的 NavHost 转场。
- 手势拖拽、滑动返回、seek、scrub。
- 逐帧业务回调。
- 文字、渐变、Canvas 和自定义绘制动画。
- 无限动画、关键帧、decay/fling。

当前版本明确使用 Compose、后续有需要再评估的属性：

- `Modifier.offset`，包括只改变 x/y 的情况。
- 单组结构化阴影参数。
- 矩形或圆角矩形 clip bounds。

Native 属性动画的目标是提高常用视觉动画在 Kuikly 线程繁忙时的稳定性，而不是取代完整的
Compose 动画系统。选择执行路径时，应首先判断动画是否只改变单个或多个 Native View 的
可插值属性；只要需要 Kuikly 逐帧重新计算布局关系或执行业务逻辑，就应保留 Compose 路径。
