/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. KuiklyUI is licensed under the terms of
 * the KuiklyUI license, and you may not use it except in compliance
 * with the License. You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.views.ohos.immersive

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView

/**
 * 创建一个鸿蒙原生 Slider（ArkUI Slider）。
 *
 * 与自绘 Slider 的区别：官方规定「Slider / Toggle / Select / 弹窗类组件」可**全页面生效**
 * 沉浸光感，不受「必须在 Navigation 标题栏或底部 TabBar 内」的位置限制。
 * Kuikly 自绘的 Slider 是普通自绘节点，不在这个白名单里，所以拿不到材质 ——
 * 只有换成真正的 ArkUI Slider 才能享受沉浸光感。
 *
 * 用法：
 * ```
 * ImmersiveLightSenseSliderOhos {
 *     attr {
 *         size(200f, 40f)
 *         value(0.5f)
 *         immersiveMaterial(style = 2)   // 可选，不设则无材质（普通原生滑块）
 *     }
 *     event {
 *         valueChanged { v -> }
 *     }
 * }
 * ```
 */
fun ViewContainer<*, *>.ImmersiveLightSenseSliderOhos(init: ImmersiveLightSenseSliderOhosView.() -> Unit) {
    ImmersiveLightSenseSliderOhosView().also {
        addChild(it, init)
    }
}

open class ImmersiveLightSenseSliderOhosView : GroupView<ImmersiveLightSenseSliderOhosAttr, ImmersiveLightSenseSliderOhosEvent>() {

    override fun createAttr(): ImmersiveLightSenseSliderOhosAttr = ImmersiveLightSenseSliderOhosAttr()

    override fun createEvent(): ImmersiveLightSenseSliderOhosEvent = ImmersiveLightSenseSliderOhosEvent()

    override fun viewName(): String = VIEW_NAME

    companion object {
        /**
         * wire 名：必须与原生侧注册名一致（cpp ComponentsRegisterEntry 注册的是
         * "KROhosSliderView"）。类名/DSL 名可以改，这个字符串不能跟着实名走。
         */
        const val VIEW_NAME = "KROhosSliderView"
    }
}

/**
 * 鸿蒙原生 Slider 的属性。
 *
 * 注意：材质参数用基类 [com.tencent.kuikly.core.base.Attr.immersiveMaterial]，
 * 不在本类重复提供 —— 沉浸光感统一挂在基类上，所有组件共用一套入口。
 */
class ImmersiveLightSenseSliderOhosAttr : GroupAttr() {

    /** 当前值，落在 [min, max] 区间内 */
    fun value(value: Float): ImmersiveLightSenseSliderOhosAttr {
        KEY_VALUE with value
        return this
    }

    /** 最小值，默认 0 */
    fun min(min: Float): ImmersiveLightSenseSliderOhosAttr {
        KEY_MIN with min
        return this
    }

    /** 最大值，默认 100 */
    fun max(max: Float): ImmersiveLightSenseSliderOhosAttr {
        KEY_MAX with max
        return this
    }

    /** 进度颜色（已滑过部分），对齐 iOS 液态玻璃 iOSSlider.progressColor */
    fun progressColor(color: Color): ImmersiveLightSenseSliderOhosAttr {
        KEY_PROGRESS_COLOR with color.toString()
        return this
    }

    /** 轨道底色（未滑过部分），对齐 iOSSlider.trackColor */
    fun trackColor(color: Color): ImmersiveLightSenseSliderOhosAttr {
        KEY_TRACK_COLOR with color.toString()
        return this
    }

    /** 滑块颜色，对齐 iOSSlider.thumbColor */
    fun thumbColor(color: Color): ImmersiveLightSenseSliderOhosAttr {
        KEY_THUMB_COLOR with color.toString()
        return this
    }

    /** 轨道粗细，对齐 iOSSlider.trackThickness */
    fun trackThickness(thickness: Float): ImmersiveLightSenseSliderOhosAttr {
        KEY_TRACK_THICKNESS with thickness
        return this
    }

    companion object {
        private const val KEY_VALUE = "value"
        private const val KEY_MIN = "min"
        private const val KEY_MAX = "max"
        private const val KEY_PROGRESS_COLOR = "progressColor"
        private const val KEY_TRACK_COLOR = "trackColor"
        private const val KEY_THUMB_COLOR = "thumbColor"
        private const val KEY_TRACK_THICKNESS = "trackThickness"
    }
}

/** 鸿蒙原生 Slider 的事件。 */
class ImmersiveLightSenseSliderOhosEvent : GroupEvent() {

    internal var valueChangedHandlerFn: ((Float) -> Unit)? = null

    /**
     * 拖动导致值变化。回传的是当前值。
     *
     * 原生侧以 KRRenderValueMap 回传（key = "value"），这里两种形态都兼容：
     * 既支持 Map 取值，也退回直接 toString，避免桥接层形态变化导致解析失败。
     */
    fun valueChanged(handler: (Float) -> Unit) {
        valueChangedHandlerFn = handler
        register(KEY_VALUE_CHANGED) { params ->
            val raw = if (params is Map<*, *>) params["value"]?.toString() else params?.toString()
            handler(raw?.toFloatOrNull() ?: 0f)
        }
    }

    companion object {
        private const val KEY_VALUE_CHANGED = "valueChanged"
    }
}
