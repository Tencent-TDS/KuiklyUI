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

#ifndef CORE_RENDER_OHOS_KRRENDERVALUE_H
#define CORE_RENDER_OHOS_KRRENDERVALUE_H

#include <ark_runtime/jsvm.h>
#include <ark_runtime/jsvm_types.h>
#include <js_native_api.h>
#include <js_native_api_types.h>
#include <cmath>
#include <cstdint>
#include <iostream>
#include <sstream>
#include <string>
#include <unordered_map>
#include <variant>
#include <vector>
#include "KRRenderCValue.h"
#include "libohos_render/foundation/ark_ts.h"
#include "libohos_render/foundation/type/KRRenderCValue.h"
#include "libohos_render/utils/KRJsUtil.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/NAPIUtil.h"
#include "thirdparty/cJSON/cJSON.h"

static std::string DoubleToString(double value) {
    std::ostringstream oss;
    oss << value;
    std::string str = oss.str();

    if (str.find(".") == std::string::npos) {  // 没有小数点的话直接返回
        return str;
    }

    // 去除小数点后所有的零
    std::size_t last_not_zero = str.find_last_not_of('0');
    if (last_not_zero != std::string::npos) {
        if (str[last_not_zero] == '.') {
            last_not_zero--;  // 保留小数点前的数字
        }
        str = str.substr(0, last_not_zero + 1);
    }
    return str;
}

struct NapiValue {
    NapiValue(napi_env e, napi_value v) : env(e), value(v) {}
    napi_env env;
    napi_value value;
};

/**
 * kotlin 侧与 Render 侧数据通信类型转换类
 * 设计上是一个 AnyValue to AnyValue 类的思想
 * 通过使用 toXX api, 可把值转换为对应的 value
 */
class KRRenderValue : public std::enable_shared_from_this<KRRenderValue> {
 public:
    using Map = std::unordered_map<std::string, std::shared_ptr<KRRenderValue>>;
    using Array = std::vector<std::shared_ptr<KRRenderValue>>;
    using ByteArray = std::shared_ptr<std::vector<uint8_t>>;

    KRRenderValue() {
        value_ = std::monostate();
        json_to_map_or_array_value_ = std::monostate();
        c_value_.type = KRRenderCValue::Type::NULL_VALUE;
    }

    explicit KRRenderValue(std::nullptr_t) : KRRenderValue() {}

