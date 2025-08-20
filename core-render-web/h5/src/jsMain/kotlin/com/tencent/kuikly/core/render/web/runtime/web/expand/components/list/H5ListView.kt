package com.tencent.kuikly.core.render.web.runtime.web.expand.components.list

import com.tencent.kuikly.core.render.web.expand.components.list.KRListViewContentInset
import com.tencent.kuikly.core.render.web.expand.components.toPanEventParams
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreContextScheduler
import org.w3c.dom.AUTO
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.SMOOTH
import org.w3c.dom.ScrollBehavior
import org.w3c.dom.ScrollToOptions
import org.w3c.dom.TouchEvent
import kotlin.js.json
import kotlin.math.abs

/**
 * Web host abstract List element implementation
 */
class H5ListView : IListElement {
    // Scroll container element
    private val listEle = kuiklyDocument.createElement(ElementType.DIV).apply {
        // By default, allow scrolling in vertical direction. To hide scrollbars,
        // add 'list-no-scrollbar' class to the element
        this.unsafeCast<HTMLDivElement>().style.apply {
            // Due to bounce effect on iOS, non-scrolling direction should be set to "hidden"
            overflowX = "hidden"
            overflowY = "scroll"
        }
    }
    // Scroll end event listener
    private var scrollEndEventTimer: Int = 0
    // Scroll offset Map
    private var offsetMap = mutableMapOf<String, Any>()
    // Starting horizontal scroll offset
    private var startX = 0f
    // Starting vertical scroll offset
    private var startY = 0f
    // Starting vertical touch position
    private var touchStartY = 0f
    // Current vertical touch position
    private var touchEndY = 0f
    // Starting horizontal touch position
    private var touchStartX = 0f
    // Current horizontal touch position
    private var touchEndX = 0f
    // Whether scrolling is enabled
    private var scrollEnabled = true
    // Whether to show scrollbar
    private var showScrollerBar = true
    // Scroll direction
    private var scrollDirection = SCROLL_DIRECTION_COLUMN
    // Actual calculated scroll direction
    private var calculateDirection = SCROLL_DIRECTION_NONE
    // Whether currently dragging
    private var isDragging = 0
    // Whether paging is enabled
    private var pagingEnabled = false
    // enable bounce effect, support Android Webview 63+ && iOS Safari 16+
    private var bounceEnabled = false
    // enable nest scroll effect
    private var nestScrollEnabled = false
    // Whether in pre-pull-down state
    private var isPrePullDown = false
    // Pull-to-refresh height
    private var canPullRefreshHeight = 0f
    // Whether it contains pull-to-refresh child node
    private var hasRefreshChild = false
    // Scroll distance threshold
    private val scrollThreshold = 8
    // Whether in scrolling state
    private var isScrolling = -1

    // real html element
    override var ele: HTMLElement = listEle.unsafeCast<HTMLElement>()


    // Scroll callback
    override var scrollEventCallback: KuiklyRenderCallback? = null
    // Drag begin callback
    override var dragBeginEventCallback: KuiklyRenderCallback? = null
    // Drag end callback
    override var dragEndEventCallback: KuiklyRenderCallback? = null
    // Will drag end callback
    override var willDragEndEventCallback: KuiklyRenderCallback? = null
    // Scroll end callback
    override var scrollEndEventCallback: KuiklyRenderCallback? = null

    private var listPagingHelper: H5ListPagingHelper = H5ListPagingHelper(ele, this)
    private var nestScrollHelper: H5NestScrollHelper = H5NestScrollHelper(ele, this)

    /**
     * Set whether listView can scroll
     */
    override fun setScrollEnable(params: Any): Boolean {
        // Set the switch for whether scrolling is enabled
        scrollEnabled = params.unsafeCast<Int>() == 1
        // Set scrolling
        ele.style.apply {
            if (scrollDirection == SCROLL_DIRECTION_COLUMN) {
                overflowY = if (scrollEnabled) "scroll" else "hidden"
                overflowX = "hidden"
            } else {
                overflowX = if (scrollEnabled) "scroll" else "hidden"
                overflowY = "hidden"
            }
        }
        return true
    }

