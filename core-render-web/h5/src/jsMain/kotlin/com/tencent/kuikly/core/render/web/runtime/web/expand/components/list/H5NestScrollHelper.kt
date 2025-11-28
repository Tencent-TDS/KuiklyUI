package com.tencent.kuikly.core.render.web.runtime.web.expand.components.list

import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject

import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.HTMLElement
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.js.Date
import kotlin.js.json
import kotlin.math.ceil
import kotlin.math.abs

class H5NestScrollHelper(private val ele: HTMLElement, private var listElement: IListElement) {
    // Nested scroll forwardMode
    private var scrollForwardMode = KRNestedScrollMode.SELF_FIRST
    // Nested scroll backwardMode
    private var scrollBackwardMode = KRNestedScrollMode.SELF_FIRST
    private var lastScrollY = 0f
    private var lastScrollX = 0f
    // Starting vertical touch position
    private var touchStartY = 0f
    // Current vertical touch position
    private var touchY = 0f
    // Starting horizontal touch position
    private var touchStartX = 0f
    // Current horizontal touch position
    private var touchX = 0f
    // Whether currently dragging
    private var isDragging = DRAG_STATE_IDLE
    // Whether the mouse is pressed
    private var isMouseDown: Boolean = false

    private var lastScrollState: KRNestedScrollState = KRNestedScrollState.CAN_SCROLL
    private var parentScrollState: KRNestedScrollState = KRNestedScrollState.CAN_SCROLL
    private var nestScrollDistanceY = 0f
    private var nestScrollDistanceX = 0f

    // Scroll direction
    var scrollDirection: String = H5ListView.SCROLL_DIRECTION_COLUMN

    // Inertia scroll related properties
    private var lastTouchTime = 0L
    private var velocityY = 0f
    private var velocityX = 0f
    private var lastTouchY = 0f
    private var lastTouchX = 0f
    private var animationFrameId: Int = 0
    // Inertia friction coefficient (0-1, higher = less friction)
    private val friction = INERTIA_FRICTION
    // Minimum velocity threshold to continue inertia scroll
    private val minVelocity = MIN_VELOCITY_THRESHOLD
    private var shouldHandleScroll = false

    init {
        ele.setAttribute(DATA_NESTED_SCROLL_ATTR, ATTR_VALUE_TRUE)

        ele.addEventListener(EVENT_NESTED_SCROLL_TO_PARENT, { event: dynamic ->
            // don't handle the event if the target element is self
            if (event.target == ele) {
                return@addEventListener
            }

            val deltaY = event.detail.deltaY.unsafeCast<Float>()
            val deltaX = event.detail.deltaX.unsafeCast<Float>()
            nestScrollDistanceY = deltaY
            nestScrollDistanceX = deltaX
        }, json(PASSIVE_KEY to false))

        ele.addEventListener(EVENT_NESTED_SCROLL_TO_CHILD, { event: dynamic ->
            // don't handle the event if the target element is self
            if (event.target == ele) {
                val deltaY = event.detail.deltaY.unsafeCast<Float>()
                val deltaX = event.detail.deltaX.unsafeCast<Float>()
                nestScrollDistanceY = deltaY
                nestScrollDistanceX = deltaX
                parentScrollState = KRNestedScrollState.SCROLL_BOUNDARY
            }
        }, json(PASSIVE_KEY to false))
    }

    private fun dispatchScrollEventToParent(deltaX: Float, deltaY: Float) {
        val detail = json(
            DETAIL_DELTA_X to deltaX,
            DETAIL_DELTA_Y to deltaY,
        )
        val scrollEvent = CustomEvent(EVENT_NESTED_SCROLL_TO_PARENT, CustomEventInit(
            cancelable = true,
            bubbles = true,
            detail = detail
        ))

        // Also dispatch to self for backward compatibility
        ele.dispatchEvent(scrollEvent)
    }

    private fun dispatchScrollEventToChild(deltaX: Float, deltaY: Float) {
        val detail = json(
            DETAIL_DELTA_X to deltaX,
            DETAIL_DELTA_Y to deltaY,
        )
        val scrollEvent = CustomEvent(EVENT_NESTED_SCROLL_TO_CHILD, CustomEventInit(
            cancelable = true,
            bubbles = true,
            detail = detail
        ))

        // Dispatch to child elements that need to receive nestedScroll events
        dispatchToChildElements(scrollEvent)
    }

