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

#ifndef CORE_RENDER_OHOS_KRRENDERNATIVECONTEXTHANDLERMANAGER_H
#define CORE_RENDER_OHOS_KRRENDERNATIVECONTEXTHANDLERMANAGER_H

#include <mutex>
#include <unordered_map>
#include "libohos_render/context/IKRRenderNativeContextHandler.h"
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/foundation/type/KRRenderValue.h"

class KRRenderNativeContextHandlerManager {
 public:
    KRRenderNativeContextHandlerManager(const KRRenderNativeContextHandlerManager &) = delete;
    KRRenderNativeContextHandlerManager &operator=(const KRRenderNativeContextHandlerManager &) = delete;

    void SetContextHandlerCreator(const KRRenderContextHandlerCreator &creator);

    std::shared_ptr<IKRRenderNativeContextHandler>
    CreateContextHandler(const std::shared_ptr<KRRenderContextParams> &context_params);

    void RegisterContextHandler(const std::string &instanceId,
                                const std::shared_ptr<IKRRenderNativeContextHandler> &contextHandler);
    void UnregisterContextHandler(const std::string &instanceId);
    KRRenderCValue DispatchCallNative(const std::string &instanceId, int methodId, const KRRenderCValue &arg0,
                                      const KRRenderCValue &arg1, const KRRenderCValue &arg2,
                                      const KRRenderCValue &arg3, const KRRenderCValue &arg4,
                                      const KRRenderCValue &arg5);
    static KRRenderNativeContextHandlerManager &GetInstance() {
        static KRRenderNativeContextHandlerManager m_instance;  // 局部静态变量
        return m_instance;
    }

    static void RegisterContextHandlerCreator(const int &mode, const KRRenderContextHandlerCreator &creator) {
        GetContextHandlerCreatorRegister()[mode] = creator;
    }
    static std::unordered_map<int, KRRenderContextHandlerCreator> &GetContextHandlerCreatorRegister() {
        static std::unordered_map<int, KRRenderContextHandlerCreator> gRegisterContextHandlerCreator;
        return gRegisterContextHandlerCreator;
    }

 private:
    KRRenderNativeContextHandlerManager() {}
    void ScheduleDeallocRenderValues(const std::shared_ptr<KRRenderValue> will_dealloc_render_value);

 private:
    std::unordered_map<std::string, std::shared_ptr<IKRRenderNativeContextHandler>> context_handler_map_;
    KRRenderContextHandlerCreator creator_;
    bool scheduling_dealloc_render_values_ = false;
    std::vector<std::shared_ptr<KRRenderValue>> pending_dealloc_render_values_;

    static KRRenderNativeContextHandlerManager *instance_;
};
#endif  // CORE_RENDER_OHOS_KRRENDERNATIVECONTEXTHANDLERMANAGER_H
