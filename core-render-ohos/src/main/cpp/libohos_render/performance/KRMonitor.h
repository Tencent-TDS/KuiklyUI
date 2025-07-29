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

#ifndef CORE_RENDER_OHOS_KRMONITOR_H
#define CORE_RENDER_OHOS_KRMONITOR_H

#include <string>

/**
 * 采集监控数据基类
 */
class KRMonitor {
 public:
    KRMonitor();
    virtual ~KRMonitor();
    virtual void OnKRRenderViewInit();
    virtual void OnInitCoreStart();
    virtual void OnInitCoreFinish();
    virtual void OnInitContextStart();
    virtual void OnInitContextFinish();
    virtual void OnCreateInstanceStart();
    virtual void OnCreateInstanceFinish();
    virtual void OnFirstFramePaint();
    virtual void OnResume();
    virtual void OnPause();
    virtual void OnDestroy();
    virtual std::string GetMonitorData() = 0;
    virtual void SetArkLaunchTime(int64_t timestamp);
    //    virtual void OnRenderException();
};
#endif  // CORE_RENDER_OHOS_KRMONITOR_H
