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

#include "KRPerformanceData.h"

#include "thirdparty/cJSON/cJSON.h"

constexpr char kKeyMode[] = "mode";
constexpr char kKeyPageExistTime[] = "pageExistTime";
constexpr char kKeyIsFirstPageProcess[] = "isFirstLaunchOfProcess";
constexpr char kKeyIsFirstPageLaunch[] = "isFirstLaunchOfPage";
constexpr char kKeyMainFPS[] = "mainFPS";
constexpr char kKeyKotlinFPS[] = "kotlinFPS";
constexpr char kKeyMemory[] = "memory";
constexpr char kKeyPageLoadTime[] = "pageLoadTime";

KRPerformanceData::KRPerformanceData(std::string page_name, int excute_mode, int spent_time, bool is_cold_launch,
                                     bool is_page_cold_launch, std::string launch_data)
    : page_name_(page_name), excute_mode_(excute_mode), spent_time_(spent_time), is_cold_launch_(is_cold_launch),
      is_page_cold_launch_(is_page_cold_launch), launch_data_(launch_data) {}

std::string KRPerformanceData::ToJsonString() {
    cJSON *performance_data = cJSON_CreateObject();
    cJSON_AddNumberToObject(performance_data, kKeyMode, excute_mode_);
    cJSON_AddNumberToObject(performance_data, kKeyPageExistTime, spent_time_);
    cJSON_AddBoolToObject(performance_data, kKeyIsFirstPageProcess, is_cold_launch_);
    cJSON_AddBoolToObject(performance_data, kKeyIsFirstPageLaunch, is_page_cold_launch_);
    cJSON_AddStringToObject(performance_data, kKeyPageLoadTime, launch_data_.c_str());
    std::string result = cJSON_Print(performance_data);
    cJSON_Delete(performance_data);
    return result;
}