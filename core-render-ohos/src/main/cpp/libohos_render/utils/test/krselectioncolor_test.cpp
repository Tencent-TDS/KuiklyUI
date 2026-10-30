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
//   cc -c -I core-render-ohos/src/main/cpp thirdparty/cJSON/cJSON.c -o cJSON.o
//   clang++ -std=c++17 -I core-render-ohos/src/main/cpp \
//     krselectioncolor_test.cpp ../KRSelectionColorUtil.cpp cJSON.o -o krselectioncolor_test

#include <cstdint>
#include <cstdio>
#include <string>

#include "libohos_render/utils/KRSelectionColorUtil.h"

namespace util = kuikly::util;

static int g_failures = 0;

#define EXPECT(cond)                                                                    \
    do {                                                                                \
        if (!(cond)) {                                                                  \
            std::fprintf(stderr, "%s:%d: EXPECT(%s)\n", __FILE__, __LINE__, #cond);     \
            ++g_failures;                                                               \
        }                                                                               \
    } while (0)

#define EXPECT_COLOR(actual, expected)                                                  \
    do {                                                                                \
        const uint32_t a = static_cast<uint32_t>(actual);                               \
        const uint32_t e = static_cast<uint32_t>(expected);                             \
        if (a != e) {                                                                   \
            std::fprintf(stderr, "%s:%d: got 0x%08X, want 0x%08X\n", __FILE__, __LINE__, a, e); \
            ++g_failures;                                                               \
        }                                                                               \
    } while (0)

// DivView.selectionColor 下发的形态：{"background": 0x66RRGGBB, "cursor": 0xFFRRGGBB}
static std::string SelectionColorJson(uint32_t background, uint32_t cursor) {
    char buffer[128];
    std::snprintf(buffer, sizeof(buffer), "{\"background\":%u,\"cursor\":%u}", background, cursor);
    return std::string(buffer);
}

// 上层按 0x66 上限下发时原样保留，RGB 分量不可被 alpha 收敛动到
void TestBackgroundColorIsParsed() {
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0x66FF0000u, 0xFF000000u)).background,
                 0x66FF0000u);
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0x66ABCDEFu, 0xFFFFFFFFu)).background,
                 0x66ABCDEFu);
    // alpha 低于上限时不下压
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0x33FF0000u, 0xFF000000u)).background,
                 0x33FF0000u);
    // alpha 高于上限时收敛到上限，RGB 保持不变
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0xFFFF0000u, 0xFF000000u)).background,
                 0x66FF0000u);
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0xFFFFFFFFu, 0xFF000000u)).background,
                 0x66FFFFFFu);
}

// 手柄色与 Android / iOS 一致，只取值不做 alpha 收敛
void TestCursorColorIsParsedWithoutClamping() {
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0x66FF0000u, 0xFF123456u)).cursor,
                 0xFF123456u);
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0x66FF0000u, 0x80ABCDEFu)).cursor,
                 0x80ABCDEFu);
    // 全不透明与全透明都原样保留
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0x66FF0000u, 0xFFFFFFFFu)).cursor,
                 0xFFFFFFFFu);
    EXPECT_COLOR(util::ParseSelectionColors(SelectionColorJson(0x66FF0000u, 0x00000000u)).cursor,
                 0x00000000u);
}

// 结构不合法时两路都回退到各自默认值，而不是抛异常或产生半截颜色
void TestIllFormedInputFallsBack() {
    const uint32_t bg = util::kDefaultSelectionColor;
    const uint32_t cursor = util::kDefaultSelectionCursorColor;
    const char *cases[] = {
        "", "{background:oops", R"({"background":"0x66FF0000"})", "[1,2]", "null",
        R"({"background":-1})", R"({"background":4294967296})", R"({"background":1e300})",
    };
    for (const char *input : cases) {
        const util::KRSelectionColors colors = util::ParseSelectionColors(input);
        EXPECT_COLOR(colors.background, bg);
        EXPECT_COLOR(colors.cursor, cursor);
    }
    // 缺 background 但 cursor 合法时，只回退 background
    char cursor_only[64];
    std::snprintf(cursor_only, sizeof(cursor_only), R"({"cursor":%u})", 0xFF123456u);
    const util::KRSelectionColors partial = util::ParseSelectionColors(cursor_only);
    EXPECT_COLOR(partial.background, bg);
    EXPECT_COLOR(partial.cursor, 0xFF123456u);
    // 范围两端仍要生效：0 是合法取值，UINT32_MAX 收敛到 alpha 上限
    EXPECT_COLOR(util::ParseSelectionColors(R"({"background":0})").background, 0x00000000u);
    EXPECT_COLOR(util::ParseSelectionColors(R"({"background":4294967295})").background, 0x66FFFFFFu);
}

// 迁移前背景是 KRRichTextView 硬编码 0x33007DFF、手柄是 KRView 硬编码 0xFF0000FF
void TestDefaultsMatchPreviousHardCodedValues() {
    EXPECT_COLOR(util::kDefaultSelectionColor, 0x33007DFFu);
    EXPECT_COLOR(util::kDefaultSelectionCursorColor, 0xFF0000FFu);
    EXPECT(((util::kDefaultSelectionColor >> util::kColorAlphaShift) & util::kColorAlphaMask) <=
           util::kSelectionColorMaxAlpha);
    const util::KRSelectionColors empty = util::ParseSelectionColors("");
    EXPECT_COLOR(empty.background, 0x33007DFFu);
    EXPECT_COLOR(empty.cursor, 0xFF0000FFu);
}

void TestClampBoundaries() {
    EXPECT_COLOR(util::ClampSelectionColorAlpha(0x66000000u), 0x66000000u);
    EXPECT_COLOR(util::ClampSelectionColorAlpha(0x67000000u), 0x66000000u);
    EXPECT_COLOR(util::ClampSelectionColorAlpha(0xFF123456u), 0x66123456u);
    EXPECT_COLOR(util::ClampSelectionColorAlpha(0x00123456u), 0x00123456u);
    EXPECT_COLOR(util::ClampSelectionColorAlpha(0x00000000u), 0x00000000u);
}

int main() {
    TestBackgroundColorIsParsed();
    TestCursorColorIsParsedWithoutClamping();
    TestIllFormedInputFallsBack();
    TestDefaultsMatchPreviousHardCodedValues();
    TestClampBoundaries();
    if (g_failures == 0) {
        std::printf("krselectioncolor_test: all passed\n");
        return 0;
    }
    std::printf("krselectioncolor_test: %d failure(s)\n", g_failures);
    return 1;
}
