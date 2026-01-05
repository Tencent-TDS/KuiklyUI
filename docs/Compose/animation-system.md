# 动画系统

本页说明 Kuikly Compose 中动画 API 的支持情况与使用注意事项。  
基础用法和官方保持一致，请**优先查阅 Jetpack Compose 官方文档**。

> 官方文档（推荐阅读）：[Animation in Compose](https://developer.android.com/develop/ui/compose/animation/choose-api?hl=zh-cn)

## 支持的动画 API

Kuikly 当前重点支持以下动画 API，并与 Jetpack Compose 对齐：

### 显示与内容动画

- **AnimatedVisibility** - 根据可见性状态动画显示/隐藏内容，支持自定义进入/退出动画
- **AnimatedContent** - 内容切换时的动画过渡，支持自定义内容变换动画
- **Crossfade** - 两个内容之间的淡入淡出过渡动画

### 状态动画

- **animate*AsState 系列** - 根据状态变化自动动画到目标值：
  - `animateColorAsState` - 颜色动画
  - `animateFloatAsState` - 浮点数动画
  - `animateDpAsState` - 尺寸动画（Dp）
  - `animateSizeAsState` - 尺寸动画（Size）
  - `animateOffsetAsState` - 位置动画（Offset）
  - `animateRectAsState` - 矩形动画
  - `animateIntAsState` - 整数动画
  - `animateIntOffsetAsState` - 整数位置动画
  - `animateIntSizeAsState` - 整数尺寸动画
  - `animateValueAsState` - 通用值动画（支持自定义类型）

### 转场动画

- **Transition** / **updateTransition** - 多状态之间的转场动画，支持同时动画多个属性
  - `Transition.animateColor` - 颜色转场动画
  - `Transition.animateFloat` - 浮点数转场动画
  - `Transition.animateDp` - 尺寸转场动画（Dp）
  - `Transition.animateSize` - 尺寸转场动画（Size）
  - `Transition.animateOffset` - 位置转场动画（Offset）
  - `Transition.animateRect` - 矩形转场动画
  - `Transition.animateInt` - 整数转场动画
  - `Transition.animateIntOffset` - 整数位置转场动画
  - `Transition.animateIntSize` - 整数尺寸转场动画
  - `Transition.animateValue` - 通用值转场动画（支持自定义类型）
- **InfiniteTransition** / **rememberInfiniteTransition** - 无限循环动画，适用于加载动画等场景
  - `InfiniteTransition.animateColor` - 无限循环颜色动画
  - `InfiniteTransition.animateFloat` - 无限循环浮点数动画
  - `InfiniteTransition.animateValue` - 无限循环通用值动画（支持自定义类型）

### 进入/退出动画

- **EnterTransition** - 进入动画：
  - `fadeIn()` - 淡入
  - `slideIn()` / `slideInVertically()` / `slideInHorizontally()` - 滑动进入（支持自定义方向和通用方向）
  - `expandIn()` / `expandVertically()` / `expandHorizontally()` - 展开进入
  - `scaleIn()` - 缩放进入
- **ExitTransition** - 退出动画：
  - `fadeOut()` - 淡出
  - `slideOut()` / `slideOutVertically()` / `slideOutHorizontally()` - 滑动退出（支持自定义方向和通用方向）
  - `shrinkOut()` / `shrinkVertically()` / `shrinkHorizontally()` - 收缩退出
  - `scaleOut()` - 缩放退出

### 其他动画 API

- **animateContentSize** - 内容尺寸变化时的平滑动画
- **Animatable** - 可动画的值容器，支持手动控制动画过程

## 动画示例

### AnimatedVisibility 示例

```kotlin
@Composable
fun AnimatedVisibilityExample() {
    var visible by remember { mutableStateOf(true) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { visible = !visible }) {
            Text(if (visible) "隐藏" else "显示")
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Blue),
                contentAlignment = Alignment.Center
            ) {
                Text("动画内容", color = Color.White)
            }
        }
    }
}
```

### 状态动画示例

```kotlin
@Composable
fun StateAnimationExample() {
    var isExpanded by remember { mutableStateOf(false) }
    
    // 颜色动画
    val backgroundColor by animateColorAsState(
        targetValue = if (isExpanded) Color.Green else Color.Red,
        animationSpec = tween(durationMillis = 500)
    )
    
    // 尺寸动画
    val size by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else 100.dp,
        animationSpec = spring()
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { isExpanded = !isExpanded }) {
            Text("切换状态")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(size)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text("动画盒子", color = Color.White)
        }
    }
}
```

### AnimatedContent 示例

```kotlin
@Composable
fun AnimatedContentExample() {
    var count by remember { mutableStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { count++ }) {
            Text("增加")
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = count,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }
        ) { targetCount ->
            Text(
                text = "$targetCount",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

### Crossfade 示例

```kotlin
@Composable
fun CrossfadeExample() {
    var currentScreen by remember { mutableStateOf("Screen1") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { currentScreen = "Screen1" }) {
                Text("屏幕1")
            }
            Button(onClick = { currentScreen = "Screen2" }) {
                Text("屏幕2")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(300)
            ) { screen ->
                when (screen) {
                    "Screen1" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Blue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("屏幕 1 内容", color = Color.White)
                        }
                    }
                    "Screen2" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Green),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("屏幕 2 内容", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
```

### 转场动画示例

```kotlin
@Composable
fun TransitionExample() {
    var state by remember { mutableStateOf(BoxState.Collapsed) }
    val transition = updateTransition(targetState = state, label = "boxTransition")

    // 同时动画多个属性
    val size by transition.animateDp(
        transitionSpec = { spring() },
        label = "size"
    ) { if (it == BoxState.Expanded) 200.dp else 100.dp }

    val color by transition.animateColor(
        transitionSpec = { tween(500) },
        label = "color"
    ) { if (it == BoxState.Expanded) Color.Green else Color.Red }

    val rotation by transition.animateFloat(
        transitionSpec = { tween(600) },
        label = "rotation"
    ) { if (it == BoxState.Expanded) 180f else 0f }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = {
            state = if (state == BoxState.Expanded) BoxState.Collapsed else BoxState.Expanded
        }) {
            Text("切换状态")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    rotationZ = rotation
                }
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text("转场动画", color = Color.White)
        }
    }
}

