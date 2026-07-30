package com.kuikly.table
import com.tencent.kuikly.core.base.*
import com.kuikly.table.log.TableLog


class TableEvent : ComposeEvent() {

    var onCellClick: ((rowIndex: Int, colIndex: Int, value: String) -> Unit)? = null
        private set
    var onRowClick: ((rowIndex: Int) -> Unit)? = null
        private set
    var onHeaderClick: ((colIndex: Int, columnKey: String) -> Unit)? = null
        private set
    var onScroll: ((offsetX: Float, offsetY: Float) -> Unit)? = null
        private set

    fun onCellClick(handler: (rowIndex: Int, colIndex: Int, value: String) -> Unit) {
        this.onCellClick = { row, col, value ->
            TableLog.interaction("TableEvent", "单元格点击 | row=$row col=$col value=$value")
            handler(row, col, value)
        }
    }

    fun onRowClick(handler: (rowIndex: Int) -> Unit) {
        this.onRowClick = { row ->
            TableLog.interaction("TableEvent", "行点击 | row=$row")
            handler(row)
        }
    }

    fun onHeaderClick(handler: (colIndex: Int, columnKey: String) -> Unit) {
        this.onHeaderClick = { col, key ->
            TableLog.interaction("TableEvent", "表头点击 | col=$col key=$key")
            handler(col, key)
        }
    }

    fun onScroll(handler: (offsetX: Float, offsetY: Float) -> Unit) {
        this.onScroll = handler  // 滚动事件频繁，不记录每次日志
    }
}
