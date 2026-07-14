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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.isUndefined
import com.tencent.kuikly.core.layout.undefined

/**
 * 单元格内容水平对齐方式。
 */
enum class TableCellAlignment {
    START,
    CENTER,
    END
}

/**
 * 表格列定义。
 *
 * @property title 列标题文本。
 * @property width 列固定宽度。默认 [Float.undefined]，此时若 [weight] 大于 0 则按权重分配，
 *                 否则使用主题中的 [TableTheme.defaultColumnWidth]。
 * @property weight 列弹性权重。当 [width] 未指定且 [weight] 大于 0 时，按权重分配剩余空间。
 * @property alignment 单元格内容水平对齐方式。
 * @property headerRenderer 自定义表头渲染器，参数依次为当前容器、列定义、列索引。
 * @property cellRenderer 自定义单元格渲染器，参数依次为当前容器、单元格值、行索引、列索引。
 */
data class TableColumn(
    val title: String,
    val width: Float = Float.undefined,
    val weight: Float = 0f,
    val alignment: TableCellAlignment = TableCellAlignment.CENTER,
    val headerRenderer: (ViewContainer<*, *>.(column: TableColumn, columnIndex: Int) -> Unit)? = null,
    val cellRenderer: (ViewContainer<*, *>.(value: Any?, rowIndex: Int, columnIndex: Int) -> Unit)? = null
)

/**
 * 表格主题配置，用于统一控制表头、数据单元格、边框等外观。
 *
 * @property headerBackgroundColor 表头背景色。
 * @property headerTextColor 表头文本颜色。
 * @property headerTextSize 表头字体大小。
 * @property headerFontWeight 表头字重。
 * @property cellBackgroundColor 数据单元格背景色。
 * @property cellTextColor 数据单元格文本颜色。
 * @property cellTextSize 数据单元格字体大小。
 * @property cellFontWeight 数据单元格字重。
 * @property borderColor 边框颜色。
 * @property borderWidth 边框线宽。
 * @property borderStyle 边框样式。
 * @property rowHeight 数据行最小高度。
 * @property headerHeight 表头最小高度。
 * @property cellPadding 单元格内边距。
 * @property defaultColumnWidth 当列未指定 [TableColumn.width] 且 [TableColumn.weight] 为 0 时的默认列宽。
 */
data class TableTheme(
    val headerBackgroundColor: Color = Color(0xFFF5F5F5),
    val headerTextColor: Color = Color.BLACK,
    val headerTextSize: Float = 14f,
    val headerFontWeight: FontWeight = FontWeight.BOLD,
    val cellBackgroundColor: Color = Color.WHITE,
    val cellTextColor: Color = Color.BLACK,
    val cellTextSize: Float = 14f,
    val cellFontWeight: FontWeight = FontWeight.NORMAL,
    val borderColor: Color = Color(0xFFE0E0E0),
    val borderWidth: Float = 1f,
    val borderStyle: BorderStyle = BorderStyle.SOLID,
    val rowHeight: Float = 44f,
    val headerHeight: Float = 44f,
    val cellPadding: Float = 8f,
    val defaultColumnWidth: Float = 100f
)

/**
 * 表格 DSL 构建器。
 */
class TableBuilder internal constructor(private val theme: TableTheme) {

    private val columns = mutableListOf<TableColumn>()
    private val rows = mutableListOf<List<Any?>>()

    /**
     * 声明一列。
     *
     * @param title 列标题。
     * @param width 列固定宽度，默认未定义。
     * @param weight 列弹性权重，默认 0。
     * @param alignment 单元格对齐方式，默认居中。
     * @param headerRenderer 自定义表头渲染器。
     * @param cellRenderer 自定义单元格渲染器。
     */
    fun column(
        title: String,
        width: Float = Float.undefined,
        weight: Float = 0f,
        alignment: TableCellAlignment = TableCellAlignment.CENTER,
        headerRenderer: (ViewContainer<*, *>.(column: TableColumn, columnIndex: Int) -> Unit)? = null,
        cellRenderer: (ViewContainer<*, *>.(value: Any?, rowIndex: Int, columnIndex: Int) -> Unit)? = null
    ) {
        columns.add(
            TableColumn(title, width, weight, alignment, headerRenderer, cellRenderer)
        )
    }

    /**
     * 添加一行数据。
     *
     * @param cells 该行各列的数据，顺序与列定义保持一致。
     */
    fun row(vararg cells: Any?) {
        rows.add(cells.toList())
    }

    /**
     * 批量添加数据行。
     *
     * @param data 表格数据，每个内部列表代表一行。
     */
    fun rows(data: List<List<Any?>>) {
        rows.addAll(data)
    }

    internal fun build(container: ViewContainer<*, *>, width: Float, height: Float) {
        container.renderTable(columns, rows, theme, width, height)
    }
}

/**
 * 创建一个表格组件，支持表头、多行数据、边框、对齐、双向滚动以及自定义单元格渲染。
 *
 * 示例：
 * ```
 * Table(width = 300f, height = 200f) {
 *     column("Name", width = 120f)
 *     column("Age", width = 80f, alignment = TableCellAlignment.CENTER)
 *     column("City", width = 120f)
 *     row("Alice", 25, "Beijing")
 *     row("Bob", 30, "Shanghai")
 * }
 * ```
 *
 * @param width 表格宽度，默认 [Float.undefined]，由父容器决定。
 * @param height 表格高度，默认 [Float.undefined]，由父容器决定。
 * @param theme 表格主题配置。
 * @param init DSL 构建器初始化块。
 */
