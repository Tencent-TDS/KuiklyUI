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

#include "libohos_render/utils/json/Box.h"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <limits>
#include <new>
#include <string_view>
#include <unordered_map>
#include <atomic>

#include "rapidjson/encodings.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"

namespace kuikly {
namespace util {
namespace json {

#if defined(KRJSON_LEAK_TEST)
std::atomic<long> g_krjson_live_boxes{0};
extern "C" long KRJSONLiveBoxCount() {
    return g_krjson_live_boxes.load(std::memory_order_relaxed);
}
#endif

// Never-returns: heap pointer is not 8-byte aligned (or 16-byte for NumberBox).
// has been violated, which means every subsequent AsBox() would decode a
// corrupted address. Fail loudly and immediately instead of corrupting memory.
[[noreturn]] void CrashOnPointerTagViolation(uintptr_t p) {
    std::fprintf(stderr,
                 "[KRJSON] fatal: heap pointer %p is not aligned for pointer-low-bit "
                 "tagging (need 8-byte, or 16-byte for NumberBox)\n",
                 reinterpret_cast<void *>(p));
    std::abort();
}

namespace {
constexpr int64_t kInt57Min = -(int64_t{1} << 56);
constexpr int64_t kInt57Max = (int64_t{1} << 56) - 1;

double BitsToDouble(uint64_t bits) {
    double d;
    std::memcpy(&d, &bits, sizeof(d));
    return d;
}
uint64_t DoubleToBits(double d) {
    uint64_t bits;
    std::memcpy(&bits, &d, sizeof(bits));
    return bits;
}
uint32_t FloatToBits(float f) {
    uint32_t bits;
    std::memcpy(&bits, &f, sizeof(bits));
    return bits;
}
float BitsToFloat(uint32_t bits) {
    float f;
    std::memcpy(&f, &bits, sizeof(f));
    return f;
}
bool FitsInt57(int64_t x) {
    return x >= kInt57Min && x <= kInt57Max;
}
KRJSON NewNumberHeap(NumberKind kind, uint64_t bits) {
    auto *box = AllocNew<HeapAllocator, NumberBox>();
    box->rc = 1;
    box->kind = kind;
    box->bits = bits;
    return EncodePtr(box, kTagNumber);
}
void FreeNumberBox(NumberBox *box) {
    AllocDelete<HeapAllocator>(box);
}
bool IsDoubleLosslessToFloat(double d) {
    const float f = static_cast<float>(d);
    return std::isfinite(d) && static_cast<double>(f) == d;
}

void AppendUTF8(std::string &out, uint32_t cp) {
    if (cp < 0x80u) {
        out.push_back(static_cast<char>(cp));
    } else if (cp < 0x800u) {
        out.push_back(static_cast<char>(0xC0u | (cp >> 6)));
        out.push_back(static_cast<char>(0x80u | (cp & 0x3Fu)));
    } else if (cp < 0x10000u) {
        out.push_back(static_cast<char>(0xE0u | (cp >> 12)));
        out.push_back(static_cast<char>(0x80u | ((cp >> 6) & 0x3Fu)));
        out.push_back(static_cast<char>(0x80u | (cp & 0x3Fu)));
    } else {
        out.push_back(static_cast<char>(0xF0u | (cp >> 18)));
        out.push_back(static_cast<char>(0x80u | ((cp >> 12) & 0x3Fu)));
        out.push_back(static_cast<char>(0x80u | ((cp >> 6) & 0x3Fu)));
        out.push_back(static_cast<char>(0x80u | (cp & 0x3Fu)));
    }
}
}  // namespace

std::string UTF16ToUTF8(const uint16_t *s, size_t n) {
    std::string out;
    out.reserve(n);
    for (size_t i = 0; i < n;) {
        uint32_t w = s[i++];
        if (w >= 0xD800u && w <= 0xDBFFu) {
            if (i < n && s[i] >= 0xDC00u && s[i] <= 0xDFFFu) {
                uint32_t lo = s[i++];
                AppendUTF8(out, 0x10000u + ((w - 0xD800u) << 10) + (lo - 0xDC00u));
            } else {
                AppendUTF8(out, 0xFFFDu);
            }
        } else if (w >= 0xDC00u && w <= 0xDFFFu) {
            AppendUTF8(out, 0xFFFDu);
        } else {
            AppendUTF8(out, w);
        }
    }
    return out;
}

std::u16string UTF8ToUTF16(const char *s, size_t n) {
    std::u16string out;
    if (s == nullptr || n == 0) {
        return out;
    }
    out.reserve(n);
    const auto *p = reinterpret_cast<const unsigned char *>(s);
    size_t i = 0;
    while (i < n) {
        uint32_t cp = p[i++];
        if (cp < 0x80u) {
            // ASCII
        } else if ((cp & 0xE0u) == 0xC0u && i < n) {
            cp = ((cp & 0x1Fu) << 6) | (p[i++] & 0x3Fu);
        } else if ((cp & 0xF0u) == 0xE0u && i + 1 < n) {
            cp = ((cp & 0x0Fu) << 12) | ((p[i] & 0x3Fu) << 6) | (p[i + 1] & 0x3Fu);
            i += 2;
        } else if ((cp & 0xF8u) == 0xF0u && i + 2 < n) {
            cp = ((cp & 0x07u) << 18) | ((p[i] & 0x3Fu) << 12) | ((p[i + 1] & 0x3Fu) << 6) |
                 (p[i + 2] & 0x3Fu);
            i += 3;
        } else {
            cp = 0xFFFDu;
        }
        if (cp >= 0x10000u) {
            cp -= 0x10000u;
            out.push_back(static_cast<char16_t>(0xD800u + (cp >> 10)));
            out.push_back(static_cast<char16_t>(0xDC00u + (cp & 0x3FFu)));
        } else {
            out.push_back(static_cast<char16_t>(cp));
        }
    }
    return out;
}

// DefaultHeapAllocator small-slot must cover short UTF-16 StringBox and NumberBox.
#ifndef KRJSON_DISABLE_SLAB
    static_assert(sizeof(StringBox) + 33 * sizeof(uint16_t) <= DefaultHeapAllocator::kSmallSlotBytes);
#endif
static_assert(alignof(NumberBox) <= DefaultHeapAllocator::kSmallSlotAlign);
static_assert(alignof(StringBox) <= DefaultHeapAllocator::kSmallSlotAlign);

// ---- StringBox ----
template <size_t CharSize, uint8_t Encoding>
StringBox *CreateWithCharSize(const void *s, size_t n) {
    static_assert(CharSize == 1 || CharSize == 2, "StringBox payload is UTF-8 or UTF-16");
    static_assert(CharSize != 1 || Encoding == kStrUTF8, "CharSize 1 requires kStrUTF8");
    static_assert(CharSize != 2 || Encoding == kStrUTF16, "CharSize 2 requires kStrUTF16");
    // Guard against uint32 len truncation and size_t overflow in the allocation
    // (KRJSONNewString is public C ABI and may be called with an arbitrary n).
    // A >4GiB JSON string is degenerate; clamp to empty rather than corrupt.
    if (n > UINT32_MAX || n > (SIZE_MAX - sizeof(StringBox)) / CharSize - 1) {
        n = 0;
        s = nullptr;
    }
    void *mem = HeapAllocator::Allocate<alignof(StringBox)>(
        sizeof(StringBox) + (n + 1) * CharSize);
    auto *box = new (mem) StringBox();
    box->rc = 1;
    box->len = static_cast<uint32_t>(n);
    box->encoding = Encoding;
#if defined(KRJSON_LEAK_TEST)
    g_krjson_live_boxes.fetch_add(1, std::memory_order_relaxed);
#endif
    auto *dst = reinterpret_cast<unsigned char *>(box + 1);
    if (n > 0 && s != nullptr) {
        std::memcpy(dst, s, n * CharSize);
    }
    std::memset(dst + n * CharSize, 0, CharSize);
    return box;
}

StringBox *StringBox::Create(const char *s, size_t n) {
    return CreateWithCharSize<sizeof(char), kStrUTF8>(s, n);
}
StringBox *StringBox::CreateUTF16(const uint16_t *s, size_t n) {
    return CreateWithCharSize<sizeof(uint16_t), kStrUTF16>(s, n);
}
void StringBox::Free(StringBox *b) {
    const uint32_t len = b->len;
    const uint8_t encoding = b->encoding;
    b->~StringBox();
    if (encoding == kStrUTF16) {
        HeapAllocator::Deallocate<alignof(StringBox)>(
            b, sizeof(StringBox) + (len + 1) * sizeof(uint16_t));
    } else {
        HeapAllocator::Deallocate<alignof(StringBox)>(b, sizeof(StringBox) + len + 1);
    }
}

BytesBox *BytesBox::Create(const uint8_t *s, size_t n) {
    if (s == nullptr || n == 0 || n > UINT32_MAX || n > SIZE_MAX - sizeof(BytesBox)) {
        n = 0;
        s = nullptr;
    }
    void *mem = HeapAllocator::Allocate<alignof(BytesBox)>(sizeof(BytesBox) + n);
    auto *box = new (mem) BytesBox();
    box->rc = 1;
    box->len = static_cast<uint32_t>(n);
#if defined(KRJSON_LEAK_TEST)
    g_krjson_live_boxes.fetch_add(1, std::memory_order_relaxed);
#endif
    if (n > 0 && s != nullptr) {
        std::memcpy(box->data(), s, n);
    }
    return box;
}
void BytesBox::Free(BytesBox *b) {
    const uint32_t len = b->len;
    b->~BytesBox();
    HeapAllocator::Deallocate<alignof(BytesBox)>(b, sizeof(BytesBox) + len);
}

// ---- container destructors: release children (POD words don't auto-release) ----
ArrayBox::~ArrayBox() {
    for (KRJSON item : items) {
        Release(item);
    }
}
ObjectBox::~ObjectBox() {
    for (auto &kv : members) {
        Release(kv.second);
    }
}

// ---- lifetime ----
KRJSON Retain(KRJSON v) {
    if (auto *b = AsBox(v)) {
        b->rc.fetch_add(1, std::memory_order_relaxed);
    }
    return v;
}
void Release(KRJSON v) {
    auto *b = AsBox(v);
    if (b == nullptr) {
        return;
    }
    if (b->rc.fetch_sub(1, std::memory_order_acq_rel) == 1) {
#if defined(KRJSON_LEAK_TEST)
        g_krjson_live_boxes.fetch_sub(1, std::memory_order_relaxed);
#endif
        switch (TagOf(v)) {
            case kTagNumber:
                FreeNumberBox(BoxOf<NumberBox>(b));
                break;
            case kTagString:
                StringBox::Free(BoxOf<StringBox>(b));
                break;
            case kTagArray:
                AllocDelete<HeapAllocator>(BoxOf<ArrayBox>(b));
                break;
            case kTagObject:
                AllocDelete<HeapAllocator>(BoxOf<ObjectBox>(b));
                break;
            case kTagBytes:
                BytesBox::Free(BoxOf<BytesBox>(b));
                break;
            case kTagExt:
                AllocDelete<HeapAllocator>(BoxOf<OpaqueBox>(b));
                break;
            default:
                break;
        }
    }
}

// ---- constructors ----
KRJSON NewNull() {
    return 0;
}
KRJSON NewBool(bool b) {
    return (static_cast<uint64_t>(b ? 1 : 0) << 3) | kTagBool;
}
KRJSON NewInt32(int32_t x) {
    return EncodeImmNumber(kNumInt32, static_cast<uint64_t>(static_cast<int64_t>(x)));
}
KRJSON NewInt(int64_t x) {
    if (x >= INT32_MIN && x <= INT32_MAX) {
        if (FitsInt57(x)) {
            return EncodeImmNumber(kNumInt32, static_cast<uint64_t>(x));
        }
        return NewNumberHeap(kNumInt32, static_cast<uint64_t>(x));
    }
    if (FitsInt57(x)) {
        return EncodeImmNumber(kNumInt64, static_cast<uint64_t>(x));
    }
    return NewNumberHeap(kNumInt64, static_cast<uint64_t>(x));
}
KRJSON NewInt64(int64_t x) {
    if (FitsInt57(x)) {
        return EncodeImmNumber(kNumInt64, static_cast<uint64_t>(x));
    }
    return NewNumberHeap(kNumInt64, static_cast<uint64_t>(x));
}
KRJSON NewUint32(uint32_t x) {
    return EncodeImmNumber(kNumUint32, static_cast<uint64_t>(x));
}
KRJSON NewUint(uint64_t x) {
    const NumberKind kind = (x <= static_cast<uint64_t>(UINT32_MAX)) ? kNumUint32 : kNumUint64;
    if (x <= static_cast<uint64_t>(kInt57Max)) {
        return EncodeImmNumber(kind, x);
    }
    return NewNumberHeap(kind, x);
}
KRJSON NewUint64(uint64_t x) {
    if (x <= static_cast<uint64_t>(kInt57Max)) {
        return EncodeImmNumber(kNumUint64, x);
    }
    return NewNumberHeap(kNumUint64, x);
}
KRJSON NewDouble(double d) {
    if (!std::isfinite(d)) {
        return NewNull();
    }
    if (IsDoubleLosslessToFloat(d)) {
        return EncodeImmNumber(kNumDouble, FloatToBits(static_cast<float>(d)));
    }
    return NewNumberHeap(kNumDouble, DoubleToBits(d));
}
KRJSON NewIntIfSafeIntegral(double d) {
    // 2^53 - 1: largest integer a double represents exactly (JS Number.MAX_SAFE_INTEGER).
    constexpr int64_t kMaxSafeInteger = (int64_t{1} << std::numeric_limits<double>::digits) - 1;
    if (std::isfinite(d) && std::trunc(d) == d && std::fabs(d) <= static_cast<double>(kMaxSafeInteger)) {
        return NewInt(static_cast<int64_t>(d));
    }
    return NewDouble(d);
}
KRJSON NewFloat(float f) {
    if (!std::isfinite(f)) {
        return NewNull();
    }
    return EncodeImmNumber(kNumFloat, FloatToBits(f));
}
KRJSON NewString(const char *s, size_t n) {
    return EncodePtr(StringBox::Create(s, n), kTagString);
}
KRJSON NewStringUTF16(const uint16_t *s, size_t n) {
    return EncodePtr(StringBox::CreateUTF16(s, n), kTagString);
}
KRJSON NewBytes(const uint8_t *data, size_t n) {
    return EncodePtr(BytesBox::Create(data, n), kTagBytes);
}
KRJSON NewArray() {
    return EncodePtr(AllocNew<HeapAllocator, ArrayBox>(), kTagArray);
}
KRJSON NewObject() {
    return EncodePtr(AllocNew<HeapAllocator, ObjectBox>(), kTagObject);
}
KRJSON NewObjectUTF16() {
    return NewObject();
}
KRJSON NewOpaque(const void *a, const void *b) {
    auto *box = AllocNew<HeapAllocator, OpaqueBox>();
    box->rc = 1;
    box->a = a;
    box->b = b;
    return EncodePtr(box, kTagExt);
}
bool GetOpaque(KRJSON v, const void **a, const void **b) {
    // KRJSON_INVALID (0x08, tag 0) is naturally rejected here (tag 0 != kTagExt),
    // but check explicitly to be safe against future INVALID value changes.
    if (v == KRJSON_INVALID || TagOf(v) != kTagExt) {
        return false;
    }
    auto *box = BoxOf<OpaqueBox>(AsBox(v));
    if (a != nullptr) {
        *a = box->a;
    }
    if (b != nullptr) {
        *b = box->b;
    }
    return true;
}

void ArrayAppend(KRJSON array, KRJSON child) {
    if (TagOf(array) != kTagArray) {
        return;
    }
    BoxOf<ArrayBox>(AsBox(array))->items.push_back(Retain(child));
}
void ArraySet(KRJSON array, size_t index, KRJSON child) {
    if (TagOf(array) != kTagArray) {
        return;
    }
    auto &items = BoxOf<ArrayBox>(AsBox(array))->items;
    if (index >= items.size()) {
        return;
    }
    // child may be borrowed from the old value (itself or a descendant): retain first.
    const KRJSON retained = Retain(child);
    Release(items[index]);
    items[index] = retained;
}
void ObjectPutUTF16(KRJSON object, const uint16_t *key, size_t units, KRJSON child) {
    if (TagOf(object) != kTagObject || (key == nullptr && units != 0)) {
        return;
    }
    auto *box = BoxOf<ObjectBox>(AsBox(object));
    const size_t nbytes = units * sizeof(uint16_t);
    auto &members = box->members;
    for (auto &kv : members) {
        if (kv.first.size() == units &&
            (units == 0 || std::memcmp(kv.first.data(), key, nbytes) == 0)) {
            const KRJSON retained = Retain(child);
            Release(kv.second);
            kv.second = retained;
            return;
        }
    }
    members.emplace_back(
        units == 0 ? std::u16string()
                   : std::u16string(reinterpret_cast<const char16_t *>(key), units),
        Retain(child));
}

void ObjectPut(KRJSON object, const char *key, size_t key_len, KRJSON child) {
    if (key == nullptr) {
        return;
    }
    const std::u16string u16 = UTF8ToUTF16(key, key_len);
    ObjectPutUTF16(object, reinterpret_cast<const uint16_t *>(u16.data()), u16.size(), child);
}

void ObjectAppendUTF16NoDedup(KRJSON object, const uint16_t *key, size_t units, KRJSON child) {
    if (TagOf(object) != kTagObject || (key == nullptr && units != 0)) {
        return;
    }
    auto *box = BoxOf<ObjectBox>(AsBox(object));
    box->members.emplace_back(
        units == 0 ? std::u16string()
                   : std::u16string(reinterpret_cast<const char16_t *>(key), units),
        Retain(child));
}

void ObjectAppendNoDedup(KRJSON object, const char *key, size_t key_len, KRJSON child) {
    if (key == nullptr) {
        return;
    }
    const std::u16string u16 = UTF8ToUTF16(key, key_len);
    ObjectAppendUTF16NoDedup(object, reinterpret_cast<const uint16_t *>(u16.data()), u16.size(), child);
}

namespace {
// Collapse duplicate keys in place: last value wins, kept at the first key's
// slot; other slots dropped. Only rewrites the vector when a duplicate exists.
template <typename Members, typename View>
void DedupLastImpl(Members &members) {
    std::unordered_map<View, size_t> first_index;
    first_index.reserve(members.size());
    bool has_dup = false;
    for (size_t i = 0; i < members.size(); ++i) {
        View key(members[i].first.data(), members[i].first.size());
        auto result = first_index.emplace(key, i);
        if (!result.second) {
            // Duplicate: later value wins at the earlier slot. Only POD
            // KRJSON words move here — no strings are relocated yet, so the
            // views stored in first_index stay valid for the rest of this pass.
            Release(members[result.first->second].second);
            members[result.first->second].second = members[i].second;
            members[i].second = KRJSON_INVALID;  // moved out; slot will be dropped
            has_dup = true;
        }
    }
    if (!has_dup) {
        return;
    }
    Members compact;
    compact.reserve(first_index.size());
    for (size_t i = 0; i < members.size(); ++i) {
        // Pass 2: do NOT look up first_index[key] here. After std::move in
        // the previous iteration, SSO short keys' internal buffers may be
        // cleared, making the view dangle. Instead, rely on the Pass 1
        // marker: slots with KRJSON_INVALID were duplicates and are dropped.
        if (members[i].second != KRJSON_INVALID) {
            compact.push_back(std::move(members[i]));
        }
    }
    members.swap(compact);
}
}  // namespace

void ObjectDedupLast(KRJSON object) {
    if (TagOf(object) != kTagObject) {
        return;
    }
    auto *box = BoxOf<ObjectBox>(AsBox(object));
    DedupLastImpl<ObjectBox::UTF16Members, std::u16string_view>(box->members);
}

// ---- accessors ----
KRJSONType GetType(KRJSON v) {
    if (v == KRJSON_INVALID) {
        return KRJSON_NULL;
    }
    switch (TagOf(v)) {
        case kTagBool:
            return KRJSON_BOOL;
        case kTagNumber:
            switch (KindOfNumber(v)) {
                case kNumInt32:
                    return KRJSON_INT;
                case kNumInt64:
                    return KRJSON_LONG;
                case kNumUint32:
                case kNumUint64:
                    return KRJSON_UINT;
                case kNumFloat:
                    return KRJSON_FLOAT;
                case kNumDouble:
                    return KRJSON_DOUBLE;
            }
            return KRJSON_INT;
        case kTagString: {
            const auto *box = BoxOf<StringBox>(AsBox(v));
            return (box != nullptr && box->encoding == kStrUTF16) ? KRJSON_U16STRING : KRJSON_STRING;
        }
        case kTagBytes:
            return KRJSON_BYTES;
        case kTagArray:
            return KRJSON_ARRAY;
        case kTagObject:
            return KRJSON_OBJECT;
        case kTagNull:
        case kTagExt:
        default:
            return KRJSON_NULL;
    }
}
bool GetBool(KRJSON v, bool default_value) {
    return TagOf(v) == kTagBool ? ((v >> 3) & 1u) != 0 : default_value;
}

namespace {
double NumberAsDouble(KRJSON v) {
    const NumberKind kind = KindOfNumber(v);
    if (IsNumberImm(v)) {
        if (kind == kNumFloat || kind == kNumDouble) {
            return static_cast<double>(BitsToFloat(static_cast<uint32_t>(v >> 7)));
        }
        return static_cast<double>(ImmNumberSigned(v));
    }
    const NumberBox *box = AsNumberBox(v);
    if (box == nullptr) {
        return 0.0;
    }
    if (box->kind == kNumDouble) {
        return BitsToDouble(box->bits);
    }
    if (box->kind == kNumUint32 || box->kind == kNumUint64) {
        return static_cast<double>(box->bits);
    }
    return static_cast<double>(static_cast<int64_t>(box->bits));
}
int64_t NumberAsInt(KRJSON v) {
    const NumberKind kind = KindOfNumber(v);
    if (IsNumberImm(v)) {
        if (kind == kNumFloat || kind == kNumDouble) {
            return static_cast<int64_t>(BitsToFloat(static_cast<uint32_t>(v >> 7)));
        }
        return ImmNumberSigned(v);
    }
    const NumberBox *box = AsNumberBox(v);
    if (box == nullptr) {
        return 0;
    }
    if (box->kind == kNumDouble) {
        return static_cast<int64_t>(BitsToDouble(box->bits));
    }
    return static_cast<int64_t>(box->bits);
}
uint64_t NumberAsUint(KRJSON v) {
    const NumberKind kind = KindOfNumber(v);
    if (IsNumberImm(v)) {
        if (kind == kNumFloat || kind == kNumDouble) {
            return static_cast<uint64_t>(BitsToFloat(static_cast<uint32_t>(v >> 7)));
        }
        return static_cast<uint64_t>(ImmNumberSigned(v));
    }
    const NumberBox *box = AsNumberBox(v);
    if (box == nullptr) {
        return 0;
    }
    if (box->kind == kNumDouble) {
        return static_cast<uint64_t>(BitsToDouble(box->bits));
    }
    return box->bits;
}
}  // namespace

int64_t GetInt(KRJSON v, int64_t default_value) {
    return TagOf(v) == kTagNumber ? NumberAsInt(v) : default_value;
}
uint64_t GetUint(KRJSON v, uint64_t default_value) {
    return TagOf(v) == kTagNumber ? NumberAsUint(v) : default_value;
}
double GetDouble(KRJSON v, double default_value) {
    return TagOf(v) == kTagNumber ? NumberAsDouble(v) : default_value;
}
const char *GetString(KRJSON v, size_t *out_len) {
    if (TagOf(v) == kTagString) {
        auto *box = BoxOf<StringBox>(AsBox(v));
        if (box != nullptr && box->encoding == kStrUTF8) {
            if (out_len != nullptr) {
                *out_len = box->len;
            }
            return box->data();
        }
    }
    if (out_len != nullptr) {
        *out_len = 0;
    }
    return "";
}
const uint16_t *GetStringUTF16(KRJSON v, size_t *out_units) {
    if (TagOf(v) == kTagString) {
        auto *box = BoxOf<StringBox>(AsBox(v));
        if (box != nullptr && box->encoding == kStrUTF16) {
            if (out_units != nullptr) {
                *out_units = box->len;
            }
            return box->u16data();
        }
    }
    if (out_units != nullptr) {
        *out_units = 0;
    }
    return nullptr;
}
const uint8_t *GetBytes(KRJSON v, size_t *out_len) {
    if (TagOf(v) == kTagBytes) {
        auto *box = BoxOf<BytesBox>(AsBox(v));
        if (out_len != nullptr) {
            *out_len = box->len;
        }
        return box->len == 0 ? nullptr : box->data();
    }
    if (out_len != nullptr) {
        *out_len = 0;
    }
    return nullptr;
}
size_t GetSize(KRJSON v) {
    switch (TagOf(v)) {
        case kTagArray:
            return BoxOf<ArrayBox>(AsBox(v))->items.size();
        case kTagObject:
            return BoxOf<ObjectBox>(AsBox(v))->members.size();
        case kTagBytes:
            return BoxOf<BytesBox>(AsBox(v))->len;
        default:
            return 0;
    }
}
KRJSON ArrayGet(KRJSON array, size_t index) {
    if (TagOf(array) == kTagArray) {
        auto &items = BoxOf<ArrayBox>(AsBox(array))->items;
        if (index < items.size()) {
            return items[index];  // borrowed
        }
    }
    return KRJSON_INVALID;
}
KRJSON ObjectGetUTF16(KRJSON object, const uint16_t *key, size_t units) {
    if (TagOf(object) != kTagObject || (key == nullptr && units != 0)) {
        return KRJSON_INVALID;
    }
    auto *box = BoxOf<ObjectBox>(AsBox(object));
    const size_t nbytes = units * sizeof(uint16_t);
    for (auto &kv : box->members) {
        if (kv.first.size() == units &&
            (units == 0 || std::memcmp(kv.first.data(), key, nbytes) == 0)) {
            return kv.second;
        }
    }
    return KRJSON_INVALID;
}
KRJSON ObjectGet(KRJSON object, const char *key, size_t key_len) {
    if (key == nullptr) {
        return KRJSON_INVALID;
    }
    const std::u16string u16 = UTF8ToUTF16(key, key_len);
    return ObjectGetUTF16(object, reinterpret_cast<const uint16_t *>(u16.data()), u16.size());
}
bool ObjectKeysAreUTF16(KRJSON object) {
    return TagOf(object) == kTagObject;
}
KRJSON ObjectValueAt(KRJSON object, size_t index) {
    if (TagOf(object) != kTagObject) {
        return KRJSON_INVALID;
    }
    auto *box = BoxOf<ObjectBox>(AsBox(object));
    if (index < box->members.size()) {
        return box->members[index].second;
    }
    return KRJSON_INVALID;
}
const char *ObjectKeyAt(KRJSON /*object*/, size_t /*index*/) {
    return nullptr;
}
const uint16_t *ObjectKeyAtUTF16(KRJSON object, size_t index, size_t *out_units) {
    if (TagOf(object) != kTagObject) {
        if (out_units != nullptr) {
            *out_units = 0;
        }
        return nullptr;
    }
    auto *box = BoxOf<ObjectBox>(AsBox(object));
    if (index >= box->members.size()) {
        if (out_units != nullptr) {
            *out_units = 0;
        }
        return nullptr;
    }
    auto &key = box->members[index].first;
    if (out_units != nullptr) {
        *out_units = key.size();
    }
    return reinterpret_cast<const uint16_t *>(key.c_str());
}
void ObjectForEach(KRJSON object, KRJSONObjectVisitor visitor, void *userdata) {
    if (TagOf(object) != kTagObject || visitor == nullptr) {
        return;
    }
    auto *box = BoxOf<ObjectBox>(AsBox(object));
    for (auto &kv : box->members) {
        const std::string utf8 = UTF16ToUTF8(
            reinterpret_cast<const uint16_t *>(kv.first.data()), kv.first.size());
        if (!visitor(utf8.data(), utf8.size(), kv.second, userdata)) {
            break;
        }
    }
}

// ---- serialize ----
namespace {
void WriteTo(KRJSON v, rapidjson::Writer<rapidjson::StringBuffer> &w) {
    switch (GetType(v)) {
        case KRJSON_NULL:
            w.Null();
            break;
        case KRJSON_BOOL:
            w.Bool(GetBool(v, false));
            break;
        case KRJSON_INT:
            w.Int64(GetInt(v, 0));
            break;
        case KRJSON_LONG:
            w.Int64(GetInt(v, 0));
            break;
        case KRJSON_UINT:
            w.Uint64(GetUint(v, 0));
            break;
        case KRJSON_DOUBLE:
            w.Double(GetDouble(v, 0));
            break;
        case KRJSON_FLOAT:
            w.Double(GetDouble(v, 0));
            break;
        case KRJSON_STRING: {
            size_t len = 0;
            const char *s = GetString(v, &len);
            w.String(s, static_cast<rapidjson::SizeType>(len));
            break;
        }
        case KRJSON_U16STRING: {
            size_t units = 0;
            const uint16_t *s = GetStringUTF16(v, &units);
            const std::string utf8 = UTF16ToUTF8(s, units);
            w.String(utf8.data(), static_cast<rapidjson::SizeType>(utf8.size()));
            break;
        }
        case KRJSON_BYTES:
            // Binary values only exist on the bridge path and have no JSON
            // text representation. Match the historical fallback to null.
            w.Null();
            break;
        case KRJSON_ARRAY: {
            w.StartArray();
            auto &items = BoxOf<ArrayBox>(AsBox(v))->items;
            for (KRJSON item : items) {
                WriteTo(item, w);
            }
            w.EndArray();
            break;
        }
        case KRJSON_OBJECT: {
            w.StartObject();
            auto *box = BoxOf<ObjectBox>(AsBox(v));
            for (auto &kv : box->members) {
                const std::string utf8 = UTF16ToUTF8(
                    reinterpret_cast<const uint16_t *>(kv.first.data()), kv.first.size());
                w.Key(utf8.data(), static_cast<rapidjson::SizeType>(utf8.size()));
                WriteTo(kv.second, w);
            }
            w.EndObject();
            break;
        }
    }
}

using UTF16Enc = rapidjson::UTF16<char16_t>;
using UTF16Buffer = rapidjson::GenericStringBuffer<UTF16Enc>;
using UTF16Writer = rapidjson::Writer<UTF16Buffer, UTF16Enc, UTF16Enc>;

void WriteToUTF16(KRJSON v, UTF16Writer &w) {
    switch (GetType(v)) {
        case KRJSON_NULL:
            w.Null();
            break;
        case KRJSON_BOOL:
            w.Bool(GetBool(v, false));
            break;
        case KRJSON_INT:
            w.Int64(GetInt(v, 0));
            break;
        case KRJSON_LONG:
            w.Int64(GetInt(v, 0));
            break;
        case KRJSON_UINT:
            w.Uint64(GetUint(v, 0));
            break;
        case KRJSON_DOUBLE:
            w.Double(GetDouble(v, 0));
            break;
        case KRJSON_FLOAT:
            w.Double(GetDouble(v, 0));
            break;
        case KRJSON_STRING: {
            size_t len = 0;
            const char *s = GetString(v, &len);
            const std::u16string u16 = UTF8ToUTF16(s == nullptr ? "" : s, len);
            w.String(u16.data(), static_cast<rapidjson::SizeType>(u16.size()));
            break;
        }
        case KRJSON_U16STRING: {
            size_t units = 0;
            const uint16_t *s = GetStringUTF16(v, &units);
            static const char16_t kEmpty[] = u"";
            w.String(s == nullptr ? kEmpty : reinterpret_cast<const char16_t *>(s),
                     static_cast<rapidjson::SizeType>(s == nullptr ? 0 : units));
            break;
        }
        case KRJSON_BYTES:
            w.Null();
            break;
        case KRJSON_ARRAY: {
            w.StartArray();
            auto &items = BoxOf<ArrayBox>(AsBox(v))->items;
            for (KRJSON item : items) {
                WriteToUTF16(item, w);
            }
            w.EndArray();
            break;
        }
        case KRJSON_OBJECT: {
            w.StartObject();
            auto *box = BoxOf<ObjectBox>(AsBox(v));
            for (auto &kv : box->members) {
                w.Key(kv.first.data(), static_cast<rapidjson::SizeType>(kv.first.size()));
                WriteToUTF16(kv.second, w);
            }
            w.EndObject();
            break;
        }
    }
}
}  // namespace

std::string Dump(KRJSON v) {
    rapidjson::StringBuffer buffer;
    rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
    WriteTo(v, writer);
    return std::string(buffer.GetString(), buffer.GetSize());
}

std::u16string DumpUTF16(KRJSON v) {
    UTF16Buffer buffer;
    UTF16Writer writer(buffer);
    WriteToUTF16(v, writer);
    return std::u16string(buffer.GetString(), buffer.GetLength());
}

bool StringEquals(KRJSON a, KRJSON b) {
    if (a == b) {
        return true;
    }
    const KRJSONType pa = GetType(a);
    const KRJSONType pb = GetType(b);
    const bool a_str = pa == KRJSON_STRING || pa == KRJSON_U16STRING;
    const bool b_str = pb == KRJSON_STRING || pb == KRJSON_U16STRING;
    if (!a_str || !b_str) {
        return false;
    }
    if (pa == KRJSON_U16STRING && pb == KRJSON_U16STRING) {
        size_t a_n = 0;
        size_t b_n = 0;
        const uint16_t *as = GetStringUTF16(a, &a_n);
        const uint16_t *bs = GetStringUTF16(b, &b_n);
        if (a_n != b_n) {
            return false;
        }
        return a_n == 0 || (as != nullptr && bs != nullptr &&
                            std::memcmp(as, bs, a_n * sizeof(uint16_t)) == 0);
    }
    if (pa == KRJSON_STRING && pb == KRJSON_STRING) {
        size_t a_n = 0;
        size_t b_n = 0;
        const char *as = GetString(a, &a_n);
        const char *bs = GetString(b, &b_n);
        if (a_n != b_n) {
            return false;
        }
        return a_n == 0 || (as != nullptr && bs != nullptr && std::memcmp(as, bs, a_n) == 0);
    }
    size_t units = 0;
    size_t n8 = 0;
    const uint16_t *u16 = nullptr;
    const char *u8 = nullptr;
    if (pa == KRJSON_U16STRING) {
        u16 = GetStringUTF16(a, &units);
        u8 = GetString(b, &n8);
    } else {
        u16 = GetStringUTF16(b, &units);
        u8 = GetString(a, &n8);
    }
    if (u8 == nullptr) {
        return units == 0 && n8 == 0;
    }
    const std::string conv = UTF16ToUTF8(u16, units);
    return conv.size() == n8 && std::memcmp(conv.data(), u8, n8) == 0;
}

namespace {

struct UTF16ViewHash {
    size_t operator()(std::u16string_view v) const noexcept {
        return std::hash<std::string_view>{}(std::string_view(
            reinterpret_cast<const char *>(v.data()), v.size() * sizeof(char16_t)));
    }
};

bool ObjectEquals(KRJSON a, KRJSON b) {
    if (TagOf(a) != kTagObject || TagOf(b) != kTagObject) {
        return false;
    }
    const auto &ma = BoxOf<ObjectBox>(AsBox(a))->members;
    const auto &mb = BoxOf<ObjectBox>(AsBox(b))->members;
    const size_t n = ma.size();
    if (n != mb.size()) {
        return false;
    }
    if (n == 0) {
        return true;
    }
    // Small objects: linear scan. Large objects: hash b's keys so total is O(n)
    // instead of O(n²) ObjectGetUTF16 (pageData-sized maps).
    constexpr size_t kLinearLimit = 8;
    if (n <= kLinearLimit) {
        for (const auto &kv : ma) {
            KRJSON child_b = KRJSON_INVALID;
            for (const auto &other : mb) {
                if (other.first == kv.first) {
                    child_b = other.second;
                    break;
                }
            }
            if (child_b == KRJSON_INVALID || !Equals(kv.second, child_b)) {
                return false;
            }
        }
        return true;
    }
    std::unordered_map<std::u16string_view, KRJSON, UTF16ViewHash> index;
    index.reserve(n);
    for (const auto &kv : mb) {
        index.emplace(std::u16string_view(kv.first), kv.second);
    }
    for (const auto &kv : ma) {
        const auto it = index.find(std::u16string_view(kv.first));
        if (it == index.end() || !Equals(kv.second, it->second)) {
            return false;
        }
    }
    return true;
}

bool IsIntegerType(KRJSONType t) {
    return t == KRJSON_INT || t == KRJSON_LONG || t == KRJSON_UINT;
}

// INT / LONG / UINT compare by value. A UINT above INT64_MAX never equals a signed value.
bool IntegerEquals(KRJSON a, KRJSONType ta, KRJSON b, KRJSONType tb) {
    const bool a_unsigned = ta == KRJSON_UINT;
    const bool b_unsigned = tb == KRJSON_UINT;
    if (a_unsigned && b_unsigned) {
        return GetUint(a, 0) == GetUint(b, 0);
    }
    if (!a_unsigned && !b_unsigned) {
        return GetInt(a, 0) == GetInt(b, 0);
    }
    const int64_t s = a_unsigned ? GetInt(b, 0) : GetInt(a, 0);
    const uint64_t u = a_unsigned ? GetUint(a, 0) : GetUint(b, 0);
    return s >= 0 && static_cast<uint64_t>(s) == u;
}

}  // namespace

bool Equals(KRJSON a, KRJSON b) {
    if (a == b) {
        return true;
    }
    if (a == KRJSON_INVALID || b == KRJSON_INVALID) {
        return false;
    }
    const uint8_t ta = TagOf(a);
    const uint8_t tb = TagOf(b);
    if (ta == kTagExt || tb == kTagExt) {
        if (ta != kTagExt || tb != kTagExt) {
            return false;
        }
        const void *a0 = nullptr;
        const void *a1 = nullptr;
        const void *b0 = nullptr;
        const void *b1 = nullptr;
        if (!GetOpaque(a, &a0, &a1) || !GetOpaque(b, &b0, &b1)) {
            return false;
        }
        return a0 == b0 && a1 == b1;
    }

    const KRJSONType pa = GetType(a);
    const KRJSONType pb = GetType(b);
    if ((pa == KRJSON_STRING || pa == KRJSON_U16STRING) &&
        (pb == KRJSON_STRING || pb == KRJSON_U16STRING)) {
        return StringEquals(a, b);
    }
    if (IsIntegerType(pa) && IsIntegerType(pb)) {
        return IntegerEquals(a, pa, b, pb);
    }
    if (pa != pb) {
        return false;
    }
    switch (pa) {
        case KRJSON_NULL:
            return true;
        case KRJSON_BOOL:
            return GetBool(a, false) == GetBool(b, false);
        case KRJSON_FLOAT:
        case KRJSON_DOUBLE:
            return GetDouble(a, 0.0) == GetDouble(b, 0.0);
        case KRJSON_BYTES: {
            size_t na = 0;
            size_t nb = 0;
            const uint8_t *ba = GetBytes(a, &na);
            const uint8_t *bb = GetBytes(b, &nb);
            if (na != nb) {
                return false;
            }
            if (na == 0) {
                return true;
            }
            return ba != nullptr && bb != nullptr && std::memcmp(ba, bb, na) == 0;
        }
        case KRJSON_ARRAY: {
            const size_t n = GetSize(a);
            if (n != GetSize(b)) {
                return false;
            }
            for (size_t i = 0; i < n; ++i) {
                if (!Equals(ArrayGet(a, i), ArrayGet(b, i))) {
                    return false;
                }
            }
            return true;
        }
        case KRJSON_OBJECT:
            return ObjectEquals(a, b);
        default:
            return false;
    }
}

}  // namespace json
}  // namespace util
}  // namespace kuikly
