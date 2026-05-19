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

#ifndef CORE_RENDER_OHOS_KRCUSTOMEMOJIPIXMAPCACHE_H
#define CORE_RENDER_OHOS_KRCUSTOMEMOJIPIXMAPCACHE_H

#include <multimedia/image_framework/image/pixelmap_native.h>

#include <functional>
#include <list>
#include <mutex>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

/**
 * 自定义表情 pixmap 进程级缓存（专用于 PostProcessor 拆段产生的 image span）。
 *
 * 设计要点：
 * 1) 进程级单例：同一张 emoji 图在多个 RichText / Input / 多页面间共享解码结果，
 *    与 KRFontCollectionWrapper::GetInstance() 同范式。
 * 2) LRU 容量上限 128：emoji 数量上限可控，超出按最久未访问淘汰；淘汰时同步释放
 *    OH_PixelmapNative。
 * 3) 异步去重：同一个 uri 多个 caller 同时 Prefetch 时，只发起一次解码，其它
 *    caller 加入 waiters_ 队列；解码完成后一次性回调全部 waiters。
 * 4) 接口最小化：Get（命中即返回，不持有所有权）、Prefetch（异步预解码 + 回调）、
 *    Evict（暴露给 trim memory 场景）。caller 不释放返回的 pixmap，由本 cache 统一管理。
 *
 * 为何不复用 KRMemoryCacheModule：
 *  - KRMemoryCacheModule 是 per-RootView 的业务 module（jsCall 通道），作用域 = 一个页面；
 *  - 本 cache 是渲染层 internal，跨页面共享，用 std::unordered_map + LRU 简单可控。
 *
 * 仅服务于"PostProcessor 拆段产生的内置 image span"，业务自己声明的 ImageSpan
 * 走父节点 ImageView 链路，不进入本 cache。
 */
class KRCustomEmojiPixmapCache {
 public:
    static KRCustomEmojiPixmapCache &GetInstance();

    // 禁止拷贝与赋值
    KRCustomEmojiPixmapCache(const KRCustomEmojiPixmapCache &) = delete;
    KRCustomEmojiPixmapCache &operator=(const KRCustomEmojiPixmapCache &) = delete;

    /**
     * 同步取已解码的 pixmap。命中返回非空指针（caller 不持有所有权，不可释放）；
     * 未命中或仍在解码返回 nullptr。访问会更新 LRU 顺序。
     */
    OH_PixelmapNative *Get(const std::string &uri);

    /**
     * 解码完成回调：在主线程被调用。即使解码失败也会回调（pixmap == nullptr）。
     */
    using LoadedCallback = std::function<void(const std::string & /*uri*/)>;

    /**
     * 异步预解码：
     *   - 已缓存：立即在主线程调度 on_loaded（保证 caller 拿到一致的"完成"语义）；
     *   - 已飞行：把 on_loaded 加入等待队列，解码完成后一次性触发；
     *   - 未发起：起后台线程解码，完成后切回主线程写缓存 + 触发全部 waiters。
     *
     * on_loaded 可为空——仅触发预解码、不需要回调时使用。
     */
    void Prefetch(const std::string &uri, LoadedCallback on_loaded);

    /**
     * 主动剔除某个 uri 的缓存（trim memory 场景）。释放 pixmap 资源。
     */
    void Evict(const std::string &uri);

    /**
     * 清空全部缓存。仅用于极端场景（如低内存告警）。
     */
    void Clear();

 private:
    KRCustomEmojiPixmapCache() = default;
    ~KRCustomEmojiPixmapCache();

    /**
     * 真正的解码工作（同步执行，调用方负责线程）。返回 nullptr 表示失败。
     * 与原 KRRichTextShadow::DecodePixmapFromUri 等价：OH_ImageSourceNative_CreateFromUri
     * + OH_ImageSourceNative_CreatePixelmap，色域走 IMAGE_DYNAMIC_RANGE_AUTO（与
     * KRMemoryCacheModule 保持一致，HDR/SDR 自适应）。
     */
    static OH_PixelmapNative *DecodeFromUri(const std::string &uri);

    // 调用方必须持锁。LRU：按访问 / 写入提到 list 头部；淘汰从 list 末尾开始。
    void TouchLocked(const std::string &uri);
    void TrimLocked();

    static constexpr size_t kCapacity = 128;

    std::mutex mu_;
    // LRU 双向链表：front 为最近访问，back 为最久未访问。
    std::list<std::string> lru_;
    // uri -> {pixmap, lru_iterator}
    struct Entry {
        OH_PixelmapNative *pixmap = nullptr;
        std::list<std::string>::iterator lru_it;
    };
    std::unordered_map<std::string, Entry> cache_;
    // 飞行中：uri -> 等待回调列表。
    std::unordered_map<std::string, std::vector<LoadedCallback>> waiters_;
};

#endif  // CORE_RENDER_OHOS_KRCUSTOMEMOJIPIXMAPCACHE_H
