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

// ========== 协议键 / 事件名 / 方法名（与老 KRTextFieldView 完全一致） ==========
// 大多数键直接复用 kuikly::text_editor 中的共享常量，避免与 `using namespace`
// 引入的同名符号发生二义性；当前文件仅保留新增的 selectionColor 键。
static constexpr const char *kSelectionColor = "selectionColor";

// ============================================================================
// 编译期 guard：当 SDK header < API 24 时，本 TU 中所有成员函数体都引用了
// API 24 才存在的 OH_ArkUI_TextEditor* / NODE_TEXT_EDITOR_* 等符号；为了让低
// SDK header 也能编译通过，整个文件主体都包在 `#if KUIKLY_TEXT_EDITOR_AVAILABLE`
// 之内。注册闭包（ComponentsRegisterEntry.h）会通过同一个宏 + 运行时 API
// 版本兜底，永远不会在低版本上选到本类。
//
// 注：开关 C API 已搬到 KRTextEditorSwitch.cpp，该 TU 永远参与编译，因此
// 即便本 TU 整体被编译期裁剪，宿主仍能稳定地读写开关。
// ============================================================================
#if KUIKLY_TEXT_EDITOR_AVAILABLE

namespace {
using kuikly::text_editor::KRTextEditorState;

bool ContainsImageSpanAfterTextPostProcessor(const std::string &text) {
    std::vector<kuikly::text::KRTextPostProcessSpan> spans;
    if (!kuikly::text::RunTextPostProcessor(kuikly::text_editor::kTextPostProcessorNameInput,
                                            text, spans)) {
        return false;
    }
    for (const auto &span : spans) {
        if (span.type == kuikly::text::KRTextPostProcessSpan::Type::kImage) {
            return true;
        }
    }
    return false;
}

std::string ReplaceUtf16Range(const std::string &text, uint32_t start, uint32_t end,
                              const std::string &replacement) {
    uint32_t safe_start = std::min(start, end);
    uint32_t safe_end = std::max(start, end);
    int head_bytes = kuikly::text_editor::GetUTF8ByteCount(text, 0, safe_start);
    int range_bytes = kuikly::text_editor::GetUTF8ByteCount(
        text, static_cast<size_t>(head_bytes), safe_end - safe_start);
    std::string result;
    result.reserve(text.size() - static_cast<size_t>(range_bytes) + replacement.size());
    result.append(text, 0, static_cast<size_t>(head_bytes));
    result.append(replacement);
    result.append(text, static_cast<size_t>(head_bytes + range_bytes), std::string::npos);
    return result;
}
}  // namespace

ArkUI_NodeHandle KRTextEditorFieldView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT_EDITOR);
}

void KRTextEditorFieldView::DidInit() {
#if !KUIKLY_TEXT_EDITOR_AVAILABLE
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

    // 默认注册 textDidChange / copy / paste 相关事件：
    //   * NODE_TEXT_EDITOR_ON_DID_CHANGE：文本变化后；payload 不含文本，需主动 GetStyledString
    //   * NODE_TEXT_EDITOR_ON_COPY：复制前临时把 image span 还原为 raw shortcode 文本
    //   * NODE_TEXT_EDITOR_ON_WILL_CHANGE：粘贴 raw shortcode 前主动重建 rich styled string
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_DID_CHANGE);
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_COPY);
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_WILL_CHANGE);
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_PASTE);

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
#if KUIKLY_TEXT_EDITOR_AVAILABLE
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
#if KUIKLY_TEXT_EDITOR_AVAILABLE
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
    // 灌入 assetsDir，让 SetStyledText 中的 emoji shortcode 替换路径能解析 "assets://..." URI。
    // 取值时机与老 KRImageView::LoadFromAssets 同条件：通过 RootView 的 KRConfig 拿到资源
    // 根目录字符串。Root view 不可用（极端 dispose 顺序）时留空，SetStyledText 自动跳过 emoji 段。
    if (auto root = GetRootView().lock()) {
        state_.assets_dir_ = root->GetContext()->Config()->GetAssetsDir();
    }
#endif
}

