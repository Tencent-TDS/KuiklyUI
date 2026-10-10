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

#include "libohos_render/utils/json/DomBuilder.h"

#include <utility>

#include "libohos_render/utils/json/Box.h"

namespace kuikly {
namespace util {
namespace json {

DomBuilder::~DomBuilder() {
    for (auto &frame : stack_) {
        Release(frame.container);
    }
    if (root_ != KRJSON_INVALID) {
        Release(root_);
    }
}

bool DomBuilder::AddValue(KRJSON owned) {
    if (stack_.empty()) {
        if (root_ != KRJSON_INVALID) {
            Release(root_);  // defensive: more than one top-level value
        }
        root_ = owned;  // transfer ownership to root_
        return true;
    }
    Frame &top = stack_.back();
    if (TagOf(top.container) == kTagArray) {
        ArrayAppend(top.container, owned);  // container retains
        Release(owned);                     // drop our ref
    } else {  // object
        if (!top.has_key) {
            Release(owned);
            return false;  // value without a preceding key
        }
        if (top.key_is_utf16) {
            ObjectAppendUTF16NoDedup(top.container, reinterpret_cast<const uint16_t *>(top.key16.data()),
                                     top.key16.size(), owned);
            top.key16.clear();
        } else {
            ObjectAppendNoDedup(top.container, top.key.data(), top.key.size(), owned);
            top.key.clear();
        }
        Release(owned);
        top.has_key = false;
        top.key_is_utf16 = false;
    }
    return true;
}

bool DomBuilder::CloseContainer() {
    KRJSON container = stack_.back().container;  // owned
    stack_.pop_back();
    return AddValue(container);
}

bool DomBuilder::OnNull() {
    return AddValue(NewNull());
}
bool DomBuilder::OnBool(bool value) {
    return AddValue(NewBool(value));
}
bool DomBuilder::OnInt(int64_t value) {
    return AddValue(NewInt(value));
}
bool DomBuilder::OnUint(uint64_t value) {
    // RapidJSON reports every non-negative integer via Uint/Uint64. Classify by
    // range, not sign, so "5" and "-5" share KRJSON_INT; UINT is only for > INT64_MAX.
    if (value <= static_cast<uint64_t>(INT64_MAX)) {
        return AddValue(NewInt(static_cast<int64_t>(value)));
    }
    return AddValue(NewUint(value));
}
bool DomBuilder::OnDouble(double value) {
    return AddValue(NewDouble(value));
}
bool DomBuilder::OnString(const char *data, size_t length, bool /*copy*/) {
    return AddValue(NewString(data, length));
}
bool DomBuilder::OnStringUTF16(const uint16_t *data, size_t units) {
    return AddValue(NewStringUTF16(data, units));
}

bool DomBuilder::OnStartObject() {
    if (stack_.size() >= kMaxDepth) {
        return false;  // too deep; abort parse
    }
    Frame frame;
    frame.container = NewObject();
    stack_.push_back(std::move(frame));
    return true;
}
bool DomBuilder::OnKey(const char *data, size_t length, bool /*copy*/) {
    if (stack_.empty()) {
        return false;
    }
    Frame &top = stack_.back();
    top.key.assign(data, length);
    top.key16.clear();
    top.has_key = true;
    top.key_is_utf16 = false;
    return true;
}
bool DomBuilder::OnKeyUTF16(const uint16_t *data, size_t units) {
    if (stack_.empty() || data == nullptr) {
        return false;
    }
    Frame &top = stack_.back();
    top.key.clear();
    top.key16.assign(reinterpret_cast<const char16_t *>(data), units);
    top.has_key = true;
    top.key_is_utf16 = true;
    return true;
}
bool DomBuilder::OnEndObject(size_t /*member_count*/) {
    // Members were appended without dedup for O(n) build; collapse any duplicate
    // keys once here (last value wins) before the object is placed in its parent.
    if (!stack_.empty()) {
        ObjectDedupLast(stack_.back().container);
    }
    return CloseContainer();
}

bool DomBuilder::OnStartArray() {
    if (stack_.size() >= kMaxDepth) {
        return false;  // too deep; abort parse
    }
    Frame frame;
    frame.container = NewArray();
    stack_.push_back(std::move(frame));
    return true;
}
bool DomBuilder::OnEndArray(size_t /*element_count*/) {
    return CloseContainer();
}

KRJSON DomBuilder::TakeResult() {
    KRJSON result = root_;
    root_ = KRJSON_INVALID;
    return result;
}

}  // namespace json
}  // namespace util
}  // namespace kuikly
