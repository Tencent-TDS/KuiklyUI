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

#include "libohos_render/scheduler/KRContextScheduler.h"

#include <atomic>
#include <chrono>
#include <cstdlib>
#include <exception>
#include <future>
#include <memory>
#include <stdexcept>
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/utils/KRRenderLoger.h"

class KRContextSchedulerInternal {
 public:
    virtual ~KRContextSchedulerInternal() = default;
    virtual void ScheduleTask(bool sync, int delayMs, const KRSchedulerTask &task) = 0;
    virtual void ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) = 0;
    virtual void DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) = 0;

    virtual bool IsCurrentOnContextThread() = 0;
};

class KRContextSchedulerMultiThreaded : public KRContextSchedulerInternal {
 public:
    void ScheduleTask(bool sync, int delayMs, const KRSchedulerTask &task) override;
    void ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) override;
    void DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) override;
    bool IsCurrentOnContextThread() override;

 private:
    static KRThread *GetContextThread() {
        static KRThread *gContextThread = new KRThread("kuikly");
        return gContextThread;
    }
    static std::atomic_bool runningOnMainThread;
    static std::thread::id mainThreadId;
};

std::atomic_bool KRContextSchedulerMultiThreaded::runningOnMainThread{false};
std::thread::id KRContextSchedulerMultiThreaded::mainThreadId;

void KRContextSchedulerMultiThreaded::DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) {
    if (isSync) {
        GetContextThread()->DirectRunOnCurThread([task]() {
            mainThreadId = std::this_thread::get_id();
            runningOnMainThread.store(true);
            task();
            runningOnMainThread.store(false);
        });
    } else {
        GetContextThread()->DispatchAsync(task, 0);
    }
}

void KRContextSchedulerMultiThreaded::ScheduleTask(bool sync, int delayMs, const KRSchedulerTask &task) {
    if (sync) {
        GetContextThread()->DispatchSync(task);
    } else {
        GetContextThread()->DispatchAsync(task, delayMs);
    }
}

void KRContextSchedulerMultiThreaded::ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) {
    if (!task) {
        return;
    }
    auto *ctx = GetContextThread();
    const bool onWorker = ctx->IsCurrentThreadWorkerThread();
    const bool onMain = KRMainThread::IsCurrentOnMainThread();

    if (sync) {
        if (onMain) {
            // 真在主线程，直跑即可。
            task();
            return;
        }
        if (onWorker) {
            // RAII 举旗：cv.wait 期间通知 DirectRunOnCurThread 立即降级异步，
            // 避免外部 caller 在 worker 持锁等主线程时 yield-spin 100ms。
            // 离开作用域时 guard 析构自动落旗，无需手动配对。
            KRThread::WorkerAwaitingMainTaskGuard guard(ctx);
            // 用 shared_ptr<promise<void>> 同步：
            //   - 任务正常结束 -> set_value
            //   - 任务抛异常   -> set_exception，并在 future::get 时原样 rethrow
            //   - lambda 因任何原因被丢弃 / 提前析构 -> promise 析构自动投递
            //     broken_promise，等待方不会永久挂死
            //   - 等待方超时 fast-fail 退出也安全：shared state 由 shared_ptr
            //     托管，比栈帧活得久，不会悬垂
            auto donePromise = std::make_shared<std::promise<void>>();
            auto doneFuture = donePromise->get_future();
            KRMainThread::RunOnMainThread([task, donePromise]() mutable {
                try {
                    task();
                    donePromise->set_value();
                } catch (...) {
                    try {
                        donePromise->set_exception(std::current_exception());
                    } catch (...) {
                        // set_exception 自身极少抛（如 promise_already_satisfied），
                        // 这里吞掉避免在主线程派发回调里再次外抛。
                    }
                }
            });
            using namespace std::chrono_literals;
            constexpr auto kSyncMainTaskWarnTimeout = 3s;
            if (doneFuture.wait_for(kSyncMainTaskWarnTimeout) == std::future_status::timeout) {
                KR_LOG_ERROR << "ScheduleTaskOnMainThread(sync, worker) wait timeout (>3s), "
                                "possible main<->worker deadlock; abort wait";
                throw std::runtime_error("ScheduleTaskOnMainThread sync timeout on worker thread");
            }
            doneFuture.get();  // 正常返回 / rethrow 主线程异常 / broken_promise
            return;
        }
        // 既不是主线程也不是 worker 线程：按设计这条路径不应该出现。
        // 与其默默走"投递回主线程同步等"的慢路径掩盖问题，不如立即 abort，
        // 让调用方在第一现场暴露错误的线程模型假设（core dump 里能直接看到调用栈）。
        KR_LOG_ERROR << "ScheduleTaskOnMainThread(sync) called from unexpected thread "
                        "(neither main nor kuikly worker); aborting to expose caller bug";
        std::abort();
    }

    // async 分支：
    if (onMain) {
        // 已在主线程，沿用旧的 inline 直跑语义。
        task();
        return;
    }
    if (onWorker) {
        // worker 线程：跨线程投递到主线程异步执行。
        KRMainThread::RunOnMainThread(task);
        return;
    }
    // 与 sync 分支一致：第三方线程不应调用本接口，立即 abort 暴露 caller 的线程模型 bug。
    KR_LOG_ERROR << "ScheduleTaskOnMainThread(async) called from unexpected thread "
                    "(neither main nor kuikly worker); aborting to expose caller bug";
    std::abort();
}

