package com.tencent.kuikly.core.render.web.processor

import org.w3c.dom.HTMLImageElement

/**
 * common event processor
 */
interface IImageProcessor {
    /**
     * get image assets source
     */
    fun getImageAssetsSource(src: String): String
    
    /**
     * Check if SVG filter is supported
     */
    fun isSVGFilterSupported(): Boolean

    /**
     * Apply tint color to image element
     *
     * @param frameHeight current image view frame height in px, used by
     * implementations that need a geometric offset (e.g. the miniapp
     * drop-shadow based implementation translates the image by this value
     * on the Y axis).
     */
    fun applyTintColor(imageElement: HTMLImageElement, tintColorValue: String, frameHeight: Double)
}