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

#ifndef CORE_RENDER_OHOS_KRMAINTHREAD_H
#define CORE_RENDER_OHOS_KRMAINTHREAD_H

#include <napi/native_api.h>
#include <functional>

class KRMainThread {
 public:
    /**
     * @brief TS环境导出绑定
     * @param env ts环境
     * @param exports 透传参数
     */
    static void Export(napi_env env, napi_value exports);

    /**
     * @brief 在主线程上执行任务，可以选择延迟执行
     * @param task 需要在主线程上执行的任务
     * @param delayMilliseconds 延迟执行任务的毫秒数，默认为0，表示立即执行
     */
    static void RunOnMainThread(const std::function<void()> &task, int delayMilliseconds = 0);

    /**
     * @brief 在主线程的下一个事件循环中执行任务
     * @param task 需要在主线程上执行的任务
     */
    static void RunOnMainThreadForNextLoop(const std::function<void()> &task);

 private:
    KRMainThread();
};

#endif  // CORE_RENDER_OHOS_KRMAINTHREAD_H
