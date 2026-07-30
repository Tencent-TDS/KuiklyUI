package com.kuikly.table.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TableThemeTest {

    @Test
    fun defaultThemeValues() {
        val theme = TableTheme.DEFAULT
        assertEquals(0xFFF5F5F5, theme.headerBackgroundColor)
        assertEquals(0xFF333333, theme.headerTextColor)
        assertEquals(14f, theme.headerFontSize)
        assertTrue(theme.headerFontBold)
        assertEquals(44f, theme.headerHeight)
        assertEquals(40f, theme.rowHeight)
        assertEquals(0xFFFFFFFF, theme.rowBackgroundColor)
        assertEquals(0xFFFAFAFA, theme.rowAlternateColor)
        assertEquals(0xFF666666, theme.cellTextColor)
        assertEquals(13f, theme.cellFontSize)
        assertEquals(8f, theme.cellPaddingHorizontal)
        assertEquals(4f, theme.cellPaddingVertical)
        assertEquals(0xFFE0E0E0, theme.borderColor)
        assertEquals(0.5f, theme.borderWidth)
        assertTrue(theme.showRowBorder)
        assertTrue(theme.showColumnBorder)
        assertTrue(theme.showOuterBorder)
        assertTrue(theme.stripedRows)
    }

    @Test
    fun darkTheme() {
        val theme = TableTheme.DARK
        assertEquals(0xFF2D2D2D, theme.headerBackgroundColor)
        assertEquals(0xFFEEEEEE, theme.headerTextColor)
        assertEquals(0xFF1E1E1E, theme.rowBackgroundColor)
        assertEquals(0xFF252525, theme.rowAlternateColor)
        assertEquals(0xFFCCCCCC, theme.cellTextColor)
        assertEquals(0xFF444444, theme.borderColor)
    }

    @Test
    fun compactTheme() {
        val theme = TableTheme.COMPACT
        assertEquals(32f, theme.headerHeight)
        assertEquals(28f, theme.rowHeight)
        assertEquals(12f, theme.headerFontSize)
        assertEquals(11f, theme.cellFontSize)
        assertEquals(4f, theme.cellPaddingHorizontal)
        assertEquals(2f, theme.cellPaddingVertical)
    }

    @Test
    fun blueTheme() {
        val theme = TableTheme.BLUE
        assertEquals(0xFF1976D2, theme.headerBackgroundColor)
        assertEquals(0xFFFFFFFF, theme.headerTextColor)
        assertEquals(0xFFBBDEFB, theme.borderColor)
        assertEquals(0xFFE3F2FD, theme.rowAlternateColor)
    }

    @Test
    fun customTheme() {
        val theme = TableTheme(
            headerBackgroundColor = 0xFF112233,
            rowHeight = 50f,
            stripedRows = false
        )
        assertEquals(0xFF112233, theme.headerBackgroundColor)
        assertEquals(50f, theme.rowHeight)
        assertFalse(theme.stripedRows)
    }
}
