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
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * PlaceholderSpan + lineSpacing 复现页：同一段内容分别以 lineSpacing = 10 / 0 排版，
 * 对比首行（非末行）与末行占位框回传的 spanFrame.y。
 */
@Page("SpanRectLineSpacingRepro")
internal class SpanRectLineSpacingReproPage : BasePager() {

    private var spacedFirst by observable(Frame.zero)
    private var spacedLast by observable(Frame.zero)
    private var plainFirst by observable(Frame.zero)
    private var plainLast by observable(Frame.zero)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                padding(top = ctx.pageData.statusBarHeight + 20f, left = 16f, right = 16f)
            }
            Text {
                attr {
                    fontSize(14f)
                    color(Color.BLACK)
                    text(
                        "lineSpacing=10  首行 y=${ctx.spacedFirst.y}  末行 y=${ctx.spacedLast.y}\n" +
                            "lineSpacing=0   首行 y=${ctx.plainFirst.y}  末行 y=${ctx.plainLast.y}\n" +
                            "首行 Δ(10 − 0) = ${ctx.spacedFirst.y - ctx.plainFirst.y}\n" +
                            "末行 Δ(10 − 0) = ${ctx.spacedLast.y - ctx.plainLast.y}"
                    )
                }
            }
            ctx.reproBlock(this, 10f, { ctx.spacedFirst = it }, { ctx.spacedLast = it }, { ctx.spacedFirst }, { ctx.spacedLast })
            ctx.reproBlock(this, 0f, { ctx.plainFirst = it }, { ctx.plainLast = it }, { ctx.plainFirst }, { ctx.plainLast })
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
}
