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

#ifndef CORE_RENDER_OHOS_KRGESTUREEVENTHANDLER_H
#define CORE_RENDER_OHOS_KRGESTUREEVENTHANDLER_H

#include <arkui/native_gesture.h>
#include <arkui/native_type.h>
#include "libohos_render/expand/events/gesture/KRGestueEventType.h"

class KRGestureEventHandler: public std::enable_shared_from_this<KRGestureEventHandler>{
 public:
    KRGestureEventHandler(ArkUI_NodeHandle node_handle, ArkUI_GestureRecognizer *gesture_group,
                          KRGestureEventCallback gesture_callback);
    KRGestureEventHandler(const KRGestureEventHandler &other) = delete;
    virtual ~KRGestureEventHandler();

    virtual const bool MineEvent(const KRGestureEventType &event_type) const;
    virtual bool RegisterEvent(const KRGestureEventType &event_type) = 0;
    virtual void OnGestureEvent(ArkUI_GestureEvent *event);

 protected:
    KRGestureEventType event_type_;
    ArkUI_GestureRecognizer *gesture_recognizer_ = nullptr;
    ArkUI_GestureRecognizer *gesture_group_ = nullptr;
    ArkUI_NodeHandle node_ = nullptr;
    KRGestureEventCallback gesture_callback_ = nullptr;
};

class KRPanGestureEventHandler : public KRGestureEventHandler {
 public:
    KRPanGestureEventHandler(ArkUI_NodeHandle node_handle, ArkUI_GestureRecognizer *gesture_group,
                             KRGestureEventCallback gesture_callback);
    KRPanGestureEventHandler(const KRPanGestureEventHandler &other) = delete;

    bool RegisterEvent(const KRGestureEventType &event_type) override;
};

class KRPinchGestureEventHandler : public KRGestureEventHandler {
 public:
    KRPinchGestureEventHandler(ArkUI_NodeHandle node_handle, ArkUI_GestureRecognizer *gesture_group,
                               KRGestureEventCallback gesture_callback);
    KRPinchGestureEventHandler(const KRPanGestureEventHandler &other) = delete;

    bool RegisterEvent(const KRGestureEventType &event_type) override;
};

class KRLongPressGestureEventHandler : public KRGestureEventHandler {
 public:
    KRLongPressGestureEventHandler(ArkUI_NodeHandle node_handle, ArkUI_GestureRecognizer *gesture_group,
                                   KRGestureEventCallback gesture_callback);
    KRLongPressGestureEventHandler(const KRLongPressGestureEventHandler &other) = delete;
    bool RegisterEvent(const KRGestureEventType &event_type) override;
};

class KRTapGestureEventHandler : public KRGestureEventHandler {
 public:
    KRTapGestureEventHandler(ArkUI_NodeHandle node_handle, ArkUI_GestureRecognizer *gesture_group,
                             KRGestureEventCallback gesture_callback);
    KRTapGestureEventHandler(const KRTapGestureEventHandler &other) = delete;
    const bool MineEvent(const KRGestureEventType &event_type) const override;
    bool RegisterEvent(const KRGestureEventType &event_type) override;
    void OnGestureEvent(ArkUI_GestureEvent *event) override;

 private:
    int64_t CurrentTimeStamp() const;
    void Reset();

 private:
    bool register_single_tap_event_ = false;
    bool register_double_tap_event_ = false;
    int current_tap_count_ = false;
    int64_t last_tap_down_time_stamp_ = 0;
    std::shared_ptr<KRGestureEventData> tap_event_data_;
};

#endif  // CORE_RENDER_OHOS_KRGESTUREEVENTHANDLER_H
