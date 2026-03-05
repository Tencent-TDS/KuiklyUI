package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.collection.fastMutableMapOf
import com.tencent.kuikly.core.render.web.const.KRActionConst
import com.tencent.kuikly.core.render.web.const.KRAttrConst
import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.const.KREventConst
import com.tencent.kuikly.core.render.web.const.KRParamConst
import com.tencent.kuikly.core.render.web.const.KRStateConst
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.processor.IEvent
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.processor.state
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.utils.DeviceType
import com.tencent.kuikly.core.render.web.utils.DeviceUtils
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Touch
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.js.json

/**
 * Convert Touch parameters to specified format
 */
fun getTouchParams(params: Touch?): MutableMap<String, Any> {
    val touchX = params?.clientX?.toFloat() ?: 0f
    val touchY = params?.clientY?.toFloat() ?: 0f
    val pageX = params?.pageX?.toFloat() ?: 0f
    val pageY = params?.pageY?.toFloat() ?: 0f

    return fastMutableMapOf<String, Any>().apply {
        fastMap = json(
            KRParamConst.X to touchX,
            KRParamConst.Y to touchY,
            KRParamConst.PAGE_X to pageX,
            KRParamConst.PAGE_Y to pageY,
        )
    }
}

/**
 * Convert Mouse parameters to specified format
 */
fun getMouseParams(event: MouseEvent): MutableMap<String, Any> {
    val mouseX = event.clientX.toFloat()
    val mouseY = event.clientY.toFloat()
    val pageX = event.pageX.toFloat()
    val pageY = event.pageY.toFloat()

    return fastMutableMapOf<String, Any>().apply {
        fastMap = json(
            KRParamConst.X to mouseX,
            KRParamConst.Y to mouseY,
            KRParamConst.PAGE_X to pageX,
            KRParamConst.PAGE_Y to pageY,
        )
    }
}

/**
 * Extension for TouchEvent, format Pan event parameters
 */
fun TouchEvent.toPanEventParams(): Map<String, Any> {
    val event: TouchEvent = this
    // Get specific values of touch parameters
    return getTouchParams(event.changedTouches[0])
}

/**
 * Extension for MouseEvent, format Pan event parameters
 */
fun MouseEvent.toPanEventParams(): Map<String, Any> {
    return getMouseParams(this)
}

/**
 * KRView, corresponding to Kuikly View
 */
open class KRView : IKuiklyRenderViewExport {
    // div instance
    private val div = kuiklyDocument.createElement(ElementType.DIV)
    // Whether touch event binding has been completed
    private var isBindTouchEvent = false
    // Whether mouse is currently pressed (for PC browser support)
    private var isMouseDown = false
    // Current device type (detected once and cached)
    private val deviceType: DeviceType by lazy { DeviceUtils.detectDeviceType() }
    // Pan event callback
    private var panEventCallback: KuiklyRenderCallback? = null
    // Touch start event callback
    private var touchDownEventCallback: KuiklyRenderCallback? = null
    // Touch move event callback
    private var touchMoveEventCallback: KuiklyRenderCallback? = null
    // Touch end event callback
    private var touchUpEventCallback: KuiklyRenderCallback? = null
    // Screen frame rate change callback
    private var screenFrameCallback: KuiklyRenderCallback? = null
    // Whether screen frame rate change event is paused
    private var screenFramePause: Boolean = false
    // Current existing frame rate binding
    private var requestId: Int = 0
    // Element actual distance from left side of page
    private var eleX = 0f
    // Element actual distance from top of page
    private var eleY = 0f
    // Slide start position
    private var x = 0f
    // Slide end position
    private var y = 0f
    // Slide distance from start position of page
    private var pageX = 0f
    // Slide distance from end position of page
    private var pageY = 0f
    // border width ratio with width and height, too close means used as border
    private val borderWithSizeRatio = BORDER_SIZE_RATIO
    private var superTouch: Boolean = false
    private var superTouchCanceled: Boolean = false
    // Window mouse up event listener reference for cleanup
    private var windowMouseUpListener: ((Event) -> Unit)? = null

    override val ele: HTMLDivElement
        get() = div.unsafeCast<HTMLDivElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            KRCssConst.PAN -> {
                // Handle drag end event
                panEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                // Bind touch event
                setTouchEvent()
                true
            }

            KRCssConst.SUPER_TOUCH -> {
                superTouch = propValue as Boolean
                true
            }

