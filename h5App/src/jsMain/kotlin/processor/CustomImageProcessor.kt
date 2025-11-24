package processor

import com.tencent.kuikly.core.render.web.processor.IImageProcessor
import com.tencent.kuikly.core.render.web.runtime.web.expand.processor.ImageProcessor as BaseImageProcessor
import org.w3c.dom.HTMLImageElement

/**
 * Custom image processor for CDN assets
 * 自定义图片处理器，将 assets 资源指向 CDN
 */
object CustomImageProcessor : IImageProcessor {
    // Assets image resource prefix, identifies assets resource images
    private const val ASSETS_IMAGE_PREFIX = "assets://"
    // CDN base URL for static assets
    // 静态资源 CDN 地址，可根据环境配置修改
    private const val CDN_BASE_URL = "https://custom.com/assets/"

    override fun getImageAssetsSource(src: String): String {
        // Replace assets:// prefix with CDN URL
        return src.replace(ASSETS_IMAGE_PREFIX, CDN_BASE_URL)
    }

    override fun isSVGFilterSupported(): Boolean {
        return BaseImageProcessor.isSVGFilterSupported()
    }

    override fun applyTintColor(imageElement: HTMLImageElement, tintColorValue: String, rootWidth: Double) {
        BaseImageProcessor.applyTintColor(imageElement, tintColorValue, rootWidth)
    }
}
