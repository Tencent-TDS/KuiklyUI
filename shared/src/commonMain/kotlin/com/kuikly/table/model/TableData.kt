package com.kuikly.table.model

/**
 * 表格数据模型
 */
data class TableData(
    val columns: List<ColumnDef>,
    val rows: List<List<String>>,
    val headerVisible: Boolean = true,
    val fixedColumns: Int = 0
) {
    val totalWidth: Float
        get() = columns.sumOf { it.width.toDouble() }.toFloat()

    val fixedWidth: Float
        get() = columns.take(fixedColumns).sumOf { it.width.toDouble() }.toFloat()

    val scrollableWidth: Float
        get() = columns.drop(fixedColumns).sumOf { it.width.toDouble() }.toFloat()

    fun getCellData(rowIndex: Int, colIndex: Int): CellData {
        val value = rows.getOrNull(rowIndex)?.getOrNull(colIndex) ?: ""
        val columnKey = columns.getOrNull(colIndex)?.key ?: ""
        return CellData(
            rowIndex = rowIndex,
            colIndex = colIndex,
            value = value,
            columnKey = columnKey
        )
    }
}
