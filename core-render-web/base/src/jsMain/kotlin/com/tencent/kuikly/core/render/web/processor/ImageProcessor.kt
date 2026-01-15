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
     */
    fun applyTintColor(imageElement: HTMLImageElement, tintColorValue: String, rootWidth: Double)
}