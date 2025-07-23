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

#ifndef CORE_RENDER_OHOS_KRGESTUREGROUPHANDLER_H
#define CORE_RENDER_OHOS_KRGESTUREGROUPHANDLER_H

#include <arkui/native_gesture.h>
#include <functional>
#include "KRGestureCaptureRule.h"
#include "libohos_render/expand/events/gesture/KRGestueEventType.h"
#include "libohos_render/expand/events/gesture/KRGestureEventHandler.h"

class KRGestureGroupHandler {
 public:
    KRGestureGroupHandler(const ArkUI_NodeHandle node_handle, KRGestureEventCallback gesture_event_callback);
    ~KRGestureGroupHandler();

    void RegisterGestureEvent(const KRGestureEventType &event_type);
    void SetCaptureRule(const std::vector<KRGestureCaptureRule> &&rules);
    bool TestCaptureRule(const ArkUI_GestureRecognizerType gesture_type,
                         const ArkUI_GestureEvent *gesture_event,
                         const float interrupt_x,
                         const float interrupt_y);

 private:
    std::shared_ptr<KRGestureEventHandler> GetGestureEventHandler(const KRGestureEventType &event_type);
    void RefreshGestureRegistration();

 private:
    std::vector<std::shared_ptr<KRGestureEventHandler>> gesture_event_handlers_;
    ArkUI_NodeHandle node_handle_ = nullptr;
    ArkUI_GestureRecognizer *gesture_group_ = nullptr;
    KRGestureEventCallback gesture_event_callback_;
    std::vector<KRGestureCaptureRule> capture_rules_;
};

#endif  // CORE_RENDER_OHOS_KRGESTUREGROUPHANDLER_H
