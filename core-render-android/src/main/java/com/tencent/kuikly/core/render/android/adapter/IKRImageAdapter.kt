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

package com.tencent.kuikly.core.render.android.adapter

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.tencent.kuikly.core.render.android.KuiklyRenderViewContext
import com.tencent.kuikly.core.render.android.css.ktx.getDisplayMetrics
import org.json.JSONObject

/**
 * 图片加载适配器
 */
interface IKRImageAdapter {

    /**
     * 根据[HRImageLoadOption]来加载图片
     *
     * @param imageLoadOption 图片加载选项
     * @param callback 回调
     *
     * @return 是否加载此图片类型
     */
    fun fetchDrawable(imageLoadOption: HRImageLoadOption, callback: (drawable: Drawable?) -> Unit)

    /**
     * 根据[HRImageLoadOption]来加载图片
     *
     * @param imageLoadOption 图片加载选项
     * @param imageParams 图片额外参数
     * @param callback 回调
     *
     * @return 是否加载此图片类型
     */
    fun fetchDrawable(imageLoadOption: HRImageLoadOption, imageParams: JSONObject?, callback: (drawable: Drawable?) -> Unit) {
        fetchDrawable(imageLoadOption, callback)
    }

    /**
     * 是否需要等待首屏完成后再加载
     * 默认值为 true，当首屏完成后再加载
     */
    val shouldWaitViewDidLoad: Boolean
        get() = true

    /**
     * 返回图片的宽度px值，如果[fetchDrawable]返回图片经过了density缩放，可以重载此方法返回正确的px值
     */
    fun getDrawableWidth(kuiklyRenderViewContext: KuiklyRenderViewContext, drawable: Drawable): Float {
        // 为了兼容旧版，这里默认乘以density
        return drawable.intrinsicWidth.toFloat() * kuiklyRenderViewContext.getDisplayMetrics().density
    }

    /**
     * 返回图片的高度px值，如果[fetchDrawable]返回图片经过了density缩放，可以重载此方法返回正确的px值
     */
    fun getDrawableHeight(kuiklyRenderViewContext: KuiklyRenderViewContext, drawable: Drawable): Float {
        // 为了兼容旧版，这里默认乘以density
        return drawable.intrinsicHeight.toFloat() * kuiklyRenderViewContext.getDisplayMetrics().density
    }

}

data class HRImageLoadOption(
    var src: String,
    val requestWidth: Int,
    val requestHeight: Int,
    val needResize: Boolean,
    val scaleType: ImageView.ScaleType
) {

    companion object {
        const val SCHEME_ASSETS = "assets://"
        const val SCHEME_FILE = "file://"
        const val SCHEME_BASE64 = "data:image"
        const val SCHEME_HTTP = "http://"
        const val SCHEME_HTTPS = "https://"
    }

    fun isAssets(): Boolean {
        return src.startsWith(SCHEME_ASSETS)
    }

    fun isFile(): Boolean {
        return src.startsWith(SCHEME_FILE)
    }

    fun isBase64(): Boolean {
        return src.startsWith(SCHEME_BASE64)
    }

    fun isWebUrl(): Boolean {
        return src.startsWith(SCHEME_HTTP) || src.startsWith(SCHEME_HTTPS)
    }

}
