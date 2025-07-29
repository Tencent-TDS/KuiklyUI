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

#include "libohos_render/expand/events/gesture/KRGestureEventHandler.h"

#include "libohos_render/scheduler/KRUIScheduler.h"
#include "libohos_render/utils/KREventUtil.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/manager/KRWeakObjectManager.h"

KRGestureEventHandler::KRGestureEventHandler(ArkUI_NodeHandle node_handle, ArkUI_GestureRecognizer *gesture_group,
                                             KRGestureEventCallback gesture_callback)
    : node_(node_handle), gesture_group_(gesture_group), gesture_callback_(gesture_callback),
      event_type_(KRGestureEventType::kUnknown) {}

KRGestureEventHandler::~KRGestureEventHandler() {
    auto gesture_api = kuikly::util::GetGestureApi();
    if (gesture_group_ == nullptr || gesture_recognizer_ == nullptr) {
        return;
    }
    gesture_api->removeChildGesture(gesture_group_, gesture_recognizer_);
    gesture_api->dispose(gesture_recognizer_);
    gesture_recognizer_ = nullptr;
    gesture_group_ = nullptr;
}

const bool KRGestureEventHandler::MineEvent(const KRGestureEventType &event_type) const {
    return event_type_ == event_type;
}

void KRGestureEventHandler::OnGestureEvent(ArkUI_GestureEvent *event) {
    gesture_callback_(node_, std::make_shared<KRGestureEventData>(event), event_type_);
}

static void OnReceiveGestureEvent(ArkUI_GestureEvent *event, void *extraParams) {
    auto weakHandler = KRWeakObjectManagerGetWeakObject<KRGestureEventHandler>(extraParams);
    if(auto strongHandler = weakHandler.lock()){
        strongHandler->OnGestureEvent(event);
    }
}

KRPanGestureEventHandler::KRPanGestureEventHandler(ArkUI_NodeHandle node_handle, ArkUI_GestureRecognizer *gesture_group,
                                                   KRGestureEventCallback gesture_callback)
    : KRGestureEventHandler(node_handle, gesture_group, gesture_callback) {}

bool KRPanGestureEventHandler::RegisterEvent(const KRGestureEventType &event_type) {
    if (gesture_recognizer_) {
        return false;
    }

    event_type_ = KRGestureEventType::kPan;
    auto gesture_api = kuikly::util::GetGestureApi();
    gesture_recognizer_ = gesture_api->createPanGesture(1, GESTURE_DIRECTION_ALL, 5);
    void* userData = KRWeakObjectManagerRegisterWeakObject<>(shared_from_this());
    
    gesture_api->setGestureEventTarget(
        gesture_recognizer_, GESTURE_EVENT_ACTION_ACCEPT | GESTURE_EVENT_ACTION_UPDATE | GESTURE_EVENT_ACTION_END, userData,
        OnReceiveGestureEvent);
    gesture_api->addChildGesture(gesture_group_, gesture_recognizer_);
    return true;
}

KRPinchGestureEventHandler::KRPinchGestureEventHandler(ArkUI_NodeHandle node_handle,
                                                       ArkUI_GestureRecognizer *gesture_group,
                                                       KRGestureEventCallback gesture_callback)
    : KRGestureEventHandler(node_handle, gesture_group, gesture_callback) {}

bool KRPinchGestureEventHandler::RegisterEvent(const KRGestureEventType &event_type) {
    if (gesture_recognizer_) {
        return false;
    }

    event_type_ = KRGestureEventType::kPinch;
    auto gesture_api = kuikly::util::GetGestureApi();
    gesture_recognizer_ = gesture_api->createPinchGesture(2, 3);
    void* userData = KRWeakObjectManagerRegisterWeakObject<>(shared_from_this());
    gesture_api->setGestureEventTarget(
        gesture_recognizer_, GESTURE_EVENT_ACTION_ACCEPT | GESTURE_EVENT_ACTION_UPDATE | GESTURE_EVENT_ACTION_END, userData,
        OnReceiveGestureEvent);
    gesture_api->addChildGesture(gesture_group_, gesture_recognizer_);
    return true;
}

KRLongPressGestureEventHandler::KRLongPressGestureEventHandler(ArkUI_NodeHandle node_handle,
                                                               ArkUI_GestureRecognizer *gesture_group,
                                                               KRGestureEventCallback gesture_callback)
    : KRGestureEventHandler(node_handle, gesture_group, gesture_callback) {}

