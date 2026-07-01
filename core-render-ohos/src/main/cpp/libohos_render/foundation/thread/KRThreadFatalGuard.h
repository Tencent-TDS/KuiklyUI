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

#ifndef CORE_RENDER_OHOS_KRTHREADFATALGUARD_H
#define CORE_RENDER_OHOS_KRTHREADFATALGUARD_H

#include <cstdlib>
#include <cxxabi.h>
#include <exception>
#include <string>
#include <utility>

#include "libohos_render/utils/KRRenderLoger.h"

namespace kuikly {
namespace thread {

// 拿到当前 catch 分支正在处理的异常的可读类型名。
//   * 使用 abi::__cxa_current_exception_type() 获取 std::type_info；
//     该 API 只在 catch 块中调用才有意义，其他上下文会返回 nullptr。
//   * 结果用 abi::__cxa_demangle 反修饰，方便识别形如
//     "kotlin::ObjHolder"、"IncorrectDereferenceException" 之类
//     不继承 std::exception 的 K/N 异常类型。
//   * 无法获取时返回 "<unknown>"，避免污染日志格式。
inline std::string CurrentExceptionTypeName() {
    const std::type_info *ti = abi::__cxa_current_exception_type();
    if (ti == nullptr) {
        return "<unknown>";
    }
    const char *mangled = ti->name();
    if (mangled == nullptr) {
        return "<unknown>";
    }
    int status = 0;
    char *demangled = abi::__cxa_demangle(mangled, nullptr, nullptr, &status);
    if (status == 0 && demangled != nullptr) {
        std::string result(demangled);
        std::free(demangled);
        return result;
    }
    // demangle 失败则退回 mangled name，好过没有信息。
    return std::string(mangled);
}

// 统一的"调度边界 fail-fast"语义：
//   * 所有跨线程/跨语言（C++ ↔ ArkTS / libuv 回调 / std::thread 入口）的"task 执行"
//     调度边界都应该用这个 guard 包裹。
//   * 设计动机：libuv 回调里抛 C++ 异常会越过 C 帧 → UB；std::thread 入口异常逃出
//     直接 std::terminate()，coredump 通常被 unwind 干净；napi C ABI 入口同理。
//     这些位置任何"放任异常逃出"的写法都会让现场不可读。
//   * 行为：catch 全部 → 打 KR_LOG_ERROR（含调用点 tag 与 e.what()）→ std::abort()。
//   * 适用点（截至本提交）：
//       - KRThread: OnAsync.batch / TimerCb.fallback / DirectRunOnCurThread.{nested,borrow}
//       - KRMainThread: MainAsync.batch / MainTimer.cb / Inline.same-thread / Inline.fallback
//       - KRRenderCore: ABI.CallNative （napi 边界）
//   * 如未来要做"可恢复"语义，需自上而下重新评估，不建议在本函数里加分支。
template <typename F>
inline void RunWithFatalGuard(const char *tag, F &&task) {
    try {
        std::forward<F>(task)();
    } catch (const std::exception &e) {
        KR_LOG_ERROR << "[" << tag << "] uncaught std::exception in task"
                     << " (type=" << CurrentExceptionTypeName() << ")"
                     << ": " << e.what()
                     << "; aborting to preserve crash context.";
        std::abort();
    } catch (...) {
        KR_LOG_ERROR << "[" << tag << "] uncaught non-std exception in task"
                     << " (type=" << CurrentExceptionTypeName() << ")"
                     << "; aborting to preserve crash context.";
        std::abort();
    }
}

}  // namespace thread
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRTHREADFATALGUARD_H
