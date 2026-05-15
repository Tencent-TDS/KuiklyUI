# Input(单行输入框)

``Input``组件为单行输入框

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/tree/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/InputViewDemoPage.kt)

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)

### text方法

作用和``Text``组件的[text属性方法一致](text.md#text方法)

### fontSize方法

作用和``Text``组件的[fontSize属性方法一致](text.md#fontSize方法)

### fontWeightNormal方法

作用和``Text``组件的[fontWeightNormal属性方法一致](text.md#fontWeightNormal方法)

### fontWeightBold方法

作用和``Text``组件的[fontWeightBold属性方法一致](text.md#fontWeightBold方法)

### color方法

指定输入框输入文本的颜色, 作用和``Text``组件的[color属性方法一致](text.md#color方法)

### textAlignLeft方法

作用和``Text``组件的[textAlignLeft属性方法一致](text.md#textAlignLeft方法)

### textAlignCenter方法

作用和``Text``组件的[textAlignCenter属性方法一致](text.md#textAlignCenter方法)

### textAlignRight方法

作用和``Text``组件的[textAlignRight属性方法一致](text.md#textAlignRight方法)

### keyboardTypePassword方法

设置输入框的内容类型为密码类型

### keyboardTypeNumber方法

设置输入框的内容类型为数字类型

### keyboardTypeEmail方法

设置输入框的内容类型为邮件类型

### returnKeyTypeSearch方法

设置输入法的下一步按钮类型为搜索类型

### returnKeyTypeSend方法

设置输入法的下一步按钮类型为发送类型

### returnKeyTypeDone方法

设置输入法的下一步按钮类型为完成类型

### returnKeyTypeNext方法

设置输入法的下一步按钮类型为下一步类型

### returnKeyTypeContinue方法<Badge text="仅iOS" type="warn"/>

设置输入法的下一步按钮类型为继续类型

### returnKeyTypeGo方法

设置输入法的下一步按钮类型为前往类型

### returnKeyTypeGoogle方法<Badge text="仅iOS" type="warn"/>

设置输入法的下一步按钮类型为谷歌类型

### enablesReturnKeyAutomatically方法<Badge text="仅iOS" type="warn"/>

自定根据内容禁用和启用iOS软件盘的Return Key

### autoHideKeyboardOnImeAction方法

设置是否在点击 IME 动作按钮（如 Send/Go/Search）时自动收起键盘

### enablePinyinCallback方法<Badge text="仅iOS" type="warn"/>

是否启用拼音输入回调。当设置为 `true` 时，在拼音输入过程中（未确认选择汉字时）也会触发 `textDidChange` 回调。

### imeNoFullscreen方法<Badge text="仅Android" type="warn"/>

控制横屏状态下 IME 输入法是否进入全屏模式。

> **使用建议**：当输入框位于**独立 Window** 的浮层中（例如 `Dialog`、或 `Modal(inWindow = true)`），并希望在横屏下也能正常弹出软键盘时，**强烈建议设置为 `true`**。
>
> Android 横屏默认进入 fullscreen IME，此时若输入框处于独立 Window 浮层，系统首次 `showSoftInput(SHOW_IMPLICIT)` 会被忽略，导致软键盘不弹出。设置 `imeNoFullscreen(true)` 会同时触发 `restartInput()` 重启输入连接，绕过该问题。
>
> Compose DSL 中可通过 `Modifier.setProp("imeNoFullscreen", true)` 设置，详见 [Compose 核心组件 - TextField 差异化点](../../Compose/core-components.md)。

### returnKeyTypeContinue方法<Badge text="仅iOS" type="warn"/>

设置输入法的下一步按钮类型为继续类型

### returnKeyTypeGo方法

设置输入法的下一步按钮类型为前往类型

### returnKeyTypeGoogle方法<Badge text="仅iOS" type="warn"/>

设置输入法的下一步按钮类型为谷歌类型

### placeholder方法<Badge text="微信小程序实现中" type="warn"/>

设置输入框的提示文本

<div class="table-01">

**placeholder方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| placeholder | 提示文本值  | String |

</div>

:::tabs

@tab:active 示例

```kotlin{12}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }
            }
        }
    }
}
```

@tab 效果

<div align="center">
<img src="./img/input_place_holder.png" style="width: 30%; border: 1px gray solid">
</div>

:::

### placeholderColor <Badge text="微信小程序实现中" type="warn"/>

设置输入框提示文本颜色

<div class="table-01">

**placeholderColor属性方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| color | 提示文本颜色  | Long `|` Color |

</div>

:::tabs

@tab:active 示例

```kotlin{13}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    placeholderColor(Color.BLUE)
                }
            }
        }
    }
}
```

@tab 效果

<div align="center">
<img src="./img/input_place_holder_color.png" style="width: 30%; border: 1px gray solid">
</div>

:::

### tintColor方法 <Badge text="微信小程序实现中" type="warn"/>

设置输入框光标颜色

<div class="table-01">

**tintColor方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| color | 输入框光标颜色  | Long `|` Color |

</div>

:::tabs

@tab:active 示例

```kotlin{13}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    tintColor(Color.RED)
                }
            }
        }
    }
}
```

@tab 效果

<div align="center">
<img src="./img/input_tint_color.png" style="width: 30%; border: 1px gray solid">
</div>

:::

### maxTextLength

限制输入框的输入长度。支持三种长度限制类型：按字节计算、按字符计算、按视觉宽度计算。

<div class="table-01">

**maxTextLength方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| length | 最大输入长度  | Int |
| type | 长度限制类型  | LengthLimitType |

</div>

**LengthLimitType 枚举类型**

| 类型  | 值 | 说明     |
|:----|:--|:-------|
| BYTE | 0 | 限制输入的长度按字节计算 |
| CHARACTER | 1 | 限制输入的长度按字符计算 |
| VISUAL_WIDTH | 2 | 限制输入的长度按视觉宽度计算 |

**长度计算示例**

| 示例       | BYTE | CHARACTER | VISUAL_WIDTH | 说明                                  |
|----------|------|-----------|--------------|-------------------------------------|
| `""`       | 0    | 0         | 0            | 空字符串：0                              |
| `"a"`      | 1    | 1         | 1            | 英文：UTF8字节数1，字符个数1，视觉宽度1             |
| `"中"`      | 3    | 1         | 2            | 中文：UTF8字节数3，字符个数1，视觉宽度2             |
| `"😂"`     | 4    | 1         | 2            | Emoji：UTF8字节数4，字符个数1，视觉宽度2          |
| `"[img]"` | 5    | 1         | 2            | ImageSpan：描述文本的UTF8字节数5，字符个数1，视觉宽度2 |
| `"\u200B"` | 3    | 1         | 1            | 不可见字符：UTF8字节数3，字符个数1，视觉宽度按1计算       |

> 注：VISUAL_WIDTH模式下，未识别出来的不可见字符可能会被统计为2

**示例**

:::tabs

@tab:active 按字符限制（推荐）

```kotlin{13}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    maxTextLength(20, LengthLimitType.CHARACTER) // 限制最多输入20个字符
                }
            }
        }
    }
}
```

@tab 按字节限制

```kotlin{13}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    maxTextLength(20, LengthLimitType.BYTE) // 限制最多输入20个字节
                }
            }
        }
    }
}
```

@tab 按视觉宽度限制

```kotlin{13}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    maxTextLength(20, LengthLimitType.VISUAL_WIDTH) // 限制最多输入视觉宽度为20
                }
            }
        }
    }
}
```

@tab 已废弃的单参数用法

```kotlin{13}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    @Suppress("DEPRECATION")
                    maxTextLength(20) // 已废弃，建议使用 maxTextLength(20, LengthLimitType.CHARACTER)
                }
            }
        }
    }
}
```

:::

### autofocus方法

是否自动获取焦点, 获取焦点后会触发软键盘的弹起

<div class="table-01">

**autofocus方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| focus | 是否自动获取焦点  | Boolean |

</div>

**示例**

```kotlin{13}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    autofocus(true)
                }
            }
        }
    }
}
```

### editable方法

是否可编辑


<div class="table-01">

**editable方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| editable | 是否可编辑  | Boolean |

</div>

**示例**

```kotlin{13}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    editable(false) // 不可编辑
                }
            }
        }
    }
}
```

### textPostProcessor方法 <Badge text="仅Android支持" type="warn"/>

声明文本后置处理器名称，用于将文本中的特定标记（如表情短码）替换为富文本样式（如 `ImageSpan`）。具体处理逻辑需在 Android 端实现 [`IKRTextPostProcessorAdapter`](../../DevGuide/text-post-processor-guide.md) 适配器。

<div class="table-01">

**textPostProcessor方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| processor | 处理器名称，由业务自定义并在 Android 适配器中实现对应逻辑  | String |

</div>

:::tip 常见处理器名称
- `"emoji"` / `"input"` — 将 `[smile]` 等短码替换为表情图片（需在适配器中实现映射）
- 其他名称可自由定义，只要在适配器的 `when` 分支中处理即可
:::

**示例**

```kotlin{14}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入表情短码如 [smile]")
                    textPostProcessor("input")
                }
            }
        }
    }
}
```

> 完整实现请参考 [文本后置处理器实践指南](../../DevGuide/text-post-processor-guide.md)。

### textInputState方法 <Badge text="2.15+" type="info"/>

原子化设置输入框的 raw text、selection、composition 状态。通过 `TextInputState` 数据类一次性设置文本内容和光标/选区位置，避免分开设置导致的闪烁问题。

<div class="table-01">

**textInputState方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| state | 文本输入状态对象  | TextInputState |

</div>

:::tip 关于 TextInputState
`TextInputState` 是用于 Core/Render 通信的跨层文本输入状态数据类。详细字段说明请参考 [TextInputState 数据结构](#textinputstate-数据结构)。
:::

**示例**

```kotlin{14}
@Page("demo_page")
internal class TestPage : BasePager() {
    var inputState by observable(TextInputState(text = ""))
    
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入文字")
                    textInputState(inputState) // 绑定状态
                }
                event {
                    textInputStateChange { state ->
                        inputState = state // 状态变化时更新
                    }
                }
            }
        }
    }
}
```

> 常见使用场景：表情短码插入、@提及、光标定位等需要精确控制文本和选区位置的功能。

### inputSpans方法<Badge text="鸿蒙实现中" type="warn"/><Badge text="H5实现中" type="warn"/> <Badge text="微信小程序实现中" type="warn"/>

设置输入文本的文本样式配合`textDidChange`来更改`spans`实现输入框富文本化。


<div class="table-01">

**inputSpans方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| spans | 富文本样式  | InputSpans |

</div>

`InputSpans`可以通过`addSpan`来添加`InputSpan`样式。`InputSpan`可用来设置`Input`的文本样式，详细使用方法见以下示例：

**示例**

:::tabs

@tab:active 示例

```kotlin{18}
@Page("demo_page")
internal class TestPage : BasePager() {
    var spans by observable(InputSpans())
    lateinit var ref: ViewRef<InputView>
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }
            Input {
                ref {
                    ctx.ref = it
                }
                attr {
                    size(pagerData.pageViewWidth, 400f)
                    inputSpans(ctx.spans)
                    backgroundColor(Color.GREEN)
                }
                event {
                    textDidChange(true) { it ->
                        val hightSpan = {
                            InputSpan().apply {
                                color(Color.RED)
                                if (it.text.length <= 10) {
                                    text(it.text)
                                } else {
                                    text(it.text.substring(0, 10))
                                }
                                fontSize(30f)
                            }
                        }
                        val normalSpan = {
                            InputSpan().apply {
                                color(Color.BLACK)
                                text(it.text.substring(10, it.text.length))
                                fontSize(20f)
                            }
                        }
                        val spans = InputSpans()

                        if (it.text.length <= 10) {
                            spans.addSpan(hightSpan.invoke())
                        } else {
                            spans.addSpan(hightSpan.invoke())
                            spans.addSpan(normalSpan.invoke())
                        }
                        ctx.spans = spans
                    }
                }
            }
        }
    }
}
```

@tab 效果

<div align="center">
<img src="./img/input_span.png" style="width: 30%; border: 1px gray solid">
</div>

:::



## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)

### textDidChange

``textDidChange``事件意为输入框文本变化事件，如果组件有设置该事件事件，当``Input``组件输入内容发生变化时，会触发``textDidChange``闭包回调。``textDidChange``闭包中含有
``InputParams``类型参数，以此来描述输入框文本变化事件的信息

<div class="table-01">

**InputParams**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| text | 当前输入的文本  | String |
| length | 当前文本长度（与 ``maxTextLength`` 的 ``LengthLimitType`` 一致，按字节/字符/视觉宽度计算）。仅当设置 ``maxTextLength`` 后有效，否则为空<Badge text="2.15+" type="info"/> | Int? |

</div>

**示例**

```kotlin{16-18}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }
                
                event { 
                    textDidChange { 
                        val text = it.text // 变化后的文本
                    }
                }
            }
        }
    }
}
```

### textInputStateChange事件 <Badge text="2.15+" type="info"/>

``textInputStateChange``事件在 raw text、selection 或 composition 发生变化时触发。与 `textDidChange` 不同，该事件会返回完整的 `TextInputState` 对象，包含文本内容和选区信息。详细字段说明请参考 [TextInputState 数据结构](#textinputstate-数据结构)。

:::tip 使用建议
- 如果需要同时获取文本和选区变化，使用 `textInputStateChange`
- 如果只需要文本变化，使用 `textDidChange`
- 建议设置 `isSyncEdit = true`（默认值）以实现同步编辑，避免异步更新带来的光标跳动
:::

**示例**

```kotlin{14-16}
Input {
    attr {
        size(200f, 40f)
        placeholder("输入文字")
        textInputState(inputState)
    }
    event {
        textInputStateChange { state ->
            // state 包含 text, selectionStart, selectionEnd, compositionStart, compositionEnd
            inputState = state
            previewText = state.text
        }
    }
}
```

### selectionChange事件 <Badge text="2.15+" type="info"/>

``selectionChange``事件在选区范围发生变化时触发（不需要文本发生变化）。例如：用户移动光标、选择文本等操作会触发此事件。返回完整的 `TextInputState` 对象，详细字段说明请参考 [TextInputState 数据结构](#textinputstate-数据结构)。

:::tip 使用场景
- 监听光标移动，实现 @提及 弹窗的触发
- 监听选区变化，实现富文本工具栏的状态更新
- 与 `textInputStateChange` 配合使用，分别处理「选区变化」和「文本+选区变化」
:::

**示例**

```kotlin{14-16}
Input {
    attr {
        size(200f, 40f)
        placeholder("输入文字")
    }
    event {
        selectionChange { state ->
            // 光标或选区发生变化，但文本未变化
            currentSelectionStart = state.selectionStart
            currentSelectionEnd = state.selectionEnd
        }
    }
}
```

### inputFocus

``inputFocus``事件意为输入框获取到焦点事件，如果组件有设置该事件事件，当``Input``组件获取到焦点时，会触发``inputFocus``闭包回调。``inputFocus``闭包中含有
``InputParams``类型参数，以此来描述输入框获取到焦点事件的信息

**示例**

```kotlin{16-18}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }

                event {
                    inputFocus { inputParams -> 
                        val text = inputParams.text
                    }
                }
            }
        }
    }
}
```

### inputBlur

``inputBlur``事件意为输入框失去焦点事件，如果组件有设置该事件事件，当``Input``组件失去焦点时，会触发``inputBlur``闭包回调。``inputBlur``闭包中含有
``InputParams``类型参数，以此来描述输入框失去焦点事件的信息

**示例**

```kotlin{16-18}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }

                event {
                    inputBlur { inputParams ->
                        val text = inputParams.text
                    }
                }
            }
        }
    }
}
```

### keyboardHeightChange<Badge text="H5实现中" type="warn"/> <Badge text="微信小程序实现中" type="warn"/>

``keyboardHeightChange``事件意为软键盘高度变化事件，如果组件有设置该事件事件，当软键盘高度变化时，会触发``keyboardHeightChange``闭包回调。``keyboardHeightChange``闭包中含有
``KeyboardParams``类型参数，以此来描述软键盘高度变化事件的信息

<div class="table-01">

**KeyboardParams**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| height | 软键盘高度  | Float |
| duration | 软键盘高度变化动画时长（秒）  | Float |
| curve | iOS键盘动画曲线值，可用于`Animation.keyboard()`实现与键盘动画同步<Badge text="仅iOS" type="warn"/> | Int |

</div>

::: tip 平台说明
- `curve` 参数仅在 iOS 平台有效，其他平台该值为默认值 0
- 在非 iOS 平台使用 `Animation.keyboard()` 时，动画效果等同于 `Animation.linear()`
:::

**基础示例**

```kotlin{16-18}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }

                event {
                    keyboardHeightChange { keyboardParams -> 
                        val height = keyboardParams.height
                        val duration = keyboardParams.duration
                        val curve = keyboardParams.curve
                    }
                }
            }
        }
    }
}
```

**跨平台键盘动画最佳实践**

推荐根据平台选择合适的动画：
- **iOS**：使用`Animation.keyboard()`配合`curve`参数实现与系统键盘动画完美同步
- **其他平台**：使用`Animation.easeInOut()`等通用动画

```kotlin
@Page("demo_page")
internal class TestPage : BasePager() {
    var keyboardHeight: Float by observable(0f)
    var keyboardAnimation: Animation by observable(Animation.easeInOut(0.25f))
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                    transform(Translate(0f, -ctx.keyboardHeight))
                    animation(ctx.keyboardAnimation, ctx.keyboardHeight)
                }

                event {
                    keyboardHeightChange { params ->
                        ctx.keyboardAnimation = createKeyboardAnimation(params)
                        ctx.keyboardHeight = params.height
                    }
                }
            }
        }
    }
    
    // Create keyboard animation based on platform
    private fun createKeyboardAnimation(params: KeyboardParams): Animation {
        return if (PlatformUtils.isIOS()) {
            // iOS: Use native keyboard curve for perfect sync
            Animation.keyboard(params.duration, params.curve)
        } else {
            // Other platforms: Use easeInOut as fallback
            Animation.easeInOut(params.duration)
        }
    }
}
```

### inputReturn <Badge text="微信小程序实现中" type="warn"/>

``inputReturn``事件意为软键盘触发了Return事件，如果组件有设置该事件事件，当软键盘触发了Return事件时，会触发``inputReturn``闭包回调。``inputReturn``闭包中含有
``InputParams``类型参数，以此来描述软键盘触发了Return事件的信息

**示例**

```kotlin{16-18}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }

                event {
                    onTextReturn { param ->

                    }
                }
            }
        }
    }
}
```

### textLengthBeyondLimit <Badge text="微信小程序实现中" type="warn"/>

``textLengthBeyondLimit``事件意为输入框发生了输入超出最大输入字符的事件，如果组件有设置该事件事件，当输入框发生了输入超出最大输入字符的事件，会触发``textLengthBeyondLimit``闭包回调。``textLengthBeyondLimit``闭包中含有
``InputParams``类型参数，以此来描述输入框触发了输入超出最大输入字符的事件的信息

**示例**

```kotlin{16-18}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Input {
                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }

                event {
                    textLengthBeyondLimit { param ->

                    }
                }
            }
        }
    }
}
```

## 方法

### setText

设置输入框的文本值

<div class="table-01">

**setText**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| text | 文本  | String |

</div>

**示例**

```kotlin{26-28}
@Page("demo_page")
internal class TestPage : BasePager() {

    lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Input {
                ref {
                    ctx.inputRef = it
                }

                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }
            }
        }
    }

    private fun setInputText() {
        inputRef.view?.setText("设置输入框文本")
    }
}
```

### focus

主动让输入框获取焦点, 焦点获取成功后，软键盘会自动弹起

**示例**

```kotlin{26-28}
@Page("demo_page")
internal class TestPage : BasePager() {

    lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Input {
                ref {
                    ctx.inputRef = it
                }

                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }
            }
        }
    }

    private fun focus() {
        inputRef.view?.focus()
    }
}
```

### blur

主动让输入框失去焦点, 焦点失去以后，软键盘会自动收起

**示例**

```kotlin{26-28}
@Page("demo_page")
internal class TestPage : BasePager() {

    lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Input {
                ref {
                    ctx.inputRef = it
                }

                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }
            }
        }
    }

    private fun blur() {
        inputRef.view?.blur()
    }
}
```

### cursorIndex <Badge text="小程序支持中" type="warn"/>

获取光标当前位置

**示例**

```kotlin{26-29}
@Page("demo_page")
internal class TestPage : BasePager() {

    lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Input {
                ref {
                    ctx.inputRef = it
                }

                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }
            }
        }
    }

    private fun getCursor() {
        ref.view?.cursorIndex {
            KLog.i("Input", "index: $it")
        }
    }
}
```

### getTextInputState方法 <Badge text="2.15+" type="info"/>

从原生输入框获取当前的 raw text、selection、composition 状态。用于获取输入框的实时状态。

<div class="table-01">

**getTextInputState方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| callback | 回调函数，返回 TextInputState 对象  | (TextInputState) -> Unit |

</div>

:::tip 使用场景
- 在按钮点击等事件中，获取当前输入框的完整状态
- 配合 `setTextInputState()` 实现状态的读取-修改-写入流程
- 例如：插入表情时，先获取当前状态和选区，然后计算新的文本和光标位置
:::

**示例**

```kotlin{20-24}
@Page("demo_page")
internal class TestPage : BasePager() {
    lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // ... UI布局
        }
    }

    private fun insertText(text: String) {
        inputRef.view?.getTextInputState { state ->
            // 获取当前状态，计算新状态，然后设置
            val newState = state.replaceSelection(text)
            inputRef.view?.setTextInputState(newState)
        }
    }
}
```

### setTextInputState方法 <Badge text="2.15+" type="info"/>

原子化设置输入框的 raw text、selection、composition 状态。与 `textInputState()` 属性方法不同，此方法可以在任意时刻调用（不限于声明阶段）。

<div class="table-01">

**setTextInputState方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| state | 文本输入状态对象  | TextInputState |

</div>

:::tip 与 textInputState() 属性的区别
- `textInputState()` 是属性方法，在声明阶段绑定状态，适合响应式更新
- `setTextInputState()` 是实例方法，可以在任意时刻调用，适合事件处理中动态设置
:::

**示例**

```kotlin{20-22}
@Page("demo_page")
internal class TestPage : BasePager() {
    lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // ... UI布局
        }
    }

    private fun clearInput() {
        // 清空输入框
        inputRef.view?.setTextInputState(TextInputState(text = ""))
    }
    
    private fun insertEmoji(shortcode: String) {
        inputRef.view?.getTextInputState { state ->
            val newState = state.replaceSelection(shortcode)
            inputRef.view?.setTextInputState(newState)
        }
    }
}
```

### setCursorIndex <Badge text="小程序支持中" type="warn"/>

设置当前光标位置

**setCursorIndex**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| index | 光标位置  | Int |

**示例**

```kotlin{26-28}
@Page("demo_page")
internal class TestPage : BasePager() {

    lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Input {
                ref {
                    ctx.inputRef = it
                }

                attr {
                    size(200f, 40f)
                    placeholder("输入框提示")
                }
            }
        }
    }

fun setCursorIndex(index: Int) {
        ref.view?.setCursorIndex(index)
    }
}
```

## TextInputState 数据结构 <Badge text="2.15+" type="info"/>

`TextInputState` 是用于 Core/Render 通信的跨层文本输入状态数据类，被 `textInputState()`、`textInputStateChange`、`selectionChange`、`getTextInputState()`、`setTextInputState()` 等属性和方法共同使用。

<div class="table-01">

**TextInputState 字段说明**

| 字段  | 描述     | 类型 | 默认值 |
|:----|:-------|:--|:--|
| text | 当前输入的文本（raw text，不含富文本渲染信息）  | String | `""` |
| selectionStart | 选区起始位置（光标位置时与 `selectionEnd` 相等）  | Int | `0` |
| selectionEnd | 选区结束位置  | Int | `0` |
| compositionStart | 组合输入起始位置（iOS 拼音输入过程中有效，无组合输入时为 `NO_COMPOSITION = -1`）  | Int | `-1` |
| compositionEnd | 组合输入结束位置（iOS 拼音输入过程中有效）  | Int | `-1` |

</div>

:::tip 常用操作方法
- `replaceSelection(text)` — 用指定文本替换当前选区，返回新的 `TextInputState`（光标移至插入内容末尾）

`replaceSelection()` 不是 SDK 内置 API，需要开发者自行实现，参考实现如下：

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
        compositionStart = NO_COMPOSITION,
        compositionEnd = NO_COMPOSITION,
        length = null
    )
}
```
:::

**示例：插入表情短码**

```kotlin
private fun insertEmoji(shortcode: String) {
    inputRef.view?.getTextInputState { state ->
        // state.selectionStart == state.selectionEnd 时表示光标（无选区）
        val newState = state.replaceSelection(shortcode)
        inputRef.view?.setTextInputState(newState)
    }
}
```





