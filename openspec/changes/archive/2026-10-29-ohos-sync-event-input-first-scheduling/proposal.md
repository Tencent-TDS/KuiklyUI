## Why

鸿蒙上的连续同步事件（同步 `touchMove`、同步 `scroll`）在 context 线程有任务时会拖慢主线程。vsync 投递的帧任务（Compose 重组就是这种任务）和同时到达的输入抢同一把 context 任务锁。大约一半的 touchMove 要在主线程上等完整个帧任务（8ms 负载下等待 p90 约 8ms，从输入到 UI 提交的延迟 p90 约 18ms，超过 120Hz 的一帧）。这是鸿蒙 Compose 以前把 touchMove 做成异步的原因。真机实验（Mate 60 Pro，release 版 Kotlin，见 design.md）表明：换线程本身只要几微秒。慢在两件事：输入和帧任务谁先执行，以及一次要等一整批任务。这两件可以在现有线程模型里改。

## What Changes

- **连续同步事件的开始和结束**：所有支持同步的连续事件（`touchMove`、`scroll`。`keyboardHeightChange` 不是逐帧回调，不包括。`pan` / `pinch` / `longPress` 这次不接入）用 native 的开始和结束标志来判断：touch 从 DOWN 到 UP/CANCEL，滚动从开始到停止。多段同时进行时用计数，view 移除或销毁时清掉它登记的事件。
- **先处理输入，再投递帧任务**：连续同步事件期间，`KRVsyncModule` 的帧任务等到本帧输入处理完（含同步 UI 提交）再投递。本帧没有输入时，最多合并下一次 vsync 再投递，之后到下一次输入之前直接投递。本帧输入已经处理完则马上投递。不用时间阈值。
- **当前任务做完后释放锁**：主线程在等 context 任务锁时，context 线程做完当前任务就释放锁，并把本批还没执行的任务按原顺序放回队列队首。队列仍然先入先出。
- **连续同步事件期间由主线程执行 context 任务**：这段时间暂停 context 线程。队列里的任务改由主线程持有同一把锁、按原顺序执行。每一轮最多 2ms，到时间回到事件循环。事件结束后，context 线程从同一队列继续。同步 `touchMove` 始终注册；这些任务若仍留在 context 线程，同步 `touchMove` 会等完整个帧任务。
- **鸿蒙 Compose 同步 touchMove**：`SuperTouchManager` 固定注册同步 `touchMove`，不再把鸿蒙排除在外。这次移动由谁消费，必须在回调返回前决定。
- **修复单线程模式死锁**：`KRContextSchedulerSingleThreaded::ScheduleTask(0)` 原先在主线程上直接执行。`KRUIScheduler::AddTaskToMainQueueWithTask` 拿着 `m_mutex_` 时再次加同一把锁，第一个 UI 任务就把页面卡死。改为投到下一轮事件循环。
- **指标**：按一次连续同步事件汇总从输入到 UI 提交的延迟、主线程等锁时长、同步事件回调本身的耗时（`execMs`，不含主线程执行 context 任务的时间）、主线程一轮执行的停止原因、帧任务怎么投递的，以及输入空闲了几次。

## Non-goals

- 不改变线程模型的全局设置方式，不引入按页面选择线程模型。
- 不处理 Android / iOS。差异见 design.md「跨端适用性」。后续单独做：Android 用 `postAtFrontOfQueue`，iOS 也改成先处理输入再投递帧任务。
- 不改变 Kotlin 侧（core / compose）的 API 和事件注册方式。鸿蒙 Compose 只把 touchMove 改成同步注册，不改谁消费，也不改 `preventTouch` 的时序。
- 不提供"连续事件等锁超时降级为异步"能力（实验证明会导致同步/异步交替与潜在乱序，损害大于收益）。
- 不切分单个长任务（单个超过帧间隔的 context 任务在任何方案下都无法被切分）。

## Capabilities

### New Capabilities
- `ohos-sync-event-scheduling`: core-render-ohos 里连续同步事件和 context 任务怎么排：事件从哪一帧开始到哪一帧结束、当前任务做完后释放锁并把剩余任务放回队首、事件期间由主线程执行 context 任务、单线程模式 `ScheduleTask` 不在当前调用栈里执行、指标。

### Modified Capabilities
- `ohos-vsync-frame-dispatcher`: 帧任务不再在 vsync 回调一到就投递。连续同步事件期间可以等到本帧输入处理完再投递，最多合并下一次 vsync。上一帧还没执行完就丢掉新帧，这条规则不变。

## Impact

- **平台**：仅 HarmonyOS。Android / iOS / Web / miniApp / macOS 不受影响。
- **模块**：renderer（`core-render-ohos`）；`compose` 只把鸿蒙的 touchMove 改成同步注册；`demo` 可以另加验证页，不随正式代码合入。
- **代码**：`foundation/thread/KRThread`（当前任务做完后释放锁，剩余任务放回队首）、`scheduler/KRContextScheduler`（暂停和恢复 context 线程、主线程取任务、单线程死锁修复）、新增 `scheduler/KRInputFirstGate`（记录连续同步事件，并决定帧任务何时投递）、新增 `scheduler/KRMainThreadTaskPuller`（事件期间由主线程执行 context 任务）、`expand/modules/vsync/KRVsyncModule`（按上面的规则投递帧任务）、`core/KRRenderCore`（同步连续事件处理前后通知 `KRInputFirstGate`，并记指标）、`KRView` 的 touch、`KRScrollerView` 的滚动。
- **API**：不给宿主增加可以关掉这些行为的 C 函数。同步 `touchMove` 始终注册。连续同步事件期间，context 任务改在主线程执行；若仍留在 context 线程，同步 `touchMove` 会等完整个帧任务。行为变化是：先处理输入再投递帧任务、当前任务做完后释放锁、事件期间由主线程执行 context 任务、鸿蒙 Compose 的 touchMove 改为同步。
- **风险**：在 context 任务中执行阻塞 IO 的 native 模块，会在手势期间阻塞主线程；需在验证中观察。
