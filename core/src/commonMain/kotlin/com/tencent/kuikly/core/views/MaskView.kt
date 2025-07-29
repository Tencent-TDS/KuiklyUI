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
import com.tencent.kuikly.core.base.event.Event
/**
 * 创建一个遮罩视图。遮罩视图的 alpha 通道用于屏蔽视图内容。
 * @param maskFromView 接收 ViewBuilder 参数，用于构建遮罩视图的源视图。
 * @param maskToView  接收 ViewBuilder 参数，用于构建遮罩视图的目标视图。
 */
fun ViewContainer<*, *>.Mask(maskFromView: ViewBuilder, maskToView: ViewBuilder) {
    addChild(MaskView()) {
        View {
            this.getViewAttr().backgroundColor(Color.TRANSPARENT)
            maskFromView.invoke(this)
            this.getViewAttr().absolutePosition(0f, 0f, 0f, 0f)
        }
        View {
            this.getViewAttr().backgroundColor(Color.TRANSPARENT)
            maskToView.invoke(this)
        }
    }
}
class MaskView : ViewContainer<ContainerAttr, Event>() {
    lateinit var maskToViewBuilder: ViewBuilder
    lateinit var maskFromViewBuilder: ViewBuilder

    override fun createAttr(): ContainerAttr = ContainerAttr()

    override fun createEvent(): Event = Event()

    override fun viewName(): String {
        return ViewConst.TYPE_MASK
    }
}
