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

import android.text.InputFilter
import android.text.Spannable
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.util.SparseArray
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.TextPostProcessorInput
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.expand.component.KRTextProps
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.max

/**
 * Created by kamlin on 2024/10/18.
 */
class KRTextLengthLimitInputFilter(
    private val maxLength: Int,
    private val kuiklyRenderContext: IKuiklyRenderContext?,
    private val fontSizeGetter: () -> Float,
    private val textLengthBeyondLimitCallback: () -> Unit
) : InputFilter {

    private var calculateStrategy = CountCalculateStrategy()

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val richDest = createRichText(dest ?: "", kuiklyRenderContext, fontSizeGetter.invoke())
        val destTextCount = if (dend == dstart) {
            0
        } else {
            countText(richDest.subSequence(dstart, dend), calculateStrategy).total(calculateStrategy)
        }
        val currentCount = countText(richDest, calculateStrategy).total(calculateStrategy)
        val keep: Int = maxLength - (currentCount - destTextCount)

        if (keep <= 0) {
            // 已超出字数限制
            textLengthBeyondLimitCallback.invoke()
            return KRCssConst.EMPTY_STRING
        } else {
            val richSource = createRichText(source ?: "", kuiklyRenderContext, fontSizeGetter.invoke())
            val sourceCount = countText(richSource, calculateStrategy).total(calculateStrategy)
            return if (keep >= sourceCount) {
                null // 没超出，保持原样
            } else {
                // 超出字数限制，截取
                val endOffset = calcEndIndexOfText(richSource, start, keep)
                textLengthBeyondLimitCallback.invoke()
                richSource.subSequence(start, endOffset)
            }
        }
    }

    private fun calcEndIndexOfText(text: CharSequence, start: Int, charLimit: Int): Int {
        val lookups: SparseArray<SpanPair> = extractSpanPairsToArray(text)
        val counter = AsciiHalfCharacterCounter(strategy = calculateStrategy)
        var index = start
        var restCount: Float = charLimit - counter.total()
        while (index < text.length && !counter.isLimit && restCount > 0) {
            val spanIndex = lookups.indexOfKey(index)
            val increase = if (spanIndex >= 0) {
                val pair: SpanPair = lookups.valueAt(spanIndex)
                counter.limitCount(pair, restCount)
            } else {
                val pos = -spanIndex - 1
                val maxEnd = if (pos < lookups.size()) lookups.valueAt(pos).start else text.length
                val offset: Int = counter.limitCount(text, index, maxEnd, restCount)
                offset
            }
            restCount = charLimit - counter.total()
            if (restCount >= 0) {
                index += increase
            }
        }
        return index
    }

    private data class AsciiHalfCharacterCounter(
        var emojiCount: Int = 0,// Unicode Emoji 计数
        var replacementCount: Int = 0,
        var mAsciiCount: Int = 0, // ascii计数
        var mNonAsciiCount: Int = 0, // 其他字符计数
        var isLimit: Boolean = false,
        var strategy: CountCalculateStrategy
    ) {
        fun total(): Float {
            return (emojiCount * strategy.emojiCount +
                    replacementCount * strategy.emoticonCount +
                    mNonAsciiCount * strategy.textCount +
                    mAsciiCount * strategy.asciiCount).toFloat()
        }

        fun limitCount(str: CharSequence, s: Int, e: Int, limit: Float): Int {
            var newLimit = limit
            var index = s
            val maxLimit = total() + newLimit
            while (index < e && newLimit > 0) {
                if (newLimit >= 1) {
                    count(str[index++])
                } else if (!tryLimitCount(str[index])) {
                    // 不到一个字符的空间, 看是否能继续计算下一个
                    isLimit = true
                    break
                } else {
                    ++index
                }
                newLimit = maxLimit - total()
            }
            if (newLimit <= 0) {
                isLimit = true
            }
            return index - s
        }

        fun count(character: Char) {
            if (character.toInt() < 128) {
                mAsciiCount++
            } else {
                mNonAsciiCount++
            }
        }

        fun count(span: SpanPair) {
            if (span.type == SpanPair.EMOJI_TYPE) {
                emojiCount++
            } else {
                replacementCount++
            }
        }

        fun count(str: CharSequence, s: Int, e: Int) {
            for (index in s until e) {
                count(str[index])
            }
        }

        fun limitCount(span: SpanPair, limit: Float): Int {
            if (limit < 1) {
                isLimit = true
                return 0
            }
            count(span)
            return span.end - span.start
        }

        fun toCountInfo(): CharacterCountInfo {
            return CharacterCountInfo(emojiCount, replacementCount, mAsciiCount, mNonAsciiCount)
        }

        private fun tryLimitCount(character: Char): Boolean {
            if (exceedLimit(character)) {
                isLimit = true
                return false
            }
            if (character.toInt() < 128) {
                mAsciiCount++
            } else {
                mNonAsciiCount++
            }
            return true
        }

        private fun exceedLimit(character: Char): Boolean {
            return character.toInt() >= 128
        }
    }

    internal data class CharacterCountInfo(
        var emojiCount: Int = 0,
        var emoticonCount: Int = 0,
        var asciiCount: Int = 0,
        var textCount: Int = 0
    ) {
        fun total(strategy: CountCalculateStrategy): Int {
            return (ceil(emojiCount * strategy.emojiCount) +
                    ceil(emoticonCount * strategy.emoticonCount) +
                    ceil(textCount * strategy.textCount) +
                    ceil(asciiCount * strategy.asciiCount)).toInt()
        }
    }

    internal data class CountCalculateStrategy(
        var emojiCount: Double = 1.0,
        var emoticonCount: Double = 1.0,
        var asciiCount: Double = 0.5,
        var textCount: Double = 1.0
    )

    private data class SpanPair(
        val start: Int,
        val end: Int,
        val type: Int
    ) {
        companion object {
            const val EMOJI_TYPE = 1
            const val REPLACEMENT_SPAN_TYPE = 2

            fun from(spans: Spannable, span: Any, type: Int): SpanPair {
                val spanEnd = spans.getSpanEnd(span)
                val spanStart = spans.getSpanStart(span)
                return SpanPair(
                    spanStart,
                    spanEnd,
                    type
                )
            }
        }
    }

    companion object {

        private val extraEmoji = Pattern.compile("[\\ud83c\\udc00-\\ud83c\\udfff]|[\\ud83d\\udc00-\\ud83d\\udfff]|[\\ud83e\\udc00-\\ud83e\\udfff]|[\\ud83f\\udc00-\\ud83f\\udfff]")

        internal fun createRichText(origin: CharSequence, kuiklyRenderContext: IKuiklyRenderContext?, fontSize: Float): CharSequence {
            val textPostProcessorAdapter = KuiklyRenderAdapterManager.krTextPostProcessorAdapter ?: return origin
            val textProp = KRTextProps(kuiklyRenderContext).apply {
                setProp(KRTextProps.PROP_KEY_FONT_SIZE, fontSize)
            }
            return textPostProcessorAdapter.onTextPostProcess(kuiklyRenderContext, TextPostProcessorInput("input", origin, textProp)).text
        }

        internal fun countText(
            text: CharSequence,
            calculateStrategy: CountCalculateStrategy
        ): CharacterCountInfo {
            val counter = AsciiHalfCharacterCounter(strategy = calculateStrategy)
            val lookups: SparseArray<SpanPair> = extractSpanPairsToArray(text)
            var index = 0
            while (index < text.length) {
                val spanIndex = lookups.indexOfKey(index)
                if (spanIndex >= 0) {
                    val pair: SpanPair = lookups.valueAt(spanIndex)
                    index += pair.end - pair.start
                    counter.count(pair)
                } else {
                    val pos = -spanIndex - 1
                    val maxEnd =
                        if (pos < lookups.size()) lookups.valueAt(pos).start else text.length
                    counter.count(text, index, maxEnd)
                    index += maxEnd - index
                }
            }
            return counter.toCountInfo()
        }

        internal fun textCount(text: CharSequence): Int {
            val calculateStrategy = CountCalculateStrategy()
            return countText(text, calculateStrategy).total(calculateStrategy)
        }

        private fun extractSpanPairsToArray(text: CharSequence): SparseArray<SpanPair> {
            val array: SparseArray<SpanPair> = SparseArray()
            if (text is Spannable) {
                val allSpans = text.getSpans(
                    0, text.length,
                    Any::class.java
                )
                var start = 0
                for (span in allSpans) {
                    if (span !is ReplacementSpan) {
                        continue
                    }
                    val pair: SpanPair = SpanPair.from(text, span, SpanPair.REPLACEMENT_SPAN_TYPE)
                    array.append(pair.start, pair)
                    start = max(start, pair.end)
                }
            }

            val matcher = extraEmoji.matcher(text)
            while (matcher.find()) {
                val matchStart = matcher.start()
                if (array.indexOfKey(matchStart) < 0) {
                    array.append(matchStart, SpanPair(matchStart, matcher.end(), SpanPair.EMOJI_TYPE))
                }
            }
            return array
        }
    }
}