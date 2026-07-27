## Why

Kuikly Compose 当前完全没有 LazyList prefetch 能力：所有列表 item 都在进入 viewport 的同一帧内完成 compose + measure + native view 创建（含 K/N ↔ ObjC/JVM 桥接），导致快速滚动时频繁掉帧。官方 Compose 1.8+ 已沉淀了完整的 LazyLayout idle-time prefetch 方案（PrefetchScheduler + LazyLayoutPrefetchState + PausableComposition + CacheWindow），但 Kuikly 在 vendor 时把 prefetch 路径全部注释掉了，且 CMP `release/1.9` 的 skiko 默认实现是空 NoOp，iOS 上至今没有真正可用的 scheduler。本次改动以 CMP `release/1.9` 为基线把 prefetch 完整引入 Kuikly，并基于 Kuikly 自有的 VSync 帧循环写一份跨平台 commonMain Scheduler，覆盖 Android + iOS（含 iOS JS 动态化）。

## What Changes

- 从 CMP `origin/release/1.9` vendor 完整的 LazyList prefetch 体系（`LazyLayoutPrefetchState` / `LazyListPrefetchStrategy` / `LazyListCacheWindowStrategy` / `CacheWindowLogic` / `LazyLayoutCacheWindow` / `PrefetchScheduler` / `PrefetchRequest` / `NestedPrefetchScope` 等），包名由 `androidx.compose.*` 改写为 `com.tencent.kuikly.compose.*`。
- 升级 Compose plugin `org.jetbrains.compose` 从 `1.7.3` 到 **1.9.3**（Kotlin `2.1.21` 构建线；公开 Maven 无 1.9.4）。**前置**：Gradle wrapper **7.6.3 → 8.2**、AGP **7.4.2 → 8.2.2**（Kotlin 2.1.21 兼容表最低 AGP；不一次性升到 AGP 9 / Gradle 8.12）。
- 新增 `KuiklyPrefetchScheduler`（commonMain）替代官方 `AndroidPrefetchScheduler` / 空 skiko 实现：挂载到 `BaseComposeScene.render()` 末尾、在帧 idle 时间内消费 prefetch 队列；为 JS target（H5 / 小程序 / iOS JS 动态化）提供 `NoOpPrefetchScheduler` 通过 `expect/actual` 规避 K/JS 上的 `Long` 性能问题。
- 在 `ComposeSceneMediator` 持有 scheduler 实例，按 Pager 隔离（天然支持 multi-Pager 各自线程）；在 `ComposeContainer.ProvideContainerCompositionLocals` 注入 `LocalKuiklyPrefetchScheduler`。
- 解除 `LazyLayout.kt` / `LazyListState.kt` / `LazyList.kt` 中既有的 prefetch 注释，并把 `prefetchState` 设计成可空（K/JS target 为 `null`，原生 target 启用），保留 commit 1 "纯 vendor" 的可审计性。
- 启用 `ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = true`（与 CMP 1.11 默认行为对齐）。**前置**：Commit 2b（runtime 1.9.3）+ Commit 6（`SubcomposeLayout.precomposePaused` 真实实现，替换 Eager 桩）。
- 把 `RecompositionProfiler` 现有的 `onFrameEnd(0)` 改为上报真实 prefetch 耗时，避免 prefetch 时间污染主帧性能指标。
- **默认关闭 + 业务显式 opt-in**：本次改动新增的 prefetch 能力**全局默认关闭**（`ComposeFoundationFlags.isLazyListPrefetchEnabled = false`），业务侧通过新增的 Kuikly 自创 `Modifier.enableLazyListPrefetch()` 在单个 `LazyColumn` / `LazyRow` 上显式启用，或通过把全局 flag 设为 `true` 一键全开。这样既能保证 vendor 后默认行为与改动前**逐 LazyList 等价**，又保留全局 kill switch 以应对线上灰度回滚。
- **业务可观察风险（启用后）**：业务侧 `LaunchedEffect(Unit)` / `DisposableEffect.onAttach` 等在 prefetched item 上会**提前触发**（与官方 Compose 行为一致），可能影响依赖这些副作用做曝光埋点 / 网络请求的业务代码。因此默认关闭 + 业务侧逐 list 显式启用，配合迁移文档说明。
- **非目标（Non-goals）**：本次改动**不**覆盖 LazyGrid / LazyStaggeredGrid / Pager 的 prefetch（留待后续 phase）；**不**覆盖 HarmonyOS / Web / 小程序的真实 prefetch 调度（保持 NoOp）；**iOS JS 动态化模式本期不做验收**（仍 NoOp，见 tasks §11 下期 TODO）；**不**调整 Kuikly 帧调度 / VSync 模块本身的频率策略；**不**改造 `SubcomposeLayout` 中已有的 Kuikly 定制（重用、movableContent、Sticky Header 等保持原状）；**不**自动迁移现有业务代码（业务自行决定哪些 list 启用 prefetch）。

