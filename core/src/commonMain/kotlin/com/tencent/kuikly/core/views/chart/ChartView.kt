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

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.TextAlign
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Cross-platform chart component backed by Kuikly's Canvas implementation.
 *
 * The component supports mixed line, bar, and area series, automatic or fixed
 * Y-axis ranges, legends, value labels, and tap/drag selection.
 */
class ChartView : ComposeView<ChartAttr, ChartEvent>() {
    private var selectedIndex by observable(-1)

    override fun createAttr(): ChartAttr = ChartAttr()

    override fun createEvent(): ChartEvent = ChartEvent()

    override fun didInit() {
        if (flexNode.styleWidth.valueEquals(Float.undefined) ||
            flexNode.styleHeight.valueEquals(Float.undefined)
        ) {
            throwRuntimeError("Chart组件需要通过attr { size(width, height) }设置宽度和高度")
        }
        super.didInit()
    }

    override fun body(): ViewBuilder {
        val chart = this
        return {
            Canvas({
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                }
                event {
                    click { chart.selectCategory(it.x) }
                    pan { chart.selectCategory(it.x) }
                }
            }) { context, width, height ->
                ChartRenderer.draw(context, width, height, chart.attr, chart.selectedIndex)
            }
        }
    }

    private fun selectCategory(x: Float) {
        if (!attr.interactive || attr.chartWidth <= 0f || attr.chartHeight <= 0f) return
        val layout = ChartLayoutEngine.layout(
            data = attr.chartData,
            width = attr.chartWidth,
            height = attr.chartHeight,
            insets = attr.effectiveInsets(),
            requestedTickCount = attr.yTickCount,
            includeZero = attr.includeZero,
            minimumOverride = attr.yMinimum,
            maximumOverride = attr.yMaximum
        )
        val newIndex = layout.categoryIndexAt(x)
        if (newIndex == selectedIndex) return
        selectedIndex = newIndex
        if (newIndex >= 0) {
            val data = attr.chartData
            event.selectionChangedHandler?.invoke(
                ChartSelection(
                    index = newIndex,
                    category = data.categories[newIndex],
                    values = data.series.map { series ->
                        ChartSelectionValue(
                            seriesName = series.name,
                            value = series.values.getOrNull(newIndex),
                            color = series.color
                        )
                    }
                )
            )
        } else {
            event.selectionClearedHandler?.invoke()
        }
    }
}

class ChartAttr : ComposeAttr() {
    internal var chartData by observable(ChartData.EMPTY)
    internal var chartWidth by observable(0f)
    internal var chartHeight by observable(0f)
    internal var chartInsets by observable(ChartInsets())
    internal var legendPosition by observable(ChartLegendPosition.TOP)
    internal var interactive by observable(true)
    internal var showTooltip by observable(true)
    internal var showGridLines by observable(true)
    internal var includeZero by observable(false)
    internal var yTickCount by observable(5)
    internal var yMinimum by observable(Float.NaN)
    internal var yMaximum by observable(Float.NaN)
    internal var labelFontSize by observable(11f)
    internal var legendFontSize by observable(11f)
    internal var emptyText by observable("No data")
    internal var chartBackgroundColor by observable(Color.TRANSPARENT)
    internal var axisColor by observable(Color(0xFF8C8C8CL))
    internal var gridColor by observable(Color(0xFFE7E7E7L))
    internal var labelColor by observable(Color(0xFF5E5E5EL))
    internal var tooltipBackgroundColor by observable(Color(0xE6222222L))
    internal var areaOpacity by observable(0.18f)
    internal var yLabelFormatter: (Float) -> String = ::formatChartValue
    internal var valueLabelFormatter: (Float) -> String = ::formatChartValue
    internal var tooltipValueFormatter: (Float) -> String = ::formatChartValue

    override fun width(width: Float): Attr {
        chartWidth = width
        return super.width(width)
    }

    override fun height(height: Float): Attr {
        chartHeight = height
        return super.height(height)
    }

    fun data(data: ChartData) {
        chartData = data
    }

    fun data(init: ChartDataBuilder.() -> Unit) {
        chartData = ChartDataBuilder().apply(init).build()
    }

    fun insets(insets: ChartInsets) {
        chartInsets = insets
    }

    fun legend(position: ChartLegendPosition) {
        legendPosition = position
    }

    fun interactive(enabled: Boolean) {
        interactive = enabled
    }

    fun tooltip(visible: Boolean) {
        showTooltip = visible
    }

    fun gridLines(visible: Boolean) {
        showGridLines = visible
    }

    fun includeZero(include: Boolean) {
        includeZero = include
    }

    fun yTickCount(count: Int) {
        yTickCount = count.coerceIn(2, 10)
    }

    fun yRange(minimum: Float, maximum: Float) {
        yMinimum = minimum
        yMaximum = maximum
    }

