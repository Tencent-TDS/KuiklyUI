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

#include "libohos_render/context/KRRenderNativeContextHandlerManager.h"

#include "libohos_render/core/KRRenderFactories.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/utils/json/Box.h"

extern CallKotlin callKotlin_;

std::shared_ptr<IKRRenderNativeContextHandler> KRRenderNativeContextHandlerManager::CreateContextHandler(
    const std::shared_ptr<KRRenderContextParams> &context_params) {
    return kuikly::ContextHandlerFactory::CreateContextHandler(context_params);
}

void KRRenderNativeContextHandlerManager::RegisterContextHandler(
    const std::string &instanceId, const std::shared_ptr<IKRRenderNativeContextHandler> &contextHandler) {
    context_handler_map_.Set(instanceId, contextHandler);
}

void KRRenderNativeContextHandlerManager::UnregisterContextHandler(const std::string &instanceId) {
    context_handler_map_.Erase(instanceId);
}

static inline KRAnyValue MakeFromCValue(const KRRenderCValue &cValue) {
    if (kuikly::util::json::GetType(cValue) == KRJSON_NULL) {
        return KRRenderValue::MakeNull();
    }
    return KRRenderValue::MakeBorrowed(cValue);
}

KRRenderCValue KRRenderNativeContextHandlerManager::DispatchCallNative(
    const std::string &instanceId, int methodId, const KRRenderCValue &arg0, const KRRenderCValue &arg1,
    const KRRenderCValue &arg2, const KRRenderCValue &arg3, const KRRenderCValue &arg4, const KRRenderCValue &arg5) {
    auto handler = context_handler_map_.Get(instanceId);
    // 优化：移除冗余的 KRRenderManager::GetInstance().GetRenderView(instanceId) 检查
    // context_handler_map_ 中存在 handler 就意味着实例有效（注册/注销是配对的），
    // 额外的 GetRenderView 每次都要获取 SpinLock + unordered_map 查找，纯属浪费。
    if (!handler) {
        return kuikly::util::json::NewNull();
    }
    // arg0 是 Kotlin 传入的 instanceId（KRJSON string）。值类型下只是 Retain
    // 已有节点，不再额外分配 façade，因此原样下发给 OnCallNative。
    auto cv0 = MakeFromCValue(arg0);
    auto cv1 = MakeFromCValue(arg1);
    auto cv2 = MakeFromCValue(arg2);
    auto cv3 = MakeFromCValue(arg3);
    auto cv4 = MakeFromCValue(arg4);
    auto cv5 = MakeFromCValue(arg5);

    auto return_value =
        handler->OnCallNative(static_cast<KuiklyRenderNativeMethod>(methodId), cv0, cv1, cv2, cv3, cv4, cv5);
    if (return_value == nullptr || return_value->isNull()) {
        return kuikly::util::json::NewNull();
    }
    // The C result is an owned reference. Kotlin converts it immediately and
    // releases this transfer reference; containers retain their own node reference.
    return kuikly::util::json::Retain(return_value->jsonValue());
}
