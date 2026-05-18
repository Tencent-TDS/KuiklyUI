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

#ifndef CORE_RENDER_OHOS_KRTEXTPOSTPROCESSOR_H
#define CORE_RENDER_OHOS_KRTEXTPOSTPROCESSOR_H

// SDK 内部头：仅供 core-render-ohos 内部使用，不对外导出。
// 对外 API 见 libohos_render/api/include/Kuikly/Kuikly.h 中的
// KRRegisterTextPostProcessorAdapter / KRTextProcessedResultAppend* 系列。

#include <string>
#include <vector>

namespace kuikly {
namespace text {

// 单个 span 描述：text or image。
struct KRTextPostProcessSpan {
    enum class Type { kText, kImage };
    Type type = Type::kText;
    std::string text_or_src;  // text 段：UTF-8 文本；image 段：可寻址 URI
    // 仅 image 段使用：image 在「raw 原始文本」中对应的字面量（如 "[smile]"）。
    //
    // 业务侧通过 KRTextProcessedResultAppendImageSpanWithRaw 显式回传；
    // 调老接口 KRTextProcessedResultAppendImageSpan（不含 raw_literal）时，此字段为空，
    // SDK 在编辑差分回写时会把该 image 视为单空格占位（编辑后无法精准还原 raw 文本）。
    //
    // 与 iOS `KRTextAttachmentStringProtocol::kr_originlTextBeforeTextAttachment`
    // 在职责边界上对齐：让 image span 自身携带"raw 字面量"，避免 SDK 侧再做模式匹配
    // / 二次推断，编辑差分回写算法完全基于权威映射推进。
    std::string raw_literal;
    float width = 0.0f;       // 仅 image 段使用，<=0 表示按字号自适应
    float height = 0.0f;      // 仅 image 段使用，<=0 表示按字号自适应
};

// 调用已注册的具名 adapter（如 "input"）。
// 返回 true 表示 adapter 已注册并产生了非空 Span 序列；out_spans 被填充。
// 返回 false 表示未注册 / 返回空，调用方应走原始文本路径。
bool RunTextPostProcessor(const std::string &name,
                          const std::string &text,
                          std::vector<KRTextPostProcessSpan> &out_spans);

}  // namespace text
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRTEXTPOSTPROCESSOR_H
