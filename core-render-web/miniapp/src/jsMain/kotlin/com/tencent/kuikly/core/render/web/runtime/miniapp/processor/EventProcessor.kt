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

// Default configuration constants
private const val DEFAULT_MOVE_TOLERANCE = 10 // Movement tolerance (pixels)


/**
 * Long press event handler for mini app
 * Handles complete long press lifecycle: start -> move -> end
 */
class LongPressHandler(
    private val element: HTMLElement,
    private val onLongPress: (dynamic, String) -> Unit,  // Pass both event and state
    private val moveTolerance: Int = DEFAULT_MOVE_TOLERANCE
) {
    private var startX: Int = 0
    private var startY: Int = 0
    private var isLongPressing: Boolean = false

    init {
        element.asDynamic().longPressHandler = this
        setupListeners()
    }

    /**
     * Set up event listeners
     */
    private fun setupListeners() {
        // Record touch start position
        element.addEventListener("touchstart", { event: dynamic ->
            if (event.touches.length == 1) {
                val touch = event.touches[0]
                if (touch != null) {
                    startX = touch.clientX as Int
                    startY = touch.clientY as Int
                }
            }
        })

        // Long press triggered by mini app native event
        element.addEventListener("longpress", { event: dynamic ->
            canPan = false
            isLongPressing = true
            onLongPress(event, EVENT_STATE_START)
        })

        // Handle touch move during long press
        element.addEventListener("touchmove", { event: dynamic ->
            if (!isLongPressing) return@addEventListener
            if (event.touches.length == 1) {
                val touch = event.touches[0]
                if (touch != null) {
                    onLongPress(event, EVENT_STATE_MOVE)
                }
            }
        })

        // Handle touch end during long press
        element.addEventListener("touchend", { event: dynamic ->
            if (isLongPressing) {
                onLongPress(event, EVENT_STATE_END)
            }
            isLongPressing = false
        })

        // Handle touch cancel during long press
        element.addEventListener("touchcancel", { event: dynamic ->
            if (isLongPressing) {
                onLongPress(event, EVENT_STATE_END)
            }
            isLongPressing = false
        })
    }

    companion object {
        // longPress event state
        const val EVENT_STATE_START = "start"
        const val EVENT_STATE_MOVE = "move"
        const val EVENT_STATE_END = "end"
    }
}

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
     * process event callback with explicit state parameter
     */
    private fun handleEventCallbackWithState(event: dynamic, state: String, callback: (event: IEvent?) -> Unit) {
        // Try to get touch from touches array first, then from changedTouches (for touchend)
        val touches = event.touches
        val changedTouches = event.changedTouches
        
        val touch: dynamic = when {
            jsTypeOf(touches) != "undefined" && touches != null && touches.length > 0 -> touches[0]
            jsTypeOf(changedTouches) != "undefined" && changedTouches != null && changedTouches.length > 0 -> changedTouches[0]
            else -> null
        }
        
        if (touch != null) {
            val miniTouch = MiniTouchEvent(
                screenX = (touch.screenX as? Int) ?: 0,
                screenY = (touch.screenY as? Int) ?: 0,
                clientX = (touch.clientX as? Int) ?: 0,
                clientY = (touch.clientY as? Int) ?: 0,
                offsetX = (touch.clientX as? Int) ?: 0,
                offsetY = (touch.clientY as? Int) ?: 0,
                pageX = (touch.pageX as? Int) ?: 0,
                pageY = (touch.pageY as? Int) ?: 0
            )
            miniTouch.state = state
            callback(miniTouch)
        } else {
            // For events without touch data (like touchend with empty touches),
            // create a minimal event with state only
            val miniTouch = MiniTouchEvent(
                screenX = 0,
                screenY = 0,
                clientX = 0,
                clientY = 0,
                offsetX = 0,
                offsetY = 0,
                pageX = 0,
                pageY = 0
            )
            miniTouch.state = state
            callback(miniTouch)
        }
    }

    /**
     * process event callback (legacy, for events without state)
     */
    private fun handleEventCallback(event: MiniEvent, callback: (event: IEvent?) -> Unit) {
        val touch = event.touches?.get(0)
        if (touch != null && jsTypeOf(touch) != "undefined") {
            val miniTouch = MiniTouchEvent(
                screenX = touch.screenX as? Int ?: 0,
                screenY = touch.screenY as? Int ?: 0,
                clientX = touch.clientX as? Int ?: 0,
                clientY = touch.clientY as? Int ?: 0,
                offsetX = touch.clientX as? Int ?: 0,
                offsetY = touch.clientY as? Int ?: 0,
                pageX = touch.pageX as? Int ?: 0,
                pageY = touch.pageY as? Int ?: 0
            )
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
     * Uses LongPressHandler to handle complete lifecycle: start -> move -> end
     */
    override fun longPress(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        LongPressHandler(element = ele.unsafeCast<HTMLElement>(), onLongPress = { event, state ->
            handleEventCallbackWithState(event, state, callback)
        })
    }

    override fun pan(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        PanHandler(element = ele.unsafeCast<HTMLElement>(), onPan = { event ->
            handleEventCallback(event.unsafeCast<MiniEvent>(), callback)
        })
    }
}