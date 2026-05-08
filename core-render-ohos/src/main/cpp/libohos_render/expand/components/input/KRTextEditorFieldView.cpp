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

#include "libohos_render/expand/components/input/KRTextEditorFieldView.h"

#include "libohos_render/manager/KRKeyboardManager.h"
#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRStringUtil.h"
#include "libohos_render/utils/KRViewUtil.h"

#include <algorithm>
#include <cstring>

namespace {
using kuikly::text_editor::KRTextEditorState;

// 新控件开关的实际存储位置——仅存在于 libkuikly.so 这一个 .so 内（本 TU 静态变量）。
// 宿主侧通过 KRSetUseNewTextInputComponent 写入、ComponentsRegisterEntry 通过
// KRGetUseNewTextInputComponent 读取，保证跨 so 调用看到同一份数据。
int g_kr_use_new_text_input_component = 0;
}  // namespace

extern "C" void KRSetUseNewTextInputComponent(int value) {
    g_kr_use_new_text_input_component = value;
}

extern "C" int KRGetUseNewTextInputComponent() {
    return g_kr_use_new_text_input_component;
}

ArkUI_NodeHandle KRTextEditorFieldView::CreateNode() {
#ifdef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    // SDK header < 24，不创建节点；注册闭包在运行时也不会选到本类，此处仅防御。
    return nullptr;
#else
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT_EDITOR);
#endif
}

void KRTextEditorFieldView::DidInit() {
#ifdef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    return;
#else
    auto node = GetNode();
    if (!node) {
        return;
    }
    // 对齐老实现的默认样式：透明背景、无圆角、无 padding。
    // TEXT_EDITOR 节点系统默认有 padding（且上下不对称，上多下少），导致叠加
    // NODE_TEXT_CONTENT_ALIGN=CENTER 后视觉重心仍偏下。与老 KRTextFieldView 一致，
    // 此处清零 padding，由 NODE_TEXT_CONTENT_ALIGN 负责单行文字容器内居中；多行场景
    // 亦不会因默认 padding 影响顶/底部边界。
    kuikly::util::UpdateNodeBackgroundColor(node, 0);
    kuikly::util::UpdateNodeBorderRadius(node, KRBorderRadiuses(0, 0, 0, 0));
    kuikly::util::SetArkUIPadding(node, 0, 0, 0, 0);

    // 单行 / 多行开关
    kuikly::text_editor::UpdateSingleLine(node, IsSingleLine());

    // 绑定 StyledStringController 到节点（文本 / 占位 / typing 样式都靠它）
    InitControllerIfNeeded();

    // 默认注册 textDidChange 相关事件：
    //   * NODE_TEXT_EDITOR_ON_DID_CHANGE：文本变化后；payload 不含文本，需主动 GetStyledString
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_DID_CHANGE);

    // 首次应用 typing style（字体色、字号、字重、对齐）
    kuikly::text_editor::ApplyTypingStyle(state_);

    // 尝试通过节点级属性 NODE_TEXT_CONTENT_ALIGN 将容器内文字整体垂直居中。
    // 该属性声明于 native_node.h (since API 21)，注释归属 Text 组件；但 TEXT_EDITOR
    // 与 Text 共享样式属性区段，实测可尝试设置，失败时 setAttribute 返回非 0。
    // 枚举：ARKUI_TEXT_CONTENT_ALIGN_TOP=0 / CENTER=1 / BOTTOM=2。
    {
        ArkUI_NumberValue align_val[] = {{.i32 = ARKUI_TEXT_CONTENT_ALIGN_TOP}};
        ArkUI_AttributeItem align_item = {align_val, 1};
        int32_t ret = kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_CONTENT_ALIGN, &align_item);
        if (ret != 0) {
            KR_LOG_DEBUG << "KRTextEditorFieldView: setAttribute(NODE_TEXT_CONTENT_ALIGN=CENTER) returned "
                         << ret << " (not supported on this node type or SDK)";
        } else {
            KR_LOG_DEBUG << "KRTextEditorFieldView: NODE_TEXT_CONTENT_ALIGN=CENTER applied";
        }
    }

    // 一次日志便于现场确认分支命中
    static bool logged = false;
    if (!logged) {
        logged = true;
        KR_LOG_DEBUG << "KRTextEditorFieldView initialized (ARKUI_NODE_TEXT_EDITOR)";
    }