    fun autoYRange() {
        yMinimum = Float.NaN
        yMaximum = Float.NaN
    }

    fun labelFontSize(size: Float) {
        labelFontSize = size.coerceAtLeast(8f)
    }

    fun legendFontSize(size: Float) {
        legendFontSize = size.coerceAtLeast(8f)
    }

    fun emptyText(text: String) {
        emptyText = text
    }

    fun colors(
        background: Color = chartBackgroundColor,
        axis: Color = axisColor,
        grid: Color = gridColor,
        label: Color = labelColor,
        tooltipBackground: Color = tooltipBackgroundColor
    ) {
        chartBackgroundColor = background
        axisColor = axis
        gridColor = grid
        labelColor = label
        tooltipBackgroundColor = tooltipBackground
    }

    fun areaOpacity(opacity: Float) {
        areaOpacity = opacity.coerceIn(0f, 1f)
    }

    fun yLabelFormatter(formatter: (Float) -> String) {
        yLabelFormatter = formatter
    }

    fun valueLabelFormatter(formatter: (Float) -> String) {
        valueLabelFormatter = formatter
    }

    fun tooltipValueFormatter(formatter: (Float) -> String) {
        tooltipValueFormatter = formatter
    }

    internal fun effectiveInsets(legendLineCount: Int = 1): ChartInsets {
        val legendHeight = if (legendLineCount <= 0) {
            0f
        } else {
            22f + (legendLineCount - 1) * (legendFontSize + 6f)
        }
        return when (legendPosition) {
            ChartLegendPosition.TOP -> chartInsets.copy(top = chartInsets.top + legendHeight)
            ChartLegendPosition.BOTTOM -> chartInsets.copy(bottom = chartInsets.bottom + legendHeight)
            ChartLegendPosition.NONE -> chartInsets
        }
    }
}

class ChartEvent : ComposeEvent() {
    internal var selectionChangedHandler: ((ChartSelection) -> Unit)? = null
    internal var selectionClearedHandler: (() -> Unit)? = null

    fun selectionChanged(handler: (ChartSelection) -> Unit) {
        selectionChangedHandler = handler
    }

    fun selectionCleared(handler: () -> Unit) {
        selectionClearedHandler = handler
    }
}

/** Add a chart to any declarative Kuikly container. */
fun ViewContainer<*, *>.Chart(init: ChartView.() -> Unit) {
    addChild(ChartView(), init)
}

private object ChartRenderer {
    fun draw(
        context: CanvasContext,
        width: Float,
        height: Float,
        attr: ChartAttr,
        selectedIndex: Int
    ) {
        fillRect(context, 0f, 0f, width, height, attr.chartBackgroundColor)
        val data = attr.chartData
        val legendLineCount = legendLineCount(context, width, data, attr)
        val layout = ChartLayoutEngine.layout(
            data = data,
            width = width,
            height = height,
            insets = attr.effectiveInsets(legendLineCount),
            requestedTickCount = attr.yTickCount,
            includeZero = attr.includeZero,
            minimumOverride = attr.yMinimum,
            maximumOverride = attr.yMaximum
        )
        if (data.categories.isEmpty() || data.series.isEmpty()) {
            drawEmptyState(context, width, height, attr)
            return
        }

        drawAxes(context, data, layout, attr)
        drawAreas(context, data, layout, attr)
        drawBars(context, data, layout, attr)
        drawLines(context, data, layout, attr)
        drawLegend(context, width, height, data, attr, legendLineCount)

        if (selectedIndex in data.categories.indices) {
            drawSelection(context, data, layout, attr, selectedIndex)
        }
    }

    private fun drawAxes(
        context: CanvasContext,
        data: ChartData,
        layout: ChartLayout,
        attr: ChartAttr
    ) {
        context.font(attr.labelFontSize)
        context.fillStyle(attr.labelColor)
        context.textAlign(TextAlign.RIGHT)
        layout.scale.ticks.forEach { tick ->
            val y = layout.yForValue(tick)
            if (attr.showGridLines) {
                drawLine(
                    context,
                    layout.plot.left,
                    y,
                    layout.plot.right,
                    y,
                    attr.gridColor,
                    0.7f,
                    dashed = true
                )
            }
            context.fillText(attr.yLabelFormatter(tick), layout.plot.left - 7f, y + attr.labelFontSize * 0.35f)
        }

        drawLine(
            context,
            layout.plot.left,
            layout.plot.top,
            layout.plot.left,
            layout.plot.bottom,
            attr.axisColor,
            1f
        )
        drawLine(
            context,
            layout.plot.left,
            layout.plot.bottom,
            layout.plot.right,
            layout.plot.bottom,
            attr.axisColor,
            1f
        )

        context.textAlign(TextAlign.CENTER)
        val labelStep = max(1, ceil(data.categories.size / 8f).toInt())
        data.categories.forEachIndexed { index, label ->
            if (index % labelStep == 0 || index == data.categories.lastIndex) {
                context.fillText(
                    label,
                    layout.xForCategory(index),
                    layout.plot.bottom + attr.labelFontSize + 7f
                )
            }
        }
    }

