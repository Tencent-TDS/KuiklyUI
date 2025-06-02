package com.tencent.kuikly.core.layout

import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.fastHashSetOf

class FlexNode {

    private var children: MutableList<FlexNode>? = null

    private val flexStyle = FlexStyle()
    private val flexLayout = FlexLayout()
    private val lastLayout = FlexLayoutCache()
    private var disableLayout = false
    var layoutFrame = Frame.zero
    var layoutFrameDidChangedCallback: (() -> Unit)? = null
    var setNeedDirtyCallback: (() -> Unit)? = null
    var isDirty = true
        private set

    val isShow = true
    var lineIndex = 0

    var nextAbsoluteChild: FlexNode? = null
    var nextFlexChild: FlexNode? = null
    var nextMinHeightChild: FlexNode? = null
    var parent: FlexNode? = null

    // ===== layout cache相关 start =====
    var lastLayoutWidth: Float
        get() = lastLayout.dimensions[FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal]
        set(value) {
            lastLayout.dimensions[FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal] = value
        }
    var lastLayoutHeight: Float
        get() = lastLayout.dimensions[FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal]
        set(value) {
            lastLayout.dimensions[FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal] = value
        }
    var lastParentMaxWith: Float
        get() = lastLayout.parentMaxWidth
        set(value) {
            lastLayout.parentMaxWidth = value
        }
    // ===== layout cache相关 end =====

    // ===== flex style相关 start =====
    var flexLayoutDirection: FlexLayoutDirection
        get() = flexLayout.direction
        set(value) {
            flexLayout.direction = value
        }

    var styleDirection: FlexLayoutDirection
        get() {
            return flexStyle.direction
        }
        set(value) {
            if (flexStyle.direction != value) {
                flexStyle.direction = value
                markDirty()
            }
        }

    var flexDirection: FlexDirection
        get() {
            return flexStyle.flexDirection
        }
        set(value) {
            if (flexStyle.flexDirection != value) {
                flexStyle.flexDirection = value
                markDirty()
            }
        }

    var justifyContent: FlexJustifyContent
        get() {
            return flexStyle.justifyContent
        }
        set(value) {
            if (flexStyle.justifyContent != value) {
                flexStyle.justifyContent = value
                markDirty()
            }
        }

    var alignItems: FlexAlign
        get() {
            return flexStyle.alignItems
        }
        set(value) {
            if (flexStyle.alignItems != value) {
                flexStyle.alignItems = value
                markDirty()
            }
        }

    var alignSelf: FlexAlign
        get() {
            return flexStyle.alignSelf
        }
        set(value) {
            if (flexStyle.alignSelf != value) {
                flexStyle.alignSelf = value
                markDirty()
            }
        }

    var positionType: FlexPositionType
        get() {
            return flexStyle.positionType
        }
        set(value) {
            if ( flexStyle.positionType != value) {
                flexStyle.positionType = value
                markDirty()
            }
        }

    var flexWrap: FlexWrap
        get() = flexStyle.flexWrap
        set(value) {
            if (flexStyle.flexWrap != value) {
                flexStyle.flexWrap = value
                markDirty()
            }
        }

    var flex: Float
        get() {
            return flexStyle.flex
        }
        set(value) {
            if (!flexStyle.flex.valueEquals(value)) {
                flexStyle.flex = value
                markDirty()
            }
        }

    var alignContent: FlexAlign
        get() {
            return flexStyle.alignContent
        }
        set(value) {
            if (flexStyle.alignContent != value) {
                flexStyle.alignContent = value
                markDirty()
            }
        }

    var styleMinWidth: Float
        get() = flexStyle.minWidth
        set(value) {
            if (!flexStyle.minWidth.valueEquals(value)) {
                flexStyle.minWidth = value
                markDirty()
            }
        }

    var styleMaxWidth: Float
        get() = flexStyle.maxWidth
        set(value) {
            if (!flexStyle.maxWidth.valueEquals(value)) {
                flexStyle.maxWidth = value
                markDirty()
            }
        }

    var styleMinHeight: Float
        get() = flexStyle.minHeight
        set(value) {
            if (!flexStyle.minHeight.valueEquals(value)) {
                flexStyle.minHeight = value
                markDirty()
            }
        }

    var styleMaxHeight: Float
        get() = flexStyle.maxHeight
        set(value) {
            if (!flexStyle.maxHeight.valueEquals(value)) {
                flexStyle.maxHeight = value
                markDirty()
            }
        }

    var styleWidth: Float
        get() = flexStyle.dimensions[FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal]
        set(value) {
            val oldValue = flexStyle.dimensions[FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal]
            if (!oldValue.valueEquals(value)) {
                flexStyle.dimensions[FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal] = value
                markDirty()
            }

        }

