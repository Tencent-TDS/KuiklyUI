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

#include "KRPerformanceManager.h"

#include "libohos_render/expand/modules/performance/KRPerformanceModule.h"
#include "libohos_render/manager/KRArkTSManager.h"
#include "libohos_render/performance/KRPerformanceData.h"
#include "libohos_render/performance/frame/KRFrameMonitor.h"
#include "libohos_render/performance/memory//KRMemoryMonitor.h"
#include "libohos_render/scheduler/KRContextScheduler.h"

static constexpr char ON_GET_LAUNCH_DATA_DATA[] = "onGetLaunchData";
static constexpr char ON_GET_PERFORMANCE_DATA_DATA[] = "onGetPerformanceData";

bool KRPerformanceManager::cold_launch_flag = true;
std::list<std::string> KRPerformanceManager::page_record_;

KRPerformanceManager::KRPerformanceManager(int performance_monitor_types_mask, std::string page_name, std::string instance_id, const std::shared_ptr<KRRenderExecuteMode> &mode)
    : performance_monitor_types_mask_(performance_monitor_types_mask), page_name_(std::move(page_name)), instance_id_(std::move(instance_id)), mode_(mode) {
    if (performance_monitor_types_mask_ & kMonitorTypeLaunch) {
        auto launch_monitor = std::make_shared<KRLaunchMonitor>();
        monitors_[KRLaunchMonitor::kMonitorName] = launch_monitor;
    }
    if (performance_monitor_types_mask_ & kMonitorTypeFrame) {
        auto frame_monitor = std::make_shared<KRFrameMonitor>();
        monitors_[KRFrameMonitor::kMonitorName] = frame_monitor;
    }
    if (performance_monitor_types_mask_ & kMonitorTypeMemory) {
        auto memory_monitor = std::make_shared<KRMemoryMonitor>(mode->GetMode());
        monitors_[KRMemoryMonitor::kMonitorName] = memory_monitor;
        memory_monitor->OnInit(); // 内存初始化
    }
    auto it = std::find(page_record_.begin(), page_record_.end(), page_name_);
    if (it == page_record_.end()) {  //  页面未曾加载过
        is_page_cold_launch = true;
        page_record_.push_front(page_name_);
    }
}

KRPerformanceManager::~KRPerformanceManager() {}

void KRPerformanceManager::SetArkLaunchTime(int64_t launch_time) {  //  ArkTS层页面加载启动事件
    init_time_stamps_ = launch_time;
    if (auto monitor = GetMonitor(KRLaunchMonitor::kMonitorName)) {  //  这个事件只有LaunchMonitor关注
        monitor->SetArkLaunchTime(launch_time);
    }
}

void KRPerformanceManager::OnKRRenderViewInit() {
    if (cold_launch_flag) {
        cold_launch_flag = false;
        is_cold_launch = true;
    }
    for (const auto &monitor : monitors_) {
        monitor.second->OnKRRenderViewInit();
    }
}

void KRPerformanceManager::OnInitCoreStart() {
    for (const auto &monitor : monitors_) {
        monitor.second->OnInitCoreStart();
    }
}
void KRPerformanceManager::OnInitCoreFinish() {
    for (const auto &monitor : monitors_) {
        monitor.second->OnInitCoreFinish();
    }
}
void KRPerformanceManager::OnInitContextStart() {
    for (const auto &monitor : monitors_) {
        monitor.second->OnInitContextStart();
    }
}
void KRPerformanceManager::OnInitContextFinish() {
    for (const auto &monitor : monitors_) {
        monitor.second->OnInitContextFinish();
    }
}
void KRPerformanceManager::OnCreateInstanceStart() {
    for (const auto &monitor : monitors_) {
        monitor.second->OnCreateInstanceStart();
    }
}
void KRPerformanceManager::OnCreateInstanceFinish() {
    for (const auto &monitor : monitors_) {
        monitor.second->OnCreateInstanceFinish();
    }
}
void KRPerformanceManager::OnFirstFramePaint() {
    for (const auto &monitor : monitors_) {
        monitor.second->OnFirstFramePaint();
    }
}

