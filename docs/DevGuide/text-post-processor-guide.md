# 自定义表情实践指南

本文介绍如何在 Kuikly 中实现自定义表情输入功能，将 `[smile]` 等短码实时渲染为表情图片，并支持在光标位置插入表情。

:::tip 功能概述
- **表情短码输入**：用户输入 `[smile]` 等短码，自动渲染为对应表情图片
- **表情面板**：点击表情按钮，在光标位置插入对应短码
- **实时预览**：输入框和预览区域同步显示表情渲染效果
- **跨端支持**：Android、iOS 和鸿蒙支持（iOS 单行输入框除外；鸿蒙单行 `Input` 暂不支持图片自定义表情）
:::

## 整体流程

实现自定义表情输入需要三步：

```
1. DSL 声明 processor 名称  →  2. 平台实现处理器适配器  →  3. 注册适配器
```

**运行时调用链**：

```
用户输入 / 调用 setTextInputState()
  → Render 层检测到组件设置了 textPostProcessor 属性
  → 调用适配器 onTextPostProcess()，传入原始文本和字体属性
  → 适配器返回带 ImageSpan 的 SpannableStringBuilder（Android）
     或带 NSTextAttachment 的 NSMutableAttributedString（iOS）
     或 Text/Image Span 序列（鸿蒙）
  → Render 层将处理结果渲染到 UI
```

:::warning iOS 平台限制
- **单行输入框（`Input` / `UITextField`）不支持 `NSTextAttachment` 图片渲染**，因此自定义表情图片预览在 iOS 单行模式下不可用。如需表情预览，请使用多行输入框（`TextArea` / `UITextView`）。
- 如果只需要短码文本输入而不需要图片预览，单行输入框可以正常使用。
:::

:::warning 鸿蒙平台限制
- **单行输入框（`Input`）暂不支持图片自定义表情**。受鸿蒙系统底层 Text Editor 限制，新输入控件暂不支持 keyboard type，因此单行 `Input` 维持原来的输入能力，不走图片自定义表情路径。
- 如果只需要短码文本输入而不需要图片预览，单行 `Input` 可以正常使用；如需图片表情预览，请使用多行 `TextArea` 或只读 `Text` 预览区域。
:::

:::tip 鸿蒙平台说明
- `Text` 只读渲染会按 DSL 传入的 `textPostProcessor("xxx")` 名称调用对应 adapter。
- `TextArea` 编辑态使用固定的输入处理器名称 `"input"`，业务侧需通过 `KRRegisterTextPostProcessorAdapter("input", adapter)` 注册。
- `TextArea` 场景推荐使用 `KRTextProcessedResultAppendImageSpanWithRaw(builder, uri, rawLiteral, width, height)` 回传图片 span。`rawLiteral` 是图片在原始文本中的短码（如 `[smile]`），用于在用户继续编辑、删除图片 span 或上抛 `TextInputState.text` 时精准还原 raw text。
- 图片 `src` 必须是可寻址 URI，目前推荐 `file://...`，也支持 `http(s)://...` 或 `data:image/...;base64,...`；不要把业务私有协议直接传给 SDK。
:::

## DSL 使用方式

### 自研 DSL

在 `TextArea` 或 `Text` 组件上通过 `textPostProcessor()` 属性声明处理器名称：

```kotlin{14,22}
@Page("emoji_demo")
internal class EmojiDemoPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            // TextArea with post-processor（多行输入框，支持表情预览）
            TextArea {
                attr {
                    size(300f, 100f)
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

:::tip 自研 DSL 关键点
- **使用 `TextArea` 而非 `Input`**：iOS 上 `Input`（UITextField）不支持 `NSTextAttachment` 图片渲染，请使用 `TextArea`（UITextView）。鸿蒙单行 `Input` 暂不支持图片自定义表情，如需图片预览请使用 `TextArea`。
- **`textPostProcessor("input")`**：声明处理器名称，触发适配器中的处理逻辑
- **processor 名称自定义**：`"emoji"`、`"input"` 是示例名称，实际名称由业务自定义；鸿蒙 `TextArea` 编辑态需注册 `"input"`，只读 `Text` 可注册并使用业务自定义名称（如 `"richtext"`）
:::

#### 表情面板：点击插入表情短码

自研 DSL 通过 `TextInputState` 管理输入框状态，配合 `replaceSelection()` 扩展函数实现光标位置插入表情：

```kotlin{4-5,12-13,19-22,27-30}
@Page("EmojiTextInputDemo")
internal class EmojiTextInputDemo : Pager() {

