package com.tencent.kuikly.compose.foundation.text

/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.tencent.kuikly.compose.extension.setProp
import com.tencent.kuikly.compose.ui.Modifier

/**
 * 设置是否在点击 IME 动作按钮（如 Send/Go/Search）时自动收起键盘
 *
 * @param autoHide 是否自动收起键盘，默认为 false
 *                 - true: 点击 Send 等按钮后自动收起键盘
 *                 - false: 点击 Send 等按钮后保持键盘打开，由业务自己控制
 *
 * 使用示例：
 * ```
 * BasicTextField(
 *     modifier = Modifier.autoHideKeyboardOnImeAction(false),
 *     keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
 *     keyboardActions = KeyboardActions(
 *         onSend = {
 *             // 处理发送逻辑，键盘保持打开
 *             // 如需收起，手动调用 keyboardController?.hide()
 *         }
 *     )
 * )
 * ```
 */
fun Modifier.autoHideKeyboardOnImeAction(autoHide: Boolean): Modifier =
    setProp("autoHideKeyboardOnImeAction", autoHide)
