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

#include "libohos_render/expand/components/input/KRTextFieldView.h"

#include "libohos_render/manager/KRKeyboardManager.h"
#include <algorithm>
#include <arkui/drag_and_drop.h>
#include <cstddef>
#include <cstdint>
#include <deviceinfo.h>
#include <regex>

#ifdef __cplusplus
extern "C" {
#endif
// Remove this declaration if compatable api is raised to 15 and above
extern ArkUI_TextChangeEvent* OH_ArkUI_NodeEvent_GetTextChangeEvent(ArkUI_NodeEvent* event) __attribute__((weak));
#ifdef __cplusplus
}
#endif

constexpr char *kText = "text";
constexpr char *kPlaceholder = "placeholder";
constexpr char *kPlaceholderColor = "placeholderColor";
constexpr char *kFontSize = "fontSize";
constexpr char *kFontWeight = "fontWeight";
constexpr char *kColor = "color";
constexpr char *kEditable = "editable";
constexpr char *kTintColor = "tintColor";
constexpr char *kSelectionColor = "selectionColor";
constexpr char *kTextAlign = "textAlign";
constexpr char *kKeyboardType = "keyboardType";    // 键盘类型
constexpr char *kReturnKeyType = "returnKeyType";  // return类型
constexpr char *kMaxTextLength = "maxTextLength";  // 最大长度
constexpr char *kLengthLimitType = "lengthLimitType"; // 长度限制类型
constexpr char *kAutoHideKeyBoardOnIMEAction = "autoHideKeyboardOnImeAction"; // 是否自动隐藏键盘
constexpr char kPropTextInputState[] = "textInputState"; // 受控组件模式，与 Kotlin InputView/TextAreaView TEXT_INPUT_STATE 一致

constexpr char kMethodFocus[] = "focus";
constexpr char kMethodBlur[] = "blur";
constexpr char kMethodSetText[] = "setText";
constexpr char kMethodGetCursorIndex[] = "getCursorIndex";
constexpr char kMethodSetCursorIndex[] = "setCursorIndex";
constexpr char kMethodSetTextInputState[] = "setTextInputState";
constexpr char kMethodGetTextInputState[] = "getTextInputState";

constexpr char kEventTextDidChanged[] = "textDidChange";
constexpr char kEventInputFocus[] = "inputFocus";
constexpr char kEventInputBlur[] = "inputBlur";
constexpr char kEventInputReturn[] = "inputReturn";
constexpr char kEventTextLengthBeyondLimit[] = "textLengthBeyondLimit";
constexpr char kEventKeyboardHeightChange[] = "keyboardHeightChange";  // 键盘高度变化
constexpr char kEventTextInputStateChange[] = "textInputStateChange"; // 与 Kotlin InputView/TextAreaView TEXT_INPUT_STATE_CHANGE 一致
constexpr char kEventSelectionChange[] = "selectionChange"; // 与 Kotlin InputView.kt:426 / TextAreaView.kt:685 一致

// textInputState JSON 协议字段名，跨端一致（参考 core/views/TextInputState.kt）
constexpr char kKeyText[] = "text";
constexpr char kKeySelectionStart[] = "selectionStart";
constexpr char kKeySelectionEnd[] = "selectionEnd";
constexpr char kKeyCompositionStart[] = "compositionStart";
constexpr char kKeyCompositionEnd[] = "compositionEnd";
constexpr char kKeyLength[] = "length";
constexpr int kNoComposition = -1;

ArkUI_NodeHandle KRTextFieldView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT_INPUT);
}

void KRTextFieldView::DidInit() {
    kuikly::util::UpdateNodeBackgroundColor(GetNode(), 0);                          // 默认背景色为透明
    kuikly::util::UpdateNodeBorderRadius(GetNode(), KRBorderRadiuses(0, 0, 0, 0));  // 系统默认有圆角，此处应默认无圆角
    kuikly::util::SetArkUIPadding(GetNode(), 0, 0, 0, 0);                           // 系统默认有padding，此处应默认无padding
    SetFont(font_size_, font_weight_);
    RegisterEvent(GetOnChangeEventType());
    
    // 默认软键盘不关闭
    ArkUI_NumberValue value = {.i32 = 0};
    ArkUI_AttributeItem item = {&value, 1};
    kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_INPUT_BLUR_ON_SUBMIT, &item);
    
    
}

void KRTextFieldView::OnDestroy() {
    if (keyboard_height_changed_callback_) {
        auto key = NewKRRenderValue(GetViewTag())->toString();
        if (auto root = GetRootView().lock()) {
            auto window_id = root->GetContext()->WindowId();
            KRKeyboardManager::GetInstance().RemoveKeyboardTask(window_id, key);
        }
    }
    keyboard_height_changed_callback_ = nullptr;
}

void KRTextFieldView::UpdateInputNodePlaceholder(const std::string& propValue){
    kuikly::util::UpdateInputNodePlaceholder(GetNode(), propValue);
}

void KRTextFieldView::UpdateInputNodePlaceholderColor(const std::string& propValue){
    kuikly::util::UpdateInputNodePlaceholderColor(GetNode(), kuikly::util::ConvertToHexColor(propValue));
}

