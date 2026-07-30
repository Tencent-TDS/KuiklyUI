package com.kuikly.table.dsl

import com.kuikly.table.model.ColumnDef
import com.kuikly.table.model.TableData
import com.kuikly.table.model.TableTheme
import com.kuikly.table.model.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class TableDslTest {

    @Test
    fun tableBuilderBasicColumns() {
        val builder = TableBuilder()
        builder.column("name", "姓名", 120f)
        builder.column("age", "年龄", 80f)

        val columns = builder.getColumns()
        assertEquals(2, columns.size)
        assertEquals("name", columns[0].key)
        assertEquals("姓名", columns[0].title)
        assertEquals(120f, columns[0].width)
    }

    @Test
    fun tableBuilderRows() {
        val builder = TableBuilder()
        builder.column("a", "A")
        builder.row("v1", "v2")
        builder.row("v3", "v4")

        val rows = builder.getRows()
        assertEquals(2, rows.size)
        assertEquals(listOf("v1", "v2"), rows[0])
        assertEquals(listOf("v3", "v4"), rows[1])
    }

    @Test
    fun tableBuilderBatchRows() {
        val builder = TableBuilder()
        builder.column("a", "A")
        val data = listOf(listOf("1"), listOf("2"), listOf("3"))
        builder.rows(data)

        assertEquals(3, builder.getRows().size)
    }

    @Test
    fun tableBuilderDataMethod() {
        val columns = listOf(
            ColumnDef("name", "姓名", 120f),
            ColumnDef("age", "年龄", 80f)
        )
        val rows = listOf(listOf("张三", "28"))
        val tableData = TableData(columns, rows, headerVisible = false, fixedColumns = 1)

        val builder = TableBuilder()
        builder.data(tableData)

        assertEquals(2, builder.getColumns().size)
        assertEquals(1, builder.getRows().size)
        assertFalse(builder.isHeaderVisible())
        assertEquals(1, builder.getFixedColumns())
    }

    @Test
    fun tableBuilderThemePreset() {
        val builder = TableBuilder()
        builder.theme(TableTheme.DARK)

        assertEquals(TableTheme.DARK, builder.getTheme())
    }

    @Test
    fun tableBuilderThemeDsl() {
        val builder = TableBuilder()
        builder.theme {
            headerBackgroundColor = 0xFF112233
            rowHeight = 50f
            stripedRows = false
        }

        val theme = builder.getTheme()
        assertEquals(0xFF112233, theme.headerBackgroundColor)
        assertEquals(50f, theme.rowHeight)
        assertFalse(theme.stripedRows)
    }

    @Test
    fun tableBuilderFixedColumns() {
        val builder = TableBuilder()
        builder.fixedColumns(2)
        assertEquals(2, builder.getFixedColumns())
    }

    @Test
    fun tableBuilderMaxHeight() {
        val builder = TableBuilder()
        builder.maxHeight(400f)
        assertEquals(400f, builder.getMaxHeight())
    }

    @Test
    fun tableBuilderScrollEnabled() {
        val builder = TableBuilder()
        builder.scrollEnabled(false)
        assertFalse(builder.isScrollEnabled())
    }

    @Test
    fun tableBuilderHeaderVisible() {
        val builder = TableBuilder()
        assertTrue(builder.isHeaderVisible())
        builder.headerVisible(false)
        assertFalse(builder.isHeaderVisible())
    }

    @Test
    fun tableBuilderEventHandlers() {
        val builder = TableBuilder()
        builder.onCellClick { _, _, _ -> }
        builder.onRowClick { _ -> }
        builder.onHeaderClick { _, _ -> }
        builder.onScroll { _, _ -> }

        assertNotNull(builder.getCellClickHandler())
        assertNotNull(builder.getRowClickHandler())
        assertNotNull(builder.getHeaderClickHandler())
        assertNotNull(builder.getScrollHandler())
    }

    @Test
    fun tableBuilderDefaultEventHandlersNull() {
        val builder = TableBuilder()
        assertNull(builder.getCellClickHandler())
        assertNull(builder.getRowClickHandler())
        assertNull(builder.getHeaderClickHandler())
        assertNull(builder.getScrollHandler())
    }

    @Test
    fun columnBuilderAlign() {
        val builder = TableBuilder()
        builder.column("price", "价格", 100f) {
            align(TextAlign.RIGHT)
            headerAlign(TextAlign.LEFT)
        }

        val col = builder.getColumns()[0]
        assertEquals(TextAlign.RIGHT, col.align)
        assertEquals(TextAlign.LEFT, col.headerAlign)
    }

    @Test
    fun columnBuilderFixed() {
        val builder = TableBuilder()
        builder.column("id", "ID", 60f) {
            fixed(true)
        }

        assertTrue(builder.getColumns()[0].fixed)
    }

    @Test
    fun batchColumnsVararg() {
        val col1 = ColumnDef("a", "A", 100f)
        val col2 = ColumnDef("b", "B", 200f)
        val builder = TableBuilder()
        builder.columns(col1, col2)

        assertEquals(2, builder.getColumns().size)
        assertEquals("a", builder.getColumns()[0].key)
        assertEquals("b", builder.getColumns()[1].key)
    }
}