            KRCssConst.TOUCH_DOWN -> {
                // Handle touch start event
                touchDownEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                // Bind touch event
                setTouchEvent()
                true
            }

            KRCssConst.TOUCH_MOVE -> {
                // Handle touch move event
                touchMoveEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                // Bind touch event
                setTouchEvent()
                true
            }

            KRCssConst.TOUCH_UP -> {
                // Handle touch end event
                touchUpEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                // Bind touch event
                setTouchEvent()
                true
            }

            KRCssConst.DOUBLE_CLICK -> {
                KuiklyProcessor.eventProcessor.doubleClick(ele) { event: IEvent? ->
                    event?.let {
                        propValue.unsafeCast<KuiklyRenderCallback>().invoke(
                            mapOf(
                                KRParamConst.X to it.clientX.toFloat(),
                                KRParamConst.Y to it.clientY.toFloat()
                            )
                        )
                    }
                }
                true
            }

            KRCssConst.LONG_PRESS -> {
                KuiklyProcessor.eventProcessor.longPress(ele) { event: IEvent? ->
                    event?.let {
                        propValue.unsafeCast<KuiklyRenderCallback>().invoke(
                            mapOf(
                                KRParamConst.X to it.clientX.toFloat(),
                                KRParamConst.Y to it.clientY.toFloat(),
                                KRParamConst.STATE to it.state
                            )
                        )
                    }
                }
                true
            }

            EVENT_SCREEN_FRAME -> {
                // Screen frame rate change event, similar to JS requestAnimationFrame capability
                setScreenFrameEvent(propValue as? KuiklyRenderCallback)
                true
            }

            SCREEN_FRAME_PAUSE -> {
                // Pause screen frame rate change event
                setScreenFramePause(propValue)
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun setSuperTouchEventParams(
        params: FastMutableMap<String, Any>,
        timestamp: Long,
        action: String
    ): FastMutableMap<String, Any> {
        if (superTouch) {
            val touch = mapOf(
                KRParamConst.X to params[KRParamConst.X],
                KRParamConst.Y to params[KRParamConst.Y],
                KRParamConst.PAGE_X to params[KRParamConst.PAGE_X],
                KRParamConst.PAGE_Y to params[KRParamConst.PAGE_Y],
                KRParamConst.POINTER_ID to 0,
                KRParamConst.HASH to params[KRParamConst.X]
            )
            val touches = arrayListOf<Map<String, Any?>>()
            touches.add(touch)
            params[KRParamConst.POINTER_ID] = 0
            params[KRParamConst.TIMESTAMP] = timestamp
            params[KRParamConst.ACTION] = action
            params[KRParamConst.TOUCHES] = touches
            params[KRParamConst.CONSUMED] = 0
        }
        return params
    }

    /**
     * Bind touch event and mouse event based on device type
     */
    private fun setTouchEvent() {
        if (isBindTouchEvent) {
            return
        }
        isBindTouchEvent = true

        when (deviceType) {
            DeviceType.MOBILE -> bindTouchEvents()
            DeviceType.MINIPROGRAM -> bindTouchEvents()
            DeviceType.DESKTOP -> bindMouseEvents()
        }
    }

    /**
     * Bind touch events for mobile devices
     */
    private fun bindTouchEvents() {
        // Touch start
        ele.addEventListener(KREventConst.TOUCH_START, {
            // Get event parameters
            val eventParams = it.unsafeCast<TouchEvent>().toPanEventParams()
            // Calculate and save element position
            val position = ele.getBoundingClientRect()
            // Element distance from left side of page
            eleX = position.left.toFloat()
            // Element distance from top of page
            eleY = position.top.toFloat()

            var params = getPanEventParams(
                fastMutableMapOf<String, Any>().apply { putAll(eventParams) },
                KRStateConst.START
            )
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), KRActionConst.TOUCH_DOWN)
            panEventCallback?.invoke(params)
            touchDownEventCallback?.invoke(params)
            // stop event propagation
            it.stopPropagation()
        }, json(KRAttrConst.PASSIVE to true))

