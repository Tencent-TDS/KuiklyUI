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

#include "libohos_render/performance/memory/KRMemoryData.h"
#include "thirdparty/cJSON/cJSON.h"

constexpr char kKeyAvgIncrement[] = "avgIncrement";
constexpr char kKeyPeakIncrement[] = "peakIncrement";
constexpr char kKeyAppPeak[] = "appPeak";
constexpr char kKeyAppAvg[] = "appAvg";

KRMemoryData::KRMemoryData(long long pss, long long env_heap): init_pss_(pss), init_env_heap_(env_heap) {}

bool KRMemoryData::IsValid() {
    return true;
    std::lock_guard<std::mutex> lock(mutex_);
    if (init_pss_ <= 0 || pss_list_.empty()) {
        return false;
    }
}

void KRMemoryData::Record(long long pss, long long env_heap) {
    std::lock_guard<std::mutex> lock(mutex_); 
    pss_list_.push_back(pss);
    env_heap_list_.push_back(env_heap);
}

void KRMemoryData::OnInit(long long pss, long long env_heap) {
    init_pss_ = pss;
    init_env_heap_ = env_heap;
}

std::string KRMemoryData::ToJSONString() {
    cJSON *memory_data = cJSON_CreateObject();
    cJSON_AddNumberToObject(memory_data, kKeyAvgIncrement, GetAvgPssIncrement());
    cJSON_AddNumberToObject(memory_data, kKeyPeakIncrement, GetMaxPssIncrement());
    cJSON_AddNumberToObject(memory_data, kKeyAppPeak, GetMaxPss());
    cJSON_AddNumberToObject(memory_data, kKeyAppAvg, GetAvgPss());
    char* json_str = cJSON_Print(memory_data);
    std::string result = json_str;
    cJSON_Delete(memory_data);
    return result;
}

long long KRMemoryData::GetMaxPss() {
    std::lock_guard<std::mutex> lock(mutex_); 
    long long maxPss = 0;
    for (auto &pss : pss_list_) {
        if (pss > maxPss) {
            maxPss = pss;
        }
    }
    return maxPss;
}

long long KRMemoryData::GetMaxEnvHeap() {
    std::lock_guard<std::mutex> lock(mutex_); 
    long long maxKotlinHeap = 0;
    for (auto &kotlinHeap : env_heap_list_) {
        if (kotlinHeap > maxKotlinHeap) {
            maxKotlinHeap = kotlinHeap;
        }
    }
    return maxKotlinHeap;
}

long long KRMemoryData::GetMaxPssIncrement() {
    std::lock_guard<std::mutex> lock(mutex_); 
    long long maxPssIncrement = 0;
    for (auto &pss : pss_list_) {
        long long pssIncrement = pss - init_pss_;
        if (pssIncrement > maxPssIncrement) {
            maxPssIncrement = pssIncrement;
        }
    }
    return maxPssIncrement;
}

long long KRMemoryData::GetMaxEnvHeapIncrement() {
    std::lock_guard<std::mutex> lock(mutex_); 
    long long maxKotlinHeapIncrement = 0;
    for (auto &kotlinHeap : env_heap_list_) {
        long long kotlinHeapIncrement = kotlinHeap - init_env_heap_;
        if (kotlinHeapIncrement > maxKotlinHeapIncrement) {
            maxKotlinHeapIncrement = kotlinHeapIncrement;
        }
    }
    return maxKotlinHeapIncrement;
}

long long KRMemoryData::GetFirstPssIncrement() {
    std::lock_guard<std::mutex> lock(mutex_); 
    if (pss_list_.empty()) {
        return 0;
    }
    return pss_list_[0] - init_pss_;
}

long long KRMemoryData::GetFirstDeltaEnvHeap() {
    std::lock_guard<std::mutex> lock(mutex_); 
    if (env_heap_list_.empty()) {
        return 0;
    }
    return env_heap_list_[0] - init_env_heap_;
}

long long KRMemoryData::GetAvgPss() {
    std::lock_guard<std::mutex> lock(mutex_); 
    if (pss_list_.empty()) {
        return 0;
    }
    long long avgPss = 0;
    for (auto &pss : pss_list_) {
        avgPss += pss;
    }

    return avgPss / pss_list_.size();
}

long long KRMemoryData::GetAvgPssIncrement() {
    std::lock_guard<std::mutex> lock(mutex_); 
    if (pss_list_.empty()) {
        return 0;
    }
    long avgPssIncrement = 0;
    for (auto &pss : pss_list_) {
        long long pssIncrement = pss - init_pss_;
        avgPssIncrement += pssIncrement;
    }
    if (pss_list_.empty()) {
        return 0;
    }
    return avgPssIncrement / pss_list_.size();
}