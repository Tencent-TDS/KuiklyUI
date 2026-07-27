package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.IKuiklyRenderContext
import com.tencent.kuikly.core.render.web.expand.module.KRMemoryCacheModule
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.convertGradientStringToCssMask
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.setFrame
import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLImageElement

/**
 * KRImageView, corresponding to Kuikly's Image
 */
open class KRImageView(
    override var kuiklyRenderContext: IKuiklyRenderContext?
) : IKuiklyRenderViewExport {
    protected val image = kuiklyDocument.createElement(ElementType.IMAGE).apply {
        val imageElement = this.unsafeCast<HTMLImageElement>()
        imageElement.style.width = "100%"
        imageElement.style.height = "100%"
        imageElement.style.display = "block"
        // Set default image content
        imageElement.src = DEFAULT_SRC
        // Set load success callback, bind only once
        imageElement.addEventListener("load", {
            imageElement.style.display = "block"
            // Skip the default transparent placeholder's load event so it
            // won't trigger loadSuccess (which would incorrectly clear the
            // user-provided placeholder background).
            if (imageElement.src != DEFAULT_SRC) {
                // When loading succeeds, callback the actual image source content
                loadSuccessCallback?.invoke(
                    mapOf(
                        SRC to imageElement.src
                    )
                )
                // When loading succeeds, callback the image dimension data
                loadResolutionCallback?.invoke(
                    mapOf(
                        IMAGE_WIDTH to imageElement.naturalWidth,
                        IMAGE_HEIGHT to imageElement.naturalHeight
                    )
                )
            }
        })
        // Hide itself when image loading fails
        imageElement.addEventListener("error", {
            imageElement.style.display = "none"
            // Ignore errors from the built-in default transparent placeholder
            if (imageElement.src != DEFAULT_SRC) {
                loadFailureCallback?.invoke(mapOf(
                    SRC to imageElement.src,
                    // web can not get error code, return -10001
                    ERROR_CODE to ERROR_UNKNOWN
                ))
            }
        })
    }.unsafeCast<HTMLImageElement>()

    protected val div = kuiklyDocument.createElement(ElementType.DIV).apply {
        val divElement = this.unsafeCast<HTMLDivElement>()
        divElement.style.overflowX = "hidden"
        divElement.style.overflowY = "hidden"

        appendChild(image)
    }.unsafeCast<HTMLDivElement>()

    private var tintColorValue = ""
    // Current frame height in px, used as the Y-axis offset for tint color
    // implementations that rely on transform/drop-shadow (e.g. miniapp).
    private var frameHeight = 0.0
    private var resizeMode = "contain"
    // Current image src that has been resolved to a real URL (http/base64),
    // used by capInsets when building the border-image CSS on the outer div.
    private var currentResolvedSrc = ""
    // capInsets edges in CSS px (top, right, bottom, left). When all four
    // are zero, capInsets is disabled and the component renders normally.
    private var capInsetsTop = 0f
    private var capInsetsRight = 0f
    private var capInsetsBottom = 0f
    private var capInsetsLeft = 0f
    private var loadSuccessCallback: KuiklyRenderCallback? = null
    private var loadResolutionCallback: KuiklyRenderCallback? = null
    private var loadFailureCallback: KuiklyRenderCallback? = null

    override val ele: HTMLDivElement
        get() = div.unsafeCast<HTMLDivElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            SRC -> {
                // Set image source
                setSrc(propValue.unsafeCast<String>())
                true
            }

            RESIZE -> {
                // Adapt image stretch mode, no stretch in dom, set to fill
                setResize(propValue)
                // border-image-repeat follows the resize mode
                applyCapInsetsIfNeeded()
                true
            }

            CAP_INSETS -> {
                // Parse "top left bottom right" (unit: CSS px) from core layer.
                parseCapInsets(propValue.unsafeCast<String>())
                applyCapInsetsIfNeeded()
                true
            }

            LOAD_SUCCESS -> {
                // Save load success callback
                loadSuccessCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }

            LOAD_RESOLUTION -> {
                // Save load resolution callback
                loadResolutionCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }

            LOAD_FAILURE -> {
                // Save load failure callback
                loadFailureCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }

            TINT_COLOR -> {
                tintColorValue = propValue.unsafeCast<String>().toRgbColor()
                tintColorIfNeed()
                true
            }

            KRCssConst.FRAME -> {
                val frame = propValue.unsafeCast<Frame>()
                frameHeight = frame.height
                ele.setFrame(frame, ele.style)
                tintColorIfNeed()
                true
            }

            PROP_DOT_NINE_IMAGE -> {
                true
            }

            BLUR_RADIUS -> {
                // Set image blur radius
                val value = propValue.unsafeCast<Float>()
                ele.style.filter = "blur(${value * 2}px)"
                true
            }

            PLACEHOLDER -> {
                // Set placeholder image, displayed on the outer div as background
                // until the real image is loaded successfully, then the core layer
                // will issue an empty PLACEHOLDER to clear it.
                setPlaceholder(propValue.unsafeCast<String>())
                true
            }

            KRCssConst.MASK_LINEAR_GRADIENT -> {
                // Apply mask on the inner <img> so the gradient actually clips the
                // image pixels. The outer div is transparent and would produce no
                // visible masking effect.
                val raw = propValue.unsafeCast<String>()
                image.style.asDynamic().webkitMask = if (raw.isEmpty()) {
                    ""
                } else {
                    convertGradientStringToCssMask(raw)
                }
                true
            }

            else -> {
                // Other unified handling
                super.setProp(propKey, propValue)
            }
        }
    }

    /**
     * Apply tint color
     */
    private fun tintColorIfNeed() {
        KuiklyProcessor.imageProcessor.applyTintColor(image, tintColorValue, frameHeight)
    }

    /**
     * Parse capInsets value string "top left bottom right" (space separated).
     * Empty / malformed string resets capInsets to zero (disabled).
     */
    private fun parseCapInsets(raw: String) {
        if (raw.isEmpty()) {
            capInsetsTop = 0f
            capInsetsRight = 0f
            capInsetsBottom = 0f
            capInsetsLeft = 0f
            return
        }
        val parts = raw.split(" ")
        if (parts.size < 4) {
            capInsetsTop = 0f
            capInsetsRight = 0f
            capInsetsBottom = 0f
            capInsetsLeft = 0f
            return
        }
        capInsetsTop = parts[0].toFloatOrNull() ?: 0f
        capInsetsLeft = parts[1].toFloatOrNull() ?: 0f
        capInsetsBottom = parts[2].toFloatOrNull() ?: 0f
        capInsetsRight = parts[3].toFloatOrNull() ?: 0f
    }

    /**
     * Whether capInsets is currently enabled (at least one edge > 0).
     */
    private fun isCapInsetsEnabled(): Boolean =
        capInsetsTop > 0f || capInsetsRight > 0f ||
                capInsetsBottom > 0f || capInsetsLeft > 0f

    /**
     * Apply capInsets as CSS border-image on the outer div. When enabled, the
     * inner <img> is hidden and the full visual is produced by border-image;
     * when disabled, the inner <img> is restored.
     *
     * Note: border-image-slice uses the raw image pixel size (not CSS px).
     * The core layer passes the insets in CSS px; we forward them as unitless
     * numbers which the browser interprets as image pixels. This matches the
     * 9-patch semantics used on native (iOS/Android) platforms where the
     * insets describe pixel offsets on the source image.
     */
    private fun applyCapInsetsIfNeeded() {
        val style = div.style
        if (!isCapInsetsEnabled() || currentResolvedSrc.isEmpty()) {
            // Disable / clear: restore the inner <img> and drop border-image.
            style.asDynamic().borderImageSource = ""
            style.asDynamic().borderImageSlice = ""
            style.asDynamic().borderImageWidth = ""
            style.asDynamic().borderImageRepeat = ""
            style.borderStyle = ""
            style.borderColor = ""
            style.borderWidth = ""
            style.boxSizing = ""
            image.style.display = "block"
            return
        }
        // Enable: hide the original <img>, draw the 9-patch via border-image.
        image.style.display = "none"
        // Use border-box so the outer frame includes the border area.
        style.boxSizing = "border-box"
        style.borderStyle = "solid"
        style.borderColor = "transparent"
        style.borderTopWidth = capInsetsTop.toPxF()
        style.borderRightWidth = capInsetsRight.toPxF()
        style.borderBottomWidth = capInsetsBottom.toPxF()
        style.borderLeftWidth = capInsetsLeft.toPxF()
        style.asDynamic().borderImageSource = "url(\"$currentResolvedSrc\")"
        // "fill" keeps the middle region visible and stretched.
        style.asDynamic().borderImageSlice =
            "${capInsetsTop} ${capInsetsRight} ${capInsetsBottom} ${capInsetsLeft} fill"
        style.asDynamic().borderImageWidth =
            "${capInsetsTop.toPxF()} ${capInsetsRight.toPxF()} " +
                    "${capInsetsBottom.toPxF()} ${capInsetsLeft.toPxF()}"
        style.asDynamic().borderImageRepeat = capInsetsRepeatForResize()
    }

    /**
     * Map the current resize mode to the most appropriate border-image-repeat
     * value. "stretch" maps to CSS stretch, "cover" uses round (preserves
     * integer tiles), other modes fall back to stretch.
     */
    private fun capInsetsRepeatForResize(): String = when (resizeMode) {
        "fill" -> "stretch" // stretch in core -> "fill" in object-fit
        "cover" -> "round"
        "contain" -> "stretch"
        else -> "stretch"
    }

    /**
     * Check if the set image src is in base64 format
     */
    private fun isBase64Src(src: String): Boolean = src.startsWith(BASE64_IMAGE_PREFIX)

    /**
     * Get base64 image data cached in memory
     */
    private fun getBase64Image(key: String): String? {
        return kuiklyRenderContext?.module<KRMemoryCacheModule>(KRMemoryCacheModule.MODULE_NAME)
            ?.get(key)
    }

    /**
     * Set image src data.
     *
     * The raw src always goes through the image processor first, no matter what
     * prefix it has. This gives hosts full freedom to intercept and rewrite any
     * scheme (assets://, file://, http(s)://, custom xxx://, etc.). The prefix
     * is only inspected afterwards on the resolved result:
     * - base64 cache keys (data:image prefix) are read from the in-memory cache;
     * - everything else is assigned to the <img> directly.
     */
    private fun setSrc(src: String) {
        // Set when image src is not empty, otherwise use default transparent image
        if (src.isEmpty()) {
            return
        }
        // Let the processor intercept/transform first, then judge the prefix.
        val resolvedSrc = KuiklyProcessor.imageProcessor.getImageAssetsSource(src)
        if (isBase64Src(resolvedSrc)) {
            getBase64Image(resolvedSrc)?.let { image.src = it }
        } else {
            image.src = resolvedSrc
        }
        currentResolvedSrc = image.src
        // If capInsets was already set before src, refresh the border-image.
        applyCapInsetsIfNeeded()
    }

    /**
     * Set image stretch mode
     */
    private fun setResize(propValue: Any) {
        // Adapt image stretch mode, no stretch in DOM, set to fill
        val resizeValue = when (propValue.unsafeCast<String>()) {
            "stretch" -> "fill"
            else -> propValue.unsafeCast<String>()
        }
        image.style.objectFit = resizeValue
        resizeMode = resizeValue
        // Keep placeholder background-size consistent with the main image's fit
        applyPlaceholderResize()
    }

    /**
     * Set placeholder image. When [placeholder] is empty, the placeholder layer
     * is cleared (triggered by core layer after main image load success).
     */
    private fun setPlaceholder(placeholder: String) {
        if (placeholder.isEmpty()) {
            // Clear placeholder background
            div.style.backgroundImage = ""
            div.style.backgroundRepeat = ""
            div.style.backgroundPosition = ""
            div.style.backgroundSize = ""
            return
        }
        val resolvedSrc = resolvePlaceholderSrc(placeholder)
        if (resolvedSrc.isEmpty()) {
            return
        }
        div.style.backgroundImage = "url(\"$resolvedSrc\")"
        div.style.backgroundRepeat = "no-repeat"
        div.style.backgroundPosition = "center center"
        applyPlaceholderResize()
    }

    /**
     * Resolve placeholder src. Keeps the same order as [setSrc]: run the image
     * processor first so it can rewrite any prefix, then resolve base64 cache
     * keys from memory; other resolved values are used as-is.
     */
    private fun resolvePlaceholderSrc(src: String): String {
        val resolvedSrc = KuiklyProcessor.imageProcessor.getImageAssetsSource(src)
        return if (isBase64Src(resolvedSrc)) {
            getBase64Image(resolvedSrc) ?: ""
        } else {
            resolvedSrc
        }
    }

    /**
     * Apply the current resize mode to the placeholder background-size,
     * so the placeholder has the same visual fit as the main image.
     */
    private fun applyPlaceholderResize() {
        if (div.style.backgroundImage.isEmpty()) {
            return
        }
        div.style.backgroundSize = when (resizeMode) {
            "contain" -> "contain"
            "cover" -> "cover"
            "fill" -> "100% 100%"
            else -> "contain"
        }
    }

    companion object {
        const val VIEW_NAME = "KRImageView"
        const val APNG_VIEW_NAME = "KRAPNGView"

        // Base64 image prefix, identifies memory cached images
        const val BASE64_IMAGE_PREFIX = "data:image"

        // Image source
        const val SRC = "src"

        // Image dimensions
        const val IMAGE_WIDTH = "imageWidth"
        const val IMAGE_HEIGHT = "imageHeight"

        // Image dimension change notification
        private const val RESIZE = "resize"

        // Load success event
        private const val LOAD_SUCCESS = "loadSuccess"
        // Load error event
        private const val LOAD_FAILURE = "loadFailure"
        private const val ERROR_CODE = "errorCode"
        private const val ERROR_UNKNOWN = -10001
        // Load resolution event
        private const val LOAD_RESOLUTION = "loadResolution"
        private const val TINT_COLOR = "tintColor"
        private const val PROP_DOT_NINE_IMAGE = "dotNineImage"
        private const val CAP_INSETS = "capInsets"

        // Blur radius
        private const val BLUR_RADIUS = "blurRadius"

        // Placeholder image source
        private const val PLACEHOLDER = "placeholder"

        // Default blank placeholder image
        private const val DEFAULT_SRC =
            "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=="
    }
}
