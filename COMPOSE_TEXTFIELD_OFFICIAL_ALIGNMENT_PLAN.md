# Compose DSL TextField 官方对齐改造方案

创建日期：2026-05-07

## 1. 目标

将 KuiklyUI Compose DSL 的 TextField 从当前“字符串驱动输入框”升级为与 Jetpack Compose 官方 TextField 架构一致的“状态驱动文本编辑系统”。

核心目标：

1. 支持 `TextFieldValue(text, selection, composition)` 完整双向同步。
2. 支持官方推荐的 `TextFieldState` 状态驱动 API。
3. 支持 `edit { insert / replace / delete / append }` 事务式编辑。
4. 支持 `InputTransformation` 输入过滤。
5. 支持 `OutputTransformation` 显示格式化。
6. 支持 selection / cursor / composition 跨端一致语义。
7. 支持业务在光标处插入文本，不再依赖 `insertTextFnRef`。
8. 支持自定义表情输入：`state.edit { insert(selection.start, "[微笑]") }`，渲染层通过输出转换或平台富文本能力显示图片。
9. 保持现有 `value + onValueChange` API 兼容。

## 2. 官方 Compose 对齐基准

官方 TextField 现有两套 API：

### 2.1 旧 API：`TextFieldValue`

```kotlin
var value by remember { mutableStateOf(TextFieldValue("hello")) }

BasicTextField(
    value = value,
    onValueChange = { value = it }
)

val pos = value.selection.start
value = TextFieldValue(
    text = value.text.substring(0, pos) + "[微笑]" + value.text.substring(pos),
    selection = TextRange(pos + "[微笑]".length)
)
```

对齐要求：

1. `value.text` 下发到原生。
2. `value.selection` 下发到原生。
3. 原生输入变化回传 `text + selection + composition`。
4. 用户点击移动光标时也回传 selection。
5. `onValueChange` 返回完整 `TextFieldValue`，不是只返回 text。

### 2.2 新 API：`TextFieldState`

```kotlin
val state = rememberTextFieldState()

BasicTextField(
    state = state,
    inputTransformation = InputTransformation.maxLength(100),
    outputTransformation = OutputTransformation { /* display only */ }
)

state.edit {
    insert(selection.start, "[微笑]")
}
```

对齐要求：

1. `TextFieldState` 保存真实文本、selection、composition。
2. 所有程序化编辑统一走 `edit {}`。
3. 用户输入先进入 `TextFieldBuffer`。
4. `InputTransformation` 在提交 state 前执行。
5. `OutputTransformation` 只影响展示，不污染真实文本。
6. selection 与 output transformation 后的显示位置自动映射。

## 3. 当前 KuiklyUI 差距

### 3.1 Compose 层

当前 `CoreTextField` 只下发 `value.text`：

```kotlin
getViewAttr().text(value.text)
```

当前 `textDidChange` 只回传 text：

```kotlin
onValueChange(TextFieldValue(it.text))
```

缺失：

1. selection 下发。
2. selection 变化回调。
3. composition 回调。
4. `TextFieldState`。
5. `TextFieldBuffer`。
6. `InputTransformation`。
7. `OutputTransformation`。
8. 官方 `edit {}` 入口。

### 3.2 Core 层

已有能力：

1. `cursorIndex(callback)`。
2. `setCursorIndex(index)`。
3. `textDidChange { text, length }`。

缺失：

1. selection range，不只是 cursor index。
2. composition range。
3. selection change event。
4. 统一 `setTextInputState`。
5. 统一 `getTextInputState`。
6. 事务式 native edit method。

### 3.3 Render 层

iOS `UITextView` / Android `EditText` / OHOS `TextArea` 都具备原生 selection 能力，但 Kuikly Render 当前没有统一抽象。

缺失：

1. `selectionStart` / `selectionEnd` prop 或 method。
2. `onSelectionChange` event。
3. `markedTextRange` / composition range event。
4. 批量设置 text + selection 的原子方法。
5. 输出转换后的 offset mapping。

### 3.4 最新自定义表情 commit 基线

最新 commit `4d84f915 feat: compose TextField support custom emoji` 已经落地一条“短码保留 + 平台富文本显示”的过渡链路，本方案必须复用，不应推倒重做。

已落地能力：

