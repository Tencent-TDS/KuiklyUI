# 文本后置处理器实践指南

本文介绍如何在 Kuikly 中使用文本后置处理器（Text Post Processor），实现输入框/文本组件的自定义表情、链接高亮、文本格式化等效果。

:::tip 适用场景
- **自定义表情**：将 `[smile]` 等短码实时渲染为表情图片
- **链接高亮**：自动识别 URL 并添加下划线/点击事件
- **文本格式化**：手机号分段显示（`138-1234-5678`）、金额千分位分隔等
- **敏感信息脱敏**：密码掩码、身份证号部分隐藏
:::

## 整体流程

使用文本后置处理器需要三步：

```
1. DSL 声明 processor 名称  →  2. 平台实现处理器适配器  →  3. 注册适配器
```

:::warning iOS 平台限制
- **单行输入框（`Input` / `UITextField`）不支持 `NSTextAttachment` 图片渲染**，因此自定义表情图片预览在 iOS 单行模式下不可用。如需表情预览，请使用多行输入框（`TextArea` / `UITextView`）。
- 非图片类的文本后处理（如文本掩码、格式化）不受此限制影响。
:::

## DSL 使用方式

### 自研 DSL

在 `Input` 或 `Text` 组件上通过 `textPostProcessor()` 属性声明处理器名称：

```kotlin{14,22}
@Page("emoji_demo")
internal class EmojiDemoPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            // Input with post-processor
            Input {
                attr {
                    size(300f, 48f)
                    placeholder("输入 [smile] 试试")
                    textPostProcessor("input")
                }
            }

            // Text with post-processor (for preview)
            Text {
                attr {
                    text("Hello [smile] World")
                    fontSize(16f)
                    textPostProcessor("emoji")
                }
            }
        }
    }
}
```

### Compose DSL

Compose DSL 使用 `textPostProcessor()` Modifier：

```kotlin{12}
@Composable
fun EmojiInputDemo() {
    var text by remember { mutableStateOf("") }

    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .textPostProcessor("input"),
            placeholder = { Text("输入表情短码") }
        )

        // Preview with processor
        Text(
            text = text,
            modifier = Modifier.textPostProcessor("emoji")
        )
    }
}
```

:::tip processor 名称说明
- `"emoji"`、`"input"` 是示例名称，**名称由业务自定义**
- 同一页面中不同组件可以使用不同 processor 名称，在适配器中按名称路由到不同处理逻辑
- 未在适配器中实现的 processor 名称会透传原文本，不会报错
:::

## Android 适配器实现

### 1. 创建适配器类

新建一个类实现 `IKRTextPostProcessorAdapter` 接口，在 `onTextPostProcess` 方法中处理文本：

