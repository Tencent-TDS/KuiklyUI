# ohos-vsync-frame-dispatcher

## Purpose

规范 HarmonyOS 端 Kuikly Compose DSL 帧调度驱动的实现契约：native `KRVsyncModule` 基于 `OH_NativeVSync` 提供系统级 vsync 回调，替代历史遗留的 Kotlin 侧 12ms 固定周期 Timer 轮询，使帧节拍与屏幕刷新率严格对齐（支持 60/90/120Hz 及 LTPO 变频自适应）。新旧宿主兼容由 `pageData.nativeBuild` 版本门控保证：宿主 native 库上报的代数达到 vsync 能力代数即启用 vsync 驱动，低于则回退 12ms Timer，无运行时探测。覆盖模块注册与调用约定、帧间隔上报口径与类型链、生命周期防护与背压语义、门控与回退行为。对应 `ComposeContainer.startFrameDispatcher` / `stopFrameDispatcher` 与 `ComposeSceneMediator.renderFrame` 下游链路。

## Requirements

### Requirement: 鸿蒙 Compose 帧调度 SHALL 由 nativeBuild 门控决定 vsync 或 12ms Timer

`ComposeContainer` 以计算属性 `useTimerFrameDispatcher`（`get()` 延迟求值：`pageData.isMiniApp || pageData.isWeb || (pageData.isOhOs && pageData.nativeBuild < OHOS_NATIVE_VSYNC_MIN_BUILD)`）统一决定帧驱动分支——真实 `pageData` 在 `onCreatePager -> pageData.init()` 才填充，构造期求值会读到默认实例恒为 false；鸿蒙端 `nativeBuild < OHOS_NATIVE_VSYNC_MIN_BUILD` 即回退 12ms Timer，否则走 native vsync。`startFrameDispatcher` 与 `stopFrameDispatcher` SHALL 共用该属性以保证注册与反注册严格对称（其依赖字段在页面生命周期内不可变，start/stop 必走同一分支）。`useTimerFrameDispatcher` 为 true 时 SHALL 走 12ms Timer（`mediator.startFrameDispatcher()`，返回的 `Timer` 实例由 `dispatchTimer` 持有）；为 false 时 SHALL 通过 `VsyncModule.registerVsyncWithFrameInterval` 注册 `OH_NativeVSync`（鸿蒙 `KRVsyncModule`）/ 平台既有 vsync（Android/iOS）驱动 `renderFrame`。判定为纯静态版本比对，无 watchdog、无 A/B 开关、无运行时特征探测。

#### Scenario: 120Hz 屏幕满帧驱动
- **WHEN** 页面在 120Hz 屏幕的 HarmonyOS 设备（nativeBuild ≥ 3）上发生滚动或动画等持续绘制活动
- **THEN** 帧调度节拍 SHALL 与屏幕刷新周期对齐（tick 间隔 p50 ≈ 8.33ms）
- **AND** 实测有效帧率 SHALL 接近 120fps（12ms Timer 时代上限 ~83fps）
- **AND** native 上报的帧间隔 SHALL 与屏幕周期一致（≈8.33ms）

#### Scenario: LTPO 变频自动跟随
- **WHEN** 页面静止或系统判定低负载，屏幕刷新率降档（如 120Hz → 60Hz）
- **THEN** 帧调度节拍 SHALL 自动跟随屏幕周期（tick 间隔 ≈16.67ms）
- **AND** 恢复绘制活动后节拍 SHALL 立即回到当前屏幕周期

#### Scenario: 旧宿主回退 12ms Timer
- **WHEN** 页面运行在 nativeBuild < 3 的宿主（旧版 native 库未内置 `KRVsyncModule`）
- **THEN** 帧调度 SHALL 走 12ms Timer 轮询，行为与改造前一致
- **AND** 页面滚动与动画 SHALL 正常工作（不因新旧 Kotlin/native 混搭失去帧驱动）
- **AND** `stopFrameDispatcher` SHALL 走 Timer 分支 `dispatchTimer.cancel()`，不调用 `unRegisterVsync`（start 时未注册 vsync）

#### Scenario: 非 HarmonyOS 平台路径不变
- **WHEN** 页面运行在 Android / iOS / miniApp / Web 宿主
- **THEN** Android / iOS SHALL 注册 `VsyncModule`（平台既有 vsync 驱动）；miniApp / Web SHALL 使用 12ms Timer 帧驱动
- **AND** miniApp / Web 的 Timer SHALL 在页面销毁时经 `dispatchTimer.cancel()` 释放（较改造前的"不 cancel"为修复项）

### Requirement: nativeBuild 门限 SHALL 随 native 能力发版同步声明