    // ① 使用 TextInputState 管理输入状态（raw text 保留短码，如 [smile]）
    private var inputState: TextInputState by observable(TextInputState(text = ""))

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // ② TextArea 绑定 textInputState，支持响应式更新
            TextArea {
                attr {
                    textPostProcessor("input")
                    textInputState { ctx.inputState }  // lambda 形式，状态变化自动刷新
                }
                event {
                    textInputStateChange { state ->
                        ctx.inputState = state  // 文本变化时同步状态
                    }
                    selectionChange { state ->
                        ctx.inputState = state  // 光标移动时同步状态
                    }
                }
            }

            // ③ 表情按钮：点击在光标处插入短码
            Button {
                attr { titleAttr { text("😊 [smile]") } }
                event {
                    click {
                        ctx.insertEmoji("[smile]")
                    }
                }
            }
        }
    }

    // ④ 使用 replaceSelection 在当前光标位置插入表情短码
    private fun insertEmoji(shortcode: String) {
        inputState = inputState.replaceSelection(shortcode)
    }
}
```

:::tip 表情面板交互要点
- **`TextInputState` 是核心**：它同时保存了文本内容和光标位置（`selectionStart` / `selectionEnd`），`replaceSelection()` 根据光标位置插入短码
- **两个事件都要监听**：`textInputStateChange` 同步文本变化，`selectionChange` 同步光标移动，两者缺一不可
- **`textInputState { }` 使用 lambda 形式**：确保 `inputState` 变化时输入框自动刷新（非 lambda 形式不会响应式更新）
- **`replaceSelection()` 非内置 API**：这是一个需要开发者自行实现的扩展函数，参考实现详见 [TextInputState 数据结构](../API/components/input.md#textinputstate-数据结构) 章节
:::

:::tip 两种 DSL 表情面板对比
| 特性 | 自研 DSL | Compose DSL |
|:----|:---------|:------------|
| 状态管理 | `TextInputState` | `TextFieldState` |
| 光标插入 | `replaceSelection()` 扩展函数 | `state.edit { replace() }` |
| 响应式绑定 | `textInputState { state }` | `state.text` 自动响应 |
| 清空内容 | `TextInputState(text = "")` | `state.clearText()` |
:::

### Compose DSL

Compose DSL 使用 `textPostProcessor()` Modifier，配合 `BasicTextField` 和 `rememberTextFieldState()` 实现表情输入：

```kotlin
@Composable
fun EmojiInputDemo() {
    // 创建 TextFieldState 管理输入状态（raw text 为短码格式，如 [smile]）
    val state = rememberTextFieldState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        // ① BasicTextField（多行输入框 + 表情预览）
        // 需要同时设置 modifier.textPostProcessor() 和 outputTransformation
        BasicTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.White)
                .padding(12.dp)
                .textPostProcessor("input"),
            outputTransformation = TextPostProcessorOutputTransformation("input"),
            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333)),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ② 表情面板（点击在光标处插入短码）
        EmojiGrid(
            emojis = listOf("[smile]" to "😊", "[heart]" to "❤️"),
            onEmojiClick = { shortCode ->
                state.edit {
                    replace(selection.start, selection.end, shortCode)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ③ 表情预览（使用 textPostProcessor 修饰符）
        Text(
            text = state.text.ifEmpty { "（请在上方输入框或表情面板输入）" },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp)
                .textPostProcessor("input")
        )
    }
}

/**
 * 表情面板组件
 */
