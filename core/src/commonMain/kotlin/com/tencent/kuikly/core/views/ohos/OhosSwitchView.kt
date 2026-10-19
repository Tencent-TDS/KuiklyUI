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

package com.tencent.kuikly.core.views.ohos

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView

/**
 * 创建一个鸿蒙原生 Switch（ArkUI Toggle / ToggleType.Switch）。
 *
 * 与自绘 Switch 的区别：官方规定「Slider / Toggle / Select / 弹窗类组件」可**全页面生效**
 * 沉浸光感，不受「必须在 Navigation 标题栏或底部 TabBar 内」的位置限制。
 * Kuikly 自绘的 Switch 是普通自绘节点，不在这个白名单里，所以拿不到材质 ——
 * 只有换成真正的 ArkUI Toggle 才能享受沉浸光感。
 *
 * 用法：
 * ```
 * OhosSwitch {
 *     attr {
 *         size(51f, 31f)
 *         isOn(true)
 *         immersiveMaterial(style = 2)   // 可选，不设则无材质（普通原生开关）
 *     }
 *     event {
 *         switchChanged { on -> }
 *     }
 * }
 * ```
 */
fun ViewContainer<*, *>.OhosSwitch(init: OhosSwitchView.() -> Unit) {
    OhosSwitchView().also {
        addChild(it, init)
    }
}

open class OhosSwitchView : GroupView<OhosSwitchAttr, OhosSwitchEvent>() {

    override fun createAttr(): OhosSwitchAttr = OhosSwitchAttr()

    override fun createEvent(): OhosSwitchEvent = OhosSwitchEvent()

    override fun viewName(): String = VIEW_NAME

    companion object {
        const val VIEW_NAME = "KROhosSwitchView"
    }
}

/**
 * 鸿蒙原生 Switch 的属性。
 *
 * 注意：材质参数用基类 [com.tencent.kuikly.core.base.Attr.immersiveMaterial]。
 */
class OhosSwitchAttr : GroupAttr() {

    /** 开关状态 */
    fun isOn(isOn: Boolean): OhosSwitchAttr {
        KEY_IS_ON with if (isOn) 1 else 0
        return this
    }

    companion object {
        private const val KEY_IS_ON = "isOn"
    }
}

/** 鸿蒙原生 Switch 的事件。 */
class OhosSwitchEvent : GroupEvent() {

    internal var switchChangedHandlerFn: ((Boolean) -> Unit)? = null

    /**
     * 开关状态变化，回传新的开关状态。
     *
     * 原生侧以 KRRenderValueMap 回传（key = "isOn"，值 "1"/"0"），两种形态都兼容。
     */
    fun switchChanged(handler: (Boolean) -> Unit) {
        switchChangedHandlerFn = handler
        register(KEY_SWITCH_CHANGED) { params ->
            val raw = if (params is Map<*, *>) params["isOn"]?.toString() else params?.toString()
            handler(raw == "1" || raw == "true")
        }
    }

    companion object {
        private const val KEY_SWITCH_CHANGED = "switchChanged"
    }
}
