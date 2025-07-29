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

#ifndef CORE_RENDER_OHOS_KRPERFORMANCEDATA_H
#define CORE_RENDER_OHOS_KRPERFORMANCEDATA_H

#include <string>

/**
 * 该类用于组织所有性能采集的数据对外输出
 */
class KRPerformanceData {
 public:
    KRPerformanceData(std::string page_name, int excute_mode, int spent_time, bool is_cold_launch,
                      bool is_page_cold_launch, std::string lanch_data);
    std::string ToJsonString();

 private:
    std::string page_name_;
    int64_t spent_time_;
    bool is_cold_launch_;
    bool is_page_cold_launch_;
    int excute_mode_;
    std::string launch_data_ = "{}";
};
#endif  // CORE_RENDER_OHOS_KRPERFORMANCEDATA_H
