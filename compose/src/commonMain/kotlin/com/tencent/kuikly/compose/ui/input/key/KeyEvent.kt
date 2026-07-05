/*
 * Copyright 2020 The Android Open Source Project
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

package com.tencent.kuikly.compose.ui.input.key

/**
 * When a user presses a key on a hardware keyboard, a [KeyEvent] is sent to the item that is
 * currently focused. Any parent composable can intercept this [key event][KeyEvent] on its way to
 * the focused item by using [Modifier.onPreviewKeyEvent()]][onPreviewKeyEvent]. If the item is
 * not consumed, it returns back to each parent and can be intercepted by using
 * [Modifier.onKeyEvent()]][onKeyEvent].
 *
 * @sample androidx.compose.ui.samples.KeyEventSample
 */
data class KeyEvent(
    val key: Key,
    val type: KeyEventType = KeyEventType.Unknown,
    val utf16CodePoint: Int = 0,
    val isAltPressed: Boolean = false,
    val isCtrlPressed: Boolean = false,
    val isMetaPressed: Boolean = false,
    val isShiftPressed: Boolean = false,
    val nativeKeyEvent: Any? = null
) {
    companion object
}

/**
 * The type of Key Event.
 *
 * @sample androidx.compose.ui.samples.KeyEventTypeSample
 */
@kotlin.jvm.JvmInline
value class KeyEventType internal constructor(@Suppress("unused") private val value: Int) {

    override fun toString(): String {
        return when (this) {
            KeyUp -> "KeyUp"
            KeyDown -> "KeyDown"
            Unknown -> "Unknown"
            else -> "Invalid"
        }
    }

    companion object {
        /**
         * Unknown key event.
         *
         * @sample androidx.compose.ui.samples.KeyEventTypeSample
         */
        val Unknown: KeyEventType = KeyEventType(0)

        /**
         * Type of KeyEvent sent when the user lifts their finger off a key on the keyboard.
         *
         * @sample androidx.compose.ui.samples.KeyEventTypeSample
         */
        val KeyUp: KeyEventType = KeyEventType(1)

        /**
         * Type of KeyEvent sent when the user presses down their finger on a key on the keyboard.
         *
         * @sample androidx.compose.ui.samples.KeyEventTypeSample
         */
        val KeyDown: KeyEventType = KeyEventType(2)
    }
}
