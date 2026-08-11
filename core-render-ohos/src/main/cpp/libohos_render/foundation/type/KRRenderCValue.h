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

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

/**
 * 与kotlin侧通信的数据类型
 */
typedef struct KRRenderCValue {
    // 定义一个枚举类型来表示值的类型
    enum Type { NULL_VALUE, INT, LONG, FLOAT, DOUBLE, BOOL, STRING, BYTES, ARRAY } type;

    // 定义一个联合体来存储不同类型的值
    union Value {
        int32_t intValue;
        int64_t longValue;
        float floatValue;
        double doubleValue;
        int boolValue;
        char *stringValue;
        char *bytesValue;
        struct KRRenderCValue *arrayValue;
    } value;

    /**
     * 当类型为数组或者二进制时, 表示其长度
     */
    int32_t size;

#ifdef __cplusplus
    KRRenderCValue() : type(NULL_VALUE), value{}, size(0) {}
#endif
} KRRenderCValue;

#ifdef __cplusplus
extern "C" {
#endif
typedef void (*CallKotlin)(int methodId, KRRenderCValue arg0, KRRenderCValue arg1, KRRenderCValue arg2,
                           KRRenderCValue arg3, KRRenderCValue arg4, KRRenderCValue arg5);
// 这几个符号由 Kotlin/Native 的 libshared.so 跨 so 解析，在 -fvisibility=hidden 下
// 必须显式导出，否则加载期符号解析失败直接 SIGSEGV。此处不 include KuiklyExport.h：
// 本头不在对外分发的 api/include 目录内，保持自包含以免依赖源码树目录层级。
__attribute__((visibility("default"))) extern int com_tencent_kuikly_SetCallKotlin(CallKotlin callKotlin);
__attribute__((visibility("default"))) extern void com_tencent_kuikly_CallNative(int methodId, const KRRenderCValue *arg0, const KRRenderCValue *arg1,
                                                          const KRRenderCValue *arg2, const KRRenderCValue *arg3, const KRRenderCValue *arg4,
                                                          const KRRenderCValue *arg5, KRRenderCValue *result);
__attribute__((visibility("default"))) extern void com_tencent_kuikly_ScheduleContextTask(const char *pagerId,
                                                          void (*onSchedule)(const char *pagerId));
__attribute__((visibility("default"))) extern bool com_tencent_kuikly_IsCurrentOnContextThread(const char *pagerId);

#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KRRENDERCVALUE_H
