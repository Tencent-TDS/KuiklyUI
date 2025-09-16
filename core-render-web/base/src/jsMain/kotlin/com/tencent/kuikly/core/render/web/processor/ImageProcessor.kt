package com.tencent.kuikly.core.render.web.processor

/**
 * common event processor
 */
interface IImageProcessor {
    /**
     * get image assets source
     */
    fun getImageAssetsSource(src: String): String
}