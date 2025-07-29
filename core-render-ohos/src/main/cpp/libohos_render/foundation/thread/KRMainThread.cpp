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

#include "KRMainThread.h"

#include <hilog/log.h>
#include "KRDelayThread.h"


napi_threadsafe_function g_threadsafe_func_handle = NULL;
// 延时Api
static void DispatchAsync(std::function<void()> task, int delayMilliseconds = 0) {
    static KRDelayThread *gDelayThread = new KRDelayThread("kuiklyasync");
    gDelayThread->DispatchAsync(task, delayMilliseconds);
}
class MainThreadTask {
 public:
    explicit MainThreadTask(std::function<void()> func) : func(std::move(func)) {}

    void execute() {
        func();
    }

 private:
    std::function<void()> func;
};

static void gcd_threadsafe_func(napi_env env, napi_value js_fun, void *context, void *data) {
    MainThreadTask *task = static_cast<MainThreadTask *>(data);
    if (task != nullptr) {
        task->execute();
        delete task;
    }
}

KRMainThread::KRMainThread() {
    // 在这里添加构造函数代码
}

void KRMainThread::Export(napi_env env, napi_value exports) {
    if ((nullptr == env) || (nullptr == exports)) {
        return;
    }
    if (g_threadsafe_func_handle == NULL) {  // 注册安全函数
        napi_value work_name;
        napi_create_string_utf8(env, "Node-API Thread-safe Call from Async Work Item", NAPI_AUTO_LENGTH, &work_name);
        napi_status status = napi_create_threadsafe_function(env, NULL, NULL, work_name, 0, 1, NULL, NULL, NULL,
                                                             gcd_threadsafe_func, &g_threadsafe_func_handle);
        if (status != napi_ok) {
            napi_throw_error(env, "-1", "napi_create_threadsafe_function error");
            return;
        }
    }
}

void KRMainThread::RunOnMainThread(const std::function<void()> &task, int delayMilliseconds) {
    if (delayMilliseconds > 0) {
        DispatchAsync([task] { RunOnMainThread(task); }, delayMilliseconds);
    } else {
        if (g_threadsafe_func_handle) {
            MainThreadTask *mainTask = new MainThreadTask(task);
            napi_call_threadsafe_function(g_threadsafe_func_handle, static_cast<void *>(mainTask), napi_tsfn_blocking);
        } else {
            OH_LOG_Print(LOG_APP, LOG_ERROR, 0x7, "KRMainThread", "function handle is null");
        }
    }
}

static std::vector<std::function<void()>> &NextRunLoopTasks(bool isClear, const std::function<void()> &task) {
    static std::vector<std::function<void()>> gTasks;
    if (task) {
        gTasks.push_back(task);
    }
    if (isClear) {
        gTasks.clear();
    }
    return gTasks;
}

static bool NeedNextRunLoop(bool isGet, bool isSet, bool setValue) {
    static bool gSetNeedNextRunloop = false;
    if (isGet) {
        return gSetNeedNextRunloop;
    }
    if (isSet) {
        gSetNeedNextRunloop = setValue;
    }
    return gSetNeedNextRunloop;
}

void KRMainThread::RunOnMainThreadForNextLoop(const std::function<void()> &task) {
    NextRunLoopTasks(false, task);
    if (!NeedNextRunLoop(true, false, false)) {
        NeedNextRunLoop(false, true, true);
        DispatchAsync([] {
            RunOnMainThread([] {
                NeedNextRunLoop(false, true, false);
                auto tasks = NextRunLoopTasks(false, nullptr);
                int size = tasks.size();
                for (auto &task : tasks) {
                    task();
                }
                NextRunLoopTasks(true, nullptr);
            });
        });
    }
}