bool KRLongPressGestureEventHandler::RegisterEvent(const KRGestureEventType &event_type) {
    if (gesture_recognizer_) {
        return false;
    }

    event_type_ = KRGestureEventType::kLongPress;
    auto gesture_api = kuikly::util::GetGestureApi();
    gesture_recognizer_ = gesture_api->createGroupGesture(ArkUI_GroupGestureMode::PARALLEL_GROUP);

    // 由长按和滑动构成的组合手势
    auto sequential_gesture_group = gesture_api->createGroupGesture(ArkUI_GroupGestureMode::SEQUENTIAL_GROUP);
    auto long_press_gesture = gesture_api->createLongPressGesture(1, false, 250);
    void* userData = KRWeakObjectManagerRegisterWeakObject(shared_from_this());
    gesture_api->setGestureEventTarget(
        long_press_gesture, GESTURE_EVENT_ACTION_ACCEPT | GESTURE_EVENT_ACTION_UPDATE | GESTURE_EVENT_ACTION_END, userData,
        OnReceiveGestureEvent);
    gesture_api->addChildGesture(sequential_gesture_group, long_press_gesture);
    auto pan_gesture = gesture_api->createPanGesture(1, GESTURE_DIRECTION_ALL, 1);
    gesture_api->setGestureEventTarget(
        pan_gesture, GESTURE_EVENT_ACTION_ACCEPT | GESTURE_EVENT_ACTION_UPDATE | GESTURE_EVENT_ACTION_END, userData,
        OnReceiveGestureEvent);
    gesture_api->addChildGesture(sequential_gesture_group, pan_gesture);

    // LongPress同时识别长按和长按加滑动的组合手势
    gesture_api->addChildGesture(gesture_recognizer_, sequential_gesture_group);
    gesture_api->addChildGesture(gesture_recognizer_, long_press_gesture);
    gesture_api->addChildGesture(gesture_group_, gesture_recognizer_);
    return true;
}

KRTapGestureEventHandler::KRTapGestureEventHandler(ArkUI_NodeHandle node_handle, ArkUI_GestureRecognizer *gesture_group,
                                                   KRGestureEventCallback gesture_callback)
    : KRGestureEventHandler(node_handle, gesture_group, gesture_callback) {}

const bool KRTapGestureEventHandler::MineEvent(const KRGestureEventType &event_type) const {
    return event_type == KRGestureEventType::kClick || event_type == KRGestureEventType::kDoubleClick;
}

bool KRTapGestureEventHandler::RegisterEvent(const KRGestureEventType &event_type) {
    if (event_type == KRGestureEventType::kClick) {
        register_single_tap_event_ = true;
    } else if (event_type == KRGestureEventType::kDoubleClick) {
        register_double_tap_event_ = true;
    }
    if (gesture_recognizer_) {
        return false;
    }
    event_type_ = KRGestureEventType::kClick;
    auto gesture_api = kuikly::util::GetGestureApi();
    gesture_recognizer_ = kuikly::util::GetGestureApi()->createTapGesture(1, 1);
    void *userData = KRWeakObjectManagerRegisterWeakObject(shared_from_this());
    gesture_api->setGestureEventTarget(gesture_recognizer_, GESTURE_EVENT_ACTION_ACCEPT, userData, OnReceiveGestureEvent);
    gesture_api->addChildGesture(gesture_group_, gesture_recognizer_);
    return true;
}

void KRTapGestureEventHandler::OnGestureEvent(ArkUI_GestureEvent *event) {
    auto action_type = kuikly::util::GetArkUIGestureActionType(event);
    if (register_double_tap_event_) {
        if (action_type == GESTURE_EVENT_ACTION_ACCEPT) {
            tap_event_data_ = std::make_shared<KRGestureEventData>(event);
            current_tap_count_++;
            if (current_tap_count_ == 1) {
                last_tap_down_time_stamp_ = CurrentTimeStamp();
                KRMainThread::RunOnMainThread(
                    [this, event] {
                        if (current_tap_count_ == 1) {
                            gesture_callback_(node_, tap_event_data_, KRGestureEventType::kClick);
                        }
                        Reset();
                    },
                    250);
            } else if (current_tap_count_ == 2) {
                auto duration = CurrentTimeStamp() - last_tap_down_time_stamp_;
                if (duration <= 250) {
                    gesture_callback_(node_, tap_event_data_, KRGestureEventType::kDoubleClick);
                    Reset();
                }
            }
        }
    } else {
        if (action_type == ArkUI_GestureEventActionType::GESTURE_EVENT_ACTION_ACCEPT) {
            gesture_callback_(node_, std::make_shared<KRGestureEventData>(event), KRGestureEventType::kClick);
        }
    }
}

int64_t KRTapGestureEventHandler::CurrentTimeStamp() const {
    auto now = std::chrono::system_clock::now();
    auto duration = now.time_since_epoch();
    return std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
}

void KRTapGestureEventHandler::Reset() {
    current_tap_count_ = 0;
    last_tap_down_time_stamp_ = 0;
    tap_event_data_ = nullptr;
}