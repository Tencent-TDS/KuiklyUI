package com.tencent.kuikly.core.render.web.runtime.web.expand.components.list

import com.tencent.kuikly.core.render.web.const.KRListConst
import com.tencent.kuikly.core.render.web.const.KRParamConst
import com.tencent.kuikly.core.render.web.const.KRStyleConst
import com.tencent.kuikly.core.render.web.expand.components.toPanEventParams
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import com.tencent.kuikly.core.render.web.utils.Log
import org.w3c.dom.HTMLElement
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.get
import kotlin.math.abs
import kotlin.math.round

/**
 * Helper class to handle paging logic for WebListElement.
 */
class H5ListPagingHelper(private val ele: HTMLElement, private var listElement: IListElement) {
    // Current translate X distance, property for paging mode
    var currentTranslateX: Float = 0f
    // Current translate Y distance, property for paging mode
    var currentTranslateY: Float = 0f
    // Maximum translate X distance, property for paging mode
    var pageMaxTranslateX: Float = 0f
    // Maximum translate Y distance, property for paging mode
    var pageMaxTranslateY: Float = 0f
    // Starting vertical touch position
    private var touchStartY = 0f
    // Current vertical touch position
    private var touchEndY = 0f
    // Starting horizontal touch position
    private var touchStartX = 0f
    // Current horizontal touch position
    private var touchEndX = 0f
    // Maximum page count, property for paging mode
    var pageCount: Float = 0f
    // Current page index, property for paging mode
    var pageIndex: Float = 0f
    // Whether touchmove gesture was triggered, used to filter click events, property for paging mode
    var isTouchMove: Boolean = false
    // Last touch event, property for paging mode
    var lastTouchEvent: TouchEvent? = null
    // Last mouse event, property for paging mode
    var lastMouseEvent: MouseEvent? = null
    // Whether bounce effect is enabled
    var bounceEnabled: Boolean = false
    // Whether the mouse is pressed
    private var isMouseDown: Boolean = false
    // Scroll direction
    var scrollDirection: String = KRListConst.SCROLL_DIRECTION_COLUMN
    // Whether currently dragging
    private var isDragging = 0
    // Whether scrolling is possible
    var canScroll: Boolean = false
        private set
    // Whether wheel page switching is locked (prevent multiple page switches)
    private var isWheelPageLocked: Boolean = false
    // Accumulated wheel delta for determining scroll direction
    private var accumulatedWheelDelta: Double = 0.0
    // Wheel scroll reset timer
    private var wheelResetTimer: Int? = null

    // Set element position using transform
    private fun setElementPosition(x: Float, y: Float) {
        ele.style.transform = "translate(${x}px, ${y}px)"
    }

    /**
     * Handle paging calculation for TouchStart gesture
     */
    fun handlePagerTouchStart(it: TouchEvent) {
        handlePagerStartCommon(it)
    }

    /**
     * Handle paging calculation for TouchMove gesture
     */
    fun handlePagerTouchMove(it: TouchEvent) {
        if (lastTouchEvent == null) {
            return
        }
        handlePagerMoveCommon(it, lastTouchEvent!!)
    }

    /**
     * Handle paging calculation for TouchEnd gesture
     */
    fun handlePagerTouchEnd(it: TouchEvent) {
        handlePagerEndCommon(it)
    }

    /**
     * Handle paging calculation for MouseDown gesture
     */
    fun handlePagerMouseDown(it: MouseEvent) {
        isMouseDown = true
        handlePagerStartCommon(it)
    }

    /**
     * Handle paging calculation for MouseMove gesture
     */
    fun handlePagerMouseMove(it: MouseEvent) {
        // 上一次鼠标事件，直接返回。(指 MouseDown 事件)
        if (lastMouseEvent == null) {
            return
        }
        if (!isMouseDown) {
            return
        }
        handlePagerMoveCommon(it, lastMouseEvent!!)
    }

    /**
     * Handle paging calculation for MouseUp gesture
     */
    fun handlePagerMouseUp(it: MouseEvent) {
        isMouseDown = false
        handlePagerEndCommon(it)
    }

