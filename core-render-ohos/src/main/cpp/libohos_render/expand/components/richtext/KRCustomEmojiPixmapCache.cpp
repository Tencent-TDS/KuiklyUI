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

#include <utility>

#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/utils/KRRenderLoger.h"

namespace {
// 自定义 deleter：把裸 OH_PixelmapNative* 包成 shared_ptr 时统一在最后一个引用
// 析构时调用 OH_PixelmapNative_Release。集中在一处，避免散在各 release 调用点
// 误调用 / 漏调用；也为未来若 Release 接口签名变化（例如返回值校验）提供单点维护。
inline KRCustomEmojiPixmapCache::PixmapPtr WrapPixmap(OH_PixelmapNative *raw) {
    if (!raw) {
        return {};
    }
    return KRCustomEmojiPixmapCache::PixmapPtr(raw, [](OH_PixelmapNative *p) {
        if (p) {
            OH_PixelmapNative_Release(p);
        }
    });
}
}  // namespace

KRCustomEmojiPixmapCache &KRCustomEmojiPixmapCache::GetInstance() {
    static KRCustomEmojiPixmapCache instance;
    return instance;
}

KRCustomEmojiPixmapCache::KRCustomEmojiPixmapCache()
    : decode_queue_(std::make_unique<kuikly::dispatch::KRDispatchQueue>(
          "kr.emoji.pixmap.decode",
          kuikly::dispatch::QueueType::Serial,
          kuikly::dispatch::QoS::Utility)) {}

KRCustomEmojiPixmapCache::~KRCustomEmojiPixmapCache() {
    // 进程级单例理论上不会析构（与 KRFontCollectionWrapper 同），但提供清理逻辑保险。
    // decode_queue_ 是 unique_ptr<KRDispatchQueue>：reset() 触发 ffrt_queue_destroy，
    // 已经在跑的解码 task 会跑完，未执行的 task 被 cancel 并通过 TaskCleanup 释放。
    // 释放顺序：先停队列、再清缓存——避免极端竞态下"队列里的 task 在 Clear 之后又
    // 写回 cache_"。
    decode_queue_.reset();
    Clear();
}

KRCustomEmojiPixmapCache::PixmapPtr KRCustomEmojiPixmapCache::Get(const std::string &uri) {
    std::lock_guard<std::mutex> lk(mu_);
    auto it = cache_.find(uri);
    if (it == cache_.end()) {
        return {};
    }
    TouchLocked(uri);
    // 返回值是 shared_ptr 的拷贝构造：caller 与 cache 各持一份强引用，互不影响。
    // 即便后续 TrimLocked / Evict 把 it->second 从 cache 中移除，它持有的那份引用
    // 析构时只是把计数从 2 降到 1，caller 这份仍然保活底层 pixmap。
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

    // 把解码作业提交给 KRDispatchQueue (serial)。等价于 GCD dispatch_async，
    // 底层是 ffrt_queue_serial：所有任务严格 FIFO，由 ffrt 全局 worker 池按需
    // 调度执行——不再独占任何 OS 线程。decode_queue_ 在构造时创建；提交本身
    // （Async）在 KRDispatchQueue 内部已是线程安全，因此可在锁外调用，缩短临界区。
    decode_queue_->Async([uri]() {
        // ---- 在 ffrt worker 上下文中解码 ----
        // 关键：DecodeFromUri 已经把裸指针包成 shared_ptr 返回，捕获到主线程 lambda
        // 的整个传递路径中**不再有任何裸 OH_PixelmapNative***——任意中间环节抛异常 /
        // 提前 return，shared_ptr 都会自动释放。KRDispatchQueue::TaskExecutor 已经
        // 包了 try-catch 兜底，本 lambda 无需再额外捕获。
        PixmapPtr pm = DecodeFromUri(uri);

        KRMainThread::RunOnMainThread([uri, pm]() mutable {
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
                    // 写入缓存（极端竞争：另一路径已塞过同 key，覆盖前先释放老的，
                    // 保证幂等）。shared_ptr 赋值会自动 release 旧的强引用——若此时
                    // 旧 pixmap 还有其它 caller 持有，底层资源会延迟到那份引用析构
                    // 后才释放，比手写 OH_PixelmapNative_Release 更安全。
                    auto cit = self.cache_.find(uri);
                    if (cit != self.cache_.end()) {
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
                        self.cache_.emplace(uri, std::move(e));
                    }
                    self.TrimLocked();
                }
            }
            // 在锁外触发回调，避免 caller 回调中反向调用 cache 形成死锁。
            // 注意：解码失败（pm 为空）也会触发回调，让 caller 知道"已尝试且失败"。
            for (auto &cb : to_notify) {
                if (cb) {
                    cb(uri);
                }
            }
        });
    });
}

