package com.kuikly.table.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

class ColumnDefTest {

    @Test
    fun defaultValues() {
        val col = ColumnDef("id", "ID")
        assertEquals("id", col.key)
        assertEquals("ID", col.title)
        assertEquals(100f, col.width)
        assertEquals(50f, col.minWidth)
        assertEquals(TextAlign.LEFT, col.align)
        assertEquals(TextAlign.CENTER, col.headerAlign)
        assertFalse(col.fixed)
        assertNull(col.customRenderer)
    }

    @Test
    fun customWidth() {
        val col = ColumnDef("name", "姓名", width = 200f)
        assertEquals(200f, col.width)
    }

    @Test
    fun customAlignment() {
        val col = ColumnDef(
            key = "price",
            title = "价格",
            align = TextAlign.RIGHT,
            headerAlign = TextAlign.RIGHT
        )
        assertEquals(TextAlign.RIGHT, col.align)
        assertEquals(TextAlign.RIGHT, col.headerAlign)
    }

    @Test
    fun withCustomRenderer() {
        val renderer: CellRendererScope = { _, _, _ -> }
        val col = ColumnDef("custom", "自定义", customRenderer = renderer)
        assertNotNull(col.customRenderer)
    }

    @Test
    fun dataClassEquality() {
        val col1 = ColumnDef("a", "A", 100f)
        val col2 = ColumnDef("a", "A", 100f)
        assertEquals(col1, col2)
    }
}
