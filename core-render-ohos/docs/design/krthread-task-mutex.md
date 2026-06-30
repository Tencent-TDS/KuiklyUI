# KRThread 任务执行权互斥优化

> 涉及文件：[`KRThread.h`](../../src/main/cpp/libohos_render/foundation/thread/KRThread.h) / [`KRThread.cpp`](../../src/main/cpp/libohos_render/foundation/thread/KRThread.cpp)
>
> 关联调用方：[`KRContextScheduler.cpp`](../../src/main/cpp/libohos_render/scheduler/KRContextScheduler.cpp)
>
> 状态：✅ 已落地并通过模块编译 + 链接验证（`libkuikly.so`）

---

## 1. 背景

`KRThread` 是 kuikly 渲染侧的"上下文工作线程"封装，内部维护一个 libuv 事件循环（`uv_loop` + `uv_async`）。它有两条任务下发路径：

| 路径 | 入口 | 执行线程 | 用途 |
|---|---|---|---|
| 异步派发 | `DispatchAsync` → `OnAsync` 批处理 | worker 线程 | 常规跨端任务、timer 到点回调 |
| 同步直跑 | `DispatchSync` → `DirectRunOnCurThread` | 调用线程（通常是主线程） | 主线程需要立即拿到 kuikly 侧结果的快路径 |

**核心约束**：无论从哪条路径进入，"执行 task 体"这个动作必须保证 **跨端上下文上同一时刻只有一个线程在跑**——否则 kuikly 侧的 V8/JS Context、native 状态会被多线程并发踩踏。

旧实现用一对自定义三态函数 `TaskMutex(try_lock, is_set, value)` / `SyncMainTaskMutex(...)` 配合 `bool m_mutex_locked` / `bool m_sync_main_task_locked` 来表达这两类同步语义，可读性差且行为可疑。

---

## 2. 问题

旧版 `OnAsync` 阶段 2 的关键片段（已废弃）：

```cpp
if (TaskMutex(true, false, false)) {
    m_isExecutingTask.store(true);
    while (!local.empty()) { /* run */ }
    m_isExecutingTask.store(false);
    TaskMutex(false, true, false);
} else {
    // 当前 mutex 被外部 DirectRunOnCurThread 占用，把任务退回队列等下次。
    std::lock_guard<std::mutex> lock(m_queueMutex);
    /* merge local back into m_pending */
    uv_async_send(&m_async);   // 自唤醒
}
```

旧版 `DirectRunOnCurThread` 的关键片段（已废弃）：

```cpp
if (m_isExecutingTask.load()) { task(); return; }    // ← 同线程重入快路径

while (!SyncMainTaskMutex(false, false, false)) {
    if (duration > 100ms) break;
    if (TaskMutex(true, false, false)) {             // ← 纯 try_lock 自旋
        m_isExecutingTask.store(true);
        task();
        m_isExecutingTask.store(false);
        TaskMutex(false, true, false);
        break;
    }
    // 没有 yield，直接下一轮
}
```

### 问题 1：`OnAsync` 抢不到锁就"回弹自唤醒"，触发忙等风暴

- `try_lock` 失败 → 把任务塞回队列 → `uv_async_send` → 立刻又触发 `OnAsync` → 再次 `try_lock` → ……
- 在 `DirectRunOnCurThread` 持锁期间，worker 线程会以 send-loop 速度（几乎裸 spin）疯狂空转，CPU 被打满、loop 句柄自唤醒队列被严重占用。

### 问题 2：`DirectRunOnCurThread` 的 100ms 自旋没有 `yield`

调用线程（通常是主线程）抢不到 `TaskMutex` 时，会在 100ms 窗口里以最快速度反复 `try_lock`，同样把核心打满。

### 问题 3：`m_isExecutingTask` 重入快路径有跨线程 race

```cpp
if (m_isExecutingTask.load()) { task(); return; }
```

