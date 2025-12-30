package com.example.kuiklyworkwithminiapp

import com.example.kuiklyworkwithminiapp.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.module.ImageRef
import com.tencent.kuikly.core.module.MemoryCacheModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Row

private const val PAGE_NAME = "image_adapter"

@Page(PAGE_NAME)
internal class ImageAdapterStandardTest : BasePager() {

    companion object {
        private const val BASE64_SRC =
            "iVBORw0KGgoAAAANSUhEUgAAAIQAAACEAQMAAABrihHkAAAABlBMVEUAAAD///+l2Z/dAAAAL0lEQVRIx2MAgf9Q8AHEGRUZFSFOBM6DyfCDREdFRkVGRUZFhpjIYCtXR0WGlAgAIh9YHRjOdfwAAAAASUVORK5CYII="
        private const val STANDARD_WIDTH = 132f
        private const val STANDARD_HEIGHT = 132f
    }

    private var base64Result by observable<Boolean?>(null)
    private var base64Resolution by observable("")
    private var base64CacheReady by observable(false)
    private lateinit var base64Cache: ImageRef

    private var assetsResult by observable<Boolean?>(null)
    private var assetsResolution by observable("")
    private var assetsCacheReady by observable(false)
    private lateinit var assetsCache: ImageRef

    private var httpResult by observable<Boolean?>(null)
    private var httpResolution by observable("")
    private var httpCacheReady by observable(false)
    private lateinit var httpCache: ImageRef

    private var gifResult by observable<Boolean?>(null)
    private var gifResolution by observable("")
    private var gifCacheReady by observable(false)
    private lateinit var gifCache: ImageRef

