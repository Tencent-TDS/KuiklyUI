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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartLayoutEngineTest {
    @Test
    fun dataBuilderNormalizesMissingCategoryLabels() {
        val data = ChartDataBuilder().apply {
            categories("Mon")
            line("Visits") { values(10f, 20f, 30f) }
        }.build()

        assertEquals(listOf("Mon", "2", "3"), data.categories)
        assertEquals(3, data.series.single().values.size)
    }

    @Test
    fun barSeriesAutomaticallyIncludesZeroInScale() {
        val data = ChartData(
            categories = listOf("A", "B"),
            series = listOf(
                ChartSeries("Sales", ChartSeriesType.BAR, listOf(12f, 18f), Color.BLUE),
            ),
        )

        val layout = layout(data)

        assertEquals(0f, layout.scale.minimum)
        assertTrue(layout.scale.maximum >= 18f)
    }

    @Test
    fun negativeAndPositiveValuesMapInsidePlot() {
        val data = ChartData(
            categories = listOf("A", "B", "C"),
            series = listOf(
                ChartSeries("Delta", ChartSeriesType.LINE, listOf(-8f, 0f, 12f), Color.RED),
            ),
        )

        val layout = layout(data)

        assertTrue(layout.yForValue(12f) < layout.yForValue(0f))
        assertTrue(layout.yForValue(0f) < layout.yForValue(-8f))
        assertTrue(layout.yForValue(12f) >= layout.plot.top)
        assertTrue(layout.yForValue(-8f) <= layout.plot.bottom)
    }

    @Test
    fun fixedRangeIsRespected() {
        val data = ChartData(
            categories = listOf("A"),
            series = listOf(
                ChartSeries("Temperature", ChartSeriesType.LINE, listOf(22f), Color.GREEN),
            ),
        )

        val layout = layout(data, minimum = -10f, maximum = 50f)

        assertEquals(-10f, layout.scale.minimum)
        assertEquals(50f, layout.scale.maximum)
    }

    @Test
    fun groupedBarsNeverOverlap() {
        val data = ChartData(
            categories = listOf("A", "B"),
            series = listOf(
                ChartSeries("2025", ChartSeriesType.BAR, listOf(10f, 20f), Color.BLUE),
                ChartSeries("2026", ChartSeriesType.BAR, listOf(12f, 24f), Color.GREEN),
            ),
        )
        val layout = layout(data)

        val first = layout.barRect(categoryIndex = 0, barSeriesIndex = 0, value = 10f)
        val second = layout.barRect(categoryIndex = 0, barSeriesIndex = 1, value = 12f)

        assertTrue(first.right <= second.left)
        assertTrue(first.width > 0f)
        assertTrue(second.right < layout.xForCategory(1))
    }

    @Test
    fun categoryHitTestingHandlesEdgesAndOutsideCoordinates() {
        val data = ChartData(
            categories = listOf("A", "B", "C"),
            series = listOf(
                ChartSeries("Value", ChartSeriesType.LINE, listOf(1f, 2f, 3f), Color.BLUE),
            ),
        )
        val layout = layout(data)

        assertEquals(0, layout.categoryIndexAt(layout.plot.left))
        assertEquals(1, layout.categoryIndexAt(layout.xForCategory(1)))
        assertEquals(2, layout.categoryIndexAt(layout.plot.right))
        assertEquals(-1, layout.categoryIndexAt(layout.plot.left - 1f))
        assertEquals(-1, layout.categoryIndexAt(layout.plot.right + 1f))
    }

    @Test
    fun emptyAndConstantDataProduceUsableScales() {
        val empty = layout(ChartData.EMPTY)
        assertTrue(empty.scale.maximum > empty.scale.minimum)

        val constant = layout(
            ChartData(
                categories = listOf("A", "B"),
                series = listOf(
                    ChartSeries("Flat", ChartSeriesType.LINE, listOf(5f, 5f), Color.BLUE),
                ),
            ),
        )
        assertTrue(constant.scale.minimum < 5f)
        assertTrue(constant.scale.maximum > 5f)
    }

    @Test
    fun tinyViewportKeepsPlotInsideCanvas() {
        val data = ChartData(
            categories = listOf("A"),
            series = listOf(
                ChartSeries("Value", ChartSeriesType.LINE, listOf(1f), Color.BLUE),
            ),
        )

        val layout = ChartLayoutEngine.layout(
            data = data,
            width = 20f,
            height = 10f,
            insets = ChartInsets(),
            requestedTickCount = 5,
            includeZero = false,
            minimumOverride = Float.NaN,
            maximumOverride = Float.NaN,
        )

        assertTrue(layout.plot.left in 0f..20f)
        assertTrue(layout.plot.right in layout.plot.left..20f)
        assertTrue(layout.plot.top in 0f..10f)
        assertTrue(layout.plot.bottom in layout.plot.top..10f)
    }

    @Test
    fun valueFormatterUsesReadableSuffixes() {
        assertEquals("12", formatChartValue(12f))
        assertEquals("1.25K", formatChartValue(1_250f))
        assertEquals("-2.5M", formatChartValue(-2_500_000f))
    }

    @Test
    fun legendRowsExpandWhenItemsWrap() {
        assertEquals(0, chartLegendLineCount(emptyList(), 200f))
        assertEquals(1, chartLegendLineCount(listOf(60f, 70f), 200f))
        assertEquals(2, chartLegendLineCount(listOf(90f, 90f, 90f), 200f))
        assertEquals(3, chartLegendLineCount(listOf(90f, 90f, 90f), 100f))
    }

    private fun layout(
        data: ChartData,
        minimum: Float = Float.NaN,
        maximum: Float = Float.NaN,
    ): ChartLayout {
        return ChartLayoutEngine.layout(
            data = data,
            width = 360f,
            height = 240f,
            insets = ChartInsets(),
            requestedTickCount = 5,
            includeZero = false,
            minimumOverride = minimum,
            maximumOverride = maximum,
        )
    }
}
