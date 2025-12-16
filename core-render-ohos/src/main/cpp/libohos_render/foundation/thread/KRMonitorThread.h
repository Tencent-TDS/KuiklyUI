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

#ifndef CORE_RENDER_OHOS_KRMONITORTHREAD_H
#define CORE_RENDER_OHOS_KRMONITORTHREAD_H

#include <thread>
#include <mutex>
#include <condition_variable>
#include <functional>
#include <queue>
#include <vector>
#include <chrono>
#include <atomic>

class KRMonitorThread {
public:
    using Task = std::function<void()>;

    static KRMonitorThread& GetInstance();

    void PostTask(Task task);

    void PostDelayedTask(Task task, long delay_ms);

    ~KRMonitorThread();

private:
    KRMonitorThread();
    void ThreadLoop();

    struct DelayedTask {
        Task task;
        std::chrono::steady_clock::time_point execute_time;

        bool operator>(const DelayedTask& other) const {
            return execute_time > other.execute_time;
        }
    };

    std::priority_queue<DelayedTask, std::vector<DelayedTask>, std::greater<DelayedTask>> task_queue_;
    std::thread thread_;
    std::mutex mutex_;
    std::condition_variable cv_;
    std::atomic<bool> stop_{false};
};

#endif //CORE_RENDER_OHOS_KRMONITORTHREAD_H
