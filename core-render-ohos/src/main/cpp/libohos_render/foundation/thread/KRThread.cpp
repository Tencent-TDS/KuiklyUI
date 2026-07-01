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

#include <pthread.h>
#include <cassert>
#include <chrono>
#include <utility>

#include "libohos_render/foundation/thread/KRThreadFatalGuard.h"
#include "libohos_render/utils/KRRenderLoger.h"

namespace {

using kuikly::thread::RunWithFatalGuard;

// 一次性 timer 持有的上下文：exec 为“到点后的提交动作”，在 TimerCb 中被调用。
struct TimerContext {
    std::function<void()> exec;
};

// DirectRunOnCurThread fast-fail 等待窗口：
//   * 设计取舍：调用者在该窗口内不停 try_lock + yield 抢 m_taskMutex；
//     超时后降级为 DispatchAsync，避免 caller 线程被 worker 长跑卡死。
//   * 之所以是 100ms 而非更短：太短会让 caller 频繁错过 worker 短任务、转 async
//     带来不必要的 latency 抖动；太长则在主线程上观感卡顿。100ms 是经验阀值。
constexpr auto kDirectRunFastFailWindow = std::chrono::milliseconds{100};

}  // namespace

KRThread::KRThread(const std::string &name) {
    m_workerThread = std::thread([this, name]() { this->WorkerLoop(name); });
    pthread_setname_np(m_workerThread.native_handle(), name.c_str());

    // 等 worker 线程把 uv_loop_init / uv_async_init 完成后再返回，
    // 这样外部立刻 DispatchAsync 也能保证 m_async 已就绪。
    std::unique_lock<std::mutex> lock(m_startMutex);
    m_startCv.wait(lock, [this]() { return m_loopReady.load() || m_stop.load(); });
}

KRThread::~KRThread() {
    if (!m_loopReady.load()) {
        // 启动失败：worker 已退出或从未起来。
        if (m_workerThread.joinable()) {
            m_workerThread.join();
        }
        return;
    }

    m_stop.store(true);
    // 唤醒 loop 线程，让它走到 OnAsync 里检查 m_stop，关闭 async 句柄并退出 loop。
    uv_async_send(&m_async);

    if (m_workerThread.joinable()) {
        m_workerThread.join();
    }
}

void KRThread::WorkerLoop(const std::string &name) {
    m_workerThreadId = std::this_thread::get_id();

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

    // 阶段 2：取出当前所有立即任务（包括 timer 到点后重新入队的任务），
    // 按入队顺序执行，受 m_taskMutex 保护以保留与 DirectRunOnCurThread 互斥语义。
    std::queue<std::function<void()>> local;
    {
        std::lock_guard<std::mutex> lock(m_queueMutex);
        std::swap(local, m_pending);
    }
    if (local.empty()) {
        return;
    }

    // 抢执行权：阻塞等待。即使此刻有 DirectRunOnCurThread 持锁，
    // worker 也只是被挂起到对方 unlock，期间不忙等、不浪费 CPU。
    // worker 线程不会自我重入（task 内嵌套提交同步任务走 m_isExecutingTask 快路径），
    // 故对 m_taskMutex 阻塞 lock 不会自死锁。
    {
        std::lock_guard<std::mutex> taskLock(m_taskMutex);
        m_isExecutingTask.store(true);
        while (!local.empty()) {
            auto fn = std::move(local.front());
            local.pop();
            if (fn) {
                // 任何未捕获异常都会一路冒到 std::thread 入口触发 std::terminate，
                // 同时让 m_taskMutex / m_isExecutingTask 来不及落回干净状态——
                // 这里 fail-fast，让崩溃栈停在第一现场。
                RunWithFatalGuard("KRThread.OnAsync.batch", fn);
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
        // RunWithFatalGuard 只挡异常，不挡数据竞争 —— 这里选择丢弃任务。
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
    if (!task) {
        return;
    }
    if (m_isExecutingTask.load() && IsCurrentThreadWorkerThread()) {
        // 仅当“当前就在 worker 的执行栈里（task 体内嵌套调用）”才允许直跑，
        // 避免外部线程在 worker 持锁跑批期间错误地“白嘍”执行权造成数据竞争。
        // fail-fast：嵌套层抛异常若逃到外层 OnAsync.batch，外层 RunWithFatalGuard
        // 同样会 abort，但在嵌套层就拦住可以让崩溃栈更接近现场。
        RunWithFatalGuard("KRThread.DirectRunOnCurThread.nested", task);
        return;
    }

    auto start = std::chrono::steady_clock::now();
    bool didHandleTask = false;
    // 观察 worker 是否正在反向同步等主线程：若已举旗，意味着 m_taskMutex 被 worker
    // 抻在 cv.wait 期间，本路径继续 yield-spin 等锁毫无意义，直接 fast-fail 降级异步。
    while (!IsWorkerAwaitingMainTask()) {
        if (std::chrono::steady_clock::now() - start > kDirectRunFastFailWindow) {
            break;  // 超过 fast-fail 窗口，落到异步派发
        }
        std::unique_lock<std::mutex> taskLock(m_taskMutex, std::try_to_lock);
        if (taskLock.owns_lock()) {
            m_isExecutingTask.store(true);
            // 借位执行 task：与 OnAsync.batch 一致，未捕获异常会让 m_isExecutingTask /
            // m_taskMutex 处于不一致中间态，并可能在调用方线程引发 std::terminate；
            // 这里 fail-fast，与上层 napi 边界 try-catch 行为对齐。
            RunWithFatalGuard("KRThread.DirectRunOnCurThread.borrow", task);
            m_isExecutingTask.store(false);
            // taskLock 离开作用域自动释放
            didHandleTask = true;
            break;
        }
        // try_lock 失败：让出一下 CPU，避免空转把核打满。
        std::this_thread::yield();
    }
    if (!didHandleTask) {
        KR_LOG_INFO << "DispatchAsync when run DirectRunOnCurThread";
        DispatchAsync(task);
    }
}