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
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "libohos_render/foundation/ffrt/KRDispatchQueue.h"

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
 * 3.1) 解码串行化经由 KRDispatchQueue（基于 OHOS libffrt 的 ffrt_queue_serial）。
 *    替代了原本"每个未缓存 uri 都 std::thread(...).detach()"模式——后者在富文本
 *    50 个 emoji 同屏时会瞬间起 50 个线程（栈内存 + 切换 + UB 退出风险）。
 *    KRDispatchQueue 不持有独占 OS 线程：底层 ffrt 调度复用全局 worker 池，idle
 *    时零线程占用；密集时由 ffrt 决定何时拉起 worker。串行语义保证了同一时刻
 *    最多一条解码在跑，与"图像 IO-bound + SDK 内部互斥"特性匹配。
 * 4) 接口最小化：Get（命中即返回 PixmapPtr 强引用句柄）、Prefetch（异步预解码 +
 *    回调）、Evict（暴露给 trim memory 场景）。底层 OH_PixelmapNative_Release 由
 *    shared_ptr 自定义 deleter 在最后一个强引用析构时自动调用——caller 在持有 Get
 *    返回值期间，即便 cache 因 LRU 淘汰 / Evict / Clear 释放自己那份引用，底层
 *    pixmap 仍然有效，**从根本消除 Get 返回裸指针时的并发悬挂指针风险**。
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
    /**
     * pixmap 强引用句柄。
     * - 自定义 deleter 包装了 OH_PixelmapNative_Release，最后一个 shared_ptr
     *   析构时调用；
     * - cache 内部与 caller 同时持有时为多引用，任一方先释放都安全；
     * - 解码失败 / 未命中等场景以空 shared_ptr 表达（`if (!ptr)` 即可判定）。
     */
    using PixmapPtr = std::shared_ptr<OH_PixelmapNative>;

    static KRCustomEmojiPixmapCache &GetInstance();

    // 禁止拷贝与赋值
    KRCustomEmojiPixmapCache(const KRCustomEmojiPixmapCache &) = delete;
    KRCustomEmojiPixmapCache &operator=(const KRCustomEmojiPixmapCache &) = delete;

    /**
     * 同步取已解码的 pixmap。命中返回非空 shared_ptr，调用方在该 shared_ptr 存活
     * 期间底层 pixmap 必然有效——即使此刻被并发 Evict / TrimLocked 从 cache 中移除，
     * cache 释放的只是它自己那份强引用，caller 这份独立计数会保活底层资源直到本地
     * 变量析构。未命中或仍在解码返回空 shared_ptr。访问会更新 LRU 顺序。
     */
    PixmapPtr Get(const std::string &uri);

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
     * 主动剔除某个 uri 的缓存（trim memory 场景）。仅释放 cache 自身那份强引用——
     * 若此时仍有 caller 持有 Get 返回的 shared_ptr，底层 pixmap 会延迟到那份引用
     * 析构后才真正释放。
     */
    void Evict(const std::string &uri);

    /**
     * 清空全部缓存。仅用于极端场景（如低内存告警）。
     * 同 Evict 语义：仅释放 cache 自身那份强引用，caller 持有的 shared_ptr 仍然有效。
     */
    void Clear();

 private:
    KRCustomEmojiPixmapCache();
    ~KRCustomEmojiPixmapCache();

    /**
     * 真正的解码工作（同步执行，调用方负责线程）。返回空 shared_ptr 表示失败。
     * 与原 KRRichTextShadow::DecodePixmapFromUri 等价：OH_ImageSourceNative_CreateFromUri
     * + OH_ImageSourceNative_CreatePixelmap，色域走 IMAGE_DYNAMIC_RANGE_AUTO（与
     * KRMemoryCacheModule 保持一致，HDR/SDR 自适应）。
     *
     * 函数返回时已用自定义 deleter 包成 shared_ptr，避免裸指针在线程切换 / 异常
     * 路径中失控。
     */
    static PixmapPtr DecodeFromUri(const std::string &uri);

    // 调用方必须持锁。LRU：按访问 / 写入提到 list 头部；淘汰从 list 末尾开始。
    void TouchLocked(const std::string &uri);
    void TrimLocked();

    static constexpr size_t kCapacity = 128;

    std::mutex mu_;
    // LRU 双向链表：front 为最近访问，back 为最久未访问。
    std::list<std::string> lru_;
    // uri -> {pixmap, lru_iterator}
    struct Entry {
        // cache 持有的强引用；与 caller 通过 Get 取得的强引用各自独立计数，
        // 任一路径先释放都不会导致另一路径访问到野指针。
        PixmapPtr pixmap;
        std::list<std::string>::iterator lru_it;
    };
    std::unordered_map<std::string, Entry> cache_;
    // 飞行中：uri -> 等待回调列表。
    std::unordered_map<std::string, std::vector<LoadedCallback>> waiters_;

    // ---- 解码串行化：基于 KRDispatchQueue（P2-2 第二次重构）----
    //
    // 设计要点：
    //   * 用 ffrt 的 serial queue 替代手写 "std::thread + condvar + queue" worker：
    //     - 不再独占 OS 线程：idle 时零内存 / 零调度成本；ffrt 内部按需拉起 worker；
    //     - 不需要 stop / join 协议：析构 KRDispatchQueue 即触发 ffrt_queue_destroy，
    //       已在跑的任务跑完，未执行的任务被 cancel 并通过 TaskCleanup 释放堆 task；
    //     - QoS 标记为 Utility（图片解码 = 用户感知但不阻塞主交互），让 ffrt 调度
    //       器在主线程繁忙时把解码挪后；
    //   * 构造时创建：KRDispatchQueue 不独占 OS 线程，idle 时不消耗 worker；提前创建
    //     可避免每次新 uri 解码提交前额外获取 mu_ 做懒初始化检查。
    std::unique_ptr<kuikly::dispatch::KRDispatchQueue> decode_queue_;
};

#endif  // CORE_RENDER_OHOS_KRCUSTOMEMOJIPIXMAPCACHE_H
