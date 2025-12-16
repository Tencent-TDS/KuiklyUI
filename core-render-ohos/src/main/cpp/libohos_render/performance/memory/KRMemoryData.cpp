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

#include "libohos_render/performance/Memory/KRMemoryData.h"
#include "thirdparty/cJSON/cJSON.h"

constexpr char kKeyAvgIncrement[] = "avgIncrement";
constexpr char kKeyPeakIncrement[] = "peakIncrement";
constexpr char kKeyAppPeak[] = "appPeak";
constexpr char kKeyAppAvg[] = "appAvg";

KRMemoryData::KRMemoryData(long long pss, long long env_heap): initPss_(pss), initEnvHeap_(env_heap) {}

bool KRMemoryData::IsValid() {
    return true;
    std::lock_guard<std::mutex> lock(mutex_);
    if (initPss_ <= 0 || pasList_.empty()) {
        return false;
    }
}

void KRMemoryData::Record(long long pss, long long env_heap) {
    std::lock_guard<std::mutex> lock(mutex_); 
    pasList_.push_back(pss);
    envHeapList_.push_back(env_heap);
}

void KRMemoryData::OnInit(long long pss, long long env_heap) {
    initPss_ = pss;
    initEnvHeap_ = env_heap;
}

std::string KRMemoryData::ToJSONString() {
    cJSON *memory_data = cJSON_CreateObject();
    cJSON_AddNumberToObject(memory_data, kKeyAvgIncrement, GetAvgPssIncrement());
    cJSON_AddNumberToObject(memory_data, kKeyPeakIncrement, GetMaxPssIncrement());
    cJSON_AddNumberToObject(memory_data, kKeyAppPeak, GetMaxPss());
    cJSON_AddNumberToObject(memory_data, kKeyAppAvg, GetAvgPss());
    std::string result = cJSON_Print(memory_data);
    cJSON_Delete(memory_data);
    return result;
}

long long KRMemoryData::GetMaxPss() {
    std::lock_guard<std::mutex> lock(mutex_); 
    long long maxPss = 0;
    for (auto &pss : pasList_) {
        if (pss > maxPss) {
            maxPss = pss;
        }
    }
    return maxPss;
}

long long KRMemoryData::GetMaxEnvHeap() {
    std::lock_guard<std::mutex> lock(mutex_); 
    long long maxKotlinHeap = 0;
    for (auto &kotlinHeap : envHeapList_) {
        if (kotlinHeap > maxKotlinHeap) {
            maxKotlinHeap = kotlinHeap;
        }
    }
    return maxKotlinHeap;
}

long long KRMemoryData::GetMaxPssIncrement() {
    std::lock_guard<std::mutex> lock(mutex_); 
    long long maxPssIncrement = 0;
    for (auto &pss : pasList_) {
        long long pssIncrement = pss - initPss_;
        if (pssIncrement > maxPssIncrement) {
            maxPssIncrement = pssIncrement;
        }
    }
    return maxPssIncrement;
}

long long KRMemoryData::GetMaxEnvHeapIncrement() {
    std::lock_guard<std::mutex> lock(mutex_); 
    long long maxKotlinHeapIncrement = 0;
    for (auto &kotlinHeap : envHeapList_) {
        long long kotlinHeapIncrement = kotlinHeap - initEnvHeap_;
        if (kotlinHeapIncrement > maxKotlinHeapIncrement) {
            maxKotlinHeapIncrement = kotlinHeapIncrement;
        }
    }
    return maxKotlinHeapIncrement;
}

long long KRMemoryData::GetFirstPssIncrement() {
    std::lock_guard<std::mutex> lock(mutex_); 
    if (pasList_.empty()) {
        return 0;
    }
    return pasList_[0] - initPss_;
}

long long KRMemoryData::GetFirstDeltaEnvHeap() {
    std::lock_guard<std::mutex> lock(mutex_); 
    if (envHeapList_.empty()) {
        return 0;
    }
    return envHeapList_[0] - initEnvHeap_;
}

long long KRMemoryData::GetAvgPss() {
    std::lock_guard<std::mutex> lock(mutex_); 
    if (pasList_.empty()) {
        return 0;
    }
    long long avgPss = 0;
    for (auto &pss : pasList_) {
        avgPss += pss;
    }

    return avgPss / pasList_.size();
}

long long KRMemoryData::GetAvgPssIncrement() {
    std::lock_guard<std::mutex> lock(mutex_); 
    if (pasList_.empty()) {
        return 0;
    }
    long avgPssIncrement = 0;
    for (auto &pss : pasList_) {
        long long pssIncrement = pss - initPss_;
        avgPssIncrement += pssIncrement;
    }
    if (pasList_.empty()) {
        return 0;
    }
    return avgPssIncrement / pasList_.size();
}