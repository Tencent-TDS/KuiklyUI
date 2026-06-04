## Context

Compose DSL 的语义树（Semantics Tree）用于无障碍访问和自动化测试。`SemanticsNode` 的构造依赖 `LayoutNode.nodes.head(Nodes.Semantics)` 和 `LayoutNode.collapsedSemantics` 两个条件同时非空。

在业务页面 `TdsStudioPage` 中，大量使用 `clickable`、`focusable`、`TextField` 等组件会频繁触发 `FocusableNode`（`SemanticsModifierNode` 子类）的添加与移除。日志分析揭示了一个稳定的崩溃链路：

1. `NodeChain.updateFrom()` 触发 `padChain()`，插入 `SentinelHead` 后 `head` 属性指向旧 head
2. `structuralUpdate()` → `executeDiff()` 依次移除旧 head 和 Semantics 节点
3. `detachAndRemoveNode(FocusableNode)` 中先调用 `autoInvalidateRemovedNode()`，同步触发语义树遍历
4. 此时 `head` 仍是 dangling pointer（已移除的旧 head），`headToTail` 遍历立即中断，返回 null
5. `aggregateChildKindSet` 也尚未更新，`has(Semantics)=true` 但 `head(Semantics)=null`
6. `!!.node` 抛出 NPE

## Goals / Non-Goals

**Goals:**
- 消除 `NodeChain.removeNode()` 导致的 `head` 指针漂移问题
- 在语义树遍历层增加防御性安全网，确保即使存在极窄的不一致窗口也不 crash
- 保持语义树变更通知的实时性（不引入延迟队列）
- 改动范围严格限定在 `compose/ui/node/` 和 `compose/ui/semantics/` 包内

**Non-Goals:**
- 不重构 `NodeChain.updateFrom()` 的整体 diff 流程
- 不修改 `autoInvalidateRemovedNode()` 的触发时机或语义
- 不引入新的并发原语或状态队列
- 不修改 `core/` 或各平台 render 层

## Decisions

### 1. 在 `removeNode()` 中同步修正 `head` 指针

**选择**: 当 `head === node` 时，将 `head` 更新为 `child ?: tail`

**理由**:
- 根因是 `padChain()` 插入 SentinelHead 后 `head` 不再反映链表实际起点，`removeNode()` 断开旧 head 但未同步指针
- 在移除点同步修正 `head` 是最小侵入的修复，不影响其他调用路径
- `tail` 作为 fallback 确保链表至少有一个有效节点

**替代方案**: 在 `padChain()` 中立即更新 `head = SentinelHead`。但被舍弃，因为 `SentinelHead` 是临时占位节点，会在 `trimChain()` 时被移除，提前更新 `head` 会导致后续 diff 逻辑依赖临时节点。

### 2. 语义树遍历层不再信任 `has()`，改用 `head()` 实际遍历结果

**选择**: `fillOneLayerOfSemanticsWrappers` 中不再检查 `has(Nodes.Semantics)`，直接调用 `head(Nodes.Semantics)`，以 null 判断决定走语义节点分支还是递归分支

**理由**:
- `has()` 依赖 `aggregateChildKindSet` 位掩码，`head()` 依赖实际链表遍历。两者更新时机不同步
- `head()` 是事实来源（source of truth），以它为准可以彻底消除 `has=true` 但 `head=null` 的不一致窗口
- 即使 `head` 指针修复后，`aggregateChildKindSet` 仍可能在 `syncAggregateChildKindSet()` 调用前存在一帧的滞后

**替代方案**: 在 `removeNode()` 中同步更新 `aggregateChildKindSet`。被舍弃，因为需要 O(n) 重新计算，批量 diff 时性能开销大。

### 3. `SemanticsNode` 工厂函数改为 nullable 返回

**选择**: `SemanticsNode(layoutNode, mergingEnabled)` 返回 `SemanticsNode?`，所有调用点安全解包

**理由**:
- 彻底消除 `!!` 强解引用这个 crash 触发点
- 即使未来出现新的时序竞争路径，也不会再因同一原因 crash
- 调用点仅有 3 处（`SemanticsOwner` 和 `fillOneLayerOfSemanticsWrappers`），改动面可控

**替代方案**: 在工厂函数内加 try-catch 返回 fallback `SemanticsNode`。被舍弃，因为构造一个无意义的 SemanticsNode 会污染语义树，可能导致无障碍信息错误。

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| `head` 同步逻辑在复杂链表操作下引入新的指针不一致 | 仅处理 `head === node` 这一种情况，逻辑简单；`tail` fallback 保证链表始终有效 |
| `SemanticsNode` 返回 nullable 后，调用点遗漏 null 处理导致静默跳过 | 所有调用点在同一变更中同步修改，编译器会检查 exhaustiveness |
| `fillOneLayerOfSemanticsWrappers` 不再用 `has()` 可能轻微影响性能 | `head()` 也是 O(n) 遍历，但 n 为单个 LayoutNode 的 modifier 链长度，通常 <= 10，影响可忽略 |
| 变更仅覆盖 `compose/ui/semantics/` 包内调用点，外部可能有隐藏调用 | 搜索确认 `SemanticsNode(layoutNode, mergingEnabled)` 的调用点仅有 3 处，全部在包内 |

## Migration Plan

无需迁移。本次为纯 bugfix，不修改公共 API 或 DSL 语法。业务项目只需升级 `compose` 模块版本即可。

## Open Questions

- 是否需要同步修复 `NodeChain.insertNode()` 中的 `head` 指针更新？当前分析显示 `insertNode` 不会导致 dangling head，但需留意。