    explicit KRRenderValue(bool value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(int32_t value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(int64_t value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(float value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(double value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(const std::string &value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(const char *value) : KRRenderValue() {
        value_ = std::string(value);
    }

    explicit KRRenderValue(const Map &value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(const Array &value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(const ByteArray &value) : KRRenderValue() {
        value_ = value;
    }

    explicit KRRenderValue(const KRRenderCValue &cValue) : KRRenderValue() {
        if (cValue.type == KRRenderCValue::Type::BOOL) {
            value_ = cValue.value.boolValue != 0;
        } else if (cValue.type == KRRenderCValue::Type::INT) {
            value_ = cValue.value.intValue;
        } else if (cValue.type == KRRenderCValue::Type::LONG) {
            value_ = cValue.value.longValue;
        } else if (cValue.type == KRRenderCValue::Type::FLOAT) {
            value_ = cValue.value.floatValue;
        } else if (cValue.type == KRRenderCValue::Type::DOUBLE) {
            value_ = cValue.value.doubleValue;
        } else if (cValue.type == KRRenderCValue::Type::STRING) {
            value_ = std::string(cValue.value.stringValue);
        } else if (cValue.type == KRRenderCValue::Type::BYTES) {
            auto length = cValue.size;
            auto start_address = cValue.value.bytesValue;
            auto bytes = std::make_shared<std::vector<uint8_t>>();
            for (int i = 0; i < length; i++) {
                bytes->push_back(*(reinterpret_cast<uint8_t *>(start_address + i)));
            }
            value_ = bytes;
        } else if (cValue.type == KRRenderCValue::Type::ARRAY) {
            auto array_size = cValue.size;
            Array array;
            for (int i = 0; i < array_size; i++) {
                array.push_back(std::make_shared<KRRenderValue>(cValue.value.arrayValue[i]));
            }
            value_ = array;
        } else {
            value_ = std::monostate();
        }
    }

    explicit KRRenderValue(const NapiValue &value) : KRRenderValue() {
        value_ = value;
    }

    KRRenderValue(const napi_env &napi_env, const napi_value &nvalue) : KRRenderValue() {
        napi_valuetype napi_value_type;
        napi_typeof(napi_env, nvalue, &napi_value_type);
        if (napi_value_type == napi_boolean) {
            auto r = false;
            napi_get_value_bool(napi_env, nvalue, &r);
            value_ = r;
        } else if (napi_value_type == napi_number) {
            auto r = 0.0;
            napi_get_value_double(napi_env, nvalue, &r);
            value_ = r;
        } else if (napi_value_type == napi_string) {
            std::string str;
            kuikly::util::GetNApiArgsStdString(napi_env, nvalue, str);
            value_ = std::move(str);
        } else {
            bool is_byte_array = false;
            napi_is_arraybuffer(napi_env, nvalue, &is_byte_array);
            if (is_byte_array) {
                void *byte_array = nullptr;
                size_t byte_length;
                napi_get_arraybuffer_info(napi_env, nvalue, &byte_array, &byte_length);
                auto bytes = std::make_shared<std::vector<uint8_t>>();
                auto byte_buffer = reinterpret_cast<uint8_t *>(byte_array);
                for (int i = 0; i < byte_length; i++) {
                    bytes->push_back(*(byte_buffer + i));
                }
                value_ = bytes;
                return;
            }

            ArkTS arkTs(napi_env);
            if (arkTs.IsTypedArray(nvalue)) {
                napi_typedarray_type typedArrayType;
                size_t typedArrayLength;
                void *typedArrayData;
                napi_value arraybuffer;
                size_t byteOffset = 0;
                napi_status status = napi_get_typedarray_info(napi_env, nvalue, &typedArrayType, &typedArrayLength,
                                                              &typedArrayData, &arraybuffer, &byteOffset);
                if (status == napi_ok && typedArrayType == napi_int8_array) {
                    if (typedArrayData != nullptr) {
                        value_ = std::make_shared<std::vector<uint8_t>>(byteOffset + (const char *)typedArrayData,
                                                                        typedArrayLength + byteOffset +
                                                                            (const char *)typedArrayData);
                    } else {
                        value_ = std::make_shared<std::vector<uint8_t>>();
                    }
                    return;
                }
            }

            bool is_array = false;
            napi_is_array(napi_env, nvalue, &is_array);
            if (is_array) {
                uint32_t array_length;
                napi_get_array_length(napi_env, nvalue, &array_length);
                Array array;
                for (int i = 0; i < array_length; i++) {
                    napi_value value;
                    napi_get_element(napi_env, nvalue, i, &value);
                    array.push_back(std::make_shared<KRRenderValue>(napi_env, value));
                }
                value_ = array;
                return;
            }

            value_ = std::monostate();
        }
    }

    KRRenderValue(const JSVM_Env &js_env, const JSVM_Value &js_value) : KRRenderValue() {
        JSVM_ValueType js_value_type;
        OH_JSVM_Typeof(js_env, js_value, &js_value_type);
        if (js_value_type == JSVM_BOOLEAN) {
            auto r = false;
            OH_JSVM_GetValueBool(js_env, js_value, &r);
            value_ = r;
        } else if (js_value_type == JSVM_NUMBER) {
            auto r = 0.0;
            OH_JSVM_GetValueDouble(js_env, js_value, &r);
            value_ = r;
        } else if (js_value_type == JSVM_STRING) {
            std::string str;
            kuikly::util::get_str_from_js_str(js_env, js_value, str);
            value_ = std::move(str);
        } else {
            bool is_byte_array = false;
            OH_JSVM_IsArraybuffer(js_env, js_value, &is_byte_array);
            if (is_byte_array) {
                void *byte_array = nullptr;
                size_t byte_length;
                OH_JSVM_GetArraybufferInfo(js_env, js_value, &byte_array, &byte_length);
                auto bytes = std::make_shared<std::vector<uint8_t>>();
                auto byte_buffer = reinterpret_cast<uint8_t *>(byte_array);
                for (int i = 0; i < byte_length; i++) {
                    bytes->push_back(*(byte_buffer + i));
                }
                value_ = bytes;
                return;
            }
            bool is_type_array;
            OH_JSVM_IsTypedarray(js_env, js_value, &is_type_array);
            if (is_type_array) {
                JSVM_TypedarrayType type;
                size_t length = 0;
                void *data = nullptr;
                JSVM_Value retArrayBuffer;
                size_t byteOffset = -1;
                OH_JSVM_GetTypedarrayInfo(js_env, js_value, &type, &length, &data, &retArrayBuffer, &byteOffset);
                auto bytes = std::make_shared<std::vector<uint8_t>>();
                auto byte_buffer = reinterpret_cast<uint8_t *>(data);
                for (int i = 0; i < length; i++) {
                    bytes->push_back(*(byte_buffer + i));
                }
                value_ = bytes;
                return;
            }

            bool is_array = false;
            OH_JSVM_IsArray(js_env, js_value, &is_array);
            if (is_array) {
                uint32_t array_length;
                OH_JSVM_GetArrayLength(js_env, js_value, &array_length);
                Array array;
                for (int i = 0; i < array_length; i++) {
                    JSVM_Value value;
                    OH_JSVM_GetElement(js_env, js_value, i, &value);
                    array.push_back(std::make_shared<KRRenderValue>(js_env, value));
                }
                value_ = array;
                return;
            }
            value_ = std::monostate();
        }
    }

    bool isNull() const {
        return std::holds_alternative<std::monostate>(value_);
    }

    bool isBool() const {
        return std::holds_alternative<bool>(value_);
    }

    bool isInt() const {
        return std::holds_alternative<int32_t>(value_);
    }

    bool isLong() const {
        return std::holds_alternative<int64_t>(value_);
    }

    bool isFloat() const {
        return std::holds_alternative<float>(value_);
    }

    bool isDouble() const {
        return std::holds_alternative<double>(value_);
    }

    bool isString() const {
        return std::holds_alternative<std::string>(value_);
    }

    bool isMap() const {
        return std::holds_alternative<Map>(value_);
    }

    bool isArray() const {
        return std::holds_alternative<Array>(value_);
    }

    bool isByteArray() const {
        return std::holds_alternative<ByteArray>(value_);
    }

    bool isNapiValue() const {
        return std::holds_alternative<NapiValue>(value_);
    }

    struct NapiValue toNapiValue() const {
        if (isNapiValue()) {
            return std::get<NapiValue>(value_);
        }
        return NapiValue(nullptr, nullptr);
    }

    bool toBool() const {
        if (isBool()) {
            return std::get<bool>(value_);
        }
        return toDouble() != 0.0;
    }

    int32_t toInt() const {
        if (isInt()) {
            return std::get<int32_t>(value_);
        }
        return static_cast<int32_t>(toDouble());
    }

    int64_t toLong() const {
        if (isLong()) {
            return std::get<int64_t>(value_);
        }
        return static_cast<int64_t>(toDouble());
    }

    float toFloat() const {
        float value = 0;
        if (isFloat()) {
            value = std::get<float>(value_);
        } else {
            value = static_cast<float>(toDouble());
        }
        if (std::isnan(value)) {
            value = 0;
        }
        return value;
    }

    double toDouble() const {
        if (isDouble()) {
            return std::get<double>(value_);
        } else if (isLong()) {
            return static_cast<double>(std::get<int64_t>(value_));
        } else if (isFloat()) {
            return static_cast<double>(std::get<float>(value_));
        } else if (isInt()) {
            return static_cast<double>(std::get<int32_t>(value_));
        } else if (isBool()) {
            return static_cast<double>(std::get<bool>(value_));
        } else if (isString()) {
            try {
                auto string = toString();
                if (string.length() == 0) {
                    return 0;
                }
                return std::stod(string);
            } catch (...) {
                return 0;
            }
        } else {
            return 0.0;
        }
    }

    const std::string &toString() const {
        if (isString()) {
            return std::get<std::string>(value_);
        }
        if (isBool() || isInt() || isDouble() || isFloat() || isLong()) {  // number to string
            auto numberString = DoubleToString(toDouble());
            if (numberString == "0") {
                outputToStringResult_ = std::string("");
                return outputToStringResult_;
            }
            outputToStringResult_ = numberString;
            return outputToStringResult_;
        }
        if (isMap() || isArray()) {  // map or array to string
            cJSON* cjson = toJson( shared_from_this());
            if(char* p = cJSON_Print(cjson)){
                outputToStringResult_ = p;
                cJSON_free(p);
            }
            cJSON_Delete(cjson);
            return outputToStringResult_;
        }
        outputToStringResult_ = "";
        return outputToStringResult_;
    }

    const Map &toMap() const {
        if (isMap()) {
            return std::get<Map>(value_);
        } else if (std::holds_alternative<Map>(json_to_map_or_array_value_)) {
            return std::get<Map>(json_to_map_or_array_value_);
        } else if (isString()) {
            cJSON *cjson = cJSON_Parse(toString().c_str());
            if(cjson == nullptr){
                json_to_map_or_array_value_ = Map();
                return std::get<Map>(json_to_map_or_array_value_);
            }
            Map map;
            for (cJSON *item = cjson->child; item != NULL; item = item->next) {
                map[item->string] = fromJsonValue(item);
            }
            json_to_map_or_array_value_ = map;
            return std::get<Map>(json_to_map_or_array_value_);
        } else {
            json_to_map_or_array_value_ = Map();
            return std::get<Map>(json_to_map_or_array_value_);
        }
    }

    const Array &toArray() const {
        if (isArray()) {
            return std::get<Array>(value_);
        } else if (std::holds_alternative<Array>(json_to_map_or_array_value_)) {
            return std::get<Array>(json_to_map_or_array_value_);
        } else if (isString()) {
            cJSON *cjson = cJSON_Parse(toString().c_str());
            if(cjson == nullptr){
                json_to_map_or_array_value_ = Array();
                return std::get<Array>(json_to_map_or_array_value_);
            }
            Array json_vec;
            for(int i = 0; i < cJSON_GetArraySize(cjson); ++i){
                json_vec.push_back(fromJsonValue(cJSON_GetArrayItem(cjson, i)));
            }
            json_to_map_or_array_value_ = json_vec;
            return std::get<Array>(json_to_map_or_array_value_);
        } else {
            json_to_map_or_array_value_ = Array();
            return std::get<Array>(json_to_map_or_array_value_);
        }
    }

    const ByteArray toByteArray() const {
        if (isByteArray()) {
            return std::get<ByteArray>(value_);
        } else {
            return std::make_shared<std::vector<uint8_t>>();
        }
    }

    const KRRenderCValue &toCValue() const {
        if (c_value_.type != KRRenderCValue::Type::NULL_VALUE) {
            return c_value_;
        }

        if (isBool()) {
            c_value_.type = KRRenderCValue::Type::BOOL;
            c_value_.value.boolValue = toBool() ? 1 : 0;
        } else if (isInt()) {
            c_value_.type = KRRenderCValue::Type::INT;
            c_value_.value.intValue = toInt();
        } else if (isLong()) {
            c_value_.type = KRRenderCValue::Type::LONG;
            c_value_.value.longValue = toLong();
        } else if (isFloat()) {
            c_value_.type = KRRenderCValue::Type::FLOAT;
            c_value_.value.floatValue = toFloat();
        } else if (isDouble()) {
            c_value_.type = KRRenderCValue::Type::DOUBLE;
            c_value_.value.doubleValue = toDouble();
        } else if (isString()) {
            c_value_.type = KRRenderCValue::Type::STRING;
            c_value_.value.stringValue = const_cast<char *>(toString().c_str());
        } else if (isByteArray()) {
            c_value_.type = KRRenderCValue::Type::BYTES;
            auto byte_array = std::get<ByteArray>(value_).get();
            c_value_.size = byte_array->size();
            c_value_.value.bytesValue = reinterpret_cast<char *>(byte_array->data());
        } else if (isMap()) {
            ToJsonMapOrArray();
        } else if (isArray()) {
            auto &array = toArray();
            if (HadByteArrayElement(array)) {  // 有二进制元素的话, 不进行 json 序列化，直接传递数组
                c_value_.type = KRRenderCValue::Type::ARRAY;
                c_value_.size = array.size();
                array_ptr_ = new KRRenderCValue[c_value_.size];
                for (int i = 0; i < c_value_.size; i++) {
                    auto &item = array[i];
                    array_ptr_[i] = item->toCValue();
                }
                c_value_.value.arrayValue = array_ptr_;
            } else {
                ToJsonMapOrArray();
            }
        } else {
            c_value_.type = KRRenderCValue::Type::NULL_VALUE;
        }

        return c_value_;
    }

    void ToJsVmValue(JSVM_Env js_env, JSVM_Value *js_value, JSVM_Status &js_status) const {
        if (isBool()) {
            js_status = OH_JSVM_GetBoolean(js_env, toBool(), js_value);
        } else if (isInt()) {
            js_status = OH_JSVM_CreateInt32(js_env, toInt(), js_value);
        } else if (isLong()) {
            js_status = OH_JSVM_CreateInt64(js_env, toLong(), js_value);
        } else if (isFloat() || isDouble()) {
            js_status = OH_JSVM_CreateDouble(js_env, toDouble(), js_value);
        } else if (isString()) {
            auto str = toString();
            js_status = OH_JSVM_CreateStringUtf8(js_env, str.c_str(), str.size(), js_value);
        } else if (isByteArray()) {
            auto &data = toByteArray();
            auto size = data->size();
            void *buffer = nullptr;
            JSVM_Value array_buffer_value = nullptr;
            js_status = OH_JSVM_CreateArraybuffer(js_env, size, &buffer, &array_buffer_value);
            if (js_status == JSVM_OK) {
                auto byte_buffer = reinterpret_cast<uint8_t *>(buffer);
                auto origin_buffer = data.get()->data();
                for (int i = 0; i < size; i++) {
                    byte_buffer[i] = origin_buffer[i];
                }
            }
            OH_JSVM_CreateTypedarray(js_env, JSVM_TypedarrayType::JSVM_INT8_ARRAY, size, array_buffer_value, 0,
                                     js_value);
        } else if (isMap()) {
            js_status = ToJsonMapOrArray(js_env, js_value);
        } else if (isArray()) {
            auto &array = toArray();
            if (HadByteArrayElement(array)) {  // 有二进制元素的话, 不进行 json 序列化，直接传递数组
                auto size = array.size();
                js_status = OH_JSVM_CreateArrayWithLength(js_env, size, js_value);
                if (js_status == JSVM_Status::JSVM_OK) {
                    for (int i = 0; i < size; i++) {
                        JSVM_Status status;
                        JSVM_Value value;
                        array[i]->ToJsVmValue(js_env, &value, status);
                        OH_JSVM_SetElement(js_env, *js_value, i, value);
                    }
                }
            } else {
                js_status = ToJsonMapOrArray(js_env, js_value);
            }
        } else {
            js_status = OH_JSVM_GetNull(js_env, js_value);
        }
    }

    void ToNapiValue(const napi_env &env, napi_value *nvalue, napi_status &nstatus) const {
        if (isBool()) {
            nstatus = napi_get_boolean(env, toBool(), nvalue);
        } else if (isInt()) {
            nstatus = napi_create_int32(env, toInt(), nvalue);
        } else if (isLong()) {
            nstatus = napi_create_int64(env, toLong(), nvalue);
        } else if (isFloat() || isDouble()) {
            nstatus = napi_create_double(env, toDouble(), nvalue);
        } else if (isString()) {
            auto str = toString();
            nstatus = napi_create_string_utf8(env, str.c_str(), str.size(), nvalue);
        } else if (isByteArray()) {
            auto &data = toByteArray();
            auto size = data->size();
            void *buffer = nullptr;
            napi_value arrayBuffer;
            nstatus = napi_create_arraybuffer(env, size, &buffer, &arrayBuffer);
            if (nstatus == napi_ok) {
                auto byte_buffer = reinterpret_cast<uint8_t *>(buffer);
                auto origin_buffer = data.get()->data();
                for (int i = 0; i < size; i++) {
                    byte_buffer[i] = origin_buffer[i];
                }
                nstatus = napi_create_typedarray(env, napi_int8_array, size, arrayBuffer, 0, nvalue);
            }
        } else if (isMap()) {
            nstatus = ToJsonMapOrArray(env, nvalue);
        } else if (isArray()) {
            auto &array = toArray();
#if 0
            if (HadByteArrayElement(array)) {
#endif
            auto size = array.size();
            nstatus = napi_create_array_with_length(env, size, nvalue);
            if (nstatus == napi_ok) {
                for (int i = 0; i < size; i++) {
                    napi_status status;
                    napi_value napi_value;
                    array[i]->ToNapiValue(env, &napi_value, status);
                    napi_set_element(env, *nvalue, i, napi_value);
                }
            }
#if 0
            } else {
                nstatus = ToJsonMapOrArray(env, nvalue);
            }
#endif
        } else {
            nstatus = napi_get_null(env, nvalue);
        }
    }

    ~KRRenderValue() {
        if (array_ptr_) {
            delete[] array_ptr_;
            array_ptr_ = nullptr;
        }
    }

 private:
    std::variant<std::monostate, bool, int32_t, int64_t, float, double, std::string, Map, Array, void *, ByteArray,
                 NapiValue>
        value_;
    mutable std::string map_or_array_json_value_;  // 缓存经过序列化的 map或者 array, 用于缓存经过序列化的std::string
    mutable KRRenderCValue c_value_;
    mutable std::variant<std::monostate, Map, Array> json_to_map_or_array_value_;
    mutable std::string outputToStringResult_;
    mutable KRRenderCValue *array_ptr_ = nullptr;  // 指向数组的指针, 用于防止数组元素copy

    const void ToJsonMapOrArray() const {
        cJSON* cjson = toJson( shared_from_this());
        char* p = cJSON_PrintUnformatted(cjson);
        map_or_array_json_value_ = p;
        c_value_.type = KRRenderCValue::Type::STRING;
        c_value_.value.stringValue = const_cast<char *>(map_or_array_json_value_.c_str());
        cJSON_free(p);
        cJSON_Delete(cjson);
    }

    const JSVM_Status ToJsonMapOrArray(JSVM_Env js_env, JSVM_Value *js_value) const {
        cJSON* cjson = toJson( shared_from_this());
        if(char* p = cJSON_PrintUnformatted(cjson)){
            map_or_array_json_value_ = p;
            cJSON_free(p);
        }
        cJSON_Delete(cjson);
        return OH_JSVM_CreateStringUtf8(js_env, map_or_array_json_value_.c_str(), map_or_array_json_value_.length(),
                                        js_value);
    }

    const napi_status ToJsonMapOrArray(const napi_env &env, napi_value *nvalue) const {
        cJSON* cjson = toJson( shared_from_this());
        if(char* p = cJSON_PrintUnformatted(cjson)){
            map_or_array_json_value_ = p;
            cJSON_free(p);
        }
        cJSON_Delete(cjson);
        return napi_create_string_utf8(env, map_or_array_json_value_.c_str(), map_or_array_json_value_.length(),
                                       nvalue);
    }

    const bool HadByteArrayElement(const Array &array) const {
        for (auto &item : array) {
            if (item->isByteArray()) {
                return true;
            }
        }
        return false;
    }
    static cJSON *toJson(const std::shared_ptr<const KRRenderValue> &value) {
        if (value->isMap()) {
            cJSON* obj = cJSON_CreateObject();
            const auto &map = value->toMap();
            for (const auto &entry : map) {
                cJSON* child = toJson(entry.second);
                cJSON_AddItemToObject(obj, entry.first.c_str(), child);
            }
            return obj;
        } else if (value->isArray()) {
            cJSON* arr = cJSON_CreateArray();
            const auto &array = value->toArray();
            for (const auto &element : array) {
                cJSON* child = toJson( element);
                cJSON_AddItemToArray(arr, child);
            }
            return arr;
        } else if (value->isBool()) {
            return cJSON_CreateBool(value->toBool());
        } else if (value->isInt()) {
            return cJSON_CreateNumber(static_cast<double>(value->toInt()));
        } else if (value->isLong()) {
            return cJSON_CreateNumber(static_cast<double>(value->toInt()));
        } else if (value->isFloat()) {
            return cJSON_CreateNumber(static_cast<double>(value->toInt()));
        } else if (value->isDouble()) {
            return cJSON_CreateNumber(value->toDouble());
        } else if (value->isString()) {
            return cJSON_CreateString(value->toString().c_str());
        } else {
            return cJSON_CreateNull();
        }
    }

    static std::shared_ptr<KRRenderValue> fromJsonValue(const cJSON *cjson) {
        if(cjson == nullptr){
            return std::make_shared<KRRenderValue>();;
        }
        if (cJSON_IsBool(cjson)) {
            return std::make_shared<KRRenderValue>(cJSON_IsTrue(cjson));
        } else if (cJSON_IsNumber(cjson)) {
            return std::make_shared<KRRenderValue>(cJSON_GetNumberValue(cjson));
        } else if (cJSON_IsString(cjson)) {
            return std::make_shared<KRRenderValue>(cJSON_GetStringValue(cjson));
        } else if (cJSON_IsObject(cjson)) {
            Map map_obj;
            for (cJSON *item = cjson->child; item != NULL; item = item->next) {
                map_obj[item->string] = fromJsonValue(item);
            }
            return std::make_shared<KRRenderValue>(map_obj);
        } else if (cJSON_IsArray(cjson)) {
            Array vec_obj;
            for(int i = 0; i < cJSON_GetArraySize(cjson); ++i){
                vec_obj.push_back(fromJsonValue(cJSON_GetArrayItem(cjson, i)));
            }
            return std::make_shared<KRRenderValue>(vec_obj);
        } else {
            return std::make_shared<KRRenderValue>();  // Null JSValue
        }
    }
};

#endif  // CORE_RENDER_OHOS_KRRENDERVALUE_H
