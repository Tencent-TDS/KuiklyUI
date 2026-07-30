package com.kuikly.table

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class TableEventTest {

    @Test
    fun defaultHandlersAreNull() {
        val event = TableEvent()
        assertNull(event.onCellClick)
        assertNull(event.onRowClick)
        assertNull(event.onHeaderClick)
        assertNull(event.onScroll)
    }

    @Test
    fun onCellClickHandler() {
        val event = TableEvent()
        var capturedRow = -1
        var capturedCol = -1
        var capturedValue = ""

        event.onCellClick { row, col, value ->
            capturedRow = row
            capturedCol = col
            capturedValue = value
        }

        assertNotNull(event.onCellClick)
        event.onCellClick!!.invoke(2, 3, "test")
        assertEquals(2, capturedRow)
        assertEquals(3, capturedCol)
        assertEquals("test", capturedValue)
    }

    @Test
    fun onRowClickHandler() {
        val event = TableEvent()
        var capturedRow = -1

        event.onRowClick { row -> capturedRow = row }

        assertNotNull(event.onRowClick)
        event.onRowClick!!.invoke(5)
        assertEquals(5, capturedRow)
    }

    @Test
    fun onHeaderClickHandler() {
        val event = TableEvent()
        var capturedCol = -1
        var capturedKey = ""

        event.onHeaderClick { col, key ->
            capturedCol = col
            capturedKey = key
        }

        assertNotNull(event.onHeaderClick)
        event.onHeaderClick!!.invoke(1, "name")
        assertEquals(1, capturedCol)
        assertEquals("name", capturedKey)
    }

    @Test
    fun onScrollHandler() {
        val event = TableEvent()
        var capturedX = 0f
        var capturedY = 0f

        event.onScroll { x, y ->
            capturedX = x
            capturedY = y
        }

        assertNotNull(event.onScroll)
        event.onScroll!!.invoke(10.5f, 20.3f)
        assertEquals(10.5f, capturedX)
        assertEquals(20.3f, capturedY)
    }
}
