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

#include "libohos_render/foundation/type/KRRenderValue.h"

#include <cassert>
#include <charconv>
#include <cmath>
#include <cstring>
#include <utility>
#include <vector>

#include "libohos_render/api/include/Kuikly/KRJSON.h"
#include "libohos_render/utils/json/Box.h"
#include "libohos_render/utils/json/Reader.h"

KRRenderValue::KRRenderValue(const KRRenderValue &other)
    : value_(kuikly::util::json::Retain(other.value_)) {}

KRRenderValue &KRRenderValue::operator=(const KRRenderValue &other) {
    if (this != &other) {
        const KRJSON retained = kuikly::util::json::Retain(other.value_);
        kuikly::util::json::Release(value_);
        value_ = retained;
    }
    return *this;
}

KRRenderValue::KRRenderValue(KRRenderValue &&other) noexcept
    : value_(std::exchange(other.value_, KRJSON_INVALID)) {}

KRRenderValue &KRRenderValue::operator=(KRRenderValue &&other) noexcept {
    if (this != &other) {
        kuikly::util::json::Release(value_);
        value_ = std::exchange(other.value_, KRJSON_INVALID);
    }
    return *this;
}

KRRenderValue &KRRenderValue::operator=(std::nullptr_t) {
    kuikly::util::json::Release(value_);
    value_ = KRJSON_INVALID;
    return *this;
}

KRRenderValue::~KRRenderValue() {
    kuikly::util::json::Release(value_);
}

bool KRRenderValue::operator==(const KRRenderValue &other) const {
    return KRJSONEquals(value_, other.value_);
}

template<>
KRRenderValue KRRenderValue::Make<const char *>(const char *&&value) {
    return value == nullptr || value[0] == '\0' ? MakeEmptyString() : MakeOwned(Build(value));
}

KRRenderValue KRRenderValue::MakeNull() {
    return MakeOwned(kuikly::util::json::NewNull());
}

KRRenderValue KRRenderValue::MakeEmptyString() {
    static const auto value = MakeOwned(kuikly::util::json::NewStringUTF16(nullptr, 0));
    return value;
}

KRRenderValue KRRenderValue::MakeUTF16(const std::string &utf8) {
    return MakeOwned(BuildUTF16FromUTF8(utf8.data(), utf8.size()));
}

KRRenderValue KRRenderValue::MakeUTF16(const char *utf8) {
    return utf8 == nullptr ? MakeEmptyString()
                           : MakeOwned(BuildUTF16FromUTF8(utf8, std::char_traits<char>::length(utf8)));
}

KRRenderValue KRRenderValue::MakeBorrowed(KRJSON value) {
    return MakeOwned(kuikly::util::json::Retain(value));
}

bool KRRenderValue::isNull() const {
    return type() == KRJSON_NULL && !isNapiValue();
}
bool KRRenderValue::isBool() const {
    return type() == KRJSON_BOOL;
}
bool KRRenderValue::isInt() const {
    return kuikly::util::json::TagOf(value_) == kuikly::util::json::kTagNumber &&
           kuikly::util::json::KindOfNumber(value_) == kuikly::util::json::kNumInt32;
}
bool KRRenderValue::isLong() const {
    return kuikly::util::json::TagOf(value_) == kuikly::util::json::kTagNumber &&
           kuikly::util::json::KindOfNumber(value_) == kuikly::util::json::kNumInt64;
}
bool KRRenderValue::isFloat() const {
    return type() == KRJSON_FLOAT;
}
bool KRRenderValue::isDouble() const {
    return type() == KRJSON_DOUBLE;
}
bool KRRenderValue::isString() const {
    return type() == KRJSON_STRING || type() == KRJSON_U16STRING;
}
bool KRRenderValue::isMap() const {
    return type() == KRJSON_OBJECT;
}
bool KRRenderValue::isArray() const {
    return type() == KRJSON_ARRAY;
}
bool KRRenderValue::isByteArray() const {
    return type() == KRJSON_BYTES;
}
bool KRRenderValue::isNapiValue() const {
    return kuikly::util::json::TagOf(value_) == kuikly::util::json::kTagExt;
}

KRRenderValue KRRenderValue::Parsed() const {
    return isString() ? parsedFromJsonText() : *this;
}

KRRenderValue KRRenderValue::opt(std::nullptr_t) const {
    return KRRenderValue();
}

KRRenderValue KRRenderValue::opt(const std::string &key) const {
    if (!isMap()) {
        return KRRenderValue();
    }
    return ChildOrEmpty(kuikly::util::json::ObjectGet(value_, key.data(), key.size()));
}

