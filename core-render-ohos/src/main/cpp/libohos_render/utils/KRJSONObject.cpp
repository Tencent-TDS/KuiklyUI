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

#include "KRJSONObject.h"

#include "thirdparty/cJSON/cJSON.h"

namespace kuikly {
namespace util {
std::shared_ptr<JSONObject> JSONObject::Parse(const std::string &str) {
    cJSON *p = cJSON_Parse(str.c_str());
    return p ? std::make_shared<JSONObject>(p) : nullptr;
}

JSONObject::JSONObject(void *cjson, std::shared_ptr<JSONObject> owner) : cjson_(cjson), owner_(owner) {
    // blank
}
JSONObject::~JSONObject() {
    if (owner_) {
        cjson_ = nullptr;
        owner_ = nullptr;
    } else if (cjson_) {
        cJSON_Delete(reinterpret_cast<cJSON *>(cjson_));
        cjson_ = nullptr;
    }
}
std::string JSONObject::GetString(const std::string &key, const std::string &default_value) {
    if (cjson_ == nullptr) {
        return default_value;
    }
    cJSON *item = cJSON_GetObjectItem(reinterpret_cast<cJSON *>(cjson_), key.c_str());
    if (item) {
        std::string str = cJSON_GetStringValue(item);
        return str;
    }
    return default_value;
}

std::vector<std::string> JSONObject::GetStringArray(const std::string &key) {
    if (cjson_ == nullptr) {
        return std::vector<std::string>();
    }
    cJSON *item = cJSON_GetObjectItem(reinterpret_cast<cJSON *>(cjson_), key.c_str());
    if (item) {
        std::vector<std::string> result;
        for (int i = 0; i < cJSON_GetArraySize(item); ++i) {
            std::string str = cJSON_GetStringValue(cJSON_GetArrayItem(item, i));
            result.emplace_back(str);
        }
        return result;
    }
    return std::vector<std::string>();
}

double JSONObject::GetNumber(const std::string &key, const double default_value) {
    if (cjson_ == nullptr) {
        return default_value;
    }
    cJSON *item = cJSON_GetObjectItem(reinterpret_cast<cJSON *>(cjson_), key.c_str());
    if (item) {
        return cJSON_GetNumberValue(item);
    }
    return default_value;
}

std::vector<double> JSONObject::GetNumberArray(const std::string &key) {
    if (cjson_ == nullptr) {
        return std::vector<double>();
    }
    cJSON *item = cJSON_GetObjectItem(reinterpret_cast<cJSON *>(cjson_), key.c_str());
    if (item) {
        std::vector<double> result;
        for (int i = 0; i < cJSON_GetArraySize(item); ++i) {
            double number = cJSON_GetNumberValue(cJSON_GetArrayItem(item, i));
            result.emplace_back(number);
        }
        return result;
    }
    return std::vector<double>();
}

std::shared_ptr<JSONObject> JSONObject::GetArrayItem(int index) {
    if (cjson_ == nullptr) {
        return nullptr;
    }
    cJSON *item = cJSON_GetArrayItem(reinterpret_cast<cJSON *>(cjson_), index);
    if (item) {
        return std::make_shared<JSONObject>(item, shared_from_this());
    }
    return nullptr;
}

int JSONObject::GetArraySize() {
    if (cjson_ == nullptr) {
        return 0;
    }
    return cJSON_GetArraySize(reinterpret_cast<cJSON *>(cjson_));
}

std::shared_ptr<JSONObject> JSONObject::GetObjectItem(const std::string &key) {
    if (cjson_ == nullptr) {
        return nullptr;
    }
    cJSON *item = cJSON_GetObjectItem(reinterpret_cast<cJSON *>(cjson_), key.c_str());
    if (item) {
        return std::make_shared<JSONObject>(item, shared_from_this());
    }
    return nullptr;
}

}  // end namespace util
}  // namespace kuikly
