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
