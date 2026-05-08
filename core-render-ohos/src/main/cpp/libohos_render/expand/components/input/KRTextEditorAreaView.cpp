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

#include "libohos_render/expand/components/input/KRTextEditorAreaView.h"

#include "libohos_render/utils/KRStringUtil.h"
#include "libohos_render/utils/KRViewUtil.h"

void KRTextEditorAreaView::DidInit() {
    KRTextEditorFieldView::DidInit();
    // 多行模式：SingleLine 已由 IsSingleLine() == false 生效；此处可额外配置默认样式。
}

bool KRTextEditorAreaView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                   const KRRenderCallback event_call_back) {
    // 多行特有：lineHeight（对齐老 KRTextAreaView 支持）。
    // ARKUI_NODE_TEXT_EDITOR 的 line height 通过 TextStyle 设置，这里做透传。
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kLineHeight)) {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
        // 将 lineHeight 写入当前 typing style（仅影响后续键入），
        // 与老 NODE_TEXT_AREA_LINE_HEIGHT 的视觉行为接近。
        if (state_.controller_) {
            OH_ArkUI_TextEditorTextStyle *style = OH_ArkUI_TextEditorTextStyle_Create();
            if (style) {
                OH_ArkUI_TextEditorTextStyle_SetFontColor(style, state_.font_color_);
                OH_ArkUI_TextEditorTextStyle_SetFontSize(style, state_.font_size_);
                OH_ArkUI_TextEditorTextStyle_SetFontWeight(
                    style, static_cast<uint32_t>(state_.font_weight_));
                OH_ArkUI_TextEditorTextStyle_SetLineHeight(
                    style, static_cast<int32_t>(prop_value->toFloat()));
                OH_ArkUI_TextEditorStyledStringController_SetTypingStyle(state_.controller_, style);
                OH_ArkUI_TextEditorTextStyle_Destroy(style);
            }
        }
#endif
        return true;
    }
    return KRTextEditorFieldView::SetProp(prop_key, prop_value, event_call_back);
}

void KRTextEditorAreaView::ApplyKeyboardType(const std::string &type) {
    // 老 KRTextAreaView 仅支持 default/number/email，不支持 password。
    // TEXT_EDITOR 不暴露 keyboardType；这里同样打 warn 并降级为默认。
    //
    // 注意：曾尝试直接复用老控件的 UpdateInputNodeKeyboardType
    // （底层写 NODE_TEXT_INPUT_TYPE）在 TEXT_EDITOR 节点上 setAttribute，
    // 实测会 crash，不可跨节点作用域复用属性。
    if (type == "password") {
        KR_LOG_DEBUG << "KRTextEditorAreaView: multi-line does not support password keyboard";
        return;
    }
    if (type != "default" && !type.empty()) {
        KR_LOG_DEBUG << "KRTextEditorAreaView: keyboardType=" << type
                     << " is not supported on ARKUI_NODE_TEXT_EDITOR (API 24+), fallback to default";
    }
}
