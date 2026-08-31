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
import com.tencent.kuikly.core.render.web.expand.components.KRImageView.Companion.BASE64_IMAGE_PREFIX
import com.tencent.kuikly.core.render.web.expand.module.KRMemoryCacheModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.processor.IEvent
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.processor.state
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.utils.DeviceType
import com.tencent.kuikly.core.render.web.utils.DeviceUtils
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.Touch
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.js.json
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            BRING_TO_FRONT -> {
                val parent = ele.parentElement ?: return null
                // Move element to end of parent's children, making it topmost in z-order
                parent.appendChild(ele)
                // Ensure this element can receive pointer events
                ele.style.asDynamic().pointerEvents = "auto"
                // Disable pointer events on all sibling elements that are now behind
                for (i in 0 until parent.children.length) {
                    val sibling = parent.children[i]
                    if (sibling !== ele) {
                        sibling.unsafeCast<HTMLDivElement>().style.asDynamic().pointerEvents = "none"
                    }
                }
                null
            }

            TO_IMAGE -> {
                toImage(params, callback)
                null
            }
            else -> super.call(method, params, callback)
        }
    }

    private fun toImage(params: String?, callback: KuiklyRenderCallback?) {
        val json = JSONObject(params ?: "{}")
        val type = json.optString(TO_IMAGE_PARAM_TYPE).ifEmpty { TO_IMAGE_TYPE_DATA_URI }
        val sampleSize = max(1, json.optInt(TO_IMAGE_PARAM_SAMPLE_SIZE, 1))
        // H5-only extension: additional upscale factor on top of DPR / sampleSize.
        // Other platforms simply ignore this field.
        val scale = max(MIN_TO_IMAGE_SCALE, json.optDouble(TO_IMAGE_PARAM_SCALE, 1.0))

        if (type == TO_IMAGE_TYPE_FILE) {
            callback?.invoke(toImageError("FILE is not supported on H5"))
            return
        }

        // Rasterize current DOM subtree via SVG foreignObject.
        // Root cause fixes on top of the naive approach:
        //   1) Inline computed styles into every cloned node so fonts / line-height /
        //      white-space are consistent inside foreignObject (avoid last-char wrap).
        //   2) Pre-fetch cross-origin <img> resources into dataURL so the SVG can render
        //      them and canvas won't be tainted (avoid broken-image placeholders).
        //   3) Keep sub-pixel size as float to avoid rounding-induced re-layout.
        //   4) Snapshot each <canvas> backing store into an <img data:...> replacement
        //      inside the clone, otherwise cloneNode(true) loses the pixel content.
        //   5) Scale output bitmap by devicePixelRatio (divided by sampleSize) so the
        //      snapshot stays crisp on Retina/HiDPI screens. Cap final canvas side to
        //      MAX_CANVAS_SIDE to avoid hitting browser canvas size limits.
        val rect = ele.getBoundingClientRect()
        val widthF = if (rect.width > 0.0) rect.width else 1.0
        val heightF = if (rect.height > 0.0) rect.height else 1.0
        val dprRaw = kuiklyWindow.asDynamic().devicePixelRatio.unsafeCast<Double?>() ?: 1.0
        val dpr = if (dprRaw > 0.0) dprRaw else 1.0
        // sampleSize keeps its "down-sample" semantics: 1 = full quality, 2 = half, etc.
        // scale is an H5-only extra upscale factor requested by the caller (default 1.0).
        // Total zoom combines DPR upscale, caller-requested scale, and sampleSize downscale.
        val zoom = max(dpr * scale / sampleSize.toDouble(), MIN_TO_IMAGE_ZOOM)
        val rawOutW = widthF * zoom
        val rawOutH = heightF * zoom
        val sideFit = min(1.0, min(MAX_CANVAS_SIDE / rawOutW, MAX_CANVAS_SIDE / rawOutH))
        val outputWidth = max(1, ceil(rawOutW * sideFit).toInt())
        val outputHeight = max(1, ceil(rawOutH * sideFit).toInt())

        val cloned = ele.cloneNode(true).unsafeCast<HTMLDivElement>()
        // Inline every node's computed style into the clone.
        inlineComputedStyleTree(ele, cloned)
        // Capture each live <canvas> bitmap and replace the empty clone-canvas with
        // an <img> holding the snapshot dataURL. Must run AFTER inlineComputedStyleTree
        // so the replacement <img> can inherit the canvas's computed styles.
        preloadCanvasesAsImage(ele, cloned)
        // Normalize cloned root to remove outer margin / positioning offsets inside SVG.
        cloned.style.margin = "0"
        cloned.style.width = "${widthF}px"
        cloned.style.height = "${heightF}px"
        cloned.style.setProperty("overflow", "hidden")
        cloned.style.setProperty("position", "relative")
        cloned.style.setProperty("left", "0px")
        cloned.style.setProperty("top", "0px")
        cloned.style.setProperty("right", "auto")
        cloned.style.setProperty("bottom", "auto")
        cloned.style.setProperty("transform", "none")
        cloned.style.setProperty("transform-origin", "0 0")

        // Preload cross-origin images inside the clone into dataURL, then rasterize.
        preloadImagesAsDataUrl(cloned) {
            try {
                val serializer: dynamic = js("new XMLSerializer()")
                val xhtml = serializer.serializeToString(cloned)
                val svg = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="$widthF" height="$heightF">
                        <foreignObject width="100%" height="100%">$xhtml</foreignObject>
                    </svg>
                """.trimIndent()
                val encodedSvg = kuiklyWindow.asDynamic().encodeURIComponent(svg).unsafeCast<String>()
                val svgDataUrl = "data:image/svg+xml;charset=utf-8,$encodedSvg"

                val image = kuiklyDocument.createElement(ElementType.IMAGE).unsafeCast<HTMLImageElement>()
                image.addEventListener("load", { _: Event ->
                    try {
                        val canvas = kuiklyDocument.createElement(ElementType.CANVAS)
                            .unsafeCast<HTMLCanvasElement>()
                        canvas.width = outputWidth
                        canvas.height = outputHeight
                        val ctx = canvas.getContext("2d")
                        if (ctx == null) {
                            callback?.invoke(toImageError("failed to get 2d context"))
                        } else {
                            val ctx2d: dynamic = ctx
                            // Use Double coords so browser can rasterize SVG straight
                            // into the target (possibly upscaled) canvas resolution.
                            ctx2d.drawImage(
                                image,
                                0.0,
                                0.0,
                                outputWidth.toDouble(),
                                outputHeight.toDouble()
                            )
                            val dataUri = canvas.toDataURL("image/png")
                            if (type == TO_IMAGE_TYPE_CACHE_KEY) {
                                val cacheKey = buildImageCacheKey()
                                kuiklyRenderContext
                                    ?.module<KRMemoryCacheModule>(KRMemoryCacheModule.MODULE_NAME)
                                    ?.set(cacheKey, dataUri)
                                callback?.invoke(toImageSuccess(cacheKey))
                            } else {
                                callback?.invoke(toImageSuccess(dataUri))
                            }
                        }
                    } catch (t: Throwable) {
                        callback?.invoke(toImageError(t.message ?: "toImage render failed"))
                    }
                })
                image.addEventListener("error", { _: Event ->
                    callback?.invoke(toImageError("toImage load svg failed"))
                })
                image.src = svgDataUrl
            } catch (t: Throwable) {
                callback?.invoke(toImageError(t.message ?: "toImage serialize failed"))
            }
        }
    }

    /**
     * Walk the source subtree and copy each node's computed style onto the cloned
     * counterpart via inline `style.cssText`. This is required because a cloned
     * node inside SVG `<foreignObject>` loses access to outer document CSS, which
     * would otherwise cause font/line-height differences and text wrapping drift.
     */
    private fun inlineComputedStyleTree(source: dynamic, clone: dynamic) {
        if (source == null || clone == null) return
        val srcNodeType = source.nodeType.unsafeCast<Int>()
        if (srcNodeType != 1) return // ELEMENT_NODE only
        val computed = kuiklyWindow.asDynamic().getComputedStyle(source)
        if (computed != null) {
            val cssText = computed.cssText.unsafeCast<String?>() ?: ""
            if (cssText.isNotEmpty()) {
                clone.style.cssText = cssText
            } else {
                // Some browsers return empty cssText; fall back to property iteration.
                val len = computed.length.unsafeCast<Int>()
                var i = 0
                while (i < len) {
                    val prop = computed.item(i).unsafeCast<String>()
                    val value = computed.getPropertyValue(prop).unsafeCast<String>()
                    clone.style.setProperty(prop, value)
                    i++
                }
            }
        }
        val srcChildren = source.children
        val cloneChildren = clone.children
        if (srcChildren == null || cloneChildren == null) return
        val count = srcChildren.length.unsafeCast<Int>()
        var idx = 0
        while (idx < count) {
            inlineComputedStyleTree(srcChildren[idx], cloneChildren[idx])
            idx++
        }
    }

    /**
     * For each live <canvas> under [srcRoot], snapshot its backing store to a PNG
     * dataURL and replace the corresponding empty <canvas> under [cloneRoot] with
     * an <img> node holding that dataURL. Traversal order matches because
     * `cloneNode(true)` preserves child order 1:1 with the source subtree.
     *
     * If the source canvas is tainted by cross-origin content, `toDataURL(...)`
     * throws SecurityError. We silently skip such canvases so other content still
     * renders correctly (that canvas will appear blank in the snapshot).
     *
     * This step is synchronous — `toDataURL` on 2D / WebGL canvas returns
     * immediately. For WebGL specifically the canvas must be created with
     * `preserveDrawingBuffer: true` or drawn on the same frame, otherwise the
     * dataURL may be blank; that's a caller-side concern this method cannot fix.
     */
    private fun preloadCanvasesAsImage(srcRoot: dynamic, cloneRoot: dynamic) {
        val srcCanvases = srcRoot.querySelectorAll("canvas")
        val cloneCanvases = cloneRoot.querySelectorAll("canvas")
        val total = srcCanvases.length.unsafeCast<Int>()
        val cloneTotal = cloneCanvases.length.unsafeCast<Int>()
        if (total == 0 || cloneTotal == 0) return
        val safeTotal = if (total < cloneTotal) total else cloneTotal
        var i = 0
        while (i < safeTotal) {
            val srcCanvas = srcCanvases[i]
            val cloneCanvas = cloneCanvases[i]
            i++
            if (srcCanvas == null || cloneCanvas == null) continue
            val parent = cloneCanvas.parentNode
            if (parent == null) continue
            val dataUrl: String = try {
                srcCanvas.toDataURL("image/png").unsafeCast<String>()
            } catch (_: Throwable) {
                // Tainted canvas or unsupported context — leave clone canvas as-is.
                continue
            }
            if (dataUrl.isEmpty() || !dataUrl.startsWith("data:")) continue

            // Build an <img> replacement carrying the same visual box as the canvas:
            //   - Copy computed style (position/size/border/transform/etc.) so it sits
            //     exactly where the original canvas sat inside foreignObject.
            //   - Force display:inline-block and object-fit:fill so the bitmap fills
            //     the box regardless of the canvas's original display mode.
            val replacement: dynamic = kuiklyDocument.createElement(ElementType.IMAGE)
            val srcComputed = kuiklyWindow.asDynamic().getComputedStyle(srcCanvas)
            if (srcComputed != null) {
                val cssText = srcComputed.cssText.unsafeCast<String?>() ?: ""
                if (cssText.isNotEmpty()) {
                    replacement.style.cssText = cssText
                } else {
                    val len = srcComputed.length.unsafeCast<Int>()
                    var k = 0
                    while (k < len) {
                        val prop = srcComputed.item(k).unsafeCast<String>()
                        val value = srcComputed.getPropertyValue(prop).unsafeCast<String>()
                        replacement.style.setProperty(prop, value)
                        k++
                    }
                }
            }
            // Prefer the actual on-screen size over the canvas backing-store size.
            val srcRect = srcCanvas.getBoundingClientRect()
            val boxW = srcRect.width.unsafeCast<Double>()
            val boxH = srcRect.height.unsafeCast<Double>()
            if (boxW > 0.0) replacement.style.width = "${boxW}px"
            if (boxH > 0.0) replacement.style.height = "${boxH}px"
            replacement.style.setProperty("display", "inline-block")
            replacement.style.setProperty("object-fit", "fill")
            replacement.setAttribute("src", dataUrl)

            parent.replaceChild(replacement, cloneCanvas)
        }
    }

    /**
     * Find all <img> nodes inside [root] whose src is a remote URL and replace src
     * with a dataURL fetched via CORS. Falls back to the original src if fetch fails
     * (that image will still render as broken inside the snapshot, but other content
     * and canvas security are preserved). Invokes [onDone] after all images settle.
     */
    private fun preloadImagesAsDataUrl(root: dynamic, onDone: () -> Unit) {
        val imgs = root.querySelectorAll("img")
        val total = imgs.length.unsafeCast<Int>()
        if (total == 0) {
            onDone()
            return
        }
        var remaining = total
        val markDone = {
            remaining -= 1
            if (remaining <= 0) onDone()
        }
        var i = 0
        while (i < total) {
            val img = imgs[i]
            val src = img.getAttribute("src").unsafeCast<String?>() ?: ""
            if (src.isEmpty() || src.startsWith("data:")) {
                markDone()
            } else {
                fetchAsDataUrl(src, onSuccess = { dataUrl ->
                    img.setAttribute("src", dataUrl)
                    img.removeAttribute("crossorigin")
                    markDone()
                }, onError = { _ ->
                    // Keep original src; snapshot may show a broken image for this node,
                    // but canvas won't be tainted because SVG will just skip it.
                    markDone()
                })
            }
            i++
        }
    }

    /**
     * Fetch [url] as blob via CORS and convert to dataURL. All errors are routed to
     * [onError] with a short reason string so callers can log and decide fallback.
     */
    private fun fetchAsDataUrl(
        url: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val init: dynamic = js("({ mode: 'cors', credentials: 'omit', cache: 'force-cache' })")
            val promise = kuiklyWindow.asDynamic().fetch(url, init)
            promise.then({ resp: dynamic ->
                if (resp == null) {
                    onError("resp-null")
                } else if (resp.ok != true) {
                    val status = resp.status.unsafeCast<Int>()
                    onError("http-$status")
                } else {
                    resp.blob().then({ blob: dynamic ->
                        val reader: dynamic = js("new FileReader()")
                        reader.onload = {
                            val result = reader.result.unsafeCast<String?>()
                            if (result.isNullOrEmpty()) onError("reader-empty") else onSuccess(result)
                        }
                        reader.onerror = { onError("reader-error") }
                        reader.readAsDataURL(blob)
                    }, { onError("blob-reject") })
                }
                null
            }, { err: dynamic ->
                val reason = try { err.message.unsafeCast<String>() } catch (_: Throwable) { "fetch-reject" }
                onError(reason)
            })
        } catch (t: Throwable) {
            onError(t.message ?: "fetch-throw")
        }
    }

    private fun buildImageCacheKey(): String {
        val timestamp = js("Date.now()").unsafeCast<Double>().toLong()
        val random = js("Math.floor(Math.random() * 1000000)").unsafeCast<Double>().toInt() + 1_000_000
        return "${BASE64_IMAGE_PREFIX}_Md5_snapshot_${timestamp}_${random}"
    }

    private fun toImageSuccess(data: String): Map<String, Any> = mapOf(
        TO_IMAGE_CODE to 0,
        TO_IMAGE_DATA to data,
        TO_IMAGE_MESSAGE to ""
    )

    private fun toImageError(message: String): Map<String, Any> = mapOf(
        TO_IMAGE_CODE to -1,
        TO_IMAGE_DATA to "",
        TO_IMAGE_MESSAGE to message
    )

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
     * Bind touch event and mouse event based on device capability first,
     * then fallback to device type. On modern browsers (including touch-screen
     * Windows), PointerEvent is the unified channel for mouse / touch / pen,
     * so prefer it whenever available.
     */
    private fun setTouchEvent() {
        if (isBindTouchEvent) {
            return
        }
        isBindTouchEvent = true

        val hasPointerEvent = js(
            "typeof window !== 'undefined' && typeof window.PointerEvent === 'function'"
        ).unsafeCast<Boolean>()

        if (hasPointerEvent && deviceType != DeviceType.MINIPROGRAM) {
            bindPointerEvents()
            return
        }

        when (deviceType) {
            DeviceType.MOBILE -> bindTouchEvents()
            DeviceType.MINIPROGRAM -> bindTouchEvents()
            DeviceType.DESKTOP -> bindMouseEvents()
        }
    }

    /**
     * Bind pointer events. Works for mouse, touch and pen on modern browsers,
     * which is the only reliable channel on touch-screen Windows (Chrome / Edge
     * may not dispatch synthetic touchstart there).
     *
     * PointerEvent inherits from MouseEvent, so we can safely reuse the
     * existing MouseEvent.toPanEventParams() extension without introducing
     * a new coordinate path.
     */
    private fun bindPointerEvents() {
        // Pointer down
        ele.addEventListener("pointerdown", { rawEvent ->
            val mouseLike = rawEvent.unsafeCast<MouseEvent>()
            // Capture pointer so we keep receiving move/up even if the finger /
            // cursor leaves the element bounds during a drag.
            val pointerId = rawEvent.asDynamic().pointerId
            if (pointerId != null) {
                try {
                    ele.asDynamic().setPointerCapture(pointerId)
                } catch (_: Throwable) {
                    // Some environments may throw if pointerId is invalid; ignore.
                }
            }

            isMouseDown = true
            val eventParams = mouseLike.toPanEventParams()
            val position = ele.getBoundingClientRect()
            eleX = position.left.toFloat()
            eleY = position.top.toFloat()

            var params = getPanEventParams(
                fastMutableMapOf<String, Any>().apply { putAll(eventParams) },
                KRStateConst.START
            )
            params = setSuperTouchEventParams(
                params, rawEvent.timeStamp.toLong(), KRActionConst.TOUCH_DOWN
            )
            panEventCallback?.invoke(params)
            touchDownEventCallback?.invoke(params)
            rawEvent.stopPropagation()
        })

        // Pointer move
        ele.addEventListener("pointermove", { rawEvent ->
            if (!isMouseDown) return@addEventListener
            val mouseLike = rawEvent.unsafeCast<MouseEvent>()
            val eventParams = mouseLike.toPanEventParams()
            var params = getPanEventParams(
                fastMutableMapOf<String, Any>().apply { putAll(eventParams) },
                KRStateConst.MOVE
            )
            params = setSuperTouchEventParams(
                params, rawEvent.timeStamp.toLong(), KRActionConst.TOUCH_MOVE
            )
            panEventCallback?.invoke(params)
            touchMoveEventCallback?.invoke(params)
            rawEvent.stopPropagation()
        })

        // Pointer up
        ele.addEventListener("pointerup", { rawEvent ->
            if (!isMouseDown) return@addEventListener
            isMouseDown = false
            var params = fastMutableMapOf<String, Any>().apply {
                put(KRParamConst.X, x)
                put(KRParamConst.Y, y)
                put(KRParamConst.STATE, KRStateConst.END)
                put(KRParamConst.PAGE_X, pageX)
                put(KRParamConst.PAGE_Y, pageY)
            }
            params = setSuperTouchEventParams(
                params, rawEvent.timeStamp.toLong(), KRActionConst.TOUCH_UP
            )
            panEventCallback?.invoke(params)
            touchUpEventCallback?.invoke(params)
            rawEvent.stopPropagation()
        })

        // Pointer cancel (system takes over the pointer, e.g. scroll / gesture)
        ele.addEventListener("pointercancel", { rawEvent ->
            if (!isMouseDown) return@addEventListener
            isMouseDown = false
            var params = fastMutableMapOf<String, Any>().apply {
                put(KRParamConst.X, x)
                put(KRParamConst.Y, y)
                put(KRParamConst.PAGE_X, pageX)
                put(KRParamConst.PAGE_Y, pageY)
                put(KRParamConst.STATE, KRStateConst.CANCEL)
            }
            params = setSuperTouchEventParams(
                params, rawEvent.timeStamp.toLong(), KRActionConst.TOUCH_CANCEL
            )
            touchUpEventCallback?.invoke(params)
            rawEvent.stopPropagation()
        })
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
        private const val BRING_TO_FRONT = "bringToFront"
        private const val TO_IMAGE = "toImage"

        private const val TO_IMAGE_PARAM_TYPE = "type"
        private const val TO_IMAGE_PARAM_SAMPLE_SIZE = "sampleSize"
        private const val TO_IMAGE_PARAM_SCALE = "scale"

        // Upper bound of a single side of the output canvas. Most browsers cap
        // canvas dimensions around 8192~16384 px; 4096 is a safe cross-browser
        // ceiling that also keeps memory usage reasonable for large scale values.
        private const val MAX_CANVAS_SIDE = 4096.0
        // Lower bound for the combined zoom factor to guarantee we never end up
        // with a zero-sized canvas even for weird inputs (scale=0 / dpr=0).
        private const val MIN_TO_IMAGE_ZOOM = 0.01
        // Minimum allowed caller-supplied scale. 0 or negative would collapse the
        // output to nothing; guard here so the caller doesn't have to.
        private const val MIN_TO_IMAGE_SCALE = 0.01

        private const val TO_IMAGE_TYPE_CACHE_KEY = "cacheKey"
        private const val TO_IMAGE_TYPE_DATA_URI = "dataUri"
        private const val TO_IMAGE_TYPE_FILE = "file"

        private const val TO_IMAGE_CODE = "code"
        private const val TO_IMAGE_DATA = "data"
        private const val TO_IMAGE_MESSAGE = "message"
        // Refresh rate interval, 16ms (approximately 60fps)
        private const val SCREEN_FRAME_REFRESH_TIME = 16
        // Border size ratio threshold
        private const val BORDER_SIZE_RATIO = 5
    }
}
