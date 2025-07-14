/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#include "libohos_render/expand/components/input/KRTextareaView.h"
#include "libohos_render/manager/KRKeyboardManager.h"
#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRViewUtil.h"
#include "libohos_render/expand/components/input/KRTextAreaView.h"

constexpr char kLineHeight[] = "lineHeight";

bool KRTextAreaView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                             const KRRenderCallback event_call_back) {
    if (kuikly::util::isEqual(prop_key, kLineHeight)) {
        kuikly::util::UpdateTextAreaNodeLineHeight(GetNode(), prop_value->toFloat());
        return true;
    }
    return KRTextFieldView::SetProp(prop_key, prop_value, event_call_back);
}


void KRTextAreaView::UpdateInputNodePlaceholder(const std::string &propValue) {
    ArkUI_AttributeItem item = {.string = propValue.c_str()};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_AREA_PLACEHOLDER, &item);
}
void KRTextAreaView::UpdateInputNodePlaceholderColor(const std::string &propValue) {
    ArkUI_NumberValue value = {.u32 = kuikly::util::ConvertToHexColor(propValue)};
    ArkUI_AttributeItem item = {&value, sizeof(ArkUI_NumberValue)};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_AREA_PLACEHOLDER_COLOR, &item);
}
void KRTextAreaView::UpdateInputNodeColor(const std::string &propValue) {
    ArkUI_NumberValue preparedColorValue[] = {{.u32 = kuikly::util::ConvertToHexColor(propValue)}};
    ArkUI_AttributeItem colorItem = {preparedColorValue, sizeof(preparedColorValue) / sizeof(ArkUI_NumberValue)};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_FONT_COLOR, &colorItem);
}
void KRTextAreaView::UpdateInputNodeCaretrColor(const std::string &propValue) {
    ArkUI_NumberValue value = {.u32 = kuikly::util::ConvertToHexColor(propValue)};
    ArkUI_AttributeItem item = {&value, sizeof(ArkUI_NumberValue)};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_AREA_CARET_COLOR, &item);
}

void KRTextAreaView::UpdateInputNodeKeyboardType(const std::string &propValue) {
    ArkUI_TextAreaType type = ARKUI_TEXTAREA_TYPE_NORMAL;
    if (propValue == "number") {
        type = ARKUI_TEXTAREA_TYPE_NUMBER;
    }else if (propValue == "email") {
        type = ARKUI_TEXTAREA_TYPE_EMAIL;
    }

    ArkUI_NumberValue value[] = {{.i32 = type}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_AREA_TYPE, &item);
}
void KRTextAreaView::UpdateInputNodeEnterKeyType(const std::string &propValue) {
    ArkUI_NumberValue value[] = {{.i32 = kuikly::util::ConvertToEnterKeyType(propValue)}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_AREA_ENTER_KEY_TYPE, &item);
}
void KRTextAreaView::UpdateInputNodeMaxLength(int maxLength) {
    ArkUI_NumberValue value[] = {{.i32 = maxLength}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_INPUT_MAX_LENGTH, &item);
}

uint32_t KRTextAreaView::GetInputNodeSelectionStartPosition() {
    auto item = kuikly::util::GetNodeApi()->getAttribute(GetNode(), NODE_TEXT_AREA_TEXT_SELECTION);
    return item ? item->value[0].i32 : 0;
}
void KRTextAreaView::UpdateInputNodeSelectionStartPosition(uint32_t index) {
    std::array<ArkUI_NumberValue, 2> value = {{{.i32 = static_cast<int32_t>(index)}, {.i32 = static_cast<int32_t>(index)}}};
    ArkUI_AttributeItem item = {value.data(), value.size()};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_AREA_TEXT_SELECTION, &item);
}
void KRTextAreaView::UpdateInputNodePlaceholderFont(uint32_t font_size, ArkUI_FontWeight font_weight) {
    ArkUI_NumberValue fontWeight = {.i32 = font_weight};
    ArkUI_NumberValue tempStyle = {.i32 = ARKUI_FONT_STYLE_NORMAL};
    std::array<ArkUI_NumberValue, 3> value = {{{.f32 = static_cast<float>(font_size)}, tempStyle, fontWeight}};
    ArkUI_AttributeItem item = {value.data(), value.size()};
    auto node = GetNode();
    kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_AREA_PLACEHOLDER_FONT, &item);

    {
        ArkUI_NumberValue valueSize[] = {{.f32 = static_cast<float>(font_size)}};
        ArkUI_AttributeItem itemSize = {valueSize, sizeof(valueSize) / sizeof(ArkUI_NumberValue)};
        kuikly::util::GetNodeApi()->setAttribute(node, NODE_FONT_SIZE, &itemSize);
    }
    {
        ArkUI_NumberValue fontWeight = {.i32 = font_weight};
        ArkUI_NumberValue valueWeight[] = {fontWeight};
        ArkUI_AttributeItem itemWeight = {valueWeight, 1};
        kuikly::util::GetNodeApi()->setAttribute(node, NODE_FONT_WEIGHT, &itemWeight);
    }
}
void KRTextAreaView::UpdateInputNodeContentText(const std::string &text) {
    ArkUI_AttributeItem item = {.string = text.c_str()};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_AREA_TEXT, &item);
}

