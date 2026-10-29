## Context

### 现状

鸿蒙上的 Kotlin 默认跑在全局唯一的 context 线程（`KRThread`，自建 libuv loop）上。同步事件（`Event.register(isSync = true)`，例如同步 `touchMove`、`syncScroll` 的 `scroll`）在主线程上执行：主线程对 context 任务锁 `m_taskMutex` 做 `try_lock`。拿不到就 `yield` 空转，最长 100ms，然后改成异步。拿到后在主线程调用 Kotlin，再同步刷新 UI。context 线程在 `OnAsync` 里把当前队列整批取出，拿着锁跑完这一批才释放。

vsync 帧任务（Compose 鸿蒙端经 `VsyncModule` → `KRVsyncModule` → `OH_NativeVSync` 驱动 `renderFrame`）在 vsync 线程回调到达时立即投递到 context 队列；而 ArkUI 也在 vsync 时刻分发输入。两者同相位，导致：

1. 帧任务先拿到锁时，主线程上的同步事件要等完整个帧任务。等待时间和帧任务差不多长。
2. context 线程一批里有多个小任务时，主线程要等完整批。

### 实验结论（真机 Mate 60 Pro / API 24 / 120Hz，release 版 Kotlin，每组 3 次）

实验分支：`sync-event-perf`；工具：`tools/sync_event_perf/`；原始数据：`logs/bench2/`。"响应延迟" = 输入事件时间戳 → 本次同步事件 UI 提交完成（含约 8.5ms 系统输入管线底数）。

| touchMove 响应延迟 p50 / p90（ms） | 4ms 单任务 | 8ms 单任务 | 8ms 拆 8 个小任务 |
|---|---|---|---|
| 双线程基线 | 13.3 / 14.3 | 11.2 / 18.2 | 19.2 / 23.9 |
| + 当前任务做完后释放锁 | 10.9 / 14.3 | 10.8 / 17.8 | 11.4 / 13.1 |
| + 帧任务等本帧输入 | 10.3 / 11.3 | 11.0 / 12.4 | 13.3 / 17.5 |
| + 两者都做 | 10.4 / 11.8 | 11.0 / 12.5 | 11.2 / 12.7 |
| 全局单线程 | 9.6 / 10.1 | 10.6 / 11.8 | 16.7 / 25.5 |
| 全局单线程，每轮最多 2ms | 9.6 / 10.1 | 10.6 / 11.9 | 10.1 / 11.1 |

关键事实：

- 无负载时各配置无差异，线程切换本身为微秒级。
- 帧任务等本帧输入，把 8ms 单任务下的等锁 p90 从 8ms 降到 1.5ms。当前任务做完后释放锁只对一批里有多个任务时有效。两者互补。
- 单线程（主线程执行）每事件的 Kotlin+layout+UI 处理约 1.07ms，双线程即使不等锁也要 1.6~2.0ms（推测为跨核缓存迁移，未单独验证），是双线程调度优化无法消除的结构性差距。
- 单线程主线程上 Kuikly 耗时为双线程的 4~6 倍，但在测试负载内（约 50% 主线程占用）未掉帧；单线程不加时间预算时多任务负载下严重恶化（响应 p90 25.5ms、输入被合并、scroll 掉帧 4 倍）。
- 连续同步事件期间每 8.3ms 就有一个同步事件。主线程和 context 线程本来就在抢同一把锁，这段时间里两条线程并行的好处大部分已经没有了。

### 适用 DSL

渲染层调度改造，对两种 DSL 同时生效：自研 DSL 的同步 `touchMove` / `syncScroll`，以及 Compose DSL 的 vsync 帧驱动（`renderFrame`）和它的同步事件。Kotlin 侧只把鸿蒙 Compose 的 touchMove 改成同步注册。

## Goals / Non-Goals

**Goals:**

- 连续同步事件期间，输入不再等 vsync 帧任务。等待从「一整批任务」降到「当前这一个任务」。
- 连续同步事件期间，context 任务改在主线程执行，每一轮最多 2ms。以前对比过「主线程按 2ms 执行」和「context 线程做完当前任务再释放锁」。同步 touchMove 始终注册之后，不再保留只走 context 线程的做法。
- 修复单线程模式死锁。
- 提供 A/B 所需的线上指标。

**Non-Goals:**

