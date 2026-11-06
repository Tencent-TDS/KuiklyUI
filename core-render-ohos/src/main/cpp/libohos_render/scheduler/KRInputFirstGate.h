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

#ifndef CORE_RENDER_OHOS_KRINPUTFIRSTGATE_H
#define CORE_RENDER_OHOS_KRINPUTFIRSTGATE_H

#include <cstddef>
#include <cstdint>
#include <functional>
#include <map>
#include <mutex>
#include <set>
#include <utility>
#include <vector>

/**
 * 连续同步事件期间，把 vsync 帧任务推迟到本帧输入处理完之后再投递。
 * 顺序是先输入、后帧任务，避免帧任务和同时到达的输入抢同一把 context 任务锁。
 *
 * 一次连续同步事件从 native 的开始标志到结束标志：touch 从首个 DOWN 到最后一个 UP/CANCEL，
 * 滚动从开始到停止。以 (owner, type) 计数。owner 被移除或销毁时 ReleaseSessions 释放它登记的全部事件。
 * 不用时间阈值判断开始和结束。
 *
 * 会话从无到有时暂停 context 线程，队列里的 context 任务改由主线程执行，一轮最多 2ms。从有到无时恢复。
 *
 * 帧任务：按 KRVsyncModule 分别暂存，每个模块最多存一帧。事件进行中、本帧输入还没处理完时先存起来，
 * 输入处理完再投递所有模块的暂存。某个模块的下一帧 vsync 到来时它的暂存仍在，就合并成一次投递，
 * 之后到下一次输入之前直接投递。
 *
 * 指标：事件进行期间累计，最后一次事件结束时打一行日志并清零。事件之外不做统计。
 * execMs 是同步事件回调本身的耗时，不含主线程这一轮里执行 context 任务的时间。
 *
 * 线程：事件开始和结束在主线程。OnInputBegin / OnInputDone 通常在主线程。没拿到 context 任务锁、
 * 这一次改成异步执行时，OnInputDone 在执行这次异步任务的线程上：context 线程暂停期间是主线程，
 * 否则是 context 线程。OnVsync / MergeDeferred 在 vsync 线程。DropDeferred 在调用 UnRegisterVsync 的线程。
 */
class KRInputFirstGate {
 public:
    enum class SessionType {
        kTouch,
        kScroll,
        kGesture,  // pan / pinch / longPress 支持同步之后再启用
    };

    // 主线程一轮执行的停止原因。事件结束导致的停止不计入。
    enum class PullStop : uint8_t {
        kRoundTimeUp,
        kQueueEmptyOrBusy,
        kCount,
    };

    static KRInputFirstGate &GetInstance();

    void BeginSession(const void *owner, SessionType type);
    void EndSession(const void *owner, SessionType type);
    void ReleaseSessions(const void *owner);

    // 一次同步连续事件：Kotlin 回调执行前调用 OnInputBegin，回调和同步 UI 提交完成后调用 OnInputDone。
    // 两者成对计数。嵌套时只有最外层的 OnInputDone 投递暂存的帧任务。
    void OnInputBegin();
    void OnInputDone();

    bool IsInSession();

    /**
     * module 的 vsync 回调到达、且它没有还没执行完的帧任务时调用。
     * @param module 发起这一帧的 KRVsyncModule
     * @return true 表示这次 dispatch 已暂存，调用方不要再投递；false 表示调用方应立即投递
     */
    bool OnVsync(const void *module, int64_t vsyncTimestampNs, std::function<void()> dispatch);

    /**
     * module 的 vsync 回调到达、但它的上一帧任务还没执行完时调用：若 module 有暂存，
     * 则以新 dispatch 替换并投递，进入输入空闲。
     * @return true 表示已合并投递；false 表示 module 没有暂存，调用方按原规则丢弃这一帧
     */
    bool MergeDeferred(const void *module, std::function<void()> dispatch);

    // 模块反注册或销毁时丢掉它暂存的帧，不执行。避免同一地址的新模块合并到旧帧。
    void DropDeferred(const void *module);

    /**
     * 记录一次连续同步事件的耗时。只在事件进行期间生效。
     * @param lockWaitMs 等待 context 任务锁的耗时
     * @param execMs 执行耗时（Kotlin 回调 + 同步 UI 提交）
     * @param latencyMs 输入时间戳到 UI 提交完成的延迟；无输入时间戳时传负数
     */
    void RecordInput(double lockWaitMs, double execMs, double latencyMs);

    /**
     * 记录主线程一轮执行的停止原因。只在连续同步事件进行期间生效。
     */
    void RecordPullStop(PullStop reason);

 private:
    struct Metrics {
        uint32_t inputs = 0;
        uint32_t frame_direct = 0;
        uint32_t frame_wait_input = 0;
        uint32_t frame_merged = 0;
        uint32_t input_idle_entries = 0;
        uint32_t pull_stops[static_cast<size_t>(PullStop::kCount)] = {};
        // 事件进行期间采样，有上限。事件结束时算 p50/p90。
        std::vector<double> latency_ms;
        std::vector<double> lock_wait_ms;
        std::vector<double> exec_ms;
    };

    KRInputFirstGate() = default;
    std::vector<std::function<void()>> TakeAllDeferredLocked();
    std::vector<std::function<void()>> TakeDeferredIfOutOfSessionLocked();
    bool TakeFinishedMetricsLocked(Metrics *out);
    static void LogMetrics(const Metrics &metrics);

    std::mutex mutex_;
    std::set<std::pair<const void *, SessionType>> sessions_;
    // 还没处理完的同步连续事件数。同步 UI 提交又触发同步事件时会嵌套，大于 1。
    uint32_t inputs_in_flight_ = 0;
    bool input_idle_ = false;
    int64_t last_input_done_ns_ = 0;
    // 按 KRVsyncModule 暂存的帧任务。
    std::map<const void *, std::function<void()>> deferred_;
    Metrics metrics_;
};

#endif  // CORE_RENDER_OHOS_KRINPUTFIRSTGATE_H
