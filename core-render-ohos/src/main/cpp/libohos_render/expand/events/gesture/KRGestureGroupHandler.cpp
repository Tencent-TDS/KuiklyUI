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

#include "libohos_render/expand/events/gesture/KRGestureGroupHandler.h"

#include "libohos_render/expand/events/KREventDispatchCenter.h"
#include "libohos_render/utils/KREventUtil.h"
#include "libohos_render/manager/KRWeakObjectManager.h"


KRGestureGroupHandler::KRGestureGroupHandler(const ArkUI_NodeHandle node_handle,
                                             KRGestureEventCallback gesture_event_callback)
    : node_handle_(node_handle), gesture_event_callback_(gesture_event_callback) {
    gesture_group_ = kuikly::util::GetGestureApi()->createGroupGesture(ArkUI_GroupGestureMode::EXCLUSIVE_GROUP);
}

KRGestureGroupHandler::~KRGestureGroupHandler() {
    std::for_each(gesture_event_handlers_.begin(), gesture_event_handlers_.end(), [](auto gesture){
        KRWeakObjectManagerUnregisterWeakObject(gesture);
    });
    gesture_event_handlers_.clear();
    capture_rules_.clear();

    auto gesture_api = kuikly::util::GetGestureApi();
    gesture_api->removeGestureFromNode(node_handle_, gesture_group_);
    gesture_api->dispose(gesture_group_);
}

void KRGestureGroupHandler::RegisterGestureEvent(const KRGestureEventType &event_type) {
    if (!node_handle_) {
        return;
    }

    if (event_type == KRGestureEventType::kUnknown) {
        return;
    }
    auto gesture_handler = GetGestureEventHandler(event_type);
    if (gesture_handler) {
        bool changed = gesture_handler->RegisterEvent(event_type);
        if (changed) {
            RefreshGestureRegistration();
        }
    }
}

std::shared_ptr<KRGestureEventHandler>
KRGestureGroupHandler::GetGestureEventHandler(const KRGestureEventType &event_type) {
    std::shared_ptr<KRGestureEventHandler> gesture_handler = nullptr;
    for (const auto &handler : gesture_event_handlers_) {
        if (handler->MineEvent(event_type)) {
            gesture_handler = handler;
            break;
        }
    }
    if (gesture_handler) {
        return gesture_handler;
    }

    auto gesture_handler_callback = [this](const ArkUI_NodeHandle node_handle,
                                           const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                                           const KRGestureEventType &event_type) {
        gesture_event_callback_(node_handle, gesture_event_data, event_type);
    };
    if (event_type == KRGestureEventType::kPan) {
        gesture_handler =
            std::make_shared<KRPanGestureEventHandler>(node_handle_, gesture_group_, gesture_handler_callback);
    } else if (event_type == KRGestureEventType::kPinch) {
        gesture_handler =
            std::make_shared<KRPinchGestureEventHandler>(node_handle_, gesture_group_, gesture_handler_callback);
    } else if (event_type == KRGestureEventType::kLongPress) {
        gesture_handler =
            std::make_shared<KRLongPressGestureEventHandler>(node_handle_, gesture_group_, gesture_handler_callback);
    } else if (event_type == KRGestureEventType::kDoubleClick || event_type == KRGestureEventType::kClick) {
        gesture_handler =
            std::make_shared<KRTapGestureEventHandler>(node_handle_, gesture_group_, gesture_handler_callback);
    } else {
        return nullptr;
    }
    gesture_event_handlers_.push_back(gesture_handler);
    return gesture_handler;
}

void KRGestureGroupHandler::RefreshGestureRegistration() {
    auto gesture_api = kuikly::util::GetGestureApi();
    gesture_api->removeGestureFromNode(node_handle_, gesture_group_);
    if (gesture_event_handlers_.empty()) {
        return;
    }
    auto mode = capture_rules_.empty() ? ArkUI_GesturePriority::NORMAL : ArkUI_GesturePriority::PRIORITY;
    gesture_api->addGestureToNode(node_handle_, gesture_group_, mode, NORMAL_GESTURE_MASK);
}

void KRGestureGroupHandler::SetCaptureRule(const std::vector<KRGestureCaptureRule> &&rules) {
    bool old_empty = capture_rules_.empty();
    capture_rules_ = std::move(rules);
    if (capture_rules_.empty() != old_empty) {
        RefreshGestureRegistration();
    }
}

bool KRGestureGroupHandler::TestCaptureRule(const ArkUI_GestureRecognizerType gesture_type,
                                            const ArkUI_GestureEvent *gesture_event,
                                            const float interrupt_x,
                                            const float interrupt_y) {
    for (auto& rule : capture_rules_) {
        if (rule.Test(gesture_type, gesture_event, interrupt_x, interrupt_y)) {
            return true;
        }
    }
    return false;
}
