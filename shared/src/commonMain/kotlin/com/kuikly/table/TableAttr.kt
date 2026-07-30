package com.kuikly.table

import com.tencent.kuikly.core.base.*
import com.kuikly.table.model.ColumnDef
import com.kuikly.table.model.TableTheme

class TableAttr : ComposeAttr() {

    var columns: List<ColumnDef> = emptyList()
        private set
    var rows: List<List<String>> = emptyList()
        private set
    var theme: TableTheme = TableTheme()
        private set
    var headerVisible: Boolean = true
        private set
    var fixedColumns: Int = 0
        private set
    var maxHeightValue: Float = Float.MAX_VALUE
    var scrollEnabled: Boolean = true
        private set

    // 存储原始请求值，等 columns 设置后再重新钳位，避免 fixedColumns() 先于 columns()
    // 调用时被错误钳位为 0
    private var rawFixedColumns: Int = 0

    fun columns(columns: List<ColumnDef>) {
        this.columns = columns
        // columns 设置后重新计算 fixedColumns，确保即使 fixedColumns() 先调用也不丢失
        this.fixedColumns = rawFixedColumns.coerceIn(0, columns.size)
    }

    fun rows(rows: List<List<String>>) {
        this.rows = rows
    }

    fun theme(theme: TableTheme) {
        this.theme = theme
    }

    fun headerVisible(visible: Boolean) {
        this.headerVisible = visible
    }

    fun fixedColumns(count: Int) {
        this.rawFixedColumns = count
        this.fixedColumns = count.coerceIn(0, columns.size)
    }

    fun scrollEnabled(enabled: Boolean) {
        this.scrollEnabled = enabled
    }

    val totalWidth: Float
        get() = columns.sumOf { it.width.toDouble() }.toFloat()

    val fixedWidth: Float
        get() = columns.take(fixedColumns).sumOf { it.width.toDouble() }.toFloat()

    val scrollableWidth: Float
        get() = columns.drop(fixedColumns).sumOf { it.width.toDouble() }.toFloat()
}
