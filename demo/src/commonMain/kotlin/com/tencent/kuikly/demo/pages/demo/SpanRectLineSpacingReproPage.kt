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
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.PlaceholderSpan
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * PlaceholderSpan + lineSpacing 复现页：
 * 1) 单段折行：对比非末行 / 末行 spanFrame.y（lineSpacing 10 vs 0）
 * 2) 多段：段1末行（非全文末行）与全文末行，核对 isLastLine 按整篇判定是否与 Android 一致
 */
@Page("SpanRectLineSpacingRepro")
internal class SpanRectLineSpacingReproPage : BasePager() {

    private var spacedFirst by observable(Frame.zero)
    private var spacedLast by observable(Frame.zero)
    private var plainFirst by observable(Frame.zero)
    private var plainLast by observable(Frame.zero)
    private var spacedParaEnd by observable(Frame.zero)
    private var spacedDocEnd by observable(Frame.zero)
    private var plainParaEnd by observable(Frame.zero)
    private var plainDocEnd by observable(Frame.zero)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            Scroller {
                attr {
                    flex(1f)
                    padding(top = ctx.pageData.statusBarHeight + 20f, left = 16f, right = 16f, bottom = 24f)
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color.BLACK)
                        text(
                            "单段  lineSpacing=10  首行 y=${ctx.spacedFirst.y}  末行 y=${ctx.spacedLast.y}\n" +
                                "单段  lineSpacing=0   首行 y=${ctx.plainFirst.y}  末行 y=${ctx.plainLast.y}\n" +
                                "单段  首行 Δ(10 − 0) = ${ctx.spacedFirst.y - ctx.plainFirst.y}\n" +
                                "单段  末行 Δ(10 − 0) = ${ctx.spacedLast.y - ctx.plainLast.y}\n" +
                                "多段  段末(非文末) Δ = ${ctx.spacedParaEnd.y - ctx.plainParaEnd.y}\n" +
                                "多段  文末 Δ = ${ctx.spacedDocEnd.y - ctx.plainDocEnd.y}"
                        )
                    }
                }
                ctx.reproBlock(this, 10f, { ctx.spacedFirst = it }, { ctx.spacedLast = it }, { ctx.spacedFirst }, { ctx.spacedLast })
                ctx.reproBlock(this, 0f, { ctx.plainFirst = it }, { ctx.plainLast = it }, { ctx.plainFirst }, { ctx.plainLast })
                ctx.multiParaBlock(this, 10f, { ctx.spacedParaEnd = it }, { ctx.spacedDocEnd = it }, { ctx.spacedParaEnd }, { ctx.spacedDocEnd })
                ctx.multiParaBlock(this, 0f, { ctx.plainParaEnd = it }, { ctx.plainDocEnd = it }, { ctx.plainParaEnd }, { ctx.plainDocEnd })
            }
        }
    }

    private fun reproBlock(
        container: ViewContainer<*, *>,
        spacing: Float,
        onFirst: (Frame) -> Unit,
        onLast: (Frame) -> Unit,
        first: () -> Frame,
        last: () -> Frame,
    ) {
        container.View {
            attr {
                marginTop(20f)
                width(300f)
            }
            RichText {
                attr {
                    fontSize(18f)
                    color(Color.BLACK)
                    backgroundColor(Color(0xFFEEEEEE))
                    if (spacing > 0f) {
                        lineSpacing(spacing)
                    }
                }
                Span { text("首行") }
                PlaceholderSpan {
                    placeholderSize(24f, 24f)
                    spanFrameDidChanged { onFirst(it) }
                }
                Span { text("后面的文字足够长，保证这一段会自动折行到第二行，再继续写一些内容到第三行，末行") }
                PlaceholderSpan {
                    placeholderSize(24f, 24f)
                    spanFrameDidChanged { onLast(it) }
                }
            }
            View {
                attr {
                    absolutePosition(top = first().y, left = first().x)
                    size(first().width, first().height)
                    backgroundColor(Color(0xAAFF3333))
                }
            }
            View {
                attr {
                    absolutePosition(top = last().y, left = last().x)
                    size(last().width, last().height)
                    backgroundColor(Color(0xAA3366FF))
                }
            }
        }
    }

    /** 两段：绿块 = 第一段末行（非全文末行），橙块 = 全文末行。 */
    private fun multiParaBlock(
        container: ViewContainer<*, *>,
        spacing: Float,
        onParaEnd: (Frame) -> Unit,
        onDocEnd: (Frame) -> Unit,
        paraEnd: () -> Frame,
        docEnd: () -> Frame,
    ) {
        container.View {
            attr {
                marginTop(20f)
                width(300f)
            }
            RichText {
                attr {
                    fontSize(18f)
                    color(Color.BLACK)
                    backgroundColor(Color(0xFFEEEEEE))
                    if (spacing > 0f) {
                        lineSpacing(spacing)
                    }
                }
                Span { text("段一末") }
                PlaceholderSpan {
                    placeholderSize(24f, 24f)
                    spanFrameDidChanged { onParaEnd(it) }
                }
                Span { text("\n段二末") }
                PlaceholderSpan {
                    placeholderSize(24f, 24f)
                    spanFrameDidChanged { onDocEnd(it) }
                }
            }
            View {
                attr {
                    absolutePosition(top = paraEnd().y, left = paraEnd().x)
                    size(paraEnd().width, paraEnd().height)
                    backgroundColor(Color(0xAA33AA55))
                }
            }
            View {
                attr {
                    absolutePosition(top = docEnd().y, left = docEnd().x)
                    size(docEnd().width, docEnd().height)
                    backgroundColor(Color(0xAAFF8800))
                }
            }
        }
    }
}
