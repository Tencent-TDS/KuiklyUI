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

#include "libohos_render/expand/components/input/KRTextEditorSwitch.h"

// 本 TU 不依赖任何 API 24 类型，因此即便编译期 SDK header < API 24
// 也始终参与编译。开关值在此一份独立存储，跨 so 调用通过本文件提供的
// C API 同步，避免 inline 变量分裂副本。
namespace {
int g_kr_use_new_text_input_component = 0;
}  // namespace

extern "C" void KRSetUseNewTextInputComponent(int value) {
    g_kr_use_new_text_input_component = value;
}

extern "C" int KRGetUseNewTextInputComponent() {
    return g_kr_use_new_text_input_component;
}

// ============================================================================
// 运行时 API 探测（弱符号 nullptr 检测）
//
// 实现策略：
//   * 仅在 SDK header >= API 24 时才真正做地址比较；此时 <arkui/native_type.h>
//     与 <arkui/styled_string.h> 已被上方头链 include，三个目标函数有真定义。
//     紧跟 SDK 强声明再追加一份带 __attribute__((weak)) 的同名重声明，clang/gcc
//     在多次声明属性合并时 weak 是 sticky 的，最终生成 ELF 时该符号引用变弱。
//   * SDK header < API 24（即 KUIKLY_TEXT_EDITOR_AVAILABLE == 0）时，整套新控件
//     实现已被编译期 guard 裁掉，再做运行时探测毫无意义——直接 return 0 让注册
//     分支永远走老控件，逻辑上等价于"运行时 API 不可用"。
//   * 探测仅做地址比较，不实际调用，避免对返回类型造成约束。
// ============================================================================

#if KUIKLY_TEXT_EDITOR_AVAILABLE
// 必须 include 一次完整的类型定义，让 SDK 强声明成为可见声明，再做 weak 重声明。
#include <arkui/styled_string.h>
// native_type.h 已在 KRTextEditorSwitch.h 中 include。

extern "C" {
// 同名重声明 + weak。属性合并后该符号在本 TU 引用为弱符号。clang 不会因为重复
// 声明而报错（声明一致），且 weak 属性会在 link 阶段 propagate。
OH_ArkUI_TextEditorStyledStringController *OH_ArkUI_TextEditorStyledStringController_Create()
    __attribute__((weak));
ArkUI_StyledString_Descriptor *OH_ArkUI_StyledString_Descriptor_Create(void)
    __attribute__((weak));
OH_ArkUI_TextEditorTextStyle *OH_ArkUI_TextEditorTextStyle_Create() __attribute__((weak));
}  // extern "C"
#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE

extern "C" int KRIsTextEditorRuntimeAvailable() {
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    // weak 符号未解析时地址为 nullptr。三个关键 API 24 入口任一缺失都视为"不可用"。
    if (&OH_ArkUI_TextEditorStyledStringController_Create == nullptr) {
        return 0;
    }
    if (&OH_ArkUI_StyledString_Descriptor_Create == nullptr) {
        return 0;
    }
    if (&OH_ArkUI_TextEditorTextStyle_Create == nullptr) {
        return 0;
    }
    return 1;
#else
    // 编译期已确认 SDK header < API 24，新控件全部 guard 掉；运行时无需再探测。
    return 0;
#endif
}
