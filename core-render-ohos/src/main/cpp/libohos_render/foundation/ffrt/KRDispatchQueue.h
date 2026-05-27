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

#ifndef CORE_RENDER_OHOS_KRDISPATCHQUEUE_H
#define CORE_RENDER_OHOS_KRDISPATCHQUEUE_H

#include <cstddef>
#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <type_traits>
#include <utility>

#include "libohos_render/foundation/ffrt/KRFfrt.h"

#if defined(__has_builtin)
#if __has_builtin(__builtin_FILE_NAME)
#define KR_DISPATCH_QUEUE_CALLER_FILE __builtin_FILE_NAME()
#elif __has_builtin(__builtin_FILE)
#define KR_DISPATCH_QUEUE_CALLER_FILE __builtin_FILE()
#endif
#endif

#ifndef KR_DISPATCH_QUEUE_CALLER_FILE
#if defined(__FILE_NAME__)
#define KR_DISPATCH_QUEUE_CALLER_FILE __FILE_NAME__
#else
#define KR_DISPATCH_QUEUE_CALLER_FILE __FILE__
#endif
#endif

#if defined(__has_builtin)
#if __has_builtin(__builtin_LINE)
#define KR_DISPATCH_QUEUE_CALLER_LINE __builtin_LINE()
#endif
#endif

#ifndef KR_DISPATCH_QUEUE_CALLER_LINE
#define KR_DISPATCH_QUEUE_CALLER_LINE __LINE__
#endif

namespace kuikly {
namespace dispatch {
namespace detail {

constexpr const char *kUnknownLocation = "<unknown>";
constexpr std::size_t kSourceLocationCapacity = 48;

constexpr const char *BaseName(const char *path, std::size_t *length) {
    if (!path) {
        path = kUnknownLocation;
    }

    const char *base = path;
    std::size_t base_length = 0;
    for (const char *p = path; *p != '\0'; ++p) {
        if (*p == '/' || *p == '\\') {
            base = p + 1;
            base_length = 0;
        } else {
            ++base_length;
        }
    }

    if (length) {
        *length = base_length;
    }
    return base;
}

constexpr std::size_t UIntLength(unsigned int value) {
    std::size_t length = 1;
    while (value >= 10) {
        value /= 10;
        ++length;
    }
    return length;
}

}  // namespace detail

class KRDispatchTask;

class KRSourceLocation {
 public:
    constexpr KRSourceLocation() : KRSourceLocation(detail::kUnknownLocation, 0) {}

    constexpr KRSourceLocation(const char *file, int line) {
        std::size_t file_length = 0;
        const char *source_file = detail::BaseName(file, &file_length);

        unsigned int line_value =
            line < 0 ? static_cast<unsigned int>(-(line + 1)) + 1U : static_cast<unsigned int>(line);
        const std::size_t digit_count = detail::UIntLength(line_value);
        const std::size_t suffix_length = 1 + (line < 0 ? 1 : 0) + digit_count;
        const std::size_t max_file_length =
            detail::kSourceLocationCapacity > suffix_length + 1
                ? detail::kSourceLocationCapacity - suffix_length - 1
                : 0;

        const std::size_t copy_length = file_length > max_file_length ? max_file_length : file_length;
        const char *file_start = source_file + file_length - copy_length;

        std::size_t index = 0;
        for (; index < copy_length; ++index) {
            location_[index] = file_start[index];
        }
        location_[index++] = ':';

        if (line < 0) {
            location_[index++] = '-';
        }

        for (std::size_t digit_index = digit_count; digit_index > 0; --digit_index) {
            unsigned int divisor = 1;
            for (std::size_t i = 1; i < digit_index; ++i) {
                divisor *= 10;
            }
            location_[index++] = static_cast<char>('0' + (line_value / divisor) % 10);
        }
        location_[index] = '\0';
    }

    constexpr const char *Location() const { return location_; }

 private:
    char location_[detail::kSourceLocationCapacity]{};
};

class KRDispatchTask {
 public:
    KRDispatchTask() = default;

