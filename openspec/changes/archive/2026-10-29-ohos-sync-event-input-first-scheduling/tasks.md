> 实施说明：代码已在分支 `ohos-sync-event-scheduling` 全部实现。任务已全部勾选。

## 1. 准备

- [x] 1.1 从 `main` 新建实现分支（实验分支 `sync-event-perf` 中的 `KRSyncEventPerf` 全量统计、测试页、`tools/sync_event_perf/` 不合入，仅作参考与回归工具）
- [x] 1.2 在实现分支保留一份轻量回归页与 bench 脚本（不随正式代码合入），用于每阶段与基线对比。页：`demo/.../debug/SyncEventPerfPages.kt`（`SyncTouchMovePerfPage` / `SyncScrollPerfPage` / `SyncComplexPerfPage`）、`SyncComplexComposePerfPage.kt`。脚本：本目录 `bench.sh`

## 2. renderer：单线程模式死锁修复（D4，可独立合入）

- [x] 2.1 `KRContextSchedulerSingleThreaded::ScheduleTask`：delay ≤ 0 改为 `KRMainThread::RunOnMainThreadForNextLoop`，delay > 0 保持定时器
- [x] 2.2 HarmonyOS 真机验证：`KRSetThreadingMode(1)` 下 demo 首屏正常渲染、无 appfreeze；多线程模式回归无变化
  - 2026-10-22 设备 2LQ0223A26007226。`aa start --pi krThreadingMode 1`：`threadingMode set to single` 后 `created threadingMode=single`，路由页首屏出来，进程存活，hilog 无 APP_FREEZE。不带参数冷启动：`created threadingMode=multi`，同一首屏，无 freeze。符号不进 `Kuikly.h`，demo napi 在 initKuikly 前调用。

## 3. renderer：当前任务做完后释放锁，剩余任务放回队首（D2）

- [x] 3.1 `KRThread::TryRunOnCurThread(window, task)`：返回是否同步执行；`DirectRunOnCurThread` 改为基于它实现，保持原 100ms 降级语义
- [x] 3.2 `KRThread`：外部等待者计数（首次 `try_lock` 失败登记，获得锁或放弃时注销，RAII）
- [x] 3.3 `KRThread::OnAsync`：任务之间看有没有人在等锁。有的话，先把剩余任务按原顺序放回 `m_pending` 队首，再释放锁。等计数归零后回到 loop 重新调度
- [x] 3.4 `KRContextScheduler::TryDirectRunOnMainThread`；`KRRenderCore` 同步事件路径改用该接口，降级时投递语义与原实现一致
- [x] 3.5 主线程拿着 context 任务锁时再次进入 `TryRunOnCurThread`：直接返回 false，改成异步执行
- [x] 3.6 HarmonyOS 真机验证：多小任务负载下同步 touchMove 等锁上限降至单个 task；FIFO 顺序校验（序号断言）；canvas / 列表懒加载 / Compose 动画回归
  - 2026-10-27 设备 2LQ0223A26007226，debug Kotlin，uitest。`SyncTouchMovePerfPage` 上每帧 8ms 拆成 8 个任务：两次拖动等锁 p90 均为 0.005ms（单个任务约 1ms），`fifoBreaks=0`。同页带 Canvas。`SyncScrollPerfPage` 懒加载列表同步滚动 `fifoBreaks=0`。`SyncComposePerfPage` 每帧刷新 40 行，会话正常结束。无崩溃。

## 4. renderer：连续同步事件的开始和结束，以及帧任务等本帧输入（D0 + D1）

