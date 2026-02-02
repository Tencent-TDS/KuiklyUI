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
    OH_LOG_INFO(LOG_APP, "KROhSharedPreferencesModule 被采用");
    if (this->preferences == nullptr) {
        std::string options = this->MODULE_NAME;
        if (auto root = GetRootView().lock()) {
            const std::string filesDir = root->GetContext()->Config()->GetFilesDir();
            if (!filesDir.empty()) {
                this->preferences = std::make_shared<util::DataOhPreferences>(filesDir, options);
            }
        }
    }
}

KRAnyValue KROhSharedPreferencesModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                                 const KRRenderCallback &callback) {
    if (method == this->GET_ITEM) {
        return std::make_shared<KRRenderValue>(this->GetItem(params));
    } else if (method == this->SET_ITEM) {
        return std::make_shared<KRRenderValue>(this->SetItem(params));
    }
    return std::make_shared<KRRenderValue>("");
}

std::string KROhSharedPreferencesModule::GetItem(const KRAnyValue &params) {
    InitIfNeeded();
    OH_LOG_INFO(LOG_APP, "KROhSharedPreferencesModule 执行GetItem");
    auto key = params->toString();
    auto value = this->preferences->GetSync(key, "");
    return value;
}

std::string KROhSharedPreferencesModule::SetItem(const KRAnyValue &params) {
    InitIfNeeded();
    OH_LOG_INFO(LOG_APP, "KROhSharedPreferencesModule 执行SetItem");
    auto jsonObj = util::JSONObject::Parse(params->toString());
    std::string key = jsonObj->GetString("key");
    std::string value = jsonObj->GetString("value");
    this->preferences->SetSync(key, value);
    return "";
}

void KROhSharedPreferencesModule::OnDestroy() {
    if (this->preferences) {
        this->preferences.reset();  // 关联Preferences的引用计数为零时，会自动调用KRPreferences.h中的~DataPreference()析构函数
    }
}
}  // namespace expand
}  // namespace kuikly