```kotlin
package com.example.myapp.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import com.tencent.kuikly.core.render.android.adapter.IKRTextPostProcessorAdapter
import com.tencent.kuikly.core.render.android.adapter.TextPostProcessorInput
import com.tencent.kuikly.core.render.android.adapter.TextPostProcessorOutput
import com.tencent.kuikly.core.render.android.css.ktx.toPxF

/**
 * 文本后置处理器适配器示例
 * 支持：emoji 短码替换、链接高亮
 */
class MyTextPostProcessorAdapter(context: Context) : IKRTextPostProcessorAdapter {

    private val appContext: Context = context.applicationContext

    companion object {
        // 短码 → drawable 资源映射
        private val EMOJI_MAP = mapOf(
            "[smile]" to R.drawable.emoji_smile,
            "[heart]" to R.drawable.emoji_heart,
            "[thumbup]" to R.drawable.emoji_thumbup,
        )

        // 匹配 [xxx] 格式的短码
        private val EMOJI_PATTERN = "\\[([^\\]]+)\\]".toRegex()

        // 匹配 URL
        private val URL_PATTERN = "https?://[^\\s]+".toRegex()
    }

    override fun onTextPostProcess(
        kuiklyRenderContext: com.tencent.kuikly.core.render.android.IKuiklyRenderContext?,
        inputParams: TextPostProcessorInput
    ): TextPostProcessorOutput {
        return when (inputParams.processor) {
            "emoji", "input" -> processEmoji(inputParams)
            "link" -> processLink(inputParams)
            else -> TextPostProcessorOutput(inputParams.sourceText)
        }
    }

    /**
     * Emoji 处理：将 [smile] 等短码替换为 ImageSpan 图片
     */
    private fun processEmoji(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        val sourceText = inputParams.sourceText?.toString() ?: return TextPostProcessorOutput("")

        val matches = EMOJI_PATTERN.findAll(sourceText).toList()
        if (matches.isEmpty()) {
            return TextPostProcessorOutput(SpannableStringBuilder(sourceText))
        }

        // 表情大小基于当前字体大小计算
        val fontSize = inputParams.textProps.fontSize.toPxF()
        val emojiSize = fontSize.toInt().coerceAtLeast(48)

        val spannable = SpannableStringBuilder(sourceText)

        for (match in matches) {
            val shortcode = match.value
            val drawableRes = EMOJI_MAP[shortcode]
            if (drawableRes != null) {
                val drawable: Drawable? = ContextCompat.getDrawable(appContext, drawableRes)
                drawable?.setBounds(0, 0, emojiSize, emojiSize)
                if (drawable != null) {
                    spannable.setSpan(
                        ImageSpan(drawable, DynamicDrawableSpan.ALIGN_CENTER),
                        match.range.first,
                        match.range.last + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        return TextPostProcessorOutput(spannable)
    }

    /**
     * 链接处理：识别 URL 并添加下划线（示例）
     */
    private fun processLink(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        val sourceText = inputParams.sourceText?.toString() ?: return TextPostProcessorOutput("")
        // ... 链接处理逻辑
        return TextPostProcessorOutput(sourceText)
    }

    // 兼容旧接口（保留即可）
    @Deprecated("Use onTextPostProcess(kuiklyRenderContext, inputParams) instead")
    override fun onTextPostProcess(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        return onTextPostProcess(null, inputParams)
    }
}
```

### 2. 注册适配器

