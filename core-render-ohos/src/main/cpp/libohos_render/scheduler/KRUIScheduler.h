/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRUISCHEDULER_H
#define CORE_RENDER_OHOS_KRUISCHEDULER_H

#include <functional>
#include <future>
#include <mutex>
#include <thread>
#include <vector>
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/scheduler/IKRScheduler.h"

using KRSyncSchedulerTask = std::function<void(bool sync)>;

class KRRenderUISchedulerDelegate {
 public:
    ~KRRenderUISchedulerDelegate() = default;
    // UI任务将要执行前回调
    virtual void WillPerformUITasksWithScheduler() = 0;
};

class KRUIScheduler : public IKRScheduler {
 public:
    explicit KRUIScheduler(KRRenderUISchedulerDelegate *delegate) : m_delegate_(delegate) {}

    // should call on context线程
    void AddTaskToMainQueueWithTask(const KRSchedulerTask &task);
    // should call on context线程
    void PerformSyncMainQueueTasksBlockIfNeed(bool sync);
    // should call on main thread
    void PerformWhenViewDidLoad(const KRSchedulerTask &task);

    void PerformTaskWhenDidEnd(const KRSchedulerTask &task);

    void Destroy();
    void PerformMainThreadTaskWaitToSyncBlockIfNeed();
    /**
     * 是否正在执行主线程任务中
     */
    bool IsPerformMainTasking();

    void ResetDelegate(){
        m_delegate_ = nullptr;
    }
 private:
    void SetNeedSyncMainQuequeTasks();

    void PerformOnMainQueueWithTask(bool sync, const std::function<void()> &task);

    void RunMainQueueTasks(const std::vector<KRSchedulerTask> &tasks);

    bool m_is_destroyed_ = false;
    KRSyncSchedulerTask m_need_sync_main_queue_tasks_block_ = nullptr;
    KRRenderUISchedulerDelegate *m_delegate_ = nullptr;
    bool m_performing_main_queue_task_ = false;
    std::vector<KRSchedulerTask> m_main_thread_tasks_on_context_queue_;
    std::vector<KRSchedulerTask> m_main_thread_tasks_;
    std::vector<KRSchedulerTask> m_view_did_load_main_thread_tasks_;
    std::vector<KRSchedulerTask> m_did_end_main_thread_tasks_;
    std::function<void()> m_main_thread_task_wait_to_sync_block_ = nullptr;
    std::mutex m_mutex_;
    bool m_view_did_load_ = false;
};

#endif  // CORE_RENDER_OHOS_KRUISCHEDULER_H
