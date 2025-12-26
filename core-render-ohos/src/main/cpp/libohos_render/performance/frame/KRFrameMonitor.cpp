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
    nativeVSync_ = OH_NativeVSync_Create(kMonitorName, strlen(kMonitorName));
}

KRFrameMonitor::~KRFrameMonitor() {
    Stop();
    if (nativeVSync_) {
        OH_NativeVSync_Destroy(nativeVSync_);
        nativeVSync_ = nullptr;
    }
}

void KRFrameMonitor::OnFirstFramePaint() {
    Start();
}

void KRFrameMonitor::Start() {
    KR_LOG_INFO_WITH_TAG(kTag) << "Start";
    if (isStarted_) {
        KR_LOG_INFO_WITH_TAG(kTag) << "has start before.";
        return;
    }
    isStarted_ = true;
    isResumed_ = true;
    lastFrameTimeNanos_ = 0;
    frameData_.Reset();
    RequestNextVSync();
}

void KRFrameMonitor::Stop() {
    if (!isStarted_) {
        KR_LOG_INFO_WITH_TAG(kTag) << "stop, not start yet.";
        return;
    }
    isStarted_ = false;
    isResumed_ = false;
    lastFrameTimeNanos_ = 0;
}

void KRFrameMonitor::OnResume() {
    if (!isStarted_ || isResumed_) {
        KR_LOG_INFO_WITH_TAG(kTag) << "resume, isStarted: " << isStarted_ << "isResumed: " << isResumed_;
        return;
    }
    isResumed_ = true;
    lastFrameTimeNanos_ = 0;
    RequestNextVSync();
}

void KRFrameMonitor::OnPause() {
    if (!isStarted_ || isResumed_) {
        KR_LOG_INFO_WITH_TAG(kTag) << "pause, isStarted: " << isStarted_ << "isResumed: " << isResumed_;
        return;   
    }
    lastFrameTimeNanos_ = 0;
    isResumed_ = false;
}

void KRFrameMonitor::OnDestroy() {
    Stop();
}

std::string KRFrameMonitor::GetMonitorData() {
    return frameData_.ToJSONString();
}

void KRFrameMonitor::RequestNextVSync() {
    if (nativeVSync_ && isStarted_ && isResumed_) {
        OH_NativeVSync_RequestFrame(nativeVSync_, &KRFrameMonitor::OnVSync, this);
    }
}

void KRFrameMonitor::OnVSync(long long timestamp, void* data) {
    auto* monitor = static_cast<KRFrameMonitor*>(data);
    if (!monitor || !monitor->isStarted_ || !monitor->isResumed_) {
        return;
    }
    if (monitor->lastFrameTimeNanos_ > 0) {
        long long frameDuration = timestamp - monitor->lastFrameTimeNanos_;
        // 1. 累计总耗时
        long long frameDurationMillis = frameDuration / ONE_MILLI_SECOND_IN_NANOS;
        monitor->frameData_.totalDuration += frameDuration / ONE_MILLI_SECOND_IN_NANOS;
        monitor->frameData_.frameCount++;
        // 2. 计算掉帧/卡顿
        // 如果一帧超过 16.6ms，多出来的部分记为卡顿
        long long hitches = 0;
        if (frameDuration > FRAME_INTERVAL_NANOS) {
            hitches = (frameDuration - FRAME_INTERVAL_NANOS) / ONE_MILLI_SECOND_IN_NANOS;
            monitor->frameData_.hitchesDuration += hitches;
        }

    }
    monitor->lastFrameTimeNanos_ = timestamp;
    // 请求下一帧
    monitor->RequestNextVSync();
}