    template <typename Callable,
              typename = typename std::enable_if<
                  !std::is_same<typename std::decay<Callable>::type, KRDispatchTask>::value &&
                  std::is_constructible<std::function<void()>, Callable &&>::value>::type>
    KRDispatchTask(Callable &&callable) : task_(std::forward<Callable>(callable)) {}

    explicit operator bool() const { return static_cast<bool>(task_); }

    void operator()() const { task_(); }

 private:
    std::function<void()> task_;
};

/**
 * @brief Queue type — 串行 / 并发。
 *
 * Serial：FIFO，逻辑上单消费者；ffrt 内部仍然按调度策略复用 worker 线程，
 *        但用户层语义保证"前一个 task 完成后才执行下一个"。常用于"需要保护
 *        共享数据但又不想用 mutex"的场景，等价于 GCD serial queue / iOS
 *        DISPATCH_QUEUE_SERIAL。
 *
 * Concurrent：多任务并发执行，受 max_concurrency 上限约束（since API 12）。
 *        适合 IO-bound、彼此独立的工作负载（如多源图片解码批量预热）。
 *        默认 max_concurrency = 4，与 GCD global queue 行为接近。
 */
enum class QueueType {
    Serial,
    Concurrent,
};

/**
 * @brief Quality of Service — 与 ffrt_qos_t 对齐的强类型枚举。
 *
 * 数值与 ffrt_qos_default_t 完全一致（见 ffrt/type_def.h），可直接 static_cast。
 *
 * 选用建议：
 *   - Background：日志落盘、metrics 上报、低优先级缓存预热
 *   - Utility：图片解码、字体加载等 "用户感知但不阻塞主交互" 的工作
 *   - Default：通用业务任务，未指定时的默认值
 *   - UserInitiated：响应用户主动操作（如点击触发的网络请求处理）
 *   - UserInteractive：动画 / 输入事件 / 与帧相关的渲染前置任务
 */
enum class QoS : int {
    Inherit         = -1,  // 继承提交方的 QoS，仅在 ffrt 任务嵌套场景有意义
    Background      = 0,
    Utility         = 1,
    Default         = 2,
    UserInitiated   = 3,
    UserInteractive = 5,
};

/**
 * @brief 通用 DispatchQueue 任务队列，基于 OHOS libffrt 的 ffrt_queue_t 封装。
 *
 * 设计目标：
 *   1) 替代 native 业务自行造的 "std::thread + condvar + queue" 工作线程模式；
 *   2) 与项目已有 KRFfrt.cpp 工厂 (ffrt_create_function_wrapper) 复用 lifecycle，
 *      task body / cleanup 都跑在 ffrt 调度的线程上；
 *   3) 接口形态向 GCD（dispatch_queue_t）/ iOS NSOperationQueue 看齐，C++ 友好；
 *   4) RAII：构造即创建底层 queue，析构即销毁；销毁时 ffrt 会等待已提交任务完成。
 *
 * 线程模型与 std::thread 自管 worker 的对比：
 *   - 自管 worker：每个业务实例 1 个常驻 thread；50 个实例 = 50 个 OS thread，
 *     退出时还要手动 stop + join；
 *   - DispatchQueue：底层 queue 与 ffrt 全局线程池共享，无独占 OS thread；
 *     idle 时不消耗内存；任务密集时由 ffrt 调度复用 worker。
 *
 * 用法示例（serial，与 GCD dispatch_async 等价）：
 * @code
 *   auto q = std::make_unique<KRDispatchQueue>("decode_queue");
 *   q->Async([uri] { Decode(uri); });
 *   q->AsyncAfter(100, [] { TimeoutCheck(); });
 *   // 析构时自动 ffrt_queue_destroy
 * @endcode
 *
 * @note Async / AsyncAfter 提交的 task 在 ffrt worker 线程执行，**不在调用方
 *       线程执行**。如需切回主线程，请显式使用 KRMainThread::RunOnMainThread。
 *
 * @note 单 KRDispatchQueue 实例线程安全：Async 可在任意线程并发调用。
 *       但 KRDispatchQueue 本身的拷贝 / 移动均被禁用，因为 ffrt_queue_t 句柄
 *       具有独占资源语义。
 */
class KRDispatchQueue {
 public:
    using Task = KRDispatchTask;

