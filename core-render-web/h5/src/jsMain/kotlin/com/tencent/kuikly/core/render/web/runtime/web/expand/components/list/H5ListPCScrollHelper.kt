package com.tencent.kuikly.core.render.web.runtime.web.expand.components.list

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.clear
import com.tencent.kuikly.core.render.web.collection.array.get
import com.tencent.kuikly.core.render.web.collection.array.isEmpty
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.const.KREventConst
import com.tencent.kuikly.core.render.web.const.KRListConst
import com.tencent.kuikly.core.render.web.expand.components.toPanEventParams
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.js.json
import kotlin.math.abs

/**
 * Checks whether the current runtime environment defines TouchEvent;
 * If the event is a TouchEvent, returns it, otherwise returns null.
 */
fun Event.isTouchEventOrNull(): TouchEvent? {
    return if ((js("typeof TouchEvent !== 'undefined'") as Boolean) && this is TouchEvent) {
        this
    } else null
}

/**
 * Convert Mouse/Touch parameters to specified format
 */
fun Event.getEventParams(): Map<String, Any> {
    val touch = this.isTouchEventOrNull()
    return when {
        touch != null -> touch.toPanEventParams()
        this is MouseEvent -> this.toPanEventParams()
        else -> emptyMap()
    }
}

/**
 * Content scroll direction caused by a PC mouse drag
 * This direction is opposite to the mouse drag direction
 */
enum class ScrollDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    NONE
}

/**
 * Scrolling state
 */
enum class ScrollingAxis { NONE, HORIZONTAL, VERTICAL }

/**
 * Centrally manage mouse move/release events on the window
 * Distribute these events to elements marked as ListView on the page
 */
object PCListScrollHandler {
    // Save all elements that trigger mouseDown event
    var mouseDownEleIds: JsArray<String> = JsArray()
    // Save the elements of ListView
    private var scrollEleIds: JsArray<String> = JsArray()
    // Record the actual distance moved by the last child node
    private var childrenEleScrollDistance: JsMap<String, JsMap<String, Float>> = JsMap()
    // Record the scroll direction in this event, opposite to the direction of mouse move
    var scrollDirection = ScrollDirection.NONE
    // Threshold to determine scroll direction (in pixels)
    private const val SCROLL_DIRECTION_THRESHOLD = 2
    // Whether in scrolling state, used to lock the horizontal/vertical direction of a single scroll
    private var isScrolling = ScrollingAxis.NONE

    private var lastX: Float = 0f
    private var lastY: Float = 0f

    // Map keys for scroll distance
    internal const val SCROLL_X_KEY = "scrollX"
    internal const val SCROLL_Y_KEY = "scrollY"
    // Event param keys
    internal const val EVENT_X_KEY = "x"
    internal const val EVENT_Y_KEY = "y"
    // Default values
    internal const val DEFAULT_POSITION = 0f
    // Media query for precise pointing device
    internal const val POINTER_FINE_QUERY = "(pointer: fine)"
    // CSS pixel unit suffix
    internal const val PX_SUFFIX = "px"

    init {
        kuiklyWindow.addEventListener(KREventConst.MOUSE_DOWN, {
            lastX = it.getEventParams()[EVENT_X_KEY].unsafeCast<Float>()
            lastY = it.getEventParams()[EVENT_Y_KEY].unsafeCast<Float>()
            isScrolling = ScrollingAxis.NONE
        })

        kuiklyWindow.addEventListener(KREventConst.MOUSE_MOVE, {
            if (scrollEleIds.isEmpty()) return@addEventListener
            detectScrollDirection(it as MouseEvent)
            // Trigger the mouseMove event handler of listViews
            handleListViewsScroll(it)
        })

        kuiklyWindow.addEventListener(KREventConst.MOUSE_UP, {
            // Prevent only clicking occurs, causing isMouseDown to not be reset to false
            filterScrollElementIds()
            if (scrollEleIds.isEmpty()) return@addEventListener
            // Trigger the mouseup event handler of ListViews
            scrollEleIds.forEach { id ->
                val ele = kuiklyDocument.getElementById(id)
                val listView: H5ListView = ele.asDynamic().listView.unsafeCast<H5ListView>()
                listView.pcScrollHelper.onMouseUpEvent(it as MouseEvent)
            }
            // reset
            mouseDownEleIds.clear()
            scrollEleIds.clear()
        }, json("passive" to true))
    }

