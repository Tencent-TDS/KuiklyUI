package com.tencent.kuikly.core.render.web.runtime.web.expand.processor

import com.tencent.kuikly.core.render.web.processor.IImageProcessor
import com.tencent.kuikly.core.render.web.utils.Log
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.svg.SVGElement

/**
 * h5 image processor
 */
object ImageProcessor : IImageProcessor {
    private const val TAG = "ImageProcessor"
    // Assets image resource prefix, identifies assets resource images
    private const val ASSETS_IMAGE_PREFIX = "assets://"
    // Assets image resource path, identifies assets resource images
    private const val ASSETS_IMAGE_PATH = "assets/"
    // SVG namespace
    private const val SVG_NS = "http://www.w3.org/2000/svg"
    
    private lateinit var svgElement: SVGElement
    private var filterId = 0

    override fun getImageAssetsSource(src: String): String =
        src.replace(ASSETS_IMAGE_PREFIX, ASSETS_IMAGE_PATH)
    
    override fun isSVGFilterSupported(): Boolean {
        return try {
            // 确保 SVG 容器已初始化
            initSvgContainer()
            true
        } catch (e: Exception) {
            Log.error(TAG, e)
            false
        }
    }
    
    /**
     * 确保全局 SVG 容器存在
     */
    private fun initSvgContainer() {
        if (!::svgElement.isInitialized) {
            svgElement = document.createElementNS(SVG_NS, "svg").unsafeCast<SVGElement>().apply {
                setAttribute("width", "0")
                setAttribute("height", "0")
                setAttribute("style", "position: absolute; visibility: hidden;")
                document.body?.appendChild(this)
            }
        }
    }
    
    override fun applyTintColor(imageElement: HTMLImageElement, tintColorValue: String, rootWidth: Double) {
        if (tintColorValue.isEmpty()) {
            imageElement.style.filter = ""
            return
        }
        
        if (isSVGFilterSupported()) {
            // 使用 SVG 滤镜实现
            val currentFilterId = "tint-${filterId++}"
            
            // 创建新的滤镜（添加到共享的 SVG 容器中）
            createFilter(currentFilterId, tintColorValue, svgElement)
            
            // 应用滤镜到图片
            applyFilter(imageElement, currentFilterId)
        } else {
            // 如果浏览器不支持 SVG 滤镜，降级到 CSS filter
            applyTintColorWithCSSFilter(imageElement, tintColorValue)
        }
    }
    
    /**
     * Create SVG filter
     */
    private fun createFilter(id: String, color: String, svgElement: SVGElement) {
        // 创建 defs（如果不存在）
        var defs = svgElement.querySelector("defs")
        if (defs == null) {
            defs = document.createElementNS(SVG_NS, "defs")
            svgElement.appendChild(defs)
        }
        
        // 创建 SVG 滤镜
        val filter = document.createElementNS(SVG_NS, "filter")
        filter.setAttribute("id", id)

        // 创建 flood 元素来设置颜色
        val flood = document.createElementNS(SVG_NS, "feFlood")
        flood.setAttribute("flood-color", color)

        // 创建 composite 元素来混合颜色和图片
        val composite = document.createElementNS(SVG_NS, "feComposite")
        composite.setAttribute("in2", "SourceAlpha")
        composite.setAttribute("operator", "in")

        // 组装 SVG 结构
        filter.appendChild(flood)
        filter.appendChild(composite)
        defs.appendChild(filter)
    }

    /**
     * Remove all filters
     */
    private fun clearFilters(svgElement: SVGElement) {
        while (svgElement.firstChild != null) {
            svgElement.removeChild(svgElement.firstChild!!)
        }
    }    /**
     * Apply filter to an element
     */
    private fun applyFilter(imageElement: HTMLImageElement, filterId: String) {
        imageElement.style.filter = "url(#$filterId)"
        
        // 修复 iOS 15.2 Safari SVG 滤镜不立即生效的问题
        forceRerender(imageElement)
    }
    
    /**
     * 强制重新渲染元素，修复 iOS 15.2 Safari SVG 滤镜渲染问题
     */
    private fun forceRerender(imageElement: HTMLImageElement) {
        val originalFilter = imageElement.style.filter
        imageElement.style.filter = ""
        
        window.setTimeout({
            imageElement.style.filter = originalFilter
        }, 0)
    }
    
    /**
     * 使用 CSS filter 实现着色效果（降级方案）
     */
    private fun applyTintColorWithCSSFilter(imageElement: HTMLImageElement, tintColorValue: String) {
        imageElement.style.filter = "brightness(0) saturate(100%) drop-shadow(0 0 0 $tintColorValue)"
    }
}