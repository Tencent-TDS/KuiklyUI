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

#ifndef CORE_RENDER_OHOS_KRMEMORYCACHEMODULE_H
#define CORE_RENDER_OHOS_KRMEMORYCACHEMODULE_H

#include "libohos_render/export/IKRRenderModuleExport.h"

constexpr char kMemoryCacheModuleName[] = "KRMemoryCacheModule";

class KRMemoryCacheModule : public IKRRenderModuleExport {
 public:
    KRMemoryCacheModule() = default;
    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;

    KRAnyValue Get(const std::string &key);
    void Set(const std::string &key, const KRAnyValue &value);
    OH_PixelmapNative* GetImage(const std::string &key);
    void OnDestroy() override;

 private:
    KRAnyValue SetObject(const KRAnyValue &params);
    KRAnyValue CacheImage(const KRAnyValue &params, const KRRenderCallback &callback);

 private:
    std::unordered_map<std::string, KRAnyValue> cache_map_;
    std::unordered_map<std::string, OH_PixelmapNative*> image_cache_map_;
    std::mutex mtx_;
    std::condition_variable cv_;
    bool download_completed_;
};

#endif  // CORE_RENDER_OHOS_KRMEMORYCACHEMODULE_H