void KRTextFieldView::UpdateInputNodeColor(const std::string& propValue){
    kuikly::util::UpdateInputNodeColor(GetNode(), kuikly::util::ConvertToHexColor(propValue));
}
void KRTextFieldView::UpdateInputNodeCaretrColor(const std::string& propValue){
    kuikly::util::UpdateInputNodeCaretrColor(GetNode(), kuikly::util::ConvertToHexColor(propValue));
}
void KRTextFieldView::UpdateInputNodeSelectionColor(const std::string& propValue){
    kuikly::util::UpdateInputNodeSelectionColor(GetNode(), kuikly::util::ConvertToHexColor(propValue));
}
void KRTextFieldView::UpdateInputNodeTextAlign(const std::string& propValue){
    kuikly::util::UpdateInputNodeTextAlign(GetNode(), propValue);
}
void KRTextFieldView::UpdateInputNodeFocusable(int propValue){
    kuikly::util::UpdateInputNodeFocusable(GetNode(), propValue);
}
void KRTextFieldView::UpdateInputNodeKeyboardType(const std::string& propValue){
    kuikly::util::UpdateInputNodeKeyboardType(GetNode(), kuikly::util::ConvertToInputType(propValue));
}
void KRTextFieldView::UpdateInputNodeEnterKeyType(const std::string& propValue){
    kuikly::util::UpdateInputNodeEnterKeyType(GetNode(), kuikly::util::ConvertToEnterKeyType(propValue));
}
void KRTextFieldView::UpdateInputNodeMaxLength(int maxLength){
    kuikly::util::UpdateInputNodeMaxLength(GetNode(), maxLength);  // 直接限制
}
void KRTextFieldView::UpdateInputNodeFocusStatus(int status){
    kuikly::util::UpdateInputNodeFocusStatus(GetNode(), status);
}
uint32_t KRTextFieldView::GetInputNodeSelectionStartPosition(){
    return kuikly::util::GetInputNodeSelectionStartPosition(GetNode());
}

void KRTextFieldView::UpdateInputNodeSelectionStartPosition(uint32_t index){
    kuikly::util::UpdateInputNodeSelectionStartPosition(GetNode(), index);
}

void KRTextFieldView::UpdateInputNodePlaceholderFont(uint32_t font_size, ArkUI_FontWeight font_weight){
    const auto &rootView = GetRootView().lock();
    bool fontSizeScaleFollowSystem = true;
    float font_size_px = 0;
    if (rootView) {
        fontSizeScaleFollowSystem = rootView->GetContext()->Config()->GetFontSizeScaleFollowSystem();
        font_size_px = rootView->GetContext()->Config()->fp2px(font_size);
    }
    kuikly::util::UpdateInputNodePlaceholderFont(GetNode(), font_size, font_weight, fontSizeScaleFollowSystem, font_size_px);
}

