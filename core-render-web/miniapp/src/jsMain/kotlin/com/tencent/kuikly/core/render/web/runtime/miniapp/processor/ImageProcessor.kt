package com.tencent.kuikly.core.render.web.runtime.miniapp.processor

import com.tencent.kuikly.core.render.web.processor.IImageProcessor
import com.tencent.kuikly.core.render.web.ktx.toPxF
import org.w3c.dom.HTMLImageElement

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
    
    override fun isSVGFilterSupported(): Boolean {
        // MiniApp 环境不支持 SVG 滤镜
        return false
    }
    
    override fun applyTintColor(imageElement: HTMLImageElement, tintColorValue: String, rootWidth: Double) {
        // 小程序使用 CSS drop-shadow 实现
        if (rootWidth != 0.0 && tintColorValue.isNotEmpty()) {
            imageElement.style.borderBottom = "${rootWidth.toPxF()} solid transparent"
            imageElement.style.transform = "translate(0px, ${(-rootWidth).toPxF()})"
            imageElement.style.filter = "drop-shadow(0px ${rootWidth.toPxF()} 0px $tintColorValue)"
        } else {
            imageElement.style.filter = ""
        }
    }
}