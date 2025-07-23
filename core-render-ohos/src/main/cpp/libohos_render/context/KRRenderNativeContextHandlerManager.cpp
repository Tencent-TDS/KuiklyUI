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

#include "libohos_render/context/DefaultRenderNativeContextHandler.h"
#include "libohos_render/manager/KRRenderManager.h"
#include "libohos_render/scheduler/KRContextScheduler.h"

extern CallKotlin callKotlin_;

void KRRenderNativeContextHandlerManager::SetContextHandlerCreator(const KRRenderContextHandlerCreator &creator) {
    creator_ = creator;
}

std::shared_ptr<IKRRenderNativeContextHandler> KRRenderNativeContextHandlerManager::CreateContextHandler(
    const std::shared_ptr<KRRenderContextParams> &context_params) {
    KRRenderContextHandlerCreator creator_;
    if (context_params->ExecuteMode()) {
        std::unordered_map<int, KRRenderContextHandlerCreator> context_creator_register =
            GetContextHandlerCreatorRegister();
        int param_mode = context_params->ExecuteMode()->GetMode();
        if (context_creator_register.find(param_mode) != context_creator_register.end()) {
            creator_ = context_creator_register[param_mode];  //  优先使用自定义注册的创建器
        } else if (auto native_mode = dynamic_cast<KRRenderNativeMode *>(context_params->ExecuteMode().get())) {
            auto context_handler_register = [](const std::shared_ptr<KRRenderContextParams> &context_params)
                -> std::shared_ptr<IKRRenderNativeContextHandler> {
                return std::make_shared<DefaultRenderNativeContextHandler>();
            };
            creator_ = context_handler_register;
        }
    }
    if (creator_) {
        return creator_(context_params);
    } else {
        throw std::runtime_error("Custom execute mode, contextHandler must be registered");
    }
}

void KRRenderNativeContextHandlerManager::RegisterContextHandler(
    const std::string &instanceId, const std::shared_ptr<IKRRenderNativeContextHandler> &contextHandler) {
    context_handler_map_[instanceId] = contextHandler;
}

void KRRenderNativeContextHandlerManager::UnregisterContextHandler(const std::string &instanceId) {
    context_handler_map_.erase(instanceId);
}

void KRRenderNativeContextHandlerManager::ScheduleDeallocRenderValues(
    const std::shared_ptr<KRRenderValue> will_dealloc_render_value) {
    pending_dealloc_render_values_.push_back(will_dealloc_render_value);
    if (!scheduling_dealloc_render_values_) {
        scheduling_dealloc_render_values_ = true;
        KRContextScheduler::ScheduleTask(false, 16, []() {
            KRRenderNativeContextHandlerManager::GetInstance().pending_dealloc_render_values_.clear();
            KRRenderNativeContextHandlerManager::GetInstance().scheduling_dealloc_render_values_ = false;
        });
    }
}

KRRenderCValue KRRenderNativeContextHandlerManager::DispatchCallNative(
    const std::string &instanceId, int methodId, const KRRenderCValue &arg0, const KRRenderCValue &arg1,
    const KRRenderCValue &arg2, const KRRenderCValue &arg3, const KRRenderCValue &arg4, const KRRenderCValue &arg5) {
    auto handler = context_handler_map_[instanceId];
    if (!handler || nullptr == KRRenderManager::GetInstance().GetRenderView(instanceId)) {
        auto cv = KRRenderCValue();
        cv.type = KRRenderCValue::NULL_VALUE;
        return cv;
    }
    auto cv0 = std::make_shared<KRRenderValue>(arg0);
    auto cv1 = std::make_shared<KRRenderValue>(arg1);
    auto cv2 = std::make_shared<KRRenderValue>(arg2);
    auto cv3 = std::make_shared<KRRenderValue>(arg3);
    auto cv4 = std::make_shared<KRRenderValue>(arg4);
    auto cv5 = std::make_shared<KRRenderValue>(arg5);

    auto return_value =
        handler->OnCallNative(static_cast<KuiklyRenderNativeMethod>(methodId), cv0, cv1, cv2, cv3, cv4, cv5);
    if (return_value == nullptr) {
        KRRenderCValue null_return_value;
        null_return_value.type = KRRenderCValue::NULL_VALUE;
        return null_return_value;
    }
    ScheduleDeallocRenderValues(return_value);
    return return_value->toCValue();
}
