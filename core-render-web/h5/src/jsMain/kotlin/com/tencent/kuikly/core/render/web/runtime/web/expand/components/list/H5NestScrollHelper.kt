package com.tencent.kuikly.core.render.web.runtime.web.expand.components.list

import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject

import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.HTMLElement
import org.w3c.dom.TouchEvent
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
    private var isDragging = 0

    private var lastScrollState: KRNestedScrollState = KRNestedScrollState.CAN_SCROLL
    private var parentScrollState: KRNestedScrollState = KRNestedScrollState.CAN_SCROLL
    private var nestScrollDistanceY = 0f;
    private var nestScrollDistanceX = 0f;

    // Scroll direction
    var scrollDirection: String = H5ListView.SCROLL_DIRECTION_COLUMN

    // Inertia scroll related properties
    private var lastTouchTime = 0L
    private var velocityY = 0f
    private var velocityX = 0f
    private var lastTouchY = 0f
    private var lastTouchX = 0f
    private var animationFrameId: Int = 0
    // Friction factor
    private val friction = 0.97f
    // Minimum velocity threshold
    private val minVelocity = 0.1f
    private var shouldHandleScroll = false

    init {
        ele.setAttribute("data-nested-scroll", "true")

        ele.addEventListener("nestedScrollToParent", { event: dynamic ->
            // don't handle the event if the target element is self
            if (event.target == ele) {
                return@addEventListener
            }

            val deltaY = event.detail.deltaY.unsafeCast<Float>()
            val deltaX = event.detail.deltaX.unsafeCast<Float>()
            nestScrollDistanceY = deltaY
            nestScrollDistanceX = deltaX
        }, json("passive" to false))

        ele.addEventListener("nestedScrollToChild", { event: dynamic ->
            // don't handle the event if the target element is self
            if (event.target == ele) {
                val deltaY = event.detail.deltaY.unsafeCast<Float>()
                val deltaX = event.detail.deltaX.unsafeCast<Float>()
                nestScrollDistanceY = deltaY
                nestScrollDistanceX = deltaX
                parentScrollState = KRNestedScrollState.SCROLL_BOUNDARY
            }
        }, json("passive" to false))
    }

    private fun dispatchScrollEventToParent(deltaX: Float, deltaY: Float) {
        val detail = json(
            "deltaX" to deltaX,
            "deltaY" to deltaY,
        )
        val scrollEvent = CustomEvent("nestedScrollToParent", CustomEventInit(
            cancelable = true,
            bubbles = true,
            detail = detail
        ))

        // Also dispatch to self for backward compatibility
        ele.dispatchEvent(scrollEvent)
    }

    private fun dispatchScrollEventToChild(deltaX: Float, deltaY: Float) {
        val detail = json(
            "deltaX" to deltaX,
            "deltaY" to deltaY,
        )
        val scrollEvent = CustomEvent("nestedScrollToChild", CustomEventInit(
            cancelable = true,
            bubbles = true,
            detail = detail
        ))

        // Dispatch to child elements that need to receive nestedScroll events
        dispatchToChildElements(scrollEvent)
    }

    private fun dispatchToChildElements(event: CustomEvent) {
        // Find all child elements that need to receive nestedScroll events
        val childElements = ele.querySelectorAll("[data-nested-scroll]")
        for (i in 0 until childElements.length) {
            val childElement = childElements[i] as? HTMLElement
            childElement?.dispatchEvent(event)
        }
    }

    private fun getNestScrollMode(rule: String): KRNestedScrollMode {
        return when (rule) {
            "" -> KRNestedScrollMode.SELF_FIRST
            else -> KRNestedScrollMode.valueOf(rule)
        }
    }

    fun setNestedScroll(propValue: Any): Boolean {
        if (propValue is String) {
            JSONObject(propValue).apply {
                scrollForwardMode = getNestScrollMode(optString("forward", ""))
                scrollBackwardMode = getNestScrollMode(optString("backward", ""))
            }
        }
        return true
    }

    fun handleNestScrollTouchStart(event: TouchEvent) {
        isDragging = 1
        val touch = (event as TouchEvent).touches[0]
        if (touch != null) {
            touchStartY = touch.clientY.toFloat()
            touchStartX = touch.clientX.toFloat()
            lastTouchY = touchStartY
            lastTouchX = touchStartX
            lastTouchTime = Date().getTime().toLong()
        }
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

    fun handleNestScrollTouchMove(event: TouchEvent) {
        val touch = (event as TouchEvent).touches[0]
        var deltaY = 0f
        var deltaX = 0f
        var distanceY = 0f
        var distanceX = 0f
        if (touch != null) {
            val currentTime = Date().getTime()
            val timeDelta = currentTime - lastTouchTime

            if (timeDelta > 0) {
                // Calculate velocity (pixels per millisecond)
                velocityY = ((touch.clientY - lastTouchY) / timeDelta).toFloat()
                velocityX = ((touch.clientX - lastTouchX) / timeDelta).toFloat()
            }

            lastTouchTime = currentTime.toLong()
            lastTouchY = touch.clientY.toFloat()
            lastTouchX = touch.clientX.toFloat()

            distanceY = touch.clientY - touchStartY - nestScrollDistanceY
            distanceX = touch.clientX - touchStartX - nestScrollDistanceX

            deltaY = touch.clientY - touchY
            deltaX = touch.clientX - touchX
            touchY = touch.clientY.toFloat()
            touchX = touch.clientX.toFloat()
        }

        // judge whether to continue scrolling
        val canScrollUp = ele.scrollTop > 0
        val canScrollDown = ceil(ele.scrollTop + ele.offsetHeight).toInt() < ele.scrollHeight
        val canScrollLeft = ele.scrollLeft > 0
        val canScrollRight = ceil(ele.scrollLeft + ele.offsetWidth).toInt() < ele.scrollWidth

        val delta = if (scrollDirection == H5ListView.SCROLL_DIRECTION_COLUMN) deltaY else deltaX
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
            lastScrollState = if (shouldHandleScroll) KRNestedScrollState.CAN_SCROLL else KRNestedScrollState.SCROLL_BOUNDARY
        }
    }

    fun handleNestScrollTouchEnd(event: TouchEvent) {
        isDragging = 0

        if (!shouldHandleScroll) {
            return
        }
        // Start inertia scroll if velocity is significant
        if (abs(velocityX) > minVelocity || abs(velocityY) > minVelocity) {
            // Convert velocity to pixels per frame (assuming 60fps)
            val frameVelocityX = velocityX * 6.67f
            val frameVelocityY = velocityY * 6.67f
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

    fun handleNestScrollTouchScroll(event: TouchEvent) {
        val offsetMap = listElement.updateOffsetMap(ele.scrollLeft.toFloat(), ele.scrollTop.toFloat(), isDragging)
        listElement.scrollEventCallback?.invoke(offsetMap)
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

            if (currentX < 0) {
                currentX = 0f
                currentVelocityX = 0f
            } else if (currentX > maxScrollX) {
                currentX = maxScrollX.toFloat()
                currentVelocityX = 0f
            }

            if (currentY < 0) {
                currentY = 0f
                currentVelocityX = 0f
            } else if (currentY > maxScrollY) {
                currentY = maxScrollY.toFloat()
                currentVelocityY = 0f
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

}

