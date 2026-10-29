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

#include "libohos_render/foundation/thread/KRThread.h"

#include <qos/qos.h>
#include <cassert>
#include <chrono>
#include <utility>

#include "libohos_render/utils/KRRenderLoger.h"

namespace {

std::atomic<uint32_t> g_tasksStartedWhileParked{0};

// 一次性 timer 持有的上下文：exec 为“到点后的提交动作”，在 TimerCb 中被调用。
struct TimerContext {
    std::function<void()> exec;
};

// 外部线程等待 context 任务锁期间的计数。异常路径也会减回去。
class ExternalWaiterGuard {
 public:
    explicit ExternalWaiterGuard(std::atomic<int> &counter) : counter_(counter) {}
    ~ExternalWaiterGuard() { Leave(); }
    void Enter() {
        if (!entered_) {
            counter_.fetch_add(1, std::memory_order_acq_rel);
            entered_ = true;
        }
    }
    void Leave() {
        if (entered_) {
            counter_.fetch_sub(1, std::memory_order_acq_rel);
            entered_ = false;
        }
    }
    ExternalWaiterGuard(const ExternalWaiterGuard &) = delete;
    ExternalWaiterGuard &operator=(const ExternalWaiterGuard &) = delete;

 private:
    std::atomic<int> &counter_;
    bool entered_ = false;
};

// 外部线程持有 context 任务锁期间的标记：m_isExecutingTask 与 m_externalOwner。
// 必须走 RAII：异常路径下手写的 store(false) 会被跳过，导致标志位残留。
class ExternalExecutionGuard {
 public:
    ExternalExecutionGuard(std::atomic<bool> &executing, std::atomic<std::thread::id> &owner)
        : executing_(executing), owner_(owner) {
        executing_.store(true);
        owner_.store(std::this_thread::get_id());
    }
    ~ExternalExecutionGuard() {
        owner_.store(std::thread::id());
        executing_.store(false);
    }
    ExternalExecutionGuard(const ExternalExecutionGuard &) = delete;
    ExternalExecutionGuard &operator=(const ExternalExecutionGuard &) = delete;

 private:
    std::atomic<bool> &executing_;
    std::atomic<std::thread::id> &owner_;
};

}  // namespace

KRThread::KRThread(const std::string &name)
    : m_workerThread([this, name]() { this->WorkerLoop(name); }, name) {

    // 等 worker 线程把 uv_loop_init / uv_async_init 完成后再返回，
    // 这样外部立刻 DispatchAsync 也能保证 m_async 已就绪。
    std::unique_lock<std::mutex> lock(m_startMutex);
    m_startCv.wait(lock, [this]() { return m_loopReady.load() || m_stop.load(); });
}

KRThread::~KRThread() {
    if (!m_loopReady.load()) {
        // 启动失败：worker 已退出或从未起来。
        if (m_workerThread.Joinable()) {
            m_workerThread.Join();
        }
        return;
    }

    m_stop.store(true);
    // 唤醒 loop 线程，让它走到 OnAsync 里检查 m_stop，关闭 async 句柄并退出 loop。
    uv_async_send(&m_async);

    if (m_workerThread.Joinable()) {
        m_workerThread.Join();
    }
}

void KRThread::WorkerLoop(const std::string &name) {
    m_workerThreadId = std::this_thread::get_id();

    // 提升 worker 线程的调度优先级到 UI 交互级：uv_run 里跑的都是 kuikly 的
    // 帧任务 / setTimeout 到点回调等对时延敏感的工作，若被系统当普通后台线程
    // 调度，setTimeout 抖动实测可到 100ms+（见 TimerDriftBenchmarkPage 数据）。
    // 必须在 worker 本体线程里调用 OH_QoS_SetThreadQoS —— 该 API 隐式作用于当前线程。
    // 失败不阻断 loop 启动，仅打日志：低版本设备/非 OH 环境上不支持时降级到默认优先级。
    int qosRet = OH_QoS_SetThreadQoS(QoS_Level::QOS_USER_INTERACTIVE);
    if (qosRet != 0) {
        KR_LOG_ERROR << "KRThread[" << name << "] OH_QoS_SetThreadQoS failed, err=" << qosRet;
    }

    int ret = uv_loop_init(&m_loop);
    if (ret != 0) {
        KR_LOG_ERROR << "KRThread[" << name << "] uv_loop_init failed: " << ret;
        m_stop.store(true);
        m_startCv.notify_all();
        return;
    }
    // 让 TimerCb 等通过 uv_loop_get_data 能反查到 this。
    uv_loop_set_data(&m_loop, this);

    ret = uv_async_init(&m_loop, &m_async, &KRThread::AsyncCb);
    if (ret != 0) {
        KR_LOG_ERROR << "KRThread[" << name << "] uv_async_init failed: " << ret;
        uv_loop_close(&m_loop);
        m_stop.store(true);
        m_startCv.notify_all();
        return;
    }
    m_async.data = this;

    {
        std::lock_guard<std::mutex> lock(m_startMutex);
        m_loopReady.store(true);
    }
    m_startCv.notify_all();

    // 与创建 loop 同一线程执行 uv_run，符合 HarmonyOS libuv 约束。
    uv_run(&m_loop, UV_RUN_DEFAULT);

    // uv_run 返回前所有句柄应该都已经 close，这里再保险关闭 loop。
    int close_ret = uv_loop_close(&m_loop);
    if (close_ret != 0) {
        KR_LOG_ERROR << "KRThread[" << name << "] uv_loop_close ret=" << close_ret;
    }
}

