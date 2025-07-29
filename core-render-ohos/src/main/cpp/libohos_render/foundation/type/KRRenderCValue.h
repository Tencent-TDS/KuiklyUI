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
#include <cstddef>
#include <cstdint>

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
        struct KRRenderCValue *arrayValue = NULL;
    } value;

    /**
     * 当类型为数组或者二进制时, 表示其长度
     */
    int32_t size;
} KRRenderCValue;

extern "C" {
typedef void (*CallKotlin)(int methodId, KRRenderCValue arg0, KRRenderCValue arg1, KRRenderCValue arg2,
                           KRRenderCValue arg3, KRRenderCValue arg4, KRRenderCValue arg5);
extern int com_tencent_kuikly_SetCallKotlin(CallKotlin callKotlin);
extern const KRRenderCValue com_tencent_kuikly_CallNative(int methodId, KRRenderCValue arg0, KRRenderCValue arg1,
                                                          KRRenderCValue arg2, KRRenderCValue arg3, KRRenderCValue arg4,
                                                          KRRenderCValue arg5);
}
#endif  // CORE_RENDER_OHOS_KRRENDERCVALUE_H