    private fun drawAreas(
        context: CanvasContext,
        data: ChartData,
        layout: ChartLayout,
        attr: ChartAttr
    ) {
        data.series.filter { it.type == ChartSeriesType.AREA }.forEach { series ->
            val segment = mutableListOf<ChartCoordinate>()
            fun flushSegment() {
                if (segment.isEmpty()) return
                val baseline = layout.yForValue(0f.coerceIn(layout.scale.minimum, layout.scale.maximum))
                context.beginPath()
                context.moveTo(segment.first().x, baseline)
                segment.forEach { point -> context.lineTo(point.x, point.y) }
                context.lineTo(segment.last().x, baseline)
                context.closePath()
                context.fillStyle(series.color.opacity(attr.areaOpacity))
                context.fill()
                segment.clear()
            }
            data.categories.indices.forEach { index ->
                val value = series.values.getOrNull(index)
                if (value == null || !value.isFinite()) {
                    flushSegment()
                } else {
                    segment.add(layout.point(index, value))
                }
            }
            flushSegment()
        }
    }

    private fun drawBars(
        context: CanvasContext,
        data: ChartData,
        layout: ChartLayout,
        attr: ChartAttr
    ) {
        var barSeriesIndex = 0
        data.series.forEach { series ->
            if (series.type != ChartSeriesType.BAR) return@forEach
            data.categories.indices.forEach { categoryIndex ->
                val value = series.values.getOrNull(categoryIndex)
                if (value != null && value.isFinite()) {
                    val rect = layout.barRect(categoryIndex, barSeriesIndex, value)
                    fillRect(context, rect.left, rect.top, rect.width, max(rect.height, 1f), series.color)
                    if (series.showValues) {
                        val labelY = if (value >= 0f) {
                            rect.top - 7f
                        } else {
                            rect.bottom + attr.labelFontSize + 3f
                        }
                        drawValueLabel(
                            context = context,
                            attr = attr,
                            series = series,
                            value = value,
                            x = (rect.left + rect.right) / 2f,
                            y = labelY
                        )
                    }
                }
            }
            barSeriesIndex++
        }
    }

    private fun drawLines(
        context: CanvasContext,
        data: ChartData,
        layout: ChartLayout,
        attr: ChartAttr
    ) {
        data.series.filter { it.type != ChartSeriesType.BAR }.forEach { series ->
            var pathOpen = false
            data.categories.indices.forEach { index ->
                val value = series.values.getOrNull(index)
                if (value == null || !value.isFinite()) {
                    if (pathOpen) context.stroke()
                    pathOpen = false
                } else {
                    val point = layout.point(index, value)
                    if (!pathOpen) {
                        context.beginPath()
                        context.strokeStyle(series.color)
                        context.lineWidth(series.lineWidth)
                        context.lineCapRound()
                        context.moveTo(point.x, point.y)
                        pathOpen = true
                    } else {
                        context.lineTo(point.x, point.y)
                    }
                }
            }
            if (pathOpen) context.stroke()

            data.categories.indices.forEach { index ->
                val value = series.values.getOrNull(index)
                if (value != null && value.isFinite()) {
                    val point = layout.point(index, value)
                    if (series.showPoints) fillCircle(context, point.x, point.y, 3f, series.color)
                    if (series.showValues) {
                        drawValueLabel(
                            context = context,
                            attr = attr,
                            series = series,
                            value = value,
                            x = point.x,
                            y = point.y - 7f
                        )
                    }
                }
            }
        }
    }

    private fun drawValueLabel(
        context: CanvasContext,
        attr: ChartAttr,
        series: ChartSeries,
        value: Float,
        x: Float,
        y: Float
    ) {
        context.font(attr.labelFontSize)
        context.textAlign(TextAlign.CENTER)
        context.fillStyle(series.color)
        context.fillText(attr.valueLabelFormatter(value), x, y)
    }

