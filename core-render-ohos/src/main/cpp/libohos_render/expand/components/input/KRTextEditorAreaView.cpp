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

#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>

#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRStringUtil.h"
#include "libohos_render/utils/KRViewUtil.h"

// 编译期 guard：同 KRTextEditorAreaView.h，运行期低 SDK header 下本类类型不存在，
// 为避免低版本上出现未声明类型、API 未声明等问题，本 TU 主体被同一宏整体
// guard。实现同文件同名类型在低版本只会被跳过。
#if KUIKLY_TEXT_EDITOR_AVAILABLE

namespace {
constexpr char kValues[] = "values";
constexpr char kValue[] = "value";
constexpr char kText[] = "text";
constexpr char kFontSize[] = "fontSize";
constexpr char kFontWeight[] = "fontWeight";
constexpr char kColor[] = "color";
constexpr char kLineHeight[] = "lineHeight";
constexpr char kTextAlign[] = "textAlign";

KRAnyValue GetSpanValue(const KRRenderValue::Map &span_map, const char *key) {
    auto it = span_map.find(key);
    if (it != span_map.end()) {
        return it->second;
    }
    return KRRenderValue::Make(nullptr);
}

bool HasSpanValue(const KRRenderValue::Map &span_map, const char *key) {
    return !GetSpanValue(span_map, key)->isNull();
}

std::string GetSpanText(const KRRenderValue::Map &span_map) {
    auto text = GetSpanValue(span_map, kValue)->toString();
    if (text.empty()) {
        text = GetSpanValue(span_map, kText)->toString();
    }
    return text;
}

ArkUI_FontWeight ResolveSpanFontWeight(const KRRenderValue::Map &span_map, ArkUI_FontWeight fallback,
                                       float font_weight_scale) {
    auto value = GetSpanValue(span_map, kFontWeight);
    if (value->isNull()) {
        return fallback;
    }
    return kuikly::util::ConvertArkUIFontWeight(value->toInt(), font_weight_scale);
}

ArkUI_TextAlignment ResolveSpanTextAlign(const KRRenderValue::Map &span_map, ArkUI_TextAlignment fallback) {
    auto value = GetSpanValue(span_map, kTextAlign);
    if (value->isNull()) {
        return fallback;
    }
    const auto align = value->toString();
    if (align == "center") {
        return ARKUI_TEXT_ALIGNMENT_CENTER;
    }
    if (align == "right" || align == "end") {
        return ARKUI_TEXT_ALIGNMENT_END;
    }
    if (align == "left" || align == "start") {
        return ARKUI_TEXT_ALIGNMENT_START;
    }
    return fallback;
}

}  // namespace

void KRTextEditorAreaView::DidInit() {
    KRTextEditorFieldView::DidInit();
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_WILL_CHANGE);
    // 多行模式：SingleLine 已由 IsSingleLine() == false 生效；此处可额外配置默认样式。
}

void KRTextEditorAreaView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    bool values_event = has_values_content_ &&
        (event_type == ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_WILL_CHANGE ||
         event_type == ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_DID_CHANGE ||
         event_type == ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_SELECTION_CHANGE);
    int32_t old_utf16_len = values_event ? kuikly::text_editor::GetUTF16Length(state_.cached_text_) : 0;
    uint32_t selection_before = values_event ? GetSelectionStartPosition() : 0;
    if (values_event && event_type == ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_WILL_CHANGE) {
        if (selection_before != last_values_typing_style_selection_) {
            values_rewrite_next_did_change_ = true;
        }
        SyncTypingStyleForValuesSelection();
    }

    KRTextEditorFieldView::OnEvent(event, event_type);
    if (!values_event || !has_values_content_) {
        return;
    }

    uint32_t selection_start = GetSelectionStartPosition();
    int32_t new_utf16_len = kuikly::text_editor::GetUTF16Length(state_.cached_text_);
    if (event_type == ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_DID_CHANGE) {
        bool should_rewrite = values_rewrite_next_did_change_;
        UpdateValuesSpanRangesAfterTextChange(old_utf16_len, new_utf16_len, selection_start);
        if (should_rewrite) {
            RewriteValuesStyledStringFromCache();
            values_rewrite_next_did_change_ = false;
        }
    } else if (event_type == ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_SELECTION_CHANGE &&
               selection_start != last_values_typing_style_selection_) {
        values_rewrite_next_did_change_ = true;
    }
    SyncTypingStyleForValuesSelection();
}

