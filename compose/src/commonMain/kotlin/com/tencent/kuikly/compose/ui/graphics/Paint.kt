/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.ui.graphics

import com.tencent.kuikly.compose.ui.text.style.modulate
import kotlin.js.JsName

/**
 * Default alpha value used on [Paint]. This value will draw source content fully opaque.
 */
const val DefaultAlpha: Float = 1.0f

//expect class NativePaint
@JsName("funPaint")
fun Paint(): Paint = KuiklyPaint()

internal fun Paint.toKuiklyColor() = color.modulate(alpha).toKuiklyColor()

class KuiklyPaint : Paint {
    override var alpha: Float = DefaultAlpha
    override var isAntiAlias: Boolean = true
    override var color: Color = Color.Black
    override var strokeWidth: Float = 0f
    override var strokeCap: StrokeCap = StrokeCap.Butt
    override var strokeMiterLimit: Float = 4f
    override var style: PaintingStyle = PaintingStyle.Fill
    override var brush: Brush? = null
}

interface Paint {

    /**
     * Configures the alpha value between 0f to 1f representing fully transparent to fully
     * opaque for the color drawn with this Paint
     */
    var alpha: Float

    /**
     * Whether to apply anti-aliasing to lines and images drawn on the
     * canvas.
     * Defaults to true.
     */
    var isAntiAlias: Boolean

    /**
     * The color to use when stroking or filling a shape.
     * Defaults to opaque black.
     * See also:
     * [style], which controls whether to stroke or fill (or both).
     * [colorFilter], which overrides [color].
     * [shader], which overrides [color] with more elaborate effects.
     * This color is not used when compositing. To colorize a layer, use [colorFilter].
     */
    var color: Color

    /**
     * Whether to paint inside shapes, the edges of shapes, or both.
     * Defaults to [PaintingStyle.Fill].
     */
    var style: PaintingStyle

    /**
     * How wide to make edges drawn when [style] is set to
     * [PaintingStyle.Stroke]. The width is given in logical pixels measured in
     * the direction orthogonal to the direction of the path.
     * Defaults to 0.0, which correspond to a hairline width.
     */
    var strokeWidth: Float

    /**
     * The kind of finish to place on the end of lines drawn when
     * [style] is set to [PaintingStyle.Stroke].
     * Defaults to [StrokeCap.Butt], i.e. no caps.
     */
    var strokeCap: StrokeCap

    /**
     * The limit for miters to be drawn on segments when the join is set to
     * [StrokeJoin.Miter] and the [style] is set to [PaintingStyle.Stroke]. If
     * this limit is exceeded, then a [StrokeJoin.Bevel] join will be drawn
     * instead. This may cause some 'popping' of the corners of a path if the
     * angle between line segments is animated.
     * This limit is expressed as a limit on the length of the miter.
     * Defaults to 4.0.  Using zero as a limit will cause a [StrokeJoin.Bevel]
     * join to be used all the time.
     */
    var strokeMiterLimit: Float

    var brush: Brush?
}
