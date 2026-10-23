/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRCOMMONEVENTNAME_H
#define CORE_RENDER_OHOS_KRCOMMONEVENTNAME_H

#include <cstring>

namespace kuikly {

/**
 * 框架关注的系统公共事件（CommonEvent）。
 * 新增事件时在此追加枚举值，并在 KRCommonEventManager 中登记系统事件名。
 */
enum class CommonEventName {
    /** 用户点击系统状态栏（用于自定义实现 back-to-top） */
    COMMON_EVENT_CLICK_STATUSBAR = 0,
};

/** 枚举值对应的系统事件名 */
inline const char *CommonEventNameToEventString(CommonEventName name) {
    switch (name) {
    case CommonEventName::COMMON_EVENT_CLICK_STATUSBAR:
        return "usual.event.CLICK_STATUSBAR";
    default:
        return "";
    }
}

/** 系统事件名转枚举值，未登记的事件返回 false */
inline bool CommonEventNameFromEventString(const char *event, CommonEventName &out) {
    if (event == nullptr) {
        return false;
    }
    constexpr CommonEventName kAllNames[] = {
        CommonEventName::COMMON_EVENT_CLICK_STATUSBAR,
    };
    for (const auto name : kAllNames) {
        const char *known_event = CommonEventNameToEventString(name);
        if (known_event != nullptr && std::strcmp(known_event, event) == 0) {
            out = name;
            return true;
        }
    }
    return false;
}

}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRCOMMONEVENTNAME_H
