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

package com.tencent.kuikly.core.render.android.expand.component.input

import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.const.KRCssConst
import java.nio.charset.StandardCharsets

/**
 * 按UTF-8字节数限制输入长度的InputFilter
 *
 * 示例:
 * - "a" = 1 byte
 * - "中" = 3 bytes
 * - "😂" = 4 bytes
 */
class KRByteLengthFilter(
    maxBytes: Int,
    kuiklyRenderContext: IKuiklyRenderContext?,
    fontSizeGetter: () -> Float,
    textLengthBeyondLimitCallback: () -> Unit
) : KRBaseLengthFilter(
    maxBytes,
    kuiklyRenderContext,
    fontSizeGetter,
    textLengthBeyondLimitCallback
) {

    override fun calculateLength(text: CharSequence, start: Int, end: Int): Int {
        if (start >= end || start < 0 || end > text.length) {
            return 0
        }
        if (text is String) {
            return text.substring(start, end).toByteArray(StandardCharsets.UTF_8).size
        }
        var length = 0
        text.forEachCodePoint(start, end) { codePoint ->
            length += utf8ByteLengthOf(codePoint)
        }
        return length
    }

    override fun truncate(
        text: CharSequence,
        start: Int,
        end: Int,
        maxLength: Int
    ): CharSequence {
        if (maxLength <= 0) {
            return KRCssConst.EMPTY_STRING
        }
        if (start >= end || start < 0 || end > text.length) {
            return KRCssConst.EMPTY_STRING
        }

        val tStart = trimReplacementSpanStart(text, start)
        var byteCount = 0
        var charIndex = tStart

        while (charIndex < end) {
            val codePoint = Character.codePointAt(text, charIndex)
            val charCount = Character.charCount(codePoint)
            val charBytes = utf8ByteLengthOf(codePoint)

            if (byteCount + charBytes > maxLength) {
                break
            }

            byteCount += charBytes
            charIndex += charCount
        }

        val tEnd = trimReplacementSpanEnd(text, minOf(charIndex, end))

        if (tEnd <= tStart) {
            return KRCssConst.EMPTY_STRING
        }
        return subSequence(text, tStart, tEnd)
    }

    companion object {
        private fun utf8ByteLengthOf(codePoint: Int): Int {
            return when {
                codePoint <= 0x7F -> 1
                codePoint <= 0x7FF -> 2
                codePoint <= 0xFFFF -> 3
                else -> 4
            }
        }
    }
}
