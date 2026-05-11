/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.TextAreaView
import com.tencent.kuikly.core.views.TextInputState
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("EmojiTextInputDemo")
internal class EmojiTextInputDemo : Pager() {

    private var inputState: TextInputState by observable(TextInputState(text = ""))
    private var previewText: String by observable("")
    private var textAreaRef: ViewRef<TextAreaView>? = null

    private val emojiShortcodes = listOf("[smile]", "[heart]", "[thumbup]", "[star]", "[fire]")
    private val emojiLabels  = listOf("😊", "❤️", "👍", "⭐", "🔥")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    backgroundColor(Color(0xFFF5F5F5))
                    flexDirectionColumn()
                }

                NavBar { attr { title = "自定义表情 Demo (自研DSL)" } }

                View {
                    attr {
                        marginTop(12f)
                        marginLeft(16f)
                        marginRight(16f)
                    }
                    Text {
                        attr {
                            text("点击表情按钮会替换当前选区或插入当前光标，raw text 保留短码。仅多行输入框（TextArea）支持表情预览，单行输入框（Input）暂不支持。")
                            color(Color(0xFF666666))
                            fontSize(14f)
                        }
                    }
                }

                View {
                    attr {
                        marginTop(12f)
                        marginLeft(16f)
                        marginRight(16f)
                        height(120f)
                        borderRadius(8f)
                        border(Border(1f, color = Color(0xFFDDDDDD), lineStyle = BorderStyle.SOLID))
                        backgroundColor(Color.WHITE)
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(8f)
                        paddingBottom(8f)
                    }
                    TextArea {
                        ref {
                            ctx.textAreaRef = it
                        }
                        attr {
                            flex(1f)
                            placeholder("输入文字或点击下方表情按钮")
                            fontSize(16f)
                            color(Color(0xFF333333))
                            textPostProcessor("input")
                            textInputState { ctx.inputState }
                        }
                        event {
                            textInputStateChange { state ->
                                ctx.inputState = state
                                ctx.previewText = state.text
                            }
                            selectionChange { state ->
                                ctx.inputState = state
                            }
                            textDidChange {
                                ctx.previewText = it.text
                            }
                        }
                    }
                }

                PreviewBlock("预览：") { ctx.previewText }

                View {
                    attr {
                        marginTop(16f)
                        marginLeft(16f)
                        marginRight(16f)
                        flexDirectionRow()
                    }
                    for (i in ctx.emojiShortcodes.indices) {
                        Button {
                            attr {
                                height(40f)
                                borderRadius(20f)
                                backgroundColor(Color.WHITE)
                                paddingLeft(12f)
                                paddingRight(12f)
                                marginRight(8f)
                                marginBottom(8f)
                                border(Border(1f, color = Color(0xFFDDDDDD), lineStyle = BorderStyle.SOLID))
                                titleAttr {
                                    text(ctx.emojiLabels[i] + " " + ctx.emojiShortcodes[i])
                                    fontSize(14f)
                                    color(Color(0xFF333333))
                                }
                            }
                            event {
                                click {
                                    ctx.insertEmoji(ctx.emojiShortcodes[i])
                                }
                            }
                        }
                    }
                }

                Button {
                    attr {
                        marginTop(24f)
                        marginLeft(16f)
                        marginRight(16f)
                        height(44f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFFFF4444))
                        titleAttr {
                            text("清空输入框")
                            fontSize(16f)
                            color(Color.WHITE)
                            fontWeightBold()
                        }
                    }
                    event {
                        click {
                            ctx.commitInputState(TextInputState(text = ""))
                        }
                    }
                }
            }
        }
    }

    private fun insertEmoji(shortcode: String) {
        commitInputState(inputState.replaceSelection(shortcode))
    }

    private fun commitInputState(state: TextInputState) {
        inputState = state
        previewText = state.text
    }
}

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

private fun ViewContainer<*, *>.PreviewBlock(title: String, textProvider: () -> String) {
    View {
        attr {
            marginTop(12f)
            marginLeft(16f)
            marginRight(16f)
            minHeight(48f)
            borderRadius(8f)
            backgroundColor(Color.WHITE)
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(12f)
            paddingBottom(12f)
        }
        Text {
            attr {
                val text = textProvider()
                text(title + if (text.isEmpty()) "（暂无内容）" else text)
                fontSize(16f)
                color(if (text.isEmpty()) Color(0xFFCCCCCC) else Color(0xFF333333))
                textPostProcessor("input")
            }
        }
    }
}
