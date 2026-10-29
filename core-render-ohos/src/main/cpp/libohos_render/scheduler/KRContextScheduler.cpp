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
#include <exception>
#include <future>
#include <memory>
#include "libohos_render/api/include/Kuikly/KuiklyExport.h"
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/utils/KRRenderLoger.h"

// 同步主线程任务的告警超时时长（默认 10s）。
// 原为 ScheduleTaskOnMainThread(sync, worker) 内的局部 constexpr 常量，现提升为文件级
// static 变量，允许通过 KRSetSyncMainTaskWarnTimeout(int ms) 在运行期覆盖（例如测试或
// 性能敏感的调试场景，需放宽/收紧主<->worker 死锁检测的等待阈值）。单位为毫秒。
static std::chrono::milliseconds g_SyncMainTaskWarnTimeout{10000};

class KRContextSchedulerInternal {
 public:
    virtual ~KRContextSchedulerInternal() = default;
    virtual void ScheduleTask(int delayMs, const KRSchedulerTask &task) = 0;
    virtual void ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) = 0;
    virtual void DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) = 0;
    virtual bool TryDirectRunOnMainThread(std::chrono::microseconds window, const KRSchedulerTask &task) = 0;
    virtual bool TryRunNextContextTaskOnMainThread() = 0;
    virtual void SetContextThreadParked(bool parked, const std::function<void()> &notifier) = 0;

    virtual bool IsCurrentOnContextThread() = 0;
    virtual bool IsContextTaskLockHeldByCurrentThread() { return false; }
};

class KRContextSchedulerMultiThreaded : public KRContextSchedulerInternal {
 public:
    void ScheduleTask(int delayMs, const KRSchedulerTask &task) override;
    void ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) override;
    void DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) override;
    bool TryDirectRunOnMainThread(std::chrono::microseconds window, const KRSchedulerTask &task) override;
    bool TryRunNextContextTaskOnMainThread() override;
    void SetContextThreadParked(bool parked, const std::function<void()> &notifier) override;
    bool IsCurrentOnContextThread() override;
    bool IsContextTaskLockHeldByCurrentThread() override;

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

bool KRContextSchedulerMultiThreaded::TryDirectRunOnMainThread(std::chrono::microseconds window,
                                                              const KRSchedulerTask &task) {
    return GetContextThread()->TryRunOnCurThread(window, [&task]() {
        MainThreadContextGuard ctxGuard;
        // 同步 UI 提交存在 m_main_thread_task_wait_to_sync_block_，等本函数返回、锁释放后再执行。
        // 这里只把 delay 为 0 的 RunOnMainThread 推到下一轮，避免在持有 m_taskMutex 的调用栈里做 UI。
        KRMainThread::RunInNextLoopScope nextLoopScope;
        task();
    });
}

bool KRContextSchedulerMultiThreaded::IsContextTaskLockHeldByCurrentThread() {
    return GetContextThread()->IsTaskLockHeldByCurrentThread();
}

bool KRContextSchedulerMultiThreaded::TryRunNextContextTaskOnMainThread() {
    return GetContextThread()->TryRunNextPendingTaskOnCurThread([](const std::function<void()> &task) {
        // 与 TryDirectRunOnMainThread 一致：拿到 context 任务锁之后才让 IsCurrentOnContextThread() 把主线程视为 context 线程。
        MainThreadContextGuard ctxGuard;
        KRMainThread::RunInNextLoopScope nextLoopScope;
        task();
    });
}

void KRContextSchedulerMultiThreaded::SetContextThreadParked(bool parked, const std::function<void()> &notifier) {
    auto *thread = GetContextThread();
    // notifier 在 context 线程读取，只允许在首次暂停前设置一次，避免并发读写。
    static std::once_flag notifierOnce;
    std::call_once(notifierOnce, [thread, &notifier] { thread->SetParkedTaskNotifier(notifier); });
    thread->SetParked(parked);
}