| 层级 | 文件 | 能力 |
|---|---|---|
| Compose DSL | `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/extension/ModifierSetProp.kt` | 新增 `Modifier.textPostProcessor(processor)`，可向 Text / TextField 下发处理器名称 |
| Kuikly DSL | `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/InputView.kt` | `InputAttr.textPostProcessor(processor)`，自研 DSL `Input` 可声明处理器 |
| Android Render | `core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/component/KRTextFieldView.kt` | `EditText` 支持 `textPostProcessor`，`afterTextChanged` 中实时刷新 `ImageSpan` |
| Android Adapter | `androidApp/src/main/java/com/tencent/kuikly/android/demo/adapter/KRTextPostProcessorAdapter.kt` | `[smile]` / `[heart]` / `[thumbup]` / `[star]` / `[fire]` → drawable `ImageSpan` |
| Compose Demo | `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextFieldEmojiDemo.kt` | `TextField + Modifier.textPostProcessor("input")` 表情预览 |
| Kuikly DSL Demo | `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/EmojiTextInputDemo.kt` | `Input.attr.textPostProcessor("input")` 表情预览 |
| Docs | `docs/DevGuide/text-post-processor-guide.md` | 文本后置处理器实践指南 |

现有实现特征：

1. 真实文本仍保留 shortcode，例如 `[smile]`。
2. Android 输入框通过 `ImageSpan` 覆盖显示，不替换底层字符。
3. `textDidChange` 回调仍返回真实文本。
4. 表情按钮当前是 `text += shortCode`，只能追加到末尾，不能插入当前光标。
5. 当前没有 selection 双向同步，Compose 层仍无法知道用户点击后的真实光标位置。
6. 当前没有 `TextFieldState` / `TextFieldBuffer` / `InputTransformation` / `OutputTransformation`。
7. 当前 Android 输入框已具备显示链路；iOS / OHOS / Web 仍需补齐同等输入框链路。

对本方案的影响：

1. `textPostProcessor` 不废弃为“立即删除”，改为 `OutputTransformation` 的平台渲染桥接能力。
2. 自定义表情近期目标不是“重新发明表情协议”，而是让现有 shortcode + span/attachment 链路获得 selection 与状态驱动能力。
3. `OutputTransformation` 第一阶段可包装现有 `textPostProcessor`：业务写 `EmojiOutputTransformation("input")`，内部继续下发 `textPostProcessor`。
4. `TextFieldValue.selection` / `TextFieldState.edit` 落地后，表情按钮从“追加到末尾”升级为“替换当前选区或插入当前光标”。
5. Android Render 的 `applyEmojiSpans` 可作为 `OutputTransformation` Android 平台实现的短期复用点。
6. iOS 可复用已有 `KRTextAttachmentStringProtocol` / `p_outputText` / cursor mapping 思路，实现与 Android 等价的 shortcode 保留模型。

## 4. 总体架构

目标架构：

```text
业务代码
  │
  ├─ BasicTextField(value = TextFieldValue, onValueChange = ...)
  │
  └─ BasicTextField(state = TextFieldState, inputTransformation, outputTransformation)
        │
        ▼
Compose Text Input Core
  ├─ TextFieldState
  ├─ TextFieldBuffer
  ├─ EditProcessor
  ├─ InputTransformation pipeline
  ├─ OutputTransformation pipeline
  ├─ OffsetMappingCalculator
  └─ CoreTextField
        │
        ▼
Core Text Editing View
  ├─ TextInputState(text, selection, composition)
  ├─ setTextInputState(state)
  ├─ getTextInputState(callback)
  ├─ editText(command)
  ├─ onTextInputStateChange
  └─ onSelectionChange
        │
        ▼
Render Native Input
  ├─ iOS: UITextView / UITextField
  ├─ Android: EditText
  ├─ OHOS: TextInput / TextArea
  ├─ Web: input / textarea
  └─ MiniApp: input / textarea
```

## 5. API 设计

### 5.1 保留旧 API

```kotlin
@Composable
fun BasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() }
)
```

兼容要求：

1. `onValueChange` 返回完整 `TextFieldValue`。
2. selection 改变必须触发 `onValueChange` 或等价内部同步。
3. 程序设置 `value.selection` 必须更新原生光标。
4. composition 字段优先透传；平台不支持时置空。

### 5.2 新增官方推荐 API

```kotlin
@Composable
fun BasicTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorator: TextFieldDecorator? = null
)
```

### 5.3 `TextFieldState`

```kotlin
@Stable
class TextFieldState internal constructor(
    initialText: String,
    initialSelection: TextRange,
) {
    val text: CharSequence
    var selection: TextRange
        internal set
    var composition: TextRange?
        internal set

    fun edit(block: TextFieldBuffer.() -> Unit)
    fun setTextAndPlaceCursorAtEnd(text: String)
    fun clearText()
}

@Composable
fun rememberTextFieldState(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length)
): TextFieldState
```

语义：

1. `text` 是真实值。
2. `selection` 是真实文本坐标系。
3. `composition` 是输入法组合区间。
4. `initialText` 只在首次初始化时生效。
5. 初始化后修改文本必须走 `edit {}` 或工具方法。

