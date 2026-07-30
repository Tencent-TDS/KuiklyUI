package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.ktx.splitCanvasColorDefinitions
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.math.tan

/**
 * Mini program canvas context, backed by the Canvas 2D ("type=2d") interface.
 *
 * Compared with the legacy `wx.createCanvasContext(id)` interface this implementation:
 * - obtains the canvas node asynchronously via `selectorQuery().select('#id').node().exec()`;
 * - exposes W3C-standard property setters (fillStyle / strokeStyle / lineWidth / font / ...);
 * - does NOT require a final `ctx.draw()` flush call;
 * - supports same-layer rendering inside scroll-view;
 * - resolves image sources through `canvas.createImage()` so drawImage works with paths,
 *   data URLs and HTTP URLs.
 *
 * Because node acquisition is asynchronous, every draw call is enqueued and flushed once the
 * native canvas / context becomes available. Calls made after the context is ready run
 * immediately. This keeps the public API synchronous for upstream KRCanvasView code.
 */
class MiniCanvasContext(private val canvas: MiniCanvasElement) {
    /**
     * Pending operations that were issued before the canvas node was ready.
     * Once the node resolves we replay them in order.
     */
    private val pending: MutableList<(dynamic) -> Unit> = mutableListOf()

    /**
     * Native canvas node returned by selectorQuery().node(). Null until selectorQuery resolves.
     */
    private var nativeCanvas: dynamic = null

    /**
     * Real CanvasRenderingContext2D from the native canvas node.
     */
    private var nativeCtx: dynamic = null

    /**
     * Whether selectorQuery has been kicked off (only do it once per canvas instance).
     */
    private var queryStarted: Boolean = false

    /**
     * Maximum number of selectorQuery retries when the canvas node is not yet attached.
     */
    private val maxQueryRetry: Int = 30

    /**
     * Cached image objects created via canvas.createImage(), keyed by src.
     * Avoids redundant decoding when drawImage is called repeatedly with the same src.
     */
    private val imageCache: MutableMap<String, dynamic> = mutableMapOf()

    /**
     * Lazily kick off the asynchronous selectorQuery to obtain the Canvas 2D node.
     * The query is retried until the node is found, because the upstream KRCanvasView
     * may issue draw calls before wxml has finished rendering the <canvas> element.
     */
    private fun ensureContext() {
        if (queryStarted) return
        queryStarted = true
        scheduleQuery(0)
    }

    /**
     * Run a single selectorQuery attempt; on failure, schedule another via setTimeout.
     */
    private fun scheduleQuery(attempt: Int) {
        if (nativeCtx != null) return
        val plat = NativeApi.plat
        // Use the canvas-element-aware context (custom-wrapper or page) so that
        // selectorQuery can pierce into custom components such as the Scroller wrapper.
        // Falls back to wx/qq global if the canvas has not been mounted yet.
        val ctx: dynamic = try {
            canvas.getCurrentContext()
        } catch (e: Throwable) {
            plat
        }
        val queryRoot: dynamic = if (ctx != null && ctx != undefined) ctx else plat
        val query = queryRoot.createSelectorQuery()
        query.select("#${canvas.id}")
            .fields(js("({ node: true, size: true })"))
            .exec { res ->
                // `res` is a plain JS Array returned by selectorQuery; use bracket
                // indexing instead of Kotlin's `.get()` which would be compiled to a
                // method call that does not exist on a JS Array.
                val first: dynamic =
                    if (res != null && (res.length as? Int ?: 0) > 0) res[0] else null
                val node: dynamic = if (first != null) first.node else null
                if (node != null) {
                    nativeCanvas = node
                    nativeCtx = node.getContext("2d")
                    // Only warn when readiness took noticeably long (>5 retries),
                    // useful for spotting timing regressions on slow devices.
                    if (attempt > 5) {
                        val rootKind: String = if (queryRoot === plat) "global" else "customWrapper"
                        Log.warn(
                            "MiniCanvasContext #${canvas.id} ready after $attempt retries " +
                                    "(root=$rootKind, pending=${pending.size})"
                        )
                    }
                    flushPending()
                } else if (attempt < maxQueryRetry) {
                    // Node not attached yet, retry on next frame / timeout tick.
                    val nextTick = plat.nextTick
                    if (nextTick != null && nextTick != undefined) {
                        nextTick({ scheduleQuery(attempt + 1) })
                    } else {
                        js("setTimeout")({ scheduleQuery(attempt + 1) }, 16)
                    }
                } else {
                    // Final failure: surface so the issue does not silently swallow draw calls.
                    val rootKind: String = if (queryRoot === plat) "global" else "customWrapper"
                    Log.warn(
                        "MiniCanvasContext #${canvas.id} selectorQuery failed after " +
                                "$maxQueryRetry retries (root=$rootKind, pending=${pending.size})"
                    )
                }
            }
    }

