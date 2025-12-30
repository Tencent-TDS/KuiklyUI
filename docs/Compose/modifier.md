# Modifier

本页说明 Kuikly Compose 中 Modifier 的支持情况与使用注意事项。  
基础用法和官方保持一致，请**优先查阅 Jetpack Compose 官方文档**。

> 官方文档（推荐阅读）：[Modifiers in Compose](https://developer.android.com/develop/ui/compose/modifiers?hl=zh-cn)

## 支持的 Modifier

Kuikly 当前重点支持以下 Modifier，并与 Jetpack Compose 对齐：

### 尺寸控制

- **基础尺寸**
  - `size()` / `size(width, height)` - 设置宽高尺寸
  - `width()` / `height()` - 单独设置宽度或高度
  - `fillMaxWidth()` / `fillMaxHeight()` / `fillMaxSize()` - 填充父容器
  - `wrapContentSize()` / `wrapContentWidth()` / `wrapContentHeight()` - 包裹内容
- **强制尺寸**
  - `requiredSize()` / `requiredWidth()` / `requiredHeight()` - 强制尺寸（忽略约束）
- **尺寸范围**
  - `widthIn()` / `heightIn()` / `sizeIn()` - 设置尺寸范围
  - `defaultMinSize()` - 设置最小尺寸
- **强制尺寸范围**
  - `requiredWidthIn()` - 强制宽度范围（忽略约束）
  - `requiredHeightIn()` - 强制高度范围（忽略约束）
  - `requiredSizeIn()` - 强制尺寸范围（忽略约束）

### 间距与位置

- **内边距**
  - `padding()` - 设置内边距（支持 all、horizontal/vertical、各边单独设置）
  - `absolutePadding()` - 绝对内边距（不考虑布局方向）
- **位移**
  - `offset()` - 相对位移（考虑布局方向）
  - `absoluteOffset()` - 绝对位移（不考虑布局方向）

### 形状与样式

- **背景**
  - `background()` - 设置背景色或渐变（支持 `Color` 或 `Brush`）
- **边框**
  - `border()` - 设置边框（支持宽度、颜色、形状）
- **裁剪**
  - `clip()` - 裁剪内容为指定形状
  - `clipToBounds()` - 裁剪到边界
- **绘图效果**
  - `alpha()` - 设置透明度
  - `shadow()` - 设置阴影效果
  - `zIndex()` - 设置 Z 轴顺序（层叠顺序）

### 交互

- **点击**
  - `clickable()` - 点击事件处理
  - `combinedClickable()` - 组合点击事件（支持单击、双击、长按）
- **切换**
  - `toggleable()` - 将组件配置为可通过输入和无障碍事件切换
  - `triStateToggleable()` - 将组件配置为可在三种状态之间切换：启用、停用和不确定

### 对齐

- **对齐方式**
  - `align()` - 对齐（配合 `Box` 使用）
  - `alignBy()` - 对齐（配合 `Row` 使用）
  - `alignByBaseline()` - 基线对齐（配合 `Row` 使用）

### 焦点

- **焦点管理**
  - `focusable()` - 将组件配置为可通过焦点系统或无障碍事件聚焦
  - `focusTarget()` - 使组件可聚焦
  - `onFocusChanged()` - 观察焦点状态事件
  - `focusProperties()` - 指定可供修饰符链中更底层或子布局节点访问的焦点属性
  - `focusRequester()` - 请求更改焦点
  - `focusRestorer()` - 保存焦点小组以及将焦点恢复到焦点小组
  - `focusGroup()` - 创建焦点群组或将组件标记为焦点群组
  - `onFocusedBoundsChanged()` - 当当前聚焦区域的边界发生变化时调用

### Graphics

- **图形变换**
  - `graphicsLayer()` - 图形层变换（支持缩放、旋转、透明度等）
  - `rotate()` - 旋转
  - `scale()` - 缩放

### Layout

- **布局控制**
  - `layout()` - 创建 LayoutModifier，允许更改元素的测量和布局方式
  - `layoutId()` - 使用 layoutId 标记元素，以在其父项中识别它
  - `onGloballyPositioned()` - 当内容的全局位置可能发生变化时调用
  - `onPlaced()` - 当元素被放置时调用
  - `onSizeChanged()` - 首次测量元素时或元素的大小发生变化时调用

### 其他常用 Modifier

- **权重与占比**
  - `weight()` - 权重（配合 `Row` / `Column` 使用）
  - `aspectRatio()` - 固定宽高比
- **父容器填充**
  - `matchParentSize()` - 匹配父容器尺寸（配合 `Box` 使用）
  - `fillParentMaxWidth()` - 填充父容器最大宽度
  - `fillParentMaxHeight()` - 填充父容器最大高度
  - `fillParentMaxSize()` - 填充父容器最大尺寸
- **动画**
  - `animateContentSize()` - 内容尺寸变化时的平滑动画
- **高级功能**
  - `composed()` - 组合多个 Modifier
  - `inspectable()` - 对一组常用的修饰符进行分组，并为生成的修饰符提供 InspectorInfo
  - `modifierLocalConsumer()` - 使用由布局树中的其他修饰符提供的 ModifierLocal
  - `modifierLocalProvider()` - 提供可被其他修饰符读取的 ModifierLocal
  - `pointerInput()` - 创建一个用于在修饰的元素区域内处理指针输入的修饰符

## Modifier 组合示例

```kotlin
@Composable
fun BadgeExample() {
    Text(
        text = "NEW",
        modifier = Modifier
            .background(Color.Red)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .size(width = 56.dp, height = 24.dp)
    )
}
```

## 内外边距示例

```kotlin
@Composable
fun MarginPaddingExample() {
    Column {
        // 1. 外边距：padding 在 background 之前，相当于父容器的 margin
        Box(
            modifier = Modifier
                .padding(16.dp)  // 外边距
                .background(Color.Yellow)
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("外边距示例")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 内边距：padding 在 background 之后，挤压内部内容
        Box(
            modifier = Modifier
                .background(Color.Yellow)
                .padding(16.dp)  // 内边距
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.background(Color.White)) {
                Text("内边距示例")
            }
        }
    }
}
```

## 更多代码示例

以下 Demo 展示了 Modifier 的典型用法，可在开源仓库中查看完整代码：

- [`MarginPaddingTestDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/MarginPaddingTestDemo.kt)：内外边距、边框、背景等 Modifier 综合示例

## 注意事项

- **Modifier 顺序很重要**：Modifier 的顺序会直接影响最终效果。例如：
  - `padding().background()` - padding 在外，background 在内（相当于外边距）
  - `background().padding()` - background 在外，padding 在内（相当于内边距）
- **性能优化**：尽量复用常用的 Modifier 链，避免在高频重组中创建新的 Modifier 对象。
- **链式组合**：Modifier 支持链式组合，多个 Modifier 可以通过 `.` 操作符连接，按顺序应用。