- 不改线程模型的全局设置方式、不引入按页面线程模型。
- 不处理 Android / iOS（见"跨端适用性"）。
- 不切分单个长任务。不提供「连续事件等锁超时后整段改成异步」。单次同步调用仍沿用原有的 100ms，超时后这一次改成异步执行。
- 不改 Compose 同步 touchMove 里谁消费、以及 `preventTouch` 的时序。只把鸿蒙改成和其他平台一样注册同步 touchMove。

## Decisions

### D0：用开始和结束标志判断连续同步事件，不用时间窗口

一次连续同步事件从 native 的开始标志到结束标志，不看「最近一次事件过去了多久」。开始和结束取自 ArkUI 状态，与 Kotlin 有没有注册 `touchDown` / `scrollEnd` 无关。只有该 view 注册了对应的同步连续事件时才上报。

| 同步连续事件 | 开始 | 结束 | 上报位置 |
|---|---|---|---|
| `touchMove` | touch DOWN（首个 pointer） | 最后一个 pointer UP / CANCEL | `KRView` touch 处理 |
| `scroll` | ArkUI 滚动开始（拖动、惯性、带动画的 contentOffset） | 滚动停止 | `KRScrollerView` scroll start/stop |
| `pan` / `pinch` / `longPress` | 暂不支持 | 暂不支持 | 没有上报。`SessionType::kGesture` 先留着，支持同步后再接 |

`keyboardHeightChange` 不算连续同步事件。鸿蒙 `KRWindowInfo.ets` 通过 `window.on('keyboardHeightChange')` 在键盘高度变化时推送一次（`durationMs` 写死为 300）。它不是逐帧回调，也没有结束标志，无法从 native 状态判断从哪一帧开始、到哪一帧结束。这个结论来自读代码，还没用真机确认。

- 多段同时进行（例如列表里拖动的同时又在滚动）用计数。计数归零才离开这段连续事件。
- 以「view + 事件类型」为键。重复开始只记一次。
- 结束标志丢了就靠生命周期：view 被移除或销毁时清掉它登记的全部事件。不用超时。
- `KRRenderCore` 在每次同步连续事件回调前后通知 `KRInputFirstGate`「本帧输入开始 / 处理完成」。处理完成是指 Kotlin 回调和同步 UI 提交都做完。

**备选**：基于最近一次事件的时间窗口判定手势（原型方案，50ms）——太短会在事件间隙误判手势结束，太长会在抬手后让后续帧等待不存在的输入，且对刷新率敏感；否决。

### D1：帧任务等本帧输入，最多合并下一次 vsync

`KRVsyncModule::OnVsync` 通过代数校验后，把「调用模块回调，再投递清除上一帧未完成标记的任务」交给 `KRInputFirstGate`：

- 没有连续同步事件：马上投递。
- 事件进行中，并且本帧输入已经处理完（最近一次「输入处理完成」不早于这次 vsync 时间戳）：马上投递。
- 事件进行中，并且已经处于「这段时间没有新输入」：马上投递。
- 其他情况先存起来，本帧输入处理完再投递。
- 下一次 vsync 到来时还存着（这一帧完全没有输入）：用新 vsync 的帧信息投递存着的那一次，然后进入「没有新输入」。之后的 vsync 直接投递，直到下一次输入。原先「上一帧还没执行完就丢掉新帧」在这里改成合并后投递。
- 存着的任务真正投递时必须再检查 `KRVsyncModule` 的 running 和 generation。反注册或销毁之后丢掉。
- vsync 时间戳和 `steady_clock` 都是 CLOCK_MONOTONIC（真机实测偏差 0.2ms）。

**为什么要有「没有新输入」**：如果每一帧都是先存着、等下一次 vsync 再合并，手指停住时帧任务会变成一半频率。和手指无关、还在跑的动画（已经开始的 Compose 动画、加载动画）也会掉到半帧率。加上这个状态后，一次停顿最多合并一帧，这类动画最多在停顿开始时少一帧。

**与标准 CMP 的对应**：Android 上输入与帧同在 UI 线程，`Choreographer` 每帧固定先处理批处理的输入（`CALLBACK_INPUT`）再执行帧回调（Compose 的 `withFrameNanos` / 重组），迟到的输入留到下一帧——帧从不等待输入，"合并"发生在输入侧；iOS / 桌面 CMP 同为单线程串行。Kuikly 鸿蒙帧任务在另一线程，只能镜像为"帧任务等本帧输入、迟到则合并到下一帧"，边界同样以帧而非时间表达；ArkUI 与 Android 一样把触摸对齐到 vsync 在帧开头分发，"本帧输入"定义明确，实测帧任务通常仅需等待 1~2ms。

