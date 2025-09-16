package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi

/**
 * Mini program canvas context
 */
class MiniCanvasContext(private val canvas: MiniCanvasElement) {
    /**
     * Get canvas context
     */
    private val canvasContext: dynamic by lazy {
        NativeApi.createCanvasContext(canvas.id)
    }

    /**
     * Drawing promise for mini app
     */
    private var drawPromise: dynamic = null

    /**
     * Set drawing style
     */
    @JsName("strokeStyle")
    var strokeStyle: dynamic
        get() = canvasContext?.strokeStyle
        set(value) {
            canvasContext?.strokeStyle = value
        }

    /**
     * Set drawing style
     */
    @JsName("fillStyle")
    var fillStyle: dynamic
        get() = canvasContext?.fillStyle
        set(value) {
            canvasContext?.setFillStyle(value)
        }

    /**
     * Set line width
     */
    @JsName("lineWidth")
    var lineWidth: dynamic
        get() = canvasContext?.lineWidth
        set(value) {
            canvasContext?.setLineWidth(value)
        }

    /**
     * set line cap
     */
    @JsName("lineCap")
    var lineCap: dynamic
        get() = canvasContext?.lineCap
        set(value) {
            canvasContext?.setLineCap(value)
        }

    /**
     * Currently using the old version of canvas, draw needs to be called once after all operations are set
     */
    private fun draw() {
        if (drawPromise == null) {
            drawPromise = js("Promise").resolve().then {
                drawPromise = null
                canvasContext?.draw(true)
            }
        }
    }

    /**
     * Begin path
     */
    @JsName("beginPath")
    fun beginPath() {
        canvasContext?.beginPath()
    }

    /**
     * Move to
     */
    @JsName("moveTo")
    fun moveTo(x: Double, y: Double) {
        canvasContext?.moveTo(x, y)
    }

    /**
     * Line to
     */
    @JsName("lineTo")
    fun lineTo(x: Double, y: Double) {
        canvasContext?.lineTo(x, y)
    }

    /**
     * Draw arc path
     */
    @JsName("arc")
    fun arc(cx: Double, cy: Double, radius: Double, startAngle: Double, endAngle: Double, counterclockwise: Boolean) {
        canvasContext?.arc(cx, cy, radius, startAngle, endAngle, counterclockwise)
    }

    /**
     * Return to the starting point of the path
     */
    @JsName("closePath")
    fun closePath() {
        canvasContext?.closePath()
    }

    /**
     * Draw the shape
     */
    @JsName("stroke")
    fun stroke() {
        canvasContext?.stroke()
        draw()
    }

    /**
     * Set stroke content area text
     */
    @JsName("strokeText")
    fun strokeText(text: String, x: Double, y: Double) {
        canvasContext?.strokeText(text, x, y)
    }

    /**
     * Fill content to create solid shape
     */
    @JsName("fill")
    fun fill() {
        canvasContext?.fill()
        draw()
    }

    /**
     * Set fill content area style
     */
    fun setFillStyle(style: dynamic) {
        canvasContext?.setFillStyle(style)
    }

    /**
     * Set fill content area text
     */
    @JsName("fillText")
    fun fillText(text: String, x: Double, y: Double) {
        canvasContext?.fillText(text, x, y)
    }

    /**
     * Set line width
     */
    fun setLineWidth(lineWidth: Double) {
        canvasContext?.setLineWidth(lineWidth)
    }

    /**
     * Draw quadratic Bezier curve path
     */
    @JsName("quadraticCurveTo")
    fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) {
        canvasContext?.quadraticCurveTo(cpx, cpy, x, y)
    }


    /**
     * Draw cubic Bezier curve path
     */
    @JsName("bezierCurveTo")
    fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) {
        canvasContext?.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y)
    }

    /**
     * Set the current created path as current clipping path
     */
    @JsName("clip")
    fun clip() {
        canvasContext?.clip()
        draw()
    }

    /**
     * Clear canvas
     */
    @JsName("clearRect")
    fun clearRect(x: Double, y: Double, width: Double, height: Double) {
        // Currently the reset method of canvasRenderingContext2D has low support,
        // so use clearRect to clear the entire canvas
        canvasContext?.clearRect(x, y, width, height)
        canvasContext?.draw()
    }

    /**
     * Create linear gradient
     */
    @JsName("createLinearGradient")
    fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): dynamic =
        canvasContext?.createLinearGradient(x0, y0, x1, y1)

    /**
     * set dash line
     */
    @JsName("setLineDash")
    fun setLineDash(segments: Array<Double>) =
        canvasContext?.setLineDash(segments)
}

/**
 * Mini program canvas node, which will eventually be rendered as canvas in the mini program
 */
class MiniCanvasElement(
    nodeName: String = TransformConst.CANVAS,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    /**
     *  Mini program needs to specify a canvasId
     */
    private val canvasId: String
        get() = this.id

    /**
     * Canvas width
     */
    @JsName("width")
    var width: Int
        get() = getAttribute(WIDTH).unsafeCast<Int?>() ?: 0
        set(value) {
            setAttribute(WIDTH, value)
        }

    /**
     * Canvas height
     */
    @JsName("height")
    var height: Int
        get() = getAttribute(HEIGHT).unsafeCast<Int?>() ?: 0
        set(value) {
            setAttribute(HEIGHT, value)
        }

    /**
     * Get canvas context
     */
    private val canvasContext = MiniCanvasContext(this)

    /**
     *  Set the canvasId before converting data to the JSON required by mini program
     */
    override fun onTransformData(): String {
        setAttribute(CANVAS_ID, this.canvasId)
        return super.onTransformData()
    }

    /**
     * Get canvas context
     */
    @JsName("getContext")
    fun getContext(): dynamic = canvasContext

    companion object {
        private const val CANVAS_ID = "canvasId"
        private const val WIDTH = "width"
        private const val HEIGHT = "height"
    }
}
