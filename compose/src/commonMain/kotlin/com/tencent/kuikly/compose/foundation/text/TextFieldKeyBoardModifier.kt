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

package com.tencent.kuikly.compose.foundation.text

import com.tencent.kuikly.compose.extension.setProp
import com.tencent.kuikly.compose.foundation.focusable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusEventModifierNode
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.FocusState
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.focus.onFocusChanged

/**
 * 设置是否在点击 IME 动作按钮（如 Send/Go/Search）时自动收起键盘
 *
 * @param autoHide 是否自动收起键盘，默认为 false
 *                 - true: 点击 Send 等按钮后自动收起键盘
 *                 - false: 点击 Send 等按钮后保持键盘打开，由业务自己控制
 * ```
 */
fun Modifier.autoHideKeyboardOnImeAction(enable: Boolean): Modifier =
    setProp("autoHideKeyboardOnImeAction", if (enable) 1 else 0)