- [x] 4.1 新增 `scheduler/KRInputFirstGate` 会话管理：以"view + 事件类型"为键的会话开始/结束（幂等）、引用计数、按 view 释放全部会话
- [x] 4.2 `KRView`：注册了同步 `touchMove` 时，首个 pointer DOWN 开始会话、最后一个 pointer UP/CANCEL 结束；view 移除/销毁时释放
- [x] 4.3 `KRScrollerView`：注册了同步 `scroll` 时，ArkUI 滚动开始/停止上报会话（覆盖拖动、惯性、带动画 contentOffset）；移除/销毁时释放
- [x] 4.4 键盘高度事件：确认鸿蒙 `keyboardHeightChange` 是否逐帧触发及动画开始/结束标志；注册了同步时上报会话；手势 `state` start/end 会话接口预留（结论：非逐帧，不纳入会话，见 design.md D0；手势会话接口已预留）
- [x] 4.5 `KRRenderCore`：同步连续事件回调前通知"本帧输入开始"，Kotlin 回调与同步 UI 提交完成后通知"本帧输入处理完成"
- [x] 4.6 `KRInputFirstGate` 帧任务状态机：不在会话 / 本帧输入已处理 / 输入空闲 → 立即投递；否则暂存，输入处理完成时投递；下一 vsync 到达仍暂存则合并投递并进入输入空闲；下一次输入开始退出输入空闲；无任何时间阈值
- [x] 4.7 `KRVsyncModule::OnVsync`：一帧的投递交给 `KRInputFirstGate`。有暂存时，新的 vsync 从「丢掉」改为「合并后投递」。真正执行时再检查 running 和 generation
- [x] 4.9 HarmonyOS 真机验证：Compose vsync 负载下同步 touchMove 等锁 p90 显著下降；手指停顿时帧节拍不减半、独立动画至多停顿开始时损失一帧；多指与 touch + scroll 并存的会话边界正确；手势中途移除 view 会话被释放；页面销毁时暂存投递被安全丢弃
    - 2026-10-09 部分完成（设备 2LQ0223A26007226，debug 包，SyncEventPerfPages 4ms/帧 vsync 负载，uitest 模拟）：touch / scroll 会话边界正确（含惯性）；等锁 p90 ≈ 0.013ms，响应延迟 p50/p90 ≈ 11.8/13.1ms；长按停顿仅合并 1 帧后进入输入空闲（frameDirect=180），帧节拍不减半；无断言。未测：多指、touch+scroll 并存、手势中途移除 view、销毁时暂存丢弃、Compose 页面
  - 2026-10-27 部分完成（同一设备，debug Kotlin，uitest）。`SyncComposePerfPage` 每帧 4ms 负载：拖动等锁 p90 0.009ms，延迟 p50/p90 11.1/12.5ms，`mainPullTasks=958`，`contextTasksWhileParked=0`；长按 `inputs=0`、`frameMerged=1`、`frameDirect=171`。手势中途移除 view：`removed` 与会话日志同一时刻打出，随后任务回到 context 线程。合成帧间隔中位数 8.34ms。未测：多指、touch 与 scroll 同时存在。强制停止进程时没有崩溃日志，但不能证明暂存的帧任务被丢掉。
  - 2026-10-28 完成（同一设备）。多指 `multitouch4.log`：一次 `session begin`，第一根手指抬起不结束会话，最后一根抬起才 `session end active=0`，inputs=657，延迟 p50/p90 9.64/10.41ms。touch 与 scroll 并存 `touchscroll.log`：active 从 1 到 2，touch 结束时 active=1，scroll 结束时 active=0。销毁 `destroy_wait.log`：在 touch 回调里同步走到 `OnDestroy`，并且反注册时暂存帧还在。同一毫秒打出 `unregister with deferred frame`、`session release`、`drop deferred frame`，接着 `KTRenderView WillDestroy`。这段 hilog 里没有 CppCrash。普通 `router.back()` 会等本次 `OnInputDone` 把暂存帧投出之后才销毁，所以那条路径打不出 drop。用来等到暂存帧的代码已经从源码去掉；丢掉帧的是 `OnDestroy` 里先 `KRVsyncModule::WillDestroy`，再由 view 释放会话并投出暂存帧。Compose 等锁和长按停一帧的 release 数据见 5.7。

## 5. renderer：连续同步事件期间由主线程执行 context 任务（D3，每轮最多 2ms）

