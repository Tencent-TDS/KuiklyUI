/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#include "libohos_render/adapter/KRRenderAdapterManager.h"

#include <sys/stat.h>
#include "libohos_render/manager/KRArkTSManager.h"
#include "libohos_render/scheduler/KRContextScheduler.h"

KRRenderAdapterManager &KRRenderAdapterManager::GetInstance() {
    static KRRenderAdapterManager adapter_manager;
    return adapter_manager;
}

void KRRenderAdapterManager::OnFatalException(const std::string &instance_id, const std::string &stack) {
    CallArkTsExceptionModule(instance_id, "onException", stack);
}

void KRRenderAdapterManager::CallArkTsExceptionModule(const std::string &instance_id, const std::string &method_name,
                                                      const std::string &stack) {
    KRContextScheduler::ScheduleTaskOnMainThread(false, [instance_id, method_name, stack] {
        auto module_name = NewKRRenderValue("KRExceptionModule");
        KRRenderValueMap params;
        params["stack"] = NewKRRenderValue(std::move(stack));
        KRArkTSManager::GetInstance().CallArkTSMethod(instance_id, KRNativeCallArkTSMethod::CallModuleMethod,
                                                      module_name, NewKRRenderValue(method_name),
                                                      NewKRRenderValue(params), nullptr, nullptr, nullptr);
    });
}

void KRRenderAdapterManager::Log(const LogLevel &log_level, const std::string &tag, const std::string &msg) {
    if (log_level == LogLevel::LOG_INFO) {
        LogInfo(tag, msg);
    } else if (log_level == LogLevel::LOG_DEBUG) {
        LogDebug(tag, msg);
    } else if (log_level == LogLevel::LOG_ERROR) {
        LogError(tag, msg);
    }
}

void KRRenderAdapterManager::LogInfo(const std::string &tag, const std::string &msg) {
    if (log_adapter_) {
        log_adapter_->LogInfo(tag, msg);
    } else {
        OH_LOG_Print(LOG_APP, LOG_INFO, 0x7, tag.c_str(), "%{public}s", msg.c_str());
    }
}

void KRRenderAdapterManager::LogDebug(const std::string &tag, const std::string &msg) {
    if (log_adapter_) {
        log_adapter_->LogDebug(tag, msg);
    } else {
        OH_LOG_Print(LOG_APP, LOG_DEBUG, 0x7, tag.c_str(), "%{public}s", msg.c_str());
    }
}

void KRRenderAdapterManager::LogError(const std::string &tag, const std::string &msg) {
    if (log_adapter_) {
        log_adapter_->LogError(tag, msg);
    } else {
        OH_LOG_Print(LOG_APP, LOG_ERROR, 0x7, tag.c_str(), "%{public}s", msg.c_str());
    }
}

void KRRenderAdapterManager::RegisterColorAdapter(std::shared_ptr<IKRColorParseAdapter> color_adapter) {
    color_adapter_ = color_adapter;
}

void KRRenderAdapterManager::RegisterLogAdapter(std::shared_ptr<IKRLogAdapter> log_adapter) {
    log_adapter_ = log_adapter;
}

void KRRenderAdapterManager::RegisterImageAdapter(IKRImageAdapter *image_adapter) {
    image_adapter_ = image_adapter;
}

std::shared_ptr<IKRColorParseAdapter> KRRenderAdapterManager::GetColorAdapter() {
    return color_adapter_;
}

IKRImageAdapter *KRRenderAdapterManager::GetImageAdapter() {
    return image_adapter_;
}