## Capabilities

### New Capabilities
- `lazy-list-prefetch`: LazyList 的 idle-time 预组合 + 预测量 + native view 预创建能力；包含 `PrefetchScheduler` 接口、`LazyLayoutPrefetchState`、`LazyListPrefetchStrategy`（默认 1-item）/ `LazyListCacheWindowStrategy`（可选 CacheWindow），以及 Kuikly 帧循环集成、JS target NoOp 兜底。

### Modified Capabilities
- 无（既有 specs 均不涉及 LazyList prefetch 行为）。

## Impact

- **受影响平台**：
  - Android：vendored Android scheduler 不引入，使用 commonMain `KuiklyPrefetchScheduler`，prefetch 真实生效。
  - iOS（K/Native）：使用 commonMain `KuiklyPrefetchScheduler`，prefetch 真实生效。
  - iOS JS 动态化（K/JS）：`expect/actual` 路径走 `NoOpPrefetchScheduler`，prefetch 静默关闭，零 Long 算术开销。
  - HarmonyOS / Web / 小程序：均走 K/JS 或暂不接帧循环，NoOp 兜底，无功能变更。
- **受影响模块**：
  - `compose/`：核心改动（vendor 文件 + Scheduler + 帧循环接入 + Local 注入）。
  - `compose/build.2.1.21.gradle.kts`：plugin / runtime 版本升级（Commit 2b）。
  - `gradle/wrapper/gradle-wrapper.properties`：Gradle 7.6.3 → 8.2（Commit 2b）。
  - `build.2.1.21.gradle.kts`：AGP 7.4.2 → 8.2.2（Commit 2b）。
  - `core/`：不修改业务接口，仅借用 `VsyncModule` 的现有 callback。
  - `core-render-*`：不动。
  - `demo/`：新增一个 prefetch 验证页（可选，不在 Phase 1 强约束内）。
- **受影响 API（业务可见）**：
  - 新增 `Modifier.enableLazyListPrefetch(enabled: Boolean = true)`（Kuikly 自创）— 单个 LazyList 开关。
  - 新增 `ComposeFoundationFlags.isLazyListPrefetchEnabled: Boolean`（Kuikly 自创全局开关，默认 `false`）。
  - 新增 `rememberLazyListState(cacheWindow: LazyLayoutCacheWindow, ...)` 重载（业务可显式启用 CacheWindow）。
  - 新增 `LazyLayoutCacheWindow(ahead: Dp, behind: Dp)` 构造函数。
  - `LazyListState` 内部新增 `prefetchState` 字段（仅 internal 可见）。
  - 业务可观测变化（仅 opt-in 后）：滚动时 prefetched item 的 `LaunchedEffect` / `DisposableEffect` 会比之前更早触发。
- **依赖**：
  - 升级 `org.jetbrains.compose` plugin `1.7.3 → 1.9.4`，runtime 同步升到 1.8.x。
  - 不引入新的第三方依赖。
- **风险**：
  - `LaunchedEffect` 提前触发可能破坏依赖它做曝光的业务代码（缓解：默认全局关闭 + 业务逐 list 显式 opt-in + 文档说明）。
  - 单 item 桥接成本未实测过，单帧 idle 预算可能塞不下复杂 item（缓解：开启 PausableComposition + 续帧机制）。
  - Compose plugin `1.7.3 → 1.9.4` 升级带来 runtime / 编译器 transparent 变化（如 strong skipping），不在本 change 的可控范围内，**业务即使不开 prefetch 也会受影响**（缓解：Phase 1 灰度时把 plugin 升级作为独立验证项）。
