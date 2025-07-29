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
void KRKeyboardManager::AddKeyboardTask(std::string key, const KRKeyboardCallback &callback) {
    keyboard_listens_[key] = callback;
}

/**
 * 删除对键盘事件的订阅者
 */
void KRKeyboardManager::RemoveKeyboardTask(std::string key) {
    keyboard_listens_.erase(key);
}

/**
 * 通知响应键盘变化，内部分发给感兴趣的订阅者
 */
void KRKeyboardManager::NotifyKeyboardHeightChanged(float height, int duration_ms) {
    // 复制keyboard_listens_
    auto keyboard_listens_copy = keyboard_listens_;
    for (auto &listener : keyboard_listens_copy) {
        // 调用订阅者的回调函数
        listener.second(height, duration_ms);
    }
}