#endif
}

void KRTextEditorFieldView::OnDestroy() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    if (state_.keyboard_height_changed_callback_) {
        auto key = NewKRRenderValue(GetViewTag())->toString();
        if (auto root = GetRootView().lock()) {
            auto window_id = root->GetContext()->WindowId();
            KRKeyboardManager::GetInstance().RemoveKeyboardTask(window_id, key);
        }
    }
    state_.keyboard_height_changed_callback_ = nullptr;

    if (state_.controller_) {
        OH_ArkUI_TextEditorStyledStringController_Destroy(state_.controller_);
        state_.controller_ = nullptr;
    }
#endif
}

void KRTextEditorFieldView::InitControllerIfNeeded() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    if (state_.controller_) {
        return;
    }
    state_.controller_ = OH_ArkUI_TextEditorStyledStringController_Create();
    if (!state_.controller_) {
        return;
    }
    ArkUI_AttributeItem item = {};
    item.object = state_.controller_;
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_EDITOR_STYLED_STRING_CONTROLLER, &item);
#endif
}

bool KRTextEditorFieldView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                    const KRRenderCallback event_call_back) {
    using namespace kuikly::text_editor;

#ifdef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
#else
    auto node = GetNode();

    if (kuikly::util::isEqual(prop_key, kText)) {
        SetContentText(prop_value->toString());
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kPlaceholder)) {
        state_.placeholder_text_ = prop_value->toString();
        ApplyPlaceholder(node, state_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kPlaceholderColor)) {
        state_.placeholder_color_ = kuikly::util::ConvertToHexColor(prop_value->toString());
        state_.placeholder_color_set_ = true;
        ApplyPlaceholder(node, state_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kFontSize)) {
        state_.font_size_ = prop_value->toFloat();
        ApplyTypingStyle(state_);
        ApplyPlaceholder(node, state_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kFontWeight)) {
        float scale = 1.0;
        if (auto root = GetRootView().lock()) {
            scale = root->GetContext()->Config()->GetFontWeightScale();
        }
        state_.font_weight_ = kuikly::util::ConvertArkUIFontWeight(prop_value->toInt(), scale);
        ApplyTypingStyle(state_);
        ApplyPlaceholder(node, state_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kColor)) {
        state_.font_color_ = kuikly::util::ConvertToHexColor(prop_value->toString());
        ApplyTypingStyle(state_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kTintColor)) {
        UpdateCaretColor(node, kuikly::util::ConvertToHexColor(prop_value->toString()));
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kTextAlign)) {
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
        // 1) 更新 typing style，保证后续键入继承新对齐
        ApplyTypingStyle(state_);
        // 2) TypingParagraphStyle 不会回写已有 span 的段落样式，因此需要把当前文本
        //    按新对齐重写一次 styled string；走静默路径，不触发 textDidChange（仅
        //    样式变化，内容未变）。
        if (!state_.cached_text_.empty()) {
            kuikly::text_editor::SetStyledText(state_, state_.cached_text_);
        }
        // 3) placeholder 对齐：OH_ArkUI_TextEditorPlaceholderOptions 没有 TextAlign
        //    接口，段落样式也只影响 StyledString 内的真实文本。参考老控件
        //    KRTextFieldView::UpdateInputNodeTextAlign，使用通用属性 NODE_TEXT_ALIGN
        //    驱动节点自绘的 placeholder 对齐。
        ArkUI_NumberValue align_value[] = {{.i32 = static_cast<int32_t>(node_align)}};
        ArkUI_AttributeItem align_item = {align_value, 1};
        kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_ALIGN, &align_item);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEditable)) {
        state_.focusable_ = prop_value->toBool();
        UpdateFocusable(node, state_.focusable_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kKeyboardType)) {
        // TEXT_EDITOR 未提供 keyboardType 属性；按 1A 方案：打 warn 日志，不映射。
        ApplyKeyboardType(prop_value->toString());
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kReturnKeyType)) {
        ApplyReturnKeyType(prop_value->toString());
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kLengthLimitType)) {
        state_.length_limit_type_ = prop_value->toInt();
        if (state_.max_length_ != -1) {
            DoResetMaxLength();
            LimitInputContentTextInMaxLength();
            SetupLengthInputFilter();
        }
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kMaxTextLength)) {
        state_.max_length_ = prop_value->toInt();
        DoResetMaxLength();
        LimitInputContentTextInMaxLength();
        SetupLengthInputFilter();
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kAutoHideKeyBoardOnIMEAction)) {
        state_.auto_hide_KeyBoard_on_ImeAction_ = prop_value->toBool();
        return true;
    }

    // --- 事件 ---
    if (kuikly::util::isEqual(prop_key, kEventTextDidChanged)) {
        state_.text_did_change_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_DID_CHANGE);
        // 若在事件注册前已经 SetContentText（初始带文字 / 外部旁路写入），此时
        // OnTextDidChanged 已打挂起标记，这里补发一次 textDidChange，行为上等价老
        // KRTextFieldView 的"SetContentText 同步触发 onTextDidChange"。
        if (state_.pending_text_did_change_ && event_call_back) {
            state_.pending_text_did_change_ = false;
            OnTextDidChanged(nullptr);
        }
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEventInputFocus)) {
        state_.input_focus_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_ON_FOCUS);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEventInputBlur)) {
        state_.input_blur_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_ON_BLUR);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEventInputReturn)) {
        state_.input_return_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_SUBMIT);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEventTextLengthBeyondLimit)) {
        state_.text_length_beyond_limit_callback_ = event_call_back;
        DoResetMaxLength();
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEventKeyboardHeightChange)) {
        state_.keyboard_height_changed_callback_ = event_call_back;
        auto key = NewKRRenderValue(GetViewTag())->toString();
        if (auto root = GetRootView().lock()) {
            auto window_id = root->GetContext()->WindowId();
            KRKeyboardManager::GetInstance().AddKeyboardTask(
                window_id, key, [event_call_back](float height, int duration_ms) {
                    KRRenderValueMap map;
                    map["height"] = NewKRRenderValue(height);
                    map["duration"] = NewKRRenderValue(duration_ms / 1000.0);
                    event_call_back(NewKRRenderValue(map));
                });
        }
        return true;
    }

    return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
