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

#ifndef CORE_RENDER_OHOS_KRBACKPRESSMODULE_H
#define CORE_RENDER_OHOS_KRBACKPRESSMODULE_H

#include "libohos_render/export/IKRRenderModuleExport.h"

enum KRBackPressState {
    Undefined = 0,
    Consumed,
    NotConsumed
};

namespace kuikly {
namespace module {

class KRBackPressModule : public IKRRenderModuleExport {
 public:
    static const char MODULE_NAME[];
    
    KRBackPressModule() = default;
    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;
    
    std::atomic<bool> is_back_consumed{false};
    std::atomic<int64_t> back_consumed_time{0};

 private:
    static const char METHOD_BACK_HANDLE[];
    void BackHandle(const KRAnyValue &params);
};

}
}

#endif  // CORE_RENDER_OHOS_KRBACKPRESSMODULE_H