bool KRTextEditorFieldView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                    const KRRenderCallback event_call_back) {
    using namespace kuikly::text_editor;

#if !KUIKLY_TEXT_EDITOR_AVAILABLE
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
    if (kuikly::util::isEqual(prop_key, kSelectionColor)) {
        state_.selection_color_ = kuikly::util::ClampSelectionColorAlpha(
            kuikly::util::ConvertToHexColor(prop_value->toString()));
        state_.selection_color_set_ = true;
        ArkUI_NumberValue value[] = {{.u32 = state_.selection_color_}};
        ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
        kuikly::util::GetNodeApi()->setAttribute(node, NODE_TEXT_EDITOR_SELECTED_BACKGROUND_COLOR, &item);
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

    if (kuikly::util::isEqual(prop_key, kTextInputState)) {
        // attr 路径下发的 textInputState（与 Android InputAttr.textInputState 同路径）。
        // method 路径走 CallMethod(kMethodSetTextInputState)，两者体现为同一 helper。
        SetTextInputStateInternal(prop_value->toString());
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
    if (kuikly::util::isEqual(prop_key, kEventTextInputStateChange)) {
        // 与 Android KRTextFieldView observeTextInputStateChanged 对齐：事件注册仅记回调，
        // 实际触发点复用 NODE_TEXT_EDITOR_ON_DID_CHANGE（文本变化后发起）。
        // 同样需要补发机制：若首次 SetContentText 已经发生但事件未注册，补发一次。
        state_.text_input_state_change_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_DID_CHANGE);
        if (state_.pending_text_input_state_change_ && event_call_back) {
            state_.pending_text_input_state_change_ = false;
            EmitTextInputStateChange();
        }
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEventSelectionChange)) {
        // 选区/光标变化事件，复用 NODE_TEXT_EDITOR_ON_SELECTION_CHANGE（API 24+）。
        // 与 textInputStateChange 互补：纯选区变化仅发 selectionChange，文本变化发 textInputStateChange。
        state_.selection_change_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_SELECTION_CHANGE);
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
#if !KUIKLY_TEXT_EDITOR_AVAILABLE
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
        case ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_COPY:
            OnCopyText(event);
            break;
        case ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_SELECTION_CHANGE:
            OnSelectionChanged(event);
            break;
        default:
            break;
    }
#endif
}

void KRTextEditorFieldView::CallMethod(const std::string &method, const KRAnyValue &params,
                                       const KRRenderCallback &callback) {
#if !KUIKLY_TEXT_EDITOR_AVAILABLE
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
    } else if (kuikly::util::isEqual(method, kMethodSetTextInputState)) {
        SetTextInputStateInternal(params->toString());
    } else if (kuikly::util::isEqual(method, kMethodGetTextInputState)) {
        GetTextInputStateInternal(callback);
    } else {
        IKRRenderViewExport::CallMethod(method, params, callback);
    }
#endif
}

// ============================================================================
// 内部实现
// ============================================================================

void KRTextEditorFieldView::SetContentText(const std::string &text) {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
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
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    if (state_.cached_text_ == text) {
        return;
    }
    kuikly::text_editor::SetStyledText(state_, text);
    kuikly::text_editor::ApplyTypingStyle(state_);
#endif
}

std::string KRTextEditorFieldView::GetContentText() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    return kuikly::text_editor::GetStyledText(state_);
#else
    return "";
#endif
}

uint32_t KRTextEditorFieldView::GetSelectionStartPosition() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
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
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    kuikly::text_editor::SetCaretOffset(state_, static_cast<int32_t>(index));
#endif
}

void KRTextEditorFieldView::Focus() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    kuikly::text_editor::UpdateFocusStatus(GetNode(), true);
#endif
}