    /**
     * Filter elements belonging to ListView
     */
    fun filterScrollElementIds() {
        val validElementIds = mouseDownEleIds.filter { id ->
            val ele = kuiklyDocument.getElementById(id)
            ele?.classList?.contains(KRListConst.IS_LIST) == true
        }
        scrollEleIds = validElementIds
    }

    /**
     * Get the scroll distance of the child element
     */
    private fun getChildScrollDistance(index: Int): JsMap<String, Float> {
        if (index <= 0) {
            return JsMap()
        }
        val childEleId = scrollEleIds[index - 1]
        return childrenEleScrollDistance.get(childEleId) ?: JsMap()
    }

    private fun getListView(index: Int): H5ListView? {
        val id = scrollEleIds[index]
        val ele = kuiklyDocument.getElementById(id)
        return ele?.asDynamic()?.listView?.unsafeCast<H5ListView>()
    }

    /**
     * Detects the primary scroll direction
     */
    private fun detectScrollDirection(event: MouseEvent) {
        // Record current scroll position
        val eventsParams = event.getEventParams()
        // Determine the scroll direction
        val currentX = eventsParams[EVENT_X_KEY].unsafeCast<Float>()
        val currentY = eventsParams[EVENT_Y_KEY].unsafeCast<Float>()

        val deltaX = abs(currentX - lastX)
        val deltaY = abs(currentY - lastY)

        // If not yet in scrolling state, determine scroll direction, once determined don't change
        if (isScrolling == ScrollingAxis.NONE) {
            if (deltaX > deltaY && deltaX > SCROLL_DIRECTION_THRESHOLD) {
                // Horizontal scrolling
                isScrolling = ScrollingAxis.HORIZONTAL
            } else if (deltaX < deltaY && deltaY > SCROLL_DIRECTION_THRESHOLD) {
                // Vertical scrolling
                isScrolling = ScrollingAxis.VERTICAL
            }
        }
        // If already in scrolling state, determine scroll direction
        when (isScrolling) {
            ScrollingAxis.HORIZONTAL -> {
                scrollDirection = if (currentX - lastX > 0) ScrollDirection.LEFT else ScrollDirection.RIGHT
            }
            ScrollingAxis.VERTICAL -> {
                scrollDirection = if (currentY - lastY > 0) ScrollDirection.UP else ScrollDirection.DOWN
            }
            ScrollingAxis.NONE -> {
                // axis not decided yet -> do nothing
            }
        }
        lastX = currentX
        lastY = currentY
    }

    /**
     * Checks whether the given listView can scroll in the current gesture direction.
     */
    private fun canScrollInDirection(listView: H5ListView): Boolean {
        return when (scrollDirection) {
            ScrollDirection.LEFT  -> listView.pcScrollHelper.canScrollLeft
            ScrollDirection.RIGHT -> listView.pcScrollHelper.canScrollRight
            ScrollDirection.UP    -> listView.pcScrollHelper.canScrollUp
            ScrollDirection.DOWN  -> listView.pcScrollHelper.canScrollDown
            else                  -> false
        }
    }

