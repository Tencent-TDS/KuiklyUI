# 核心组件

本页说明 Kuikly Compose 中对齐的核心组件及使用注意事项。  
基础用法和官方保持一致，请优先查阅 Jetpack Compose 官方文档。

> 官方文档（推荐阅读）：[Compose Components](https://developer.android.com/develop/ui/compose/components?hl=zh-cn)  

## 支持的组件

Kuikly 基于 Compose 1.7 的能力做了对齐，下列为当前支持的常用组件

### 文本与输入
- **Text** - 文本显示（支持 `style` / `color` / `maxLines` 等）
- **TextField** / **OutlinedTextField** - 文本输入框（支持 label / placeholder / leadingIcon / trailingIcon 等）

### 图片展示
- **Image** - 图片组件，支持：  
  - 本地图片（如 `painterResource`、`ColorPainter`、`BrushPainter` 等）  
  - 网络图片（通过 `rememberAsyncImagePainter("https://...")` 加载远程资源）  

### 基础容器
- **Scaffold** - 页面脚手架（支持 TopBar、BottomBar、SnackbarHost 等插槽）
- **Surface** - 基础容器组件，提供背景色、内容色等配置

### 按钮
- **Button** / **ElevatedButton** / **FilledTonalButton** - 实心按钮、带阴影按钮、色彩弱化按钮
- **OutlinedButton** / **TextButton** - 轮廓按钮、文字按钮

### 控件
- **Checkbox** - 复选框
- **Switch** - 开关
- **Slider** / **RangeSlider** - 单值/范围滑块

### 导航栏与标签栏
- **TopAppBar** / **CenterAlignedTopAppBar** - 顶部应用栏
- **TabRow** / **ScrollableTabRow** - 标签栏
- **Tab** - 标签页内容

### 状态与反馈
- **Dialog** - 对话框（支持自定义内容、背景遮罩、点击外部关闭等配置）
- **Snackbar** / **SnackbarHost** - 底部消息提示
- **ModalBottomSheet** - 底部弹窗
- **CircularProgressIndicator** / **LinearProgressIndicator** - 圆形 / 线性进度条（支持确定/不确定两种形态）

## 兼容性说明

**完全兼容的组件**：

- 绝大多数 Material3 组件在 Kuikly 中的 API 形态与行为都与 Jetpack Compose 一致
- 对于这类组件，我们在不会重复参数表，建议直接参考 Jetpack Compose 官方文档了解详细 API

**存在差异或局限的组件**：
- 某些平台上受限于原生控件实现，可能在细节上与 Android 官方实现略有差异
- 若有明显行为差异或暂不支持的属性，会在「差异化说明」中明确标注

## 差异化说明

Kuikly Compose 基于原生组件实现，部分功能与官方 Compose 存在差异。这些差异主要分为两类：
1. **原生组件切换导致的差异**：由于将 Compose 的渲染层从 Android View/Skia 切换到 KuiklyCore 渲染引擎，部分功能受限于原生控件的实现能力
2. **完全有平替 API 的差异**：某些组件或功能虽然不直接支持，但可以通过其他 API 组合实现相同效果

### 已知差异化点

#### 1. ClickableText 组件

**差异说明**：`ClickableText` 组件暂不支持，但有完全可用的平替方案。

**平替方案**：使用 `Text` + `AnnotatedString`，配合 `LinkAnnotation.Clickable` 实现可点击文本。

```kotlin
@Composable
fun ClickableTextAlternative() {
    val linkStyle = SpanStyle(
        color = Color.Blue,
        fontWeight = FontWeight.Medium,
    )
    
    val annotatedString = buildAnnotatedString {
        append("我已阅读并同意")
        withLink(
            LinkAnnotation.Clickable(
                tag = "agreement",
                styles = linkStyle,
                linkInteractionListener = LinkInteractionListener {
                    // 处理点击事件
                },
            ),
        ) {
            append("《用户协议》")
        }
    }
    
    Text(text = annotatedString)
}
```

**参考示例**：[TextDemo.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/2947359b91b072e1868bb3200faf46bb0fe583f9/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextDemo.kt#L127)

#### 2. Modifier.shadow 的 ambientShadowColor 参数

**差异说明**：`Modifier.shadow` 的 `ambientShadowColor` 参数目前明确无效，这是原生组件切换导致的限制。

**解决方案**：使用 `spotShadowColor` 参数来调整阴影效果。

```kotlin
@Composable
fun ShadowExample() {
    Box(
        modifier = Modifier
            .shadow(
                elevation = 8.dp,
                spotShadowColor = Color.Black.copy(alpha = 0.3f)
                // ambientShadowColor 参数无效，忽略即可
            )
    ) {
        Text("带阴影的文本")
    }
}
```

#### 3. Text 组件不支持自由复制

**差异说明**：`Text` 组件不支持用户自由选择和复制文本，这是原生组件切换导致的限制。

**影响范围**：所有使用 `Text` 组件的地方，用户无法通过长按等方式选择并复制文本内容。

#### 4. TextField 不支持指定选择范围

**差异说明**：`TextField` 组件不支持通过代码指定文本的选择范围，这是原生组件切换导致的限制。

**影响范围**：无法通过 `TextFieldValue` 的 `selection` 参数来程序化控制文本选择范围。

#### 5. Modifier.horizontalScroll 和 Modifier.verticalScroll

**差异说明**：`Modifier.horizontalScroll` 和 `Modifier.verticalScroll` 暂不支持，但有完全可用的平替方案。

**平替方案**：
- `Modifier.horizontalScroll` → 使用 `LazyRow` 代替
- `Modifier.verticalScroll` → 使用 `LazyColumn` 代替

`LazyRow` 和 `LazyColumn` 提供了更好的性能和更丰富的功能（如懒加载、状态管理等）。

```kotlin
@Composable
fun ScrollAlternatives() {
    // 水平滚动：使用 LazyRow 代替 Modifier.horizontalScroll
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = listOf("Item 1", "Item 2", "Item 3")) { item ->
            Text(item)
        }
    }
    
    // 垂直滚动：使用 LazyColumn 代替 Modifier.verticalScroll
    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = listOf("Item 1", "Item 2", "Item 3")) { item ->
            Text(item)
        }
    }
}
```

> **提示**：以上为当前已知的差异化点，更多差异化内容将持续更新补充。

## 扩展能力

Kuikly Compose 在保持与官方 Compose API 兼容的基础上，为部分组件提供了额外的扩展能力，以满足跨平台场景的特殊需求。

### 通用扩展能力

#### 原生视图引用：`Modifier.nativeRef`

用于获取组件的原生视图引用，常用于需要直接操作原生视图的场景。

```kotlin
@Composable
fun ComponentWithNativeRef() {
    Text(
        text = "示例文本",
        modifier = Modifier.nativeRef { viewRef ->
            // 获取原生视图引用，可用于扩展原生能力
            println("Native ref: ${viewRef.nativeRef}")
        }
    )
}
```

**相关 API**：
- `Modifier.nativeRef(ref: RefFunc<DeclarativeBaseView<*, *>>?)` - 获取原生视图引用
- `ViewRef` 包含 `pagerId` 和 `nativeRef` 属性


### Text 组件扩展

#### 最后一行预留空间：`Modifier.lineBreakMargin`

用于控制文本最后一行折叠"..." 距离最右边的距离，常用于显示"更多"展开场景。

```kotlin
@Composable
fun TextWithLineBreakMargin() {
    Text(
        text = "这是一段很长的文本，用来测试文本的自动换行和截断功能...",
        modifier = Modifier
            .fillMaxWidth()
            .lineBreakMargin(100.dp),  // 最后一行预留 100dp 空间
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
```

**相关 API**：
- `Modifier.lineBreakMargin(dp: Dp)` - 设置最后一行预留空间
- `Modifier.onLineBreakMargin(callback: (Any?) -> Unit)` - 监听预留空间相关事件

#### 行间距设置：`Modifier.lineSpacing`

用于设置文本的行间距。

```kotlin
@Composable
fun TextWithLineSpacing() {
    Text(
        text = "这是一段多行文本\n第二行文本\n第三行文本",
        modifier = Modifier.lineSpacing(8f)  // 设置行间距为 8
    )
}
```

**相关 API**：
- `Modifier.lineSpacing(lineSpace: Float?)` - 设置行间距（单位：dp）

### TextField 组件扩展

#### 键盘高度变化监听：`Modifier.keyboardHeightChange`

用于监听键盘弹出/收起时的高度变化，常用于实现输入框跟随键盘移动的效果。

```kotlin
@Composable
fun TextFieldWithKeyboardHeight() {
    var keyboardHeight by remember { mutableStateOf(0f) }
    
    TextField(
        value = "",
        onValueChange = { },
        modifier = Modifier
            .keyboardHeightChange { params ->
                keyboardHeight = params.height
                // 根据键盘高度调整布局
            }
    )
}
```

**相关 API**：
- `Modifier.keyboardHeightChange(callback: (KeyboardParams) -> Unit)` - 监听键盘高度变化
- `KeyboardParams` 包含 `height`（键盘高度）和 `duration`（动画时长）属性

#### 占位符设置：`Modifier.placeHolder` / `Modifier.placeholderColor`

用于设置输入框的占位符文本和颜色。

```kotlin
@Composable
fun TextFieldWithPlaceholder() {
    TextField(
        value = "",
        onValueChange = { },
        modifier = Modifier
            .placeHolder(
                placeholder = "请输入内容",
                placeholderColor = Color.Gray
            )
    )
}
```

**相关 API**：
- `Modifier.placeHolder(placeholder: String, placeholderColor: Color)` - 同时设置占位符文本和颜色
- `Modifier.placeholderColor(color: Color)` - 单独设置占位符颜色

> **提示**：以上为当前已支持的扩展能力，更多扩展能力将持续更新补充。

## 更多代码示例

以下 Demo 展示了核心组件的典型用法，可在开源仓库中查看完整代码：

- [`MaterialDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/MaterialDemo.kt)：Material3 组件综合示例（包含 Checkbox、Switch、Slider、ProgressIndicator、Snackbar 等）
- [`TextFieldDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextFieldDemo.kt)：`TextField` / `OutlinedTextField` 组件示例
- [`TextDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextDemo.kt)：`Text` 组件示例
- [`ImageDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ImageDemo.kt)：`Image` 组件示例（包含本地图片和网络图片加载）
- [`AppBarDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/AppBarDemo.kt)：`TopAppBar` / `CenterAlignedTopAppBar` 组件示例
- [`TabRowDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TabRowDemo.kt)：`TabRow` / `ScrollableTabRow` / `Tab` 组件示例
- [`DialogDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/DialogDemo.kt)：`Dialog` 组件示例
- [`BottomSheetDemo1.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/BottomSheetDemo1.kt)：`ModalBottomSheet` 组件示例
- [`ScaffoldDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ScaffoldDemo.kt)：`Scaffold` 组件示例（包含 TopBar、BottomBar、SnackbarHost 等插槽）

## 注意事项

- **扩展能力使用**：`lineBreakMargin`、`keyboardHeightChange`、`nativeRef` 等扩展能力是 Kuikly 特有的功能，在官方 Compose 中不可用。
- **原生视图引用**：`nativeRef` 主要用于需要直接操作原生视图的高级场景，如集成第三方原生组件、调用平台特定 API 等。使用时应谨慎，避免破坏 Compose 的声明式特性。
