/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.LengthLimitType
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("maxLength")
internal class MaxTextLengthDemoPage : BasePager() {
    var byteInputText by observable("")
    var byteInputLength by observable(0)
    var characterInputText by observable("")
    var characterInputLength by observable(0)
    var visualWidthInputText by observable("")
    var visualWidthInputLength by observable(0)
    var byteTextAreaText by observable("")
    var byteTextAreaLength by observable(0)
    var characterTextAreaText by observable("")
    var characterTextAreaLength by observable(0)
    var visualWidthTextAreaText by observable("")
    var visualWidthTextAreaLength by observable(0)
    var imeHeight by observable(0f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "maxTextLength接口Demo"
                }
            }

            Scroller {
                attr {
                    flex(1f)
                }
                View {
                    attr {
                        justifyContentCenter()
                        backgroundColor(Color(0xFFC9C9C9L))
                        height(32f)
                        paddingLeft(16f)
                        paddingRight(16f)
                    }
                    Text {
                        attr {
                            color(Color.BLACK)
                            fontSize(14f)
                            text("说明")
                        }
                    }
                }
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            text(
                                """
                                maxTextLength接口支持三种长度限制类型：
                                1. BYTE - 按UTF-8字节数限制
                                2. CHARACTER - 按字符个数限制
                                3. VISUAL_WIDTH - 按视觉宽度限制
                                
                                示例对比：
                                • "a" - BYTE:1, CHARACTER:1, VISUAL_WIDTH:1
                                • "中" - BYTE:3, CHARACTER:1, VISUAL_WIDTH:2
                                • "😂" - BYTE:4, CHARACTER:1, VISUAL_WIDTH:2
                                """.trimIndent()
                            )
                            lineHeight(20f)
                        }
                    }
                }

                // ========== Input 组件示例 ==========
                View {
                    attr {
                        justifyContentCenter()
                        backgroundColor(Color(0xFFC9C9C9L))
                        height(32f)
                        paddingLeft(16f)
                        paddingRight(16f)
                    }
                    Text {
                        attr {
                            color(Color.BLACK)
                            fontSize(14f)
                            text("Input 组件示例")
                        }
                    }
                }

                // 未指定限制类型
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            text("0. 未指定限制类型 maxTextLength(10)")
                            marginBottom(10f)
                        }
                    }
                    Input {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(50f)
                            fontSize(16f)
                            margin(10f)
                            borderRadius(4f)
                            border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            placeholder("请输入文本")
                            maxTextLength(10)
                        }
                    }
                }

                // BYTE 类型
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            text("1. BYTE 类型限制")
                            marginBottom(10f)
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF666666))
                            text("按UTF-8字节数计算")
                            marginBottom(8f)
                        }
                    }
                    Input {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(50f)
                            fontSize(16f)
                            margin(10f)
                            borderRadius(4f)
                            border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            placeholder("请输入文本（按字节限制）")
                            maxTextLength(10, LengthLimitType.BYTE)
                        }
                        event {
                            textDidChange {
                                ctx.byteInputText = it.text
                                ctx.byteInputLength = it.length ?: -1
                            }
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            text("字节数: ${ctx.byteInputLength}/10")
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            marginTop(8f)
                            text("当前输入: ${ctx.byteInputText}")
                        }
                    }
                }

                // CHARACTER 类型
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            text("2. CHARACTER 类型限制")
                            marginBottom(10f)
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF666666))
                            text("按字符个数计算，中文、英文、emoji都算1个字符")
                            marginBottom(8f)
                        }
                    }
                    Input {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(50f)
                            fontSize(16f)
                            margin(10f)
                            borderRadius(4f)
                            border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            placeholder("请输入文本（按字符限制）")
                            maxTextLength(10, LengthLimitType.CHARACTER)
                        }
                        event {
                            textDidChange {
                                ctx.characterInputText = it.text
                                ctx.characterInputLength = it.length ?: -1
                            }
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            text("字符数: ${ctx.characterInputLength}/10")
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            marginTop(8f)
                            text("当前输入: ${ctx.characterInputText}")
                        }
                    }
                }

                // VISUAL_WIDTH 类型
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            text("3. VISUAL_WIDTH 类型限制")
                            marginBottom(10f)
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF666666))
                            text("按视觉宽度计算，英文=1，中文/emoji=2")
                            marginBottom(8f)
                        }
                    }
                    Input {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(50f)
                            fontSize(16f)
                            margin(10f)
                            borderRadius(4f)
                            border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            placeholder("请输入文本（按视觉宽度限制）")
                            maxTextLength(10, LengthLimitType.VISUAL_WIDTH)
                        }
                        event {
                            textDidChange {
                                ctx.visualWidthInputText = it.text
                                ctx.visualWidthInputLength = it.length ?: -1
                            }
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            text("视觉宽度: ${ctx.visualWidthInputLength}/10")
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            marginTop(8f)
                            text("当前输入: ${ctx.visualWidthInputText}")
                        }
                    }
                }

                // ========== TextArea 组件示例 ==========
                View {
                    attr {
                        justifyContentCenter()
                        backgroundColor(Color(0xFFC9C9C9L))
                        height(32f)
                        paddingLeft(16f)
                        paddingRight(16f)
                    }
                    Text {
                        attr {
                            color(Color.BLACK)
                            fontSize(14f)
                            text("TextArea 组件示例")
                        }
                    }
                }

                // 未指定限制类型 TextArea
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            text("0. 未指定限制类型 maxTextLength(20)")
                            marginBottom(10f)
                        }
                    }
                    TextArea {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(100f)
                            fontSize(16f)
                            margin(10f)
                            borderRadius(4f)
                            border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            placeholder("请输入多行文本")
                            maxTextLength(20)
                        }
                    }
                }

                // BYTE 类型 TextArea
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            text("1. BYTE 类型限制")
                            marginBottom(10f)
                        }
                    }
                    TextArea {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(100f)
                            fontSize(16f)
                            margin(10f)
                            borderRadius(4f)
                            border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            placeholder("请输入多行文本（按字节限制）")
                            maxTextLength(20, LengthLimitType.BYTE)
                        }
                        event {
                            textDidChange {
                                ctx.byteTextAreaText = it.text
                                ctx.byteTextAreaLength = it.length ?: -1
                            }
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            marginTop(8f)
                            text("字节数: ${ctx.byteTextAreaLength}/20")
                        }
                    }
                }

                // CHARACTER 类型 TextArea
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            text("2. CHARACTER 类型限制")
                            marginBottom(10f)
                        }
                    }
                    TextArea {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(100f)
                            fontSize(16f)
                            margin(10f)
                            borderRadius(4f)
                            border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            placeholder("请输入多行文本（按字符限制）")
                            maxTextLength(20, LengthLimitType.CHARACTER)
                        }
                        event {
                            textDidChange {
                                ctx.characterTextAreaText = it.text
                                ctx.characterTextAreaLength = it.length ?: -1
                            }
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            marginTop(8f)
                            text("字符数: ${ctx.characterTextAreaLength}/20")
                        }
                    }
                }

                // VISUAL_WIDTH 类型 TextArea
                View {
                    attr {
                        padding(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            text("3. VISUAL_WIDTH 类型限制")
                            marginBottom(10f)
                        }
                    }
                    TextArea {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(100f)
                            fontSize(16f)
                            margin(10f)
                            borderRadius(4f)
                            border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                            placeholder("请输入多行文本（按视觉宽度限制）")
                            maxTextLength(20, LengthLimitType.VISUAL_WIDTH)
                        }
                        event {
                            textDidChange {
                                ctx.visualWidthTextAreaText = it.text
                                ctx.visualWidthTextAreaLength = it.length ?: -1
                            }
                            keyboardHeightChange {
                                ctx.imeHeight = it.height
                            }
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            marginTop(8f)
                            text("视觉宽度: ${ctx.visualWidthTextAreaLength}/20")
                        }
                    }
                }
            }
            View {
                attr {
                    height(ctx.imeHeight)
                }
            }
        }
    }
}