### 5.4 `TextFieldBuffer`

```kotlin
class TextFieldBuffer internal constructor(
    initialText: String,
    initialSelection: TextRange,
    initialComposition: TextRange?
) : CharSequence {
    var selection: TextRange
    var composition: TextRange?

    fun append(text: String)
    fun insert(index: Int, text: String)
    fun replace(start: Int, end: Int, text: String)
    fun delete(start: Int, end: Int)
    fun selectAll()
    fun placeCursorAtEnd()
    fun revertAllChanges()
    fun asCharSequence(): CharSequence
}
```

### 5.5 `InputTransformation`

```kotlin
fun interface InputTransformation {
    fun TextFieldBuffer.transformInput()

    fun then(next: InputTransformation): InputTransformation

    companion object {
        fun maxLength(maxLength: Int): InputTransformation
    }
}
```

执行时机：

```text
原生输入事件 → TextFieldBuffer → InputTransformation → TextFieldState → Render
```

约束：

1. 只能修改 `TextFieldBuffer`。
2. 不允许直接修改外部 state。
3. 可调用 `revertAllChanges()` 拒绝本次输入。
4. 多个 transformation 按声明顺序执行。

### 5.6 `OutputTransformation`

```kotlin
fun interface OutputTransformation {
    fun TextFieldBuffer.transformOutput()
}
```

执行时机：

```text
TextFieldState.text → OutputTransformation → displayText / displaySpans → Render
```

约束：

1. 不修改 `TextFieldState.text`。
2. 只生成显示内容。
3. 框架负责真实坐标与显示坐标映射。
4. 替代一部分旧 `VisualTransformation` 场景。

### 5.7 自定义表情输入 API

业务用法对齐官方：

```kotlin
val state = rememberTextFieldState()

BasicTextField(
    state = state,
    outputTransformation = EmojiOutputTransformation("emoji")
)

EmojiPanel { code ->
    state.edit {
        insert(selection.start, code)
    }
}
```

不再需要：

```kotlin
Modifier.insertTextFnRef { ... }
```

## 6. 内部数据结构

### 6.1 `TextInputState`

`TextInputState` 不是官方 Compose API，仅作为 Kuikly Core / Render 通信与自研 DSL 绑定用的内部/框架层 payload。官方对外对齐对象仍是 `TextFieldValue`、`TextFieldState`、`TextFieldBuffer`、`TextRange`。

Core / Render 通信统一使用：

```kotlin
data class TextInputState(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    val compositionStart: Int = -1,
    val compositionEnd: Int = -1
)
```

JSON payload：

```json
{
  "text": "hello",
  "selectionStart": 5,
  "selectionEnd": 5,
  "compositionStart": -1,
  "compositionEnd": -1,
  "length": 5
}
```

### 6.2 坐标系约定

所有 Kotlin 层 API 使用真实文本坐标系：

```text
真实文本：你好[微笑]
selection: 6
显示文本：你好🖼
displaySelection: 3
```

Render 层需要维护：

1. raw index → display index。
2. display index → raw index。
3. attachment / span 删除回退到 raw text。
4. selection 回调必须转换成 raw index。

## 7. Compose 层改造

### 7.1 新增文件

```text
compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/TextFieldState.kt
compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/TextFieldBuffer.kt
compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/InputTransformation.kt
compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/OutputTransformation.kt
compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/TextInputState.kt
compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/TextEditProcessor.kt
compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/TextFieldLineLimits.kt
```

### 7.2 修改 `CoreTextField.kt`

改造点：

1. `value` API 内部适配成临时 `TextFieldStateAdapter`。
2. `state` API 直接绑定 `TextFieldState`。
3. 下发 native state 时使用 `setTextInputState`。
4. 接收 native event 时构造 `TextInputState`。
5. 经过 `InputTransformation` 后提交到 `TextFieldState`。
6. `TextFieldState` 变化后执行 `OutputTransformation`。
7. 通过 raw/display offset mapping 恢复 selection。
8. 只在 state 与 native 不一致时下发，避免输入回环。

### 7.3 旧 API 适配策略

`BasicTextField(value, onValueChange)` 内部创建 adapter：

```text
外部 TextFieldValue
  → CoreTextFieldAdapter
  → TextInputState
  → Native
```

Native 回调：

```text
Native TextInputState
  → TextFieldValue(text, selection, composition)
  → onValueChange
```

必须处理两种变更：

1. 用户输入导致 text 变。
2. 用户点击或拖拽导致 selection 变但 text 不变。

## 8. Core 层改造

### 8.1 新增统一方法

`AutoHeightTextAreaView` / `TextAreaView` / `InputView` 增加：

