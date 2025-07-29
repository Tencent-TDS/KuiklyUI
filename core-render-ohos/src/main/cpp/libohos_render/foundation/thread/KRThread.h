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

#ifndef CORE_RENDER_OHOS_KRTHREAD_H
#define CORE_RENDER_OHOS_KRTHREAD_H

#include <cassert>
#include <chrono>
#include <condition_variable>
#include <functional>
#include <future>
#include <mutex>
#include <queue>
#include <thread>
#include "KRDelayThread.h"

#include "libohos_render/utils/KRRenderLoger.h"
class KRThread {
 public:
    explicit KRThread(const std::string &name) : m_stop(false) {
        m_workerThread = std::thread([this] {
            m_workerThreadId = std::this_thread::get_id();
            this->Worker();
        });
        pthread_setname_np(m_workerThread.native_handle(), name.c_str());

        m_delayThread = new KRDelayThread(name + "d");
    }

    ~KRThread() {
        delete m_delayThread;
        m_delayThread = nullptr;
        {
            std::unique_lock<std::mutex> lock(m_mutex);
            m_stop = true;
            m_condition.notify_one();
        }
        m_workerThread.join();
    }

    void DispatchAsync(const std::function<void()> &task, int delayMilliseconds = 0) {
        if (delayMilliseconds > 0) {
            m_delayThread->DispatchAsync([task, this] { this->DispatchAsync(task, 0); }, delayMilliseconds);
            return;
        }
        if (IsCurrentThreadWorkerThread()) {
            // 如果当前线程已经是工作线程，直接将任务添加到队列
            std::unique_lock<std::mutex> lock(m_mutex);
            m_tasks.emplace(task);
        } else {
            // 否则，将任务添加到队列
            {
                std::unique_lock<std::mutex> lock(m_mutex);
                m_tasks.emplace(task);
            }
            m_condition.notify_one();
        }
    }

    void DispatchSync(const std::function<void()> &task) {
        DirectRunOnCurThread(task);
        return;
    }

    void DirectRunOnCurThread(const std::function<void()> &task) {
        if (m_isExecutingTask.load()) {
            task();
        } else {
            auto start = std::chrono::steady_clock::now();  // 记录开始时间
            bool didHandleTask = false;
            while (!SyncMainTaskMutex(false, false, false)) {
                // 检查是否超时
                auto now = std::chrono::steady_clock::now();
                auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now - start);
                if (duration.count() > 100) {
                    break;  // 超时，跳出循环
                }
                // 尝试获取互斥锁
                if (TaskMutex(true, false, false)) {
                    m_isExecutingTask.store(true);  // 设置正在执行任务标志
                    task();
                    m_isExecutingTask.store(false);  // 清除正在执行任务标志
                    TaskMutex(false, true, false);
                    didHandleTask = true;
                    break;  // 任务执行完成，跳出循环
                }
            }
            if (!didHandleTask) {
                KR_LOG_INFO << "DispatchAsync when run DirectRunOnCurThread";
                DispatchAsync(task);
            }
        }
    }

    bool TaskMutex(bool try_lock, bool is_set, bool value) {
        std::unique_lock<std::mutex> lock(m_taskMutex);
        if (try_lock) {
            if (m_mutex_locked) {
                return false;
            } else {
                m_mutex_locked = true;
                return true;
            }
        }
        if (is_set) {
            m_mutex_locked = value;
        }
        return m_mutex_locked;
    }

    bool SyncMainTaskMutex(bool try_lock, bool is_set, bool value) {
        std::unique_lock<std::mutex> lock(m_syncMainTaskMutex);
        if (try_lock) {
            if (m_sync_main_task_locked) {
                return false;
            } else {
                m_sync_main_task_locked = true;
                return true;
            }
        }
        if (is_set) {
            m_sync_main_task_locked = value;
        }
        return m_sync_main_task_locked;
    }

    bool IsCurrentThreadWorkerThread() const {
        return std::this_thread::get_id() == m_workerThreadId;
    }

    void AssertCurrentThreadWorker() {
        assert(IsCurrentThreadWorkerThread() && "This is NOT the worker thread.");
    }

 private:
    void Worker() {
        while (true) {
            std::queue<std::function<void()>> tasks;
            {
                std::unique_lock<std::mutex> lock(m_mutex);
                m_condition.wait(lock, [this] { return m_stop || !m_tasks.empty(); });

                if (m_stop && m_tasks.empty()) {
                    break;
                }

                std::swap(tasks, m_tasks);  // 将m_tasks所有任务一次性移动到tasks
            }
            {
                if (TaskMutex(true, false, false)) {
                    while (!tasks.empty()) {
                        auto &task = tasks.front();
                        task();
                        tasks.pop();
                    }
                    TaskMutex(false, true, false);
                } else {
                    std::unique_lock<std::mutex> lock(m_mutex);
                    while (!tasks.empty()) {
                        m_tasks.push(std::move(tasks.front()));
                        tasks.pop();
                    }
                }
            }
        }
    }

    std::queue<std::function<void()>> m_tasks;
    std::mutex m_mutex;
    std::mutex m_taskMutex;
    bool m_mutex_locked = false;
    std::mutex m_syncMainTaskMutex;
    bool m_sync_main_task_locked = false;
    std::condition_variable m_condition;
    bool m_stop = false;
    std::thread m_workerThread;
    std::thread::id m_workerThreadId;
    KRDelayThread *m_delayThread = nullptr;
    std::atomic<bool> m_isExecutingTask{false};
};

#endif  // CORE_RENDER_OHOS_KRTHREAD_H