bool KRTextEditorAreaView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                   const KRRenderCallback event_call_back) {
    if (kuikly::util::isEqual(prop_key, kValues)) {
        return ApplyValues(prop_value);
    }
    if (has_values_content_ && HandleValuesModeStyleProp(prop_key, prop_value)) {
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kText)) {
        has_values_content_ = false;
        values_span_styles_.clear();
        values_rewrite_next_did_change_ = false;
        last_values_typing_style_selection_ = static_cast<uint32_t>(-1);
    }
    // 多行特有：lineHeight（对齐老 KRTextAreaView 支持）。
    // 设计要点：
    //   1. 仅持久化到 state_，由 ApplyTypingStyle / SetStyledText 统一从 state 推导，
    //      避免在 fontSize 等其它属性变更触发 ApplyTypingStyle 时把 lineHeight 误清掉。
    //   2. 立即重写已有文本 span（3.3-α 方案）——因为 typing style 只影响后续键入，
    //      不主动重写 span 的话当前已有文本视觉不会跟随变化。
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kLineHeight)) {
        float new_lh = prop_value->toFloat();
        state_.line_height_ = new_lh;
        state_.line_height_set_ = new_lh > 0;
        if (state_.controller_) {
            // typing style：影响后续键入。
            kuikly::text_editor::ApplyTypingStyle(state_);
            // 已有文本：通过重写 SpanStyle 立即生效（含 LineHeightStyle）。
            kuikly::text_editor::SetStyledText(state_, state_.cached_text_);
        }
        return true;
    }
    return KRTextEditorFieldView::SetProp(prop_key, prop_value, event_call_back);
}

bool KRTextEditorAreaView::ResetControllerForValuesMode() {
    if (!GetNode()) {
        return false;
    }
    if (state_.controller_) {
        OH_ArkUI_TextEditorStyledStringController_Destroy(state_.controller_);
        state_.controller_ = nullptr;
    }
    state_.controller_ = OH_ArkUI_TextEditorStyledStringController_Create();
    if (!state_.controller_) {
        return false;
    }
    ArkUI_AttributeItem item = {};
    item.object = state_.controller_;
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_EDITOR_STYLED_STRING_CONTROLLER, &item);
    return true;
}

bool KRTextEditorAreaView::HandleValuesModeStyleProp(const std::string &prop_key, const KRAnyValue &prop_value) {
    auto node = GetNode();
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kFontSize)) {
        state_.font_size_ = prop_value->toFloat();
        kuikly::text_editor::ApplyPlaceholder(node, state_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kFontWeight)) {
        float scale = 1.0f;
        if (auto root = GetRootView().lock()) {
            scale = root->GetContext()->Config()->GetFontWeightScale();
        }
        state_.font_weight_ = kuikly::util::ConvertArkUIFontWeight(prop_value->toInt(), scale);
        kuikly::text_editor::ApplyPlaceholder(node, state_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kColor)) {
        state_.font_color_ = kuikly::util::ConvertToHexColor(prop_value->toString());
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kTextAlign)) {
        const auto &val = prop_value->toString();
        ArkUI_TextAlignment node_align = ARKUI_TEXT_ALIGNMENT_START;
        if (val == "center") {
            state_.text_align_ = ARKUI_TEXT_ALIGNMENT_CENTER;
            node_align = ARKUI_TEXT_ALIGNMENT_CENTER;
        } else if (val == "right" || val == "end") {
            state_.text_align_ = ARKUI_TEXT_ALIGNMENT_END;
            node_align = ARKUI_TEXT_ALIGNMENT_END;
        } else {
            state_.text_align_ = ARKUI_TEXT_ALIGNMENT_START;
            node_align = ARKUI_TEXT_ALIGNMENT_START;
        }
        ArkUI_NumberValue align_value[] = {{.i32 = static_cast<int32_t>(node_align)}};
        ArkUI_AttributeItem align_item = {align_value, 1};
        kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_ALIGN, &align_item);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kLineHeight)) {
        float new_lh = prop_value->toFloat();
        state_.line_height_ = new_lh;
        state_.line_height_set_ = new_lh > 0;
        return true;
    }
    return false;
}

