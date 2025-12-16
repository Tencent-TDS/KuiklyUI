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

#include "KRMonitorThread.h"

KRMonitorThread& KRMonitorThread::GetInstance() {
    static KRMonitorThread instance;
    return instance;
}

KRMonitorThread::KRMonitorThread() {
    thread_ = std::thread(&KRMonitorThread::ThreadLoop, this);
}

KRMonitorThread::~KRMonitorThread() {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        stop_ = true;
    }
    cv_.notify_all();
    if (thread_.joinable()) {
        thread_.join();
    }
}

void KRMonitorThread::PostTask(Task task) {
    PostDelayedTask(std::move(task), 0);
}

void KRMonitorThread::PostDelayedTask(Task task, long delay_ms) {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        auto execute_time = std::chrono::steady_clock::now() + std::chrono::milliseconds(delay_ms);
        task_queue_.push({std::move(task), execute_time});
    }
    cv_.notify_one();
}

void KRMonitorThread::ThreadLoop() {
    while (true) {
        Task current_task;
        {
            std::unique_lock<std::mutex> lock(mutex_);
            
            while (!stop_ && task_queue_.empty()) {
                cv_.wait(lock);
            }
            
            if (stop_ && task_queue_.empty()) return;

            auto now = std::chrono::steady_clock::now();
            const auto& top_task = task_queue_.top();
            
            if (now >= top_task.execute_time) {
                current_task = top_task.task;
                task_queue_.pop();
            } else {
                cv_.wait_until(lock, top_task.execute_time);
                continue; 
            }
        }
        if (current_task) {
            current_task();
        }
    }
}