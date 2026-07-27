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

package com.tencent.kuikly.core.views.chart

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

internal data class ChartRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

internal data class ChartCoordinate(val x: Float, val y: Float)

internal data class ChartScale(
    val minimum: Float,
    val maximum: Float,
    val ticks: List<Float>
)

internal data class ChartLayout(
    val plot: ChartRect,
    val categoryCount: Int,
    val scale: ChartScale,
    val barSeriesCount: Int
) {
    val bandWidth: Float
        get() = if (categoryCount == 0) 0f else plot.width / categoryCount

    fun xForCategory(index: Int): Float {
        if (categoryCount == 0) return plot.left
        return plot.left + bandWidth * (index.coerceIn(0, categoryCount - 1) + 0.5f)
    }

    fun yForValue(value: Float): Float {
        val range = scale.maximum - scale.minimum
        if (range <= 0f) return plot.bottom
        val ratio = ((value - scale.minimum) / range).coerceIn(0f, 1f)
        return plot.bottom - ratio * plot.height
    }

    fun point(index: Int, value: Float): ChartCoordinate {
        return ChartCoordinate(xForCategory(index), yForValue(value))
    }

    fun categoryIndexAt(x: Float): Int {
        if (categoryCount == 0 || x < plot.left || x > plot.right) return -1
        return floor((x - plot.left) / bandWidth)
            .toInt()
            .coerceIn(0, categoryCount - 1)
    }

    fun barRect(categoryIndex: Int, barSeriesIndex: Int, value: Float): ChartRect {
        if (barSeriesCount <= 0 || categoryCount <= 0) {
            return ChartRect(plot.left, plot.bottom, plot.left, plot.bottom)
        }
        val groupWidth = bandWidth * 0.72f
        val slotWidth = groupWidth / barSeriesCount
        val gap = min(2f, slotWidth * 0.12f)
        val width = (slotWidth - gap).coerceAtLeast(1f)
        val groupLeft = xForCategory(categoryIndex) - groupWidth / 2f
        val left = groupLeft + slotWidth * barSeriesIndex + gap / 2f
        val zeroY = yForValue(0f.coerceIn(scale.minimum, scale.maximum))
        val valueY = yForValue(value)
        return ChartRect(
            left = left,
            top = min(zeroY, valueY),
            right = left + width,
            bottom = max(zeroY, valueY)
        )
    }
}

internal object ChartLayoutEngine {
    fun layout(
        data: ChartData,
        width: Float,
        height: Float,
        insets: ChartInsets,
        requestedTickCount: Int,
        includeZero: Boolean,
        minimumOverride: Float,
        maximumOverride: Float
    ): ChartLayout {
        val safeWidth = width.coerceAtLeast(0f)
        val safeHeight = height.coerceAtLeast(0f)
        val plotLeft = insets.left.coerceIn(0f, safeWidth)
        val plotTop = insets.top.coerceIn(0f, safeHeight)
        val plot = ChartRect(
            left = plotLeft,
            top = plotTop,
            right = (safeWidth - insets.right).coerceIn(plotLeft, safeWidth),
            bottom = (safeHeight - insets.bottom).coerceIn(plotTop, safeHeight)
        )
        val values = data.series
            .flatMap { it.values }
            .filterNotNull()
            .filter { it.isFinite() }
        val shouldIncludeZero = includeZero || data.series.any {
            it.type == ChartSeriesType.BAR || it.type == ChartSeriesType.AREA
        }
        val scale = createScale(
            values = values,
            requestedTickCount = requestedTickCount,
            includeZero = shouldIncludeZero,
            minimumOverride = minimumOverride,
            maximumOverride = maximumOverride
        )
        return ChartLayout(
            plot = plot,
            categoryCount = data.categories.size,
            scale = scale,
            barSeriesCount = data.series.count { it.type == ChartSeriesType.BAR }
        )
    }