enum class BoxState { Collapsed, Expanded }
```

## 更多代码示例

以下 Demo 展示了动画 API 的典型用法，可在开源仓库中查看完整代码：

- [`ComposeAnimateDemo1.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeAnimateDemo1.kt)：综合动画示例（包含 AnimatedVisibility、AnimatedContent、animate*AsState、Animatable 等）
- [`ComposeAnimationPage.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeAnimationPage.kt)：基础动画示例页（可见性、颜色、尺寸动画等）
- [`AnimatedVisibilityDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/AnimatedVisibilityDemo.kt)：`AnimatedVisibility` 详细用法示例
- [`AnimatedContentDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/AnimatedContentDemo.kt)：`AnimatedContent` 内容过渡动画示例
- [`CrossfadeDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/CrossfadeDemo.kt)：`Crossfade` 淡入淡出过渡动画示例
- [`TransitionDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TransitionDemo.kt)：`Transition` / `updateTransition` 转场动画示例（包含基础转场、多属性转场、复杂状态转场等）

## 注意事项

- **动画性能**：使用 `animate*AsState` 时，确保目标值的变化是合理的，避免频繁触发动画导致性能问题。对于复杂动画，建议使用 `Transition` 统一管理。
- **动画规格**：`animationSpec` 参数用于控制动画的时长、缓动曲线等。常用的有 `tween()`（指定时长）、`spring()`（弹性动画）、`keyframes()`（关键帧动画）等。
- **组合动画**：进入/退出动画可以通过 `+` 操作符组合多个效果，如 `fadeIn() + slideInVertically()`，多个动画会同时执行。