void KRContextSchedulerMultiThreaded::ScheduleTask(int delayMs, const KRSchedulerTask &task) {
    GetContextThread()->DispatchAsync(task, delayMs);
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
            // 真在主线程，直跑即可。调用方要当场拿到结果，持有 context 任务锁时也不能推迟；
            // 若因此再次进入同步事件，TryRunOnCurThread 会返回 false，改成异步执行。
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
            if (doneFuture.wait_for(g_SyncMainTaskWarnTimeout) == std::future_status::timeout) {
                // 各路径 fail-fast 同口径。throw 出去也走不到任何业务可达的 catch 点：
                //   - ToCallArkTSMethod / SyncCallArkTSMethod / KRForwardArkTSModule 都不接异常，
                //   - 一路冒到 napi C ABI 边界后直接暴露给 K/N runtime，由 K/N 的
                //     unhandled-exception hook 打出完整 Kotlin 栈后 std::terminate。
                // 所以这里直接走 __assert_fail 让 coredump 直接携带 file:line:func，
                // 避免栈 unwind 现场失真；也不会让 caller 误以为“这个 throw 可以 catch”
                // 这种 API 双重含义陷阱。裸调 __assert_fail（而非 assert 宏）确保 release
                // 也一定触发，与 KRThreadChecker.cpp 现有用法一致。
                KR_LOG_ERROR << "ScheduleTaskOnMainThread(sync, worker) wait timeout (>"
                                << g_SyncMainTaskWarnTimeout.count() << "ms), "
                                "possible main<->worker deadlock; aborting to preserve crash context";
                const std::string assertMsg =
                    "ScheduleTaskOnMainThread(sync, worker) wait timeout (>" +
                    std::to_string(g_SyncMainTaskWarnTimeout.count()) + "ms), "
                    "possible main<->worker deadlock";
                __assert_fail(assertMsg.c_str(), __FILE__, __LINE__, __func__);
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
    if (onMain || onWorker) {
        // 主线程上沿用 inline 直跑语义；主线程正持有 context 任务锁执行 context 任务时，
        // RunOnMainThread 改到下一轮主线程循环执行。worker 线程上跨线程投递到主线程异步执行。
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
    void ScheduleTask(int delayMs, const KRSchedulerTask &task) override;
    void ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) override;
    void DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) override;
    bool TryDirectRunOnMainThread(std::chrono::microseconds window, const KRSchedulerTask &task) override;
    bool TryRunNextContextTaskOnMainThread() override;
    void SetContextThreadParked(bool parked, const std::function<void()> &notifier) override;
    bool IsCurrentOnContextThread() override;
    std::thread::id mainThreadId;
};

bool KRContextSchedulerSingleThreaded::TryDirectRunOnMainThread(std::chrono::microseconds /*window*/,
                                                               const KRSchedulerTask &task) {
    mainThreadId = std::this_thread::get_id();
    task();
    return true;
}

bool KRContextSchedulerSingleThreaded::TryRunNextContextTaskOnMainThread() {
    // 单线程模式下 context 任务本来就在主线程事件循环里执行，不用再从队列取。
    return false;
}

void KRContextSchedulerSingleThreaded::SetContextThreadParked(bool /*parked*/,
                                                              const std::function<void()> & /*notifier*/) {}

void KRContextSchedulerSingleThreaded::DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) {
    mainThreadId = std::this_thread::get_id();
    if (isSync) {
        task();
    } else {
        KRMainThread::RunOnMainThread(task, 0);
    }
}

