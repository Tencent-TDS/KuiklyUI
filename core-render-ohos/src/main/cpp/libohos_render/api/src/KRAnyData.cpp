/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
    return internal->anyValue->isDouble();
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

const char *KRAnyDataGetString(KRAnyData data) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return nullptr;
    }
    return internal->anyValue->toCValue().value.stringValue;
}

int KRAnyDataGetInt(KRAnyData data, int32_t* value) {
    if (value == nullptr) {
        return KRANYDATA_NULL_OUTPUT;
    }
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr || internal->anyValue == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    *value = internal->anyValue->toCValue().value.intValue;
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
    *value = internal->anyValue->toCValue().value.longValue;
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
    *value = internal->anyValue->toCValue().value.doubleValue;
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
    *value = internal->anyValue->toCValue().value.boolValue;
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
    auto cValue = internal->anyValue->toCValue();
    *value = cValue.value.bytesValue;
    *size = cValue.size; 
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
    auto cValue = internal->anyValue->toCValue();
    *value = cValue.value.stringValue;
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
    *size = internal->anyValue->toArray().size();
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
    auto array = internal->anyValue->toArray();
    if (index >= array.size()) {
        return KRANYDATA_OUT_OF_INDEX;
    }
    *value = array[index].get();
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
    data->anyValue = std::make_shared<KRRenderValue>(value);
    return data;
}

KRAnyData KRAnyDataCreateLong(int64_t value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = std::make_shared<KRRenderValue>(value);
    return data;
}

KRAnyData KRAnyDataCreateFloat(float value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = std::make_shared<KRRenderValue>(value);
    return data;
}

KRAnyData KRAnyDataCreateBool(bool value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = std::make_shared<KRRenderValue>(value);
    return data;
}

KRAnyData KRAnyDataCreateString(const char* value) {
    auto data = new KRAnyDataInternal();
    data->anyValue = std::make_shared<KRRenderValue>(value);
    return data;
}

KRAnyData KRAnyDataCreateBytes(const char* value, int size) {
    auto data = new KRAnyDataInternal();
    auto byteArray = std::make_shared<std::vector<uint8_t>>(value, value + size);
    data->anyValue = std::make_shared<KRRenderValue>(byteArray);
    return data;
}

KRAnyData KRAnyDataCreateArray(int size) {
    auto data = new KRAnyDataInternal();
    std::vector<std::shared_ptr<KRRenderValue>> valueArray;
    valueArray.reserve(size);
    for (int i = 0; i < size; ++i) {
        valueArray.emplace_back(std::make_shared<KRRenderValue>());
    }
    data->anyValue = std::make_shared<KRRenderValue>(valueArray);
    return data;
}

int KRAnyDataSetArrayElement(KRAnyData data, KRAnyData value, int index) {
    struct KRAnyDataInternal *internal = (struct KRAnyDataInternal *)data;
    if (internal == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    struct KRAnyDataInternal *valueInternal = (struct KRAnyDataInternal *)value;
    if (internal == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    if (!internal->anyValue) {
        internal->anyValue = std::make_shared<KRRenderValue>();
    }

    if (internal->anyValue->isArray()) {
        auto array = internal->anyValue->toArray();
        if (index >= array.size()) {
            return KRANYDATA_OUT_OF_INDEX;
        }
        std::vector<std::shared_ptr<KRRenderValue>> valueArray;
        valueArray = array;
        valueArray[index] = valueInternal->anyValue;
        internal->anyValue = std::make_shared<KRRenderValue>(valueArray);
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
    if (internal == nullptr) {
        return KRANYDATA_NULL_INPUT;
    }
    if (!internal->anyValue) {
        internal->anyValue = std::make_shared<KRRenderValue>();
    }

    if (internal->anyValue->isArray()) {
        auto array = internal->anyValue->toArray();
        std::vector<std::shared_ptr<KRRenderValue>> valueArray;
        valueArray = array;
        valueArray.push_back(valueInternal->anyValue);
        internal->anyValue = std::make_shared<KRRenderValue>(valueArray);
    } else {
        return KRANYDATA_TYPE_MISMATCH;
    }
    return KRANYDATA_SUCCESS;
}

#ifdef __cplusplus
}
#endif