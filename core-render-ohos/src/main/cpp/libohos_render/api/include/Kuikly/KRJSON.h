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

#ifndef CORE_RENDER_OHOS_KRJSON_H
#define CORE_RENDER_OHOS_KRJSON_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#include "KuiklyExport.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * A JSON value is a tagged 8-byte word: bits[0..2] are the storage tag
 * (null/bool/number/string/array/object/bytes/ext). Heap values are
 * `aligned_ptr | tag`. Copying the word does NOT change ownership;
 * use KRJSONRetain / KRJSONRelease. See KRJSON-pointer-tagging.md.
 *
 * Immediate values (null / bool / inlined numbers) need no allocation;
 * KRJSONRetain / KRJSONRelease on them are no-ops.
 */
typedef uint64_t KRJSON;

/**
 * Sentinel for "not a valid KRJSON value" (parse failure, lookup miss, moved-out slot).
 * Uses tag 0 (kTagNull) with a non-zero payload bit, so IsHeap/AsBox/GetOpaque
 * naturally treat it as non-heap without needing a special case. GetType reports
 * KRJSON_NULL — always test == KRJSON_INVALID to distinguish from real JSON null.
 */
#define KRJSON_INVALID ((KRJSON)0x08u)

/**
 * Public KRJSONType: storage tags 0/1/3–6 match this enum. Number (storage tag 2)
 * and string encoding (UTF-8 vs UTF-16) are expanded here. Storage tag 7 (ext /
 * NAPI) reports KRJSON_NULL. Use == KRJSON_INVALID to distinguish from JSON null.
 *
 *   0 null, 1 bool, 3 UTF-8 string, 4 array, 5 object, 6 bytes
 *   8 int, 9 double, 10 float, 11 long, 12 uint, 13 UTF-16 string
 */
typedef enum {
    KRJSON_NULL = 0,
    KRJSON_BOOL = 1,
    KRJSON_STRING = 3,      /* UTF-8 heap string; use KRJSONGetString */
    KRJSON_ARRAY = 4,
    KRJSON_OBJECT = 5,
    KRJSON_BYTES = 6,
    KRJSON_INT = 8,         /* Kotlin Int, or parser JSON integer within int32 */
    KRJSON_DOUBLE = 9,      /* Kotlin Double, or parser JSON float */
    KRJSON_FLOAT = 10,      /* Kotlin Float */
    KRJSON_LONG = 11,       /* Kotlin Long, or parser JSON integer within int64 */
    KRJSON_UINT = 12,       /* parser integer above int64 max */
    KRJSON_U16STRING = 13,  /* UTF-16 heap string; use KRJSONGetStringUTF16 */
} KRJSONType;

/** Object iteration callback; return false to stop early. `value` is borrowed. */
typedef bool (*KRJSONObjectVisitor)(const char *key, size_t key_len, KRJSON value, void *userdata);

// ---- lifetime ----
/** Increment refcount (no-op for immediates). Returns the same value. */
KUIKLY_EXPORT KRJSON KRJSONRetain(KRJSON value);
/** Decrement refcount; frees the heap box (and its children) at zero. */
KUIKLY_EXPORT void KRJSONRelease(KRJSON value);

// ---- parse / serialize ----
/**
 * Parse UTF-8 JSON. String values are `KRJSON_STRING` (UTF-8); object keys are
 * stored as UTF-16. Returns an OWNED
 * value (release with KRJSONRelease), or KRJSON_INVALID on error. If `err` is
 * non-null it receives a malloc'd message on failure (free with
 * KRJSONFreeString); it is set to NULL on success.
 */
KUIKLY_EXPORT KRJSON KRJSONParse(const char *data, size_t len, char **err);
/**
 * Parse UTF-16 JSON source (`unit_count` = code units). String values are
 * `KRJSON_U16STRING`; object keys are UTF-16. Same ownership as KRJSONParse.
 */
KUIKLY_EXPORT KRJSON KRJSONParseUTF16(const uint16_t *data, size_t unit_count, char **err);
/** Serialize to malloc'd, NUL-terminated UTF-8 JSON text (free with KRJSONFreeString). */
KUIKLY_EXPORT char *KRJSONDump(KRJSON value);
/**
 * Serialize to malloc'd, NUL-terminated UTF-16 JSON text.
 * `out_units` is the code-unit count excluding the terminator. Free the buffer
 * with KRJSONFreeString((char *)ptr). Prefer this on the Kotlin stringify path.
 */
KUIKLY_EXPORT uint16_t *KRJSONDumpUTF16(KRJSON value, size_t *out_units);
/** Free a string returned by KRJSONDump / KRJSONDumpUTF16 / KRJSONParse's err. */
KUIKLY_EXPORT void KRJSONFreeString(char *str);

// ---- type / scalar accessors (by value) ----
KUIKLY_EXPORT KRJSONType KRJSONGetType(KRJSON value);
KUIKLY_EXPORT bool KRJSONGetBool(KRJSON value, bool default_value);
KUIKLY_EXPORT int64_t KRJSONGetInt(KRJSON value, int64_t default_value);
KUIKLY_EXPORT uint64_t KRJSONGetUint(KRJSON value, uint64_t default_value);
KUIKLY_EXPORT double KRJSONGetDouble(KRJSON value, double default_value);
/** Borrowed, NUL-terminated UTF-8 bytes valid while `value` is retained.
 *  Only `KRJSON_STRING`. Empty on mismatch (including `KRJSON_U16STRING`). */
