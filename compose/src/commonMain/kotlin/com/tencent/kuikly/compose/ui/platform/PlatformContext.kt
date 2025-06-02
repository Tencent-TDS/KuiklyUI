/*
 * Copyright 2022 The Android Open Source Project
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
package com.tencent.kuikly.compose.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.focus.FocusDirection
import com.tencent.kuikly.compose.ui.focus.FocusManager
import com.tencent.kuikly.compose.ui.input.InputMode
import com.tencent.kuikly.compose.ui.input.InputModeManager

/**
 * Platform context that provides platform-specific bindings.
 */
//@InternalComposeUiApi
interface PlatformContext {
    /**
     * The value that will be provided to [LocalWindowInfo] by default.
     */
    val windowInfo: WindowInfo

    val viewConfiguration: ViewConfiguration get() = EmptyViewConfiguration
    val inputModeManager: InputModeManager

    val parentFocusManager: FocusManager get() = EmptyFocusManager
    fun requestFocus(): Boolean = true

    companion object {
        val Empty = object : PlatformContext {
            override val windowInfo: WindowInfo = WindowInfoImpl().apply {
                // true is a better default if platform doesn't provide WindowInfo.
                // otherwise UI will be rendered always in unfocused mode
                // (hidden textfield cursor, gray titlebar, etc)
                isWindowFocused = true
            }
            override val inputModeManager: InputModeManager = DefaultInputModeManager()
        }
    }
}

internal class DefaultInputModeManager(
    initialInputMode: InputMode = InputMode.Keyboard
) : InputModeManager {
    override var inputMode: InputMode by mutableStateOf(initialInputMode)

    @ExperimentalComposeUiApi
    override fun requestInputMode(inputMode: InputMode) =
        if (inputMode == InputMode.Touch || inputMode == InputMode.Keyboard) {
            this.inputMode = inputMode
            true
        } else {
            false
        }
}

internal object EmptyViewConfiguration : ViewConfiguration {
    override val longPressTimeoutMillis: Long = 500
    override val doubleTapTimeoutMillis: Long = 300
    override val doubleTapMinTimeMillis: Long = 40
    override val touchSlop: Float = 18f
}

private object EmptyFocusManager : FocusManager {
    override fun clearFocus(force: Boolean) = Unit
    override fun moveFocus(focusDirection: FocusDirection) = false
}