@Composable
fun EmojiGrid(
    emojis: List<Pair<String, String>>,
    onEmojiClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        emojis.forEach { (shortCode, emoji) ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFF0F0F0), shape = CircleShape)
                    .clickable { onEmojiClick(shortCode) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
        }
    }
}
```

:::tip Compose DSL 关键点
- **`Modifier.textPostProcessor("input")`**：声明处理器名称，触发适配器中的处理逻辑
- **`TextPostProcessorOutputTransformation("input")`**：将处理器输出同步到 `BasicTextField` 的展示层（必需，否则输入框内不显示处理后的效果）
- **`state.edit { replace(...) }`**：在光标位置插入或替换选区内容（支持光标定位）
:::

#### 表情面板：点击插入表情短码

Compose DSL 通过 `rememberTextFieldState()` 管理状态，使用 `state.edit {}` 在光标位置插入表情短码：

```kotlin{3,10-12,20-22}
@Composable
fun EmojiInputDemo() {
    val state = rememberTextFieldState()  // ① 管理输入状态（raw text 为短码格式）

    Column {
        BasicTextField(
            state = state,
            modifier = Modifier.textPostProcessor("input"),
            outputTransformation = TextPostProcessorOutputTransformation("input"),
        )

        // ② 表情面板：点击在光标处插入短码
        EmojiGrid(
            emojis = listOf("[smile]" to "微笑", "[heart]" to "爱心"),
            onEmojiClick = { shortCode ->
                state.edit {
                    // ③ replace 会根据 selection 替换选区或在光标处插入
                    replace(selection.start, selection.end, shortCode)
                }
            }
        )
    }
}
```

:::tip Compose DSL 表情面板要点
- **`state.edit {}` 是线程安全的**：框架保证在 `edit` 块内对 `selection` 和文本的修改是原子的
- **`selection.start` / `selection.end`**：当前光标位置或选区范围，`replace` 会自动替换选区内容或插入到光标处
- **`state.text` 获取原始文本**：始终返回短码格式（如 `[smile]`），不会包含处理后的图片
- **清空输入**：使用 `state.clearText()` 一次性清空文本和重置选区
:::

:::warning 与 `maxLength` / 自定义 processor 组合时的注意事项
- **processor 名称要全链路一致**：Kotlin 侧 `textPostProcessor("comment_input")`、Android `IKRTextPostProcessorAdapter` 路由、iOS `hr_customTextWithAttributedString:textPostProcessor:` 路由、鸿蒙 `KRRegisterTextPostProcessorAdapter("comment_input", ...)` 注册名，都应使用同一个名称；鸿蒙 `TextArea` 编辑态当前约定使用 `"input"`。
- **表情插入请始终基于当前 raw selection 做 `replace(selection.start, selection.end, shortCode)`**：不要自己拆成“删选区 + append”，否则容易破坏中间插入和选区替换语义。
- **与 `Modifier.maxLength` 组合时，超限应拒绝整段 shortcode**：例如 `[smile]` 放不下时，应保留原有 raw text 和合法选区，而不是写入半个 token。
- **鸿蒙 `TextArea` 请回传 raw 字面量**：使用 `KRTextProcessedResultAppendImageSpanWithRaw`，否则图片 span 在 ArkUI 内部被扁平化为占位字符后，编辑回写时无法可靠还原原始短码。
:::

## Android 适配器实现

### 1. 创建适配器类

新建一个类实现 `IKRTextPostProcessorAdapter` 接口，在 `onTextPostProcess` 方法中处理表情短码：

```kotlin
/**
 * 自定义表情处理器适配器
 * 将 [smile] 等短码替换为表情图片
 */
class EmojiPostProcessorAdapter(context: Context) : IKRTextPostProcessorAdapter {

    private val appContext: Context = context.applicationContext

    companion object {
        // 短码 → drawable 资源映射
        private val EMOJI_MAP = mapOf(
            "[smile]" to R.drawable.emoji_smile,
            "[heart]" to R.drawable.emoji_heart,
            "[thumbup]" to R.drawable.emoji_thumbup,
            "[star]" to R.drawable.emoji_star,
            "[fire]" to R.drawable.emoji_fire,
        )

        // 匹配 [xxx] 格式的短码
        private val EMOJI_PATTERN = "\\[([^\\]]+)\\]".toRegex()
    }

