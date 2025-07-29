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

#include "libohos_render/expand/modules/performance/KRPerformanceModule.h"

#include "libohos_render/foundation/KRCommon.h"

namespace kuikly {
namespace module {
constexpr char kMethodNameOnCreatePageFinish[] = "onPageCreateFinish";
constexpr char kMethodNameGetPerformanceData[] = "getPerformanceData";

const char KRPerformanceModule::MODULE_NAME[] = "KRPerformanceModule";

KRAnyValue KRPerformanceModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                           const KRRenderCallback &callback) {
    if (auto root_view = GetRootView().lock()) {
        std::shared_ptr<KRPerformanceManager> performance_manager = root_view->GetPerformanceManager();
        if (method == kMethodNameOnCreatePageFinish) {
            KRPageCreateTrace trace = KRPageCreateTrace(params->toString());
            performance_manager->OnPageCreateFinish(trace);
        } else if (method == kMethodNameGetPerformanceData) {
            std::string data = performance_manager->GetPerformanceData();
            std::shared_ptr<KRRenderValue> callback_param;
            callback_param = std::make_shared<KRRenderValue>(data);
            callback(callback_param);
        }
    }
    return KREmptyValue();
}
}  // namespace module
}  // namespace kuikly