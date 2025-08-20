package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.collection.fastMutableMapOf
import com.tencent.kuikly.core.render.web.processor.IEvent
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.utils.DeviceType
import com.tencent.kuikly.core.render.web.utils.DeviceUtils
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Touch
import org.w3c.dom.TouchEvent
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
            "x" to touchX,
            "y" to touchY,
            "pageX" to pageX,
            "pageY" to pageY,
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
            "x" to mouseX,
            "y" to mouseY,
            "pageX" to pageX,
            "pageY" to pageY,
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
    private val borderWithSizeRatio = 5
    private var superTouch: Boolean = false
    private var superTouchCanceled: Boolean = false

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
                                "x" to it.clientX.toFloat(),
                                "y" to it.clientY.toFloat()
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
                                "x" to it.clientX.toFloat(),
                                "y" to it.clientY.toFloat()
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

    private fun setSuperTouchEventParams(params: FastMutableMap<String, Any>, timestamp: Long, action: String): FastMutableMap<String, Any> {
        if (superTouch) {
            val touch = mapOf(
                "x" to params["x"],
                "y" to params["y"],
                "pageX" to params["pageX"],
                "pageY" to params["pageY"],
                "pointerId" to 0,
                "hash" to params["x"]
            )
            val touches = arrayListOf<Map<String, Any?>>()
            touches.add(touch)
            params["pointerId"] = 0
            params["timestamp"] = timestamp
            params["action"] = action
            params["touches"] = touches
            params["consumed"] = 0
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
        ele.addEventListener("touchstart", {
            // Get event parameters
            val eventParams = it.unsafeCast<TouchEvent>().toPanEventParams()
            // Calculate and save element position
            val position = ele.getBoundingClientRect()
            // Element distance from left side of page
            eleX = position.left.toFloat()
            // Element distance from top of page
            eleY = position.top.toFloat()

            var params = getPanEventParams(fastMutableMapOf<String, Any>().apply { putAll(eventParams) }, "start")
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchDown")
            panEventCallback?.invoke(params)
            touchDownEventCallback?.invoke(params)
            // stop event propagation
            it.stopPropagation()
        }, json("passive" to true))
        
        // Touch move
        ele.addEventListener("touchmove", {
            val eventParams = it.unsafeCast<TouchEvent>().toPanEventParams()
            var params = getPanEventParams(fastMutableMapOf<String, Any>().apply { putAll(eventParams) }, "move")
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchMove")
            panEventCallback?.invoke(params)
            touchMoveEventCallback?.invoke(params)
            // stop event propagation
            it.stopPropagation()
        }, json("passive" to true))
        
        // Touch end
        ele.addEventListener("touchend", {
            var params = fastMutableMapOf<String, Any>().apply {
                put("x", x)
                put("y", y)
                put("state", "end")
                put("pageX", pageX)
                put("pageY", pageY)
            }
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchUp")
            // Touch end event has no position parameters, so use move recorded cache parameter value callback
            panEventCallback?.invoke(params)
            touchUpEventCallback?.invoke(params)
            // stop event propagation
            it.stopPropagation()
        }, json("passive" to true))
        
        // Touch cancel
        ele.addEventListener("touchcancel", {
            var params = fastMutableMapOf<String, Any>().apply {
                put("x", x)
                put("y", y)
                put("pageX", pageX)
                put("pageY", pageY)
                put("state", "cancel")
            }
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchCancel")
            touchUpEventCallback?.invoke(params)
            it.stopPropagation()
        }, json("passive" to true))
    }

    /**
     * Bind mouse events for PC browsers
     */
    private fun bindMouseEvents() {
        // Mouse down
        ele.addEventListener("mousedown", {
            isMouseDown = true
            // Get event parameters
            val eventParams = it.unsafeCast<MouseEvent>().toPanEventParams()
            // Calculate and save element position
            val position = ele.getBoundingClientRect()
            // Element distance from left side of page
            eleX = position.left.toFloat()
            // Element distance from top of page
            eleY = position.top.toFloat()

            var params = getPanEventParams(fastMutableMapOf<String, Any>().apply { putAll(eventParams) }, "start")
            params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchDown")
            panEventCallback?.invoke(params)
            touchDownEventCallback?.invoke(params)
            // stop event propagation
            it.stopPropagation()
        })
        
        // Mouse move
        ele.addEventListener("mousemove", {
            // Only trigger if mouse is down (dragging)
            if (isMouseDown) {
                val eventParams = it.unsafeCast<MouseEvent>().toPanEventParams()
                var params = getPanEventParams(fastMutableMapOf<String, Any>().apply { putAll(eventParams) }, "move")
                params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchMove")
                panEventCallback?.invoke(params)
                touchMoveEventCallback?.invoke(params)
                // stop event propagation
                it.stopPropagation()
            }
        })
        
        // Mouse up
        ele.addEventListener("mouseup", {
            if (isMouseDown) {
                isMouseDown = false
                var params = fastMutableMapOf<String, Any>().apply {
                    put("x", x)
                    put("y", y)
                    put("state", "end")
                    put("pageX", pageX)
                    put("pageY", pageY)
                }
                params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchUp")
                // Mouse up event has no position parameters, so use move recorded cache parameter value callback
                panEventCallback?.invoke(params)
                touchUpEventCallback?.invoke(params)
                // stop event propagation
                it.stopPropagation()
            }
        })
        
        // Mouse leave (equivalent to touchcancel for mouse)
        ele.addEventListener("mouseleave", {
            if (isMouseDown) {
                isMouseDown = false
                var params = fastMutableMapOf<String, Any>().apply {
                    put("x", x)
                    put("y", y)
                    put("pageX", pageX)
                    put("pageY", pageY)
                    put("state", "cancel")
                }
                params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchCancel")
                touchUpEventCallback?.invoke(params)
                it.stopPropagation()
            }
        })

        // Add global mouse event listeners to handle mouse release outside of element
        kuiklyWindow.addEventListener("mouseup", {
            if (isMouseDown) {
                isMouseDown = false
                var params = fastMutableMapOf<String, Any>().apply {
                    put("x", x)
                    put("y", y)
                    put("state", "end")
                    put("pageX", pageX)
                    put("pageY", pageY)
                }
                params = setSuperTouchEventParams(params, it.timeStamp.toLong(), "touchUp")
                panEventCallback?.invoke(params)
                touchUpEventCallback?.invoke(params)
            }
        })
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
        eventParams["x"] = eventParams["x"].unsafeCast<Float>() - eleX
        eventParams["y"] = eventParams["y"].unsafeCast<Float>() - eleY
        // Save current movement distance
        x = eventParams["x"].unsafeCast<Float>()
        y = eventParams["y"].unsafeCast<Float>()
        // Save current Page position
        pageX = eventParams["pageX"].unsafeCast<Float>()
        pageY = eventParams["pageY"].unsafeCast<Float>()
        // Current drag state
        eventParams["state"] = state

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

    companion object {
        const val VIEW_NAME = "KRView"
        private const val EVENT_SCREEN_FRAME = "screenFrame"
        private const val SCREEN_FRAME_PAUSE = "screenFramePause"
        // Refresh rate interval, 16ms
        private const val SCREEN_FRAME_REFRESH_TIME = 16
    }
}
