package com.tencent.kuikly.core.render.web.runtime.web.expand.processor

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

            // Use mouse events for desktop devices as a fallback
            element.addEventListener("mousedown", { event ->
                event as MouseEvent
                if (event.button == 0.toShort()) { // Only handle left click
                    handleMouseTap(event)
                }
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
                    if (currentTime - lastTapTime < doubleTapDelay &&
                        abs(x - lastTapX) < moveTolerance &&
                        abs(y - lastTapY) < moveTolerance
                    ) {
                        // Double tap triggered
                        event.preventDefault()
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

        /**
         * Handle non-mobile tap event
         */
        private fun handleMouseTap(event: MouseEvent) {
            val currentTime = Date.now()
            val x = event.clientX
            val y = event.clientY

            if (currentTime - lastTapTime < doubleTapDelay &&
                abs(x - lastTapX) < moveTolerance &&
                abs(y - lastTapY) < moveTolerance
            ) {
                // Double tap triggered
                event.preventDefault()
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
                        if (abs(moveX - startX) > moveTolerance ||
                            abs(moveY - startY) > moveTolerance
                        ) {
                            cancelTimer()
                        }
                    }
                }
            })

            /**
             * Cancel listener
             */
            element.addEventListener("touchend", { _ ->
                cancelTimer()
            })
            /**
             * Cancel listener
             */
            element.addEventListener("touchcancel", { _ ->
                cancelTimer()
            })

            // Mouse events as fallback
            element.addEventListener("mousedown", { event ->
                event as MouseEvent
                if (event.button == 0.toShort()) { // Only handle left click
                    startX = event.clientX
                    startY = event.clientY
                    startTimer(event)
                }
            })

            element.addEventListener("mousemove", { event ->
                event as MouseEvent
                val moveX = event.clientX
                val moveY = event.clientY

                // If movement exceeds tolerance, cancel long press
                if (abs(moveX - startX) > moveTolerance ||
                    abs(moveY - startY) > moveTolerance
                ) {
                    cancelTimer()
                }
            })
            /**
             * Cancel listener
             */
            element.addEventListener("mouseup", { _ ->
                cancelTimer()
            })
            /**
             * Cancel listener
             */
            element.addEventListener("mouseleave", { _ ->
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
                // Prevent default event
                event.preventDefault()
                onLongPress(event)
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
        }
    }

    companion object {
        // Default configuration
        private const val DEFAULT_DOUBLE_TAP_DELAY = 300 // Double tap interval time (milliseconds)
        private const val DEFAULT_LONG_PRESS_DELAY = 700 // Long press trigger time (milliseconds)
        private const val DEFAULT_MOVE_TOLERANCE = 10 // Movement tolerance (pixels)
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
        if (event is TouchEvent) {
            val touch = event.touches[0]
            touch?.let {
                callback(
                    H5Event(
                    screenX = touch.screenX,
                    screenY = touch.screenY,
                    clientX = touch.clientX,
                    clientY = touch.clientY,
                    pageX = touch.pageX,
                    pageY = touch.pageY
                )
                )
            }
        } else if (event is MouseEvent) {
            callback(
                H5Event(
                screenX = event.screenX,
                screenY = event.screenY,
                clientX = event.clientX,
                clientY = event.clientY,
                pageX = event.pageX.toInt(),
                pageY = event.pageY.toInt()
            )
            )
        }
    }

    /**
     * simulate double click event for h5 touch event
     */
    override fun doubleClick(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        // Simulate double tap event for h5
        TouchEventHandlers.DoubleTapHandler(
            element = ele,
            onDoubleTap = { event: Event ->
                handleEventCallback(event, callback)
            }
        )
    }

    /**
     * simulate long press event for h5 touch event
     */
    override fun longPress(ele: HTMLElement, callback: (event: IEvent?) -> Unit) {
        // Simulate longPress tap event for h5
        TouchEventHandlers.LongPressHandler(
            element = ele.unsafeCast<HTMLElement>(),
            onLongPress = { event ->
                handleEventCallback(event, callback)
            }
        )
    }
}