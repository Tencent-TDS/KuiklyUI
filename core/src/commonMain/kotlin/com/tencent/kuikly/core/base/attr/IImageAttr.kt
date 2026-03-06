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

package com.tencent.kuikly.core.base.attr

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Image 相关接口
 */
interface IImageAttr {
    /**
     * 设置图片src
     * @param src 数据源
     * @param isDotNineImage 是否为.9图
     * @return this
     */
    fun src(src: String, isDotNineImage: Boolean = false): IImageAttr

    fun src(uri: ImageUri, isDotNineImage: Boolean = false): IImageAttr

    fun src(src: String, imageParams: JSONObject?, isDotNineImage: Boolean = false): IImageAttr

    fun src(uri: ImageUri, imageParams: JSONObject?, isDotNineImage: Boolean = false): IImageAttr

    /**
     * 设置图片占位图。
     * @param placeholder 占位图路径。
     * @return 返回 ImageAttr 以支持链式调用。
     */
    fun placeholderSrc(placeholder: String): IImageAttr
    /**
     * 设置图片高斯模糊半径
     * @param blurRadius 模糊半径，取值[0,12.5]
     * @return 返回 ImageAttr 以支持链式调用。
     */
    fun blurRadius(blurRadius: Float): IImageAttr

    /**
     * 将指定颜色应用于图像，生成一个新的已染色的图像。
     * @param color 要应用于图像的颜色。非透明部分将被此颜色覆盖。如果为null，则可取消染色作用
     * @return 一个新的 ImageAttr 实例，其中包含已染色的图像。
     */
    fun tintColor(color: Color?): IImageAttr

    /*
      * 设置图片拉伸模式：cover
      * 在保持图片宽高比的情况下下缩放图片，直到宽度和高度都大于等于组件的大小（超出部分将裁剪）
      */
    fun resizeCover(): IImageAttr

    fun resizeContain(): IImageAttr

    fun resizeStretch(): IImageAttr

    /**
     * 设置图片拉伸模式：widthFix
     * 保持宽高比，宽度固定，高度自适应
     * 适用于需要图片宽度与容器宽度一致的场景
     */
    fun resizeWidthFix(): IImageAttr

    /**
     * 设置图片拉伸模式：heightFix
     * 保持宽高比，高度固定，宽度自适应
     * 适用于需要图片高度与容器高度一致的场景
     */
    fun resizeHeightFix(): IImageAttr

    /**
     * 设置拉伸区域
     * @param top 距离上边偏移
     * @param left 距离左边偏移
     * @param bottom 距离下边偏移
     * @param right 距离右边偏移
     * @return 一个新的 ImageAttr 实例。
     */
    fun capInsets(top: Float, left: Float, bottom: Float, right: Float): IImageAttr
}

class ImageUri private constructor(private val scheme: String, private val path: String){

    companion object {

        private const val PAGE_PLACEHOLDER = "#pageName#"

        const val SCHEME_COMMON_ASSETS = "assets://common/"
        const val SCHEME_PAGE_ASSETS = "assets://${PAGE_PLACEHOLDER}/"
        const val SCHEME_FILE = "file://"
        const val SCHEME_BASE64 = "data:image"

        /**
         * 生成 assets common 目录下的 Uri
         * eg. kuikly.png -> assets://common/kuikly.png
         */
        fun commonAssets(path: String): ImageUri {
            if (path.startsWith(SCHEME_COMMON_ASSETS)) {
                return ImageUri(path.substring(0, SCHEME_COMMON_ASSETS.length), path.substring(SCHEME_COMMON_ASSETS.length))
            }
            return ImageUri(SCHEME_COMMON_ASSETS, path)
        }

        /**
         * 生成 assets 对应 page 下的 Uri
         * eg. kuikly.png -> assets://#page#/kuikly.png
         * 在运行时，#page# 会被替换为 Image 所在的 page
         */
        fun pageAssets(path: String): ImageUri {
            if (path.startsWith(SCHEME_PAGE_ASSETS)) {
                return ImageUri(path.substring(0, SCHEME_PAGE_ASSETS.length), path.substring(SCHEME_PAGE_ASSETS.length))
            }
            return ImageUri(SCHEME_PAGE_ASSETS, path)
        }

        fun file(path: String): ImageUri {
            if (path.startsWith(SCHEME_FILE)) {
                return ImageUri(path.substring(0, SCHEME_FILE.length), path.substring(SCHEME_FILE.length))
            }
            return ImageUri(SCHEME_FILE, path)
        }

    }

    fun toUrl(pageName: String): String {
        val url = "${scheme}${path}"
        if (scheme == SCHEME_PAGE_ASSETS) {
            return url.replaceFirst(PAGE_PLACEHOLDER, pageName)
        }
        return url
    }

}