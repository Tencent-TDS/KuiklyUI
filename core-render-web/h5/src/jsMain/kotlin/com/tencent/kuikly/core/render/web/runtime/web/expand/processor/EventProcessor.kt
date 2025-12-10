package com.tencent.kuikly.core.render.web.runtime.web.expand.processor

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.get
import com.tencent.kuikly.core.render.web.collection.array.clear
import com.tencent.kuikly.core.render.web.collection.array.isEmpty
import com.tencent.kuikly.core.render.web.collection.array.remove
import com.tencent.kuikly.core.render.web.processor.IEvent
import com.tencent.kuikly.core.render.web.processor.IEventProcessor
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import org.w3c.dom.HTMLElement
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.js.Date
import kotlin.math.abs
import com.tencent.kuikly.core.render.web.processor.state
import com.tencent.kuikly.core.render.web.runtime.web.expand.components.list.isTouchEventOrNull


/**
 * Implement pan events across element boundaries
 */
object PCPanEventHandler {
    // Save all elements that trigger mouseDown event
    var mouseDownEleIds: JsArray<String> = JsArray()
    // Save the elements of ListView
    private var panHandler: TouchEventHandlers.PanHandler? = null

    init {
        kuiklyWindow.addEventListener("mousemove", {
            if (mouseDownEleIds.isEmpty()) return@addEventListener
            val id = mouseDownEleIds[0]
            val ele = kuiklyWindow.document.getElementById(id)
            panHandler = ele.asDynamic().panHandler.unsafeCast<TouchEventHandlers.PanHandler>()
            panHandler?.handleMouseMove(it as MouseEvent)
        })

        kuiklyWindow.addEventListener("mouseup", {
            panHandler?.handleMouseUp(it as MouseEvent)
            mouseDownEleIds.clear()
            panHandler = null
        })
    }
}

// 为 Event 增加 state 属性, 用于记录longpress/pan事件状态
var Event.state: String?
    get() = asDynamic().state as? String
    set(value) {
        asDynamic().state = value
    }

// Event 添加 touchOffsetX/touchOffsetY 属性, 供 H5Event 使用
var Event.touchOffsetX: Int?
    get() = asDynamic().touchOffsetX as? Int
    set(value) {
        asDynamic().touchOffsetX = value
    }

var Event.touchOffsetY: Int?
    get() = asDynamic().touchOffsetY as? Int
    set(value) {
        asDynamic().touchOffsetY = value
    }

// 互斥 longPress 与 pan 标志
var canPan: Boolean = false

/**
 * Mobile touch event handler
 * Supports double tap and long press events
 */
class TouchEventHandlers {
    // Double tap event handling
    class DoubleTapHandler(
        private val element: HTMLElement,
        private val onDoubleTap: (Event) -> Unit,
        private val doubleTapDelay: Int = DEFAULT_DOUBLE_TAP_DELAY,
        private val moveTolerance: Int = DEFAULT_MOVE_TOLERANCE
    ) {
        private var lastTapTime: Double = 0.0
        private var lastTapX: Int = 0
        private var lastTapY: Int = 0

        init {
            setupListeners()
        }

        /**
         * Set up event listeners
         */
        private fun setupListeners() {
            // Use touch events for mobile devices
            element.addEventListener("touchstart", { event ->
                event as TouchEvent
                handleTap(event)
            })
            element.addEventListener("dblclick", { event ->
                val clickEvent = event.asDynamic()
                clickEvent.stopPropagation()
                onDoubleTap(event)
            })
        }

        /**
         * Handle mobile tap event
         */
        private fun handleTap(event: TouchEvent) {
            if (event.touches.length == 1) {
                val touch = event.touches[0]
                val currentTime = Date.now()
                val x = touch?.clientX
                val y = touch?.clientY

                if (x != null && y != null) {
                    if (currentTime - lastTapTime < doubleTapDelay
                        && abs(x - lastTapX) < moveTolerance
                        && abs(y - lastTapY) < moveTolerance
                    ) {
                        // Double tap triggered
                        event.preventDefault()
                        event.stopPropagation()
                        event.touchOffsetX = x - element.getBoundingClientRect().left.toInt() - element.clientLeft
                        event.touchOffsetY = y - element.getBoundingClientRect().top.toInt() - element.clientTop
                        onDoubleTap(event)
                        // Reset timer to prevent continuous triggering
                        lastTapTime = 0.0
                    } else {
                        // Record first click
                        lastTapTime = currentTime
                        lastTapX = x
                        lastTapY = y
                    }
                }
            }
        }
    }

