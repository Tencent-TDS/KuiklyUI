package com.tencent.kuikly.core.render.web.runtime.web.expand.components.list

import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.expand.components.list.KRListViewContentInset
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreContextScheduler
import com.tencent.kuikly.core.render.web.utils.Log
import org.w3c.dom.AUTO
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.SMOOTH
import org.w3c.dom.ScrollBehavior
import org.w3c.dom.ScrollToOptions
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.get
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
        this.classList.add(IS_LIST)
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
    var pagingEnabled = false
        private set
    // enable bounce effect, support Android Webview 63+ && iOS Safari 16+
    private var bounceEnabled = false
    // enable nest scroll effect
    var nestScrollEnabled = false
        private set
    // Whether in pre-pull-down state
    private var isPrePullDown = false
    // Pull-to-refresh height
    private var canPullRefreshHeight = 0f
    // Whether it contains pull-to-refresh child node
    private var hasRefreshChild = false
    // Scroll distance threshold
    private val scrollThreshold = SCROLL_THRESHOLD
    // Whether in scrolling state
    private var isScrolling = ScrollingAxis.NONE
    // Decide whether the interaction should be treated as a click
    private var clickDetectionTimer: Int? = null
    // Delay invoking the single-click callback so a possible second click can be detected
    private var singleClickConfirmTimer: Int? = null
    // Whether it's a click event
    private var isClickEvent = false
    private var touchStartTime: Double = 0.0
    // Whether the wheel is rolling
    private var isWheelRolling = false
    // Whether the wheel is stopped
    private var wheelStopTimer: Int? = null
    // Count of clicks on the current element, used to determine whether it's a double click
    private var clickCount = 0

    // real html element
    override var ele: HTMLElement = listEle.unsafeCast<HTMLElement>()

    init {
        ele.asDynamic().listView = this
    }

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
    // Click callback
    override var clickEventCallback: KuiklyRenderCallback? = null

    override var doubleClickEventCallback: KuiklyRenderCallback? = null

    var listPagingHelper: H5ListPagingHelper = H5ListPagingHelper(ele, this)
        private set
    var nestScrollHelper: H5NestScrollHelper = H5NestScrollHelper(ele, this)
        internal set
    var pcScrollHelper: H5ListPCScrollHelper = H5ListPCScrollHelper(ele, this, this)
        private set

    /**
     * Set whether listView can scroll
     */
    override fun setScrollEnable(params: Any): Boolean {
        // Set the switch for whether scrolling is enabled
        scrollEnabled = params.unsafeCast<Int>() == ENABLED_FLAG
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
        bounceEnabled = params.unsafeCast<Int>() == ENABLED_FLAG
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
        pagingEnabled = params.unsafeCast<Int>() == ENABLED_FLAG
        return true
    }

    /**
     * Set the scroll direction of listView, 1 for horizontal, 0 for vertical
     */
    override fun setScrollDirection(params: Any): Boolean {
        val direction = if (params.unsafeCast<Int>() == ENABLED_FLAG) SCROLL_DIRECTION_ROW else SCROLL_DIRECTION_COLUMN
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
            return firstChild.style.transform.contains(REFRESH_CHILD_TRANSFORM)
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

    internal fun handleTouchStart(event: Event, isMouseEvent: Boolean = false) {
        Log.trace("scroll direction event begin")
        // Set as dragging
        isDragging = 1
        // Clear pull-to-refresh height
        canPullRefreshHeight = 0f
        // Check if it contains pull-to-refresh child node
        hasRefreshChild = checkHasRefreshChild()
        // Reset scrolling state
        isScrolling = ScrollingAxis.NONE
        if (isMouseEvent) pcScrollHelper.handleMouseDown(event as MouseEvent)
        // Get horizontal and vertical offset of the element during scroll event
        val offsetX = ele.scrollLeft.toFloat()
        val offsetY = ele.scrollTop.toFloat()
        // Record scrollbar position at start of sliding
        startX = offsetX
        startY = offsetY
        // Starting drag position map
        val eventsParams = event.getEventParams()
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

    private fun handleMoveCommon(event: Event) {
        // Need to check if it contains pull-to-refresh component, if not, don't process todo fixme
        val eventsParams = event.getEventParams()
        var deltaY = eventsParams["y"] as Float - touchStartY
        var deltaX = eventsParams["x"] as Float - touchStartX
        var absDeltaY = abs(deltaY)
        var absDeltaX = abs(deltaX)

        // If not yet in scrolling state, determine scroll direction, once determined don't change
        if (isScrolling == ScrollingAxis.NONE) {
            if (absDeltaY > scrollThreshold && absDeltaY > absDeltaX) {
                // Vertical scrolling
                isScrolling = ScrollingAxis.VERTICAL
            } else if (absDeltaX > scrollThreshold && absDeltaX > absDeltaY) {
                // Horizontal scrolling
                isScrolling = ScrollingAxis.HORIZONTAL
            }
        }
        if ((scrollDirection == SCROLL_DIRECTION_COLUMN && isScrolling == ScrollingAxis.VERTICAL) ||
            (scrollDirection == SCROLL_DIRECTION_ROW && isScrolling == ScrollingAxis.HORIZONTAL)) {
            // Scroll direction matches set direction, prevent bubbling to avoid affecting parent node's scroll events
            event.stopPropagation()
        }
        // If current scroll distance is 0, starting to drag down, contains pull-to-refresh child node,
        // and is vertical scrolling, handle pull-to-refresh logic, deltaY > 0 means pulling down
        if (isPrePullDown && deltaY > 0 && hasRefreshChild && isScrolling == ScrollingAxis.VERTICAL) {
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

    private fun handleTouchMove(it: TouchEvent) {
        handleMoveCommon(it)
    }

    internal fun handleTouchEnd() {
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
                    if (scrollDirection == SCROLL_DIRECTION_COLUMN) {
                        ele.style.overflowY = "scroll"
                    } else {
                        ele.style.overflowX = "scroll"
                    }
                }

                // remove transform attribute after transform end
                kuiklyWindow.setTimeout({
                    ele.style.transform = ""
                }, IMMEDIATE_TIMEOUT)
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

    private fun handleTouchScroll() {
        // Get horizontal and vertical offset of the element during scroll event
        val offsetMap = updateOffsetMap(ele.scrollLeft.toFloat(), ele.scrollTop.toFloat(), isDragging)
        // Callback with offset
        scrollEventCallback?.invoke(offsetMap)
    }

    /**
     * 执行 click、doubleClick 回调
     */
    private fun invokeClickCallback(event: Event, isDoubleClick: Boolean) {
        val clickOffsetMap = if (event.isTouchEventOrNull() != null) {
            val touch = event.unsafeCast<TouchEvent>().changedTouches[0] ?: return
            val x = touch.clientX
            val y = touch.clientY
            // Calculate element position
            val position = ele.getBoundingClientRect()
            // Element distance from left side of page
            val eleX = position.left
            // Element distance from top of page
            val eleY = position.top
            // Calculate offset
            val offsetX = x.toDouble() - eleX
            val offsetY = y.toDouble() - eleY
            mapOf("x" to offsetX, "y" to offsetY)
        } else {
            mapOf("x" to event.unsafeCast<MouseEvent>().offsetX, "y" to event.unsafeCast<MouseEvent>().offsetY)
        }

        if (isDoubleClick) {
            doubleClickEventCallback?.invoke(clickOffsetMap)
        } else {
            clickEventCallback?.invoke(clickOffsetMap)
        }
    }

    /**
     * 处理 click、doubleClick 事件
     */
    internal fun handleClickEvent(it: Event) {
        // If it is considered as a click event
        // Record the current click count
        clickCount++
        // Whether the double-click event is registered
        if (!ele.asDynamic().hasDoubleClickListener as Boolean) {
            // If no double-click event is registered，invoke the click callback
            invokeClickCallback(it, false)
            // Reset the click count
            clickCount = 0
            return
        } else {
            // If a double click handler is registered
            if (clickCount == DOUBLE_CLICK_COUNT) {
                // Clear the timer to prevent the click callback from being invoked afterward
                val timer = singleClickConfirmTimer
                if (timer != null) {
                    kuiklyWindow.clearTimeout(timer)
                    singleClickConfirmTimer = null
                }
                // Reset the click count
                clickCount = 0
                // Invoke the double-click callback
                invokeClickCallback(it, true)
            } else {
                // If the timer exists , clear it (reset the timing)
                val prevTimer = singleClickConfirmTimer
                if (prevTimer != null) kuiklyWindow.clearTimeout(prevTimer)
                singleClickConfirmTimer = kuiklyWindow.setTimeout({
                    // If the double click callback is not triggered within timeout, invoke the click callback
                    // When double click callback triggered, the timer will be cleared
                    invokeClickCallback(it, false)
                    // Clear the timer
                    singleClickConfirmTimer = null
                    // Reset the click count
                    clickCount = 0
                }, DOUBLE_CLICK_TIMEOUT)
            }
        }
    }

    // Helper methods for PC scroll helper to access click state
    internal fun isClickEvent(): Boolean = isClickEvent
    internal fun setClickEvent(value: Boolean) { isClickEvent = value }
    internal fun cancelClickDetectionTimer() {
        clickDetectionTimer?.let {
            kuiklyWindow.clearTimeout(it)
            clickDetectionTimer = null
        }
    }

    /**
     * Bind scroll-related events
     */
    override fun setScrollEvent() {
        // If it is a pointing device with limited precision, listen for touch events.
        if (kuiklyWindow.matchMedia("(pointer: coarse)").matches) {
            // Start dragging
            ele.addEventListener(DRAG_BEGIN_EVENT, {
                isClickEvent = true
                // If the mousemove event is not triggered, it will be considered a click event
                clickDetectionTimer = kuiklyWindow.setTimeout({
                    isClickEvent = true
                }, CLICK_DETECTION_TIMEOUT_TOUCH)
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
                clickDetectionTimer?.let {
                    kuiklyWindow.clearTimeout(it)
                    clickDetectionTimer = null
                }
                isClickEvent = false
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
                if (isClickEvent) {
                    handleClickEvent(it)
                    return@addEventListener
                }
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
        }

        // If it is a precise pointing device, listen for mouse events.
        if (kuiklyWindow.matchMedia("(pointer: fine)").matches) {
            ele.addEventListener(MOUSE_DOWN, { event ->
                event as MouseEvent
                // Only left button
                if (event.button != LEFT_MOUSE_BUTTON) return@addEventListener
                pcScrollHelper.isMouseDown = true
                // Reset click flag
                isClickEvent = true
                // If the mousemove event is not triggered, it will be considered a click event
                clickDetectionTimer = kuiklyWindow.setTimeout({
                    isClickEvent = true
                }, CLICK_DETECTION_TIMEOUT_MOUSE)
                // Save the current element
                PCListScrollHandler.mouseDownEleIds.add(ele.id)
                // Filter elements belonging to ListView
                PCListScrollHandler.filterScrollElementIds()
                // Initialize canScroll state
                pcScrollHelper.initCanScroll(showScrollerBar)
                if (pagingEnabled) {
                    listPagingHelper.handlePagerMouseDown(event)
                    return@addEventListener
                }
                if (nestScrollEnabled) {
                    nestScrollHelper.handleNestScrollMouseDown(event)
                    return@addEventListener
                }
                handleTouchStart(event, true)
            }, json("passive" to true))

            // Prevent text selection
            ele.addEventListener(SELECT_START_EVENT, {
                it.preventDefault()
            })
            // Prevent image drag
            ele.addEventListener(DRAG_START_EVENT, {
                it.preventDefault()
            })
        }
        ele.addEventListener(WHEEL_EVENT, { event ->
            // Handle paging mode with wheel event
            if (pagingEnabled) {
                listPagingHelper.handlePagerWheel(event as WheelEvent)
                return@addEventListener
            }

            // Normal scroll mode
            if (!isWheelRolling) {
                isWheelRolling = true
                // 滚动条触发尾部刷新（FooterRefreshView需要拖拽过一次才能进行加载更多）
                handleTouchStart(event)
            }
            // When the wheel is rolled, the previous timer is cleared and a new timer is set.
            wheelStopTimer?.let {
                kuiklyWindow.clearTimeout(it)
            }
            wheelStopTimer = kuiklyWindow.setTimeout({
                // The callback is executed when the timer expires.
                isWheelRolling = false
                handleTouchEnd()
            }, WHEEL_STOP_TIMEOUT)
        })
        // Scroll event
        ele.addEventListener(SCROLL, {
            if (pagingEnabled) {
                // In paging mode, no need to trigger scroll
                // Calculate offset through touchmove and touchend,
                // and callback scroll event to upper layer for processing
                return@addEventListener
            }
            if (nestScrollEnabled) {
                nestScrollHelper.handleNestScrollTouchScroll(it)
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
        val animate = contentOffsetSplits[2] == ANIMATE_FLAG

        if (offsetX.isNaN() || offsetY.isNaN()) {
            // Position parameters abnormal, return
            return
        }
        if (pagingEnabled) {
            listPagingHelper.setContentOffset(offsetX, offsetY, animate)
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
        showScrollerBar = params.unsafeCast<Int>() == ENABLED_FLAG
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
        KuiklyRenderCoreContextScheduler.scheduleTask(IMMEDIATE_TIMEOUT) {
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
            if (scrollDirection == SCROLL_DIRECTION_COLUMN) {
                ele.style.overflowY = "scroll"
                ele.style.overflowX = "hidden"
            } else {
                ele.style.overflowX = "scroll"
                ele.style.overflowY = "hidden"
            }
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
        // DOM Event names
        const val DRAG_BEGIN_EVENT = "touchstart"
        const val DRAG_END_EVENT = "touchend"
        const val DRAG_MOVE_EVENT = "touchmove"
        const val SCROLL = "scroll"
        const val MOUSE_DOWN = "mousedown"
        const val MOUSE_UP = "mouseup"
        const val MOUSE_MOVE = "mousemove"
        private const val SELECT_START_EVENT = "selectstart"
        private const val DRAG_START_EVENT = "dragstart"
        private const val WHEEL_EVENT = "wheel"

        // Timeout durations (in milliseconds)
        private const val SCROLL_END_OVERTIME = 200
        private const val BOUND_BACK_DURATION = 250L
        private const val CLICK_DETECTION_TIMEOUT_TOUCH = 300
        private const val CLICK_DETECTION_TIMEOUT_MOUSE = 200
        private const val DOUBLE_CLICK_TIMEOUT = 200
        private const val WHEEL_STOP_TIMEOUT = 300
        private const val IMMEDIATE_TIMEOUT = 0

        // Pull-to-refresh bounce timing function
        private const val REFRESH_TIMING_FUNCTION = "ease-in"
        // Delay duration for resuming scroll events
        private const val RESUME_SCROLL_DELAY = 50
        // Style name for no scrollbar
        private const val NO_SCROLL_BAR_CLASS = "list-no-scrollbar"

        // Scroll direction constants
        const val SCROLL_DIRECTION_COLUMN = "column"
        const val SCROLL_DIRECTION_ROW = "row"
        const val SCROLL_DIRECTION_NONE = "none"

        // Threshold and count constants
        const val SCROLL_CAPTURE_THRESHOLD = 2
        private const val SCROLL_THRESHOLD = 8
        private const val DOUBLE_CLICK_COUNT = 2

        // CSS class names
        const val IS_LIST = "isList"

        // Boolean flag values from params
        private const val ENABLED_FLAG = 1
        private const val ANIMATE_FLAG = "1"

        // Mouse button constants
        private const val LEFT_MOUSE_BUTTON: Short = 0

        // Pull-to-refresh transform pattern
        private const val REFRESH_CHILD_TRANSFORM = "translate(0%, -100%) rotate(0deg) scale(1, 1) skew(0deg, 0deg)"
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