    override fun onTextPostProcess(
        kuiklyRenderContext: com.tencent.kuikly.core.render.android.IKuiklyRenderContext?,
        inputParams: TextPostProcessorInput
    ): TextPostProcessorOutput {
        val sourceText = inputParams.sourceText?.toString() ?: return TextPostProcessorOutput("")
        
        // 查找所有短码匹配
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

    // 兼容旧接口：框架内部可能仍通过旧接口调用，必须保留此方法
    // 新版本请实现带 kuiklyRenderContext 参数的重载方法
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

        // 注册自定义表情处理器适配器
        KuiklyRenderAdapterManager.krTextPostProcessorAdapter = EmojiPostProcessorAdapter(this)
    }
}
```

:::warning 重要提醒
必须在 **Application.onCreate()** 中注册，确保在页面创建前完成初始化。
:::

## iOS 适配器实现

iOS 侧通过实现 `KuiklyRenderComponentExpandHandler` 的 `hr_customTextWithAttributedString:textPostProcessor:` 方法来处理表情短码替换。

### 1. 实现处理器方法

在 `KuiklyRenderComponentExpandHandler` 中实现 `hr_customTextWithAttributedString:textPostProcessor:`：

```objc
@implementation YourKuiklyRenderComponentExpandHandler

// Emoji 短码与图片名称映射（类属性，避免重复创建）
- (NSDictionary<NSString *, NSString *> *)emojiImageMap {
    static NSDictionary *map = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        map = @{
            @"[smile]": @"emoji_smile",
            @"[heart]": @"emoji_heart",
            @"[thumbup]": @"emoji_thumbup",
            @"[star]": @"emoji_star",
            @"[fire]": @"emoji_fire",
        };
    });
    return map;
}

// 匹配 [xxx] 短码的正则（类属性，避免重复编译）
- (NSRegularExpression *)emojiRegex {
    static NSRegularExpression *regex = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        regex = [NSRegularExpression regularExpressionWithPattern:@"\\[([^\\]]+)\\]"
                                                         options:0
                                                           error:nil];
    });
    return regex;
}

