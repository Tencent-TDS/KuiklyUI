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

#ifndef CORE_RENDER_OHOS_KRTEXTEDITORFIELDVIEW_H
#define CORE_RENDER_OHOS_KRTEXTEDITORFIELDVIEW_H

#include <chrono>
#include <cstddef>
#include <cstdint>

#include "libohos_render/expand/components/input/KRTextEditorCommon.h"
#include "libohos_render/export/IKRRenderViewExport.h"

// 编译期 guard：当 SDK header < API 24 时，KRTextEditorCommon.h 已通过
// `#if KUIKLY_TEXT_EDITOR_AVAILABLE` 宏屏蔽全部 ARKUI_NODE_TEXT_EDITOR 相关类型；
// 本头中的类成员引用了大量 API 24 类型/枚举，因此必须整体被同一宏 guard 掉，
// 否则低 SDK header 编译会因 OH_ArkUI_TextEditor* 等未声明而失败。
//
// 注：开关 C API（KRSetUseNewTextInputComponent / KRGetUseNewTextInputComponent）
// 已被搬到 KRTextEditorSwitch.h，永远独立可用，不受本宏 guard 影响。
#if KUIKLY_TEXT_EDITOR_AVAILABLE

/**
 * 基于 ARKUI_NODE_TEXT_EDITOR（OpenHarmony API 24 新增）的单行输入实现，与老
 * KRTextFieldView（基于 ARKUI_NODE_TEXT_INPUT）并列存在。
 *
 * 注册策略（β 方案，见 ComponentsRegisterEntry.h）：
 *   * KRTextFieldView 永远走老实现，与本类无关；
 *   * 本类仅作为 KRTextEditorAreaView 的基类存在，承载共享逻辑。
 */
class KRTextEditorFieldView : public IKRRenderViewExport {
 public:
    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    void OnDestroy() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    void CallMethod(const std::string &method, const KRAnyValue &params,
                    const KRRenderCallback &callback) override;

 protected:
    // 多行派生类通过以下 hook 改变默认行为
    virtual bool IsSingleLine() const {
        return true;
    }
    // 是否拦截换行符（单行拦截、多行允许）
    virtual bool InterceptNewline() const {
        return true;
    }

    kuikly::text_editor::KRTextEditorState state_;

    // 「保焦点关键盘」状态机（与老 KRTextFieldView 完全对齐，属性换用 TEXT_EDITOR 专属
    // NODE_TEXT_EDITOR_ENABLE_KEYBOARD_ON_FOCUS，@since 24 与组件同版本）：
    // is_node_focused_：由 ON_FOCUS / ON_BLUR 事件维护的节点焦点状态位
    // pending_refocus_after_blur_：主动 Blur 后等待 ON_BLUR 落地再于下一帧 refocus（两步流程）
    // awaiting_teardown_blur_：refocus#1 后等待 IME 拆卸尾部 blur（150ms 窗口内补夺焦点）
    bool is_node_focused_ = false;
    bool pending_refocus_after_blur_ = false;
    bool awaiting_teardown_blur_ = false;
    std::chrono::steady_clock::time_point last_refocus_ts_;

    // controller 首次绑定（DidInit 里调用）
    void InitControllerIfNeeded();

    // 文本 / 光标操作
    void SetContentText(const std::string &text);
    // 内部静默写入（不触发 textDidChange）。长度限制等内部链路使用，
    // 由外层统一收尾发一次回调，避免双回调。
    void SetContentTextSilent(const std::string &text);
    std::string GetContentText();
    uint32_t GetSelectionStartPosition();
    void SetSelectionStartPosition(uint32_t index);

    // Focus/Blur
    void Focus();
    void Blur();
    // 获焦但键盘全程不出现（对齐老 KRTextFieldView::FocusWithoutKeyBoard 的跨端语义）。
    // 实现：NODE_TEXT_EDITOR_ENABLE_KEYBOARD_ON_FOCUS=0 + blur→refocus 状态机，
    // 与老组件同构；详见各实现处注释。
    void FocusWithoutKeyboard();
    // 两步 refocus 调度：下一帧 refocus，arm 控制是否等待 IME 拆卸尾部 blur
    void ScheduleRefocus(bool arm_awaiting_teardown);
    void GetCursorIndex(const KRRenderCallback &callback);
    void SetCursorIndex(uint32_t index);

    // textInputState 协议（与 Android KRTextFieldView.setTextInputState/getTextInputState 同语义）。
    // 设置文本+选区+composition 的原子化 API；composition 字段被忽略（OHOS 不暴露 IME 组合区间）。
    // 调用期间会用 state_.is_setting_text_input_state_ guard 抑制 textDidChange /
    // textInputStateChange / selectionChange 三个回调，避免业务侧 set->callback->set 回环。
    void SetTextInputStateInternal(const std::string &json);
    // 同步收集当前 text+selection 通过 callback 回传，便于业务侧 read-modify-write。
    void GetTextInputStateInternal(const KRRenderCallback &callback);
    // 触发一次 textInputStateChange 回调（带完整 state payload）。仅在 textDidChange 一同发。
    void EmitTextInputStateChange();
    // 触发一次 selectionChange 回调（仅选区/光标变化、文本未变时使用）。
    void EmitSelectionChange();

    // 事件分发入口
    void OnTextDidChanged(ArkUI_NodeEvent *event);
    void OnInputFocus(ArkUI_NodeEvent *event);
    void OnInputBlur(ArkUI_NodeEvent *event);
    // 编辑态变化（NODE_TEXT_EDITOR_ON_EDITING_CHANGE, @since 24, data[0].i32=1/0）。
    // TEXT_EDITOR 上程序化 NODE_FOCUS_STATUS=0 不派发 NODE_ON_BLUR（真机日志证实），
    // 状态机由本事件驱动：editing=0 走原三分支（pending refocus / teardown / 真实停止上抛 blur）。
    void OnEditingChange(ArkUI_NodeEvent *event);
    void OnInputReturn(ArkUI_NodeEvent *event);
    void OnWillChangeText(ArkUI_NodeEvent *event);
    void OnPasteText(ArkUI_NodeEvent *event);
    void OnCopyText(ArkUI_NodeEvent *event);
    // SDK NODE_TEXT_EDITOR_ON_SELECTION_CHANGE：选区/光标变化（含纯光标移动）。
    // 与 textDidChange 解耦——纯选区变化不会触发 textDidChange。
    void OnSelectionChanged(ArkUI_NodeEvent *event);

    // max-length 过滤
    bool LimitInputContentTextInMaxLength();
    void NotifyTextLengthBeyondLimit();
    void SetupLengthInputFilter();
    void DoResetMaxLength();

    // keyboardType 映射（单/多行对 password 支持不同）
    virtual void ApplyKeyboardType(const std::string &type);

    // returnKeyType 映射
    void ApplyReturnKeyType(const std::string &type);
};

#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE

#endif  // CORE_RENDER_OHOS_KRTEXTEDITORFIELDVIEW_H
