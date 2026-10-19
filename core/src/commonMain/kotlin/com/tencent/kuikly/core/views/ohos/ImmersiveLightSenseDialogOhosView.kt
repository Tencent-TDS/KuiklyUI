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

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.ModalEvent

/**
 * 创建一个鸿蒙「沉浸光感」弹窗（Immersive Light Sense Dialog）。
 *
 * 与通用 [com.tencent.kuikly.core.views.Modal] 的区别：本组件在鸿蒙侧走原生弹窗
 * （ArkUI_NativeDialog）并**默认启用沉浸光感材质**，业务不需要也无法关闭 ——
 * 因为材质生效的前提是弹窗背板使用系统样式（customStyle=false，宽度走栅格、圆角固定），
 * 这一副作用由组件语义固化，不开放给业务配置。
 *
 * 弹窗类组件在官方白名单内，可**全页面生效**沉浸光感，不受「必须在 Navigation
 * 标题栏或底部 TabBar 内」的位置限制。
 *
 * 命名体现「沉浸光感 + Dialog」，与标题栏容器 [ImmersiveLightSenseOhos] 同属一套体系。
 *
 * 属性覆盖原则：**只暴露通用 Dialog 本来就有的能力**，不引入鸿蒙独有开关。
 *   - 尺寸：通用 width / height → 弹窗背板尺寸
 *   - 圆角：通用 borderRadius → 弹窗背板圆角
 *   - 背景色：通用 backgroundColor → 弹窗背板背景色
 *   - 材质：immersiveMaterial()，不设置时也有默认档位（REGULAR）
 *
 * 已验证限制：**背板 border 无效** —— 启用材质后背板描边归系统材质管，
 * 官方未开放配置接口，业务设多宽都不会画出来。需要描边请在**内容层**自绘
 * （内容根节点用 flex 撑满弹窗，再设 borderRadius + border）。
 *
 * 不支持的设备上自动降级：弹窗照常显示，只是没有材质。
 */
fun ViewContainer<*, *>.ImmersiveLightSenseDialogOhos(
    init: ImmersiveLightSenseDialogOhosView.() -> Unit
) {
    ImmersiveLightSenseDialogOhosView().also {
        addChild(it, init)
    }
}

open class ImmersiveLightSenseDialogOhosView : ViewContainer<ImmersiveLightSenseDialogOhosAttr, ModalEvent>() {

    /**
     * 对齐 ModalView 在 inWindow=true 时的处理：
     * 绝对定位到 (0,0) 并挂一个空 click 避免手势穿透。
     * 缺了这段时弹窗内容节点拿不到布局尺寸（0x0），子节点会被它裁掉，
     * 表现为"背板正常、内部全空"。
     */
    override fun willInit() {
        super.willInit()
        attr {
            absolutePosition(top = 0f, left = 0f)
        }
        event {
            click { }
        }
    }

    override fun createAttr(): ImmersiveLightSenseDialogOhosAttr {
        return ImmersiveLightSenseDialogOhosAttr()
    }

    override fun createEvent(): ModalEvent {
        return ModalEvent()
    }

    override fun viewName(): String {
        return VIEW_NAME
    }

    companion object {
        const val VIEW_NAME = "KRImmersiveLightSenseDialogView"
    }
}

/**
 * 沉浸光感弹窗的属性。
 *
 * 材质参数直接使用基类 [com.tencent.kuikly.core.base.Attr.immersiveMaterial]，
 * 不再提供本组件的转发方法 —— 沉浸光感的特性统一挂在基类 Attr 上，
 * 所有组件共用一套设置入口，避免重复。
 * 本组件即使不设置也会启用材质（默认 REGULAR 档），设置则覆盖。
 *
 * 刻意**不提供** customStyle / 对齐 / 栅格列数这类鸿蒙专属开关，避免业务踩
 * "设了没效果"的坑，也避免跨平台噪音。
 */
class ImmersiveLightSenseDialogOhosAttr : ContainerAttr()