    /**
     * Handle paging calculation for Wheel event
     * Uses accumulated delta and lock mechanism to ensure only one page switch per wheel gesture
     * @param event WheelEvent from wheel listener
     * @return true if the event was handled, false otherwise
     */
    fun handlePagerWheel(event: WheelEvent): Boolean {
        event.preventDefault()
        event.stopPropagation()

        // Initialize paging state if not already done
        initPagingStateIfNeeded()

        // Get delta based on scroll direction
        val delta = if (scrollDirection == KRListConst.SCROLL_DIRECTION_COLUMN) {
            event.deltaY
        } else {
            event.deltaX
        }

        // Accumulate delta
        accumulatedWheelDelta += delta

        // Reset the accumulated delta after wheel events stop
        wheelResetTimer?.let { kuiklyWindow.clearTimeout(it) }
        wheelResetTimer = kuiklyWindow.setTimeout({
            accumulatedWheelDelta = 0.0
            isWheelPageLocked = false
        }, WHEEL_RESET_TIMEOUT)

        // If page is locked or accumulated delta is too small, skip
        if (isWheelPageLocked || abs(accumulatedWheelDelta) < WHEEL_DELTA_THRESHOLD) {
            return true
        }

        // Calculate new page index based on accumulated delta direction
        var newPageIndex = pageIndex
        if (accumulatedWheelDelta > 0) {
            // Scroll down/right -> next page
            if (pageIndex < pageCount - 1) {
                newPageIndex = pageIndex + 1
            }
        } else {
            // Scroll up/left -> previous page
            if (pageIndex > 0) {
                newPageIndex = pageIndex - 1
            }
        }

        // If page index changed, perform page switch
        if (newPageIndex != pageIndex) {
            // Lock page switching until wheel gesture ends
            isWheelPageLocked = true
            pageIndex = newPageIndex

            // Calculate scroll offset
            val scrollOffsetX: Float
            val scrollOffsetY: Float
            if (scrollDirection == KRListConst.SCROLL_DIRECTION_COLUMN) {
                scrollOffsetY = -ele.offsetHeight * pageIndex
                scrollOffsetX = currentTranslateX
                currentTranslateY = scrollOffsetY
            } else {
                scrollOffsetX = -ele.offsetWidth * pageIndex
                scrollOffsetY = currentTranslateY
                currentTranslateX = scrollOffsetX
            }

            // Perform scroll animation
            handlePagerScrollTo(scrollOffsetX, scrollOffsetY, true)

            // Notify callbacks
            val offsetMap = listElement.updateOffsetMap(abs(currentTranslateX), abs(currentTranslateY), isDragging)
            listElement.scrollEventCallback?.invoke(offsetMap)
        }

        return true
    }

    /**
     * Initialize paging state if not already initialized
     */
    private fun initPagingStateIfNeeded() {
        if (!ele.classList.contains(PAGE_LIST_CLASS)) {
            ele.classList.add(PAGE_LIST_CLASS)
            ele.style.overflowX = KRStyleConst.OVERFLOW_VISIBLE
            ele.style.overflowY = KRStyleConst.OVERFLOW_VISIBLE
            pageIndex = 0f
        }

        // Update page dimensions
        if (scrollDirection == KRListConst.SCROLL_DIRECTION_COLUMN) {
            val containerHeight = (ele.firstElementChild as HTMLElement).offsetHeight.toFloat()
            val pageHeight = ele.offsetHeight.toFloat()
            if (pageHeight > 0) {
                pageMaxTranslateY = containerHeight - pageHeight
                pageCount = round(containerHeight / pageHeight)
            }
        } else {
            val containerWidth = (ele.firstElementChild as HTMLElement).offsetWidth.toFloat()
            val pageWidth = ele.offsetWidth.toFloat()
            if (pageWidth > 0) {
                pageMaxTranslateX = containerWidth - pageWidth
                pageCount = round(containerWidth / pageWidth)
            }
        }
    }

