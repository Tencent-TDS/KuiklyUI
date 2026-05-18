## ADDED Requirements

### Requirement: 日志输出模式
系统 SHALL 支持将重组分析数据通过日志形式输出，供开发者在控制台查看。

#### Scenario: 日志输出重组事件
- **GIVEN** RecompositionProfiler 已启用且 Log 输出策略已激活
- **WHEN** 一次重组帧完成
- **THEN** 系统 SHALL 通过 `println` 输出包含以下信息的结构化日志：帧 ID、帧内重组计数、发生重组的 Composable 名称（含源码位置）、各自的重组耗时、精确的触发 State（如 `MutableState(value=3)@128220496`）、父 Composable 名称

#### Scenario: 日志格式可读性
- **GIVEN** Log 输出策略已激活
- **WHEN** 输出重组日志
- **THEN** 日志 SHALL 使用统一的 Tag（`[RecompositionProfiler]`），并以人类可读的缩进格式展示嵌套关系

#### Scenario: 跨平台日志一致性
- **GIVEN** Log 输出策略在 Android/iOS/HarmonyOS/Web/macOS 任一平台激活
- **WHEN** 输出重组日志
- **THEN** 日志内容格式 SHALL 在所有平台保持一致

### Requirement: UI Overlay 输出模式
系统 SHALL 支持在应用界面上叠加半透明的重组可视化层，实时显示各组件的重组热度。

#### Scenario: Overlay 显示重组热力图
- **GIVEN** RecompositionProfiler 已启用且 UI Overlay 输出策略已激活
- **WHEN** Composable 发生重组
- **THEN** 系统 SHALL 在该 Composable 对应的区域叠加颜色编码的高亮，颜色从冷色（低频重组）到暖色（高频重组）渐变

#### Scenario: Overlay 显示重组计数
- **GIVEN** UI Overlay 输出策略已激活
- **WHEN** 一个被追踪的 Composable 发生重组
- **THEN** Overlay SHALL 在该组件区域显示当前的重组累计次数数字

#### Scenario: Overlay 不阻挡交互
- **GIVEN** UI Overlay 已显示
- **WHEN** 用户触摸屏幕
- **THEN** 触摸事件 SHALL 穿透 Overlay 层传递到下层 UI 组件，Overlay 不阻挡正常交互

#### Scenario: Overlay 可动态开关
- **GIVEN** 应用正在运行
- **WHEN** 开发者调用 `RecompositionProfiler.setOverlayEnabled(false)`
- **THEN** Overlay SHALL 立即隐藏，不影响其他输出模式

### Requirement: JSON 结构化数据输出模式
系统 SHALL 支持将重组分析数据以 JSON 结构化格式输出，供 AI Agent 或外部工具消费。

#### Scenario: JSON 报告生成
- **GIVEN** RecompositionProfiler 已启用且收集了重组数据
- **WHEN** 调用 `RecompositionProfiler.getReport()`
- **THEN** 系统 SHALL 返回 `RecompositionReport` 对象，包含：
  - `sessionId`: 分析会话标识
  - `durationMs`: 追踪持续时间
  - `totalRecompositions`: 总重组次数
  - `composables`: 按 Composable 聚合的重组统计列表
  - `hotspots`: 热点组件列表
  - `stateChanges`: State 变更时间线

#### Scenario: JSON 序列化
- **GIVEN** 已获取 `RecompositionReport` 对象
- **WHEN** 调用 `report.toJson()`
- **THEN** 系统 SHALL 返回有效的 JSON 字符串，可被标准 JSON 解析器解析

#### Scenario: AI Agent 可消费格式
- **GIVEN** JSON 报告已生成
- **WHEN** AI Agent 解析报告
- **THEN** JSON 结构 SHALL 包含足够的语义信息（字段名自解释、包含单位标注），使 AI 无需额外上下文即可理解数据含义

### Requirement: 输出策略可组合
系统 SHALL 支持同时激活多种输出策略。

#### Scenario: 多策略并行
- **GIVEN** RecompositionProfiler 已启用
- **WHEN** 同时激活 Log 和 JSON 输出策略
- **THEN** 系统 SHALL 同时将数据输出到日志和 JSON 报告，二者互不干扰

#### Scenario: 无输出策略时静默工作
- **GIVEN** RecompositionProfiler 已启用但未激活任何输出策略
- **WHEN** 发生重组
- **THEN** 系统 SHALL 仍然采集数据（可通过 `getReport()` 后续获取），但不主动输出
