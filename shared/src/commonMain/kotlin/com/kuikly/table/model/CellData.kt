package com.kuikly.table.model

/**
 * 单元格数据模型
 */
data class CellData(
    val rowIndex: Int,
    val colIndex: Int,
    val value: String,
    val columnKey: String = "",
    val extra: Map<String, Any>? = null
)