const KRTextEditorAreaView::ValuesSpanStyle *KRTextEditorAreaView::FindValuesSpanStyleForOffset(
    uint32_t offset) const {
    if (values_span_styles_.empty()) {
        return nullptr;
    }
    for (const auto &span : values_span_styles_) {
        uint32_t start = static_cast<uint32_t>(std::max<int32_t>(span.start, 0));
        uint32_t end = start + static_cast<uint32_t>(std::max<int32_t>(span.length, 0));
        if ((offset == start && span.length > 0) || (offset > start && offset <= end)) {
            return &span;
        }
    }
    if (offset == 0) {
        return &values_span_styles_.front();
    }
    return &values_span_styles_.back();
}

bool KRTextEditorAreaView::SyncTypingStyleForValuesSelection() {
    if (!has_values_content_ || !state_.controller_) {
        return false;
    }
    uint32_t selection_start = GetSelectionStartPosition();
    const auto *span = FindValuesSpanStyleForOffset(selection_start);
    if (!span) {
        return false;
    }
    auto typing_state = state_;
    typing_state.font_size_ = span->font_size;
    typing_state.font_weight_ = span->font_weight;
    typing_state.font_color_ = span->font_color;
    typing_state.text_align_ = span->text_align;
    typing_state.line_height_ = span->line_height;
    typing_state.line_height_set_ = span->line_height_set;
    kuikly::text_editor::ApplyTypingStyle(typing_state);
    last_values_typing_style_selection_ = selection_start;
    return true;
}

