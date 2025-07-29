/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.attr.IImageAttr
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * 创建一个 ImageView 实例并添加到视图容器中。
 * @param init 一个 ImageView.() -> Unit 函数，用于初始化 ImageView 的属性和子视图。
 */
fun ViewContainer<*, *>.Image(init: ImageView.() -> Unit) {
    val imageView = createViewFromRegister(ViewConst.TYPE_IMAGE_CLASS_NAME) as? ImageView
    if (imageView != null) {
        addChild(imageView, init)
    } else {
        addChild(ImageView(), init)
    }
}

open class ImageAttr : Attr(), IImageAttr {
    internal var shouldWrapper = false

    /**
     * 设置图片源(支持http&base64&宿主扩展能力)。
     * @param src 图片源路径。
     * @param isDotNineImage 是否为 .9 图。
     * @return 返回 ImageAttr 以支持链式调用。
     */
    override fun src(src: String, isDotNineImage: Boolean): ImageAttr {
        if (isDotNineImage) {
            resizeStretch()
        }
        ImageConst.DOT_NINE_IMAGE with isDotNineImage.toInt()
        if (src.startsWith(ImageConst.BASE64_ICON_PREFIX)) {
            val cacheKey = if (isCacheKey(src)) src else generateCacheKey(src).also {
                val srcCacheKey = getPager().getValueForKey(it)
                if (srcCacheKey == null) {
                    getPager().setMemoryCache(it, src)
                }
            }
            ImageConst.SRC with cacheKey
        } else {
            ImageConst.SRC with src
        }
        return this
    }

    /**
     * 设置图片源。
     * @param uri ImageUri 对象。
     * @param isDotNineImage 是否为 .9 图。
     * @return 返回 IImageAttr 以支持链式调用。
     */
    override fun src(uri: ImageUri, isDotNineImage: Boolean): IImageAttr {
        return src(uri.toUrl(getPager().pageName), isDotNineImage)
    }

    /**
     * 设置图片占位图。
     * @param placeholder 占位图路径。
     * @return 返回 ImageAttr 以支持链式调用。
     */
    override fun placeholderSrc(placeholder: String): ImageAttr {
        ImageConst.PLACEHOLDER with placeholder
        shouldWrapper = true
        (view() as? ImageView)?.setClearPlaceholderHandler()
        return this
    }
    /**
     * 设置图片高斯模糊半径
     * @param blurRadius 模糊半径，取值[0-12.5]，一般为10f
     * @return 返回 ImageAttr 以支持链式调用。
     */
    override fun blurRadius(blurRadius: Float): IImageAttr {
        val radius = min( max(0f, blurRadius), 12.5f)
        ImageConst.BLUR_RADIUS with radius
        return this
    }
    /**
     * 将指定颜色应用于图像，生成一个新的已染色的图像。
     * @param color 要应用于图像的颜色。非透明部分将被此颜色覆盖。
     * @return 一个新的 ImageAttr 实例，其中包含已染色的图像。
     */
    override fun tintColor(color: Color?): IImageAttr {
        if (color == null) {
            ImageConst.TINT_COLOR with ""
        } else {
            ImageConst.TINT_COLOR with color.toString()
        }
        return this
    }

    /**
     * 设置图片组件鸿蒙平台上是否可以拖动
     */
    fun dragEnable(dragEnable: Boolean): Attr {
        ImageConst.DRAG_ENABLE with dragEnable.toInt()
        return this
    }

    /**
     *  设置图片组件的渐变遮罩（其渐变遮罩像素颜色的alpha值会应用在图片组件同位置像素的alpha上）
     */
    fun maskLinearGradient(
        direction: Direction,
        vararg colorStops: ColorStop
    ): IImageAttr {
        var cssLinearGradient = "linear-gradient(${direction.ordinal}"
        for (color in colorStops) {
            cssLinearGradient += ",$color"
        }
        cssLinearGradient += ")"
        ImageConst.MASK_LINEAR_GRADIENT with cssLinearGradient
        return this
    }

    /**
     * 设置图片拉伸模式：cover。
     * 在保持图片宽高比的情况下缩放图片，直到宽度和高度都大于等于组件的大小（超出部分将裁剪）。
     * @return 返回 ImageAttr 以支持链式调用。
     */
    override fun resizeCover(): ImageAttr {
        ImageConst.RESIZE with "cover"
        return this
    }

    /**
     * 设置图片拉伸模式：contain。
     * 在保持图片宽高比的情况下缩放图片，直到宽度和高度都小于等于组件的大小。
     * 这样图片完全被包裹在容器中，容器中可能留有空白（可完整显示图片内容，不裁剪）。
     * @return 返回 ImageAttr 以支持链式调用。
     */
    override fun resizeContain(): ImageAttr {
        ImageConst.RESIZE with "contain"
        return this
    }

    /**
     * 设置图片拉伸模式：stretch。
     * 拉伸图片并且不维持宽高比，直到宽高都刚好填满容器大小（图片可能变形）。
     * @return 返回 ImageAttr 以支持链式调用。
     */
    override fun resizeStretch(): ImageAttr {
        ImageConst.RESIZE with "stretch"
        return this
    }

