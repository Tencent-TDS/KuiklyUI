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

#ifndef CORE_RENDER_OHOS_KRCUSTOMUSERCALLBACK_H
#define CORE_RENDER_OHOS_KRCUSTOMUSERCALLBACK_H
#include <arkui/native_node.h>
#include <functional>
using CustomCallback = std::function<void(ArkUI_NodeCustomEvent *event)>;

class KRCustomUserCallback : public std::enable_shared_from_this<KRCustomUserCallback> {
 public:
    explicit KRCustomUserCallback(const CustomCallback &callback) {
        callback_ = callback;
    }

    void Invoke(ArkUI_NodeCustomEvent *event) {
        if (callback_ != nullptr) {
            callback_(event);
        }
    }

 private:
    CustomCallback callback_ = nullptr;
};

#endif  // CORE_RENDER_OHOS_KRCUSTOMUSERCALLBACK_H
