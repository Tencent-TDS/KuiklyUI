/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

#include "libohos_render/export/IKRRenderModuleExport.h"

// Registry lives in libkuikly.so (not the header) so app-side .so modules
// that call RegisterModuleCreator share the same map as CreateModule.
std::unordered_map<std::string, KRModuleCreator> &IKRRenderModuleExport::GetRegisterModuleCreator() {
    static std::unordered_map<std::string, KRModuleCreator> gRegisterModuleCreator;
    return gRegisterModuleCreator;
}

void IKRRenderModuleExport::RegisterModuleCreator(const std::string &module_name, const KRModuleCreator &creator) {
    GetRegisterModuleCreator()[module_name] = creator;
}

void IKRRenderModuleExport::RegisterForwardArkTSModuleCreator(const KRModuleCreator &creator) {
    RegisterModuleCreator(std::string(FOAWARD_ARTKS_MODULE_NAME), creator);
}

std::shared_ptr<IKRRenderModuleExport> IKRRenderModuleExport::CreateModule(const std::string &module_name) {
    auto it = GetRegisterModuleCreator().find(module_name);
    if (it != GetRegisterModuleCreator().end()) {
        return it->second();
    }
    // Fall back to generic ArkTS forward module.
    it = GetRegisterModuleCreator().find(std::string(FOAWARD_ARTKS_MODULE_NAME));
    if (it != GetRegisterModuleCreator().end()) {
        return it->second();
    }
    return nullptr;
}