    override fun setBounceEnable(params: Any): Boolean {
        bounceEnabled = params.unsafeCast<Int>() == 1
        listPagingHelper.bounceEnabled = bounceEnabled
        return true
    }

    override fun setNestedScroll(propValue: Any): Boolean {
        nestScrollEnabled = true
        nestScrollHelper.setNestedScroll(propValue)
        return true
    }

    /**
     * Set whether to enable paging
     */
    override fun setPagingEnable(params: Any): Boolean {
        // Whether to enable paging
        pagingEnabled = params.unsafeCast<Int>() == 1
        return true
    }

    /**
     * Set the scroll direction of listView, 1 for horizontal, 0 for vertical
     */
    override fun setScrollDirection(params: Any): Boolean {
        val direction = if (params.unsafeCast<Int>() == 1) SCROLL_DIRECTION_ROW else SCROLL_DIRECTION_COLUMN
        // Set scroll direction
        ele.style.apply {
            if (direction == SCROLL_DIRECTION_COLUMN) {
                overflowX = "hidden"
                overflowY = "scroll"
            } else {
                overflowX = "scroll"
                overflowY = "hidden"
            }
        }
        scrollDirection = direction
        listPagingHelper.scrollDirection = scrollDirection
        nestScrollHelper.scrollDirection = scrollDirection
        return true
    }

    /**
     * Check if it contains pull-to-refresh child node
     */
    private fun checkHasRefreshChild(): Boolean {
        // Check the first child element to see if it's a pull-to-refresh node. Since the listView
        // implementation wraps a ScrollContentView,
        // which then wraps the actual scrollable content, we need to get the child node of ScrollContentView
        val firstChild = ele.firstElementChild?.firstElementChild.unsafeCast<HTMLElement?>()

        if (firstChild !== null) {
            // Determine if the first child node is a pull-to-refresh node. This is a hardcoded way to check,
            // todo: optimize for a more reasonable approach
            return firstChild.style.transform.contains("translate(0%, -100%) rotate(0deg) scale(1, 1) skew(0deg, 0deg)")
        }
        return false
    }

    override fun updateOffsetMap(offsetX: Float, offsetY: Float, isDragging: Int): MutableMap<String, Any> {
        offsetMap["offsetX"] = offsetX
        offsetMap["offsetY"] = offsetY
        offsetMap["viewWidth"] = ele.offsetWidth
        offsetMap["viewHeight"] = ele.offsetHeight
        offsetMap["contentWidth"] = ele.scrollWidth
        offsetMap["contentHeight"] = ele.scrollHeight
        offsetMap["isDragging"] = isDragging
        return offsetMap
    }

    fun handleTouchStart(it: TouchEvent) {
        // Set as dragging
        isDragging = 1
        // Clear pull-to-refresh height
        canPullRefreshHeight = 0f
        // Check if it contains pull-to-refresh child node
        hasRefreshChild = checkHasRefreshChild()
        // Reset scrolling state
        isScrolling = -1
        // Get horizontal and vertical offset of the element during scroll event
        val offsetX = ele.scrollLeft.toFloat()
        val offsetY = ele.scrollTop.toFloat()
        // Record scrollbar position at start of sliding
        startX = offsetX
        startY = offsetY
        // Starting drag position map
        val eventsParams = it.unsafeCast<TouchEvent>().toPanEventParams()
        // Record starting vertical drag position
        touchStartY = eventsParams["y"].unsafeCast<Float>()
        // Record starting horizontal drag position
        touchStartX = eventsParams["x"].unsafeCast<Float>()
        // Current vertical offset of the list
        offsetMap["offsetX"] = offsetX
        // Current horizontal offset of the list
        offsetMap["offsetY"] = offsetY
        val offsetMap = updateOffsetMap(offsetX, offsetY, isDragging)
        // If current scroll distance is 0, and not a PageList paging component, enter pre-pull-down state
        isPrePullDown = offsetY == 0f && !pagingEnabled

        // Event callback
        dragBeginEventCallback?.invoke(offsetMap)
    }