KRRenderValue KRRenderValue::opt(const std::u16string &key) const {
    if (!isMap()) {
        return KRRenderValue();
    }
    return ChildOrEmpty(kuikly::util::json::ObjectGetUTF16(
        value_, reinterpret_cast<const uint16_t *>(key.data()), key.size()));
}

KRRenderValue KRRenderValue::opt(const char *key) const {
    return key == nullptr ? KRRenderValue() : opt(std::string(key));
}

KRRenderValue KRRenderValue::opt(const char16_t *key) const {
    return key == nullptr ? KRRenderValue() : opt(std::u16string(key));
}

KRRenderValue KRRenderValue::opt(const uint16_t *key) const {
    return key == nullptr ? KRRenderValue()
                          : opt(std::u16string(reinterpret_cast<const char16_t *>(key)));
}

KRRenderValue KRRenderValue::at(size_t index) const {
    if (!isArray()) {
        return KRRenderValue();
    }
    return ChildOrEmpty(kuikly::util::json::ArrayGet(value_, index));
}

size_t KRRenderValue::size() const {
    return kuikly::util::json::GetSize(value_);
}

KRRenderValue KRRenderValue::Parse(const std::string &json) {
    return MakeParsed(json);
}

bool KRRenderValue::toBool() const {
    if (isBool()) {
        return kuikly::util::json::GetBool(value_, false);
    }
    return toDouble() != 0.0;
}

int32_t KRRenderValue::toInt() const {
    return static_cast<int32_t>(toLong());
}

int64_t KRRenderValue::toLong() const {
    if (isString()) {
        try {
            return std::stoll(toAsciiString());
        } catch (...) {
            return 0;
        }
    }
    if (isBool()) {
        return kuikly::util::json::GetBool(value_, false) ? 1 : 0;
    }
    return kuikly::util::json::GetInt(value_, 0);
}

float KRRenderValue::toFloat() const {
    float result = static_cast<float>(toDouble());
    return std::isnan(result) ? 0.0f : result;
}

double KRRenderValue::toDouble() const {
    if (isString()) {
        try {
            const auto str = toAsciiString();
            return str.empty() ? 0.0 : std::stod(str);
        } catch (...) {
            return 0.0;
        }
    }
    if (isBool()) {
        return toBool() ? 1.0 : 0.0;
    }
    return kuikly::util::json::GetDouble(value_, 0.0);
}

std::u16string KRRenderValue::toU16String() const {
    if (type() == KRJSON_U16STRING) {
        size_t units = 0;
        const uint16_t *utf16 = kuikly::util::json::GetStringUTF16(value_, &units);
        return utf16 == nullptr ? std::u16string()
                                : std::u16string(reinterpret_cast<const char16_t *>(utf16), units);
    }
    if (type() == KRJSON_STRING) {
        size_t size = 0;
        const char *data = kuikly::util::json::GetString(value_, &size);
        return data == nullptr ? std::u16string() : kuikly::util::json::UTF8ToUTF16(data, size);
    }
    const std::string utf8 = toString();
    return kuikly::util::json::UTF8ToUTF16(utf8.data(), utf8.size());
}

std::pair<const uint16_t *, size_t> KRRenderValue::utf16View() const {
    if (type() != KRJSON_U16STRING) {
        return {nullptr, 0};
    }
    size_t units = 0;
    const uint16_t *utf16 = kuikly::util::json::GetStringUTF16(value_, &units);
    return {utf16, units};
}

bool KRRenderValue::equalsAscii(const char *lit) const {
    if (lit == nullptr || !isString()) {
        return false;
    }
    const size_t lit_len = std::strlen(lit);
    if (type() == KRJSON_U16STRING) {
        size_t units = 0;
        const uint16_t *utf16 = kuikly::util::json::GetStringUTF16(value_, &units);
        if (utf16 == nullptr || units != lit_len) {
            return false;
        }
        for (size_t i = 0; i < units; ++i) {
            if (utf16[i] != static_cast<unsigned char>(lit[i])) {
                return false;
            }
        }
        return true;
    }
    size_t size = 0;
    const char *data = kuikly::util::json::GetString(value_, &size);
    return data != nullptr && size == lit_len && std::memcmp(data, lit, lit_len) == 0;
}

bool KRRenderValue::stringEquals(const KRRenderValue &other) const {
    return KRJSONStringEquals(value_, other.value_);
}

