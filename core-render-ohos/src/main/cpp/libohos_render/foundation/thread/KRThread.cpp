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
#include <chrono>
#include <utility>

#include "libohos_render/utils/KRRenderLoger.h"

namespace {

// 一次性 timer 持有的上下文：执行回调函数 + 反指 KRThread（仅用于日志/调试，可省略）。
struct TimerContext {
    std::function<void()> func;
};

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
                        uv_close(handle, nullptr);
                    }
                }
            },
            nullptr);
        return;
    }

    // 取出当前所有待办任务（按入队顺序处理）。
    std::queue<std::unique_ptr<PendingTask>> local;
    {
        std::lock_guard<std::mutex> lock(m_queueMutex);
        std::swap(local, m_pending);
    }

    // 真正"运行任务"的批处理，需要受 TaskMutex 保护，沿用旧的互斥语义，
    // 让 DirectRunOnCurThread 在外线程能竞争到执行权。
    if (TaskMutex(true, false, false)) {
        m_isExecutingTask.store(true);
        while (!local.empty()) {
            auto pending = std::move(local.front());
            local.pop();
            if (pending == nullptr || !pending->func) {
                continue;
            }
            if (pending->delayMs <= 0) {
                pending->func();
            } else {
                StartTimerInLoop(std::move(pending->func), pending->delayMs);
            }
        }
        m_isExecutingTask.store(false);
        TaskMutex(false, true, false);
    } else {
        // 当前 mutex 被外部 DirectRunOnCurThread 占用，把任务退回队列等下次触发。
        std::lock_guard<std::mutex> lock(m_queueMutex);
        while (!local.empty()) {
            m_pending.push(std::move(local.front()));
            local.pop();
        }
        // 下一次有人 send 时还会再进来；为避免饥饿，这里也主动再 send 一次。
        uv_async_send(&m_async);
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
    // 必须在 loop 线程上执行（OnAsync 内部即是）。
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
    uv_timer_start(timer, &KRThread::TimerCb, static_cast<uint64_t>(delayMs), 0);
}

void KRThread::TimerCb(uv_timer_t *handle) {
    auto *ctx = static_cast<TimerContext *>(handle->data);
    if (ctx != nullptr && ctx->func) {
        ctx->func();
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

    auto pending = std::make_unique<PendingTask>();
    pending->func = std::move(task);
    pending->delayMs = delayMilliseconds;
    {
        std::lock_guard<std::mutex> lock(m_queueMutex);
        m_pending.push(std::move(pending));
    }
    uv_async_send(&m_async);
}

void KRThread::DispatchSync(const std::function<void()> &task) {
    DirectRunOnCurThread(task);
}

void KRThread::DirectRunOnCurThread(const std::function<void()> &task) {
    if (!task) {
        return;
    }
    if (m_isExecutingTask.load()) {
        // 当前线程已经在 worker 的任务执行栈里（典型为重入），直接运行。
        task();
        return;
    }

    auto start = std::chrono::steady_clock::now();
    bool didHandleTask = false;
    while (!SyncMainTaskMutex(false, false, false)) {
        auto now = std::chrono::steady_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now - start);
        if (duration.count() > 100) {
            break;  // 超时
        }
        if (TaskMutex(true, false, false)) {
            m_isExecutingTask.store(true);
            task();
            m_isExecutingTask.store(false);
            TaskMutex(false, true, false);
            didHandleTask = true;
            break;
        }
    }
    if (!didHandleTask) {
        KR_LOG_INFO << "DispatchAsync when run DirectRunOnCurThread";
        DispatchAsync(task);
    }
}

bool KRThread::TaskMutex(bool try_lock, bool is_set, bool value) {
    std::unique_lock<std::mutex> lock(m_taskMutex);
    if (try_lock) {
        if (m_mutex_locked) {
            return false;
        }
        m_mutex_locked = true;
        return true;
    }
    if (is_set) {
        m_mutex_locked = value;
    }
    return m_mutex_locked;
}

bool KRThread::SyncMainTaskMutex(bool try_lock, bool is_set, bool value) {
    std::unique_lock<std::mutex> lock(m_syncMainTaskMutex);
    if (try_lock) {
        if (m_sync_main_task_locked) {
            return false;
        }
        m_sync_main_task_locked = true;
        return true;
    }
    if (is_set) {
        m_sync_main_task_locked = value;
    }
    return m_sync_main_task_locked;
}