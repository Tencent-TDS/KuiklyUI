package com.kuikly.table.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableDataTest {

    private val columns = listOf(
        ColumnDef("name", "姓名", 120f),
        ColumnDef("age", "年龄", 80f),
        ColumnDef("city", "城市", 100f)
    )

    private val rows = listOf(
        listOf("张三", "28", "北京"),
        listOf("李四", "32", "上海")
    )

    @Test
    fun defaultValues() {
        val data = TableData(columns, rows)
        assertTrue(data.headerVisible)
        assertEquals(0, data.fixedColumns)
    }

    @Test
    fun totalWidth() {
        val data = TableData(columns, rows)
        assertEquals(300f, data.totalWidth)
    }

    @Test
    fun fixedAndScrollableWidth() {
        val data = TableData(columns, rows, fixedColumns = 1)
        assertEquals(120f, data.fixedWidth)
        assertEquals(180f, data.scrollableWidth)
    }

    @Test
    fun getCellDataValid() {
        val data = TableData(columns, rows)
        val cell = data.getCellData(0, 1)
        assertEquals(0, cell.rowIndex)
        assertEquals(1, cell.colIndex)
        assertEquals("28", cell.value)
        assertEquals("age", cell.columnKey)
    }

    @Test
    fun getCellDataOutOfBounds() {
        val data = TableData(columns, rows)
        val cell = data.getCellData(10, 10)
        assertEquals("", cell.value)
        assertEquals("", cell.columnKey)
    }

    @Test
    fun getCellDataEmptyRow() {
        val data = TableData(columns, emptyList())
        val cell = data.getCellData(0, 0)
        assertEquals("", cell.value)
    }
}
