## Why

Kuikly Compose DSL 目前缺乏运行时的重组（recomposition）性能分析能力。当开发者遇到界面卡顿或性能问题时，无法快速定位哪些 Composable 发生了不必要的重组、重组的触发原因（哪个 State 变化导致），以及重组的频率和耗时。这直接影响了 Compose DSL 模式下的开发效率和性能调优体验。目前项目已有 `PageEventTrace` 机制追踪页面级事件，但尚未覆盖 Compose 重组维度的细粒度分析。

## What Changes

- **新增重组追踪引擎**：基于 `CompositionTracer`（`@InternalComposeTracingApi`）自动接收编译器注入的 traceEventStart/End 回调，**零侵入覆盖所有 @Composable 函数**的重组追踪，记录 Composable 名称（含源码位置）、重组耗时、父子关系
- **新增精确重组原因追踪**：通过 `CompositionObserver`（`@ExperimentalComposeRuntimeApi`）+ `RecomposeScopeObserver`，利用 `invalidationMap: Map<RecomposeScope, Set<Any>?>` 实现精确的 **scope→State** 关联，准确识别每个 Composable 重组是由哪个 State 变更触发的
- **新增框架 Composable 过滤**：通过 `RecompositionConfig.includeFrameworkComposables` 配置项控制是否包含框架内部 Composable（`androidx.compose.*`、`com.tencent.kuikly.compose.*` 等前缀），默认过滤以减少噪音
- **新增多形态输出**：支持三种产品形态输出重组分析结果：
  - **日志模式**（Log）：通过 `println` 输出结构化重组日志，包含 Composable 名称、耗时、触发 State
  - **UI Overlay 模式**：在界面上叠加半透明 overlay，实时显示各组件的重组次数和热力图
  - **结构化数据模式**（JSON/API）：以结构化数据格式输出分析结果，供 AI Agent 或外部工具程序化调用
- **新增开发者 API**：提供 `RecompositionProfiler` API，含 `start()`/`stop()`/`getReport()` 等接口
- **新增 Demo 页面**：在 demo 模块中添加重组分析工具的示例页面，演示 Start/Stop/Report 交互

## Non-goals

- **不修改 Compose Compiler 插件**：本工具完全基于运行时 API 实现，不涉及编译器插件开发
- **不覆盖自研 DSL 模式**：仅针对 Compose DSL（`ComposeContainer` + `setContent{}`），自研 DSL 的响应式追踪已有 `ReactiveObserver` + `PageEventTrace` 覆盖
- **不做性能自动修复**：工具只负责发现和报告问题，不自动修改用户代码
- **不做持久化存储**：分析数据为会话级别，不持久化到磁盘

## Capabilities

### New Capabilities

- `recomposition-tracker`: 重组追踪引擎核心能力 — 基于 CompositionTracer 自动追踪所有 Composable 重组事件（组件名含源码位置、耗时、频率、父子关系），通过 CompositionObserver 精确关联触发 State，支持框架 Composable 过滤
- `recomposition-output`: 多形态输出能力 — 将追踪数据以日志、UI Overlay、结构化 JSON 三种形式输出给开发者或 AI Agent
- `recomposition-profiler-api`: 开发者 API — 提供编程接口控制追踪的启停、过滤、报告生成；含 ProfilerLifecycleListener 机制通知已存在的 Scene 注册/取消 CompositionObserver

### Modified Capabilities

（无现有 spec 需要修改）

## Impact

**受影响模块：**
- `compose` — 核心改动模块，新增 `profiler` 包，修改 `ComposeSceneRecomposer`（帧追踪 hook）和 `BaseComposeScene`（CompositionObserver 注册）
- `demo` — 新增示例页面展示重组分析工具用法

**受影响平台：**
- Android / iOS / HarmonyOS / Web / macOS — 追踪引擎基于 `commonMain` 实现，跨全平台生效
- UI Overlay 模式需要各平台渲染器支持叠加层

**API 影响：**
- 新增 `com.tencent.kuikly.compose.profiler` 包，暴露 `RecompositionProfiler` 等公开 API
- 使用 `@InternalComposeTracingApi`（CompositionTracer）和 `@ExperimentalComposeRuntimeApi`（CompositionObserver）内部/实验性 API
- 不影响现有 API 的签名或行为，纯增量变更

**依赖影响：**
- 依赖 `androidx.compose.runtime` 的 `CompositionTracer`（`@InternalComposeTracingApi`）、`CompositionObserver`（`@ExperimentalComposeRuntimeApi`）和 `Snapshot` API
- 无新增外部依赖