void KRThread::AsyncCb(uv_async_t *handle) {
    auto *self = static_cast<KRThread *>(handle->data);
    if (self != nullptr) {
        self->OnAsync();
    }
}

void KRThread::OnAsync() {
    // 如果已经发出停止信号，直接走停止路径，不再取出任何任务、也不再调 uv_async_send。
    if (m_stop.load()) {
        // 走一遍 loop 上所有还活着的句柄，全部 close，让 uv_run 能够退出。
        uv_walk(
            &m_loop,
            [](uv_handle_t *handle, void *) {
                if (uv_is_closing(handle) == 0) {
                    if (handle->type == UV_TIMER) {
                        uv_timer_stop(reinterpret_cast<uv_timer_t *>(handle));
                        uv_close(handle, &KRThread::TimerCloseCb);
                    } else if (handle->type == UV_ASYNC) {
                        uv_close(handle, &KRThread::AsyncCloseCb);
                    } else {
                        // 本 loop 的句柄全集由 KRThread 私有维护：仅 UV_ASYNC(m_async) + UV_TIMER。
                        // m_loop 是 private、无 getter/friend，句柄类型对 KRThread 是闭集。
                        // 若未来新增其它句柄类型，请同步在此处补对应的 close cb 分支——
                        // 不要用默认 close cb 兜底：uv_close 的 close_cb 无法区分 handle 是成员型
                        // （如 m_async，禁止 delete）还是 new 出来的（如 uv_timer_t，需要 delete）。
                        //
                        // 双层策略：
                        //   * debug：assert(false) 让遗漏在开发阶段第一时间暴露到崩溃栈；
                        //   * release：assert 被吃掉后走 uv_close(handle, nullptr) 保底 —— 若 debug 没抓到，
                        //     说明这条路径低频到测试未覆盖，release 泄漏一次 handle 关联堆内存，
                        //     换 loop 能正常退出，避免因低频路径 abort 掉整个宿主进程。
                        KR_LOG_ERROR << "KRThread::OnAsync stop path met unregistered uv_handle_t type="
                                     << handle->type << "; add a dedicated close cb.";
                        assert(false && "KRThread::OnAsync unregistered uv_handle_t type");
                        uv_close(handle, nullptr);
                    }
                }
            },
            nullptr);
        return;
    }

    // 阶段 1：消费跨线程提交的 "待起 timer" 队列，立即在 loop 线程上起 uv_timer。
    // 只是注册句柄、不执行任务代码，无需 TaskMutex 保护。
    std::queue<std::unique_ptr<PendingTimer>> timers;
    {
        std::lock_guard<std::mutex> lock(m_queueMutex);
        std::swap(timers, m_pendingTimers);
    }
    while (!timers.empty()) {
        auto pt = std::move(timers.front());
        timers.pop();
        if (pt == nullptr || !pt->func) {
            continue;
        }
        StartTimerInLoop(std::move(pt->func), pt->delayMs);
    }

    // 阶段 2：先拿到 context 任务锁，再取出当前所有立即任务（包括 timer 到点后重新入队的任务），
    // 按入队顺序执行。必须先拿锁再取这批任务：主线程也从 m_pending 取任务。
    // 若先取走再等锁，等锁期间主线程会先执行更晚入队的任务，顺序就乱了。
    // 即使此刻有 DirectRunOnCurThread 持锁，worker 也只是被挂起到对方 unlock，期间不忙等。
    // worker 线程不会自我重入（task 内嵌套提交同步任务走 m_isExecutingTask 快路径），
    // 故对 m_taskMutex 阻塞 lock 不会自死锁。
    if (m_parked.load(std::memory_order_acquire)) {
        NotifyParkedIfPending();
        return;
    }
    {
        std::unique_lock<std::mutex> taskLock(m_taskMutex);
        std::queue<std::function<void()>> local;
        {
            std::lock_guard<std::mutex> lock(m_queueMutex);
            std::swap(local, m_pending);
        }
        if (local.empty()) {
            return;
        }
        // 等锁期间可能已进入暂停：本批原样退回队首，不执行任何任务。
        if (m_parked.load(std::memory_order_acquire)) {
            {
                std::lock_guard<std::mutex> lock(m_queueMutex);
                while (!m_pending.empty()) {
                    local.push(std::move(m_pending.front()));
                    m_pending.pop();
                }
                std::swap(local, m_pending);
            }
            taskLock.unlock();
            NotifyParkedIfPending();
            return;
        }
        m_isExecutingTask.store(true);
        while (!local.empty()) {
            auto fn = std::move(local.front());
            local.pop();
            if (fn) {
                if (m_parked.load(std::memory_order_acquire)) {
                    g_tasksStartedWhileParked.fetch_add(1, std::memory_order_relaxed);
                }
                // 任何未捕获异常都会一路冒到 std::thread 入口触发 std::terminate，
                // 同时让 m_taskMutex / m_isExecutingTask 来不及落回干净状态——
                // 这里不套 C++ catch，为的是让 K/N unhandled hook 能先于 std::terminate
                // 触发、打出完整 Kotlin 侧崩溃栈（catch 会让 K/N 观察到 "C++ 已处理"
                // 从而抑制 hook）。同时靠 std::mutex / std::atomic 的 RAII 保证 unwind
                // 将 m_taskMutex 释放、m_isExecutingTask 下文恢复。
                fn();
            }
            // 主线程在等锁，或 context 线程要暂停：先把剩余任务按原顺序放回队首，再释放锁。
            // 不放回的话，接下来拿到锁的线程会先执行更晚入队的任务。
            const bool parked = m_parked.load(std::memory_order_acquire);
            if (!local.empty() && (parked || m_externalWaiters.load(std::memory_order_acquire) > 0)) {
                {
                    std::lock_guard<std::mutex> lock(m_queueMutex);
                    while (!m_pending.empty()) {
                        local.push(std::move(m_pending.front()));
                        m_pending.pop();
                    }
                    std::swap(local, m_pending);
                }
                m_isExecutingTask.store(false);
                taskLock.unlock();
                if (parked) {
                    NotifyParkedIfPending();
                    return;
                }
                // std::mutex 不保证谁先拿到。等主线程真正拿到锁后，再回到 loop 重新竞争。
                while (m_externalWaiters.load(std::memory_order_acquire) > 0) {
                    std::this_thread::yield();
                }
                uv_async_send(&m_async);
                return;
            }
        }
        m_isExecutingTask.store(false);
    }
}