    var styleHeight: Float
        get() = flexStyle.dimensions[FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal]
        set(value) {
            val oldValue = flexStyle.dimensions[FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal]
            if (!oldValue.valueEquals(value)) {
                flexStyle.dimensions[FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal] = value
                markDirty()
            }
        }

    fun setStylePosition(positionType: FlexLayout.PositionType, value: Float) {
        if (!flexStyle.position[positionType.ordinal].valueEquals(value)) {
            flexStyle.position[positionType.ordinal] = value
            markDirty()
        }
    }

    fun setMargin(spacingType: StyleSpace.Type, value: Float) {
        if (!getMargin(spacingType).valueEquals(value)) {
            setStyleSpace(spacingType, flexStyle.margin, value)
            markDirty()
        }
    }

    fun getMargin(spacingType: StyleSpace.Type): Float {
        return flexStyle.margin[spacingType]
    }

    fun setPadding(spacingType: StyleSpace.Type, value: Float) {
        if (!getPadding(spacingType).valueEquals(value)) {
            setStyleSpace(spacingType, flexStyle.padding, value)
            children?.forEach {
                if (it.positionType != FlexPositionType.ABSOLUTE) {
                    it.markDirty()
                }
            }
        }
    }

    fun getPadding(spacingType: StyleSpace.Type): Float {
        return flexStyle.padding[spacingType]
    }

    fun setBorder(spacingType: StyleSpace.Type, value: Float) {
        if (!getBorder(spacingType).valueEquals(value)) {
            setStyleSpace(spacingType, flexStyle.border, value)
            markDirty()
        }
    }

    fun getBorder(spacingType: StyleSpace.Type): Float {
        return flexStyle.border[spacingType]
    }

    fun getStylePaddingWithFallback(spacingType: StyleSpace.Type, padding: StyleSpace.Type): Float {
        return flexStyle.padding.getWithFallback(spacingType, padding)
    }

    // done
    fun getStyleBorderWithFallback(spacingType: StyleSpace.Type, padding: StyleSpace.Type): Float {
        return flexStyle.border.getWithFallback(spacingType, padding)
    }

    fun getStyleMarginWithFallback(spacingType: StyleSpace.Type, padding: StyleSpace.Type): Float {
        return flexStyle.margin.getWithFallback(spacingType, padding)
    }

    // ===== flexStyle相关 end =====

    // ===== layout相关 start =====
    var layoutWidth: Float
        get() {
            return flexLayout.dimensions[FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal]
        }
        set(value) {
            flexLayout.dimensions[FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal] = value
        }
    var layoutHeight: Float
        get() = flexLayout.dimensions[FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal]
        set(value) {
            flexLayout.dimensions[FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal] = value
        }

    val layoutX: Float
        get() = flexLayout.position[FlexLayout.PositionType.POSITION_LEFT.ordinal]
    val layoutY: Float
        get() = flexLayout.position[FlexLayout.PositionType.POSITION_TOP.ordinal]
    // ===== layout相关 end =====

    internal val layoutDimensions: FloatArray
        get() = flexLayout.dimensions

    internal val styleDimensions: FloatArray
        get() = flexStyle.dimensions

    internal val stylePosition: FloatArray
        get() = flexStyle.position

    internal val layoutPosition: FloatArray
        get() = flexLayout.position

    // ===== 自定义测量函数 start =====
    var measureFunction: MeasureFunction? = null
        set(value) {
            if (field != value) {
                field = value
            }
        }

    fun measure(
        measureOutput: MeasureOutput,
        width: Float
    ): MeasureOutput {
        if (measureFunction == null) {
            throw RuntimeException("Measure function isn't defined!")
        }
        measureOutput.height = Float.undefined
        measureOutput.width = Float.undefined
        val height = if (!layoutHeight.isUndefined()) {
            layoutHeight
        } else if (!styleHeight.isUndefined()) {
            styleHeight
        } else {
            Float.undefined
        }
        measureFunction?.measure(this, width, height, measureOutput)
        return measureOutput
    }
    // ===== 自定义测量函数 end =====

    // ===== child操作相关 start =====
    val childCount: Int
        get() {
            return children?.size ?: 0
        }

    fun getChildAt(index: Int): FlexNode? {
        return children?.getOrNull(index)
    }

    fun addChildAt(child: FlexNode, index: Int) {
        if (child.parent != null) {
            throw IllegalStateException("Child already has a parent, it must be removed first.")
        }

        if (children == null) {
            children = fastArrayListOf()
        }
        if (index >= children!!.count()) {
            children?.add(child)
        } else {
            children?.add(index, child)
        }
        child.parent = this
        markDirty()
    }

    fun onlyClearChildren() {
        children?.clear()
    }

