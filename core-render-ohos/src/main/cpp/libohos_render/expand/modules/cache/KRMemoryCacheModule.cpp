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

#include "libohos_render/expand/modules/cache/KRMemoryCacheModule.h"
#include "libohos_render/expand/modules/codec/KRCodec.h"
#include "libohos_render/utils/KRViewUtil.h"
#include <multimedia/image_framework/image/pixelmap_native.h>

constexpr char kMethodNameSetObject[] = "setObject";
constexpr char kMethodNameCacheImage[] = "cacheImage";
constexpr char kParamNameKey[] = "key";
constexpr char kParamNameValue[] = "value";
constexpr char kParamNameSrc[] = "src";
constexpr char kParamNameSync[] = "sync";
constexpr char kStatusKeyErrorCode[] = "errorCode";
constexpr char kStatusKeyErrorMsg[] = "errorMsg";
constexpr char kStatusKeyState[] = "state";
constexpr char kStatusKeyCacheKey[] = "cacheKey";
constexpr char kCacheStateComplete[] = "Complete";
constexpr char kCacheStateInProgress[] = "InProgress";
constexpr char kCacheStateFailed[] = "Failed";
constexpr char kHttpPrefix[] = "http://";
constexpr char kHttpsPrefix[] = "https://";

KRAnyValue KRMemoryCacheModule::Get(const std::string &key) {
    auto it = cache_map_.find(key);
    if (it == cache_map_.end()) {
        return KREmptyValue();
    } else {
        return it->second;
    }
}

OH_PixelmapNative *KRMemoryCacheModule::GetImage(const std::string &key) {
    auto it = image_cache_map_.find(key);
    if (it == image_cache_map_.end()) {
        return nullptr;
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
    } else if (std::strcmp(method.c_str(), kMethodNameCacheImage) == 0) {
        return CacheImage(params, callback);
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

KRAnyValue KRMemoryCacheModule::CacheImage(const KRAnyValue &params, const KRRenderCallback &callback) {
    auto map = params->toMap();
    auto src = map[kParamNameSrc]->toString();
    auto sync = map[kParamNameSync]->toBool();
    std::string key = "Image:" + kuikly::KRMd5(src);
    
    if (!KRRenderAdapterManager::GetInstance().HasCustomImageAdapter()) {
        KR_LOG_ERROR << "Please implement image adapter to cache image.";
        KRRenderValueMap resultMap;
        resultMap[kStatusKeyState] = NewKRRenderValue(kCacheStateFailed);
        resultMap[kStatusKeyErrorCode] = NewKRRenderValue(2);
        resultMap[kStatusKeyErrorMsg] = NewKRRenderValue("No image adapter implemented");
        resultMap[kStatusKeyCacheKey] = NewKRRenderValue("");
        return NewKRRenderValue(std::move(resultMap));
    }

    KRRenderValueMap completeMap;
    completeMap[kStatusKeyState] = NewKRRenderValue(kCacheStateComplete);
    completeMap[kStatusKeyErrorCode] = NewKRRenderValue(0);
    completeMap[kStatusKeyErrorMsg] = NewKRRenderValue("");
    completeMap[kStatusKeyCacheKey] = NewKRRenderValue(key);

    KRRenderValueMap inProgressMap;
    inProgressMap[kStatusKeyState] = NewKRRenderValue(kCacheStateInProgress);
    inProgressMap[kStatusKeyErrorCode] = NewKRRenderValue(1);
    inProgressMap[kStatusKeyErrorMsg] = NewKRRenderValue(kCacheStateInProgress);
    inProgressMap[kStatusKeyCacheKey] = NewKRRenderValue(key);

    bool isNetwork = src.find(kHttpPrefix) == 0 || src.find(kHttpsPrefix) == 0;
    // 同步缓存，网络下载图片不支持同步
    if (sync && !isNetwork) {
        download_completed_ = false;
        // 获取图片，需实现图片适配器
        KRRenderAdapterManager::GetInstance().CallImageAdapter(src.c_str(),
            [this, key](char *imageSrc, OH_PixelmapNative *imagePixelMap) {
                {
                    std::lock_guard<std::mutex> lock(mtx_);
                    if (!imagePixelMap) {
                        imagePixelMap = kuikly::util::GetPixelMapFromUri(imageSrc);
                    }
                    image_cache_map_[key] = imagePixelMap;
                    download_completed_ = true;
                }
                cv_.notify_one();
            });
        // 等待图片结果
        {
            std::unique_lock<std::mutex> lock(mtx_);
            auto timeout = std::chrono::seconds(5);
            cv_.wait_for(lock, timeout, [this, key] { return download_completed_ && image_cache_map_.count(key) > 0; });
        }
        if (download_completed_) {
            return NewKRRenderValue(std::move(completeMap));
        } else {
            return NewKRRenderValue(std::move(inProgressMap));
        }
    } else {
        // 获取图片，需实现图片适配器
        KRRenderAdapterManager::GetInstance().CallImageAdapter(src.c_str(),
            [this, key, callback, completeMap](char *imageSrc, OH_PixelmapNative *imagePixelMap) {
                {
                    std::lock_guard<std::mutex> lock(mtx_);
                    image_cache_map_[key] = imagePixelMap;
                }
                callback(NewKRRenderValue(std::move(completeMap)));
            });
        return NewKRRenderValue(std::move(inProgressMap));
    }
}

void KRMemoryCacheModule::OnDestroy() {
    for (const auto &pair : image_cache_map_) {
        OH_PixelmapNative_Release(pair.second);
    }
}