void KRTextFieldView::UpdateInputNodeContentText(const std::string &text){
    kuikly::util::UpdateInputNodeContentText(GetNode(), text);
}
std::string KRTextFieldView::GetInputNodeContentText(){
    return kuikly::util::GetInputNodeContentText(GetNode());
}
bool KRTextFieldView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                              const KRRenderCallback event_call_back) {
    if (kuikly::util::isEqual(prop_key, kText)) {  // 占位
        SetContentText(prop_value->toString());
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kPlaceholder)) {  // 占位
        UpdateInputNodePlaceholder(prop_value->toString());
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kPlaceholderColor)) {  // 占位颜色
        UpdateInputNodePlaceholderColor(prop_value->toString());
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kFontSize)) {  // 字体大小
        font_size_ = prop_value->toFloat();
        SetFont(font_size_, font_weight_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kFontWeight)) {  // 字重
        float scale = 1.0;
        if (auto root = GetRootView().lock()) {
            scale = root->GetContext()->Config()->GetFontWeightScale();
        }
        font_weight_ = kuikly::util::ConvertArkUIFontWeight(prop_value->toInt(), scale);
        SetFont(font_size_, font_weight_);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kColor)) {  // 字体颜色
        UpdateInputNodeColor(prop_value->toString());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kTintColor)) {  // 光标颜色
        UpdateInputNodeCaretrColor(prop_value->toString());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kSelectionColor)) {  // 选中高亮颜色
        UpdateInputNodeSelectionColor(prop_value->toString());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kTextAlign)) {  // 文本对齐
        UpdateInputNodeTextAlign(prop_value->toString());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEditable)) {  // 是否可以编辑输入
        focusable_ = prop_value->toBool();
        UpdateInputNodeFocusable(prop_value->toInt());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kKeyboardType)) {  // 键盘输入类型
        UpdateInputNodeKeyboardType(prop_value->toString());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kReturnKeyType)) {  // 完成键类型
        UpdateInputNodeEnterKeyType(prop_value->toString());
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kLengthLimitType)) { // 长度限制类型
        length_limit_type_ = prop_value->toInt();
        if (max_length_ != -1) {
            DoResetMaxLength();
            LimitInputContentTextInMaxLength();
            SetupLengthInputFilter();
        }
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kMaxTextLength)) {  // 输入长度限制
        max_length_ = prop_value->toInt();
        DoResetMaxLength();
        LimitInputContentTextInMaxLength();
        SetupLengthInputFilter();
        return true;
    }
    
    if (kuikly::util::isEqual(prop_key, kAutoHideKeyBoardOnIMEAction)) {  // 输入长度限制
        auto_hide_KeyBoard_on_ImeAction_ = prop_value->toBool();
        return true;
    }

    // 受控组件模式：prop 形式写入 textInputState（与 InputView.kt:58 / TextAreaView.kt:116 等价）
    if (kuikly::util::isEqual(prop_key, kPropTextInputState)) {
        SetTextInputStateInternal(prop_value->toString());
        return true;
    }

    // 事件
    if (kuikly::util::isEqual(prop_key, kEventTextDidChanged)) {  // 文本变化事件
        text_did_change_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_ON_FOCUS);
        RegisterEvent(ArkUI_NodeEventType::NODE_ON_BLUR);
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventInputFocus)) {  // 获焦事件
        input_focus_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_ON_FOCUS);
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventInputBlur)) {  // 失焦事件
        input_blur_callback_ = event_call_back;
        RegisterEvent(ArkUI_NodeEventType::NODE_ON_BLUR);
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEventInputReturn)) {  // 按下完成键回调事件
        input_return_callback_ = event_call_back;
        RegisterEvent(GetOnSubmitEventType());
        return true;
    }
    if (kuikly::util::isEqual(prop_key, kEventTextLengthBeyondLimit)) {  // 监听文字是否超过输入最大的限制事件
        text_length_beyond_limit_callback_ = event_call_back;
        DoResetMaxLength();
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventKeyboardHeightChange)) {  // 监听文字是否超过输入最大的限制事件
        keyboard_height_changed_callback_ = event_call_back;
        auto key = NewKRRenderValue(GetViewTag())->toString();
        if (auto root = GetRootView().lock()) {
            auto window_id = root->GetContext()->WindowId();
            KRKeyboardManager::GetInstance().AddKeyboardTask(window_id, key, [event_call_back](float height, int duration_ms) {
                KRRenderValueMap map;
                map["height"] = NewKRRenderValue(height);
                map["duration"] = NewKRRenderValue(duration_ms / 1000.0);
                event_call_back(NewKRRenderValue(map));
            });
        }
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventTextInputStateChange)) {  // textInputState 变化事件
        text_input_state_change_callback_ = event_call_back;
        // 复用 OnChange 事件源（默认已 RegisterEvent），无需额外注册
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventSelectionChange)) {  // 选区变化事件
        selection_change_callback_ = event_call_back;
        // ArkUI 老节点原生事件：单行走 NODE_TEXT_INPUT_ON_TEXT_SELECTION_CHANGE，
        // 多行由子类 override 为 NODE_TEXT_AREA_ON_TEXT_SELECTION_CHANGE。
        RegisterEvent(GetOnTextSelectionChangeEventType());
        return true;
    }

    return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
}

void KRTextFieldView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type == GetOnChangeEventType()) {
        OnTextDidChanged(event);  // 文本变化
    } else if (event_type == ArkUI_NodeEventType::NODE_ON_FOCUS) {
        OnInputFocus(event);  // 获焦
    } else if (event_type == ArkUI_NodeEventType::NODE_ON_BLUR) {
        OnInputBlur(event);  // 失焦
    } else if (event_type == GetOnSubmitEventType()) {
        OnInputReturn(event);  // 按下返回键
    } else if (event_type == GetOnWillInsertEventType()) {
        OnWillInsertText(event);
    } else if (event_type == GetOnPasteEventType()) {
        OnPasteText(event);
    } else if (event_type == GetOnWillChangeEventType()) {
        OnWillChangeText(event);
    } else if (event_type == GetOnTextSelectionChangeEventType()) {
        OnTextSelectionChange(event);  // 选区变化
    } else if (event_type == ArkUI_NodeEventType::NODE_ON_DRAG_ENTER) {
        drag_entered_ = true;
    } else if (event_type == ArkUI_NodeEventType::NODE_ON_DRAG_LEAVE) {
        drag_entered_ = false;
    }
}

void KRTextFieldView::CallMethod(const std::string &method, const KRAnyValue &params,
                                 const KRRenderCallback &callback) {
    if (kuikly::util::isEqual(method, kMethodFocus)) {  // 获焦
        Focus();
    } else if (kuikly::util::isEqual(method, kMethodBlur)) {  // 失焦
        Blur();
    } else if (kuikly::util::isEqual(method, kMethodSetText)) {  // 主动设置文本
        SetContentText(params->toString());
    } else if (kuikly::util::isEqual(method, kMethodGetCursorIndex)) {  // 获取光标位置
        GetCursorIndex(callback);
    } else if (kuikly::util::isEqual(method, kMethodSetCursorIndex)) {  // 设置光标位置
        SetCursorIndex(params->toInt());
    } else if (kuikly::util::isEqual(method, kMethodSetTextInputState)) {  // 受控写入 textInputState
        SetTextInputStateInternal(params->toString());
    } else if (kuikly::util::isEqual(method, kMethodGetTextInputState)) {  // 读取当前 textInputState
        GetTextInputStateInternal(callback);
    } else {
        IKRRenderViewExport::CallMethod(method, params, callback);
    }
}

/**
 * 输入框获焦（弹起键盘）
 */