- (NSMutableAttributedString *)hr_customTextWithAttributedString:(NSAttributedString *)attributedString
                                               textPostProcessor:(NSString *)textPostProcessor {
    // 按 processor 名称路由（可匹配多个名称）
    if (![textPostProcessor isEqualToString:@"KRTextAreaView"] &&
        ![textPostProcessor isEqualToString:@"input"] &&
        ![textPostProcessor isEqualToString:@"emoji"]) {
        return [attributedString mutableCopy]; // 未匹配的 processor 透传原文本
    }
    
    NSString *sourceString = attributedString.string;
    if (sourceString.length == 0) {
        return [attributedString mutableCopy];
    }
    
    // 匹配 [xxx] 短码
    NSArray<NSTextCheckingResult *> *matches = [self.emojiRegex matchesInString:sourceString
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
        NSString *imageName = self.emojiImageMap[shortcode]; // 如 @"emoji_smile"
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

@end
```

### 2. processor 名称说明

iOS 侧 processor 名称的来源：
- **类名**（如 `KRTextAreaView`）：`setCss_values` 渲染路径默认使用 `NSStringFromClass([self class])`
- **业务自定义**（如 `input`、`emoji`）：Kotlin 侧通过 `textPostProcessor("input")` 设置

### 3. 图片资源准备

将表情图片添加到 iOS 项目的 `Assets.xcassets` 中，命名为与 `EMOJI_IMAGE_MAP` 中对应的值（如 `emoji_smile`、`emoji_heart` 等）。

### 4. 已知限制

| 限制 | 说明 |
|------|------|
| `UITextField` 不渲染 `NSTextAttachment` 图片 | iOS 单行输入框（`Input` 组件）无法显示表情图片，只有 `UITextView`（`TextArea` 组件）支持 |
| 光标跳动 | `UITextView` + `NSTextAttachment` 点击时可能触发两次 `selectionChange`，导致光标短暂跳回（iOS 原生问题） |

## 鸿蒙适配器实现

鸿蒙侧通过 `KRRegisterTextPostProcessorAdapter(name, adapter)` 注册具名 adapter。SDK 将原始 UTF-8 文本传入 adapter，业务按顺序追加 `TextSpan` / `ImageSpan` 后，由 Render 层渲染为只读文本或 ArkUI `StyledString`。

关键差异：

- **输入态名称**：`TextArea` 编辑态当前使用 `"input"` 处理器；只读 `Text` 使用 DSL 中 `textPostProcessor("xxx")` 传入的名称。单行 `Input` 暂不支持图片自定义表情。
- **图片 URI**：图片 `src` 需是可寻址 URI，例如 `file://...`、`http(s)://...` 或 `data:image/...;base64,...`。
- **raw 字面量**：`TextArea` 图片 span 推荐使用 `KRTextProcessedResultAppendImageSpanWithRaw(builder, src, rawLiteral, width, height)`，其中 `rawLiteral` 是 `[smile]` 这类原始短码，用于编辑后还原 `TextInputState.text`。

最小注册示例：

```cpp
KRRegisterTextPostProcessorAdapter("input", MyTextPostProcessorAdapter);
KRRegisterTextPostProcessorAdapter("richtext", MyTextPostProcessorAdapter);
```

## 完整示例一：自研 DSL 实现

完整的自研 DSL 自定义表情输入 Demo，请参考仓库中的实现：

- **Demo 文件**：[EmojiTextInputDemo.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/EmojiTextInputDemo.kt)

## 完整示例二：Compose DSL 实现

完整的 Compose DSL 自定义表情输入 Demo，请参考仓库中的实现：

- **Demo 文件**：[TextFieldEmojiDemo.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextFieldEmojiDemo.kt)

## 常见问题

### Q: 设置 textPostProcessor 后输入框不显示表情？

A: 检查以下几点：
1. **适配器是否已注册** — Android 确认在 Application.onCreate() 中设置了 `KuiklyRenderAdapterManager.krTextPostProcessorAdapter`；鸿蒙确认 native 初始化阶段调用了 `KRRegisterTextPostProcessorAdapter("input", adapter)`
2. **processor 名称是否匹配** — DSL 中 `textPostProcessor("xxx")` 的名称要与适配器中的处理逻辑匹配；鸿蒙 `TextArea` 编辑态当前固定使用 `"input"`
3. **图片资源是否存在** — Android 检查 `R.drawable.xxx` 是否引用正确；iOS 检查 `Assets.xcassets` 中是否有对应图片；鸿蒙检查 `file://` 真实路径是否存在，或 `http(s)` / `data:image` URI 是否可访问
4. **是否使用了正确的组件** — iOS 上确保使用 `TextArea` 而非 `Input`

### Q: 表情显示位置偏移/大小不对？

A: 确认 `drawable.setBounds(0, 0, size, size)` 已正确设置，并且 `ImageSpan` 使用了 `DynamicDrawableSpan.ALIGN_CENTER` 对齐方式。

### Q: iOS 上表情图片不显示？

A: 检查以下几点：
1. **是否使用了单行输入框** — iOS 的 `UITextField` 不支持 `NSTextAttachment` 图片渲染，表情图片只在 `UITextView`（即 `TextArea` 组件）中显示
2. **图片资源是否在 Asset Catalog 中** — iOS 侧需要将表情 PNG 添加到 `Assets.xcassets`
3. **processor 名称是否在 expandHandler 中匹配** — 确认 `hr_customTextWithAttributedString:textPostProcessor:` 中有对应的名称路由

### Q: iOS 上点击带表情的文本时光标会跳一下？

A: 这是 `UITextView` + `NSTextAttachment` 的 iOS 原生问题。点击时 `UITextView` 会先设置一个初始 selection，然后约 50ms 后修正到实际位置，导致光标短暂跳动。目前无法在 App 层面修复，长期方案需要避免在 `UITextView` 中使用 `NSTextAttachment`。

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

### Q: 为什么 EditText 需要返回 Editable？

A: EditText 内部使用 `Editable` 接口管理文本和 Span。如果返回普通 String，EditText 会丢失 Span 信息，导致表情图片无法显示。`SpannableStringBuilder` 是 `Editable` 的子类，因此是最佳选择。

### Q: 未设置 textPostProcessor 的组件会有额外性能开销吗？

A: 不会。适配器仅在组件设置了 `textPostProcessor` 属性时才会被调用，未设置该属性的 `Text`、`TextArea`、`Input` 组件走正常渲染路径，不会触发任何适配器逻辑。

## 参考

- [TextArea 组件文档](../API/components/text-area.md)
- [Text 组件文档](../API/components/text.md)
- [Input 组件文档 - TextInputState 数据结构](../API/components/input.md#textinputstate-数据结构)
- [鸿蒙 TextPostProcessor C API](../../core-render-ohos/src/main/cpp/libohos_render/api/include/Kuikly/Kuikly.h)
