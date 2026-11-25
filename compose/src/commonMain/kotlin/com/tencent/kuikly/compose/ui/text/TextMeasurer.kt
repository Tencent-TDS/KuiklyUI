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

package com.tencent.kuikly.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.text.toKuiklyFontWeight
import com.tencent.kuikly.compose.resources.toKuiklyFontFamily
import com.tencent.kuikly.compose.ui.KuiklyCanvasFont
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.font.FontListFontFamily
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.GenericFontFamily
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.core.views.FontStyle as KuiklyFontStyle
import com.tencent.kuikly.core.views.measureCanvasText
import kotlin.math.ceil

/** A tight single-line box in pixels. [firstBaseline] is estimated from the native line height. */
class CanvasTextLayoutResult internal constructor(
    val size: IntSize,
    val firstBaseline: Float,
    internal val text: String,
    internal val style: TextStyle,
    internal val font: KuiklyCanvasFont
)

/** Remembers a page-scoped measurer. Set [cacheSize] to zero to disable its layout cache. */
@Composable
fun rememberTextMeasurer(cacheSize: Int = 8): TextMeasurer {
    val pager = LocalActivity.current
    val density = LocalDensity.current.density
    val pageDensity = pager.pagerDensity()
    return remember(pager, density, pageDensity, cacheSize) {
        TextMeasurer(pager.pagerId, density, pageDensity, cacheSize)
    }
}

/** Measures single-line canvas labels on the Kuikly thread, including before the first draw. */
class TextMeasurer internal constructor(
    private val pagerId: String,
    private val density: Float,
    private val pageDensity: Float,
    private val cacheSize: Int
) {
    init {
        require(cacheSize >= 0) { "cacheSize must be non-negative" }
    }

    // Move a hit to the end so the first entry is always the least recently used.
    private val layouts = linkedMapOf<Pair<String, TextStyle>, CanvasTextLayoutResult>()

    /** A cache miss synchronously measures a native rich-text shadow. Reuse layouts across frames. */
    fun measure(text: String, style: TextStyle = TextStyle.Default): CanvasTextLayoutResult {
        requireSingleLine(text)
        val key = text to style
        layouts.remove(key)?.let {
            layouts[key] = it
            return it
        }

        val font = style.canvasFont(density)
        val layout = if (text.isEmpty()) {
            CanvasTextLayoutResult(IntSize.Zero, 0f, text, style, font)
        } else {
            val metrics = measureCanvasText(
                pagerId, text, font.sizePx / pageDensity, font.style, font.weight, font.family
            )
            val height = metrics.actualBoundingBoxAscent + metrics.actualBoundingBoxDescent
            CanvasTextLayoutResult(
                IntSize(ceil(metrics.width * pageDensity).toInt(), ceil(height * pageDensity).toInt()),
                metrics.actualBoundingBoxAscent * pageDensity,
                text, style, font
            )
        }
        if (cacheSize > 0) {
            if (layouts.size == cacheSize) layouts.remove(layouts.keys.first())
            layouts[key] = layout
        }
        return layout
    }
}

internal fun requireSingleLine(text: String) {
    require('\n' !in text && '\r' !in text) { "Canvas text must be a single line" }
}

internal fun TextStyle.canvasFont(density: Float): KuiklyCanvasFont {
    val sizeDp = when {
        fontSize.isSp -> fontSize.value
        fontSize.isEm -> 15f * fontSize.value
        else -> 15f
    }
    val family = when (val family = fontFamily) {
        is GenericFontFamily -> family.name
        is FontListFontFamily -> family.fonts.toKuiklyFontFamily()
        else -> ""
    }
    return KuiklyCanvasFont(
        sizePx = sizeDp * density,
        weight = fontWeight.toKuiklyFontWeight(),
        style = if (fontStyle == FontStyle.Italic) KuiklyFontStyle.ITALIC else KuiklyFontStyle.NORMAL,
        family = family
    )
}
