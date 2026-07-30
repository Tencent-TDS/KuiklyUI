## ADDED Requirements

### Requirement: 重组事件自动采集（CompositionTracer）
系统 SHALL 通过 `CompositionTracer`（`@InternalComposeTracingApi`）自动接收编译器注入的 `traceEventStart/End` 回调，零侵入覆盖所有 @Composable 函数的重组追踪。

#### Scenario: CompositionTracer 自动追踪
- **GIVEN** RecompositionProfiler 已启用
- **WHEN** 任意 @Composable 函数发生重组（非 skip 路径）
- **THEN** 系统 SHALL 通过编译器注入的 `traceEventStart(key, dirty1, dirty2, info)` / `traceEventEnd()` 回调自动记录 `ComposableRecomposedEvent`，包含 composableName（函数名 + 源码位置，如 `"CounterSection (Demo.kt:195)"`）、重组耗时（durationMs）、父 Composable 名称（parentName）

#### Scenario: 重组帧边界追踪
- **GIVEN** RecompositionProfiler 已启用
- **WHEN** ComposeSceneRecomposer 执行一次重组帧（调用 `performScheduledTasks`）
- **THEN** 系统 SHALL 记录一个 `RecompositionFrameStartEvent` 和 `RecompositionFrameEndEvent`，包含帧开始/结束时间戳、帧 ID、帧内重组计数

#### Scenario: 追踪关闭时零开销
- **GIVEN** RecompositionProfiler 未启用（`isEnabled = false`）
- **WHEN** 任意 Composable 发生重组
- **THEN** 系统 SHALL NOT 执行任何事件采集逻辑，CompositionTracer 的 `traceEventStart/End` 回调直接短路返回

### Requirement: 精确重组原因追踪（CompositionObserver）
系统 SHALL 通过 `CompositionObserver`（`@ExperimentalComposeRuntimeApi`）的 `invalidationMap` 实现精确的 scope→State 关联。

#### Scenario: 精确 State→Composable 关联
- **GIVEN** RecompositionProfiler 已启用且 CompositionObserver 已注册
- **WHEN** 某个 `MutableState` 的值发生变更并触发特定 Composable 的重组
- **THEN** 该 Composable 的 `ComposableRecomposedEvent.triggerStates` SHALL 仅包含直接导致此 Composable 重组的 State 对象标识，而非帧内所有 State 变更

#### Scenario: Fallback 粗粒度关联
- **GIVEN** RecompositionProfiler 已启用但 CompositionObserver 未注册或不可用
- **WHEN** 某个 Composable 发生重组
- **THEN** `triggerStates` SHALL fallback 到帧级别的粗粒度关联（当前帧内所有 State 变更）

#### Scenario: Forced recomposition 识别
- **GIVEN** CompositionObserver 已注册且 `invalidationMap` 中某 scope 对应的 value 为 null
- **WHEN** 该 scope 对应的 Composable 发生重组
- **THEN** `triggerStates` SHALL 包含 `"[forced recomposition]"` 标识

### Requirement: 框架 Composable 过滤
系统 SHALL 支持过滤框架内部 Composable，使开发者聚焦业务代码。

#### Scenario: 默认过滤框架 Composable
- **GIVEN** RecompositionProfiler 已启用且 `includeFrameworkComposables = false`（默认值）
- **WHEN** `androidx.compose.runtime.`、`androidx.compose.ui.`、`com.tencent.kuikly.compose.foundation.` 等框架前缀的 Composable 发生重组
- **THEN** 系统 SHALL NOT 记录这些 Composable 的重组事件

#### Scenario: 包含框架 Composable
- **GIVEN** RecompositionProfiler 已启用且 `includeFrameworkComposables = true`
- **WHEN** 任意 Composable（包括框架内部）发生重组
- **THEN** 系统 SHALL 记录所有 Composable 的重组事件

### Requirement: State 变更记录
系统 SHALL 追踪 Snapshot State 的变更。

#### Scenario: State 变更记录
- **GIVEN** RecompositionProfiler 已启用且 `enableStateTracking = true`
- **WHEN** 一个 `MutableState` 的值发生变更
- **THEN** 系统 SHALL 通过 `Snapshot.registerApplyObserver` 记录 State 变更，包含 state 标识符和变更时间戳

### Requirement: State 身份标识（可选）
系统 SHALL 支持通过配置开启 State 身份追踪，为 State 对象提供稳定短 ID 和读取者 Composable 信息。

#### Scenario: State 身份标识关闭（默认）
- **GIVEN** RecompositionProfiler 已启用且 `enableStateIdentity = false`（默认值）
- **WHEN** 输出 `triggerStates` 信息
- **THEN** 系统 SHALL 使用 State 对象的 `toString()` 作为标识（如 `MutableState(value=3)@128220496`）

#### Scenario: State 身份标识开启
- **GIVEN** RecompositionProfiler 已启用且 `enableStateIdentity = true`
- **WHEN** 输出 `triggerStates` 信息
- **THEN** 系统 SHALL 为每个 State 对象分配稳定递增短 ID（如 `State#1`），并附带读取该 State 的 Composable 名称列表
- **AND** 输出格式 SHALL 为 `State#1(value=3), readers: CounterSection, StatusBar`

#### Scenario: 稳定 ID 在 session 内一致
- **GIVEN** `enableStateIdentity = true`
- **WHEN** 同一个 State 对象在不同帧中多次触发重组
- **THEN** 该 State 的短 ID SHALL 在整个 profiler session 内保持不变（如始终为 `State#1`）

#### Scenario: 读取者 Composable 映射
- **GIVEN** `enableStateIdentity = true`
- **WHEN** 某个 State 被 Composable A 和 Composable B 读取（`state.value`）
- **THEN** 该 State 的 readers 列表 SHALL 包含 A 和 B 的名称

### Requirement: 重组统计聚合
系统 SHALL 对采集的重组事件进行统计聚合，提供按 Composable 维度的汇总数据。

#### Scenario: 重组次数统计
- **GIVEN** RecompositionProfiler 已启用且运行了一段时间
- **WHEN** 开发者请求分析报告
- **THEN** 报告 SHALL 包含每个追踪的 Composable 的总重组次数、平均耗时、最大/最小耗时、关联的触发 State 集合

#### Scenario: 热点组件识别
- **GIVEN** 分析报告已生成
- **WHEN** 某个 Composable 的每秒重组次数超过配置的阈值（`hotspotThreshold`，默认 10 次/秒）
- **THEN** 该 Composable SHALL 被标记为「热点组件」（`isHotspot = true`）

### Requirement: 采样率控制
系统 SHALL 支持配置采样率以控制追踪的数据量。

#### Scenario: 采样率生效
- **GIVEN** RecompositionProfiler 已启用且 `sampleRate = 0.5f`
- **WHEN** 发生 100 次重组帧
- **THEN** 系统 SHALL 大约记录 50 次重组帧的详细数据（允许 ±10% 误差）

#### Scenario: 全量采集
- **GIVEN** RecompositionProfiler 已启用且 `sampleRate = 1.0f`（默认值）
- **WHEN** 发生重组
- **THEN** 系统 SHALL 记录所有重组事件
