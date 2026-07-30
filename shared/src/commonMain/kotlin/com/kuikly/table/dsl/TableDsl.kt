package com.kuikly.table.dsl

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.kuikly.table.KuiklyTableView
import com.kuikly.table.TableAttr
import com.kuikly.table.TableEvent
import com.kuikly.table.model.CellRendererScope
import com.kuikly.table.model.ColumnDef
import com.kuikly.table.model.TableData
import com.kuikly.table.model.TableTheme
import com.kuikly.table.model.TextAlign

/**
 * DSL 入口方法 -- 在 ViewContainer 中使用 Table {} 快速创建表格
 *
 * 示例:
 * ```
 * Table {
 *     column("name", "姓名", 120f)
 *     column("age", "年龄", 80f)
 *     row("张三", "28")
 *     row("李四", "32")
 * }
 * ```
 */
fun ViewContainer<*, *>.Table(init: TableBuilder.() -> Unit) {
    val builder = TableBuilder().apply(init)
    val theme = builder.getTheme()

    // ComposeView 必须有明确高度约束，否则 Kuikly 将其高度算为 0，
    // 表格渲染了却不可见。
    //
    // 双保险策略：
    // ① flex(1f) —— 首选：让 ComposeView 撑满父容器（demoPageShell 内容区
    //    已设 flex(1f) 提供确定高度），自适应剩余空间。
    // ② height(contentHeight) —— 兜底：以表格内容的自然高度（表头+行高）
    //    作为 flex-basis。即使 flex(1f) 因框架 ComposeView 测量机制失效，
    //    ComposeView 也有最小可见高度，不会坍缩为 0。
    //    当有 maxHeight 时，basis 用 maxHeight（配合内部滚动）；
    //    无 maxHeight 时，basis = headerHeight × isHeaderVisible + rowHeight × 行数。
    val contentHeight = if (builder.getMaxHeight() != Float.MAX_VALUE) {
        builder.getMaxHeight()
    } else {
        val headerH = if (builder.isHeaderVisible()) theme.headerHeight else 0f
        val bodyH = builder.getRows().size * theme.rowHeight
        headerH + bodyH
    }

    addChild(KuiklyTableView()) {
        attr {
            flex(1f)
            height(contentHeight)
            columns(builder.getColumns())
            rows(builder.getRows())
            theme(theme)
            headerVisible(builder.isHeaderVisible())
            fixedColumns(builder.getFixedColumns())
            maxHeightValue = builder.getMaxHeight()
            scrollEnabled(builder.isScrollEnabled())
        }
        event {
            builder.getCellClickHandler()?.let { onCellClick(it) }
            builder.getRowClickHandler()?.let { onRowClick(it) }
            builder.getHeaderClickHandler()?.let { onHeaderClick(it) }
            builder.getScrollHandler()?.let { onScroll(it) }
        }
    }
}

/**
 * 表格构建器
 */
class TableBuilder {
    private val columnDefs = mutableListOf<ColumnDef>()
    private val rowDataList = mutableListOf<List<String>>()
    private var theme: TableTheme = TableTheme.DEFAULT
    private var headerVisible: Boolean = true
    private var fixedColumns: Int = 0
    private var maxHeight: Float = Float.MAX_VALUE
    private var scrollEnabled: Boolean = true

    private var onCellClick: ((Int, Int, String) -> Unit)? = null
    private var onRowClick: ((Int) -> Unit)? = null
    private var onHeaderClick: ((Int, String) -> Unit)? = null
    private var onScroll: ((Float, Float) -> Unit)? = null

    // --- Column definition ---

    fun column(
        key: String,
        title: String,
        width: Float = 100f,
        block: ColumnBuilder.() -> Unit = {}
    ) {
        val colBuilder = ColumnBuilder(key, title, width).apply(block)
        columnDefs.add(colBuilder.build())
    }

    fun columns(vararg cols: ColumnDef) {
        columnDefs.addAll(cols)
    }

    // --- Data ---

    fun row(vararg values: String) {
        rowDataList.add(values.toList())
    }

    fun rows(data: List<List<String>>) {
        rowDataList.clear()
        rowDataList.addAll(data)
    }

    fun data(tableData: TableData) {
        columnDefs.clear()
        columnDefs.addAll(tableData.columns)
        rowDataList.clear()
        rowDataList.addAll(tableData.rows)
        headerVisible = tableData.headerVisible
        fixedColumns = tableData.fixedColumns
    }

    // --- Configuration ---

    fun theme(theme: TableTheme) {
        this.theme = theme
    }

