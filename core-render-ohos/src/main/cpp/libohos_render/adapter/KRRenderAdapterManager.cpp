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

std::string KRRenderAdapterManager::GetIncreaseImageCallbackId(ImageCallback image_call_back) {
    static int g_image_callback_id = 0;
    g_image_callback_id++;
    std::string image_callback_id =  NewKRRenderValue(g_image_callback_id)->toString();
    image_callback_map_[image_callback_id] = image_call_back;
    return image_callback_id;
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

void KRRenderAdapterManager::RegisterColorAdapter(IKRColorParseAdapter *color_adapter) {
    color_adapter_ = color_adapter;
}

void KRRenderAdapterManager::RegisterLogAdapter(std::shared_ptr<IKRLogAdapter> log_adapter) {
    log_adapter_ = log_adapter;
}

void KRRenderAdapterManager::RegisterImageAdapter(napi_env env, napi_value func) {
    if (image_adapter_ != nullptr) {
        napi_delete_reference(image_adapter_->env, image_adapter_->func_ref);
    }
    image_adapter_ = std::make_shared<ArkTSFuncData>();
    image_adapter_->env = env;
    image_adapter_->func = func;
    // 将传入的callback转换为napi_ref延长其生命周期，防止被GC掉
    napi_create_reference(env, func, 1, &(image_adapter_->func_ref));
}

void KRRenderAdapterManager::CallImageAdapter(const char *image_src, ImageCallback callback) {
    napi_env env = image_adapter_->env;
    napi_value image_adapter_func;
    napi_get_reference_value(env, image_adapter_->func_ref, &image_adapter_func);
    napi_value func_Args[2] = {nullptr};
    napi_value src_value;
    napi_create_string_utf8(env, image_src, strlen(image_src), &src_value);
    func_Args[0] = src_value;
    std::string image_callback_id = GetIncreaseImageCallbackId(callback);
    napi_value callback_id_value;
    napi_create_string_utf8(env, image_callback_id.c_str(), image_callback_id.length(), &callback_id_value);
    func_Args[1] = callback_id_value;
    // 执行ArkTS图片适配器函数
    napi_value result;
    napi_call_function(env, nullptr, image_adapter_func, 2, func_Args, &result);
}

void KRRenderAdapterManager::fireImageCallback(const char *image_src, OH_PixelmapNative *pixel_map, std::string callback_id) {
    auto callback = image_callback_map_.find(callback_id);
    if (callback != image_callback_map_.end()) {
        if (callback->second != nullptr) {
            callback->second((char *)image_src, pixel_map);
        }
        image_callback_map_.erase(callback_id);
    }
}

IKRColorParseAdapter *KRRenderAdapterManager::GetColorAdapter() {
    return color_adapter_;
}
