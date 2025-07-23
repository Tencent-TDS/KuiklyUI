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

#include "libohos_render/expand/modules/cache/KRMemoryCacheModule.h"

constexpr char kMethodNameSetObject[] = "setObject";
constexpr char kParamNameKey[] = "key";
constexpr char kParamNameValue[] = "value";

KRAnyValue KRMemoryCacheModule::Get(const std::string &key) {
    auto it = cache_map_.find(key);
    if (it == cache_map_.end()) {
        return KREmptyValue();
    } else {
        return it->second;
    }
}

void KRMemoryCacheModule::Set(const std::string &key, const KRAnyValue &value) {
    cache_map_[key] = value;
}

KRAnyValue KRMemoryCacheModule::CallMethod(bool sync, const std::string &method, KRAnyValue params,
                                           const KRRenderCallback &callback) {
    if (std::strcmp(method.c_str(), kMethodNameSetObject) == 0) {
        return SetObject(params);
    } else {
        return KREmptyValue();
    }
}

KRAnyValue KRMemoryCacheModule::SetObject(const KRAnyValue &params) {
    auto map = params->toMap();
    auto key = map[kParamNameKey]->toString();
    auto value = map[kParamNameValue];
    cache_map_[key] = value;
    return KREmptyValue();
}
