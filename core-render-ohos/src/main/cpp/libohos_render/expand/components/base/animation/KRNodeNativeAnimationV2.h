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

#ifndef CORE_RENDER_OHOS_KRNODENATIVEANIMATIONV2_H
#define CORE_RENDER_OHOS_KRNODENATIVEANIMATIONV2_H

#include <arkui/native_animate.h>
#include <string>
#include <vector>
#include "libohos_render/utils/KRConvertUtil.h"

struct KRNodeNativeAnimationV2 {
    std::string kind;
    std::vector<float> values;

    static KRNodeNativeAnimationV2 Parse(const std::vector<std::string> &tokens) {
        KRNodeNativeAnimationV2 descriptor;
        for (const auto &token : tokens) {
            if (token.rfind("v2,", 0) != 0) {
                continue;
            }
            auto payload = kuikly::util::ConvertSplit(token, ",");
            if (payload.size() < 2) {
                continue;
            }
            descriptor.kind = payload[1];
            for (size_t index = 2; index < payload.size(); index++) {
                descriptor.values.push_back(kuikly::util::ConvertToFloat(payload[index]));
            }
            break;
        }
        return descriptor;
    }
};

inline ArkUI_CurveHandle KRCreateNativeCubicBezierCurve(const std::vector<float> &values) {
    if (values.size() != 4) {
        return nullptr;
    }
    return OH_ArkUI_Curve_CreateCubicBezierCurve(values[0], values[1], values[2], values[3]);
}

#endif  // CORE_RENDER_OHOS_KRNODENATIVEANIMATIONV2_H
