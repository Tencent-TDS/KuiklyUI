# 已支持组件列表

Kuikly Compose 致力于提供完整的 Compose API 支持，以实现跨平台 UI 开发的一致性体验。本文档列出了当前已支持的API说明。

> **注意**：
> 1. 我们的目标是支持所有标准的 Compose API，但部分 API 的参数可能暂时未完全支持
> 2. 所有 API 的具体实现和参数支持情况以 Kuikly SDK 的实际表现为准
> 3. 如果您在使用过程中发现任何 API 支持问题，欢迎在 [GitHub Issues](https://github.com/Tencent-TDS/KuiklyUI/issues) 中反馈
> 4. 目前除了 Material 库中的组件外，我们已经支持了标准 Compose API 约 90% 的功能，包括布局系统、动画系统、手势系统等核心功能

## 组件

Kuikly Compose 完全支持 Compose 的布局系统，包括所有布局组件和布局修饰符。

### Text
```kotlin
Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
)
```
- 功能：显示文本内容
- 主要参数：
  - text: 显示的文本内容
  - color: 文本颜色
  - fontSize: 字体大小
  - fontWeight: 字体粗细
  - textAlign: 文本对齐方式

### Image
```kotlin
Image(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
)
```
- 功能：显示图片
- 主要参数：
  - painter: 图片资源
  - contentDescription: 图片描述
  - contentScale: 图片缩放方式
  - colorFilter: 颜色滤镜

### BasicTextField
```kotlin
BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = @Composable { innerTextField -> innerTextField() }
)
```
- 功能：基础文本输入框
- 参数：
  - value: 文本值
  - onValueChange: 值变化回调
  - textStyle: 文本样式
  - keyboardOptions: 键盘选项
  - keyboardActions: 键盘动作
  - singleLine: 是否单行
  - maxLines: 最大行数
  - visualTransformation: 视觉转换
  - decorationBox: 装饰盒

### TextField
```kotlin
TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = TextFieldDefaults.colors()
)
```
- 功能：文本输入框
- 参数：
  - value: 文本值
  - onValueChange: 值变化回调
  - label: 标签
  - placeholder: 占位符
  - leadingIcon: 前导图标
  - trailingIcon: 后导图标
  - isError: 是否错误
  - visualTransformation: 视觉转换
  - keyboardOptions: 键盘选项
  - keyboardActions: 键盘动作
  - singleLine: 是否单行
  - maxLines: 最大行数
  - shape: 形状
  - colors: 颜色

### Box
```kotlin
Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
)
```
- 功能：创建一个可以叠加子元素的容器
- 参数：
  - modifier: 修饰符
  - contentAlignment: 内容对齐方式
  - content: 子元素内容

### Column
```kotlin
Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
)
```
- 功能：垂直排列子元素的容器
- 参数：
  - modifier: 修饰符
  - verticalArrangement: 垂直排列方式
  - horizontalAlignment: 水平对齐方式
  - content: 子元素内容

### Row
```kotlin
Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
)
```
- 功能：水平排列子元素的容器
- 参数：
  - modifier: 修饰符
  - horizontalArrangement: 水平排列方式
  - verticalAlignment: 垂直对齐方式
  - content: 子元素内容

### LazyColumn
```kotlin
LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
)
```
- 功能：垂直滚动的列表
- 参数：
  - modifier: 修饰符
  - state: 列表状态
  - contentPadding: 内容内边距
  - reverseLayout: 是否反向布局
  - verticalArrangement: 垂直排列方式
  - horizontalAlignment: 水平对齐方式
  - content: 列表内容

### LazyRow
```kotlin
LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: LazyListScope.() -> Unit
)
```
- 功能：水平滚动的列表
- 参数：
  - modifier: 修饰符
  - state: 列表状态
  - contentPadding: 内容内边距
  - reverseLayout: 是否反向布局
  - horizontalArrangement: 水平排列方式
  - verticalAlignment: 垂直对齐方式
  - content: 列表内容

### FlowRow
```kotlin
FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp = 0.dp,
    crossAxisSpacing: Dp = 0.dp,
    mainAxisSize: MainAxisSize = MainAxisSize.Max,
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Start,
    content: @Composable FlowRowScope.() -> Unit
)
```
- 功能：流式布局的行容器，自动换行
- 参数：
  - mainAxisSpacing: 主轴间距
  - crossAxisSpacing: 交叉轴间距
  - mainAxisSize: 主轴尺寸
  - mainAxisAlignment: 主轴对齐方式
  - crossAxisAlignment: 交叉轴对齐方式

### FlowColumn
```kotlin
FlowColumn(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp = 0.dp,
    crossAxisSpacing: Dp = 0.dp,
    mainAxisSize: MainAxisSize = MainAxisSize.Max,
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Start,
    content: @Composable FlowColumnScope.() -> Unit
)
```
- 功能：流式布局的列容器，自动换列
- 参数：同 FlowRow

### LazyVerticalGrid
```kotlin
LazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: LazyGridScope.() -> Unit
)
```
- 功能：垂直网格布局
- 参数：
  - columns: 列配置
  - state: 网格状态
  - contentPadding: 内容内边距
  - verticalArrangement: 垂直排列方式
  - horizontalArrangement: 水平排列方式

### LazyHorizontalGrid
```kotlin
LazyHorizontalGrid(
    rows: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyGridScope.() -> Unit
)
```
- 功能：水平网格布局
- 参数：同 LazyVerticalGrid

### LazyVerticalStaggeredGrid
```kotlin
LazyVerticalStaggeredGrid(
    columns: StaggeredGridCells,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: LazyStaggeredGridScope.() -> Unit
)
```
- 功能：垂直交错网格布局
- 参数：
  - columns: 列配置
  - state: 网格状态
  - contentPadding: 内容内边距
  - verticalArrangement: 垂直排列方式
  - horizontalArrangement: 水平排列方式

### LazyHorizontalStaggeredGrid
```kotlin
LazyHorizontalStaggeredGrid(
    rows: StaggeredGridCells,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyStaggeredGridScope.() -> Unit
)
```
- 功能：水平交错网格布局
- 参数：同 LazyVerticalStaggeredGrid

### HorizontalPager
```kotlin
HorizontalPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSpacing: Dp = 0.dp,
    content: @Composable (page: Int) -> Unit
)
```
- 功能：水平页面切换器
- 参数：
  - pageCount: 页面数量
  - state: 页面状态
  - contentPadding: 内容内边距
  - pageSpacing: 页面间距

### VerticalPager
```kotlin
VerticalPager(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSpacing: Dp = 0.dp,
    content: @Composable (page: Int) -> Unit
)
```
- 功能：垂直页面切换器
- 参数：同 HorizontalPager

### TabRow
```kotlin
TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    edgePadding: Dp = TabRowDefaults.EdgePadding,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = { TabRowDefaults.Indicator },
    divider: @Composable () -> Unit = { TabRowDefaults.Divider },
    tabs: @Composable () -> Unit
)
```
- 功能：标签页行
- 参数：
  - selectedTabIndex: 选中的标签索引
  - containerColor: 容器颜色
  - contentColor: 内容颜色
  - indicator: 指示器
  - divider: 分割线

### Tab
```kotlin
Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null
)
```
- 功能：标签页
- 参数：
  - selected: 是否选中
  - onClick: 点击事件
  - text: 文本内容
  - icon: 图标内容

### ScrollableTabRow
```kotlin
ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    edgePadding: Dp = TabRowDefaults.EdgePadding,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = { TabRowDefaults.Indicator },
    divider: @Composable () -> Unit = { TabRowDefaults.Divider },
    tabs: @Composable () -> Unit
)
```
- 功能：可滚动的标签页行
- 参数：同 TabRow

### Surface
```kotlin
Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
)
```
- 功能：表面容器
- 参数：
  - shape: 形状
  - color: 颜色
  - border: 边框
  - shadowElevation: 阴影高度

### Space
```kotlin
Spacer(modifier: Modifier)
```
- 功能：空白占位符

### Divider
```kotlin
Divider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color
)
```
- 功能：分割线
- 参数：
  - thickness: 厚度
  - color: 颜色

### Card
```kotlin
Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    border: BorderStroke? = null,
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable () -> Unit
)
```
- 功能：卡片容器
- 参数：
  - shape: 形状
  - containerColor: 容器颜色
  - border: 边框
  - elevation: 阴影

### Layout
```kotlin
Layout(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
)
```
- 功能：自定义布局容器
- 参数：
  - content: 内容
  - measurePolicy: 测量策略

### Dialog
```kotlin
Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
)
```
- 功能：对话框
- 参数：
  - onDismissRequest: 关闭请求回调
  - properties: 对话框属性

### Button
```kotlin
Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
)
```
- 功能：可点击的按钮
- 主要参数：
  - onClick: 点击事件回调
  - enabled: 是否启用
  - content: 按钮内容


## 状态管理
Kuikly Compose 完全支持官方Compose 的状态管理API，可查阅官网文档。

## 动画

Kuikly Compose 完全支持官方Compose的动画系统，包括所有动画 API 和动画修饰符，可查阅官网文档。

## 手势

Kuikly Compose 完全支持官方Compose的手势系统，包括所有手势检测和手势修饰符，可查阅官网文档。