vsync 能力随 `nativeBuild = 3` 这一代 native 库发布（`KRVsyncModule` 合入）：`KRNativeRenderController.ets` 上报 `pageData[KRNativeBuild] = 3`，Kotlin 侧 `OHOS_NATIVE_VSYNC_MIN_BUILD = 3`，两者 SHALL 在同一次发版中同步变更。nativeBuild 只增不减，既有能力门槛（`>= 1`、`>= 2`、`< 3`、`isAndroid && < 4`、`>= 8` 等）不受 2→3 递增影响（经全量核对，唯一跨越的 `< 3` 垫片为字体缩放，受 `scaleFontSizeEnable` 默认 false 与鸿蒙 native 未实现双重保护，无行为变化）。

#### Scenario: 门限与能力同步发布
- **WHEN** native 库合入 `KRVsyncModule` 并发版
- **THEN** 上报的 nativeBuild SHALL 递增至能力代数（3），Kotlin 侧门限常量 SHALL 等于该代数
- **AND** 后续新增能力递增 nativeBuild 时，本门限 SHALL 保持不变

#### Scenario: 门控判定时机
- **WHEN** `startFrameDispatcher` 在 `onCreatePager` 内执行（`pageData.init()` 已完成）
- **THEN** `enableUseOhosNativeVsync` SHALL 读到真实 nativeBuild（非默认值 0）
- **AND** 构造期求值 SHALL 被禁止（读默认实例恒为 false，vsync 永不启用）

### Requirement: 帧间隔 SHALL 以 int32 纳秒值经相邻 vsync 时间戳差值计算并上报

`KRVsyncModule` SHALL 以相邻两次 vsync 时间戳差值计算帧间隔，钳制到 [1ms, 100ms] 区间；越界差值沿用最近一次有效值；首个 tick 无基准时不上报。回调值 SHALL 以 `KRRenderValue` int32 编码传递，保证 Kotlin 侧 `(data as? Int)` 类型匹配成立（int64 编码会使 Kotlin 收到 Long 并静默回退默认帧间隔 16.67ms，仅影响 idle 判定与 prefetch deadline 口径，不影响帧率）。

#### Scenario: 高刷屏帧间隔上报
- **WHEN** 设备屏幕以 120Hz 刷新且 vsync 回调连续到达
- **THEN** Kotlin 侧回调收到的帧间隔 SHALL 为 `Int` 类型的纳秒值且 ≈8,330,000
- **AND** 该值 SHALL 被透传给 `renderFrame` 用于 idle 判定与 prefetch deadline

#### Scenario: 异常差值防御
- **WHEN** 相邻 vsync 时间戳差值越界（如系统休眠恢复产生超大间隔）
- **THEN** 模块 SHALL 沿用最近一次有效帧间隔上报，不产出异常值

### Requirement: 模块 SHALL 具备完整生命周期防护与对称的注册/反注册

`KRVsyncModule` 的 vsync 请求 SHALL 携带 `weak_ptr` + `generation` 双重校验，模块销毁或反注册后在途回调 SHALL 安全丢弃（不发生 UAF）；`OH_NativeVSync_RequestFrame` 为一次性请求，每次回调 SHALL 先重新 arm 下一帧。当 context 线程尚未消化上一 tick 时，新 tick SHALL 被背压丢弃（`tick_pending_` 原子标志），防止慢帧堆积追帧。`ComposeContainer.stopFrameDispatcher` SHALL 与 `startFrameDispatcher` 经 `useTimerFrameDispatcher` 对称：Timer 分支 SHALL 调用 `dispatchTimer.cancel()` 停止 Timer 协程；vsync 分支 SHALL 调用 `unRegisterVsync`。此举修复历史上两处开合不对称的缺陷——① 原鸿蒙 stop 为空实现，vsync 注册后从不反注册（常驻回调功耗与泄漏）；② 原 miniApp/Web Timer 从不 cancel（协程泄漏，仅靠 paused 空转兜底）。

#### Scenario: 页面销毁安全
- **WHEN** 页面销毁触发 `stopFrameDispatcher` 且存在在途 vsync 回调
- **THEN** 回调 SHALL 因代数不匹配或 weak_ptr 失效被丢弃，进程不发生崩溃
- **AND** 后续不再有新的 vsync 回调进入 context 队列

#### Scenario: 慢帧背压
- **WHEN** 某帧处理耗时超过一个屏幕周期，期间新的 vsync 到达
- **THEN** 新 tick SHALL 被丢弃而非排队
- **AND** 帧任务消化完毕后的下一个 vsync SHALL 恢复正常节拍

#### Scenario: Timer 兜底路径销毁对称释放
- **WHEN** 走 Timer 帧驱动的页面（miniApp / Web / nativeBuild < 3 的鸿蒙）销毁触发 `stopFrameDispatcher`
- **THEN** SHALL 走 `useTimerFrameDispatcher` 分支执行 `dispatchTimer.cancel()`，停止 Timer 协程
- **AND** SHALL 不调用 `unRegisterVsync`（该页面从未注册 vsync）
- **AND** `dispatchTimer` 由 `mediator.startFrameDispatcher()` 返回的实例持有，start/stop 一一对应