void KRCustomEmojiPixmapCache::Evict(const std::string &uri) {
    // shared_ptr 改造前的版本需要"先在锁内取出裸指针，再到锁外 Release"以避免在持锁
    // 期间触发 SDK 内部回调；现在仅释放 cache 自身那份强引用即可——shared_ptr 析构在
    // 引用计数降到 0 时才会真正调用 OH_PixelmapNative_Release，调用栈在哪里发生由
    // 最后一个释放者决定。这里在锁内 erase 即可，无需暂存。
    std::lock_guard<std::mutex> lk(mu_);
    auto it = cache_.find(uri);
    if (it == cache_.end()) {
        return;
    }
    lru_.erase(it->second.lru_it);
    cache_.erase(it);  // 此处释放 cache 这一份强引用；caller 仍持有的引用不受影响。
}

void KRCustomEmojiPixmapCache::Clear() {
    std::lock_guard<std::mutex> lk(mu_);
    cache_.clear();  // shared_ptr 析构链自动 Release 那些没有外部引用的 pixmap。
    lru_.clear();
    // waiters_ 不清理：飞行中的解码完成后会发现 cache_ 没自己，自然结束（不会泄漏）。
}

void KRCustomEmojiPixmapCache::TouchLocked(const std::string &uri) {
    auto it = cache_.find(uri);
    if (it == cache_.end()) {
        return;
    }
    lru_.erase(it->second.lru_it);
    lru_.push_front(uri);
    it->second.lru_it = lru_.begin();
}

void KRCustomEmojiPixmapCache::TrimLocked() {
    while (cache_.size() > kCapacity && !lru_.empty()) {
        const std::string &oldest = lru_.back();
        auto it = cache_.find(oldest);
        if (it != cache_.end()) {
            cache_.erase(it);  // 释放 cache 这一份强引用；caller 持有的引用仍保活底层 pixmap。
        }
        lru_.pop_back();
    }
}

KRCustomEmojiPixmapCache::PixmapPtr KRCustomEmojiPixmapCache::DecodeFromUri(const std::string &uri) {
    if (uri.empty()) return {};
    OH_PixelmapNative *pixelmap = nullptr;
    OH_ImageSourceNative *source = nullptr;
    // OH_ImageSourceNative_CreateFromUri 接受 char* 而非 const char*（API 12 签名），
    // 这里复制一份可写副本避免触动 caller 字符串。
    std::string mutable_uri = uri;
    auto code = OH_ImageSourceNative_CreateFromUri(mutable_uri.data(), mutable_uri.length(), &source);
    if (code != IMAGE_SUCCESS || source == nullptr) {
        KR_LOG_ERROR << "[KRCustomEmojiPixmapCache] CreateFromUri failed, uri=" << uri << " code=" << code;
        return {};
    }
    OH_DecodingOptions *ops = nullptr;
    if (OH_DecodingOptions_Create(&ops) == IMAGE_SUCCESS && ops) {
        // AUTO：HDR 资源解为 HDR pixmap，普通图按原色域；与 KRMemoryCacheModule 一致。
        OH_DecodingOptions_SetDesiredDynamicRange(ops, IMAGE_DYNAMIC_RANGE_AUTO);
        OH_ImageSourceNative_CreatePixelmap(source, ops, &pixelmap);
        OH_DecodingOptions_Release(ops);
    }
    OH_ImageSourceNative_Release(source);
    // 立即把裸指针包成 shared_ptr 返回；调用方（Prefetch 后台线程 lambda）拿到的是
    // 强引用句柄，整条链路上不再有"裸 OH_PixelmapNative*"，避免线程切换 / 异常路径
    // 中失控。
    return WrapPixmap(pixelmap);
}
