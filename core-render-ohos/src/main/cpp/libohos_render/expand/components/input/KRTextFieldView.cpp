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

constexpr char *kText = "text";
constexpr char *kPlaceholder = "placeholder";
constexpr char *kPlaceholderColor = "placeholderColor";
constexpr char *kFontSize = "fontSize";
constexpr char *kFontWeight = "fontWeight";
constexpr char *kColor = "color";
constexpr char *kEditable = "editable";
constexpr char *kTintColor = "tintColor";
constexpr char *kTextAlign = "textAlign";
constexpr char *kKeyboardType = "keyboardType";    // 键盘类型
constexpr char *kReturnKeyType = "returnKeyType";  // return类型
constexpr char *kMaxTextLength = "maxTextLength";  // 最大长度

constexpr char kMethodFocus[] = "focus";
constexpr char kMethodBlur[] = "blur";
constexpr char kMethodSetText[] = "setText";
constexpr char kMethodGetCursorIndex[] = "getCursorIndex";
constexpr char kMethodSetCursorIndex[] = "setCursorIndex";

constexpr char kEventTextDidChanged[] = "textDidChange";
constexpr char kEventInputFocus[] = "inputFocus";
constexpr char kEventInputBlur[] = "inputBlur";
constexpr char kEventInputReturn[] = "inputReturn";
constexpr char kEventTextLengthBeyondLimit[] = "textLengthBeyondLimit";
constexpr char kEventKeyboardHeightChange[] = "keyboardHeightChange";  // 键盘高度变化

ArkUI_NodeHandle KRTextFieldView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT_INPUT);
}

void KRTextFieldView::DidInit() {
    kuikly::util::UpdateNodeBackgroundColor(GetNode(), 0);                          // 默认背景色为透明
    kuikly::util::UpdateNodeBorderRadius(GetNode(), KRBorderRadiuses(0, 0, 0, 0));  // 系统默认有圆角，此处应默认无圆角
    kuikly::util::SetArkUIPadding(GetNode(), 0, 0, 0, 0);                           // 系统默认有padding，此处应默认无padding
    SetFont(font_size_, font_weight_);
    RegisterEvent(GetOnChangeEventType());
}

void KRTextFieldView::OnDestroy() {
    if (keyboard_height_changed_callback_) {
        auto key = NewKRRenderValue(GetViewTag())->toString();
        KRKeyboardManager::GetInstance().RemoveKeyboardTask(key);
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
    kuikly::util::UpdateInputNodePlaceholderFont(GetNode(), font_size, font_weight);
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
        font_weight_ = kuikly::util::ConvertArkUIFontWeight(prop_value->toInt());
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

    if (kuikly::util::isEqual(prop_key, kMaxTextLength)) {  // 输入长度限制
        max_length_ = prop_value->toInt();
        LimitInputContentTextInMaxLength();
        if (!text_length_beyond_limit_callback_) {
            UpdateInputNodeMaxLength(max_length_);  // 直接限制
        }
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
        UpdateInputNodeMaxLength(10000000);  // 不限制，通过LimitInputContentTextInMaxLength
        return true;
    }

    if (kuikly::util::isEqual(prop_key, kEventKeyboardHeightChange)) {  // 监听文字是否超过输入最大的限制事件
        keyboard_height_changed_callback_ = event_call_back;
        auto key = NewKRRenderValue(GetViewTag())->toString();
        KRKeyboardManager::GetInstance().AddKeyboardTask(key, [event_call_back](float height, int duration_ms) {
            KRRenderValueMap map;
            map["height"] = NewKRRenderValue(height);
            map["duration"] = NewKRRenderValue(duration_ms / 1000.0);
            event_call_back(NewKRRenderValue(map));
        });
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
 * 设置字体（包括占位字体）
 * @param font_size
 * @param font_weight
 */
void KRTextFieldView::SetFont(uint32_t font_size, ArkUI_FontWeight font_weight) {
    UpdateInputNodePlaceholderFont(font_size, font_weight);
    
}

/******* 事件回调 ******/

void KRTextFieldView::OnTextDidChanged(ArkUI_NodeEvent *event) {
    LimitInputContentTextInMaxLength();
    if (text_did_change_callback_) {
        auto text = GetContentText();
        KRRenderValueMap map;
        map["text"] = NewKRRenderValue(text);
        text_did_change_callback_(NewKRRenderValue(map));
    }
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
        input_return_callback_(NewKRRenderValue(map));
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

bool KRTextFieldView::LimitInputContentTextInMaxLength() {
    if (max_length_ < 0) {
        return false;
    }
    auto text = kuikly::util::ConvertToU32String(GetContentText());
    if (text.length() > max_length_) {
        text = text.substr(0, max_length_);
        SetContentText(kuikly::util::ConvertToNormalString(text));  // 假设你有一个SetContentText函数来设置文本内容
        if (text_length_beyond_limit_callback_) {
            KRRenderValueMap map;
            text_length_beyond_limit_callback_(NewKRRenderValue(map));
        }
        return true;
    }
    return false;
}
