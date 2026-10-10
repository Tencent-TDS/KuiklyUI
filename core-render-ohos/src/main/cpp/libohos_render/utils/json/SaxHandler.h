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

#ifndef CORE_RENDER_OHOS_JSON_SAXHANDLER_H
#define CORE_RENDER_OHOS_JSON_SAXHANDLER_H

#include <cstddef>
#include <cstdint>

namespace kuikly {
namespace util {
namespace json {

/**
 * Runtime-polymorphic SAX event sink.
 *
 * `Reader::ParseSax` drives an instance of this interface, emitting one
 * callback per JSON token as the input is scanned. Subclass it to consume JSON
 * without materializing the whole document (true streaming). Every callback
 * returns bool: return false to abort parsing immediately.
 *
 * This header intentionally does not depend on RapidJSON so that consumers stay
 * decoupled from the underlying engine.
 */
class SaxHandler {
 public:
    virtual ~SaxHandler() = default;

    virtual bool OnNull() = 0;
    virtual bool OnBool(bool value) = 0;
    virtual bool OnInt(int64_t value) = 0;
    virtual bool OnUint(uint64_t value) = 0;
    virtual bool OnDouble(double value) = 0;
    /** A string value. `copy` mirrors RapidJSON's flag: false means `data`
     *  points into the source buffer and is only valid during the call. */
    virtual bool OnString(const char *data, size_t length, bool copy) = 0;

    virtual bool OnStartObject() = 0;
    /** An object member key (always followed by exactly one value event). */
    virtual bool OnKey(const char *data, size_t length, bool copy) = 0;
    virtual bool OnEndObject(size_t member_count) = 0;

    virtual bool OnStartArray() = 0;
    virtual bool OnEndArray(size_t element_count) = 0;
};

}  // namespace json
}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_JSON_SAXHANDLER_H