#endif
}

void KRTextEditorFieldView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
#ifdef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    return;
#else
    switch (event_type) {
        case ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_DID_CHANGE:
            OnTextDidChanged(event);
            break;
        case ArkUI_NodeEventType::NODE_ON_FOCUS:
            OnInputFocus(event);
            break;
        case ArkUI_NodeEventType::NODE_ON_BLUR:
            OnInputBlur(event);
            break;
        case ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_SUBMIT:
            OnInputReturn(event);
            break;
        case ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_WILL_CHANGE:
            OnWillChangeText(event);
            break;
        case ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_PASTE:
            OnPasteText(event);
            break;
        default:
            break;
    }
#endif
}

void KRTextEditorFieldView::CallMethod(const std::string &method, const KRAnyValue &params,
                                       const KRRenderCallback &callback) {
#ifdef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    IKRRenderViewExport::CallMethod(method, params, callback);
    return;
#else
    using namespace kuikly::text_editor;
    if (kuikly::util::isEqual(method, kMethodFocus)) {
        Focus();
    } else if (kuikly::util::isEqual(method, kMethodBlur)) {
        Blur();
    } else if (kuikly::util::isEqual(method, kMethodSetText)) {
        SetContentText(params->toString());
    } else if (kuikly::util::isEqual(method, kMethodGetCursorIndex)) {
        GetCursorIndex(callback);
    } else if (kuikly::util::isEqual(method, kMethodSetCursorIndex)) {
        SetCursorIndex(params->toInt());
    } else {
        IKRRenderViewExport::CallMethod(method, params, callback);
    }
#endif
}

