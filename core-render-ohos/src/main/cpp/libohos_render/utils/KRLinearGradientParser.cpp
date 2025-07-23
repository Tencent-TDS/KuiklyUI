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

#include "libohos_render/utils/KRLinearGradientParser.h"

#include <native_drawing/drawing_point.h>
#include "libohos_render/utils/KRConvertUtil.h"

namespace kuikly {
namespace util {
bool KRLinearGradientParser::ParseFromCssLinearGradient(const std::string &cssGradient) {
    const std::string lineargradientPrefix = "linear-gradient(";
    if (cssGradient.find(lineargradientPrefix) != 0) {
        return false;
    }
    std::string cssGradientSubstring =
        cssGradient.substr(lineargradientPrefix.length(), cssGradient.length() - lineargradientPrefix.length() - 1);
    std::vector<std::string> splits = ConvertSplit(cssGradientSubstring, ",");
    direction = std::stoi(splits[0]);

    colors.clear();
    locations.clear();

    for (size_t i = 1; i < splits.size(); ++i) {
        std::string colorStopStr = splits[i];
        std::vector<std::string> colorAndStop = ConvertSplit(colorStopStr, " ");
        if (colorAndStop.size() >= 2) {
            colors.push_back(ConvertToHexColor(colorAndStop[0]));
            locations.push_back(std::stof(colorAndStop[1]));
        }
    }

    return true;
}
const int KRLinearGradientParser::GetArkUIDirection() const {
    switch (direction) {
    case 0:
        return 1;
    case 1:
        return 3;
    case 2:
        return 0;
    case 3:
        return 2;
    case 4:
        return 4;
    case 5:
        return 6;
    case 6:
        return 5;
    case 7:
        return 7;
    default:
        return 8;  // 不渐变
    }
    return 8;
}

OH_Drawing_Point *KRLinearGradientParser::GetStartPoint(double width, double height) {
    int direction = GetArkUIDirection();
    double x = 0;
    double y = 0;

    switch (direction) {
    case 0:  // 向左
        x = width;
        break;
    case 1:  // 向上
        y = height;
        break;
    case 2:  // 向右
        break;
    case 3:  // 向下
        break;
    case 4:  // 左上
        y = height;
        x = width;
        break;
    case 5:  // 左下
        x = width;
        break;
    case 6:  // 右上
        y = height;
        break;
    case 7:  // 右下
        break;
    }

    return OH_Drawing_PointCreate(x, y);
}

OH_Drawing_Point *KRLinearGradientParser::GetEndPoint(double width, double height) {
    int direction = GetArkUIDirection();
    double x = 0;
    double y = 0;

    switch (direction) {
    case 0:  // 向左
        break;
    case 1:  // 向上
        break;
    case 2:  // 向右
        x = width;
        break;
    case 3:  // 向下
        y = height;
        break;
    case 4:  // 左上
        break;
    case 5:  // 左下
        y = height;
        break;
    case 6:  // 右上
        x = width;
        break;
    case 7:  // 右下
        y = height;
        x = width;
        break;
    }

    return OH_Drawing_PointCreate(x, y);
}
}  // namespace util
}  // namespace kuikly
