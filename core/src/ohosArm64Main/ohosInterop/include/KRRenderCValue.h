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

#ifndef MYAPPLICATION_KRRENDERCVALUE_H
#define MYAPPLICATION_KRRENDERCVALUE_H
#include <bits/alltypes.h>
#include <stdint.h>

typedef struct KRRenderCValue {
    // 定义一个枚举类型来表示值的类型
    enum Type { NULL, INT, LONG, FLOAT, DOUBLE, BOOL, STRING, BYTES, ARRAY } type;

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

    int32_t size;
    
} KRRenderCValue;

//extern "C" { // kotlin interop tool does not recognize extern "C" syntax, commenting it out.
typedef void (*CallKotlin)(int methodId, KRRenderCValue arg0, KRRenderCValue arg1, KRRenderCValue arg2, KRRenderCValue arg3, KRRenderCValue arg4, KRRenderCValue arg5);
extern int com_tencent_kuikly_SetCallKotlin(CallKotlin callKotlin);
extern const KRRenderCValue com_tencent_kuikly_CallNative(int methodId, KRRenderCValue arg0, KRRenderCValue arg1, KRRenderCValue arg2,
                                                          KRRenderCValue arg3, KRRenderCValue arg4, KRRenderCValue arg5);
//}
#endif //MYAPPLICATION_KRRENDERCVALUE_H
