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

#ifndef CORE_RENDER_OHOS_KRMAINTHREADTASKPULLER_H
#define CORE_RENDER_OHOS_KRMAINTHREADTASKPULLER_H

#include <atomic>

/**
 * 连续同步事件（同步 touchMove、同步 scroll）从开始到结束期间，暂停 context 线程。
 * 队列中的 context 任务改由主线程按原顺序执行，执行时持有 context 任务锁（m_taskMutex）。
 * 主线程每次最多连续执行 2ms，然后回到 ArkUI 事件循环，下一轮再继续。
 * 事件结束后，context 线程从同一队列接着执行。
 */
class KRMainThreadTaskPuller {
 public:
    // 主线程调用：连续同步事件从无到有，或从有到无。
    static void OnSessionActiveChanged(bool active);

    // 连续同步事件期间，主线程执行的 context 任务数。读出后清零。
    static uint32_t ExchangeMainPullTaskCount();

 private:
    // 任意线程调用。context 线程暂停期间有新任务时由它调用。
    static void PostMainThreadRound();
    static void RunMainThreadRound();

    static std::atomic<bool> parked_;
    // 已经投了一轮、还没执行时为 true，避免重复投递。
    static std::atomic<bool> round_posted_;
};

#endif  // CORE_RENDER_OHOS_KRMAINTHREADTASKPULLER_H
