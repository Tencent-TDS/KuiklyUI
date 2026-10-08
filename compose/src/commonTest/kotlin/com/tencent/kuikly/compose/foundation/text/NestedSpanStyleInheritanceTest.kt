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

import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shadow
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.views.RichTextAttr
import com.tencent.kuikly.core.views.TextConst
import com.tencent.kuikly.core.views.TextSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Overlapping spans are lowered onto one TextSpan in order. A property the
 * inner span leaves unset must keep the value written by the enclosing span.
 */
class NestedSpanStyleInheritanceTest {

    @Test
    fun innerSpanWithoutFontWeightInheritsOuterFontWeight() {
        val spans = lower {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("bold ")
                withStyle(SpanStyle(fontSize = 20.sp)) { append("big") }
            }
        }
        assertEquals("700", spans[0].spanPropsMap()[TextConst.FONT_WEIGHT])
        assertEquals("700", spans[1].spanPropsMap()[TextConst.FONT_WEIGHT])
    }

    @Test
    fun innerSpanWithExplicitNormalWeightResetsOuterFontWeight() {
        val spans = lower {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("bold ")
                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append("plain") }
            }
        }
        assertEquals("700", spans[0].spanPropsMap()[TextConst.FONT_WEIGHT])
        assertEquals("400", spans[1].spanPropsMap()[TextConst.FONT_WEIGHT])
    }

    @Test
    fun innerSpanWithoutFontStyleInheritsOuterItalic() {
        val spans = lower {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append("italic ")
                withStyle(SpanStyle(color = Color.Red)) { append("red") }
            }
        }
        assertEquals("italic", spans[0].spanPropsMap()[TextConst.FONT_STYLE])
        assertEquals("italic", spans[1].spanPropsMap()[TextConst.FONT_STYLE])
    }

    @Test
    fun innerSpanWithoutShadowInheritsOuterShadow() {
        val shadow = Shadow(color = Color.Blue, offset = Offset(1f, 2f), blurRadius = 3f)
        val spans = lower {
            withStyle(SpanStyle(shadow = shadow)) {
                append("shadow ")
                withStyle(SpanStyle(fontSize = 20.sp)) { append("big") }
            }
        }
        val outer = spans[0].spanPropsMap()[TextConst.TEXT_SHADOW]
        assertNotEquals(null, outer)
        assertEquals(outer, spans[1].spanPropsMap()[TextConst.TEXT_SHADOW])
    }

    @Test
    fun innerSpanWithoutColorInheritsOuterColor() {
        val spans = lower {
            withStyle(SpanStyle(color = Color.Red)) {
                append("red ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bold") }
            }
        }
        val outer = spans[0].spanPropsMap()[TextConst.TEXT_COLOR]
        assertNotEquals(null, outer)
        assertEquals(outer, spans[1].spanPropsMap()[TextConst.TEXT_COLOR])
        assertEquals("700", spans[1].spanPropsMap()[TextConst.FONT_WEIGHT])
    }

    @Test
    fun spanWithoutAnyOfThesePropertiesWritesNone() {
        val spans = lower {
            withStyle(SpanStyle(fontSize = 20.sp)) { append("big") }
        }
        val props = spans[0].spanPropsMap()
        assertNull(props[TextConst.FONT_WEIGHT])
        assertNull(props[TextConst.FONT_STYLE])
        assertNull(props[TextConst.TEXT_SHADOW])
        assertNull(props[TextConst.TEXT_COLOR])
    }

    private fun lower(build: AnnotatedString.Builder.() -> Unit): List<TextSpan> {
        val text = AnnotatedString.Builder().apply(build).toAnnotatedString()
        val attr = RichTextAttr()
        attr.applyAnnotatedString(text, density = Density(1f))
        return attr.getSpans().map { assertIs<TextSpan>(it) }
    }
}
