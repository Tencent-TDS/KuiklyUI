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

#include "libohos_render/manager/KRKeyboardManager.h"

KRKeyboardManager &KRKeyboardManager::GetInstance() {
    static KRKeyboardManager instance;  // 静态局部变量
    return instance;
}

/**
 * 添加对键盘事件的订阅者
 */
void KRKeyboardManager::AddKeyboardTask(std::string window_id, std::string key, const KRKeyboardCallback &callback) {
    keyboard_listens_[window_id][key] = callback;
}

/**
 * 删除对键盘事件的订阅者
 */
void KRKeyboardManager::RemoveKeyboardTask(std::string window_id, std::string key) {
    auto it = keyboard_listens_.find(window_id);
    if (it != keyboard_listens_.end()) {
        auto& window_tasks = it->second;
        window_tasks.erase(key);
        if (window_tasks.empty()) {
            keyboard_listens_.erase(window_id);
        }
    }
}

/**
 * 通知响应键盘变化，内部分发给感兴趣的订阅者
 */
void KRKeyboardManager::NotifyKeyboardHeightChanged(float height, int duration_ms, std::string window_id) {
    auto it = keyboard_listens_.find(window_id);
    if (it != keyboard_listens_.end()) {
        auto keyboard_listens_copy = it->second;
        for (auto &listener : keyboard_listens_copy) {
            listener.second(height, duration_ms);
        }
    }
}
