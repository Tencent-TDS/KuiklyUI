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

package com.tencent.kuikly.compose.ui.graphics.drawscope

import com.tencent.kuikly.compose.resources.toKuiklyFontFamily
import com.tencent.kuikly.compose.ui.KuiklyCanvas
import com.tencent.kuikly.compose.ui.KuiklyCanvasFont
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.isSpecified
import com.tencent.kuikly.compose.ui.graphics.KuiklyPaint
import com.tencent.kuikly.compose.ui.graphics.Paint
import com.tencent.kuikly.compose.ui.graphics.PaintingStyle
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontListFontFamily
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.font.GenericFontFamily
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.isSpecified
import com.tencent.kuikly.core.views.FontStyle as KuiklyFontStyle
import com.tencent.kuikly.core.views.FontWeight as KuiklyFontWeight
import com.tencent.kuikly.core.views.TextAlign as KuiklyTextAlign

/**
 * Font size used by [drawText] and [measureText] when [TextStyle.fontSize] is unspecified,
 * in density-independent pixels. Matches the renderer canvas default.
 */
private const val DefaultCanvasFontSizeDp = 15f

/**
 * Measured extent of a single line of canvas text, in pixels.
 *
 * @param width advance width of the run
 * @param ascent distance from the alphabetic baseline up to the top of the line box
 * @param descent distance from the alphabetic baseline down to the bottom of the line box
 */
class CanvasTextMetrics(val width: Float, val ascent: Float, val descent: Float) {
    /** Total line box height. */
    val height: Float get() = ascent + descent

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CanvasTextMetrics) return false
        return width == other.width && ascent == other.ascent && descent == other.descent
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + ascent.hashCode()
        result = 31 * result + descent.hashCode()
        return result
    }

    override fun toString(): String =
        "CanvasTextMetrics(width=$width, ascent=$ascent, descent=$descent)"
}

/**
 * Draws a single line of [text] through the Kuikly canvas text primitive.
 *
 * This is the Kuikly counterpart of `drawText(TextMeasurer, ...)`: the renderer shapes and
 * draws the text itself, so there is no `TextLayoutResult`. Semantics follow the HTML canvas
 * `fillText`/`strokeText` model:
 *
 * - [origin] is the alphabetic baseline point in pixels, not the top-left corner. Use
 *   [measureText] when the text has to be positioned by its box.
 * - [TextStyle.textAlign] picks which side of `origin.x` the run extends to:
 *   [TextAlign.Center] centers on it, [TextAlign.End]/[TextAlign.Right] end on it, anything
 *   else starts on it. Right-to-left layout direction is not consulted.
 * - [TextStyle.fontSize] is interpreted like the Kuikly `Text` composable: an `sp` value is
 *   scaled by [DrawScope.density] (font scale is not applied); unspecified falls back to the
 *   renderer default of 15dp.
 * - Only [TextStyle.color], [TextStyle.fontSize], [TextStyle.fontWeight],
 *   [TextStyle.fontStyle], [TextStyle.fontFamily] and [TextStyle.textAlign] are honored.
 *   Line breaks are not laid out; split multi-line content into separate calls.
 *
 * @param text the line to draw
 * @param origin baseline point, in pixels, relative to the current transform
 * @param style typography and color of the run; an unspecified color draws black
 * @param alpha opacity applied to the text color, 0f (transparent) to 1f (opaque)
 * @param drawStyle [Fill] fills the glyphs, a [Stroke] outlines them with its width
 */
fun DrawScope.drawText(
    text: String,
    origin: Offset = Offset.Zero,
    style: TextStyle = TextStyle.Default,
    alpha: Float = 1f,
    drawStyle: DrawStyle = Fill
) {
    if (text.isEmpty()) return
    val canvas = drawContext.canvas as? KuiklyCanvas ?: return
    val paint = textPaint(style.color, alpha, drawStyle)
    canvas.drawText(text, origin, canvasFont(style), style.textAlign.toKuikly(), paint)
}

/**
 * Measures a single line of [text] as [drawText] would draw it with [style], or null when
 * called outside a draw pass (the Kuikly canvas is not bound to a native view).
 */
fun DrawScope.measureText(
    text: String,
    style: TextStyle = TextStyle.Default
): CanvasTextMetrics? {
    val canvas = drawContext.canvas as? KuiklyCanvas ?: return null
    val metrics = canvas.measureText(text, canvasFont(style)) ?: return null
    val densityValue = canvas.density()
    return CanvasTextMetrics(
        width = metrics.width * densityValue,
        ascent = metrics.actualBoundingBoxAscent * densityValue,
        descent = metrics.actualBoundingBoxDescent * densityValue
    )
}

private fun DrawScope.canvasFont(style: TextStyle): KuiklyCanvasFont {
    val fontSize = style.fontSize
    val sizePx = when {
        fontSize.isSpecified && fontSize.isSp -> fontSize.value * density
        fontSize.isSpecified && fontSize.isEm -> DefaultCanvasFontSizeDp * density * fontSize.value
        else -> DefaultCanvasFontSizeDp * density
    }
    return KuiklyCanvasFont(
        sizePx = sizePx,
        weight = style.fontWeight.toKuikly(),
        style = if (style.fontStyle == FontStyle.Italic) KuiklyFontStyle.ITALIC else KuiklyFontStyle.NORMAL,
        family = style.fontFamily.toKuiklyName()
    )
}

private fun textPaint(color: Color, alpha: Float, drawStyle: DrawStyle): Paint =
    KuiklyPaint().apply {
        this.color = if (color.isSpecified) color else Color.Black
        this.alpha = alpha
        when (drawStyle) {
            Fill -> style = PaintingStyle.Fill
            is Stroke -> {
                style = PaintingStyle.Stroke
                strokeWidth = drawStyle.width
                strokeCap = drawStyle.cap
                strokeMiterLimit = drawStyle.miter
            }
        }
    }

// The renderer canvases accept the same numeric weights as the text component; snap the
// continuous Compose weight to the nearest Kuikly step below it.
private fun FontWeight?.toKuikly(): KuiklyFontWeight {
    val weight = this?.weight ?: FontWeight.Normal.weight
    return when {
        weight >= FontWeight.W900.weight -> KuiklyFontWeight.BLACK
        weight >= FontWeight.W800.weight -> KuiklyFontWeight.EXTRABOLD
        weight >= FontWeight.W700.weight -> KuiklyFontWeight.BOLD
        weight >= FontWeight.W600.weight -> KuiklyFontWeight.SEMIBOLD
        weight >= FontWeight.W500.weight -> KuiklyFontWeight.MEDIUM
        else -> KuiklyFontWeight.NORMAL
    }
}

private fun FontFamily?.toKuiklyName(): String = when (this) {
    is GenericFontFamily -> name
    is FontListFontFamily -> fonts.toKuiklyFontFamily()
    else -> ""
}

private fun TextAlign.toKuikly(): KuiklyTextAlign = when (this) {
    TextAlign.Center -> KuiklyTextAlign.CENTER
    TextAlign.End, TextAlign.Right -> KuiklyTextAlign.RIGHT
    else -> KuiklyTextAlign.LEFT
}
