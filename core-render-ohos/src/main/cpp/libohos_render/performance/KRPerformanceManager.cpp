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

#include "libohos_render/performance/KRPerformanceData.h"

bool KRPerformanceManager::cold_launch_flag = true;
std::list<std::string> KRPerformanceManager::page_record_;

KRPerformanceManager::KRPerformanceManager(std::string page_name, const std::shared_ptr<KRRenderExecuteMode> &mode)
    : page_name_(std::move(page_name)), mode_(mode) {
    auto launch_monitor = std::make_shared<KRLaunchMonitor>();
    monitors_[KRLaunchMonitor::kMonitorName] = launch_monitor;
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
    }
}
void KRPerformanceManager::OnResume() {}
void KRPerformanceManager::OnPause() {}
void KRPerformanceManager::OnDestroy() {}

std::string KRPerformanceManager::GetPerformanceData() {  //  收集所有性能数据
    auto monitor = GetMonitor(KRLaunchMonitor::kMonitorName);
    if (monitor) {
        auto now = std::chrono::system_clock::
            now();  //  这里用system_clock。原因是KuiklyCore回调的页面创建事件都是epoch_time。
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());
        auto spent_time = duration.count() - init_time_stamps_;
        int kuikly_core_mode_value = mode_->ModeToCoreValue();
        KRPerformanceData performance =
            KRPerformanceData(page_name_, kuikly_core_mode_value, spent_time, is_cold_launch, is_page_cold_launch,
                              monitor->GetMonitorData());
        return performance.ToJsonString();
    }
    return "{}";
}

std::shared_ptr<KRMonitor> KRPerformanceManager::GetMonitor(std::string monitor_name) {
    if (monitors_.find(KRLaunchMonitor::kMonitorName) != monitors_.end()) {
        return monitors_[monitor_name];
    }
    return nullptr;
}