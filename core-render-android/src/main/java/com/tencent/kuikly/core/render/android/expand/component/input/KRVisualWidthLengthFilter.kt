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
 * 按视觉宽度限制输入长度的InputFilter
 *
 * 视觉宽度规则:
 * - ASCII字符 = 1
 * - 中文字符 = 2
 * - Emoji = 2
 * - ImageSpan = 2
 * - 不可见字符 = 1
 */
class KRVisualWidthLengthFilter(
    maxVisualWidth: Int,
    kuiklyRenderContext: IKuiklyRenderContext?,
    fontSizeGetter: () -> Float,
    textLengthBeyondLimitCallback: () -> Unit
) : KRBaseLengthFilter(
    maxVisualWidth,
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
                    length += visualWidthCount(
                        text,
                        index,
                        range.first
                    ) + VISUAL_WIDTH_WIDE
                    index = range.last + 1
                }
            }
        }
        length += visualWidthCount(text, index, end)
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
                            val visualWidth = visualWidthOf(it)
                            if (charCount + visualWidth > maxLength) {
                                return@count
                            }
                            charCount += visualWidth
                            charIndex += Character.charCount(it)
                        }
                        if (charCount + VISUAL_WIDTH_WIDE > maxLength) {
                            return@count
                        }
                        charCount += VISUAL_WIDTH_WIDE
                        charIndex = range.last + 1
                    }
                }
            }
            text.forEachCodePoint(charIndex, end) {
                val visualWidth = visualWidthOf(it)
                if (charCount + visualWidth > maxLength) {
                    return@count
                }
                charCount += visualWidth
                charIndex += Character.charCount(it)
            }
        }
        val tEnd = trimReplacementSpanEnd(text, minOf(charIndex, end))
        if (tEnd <= tStart) {
            return KRCssConst.EMPTY_STRING
        }
        return subSequence(text, tStart, tEnd)
    }

    companion object {
        private const val VISUAL_WIDTH_WIDE = 2

        private fun visualWidthOf(codePoint: Int): Int {
            return if (codePoint < 128) 1 else when (Character.getType(codePoint)) {
                Character.CONTROL.toInt(),
                Character.FORMAT.toInt(),
                Character.SURROGATE.toInt() -> 1
                else -> 2
            }
        }

        private fun visualWidthCount(text: CharSequence, start: Int, end: Int): Int {
            if (end <= start) {
                return 0
            }
            var count = 0
            text.forEachCodePoint(start, end) {
                count += visualWidthOf(it)
            }
            return count
        }
    }
}
