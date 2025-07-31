/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.geometry.isFinite
import com.tencent.kuikly.compose.ui.text.style.modulate
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.Direction
import kotlin.math.abs

@Immutable
sealed class Brush {

    /**
     * Return the intrinsic size of the [Brush].
     * If the there is no intrinsic size (i.e. filling bounds with an arbitrary color) return
     * [Size.Unspecified].
     * If there is no intrinsic size in a single dimension, return [Size] with
     * [Float.NaN] in the desired dimension.
     */
    open val intrinsicSize: Size = Size.Unspecified

    abstract fun applyTo(size: Size, p: Paint, alpha: Float)

    abstract fun applyTo(view: DeclarativeBaseView<*, *>, alpha: Float)

    /**
     * Creates a copy of this brush with the specified alpha value.
     * @param alpha The alpha value to apply to the brush, between 0.0 and 1.0
     * @return A new brush instance with the specified alpha value
     */
    abstract fun copy(alpha: Float): Brush

    companion object {

        /**
         * Creates a linear gradient with the provided colors along the given start and end
         * coordinates. The colors are dispersed at the provided offset defined in the
         * colorstop pair.
         *
         * ```
         *  Brush.linearGradient(
         *      0.0f to Color.Red,
         *      0.3f to Color.Green,
         *      1.0f to Color.Blue,
         *      start = Offset(0.0f, 50.0f),
         *      end = Offset(0.0f, 100.0f)
         * )
         * ```
         *
         * @sample androidx.compose.ui.graphics.samples.LinearGradientColorStopSample
         * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
         *
         * @param colorStops Colors and their offset in the gradient area
         * @param start Starting position of the linear gradient. This can be set to
         * [Offset.Zero] to position at the far left and top of the drawing area
         * @param end Ending position of the linear gradient. This can be set to
         * [Offset.Infinite] to position at the far right and bottom of the drawing area
         * @param tileMode Determines the behavior for how the shader is to fill a region outside
         * its bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
         */
        @Stable
        fun linearGradient(
            vararg colorStops: Pair<Float, Color>,
            start: Offset = Offset.Zero,
            end: Offset = Offset.Infinite,
            tileMode: TileMode = TileMode.Clamp
        ): Brush = LinearGradient(
            colors = List<Color>(colorStops.size) { i -> colorStops[i].second },
            stops = List<Float>(colorStops.size) { i -> colorStops[i].first },
            start = start,
            end = end,
            tileMode = tileMode
        )

        /**
         * Creates a linear gradient with the provided colors along the given start and end coordinates.
         * The colors are
         *
         * ```
         *  Brush.linearGradient(
         *      listOf(Color.Red, Color.Green, Color.Blue),
         *      start = Offset(0.0f, 50.0f),
         *      end = Offset(0.0f, 100.0f)
         * )
         * ```
         *
         * @sample androidx.compose.ui.graphics.samples.LinearGradientSample
         * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
         *
         * @param colors Colors to be rendered as part of the gradient
         * @param start Starting position of the linear gradient. This can be set to
         * [Offset.Zero] to position at the far left and top of the drawing area
         * @param end Ending position of the linear gradient. This can be set to
         * [Offset.Infinite] to position at the far right and bottom of the drawing area
         * @param tileMode Determines the behavior for how the shader is to fill a region outside
         * its bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
         */
        @Stable
        fun linearGradient(
            colors: List<Color>,
            start: Offset = Offset.Zero,
            end: Offset = Offset.Infinite,
            tileMode: TileMode = TileMode.Clamp
        ): Brush = LinearGradient(
            colors = colors,
            stops = null,
            start = start,
            end = end,
            tileMode = tileMode
        )

        /**
         * Creates a horizontal gradient with the given colors evenly dispersed within the gradient
         *
         * Ex:
         * ```
         *  Brush.horizontalGradient(
         *      listOf(Color.Red, Color.Green, Color.Blue),
         *      startX = 10.0f,
         *      endX = 20.0f
         * )
         * ```
         *
         * @sample androidx.compose.ui.graphics.samples.HorizontalGradientSample
         * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
         *
         * @param colors colors Colors to be rendered as part of the gradient
         * @param startX Starting x position of the horizontal gradient. Defaults to 0 which
         * represents the left of the drawing area
         * @param endX Ending x position of the horizontal gradient.
         * Defaults to [Float.POSITIVE_INFINITY] which indicates the right of the specified
         * drawing area
         * @param tileMode Determines the behavior for how the shader is to fill a region outside
         * its bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
         */
        @Stable
        fun horizontalGradient(
            colors: List<Color>,
            startX: Float = 0.0f,
            endX: Float = Float.POSITIVE_INFINITY,
            tileMode: TileMode = TileMode.Clamp
        ): Brush = linearGradient(colors, Offset(startX, 0.0f), Offset(endX, 0.0f), tileMode)

        /**
         * Creates a horizontal gradient with the given colors dispersed at the provided offset
         * defined in the colorstop pair.
         *
         * Ex:
         * ```
         *  Brush.horizontalGradient(
         *      0.0f to Color.Red,
         *      0.3f to Color.Green,
         *      1.0f to Color.Blue,
         *      startX = 0.0f,
         *      endX = 100.0f
         * )
         * ```
         *
         * @sample androidx.compose.ui.graphics.samples.HorizontalGradientColorStopSample
         * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
         *
         * @param colorStops Colors and offsets to determine how the colors are dispersed throughout
         * the vertical gradient
         * @param startX Starting x position of the horizontal gradient. Defaults to 0 which
         * represents the left of the drawing area
         * @param endX Ending x position of the horizontal gradient.
         * Defaults to [Float.POSITIVE_INFINITY] which indicates the right of the specified
         * drawing area
         * @param tileMode Determines the behavior for how the shader is to fill a region outside
         * its bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
         */
        @Stable
        fun horizontalGradient(
            vararg colorStops: Pair<Float, Color>,
            startX: Float = 0.0f,
            endX: Float = Float.POSITIVE_INFINITY,
            tileMode: TileMode = TileMode.Clamp
        ): Brush = linearGradient(
            *colorStops,
            start = Offset(startX, 0.0f),
            end = Offset(endX, 0.0f),
            tileMode = tileMode
        )

        /**
         * Creates a vertical gradient with the given colors evenly dispersed within the gradient
         * Ex:
         * ```
         *  Brush.verticalGradient(
         *      listOf(Color.Red, Color.Green, Color.Blue),
         *      startY = 0.0f,
         *      endY = 100.0f
         * )
         * ```
         *
         * @sample androidx.compose.ui.graphics.samples.VerticalGradientSample
         * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
         *
         * @param colors colors Colors to be rendered as part of the gradient
         * @param startY Starting y position of the vertical gradient. Defaults to 0 which
         * represents the top of the drawing area
         * @param endY Ending y position of the vertical gradient.
         * Defaults to [Float.POSITIVE_INFINITY] which indicates the bottom of the specified
         * drawing area
         * @param tileMode Determines the behavior for how the shader is to fill a region outside
         * its bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
         */
        @Stable
        fun verticalGradient(
            colors: List<Color>,
            startY: Float = 0.0f,
            endY: Float = Float.POSITIVE_INFINITY,
            tileMode: TileMode = TileMode.Clamp
        ): Brush = linearGradient(colors, Offset(0.0f, startY), Offset(0.0f, endY), tileMode)

        /**
         * Creates a vertical gradient with the given colors at the provided offset defined
         * in the [Pair<Float, Color>]
         *
         * Ex:
         * ```
         *  Brush.verticalGradient(
         *      0.1f to Color.Red,
         *      0.3f to Color.Green,
         *      0.5f to Color.Blue,
         *      startY = 0.0f,
         *      endY = 100.0f
         * )
         * ```
         *
         * @sample androidx.compose.ui.graphics.samples.VerticalGradientColorStopSample
         * @sample androidx.compose.ui.graphics.samples.GradientBrushSample
         *
         * @param colorStops Colors and offsets to determine how the colors are dispersed throughout
         * the vertical gradient
         * @param startY Starting y position of the vertical gradient. Defaults to 0 which
         * represents the top of the drawing area
         * @param endY Ending y position of the vertical gradient.
         * Defaults to [Float.POSITIVE_INFINITY] which indicates the bottom of the specified
         * drawing area
         * @param tileMode Determines the behavior for how the shader is to fill a region outside
         * its bounds. Defaults to [TileMode.Clamp] to repeat the edge pixels
         */
        @Stable
        fun verticalGradient(
            vararg colorStops: Pair<Float, Color>,
            startY: Float = 0f,
            endY: Float = Float.POSITIVE_INFINITY,
            tileMode: TileMode = TileMode.Clamp
        ): Brush = linearGradient(
            *colorStops,
            start = Offset(0.0f, startY),
            end = Offset(0.0f, endY),
            tileMode = tileMode
        )
    }
}

