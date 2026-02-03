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
        p.alpha = DefaultAlpha
        p.color = if (alpha != DefaultAlpha) {
            value.copy(alpha = value.alpha * alpha)
        } else {
            value
        }
        if (p.shader != null) p.shader = null
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

    private val isFinite get() = start.isFinite && end.isFinite

    override val intrinsicSize: Size
        get() =
            Size(
                if (start.x.isFinite() && end.x.isFinite() && start.x != end.x) abs(start.x - end.x) else Float.NaN,
                if (start.y.isFinite() && end.y.isFinite() && start.x != end.x) abs(start.y - end.y) else Float.NaN
            )

    val colorStops: ArrayList<ColorStop> by lazy {
        val tStops = stops ?: computeEvenlyDistributedStops(colors.size)
        val res = arrayListOf<ColorStop>()
        colors.forEachIndexed { index, color ->
            val stop = tStops.getOrNull(index) ?: 1f
            res.add(ColorStop(color.toKuiklyColor(), stop))
        }
        res
    }

    override fun applyTo(size: Size, p: Paint, alpha: Float) {
        p.alpha = DefaultAlpha
        p.shader = if (isFinite && alpha == DefaultAlpha) {
            this
        } else {
            LinearGradient(
                colors = if (alpha == DefaultAlpha) colors else colors.map { it.modulate(alpha) },
                stops = stops,
                start = if (start.isFinite) start else Offset(
                    if (start.x.isFinite()) start.x else size.width,
                    if (start.y.isFinite()) start.y else size.height
                ),
                end = if (end.isFinite) end else Offset(
                    if (end.x.isFinite()) end.x else size.width,
                    if (end.y.isFinite()) end.y else size.height
                ),
                tileMode = tileMode
            )
        }
    }

    override fun applyTo(view: DeclarativeBaseView<*, *>, alpha: Float) {
        val brush = withAlpha(alpha).resolveForView(view)
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

    /**
     * 应用 alpha 值，如果不需要修改则返回 this
     * @param alpha 透明度值
     * @return 如果 alpha >= 1 或 NaN 则返回 this，否则返回应用了 alpha 的新对象
     */
    fun withAlpha(alpha: Float): LinearGradient {
        return if (alpha.isNaN() || alpha >= 1f) this else copy(alpha)
    }

    /**
     * 根据 View 的实际尺寸解析渐变
     * 将像素坐标的渐变转换为归一化坐标的渐变
     * @param view 目标 View，用于获取实际尺寸
     * @return 如果无需转换则返回 this，否则返回转换后的新对象
     */
    fun resolveForView(view: DeclarativeBaseView<*, *>?): LinearGradient {
        // 相对模式（start/end 不是有限值），无需转换
        if (!isFinite) return this
        
        // 优先使用 view 的实际尺寸
        view?.renderView?.currentFrame?.let { frame ->
            return resolveForSize(frame.width, frame.height)
        }
        
        // 使用坐标中的最大值作为参考尺寸
        return resolveForText()
    }

    /**
     * 根据指定尺寸解析渐变
     * 将像素坐标的渐变转换为归一化坐标的渐变
     * @param width 参考宽度
     * @param height 参考高度
     * @return 如果无需转换则返回 this，否则返回转换后的新对象
     */
    fun resolveForSize(width: Float, height: Float): LinearGradient {
        // 相对模式，无需转换
        if (!isFinite) return this
        
        val (startPos, endPos) = computePixelPositions(width, height)
        
        // 如果映射后与原始一致（0~1 范围），复用原对象
        if (startPos == 0f && endPos == 1f) return this
        
        return createMappedGradient(startPos, endPos)
    }

    /**
     * 使用坐标中的最大值作为参考尺寸来解析渐变
     * 主要用于 Text 渐变动画等没有 View 尺寸的场景
     * @return 如果无需转换则返回 this，否则返回转换后的新对象
     */
    fun resolveForText(): LinearGradient {
        val maxCoord = maxOf(
            abs(start.x),
            abs(start.y),
            abs(end.x),
            abs(end.y)
        )
        return if (maxCoord > 0f) resolveForSize(maxCoord, maxCoord) else this
    }

    /**
     * 创建映射后的 LinearGradient
     * 将原始 stops (0~1) 映射到 startPos~endPos 范围，并处理边界情况
     */
    private fun createMappedGradient(startPos: Float, endPos: Float): LinearGradient {
        val tStops = stops ?: computeEvenlyDistributedStops(colors.size)
        val (mappedColors, mappedStops) = mapColorsAndStopsToRange(tStops, startPos, endPos)
        
        return LinearGradient(
            colors = mappedColors,
            stops = mappedStops,
            start = Offset.Zero,
            end = Offset.Infinite,
            tileMode = tileMode
        )
    }

    /**
     * 将 colors 和 stops 映射到指定范围，处理边界情况
     * 确保输出的 stops 始终覆盖 0~1 范围
     * 
     * @param tStops 原始的 stops 列表 (0~1)
     * @param startPos 渐变起点在 view 中的归一化位置
     * @param endPos 渐变终点在 view 中的归一化位置
     * @return 映射后的 (colors, stops) 对
     */
    private fun mapColorsAndStopsToRange(
        tStops: List<Float>,
        startPos: Float,
        endPos: Float
    ): Pair<List<Color>, List<Float>> {
        val firstColor = colors.firstOrNull() ?: return Pair(emptyList(), emptyList())
        val lastColor = colors.lastOrNull() ?: firstColor
        
        // 处理反向渐变的情况
        val actualStartPos = minOf(startPos, endPos)
        val actualEndPos = maxOf(startPos, endPos)
        val isReversed = startPos > endPos
        
        // 情况1：渐变完全在 view 左侧 (endPos <= 0)
        if (actualEndPos <= 0f) {
            val solidColor = if (isReversed) firstColor else lastColor
            return Pair(listOf(solidColor, solidColor), listOf(0f, 1f))
        }
        
        // 情况2：渐变完全在 view 右侧 (startPos >= 1)
        if (actualStartPos >= 1f) {
            val solidColor = if (isReversed) lastColor else firstColor
            return Pair(listOf(solidColor, solidColor), listOf(0f, 1f))
        }
        
        // 情况3：渐变与 view 有交集
        val resultColors = mutableListOf<Color>()
        val resultStops = mutableListOf<Float>()
        
        // 添加起始边界颜色
        resultColors.add(firstColor)
        resultStops.add(0f)
        if (startPos > 0f && startPos < 1f) {
            resultColors.add(firstColor)
            resultStops.add(startPos)
        }
        
        // 添加中间颜色（跳过与边界重复的颜色）
        colors.forEachIndexed { index, color ->
            val originalStop = tStops.getOrNull(index) ?: 1f
            // 跳过首尾颜色，避免重复
            if (originalStop > 0f && originalStop < 1f) {
                val mappedStop = startPos + originalStop * (endPos - startPos)
                // 只添加在 view 范围内的颜色
                if (mappedStop > 0f && mappedStop < 1f) {
                    resultColors.add(color)
                    resultStops.add(mappedStop)
                }
            }
        }
        
        // 添加结束边界颜色
        if (endPos > 0f && endPos < 1f) {
            resultColors.add(lastColor)
            resultStops.add(endPos)
        }
        resultColors.add(lastColor)
        resultStops.add(1f)
        
        // 按位置排序
        val sortedPairs = resultColors.zip(resultStops).sortedBy { it.second }
        return Pair(sortedPairs.map { it.first }, sortedPairs.map { it.second })
    }

    val direction: Direction by lazy {
        getDirection(start, end)
    }

    /**
     * 计算 start 和 end 在参考尺寸中的归一化位置
     *
     * @param refWidth 参考宽度
     * @param refHeight 参考高度
     * @return (startPos, endPos) 归一化后的位置，范围通常在 0~1，但可以超出
     */
    private fun computePixelPositions(refWidth: Float, refHeight: Float): Pair<Float, Float> {
        return when {
            // 水平方向渐变
            start.y == end.y -> Pair(start.x / refWidth, end.x / refWidth)
            // 垂直方向渐变
            start.x == end.x -> Pair(start.y / refHeight, end.y / refHeight)
            // 对角线方向渐变
            else -> {
                // 计算参考对角线长度
                val refDiagonal = kotlin.math.sqrt(refWidth * refWidth + refHeight * refHeight)
                // 计算渐变方向上的投影距离
                // 使用渐变向量的方向来计算每个点在该方向上的投影
                val gradientDx = end.x - start.x
                val gradientDy = end.y - start.y
                val gradientLength = kotlin.math.sqrt(gradientDx * gradientDx + gradientDy * gradientDy)
                
                if (gradientLength > 0f) {
                    // 归一化渐变方向向量
                    val dirX = gradientDx / gradientLength
                    val dirY = gradientDy / gradientLength
                    // 计算 start 和 end 在渐变方向上的投影
                    val startProj = (start.x * dirX + start.y * dirY) / refDiagonal
                    val endProj = (end.x * dirX + end.y * dirY) / refDiagonal
                    Pair(startProj, endProj)
                } else {
                    // 如果渐变长度为 0，返回相同位置
                    val pos = start.x / refWidth
                    Pair(pos, pos)
                }
            }
        }
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