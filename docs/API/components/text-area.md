# TextArea(多行输入框)

``TextArea`` 为多行输入框组件，支持多行文本输入、滚动等特性。

## 与 Input 组件的差异

`TextArea` 与 `Input` 组件大部分属性、事件和方法都相同，主要差异如下：

| 特性  | Input | TextArea |
|:----|:-----|:---------|
| 输入模式 | 单行输入 | 多行输入 |
| 不支持的属性 | - | `imeNoFullscreen`、`enablesReturnKeyAutomatically` |
| 不支持的事件 | - | `inputReturn`、`onTextReturn` |
| 特有属性 | - | `lineHeight()` |

## 属性

### 基础属性

支持所有[基础属性](basic-attr-event.md#基础属性)

### lineHeight方法 <Badge text="2.15+" type="info"/>

设置多行文本的行高。

<div class="table-01">

**lineHeight方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| lineHeight | 行高值（px）  | Float |

</div>

**示例**

```kotlin{10}
TextArea {
    attr {
        size(200f, 100f)
        lineHeight(24f) // 设置行高为 24px
        fontSize(16f)
    }
}
```

### textInputState方法 <Badge text="2.15+" type="info"/>

原子化设置输入框的 raw text、selection、composition 状态。具体说明请参考 [Input#textInputState](input.md#textinputstate方法)。

### textPostProcessor方法

声明文本后置处理器，用于表情短码替换等场景。具体说明请参考 [Input#textPostProcessor](input.md#textpostprocessor方法)。

### 其他属性

`TextArea` 支持除 `imeNoFullscreen`、`enablesReturnKeyAutomatically` 以外 [Input 组件的所有属性](input.md#属性)。

常用属性包括：
- `text()` - 设置文本内容
- `fontSize()` - 设置字体大小
- `color()` - 设置文本颜色
- `placeholder()` - 设置提示文本
- `placeholderColor()` - 设置提示文本颜色
- `tintColor()` - 设置光标颜色
- `maxTextLength()` - 设置最大输入长度
- `keyboardTypePassword()` / `keyboardTypeNumber()` / `keyboardTypeEmail()` - 设置键盘类型
- `returnKeyTypeSearch()` / `returnKeyTypeSend()` / `returnKeyTypeDone()` 等 - 设置返回键类型
- `autofocus()` - 设置自动获取焦点
- `editable()` - 设置是否可编辑
- `inputSpans()` - 设置富文本样式

## 事件

`TextArea` 支持除了 `inputReturn`、`onTextReturn` 以外 [Input 组件的所有事件](input.md#事件)。

常用事件包括：
- `textDidChange` - 文本变化事件
- `textInputStateChange` - 文本输入状态变化事件（返回 TextInputState）
- `selectionChange` - 选区变化事件（返回 TextInputState）
- `inputFocus` - 获取焦点事件
- `inputBlur` - 失去焦点事件
- `keyboardHeightChange` - 键盘高度变化事件
- `textLengthBeyondLimit` - 输入超出限制事件

## 方法

`TextArea` 支持 [Input 组件的所有方法](input.md#方法)。

常用方法包括：
- `setText()` - 设置文本内容
- `focus()` - 获取焦点
- `blur()` - 失去焦点
- `cursorIndex()` - 获取光标位置
- `setCursorIndex()` - 设置光标位置
- `getTextInputState()` - 获取文本输入状态
- `setTextInputState()` - 设置文本输入状态

## 完整示例

以下是一个使用 `TextArea` 实现表情插入功能的完整示例：

```kotlin
@Page("TextAreaDemo")
internal class TextAreaDemo : Pager() {
    private var inputState: TextInputState by observable(TextInputState(text = ""))
    private var inputRef: ViewRef<TextAreaView>? = null

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    flexDirectionColumn()
                }

                TextArea {
                    ref { ctx.inputRef = it }
                    attr {
                        flex(1f)
                        placeholder("输入文字或点击表情按钮")
                        fontSize(16f)
                        lineHeight(24f) // TextArea 特有属性
                        textPostProcessor("input")
                        textInputState(ctx.inputState)
                    }
                    event {
                        textInputStateChange { state ->
                            ctx.inputState = state
                        }
                        selectionChange { state ->
                            // 处理选区变化
                        }
                    }
                }

                Button {
                    attr { titleAttr { text("插入表情") } }
                    event {
                        click {
                            ctx.insertEmoji("[smile]")
                        }
                    }
                }
            }
        }
    }

    private fun insertEmoji(shortcode: String) {
        inputRef?.view?.getTextInputState { state ->
            val newState = state.replaceSelection(shortcode)
            inputRef?.view?.setTextInputState(newState)
        }
    }
}
```