    /**
     * Run [op] immediately if the context is ready, otherwise enqueue it.
     */
    private fun runOrEnqueue(op: (dynamic) -> Unit) {
        val ctx = nativeCtx
        if (ctx != null) {
            op(ctx)
        } else {
            pending.add(op)
            ensureContext()
        }
    }

    /**
     * Replay queued operations after the native context has been resolved.
     */
    private fun flushPending() {
        val ctx = nativeCtx ?: return
        if (pending.isEmpty()) return
        // Snapshot then clear to avoid re-entrancy issues if an op enqueues more work.
        val snapshot = pending.toList()
        pending.clear()
        snapshot.forEach { it(ctx) }
    }

    /**
     * fillStyle setter. Accepts W3C colors, CanvasGradient instances and the kuikly
     * `linear-gradient{...}` JSON string. Reads return the underlying ctx value once
     * the context is ready (otherwise null).
     */
    @JsName("fillStyle")
    var fillStyle: dynamic
        get() = nativeCtx?.fillStyle
        set(value) {
            runOrEnqueue { ctx ->
                ctx.fillStyle = resolveStyle(ctx, value)
            }
        }

    /**
     * strokeStyle setter. Same semantics as [fillStyle].
     */
    @JsName("strokeStyle")
    var strokeStyle: dynamic
        get() = nativeCtx?.strokeStyle
        set(value) {
            runOrEnqueue { ctx ->
                ctx.strokeStyle = resolveStyle(ctx, value)
            }
        }

    /**
     * Line width.
     */
    @JsName("lineWidth")
    var lineWidth: dynamic
        get() = nativeCtx?.lineWidth
        set(value) {
            runOrEnqueue { ctx -> ctx.lineWidth = value }
        }

    /**
     * Line cap. Accepts "butt" / "round" / "square".
     */
    @JsName("lineCap")
    var lineCap: dynamic
        get() = nativeCtx?.lineCap
        set(value) {
            runOrEnqueue { ctx -> ctx.lineCap = value }
        }

    /**
     * Canvas font (CSS font shorthand string), e.g. "normal 14px sans-serif".
     */
    @JsName("font")
    var font: dynamic
        get() = nativeCtx?.font
        set(value) {
            runOrEnqueue { ctx -> ctx.font = value }
        }

    /**
     * textAlign: "left" | "right" | "center" | "start" | "end".
     */
    @JsName("textAlign")
    var textAlign: dynamic
        get() = nativeCtx?.textAlign
        set(value) {
            runOrEnqueue { ctx -> ctx.textAlign = value }
        }

    /**
     * Global alpha, 0..1.
     */
    @JsName("globalAlpha")
    var globalAlpha: dynamic
        get() = nativeCtx?.globalAlpha
        set(value) {
            runOrEnqueue { ctx -> ctx.globalAlpha = value }
        }

    /**
     * Resolve a style value:
     * - if it is a CanvasGradient (object with addColorStop), return as-is;
     * - if it is a "linear-gradient{...}" string, build a real CanvasGradient on [ctx];
     * - otherwise (regular color / pattern), return unchanged.
     */
    private fun resolveStyle(ctx: dynamic, value: dynamic): dynamic {
        // Non-string values (color objects, CanvasGradient...) pass through unchanged.
        val isString = js("typeof value === 'string'") as Boolean
        if (!isString) return value
        val str: String = value.unsafeCast<String>()
        val prefix = "linear-gradient"
        if (!str.startsWith(prefix)) return str
        return parseLinearGradient(ctx, str.substring(prefix.length)) ?: str
    }

