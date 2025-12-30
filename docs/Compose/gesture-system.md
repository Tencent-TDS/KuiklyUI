# 手势系统

本页说明 Kuikly Compose 中手势处理 API 的支持情况与使用注意事项。  
基础用法和官方保持一致，请**优先查阅 Jetpack Compose 官方文档**。

> 官方文档（推荐阅读）：[Gestures in Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures?hl=zh-cn)

## 支持的手势 API

Kuikly 当前重点支持以下手势 API，并与 Jetpack Compose 对齐：

### 点击手势

- **`clickable()`** - 基础点击事件处理，支持单击、双击、长按等
- **`combinedClickable()`** - 组合点击事件，可同时处理单击、双击、长按

### 拖拽手势

- **`draggable()`** - 单向拖拽（水平或垂直方向）
- **`draggable2D()`** - 双向拖拽（同时支持水平和垂直方向）
- **`rememberDraggableState()`** - 创建并记住拖拽状态
- **`rememberDraggable2DState()`** - 创建并记住双向拖拽状态
- **`detectDragGestures()`** - 通过 `pointerInput` 检测拖拽手势（支持自定义处理）
- **`detectDragGesturesAfterLongPress()`** - 长按后拖拽
- **`detectHorizontalDragGestures()`** - 水平方向拖拽检测
- **`detectVerticalDragGestures()`** - 垂直方向拖拽检测

### 自定义手势

- **`pointerInput()`** - 底层指针输入处理，可用于实现自定义手势
  - `detectTapGestures()` - 检测点击手势（单击、双击、长按）
  - `detectDragGestures()` - 检测拖拽手势
  - `detectTransformGestures()` - 检测变换手势（支持**缩放、旋转、平移**）

### 高级手势（实验性）

- **`anchoredDraggable()`** - 锚点拖拽（标记为 `@ExperimentalFoundationApi`，用于实现类似底部抽屉等组件）

## 手势示例

### 点击手势示例

```kotlin
@Composable
fun ClickableExample() {
    var clickCount by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color.Blue)
            .clickable { clickCount++ },
        contentAlignment = Alignment.Center
    ) {
        Text("点击次数: $clickCount", color = Color.White)
    }
}
```

### 拖拽手势示例

```kotlin
@Composable
fun DraggableExample() {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .background(Color.Red)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("可拖拽", color = Color.White)
    }
}
```

### 缩放手势示例

```kotlin
@Composable
fun ZoomGestureExample() {
    var scale by remember { mutableStateOf(1f) }
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .background(Color.Green)
            .pointerInput(Unit) {
                detectTransformGestures(
                    panZoomLock = true  // 锁定平移和缩放，禁用旋转
                ) { _, _, zoomChange, _ ->
                    scale *= zoomChange
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text("双指缩放", color = Color.White)
    }
}
```

### 变换手势示例

```kotlin
@Composable
fun TransformableExample() {
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                rotationZ = rotation,
                translationX = offset.x,
                translationY = offset.y
            )
            .background(Color.Green)
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, rotationChange ->
                    scale *= zoomChange
                    rotation += rotationChange
                    offset += panChange
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text("可缩放/旋转/平移", color = Color.White)
    }
}
```

### 组合点击示例

```kotlin
@Composable
fun CombinedClickableExample() {
    var clickInfo by remember { mutableStateOf("等待点击...") }
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color.Blue)
            .combinedClickable(
                onClick = { clickInfo = "单击" },
                onDoubleClick = { clickInfo = "双击" },
                onLongClick = { clickInfo = "长按" }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(clickInfo, color = Color.White)
    }
}
```

## 更多代码示例

以下 Demo 展示了手势 API 的典型用法，可在开源仓库中查看完整代码：

- [`GestureTestDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/GestureTestDemo.kt)：手势综合示例（包含点击、拖拽、变换等手势）

## 注意事项

- **性能考虑**：使用 `pointerInput()` 进行自定义手势处理时，注意避免在回调中进行耗时操作，以免影响手势响应性能。
- **状态管理**：手势相关的状态（如拖拽偏移量、缩放比例等）应使用 `remember` 和 `mutableStateOf` 管理，确保状态在重组时正确保持。
