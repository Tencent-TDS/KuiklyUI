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

#ifndef CORE_RENDER_OHOS_KRGRADIENTRICHTEXTSHADOW_H
#define CORE_RENDER_OHOS_KRGRADIENTRICHTEXTSHADOW_H
#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"
#include "libohos_render/utils/KRLinearGradientParser.h"
class KRGradientRichTextShadow : public KRRichTextShadow {
    /**
     * 更新 shadow 对象属性时调用
     * @param prop_key 属性名
     * @param prop_value 属性数据
     */
    void SetProp(const std::string &prop_key, const KRAnyValue &prop_value) override;

    /**
     * 根据布局约束尺寸计算返回 RenderView 的实际尺寸
     * @param constraint_width
     * @param constraint_height
     * @return
     */
    KRSize CalculateRenderViewSize(double constraint_width, double constraint_height) override;
    /**
     * 完成对某个Span对应TextStyle
     * @param textStyle
     * @param dpi 屏幕密度 即ppi和dp换算比例，如3.0
     */
    void DidBuildTextStyle(OH_Drawing_TextStyle *textStyle, double dpi) override;

 private:
    std::shared_ptr<kuikly::util::KRLinearGradientParser> text_linearGradient_;
    double calculate_width_ = 0.0;
    double calculate_height_ = 0.0;
};

#endif  // CORE_RENDER_OHOS_KRGRADIENTRICHTEXTSHADOW_H
