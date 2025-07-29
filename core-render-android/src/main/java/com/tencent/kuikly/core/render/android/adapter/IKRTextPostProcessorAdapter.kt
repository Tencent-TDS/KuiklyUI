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

package com.tencent.kuikly.core.render.android.adapter

import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.expand.component.KRTextProps

/**
 * Created by kam on 2023/10/24.
 * 文本后置处理器, 业务可实现此适配器，来实现文本的后置处理
 * 例如：替换emoji，抓取链接等效果
 */
interface IKRTextPostProcessorAdapter {

    /**
     * Core内部检测到文本需要后置处理，调用此方法，业务在此方法中，自定义文本后置处理
     * @param inputParams 处理出入参数
     * @return 处理输出参数
     */
    @Deprecated("请使用带有 IKuiklyRenderContext 的新方法，override 新方法后，本方法将不再被调用")
    fun onTextPostProcess(inputParams: TextPostProcessorInput): TextPostProcessorOutput

    /**
     * Core内部检测到文本需要后置处理，调用此方法，业务在此方法中，自定义文本后置处理
     * @param inputParams 处理出入参数
     * @return 处理输出参数
     */
    fun onTextPostProcess(
        kuiklyRenderContext: IKuiklyRenderContext?,
        inputParams: TextPostProcessorInput
    ): TextPostProcessorOutput = onTextPostProcess(inputParams)
}

/**
 * 文本后置处理器输入参数
 * @param processor 处理器名字，有Kuikly业务上层定义
 * @param sourceText 原先文本
 * @param textProps 文本属性集
 */
class TextPostProcessorInput(val processor: String, val sourceText: CharSequence, val textProps: KRTextProps)

/**
 * 文本后置处理器输出参数
 * @param text 后置处理的文本
 */
class TextPostProcessorOutput(val text: CharSequence)