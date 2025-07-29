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

#include "libohos_render/expand/events/KRBaseEventHandler.h"

#include <arkui/native_node.h>
#include "libohos_render/expand/events/KREventDispatchCenter.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRStringUtil.h"

constexpr char kClickEventName[] = "click";
constexpr char kDoubleClickEventName[] = "doubleClick";
constexpr char kLongPressEventName[] = "longPress";
constexpr char kPanEventName[] = "pan";
constexpr char kPinchEventName[] = "pinch";
constexpr char kCaptureAttrName[] = "capture";

constexpr char kParamKeyX[] = "x";
constexpr char kParamKeyY[] = "y";
constexpr char kParamKeyPageX[] = "pageX";
constexpr char kParamKeyPageY[] = "pageY";
constexpr char kParamKeyState[] = "state";
constexpr char kStartState[] = "start";
constexpr char kEndState[] = "end";
constexpr char kParamKeyScale[] = "scale";

KRBaseEventHandler::KRBaseEventHandler(const std::shared_ptr<KRConfig> &kr_config) : kr_config_(kr_config) {}

bool KRBaseEventHandler::SetProp(const std::shared_ptr<IKRRenderViewExport> &view_export, const std::string &prop_key,
                                 const KRAnyValue &prop_value, const KRRenderCallback event_call_back) {
    auto didHanded = false;
    if (event_call_back != nullptr) {
        if (kuikly::util::isEqual(prop_key, kClickEventName)) {
            didHanded = RegisterOnClick(view_export, event_call_back);
        } else if (kuikly::util::isEqual(prop_key, kDoubleClickEventName)) {
            didHanded = RegisterOnDoubleClick(view_export, event_call_back);
        } else if (kuikly::util::isEqual(prop_key, kLongPressEventName)) {
            didHanded = RegisterOnLongPress(view_export, event_call_back);
        } else if (kuikly::util::isEqual(prop_key, kPanEventName)) {
            didHanded = RegisterOnPan(view_export, event_call_back);
        } else if (kuikly::util::isEqual(prop_key, kPinchEventName)) {
            didHanded = RegisterOnPinch(view_export, event_call_back);
        }
    } else if (kuikly::util::isEqual(prop_key, kCaptureAttrName)) {
        didHanded = SetCaptureRule(view_export, prop_value->toString());
    }
    return didHanded;
}

bool KRBaseEventHandler::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    return false;
}

bool KRBaseEventHandler::OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) {
    return false;
}

bool KRBaseEventHandler::OnGestureEvent(const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                                        const KRGestureEventType &event_type) {
    auto didHanded = false;
    if (event_type == KRGestureEventType::kClick) {
        didHanded = FireOnClickCallback(gesture_event_data);
    } else if (event_type == KRGestureEventType::kDoubleClick) {
        didHanded = FireOnDoubleClickCallback(gesture_event_data);
    } else if (event_type == KRGestureEventType::kLongPress) {
        didHanded = FireOnLongPressCallback(gesture_event_data);
    } else if (event_type == KRGestureEventType::kPan) {
        didHanded = FireOnPanCallback(gesture_event_data);
    } else if (event_type == KRGestureEventType::kPinch) {
        didHanded = FireOnPinchCallback(gesture_event_data);
    }
    return didHanded;
}

bool KRBaseEventHandler::ResetProp(const std::string &prop_key) {
    auto didHanded = false;
    if (kuikly::util::isEqual(prop_key, kClickEventName)) {
        click_callback_ = nullptr;
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kDoubleClickEventName)) {
        double_click_callback_ = nullptr;
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kLongPressEventName)) {
        long_press_callback_ = nullptr;
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kPanEventName)) {
        pan_event_callback_ = nullptr;
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kPinchEventName)) {
        pinch_event_callback_ = nullptr;
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kCaptureAttrName)) {
        // KREventDispatchCenter has reset by view_export->UnregisterEvent()
        has_capture_rule_ = false;
        didHanded = true;
    }
    return didHanded;
}

void KRBaseEventHandler::OnDestroy() {
    click_callback_ = nullptr;
    double_click_callback_ = nullptr;
    long_press_callback_ = nullptr;
    pan_event_callback_ = nullptr;
    pinch_event_callback_ = nullptr;
}

bool KRBaseEventHandler::RegisterOnClick(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                         const KRRenderCallback &event_callback) {
    click_callback_ = event_callback;
    KREventDispatchCenter::GetInstance().RegisterGestureEvent(view_export, KRGestureEventType::kClick);
    return true;
}

bool KRBaseEventHandler::FireOnClickCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data) {
    if (!click_callback_) {
        return false;
    }

    KRRenderValueMap params;
    params[kParamKeyX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.x));
    params[kParamKeyY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.y));
    params[kParamKeyPageX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.x));
    params[kParamKeyPageY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.y));
    click_callback_(NewKRRenderValue(params));
    return true;
}

