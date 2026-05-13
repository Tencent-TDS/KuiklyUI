// 压测程序: stress_schedule_dealloc_render_values
//
// 目标:
//   验证 KRRenderNativeContextHandlerManager::ScheduleDeallocRenderValues
//   在方案 A(std::atomic<bool> + CAS + 先 store(false) 再析构 values)下的并发安全性。
//
// 为什么不直接链接生产代码:
//   1) 生产代码依赖 pthread_spinlock_t, macOS 未提供该接口;
//   2) 生产代码依赖 napi / KRContextScheduler 等 OHOS 运行时;
//   因此本文件原地复刻与生产实现在并发语义上完全一致的最小骨架,
//   若生产实现有变更, 需同步更新本压测。
//
// 编译(macOS/Linux 均可):
//   clang++ -std=c++17 -O2 -pthread stress_schedule_dealloc_render_values.cpp \
//       -o stress_sdrv
//   开启 TSAN:
//   clang++ -std=c++17 -O1 -g -pthread -fsanitize=thread \
//       stress_schedule_dealloc_render_values.cpp -o stress_sdrv_tsan
//   运行:
//   ./stress_sdrv                 # 默认配置
//   ./stress_sdrv 16 200000       # 16 producer, 每个 producer push 20w 次
//
// 验证项:
//   A. 不丢单      : 所有 push 的 value 都被调度线程析构 (destroyed == pushed)
//   B. 不重复投递  : schedule_task 的 invoke 次数 <= pushed, 且 >= 1
//   C. 析构重入    : 在 value 析构中再 push 一条, 也不能卡死调度
//   D. 数据竞争    : TSAN 下无 warning (需要用户本机跑一次 TSAN 变体)

#include <atomic>
#include <cassert>
#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstdlib>
#include <functional>
#include <memory>
#include <mutex>
#include <queue>
#include <string>
#include <thread>
#include <vector>

// ---------------------------------------------------------------------------
// 0. 轻量 spinlock (等价于生产 KRSpinLock, 但跨平台)
// ---------------------------------------------------------------------------
class MiniSpinLock {
 public:
    void lock() {
        while (flag_.test_and_set(std::memory_order_acquire)) {
            std::this_thread::yield();
        }
    }
    void unlock() { flag_.clear(std::memory_order_release); }

 private:
    std::atomic_flag flag_ = ATOMIC_FLAG_INIT;
};

class ScopedMiniSpin {
 public:
    explicit ScopedMiniSpin(MiniSpinLock* l) : l_(l) { l_->lock(); }
    ~ScopedMiniSpin() { l_->unlock(); }
    ScopedMiniSpin(const ScopedMiniSpin&) = delete;
    ScopedMiniSpin& operator=(const ScopedMiniSpin&) = delete;

 private:
    MiniSpinLock* l_;
};

// ---------------------------------------------------------------------------
// 1. Scheduler 替身: 把 task 丢到后台线程串行执行, 模拟 context 线程
// ---------------------------------------------------------------------------
class BackgroundScheduler {
 public:
    BackgroundScheduler() : stop_(false), invoked_(0) {
        worker_ = std::thread([this]() { Loop(); });
    }
    ~BackgroundScheduler() {
        {
            std::lock_guard<std::mutex> g(mu_);
            stop_ = true;
            cv_.notify_all();
        }
        if (worker_.joinable()) worker_.join();
    }

    void Schedule(std::function<void()> task) {
        std::lock_guard<std::mutex> g(mu_);
        q_.push(std::move(task));
        cv_.notify_one();
    }

    void DrainAndStop() {
        // 等待队列排空
        for (;;) {
            {
                std::lock_guard<std::mutex> g(mu_);
                if (q_.empty() && !running_) break;
            }
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
        }
        {
            std::lock_guard<std::mutex> g(mu_);
            stop_ = true;
            cv_.notify_all();
        }
        if (worker_.joinable()) worker_.join();
    }

    uint64_t InvokedCount() const { return invoked_.load(); }

 private:
    void Loop() {
        for (;;) {
            std::function<void()> task;
            {
                std::unique_lock<std::mutex> lk(mu_);
                cv_.wait(lk, [this]() { return stop_ || !q_.empty(); });
                if (stop_ && q_.empty()) return;
                task = std::move(q_.front());
                q_.pop();
                running_ = true;
            }
            task();
            invoked_.fetch_add(1, std::memory_order_relaxed);
            {
                std::lock_guard<std::mutex> g(mu_);
                running_ = false;
            }
        }
    }

