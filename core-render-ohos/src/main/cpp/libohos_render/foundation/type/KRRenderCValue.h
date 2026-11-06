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

#ifndef CORE_RENDER_OHOS_KRRENDERCVALUE_H
#define CORE_RENDER_OHOS_KRRENDERCVALUE_H

#include <stdbool.h>
#include "../../api/include/Kuikly/KRJSON.h"

/**
 * Compatibility name for the unified Kotlin/render bridge value.
 *
 * Keep the typedef temporarily so downstream source code can migrate without
 * carrying two representations. The ABI is exactly one KRJSON word.
 */
typedef KRJSON KRRenderCValue;

#ifdef __cplusplus
extern "C" {
#endif
typedef void (*CallKotlin)(int methodId, KRRenderCValue arg0, KRRenderCValue arg1, KRRenderCValue arg2,
                           KRRenderCValue arg3, KRRenderCValue arg4, KRRenderCValue arg5);
// 这几个符号由 Kotlin/Native 的 libshared.so 跨 so 解析，在 -fvisibility=hidden 下
// 必须显式导出，否则加载期符号解析失败直接 SIGSEGV。此处不 include KuiklyExport.h：
// 本头不在对外分发的 api/include 目录内，保持自包含以免依赖源码树目录层级。
__attribute__((visibility("default"))) extern int com_tencent_kuikly_SetCallKotlin(CallKotlin callKotlin);
__attribute__((visibility("default"))) extern KRRenderCValue com_tencent_kuikly_CallNative(
    int methodId, KRRenderCValue arg0, KRRenderCValue arg1, KRRenderCValue arg2,
    KRRenderCValue arg3, KRRenderCValue arg4, KRRenderCValue arg5);
__attribute__((visibility("default"))) extern void com_tencent_kuikly_ScheduleContextTask(const char *pagerId,
                                                          void (*onSchedule)(const char *pagerId));
__attribute__((visibility("default"))) extern bool com_tencent_kuikly_IsCurrentOnContextThread(const char *pagerId);

#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KRRENDERCVALUE_H