    private fun handleListViewsScroll(event: MouseEvent) {
        for (i in 0 until scrollEleIds.length) {
            //  Get the listView
            val listView = getListView(i) ?: continue
            val pagingEnabled = listView.pagingEnabled
            val nestedEnabled = listView.nestScrollEnabled

            // Processing based on scrolling category
            // Basic scroll
            when {
                // Basic scroll: neither paging nor nested
                !pagingEnabled && !nestedEnabled -> {
                    listView.pcScrollHelper.onMouseMoveEvent(event, getChildScrollDistance(i))

                    val canScroll = canScrollInDirection(listView)

                    if (!canScroll) {
                        val id = scrollEleIds[i]
                        childrenEleScrollDistance[id] = listView.pcScrollHelper.getScrollDistance()
                        // 获取当前托拽滚动元素的父元素
                        val parentListView = getListView(i + 1) ?: break
                        // 若父元素为托拽滚动元素, 但无法滚动，则跳出循环(模拟阻止冒泡)，防止影响祖先分页滚动元素
                        // 若父元素为分页滚动元素，由于当前元素在该方向上无法滚动，继续循环，让分页元素处理(分页)
                        if (!parentListView.pagingEnabled) {
                            val parentCanScroll = canScrollInDirection(parentListView)
                            if (!parentCanScroll) {
                                break
                            }
                        }
                    } else {
                        break
                    }
                }
                // Paging enabled
                pagingEnabled -> {
                    val listPagingHelper = listView.listPagingHelper
                    // 对 paging 的滚动用空 JsMap
                    listView.pcScrollHelper.onMouseMoveEvent(event, JsMap())

                    if (listPagingHelper.canScroll) {
                        // 获取当前分页滚动元素的父元素
                        val parentListView = getListView(i + 1) ?: break
                        // 若父元素也是分页滚动元素（分页嵌套分页），则不处理父元素（模拟阻止冒泡）
                        if (parentListView.pagingEnabled) {
                            break
                        } else { // 若父元素不是分页滚动元素，根据滚动方向修改父元素该方向上的可滚动状态
                            when (listPagingHelper.scrollDirection) {
                                KRListConst.SCROLL_DIRECTION_COLUMN -> {
                                    parentListView.pcScrollHelper.canScrollUp = false
                                    parentListView.pcScrollHelper.canScrollDown = false
                                }
                                KRListConst.SCROLL_DIRECTION_ROW -> {
                                    parentListView.pcScrollHelper.canScrollLeft = false
                                    parentListView.pcScrollHelper.canScrollRight = false
                                }
                            }
                        }
                    }
                }
                // Nested scroll enabled
                nestedEnabled -> {
                    listView.pcScrollHelper.onMouseMoveEvent(event, JsMap())
                }
            }
        }
    }
}

/**
 * Helper class for PC mouse scroll handling in H5ListView
 */
