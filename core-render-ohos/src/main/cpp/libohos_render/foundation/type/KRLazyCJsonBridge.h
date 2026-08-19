/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRLAZYCJSONBRIDGE_H
#define CORE_RENDER_OHOS_KRLAZYCJSONBRIDGE_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Shared ownership of a cJSON tree between C++ KRRenderValue and Kotlin.
 *
 * Internally each opaque handle is a heap-allocated std::shared_ptr<KuiklyCJsonOwner>.
 * - create → one shared_ptr handle (held by KRRenderValue)
 * - retain → returns a new handle sharing the same owner (Kotlin / child wrappers)
 * - release → destroys that handle's shared_ptr; tree deleted when last handle is gone
 *
 * NATIVE_JSON longValue is a create/retain handle.
 */

int64_t kuikly_cjson_owner_create(void *cjson /* cJSON* */);
/** Returns a new handle that shares ownership with [owner]. 0 if invalid. */
int64_t kuikly_cjson_retain(int64_t owner);
void kuikly_cjson_release(int64_t owner);
/** Root cJSON* as int64 (0 if owner invalid). */
int64_t kuikly_cjson_owner_root(int64_t owner);

/** Field accessors: ptr is a cJSON* node (root or child). */
int kuikly_cjson_has(int64_t ptr, const char *key);
int kuikly_cjson_size(int64_t ptr);
const char *kuikly_cjson_key_at(int64_t ptr, int index);
int kuikly_cjson_value_kind(int64_t ptr, const char *key);
int kuikly_cjson_get_bool(int64_t ptr, const char *key, int fallback);
double kuikly_cjson_get_number(int64_t ptr, const char *key, double fallback);
const char *kuikly_cjson_get_string(int64_t ptr, const char *key);
int64_t kuikly_cjson_get_object(int64_t ptr, const char *key);
int64_t kuikly_cjson_get_array(int64_t ptr, const char *key);
char *kuikly_cjson_print(int64_t ptr);
void kuikly_cjson_free_string(char *s);

#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KRLAZYCJSONBRIDGE_H
