## ADDED Requirements

### Requirement: 热点行「过滤」按钮
Overlay 面板的每个热点行 SHALL 在右侧显示「过滤」按钮，点击后将该 Composable 加入排除列表。

#### Scenario: 点击热点过滤按钮
- **GIVEN** Overlay 面板已展开，热点列表不为空
- **WHEN** 用户点击某热点行右侧的「过滤」按钮
- **THEN** 系统 SHALL 调用 `RecompositionProfiler.excludeByName(listOf(item.name))`
- **AND** `KLog` SHALL 输出当前完整过滤列表（由 Profiler 内部触发）
- **AND** 该 Composable 在后续新帧中 SHALL 不再出现在热点列表中
- **AND** 已有的历史统计数据 SHALL 保留（不回撤当前帧计数）

#### Scenario: 按钮布局
- **GIVEN** 热点行展开显示
- **THEN** 「过滤」按钮 SHALL 位于热点行右侧，与重组次数显示并排或位于其右侧
- **AND** 按钮文字 SHALL 为「过滤」
- **AND** 按钮样式 SHALL 与已有控制按钮（暂停/重置/报告）保持一致

### Requirement: 控制栏「清空过滤」按钮
Overlay 控制按钮行 SHALL 新增「清空过滤」按钮。

#### Scenario: 点击清空过滤
- **GIVEN** Overlay 面板已展开，已有业务自定义过滤规则
- **WHEN** 用户点击「清空过滤」按钮
- **THEN** 系统 SHALL 调用 `RecompositionProfiler.clearCustomFilters()`
- **AND** `KLog` SHALL 输出 `Custom filter cleared`

#### Scenario: 无过滤时点击清空
- **GIVEN** 当前没有任何业务自定义过滤规则
- **WHEN** 用户点击「清空过滤」按钮
- **THEN** 系统 SHALL 幂等处理，不报错，不输出日志（无变化则无日志）
