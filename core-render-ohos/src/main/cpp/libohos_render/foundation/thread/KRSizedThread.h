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

#ifndef CORE_RENDER_OHOS_KRSIZEDTHREAD_H
#define CORE_RENDER_OHOS_KRSIZEDTHREAD_H

#include <pthread.h>

#include <cerrno>
#include <cstddef>
#include <exception>
#include <functional>
#include <memory>
#include <system_error>
#include <type_traits>
#include <utility>

// OpenHarmony context workers can inherit a small platform-default stack
// (about 132 KiB on API 23). Deep Compose measure/place chains need the same
// 8 MiB stack budget recommended for Kuikly's Android context thread.
inline constexpr std::size_t kKRContextWorkerStackSize = 8U * 1024U * 1024U;

// Narrow std::thread-compatible wrapper used by the Kuikly context worker.
// It intentionally keeps the construct/join/joinable/native_handle surface
// used by KRThread while owning the platform-specific stack-size setup.
class KRSizedThread {
 public:
    using native_handle_type = pthread_t;

    KRSizedThread() noexcept = default;

    template <typename Callable,
              typename = std::enable_if_t<!std::is_same_v<std::decay_t<Callable>, KRSizedThread>>>
    explicit KRSizedThread(Callable &&callable) {
        using TaskType = Task<std::decay_t<Callable>>;
        auto task = std::make_unique<TaskType>(std::forward<Callable>(callable));
        const int error = CreateWithStack(&thread_, &TaskType::Run, task.get());
        if (error != 0) {
            throw std::system_error(error, std::generic_category(), "KRSizedThread pthread_create");
        }
        task.release();
        joinable_ = true;
    }

    ~KRSizedThread() {
        if (joinable_) {
            std::terminate();
        }
    }

    KRSizedThread(const KRSizedThread &) = delete;
    KRSizedThread &operator=(const KRSizedThread &) = delete;

    KRSizedThread(KRSizedThread &&other) noexcept
        : thread_(other.thread_), joinable_(std::exchange(other.joinable_, false)) {
        other.thread_ = pthread_t{};
    }

    KRSizedThread &operator=(KRSizedThread &&other) noexcept {
        // Match std::thread move assignment: assigning to any joinable target,
        // including itself, terminates instead of silently losing ownership.
        if (joinable_) {
            std::terminate();
        }
        thread_ = other.thread_;
        joinable_ = std::exchange(other.joinable_, false);
        other.thread_ = pthread_t{};
        return *this;
    }

    bool joinable() const noexcept {
        return joinable_;
    }

    void join() {
        if (!joinable_) {
            throw std::system_error(EINVAL, std::generic_category(), "KRSizedThread::join");
        }
        if (pthread_equal(thread_, pthread_self()) != 0) {
            throw std::system_error(EDEADLK, std::generic_category(), "KRSizedThread::join self");
        }

        const int error = pthread_join(thread_, nullptr);
        if (error != 0) {
            // Match std::thread: a failed join must not discard ownership of a
            // thread whose termination and native resources are still unknown.
            throw std::system_error(error, std::generic_category(), "KRSizedThread pthread_join");
        }
        joinable_ = false;
        thread_ = pthread_t{};
    }

    native_handle_type native_handle() noexcept {
        return thread_;
    }

 private:
    template <typename Callable>
    struct Task {
        template <typename Source>
        explicit Task(Source &&source) : callable(std::forward<Source>(source)) {}

        static void *Run(void *rawTask) {
            std::unique_ptr<Task> task(static_cast<Task *>(rawTask));
            // Do not catch here. KRThread deliberately lets Kotlin/Native
            // unhandled hooks observe the exception before std::terminate,
            // matching the previous std::thread entry behavior.
            std::invoke(std::move(task->callable));
            return nullptr;
        }

        Callable callable;
    };

    static int CreateWithStack(pthread_t *thread, void *(*startRoutine)(void *), void *argument) noexcept {
        pthread_attr_t attributes;
        int error = pthread_attr_init(&attributes);
        if (error != 0) {
            return error;
        }

        error = pthread_attr_setstacksize(&attributes, kKRContextWorkerStackSize);
        if (error == 0) {
            error = pthread_create(thread, &attributes, startRoutine, argument);
        }
        // pthread_create copies the attributes, so destroy failure cannot
        // invalidate a successfully created thread.
        static_cast<void>(pthread_attr_destroy(&attributes));
        return error;
    }

    pthread_t thread_{};
    bool joinable_{false};
};

#endif  // CORE_RENDER_OHOS_KRSIZEDTHREAD_H