    private fun dispatchToChildElements(event: CustomEvent) {
        // Find all child elements that need to receive nestedScroll events
        val childElements = ele.querySelectorAll(NESTED_SCROLL_SELECTOR)
        for (i in 0 until childElements.length) {
            val childElement = childElements[i] as? HTMLElement
            childElement?.dispatchEvent(event)
        }
    }

    private fun getNestScrollMode(rule: String): KRNestedScrollMode {
        return when (rule) {
            EMPTY_STRING -> KRNestedScrollMode.SELF_FIRST
            else -> KRNestedScrollMode.valueOf(rule)
        }
    }

    fun setNestedScroll(propValue: Any): Boolean {
        if (propValue is String) {
            JSONObject(propValue).apply {
                scrollForwardMode = getNestScrollMode(optString(JSON_KEY_FORWARD, EMPTY_STRING))
                scrollBackwardMode = getNestScrollMode(optString(JSON_KEY_BACKWARD, EMPTY_STRING))
            }
        }
        return true
    }

    /**
     * Handle common logic of touchStart and mouseDown in nested scrolling
     */
    private fun handleNestScrollStart(startX: Float, startY: Float, setMouseDown: Boolean = false) {
        isDragging = DRAG_STATE_DRAGGING
        if (setMouseDown) isMouseDown = true

        touchStartY = startY
        touchStartX = startX
        lastTouchY = touchStartY
        lastTouchX = touchStartX
        lastTouchTime = Date().getTime().toLong()

        lastScrollY = ele.scrollTop.toFloat()
        lastScrollX = ele.scrollLeft.toFloat()
        parentScrollState = KRNestedScrollState.CAN_SCROLL
        lastScrollState = KRNestedScrollState.CAN_SCROLL
        touchY = touchStartY
        touchX = touchStartX
        nestScrollDistanceY = 0f
        nestScrollDistanceX = 0f
        velocityY = 0f
        velocityX = 0f
        shouldHandleScroll = false
        cancelInertiaScroll()
    }

    /**
     * Handle common logic of touchMove and mouseMove in nested scrolling
     */
    private fun handleNestedScrollMove(currentX: Float?, currentY: Float?, event: Event) {
        var deltaY = 0f
        var deltaX = 0f
        var distanceY = 0f
        var distanceX = 0f

        if (currentX != null && currentY != null) {
            val currentTime = Date().getTime()
            val timeDelta = currentTime - lastTouchTime

            if (timeDelta > 0) {
                // Calculate velocity (pixels per millisecond)
                velocityY = ((currentY - lastTouchY) / timeDelta).toFloat()
                velocityX = ((currentX - lastTouchX) / timeDelta).toFloat()
            }

            lastTouchTime = currentTime.toLong()
            lastTouchY = currentY
            lastTouchX = currentX

            distanceY = currentY - touchStartY - nestScrollDistanceY
            distanceX = currentX - touchStartX - nestScrollDistanceX

            deltaY = currentY - touchY
            deltaX = currentX - touchX
            touchY = currentY
            touchX = currentX
        }

        val delta = if (scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN) deltaY else deltaX

        if (delta == 0f) {
            return
        }

        // judge whether to continue scrolling
        val canScrollUp = ele.scrollTop > 0
        val canScrollDown = ceil(ele.scrollTop + ele.offsetHeight).toInt() < ele.scrollHeight
        val canScrollLeft = ele.scrollLeft > 0
        val canScrollRight = ceil(ele.scrollLeft + ele.offsetWidth).toInt() < ele.scrollWidth
        val scrollMode = if (delta < 0) scrollForwardMode else scrollBackwardMode
        when (scrollMode) {
            KRNestedScrollMode.SELF_FIRST -> {
                if (scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN) {
                    shouldHandleScroll = (deltaY < 0 && canScrollDown) || (deltaY > 0 && canScrollUp)
                } else if (scrollDirection == H5ListView.SCROLL_DIRECTION_ROW) {
                    shouldHandleScroll = (deltaX < 0 && canScrollRight) || (deltaX > 0 && canScrollLeft)
                }
                parentScrollState = KRNestedScrollState.CAN_SCROLL
            }
            KRNestedScrollMode.PARENT_FIRST -> {
                // if parent first, pass event to parent
                shouldHandleScroll = (parentScrollState == KRNestedScrollState.SCROLL_BOUNDARY)
            }
            KRNestedScrollMode.SELF_ONLY -> {
                // if self only, handle self scroll, not pass event to parent
                shouldHandleScroll = true
            }
        }

        if (shouldHandleScroll) {
            event.preventDefault()
            event.stopPropagation()

            // manually control scroll
            if (scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN) {
                ele.scrollTo(ele.scrollLeft, (lastScrollY - distanceY).toDouble())
            } else {
                ele.scrollTo((lastScrollX - distanceX).toDouble(), ele.scrollTop)
            }

            // update offsetMap
            val offsetMap = listElement.updateOffsetMap(ele.scrollLeft.toFloat(), ele.scrollTop.toFloat(), isDragging)
            listElement.scrollEventCallback?.invoke(offsetMap)
        }  else if (lastScrollState == KRNestedScrollState.CAN_SCROLL) {
            // Dispatch scroll event to parent
            if (scrollMode == KRNestedScrollMode.SELF_FIRST) {
                dispatchScrollEventToChild(distanceX, distanceY)
                dispatchScrollEventToParent(distanceX, distanceY)
            }
        }

        // update lastScrollState
        if (scrollMode == KRNestedScrollMode.SELF_FIRST) {
            lastScrollState = if (shouldHandleScroll)
                KRNestedScrollState.CAN_SCROLL
            else KRNestedScrollState.SCROLL_BOUNDARY
        }
    }