    // Long press event handling
    class LongPressHandler(
        private val element: HTMLElement,
        private val onLongPress: (Event) -> Unit,
        private val longPressDelay: Int = DEFAULT_LONG_PRESS_DELAY,
        private val moveTolerance: Int = DEFAULT_MOVE_TOLERANCE
    ) {
        private var pressTimer: Int? = null
        private var startX: Int = 0
        private var startY: Int = 0
        private var isLongPressing: Boolean = false
        private var isMouseDown: Boolean = false

        init {
            setupListeners()
        }

        /**
         * Set up event listeners
         */
        private fun setupListeners() {
            // Prevent default context menu
            element.addEventListener("contextmenu", { event ->
                event.preventDefault()
            })

            // Touch events
            element.addEventListener("touchstart", { event ->
                event as TouchEvent
                if (event.touches.length == 1) {
                    val touch = event.touches[0]
                    if (touch != null) {
                        startX = touch.clientX
                        startY = touch.clientY
                    }
                    startTimer(event)
                }
            })

            element.addEventListener("touchmove", { event ->
                event as TouchEvent
                if (event.touches.length == 1) {
                    val touch = event.touches[0]
                    if (touch != null) {
                        val moveX = touch.clientX
                        val moveY = touch.clientY
                        // If movement exceeds tolerance, cancel long press
                        if ((abs(moveX - startX) > moveTolerance ||
                                    abs(moveY - startY) > moveTolerance) && !isLongPressing) {
                            cancelTimer()
                        }
                        if (isLongPressing) {
                            event.state = EVENT_STATE_MOVE
                            onLongPress(event)
                        }
                    }
                }
            })

            /**
             * Cancel listener
             */
            element.addEventListener("touchend", { event ->
                if (isLongPressing) {
                    event.state = EVENT_STATE_END
                    onLongPress(event)
                }
                cancelTimer()
            })
            /**
             * Cancel listener
             */
            element.addEventListener("touchcancel", { event ->
                if (isLongPressing) {
                    event.state = EVENT_STATE_END
                    onLongPress(event)
                }
                cancelTimer()
            })

            // Mouse events as fallback
            element.addEventListener("mousedown", { event ->
                event as MouseEvent
                if (event.button == 0.toShort()) { // Only handle left click
                    startX = event.clientX
                    startY = event.clientY
                    isMouseDown = true
                    // Clears the pan/long-press triggered flag
                    element.asDynamic().panOrLongPressTriggered = false
                    startTimer(event)
                }
            })

            element.addEventListener("mousemove", { event ->
                event as MouseEvent
                if (!isMouseDown) return@addEventListener
                val moveX = event.clientX
                val moveY = event.clientY

                // If movement exceeds tolerance, cancel long press
                if ((abs(moveX - startX) > moveTolerance ||
                            abs(moveY - startY) > moveTolerance) && !isLongPressing) {
                    cancelTimer()
                }
                if (isLongPressing) {
                    event.state = EVENT_STATE_MOVE
                    onLongPress(event)
                }
            })
            /**
             * Cancel listener
             */
            element.addEventListener("mouseup", { event ->
                if (isLongPressing) {
                    event.state = EVENT_STATE_END
                    onLongPress(event)
                    // Marks the current element as having triggered a pan or long-press event
                    element.asDynamic().panOrLongPressTriggered = true
                }
                isMouseDown = false
                cancelTimer()
            })
            /**
             * Cancel listener
             */
            element.addEventListener("mouseleave", { _ ->
                isMouseDown = false
                cancelTimer()
            })
        }

        /**
         * Start touch listener
         */
        private fun startTimer(event: Event) {
            cancelTimer() // Ensure no running timer
            isLongPressing = false
            pressTimer = kuiklyWindow.setTimeout({
                isLongPressing = true
                canPan = false // Disable pan event when longPress
                // Prevent default event
                event.state = EVENT_STATE_START
                onLongPress(event)
                event.preventDefault()

            }, longPressDelay)
        }

        /**
         * Cancel touch listener
         */
        private fun cancelTimer() {
            pressTimer?.let {
                kuiklyWindow.clearTimeout(it)
                pressTimer = null
            }
            isLongPressing = false
        }
    }

