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

#ifndef CORE_RENDER_OHOS_KRGESTURECAPTURERULE_H
#define CORE_RENDER_OHOS_KRGESTURECAPTURERULE_H

#include <arkui/native_gesture.h>
#include <vector>
class KRGestureCaptureRule {
    enum Type { TYPE_UNKNOWN = 0, TYPE_CLICK = 1, TYPE_DOUBLE_CLICK = 2, TYPE_LONG_PRESS = 3, TYPE_PAN = 4 };
    enum Direction {
        TO_LEFT = 1,
        TO_TOP = 1 << 1,
        TO_RIGHT = 1 << 2,
        TO_BOTTOM = 1 << 3,
        HORIZONTAL = TO_LEFT | TO_RIGHT,
        VERTICAL = TO_TOP | TO_BOTTOM,
        ALL = HORIZONTAL | VERTICAL
    };

 public:
    static std::vector<KRGestureCaptureRule> Parse(const std::string &rule_data);
    bool Valid();
    bool Test(const ArkUI_GestureRecognizerType gesture_type, const ArkUI_GestureEvent *gesture_event,
              const float interrupt_x, const float interrupt_y);

 private:
    bool TestType(const ArkUI_GestureRecognizerType gesture_type);
    bool TestArea(const float interrupt_x, const float interrupt_y);
    bool TestPanDirection(const ArkUI_GestureEvent *gesture_event);
    int type = TYPE_UNKNOWN;
    float x = NAN;
    float y = NAN;
    float width = NAN;
    float height = NAN;
    int direction = ALL;
};

#endif  // CORE_RENDER_OHOS_KRGESTURECAPTURERULE_H
