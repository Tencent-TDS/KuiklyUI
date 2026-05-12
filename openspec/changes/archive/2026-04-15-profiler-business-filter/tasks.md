## 1. 清理 Phase 3 遗留代码（compose/）

- [x] 1.1 在 `RecompositionProfiler.kt` 中移除未提交的 Phase 3 遗留 API：`addCustomFilter(ComposableFilter)`、`removeCustomFilter`、`updateFilterConfig`、`getCustomFilters`、`getFilterConfig`
- [x] 1.2 移除 `RecompositionProfiler.kt` 顶部对 `ComposableFilter` 的 import（如不再需要）

## 2. RecompositionProfiler 新增业务过滤 API（compose/）

- [x] 2.1 在 `RecompositionProfiler` object 内新增 `private val excludedNames: MutableSet<String>` 和 `private val excludedPrefixes: MutableSet<String>`（均在 `lock` 保护下访问）
- [x] 2.2 实现 `excludeByName(names: List<String>)` — 将 names 追加到 `excludedNames`，调用内部 `rebuildCustomFilters()`，如 Profiler 运行中则调用 `logFilterUpdated()`
- [x] 2.3 实现 `excludeByPrefix(prefixes: List<String>)` — 将 prefixes 追加到 `excludedPrefixes`，调用内部 `rebuildCustomFilters()`，如 Profiler 运行中则调用 `logFilterUpdated()`
- [x] 2.4 改造 `clearCustomFilters()` — 清空 `excludedNames` 和 `excludedPrefixes`，调用 `rebuildCustomFilters()`，仅当有内容被清空时输出 `KLog.i(TAG, "Custom filter cleared")`
- [x] 2.5 实现私有方法 `rebuildCustomFilters()` — 根据 `excludedNames` 和 `excludedPrefixes` 构建 `customFilters` list（`ExclusionComposableFilter` + `PrefixComposableFilter`），调用 `configure` 更新到 tracker
- [x] 2.6 实现私有方法 `logFilterUpdated()` — 输出 `KLog.i(TAG, "Custom filter updated — names: [...], prefixes: [...]")` 格式日志，列出当前完整 Set 内容
- [x] 2.7 确认 `start()` 调用时正确应用已有的 `excludedNames` / `excludedPrefixes`（通过 `rebuildCustomFilters` 在 start 前生效）

## 3. RecompositionReport 新增过滤快照字段（compose/）

- [x] 3.1 在 `RecompositionReport.kt` 中新增 `filteredNames: List<String> = emptyList()` 和 `filteredPrefixes: List<String> = emptyList()` 两个字段
- [x] 3.2 在 `RecompositionProfiler.getReport()` 中，从 `excludedNames` / `excludedPrefixes` 取快照，传入 `RecompositionReport`
- [x] 3.3 在 `RecompositionReport.toJson()` 中新增 `"filteredNames"` 和 `"filteredPrefixes"` 字段输出

## 4. ProfilerOverlay UI 新增过滤交互（compose/）

- [x] 4.1 在 `ProfilerOverlay.kt` 的 `ProfilerExpandedPanel` 函数签名中新增 `onFilterByName: (String) -> Unit` 和 `onClearFilters: () -> Unit` 两个 lambda 参数
- [x] 4.2 修改 `ProfilerOverlaySlot` 中调用 `ProfilerExpandedPanel` 的位置，传入对应的 lambda：`onFilterByName = { RecompositionProfiler.excludeByName(listOf(it)) }`、`onClearFilters = { RecompositionProfiler.clearCustomFilters() }`
- [x] 4.3 修改 `HotspotRow` 函数签名，新增 `onFilter: (() -> Unit)?` 参数（可空，保持向后兼容）
- [x] 4.4 在 `HotspotRow` 的布局中，热点名称和次数右侧新增「过滤」小按钮（`OverlayControlButton`），点击调用 `onFilter?.invoke()`
- [x] 4.5 在 `ProfilerExpandedPanel` 热点列表渲染循环中，将 `onFilter = { onFilterByName(item.name) }` 传给 `HotspotRow`
- [x] 4.6 在 `ProfilerExpandedPanel` 控制按钮行新增「清空过滤」按钮（`OverlayControlButton`），点击调用 `onClearFilters()`

## 5. Overlay 无限重组防护 & 框架过滤增强（compose/）

- [x] 5.1 在 `RecompositionTracker` 中实现 `overlayFilterDepth` 机制：Overlay 自身的 Composable 及其子节点完全跳过 tracing，防止 Overlay 引发无限重组循环
- [x] 5.2 扩充 `FilterChain.frameworkPrefixes`：新增 material3、animation、runtime、profiler 前缀，以及 `frameworkNamePatterns` 用于非前缀匹配模式

## 6. Overlay UI 交互调整（compose/）

- [x] 6.1 被过滤的热点从 hotspot 列表中隐藏（而非灰显），在底部以紧凑 chip 形式展示，带 ✕ 按钮可恢复
- [x] 6.2 过滤状态从 `RecompositionProfiler.isNameExcluded()` / `getExcludedNames()` 读取，而非本地 `remember` 状态

## 7. RecompositionProfiler 新增查询/移除 API（compose/）

- [x] 7.1 新增 `removeExcludedName(name: String)` API，支持从 Overlay chip 的 ✕ 按钮单独移除某个过滤项
- [x] 7.2 新增 `getExcludedNames(): Set<String>` API，供 Overlay 读取当前过滤列表
- [x] 7.3 新增 `isNameExcluded(name: String): Boolean` API，供 Overlay 判断某个热点是否已被过滤

## 8. 提交 commit（compose/）

- [x] 8.1 将以上所有改动（含 worktree 中已存在的 Phase 1/2 代码）整理为一个完整 commit，message：`feat(compose): add business custom filter API and overlay filter UI for RecompositionProfiler`
