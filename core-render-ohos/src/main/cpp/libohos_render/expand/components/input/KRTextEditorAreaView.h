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

#ifndef CORE_RENDER_OHOS_KRTEXTEDITORAREAVIEW_H
#define CORE_RENDER_OHOS_KRTEXTEDITORAREAVIEW_H

#include <vector>

#include "libohos_render/expand/components/input/KRTextEditorFieldView.h"

// 编译期 guard：同 KRTextEditorFieldView.h，本类继承自 KRTextEditorFieldView，
// 这个基类在低 SDK header 下不存在，因此本类也必须被同一宏 guard 掉。
#if KUIKLY_TEXT_EDITOR_AVAILABLE

/**
 * 基于 ARKUI_NODE_TEXT_EDITOR 的多行输入实现。
 * 与 KRTextEditorFieldView 共享绝大多数代码，仅差异：
 *   * 不强制 SingleLine
 *   * 不拦截换行符
 *   * keyboardType == "password" 打 warn 并降级（对齐老 KRTextAreaView 语义）
 *   * 多一个 lineHeight 属性（与老 KRTextAreaView 一致）
 */
class KRTextEditorAreaView : public KRTextEditorFieldView {
 public:
    void DidInit() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;

 protected:
    bool IsSingleLine() const override {
        return false;
    }
    bool InterceptNewline() const override {
        return false;
    }
    void ApplyKeyboardType(const std::string &type) override;

 private:
    struct ValuesSpanStyle {
        int32_t start = 0;
        int32_t length = 0;
        float font_size = 15.0f;
        ArkUI_FontWeight font_weight = ARKUI_FONT_WEIGHT_NORMAL;
        uint32_t font_color = 0xFF000000;
        ArkUI_TextAlignment text_align = ARKUI_TEXT_ALIGNMENT_START;
        float line_height = 0.0f;
        bool line_height_set = false;
    };

    bool HandleValuesModeStyleProp(const std::string &prop_key, const KRAnyValue &prop_value);
    bool ResetControllerForValuesMode();
    bool ApplyValues(const KRAnyValue &prop_value);
    bool SyncTypingStyleForValuesSelection();
    bool RewriteValuesStyledStringFromCache();
    void UpdateValuesSpanRangesAfterTextChange(int32_t old_length, int32_t new_length, uint32_t selection_after);
    const ValuesSpanStyle *FindValuesSpanStyleForOffset(uint32_t offset) const;
    bool has_values_content_ = false;
    std::vector<ValuesSpanStyle> values_span_styles_;
    uint32_t last_values_typing_style_selection_ = static_cast<uint32_t>(-1);
    bool values_rewrite_next_did_change_ = false;
};

#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE

#endif  // CORE_RENDER_OHOS_KRTEXTEDITORAREAVIEW_H