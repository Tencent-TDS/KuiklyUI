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

#include "libohos_render/utils/json/Allocator.h"

#include <cstdlib>
#include <mutex>
#include <new>
#include <stdlib.h>

namespace kuikly {
namespace util {
namespace json {
namespace alloc_detail {

void *SysAlloc(size_t bytes, size_t alignment) {
    void *mem = nullptr;
    if (posix_memalign(&mem, alignment, bytes) != 0 || mem == nullptr) {
        std::abort();
    }
    return mem;
}

void SysFree(void *ptr) {
    std::free(ptr);
}

}  // namespace alloc_detail

namespace {

struct SmallChunk {
    SmallChunk *next = nullptr;
    alignas(DefaultHeapAllocator::kSmallSlotAlign) char
        slots[DefaultHeapAllocator::kSmallChunkSlots][DefaultHeapAllocator::kSmallSlotBytes];
};

struct SmallFreeNode {
    SmallFreeNode *next;
};

std::mutex g_small_mu;
SmallChunk *g_small_chunks = nullptr;
SmallFreeNode *g_small_free = nullptr;

constexpr int kSmallTlsBatch = 32;
constexpr int kSmallTlsMax = 64;

void SmallGrowLocked() {
    void *mem = alloc_detail::SysAlloc(sizeof(SmallChunk), alignof(SmallChunk));
    auto *chunk = ::new (mem) SmallChunk();
    chunk->next = g_small_chunks;
    g_small_chunks = chunk;
    for (int i = 0; i < DefaultHeapAllocator::kSmallChunkSlots; ++i) {
        auto *node = reinterpret_cast<SmallFreeNode *>(&chunk->slots[i][0]);
        node->next = g_small_free;
        g_small_free = node;
    }
}

struct SmallTlsCache {
    SmallFreeNode *head = nullptr;
    int count = 0;

    ~SmallTlsCache() { FlushAll(); }

    void FlushAll() {
        if (head == nullptr) {
            return;
        }
        std::lock_guard<std::mutex> lock(g_small_mu);
        while (head != nullptr) {
            SmallFreeNode *node = head;
            head = node->next;
            node->next = g_small_free;
            g_small_free = node;
        }
        count = 0;
    }
};

thread_local SmallTlsCache t_small_cache;

void SmallRefillTls() {
    std::lock_guard<std::mutex> lock(g_small_mu);
    if (g_small_free == nullptr) {
        SmallGrowLocked();
    }
    int n = 0;
    while (g_small_free != nullptr && n < kSmallTlsBatch) {
        SmallFreeNode *node = g_small_free;
        g_small_free = node->next;
        node->next = t_small_cache.head;
        t_small_cache.head = node;
        ++t_small_cache.count;
        ++n;
    }
}

void SmallSpillTls() {
    std::lock_guard<std::mutex> lock(g_small_mu);
    int n = 0;
    while (t_small_cache.head != nullptr && n < kSmallTlsBatch) {
        SmallFreeNode *node = t_small_cache.head;
        t_small_cache.head = node->next;
        --t_small_cache.count;
        node->next = g_small_free;
        g_small_free = node;
        ++n;
    }
}

void *DoSmallAlloc() {
    if (t_small_cache.head == nullptr) {
        SmallRefillTls();
    }
    SmallFreeNode *node = t_small_cache.head;
    t_small_cache.head = node->next;
    --t_small_cache.count;
    return node;
}

void DoSmallFree(void *p) {
    auto *node = static_cast<SmallFreeNode *>(p);
    node->next = t_small_cache.head;
    t_small_cache.head = node;
    ++t_small_cache.count;
    if (t_small_cache.count > kSmallTlsMax) {
        SmallSpillTls();
    }
}

}  // namespace

namespace alloc_detail {

void *SmallAlloc() {
    return DoSmallAlloc();
}

void SmallFree(void *p) {
    DoSmallFree(p);
}

}  // namespace alloc_detail

}  // namespace json
}  // namespace util
}  // namespace kuikly
