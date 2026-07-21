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
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
    private val crashReproHandler = Handler(Looper.getMainLooper())
    private val crashReproBuilder = SpannableStringBuilder()
    private var crashReproMutationUntil = 0L
    private var crashReproMutationScheduled = false

    private val crashReproMutation = object : Runnable {
        override fun run() {
            if (SystemClock.uptimeMillis() >= crashReproMutationUntil) {
                crashReproMutationScheduled = false
                return
            }
            // Deliberately mutate the same CharSequence after StaticLayout may have
            // captured it. This is a demo-only reproduction of the production risk.
            if (crashReproBuilder.length > 1) {
                crashReproBuilder.delete(1, crashReproBuilder.length)
            } else {
                crashReproBuilder.append("x")
            }
            crashReproHandler.postDelayed(this, 4L)
        }
    }

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
            "crash_repro" -> processCrashRepro(inputParams)
            else -> TextPostProcessorOutput(inputParams.sourceText)
        }
    }

    private fun processCrashRepro(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        // Intentionally return one shared mutable builder. The normal processors above
        // must continue returning independent values and are not part of this test.
        crashReproBuilder.clear()
        crashReproBuilder.append(inputParams.sourceText.toString())
        crashReproMutationUntil = SystemClock.uptimeMillis() + 8_000L
        if (!crashReproMutationScheduled) {
            crashReproMutationScheduled = true
            // Let the first StaticLayout finish measuring. Later mutations then
            // target the Layout.draw() window instead of failing in getDesiredWidth.
            crashReproHandler.postDelayed(crashReproMutation, 20L)
        }
        return TextPostProcessorOutput(crashReproBuilder)
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

    // 保留旧方法以兼容接口
    @Deprecated("Use onTextPostProcess(kuiklyRenderContext, inputParams) instead")
    override fun onTextPostProcess(inputParams: TextPostProcessorInput): TextPostProcessorOutput {
        return onTextPostProcess(null, inputParams)
    }
}
