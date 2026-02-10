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
#include <cstdint>
#include <fcntl.h>
#include <fstream>
#include <thread>
#include "thirdparty/tinyXml/tinyxml2.h"
#include <database/preferences/oh_preferences.h>
#include <database/preferences/oh_preferences_option.h>
#include <database/preferences/oh_preferences_err_code.h>


namespace kuikly {
namespace util {


// 实例化Preferences
DataOhPreferences::DataOhPreferences(const std::string &bundleName, const std::string &filesName) {
    // 创建Preferences的配置选项
    OH_PreferencesOption *option = OH_PreferencesOption_Create();
    if (!option) {
//        KLOG_ERROR(TAG) << "Failed to create PreferencesOption";
        return;
    }
    // 设置配置选中 文件名参数
    int ret = OH_PreferencesOption_SetFileName(option, filesName.c_str());
    if (ret != PREFERENCES_OK) {
//        KLOG_ERROR(TAG) << "Failed to set FileName, ret: " << ret;
        OH_PreferencesOption_Destroy(option);
        return;
    }
    // 设置配置选项中 应用组ID参数
    ret = OH_PreferencesOption_SetDataGroupId(option, "");
    if (ret != PREFERENCES_OK) {
//        KLOG_ERROR(TAG) << "Failed to set DataGroupId, ret: " << ret;
        OH_PreferencesOption_Destroy(option);
        return;
    }
    // 设置配置选项中 包名称参数
    ret = OH_PreferencesOption_SetBundleName(option, bundleName.c_str());
    if (ret != PREFERENCES_OK) {
        OH_PreferencesOption_Destroy(option);
//         KLOG_ERROR(TAG) << "SetBundleName failed: " << ret;
        return;
    }
    
    // 配置选项设置完成，实例化全局Preferences实例
    int errCode = PREFERENCES_OK;
    this->preferences_ = OH_Preferences_Open(option, &errCode);
    OH_PreferencesOption_Destroy(option);
    option = nullptr;
    
    if (!this->preferences_ || errCode != PREFERENCES_OK) {
        return;
    }
}

// 回收Preferences
DataOhPreferences::~DataOhPreferences() {
    if (this->preferences_) {
        OH_Preferences_Close(preferences_);
    }
} 

// Preferences写入键值对，以锁机制维持线程环境下并发访问安全
void DataOhPreferences::SetSync(const std::string &key, const std::string &value) {
    std::unique_lock<std::mutex> lock(this->mtx_);
    if (!this->preferences_) {
        lock.unlock();
        return;
    }
    // 向首选项Preferences中写入键值对
    int ret = OH_Preferences_SetString(this->preferences_, key.c_str(), value.c_str());
    if (ret != PREFERENCES_OK) {
        // 异常情况处理
    }
    lock.unlock();
}
// Preferences读出键值对，以锁机制维持线程环境下并发访问安全
std::string DataOhPreferences::GetSync(const std::string &key, const std::string &defaultValue) {
    std::unique_lock<std::mutex> lock(this->mtx_);
    if (!this->preferences_) {
        lock.unlock();
        return defaultValue;
    }
    
    char *value = nullptr;
    uint32_t len = 0;
    int ret = OH_Preferences_GetString(this->preferences_, key.c_str(), &value, &len);
    
    if (ret == PREFERENCES_OK && value) {
        std::string result(value);          
        OH_Preferences_FreeString(value);
        lock.unlock();
        return result;
    }
    lock.unlock();
    return defaultValue;
}


}  //  namespace util
}  //  namespace kuikly

