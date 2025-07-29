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

package com.tencent.kuikly.compose.platform

import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.core.base.DeclarativeBaseView

data class TapEvent(
    val position: Offset,
    val node: Any?,
    val nativeView: DeclarativeBaseView<*, *>,
    val eventType: TapEventType
) {
    override fun toString(): String {
        return "TapEvent(position=$position, node=$node, eventType=$eventType)"
    }
}

enum class TapEventType {
    TAP,
}

object GlobalTapManager {
    private val tapEventListeners = mutableListOf<(TapEvent) -> Unit>()

    fun addTapEventListener(listener: (TapEvent) -> Unit) {
        tapEventListeners += listener
    }

    fun removeTapEventListener(listener: (TapEvent) -> Unit) {
        tapEventListeners -= listener
    }

    fun collectTap(position: Offset, node: Any?, eventType: TapEventType) {
        if (node is KNode<*>) {
            val event = TapEvent(position, node, node.view, eventType)
            tapEventListeners.forEach { it(event) }
        }
    }
}
