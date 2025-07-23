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
#ifndef CORE_RENDER_OHOS_KRSTRINGUTIL_H
#define CORE_RENDER_OHOS_KRSTRINGUTIL_H
#include <string>
#include <vector>
#include "libohos_render/foundation/KRCommon.h"

namespace kuikly {
namespace util {
std::vector<std::string_view> SplitStringView(const std::string_view &str, std::string separator);
std::vector<std::string> SplitString(const std::string &str, std::string separator);

bool isEqual2(const char *str1, const char *str2);

bool isEqual(const std::string &str1, const char *str2);

std::vector<KRAnyValue> SplitString(const std::string &str, char delimiter);

}  // namespace util
}  // namespace kuikly
#endif  // CORE_RENDER_OHOS_KRSTRINGUTIL_H