std::string KRRenderValue::toAsciiString() const {
    if (type() == KRJSON_U16STRING) {
        size_t units = 0;
        const uint16_t *utf16 = kuikly::util::json::GetStringUTF16(value_, &units);
        if (utf16 == nullptr) {
            return {};
        }
        for (size_t i = 0; i < units; ++i) {
            if (utf16[i] >= 0x80) {
                return stringValue();
            }
        }
        std::string out(units, '\0');
        for (size_t i = 0; i < units; ++i) {
            out[i] = static_cast<char>(utf16[i]);
        }
        return out;
    }
    if (type() == KRJSON_STRING) {
        size_t size = 0;
        const char *data = kuikly::util::json::GetString(value_, &size);
        return data == nullptr ? std::string() : std::string(data, size);
    }
    return toString();
}

std::string KRRenderValue::toString() const {
    if (isString()) {
        return stringValue();
    }
    if (isBool()) {
        return toBool() ? "1" : "0";
    }
    if (isInt() || isLong() || type() == KRJSON_INT) {
        return std::to_string(toLong());
    }
    if (type() == KRJSON_UINT) {
        return std::to_string(kuikly::util::json::GetUint(value_, 0));
    }
    if (isFloat() || isDouble()) {
        std::string result(32, '\0');
        for (;;) {
            // general = shortest round-trip form (scientific when shorter),
            // matching Dump()/RapidJSON and the Kotlin tokenizer's toDouble()
            // fallthrough. fixed would expand large-magnitude values past any
            // cap and yield an empty string (silent data loss).
            auto conversion = std::to_chars(result.data(), result.data() + result.size(), toDouble(),
                                            std::chars_format::general);
            if (conversion.ec == std::errc()) {
                result.resize(static_cast<size_t>(conversion.ptr - result.data()));
                return result;
            }
            if (conversion.ec != std::errc::value_too_large || result.size() > 128) {
                return std::string();
            }
            result.resize(result.size() * 2);
        }
    }
    if (isMap() || isArray()) {
        return kuikly::util::json::Dump(value_);
    }
    return std::string();
}

KRRenderValue::Map KRRenderValue::toMap() const {
    if (isString()) {
        return parsedFromJsonText()->toMap();
    }
    Map result;
    if (!isMap()) {
        return result;
    }
    const size_t count = kuikly::util::json::GetSize(value_);
    result.reserve(count);
    for (size_t i = 0; i < count; ++i) {
        const KRJSON child = kuikly::util::json::ObjectValueAt(value_, i);
        if (child == KRJSON_INVALID) {
            continue;
        }
        size_t units = 0;
        const uint16_t *key16 = kuikly::util::json::ObjectKeyAtUTF16(value_, i, &units);
        if (key16 != nullptr) {
            result.emplace(std::u16string(reinterpret_cast<const char16_t *>(key16), units),
                           MakeBorrowed(child));
        }
    }
    return result;
}

KRRenderValue::Array KRRenderValue::toArray() const {
    if (isString()) {
        return parsedFromJsonText()->toArray();
    }
    Array result;
    if (!isArray()) {
        return result;
    }
    const size_t count = kuikly::util::json::GetSize(value_);
    result.reserve(count);
    for (size_t i = 0; i < count; ++i) {
        const KRJSON child = kuikly::util::json::ArrayGet(value_, i);
        if (child != KRJSON_INVALID) {
            result.emplace_back(MakeBorrowed(child));
        }
    }
    return result;
}

KRRenderValue::ByteArray KRRenderValue::toByteArray() const {
    auto result = std::make_shared<std::vector<uint8_t>>();
    size_t size = 0;
    const uint8_t *bytes = kuikly::util::json::GetBytes(value_, &size);
    if (bytes != nullptr && size > 0) {
        result->assign(bytes, bytes + size);
    }
    return result;
}

KRJSON KRRenderValue::toCValue() const {
    return !static_cast<bool>(*this) || isNapiValue() ? kuikly::util::json::NewNull() : value_;
}

KRRenderValue KRRenderValue::Accessor::Adopt(KRJSON owned) {
    return MakeOwned(owned);
}

KRJSONType KRRenderValue::Accessor::Type(const KRRenderValue &value) {
    return value.type();
}

