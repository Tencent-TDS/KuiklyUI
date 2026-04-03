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

#include "KROhPreferences.h"
#include "KROhSharedPreferencesModule.h"
#include "libohos_render/utils/KRJSONObject.h"
#include "hilog/log.h"

#undef LOG_DOMAIN
#undef LOG_TAG
#define LOG_DOMAIN 0x3200  // 全局domain宏，标识业务领域
#define LOG_TAG "KROhSharedPreferencesModule"   // 全局tag宏，标识模块日志tag

namespace kuikly {
namespace expand {
const char KROhSharedPreferencesModule::MODULE_NAME[] = "KROhSharedPreferencesModule";
const char KROhSharedPreferencesModule::GET_ITEM[] = "getItem";
const char KROhSharedPreferencesModule::SET_ITEM[] = "setItem";
const char KROhSharedPreferencesModule::BUNDLE_NAME[] = "com.tencent.kuiklyharmonyapp";

bool KROhSharedPreferencesModule::SyncMode() {
    return true;
}

void KROhSharedPreferencesModule::InitIfNeeded() {
    if (this->preferences == nullptr) {
        std::string options = this->MODULE_NAME;
        if (auto root = GetRootView().lock()) {
            const std::string filesDir = root->GetContext()->Config()->GetFilesDir();
            if (!filesDir.empty()) {
                this->preferences = util::DataOhPreferences::GetInstance(BUNDLE_NAME, options);
            }
        }
    }
}

KRAnyValue KROhSharedPreferencesModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                                 const KRRenderCallback &callback) {
    if (method == this->GET_ITEM) {
        return KRRenderValue::Make(this->GetItem(params));
    } else if (method == this->SET_ITEM) {
        return KRRenderValue::Make(this->SetItem(params));
    }
    return KRRenderValue::Make("");
}

std::string KROhSharedPreferencesModule::GetItem(const KRAnyValue &params) {
    InitIfNeeded();
    auto key = params->toString();
    auto value = this->preferences->GetSync(key, "");
    return value;
}

std::string KROhSharedPreferencesModule::SetItem(const KRAnyValue &params) {
    InitIfNeeded();
    auto jsonObj = util::JSONObject::Parse(params->toString());
    std::string key = jsonObj->GetString("key");
    std::string value = jsonObj->GetString("value");
    this->preferences->SetSync(key, value);
    return "";
}

void KROhSharedPreferencesModule::OnDestroy() {
    // Intentionally left blank
}
}  // namespace expand
}  // namespace kuikly