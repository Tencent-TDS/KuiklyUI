package com.tencent.kuikly.core.render.web.runtime.dom.element

import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import org.w3c.dom.HTMLElement

/**
 * Abstract ListView element interface
 */
interface IListElement {
    var ele: HTMLElement

    // Scroll callback
    var scrollEventCallback: KuiklyRenderCallback?

    // Drag begin callback
    var dragBeginEventCallback: KuiklyRenderCallback?

    // Drag end callback
    var dragEndEventCallback: KuiklyRenderCallback?

    // Will drag end callback
    var willDragEndEventCallback: KuiklyRenderCallback?

    // Scroll end drag
    var scrollEndEventCallback: KuiklyRenderCallback?

    /**
     * Scroll element to specified position
     */
    fun setContentOffset(params: String?)

    /**
     * Set content margin with animation
     */
    fun setContentInset(params: String?)

    /**
     * Set padding when drag ends, i.e. translateX and Y values
     */
    fun setContentInsetWhenEndDrag(params: String?)

    /**
     * Bind scroll-related event handlers
     */
    fun setScrollEvent()

    /**
     * Bind scroll end event
     */
    fun setScrollEndEvent()

    /**
     * Set whether scrolling is enabled
     */
    fun setScrollEnable(params: Any): Boolean

    /**
     * Set whether to show scroll indicator
     */
    fun setShowScrollIndicator(params: Any): Boolean

    /**
     * Set scroll direction
     */
    fun setScrollDirection(params: Any): Boolean

    /**
     * Set whether to enable paging scroll
     */
    fun setPagingEnable(params: Any): Boolean

    /**
     * enable bounce effect
     */
    fun setBounceEnable(params: Any): Boolean

    /**
     * Set list nested scroll props
     */
    fun setNestedScroll(propValue: Any): Boolean

    /**
     * update offset
     */
    fun updateOffsetMap(offsetX: Float, offsetY: Float, isDragging: Int): MutableMap<String, Any>

    /**
     * Callback to be executed when component is destroyed
     */
    fun destroy()
}