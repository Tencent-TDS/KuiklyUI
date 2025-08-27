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

#ifndef CORE_RENDER_OHOS_KRMEMORYCACHEMODULE_H
#define CORE_RENDER_OHOS_KRMEMORYCACHEMODULE_H

#include <cstdint>
#include <shared_mutex>

#include "libohos_render/export/IKRRenderModuleExport.h"

constexpr char kMemoryCacheModuleName[] = "KRMemoryCacheModule";

class KRMemoryCacheModule : public IKRRenderModuleExport {
 public:
    KRMemoryCacheModule() = default;
    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;

    KRAnyValue Get(const std::string &key);
    OH_PixelmapNative *GetImage(const std::string &key);
    void OnDestroy() override;

 private:
    KRAnyValue SetObject(const KRAnyValue &params);
    KRAnyValue CacheImage(const KRAnyValue &params, const KRRenderCallback &callback);
    std::string GenerateCacheKey(const std::string &src);
    void SetImage(const std::string &cache_key, OH_PixelmapNative *pixelmap);
    KRRenderValueMap GenerateResult(const std::string &cache_key, OH_PixelmapNative *pixelmap);
    KRRenderValueMap GenerateError(int32_t code, const std::string &message);
    OH_PixelmapNative *LoadPixelmapFromLocal(std::string &src);
    void ReleasePixelmap(OH_PixelmapNative *pixelmap);

 private:
    std::unordered_map<std::string, KRAnyValue> cache_map_;
    std::unordered_map<std::string, OH_PixelmapNative *> image_cache_map_;
    std::shared_mutex mtx_;
};

#endif  // CORE_RENDER_OHOS_KRMEMORYCACHEMODULE_H
