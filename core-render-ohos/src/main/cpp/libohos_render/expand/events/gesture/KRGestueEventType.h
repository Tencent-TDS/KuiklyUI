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

#ifndef CORE_RENDER_OHOS_KRGESTUEEVENTTYPE_H
#define CORE_RENDER_OHOS_KRGESTUEEVENTTYPE_H

#include <arkui/native_gesture.h>
#include <arkui/native_type.h>
#include <functional>
#include "libohos_render/foundation/KRPoint.h"
#include "libohos_render/utils/KREventUtil.h"

enum class KRGestureEventType {
    kUnknown = 0,
    kClick = 1,
    kDoubleClick = 2,
    kLongPress = 3,
    kPan = 4,
    kPinch = 5,
};

struct KRGestureEventData {
 public:
    explicit KRGestureEventData(ArkUI_GestureEvent *event) : gesture_event_(event) {
        gesture_event_point_ = kuikly::util::GetArkUIGestureEventPoint(event);
        gesture_event_window_point_ = kuikly::util::GetArkUIGestureEventWindowPoint(event);
    }

 public:
    KRPoint gesture_event_point_;
    KRPoint gesture_event_window_point_;
    ArkUI_GestureEvent *gesture_event_;
};

using KRGestureEventCallback = std::function<void(const ArkUI_NodeHandle node_handle,
                                                  const std::shared_ptr<KRGestureEventData> &gesture_event_data,
                                                  const KRGestureEventType &event_type)>;

#endif  // CORE_RENDER_OHOS_KRGESTUEEVENTTYPE_H
