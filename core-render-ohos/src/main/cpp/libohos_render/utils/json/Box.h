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

#ifndef CORE_RENDER_OHOS_JSON_BOX_H
#define CORE_RENDER_OHOS_JSON_BOX_H

// Pointer-low-bit tagged KRJSON (see /KRJSON-pointer-tagging.md).
// Public C ABI: libohos_render/api/include/Kuikly/KRJSON.h
//
//   heap:   word = aligned_ptr | tag     (tag in bits[0..2])
//   null:   word == 0
//   bool:   tag 1, true/false in bit 3
//   number: tag 2; bit 3 = immediate; NumberBox is 16-byte aligned

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <string>
#include <type_traits>
#include <utility>
#include <vector>

#include "libohos_render/api/include/Kuikly/KRJSON.h"
#include "libohos_render/utils/json/Allocator.h"

namespace kuikly {
namespace util {
namespace json {

static_assert(sizeof(void *) == 8, "KRJSON tagging assumes 64-bit pointers");

// Active heap allocator for all HeapBox allocations. Swap this alias to plug in
// another type that implements Allocate/Deallocate (see Allocator.h).
using HeapAllocator = DefaultHeapAllocator;

enum : uint8_t {
    kTagNull = 0,    // immediate; only the all-zero word
    kTagBool = 1,    // immediate
    kTagNumber = 2,  // immediate or 16-aligned NumberBox
    kTagString = 3,  // heap StringBox (UTF-8 or UTF-16)
    kTagArray = 4,
    kTagObject = 5,
    kTagBytes = 6,
    kTagExt = 7,  // OpaqueBox (NAPI). GetType = KRJSON_NULL
};

constexpr uint64_t kNumberImmBit = 1ull << 3;

enum NumberKind : uint8_t {
    kNumInt32 = 1,
    kNumInt64 = 2,
    kNumUint32 = 3,
    kNumUint64 = 4,
    kNumFloat = 5,
    kNumDouble = 6,
};

constexpr uint8_t kStrUTF8 = 0;
constexpr uint8_t kStrUTF16 = 1;

// Boxes that the tagged pointer refers to are C-style prefixes: rc at offset 0,
// no inheritance (a base + extra fields is not standard-layout / POD).
struct HeapBox {
    std::atomic<int32_t> rc;
};

struct alignas(16) NumberBox {
    std::atomic<int32_t> rc;
    uint64_t bits;
    NumberKind kind;
};

struct StringBox {
    std::atomic<int32_t> rc;
    uint32_t len;
    uint8_t encoding;
    const char *data() const { return reinterpret_cast<const char *>(this + 1); }
    const uint16_t *u16data() const { return reinterpret_cast<const uint16_t *>(this + 1); }
    uint16_t *u16data() { return reinterpret_cast<uint16_t *>(this + 1); }
    static StringBox *Create(const char *s, size_t n);
    static StringBox *CreateUTF16(const uint16_t *s, size_t n);
    static void Free(StringBox *b);
};

struct BytesBox {
    std::atomic<int32_t> rc;
    uint32_t len;
    const uint8_t *data() const { return reinterpret_cast<const uint8_t *>(this + 1); }
    uint8_t *data() { return reinterpret_cast<uint8_t *>(this + 1); }
    static BytesBox *Create(const uint8_t *s, size_t n);
    static void Free(BytesBox *b);
};

struct ArrayBox {
    std::atomic<int32_t> rc;
    std::vector<KRJSON> items;
    ArrayBox() : rc{1} {}
    ~ArrayBox();
};

struct ObjectBox {
    std::atomic<int32_t> rc;
    using UTF16Members = std::vector<std::pair<std::u16string, KRJSON>>;
    UTF16Members members;
    ObjectBox() : rc{1} {}
    ObjectBox(const ObjectBox &) = delete;
    ObjectBox &operator=(const ObjectBox &) = delete;
    ~ObjectBox();
};

struct OpaqueBox {
    std::atomic<int32_t> rc;
    const void *a;
    const void *b;
};

template <typename T>
constexpr bool kIsPodBox = std::is_pod<T>::value && std::is_standard_layout<T>::value &&
                           std::is_trivial<T>::value;

static_assert(kIsPodBox<HeapBox>, "HeapBox must be POD");
static_assert(sizeof(HeapBox) == 4 && alignof(HeapBox) == 4);
static_assert(offsetof(HeapBox, rc) == 0);

static_assert(kIsPodBox<NumberBox>, "NumberBox must be POD");
static_assert(alignof(NumberBox) == 16);
static_assert(sizeof(NumberBox) == 32);
static_assert(offsetof(NumberBox, rc) == 0);
static_assert(offsetof(NumberBox, bits) == 8);
static_assert(offsetof(NumberBox, kind) == 16);
static_assert(sizeof(NumberKind) == 1);

static_assert(kIsPodBox<StringBox>, "StringBox must be POD");
static_assert(sizeof(StringBox) == 12 && alignof(StringBox) == 4);
static_assert(offsetof(StringBox, rc) == 0);
static_assert(offsetof(StringBox, len) == 4);
static_assert(offsetof(StringBox, encoding) == 8);
static_assert(sizeof(StringBox) % alignof(uint16_t) == 0,
              "UTF-16 payload follows StringBox and must be 2-byte aligned");

static_assert(kIsPodBox<BytesBox>, "BytesBox must be POD");
static_assert(sizeof(BytesBox) == 8 && alignof(BytesBox) == 4);
static_assert(offsetof(BytesBox, rc) == 0);
static_assert(offsetof(BytesBox, len) == 4);

static_assert(kIsPodBox<OpaqueBox>, "OpaqueBox must be POD");
static_assert(sizeof(OpaqueBox) == 24 && alignof(OpaqueBox) == 8);
static_assert(offsetof(OpaqueBox, rc) == 0);
static_assert(offsetof(OpaqueBox, a) == 8);
static_assert(offsetof(OpaqueBox, b) == 16);

template <typename T>
T *BoxOf(HeapBox *b) {
    return reinterpret_cast<T *>(b);
}
template <typename T>
const T *BoxOf(const HeapBox *b) {
    return reinterpret_cast<const T *>(b);
}

inline uint8_t TagOf(KRJSON v) {
    return static_cast<uint8_t>(v & 7u);
}

static_assert(KRJSON_NULL == kTagNull);
static_assert(KRJSON_BOOL == kTagBool);
static_assert(KRJSON_STRING == kTagString);
static_assert(KRJSON_ARRAY == kTagArray);
static_assert(KRJSON_OBJECT == kTagObject);
static_assert(KRJSON_BYTES == kTagBytes);

inline bool IsNumberImm(KRJSON v) {
    return TagOf(v) == kTagNumber && (v & kNumberImmBit) != 0;
}

inline bool IsHeap(KRJSON v) {
    // v == 0 is JSON null; KRJSON_INVALID (0x08) has tag 0 too. Both are
    // non-heap without needing this special case, but keep it for clarity.
    if (v == 0 || v == KRJSON_INVALID) {
        return false;
    }
    const uint8_t t = TagOf(v);
    if (t == kTagNumber) {
        return !IsNumberImm(v);
    }
    return t >= kTagString && t <= kTagExt;
}

inline HeapBox *AsBox(KRJSON v) {
    if (!IsHeap(v)) {
        return nullptr;
    }
    const uintptr_t mask = TagOf(v) == kTagNumber ? ~uintptr_t{0xF} : ~uintptr_t{0x7};
    return reinterpret_cast<HeapBox *>(static_cast<uintptr_t>(v) & mask);
}

inline NumberBox *AsNumberBox(KRJSON v) {
    return TagOf(v) == kTagNumber && !IsNumberImm(v) ? BoxOf<NumberBox>(AsBox(v)) : nullptr;
}

inline NumberKind KindOfNumber(KRJSON v) {
    if (TagOf(v) != kTagNumber) {
        return kNumInt32;
    }
    if (IsNumberImm(v)) {
        return static_cast<NumberKind>((v >> 4) & 7u);
    }
    NumberBox *box = AsNumberBox(v);
    return box == nullptr ? kNumInt32 : box->kind;
}

inline bool IsUnique(KRJSON v) {
    HeapBox *box = AsBox(v);
    return box != nullptr && box->rc.load(std::memory_order_acquire) == 1;
}

[[noreturn]] void CrashOnPointerTagViolation(uintptr_t p);

inline KRJSON EncodePtr(const void *p, uint8_t tag) {
    const uintptr_t u = reinterpret_cast<uintptr_t>(p);
    const uintptr_t align_mask = tag == kTagNumber ? uintptr_t{0xF} : uintptr_t{0x7};
    if (__builtin_expect((u & align_mask) != 0 || tag < kTagNumber || tag > kTagExt, 0)) {
        CrashOnPointerTagViolation(u);
    }
    return static_cast<uint64_t>(u) | tag;
}

inline KRJSON EncodeImmNumber(NumberKind kind, uint64_t payload) {
    return (payload << 7) | (static_cast<uint64_t>(kind) << 4) | kTagNumber | kNumberImmBit;
}

inline int64_t ImmNumberSigned(KRJSON v) {
    return static_cast<int64_t>(v) >> 7;
}

KRJSON Retain(KRJSON v);
void Release(KRJSON v);

KRJSON NewNull();
KRJSON NewBool(bool b);
KRJSON NewInt32(int32_t x);
KRJSON NewInt(int64_t x);
KRJSON NewInt64(int64_t x);
KRJSON NewUint(uint64_t x);
KRJSON NewUint32(uint32_t x);
KRJSON NewUint64(uint64_t x);
KRJSON NewFloat(float f);
KRJSON NewDouble(double d);
// JS numbers are doubles; integral values within ±(2^53 - 1) become integers so they dump as
// "5" (like JSON.stringify), anything else stays double.
KRJSON NewIntIfSafeIntegral(double d);
KRJSON NewString(const char *s, size_t n);
KRJSON NewStringUTF16(const uint16_t *s, size_t n);
KRJSON NewBytes(const uint8_t *data, size_t n);
KRJSON NewArray();
KRJSON NewObject();
KRJSON NewObjectUTF16();
KRJSON NewOpaque(const void *a, const void *b);
bool GetOpaque(KRJSON v, const void **a, const void **b);
void ArrayAppend(KRJSON array, KRJSON child);
void ArraySet(KRJSON array, size_t index, KRJSON child);
void ObjectPut(KRJSON object, const char *key, size_t key_len, KRJSON child);
void ObjectPutUTF16(KRJSON object, const uint16_t *key, size_t units, KRJSON child);
void ObjectAppendNoDedup(KRJSON object, const char *key, size_t key_len, KRJSON child);
void ObjectAppendUTF16NoDedup(KRJSON object, const uint16_t *key, size_t units, KRJSON child);
void ObjectDedupLast(KRJSON object);

KRJSONType GetType(KRJSON v);
bool GetBool(KRJSON v, bool default_value);
int64_t GetInt(KRJSON v, int64_t default_value);
uint64_t GetUint(KRJSON v, uint64_t default_value);
double GetDouble(KRJSON v, double default_value);
const char *GetString(KRJSON v, size_t *out_len);
std::string UTF16ToUTF8(const uint16_t *s, size_t n);
std::u16string UTF8ToUTF16(const char *s, size_t n);
const uint16_t *GetStringUTF16(KRJSON v, size_t *out_units);
const uint8_t *GetBytes(KRJSON v, size_t *out_len);
size_t GetSize(KRJSON v);
KRJSON ArrayGet(KRJSON array, size_t index);
KRJSON ObjectGet(KRJSON object, const char *key, size_t key_len);
KRJSON ObjectGetUTF16(KRJSON object, const uint16_t *key, size_t units);
bool ObjectKeysAreUTF16(KRJSON object);
KRJSON ObjectValueAt(KRJSON object, size_t index);
const char *ObjectKeyAt(KRJSON object, size_t index);
const uint16_t *ObjectKeyAtUTF16(KRJSON object, size_t index, size_t *out_units);
void ObjectForEach(KRJSON object, KRJSONObjectVisitor visitor, void *userdata);

std::string Dump(KRJSON v);
std::u16string DumpUTF16(KRJSON v);

bool Equals(KRJSON a, KRJSON b);
bool StringEquals(KRJSON a, KRJSON b);

}  // namespace json
}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_JSON_BOX_H
