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

#ifndef CORE_RENDER_OHOS_KRRENDERVALUE_H
#define CORE_RENDER_OHOS_KRRENDERVALUE_H

// NAPI conversions live in KRRenderValueNapi.cpp.
#include <cstddef>
#include <cstdint>
#include <memory>
#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

#include "libohos_render/foundation/type/KRRenderCValue.h"

class KRRenderValue;
using KRRenderValueMap = std::unordered_map<std::u16string, KRRenderValue>;
using KRRenderValueArray = std::vector<KRRenderValue>;

/**
 * Compatibility façade over the unified KRJSON storage.
 *
 * Every value — including the ArkTS-only raw NAPI handle side channel (snapshot
 * PixelMap/drawableDescriptor, which must never cross the Kotlin ABI) — is
 * exactly one 8-byte tagged KRJSON word: NAPI handles live in a kTagExt
 * OpaqueBox. This façade is therefore a trivially-small RAII value type — copy
 * retains the word, move transfers it, destruction releases it — with no
 * per-value shared_ptr. (Retain/Release are no-ops for immediates and one atomic
 * for heap-backed words.)
 */
class KRRenderValue {
 public:
    using Map = KRRenderValueMap;
    using Array = KRRenderValueArray;
    using ByteArray = std::shared_ptr<std::vector<uint8_t>>;

    // Default/nullptr construction preserves the old empty shared_ptr state.
    // KRRenderValue::Make() creates a present JSON null instead.
    KRRenderValue() = default;
    KRRenderValue(std::nullptr_t) {}

    KRRenderValue(const KRRenderValue &other);
    KRRenderValue &operator=(const KRRenderValue &other);
    KRRenderValue(KRRenderValue &&other) noexcept;
    KRRenderValue &operator=(KRRenderValue &&other) noexcept;
    KRRenderValue &operator=(std::nullptr_t);
    ~KRRenderValue();

    explicit operator bool() const { return value_ != KRJSON_INVALID; }
    bool operator==(std::nullptr_t) const { return !static_cast<bool>(*this); }
    bool operator!=(std::nullptr_t) const { return static_cast<bool>(*this); }
    // Value equality: same tagged word, or same JSON content. UTF-8 / UTF-16
    // strings compare by text; objects compare by key (order-insensitive).
    // Empty (INVALID) is not equal to JSON null. NAPI boxes compare by handle.
    bool operator==(const KRRenderValue &other) const;
    bool operator!=(const KRRenderValue &other) const { return !(*this == other); }

    // Transitional compatibility: existing KRAnyValue call sites may keep `value->`.
    KRRenderValue *operator->() { return this; }
    const KRRenderValue *operator->() const { return this; }

    template<typename... Args>
    static KRRenderValue Make(Args &&...args) {
        return MakeOwned(Build(std::forward<Args>(args)...));
    }

    static KRRenderValue MakeNull();
    static KRRenderValue MakeEmptyString();
    /** Box a C ABI / ArkUI UTF-8 buffer as UTF-16. Prefer a u16string source and Make(). */
    static KRRenderValue MakeUTF16(const std::string &utf8);
    static KRRenderValue MakeUTF16(const char *utf8);
    static KRRenderValue MakeBorrowed(KRJSON value);

    KRJSON jsonValue() const { return value_; }

    bool isNull() const;
    bool isBool() const;
    bool isInt() const;
    bool isLong() const;
    bool isFloat() const;
    bool isDouble() const;
    bool isString() const;
    bool isMap() const;
    bool isArray() const;
    bool isByteArray() const;
    bool isNapiValue() const;

    // toMap()/toArray() parse a JSON *string* payload transparently. Point
    // queries keep that conversion explicit and single-shot: bridge payloads
    // that may arrive as text call Parsed() once, then opt/at on the result.
    KRRenderValue Parsed() const;

    // Object/array point query without materializing unordered_map/vector.
    // Missing key / OOB index / wrong type → empty. String JSON is not
    // auto-parsed; call Parsed()/Parse() once, then opt/at.
    // Prefer opt(u"key") / opt(std::u16string) for C++ literals. Pointer
    // overloads forward to the string versions. Object keys are UTF-16;
    // opt(std::string) transcodes the query key at this edge.
    KRRenderValue opt(std::nullptr_t) const;
    KRRenderValue opt(const std::string &key) const;
    KRRenderValue opt(const std::u16string &key) const;
    KRRenderValue opt(const char *key) const;
    KRRenderValue opt(const char16_t *key) const;
    KRRenderValue opt(const uint16_t *key) const;
    KRRenderValue at(size_t index) const;
    size_t size() const;

    static KRRenderValue Parse(const std::string &json);

    bool toBool() const;
    int32_t toInt() const;
    int64_t toLong() const;
    float toFloat() const;
    double toDouble() const;
    std::u16string toU16String() const;
    // Zero-copy view of a U16 string box. {nullptr, 0} if not KRJSON_U16STRING.
    std::pair<const uint16_t *, size_t> utf16View() const;
    // Compare this string box to an ASCII literal without allocating or transcoding.
    bool equalsAscii(const char *lit) const;
    bool stringEquals(const KRRenderValue &other) const;
    // Narrow a U16 box that is all < 0x80 without running UTF16ToUTF8.
    // Non-ASCII U16 falls back to stringValue(); UTF-8 boxes and non-strings
    // match toString() / stringValue().
    std::string toAsciiString() const;
    std::string toString() const;
    Map toMap() const;
    Array toArray() const;
    ByteArray toByteArray() const;
    KRJSON toCValue() const;

    // The NAPI bridge adopts an already-owned KRJSON word without an extra Retain.
    struct Accessor {
        static KRRenderValue Adopt(KRJSON owned);
        static KRJSONType Type(const KRRenderValue &value);
    };

 private:
    explicit KRRenderValue(KRJSON value) : value_(value) {}

    static KRRenderValue MakeOwned(KRJSON value) { return KRRenderValue(value); }

    static KRJSON Build();
    static KRJSON Build(std::nullptr_t);
    static KRJSON Build(bool value);
    static KRJSON Build(int32_t value);
    static KRJSON Build(int64_t value);
    static KRJSON Build(float value);
    static KRJSON Build(double value);
    static KRJSON Build(const std::string &value);
    static KRJSON Build(const char *value);
    static KRJSON Build(const std::u16string &value);
    static KRJSON Build(const char16_t *value);
    static KRJSON Build(const ByteArray &value);
    static KRJSON Build(const Map &value);
    static KRJSON Build(const Array &value);
    // KRRenderCValue is uint64_t (== size_t on aarch64): accepting it would make
    // Make(vec.size()) reinterpret a number as a tagged pointer. Wrap raw words
    // with MakeBorrowed() or Accessor::Adopt() instead.
    static KRJSON Build(const KRRenderCValue &value) = delete;
    static KRJSON BuildUTF16FromUTF8(const char *s, size_t n);

    static KRRenderValue ChildOrEmpty(KRJSON child);
    static KRRenderValue MakeParsed(const char *data, size_t length);
    static KRRenderValue MakeParsed(const std::string &json);
    static KRRenderValue MakeParsedUTF16(const uint16_t *data, size_t units);

    KRRenderValue parsedFromJsonText() const;
    std::string stringValue() const;
    KRJSONType type() const;

    KRJSON value_ = KRJSON_INVALID;
};

template<>
KRRenderValue KRRenderValue::Make<const char *>(const char *&&value);

#endif  // CORE_RENDER_OHOS_KRRENDERVALUE_H
