package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.IKuiklyRenderContext
import com.tencent.kuikly.core.render.web.expand.module.KRMemoryCacheModule
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
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
        })
        // Hide itself when image loading fails
        imageElement.addEventListener("error", {
            imageElement.style.display = "none"
            loadFailureCallback?.invoke(mapOf(
                SRC to imageElement.src,
                // web can not get error code, return -10001
                ERROR_CODE to ERROR_UNKNOWN
            ))
        })
    }.unsafeCast<HTMLImageElement>()

    protected val div = kuiklyDocument.createElement(ElementType.DIV).apply {
        val divElement = this.unsafeCast<HTMLDivElement>()
        divElement.style.overflowX = "hidden"
        divElement.style.overflowY = "hidden"

        appendChild(image)
    }

    private var tintColorValue = ""
    private var rootWidth = 0.0
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
                rootWidth = frame.width
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
        if (rootWidth != 0.0 && tintColorValue != "") {
            image.style.borderBottom = "${this.rootWidth.toPxF()} solid transparent"
            image.style.transform = "translate(0px, ${(-this.rootWidth).toPxF()})"
            image.style.filter =
                "drop-shadow(0px ${this.rootWidth.toPxF()} 0px ${this.tintColorValue.toRgbColor()})"
        }
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
     * Set image src data
     */
    private fun setSrc(src: String) {
        // Set when image src is not empty, otherwise use default transparent image
        if (src != "") {
            if (isAssetsSrc(src)) {
                // If it's an assets resource image, remove assets prefix and replace with assets path
                image.src = KuiklyProcessor.imageProcessor.getImageAssetsSource(src)
            } else if (isBase64Src(src)) {
                // If base64, read data from memory cache module and return
                val base64Image = getBase64Image(src)
                if (base64Image != null) {
                    image.src = base64Image
                }
            } else {
                // Otherwise directly set image link
                image.src = src
            }
        }
    }

    /**
     * Set image stretch mode
     */
    private fun setResize(propValue: Any) {
        // Adapt image stretch mode, no stretch in DOM, set to fill
        image.style.objectFit = when (propValue.unsafeCast<String>()) {
            "stretch" -> "fill"
            else -> propValue.unsafeCast<String>()
        }
    }

    /**
     * Check if the given image source is an assets resource or file resource
     */
    private fun isAssetsSrc(src: String): Boolean = src.startsWith(ASSETS_IMAGE_PREFIX) ||
            src.startsWith(FILE_IMAGE_PREFIX)

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

        // Blur radius
        private const val BLUR_RADIUS = "blurRadius"

        // Default blank placeholder image
        private const val DEFAULT_SRC =
            "data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=="

        // Assets image resource prefix, identifies assets resource images
        private const val ASSETS_IMAGE_PREFIX = "assets://"
        private const val FILE_IMAGE_PREFIX = "file://"
    }
}
