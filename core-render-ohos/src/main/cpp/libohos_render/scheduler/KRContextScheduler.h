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

#ifndef CORE_RENDER_OHOS_KRCONTEXTSCHEDULER_H
#define CORE_RENDER_OHOS_KRCONTEXTSCHEDULER_H

#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/foundation/thread/KRThread.h"
#include "libohos_render/scheduler/IKRScheduler.h"

class KRContextSchedulerInternal;
class KRContextScheduler {
 public:
    enum ThreadingMode {
        MultiThread = 0,  // 默认线程模型，Kuikly逻辑在独立线程执行
        SingleThread = 1  // 单线程模型，Kuikly逻辑在主线程执行
    };

    /**
     * 调度任务到Context线程异步执行
     * @param delayMs 延时毫秒，0为不延时
     * @param task 任务闭包
     */
    static void ScheduleTask(int delayMs, const KRSchedulerTask &task);

    /**
     * Context线程调度任务到主线程执行(注：该方法只能在主线程或Context线程被调用)
     * @param sync 是否同步执行
     * @param task 任务闭包
     */
    static void ScheduleTaskOnMainThread(bool sync, const KRSchedulerTask &task);
    /**
     * 直接在主线线程同步执行在Context线程安全的任务
     * @param sync 是否同步执行
     * @param task 任务闭包
     */
    static void DirectRunOnMainThread(bool isSync, const KRSchedulerTask &task);

    /**
     * 在 window 内拿到 context 任务锁则在主线程同步执行 task 并返回 true；否则不执行，返回 false
     */
    static bool TryDirectRunOnMainThread(std::chrono::microseconds window, const KRSchedulerTask &task);

    // 当前线程是否正持有 context 任务锁 m_taskMutex。单线程模式下恒为 false。
    static bool IsContextTaskLockHeldByCurrentThread();

    /**
     * 在主线程上持有 context 任务锁，执行共享队列中的下一个 context 任务。
     * @return 执行了一个任务返回 true；队列为空或没能立刻拿到锁返回 false
     */
    static bool TryRunNextContextTaskOnMainThread();

    /**
     * 暂停或恢复 context 线程执行任务。暂停期间有待执行任务时，在 context 线程调用 notifier。
     * 单线程模式下为空操作。
     */
    static void SetContextThreadParked(bool parked, const std::function<void()> &notifier);

    /**
     * 判断当前是否在Context线程
     */
    static bool IsCurrentOnContextThread();

    /**
     * 设置线程模型，初始化kuikly前调用，初始化后调用无作用
     * @param mode 单线程或多线程模式
     */
    static void SetThreadingMode(ThreadingMode mode);

 private:
    static std::shared_ptr<KRContextSchedulerInternal> GetInstance();
};

#endif  // CORE_RENDER_OHOS_KRCONTEXTSCHEDULER_H