// ============================================================================
// 内部实现
// ============================================================================

void KRTextEditorFieldView::SetContentText(const std::string &text) {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    if (state_.cached_text_ == text) {
        return;  // 幂等：内容未变，不写入也不回调
    }
    kuikly::text_editor::SetStyledText(state_, text);
    // 重新应用 typing style 保证后续键入继承样式
    kuikly::text_editor::ApplyTypingStyle(state_);
    // 主动补发一次 textDidChange，对齐老 KRTextFieldView 行为：
    // ARKUI_NODE_TEXT_EDITOR 通过 styled string controller 写入不会反弹 ON_DID_CHANGE，
    // 因此必须在此手工触发一次。
    // 注意：LimitInputContentTextInMaxLength 等内部截断路径应使用 SetContentTextSilent
    // 以免在外层 OnTextDidChanged 收尾前多发一次。
    OnTextDidChanged(nullptr);
#endif
}

void KRTextEditorFieldView::SetContentTextSilent(const std::string &text) {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    if (state_.cached_text_ == text) {
        return;
    }
    kuikly::text_editor::SetStyledText(state_, text);
    kuikly::text_editor::ApplyTypingStyle(state_);
#endif
}

std::string KRTextEditorFieldView::GetContentText() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    return kuikly::text_editor::GetStyledText(state_);
#else
    return "";
#endif
}

uint32_t KRTextEditorFieldView::GetSelectionStartPosition() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    // 优先走 Selection；若区间为 0，说明无选区，则读 caret
    if (state_.controller_) {
        uint32_t start = 0, end = 0;
        if (OH_ArkUI_TextEditorStyledStringController_GetSelection(state_.controller_, &start, &end) ==
                ARKUI_ERROR_CODE_NO_ERROR &&
            start != end) {
            return start;
        }
        int32_t offset = 0;
        OH_ArkUI_TextEditorStyledStringController_GetCaretOffset(state_.controller_, &offset);
        return offset < 0 ? 0 : static_cast<uint32_t>(offset);
    }
#endif
    return 0;
}

void KRTextEditorFieldView::SetSelectionStartPosition(uint32_t index) {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    kuikly::text_editor::SetCaretOffset(state_, static_cast<int32_t>(index));
#endif
}

void KRTextEditorFieldView::Focus() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    kuikly::text_editor::UpdateFocusStatus(GetNode(), true);
#endif
}

void KRTextEditorFieldView::Blur() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    // 优先走 controller 的 StopEditing（更精准收键盘），再 fallback 到 FocusStatus
    if (state_.controller_) {
        OH_ArkUI_TextEditorStyledStringController_StopEditing(state_.controller_);
    } else {
        kuikly::text_editor::UpdateFocusStatus(GetNode(), false);
    }
#endif
}

void KRTextEditorFieldView::GetCursorIndex(const KRRenderCallback &callback) {
    if (!callback) {
        return;
    }
    uint32_t pos = GetSelectionStartPosition();
    KRRenderValueMap map;
    map["cursorIndex"] = NewKRRenderValue(static_cast<int>(pos));
    callback(NewKRRenderValue(map));
}

void KRTextEditorFieldView::SetCursorIndex(uint32_t index) {
    SetSelectionStartPosition(index);
}

// ============================================================================
// 事件回调
// ============================================================================

