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
#include <mutex>
#include <database/preferences/oh_preferences.h>

namespace kuikly {
namespace util {

/*
 应用沙箱目录:
 https://developer.huawei.com/consumer/cn/doc/harmonyos-guides-V2/app-sandbox-directory-0000001491863498-V2
 鸿蒙js端内部接口 context 用于获取路径
*/
// #define PREFERENCES_PATH "/data/storage/el2/base/haps/entry/CAPIpreferences/"
static const char *OHTAG = __FILE_NAME__;

class DataOhPreferences {
 public:
    DataOhPreferences(const std::string &filesDir, const std::string &filesName);     // 构造函数，构造Preferences
    ~DataOhPreferences();                                                             // 回收函数，回收Preferences
    void SetSync(const std::string &key, const std::string &value);                 // 同步向Preferences写入键值对
    std::string GetSync(const std::string &key, const std::string &defaultValue);   // 同步从Preferences读取键值对


    // 私有成员变量
 private:
    std::mutex mtx_;
    OH_Preferences *preferences_;   // oh_preferences实例
    
};
}  //  namespace util
}  //  namespace kuikly