bool KRContextSchedulerMultiThreaded::IsCurrentOnContextThread() {
    if (runningOnMainThread.load()) {
        return std::this_thread::get_id() == mainThreadId;
    }
    return GetContextThread()->IsCurrentThreadWorkerThread();
}

class KRContextSchedulerSingleThreaded : public KRContextSchedulerInternal {
 public:
    void ScheduleTask(bool sync, int delayMs, const KRSchedulerTask &task) override;
    void ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) override;
    void DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) override;
    bool IsCurrentOnContextThread() override;
    std::thread::id mainThreadId;
};

void KRContextSchedulerSingleThreaded::DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) {
    mainThreadId = std::this_thread::get_id();
    if (isSync) {
        task();
    } else {
        KRMainThread::RunOnMainThread(task, 0);
    }
}

void KRContextSchedulerSingleThreaded::ScheduleTask(bool sync, int delayMs, const KRSchedulerTask &task) {
    if (sync) {
        task();
    } else {
        KRMainThread::RunOnMainThread(task, delayMs);
    }
}

void KRContextSchedulerSingleThreaded::ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) {
    if (sync) {
        task();
    } else {
        KRMainThread::RunOnMainThread([task] { task(); });
    }
}

bool KRContextSchedulerSingleThreaded::IsCurrentOnContextThread() {
    return mainThreadId == std::this_thread::get_id();
}

static KRContextScheduler::ThreadingMode gThreadingMode = KRContextScheduler::ThreadingMode::MultiThread;

void KRContextScheduler::SetThreadingMode(ThreadingMode mode) {
    // 仅应在初始化前调用一次，并仅仅使用一次，无需考虑多线程问题
    gThreadingMode = mode;
}

std::shared_ptr<KRContextSchedulerInternal> KRContextScheduler::GetInstance() {
    static std::shared_ptr<KRContextSchedulerInternal> instance_ = nullptr;
    static std::once_flag flag;
    std::call_once(flag, []() {
        instance_ = gThreadingMode == KRContextScheduler::ThreadingMode::MultiThread
                        ? std::dynamic_pointer_cast<KRContextSchedulerInternal>(
                              std::make_shared<KRContextSchedulerMultiThreaded>())
                        : std::dynamic_pointer_cast<KRContextSchedulerInternal>(
                              std::make_shared<KRContextSchedulerSingleThreaded>());
    });
    return instance_;
}

void KRContextScheduler::ScheduleTask(bool sync, int delayMs, const KRSchedulerTask &task) {
    GetInstance()->ScheduleTask(sync, delayMs, task);
}
void KRContextScheduler::ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) {
    GetInstance()->ScheduleTaskOnMainThread(sync, task);
}
void KRContextScheduler::DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) {
    GetInstance()->DirectRunOnMainThread(isSync, task);
}
bool KRContextScheduler::IsCurrentOnContextThread() {
    return GetInstance()->IsCurrentOnContextThread();
}

EXTERN_C_START
/**
 * 让用户设置线程模型。
 * @param mode 0 ：默认模式，多线程， 1 ：单线程同步模式
 *
 * 线程模式暂时仅允许深度合作用户进行设置，暂不暴露到头文件。
 */
void KRSetThreadingMode(int mode) {
    KRContextScheduler::SetThreadingMode(static_cast<KRContextScheduler::ThreadingMode>(!!mode));
}
EXTERN_C_END
