package com.kuikly.table.model
import com.tencent.kuikly.core.base.*


/**
 * 单元格自定义渲染器类型
 * 作为 ViewContainer 的扩展函数，在单元格视图上下文中可直接使用 View{} / Text{} 等 DSL 方法
 */
typealias CellRendererScope = ViewContainer<*, *>.(cellData: CellData, rowIndex: Int, colIndex: Int) -> Unit

/**
 * 列定义模型
 */
data class ColumnDef(
    val key: String,
    val title: String,
    val width: Float = 100f,
    val minWidth: Float = 50f,
    val align: TextAlign = TextAlign.LEFT,
    val headerAlign: TextAlign = TextAlign.CENTER,
    val fixed: Boolean = false,
    val customRenderer: CellRendererScope? = null
)
