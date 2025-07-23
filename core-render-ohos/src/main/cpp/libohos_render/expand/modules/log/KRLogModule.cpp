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

#include "libohos_render/expand/modules/log/KRLogModule.h"

#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRStringUtil.h"

constexpr char kMethodNameLogInfo[] = "logInfo";
constexpr char kMethodNameLogDebug[] = "logDebug";
constexpr char kMethodNameLogError[] = "logError";

KRAnyValue KRLogModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                   const KRRenderCallback &callback) {
    // if use has set a C/C++ adapter, then redirect it directly throw it,
    // otherwise redirect logs to the arkts version.
    if (KRRenderAdapterManager::GetInstance().HasCustomLogAdapter()) {
        if (kuikly::util::isEqual(method, kMethodNameLogInfo)) {
            LogInfo(params);
        } else if (kuikly::util::isEqual(method, kMethodNameLogDebug)) {
            LogDebug(params);
        } else if (kuikly::util::isEqual(method, kMethodNameLogError)) {
            LogError(params);
        }
    } else {
        auto instance_id = GetInstanceId();
        auto method_name = method;
        KRContextScheduler::ScheduleTaskOnMainThread(false, [instance_id, method_name, params] {
            auto module_name = NewKRRenderValue("KRLogModuleArkTS");

            KRArkTSManager::GetInstance().CallArkTSMethod(instance_id, KRNativeCallArkTSMethod::CallModuleMethod,
                                                          module_name, NewKRRenderValue(method_name), params, nullptr,
                                                          nullptr, nullptr);
        });
    }
    return KREmptyValue();
}

void KRLogModule::LogInfo(const KRAnyValue &params) {
    auto msg = params->toString();
    auto tag = FindTag(msg);
    KR_LOG_INFO_WITH_TAG(tag) << msg;
}

void KRLogModule::LogDebug(const KRAnyValue &params) {
    auto msg = params->toString();
    auto tag = FindTag(msg);
    KR_LOG_DEBUG_WITH_TAG(tag) << msg;
}

void KRLogModule::LogError(const KRAnyValue &params) {
    auto msg = params->toString();
    auto tag = FindTag(msg);
    KR_LOG_ERROR_WITH_TAG(tag) << msg;
}

std::string KRLogModule::FindTag(const std::string &msg) {
    std::string prefix = "[KLog][";
    std::string suffix = "]:";
    std::size_t start_index = msg.find(prefix);
    if (start_index != std::string::npos) {
        std::size_t endIndex = msg.find(suffix, start_index + prefix.length());
        if (endIndex != std::string::npos) {
            return std::move(msg.substr(start_index + prefix.length(), endIndex - start_index - prefix.length()));
        }
    }
    return "";
}
