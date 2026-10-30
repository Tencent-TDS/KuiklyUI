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

// Host build:
//   clang++ -std=c++17 -I core-render-ohos/src/main/cpp -I <simdutf>/include \
//     krutfutil_test.cpp ../KRUtfUtil.cpp <simdutf>/src/simdutf.cpp -o krutfutil_test

#include <cstdint>
#include <cstdio>
#include <string>

#include "libohos_render/utils/KRUtfUtil.h"

namespace utf = kuikly::util::utf;

static int g_failures = 0;

#define EXPECT(cond)                                                                    \
    do {                                                                                \
        if (!(cond)) {                                                                  \
            std::fprintf(stderr, "%s:%d: EXPECT(%s)\n", __FILE__, __LINE__, #cond);    \
            ++g_failures;                                                               \
        }                                                                               \
    } while (0)

#define EXPECT_EQ_SIZE(actual, expected)                                                \
    do {                                                                                \
        const size_t a = static_cast<size_t>(actual);                                   \
        const size_t e = static_cast<size_t>(expected);                                 \
        if (a != e) {                                                                   \
            std::fprintf(stderr, "%s:%d: got %zu, want %zu\n", __FILE__, __LINE__, a, e); \
            ++g_failures;                                                               \
        }                                                                               \
    } while (0)

// 「这是一段测试文本」 as UTF-8: 8 three-byte characters, 24 bytes.
const char *kCjkText = "\xE8\xBF\x99"
                       "\xE6\x98\xAF"
                       "\xE4\xB8\x80"
                       "\xE6\xAE\xB5"
                       "\xE6\xB5\x8B"
                       "\xE8\xAF\x95"
                       "\xE6\x96\x87"
                       "\xE6\x9C\xAC";
const std::u32string kCjkCodePoints =
    U"\u8FD9\u662F\u4E00\u6BB5\u6D4B\u8BD5\u6587\u672C";

// A paste buffer cut by a byte cap on the middle of a character, which is what
// KRTextFieldView::OnPasteText produced before the boundary fix
// (min(24 bytes, maxLength 4 * 4) = 16 bytes = 5 characters + 1 orphan byte).
void TestByteTruncatedPasteDecodes() {
    const std::string truncated(kCjkText, 16);
    const std::u32string decoded = utf::ToUTF32(truncated);
    EXPECT_EQ_SIZE(decoded.size(), 6);
    EXPECT(decoded.compare(0, 5, kCjkCodePoints, 0, 5) == 0);
    EXPECT(decoded[5] == U'\uFFFD');
}

void TestBoundaryLengthFloorsToCharacterBoundary() {
    // 16 bytes hold 5 complete 3-byte characters; the sixth is cut after 1 byte.
    EXPECT_EQ_SIZE(utf::UTF8BoundaryLength(kCjkText, 16), 15);
    EXPECT_EQ_SIZE(utf::UTF8BoundaryLength(kCjkText, 24), 24);
    // A cut at a boundary stays put, including after a 4-byte sequence.
    EXPECT_EQ_SIZE(utf::UTF8BoundaryLength(kCjkText, 15), 15);
    const char *kEmoji = "\xF0\x9F\x98\x80";  // U+1F600
    EXPECT_EQ_SIZE(utf::UTF8BoundaryLength(kEmoji, 4), 4);
    EXPECT_EQ_SIZE(utf::UTF8BoundaryLength(kEmoji, 1), 0);
    EXPECT_EQ_SIZE(utf::UTF8BoundaryLength(kEmoji, 3), 0);
    EXPECT_EQ_SIZE(utf::UTF8BoundaryLength("abc", 2), 2);
}

void TestValidInputRoundTrips() {
    EXPECT(utf::ToUTF32(kCjkText) == kCjkCodePoints);
    EXPECT(utf::ToUTF8(kCjkCodePoints) == kCjkText);
    EXPECT(utf::ToUTF32("") == std::u32string());
    EXPECT(utf::ToUTF8(std::u32string()) == "");
}

// Every maximal ill-formed subpart becomes one U+FFFD, matching the UTF-16 paths.
void TestIllFormedUTF8DecodesWithReplacement() {
    EXPECT(utf::ToUTF32("a\xFFz") == U"a\uFFFDz");
    EXPECT(utf::ToUTF32(std::string(kCjkText, 16))[5] == U'\uFFFD');
    EXPECT_EQ_SIZE(utf::ToUTF32("\xE4\xB8").size(), 1);
    EXPECT(utf::ToUTF32("\xE4\xB8").front() == U'\uFFFD');
}

// ToUTF8(u32) sits on the same callback path, so an ill-formed code point must encode, not throw.
void TestIllFormedUTF32EncodesWithReplacement() {
    EXPECT(utf::ToUTF8(std::u32string(1, static_cast<char32_t>(0xD800))) == "\xEF\xBF\xBD");
    EXPECT(utf::ToUTF8(std::u32string(1, static_cast<char32_t>(0x110000))) == "\xEF\xBF\xBD");
    // A surrogate has no universal character name, so build the sequence directly.
    std::u32string mixed = U"a";
    mixed.push_back(static_cast<char32_t>(0xD800));
    mixed.push_back(U'z');
    EXPECT(utf::ToUTF8(mixed) == "a\xEF\xBF\xBDz");
}

void TestSupplementaryPlaneRoundTrips() {
    const std::u32string emoji = U"\U0001F600";
    EXPECT(utf::ToUTF8(emoji) == "\xF0\x9F\x98\x80");
    EXPECT(utf::ToUTF32("\xF0\x9F\x98\x80") == emoji);
    EXPECT(utf::ToUTF32(utf::ToUTF8(emoji)) == emoji);
}

int main() {
    TestByteTruncatedPasteDecodes();
    TestBoundaryLengthFloorsToCharacterBoundary();
    TestValidInputRoundTrips();
    TestIllFormedUTF8DecodesWithReplacement();
    TestIllFormedUTF32EncodesWithReplacement();
    TestSupplementaryPlaneRoundTrips();
    if (g_failures != 0) {
        std::fprintf(stderr, "%d failure(s)\n", g_failures);
        return 1;
    }
    std::printf("krutfutil_test: all passed\n");
    return 0;
}
