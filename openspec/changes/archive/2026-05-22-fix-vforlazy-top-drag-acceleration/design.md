## Context

本次变更来自 `demo/` 中 `VforLazyExamplePage` 的聊天前插验证场景。页面在接近顶部时会触发前插加载，并在插入新消息后通过 `scrollToPosition` 保持当前可见锚点。这个场景会频繁触发 `vforLazy` 的窗口切换、顶部占位估算和 offset 校正，是检验滚动稳定性的高压路径。

问题的根因不在 Demo 页面本身，而在 `core/` 的 `LazyLoopDirectivesView`。当 `correctScrollOffsetInLayout()` 发现顶部占位空间不足时，`handleStartSpaceNotEnoughInLayout()` 过去会直接发起动画 `setContentOffset(...)`，试图平滑修正 offset。这个策略在惯性滚动里通常成立，但在用户连续低速拖拽时会导致框架在手指仍然按住屏幕时抢夺滚动控制权，表现为“突然加速滚到顶”。

同时，之前验证过“完全移除动画校正”的方案也有明显副作用：会让非拖拽路径的顶部空白更容易暴露，并把补偿集中到 `scrollEnd`，导致问题从“拖拽中被接管”变成“结束时一次性跳变更大”。因此需要一个只影响主动拖拽路径的更窄设计。

## Goals / Non-Goals

**Goals:**
- 让 `vforLazy` 在主动拖拽到顶部时不再通过动画 offset 校正打断手势。
- 保留非主动拖拽路径的原有校正能力，避免顶部长时间露白。
- 让聊天前插 Demo 能稳定覆盖“触顶加载、保持锚点、继续上划”的验证路径。
- 把 `ScrollParams.isDragging` 显式纳入 `LazyLoopDirectivesView` 的顶部校正决策。

**Non-Goals:**
- 不重构 `vforLazy` 的窗口管理、占位估算或平均高度算法。
- 不为 render 层追加新的原生事件或 bridge 协议。
- 不把 Demo 中的前插锁、触顶阈值抽象成框架公共 API。
- 不试图在本次设计里消除所有由动态高度和窗口切换带来的跳变来源。

## Decisions

### Decision 1: 只在主动拖拽路径禁用动画校正

`LazyLoopDirectivesView` 将把 `ScrollParams.isDragging` 透传到 `correctScrollOffsetInLayout()` 的顶部空间不足分支。

- 当 `isDragging = true` 时：
  - 不调用动画 `setContentOffset(...)`
  - 只记录 `itemStart.sizeExtra`
  - 注册 `scrollEnd` 后再补偿 offset
- 当 `isDragging = false` 时：
  - 保留现有过滤规则与动画校正逻辑

**Why this option**
- 它直接对准 root cause，只影响“手指仍在拖拽”的阶段。
- 它避免把已经验证有效的非拖拽校正一并禁掉。

**Alternatives considered**
- **方案 A：完全移除顶部动画校正**
  - 优点：实现最简单，也能消除拖拽中的突发加速
  - 缺点：非拖拽路径会更容易露白，`scrollEnd` 时跳变更集中
  - 结论：放弃
- **方案 B：无论是否拖拽都延后到 `scrollEnd`**
  - 优点：行为统一
  - 缺点：损失现有平滑校正能力，退化所有场景
  - 结论：放弃

### Decision 2: 在延后 layout 路径中保留 dragging 上下文

`LazyLoopDirectivesView` 存在 `needWaitLayout()` 为真的分支，会通过 `registerAfterLayoutTaskFor(...)` 延后执行 `handleOnContentOffsetDidChanged(...)`。如果不把 dragging 状态一并保存，就会在真实拖拽场景里丢失上下文，导致延后执行时误走非拖拽分支。

因此设计上需要把“本次 contentOffset 处理是否源于 dragging”作为 wait state 的一部分持久化，并在 after-layout 回调里恢复。

**Why this option**
- 顶部空间不足常常发生在 layout 与滚动交错的时刻；不保留上下文，分支判断会失真。

**Alternatives considered**
- **方案 A：只在实时滚动回调里判断 dragging**
  - 缺点：延后布局回调无法复用这个状态
  - 结论：不足以覆盖真实路径

### Decision 3: Demo 继续承担聊天前插验收职责

`demo/` 中的 `VforLazyExamplePage` 将继续作为回归场景，保留顶部触发区、前插锁、前插后定位锚点和较大的 `maxLoadItem`。

**Why this option**
- 本次行为问题就是在聊天前插验证中暴露出来的；如果移除这个场景，`core/` 的修复就缺少可靠回归入口。

**Alternatives considered**
- **方案 A：只改 core，不维护 Demo 验证逻辑**
  - 缺点：回归验证成本高，问题难以稳定复测
  - 结论：放弃

## Risks / Trade-offs

- **[风险] 某些平台 render 层对 dragging 状态上报不一致** → **Mitigation**：本次先以 Android 为主验证，同时把 capability 写在 `core/` 层，后续如平台行为不同再补平台特定修订。
- **[风险] 延后到 `scrollEnd` 的补偿仍可能形成轻微跳变** → **Mitigation**：限定只在主动拖拽路径启用，且继续保留非拖拽平滑校正，避免放大影响面。
- **[风险] Demo 验证逻辑与框架逻辑耦合较深** → **Mitigation**：在 proposal/spec 中明确 Demo 只是验收场景，不把这些页面状态视为通用框架 API。

## Migration Plan

1. 在 `core/` 中把 dragging 状态沿 `onContentOffsetDidChanged -> createItemByOffset -> createItemByPosition -> correctScrollOffsetInLayout` 的链路向下传递。
2. 调整顶部空间不足分支：拖拽中只记录待校正状态，非拖拽保留原逻辑。
3. 在 wait-to-apply state 中补充 dragging 标记，确保延后 layout 路径分支判断一致。
4. 保持 `demo/` 的聊天前插回归页面可用，用它验证触顶前插与连续低速拖拽路径。
5. 如回归发现非拖拽路径出现新的露白或跳变，再评估是否需要更细粒度地区分程序滚动与惯性滚动。

**Rollback strategy**
- 若该策略在多平台回归中出现不可接受的顶部露白或新的跳变，可回退到“统一保留动画校正”的旧逻辑，再单独设计平台或场景级细分策略。

## Open Questions

- iOS / HarmonyOS / Web 的 render 层在顶部校正场景下，对 `isDragging` 的上报时序是否与 Android 一致？
- 当前 Demo 的 `TOP_INSERT_TRIGGER_OFFSET` 与 `maxLoadItem = 100` 是否应在后续抽象为更明确的聊天场景推荐值，还是继续保留为 Demo 内部调参？
