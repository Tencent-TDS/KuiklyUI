package com.tencent.kuikly.core.layout

open class FlexLayout {

    val position = FloatArray(4)
    val dimensions = FloatArray(2)
    var direction = FlexLayoutDirection.LTR

    fun resetResult() {
        position.fill(0f)
        dimensions.fill(Float.undefined)
        direction = FlexLayoutDirection.LTR
    }

    fun copy(layout: FlexLayout) {
        val left = PositionType.POSITION_LEFT.ordinal
        position[left] = layout.position[left]

        val top = PositionType.POSITION_TOP.ordinal
        position[top] = layout.position[top]

        val right = PositionType.POSITION_RIGHT.ordinal
        position[right] = layout.position[right]

        val bottom = PositionType.POSITION_BOTTOM.ordinal
        position[bottom] = layout.position[bottom]

        val dw = DimensionType.DIMENSION_WIDTH.ordinal
        dimensions[dw] = layout.dimensions[dw]

        val dh = DimensionType.DIMENSION_HEIGHT.ordinal
        dimensions[dh] = layout.dimensions[dh]
        direction = layout.direction
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlexLayout) return false

        if (!position.contentEquals(other.position)) return false
        if (!dimensions.contentEquals(other.dimensions)) return false
        if (direction != other.direction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.contentHashCode()
        result = 31 * result + dimensions.contentHashCode()
        result = 31 * result + direction.hashCode()
        return result
    }

    enum class PositionType(value: Int) {
        POSITION_LEFT(0),
        POSITION_TOP(1),
        POSITION_RIGHT(2),
        POSITION_BOTTOM(3)
    }

    enum class DimensionType(value: Int) {
        DIMENSION_WIDTH(0),
        DIMENSION_HEIGHT(1)
    }
}

class FlexLayoutCache : FlexLayout() {
    var parentMaxWidth = Float.undefined
}

enum class FlexLayoutDirection {
    INHERIT, LTR, RTL
}