**备选**：加一个 4ms 超时（原型试过）。太短时，帧任务会赶在迟到的输入之前投递，两边又抢锁。太长会占掉 context 线程这一帧的时间，而且同一个数在 60Hz 和 120Hz 下差一倍。否决。vsync 之后固定晚半帧再投递：所有帧任务都晚半帧。否决。在 Compose 帧任务中途释放锁：同步事件会看到一帧只做了一半。否决。

### D2：当前任务做完后释放锁，剩余任务放回队首

context 线程跑一批任务时，每个任务结束后看等待计数。主线程在 `TryRunOnCurThread` 第一次 `try_lock` 失败后加一。有人等、这批还没完时：先把剩余任务按原顺序放回共享队列 `m_pending` 队首，再释放 context 任务锁，等到主线程真正拿到锁（计数归零）后回到 loop 重新调度。`std::mutex` 不保证谁先拿到，必须等计数归零再竞争。

放回队首也是下面「主线程执行 context 任务」的前提。主线程从同一个 `m_pending` 取任务。剩余任务若留在 context 线程的局部队列里，主线程会先执行更晚入队的任务，顺序就乱了。

**对现有行为的影响**：一批任务做到哪里，本来就由时间决定。`setTimeout` 这类回调本来就是单独一次执行。框架内部 `TaskManager.nextTick` 会合成一个任务，和产生它的任务之间本来就会再投一次 UI 刷新。canvas 一次重绘（reset、绘制、flush）在同一个任务里做完。所以在任务之间释放锁，不改变这些约定。

### D3：连续同步事件期间，由主线程执行 context 任务

以前对比过另一种做法：context 线程继续执行任务，主线程只执行同步事件本身，并在当前任务做完后拿锁（D2）。同步 touchMove 始终注册之后，不再保留这种做法。连续同步事件期间一律改由主线程执行 context 任务。

行为：

1. **进入**：连续同步事件从无到有时暂停 context 线程（`KRThread::SetParked(true)`）。正在执行的任务做完，然后把剩余任务按原顺序放回 `m_pending` 队首（D2），之后不再执行任务。
2. **执行**：所有 context 任务（包括这段时间新到的帧任务、定时器、回调）仍进入同一个 `m_pending`。context 线程被唤醒时发现已暂停，就通知主线程再执行一轮。这一轮经 `RunOnMainThreadForNextLoop` 投递，持有和同步事件同一把 context 任务锁，并套上 `MainThreadContextGuard`，按先入先出逐个执行。
3. **每轮最多 2ms**：2ms 写死，没有配置。到时间就回到 ArkUI 事件循环，让 ArkUI 分发输入和渲染，并投递下一轮。队列空就停。有新任务时由 context 线程再通知。主线程执行期间 context 线程已暂停，不按帧设配额，也不预估单个任务要跑多久。到 2ms 只能回到事件循环，下一轮再继续。这和实验里验证过的 `single_budget` 一致。
4. **退出**：连续同步事件从有到无时恢复 context 线程（`SetParked(false)` 并唤醒）。它从同一个 `m_pending` 继续执行。已经投出去、还没执行的那一轮发现暂停已结束，就直接返回。

**进出不换队列**：全程只有一个先入先出队列和一把锁。进入时最多等 context 线程做完当前这一个任务（和 D2 一样）。退出时不用再加一道屏障，顺序不会乱。

**拿着锁时不能直接执行 UI**：主线程执行的任务里会再投 UI 刷新（`PerformSyncMainQueueTasksBlockIfNeed(false)` → `PerformOnMainQueueWithTask(false)` → `KRMainThread::RunOnMainThread`）。在主线程上会直接执行 UI。UI 可能同步触发 ArkUI 事件，再次进入同步事件。这把锁不能重入。主线程拿着锁再次进入 `TryRunOnCurThread` 会直接返回 false，改成异步执行。所以这一轮里产生的主线程 UI 任务必须等锁释放后再执行。这段时间内改走 `RunOnMainThreadForNextLoop`。

