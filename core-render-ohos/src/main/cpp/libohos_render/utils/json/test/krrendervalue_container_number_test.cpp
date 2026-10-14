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
//   clang++ -std=c++17 -I core-render-ohos/src/main/cpp -I <rapidjson>/include \
//     krrendervalue_container_number_test.cpp ../../../foundation/type/KRRenderValue.cpp \
//     ../Box.cpp ../Allocator.cpp ../Reader.cpp ../DomBuilder.cpp ../../../api/src/KRJSON.cpp

#include <cstdio>
#include <string>

#include "libohos_render/foundation/type/KRRenderValue.h"
#include "libohos_render/utils/json/Box.h"

namespace kj = kuikly::util::json;

static int g_failures = 0;

#define EXPECT_EQ_STR(actual, expected)                                                         \
    do {                                                                                        \
        const std::string a = (actual);                                                         \
        if (a != (expected)) {                                                                  \
            std::fprintf(stderr, "%s:%d: got %s, want %s\n", __FILE__, __LINE__, a.c_str(),     \
                         (expected));                                                           \
            ++g_failures;                                                                       \
        }                                                                                       \
    } while (0)

static void TestMapElements() {
    KRRenderValue::Map map;
    map[u"d_int"] = KRRenderValue::Make(5.0);
    map[u"d_frac"] = KRRenderValue::Make(1.5);
    map[u"f_int"] = KRRenderValue::Make(2.0f);
    map[u"i"] = KRRenderValue::Make(7);
    auto value = KRRenderValue::Make(std::move(map));
    EXPECT_EQ_STR(kj::Dump(value.Parsed().opt(u"d_int").jsonValue()), "5");
    EXPECT_EQ_STR(kj::Dump(value.Parsed().opt(u"d_frac").jsonValue()), "1.5");
    EXPECT_EQ_STR(kj::Dump(value.Parsed().opt(u"f_int").jsonValue()), "2");
    EXPECT_EQ_STR(kj::Dump(value.Parsed().opt(u"i").jsonValue()), "7");
}

static void TestArrayElementsAndTopLevel() {
    KRRenderValue::Array array;
    array.push_back(KRRenderValue::Make(100.0));
    array.push_back(KRRenderValue::Make(0.25));
    EXPECT_EQ_STR(kj::Dump(KRRenderValue::Make(std::move(array)).jsonValue()), "[100,0.25]");
    // Top-level doubles are not containers: they keep the double type.
    EXPECT_EQ_STR(kj::Dump(KRRenderValue::Make(5.0).jsonValue()), "5.0");
}

int main() {
    TestMapElements();
    TestArrayElementsAndTopLevel();
    if (g_failures != 0) {
        std::fprintf(stderr, "%d failure(s)\n", g_failures);
        return 1;
    }
    std::printf("krrendervalue_container_number_test: all passed\n");
    return 0;
}
