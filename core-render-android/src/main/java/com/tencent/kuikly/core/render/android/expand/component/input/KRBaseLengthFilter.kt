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
import android.text.Spanned
import android.text.style.ReplacementSpan
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.TextPostProcessorInput
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.expand.component.KRTextProps

/**
 * 输入长度限制InputFilter的基类
 * 
 * 提供通用的filter逻辑框架，子类只需实现具体的长度计算和截取方法
 */
abstract class KRBaseLengthFilter(
    protected val maxLimit: Int,
    protected val kuiklyRenderContext: IKuiklyRenderContext?,
    protected val fontSizeGetter: () -> Float,
    protected val textLengthBeyondLimitCallback: () -> Unit
) : InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        if (start >= end) {
            return null // quick return for deletion
        }
        val keep =
            maxLimit - calculateLength(dest, 0, dstart) - calculateLength(dest, dend, dest.length)
        if (keep <= 0) {
            textLengthBeyondLimitCallback.invoke()
            return KRCssConst.EMPTY_STRING
        }

        val richSource = createRichText(
            source,
            start,
            end,
            kuiklyRenderContext,
            fontSizeGetter
        )
        if (richSource == source) {
            if (keep >= calculateLength(source, start, end)) {
                return null // keep original
            } else {
                // need to truncate
                textLengthBeyondLimitCallback.invoke()
                return truncate(source, start, end, keep)
            }
        } else {
            if (keep >= calculateLength(richSource)) {
                return richSource
            } else {
                // need to truncate
                textLengthBeyondLimitCallback.invoke()
                return truncate(richSource, maxLength = keep)
            }
        }
    }

    /**
     * 计算文本的长度（根据不同的限制类型，可能是字节数、字符数、视觉宽度等）
     * @param text 要计算的文本
     * @param start 文本起始位置
     * @param end 文本结束位置
     * @return 文本的长度值
     */
    abstract fun calculateLength(text: CharSequence, start: Int = 0, end: Int = text.length): Int

    /**
     * 截取文本到指定的长度限制
     * @param text 要截取的文本
     * @param start 文本起始位置
     * @param end 文本结束位置
     * @param maxLength 最大长度限制
     * @return 截取后的文本
     */
    protected abstract fun truncate(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
        maxLength: Int
    ): CharSequence

    companion object Companion {

        internal inline fun subSequence(text: CharSequence, start: Int, end: Int) =
            if (start > 0 || end < text.length) text.subSequence(start, end) else text

        /**
         * 创建富文本（应用文本后处理器）
         */
        internal fun createRichText(
            origin: CharSequence,
            start: Int,
            end: Int,
            kuiklyRenderContext: IKuiklyRenderContext?,
            fontSizeGetter: () -> Float
        ): CharSequence {
            val textPostProcessorAdapter = KuiklyRenderAdapterManager.krTextPostProcessorAdapter
                ?: return origin
            val textProp = KRTextProps(kuiklyRenderContext).apply {
                setProp(KRTextProps.PROP_KEY_FONT_SIZE, fontSizeGetter.invoke())
            }
            val inputParams = TextPostProcessorInput(
                "input",
                subSequence(origin, start, end),
                textProp
            )
            return textPostProcessorAdapter.onTextPostProcess(
                kuiklyRenderContext,
                inputParams
            ).text
        }

        internal fun trimReplacementSpanStart(text: CharSequence, start: Int): Int {
            var tStart = start
            if (text is Spanned) {
                val spans = text.getSpans(start, start, ReplacementSpan::class.java)
                for (span in spans) {
                    val spanStart = text.getSpanStart(span)
                    val spanEnd = text.getSpanEnd(span)
                    if (start > spanStart && start < spanEnd) {
                        tStart = maxOf(tStart, spanEnd)
                    }
                }
            }
            return tStart
        }

        internal fun trimReplacementSpanEnd(text: CharSequence, end: Int): Int {
            var tEnd = end
            if (text is Spanned) {
                val spans = text.getSpans(end, end, ReplacementSpan::class.java)
                for (span in spans) {
                    val spanStart = text.getSpanStart(span)
                    val spanEnd = text.getSpanEnd(span)
                    if (end > spanStart && end < spanEnd) {
                        tEnd = minOf(tEnd, spanStart)
                    }
                }
            }
            return tEnd
        }

        internal inline fun CharSequence.forEachCodePoint(
            start: Int,
            end: Int,
            action: (codePoint: Int) -> Unit
        ) {
            var index = start
            val last = end - 1
            while (index < last) {
                val codePoint = Character.codePointAt(this, index)
                index += Character.charCount(codePoint)
                action(codePoint)
            }
            if (index == last) {
                val codePoint = this[index].code
                action(codePoint)
            }
        }
    }
}

