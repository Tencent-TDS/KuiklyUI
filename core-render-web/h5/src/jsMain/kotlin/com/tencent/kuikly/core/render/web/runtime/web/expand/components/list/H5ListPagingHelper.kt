package com.tencent.kuikly.core.render.web.runtime.web.expand.components.list

import com.tencent.kuikly.core.render.web.expand.components.toPanEventParams
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import com.tencent.kuikly.core.render.web.utils.Log
import org.w3c.dom.HTMLElement
import org.w3c.dom.TouchEvent
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
    // Whether bounce effect is enabled
    var bounceEnabled: Boolean = false

    // Scroll direction
    var scrollDirection: String = H5ListView.SCROLL_DIRECTION_COLUMN
    // Whether currently dragging
    private var isDragging = 0

    // Add property to track iOS version
    private val isIOS14OrLower: Boolean = checkIOSVersion()

    // Add function to check iOS version
    private fun checkIOSVersion(): Boolean {
        val userAgent = kuiklyWindow.navigator.userAgent
        val match = Regex("OS (\\d+)_").find(userAgent)
        if (match != null) {
            val version = match.groupValues[1].toIntOrNull()
            return version != null && version <= 14
        }
        return false
    }

    // Add function to set position based on iOS version
    private fun setElementPosition(x: Float, y: Float) {
        if (isIOS14OrLower) {
            // Use absolute positioning for iOS 14 and lower
            (ele.firstElementChild as HTMLElement).style.left = "${x}px"
            (ele.firstElementChild as HTMLElement).style.top = "${y}px"
        } else {
            // Use transform for other versions
            ele.style.transform = "translate(${x}px, ${y}px)"
        }
    }

    /**
     * Handle paging calculation for TouchStart gesture
     */
    fun handlePagerTouchStart(it: TouchEvent) {
        isDragging = 1
        lastTouchEvent = it
        ele.style.overflowX = "visible"
        ele.style.overflowY = "visible"
        // Set position absolute for iOS 14 and lower
        if (isIOS14OrLower) {
            ele.style.position = "relative"
            (ele.firstElementChild as HTMLElement).style.position = "absolute"
        }
        if (!ele.classList.contains("page-list")) {
            ele.classList.add("page-list")
            pageIndex = 0f
        }
        if (scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN) {
            ele.style.apply {
                setProperty("overscroll-behavior-y", if(bounceEnabled) "auto" else "none")
            }
            val containerHeight = (ele.firstElementChild as HTMLElement).offsetHeight.toFloat()
            val pageHeight = ele.offsetHeight.toFloat()
            pageMaxTranslateY = containerHeight - pageHeight
            pageCount = round(containerHeight / pageHeight)
        } else {
            ele.style.apply {
                setProperty("overscroll-behavior-x", if(bounceEnabled) "auto" else "none")
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
        val eventsParams = it.unsafeCast<TouchEvent>().toPanEventParams()
        // Record starting vertical drag position
        touchStartY = eventsParams["y"].unsafeCast<Float>()
        // Record starting horizontal drag position
        touchStartX = eventsParams["x"].unsafeCast<Float>()

        val offsetMap = listElement.updateOffsetMap(offsetX, offsetY, isDragging)
        // Event callback
        listElement.dragBeginEventCallback?.invoke(offsetMap)
    }

    /**
     * Handle paging calculation for TouchMove gesture
     */
    fun handlePagerTouchMove(it: TouchEvent) {
        if (lastTouchEvent == null) {
            return
        }

        val lastEventsParams = (lastTouchEvent as TouchEvent).toPanEventParams()
        val eventsParams = it.toPanEventParams()
        lastTouchEvent = it
        touchEndY = eventsParams["y"] as Float
        touchEndX = eventsParams["x"] as Float
        val lastEventY = lastEventsParams["y"] as Float
        val lastEventX = lastEventsParams["x"] as Float
        val deltaY = touchEndY - lastEventY
        val deltaX = touchEndX - lastEventX
        val absDeltaY = abs(deltaY)
        val absDeltaX = abs(deltaX)
        var delta = 0f
        var canScroll = true
        var needParentNodeScroll = false
        if (scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN
            && absDeltaY > absDeltaX) {
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
        } else if (
            scrollDirection == H5ListView.SCROLL_DIRECTION_ROW
            && absDeltaX > absDeltaY) {
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
        } else {
            needParentNodeScroll = true
        }
        if (needParentNodeScroll) {
            Log.trace("pagelist needParentNodeScroll")
            return
        } else {
            it.preventDefault()
            it.stopPropagation()
            if (!canScroll) {
                Log.trace("pagelist can't scroll")
                return
            }
        }
        Log.trace("pagelist scroll")
        setElementPosition(currentTranslateX, currentTranslateY)
        if (abs(delta) < H5ListView.SCROLL_CAPTURE_THRESHOLD) {
            return
        }
        isTouchMove = true
        val offsetMap = listElement.updateOffsetMap(abs(currentTranslateX), abs(currentTranslateY), isDragging)
        listElement.scrollEventCallback?.invoke(offsetMap)
    }

    /**
     * Handle paging calculation for TouchEnd gesture
     */
    fun handlePagerTouchEnd(it: TouchEvent) {
        if (!isTouchMove) {
            return
        }
        isDragging = 0
        isTouchMove = false
        val deltaY = touchEndY - touchStartY
        val deltaX = touchEndX - touchStartX
        val offset = 50f
        var scrollOffsetX = 0f
        var scrollOffsetY = 0f
        var newPageIndex = pageIndex
        if (scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN) {
            Log.trace("delta y: ", deltaY, " currentTranslateY: ", currentTranslateY)
            newPageIndex = getNewPageIndex(deltaY, offset, newPageIndex)
            scrollOffsetY = -ele.offsetHeight * newPageIndex
            currentTranslateY = scrollOffsetY
        } else {
            Log.trace("delta x: ", deltaX, " currentTranslateX: ", currentTranslateX)
            newPageIndex = getNewPageIndex(deltaX, offset, newPageIndex)
            scrollOffsetX = -ele.offsetWidth * newPageIndex
            currentTranslateX = scrollOffsetX
        }
        if (newPageIndex != pageIndex) {
            pageIndex = newPageIndex
            it.stopPropagation()
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
                if (isIOS14OrLower) {
                    (ele.firstElementChild as HTMLElement).style.transition = "all ${PAGING_SCROLL_ANIMATION_TIME}ms"
                } else {
                    ele.style.transition = "transform ${PAGING_SCROLL_ANIMATION_TIME}ms"
                }
            }
            setElementPosition(scrollOffsetX, scrollOffsetY)
        }, PAGING_SCROLL_DELAY)
        if (isAnimation) {
            kuiklyWindow.setTimeout({
                if (isIOS14OrLower) {
                    (ele.firstElementChild as HTMLElement).style.transition = ""
                } else {
                    ele.style.transition = ""
                }
            }, PAGING_SCROLL_ANIMATION_TIME)
        }
    }

    fun setContentOffset(offsetX: Float, offsetY: Float, animate: Boolean) {
        val elementHeight = ele.offsetHeight.toFloat()
        val elementWidth = ele.offsetWidth.toFloat()
        if(scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN && elementHeight > 0) {
            pageIndex = round(offsetY / elementHeight)
            currentTranslateY = -offsetY
        } else if (scrollDirection == H5ListView.SCROLL_DIRECTION_ROW && elementWidth > 0){
            pageIndex = round(offsetX / elementWidth)
            currentTranslateX = -offsetX
        } else {
            Log.trace("ele offset is invalid", elementWidth, elementHeight)
        }
        ele.style.overflowX = "visible"
        ele.style.overflowY = "visible"
        ele.classList.add("page-list")
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
            }, 200)
        }
        // This is to handle nested PageLists. After sliding the inner PageList, clicking the outer PageList needs to hide the overflow position of the inner node
        // Implementation is not very elegant, pending refactoring with reference to swiper logic
        val length = ele.firstElementChild?.children?.length
        if (length != null){
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
        if (element.classList.contains("page-list")) {
            if (isVisible) {
                element.style.overflowX = "visible"
                element.style.overflowY = "visible"
            } else {
                element.style.overflowX = "hidden"
                element.style.overflowY = "hidden"
            }

        }

        // Recursively traverse all child nodes
        for (i in 0 until element.children.length) {
            modifyOverflowIfPageList(element.children[i] as HTMLElement, isVisible)
        }
    }

    companion object {
        private const val PAGING_SCROLL_DELAY = 20
        private const val PAGING_SCROLL_ANIMATION_TIME = 200
    }
}