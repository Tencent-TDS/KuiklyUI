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

#include <assert.h>
#include <atomic>
#include <chrono>
#include <cstdlib>
#include <exception>
#include <future>
#include <memory>
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

    // RAII：在主线程上运行 task 期间，让 IsCurrentOnContextThread() 把主线程
    // 视为 "context 线程"。被 DirectRunOnMainThread(sync=true) 与
    // ScheduleTaskOnMainThread(sync=true) 投递回主线程的路径共用，确保两条
    // 路径下 IsCurrentOnContextThread() 的判定行为对称一致。
    // 异常路径下析构同样会落标，不会污染全局状态。
    class MainThreadContextGuard {
     public:
        MainThreadContextGuard() {
            mainThreadId = std::this_thread::get_id();
            runningOnMainThread.store(true);
        }
        ~MainThreadContextGuard() {
            runningOnMainThread.store(false);
        }
        MainThreadContextGuard(const MainThreadContextGuard &) = delete;
        MainThreadContextGuard &operator=(const MainThreadContextGuard &) = delete;
    };
};

std::atomic_bool KRContextSchedulerMultiThreaded::runningOnMainThread{false};
std::thread::id KRContextSchedulerMultiThreaded::mainThreadId;

void KRContextSchedulerMultiThreaded::DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) {
    if (isSync) {
        GetContextThread()->DirectRunOnCurThread([task]() {
            // 异常安全的 set/clear：与 ScheduleTaskOnMainThread(sync=true) 共用同一 guard。
            MainThreadContextGuard ctxGuard;
            task();
        });
    } else {
        GetContextThread()->DispatchAsync(task, 0);
    }
}

void KRContextSchedulerMultiThreaded::ScheduleTask(bool sync, int delayMs, const KRSchedulerTask &task) {
    if (sync) {
        // 历史遗留路径：KRThread::DispatchSync 在全仓已被评估为死代码（只有本处作为 caller，
        // 而本函数上层 KRContextScheduler::ScheduleTask 的全部 caller 都传 sync=false）。
        // 详见 docs/review/review-methodology.md。保留代码形状仅为了不打破接口划分，
        // 但需局部抑制 deprecated 警告；未来若要激活请先重做语义（参考
        // ScheduleTaskOnMainThread 的 promise/future 范式）。
#if defined(__clang__) || defined(__GNUC__)
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
#endif
        GetContextThread()->DispatchSync(task);
#if defined(__clang__) || defined(__GNUC__)
#pragma GCC diagnostic pop
#endif
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
            KRMainThread::RunOnMainThread([task, donePromise]() {
                // 与 DirectRunOnMainThread(sync=true) 对称：让 task 在主线程执行
                // 期间被 IsCurrentOnContextThread() 视为"在 context 线程上"，
                // 否则相同的 task 取决于派发路径会得到不一致的判定结果。
                // 用 RAII 守卫保证异常路径下也能正确落标，避免污染全局状态。
                MainThreadContextGuard ctxGuard;
                try {
                    task();
                    donePromise->set_value();
                } catch (...) {
                    // lambda 仅执行一次，shared state 此时一定未被 set，
                    // set_exception 不会抛 promise_already_satisfied，无需再嵌套 try。
                    donePromise->set_exception(std::current_exception());
                }
            });
            using namespace std::chrono_literals;
            constexpr auto kSyncMainTaskWarnTimeout = 5s;
            if (doneFuture.wait_for(kSyncMainTaskWarnTimeout) == std::future_status::timeout) {
                // 各路径 fail-fast 同口径。throw 出去也走不到任何业务可达的 catch 点：
                //   - ToCallArkTSMethod / SyncCallArkTSMethod / KRForwardArkTSModule 都不接异常，
                //   - 一路冒到 napi C ABI 边界被 KRRenderCore.ABI.CallNative 的
                //     RunWithFatalGuard 接住 → std::abort()。
                // 所以这里直接走 __assert_fail 让 coredump 直接携带 file:line:func，
                // 避免栈 unwind 现场失真；也不会让 caller 误以为“这个 throw 可以 catch”
                // 这种 API 双重含义陷阱。裸调 __assert_fail（而非 assert 宏）确保 release
                // 也一定触发，与 KRThreadChecker.cpp 现有用法一致。
                KR_LOG_ERROR << "ScheduleTaskOnMainThread(sync, worker) wait timeout (>5s), "
                                "possible main<->worker deadlock; aborting to preserve crash context";
                __assert_fail("ScheduleTaskOnMainThread(sync, worker) wait timeout (>5s), "
                              "possible main<->worker deadlock",
                              __FILE__, __LINE__, __func__);
            }
            doneFuture.get();  // 正常返回 / rethrow 主线程异常 / broken_promise
            return;
        }
        // 既不是主线程也不是 worker 线程：按设计这条路径不应该出现（契约违反）。
        // 与其默默走"投递回主线程同步等"的慢路径掩盖问题，不如立即 __assert_fail，
        // 让调用方在第一现场暴露错误的线程模型假设（core dump 里能直接看到调用栈，
        // 并携带 file:line:func 元数据）。
        KR_LOG_ERROR << "ScheduleTaskOnMainThread(sync) called from unexpected thread "
                        "(neither main nor kuikly worker); aborting to expose caller bug";
        __assert_fail("ScheduleTaskOnMainThread(sync) called from unexpected thread "
                      "(neither main nor kuikly worker)",
                      __FILE__, __LINE__, __func__);
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
    // 与 sync 分支一致：第三方线程不应调用本接口（契约违反），
    // 立即 __assert_fail 暴露 caller 的线程模型 bug。
    KR_LOG_ERROR << "ScheduleTaskOnMainThread(async) called from unexpected thread "
                    "(neither main nor kuikly worker); aborting to expose caller bug";
    __assert_fail("ScheduleTaskOnMainThread(async) called from unexpected thread "
                  "(neither main nor kuikly worker)",
                  __FILE__, __LINE__, __func__);
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
 * @param mode 0 ：默认模式，多线程， 1 ：非 0 即视为单线程同步模式
 *
 * 线程模式暂时仅允许深度合作用户进行设置，暂不暴露到头文件。
 */
void KRSetThreadingMode(int mode) {
    // 显式 mode != 0 比 !!mode 更直观；ThreadingMode 是二元枚举（MultiThread / SingleThread），
    // 这里仅做"非 0 即 SingleThread"的语义映射。
    const auto target = (mode != 0) ? KRContextScheduler::ThreadingMode::SingleThread
                                    : KRContextScheduler::ThreadingMode::MultiThread;
    KRContextScheduler::SetThreadingMode(target);
}
EXTERN_C_END
