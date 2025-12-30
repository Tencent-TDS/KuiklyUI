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

#include "KRFfrt.h"

#define FFRT_STATIC_ASSERT(cond, msg) int x(int static_assertion_##msg[(cond) ? 1 : -1])

void ffrt_exec_function_wrapper(void* t)
{
    ffrt_function_wrapper_t* f = (ffrt_function_wrapper_t*)t;
    if (f->func) {
        f->func(f->arg);
    }
}

void ffrt_destroy_function_wrapper(void* t)
{
    ffrt_function_wrapper_t* f = (ffrt_function_wrapper_t*)t;
    if (f->after_func) {
        f->after_func(f->arg);
    }
}

ffrt_function_header_t* ffrt_create_function_wrapper(
    ffrt_function_t func,
    ffrt_function_t after_func, 
    void* arg, 
    ffrt_function_kind_t kind)
{
    FFRT_STATIC_ASSERT(sizeof(ffrt_function_wrapper_t) <= ffrt_auto_managed_function_storage_size,
        size_of_function_must_be_less_than_ffrt_auto_managed_function_storage_size);

    ffrt_function_wrapper_t* f = (ffrt_function_wrapper_t*)ffrt_alloc_auto_managed_function_storage_base(kind);
    f->header.exec = ffrt_exec_function_wrapper;
    f->header.destroy = ffrt_destroy_function_wrapper;
    f->func = func;
    f->after_func = after_func;
    f->arg = arg;
    return (ffrt_function_header_t*)f;
}