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
     * @brief 在当前调用线程上"直跑"任务，与 worker 线程协调互斥；
     *        若长时间拿不到 mutex 则降级为 DispatchAsync。沿用旧语义。
     */
    void DirectRunOnCurThread(const std::function<void()> &task);

    /**
     * @brief 当前 worker 线程是否正在反向同步等主线程跑回调（cv.wait 中）。
     *        DirectRunOnCurThread 自旋时观察此标志，发现已举旗时立刻让步、降级为
     *        DispatchAsync，避免无谓的 100ms yield-spin 等待 worker 释放 m_taskMutex。
     */
    bool IsWorkerAwaitingMainTask() const noexcept {
        return m_workerAwaitingMainTask.load(std::memory_order_acquire);
    }

    /**
     * @brief RAII 守卫：worker 线程在调用 KRMainThread::RunOnMainThread + cv.wait
     *        反向同步等主线程之前构造，cv.wait 返回后随作用域析构自动落旗。
     *        仅 KRContextScheduler 在 worker 线程上使用。
     */
    class WorkerAwaitingMainTaskGuard {
     public:
        explicit WorkerAwaitingMainTaskGuard(KRThread *t) : t_(t) {
            t_->m_workerAwaitingMainTask.store(true, std::memory_order_release);
        }
        ~WorkerAwaitingMainTaskGuard() {
            t_->m_workerAwaitingMainTask.store(false, std::memory_order_release);
        }
        WorkerAwaitingMainTaskGuard(const WorkerAwaitingMainTaskGuard &) = delete;
        WorkerAwaitingMainTaskGuard &operator=(const WorkerAwaitingMainTaskGuard &) = delete;

     private:
        KRThread *t_;
    };

    bool IsCurrentThreadWorkerThread() const {
        return std::this_thread::get_id() == m_workerThreadId;
    }

    void AssertCurrentThreadWorker() {
        assert(IsCurrentThreadWorkerThread() && "This is NOT the worker thread.");
    }

 private:
    struct PendingTimer {
        std::function<void()> func;
        int delayMs;
    };

    void WorkerLoop(const std::string &name);
    void OnAsync();
    static void AsyncCb(uv_async_t *handle);
    static void TimerCb(uv_timer_t *handle);
    static void TimerCloseCb(uv_handle_t *handle);
    static void AsyncCloseCb(uv_handle_t *handle);

    // 仅允许在 worker 线程（loop 线程）上调用。
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
    // m_pending：立即执行的任务队列（delayMs<=0）。
    // m_pendingTimers：需要起 uv_timer 的任务队列（跨线程提交 + delayMs>0），
    //   OnAsync 在 loop 线程上起 timer。
    std::mutex m_queueMutex;
    std::queue<std::function<void()>> m_pending;
    std::queue<std::unique_ptr<PendingTimer>> m_pendingTimers;

    // ---- 任务执行权 / 同步主任务标志 ----
    // m_taskMutex："kuikly context 任务执行权"令牌。任意时刻只允许一个线程持有，
    // 持有者就是当前正在跑 task 体的线程——可能是 worker 线程（OnAsync batch 模式），
    // 也可能是任意调用线程（DirectRunOnCurThread 借位模式）。
    // 不可递归——同线程嵌套调用靠 m_isExecutingTask + 线程比对短路规避。
    // 决策记录：为什么不用 std::recursive_mutex，见
    // docs/design/krthread-task-mutex.md §4.1。
    std::mutex m_taskMutex;
    // m_isExecutingTask：标记 worker 线程当前是否正处于 task 体执行栈中。
    // 仅用于在 worker 线程内部识别"task 体内嵌套提交同步任务"的重入场景，
    // 让其直接执行避免对 m_taskMutex 自死锁。其它线程读取无意义。
    std::atomic<bool> m_isExecutingTask{false};

    // m_workerAwaitingMainTask：worker 线程是否正在反向同步等主线程回调。
    // 由 WorkerAwaitingMainTaskGuard 通过 RAII 在 ScheduleTaskOnMainThread(sync=true)
    // 的 cv.wait 期间举旗/落旗；DirectRunOnCurThread 自旋时观察此旗实现 fast-fail 让步。
    // 语义就是一个跨线程可见的 bool 标志位，atomic 即可，无需 mutex 保护。
    std::atomic<bool> m_workerAwaitingMainTask{false};
};

#endif  // CORE_RENDER_OHOS_KRTHREAD_H
