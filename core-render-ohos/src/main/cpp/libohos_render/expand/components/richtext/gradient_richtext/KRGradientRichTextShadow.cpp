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

#include "libohos_render/expand/components/richtext/gradient_richtext/KRGradientRichTextShadow.h"

#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_point.h>
#include <native_drawing/drawing_shader_effect.h>
#include "libohos_render/utils/KRLinearGradientParser.h"

const char *gBackgroundImage = "backgroundImage";
/**
 * 更新 shadow 对象属性时调用
 * @param prop_key 属性名
 * @param prop_value 属性数据
 */
void KRGradientRichTextShadow::SetProp(const std::string &prop_key, const KRAnyValue &prop_value) {
    if (strcmp(prop_key.c_str(), gBackgroundImage) == 0) {  // 背景渐变在文本组件这该切换为文字渐变
        // parse
        text_linearGradient_ = std::make_shared<kuikly::util::KRLinearGradientParser>();
        bool success = text_linearGradient_->ParseFromCssLinearGradient(prop_value->toString());
        if (!success) {
            text_linearGradient_ = nullptr;
        }
        return;
    }
    KRRichTextShadow::SetProp(prop_key, prop_value);
}

KRSize KRGradientRichTextShadow::CalculateRenderViewSize(double constraint_width, double constraint_height) {
    //
    calculate_width_ = 0;
    calculate_height_ = 0;
    auto size = KRRichTextShadow::CalculateRenderViewSize(constraint_width, constraint_height);
    if (text_linearGradient_ == nullptr) {
        return size;
    }
    calculate_width_ = size.width;
    calculate_height_ = size.height;
    KRRichTextShadow::CalculateRenderViewSize(constraint_width, constraint_height);
    return size;
}

void KRGradientRichTextShadow::DidBuildTextStyle(OH_Drawing_TextStyle *textStyle, double dpi) {
    if (calculate_width_ != 0 && calculate_height_ != 0 && text_linearGradient_ != nullptr) {
        // 文字渐变
        auto brush = OH_Drawing_BrushCreate();
        OH_Drawing_BrushSetAntiAlias(brush, true);  // 抗锯齿
        // 获取 colors 和 locations
        const std::vector<uint32_t> &colors = text_linearGradient_->GetColors();
        const std::vector<float> &locations = text_linearGradient_->GetLocations();

        // 创建 C 风格数组
        uint32_t colorsArray[colors.size()];
        float stopsArray[locations.size()];

        // 填充数组
        for (size_t i = 0; i < colors.size(); ++i) {
            colorsArray[i] = colors[i];
        }
        for (size_t i = 0; i < locations.size(); ++i) {
            if (i == locations.size() - 1) {
                stopsArray[i] = 1.0;
            } else {
                stopsArray[i] = locations[i];
            }
        }

        OH_Drawing_Point *startPt =
            text_linearGradient_->GetStartPoint(calculate_width_ * dpi, calculate_height_ * dpi);
        OH_Drawing_Point *endPt = text_linearGradient_->GetEndPoint(calculate_width_ * dpi, calculate_height_ * dpi);

        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreateLinearGradient(
            startPt, endPt, colorsArray, stopsArray, colors.size(), OH_Drawing_TileMode::CLAMP);
        OH_Drawing_BrushSetShaderEffect(brush, shaderEffect);
        OH_Drawing_SetTextStyleForegroundBrush(textStyle, brush);
        OH_Drawing_BrushDestroy(brush);
        OH_Drawing_PointDestroy(startPt);
        OH_Drawing_PointDestroy(endPt);
    }
}
