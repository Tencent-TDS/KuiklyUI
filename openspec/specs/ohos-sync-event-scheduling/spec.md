# ohos-sync-event-scheduling

## Purpose

规范 HarmonyOS renderer 连续同步事件与 context 任务怎么排：会话边界、当前任务做完后释放 context 任务锁、事件期间主线程一轮最多 2ms 执行 context 任务、持锁时推迟 UI、单线程 ScheduleTask、会话指标。

## Requirements

### Requirement: 同步连续事件 SHALL 以明确的开始/结束标志界定会话

HarmonyOS renderer SHALL 为所有支持同步的连续事件维护"连续同步事件会话"。会话的开始/结束标志 SHALL 取自 native 侧 ArkUI 状态，SHALL 不依赖 Kotlin 是否注册了对应的开始/结束事件，且 SHALL 仅在该 view 注册了对应的同步连续事件时上报：

- 同步 `touchMove`：首个 pointer DOWN 开始，最后一个 pointer UP 或 CANCEL 结束；
- 同步 `scroll`：ArkUI 滚动开始（拖动、惯性、带动画的 contentOffset）开始，滚动停止结束；
- `pan` / `pinch` / `longPress`（同步开放后）：手势 `state` 为 start 开始，为 end 结束。

`keyboardHeightChange` 不属于连续同步事件，SHALL 不产生会话：鸿蒙侧由 ArkTS `window.on('keyboardHeightChange')` 在键盘高度变化时推送一次，不是逐帧流，也没有动画结束标志。

多个会话并存时 SHALL 以引用计数合并，计数归零才离开会话状态；同一 view 同一事件类型的重复开始 SHALL 幂等。view 被移除或销毁时 SHALL 释放其持有的全部会话。会话判定 SHALL 不使用任何时间阈值。此外，`KRRenderCore` 在每次同步连续事件回调执行前 SHALL 通知 `KRInputFirstGate`"本帧输入开始"，在 Kotlin 回调与同步 UI 提交（`PerformMainThreadTaskWaitToSyncBlockIfNeed`）全部完成后 SHALL 通知"本帧输入处理完成"。

#### Scenario: touch 会话边界
- **GIVEN** HarmonyOS 页面通过 `touchMove(isSync = true)` 注册了同步移动事件
- **WHEN** 用户按下、拖动、抬起手指
- **THEN** 会话 SHALL 在 DOWN 时开始、在 UP 时结束
- **AND** 拖动期间每次 touchMove SHALL 在 Kotlin 回调前通知"本帧输入开始"，在 UI 提交后通知"本帧输入处理完成"

#### Scenario: 多指 touch
- **GIVEN** 同步 touch 会话进行中
- **WHEN** 第二根手指按下后先于第一根手指抬起
- **THEN** 会话 SHALL 持续到最后一根手指抬起或 CANCEL

#### Scenario: 惯性滚动属于 scroll 会话
- **GIVEN** HarmonyOS 列表开启 `syncScroll(true)`
- **WHEN** 用户拖动后松手进入惯性滚动直至停止
- **THEN** scroll 会话 SHALL 覆盖拖动与惯性全过程，在滚动停止时结束

#### Scenario: 多会话并存
- **GIVEN** 同步 touch 会话与同步 scroll 会话同时进行
- **WHEN** touch 会话结束而 scroll 仍在惯性滚动
- **THEN** 连续同步事件 SHALL 仍算进行中，直到 scroll 结束

#### Scenario: 结束标志丢失由生命周期释放
- **GIVEN** 某 view 的同步 touch 会话进行中
- **WHEN** 该 view 在手势中途被移除或销毁，未收到 UP/CANCEL
- **THEN** 该 view 持有的会话 SHALL 被释放，计数随之递减

#### Scenario: 异步事件不产生会话
- **GIVEN** HarmonyOS 页面以异步方式注册 `touchMove` 与 `scroll`
- **WHEN** 用户拖动或滚动
- **THEN** SHALL 不产生会话，帧任务投递行为与改造前一致

#### Scenario: 其他平台不受影响
- **GIVEN** Android 或 iOS 页面注册了同步 touchMove
- **WHEN** 用户拖动手指
- **THEN** 事件调度行为 SHALL 与改造前完全一致

