package com.tencent.kuikly.core.render.web.runtime.web.expand.processor

import com.tencent.kuikly.core.render.web.processor.IImageProcessor

/**
 * h5 image processor
 */
object ImageProcessor : IImageProcessor {
    // Assets image resource prefix, identifies assets resource images
    private const val ASSETS_IMAGE_PREFIX = "assets://"
    // Assets image resource path, identifies assets resource images
    private const val ASSETS_IMAGE_PATH = "assets/"

    override fun getImageAssetsSource(src: String): String =
        src.replace(ASSETS_IMAGE_PREFIX, ASSETS_IMAGE_PATH)
}