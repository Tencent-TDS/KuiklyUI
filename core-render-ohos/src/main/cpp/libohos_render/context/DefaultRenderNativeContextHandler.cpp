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

#include <assert.h>
#include "DefaultRenderNativeContextHandler.h"
#include "libohos_render/utils/KRRenderLoger.h"

extern CallKotlin callKotlin_;

void DefaultRenderNativeContextHandler::CallKotlinMethod(const KuiklyRenderContextMethod &method,
                                                         const std::shared_ptr<KRRenderValue> &arg0,
                                                         const std::shared_ptr<KRRenderValue> &arg1,
                                                         const std::shared_ptr<KRRenderValue> &arg2,
                                                         const std::shared_ptr<KRRenderValue> &arg3,
                                                         const std::shared_ptr<KRRenderValue> &arg4,
                                                         const std::shared_ptr<KRRenderValue> &arg5) {
    if (callKotlin_ == nullptr) {
        __assert_fail("Tips: make sure initKuikly() has been called!", __FILE__, __LINE__, __func__);
    }
    // K/N 调用边界：不套任何 C++ catch，让异常原样冒到 K/N runtime。
    // 曾经在此处 catch → 补一条 "哪个 method_id 抛的" 诊断日志 → rethrow，
    // 但实测 K/N 会因为观察到 "C++ 已 catch 过" 而不再触发 unhandled-exception hook，
    // 导致丢失 Kotlin 侧真正有价值的 Throwable class / message / Kotlin 栈。
    // 为保留 hook 触发窗口，放弃 C++ 侧的补充诊断日志（method_id 可在 Kotlin 栈中反查）。
    callKotlin_(static_cast<int>(method), arg0->toCValue(), arg1->toCValue(), arg2->toCValue(), arg3->toCValue(),
                arg4->toCValue(), arg5->toCValue());
}
