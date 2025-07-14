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

#ifndef CORE_RENDER_OHOS_KRTEXTFIELDVIEW_H
#define CORE_RENDER_OHOS_KRTEXTFIELDVIEW_H

#include "libohos_render/export/IKRRenderViewExport.h"

class KRTextFieldView : public IKRRenderViewExport {
 public:
    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    void OnDestroy() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    /**
     * Kotlin侧对该View的方法进行调用时回调用
     * @param method
     * @param params
     * @param callback 可空
     */
    void CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &callback) override;

    //     void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
 protected:
    virtual ArkUI_NodeEventType GetOnSubmitEventType() {
        return ArkUI_NodeEventType::NODE_TEXT_INPUT_ON_SUBMIT;
    }
    virtual ArkUI_NodeEventType GetOnChangeEventType() {
        return ArkUI_NodeEventType::NODE_TEXT_INPUT_ON_CHANGE;
    }
    
    virtual void UpdateInputNodePlaceholder(const std::string &propValue);
    virtual void UpdateInputNodePlaceholderColor(const std::string &propValue);
    virtual void UpdateInputNodeColor(const std::string &propValue);
    virtual void UpdateInputNodeCaretrColor(const std::string &propValue);
    virtual void UpdateInputNodeTextAlign(const std::string &propValue);
    virtual void UpdateInputNodeFocusable(int propValue);
    virtual void UpdateInputNodeKeyboardType(const std::string &propValue);
    virtual void UpdateInputNodeEnterKeyType(const std::string &propValue);
    virtual void UpdateInputNodeMaxLength(int maxLength);
    virtual void UpdateInputNodeFocusStatus(int status);
    virtual uint32_t GetInputNodeSelectionStartPosition();
    virtual void UpdateInputNodeSelectionStartPosition(uint32_t index);
    virtual void UpdateInputNodePlaceholderFont(uint32_t font_size, ArkUI_FontWeight font_weight);
    virtual void UpdateInputNodeContentText(const std::string &text);
    virtual std::string GetInputNodeContentText();

 private:
    float font_size_ = 15;  // default 15
    ArkUI_FontWeight font_weight_ = ARKUI_FONT_WEIGHT_NORMAL;
    bool focusable_ = true;
    uint32_t max_length_ = -1;
    KRRenderCallback text_did_change_callback_;           // 文本变化callback
    KRRenderCallback input_focus_callback_;               // 输入框获焦
    KRRenderCallback input_blur_callback_;                // 输入框失焦
    KRRenderCallback input_return_callback_;              // 完成键按下回调
    KRRenderCallback text_length_beyond_limit_callback_;  // 输入超过MaxLength限制
    KRRenderCallback keyboard_height_changed_callback_;   // 键盘高度变化

    /**
     * 输入框获焦（弹起键盘）
     */
    void Focus();

    /**
     * 输入框失焦（收起键盘）
     */
    void Blur();

    /**
     * 获取光标位置
     */
    void GetCursorIndex(const KRRenderCallback &callback);

    /**
     * 设置光标位置
     */
    void SetCursorIndex(uint32_t index);

    /**
     * 设置字体（包括占位字体）
     * @param font_size
     * @param font_weight
     */
    void SetFont(uint32_t font_size, ArkUI_FontWeight font_weight);

    /**
     * 文本变化回调
     */
    void OnTextDidChanged(ArkUI_NodeEvent *event);
    /**
     * 获焦回调
     */
    void OnInputFocus(ArkUI_NodeEvent *event);
    /**
     * 失焦回调
     */
    void OnInputBlur(ArkUI_NodeEvent *event);
    /**
     * 按下完成键回调
     */
    void OnInputReturn(ArkUI_NodeEvent *event);
    /***
     * 获取输入的文本内容
     */
    std::string GetContentText();
    /***
     * 获取输入的文本内容
     */
    void SetContentText(const std::string &text);
    /**
     * 限制输入文本到最大长度
     */
    bool LimitInputContentTextInMaxLength();
};

#endif  // CORE_RENDER_OHOS_KRTEXTFIELDVIEW_H