void KRTextFieldView::Focus() {
    UpdateInputNodeFocusStatus(1);
}

/**
 * 输入框失焦（收起键盘）
 */
void KRTextFieldView::Blur() {
    UpdateInputNodeFocusStatus(0);
}

/**
 * 获取光标位置
 */
void KRTextFieldView::GetCursorIndex(const KRRenderCallback &callback) {
    int32_t selectionLeft = GetInputNodeSelectionStartPosition();
    if (callback) {
        KRRenderValueMap map;
        map["cursorIndex"] = NewKRRenderValue(selectionLeft);
        callback(NewKRRenderValue(map));
    }
}

/**
 * 设置光标位置
 */
void KRTextFieldView::SetCursorIndex(uint32_t index) {
    UpdateInputNodeSelectionStartPosition(index);
}

/**
 * 获取选区范围 [start, end]（按 UTF-16 算）。
 * Base 实现走 NODE_TEXT_INPUT_TEXT_SELECTION；KRTextAreaView 会 override 用 NODE_TEXT_AREA_TEXT_SELECTION。
 */
std::pair<uint32_t, uint32_t> KRTextFieldView::GetInputNodeTextSelectionRange() {
    return kuikly::util::GetInputNodeSelectionRange(GetNode());
}

/**
 * 受控写入 textInputState：解析 JSON 并把 text/光标 写入 ArkUI 节点。
 *
 * 跨端语义参考 Android KRTextFieldView.setTextInputState：
 *   - 仅消费 text / selectionStart / selectionEnd 三字段；
 *   - composition 不消费；
 *
 * ⚠️ OHOS 老节点能力局限：selection 范围写入降级为「只把光标设到 selectionStart」。
 * TODO：后续如有真选区需求，可改用 NODE_TEXT_INPUT_TEXT_SELECTION / NODE_TEXT_AREA_TEXT_SELECTION
 *       的 [start,end] 写入。Q1 已先接受降级。
 */
void KRTextFieldView::SetTextInputStateInternal(const std::string &json) {
    // KRRenderValue::toMap 内部调 cJSON_Parse 解析 JSON 字符串到 Map；解析失败回空 Map。
    KRRenderValue::Map parsed = NewKRRenderValue(json)->toMap();

    auto get_string = [&](const char *key) -> std::string {
        auto it = parsed.find(key);
        if (it == parsed.end() || it->second == nullptr) {
            return "";
        }
        return it->second->toString();
    };
    auto get_int = [&](const char *key, int default_value) -> int {
        auto it = parsed.find(key);
        if (it == parsed.end() || it->second == nullptr) {
            return default_value;
        }
        return it->second->toInt();
    };

    std::string text = get_string(kKeyText);
    if (ShouldRejectProgrammaticShortcodeInput(text)) {
        NotifyTextLengthBeyondLimit();
        NotifyTextInputStateChange();
        return;
    }

    // selection 用 UTF-16 长度做 clamp，与 Android 行为一致。
    int u16_len = GetUTF16Length(text);
    int selection_start = get_int(kKeySelectionStart, u16_len);
    selection_start = std::max(0, std::min(selection_start, u16_len));
    // selection_end 解析但当前降级为不使用（Q1 TODO）；预留以便日后实现真选区。
    int selection_end = get_int(kKeySelectionEnd, selection_start);
    (void)selection_end;

    is_setting_text_input_state_ = true;
    SetContentText(text);

    // ⚠️ ArkUI NODE_TEXT_INPUT_TEXT/NODE_TEXT_AREA_TEXT 的 setAttribute 会在内部异步触发
    // onChange，并把光标重置到文本末尾。如果在这里同步调用 UpdateInputNodeSelectionStartPosition，
    // 会被随后到来的 ArkUI 内部 caret reset 吞掉，表现为「光标永远跳到末尾」。
    // 解决：把光标修正 post 到 next-loop，等 ArkUI 内部 onChange 完成后再设选区，
    // 与 KRTextEditorFieldView 中 RunOnMainThreadForNextLoop 的策略一致，也与 LimitInputContentTextInMaxLength
    // 中已有的「先改文本后异步设光标」pattern 一致。
    // 同时 is_setting_text_input_state_ flag 也延迟到此处清除，以覆盖 SetContentText 异步触发
    // OnTextDidChanged 的整个时窗，避免业务把"末尾光标"的脏 textInputStateChange 写回来形成回环。
    KRMainThread::RunOnMainThreadForNextLoop(
        [weakSelf = weak_from_this(), selection_start]() {
            if (auto strongSelf = std::dynamic_pointer_cast<KRTextFieldView>(weakSelf.lock())) {
                strongSelf->UpdateInputNodeSelectionStartPosition(static_cast<uint32_t>(selection_start));
                strongSelf->is_setting_text_input_state_ = false;
                if (strongSelf->length_limit_type_ != -1) {
                    strongSelf->NotifyTextInputStateChange();
                }
            }
        });
    // 调用方调用 setTextInputState 时已知道目标状态，但 length 依赖原生计算，需在限长模式下回传。
}

/**
 * 拼装当前 textInputState 出参 map，与 Android createTextInputStateParamMap 对齐：
 *   - 始终回 {text, selectionStart, selectionEnd, compositionStart=-1, compositionEnd=-1}；
 *   - 仅当 length_limit_type_ != -1 时附带 length。
 */