void KRTextEditorFieldView::OnTextDidChanged(ArkUI_NodeEvent *event) {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    (void)event;
    // 长度限制：无 lengthLimitType 分支走 MaxLength 节点属性直接约束；
    // 有 lengthLimitType 分支通过 ON_WILL_CHANGE 拦截 + ON_DID_CHANGE 补救（非法状态下截断）。
    if (state_.length_limit_type_ == -1) {
        LimitInputContentTextInMaxLength();
    } else if (state_.max_length_ != -1) {
        LimitInputContentTextInMaxLength();
    }
    if (state_.text_did_change_callback_) {
        auto text = GetContentText();
        state_.cached_text_ = text;
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(text);
        if (state_.length_limit_type_ != -1) {
            int length = kuikly::text_editor::CalculateTextLength(state_.length_limit_type_, text);
            map["length"] = NewKRRenderValue(length);
        }
        state_.text_did_change_callback_(NewKRRenderValue(map));
        state_.pending_text_did_change_ = false;  // 清挂起标记
    } else {
        // callback 还未注册（Kuikly 侧先下发属性再下发事件，首次 setProp(text)
        // 会走到这里）；打挂起标记，等事件注册分支补发一次。
        state_.pending_text_did_change_ = true;
    }
#endif
}

void KRTextEditorFieldView::OnInputFocus(ArkUI_NodeEvent *event) {
    (void)event;
    if (state_.input_focus_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(GetContentText());
        state_.input_focus_callback_(NewKRRenderValue(map));
    }
}

void KRTextEditorFieldView::OnInputBlur(ArkUI_NodeEvent *event) {
    (void)event;
    if (state_.input_blur_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(GetContentText());
        state_.input_blur_callback_(NewKRRenderValue(map));
    }
}

void KRTextEditorFieldView::OnInputReturn(ArkUI_NodeEvent *event) {
    (void)event;
    if (state_.input_return_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(GetContentText());
        map["ime_action"] =
            NewKRRenderValue(kuikly::util::ConvertEnterKeyTypeToString(state_.enter_key_type_));
        state_.input_return_callback_(NewKRRenderValue(map));
        if (state_.auto_hide_KeyBoard_on_ImeAction_) {
            Blur();
        }
    }
}