    /**
     * Handle the same part of the paging calculation for TouchStart and MouseDown
     */
    private fun handlePagerStartCommon(event: Event) {
        isDragging = 1
        // 针对safari浏览器没有 TouchEvent
        if (event.isTouchEventOrNull() != null) {
            lastTouchEvent = event as TouchEvent
        } else if (event is MouseEvent) {
            lastMouseEvent = event
        }
        ele.style.overflowX = KRStyleConst.OVERFLOW_VISIBLE
        ele.style.overflowY = KRStyleConst.OVERFLOW_VISIBLE
        if (!ele.classList.contains(PAGE_LIST_CLASS)) {
            ele.classList.add(PAGE_LIST_CLASS)
            pageIndex = 0f
        }
        if (scrollDirection == KRListConst.SCROLL_DIRECTION_COLUMN) {
            ele.style.apply {
                setProperty(OVERSCROLL_BEHAVIOR_Y, if (bounceEnabled) OVERSCROLL_AUTO else OVERSCROLL_NONE)
            }
            val containerHeight = (ele.firstElementChild as HTMLElement).offsetHeight.toFloat()
            val pageHeight = ele.offsetHeight.toFloat()
            pageMaxTranslateY = containerHeight - pageHeight
            pageCount = round(containerHeight / pageHeight)
        } else {
            ele.style.apply {
                setProperty(OVERSCROLL_BEHAVIOR_X, if (bounceEnabled) OVERSCROLL_AUTO else OVERSCROLL_NONE)
            }
            val containerWidth = (ele.firstElementChild as HTMLElement).offsetWidth.toFloat()
            val pageWidth = ele.offsetWidth.toFloat()
            pageMaxTranslateX = containerWidth - pageWidth
            pageCount = round(containerWidth / pageWidth)
        }
        // Get horizontal and vertical offset of the element during scroll event
        val offsetX = ele.scrollLeft.toFloat()
        val offsetY = ele.scrollTop.toFloat()
        // Starting drag position map
        val eventsParams = event.getEventParams()
        // Record starting vertical drag position
        touchStartY = eventsParams[KRParamConst.Y].unsafeCast<Float>()
        // Record starting horizontal drag position
        touchStartX = eventsParams[KRParamConst.X].unsafeCast<Float>()

        val offsetMap = listElement.updateOffsetMap(offsetX, offsetY, isDragging)
        // Event callback
        listElement.dragBeginEventCallback?.invoke(offsetMap)
    }

    /**
     * Handle the same part of the paging calculation for TouchMove and MouseMove
     */
    private fun handlePagerMoveCommon(event: Event, lastEvent: Event) {
        val eventsParams = event.getEventParams()
        touchEndY = eventsParams[KRParamConst.Y] as Float
        touchEndX = eventsParams[KRParamConst.X] as Float
        val lastEventsParams = lastEvent.getEventParams()
        val lastEventY = lastEventsParams[KRParamConst.Y] as Float
        val lastEventX = lastEventsParams[KRParamConst.X] as Float
        val deltaY = touchEndY - lastEventY
        val deltaX = touchEndX - lastEventX
        val absDeltaY = abs(deltaY)
        val absDeltaX = abs(deltaX)
        var delta = 0f
        canScroll = true
        var needParentNodeScroll = false
        if (scrollDirection == KRListConst.SCROLL_DIRECTION_COLUMN && absDeltaY > absDeltaX) {
            delta = deltaY
            currentTranslateY += deltaY
            if (deltaY > 0) {
                if (currentTranslateY > 0) {
                    currentTranslateY = 0f
                    if (pageIndex == 0f) {
                        canScroll = false
                    }
                }
            } else {
                if (currentTranslateY < -pageMaxTranslateY) {
                    currentTranslateY = -pageMaxTranslateY
                    if (pageIndex == pageCount - 1) {
                        canScroll = false
                    }
                }
            }
        } else if (scrollDirection == KRListConst.SCROLL_DIRECTION_ROW && absDeltaX > absDeltaY) {
            delta = deltaX
            currentTranslateX += deltaX
            if (deltaX > 0) {
                if (currentTranslateX >= 0) {
                    currentTranslateX = 0f
                    if (pageIndex == 0f) {
                        canScroll = false
                    }
                }
            } else {
                if (currentTranslateX <= -pageMaxTranslateX) {
                    currentTranslateX = -pageMaxTranslateX
                    if (pageIndex == pageCount - 1) {
                        canScroll = false
                    }
                }
            }
        } else if (!isTouchMove) {
            // if node is move, not dispatch event parent
            needParentNodeScroll = true
        }
        if (needParentNodeScroll) {
            Log.trace("pagelist needParentNodeScroll")
            // 不处理时不更新 lastEvent，避免下一帧的 delta 计算错误
            return
        } else {
            event.preventDefault()
            event.stopPropagation()
            // 只有确定要处理时才更新 lastEvent
            if (event.isTouchEventOrNull() != null) {
                lastTouchEvent = event as TouchEvent
            } else if (event is MouseEvent) {
                lastMouseEvent = event
            }
            if (!canScroll) {
                Log.trace("pagelist can't scroll")
                return
            }
        }
        Log.trace("pagelist scroll")
        setElementPosition(currentTranslateX, currentTranslateY)
        if (abs(delta) < KRListConst.SCROLL_CAPTURE_THRESHOLD) {
            return
        }
        isTouchMove = true
        val offsetMap = listElement.updateOffsetMap(abs(currentTranslateX), abs(currentTranslateY), isDragging)
        listElement.scrollEventCallback?.invoke(offsetMap)
    }