KRRenderValueMap KRTextFieldView::CreateTextInputStateMap() {
    auto text = GetContentText();
    auto range = GetInputNodeTextSelectionRange();
    int u16_len = GetUTF16Length(text);
    int selection_start = std::min<int>(range.first, u16_len);
    int selection_end = std::min<int>(range.second, u16_len);
    selection_start = std::max(0, selection_start);
    selection_end = std::max(selection_start, selection_end);

    KRRenderValueMap map;
    map[kKeyText] = NewKRRenderValue(text);
    map[kKeySelectionStart] = NewKRRenderValue(selection_start);
    map[kKeySelectionEnd] = NewKRRenderValue(selection_end);
    map[kKeyCompositionStart] = NewKRRenderValue(kNoComposition);
    map[kKeyCompositionEnd] = NewKRRenderValue(kNoComposition);
    if (length_limit_type_ != -1) {
        int length = CalculateTextLength(text);
        map[kKeyLength] = NewKRRenderValue(length);
    }
    return map;
}

/**
 * getTextInputState method 路径：把当前 state 通过 callback 回吐给业务。
 */
void KRTextFieldView::GetTextInputStateInternal(const KRRenderCallback &callback) {
    if (callback) {
        callback(NewKRRenderValue(CreateTextInputStateMap()));
    }
}

/**
 * 在 OnTextDidChanged 末尾按需触发 textInputStateChange。
 * 主动写入期间通过 is_setting_text_input_state_ 抑制，避免业务死循环。
 */
void KRTextFieldView::NotifyTextInputStateChange() {
    if (is_setting_text_input_state_) {
        return;
    }
    if (!text_input_state_change_callback_) {
        return;
    }
    text_input_state_change_callback_(NewKRRenderValue(CreateTextInputStateMap()));
}

/**
 * 选区变化事件回调（与 Android KRTextFieldView.onSelectionChanged 对齐）。
 * 出参与 textInputStateChange 完全一致（CreateTextInputStateMap）：
 *   {text, selectionStart, selectionEnd, compositionStart=-1, compositionEnd=-1, [length]}
 * 主动写入期间通过 is_setting_text_input_state_ 抑制。
 */
void KRTextFieldView::NotifySelectionChange() {
    if (is_setting_text_input_state_) {
        return;
    }
    if (!selection_change_callback_) {
        return;
    }
    selection_change_callback_(NewKRRenderValue(CreateTextInputStateMap()));
}

/**
 * 处理 ArkUI 原生选区变化事件（NODE_TEXT_INPUT_ON_TEXT_SELECTION_CHANGE /
 * NODE_TEXT_AREA_ON_TEXT_SELECTION_CHANGE）。
 *
 * 事件参数：
 *   - data[0].i32 = start position（UTF-16）
 *   - data[1].i32 = end position（UTF-16）
 *
 * 注意：当前并不直接消费 event 参数（CreateTextInputStateMap 内部会通过
 * GetInputNodeTextSelectionRange 重新拉取最新选区），这样可以保证多次回调间的
 * 一致性，并避免事件参数与节点属性短暂不一致带来的奇怪状态。事件参数仅作为
 * 「触发信号」存在；如果未来发现 attribute 读取与事件值不一致带来体感问题，
 * 可以改为优先使用 event 参数构造 map。
 *
 * 我们同时触发 selectionChange 与 textInputStateChange，与 Compose `CoreTextField`
 * 业务侧期望的「选区变化即可拿到完整 state」语义对齐。
 */
void KRTextFieldView::OnTextSelectionChange(ArkUI_NodeEvent *event) {
    NotifySelectionChange();
    NotifyTextInputStateChange();
}

/**
 * 设置字体（包括占位字体）
 * @param font_size
 * @param font_weight
 */
void KRTextFieldView::SetFont(uint32_t font_size, ArkUI_FontWeight font_weight) {
    UpdateInputNodePlaceholderFont(font_size, font_weight);
}

/******* 事件回调 ******/

void KRTextFieldView::OnTextDidChanged(ArkUI_NodeEvent *event) {
    if (length_limit_type_ == -1) { // 兼容旧逻辑
        LimitInputContentTextInMaxLength();
    } else if (max_length_ != -1 && (drag_entered_ || OH_GetSdkApiVersion() < 20)) {
        // 处理拖拽场景和低版本api不支持ON_WILL_CHANGE场景
        LimitInputContentTextInMaxLength();
        drag_entered_ = false;
    }
    if (text_did_change_callback_) {
        auto text = GetContentText();
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(text);
        if (length_limit_type_ != -1) {
            int length = CalculateTextLength(text);
            map["length"] = NewKRRenderValue(length);
            // KR_LOG_DEBUG << "OnTextDidChanged: text=" << text << ", length=" << length;
        }
        text_did_change_callback_(NewKRRenderValue(map));
    }
    // 同一时机触发 textInputStateChange（与 Android KRTextFieldView 一致）。
    // 主动写入期间由 NotifyTextInputStateChange 内部抑制，避免业务回流。
    NotifyTextInputStateChange();
}

/**
 * 获焦回调
 */
