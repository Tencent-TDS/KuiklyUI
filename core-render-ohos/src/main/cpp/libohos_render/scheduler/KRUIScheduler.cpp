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

#include "libohos_render/scheduler/KRUIScheduler.h"

#include "libohos_render/scheduler/KRContextScheduler.h"

// should call on context线程
void KRUIScheduler::AddTaskToMainQueueWithTask(const KRSchedulerTask &task) {
    std::lock_guard<std::mutex> lock(m_mutex_);
    m_main_thread_tasks_on_context_queue_.push_back(task);
    SetNeedSyncMainQuequeTasks();
}
// should call on context线程
void KRUIScheduler::PerformSyncMainQueueTasksBlockIfNeed(bool sync) {
    if (m_need_sync_main_queue_tasks_block_) {
        m_need_sync_main_queue_tasks_block_(sync);
        m_need_sync_main_queue_tasks_block_ = nullptr;
    }
}
// should call on main thread
void KRUIScheduler::PerformWhenViewDidLoad(const KRSchedulerTask &task) {
    if (m_view_did_load_) {
        task();
    } else {
        m_view_did_load_main_thread_tasks_.push_back(task);
    }
}

bool KRUIScheduler::IsPerformMainTasking() {
    return m_performing_main_queue_task_;
}

void KRUIScheduler::PerformTaskWhenDidEnd(const KRSchedulerTask &task) {
    m_did_end_main_thread_tasks_.push_back(task);
}

void KRUIScheduler::Destroy() {
    std::lock_guard<std::mutex> lock(m_mutex_);
    m_is_destroyed_ = true;
    m_delegate_ = nullptr;
    m_need_sync_main_queue_tasks_block_ = nullptr;
    m_main_thread_tasks_on_context_queue_.clear();
}

void KRUIScheduler::SetNeedSyncMainQuequeTasks() {
    if (m_need_sync_main_queue_tasks_block_ == nullptr) {
        std::weak_ptr<IKRScheduler> weakSelf = shared_from_this();
        m_need_sync_main_queue_tasks_block_ = [weakSelf](bool sync) {
            auto strongSelf = weakSelf.lock();
            if (!strongSelf) {
                return;
            }
            
            auto scheduler = std::dynamic_pointer_cast<KRUIScheduler>(strongSelf);
            if (scheduler->m_is_destroyed_) {
                return;
            }
            
            if (scheduler->m_delegate_) {
                scheduler->m_delegate_->WillPerformUITasksWithScheduler();
            }
            
            std::vector<KRSchedulerTask> performTasks;
            {
                std::lock_guard<std::mutex> lock(scheduler->m_mutex_);
                performTasks = scheduler->m_main_thread_tasks_on_context_queue_;
                scheduler->m_main_thread_tasks_on_context_queue_ = {};
                scheduler->m_main_thread_tasks_.insert(scheduler->m_main_thread_tasks_.end(), performTasks.begin(), performTasks.end());
            }
            
            scheduler->PerformOnMainQueueWithTask(sync, [weakSelf] {
                auto strongSelf = weakSelf.lock();
                if (!strongSelf) {
                    return;
                }
                
                auto scheduler = std::dynamic_pointer_cast<KRUIScheduler>(strongSelf);
                
                std::vector<KRSchedulerTask> mainTasks;
                {
                    std::lock_guard<std::mutex> lock(scheduler->m_mutex_);
                    mainTasks = scheduler->m_main_thread_tasks_;
                    scheduler->m_main_thread_tasks_ = {};
                }
                scheduler->RunMainQueueTasks(mainTasks);
            });
        };
        KRContextScheduler::ScheduleTask(false, 0, [weakSelf] { 
            auto strongSelf = weakSelf.lock();
            if (!strongSelf) {
                return;
            }
            auto scheduler = std::dynamic_pointer_cast<KRUIScheduler>(strongSelf);
            scheduler->PerformSyncMainQueueTasksBlockIfNeed(false); 
        });
    }
}

void KRUIScheduler::PerformOnMainQueueWithTask(bool sync, const std::function<void()> &task) {
    if (sync) {
        m_main_thread_task_wait_to_sync_block_ = task;
        KRMainThread::RunOnMainThread(
            [this] {  // 兜底执行
                this->PerformMainThreadTaskWaitToSyncBlockIfNeed();
            },
            1);
    } else {
        KRMainThread::RunOnMainThread(task);
    }
}

void KRUIScheduler::RunMainQueueTasks(const std::vector<KRSchedulerTask> &tasks) {
    // 主线程
    m_performing_main_queue_task_ = true;
    for (size_t i = 0; i < tasks.size(); i++) {
        tasks[i]();
    }
    m_performing_main_queue_task_ = false;
    if (!m_view_did_load_) {
        m_view_did_load_ = true;
        auto viewDidLoadTasks = m_view_did_load_main_thread_tasks_;
        m_view_did_load_main_thread_tasks_ = {};
        for (size_t i = 0; i < viewDidLoadTasks.size(); i++) {
            viewDidLoadTasks[i]();
        }
    }
    if (m_did_end_main_thread_tasks_.size() > 0) {
        auto tasks = m_did_end_main_thread_tasks_;
        m_did_end_main_thread_tasks_ = {};
        for (size_t i = 0; i < tasks.size(); i++) {
            tasks[i]();
        }
    }
}

void KRUIScheduler::PerformMainThreadTaskWaitToSyncBlockIfNeed() {
    if (m_main_thread_task_wait_to_sync_block_) {
        m_main_thread_task_wait_to_sync_block_();
        m_main_thread_task_wait_to_sync_block_ = nullptr;
    }
}
