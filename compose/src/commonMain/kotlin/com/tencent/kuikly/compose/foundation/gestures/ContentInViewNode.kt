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

package com.tencent.kuikly.compose.foundation.gestures

import com.tencent.kuikly.compose.ui.geometry.Rect

/**
 * Static field to turn on a bunch of verbose logging to debug animations. Since this is a constant,
 * any log statements guarded by this value should be removed by the compiler when it's false.
 */
internal const val CONTENT_IN_VIEW_DEBUG = false
internal const val CONTENT_IN_VIEW_TAG = "BringIntoView"

/**
 * A minimum amount of delta that it is considered a valid scroll.
 */
internal const val MinScrollThreshold = 0.5f

/**
 * Checks whether [focusedChildRect] was fully visible in [oldViewport] but is at least partially
 * clipped by [newViewport]. This is the official Compose condition for triggering path-A
 * (FocusedBounds) compensation scrolling when the viewport shrinks (e.g. keyboard appearing
 * with ADJUST_RESIZE semantics).
 *
 * Aligned with official `ContentInViewNode.kt:150-167`:
 * ```kotlin
 * previousFocusedChildBounds.isMaxVisible(oldSize) && !focusedChild.isMaxVisible(size)
 * ```
 *
 * @param focusedChildRect The current bounds of the focused child, in container-local coordinates.
 * @param oldViewport      The viewport rect before the resize.
 * @param newViewport      The viewport rect after the resize.
 * @return `true` if the focused child was fully visible before but is now partially clipped.
 */
internal fun wasFocusedChildClippedByViewportShrink(
    focusedChildRect: Rect,
    oldViewport: Rect,
    newViewport: Rect,
): Boolean {
    return isMaxVisible(focusedChildRect, oldViewport) && !isMaxVisible(focusedChildRect, newViewport)
}

/**
 * Returns `true` if [rect] is fully contained within [viewport] (i.e. visible on all sides).
 * Aligned with official `Rect.isMaxVisible(...)`.
 */
private fun isMaxVisible(rect: Rect, viewport: Rect): Boolean {
    return rect.top >= viewport.top &&
        rect.bottom <= viewport.bottom &&
        rect.left >= viewport.left &&
        rect.right <= viewport.right
}