void KRTextEditorFieldView::OnWillChangeText(ArkUI_NodeEvent *event) {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    OH_ArkUI_TextEditorChangeEvent *change_event =
        OH_ArkUI_NodeEvent_GetTextEditorOnWillChangeEvent(event);
    if (!change_event) {
        return;
    }

    // 单行模式：拦截换行符 —— 若待替换串中含 '\n'，拒绝（返回 0）
    if (InterceptNewline()) {
        ArkUI_StyledString_Descriptor *repl = OH_ArkUI_StyledString_Descriptor_Create();
        if (repl) {
            bool has_newline = false;
            if (OH_ArkUI_TextEditorChangeEvent_GetReplacementStyledString(change_event, repl) ==
                ARKUI_ERROR_CODE_NO_ERROR) {
                // 使用通用工具函数读取，避免 buffer 越界导致 Destroy 时 crash
                std::string buf = kuikly::text_editor::ReadDescriptorString(repl);
                if (buf.find('\n') != std::string::npos) {
                    has_newline = true;
                }
            }
            // TODO(kuikly-text-editor): OH_ArkUI_StyledString_Descriptor_Destroy 在真机会触发
            // free_default 崩溃（详见 KRTextEditorCommon.h GetStyledText 处的 TODO）。
            // 暂注释掉，每次 OnWillChange 泄漏一个空壳 descriptor；后续跟进。
            // OH_ArkUI_StyledString_Descriptor_Destroy(repl);
            (void)repl;
            if (has_newline) {
                ArkUI_NumberValue ret[] = {{.i32 = 0}};  // 拒绝
                OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
                return;
            }
        }
    }

    // max-length 过滤（length_limit_type != -1 时手动过滤）
    if (state_.max_length_ != -1 && state_.length_limit_type_ != -1) {
        uint32_t r_start = 0, r_end = 0;
        OH_ArkUI_TextEditorChangeEvent_GetRangeBefore(change_event, &r_start, &r_end);

        ArkUI_StyledString_Descriptor *repl = OH_ArkUI_StyledString_Descriptor_Create();
        if (repl &&
            OH_ArkUI_TextEditorChangeEvent_GetReplacementStyledString(change_event, repl) ==
                ARKUI_ERROR_CODE_NO_ERROR) {
            std::string repl_str = kuikly::text_editor::ReadDescriptorString(repl);
            if (!repl_str.empty()) {
                auto dest = GetContentText();
                // 使用与老实现同语义的 filter：source 截断后通过拒绝方式落地
                std::string tmp = repl_str;
                tmp.push_back('\0');  // FilterSource 依赖 '\0' 结尾
                bool changed =
                    kuikly::text_editor::FilterSource(tmp.data(), dest, r_start, r_end, state_);
                if (changed) {
                    NotifyTextLengthBeyondLimit();
                    if (tmp.empty() || tmp[0] == '\0') {
                        ArkUI_NumberValue ret[] = {{.i32 = 0}};  // 拒绝
                        OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
                        // TODO(kuikly-text-editor): Destroy 会崩，暂注释。
                        // OH_ArkUI_StyledString_Descriptor_Destroy(repl);
                        return;
                    }
                    // 原生 API 无法在此替换插入文本；退而求其次：放行本次 ->
                    // 由 ON_DID_CHANGE 的 LimitInputContentTextInMaxLength 做后置截断。
                }
            }
            // TODO(kuikly-text-editor): Destroy 会崩，暂注释（详见 KRTextEditorCommon.h 处 TODO）。
            // OH_ArkUI_StyledString_Descriptor_Destroy(repl);
            (void)repl;
        } else if (repl) {
            // TODO(kuikly-text-editor): Destroy 会崩，暂注释。
            // OH_ArkUI_StyledString_Descriptor_Destroy(repl);
            (void)repl;
        }
    }

    // 默认允许
    ArkUI_NumberValue ret[] = {{.i32 = 1}};
    OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
#else
    (void)event;
#endif
}

void KRTextEditorFieldView::OnPasteText(ArkUI_NodeEvent *event) {
    // TEXT_EDITOR ON_PASTE 只能返回是否放行，不提供粘贴文本。按 2A 方案：
    // 此处不拦截；若 length_limit_type != -1，由 ON_WILL_CHANGE 的
    // GetReplacementStyledString 路径负责截断（已在 OnWillChangeText 实现）。
    (void)event;
}

// ============================================================================
// max-length 过滤辅助
// ============================================================================

bool KRTextEditorFieldView::LimitInputContentTextInMaxLength() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    if (state_.max_length_ < 0) {
        return false;
    }
    if (state_.length_limit_type_ == -1) {
        auto text32 = kuikly::util::ConvertToU32String(GetContentText());
        if (static_cast<int32_t>(text32.length()) > state_.max_length_) {
            text32 = text32.substr(0, state_.max_length_);
            // 内部截断走静默写入：外层 OnTextDidChanged 会在本函数返回后统一发一次回调
            SetContentTextSilent(kuikly::util::ConvertToNormalString(text32));
            NotifyTextLengthBeyondLimit();
            return true;
        }
        return false;
    } else {
        auto destText = GetContentText();
        uint32_t cursor = GetSelectionStartPosition();
        if (kuikly::text_editor::CalculateTextLength(state_.length_limit_type_, destText) >
            state_.max_length_) {
            NotifyTextLengthBeyondLimit();
            int u8position =
                kuikly::text_editor::GetUTF8ByteCount(destText, 0, cursor);
            std::string head = destText.substr(0, u8position);
            std::string tail = destText.substr(u8position);
            // 将 head 按与老实现一致的 filter 策略截断：source 为当前光标前文本，dest 为光标后文本
            std::string src = head;
            src.push_back('\0');
            kuikly::text_editor::FilterSource(src.data(), tail, 0, 0, state_);
            std::string new_head(src.c_str());
            // 内部截断走静默写入：外层 OnTextDidChanged 会在本函数返回后统一发一次回调
            SetContentTextSilent(new_head + tail);
            uint32_t new_pos = static_cast<uint32_t>(kuikly::text_editor::GetUTF16Length(new_head));
            KRMainThread::RunOnMainThread(
                [weakSelf = weak_from_this(), new_pos] {
                    if (auto strongSelf =
                            std::dynamic_pointer_cast<KRTextEditorFieldView>(weakSelf.lock())) {
                        strongSelf->SetCursorIndex(new_pos);
                    }
                });
            return true;
        }
        return false;
    }