void KRTextEditorFieldView::Blur() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
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
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    (void)event;
    // guard：SetTextInputStateInternal 主动写入期间不发任何 textDidChange / textInputStateChange，
    // 与 Android isSettingTextInputState / iOS _ignoreTextDidChanged 同语义。
    if (state_.is_setting_text_input_state_) {
        return;
    }
    // 长度限制：无 lengthLimitType 分支走 MaxLength 节点属性直接约束；
    // 有 lengthLimitType 分支通过 ON_WILL_CHANGE 拦截 + ON_DID_CHANGE 补救（非法状态下截断）。
    if (state_.length_limit_type_ == -1) {
        LimitInputContentTextInMaxLength();
    } else if (state_.max_length_ != -1) {
        LimitInputContentTextInMaxLength();
    }
    // 对外暴露 / 业务持有的 text 永远是「raw shortcode 文本」（如 "[smile]a"）。
    // ArkUI GetStyledString 拿到的是「flat」（image span 被扁平化为占位空格，如 " a"）。
    // 直接上抛 flat 会导致业务把它写回给 setTextInputState → SetStyledText，shortcode 永久
    // 丢失（image span 被当成纯空格回写）。这里用基于 image_spans_ 权威映射的差分回写
    // 算法把 flat 还原成 raw —— 与 iOS 通过 NSTextStorage 自动维护 NSTextAttachment
    // 位置等价；详见 KRTextEditorCommon.h: ReconstructRawFromFlat / RebuildRawAfterUserEdit。
    auto new_flat = GetContentText();
    // 先读取变更后的 ArkUI flat selection/caret，再重建 raw。删除连续 image span 时，
    // selection 是区分"删第几个占位空格"的关键锚点。
    uint32_t fs = static_cast<uint32_t>(-1);
    uint32_t fe = static_cast<uint32_t>(-1);
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    if (state_.controller_) {
        if (OH_ArkUI_TextEditorStyledStringController_GetSelection(state_.controller_, &fs, &fe) !=
            ARKUI_ERROR_CODE_NO_ERROR) {
            fs = fe = static_cast<uint32_t>(-1);
        } else if (fs == fe) {
            int32_t caret = 0;
            if (OH_ArkUI_TextEditorStyledStringController_GetCaretOffset(
                    state_.controller_, &caret) == ARKUI_ERROR_CODE_NO_ERROR) {
                fs = fe = static_cast<uint32_t>(caret < 0 ? 0 : caret);
            }
        }
    }
#endif
    auto text = kuikly::text_editor::RebuildRawAfterUserEdit(state_, new_flat, fs, fe);
    state_.cached_text_ = text;
    bool any_callback_present =
        state_.text_did_change_callback_ || state_.text_input_state_change_callback_;
    if (state_.text_did_change_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(text);
        if (state_.length_limit_type_ != -1) {
            int length = kuikly::text_editor::CalculateRenderedTextLength(state_, text);
            map["length"] = NewKRRenderValue(length);
        }
        state_.text_did_change_callback_(NewKRRenderValue(map));
        state_.pending_text_did_change_ = false;
    } else {
        // 只要 textDidChange 尚未注册就打挂起标记，不考虑 textInputStateChange 是否有。
        state_.pending_text_did_change_ = true;
    }
    // 同一次文本变化中，textInputStateChange 与 textDidChange 并发，使业务侧可拿到完整选区。
    if (state_.text_input_state_change_callback_) {
        EmitTextInputStateChange();
        state_.pending_text_input_state_change_ = false;
    } else {
        // textInputStateChange 未注册，打挂起，事件注册路径补发。
        state_.pending_text_input_state_change_ = true;
    }
    (void)any_callback_present;
#endif
}

void KRTextEditorFieldView::OnInputFocus(ArkUI_NodeEvent *event) {
    (void)event;
    if (state_.input_focus_callback_) {
        KRRenderValueMap map;
        // 上抛 raw 而非 flat（与 textDidChange 一致），避免业务拿到带占位空格的字符串。
        map["text"] = NewKRRenderValue(state_.cached_text_);
        state_.input_focus_callback_(NewKRRenderValue(map));
    }
}

void KRTextEditorFieldView::OnInputBlur(ArkUI_NodeEvent *event) {
    (void)event;
    if (state_.input_blur_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(state_.cached_text_);
        state_.input_blur_callback_(NewKRRenderValue(map));
    }
}

