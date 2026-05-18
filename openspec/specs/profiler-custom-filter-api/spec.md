## ADDED Requirements

### Requirement: 按名字排除 Composable
系统 SHALL 提供 `RecompositionProfiler.excludeByName(names: List<String>)` API，允许业务侧通过精确名称批量排除 Composable 不被追踪。

#### Scenario: 追加排除名称
- **GIVEN** `RecompositionProfiler` 处于任意状态（运行中或已停止）
- **WHEN** 开发者调用 `RecompositionProfiler.excludeByName(listOf("MyBaseButton", "CommonLoading"))`
- **THEN** 系统 SHALL 将这些名称追加到内部 `excludedNames` Set（不替换已有名称）
- **AND** 如果 Profiler 运行中，SHALL 立即通过 `KLog.i("RCProfiler", ...)` 输出当前完整排除列表日志
- **AND** 后续帧中名称匹配的 Composable SHALL 不出现在面板和日志中

#### Scenario: 追加语义 — 不替换已有
- **GIVEN** 已调用 `excludeByName(listOf("A"))` 且 Profiler 运行中
- **WHEN** 再次调用 `excludeByName(listOf("B"))`
- **THEN** `excludedNames` SHALL 包含 `{A, B}`，不只有 `{B}`

#### Scenario: 幂等 — 重复名称不重复添加
- **GIVEN** 已调用 `excludeByName(listOf("MyBaseButton"))`
- **WHEN** 再次调用 `excludeByName(listOf("MyBaseButton"))`
- **THEN** `excludedNames` SHALL 仍只包含一个 `"MyBaseButton"`（Set 语义）

#### Scenario: Profiler 未运行时调用
- **GIVEN** `RecompositionProfiler` 未启动
- **WHEN** 调用 `excludeByName(listOf("MyWidget"))`
- **THEN** 系统 SHALL 保存配置，不抛出异常，不输出日志
- **AND** 下次 `start()` 后 SHALL 应用该配置

### Requirement: 按前缀排除 Composable
系统 SHALL 提供 `RecompositionProfiler.excludeByPrefix(prefixes: List<String>)` API，允许业务侧通过包名前缀批量排除 Composable。

#### Scenario: 追加排除前缀
- **GIVEN** `RecompositionProfiler` 运行中
- **WHEN** 开发者调用 `excludeByPrefix(listOf("com.myapp.foundation.", "com.myapp.common."))`
- **THEN** 系统 SHALL 将这些前缀追加到内部 `excludedPrefixes` Set
- **AND** SHALL 立即通过 `KLog.i("RCProfiler", ...)` 输出当前完整排除列表日志
- **AND** 后续帧中 info 字段以这些前缀开头的 Composable SHALL 不出现在面板和日志中

#### Scenario: 前缀匹配规则
- **GIVEN** 已调用 `excludeByPrefix(listOf("com.myapp.foundation."))`
- **WHEN** 追踪到 `info = "com.myapp.foundation.BaseButton (BaseButton.kt:42)"`
- **THEN** 该 Composable SHALL 被过滤，不产生任何事件

### Requirement: 清空所有业务自定义过滤
系统 SHALL 提供 `RecompositionProfiler.clearCustomFilters()` API，清空全部业务自定义排除规则。

#### Scenario: 清空过滤
- **GIVEN** 已通过 `excludeByName` 或 `excludeByPrefix` 添加了过滤规则
- **WHEN** 调用 `RecompositionProfiler.clearCustomFilters()`
- **THEN** `excludedNames` 和 `excludedPrefixes` SHALL 均被清空
- **AND** SHALL 立即通过 `KLog.i("RCProfiler", "Custom filter cleared")` 输出日志
- **AND** 后续帧中之前被排除的 Composable SHALL 重新出现在面板和日志中

#### Scenario: 清空不影响内置框架过滤
- **GIVEN** 内置过滤（`enableBuiltinFilters = true`）处于启用状态
- **WHEN** 调用 `clearCustomFilters()`
- **THEN** 内置框架过滤 SHALL 继续生效，不受影响

### Requirement: 过滤操作日志输出
系统 SHALL 在过滤配置变更时通过 `KLog` 输出日志，日志 Tag 为 `RCProfiler`。

#### Scenario: 添加过滤时日志格式
- **GIVEN** `RecompositionProfiler` 运行中，已有排除名称 `["A"]` 和排除前缀 `["com.x."]`
- **WHEN** 调用 `excludeByName(listOf("B"))`
- **THEN** SHALL 输出格式为：
  `[RCProfiler] Custom filter updated — names: [A, B], prefixes: [com.x.]`

#### Scenario: 清空时日志格式
- **WHEN** 调用 `clearCustomFilters()`
- **THEN** SHALL 输出：`[RCProfiler] Custom filter cleared`
