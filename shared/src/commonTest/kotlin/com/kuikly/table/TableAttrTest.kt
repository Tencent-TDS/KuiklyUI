package com.kuikly.table

import com.kuikly.table.model.ColumnDef
import com.kuikly.table.model.TableTheme
import com.kuikly.table.model.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TableAttrTest {

    @Test
    fun defaultValues() {
        val attr = TableAttr()
        assertEquals(emptyList(), attr.columns)
        assertEquals(emptyList(), attr.rows)
        assertTrue(attr.headerVisible)
        assertEquals(0, attr.fixedColumns)
        assertEquals(Float.MAX_VALUE, attr.maxHeightValue)
        assertTrue(attr.scrollEnabled)
    }

    @Test
    fun setColumns() {
        val attr = TableAttr()
        val cols = listOf(
            ColumnDef("name", "姓名", 120f),
            ColumnDef("age", "年龄", 80f)
        )
        attr.columns(cols)
        assertEquals(2, attr.columns.size)
        assertEquals("name", attr.columns[0].key)
        assertEquals("年龄", attr.columns[1].title)
    }

    @Test
    fun setRows() {
        val attr = TableAttr()
        val data = listOf(
            listOf("张三", "28"),
            listOf("李四", "32")
        )
        attr.rows(data)
        assertEquals(2, attr.rows.size)
        assertEquals("张三", attr.rows[0][0])
    }

    @Test
    fun fixedColumnsClampedToColumnSize() {
        val attr = TableAttr()
        val cols = listOf(
            ColumnDef("a", "A", 100f),
            ColumnDef("b", "B", 100f)
        )
        attr.columns(cols)
        attr.fixedColumns(5)
        assertEquals(2, attr.fixedColumns)
    }

    @Test
    fun fixedColumnsClampedToZero() {
        val attr = TableAttr()
        val cols = listOf(ColumnDef("a", "A", 100f))
        attr.columns(cols)
        attr.fixedColumns(-1)
        assertEquals(0, attr.fixedColumns)
    }

    @Test
    fun totalWidth() {
        val attr = TableAttr()
        attr.columns(listOf(
            ColumnDef("a", "A", 100f),
            ColumnDef("b", "B", 150f),
            ColumnDef("c", "C", 80f)
        ))
        assertEquals(330f, attr.totalWidth)
    }

    @Test
    fun fixedAndScrollableWidth() {
        val attr = TableAttr()
        attr.columns(listOf(
            ColumnDef("a", "A", 100f),
            ColumnDef("b", "B", 150f),
            ColumnDef("c", "C", 80f)
        ))
        attr.fixedColumns(1)
        assertEquals(100f, attr.fixedWidth)
        assertEquals(230f, attr.scrollableWidth)
    }

    @Test
    fun headerVisibleToggle() {
        val attr = TableAttr()
        assertTrue(attr.headerVisible)
        attr.headerVisible(false)
        assertFalse(attr.headerVisible)
    }

    @Test
    fun maxHeightSetting() {
        val attr = TableAttr()
        attr.maxHeightValue = 300f
        assertEquals(300f, attr.maxHeightValue)
    }

    @Test
    fun scrollEnabledToggle() {
        val attr = TableAttr()
        attr.scrollEnabled(false)
        assertFalse(attr.scrollEnabled)
    }

    @Test
    fun themeSetting() {
        val attr = TableAttr()
        attr.theme(TableTheme.DARK)
        assertEquals(0xFF2D2D2D, attr.theme.headerBackgroundColor)
    }
}
