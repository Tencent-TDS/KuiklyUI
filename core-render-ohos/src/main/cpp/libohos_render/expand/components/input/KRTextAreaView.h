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

#ifndef CORE_RENDER_OHOS_KRTEXTAREAVIEW_H
#define CORE_RENDER_OHOS_KRTEXTAREAVIEW_H

#include "libohos_render/expand/components/input/KRTextFieldView.h"

class KRTextAreaView : public KRTextFieldView {
 public:
    ArkUI_NodeHandle CreateNode() override {
        return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT_AREA);
    }

    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;

 private:
    ArkUI_NodeEventType GetOnSubmitEventType() override {
        return ArkUI_NodeEventType::NODE_TEXT_AREA_ON_SUBMIT;
    }
    ArkUI_NodeEventType GetOnChangeEventType() override {
        return ArkUI_NodeEventType::NODE_TEXT_AREA_ON_CHANGE;
    }

    void UpdateInputNodePlaceholder(const std::string &propValue) override;
    void UpdateInputNodePlaceholderColor(const std::string &propValue) override;
    void UpdateInputNodeColor(const std::string &propValue) override;
    void UpdateInputNodeCaretrColor(const std::string &propValue) override;
    void UpdateInputNodeKeyboardType(const std::string &propValue) override;
    void UpdateInputNodeEnterKeyType(const std::string &propValue) override;
    void UpdateInputNodeMaxLength(int maxLength) override;
    uint32_t GetInputNodeSelectionStartPosition() override;
    void UpdateInputNodeSelectionStartPosition(uint32_t index) override;
    void UpdateInputNodePlaceholderFont(uint32_t font_size, ArkUI_FontWeight font_weight) override;
    void UpdateInputNodeContentText(const std::string &text) override;

};

#endif  // CORE_RENDER_OHOS_KRTEXTAREAVIEW_H
