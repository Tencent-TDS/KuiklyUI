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

package com.tencent.kuikly.android.demo.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import com.tencent.kuikly.core.render.android.adapter.IKRTextPostProcessorAdapter
import com.tencent.kuikly.core.render.android.adapter.TextPostProcessorInput
import com.tencent.kuikly.core.render.android.adapter.TextPostProcessorOutput
import com.tencent.kuikly.core.render.android.css.ktx.toPxF

/**
 * 自定义表情后置处理器
 * 将文本中的 emoji 短码（如 [smile]）替换为对应的图片 span
 *
 * 实现方式：在短码字符位置上设置 ImageSpan，保留原始字符
 * 返回 SpannableStringBuilder（实现了 Editable 接口），这样 EditText 可以正确显示
 */
class KRTextPostProcessorAdapter(context: Context) : IKRTextPostProcessorAdapter {

    private val appContext: Context = context.applicationContext

    companion object {
        // 短码 -> drawable 资源映射
        private val EMOJI_MAP = mapOf(
            "[smile]" to com.tencent.kuikly.android.demo.R.drawable.emoji_smile,
            "[heart]" to com.tencent.kuikly.android.demo.R.drawable.emoji_heart,
            "[thumbup]" to com.tencent.kuikly.android.demo.R.drawable.emoji_thumbup,
            "[star]" to com.tencent.kuikly.android.demo.R.drawable.emoji_star,
            "[fire]" to com.tencent.kuikly.android.demo.R.drawable.emoji_fire,
        )

        // 匹配 [xxx] 格式的短码（含中文）
        private val EMOJI_PATTERN = "\\[([^\\]]+)\\]".toRegex()
    }

    override fun onTextPostProcess(
        kuiklyRenderContext: com.tencent.kuikly.core.render.android.IKuiklyRenderContext?,
        inputParams: TextPostProcessorInput
    ): TextPostProcessorOutput {
        return when (inputParams.processor) {
            "emoji", "input" -> processEmoji(inputParams)
            else -> TextPostProcessorOutput(inputParams.sourceText)
        }
    }

    /**
     * Emoji processor: replace shortcode like [smile] with ImageSpan
     */
    private fun processEmoji(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        val sourceText = inputParams.sourceText?.toString() ?: return TextPostProcessorOutput("")

        // 查找所有短码匹配
        val matches = EMOJI_PATTERN.findAll(sourceText).toList()
        if (matches.isEmpty()) {
            return TextPostProcessorOutput(SpannableStringBuilder(sourceText))
        }

        // 计算 emoji 大小（基于字体大小）
        val fontSize = inputParams.textProps.fontSize.toPxF()
        val emojiSize = fontSize.toInt().coerceAtLeast(48)

        // 使用 SpannableStringBuilder 以保留原始字符（短码）并附加 ImageSpan
        val spannable = SpannableStringBuilder(sourceText)

        for (match in matches) {
            val shortcode = match.value
            val drawableRes = EMOJI_MAP[shortcode]
            if (drawableRes != null) {
                val drawable: Drawable? = ContextCompat.getDrawable(appContext, drawableRes)
                drawable?.setBounds(0, 0, emojiSize, emojiSize)
                if (drawable != null) {
                    spannable.setSpan(
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            // Android 10 (API 29) 上系统 ImageSpan 换行时基线计算有 bug，使用自定义实现绕过
                            CenterAlignedImageSpan(drawable)
                        } else {
                            ImageSpan(drawable, DynamicDrawableSpan.ALIGN_CENTER)
                        },
                        match.range.first,
                        match.range.last + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        return TextPostProcessorOutput(spannable)
    }

    /**
     * Android 10 (API 29) 专用：修复系统 [ImageSpan] 在换行时基线计算错误导致的错位问题。
     *
     * 系统 bug 原因：Android 10 上 [DynamicDrawableSpan] 的 [getSize] 方法对行高的计算
     * 与文本实际行高不一致，导致跨行时图片垂直位置偏移。
     *
     * 解决方案：手动计算图片在所在行的垂直居中位置，精确控制绘制。
     */
    private class CenterAlignedImageSpan(drawable: Drawable) : ImageSpan(drawable) {

        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?,
        ): Int {
            val drawable = drawable
            val rect: Rect = drawable.bounds

            // 计算行高并设置 FontMetricsInt，确保图片在行内垂直居中
            fm?.let {
                val fontHeight = paint.fontMetricsInt.descent - paint.fontMetricsInt.ascent
                val drHeight = rect.height()
                val top = fontHeight / 2 - drHeight / 2
                val bottom = fontHeight / 2 + drHeight / 2

                it.ascent = -bottom
                it.top = -bottom
                it.bottom = top
                it.descent = top
            }

            return rect.right
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence?,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint,
        ) {
            val drawable = drawable
            canvas.save()

            // 计算垂直居中偏移
            val fontMetrics = paint.fontMetricsInt
            val transY = y + fontMetrics.ascent + (fontMetrics.descent - fontMetrics.ascent) / 2 - drawable.bounds.height() / 2

            canvas.translate(x, transY.toFloat())
            drawable.draw(canvas)
            canvas.restore()
        }
    }

    // 保留旧方法以兼容接口
    @Deprecated("Use onTextPostProcess(kuiklyRenderContext, inputParams) instead")
    override fun onTextPostProcess(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        return onTextPostProcess(null, inputParams)
    }
}