KRJSON KRRenderValue::Build() {
    return kuikly::util::json::NewNull();
}
KRJSON KRRenderValue::Build(std::nullptr_t) {
    return Build();
}
KRJSON KRRenderValue::Build(bool value) {
    return kuikly::util::json::NewBool(value);
}
KRJSON KRRenderValue::Build(int32_t value) {
    return kuikly::util::json::NewInt32(value);
}
KRJSON KRRenderValue::Build(int64_t value) {
    return kuikly::util::json::NewInt64(value);
}
KRJSON KRRenderValue::Build(float value) {
    return kuikly::util::json::NewFloat(value);
}
KRJSON KRRenderValue::Build(double value) {
    return kuikly::util::json::NewDouble(value);
}
KRJSON KRRenderValue::Build(const std::string &value) {
    return kuikly::util::json::NewString(value.data(), value.size());
}
KRJSON KRRenderValue::Build(const char *value) {
    return value == nullptr ? Build() : kuikly::util::json::NewString(value, std::char_traits<char>::length(value));
}
KRJSON KRRenderValue::Build(const std::u16string &value) {
    return kuikly::util::json::NewStringUTF16(reinterpret_cast<const uint16_t *>(value.data()), value.size());
}
KRJSON KRRenderValue::Build(const char16_t *value) {
    return value == nullptr
               ? Build()
               : kuikly::util::json::NewStringUTF16(
                     reinterpret_cast<const uint16_t *>(value), std::char_traits<char16_t>::length(value));
}
KRJSON KRRenderValue::BuildUTF16FromUTF8(const char *s, size_t n) {
    const std::u16string u16 = kuikly::util::json::UTF8ToUTF16(s, n);
    return kuikly::util::json::NewStringUTF16(reinterpret_cast<const uint16_t *>(u16.data()), u16.size());
}
KRJSON KRRenderValue::Build(const ByteArray &value) {
    if (!value || value->empty()) {
        return kuikly::util::json::NewBytes(nullptr, 0);
    }
    return kuikly::util::json::NewBytes(value->data(), value->size());
}
namespace {
// C++-built containers used to reach Kotlin as cJSON text, where 5.0 printed as "5" and parsed
// back as Int, and any float as Double. Keep that number shape for container elements.
KRJSON ContainerElement(KRJSON child) {
    const KRJSONType type = kuikly::util::json::GetType(child);
    if (type == KRJSON_DOUBLE || type == KRJSON_FLOAT) {
        return kuikly::util::json::NewIntIfSafeIntegral(kuikly::util::json::GetDouble(child, 0.0));
    }
    return kuikly::util::json::Retain(child);
}
}  // namespace

KRJSON KRRenderValue::Build(const Map &value) {
    KRJSON object = kuikly::util::json::NewObjectUTF16();
    for (const auto &entry : value) {
        const KRJSON child = ContainerElement(entry.second ? entry.second->value_ : kuikly::util::json::NewNull());
        kuikly::util::json::ObjectAppendUTF16NoDedup(object, reinterpret_cast<const uint16_t *>(entry.first.data()),
                                           entry.first.size(), child);
        kuikly::util::json::Release(child);
    }
    return object;
}
KRJSON KRRenderValue::Build(const Array &value) {
    KRJSON array = kuikly::util::json::NewArray();
    for (const auto &entry : value) {
        const KRJSON child = ContainerElement(entry ? entry->value_ : kuikly::util::json::NewNull());
        kuikly::util::json::ArrayAppend(array, child);
        kuikly::util::json::Release(child);
    }
    return array;
}
KRRenderValue KRRenderValue::ChildOrEmpty(KRJSON child) {
    return child == KRJSON_INVALID ? KRRenderValue() : MakeBorrowed(child);
}

KRRenderValue KRRenderValue::MakeParsed(const char *data, size_t length) {
    std::string error;
    KRJSON parsed = kuikly::util::json::Reader::Parse(data, length, &error);
    return parsed == KRJSON_INVALID ? MakeNull() : MakeOwned(parsed);
}

KRRenderValue KRRenderValue::MakeParsed(const std::string &json) {
    return MakeParsed(json.data(), json.size());
}

KRRenderValue KRRenderValue::MakeParsedUTF16(const uint16_t *data, size_t units) {
    std::string error;
    KRJSON parsed = kuikly::util::json::Reader::ParseUTF16(data, units, &error);
    return parsed == KRJSON_INVALID ? MakeNull() : MakeOwned(parsed);
}

KRRenderValue KRRenderValue::parsedFromJsonText() const {
    if (type() == KRJSON_U16STRING) {
        size_t units = 0;
        const uint16_t *utf16 = kuikly::util::json::GetStringUTF16(value_, &units);
        return MakeParsedUTF16(utf16, units);
    }
    size_t size = 0;
    const char *data = kuikly::util::json::GetString(value_, &size);
    return MakeParsed(data, size);
}

std::string KRRenderValue::stringValue() const {
    if (type() == KRJSON_U16STRING) {
        size_t units = 0;
        const uint16_t *utf16 = kuikly::util::json::GetStringUTF16(value_, &units);
        return utf16 == nullptr ? std::string() : kuikly::util::json::UTF16ToUTF8(utf16, units);
    }
    size_t size = 0;
    const char *data = kuikly::util::json::GetString(value_, &size);
    return std::string(data, size);
}

KRJSONType KRRenderValue::type() const {
    return kuikly::util::json::GetType(value_);
}
