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

#ifndef CORE_RENDER_OHOS_KRFRAMEMONITOR_H
#define CORE_RENDER_OHOS_KRFRAMEMONITOR_H

#include "libohos_render/performance/KRMonitor.h"
#include "libohos_render/performance/frame/KRFrameData.h"
#include <native_vsync/native_vsync.h>
#include <string>
#include <mutex>

class KRFrameMonitor : public KRMonitor {
public:
    KRFrameMonitor();
    virtual ~KRFrameMonitor();

    void OnFirstFramePaint() override;
    void OnResume() override;
    void OnPause() override;
    void OnDestroy() override;
    std::string GetMonitorData() override;
    
    static const char kMonitorName[];

private:
    void Start();
    void Stop();
    void RequestNextVSync();
    
    // VSync 回调函数
    static void OnVSync(long long timestamp, void* data);

private:
    static constexpr long FRAME_INTERVAL_NANOS = 16666667L; // 60fps 的刷新间隔（ns）
    static constexpr long ONE_MILLI_SECOND_IN_NANOS = 1000000L; // 1毫秒的纳秒表示 1ms = 1000,000 ns
    
    KRFrameData frame_data_;
    std::mutex mutex_;
    bool is_started_ = false;
    bool is_resumed_ = false;
    long long last_frame_time_nanos_ = 0;
    long long pending_duration_nanos_ = 0;
    OH_NativeVSync* native_vsync_ = nullptr;
};

#endif //CORE_RENDER_OHOS_KRFRAMEMONITOR_H