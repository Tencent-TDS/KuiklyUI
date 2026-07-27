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

import com.tencent.kuikly.core.base.Color

/** Rendering style for a chart series. */
enum class ChartSeriesType {
    LINE,
    BAR,
    AREA,
}
/** Position of the built-in legend. */
enum class ChartLegendPosition {
    NONE,
    TOP,
    BOTTOM,
}

/** A single named data series. A null value is rendered as a gap. */
data class ChartSeries(
    val name: String,
    val type: ChartSeriesType,
    val values: List<Float?>,
    val color: Color,
    val lineWidth: Float = 2f,
    val showPoints: Boolean = true,
    val showValues: Boolean = false,
)

/** Immutable chart data shared by every Kuikly target. */
data class ChartData(
    val categories: List<String>,
    val series: List<ChartSeries>,
) {
    companion object {
        val EMPTY = ChartData(emptyList(), emptyList())
    }
}

/** Insets reserved around the plot for labels and legends. */
data class ChartInsets(
    val left: Float = 48f,
    val top: Float = 18f,
    val right: Float = 16f,
    val bottom: Float = 38f,
)

/** Value returned when the user taps or drags over a category. */
data class ChartSelection(
    val index: Int,
    val category: String,
    val values: List<ChartSelectionValue>,
)

data class ChartSelectionValue(
    val seriesName: String,
    val value: Float?,
    val color: Color,
)

/**
 * Builder used by [ChartDataBuilder.line], [ChartDataBuilder.bar], and
 * [ChartDataBuilder.area].
 */
class ChartSeriesBuilder internal constructor() {
    private val items = mutableListOf<Float?>()
    var lineWidth: Float = 2f
    var showPoints: Boolean = true
    var showValues: Boolean = false

    fun values(vararg values: Float?) {
        items.addAll(values)
    }

    fun value(value: Float?) {
        items.add(value)
    }

    internal fun build(name: String, type: ChartSeriesType, color: Color): ChartSeries {
        return ChartSeries(
            name = name,
            type = type,
            values = items.toList(),
            color = color,
            lineWidth = lineWidth.coerceAtLeast(0.5f),
            showPoints = showPoints,
            showValues = showValues,
        )
    }
}

/** Declarative data DSL consumed by [ChartAttr.data]. */
class ChartDataBuilder {
    private val categoryItems = mutableListOf<String>()
    private val seriesItems = mutableListOf<ChartSeries>()
    private var paletteIndex = 0

    fun categories(vararg labels: String) {
        categoryItems.clear()
        categoryItems.addAll(labels)
    }

    fun line(
        name: String,
        color: Color = nextColor(),
        init: ChartSeriesBuilder.() -> Unit,
    ) {
        addSeries(name, ChartSeriesType.LINE, color, init)
    }

    fun bar(
        name: String,
        color: Color = nextColor(),
        init: ChartSeriesBuilder.() -> Unit,
    ) {
        addSeries(name, ChartSeriesType.BAR, color, init)
    }

    fun area(
        name: String,
        color: Color = nextColor(),
        init: ChartSeriesBuilder.() -> Unit,
    ) {
        addSeries(name, ChartSeriesType.AREA, color, init)
    }

    private fun addSeries(
        name: String,
        type: ChartSeriesType,
        color: Color,
        init: ChartSeriesBuilder.() -> Unit,
    ) {
        val builder = ChartSeriesBuilder().apply(init)
        seriesItems.add(builder.build(name, type, color))
    }

    private fun nextColor(): Color {
        val color = DEFAULT_PALETTE[paletteIndex % DEFAULT_PALETTE.size]
        paletteIndex++
        return color
    }

    internal fun build(): ChartData {
        val itemCount = maxOf(
            categoryItems.size,
            seriesItems.maxOfOrNull { it.values.size } ?: 0,
        )
        val normalizedCategories = List(itemCount) { index ->
            categoryItems.getOrNull(index) ?: (index + 1).toString()
        }
        return ChartData(normalizedCategories, seriesItems.toList())
    }

    private companion object {
        val DEFAULT_PALETTE = listOf(
            Color(0xFF0052D9L),
            Color(0xFF00A870L),
            Color(0xFFED7B2FL),
            Color(0xFFE34D59L),
            Color(0xFF7B61FFL),
            Color(0xFF00A6A6L),
        )
    }
}
