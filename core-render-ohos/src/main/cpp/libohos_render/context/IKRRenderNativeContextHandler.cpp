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

#include "libohos_render/context/IKRRenderNativeContextHandler.h"

#include "libohos_render/context/KRRenderNativeContextHandlerManager.h"
#include "libohos_render/foundation/thread/KRMainThread.h"

void IKRRenderNativeContextHandler::SetContextHandlerCreator(const KRRenderContextHandlerCreator &creator) {
    KRRenderNativeContextHandlerManager::GetInstance().SetContextHandlerCreator(creator);
}

std::shared_ptr<IKRRenderNativeContextHandler>
IKRRenderNativeContextHandler::CreateContextHandler(const std::shared_ptr<KRRenderContextParams> &context_params) {
    return KRRenderNativeContextHandlerManager::GetInstance().CreateContextHandler(context_params);
}

void IKRRenderNativeContextHandler::RegisterCallNative(ICallNativeCallback *callback) {
    this->call_native_callback_ = callback;
}

std::shared_ptr<KRRenderValue>
IKRRenderNativeContextHandler::OnCallNative(const KuiklyRenderNativeMethod &method, std::shared_ptr<KRRenderValue> &arg0,
                                            std::shared_ptr<KRRenderValue> &arg1, std::shared_ptr<KRRenderValue> &arg2,
                                            std::shared_ptr<KRRenderValue> &arg3, std::shared_ptr<KRRenderValue> &arg4,
                                            std::shared_ptr<KRRenderValue> &arg5) {
    return call_native_callback_ ? call_native_callback_->OnCallNative(method, arg0, arg1, arg2, arg3, arg4, arg5)
                              : std::make_shared<KRRenderValue>();
}

KRRenderCValue IKRRenderNativeContextHandler::DispatchCallNative(const std::string &instanceId, int methodId,
                                                                 const KRRenderCValue &arg0, const KRRenderCValue &arg1,
                                                                 const KRRenderCValue &arg2, const KRRenderCValue &arg3,
                                                                 const KRRenderCValue &arg4,
                                                                 const KRRenderCValue &arg5) {
    return KRRenderNativeContextHandlerManager::GetInstance().DispatchCallNative(instanceId, methodId, arg0, arg1, arg2,
                                                                                 arg3, arg4, arg5);
}

void IKRRenderNativeContextHandler::Init(const std::shared_ptr<KRRenderContextParams> context_params) {
    this->instance_id_ = context_params->InstanceId();
    KRRenderNativeContextHandlerManager::GetInstance().RegisterContextHandler(this->instance_id_, shared_from_this());
    OnInit(context_params);
}

void IKRRenderNativeContextHandler::OnInit(const std::shared_ptr<KRRenderContextParams> &context_params) {}

void IKRRenderNativeContextHandler::InitContext() {}

void IKRRenderNativeContextHandler::WillDestroy() {
    is_destroying_ = true;
}

void IKRRenderNativeContextHandler::Call(const KuiklyRenderContextMethod &method,
                                         const std::shared_ptr<KRRenderValue> &arg0,
                                         const std::shared_ptr<KRRenderValue> &arg1,
                                         const std::shared_ptr<KRRenderValue> &arg2,
                                         const std::shared_ptr<KRRenderValue> &arg3,
                                         const std::shared_ptr<KRRenderValue> &arg4,
                                         const std::shared_ptr<KRRenderValue> &arg5) {
    if (is_destroying_ && (method == KuiklyRenderContextMethod::KuiklyRenderContextMethodFireCallback ||
                          method == KuiklyRenderContextMethod::KuiklyRenderContextMethodLayoutView)) {
        return;
    }

    CallKotlinMethod(method, arg0, arg1, arg2, arg3, arg4, arg5);
}

void IKRRenderNativeContextHandler::OnDestroy() {
    call_native_callback_ = nullptr;
    auto instanceId = this->instance_id_;
    KRMainThread::RunOnMainThread(  // 线程安全
        [instanceId] { KRRenderNativeContextHandlerManager::GetInstance().UnregisterContextHandler(instanceId); });
}