class H5ListPCScrollHelper(
    private val ele: HTMLElement,
    private val listElement: IListElement,
    private val listView: H5ListView
) {
    companion object {
        private const val DEFAULT_PARSE_VALUE = 0.0
    }

    // Whether the mouse is pressed
    var isMouseDown = false
    // Last mouse event, used to measure the distance when mouse moves
    private var lastMouseEvent: MouseEvent? = null
    // Whether it's the first mouse move event triggered in this direction
    private var isFirstMoveLeft = true
    private var isFirstMoveRight = true
    private var isFirstMoveUp = true
    private var isFirstMoveDown = true
    // Record total scroll distance when mouse moves
    private var scrollDistanceX: Float = 0f
    private var scrollDistanceY: Float = 0f
    // Current element scroll height
    private var scrollHeight: Int = 0
    // Whether it can scroll
    var canScrollLeft: Boolean = false
    var canScrollRight: Boolean = false
    var canScrollUp: Boolean = false
    var canScrollDown: Boolean = false
    // Starting horizontal scroll offset
    private var startX = 0f
    // Starting vertical scroll offset
    private var startY = 0f

    init {
        // Create PCListScrollHandler for PC
        if (kuiklyWindow.matchMedia(PCListScrollHandler.POINTER_FINE_QUERY).matches) {
            PCListScrollHandler
        }
    }

    /**
     * Return the scroll distance as a map
     */
    fun getScrollDistance(): JsMap<String, Float> {
        val distancesMap = JsMap<String, Float>()
        distancesMap.set(PCListScrollHandler.SCROLL_X_KEY, scrollDistanceX)
        distancesMap.set(PCListScrollHandler.SCROLL_Y_KEY, scrollDistanceY)
        return distancesMap
    }

    /**
     * Handle mouse down event - init start positions
     */
    fun handleMouseDown(event: MouseEvent) {
        isMouseDown = true
        lastMouseEvent = event
        // Get horizontal and vertical offset of the element during scroll event
        val offsetX = ele.scrollLeft.toFloat()
        val offsetY = ele.scrollTop.toFloat()
        // Record scrollbar position at start of sliding
        startX = offsetX
        startY = offsetY
    }

    /**
     * Initialize canScroll state
     */
    fun initCanScroll(showScrollerBar: Boolean) {
        val style = kuiklyWindow.getComputedStyle(ele)
        fun parsePx(s: String?): Double {
            if (s == null) return DEFAULT_PARSE_VALUE
            val str = s.trim()
            if (str.endsWith(PCListScrollHandler.PX_SUFFIX)) {
                return str.removeSuffix(PCListScrollHandler.PX_SUFFIX).toDoubleOrNull() ?: DEFAULT_PARSE_VALUE
            }
            return str.toDoubleOrNull() ?: DEFAULT_PARSE_VALUE
        }
        val borderLeft = parsePx(style.borderLeftWidth)
        val borderRight = parsePx(style.borderRightWidth)
        val borderTop = parsePx(style.borderTopWidth)
        val borderBottom = parsePx(style.borderBottomWidth)

        // 垂直滚动条宽度
        val scrollbarWidthY = (ele.offsetWidth - ele.clientWidth) - (borderLeft + borderRight)
        // 水平滚动条宽度
        val scrollbarWidthX = (ele.offsetHeight - ele.clientHeight) - (borderTop + borderBottom)

        // 水平方向上是否可以滚动
        val scrollableX = if (showScrollerBar) {
            ele.clientWidth + scrollbarWidthY < ele.scrollWidth
        } else {
            ele.scrollWidth > ele.clientWidth
        }

        // 垂直方向上是否可以滚动
        val scrollableY = if (showScrollerBar) {
            ele.clientHeight + scrollbarWidthX < ele.scrollHeight
        } else {
            ele.scrollHeight > ele.clientHeight
        }

        // initialize canScroll
        canScrollLeft = scrollableX
        canScrollRight = scrollableX
        canScrollUp = scrollableY
        canScrollDown = scrollableY

        // initialize scroll height
        scrollHeight = ele.scrollHeight
    }

    private fun handleMouseMove(it: MouseEvent, prevScroll: JsMap<String, Float>) {
        if (lastMouseEvent == null) {
            return
        }
        // Trigger the scroll event by modifying scrollLeft and scrollTop
        val eventsParams = it.getEventParams()
        val lastEventsParams = lastMouseEvent!!.getEventParams()
        lastMouseEvent = it
        // 如果当前在该方向上无法滚动，记录鼠标位置后不进行后续处理
        when (PCListScrollHandler.scrollDirection) {
            ScrollDirection.UP -> { if (!canScrollUp) return }
            ScrollDirection.DOWN -> {
                // 针对列表尾部刷新时，元素的 scrollHeight 会变大的情况，对该方向滚动标志位进行修改
                if (ele.scrollHeight > scrollHeight) {
                    canScrollDown = true
                }
                if (!canScrollDown) return
            }
            ScrollDirection.LEFT -> { if (!canScrollLeft) return }
            ScrollDirection.RIGHT -> { if (!canScrollRight) return }
            else -> { return }
        }
        // Record current scroll height
        scrollHeight = ele.scrollHeight
        // calculate the delta between the last event and the current event
        val deltaY = eventsParams[PCListScrollHandler.EVENT_Y_KEY] as Float - lastEventsParams[PCListScrollHandler.EVENT_Y_KEY].unsafeCast<Float>()
        val deltaX = eventsParams[PCListScrollHandler.EVENT_X_KEY] as Float - lastEventsParams[PCListScrollHandler.EVENT_X_KEY].unsafeCast<Float>()
        // real delta
        var delta = 0f

        val maxScrollTop = (ele.scrollHeight - ele.clientHeight).toFloat()
        val maxScrollLeft = (ele.scrollWidth - ele.clientWidth).toFloat()

        when (PCListScrollHandler.scrollDirection) {
            ScrollDirection.UP, ScrollDirection.DOWN -> {
                // 计算当前滚动的距离
                delta = calculateVerticalDelta(deltaY, prevScroll)
                // 计算新的 scrollTop
                val newScrollTop = startY - delta
                // 判断是否超出上下边界, 并更新滚动距离
                applyVerticalScroll(delta, newScrollTop, maxScrollTop)
            }
            ScrollDirection.LEFT, ScrollDirection.RIGHT -> {
                delta = calculateHorizontalDelta(deltaX, prevScroll)
                val newScrollLeft = startX - delta
                applyHorizontalScroll(delta, newScrollLeft, maxScrollLeft)
            }
            else -> { /* 不处理其他方向 */ }
        }
    }

    private fun calculateVerticalDelta(deltaY: Float, prevScroll: JsMap<String, Float>): Float {
        // 获取子元素已经滚动的距离
        val childEleScrollY = prevScroll.get(PCListScrollHandler.SCROLL_Y_KEY) ?: PCListScrollHandler.DEFAULT_POSITION
        return if (PCListScrollHandler.scrollDirection == ScrollDirection.UP) {
            // 每个方向上的首次滚动，需要校正距离；同时修改当前方向上 firstMove 标志
            if (isFirstMoveUp) {
                isFirstMoveUp = false
                isFirstMoveDown = true
                scrollDistanceY = 0f
                deltaY - childEleScrollY
            } else {
                deltaY
            }
        } else { // DOWN
            if (isFirstMoveDown) {
                isFirstMoveDown = false
                isFirstMoveUp = true
                scrollDistanceY = 0f
                deltaY - childEleScrollY
            } else {
                deltaY
            }
        }
    }

    private fun calculateHorizontalDelta(deltaX: Float, prevScroll: JsMap<String, Float>): Float {
        val childEleScrollX = prevScroll.get(PCListScrollHandler.SCROLL_X_KEY) ?: PCListScrollHandler.DEFAULT_POSITION
        return if (PCListScrollHandler.scrollDirection == ScrollDirection.LEFT) {
            if (isFirstMoveLeft) {
                isFirstMoveLeft = false
                isFirstMoveRight = true
                scrollDistanceX = 0f
                deltaX - childEleScrollX
            } else {
                deltaX
            }
        } else { // RIGHT
            if (isFirstMoveRight) {
                isFirstMoveRight = false
                isFirstMoveLeft = true
                scrollDistanceX = 0f
                deltaX - childEleScrollX
            } else {
                deltaX
            }
        }
    }

    private fun applyVerticalScroll(delta: Float, newScrollTop: Float, maxScrollTop: Float) {
        when {
            newScrollTop < 0f -> {
                // 超出上边界 (delta为正)
                canScrollUp = false
                canScrollDown = true
                // 更新当前已经滚动的距离
                scrollDistanceY += (delta - (0f - newScrollTop))
            }
            newScrollTop > maxScrollTop -> {
                // 超出下边界 (delta为负数)
                canScrollDown = false
                canScrollUp = true
                scrollDistanceY += (delta - (maxScrollTop - newScrollTop))
            }
            else -> {
                canScrollUp = true
                canScrollDown = true
                scrollDistanceY += delta
            }
        }
        // 设置 scrollTop
        ele.scrollTop = newScrollTop.coerceIn(0f, maxScrollTop).toDouble()
        // 更新鼠标初始位置
        startY = ele.scrollTop.toFloat()
    }

    private fun applyHorizontalScroll(delta: Float, newScrollLeft: Float, maxScrollLeft: Float) {
        when {
            newScrollLeft < 0f -> {
                // 超出左边界 (delta为正数)
                canScrollLeft = false
                canScrollRight = true
                scrollDistanceX += (delta - (0f - newScrollLeft))
            }
            newScrollLeft > maxScrollLeft -> {
                // 超出右边界 (delta为负数)
                canScrollRight = false
                canScrollLeft = true
                scrollDistanceX += (delta - (maxScrollLeft - newScrollLeft))
            }
            else -> {
                canScrollLeft = true
                canScrollRight = true
                scrollDistanceX += delta
            }
        }
        ele.scrollLeft = newScrollLeft.coerceIn(0f, maxScrollLeft).toDouble()
        startX = ele.scrollLeft.toFloat()
    }

    fun onMouseMoveEvent(it: MouseEvent, prevScroll: JsMap<String, Float>) {
        if (!isMouseDown) return
        // cancel timer
        listView.cancelClickDetectionTimer()
        listView.setClickEvent(false)
        if (listView.pagingEnabled) {
            listView.listPagingHelper.handlePagerMouseMove(it)
            return
        }
        if (listView.nestScrollEnabled) {
            listView.nestScrollHelper.handleNestScrollMouseMove(it)
            return
        }
        handleMouseMove(it, prevScroll)
    }

    fun onMouseUpEvent(it: MouseEvent) {
        isMouseDown = false
        if (listView.isClickEvent()) {
            listView.handleClickEvent(it)
            return
        }
        if (listView.pagingEnabled) {
            listView.listPagingHelper.handlePagerMouseUp(it)
            return
        }
        if (listView.nestScrollEnabled) {
            listView.nestScrollHelper.handleNestScrollMouseUp(it)
            return
        }
        listView.handleTouchEnd()
    }
}