```kotlin
fun setTextInputState(state: TextInputState)
fun getTextInputState(callback: (TextInputState) -> Unit)
fun editText(command: TextEditCommand)
```

### 8.2 新增事件

`TextAreaEvent` / `InputEvent` 增加：

```kotlin
fun textInputStateChange(
    isSyncEdit: Boolean = true,
    handler: (TextInputState) -> Unit
)

fun selectionChange(
    handler: (TextRange) -> Unit
)
```

### 8.3 兼容旧事件

旧 `textDidChange { InputParams(text, length) }` 保留。

内部实现：

```text
textInputStateChange → 派生 textDidChange
```

## 9. Render 层改造

### 9.1 iOS

目标组件：

```text
core-render-ios/Extension/Components/KRTextAreaView.m
core-render-ios/Extension/Components/KRTextFieldView.m
```

新增 prop / method / event：

```objc
css_setTextInputState:
css_getTextInputState:
css_editText:
css_textInputStateChange
css_selectionChange
```

实现要点：

1. `UITextView.selectedRange` 映射到 raw selection。
2. `markedTextRange` 映射到 raw composition。
3. `textViewDidChange:` 回传完整 state。
4. `textViewDidChangeSelection:` 回传 selection。
5. 设置 text + selection 必须原子化，避免先改 text 后光标跳动。
6. attachment 场景使用已有 `p_outputText` / cursor mapping 能力。
7. `OutputTransformation` 后的 attachment 不污染 raw text。
8. `_ignoreTextDidChanged` 只屏蔽内部重入，不屏蔽最终 state 同步。

### 9.2 Android

目标组件：

```text
core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/component/KRTextFieldView.kt
```

实现要点：

1. `selectionStart` / `selectionEnd` 回传到 Kotlin。
2. `onSelectionChanged` 触发 `selectionChange`。
3. `afterTextChanged` 回传完整 `TextInputState`。
4. composition 可通过 `BaseInputConnection` / composing span 尽量获取；取不到时置 `-1`。
5. `setTextInputState` 使用 `Editable` 原子替换文本并设置 selection。
6. 避免 `setText` 导致 selection 强制到末尾。
7. 现有 `textPostProcessor` span 能力迁移为 `OutputTransformation` 的平台实现之一。

### 9.3 OHOS

目标组件：

```text
core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextFieldView.*
core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextAreaView.*
```

实现要点：

1. 复用 ArkUI text selection 属性。
2. 扩展 selection range，而不是只支持 start position。
3. 文本变化事件携带 selection。
4. composition 能力不足时先置空。

### 9.4 Web / MiniApp

目标组件：

```text
core-render-web/base/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/expand/components/KRTextFieldView.kt
core-render-web/base/src/jsMain/kotlin/com/tencent/kuikly/core/render/web/expand/components/KRTextAreaView.kt
```

实现要点：

1. `HTMLInputElement.selectionStart/selectionEnd`。
2. `compositionstart/compositionupdate/compositionend`。
3. `input` 事件回传 text + selection + composition。
4. 程序化设置 value 后恢复 selection。

## 10. OutputTransformation 与自定义表情

### 10.1 官方语义

`OutputTransformation` 只影响显示，不改变真实文本。

```text
state.text = "你好[微笑]"
display = "你好🖼"
```

### 10.2 Kuikly 实现策略

短期：

1. Compose 层定义 `OutputTransformation`。
2. 对纯文本格式化场景在 Kotlin 层生成 display text。
3. 对图片 attachment / span 场景，复用最新 commit 已落地的 `textPostProcessor` 链路。
4. `EmojiOutputTransformation("input")` 第一阶段只作为声明式包装，内部继续下发 `textPostProcessor = "input"`。
5. Android 继续复用 `KRTextFieldView.applyEmojiSpans`：短码字符保留在 `Editable` 中，显示层叠加 `ImageSpan`。
6. iOS 使用 `NSTextAttachment + KRTextAttachmentStringProtocol` 达成同等语义：显示图片，回调还原 shortcode。
7. Render 回调必须返回 raw text，不允许把图片占位符污染到 Kotlin state。
8. selection 回调必须把 display index 映射回 raw shortcode index。

中期：

1. `OutputTransformation` 统一 raw/display mapping。
2. `textPostProcessor` 成为 `OutputTransformation` 的平台显示后端之一。
3. `TextFieldValue.selection` 与 `TextFieldState.selection` 使用 raw text 坐标。
4. 删除 Android `ImageSpan` 或 iOS `NSTextAttachment` 时，必须删除完整 shortcode 区间。

长期：

1. 定义跨端 `InlineContent` / `Placeholder` 数据结构。
2. `OutputTransformation` 输出 display buffer + inline content spans。
3. 各端 render 统一消费 inline spans。
4. 业务侧不再直接依赖平台私有 `textPostProcessor`，但保留兼容入口。

