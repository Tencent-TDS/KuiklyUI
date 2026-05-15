## ADDED Requirements

### Requirement: Profiler 启停控制
系统 SHALL 提供全局 API 控制重组追踪的启用和停止。

#### Scenario: 启用追踪
- **GIVEN** RecompositionProfiler 处于未启用状态
- **WHEN** 开发者调用 `RecompositionProfiler.start()`
- **THEN** 系统 SHALL 开始采集重组事件（注册 CompositionTracer、启动 Snapshot 观察者），`RecompositionProfiler.isEnabled` 返回 `true`，通知所有 `ProfilerLifecycleListener` 注册 CompositionObserver

#### Scenario: 停止追踪
- **GIVEN** RecompositionProfiler 处于启用状态
- **WHEN** 开发者调用 `RecompositionProfiler.stop()`
- **THEN** 系统 SHALL 停止采集重组事件，释放追踪相关资源（取消 CompositionTracer 注册、注销 Snapshot 观察者），`RecompositionProfiler.isEnabled` 返回 `false`，通知所有 `ProfilerLifecycleListener` 取消 CompositionObserver

#### Scenario: 重复启停安全
- **GIVEN** RecompositionProfiler 处于任意状态
- **WHEN** 开发者连续调用 `start()` 或 `stop()` 多次
- **THEN** 系统 SHALL 幂等处理，不抛出异常，不产生副作用

### Requirement: 追踪配置
系统 SHALL 提供配置接口，允许开发者自定义追踪行为。

#### Scenario: 配置采样率
- **GIVEN** RecompositionProfiler 未启用
- **WHEN** 开发者调用 `RecompositionProfiler.configure { sampleRate = 0.5f }`
- **THEN** 后续启用时 SHALL 以 50% 采样率采集数据

#### Scenario: 配置热点阈值
- **GIVEN** RecompositionProfiler 未启用
- **WHEN** 开发者调用 `RecompositionProfiler.configure { hotspotThreshold = 20 }`
- **THEN** 报告中 SHALL 将每秒重组超过 20 次的 Composable 标记为热点

#### Scenario: 配置框架 Composable 过滤
- **GIVEN** RecompositionProfiler 未启用
- **WHEN** 开发者调用 `RecompositionProfiler.configure { includeFrameworkComposables = true }`
- **THEN** 后续追踪 SHALL 包含框架内部 Composable（`androidx.compose.*`、`com.tencent.kuikly.compose.*` 等前缀）

#### Scenario: 配置 State 身份标识
- **GIVEN** RecompositionProfiler 未启用
- **WHEN** 开发者调用 `RecompositionProfiler.configure { enableStateIdentity = true }`
- **THEN** 后续追踪 SHALL 为 State 对象分配稳定短 ID 并追踪读取者 Composable，`triggerStates` 输出格式变为 `State#1(value=3), readers: CounterSection`

#### Scenario: 运行中修改配置
- **GIVEN** RecompositionProfiler 已启用
- **WHEN** 开发者调用 `configure {}` 修改配置
- **THEN** 新配置 SHALL 在下一次重组帧开始时生效

### Requirement: TrackRecomposition 包装器（兼容保留）
系统 SHALL 保留 `TrackRecomposition` Composable 函数，供特定场景手动标记追踪。

#### Scenario: 手动追踪 Composable
- **GIVEN** RecompositionProfiler 已启用
- **WHEN** 开发者编写 `TrackRecomposition("MyComponent") { MyComponent() }`
- **THEN** 系统 SHALL 以 "MyComponent" 为名称追踪该 Composable 的重组事件

#### Scenario: Profiler 未启用时无开销
- **GIVEN** RecompositionProfiler 未启用
- **WHEN** `TrackRecomposition` 包装器执行
- **THEN** SHALL 直接调用内部 content，不执行任何追踪逻辑

**注：** 主要追踪方式已切换为 CompositionTracer 自动全覆盖，`TrackRecomposition` 保留供需要自定义名称等特定场景使用。

### Requirement: 分析报告获取
系统 SHALL 提供 API 获取结构化的重组分析报告，报告中 SHALL 同时包含当前生效的业务自定义过滤配置快照（`filteredNames` 和 `filteredPrefixes`）。

#### Scenario: 获取当前报告
- **GIVEN** RecompositionProfiler 已启用且已收集数据
- **WHEN** 开发者调用 `RecompositionProfiler.getReport()`
- **THEN** 系统 SHALL 返回 `RecompositionReport` 对象，包含 sessionId、durationMs、totalFrames、totalRecompositions、composables（按重组次数降序）、hotspots、stateChanges、**filteredNames**、**filteredPrefixes**

#### Scenario: 重置数据
- **GIVEN** RecompositionProfiler 已启用
- **WHEN** 开发者调用 `RecompositionProfiler.reset()`
- **THEN** 系统 SHALL 清空已采集的所有事件数据，后续报告从零开始统计
- **AND** 过滤配置 SHALL 保留不受影响（reset 只清数据，不清配置）

#### Scenario: 未启用时获取报告
- **GIVEN** RecompositionProfiler 未启用
- **WHEN** 开发者调用 `RecompositionProfiler.getReport()`
- **THEN** 系统 SHALL 返回一个空的 `RecompositionReport`（所有计数为 0），`filteredNames` 和 `filteredPrefixes` 反映当前配置快照，不抛出异常

### Requirement: 输出策略管理
系统 SHALL 提供 API 管理输出策略的注册和激活。

#### Scenario: 注册输出策略
- **GIVEN** RecompositionProfiler 初始化
- **WHEN** 开发者调用 `RecompositionProfiler.addOutputStrategy(LogOutputStrategy())`
- **THEN** 系统 SHALL 将该策略加入活跃策略列表，后续帧完成时将事件分发给该策略

#### Scenario: 移除输出策略
- **GIVEN** 某输出策略已注册
- **WHEN** 开发者调用 `RecompositionProfiler.removeOutputStrategy(strategy)`
- **THEN** 系统 SHALL 移除该策略，后续事件不再分发给该策略

### Requirement: ProfilerLifecycleListener 机制
系统 SHALL 提供生命周期监听机制，使已创建的 Scene 能响应 Profiler 启停。

#### Scenario: Scene 响应 Profiler 启动
- **GIVEN** BaseComposeScene 已注册为 ProfilerLifecycleListener
- **WHEN** 开发者调用 `RecompositionProfiler.start()`
- **THEN** Scene SHALL 收到 `onProfilerStarted(tracker)` 回调，注册 CompositionObserver 到其 Composition

#### Scenario: Scene 响应 Profiler 停止
- **GIVEN** BaseComposeScene 已注册为 ProfilerLifecycleListener 且 CompositionObserver 已注册
- **WHEN** 开发者调用 `RecompositionProfiler.stop()`
- **THEN** Scene SHALL 收到 `onProfilerStopped()` 回调，dispose CompositionObserverHandle

### Requirement: 线程安全
所有 RecompositionProfiler 的公开 API SHALL 保证线程安全。

#### Scenario: 并发调用安全
- **GIVEN** RecompositionProfiler 已启用
- **WHEN** 多个线程同时调用 `getReport()`、`start()`、`stop()` 等 API
- **THEN** 系统 SHALL 不抛出并发异常，不产生数据损坏
