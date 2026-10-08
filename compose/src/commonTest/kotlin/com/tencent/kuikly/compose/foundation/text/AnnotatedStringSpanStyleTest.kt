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

package com.tencent.kuikly.compose.foundation.text

import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.LinkAnnotation
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.core.views.RichTextAttr
import com.tencent.kuikly.core.views.TextConst
import com.tencent.kuikly.core.views.TextSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AnnotatedStringSpanStyleTest {

    @Test
    fun spanStyleFontFamilyIsSerializedOnTheSpan() {
        val text = AnnotatedString.Builder().apply {
            withStyle(SpanStyle(fontFamily = FontFamily.Serif)) {
                append("serif")
            }
            append(" plain")
        }.toAnnotatedString()

        val spans = lowerToTextSpans(text)
        assertEquals(2, spans.size)
        assertEquals("serif", spans[0].spanPropsMap()[TextConst.FONT_FAMILY])
        assertNull(spans[1].spanPropsMap()[TextConst.FONT_FAMILY])
    }

    @Test
    fun stylelessLinkPreservesInheritedFontFamilyAndWeight() {
        val text = AnnotatedString.Builder().apply {
            withStyle(SpanStyle(fontFamily = FontFamily.SansSerif)) {
                append("body ")
                withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)) {
                    append("bold")
                }
            }
        }.toAnnotatedString()
        val clickable = LinkAnnotation.Clickable(tag = "body", linkInteractionListener = {})
        val linked = AnnotatedString.Builder(text.length).apply {
            append(text)
            addLink(clickable, 0, text.length)
        }.toAnnotatedString()

        val spans = lowerToTextSpans(linked)
        assertEquals(2, spans.size)
        assertEquals("sans-serif", spans[0].spanPropsMap()[TextConst.FONT_FAMILY])
        assertEquals("serif", spans[1].spanPropsMap()[TextConst.FONT_FAMILY])
        assertEquals("700", spans[1].spanPropsMap()[TextConst.FONT_WEIGHT])
    }

    @Test
    fun nestedSpanWithoutFontFamilyInheritsOuterFontFamily() {
        val text = AnnotatedString.Builder().apply {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                append("code ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("bold")
                }
            }
        }.toAnnotatedString()

        val spans = lowerToTextSpans(text)
        assertEquals(2, spans.size)
        assertEquals("monospace", spans[0].spanPropsMap()[TextConst.FONT_FAMILY])
        assertEquals("monospace", spans[1].spanPropsMap()[TextConst.FONT_FAMILY])
        assertEquals("700", spans[1].spanPropsMap()[TextConst.FONT_WEIGHT])
    }

    @Test
    fun nestedSpanWithDefaultFontFamilyResetsOuterFontFamily() {
        val text = AnnotatedString.Builder().apply {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                append("code ")
                withStyle(SpanStyle(fontFamily = FontFamily.Default)) {
                    append("plain")
                }
            }
        }.toAnnotatedString()

        val spans = lowerToTextSpans(text)
        assertEquals(2, spans.size)
        assertEquals("monospace", spans[0].spanPropsMap()[TextConst.FONT_FAMILY])
        assertEquals("", spans[1].spanPropsMap()[TextConst.FONT_FAMILY])
    }

    private fun lowerToTextSpans(text: AnnotatedString): List<TextSpan> {
        val attr = RichTextAttr()
        attr.applyAnnotatedString(text, density = Density(1f))
        return attr.getSpans().map { assertIs<TextSpan>(it) }
    }
}
