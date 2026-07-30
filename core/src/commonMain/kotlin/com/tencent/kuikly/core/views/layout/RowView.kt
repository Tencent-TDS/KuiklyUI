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

package com.tencent.kuikly.core.views.layout

import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.layout.FlexAlign

class RowView : LayoutView<ContainerAttr, Event>() {
    var align: FlexAlign = FlexAlign.STRETCH

    override fun willInit() {
        super.willInit()
        val ctx = this
        attr {
            flexDirectionRow()
            alignItems(ctx.align)
        }
    }

    override fun createAttr(): ContainerAttr {
        return ContainerAttr()
    }

    override fun createEvent(): Event {
        return Event()
    }
}

fun ViewContainer<*, *>.Row(align: FlexAlign = FlexAlign.STRETCH, init: RowView.() -> Unit) {
    val rowComponent = RowView()
    rowComponent.align = align
    addChild(rowComponent, init)
}

fun ViewContainer<*, *>.Row(init: RowView.() -> Unit) {
    Row(FlexAlign.STRETCH, init)
}
