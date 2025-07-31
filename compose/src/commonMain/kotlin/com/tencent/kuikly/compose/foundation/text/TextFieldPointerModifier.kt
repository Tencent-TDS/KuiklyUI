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

package com.tencent.kuikly.compose.foundation.text

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusRequester

//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.text.selection.TextFieldSelectionManager
//import androidx.compose.foundation.text.selection.selectionGestureInput
//import androidx.compose.foundation.text.selection.updateSelectionTouchMode
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.FocusRequester
//import androidx.compose.ui.input.pointer.pointerHoverIcon
//import androidx.compose.ui.text.input.OffsetMapping

// Kuikly Team: use android imple
@Composable
internal fun Modifier.textFieldPointer(
//    manager: TextFieldSelectionManager,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    state: LegacyTextFieldState,
    focusRequester: FocusRequester,
    readOnly: Boolean,
//    offsetMapping: OffsetMapping
): Modifier = Modifier.defaultTextFieldPointer(
//    manager,
    enabled,
    interactionSource,
    state,
    focusRequester,
    readOnly,
//    offsetMapping,
)

@Composable
internal fun Modifier.defaultTextFieldPointer(
//    manager: TextFieldSelectionManager,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    state: LegacyTextFieldState,
    focusRequester: FocusRequester,
    readOnly: Boolean,
//    offsetMapping: OffsetMapping
) = this
//    .updateSelectionTouchMode { state.isInTouchMode = it }
    .tapPressTextFieldModifier(interactionSource, enabled) { offset ->
        requestFocusAndShowKeyboardIfNeeded(state, focusRequester, !readOnly)
        if (state.hasFocus && enabled) {
//            if (state.handleState != HandleState.Selection) {
//                state.layoutResult?.let { layoutResult ->
//                    TextFieldDelegate.setCursorOffset(
//                        offset,
//                        layoutResult,
//                        state.processor,
//                        offsetMapping,
//                        state.onValueChange
//                    )
//                    // Won't enter cursor state when text is empty.
//                    if (state.textDelegate.text.isNotEmpty()) {
//                        state.handleState = HandleState.Cursor
//                    }
//                }
//            } else {
//                manager.deselect(offset)
//            }
        }
    }
//    .selectionGestureInput(
//        mouseSelectionObserver = manager.mouseSelectionObserver,
//        textDragObserver = manager.touchSelectionObserver,
//    )
//    .pointerHoverIcon(textPointerIcon)