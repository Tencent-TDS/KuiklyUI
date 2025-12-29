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

#include "KRFrameMonitor.h"
#include "libohos_render/utils/KRRenderLoger.h"

const char KRFrameMonitor::kMonitorName[] = "KRFrameMonitor";
constexpr char kTag[] = "KRFrameMonitor";

KRFrameMonitor::KRFrameMonitor() {
    native_vsync_ = OH_NativeVSync_Create(kMonitorName, strlen(kMonitorName));
}

KRFrameMonitor::~KRFrameMonitor() {
    Stop();
    if (native_vsync_) {
        OH_NativeVSync_Destroy(native_vsync_);
        native_vsync_ = nullptr;
    }
}

void KRFrameMonitor::OnFirstFramePaint() {
    Start();
}

void KRFrameMonitor::Start() {
    KR_LOG_INFO_WITH_TAG(kTag) << "Start";
    if (is_started_) {
        KR_LOG_INFO_WITH_TAG(kTag) << "has start before.";
        return;
    }
    is_started_ = true;
    is_resumed_ = true;
    last_frame_time_nanos_ = 0;
    frame_data_.Reset();
    RequestNextVSync();
}

void KRFrameMonitor::Stop() {
    if (!is_started_) {
        KR_LOG_INFO_WITH_TAG(kTag) << "stop, not start yet.";
        return;
    }
    is_started_ = false;
    is_resumed_ = false;
    last_frame_time_nanos_ = 0;
}

void KRFrameMonitor::OnResume() {
    if (!is_started_ || is_resumed_) {
        KR_LOG_INFO_WITH_TAG(kTag) << "resume, isStarted: " << is_started_ << "isResumed: " << is_resumed_;
        return;
    }
    is_resumed_ = true;
    last_frame_time_nanos_ = 0;
    RequestNextVSync();
}

void KRFrameMonitor::OnPause() {
    if (!is_started_ || is_resumed_) {
        KR_LOG_INFO_WITH_TAG(kTag) << "pause, isStarted: " << is_started_ << "isResumed: " << is_resumed_;
        return;   
    }
    last_frame_time_nanos_ = 0;
    is_resumed_ = false;
}

void KRFrameMonitor::OnDestroy() {
    Stop();
}

std::string KRFrameMonitor::GetMonitorData() {
    return frame_data_.ToJSONString();
}

void KRFrameMonitor::RequestNextVSync() {
    if (native_vsync_ && is_started_ && is_resumed_) {
        OH_NativeVSync_RequestFrame(native_vsync_, &KRFrameMonitor::OnVSync, this);
    }
}

void KRFrameMonitor::OnVSync(long long timestamp, void* data) {
    auto* monitor = static_cast<KRFrameMonitor*>(data);
    if (!monitor || !monitor->is_started_ || !monitor->is_resumed_) {
        return;
    }
    if (monitor->last_frame_time_nanos_ > 0) {
        long long frameDuration = timestamp - monitor->last_frame_time_nanos_;
        // 1. 累计总耗时
        long long frameDurationMillis = frameDuration / ONE_MILLI_SECOND_IN_NANOS;
        monitor->frame_data_.total_duration += frameDuration / ONE_MILLI_SECOND_IN_NANOS;
        monitor->frame_data_.frame_count++;
        // 2. 计算掉帧/卡顿
        // 如果一帧超过 16.6ms，多出来的部分记为卡顿
        long long hitches = 0;
        if (frameDuration > FRAME_INTERVAL_NANOS) {
            hitches = (frameDuration - FRAME_INTERVAL_NANOS) / ONE_MILLI_SECOND_IN_NANOS;
            monitor->frame_data_.hitches_duration += hitches;
        }

    }
    monitor->last_frame_time_nanos_ = timestamp;
    // 请求下一帧
    monitor->RequestNextVSync();
}