bool KRTextEditorAreaView::RewriteValuesStyledStringFromCache() {
    if (!has_values_content_ || !state_.controller_ || values_span_styles_.empty()) {
        return false;
    }

    std::vector<OH_ArkUI_TextStyle *> temp_text_styles;
    std::vector<OH_ArkUI_SpanStyle *> temp_span_styles;
    std::vector<OH_ArkUI_ParagraphStyle *> temp_para_styles;
    std::vector<OH_ArkUI_LineHeightStyle *> temp_line_height_styles;
    std::vector<const OH_ArkUI_SpanStyle *> span_style_refs;

    auto cached_utf16_len = static_cast<int32_t>(kuikly::text_editor::GetUTF16Length(state_.cached_text_));
    for (const auto &span : values_span_styles_) {
        int32_t span_start = std::max<int32_t>(0, std::min<int32_t>(span.start, cached_utf16_len));
        int32_t span_length = std::max<int32_t>(0, std::min<int32_t>(span.length, cached_utf16_len - span_start));
        if (span_length <= 0) {
            continue;
        }
        auto set_span_range = [&](OH_ArkUI_SpanStyle *span_style) {
            OH_ArkUI_SpanStyle_SetStart(span_style, span_start);
            OH_ArkUI_SpanStyle_SetLength(span_style, span_length);
        };

        auto *text_style = OH_ArkUI_TextStyle_Create();
        auto *text_span_style = OH_ArkUI_SpanStyle_Create();
        if (text_style && text_span_style) {
            OH_ArkUI_TextStyle_SetFontColor(text_style, span.font_color);
            OH_ArkUI_TextStyle_SetFontSize(text_style, span.font_size);
            OH_ArkUI_TextStyle_SetFontWeight(text_style, static_cast<uint32_t>(span.font_weight));
            set_span_range(text_span_style);
            OH_ArkUI_SpanStyle_SetTextStyle(text_span_style, text_style);
            span_style_refs.push_back(text_span_style);
        }
        if (text_style) {
            temp_text_styles.push_back(text_style);
        }
        if (text_span_style) {
            temp_span_styles.push_back(text_span_style);
        }

        auto span_state = state_;
        span_state.line_height_ = span.line_height;
        span_state.line_height_set_ = span.line_height_set;
        float line_height = kuikly::text_editor::ResolveLineHeightVp(span_state);
        if (line_height > 0) {
            auto *line_height_style = OH_ArkUI_LineHeightStyle_Create();
            auto *line_height_span_style = OH_ArkUI_SpanStyle_Create();
            if (line_height_style && line_height_span_style) {
                OH_ArkUI_LineHeightStyle_SetLineHeight(line_height_style, line_height);
                set_span_range(line_height_span_style);
                OH_ArkUI_SpanStyle_SetLineHeightStyle(line_height_span_style, line_height_style);
                span_style_refs.push_back(line_height_span_style);
            }
            if (line_height_style) {
                temp_line_height_styles.push_back(line_height_style);
            }
            if (line_height_span_style) {
                temp_span_styles.push_back(line_height_span_style);
            }
        }

        auto *paragraph_style = OH_ArkUI_ParagraphStyle_Create();
        auto *paragraph_span_style = OH_ArkUI_SpanStyle_Create();
        if (paragraph_style && paragraph_span_style) {
            OH_ArkUI_ParagraphStyle_SetTextAlign(paragraph_style, span.text_align);
            OH_ArkUI_ParagraphStyle_SetTextVerticalAlign(
                paragraph_style, ArkUI_TextVerticalAlignment::ARKUI_TEXT_VERTICAL_ALIGNMENT_CENTER);
            set_span_range(paragraph_span_style);
            OH_ArkUI_SpanStyle_SetParagraphStyle(paragraph_span_style, paragraph_style);
            span_style_refs.push_back(paragraph_span_style);
        }
        if (paragraph_style) {
            temp_para_styles.push_back(paragraph_style);
        }
        if (paragraph_span_style) {
            temp_span_styles.push_back(paragraph_span_style);
        }
    }

    bool rewrite_ret = false;
    uint32_t selection = GetSelectionStartPosition();
    if (!span_style_refs.empty()) {
        auto *desc = OH_ArkUI_StyledString_Descriptor_CreateWithString(
            state_.cached_text_.c_str(), span_style_refs.data(), static_cast<int32_t>(span_style_refs.size()));
        if (desc) {
            auto set_ret = OH_ArkUI_TextEditorStyledStringController_SetStyledString(state_.controller_, desc);
            OH_ArkUI_StyledString_Descriptor_Destroy(desc);
            SetSelectionStartPosition(std::min<uint32_t>(selection, kuikly::text_editor::GetUTF16Length(state_.cached_text_)));
            rewrite_ret = set_ret == ARKUI_ERROR_CODE_NO_ERROR;
        }
    }

    for (auto *style : temp_text_styles) {
        OH_ArkUI_TextStyle_Destroy(style);
    }
    for (auto *style : temp_span_styles) {
        OH_ArkUI_SpanStyle_Destroy(style);
    }
    for (auto *style : temp_para_styles) {
        OH_ArkUI_ParagraphStyle_Destroy(style);
    }
    for (auto *style : temp_line_height_styles) {
        OH_ArkUI_LineHeightStyle_Destroy(style);
    }
    return rewrite_ret;
}