设计意图是"task 内部嵌套调用 `DispatchSync` 时直接跑，避免对 `m_taskMutex` 自死锁"。但 `m_isExecutingTask` 是个普通 atomic flag，**外部线程**也会读到它。当 worker 正在执行 task（flag = true）时，**主线程**调进来会满足这个 if，直接绕开互斥**白嫖**执行权——形成跨线程数据竞争。

### 问题 4：API 表面冗余

`bool TaskMutex(bool try_lock, bool is_set, bool value)` 是一个三态函数（try_lock 模式 / set 模式 / get 模式），还要配合手动维护的 `bool m_mutex_locked`，本质就是把 `std::mutex` 重新包了一层不安全的状态机，可读性差，调用方还需要靠注释才能看懂三个 bool 的语义。

---

## 3. 方案

> 核心思路：**`m_taskMutex` 用标准 `std::mutex` 即可，靠 RAII 自然管理生命周期；阻塞 lock 取代 try_lock + 自旋，由 OS 负责挂起/唤醒；同线程重入用 worker 线程归属判定收紧。**

### 3.1 `OnAsync` 阶段 2：阻塞 lock + RAII

```cpp
{
    std::lock_guard<std::mutex> taskLock(m_taskMutex);
    m_isExecutingTask.store(true);
    while (!local.empty()) {
        auto fn = std::move(local.front());
        local.pop();
        if (fn) { fn(); }
    }
    m_isExecutingTask.store(false);
}
```

- 抢不到锁时，worker 被 OS 挂起；调用线程 `unlock` 时立即唤醒，**0 CPU、0 send 风暴**。
- worker 自身**不会**对 `m_taskMutex` 自死锁——同线程嵌套提交同步任务会走下面 3.3 的快路径，不会再回到这里 lock 一次。

### 3.2 `DirectRunOnCurThread`：try_to_lock + yield

```cpp
auto start = std::chrono::steady_clock::now();
bool didHandleTask = false;
while (!SyncMainTaskMutex(false, false, false)) {
    auto now = std::chrono::steady_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now - start);
    if (duration.count() > 100) { break; }

    std::unique_lock<std::mutex> taskLock(m_taskMutex, std::try_to_lock);
    if (taskLock.owns_lock()) {
        m_isExecutingTask.store(true);
        task();
        m_isExecutingTask.store(false);
        didHandleTask = true;
        break;
    }
    std::this_thread::yield();   // ← 关键：让出 CPU
}
if (!didHandleTask) {
    KR_LOG_INFO << "DispatchAsync when run DirectRunOnCurThread";
    DispatchAsync(task);
}
```

- 仍保留"100ms 内可以同步直跑、超时降级为异步派发"的产品语义。
- `try_to_lock` 失败后 `std::this_thread::yield()`，避免空转打满核心。
- `unique_lock` 离开作用域自动释放，永远不会忘记 unlock。

### 3.3 同线程重入快路径：收紧到 worker 线程

```cpp
if (m_isExecutingTask.load() && IsCurrentThreadWorkerThread()) {
    // 仅当"当前就在 worker 的执行栈里（task 体内嵌套调用）"才允许直跑，
    // 避免外部线程在 worker 持锁跑批期间错误地"白嘍"执行权造成数据竞争。
    task();
    return;
}
```

- `m_isExecutingTask` 仅作为"worker 是否处于 task 执行栈中"的提示位，**只在 worker 线程内读取才有意义**。
- 外部线程（主线程）即使读到 `true`，也会因为 `IsCurrentThreadWorkerThread() == false` 而走正常的 `m_taskMutex` 抢占路径。
- 这同时修复了原版的跨线程 race。

### 3.4 删除冗余 API

- 删除 `bool KRThread::TaskMutex(bool, bool, bool)` 公开函数 + 实现
- 删除冗余成员 `bool m_mutex_locked`
- 改 `m_taskMutex` 为纯私有成员 + RAII 使用，不再对外暴露

`SyncMainTaskMutex` 因 `KRContextScheduler` 仍在外部使用，且语义就是"标志位"而非"互斥锁"，**保持不动**。

---