void KRThread::AsyncCloseCb(uv_handle_t *handle) {
    auto *self = static_cast<KRThread *>(handle->data);
    if (self != nullptr) {
        // 没有别的活跃句柄后，uv_run 会返回；这里显式 stop 一下更稳。
        uv_stop(&self->m_loop);
    }
}

void KRThread::StartTimerInLoop(std::function<void()> task, int delayMs) {
    // 必须在 loop 线程上执行（OnAsync / DispatchAsync 当前线程即 worker 时）。
    auto *timer = new uv_timer_t();
    auto *ctx = new TimerContext{std::move(task)};
    timer->data = ctx;

    int ret = uv_timer_init(&m_loop, timer);
    if (ret != 0) {
        KR_LOG_ERROR << "KRThread uv_timer_init failed: " << ret;
        delete ctx;
        delete timer;
        return;
    }
    ret = uv_timer_start(timer, &KRThread::TimerCb, static_cast<uint64_t>(delayMs), 0);
    if (ret != 0) {
        // 与 uv_timer_init 失败对称：uv_timer_start 失败意味着 timer 未启动，
        // TimerCb 不会触发，若不清理会造成 TimerContext + timer 堆内存泄漏。
        // 注意 uv_timer_init 已成功，此时 timer 已挂到 loop 上，必须走 uv_close
        // 让 loop 释放句柄，close_cb 里再回收 ctx / timer 内存（复用 TimerCloseCb）。
        KR_LOG_ERROR << "KRThread uv_timer_start failed: " << ret << "; drop task to avoid leak.";
        uv_close(reinterpret_cast<uv_handle_t *>(timer), &KRThread::TimerCloseCb);
    }
}