    private fun generateCacheKey(base64Src: String): String {
        return ImageConst.BASE64_CACHE_KEY_PREFIX + base64Src.hashCode().toString()
    }

    private fun isCacheKey(base64Src: String): Boolean {
        return base64Src.startsWith(ImageConst.BASE64_CACHE_KEY_PREFIX)
    }

    override fun boxShadow(boxShadow: BoxShadow): Attr {
        shouldWrapper = true
        return super.boxShadow(boxShadow)
    }

    /**
     * 设置拉伸区域
     * @param top 距离上边偏移
     * @param left 距离左边偏移
     * @param bottom 距离下边偏移
     * @param right 距离右边偏移
     * @return ImageAttr 实例。
     */
    override fun capInsets(top: Float, left: Float, bottom: Float, right: Float): IImageAttr{
        val edge = EdgeInsets(top, left, bottom, right)
        ImageConst.CAP_INSETS with edge.toString()
        return this
    }
}

class ImageEvent : Event() {
    /**
     * 设置图片成功加载时的回调。
     * @param handler 一个函数，接收 LoadSuccessParams 参数，当图片成功加载时调用。
     */
    fun loadSuccess(handler: (LoadSuccessParams) -> Unit) {
        this.register(LOAD_SUCCESS) {
            handler(LoadSuccessParams.decode(it))
            (getView() as? ImageView)?.clearPlaceholder()
        }
    }

    /**
     * 设置图片分辨率获取成功时的回调。
     * @param handler 一个函数，接收 LoadResolutionParams 参数，当图片分辨率获取成功时调用。
     */
    fun loadResolution(handler: (LoadResolutionParams) -> Unit) {
        this.register(LOAD_RESOLUTION) {
            handler(LoadResolutionParams.decode(it))
        }
    }

    /**
     * 设置图片加载失败时的回调。
     * @param handler 一个函数，接收 LoadFailureParams 参数，当图片加载失败时调用。
     */
    fun loadFailure(handler: (LoadFailureParams) -> Unit) {
        this.register(LOAD_FAILURE) {
            handler(LoadFailureParams.decode(it))
        }
    }

    companion object {
        const val LOAD_SUCCESS = "loadSuccess"
        const val LOAD_FAILURE = "loadFailure"
        const val LOAD_RESOLUTION = "loadResolution"
    }
}

// 图片分辨率参数
data class LoadResolutionParams(
    val width: Int, // 分辨率实际像素宽大小
    val height: Int // 分辨率实际像素高大小
) {
    companion object {
        fun decode(params: Any?): LoadResolutionParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val width = tempParams.optInt("imageWidth", 0)
            val height = tempParams.optInt("imageHeight", 0)
            return LoadResolutionParams(width, height)
        }
    }
}
class ImageView : DeclarativeBaseView<ImageAttr, ImageEvent>() {

    override fun createAttr(): ImageAttr {
        return ImageAttr()
    }

    override fun createEvent(): ImageEvent {
        return ImageEvent()
    }

    override fun viewName(): String {
        if (attr.shouldWrapper && getPager().pageData.nativeBuild >= 1) {
            return ViewConst.TYPE_WRAPPER_IMAGE
        }
        return ViewConst.TYPE_IMAGE
    }

    internal fun clearPlaceholder() {
        didSetProp(ImageConst.PLACEHOLDER, "")
    }

    internal fun setClearPlaceholderHandler() {
        if (getViewEvent().handlerWithEventName(ImageEvent.LOAD_SUCCESS) == null) {
            getViewEvent().loadSuccess {
                clearPlaceholder()
            }
        }
    }
}

data class LoadSuccessParams(
    val src: String // 对应属性的 src
) {
    companion object {
        fun decode(params: Any?): LoadSuccessParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val src = tempParams.optString("src", "")
            return LoadSuccessParams(src)
        }
    }
}

data class LoadFailureParams(
    val src: String, // 对应属性的 src
    val errorCode: Int
) {
    companion object {
        fun decode(params: Any?): LoadFailureParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val src = tempParams.optString("src", "")
            val errorCode = tempParams.optInt("errorCode", -1)
            return LoadFailureParams(src, errorCode)
        }
    }
}

object ImageConst {
    const val SRC = "src"
    const val PLACEHOLDER = "placeholder"
    const val RESIZE = "resize"
    const val BLUR_RADIUS = "blurRadius"
    const val TINT_COLOR = "tintColor"
    const val MASK_LINEAR_GRADIENT = "maskLinearGradient"
    const val BASE64_ICON_PREFIX = "data:image"
    const val BASE64_CACHE_KEY_PREFIX = BASE64_ICON_PREFIX + "_Md5_"
    const val DOT_NINE_IMAGE = "dotNineImage"
    const val CAP_INSETS = "capInsets"

    const val RESIZE_MODE_COVER = "cover"
    const val RESIZE_MODE_CONTAIN = "contain"
    const val RESIZE_MODE_STRETCH = "stretch"

    const val DRAG_ENABLE = "dragEnable"
}