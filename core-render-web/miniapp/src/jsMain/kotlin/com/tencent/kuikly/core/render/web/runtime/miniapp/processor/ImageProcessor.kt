package com.tencent.kuikly.core.render.web.runtime.miniapp.processor

import com.tencent.kuikly.core.render.web.processor.IImageProcessor

/**
 * mini app image processor
 */
object ImageProcessor : IImageProcessor {
    // file image resource prefix, identifies file resource images
    private const val SCHEME_FILE = "file://"
    // Assets image resource prefix, identifies assets resource images
    private const val SCHEME_ASSETS = "assets://"

    override fun getImageAssetsSource(src: String): String =
        src.replace(Regex("^($SCHEME_FILE|$SCHEME_ASSETS)"), "/assets/")
}