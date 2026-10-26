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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * Per-span background colour on an AnnotatedString (`SpanStyle(background = ...)`).
 *
 * Every case states what should be visible so the page can be compared across
 * Android, iOS and OHOS by eye. Before the renderer fix the span background was
 * silently dropped on all three platforms.
 */
@Page("RichTextSpanBackgroundDemo")
class RichTextSpanBackgroundDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("RichText span background") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                ) {
                    item { SpanBackgroundCases() }
                }
            }
        }
    }
}

private val Highlight = Color(0xFFFFE08A)
private val Mint = Color(0xFFB9F6CA)
private val Sky = Color(0xFFBBDEFB)
private val PanelBackground = Color(0xFFE0E0E0)

@Composable
private fun SpanBackgroundCases() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Case("1. Single span. Expected: only the middle word carries a yellow background.") {
            Text(
                buildAnnotatedString {
                    append("plain ")
                    withStyle(SpanStyle(background = Highlight)) { append("highlighted") }
                    append(" plain")
                },
                fontSize = 18.sp,
            )
        }

        Case(
            "2. Long span wrapping over several lines. Expected: the yellow background follows " +
                "the span's glyphs on every line it occupies and stops at the span end; the " +
                "trailing plain text has no background.",
        ) {
            Text(
                buildAnnotatedString {
                    append("Before the span. ")
                    withStyle(SpanStyle(background = Highlight)) {
                        append(
                            "This span is long enough to wrap onto at least two or three lines " +
                                "at 18sp in a 260dp wide column, so the background must break " +
                                "and continue on the next line exactly where the glyphs are.",
                        )
                    }
                    append(" After the span, no background.")
                },
                fontSize = 18.sp,
                modifier = Modifier.width(260.dp),
            )
        }

        Case(
            "3. Text background plus span background. Expected: the whole text box is grey; " +
                "the two inner spans paint green and blue on top of the grey, only behind " +
                "their own glyphs; grey stays visible around them and in the line gaps.",
        ) {
            Text(
                buildAnnotatedString {
                    append("grey box, ")
                    withStyle(SpanStyle(background = Mint)) { append("green span") }
                    append(", then ")
                    withStyle(SpanStyle(background = Sky, fontWeight = FontWeight.Bold)) {
                        append("bold blue span that also wraps to the next line")
                    }
                    append(", grey again.")
                },
                fontSize = 18.sp,
                modifier = Modifier.width(260.dp).background(PanelBackground).padding(8.dp),
            )
        }

        Case(
            "4. Adjacent spans with different backgrounds and sizes. Expected: yellow, green " +
                "and blue sit edge to edge with no gap and no overlap; the larger span's " +
                "background is taller.",
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(background = Highlight)) { append("yellow") }
                    withStyle(SpanStyle(background = Mint, fontSize = 26.sp)) { append("GREEN") }
                    withStyle(SpanStyle(background = Sky)) { append("blue") }
                },
                fontSize = 18.sp,
            )
        }

        Case(
            "5. Span background together with a span colour. Expected: white text on a blue " +
                "background for the span; the surrounding text stays black on white.",
        ) {
            Text(
                buildAnnotatedString {
                    append("before ")
                    withStyle(SpanStyle(background = Color(0xFF1E88E5), color = Color.White)) {
                        append(" inverted ")
                    }
                    append(" after")
                },
                fontSize = 18.sp,
            )
        }

        Case("6. Control: view-level background only, no span background. Expected: unchanged, the whole box is yellow.") {
            Text(
                "plain text with a view background",
                fontSize = 18.sp,
                modifier = Modifier.background(Highlight),
            )
        }
    }
}

@Composable
private fun Case(expected: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(expected, fontSize = 12.sp, color = Color(0xFF616161))
        content()
    }
}