    fun theme(block: ThemeBuilder.() -> Unit) {
        this.theme = ThemeBuilder().apply(block).build()
    }

    fun headerVisible(visible: Boolean) {
        this.headerVisible = visible
    }

    fun fixedColumns(count: Int) {
        this.fixedColumns = count
    }

    fun maxHeight(height: Float) {
        this.maxHeight = height
    }

    fun scrollEnabled(enabled: Boolean) {
        this.scrollEnabled = enabled
    }

    // --- Events ---

    fun onCellClick(handler: (rowIndex: Int, colIndex: Int, value: String) -> Unit) {
        this.onCellClick = handler
    }

    fun onRowClick(handler: (rowIndex: Int) -> Unit) {
        this.onRowClick = handler
    }

    fun onHeaderClick(handler: (colIndex: Int, columnKey: String) -> Unit) {
        this.onHeaderClick = handler
    }

    fun onScroll(handler: (offsetX: Float, offsetY: Float) -> Unit) {
        this.onScroll = handler
    }

    // --- Internal accessors ---

    internal fun getColumns(): List<ColumnDef> = columnDefs.toList()
    internal fun getRows(): List<List<String>> = rowDataList.toList()
    internal fun getTheme(): TableTheme = theme
    internal fun isHeaderVisible(): Boolean = headerVisible
    internal fun getFixedColumns(): Int = fixedColumns
    internal fun getMaxHeight(): Float = maxHeight
    internal fun isScrollEnabled(): Boolean = scrollEnabled
    internal fun getCellClickHandler() = onCellClick
    internal fun getRowClickHandler() = onRowClick
    internal fun getHeaderClickHandler() = onHeaderClick
    internal fun getScrollHandler() = onScroll
}

/**
 * 列构建器
 */
class ColumnBuilder(
    private val key: String,
    private val title: String,
    private var width: Float = 100f
) {
    private var minWidth: Float = 50f
    private var align: TextAlign = TextAlign.LEFT
    private var headerAlign: TextAlign = TextAlign.CENTER
    private var fixed: Boolean = false
    private var customRenderer: CellRendererScope? = null

    fun width(w: Float) { width = w }
    fun minWidth(w: Float) { minWidth = w }
    fun align(a: TextAlign) { align = a }
    fun headerAlign(a: TextAlign) { headerAlign = a }
    fun fixed(f: Boolean) { fixed = f }

    fun customRenderer(renderer: CellRendererScope) {
        customRenderer = renderer
    }

    internal fun build(): ColumnDef {
        return ColumnDef(
            key = key,
            title = title,
            width = width,
            minWidth = minWidth,
            align = align,
            headerAlign = headerAlign,
            fixed = fixed,
            customRenderer = customRenderer
        )
    }
}

/**
 * 主题构建器
 */
class ThemeBuilder {
    var headerBackgroundColor: Long = 0xFFF5F5F5
    var headerTextColor: Long = 0xFF333333
    var headerFontSize: Float = 14f
    var headerFontBold: Boolean = true
    var headerHeight: Float = 44f
    var rowHeight: Float = 40f
    var rowBackgroundColor: Long = 0xFFFFFFFF
    var rowAlternateColor: Long = 0xFFFAFAFA
    var cellTextColor: Long = 0xFF666666
    var cellFontSize: Float = 13f
    var cellPaddingHorizontal: Float = 8f
    var cellPaddingVertical: Float = 4f
    var borderColor: Long = 0xFFE0E0E0
    var borderWidth: Float = 0.5f
    var showRowBorder: Boolean = true
    var showColumnBorder: Boolean = true
    var showOuterBorder: Boolean = true
    var stripedRows: Boolean = true

    internal fun build(): TableTheme {
        return TableTheme(
            headerBackgroundColor = headerBackgroundColor,
            headerTextColor = headerTextColor,
            headerFontSize = headerFontSize,
            headerFontBold = headerFontBold,
            headerHeight = headerHeight,
            rowHeight = rowHeight,
            rowBackgroundColor = rowBackgroundColor,
            rowAlternateColor = rowAlternateColor,
            cellTextColor = cellTextColor,
            cellFontSize = cellFontSize,
            cellPaddingHorizontal = cellPaddingHorizontal,
            cellPaddingVertical = cellPaddingVertical,
            borderColor = borderColor,
            borderWidth = borderWidth,
            showRowBorder = showRowBorder,
            showColumnBorder = showColumnBorder,
            showOuterBorder = showOuterBorder,
            stripedRows = stripedRows
        )
    }
}