void KRTextEditorFieldView::OnInputReturn(ArkUI_NodeEvent *event) {
    (void)event;
    if (state_.input_return_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(state_.cached_text_);
        map["ime_action"] =
            NewKRRenderValue(kuikly::util::ConvertEnterKeyTypeToString(state_.enter_key_type_));
        state_.input_return_callback_(NewKRRenderValue(map));
        if (state_.auto_hide_KeyBoard_on_ImeAction_) {
            Blur();
        }
    }
}

// ============================================================================
// textInputState 协议实现
// ============================================================================

void KRTextEditorFieldView::OnSelectionChanged(ArkUI_NodeEvent *event) {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    (void)event;
    // SetTextInputStateInternal 主动写入选区时由 guard 抑制，避免回环。
    if (state_.is_setting_text_input_state_) {
        return;
    }
    EmitSelectionChange();
#endif
}

void KRTextEditorFieldView::EmitTextInputStateChange() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    if (!state_.text_input_state_change_callback_) {
        return;
    }
    auto map = kuikly::text_editor::BuildTextInputStatePayload(state_);
    state_.text_input_state_change_callback_(NewKRRenderValue(map));
#endif
}

void KRTextEditorFieldView::EmitSelectionChange() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    if (!state_.selection_change_callback_) {
        return;
    }
    auto map = kuikly::text_editor::BuildTextInputStatePayload(state_);
    state_.selection_change_callback_(NewKRRenderValue(map));
#endif
}

void KRTextEditorFieldView::SetTextInputStateInternal(const std::string &json) {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    auto parsed = kuikly::text_editor::ParseTextInputStateJson(json);

    // guard：抑制 textDidChange / textInputStateChange / selectionChange 三个回调，
    // 与 Android isSettingTextInputState、iOS _ignoreTextDidChanged 同语义。业务侧
    // 通过 setTextInputState(...) 主动写入的状态变化不应再回流到业务层。
    state_.is_setting_text_input_state_ = true;

    // 1) 文本写入：与 iOS 相同——文本未变时跳过 SetStyledText，保留已有 span / typing 样式。
    bool text_changed = (parsed.text != state_.cached_text_);
    if (text_changed && state_.max_length_ >= 0 && state_.length_limit_type_ != -1 &&
        kuikly::text_editor::CalculateCandidateRenderedTextLength(state_.length_limit_type_, parsed.text) >
            state_.max_length_) {
        NotifyTextLengthBeyondLimit();
        state_.is_setting_text_input_state_ = false;
        EmitTextInputStateChange();
        return;
    }
    if (text_changed) {
        kuikly::text_editor::SetStyledText(state_, parsed.text);
        kuikly::text_editor::ApplyTypingStyle(state_);
        state_.cached_text_ = parsed.text;
    }

    // 2) 选区写入：业务侧给的 selection 是 raw 文本上的 UTF-16 偏移，必须先映射到
    //    ArkUI flat 上的 UTF-16 偏移（image span 在 raw 中占 utf16Len(raw_literal)，
    //    在 flat 中固定占 1 个 UTF-16 code unit），否则光标会落到 image 占位的"中间"，
    //    后续受控插入会切碎 shortcode 字面量。
    uint32_t flat_start =
        kuikly::text_editor::RawUtf16ToFlatUtf16(state_.image_spans_, parsed.selection_start);
    uint32_t flat_end =
        kuikly::text_editor::RawUtf16ToFlatUtf16(state_.image_spans_, parsed.selection_end);

    if (text_changed) {
        // ArkUI SDK 在 SetStyledString 之后会异步把 caret 重置到文本末尾（实测 readback
        // 与我们 set 的目标值不一致），导致同步调用的 SetSelection 被吞。把 SetSelection
        // 推迟到下一帧执行，绕过这次重置；同时下一帧重新挂上 guard，避免下一帧 SDK 触发
        // SelectionChanged 时回流业务层造成回环。
        KRMainThread::RunOnMainThreadForNextLoop(
            [weakSelf = weak_from_this(), flat_start, flat_end] {
                auto strongSelf =
                    std::dynamic_pointer_cast<KRTextEditorFieldView>(weakSelf.lock());
                if (!strongSelf || !strongSelf->state_.controller_) {
                    return;
                }
                strongSelf->state_.is_setting_text_input_state_ = true;
                // 纯光标（start == end）走 SetCaretOffset，路径更短、不触发 SetSelection 路径上
                // 潜在的 caret reset；非纯光标（真选区）才走 SetSelection。
                bool used_caret_api = (flat_start == flat_end);
                if (used_caret_api) {
                    kuikly::text_editor::SetCaretOffset(strongSelf->state_,
                                                        static_cast<int32_t>(flat_start));
                } else {
                    kuikly::text_editor::SetSelectionRange(strongSelf->state_, flat_start, flat_end);
                }
                strongSelf->state_.is_setting_text_input_state_ = false;
                if (strongSelf->state_.length_limit_type_ != -1) {
                    strongSelf->EmitTextInputStateChange();
                }
            });
    } else {
        // 文本未变路径：同步设 selection（已验证 SDK 不会重置 caret）。
        // 纯光标（start == end）走 SetCaretOffset，路径更短、不触发 SetSelection 路径上
        // 潜在的 caret reset；非纯光标（真选区）才走 SetSelection。
        bool used_caret_api = (flat_start == flat_end);
        if (used_caret_api) {
            kuikly::text_editor::SetCaretOffset(state_, static_cast<int32_t>(flat_start));
        } else {
            kuikly::text_editor::SetSelectionRange(state_, flat_start, flat_end);
        }
    }

    state_.is_setting_text_input_state_ = false;

    // 注意：不主动触发 textDidChange / textInputStateChange / selectionChange——
    // 调用方调用 setTextInputState 时已知道目标状态，无需再回调（与 Android 行为一致）。
#else
    (void)json;
#endif
}

