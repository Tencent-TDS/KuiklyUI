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

#ifndef CORE_RENDER_OHOS_KRUTFUTIL_H
#define CORE_RENDER_OHOS_KRUTFUTIL_H

#include <cstddef>
#include <cstdint>
#include <string>

namespace kuikly {
namespace util {
namespace utf {

std::u16string ToUTF16(const char *input, size_t bytes);
// The strict overload is reserved for a future explicit caller; renderer paths use replacement.
std::string ToUTF8(const uint16_t *input, size_t units, bool strict = false);
std::u32string ToUTF32(const std::string &input);
std::string ToUTF8(const std::u32string &input);

// UTF-16 unit count after Unicode maximal-subpart replacement.
size_t UTF16Length(const std::string &input);

// Clamps a byte cut to a UTF-8 character boundary.
//
// Call this before slicing a UTF-8 buffer at an arbitrary byte offset. A cut inside a
// multi-byte sequence yields ill-formed UTF-8, which the strict ToUTF32 overload
// rejects by throwing std::range_error; an uncaught throw aborts the renderer process.
//
// `bytes` must be <= strlen(input), so reading input[bytes] stays in bounds (it is a
// real byte or the terminator). Returns the largest boundary-aligned length <= bytes:
// continuation bytes (0x80..0xBF) at the cut point are dropped one by one.
size_t TruncateToUTF8Boundary(const char *input, size_t bytes);

// Allocation-free decode. Strict mode is reserved for a future explicit caller and returns
// SIZE_MAX on malformed input; default mode replaces each maximal ill-formed subpart with U+FFFD.
// Capacity is in UTF-16 units and must be at least bytes. No terminator is written.
size_t DecodeInto(const uint8_t *input, size_t bytes, uint16_t *output, size_t capacity,
                  bool strict);

}  // namespace utf
}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRUTFUTIL_H
