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

/**
 * Represents a tap event collected from the UI.
 *
 * @property position The coordinates of the tap event relative to the view.
 * @property node The Compose node associated with the tap event (usually KNode).
 * @property nativeView The underlying native view (DeclarativeBaseView) associated with the node.
 * @property eventType The type of the tap event (e.g., TAP).
 */
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

/**
 * Enum class defining the types of tap events.
 */
enum class TapEventType {
    TAP,
}

/**
 * A global manager for handling and distributing tap events across the application.
 * This allows for centralized monitoring or processing of tap interactions.
 */
object GlobalTapManager {

    /**
     * Flag to enable or disable touch slop detection for tap gestures.
     * When enabled, tap gestures that move beyond the touch slop distance will be cancelled.
     * This helps in preventing accidental taps during scrolling or other gestures.
     * Default is false.
     */
    var enableTouchSlopForTap: Boolean = true

    private val tapEventListeners = mutableListOf<(TapEvent) -> Unit>()

    /**
     * Registers a listener to receive global tap events.
     *
     * @param listener A lambda that will be invoked with a [TapEvent] when a tap occurs.
     */
    fun addTapEventListener(listener: (TapEvent) -> Unit) {
        tapEventListeners += listener
    }

    /**
     * Unregisters a previously added tap event listener.
     *
     * @param listener The listener to remove.
     */
    fun removeTapEventListener(listener: (TapEvent) -> Unit) {
        tapEventListeners -= listener
    }

    /**
     * Collects a tap event and dispatches it to all registered listeners.
     * This method should be called by gesture detectors when a valid tap is detected.
     *
     * @param position The position of the tap.
     * @param node The Compose node where the tap occurred.
     * @param eventType The type of the tap event.
     */
    fun collectTap(position: Offset, node: Any?, eventType: TapEventType) {
        if (node is KNode<*>) {
            val event = TapEvent(position, node, node.view, eventType)
            tapEventListeners.forEach { it(event) }
        }
    }
}
