package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.const.KRViewConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.setFrame
import com.tencent.kuikly.core.render.web.ktx.splitCanvasColorDefinitions
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.BUTT
import org.w3c.dom.CanvasGradient
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ROUND
import org.w3c.dom.SQUARE

/**
 * Kuikly canvas view, corresponding to web's canvas
 */
class KRCanvasView : IKuiklyRenderViewExport {
    /**
     * canvas object
     */
    private val canvas = kuiklyDocument.createElement(ElementType.CANVAS)
    /**
     * real canvas context instance
     */
    private var _context: CanvasRenderingContext2D? = null

    /**
     * get canvas context
     */
    private val canvasContext: CanvasRenderingContext2D?
        get() = if (_context != null) {
            _context
        } else {
            _context = ele.getContext("2d").unsafeCast<CanvasRenderingContext2D?>()
            _context
        }


    /**
     * canvas element
     */
    override val ele: HTMLCanvasElement
        get() = canvas.unsafeCast<HTMLCanvasElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            KRCssConst.FRAME -> {
                // Set canvas size, need to set canvas drawing area size simultaneously
                val frame = propValue.unsafeCast<Frame>()
                // First set canvas size
                ele.setFrame(frame, ele.style)
                // Then set drawing area size
                ele.width = frame.width.toInt()
                ele.height = frame.height.toInt()
                // If setting size, notify that size information has changed
                onFrameChange(frame)
                true
            }
            // Other props use unified setting
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            BEGIN_PATH -> canvasContext?.beginPath()
            MOVE_TO -> moveTo(params)
            LINE_TO -> lineTo(params)
            ARC -> arc(params)
            CLOSE_PATH -> canvasContext?.closePath()
            STROKE -> canvasContext?.stroke()
            STROKE_STYLE -> setStrokeStyle(params)
            STROKE_TEXT -> setStrokeText(params)
            FILL -> canvasContext?.fill()
            FILL_TEXT -> setFillText(params)
            FILL_STYLE -> setFillStyle(params)
            LINE_WIDTH -> setLineWidth(params)
            LINE_CAP -> setLineCap(params)
            LINE_DASH -> setLineDash(params)
            // Linear gradient is handled by core layer, will be set during fillStyle and strokeStyle,
            // empty method kept here
            CREATE_LINEAR_GRADIENT -> {}
            QUADRATIC_CURVE_TO -> quadraticCurveTo(params)
            BEZIER_CURVE_TO -> bezierCurveTo(params)
            RESET -> reset()
            CLIP -> canvasContext?.clip()
            else -> super.call(method, params, callback)
        }
    }

    /**
     * move to
     */
    private fun moveTo(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        val x = paramsJSON.optDouble(KRViewConst.X)
        val y = paramsJSON.optDouble(KRViewConst.Y)
        canvasContext?.moveTo(x, y)
    }

    /**
     * line to
     */
    private fun lineTo(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        val x = paramsJSON.optDouble(KRViewConst.X)
        val y = paramsJSON.optDouble(KRViewConst.Y)
        canvasContext?.lineTo(x, y)
    }

    /**
     * draw arc path
     */
    private fun arc(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        val cx = paramsJSON.optDouble(KRViewConst.X)
        val cy = paramsJSON.optDouble(KRViewConst.Y)
        val radius = paramsJSON.optDouble(RADIUS)
        val startAngle = paramsJSON.optDouble(START_ANGLE)
        val endAngle = paramsJSON.optDouble(END_ANGLE)
        val counterclockwise = paramsJSON.optInt(COUNTER_CLOCKWISE) == TYPE_COUNTER_CLOCKWISE
        canvasContext?.arc(cx, cy, radius, startAngle, endAngle, counterclockwise)
    }

    /**
     * set stroke style
     */
    private fun setStrokeStyle(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        val style = paramsJSON.optString(STYLE)
        canvasContext?.strokeStyle = tryParseGradient(style) ?: style.toRgbColor()
    }

    /**
     * set stroke text
     */
    private fun setStrokeText(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        canvasContext?.strokeText(
            paramsJSON.optString(TEXT),
            paramsJSON.optDouble(KRViewConst.X),
            paramsJSON.optDouble(KRViewConst.Y))
    }

    /**
     * set fill style
     */
    private fun setFillStyle(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        val style = paramsJSON.optString(STYLE)
        canvasContext?.fillStyle = tryParseGradient(style) ?: style.toRgbColor()
    }

    /**
     * set fill text
     */
    private fun setFillText(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        canvasContext?.fillText(
            paramsJSON.optString(TEXT),
            paramsJSON.optDouble(KRViewConst.X),
            paramsJSON.optDouble(KRViewConst.Y))
    }

    /**
     * set line width
     */
    private fun setLineWidth(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        canvasContext?.lineWidth = paramsJSON.optDouble(KRViewConst.WIDTH)
    }

    /**
     * Create canvas linear gradient
     */
    private fun createLinearGradient(params: String?): CanvasGradient? {
        val paramsJSON = params.toJSONObjectSafely()
        val leftX = paramsJSON.optDouble("x0")
        val leftY = paramsJSON.optDouble("y0")
        val rightX = paramsJSON.optDouble("x1")
        val rightY = paramsJSON.optDouble("y1")
        // Since the color is in rgba format, like "rgba(255,255,0,1) 0,rgba(255,0,0,1) 1",
        // we need to process it separately
        val colorStops = paramsJSON.optString("colorStops")
        val colors = splitCanvasColorDefinitions(colorStops)
        val gradient = canvasContext?.createLinearGradient(leftX, leftY, rightX, rightY)
        colors.forEach { item ->
            val colorAndPosition = item.split(" ")
            gradient?.addColorStop(colorAndPosition[1].toDouble(), colorAndPosition[0].toRgbColor())
        }

        return gradient
    }

    /**
     * handle gradient
     */
    private fun tryParseGradient(style: String): CanvasGradient? {
        val gradientPrefix = "linear-gradient"
        return if (style.startsWith(gradientPrefix)) {
            createLinearGradient(style.substring(gradientPrefix.length))
        } else {
            null
        }
    }

    /**
     * set line cap
     */
    private fun setLineCap(params: String?) {
        val paramsJSON = params.toJSONObjectSafely()
        canvasContext?.lineCap = when (paramsJSON.optString(STYLE)) {
            "butt" -> CanvasLineCap.BUTT
            "round" -> CanvasLineCap.ROUND
            else -> CanvasLineCap.SQUARE
        }
    }

    /**
     * set line dash
     */
    private fun setLineDash(params: String?) {
        val json = params.toJSONObjectSafely()
        val jsonArray = json.optJSONArray("intervals")
        if (jsonArray == null) {
            // no segments, clear dash
            canvasContext?.setLineDash(arrayOf())
        } else {
            val intervals: Array<Double> = arrayOf()
            for (i in 0 until jsonArray.length()) {
                intervals[i] = jsonArray.optDouble(i)
            }
            canvasContext?.setLineDash(intervals)
        }
    }

    /**
     * draw quadratic curve path
     */
    private fun quadraticCurveTo(params: String?) {
        val json = params.toJSONObjectSafely()
        val cpx = json.optDouble("cpx")
        val cpy = json.optDouble("cpy")
        val x = json.optDouble(KRViewConst.X)
        val y = json.optDouble(KRViewConst.Y)
        canvasContext?.quadraticCurveTo(cpx, cpy, x, y)
    }


    /**
     * draw bezier curve path
     */
    private fun bezierCurveTo(params: String?) {
        val json = params.toJSONObjectSafely()
        val cp1x = json.optDouble("cp1x")
        val cp1y = json.optDouble("cp1y")
        val cp2x = json.optDouble("cp2x")
        val cp2y = json.optDouble("cp2y")
        val x = json.optDouble(KRViewConst.X)
        val y = json.optDouble(KRViewConst.Y)
        canvasContext?.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y)
    }

    /**
     * clear canvas
     */
    private fun reset() {
        // because canvasRenderingContext2D's reset method support degree too low,
        // so use clearRect to implement, clear the whole canvas
        canvasContext?.clearRect(0.0, 0.0, ele.width.toDouble(), ele.height.toDouble())
    }

    companion object {
        const val VIEW_NAME = "KRCanvasView"

        private const val BEGIN_PATH = "beginPath"
        private const val MOVE_TO = "moveTo"
        private const val LINE_TO = "lineTo"
        private const val ARC = "arc"
        private const val CLOSE_PATH = "closePath"
        private const val STROKE = "stroke"
        private const val STROKE_STYLE = "strokeStyle"
        private const val STROKE_TEXT = "strokeText"
        private const val FILL = "fill"
        private const val FILL_STYLE = "fillStyle"
        private const val FILL_TEXT = "fillText"
        private const val LINE_WIDTH = "lineWidth"
        private const val LINE_CAP = "lineCap"
        private const val LINE_DASH = "lineDash"
        private const val CLIP = "clip"
        private const val CREATE_LINEAR_GRADIENT = "createLinearGradient"
        private const val QUADRATIC_CURVE_TO = "quadraticCurveTo"
        private const val BEZIER_CURVE_TO = "bezierCurveTo"
        private const val RESET = "reset"
        private const val STYLE = "style"
        private const val TEXT = "text"

        private const val RADIUS = "r"
        private const val START_ANGLE = "sAngle"
        private const val END_ANGLE = "eAngle"
        private const val COUNTER_CLOCKWISE = "counterclockwise"
        private const val TYPE_COUNTER_CLOCKWISE = 1
    }
}
