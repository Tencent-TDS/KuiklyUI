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

#ifndef CORE_RENDER_OHOS_KRPERFORMANCEMANAGER_H
#define CORE_RENDER_OHOS_KRPERFORMANCEMANAGER_H

#include <list>
#include <string>
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/expand/modules/performance/KRPageCreateTrace.h"
#include "libohos_render/performance/launch/KRLaunchMonitor.h"

enum class MonitorType { kLaunch = 0, KFrame = 1, KMemory = 2 };

class KRPerformanceManager {
 public:
    KRPerformanceManager(std::string page_name, const std::shared_ptr<KRRenderExecuteMode> &mode);
    ~KRPerformanceManager();
    void OnKRRenderViewInit();
    void OnInitCoreStart();
    void OnInitCoreFinish();
    void OnInitContextStart();
    void OnInitContextFinish();
    void OnCreateInstanceStart();
    void OnCreateInstanceFinish();
    void OnFirstFramePaint();
    void OnPageCreateFinish(KRPageCreateTrace &trace);
    void OnResume();
    void OnPause();
    void OnDestroy();
    std::string GetPerformanceData();
    std::shared_ptr<KRMonitor> GetMonitor(std::string monitor_name);
    void SetArkLaunchTime(int64_t launch_time);

 private:
    std::string page_name_ = "";
    std::shared_ptr<KRRenderExecuteMode> mode_;
    int64_t init_time_stamps_ = 0;
    bool is_cold_launch = false;       //  是否是冷启动
    bool is_page_cold_launch = false;  //  页面是否是首次启动
    std::unordered_map<std::string, std::shared_ptr<KRMonitor>> monitors_;
    static std::list<std::string> page_record_;  // 静态变量，全局记录页面是否曾经加载过
    static bool cold_launch_flag;                // 静态变量，用于标识进程是否首次启动
};
#endif  // CORE_RENDER_OHOS_KRPERFORMANCEMANAGER_H