**更正记录**：第一版是 context 线程照常执行，主线程按每帧配额从队列取任务。这仍然是两条线程抢同一把锁，比的只是「任务能不能被主线程取走」。真机复测里，主线程几乎每次都因为队列空、或 context 线程拿着锁而停下来（帧任务一投递就被 context 线程取走）。开和关没有差别。现改为事件期间暂停 context 线程。原来的复测结论作废。

**备选**：手势期间把 context 线程的任务转发到主线程另一个队列。进入要等它手头那批做完，退出要保证先入先出。否决。全程改成单线程：没有手势时也不能和主线程并行。否决。已有的单线程模式仍然保留。

### D4：单线程模式 `ScheduleTask` 异步语义

`KRContextSchedulerSingleThreaded::ScheduleTask(delay <= 0)` 改为 `KRMainThread::RunOnMainThreadForNextLoop`；`delay > 0` 保持 timer。修复 `AddTaskToMainQueueWithTask` 持 `m_mutex_` 时内联重入导致的死锁。

### D5：这些行为都没有单独的关闭方式

| 能力 | 行为 |
|---|---|
| 连续同步事件的开始和结束（D0），以及帧任务等本帧输入（D1） | 始终如此 |
| 当前任务做完后释放锁，剩余任务放回队首（D2） | 始终如此 |
| 连续同步事件期间由主线程执行 context 任务（D3） | 始终如此。同步 touchMove 也始终注册。若这些任务仍留在 context 线程，同步 touchMove 会等完整个帧任务。每轮 2ms 写死 |
| 鸿蒙 Compose 同步 touchMove | `SuperTouchManager` 固定 `setTouchMove(true)`，和其他平台一样 |
| 单线程死锁修复（D4） | 始终如此 |

### D6：指标

按一次连续同步事件汇总，经日志或性能 adapter 上报。响应延迟、等锁、同步事件回调耗时输出 p50 / p90 / max，和实验里的 p50/p90 同一口径。内容：同步事件响应延迟（输入时间戳到 UI 提交，只含 touch）、等锁时长、同步事件回调本身的耗时（`execMs`，不含主线程执行 context 任务的时间）、帧任务怎么投递的（等输入处理完 / 合并下一次 vsync / 直接投递）、进入「没有新输入」的次数、主线程一轮执行的停止原因（2ms 用完 / 队列空或拿不到锁）。实验分支里的全量统计（`KRSyncEventPerf`、测试页、脚本）不随正式代码合入。

### NativeBridge 交互

无新增 Kotlin ↔ native 方法，也无宿主可关的 C 开关。`VsyncModule` 调用约定与帧间隔上报类型不变。

## 跨端适用性

| 能力 | HarmonyOS | Android | iOS |
|---|---|---|---|
| 同步事件执行方式 | 主线程拿 context 任务锁后执行 | 投到 HandlerThread，主线程阻塞等待（先入先出，等整个队列） | 投到 context runloop，用信号量等待（先入先出） |
| 先处理输入，再投递帧任务 | 本次改动 | 已满足（`Choreographer` 先处理输入，再跑帧回调） | 还要做（`CADisplayLink` 挂在 context 线程，和输入同一节拍） |
| 当前任务做完就释放锁 | 本次改动 | `Handler.postAtFrontOfQueue` 可以把等待降到一条消息 | 要自己做优先级队列 |
| 事件期间由主线程执行 context 任务 | 本次改动 | 要先有同一把任务锁，改动大 | 同左 |

Android / iOS 作为后续独立 change，在鸿蒙 A/B 结论后决策。

## 文件变更（按模块）

**core-render-ohos**

