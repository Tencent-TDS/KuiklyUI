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

#ifndef CORE_RENDER_OHOS_KRDELAYTHREAD_H
#define CORE_RENDER_OHOS_KRDELAYTHREAD_H

#include <chrono>
#include <condition_variable>
#include <functional>
#include <mutex>
#include <queue>
#include <thread>

struct Task {
    std::chrono::steady_clock::time_point execTime;
    std::function<void()> func;

    bool operator<(const Task &other) const {
        // 注意：我们需要反转比较运算符，因为 std::priority_queue 是最大堆
        return execTime > other.execTime;
    }
};

class KRDelayThread {
 public:
    explicit KRDelayThread(const std::string &name) : m_stop(false) {
        m_dispatcherThread = std::thread([this] { this->Dispatcher(); });
        pthread_setname_np(m_dispatcherThread.native_handle(), name.c_str());
    }

    ~KRDelayThread() {
        {
            std::unique_lock<std::mutex> lock(m_mutex);
            m_stop = true;
            m_condition.notify_one();
        }
        m_dispatcherThread.join();
    }

    void DispatchAsync(const std::function<void()> &task, int delayMilliseconds = 0) {
        std::unique_lock<std::mutex> lock(m_mutex);
        m_tasks.emplace(Task{std::chrono::steady_clock::now() + std::chrono::milliseconds(delayMilliseconds), task});
        m_condition.notify_one();
    }

 private:
    void Dispatcher() {
        while (true) {
            Task task;
            {
                std::unique_lock<std::mutex> lock(m_mutex);
                while (!m_stop && (m_tasks.empty() || std::chrono::steady_clock::now() < m_tasks.top().execTime)) {
                    if (m_tasks.empty()) {
                        m_condition.wait(lock);
                    } else {
                        auto time = m_tasks.top().execTime;
                        m_condition.wait_until(lock, time);
                    }
                }

                if (m_stop) {
                    break;
                }

                task = std::move(const_cast<Task &>(m_tasks.top()));
                m_tasks.pop();
            }

            task.func();
        }
    }

    std::priority_queue<Task> m_tasks;
    std::mutex m_mutex;
    std::condition_variable m_condition;
    bool m_stop = false;
    std::thread m_dispatcherThread;
};

#endif  // CORE_RENDER_OHOS_KRDELAYTHREAD_H