    /**
     * Handle the same part of the paging calculation for TouchEnd and MouseUp
     */
    private fun handlePagerEndCommon(event: Event) {
        if (!isTouchMove) {
            return
        }
        isDragging = 0
        isTouchMove = false
        val deltaY = touchEndY - touchStartY
        val deltaX = touchEndX - touchStartX
        var scrollOffsetX = 0f
        var scrollOffsetY = 0f
        var newPageIndex = pageIndex
        if (scrollDirection == KRListConst.SCROLL_DIRECTION_COLUMN) {
            Log.trace("delta y: ", deltaY, " currentTranslateY: ", currentTranslateY)
            newPageIndex = getNewPageIndex(deltaY, PAGE_SWITCH_THRESHOLD, newPageIndex)
            scrollOffsetY = -ele.offsetHeight * newPageIndex
            currentTranslateY = scrollOffsetY
        } else {
            Log.trace("delta x: ", deltaX, " currentTranslateX: ", currentTranslateX)
            newPageIndex = getNewPageIndex(deltaX, PAGE_SWITCH_THRESHOLD, newPageIndex)
            scrollOffsetX = -ele.offsetWidth * newPageIndex
            currentTranslateX = scrollOffsetX
        }
        if (newPageIndex != pageIndex) {
            pageIndex = newPageIndex
            event.stopPropagation()
        }
        handlePagerScrollTo(scrollOffsetX, scrollOffsetY, true)
        val offsetMap = listElement.updateOffsetMap(abs(currentTranslateX), abs(currentTranslateY), isDragging)
        listElement.willDragEndEventCallback?.invoke(offsetMap)
        listElement.dragEndEventCallback?.invoke(offsetMap)
        listElement.scrollEventCallback?.invoke(offsetMap)
    }

    /**
     * Get new page index after sliding
     */
    private fun getNewPageIndex(delta: Float, offset: Float, newPageIndex: Float): Float {
        var resultPageIndex = newPageIndex
        if (abs(delta) > offset) {
            if (delta > 0) {
                if (this.pageIndex > 0) {
                    resultPageIndex = pageIndex - 1
                }
            } else {
                if (pageIndex < pageCount - 1) {
                    resultPageIndex = pageIndex + 1
                }
            }
        }
        return resultPageIndex
    }

    /**
     * Handle paging scroll to specified position
     */
    fun handlePagerScrollTo(scrollOffsetX: Float, scrollOffsetY: Float, isAnimation: Boolean) {
        kuiklyWindow.setTimeout({
            if (isAnimation) {
                ele.style.transition = "transform ${PAGING_SCROLL_ANIMATION_TIME}ms"
            }
            setElementPosition(scrollOffsetX, scrollOffsetY)
        }, PAGING_SCROLL_DELAY)
        if (isAnimation) {
            kuiklyWindow.setTimeout({
                ele.style.transition = ""
            }, PAGING_SCROLL_ANIMATION_TIME)
        }
    }