### 10.3 表情插入方式

业务代码：

```kotlin
state.edit {
    insert(selection.start, "[微笑]")
}
```

框架流程：

```text
edit insert raw shortcode
  → state.text 更新
  → outputTransformation 生成 attachment display
  → render 显示图片
  → selection raw index 映射到图片后 display index
```

## 11. 输入流程

用户输入：

```text
Native input
  → native raw TextInputState
  → Core textInputStateChange
  → Compose TextEditProcessor
  → TextFieldBuffer
  → InputTransformation
  → TextFieldState commit
  → OutputTransformation
  → Native set display state
```

程序编辑：

```text
state.edit { insert(...) }
  → TextFieldBuffer
  → TextFieldState commit
  → OutputTransformation
  → Core setTextInputState
  → Native set text + selection
```

selection 变化：

```text
User tap / drag selection handle
  → Native selectionChange
  → raw selection mapping
  → TextFieldState.selection update
  → value API onValueChange(TextFieldValue(...))
```

composition 变化：

```text
IME composing
  → Native composition range
  → TextFieldState.composition update
  → 不执行 OutputTransformation 破坏 composing 区间
  → composition end 后再完整格式化
```

## 12. 兼容与迁移

### 12.1 保持兼容

保留：

1. `BasicTextField(value: String, onValueChange: (String) -> Unit)`。
2. `BasicTextField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit)`。
3. `Modifier.textPostProcessor(processor)`。
4. `cursorIndex` / `setCursorIndex`。
5. `textDidChange`。

### 12.2 推荐迁移

旧写法：

```kotlin
var value by remember { mutableStateOf(TextFieldValue("")) }
BasicTextField(value = value, onValueChange = { value = it })
```

新写法：

```kotlin
val state = rememberTextFieldState()
BasicTextField(state = state)
```

表情旧写法：

```kotlin
insertTextFn?.invoke("[微笑]")
```

表情新写法：

```kotlin
state.edit {
    insert(selection.start, "[微笑]")
}
```

### 12.3 预期使用案例

以下代码分为两类：

1. “已落地写法”：最新自定义表情 commit 当前已经支持。
2. “目标写法”：改造完成后的官方对齐 API。

#### 12.3.0 已落地写法：`textPostProcessor` 追加式表情输入

Compose DSL 当前写法：

```kotlin
@Composable
private fun CurrentComposeEmojiDemo() {
    var text by remember { mutableStateOf("") }

    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .textPostProcessor("input"),
            placeholder = { Text("输入内容或点击表情按钮") },
        )

        Row {
            listOf("[smile]", "[heart]", "[thumbup]").forEach { code ->
                Button(onClick = { text += code }) {
                    Text(code)
                }
            }
        }

        Text(
            text = text,
            modifier = Modifier.textPostProcessor("input"),
        )
    }
}
```

Kuikly DSL 当前写法：

```kotlin
Input {
    attr {
        text(ctx.inputText)
        placeholder("输入文字或点击下方表情按钮")
        textPostProcessor("input")
    }
    event {
        textDidChange { params ->
            ctx.inputText = params.text
        }
    }
}
```

当前限制：

1. 表情按钮只能 `text += code` 追加到末尾。
2. Compose 层拿不到用户点击后的光标位置。
3. 不能替换选区。
4. Android 已支持输入框实时 `ImageSpan`；iOS / OHOS / Web 输入框链路仍需补齐。
5. 这是后续 `OutputTransformation` 与 selection 双向同步的基线，不应回退。

#### 12.3.1 Compose DSL：官方新 API，表情输入

```kotlin
@Page("ComposeEmojiInputDemo")
class ComposeEmojiInputDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent { Content() }
    }

    @Composable
    private fun Content() {
        val state = rememberTextFieldState()

        Column(modifier = Modifier.padding(16.dp)) {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(1.dp, Color.Gray)
                    .padding(8.dp),
                inputTransformation = InputTransformation.maxLength(500),
                outputTransformation = EmojiOutputTransformation("emoji"),
                textStyle = TextStyle(fontSize = 16.sp),
            )

            Row(modifier = Modifier.padding(top = 12.dp)) {
                listOf("[微笑]", "[爱心]", "[点赞]").forEach { code ->
                    Button(onClick = {
                        state.edit {
                            replace(selection.start, selection.end, code)
                        }
                    }) {
                        Text(code)
                    }
                }
            }
        }
    }
}
```

预期行为：