    /**
     * Build a CanvasGradient from a kuikly linear-gradient JSON payload using [ctx].
     */
    private fun parseLinearGradient(ctx: dynamic, jsonStr: String): dynamic {
        return try {
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

    @JsName("beginPath")
    fun beginPath() = runOrEnqueue { it.beginPath() }

    @JsName("moveTo")
    fun moveTo(x: Double, y: Double) = runOrEnqueue { it.moveTo(x, y) }

    @JsName("lineTo")
    fun lineTo(x: Double, y: Double) = runOrEnqueue { it.lineTo(x, y) }

    @JsName("arc")
    fun arc(cx: Double, cy: Double, radius: Double, startAngle: Double, endAngle: Double, counterclockwise: Boolean) =
        runOrEnqueue { it.arc(cx, cy, radius, startAngle, endAngle, counterclockwise) }

    @JsName("closePath")
    fun closePath() = runOrEnqueue { it.closePath() }

    @JsName("stroke")
    fun stroke() = runOrEnqueue { it.stroke() }

    @JsName("strokeText")
    fun strokeText(text: String, x: Double, y: Double) =
        runOrEnqueue { it.strokeText(text, x, y) }

    @JsName("fill")
    fun fill() = runOrEnqueue { it.fill() }

    @JsName("fillText")
    fun fillText(text: String, x: Double, y: Double) =
        runOrEnqueue { it.fillText(text, x, y) }

    @JsName("fillRect")
    fun fillRect(x: Double, y: Double, w: Double, h: Double) =
        runOrEnqueue { it.fillRect(x, y, w, h) }

    @JsName("strokeRect")
    fun strokeRect(x: Double, y: Double, w: Double, h: Double) =
        runOrEnqueue { it.strokeRect(x, y, w, h) }

    @JsName("rect")
    fun rect(x: Double, y: Double, w: Double, h: Double) =
        runOrEnqueue { it.rect(x, y, w, h) }

    @JsName("quadraticCurveTo")
    fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) =
        runOrEnqueue { it.quadraticCurveTo(cpx, cpy, x, y) }

    @JsName("bezierCurveTo")
    fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) =
        runOrEnqueue { it.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y) }

    @JsName("clip")
    fun clip() = runOrEnqueue { it.clip() }

    @JsName("clearRect")
    fun clearRect(x: Double, y: Double, width: Double, height: Double) =
        runOrEnqueue { it.clearRect(x, y, width, height) }

    /**
     * Synchronous createLinearGradient is supported only after the native ctx is ready.
     * Returns null if called before then; upstream code generally does not rely on the
     * synchronous return value because gradients are also expressible via the
     * "linear-gradient{...}" string passed to fillStyle / strokeStyle.
     */
    @JsName("createLinearGradient")
    fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): dynamic =
        nativeCtx?.createLinearGradient(x0, y0, x1, y1)

    /**
     * createRadialGradient. With Canvas 2D the W3C signature is supported natively.
     */
    @JsName("createRadialGradient")
    fun createRadialGradient(
        x0: Double, y0: Double, r0: Double,
        x1: Double, y1: Double, r1: Double
    ): dynamic = nativeCtx?.createRadialGradient(x0, y0, r0, x1, y1, r1)

    @JsName("setLineDash")
    fun setLineDash(segments: Array<Double>) =
        runOrEnqueue { it.setLineDash(segments) }

    @JsName("save")
    fun save() = runOrEnqueue { it.save() }

    @JsName("restore")
    fun restore() = runOrEnqueue { it.restore() }

    @JsName("translate")
    fun translate(x: Double, y: Double) =
        runOrEnqueue { it.translate(x, y) }

    @JsName("scale")
    fun scale(x: Double, y: Double) = runOrEnqueue { it.scale(x, y) }

    @JsName("rotate")
    fun rotate(angle: Double) = runOrEnqueue { it.rotate(angle) }

    @JsName("transform")
    fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) =
        runOrEnqueue { it.transform(a, b, c, d, e, f) }

    @JsName("setTransform")
    fun setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) =
        runOrEnqueue { it.setTransform(a, b, c, d, e, f) }

    /**
     * Skew is not natively supported, simulate via transform.
     */
    @JsName("skew")
    fun skew(x: Double, y: Double) =
        runOrEnqueue { it.transform(1.0, tan(y), tan(x), 1.0, 0.0, 0.0) }

    /**
     * drawImage. Canvas 2D only accepts an Image / Canvas / ImageBitmap object as the
     * first argument; passing a path or data: URL throws "image is null". So whenever
     * we receive a string we transparently convert it into a real Image via
     * canvas.createImage() and replay drawImage once it loads.
     */
    @JsName("drawImage")
    fun drawImage(
        image: dynamic, dx: dynamic, dy: dynamic,
        a: dynamic, b: dynamic, c: dynamic, d: dynamic, e: dynamic, f: dynamic
    ) {
        runOrEnqueue { ctx ->
            val isString = js("typeof image === 'string'") as Boolean
            if (isString) {
                val src: String = image.unsafeCast<String>()
                val cached = imageCache[src]
                val cachedReady: Boolean = if (cached != null) {
                    js("(cached.complete === true) || (typeof cached.width === 'number' && cached.width > 0)") as Boolean
                } else false
                if (cached != null && cachedReady) {
                    invokeDrawImage(ctx, cached, dx, dy, a, b, c, d, e, f)
                } else {
                    val img: dynamic = cached ?: nativeCanvas?.createImage()
                    if (img != null) {
                        if (cached == null) imageCache[src] = img
                        val onload: () -> Unit = {
                            invokeDrawImage(nativeCtx, img, dx, dy, a, b, c, d, e, f)
                        }
                        // Stack onload so multiple drawImage calls before completion
                        // each get their replay.
                        val previous: dynamic = img.onload
                        img.onload = {
                            if (previous != null && previous != undefined) previous()
                            onload()
                        }
                        img.onerror = {
                            // No-op: failing silently mirrors the behaviour on H5 where a broken
                            // <img> simply renders nothing.
                        }
                        if (img.src == null || img.src == undefined || img.src == "") {
                            img.src = src
                        }
                    }
                }
            } else {
                invokeDrawImage(ctx, image, dx, dy, a, b, c, d, e, f)
            }
        }
    }

    /**
     * Call ctx.drawImage with the appropriate arity based on which arguments are defined.
     */
    private fun invokeDrawImage(
        ctx: dynamic, image: dynamic, dx: dynamic, dy: dynamic,
        a: dynamic, b: dynamic, c: dynamic, d: dynamic, e: dynamic, f: dynamic
    ) {
        if (ctx == null || image == null) return
        val hasF = js("typeof f !== 'undefined'") as Boolean
        val hasB = js("typeof b !== 'undefined'") as Boolean
        when {
            hasF -> ctx.drawImage(image, dx, dy, a, b, c, d, e, f)
            hasB -> ctx.drawImage(image, dx, dy, a, b)
            else -> ctx.drawImage(image, dx, dy)
        }
    }

    /**
     * Synchronise the underlying canvas drawing buffer size with [width] / [height]
     * (interpreted as CSS pixels). The backing store is scaled by `devicePixelRatio`
     * so HiDPI screens render sharp, and a matching `setTransform(dpr,0,0,dpr,0,0)`
     * is installed so upstream draw calls keep using CSS-pixel coordinates.
     */
    fun resizeBackingStore(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val dpr = currentDpr()
        val applyResize: () -> Unit = {
            val node: dynamic = nativeCanvas
            val ctx: dynamic = nativeCtx
            if (node != null) {
                node.width = (width * dpr + 0.5).toInt()
                node.height = (height * dpr + 0.5).toInt()
            }
            if (ctx != null) {
                // Reset to the dpr-scaled identity so CSS-pixel coordinates render correctly.
                ctx.setTransform(dpr, 0.0, 0.0, dpr, 0.0, 0.0)
            }
        }
        if (nativeCanvas != null) {
            applyResize()
        } else {
            ensureContext()
            // Insert at the front so resize happens before subsequent draw operations.
            pending.add(0) { applyResize() }
        }
    }

    /**
     * Best-effort lookup of the current device pixel ratio. Falls back to 1.0 if the
     * platform API is unavailable (e.g. some sandboxed unit-test environments).
     */
    private fun currentDpr(): Double {
        return try {
            val ratio: dynamic = MiniGlobal.devicePixelRatio
            if (ratio != null && ratio != undefined) {
                (ratio.unsafeCast<Number>()).toDouble()
            } else 1.0
        } catch (e: Throwable) {
            1.0
        }
    }
}

