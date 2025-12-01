package com.tencent.kuikly.core.render.web.runtime.miniapp.processor

import com.tencent.kuikly.core.render.web.processor.IEvent
import com.tencent.kuikly.core.render.web.processor.IEventProcessor
import com.tencent.kuikly.core.render.web.processor.state
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.MiniEvent
import org.w3c.dom.HTMLElement
import kotlin.math.abs

// 为 MiniEvent 增加 state 属性, 用于记录longpress/pan事件状态
var MiniEvent.state: String?
    get() = asDynamic().state as? String
    set(value) {
        asDynamic().state = value
    }

// 互斥 longPress 与 pan 标志
var canPan: Boolean = false


class PanHandler(
    private val element: HTMLElement,
    private val onPan: (MiniEvent) -> Unit,
    private val moveTolerance: Int = DEFAULT_MOVE_TOLERANCE
) {
    private var startX: Int = 0
    private var startY: Int = 0
    private var isPaning: Boolean = false

    init {
        element.asDynamic().panHandler = this
        setupListeners()
    }

    /**
     * Set up event listeners
     */
    private fun setupListeners() {
        // Touch events
        element.addEventListener("touchstart", { event: dynamic ->
            if (event.touches.length == 1) {
                val touch = event.unsafeCast<MiniEvent>().touches[0]
                if (touch != null) {
                    startX = touch.clientX
                    startY = touch.clientY
                    canPan = true
                }
            }
        })

        element.addEventListener("touchmove", { event: dynamic ->
            if (!canPan) return@addEventListener
            if (event.touches.length == 1) {
                val touch = event.unsafeCast<MiniEvent>().touches[0]
                if (touch != null) {
                    val moveX = touch.clientX as Int
                    val moveY = touch.clientY as Int
                    // If movement exceeds tolerance, cancel long press
                    if ((abs(moveX - startX) > moveTolerance || abs(moveY - startY) > moveTolerance) && !isPaning) {
                        isPaning = true
                        event.state = EVENT_STATE_START
                        onPan(event as MiniEvent)
                    }
                    if (isPaning) {
                        event.state = EVENT_STATE_MOVE
                        onPan(event as MiniEvent)
                    }
                }
            }
        })

        /**
         * Cancel listener
         */
        element.addEventListener("touchend", { event: dynamic ->
            if (isPaning) {
                event.state = EVENT_STATE_END
                onPan(event as MiniEvent)
            }
            isPaning = false
            canPan = false
        })
        /**
         * Cancel listener
         */
        element.addEventListener("touchcancel", { _ ->
            isPaning = false
            canPan = false
        })

    }
    
    companion object {
        // Default configuration
        private const val DEFAULT_DOUBLE_TAP_DELAY = 300 // Double tap interval time (milliseconds)
        private const val DEFAULT_LONG_PRESS_DELAY = 700 // Long press trigger time (milliseconds)
        private const val DEFAULT_MOVE_TOLERANCE = 10 // Movement tolerance (pixels)

        // longPress/pan event state
        private const val EVENT_STATE_START = "start"
        const val EVENT_STATE_MOVE = "move"
        private const val EVENT_STATE_END = "end"
    }
}

/**
 * mini app touch event
 */
data class MiniTouchEvent(
    override val screenX: Int,
    override val screenY: Int,
    override val clientX: Int,
    override val clientY: Int,
    override val offsetX: Int,
    override val offsetY: Int,
    override val pageX: Int,
    override val pageY: Int
) : IEvent

/**
 * mini app common event processor
 */
object EventProcessor : IEventProcessor {
    /**
     * process event callback
     */
    private fun handleEventCallback(event: MiniEvent, callback: (event: IEvent?) -> Unit) {
        val touch = event.unsafeCast<MiniEvent>().touches[0]
        if (jsTypeOf(touch) != "undefined") {
            val miniTouch = MiniTouchEvent(
                screenX = touch.screenX,
                screenY = touch.screenY,
                clientX = touch.clientX,
                clientY = touch.clientY,
                offsetX = touch.clientX,
                offsetY = touch.clientY,
                pageX = touch.pageX,
                pageY = touch.pageY
            )
            miniTouch.state = event.state
            callback(miniTouch)
        }
    }

    /**
     * bind mini app double click event
     */
    override fun doubleClick(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        ele.addEventListener("tap", { event: dynamic ->
            val target = ele.asDynamic()
            val nowTime = js("Date.now()")
            val clickTime = target["clickTime"] as Int? ?: 0
            val dbTime = 500
            if (nowTime - clickTime < dbTime) {
                // double click，trigger event
                target["clickTime"] = 0
                handleEventCallback(event.unsafeCast<MiniEvent>(), callback)
            } else {
                // single tap, record click time
                target["clickTime"] = nowTime
            }
        })
    }

    /**
     * bind mini app long press event
     */
    override fun longPress(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        ele.addEventListener("longpress", { event: dynamic ->
            canPan = false
            event.unsafeCast<MiniEvent>().state = PanHandler.EVENT_STATE_MOVE
            handleEventCallback(event.unsafeCast<MiniEvent>(), callback)
        })
    }

    override fun pan(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        PanHandler(element = ele.unsafeCast<HTMLElement>(), onPan = { event ->
            handleEventCallback(event.unsafeCast<MiniEvent>(), callback)
        })
    }
}