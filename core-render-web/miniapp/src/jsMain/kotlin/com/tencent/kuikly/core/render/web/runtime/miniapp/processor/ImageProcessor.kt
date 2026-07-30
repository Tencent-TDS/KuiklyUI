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
    
    override fun applyTintColor(imageElement: HTMLImageElement, tintColorValue: String, frameHeight: Double) {
        // 小程序使用 CSS drop-shadow 实现：先用 translate 把原图向上推出容器，
        // 再通过 drop-shadow 在原位置投下等高的纯色阴影，从而达到按 alpha 染色的效果。
        // Y 方向的偏移量必须使用 frame 的高度，否则当图片宽高不等时会出现：
        //   - 宽 > 高：阴影被底部裁切
        //   - 宽 < 高：原图未被完全推出容器，与阴影叠加显示
        if (frameHeight != 0.0 && tintColorValue.isNotEmpty()) {
            imageElement.style.borderBottom = "${frameHeight.toPxF()} solid transparent"
            imageElement.style.transform = "translate(0px, ${(-frameHeight).toPxF()})"
            imageElement.style.filter = "drop-shadow(0px ${frameHeight.toPxF()} 0px $tintColorValue)"
        } else {
            imageElement.style.filter = ""
        }
    }
}