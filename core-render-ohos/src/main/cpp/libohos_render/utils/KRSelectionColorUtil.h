/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRSELECTIONCOLORUTIL_H
#define CORE_RENDER_OHOS_KRSELECTIONCOLORUTIL_H

#include <cstdint>
#include <string>

namespace kuikly {
namespace util {

// ARGB 颜色位操作常量
static constexpr uint32_t kColorAlphaShift = 24;
static constexpr uint32_t kColorAlphaMask = 0xFF;
static constexpr uint32_t kColorRGBMask = 0x00FFFFFF;

// 选中高亮色 alpha 上限（0x66 ≈ 40%），避免高亮完全覆盖文字，多端统一
static constexpr uint32_t kSelectionColorMaxAlpha = 0x66;

// 上层未配置 selectionColor 时选区底色的回退值，沿用迁移前 KRRichTextView 的硬编码值。
// 三端默认色并不相同（Android KRTextSelector 用 textColorHighlight 兜底，iOS KRLabel 用内置
// 0x0099FF@0.3），此取值与它们无关，只是保持鸿蒙既有观感不变。
static constexpr uint32_t kDefaultSelectionColor = 0x33007DFF;

/// 限制选中色 alpha 不超过 kSelectionColorMaxAlpha，返回 clamped 后的颜色值
uint32_t ClampSelectionColorAlpha(uint32_t color);

/// 解析容器下发的 selectionColor（形如 {"background": 0x66RRGGBB, "cursor": 0xFFRRGGBB}，
/// 见 DivView.selectionColor），取 background 分量并按 kSelectionColorMaxAlpha 收敛。
/// background 缺失、非数字或整体不是合法 JSON 时返回 fallback。
uint32_t ParseSelectionBackgroundColor(const std::string &selection_color, uint32_t fallback);

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRSELECTIONCOLORUTIL_H
