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

#ifndef CORE_RENDER_OHOS_KRLINEARGRADIENTPARSER_H
#define CORE_RENDER_OHOS_KRLINEARGRADIENTPARSER_H

#include <string>
#include <vector>
#include "libohos_render/utils/KRConvertUtil.h"

namespace kuikly {
namespace util {
class KRLinearGradientParser : public std::enable_shared_from_this<KRLinearGradientParser> {
 private:
    int direction = 0;
    std::vector<uint32_t> colors;
    std::vector<float> locations;

 public:
    bool ParseFromCssLinearGradient(const std::string &cssGradient);

    // 添加访问器方法
    const std::vector<uint32_t> &GetColors() const {
        return colors;
    }

    const std::vector<float> &GetLocations() const {
        return locations;
    }
    const int GetArkUIDirection() const;
    OH_Drawing_Point *GetStartPoint(double width, double height);
    OH_Drawing_Point *GetEndPoint(double width, double height);
};
}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRLINEARGRADIENTPARSER_H
