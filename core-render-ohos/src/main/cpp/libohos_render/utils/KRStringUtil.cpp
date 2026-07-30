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

#include "KRStringUtil.h"

namespace kuikly {
namespace util {
std::vector<std::string_view> SplitStringView(const std::string_view &str, std::string separator) {
    size_t offset = 0;
    const size_t sz = str.size();
    std::vector<std::string_view> result;
    do {
        size_t pos = str.find(separator, offset);
        if (pos < sz) {
            result.push_back(str.substr(offset, pos - offset));
            offset = pos + separator.size();
        } else {
            break;
        }
    } while (offset < sz);
    if (offset < sz) {
        result.push_back(str.substr(offset));
    }
    return result;
}

std::vector<std::string> SplitString(const std::string &str, std::string separator) {
    size_t offset = 0;
    const size_t sz = str.size();
    std::vector<std::string> result;
    do {
        size_t pos = str.find(separator, offset);
        if (pos < sz) {
            result.push_back(str.substr(offset, pos - offset));
            offset = pos + separator.size();
        } else {
            break;
        }
    } while (offset < sz);
    if (offset < sz) {
        result.push_back(str.substr(offset));
    }
    return result;
}

bool isEqual2(const char *str1, const char *str2) {
    if ((str1 == NULL && str2) || (str1 && str2 == NULL) || (str1 == NULL && str2 == NULL)) {
        return false;
    }
    if (std::strcmp(str1, str2) == 0) {
        return true;
    } else {
        return false;
    }
}

bool isEqual(const std::string &str1, const char *str2) {
    return isEqual2(str1.c_str(), str2);
}

std::vector<KRAnyValue> SplitString(const std::string &str, char delimiter) {
    std::vector<KRAnyValue> tokens;
    std::string token;
    std::istringstream tokenStream(str);
    while (std::getline(tokenStream, token, delimiter)) {
        tokens.push_back(NewKRRenderValue(token));
    }
    return std::move(tokens);
}

}  // namespace util
}  // namespace kuikly
