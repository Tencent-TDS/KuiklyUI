package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.collection.fastMutableMapOf
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.expand.components.list.KRListViewContentInset
import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.CHANGE
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.DRAG_BEGIN_EVENT
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.DRAG_END_EVENT
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.DRAG_MOVE_EVENT
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.SCROLL
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.TOUCH_BEGIN_EVENT
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.TOUCH_END_EVENT
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage.TOUCH_MOVE_EVENT
import com.tencent.kuikly.core.render.web.runtime.miniapp.page.MiniPageManage
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreContextScheduler
import org.w3c.dom.HTMLElement
import kotlin.math.abs
import kotlin.math.round

/**
 * Mini program list node, depending on the situation, decides to use scroll-view or movable-area implementation
 */
class MiniListElement(
    nodeName: String = TransformConst.LIST,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType), IListElement {
    // real list node
    override var ele: HTMLElement = this.unsafeCast<HTMLElement>()

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

    // When manually set, record the scroll value. When the triggered scroll equals this value,
    // it can be considered as scroll ended
    private var tempScrollLeft: Float? = 0f

    // When manually set, record the scroll value. When the triggered scroll equals this value,
    // it can be considered as scroll ended
    private var tempScrollTop: Float? = 0f

    // Start scroll horizontal offset
    private var startX = 0f

    // Start scroll vertical offset
    private var startY = 0f

    // Vertical position at drag start
    private var touchStartY = 0f

    // Vertical position during drag
    private var touchEndY = 0f

    // Horizontal position during drag
    private var touchEndX = 0f

    // Horizontal position at drag start
    private var touchStartX = 0f

    // Whether to show scrollbar
    private var showScrollerBar = true

    // Scroll direction
    private var scrollDirection = "column"

    // Whether in dragging state
    private var isDragging = 0

    // Whether paging is enabled
    private var pagingEnabled = false

    // Whether in pre-pull-down state
    private var isPrePullDown = false

    // Pull-to-refresh height
    private var canPullRefreshHeight = 0f

    // Whether has refresh child node
    private var hasRefreshChild = false

    // Scroll slide distance threshold
    private val scrollThreshold = 16

    // Whether entered scrolling state
    private var isScrolling = -1

    // Whether scroll event is bound
    private var hasAddScrollListener: Boolean = false

    // Scroll end event timer
    private var scrollEndEventTimer: Int = 0

    // Last scrollTop for determining if scrolling
    private var lastScrollTop = 0f

    // Last scrollLeft for determining if scrolling
    private var lastScrollLeft = 0f

    // Whether in dragging
    private var hasDragMove = false

    // Whether scroll has changed scrollTop and scrollLeft
    private var hasChangeScrollPosition = false

    // Map object for scroll data passed to core
    private val offsetMapData = fastMutableMapOf<String, Any>()

    // Whether to disable movableView movement during pull-to-refresh
    private var isDisablePullDownWithMovableView = false

    // Whether in column direction
    private val isColumnDirection: Boolean
        get() = scrollDirection == "column"

    // Whether movable-view sliding is disabled
    var isDisableMovableView = false

    // Whether scrolling is enabled
    var scrollEnabled = true

    // Use movableArea when paging is enabled and horizontal scrolling
    val isMovableArea: Boolean
        get() = pagingEnabled && !isColumnDirection

    init {
        needCustomWrapper = true
        // Default allow Y-axis sliding
        setScrollYEnable(true)
    }

    /**
     * Scroll element to specified position
     */
    override fun setContentOffset(params: String?) {
        // Do nothing if no parameters
        if (params == null) {
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

        MiniGlobal.setTimeout({
            scrollTo(offsetY, offsetX, animate)
        }, 0)

        if (!hasChangeScrollPosition) {
            scrollLeft = offsetX.toDouble()
            scrollTop = offsetY.toDouble()
        }
    }


    /**
     * Set content inset with animation
     */
    override fun setContentInset(params: String?) {
        // Inset value to be set
        val contentInsetString = params ?: return
        // Format inset value
        val contentInset = KRListViewContentInset(contentInsetString)
        // Get the scroll content view (first child) to apply transform
        // This ensures only the content moves, not the entire scroll-view (which would move the border)
        val contentView = firstElementChild ?: return
        
        // If needed, set inset value with animation
        contentView.style.transition =
            if (contentInset.animate) {
                "transform ${BOUND_BACK_DURATION}ms $REFRESH_TIMING_FUNCTION"
            } else {
                ""
            }
        // Set the value to complete
        contentView.style.transform = "translate(${contentInset.left}px, ${contentInset.top}px)"

        if (contentInset.top == 0f && contentInset.left == 0f) {
            // remove transform value
            MiniGlobal.setTimeout({
                contentView.style.transition = ""
                contentView.style.transform = ""
            }, BOUND_BACK_DURATION.toInt() + 100)
        }
    }


    /**
     * Set padding when drag ends, i.e. translateX and Y values
     */
    override fun setContentInsetWhenEndDrag(params: String?) {
        // Inset value to be set
        val contentInsetString = params ?: return
        // Format inset value
        val contentInset = KRListViewContentInset(contentInsetString)
        if (contentInset.top == 0f) {
            canPullRefreshHeight = 0f
            // When top is greater than 0 during terminal pull-to-refresh, set terminal
            // listView inset height; 0 indicates pull-to-refresh is complete and refresh can end
            hiddenRefresh()
        } else {
            canPullRefreshHeight = contentInset.top
        }
    }

    /**
     * Initialize list events
     */
    override fun setScrollEvent() {
        if (hasAddScrollListener) {
            return
        }
        hasAddScrollListener = true
        // Ensure ScrollView-related events are set after setData, because we need to know
        // whether to use scroll-view or movable-area after setting properties
        KuiklyRenderCoreContextScheduler.scheduleTask {
            initScrollView()
        }
    }

    /**
     * setScrollEvent handles scrollEnd, mini program's scrollEnd also depends
     * on other scroll events, just call the same setScrollEvent here
     */
    override fun setScrollEndEvent() {
        setScrollEvent()
    }

    /**
     * Set whether scrolling is enabled
     */
    override fun setScrollEnable(params: Any): Boolean {
        // Set whether scrolling is enabled
        scrollEnabled = params.unsafeCast<Int>() == 1
        // Set scrolling
        if (isColumnDirection) {
            setScrollYEnable(scrollEnabled)
        } else {
            setScrollXEnable(scrollEnabled)
        }
        // Mark whether scrolling is allowed, child nodes need to follow this restriction
        setAttribute("isScrollDisable", !scrollEnabled)

        return true
    }

    /**
     * Set whether to show scrollbar, note: movable-area does not have scrollbar
     */
    override fun setShowScrollIndicator(params: Any): Boolean {
        // Whether to show scrollbar
        showScrollerBar = params.unsafeCast<Int>() == 1
        setAttribute("showScrollbar", showScrollerBar)
        return true
    }

    /**
     * Set scrolling direction
     */
    override fun setScrollDirection(params: Any): Boolean {
        if (params == 1) {
            setScrollXEnable(true)
        } else {
            setScrollYEnable(true)
        }
        val direction = if (params == 1) "row" else "column"
        scrollDirection = direction
        return true
    }

    /**
     * Set whether to enable paging
     */
    override fun setPagingEnable(params: Any): Boolean {
        // Whether to enable paging
        pagingEnabled = params == 1
        initScrollView(true)
        // Mark isMovableArea, child components need to judge similar decision of parent component
        setAttribute("isMovableArea", isMovableArea)
        return true
    }

    override fun setBounceEnable(params: Any): Boolean {
        return true;
    }

    override fun setNestedScroll(propValue: Any): Boolean {
        return true;
    }

    override fun updateOffsetMap(
        offsetX: Float,
        offsetY: Float,
        isDragging: Int
    ): MutableMap<String, Any> {
        return offsetMapData
    }

    /**
     * When converting to setData data, need to follow actual rendered component name
     */
    override fun onTransformData(): String {
        props["scrollTop"] = scrollTop
        props["scrollLeft"] = scrollLeft

        if (isMovableArea) {
            // movableArea does not support nesting, add customWrapper for him
            needCustomWrapper = true
            return TransformConst.MOVABLE_AREA
        }
        return super.onTransformData()
    }

    override fun destroy() {
        // Clear existing timer
        if (scrollEndEventTimer > 0) {
            MiniGlobal.clearTimeout(scrollEndEventTimer)
        }
    }

    /**
     * Enable Y-axis scrolling
     */
    private fun setScrollYEnable(enabled: Boolean) {
        setAttribute("scrollY", enabled)
        setAttribute("scrollX", !enabled)
    }


    /**
     * Enable X-axis scrolling
     */
    private fun setScrollXEnable(enabled: Boolean) {
        setAttribute("scrollX", enabled)
        setAttribute("scrollY", !enabled)
    }

    /**
     * Handle paging calculation
     */
    private fun handlePager(deltaX: Float, deltaY: Float) {
        if (
            !pagingEnabled ||
            MiniPageManage.currentPage?.isPaging == true ||
            MiniPageManage.currentPage?.isScrolling == true
        ) {
            // Only calculate paging for paging components
            return
        }
        MiniPageManage.currentPage?.isPaging = true
        // Page difference
        val offset = 100f
        // Page index of offset location
        var pageIndex: Float
        // Scroll offset
        var scrollOffsetX = 0f
        var scrollOffsetY = 0f

        if (isColumnDirection) {
            // Handle vertical scrolling
            pageIndex = round(startY / offsetHeight)
            if (abs(deltaY) > offset) {
                // Page cut
                if (deltaY < 0) {
                    // Left page cut
                    pageIndex--
                } else {
                    // Right page cut
                    pageIndex++
                }
            }
            // Calculate page offset
            scrollOffsetY = offsetHeight * pageIndex
        } else {
            // Handle horizontal scrolling
            pageIndex = round(startX / offsetWidth)
            if (abs(deltaX) > offset) {
                // Page cut
                if (deltaX < 0) {
                    // Left page cut
                    pageIndex--
                } else {
                    // Right page cut
                    pageIndex++
                }
            }
            // Calculate page offset
            scrollOffsetX = offsetWidth * pageIndex
        }
        // Scroll to specified distance
        if (scrollOffsetY > 0) {
            scrollTo(
                left = scrollOffsetX,
                top = scrollOffsetY,
                animate = true
            )
        } else {
            scrollTo(
                left = scrollOffsetX,
                top = null,
                animate = true
            )
        }

        // Delay 500 milliseconds to unlock handlePager logic
        KuiklyRenderCoreContextScheduler.scheduleTask(500) {
            MiniPageManage.currentPage?.isPaging = false
        }
    }

    /**
     * Handle scroll animation, different settings for two views
     */
    private fun handleAnimate(animate: Boolean?) {
        if (animate != null) {
            if (isMovableArea) {
                if (firstElementChild?.getAttribute("animation") != animate) {
                    firstElementChild?.setAttribute("animation", animate)
                }
            } else {
                if (getAttribute("scroll-with-animation") != animate) {
                    setAttribute("scroll-with-animation", animate)
                }
            }
        }
    }

    /**
     * Scroll to specified position
     */
    private fun scrollTo(top: Float?, left: Float?, animate: Boolean?) {
        handleAnimate(animate)
        if (top != null) {
            tempScrollTop = top
            firstElementChild.unsafeCast<MiniElement?>()?.setAttributeForce("y", -top)
            if (left == null) {
                tempScrollLeft = null
            }
            setAttributeForce("scrollTop", top)
        }
        if (left != null) {
            tempScrollLeft = left
            firstElementChild.unsafeCast<MiniElement?>()?.setAttributeForce("x", -left)
            if (top == null) {
                tempScrollTop = null
            }
            setAttributeForce("scrollLeft", left)
        }
    }

    /**
     * Initialize node, different initial values need to be set based on whether movableArea
     */
    private fun initScrollView(needClear: Boolean? = false) {
        if (isMovableArea) {
            if (needClear == true) {
                clearScrollViewScrollEvent()
            }
            setMovableViewScrollEvent()
            style.overflowX = "hidden"
            style.overflowY = "hidden"
            // Add movable view to Page
            MiniPageManage.currentPage?.addMovableViewToList(this)
        } else {
            if (needClear == true) {
                clearMovableViewScrollEvent()
            }
            setScrollViewScrollEvent()
            // Enable scroll-view enhancement mode
            setAttribute("enhanced", true)
            // Disable bounces animation
            setAttribute("bounces", false)
            // Default set scrollbar with animation
            setAttribute("scroll-with-animation", true)
            setAttribute("scroll-anchoring", true)
            setAttribute("enable-passive", false)
            setAttribute("cacheExtent", 300)
        }
    }

    private fun setScrollViewScrollEvent() {
        /**
         * Drag start, record scrollLeft etc. values, reset dragging flag and inertial speed
         */
        addEventListener(DRAG_BEGIN_EVENT, { event: dynamic ->
            updateScrollDetailByEvent(event.detail)
        })

        /**
         * Confirm whether scrollLeft scrollTop changes based on scroll direction and scrollLeft scrollTop
         */
        addEventListener(DRAG_MOVE_EVENT, { event: dynamic ->
            if (!isColumnDirection && event.detail.scrollLeft != scrollLeft) {
                hasDragMove = true
            }
            if (isColumnDirection && event.detail.scrollTop != scrollTop) {
                hasDragMove = true
            }
        })

        /**
         * Drag end, need to modify dragging flag, at the same time modify inertial speed and scroll position
         */
        addEventListener(DRAG_END_EVENT, { event: dynamic ->
            updateScrollDetailByEvent(event.detail)
        })

        addEventListener(TOUCH_BEGIN_EVENT, { event ->
            hasChangeScrollPosition = true
            val eventsParams = getTouchParams(event)
            isDragging = 1
            // Pull-to-refresh height clear
            canPullRefreshHeight = 0f
            // Check whether there is a pull-to-refresh child node
            hasRefreshChild = checkHasRefreshChild()
            // Whether scrolling can be restored
            isScrolling = -1
            // Record scrollbar position at start of sliding
            startX = scrollLeft.toFloat()
            startY = scrollTop.toFloat()
            // Record starting drag vertical axis position
            touchStartY = eventsParams["y"].unsafeCast<Float>()
            // Record starting drag horizontal axis position
            touchStartX = eventsParams["x"].unsafeCast<Float>()
            // If current scroll distance is 0 and not PageList paging component,
            // enter waiting state for pull-to-refresh
            isPrePullDown = startY == 0f && !pagingEnabled
            fireBeginDragEvent()
        })

        addEventListener(TOUCH_MOVE_EVENT, { event ->
            val eventsParams = getTouchParams(event)
            val deltaY = eventsParams["y"].unsafeCast<Float>() - touchStartY
            val deltaX = abs(eventsParams["x"].unsafeCast<Float>() - touchStartX)

            // If current not in scrolling state, judge scroll direction,
            // scroll direction determined after not change
            if (isScrolling == -1) {
                if (deltaY > scrollThreshold) {
                    // Vertical scrolling
                    isScrolling = 1
                } else if (deltaX > scrollThreshold) {
                    // Horizontal scrolling
                    isScrolling = 0
                }
            }

            // If current scroll distance is 0 and start dragging downward, and there is a
            // pull-to-refresh child node, and it is vertical scrolling, then handle pull-to-refresh
            // logic, deltaY > 0 indicates downward
            if (isPrePullDown && deltaY > 0 && hasRefreshChild && isScrolling == 1) {
                // If not disabled move, then disable
                if (!isDisablePullDownWithMovableView) {
                    isDisablePullDownWithMovableView = true
                    // Disable moveable-view
                    MiniPageManage.currentPage?.disableMovableView()
                }
                // Set ending position before drag ends
                touchEndY = eventsParams["y"].unsafeCast<Float>()
                // Set element translate
                firstElementChild?.style?.transform = "translate(0, ${deltaY}px)"
                // offsetY needs additional setting, possibly negative
                val offsetMap = fastMutableMapOf<String, Any>().apply {
                    set("offsetY", -deltaY)
                }
                fireScrollEvent(offsetMap)
            }
        })

        addEventListener(TOUCH_END_EVENT, {
            if (hasDragMove || hasRefreshChild) {
                // If dragging or touch end but there is a pull-to-refresh node, need to handle drag end event
                var offsetY = scrollTop.toFloat()
                isDragging = 0
                if (isPrePullDown) {
                    // If disabled move, then restore
                    if (isDisablePullDownWithMovableView) {
                        isDisablePullDownWithMovableView = false
                        // Enable moveable-view
                        MiniPageManage.currentPage?.enableMovableView()
                    }
                    val deltaY = touchEndY - touchStartY
                    if (deltaY > 0) {
                        offsetY = -deltaY
                    }
                    if (canPullRefreshHeight == 0f) {
                        // Hand off when not reaching pull-to-refresh height, hide pull-to-refresh component
                        hiddenRefresh()
                    } else if (deltaY > canPullRefreshHeight) {
                        // If pull-to-refresh hand off when already exceeds pull-to-refresh height,
                        // need to rebound to can pull-to-refresh height before refresh
                        firstElementChild?.style?.transition =
                            "transform ${BOUND_BACK_DURATION}ms $REFRESH_TIMING_FUNCTION"
                        // Set element translate
                        firstElementChild?.style?.transform =
                            "translate(0, ${canPullRefreshHeight}px)"
                    }
                }
                val offsetMap = fastMutableMapOf<String, Any>().apply {
                    set("offsetY", offsetY)
                }
                fireWillDragEndEventCallback(offsetMap)
                fireScrollEvent(offsetMap)
                fireEndDragEvent(offsetMap)
            }
            hasDragMove = false
        })

        addEventListener(SCROLL, { event: dynamic ->
            // If not dragging, if scroll still triggers scroll, it means it's inertial scrolling,
            // need to update scroll related variable data at this time
            updateScrollDetailByEvent(event.detail)
            val dist = if (isColumnDirection) {
                scrollTop - lastScrollTop
            } else {
                scrollLeft - lastScrollLeft
            }

            if (abs(dist) > scrollThreshold) {
                MiniPageManage.currentPage?.isScrolling = true
            } else {
                MiniPageManage.currentPage?.isScrolling = false
            }

            lastScrollTop = event.detail.scrollTop.unsafeCast<Float>()
            lastScrollLeft = event.detail.scrollLeft.unsafeCast<Float>()
            if (scrollEndEventTimer > 0) {
                MiniGlobal.clearTimeout(scrollEndEventTimer)
            }
            scrollEndEventTimer = MiniGlobal.setTimeout({
                MiniPageManage.currentPage?.isScrolling = false
                fireEndScrollEvent()
            }, SCROLL_END_OVERTIME)
            fireScrollEvent()
        })
    }

    /**
     * Clear scrollView scroll event listener
     */
    private fun clearScrollViewScrollEvent() {
        removeEventListener(TOUCH_BEGIN_EVENT, null)
        removeEventListener(TOUCH_MOVE_EVENT, null)
        removeEventListener(TOUCH_END_EVENT, null)
        removeEventListener(DRAG_BEGIN_EVENT, null)
        removeEventListener(DRAG_END_EVENT, null)
        removeEventListener(DRAG_MOVE_EVENT, null)
        removeEventListener(SCROLL, null)
    }

    /**
     * Clear movableView scroll event listener
     */
    private fun clearMovableViewScrollEvent() {
        firstElementChild?.removeEventListener(TOUCH_BEGIN_EVENT, null)
        firstElementChild?.removeEventListener(TOUCH_END_EVENT, null)
        firstElementChild?.removeEventListener(CHANGE, null)
    }

    /**
     * Check whether there is a pull-to-refresh child node
     */
    private fun checkHasRefreshChild(): Boolean {
        // Check first child node of the first element, whether it is a pull-to-refresh node,
        // here because listView implementation is to wrap a ScrollContentView, then
        // Wrap the specific scrollable content, so take child node to ScrollContentView's child node
        val firstChild = firstElementChild?.firstElementChild

        if (firstChild != null) {
            // Check if the first child node is a pull-to-refresh node
            // Method 1: Check by transform style (DSL RefreshView uses translate(0%, -100%))
            val transform = firstChild.style.transform
            if (transform == "translate(0%, -100%) rotate(0deg) scale(1, 1) skew(0deg, 0deg)") {
                return true
            }
            // Method 2: Check if the element is positioned above the viewport (Compose pullToRefreshItem)
            // The element has negative top position or offset to position it above visible area
            val top = firstChild.style.top
            if (top.isNotEmpty() && top != "0px" && top.startsWith("-")) {
                return true
            }
            // Method 3: Check if marginTop or transform translateY is negative (indicating positioned above)
            val marginTop = firstChild.style.marginTop
            if (marginTop.isNotEmpty() && marginTop.startsWith("-")) {
                return true
            }
        }
        return false
    }

    /**
     * Handle movableView scroll event
     */
    private fun setMovableViewScrollEvent() {
        firstElementChild?.addEventListener(TOUCH_BEGIN_EVENT, { event ->
            hasChangeScrollPosition = true
            val eventsParams = getTouchParams(event)
            // Set as dragging
            isDragging = 1
            // Record scrollbar position at start of sliding
            startX = scrollLeft.toFloat()
            startY = scrollTop.toFloat()
            // Record starting drag vertical axis position
            touchStartY = eventsParams["y"].unsafeCast<Float>()
            // Record starting drag horizontal axis position
            touchStartX = eventsParams["x"].unsafeCast<Float>()
            // Check if has refresh child (for Compose pullToRefreshItem)
            hasRefreshChild = checkHasRefreshChild()
            // If current scroll distance is 0 and not PageList paging component,
            // enter waiting state for pull-to-refresh
            isPrePullDown = scrollTop.toFloat() == 0f && !pagingEnabled && hasRefreshChild
            // Event callback
            fireBeginDragEvent()
        })

        firstElementChild?.addEventListener(TOUCH_END_EVENT, { event ->
            val eventsParams = getTouchParams(event)
            // Set ending position before drag ends
            touchEndY = eventsParams["y"].unsafeCast<Float>()
            touchEndX = eventsParams["x"].unsafeCast<Float>()
            isDragging = 0
            // Judge whether to handle paging
            if (pagingEnabled && scrollEnabled) {
                val deltaX = touchEndX - touchStartX
                val deltaY = touchEndY - touchStartY
                handlePager(-deltaX, -deltaY)
            }
            // Event callback
            fireScrollEvent()
            fireWillDragEndEventCallback()
            fireEndDragEvent()
        })

        firstElementChild?.addEventListener(CHANGE, { event: dynamic ->
            val detailX: Float = -event.detail.x.unsafeCast<Float>()
            val detailY: Float = -event.detail.y.unsafeCast<Float>()

            scrollLeft = detailX.toDouble()
            scrollTop = detailY.toDouble()

            // Horizontal scrolling, judge X
            if (!isColumnDirection && tempScrollLeft != null) {
                // When scroll distance and setting are consistent, scroll ends, triggering scroll end callback
                if (detailX == tempScrollLeft) {
                    fireEndScrollEvent()
                }
            }

            // Vertical scrolling, judge Y
            if (isColumnDirection && tempScrollTop != null) {
                // When scroll distance and setting are consistent, scroll ends, triggering scroll end callback
                if (detailY == tempScrollTop) {
                    fireEndScrollEvent()
                }
            }

            fireScrollEvent()
        })
    }

    /**
     * Get scroll related parameters
     */
    private fun getCommonScrollParams(): MutableMap<String, Any> {
        // List current vertical axis offset
        offsetMapData["offsetX"] = scrollLeft
        // List current horizontal axis offset
        offsetMapData["offsetY"] = scrollTop
        // List current total width
        offsetMapData["viewWidth"] = offsetWidth
        // List current total height
        offsetMapData["viewHeight"] = offsetHeight
        // ListView width
        offsetMapData["contentWidth"] = firstElementChild.unsafeCast<MiniElement>().offsetWidth
        // ListView height
        offsetMapData["contentHeight"] = firstElementChild.unsafeCast<MiniElement>().offsetHeight
        // Whether dragging
        offsetMapData["isDragging"] = isDragging
        return offsetMapData
    }


    private fun fireBeginDragEvent(params: MutableMap<String, Any>? = null) {
        val map = getCommonScrollParams()
        if (params != null) {
            map.putAll(params)
        }
        dragBeginEventCallback?.invoke(map)
    }

    private fun fireEndDragEvent(params: MutableMap<String, Any>? = null) {
        val map = getCommonScrollParams()
        if (params != null) {
            map.putAll(params)
        }
        dragEndEventCallback?.invoke(map)
    }

    private fun fireWillDragEndEventCallback(params: MutableMap<String, Any>? = null) {
        val map = getCommonScrollParams()
        if (params != null) {
            map.putAll(params)
        }

        willDragEndEventCallback?.invoke(map)
    }

    private fun fireScrollEvent(params: MutableMap<String, Any>? = null) {
        val map = getCommonScrollParams()
        if (params != null) {
            map.putAll(params)
        }
        scrollEventCallback?.invoke(map)
    }

    private fun fireEndScrollEvent() {
        val map = getCommonScrollParams()
        scrollEndEventCallback?.invoke(map)
    }

    private fun updateScrollDetailByEvent(detail: dynamic) {
        scrollLeft = detail.scrollLeft.unsafeCast<Double>()
        scrollTop = detail.scrollTop.unsafeCast<Double>()
    }

    private fun hiddenRefresh() {
        if (firstElementChild?.style?.transform != "translate(0,0)") {
            firstElementChild?.style?.transition =
                "transform ${BOUND_BACK_DURATION}ms ease-out"
            firstElementChild?.style?.transform = "translate(0,0)"

            // remove transform value
            MiniGlobal.setTimeout({
                firstElementChild?.style?.transition = ""
                firstElementChild?.style?.transform = ""
            }, BOUND_BACK_DURATION.toInt() + 100)
        }
    }

    companion object {
        // Simulated scroll end timeout duration
        private const val SCROLL_END_OVERTIME = 100

        // Set content padding animation duration
        const val BOUND_BACK_DURATION = 250L

        // Pull-to-refresh rebound time function
        const val REFRESH_TIMING_FUNCTION = "ease-in"
    }
}
