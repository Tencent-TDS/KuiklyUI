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

#include "libohos_render/api/include/Kuikly/KRAnyData.h"
#include "KRAnyDataInternal.h"
#include "libohos_render/utils/json/Box.h"

namespace {
const char *AnyDataUTF8(struct KRAnyDataInternal *internal) {
    if (internal == nullptr || internal->anyValue == nullptr) {
        return nullptr;
    }
    if (kuikly::util::json::GetType(internal->anyValue->jsonValue()) == KRJSON_U16STRING) {
        if (!internal->utf8_cache_valid) {
            internal->utf8_cache = internal->anyValue->toString();
            internal->utf8_cache_valid = true;
        }
        return internal->utf8_cache.c_str();
    }
    return kuikly::util::json::GetString(internal->anyValue->jsonValue(), nullptr);
}
}  // namespace

#ifdef __cplusplus
extern "C" {
#endif

bool KRAnyDataIsString(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isString();
}

bool KRAnyDataIsInt(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isInt();
}

bool KRAnyDataIsLong(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isLong();
}

bool KRAnyDataIsFloat(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isFloat() || internal->anyValue->isDouble();
}

bool KRAnyDataIsBool(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isBool();
}

bool KRAnyDataIsBytes(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isByteArray();
}

bool KRAnyDataIsArray(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isArray();
}

// 判断一个 KRAnyData 是否是 Map（字典/对象）类型
bool KRAnyDataIsMap(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return false;
    }
    return internal->anyValue->isMap();
}

int KRAnyDataVisitMap(KRAnyData data, KRAnyDataMapVisitor visitor, void *userData) {
    if (visitor == nullptr) {
        return KRANYDATA_INVALID_PARAM;
    }
    
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    if (!internal->anyValue->isMap()) {
        return KRANYDATA_TYPE_MISMATCH;
    }
    
    const KRJSON root = internal->anyValue.jsonValue();
    const size_t count = kuikly::util::json::GetSize(root);
    for (size_t i = 0; i < count; ++i) {
        const KRJSON child = kuikly::util::json::ObjectValueAt(root, i);
        if (child == KRJSON_INVALID) {
            continue;
        }
        size_t units = 0;
        const uint16_t *key16 = kuikly::util::json::ObjectKeyAtUTF16(root, i, &units);
        if (key16 == nullptr) {
            continue;
        }
        const std::string key = kuikly::util::json::UTF16ToUTF8(key16, units);
        visitor(key.c_str(), internal->Borrow(KRRenderValue::MakeBorrowed(child)), userData);
    }
    return KRANYDATA_SUCCESS;
}

// 根据 key 获取 Map 中对应的 value
int KRAnyDataGetMapValue(KRAnyData data, const char* key, KRAnyData* value) {
    if (value == nullptr || key == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    if (!internal->anyValue->isMap()) {
        return KRANYDATA_TYPE_MISMATCH;
    }
    
    auto child = internal->anyValue.opt(key);
    if (!child) {
        *value = nullptr;
        return KRANYDATA_KEY_NOT_FOUND;
    }
    *value = internal->Borrow(child);
    return KRANYDATA_SUCCESS;
}


const char *KRAnyDataGetString(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return nullptr;
    }
    return AnyDataUTF8(internal);
}