void KRTextFieldView::OnInputFocus(ArkUI_NodeEvent *event) {
    if (input_focus_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(GetContentText());
        input_focus_callback_(NewKRRenderValue(map));
    }
}
/**
 * 失焦回调
 */
void KRTextFieldView::OnInputBlur(ArkUI_NodeEvent *event) {
    if (input_blur_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(GetContentText());
        input_blur_callback_(NewKRRenderValue(map));
    }
}
/**
 * 按下完成键回调
 */
void KRTextFieldView::OnInputReturn(ArkUI_NodeEvent *event) {
    if (input_return_callback_) {
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(GetContentText());
        auto returnKeyType = kuikly::util::GetInputNodeEnterKeyType(GetNode());
        map["ime_action"] = NewKRRenderValue(kuikly::util::ConvertEnterKeyTypeToString(returnKeyType));
        input_return_callback_(NewKRRenderValue(map));
        
        // 强制关闭软键盘
        if (auto_hide_KeyBoard_on_ImeAction_) {
            UpdateInputNodeFocusStatus(0);
        } 
    }
}
/***
 * 获取输入的文本内容
 */
std::string KRTextFieldView::GetContentText() {
    return GetInputNodeContentText();
}
/***
 * 设置输入的文本内容
 */
void KRTextFieldView::SetContentText(const std::string &text) {
    UpdateInputNodeContentText(text);
}

bool KRTextFieldView::ShouldRejectProgrammaticShortcodeInput(const std::string &text) {
    if (length_limit_type_ < 0 || max_length_ <= 0) {
        return false;
    }

    static const std::regex shortcode_regex(R"(\[[a-zA-Z0-9_\-]+\])");
    if (!std::regex_search(text, shortcode_regex)) {
        return false;
    }

    return CalculateTextLength(text) > max_length_;
}

bool KRTextFieldView::LimitInputContentTextInMaxLength() {
    if (max_length_ < 0) {
        return false;
    }
    if (length_limit_type_ == -1) { // 兼容旧逻辑
        auto text = kuikly::util::ConvertToU32String(GetContentText());
        if (text.length() > max_length_) {
            text = text.substr(0, max_length_);
            SetContentText(kuikly::util::ConvertToNormalString(text)); // 假设你有一个SetContentText函数来设置文本内容
            NotifyTextLengthBeyondLimit();
            return true;
        }
        return false;
    } else {
        auto destText = GetContentText();
        auto cursorIndex = GetInputNodeSelectionStartPosition();
        if (CalculateTextLength(destText) > max_length_) {
            KR_LOG_DEBUG << "InputContent beyond limit";
            NotifyTextLengthBeyondLimit();
            int u8position = GetUTF8ByteCount(destText, 0, cursorIndex);
            char *buffer = new char[u8position + 1];
            strncpy(buffer, destText.c_str(), u8position);
            buffer[u8position] = '\0';
            filter(buffer, destText, 0, cursorIndex);
            std::string bufferStr(buffer);
            SetContentText(bufferStr + destText.substr(u8position));
            KRMainThread::RunOnMainThread([weakSelf = weak_from_this(), pos = GetUTF16Length(bufferStr)] {
                if (auto strongSelf = std::dynamic_pointer_cast<KRTextFieldView>(weakSelf.lock())) {
                    strongSelf->SetCursorIndex(pos);
                }
            });
            delete[] buffer;
            return true;
        }
        return false;
    }
}

void KRTextFieldView::NotifyTextLengthBeyondLimit() {
    // KR_LOG_DEBUG << "NotifyTextLengthBeyondLimit";
    if (text_length_beyond_limit_callback_) {
        KRRenderValueMap map;
        text_length_beyond_limit_callback_(NewKRRenderValue(map));
    }
}

void KRTextFieldView::SetupLengthInputFilter() {
    if (length_limit_type_ == -1 || length_input_filter_) {
        return;
    }
    length_input_filter_ = true;
    RegisterEvent(GetOnWillInsertEventType());
    RegisterEvent(GetOnPasteEventType());
    RegisterEvent(GetOnWillChangeEventType());
    RegisterEvent(ArkUI_NodeEventType::NODE_ON_DRAG_ENTER);
    RegisterEvent(ArkUI_NodeEventType::NODE_ON_DRAG_LEAVE);
}

bool KRTextFieldView::filter(char source[], const std::string &dest, const size_t dStart, const size_t dEnd) {
    if (source[0] == '\0') {
        // quick return for deletion
        return false;
    }
    int32_t keep = max_length_ - CalculateTextLength(dest, dStart, dEnd);
    if (keep >= CalculateTextLength(source)) {
        return false; // keep original
    } else if (keep <= 0) {
        // need to block
        source[0] = '\0';
        return true;
    } else {
        // need to truncate
        auto index = CalculateTruncateIndex(source, keep);
        source[index] = '\0';
        return true;
    }
}

