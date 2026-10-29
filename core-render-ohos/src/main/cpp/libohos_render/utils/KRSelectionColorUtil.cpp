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

namespace {

/// 读取 JSON 对象里的 uint32 颜色字段。缺字段、非数字、负数、超 UINT32_MAX 与 NaN 都返回 false：
/// double 转 uint32_t 时超出目标范围是未定义行为，NaN 参与比较也一律为假。
bool ReadColorField(const cJSON *root, const char *key, uint32_t *out) {
    const cJSON *item = cJSON_GetObjectItemCaseSensitive(root, key);
    if (!cJSON_IsNumber(item)) {
        return false;
    }
    const double value = item->valuedouble;
    if (value < 0 || value > static_cast<double>(UINT32_MAX)) {
        return false;
    }
    *out = static_cast<uint32_t>(value);
    return true;
}

}  // namespace

uint32_t ClampSelectionColorAlpha(uint32_t color) {
    uint32_t alpha = (color >> kColorAlphaShift) & kColorAlphaMask;
    if (alpha > kSelectionColorMaxAlpha) {
        color = (kSelectionColorMaxAlpha << kColorAlphaShift) | (color & kColorRGBMask);
    }
    return color;
}

KRSelectionColors ParseSelectionColors(const std::string &selection_color) {
    KRSelectionColors colors;
    if (selection_color.empty()) {
        return colors;
    }
    cJSON *root = cJSON_Parse(selection_color.c_str());
    if (root == nullptr) {
        return colors;
    }
    uint32_t background = 0;
    if (ReadColorField(root, "background", &background)) {
        colors.background = ClampSelectionColorAlpha(background);
    }
    uint32_t cursor = 0;
    if (ReadColorField(root, "cursor", &cursor)) {
        colors.cursor = cursor;
    }
    cJSON_Delete(root);
    return colors;
}

}  // namespace util
}  // namespace kuikly
