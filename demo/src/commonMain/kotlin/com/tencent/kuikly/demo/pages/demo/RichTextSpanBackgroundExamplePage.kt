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
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Per-span `backgroundColor` on a Kuikly DSL RichText.
 *
 * Same cases as the Compose page `RichTextSpanBackgroundDemo`; each case
 * states what should be visible so Android, iOS and OHOS can be compared by eye.
 */
@Page("RichTextSpanBackgroundExamplePage")
internal class RichTextSpanBackgroundExamplePage : BasePager() {

    private val highlight = Color(0xFFFFE08A)
    private val mint = Color(0xFFB9F6CA)
    private val sky = Color(0xFFBBDEFB)
    private val panel = Color(0xFFE0E0E0)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "RichText span backgroundColor"
                }
            }
            View {
                attr {
                    flex(1f)
                    padding(all = 16f)
                    backgroundColor(Color.WHITE)
                }

                caption("1. Single span. Expected: only the middle word carries a yellow background.")
                RichText {
                    attr { fontSize(18f) }
                    Span { text("plain ") }
                    Span {
                        text("highlighted")
                        backgroundColor(ctx.highlight)
                    }
                    Span { text(" plain") }
                }

                caption(
                    "2. Long span wrapping over several lines. Expected: the yellow background " +
                        "follows the span's glyphs on every line it occupies and stops at the " +
                        "span end; the trailing plain text has no background."
                )
                RichText {
                    attr {
                        fontSize(18f)
                        width(260f)
                    }
                    Span { text("Before the span. ") }
                    Span {
                        text(
                            "This span is long enough to wrap onto at least two or three lines " +
                                "at 18 in a 260 wide box, so the background must break and " +
                                "continue on the next line exactly where the glyphs are."
                        )
                        backgroundColor(ctx.highlight)
                    }
                    Span { text(" After the span, no background.") }
                }

                caption(
                    "3. RichText backgroundColor plus span backgroundColor. Expected: the whole " +
                        "box is grey; the inner spans paint green and blue on top of the grey, " +
                        "only behind their own glyphs; grey stays visible around them and in the " +
                        "line gaps."
                )
                RichText {
                    attr {
                        fontSize(18f)
                        width(260f)
                        backgroundColor(ctx.panel)
                    }
                    Span { text("grey box, ") }
                    Span {
                        text("green span")
                        backgroundColor(ctx.mint)
                    }
                    Span { text(", then ") }
                    Span {
                        text("bold blue span that also wraps to the next line")
                        fontWeightBold()
                        backgroundColor(ctx.sky)
                    }
                    Span { text(", grey again.") }
                }

                caption(
                    "4. Adjacent spans with different backgrounds and sizes. Expected: yellow, " +
                        "green and blue sit edge to edge with no gap and no overlap; the larger " +
                        "span's background is taller."
                )
                RichText {
                    attr { fontSize(18f) }
                    Span {
                        text("yellow")
                        backgroundColor(ctx.highlight)
                    }
                    Span {
                        text("GREEN")
                        fontSize(26f)
                        backgroundColor(ctx.mint)
                    }
                    Span {
                        text("blue")
                        backgroundColor(ctx.sky)
                    }
                }

                caption(
                    "5. Span background together with a span colour. Expected: white text on a " +
                        "blue background for the span; the surrounding text stays black on white."
                )
                RichText {
                    attr { fontSize(18f) }
                    Span { text("before ") }
                    Span {
                        text(" inverted ")
                        color(Color.WHITE)
                        backgroundColor(Color(0xFF1E88E5))
                    }
                    Span { text(" after") }
                }

                caption(
                    "6. Control: Text with a view-level backgroundColor and no spans. Expected: " +
                        "unchanged, the whole box is yellow."
                )
                Text {
                    attr {
                        fontSize(18f)
                        text("plain text with a view background")
                        backgroundColor(ctx.highlight)
                    }
                }
            }
        }
    }
}

/** Grey caption above each case describing the expected rendering. */
private fun ViewContainer<*, *>.caption(expected: String) {
    Text {
        attr {
            fontSize(12f)
            color(Color(0xFF616161))
            text(expected)
            marginTop(16f)
            marginBottom(6f)
        }
    }
}