void KRThread::TimerCb(uv_timer_t *handle) {
    // timer 到点后：不直接执行 task，而是重新提交到立即任务队列，
    // 走 OnAsync 的 m_taskMutex 保护路径，保留与 DirectRunOnCurThread 的互斥语义。
    auto *self = static_cast<KRThread *>(uv_loop_get_data(handle->loop));
    auto *ctx = static_cast<TimerContext *>(handle->data);
    if (self != nullptr && ctx != nullptr && ctx->exec) {
        {
            std::lock_guard<std::mutex> lock(self->m_queueMutex);
            self->m_pending.push(std::move(ctx->exec));
        }
        uv_async_send(&self->m_async);
    } else if (self == nullptr) {
        // self == nullptr 只可能来自内存踩踏 / UAF 冲掉 loop->data，
        // 或对象生命周期已损坏；此时任何 KRThread 状态（worker 线程身份、
        // m_taskMutex、m_pending 等不变式）都不可信。
        //
        // 绝不能在此裸跑 ctx->exec：正常路径 task 必须在 worker 线程且受
        // m_taskMutex 保护执行，fallback 直接调用会绕开互斥语义、并可能与
        // 正在借位执行的 DirectRunOnCurThread 并发踩踏 kuikly 上下文。
        // 这里不依赖任何 C++ catch，直接丢弃任务。
        //
        // 双层策略（与 OnAsync 未知句柄分支保持一致）：
        //   * debug：assert(false) 让状态损坏第一时间暴露到崩溃栈；
        //   * release：assert 被吃掉后走 log + drop task + 关闭 timer 保底，
        //     换宿主进程存活，避免因低频/损坏路径 abort 掉整个进程。
        KR_LOG_ERROR << "KRThread::TimerCb loop data is null, drop task; "
                        "loop->data was corrupted or KRThread lifecycle broken.";
        assert(false && "KRThread::TimerCb loop->data is null");
    }
    uv_timer_stop(handle);
    uv_close(reinterpret_cast<uv_handle_t *>(handle), &KRThread::TimerCloseCb);
}

void KRThread::TimerCloseCb(uv_handle_t *handle) {
    auto *t = reinterpret_cast<uv_timer_t *>(handle);
    auto *ctx = static_cast<TimerContext *>(t->data);
    delete ctx;
    delete t;
}
void KRThread::DispatchAsync(std::function<void()> task, int delayMilliseconds) {
    if (!task) {
        return;
    }
    if (!m_loopReady.load()) {
        KR_LOG_ERROR << "KRThread::DispatchAsync before loop ready, drop task";
        return;
    }

    if (delayMilliseconds > 0) {
        // delay 任务由 uv_timer 负责计时，避免被 OnAsync 的批处理延迟。
        if (IsCurrentThreadWorkerThread()) {
            // 在 loop 线程上，可以直接起 timer，无需 uv_async 中转。
            StartTimerInLoop(std::move(task), delayMilliseconds);
            return;
        }
        // 跨线程：入 timer 待起队列 + uv_async_send，由 OnAsync 在 loop 线程起 timer。
        auto pt = std::make_unique<PendingTimer>();
        pt->func = std::move(task);
        pt->delayMs = delayMilliseconds;
        {
            std::lock_guard<std::mutex> lock(m_queueMutex);
            m_pendingTimers.push(std::move(pt));
        }
        uv_async_send(&m_async);
        return;
    }

    // 立即任务：入立即队列 + uv_async_send。
    {
        std::lock_guard<std::mutex> lock(m_queueMutex);
        m_pending.push(std::move(task));
    }
    uv_async_send(&m_async);
}

void KRThread::DirectRunOnCurThread(const std::function<void()> &task) {
    if (!TryRunOnCurThread(kDirectRunFastFailWindow, task)) {
        KR_LOG_INFO << "DispatchAsync when run DirectRunOnCurThread";
        DispatchAsync(task);
    }
}

void KRThread::SetParked(bool parked) {
    m_parked.store(parked, std::memory_order_release);
    if (!parked) {
        // 唤醒 context 线程，从 m_pending 继续执行。
        uv_async_send(&m_async);
    }
}