### Requirement: context 线程 SHALL 在当前任务结束后释放锁，并把本批剩余任务放回队列队首

HarmonyOS context 线程（`KRThread`）一批任务执行期间，若主线程正在等 context 任务锁（`m_taskMutex`），context 线程 SHALL 在当前任务做完后、执行下一个任务前释放这把锁。释放前 SHALL 把本批还没执行的任务按原顺序放回共享队列队首，队列保持先入先出。context 线程 SHALL 等主线程真正拿到锁之后才重新竞争。该行为始终生效。

#### Scenario: 一批任务做到一半时主线程需要这把锁
- **GIVEN** context 线程正在执行一批 8 个各约 1ms 的任务
- **WHEN** 主线程在第 1 个任务执行期间到达同步事件并等待这把锁
- **THEN** 主线程等待时长 SHALL 不超过第 1 个任务的剩余耗时
- **AND** 剩余 7 个任务 SHALL 在同步事件之后按原顺序执行

#### Scenario: 释放锁不改变任务顺序
- **GIVEN** 释放锁时本批剩余任务 T2、T3，共享队列中已有更晚入队的任务 T4
- **WHEN** 主线程或 context 线程随后在持有这把锁时取任务执行
- **THEN** 执行顺序 SHALL 为 T2、T3、T4

#### Scenario: 单个长任务不被切分
- **GIVEN** worker 正在执行单个 8ms 的 context 任务
- **WHEN** 主线程到达同步事件
- **THEN** 主线程 SHALL 等待该任务执行完毕，任务本身 SHALL 不被中断

### Requirement: 连续同步事件期间 SHALL 由主线程执行 context 任务，每轮最多 2ms

HarmonyOS renderer 在连续同步事件期间 SHALL 由主线程执行 context 任务。同步 `touchMove` 始终注册。若这些任务仍留在 context 线程，同步 `touchMove` 会在这次连续触摸里等完整个帧任务。

- 连续同步事件从无到有 SHALL 暂停 context 线程：正在执行的任务 SHALL 做完，随后本批剩余任务 SHALL 按原顺序放回共享队列队首，context 线程 SHALL 不再执行任何任务。
- 暂停期间所有 context 任务 SHALL 仍进入同一共享队列，并 SHALL 由主线程经事件循环投递的一轮执行，在与同步事件相同的 context 任务锁下按先入先出执行。
- 每一轮 SHALL 最多连续执行 2ms。到时间 SHALL 回到主线程事件循环，并投递下一轮；队列为空 SHALL 停止，暂停期间有新任务时 SHALL 再投递一轮。不按帧设配额，也不提供配置。
- 连续同步事件从有到无 SHALL 恢复 context 线程，从同一共享队列继续执行；已经投递、还没执行的那一轮 SHALL 直接返回。
- 全过程 SHALL 只有一个先入先出队列和一把 context 任务锁。进入和退出 SHALL 不改变任务执行顺序。

#### Scenario: 会话期间由主线程执行
- **GIVEN** 页面 context 侧存在持续的帧任务负载
- **WHEN** 用户进行连续同步拖动
- **THEN** 会话期间 context 线程 SHALL 不执行任何 context 任务
- **AND** 帧任务、定时器等 context 任务 SHALL 在主线程上按 FIFO 执行
- **AND** 一轮的执行时长 SHALL 不超过 2ms 再加上一个已经开始的任务的执行时长

#### Scenario: 进入会话时 context 线程正在执行任务
- **GIVEN** 会话开始时 context 线程正在执行任务 T1，本批其后还有 T2、T3
- **WHEN** T1 执行完毕
- **THEN** T2、T3 SHALL 按原顺序退回共享队列队首，并由主线程按 T2、T3 的顺序执行

#### Scenario: 到 2ms 回到事件循环
- **GIVEN** 连续同步事件期间，共享队列里的任务超过 2ms
- **WHEN** 主线程执行一轮
- **THEN** 这一轮 SHALL 在 2ms 用完后回到事件循环，ArkUI SHALL 能在两轮之间分发输入
- **AND** 剩余任务 SHALL 在下一轮按先入先出继续执行

