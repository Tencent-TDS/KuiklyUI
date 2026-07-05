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

package com.tencent.kuikly.compose.ui.input.key

import android.view.KeyEvent as AndroidKeyEvent

/**
 * Convert an Android hardware key event into the Kuikly Compose key event model.
 */
fun AndroidKeyEvent.toComposeKeyEvent(): KeyEvent =
    KeyEvent(
        key = Key(keyCode.toLong()),
        type = when (action) {
            AndroidKeyEvent.ACTION_UP -> KeyEventType.KeyUp
            AndroidKeyEvent.ACTION_DOWN -> KeyEventType.KeyDown
            else -> KeyEventType.Unknown
        },
        utf16CodePoint = unicodeChar,
        isAltPressed = isAltPressed,
        isCtrlPressed = isCtrlPressed,
        isMetaPressed = isMetaPressed,
        isShiftPressed = isShiftPressed,
        nativeKeyEvent = this,
    )
