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

#ifndef CORE_RENDER_OHOS_KRFFRT_H
#define CORE_RENDER_OHOS_KRFFRT_H

#include "ffrt/task.h"
#include "ffrt/mutex.h"
#include "ffrt/shared_mutex.h"
#include "ffrt/condition_variable.h"
#include "ffrt/sleep.h"
#include "ffrt/queue.h"
#include "ffrt/loop.h"
#include "ffrt/timer.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief FFRT 函数包装器结构体
 */
typedef struct {
    ffrt_function_header_t header;
    ffrt_function_t func;
    ffrt_function_t after_func;
    void* arg;
} ffrt_function_wrapper_t;

/**
 * @brief 创建 FFRT 函数包装器
 * 
 * @param func 要执行的函数
 * @param after_func 清理函数（可以为 NULL）
 * @param arg 传递给函数的参数
 * @param kind 函数类型
 * @return ffrt_function_header_t* 函数包装器指针
 */
ffrt_function_header_t* ffrt_create_function_wrapper(
    ffrt_function_t func,
    ffrt_function_t after_func, 
    void* arg, 
    ffrt_function_kind_t kind
);

/**
 * @brief 执行函数包装器（内部使用）
 * 
 * @param t 函数包装器指针
 */
void ffrt_exec_function_wrapper(void* t);

/**
 * @brief 销毁函数包装器（内部使用）
 * 
 * @param t 函数包装器指针
 */
void ffrt_destroy_function_wrapper(void* t);

#ifdef __cplusplus
}
#endif

#endif // CORE_RENDER_OHOS_KRFFRT_H