    /**
     * @brief 创建一个 DispatchQueue。
     *
     * @param name             队列名（用于 ffrt 调度日志 / hilog 定位；建议 ASCII，
     *                         不超过 ~32 字符）。
     * @param type             Serial / Concurrent。
     * @param qos              默认 QoS（每个 Async 任务可单独通过未来扩展覆盖）。
     * @param max_concurrency  仅对 Concurrent 生效（Serial 时忽略）；默认 4。
     *                         需要 SDK API 12+；运行在低于 API 12 的设备上时
     *                         ffrt 会回退到默认并发度。
     */
    explicit KRDispatchQueue(const std::string &name,
                             QueueType type = QueueType::Serial,
                             QoS qos = QoS::Default,
                             int max_concurrency = 4);

    ~KRDispatchQueue();

    KRDispatchQueue(const KRDispatchQueue &) = delete;
    KRDispatchQueue &operator=(const KRDispatchQueue &) = delete;
    KRDispatchQueue(KRDispatchQueue &&) = delete;
    KRDispatchQueue &operator=(KRDispatchQueue &&) = delete;

    /**
     * @brief 异步派发一个任务到本队列；fire-and-forget。
     *
     * 行为：
     *   - Serial 队列：任务按 FIFO 严格串行执行；
     *   - Concurrent 队列：受 max_concurrency 约束的并发执行。
     *
     * task 通过 std::function 拷贝持有，捕获的对象按 lambda 捕获语义复制 / 共享。
     * **不要捕获原始 this 指针指向的栈对象** —— 任务执行时 caller 帧可能已退栈。
     *
     * 若 queue 创建失败（ctor 中 ffrt_queue_create 返回 nullptr），Async 会
     * **同步** 在调用线程执行 task —— 退化保证 caller 看到的语义不变（避免
     * "提交后无任何回调" 的静默丢失）。
     *
     * Task 会自动携带 Async / AsyncAfter 调用点的 `文件名:行号` 作为 location，便于排查
     * schedule 进队列的任务来源。
     */
    void Async(Task task,
               KRSourceLocation location = KRSourceLocation(KR_DISPATCH_QUEUE_CALLER_FILE,
                                                            KR_DISPATCH_QUEUE_CALLER_LINE));

    /**
     * @brief 延迟若干毫秒后异步派发。底层走 ffrt_task_attr_set_delay。
     *
     * @param delay_ms  延迟毫秒数；0 等价于 Async。
     * @param task      任务体（捕获语义同 Async）。
     */
    void AsyncAfter(uint64_t delay_ms,
                    Task task,
                    KRSourceLocation location = KRSourceLocation(KR_DISPATCH_QUEUE_CALLER_FILE,
                                                                 KR_DISPATCH_QUEUE_CALLER_LINE));

    /**
     * @brief 取得底层 ffrt_queue_t 句柄。仅供调试 / 测试。
     *        正常业务代码不应直接操作该句柄。
     */
    ffrt_queue_t Handle() const { return queue_; }

    /**
     * @brief 队列名（构造时传入；析构后仍有效，用于错误日志）。
     */
    const std::string &Name() const { return name_; }

 private:
    struct TaskItem {
        KRSourceLocation location;
        Task task;
    };

    /// ffrt task body，转 void* arg 回 TaskItem* 并执行。
    static void TaskExecutor(void *arg);
    /// ffrt task cleanup（after_func）；释放 TaskItem。
    /// 仅作 ffrt 协议占位，避免极端 cleanup-without-exec 路径泄漏。
    static void TaskCleanup(void *arg);

    void Submit(Task task, uint64_t delay_ms, KRSourceLocation location);

    std::string name_;
    ffrt_queue_t queue_{nullptr};
};

}  // namespace dispatch
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRDISPATCHQUEUE_H