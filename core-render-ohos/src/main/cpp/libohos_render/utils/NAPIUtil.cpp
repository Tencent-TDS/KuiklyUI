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

#include "NAPIUtil.h"

namespace kuikly {
namespace util {

double getNApiArgsDouble(napi_env env, napi_value value) {
    double doubleValue;
    napi_get_value_double(env, value, &doubleValue);
    return doubleValue;
}

int32_t getNApiArgsInt(napi_env env, napi_value value) {
    int32_t intValue;
    napi_get_value_int32(env, value, &intValue);
    return intValue;
}

int64_t getNApiArgsInt64(napi_env env, napi_value value) {
    int64_t result = 0;
    napi_get_value_int64(env, value, &result);
    return result;
}

bool getNApiArgsBool(napi_env env, napi_value value) {
    bool result = false;
    napi_get_value_bool(env, value, &result);
    return result;
}

char *getNApiArgsString(napi_env env, napi_value value) {
    // 4、获取传递的string长度
    size_t length = 0;
    if (napi_ok != napi_get_value_string_utf8(env, value, nullptr, 0, &length)) {
        napi_throw_error(env, "-1003", "napi_get_value_string_utf8 error");
        return 0;
    }

    // 6、读取传递的string参数放入buffer中
    char *buffer = new char[length + 1];
    if (napi_ok != napi_get_value_string_utf8(env, value, buffer, length + 1, &length)) {
        delete[] buffer;
        buffer = nullptr;
        napi_throw_error(env, "-1005", "napi_get_value_string_utf8 error");
        return 0;
    }
    return buffer;
}

std::string getNApiArgsStdString(napi_env env, napi_value value) {
    char *resStr = getNApiArgsString(env, value);
    std::string resString(resStr);
    delete[] resStr;
    return resString;
}

void GetNApiArgsStdString(const napi_env &env, const napi_value &value, std::string &result) {
    // 获取传递的string长度
    size_t length = 0;
    if (napi_ok != napi_get_value_string_utf8(env, value, nullptr, 0, &length)) {
        napi_throw_error(env, "-1003", "napi_get_value_string_utf8 error");
        return;
    }

    // 读取传递的string参数放入buffer中
    std::vector<char> buffer(length + 1);
    if (napi_ok != napi_get_value_string_utf8(env, value, buffer.data(), length + 1, &length)) {
        napi_throw_error(env, "-1005", "napi_get_value_string_utf8 error");
        return;
    }
    result.append(buffer.data());
}

std::string getNApiArgsAsciiStdString(napi_env env, napi_value value) {
    const std::u16string u16 = getNApiArgsStdU16String(env, value);
    std::string out(u16.size(), '\0');
    for (size_t i = 0; i < u16.size(); ++i) {
        out[i] = static_cast<char>(u16[i]);
    }
    return out;
}

std::u16string getNApiArgsStdU16String(napi_env env, napi_value value) {
    std::u16string result;
    GetNApiArgsStdU16String(env, value, result);
    return result;
}

void GetNApiArgsStdU16String(const napi_env &env, const napi_value &value, std::u16string &result) {
    size_t units = 0;
    if (napi_ok != napi_get_value_string_utf16(env, value, nullptr, 0, &units)) {
        napi_throw_error(env, "-1003", "napi_get_value_string_utf16 error");
        return;
    }
    std::u16string buffer(units + 1, u'\0');
    size_t copied = 0;
    if (napi_ok != napi_get_value_string_utf16(env, value, reinterpret_cast<char16_t *>(buffer.data()), units + 1,
                                               &copied)) {
        napi_throw_error(env, "-1005", "napi_get_value_string_utf16 error");
        return;
    }
    buffer.resize(copied);
    result.append(buffer);
}
}  // namespace util
}  // namespace kuikly