void KRTextEditorAreaView::UpdateValuesSpanRangesAfterTextChange(int32_t old_length, int32_t new_length,
                                                                 uint32_t selection_after) {
    int32_t delta = new_length - old_length;
    if (delta == 0 || values_span_styles_.empty()) {
        return;
    }

    if (delta > 0) {
        int32_t insert_start = std::max<int32_t>(0, static_cast<int32_t>(selection_after) - delta);
        auto *span = const_cast<ValuesSpanStyle *>(FindValuesSpanStyleForOffset(static_cast<uint32_t>(insert_start)));
        if (span) {
            span->length += delta;
        }
        for (auto &item : values_span_styles_) {
            if (&item != span && item.start >= insert_start) {
                item.start += delta;
            }
        }
    } else {
        int32_t delete_start = std::max<int32_t>(0, static_cast<int32_t>(selection_after));
        int32_t delete_end = delete_start - delta;
        for (auto &span : values_span_styles_) {
            int32_t span_start = span.start;
            int32_t span_end = span.start + span.length;
            if (span_end <= delete_start) {
                continue;
            }
            if (span_start >= delete_end) {
                span.start += delta;
                continue;
            }
            int32_t overlap_start = std::max(span_start, delete_start);
            int32_t overlap_end = std::min(span_end, delete_end);
            span.length = std::max<int32_t>(0, span.length - std::max<int32_t>(0, overlap_end - overlap_start));
            if (span_start >= delete_start) {
                span.start = delete_start;
            }
        }
        values_span_styles_.erase(
            std::remove_if(values_span_styles_.begin(), values_span_styles_.end(),
                           [](const ValuesSpanStyle &span) { return span.length <= 0; }),
            values_span_styles_.end());
    }
}

