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

#include "libohos_render/utils/KRUtfUtil.h"

#include <codecvt>
#include <locale>
#include <stdexcept>
#include "simdutf.h"

namespace kuikly {
namespace util {
namespace utf {
namespace {

}  // namespace

size_t DecodeInto(const uint8_t *input, size_t bytes, uint16_t *output, size_t capacity,
                  bool strict) {
    if (bytes == 0) {
        return 0;
    }
    if (input == nullptr || output == nullptr || capacity < bytes) {
        return SIZE_MAX;
    }
    if (strict) {
        const auto result = simdutf::convert_utf8_to_utf16_with_errors(
            reinterpret_cast<const char *>(input), bytes, reinterpret_cast<char16_t *>(output));
        return result.error == simdutf::SUCCESS ? result.count : SIZE_MAX;
    }
    const auto locations = simdutf::utf16_length_from_utf8_with_replacement(
        reinterpret_cast<const char *>(input), bytes);
    return simdutf::convert_utf8_to_utf16_with_replacement(
        reinterpret_cast<const char *>(input), bytes, reinterpret_cast<char16_t *>(output),
        locations);
}

std::u16string ToUTF16(const char *input, size_t bytes) {
    if (input == nullptr || bytes == 0) {
        return {};
    }
    std::u16string out(bytes, u'\0');
    const size_t written = DecodeInto(reinterpret_cast<const uint8_t *>(input), bytes,
                                     reinterpret_cast<uint16_t *>(out.data()), out.size(), false);
    out.resize(written);
    return out;
}

std::string ToUTF8(const uint16_t *input, size_t units, bool strict) {
    if (units == 0) {
        return {};
    }
    std::string out;
    if (units > out.max_size() / 3) {
        throw std::length_error("UTF-16 output too large");
    }
    out.resize(units * 3);
    if (strict) {
        const auto result = simdutf::convert_utf16_to_utf8_with_errors(
            reinterpret_cast<const char16_t *>(input), units, out.data());
        if (result.error != simdutf::SUCCESS) {
            throw std::invalid_argument("Malformed UTF-16 sequence");
        }
        out.resize(result.count);
        return out;
    }
    const size_t written = simdutf::convert_utf16_to_utf8_with_replacement(
        reinterpret_cast<const char16_t *>(input), units, out.data());
    out.resize(written);
    return out;
}

std::u32string ToUTF32(const std::string &input) {
    if (input.empty()) {
        return {};
    }
    std::u32string out(input.size(), U'\0');
    const auto result = simdutf::convert_utf8_to_utf32_with_errors(input.data(), input.size(), out.data());
    if (result.error != simdutf::SUCCESS) {
        std::wstring_convert<std::codecvt_utf8<char32_t>, char32_t> converter;
        return converter.from_bytes(input);
    }
    out.resize(result.count);
    return out;
}

std::string ToUTF8(const std::u32string &input) {
    if (input.empty()) {
        return {};
    }
    std::string out;
    if (input.size() > out.max_size() / 4) {
        throw std::length_error("UTF-32 output too large");
    }
    out.resize(input.size() * 4);
    const auto result = simdutf::convert_utf32_to_utf8_with_errors(input.data(), input.size(), out.data());
    if (result.error != simdutf::SUCCESS) {
        std::wstring_convert<std::codecvt_utf8<char32_t>, char32_t> converter;
        return converter.to_bytes(input);
    }
    out.resize(result.count);
    return out;
}

size_t UTF16Length(const std::string &input) {
    if (input.empty()) {
        return 0;
    }
    return simdutf::utf16_length_from_utf8_with_replacement(input.data(), input.size()).count;
}

}  // namespace utf
}  // namespace util
}  // namespace kuikly
