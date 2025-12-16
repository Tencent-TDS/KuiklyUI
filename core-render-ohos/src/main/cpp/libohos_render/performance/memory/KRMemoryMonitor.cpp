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

#include "KRMemoryMonitor.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/foundation/thread/KRMonitorThread.h"
#include "libohos_render/performance/Memory/KRMemoryData.h"
#include <atomic>
#include <hidebug/hidebug.h>
#include "libohos_render/foundation/type/KRRenderValue.h"

const char KRMemoryMonitor::kMonitorName[] = "KRMemoryMonitor";
constexpr char kTag[] = "KRMemoryMonitor";

KRMemoryMonitor::KRMemoryMonitor(int mode) : memoryData_(0, 0), mode_(mode) {
}

void KRMemoryMonitor::OnFirstFramePaint() {
    Start();
}

void KRMemoryMonitor::OnInit() {
    std::weak_ptr<KRMemoryMonitor> weak_self = shared_from_this();
    KRMonitorThread::GetInstance().PostTask([weak_self]() {
        if (auto self = weak_self.lock()) {
            // 获取初始数据
            long initPss = self->GetPssSize();
            long initEnvHeap = self->GetEnvHeapSize();
            // 初始化 Data
            self->memoryData_.OnInit(initPss, initEnvHeap);
        }
    });
}

void KRMemoryMonitor::Start() {
    if (isStarted_) return;
    KR_LOG_INFO_WITH_TAG(kTag) << "Start";
    isStarted_ = true;
    isResumed_ = true;
    std::weak_ptr<KRMemoryMonitor> weak_self = shared_from_this();
    KRMonitorThread::GetInstance().PostTask([weak_self]() {
        if (auto self = weak_self.lock()) {
            self->DoDumpMemory();
        }
    });
}

void KRMemoryMonitor::OnResume() {
    if (!isStarted_) return;
    if (isResumed_) return;
    KR_LOG_INFO_WITH_TAG(kTag) << "OnResume";
    isResumed_ = true;

    if (dumpMemoryCount_ < MAX_DUMP_MEMORY_COUNT) {
        ScheduleNextDump(); 
    }
}

void KRMemoryMonitor::OnPause() {
    if (!isStarted_) return;
    KR_LOG_INFO_WITH_TAG(kTag) << "OnPause";
    isResumed_ = false; 
}

void KRMemoryMonitor::OnDestroy() {
    KR_LOG_INFO_WITH_TAG(kTag) << "OnDestroy";
    isStarted_ = false;
    isResumed_ = false;
}

void KRMemoryMonitor::DoDumpMemory() {
    if (!isStarted_ || !isResumed_) {
        return;
    }
    long pss = GetPssSize();
    long heap = GetEnvHeapSize();
    KR_LOG_INFO_WITH_TAG(kTag) << "dumpMemory["<< dumpMemoryCount_ <<"]pssSize: " << pss << ", EnvHeap: " << heap;
    memoryData_.Record(pss, heap);
    dumpMemoryCount_++;
    if (dumpMemoryCount_ < MAX_DUMP_MEMORY_COUNT) {
        ScheduleNextDump();
    } else {
        KR_LOG_INFO_WITH_TAG(kTag) << "Dump Memory finished.";
    }
}

void KRMemoryMonitor::ScheduleNextDump() {
    std::weak_ptr<KRMemoryMonitor> weak_self = shared_from_this();
    KRMonitorThread::GetInstance().PostDelayedTask([weak_self]() {
        if (auto self = weak_self.lock()) {
            self->DoDumpMemory();
        }
    }, UPDATE_MEMORY_INTERVAL);
}

std::string KRMemoryMonitor::GetMonitorData() {
    if (memoryData_.IsValid()) {
        return memoryData_.ToJSONString();
    }
    return "{}";
}

long long KRMemoryMonitor::GetPssSize() {
    HiDebug_NativeMemInfo memInfo;
    OH_HiDebug_GetAppNativeMemInfo(&memInfo);
    return memInfo.pss * BYTES_PER_KILOBYTE;
}

long long KRMemoryMonitor::GetEnvHeapSize() {
    // todo 需要根据运行环境区分
    return 0;
}