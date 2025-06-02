package com.tencent.kuikly.core.layout

class FlexStyle {

    var direction = FlexLayoutDirection.INHERIT
    var flexDirection = FlexDirection.COLUMN
    var justifyContent =
        FlexJustifyContent.FLEX_START
    var alignContent = FlexAlign.FLEX_START
    var alignSelf = FlexAlign.AUTO
    var alignItems = FlexAlign.STRETCH
    var positionType =
        FlexPositionType.RELATIVE
    var flexWrap = FlexWrap.NOWRAP
    var flex = 0f

    val margin = StyleSpace()
    val padding = StyleSpace()
    val border = StyleSpace()

    val position = FloatArray(4).apply { fill(Float.undefined) }
    val dimensions = FloatArray(2).apply { fill(Float.undefined) }

    var minWidth = Float.undefined
    var minHeight = Float.undefined

    var maxWidth = Float.undefined
    var maxHeight = Float.undefined
}

enum class FlexDirection {
    COLUMN, COLUMN_REVERSE, ROW, ROW_REVERSE
}

enum class FlexJustifyContent {
    FLEX_START, CENTER, FLEX_END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY
}

enum class FlexAlign {
    AUTO, FLEX_START, CENTER, FLEX_END, STRETCH
}

enum class FlexPositionType {
    RELATIVE, ABSOLUTE
}

enum class FlexWrap {
    NOWRAP, WRAP
}

class StyleSpace {

    private val spacing = floatArrayOf(
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0f,
        0f
    )
    private var valueFlags = 0

    fun getWithFallback(spacingType: Type, fallbackType: Type): Float {
        return if (valueFlags and sFlagsMap[spacingType.ordinal] != 0) {
            spacing[spacingType.ordinal]
        } else {
            spacing[fallbackType.ordinal]
        }
    }

    fun set(spacingType: Type, value: Float) {
        if (spacing[spacingType.ordinal].valueEquals(value)) {
            return
        }

        spacing[spacingType.ordinal] = value

        valueFlags = if (value.isUndefined()) {
            valueFlags and sFlagsMap[spacingType.ordinal].inv()
        } else {
            valueFlags or sFlagsMap[spacingType.ordinal]
        }
    }

    operator fun get(spacingType: Type): Float {
        return spacing[spacingType.ordinal]
    }

    enum class Type(value: Int) {
        LEFT(0),
        TOP(1),
        RIGHT(2),
        BOTTOM(3),

        VERTICAL(4),
        HORIZONTAL(5),

        START(6),
        END(7),
        ALL(8);

        companion object {
            fun fromInt(value: Int): Type {
                return values().firstOrNull { it.ordinal == value } ?: ALL
            }
        }
    }

    companion object {
        private val sFlagsMap = intArrayOf(
            1,  /*LEFT*/
            2,  /*TOP*/
            4,  /*RIGHT*/
            8,  /*BOTTOM*/
            16,  /*VERTICAL*/
            32,  /*HORIZONTAL*/
            64,  /*START*/
            128,  /*END*/
            256
        )
    }
}