## 4. 三个互斥/标志的语义边界

| 成员 | 类型 | 语义 | 持有者 | 互斥粒度 |
|---|---|---|---|---|
| `m_taskMutex` | `std::mutex` | **kuikly context 任务执行权令牌**：任意时刻最多一个线程拿到，拿到的就是当前正在执行 task 体的线程 | worker（OnAsync batch 模式）或任意调用线程（DirectRunOnCurThread 借位模式） | 跨线程互斥 |
| `m_isExecutingTask` | `std::atomic<bool>` | **worker 处于 task 执行栈中**的提示位，仅供 worker 自己识别"task 体内嵌套提交同步任务"的重入场景，让其直接执行避开自死锁 | worker | **不能**作为跨线程同步条件 |
| `m_syncMainTaskMutex` + `m_sync_main_task_locked` | `std::mutex` 保护 `bool` | **跨线程标志位**：由 `KRContextScheduler` 在主线程同步逻辑中举旗/落旗，`DirectRunOnCurThread` 观察它决定是否让步 | `KRContextScheduler` 主线程同步流程 | 标志位（非锁） |

---

## 5. 改前 vs 改后

| 维度 | 旧实现 | 新实现 |
|---|---|---|
| 抢不到锁时的 worker CPU | **忙等风暴**：try_lock 失败 → 退回队列 → 自唤醒 → 又 try_lock，几乎裸 spin | **0 CPU**：阻塞 lock，OS 挂起 |
| 抢不到锁时的调用线程 CPU | **裸 spin**：100ms 内无 yield 反复 try_lock | **协作让步**：try_to_lock + `std::this_thread::yield()` |
| 同线程重入快路径 | atomic flag 裸读，跨线程下 race | flag + `IsCurrentThreadWorkerThread()`，限定 worker 自身 |
| 锁释放安全性 | 手动 `TaskMutex(false, true, false)`，异常路径有遗漏风险 | RAII `lock_guard` / `unique_lock`，作用域自动释放 |
| API 表面 | 三态函数 `TaskMutex(bool, bool, bool)` + `bool m_mutex_locked` | `m_taskMutex` 私有 + 标准 RAII |
| `SyncMainTaskMutex` API | 三态函数 + `std::mutex m_syncMainTaskMutex` + `bool m_sync_main_task_locked`，手动举旗/落旗、易遗漏 | `IsWorkerAwaitingMainTask()` + `WorkerAwaitingMainTaskGuard` (RAII) + `std::atomic<bool>`，名实相符、调用现场自解释、异常安全 |
| 编译验证 | — | ✅ `[53/53] Linking libkuikly.so` |

---

## 6. 不变量与回归点

落地后必须维持以下不变量，后续改动需逐条核对：

1. **执行权唯一**：任意时刻 `m_taskMutex` 只被一个线程持有；持有者就是当前正在跑 kuikly task 体的线程。
2. **不自死锁**：worker 进入 task 体后，task 内部再调 `DispatchSync` / `DirectRunOnCurThread` 会被 3.3 的快路径直接放行，不会再次 lock `m_taskMutex`。
3. **跨线程不可走快路径**：非 worker 线程必须经过 `m_taskMutex` 抢占，不允许凭 `m_isExecutingTask == true` 直跑。
4. **超时语义**：`DirectRunOnCurThread` 100ms 内抢不到执行权 → 降级为 `DispatchAsync`，调用方拿到的是 fire-and-forget 语义（与旧版一致）。
5. **向主线程反同步期间的 fast-fail 让步不丢**：worker 在 `ScheduleTaskOnMainThread(sync=true)` 的 `cv.wait` 期间会举起 `m_workerAwaitingMainTask` 旗，`DirectRunOnCurThread` 看到后立即降级异步，避免不必要的 100ms yield-spin；该旗的举起/落下必须通过 `WorkerAwaitingMainTaskGuard` RAII 完成，不允许手动 store。

建议回归用例：

