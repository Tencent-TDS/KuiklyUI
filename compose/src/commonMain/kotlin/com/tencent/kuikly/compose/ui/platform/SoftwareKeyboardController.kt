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

@file:Suppress("DEPRECATION")

package com.tencent.kuikly.compose.ui.platform

import androidx.compose.runtime.Stable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.AutoHeightTextAreaView

/**
 * Provide software keyboard control.
 */
@Stable
interface SoftwareKeyboardController {
    /**
     * Request that the system show a software keyboard.
     *
     * This request is best effort. If the system can currently show a software keyboard, it
     * will be shown. However, there is no guarantee that the system will be able to show a
     * software keyboard. If the system cannot show a software keyboard currently,
     * this call will be silently ignored.
     *
     * The software keyboard will never show if there is no composable that will accept text input,
     * such as a [TextField][androidx.compose.foundation.text.BasicTextField] when it is focused.
     * You may find it useful to ensure focus when calling this function.
     *
     * You do not need to call this function unless you also call [hide], as the
     * keyboard is automatically shown and hidden by focus events in the BasicTextField.
     *
     * Calling this function is considered a side-effect and should not be called directly from
     * recomposition.
     *
     * @sample androidx.compose.ui.samples.SoftwareKeyboardControllerSample
     */
    fun show()

    /**
     * Hide the software keyboard.
     *
     * This request is best effort, if the system cannot hide the software keyboard this call
     * will silently be ignored.
     *
     * Calling this function is considered a side-effect and should not be called directly from
     * recomposition.
     *
     * @sample androidx.compose.ui.samples.SoftwareKeyboardControllerSample
     */
    fun hide()
}

internal class KuiklySoftwareKeyboardController : SoftwareKeyboardController {
    private enum class PendingAction {
        NONE, START_INPUT, STOP_INPUT, SHOW_KEYBOARD, HIDE_KEYBOARD
    }
    private var activeView: AutoHeightTextAreaView? = null
    private var pendingView: AutoHeightTextAreaView? = null
    private var pendingAction = PendingAction.NONE
    private var scheduleInputCommand = false

    override fun show() {
        activeView?.also { sendInputCommand(it, PendingAction.SHOW_KEYBOARD) }
    }

    override fun hide() {
        activeView?.also { sendInputCommand(it, PendingAction.HIDE_KEYBOARD) }
    }

    internal fun startInput(view: AutoHeightTextAreaView) {
        sendInputCommand(view, PendingAction.START_INPUT)
    }

    internal fun stopInput(view: AutoHeightTextAreaView) {
        sendInputCommand(view, PendingAction.STOP_INPUT)
    }

    private fun sendInputCommand(view: AutoHeightTextAreaView, action: PendingAction) {
        if (!scheduleInputCommand) {
            scheduleInputCommand = true
            setTimeout(view.pagerId) {
                scheduleInputCommand = false
                when (pendingAction) {
                    PendingAction.START_INPUT -> {
                        pendingView?.focus()
                        activeView = pendingView
                    }
                    PendingAction.STOP_INPUT -> {
                        if (activeView == pendingView) {
                            activeView?.blur()
                            activeView = null
                        }
                    }
                    PendingAction.SHOW_KEYBOARD -> {
                        activeView?.focus()
                    }
                    PendingAction.HIDE_KEYBOARD -> {
                        activeView?.blur()
                    }
                    else -> {}
                }
                pendingAction = PendingAction.NONE
                pendingView = null
            }
        }
        pendingView = view
        pendingAction = action
    }

}
