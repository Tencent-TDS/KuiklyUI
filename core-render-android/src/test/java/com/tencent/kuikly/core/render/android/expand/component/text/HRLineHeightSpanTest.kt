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

package com.tencent.kuikly.core.render.android.expand.component.text

import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Test

class HRLineHeightSpanTest {

    private fun metrics(top: Int, ascent: Int, descent: Int, bottom: Int) =
        Paint.FontMetricsInt().apply {
            this.top = top
            this.ascent = ascent
            this.descent = descent
            this.bottom = bottom
        }

    @Test
    fun oddLeadingDoesNotPushBaselineDown() {
        val fm = metrics(top = -13, ascent = -13, descent = 4, bottom = 4)

        HRLineHeightSpan(22).chooseHeight("", 0, 0, 0, 0, fm)

        assertEquals(-15, fm.top)
        assertEquals(-15, fm.ascent)
        assertEquals(7, fm.descent)
        assertEquals(7, fm.bottom)
        assertEquals(22, fm.bottom - fm.top)
    }

    @Test
    fun leadingIsDistributedAroundAscentDescentNotFontPadding() {
        // top/bottom carry extra font padding that ascent/descent do not.
        val fm = metrics(top = -18, ascent = -13, descent = 4, bottom = 6)

        HRLineHeightSpan(22).chooseHeight("", 0, 0, 0, 0, fm)

        assertEquals(-15, fm.top)
        assertEquals(-15, fm.ascent)
        assertEquals(7, fm.descent)
        assertEquals(7, fm.bottom)
        assertEquals(22, fm.bottom - fm.top)
    }

    @Test
    fun compressedLineHeightIgnoresFontPaddingExtents() {
        val fm = metrics(top = -18, ascent = -13, descent = 4, bottom = 6)

        HRLineHeightSpan(14).chooseHeight("", 0, 0, 0, 0, fm)

        assertEquals(-12, fm.top)
        assertEquals(-12, fm.ascent)
        assertEquals(2, fm.descent)
        assertEquals(2, fm.bottom)
        assertEquals(14, fm.bottom - fm.top)
    }

    @Test
    fun exactLineHeightIsKeptForOddHeights() {
        val fm = metrics(top = -10, ascent = -10, descent = 3, bottom = 3)

        HRLineHeightSpan(17).chooseHeight("x", 0, 1, 0, 17, fm)

        assertEquals(17, fm.bottom - fm.top)
        assertEquals(fm.top, fm.ascent)
        assertEquals(fm.bottom, fm.descent)
    }

    @Test
    fun sameMetricsResolveToSameLineBoxRegardlessOfText() {
        // The line box must depend on font metrics only, never on the measured
        // string, so typing an ascender glyph cannot re-center an editable line.
        val span = HRLineHeightSpan(20)
        val short = metrics(top = -12, ascent = -12, descent = 4, bottom = 4)
        val tall = metrics(top = -12, ascent = -12, descent = 4, bottom = 4)

        span.chooseHeight("as", 0, 2, 0, 20, short)
        span.chooseHeight("asf", 0, 3, 0, 20, tall)

        assertEquals(short.top, tall.top)
        assertEquals(short.ascent, tall.ascent)
        assertEquals(short.descent, tall.descent)
        assertEquals(short.bottom, tall.bottom)
        assertEquals(20, short.bottom - short.top)
    }
}
