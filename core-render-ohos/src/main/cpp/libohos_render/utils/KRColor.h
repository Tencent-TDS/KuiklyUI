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

#ifndef CORE_RENDER_OHOS_KRCOLOR_H
#define CORE_RENDER_OHOS_KRCOLOR_H
#include <string>

#include "KRStringUtil.h"

namespace kuikly {
namespace graphics {
using Float = float;

struct Color {
    union {
        struct {
            unsigned char b, g, r, a;
        };
        unsigned int value;
    };

    Float alpha() {
        return a / 255.0;
    }
    Float red() {
        return r / 255.0;
    }
    Float green() {
        return g / 255.0;
    }
    Float blue() {
        return b / 255.0;
    }

    static struct Color FromString(const std::string &colorString) {
        struct Color ret;
        // ets侧给的颜色字符串示例：rgba(0,0,0,0.9411764705882353)
        if (colorString.length() > 0) {
            std::string prefix("rgba(");
            if (colorString.compare(0, prefix.length(), prefix) == 0) {
                std::string valueString =
                    colorString.substr(prefix.length(), colorString.length() - prefix.length() - 1);
                std::vector<std::string> values = kuikly::util::SplitString(valueString, ",");
                if (values.size() == 4) {
                    uint32_t r = values[0].length() > 0 ? std::stoul(values[0]) & 0xff : 0;
                    uint32_t g = values[1].length() > 0 ? std::stoul(values[1]) & 0xff : 0;
                    uint32_t b = values[2].length() > 0 ? std::stoul(values[2]) & 0xff : 0;
                    uint32_t a = values[3].length() > 0 ? (uint32_t)(std::stof(values[3]) * 0xff) & 0xff : 0xff;
                    ret.value = (a << 24) | (r << 16) | (g << 8) | b;
                }
            } else if (colorString.rfind("#", 0) == 0) {
                if (colorString.size() == 9) {
                    // #AARRGGBB
                    unsigned int v = strtoul(((const char *)colorString.data()) + 1, NULL, 16);
                    ret.value = v;
                } else if (colorString.size() == 7) {
                    // #RRGGBB
                    unsigned int v = strtoul(((const char *)colorString.data()) + 1, NULL, 16);
                    v = v | 0xFF000000;
                    ret.value = v;
                } else {
                    ret.value = 0xff000000;
                }
            } else {
                // 10 base integer string
                ret.value = std::atoi(colorString.c_str());
            }
        }
        return ret;
    }
};
}  // namespace graphics
}  // namespace kuikly
#endif  // CORE_RENDER_OHOS_KRCOLOR_H
