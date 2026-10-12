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
    // 无焦点场景下 hide() 打的标记：焦点由 requestFocus 经 Compose 焦点系统
    // 异步送达，届时 startInput 会把默认 focus() 替换为 focusWithoutKeyboard()。
    private var pendingFocusNoKeyboard = false

    override fun show() {
        // 显式弹键盘意图，清除免键盘标记，避免被之前残留的 hide 标记拦截
        pendingFocusNoKeyboard = false
        activeView?.also { sendInputCommand(it, PendingAction.SHOW_KEYBOARD) }
    }

    override fun hide() {
        // 收起键盘但保留/获取焦点（与 KuiklyUI 对齐：hide 即「收键盘不失焦」）。
        val target = activeView ?: pendingView
        if (target != null) {
            // 已有目标 view（已获焦，或 requestFocus 已同步生效）：直接下发自包含命令。
            sendInputCommand(target, PendingAction.HIDE_KEYBOARD)
        } else {
            // 无焦点：此刻 requestFocus 触发的 startInput 尚未到达，拿不到 view。
            // 打标记，等 startInput 送来 pendingView 时把默认 focus() 替换为 focusWithoutKeyboard()，
            // 最终仍只下发一条命令，键盘全程不出现。
            pendingFocusNoKeyboard = true
        }
    }


    internal fun startInput(view: AutoHeightTextAreaView) {
        sendInputCommand(view, PendingAction.START_INPUT)
    }

    internal fun stopInput(view: AutoHeightTextAreaView) {
        // 焦点离开即清理免键盘标记，避免 requestFocus 失败/事件取消导致其永久残留、污染后续输入框
        pendingFocusNoKeyboard = false
        sendInputCommand(view, PendingAction.STOP_INPUT)
    }

    private fun sendInputCommand(view: AutoHeightTextAreaView, action: PendingAction) {
        if (!scheduleInputCommand) {
            scheduleInputCommand = true
            setTimeout(view.pagerId) {
                scheduleInputCommand = false
                when (pendingAction) {
                    PendingAction.START_INPUT -> {
                        if (pendingFocusNoKeyboard) {
                            // hide() 在无焦点时先被调用并打了标记；焦点经 Compose 焦点系统
                            // 异步送达，此刻才拿到 pendingView。把默认 focus() 替换为 focusWithoutKeyboard()，
                            // 只下发一条命令，键盘全程不出现。
                            pendingView?.focusWithoutKeyboard()
                            activeView = pendingView
                            pendingFocusNoKeyboard = false
                        } else {
                            pendingView?.focus()
                            activeView = pendingView
                        }
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
                        // 收键盘但保留/获取焦点：单条自包含命令（原生侧挂 dummy inputView）。
                        // 已获焦时收起键盘不失焦；未获焦时（requestFocus 已同步生效）获焦且键盘全程不出现。
                        val targetView = activeView ?: pendingView
                        targetView?.focusWithoutKeyboard()
                        // 命令保持获焦态，无条件同步 activeView
                        activeView = pendingView
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