void KRContextSchedulerSingleThreaded::ScheduleTask(int delayMs, const KRSchedulerTask &task) {
    // ScheduleTask 是异步的：delay 为 0 也不能在当前调用栈里直接执行。调用方可能正拿着锁，
    // 例如 KRUIScheduler::AddTaskToMainQueueWithTask 拿着 m_mutex_ 时投递下一次 UI 刷新。
    if (delayMs > 0) {
        KRMainThread::RunOnMainThread(task, delayMs);
    } else {
        KRMainThread::RunOnMainThreadForNextLoop(task);
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
        const bool multi = gThreadingMode == KRContextScheduler::ThreadingMode::MultiThread;
        KR_LOG_INFO << "KRContextScheduler created threadingMode=" << (multi ? "multi" : "single");
        instance_ = multi ? std::dynamic_pointer_cast<KRContextSchedulerInternal>(
                                std::make_shared<KRContextSchedulerMultiThreaded>())
                          : std::dynamic_pointer_cast<KRContextSchedulerInternal>(
                                std::make_shared<KRContextSchedulerSingleThreaded>());
    });
    return instance_;
}

void KRContextScheduler::ScheduleTask(int delayMs, const KRSchedulerTask &task) {
    GetInstance()->ScheduleTask(delayMs, task);
}
void KRContextScheduler::ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task) {
    GetInstance()->ScheduleTaskOnMainThread(sync, task);
}
void KRContextScheduler::DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task) {
    GetInstance()->DirectRunOnMainThread(isSync, task);
}
bool KRContextScheduler::TryDirectRunOnMainThread(std::chrono::microseconds window, const KRSchedulerTask &task) {
    return GetInstance()->TryDirectRunOnMainThread(window, task);
}

bool KRContextScheduler::IsContextTaskLockHeldByCurrentThread() {
    return GetInstance()->IsContextTaskLockHeldByCurrentThread();
}
bool KRContextScheduler::TryRunNextContextTaskOnMainThread() {
    return GetInstance()->TryRunNextContextTaskOnMainThread();
}
void KRContextScheduler::SetContextThreadParked(bool parked, const std::function<void()> &notifier) {
    GetInstance()->SetContextThreadParked(parked, notifier);
}
bool KRContextScheduler::IsCurrentOnContextThread() {
    return GetInstance()->IsCurrentOnContextThread();
}

EXTERN_C_START
/**
 * 让用户设置线程模型。
 * @param mode 0 ：默认模式，多线程， 1 ：非 0 即视为单线程同步模式
 *
 * 须在 KRContextScheduler 首次创建前调用一次。不放进 Kuikly.h，不给宿主调用。
 */
KUIKLY_EXPORT void KRSetThreadingMode(int mode) {
    // 显式 mode != 0 比 !!mode 更直观；ThreadingMode 是二元枚举（MultiThread / SingleThread），
    // 这里仅做"非 0 即 SingleThread"的语义映射。
    const auto target = (mode != 0) ? KRContextScheduler::ThreadingMode::SingleThread
                                    : KRContextScheduler::ThreadingMode::MultiThread;
    KRContextScheduler::SetThreadingMode(target);
}
EXTERN_C_END

EXTERN_C_START
/**
 * 设置同步主线程任务（ScheduleTaskOnMainThread sync=true 的 worker 等待）的告警超时时长。
 * @param ms 超时毫秒数；默认 10000(10s)。超时将触发 __assert_fail 以保留死锁现场。
 *
 * 该超时用于检测主<->worker 死锁：worker 线程同步等待主线程执行任务超过该时长即视为异常。
 *
 * 注意：g_SyncMainTaskWarnTimeout 为文件级 static 变量，本接口未做线程同步保护。
 * 仅应在初始化 kuikly 前调用一次（且只调用一次），不要多次调用或在运行期并发设置，
 * 否则存在数据竞争风险。
 */
KUIKLY_EXPORT void KRSetSyncMainTaskWarnTimeout(int ms) {
    // 负数 / 0 视为非法输入，保持默认 10s，避免把等待阈值直接打到 0 导致
    // 任何一次主线程轻微抖动都被判为死锁而触发 assert。
    if (ms > 0) {
        g_SyncMainTaskWarnTimeout = std::chrono::milliseconds(ms);
    }
}
EXTERN_C_END