#### Scenario: 会话结束恢复双线程
- **GIVEN** 会话进行中且共享队列仍有任务
- **WHEN** 全部连续同步事件会话结束（如最后一根手指抬起且滚动已停止）
- **THEN** context 线程 SHALL 恢复执行剩余任务，顺序与入队顺序一致
- **AND** 已经投递、还没执行的那一轮 SHALL 直接返回

#### Scenario: 单线程模式下不重复暂停
- **GIVEN** 宿主以单线程模式（`KRSetThreadingMode(1)`）初始化
- **WHEN** 进行连续同步拖动
- **THEN** 行为 SHALL 与单线程模式一致，不发生暂停/恢复操作

### Requirement: 主线程执行 context 任务期间产生的 UI 任务 SHALL 在释放锁之后执行

主线程持有 context 任务锁执行 context 任务时，任务产生的主线程 UI 任务（包括经 `PerformOnMainQueueWithTask` 投递的 UI 刷新）SHALL 不在当前调用栈里直接执行，SHALL 推迟到这把锁释放之后执行。否则 UI 操作可能同步触发 ArkUI 事件，并在还拿着锁时再次进入同步事件。主线程拿着这把锁时再次进入 `TryRunOnCurThread` SHALL 直接返回 false，由调用方改成异步执行，SHALL 不等锁。

#### Scenario: 一轮任务里的 UI 刷新推迟执行
- **GIVEN** 主线程这一轮执行的任务里会投递 UI 刷新
- **WHEN** 这次刷新要投递主线程 UI 任务
- **THEN** UI 任务 SHALL 在这一轮释放 context 任务锁之后执行
- **AND** UI 操作同步触发的同步事件 SHALL 能拿到这把锁，不会空转 100ms 后改成异步

### Requirement: 单线程模式 ScheduleTask SHALL 保持异步语义

HarmonyOS 单线程模式（`KRSetThreadingMode(1)`）下，`KRContextScheduler::ScheduleTask` 在 delay ≤ 0 时 SHALL 将任务投递到主线程下一轮事件循环执行，SHALL 不在调用栈内联执行；delay > 0 时 SHALL 由主线程定时器到期后执行。

#### Scenario: 单线程模式首屏不再死锁
- **GIVEN** 宿主以单线程模式初始化 kuikly
- **WHEN** 页面创建过程中首次产生 UI 任务（`KRUIScheduler::AddTaskToMainQueueWithTask` 拿着锁投递 UI 刷新）
- **THEN** 这次 UI 刷新 SHALL 在下一轮事件循环执行
- **AND** 页面 SHALL 正常完成首屏渲染，不出现主线程死锁与 appfreeze

#### Scenario: 多线程模式不受影响
- **GIVEN** 宿主以默认多线程模式初始化
- **WHEN** 调用 `ScheduleTask`
- **THEN** 任务 SHALL 仍投递到 context 线程执行

### Requirement: renderer SHALL 提供连续同步事件调度的 A/B 指标

HarmonyOS renderer SHALL 按一次连续同步事件聚合并通过性能或日志 adapter 输出以下指标：同步事件响应延迟（输入事件时间戳到本次 UI 提交完成，只含带输入时间戳的 touch 类事件）、等待 context 任务锁的时长、同步事件回调本身的耗时（`execMs`，不含主线程执行 context 任务的时间）、帧任务投递分类计数（等输入处理完再投递 / 合并下一次 vsync 后投递 / 直接投递）、进入「输入已经空闲」的次数、主线程一轮执行的停止原因计数（2ms 用完 / 队列空或拿不到锁）。事件之外 SHALL 不采集这些指标。

#### Scenario: 会话结束输出指标
- **GIVEN** 用户在 HarmonyOS 页面完成一次连续同步拖动
- **WHEN** 全部会话结束
- **THEN** renderer SHALL 输出一组包含上述字段的指标

#### Scenario: 会话之外不采集
- **GIVEN** 页面无任何连续同步事件会话
- **WHEN** context 线程持续执行任务
- **THEN** renderer SHALL 不记录调度指标