    std::mutex mu_;
    std::condition_variable cv_;
    std::queue<std::function<void()>> q_;
    bool stop_;
    bool running_ = false;
    std::thread worker_;
    std::atomic<uint64_t> invoked_;
};

// ---------------------------------------------------------------------------
// 2. DummyValue: 模拟 shared_ptr<KRRenderValue>
//    - 析构时计数 ++
//    - 若 reenter_on_destroy_ == true, 在析构时再次触发一次 push
//      (模拟 C 项: 析构链中重入 ScheduleDeallocRenderValues)
// ---------------------------------------------------------------------------
struct ManagerFacade;
static ManagerFacade* g_mgr = nullptr;
static std::atomic<uint64_t> g_destroyed{0};
static std::atomic<uint64_t> g_reentered_pushes{0};

struct DummyValue {
    bool reenter_on_destroy;
    DummyValue() : reenter_on_destroy(false) {}
    explicit DummyValue(bool r) : reenter_on_destroy(r) {}
    ~DummyValue();  // 声明, 定义放在 ManagerFacade 之后
};

// ---------------------------------------------------------------------------
// 3. ManagerFacade: 复刻 KRRenderNativeContextHandlerManager 中
//                   ScheduleDeallocRenderValues 的并发语义(方案 A)
// ---------------------------------------------------------------------------
struct ManagerFacade {
    std::atomic<bool> scheduling_{false};
    MiniSpinLock pending_lock_;
    std::vector<std::shared_ptr<DummyValue>> pending_;
    BackgroundScheduler scheduler_;

    std::atomic<uint64_t> pushed_{0};
    std::atomic<uint64_t> cas_won_{0};   // CAS 成功次数 == 调度任务投递次数

    void ScheduleDealloc(std::shared_ptr<DummyValue> v) {
        pushed_.fetch_add(1, std::memory_order_relaxed);
        {
            ScopedMiniSpin g(&pending_lock_);
            pending_.push_back(std::move(v));
        }
        bool expected = false;
        if (scheduling_.compare_exchange_strong(expected, true)) {
            cas_won_.fetch_add(1, std::memory_order_relaxed);
            scheduler_.Schedule([this]() {
                std::vector<std::shared_ptr<DummyValue>> values;
                {
                    ScopedMiniSpin g(&pending_lock_);
                    values.swap(pending_);
                }
                // 必须先 store(false) 再让 values 析构, 否则析构链中若再 push
                // 会因标志仍为 true 而永久无法再投递。
                scheduling_.store(false);
                // values 在作用域结束时析构 -> 可能触发 DummyValue::~DummyValue()
                // -> 可能重入 ScheduleDealloc (这正是我们要压的场景 C)
            });
        }
    }
};

DummyValue::~DummyValue() {
    g_destroyed.fetch_add(1, std::memory_order_relaxed);
    if (reenter_on_destroy && g_mgr != nullptr) {
        g_reentered_pushes.fetch_add(1, std::memory_order_relaxed);
        // 重入 push, 模拟析构链中再次触发 ScheduleDealloc
        // 注意这条 value 自己不再 reenter, 避免无限递归
        g_mgr->ScheduleDealloc(std::make_shared<DummyValue>(false));
    }
}