    /**
     * Handle common logic of touchEnd and mouseUp in nested scrolling
     */
    private fun handleNestedScrollEnd(event: Event, clearMouseDown: Boolean = false) {
        isDragging = DRAG_STATE_IDLE
        if (clearMouseDown) isMouseDown = false

        if (!shouldHandleScroll) {
            return
        }
        // Start inertia scroll if velocity is significant
        if (abs(velocityX) > minVelocity || abs(velocityY) > minVelocity) {
            // Convert velocity to pixels per frame (velocity is in px/ms, multiply by frame duration)
            val frameVelocityX = velocityX * FRAME_DURATION_MS
            val frameVelocityY = velocityY * FRAME_DURATION_MS
            startInertiaScroll(frameVelocityX, frameVelocityY)
        }

        // Get horizontal and vertical offset of the element during scroll event
        val offsetX = ele.scrollLeft.toFloat()
        val offsetY = ele.scrollTop.toFloat()
        val offsetMap = listElement.updateOffsetMap(offsetX, offsetY, isDragging)
        // Event callback
        listElement.willDragEndEventCallback?.invoke(offsetMap)
        listElement.dragEndEventCallback?.invoke(offsetMap)
        listElement.scrollEventCallback?.invoke(offsetMap)
    }

    fun handleNestScrollTouchStart(event: TouchEvent) {
        val touch = event.touches[0]
        if (touch != null) {
            handleNestScrollStart(touch.clientX.toFloat(), touch.clientY.toFloat())
        }
    }

    fun handleNestScrollTouchMove(event: TouchEvent) {
        val touch = event.touches[0]
        if (touch != null) {
            handleNestedScrollMove(touch.clientX.toFloat(), touch.clientY.toFloat(), event)
        } else {
            handleNestedScrollMove(null, null, event)
        }
    }

    fun handleNestScrollTouchEnd(event: TouchEvent) {
        handleNestedScrollEnd(event)
    }

    fun handleNestScrollTouchScroll(event: Event) {
        val offsetMap = listElement.updateOffsetMap(ele.scrollLeft.toFloat(), ele.scrollTop.toFloat(), isDragging)
        listElement.scrollEventCallback?.invoke(offsetMap)

    }

    fun handleNestScrollMouseDown(event: MouseEvent) {
        handleNestScrollStart(event.clientX.toFloat(), event.clientY.toFloat(), setMouseDown = true)
    }