void KRTextEditorFieldView::GetTextInputStateInternal(const KRRenderCallback &callback) {
    if (!callback) {
        return;
    }
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    auto map = kuikly::text_editor::BuildTextInputStatePayload(state_);
    callback(NewKRRenderValue(map));
#else
    callback(KREmptyValue());
#endif
}

void KRTextEditorFieldView::OnWillChangeText(ArkUI_NodeEvent *event) {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    OH_ArkUI_TextEditorChangeEvent *change_event =
        OH_ArkUI_NodeEvent_GetTextEditorOnWillChangeEvent(event);
    if (!change_event) {
        return;
    }

    // 单行模式：拦截换行符 —— 若待替换串中含 '\n'，拒绝（返回 0）
    if (InterceptNewline()) {
        // SDK 缺陷规避（详见 .ai/references/ohos-styledstring-descriptor-quirks.md）：
        // 不能用 `OH_ArkUI_StyledString_Descriptor_Create()`，那条路径会返回内部指针未初始化
        // 的 struct，Destroy 时会 free 野指针并崩溃。改用 `_CreateWithString` 走 SDK 正常初始化路径。
        // RAII 守卫消除原本两处手动 _Destroy 配对（任一 early return 都安全释放）。
        bool has_newline = false;
        if (kuikly::text_editor::EmptyStyledStringDescGuard g; g) {
            if (OH_ArkUI_TextEditorChangeEvent_GetReplacementStyledString(change_event, g.desc()) ==
                ARKUI_ERROR_CODE_NO_ERROR) {
                // 使用通用工具函数读取，避免 buffer 越界导致 Destroy 时 crash
                std::string buf = kuikly::text_editor::ReadDescriptorString(g.desc());
                if (buf.find('\n') != std::string::npos) {
                    has_newline = true;
                }
            }
            // g 在此 if 块结束时析构，自动 Destroy(desc) + Destroy(spanStyle)
        }
        if (has_newline) {
            ArkUI_NumberValue ret[] = {{.i32 = 0}};  // 拒绝
            OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
            return;
        }
    }

    uint32_t r_start = 0, r_end = 0;
    OH_ArkUI_TextEditorChangeEvent_GetRangeBefore(change_event, &r_start, &r_end);

    if (kuikly::text_editor::EmptyStyledStringDescGuard g; g) {
        if (OH_ArkUI_TextEditorChangeEvent_GetReplacementStyledString(change_event, g.desc()) ==
            ARKUI_ERROR_CODE_NO_ERROR) {
            std::string repl_str = kuikly::text_editor::ReadDescriptorString(g.desc());
            if (!repl_str.empty() && ContainsImageSpanAfterTextPostProcessor(repl_str)) {
                uint32_t raw_start = kuikly::text_editor::FlatUtf16ToRawUtf16(state_.image_spans_, r_start);
                uint32_t raw_end = kuikly::text_editor::FlatUtf16ToRawUtf16(state_.image_spans_, r_end);
                std::string candidate_raw = ReplaceUtf16Range(state_.cached_text_, raw_start, raw_end, repl_str);
                if (state_.max_length_ >= 0 && state_.length_limit_type_ != -1 &&
                    kuikly::text_editor::CalculateCandidateRenderedTextLength(
                        state_.length_limit_type_, candidate_raw) > state_.max_length_) {
                    NotifyTextLengthBeyondLimit();
                    ArkUI_NumberValue ret[] = {{.i32 = 0}};
                    OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
                    return;
                }

                uint32_t raw_cursor = raw_start +
                    static_cast<uint32_t>(kuikly::text_editor::GetUTF16Length(repl_str));
                state_.is_setting_text_input_state_ = true;
                kuikly::text_editor::SetStyledText(state_, candidate_raw);
                kuikly::text_editor::ApplyTypingStyle(state_);
                KRMainThread::RunOnMainThreadForNextLoop(
                    [weakSelf = weak_from_this(), candidate_raw, raw_cursor] {
                        auto strongSelf = std::dynamic_pointer_cast<KRTextEditorFieldView>(weakSelf.lock());
                        if (!strongSelf || !strongSelf->state_.controller_) {
                            return;
                        }
                        uint32_t flat_cursor = kuikly::text_editor::RawUtf16ToFlatUtf16(
                            strongSelf->state_.image_spans_, raw_cursor);
                        kuikly::text_editor::SetCaretOffset(strongSelf->state_, static_cast<int32_t>(flat_cursor));
                        strongSelf->state_.is_setting_text_input_state_ = false;
                        if (strongSelf->state_.text_did_change_callback_) {
                            KRRenderValueMap map;
                            map["text"] = NewKRRenderValue(candidate_raw);
                            if (strongSelf->state_.length_limit_type_ != -1) {
                                int length = kuikly::text_editor::CalculateRenderedTextLength(
                                    strongSelf->state_, candidate_raw);
                                map["length"] = NewKRRenderValue(length);
                            }
                            strongSelf->state_.text_did_change_callback_(NewKRRenderValue(map));
                            strongSelf->state_.pending_text_did_change_ = false;
                        } else {
                            strongSelf->state_.pending_text_did_change_ = true;
                        }
                        if (strongSelf->state_.text_input_state_change_callback_) {
                            strongSelf->EmitTextInputStateChange();
                            strongSelf->state_.pending_text_input_state_change_ = false;
                        } else {
                            strongSelf->state_.pending_text_input_state_change_ = true;
                        }
                    });
                ArkUI_NumberValue ret[] = {{.i32 = 0}};
                OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
                return;
            }
        }
    }

    // max-length 过滤（length_limit_type != -1 时手动过滤）
    if (state_.max_length_ != -1 && state_.length_limit_type_ != -1) {
        // SDK 缺陷规避（详见 .ai/references/ohos-styledstring-descriptor-quirks.md）：
        // 同上方注释——必须用 `_CreateWithString` 而非 `_Create()`，否则 Destroy 会崩。
        // RAII 守卫：原本 5 处手动 _Destroy 配对 + 1 处 early return 全部由作用域接管，
        // 任何分支退出都不可能漏 Destroy。
        bool reject = false;
        if (kuikly::text_editor::EmptyStyledStringDescGuard g; g) {
            if (OH_ArkUI_TextEditorChangeEvent_GetReplacementStyledString(change_event, g.desc()) ==
                ARKUI_ERROR_CODE_NO_ERROR) {
                std::string repl_str = kuikly::text_editor::ReadDescriptorString(g.desc());
                if (!repl_str.empty()) {
                    auto dest = GetContentText();
                    // 使用与老实现同语义的 filter：source 截断后通过拒绝方式落地
                    std::string tmp = repl_str;
                    tmp.push_back('\0');  // FilterSource 依赖 '\0' 结尾
                    bool changed = kuikly::text_editor::FilterSource(tmp.data(), dest, r_start,
                                                                       r_end, state_);
                    if (changed) {
                        NotifyTextLengthBeyondLimit();
                        if (tmp.empty() || tmp[0] == '\0') {
                            reject = true;  // 退出 if 块后再 return，让 g 正常析构
                        }
                        // 原生 API 无法在此替换插入文本；退而求其次：放行本次 ->
                        // 由 ON_DID_CHANGE 的 LimitInputContentTextInMaxLength 做后置截断。
                    }
                }
            }
        }
        if (reject) {
            ArkUI_NumberValue ret[] = {{.i32 = 0}};  // 拒绝
            OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
            return;
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
    // 此处不拦截；若粘贴文本含 shortcode 或触发 lengthLimitType，由 ON_WILL_CHANGE 的
    // GetReplacementStyledString 路径负责 raw 重建 / 截断（已在 OnWillChangeText 实现）。
    (void)event;
}

void KRTextEditorFieldView::OnCopyText(ArkUI_NodeEvent *event) {
    uint32_t flat_start = 0;
    uint32_t flat_end = 0;
    kuikly::text_editor::ReadSelection(state_, state_.cached_text_, &flat_start, &flat_end);
    if (flat_start == flat_end) {
        ArkUI_NumberValue ret[] = {{.i32 = 0}};
        OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
        return;
    }

    std::string original_raw = state_.cached_text_;
    std::vector<KRTextEditorState::KRImageSpanRecord> original_spans = state_.image_spans_;
    uint32_t raw_start = kuikly::text_editor::FlatUtf16ToRawUtf16(original_spans, flat_start);
    uint32_t raw_end = kuikly::text_editor::FlatUtf16ToRawUtf16(original_spans, flat_end);
    std::string selected_raw = ReplaceUtf16Range(original_raw, 0, raw_start, "");
    uint32_t selected_raw_u16_len = raw_end - raw_start;
    int selected_raw_bytes = kuikly::text_editor::GetUTF8ByteCount(selected_raw, 0, selected_raw_u16_len);
    selected_raw = selected_raw.substr(0, static_cast<size_t>(selected_raw_bytes));

    state_.is_setting_text_input_state_ = true;
    kuikly::text_editor::SetPlainStyledText(state_, selected_raw);
    kuikly::text_editor::SetSelectionRange(
        state_, 0, static_cast<uint32_t>(kuikly::text_editor::GetUTF16Length(selected_raw)));

    KRMainThread::RunOnMainThreadForNextLoop(
        [weakSelf = weak_from_this(), original_raw, original_spans, flat_start, flat_end] {
            auto strongSelf = std::dynamic_pointer_cast<KRTextEditorFieldView>(weakSelf.lock());
            if (!strongSelf || !strongSelf->state_.controller_) {
                return;
            }
            kuikly::text_editor::SetStyledText(strongSelf->state_, original_raw);
            strongSelf->state_.image_spans_ = original_spans;
            kuikly::text_editor::ApplyTypingStyle(strongSelf->state_);
            kuikly::text_editor::SetSelectionRange(strongSelf->state_, flat_start, flat_end);
            strongSelf->state_.is_setting_text_input_state_ = false;
        });

    ArkUI_NumberValue ret[] = {{.i32 = 0}};
    OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
}

// ============================================================================
// max-length 过滤辅助
// ============================================================================

bool KRTextEditorFieldView::LimitInputContentTextInMaxLength() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
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
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    if (state_.length_limit_type_ == -1 || state_.length_input_filter_) {
        return;
    }
    state_.length_input_filter_ = true;
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_WILL_CHANGE);
    RegisterEvent(ArkUI_NodeEventType::NODE_TEXT_EDITOR_ON_PASTE);
#endif
}

void KRTextEditorFieldView::DoResetMaxLength() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
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
    ArkUI_EnterKeyType ek = kuikly::util::ConvertToEnterKeyType(type);
    state_.enter_key_type_ = ek;
    kuikly::text_editor::UpdateEnterKeyType(GetNode(), ek);
}

#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE
