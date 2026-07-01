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

#include "libohos_render/foundation/thread/KRMainThread.h"

#include <uv.h>
#include <atomic>
#include <memory>
#include <mutex>
#include <queue>
#include <thread>
#include <utility>

#include "libohos_render/foundation/thread/KRThreadFatalGuard.h"
#include "libohos_render/utils/KRRenderLoger.h"

namespace {

using kuikly::thread::RunWithFatalGuard;

struct PendingTask {
    std::function<void()> func;
    int delayMs;
};

// 主线程 uv_loop（来自 ArkTS 主线程的 napi_env）。
// 该 loop 的生命周期由 ArkTS 运行时管理，KRMainThread 仅持有指针、不创建也不销毁。
uv_loop_t *g_main_loop = nullptr;
// 主线程线程 ID（即 Export 被调用所在的线程）。
std::thread::id g_main_thread_id;
std::atomic<bool> g_initialized{false};

// 跨线程把任务投递到主线程的 async 句柄，必须在主线程（loop 线程）上 init。
uv_async_t g_main_async{};

// 待投递任务队列（生产者：任意线程；消费者：主线程 uv 回调）。
std::mutex g_queue_mutex;
std::queue<std::unique_ptr<PendingTask>> g_pending_queue;

// 在主线程上注册一个一次性定时器，到点执行 task 并清理自身。
// 设计要点：
//   * uv_timer_init 返回值必须检查；失败时不能泄漏 timer/holder 堆内存，
//     也不能静默丢弃 task——这里选择 log + 释放资源 + return（崩不是
//     责任，caller 应该能容忍 timer 创建失败这个极端低概率 case）。
//   * timer 回调里调用 user task 必须走 RunWithFatalGuard：taskcb 在 libuv 回调上下文
//     里执行，异常越过 C 帧会造成 UB，与 KRThread::TimerCb.fallback 同口径。
void StartTimerOnMainThread(std::function<void()> task, int delayMs) {
    auto *timer = new uv_timer_t();
    auto *holder = new std::function<void()>(std::move(task));
    timer->data = holder;
    int ret = uv_timer_init(g_main_loop, timer);
    if (ret != 0) {
        KR_LOG_ERROR << "KRMainThread::StartTimerOnMainThread uv_timer_init failed, ret=" << ret
                     << "; drop task to avoid leak.";
        delete holder;
        delete timer;
        return;
    }
    ret = uv_timer_start(
        timer,
        [](uv_timer_t *handle) {
            auto *fn = static_cast<std::function<void()> *>(handle->data);
            if (fn != nullptr) {
                // libuv 回调边界：异常越 C 帧 = UB，这里必须 fail-fast。
                RunWithFatalGuard("KRMainThread.MainTimer.cb", *fn);
            }
            uv_timer_stop(handle);
            uv_close(reinterpret_cast<uv_handle_t *>(handle), [](uv_handle_t *h) {
                auto *t = reinterpret_cast<uv_timer_t *>(h);
                auto *fn = static_cast<std::function<void()> *>(t->data);
                delete fn;
                delete t;
            });
        },
        static_cast<uint64_t>(delayMs), 0);
    if (ret != 0) {
        // 与 uv_timer_init 失败对称：uv_timer_start 失败意味着 timer 未启动，
        // 回调不会触发，若不清理会造成 holder + timer 堆内存泄漏。
        // 注意 uv_timer_init 已成功，此时 timer 已挂到 loop 上，必须走 uv_close
        // 让 loop 释放句柄，close_cb 里再回收 holder / timer 内存。
        KR_LOG_ERROR << "KRMainThread::StartTimerOnMainThread uv_timer_start failed, ret=" << ret
                     << "; drop task to avoid leak.";
        uv_close(reinterpret_cast<uv_handle_t *>(timer), [](uv_handle_t *h) {
            auto *t = reinterpret_cast<uv_timer_t *>(h);
            auto *fn = static_cast<std::function<void()> *>(t->data);
            delete fn;
            delete t;
        });
    }
}

// 主线程 uv_async 回调：把队列里所有任务取出，根据 delay 决定立即执行还是注册 uv_timer。
// 注意：本函数在主线程（loop 线程）执行，因此 uv_timer_init / uv_timer_start 都是合规的。
// 异常路径：user task 是业务提供的回调，本函数有 libuv async 回调上下文、异常逃出
// 会越 C 帧 UB，所以 inline 路径 fail-fast；delay > 0 路径交给 timer cb 里的 guard 处理。
void OnMainAsync(uv_async_t * /*handle*/) {
    std::queue<std::unique_ptr<PendingTask>> local;
    {
        std::lock_guard<std::mutex> lock(g_queue_mutex);
        std::swap(local, g_pending_queue);
    }
    while (!local.empty()) {
        auto pending = std::move(local.front());
        local.pop();
        if (pending == nullptr || !pending->func) {
            continue;
        }
        if (pending->delayMs <= 0) {
            RunWithFatalGuard("KRMainThread.MainAsync.batch", pending->func);
        } else {
            StartTimerOnMainThread(std::move(pending->func), pending->delayMs);
        }
    }
}

// 把任务塞进队列并唤醒主线程 loop。
void EnqueueAndNotify(std::function<void()> task, int delayMs) {
    auto pending = std::make_unique<PendingTask>();
    pending->func = std::move(task);
    pending->delayMs = delayMs;
    {
        std::lock_guard<std::mutex> lock(g_queue_mutex);
        g_pending_queue.push(std::move(pending));
    }
    uv_async_send(&g_main_async);
}

bool IsCurrentMainThread() {
    return std::this_thread::get_id() == g_main_thread_id;
}

}  // namespace

void KRMainThread::Export(napi_env env, napi_value /*exports*/) {
    // Export 由 ArkTS 主线程调用，必须在主线程上完成 loop 获取与 async 初始化，
    // 这样后续 uv_async_init/uv_timer_init 等非线程安全 API 才与 loop 线程一致。
    bool expected = false;
    if (!g_initialized.compare_exchange_strong(expected, true)) {
        return;  // 已经初始化过，避免重复 init async 句柄
    }

    g_main_thread_id = std::this_thread::get_id();

    napi_status status = napi_get_uv_event_loop(env, &g_main_loop);
    if (status != napi_ok || g_main_loop == nullptr) {
        KR_LOG_ERROR << "KRMainThread::Export napi_get_uv_event_loop failed, status=" << status;
        g_initialized.store(false);
        return;
    }

    int ret = uv_async_init(g_main_loop, &g_main_async, &OnMainAsync);
    if (ret != 0) {
        KR_LOG_ERROR << "KRMainThread::Export uv_async_init failed, ret=" << ret;
        g_initialized.store(false);
        return;
    }
    // 不让常驻 async 阻止 loop 退出。
    uv_unref(reinterpret_cast<uv_handle_t *>(&g_main_async));
}

void KRMainThread::RunOnMainThread(std::function<void()> task, int delayMilliseconds) {
    if (!task) {
        return;
    }
    if (!g_initialized.load() || g_main_loop == nullptr) {
        // 尚未初始化（理论上不应发生），降级为同步执行以避免任务丢失。
        // 本 fallback 路径本身不在 libuv 回调上下文，但 caller 期待“调用后 task
        // 安全运行”，同样需要边界 fail-fast，与 libuv 路径口径一致。
        KR_LOG_ERROR << "KRMainThread::RunOnMainThread before Export, fallback to inline run";
        RunWithFatalGuard("KRMainThread.Inline.fallback", task);
        return;
    }

    if (IsCurrentMainThread()) {
        // 已经在主线程（loop 线程），可以直接安全地操作 uv 句柄。
        if (delayMilliseconds <= 0) {
            // 立即执行：保持与原实现一致的"同步直跑"语义。
            // 这里 caller 可能是任意业务栈帧（业务组件在主线程调用 RunOnMainThread），
            // 严格说允许异常逃出 caller 也是合法的；但为了跟 libuv 路径同口径、
            // 且避免 caller 在"主线程 inline" vs "跨线程异步" 两种环境下行为不一致，
            // 这里同样走 fail-fast。
            RunWithFatalGuard("KRMainThread.Inline.same-thread", task);
        } else {
            StartTimerOnMainThread(std::move(task), delayMilliseconds);
        }
        return;
    }

    // 跨线程：必须经由 uv_async_send 把任务带回主线程，再在主线程上视情况起 timer。
    EnqueueAndNotify(std::move(task), delayMilliseconds);
}

// 语义说明（避免调用方误解）：
//   * 该 API 仅承诺 "task 在下一次 loop 回合执行"（相对当前调用点），
//     不承诺多次调用之间的批量原子性。
//   * 实现上，多次 RunOnMainThreadForNextLoop 会依次 push 到同一个 pending 队列，
//     由主线程一次 OnMainAsync 回调 swap 出全部任务、在同一主线程栈上顺序执行；
//     因此"同一 caller tick 内提交的多个任务"天然会在同一次 OnMainAsync 内跑完，
//     UI 层看到的就是原子的。但这**是实现副产物，不是 API 契约**。
//   * 若某任务内部又调用 RunOnMainThreadForNextLoop 提交新任务，新任务会落到
//     再下一次 loop 回合执行——这是"下一帧"的语义本身，不是 bug。
//   * 若未来出现"必须与其它 NextLoop 任务原子生效"的调用方，需要显式合批，
//     不要依赖当前 OnMainAsync 的 swap-drain 实现细节。
void KRMainThread::RunOnMainThreadForNextLoop(std::function<void()> task) {
    if (!task) {
        return;
    }
    if (!g_initialized.load() || g_main_loop == nullptr) {
        KR_LOG_ERROR << "KRMainThread::RunOnMainThreadForNextLoop before Export, fallback to inline run";
        RunWithFatalGuard("KRMainThread.Inline.fallback", task);
        return;
    }
    // 不论当前是否在主线程，都强制走 uv_async 投递，保证在"下一次 loop 回合"才执行。
    EnqueueAndNotify(std::move(task), 0);
}

bool KRMainThread::IsCurrentOnMainThread() {
    // Export 之前 g_main_thread_id 未赋值，无法做出可靠判断；
    // 此时一律返回 false，让调用方走"非主线程"安全路径（跨线程投递）。
    if (!g_initialized.load()) {
        return false;
    }
    return IsCurrentMainThread();
}