    override fun created() {
        super.created()
        val mcm = acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)
        mcm.cacheImage("data:image/png;base64,$BASE64_SRC", false) {
            if (it.state == "Complete" && it.errorCode == 0) {
                base64Cache = ImageRef(it.cacheKey)
                base64CacheReady = true
            }
        }
        mcm.cacheImage(ImageUri.pageAssets("sample.png").toUrl(PAGE_NAME), false) {
            if (it.state == "Complete" && it.errorCode == 0) {
                assetsCache = ImageRef(it.cacheKey)
                assetsCacheReady = true
            }
        }
        mcm.cacheImage(
            "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/21e7b9c2.png",
            false
        ) {
            if (it.state == "Complete" && it.errorCode == 0) {
                httpCache = ImageRef(it.cacheKey)
                httpCacheReady = true
            }
        }
        mcm.cacheImage(
            "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/2963d536.gif",
            false
        ) {
            if (it.state == "Complete" && it.errorCode == 0) {
                gifCache = ImageRef(it.cacheKey)
                gifCacheReady = true
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            RouterNavBar {
                attr {
                    title = "ImageAdapter基准测试"
                }
            }
            Scroller {
                attr {
                    flex(1f)
                    padding(10f)
                }
                // 1. base64
                Text {
                    attr { text("1. base64");fontSize(16f);fontWeightBold() }
                }
                Row {
                    View {
                        attr {
                            size(200f, 150f)
                        }
                        Image {
                            attr {
                                positionAbsolute()
                                size(180f, 120f)
                                capInsets(32f, 32f, 32f, 32f)
                                resizeStretch()
                                src("data:image/png;base64,$BASE64_SRC")
                            }
                            event {
                                loadResolution {
                                    ctx.base64Resolution = "${it.width}x${it.height}"
                                    ctx.base64Result =
                                        it.width == STANDARD_WIDTH.toInt() && it.height == STANDARD_HEIGHT.toInt()
                                }
                                loadFailure {
                                    ctx.base64Result = false
                                }
                            }
                        }
                        View {
                            attr {
                                absolutePosition(left = 32f, top = 32f)
                                size(180f - 64f, 120f - 64f)
                                backgroundColor(Color(0x990000ff))
                            }
                        }
                    }
                    Canvas({
                        attr {
                            size(150f, 150f)
                        }
                    }) { context, _, _ ->
                        if (ctx.base64CacheReady) {
                            context.drawImage(ctx.base64Cache, 0f, 0f)
                        }
                        context.beginPath()
                        context.moveTo(0f, 0f)
                        context.lineTo(STANDARD_WIDTH, 0f)
                        context.lineTo(STANDARD_WIDTH, STANDARD_HEIGHT)
                        context.lineTo(0f, STANDARD_HEIGHT)
                        context.closePath()
                        context.fillStyle(Color(0x99ff0000))
                        context.fill()
                    }
                }
                Check("resolution测试") { ctx.base64Result }
                Check("capInset测试，查看蓝色方块是否与图片中心黑色区域重合")
                Check("drawImage测试，查看红色方块是否与图片重合")

                // 2. assets
                View { attr { height(20f) } }
                Text {
                    attr { text("2. assets");fontSize(16f);fontWeightBold() }
                }
                Row {
                    View {
                        attr {
                            size(200f, 150f)
                        }
                        Image {
                            attr {
                                positionAbsolute()
                                size(180f, 120f)
                                capInsets(32f, 32f, 32f, 32f)
                                resizeStretch()
                                src(ImageUri.pageAssets("sample.png"))
                            }
                            event {
                                loadResolution {
                                    ctx.assetsResolution = "${it.width}x${it.height}"
                                    ctx.assetsResult =
                                        it.width == STANDARD_WIDTH.toInt() && it.height == STANDARD_HEIGHT.toInt()
                                }
                                loadFailure {
                                    ctx.assetsResult = false
                                }
                            }
                        }
                        View {
                            attr {
                                absolutePosition(left = 32f, top = 32f)
                                size(180f - 64f, 120f - 64f)
                                backgroundColor(Color(0x990000ff))
                            }
                        }
                    }
                    Canvas({
                        attr {
                            size(150f, 150f)
                        }
                    }) { context, _, _ ->
                        if (ctx.assetsCacheReady) {
                            context.drawImage(ctx.assetsCache, 0f, 0f)
                        }
                        context.beginPath()
                        context.moveTo(0f, 0f)
                        context.lineTo(STANDARD_WIDTH, 0f)
                        context.lineTo(STANDARD_WIDTH, STANDARD_HEIGHT)
                        context.lineTo(0f, STANDARD_HEIGHT)
                        context.closePath()
                        context.fillStyle(Color(0x99ff0000))
                        context.fill()
                    }
                }
                Check("resolution测试") { ctx.assetsResult }
                Check("capInset测试，查看蓝色方块是否与图片中心黑色区域重合")
                Check("drawImage测试，查看红色方块是否与图片重合")

                // 3. http/https
                View { attr { height(20f) } }
                Text {
                    attr { text("3. http/https");fontSize(16f);fontWeightBold() }
                }
                Row {
                    View {
                        attr {
                            size(200f, 150f)
                        }
                        Image {
                            attr {
                                positionAbsolute()
                                size(180f, 120f)
                                capInsets(32f, 32f, 32f, 32f)
                                resizeStretch()
                                src("https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/21e7b9c2.png")
                            }
                            event {
                                loadResolution {
                                    ctx.httpResolution = "${it.width}x${it.height}"
                                    ctx.httpResult =
                                        it.width == STANDARD_WIDTH.toInt() && it.height == STANDARD_HEIGHT.toInt()
                                }
                                loadFailure {
                                    ctx.httpResult = false
                                }
                            }
                        }
                        View {
                            attr {
                                absolutePosition(left = 32f, top = 32f)
                                size(180f - 64f, 120f - 64f)
                                backgroundColor(Color(0x990000ff))
                            }
                        }
                    }
                    Canvas({
                        attr {
                            size(150f, 150f)
                        }
                    }) { context, _, _ ->
                        if (ctx.httpCacheReady) {
                            context.drawImage(ctx.httpCache, 0f, 0f)
                        }
                        context.beginPath()
                        context.moveTo(0f, 0f)
                        context.lineTo(STANDARD_WIDTH, 0f)
                        context.lineTo(STANDARD_WIDTH, STANDARD_HEIGHT)
                        context.lineTo(0f, STANDARD_HEIGHT)
                        context.closePath()
                        context.fillStyle(Color(0x99ff0000))
                        context.fill()
                    }
                }
                Check("resolution测试") { ctx.httpResult }
                Check("capInset测试，查看蓝色方块是否与图片中心黑色区域重合")
                Check("drawImage测试，查看红色方块是否与图片重合")

                // 4. gif
                View { attr { height(20f) } }
                Text {
                    attr { text("4. gif");fontSize(16f);fontWeightBold() }
                }
                Row {
                    View {
                        attr {
                            size(200f, 150f)
                        }
                        Image {
                            attr {
                                positionAbsolute()
                                size(180f, 120f)
                                capInsets(32f, 32f, 32f, 32f)
                                resizeStretch()
                                src("https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/2963d536.gif")
                            }
                            event {
                                loadResolution {
                                    ctx.gifResolution = "${it.width}x${it.height}"
                                    ctx.gifResult =
                                        it.width == STANDARD_WIDTH.toInt() && it.height == STANDARD_HEIGHT.toInt()
                                }
                                loadFailure {
                                    ctx.gifResult = false
                                }
                            }
                        }
                        View {
                            attr {
                                absolutePosition(left = 32f, top = 32f)
                                size(180f - 64f, 120f - 64f)
                                backgroundColor(Color(0x990000ff))
                            }
                        }
                    }
                    Canvas({
                        attr {
                            size(150f, 150f)
                        }
                    }) { context, _, _ ->
                        if (ctx.gifCacheReady) {
                            context.drawImage(ctx.gifCache, 0f, 0f)
                        }
                        context.beginPath()
                        context.moveTo(0f, 0f)
                        context.lineTo(STANDARD_WIDTH, 0f)
                        context.lineTo(STANDARD_WIDTH, STANDARD_HEIGHT)
                        context.lineTo(0f, STANDARD_HEIGHT)
                        context.closePath()
                        context.fillStyle(Color(0x99ff0000))
                        context.fill()
                    }
                }
                Check("resolution测试") { ctx.gifResult }
                Check("capInset测试，查看蓝色方块是否与图片中心黑色区域重合")
                Check("drawImage测试，查看红色方块是否与图片重合")
            }
        }
    }
}

private fun ViewContainer<*, *>.Check(text: String, pass: (() -> Boolean?)? = null) {
    RichText {
        Span { fontSize(12f);text("- $text ") }
        if (pass?.invoke() == true) {
            Span { color(Color.GREEN);text("✓") }
        } else if (pass?.invoke() == false) {
            Span { color(Color.RED);text("✗") }
        }
    }
}