- [x] 5.1 `KRThread`：持有 context 任务锁时取下一个任务的接口，供主线程每一轮调用
- [x] 5.2 `KRThread` 暂停/恢复：`SetParked` 与暂停期间的待执行通知；进入暂停时正在执行的 task 结束后（含等锁期间进入暂停）剩余批次退回队首、不再执行；恢复时唤醒 worker
- [x] 5.3 `KRMainThreadTaskPuller`：连续同步事件从无到有时暂停 context 线程并投递一轮，从有到无时恢复。每一轮最多 2ms，到时间再投下一轮，队列空就停。`KRInputFirstGate` 在开始和结束时通知
- [x] 5.4 主线程正在执行 context 任务时，产生的 UI 任务等锁释放后再执行（`PerformOnMainQueueWithTask`）
- [x] 5.5 不导出可以关掉这段行为的 C 函数。同步 touchMove 固定注册。若 context 任务仍留在 context 线程，同步 touchMove 会等完整个帧任务
- [x] 5.6 HarmonyOS 真机验证：会话期间 context 线程不执行任务（可通过 worker 唤醒/执行计数确认）、FIFO 顺序不变（序号断言）、会话结束后 worker 接续积压任务；UI 任务同步触发的 scroll 事件不出现 100ms 降级
  - 2026-10-27 同一设备。同步 touch / scroll 会话里 `contextTasksWhileParked=0`，`mainPullTasks` 为 1015–1228。滚动过程中任务日志在主线程，`scrollEnd` 之后回到 context 线程，并打出 `backlog`。`fifoBreaks=0`。hilog 没有 `DispatchAsync when run sync event`。
- [x] 5.7 复杂页面 release 版 Kotlin 指标：响应延迟 p50/p90、掉帧率、会话期间主线程 Kuikly 耗时；负载至少覆盖"每帧一个大任务"与"拆分小任务"两类。关闭分支已不保留，见 9.2
  - 2026-10-28 设备 2LQ0223A26007226，Mate 60 Pro，release Kotlin，120Hz，uitest 三次拖动。延迟 p50/p90、等锁 p90、exec p90、`frameMerged`、`mainPullTasks`：
    - 4ms 单任务：10.05/11.46、10.27/12.73、10.07/10.78；等锁 p90 0.009；exec p90 1.25–1.35；`frameMerged` 0/0/1；pull 261–276。对照 design.md「两者都做」10.4/11.8。
    - 8ms 拆 8 个：10.47/11.88、10.58/11.73、10.36/11.72；等锁 p90 0.006；exec p90 0.81–0.87；`frameMerged` 3/1/3；pull 555–576；`fifoBreaks=0`。对照 11.2/12.7。
    - 8ms 单任务：14.97/18.91、14.69/18.33、12.80/16.33；等锁 p90 0.007；exec p90 0.85–0.92；每次 `frameMerged=11`；pull 209–222。比 design.md「两者都做」的 11.0/12.5 差。那一行是主线程按 2ms 拉取、单个长任务仍可被拆开之前的实验。现在单个 8ms 任务在主线程上整段执行，输入在 looper 里等它，不把这个任务拆开。
    - Compose 每帧 4ms：9.26/9.77、9.37/10.85、9.38/10.67；等锁 p90 0.008–0.009。长按 `frameDirect=178`、`frameMerged=1`。
    - `KRSetThreadingMode(1)`，8ms 拆 8 个：`threadingMode=single`，`mainPullTasks=0`，延迟 13.47/18.52、13.61/18.20、13.57/18.28。对照「全局单线程」16.7/25.5，以及「每轮最多 2ms」10.1/11.1。这个开关是前者，主线程拉取不会在单线程模式里执行。
    - 合成帧间隔中位数都是 8.34ms。一次采集里超过 12ms 的间隔是 0–4 个，没有连续掉帧。`contextTasksWhileParked=0`，hilog 没有 `DispatchAsync when run sync event`。4ms/8ms 单任务页 `fifoBreaks` 最大 3，拆分负载是 0。
    - 以下为初版 D3（worker 照常执行、主线程按每帧配额拉取）的复测记录，因设计有误已作废，仅供参考：
      - 2026-10-09 部分完成：开关关闭时行为与第 4 组一致；开启（`--pi krTaskPull 1`）后无崩溃、无重入断言，但"每帧一个大任务"负载下，主线程每一轮都因队列空或拿不到锁停止（每会话约 200 次，执行到 0 个任务）——帧任务投递后 worker 立即取走并持锁执行，单次 try_lock 拿不到。需用 loadMode 2（拆分小任务）复测后再评估 D3 价值
      - 2026-10-09 loadMode 2 复测（`--pi krLoadMs 4 --pi krLoadMode 2`，三轮拖动，`krLoadMs`/`krLoadMode` 已确认透传到 pageData.params）：开关关（`krTaskPull 0`）与开（`krTaskPull 1`）的 touchMove 延迟无可测差异（p50 约 11.1–11.5ms，p90 约 11.8–13.3ms）；开启时 `pullSliceBudget` / `pullFrameQuota` / `pullPredictedCost` 均为 0，`pullQueueEmptyOrBusy` 约等于每次输入（每会话约 140，与 loadMode 0 时同形），即每次输入时要么队列已空、要么 worker 持锁，计数器无法区分二者。结论：在当前负载与交互下 D3 未见收益，保持默认关闭；D3 的有效场景（同步事件到达时主线程队列仍有积压且 worker 空闲）未覆盖，需另行构造后再决定是否默认开启
      - 2026-10-09 复杂列表 Compose 页复测（`SyncComplexComposePerfPage`：200 行、50ms 定时器刷新 40 行、手柄竖向拖拽排序；帧由 ComposeContainer vsync 驱动；拖拽 touchMove 为同步，仅在实验期间本地临时打开 `SuperTouchManager.useSyncMove`，已恢复，未提交）：开关关与开的 latency p50 中位数均为 11.7ms，p90 中位数 21.6 / 21.6ms；lockWait p90 中位数 3.25ms（关）对 3.84ms（开），开启时略差；`pullPredictedCost=2144`、`pullQueueEmptyOrBusy=4238`、`pullSliceBudget=1`、`pullFrameQuota=4`。结论：该负载下 D3 无收益，保持默认关闭。鸿蒙 Compose 同步 move 仍按设计保持关闭（`useSyncMove = !isOhOs`）

