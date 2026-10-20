/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. KuiklyUI is licensed under the terms of
 * the KuiklyUI license, and you may not use this file except in compliance
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
 * 创建一个鸿蒙「沉浸光感」容器（Immersive Light Sense）。
 *
 * 该容器在 OHOS 侧走 ArkTS 转发通道（V2）：容器本体渲染为 Navigation，
 * 子视图会被挂进 Navigation 标题栏区域的 ContentSlot，因此落在系统规定的
 * 沉浸光感生效区域内——即便子视图是 Kuikly 自绘节点，也能正常渲染沉浸材质。
 *
 * 平台说明：沉浸光感与 iOS 的液态玻璃是两条独立赛道（视觉表现、生效范围、API 都不同），
 * 不做跨端统一，鸿蒙单独提供这一套能力。
 *
 * 使用约束：
 * 1. 只有放在本容器内的组件才能获得沉浸光感，容器外的页面组件不受影响；
 * 2. 材质要设在**子视图**上（见 [com.tencent.kuikly.core.base.Attr.immersiveMaterial]）；
 * 3. 子视图背景需保持透明，否则会遮挡材质层；
 * 4. 语义上它是页面的标题栏，与自绘标题栏二选一，不要同时存在；
 * 5. 官方建议控制材质面积，且不要嵌套材质容器。
 *
 * 不支持沉浸光感的设备上，本容器会自动降级为普通容器（子视图照常显示，只是没有材质），
 * 业务无需额外写降级分支。
 */
fun ViewContainer<*, *>.ImmersiveLightSenseOhos(init: ImmersiveLightSenseOhosView.() -> Unit) {
    ImmersiveLightSenseOhosView().also {
        addChild(it, init)
    }
}

open class ImmersiveLightSenseOhosView : GroupView<ImmersiveLightSenseOhosAttr, ImmersiveLightSenseOhosEvent>() {

    override fun createAttr(): ImmersiveLightSenseOhosAttr {
        return ImmersiveLightSenseOhosAttr()
    }

    override fun createEvent(): ImmersiveLightSenseOhosEvent {
        return ImmersiveLightSenseOhosEvent()
    }

    override fun viewName(): String {
        return VIEW_NAME
    }

    companion object {
        const val VIEW_NAME = "KRImmersiveLightSenseView"
    }
}

/**
 * 沉浸光感容器的属性。
 *
 * 通用布局属性（宽高、padding、margin、对齐等）中，**高度**即系统标题栏高度，
 * 决定子视图在标题栏内的可用区域。
 */
class ImmersiveLightSenseOhosAttr : GroupAttr() {

    /**
     * 标题栏区域本身的沉浸光感材质（"玻璃底"）。
     *
     * 注意落点：材质**不是**设在容器节点上（容器节点不在系统规定的生效区域内），
     * 而是设在标题栏内承载子视图的那一层上，因此表现为"整个标题栏区域有一层玻璃底"。
     * 子视图可以再各自调用 [immersiveMaterial] 覆盖，做玻璃底之上的玻璃元素。
     *
     * 参数与 [immersiveMaterial] 完全一致：
     * @param style 材质厚薄：0=ULTRA_THIN 1=THIN 2=REGULAR 3=THICK 4=ULTRA_THICK
     * @param materialColor 材质层赋色，必须带透明度，不透明色会盖死材质
     * @param interactive 按压时材质产生弹性形变
     * @param applyShadow 是否使用材质自带阴影（true 时优先于 shadow 通用属性）
     */
    fun immersiveMaterial(
        style: Int = 2,
        interactive: Boolean? = null,
        applyShadow: Boolean? = null,
        materialColor: Color? = null
    ) {
        immersiveMaterial(
            enable = true,
            style = style,
            interactive = interactive,
            applyShadow = applyShadow,
            materialColor = materialColor
        )
    }
}

/**
 * 沉浸光感容器的事件。
 * 目前不需要额外事件，独立出类型是为了后续 API 演进时不破坏业务侧写法。
 */
class ImmersiveLightSenseOhosEvent : GroupEvent()