    fun handleTouchMove(it: TouchEvent) {
        // Need to check if it contains pull-to-refresh component, if not, don't process todo fixme
        val eventsParams = it.unsafeCast<TouchEvent>().toPanEventParams()
        val deltaY = eventsParams["y"] as Float - touchStartY
        val deltaX = eventsParams["x"] as Float - touchStartX
        val absDeltaY = abs(deltaY)
        val absDeltaX = abs(deltaX)

        // If not yet in scrolling state, determine scroll direction, once determined don't change
        if (isScrolling == -1) {
            if (absDeltaY > scrollThreshold && absDeltaY > absDeltaX) {
                // Vertical scrolling
                isScrolling = 1
            } else if (absDeltaX > scrollThreshold && absDeltaX > absDeltaY) {
                // Horizontal scrolling
                isScrolling = 0
            }
        }
        if ((scrollDirection == SCROLL_DIRECTION_COLUMN && isScrolling == 1) ||
            (scrollDirection == SCROLL_DIRECTION_ROW && isScrolling == 0)) {
            // Scroll direction matches set direction, prevent bubbling to avoid affecting parent node's scroll events
            it.stopPropagation()
        }
        // If current scroll distance is 0, starting to drag down, contains pull-to-refresh child node,
        // and is vertical scrolling, handle pull-to-refresh logic, deltaY > 0 means pulling down
        if (isPrePullDown && deltaY > 0 && hasRefreshChild && isScrolling == 1) {
            // Set end position before drag ends
            touchEndY = eventsParams["y"].unsafeCast<Float>()
            // Set element's translate
            ele.style.transform = "translate(0, ${deltaY}px)"
            // During pull-to-refresh, set overflow to visible, restore after pull-to-refresh completes
            ele.style.overflowY = "visible"
            ele.style.overflowX = "visible"
            val offsetMap = updateOffsetMap(ele.scrollLeft.toFloat(), -deltaY, isDragging)
            // Notify
            scrollEventCallback?.invoke(offsetMap)
        }
    }

    fun handleTouchEnd() {
        isDragging = 0
        // Get horizontal and vertical offset of the element during scroll event
        val offsetX = ele.scrollLeft.toFloat()
        var offsetY = ele.scrollTop.toFloat()
        if (isPrePullDown) {
            // Special handling for pull-to-refresh
            val deltaY = touchEndY - touchStartY
            if (canPullRefreshHeight == 0f) {
                // If at pull-to-refresh release but not reaching pull-to-refresh position,
                // need to restore contentInset and scrolling
                ele.style.transform = "translate(0, 0)"
                // Handle extreme sliding in static sliding scenarios
                if (scrollEnabled) {
                    ele.style.overflowY = "scroll"
                }

                // remove transform attribute after transform end
                kuiklyWindow.setTimeout({
                    ele.style.transform = ""
                }, 0)
            } else if (deltaY > canPullRefreshHeight) {
                ele.style.transition = "transform ${BOUND_BACK_DURATION}ms $REFRESH_TIMING_FUNCTION"
                // If at pull-to-refresh release and exceeding pull-to-refresh height,
                // need to bounce back to pull-to-refresh height before refreshing
                ele.style.transform = "translate(0, ${canPullRefreshHeight}px)"
            }
            // If current scroll distance is 0 and starting to drag down, handle pull-to-refresh logic,
            // deltaY > 0 means pulling down
            if (deltaY > 0) {
                // Result is negative
                offsetY = -deltaY
            }
        }
        // Current vertical offset of the list
        offsetMap["offsetX"] = offsetX
        // Current horizontal offset of the list
        offsetMap["offsetY"] = offsetY
        val offsetMap = updateOffsetMap(offsetX, offsetY, isDragging)
        // Event callback
        willDragEndEventCallback?.invoke(offsetMap)
        dragEndEventCallback?.invoke(offsetMap)
        scrollEventCallback?.invoke(offsetMap)
    }

    fun handleTouchScroll() {
        // Get horizontal and vertical offset of the element during scroll event
        val offsetMap = updateOffsetMap(ele.scrollLeft.toFloat(), ele.scrollTop.toFloat(), isDragging)
        // Callback with offset
        scrollEventCallback?.invoke(offsetMap)
    }


