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

#ifndef CORE_RENDER_OHOS_KRTHREAD_H
#define CORE_RENDER_OHOS_KRTHREAD_H

#include <uv.h>
#include <atomic>
#include <cassert>
#include <condition_variable>
#include <functional>
#include <memory>
#include <mutex>
#include <queue>
#include <string>
#include <thread>

// KRThread：基于自建 uv_loop_t 的工作线程。
// 关键约束（HarmonyOS libuv）：
//   * uv_loop_init / uv_run / 所有非线程安全 uv_* 接口必须在 loop 线程；
//   * 跨线程提交任务只允许通过 uv_async_send（线程安全）唤醒 loop 线程，
//     再由 loop 线程在回调中处理 uv 句柄（uv_timer_init/uv_timer_start 等）。
class KRThread {
 public:
    explicit KRThread(const std::string &name);
    ~KRThread();

    KRThread(const KRThread &) = delete;
    KRThread &operator=(const KRThread &) = delete;

    /**
     * @brief 在 worker 线程上异步执行任务（可延时）。线程安全，可在任意线程调用。
     */
    void DispatchAsync(std::function<void()> task, int delayMilliseconds = 0);

    /**
     * @brief 同步执行任务。当前实现下与 DirectRunOnCurThread 等价（沿用旧语义）。
     */
    void DispatchSync(const std::function<void()> &task);

    /**
     * @brief 在当前调用线程上"直跑"任务，与 worker 线程协调互斥；
     *        若长时间拿不到 mutex 则降级为 DispatchAsync。沿用旧语义。
     */
    void DirectRunOnCurThread(const std::function<void()> &task);

    bool TaskMutex(bool try_lock, bool is_set, bool value);
    bool SyncMainTaskMutex(bool try_lock, bool is_set, bool value);

    bool IsCurrentThreadWorkerThread() const {
        return std::this_thread::get_id() == m_workerThreadId;
    }

    void AssertCurrentThreadWorker() {
        assert(IsCurrentThreadWorkerThread() && "This is NOT the worker thread.");
    }

 private:
    struct PendingTask {
        std::function<void()> func;
        int delayMs;
    };

    void WorkerLoop(const std::string &name);
    void OnAsync();
    static void AsyncCb(uv_async_t *handle);
    static void TimerCb(uv_timer_t *handle);
    static void TimerCloseCb(uv_handle_t *handle);
    static void AsyncCloseCb(uv_handle_t *handle);

    void StartTimerInLoop(std::function<void()> task, int delayMs);

    // ---- 线程与 loop ----
    std::thread m_workerThread;
    std::thread::id m_workerThreadId;
    uv_loop_t m_loop{};
    uv_async_t m_async{};
    // loop 是否已经在 worker 线程里完成 init / async 注册。
    std::atomic<bool> m_loopReady{false};
    std::atomic<bool> m_stop{false};
    // 用于 ctor 等待 loop 在 worker 线程里 init 完成。
    std::mutex m_startMutex;
    std::condition_variable m_startCv;

    // ---- 跨线程任务队列 ----
    std::mutex m_queueMutex;
    std::queue<std::unique_ptr<PendingTask>> m_pending;

    // ---- 兼容旧版本的 mutex 协调（供 KRContextScheduler 使用） ----
    std::mutex m_taskMutex;
    bool m_mutex_locked = false;
    std::mutex m_syncMainTaskMutex;
    bool m_sync_main_task_locked = false;
    std::atomic<bool> m_isExecutingTask{false};
};

#endif  // CORE_RENDER_OHOS_KRTHREAD_H
