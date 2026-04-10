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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("HeadIndentExamplePage")
internal class HeadIndentExamplePage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar {
                attr {
                    title = "HeadIndent Example"
                }
            }
            List {
                attr {
                    flex(1f)
                }

                // Section 1: 简单文本不同缩进值对比
                View {
                    attr {
                        margin(16f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color(0xFF333333))
                            text("1. 简单文本 - 不同缩进值对比")
                        }
                    }
                }

                // 无缩进
                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFF5F5F5))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            text("无缩进: 这是一段普通文本，没有设置首行缩进。可以看到文本从最左侧开始排列，没有任何缩进效果。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                // 缩进 16
                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFE3F2FD))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            firstLineHeadIndent(16f)
                            text("缩进16: 这是一段设置了 headIndent=16 的文本。首行应该有明显的缩进效果，而后续行保持正常的左对齐。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                // 缩进 32 (两个字符宽度)
                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFE8F5E9))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            firstLineHeadIndent(32f)
                            text("缩进32: 这是一段设置了 headIndent=32 的文本，相当于两个汉字的宽度。首行缩进更加明显，这是中文排版中常见的首行缩进两字的效果。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                // 缩进 48
                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFFFF3E0))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            firstLineHeadIndent(48f)
                            text("缩进48: 这是一段设置了 headIndent=48 的文本。缩进距离非常大，首行有明显的空白区域。这可以用于特殊的排版需求。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                // Section 2: 富文本缩进
                View {
                    attr {
                        margin(16f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color(0xFF333333))
                            text("2. 富文本 + 首行缩进")
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFFCE4EC))
                        borderRadius(8f)
                        padding(12f)
                    }
                    RichText {
                        attr {
                            firstLineHeadIndent(30f)
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFFE91E63))
                            fontWeightBold()
                            text("富文本缩进：")
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF333333))
                            text("这是一段富文本，第一个Span是粗体红色，后面是普通文本。整段文本的首行设置了 30 的缩进距离，可以验证富文本模式下缩进是否正常工作。")
                        }
                    }
                }

                // Section 3: 与其他属性组合
                View {
                    attr {
                        margin(16f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color(0xFF333333))
                            text("3. 与行高、对齐组合")
                        }
                    }
                }

                // 缩进 + 行高
                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFE8EAF6))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            firstLineHeadIndent(30f)
                            lineHeight(24f)
                            text("缩进+行高: 这是一段设置了 headIndent=30 且 lineHeight=24 的文本。可以验证首行缩进和行高设置是否可以正确组合使用，不会互相干扰。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                // 缩进 + 居中对齐
                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFF3E5F5))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            firstLineHeadIndent(30f)
                            textAlignCenter()
                            text("缩进+居中: 这是一段设置了 headIndent=30 且居中对齐的文本。可以观察缩进和居中对齐的组合效果。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                // Section 4: 多行限制 + 缩进
                View {
                    attr {
                        margin(16f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color(0xFF333333))
                            text("4. 多行限制 + 缩进")
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFE0F7FA))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            firstLineHeadIndent(30f)
                            lines(2)
                            textOverFlowTail()
                            text("2行+缩进: 这是一段很长的文本，设置了 headIndent=30 且限制最多显示2行，超出部分会以省略号结尾。首行应该有缩进效果，第二行正常左对齐，最后以...结尾。这段文本足够长确保能触发截断。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                // Section 5: 不同字号 + 缩进
                View {
                    attr {
                        margin(16f)
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            fontWeightBold()
                            color(Color(0xFF333333))
                            text("5. 不同字号 + 固定缩进")
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFF1F8E9))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            firstLineHeadIndent(24f)
                            text("12号字+缩进24: 小字号文本首行缩进效果。这段文本用较小的字号展示，缩进距离相同，可以对比不同字号下的视觉效果。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 16f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFF1F8E9))
                        borderRadius(8f)
                        padding(12f)
                    }
                    Text {
                        attr {
                            fontSize(20f)
                            firstLineHeadIndent(24f)
                            text("20号字+缩进24: 大字号文本首行缩进效果。同样的缩进距离，大字号下看起来缩进相对较小。")
                            color(Color(0xFF333333))
                        }
                    }
                }

                // 底部间距
                View {
                    attr {
                        height(40f)
                    }
                }
            }
        }
    }
}
