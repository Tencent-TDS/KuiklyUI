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

#ifndef OHOSAPP_KRMEMORYDATA_H
#define OHOSAPP_KRMEMORYDATA_H

#include <string>
#include <vector>
#include <mutex>

// todo envHeap占位，待补充
class KRMemoryData {
 public:
    KRMemoryData(long long pss, long long env_heap);
    void OnInit(long long pss, long long env_heap);
    std::string ToJSONString();
    bool IsValid();
    void Record(long long pss, long long env_eap);
    long long GetMaxPss();
    long long GetMaxEnvHeap();
    long long GetMaxPssIncrement();
    long long GetMaxEnvHeapIncrement();
    long long GetFirstPssIncrement();
    long long GetFirstDeltaEnvHeap();
    long long GetAvgPss();
    long long GetAvgPssIncrement();
    
 private:
    uint32_t init_pss_ = 0;
    long long init_env_heap_ = 0;
    std::vector<long long> pss_list_;
    std::vector<long long> env_heap_list_;
    std::mutex mutex_;
};

#endif //OHOSAPP_KRMEMORYDATA_H
