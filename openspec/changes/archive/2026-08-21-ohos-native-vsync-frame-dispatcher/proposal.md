# Proposal: 鸿蒙 Compose 帧驱动从 12ms Timer 迁移到 Native Vsync

> 参考上游 PR：Tencent-TDS/KuiklyUI#1520（该 PR 同时包含 iOS CADisplayLink 与语义树合帧，均不在本次范围内）

## Why

Kuikly 的 Compose DSL 在鸿蒙端，帧驱动（`ComposeContainer.startFrameDispatcher`）历史上因为 native 侧没有实现 `KRVsyncModule`，退化为 Kotlin 侧 **12ms 固定周期 Timer 轮询**（`ComposeSceneMediator.startFrameDispatcher`）。该方案存在三个结构性缺陷：

1. **帧率上限锁死 ~83fps**：12ms 一 tick = 理论上限 83.3fps。120Hz 高刷屏（8.33ms 帧间隔）根本喂不满，约 30% 的硬件刷新能力被浪费。
2. **节拍失拍（beat）**：83Hz 的 tick 网格与 60Hz（16.67ms）/120Hz（8.33ms）的屏幕 vsync 网格不对齐，tick 时刻周期性落在 vsync 窗口之外造成整帧丢弃，肉眼可见的周期性顿挫。
3. **对屏幕状态无感知**：Timer 间隔固定，无法跟随 LTPO 变频（静止时屏幕降到 60Hz，Timer 仍按 12ms 空转，浪费 CPU）；也无法向 Kotlin 侧上报真实帧间隔（`renderFrame` 的 idle 判定 / prefetch deadline 一直按默认 16.67ms 假设计算）。

实测基线（120Hz 真机，LazyColumnDemo5 / VerticalPagerDemo / ComposeAnimationPage）：p50 tick 间隔 12.1~12.8ms，est_fps 78~83 封顶，jank>12.5ms 占 22%~68%。

## What Changes

- 新增鸿蒙 native 模块 `KRVsyncModule`（基于 `OH_NativeVSync`），实现 `registerVsync` / `unRegisterVsync`：
  - 每次系统 vsync 回调后重新 arm 下一帧（`OH_NativeVSync_RequestFrame` 为一次性请求）
  - 以相邻两次 vsync 时间戳差值计算帧间隔，钳制到 [1ms, 100ms]，**以 int32 纳秒值**经 KRRenderCallback 上报 Kotlin（保证 Kotlin 侧 `as? Int` 类型链成立，int64 会退化为 Long 导致回退默认值）
  - 生命周期防护：`weak_ptr` + `generation` 双重校验，模块销毁/反注册后在途回调安全丢弃（无 UAF）
  - 背压：`tick_pending_` 原子标志，context 线程未消化上一 tick 时丢弃新 tick，防止慢帧堆积
- 注册模块到 `ModulesRegisterEntry.h`，`CMakeLists.txt` 追加编译单元（`libnative_vsync.so` 已有链接，无需改动）
- `ComposeContainer.kt`（compose 模块）：
  - 鸿蒙分支从 12ms Timer 切换到 `VsyncModule.registerVsyncWithFrameInterval` 路径（与 Android/iOS 共用）
  - **watchdog 兜底**：注册后 100ms 未收到首个 tick（旧宿主 so 无 KRVsyncModule，请求被转发到 ArkTS 层后无回调）→ 自动 `unRegisterVsync` 并退回 12ms Timer，打降级日志
  - `ohosUseNativeVsync` 静态开关（默认 true），可切回旧行为做 A/B 对比
  - 修复 `stopFrameDispatcher` 对鸿蒙的空实现：补上 `unRegisterVsync`（消除 vsync 常驻回调的功耗隐患与 Timer 泄漏隐患）
- miniApp / Web / Android / iOS 路径零改动

### Non-goals

- 不改 iOS（上游 PR 的 CADisplayLink 方案）
- 不改自研 DSL（其动画/滚动由 native 驱动，不经过 `ComposeContainer` 帧调度器）
- 不含语义树合帧修复（上游 PR 中的 RootNodeOwner 语义合帧已在 main 合入）
- 不含 performanceAPI 观测链改动（`KRFrameData` 的 realFps / 字段透传 / demo FRAME 监控，另行提交）
- 不含验证期设施（`FrameTickMonitor` 观测日志、`VsyncBenchmarkPage` Demo 页，本地保留不提交）

## Capabilities

### New Capabilities
- `ohos-vsync-frame-dispatcher`: 鸿蒙 Compose 帧调度的 vsync 驱动契约——模块注册、帧间隔上报口径、类型链、生命周期与背压语义、watchdog 兜底与 A/B 开关行为

### Modified Capabilities
- （无）

## Impact

- **影响平台**：仅 HarmonyOS
- **影响模块**：
  - `core-render-ohos`：新增 `expand/modules/vsync/KRVsyncModule.h/.cpp`；`ModulesRegisterEntry.h` 注册；`CMakeLists.txt` 源文件列表
  - `compose`：`ComposeContainer.kt` 帧驱动分支、watchdog、开关、停止逻辑
- **不影响**：`core/` 模块（`VsyncModule.registerVsyncWithFrameInterval` 为既有 API，未改动）、miniApp/Web/Android/iOS
- **兼容性**：旧宿主（无 KRVsyncModule 的 so）经 watchdog 自动退回 12ms Timer，行为与改造前一致；新宿主默认启用 vsync 驱动
- **依赖**：`libnative_vsync.so`（`native_vsync/native_vsync.h`，NDK 既有能力，无新增第三方依赖）

## 验证结论（120Hz 真机）

| 指标 | timer-12ms 基线 | ohos-vsync |
|---|---|---|
| FrameTick p50 间隔 | 12.1~12.8ms | 8.3ms（120Hz）/ 16.6ms（LTPO 降频时） |
| est_fps | 78~83 封顶 | 119.4~120.7 |
| driverReportedInterval | n/a | 8.28ms（与屏幕周期一致，LTPO 60Hz 时 16.57ms） |
| 稳态 jank>12.5ms | 51~204 /300 帧 | 4~29 /300 帧 |
| 静止时无效帧计算 | 12ms 空转 | 跟随屏幕降频（-44%） |
