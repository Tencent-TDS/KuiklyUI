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
#pragma once
#include "libohos_render/expand/modules/preferences/KRPreferences.h"
#include "libohos_render/export/IKRRenderModuleExport.h"

namespace kuikly {
namespace util {
class DataPreferences;
}

namespace expand {

class KRSharedPreferencesModule : public IKRRenderModuleExport {
 public:
    static const char MODULE_NAME[];
    bool SyncMode();
    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;
    virtual void OnDestroy() override;

 private:
    static const char GET_ITEM[];
    static const char SET_ITEM[];
    std::string GetItem(const KRAnyValue &params);
    std::string SetItem(const KRAnyValue &params);
    std::shared_ptr<util::DataPreferences> preferences;
    void InitIfNeeded();
};

}  // namespace expand
}  // namespace kuikly
