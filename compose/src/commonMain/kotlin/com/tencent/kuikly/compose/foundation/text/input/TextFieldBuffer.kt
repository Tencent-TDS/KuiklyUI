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

import com.tencent.kuikly.compose.ui.text.TextRange
import com.tencent.kuikly.compose.ui.text.coerceIn

class TextFieldBuffer internal constructor(
    initialText: String,
    initialSelection: TextRange,
    initialComposition: TextRange?
) : CharSequence {
    private val initialText = initialText
    private val initialSelection = initialSelection
    private val initialComposition = initialComposition
    private val content = StringBuilder(initialText)
    private var reverted = false

    var selection: TextRange = initialSelection.coerceIn(0, content.length)
    var composition: TextRange? = initialComposition?.coerceIn(0, content.length)

    internal val hasReverted: Boolean get() = reverted

    override val length: Int get() = content.length

    override fun get(index: Int): Char = content[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return content.subSequence(startIndex, endIndex)
    }

    fun append(text: String) {
        replace(content.length, content.length, text)
    }

    fun insert(index: Int, text: String) {
        replace(index, index, text)
    }

    fun replace(start: Int, end: Int, text: String) {
        val coercedStart = start.coerceIn(0, content.length)
        val coercedEnd = end.coerceIn(0, content.length)
        val rangeStart = minOf(coercedStart, coercedEnd)
        val rangeEnd = maxOf(coercedStart, coercedEnd)
        // 在 Kotlin 公共代码中，StringBuilder 的 API 受限，使用子序列拼接方式
        val prefix = content.substring(0, rangeStart)
        val suffix = if (rangeEnd < content.length) content.substring(rangeEnd) else ""
        content.clear()
        content.append(prefix).append(text).append(suffix)
        val newCursor = rangeStart + text.length
        selection = TextRange(newCursor)
        composition = null
    }

    fun delete(start: Int, end: Int) {
        replace(start, end, "")
    }

    fun selectAll() {
        selection = TextRange(0, content.length)
    }

    fun placeCursorAtEnd() {
        selection = TextRange(content.length)
    }

    fun revertAllChanges() {
        content.clear()
        content.append(initialText)
        selection = initialSelection.coerceIn(0, content.length)
        composition = initialComposition?.coerceIn(0, content.length)
        reverted = true
    }

    fun asCharSequence(): CharSequence = content

    override fun toString(): String = content.toString()
}
