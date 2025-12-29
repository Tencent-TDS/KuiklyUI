
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
#include "libohos_render/performance/memory/KRMemoryData.h"
#include <atomic>
#include <hidebug/hidebug.h>
#include "libohos_render/foundation/ffrt/KRFfrt.h"
#include "libohos_render/foundation/type/KRRenderValue.h"

const char KRMemoryMonitor::kMonitorName[] = "KRMemoryMonitor";
constexpr char kTag[] = "KRMemoryMonitor";

KRMemoryMonitor::KRMemoryMonitor(int mode) : memory_data_(0, 0), mode_(mode) {
}

void KRMemoryMonitor::OnFirstFramePaint() {
    Start();
}

void KRMemoryMonitor::OnInit() {
    SubmitMonitorTask(MemoryTaskType::INIT);
}

void KRMemoryMonitor::Start() {
    if (is_started_) return;
    KR_LOG_INFO_WITH_TAG(kTag) << "Start";
    is_started_ = true;
    is_resumed_ = true;
    SubmitMonitorTask(MemoryTaskType::DUMP_MEMORY);
}

void KRMemoryMonitor::OnResume() {
    if (!is_started_) return;
    if (is_resumed_) return;
    KR_LOG_INFO_WITH_TAG(kTag) << "OnResume";
    is_resumed_ = true;

    if (dump_memory_count_ < MAX_DUMP_MEMORY_COUNT) {
        ScheduleNextDump(); 
    }
}

void KRMemoryMonitor::OnPause() {
    if (!is_started_) return;
    KR_LOG_INFO_WITH_TAG(kTag) << "OnPause";
    is_resumed_ = false; 
}

void KRMemoryMonitor::OnDestroy() {
    KR_LOG_INFO_WITH_TAG(kTag) << "OnDestroy";
    is_started_ = false;
    is_resumed_ = false;
}

void KRMemoryMonitor::DoDumpMemory() {
    if (!is_started_ || !is_resumed_) {
        return;
    }
    long pss = GetPssSize();
    long heap = GetEnvHeapSize();
    KR_LOG_INFO_WITH_TAG(kTag) << "dumpMemory["<< dump_memory_count_ <<"]pssSize: " << pss << ", EnvHeap: " << heap;
    memory_data_.Record(pss, heap);
    dump_memory_count_++;
    if (dump_memory_count_ < MAX_DUMP_MEMORY_COUNT) {
        ScheduleNextDump();
    } else {
        KR_LOG_INFO_WITH_TAG(kTag) << "Dump Memory finished.";
    }
}

void KRMemoryMonitor::ScheduleNextDump() {
    SubmitMonitorTask(MemoryTaskType::DUMP_MEMORY, UPDATE_MEMORY_INTERVAL);
}

std::string KRMemoryMonitor::GetMonitorData() {
    if (memory_data_.IsValid()) {
        return memory_data_.ToJSONString();
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

void KRMemoryMonitor::ExecuteMemoryTask(void* arg) {
    auto* task_param = static_cast<MonitorTaskParam*>(arg);
    if (!task_param) return;
    try {
        if (auto monitor = task_param->monitor_weak_ptr.lock()) {
            switch (task_param->type) {
                case MemoryTaskType::INIT: {
                    long initPss = monitor->GetPssSize();
                    long initEnvHeap = monitor->GetEnvHeapSize();
                    monitor->memory_data_.OnInit(initPss, initEnvHeap);
                    break;
                }
                case MemoryTaskType::DUMP_MEMORY: {
                    monitor->DoDumpMemory();
                    break;
                }
            }
        }
    } catch (...) {
        KR_LOG_ERROR_WITH_TAG(kTag) << "ExecuteMonitorTask error ";
    }
}

void KRMemoryMonitor::CleanupMemoryTask(void* arg) {
    auto* task_param = static_cast<MonitorTaskParam*>(arg);
    delete task_param;
}

void KRMemoryMonitor::SubmitMonitorTask(MemoryTaskType type, long delay_ms) {
    std::weak_ptr<KRMemoryMonitor> weak_self = shared_from_this();
    auto* param = new MonitorTaskParam(type, std::move(weak_self));
    ffrt_task_attr_t attr;
    ffrt_task_attr_init(&attr);
    if (delay_ms > 0) {
        ffrt_task_attr_set_delay(&attr, delay_ms * 1000); // 毫秒转纳秒
    }
    ffrt_submit_base(
        ffrt_create_function_wrapper(ExecuteMemoryTask, CleanupMemoryTask, param, ffrt_function_kind_general), 
        NULL, NULL, &attr);
    ffrt_task_attr_destroy(&attr);
}