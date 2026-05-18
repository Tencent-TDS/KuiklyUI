## Why

重组 Profiler 已具备自动追踪所有 Composable 重组的能力，但业务团队的公共基础组件（如 `BaseButton`、`LoadingSpinner`、通用布局容器等）往往在每帧都大量重组，淹没了真正需要关注的业务页面组件热点。目前没有任何机制让业务侧声明「这些基础组件不需要追踪」，导致面板和日志噪音极大。

## What Changes

- **新增** `RecompositionProfiler.excludeByName(names: List<String>)` — 按 Composable 名称精确排除，追加语义
- **新增** `RecompositionProfiler.excludeByPrefix(prefixes: List<String>)` — 按包名前缀批量排除，追加语义
- **改造** `RecompositionProfiler.clearCustomFilters()` — 清空所有业务自定义过滤（内置框架过滤不受影响）
- **新增** Overlay 面板每个热点行右侧「过滤」按钮，点击立即将该 Composable 加入排除列表
- **新增** Overlay 控制栏「清空过滤」按钮
- **新增** `RecompositionReport` 中 `filteredNames` / `filteredPrefixes` 字段，反映当前生效的过滤配置快照
- **新增** 操作时 KLog 输出当前完整过滤列表（`addCustomFilter` / `clearCustomFilters` 时各输出一行）
- **移除** Phase 3 遗留的细粒度 filter 对象 API（`addCustomFilter(ComposableFilter)` / `removeCustomFilter` / `updateFilterConfig` / `getCustomFilters` / `getFilterConfig`）— 替换为更简洁的字符串 list 接口

## Capabilities

### New Capabilities

- `profiler-custom-filter-api`: `RecompositionProfiler` 上的业务自定义过滤 API（`excludeByName` / `excludeByPrefix` / `clearCustomFilters`），以及操作时的日志输出行为
- `profiler-overlay-filter-ui`: Overlay 面板上的过滤交互 UI（热点行「过滤」按钮 + 控制栏「清空过滤」按钮）
- `profiler-report-filter-snapshot`: `RecompositionReport` 新增过滤列表快照字段

### Modified Capabilities

- `recomposition-profiler-api`: 移除 Phase 3 遗留的 `ComposableFilter` 对象级 API，由新字符串 list API 替代

## Impact

**受影响模块**：`compose/`（Profiler 核心 + Overlay UI）

**受影响文件**：
- `compose/src/commonMain/.../profiler/RecompositionProfiler.kt` — 新增/移除 API
- `compose/src/commonMain/.../profiler/RecompositionReport.kt` — 新增字段
- `compose/src/commonMain/.../profiler/output/ProfilerOverlay.kt` — 新增 UI 按钮
- `compose/src/commonMain/.../profiler/output/OverlayOutputStrategy.kt` — 透传回调

**受影响平台**：Android / iOS / HarmonyOS（`compose/` 是纯 KMP 模块，所有平台共享）

**Non-goals**：
- 不记录每帧「被过滤了哪些 Composable」的实时日志（只在操作时输出一次）
- 不支持过滤规则持久化（关闭 App 后规则不保留）
- 不改动内置框架过滤逻辑（`includeFrameworkComposables` / `enableBuiltinFilters`）
- 不提供过滤规则的 UI 编辑界面（只有「过滤单个」和「清空全部」）
