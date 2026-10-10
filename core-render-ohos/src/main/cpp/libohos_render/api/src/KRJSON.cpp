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

// Thin C-ABI surface over pointer-low-bit tagged KRJSON. External
// consumers (including Kotlin/Native via cinterop) use only these functions;
// the reader/builder/tests call the kuikly::util::json:: functions directly.

#include "libohos_render/api/include/Kuikly/KRJSON.h"

#include <cstdlib>
#include <cstring>
#include <string>

#include "libohos_render/utils/json/Reader.h"
#include "libohos_render/utils/json/Box.h"

namespace kjson = kuikly::util::json;

static void SetParseError(char **err, const std::string &message) {
    if (err == nullptr) {
        return;
    }
    *err = nullptr;
    if (message.empty()) {
        return;
    }
    char *buf = static_cast<char *>(std::malloc(message.size() + 1));
    if (buf != nullptr) {
        std::memcpy(buf, message.c_str(), message.size() + 1);
    }
    *err = buf;
}

extern "C" {

KRJSON KRJSONRetain(KRJSON value) {
    return kjson::Retain(value);
}
void KRJSONRelease(KRJSON value) {
    kjson::Release(value);
}

KRJSON KRJSONParse(const char *data, size_t len, char **err) {
    std::string message;
    KRJSON value = kjson::Reader::Parse(data, len, &message);
    if (value == KRJSON_INVALID) {
        SetParseError(err, message);
    } else if (err != nullptr) {
        *err = nullptr;
    }
    return value;
}

KRJSON KRJSONParseUTF16(const uint16_t *data, size_t unit_count, char **err) {
    std::string message;
    KRJSON value = kjson::Reader::ParseUTF16(data, unit_count, &message);
    if (value == KRJSON_INVALID) {
        SetParseError(err, message);
    } else if (err != nullptr) {
        *err = nullptr;
    }
    return value;
}

char *KRJSONDump(KRJSON value) {
    std::string s = kjson::Dump(value);
    char *buf = static_cast<char *>(std::malloc(s.size() + 1));
    if (buf != nullptr) {
        std::memcpy(buf, s.c_str(), s.size() + 1);
    }
    return buf;
}

uint16_t *KRJSONDumpUTF16(KRJSON value, size_t *out_units) {
    const std::u16string s = kjson::DumpUTF16(value);
    const size_t n = s.size();
    if (out_units != nullptr) {
        *out_units = n;
    }
    auto *buf = static_cast<uint16_t *>(std::malloc((n + 1) * sizeof(uint16_t)));
    if (buf != nullptr) {
        std::memcpy(buf, s.data(), n * sizeof(uint16_t));
        buf[n] = 0;
    }
    return buf;
}
void KRJSONFreeString(char *str) {
    std::free(str);
}

KRJSONType KRJSONGetType(KRJSON value) {
    return kjson::GetType(value);
}
bool KRJSONGetBool(KRJSON value, bool default_value) {
    return kjson::GetBool(value, default_value);
}
int64_t KRJSONGetInt(KRJSON value, int64_t default_value) {
    return kjson::GetInt(value, default_value);
}
uint64_t KRJSONGetUint(KRJSON value, uint64_t default_value) {
    return kjson::GetUint(value, default_value);
}
double KRJSONGetDouble(KRJSON value, double default_value) {
    return kjson::GetDouble(value, default_value);
}
const char *KRJSONGetString(KRJSON value, size_t *out_len) {
    return kjson::GetString(value, out_len);
}
const uint16_t *KRJSONGetStringUTF16(KRJSON value, size_t *out_units) {
    return kjson::GetStringUTF16(value, out_units);
}
const uint8_t *KRJSONGetBytes(KRJSON value, size_t *out_len) {
    return kjson::GetBytes(value, out_len);
}

size_t KRJSONGetSize(KRJSON value) {
    return kjson::GetSize(value);
}
KRJSON KRJSONArrayGet(KRJSON array, size_t index) {
    return kjson::ArrayGet(array, index);
}
KRJSON KRJSONObjectGet(KRJSON object, const char *key) {
    return kjson::ObjectGet(object, key, key != nullptr ? std::strlen(key) : 0);
}
KRJSON KRJSONObjectGetUTF16(KRJSON object, const uint16_t *key, size_t units) {
    return kjson::ObjectGetUTF16(object, key, units);
}
bool KRJSONObjectKeysAreUTF16(KRJSON object) {
    return kjson::ObjectKeysAreUTF16(object);
}
KRJSON KRJSONObjectValueAt(KRJSON object, size_t index) {
    return kjson::ObjectValueAt(object, index);
}
const char *KRJSONObjectKeyAt(KRJSON object, size_t index) {
    return kjson::ObjectKeyAt(object, index);
}
const uint16_t *KRJSONObjectKeyAtUTF16(KRJSON object, size_t index, size_t *out_units) {
    return kjson::ObjectKeyAtUTF16(object, index, out_units);
}
void KRJSONObjectForEach(KRJSON object, KRJSONObjectVisitor visitor, void *userdata) {
    kjson::ObjectForEach(object, visitor, userdata);
}

KRJSON KRJSONNewNull(void) {
    return kjson::NewNull();
}
KRJSON KRJSONNewBool(bool v) {
    return kjson::NewBool(v);
}
KRJSON KRJSONNewInt32(int32_t v) {
    return kjson::NewInt32(v);
}
KRJSON KRJSONNewInt(int64_t v) {
    return kjson::NewInt(v);
}
KRJSON KRJSONNewInt64(int64_t v) {
    return kjson::NewInt64(v);
}
KRJSON KRJSONNewUint(uint64_t v) {
    return kjson::NewUint(v);
}
KRJSON KRJSONNewDouble(double v) {
    return kjson::NewDouble(v);
}
KRJSON KRJSONNewFloat(float v) {
    return kjson::NewFloat(v);
}
KRJSON KRJSONNewString(const char *data, size_t len) {
    return kjson::NewString(data, len);
}
KRJSON KRJSONNewStringUTF16(const uint16_t *data, size_t unit_count) {
    return kjson::NewStringUTF16(data, unit_count);
}
KRJSON KRJSONNewBytes(const uint8_t *data, size_t len) {
    return kjson::NewBytes(data, len);
}
KRJSON KRJSONNewArray(void) {
    return kjson::NewArray();
}
KRJSON KRJSONNewObject(void) {
    return kjson::NewObject();
}
KRJSON KRJSONNewObjectUTF16(void) {
    return kjson::NewObjectUTF16();
}
void KRJSONArrayAppend(KRJSON array, KRJSON child) {
    kjson::ArrayAppend(array, child);
}
void KRJSONObjectPut(KRJSON object, const char *key, size_t key_len, KRJSON child) {
    kjson::ObjectPut(object, key, key_len, child);
}
void KRJSONObjectPutUTF16(KRJSON object, const uint16_t *key, size_t units, KRJSON child) {
    kjson::ObjectPutUTF16(object, key, units, child);
}
void KRJSONObjectAppendUTF16NoDedup(KRJSON object, const uint16_t *key, size_t units, KRJSON child) {
    kjson::ObjectAppendUTF16NoDedup(object, key, units, child);
}

bool KRJSONEquals(KRJSON a, KRJSON b) {
    return kjson::Equals(a, b);
}
bool KRJSONStringEquals(KRJSON a, KRJSON b) {
    return kjson::StringEquals(a, b);
}

}  // extern "C"
