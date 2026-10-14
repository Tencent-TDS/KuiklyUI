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

#include "libohos_render/utils/json/Reader.h"

#include "libohos_render/utils/json/DomBuilder.h"
#include "libohos_render/utils/json/Box.h"
#include "rapidjson/encodings.h"
#include "rapidjson/error/en.h"
#include "rapidjson/memorystream.h"
#include "rapidjson/reader.h"

namespace kuikly {
namespace util {
namespace json {

namespace {
/**
 * Bridges RapidJSON's compile-time (static) handler concept onto our
 * runtime-polymorphic `SaxHandler`. 32-bit signed/unsigned variants widen
 * to the 64-bit callbacks; `RawNumber` (unused here) maps to a string.
 */
struct SaxAdapter {
    explicit SaxAdapter(SaxHandler &handler) : handler_(handler) {}

    bool Null() { return handler_.OnNull(); }
    bool Bool(bool b) { return handler_.OnBool(b); }
    bool Int(int i) { return handler_.OnInt(i); }
    bool Uint(unsigned u) { return handler_.OnUint(u); }
    bool Int64(int64_t i) { return handler_.OnInt(i); }
    bool Uint64(uint64_t u) { return handler_.OnUint(u); }
    bool Double(double d) { return handler_.OnDouble(d); }
    bool RawNumber(const char *str, rapidjson::SizeType len, bool copy) { return handler_.OnString(str, len, copy); }
    bool String(const char *str, rapidjson::SizeType len, bool copy) { return handler_.OnString(str, len, copy); }
    bool StartObject() { return handler_.OnStartObject(); }
    bool Key(const char *str, rapidjson::SizeType len, bool copy) { return handler_.OnKey(str, len, copy); }
    bool EndObject(rapidjson::SizeType member_count) { return handler_.OnEndObject(member_count); }
    bool StartArray() { return handler_.OnStartArray(); }
    bool EndArray(rapidjson::SizeType element_count) { return handler_.OnEndArray(element_count); }

 private:
    SaxHandler &handler_;
};

/**
 * Length-bounded UTF-16 JSON source. RapidJSON's MemoryStream is byte-oriented;
 * GenericStringStream requires a terminator. This matches MemoryStream's EOF=0
 * contract on `char16_t` units.
 */
struct UTF16MemoryStream {
    using Ch = char16_t;

    UTF16MemoryStream(const uint16_t *src, size_t units)
        : src_(reinterpret_cast<const Ch *>(src)),
          begin_(reinterpret_cast<const Ch *>(src)),
          end_(reinterpret_cast<const Ch *>(src) + units) {}

    Ch Peek() const { return src_ == end_ ? static_cast<Ch>(0) : *src_; }
    Ch Take() { return src_ == end_ ? static_cast<Ch>(0) : *src_++; }
    size_t Tell() const { return static_cast<size_t>(src_ - begin_); }
    Ch *PutBegin() { return nullptr; }
    void Put(Ch) {}
    void Flush() {}
    size_t PutEnd(Ch *) { return 0; }

 private:
    const Ch *src_;
    const Ch *begin_;
    const Ch *end_;
};

struct UTF16DomAdapter {
    explicit UTF16DomAdapter(DomBuilder &builder) : builder_(builder) {}

    bool Null() { return builder_.OnNull(); }
    bool Bool(bool b) { return builder_.OnBool(b); }
    bool Int(int i) { return builder_.OnInt(i); }
    bool Uint(unsigned u) { return builder_.OnUint(u); }
    bool Int64(int64_t i) { return builder_.OnInt(i); }
    bool Uint64(uint64_t u) { return builder_.OnUint(u); }
    bool Double(double d) { return builder_.OnDouble(d); }
    bool RawNumber(const char16_t *str, rapidjson::SizeType len, bool) {
        return builder_.OnStringUTF16(reinterpret_cast<const uint16_t *>(str), len);
    }
    bool String(const char16_t *str, rapidjson::SizeType len, bool) {
        return builder_.OnStringUTF16(reinterpret_cast<const uint16_t *>(str), len);
    }
    bool StartObject() { return builder_.OnStartObject(); }
    bool Key(const char16_t *str, rapidjson::SizeType len, bool) {
        return builder_.OnKeyUTF16(reinterpret_cast<const uint16_t *>(str), len);
    }
    bool EndObject(rapidjson::SizeType member_count) { return builder_.OnEndObject(member_count); }
    bool StartArray() { return builder_.OnStartArray(); }
    bool EndArray(rapidjson::SizeType element_count) { return builder_.OnEndArray(element_count); }

 private:
    DomBuilder &builder_;
};
}  // namespace

bool Reader::ParseSax(const char *data, size_t length, SaxHandler &handler, std::string *error) {
    if (data == nullptr) {
        if (error != nullptr) {
            *error = "null input";
        }
        return false;
    }
    // MemoryStream reads exactly `length` bytes and yields '\0' past the end,
    // so a non-NUL-terminated caller buffer is never over-read.
    //
    // Deliberate: no kParseFullPrecisionFlag. Default (normal) precision uses
    // RapidJSON's fast strtod, which can differ from the correctly-rounded
    // double by up to 1 ULP on rare hard decimal inputs. This is a speed/size
    // tradeoff for this perf-sensitive render path; the structure and all
    // exactly-representable numbers still round-trip. Flip the flag here (and in
    // ParseUTF16) if a caller ever needs guaranteed correct rounding.
    rapidjson::Reader reader;
    rapidjson::MemoryStream stream(data, length);
    SaxAdapter adapter(handler);
    rapidjson::ParseResult result = reader.Parse(stream, adapter);
    if (result.IsError()) {
        if (error != nullptr) {
            *error = std::string(rapidjson::GetParseError_En(result.Code())) + " (offset " +
                     std::to_string(result.Offset()) + ")";
        }
        return false;
    }
    return true;
}

KRJSON Reader::Parse(const char *data, size_t length, std::string *error) {
    DomBuilder builder;
    if (!ParseSax(data, length, builder, error)) {
        return KRJSON_INVALID;
    }
    return builder.TakeResult();
}

KRJSON Reader::ParseUTF16(const uint16_t *data, size_t units, std::string *error) {
    if (data == nullptr) {
        if (error != nullptr) {
            *error = "null input";
        }
        return KRJSON_INVALID;
    }
    using Encoding = rapidjson::UTF16<char16_t>;
    // Default (normal) precision on purpose — see the note in ParseSax.
    rapidjson::GenericReader<Encoding, Encoding> reader;
    UTF16MemoryStream stream(data, units);
    DomBuilder builder;
    builder.SetUTF16Mode(true);
    UTF16DomAdapter adapter(builder);
    rapidjson::ParseResult result = reader.Parse(stream, adapter);
    if (result.IsError()) {
        if (error != nullptr) {
            *error = std::string(rapidjson::GetParseError_En(result.Code())) + " (offset " +
                     std::to_string(result.Offset()) + ")";
        }
        return KRJSON_INVALID;
    }
    return builder.TakeResult();
}

}  // namespace json
}  // namespace util
}  // namespace kuikly