#else
    return false;
#endif
}

void KRTextEditorFieldView::NotifyTextLengthBeyondLimit() {
    if (state_.text_length_beyond_limit_callback_) {
        KRRenderValueMap map;
        state_.text_length_beyond_limit_callback_(NewKRRenderValue(map));
    }
}

void KRTextEditorFieldView::SetupLengthInputFilter() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    if (state_.length_limit_type_ == -1 || state_.length_input_filter_) {
        return;
    }
    state_.length_input_filter_ = true;
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_WILL_CHANGE);
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_PASTE);
#endif
}

void KRTextEditorFieldView::DoResetMaxLength() {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    auto node = GetNode();
    if (state_.max_length_ == -1) {
        kuikly::text_editor::ResetMaxLengthAttr(node);
    } else {
        if (state_.length_limit_type_ == -1) {
            // 简单路径：有 beyondLimit 回调则放宽节点级限制以便手动截断，否则直接限制
            if (state_.text_length_beyond_limit_callback_) {
                kuikly::text_editor::UpdateMaxLengthAttr(node, 10000000);
            } else {
                kuikly::text_editor::UpdateMaxLengthAttr(node, state_.max_length_);
            }
        } else {
            size_t limit = static_cast<size_t>(state_.max_length_);
            if (state_.length_limit_type_ == 1) {  // CHARACTER
                limit *= 2;
            }
            if (state_.text_length_beyond_limit_callback_) {
                limit += 2;
            }
            kuikly::text_editor::UpdateMaxLengthAttr(node, static_cast<int32_t>(limit));
        }
    }
#endif
}

// ============================================================================
// keyboardType / returnKeyType 映射
// ============================================================================

void KRTextEditorFieldView::ApplyKeyboardType(const std::string &type) {
    // ARKUI_NODE_TEXT_EDITOR 不暴露 keyboardType 属性。
    // 按 1A 方案：打 warn 日志，不做映射（键盘默认形态）。
    //
    // 注意：曾尝试直接复用老控件的 UpdateInputNodeKeyboardType
    // （底层写 NODE_TEXT_INPUT_TYPE，属于 ARKUI_NODE_TEXT_INPUT 作用域的属性）
    // 在 ARKUI_NODE_TEXT_EDITOR 节点上 setAttribute 会 crash，
    // 因此两类节点底层实现并不共享属性通路，不可跨节点复用。
    // 后续若要支持 number/email，需通过 NODE_TEXT_EDITOR_ON_WILL_CHANGE
    // 过滤或 NODE_TEXT_EDITOR_CUSTOM_KEYBOARD 自绘键盘等方式实现。
    if (type != "default" && !type.empty()) {
        KR_LOG_DEBUG << "KRTextEditorFieldView: keyboardType=" << type
                     << " is not supported on ARKUI_NODE_TEXT_EDITOR (API 24+), fallback to default";
    }
}

void KRTextEditorFieldView::ApplyReturnKeyType(const std::string &type) {
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    ArkUI_EnterKeyType ek = kuikly::util::ConvertToEnterKeyType(type);
    state_.enter_key_type_ = ek;
    kuikly::text_editor::UpdateEnterKeyType(GetNode(), ek);
#endif
}