    /**
     * Bind scroll-related events
     */
    override fun setScrollEvent() {
        // Start dragging
        ele.addEventListener(DRAG_BEGIN_EVENT, {
            if (pagingEnabled) {
                listPagingHelper.handlePagerTouchStart(it as TouchEvent)
                return@addEventListener
            }
            if (nestScrollEnabled) {
                nestScrollHelper.handleNestScrollTouchStart(it as TouchEvent)
                return@addEventListener
            }
            handleTouchStart(it as TouchEvent)
        }, json("passive" to true))

        // Move event
        ele.addEventListener(DRAG_MOVE_EVENT, {
            if (pagingEnabled) {
                listPagingHelper.handlePagerTouchMove(it as TouchEvent)
                return@addEventListener
            }
            if (nestScrollEnabled) {
                nestScrollHelper.handleNestScrollTouchMove(it as TouchEvent)
                return@addEventListener
            }
            handleTouchMove(it as TouchEvent)
        }, json("passive" to (!pagingEnabled && !nestScrollEnabled)))

        // End dragging
        ele.addEventListener(DRAG_END_EVENT, {
            if (pagingEnabled) {
                listPagingHelper.handlePagerTouchEnd(it as TouchEvent)
                return@addEventListener
            }
            if (nestScrollEnabled) {
                nestScrollHelper.handleNestScrollTouchEnd(it as TouchEvent)
                return@addEventListener
            }
            handleTouchEnd()
        }, json("passive" to true))

        // Scroll event
        ele.addEventListener(SCROLL, {
            if (pagingEnabled) {
                // In paging mode, no need to trigger scroll
                // Calculate offset through touchmove and touchend,
                // and callback scroll event to upper layer for processing
                return@addEventListener
            }
            if (nestScrollEnabled) {
                nestScrollHelper.handleNestScrollTouchScroll(it as TouchEvent)
                return@addEventListener
            }
            handleTouchScroll()
        }, json("passive" to false))
    }

    /**
     * Set scroll end callback event
     */
    override fun setScrollEndEvent() {
        // scroll end event not available, simulate through other means
        ele.addEventListener(SCROLL, {
            // Clear existing timer first
            if (scrollEndEventTimer > 0) {
                kuiklyWindow.clearTimeout(scrollEndEventTimer)
            }
            // Reset timer
            scrollEndEventTimer = kuiklyWindow.setTimeout({
                // Get horizontal and vertical offset of the element during scroll event
                var offsetMap = updateOffsetMap(ele.scrollLeft.toFloat(), ele.scrollTop.toFloat(), isDragging)
                scrollEndEventCallback?.invoke(offsetMap)
            }, SCROLL_END_OVERTIME)
        }, json("passive" to true))
    }

    /**
     * Scroll element to specified position
     */
    override fun setContentOffset(params: String?) {
        // Don't process if no parameters
        if (params === null) {
            return
        }

        // Format scroll parameters
        val contentOffsetSplits = params.split(KRCssConst.BLANK_SEPARATOR)
        val offsetX = contentOffsetSplits[0].toFloat()
        val offsetY = contentOffsetSplits[1].toFloat()
        val animate = contentOffsetSplits[2] == "1" // "1" means scroll with animation

        if (offsetX.isNaN() || offsetY.isNaN()) {
            // Position parameters abnormal, return
            return
        }
        if (pagingEnabled) {
            listPagingHelper.setContentOffset(offsetX, offsetY, animate);
            return
        }
        // Scroll to specified distance
        ele.scrollTo(
            ScrollToOptions(
                offsetX.toDouble(),
                offsetY.toDouble(),
                if (animate) ScrollBehavior.SMOOTH else ScrollBehavior.AUTO
            )
        )
    }

    /**
     * Set whether listView needs scrollbars
     */
    override fun setShowScrollIndicator(params: Any): Boolean {
        // Whether to show scrollbars
        showScrollerBar = params.unsafeCast<Int>() == 1
        if (showScrollerBar) {
            // Remove the class that hides scrollbars
            ele.classList.remove(NO_SCROLL_BAR_CLASS)
        } else {
            // Add the class that hides scrollbars
            ele.classList.add(NO_SCROLL_BAR_CLASS)
        }
        return true
    }

