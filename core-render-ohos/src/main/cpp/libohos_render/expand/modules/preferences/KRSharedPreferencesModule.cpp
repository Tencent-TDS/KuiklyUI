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

#include "KRSharedPreferencesModule.h"

#include "KRPreferences.h"
#include "libohos_render/utils/KRJSONObject.h"

namespace kuikly {
namespace expand {
const char KRSharedPreferencesModule::MODULE_NAME[] = "KRSharedPreferencesModule";
const char KRSharedPreferencesModule::GET_ITEM[] = "getItem";
const char KRSharedPreferencesModule::SET_ITEM[] = "setItem";

bool KRSharedPreferencesModule::SyncMode() {
    return true;
}
void KRSharedPreferencesModule::InitIfNeeded() {
    if (this->preferences == nullptr) {
        std::string options = this->MODULE_NAME;
        if (auto root = GetRootView().lock()) {
            const std::string filesDir = root->GetContext()->Config()->GetFilesDir();
            if (!filesDir.empty()) {
                this->preferences = util::DataPreferences::GetInstance(filesDir, options);
            }
        }
    }
}

KRAnyValue KRSharedPreferencesModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                                 const KRRenderCallback &callback) {
    if (method == this->GET_ITEM) {
        return std::make_shared<KRRenderValue>(this->GetItem(params));
    } else if (method == this->SET_ITEM) {
        return std::make_shared<KRRenderValue>(this->SetItem(params));
    }
    return std::make_shared<KRRenderValue>("");
}

std::string KRSharedPreferencesModule::GetItem(const KRAnyValue &params) {
    InitIfNeeded();
    auto key = params->toString();
    auto value = this->preferences->GetSync(key, "");
    // KLOG_INFO(TAG) << "==== get item key = " << key << ' ' << value;
    return value;
}

std::string KRSharedPreferencesModule::SetItem(const KRAnyValue &params) {
    InitIfNeeded();

    auto jsonObj = util::JSONObject::Parse(params->toString());
    std::string key = jsonObj->GetString("key");
    std::string value = jsonObj->GetString("value");
    // KLOG_INFO(TAG) << "==== set item key = " << key << " value = " << value;
    this->preferences->SetSync(key, value);
    this->preferences->Flush();
    return "";
}

void KRSharedPreferencesModule::OnDestroy() {
    // Intentionally left blank
}
}  // namespace expand
}  // namespace kuikly