    fun onlyAddChild(child: FlexNode) {
        if (children == null) {
            children = fastArrayListOf()
        }
        children?.add(child)
    }

    fun removeChildAt(index: Int): FlexNode? {
        return children?.removeAt(index)?.also { removed ->
            removed.parent = null
        }
        markDirty()
    }

    fun clearChild() {
        children?.clear()
        markDirty()
    }

    fun indexOf(child: FlexNode): Int {
        return children?.indexOf(child) ?: -1
    }
    // ===== child操作相关 end =====

    fun calculateLayout(layoutContext: FlexLayoutContext?) {
        flexLayout.resetResult()
        val dirtyList = fastHashSetOf<FlexNode>()
        val maxWidth = if (!styleMaxWidth.isUndefined()) {
            styleMaxWidth
        } else {
            styleWidth
        }
        LayoutImpl.layoutNode(this, maxWidth, layoutContext, dirtyList = dirtyList)
        dirtyList.forEach {
            it.updateLastLayout()
            it.markNotDirty()
        }
    }

    fun resetLayout() {
        flexLayout.resetResult()
    }

    private fun updateLastLayout() {
        if (disableLayout) {
            return
        }
        lastLayout.copy(flexLayout)
        val newFrame = Frame(layoutX, layoutY, layoutWidth, layoutHeight)
        updateLayoutFrame(newFrame)
    }

    fun updateLayoutFrame(newFrame: Frame) {
        if (layoutFrame.isDefaultValue() || !layoutFrame.equals(newFrame)) {
            layoutFrame = newFrame
            layoutFrameDidChangedCallback?.invoke()
        }
    }

    fun updateLayoutUsingLast() {
        flexLayout.copy(lastLayout)
    }

    fun markDirty() {
        if (isDirty) {
            return
        }
        isDirty = true
        parent?.also {
            if (!it.isDirty) {
                it.markDirty()
            }
        }
        setNeedDirtyCallback?.invoke()
    }

    fun markNotDirty() {
        isDirty = false
    }

    fun markDisable() {
        markDirty()
        disableLayout = true
    }

    fun markEnable() {
        disableLayout = false
    }

    private fun setStyleSpace(spacingType: StyleSpace.Type, styleSpace: StyleSpace, value: Float) {
        when (spacingType) {
            StyleSpace.Type.ALL -> {
                styleSpace.apply {
                    set(StyleSpace.Type.LEFT, value)
                    set(StyleSpace.Type.TOP, value)
                    set(StyleSpace.Type.RIGHT, value)
                    set(StyleSpace.Type.BOTTOM, value)
                }
            }
            StyleSpace.Type.HORIZONTAL -> {
                styleSpace.apply {
                    set(StyleSpace.Type.LEFT, value)
                    set(StyleSpace.Type.RIGHT, value)
                }
            }
            StyleSpace.Type.VERTICAL -> {
                styleSpace.apply {
                    set(StyleSpace.Type.TOP, value)
                    set(StyleSpace.Type.BOTTOM, value)
                }
            }
            else -> styleSpace.set(spacingType, value)
        }
    }

    companion object {
        const val TAG = "FlexNode"
    }
}

interface MeasureFunction {
    /**
     * Should measure the given node and put the result in the given MeasureOutput.
     * NB: measure is NOT guaranteed to be threadsafe/re-entrant safe!
     */
    fun measure(
        node: FlexNode,
        width: Float,
        height: Float,
        measureOutput: MeasureOutput
    )
}

class MeasureOutput {
    var width = 0f
    var height = 0f
}

class FlexLayoutContext {
    val measureOutput = MeasureOutput()
}

class MutableFrame(var x: Float,
                    var y: Float,
                    var width: Float,
                    var height: Float
) {
    fun toFrame(): Frame {
        return Frame(x, y, width, height)
    }
}

data class Frame(val x: Float = 0f,
            val y: Float = 0f,
            val width: Float = 0f,
            val height: Float = 0f
) {

    companion object  {
        val zero = Frame(0f, 0f, 0f, 0f)
    }

    fun toMutableFrame() : MutableFrame {
        return MutableFrame(x, y, width, height)
    }

    override fun equals(other: Any?): Boolean {
        val otherFrame = other as Frame
        return x == otherFrame.x
                && y == otherFrame.y
                && width == otherFrame.width
                && height == otherFrame.height
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    fun isDefaultValue(): Boolean {
        return this === zero
    }

    fun minY(): Float {
        return y
    }

    fun maxY(): Float {
        return y + height
    }

    fun minX(): Float {
        return x
    }

    fun maxX(): Float {
        return x + width
    }
    fun midX(): Float {
        return minX() + width / 2
    }
    fun midY(): Float {
        return minY() + height / 2
    }

    override fun toString(): String {
        return "x:$x y:$y width:$width height:$height"
    }
}