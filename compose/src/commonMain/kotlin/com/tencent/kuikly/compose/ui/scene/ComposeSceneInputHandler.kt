/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.ui.scene

import androidx.compose.runtime.mutableStateMapOf
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.input.pointer.PointerButton
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.PointerId
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputEvent
import com.tencent.kuikly.compose.ui.input.pointer.PointerType
import com.tencent.kuikly.compose.ui.input.pointer.ProcessResult
import com.tencent.kuikly.compose.ui.input.pointer.SyntheticEventSender
import com.tencent.kuikly.compose.ui.node.InternalCoreApi
import com.tencent.kuikly.compose.ui.node.LayoutNode
import com.tencent.kuikly.core.datetime.DateTime

/**
 * Handles input events for [ComposeScene].
 * It's used to encapsulate input handling and share between scene implementations.
 * Also, it's passed to [RootNodeOwner] to handle [onPointerUpdate] callback and provide
 * interface for initiating input from tests.
 *
 * @see SyntheticEventSender
 */
internal class ComposeSceneInputHandler(
    private val prepareForPointerInputEvent: () -> Unit,
    private val processPointerInputEvent: (PointerInputEvent) -> Unit,
//    private val processKeyEvent: (KeyEvent) -> Boolean,
) {
    private val defaultPointerStateTracker = DefaultPointerStateTracker()
    private val pointerPositions = mutableStateMapOf<PointerId, Offset>()
    private val syntheticEventSender = SyntheticEventSender(processPointerInputEvent)
    private var lastProcessResult: ProcessResult? = null

    /**
     * The mouse cursor (also works with touch pointer) position
     * or `null` if cursor is not inside a scene.
     */
    val lastKnownPointerPosition: Offset?
        get() = pointerPositions.values.firstOrNull()

    /**
     * Indicates if there were invalidations and triggering [BaseComposeScene.measureAndLayout]
     * is now required.
     */
    val hasInvalidations: Boolean
        get() = syntheticEventSender.needUpdatePointerPosition

    @OptIn(ExperimentalComposeUiApi::class, InternalCoreApi::class)
    fun onPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        scrollDelta: Offset = Offset(0f, 0f),
        timeMillis: Long = DateTime.currentTimestamp(),
        nativeEvent: Any? = null,
        button: PointerButton? = null,
        rootNode: LayoutNode? = null,
    ): ProcessResult {
        val event =
            PointerInputEvent(
                eventType,
                pointers,
                timeMillis,
                nativeEvent,
                scrollDelta,
                button,
                rootNode,
            )
        prepareForPointerInputEvent()
        updatePointerPosition()
        syntheticEventSender.send(event)
        updatePointerPositions(event)
        return lastProcessResult ?: ProcessResult(false, false)
    }

    fun updatePointerPosition() {
        syntheticEventSender.updatePointerPosition()
    }

    fun onChangeContent() {
        syntheticEventSender.reset()
    }

    fun onPointerUpdate() {
        syntheticEventSender.needUpdatePointerPosition = true
    }

    @OptIn(InternalCoreApi::class)
    private fun updatePointerPositions(event: PointerInputEvent) {
        // update positions for pointers that are down + mouse (if it is not Exit event)
        for (pointer in event.pointers) {
            if ((pointer.type == PointerType.Mouse && event.eventType != PointerEventType.Exit) ||
                pointer.down
            ) {
                pointerPositions[pointer.id] = pointer.position
            }
        }
        // touches/styluses positions should be removed from [pointerPositions] if they are not down anymore
        // also, mouse exited ComposeScene should be removed
        val iterator = pointerPositions.iterator()
        while (iterator.hasNext()) {
            val pointerId = iterator.next().key
            val pointer = event.pointers.find { it.id == pointerId } ?: continue
            if ((pointer.type != PointerType.Mouse && !pointer.down) ||
                (pointer.type == PointerType.Mouse && event.eventType == PointerEventType.Exit)
            ) {
                iterator.remove()
            }
        }
    }

    fun setProcessResult(result: ProcessResult) {
        lastProcessResult = result
    }
}

private class DefaultPointerStateTracker
