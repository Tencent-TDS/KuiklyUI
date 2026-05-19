package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.ktx.splitCanvasColorDefinitions
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import kotlin.math.tan

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
            val resolved = resolveGradientStyle(value)
            canvasContext?.setStrokeStyle(resolved)
        }

    /**
     * Set drawing style
     */
    @JsName("fillStyle")
    var fillStyle: dynamic
        get() = canvasContext?.fillStyle
        set(value) {
            val resolved = resolveGradientStyle(value)
            canvasContext?.setFillStyle(resolved)
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
     * Set canvas font (CSS font shorthand string).
     * The legacy mini-program canvas API only exposes setFontSize; we extract the px size
     * from the shorthand and apply it for best-effort compatibility.
     */
    @JsName("font")
    var font: dynamic
        get() = canvasContext?.font
        set(value) {
            val str = value?.toString().orEmpty()
            // Try to extract a size token like "12px" / "14.5px"
            val match = Regex("(\\d+(?:\\.\\d+)?)px").find(str)
            val size = match?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            if (size != null) {
                canvasContext?.setFontSize(size)
            }
        }

    /**
     * Set canvas textAlign.
     */
    @JsName("textAlign")
    var textAlign: dynamic
        get() = canvasContext?.textAlign
        set(value) {
            canvasContext?.setTextAlign(value)
        }

    /**
     * Set canvas globalAlpha.
     */
    @JsName("globalAlpha")
    var globalAlpha: dynamic
        get() = canvasContext?.globalAlpha
        set(value) {
            canvasContext?.setGlobalAlpha(value)
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
     * Resolve a style value: if it is a string starting with "linear-gradient",
     * convert it to a CanvasGradient object using the embedded JSON params.
     * Otherwise the value is returned unchanged.
     */
    private fun resolveGradientStyle(value: dynamic): dynamic {
        // Only handle string values; non-string and null values are returned as-is
        val isString = js("typeof value === 'string'") as Boolean
        if (!isString) {
            return value
        }
        val str: String = value.unsafeCast<String>()
        val prefix = "linear-gradient"
        if (!str.startsWith(prefix)) {
            return str
        }
        val gradient = parseLinearGradient(str.substring(prefix.length))
        return gradient ?: str
    }

    private fun parseLinearGradient(jsonStr: String): dynamic {
        val ctx = canvasContext ?: return null
        return try {
            // Use JSON.parse to keep this lightweight
            val obj = js("JSON.parse")(jsonStr)
            val x0 = (obj.x0 as? Number)?.toDouble() ?: 0.0
            val y0 = (obj.y0 as? Number)?.toDouble() ?: 0.0
            val x1 = (obj.x1 as? Number)?.toDouble() ?: 0.0
            val y1 = (obj.y1 as? Number)?.toDouble() ?: 0.0
            val colorStops = (obj.colorStops as? String).orEmpty()
            val gradient = ctx.createLinearGradient(x0, y0, x1, y1)
            if (colorStops.isNotEmpty()) {
                splitCanvasColorDefinitions(colorStops).forEach { item ->
                    val parts = item.split(" ")
                    if (parts.size >= 2) {
                        gradient.addColorStop(parts[1].toDouble(), parts[0].toRgbColor())
                    }
                }
            }
            gradient
        } catch (e: Throwable) {
            null
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
        canvasContext?.setFillStyle(resolveGradientStyle(style))
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
     * Create radial gradient.
     * The legacy mini-program canvas only exposes createCircularGradient(x, y, r), so we
     * approximate with the destination circle. To stay aligned with iOS behavior the
     * gradient is immediately painted across the whole canvas.
     */
    @JsName("createRadialGradient")
    fun createRadialGradient(
        x0: Double, y0: Double, r0: Double,
        x1: Double, y1: Double, r1: Double
    ): dynamic {
        val ctx = canvasContext ?: return null
        // Prefer the new-style API if available, otherwise fall back to circular gradient
        val hasNewApi = js("typeof ctx.createRadialGradient === 'function'") as Boolean
        return if (hasNewApi) {
            ctx.createRadialGradient(x0, y0, r0, x1, y1, r1)
        } else {
            ctx.createCircularGradient(x1, y1, r1)
        }
    }

    /**
     * set dash line
     */
    @JsName("setLineDash")
    fun setLineDash(segments: Array<Double>) =
        canvasContext?.setLineDash(segments)

    /**
     * Save current drawing state to the stack.
     */
    @JsName("save")
    fun save() {
        canvasContext?.save()
    }

    /**
     * Restore previously saved drawing state.
     */
    @JsName("restore")
    fun restore() {
        canvasContext?.restore()
    }

    /**
     * Translate the canvas origin.
     */
    @JsName("translate")
    fun translate(x: Double, y: Double) {
        canvasContext?.translate(x, y)
    }

    /**
     * Scale the canvas.
     */
    @JsName("scale")
    fun scale(x: Double, y: Double) {
        canvasContext?.scale(x, y)
    }

    /**
     * Rotate the canvas (radians).
     */
    @JsName("rotate")
    fun rotate(angle: Double) {
        canvasContext?.rotate(angle)
    }

    /**
     * Apply an affine transform on top of the existing one.
     */
    @JsName("transform")
    fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
        canvasContext?.transform(a, b, c, d, e, f)
    }

    /**
     * Replace the current transform with the given affine matrix.
     */
    @JsName("setTransform")
    fun setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
        canvasContext?.setTransform(a, b, c, d, e, f)
    }

    /**
     * Skew is not natively supported by mini-program canvas, simulate via transform.
     */
    @JsName("skew")
    fun skew(x: Double, y: Double) {
        canvasContext?.transform(1.0, tan(y), tan(x), 1.0, 0.0, 0.0)
    }

    /**
     * Draw an image. Mini-program canvas accepts an image URL/path string instead of an
     * HTMLImageElement, so we expect the cache to store a string keyed by cacheKey.
     *
     * Implemented via raw JS dispatch so callers can use the W3C-style
     * 3 / 5 / 9 argument forms transparently.
     */
    @JsName("drawImage")
    fun drawImage(image: dynamic, dx: dynamic, dy: dynamic, a: dynamic, b: dynamic, c: dynamic, d: dynamic, e: dynamic, f: dynamic) {
        val ctx = canvasContext ?: return
        val hasF = js("typeof f !== 'undefined'") as Boolean
        val hasB = js("typeof b !== 'undefined'") as Boolean
        when {
            // 9-arg: image, sx, sy, sWidth, sHeight, dx, dy, dWidth, dHeight
            hasF -> ctx.drawImage(image, dx, dy, a, b, c, d, e, f)
            // 5-arg: image, dx, dy, dWidth, dHeight
            hasB -> ctx.drawImage(image, dx, dy, a, b)
            // 3-arg: image, dx, dy
            else -> ctx.drawImage(image, dx, dy)
        }
        draw()
    }

    /**
     * Set the canvas font size directly. Used by font() setter helper.
     */
    @JsName("setFontSize")
    fun setFontSize(size: Double) {
        canvasContext?.setFontSize(size)
    }

    /**
     * Set the canvas text align mode directly.
     */
    @JsName("setTextAlign")
    fun setTextAlign(align: String) {
        canvasContext?.setTextAlign(align)
    }

    /**
     * Set the canvas global alpha.
     */
    @JsName("setGlobalAlpha")
    fun setGlobalAlpha(alpha: Double) {
        canvasContext?.setGlobalAlpha(alpha)
    }
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
