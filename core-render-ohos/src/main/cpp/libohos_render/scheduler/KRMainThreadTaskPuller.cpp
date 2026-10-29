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

#include "libohos_render/scheduler/KRMainThreadTaskPuller.h"

#include <chrono>
#include <cstdint>

#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/scheduler/KRInputFirstGate.h"

namespace {

// 主线程一轮最多执行 2ms。到时间就回到 ArkUI 事件循环，避免输入一直排队。
constexpr int64_t kMaxRoundNs = 2'000'000;

std::atomic<uint32_t> g_mainPullTasks{0};

int64_t NowNs() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
               std::chrono::steady_clock::now().time_since_epoch())
        .count();
}

}  // namespace

std::atomic<bool> KRMainThreadTaskPuller::parked_{false};
std::atomic<bool> KRMainThreadTaskPuller::round_posted_{false};

void KRMainThreadTaskPuller::OnSessionActiveChanged(bool active) {
    if (active) {
        if (parked_.exchange(true)) {
            return;
        }
        // 暂停 context 线程，并开始在主线程上执行队列里的任务。
        KRContextScheduler::SetContextThreadParked(true, &KRMainThreadTaskPuller::PostMainThreadRound);
        PostMainThreadRound();
    } else if (parked_.exchange(false)) {
        // 恢复 context 线程。已经投出、还没开始的那一轮会在 RunMainThreadRound 里直接返回。
        KRContextScheduler::SetContextThreadParked(false, &KRMainThreadTaskPuller::PostMainThreadRound);
    }
}

uint32_t KRMainThreadTaskPuller::ExchangeMainPullTaskCount() {
    return g_mainPullTasks.exchange(0, std::memory_order_relaxed);
}

void KRMainThreadTaskPuller::PostMainThreadRound() {
    if (round_posted_.exchange(true)) {
        return;
    }
    KRMainThread::RunOnMainThreadForNextLoop(&KRMainThreadTaskPuller::RunMainThreadRound);
}

void KRMainThreadTaskPuller::RunMainThreadRound() {
    round_posted_.store(false);
    if (!parked_.load()) {
        return;
    }
    auto &gate = KRInputFirstGate::GetInstance();
    const int64_t roundEndNs = NowNs() + kMaxRoundNs;
    while (true) {
        // 事件已结束，剩下的任务由 context 线程执行。
        if (!parked_.load()) {
            return;
        }
        if (NowNs() >= roundEndNs) {
            gate.RecordPullStop(KRInputFirstGate::PullStop::kRoundTimeUp);
            PostMainThreadRound();
            return;
        }
        // 队列空：暂停期间有新任务会再次通知。拿不到锁：context 线程还在执行进入暂停前的那个任务，
        // 结束后会把剩余任务放回队首并通知。两种情况都不用在这里重试。
        if (!KRContextScheduler::TryRunNextContextTaskOnMainThread()) {
            gate.RecordPullStop(KRInputFirstGate::PullStop::kQueueEmptyOrBusy);
            return;
        }
        g_mainPullTasks.fetch_add(1, std::memory_order_relaxed);
    }
}