int KRAnyDataGetInt(KRAnyData data, int32_t* value) {
    if (value == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    *value = internal->anyValue->toInt();
    return KRANYDATA_SUCCESS;
}

int KRAnyDataGetLong(KRAnyData data, int64_t* value) {
    if (value == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    *value = internal->anyValue->toLong();
    return KRANYDATA_SUCCESS;
}

int KRAnyDataGetFloat(KRAnyData data, float* value) {
    if (value == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    *value = internal->anyValue->toFloat();
    return KRANYDATA_SUCCESS;
}

int KRAnyDataGetBool(KRAnyData data, bool* value) {
    if (value == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    *value = internal->anyValue->toBool();
    return KRANYDATA_SUCCESS;
}

int KRAnyDataGetBytes(KRAnyData data, const char** value, int *size) {
    if (value == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    size_t byteSize = 0;
    const uint8_t *bytes = kuikly::util::json::GetBytes(internal->anyValue->jsonValue(), &byteSize);
    *value = reinterpret_cast<const char *>(bytes);
    *size = static_cast<int>(byteSize);
    return 0;
}

int KRAnyDataGetStr(KRAnyData data, const char** value) {
    if (value == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    *value = AnyDataUTF8(internal);
    return KRANYDATA_SUCCESS;
}

int KRAnyDataGetArraySize(KRAnyData data, int* size) {
    if (size == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    *size = internal->anyValue->isArray() ? static_cast<int>(internal->anyValue.size()) : 0;
    return KRANYDATA_SUCCESS;
}

int KRAnyDataGetArrayElement(KRAnyData data, KRAnyData* value, int index) {
    if (value == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    if (!internal->anyValue->isArray() || index < 0 || static_cast<size_t>(index) >= internal->anyValue.size()) {
        return KRANYDATA_OUT_OF_INDEX;
    }
    *value = internal->Borrow(internal->anyValue.at(static_cast<size_t>(index)));
    return KRANYDATA_SUCCESS;
}

KRAnyData KRAnyDataCreate() {
    auto value = new KRAnyDataInternal();
    return value;
}

void KRAnyDataDestroy(KRAnyData data) {
    if (data == nullptr) {
        return;
    }
    delete (KRAnyDataInternal*)data;
}

KRAnyData KRAnyDataCreateInt(int32_t value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = KRRenderValue::Make(value);
    return data;
}

KRAnyData KRAnyDataCreateLong(int64_t value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = KRRenderValue::Make(value);
    return data;
}

KRAnyData KRAnyDataCreateFloat(float value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = KRRenderValue::Make(value);
    return data;
}

KRAnyData KRAnyDataCreateBool(bool value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = KRRenderValue::Make(value);
    return data;
}

KRAnyData KRAnyDataCreateString(const char* value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = KRRenderValue::MakeUTF16(value);
    return data;
}

KRAnyData KRAnyDataCreateBytes(const char* value, int size) {
    auto data = new KRAnyDataInternal();
    auto byteArray = std::make_shared<std::vector<uint8_t>>(value, value + size);
    data->anyValue = KRRenderValue::Make(byteArray);
    return data;
}

KRAnyData KRAnyDataCreateArray(int size) {
    auto data = new KRAnyDataInternal();
    KRRenderValue::Array valueArray;
    valueArray.reserve(size);
    for (int i = 0; i < size; ++i) {
        valueArray.emplace_back(KRRenderValue::Make());
    }
    data->anyValue = KRRenderValue::Make(valueArray);
    return data;
}

int KRAnyDataSetArrayElement(KRAnyData data, KRAnyData value, int index) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    struct KRAnyDataInternal *valueInternal = (struct KRAnyDataInternal *)value;
    if (valueInternal == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    if (!internal->anyValue) {
        internal->anyValue = KRRenderValue::Make();
    }

    if (internal->anyValue->isArray()) {
        if (index < 0 || static_cast<size_t>(index) >= internal->anyValue.size()) {
            return KRANYDATA_OUT_OF_INDEX;
        }
        if (kuikly::util::json::IsUnique(internal->anyValue.jsonValue())) {
            kuikly::util::json::ArraySet(internal->anyValue.jsonValue(), static_cast<size_t>(index),
                                         valueInternal->anyValue.jsonValue());
        } else {
            auto array = internal->anyValue->toArray();
            array[static_cast<size_t>(index)] = valueInternal->anyValue;
            internal->anyValue = KRRenderValue::Make(std::move(array));
        }
    } else {
        return KRANYDATA_TYPE_MISMATCH;
    }
    return KRANYDATA_SUCCESS;
}

int KRAnyDataAddArrayElement(KRAnyData data, KRAnyData value) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    struct KRAnyDataInternal *valueInternal = (struct KRAnyDataInternal *)value;
    if (valueInternal == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    if (!internal->anyValue) {
        internal->anyValue = KRRenderValue::Make();
    }

    if (internal->anyValue->isArray()) {
        if (kuikly::util::json::IsUnique(internal->anyValue.jsonValue())) {
            kuikly::util::json::ArrayAppend(internal->anyValue.jsonValue(), valueInternal->anyValue.jsonValue());
        } else {
            auto array = internal->anyValue->toArray();
            array.push_back(valueInternal->anyValue);
            internal->anyValue = KRRenderValue::Make(std::move(array));
        }
    } else {
        return KRANYDATA_TYPE_MISMATCH;
    }
    return KRANYDATA_SUCCESS;
}

#ifdef __cplusplus
}
#endif