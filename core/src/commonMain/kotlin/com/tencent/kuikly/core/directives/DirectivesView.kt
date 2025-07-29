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

package com.tencent.kuikly.core.directives

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event

/**
 * 指令标签节点, 作为模板指令的基类，如vif条件指令, vfor循环指令等模板指令
 */

abstract class DirectivesView : VirtualView<ContainerAttr, Event>() {

    val prevDirectivesView: DirectivesView?
        get() {
            val views = (parent as? ViewContainer)?.templateChildren()
            if (views == null || views.isEmpty()) {
                return null
            }
            val index = views.indexOf(this)
            if (index - 1 < 0) {
                return null
            }
            val prevView = views[index - 1]
            if (prevView is DirectivesView) {
                return prevView as DirectivesView
            }
            return null
        }

    val nextDirectivesView: DirectivesView?
        get() {
            val views = (parent as? ViewContainer)?.templateChildren()
            if (views == null || views.isEmpty()) {
                return null
            }
            val index = views.indexOf(this)
            if (index < 0 || index == views.lastIndex) {
                return null
            }
            val nextView = views[index + 1]
            if (nextView is DirectivesView) {
                return nextView as DirectivesView
            }
            return null
        }

    override fun createAttr(): ContainerAttr {
        return ContainerAttr()
    }

    override fun createEvent(): Event {
        return Event()
    }
}