void KRPerformanceManager::OnPageCreateFinish(KRPageCreateTrace &trace) {  //  KuiklyCore回调的页面创建事件
    auto monitor = GetMonitor(KRLaunchMonitor::kMonitorName);
    if (monitor) {
        auto launch_monitor = std::static_pointer_cast<KRLaunchMonitor>(monitor);
        launch_monitor->OnPageCreateFinish(trace);
        OnLaunchResult();
    }
}

void KRPerformanceManager::OnResume() {
    for (auto &monitor: monitors_) {
        monitor.second->OnResume();
    }
}

void KRPerformanceManager::OnPause() {
    for (auto &monitor: monitors_) {
        monitor.second->OnPause();
    }
}

void KRPerformanceManager::OnDestroy() {
    for (auto &monitor: monitors_) {
        monitor.second->OnDestroy();
    }
    if (!monitors_.empty()) {
        OnResult();
    }
}

std::string KRPerformanceManager::GetInstanceId() {
    return instance_id_;
}

std::string KRPerformanceManager::GetLaunchData() {  //  收集启动数据
    auto monitor = GetMonitor(KRLaunchMonitor::kMonitorName);
    if (monitor) {
        return monitor->GetMonitorData();
    }
    return "{}";
}

std::string KRPerformanceManager::GetFrameData() {  //  获取帧数据
    auto monitor = GetMonitor(KRFrameMonitor::kMonitorName);
    if (monitor) {
        auto temp = monitor->GetMonitorData();
        return monitor->GetMonitorData();
    }
    return "";
}

std::string KRPerformanceManager::GetMemoryData() {  //  获取内存数据
    auto monitor = GetMonitor(KRMemoryMonitor::kMonitorName);
    if (monitor) {
        return monitor->GetMonitorData();
    }
    return "";
}

std::string KRPerformanceManager::GetPerformanceData() {  //  收集所有性能数据
    if (performance_monitor_types_mask_ == 0) {
        return "";
    }
    
    auto now = std::chrono::system_clock::
        now();  //  这里用system_clock。原因是KuiklyCore回调的页面创建事件都是epoch_time。
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());
    auto spent_time = duration.count() - init_time_stamps_;
    int kuikly_core_mode_value = mode_->ModeToCoreValue();
    auto launch_data = GetLaunchData();
    auto frame_data = GetFrameData();
    auto memory_data = GetMemoryData();
    auto monitor = GetMonitor(KRLaunchMonitor::kMonitorName);
    KRPerformanceData performance =
        KRPerformanceData(page_name_, kuikly_core_mode_value, spent_time, is_cold_launch, is_page_cold_launch,
                              launch_data, frame_data, memory_data);
    return performance.ToJsonString();
}

std::shared_ptr<KRMonitor> KRPerformanceManager::GetMonitor(std::string monitor_name) {
    if (monitors_.find(monitor_name) != monitors_.end()) {
        return monitors_[monitor_name];
    }
    return nullptr;
}

void KRPerformanceManager::OnLaunchResult() {
    auto data = GetLaunchData();
    CallArkTsPerformanceModule(ON_GET_LAUNCH_DATA_DATA, data);
}

void KRPerformanceManager::OnResult() {
    auto data = GetPerformanceData();
    CallArkTsPerformanceModule(ON_GET_PERFORMANCE_DATA_DATA, data);
}

void KRPerformanceManager::CallArkTsPerformanceModule(const char* method_name, std::string &data) {
    auto instance_id = GetInstanceId();
    KRContextScheduler::ScheduleTaskOnMainThread(false, [instance_id, method_name, data] {
        KRArkTSManager::GetInstance().CallArkTSMethod(instance_id, KRNativeCallArkTSMethod::CallModuleMethod,
            NewKRRenderValue("KRPerformanceModule"), NewKRRenderValue(method_name),
            NewKRRenderValue(data), nullptr, nullptr, nullptr);
    });
}