/**
 * Mini program canvas node, rendered via the Canvas 2D (`type="2d"`) interface.
 * The legacy `canvas-id` attribute is kept for backward compatibility, but in 2d mode
 * the small program selects the node by `id`.
 */
class MiniCanvasElement(
    nodeName: String = TransformConst.CANVAS,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    init {
        // Default to Canvas 2D; this drives the wxml `type` attribute and is what enables
        // same-layer rendering inside scroll-view, drawImage with Image objects, etc.
        setAttribute(TYPE, TYPE_2D)
    }

    /**
     * Mini program needs a canvasId for the legacy bridge. We still emit it so the wxml
     * keeps working unchanged when running the (deprecated) old API.
     */
    private val canvasId: String
        get() = this.id

    /**
     * Canvas width in CSS pixels (logical size). The drawing buffer is sized
     * width * dpr internally so the rendering stays sharp on HiDPI screens.
     *
     * Note: do NOT route this through setAttribute — the wxml `<canvas>` tag does not
     * bind a `width` attribute, and even if it did, Canvas 2D requires setting
     * width / height directly on the native node returned by selectorQuery.
     */
    @JsName("width")
    var width: Int
        get() = cssWidth
        set(value) {
            cssWidth = value
            canvasContext.resizeBackingStore(value, cssHeight)
        }

    /**
     * Canvas height in CSS pixels (logical size).
     */
    @JsName("height")
    var height: Int
        get() = cssHeight
        set(value) {
            cssHeight = value
            canvasContext.resizeBackingStore(cssWidth, value)
        }

    private var cssWidth: Int = 0
    private var cssHeight: Int = 0

    /**
     * Canvas context.
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
     * Get canvas context. The argument (typically "2d") is ignored because we always
     * return the Canvas 2D wrapper.
     */
    @JsName("getContext")
    fun getContext(): dynamic = canvasContext

    companion object {
        private const val CANVAS_ID = "canvasId"
        private const val WIDTH = "width"
        private const val HEIGHT = "height"
        private const val TYPE = "type"
        private const val TYPE_2D = "2d"
    }
}
