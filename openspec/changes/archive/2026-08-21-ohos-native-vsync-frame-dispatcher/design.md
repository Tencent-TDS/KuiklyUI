# Design: 鸿蒙 Native Vsync 帧驱动

## Context

Compose DSL 的动画/重组/重绘依赖 Kotlin 侧的「帧时钟」（等价 Android Choreographer）。`ComposeContainer.startFrameDispatcher` 负责为每个页面实例启动帧驱动，`ComposeSceneMediator.renderFrame(frameIntervalNanos)` 是所有驱动源的共同下游：

```
驱动源(Timer/vsync) → renderFrame(frameIntervalNanos) → ComposeScene.render(canvas, nanoTime)
                                                     → 动画时钟(本地 nanoTime) + idle/prefetch deadline(帧间隔)
```

各端现状：Android 走 `VsyncModule`（Choreographer）；iOS 定时器；鸿蒙因为 native 从未实现 `KRVsyncModule`——Kotlin `registerVsync` 经 bridge 查找模块失败后由 `KRForwardArkTSModule` 转发到 ArkTS 层，ArkTS 层同样没有实现，回调石沉大海——历史上退化为 `mediator.startFrameDispatcher()` 的 12ms Timer。

本地 `VsyncModule.registerVsyncWithFrameInterval` 为 atomic 回调 API：`toNativeWithAtomicCallback(keepCallbackAlive=true, "registerVsync", callback)`，回调签名 `(Int) -> Unit`（帧间隔纳秒），无每帧 JSON 序列化开销。

## Goals / Non-Goals

**Goals:**
- 鸿蒙 Compose 帧节拍与屏幕 vsync 严格对齐，120Hz 满帧
- 帧间隔经时间戳差值计算，自适应 60/90/120Hz 与 LTPO 变频
- 旧宿主（无该模块的 so）自动兜底退回 12ms Timer，不出现「失去帧驱动」的回归
- 页面销毁可靠反注册，无 UAF、无泄漏、无常驻回调功耗

**Non-Goals:**
- 不改 `core/` 的 `VsyncModule` API 与 bridge 协议
- 不改动画时钟实现（`renderFrame` 继续用 `DateTime.nanoTime()`，帧间隔仅用于 idle 判定与 prefetch deadline）
- 不做 iOS / miniApp / Web
- 不含 performance 观测链与验证期日志设施

## 技术方案

### 1. KRVsyncModule（native，发动机本体）

`core-render-ohos/src/main/cpp/libohos_render/expand/modules/vsync/KRVsyncModule.h/.cpp`

- 接口：`CallMethod(sync, method, params, callback)`，`method` 支持 `registerVsync` / `unRegisterVsync`（与 `ModuleConst.VSYNC` 模块名、Kotlin `VsyncModule` 的调用约定一致）
- **每帧重 arm**：`OH_NativeVSync_RequestFrame` 是一次性请求，`OnVsync` 回调内先重新请求下一帧，再处理本帧
- **帧间隔口径**：`interval = timestamp - last_timestamp_nanos_`；钳制 [1ms, 100ms]（与 Kotlin 侧同区间）；越界（休眠恢复等）沿用最近有效值；首个 tick 无基准不上报（Kotlin 侧启动时已手动渲染首帧）
- **类型链（关键约束）**：回调值必须 `KRRenderValue::Make((int32_t)interval)`。napi 路径 `Type::INT -> napi_create_int32 -> Kotlin Int` 成立；若传 int64 会在 Kotlin 侧变 `Long`，`(data as? Int)` 匹配失败后**静默**回退 16.67ms 默认帧间隔（帧率不受影响，但 idle/prefetch 按 60Hz 口径计算）
- **生命周期防护**：请求上下文携带 `weak_ptr<KRVsyncModule> + generation`；模块销毁 / 反注册 → `running_=false; callback_=nullptr; generation_++`，在途回调因 weak_ptr 失效或代数不匹配直接丢弃
- **背压**：`tick_pending_` 原子标志。`exchange(true)` 返回 true（上一 tick 尚在 context 队列）→ 丢弃本 tick；复位通过 `KRContextScheduler::ScheduleTask(0, ...)` 排入与帧任务相同的 FIFO 队列紧随其后，保证帧任务实际消费后才复位
- **线程模型**：`OnVsync` 在系统 vsync 线程执行；`KRRenderCallback` 内部（`KRRenderCore.cpp:345-355`）自动 `PerformTaskOnContextQueue` 切到 context 线程，因此 vsync 线程直接调用回调是安全的