KUIKLY_EXPORT const char *KRJSONGetString(KRJSON value, size_t *out_len);
/** Borrowed, NUL-terminated UTF-16 units; NULL if not `KRJSON_U16STRING`.
 *  `out_units` is the code-unit count (not including the terminator). */
KUIKLY_EXPORT const uint16_t *KRJSONGetStringUTF16(KRJSON value, size_t *out_units);
/** Borrowed binary data valid while `value` is retained; NULL on mismatch. */
KUIKLY_EXPORT const uint8_t *KRJSONGetBytes(KRJSON value, size_t *out_len);

/**
 * Deep equality. The same word is equal. KRJSON_INVALID equals only itself
 * (not JSON null). UTF-8 and UTF-16 strings compare as Unicode. Objects compare
 * by key set (order-insensitive). INT / LONG / UINT compare by numeric value;
 * integers never equal FLOAT / DOUBLE values.
 */
KUIKLY_EXPORT bool KRJSONEquals(KRJSON a, KRJSON b);
/** True only when both sides are strings (UTF-8 and/or UTF-16). */
KUIKLY_EXPORT bool KRJSONStringEquals(KRJSON a, KRJSON b);

// ---- containers ----
/** Array length, object member count, or byte payload length; else 0. */
KUIKLY_EXPORT size_t KRJSONGetSize(KRJSON value);
/** Array element by index; BORROWED (retain to keep). KRJSON_INVALID if out of range. */
KUIKLY_EXPORT KRJSON KRJSONArrayGet(KRJSON array, size_t index);
/** Object member by UTF-8 key; BORROWED. The key is transcoded to UTF-16 then
 *  looked up. KRJSON_INVALID if missing. O(n) linear scan. */
KUIKLY_EXPORT KRJSON KRJSONObjectGet(KRJSON object, const char *key);
/** Object member by UTF-16 key (native storage). KRJSON_INVALID if missing. */
KUIKLY_EXPORT KRJSON KRJSONObjectGetUTF16(KRJSON object, const uint16_t *key, size_t units);
/** True for any object: keys are always stored as UTF-16. */
KUIKLY_EXPORT bool KRJSONObjectKeysAreUTF16(KRJSON object);
/** Object member by insertion index; BORROWED. KRJSON_INVALID if out of range. */
KUIKLY_EXPORT KRJSON KRJSONObjectValueAt(KRJSON object, size_t index);
/** Always NULL — keys are UTF-16. Use KRJSONObjectKeyAtUTF16. */
KUIKLY_EXPORT const char *KRJSONObjectKeyAt(KRJSON object, size_t index);
/** Borrowed UTF-16 key units. NULL if out of range. */
KUIKLY_EXPORT const uint16_t *KRJSONObjectKeyAtUTF16(KRJSON object, size_t index, size_t *out_units);
/** Iterate object members in insertion order. */
KUIKLY_EXPORT void KRJSONObjectForEach(KRJSON object, KRJSONObjectVisitor visitor, void *userdata);

// ---- constructors (return OWNED values) ----
KUIKLY_EXPORT KRJSON KRJSONNewNull(void);
KUIKLY_EXPORT KRJSON KRJSONNewBool(bool v);
KUIKLY_EXPORT KRJSON KRJSONNewInt32(int32_t v);
KUIKLY_EXPORT KRJSON KRJSONNewInt(int64_t v);
KUIKLY_EXPORT KRJSON KRJSONNewInt64(int64_t v);
KUIKLY_EXPORT KRJSON KRJSONNewUint(uint64_t v);
KUIKLY_EXPORT KRJSON KRJSONNewFloat(float v);
KUIKLY_EXPORT KRJSON KRJSONNewDouble(double v);
KUIKLY_EXPORT KRJSON KRJSONNewString(const char *data, size_t len);
/** UTF-16 payload (`unit_count` = code units, not bytes). Prefer this on the ArkVM/Kotlin path. */
KUIKLY_EXPORT KRJSON KRJSONNewStringUTF16(const uint16_t *data, size_t unit_count);
KUIKLY_EXPORT KRJSON KRJSONNewBytes(const uint8_t *data, size_t len);
KUIKLY_EXPORT KRJSON KRJSONNewArray(void);
KUIKLY_EXPORT KRJSON KRJSONNewObject(void);
/** Same as KRJSONNewObject: keys are always UTF-16. */
KUIKLY_EXPORT KRJSON KRJSONNewObjectUTF16(void);
/** Append `child` to array; the array retains it (caller still owns its own ref). */
KUIKLY_EXPORT void KRJSONArrayAppend(KRJSON array, KRJSON child);
/** Put UTF-8 key->child. The key is transcoded to UTF-16 then stored. */
KUIKLY_EXPORT void KRJSONObjectPut(KRJSON object, const char *key, size_t key_len, KRJSON child);
/** Put UTF-16 key->child (native storage). */
KUIKLY_EXPORT void KRJSONObjectPutUTF16(KRJSON object, const uint16_t *key, size_t units, KRJSON child);
/**
 * Append a UTF-16 key without scanning existing members. Callers must
 * guarantee unique keys; Kotlin Map conversion satisfies that contract.
 */
KUIKLY_EXPORT void KRJSONObjectAppendUTF16NoDedup(
    KRJSON object, const uint16_t *key, size_t units, KRJSON child);

#ifdef __cplusplus
}
#endif

#endif  // CORE_RENDER_OHOS_KRJSON_H
