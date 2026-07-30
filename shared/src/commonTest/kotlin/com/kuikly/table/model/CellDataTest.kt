package com.kuikly.table.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CellDataTest {

    @Test
    fun defaultValues() {
        val cell = CellData(rowIndex = 0, colIndex = 0, value = "test")
        assertEquals(0, cell.rowIndex)
        assertEquals(0, cell.colIndex)
        assertEquals("test", cell.value)
        assertEquals("", cell.columnKey)
        assertNull(cell.extra)
    }

    @Test
    fun withColumnKey() {
        val cell = CellData(rowIndex = 1, colIndex = 2, value = "hello", columnKey = "name")
        assertEquals("name", cell.columnKey)
    }

    @Test
    fun withExtra() {
        val extras = mapOf<String, Any>("highlight" to true, "color" to "red")
        val cell = CellData(rowIndex = 0, colIndex = 0, value = "v", extra = extras)
        assertEquals(true, cell.extra!!["highlight"])
        assertEquals("red", cell.extra!!["color"])
    }

    @Test
    fun dataClassEquality() {
        val cell1 = CellData(0, 1, "a", "key")
        val cell2 = CellData(0, 1, "a", "key")
        assertEquals(cell1, cell2)
    }
}