    private fun createScale(
        values: List<Float>,
        requestedTickCount: Int,
        includeZero: Boolean,
        minimumOverride: Float,
        maximumOverride: Float
    ): ChartScale {
        val tickCount = requestedTickCount.coerceIn(2, 10)
        var minimum = 0f
        var maximum = 1f
        if (values.isNotEmpty()) {
            minimum = values[0]
            maximum = values[0]
            values.forEach { value ->
                minimum = min(minimum, value)
                maximum = max(maximum, value)
            }
        }

        if (includeZero) {
            minimum = min(minimum, 0f)
            maximum = max(maximum, 0f)
        }
        if (minimumOverride.isFinite()) minimum = minimumOverride
        if (maximumOverride.isFinite()) maximum = maximumOverride
        if (minimum > maximum) {
            val swap = minimum
            minimum = maximum
            maximum = swap
        }
        if (minimum == maximum) {
            val padding = max(abs(minimum) * 0.1f, 1f)
            minimum -= padding
            maximum += padding
        }

        val rawRange = maximum - minimum
        val step = niceNumber(rawRange / (tickCount - 1), true).coerceAtLeast(0.000001f)
        val niceMinimum = if (minimumOverride.isFinite()) minimum else floor(minimum / step) * step
        val niceMaximum = if (maximumOverride.isFinite()) maximum else ceil(maximum / step) * step
        val ticks = mutableListOf<Float>()
        var value = niceMinimum
        val limit = niceMaximum + step * 0.5f
        while (value <= limit && ticks.size <= 12) {
            ticks.add(normalizeFloat(value))
            value += step
        }
        if (ticks.size < 2) {
            ticks.add(niceMaximum)
        }
        return ChartScale(niceMinimum, niceMaximum, ticks)
    }

    private fun niceNumber(value: Float, roundValue: Boolean): Float {
        if (!value.isFinite() || value <= 0f) return 1f
        val exponent = floor(log10(value.toDouble())).toInt()
        val power = 10.0.pow(exponent).toFloat()
        val fraction = value / power
        val niceFraction = if (roundValue) {
            when {
                fraction < 1.5f -> 1f
                fraction < 3f -> 2f
                fraction < 7f -> 5f
                else -> 10f
            }
        } else {
            when {
                fraction <= 1f -> 1f
                fraction <= 2f -> 2f
                fraction <= 5f -> 5f
                else -> 10f
            }
        }
        return niceFraction * power
    }

    private fun normalizeFloat(value: Float): Float {
        if (abs(value) < 0.000001f) return 0f
        return (value * 1_000_000f).roundToInt() / 1_000_000f
    }
}

internal fun formatChartValue(value: Float): String {
    val absolute = abs(value)
    val suffix: String
    val scaled: Float
    when {
        absolute >= 1_000_000f -> {
            suffix = "M"
            scaled = value / 1_000_000f
        }
        absolute >= 1_000f -> {
            suffix = "K"
            scaled = value / 1_000f
        }
        else -> {
            suffix = ""
            scaled = value
        }
    }
    val rounded = round(scaled)
    val text = if (abs(scaled - rounded) < 0.0001f) {
        rounded.toInt().toString()
    } else {
        val hundredths = (scaled * 100f).roundToInt() / 100f
        hundredths.toString().trimEnd('0').trimEnd('.')
    }
    return text + suffix
}

internal fun chartLegendLineCount(itemWidths: List<Float>, availableWidth: Float): Int {
    if (itemWidths.isEmpty()) return 0
    val safeWidth = availableWidth.coerceAtLeast(0f)
    var lineCount = 1
    var usedWidth = 0f
    itemWidths.forEach { itemWidth ->
        val safeItemWidth = itemWidth.coerceAtLeast(0f)
        if (usedWidth > 0f && usedWidth + safeItemWidth > safeWidth) {
            lineCount++
            usedWidth = 0f
        }
        usedWidth += safeItemWidth
    }
    return lineCount
}
