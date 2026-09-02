# ohos-vsync-frame-dispatcher

## Purpose

规范 HarmonyOS 端 Kuikly Compose DSL 帧调度驱动的实现契约：native `KRVsyncModule` 基于 `OH_NativeVSync` 提供系统级 vsync 回调，替代历史遗留的 Kotlin 侧 12ms 固定周期 Timer 轮询，使帧节拍与屏幕刷新率严格对齐（支持 60/90/120Hz 及 LTPO 变频自适应）。覆盖模块注册与调用约定、帧间隔上报口径与类型链、生命周期防护与背压语义、旧宿主 watchdog 兜底与 A/B 开关行为。对应 `ComposeContainer.startFrameDispatcher` 鸿蒙分支与 `ComposeSceneMediator.renderFrame` 下游链路。

## Requirements

### Requirement: 鸿蒙 Compose 帧调度 SHALL 由 native vsync 驱动替代 12ms Timer

当 `ohosUseNativeVsync` 开启（默认）且页面运行在 HarmonyOS 宿主（native 库包含 `KRVsyncModule`）时，`ComposeContainer` 的帧调度器 SHALL 通过 `VsyncModule.registerVsyncWithFrameInterval` 注册 `OH_NativeVSync` 回调驱动 `ComposeSceneMediator.renderFrame`，不再使用固定 12ms Timer。

#### Scenario: 120Hz 屏幕满帧驱动
- **WHEN** 页面在 120Hz 屏幕的 HarmonyOS 设备上发生滚动或动画等持续绘制活动
- **THEN** 帧调度节拍 SHALL 与屏幕刷新周期对齐（tick 间隔 p50 ≈ 8.33ms）
- **AND** 实测有效帧率 SHALL 接近 120fps（12ms Timer 时代上限 ~83fps）
- **AND** native 上报的帧间隔（`driverReportedInterval`）SHALL 与屏幕周期一致（≈8.28ms）

#### Scenario: LTPO 变频自动跟随
- **WHEN** 页面静止或系统判定低负载，屏幕刷新率降档（如 120Hz → 60Hz）
- **THEN** 帧调度节拍 SHALL 自动跟随屏幕周期（tick 间隔 ≈16.67ms）
- **AND** 恢复绘制活动后节拍 SHALL 立即回到当前屏幕周期

#### Scenario: A/B 开关闭合时回到旧行为
- **WHEN** `ComposeContainer.ohosUseNativeVsync` 被设置为 `false`
- **THEN** 鸿蒙端 SHALL 使用改造前的 12ms Timer 帧调度
- **AND** miniApp / Web / Android / iOS 的帧调度路径 SHALL 不受任何影响

### Requirement: 帧间隔 SHALL 以 int32 纳秒值经相邻 vsync 时间戳差值计算并上报

`KRVsyncModule` SHALL 以相邻两次 vsync 时间戳差值计算帧间隔，钳制到 [1ms, 100ms] 区间；越界差值沿用最近一次有效值；首个 tick 无基准时不上报。回调值 SHALL 以 `KRRenderValue` int32 编码传递，保证 Kotlin 侧 `(data as? Int)` 类型匹配成立（int64 编码会使 Kotlin 收到 Long 并静默回退默认帧间隔 16.67ms，仅影响 idle 判定与 prefetch deadline 口径，不影响帧率）。

#### Scenario: 高刷屏帧间隔上报
- **WHEN** 设备屏幕以 120Hz 刷新且 vsync 回调连续到达
- **THEN** Kotlin 侧回调收到的帧间隔 SHALL 为 `Int` 类型的纳秒值且 ≈8,330,000
- **AND** 该值 SHALL 被透传给 `renderFrame` 用于 idle 判定与 prefetch deadline

#### Scenario: 异常差值防御
- **WHEN** 相邻 vsync 时间戳差值越界（如系统休眠恢复产生超大间隔）
- **THEN** 模块 SHALL 沿用最近一次有效帧间隔上报，不产出异常值

### Requirement: 模块 SHALL 具备完整生命周期防护与背压语义

`KRVsyncModule` 的 vsync 请求 SHALL 携带 `weak_ptr` + `generation` 双重校验，模块销毁或反注册后在途回调 SHALL 安全丢弃（不发生 UAF）；`OH_NativeVSync_RequestFrame` 为一次性请求，每次回调 SHALL 先重新 arm 下一帧。当 context 线程尚未消化上一 tick 时，新 tick SHALL 被背压丢弃（`tick_pending_` 原子标志），防止慢帧堆积追帧。`ComposeContainer.stopFrameDispatcher` 在鸿蒙端 SHALL 调用 `unRegisterVsync` 停止 vsync 回调（修复历史上的空实现），消除常驻回调的功耗与泄漏隐患。

#### Scenario: 页面销毁安全
- **WHEN** 页面销毁触发 `stopFrameDispatcher` 且存在在途 vsync 回调
- **THEN** 回调 SHALL 因代数不匹配或 weak_ptr 失效被丢弃，进程不发生崩溃
- **AND** 后续不再有新的 vsync 回调进入 context 队列

#### Scenario: 慢帧背压
- **WHEN** 某帧处理耗时超过一个屏幕周期，期间新的 vsync 到达
- **THEN** 新 tick SHALL 被丢弃而非排队
- **AND** 帧任务消化完毕后的下一个 vsync SHALL 恢复正常节拍

### Requirement: 旧宿主 SHALL 由 watchdog 自动兜底退回 12ms Timer

当宿主 app 使用不含 `KRVsyncModule` 的旧版 native 库时，`registerVsync` 请求会被转发到 ArkTS 层且不产生回调。`ComposeContainer` SHALL 通过 watchdog（注册后 100ms 内未收到首个 tick）判定该情形，自动执行 `unRegisterVsync` 并退回 12ms Timer 帧调度，同时输出一次降级日志；页面 SHALL 保持与改造前一致的可用性（不出现失去帧驱动导致的不可滚动）。

#### Scenario: 旧宿主降级
- **WHEN** 页面运行在 native 库未内置 `KRVsyncModule` 的宿主中
- **THEN** watchdog 超时后帧调度 SHALL 退回 12ms Timer
- **AND** SHALL 输出一次降级日志（tag `ComposeContainer`）
- **AND** 页面滚动与动画 SHALL 正常工作

#### Scenario: 新宿主无误降级
- **WHEN** 页面运行在包含 `KRVsyncModule` 的宿主中且 vsync 回调正常
- **THEN** watchdog SHALL 在首个 tick 到达后失效，不触发降级