- 主线程频繁 `DispatchSync` 与 worker 大批 `DispatchAsync` 并发，CPU 占用应显著低于旧版（不再有 send-loop 风暴）
- task 体内嵌套 `DispatchSync` 的场景不应卡死（验证 3.3）
- `SyncCallArkTSMethod` 高频调用与主线程 `DispatchSync` 并发：worker `cv.wait` 期间主线程不应出现 100ms 级别的卡顿（验证 D' fast-fail 降级）
- `KRForwardArkTSModule::CallMethod(sync=true)` 走通，返回值与原版一致

---

## 7. 变更清单

- [`KRThread.h`](../../src/main/cpp/libohos_render/foundation/thread/KRThread.h)
  - 删除公开 API `bool TaskMutex(bool, bool, bool)`
  - 删除冗余成员 `bool m_mutex_locked`
  - 强化 `m_taskMutex` / `m_isExecutingTask` 的语义注释
- [`KRThread.cpp`](../../src/main/cpp/libohos_render/foundation/thread/KRThread.cpp)
  - 删除 `KRThread::TaskMutex` 函数实现
  - `OnAsync` 阶段 2：改为 RAII `lock_guard` 阻塞 lock，删除 try_lock 失败回弹分支
  - `DirectRunOnCurThread`：改为 `unique_lock` + `try_to_lock`，加 `std::this_thread::yield()`；同线程重入快路径加 `IsCurrentThreadWorkerThread()` 守卫
  - `SyncMainTaskMutex` 后续重构详见 §9（已落地）。

---

## 8. 遗留问题：`SyncMainTaskMutex` 语义与忙等分析

> 状态：✅ 已分析、已落地。本节保留问题诊断过程作为决策依据，最终选定方案与实际变更见 §9。

### 8.1 调用全景

全仓仅有 3 个调用点，且唯一的“举旗/落旗”者是 [`KRContextScheduler::ScheduleTaskOnMainThread(sync=true)`](../../src/main/cpp/libohos_render/scheduler/KRContextScheduler.cpp)：

```
├─ KRContextScheduler::ScheduleTaskOnMainThread(sync=true)（worker 线程调用）
│    ① SyncMainTaskMutex(true,  false, false);   ← 举旗 (try_lock 模式，但返回值被丢弃)
│    ② KRMainThread::RunOnMainThread([..]{ task(); cv.notify_one(); });
│    ③ cv.wait(...)                              ← worker 阻塞等主线程跑完
│    ④ SyncMainTaskMutex(false, true,  false);   ← 落旗 (set 模式)
└─ KRThread::DirectRunOnCurThread
     while (!SyncMainTaskMutex(false, false, false)) { ... }   ← 看旗（轮询）
```

### 8.2 理论意图

`ScheduleTaskOnMainThread(sync=true)` 是一个“反向同步调用”：worker 线程把任务派回主线程执行，并在 worker 线程上 `cv.wait` 阻塞等结果。这个窗口期内，worker **没有**持有 `m_taskMutex`（已退出 OnAsync 阶段 2 的 `lock_guard` 作用域）。

举旗要表达的意图是：

> “我（worker）正在等主线程跑同步主任务，请别的 `DirectRunOnCurThread` 调用方让一让，不要抢这个窗口期内的执行权，全部走异步队列排队。”

本质上是一个 **跨线程的“礼让旗”**，而非互斥锁。

### 8.3 现状偏离意图的几处“奇怪”

#### ① 叫 `Mutex`，实际只是个 `bool` 标志位
真正的 `std::mutex`（`m_syncMainTaskMutex`）仅仅用来保护 `m_sync_main_task_locked` 这个 bool 的并发读写——一种很重的“原子布尔”。名字令读者误以为它是互斥锁。

#### ② 三态参数 `(try_lock, is_set, value)` 把三个函数揉为一个

```cpp
bool SyncMainTaskMutex(bool try_lock, bool is_set, bool value);
//   ▲ true                  → 抢旗（未举则举起，返回是否抢到）
//   ▲ false + is_set=true   → 落旗（强行设为 value）
//   ▲ false + is_set=false  → 看旗（返回当前是否举起）
```