    fun setContentOffset(offsetX: Float, offsetY: Float, animate: Boolean) {
        val elementHeight = ele.offsetHeight.toFloat()
        val elementWidth = ele.offsetWidth.toFloat()
        if (scrollDirection == KRListConst.SCROLL_DIRECTION_COLUMN && elementHeight > 0) {
            pageIndex = round(offsetY / elementHeight)
            currentTranslateY = -offsetY
        } else if (scrollDirection == KRListConst.SCROLL_DIRECTION_ROW && elementWidth > 0) {
            pageIndex = round(offsetX / elementWidth)
            currentTranslateX = -offsetX
        } else {
            Log.trace("ele offset is invalid", elementWidth, elementHeight)
        }
        ele.style.overflowX = KRStyleConst.OVERFLOW_VISIBLE
        ele.style.overflowY = KRStyleConst.OVERFLOW_VISIBLE
        ele.classList.add(PAGE_LIST_CLASS)
        val offsetMap = listElement.updateOffsetMap(offsetX, offsetY, isDragging)
        listElement.willDragEndEventCallback?.invoke(offsetMap)
        listElement.dragEndEventCallback?.invoke(offsetMap)
        listElement.scrollEventCallback?.invoke(offsetMap)
        if (animate) {
            handlePagerScrollTo(-offsetX, -offsetY, animate)
        } else {
            // wait index changed
            kuiklyWindow.setTimeout({
                handlePagerScrollTo(-offsetX, -offsetY, animate)
            }, CONTENT_OFFSET_DELAY)
        }
        // This is to handle nested PageLists. After sliding the inner PageList,
        // clicking the outer PageList needs to hide the overflow position of the inner node
        // Implementation is not very elegant, pending refactoring with reference to swiper logic
        val length = ele.firstElementChild?.children?.length
        if (length != null) {
            for (i in 0 until length) {
                val element = ele.firstElementChild?.children?.get(i) as HTMLElement
                if (element.offsetLeft.toFloat() == offsetX) {
                    modifyOverflowIfPageList(element, true)
                } else {
                    modifyOverflowIfPageList(element, false)
                }
            }
        }
    }

    private fun modifyOverflowIfPageList(element: HTMLElement, isVisible: Boolean) {
        // Check if the current element's class contains page-list
        if (element.classList.contains(PAGE_LIST_CLASS)) {
            if (isVisible) {
                element.style.overflowX = KRStyleConst.OVERFLOW_VISIBLE
                element.style.overflowY = KRStyleConst.OVERFLOW_VISIBLE
            } else {
                element.style.overflowX = KRStyleConst.OVERFLOW_HIDDEN
                element.style.overflowY = KRStyleConst.OVERFLOW_HIDDEN
            }
        }

        // Recursively traverse all child nodes
        for (i in 0 until element.children.length) {
            modifyOverflowIfPageList(element.children[i] as HTMLElement, isVisible)
        }
    }

    /**
     * Clean up resources when helper is destroyed
     */
    fun destroy() {
        // Clear wheel reset timer
        wheelResetTimer?.let {
            kuiklyWindow.clearTimeout(it)
        }
        wheelResetTimer = null
    }

    companion object {
        // Timing constants (in milliseconds)
        private const val PAGING_SCROLL_DELAY = 20
        private const val PAGING_SCROLL_ANIMATION_TIME = 200
        private const val CONTENT_OFFSET_DELAY = 200
        // Timeout to reset wheel state after wheel events stop (ms)
        private const val WHEEL_RESET_TIMEOUT = 150
        // Minimum accumulated wheel delta to trigger page switch
        private const val WHEEL_DELTA_THRESHOLD = 30.0
        // Minimum drag distance to trigger page switch (in pixels)
        private const val PAGE_SWITCH_THRESHOLD = 50f

        // CSS class name for page list
        private const val PAGE_LIST_CLASS = "page-list"

        // CSS overscroll-behavior property names and values
        private const val OVERSCROLL_BEHAVIOR_X = "overscroll-behavior-x"
        private const val OVERSCROLL_BEHAVIOR_Y = "overscroll-behavior-y"
        private const val OVERSCROLL_AUTO = "auto"
        private const val OVERSCROLL_NONE = "none"
    }
}