1. `state.text` 保存真实文本，例如 `你好[微笑]`。
2. `OutputTransformation` 只影响显示，例如展示为 `你好🖼`。
3. `state.edit { replace(selection.start, selection.end, code) }` 在当前光标或选区插入表情占位符。
4. 用户点击移动光标后，`state.selection` 自动更新。
5. 删除图片时，真实文本删除完整 shortcode。

#### 12.3.2 Compose DSL：官方旧 API，`TextFieldValue` 双向 selection

```kotlin
@Composable
private fun TextFieldValueDemo() {
    var value by remember {
        mutableStateOf(TextFieldValue("hello", selection = TextRange(5)))
    }

    Column {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(1.dp, Color.Gray)
                .padding(8.dp),
        )

        Button(onClick = {
            val start = value.selection.start
            val end = value.selection.end
            val insertText = "[微笑]"
            val newText = value.text.replaceRange(start, end, insertText)
            val newCursor = start + insertText.length
            value = TextFieldValue(
                text = newText,
                selection = TextRange(newCursor),
            )
        }) {
            Text("插入表情")
        }
    }
}
```

预期行为：

1. `value.selection` 下发到原生光标。
2. 用户点击移动光标后，`onValueChange` 返回新的 `TextFieldValue.selection`。
3. 业务可用官方旧 API 的方式在光标处拼接文本。

#### 12.3.3 Compose DSL：输入过滤 + 展示格式化

```kotlin
@Composable
private fun PhoneInputDemo() {
    val state = rememberTextFieldState()

    BasicTextField(
        state = state,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        inputTransformation = InputTransformation.maxLength(11).then {
            if (!asCharSequence().all { it.isDigit() }) {
                revertAllChanges()
            }
        },
        outputTransformation = OutputTransformation {
            if (length > 3) insert(3, " ")
            if (length > 8) insert(8, " ")
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, Color.Gray)
            .padding(8.dp),
    )
}
```

预期行为：

1. `state.text` 只保存数字，例如 `13800138000`。
2. UI 显示格式化结果，例如 `138 0013 8000`。
3. 过滤逻辑不写在 `onValueChange`，避免异步状态回写导致跳变。

#### 12.3.4 Kuikly DSL：状态驱动 TextArea，表情输入

自研 Kuikly DSL 不直接暴露官方 Compose `TextFieldState`，但应提供同语义的 `TextInputState` 绑定能力。

```kotlin
@Page("KuiklyDslEmojiInputDemo")
internal class KuiklyDslEmojiInputDemo : BasePager() {
    private var inputState by observable(TextInputState(text = ""))

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                padding(16f)
                backgroundColor(Color.WHITE)
            }

            TextArea {
                attr {
                    height(120f)
                    border(1f, BorderStyle.SOLID, Color.GRAY)
                    borderRadius(8f)
                    padding(8f)
                    textInputState(ctx.inputState)
                    textPostProcessor("emoji")
                }
                event {
                    textInputStateChange(true) { state ->
                        ctx.inputState = state
                    }
                }
            }

            View {
                attr {
                    flexDirectionRow()
                    marginTop(12f)
                }

                listOf("[微笑]", "[爱心]", "[点赞]").forEach { code ->
                    Text {
                        attr {
                            text(code)
                            marginRight(12f)
                            padding(8f)
                            border(1f, BorderStyle.SOLID, Color.GRAY)
                            borderRadius(6f)
                        }
                        event {
                            click {
                                ctx.inputState = ctx.inputState.edit {
                                    replace(selection.start, selection.end, code)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

预期行为：

1. Kuikly DSL 业务状态保存 `TextInputState`。
2. `TextArea.attr.textInputState(state)` 原子下发 text + selection + composition。
3. `textInputStateChange` 回传完整 text + selection + composition。
4. 表情按钮不调用原生 `insertText`，而是修改 state 后由框架同步到原生。
5. 自研 DSL 与 Compose DSL 使用同一套 Core / Render 能力。

#### 12.3.5 Kuikly DSL：兼容旧 `textDidChange` 写法

```kotlin
@Page("KuiklyDslLegacyInputDemo")
internal class KuiklyDslLegacyInputDemo : BasePager() {
    private var text by observable("")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            TextArea {
                attr {
                    height(80f)
                    text(ctx.text)
                }
                event {
                    textDidChange {
                        ctx.text = it.text
                    }
                }
            }
        }
    }
}
```

兼容策略：

1. 旧 API 继续可用。
2. 旧 `textDidChange` 只关心 text，不暴露 selection。
3. 新需求优先使用 `TextInputState` / `textInputStateChange`。

#### 12.3.6 Kuikly DSL：程序设置光标与选区

```kotlin
private fun moveCursorToEnd() {
    inputState = inputState.copy(
        selectionStart = inputState.text.length,
        selectionEnd = inputState.text.length,
        compositionStart = -1,
        compositionEnd = -1,
    )
}

