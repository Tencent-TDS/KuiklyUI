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

#ifndef CORE_RENDER_OHOS_KRTRANSFORMPARSER_H
#define CORE_RENDER_OHOS_KRTRANSFORMPARSER_H

#include <memory>

namespace kuikly {
namespace util {
class KRTransformParser : public std::enable_shared_from_this<KRTransformParser> {
 public:
    double anchor_x_ = 0.5;     // default 0.5
    double anchor_y_ = 0.5;     // default 0.5
    double translation_x_ = 0;  // default 0.0
    double translation_y_ = 0;  // default 0.0
    double rotate_angle_ = 0;   // default 0 [-360, 360] deg角度
    double rotate_x_angle_ = 0; // default 0 [-360, 360] deg角度
    double rotate_y_angle_ = 0; // default 0 [-360, 360] deg角度
    double scale_x_ = 1;        // default 1
    double scale_y_ = 1;        // default 1
    double skew_x_ = 0;         // 水平方向倾斜角度[-180, 180]
    double skew_y_ = 0;         // 垂直方向倾斜角度[-180, 180]

 private:
    std::array<double, 16> GenerateTransformMatrix(double translateX, double translateY, double scaleX, double scaleY,
                                                   double rotate, double skewX, double skewY);

 public:
    bool ParseFromCssTransform(const std::string &css_transform);

    std::array<double, 16> GetMatrixWithNoRotate();
};
}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRTRANSFORMPARSER_H