void KRThread::SetParkedTaskNotifier(std::function<void()> notifier) {
    m_parkedTaskNotifier = std::move(notifier);
}

void KRThread::NotifyParkedIfPending() {
    bool hasPending = false;
    {
        std::lock_guard<std::mutex> lock(m_queueMutex);
        hasPending = !m_pending.empty();
    }
    if (hasPending && m_parkedTaskNotifier) {
        m_parkedTaskNotifier();
    }
}

bool KRThread::TryRunNextPendingTaskOnCurThread(const std::function<void(const std::function<void()> &)> &run) {
    assert(!IsCurrentThreadWorkerThread() && "TryRunNextPendingTaskOnCurThread must be called by external thread");
    assert(m_externalOwner.load() != std::this_thread::get_id() &&
           "KRThread::TryRunNextPendingTaskOnCurThread re-entered while holding the task mutex");
    // 只尝试拿一次锁。拿不到就返回，主线程不在这里空转。
    std::unique_lock<std::mutex> taskLock(m_taskMutex, std::try_to_lock);
    if (!taskLock.owns_lock()) {
        return false;
    }
    std::function<void()> task;
    {
        std::lock_guard<std::mutex> lock(m_queueMutex);
        if (m_pending.empty()) {
            return false;
        }
        task = std::move(m_pending.front());
        m_pending.pop();
    }
    // 和 context 线程用同一个队列：只从队首取，取出和执行都在持有 context 任务锁时进行。
    ExternalExecutionGuard execGuard(m_isExecutingTask, m_externalOwner);
    run(task);
    return true;
}

bool KRThread::IsTaskLockHeldByCurrentThread() const {
    return m_externalOwner.load() == std::this_thread::get_id();
}

bool KRThread::TryRunOnCurThread(std::chrono::microseconds window, const std::function<void()> &task) {
    if (!task) {
        return true;
    }
    // 本线程已经持有 m_taskMutex（例如主线程持锁执行任务时，UI 操作同步触发了同步事件）。
    // std::mutex 不能重入，直接返回 false，由调用方改成异步执行。
    if (m_externalOwner.load() == std::this_thread::get_id()) {
        return false;
    }
    if (m_isExecutingTask.load() && IsCurrentThreadWorkerThread()) {
        // 仅当“当前就在 worker 的执行栈里（task 体内嵌套调用）”才允许直跑，
        // 避免主线程在 context 线程持锁执行任务时绕过这把锁，造成数据竞争。
        //
        // 异常语义：不套 C++ catch，让异常一路冒到 K/N unhandled hook，避免
        // “C++ 已处理”误判拖喽 hook 触发而丢失 Kotlin 侧崩溃信息。
        task();
        return true;
    }

    auto start = std::chrono::steady_clock::now();
    ExternalWaiterGuard waiter(m_externalWaiters);
    // 观察 worker 是否正在反向同步等主线程：若已举旗，意味着 m_taskMutex 被 worker
    // 抻在 cv.wait 期间，本路径继续 yield-spin 等锁毫无意义，直接 fast-fail 降级。
    while (!IsWorkerAwaitingMainTask()) {
        std::unique_lock<std::mutex> taskLock(m_taskMutex, std::try_to_lock);
        if (taskLock.owns_lock()) {
            waiter.Leave();
            // 借位执行 task。不套 C++ catch，让异常一路冒到 K/N unhandled hook：
            // 任何中间层 catch（即使手动 rethrow）都会让 K/N 观察到 "C++ 已处理"
            // 从而不再触发 hook，导致丢失 Kotlin 侧崩溃栈。unwind 期间会自动展开
            // 下面的 ExternalExecutionGuard 与 taskLock（unique_lock），保证
            // m_isExecutingTask / m_taskMutex 状态一致，不残留中间态。
            ExternalExecutionGuard execGuard(m_isExecutingTask, m_externalOwner);
            task();
            return true;
        }
        // 第一次没拿到锁后记入等待计数。context 线程据此在当前任务做完后释放锁。
        waiter.Enter();
        if (std::chrono::steady_clock::now() - start > window) {
            break;  // 超过 fast-fail 窗口，交给调用方降级
        }
        // try_lock 失败：让出一下 CPU，避免空转把核打满。
        std::this_thread::yield();
    }
    return false;
}

uint32_t KRThread::ExchangeTasksStartedWhileParked() {
    return g_tasksStartedWhileParked.exchange(0, std::memory_order_relaxed);
}