    fun handleNestScrollMouseMove(event: MouseEvent) {
        if (!isMouseDown) return
        handleNestedScrollMove(event.clientX.toFloat(), event.clientY.toFloat(), event)
    }

    fun handleNestScrollMouseUp(event: MouseEvent) {
        handleNestedScrollEnd(event, clearMouseDown = true)
    }

    private fun startInertiaScroll(initialVelocityX: Float, initialVelocityY: Float) {
        var currentVelocityX = initialVelocityX
        var currentVelocityY = initialVelocityY
        var currentX = ele.scrollLeft.toFloat()
        var currentY = ele.scrollTop.toFloat()

        fun animate(timestamp: Double) {
            if (abs(currentVelocityX) < minVelocity && abs(currentVelocityY) < minVelocity) {
                kuiklyWindow.cancelAnimationFrame(animationFrameId)
                return
            }

            // Apply friction
            currentVelocityX *= friction
            currentVelocityY *= friction

            // Update position
            currentX -= currentVelocityX
            currentY -= currentVelocityY

            // Check boundaries
            val maxScrollX = ele.scrollWidth - ele.clientWidth
            val maxScrollY = ele.scrollHeight - ele.clientHeight

            if (currentX < MIN_SCROLL_POSITION) {
                currentX = MIN_SCROLL_POSITION
                currentVelocityX = VELOCITY_ZERO
            } else if (currentX > maxScrollX) {
                currentX = maxScrollX.toFloat()
                currentVelocityX = VELOCITY_ZERO
            }

            if (currentY < MIN_SCROLL_POSITION) {
                currentY = MIN_SCROLL_POSITION
                currentVelocityY = VELOCITY_ZERO  // Fixed: was incorrectly setting currentVelocityX
            } else if (currentY > maxScrollY) {
                currentY = maxScrollY.toFloat()
                currentVelocityY = VELOCITY_ZERO
            }

            // Apply scroll
            if (scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN) {
                ele.scrollTo(ele.scrollLeft, currentY.toDouble())
            } else {
                ele.scrollTo(currentX.toDouble(), ele.scrollTop)
            }

            // Update offset map
            val offsetMap = listElement.updateOffsetMap(currentX, currentY, isDragging)
            listElement.scrollEventCallback?.invoke(offsetMap)

            animationFrameId = kuiklyWindow.requestAnimationFrame(::animate)
        }

        animationFrameId = kuiklyWindow.requestAnimationFrame(::animate)
    }

    private fun cancelInertiaScroll() {
        kuiklyWindow.cancelAnimationFrame(animationFrameId)
    }

    companion object {
        // Drag state constants
        private const val DRAG_STATE_IDLE = 0
        private const val DRAG_STATE_DRAGGING = 1

        // Inertia scroll constants
        private const val INERTIA_FRICTION = 0.97f
        private const val MIN_VELOCITY_THRESHOLD = 0.1f
        // Frame duration in milliseconds (assuming ~60fps, 1000ms / 150fps ≈ 6.67ms for smooth scrolling)
        private const val FRAME_DURATION_MS = 6.67f
        private const val MIN_SCROLL_POSITION = 0f
        private const val VELOCITY_ZERO = 0f

        // DOM attribute constants
        private const val DATA_NESTED_SCROLL_ATTR = "data-nested-scroll"
        private const val ATTR_VALUE_TRUE = "true"
        private const val NESTED_SCROLL_SELECTOR = "[data-nested-scroll]"

        // Custom event names
        private const val EVENT_NESTED_SCROLL_TO_PARENT = "nestedScrollToParent"
        private const val EVENT_NESTED_SCROLL_TO_CHILD = "nestedScrollToChild"

        // Event detail keys
        private const val DETAIL_DELTA_X = "deltaX"
        private const val DETAIL_DELTA_Y = "deltaY"

        // JSON keys for nested scroll config
        private const val JSON_KEY_FORWARD = "forward"
        private const val JSON_KEY_BACKWARD = "backward"
        private const val EMPTY_STRING = ""

        // Event listener option key
        private const val PASSIVE_KEY = "passive"
    }
}

