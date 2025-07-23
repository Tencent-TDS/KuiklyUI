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

#ifndef CORE_RENDER_OHOS_NAPIUTIL_H
#define CORE_RENDER_OHOS_NAPIUTIL_H

#include <cstdint>
#include <string>
#include "napi/native_api.h"

namespace kuikly {
namespace util {

double getNApiArgsDouble(napi_env env, napi_value value);

int32_t getNApiArgsInt(napi_env env, napi_value value);

int64_t getNApiArgsInt64(napi_env env, napi_value value);

char *getNApiArgsString(napi_env env, napi_value value);

std::string getNApiArgsStdString(napi_env env, napi_value value);

void GetNApiArgsStdString(const napi_env &env, const napi_value &value, std::string &result);

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_NAPIUTIL_H
