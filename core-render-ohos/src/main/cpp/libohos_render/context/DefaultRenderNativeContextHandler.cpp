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
#include <exception>
#include "DefaultRenderNativeContextHandler.h"
#include "libohos_render/foundation/thread/KRThreadFatalGuard.h"
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
    // Diagnostics-only wrapper around the Kotlin/Native call boundary.
    //
    // 语义：与 KRThreadFatalGuard 一致的 fail-forward
    //   * 在 catch 里补一条"哪个 method_id 抛的"诊断（这条信息在外层 fatal guard
    //     里拿不到，故必须就近记录）；
    //   * 立即 `throw;` 让原始异常继续 unwind：
    //       - 保留 K/N runtime 的 unhandled-exception hook 触发窗口（hook 挂在
    //         std::terminate 路径上，会先于最终 abort 打出完整 Kotlin 栈）；
    //       - 上层 KRThread::DirectRunOnCurThread.{nested,borrow} 的
    //         RunWithFatalGuard 会再打一层 tag + demangled 类型 + e.what() 后 rethrow，
    //         最终 std::terminate → abort。
    //   * 类型名 demangle 委托给 kuikly::thread::CurrentExceptionTypeName，
    //     全仓单实现，避免遗漏 K/N 非 std::exception 派生类型。
    const int method_id = static_cast<int>(method);
    try {
        callKotlin_(method_id, arg0->toCValue(), arg1->toCValue(), arg2->toCValue(), arg3->toCValue(),
                    arg4->toCValue(), arg5->toCValue());
    } catch (const std::exception &e) {
        KR_LOG_ERROR_WITH_TAG("KRRender")
            << "[callKotlin_] std::exception at K/N boundary; method=" << method_id
            << " type=" << kuikly::thread::CurrentExceptionTypeName() << " what=" << e.what();
        throw;
    } catch (...) {
        KR_LOG_ERROR_WITH_TAG("KRRender")
            << "[callKotlin_] non-std exception at K/N boundary; method=" << method_id
            << " type=" << kuikly::thread::CurrentExceptionTypeName();
        throw;
    }
}