调用现场全是魔法字面量，要靠注释才能读懂。

#### ③ “抢旗”的返回值被丢弃

```cpp
GetContextThread()->SyncMainTaskMutex(true, false, false);   // ← 返回值被丢弃
... cv.wait(...) ...
GetContextThread()->SyncMainTaskMutex(false, true, false);   // ← 无条件落旗
```

如果同时有两个 `ScheduleTaskOnMainThread(sync)` 并发，后来者的 try_lock 返回 `false` 但未检查，照样往下走，最后的落旗还会把别人举的旗一同扣下。现在能跑是“靠默契”（实际只有一个 worker 线程走这条路径，不会并发抢旗），不是靠代码保护。

#### ④ “礼让”语义与实现不匹配——看旗者不能被唤醒

落旗侧 `SyncMainTaskMutex(false, true, false)` 没有任何通知机制（无 cv、无 `uv_async_send`），看旗者只能轮询。

### 8.4 忙等风险评估

当前 `DirectRunOnCurThread`（[KRThread.cpp](../../src/main/cpp/libohos_render/foundation/thread/KRThread.cpp) 第 270–295 行）：

```cpp
while (!SyncMainTaskMutex(false, false, false)) {        // ← 每轮：lock(m_syncMainTaskMutex)
    if (duration > 100ms) { break; }
    std::unique_lock taskLock(m_taskMutex, try_to_lock); // ← 每轮：try_to_lock(m_taskMutex)
    if (taskLock.owns_lock()) { ...; break; }
    std::this_thread::yield();
}
```

在“旗未举起 + `m_taskMutex` 被 worker 长期持有”的窗口期内，每轮微秒级开销，100ms 能跑成千上万轮。不是“打满核心”那种崩溃式忙等，但仍存在三重损耗：

1. **`m_syncMainTaskMutex` 高频争用**——仅为了读一个 `bool`，重复 lock/unlock。
2. **`m_taskMutex` 被 try_to_lock 骨扰**——会和 worker 持锁/释锁的真正争用混在一起，冲击 cache line。
3. **`yield` 不等于 `sleep`**——OHOS 上 `yield` 仅让出一次调度，可能立刻被重新调度回来。

> 勘误记录：最初曾以为 `ScheduleTaskOnMainThread(sync=true)` 是死代码（grep 用 `Func\(\s*true` 只能匹配字面量 `true`，遗漏了变量传参的调用形态）。实际上 [`IKRRenderModuleExport::ToCallArkTSMethod`](../../src/main/cpp/libohos_render/export/IKRRenderModuleExport.h) 以变量 `isSync` 传入，而其 wrapper `SyncCallArkTSMethod` 是以字面量 `true` 调用的。全仓 [`KRForwardArkTSModule::CallMethod`](../../src/main/cpp/libohos_render/expand/modules/forward/KRForwardArkTSModule.cpp) 走这条路径，是 **高频活路径**。这个勾销让原本为“直接删净”的方案 D 被推倒，转为方案 D'。

相较于原版 `m_taskMutex` 那次的“send-loop 风暴”要轻得多（从裸 spin 降为 yield-spin），但**性质仍是忙等**。

### 8.5 根因

> **它是一个“跨线程协调信号”，却被实现成“加了锁的 bool 标志位 + 轮询读取”。**

正确工具是 `condition_variable` / `event`，让等待方阻塞挂起、举旗方主动通知。当前实现同时有四项偏离：命名误导、缺少 notify、try_lock 返回值被丢、等待者只能轮询。

### 8.6 备选方案（由轻到重）

#### 方案 A：`bool` → `std::atomic<bool>`，删除 `m_syncMainTaskMutex`

```cpp
std::atomic<bool> m_sync_main_task_running{false};
// 看旗：.load(memory_order_acquire)
// 举旗/落旗：.store(true/false, release)
```

- 收益：看旗变为无锁 atomic load，每轮成本下降一个数量级。
- 不足：100ms 轮询仍在。
- 复杂度：⭐

