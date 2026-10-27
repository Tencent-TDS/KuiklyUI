/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Manual OHOS regression page: toggle width repeatedly and check glyphs, alignment,
 * and the position of the following label. minWidth exercises height changes within
 * one measure; it must not require a second Pager layout to place the label correctly.
 * Absolute left/right samples must align to the final background, including when the
 * parent width comes from a sibling rather than an explicit width.
 */
@Page("OhosTextLayoutRepro")
internal class OhosTextLayoutReproPage : BasePager() {
    private var narrow by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        val sample = "多行中文应完整显示，右侧对齐，反复切换宽度后不能漏字、错行或重叠。"
        return {
            NavBar { attr { title = "Text layout" } }
            Scroller {
                attr { flex(1f); padding(16f) }
                Text {
                    attr {
                        text("点击切换宽度：${if (ctx.narrow) 180 else 280}")
                        fontSize(18f)
                        marginBottom(16f)
                        backgroundColor(Color(0xFFE0EFFF))
                    }
                    event { click { ctx.narrow = !ctx.narrow } }
                }
                View {
                    attr {
                        width(if (ctx.narrow) 180f else 280f)
                        flexDirectionRow()
                        marginBottom(12f)
                    }
                    Text { attr { text("flex"); width(36f) } }
                    Text {
                        attr {
                            flex(1f)
                            text(sample)
                            fontSize(18f)
                            textAlignRight()
                            backgroundColor(Color(0xFFFFF1B8))
                        }
                    }
                }
                View {
                    attr { width(if (ctx.narrow) 180f else 280f); marginBottom(12f) }
                    Text {
                        attr {
                            text("较长的第一行\n短行")
                            textAlignRight()
                            backgroundColor(Color(0xFFFFF1B8))
                        }
                    }
                    Text {
                        attr {
                            text("较长的第一行\n短行")
                            textAlignCenter()
                            backgroundColor(Color(0xFFD9F7BE))
                        }
                    }
                }
                View {
                    // The child expands beyond the initial 120 vp measurement constraint.
                    attr { width(120f); marginBottom(12f) }
                    Text {
                        attr {
                            minWidth(if (ctx.narrow) 120f else 240f)
                            text(sample)
                            fontSize(18f)
                            textAlignRight()
                            backgroundColor(Color(0xFFFFF1B8))
                        }
                    }
                    Text { attr { text("这一行应紧随正文，无重叠或旧高度空隙") } }
                }
                View {
                    attr {
                        width(if (ctx.narrow) 220f else 300f)
                        height(72f)
                        marginBottom(12f)
                    }
                    Text {
                        attr {
                            absolutePosition(top = 0f, left = 0f, right = 0f)
                            text("短文本")
                            fontSize(18f)
                            textAlignRight()
                            backgroundColor(Color(0xFFFFF1B8))
                        }
                    }
                }
                View {
                    attr {
                        width(if (ctx.narrow) 220f else 300f)
                        height(72f)
                        marginBottom(12f)
                    }
                    Text {
                        attr {
                            absolutePosition(top = 0f, left = 0f, right = 0f)
                            text("短文本")
                            fontSize(18f)
                            textAlignCenter()
                            backgroundColor(Color(0xFFD9F7BE))
                        }
                    }
                }
                View {
                    // Width comes from the relative sibling after that sibling is measured.
                    attr {
                        alignSelfFlexStart()
                        height(120f)
                        marginBottom(12f)
                    }
                    Text {
                        attr {
                            text(if (ctx.narrow) "短父宽" else "相对布局这一行撑开父宽度")
                            fontSize(18f)
                            backgroundColor(Color(0xFFE0EFFF))
                        }
                    }
                    Text {
                        attr {
                            absolutePosition(top = 32f, left = 0f, right = 0f)
                            text(sample)
                            fontSize(16f)
                            textAlignRight()
                            backgroundColor(Color(0xFFFFF1B8))
                        }
                    }
                }
                View {
                    attr { width(if (ctx.narrow) 180f else 280f) }
                    RichText {
                        attr {
                            alignSelfStretch()
                            textAlignRight()
                            fontSize(18f)
                            backgroundColor(Color(0xFFD9F7BE))
                        }
                        Span { text("富文本："); color(Color.BLUE) }
                        Span { text(sample) }
                    }
                    Text {
                        attr {
                            alignSelfStretch()
                            text(sample + sample)
                            textAlignCenter()
                            lines(2)
                            marginTop(12f)
                            backgroundColor(Color(0xFFFFF1B8))
                        }
                    }
                }
            }
        }
    }
}