## 6. renderer：A/B 指标（D6）

- [x] 6.1 按会话聚合：响应延迟、等锁、同步事件回调本身的耗时（`execMs`）、帧任务投递分类计数（等待输入 / 合并 vsync / 直接投递）、进入输入空闲次数、主线程一轮执行的停止原因
- [x] 6.2 经性能/日志 adapter 输出；会话之外零开销路径
- [x] 6.3 校验指标与实验分支 bench 结果口径一致（已按代码核对：响应延迟 = 输入时间戳 → UI 提交完成，等锁与主线程耗时同口径，统计为 p50/p90/max；差异：实验按"空闲 2.5s 一轮"汇总，本实现按会话汇总）

## 7. demo 宿主（ohosApp，可选，不随正式代码合入）

- [x] 7.1 不透传 D3 开关。本地 A/B 用过的 `setMainThreadTaskPullEnabled` / `krTaskPull` 已从 ohosApp 去掉，不随正式代码合入

## 8. 鸿蒙回归

- [x] 8.1 HarmonyOS：自研 DSL 同步 touchMove / syncScroll、Compose 列表与动画、canvas、输入框、单线程模式，release 版 Kotlin 跑回归 bench（对照 design.md 数据表）
  - 2026-10-27 冒烟（debug，60Hz，无 bench）：`ComposeAnimationPage` 见 4.9；`CanvasExamplePage`、`ListViewDemoPage`（滑到第 9–14 项）、`InputViewDemoPage`（输入 kuikly，键盘弹出）均渲染且进程存活。
  - 2026-10-28 release Kotlin，同一设备。同步 touchMove / syncScroll / Compose 列表的数字见 5.7，单线程见 5.7 的 `threadingMode=single`。`InputViewDemoPage`：`keyboardHeightChange height=314`，text 从 k 到 kuikly。`CanvasExamplePage`、`ListViewDemoPage`、`ComposeAnimationPage` 都打出 LaunchData，进程存活。没有 `DispatchAsync when run sync event`。

## 9. 文档与后续

- [x] 9.1 运行 doc-archive-review。接入文档 `docs/QuickStart/harmony.md` 不写内部调度。说明写在 `.ai/references/ohos-sync-event-scheduling.md`（索引已登记）
- [x] 9.2 复杂页面的结论：主线程执行 context 任务，和鸿蒙 Compose 的同步 touchMove，要一起生效。没有单独关掉主线程执行的方式。只改同步 touchMove 时，滚动等锁 p90 10.4ms、延迟 p90 21.4ms；一起生效时 0.008ms / 11.1ms，手势期间上屏仍为 120Hz
- [x] 9.3 立项 Android（`postAtFrontOfQueue`）与 iOS（先处理输入再投递帧任务）后续 change：`openspec/changes/android-sync-event-post-at-front/`、`openspec/changes/ios-sync-event-input-first/`。两者都只写了 proposal，design / specs / tasks 未开始
