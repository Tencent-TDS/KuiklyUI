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

#ifndef CORE_RENDER_OHOS_JSON_DOMBUILDER_H
#define CORE_RENDER_OHOS_JSON_DOMBUILDER_H

#include <cstdint>
#include <string>
#include <vector>

#include "libohos_render/api/include/Kuikly/KRJSON.h"
#include "libohos_render/utils/json/SaxHandler.h"

namespace kuikly {
namespace util {
namespace json {

/**
 * SAX handler that assembles streamed events into a reference-counted
 * `KRJSON` tree (pointer-low-bit tagged words). Nesting is tracked with an
 * explicit stack (no recursion). Ownership is manual: created values are owned;
 * appending/putting retains into the container and drops the builder's own ref.
 * On an abandoned/failed parse the destructor releases everything still held.
 */
class DomBuilder : public SaxHandler {
 public:
    DomBuilder() = default;
    ~DomBuilder() override;

    bool OnNull() override;
    bool OnBool(bool value) override;
    bool OnInt(int64_t value) override;
    bool OnUint(uint64_t value) override;
    bool OnDouble(double value) override;
    bool OnString(const char *data, size_t length, bool copy) override;
    /** UTF-16 JSON string value (`ParseUTF16`); not part of the UTF-8 SaxHandler. */
    bool OnStringUTF16(const uint16_t *data, size_t units);
    bool OnStartObject() override;
    bool OnKey(const char *data, size_t length, bool copy) override;
    /** UTF-16 object key (`ParseUTF16`); not part of the UTF-8 SaxHandler. */
    bool OnKeyUTF16(const uint16_t *data, size_t units);
    bool OnEndObject(size_t member_count) override;
    bool OnStartArray() override;
    bool OnEndArray(size_t element_count) override;

    /** Transfer the parsed root (OWNED) to the caller. KRJSON_INVALID if none. */
    KRJSON TakeResult();
    /** UTF-16 parse: objects store UTF-16 keys and string values. */
    void SetUTF16Mode(bool enabled) { utf16_mode_ = enabled; }

 private:
    struct Frame {
        KRJSON container = KRJSON_INVALID;
        std::string key;
        std::u16string key16;
        bool has_key = false;
        bool key_is_utf16 = false;
    };

    // Bounds tree depth so a parsed-then-serialized document can't overflow the
    // C++ stack in the recursive Dump/WriteTo walk. Exceeding it aborts parsing.
    static constexpr size_t kMaxDepth = 256;

    // Place an OWNED value: into the top container (which retains; we then drop
    // our ref), or as the root. Consumes `owned`.
    bool AddValue(KRJSON owned);
    // Pop the top container and place it into its parent / root.
    bool CloseContainer();

    std::vector<Frame> stack_;
    KRJSON root_ = KRJSON_INVALID;
    bool utf16_mode_ = false;
};

}  // namespace json
}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_JSON_DOMBUILDER_H