// ---------------------------------------------------------------------------
// 4. 压测主循环
// ---------------------------------------------------------------------------
static void RunStress(int num_producers, int per_producer, double reenter_rate) {
    ManagerFacade mgr;
    g_mgr = &mgr;
    g_destroyed.store(0);
    g_reentered_pushes.store(0);

    auto t0 = std::chrono::steady_clock::now();

    std::vector<std::thread> producers;
    producers.reserve(num_producers);
    for (int i = 0; i < num_producers; ++i) {
        producers.emplace_back([&mgr, per_producer, reenter_rate, i]() {
            // 每个线程用独立的 RNG 种子, 避免全局锁
            uint32_t seed = static_cast<uint32_t>(i * 2654435761u + 1);
            for (int k = 0; k < per_producer; ++k) {
                seed ^= seed << 13;
                seed ^= seed >> 17;
                seed ^= seed << 5;
                bool reenter = ((seed & 0xFF) / 255.0) < reenter_rate;
                mgr.ScheduleDealloc(std::make_shared<DummyValue>(reenter));
            }
        });
    }
    for (auto& t : producers) t.join();

    // 所有 producer 结束后, 等 scheduler 处理完队列
    mgr.scheduler_.DrainAndStop();

    auto t1 = std::chrono::steady_clock::now();
    double ms = std::chrono::duration<double, std::milli>(t1 - t0).count();

    uint64_t pushed = mgr.pushed_.load();
    uint64_t destroyed = g_destroyed.load();
    uint64_t cas_won = mgr.cas_won_.load();
    uint64_t reentered = g_reentered_pushes.load();
    uint64_t invoked = mgr.scheduler_.InvokedCount();

    std::printf("------------------------------------------------------------\n");
    std::printf("Producers          : %d\n", num_producers);
    std::printf("Per producer       : %d\n", per_producer);
    std::printf("Reenter rate       : %.2f%%\n", reenter_rate * 100.0);
    std::printf("Elapsed            : %.2f ms\n", ms);
    std::printf("Pushed  (raw+reent): %llu\n", (unsigned long long)pushed);
    std::printf("Destroyed          : %llu\n", (unsigned long long)destroyed);
    std::printf("CAS won (dispatched): %llu\n", (unsigned long long)cas_won);
    std::printf("Scheduler invoked  : %llu\n", (unsigned long long)invoked);
    std::printf("Reenter pushes     : %llu\n", (unsigned long long)reentered);

    bool ok = true;

    // 断言 A: 不丢单
    if (destroyed != pushed) {
        std::printf("[FAIL A] destroyed(%llu) != pushed(%llu)\n",
                    (unsigned long long)destroyed, (unsigned long long)pushed);
        ok = false;
    } else {
        std::printf("[PASS A] destroyed == pushed\n");
    }

    // 断言 B: 不重复投递 & 至少投递一次
    if (cas_won < 1 || cas_won > pushed) {
        std::printf("[FAIL B] cas_won(%llu) 超出合理范围 [1, %llu]\n",
                    (unsigned long long)cas_won, (unsigned long long)pushed);
        ok = false;
    } else {
        std::printf("[PASS B] 1 <= cas_won <= pushed\n");
    }
    if (cas_won != invoked) {
        std::printf("[FAIL B2] cas_won(%llu) != scheduler.invoked(%llu)\n",
                    (unsigned long long)cas_won, (unsigned long long)invoked);
        ok = false;
    } else {
        std::printf("[PASS B2] cas_won == scheduler.invoked\n");
    }

    // 断言 C: 析构重入不死锁 / 不死寂 (已经能跑到这里即证明没卡死;
    //          如果卡死主线程会永远 join 不完)
    if (reenter_rate > 0.0 && reentered == 0) {
        std::printf("[FAIL C] 开启了 reenter 但 reentered = 0\n");
        ok = false;
    } else {
        std::printf("[PASS C] 析构链重入未死锁/未死寂 (reentered=%llu)\n",
                    (unsigned long long)reentered);
    }

    std::printf("%s\n", ok ? ">>> ALL PASS <<<" : ">>> FAILED <<<");
    std::printf("------------------------------------------------------------\n\n");

    g_mgr = nullptr;
    if (!ok) std::exit(1);
}

int main(int argc, char** argv) {
    int producers = 8;
    int per_producer = 50000;
    if (argc >= 2) producers = std::atoi(argv[1]);
    if (argc >= 3) per_producer = std::atoi(argv[2]);

    std::printf("\n=== Stress: ScheduleDeallocRenderValues (plan A) ===\n");

    // 轮次 1: 纯 push, 不重入
    RunStress(producers, per_producer, 0.0);

    // 轮次 2: 1% 概率在析构中重入 push
    RunStress(producers, per_producer, 0.01);

    // 轮次 3: 10% 概率重入, 压榨析构链
    RunStress(producers, per_producer, 0.10);

    // 轮次 4: 高并发小 batch, 放大 CAS 竞争
    RunStress(std::max(producers * 2, 16), 5000, 0.05);

    std::printf("=== DONE ===\n");
    return 0;
}
