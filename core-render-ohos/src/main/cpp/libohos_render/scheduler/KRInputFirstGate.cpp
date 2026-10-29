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

#include "libohos_render/scheduler/KRInputFirstGate.h"

#include <algorithm>
#include <chrono>
#include <cmath>
#include <iterator>

#include "libohos_render/foundation/thread/KRThread.h"
#include "libohos_render/scheduler/KRMainThreadTaskPuller.h"
#include "libohos_render/utils/KRRenderLoger.h"

namespace {

// 一次连续触摸或滚动里，每类采样的上限。超出后丢弃，避免长手势占太多内存。
constexpr size_t kMaxSamplesPerSession = 4096;

// vsync 时间戳与 steady_clock 同为 CLOCK_MONOTONIC，可直接比较。
int64_t NowNs() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
               std::chrono::steady_clock::now().time_since_epoch())
        .count();
}

void AppendSample(std::vector<double> *samples, double value) {
    if (samples->size() < kMaxSamplesPerSession) {
        samples->push_back(value);
    }
}

const char *SessionTypeName(KRInputFirstGate::SessionType type) {
    switch (type) {
        case KRInputFirstGate::SessionType::kTouch:
            return "touch";
        case KRInputFirstGate::SessionType::kScroll:
            return "scroll";
        case KRInputFirstGate::SessionType::kGesture:
            return "gesture";
    }
    return "unknown";
}

// 最近邻秩法分位数（q ∈ (0, 1]）；无样本返回 0。
double Percentile(std::vector<double> samples, double q) {
    if (samples.empty()) {
        return 0;
    }
    std::sort(samples.begin(), samples.end());
    size_t rank = static_cast<size_t>(std::ceil(q * static_cast<double>(samples.size())));
    rank = std::min(std::max<size_t>(rank, 1), samples.size());
    return samples[rank - 1];
}

}  // namespace

KRInputFirstGate &KRInputFirstGate::GetInstance() {
    static KRInputFirstGate gate;
    return gate;
}

void KRInputFirstGate::BeginSession(const void *owner, SessionType type) {
    bool becameActive = false;
    size_t active = 0;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        becameActive = sessions_.empty();
        if (becameActive) {
            input_idle_ = false;
        }
        sessions_.emplace(owner, type);
        active = sessions_.size();
    }
    KR_LOG_INFO_WITH_TAG("KRInputFirstGate")
        << "session begin type=" << SessionTypeName(type) << " active=" << active;
    if (becameActive) {
        KRThread::ExchangeTasksStartedWhileParked();
        KRMainThreadTaskPuller::ExchangeMainPullTaskCount();
        KRMainThreadTaskPuller::OnSessionActiveChanged(true);
    }
}

void KRInputFirstGate::EndSession(const void *owner, SessionType type) {
    std::vector<std::function<void()>> dispatches;
    Metrics finished;
    bool hasFinished = false;
    bool becameInactive = false;
    size_t active = 0;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        const bool wasActive = !sessions_.empty();
        sessions_.erase({owner, type});
        becameInactive = wasActive && sessions_.empty();
        active = sessions_.size();
        dispatches = TakeDeferredIfOutOfSessionLocked();
        hasFinished = TakeFinishedMetricsLocked(&finished);
    }
    KR_LOG_INFO_WITH_TAG("KRInputFirstGate")
        << "session end type=" << SessionTypeName(type) << " active=" << active;
    if (becameInactive) {
        KRMainThreadTaskPuller::OnSessionActiveChanged(false);
    }
    for (auto &dispatch : dispatches) {
        dispatch();
    }
    if (hasFinished) {
        LogMetrics(finished);
    }
}

void KRInputFirstGate::ReleaseSessions(const void *owner) {
    std::vector<std::function<void()>> dispatches;
    Metrics finished;
    bool hasFinished = false;
    bool becameInactive = false;
    size_t active = 0;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        const bool wasActive = !sessions_.empty();
        for (auto it = sessions_.begin(); it != sessions_.end();) {
            it = (it->first == owner) ? sessions_.erase(it) : std::next(it);
        }
        becameInactive = wasActive && sessions_.empty();
        active = sessions_.size();
        dispatches = TakeDeferredIfOutOfSessionLocked();
        hasFinished = TakeFinishedMetricsLocked(&finished);
    }
    KR_LOG_INFO_WITH_TAG("KRInputFirstGate")
        << "session release active=" << active;
    if (becameInactive) {
        KRMainThreadTaskPuller::OnSessionActiveChanged(false);
    }
    for (auto &dispatch : dispatches) {
        dispatch();
    }
    if (hasFinished) {
        LogMetrics(finished);
    }
}

void KRInputFirstGate::OnInputBegin() {
    std::lock_guard<std::mutex> lock(mutex_);
    ++inputs_in_flight_;
    input_idle_ = false;
}