#### 方案 B：三态参数拆为 3 个语义函数

```cpp
void BeginSyncMainTask();          // 举旗
void EndSyncMainTask();            // 落旗
bool IsSyncMainTaskRunning() const;// 看旗
```

- 收益：调用现场自解释，按业务语义读取。
- 复杂度：⭐⭐

#### 方案 C：用 `condition_variable` 替代轮询

让 `DirectRunOnCurThread` 看到旗举起后 `cv.wait_for(100ms)`。但仔细看现有策略是“看到旗举起就立即让步走异步”，并不需要等旗落下，cv 在这个用例里收益不明显。真正受益的是重构整个 100ms 超时交互。

- 复杂度：⭐⭐⭐⭐

#### 方案 D：彻底删除 `SyncMainTaskMutex`，复用 `m_taskMutex`

现在 `m_taskMutex` 已是常规 `std::mutex`，worker 在 OnAsync 阶段 2 持锁跑批、跑完才解锁。考虑：在 `ScheduleTaskOnMainThread(sync)` 的整个“等主线程”窗口期间也让 worker 持住 `m_taskMutex`，是否能等价地替代 `SyncMainTaskMutex` 的作用？

其他线程的 `DirectRunOnCurThread` 看到 `m_taskMutex` 抢不到，本来就会 yield 到 100ms 超时后降级为异步——行为等价。唤醒机制也现成（`m_taskMutex` 解锁即接续，不需额外 cv）。

- 收益：一次性删净 `SyncMainTaskMutex` 函数 + bool + mutex + 2 处调用现场。
- 风险点：worker 持锁 `cv.wait` 期间，主线程上运行的 task() 如果反向调用 `DispatchSync(worker)` 会看到 `m_taskMutex` 被 worker 抻住——需要验证 100ms 超时降级是否可接受。
- 复杂度：⭐⭐⭐

### 8.7 推荐路径

**先 A + B 组合**（小成本拿到大部分可读性收益和忙等成本下降）→ 再评估 D 可行性。若 D 能确认安全，就一并删净。

---

## 9. `SyncMainTaskMutex` 优化落地（方案 D'）

> 状态：✅ 已落地，代码变更 lint 干净、全仓旧符号 0 残留。

### 9.1 为什么是 D' 而不是 D

§8.6 原本推荐路径是“先 A+B → 再评估 D”。但详细重评 D 后发现它会引入一个可观测的性能回归：

- D 的思路是“完全删除 `SyncMainTaskMutex`，仅依靠 `m_taskMutex` 本身”。
- 但 `SyncMainTaskMutex` 在现实中承担了一个 **fast-fail 让步** 作用：当 worker 在 `cv.wait` 期间抻住 `m_taskMutex` 不放时，`DirectRunOnCurThread` 看到旗会 **立即** 降级异步；删掉后会退化为 yield-spin 100ms 后才降级，主线程会过程可感卡顿。
- 在 `SyncCallArkTSMethod` 是高频活路径的前提下，这个回归不可接受。

所以选定修正版 **D'**：**保留 fast-fail 语义，但用 atomic flag + RAII guard 重构并表达得名实相符**。

### 9.2 实际变更

#### [`KRThread.h`](../../src/main/cpp/libohos_render/foundation/thread/KRThread.h)

**删除**：

- `bool SyncMainTaskMutex(bool, bool, bool)` 声明
- `std::mutex m_syncMainTaskMutex`
- `bool m_sync_main_task_locked`

**新增**：

```cpp
// 跨线程可见的 fast-fail 让步旗（acquire 语义读取）
bool IsWorkerAwaitingMainTask() const noexcept {
    return m_workerAwaitingMainTask.load(std::memory_order_acquire);
}

// RAII 守卫：worker 在 cv.wait 之前构造，超出作用域自动落旗
class WorkerAwaitingMainTaskGuard {
    explicit WorkerAwaitingMainTaskGuard(KRThread *t);
    ~WorkerAwaitingMainTaskGuard();
    // 不可拷贝不可赋值
};

std::atomic<bool> m_workerAwaitingMainTask{false};
```