    /**
     * Set content inset with animation
     */
    override fun setContentInset(params: String?) {
        // Inset value to set
        val contentInsetString = params ?: return
        // Format inset value
        val contentInset = KRListViewContentInset(contentInsetString)
        // Complete setting asynchronously
        KuiklyRenderCoreContextScheduler.scheduleTask(0) {
            // Use animation to set inset value if needed
            ele.style.transition = if (contentInset.animate) {
                "transform ${BOUND_BACK_DURATION}ms $REFRESH_TIMING_FUNCTION"
            } else {
                ""
            }
            // Set the value to complete
            ele.style.transform = "translate(${contentInset.left}px, ${contentInset.top}px)"
        }
    }

    /**
     * Set inner padding when drag ends, i.e., translateX and Y values
     */
    override fun setContentInsetWhenEndDrag(params: String?) {
        // Inset value to set
        val contentInsetString = params ?: return
        // Format inset value
        val contentInset = KRListViewContentInset(contentInsetString)
        // Transform content to set
        val transform = "translate(${contentInset.left}px, ${contentInset.top}px)"
        if (contentInset.top == 0f) {
            // Restore listView to scrollable
            ele.style.overflowY = "scroll"
            ele.style.overflowX = "hidden"
            // When top > 0, it sets the terminal listView inset height when terminal pull-to-refresh,
            // web doesn't support pull bounce by default,
            // so this value is not processed, only handle the value when preparing for pull-to-refresh
            KuiklyRenderCoreContextScheduler.scheduleTask(BOUND_BACK_DURATION.toInt()) {
                // Clear animation
                ele.style.transition = ""
                // Delay setting inset value until pull-down animation completes
                ele.style.transform = if (contentInset.left == 0f && contentInset.top == 0f) "" else
                    transform
            }
        } else {
            // This indicates it has been pulled down to a position where it can refresh,
            // record the pull-to-refresh position
            canPullRefreshHeight = contentInset.top
        }
    }


    /**
     * Clear existing timers when component is destroyed
     */
    override fun destroy() {
        // Clear existing timer
        if (scrollEndEventTimer > 0) {
            kuiklyWindow.clearTimeout(scrollEndEventTimer)
        }
    }

    companion object {
        const val DRAG_BEGIN_EVENT = "touchstart"
        const val DRAG_END_EVENT = "touchend"
        const val DRAG_MOVE_EVENT = "touchmove"
        const val SCROLL = "scroll"

        // Simulated scroll end timeout duration
        private const val SCROLL_END_OVERTIME = 200
        // Content inset animation duration
        private const val BOUND_BACK_DURATION = 250L
        // Pull-to-refresh bounce timing function
        private const val REFRESH_TIMING_FUNCTION = "ease-in"
        // Delay duration for resuming scroll events, this is a practical
        // duration to allow frequent scroll events to complete before resuming
        private const val RESUME_SCROLL_DELAY = 50
        // Style name for no scrollbar
        private const val NO_SCROLL_BAR_CLASS = "list-no-scrollbar"
        // Vertical scroll direction
        const val SCROLL_DIRECTION_COLUMN = "column"
        // Horizontal scroll direction
        const val SCROLL_DIRECTION_ROW = "row"
        // Scroll direction not set
        const val SCROLL_DIRECTION_NONE = "none"
        // Distance to determine if scrolling has occurred
        const val SCROLL_CAPTURE_THRESHOLD = 2
    }
}

enum class KRNestedScrollMode(val value: String) {
    SELF_ONLY("SELF_ONLY"),
    SELF_FIRST("SELF_FIRST"),
    PARENT_FIRST("PARENT_FIRST"),
}

enum class KRNestedScrollState(val value: String) {
    CAN_SCROLL("CAN_SCROLL"),
    SCROLL_BOUNDARY("SCROLL_BOUNDARY"),
    CANNOT_SCROLL("CANNOT_SCROLL"),
}