void KRInputFirstGate::OnInputDone() {
    std::vector<std::function<void()>> dispatches;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (inputs_in_flight_ > 0) {
            --inputs_in_flight_;
        }
        // 外层的同步事件还没处理完，帧任务继续等。
        if (inputs_in_flight_ > 0) {
            return;
        }
        last_input_done_ns_ = NowNs();
        dispatches = TakeAllDeferredLocked();
    }
    for (auto &dispatch : dispatches) {
        dispatch();
    }
}

bool KRInputFirstGate::IsInSession() {
    std::lock_guard<std::mutex> lock(mutex_);
    return !sessions_.empty();
}

bool KRInputFirstGate::OnVsync(const void *module, int64_t vsyncTimestampNs, std::function<void()> dispatch) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (sessions_.empty()) {
        return false;
    }
    // 输入空闲，或本帧输入已在 vsync 之后处理完：立即投递。
    if (input_idle_ || (inputs_in_flight_ == 0 && last_input_done_ns_ >= vsyncTimestampNs)) {
        ++metrics_.frame_direct;
        return false;
    }
    deferred_[module] = std::move(dispatch);
    return true;
}

void KRInputFirstGate::DropDeferred(const void *module) {
    std::lock_guard<std::mutex> lock(mutex_);
    deferred_.erase(module);
}

bool KRInputFirstGate::MergeDeferred(const void *module, std::function<void()> dispatch) {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = deferred_.find(module);
        if (it == deferred_.end()) {
            return false;
        }
        // 这个模块整帧无输入：以新 vsync 的帧信息投递，丢弃它旧的暂存，并停止等待直到下一次输入。
        deferred_.erase(it);
        input_idle_ = true;
        ++metrics_.frame_merged;
        ++metrics_.input_idle_entries;
    }
    dispatch();
    return true;
}

void KRInputFirstGate::RecordInput(double lockWaitMs, double execMs, double latencyMs) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (sessions_.empty()) {
        return;
    }
    ++metrics_.inputs;
    AppendSample(&metrics_.lock_wait_ms, lockWaitMs);
    AppendSample(&metrics_.exec_ms, execMs);
    if (latencyMs >= 0) {
        AppendSample(&metrics_.latency_ms, latencyMs);
    }
}

void KRInputFirstGate::RecordPullStop(PullStop reason) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (sessions_.empty()) {
        return;
    }
    ++metrics_.pull_stops[static_cast<size_t>(reason)];
}

std::vector<std::function<void()>> KRInputFirstGate::TakeAllDeferredLocked() {
    std::vector<std::function<void()>> dispatches;
    dispatches.reserve(deferred_.size());
    for (auto &entry : deferred_) {
        dispatches.push_back(std::move(entry.second));
    }
    deferred_.clear();
    metrics_.frame_wait_input += static_cast<uint32_t>(dispatches.size());
    return dispatches;
}

std::vector<std::function<void()>> KRInputFirstGate::TakeDeferredIfOutOfSessionLocked() {
    if (!sessions_.empty()) {
        return {};
    }
    return TakeAllDeferredLocked();
}

bool KRInputFirstGate::TakeFinishedMetricsLocked(Metrics *out) {
    if (!sessions_.empty()) {
        return false;
    }
    const Metrics &m = metrics_;
    const bool hasData = m.inputs > 0 || m.frame_direct > 0 || m.frame_wait_input > 0 || m.frame_merged > 0;
    if (hasData) {
        *out = std::move(metrics_);
    }
    metrics_ = Metrics{};
    return hasData;
}

void KRInputFirstGate::LogMetrics(const Metrics &m) {
    KR_LOG_INFO_WITH_TAG("KRInputFirstGate")
        << "session metrics: inputs=" << m.inputs
        << " latencyMs p50=" << Percentile(m.latency_ms, 0.5) << " p90=" << Percentile(m.latency_ms, 0.9)
        << " max=" << Percentile(m.latency_ms, 1.0)
        << " lockWaitMs p50=" << Percentile(m.lock_wait_ms, 0.5) << " p90=" << Percentile(m.lock_wait_ms, 0.9)
        << " execMs p50=" << Percentile(m.exec_ms, 0.5) << " p90=" << Percentile(m.exec_ms, 0.9)
        << " frameDirect=" << m.frame_direct
        << " frameWaitInput=" << m.frame_wait_input
        << " frameMerged=" << m.frame_merged
        << " inputIdleEntries=" << m.input_idle_entries
        << " round2msUsedUp=" << m.pull_stops[static_cast<size_t>(PullStop::kRoundTimeUp)]
        << " pullQueueEmptyOrBusy=" << m.pull_stops[static_cast<size_t>(PullStop::kQueueEmptyOrBusy)]
        << " contextTasksWhileParked=" << KRThread::ExchangeTasksStartedWhileParked()
        << " mainPullTasks=" << KRMainThreadTaskPuller::ExchangeMainPullTaskCount();
}