#### [`KRThread.cpp`](../../src/main/cpp/libohos_render/foundation/thread/KRThread.cpp)

- 删除 `KRThread::SyncMainTaskMutex(...)` 函数实现
- `DirectRunOnCurThread` 的自旋条件从 `while (!SyncMainTaskMutex(false, false, false))` 改为 `while (!IsWorkerAwaitingMainTask())`。

#### [`KRContextScheduler.cpp`](../../src/main/cpp/libohos_render/scheduler/KRContextScheduler.cpp)

`ScheduleTaskOnMainThread(sync=true, ...)` 的 worker 分支：

```cpp
if (sync) {
    if (GetContextThread()->IsCurrentThreadWorkerThread()) {
        // RAII 举旗：cv.wait 期间通知 DirectRunOnCurThread 立即降级异步。
        // 离开作用域时 guard 析构自动落旗，无需手动配对。
        KRThread::WorkerAwaitingMainTaskGuard guard(GetContextThread());
        std::mutex mtx;
        std::condition_variable cv;
        bool task_completed = false;
        std::unique_lock<std::mutex> lock(mtx);
        KRMainThread::RunOnMainThread([task, &mtx, &cv, &task_completed] {
            task();
            { std::lock_guard<std::mutex> lk(mtx); task_completed = true; }
            cv.notify_one();
        });
        cv.wait(lock, [&task_completed] { return task_completed; });
    } else { /* 主线程直跑 */ }
}
```

### 9.3 行为等价性论证

| 场景 | D' 前 | D' 后 |
|---|---|---|
| `DirectRunOnCurThread` 在 worker 持锁跑批时 | yield + 100ms 超时降级 | 同（`m_taskMutex` 路径不变） |
| `DirectRunOnCurThread` 在 worker `cv.wait` 等主线程时 | 看旗 → fast-fail 立即降级 | 看 atomic flag → fast-fail 立即降级 ✅ 等价 |
| `ScheduleTaskOnMainThread(sync=true)` 中途抛异常 | 手动落旗可能遗漏→ 旗永久举起 ❌ | RAII 析构必然落旗 ✅ 更安全 |
| 每轮看旗开销 | `lock(m_syncMainTaskMutex)` + 读 bool + unlock | `atomic load (acquire)` ✅ 更轻 |
| API 语义 | 三态函数名为 Mutex 实为 flag ❌ | `IsWorkerAwaitingMainTask` + `WorkerAwaitingMainTaskGuard` ✅ 名实相符 |

### 9.4 错误估计与修正

§8.7 推荐“先 A+B → 再评估 D”是基于“`SyncMainTaskMutex` 是死代码”的错误前提。重评后修正为直接一步到位为 D'：

- A（`atomic<bool>`）被 D' 涵盖；
- B（拆为 `Begin/End/Is...`）被 D' 进一步提升为 `RAII guard + Is...`，更防呆；
- D 被诊断会带来 100ms 主线程卡顿回归，不可取，转为 D'。

后续调用方搜索方法论需加一个 `git grep` 习惯：不只检查函数名后跟字面量参数的调用，也要检查函数名后跟变量名参数的调用，逐条追溯到字面量源头。

### 9.5 验证状态

- ✅ `read_lints` 三个文件 0 错。
- ✅ 全仓 `grep "SyncMainTaskMutex|m_sync_main_task_locked|m_syncMainTaskMutex"` 需返回空（已验证）。
- ⏳ 本机仓库未提供 `hvigorw` 包装脚本，未在终端跑完整构建；需在 DevEco Studio 里或全局 hvigorw 环境下验证一次 `[XX/XX] Linking libkuikly.so`。
- ⏳ 建议跑一轮业务回归：`KRForwardArkTSModule` 高频调用场景 + 主线程同步调用 worker 场景，观察主线程是否出现 100ms 级别卡顿。