private fun selectAll() {
    inputState = inputState.copy(
        selectionStart = 0,
        selectionEnd = inputState.text.length,
        compositionStart = -1,
        compositionEnd = -1,
    )
}
```

预期行为：

1. 修改 `inputState.selectionStart/selectionEnd` 后，原生 selection 同步变化。
2. `selectionStart == selectionEnd` 表示光标。
3. `selectionStart != selectionEnd` 表示选区。
4. selection 坐标始终使用真实文本坐标，不使用富文本显示坐标。

## 13. 分阶段实施计划

### Phase 0：基于现有表情 commit 建立行为基线

交付：

1. 记录当前 `CoreTextField` 行为。
2. 以 `TextFieldEmojiDemo` 和 `EmojiTextInputDemo` 作为现有自定义表情基线 demo。
3. 补充 selection 观测 demo：点击文本中间、拖选、删除、粘贴、拼音输入。
4. 固化 Android 当前 `textPostProcessor + ImageSpan` 行为：raw text 保留 shortcode，显示层渲染图片。
5. 定义跨端 `TextInputState` payload。

验收：

1. 现有 `textPostProcessor("input")` 表情显示不回归。
2. `textDidChange` 继续返回 raw shortcode 文本。
3. 当前“表情按钮追加到末尾”的限制被记录为待解决问题。
4. 点击移动光标、删除、粘贴、拼音输入均有基线记录。

### Phase 1：旧 API 完整支持 `TextFieldValue`

交付：

1. `textDidChange` 回传 selection。
2. `selectionChange` event。
3. `CoreTextField` 下发 `value.selection`。
4. `onValueChange(TextFieldValue(text, selection, composition))`。

验收：

1. 业务修改 `TextFieldValue.selection` 可移动光标。
2. 用户点击移动光标后 Kotlin 可感知。
3. 在光标处拼接字符串方案可正常插入。

### Phase 2：Core `TextInputState`

交付：

1. 新增 `TextInputState`。
2. 新增 `setTextInputState`。
3. 新增 `getTextInputState`。
4. 新增 `textInputStateChange`。
5. 旧 `textDidChange` 由新事件派生。

验收：

1. text + selection 原子同步。
2. 不出现 set text 后光标跳末尾。
3. 旧 API 不破坏。

### Phase 3：`TextFieldState` / `TextFieldBuffer`

交付：

1. `rememberTextFieldState`。
2. `TextFieldState.edit {}`。
3. `TextFieldBuffer` 基础编辑操作。
4. `BasicTextField(state = ...)`。

验收：

1. `state.edit { insert(...) }` 在当前光标插入。
2. `setTextAndPlaceCursorAtEnd` 正常。
3. `clearText` 正常。
4. state API 与 value API 行为一致。

### Phase 4：`InputTransformation`

交付：

1. `InputTransformation` 接口。
2. `then` 链式组合。
3. `maxLength`。
4. `revertAllChanges`。
5. 数字过滤 demo。

验收：

1. 限长不依赖 render 层 maxLength。
2. 拼音输入期间不误删 composing text。
3. 粘贴大段文本可正确过滤。

### Phase 5：`OutputTransformation` 与现有 `textPostProcessor` 桥接

交付：

1. `OutputTransformation` 接口。
2. display buffer。
3. raw/display offset mapping。
4. 电话号码格式化 demo。
5. `EmojiOutputTransformation(processor)`，内部复用 `textPostProcessor`。
6. Android 复用现有 `applyEmojiSpans`，iOS 补齐 `NSTextAttachment` 输入框显示链路。
7. 将 `TextFieldEmojiDemo` 从 `Modifier.textPostProcessor("input")` 迁移为推荐写法，同时保留旧写法兼容示例。

验收：

1. state 保存 raw text，例如 `[smile]`。
2. UI 显示图片或 formatted text。
3. 光标位置映射正确。
4. 删除 display token 后 raw text 删除完整 shortcode。
5. 最新 commit 中的 Android 表情显示能力不回归。

### Phase 6：跨端补齐

优先级：

1. iOS TextArea。
2. Android EditText。
3. iOS TextField。
4. OHOS TextArea / TextField。
5. Web / MiniApp。

验收：

1. 同一 demo 五端行为一致。
2. selection / composition payload 一致。
3. 表情输入至少 iOS / Android 可用。

### Phase 7：文档与废弃策略

交付：

1. Compose TextField 官方对齐文档。
2. 迁移指南。
3. 表情输入最佳实践。
4. 标记不推荐 `insertTextFnRef`。
5. 标记 `textPostProcessor` 为过渡 API，推荐 `OutputTransformation`。

## 14. 测试矩阵

### 14.1 基础输入

| Case | 预期 |
|---|---|
| 输入英文 | text 与 selection 同步 |
| 输入中文拼音 | composition 不被破坏 |
| 输入 emoji unicode | 不切割 surrogate pair |
| 删除单字符 | selection 正确回退 |
| 选区替换 | start/end 范围被替换 |
| 粘贴文本 | transformation 生效 |

### 14.2 selection

| Case | 预期 |
|---|---|
| 点击文本中间 | Kotlin selection 更新 |
| 拖拽选区 | start/end 更新 |
| 程序设置 selection | 原生光标移动 |
| state.edit 后 selection | 按编辑操作更新 |
| output format 后 selection | raw/display 映射正确 |

### 14.3 transformation

| Case | 预期 |
|---|---|
| maxLength | 超长输入拒绝或截断 |
| digit only | 非数字输入 revert |
| phone output | raw text 无格式符 |
| emoji output | raw text 保留 `[微笑]` |
| 删除 emoji 图片 | raw text 删除完整 shortcode |

### 14.4 平台

| 平台 | 必测 |
|---|---|
| iOS TextArea | selection、composition、attachment |
| iOS TextField | selection、单行插入 |
| Android EditText | Spannable、selection、IME |
| OHOS | selection range |
| Web | composition event、selectionStart/End |
| MiniApp | input/textarea 能力降级 |

## 15. 风险与处理

### 15.1 composition 跨端能力不一致

处理：

1. iOS / Web 优先支持 composition range。
2. Android 尽量通过 composing span 获取。
3. OHOS / MiniApp 不支持时回传 `composition = null`。
4. transformation 在 composition 期间默认延迟执行破坏性格式化。

### 15.2 OutputTransformation offset mapping 复杂

处理：

1. Phase 5 先支持 insert-only output transformation。
2. 再支持 replace/delete。
3. 每次 buffer edit 记录 change list。
4. 根据 change list 自动生成 raw/display mapping。

### 15.3 原生 setText 导致循环回调

处理：

1. Native 设置期间使用 `_ignoreTextDidChanged` / `ignoreTextWatcher`。
2. 设置完成后主动发一次最终 state，或由 Compose state 驱动确认。
3. Compose 层比较 `lastNativeState` 与 `targetState`，避免重复下发。

### 15.4 旧 API 行为变化

处理：

1. 旧 `String` API 保持只关心 text。
2. 旧 `TextFieldValue` API 增强 selection，不破坏 text。
3. 新 state API 独立推出。
4. Demo 和文档引导新 API。

## 16. 不建议的方案

### 16.1 不建议继续推进 `insertTextFnRef`

原因：

1. 与官方 `TextFieldState.edit { insert(...) }` 不一致。
2. 只能解决插入文本，不能解决 selection 双向同步。
3. 后续还要追加 delete / replace / cursor / selection 等命令。
4. 容易形成平台命令式 API，而不是 Compose 状态驱动模型。

### 16.2 不建议只在 iOS Render 做表情替换

原因：

1. 不能解决 Compose 层 selection 缺失。
2. 不能跨端一致。
3. 不能覆盖官方 `OutputTransformation` 场景。
4. 业务仍需平台定制代码。

### 16.3 不建议只扩展 `TextFieldValue.selection`

原因：

1. 只能对齐旧 API。
2. 无法支持官方推荐的 `InputTransformation` / `OutputTransformation`。
3. 复杂输入仍会堆在 `onValueChange`。

## 17. 推荐优先级

最小闭环：

```text
Phase 1 TextFieldValue selection 双向同步
  → Phase 2 TextInputState
  → Phase 3 TextFieldState.edit
```

表情输入闭环：

```text
TextFieldState.edit insert shortcode
  → OutputTransformation / textPostProcessor display
  → raw/display selection mapping
```

最终官方对齐闭环：

```text
TextFieldState
  + TextFieldBuffer
  + InputTransformation
  + OutputTransformation
  + selection/composition 双向同步
  + BasicTextField / Material TextField 分层
```

## 18. 验收标准

1. `BasicTextField(value = TextFieldValue(...))` 与官方旧 API 行为一致。
2. `BasicTextField(state = rememberTextFieldState())` 与官方新 API 核心行为一致。
3. `state.edit { insert(selection.start, text) }` 可在当前光标插入。
4. 用户点击、拖选、输入、删除、粘贴都能同步 `selection`。
5. 拼音输入期间 `composition` 不被错误提交或格式化破坏。
6. `InputTransformation.maxLength()` 可跨端一致限长。
7. `OutputTransformation` 不污染 `state.text`。
8. 自定义表情输入不需要 `insertTextFnRef`。
9. iOS / Android 至少完成全链路验证。
10. 旧 API demo 不回归。