- `foundation/thread/KRThread.{h,cpp}`：`TryRunOnCurThread(window, task)`；等待计数；`OnAsync` 在当前任务做完后释放锁，并把剩余任务放回队首；暂停和恢复（`SetParked`），以及暂停期间有任务时的通知；主线程持锁取下一个任务的接口。
- `scheduler/KRInputFirstGate.{h,cpp}`（新增）：按 view 和事件类型计数，view 销毁时清掉；从无到有、从有到无时通知主线程执行逻辑；按 `KRVsyncModule` 分别暂存帧任务、合并该模块的下一次 vsync、没有新输入时直接投递；同步连续事件按计数判断是否处理完，嵌套时以最外层为准。
- `scheduler/KRMainThreadTaskPuller.{h,cpp}`（新增）：连续同步事件期间暂停和恢复 context 线程，主线程每一轮最多执行 2ms。没有单独的关闭方式。
- `scheduler/KRContextScheduler.{h,cpp}`：`TryDirectRunOnMainThread`；`TryRunNextContextTaskOnMainThread`；`SetContextThreadParked`；D4 修复。
- `expand/modules/vsync/KRVsyncModule.{h,cpp}`：把一帧的投递交给 `KRInputFirstGate`；暂存时下一次 vsync 改为合并后投递；投递时重新校验 generation。
- `core/KRRenderCore.cpp`：同步连续事件回调前后通知 `KRInputFirstGate`「本帧输入开始 / 处理完成」；记指标。
- `expand/components/view/KRView.cpp`：注册了同步 `touchMove` 时上报触摸开始和结束（第一根手指 DOWN / 最后一根 UP、CANCEL）；移除或销毁时清掉。
- `expand/components/scroller/KRScrollerView.cpp`：注册了同步 `scroll` 时上报滚动开始和停止；移除或销毁时清掉。
- `KRInputFirstGate::SessionType::kGesture` 先留着。pan / pinch / longPress 支持同步后再接，这次没有改 `expand/events/`。
- `scheduler/KRUIScheduler.cpp`：主线程正在执行 context 任务时，产生的 UI 任务等锁释放后再执行。
- `CMakeLists.txt`：新增源文件。

**ohosApp（demo 宿主）**：不提供把这段主线程执行关掉的入口。

**compose**：`SuperTouchManager` 固定注册同步 touchMove（含鸿蒙）。**core / demo**：无改动。

## Risks / Trade-offs

- [帧任务因此晚约 1~2ms；停顿开始时合并一次 vsync，和手指无关的动画在停顿开始时少一帧] → 「没有新输入」之后直接投递，一次停顿最多合并一帧。实验里帧表现没有变差。
- [结束标志丢了，会一直停留在连续同步事件里] → view 移除或销毁时清掉。即便没清掉，「没有新输入」之后帧任务也会直接投递，只少一帧。
- [负载接近一帧时，下一次输入仍会撞上帧任务的尾巴] → 单个任务不能从中间切开。事件期间改由主线程执行，一轮最多 2ms，能减轻这种情况。
- [不经 vsync 的长任务仍会让人等锁] → 只能保证等到当前这一个任务结束。更长的任务要业务自己拆开。
- [context 任务里的阻塞 IO 会在手势期间堵住主线程] → 2ms 只在任务之间生效，切不开一个正在执行的阻塞任务。要在验证里看。有问题的模块应改成异步。
- [事件期间主线程承担全部 context 任务，复杂页面可能掉帧] → 每轮最多 2ms，ArkUI 能在两轮之间分发输入和渲染。2026-10-12 复杂 Compose 列表（200 行、50ms 定时器、拖拽和快速滚动，120Hz）上，同步 touchMove 和主线程执行一起生效时，手势期间上屏仍是 120Hz，没有超过 2 帧的间隔。只开同步 touchMove、context 任务仍留在 context 线程时，滚动等锁 p90 为 10.4ms，延迟 p90 为 21.4ms。两者一起时降到 0.008ms / 11.1ms。
- [进入时要等 context 线程把当前任务做完] → 和「当前任务做完后释放锁」的等待上限一样。
- [主线程执行任务时直接做 UI，会在还拿着锁时再次进入同步事件] → 这段时间内的 UI 任务等锁释放后再执行。主线程拿着锁再次进入 `TryRunOnCurThread` 会直接返回 false，改成异步执行。
- [`KRInputFirstGate` 是全局单例，多个页面共用] → context 线程也是全局的。只有注册了同步连续事件的 view 会上报。没有这类事件的页面不受影响。

## Migration Plan

这次一次合入，没有单独的关闭方式。回滚靠版本回退。

回归：同步 touch / scroll、键盘高度、多指与 touch + scroll 并存、手指停顿时的独立动画、页面销毁时丢掉暂存帧。复杂页的数据见上面的风险。只改同步 touchMove、任务仍留在 context 线程时，滚动等锁会留在约 10ms，所以这两件事一起上。

## Open Questions

- 是否需要识别并排除执行阻塞 IO 的 native 模块任务。这些行为没有开关，这类任务会在连续同步事件期间堵在主线程上。
