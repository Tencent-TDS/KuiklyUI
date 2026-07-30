package com.kuikly.table.model

data class TableTheme(
    // 表头样式
    val headerBackgroundColor: Long = 0xFFF5F5F5,
    val headerTextColor: Long = 0xFF333333,
    val headerFontSize: Float = 14f,
    val headerFontBold: Boolean = true,
    val headerHeight: Float = 44f,

    // 表体样式
    val rowHeight: Float = 40f,
    val rowBackgroundColor: Long = 0xFFFFFFFF,
    val rowAlternateColor: Long = 0xFFFAFAFA,
    val cellTextColor: Long = 0xFF666666,
    val cellFontSize: Float = 13f,
    val cellPaddingHorizontal: Float = 8f,
    val cellPaddingVertical: Float = 4f,

    // 边框样式
    val borderColor: Long = 0xFFE0E0E0,
    val borderWidth: Float = 0.5f,
    val showRowBorder: Boolean = true,
    val showColumnBorder: Boolean = true,
    val showOuterBorder: Boolean = true,

    // 斑马纹
    val stripedRows: Boolean = true
) {
    companion object {
        val DEFAULT = TableTheme()

        val DARK = TableTheme(
            headerBackgroundColor = 0xFF2D2D2D,
            headerTextColor = 0xFFEEEEEE,
            rowBackgroundColor = 0xFF1E1E1E,
            rowAlternateColor = 0xFF252525,
            cellTextColor = 0xFFCCCCCC,
            borderColor = 0xFF444444
        )

        val COMPACT = TableTheme(
            headerHeight = 32f,
            rowHeight = 28f,
            headerFontSize = 12f,
            cellFontSize = 11f,
            cellPaddingHorizontal = 4f,
            cellPaddingVertical = 2f
        )

        val BLUE = TableTheme(
            headerBackgroundColor = 0xFF1976D2,
            headerTextColor = 0xFFFFFFFF,
            borderColor = 0xFFBBDEFB,
            rowAlternateColor = 0xFFE3F2FD
        )
    }
}
