# vforLazy 顶部校正稳定性

## Purpose

定义 `vforLazy` 在顶部占位不足、聊天前插和用户主动拖拽交错时的稳定性约束，避免框架在拖拽过程中通过动画 offset 校正抢夺滚动控制权，同时保留非主动拖拽路径的连续性补偿能力。

## Requirements

### Requirement: 主动拖拽中延后顶部校正
当 `vforLazy` 在滚动中发现顶部占位空间不足，且当前滚动由用户主动拖拽驱动时，系统 SHALL 记录待校正状态并把实际 offset 补偿延后到滚动结束后处理，而不是在拖拽过程中立即发起动画 `setContentOffset(...)`。

#### Scenario: 连续低速拖拽逼近顶部
- **GIVEN** 列表启用了 `vforLazy`
- **AND** 用户正在主动拖拽列表，`ScrollParams.isDragging = true`
- **AND** `LazyLoopDirectivesView` 计算出顶部占位空间不足
- **WHEN** 本次 layout 触发顶部校正分支
- **THEN** 系统 SHALL 只记录待校正的顶部占位信息
- **AND** SHALL 注册一次延后的 `scrollEnd` 校正任务
- **AND** SHALL NOT 在当前拖拽过程中调用动画 `setContentOffset(...)` 接管滚动

#### Scenario: 拖拽结束后补偿顶部 offset
- **GIVEN** 拖拽过程中已经记录了顶部待校正状态
- **WHEN** 列表进入 `scrollEnd`
- **THEN** 系统 SHALL 根据最终顶部占位差值执行一次 offset 补偿
- **AND** 补偿后首个可见内容锚点 SHALL 与拖拽结束时保持连续，不得出现额外的突发加速效果

### Requirement: 非主动拖拽场景允许动画校正
当 `vforLazy` 发现顶部占位空间不足，但当前滚动不处于用户主动拖拽阶段时，系统 SHALL 可以继续使用现有动画校正策略，以避免顶部出现长时间空白或明显停顿。

#### Scenario: 惯性滚动中顶部占位不足
- **GIVEN** 列表仍在滚动，但 `ScrollParams.isDragging = false`
- **AND** 顶部占位空间不足
- **WHEN** `LazyLoopDirectivesView` 进入顶部校正分支
- **THEN** 系统 SHALL 允许设置滚动过滤规则并发起一次动画 `setContentOffset(...)`
- **AND** 本次动画校正 SHALL 以恢复内容连续性为目标，而不是等待到 `scrollEnd` 才开始处理

#### Scenario: 程序滚动后的顶部补偿
- **GIVEN** 列表通过程序调用触发滚动或定位
- **AND** 当前不处于主动拖拽状态
- **WHEN** 需要修正顶部占位误差
- **THEN** 系统 SHALL 允许沿用非拖拽路径的 offset 校正逻辑
- **AND** SHALL NOT 因为本次 capability 一律禁用所有顶部动画校正

### Requirement: 聊天前插场景保持可见锚点连续
在使用 `vforLazy` 实现聊天消息向顶部前插的场景中，系统 SHALL 支持在触顶加载后保持当前首个可见消息锚点连续，并允许继续上划触发后续前插。

#### Scenario: 触顶加载并前插消息
- **GIVEN** 列表顶部存在加载触发区
- **AND** 用户滚动到顶部附近触发前插加载
- **WHEN** Demo 页面向列表头部插入一批新消息
- **THEN** 列表 SHALL 保持插入前的首个可见消息仍然处于可见位置
- **AND** 新插入的消息 SHALL 位于该锚点之前
- **AND** 用户无需手动回拉即可继续向上滚动

#### Scenario: 同一次触顶只触发一次前插
- **GIVEN** 用户已经在顶部触发了一次前插加载
- **WHEN** 用户仍停留在顶部触发区内持续滚动或松手
- **THEN** Demo 验证逻辑 SHALL 锁定重复前插
- **AND** 只有在用户明显离开顶部触发区后，下一次触顶才 SHALL 再次触发前插
