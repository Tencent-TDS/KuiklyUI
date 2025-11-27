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

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * PAG scale mode, aligned with libpag's `PAGScaleMode`.
 *
 * This enum specifies how to scale the PAG content to fit the surface size.
 * The [value] of each enum item is the corresponding libpag numeric constant:
 * 0: NONE, 1: STRETCH, 2: LETTER_BOX, 3: ZOOM.
 *
 * Reference: https://github.com/Tencent/libpag
 * Default: [LETTER_BOX] (2)
 */
enum class PAGScaleMode(val value: Int) {
    /** Content is not scaled. The original size is used. */
    NONE(0),

    /** Stretches to fill without preserving aspect ratio (may distort). */
    STRETCH(1),

    /** Scales to fit while preserving aspect ratio (default, libpag default behavior). */
    LETTER_BOX(2),

    /** Scales to fill while preserving aspect ratio. Content may be cropped. */
    ZOOM(3);

    companion object {
        fun fromValue(value: Int): PAGScaleMode {
            return values().firstOrNull { it.value == value } ?: LETTER_BOX
        }
    }
}

/**
 * 创建一个 PAGView 实例并添加到视图容器中。
 * @param init 一个 PAGView.() -> Unit 函数，用于初始化 PAGView 的属性和子视图。
 */
fun ViewContainer<*, *>.PAG(init: PAGView.() -> Unit) {
    addChild(PAGView(), init)
}

class PAGViewAttr : Attr() {
    /**
     * 设置 PAGView 的源文件路径。
     * 支持 CDN URL 或本地文件路径。
     * @param src 源文件路径。
     */
    fun src(src: String) {
        SRC with src
    }

    /**
     * 设置 PAGView 的 Assets 资源文件路径。
     * 支持 commonMain/assets 资源路径
     * @param uri Assets 资源 uri。
     */
    fun src(uri: ImageUri) {
        SRC with uri.toUrl(getPager().pageName)
    }

    /**
     * 设置动画重复次数。
     * 默认值为 1，表示动画仅播放一次。0 表示动画将无限次播放。
     * @param repeatCount 动画重复次数。
     */
    fun repeatCount(repeatCount: Int) {
        REPEAT_COUNT with repeatCount
    }

    /**
     * 设置是否自动播放。
     * 默认值为 true。
     * @param play 布尔值，表示是否自动播放。
     */
    fun autoPlay(play: Boolean) {
        AUTO_PLAY with play.toInt()
    }

    /**
     * 替换当前 PAG 资源中的文字图层信息
     * @param layerName 目标图层名称
     * @param textContent 替换的文本内容
     */
    fun replaceTextLayerContent(layerName: String = "", textContent: String = "") {
        REPLACE_TEXT_LAYER_CONTENT with "$layerName,$textContent"
    }

    /**
     * 替换当前 PAG 资源中的图像图层信息
     * @param layerName 目标图层名称
     * @param imageFilePath 替换的图片资源文件路径
     */
    fun replaceImageLayerContent(layerName: String = "", imageFilePath: String = "") {
        REPLACE_IMAGE_LAYER_CONTENT with "$layerName,$imageFilePath"
    }

    /**
     * 替换当前 PAG 资源中的图像图层信息
     * @param layerName 目标图层名称
     * @param uri 替换的图片 Assets 资源 uri
     */
    fun replaceImageLayerContent(layerName: String = "", uri: ImageUri) {
        val imageFilePath = uri.toUrl(getPager().pageName)
        REPLACE_IMAGE_LAYER_CONTENT with "$layerName,$imageFilePath"
    }

    /**
     * 设置缩放模式，对齐 libpag 的 `PAGScaleMode`。
     *
     * @param mode 缩放模式枚举值：
     * [PAGScaleMode.NONE]、[PAGScaleMode.STRETCH]、[PAGScaleMode.LETTER_BOX]、[PAGScaleMode.ZOOM]
     */
    fun scaleMode(mode: PAGScaleMode) {
        SCALE_MODE with mode.value
    }

    /**
     * 使用 NONE 模式：不缩放，使用原始大小。
     */
    fun scaleModeNone() {
        scaleMode(PAGScaleMode.NONE)
    }

    /**
     * 使用 STRETCH 模式：拉伸填充，不保持宽高比（可能会变形）。
     */
    fun scaleModeStretch() {
        scaleMode(PAGScaleMode.STRETCH)
    }

    /**
     * 使用 LETTER_BOX 模式：按比例缩放以完整显示内容（默认行为）。
     */
    fun scaleModeLetterBox() {
        scaleMode(PAGScaleMode.LETTER_BOX)
    }

    /**
     * 使用 ZOOM 模式：按比例缩放以填满容器，内容可能会被裁剪。
     */
    fun scaleModeZoom() {
        scaleMode(PAGScaleMode.ZOOM)
    }

    companion object {
        const val SRC = "src"
        const val REPEAT_COUNT = "repeatCount"
        const val AUTO_PLAY = "autoPlay"
        const val SCALE_MODE = "scaleMode"
        const val REPLACE_TEXT_LAYER_CONTENT = "replaceTextLayerContent"
        const val REPLACE_IMAGE_LAYER_CONTENT = "replaceImageLayerContent"
    }
}

class PAGViewEvent : Event() {
    /**
     * 设置加载失败时的事件处理器。
     * @param handler 一个函数，当加载失败时调用。
     */
    fun loadFailure(handler: EventHandlerFn) {
        register("loadFailure", handler)
    }

    /**
     * 设置动画开始时的事件处理器。
     * @param handler 一个函数，当动画开始时调用。
     */
    fun animationStart(handler: EventHandlerFn) {
        register("animationStart", handler)
    }

    /**
     * 设置动画结束时的事件处理器。
     * @param handler 一个函数，当动画结束时调用。
     */
    fun animationEnd(handler: EventHandlerFn) {
        register("animationEnd", handler)
    }

    /**
     * 设置动画取消时的事件处理器。
     * @param handler 一个函数，当动画取消时调用。
     */
    fun animationCancel(handler: EventHandlerFn) {
        register("animationCancel", handler)
    }

    /**
     * 设置动画重复时的事件处理器。
     * @param handler 一个函数，当动画重复时调用。
     */
    fun animationRepeat(handler: EventHandlerFn) {
        register("animationRepeat", handler)
    }
}
class PAGView : DeclarativeBaseView<PAGViewAttr, PAGViewEvent>() {
    override fun createAttr(): PAGViewAttr = PAGViewAttr()

    override fun createEvent(): PAGViewEvent = PAGViewEvent()

    override fun viewName(): String = ViewConst.TYPE_PAG_VIEW
    /*
     * 播放动画（在attr.autoPlay为true时不需要手动调用该接口）
     */
    fun play() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("play", "")
        }
    }
    /*
     * 停止动画
     */
    fun stop() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("stop", "")
        }
    }

    /*
     * 设置播放的进度，有效值为0.0到1.0
     */
    fun setProgress(value: Float) {
        val jsonObject = JSONObject()
        jsonObject.put("value", value)
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setProgress", jsonObject.toString())
        }
    }

}
