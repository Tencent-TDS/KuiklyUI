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

/**
 * Build a Kuikly Compose key event from a native-family platform key code.
 *
 * iOS, macOS and OHOS hosts can use this when they already normalize their native key code to the
 * same key-code space as [Key] on Kotlin/Native targets.
 */
fun nativePlatformKeyEvent(
    keyCode: Long,
    type: KeyEventType = KeyEventType.Unknown,
    utf16CodePoint: Int = 0,
    isAltPressed: Boolean = false,
    isCtrlPressed: Boolean = false,
    isMetaPressed: Boolean = false,
    isShiftPressed: Boolean = false,
    nativeKeyEvent: Any? = null,
): KeyEvent =
    KeyEvent(
        key = Key(keyCode),
        type = type,
        utf16CodePoint = utf16CodePoint,
        isAltPressed = isAltPressed,
        isCtrlPressed = isCtrlPressed,
        isMetaPressed = isMetaPressed,
        isShiftPressed = isShiftPressed,
        nativeKeyEvent = nativeKeyEvent,
    )
/**
 * Build a Kuikly Compose key event from a [SkikoKey] value used by Kuikly native targets.
 */
fun SkikoKey.toComposeKeyEvent(
    type: KeyEventType = KeyEventType.Unknown,
    utf16CodePoint: Int = 0,
    isAltPressed: Boolean = false,
    isCtrlPressed: Boolean = false,
    isMetaPressed: Boolean = false,
    isShiftPressed: Boolean = false,
    nativeKeyEvent: Any? = null,
): KeyEvent =
    nativePlatformKeyEvent(
        keyCode = platformKeyCode.toLong(),
        type = type,
        utf16CodePoint = utf16CodePoint,
        isAltPressed = isAltPressed,
        isCtrlPressed = isCtrlPressed,
        isMetaPressed = isMetaPressed,
        isShiftPressed = isShiftPressed,
        nativeKeyEvent = nativeKeyEvent,
    )