在 Application 初始化时，通过 `KuiklyRenderAdapterManager` 注册：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 注册文本后置处理器适配器
        KuiklyRenderAdapterManager.krTextPostProcessorAdapter = MyTextPostProcessorAdapter(this)
    }
}
```

:::warning 重要提醒
必须在 **Application.onCreate()** 中注册，确保在页面创建前完成初始化。
:::

## iOS 适配器实现

iOS 侧通过实现 `KuiklyRenderComponentExpandHandler` 的 `hr_customTextWithAttributedString:textPostProcessor:` 方法来处理文本后置变换。

### 1. 实现处理器方法

在 `KuiklyRenderComponentExpandHandler` 中实现 `hr_customTextWithAttributedString:textPostProcessor:`：

```objc
- (NSMutableAttributedString *)hr_customTextWithAttributedString:(NSAttributedString *)attributedString
                                               textPostProcessor:(NSString *)textPostProcessor {
    // 按 processor 名称路由
    if (![textPostProcessor isEqualToString:@"KRTextAreaView"] &&
        ![textPostProcessor isEqualToString:@"input"] &&
        ![textPostProcessor isEqualToString:@"emoji"]) {
        return [attributedString mutableCopy]; // 未匹配的 processor 透传
    }
    
    NSString *sourceString = attributedString.string;
    if (sourceString.length == 0) {
        return [attributedString mutableCopy];
    }
    
    // 匹配 [xxx] 短码
    NSArray<NSTextCheckingResult *> *matches = [EMOJI_REGEX matchesInString:sourceString
                                                                     options:0
                                                                       range:NSMakeRange(0, sourceString.length)];
    if (matches.count == 0) {
        return [attributedString mutableCopy];
    }
    
    NSMutableAttributedString *result = [attributedString mutableCopy];
    NSInteger offset = 0;
    UIFont *font = [attributedString attribute:NSFontAttributeName atIndex:0 effectiveRange:NULL];
    if (!font) font = [UIFont systemFontOfSize:16];
    CGFloat emojiSize = font.pointSize * 1.2;
    
    for (NSTextCheckingResult *match in matches) {
        NSRange matchRange = [match range];
        NSRange adjustedRange = NSMakeRange(matchRange.location + offset, matchRange.length);
        NSString *shortcode = [sourceString substringWithRange:matchRange];
        NSString *imageName = EMOJI_IMAGE_MAP[shortcode]; // 如 @"emoji_smile"
        if (!imageName) continue;
        
        UIImage *emojiImage = [UIImage imageNamed:imageName];
        if (!emojiImage) continue;
        
        // 创建 NSTextAttachment 替换短码
        NSTextAttachment *attachment = [[NSTextAttachment alloc] init];
        attachment.image = emojiImage;
        attachment.bounds = CGRectMake(0, font.descender * 0.5, emojiSize, emojiSize);
        
        NSMutableAttributedString *attachmentStr = [[NSAttributedString attributedStringWithAttachment:attachment] mutableCopy];
        [attachmentStr addAttribute:NSFontAttributeName value:font range:NSMakeRange(0, attachmentStr.length)];
        [result replaceCharactersInRange:adjustedRange withAttributedString:attachmentStr];
        offset += (1 - matchRange.length); // 附件占1字符，短码占N字符
    }
    
    return result;
}
```

### 2. processor 名称说明

iOS 侧 processor 名称的来源：
- **类名**（如 `KRTextAreaView`）：`setCss_values` 渲染路径默认使用 `NSStringFromClass([self class])`
- **业务自定义**（如 `input`、`emoji`）：Kotlin 侧通过 `textPostProcessor("input")` 设置

### 3. 已知限制

| 限制 | 说明 |
|------|------|
| `UITextField` 不渲染 `NSTextAttachment` 图片 | iOS 单行输入框（`Input` 组件）无法显示表情图片，只有 `UITextView`（`TextArea` 组件）支持 |
| 光标跳动 | `UITextView` + `NSTextAttachment` 点击时可能触发两次 `selectionChange`，导致光标短暂跳回（iOS 原生问题） |
| 非图片后处理不受限 | 文本掩码、格式化等不依赖 `NSTextAttachment` 的后处理器在单行模式下正常工作 |

## 关键实现细节

### 为什么返回 SpannableStringBuilder？

`Input`（EditText）组件需要接收 `Editable` 类型的文本才能正常编辑。`SpannableStringBuilder` 同时实现了 `Editable` 和 `Spannable` 接口，因此既能保留 ImageSpan 等样式，又能支持光标定位和文本编辑。

| 返回类型 | 适用场景 | 说明 |
|:----|:-------|:--|
| `SpannableStringBuilder` | `Input` 组件 | 支持编辑 + 富文本样式 |
| `SpannableString` | `Text` 组件展示 | 只读展示场景 |
| 普通 `String` |  passthrough | 不做任何处理时直接返回原文本 |

### 表情尺寸动态计算

建议基于当前字体大小计算表情尺寸，保证不同字号下表情比例协调：

```kotlin
val fontSize = inputParams.textProps.fontSize.toPxF()
val emojiSize = fontSize.toInt().coerceAtLeast(48) // 最小 48px，防止过小
```

### 保留原始字符

`ImageSpan` 设置后会覆盖对应区域的文本渲染，但底层字符仍然保留。这意味着：

- 用户看到的：😊（图片）
- 实际存储的：`[smile]`
- 业务回调获取的：`[smile]`（便于后端存储和跨端同步）

### 多处理器路由

当业务需要多种处理方式时，通过 `when` 分支按 `inputParams.processor` 路由：

```kotlin
override fun onTextPostProcess(
    kuiklyRenderContext: IKuiklyRenderContext?,
    inputParams: TextPostProcessorInput
): TextPostProcessorOutput {
    return when (inputParams.processor) {
        "emoji", "input" -> processEmoji(inputParams)
        "link" -> processLink(inputParams)
        "format" -> processFormat(inputParams)
        else -> TextPostProcessorOutput(inputParams.sourceText) // 未知类型透传
    }
}
```

## 完整示例：自定义表情输入

下面是一个完整的自定义表情输入 Demo，包含表情面板和实时预览：

```kotlin
@Page("EmojiTextInputDemo")
internal class EmojiTextInputDemo : Pager() {