@Immutable
class SolidColor(val value: Color) : Brush() {
    override fun applyTo(size: Size, p: Paint, alpha: Float) {
        p.alpha = alpha
        p.color = if (alpha != DefaultAlpha) {
            value.copy(alpha = value.alpha * alpha)
        } else {
            value
        }
        // todo: jonas
//        if (p.shader != null) p.shader = null
    }

    override fun applyTo(view: DeclarativeBaseView<*, *>, alpha: Float) {
        view.getViewAttr().backgroundColor(value.modulate(alpha).toKuiklyColor())
    }

    override fun copy(alpha: Float): Brush {
        return SolidColor(value.copy(alpha = value.alpha * alpha))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SolidColor) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "SolidColor(value=$value)"
    }
}

/**
 * Brush implementation used to apply a linear gradient on a given [Paint]
 */
@Immutable
class LinearGradient internal constructor(
    val colors: List<Color>,
    val stops: List<Float>? = null,
    val start: Offset,
    val end: Offset,
    private val tileMode: TileMode = TileMode.Clamp
) : Brush() {

    override val intrinsicSize: Size
        get() =
            Size(
                if (start.x.isFinite() && end.x.isFinite() && start.x != end.x) abs(start.x - end.x) else Float.NaN,
                if (start.y.isFinite() && end.y.isFinite() && start.x != end.x) abs(start.y - end.y) else Float.NaN
            )

    override fun applyTo(size: Size, p: Paint, alpha: Float) {
        p.alpha = alpha
        p.brush = this
        // todo: jonas
//        if (p.shader != null) p.shader = null
    }

    override fun applyTo(view: DeclarativeBaseView<*, *>, alpha: Float) {
        val brush = if (alpha.isNaN() || alpha >= 1f) this else copy(alpha)
        view.getViewAttr().backgroundLinearGradient(
            brush.direction,
            *brush.colorStops.toTypedArray()
        )
    }

    override fun copy(alpha: Float): LinearGradient {
        return LinearGradient(
            colors = colors.map { it.modulate(alpha) },
            stops = stops,
            start = start,
            end = end,
            tileMode = tileMode
        )
    }

    val direction: Direction by lazy {
        getDirection(start, end)
    }

    val colorStops: ArrayList<ColorStop> by lazy {
        val res = arrayListOf<ColorStop>()
        var tStops = stops
        if (stops == null) {
            tStops = computeEvenlyDistributedStops(colors.size)
        }
        colors.forEachIndexed { index, color ->
            val stop = tStops?.getOrNull(index) ?: 1f
            res.add(ColorStop(color.toKuiklyColor(), stop))
        }
        res
    }

    private fun computeEvenlyDistributedStops(colorCount: Int): List<Float> {
        val stopsList = mutableListOf<Float>()
        if (colorCount <= 1) {
            stopsList.add(0f)
            return stopsList
        }
        for (i in 0 until colorCount) {
            stopsList.add(i.toFloat() / (colorCount - 1))
        }
        return stopsList
    }


    private fun getDirection(start: Offset, end: Offset): Direction {
        return when {
            start.y > end.y && start.x == end.x -> Direction.TO_TOP
            start.y < end.y && start.x == end.x -> Direction.TO_BOTTOM
            start.y == end.y && start.x > end.x -> Direction.TO_LEFT
            start.y == end.y && start.x < end.x -> Direction.TO_RIGHT
            start.y > end.y && start.x > end.x -> Direction.TO_TOP_LEFT
            start.y > end.y && start.x < end.x -> Direction.TO_TOP_RIGHT
            start.y < end.y && start.x > end.x -> Direction.TO_BOTTOM_LEFT
            start.y < end.y && start.x < end.x -> Direction.TO_BOTTOM_RIGHT
            else -> throw IllegalArgumentException("Invalid start and end offsets")
        }
    }
//    override fun toString(): String {
//
//        return ""
//    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearGradient) return false

        if (colors != other.colors) return false
        if (stops != other.stops) return false
        if (start != other.start) return false
        if (end != other.end) return false
        if (tileMode != other.tileMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.hashCode()
        result = 31 * result + (stops?.hashCode() ?: 0)
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + tileMode.hashCode()
        return result
    }

    override fun toString(): String {
        val startValue = if (start.isFinite) "start=$start, " else ""
        val endValue = if (end.isFinite) "end=$end, " else ""
        return "LinearGradient(colors=$colors, " +
                "stops=$stops, " +
                startValue +
                endValue +
                "tileMode=$tileMode)"
    }
}