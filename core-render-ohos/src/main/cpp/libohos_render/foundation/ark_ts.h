/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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
#pragma once

#include <arkui/native_type.h>
#include <js_native_api.h>
#include <js_native_api_types.h>
#include <array>
#include <functional>
#include <string>
#include <variant>
#include <vector>

// class OhNapiObjectBuilder;
// class OhNapiObject;

class ArkTS {
 public:
    explicit ArkTS(napi_env env);

    template <size_t args_count>
    napi_value Call(napi_value callback, std::array<napi_value, args_count> args, napi_value this_object = nullptr) {
        napi_value result;
        auto status = napi_call_function(env_, this_object, callback, args.size(), args.data(), &result);
        this->MaybeThrowFromStatus(status, "Couldn't call a callback");
        return result;
    }

    napi_value Call(napi_value callback, std::vector<napi_value> args, napi_value this_object = nullptr);

    napi_value Call(napi_value callback, const napi_value *args, int args_count, napi_value this_object);

    napi_value CreateBoolean(bool value);

    napi_value CreateInt(int value);

    napi_value CreateInt64(int64_t value);

    napi_value CreateUint32(uint32_t value);

    napi_value CreateDouble(double value);

    napi_value CreateString(std::string const &str);

    napi_value CreateStringUtf16(std::u16string const &str);

    napi_value CreateExternalArrayBuffer(void *external_data, size_t byte_length);
    napi_value CreateInt8Array(const void *external_data, size_t byte_length);

    napi_ref CreateReference(napi_value value);

    void DeleteReference(napi_ref reference);

    napi_value CreateArray();

    napi_value CreateArray(std::vector<napi_value>);

    //  OhNapiObjectBuilder CreateObjectBuilder();
    //
    //  OhNapiObjectBuilder GetObjectBuilder(napi_value object);

    bool IsPromise(napi_value);

    napi_value GetUndefined();

    napi_value GetNull();

    napi_value GetReferenceValue(napi_ref ref);

    std::vector<napi_value> GetCallbackArgs(napi_callback_info info, void **data = nullptr);

    std::vector<napi_value> GetCallbackArgs(napi_callback_info info, size_t args_count, void **data = nullptr);

    //  OhNapiObject GetObject(napi_value object);
    //
    //  OhNapiObject GetObject(napi_ref object_ref);

    napi_value GetObjectProperty(napi_value object, std::string const &key);

    napi_value GetObjectProperty(napi_value object, napi_value key);

    bool GetBoolean(napi_value value);

    double GetDouble(napi_value value);

    int GetInteger(napi_value value);

    int64_t GetInt64(napi_value value);

    bool IsArray(napi_value array);
    bool IsTypedArray(napi_value array);

    napi_value GetArrayElement(napi_value array, uint32_t index);

    uint32_t GetArrayLength(napi_value array);

    std::vector<std::pair<napi_value, napi_value>> GetObjectProperties(napi_value object);

    std::string GetString(napi_value value);

    bool GetArrayBufferInfo(napi_value value, void **data, size_t *byte_length);

    bool IsArrayBuffer(napi_value value);

    napi_valuetype GetType(napi_value value);

    napi_env GetEnv();

    void ThrowError(const char *message);

    void PrintValue(napi_value value);

    bool IsNull(napi_value value);

    bool IsUndefined(napi_value value);

 private:
    napi_env env_;

    void MaybeThrowFromStatus(napi_status status, const char *message);
};
