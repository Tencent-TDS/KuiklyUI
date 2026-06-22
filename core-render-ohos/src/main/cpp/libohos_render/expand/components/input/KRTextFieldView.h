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

#ifndef CORE_RENDER_OHOS_KRTEXTFIELDVIEW_H
#define CORE_RENDER_OHOS_KRTEXTFIELDVIEW_H

#include "libohos_render/export/IKRRenderViewExport.h"
#include <cstddef>
#include <cstdint>
#include <utility>

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
    /**
     * 输入内容过滤
     * @param source 输入的内容字符串（UTF-8编码），当返回true时，source将更新为截断后的内容
     * @param dest 当前已有的内容字符串（UTF-8编码）
     * @param dStart 已有内容被替换的起始位置（按UTF-16算）
     * @param dEnd 已有内容被替换的结束位置（按UTF-16算）
     * @return 是否截断输入
     */
    bool filter(char source[], const std::string &dest, const size_t dStart, const size_t dEnd);
    virtual ArkUI_NodeEventType GetOnSubmitEventType() {
        return ArkUI_NodeEventType::NODE_TEXT_INPUT_ON_SUBMIT;
    }
    virtual ArkUI_NodeEventType GetOnChangeEventType() {
        return ArkUI_NodeEventType::NODE_TEXT_INPUT_ON_CHANGE;
    }
    virtual ArkUI_NodeEventType GetOnWillInsertEventType() {
        return ArkUI_NodeEventType::NODE_TEXT_INPUT_ON_WILL_INSERT;
    }
    virtual ArkUI_NodeEventType GetOnPasteEventType() {
        return ArkUI_NodeEventType::NODE_TEXT_INPUT_ON_PASTE;
    }
    virtual ArkUI_NodeEventType GetOnWillChangeEventType() {
        return ArkUI_NodeEventType::NODE_TEXT_INPUT_ON_WILL_CHANGE;
    }
    /**
     * 选区变化事件类型。子类（如 KRTextAreaView）可 override 以适配不同的 ArkUI 节点类型。
     */
    virtual ArkUI_NodeEventType GetOnTextSelectionChangeEventType() {
        return ArkUI_NodeEventType::NODE_TEXT_INPUT_ON_TEXT_SELECTION_CHANGE;
    }
    
    virtual void UpdateInputNodePlaceholder(const std::string &propValue);
    virtual void UpdateInputNodePlaceholderColor(const std::string &propValue);
    virtual void UpdateInputNodeColor(const std::string &propValue);
    virtual void UpdateInputNodeCaretrColor(const std::string &propValue);
    virtual void UpdateInputNodeSelectionColor(const std::string &propValue);
    virtual void UpdateInputNodeTextAlign(const std::string &propValue);
    virtual void UpdateInputNodeFocusable(int propValue);
    virtual void UpdateInputNodeKeyboardType(const std::string &propValue);
    virtual void UpdateInputNodeEnterKeyType(const std::string &propValue);
    virtual void UpdateInputNodeMaxLength(int maxLength);
    virtual void UpdateInputNodeFocusStatus(int status);
    virtual uint32_t GetInputNodeSelectionStartPosition();
    virtual void UpdateInputNodeSelectionStartPosition(uint32_t index);
    /**
     * 获取选区范围 [start, end]（按 UTF-16 算）。
     * 子类（如 KRTextAreaView）可 override 以适配不同的 ArkUI 节点类型。
     */
    virtual std::pair<uint32_t, uint32_t> GetInputNodeTextSelectionRange();
    virtual void UpdateInputNodePlaceholderFont(uint32_t font_size, ArkUI_FontWeight font_weight);
    virtual void UpdateInputNodeContentText(const std::string &text);
    virtual std::string GetInputNodeContentText();

 private:
    float font_size_ = 15;  // default 15
    ArkUI_FontWeight font_weight_ = ARKUI_FONT_WEIGHT_NORMAL;
    bool focusable_ = true;
    int32_t max_length_ = -1;
    int length_limit_type_ = -1;  // -1: unset, 0: BYTE, 1: CHARACTER, 2: VISUAL_WIDTH
    bool length_input_filter_ = false;
    bool drag_entered_ = false;
    KRRenderCallback text_did_change_callback_;           // 文本变化callback
    KRRenderCallback input_focus_callback_;               // 输入框获焦
    KRRenderCallback input_blur_callback_;                // 输入框失焦
    KRRenderCallback input_return_callback_;              // 完成键按下回调
    KRRenderCallback text_length_beyond_limit_callback_;  // 输入超过MaxLength限制
    KRRenderCallback keyboard_height_changed_callback_;   // 键盘高度变化
    KRRenderCallback text_input_state_change_callback_;   // 文本输入状态变化callback（与 Android textInputStateChange 对齐）
    KRRenderCallback selection_change_callback_;          // 选区变化callback（与 Android KRTextFieldView.selectionChangeCallback 对齐）
    bool auto_hide_KeyBoard_on_ImeAction_ = false;        // 在触发各种IME 按钮时是否回收键盘，默认是不回收
    bool is_setting_text_input_state_ = false;            // 通过 setTextInputState 主动写入期间，抑制 textInputStateChange 回流防止业务死循环

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
     * 受控组件模式：解析 textInputState JSON 并把 text/光标 写入到 ArkUI 节点。
     *
     * 跨端语义参考 Android KRTextFieldView.setTextInputState：
     *   - 仅消费 text / selectionStart / selectionEnd 三字段；
     *   - composition 区不在 OHOS 老节点的可写能力内，忽略。
     *
     * ⚠️ 当前 OHOS 老节点的可写能力局限：
     *   - selection 范围写入降级为「只把光标设到 selectionStart」，不支持真选中态。
     *   - TODO：后续如有需要，再用 NODE_TEXT_INPUT_TEXT_SELECTION / NODE_TEXT_AREA_TEXT_SELECTION
     *     的 [start,end] 形式实现真选区。
     *
     * 主动写入期间通过 is_setting_text_input_state_ 抑制 textInputStateChange 回调，
     * 避免业务把状态写回来形成死循环。
     */
    void SetTextInputStateInternal(const std::string &json);

    /**
     * 拼装 textInputState 出参 map，与 Android createTextInputStateParamMap 对齐：
     *   - 始终回 {text, selectionStart, selectionEnd, compositionStart=-1, compositionEnd=-1}；
     *   - 仅当 length_limit_type_ != -1 时附带 length 字段。
     */
    KRRenderValueMap CreateTextInputStateMap();

    /**
     * getTextInputState 方法路径：把当前 state 通过 callback 回吐给业务。
     */
    void GetTextInputStateInternal(const KRRenderCallback &callback);

    /**
     * 在 OnTextDidChanged 末尾按需触发，参考 Android 时机一致。
     * 处于 SetTextInputStateInternal 主动写入期间会被抑制。
     */
    void NotifyTextInputStateChange();

    /**
     * 选区变化事件回调，跨端语义对齐 Android KRTextFieldView.onSelectionChanged。
     * 主动写入期间通过 is_setting_text_input_state_ 抑制。
     */
    void NotifySelectionChange();

    /**
     * 处理 ArkUI 原生选区变化事件（NODE_TEXT_INPUT_ON_TEXT_SELECTION_CHANGE /
     * NODE_TEXT_AREA_ON_TEXT_SELECTION_CHANGE）。事件中携带 [start, end]，我们同时触发
     * selectionChange 与 textInputStateChange（后者会以最新选区重新拼装 state map）。
     */
    void OnTextSelectionChange(ArkUI_NodeEvent *event);

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
     * 设置输入的文本内容
     */
    void SetContentText(const std::string &text);

    /**
     * 对齐 iOS：程序化写入包含短码且超出 maxLength 时，整段拒绝本次写入。
     */
    bool ShouldRejectProgrammaticShortcodeInput(const std::string &text);

    /**
     * 限制输入文本到最大长度
     */
    bool LimitInputContentTextInMaxLength();
    /**
     * 通知文本长度超过限制
     */
    void NotifyTextLengthBeyondLimit();
    /**
     * 初始化文本长度过滤
     */
    void SetupLengthInputFilter();
    /**
     * 即将插入文本回调
     */
    void OnWillInsertText(ArkUI_NodeEvent *event);
    /**
     * 粘贴文本回调
     */
    void OnPasteText(ArkUI_NodeEvent *event);
    /**
     * 文本即将变化回调
     */
    void OnWillChangeText(ArkUI_NodeEvent *event);
    /**
     * 重置最大长度
     */
    void DoResetMaxLength();
    /**
     * 异步重置最大长度
     */
    void DelayResetMaxLength();
    
    /**
     * 计算文本长度（根据不同的限制类型）
     * @param text 要计算的文本
     * @param rmStart 要移除计算的起始位置（按UTF-16算）
     * @param rmEnd 要移除计算的结束位置（按UTF-16算）
     * @return 文本的长度值
     */
    int CalculateTextLength(const std::string &text, size_t rmStart = 0, size_t rmEnd = 0);

    /**
     * 截取文本到指定的长度限制
     * @param text 要截取的文本（UTF-8编码）
     * @param keep 最大长度限制
     * @return 截断的索引位置
     */
    int CalculateTruncateIndex(const std::string &text, size_t keep);

    /**
     * 根据UTF-8首字节获取对应字符的字节数
     */
    int GetUTF8ByteLengthOfFirstCharacter(unsigned char c);
    
    /**
     * 获取指定code point对应的UTF-8字节数
     */
    int GetUTF8ByteLengthOfCodePoint(char32_t codePoint);
    
    /**
     * 获取指定code point对应的可视宽度
     */
    int GetVisualWidthOfCodePoint(char32_t codePoint);
    
    /**
     * 获取text从u8Start到u16Count的UTF-8字节数
     * @param text 输入文本
     * @param u8Start UTF-8起始字节索引
     * @param u16Count UTF-16字符数量
     * @return 对应的UTF-8字节数
     */
    int GetUTF8ByteCount(const std::string &text, size_t u8Start, size_t u16Count);
    
    int GetUTF16Length(const std::string &text);
};

#endif  // CORE_RENDER_OHOS_KRTEXTFIELDVIEW_H
