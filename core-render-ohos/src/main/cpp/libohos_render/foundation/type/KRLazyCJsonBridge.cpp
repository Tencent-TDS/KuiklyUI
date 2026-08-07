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

#include "libohos_render/foundation/type/KRLazyCJsonBridge.h"

#include <cstdint>
#include <memory>

#include "thirdparty/cJSON/cJSON.h"

namespace {

struct KuiklyCJsonOwner {
    cJSON *json{nullptr};

    explicit KuiklyCJsonOwner(cJSON *tree) : json(tree) {}

    ~KuiklyCJsonOwner() {
        if (json != nullptr) {
            cJSON_Delete(json);
            json = nullptr;
        }
    }

    KuiklyCJsonOwner(const KuiklyCJsonOwner &) = delete;
    KuiklyCJsonOwner &operator=(const KuiklyCJsonOwner &) = delete;
};

using OwnerPtr = std::shared_ptr<KuiklyCJsonOwner>;

OwnerPtr *AsHandle(int64_t ptr) {
    return reinterpret_cast<OwnerPtr *>(static_cast<intptr_t>(ptr));
}

int64_t ToHandle(OwnerPtr sp) {
    if (!sp) {
        return 0;
    }
    return static_cast<int64_t>(reinterpret_cast<intptr_t>(new OwnerPtr(std::move(sp))));
}

cJSON *Node(int64_t ptr) {
    return reinterpret_cast<cJSON *>(static_cast<intptr_t>(ptr));
}

cJSON *Item(int64_t ptr, const char *key) {
    if (ptr == 0 || key == nullptr) {
        return nullptr;
    }
    return cJSON_GetObjectItemCaseSensitive(Node(ptr), key);
}

}  // namespace

extern "C" {

int64_t kuikly_cjson_owner_create(void *cjson) {
    if (cjson == nullptr) {
        return 0;
    }
    return ToHandle(std::make_shared<KuiklyCJsonOwner>(reinterpret_cast<cJSON *>(cjson)));
}

int64_t kuikly_cjson_retain(int64_t owner_ptr) {
    auto *handle = AsHandle(owner_ptr);
    if (handle == nullptr || !(*handle)) {
        return 0;
    }
    // New heap shared_ptr sharing the same control block.
    return ToHandle(*handle);
}

void kuikly_cjson_release(int64_t owner_ptr) {
    auto *handle = AsHandle(owner_ptr);
    if (handle == nullptr) {
        return;
    }
    delete handle;
}

int64_t kuikly_cjson_owner_root(int64_t owner_ptr) {
    auto *handle = AsHandle(owner_ptr);
    if (handle == nullptr || !(*handle) || (*handle)->json == nullptr) {
        return 0;
    }
    return static_cast<int64_t>(reinterpret_cast<intptr_t>((*handle)->json));
}

int kuikly_cjson_has(int64_t ptr, const char *key) {
    return Item(ptr, key) != nullptr ? 1 : 0;
}

int kuikly_cjson_size(int64_t ptr) {
    if (ptr == 0) {
        return 0;
    }
    int n = 0;
    for (cJSON *c = Node(ptr)->child; c != nullptr; c = c->next) {
        n++;
    }
    return n;
}

const char *kuikly_cjson_key_at(int64_t ptr, int index) {
    if (ptr == 0 || index < 0) {
        return nullptr;
    }
    int i = 0;
    for (cJSON *c = Node(ptr)->child; c != nullptr; c = c->next) {
        if (i == index) {
            return c->string;
        }
        i++;
    }
    return nullptr;
}

int kuikly_cjson_value_kind(int64_t ptr, const char *key) {
    cJSON *item = Item(ptr, key);
    if (item == nullptr || cJSON_IsNull(item)) {
        return 0;
    }
    if (cJSON_IsBool(item)) {
        return 1;
    }
    if (cJSON_IsNumber(item)) {
        return 2;
    }
    if (cJSON_IsString(item)) {
        return 3;
    }
    if (cJSON_IsObject(item)) {
        return 4;
    }
    if (cJSON_IsArray(item)) {
        return 5;
    }
    return 0;
}

int kuikly_cjson_get_bool(int64_t ptr, const char *key, int fallback) {
    cJSON *item = Item(ptr, key);
    if (item == nullptr || !cJSON_IsBool(item)) {
        return fallback;
    }
    return cJSON_IsTrue(item) ? 1 : 0;
}

double kuikly_cjson_get_number(int64_t ptr, const char *key, double fallback) {
    cJSON *item = Item(ptr, key);
    if (item == nullptr || !cJSON_IsNumber(item)) {
        return fallback;
    }
    return cJSON_GetNumberValue(item);
}

const char *kuikly_cjson_get_string(int64_t ptr, const char *key) {
    cJSON *item = Item(ptr, key);
    if (item == nullptr || !cJSON_IsString(item)) {
        return nullptr;
    }
    return cJSON_GetStringValue(item);
}

int64_t kuikly_cjson_get_object(int64_t ptr, const char *key) {
    cJSON *item = Item(ptr, key);
    if (item == nullptr || !cJSON_IsObject(item)) {
        return 0;
    }
    return static_cast<int64_t>(reinterpret_cast<intptr_t>(item));
}

int64_t kuikly_cjson_get_array(int64_t ptr, const char *key) {
    cJSON *item = Item(ptr, key);
    if (item == nullptr || !cJSON_IsArray(item)) {
        return 0;
    }
    return static_cast<int64_t>(reinterpret_cast<intptr_t>(item));
}

char *kuikly_cjson_print(int64_t ptr) {
    if (ptr == 0) {
        return nullptr;
    }
    return cJSON_PrintUnformatted(Node(ptr));
}

void kuikly_cjson_free_string(char *s) {
    if (s != nullptr) {
        cJSON_free(s);
    }
}

}  // extern "C"
