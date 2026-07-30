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

#ifndef CORE_RENDER_HARMONY_KRPARAGRAPH_H
#define CORE_RENDER_HARMONY_KRPARAGRAPH_H
#include <arkui/styled_string.h>
#include <tuple>

#include "libohos_render/foundation/type/KRRenderValue.h"
#include "libohos_render/utils/KRLinearGradientParser.h"


class KRParagraph final {
  public:
    KRParagraph(KRRenderValue::Array spans, KRRenderValue::Map props, float font_size_scale, float font_weight_scale,
                float density, NativeResourceManager *resource_manager,
                std::shared_ptr<kuikly::util::KRLinearGradientParser> gradient);
    ~KRParagraph();

    std::pair<float, float> Measure(float max_width_pt);
    ArkUI_StyledString *GetStyledString();
    int SpanIndexAt(float spanX, float spanY);
    std::tuple<float, float, float, float> SpanRect(int spanIndex);

  private:
    OH_Drawing_ShaderEffect *CreateShaderEffect(std::shared_ptr<kuikly::util::KRLinearGradientParser> linearGradient);
    std::tuple<float, float, OH_Drawing_Typography *> Measure(ArkUI_StyledString *, float max_width_pt,
                                                              float max_height_pt);
    OH_Drawing_TypographyStyle *CreateTypographyStyle();
    void AddSpanToStyledString(const KRRenderValue::Map &spanMap, ArkUI_StyledString *styled_string);
    ArkUI_StyledString *BuildStyledString(float width, float height);

  private:
    OH_Drawing_TextDirection text_direction_ = TEXT_DIRECTION_LTR;
    KRRenderValue::Array spans_;
    KRRenderValue::Map props_;
    float font_size_scale_;
    float font_weight_scale_;
    float density_;
    int charOffset = 0;
    int placeholder_count = 0;
    int spanIndex = 0;
    std::unordered_map<int, int> placeholder_index_map_;
    std::vector<std::tuple<int, int, int>> span_offsets_; // span, begin, end

    ArkUI_StyledString *styled_string_ = nullptr;
    OH_Drawing_Typography *typography_ = nullptr;
    std::shared_ptr<kuikly::util::KRLinearGradientParser> linear_gradient_ = nullptr;
    float measured_width_ = 0;
    float measured_height_ = 0;

    NativeResourceManager *resource_manager_ = nullptr;
};

#endif // CORE_RENDER_HARMONY_KRPARAGRAPH_H