bool KRBaseEventHandler::RegisterOnDoubleClick(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                               const KRRenderCallback &event_callback) {
    double_click_callback_ = event_callback;
    KREventDispatchCenter::GetInstance().RegisterGestureEvent(view_export, KRGestureEventType::kDoubleClick);
    return true;
}

bool KRBaseEventHandler::FireOnDoubleClickCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data) {
    if (!double_click_callback_) {
        return false;
    }

    KRRenderValueMap params;
    params[kParamKeyX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.x));
    params[kParamKeyY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.y));
    params[kParamKeyPageX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.x));
    params[kParamKeyPageY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.y));
    double_click_callback_(NewKRRenderValue(params));
    return true;
}

bool KRBaseEventHandler::RegisterOnLongPress(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                             const KRRenderCallback &event_callback) {
    long_press_callback_ = event_callback;
    KREventDispatchCenter::GetInstance().RegisterGestureEvent(view_export, KRGestureEventType::kLongPress);
    return true;
}

bool KRBaseEventHandler::FireOnLongPressCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data) {
    if (!long_press_callback_) {
        return false;
    }
    std::string state = kuikly::util::GetArkUIGestureActionState(gesture_event_data->gesture_event_);
    if ((is_long_press_happening && state == kStartState) || (!is_long_press_happening && state == kEndState)) {
        return false;
    }
    is_long_press_happening = (state == kEndState) ? false : true;
    KRRenderValueMap params;
    params[kParamKeyX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.x));
    params[kParamKeyY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.y));
    params[kParamKeyPageX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.x));
    params[kParamKeyPageY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.y));
    params[kParamKeyState] = NewKRRenderValue(kuikly::util::GetArkUIGestureActionState(gesture_event_data->gesture_event_));
    long_press_callback_(NewKRRenderValue(params));
    return true;
}

bool KRBaseEventHandler::RegisterOnPan(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                       const KRRenderCallback &event_callback) {
    pan_event_callback_ = event_callback;
    KREventDispatchCenter::GetInstance().RegisterGestureEvent(view_export, KRGestureEventType::kPan);
    return true;
}

bool KRBaseEventHandler::FireOnPanCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data) {
    if (!pan_event_callback_) {
        return false;
    }

    KRRenderValueMap params;
    params[kParamKeyX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.x));
    params[kParamKeyY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.y));
    params[kParamKeyPageX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.x));
    params[kParamKeyPageY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.y));
    params[kParamKeyState] = NewKRRenderValue(kuikly::util::GetArkUIGestureActionState(gesture_event_data->gesture_event_));
    pan_event_callback_(NewKRRenderValue(params));
    return true;
}

bool KRBaseEventHandler::RegisterOnPinch(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                         const KRRenderCallback &event_callback) {
    pinch_event_callback_ = event_callback;
    KREventDispatchCenter::GetInstance().RegisterGestureEvent(view_export, KRGestureEventType::kPinch);
    return true;
}

bool KRBaseEventHandler::FireOnPinchCallback(const std::shared_ptr<KRGestureEventData> &gesture_event_data) {
    if (!pinch_event_callback_) {
        return false;
    }

    KRRenderValueMap params;
    params[kParamKeyX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.x));
    params[kParamKeyY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_point_.y));
    params[kParamKeyPageX] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.x));
    params[kParamKeyPageY] = NewKRRenderValue(kr_config_->Px2Vp(gesture_event_data->gesture_event_window_point_.y));
    params[kParamKeyScale] = NewKRRenderValue(kuikly::util::GetArkUIGesturePinchScale(gesture_event_data->gesture_event_));
    params[kParamKeyState] = NewKRRenderValue(kuikly::util::GetArkUIGestureActionState(gesture_event_data->gesture_event_));
    pinch_event_callback_(NewKRRenderValue(params));
    return true;
}

bool KRBaseEventHandler::HasTouchEvent() {
    return click_callback_ != nullptr || double_click_callback_ != nullptr || pan_event_callback_ != nullptr ||
           long_press_callback_ != nullptr;
}

bool KRBaseEventHandler::SetCaptureRule(const std::shared_ptr<IKRRenderViewExport> &view_export,
                                        const std::string &rule_data) {
    auto rules = KRGestureCaptureRule::Parse(rule_data);
    has_capture_rule_ = !rules.empty();
    if (has_capture_rule_) {
        view_export->SetupTouchInterrupter();
    }
    KREventDispatchCenter::GetInstance().RegisterGestureInterrupter(view_export);
    KREventDispatchCenter::GetInstance().SetCaptureRule(view_export, std::move(rules));
    return true;
}

bool KRBaseEventHandler::HasCaptureRule() {
    return has_capture_rule_;
}