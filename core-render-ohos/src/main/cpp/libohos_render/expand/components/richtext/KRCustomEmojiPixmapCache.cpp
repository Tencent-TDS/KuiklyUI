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

#include "libohos_render/expand/components/richtext/KRCustomEmojiPixmapCache.h"

#include <multimedia/image_framework/image/image_source_native.h>

#include <thread>
#include <utility>

#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/utils/KRRenderLoger.h"

KRCustomEmojiPixmapCache &KRCustomEmojiPixmapCache::GetInstance() {
    static KRCustomEmojiPixmapCache instance;
    return instance;
}

KRCustomEmojiPixmapCache::~KRCustomEmojiPixmapCache() {
    // 进程级单例理论上不会析构（与 KRFontCollectionWrapper 同），但提供清理逻辑保险。
    Clear();
}

OH_PixelmapNative *KRCustomEmojiPixmapCache::Get(const std::string &uri) {
    std::lock_guard<std::mutex> lk(mu_);
    auto it = cache_.find(uri);
    if (it == cache_.end()) {
        return nullptr;
    }
    TouchLocked(uri);
    return it->second.pixmap;
}

void KRCustomEmojiPixmapCache::Prefetch(const std::string &uri, LoadedCallback on_loaded) {
    if (uri.empty()) {
        if (on_loaded) {
            // 保持回调在主线程触发的一致性。
            KRMainThread::RunOnMainThread([on_loaded, uri]() { on_loaded(uri); });
        }
        return;
    }

    bool need_decode = false;
    {
        std::lock_guard<std::mutex> lk(mu_);
        auto it = cache_.find(uri);
        if (it != cache_.end()) {
            // 已缓存：直接调度回调，不重复解码。
            TouchLocked(uri);
            if (on_loaded) {
                KRMainThread::RunOnMainThread([on_loaded, uri]() { on_loaded(uri); });
            }
            return;
        }
        // 未缓存：登记 waiter（如果有）；如果没有飞行任务则我们成为发起者。
        auto wit = waiters_.find(uri);
        if (wit == waiters_.end()) {
            // 我们是第一个发起者
            std::vector<LoadedCallback> initial;
            if (on_loaded) {
                initial.push_back(std::move(on_loaded));
            }
            waiters_.emplace(uri, std::move(initial));
            need_decode = true;
        } else {
            // 已有飞行任务，仅追加等待回调
            if (on_loaded) {
                wit->second.push_back(std::move(on_loaded));
            }
        }
    }

    if (!need_decode) {
        return;
    }

    // 起后台线程解码。完成后切回主线程写缓存 + 触发全部 waiters。
    std::thread([uri]() {
        OH_PixelmapNative *pm = DecodeFromUri(uri);
        KRMainThread::RunOnMainThread([uri, pm]() {
            auto &self = KRCustomEmojiPixmapCache::GetInstance();
            std::vector<LoadedCallback> to_notify;
            {
                std::lock_guard<std::mutex> lk(self.mu_);
                // 取出 waiters
                auto wit = self.waiters_.find(uri);
                if (wit != self.waiters_.end()) {
                    to_notify = std::move(wit->second);
                    self.waiters_.erase(wit);
                }
                if (pm) {
                    // 写入缓存（极端竞争：另一路径已塞过同 key，覆盖前先释放老的，保证幂等）。
                    auto cit = self.cache_.find(uri);
                    if (cit != self.cache_.end()) {
                        if (cit->second.pixmap) {
                            OH_PixelmapNative_Release(cit->second.pixmap);
                        }
                        // 复用 lru 节点：先移到头部
                        self.lru_.erase(cit->second.lru_it);
                        self.lru_.push_front(uri);
                        cit->second.pixmap = pm;
                        cit->second.lru_it = self.lru_.begin();
                    } else {
                        self.lru_.push_front(uri);
                        Entry e;
                        e.pixmap = pm;
                        e.lru_it = self.lru_.begin();
                        self.cache_.emplace(uri, e);
                    }
                    self.TrimLocked();
                }
            }
            // 在锁外触发回调，避免 caller 回调中反向调用 cache 形成死锁。
            // 注意：解码失败（pm == nullptr）也会触发回调，让 caller 知道"已尝试且失败"。
            for (auto &cb : to_notify) {
                if (cb) cb(uri);
            }
        });
    }).detach();
}

void KRCustomEmojiPixmapCache::Evict(const std::string &uri) {
    OH_PixelmapNative *to_release = nullptr;
    {
        std::lock_guard<std::mutex> lk(mu_);
        auto it = cache_.find(uri);
        if (it == cache_.end()) {
            return;
        }
        to_release = it->second.pixmap;
        lru_.erase(it->second.lru_it);
        cache_.erase(it);
    }
    if (to_release) {
        OH_PixelmapNative_Release(to_release);
    }
}

void KRCustomEmojiPixmapCache::Clear() {
    std::vector<OH_PixelmapNative *> to_release;
    {
        std::lock_guard<std::mutex> lk(mu_);
        to_release.reserve(cache_.size());
        for (auto &kv : cache_) {
            if (kv.second.pixmap) {
                to_release.push_back(kv.second.pixmap);
            }
        }
        cache_.clear();
        lru_.clear();
        // waiters_ 不清理：飞行中的解码完成后会发现 cache_ 没自己，自然结束（不会泄漏）。
    }
    for (auto *pm : to_release) {
        OH_PixelmapNative_Release(pm);
    }
}

void KRCustomEmojiPixmapCache::TouchLocked(const std::string &uri) {
    auto it = cache_.find(uri);
    if (it == cache_.end()) return;
    lru_.erase(it->second.lru_it);
    lru_.push_front(uri);
    it->second.lru_it = lru_.begin();
}

void KRCustomEmojiPixmapCache::TrimLocked() {
    while (cache_.size() > kCapacity && !lru_.empty()) {
        const std::string &oldest = lru_.back();
        auto it = cache_.find(oldest);
        if (it != cache_.end()) {
            if (it->second.pixmap) {
                OH_PixelmapNative_Release(it->second.pixmap);
            }
            cache_.erase(it);
        }
        lru_.pop_back();
    }
}

OH_PixelmapNative *KRCustomEmojiPixmapCache::DecodeFromUri(const std::string &uri) {
    if (uri.empty()) return nullptr;
    OH_PixelmapNative *pixelmap = nullptr;
    OH_ImageSourceNative *source = nullptr;
    // OH_ImageSourceNative_CreateFromUri 接受 char* 而非 const char*（API 12 签名），
    // 这里复制一份可写副本避免触动 caller 字符串。
    std::string mutable_uri = uri;
    auto code = OH_ImageSourceNative_CreateFromUri(mutable_uri.data(), mutable_uri.length(), &source);
    if (code != IMAGE_SUCCESS || source == nullptr) {
        KR_LOG_ERROR << "[KRCustomEmojiPixmapCache] CreateFromUri failed, uri=" << uri << " code=" << code;
        return nullptr;
    }
    OH_DecodingOptions *ops = nullptr;
    if (OH_DecodingOptions_Create(&ops) == IMAGE_SUCCESS && ops) {
        // AUTO：HDR 资源解为 HDR pixmap，普通图按原色域；与 KRMemoryCacheModule 一致。
        OH_DecodingOptions_SetDesiredDynamicRange(ops, IMAGE_DYNAMIC_RANGE_AUTO);
        OH_ImageSourceNative_CreatePixelmap(source, ops, &pixelmap);
        OH_DecodingOptions_Release(ops);
    }
    OH_ImageSourceNative_Release(source);
    return pixelmap;
}
