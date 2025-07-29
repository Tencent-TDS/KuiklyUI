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

#include "libohos_render/expand/components/view/KRView.h"

#include "libohos_render/manager/KRSnapshotManager.h"

#define NS_PER_MS 1000000

constexpr char kPropNameTouchDown[] = "touchDown";
constexpr char kPropNameTouchMove[] = "touchMove";
constexpr char kPropNameTouchUp[] = "touchUp";
constexpr char kPropNameTouchCancel[] = "touchCancel";
constexpr char kPropNamePreventTouch[] = "preventTouch";
constexpr char kPropNameSuperTouch[] = "superTouch";
constexpr char kPropNameHitTestModeOhos[] = "hit-test-ohos";

constexpr char kOhosHitTestModeDefault[] = "default";
constexpr char kOhosHitTestModeBlock[] = "block";
constexpr char kOhosHitTestModeNone[] = "none";
constexpr char kOhosHitTestModeTransparent[] = "transparent";


bool KRView::ReuseEnable() {
    return true;
}

void KRView::WillReuse() {
    UpdateHitTestMode(true);
}

bool KRView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                     const KRRenderCallback event_call_back) {
    auto didHand = false;
    if (kuikly::util::isEqual(prop_key, kPropNameTouchDown)) {
        didHand = RegisterTouchDownEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kPropNameTouchMove)) {
        didHand = RegisterTouchMoveEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kPropNameTouchUp)) {
        didHand = RegisterTouchUpEvent(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kPropNamePreventTouch)) {
        if (super_touch_handler_) {
            super_touch_handler_->PreventTouch(prop_value->toBool());
        }
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameSuperTouch)) {
        if (prop_value->toBool()) {
            if (!super_touch_handler_) {
                super_touch_handler_ = std::make_shared<SuperTouchHandler>();
            }
        } else {
            if (super_touch_handler_) {
                super_touch_handler_ = nullptr;
            }
        }
        didHand = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameHitTestModeOhos)) {
        didHand = SetTargetHitTestMode(prop_value->toString());
    }
    return didHand;
}

void KRView::DidSetProp(const std::string &prop_key) {
    UpdateHitTestMode(HasBaseEvent() || HasTouchEvent());
}

void KRView::CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &cb) {
    IKRRenderViewExport::CallMethod(method, params, cb);
}

void KRView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type == NODE_TOUCH_EVENT) {
        ProcessTouchEvent(event);
    }
}

bool KRView::ResetProp(const std::string &prop_key) {
    auto didHande = false;
    register_touch_event_ = false;
    if (kuikly::util::isEqual(prop_key, kPropNameTouchDown)) {
        touch_down_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameTouchMove)) {
        touch_move_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameTouchUp)) {
        touch_up_callback_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNamePreventTouch)) {
        // reset handled by kPropNameSuperTouch, do nothing here
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameSuperTouch)) {
        super_touch_handler_ = nullptr;
        didHande = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameHitTestModeOhos)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_DEFAULT;
        UpdateHitTestMode(HasBaseEvent() || HasTouchEvent());
        didHande = true;
    } else {
        didHande = IKRRenderViewExport::ResetProp(prop_key);
    }
    return didHande;
}

void KRView::ProcessTouchEvent(ArkUI_NodeEvent *event) {
    auto input_event = kuikly::util::GetArkUIInputEvent(event);
    TryFireSuperTouchCancelEvent(input_event)
    auto action = kuikly::util::GetArkUIInputEventAction(input_event);
    if (action == UI_TOUCH_EVENT_ACTION_DOWN) {
        TryFireOnTouchDownEvent(input_event);
    } else if (action == UI_TOUCH_EVENT_ACTION_MOVE) {
        TryFireOnTouchMoveEvent(input_event);
    } else if (action == UI_TOUCH_EVENT_ACTION_UP) {
        TryFireOnTouchUpEvent(input_event);
    } else if (action == UI_TOUCH_EVENT_ACTION_CANCEL) {
        TryFireOnTouchCancelEvent(input_event);
    }
}

void KRView::EnsureRegisterTouchEvent() {
    if (register_touch_event_) {
        return;
    }

    RegisterEvent(NODE_TOUCH_EVENT);
    register_touch_event_ = true;
}

bool KRView::RegisterTouchDownEvent(const KRRenderCallback &event_call_back) {
    EnsureRegisterTouchEvent();
    touch_down_callback_ = event_call_back;
    return true;
}

bool KRView::RegisterTouchMoveEvent(const KRRenderCallback &event_call_back) {
    EnsureRegisterTouchEvent();
    touch_move_callback_ = event_call_back;
    return true;
}

bool KRView::RegisterTouchUpEvent(const KRRenderCallback &event_call_back) {
    EnsureRegisterTouchEvent();
    touch_up_callback_ = event_call_back;
    return true;
}

