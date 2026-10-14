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

// Parser integer classification + numeric integer equality.

#include <cstdint>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <string>

#include "libohos_render/utils/json/Box.h"
#include "libohos_render/utils/json/Reader.h"

namespace kj = kuikly::util::json;

static int g_failures = 0;

#define EXPECT(cond)                                                     \
    do {                                                                 \
        if (!(cond)) {                                                   \
            std::fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); \
            ++g_failures;                                                \
        }                                                                \
    } while (0)

static KRJSON Member(KRJSON obj, const char *key) {
    return kj::ObjectGet(obj, key, std::strlen(key));
}

static void TestParserClassification(bool utf16) {
    const std::string text =
        R"({"zero":0,"pos":5,"neg":-5,"i32max":2147483647,"i32over":2147483648,)"
        R"("i64max":9223372036854775807,"u64":18446744073709551615,"dbl":1.5})";
    KRJSON root = KRJSON_INVALID;
    if (utf16) {
        const std::u16string u16 = kj::UTF8ToUTF16(text.data(), text.size());
        root = kj::Reader::ParseUTF16(reinterpret_cast<const uint16_t *>(u16.data()), u16.size(), nullptr);
    } else {
        root = kj::Reader::Parse(text.data(), text.size(), nullptr);
    }
    EXPECT(root != KRJSON_INVALID);

    EXPECT(kj::GetType(Member(root, "zero")) == KRJSON_INT);
    EXPECT(kj::GetType(Member(root, "pos")) == KRJSON_INT);
    EXPECT(kj::GetType(Member(root, "neg")) == KRJSON_INT);
    EXPECT(kj::GetType(Member(root, "i32max")) == KRJSON_INT);
    EXPECT(kj::GetType(Member(root, "i32over")) == KRJSON_LONG);
    EXPECT(kj::GetType(Member(root, "i64max")) == KRJSON_LONG);
    EXPECT(kj::GetType(Member(root, "u64")) == KRJSON_UINT);
    EXPECT(kj::GetType(Member(root, "dbl")) == KRJSON_DOUBLE);

    EXPECT(kj::GetInt(Member(root, "pos"), 0) == 5);
    EXPECT(kj::GetInt(Member(root, "i64max"), 0) == INT64_MAX);
    EXPECT(kj::GetUint(Member(root, "u64"), 0) == UINT64_MAX);

    KRJSON five = kj::NewInt32(5);
    EXPECT(kj::Equals(Member(root, "pos"), five));
    kj::Release(five);

    kj::Release(root);
}

static void TestIntegerEquality() {
    KRJSON i32 = kj::NewInt32(5);
    KRJSON i64 = kj::NewInt64(5);
    KRJSON u = kj::NewUint(5);
    KRJSON neg64 = kj::NewInt64(-1);
    KRJSON umax = kj::NewUint64(UINT64_MAX);
    KRJSON big_i64 = kj::NewInt64(INT64_MAX);
    KRJSON big_u = kj::NewUint64(static_cast<uint64_t>(INT64_MAX));
    KRJSON dbl = kj::NewDouble(5.0);

    EXPECT(kj::Equals(i32, i64));
    EXPECT(kj::Equals(i64, i32));
    EXPECT(kj::Equals(i32, u));
    EXPECT(kj::Equals(u, i64));
    EXPECT(kj::Equals(big_i64, big_u));  // heap-boxed on both sides
    EXPECT(!kj::Equals(neg64, umax));    // -1 must not equal UINT64_MAX
    EXPECT(!kj::Equals(umax, neg64));
    EXPECT(!kj::Equals(i32, neg64));
    EXPECT(!kj::Equals(i32, dbl));       // integers never equal floating values

    KRJSON arr_a = kj::NewArray();
    KRJSON arr_b = kj::NewArray();
    kj::ArrayAppend(arr_a, i32);
    kj::ArrayAppend(arr_b, i64);
    EXPECT(kj::Equals(arr_a, arr_b));

    for (KRJSON v : {i32, i64, u, neg64, umax, big_i64, big_u, dbl, arr_a, arr_b}) {
        kj::Release(v);
    }
}

static void TestReplaceRetainFirst() {
    KRJSON arr = kj::NewArray();
    KRJSON one = kj::NewInt32(1);
    kj::ArrayAppend(arr, one);
    kj::Release(one);
    kj::ArraySet(arr, 0, kj::ArrayGet(arr, 0));
    EXPECT(kj::GetInt(kj::ArrayGet(arr, 0), 0) == 1);
    kj::Release(arr);

    const uint16_t key_a[] = {u'a'};
    const uint16_t key_b[] = {u'b'};
    KRJSON obj = kj::NewObject();
    KRJSON seven = kj::NewInt32(7);
    kj::ObjectPutUTF16(obj, key_a, 1, seven);
    kj::Release(seven);
    kj::ObjectPutUTF16(obj, key_a, 1, kj::ObjectGetUTF16(obj, key_a, 1));
    EXPECT(kj::GetInt(kj::ObjectGetUTF16(obj, key_a, 1), 0) == 7);

    KRJSON inner = kj::NewObject();
    KRJSON nine = kj::NewInt32(9);
    kj::ObjectPutUTF16(inner, key_b, 1, nine);
    kj::Release(nine);
    kj::ObjectPutUTF16(obj, key_a, 1, inner);
    kj::Release(inner);
    kj::ObjectPutUTF16(obj, key_a, 1, kj::ObjectGetUTF16(kj::ObjectGetUTF16(obj, key_a, 1), key_b, 1));
    EXPECT(kj::GetInt(kj::ObjectGetUTF16(obj, key_a, 1), 0) == 9);
    kj::Release(obj);
}

static void TestIntIfSafeIntegral() {
    const double max_safe = 9007199254740991.0;  // 2^53 - 1
    struct Case {
        double input;
        KRJSONType type;
    } cases[] = {
        {5.0, KRJSON_INT},          {-5.0, KRJSON_INT},          {0.0, KRJSON_INT},
        {-0.0, KRJSON_INT},         {2147483648.0, KRJSON_LONG}, {max_safe, KRJSON_LONG},
        {-max_safe, KRJSON_LONG},   {max_safe + 1, KRJSON_DOUBLE}, {1.5, KRJSON_DOUBLE},
    };
    for (const auto &c : cases) {
        KRJSON v = kj::NewIntIfSafeIntegral(c.input);
        EXPECT(kj::GetType(v) == c.type);
        if (c.type != KRJSON_DOUBLE) {
            EXPECT(kj::GetInt(v, 1) == static_cast<int64_t>(c.input));
        }
        kj::Release(v);
    }
    KRJSON five = kj::NewIntIfSafeIntegral(5.0);
    EXPECT(kj::Dump(five) == "5");
    kj::Release(five);
    KRJSON nan = kj::NewIntIfSafeIntegral(std::nan(""));
    EXPECT(kj::GetType(nan) == KRJSON_NULL);
    kj::Release(nan);
}

int main() {
    TestParserClassification(false);
    TestParserClassification(true);
    TestIntegerEquality();
    TestReplaceRetainFirst();
    TestIntIfSafeIntegral();
    if (g_failures != 0) {
        std::fprintf(stderr, "%d failure(s)\n", g_failures);
        return 1;
    }
    std::printf("krjson_number_test: all passed\n");
    return 0;
}