bool KRTextEditorAreaView::ApplyValues(const KRAnyValue &prop_value) {
    if (!state_.controller_) {
        return true;
    }

    auto spans = prop_value->toArray();
    if (spans.empty()) {
        has_values_content_ = false;
        values_span_styles_.clear();
        values_rewrite_next_did_change_ = false;
        last_values_typing_style_selection_ = static_cast<uint32_t>(-1);
        SetContentText("");
        return true;
    }
    has_values_content_ = false;
    values_span_styles_.clear();
    values_rewrite_next_did_change_ = false;
    last_values_typing_style_selection_ = static_cast<uint32_t>(-1);

    uint32_t selection_start = GetSelectionStartPosition();
    if (!ResetControllerForValuesMode()) {
        return true;
    }
    std::vector<OH_ArkUI_TextStyle *> temp_text_styles;
    std::vector<OH_ArkUI_SpanStyle *> temp_span_styles;
    std::vector<OH_ArkUI_ParagraphStyle *> temp_para_styles;
    std::vector<OH_ArkUI_LineHeightStyle *> temp_line_height_styles;
    std::vector<const OH_ArkUI_SpanStyle *> span_style_refs;

    float font_weight_scale = 1.0f;
    if (auto root = GetRootView().lock()) {
        font_weight_scale = root->GetContext()->Config()->GetFontWeightScale();
    }

    std::string plain_text;
    int32_t utf16_cursor = 0;
    for (const auto &span_value : spans) {
        auto span_map = span_value->toMap();
        auto span_text = GetSpanText(span_map);
        auto span_utf16_len = static_cast<int32_t>(kuikly::text_editor::GetUTF16Length(span_text));
        plain_text.append(span_text);

        auto span_state = state_;
        if (HasSpanValue(span_map, kFontSize)) {
            span_state.font_size_ = GetSpanValue(span_map, kFontSize)->toFloat();
        }
        if (HasSpanValue(span_map, kColor)) {
            span_state.font_color_ = kuikly::util::ConvertToHexColor(GetSpanValue(span_map, kColor)->toString());
        }
        span_state.font_weight_ = ResolveSpanFontWeight(span_map, state_.font_weight_, font_weight_scale);
        span_state.text_align_ = ResolveSpanTextAlign(span_map, state_.text_align_);
        if (HasSpanValue(span_map, kLineHeight)) {
            float line_height = GetSpanValue(span_map, kLineHeight)->toFloat();
            span_state.line_height_ = line_height;
            span_state.line_height_set_ = line_height > 0;
        }
        values_span_styles_.push_back({utf16_cursor,
                                       span_utf16_len,
                                       span_state.font_size_,
                                       span_state.font_weight_,
                                       span_state.font_color_,
                                       span_state.text_align_,
                                       span_state.line_height_,
                                       span_state.line_height_set_});

        auto set_span_range = [&](OH_ArkUI_SpanStyle *span_style) {
            OH_ArkUI_SpanStyle_SetStart(span_style, utf16_cursor);
            OH_ArkUI_SpanStyle_SetLength(span_style, span_utf16_len);
        };

        auto *text_style = OH_ArkUI_TextStyle_Create();
        auto *text_span_style = OH_ArkUI_SpanStyle_Create();
        if (text_style && text_span_style) {
            OH_ArkUI_TextStyle_SetFontColor(text_style, span_state.font_color_);
            OH_ArkUI_TextStyle_SetFontSize(text_style, span_state.font_size_);
            OH_ArkUI_TextStyle_SetFontWeight(text_style, static_cast<uint32_t>(span_state.font_weight_));
            set_span_range(text_span_style);
            OH_ArkUI_SpanStyle_SetTextStyle(text_span_style, text_style);
            span_style_refs.push_back(text_span_style);
        }
        if (text_style) {
            temp_text_styles.push_back(text_style);
        }
        if (text_span_style) {
            temp_span_styles.push_back(text_span_style);
        }

        float line_height = kuikly::text_editor::ResolveLineHeightVp(span_state);
        if (line_height > 0) {
            auto *line_height_style = OH_ArkUI_LineHeightStyle_Create();
            auto *line_height_span_style = OH_ArkUI_SpanStyle_Create();
            if (line_height_style && line_height_span_style) {
                OH_ArkUI_LineHeightStyle_SetLineHeight(line_height_style, line_height);
                set_span_range(line_height_span_style);
                OH_ArkUI_SpanStyle_SetLineHeightStyle(line_height_span_style, line_height_style);
                span_style_refs.push_back(line_height_span_style);
            }
            if (line_height_style) {
                temp_line_height_styles.push_back(line_height_style);
            }
            if (line_height_span_style) {
                temp_span_styles.push_back(line_height_span_style);
            }
        }

        auto *paragraph_style = OH_ArkUI_ParagraphStyle_Create();
        auto *paragraph_span_style = OH_ArkUI_SpanStyle_Create();
        if (paragraph_style && paragraph_span_style) {
            OH_ArkUI_ParagraphStyle_SetTextAlign(paragraph_style, span_state.text_align_);
            OH_ArkUI_ParagraphStyle_SetTextVerticalAlign(
                paragraph_style, ArkUI_TextVerticalAlignment::ARKUI_TEXT_VERTICAL_ALIGNMENT_CENTER);
            set_span_range(paragraph_span_style);
            OH_ArkUI_SpanStyle_SetParagraphStyle(paragraph_span_style, paragraph_style);
            span_style_refs.push_back(paragraph_span_style);
        }
        if (paragraph_style) {
            temp_para_styles.push_back(paragraph_style);
        }
        if (paragraph_span_style) {
            temp_span_styles.push_back(paragraph_span_style);
        }
        utf16_cursor += span_utf16_len;
    }

    if (plain_text.empty()) {
        SetContentText("");
    } else if (!span_style_refs.empty()) {
        kuikly::text_editor::ApplyTypingStyle(state_);
        auto *desc = OH_ArkUI_StyledString_Descriptor_CreateWithString(
            plain_text.c_str(), span_style_refs.data(), static_cast<int32_t>(span_style_refs.size()));
        if (desc) {
            state_.image_spans_.clear();
            OH_ArkUI_TextEditorStyledStringController_SetStyledString(state_.controller_, desc);
            OH_ArkUI_StyledString_Descriptor_Destroy(desc);
            state_.cached_text_ = plain_text;
            has_values_content_ = true;
            auto final_selection = std::min<uint32_t>(selection_start, kuikly::text_editor::GetUTF16Length(plain_text));
            SetSelectionStartPosition(final_selection);
            SyncTypingStyleForValuesSelection();
            OnTextDidChanged(nullptr);
        }
    }

    for (auto *style : temp_text_styles) {
        OH_ArkUI_TextStyle_Destroy(style);
    }
    for (auto *style : temp_span_styles) {
        OH_ArkUI_SpanStyle_Destroy(style);
    }
    for (auto *style : temp_para_styles) {
        OH_ArkUI_ParagraphStyle_Destroy(style);
    }
    for (auto *style : temp_line_height_styles) {
        OH_ArkUI_LineHeightStyle_Destroy(style);
    }
    return true;
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

#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE