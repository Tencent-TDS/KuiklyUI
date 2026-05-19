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

#ifndef CORE_RENDER_OHOS_KRTEXTEDITORSWITCH_H
#define CORE_RENDER_OHOS_KRTEXTEDITORSWITCH_H

// 编译期门控宏：始终显式定义 KUIKLY_TEXT_EDITOR_AVAILABLE 为 0 或 1。
//   * 1 = SDK header >= API 24，TEXT_EDITOR 配套类型 / API 全部可用；
//   * 0 = SDK header <  API 24，TEXT_EDITOR 配套类型尚未声明，需要编译期裁掉。
// 该宏被以下位置依赖：
//   * KRTextEditorCommon.h / KRTextEditorFieldView.h/.cpp / KRTextEditorAreaView.h/.cpp
//     用 `#if KUIKLY_TEXT_EDITOR_AVAILABLE` 整体保留 / 裁剪带 API 24 类型的实现；
//   * ComponentsRegisterEntry.h 用 `#if KUIKLY_TEXT_EDITOR_AVAILABLE` 决定 include
//     与注册分支。
// 务必在本头中定义（而不是放在 KRTextEditorCommon.h 内），理由是本头不依赖任何
// API 24 类型，能被最早、最独立地 include；避免 include 顺序带来的宏未定义错位。
//
// 之所以采用"始终显式 0/1"而非"条件 #define / 不定义"，是为了：
//   * 在 `#if MACRO` 处即便忘了 include 本头，也会因宏未定义而被预处理为 0，
//     引发明显的编译期或链接期错误而非沉默错位；
//   * 阅读端永远看到一个明确的真值表达式，不必反复回忆"宏存在与否"语义；
//   * 运行时探测函数（KRIsTextEditorRuntimeAvailable）与本宏共用同一字面"1=可用"
//     约定，跨编译期 / 运行期心智模型统一。
#include <arkui/native_type.h>

#ifndef KUIKLY_TEXT_EDITOR_AVAILABLE
#if defined(OH_CURRENT_API_VERSION) && OH_CURRENT_API_VERSION >= 24
#define KUIKLY_TEXT_EDITOR_AVAILABLE 1
#else
#define KUIKLY_TEXT_EDITOR_AVAILABLE 0
#endif
#endif

/**
 * 新输入控件（基于 ARKUI_NODE_TEXT_EDITOR / API 24）的全局开关。
 *
 * 语义（β 方案）：
 *   * 开关只影响 KRTextAreaView：
 *     - 0（默认）= KRTextAreaView 走老实现；
 *     - 1 = 在 API >= 24 时 KRTextAreaView 走新实现 KRTextEditorAreaView，
 *           否则仍回退到老实现。
 *   * KRTextFieldView 永远走老实现（与开关无关），保证单行输入行为零变化。
 *
 * 之所以将开关声明独立为本头：
 *   * KRTextEditorFieldView.h / KRTextEditorAreaView.h / KRTextEditorCommon.h
 *     在编译期 SDK header < API 24 时整体被 `#if KUIKLY_TEXT_EDITOR_AVAILABLE`
 *     guard 掉；
 *   * ComponentsRegisterEntry.h / 宿主 napi 入口 / 等位置仍需要稳定地访问
 *     这两个开关 C API；
 *   * 因此把"开关 API 声明"和"使用 API 24 类型的 view 声明"解耦：本头不依赖
 *     任何 API 24 类型，永远可用。
 *
 * 跨 so 注意：开关值的实际存储位于 libkuikly.so（core-render-ohos 产物）内的
 * 静态变量。宿主（如 ohosApp 的 libkuikly_entry.so）只能通过本 C API 访问，
 * 不要把开关变量做成 inline / static 头变量——会导致每个 so 各有一份副本。
 */

#ifdef __cplusplus
extern "C" {
#endif

__attribute__((visibility("default"))) void KRSetUseNewTextInputComponent(int value);
__attribute__((visibility("default"))) int KRGetUseNewTextInputComponent();

/**
 * 运行时 API 探测：返回 1 表示当前进程加载 libkuikly.so 后，ARKUI_NODE_TEXT_EDITOR
 * 配套的 API 24 关键符号已被动态链接器解析成功；返回 0 表示运行系统 < API 24，
 * 弱符号未解析（符号地址为 nullptr），新输入控件不可用。
 *
 * 与编译期门控 KUIKLY_TEXT_EDITOR_AVAILABLE 的区别：
 *   * KUIKLY_TEXT_EDITOR_AVAILABLE：始终显式定义为 0/1（见本头顶部）。
 *     1=SDK header >= API 24，决定本轮编译是否把 KRTextEditor*View 整套实现编进 .so；
 *   * KRIsTextEditorRuntimeAvailable()：SDK header >= API 24 编译出 .so 后，运行
 *     在低系统（如 API 20）上时，所有 API 24 符号被弱声明 + 运行时取 nullptr，
 *     该函数据此判定是否走新控件路径。
 *
 * 注册分支必须用本函数而非 OH_GetSdkApiVersion()——后者只能反映系统宣称的 API
 * 版本，但部分鸿蒙发行版 / 模拟器可能 API 数值已 ≥24 却尚未实现 TEXT_EDITOR 子集，
 * 直接调用仍会失败；本函数以"符号是否真的存在"为准，最贴近运行时事实。
 */
__attribute__((visibility("default"))) int KRIsTextEditorRuntimeAvailable();

#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KRTEXTEDITORSWITCH_H