    private var inputText: String by observable("")
    private val emojiShortcodes = listOf("[smile]", "[heart]", "[thumbup]")
    private val emojiLabels = listOf("😊", "❤️", "👍")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    backgroundColor(Color(0xFFF5F5F5))
                    flexDirectionColumn()
                }

                // 输入框
                Input {
                    attr {
                        text(ctx.inputText)
                        placeholder("输入文字或点击表情")
                        textPostProcessor("input")
                    }
                    event {
                        textDidChange { params ->
                            ctx.inputText = params.text
                        }
                    }
                }

                // 预览（使用 Text 组件）
                Text {
                    attr {
                        text("预览：${ctx.inputText}")
                        textPostProcessor("emoji")
                    }
                }

                // 表情按钮
                View {
                    attr { flexDirectionRow() }
                    for (i in ctx.emojiShortcodes.indices) {
                        View {
                            event {
                                click {
                                    ctx.inputText += ctx.emojiShortcodes[i]
                                }
                            }
                            Text {
                                attr {
                                    text(ctx.emojiLabels[i])
                                    fontSize(24f)
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

## 常见问题

### Q: 设置 textPostProcessor 后输入框不显示表情？

A: 检查以下几点：
1. **适配器是否已注册** — 确认在 Application.onCreate() 中设置了 `KuiklyRenderAdapterManager.krTextPostProcessorAdapter`
2. **processor 名称是否匹配** — DSL 中 `textPostProcessor("xxx")` 的名称要与适配器 `when` 分支中的名称一致
3. **drawable 资源是否存在** — 检查 `R.drawable.xxx` 是否引用正确

### Q: 表情显示位置偏移/大小不对？

A: 确认 `drawable.setBounds(0, 0, size, size)` 已正确设置，并且 `ImageSpan` 使用了 `DynamicDrawableSpan.ALIGN_CENTER` 对齐方式。

### Q: 为什么 EditText 需要返回 Editable？

A: EditText 内部使用 `Editable` 接口管理文本和 Span。如果返回普通 String，EditText 会丢失 Span 信息，导致表情图片无法显示。`SpannableStringBuilder` 是 `Editable` 的子类，因此是最佳选择。

### Q: 可以支持网络图片作为表情吗？

A: 可以。在适配器中拿到网络图片 URL 后，使用 Glide/Picasso 等库加载为 `Drawable`，再创建 `ImageSpan`。注意需要在图片加载完成后刷新文本。

```kotlin
// 异步加载网络图片表情示例
glide.load(url).into(object : CustomTarget<Drawable>() {
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        resource.setBounds(0, 0, emojiSize, emojiSize)
        spannable.setSpan(ImageSpan(resource), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // 通知 UI 刷新
        editText.text = spannable
    }
    // ...
})
```

### Q: iOS 上表情图片不显示？

A: 检查以下几点：
1. **是否使用了单行输入框** — iOS 的 `UITextField` 不支持 `NSTextAttachment` 图片渲染，表情图片只在 `UITextView`（即 `TextArea` 组件）中显示
2. **图片资源是否在 Asset Catalog 中** — iOS 侧需要将表情 PNG 添加到 `Assets.xcassets`
3. **processor 名称是否在 expandHandler 中匹配** — 确认 `hr_customTextWithAttributedString:textPostProcessor:` 中有对应的名称路由

### Q: iOS 上点击带表情的文本时光标会跳一下？

A: 这是 `UITextView` + `NSTextAttachment` 的 iOS 原生问题。点击时 `UITextView` 会先设置一个初始 selection，然后约 50ms 后修正到实际位置，导致光标短暂跳动。目前无法在 App 层面修复，长期方案需要避免在 `UITextView` 中使用 `NSTextAttachment`。

## 迁移：从追加式输入到光标插入

旧写法通常直接拼接短码，只能追加到末尾：

```kotlin
EmojiGrid { shortcode ->
    text += shortcode
}
```

新的 Compose DSL 推荐使用 `TextFieldState.edit {}`，框架会根据当前 selection 替换选区或在光标处插入：

```kotlin
val state = rememberTextFieldState()

BasicTextField(
    state = state,
    outputTransformation = TextPostProcessorOutputTransformation("input"),
)

EmojiGrid { shortcode ->
    state.edit {
        replace(selection.start, selection.end, shortcode)
    }
}
```

自研 DSL 可使用 `TextInputState` 保存 raw text 与 selection：

```kotlin
private var inputState by observable(TextInputState(text = ""))

TextArea {  // 注意：iOS 上请使用 TextArea，Input 不支持表情图片渲染
    attr {
        textPostProcessor("input")
        textInputState { inputState }  // lambda 形式支持响应式绑定
    }
    event {
        textInputStateChange { state ->
            inputState = state
        }
        selectionChange { state ->
            inputState = state
        }
    }
}
```

> `textPostProcessor` 仍然保留；`TextPostProcessorOutputTransformation` 是 Compose DSL 对现有渲染链路的声明式包装。

## TextInputState 数据结构

`TextInputState` 是用于 Core/Render 通信的跨层文本输入状态数据类，用于在 DSL 层精确控制输入框的文本内容、光标位置和组合输入状态。

### 字段说明

| 字段  | 类型 | 说明 |
|:----|:-----|:-----|
| `text` | `String` | 当前输入的文本内容（raw text，如表情短码 `[smile]`） |
| `selectionStart` | `Int` | 选区起始位置（光标位置或选区开始） |
| `selectionEnd` | `Int` | 选区结束位置（与 selectionStart 相等表示无选区） |
| `compositionStart` | `Int` | 组合输入起始位置（如 iOS 拼音输入时高亮的拼音范围） |
| `compositionEnd` | `Int` | 组合输入结束位置 |
| `length` | `Int?` | 文本长度（可选，与 `maxTextLength` 的计算方式一致） |

### 常量

| 常量  | 值 | 说明 |
|:----|:-----|:-----|
| `NO_COMPOSITION` | `-1` | 表示当前没有组合输入（如拼音输入时的拼音高亮状态） |

### 构造方法

```kotlin
TextInputState(
    text: String = "",
    selectionStart: Int = 0,
    selectionEnd: Int = 0,
    compositionStart: Int = NO_COMPOSITION,
    compositionEnd: Int = NO_COMPOSITION,
    length: Int? = null
)
```

### encode/decode 方法

`TextInputState` 内部使用 `encode()` 和 `decode()` 方法进行 JSON 序列化和反序列化，用于跨层通信。

```kotlin
// 内部使用，一般无需直接调用
val jsonString = state.encode()  // 编码为 JSON 字符串
val state = TextInputState.decode(jsonObject)  // 从 JSONObject 解码
```

### replaceSelection 扩展函数

`replaceSelection()` 是一个便捷的扩展函数，用于在当前选区位置插入或替换文本，并自动计算新的光标位置。

```kotlin
/**
 * 在当前选区位置插入或替换文本
 * @param insertText 要插入的文本
 * @return 新的 TextInputState 对象
 */
fun TextInputState.replaceSelection(insertText: String): TextInputState
```

**实现逻辑**：

```kotlin
private fun TextInputState.replaceSelection(insertText: String): TextInputState {
    val start = selectionStart.coerceIn(0, text.length)
    val end = selectionEnd.coerceIn(0, text.length)
    val rangeStart = minOf(start, end)
    val rangeEnd = maxOf(start, end)
    val newText = text.substring(0, rangeStart) + insertText + text.substring(rangeEnd)
    val cursor = rangeStart + insertText.length
    return copy(
        text = newText,
        selectionStart = cursor,
        selectionEnd = cursor,
        compositionStart = TextInputState.NO_COMPOSITION,
        compositionEnd = TextInputState.NO_COMPOSITION,
        length = null
    )
}
```

**使用示例**：

```kotlin
// 插入表情短码
private fun insertEmoji(shortcode: String) {
    inputRef?.view?.getTextInputState { state ->
        val newState = state.replaceSelection(shortcode)
        inputRef?.view?.setTextInputState(newState)
    }
}
```

### 完整使用示例

```kotlin
@Page("TextInputStateDemo")
internal class TextInputStateDemo : Pager() {
    private var inputState: TextInputState by observable(TextInputState(text = ""))
    private var inputRef: ViewRef<InputView>? = null

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Input {
                ref { ctx.inputRef = it }
                attr {
                    textPostProcessor("input")
                    textInputState { ctx.inputState }
                }
                event {
                    // 监听文本和选区变化
                    textInputStateChange { state ->
                        ctx.inputState = state
                    }
                    // 监听选区变化（不需要文本变化）
                    selectionChange { state ->
                        ctx.inputState = state
                    }
                }
            }

            Button {
                attr { titleAttr { text("插入 [smile]") } }
                event {
                    click {
                        ctx.insertText("[smile]")
                    }
                }
            }
        }
    }

    private fun insertText(text: String) {
        // 同步修改 inputState，通过 textInputState lambda 绑定自动同步到原生层
        inputState = inputState.replaceSelection(text)
    }
}
```

## 参考

- [Input 组件文档](../API/components/input.md)
- [TextArea 组件文档](../API/components/text-area.md)
- [Text 组件文档](../API/components/text.md)
- [扩展原生 UI 文档](./expand-native-ui.md)