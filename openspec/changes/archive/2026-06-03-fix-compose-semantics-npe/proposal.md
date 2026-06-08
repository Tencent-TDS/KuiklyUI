## Why

Compose DSL 的语义树遍历在 `NodeChain` 移除 `SemanticsModifierNode`（如 `FocusableNode`）时，因 `head` 指针漂移和 `aggregateChildKindSet` 位掩码更新不同步，导致 `has(Semantics)=true` 但 `head(Semantics)=null` 的不一致状态，进而触发 `!!.node` NPE。该问题在业务页面大量使用 `clickable`、`TextField` 等可交互组件时稳定复现。

## What Changes

- **修复 `NodeChain.removeNode()` 中的 `head` 指针漂移**：在移除节点时同步更新 `head` 属性，避免 `headToTail` 遍历从 dangling pointer 出发导致遍历中断。
- **防御性重构 `SemanticsNode` 工厂函数**：将 `SemanticsNode(layoutNode, mergingEnabled)` 改为 nullable 返回，消除 `!!` 强解引用。
- **重构 `fillOneLayerOfSemanticsWrappers`**：不再依赖 `has()` 缓存位掩码，直接以 `head()` 实际遍历结果判断语义节点存在性。
- **同步修复 `SemanticsOwner.unmergedRootSemanticsNode`**：将 `!!.node` 改为安全访问，与工厂函数保持一致。

## Capabilities

### New Capabilities

- `compose-semantics-node-stability`: 确保语义树遍历期间 `NodeChain` 的 `head` 指针和 `aggregateChildKindSet` 位掩码始终与链表实际状态保持一致，防止因时序竞争导致的 NPE。

### Modified Capabilities

<!-- 无现有 spec 级别的行为变更，本次为纯实现层修复 -->

## Impact

- **模块**: `compose/`（核心影响），`demo/`（验证用例）
- **平台**: 全平台（Android / iOS / HarmonyOS / Web / miniApp / macOS），因为 `compose/` 是纯 KMP 模块
- **API 变更**: 内部函数 `SemanticsNode(layoutNode, mergingEnabled)` 返回类型从非空改为 nullable，仅影响 `compose/ui/semantics/` 包内调用点
- **风险**: 低。改动为防御性修复，不引入新行为；语义树结构不变，仅消除不一致窗口

## Non-goals

- 不重构 `NodeChain` 的整体 diff 算法或 `updateFrom()` 的调用流程
- 不引入延迟失效通知队列（避免新增并发复杂度）
- 不改写 `autoInvalidateRemovedNode()` 的触发时机
- 不修改 `core/` 或其他 `core-render-*` 模块
