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

#ifndef CORE_RENDER_OHOS_JSON_ALLOCATOR_H
#define CORE_RENDER_OHOS_JSON_ALLOCATOR_H

#include <cstddef>
#include <new>
#include <utility>
#include <atomic>

namespace kuikly {
namespace util {
namespace json {

namespace alloc_detail {
void *SysAlloc(size_t bytes, size_t alignment);
void SysFree(void *ptr);
void *SmallAlloc();
void SmallFree(void *p);
}  // namespace alloc_detail

template <size_t Alignment>
constexpr size_t NormalizedAlignment() {
    static_assert(Alignment > 0, "alignment must be > 0");
    static_assert((Alignment & (Alignment - 1)) == 0, "alignment must be a power of two");
    constexpr size_t min_align = sizeof(void *);
    return Alignment < min_align ? min_align : Alignment;
}

constexpr size_t NormalizedSize(size_t bytes, size_t alignment) {
    if (bytes == 0) {
        bytes = alignment;
    }
    return (bytes + alignment - 1) & ~(alignment - 1);
}

// Heap allocation trait for KRJSON boxes.
// A type T is an allocator if it provides:
//   template <size_t Alignment> static void *Allocate(size_t bytes);
//   template <size_t Alignment> static void Deallocate(void *ptr, size_t bytes);
// Alignment is a compile-time power of two (typically alignof(T)). The returned
// pointer satisfies `ptr % max(Alignment, sizeof(void*)) == 0` for tagging.
// Allocate and Deallocate must be instantiated with the same Alignment; bytes
// must match. Do not assume a 1:1 posix_memalign (small sizes may use a slab).

struct DefaultHeapAllocator {
    // 16-aligned slots covering StringBox + 32 UTF-16 units + NUL
    // (JsonPlatform p90/p95) and NumberBox (alignas 16).
    static constexpr size_t kSmallSlotAlign = 16;
    static constexpr size_t kSmallSlotBytes =
#ifdef KRJSON_DISABLE_SLAB
        0
#else
        128
#endif
        ;
    static constexpr int kSmallChunkSlots = 256;

    template <size_t Alignment>
    static void *Allocate(size_t bytes) {
        constexpr size_t alignment = NormalizedAlignment<Alignment>();
        bytes = NormalizedSize(bytes, alignment);
        if constexpr (alignment <= kSmallSlotAlign) {
            if (bytes <= kSmallSlotBytes) {
                return alloc_detail::SmallAlloc();
            }
        }
        return alloc_detail::SysAlloc(bytes, alignment);
    }

    template <size_t Alignment>
    static void Deallocate(void *ptr, size_t bytes) {
        if (ptr == nullptr) {
            return;
        }
        constexpr size_t alignment = NormalizedAlignment<Alignment>();
        bytes = NormalizedSize(bytes, alignment);
        if constexpr (alignment <= kSmallSlotAlign) {
            if (bytes <= kSmallSlotBytes) {
                alloc_detail::SmallFree(ptr);
                return;
            }
        }
        alloc_detail::SysFree(ptr);
    }
};

extern std::atomic<long> g_krjson_live_boxes;

template <typename Alloc, typename T, typename... Args>
T *AllocNew(Args &&...args) {
    void *mem = Alloc::template Allocate<alignof(T)>(sizeof(T));
    auto *p = ::new (mem) T(std::forward<Args>(args)...);
#if defined(KRJSON_LEAK_TEST)
    g_krjson_live_boxes.fetch_add(1, std::memory_order_relaxed);
#endif
    return p;
}

template <typename Alloc, typename T>
void AllocDelete(T *ptr) {
    ptr->~T();
    Alloc::template Deallocate<alignof(T)>(ptr, sizeof(T));
}

}  // namespace json
}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_JSON_ALLOCATOR_H
