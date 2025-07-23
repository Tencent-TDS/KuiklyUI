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

#ifndef CORE_RENDER_OHOS_KRLOGMODULE_H
#define CORE_RENDER_OHOS_KRLOGMODULE_H

#include "libohos_render/export/IKRRenderModuleExport.h"

constexpr char kLogModuleName[] = "KRLogModule";

class KRLogModule : public IKRRenderModuleExport {
 public:
    KRLogModule() = default;

    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;

 private:
    void LogInfo(const KRAnyValue &params);
    void LogDebug(const KRAnyValue &params);
    void LogError(const KRAnyValue &params);
    std::string FindTag(const std::string &msg);
};

#endif  // CORE_RENDER_OHOS_KRLOGMODULE_H
