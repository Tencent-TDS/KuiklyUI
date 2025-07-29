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

#include "KRThreadChecker.h"

#include <unistd.h>
#include "libohos_render/scheduler/KRContextScheduler.h"

static __attribute__((always_inline)) bool isMainThread() {
    return getpid() == gettid();
}

void KREnsureMainThreadChecker(const char *file, unsigned int line, const char *function) {
    if (!isMainThread()) {
        // threading state is abnormal, KR_LOG_ERROR is not guaranteed to work correctly,
        // so we first write a log with OH_LOG_Print and then KR_LOG_ERROR.
        OH_LOG_Print(LOG_APP, LOG_ERROR, 0x7, "ThreadChecker",
                     "Main Thread Check Failed. %{public}s %{public}d %{public}s", file, line, function);
        KR_LOG_ERROR << "Main Thread Check Failed. " << file << ":" << line << ":" << function;

        __assert_fail("Main Thread Check Failed.", file, line, function);
    }
}