    class PanHandler(
        private val element: HTMLElement,
        private val onPan: (Event) -> Unit,
        private val moveTolerance: Int = DEFAULT_MOVE_TOLERANCE
    ) {
        private var startX: Int = 0
        private var startY: Int = 0
        private var isPaning: Boolean = false
        private var isMouseDown: Boolean = false

        init {
            element.asDynamic().panHandler = this
            setupListeners()
            PCPanEventHandler
        }

        /**
         * Set up event listeners
         */
        private fun setupListeners() {
            // Prevent default context menu
            element.addEventListener("contextmenu", { event ->
                event.preventDefault()
            })

            // Touch events
            element.addEventListener("touchstart", { event ->
                event as TouchEvent
                if (event.touches.length == 1) {
                    val touch = event.touches[0]
                    if (touch != null) {
                        startX = touch.clientX
                        startY = touch.clientY
                        canPan = true
                    }
                }
            })

            element.addEventListener("touchmove", { event ->
                event as TouchEvent
                if (!canPan) return@addEventListener
                if (event.touches.length == 1) {
                    val touch = event.touches[0]
                    if (touch != null) {
                        val moveX = touch.clientX
                        val moveY = touch.clientY
                        // If movement exceeds tolerance, cancel long press
                        if ((abs(moveX - startX) > moveTolerance || abs(moveY - startY) > moveTolerance) && !isPaning) {
                            isPaning = true
                            event.state = EVENT_STATE_START
                            onPan(event)
                            event.preventDefault()
                        }
                        if (isPaning) {
                            event.state = EVENT_STATE_MOVE
                            onPan(event)
                        }
                    }
                }
            })

            /**
             * Cancel listener
             */
            element.addEventListener("touchend", { event ->
                if (isPaning) {
                    event.state = EVENT_STATE_END
                    onPan(event)
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

            // Mouse events as fallback
            element.addEventListener("mousedown", { event ->
                event as MouseEvent
                if (event.button == 0.toShort()) { // Only handle left click
                    startX = event.clientX
                    startY = event.clientY
                    canPan = true
                    isMouseDown= true
                    PCPanEventHandler.mouseDownEleIds.add(element.id)
                    // Clears the pan/long-press triggered flag
                    element.asDynamic().panOrLongPressTriggered = false
                }
            })

            // Prevent text selection
            element.addEventListener("selectstart", {
                it.preventDefault();
            },)
            // Prevent image drag
            element.addEventListener("dragstart", {
                it.preventDefault();
            })
        }

         fun handleMouseMove(event: MouseEvent) {
            if (!canPan || !isMouseDown) {
                PCPanEventHandler.mouseDownEleIds.remove(element.id)
                return
            }
            val moveX = event.clientX
            val moveY = event.clientY
            // If movement exceeds tolerance, start pan
            if ((abs(moveX - startX) > moveTolerance || abs(moveY - startY) > moveTolerance) && !isPaning) {
                isPaning = true
                event.state = EVENT_STATE_START
                onPan(event)
                event.preventDefault()
            }
            if (isPaning) {
                event.state = EVENT_STATE_MOVE
                onPan(event)
            }
        }

        fun handleMouseUp(event: MouseEvent) {
            if (isPaning) {
                event.state = EVENT_STATE_END
                onPan(event)
                // Marks the current element as having triggered a pan or long-press event.
                element.asDynamic().panOrLongPressTriggered = true
            }
            isPaning = false
            canPan = false
            isMouseDown = false
        }
    }

    companion object {
        // Default configuration
        private const val DEFAULT_DOUBLE_TAP_DELAY = 300 // Double tap interval time (milliseconds)
        private const val DEFAULT_LONG_PRESS_DELAY = 700 // Long press trigger time (milliseconds)
        private const val DEFAULT_MOVE_TOLERANCE = 10 // Movement tolerance (pixels)

        // longPress/pan event state
        private const val EVENT_STATE_START = "start"
        private const val EVENT_STATE_MOVE = "move"
        private const val EVENT_STATE_END = "end"
    }
}

/**
 * h5 common event
 */
data class H5Event(
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
 * h5 event processor
 */
object EventProcessor : IEventProcessor {
    /**
     * process event callback
     */
    private fun handleEventCallback(event: Event, callback: (event: IEvent?) -> Unit) {
        if (event.isTouchEventOrNull() != null) {
            // touchend 时 touches 为空
            val touch = if (event.type == "touchend") {
                event.unsafeCast<TouchEvent>().changedTouches[0]
            } else {
                event.unsafeCast<TouchEvent>().touches[0]
            }
            touch?.let {
                val touchEvent = H5Event(
                    screenX = touch.screenX,
                    screenY = touch.screenY,
                    clientX = touch.clientX,
                    clientY = touch.clientY,
                    offsetX = event.touchOffsetX ?: touch.clientX,
                    offsetY = event.touchOffsetY ?: touch.clientY,
                    pageX = touch.pageX,
                    pageY = touch.pageY
                )
                touchEvent.state = event.state
                callback(touchEvent)
            }
        } else if (event is MouseEvent) {
            val mouse = H5Event(
                screenX = event.screenX,
                screenY = event.screenY,
                clientX = event.clientX,
                clientY = event.clientY,
                offsetX = event.offsetX.toInt(),
                offsetY = event.offsetY.toInt(),
                pageX = event.pageX.toInt(),
                pageY = event.pageY.toInt(),
            )
            mouse.state = event.state
            callback(mouse)
        }
    }

    /**
     * simulate double click event for h5 touch event
     */
    override fun doubleClick(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        // Simulate double tap event for h5
        TouchEventHandlers.DoubleTapHandler(
            element = ele, onDoubleTap = { event: Event ->
                handleEventCallback(event, callback)
            })
    }

    /**
     * simulate long press event for h5 touch event
     */
    override fun longPress(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        // Simulate longPress tap event for h5
        TouchEventHandlers.LongPressHandler(
            element = ele.unsafeCast<HTMLElement>(), onLongPress = { event ->
                handleEventCallback(event, callback)
            })
    }

    /**
     * simulate pan event for h5 touch event
     */
    override fun pan(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        // Simulate pan tap event for h5
        TouchEventHandlers.PanHandler(
            element = ele.unsafeCast<HTMLElement>(), onPan = { event ->
                handleEventCallback(event, callback)
            })
    }
}