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

// 统一的"调度边界 fail-forward"语义（catch → 日志 → rethrow）：
//   * 所有跨线程/跨语言（C++ ↔ ArkTS / libuv 回调 / std::thread 入口）的"task 执行"
//     调度边界都应该用这个 guard 包裹。
//   * 设计动机：
//       - libuv 回调 / std::thread 入口 / napi C ABI 里放任 C++ 异常自然逃出会 UB
//         或 std::terminate 无 unwind，crash 现场不可读；
//       - 但**直接 abort()** 会抢在 K/N runtime 的 unhandled-exception hook 之前，
//         吞掉 Kotlin 侧真正有价值的 Throwable class / message / Kotlin 栈；
//       - 折中方案：先在 catch 里打完整诊断日志（tag + demangled 类型名 + e.what()），
//         再 `throw;` 让异常继续 unwind。unwind 一路到 `std::terminate()`
//         等价于 `std::abort()`，但 K/N runtime 挂在那条路径上的 unhandled hook
//         有机会先跑并打出 Kotlin 栈；同时 RAII 会正常展开，避免 mutex/标志位残留。
//   * 行为：
//       1. `try { task(); }` 正常路径直通；
//       2. `catch (std::exception&)` / `catch (...)`：打 KR_LOG_ERROR
//          （含 tag、demangled type、e.what()），然后 `throw;` 继续 unwind；
//       3. 异常最终由 K/N unhandled hook 或 `std::terminate`（→ `abort`）终止进程。
//   * 语义要点：
//       - 保留 fail-fast 精神（进程一定终止），但把"终止方式"从 abort 改为 rethrow，
//         把 abort 决策权让渡给运行时（K/N hook / std::terminate handler）；
//       - `throw;` 沿用原始异常对象，不产生新异常，`std::current_exception()`
//         语义保持不变；
//       - 上层 caller 需要预期本函数**可能向外抛异常**，若上层想"吸收异常继续运行"
//         必须自行套 catch —— 但当前工程约定就是 fail-fast，不建议这么做。
//   * 适用点（截至本提交）：
//       - KRThread: OnAsync.batch / TimerCb.fallback / DirectRunOnCurThread.{nested,borrow}
//       - KRMainThread: MainAsync.batch / MainTimer.cb / Inline.same-thread / Inline.fallback
//       - KRRenderCore: ABI.CallNative （napi C ABI 边界，异常在这里会被最外层
//         由 catch 打 log + rethrow → std::terminate；由于这里已经是 napi ABI 边界，
//         rethrow 后异常会到达 std::terminate，与 abort 等价，但保留了 K/N hook 触发窗口）
template <typename F>
inline void RunWithFatalGuard(const char *tag, F &&task) {
    try {
        std::forward<F>(task)();
    } catch (const std::exception &e) {
        KR_LOG_ERROR << "[" << tag << "] std::exception at dispatch boundary"
                     << " (type=" << CurrentExceptionTypeName() << ")"
                     << ": " << e.what()
                     << "; rethrowing to let K/N unhandled-exception hook run.";
        throw;
    } catch (...) {
        KR_LOG_ERROR << "[" << tag << "] non-std exception at dispatch boundary"
                     << " (type=" << CurrentExceptionTypeName() << ")"
                     << "; rethrowing to let K/N unhandled-exception hook run.";
        throw;
    }
}

}  // namespace thread
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRTHREADFATALGUARD_H
