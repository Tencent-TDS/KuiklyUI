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

#include <stdexcept>
#include "simdutf.h"

namespace kuikly {
namespace util {
namespace utf {
namespace {

// Widens well-formed UTF-16, the only shape ToUTF16 produces, by combining surrogate pairs.
std::u32string WidenToUTF32(const std::u16string &input) {
    std::u32string out;
    out.reserve(input.size());
    for (size_t i = 0; i < input.size(); ++i) {
        const char16_t high = input[i];
        if (high < 0xD800 || high > 0xDBFF || i + 1 == input.size()) {
            out.push_back(static_cast<char32_t>(high));
            continue;
        }
        out.push_back(static_cast<char32_t>(0x10000 + ((high - 0xD800) << 10) +
                                            (input[++i] - 0xDC00)));
    }
    return out;
}

// Supplementary code points become surrogate pairs; a surrogate half or anything above U+10FFFF
// becomes U+FFFD.
std::u16string NarrowToUTF16(const std::u32string &input) {
    std::u16string out;
    out.reserve(input.size());
    for (const char32_t codePoint : input) {
        if (codePoint > 0x10FFFF || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
            out.push_back(static_cast<char16_t>(0xFFFD));
        } else if (codePoint > 0xFFFF) {
            const char32_t offset = codePoint - 0x10000;
            out.push_back(static_cast<char16_t>(0xD800 + (offset >> 10)));
            out.push_back(static_cast<char16_t>(0xDC00 + (offset & 0x3FF)));
        } else {
            out.push_back(static_cast<char16_t>(codePoint));
        }
    }
    return out;
}

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
        // Ill-formed UTF-8 reaches here (a byte-truncated buffer, for instance). Callers sit on
        // OH_ArkUI_NodeEvent callbacks where a thrown exception terminates the process, so decode
        // through the UTF-16 replacement path and widen it.
        return WidenToUTF32(ToUTF16(input.data(), input.size()));
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
        // Ill-formed UTF-32 (lone surrogate, code point above U+10FFFF) must encode, not throw.
        const std::u16string narrowed = NarrowToUTF16(input);
        return ToUTF8(reinterpret_cast<const uint16_t *>(narrowed.data()), narrowed.size(), false);
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

size_t UTF8BoundaryLength(const char *input, size_t bytes) {
    // A UTF-8 sequence is at most 4 bytes, so only the last 3 bytes can hold an incomplete tail.
    for (size_t back = 1; back <= 3 && back <= bytes; ++back) {
        const unsigned char lead = static_cast<unsigned char>(input[bytes - back]);
        if ((lead & 0xC0) == 0x80) {
            continue;
        }
        const size_t sequenceLength = lead < 0x80 ? 1 : lead < 0xE0 ? 2 : lead < 0xF0 ? 3 : 4;
        return bytes - back + sequenceLength > bytes ? bytes - back : bytes;
    }
    return bytes;
}

}  // namespace utf
}  // namespace util
}  // namespace kuikly
