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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.input.pointer.PointerButton
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.PointerType
import com.tencent.kuikly.compose.ui.input.pointer.ProcessResult
import com.tencent.kuikly.compose.ui.node.LayoutNode
import com.tencent.kuikly.compose.ui.platform.LocalLayoutDirection
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.container.VsyncTickConditions
import com.tencent.kuikly.core.datetime.DateTime

/**
 * Represents a static [CompositionLocal] key for a [ComposeScene] in Jetpack Compose.
 *
 * @see ComposeScene
 */
@OptIn(InternalComposeUiApi::class)
internal val LocalComposeScene = staticCompositionLocalOf<ComposeScene?> { null }

/**
 * The local [ComposeScene] is typically not-null. This extension can be used in these cases.
 */
@OptIn(InternalComposeUiApi::class)
@Composable
internal fun CompositionLocal<ComposeScene?>.requireCurrent(): ComposeScene =
    current ?: error("CompositionLocal LocalComposeScene not provided")

/**
 * A virtual container that encapsulates Compose UI content. UI content can be constructed via
 * [setContent] method and with any Composable that manipulates [LayoutNode] tree.
 *
 * After [ComposeScene] will no longer needed, you should call [close] method, so all resources
 * and subscriptions will be properly closed. Otherwise, there can be a memory leak.
 *
 * It is marked as [InternalComposeUiApi] and used by default Compose entry points
 * (such as application, runComposeUiTest, ComposeWindow). While it can be used by
 * third-party users for integrating Compose into other platforms, it does not come
 * with any guarantee of stability.
 */
@InternalComposeUiApi
interface ComposeScene {
    /**
     * Density of the content which will be used to convert [Dp] units.
     */
    var density: Density

    /**
     * The layout direction of the content, provided to the composition via [LocalLayoutDirection].
     */
    var layoutDirection: LayoutDirection

    /**
     * Represents the scene bounds relatively to window in pixels.
     *
     * The position is used to convert local coordinates to window ones.
     *
     * The size is used to impose constraints on the content. If the size is undefined, it can be
     * set to `null`. In such a case, the content will be laid out without any restrictions and
     * the window size will be utilized to bounds verification.
     *
     * TODO split boundsInWindow and size https://youtrack.jetbrains.com/issue/COMPOSE-964
     */
    var boundsInWindow: IntRect?

    /**
     * 控制按需刷新
     */
    val vsyncTickConditions: VsyncTickConditions

    /**
     * Top-level composition locals, which will be provided for the Composable content, which is set by [setContent].
     *
     * `null` if no composition locals should be provided.
     */
    var compositionLocalContext: CompositionLocalContext?

    /**
     * Close all resources and subscriptions. Not calling this method when [ComposeScene] is no
     * longer needed will cause a memory leak.
     *
     * All effects launched via [LaunchedEffect] or [rememberCoroutineScope] will be cancelled
     * (but not immediately).
     *
     * After calling this method, you cannot call any other method of this [ComposeScene].
     */
    fun close()

    /**
     * Returns the current content size in infinity constraints.
     *
     * @throws IllegalStateException when [ComposeScene] content has lazy layouts without maximum size bounds
     * (e.g. LazyColumn without maximum height).
     */
    fun calculateContentSize(): IntSize

    /**
     * Returns true if there are pending recompositions, renders or dispatched tasks.
     * Can be called from any thread.
     */
    fun hasInvalidations(): Boolean

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content Content of the [ComposeScene]
     */
    fun setContent(content: @Composable () -> Unit)

    /**
     * Render the current content on [canvas]. Passed [nanoTime] will be used to drive all
     * animations in the content (or any other code, which uses [withFrameNanos]
     */
    fun render(
        canvas: Canvas?,
        nanoTime: Long,
    )

    /**
     * Send pointer event to the content.
     *
     * @param eventType Indicates the primary reason that the event was sent.
     * @param position The [Offset] of the current pointer event, relative to the content.
     * @param scrollDelta scroll delta for the PointerEventType.Scroll event
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param type The device type that produced the event, such as [mouse][PointerType.Mouse],
     * or [touch][PointerType.Touch].
     * @param buttons Contains the state of pointer buttons (e.g. mouse and stylus buttons) after the event.
     * @param keyboardModifiers Contains the state of modifier keys, such as Shift, Control,
     * and Alt, as well as the state of the lock keys, such as Caps Lock and Num Lock.
     * @param nativeEvent The original native event.
     * @param button Represents the index of a button which state changed in this event. It's null
     * when there was no change of the buttons state or when button is not applicable (e.g. touch event).
     */
    fun sendPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset = Offset.Zero,
        timeMillis: Long = currentTimeForEvent(),
        type: PointerType = PointerType.Mouse,
        nativeEvent: Any? = null,
    )

    /**
     * Send pointer event to the content. The more detailed version of [sendPointerEvent] that can accept
     * multiple pointers.
     *
     * @param eventType Indicates the primary reason that the event was sent.
     * @param pointers The current pointers with position relative to the content.
     * There can be multiple pointers, for example, if we use Touch and touch screen with multiple fingers.
     * Contains only the state of the active pointers.
     * Touch that is released still considered as active on PointerEventType.Release event (but with pressed=false). It
     * is no longer active after that, and shouldn't be passed to the scene.
     * @param buttons Contains the state of pointer buttons (e.g. mouse and stylus buttons) after the event.
     * @param keyboardModifiers Contains the state of modifier keys, such as Shift, Control,
     * and Alt, as well as the state of the lock keys, such as Caps Lock and Num Lock.
     * @param scrollDelta scroll delta for the PointerEventType.Scroll event
     * @param timeMillis The time of the current pointer event, in milliseconds. The start (`0`) time
     * is platform-dependent.
     * @param nativeEvent The original native event.
     * @param button Represents the index of a button which state changed in this event. It's null
     * when there was no change of the buttons state or when button is not applicable (e.g. touch event).
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun sendPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        scrollDelta: Offset = Offset.Zero,
        timeMillis: Long = currentTimeForEvent(),
        nativeEvent: Any? = null,
        button: PointerButton? = null,
        rootNode: LayoutNode? = null,
    ): ProcessResult
}

private fun currentTimeForEvent(): Long = DateTime.currentTimestamp()
