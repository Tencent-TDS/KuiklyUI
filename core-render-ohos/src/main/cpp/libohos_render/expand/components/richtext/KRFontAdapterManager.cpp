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

#include "KRFontAdapterManager.h"
KRFontAdapterManager *KRFontAdapterManager::GetInstance() {
    static KRFontAdapterManager *instance_ = nullptr;
    static std::once_flag flag;
    std::call_once(flag, []() { instance_ = new KRFontAdapterManager(); });
    return instance_;
}

void KRFontAdapterManager::RegisterFontAdapter(KRFontAdapter adapter, const char *fontFamily) {
    if (fontFamily != nullptr && adapter != nullptr) {
        std::lock_guard<std::mutex> lock(mutex_);
        adapterMap_[fontFamily] = adapter;
    }
}

std::unordered_map<std::string, KRFontAdapter> KRFontAdapterManager::AllAdapters() {
    std::lock_guard<std::mutex> lock(mutex_);
    return adapterMap_;
}
