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
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(SelectionColorJson(0x66FF0000u, 0xFF000000u), 0),
                 0x66FF0000u);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(SelectionColorJson(0x66ABCDEFu, 0xFFFFFFFFu), 0),
                 0x66ABCDEFu);
    // alpha 低于上限时不下压
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(SelectionColorJson(0x33FF0000u, 0xFF000000u), 0),
                 0x33FF0000u);
    // alpha 高于上限时收敛到上限，RGB 保持不变
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(SelectionColorJson(0xFFFF0000u, 0xFF000000u), 0),
                 0x66FF0000u);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(SelectionColorJson(0xFFFFFFFFu, 0xFF000000u), 0),
                 0x66FFFFFFu);
}

// 结构不合法时回退到调用方的默认值，而不是抛异常或产生半截颜色
void TestIllFormedInputFallsBack() {
    const uint32_t fallback = util::kDefaultSelectionColor;
    EXPECT_COLOR(util::ParseSelectionBackgroundColor("", fallback), fallback);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor("{background:oops", fallback), fallback);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(R"({"cursor":4278190080})", fallback), fallback);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(R"({"background":"0x66FF0000"})", fallback), fallback);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor("[1,2]", fallback), fallback);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor("null", fallback), fallback);
    // 越界数值同样回退，而不是走未定义的 double → uint32_t 转换
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(R"({"background":-1})", fallback), fallback);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(R"({"background":4294967296})", fallback), fallback);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(R"({"background":1e300})", fallback), fallback);
    // 范围两端仍要生效：0 是合法取值，UINT32_MAX 收敛到 alpha 上限
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(R"({"background":0})", fallback), 0x00000000u);
    EXPECT_COLOR(util::ParseSelectionBackgroundColor(R"({"background":4294967295})", fallback),
                 0x66FFFFFFu);
}

// 迁移前 KRRichTextView 硬编码 0x33007DFF，未配置时必须仍是它
void TestDefaultMatchesPreviousHardCodedValue() {
    EXPECT_COLOR(util::kDefaultSelectionColor, 0x33007DFFu);
    EXPECT_COLOR((util::kDefaultSelectionColor >> util::kColorAlphaShift) & util::kColorAlphaMask, 0x33u);
    EXPECT(util::kDefaultSelectionColor <= util::kSelectionColorMaxAlpha << util::kColorAlphaShift ||
           (((util::kDefaultSelectionColor >> util::kColorAlphaShift) & util::kColorAlphaMask) <=
            util::kSelectionColorMaxAlpha));
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
    TestIllFormedInputFallsBack();
    TestDefaultMatchesPreviousHardCodedValue();
    TestClampBoundaries();
    if (g_failures == 0) {
        std::printf("krselectioncolor_test: all passed\n");
        return 0;
    }
    std::printf("krselectioncolor_test: %d failure(s)\n", g_failures);
    return 1;
}