void KRTextFieldView::DoResetMaxLength() {
    if (max_length_ == -1) {
        kuikly::util::ResetInputNodeMaxLength(GetNode());
    } else {
        if (length_limit_type_ == -1) { // 兼容旧逻辑
            if (text_length_beyond_limit_callback_) {
                UpdateInputNodeMaxLength(10000000);  // 不限制，通过LimitInputContentTextInMaxLength
            } else {
                UpdateInputNodeMaxLength(max_length_);  // 直接限制
            }
        } else {
            // UpdateInputNodeMaxLength按UTF-16长度限制，把max_length_转换为最大UTF-16长度
            size_t limit = max_length_;
            if (length_limit_type_ == 1) { // CHARACTER
                limit *= 2; // 最坏情况每个字符占2个UTF-16单位
            }
            if (text_length_beyond_limit_callback_) {
                limit += 2; // 增加1个字符（最多2个UTF-16单位）限制，确保达到限制后仍能触发回调
            }
            UpdateInputNodeMaxLength(limit);
        }
    }
}

void KRTextFieldView::DelayResetMaxLength() {
    KRMainThread::RunOnMainThread(
        [weakSelf = weak_from_this()] {
            if (auto strongSelf = std::dynamic_pointer_cast<KRTextFieldView>(weakSelf.lock())) {
                strongSelf->DoResetMaxLength();
            }
        },
        // 经验值，不能设太小，否则不能截断粘贴长度超限制（tested api 20）
        100);
}

void KRTextFieldView::OnWillChangeText(ArkUI_NodeEvent *event) {
    if (max_length_ != -1 && length_limit_type_ != -1 && !drag_entered_) {
        // 处理英文输入法切换候选词触发长度限制（API >= 20，排除拖拽场景）
        LimitInputContentTextInMaxLength();
    }
}

constexpr size_t MAX_INSERT_LENGTH = 256;
void KRTextFieldView::OnWillInsertText(ArkUI_NodeEvent *event) {
    if (max_length_ != -1 && length_limit_type_ != -1) {
        // 处理键盘输入文本触发长度限制
        // KR_LOG_DEBUG << "OnWillInsertText: max_length=" << max_length_ << ", limit_type=" << length_limit_type_;
        char buffer[MAX_INSERT_LENGTH] = "";
        int32_t size = MAX_INSERT_LENGTH;
        char *pBuffer = buffer;
        OH_ArkUI_NodeEvent_GetStringValue(event, 0, &pBuffer, &size);
        // KR_LOG_DEBUG << "OnWillInsertText: to insert text: " << buffer;
        auto destText = GetContentText();
        auto range = kuikly::util::GetInputNodeSelectionRange(GetNode());
        bool filtered = filter(buffer, destText, range.first, range.second);
        if (filtered || strlen(buffer) >= MAX_INSERT_LENGTH - 1) {
            if (filtered) {
                KR_LOG_DEBUG << "OnWillInsertText beyond limit";
                // 超过最大输入长度限制
                NotifyTextLengthBeyondLimit();
                if (buffer[0] == '\0') {
                    // 阻止插入
                    ArkUI_NumberValue ret[] = {false};
                    OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
                    return;
                }
            }
            uint32_t realMaxLength = GetUTF16Length(destText) - (range.second - range.first) + GetUTF16Length(buffer);
            UpdateInputNodeMaxLength(realMaxLength);
            DelayResetMaxLength();
        }
    }
    // 允许插入
    ArkUI_NumberValue ret[] = {true};
    OH_ArkUI_NodeEvent_SetReturnNumberValue(event, ret, 1);
}

void KRTextFieldView::OnPasteText(ArkUI_NodeEvent *event) {
    if (max_length_ != -1 && length_limit_type_ != -1) {
        // 处理粘贴文本触发长度限制
        // KR_LOG_DEBUG << "OnPasteText: max_length=" << max_length_ << ", limit_type=" << length_limit_type_;
        auto stringAsyncEvent = OH_ArkUI_NodeEvent_GetStringAsyncEvent(event);
        if (!stringAsyncEvent || !stringAsyncEvent->pStr) {
            return;
        }
        size_t size = std::min(strlen(stringAsyncEvent->pStr), static_cast<size_t>(max_length_) * 4);
        char *buffer = new char[size + 1];
        strncpy(buffer, stringAsyncEvent->pStr, size);
        buffer[size] = '\0';
        auto destText = GetContentText();
        auto range = kuikly::util::GetInputNodeSelectionRange(GetNode());
        if (filter(buffer, destText, range.first, range.second)) {
            KR_LOG_DEBUG << "OnPasteText beyond limit";
            // 超过最大输入长度限制
            NotifyTextLengthBeyondLimit();
            uint32_t realMaxLength = GetUTF16Length(destText) - (range.second - range.first) + GetUTF16Length(buffer);
            UpdateInputNodeMaxLength(realMaxLength);
            DelayResetMaxLength();
        }
        delete[] buffer;
    }
}

int KRTextFieldView::GetUTF8ByteLengthOfFirstCharacter(unsigned char c) {
    if ((c & 0x80) == 0) {
        return 1; // 1-byte character
    } else if ((c & 0xE0) == 0xC0) {
        return 2; // 2-byte character
    } else if ((c & 0xF0) == 0xE0) {
        return 3; // 3-byte character
    } else /*if ((c & 0xF8) == 0xF0)*/ {
        return 4; // 4-byte character
    }
}

