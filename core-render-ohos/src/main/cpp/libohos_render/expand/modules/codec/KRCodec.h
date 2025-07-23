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
#pragma once

#include <string>

namespace kuikly {
inline namespace model_util {
std::string KREncodeURLComponent(const std::string &str);

std::string KRDecodeURLComponent(const std::string &str);

std::string KRBase64Encode(const std::string &str);
std::string KRBase64Encode(const std::string_view in);

std::string KRBase64Decode(const std::string &str);

std::string KRMd5(const std::string &str);

std::string KRSha256(const std::string &str);
}  //  namespace util
}  //  namespace kuikly
