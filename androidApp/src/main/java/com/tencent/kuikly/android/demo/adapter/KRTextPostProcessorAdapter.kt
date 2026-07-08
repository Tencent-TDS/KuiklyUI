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
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.text.style.ReplacementSpan
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min
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
            "dashed" -> processDashedUnderline(inputParams)
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
                        ImageSpan(drawable, DynamicDrawableSpan.ALIGN_CENTER),
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
     * 虚线下划线处理器：Kuikly 文本装饰本身不支持虚线，这里把整段文本交给自定义
     * [DashedUnderlineSpan]，由原生 TextView 在 baseline 下方画出贴合文字宽度的虚线。
     *
     * 使用方式（Compose DSL）：
     * ```
     * Text("带虚线下划线的文字", modifier = Modifier.textPostProcessor("dashed"))
     * ```
     */
    private fun processDashedUnderline(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        val sourceText = inputParams.sourceText?.toString()
        if (sourceText.isNullOrEmpty()) {
            return TextPostProcessorOutput(inputParams.sourceText)
        }
        val spannable = SpannableStringBuilder(sourceText)
        spannable.setSpan(
            DashedUnderlineSpan(),
            0,
            sourceText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return TextPostProcessorOutput(spannable)
    }

    // 保留旧方法以兼容接口
    @Deprecated("Use onTextPostProcess(kuiklyRenderContext, inputParams) instead")
    override fun onTextPostProcess(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        return onTextPostProcess(null, inputParams)
    }
}

/**
 * 在文字 baseline（基线）正下方手动绘制虚线的 [ReplacementSpan]。
 *
 * Kuikly 的 Text 装饰只有实线下划线，没有虚线；用原生 [ReplacementSpan] 接管整段文字的
 * 绘制，先画文字本身，再紧贴基线画一段段短线，实现“贴合文字宽度”的虚线下划线。
 */
class DashedUnderlineSpan : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        // 让文本宽度照常参与排版，高度沿用系统文本度量（fm 不动）
        return paint.measureText(text, start, end).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        baseline: Int,
        bottom: Int,
        paint: Paint
    ) {
        // 1. 先把文字画出来（沿用系统 paint，字体/颜色与页面一致）
        canvas.drawText(text, start, end, x, baseline.toFloat(), paint)

        // 2. 在基线下方画虚线。用一份拷贝的 paint，避免污染文字本身的 paint。
        val textWidth = paint.measureText(text, start, end)
        val thickness = max(1f, paint.textSize * 0.08f)
        val dash = paint.textSize * 0.45f
        val gap = paint.textSize * 0.35f
        val lineY = baseline + thickness * 2f // 略低于基线，避免压到文字的下沿

        val underlinePaint = Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = thickness
            color = paint.color // 与文字同色
        }

        var cx = x
        while (cx < x + textWidth) {
            val next = min(cx + dash, x + textWidth)
            canvas.drawLine(cx, lineY, next, lineY, underlinePaint)
            cx += dash + gap
        }
    }
}
