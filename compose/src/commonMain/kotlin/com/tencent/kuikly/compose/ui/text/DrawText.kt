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

import com.tencent.kuikly.compose.ui.KuiklyCanvas
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.KuiklyPaint
import com.tencent.kuikly.compose.ui.graphics.Paint
import com.tencent.kuikly.compose.ui.graphics.PaintingStyle
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawStyle
import com.tencent.kuikly.compose.ui.graphics.drawscope.Fill
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.graphics.isSpecified
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.core.views.TextAlign as KuiklyTextAlign

/** Draws a single line with the top-left of its measured box at [topLeft], in pixels. */
fun DrawScope.drawText(
    textMeasurer: TextMeasurer,
    text: String,
    topLeft: Offset = Offset.Zero,
    style: TextStyle = TextStyle.Default,
    alpha: Float = 1f,
    drawStyle: DrawStyle = Fill
) {
    if (text.isEmpty()) return
    drawText(textMeasurer.measure(text, style), topLeft = topLeft, alpha = alpha, drawStyle = drawStyle)
}

/** Draws an existing layout without measuring it again. [color] can override its original color. */
fun DrawScope.drawText(
    textLayoutResult: CanvasTextLayoutResult,
    color: Color = Color.Unspecified,
    topLeft: Offset = Offset.Zero,
    alpha: Float = 1f,
    drawStyle: DrawStyle = Fill
) {
    val canvas = drawContext.canvas as? KuiklyCanvas ?: return
    if (textLayoutResult.text.isEmpty()) return
    // The line occupies its entire box, so start/center/end have the same physical left edge.
    canvas.drawText(
        textLayoutResult.text,
        Offset(topLeft.x, topLeft.y + textLayoutResult.firstBaseline),
        textLayoutResult.font,
        KuiklyTextAlign.LEFT,
        textPaint(if (color.isSpecified) color else textLayoutResult.style.color, alpha, drawStyle)
    )
}

/**
 * Draws at a baseline anchor in pixels without measuring. [TextStyle.textAlign] selects the
 * left, center or right anchor on that baseline. Useful for chart ticks and other aligned labels.
 */
@ExperimentalTextApi
fun DrawScope.drawTextAtBaseline(
    text: String,
    origin: Offset,
    style: TextStyle = TextStyle.Default,
    alpha: Float = 1f,
    drawStyle: DrawStyle = Fill
) {
    if (text.isEmpty()) return
    requireSingleLine(text)
    val canvas = drawContext.canvas as? KuiklyCanvas ?: return
    val align = when (style.textAlign) {
        TextAlign.Center -> KuiklyTextAlign.CENTER
        TextAlign.End, TextAlign.Right -> KuiklyTextAlign.RIGHT
        else -> KuiklyTextAlign.LEFT
    }
    canvas.drawText(text, origin, style.canvasFont(density), align, textPaint(style.color, alpha, drawStyle))
}

private fun textPaint(color: Color, alpha: Float, drawStyle: DrawStyle): Paint =
    KuiklyPaint().apply {
        this.color = if (color.isSpecified) color else Color.Black
        this.alpha = alpha
        if (drawStyle is Stroke) {
            style = PaintingStyle.Stroke
            strokeWidth = drawStyle.width
            strokeCap = drawStyle.cap
        }
    }
