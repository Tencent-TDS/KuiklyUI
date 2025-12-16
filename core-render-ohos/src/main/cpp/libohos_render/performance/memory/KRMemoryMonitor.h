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

#ifndef CORE_RENDER_OHOS_KRMEMORYMONITOR_H
#define CORE_RENDER_OHOS_KRMEMORYMONITOR_H

#include "libohos_render/performance/KRMonitor.h"
#include "libohos_render/performance/Memory/KRMemoryData.h"
#include <atomic>


class KRMemoryMonitor: public KRMonitor, public std::enable_shared_from_this<KRMemoryMonitor> {
 public:
    KRMemoryMonitor(int mode);
    virtual ~KRMemoryMonitor() = default;
    
    void OnFirstFramePaint() override;
    void OnResume() override;
    void OnPause() override;
    void OnDestroy() override;
    std::string GetMonitorData() override;
    void OnInit() override;
    static const char kMonitorName[];

 private:
    void Start();
    void DoDumpMemory();
    void ScheduleNextDump();
    /**
     * 获取当前pss内存信息
     */
    long long GetPssSize();
    /**
     * 获取当前运行环境内存信息
     */
    long long GetEnvHeapSize();
    std::atomic<bool> isStarted_{false};
    std::atomic<bool> isResumed_{false};
    int dumpMemoryCount_ = 0;
    static const int MAX_DUMP_MEMORY_COUNT = 10;
    static constexpr int UPDATE_MEMORY_INTERVAL = 10 * 1000;
    static const int BYTES_PER_KILOBYTE = 1024;
    KRMemoryData memoryData_; 
    // 根据运行模式区分获取类型
    int mode_;
};


#endif //CORE_RENDER_OHOS_KRMEMORYMONITOR_H
