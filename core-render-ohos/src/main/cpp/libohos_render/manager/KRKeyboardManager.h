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

#ifndef CORE_RENDER_OHOS_KRKEYBOARDMANAGER_H
#define CORE_RENDER_OHOS_KRKEYBOARDMANAGER_H

#include <memory>
#include <vector>
#include "libohos_render/foundation/KRCommon.h"
class KRKeyboardDelegate {
    /**
     * 键盘变化回调
     * @param height 键盘此时高度（单位vp）
     * @param duration 键盘弹出动画时间 （单位ms）
     */
    virtual void KeyboardHeightChange(float height, int duration_ms);
};

using KRKeyboardCallback = std::function<void(float height, int duration_ms)>;

class KRKeyboardManager {
 public:
    static KRKeyboardManager &GetInstance();
    // 禁止复制和赋值
    KRKeyboardManager(const KRKeyboardManager &) = delete;
    KRKeyboardManager &operator=(const KRKeyboardManager &) = delete;

    /**
     * 添加对键盘事件的订阅者
     */
    void AddKeyboardTask(std::string key, const KRKeyboardCallback &callback);

    /**
     * 删除对键盘事件的订阅者
     */
    void RemoveKeyboardTask(std::string key);

    /**
     * 通知响应键盘变化，内部分发给感兴趣的订阅者
     */
    void NotifyKeyboardHeightChanged(float height, int duration_ms);

 private:
    KRKeyboardManager() {}  // 构造函数私有化
    std::unordered_map<std::string, KRKeyboardCallback> keyboard_listens_;
};

#endif  // CORE_RENDER_OHOS_KRKEYBOARDMANAGER_H