fun ViewContainer<*, *>.Table(
    width: Float = Float.undefined,
    height: Float = Float.undefined,
    theme: TableTheme = TableTheme(),
    init: TableBuilder.() -> Unit
) {
    val builder = TableBuilder(theme)
    builder.init()
    builder.build(this, width, height)
}

/**
 * 通过列定义和数据快速创建一个表格组件。
 *
 * @param columns 列定义列表。
 * @param data 表格数据，每个内部列表代表一行。
 * @param width 表格宽度，默认 [Float.undefined]，由父容器决定。
 * @param height 表格高度，默认 [Float.undefined]，由父容器决定。
 * @param theme 表格主题配置。
 */
fun ViewContainer<*, *>.Table(
    columns: List<TableColumn>,
    data: List<List<Any?>>,
    width: Float = Float.undefined,
    height: Float = Float.undefined,
    theme: TableTheme = TableTheme()
) {
    Table(width = width, height = height, theme = theme) {
        columns.forEach { col ->
            column(
                title = col.title,
                width = col.width,
                weight = col.weight,
                alignment = col.alignment,
                headerRenderer = col.headerRenderer,
                cellRenderer = col.cellRenderer
            )
        }
        rows(data)
    }
}

private fun ViewContainer<*, *>.renderTable(
    columns: List<TableColumn>,
    rows: List<List<Any?>>,
    theme: TableTheme,
    width: Float,
    height: Float
) {
    val border = Border(theme.borderWidth, theme.borderStyle, theme.borderColor)

    // 外层横向 Scroller 负责水平滚动；内层纵向 Scroller 负责垂直滚动。
    // 这种嵌套方式可保证表格内容在宽度和高度均超出可视区域时，两个方向都能滚动。
    Scroller {
        attr {
            if (!width.isUndefined()) width(width)
            if (!height.isUndefined()) height(height)
            flexDirection(FlexDirection.ROW)
        }
        Scroller {
            attr {
                flexDirection(FlexDirection.COLUMN)
            }
            View {
                attr {
                    flexDirection(FlexDirection.COLUMN)
                    borderTop(border)
                    borderLeft(border)
                }

                // Header
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        minHeight(theme.headerHeight)
                        backgroundColor(theme.headerBackgroundColor)
                    }
                    columns.forEachIndexed { index, column ->
                        renderCell(
                            column = column,
                            content = column.title,
                            rowIndex = -1,
                            columnIndex = index,
                            theme = theme,
                            isHeader = true,
                            border = border
                        )
                    }
                }

                // Data rows
                rows.forEachIndexed { rowIndex, rowData ->
                    View {
                        attr {
                            flexDirection(FlexDirection.ROW)
                            minHeight(theme.rowHeight)
                            backgroundColor(theme.cellBackgroundColor)
                        }
                        columns.forEachIndexed { columnIndex, column ->
                            val value = rowData.getOrNull(columnIndex)
                            renderCell(
                                column = column,
                                content = value,
                                rowIndex = rowIndex,
                                columnIndex = columnIndex,
                                theme = theme,
                                isHeader = false,
                                border = border
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.renderCell(
    column: TableColumn,
    content: Any?,
    rowIndex: Int,
    columnIndex: Int,
    theme: TableTheme,
    isHeader: Boolean,
    border: Border
) {
    View {
        attr {
            when {
                !column.width.isUndefined() -> width(column.width)
                column.weight > 0f -> flex(column.weight)
                else -> width(theme.defaultColumnWidth)
            }
            flexDirection(FlexDirection.ROW)
            justifyContent(column.alignment.toFlexJustify())
            alignItems(FlexAlign.CENTER)
            padding(theme.cellPadding)
            borderRight(border)
            borderBottom(border)
        }

        when {
            isHeader && column.headerRenderer != null -> {
                column.headerRenderer.invoke(this, column, columnIndex)
            }
            !isHeader && column.cellRenderer != null -> {
                column.cellRenderer.invoke(this, content, rowIndex, columnIndex)
            }
            else -> {
                val text = content?.toString() ?: ""
                val textColor = if (isHeader) theme.headerTextColor else theme.cellTextColor
                val textSize = if (isHeader) theme.headerTextSize else theme.cellTextSize
                val fontWeight = if (isHeader) theme.headerFontWeight else theme.cellFontWeight
                Text {
                    attr {
                        text(text)
                        color(textColor)
                        fontSize(textSize)
                        applyFontWeight(fontWeight)
                        lines(1)
                        textOverFlowTail()
                    }
                }
            }
        }
    }
}

private fun TableCellAlignment.toFlexJustify(): FlexJustifyContent = when (this) {
    TableCellAlignment.START -> FlexJustifyContent.FLEX_START
    TableCellAlignment.CENTER -> FlexJustifyContent.CENTER
    TableCellAlignment.END -> FlexJustifyContent.FLEX_END
}

private fun TextAttr.applyFontWeight(weight: FontWeight): TextAttr {
    return when (weight) {
        FontWeight.NORMAL -> fontWeightNormal()
        FontWeight.MEDIUM -> fontWeightMedium()
        FontWeight.SEMIBOLD -> fontWeightSemiBold()
        FontWeight.BOLD -> fontWeightBold()
        FontWeight.EXTRABOLD -> fontWeightExtraBold()
        FontWeight.BLACK -> fontWeightBlack()
        FontWeight.SEMISOLID -> fontWeightSemisolid()
    }
}
