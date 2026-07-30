## ADDED Requirements

### Requirement: 报告包含过滤配置快照
`RecompositionReport` SHALL 包含 `filteredNames` 和 `filteredPrefixes` 两个字段，反映 `getReport()` 调用时刻生效的业务自定义过滤配置。

#### Scenario: 报告包含当前过滤名称列表
- **GIVEN** 已调用 `excludeByName(listOf("MyBaseButton", "CommonLoading"))`
- **WHEN** 调用 `RecompositionProfiler.getReport()`
- **THEN** 返回的 `RecompositionReport.filteredNames` SHALL 为 `["MyBaseButton", "CommonLoading"]`

#### Scenario: 报告包含当前过滤前缀列表
- **GIVEN** 已调用 `excludeByPrefix(listOf("com.myapp.foundation."))`
- **WHEN** 调用 `RecompositionProfiler.getReport()`
- **THEN** 返回的 `RecompositionReport.filteredPrefixes` SHALL 为 `["com.myapp.foundation."]`

#### Scenario: 无自定义过滤时报告字段为空列表
- **GIVEN** 未调用任何 `excludeByName` / `excludeByPrefix`
- **WHEN** 调用 `getReport()`
- **THEN** `filteredNames` 和 `filteredPrefixes` SHALL 均为空列表 `[]`

#### Scenario: 报告 JSON 包含过滤字段
- **GIVEN** `filteredNames = ["MyBaseButton"]`，`filteredPrefixes = ["com.myapp."]`
- **WHEN** 调用 `report.toJson()`
- **THEN** 输出的 JSON SHALL 包含：
  ```json
  "filteredNames": ["MyBaseButton"],
  "filteredPrefixes": ["com.myapp."]
  ```

## MODIFIED Requirements

### Requirement: 获取当前报告

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
