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
 *
 * KRDispatchQueue.cpp — 基于 OHOS libffrt 的通用任务队列实现。
 * 设计与 lifecycle 详见 KRDispatchQueue.h 顶部注释。
 */

#include "libohos_render/foundation/ffrt/KRDispatchQueue.h"

#include <exception>
#include <utility>

#include "libohos_render/utils/KRRenderLoger.h"

namespace kuikly {
namespace dispatch {

// ----------------------------------------------------------------------
// Lifecycle 设计要点（critical path）：
//
//   提交 Async(task) 的步骤：
//     1) heap-allocate `TaskItem* heap_item = new TaskItem{location, std::move(task)};`
//     2) ffrt_create_function_wrapper(TaskExecutor, TaskCleanup, heap_item,
//                                       ffrt_function_kind_queue) → header
//     3) ffrt_queue_submit(queue_, header, attr)
//
//   ffrt 内部对 header 的两次回调（见 KRFfrt.cpp）：
//     - exec  → TaskExecutor(arg = heap_item) ：跑用户 lambda
//     - destroy → TaskCleanup(arg = heap_item) ：delete heap_item
//
//   两次回调的 arg 来自同一字段（ffrt_function_wrapper_t::arg），在 normal /
//   cancel / timeout 等路径下 destroy 都会被调用，所以 **delete 集中在
//   TaskCleanup**，TaskExecutor 不再 delete。这样：
//     - 正常路径：exec 跑 lambda；destroy 释放 TaskItem。
//     - 取消路径：exec 不跑；destroy 仍释放 TaskItem，避免泄漏。
//   把 TaskItem 的所有权完全托管给 wrapper 框架，简单且无双重释放风险。
// ----------------------------------------------------------------------

void KRDispatchQueue::TaskExecutor(void *arg) {
    auto *heap_item = static_cast<TaskItem *>(arg);
    if (!heap_item) {
        return;
    }
    // try/catch 把任务里的异常吃掉 —— ffrt worker 抛异常会让进程崩溃；与 KRMemoryMonitor
    // 风格保持一致（那边也是 try-catch all）。业务自身需要异常捕获时应在 lambda 内处理。
    try {
        if (heap_item->task) {
            heap_item->task();
        }
    } catch (const std::exception &e) {
        KR_LOG_ERROR << "[KRDispatchQueue] location=" << heap_item->location.Location()
                     << " threw std::exception: " << e.what();
    } catch (...) {
        KR_LOG_ERROR << "[KRDispatchQueue] location=" << heap_item->location.Location()
                     << " threw non-std exception";
    }
}

void KRDispatchQueue::TaskCleanup(void *arg) {
    auto *heap_item = static_cast<TaskItem *>(arg);
    delete heap_item;  // delete nullptr 安全；正常路径与 cancel 路径都走这里
}

// ----------------------------------------------------------------------
// 构造：创建 ffrt_queue_t。失败时 queue_ 留 nullptr，Async 走同步退化路径。
// ----------------------------------------------------------------------
KRDispatchQueue::KRDispatchQueue(const std::string &name, QueueType type, QoS qos,
                                   int max_concurrency)
    : name_(name) {
    ffrt_queue_attr_t attr;
    if (ffrt_queue_attr_init(&attr) != 0) {
        KR_LOG_ERROR << "[KRDispatchQueue] ffrt_queue_attr_init failed for queue=" << name;
        return;
    }
    ffrt_queue_attr_set_qos(&attr, static_cast<ffrt_qos_t>(qos));

    // max_concurrency 只对 ffrt_queue_concurrent 有意义；ffrt 在 SDK < 12 上
    // 该接口可能不存在。我们靠链接器的弱符号 / SDK header 守护：当前最低 API
    // 已经 >= 12（与 KRMemoryMonitor / Prefetch 用的 ffrt_submit_base 同基线），
    // 直接调用即可；若未来需要降低基线可在此加 weak guard。
    if (type == QueueType::Concurrent && max_concurrency > 1) {
        ffrt_queue_attr_set_max_concurrency(&attr, max_concurrency);
    }

    const ffrt_queue_type_t ffrt_type =
        (type == QueueType::Serial) ? ffrt_queue_serial : ffrt_queue_concurrent;
    queue_ = ffrt_queue_create(ffrt_type, name_.c_str(), &attr);
    ffrt_queue_attr_destroy(&attr);

    if (!queue_) {
        KR_LOG_ERROR << "[KRDispatchQueue] ffrt_queue_create returned null for queue=" << name_;
    }
}

// ----------------------------------------------------------------------
// 析构：销毁底层 queue。ffrt 文档（queue.h）中 ffrt_queue_destroy 会等待
// 已提交但尚未执行的任务被 cancel，并释放资源；正在执行中的 task 会跑完。
// 因此析构后即可保证：
//   * 没有任何后续任务再被调度；
//   * 仍在执行的 task 会安全完成（执行完后才返回）；
//   * 所有未执行任务的 TaskItem 都通过 TaskCleanup 路径释放。
// ----------------------------------------------------------------------
KRDispatchQueue::~KRDispatchQueue() {
    if (queue_) {
        ffrt_queue_destroy(queue_);
        queue_ = nullptr;
    }
}

// ----------------------------------------------------------------------
// Async：把 TaskItem 堆分配后塞入 ffrt 队列；若队列创建失败则同步退化。
// ----------------------------------------------------------------------
void KRDispatchQueue::Async(Task task, KRSourceLocation location) {
    Submit(std::move(task), 0, location);
}

void KRDispatchQueue::Submit(Task task, uint64_t delay_ms, KRSourceLocation source_location) {
    if (!task) {
        return;  // 显式空 task，无操作；不创建 wrapper、不分配
    }

    const char *location = source_location.Location();
    if (!location) {
        location = detail::kUnknownLocation;
    }

    if (!queue_) {
        // 队列创建失败：同步执行，保证 "提交后必有调用" 的语义不静默断裂。
        try {
            task();
        } catch (const std::exception &e) {
            KR_LOG_ERROR << "[KRDispatchQueue:" << name_ << "] sync-fallback location="
                         << location << " threw: " << e.what();
        } catch (...) {
            KR_LOG_ERROR << "[KRDispatchQueue:" << name_ << "] sync-fallback location="
                         << location << " threw non-std exception";
        }
        return;
    }

    KR_LOG_DEBUG << "[KRDispatchQueue:" << name_ << "] schedule location=" << location
                 << " delay_ms=" << delay_ms;

    auto *heap_item = new TaskItem{source_location, std::move(task)};
    auto *header = ffrt_create_function_wrapper(TaskExecutor, TaskCleanup, heap_item,
                                                  ffrt_function_kind_queue);
    if (!header) {
        // 极端情况：wrapper 分配失败。回退同步执行 + 释放 heap_item。
        KR_LOG_ERROR << "[KRDispatchQueue:" << name_
                     << "] ffrt_create_function_wrapper failed; falling back to sync, location="
                     << heap_item->location.Location();
        try {
            heap_item->task();
        } catch (const std::exception &e) {
            KR_LOG_ERROR << "[KRDispatchQueue:" << name_ << "] sync-fallback location="
                         << heap_item->location.Location() << " threw: " << e.what();
        } catch (...) {
            KR_LOG_ERROR << "[KRDispatchQueue:" << name_ << "] sync-fallback location="
                         << heap_item->location.Location() << " threw non-std exception";
        }
        delete heap_item;
        return;
    }

    if (delay_ms == 0) {
        ffrt_queue_submit(queue_, header, nullptr);
        return;
    }

    ffrt_task_attr_t task_attr;
    ffrt_task_attr_init(&task_attr);
    ffrt_task_attr_set_delay(&task_attr, delay_ms * 1000);  // ms → us
    ffrt_queue_submit(queue_, header, &task_attr);
    ffrt_task_attr_destroy(&task_attr);
}

// ----------------------------------------------------------------------
// AsyncAfter：与 Async 相同，但额外在 task_attr 上设置 delay。
// ----------------------------------------------------------------------
void KRDispatchQueue::AsyncAfter(uint64_t delay_ms, Task task, KRSourceLocation location) {
    Submit(std::move(task), delay_ms, location);
}

}  // namespace dispatch
}  // namespace kuikly