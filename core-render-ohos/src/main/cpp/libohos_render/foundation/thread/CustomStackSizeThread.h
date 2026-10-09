/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_CUSTOMSTACKSIZETHREAD_H
#define CORE_RENDER_OHOS_CUSTOMSTACKSIZETHREAD_H

#include <pthread.h>
#include <cstddef>
#include <exception>
#include <functional>
#include <memory>
#include <string>
#include <system_error>
#include <type_traits>
#include <utility>

namespace kuikly {

// std::thread 的鸿蒙替代：创建时把栈设为 8MB。
// 只保留可调用对象构造。默认构造、复制、移动都不可用。
class CustomStackSizeThread {
 public:
    CustomStackSizeThread() = delete;
    CustomStackSizeThread(const CustomStackSizeThread &) = delete;
    CustomStackSizeThread &operator=(const CustomStackSizeThread &) = delete;
    CustomStackSizeThread(CustomStackSizeThread &&) = delete;
    CustomStackSizeThread &operator=(CustomStackSizeThread &&) = delete;

    template <class F, class = std::enable_if_t<!std::is_same_v<std::decay_t<F>, CustomStackSizeThread>>>
    explicit CustomStackSizeThread(F &&f, const std::string &name) {
        auto *fnPtr = new std::function<void()>(std::forward<F>(f));
        pthread_attr_t attr;
        const int initRet = pthread_attr_init(&attr);
        if (initRet != 0) {
            delete fnPtr;
            throw std::system_error(initRet, std::generic_category(), "kuikly::CustomStackSizeThread");
        }
        const int stackRet = pthread_attr_setstacksize(&attr, kStackSize);
        if (stackRet != 0) {
            pthread_attr_destroy(&attr);
            delete fnPtr;
            throw std::system_error(stackRet, std::generic_category(), "kuikly::CustomStackSizeThread");
        }
        const int createRet = pthread_create(&handle_, &attr, &CustomStackSizeThread::Entry, fnPtr);
        pthread_attr_destroy(&attr);
        if (createRet != 0) {
            delete fnPtr;
            throw std::system_error(createRet, std::generic_category(), "kuikly::CustomStackSizeThread");
        }
        joinable_ = true;
        pthread_setname_np(handle_, name.c_str());
    }

    ~CustomStackSizeThread() {
        if (joinable_) {
            std::terminate();
        }
    }

    bool Joinable() const noexcept {
        return joinable_;
    }

    void Join() {
        if (!joinable_) {
            throw std::system_error(std::make_error_code(std::errc::invalid_argument),
                                    "kuikly::CustomStackSizeThread::Join");
        }
        if (pthread_equal(handle_, pthread_self())) {
            throw std::system_error(std::make_error_code(std::errc::resource_deadlock_would_occur),
                                    "kuikly::CustomStackSizeThread::Join");
        }
        const int ret = pthread_join(handle_, nullptr);
        if (ret != 0) {
            throw std::system_error(ret, std::generic_category(), "kuikly::CustomStackSizeThread::Join");
        }
        joinable_ = false;
    }

 private:
    // HarmonyOS pthread 默认栈约 1MB。Kuikly context 在 worker 上会跑较深的
    // Kotlin/Native 调用链，默认栈会溢出，这里显式提到 8MB。
    static constexpr size_t kStackSize = 8u * 1024u * 1024u;

    static void *Entry(void *arg) {
        std::unique_ptr<std::function<void()>> fn(static_cast<std::function<void()> *>(arg));
        (*fn)();
        return nullptr;
    }

    pthread_t handle_{};
    bool joinable_{false};
};

}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_CUSTOMSTACKSIZETHREAD_H
