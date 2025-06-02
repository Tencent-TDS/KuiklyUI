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

@file:OptIn(InternalComposeUiApi::class)

package com.tencent.kuikly.compose.ui.scene

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.GlobalSnapshotManager
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.input.pointer.PointerButton
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputEvent
import com.tencent.kuikly.compose.ui.input.pointer.PointerType
import com.tencent.kuikly.compose.ui.input.pointer.ProcessResult
import com.tencent.kuikly.compose.ui.node.InternalCoreApi
import com.tencent.kuikly.compose.ui.node.LayoutNode
import com.tencent.kuikly.compose.ui.node.SnapshotInvalidationTracker
import com.tencent.kuikly.compose.container.VsyncTickConditions
import com.tencent.kuikly.compose.ui.KuiklyCanvas
import com.tencent.kuikly.core.exception.throwRuntimeError
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext

/**
 * BaseComposeScene is an internal abstract class that implements the ComposeScene interface.
 * It provides a base implementation for managing composition, input events, and rendering.
 *
 * @property composeSceneContext the object that used to share "context" between multiple scenes
 * on the screen. Also, it provides a way for platform interaction that required within a scene.
 */
@OptIn(InternalComposeUiApi::class)
internal abstract class BaseComposeScene(
    coroutineContext: CoroutineContext,
    val composeSceneContext: ComposeSceneContext,
    private val invalidate: () -> Unit,
) : ComposeScene {
    private var paused = false

    override val vsyncTickConditions =
        VsyncTickConditions { paused ->
            this.paused = paused
        }

    protected val snapshotInvalidationTracker = SnapshotInvalidationTracker(::invalidateIfNeeded)

    @OptIn(InternalCoreApi::class)
    protected val inputHandler: ComposeSceneInputHandler =
        ComposeSceneInputHandler(
            prepareForPointerInputEvent = ::doLayout,
            processPointerInputEvent = ::processPointerInputEvent,
//            processKeyEvent = ::processKeyEvent
        )

    private val frameClock = BroadcastFrameClock(onNewAwaiters = ::invalidateIfNeeded)
    private val recomposer: ComposeSceneRecomposer =
        ComposeSceneRecomposer(coroutineContext, frameClock)
    private var composition: Composition? = null

    protected val compositionContext: CompositionContext
        get() = recomposer.compositionContext

    protected var isClosed = false
        private set

    private var isInvalidationDisabled = false

    private inline fun <T> postponeInvalidation(crossinline block: () -> T): T {
        check(!isClosed) { "ComposeScene is closed" }
        isInvalidationDisabled = true
        return try {
            // Try to get see the up-to-date state before running block
            // Note that this doesn't guarantee it, if sendApplyNotifications is called concurrently
            // in a different thread than this code.
            snapshotInvalidationTracker.sendAndPerformSnapshotChanges()
            snapshotInvalidationTracker.performSnapshotChangesSynchronously(block)
        } finally {
            isInvalidationDisabled = false
        }.also {
            invalidateIfNeeded()
        }
    }

    @Volatile
    private var hasPendingDraws = true

    protected fun invalidateIfNeeded() {
        hasPendingDraws = frameClock.hasAwaiters ||
            snapshotInvalidationTracker.hasInvalidations ||
            inputHandler.hasInvalidations
        if (hasPendingDraws && !isInvalidationDisabled && !isClosed && composition != null) {
            invalidate()
        }
    }

    override var compositionLocalContext: CompositionLocalContext? by mutableStateOf(null)

    /**
     * The last known position of pointer cursor position or `null` if cursor is not inside a scene.
     *
     * TODO: Move it to PlatformContext
     */
    val lastKnownPointerPosition by inputHandler::lastKnownPointerPosition

    init {
        GlobalSnapshotManager.ensureStarted()
    }

    override fun close() {
        check(!isClosed) { "ComposeScene is already closed" }
        isClosed = true

        composition?.dispose()
        recomposer.cancel()
    }

    override fun hasInvalidations(): Boolean = hasPendingDraws || recomposer.hasPendingWork

    override fun setContent(content: @Composable () -> Unit) =
        postponeInvalidation {
            check(!isClosed) { "ComposeScene is closed" }
            inputHandler.onChangeContent()

        /*
         * It's required before setting content to apply changed parameters
         * before first recomposition. Otherwise, it can lead to double recomposition.
         */
            recomposer.performScheduledTasks()

            composition?.dispose()
            composition =
                createComposition {
                    CompositionLocalProvider(
                        LocalComposeScene provides this,
                        content = content,
                    )
                }

            recomposer.performScheduledTasks()
        }

    override fun render(
        canvas: Canvas?,
        nanoTime: Long,
    ) {
        if (paused) {
            return
        }

        return postponeInvalidation {
            // Note that on Android the order is slightly different:
            // - Recomposition
            // - Layout
            // - Draw
            // - Composition effects
            // - Synthetic events
            // We do this differently in order to be able to observe changes made by synthetic events
            // in the drawing phase, thus reducing the time before they are visible on screen.
            //
            // It is important, however, to run the composition effects before the synthetic events are
            // dispatched, in order to allow registering for these events before they are sent.
            // Otherwise, events like a synthetic mouse-enter sent due to a new element appearing under
            // the pointer would be missed by e.g. InteractionSource.collectHoverAsState
            recomposer.performScheduledTasks()
            frameClock.sendFrame(nanoTime) // Recomposition
            doLayout() // Layout
            recomposer.performScheduledEffects() // Composition effects (e.g. LaunchedEffect)
            inputHandler.updatePointerPosition() // Synthetic move event
            snapshotInvalidationTracker.onDraw()
            draw(KuiklyCanvas()) // Draw
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun sendPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        scrollDelta: Offset,
        timeMillis: Long,
        nativeEvent: Any?,
        button: PointerButton?,
        rootNode: LayoutNode?,
    ): ProcessResult {
        if (eventType == PointerEventType.Press || eventType == PointerEventType.Release) {
            vsyncTickConditions.needsToBeProactive = eventType == PointerEventType.Press
        }
        return postponeInvalidation {
            val result =
                inputHandler.onPointerEvent(
                    eventType = eventType,
                    pointers = pointers,
//            buttons = buttons,
//            keyboardModifiers = keyboardModifiers,
                    scrollDelta = scrollDelta,
                    timeMillis = timeMillis,
                    nativeEvent = nativeEvent,
                    button = button,
                    rootNode = rootNode,
                )

            result
        }
    }

    override fun sendPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset,
        timeMillis: Long,
        type: PointerType,
        nativeEvent: Any?,
    ) = postponeInvalidation {
        throwRuntimeError("invalid invoke")
    }

    private fun doLayout() {
        snapshotInvalidationTracker.onMeasureAndLayout()
        measureAndLayout()
    }

    protected abstract fun createComposition(content: @Composable () -> Unit): Composition

    protected abstract fun processPointerInputEvent(event: PointerInputEvent)

    protected abstract fun measureAndLayout()

    protected abstract fun draw(canvas: Canvas)
}
