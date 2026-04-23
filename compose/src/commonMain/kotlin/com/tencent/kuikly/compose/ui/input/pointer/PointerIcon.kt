/*
 * Copyright 2021 The Android Open Source Project
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

package com.tencent.kuikly.compose.ui.input.pointer

import androidx.compose.runtime.Stable
import com.tencent.kuikly.compose.extension.cursor
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.core.base.CursorType

/**
 * Represents a pointer icon to use in [Modifier.pointerHoverIcon].
 */
@Stable
interface PointerIcon {
    companion object {
        /** The default (arrow) pointer icon. */
        val Default: PointerIcon = PointerIconImpl(CursorType.DEFAULT)

        /** A hand pointer icon, typically used for clickable elements. */
        val Hand: PointerIcon = PointerIconImpl(CursorType.POINTER)

        /** A text cursor (I-beam) pointer icon. */
        val Text: PointerIcon = PointerIconImpl(CursorType.TEXT)

        /** A crosshair pointer icon. */
        val Crosshair: PointerIcon = PointerIconImpl(CursorType.CROSSHAIR)

        /** A grab (open hand) pointer icon. */
        val Grab: PointerIcon = PointerIconImpl(CursorType.GRAB)

        /** A grabbing (closed hand) pointer icon. */
        val Grabbing: PointerIcon = PointerIconImpl(CursorType.GRABBING)

        /** A not-allowed (forbidden) pointer icon. */
        val NotAllowed: PointerIcon = PointerIconImpl(CursorType.NOT_ALLOWED)
    }
}

/**
 * Internal implementation of [PointerIcon] backed by a [CursorType] string.
 */
internal data class PointerIconImpl(val cursorType: String) : PointerIcon

/**
 * Modifier that lets a developer define a pointer icon to display when the cursor is
 * hovered over the element. When [overrideDescendants] is set to true, this modifier will
 * override any pointer icon modifications done by descendants of the composable it is applied to.
 *
 * @param icon the icon to set
 * @param overrideDescendants if true, this icon will take precedence over any pointer icon
 * modifier applied to descendants. The default is false.
 */
@Stable
fun Modifier.pointerHoverIcon(icon: PointerIcon, overrideDescendants: Boolean = false): Modifier {
    val cursorType = when (icon) {
        is PointerIconImpl -> icon.cursorType
        else -> CursorType.DEFAULT
    }
    return this.cursor(cursorType)
}