int KRTextFieldView::GetUTF8ByteLengthOfCodePoint(char32_t codePoint) {
    if (codePoint <= 0x7F) {
        return 1;
    } else if (codePoint <= 0x7FF) {
        return 2;
    } else if (codePoint <= 0xFFFF) {
        return 3;
    } else {
        return 4;
    }
}

int KRTextFieldView::GetVisualWidthOfCodePoint(char32_t codePoint) {
    // ASCII字符
    if (codePoint < 128) {
        return 1;
    }

    if (codePoint >= 0x200B && codePoint <= 0x200D) { // 零宽字符
        return 1;
    }
    if (codePoint == 0xFEFF) { // 零宽不换行空格
        return 1;
    }

    // 其他字符（中文、emoji等）返回2
    return 2;
}

int KRTextFieldView::CalculateTextLength(const std::string &text, size_t rmStart, size_t rmEnd) {
    switch (length_limit_type_) {
    case 0: { // BYTE
        auto size = text.length();
        if (rmEnd > rmStart) {
            auto byteCountToStart = GetUTF8ByteCount(text, 0, rmStart);
            auto byteCountToEnd = GetUTF8ByteCount(text, byteCountToStart, rmEnd - rmStart);
            size -= byteCountToEnd;
        }
        return size;
    }
    case 1: { // CHARACTER
        // 转换为 u32string 后计算字符数（code point）
        auto u32text = kuikly::util::ConvertToU32String(text);
        auto size = u32text.length();
        if (rmEnd > rmStart) {
            size_t u32Index = 0;
            size_t u16Index = 0;
            while (u16Index < rmStart && u32Index < size) {
                u16Index += (u32text[u32Index] > 0xFFFF) ? 2 : 1;
                u32Index++;
            }
            auto u32Start = u32Index;
            while (u16Index < rmEnd && u32Index < size) {
                u16Index += (u32text[u32Index] > 0xFFFF) ? 2 : 1;
                u32Index++;
            }
            size -= (u32Index - u32Start);
        }
        return size;
    }
    case 2: { // VISUAL_WIDTH
        auto u32text = kuikly::util::ConvertToU32String(text);
        auto size = u32text.length();
        int visualWidth = 0;
        size_t u16Index = 0;
        for (size_t i = 0; i < size; ++i) {
            char32_t codePoint = u32text[i];
            if (u16Index < rmStart || u16Index >= rmEnd) {
                visualWidth += GetVisualWidthOfCodePoint(codePoint);
            }
            u16Index += (codePoint > 0xFFFF) ? 2 : 1;
        }
        return visualWidth;
    }
    default:
        return 0;
    }
}

int KRTextFieldView::CalculateTruncateIndex(const std::string &text, size_t keep) {
    switch (length_limit_type_) {
    case 0: { // BYTE
        size_t textLength = text.length();
        size_t byteIndex = 0;
        while (byteIndex < textLength) {
            unsigned char c = static_cast<unsigned char>(text[byteIndex]);
            int pointBytes = GetUTF8ByteLengthOfFirstCharacter(c);
            if (byteIndex + pointBytes > keep) {
                break;
            }
            byteIndex += pointBytes;
        }
        return byteIndex;
    }
    case 1: { // CHARACTER
        size_t textLength = text.length();
        size_t byteIndex = 0;
        for (size_t i = 0; i < keep && byteIndex < textLength; ++i) {
            unsigned char c = static_cast<unsigned char>(text[byteIndex]);
            byteIndex += GetUTF8ByteLengthOfFirstCharacter(c);
        }
        return byteIndex;
    }
    case 2: { // VISUAL_WIDTH
        auto u32text = kuikly::util::ConvertToU32String(text);
        size_t u32Length = u32text.length();
        size_t visualWidth = 0;
        size_t byteIndex = 0;
        for (size_t i = 0; i < u32Length; ++i) {
            auto u32Char = u32text[i];
            int charWidth = GetVisualWidthOfCodePoint(u32Char);
            if (visualWidth + charWidth > keep) {
                break;
            }
            visualWidth += charWidth;
            byteIndex += GetUTF8ByteLengthOfCodePoint(u32Char);
        }
        return byteIndex;
    }
    default:
        return 0;
    }
}

int KRTextFieldView::GetUTF8ByteCount(const std::string &text, size_t u8Start, size_t u16Count) {
    size_t textLength = text.length();
    size_t u8Index = u8Start;
    size_t u16Index = 0;
    while (u16Index < u16Count && u8Index < textLength) {
        unsigned char c = static_cast<unsigned char>(text[u8Index]);
        int byteLength = GetUTF8ByteLengthOfFirstCharacter(c);
        u8Index += byteLength;
        u16Index += byteLength >= 4 ? 2 : 1; // UTF-16中，4字节的UTF-8字符占2个code unit
    }
    return u8Index - u8Start;
}

int KRTextFieldView::GetUTF16Length(const std::string &text) {
    size_t textLength = text.length();
    size_t u8Index = 0;
    size_t u16Index = 0;
    while (u8Index < textLength) {
        unsigned char c = static_cast<unsigned char>(text[u8Index]);
        int byteLength = GetUTF8ByteLengthOfFirstCharacter(c);
        u8Index += byteLength;
        u16Index += byteLength >= 4 ? 2 : 1; // UTF-16中，4字节的UTF-8字符占2个code unit
    }
    return u16Index;
}
