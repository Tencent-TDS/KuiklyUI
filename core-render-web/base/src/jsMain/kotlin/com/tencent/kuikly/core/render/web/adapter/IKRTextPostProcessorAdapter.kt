package com.tencent.kuikly.core.render.web.adapter

import com.tencent.kuikly.core.render.web.expand.components.KRTextProps

interface IKRTextPostProcessorAdapter {
    fun onTextPostProcess(inputParams: TextPostProcessorInput): TextPostProcessorOutput
}

class TextPostProcessorInput(
    val processor: String,
    val sourceText: CharSequence,
    val textProps: KRTextProps
)

class TextPostProcessorOutput(val text: CharSequence)
