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

#include "KRGestureCaptureRule.h"

#include "libohos_render/utils/KRJSONObject.h"

std::vector<KRGestureCaptureRule> KRGestureCaptureRule::Parse(const std::string &rule_data) {
    auto result = std::vector<KRGestureCaptureRule>();
    if (!rule_data.empty()) {
        auto array = kuikly::util::JSONObject::Parse(rule_data);
        if (array) {
            int len = array->GetArraySize();
            for (int i = 0; i < len; ++i) {
                auto obj = array->GetArrayItem(i);
                KRGestureCaptureRule rule;
                rule.type = obj->GetNumber("type", TYPE_UNKNOWN);
                auto area = obj->GetObjectItem("area");
                if (area) {
                    rule.x = area->GetNumber("x");
                    rule.y = area->GetNumber("y");
                    rule.width = area->GetNumber("width");
                    rule.height = area->GetNumber("height");
                }
                rule.direction = obj->GetNumber("direction", ALL);
                if (rule.Valid()) {
                    result.push_back(std::move(rule));
                }
            }
        }
    }
    return std::move(result);
}

bool KRGestureCaptureRule::Valid() {
    return type >= TYPE_CLICK && type <= TYPE_PAN;
}

bool KRGestureCaptureRule::Test(const ArkUI_GestureRecognizerType gesture_type, const ArkUI_GestureEvent *gesture_event,
                                const float interrupt_x, const float interrupt_y) {
    if (TestType(gesture_type) && TestArea(interrupt_x, interrupt_y)) {
        return type != TYPE_PAN || TestPanDirection(gesture_event);
    }
    return false;
}

bool KRGestureCaptureRule::TestType(const ArkUI_GestureRecognizerType gesture_type) {
    switch (type) {
    case TYPE_CLICK:
    case TYPE_DOUBLE_CLICK:
        return gesture_type == TAP_GESTURE;
    case TYPE_LONG_PRESS:
        return gesture_type == LONG_PRESS_GESTURE;
    case TYPE_PAN:
        return gesture_type == PAN_GESTURE;
    default:
        return false;
    }
}

bool KRGestureCaptureRule::TestArea(const float interrupt_x, const float interrupt_y) {
    if (!isnan(x) && !isnan(width)) {
        if (interrupt_x < x || interrupt_x > x + width) {
            return false;
        }
    }
    if (!isnan(y) && !isnan(height)) {
        if (interrupt_y < y || interrupt_y > y + height) {
            return false;
        }
    }
    return true;
}

bool KRGestureCaptureRule::TestPanDirection(const ArkUI_GestureEvent *gesture_event) {
    if ((direction & ALL) == ALL) {
        return true;
    }
    float offset_x = OH_ArkUI_PanGesture_GetOffsetX(gesture_event);
    float offset_y = OH_ArkUI_PanGesture_GetOffsetY(gesture_event);
    if (abs(offset_x) > abs(offset_y)) {
        return (offset_x < 0 && (direction & TO_LEFT)) || (offset_x > 0 && (direction & TO_RIGHT));
    } else {
        return (offset_y < 0 && (direction & TO_TOP)) || (offset_y > 0 && (direction & TO_BOTTOM));
    }
}
