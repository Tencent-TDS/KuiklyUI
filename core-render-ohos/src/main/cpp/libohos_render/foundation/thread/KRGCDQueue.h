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

#ifndef CORE_RENDER_OHOS_KRGCDQUEUE_H
#define CORE_RENDER_OHOS_KRGCDQUEUE_H

#include <atomic>
#include <condition_variable>
#include <functional>
#include <iostream>
#include <mutex>
#include <queue>
#include <thread>
#include <vector>

class KRGCDQueue {
 public:
    // 删除拷贝构造函数和赋值操作符
    KRGCDQueue(const KRGCDQueue &) = delete;
    KRGCDQueue &operator=(const KRGCDQueue &) = delete;

    // 提供一个静态方法来获取类的唯一实例
    static KRGCDQueue &GetInstance() {
        static KRGCDQueue instance(4);  // 最大4条线程并行
        return instance;
    }

    ~KRGCDQueue() {}

    /**
     * 切换到多线程环境执行任务
     * @param task 任务
     */
    void DispatchAsync(std::function<void()> task) {
        {
            std::unique_lock<std::mutex> lock(queue_mutex);
            tasks.emplace(task);
        }
        condition.notify_one();
    }

 private:
    // 将构造函数设为私有
    explicit KRGCDQueue(size_t num_threads) {
        for (size_t i = 0; i < num_threads; ++i) {
            threads.emplace_back([this] {
                while (true) {
                    std::function<void()> task;
                    {
                        std::unique_lock<std::mutex> lock(queue_mutex);
                        condition.wait(lock, [this] { return !tasks.empty(); });
                        if (tasks.empty()) {
                            return;
                        }
                        task = std::move(tasks.front());
                        tasks.pop();
                    }
                    task();
                }
            });
        }
    }

    std::vector<std::thread> threads;
    std::queue<std::function<void()>> tasks;
    std::mutex queue_mutex;
    std::condition_variable condition;
};

#endif  // CORE_RENDER_OHOS_KRGCDQUEUE_H
