## Context

重组 Profiler（`RecompositionProfiler`）已在 Phase 1/2 中引入了 `ComposableFilter` 接口体系和 `FilterChain`，Phase 3 在 `RecompositionProfiler` 上暴露了 `addCustomFilter(ComposableFilter)` 等对象级 API，但**尚未提交**。

业务使用场景是：开发者在调试时看到面板/日志被公共基础组件（`BaseButton`、`LoadingSpinner` 等）刷屏，希望能一键屏蔽这些组件，聚焦真正的业务逻辑热点。

**适用 DSL**：Compose DSL 专属（`compose/` 模块）

## Goals / Non-Goals

**Goals:**
- 提供极简字符串 list API，业务侧无需了解 `ComposableFilter` 接口
- Overlay 面板支持直接从热点列表点击过滤
- 操作时输出一行日志，报告中包含过滤配置快照

**Non-Goals:**
- 不记录每帧实时「被过滤了哪些」
- 不支持过滤规则持久化

## Decisions

### Decision 1：Profiler 层维护可变 Set，不下沉到 FilterChain

**选择**：`RecompositionProfiler` 内部维护 `excludedNames: MutableSet<String>` 和 `excludedPrefixes: MutableSet<String>`，每次变更后重建 `customFilters` 列表传给 `RecompositionConfig`。

**原因**：
- 调用方只传字符串，无需关心 `ExclusionComposableFilter` / `PrefixComposableFilter` 的存在
- FilterChain 层无需改动，追加语义在 Profiler 层实现
- 对比让 filter 对象本身可变：可变对象与 FilterChain 的缓存机制有冲突（缓存基于 info 字符串，filter 变了但缓存 key 不变会产生错误结果）

**代价**：每次追加都触发 `FilterChain` 缓存重建（`clearCache()`），但这是低频操作（用户主动触发），可接受。

### Decision 2：移除 Phase 3 遗留的对象级 API

**选择**：将 `addCustomFilter(ComposableFilter)` / `removeCustomFilter` / `updateFilterConfig` / `getCustomFilters` / `getFilterConfig` 全部移除（它们尚未提交，无需 deprecation）。

**原因**：两套 API 并存会造成状态不一致——对象级 API 和字符串 Set 都能修改 `customFilters`，但字符串 Set 是 Profiler 层维护的 source of truth，对象级 API 会绕过它。

### Decision 3：过滤日志由 Profiler 直接 KLog，不走 OutputStrategy

**选择**：在 `excludeByName` / `excludeByPrefix` / `clearCustomFilters` 内部直接调用 `KLog.i(TAG, ...)`。

**原因**：过滤是配置操作，不是帧事件，不属于 OutputStrategy 的职责域。OutputStrategy 处理「采集到的数据输出」，而过滤配置变更是「控制面板」行为。

### Decision 4：Report 中过滤列表是当前快照，不是历史累计

**选择**：`getReport()` 时从当前 `excludedNames` / `excludedPrefixes` 读取，反映「此刻生效的过滤配置」。

**原因**：开发者关心的是「我现在屏蔽了什么」，历史变更记录增加复杂度但实用价值低。

### Decision 5：Overlay 的「过滤」回调通过 lambda 透传

**选择**：`ProfilerExpandedPanel` 接收 `onFilterByName: (String) -> Unit` 和 `onClearFilters: () -> Unit` 两个 lambda，调用 `RecompositionProfiler.excludeByName` / `clearCustomFilters`。

**原因**：Overlay UI 不直接持有 Profiler 引用，通过 lambda 保持解耦，方便测试。

### Decision 6：overlayFilterDepth 机制防止 Overlay 无限重组

**选择**：在 `RecompositionTracker` 中引入 `overlayFilterDepth: Int` 计数器。当进入 Overlay 的 Composable 树时深度 +1，退出时 -1。当 `overlayFilterDepth > 0` 时，完全跳过当前 Composable 的 tracing（不记录、不触发过滤判断）。

**原因**：Overlay 面板本身会产生大量重组（UI 刷新、状态读取），如果被 Profiler 追踪，会形成「追踪 → 数据变化 → Overlay 刷新 → 追踪」的无限循环。之前尝试通过框架前缀过滤 Overlay 组件，但无法覆盖匿名 lambda 和嵌套子组件。`overlayFilterDepth` 从源头切断，按 Composable 树深度整体跳过。

### Decision 7：被过滤项在 Overlay 中隐藏而非灰显，底部 chip 可恢复

**选择**：被过滤的热点从 hotspot 列表中完全隐藏，在面板底部以紧凑的 chip 形式展示（显示名称 + ✕ 按钮）。点击 ✕ 调用 `RecompositionProfiler.removeExcludedName()` 恢复该项。

**原因**：
- 灰显方案会占用列表空间，过滤项多时挤压有效内容
- chip 方案让用户一目了然「当前过滤了什么」，且支持单项恢复（比只有「清空全部」更灵活）
- 过滤状态从 `RecompositionProfiler.isNameExcluded()` / `getExcludedNames()` 读取（而非本地 `remember`），确保 Overlay 与 Profiler 状态一致

### Decision 8：框架过滤前缀全面扩充

**选择**：`FilterChain.frameworkPrefixes` 从最初的基础列表扩充为包含 material3、animation、runtime、profiler 等前缀，并新增 `frameworkNamePatterns` 用于非前缀匹配模式（如匿名 lambda、内部实现类名）。

**原因**：实际测试中发现大量 Compose 框架内部组件（Material3 主题、动画系统、Runtime 调度等）出现在热点列表中，干扰业务分析。全面的框架过滤是"聚焦业务热点"的基础。

## Risks / Trade-offs

- [风险] `FilterChain` 缓存在每次 `excludeByName` 时被重建 → 缓和：这是低频用户操作，性能可接受；缓存重建后下一帧会重新填充
- [风险] 用户从 Overlay 频繁点击过滤（快速连续点多个热点）会触发多次 `updateConfig` → 缓和：`synchronized(lock)` 保证线程安全；可在未来优化为批量提交
- [风险] Overlay 自身产生的重组会形成无限循环 → 缓和：`overlayFilterDepth` 机制从 Composable 树深度整体跳过 Overlay 节点及其所有子节点，从源头切断循环
- [Trade-off] 移除对象级 API 意味着无法表达「正则过滤」等复杂逻辑 → 可在未来需要时通过 `configure { customFilters = ... }` 低层 API 追加，不影响主流程

## File Changes

**`compose/` 模块**：
- `profiler/RecompositionProfiler.kt` — 主改动：新增 API（`excludeByName`/`excludeByPrefix`/`clearCustomFilters`/`removeExcludedName`/`getExcludedNames`/`isNameExcluded`）+ 内部 Set + 移除遗留 API
- `profiler/RecompositionReport.kt` — 新增 `filteredNames` / `filteredPrefixes` 字段
- `profiler/RecompositionTracker.kt` — 新增 `overlayFilterDepth` 机制，防止 Overlay 无限重组
- `profiler/filter/FilterChain.kt` — 扩充 `frameworkPrefixes` + 新增 `frameworkNamePatterns`
- `profiler/output/ProfilerOverlay.kt` — 过滤交互：hotspot 列表隐藏已过滤项，底部 chip 展示 + ✕ 恢复
- `profiler/output/OverlayOutputStrategy.kt` — 无需改动（过滤逻辑不在此）

## Open Questions

无。方案已在 explore 阶段与开发者对齐。