### 2. 模块注册与构建

- `ModulesRegisterEntry.h`：`RegisterModuleCreator(KRVsyncModule::MODULE_NAME, ...)`（仿 KRFileModule 写法）
- `CMakeLists.txt`：SOURCE_SET 追加源文件；`libnative_vsync.so` 已在既有链接列表中

### 3. Kotlin 侧切换（ComposeContainer.kt）

```
isMiniApp/isWeb → Timer（不变）
isOhOs → ohosUseNativeVsync ? startOhosNativeVsyncDriver() : Timer（A/B 开关）
else（Android/iOS）→ VsyncModule.registerVsyncWithFrameInterval（不变）
```

- `startOhosNativeVsyncDriver`：注册 vsync 回调；`setTimeout(pagerId, 100ms)` watchdog——超时未收到首个 tick 判定宿主 so 不支持 → `unRegisterVsync` + 退回 `mediator.startFrameDispatcher()`（12ms Timer）+ 降级日志
- `stopFrameDispatcher`：鸿蒙补上 `unRegisterVsync`（修复原空实现）；同时取消 watchdog 与兜底 Timer
- 状态标志（`ohosVsyncTickArrived` / `ohosVsyncDriverStopped`）均在 context 线程读写，无需原子操作

## 备选方案与取舍

| 备选 | 否决原因 |
|---|---|
| `OH_NativeVSync_GetRefreshRate` 查刷新率换算帧间隔 | 一次性查询无法跟随 LTPO 动态变频；时间戳差值天然自适应 |
| Cherry-pick 上游 PR #1520 的 vsync 部分 | 该 PR 基线为 `registerVsync((Long)->Unit)` 时间戳回调；本地已演进为 atomic `(Int)` 帧间隔 API，需适配重写而非直接搬 |
| watchdog 用同步返回值判断模块存在 | bridge 异步转发到 forward 模块时同样"成功接收"，同步判断不可靠，超时探测是唯一可靠手段 |

## 已知风险与缓解

| 风险 | 缓解 |
|---|---|
| Kotlin 侧收到非 Int（Long/Double）静默回退默认帧间隔 | 类型链已在实现中锁定 int32；如发生，帧率不受影响（仅 idle/prefetch 口径），可用 debug 日志快速定位 |
| vsync 线程回调与模块销毁竞态 | weak_ptr + generation + running_ 三重校验，全链路持锁 |
| 慢帧时 tick 堆积造成追帧抖动 | tick_pending_ 背压直接丢弃，下一 vsync 重新对齐 |
| 旧宿主无模块 → 页面失去帧驱动 | watchdog 100ms 超时退回 Timer，行为与改造前完全一致 |

## 验证

- 编译：`:compose:compileDebugKotlinAndroid`、`2.0_ohos_demo_build.sh`、hvigor `assembleHap` 全通过；`libkuikly.so` 含 `KRVsyncModule` 符号（`OH_NativeVSync_*` 动态链接）
- 真机（120Hz）：p50 8.3ms / est_fps ~120 / driverReportedInterval=8.28ms；LTPO 降频时 16.57ms 自动跟随；稳态 jank 大幅下降；页面退出无 crash、无 fallback 日志（新 so 链路正常）
- A/B 开关实测：timer 段 p50 12.1ms 恒定、est_fps 82；vsync 段 p50 8.3ms、est_fps 120（详见 proposal 验证表）