bool KRView::SetTargetHitTestMode(const std::string &mode) {
    if (kuikly::util::isEqual(mode, kOhosHitTestModeBlock)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_BLOCK;
    } else if (kuikly::util::isEqual(mode, kOhosHitTestModeNone)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_NONE;
    } else if (kuikly::util::isEqual(mode, kOhosHitTestModeTransparent)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_TRANSPARENT;
    } else if (kuikly::util::isEqual(mode, kOhosHitTestModeDefault)) {
        target_hit_test_mode = ARKUI_HIT_TEST_MODE_DEFAULT;
    }
    return true;
}

void KRView::TryFireOnTouchDownEvent(ArkUI_UIInputEvent *input_event) {
    if (!touch_down_callback_) {
        return;
    }
    touch_down_callback_(GenerateBaseParamsWithTouch(input_event, kPropNameTouchDown));
}

void KRView::TryFireOnTouchMoveEvent(ArkUI_UIInputEvent *input_event) {
    if (!touch_move_callback_) {
        return;
    }
    touch_move_callback_(GenerateBaseParamsWithTouch(input_event, kPropNameTouchMove));
}

void KRView::TryFireOnTouchUpEvent(ArkUI_UIInputEvent *input_event) {
    if (!touch_up_callback_) {
        return;
    }
    touch_up_callback_(GenerateBaseParamsWithTouch(input_event, kPropNameTouchUp));
}

void KRView::TryFireOnTouchCancelEvent(ArkUI_UIInputEvent *input_event) {
    if (!touch_up_callback_) {
        return;
    }
    touch_up_callback_(GenerateBaseParamsWithTouch(input_event, kPropNameTouchCancel));
}

bool KRView::TryFireSuperTouchCancelEvent(ArkUI_UIInputEvent *input_event) {
    if (!super_touch_handler_) {
        return false;
    }
    auto action = kuikly::util::GetArkUIInputEventAction(input_event);
    auto pointer_count = kuikly::util::GetArkUIInputEventPointerCount(input_event);
    if (action == UI_TOUCH_EVENT_ACTION_DOWN && pointer_count == 1) {
        super_touch_handler_->ResetCancel();
        return false;
    }
    bool canceled = false;
    if (super_touch_handler_->IsCanceled()) {
        canceled = true;
    } else if (super_touch_handler_->ProcessCancel()) {
        canceled = true;
    }
    if ((action == UI_TOUCH_EVENT_ACTION_UP && pointer_count == 1) || action == UI_TOUCH_EVENT_ACTION_CANCEL) {
        super_touch_handler_->ResetCancel();
    }
    return canceled;
}

KRAnyValue KRView::GenerateBaseParamsWithTouch(ArkUI_UIInputEvent *input_event, const std::string &action) {
    if (!input_event) {
        return KREmptyValue();
    }

    auto pointer_count = kuikly::util::GetArkUIInputEventPointerCount(input_event);
    if (pointer_count <= 0) {
        return KREmptyValue();
    }

    KRRenderValueArray touches;
    for (int i = 0; i < pointer_count; i++) {
        auto point = kuikly::util::GetArkUIInputEventPoint(input_event, i);
        auto window_point = kuikly::util::GetArkUIInputEventWindowPoint(input_event, i);
        KRRenderValueMap touch_map;
        touch_map["x"] = NewKRRenderValue(point.x);
        touch_map["y"] = NewKRRenderValue(point.y);
        touch_map["pageX"] = NewKRRenderValue(window_point.x);
        touch_map["pageY"] = NewKRRenderValue(window_point.y);
        touch_map["pointerId"] = NewKRRenderValue(OH_ArkUI_PointerEvent_GetPointerId(input_event, i));
        touches.push_back(NewKRRenderValue(touch_map));
    }
    auto first_touch = touches[0]->toMap();
    first_touch["touches"] = NewKRRenderValue(touches);
    first_touch["action"] = NewKRRenderValue(action);
    auto event_time_millis = kuikly::util::GetArkUIInputEventTime(input_event) / NS_PER_MS;
    first_touch["timestamp"] = NewKRRenderValue(event_time_millis);
    if (super_touch_handler_) {
        first_touch["consumed"] = NewKTRenderValue(super_touch_handler_->IsCanceled() ? 1 : 0);
    }
    return NewKRRenderValue(first_touch);
}

bool KRView::HasTouchEvent() {
    return touch_up_callback_ != nullptr || touch_down_callback_ != nullptr || touch_move_callback_ != nullptr;
}

void KRView::UpdateHitTestMode(bool shouldUseTarget) {
    if (using_target_hit_test_mode != (shouldUseTarget ? 1 : 0)) {
        using_target_hit_test_mode = shouldUseTarget;
        kuikly::util::UpdateNodeHitTestMode(GetNode(), shouldUseTarget ? target_hit_test_mode : ARKUI_HIT_TEST_MODE_NONE);
    }
}
