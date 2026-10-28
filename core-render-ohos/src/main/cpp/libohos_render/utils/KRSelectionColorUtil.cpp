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

#include "libohos_render/utils/KRSelectionColorUtil.h"

#include <cstdint>

#include "thirdparty/cJSON/cJSON.h"

namespace kuikly {
namespace util {

uint32_t ClampSelectionColorAlpha(uint32_t color) {
    uint32_t alpha = (color >> kColorAlphaShift) & kColorAlphaMask;
    if (alpha > kSelectionColorMaxAlpha) {
        color = (kSelectionColorMaxAlpha << kColorAlphaShift) | (color & kColorRGBMask);
    }
    return color;
}

uint32_t ParseSelectionBackgroundColor(const std::string &selection_color, uint32_t fallback) {
    if (selection_color.empty()) {
        return fallback;
    }
    cJSON *root = cJSON_Parse(selection_color.c_str());
    if (root == nullptr) {
        return fallback;
    }
    uint32_t background = fallback;
    // 取值是 0xRRGGBB 形式的整数，double 可精确承载 32 位整数
    const cJSON *background_item = cJSON_GetObjectItemCaseSensitive(root, "background");
    if (cJSON_IsNumber(background_item)) {
        background = ClampSelectionColorAlpha(static_cast<uint32_t>(background_item->valuedouble));
    }
    cJSON_Delete(root);
    return background;
}

}  // namespace util
}  // namespace kuikly
