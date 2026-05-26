package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.IKuiklyRenderContext
import com.tencent.kuikly.core.render.web.expand.module.KRMemoryCacheModule
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
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.utils.Log
import org.w3c.dom.BUTT
import org.w3c.dom.CanvasGradient
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ROUND
import org.w3c.dom.SQUARE
import kotlin.math.tan

/**
 * Kuikly canvas view, corresponding to web's canvas
 */
class KRCanvasView(
    override var kuiklyRenderContext: IKuiklyRenderContext? = null
) : IKuiklyRenderViewExport {
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
            CREATE_RADIAL_GRADIENT -> createRadialGradient(params)
            QUADRATIC_CURVE_TO -> quadraticCurveTo(params)
            BEZIER_CURVE_TO -> bezierCurveTo(params)
            RESET -> reset()
            CLIP -> clip(params)
            TEXT_ALIGN -> setTextAlign(params)
            FONT -> setFont(params)
            DRAW_IMAGE -> drawImage(params)
            SAVE -> canvasContext?.save()
            SAVE_LAYER -> saveLayer(params)
            RESTORE -> canvasContext?.restore()
            TRANSLATE -> translate(params)
            SCALE -> scale(params)
            ROTATE -> rotate(params)
            SKEW -> skew(params)
            TRANSFORM -> transform(params)
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
        canvasContext?.strokeStyle = resolveCanvasStyle(style)
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
        canvasContext?.fillStyle = resolveCanvasStyle(style)
    }

    /**
     * Resolve a canvas paint style. For solid colors, parse to RGB. For
     * `linear-gradient{...}` strings, prefer building a CanvasGradient now (works
     * synchronously on H5). When the underlying ctx is not yet ready (e.g. the mini
     * program Canvas 2D node is resolved asynchronously via selectorQuery), fall
     * back to passing the raw `linear-gradient{...}` string through — the platform
     * fillStyle / strokeStyle setter is expected to defer-parse it on its side.
     * Without this fallback, gradients would silently become solid black on first
     * paint in async-ctx environments.
     */
    private fun resolveCanvasStyle(style: String): dynamic {
        val gradient = tryParseGradient(style)
        if (gradient != null) return gradient
        if (style.startsWith("linear-gradient")) return style
        return style.toRgbColor()
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
            val intervals = Array(jsonArray.length()) { i -> jsonArray.optDouble(i) }
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

    /**
     * Clip current path. Web canvas only natively supports the intersect form;
     * the difference (clipPathDifference) variant is not supported and a warning is logged.
     */
    private fun clip(params: String?) {
        val intersect = if (params.isNullOrEmpty()) {
            true
        } else {
            params.toJSONObjectSafely().optInt("intersect", 1) == 1
        }
        if (!intersect) {
            // clipPathDifference is not implemented on Web/H5, downgrade to a no-op clip with warning
            Log.warn("KRCanvasView: clipPathDifference is not supported on web, fallback to no-op")
            return
        }
        canvasContext?.clip()
    }

    /**
     * set text align
     * The core layer sends the raw align value as the params string itself (not JSON).
     */
    private fun setTextAlign(params: String?) {
        val align = params.orEmpty()
        // textAlign on CanvasRenderingContext2D is a String typealias; assign directly via dynamic
        val ctx = canvasContext ?: return
        ctx.asDynamic().textAlign = when (align) {
            "left", "right", "center", "start", "end" -> align
            else -> "left"
        }
    }

    /**
     * set font
     */
    private fun setFont(params: String?) {
        val json = params.toJSONObjectSafely()
        val style = json.optString("style").ifEmpty { "normal" }
        val weight = json.optString("weight").ifEmpty { "normal" }
        val size = json.optDouble("size", 0.0)
        val family = json.optString("family").ifEmpty { "sans-serif" }
        if (size <= 0) {
            return
        }
        // CSS font shorthand: <style> <weight> <size>px <family>
        canvasContext?.font = "$style $weight ${size}px $family"
    }

    /**
     * draw image. The image must have already been cached by KRMemoryCacheModule#cacheImage.
     *
     * The cached value can either be a real HTMLImageElement (browser) or a plain
     * string path (mini-program). The underlying canvas context accepts both, so we
     * dispatch via the dynamic API to keep one code path.
     *
     * If the cached HTMLImageElement is still loading (typical right after a
     * cacheImage call for a remote / asynchronously-decoded resource), we register a
     * one-shot listener that re-issues the same drawImage once the bitmap is ready,
     * so the picture eventually lands on the canvas without losing previously
     * rendered content.
     */
    private fun drawImage(params: String?) {
        val ctx = canvasContext ?: return
        val json = params.toJSONObjectSafely()
        val cacheKey = json.optString("cacheKey")
        if (cacheKey.isEmpty()) {
            return
        }
        val cacheModule = kuiklyRenderContext?.module<KRMemoryCacheModule>(KRMemoryCacheModule.MODULE_NAME)
        val cached: Any = cacheModule?.get<Any>(cacheKey) ?: return

        // If the cached value is an image element that hasn't decoded yet, defer
        // the draw until onload fires. Until then naturalWidth is 0 and drawing
        // would either throw or render nothing.
        // NOTE: don't use `is HTMLImageElement` here because the `HTMLImageElement`
        // global doesn't exist in mini-program runtime (would throw ReferenceError).
        val cachedDyn: dynamic = cached
        val isPendingImage = js("typeof cached === 'object' && cached !== null && 'naturalWidth' in cached") as Boolean
            && (cachedDyn.naturalWidth as? Number)?.toInt() == 0
        if (isPendingImage) {
            val pending = cachedDyn
            // Chain previous onload (if any) so we don't drop the cache module's listener
            val previousOnload: dynamic = pending.onload
            pending.onload = {
                if (previousOnload != null && previousOnload != undefined) {
                    previousOnload()
                }
                // Replay this draw call once the image is finally ready
                performDrawImage(ctx, cached, json)
            }
            return
        }

        performDrawImage(ctx, cached, json)
    }

    /**
     * Cross-environment check for an image-like object that exposes naturalWidth/Height.
     * This avoids referencing `HTMLImageElement`, which doesn't exist in mini-program
     * runtime and would throw ReferenceError.
     */
    private fun isImageLike(value: Any): Boolean {
        val v: dynamic = value
        return js("typeof v === 'object' && v !== null && 'naturalWidth' in v") as Boolean
    }

    /**
     * Perform the actual canvas drawImage call once we have a usable image source.
     */
    private fun performDrawImage(
        ctx: CanvasRenderingContext2D,
        cached: Any,
        json: JSONObject
    ) {
        // Resolve intrinsic size from the cached value when possible.
        // We avoid `is HTMLImageElement` because that global is undefined in mini-program.
        val intrinsicWidth: Double
        val intrinsicHeight: Double
        if (isImageLike(cached)) {
            val img: dynamic = cached
            val nw = (img.naturalWidth as? Number)?.toInt() ?: 0
            val nh = (img.naturalHeight as? Number)?.toInt() ?: 0
            if (nw == 0) {
                return
            }
            intrinsicWidth = nw.toDouble()
            intrinsicHeight = nh.toDouble()
        } else {
            // string path or other dynamic value, intrinsic size is unknown
            intrinsicWidth = 0.0
            intrinsicHeight = 0.0
        }

        val sx = json.optDouble("sx", 0.0)
        val sy = json.optDouble("sy", 0.0)
        val hasSrcRect = json.has("sx") || json.has("sy") || json.has("sWidth") || json.has("sHeight")
        val sWidth = json.optDouble("sWidth", intrinsicWidth)
        val sHeight = json.optDouble("sHeight", intrinsicHeight)
        val dx = json.optDouble("dx", 0.0)
        val dy = json.optDouble("dy", 0.0)
        val hasDstSize = json.has("dWidth") || json.has("dHeight")
        val dWidth = json.optDouble("dWidth", if (sWidth > 0) sWidth else intrinsicWidth)
        val dHeight = json.optDouble("dHeight", if (sHeight > 0) sHeight else intrinsicHeight)

        val image: dynamic = cached

        // Decide which Canvas2D drawImage overload to use.
        //
        // Important: when Compose ships dWidth/dHeight (or sWidth/sHeight) explicitly
        // but their values are 0, it means the underlying KuiklyImageBitmap was not
        // ready yet at compose time (kImage.width/height == 0 → divided by density
        // still 0). In that case we must NOT fall back to the 3-arg overload
        //   ctx.drawImage(image, dx, dy)
        // which would paint the picture at its natural pixel size and overflow the
        // small canvas. Skip the draw instead; Compose will reissue a correct call
        // once the bitmap finishes loading.
        val srcRectValid = hasSrcRect && sWidth > 0 && sHeight > 0
        val dstSizeValid = hasDstSize && dWidth > 0 && dHeight > 0
        val srcRectInvalid = hasSrcRect && !srcRectValid
        val dstSizeInvalid = hasDstSize && !dstSizeValid
        if (srcRectInvalid || dstSizeInvalid) {
            // The compose layer explicitly requested a size but it isn't usable yet;
            // wait for the next reissue rather than drawing at natural size.
            return
        }

        when {
            srcRectValid ->
                ctx.asDynamic().drawImage(
                    image,
                    sx, sy, sWidth, sHeight,
                    dx, dy, dWidth, dHeight
                )
            dstSizeValid ->
                ctx.asDynamic().drawImage(image, dx, dy, dWidth, dHeight)
            else ->
                ctx.asDynamic().drawImage(image, dx, dy)
        }
    }

    /**
     * saveLayer is downgraded to save() + clip(rect) on H5.
     * The matching restore() will be issued by the core layer at the end of the layer scope.
     */
    private fun saveLayer(params: String?) {
        val ctx = canvasContext ?: return
        val json = params.toJSONObjectSafely()
        val x = json.optDouble(KRViewConst.X, 0.0)
        val y = json.optDouble(KRViewConst.Y, 0.0)
        val width = json.optDouble(KRViewConst.WIDTH, ele.width.toDouble())
        val height = json.optDouble(KRViewConst.HEIGHT, ele.height.toDouble())
        ctx.save()
        ctx.beginPath()
        ctx.rect(x, y, width, height)
        ctx.clip()
    }

    private fun translate(params: String?) {
        val json = params.toJSONObjectSafely()
        canvasContext?.translate(
            json.optDouble(KRViewConst.X, 0.0),
            json.optDouble(KRViewConst.Y, 0.0)
        )
    }

    private fun scale(params: String?) {
        val json = params.toJSONObjectSafely()
        canvasContext?.scale(
            json.optDouble(KRViewConst.X, 1.0),
            json.optDouble(KRViewConst.Y, 1.0)
        )
    }

    private fun rotate(params: String?) {
        val json = params.toJSONObjectSafely()
        canvasContext?.rotate(json.optDouble("angle", 0.0))
    }

    private fun skew(params: String?) {
        val ctx = canvasContext ?: return
        val json = params.toJSONObjectSafely()
        val sx = json.optDouble(KRViewConst.X, 0.0)
        val sy = json.optDouble(KRViewConst.Y, 0.0)
        // Canvas 2D has no native skew, simulate via transform(1, tan(y), tan(x), 1, 0, 0)
        ctx.transform(1.0, tan(sy), tan(sx), 1.0, 0.0, 0.0)
    }

    /**
     * Apply a 3x3 transform matrix. The core layer serializes a row-major 3x3 affine
     * matrix as 9 floats following Android `android.graphics.Matrix.getValues` order:
     *
     *     [0] scaleX  [1] skewX   [2] translateX
     *     [3] skewY   [4] scaleY  [5] translateY
     *     [6] persp0  [7] persp1  [8] persp2
     *
     * Canvas 2D `transform(a, b, c, d, e, f)` expects column-major affine values where
     *   a = scaleX, b = skewY, c = skewX, d = scaleY, e = translateX, f = translateY,
     * which corresponds to indices (0, 3, 1, 4, 2, 5) of the serialized array.
     */
    private fun transform(params: String?) {
        val json = params.toJSONObjectSafely()
        val values = json.optJSONArray("values") ?: return
        if (values.length() < 6) {
            return
        }
        canvasContext?.transform(
            values.optDouble(0), // a  = scaleX
            values.optDouble(3), // b  = skewY
            values.optDouble(1), // c  = skewX
            values.optDouble(4), // d  = scaleY
            values.optDouble(2), // e  = translateX
            values.optDouble(5)  // f  = translateY
        )
    }

    /**
     * createRadialGradient. To stay aligned with iOS behavior (which immediately draws the
     * gradient covering the whole context with the given globalAlpha), we paint a fullscreen
     * radial gradient on the canvas right here.
     */
    private fun createRadialGradient(params: String?) {
        val ctx = canvasContext ?: return
        val json = params.toJSONObjectSafely()
        val x0 = json.optDouble("x0")
        val y0 = json.optDouble("y0")
        val r0 = json.optDouble("r0")
        val x1 = json.optDouble("x1")
        val y1 = json.optDouble("y1")
        val r1 = json.optDouble("r1")
        val alpha = json.optDouble("alpha", 1.0)
        val colorStops = json.optString("colors")
        val gradient = ctx.createRadialGradient(x0, y0, r0, x1, y1, r1)
        if (colorStops.isNotEmpty()) {
            val splits = splitCanvasColorDefinitions(colorStops)
            splits.forEach { item ->
                val colorAndPosition = item.split(" ")
                if (colorAndPosition.size >= 2) {
                    gradient.addColorStop(
                        colorAndPosition[1].toDouble(),
                        colorAndPosition[0].toRgbColor()
                    )
                }
            }
        }
        ctx.save()
        ctx.globalAlpha = alpha
        ctx.fillStyle = gradient
        ctx.fillRect(0.0, 0.0, ele.width.toDouble(), ele.height.toDouble())
        ctx.restore()
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
        private const val CREATE_RADIAL_GRADIENT = "createRadialGradient"
        private const val QUADRATIC_CURVE_TO = "quadraticCurveTo"
        private const val BEZIER_CURVE_TO = "bezierCurveTo"
        private const val RESET = "reset"
        private const val STYLE = "style"
        private const val TEXT = "text"
        private const val TEXT_ALIGN = "textAlign"
        private const val FONT = "font"
        private const val DRAW_IMAGE = "drawImage"
        private const val SAVE = "save"
        private const val SAVE_LAYER = "saveLayer"
        private const val RESTORE = "restore"
        private const val TRANSLATE = "translate"
        private const val SCALE = "scale"
        private const val ROTATE = "rotate"
        private const val SKEW = "skew"
        private const val TRANSFORM = "transform"

        private const val RADIUS = "r"
        private const val START_ANGLE = "sAngle"
        private const val END_ANGLE = "eAngle"
        private const val COUNTER_CLOCKWISE = "counterclockwise"
        private const val TYPE_COUNTER_CLOCKWISE = 1
    }
}