        // Touch move
        ele.addEventListener(KREventConst.TOUCH_MOVE, {
            val eventParams = it.unsafeCast<TouchEvent>().toPanEventParams()
            var params = getPanEventParams(
                fastMutableMapOf<String, Any>().apply { putAll(eventParams) },
                KRStateConst.MOVE
            )
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), KRActionConst.TOUCH_MOVE)
            panEventCallback?.invoke(params)
            touchMoveEventCallback?.invoke(params)
            // stop event propagation
            it.stopPropagation()
        }, json(KRAttrConst.PASSIVE to true))

        // Touch end
        ele.addEventListener(KREventConst.TOUCH_END, {
            var params = fastMutableMapOf<String, Any>().apply {
                put(KRParamConst.X, x)
                put(KRParamConst.Y, y)
                put(KRParamConst.STATE, KRStateConst.END)
                put(KRParamConst.PAGE_X, pageX)
                put(KRParamConst.PAGE_Y, pageY)
            }
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), KRActionConst.TOUCH_UP)
            // Touch end event has no position parameters, so use move recorded cache parameter value callback
            panEventCallback?.invoke(params)
            touchUpEventCallback?.invoke(params)
            // stop event propagation
            it.stopPropagation()
        }, json(KRAttrConst.PASSIVE to true))

        // Touch cancel
        ele.addEventListener(KREventConst.TOUCH_CANCEL, {
            var params = fastMutableMapOf<String, Any>().apply {
                put(KRParamConst.X, x)
                put(KRParamConst.Y, y)
                put(KRParamConst.PAGE_X, pageX)
                put(KRParamConst.PAGE_Y, pageY)
                put(KRParamConst.STATE, KRStateConst.CANCEL)
            }
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), KRActionConst.TOUCH_CANCEL)
            touchUpEventCallback?.invoke(params)
            it.stopPropagation()
        }, json(KRAttrConst.PASSIVE to true))
    }

    /**
     * Bind mouse events for PC browsers
     */
    private fun bindMouseEvents() {
        // Mouse down
        ele.addEventListener(KREventConst.MOUSE_DOWN, {
            isMouseDown = true
            // Get event parameters
            val eventParams = it.unsafeCast<MouseEvent>().toPanEventParams()
            // Calculate and save element position
            val position = ele.getBoundingClientRect()
            // Element distance from left side of page
            eleX = position.left.toFloat()
            // Element distance from top of page
            eleY = position.top.toFloat()

            var params = getPanEventParams(
                fastMutableMapOf<String, Any>().apply { putAll(eventParams) },
                KRStateConst.START
            )
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), KRActionConst.TOUCH_DOWN)
            panEventCallback?.invoke(params)
            touchDownEventCallback?.invoke(params)
            // stop event propagation
            it.stopPropagation()
        })

        // Mouse move
        ele.addEventListener(KREventConst.MOUSE_MOVE, {
            // Only trigger if mouse is down (dragging)
            if (isMouseDown) {
                val eventParams = it.unsafeCast<MouseEvent>().toPanEventParams()
                var params = getPanEventParams(
                    fastMutableMapOf<String, Any>().apply { putAll(eventParams) },
                    KRStateConst.MOVE
                )
                params = setSuperTouchEventParams(params, it.timeStamp.toLong(), KRActionConst.TOUCH_MOVE)
                panEventCallback?.invoke(params)
                touchMoveEventCallback?.invoke(params)
                // stop event propagation
                it.stopPropagation()
            }
        })

        // Mouse up
        ele.addEventListener(KREventConst.MOUSE_UP, {
            if (isMouseDown) {
                isMouseDown = false
                var params = fastMutableMapOf<String, Any>().apply {
                    put(KRParamConst.X, x)
                    put(KRParamConst.Y, y)
                    put(KRParamConst.STATE, KRStateConst.END)
                    put(KRParamConst.PAGE_X, pageX)
                    put(KRParamConst.PAGE_Y, pageY)
                }
                params = setSuperTouchEventParams(params, it.timeStamp.toLong(), KRActionConst.TOUCH_UP)
                // Mouse up event has no position parameters, so use move recorded cache parameter value callback
                panEventCallback?.invoke(params)
                touchUpEventCallback?.invoke(params)
                // stop event propagation
                it.stopPropagation()
            }
        })

        // Mouse leave (equivalent to touchcancel for mouse)
        ele.addEventListener(KREventConst.MOUSE_LEAVE, {
            if (isMouseDown) {
                isMouseDown = false
                var params = fastMutableMapOf<String, Any>().apply {
                    put(KRParamConst.X, x)
                    put(KRParamConst.Y, y)
                    put(KRParamConst.PAGE_X, pageX)
                    put(KRParamConst.PAGE_Y, pageY)
                    put(KRParamConst.STATE, KRStateConst.CANCEL)
                }
                params = setSuperTouchEventParams(params, it.timeStamp.toLong(), KRActionConst.TOUCH_CANCEL)
                touchUpEventCallback?.invoke(params)
                it.stopPropagation()
            }
        })

        // Add global mouse event listeners to handle mouse release outside of element
        // Save reference for cleanup in onDestroy
        windowMouseUpListener = { event: Event ->
            if (isMouseDown) {
                isMouseDown = false
                var params = fastMutableMapOf<String, Any>().apply {
                    put(KRParamConst.X, x)
                    put(KRParamConst.Y, y)
                    put(KRParamConst.STATE, KRStateConst.END)
                    put(KRParamConst.PAGE_X, pageX)
                    put(KRParamConst.PAGE_Y, pageY)
                }
                params = setSuperTouchEventParams(params, event.timeStamp.toLong(), KRActionConst.TOUCH_UP)
                panEventCallback?.invoke(params)
                touchUpEventCallback?.invoke(params)
            }
        }
        kuiklyWindow.addEventListener(KREventConst.MOUSE_UP, windowMouseUpListener)
    }

    /**
     * Get pan event corresponding parameter map
     */
    private fun getPanEventParams(
        eventParams: FastMutableMap<String, Any>,
        state: String
    ): FastMutableMap<String, Any> {
        // Get the actual position of the element, the left and top distances need to be
        // subtracted from the element distance from the page top and left
        eventParams[KRParamConst.X] = eventParams[KRParamConst.X].unsafeCast<Float>() - eleX
        eventParams[KRParamConst.Y] = eventParams[KRParamConst.Y].unsafeCast<Float>() - eleY
        // Save current movement distance
        x = eventParams[KRParamConst.X].unsafeCast<Float>()
        y = eventParams[KRParamConst.Y].unsafeCast<Float>()
        // Save current Page position
        pageX = eventParams[KRParamConst.PAGE_X].unsafeCast<Float>()
        pageY = eventParams[KRParamConst.PAGE_Y].unsafeCast<Float>()
        // Current drag state
        eventParams[KRParamConst.STATE] = state

        return eventParams
    }

    /**
     * Pause screen frame rate change event
     */
    private fun setScreenFramePause(propValue: Any) {
        val result = propValue == 1
        if (result != screenFramePause) {
            screenFramePause = result
            if (screenFramePause) {
                screenFrameCallback?.also {
                    // Pause current frame rate event
                    kuiklyWindow.clearTimeout(requestId)
                }
            } else {
                // Restore execution
                screenFrameCallback?.also {
                    executeScreenFrameCallback(screenFrameCallback)
                }
            }
        }
    }

    /**
     * Set screen frame rate callback
     */
    private fun setScreenFrameEvent(callback: KuiklyRenderCallback?) {
        screenFrameCallback?.also {
            // First remove the currently bound callback
            kuiklyWindow.clearTimeout(requestId)
        }

        if (callback != null) {
            screenFrameCallback = KuiklyRenderCallback {
                callback.invoke(null)
                // Continue callback requestAnimationFrame
                executeScreenFrameCallback(screenFrameCallback)
            }
            if (!screenFramePause) {
                executeScreenFrameCallback(screenFrameCallback)
            }
        }
    }


    /**
     * Execute frame rate change callback
     */
    private fun executeScreenFrameCallback(callback: KuiklyRenderCallback?) {
        requestId = kuiklyWindow.setTimeout({
            // Execute frame rate change callback
            callback?.invoke(null)
        }, SCREEN_FRAME_REFRESH_TIME)
    }

    /**
     * Clean up resources when view is destroyed to prevent memory leaks
     */
    override fun onDestroy() {
        super.onDestroy()
        
        // Remove global window event listener (must be removed to prevent memory leak)
        windowMouseUpListener?.let {
            kuiklyWindow.removeEventListener(KREventConst.MOUSE_UP, it)
        }
        windowMouseUpListener = null
        
        // Clear screen frame timer
        if (requestId != 0) {
            kuiklyWindow.clearTimeout(requestId)
            requestId = 0
        }
    }

    companion object {
        const val VIEW_NAME = "KRView"
        private const val EVENT_SCREEN_FRAME = "screenFrame"
        private const val SCREEN_FRAME_PAUSE = "screenFramePause"
        // Refresh rate interval, 16ms (approximately 60fps)
        private const val SCREEN_FRAME_REFRESH_TIME = 16
        // Border size ratio threshold
        private const val BORDER_SIZE_RATIO = 5
    }
}
