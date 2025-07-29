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

#include "libohos_render/utils/KRTransformParser.h"

#include "libohos_render/utils/KRConvertUtil.h"

namespace kuikly {
namespace util {
/// 矩阵相乘
std::array<double, 16> MultiplyMatrices(const std::array<double, 16> &lhs, const std::array<double, 16> &rhs) {
    std::array<double, 16> result;

    auto lhs00 = lhs[0];
    auto lhs01 = lhs[1];
    auto lhs02 = lhs[2];
    auto lhs03 = lhs[3];
    auto lhs10 = lhs[4];
    auto lhs11 = lhs[5];
    auto lhs12 = lhs[6];
    auto lhs13 = lhs[7];
    auto lhs20 = lhs[8];
    auto lhs21 = lhs[9];
    auto lhs22 = lhs[10];
    auto lhs23 = lhs[11];
    auto lhs30 = lhs[12];
    auto lhs31 = lhs[13];
    auto lhs32 = lhs[14];
    auto lhs33 = lhs[15];

    auto rhs0 = rhs[0];
    auto rhs1 = rhs[1];
    auto rhs2 = rhs[2];
    auto rhs3 = rhs[3];
    result[0] = rhs0 * lhs00 + rhs1 * lhs10 + rhs2 * lhs20 + rhs3 * lhs30;
    result[1] = rhs0 * lhs01 + rhs1 * lhs11 + rhs2 * lhs21 + rhs3 * lhs31;
    result[2] = rhs0 * lhs02 + rhs1 * lhs12 + rhs2 * lhs22 + rhs3 * lhs32;
    result[3] = rhs0 * lhs03 + rhs1 * lhs13 + rhs2 * lhs23 + rhs3 * lhs33;

    rhs0 = rhs[4];
    rhs1 = rhs[5];
    rhs2 = rhs[6];
    rhs3 = rhs[7];
    result[4] = rhs0 * lhs00 + rhs1 * lhs10 + rhs2 * lhs20 + rhs3 * lhs30;
    result[5] = rhs0 * lhs01 + rhs1 * lhs11 + rhs2 * lhs21 + rhs3 * lhs31;
    result[6] = rhs0 * lhs02 + rhs1 * lhs12 + rhs2 * lhs22 + rhs3 * lhs32;
    result[7] = rhs0 * lhs03 + rhs1 * lhs13 + rhs2 * lhs23 + rhs3 * lhs33;

    rhs0 = rhs[8];
    rhs1 = rhs[9];
    rhs2 = rhs[10];
    rhs3 = rhs[11];
    result[8] = rhs0 * lhs00 + rhs1 * lhs10 + rhs2 * lhs20 + rhs3 * lhs30;
    result[9] = rhs0 * lhs01 + rhs1 * lhs11 + rhs2 * lhs21 + rhs3 * lhs31;
    result[10] = rhs0 * lhs02 + rhs1 * lhs12 + rhs2 * lhs22 + rhs3 * lhs32;
    result[11] = rhs0 * lhs03 + rhs1 * lhs13 + rhs2 * lhs23 + rhs3 * lhs33;

    rhs0 = rhs[12];
    rhs1 = rhs[13];
    rhs2 = rhs[14];
    rhs3 = rhs[15];
    result[12] = rhs0 * lhs00 + rhs1 * lhs10 + rhs2 * lhs20 + rhs3 * lhs30;
    result[13] = rhs0 * lhs01 + rhs1 * lhs11 + rhs2 * lhs21 + rhs3 * lhs31;
    result[14] = rhs0 * lhs02 + rhs1 * lhs12 + rhs2 * lhs22 + rhs3 * lhs32;
    result[15] = rhs0 * lhs03 + rhs1 * lhs13 + rhs2 * lhs23 + rhs3 * lhs33;
    return result;
}

bool KRTransformParser::ParseFromCssTransform(const std::string &css_transform) {
    const size_t ROTATE_INDEX = 0;
    const size_t SCALE_INDEX = 1;
    const size_t TRANSLATE_INDEX = 2;
    const size_t ANCHOR_INDEX = 3;
    const size_t SKEW_INDEX = 4;
    const size_t ROTATE_XY_INDEX = 5;
    
    auto splits = ConvertSplit(css_transform, "|");
    // 至少5个参数
    if (splits.size() < 5) {
        return false;
    }
    
    auto checkSize = [](const std::vector<std::string>& v, size_t n) {
        return v.size() >= n;
    };
    
    rotate_angle_ = ConvertToDouble(splits[ROTATE_INDEX]);
    
    auto scales = ConvertSplit(splits[SCALE_INDEX], " ");
    if (!checkSize(scales, 2)) return false;
    scale_x_ = ConvertToDouble(scales[0]);
    scale_y_ = ConvertToDouble(scales[1]);
    
    auto translates = ConvertSplit(splits[TRANSLATE_INDEX], " ");
    if (!checkSize(translates, 2)) return false;
    translation_x_ = ConvertToDouble(translates[0]);
    translation_y_ = ConvertToDouble(translates[1]);
    auto anchors = ConvertSplit(splits[ANCHOR_INDEX], " ");
    if (!checkSize(anchors, 2)) return false;
    anchor_x_ = ConvertToDouble(anchors[0]);
    anchor_y_ = ConvertToDouble(anchors[1]);
    
    auto skews = ConvertSplit(splits[SKEW_INDEX], " ");
    if (!checkSize(skews, 2)) return false;
    skew_x_ = ConvertToDouble(skews[0]);
    skew_y_ = ConvertToDouble(skews[1]);
    
    // 旋转XY
    if (splits.size() > ROTATE_XY_INDEX) {
        auto rotateXY = ConvertSplit(splits[ROTATE_XY_INDEX], " ");
        if (checkSize(rotateXY, 2)) {
            rotate_x_angle_ = ConvertToDouble(rotateXY[0]);
            rotate_y_angle_ = ConvertToDouble(rotateXY[1]);
        }
    } else {
        rotate_x_angle_ = 0.0;
        rotate_y_angle_ = 0.0;
    }
    
    return true;
}

std::array<double, 16> KRTransformParser::GetMatrixWithNoRotate() {
    return GenerateTransformMatrix(translation_x_, translation_y_, scale_x_, scale_y_, 0, skew_x_, skew_y_);
}

std::array<double, 16> KRTransformParser::GenerateTransformMatrix(double translateX, double translateY, double scaleX,
                                                                  double scaleY, double rotate, double skewX,
                                                                  double skewY) {
    // Initialize identity matrices for scale, rotation, and skew
    std::array<double, 16> scaleMatrix = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    std::array<double, 16> rotationMatrix = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    std::array<double, 16> skewMatrix = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

    // Apply translation
    scaleMatrix[12] = translateX;
    scaleMatrix[13] = translateY;

    // Apply scale
    scaleMatrix[0] *= scaleX;
    scaleMatrix[5] *= scaleY;

    // Apply rotation (clockwise)
    double radian = rotate * M_PI / 180.0;  // convert degree to radian
    double cos_radian = cos(radian);
    double sin_radian = sin(radian);

    rotationMatrix[0] = cos_radian;
    rotationMatrix[1] = sin_radian;
    rotationMatrix[4] = -sin_radian;
    rotationMatrix[5] = cos_radian;

    // Apply skew
    double skew_x_radian = -skewX * M_PI / 180.0;  // convert degree to radian (clockwise)
    double skew_y_radian = -skewY * M_PI / 180.0;  // convert degree to radian (clockwise)
    skewMatrix[4] = tan(skew_x_radian);
    skewMatrix[1] = tan(skew_y_radian);

    // Multiply the three matrices: scaleMatrix * rotationMatrix * skewMatrix
    std::array<double, 16> resultMatrix = MultiplyMatrices(scaleMatrix, rotationMatrix);
    //  return resultMatrix;
    std::array<double, 16> finalMatrix = MultiplyMatrices(resultMatrix, skewMatrix);

    return finalMatrix;
}
}  // namespace util
}  // namespace kuikly