/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.foundation.text.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ui.text.TextRange
import com.tencent.kuikly.compose.ui.text.coerceIn

@Stable
class TextFieldState internal constructor(
    initialText: String,
    initialSelection: TextRange
) {
    var text: String by mutableStateOf(initialText)
        private set

    var selection: TextRange by mutableStateOf(initialSelection.coerceIn(0, initialText.length))
        internal set

    var composition: TextRange? by mutableStateOf(null)
        internal set

    fun edit(block: TextFieldBuffer.() -> Unit) {
        val buffer = TextFieldBuffer(text, selection, composition)
        buffer.block()
        if (buffer.hasReverted) {
            return
        }
        applyBuffer(buffer)
    }

    /**
     * TextFieldState 的唯一写入收口：所有 text / selection / composition 的变更（edit{}、clearText、
     * setTextAndPlaceCursorAtEnd、updateFromTextField）最终都走此方法，确保 selection 和 composition
     * 始终 coerceIn 到 text.length 范围内。
     */
    fun setTextAndSelect(
        text: String,
        selection: TextRange = TextRange(text.length),
        composition: TextRange? = null
    ) {
        this.text = text
        this.selection = selection.coerceIn(0, text.length)
        this.composition = composition?.coerceIn(0, text.length)
    }

    fun setTextAndPlaceCursorAtEnd(text: String) {
        setTextAndSelect(text, selection = TextRange(text.length))
    }

    fun clearText() {
        setTextAndSelect("", selection = TextRange.Zero)
    }

    private fun applyBuffer(buffer: TextFieldBuffer) {
        setTextAndSelect(
            text = buffer.toString(),
            selection = buffer.selection,
            composition = buffer.composition
        )
    }

    internal fun updateFromTextField(
        text: String,
        selection: TextRange,
        composition: TextRange?
    ) {
        setTextAndSelect(text, selection, composition)
    }
}

@Composable
fun rememberTextFieldState(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length)
): TextFieldState {
    return remember { TextFieldState(initialText, initialSelection) }
}