    private fun drawLegend(
        context: CanvasContext,
        width: Float,
        height: Float,
        data: ChartData,
        attr: ChartAttr,
        lineCount: Int
    ) {
        if (attr.legendPosition == ChartLegendPosition.NONE) return
        context.font(attr.legendFontSize)
        context.textAlign(TextAlign.LEFT)
        var x = attr.chartInsets.left
        val lineHeight = attr.legendFontSize + 6f
        var y = if (attr.legendPosition == ChartLegendPosition.TOP) {
            attr.chartInsets.top + attr.legendFontSize
        } else {
            height - attr.chartInsets.bottom + attr.legendFontSize + 6f -
                (lineCount - 1).coerceAtLeast(0) * lineHeight
        }
        data.series.forEach { series ->
            val itemWidth = 14f + context.measureText(series.name).width + 14f
            if (x + itemWidth > width - attr.chartInsets.right && x > attr.chartInsets.left) {
                x = attr.chartInsets.left
                y += lineHeight
            }
            fillRect(context, x, y - 9f, 9f, 9f, series.color)
            context.fillStyle(attr.labelColor)
            context.fillText(series.name, x + 14f, y)
            x += itemWidth
        }
    }

    private fun legendLineCount(
        context: CanvasContext,
        width: Float,
        data: ChartData,
        attr: ChartAttr
    ): Int {
        if (attr.legendPosition == ChartLegendPosition.NONE || data.series.isEmpty()) return 0
        context.font(attr.legendFontSize)
        val itemWidths = data.series.map { series ->
            14f + context.measureText(series.name).width + 14f
        }
        return chartLegendLineCount(
            itemWidths = itemWidths,
            availableWidth = width - attr.chartInsets.left - attr.chartInsets.right
        )
    }

    private fun drawSelection(
        context: CanvasContext,
        data: ChartData,
        layout: ChartLayout,
        attr: ChartAttr,
        selectedIndex: Int
    ) {
        val x = layout.xForCategory(selectedIndex)
        drawLine(
            context,
            x,
            layout.plot.top,
            x,
            layout.plot.bottom,
            attr.axisColor,
            1f,
            dashed = true
        )
        data.series.forEach { series ->
            val value = series.values.getOrNull(selectedIndex)
            if (value != null && value.isFinite()) {
                val point = layout.point(selectedIndex, value)
                fillCircle(context, point.x, point.y, 4f, series.color)
            }
        }
        if (!attr.showTooltip) return

        val lines = mutableListOf(data.categories[selectedIndex])
        data.series.forEach { series ->
            val value = series.values.getOrNull(selectedIndex)
            lines.add(
                if (value != null && value.isFinite()) {
                    "${series.name}: ${attr.tooltipValueFormatter(value)}"
                } else {
                    "${series.name}: --"
                }
            )
        }
        context.font(attr.labelFontSize)
        val tooltipWidth = lines.maxOf { context.measureText(it).width } + 18f
        val lineHeight = attr.labelFontSize + 5f
        val tooltipHeight = lines.size * lineHeight + 10f
        var left = x + 9f
        if (left + tooltipWidth > layout.plot.right) left = x - tooltipWidth - 9f
        left = left.coerceIn(layout.plot.left, max(layout.plot.left, layout.plot.right - tooltipWidth))
        val top = (layout.plot.top + 7f).coerceAtMost(max(layout.plot.top, layout.plot.bottom - tooltipHeight))
        fillRect(context, left, top, tooltipWidth, tooltipHeight, attr.tooltipBackgroundColor)
        context.textAlign(TextAlign.LEFT)
        context.fillStyle(Color.WHITE)
        lines.forEachIndexed { index, line ->
            context.fillText(line, left + 9f, top + 7f + lineHeight * (index + 1) - 3f)
        }
    }

    private fun drawEmptyState(
        context: CanvasContext,
        width: Float,
        height: Float,
        attr: ChartAttr
    ) {
        context.font(max(attr.labelFontSize, 12f))
        context.textAlign(TextAlign.CENTER)
        context.fillStyle(attr.labelColor)
        context.fillText(attr.emptyText, width / 2f, height / 2f)
    }

    private fun drawLine(
        context: CanvasContext,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        color: Color,
        width: Float,
        dashed: Boolean = false
    ) {
        context.beginPath()
        context.strokeStyle(color)
        context.lineWidth(width)
        context.setLineDash(if (dashed) listOf(3f, 3f) else emptyList())
        context.moveTo(startX, startY)
        context.lineTo(endX, endY)
        context.stroke()
        if (dashed) context.setLineDash(emptyList())
    }

    private fun fillRect(
        context: CanvasContext,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        color: Color
    ) {
        if (width <= 0f || height <= 0f) return
        context.beginPath()
        context.moveTo(left, top)
        context.lineTo(left + width, top)
        context.lineTo(left + width, top + height)
        context.lineTo(left, top + height)
        context.closePath()
        context.fillStyle(color)
        context.fill()
    }

    private fun fillCircle(
        context: CanvasContext,
        centerX: Float,
        centerY: Float,
        radius: Float,
        color: Color
    ) {
        context.beginPath()
        context.arc(centerX, centerY, radius, 0f, (kotlin.math.PI * 2).toFloat(), false)
        context.fillStyle(color)
        context.fill()
    }
}
