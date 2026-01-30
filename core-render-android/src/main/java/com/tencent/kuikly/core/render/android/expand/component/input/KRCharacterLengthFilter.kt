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

import android.text.Spanned
import android.text.style.ReplacementSpan
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.const.KRCssConst

/**
 * 按字符个数限制输入长度的InputFilter
 *
 * 示例:
 * - "a" = 1 character
 * - "中" = 1 character
 * - "😂" = 1 character (Unicode code point)
 * - "[img]" (ImageSpan) = 1 character
 */
class KRCharacterLengthFilter(
    maxCharacters: Int,
    kuiklyRenderContext: IKuiklyRenderContext?,
    fontSizeGetter: () -> Float,
    textLengthBeyondLimitCallback: () -> Unit
) : KRBaseLengthFilter(
    maxCharacters,
    kuiklyRenderContext,
    fontSizeGetter,
    textLengthBeyondLimitCallback
) {

    override fun calculateLength(text: CharSequence, start: Int, end: Int): Int {
        if (start >= end || start < 0 || end > text.length) {
            return 0
        }
        var length = 0
        var index = start
        if (text is Spanned) {
            val spanRanges = mutableListOf<IntRange>()
            text.getSpans(start, end, ReplacementSpan::class.java).forEach { span ->
                val spanStart = text.getSpanStart(span)
                val spanEnd = text.getSpanEnd(span)
                if (start < spanEnd && spanStart < end) {
                    spanRanges.add(spanStart until spanEnd)
                }
            }
            if (spanRanges.isNotEmpty()) {
                spanRanges.sortWith(Comparator { o1, o2 -> o1.first - o2.first })
                spanRanges.forEach { range ->
                    length += codePointCount(text, index, range.first) + 1
                    index = range.last + 1
                }
            }
        }
        length += codePointCount(text, index, end)
        return length
    }

    override fun truncate(text: CharSequence, start: Int, end: Int, maxLength: Int): CharSequence {
        if (maxLength <= 0) {
            return KRCssConst.EMPTY_STRING
        }
        if (start >= end || start < 0 || end > text.length) {
            return KRCssConst.EMPTY_STRING
        }

        val tStart = trimReplacementSpanStart(text, start)
        var charCount = 0
        var charIndex = tStart

        run count@ {
            if (text is Spanned) {
                val spanRanges = mutableListOf<IntRange>()
                text.getSpans(tStart, end, ReplacementSpan::class.java).forEach { span ->
                    val spanStart = text.getSpanStart(span)
                    val spanEnd = text.getSpanEnd(span)
                    if (start < spanEnd && spanStart < end) {
                        spanRanges.add(spanStart until spanEnd)
                    }
                }
                if (spanRanges.isNotEmpty()) {
                    spanRanges.sortWith(Comparator { o1, o2 -> o1.first - o2.first})
                    spanRanges.forEach { range ->
                        text.forEachCodePoint(charIndex, range.first) {
                            charCount++
                            charIndex += Character.charCount(it)
                            if (charCount == maxLength) {
                                return@count
                            }
                        }
                        charCount++
                        charIndex = range.last + 1
                        if (charCount == maxLength) {
                            return@count
                        }
                    }
                }
            }
            text.forEachCodePoint(charIndex, end) {
                charCount++
                charIndex += Character.charCount(it)
                if (charCount == maxLength) {
                    return@count
                }
            }
        }
        val tEnd = trimReplacementSpanEnd(text, minOf(charIndex, end))
        if (tEnd <= tStart) {
            return KRCssConst.EMPTY_STRING
        }
        return subSequence(text, tStart, tEnd)
    }

    companion object {
        private fun codePointCount(text: CharSequence, start: Int, end: Int): Int {
            if (end <= start) {
                return 0
            }
            var count = 0
            text.forEachCodePoint(start, end) {
                count++
            }
            return count
        }
    }
}
