# 文本输入

本篇说明 Compose DSL 下的文本渲染与输入组件：`BasicText`、`BasicTextField`、`TextFieldState`、`KeyboardOptions`。

:::tip 与 material3 Text/TextField 的区别
`BasicText`、`BasicTextField` 属于 foundation 层，**不消费主题样式**；`Text`、`TextField` 属于 material3 层，会自动读取 `MaterialTheme` 的样式。需要主题能力时优先使用后者（见[核心组件](./core-components.md)）。
:::

## BasicText

纯文本渲染组件。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| text <Badge text="必需" type="warn"/> | 文本内容 | String / AnnotatedString |
| modifier <Badge text="非必需" type="warn"/> | 修饰符 | Modifier |
| style <Badge text="非必需" type="warn"/> | 文本样式 | TextStyle |
| onTextLayout <Badge text="非必需" type="warn"/> | 文本布局完成回调 | ((TextLayoutResult) -> Unit)? |
| overflow <Badge text="非必需" type="warn"/> | 溢出处理方式 | TextOverflow |
| softWrap <Badge text="非必需" type="warn"/> | 是否自动换行 | Boolean |
| maxLines <Badge text="非必需" type="warn"/> | 最大行数 | Int |
| minLines <Badge text="非必需" type="warn"/> | 最小行数 | Int |
| color <Badge text="非必需" type="warn"/> | 覆盖 `style` 中的颜色 | ColorProducer? |

`AnnotatedString` 重载额外支持 `inlineContent` 参数，用于在文本中插入行内组合内容。

**示例**

```kotlin
BasicText(
    text = "基础文本",
    style = TextStyle(fontSize = 16.sp, color = Color.Black),
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

## BasicTextField

基于 `TextFieldState` 的基础输入框。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| state <Badge text="必需" type="warn"/> | 输入框状态 | TextFieldState |
| modifier <Badge text="非必需" type="warn"/> | 修饰符 | Modifier |
| enabled <Badge text="非必需" type="warn"/> | 是否可编辑 | Boolean |
| readOnly <Badge text="非必需" type="warn"/> | 是否只读 | Boolean |
| textStyle <Badge text="非必需" type="warn"/> | 文本样式 | TextStyle |
| keyboardOptions <Badge text="非必需" type="warn"/> | 键盘配置 | KeyboardOptions |
| keyboardActions <Badge text="非必需" type="warn"/> | IME 动作回调 | KeyboardActions |
| singleLine <Badge text="非必需" type="warn"/> | 是否单行 | Boolean |
| maxLines / minLines <Badge text="非必需" type="warn"/> | 最大/最小行数 | Int |
| inputTransformation <Badge text="非必需" type="warn"/> | 输入内容的转换与校验 | InputTransformation? |
| outputTransformation <Badge text="非必需" type="warn"/> | 展示内容的转换 | OutputTransformation? |
| onTextLayout <Badge text="非必需" type="warn"/> | 文本布局完成回调 | (TextLayoutResult) -> Unit |
| interactionSource <Badge text="非必需" type="warn"/> | 交互源 | MutableInteractionSource? |
| cursorBrush <Badge text="非必需" type="warn"/> | 光标画刷 | Brush |
| decorationBox <Badge text="非必需" type="warn"/> | 装饰容器，包裹内部输入框 | @Composable (innerTextField: @Composable () -> Unit) -> Unit |

**示例**

```kotlin
val state = rememberTextFieldState()

BasicTextField(
    state = state,
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    singleLine = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
)
```

## TextFieldState

输入框状态，承载文本、选区与组合态。

| 成员 | 说明 |
| -- | -- |
| `text` / `selection` / `composition` | 当前文本、选区和组合态 |
| `edit { }` | **推荐的写入入口**，在 `TextFieldBuffer` 中修改内容 |
| `clearText()` | 清空文本 |
| `setTextAndPlaceCursorAtEnd(text)` | 设置文本并将光标置于末尾 |
| `setTextAndSelect(text, selection, composition)` | 整体写入文本与选区 |

创建方式：

```kotlin
val state = rememberTextFieldState(initialText = "", initialSelection = TextRange(0))
```

::::warning 写入收敛约定
`TextFieldState` 的所有变更最终都收敛到 `setTextAndSelect`，它会将 `selection` 与 `composition` 约束到文本长度范围内。业务侧写入应走 `edit { }`，`setTextAndSelect` 仅在受控场景下调用。
::::

## KeyboardOptions

键盘与 IME 配置。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| capitalization <Badge text="非必需" type="warn"/> | 首字母大写策略 | KeyboardCapitalization |
| autoCorrectEnabled <Badge text="非必需" type="warn"/> | 是否启用自动纠错 | Boolean? |
| keyboardType <Badge text="非必需" type="warn"/> | 键盘类型 | KeyboardType |
| imeAction <Badge text="非必需" type="warn"/> | 回车键动作 | ImeAction |
| platformImeOptions <Badge text="非必需" type="warn"/> | 平台特定的 IME 配置 | PlatformImeOptions? |
| showKeyboardOnFocus <Badge text="非必需" type="warn"/> | 获焦时是否弹出键盘，null 表示弹出 | Boolean? |
| hintLocales <Badge text="非必需" type="warn"/> | 期望切换到的语言提示 | LocaleList? |

```kotlin
KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
```

::::warning 废弃构造
不带 `autoCorrectEnabled` 参数的旧构造已标记废弃，请使用新构造。
::::

## 相关 Modifier

文本输入相关的能力通过 Modifier 提供，详见[核心组件](./core-components.md)中的差异化章节：

| Modifier | 说明 |
| -- | -- |
| `Modifier.maxLength` / `onLimitChange` | 输入长度限制与超限回调 |
| `Modifier.placeHolder` / `placeholderColor` | 占位文案与颜色 |
| `Modifier.autoHideKeyboardOnImeAction` | 触发 IME 动作后自动收起键盘 |
| `Modifier.keyboardHeightChange` | 键盘高度变化回调 |
