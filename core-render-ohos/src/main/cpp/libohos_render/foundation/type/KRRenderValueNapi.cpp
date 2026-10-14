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

#include "libohos_render/foundation/type/KRRenderValueNapi.h"

#include <js_native_api.h>
#include <chrono>
#include <cstring>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

#include "libohos_render/foundation/ark_ts.h"
#include "libohos_render/utils/NAPIUtil.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/json/Box.h"

namespace kuikly {
namespace util {

namespace {
constexpr int kMaxBridgeDepth = 256;
constexpr uint32_t kKRJSONWrapMagic = 0x4B524A53u;  // 'KRJS'
constexpr char kKRJSONRoutePayloadTokenKey[] = "__kuiklyKRJsonPayloadId";
constexpr auto kKRJSONRoutePayloadMaxAge = std::chrono::minutes(1);
const napi_type_tag kKRJSONTypeTag = {
    0x4B75696B6C794A53ULL,
    0x4F4E577261704B52ULL,
};

template <typename Get, typename Status>
KRJSON NewStringFromUTF16Get(Get &&get, Status ok) {
    size_t units = 0;
    if (get(nullptr, 0, &units) != ok) {
        return kuikly::util::json::NewStringUTF16(nullptr, 0);
    }
    if (units == 0) {
        return kuikly::util::json::NewStringUTF16(nullptr, 0);
    }

    KRJSON owned = kuikly::util::json::NewStringUTF16(nullptr, units);
    const uint16_t *utf16 = kuikly::util::json::GetStringUTF16(owned, nullptr);
    if (utf16 == nullptr) {
        kuikly::util::json::Release(owned);
        return kuikly::util::json::NewStringUTF16(nullptr, 0);
    }
    char16_t *dst = reinterpret_cast<char16_t *>(const_cast<uint16_t *>(utf16));
    size_t copied = 0;
    if (get(dst, units + 1, &copied) != ok) {
        kuikly::util::json::Release(owned);
        return kuikly::util::json::NewStringUTF16(nullptr, 0);
    }
    return owned;
}

struct KRJSONWrap {
    uint32_t magic;
    KRJSON value;
};

struct KRJSONRoutePayloadEntry {
    KRJSON value;
    std::chrono::steady_clock::time_point expires_at;
    // Registrations not yet taken; the entry is erased when the last one is taken or it expires.
    uint32_t pending;
};

std::mutex g_krjson_route_payload_mutex;
std::unordered_map<std::string, KRJSONRoutePayloadEntry> g_krjson_route_payloads;
uint64_t g_krjson_route_payload_next_id = 1;

void PruneExpiredKRJSONRoutePayloadsLocked(std::chrono::steady_clock::time_point now) {
    for (auto it = g_krjson_route_payloads.begin(); it != g_krjson_route_payloads.end();) {
        if (it->second.expires_at < now) {
            kuikly::util::json::Release(it->second.value);
            it = g_krjson_route_payloads.erase(it);
        } else {
            ++it;
        }
    }
}

std::string RegisterKRJSONRoutePayload(KRJSON value) {
    const auto now = std::chrono::steady_clock::now();
    std::lock_guard<std::mutex> lock(g_krjson_route_payload_mutex);
    PruneExpiredKRJSONRoutePayloadsLocked(now);
    // toJSON runs on every JSON.stringify of the wrapper (router, logging); keep one entry per value.
    for (auto &entry : g_krjson_route_payloads) {
        if (entry.second.value == value) {
            entry.second.expires_at = now + kKRJSONRoutePayloadMaxAge;
            ++entry.second.pending;
            return entry.first;
        }
    }
    const std::string token = std::to_string(
        std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch()).count()) +
        "-" + std::to_string(g_krjson_route_payload_next_id++);
    g_krjson_route_payloads.emplace(
        token,
        KRJSONRoutePayloadEntry{
            kuikly::util::json::Retain(value),
            now + kKRJSONRoutePayloadMaxAge,
            1,
        });
    return token;
}

KRJSON TakeKRJSONRoutePayloadByToken(const std::string &token) {
    const auto now = std::chrono::steady_clock::now();
    std::lock_guard<std::mutex> lock(g_krjson_route_payload_mutex);
    PruneExpiredKRJSONRoutePayloadsLocked(now);
    const auto it = g_krjson_route_payloads.find(token);
    if (it == g_krjson_route_payloads.end()) {
        return KRJSON_INVALID;
    }
    const KRJSON value = it->second.value;
    if (--it->second.pending > 0) {
        return kuikly::util::json::Retain(value);
    }
    g_krjson_route_payloads.erase(it);
    return value;
}

void FinalizeKRJSONWrap(napi_env, void *data, void *) {
    auto *wrap = static_cast<KRJSONWrap *>(data);
    if (wrap == nullptr) {
        return;
    }
    if (wrap->magic == kKRJSONWrapMagic) {
        wrap->magic = 0;
        kuikly::util::json::Release(wrap->value);
    }
    delete wrap;
}

KRJSONWrap *UnwrapKRJSON(napi_env env, napi_callback_info info, size_t argc, napi_value *args) {
    napi_value self = nullptr;
    if (napi_get_cb_info(env, info, &argc, args, &self, nullptr) != napi_ok || self == nullptr) {
        return nullptr;
    }
    bool matches = false;
    if (napi_check_object_type_tag(env, self, &kKRJSONTypeTag, &matches) != napi_ok || !matches) {
        return nullptr;
    }
    void *raw = nullptr;
    if (napi_unwrap(env, self, &raw) != napi_ok || raw == nullptr) {
        return nullptr;
    }
    auto *wrap = static_cast<KRJSONWrap *>(raw);
    return wrap->magic == kKRJSONWrapMagic ? wrap : nullptr;
}

napi_status ToNapiBytes(napi_env env, KRJSON value, napi_value *result) {
    size_t size = 0;
    const uint8_t *source = kuikly::util::json::GetBytes(value, &size);
    void *destination = nullptr;
    napi_value array_buffer = nullptr;
    napi_status status = napi_create_arraybuffer(env, size, &destination, &array_buffer);
    if (status == napi_ok && size > 0) {
        std::memcpy(destination, source, size);
    }
    if (status == napi_ok) {
        status = napi_create_typedarray(env, napi_int8_array, size, array_buffer, 0, result);
    }
    return status;
}

napi_value MakeNapiFromKRJSON(napi_env env, KRJSON value) {
    if (value == KRJSON_INVALID) {
        napi_value undefined = nullptr;
        napi_get_undefined(env, &undefined);
        return undefined;
    }
    napi_status status = napi_ok;
    return NapiValue::FromRenderValue(env, KRRenderValue::MakeBorrowed(value), &status).value;
}

bool ReadUTF16Key(napi_env env, napi_value value, std::u16string *out) {
    if (out == nullptr || value == nullptr) {
        return false;
    }
    napi_valuetype type = napi_undefined;
    if (napi_typeof(env, value, &type) != napi_ok || type != napi_string) {
        return false;
    }
    kuikly::util::GetNApiArgsStdU16String(env, value, *out);
    return true;
}

KRJSON GetObjectChild(KRJSON object, const std::u16string &key) {
    return kuikly::util::json::ObjectGetUTF16(
        object, reinterpret_cast<const uint16_t *>(key.data()), key.size());
}

napi_value KRJsonGet(napi_env env, napi_callback_info info) {
    napi_value args[1] = {nullptr};
    auto *wrap = UnwrapKRJSON(env, info, 1, args);
    std::u16string key;
    if (wrap == nullptr || args[0] == nullptr || !ReadUTF16Key(env, args[0], &key) ||
        kuikly::util::json::GetType(wrap->value) != KRJSON_OBJECT) {
        napi_value undefined = nullptr;
        napi_get_undefined(env, &undefined);
        return undefined;
    }
    return MakeNapiFromKRJSON(env, GetObjectChild(wrap->value, key));
}

napi_value KRJsonHas(napi_env env, napi_callback_info info) {
    napi_value args[1] = {nullptr};
    auto *wrap = UnwrapKRJSON(env, info, 1, args);
    std::u16string key;
    bool has = wrap != nullptr && args[0] != nullptr && ReadUTF16Key(env, args[0], &key) &&
               kuikly::util::json::GetType(wrap->value) == KRJSON_OBJECT &&
               GetObjectChild(wrap->value, key) != KRJSON_INVALID;
    napi_value result = nullptr;
    napi_get_boolean(env, has, &result);
    return result;
}

napi_value KRJsonAt(napi_env env, napi_callback_info info) {
    napi_value args[1] = {nullptr};
    auto *wrap = UnwrapKRJSON(env, info, 1, args);
    int32_t index = -1;
    if (wrap == nullptr || args[0] == nullptr ||
        napi_get_value_int32(env, args[0], &index) != napi_ok || index < 0 ||
        kuikly::util::json::GetType(wrap->value) != KRJSON_ARRAY) {
        napi_value undefined = nullptr;
        napi_get_undefined(env, &undefined);
        return undefined;
    }
    return MakeNapiFromKRJSON(env, kuikly::util::json::ArrayGet(wrap->value, index));
}

napi_value KRJsonToJSONString(napi_env env, napi_callback_info info) {
    napi_value args[1] = {nullptr};
    auto *wrap = UnwrapKRJSON(env, info, 0, args);
    const std::string json = wrap == nullptr ? std::string() : kuikly::util::json::Dump(wrap->value);
    napi_value result = nullptr;
    napi_create_string_utf8(env, json.data(), json.size(), &result);
    return result;
}

napi_value KRJsonToJSON(napi_env env, napi_callback_info info) {
    napi_value args[1] = {nullptr};
    auto *wrap = UnwrapKRJSON(env, info, 0, args);
    napi_value result = nullptr;
    if (wrap == nullptr || napi_create_object(env, &result) != napi_ok) {
        return nullptr;
    }
    const std::string token = RegisterKRJSONRoutePayload(wrap->value);
    napi_value token_value = nullptr;
    if (napi_create_string_utf8(env, token.data(), token.size(), &token_value) != napi_ok ||
        napi_set_named_property(env, result, kKRJSONRoutePayloadTokenKey, token_value) != napi_ok) {
        return nullptr;
    }
    return result;
}

napi_value KRJsonGetType(napi_env env, napi_callback_info info) {
    napi_value args[1] = {nullptr};
    auto *wrap = UnwrapKRJSON(env, info, 0, args);
    napi_value result = nullptr;
    napi_create_int32(env, wrap == nullptr ? KRJSON_NULL : kuikly::util::json::GetType(wrap->value), &result);
    return result;
}

napi_value KRJsonGetSize(napi_env env, napi_callback_info info) {
    napi_value args[1] = {nullptr};
    auto *wrap = UnwrapKRJSON(env, info, 0, args);
    napi_value result = nullptr;
    napi_create_double(env, wrap == nullptr ? 0 : static_cast<double>(kuikly::util::json::GetSize(wrap->value)),
                       &result);
    return result;
}

KRJSON FromNapi(napi_env env, napi_value value, int depth) {
    if (depth > kMaxBridgeDepth) {
        return kuikly::util::json::NewNull();
    }
    KRJSON wrapped = KRJSON_INVALID;
    if (TryUnwrapKRJSON(env, value, &wrapped)) {
        return kuikly::util::json::Retain(wrapped);
    }
    napi_valuetype value_type = napi_undefined;
    napi_typeof(env, value, &value_type);
    if (value_type == napi_boolean) {
        bool result = false;
        napi_get_value_bool(env, value, &result);
        return kuikly::util::json::NewBool(result);
    }
    if (value_type == napi_number) {
        double result = 0;
        napi_get_value_double(env, value, &result);
        // Inside containers, integral numbers keep JSON integer form, matching JSON.stringify and
        // the historical cJSON text. Top-level numbers stay double as before.
        return depth > 0 ? kuikly::util::json::NewIntIfSafeIntegral(result) : kuikly::util::json::NewDouble(result);
    }
    if (value_type == napi_string) {
        return NewStringFromUTF16Get(
            [&](char16_t *buf, size_t bufsize, size_t *out) {
                return napi_get_value_string_utf16(env, value, buf, bufsize, out);
            },
            napi_ok);
    }
    if (value_type != napi_object) {
        return kuikly::util::json::NewNull();
    }

    bool is_array_buffer = false;
    napi_is_arraybuffer(env, value, &is_array_buffer);
    if (is_array_buffer) {
        void *data = nullptr;
        size_t size = 0;
        napi_get_arraybuffer_info(env, value, &data, &size);
        return kuikly::util::json::NewBytes(static_cast<const uint8_t *>(data), size);
    }

    ArkTS ark_ts(env);
    if (ark_ts.IsTypedArray(value)) {
        napi_typedarray_type typed_type;
        size_t length = 0;
        void *data = nullptr;
        napi_value array_buffer = nullptr;
        size_t byte_offset = 0;
        if (napi_get_typedarray_info(env, value, &typed_type, &length, &data, &array_buffer, &byte_offset) == napi_ok &&
            typed_type == napi_int8_array) {
            return kuikly::util::json::NewBytes(static_cast<const uint8_t *>(data), length);
        }
    }

    bool is_array = false;
    napi_is_array(env, value, &is_array);
    if (is_array) {
        uint32_t length = 0;
        napi_get_array_length(env, value, &length);
        KRJSON result = kuikly::util::json::NewArray();
        for (uint32_t i = 0; i < length; ++i) {
            napi_value element = nullptr;
            napi_get_element(env, value, i, &element);
            KRJSON child = FromNapi(env, element, depth + 1);
            kuikly::util::json::ArrayAppend(result, child);
            kuikly::util::json::Release(child);
        }
        return result;
    }

    KRJSON result = kuikly::util::json::NewObjectUTF16();
    napi_value names = nullptr;
    if (napi_get_property_names(env, value, &names) == napi_ok) {
        uint32_t count = 0;
        napi_get_array_length(env, names, &count);
        for (uint32_t i = 0; i < count; ++i) {
            napi_value key_value = nullptr;
            napi_value property = nullptr;
            std::u16string key;
            napi_get_element(env, names, i, &key_value);
            kuikly::util::GetNApiArgsStdU16String(env, key_value, key);
            napi_get_property(env, value, key_value, &property);
            KRJSON child = FromNapi(env, property, depth + 1);
            kuikly::util::json::ObjectPutUTF16(result, reinterpret_cast<const uint16_t *>(key.data()), key.size(),
                                               child);
            kuikly::util::json::Release(child);
        }
    }
    return result;
}
}  // namespace

napi_value WrapKRJSON(napi_env env, KRJSON value) {
    napi_value result = nullptr;
    if (env == nullptr || napi_create_object(env, &result) != napi_ok) {
        KR_LOG_ERROR_WITH_TAG("KRJsonNative") << "stage=wrap_create_object_failed";
        return nullptr;
    }
    auto *wrap = new KRJSONWrap{kKRJSONWrapMagic, kuikly::util::json::Retain(value)};
    if (napi_wrap(env, result, wrap, FinalizeKRJSONWrap, nullptr, nullptr) != napi_ok) {
        KR_LOG_ERROR_WITH_TAG("KRJsonNative") << "stage=wrap_napi_wrap_failed";
        FinalizeKRJSONWrap(env, wrap, nullptr);
        return nullptr;
    }
    (void)napi_type_tag_object(env, result, &kKRJSONTypeTag);
    napi_property_descriptor descriptors[] = {
        {"get", nullptr, KRJsonGet, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"has", nullptr, KRJsonHas, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"at", nullptr, KRJsonAt, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"toJSONString", nullptr, KRJsonToJSONString, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"toJSON", nullptr, KRJsonToJSON, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"type", nullptr, nullptr, KRJsonGetType, nullptr, nullptr, napi_default, nullptr},
        {"size", nullptr, nullptr, KRJsonGetSize, nullptr, nullptr, napi_default, nullptr},
    };
    if (napi_define_properties(env, result, sizeof(descriptors) / sizeof(descriptors[0]), descriptors) != napi_ok) {
        KR_LOG_ERROR_WITH_TAG("KRJsonNative") << "stage=wrap_define_properties_failed";
        return nullptr;
    }
    return result;
}

bool TryUnwrapKRJSON(napi_env env, napi_value value, KRJSON *out) {
    if (env == nullptr || value == nullptr || out == nullptr) {
        return false;
    }
    napi_valuetype type = napi_undefined;
    if (napi_typeof(env, value, &type) != napi_ok || type != napi_object) {
        return false;
    }
    bool matches = false;
    if (napi_check_object_type_tag(env, value, &kKRJSONTypeTag, &matches) != napi_ok || !matches) {
        return false;
    }
    void *raw = nullptr;
    if (napi_unwrap(env, value, &raw) != napi_ok || raw == nullptr) {
        return false;
    }
    auto *wrap = static_cast<KRJSONWrap *>(raw);
    if (wrap->magic != kKRJSONWrapMagic) {
        return false;
    }
    *out = wrap->value;
    return true;
}

napi_value TakeKRJSONRoutePayload(napi_env env, napi_value token) {
    std::string token_string;
    napi_valuetype type = napi_undefined;
    if (env == nullptr || token == nullptr ||
        napi_typeof(env, token, &type) != napi_ok || type != napi_string) {
        napi_value undefined = nullptr;
        napi_get_undefined(env, &undefined);
        return undefined;
    }
    kuikly::util::GetNApiArgsStdString(env, token, token_string);
    const KRJSON value = TakeKRJSONRoutePayloadByToken(token_string);
    if (value == KRJSON_INVALID) {
        napi_value undefined = nullptr;
        napi_get_undefined(env, &undefined);
        return undefined;
    }
    napi_value result = WrapKRJSON(env, value);
    kuikly::util::json::Release(value);
    return result;
}

KRRenderValue NapiValue::ToRenderValue() const {
    return KRRenderValue::Accessor::Adopt(FromNapi(env, value, 0));
}

KRRenderValue NapiValue::ToOpaqueRenderValue() const {
    return KRRenderValue::Accessor::Adopt(kuikly::util::json::NewOpaque(env, value));
}

NapiValue NapiValue::RawHandle(const KRRenderValue &render_value) {
    const void *opaque_env = nullptr;
    const void *handle = nullptr;
    if (!kuikly::util::json::GetOpaque(render_value.jsonValue(), &opaque_env, &handle)) {
        return NapiValue();
    }
    return NapiValue(static_cast<napi_env>(const_cast<void *>(opaque_env)),
                     static_cast<napi_value>(const_cast<void *>(handle)));
}

NapiValue NapiValue::FromRenderValue(napi_env env, const KRRenderValue &render_value, napi_status *status) {
    napi_status local = napi_ok;
    napi_value result = nullptr;
    if (render_value.isNapiValue()) {
        result = RawHandle(render_value).value;
        local = napi_ok;
    } else {
        switch (KRRenderValue::Accessor::Type(render_value)) {
            case KRJSON_BOOL:
                local = napi_get_boolean(env, render_value.toBool(), &result);
                break;
            case KRJSON_INT:
                local = render_value.isInt() ? napi_create_int32(env, render_value.toInt(), &result)
                                             : napi_create_int64(env, render_value.toLong(), &result);
                break;
            case KRJSON_LONG:
                local = napi_create_int64(env, render_value.toLong(), &result);
                break;
            case KRJSON_FLOAT:
            case KRJSON_DOUBLE:
            case KRJSON_UINT:
                local = napi_create_double(env, render_value.toDouble(), &result);
                break;
            case KRJSON_U16STRING: {
                size_t units = 0;
                const uint16_t *utf16 = kuikly::util::json::GetStringUTF16(render_value.jsonValue(), &units);
                local = napi_create_string_utf16(env, reinterpret_cast<const char16_t *>(utf16), units, &result);
                break;
            }
            case KRJSON_STRING: {
                size_t len = 0;
                const char *utf8 = kuikly::util::json::GetString(render_value.jsonValue(), &len);
                local = napi_create_string_utf8(env, utf8, len, &result);
                break;
            }
            case KRJSON_BYTES:
                local = ToNapiBytes(env, render_value.jsonValue(), &result);
                break;
            case KRJSON_ARRAY:
            case KRJSON_OBJECT:
                result = WrapKRJSON(env, render_value.jsonValue());
                local = result == nullptr ? napi_generic_failure : napi_ok;
                break;
            default:
                local = napi_get_null(env, &result);
                break;
        }
    }
    if (status != nullptr) {
        *status = local;
    }
    return NapiValue(env, result);
}

}  // namespace util
}  // namespace kuikly
