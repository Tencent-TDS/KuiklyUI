## 0. Commit 顺序总览

```
✅ 1  vendor (CMP 1.9 prefetch 体系)
✅ 2  collection-internal 1.9.3
✅ 2b Gradle 8.7 + AGP 8.6 + compose plugin 1.9.3  ← Pausable 构建前置（高于 design 最低 8.2/8.2.2）
✅ 3  vendor 编译修复 + Scheduler 骨架
✅ 4  LazyList 接线 + Modifier opt-in
✅ 5  KuiklyPrefetchScheduler + 帧循环
✅ 6  SubcomposeLayout.precomposePaused merge
✅ 7  isPausableCompositionInPrefetchEnabled = true
🟡 8–14 跨平台验证 / demo / appium / 文档（Android ✅；iOS Native ✅；JS 动态化 ⏸ 下期）
```

## 1. Commit 1 — Vendor pure copy from CMP release/1.9 (compose 模块)

> 目标：100% 官方代码 + package rename，**不夹带任何 Kuikly 修改**；本 commit 编译可能不通过（找不到符号），允许。
> 参考 commit sha：`origin/release/1.9` HEAD（记录在 commit message 里）。

- [x] 1.1 在 `~/Git/Work/compose-multiplatform-core` 切到 `origin/release/1.9`，记录 HEAD sha (`278178153c9189353ca7bb03f0904ddd9d995ce3`)
- [x] 1.2 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/ComposeFoundationFlags.kt`（package rename，flag 默认值保持官方原状 `false`）
- [x] 1.3 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/LazyListPrefetchStrategy.kt`（含默认 1-item `DefaultLazyListPrefetchStrategy`）
- [x] 1.4 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/LazyListCacheWindowStrategy.kt`
- [x] 1.5 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/layout/LazyLayoutCacheWindow.kt`
- [x] 1.6 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/layout/CacheWindowLogic.kt`
- [x] 1.7 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/layout/LazyLayoutPrefetchState.kt`（含 `PrefetchHandleProvider`、`PrefetchMetrics`、`PriorityTask` 等内嵌类型）
- [x] 1.8 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/layout/PrefetchScheduler.kt`（含 `PrefetchScheduler` / `PrefetchRequest` / `PrefetchRequestScope` / `PriorityPrefetchScheduler` interface；保留 `internal expect fun rememberDefaultPrefetchScheduler()` 签名，Commit 3 改造）
- [x] 1.9 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/layout/NestedPrefetchScope.kt`（官方 1.9 中内嵌于 `LazyLayoutPrefetchState.kt`，无独立文件）
- [x] 1.10 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/layout/TraversablePrefetchStateModifierElement.kt`（官方 1.9 中内嵌于 `LazyLayoutPrefetchState.kt`，无独立文件）
- [x] 1.11 新增 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/layout/TraversablePrefetchStateNode.kt`（官方 1.9 中内嵌于 `LazyLayoutPrefetchState.kt`，无独立文件）
- [x] 1.12 vendor `foundation/internal/InlineClassHelper.kt`（含 `checkPrecondition` / `requirePrecondition`；官方无独立 `CheckPrecondition.kt`）
- [x] 1.13 package rename: `androidx.compose.foundation` → `com.tencent.kuikly.compose.foundation`；`androidx.compose.ui` → `com.tencent.kuikly.compose.ui`；`androidx.compose.runtime.*` 不动
- [x] 1.14 提交：`vendor(compose): import lazy list prefetch from CMP release/1.9 (278178153c9)`，commit message 列出所有 sed 命令以便重放

## 2. Commit 2 — Build: collection-internal 1.9.3 (Kotlin 2.1 line)

> Commit 2 原目标含 plugin 1.9.x，因 Gradle 7.6.3 阻塞已拆到 **Commit 2b**。本节仅 collection。

- [x] 2.1 在 `settings.gradle.kts` 中找到 Kotlin 2.1.21 构建线对应的 `build.X.Y.Z.gradle.kts`，确认是 `build.2.1.21.gradle.kts`
- [x] 2.2 ~~plugin 1.7.3 → 1.9.4~~ → 移至 Commit 2b（Gradle 7.6.3 无法加载 compose plugin 1.8+）
- [x] 2.3 `compose/build.2.1.21.gradle.kts`：`collection-internal:collection` 1.7.3 → 1.9.3
- [x] 2.4 `./gradlew :compose:compileDebugKotlinAndroid --refresh-dependencies` 通过
- [x] 2.5 提交：`build(compose): bump collection-internal to 1.9.3 on Kotlin 2.1.21 line` (aa79fec7)

## 2b. Commit 2b — Build: 最小 Gradle/AGP/Compose runtime 升级（Pausable 前置）

> **目标**：升到 PausableComposition 可用的最低构建链，**不追最新**（见 design D10）。
>
> | 组件 | 7.6.3 线当前 | 目标（最低） |
> |------|-------------|-------------|
> | Gradle wrapper | 7.6.3 | **8.2** |
> | AGP | 7.4.2 | **8.2.2** |
> | `org.jetbrains.compose` plugin | 1.7.3 | **1.9.3** |
> | runtime / runtime-saveable | 1.7.3 BOM | **1.9.3** |
>
> 依据：Kotlin 2.1.21 兼容表 AGP 最低 8.2.2；compose plugin 1.9.3 在 Gradle 8.0/8.2/8.3 实测可配置；AGP 8.2.x 要求 Gradle ≥ 8.2。

- [x] 2b.1 `gradle/wrapper/gradle-wrapper.properties`：`distributionUrl` 改为 `gradle-8.2-bin.zip`（**不**升到 8.12）
- [x] 2b.2 `build.2.1.21.gradle.kts`：`com.android.application` / `com.android.library` **7.4.2 → 8.2.2**（兼容表最低，**不**升到 AGP 8.7/9.0）
- [x] 2b.3 `build.2.1.21.gradle.kts`：`org.jetbrains.compose` **1.7.3 → 1.9.3**
- [x] 2b.4 `compose/build.2.1.21.gradle.kts`：确认 `compose.runtime` / `runtime-saveable` 解析到 **1.9.3**（必要时显式 `api("org.jetbrains.compose.runtime:runtime:1.9.3")`）；`collection-internal` 保持 1.9.3
- [x] 2b.5 处理 AGP 8.2 迁移项（按需最小修复）：`compileSdk`/`namespace` 警告、废弃 API、`android.defaults.buildfeatures.buildconfig` 等；**不**借机做大范围 AGP 现代化
- [x] 2b.6 适配 runtime 1.9 API 漂移：`ProfilerCompositionObserver`（`CompositionObserver` / `RecomposeScopeObserver` 新签名）、`BaseComposeScene` 中 `recomposer.observe` 调用
- [x] 2b.7 `./gradlew :compose:compileDebugKotlinAndroid`、`./gradlew :core:compileDebugKotlinAndroid`、`./gradlew :androidApp:assembleDebug` 通过
- [x] 2b.8 iOS 快速编译抽查：`./gradlew :compose:compileKotlinIosSimulatorArm64`（2026-05-27 BUILD SUCCESSFUL）
- [x] 2b.9 提交：`build(compose): minimal Gradle 8.2 + AGP 8.2.2 + compose plugin 1.9.3 for PausableComposition`

## 3. Commit 3 — Fix: make vendored prefetch compile (compose 模块)

> 目标：让 Commit 1 vendor 进来的代码能编过；不接入 LazyList、不接入帧循环。

- [x] 3.1 改 `PrefetchScheduler.kt`：`rememberDefaultPrefetchScheduler()` 从 LocalKuiklyPrefetchScheduler 读取
- [x] 3.2 新增 `LocalKuiklyPrefetchScheduler.kt`
- [x] 3.3 新增 `NoOpPrefetchScheduler.kt`
- [x] 3.4 新增 `KuiklyPrefetchScheduler.kt` 骨架（processRequests 桩）
- [x] 3.5 新增 `KuiklyPrefetchSchedulerFactory.kt` expect
- [x] 3.6 新增 `KuiklyPrefetchSchedulerFactory.android.kt`
- [x] 3.7 新增 `KuiklyPrefetchSchedulerFactory.native.kt`（替代 iosMain）
- [x] 3.8 新增 `KuiklyPrefetchSchedulerFactory.js.kt`
- [x] 3.9 vendor stub：`Trace.kt`（trace/traceValue no-op）、`LazyListViewportExtensions.kt`（singleAxisViewportSize）、`SubcomposeLayout`（EagerPausedPrecomposition + getSize）
- [x] 3.10 跑 `./gradlew :compose:compileDebugKotlinAndroid` BUILD SUCCESSFUL
- [ ] 3.11 跑 `./gradlew :compose:compileKotlinIosX64`（如可用）或对等 iOS 编译验证
- [x] 3.12 提交：`fix(compose): make vendored prefetch compile, add Kuikly scheduler skeleton` (de210862)

## 4. Commit 4 — Wire LazyList to prefetch (compose 模块)

> 目标：解除 Kuikly 既有 `LazyLayout.kt` / `LazyListState.kt` / `LazyList.kt` 中被注释的 prefetch 代码；让 LazyList 走 prefetch 路径。每一行修改都是 Kuikly 改动，与官方代码无关。

- [x] 4.1 改 `LazyLayout.kt`：解除 prefetch 注释
- [x] 4.2 改 `LazyListState.kt`：解除 prefetchStrategy / prefetchState
- [x] 4.3 改 `LazyListState.kt`：补齐 cacheWindow 重载
- [x] 4.4 改 `LazyList.kt`：prefetchState 透传
- [x] 4.5 解除 onScroll / onVisibleItemsUpdated prefetch 调用
- [x] 4.6 新增 `LazyListPrefetchEnabledModifier.kt`
- [x] 4.7 改 `LazyList.kt`：ModifierLocalConsumer + effectiveEnabled
- [x] 4.8 LazyListState：effectiveEnabled=false 时早 return
- [x] 4.9 ComposeFoundationFlags.isLazyListPrefetchEnabled = false
- [x] 4.10 `./gradlew :compose:compileDebugKotlinAndroid` 通过
- [x] 4.11 提交：`feat(compose): wire LazyList to prefetch with Modifier opt-in and global kill switch`

## 5. Commit 5 — KuiklyPrefetchScheduler + frame loop integration (compose 模块)

> 目标：填充 Scheduler 实现 + 接入 BaseComposeScene render() + Profiler 上报真实 prefetch 时间。本 commit 是核心 Kuikly 自研部分。

- [x] 5.1–5.14 KuiklyPrefetchScheduler + BaseComposeScene 帧循环集成（commit a93d5bb5）

## 6. Commit 6 — SubcomposeLayout precomposePaused

- [x] 6.1–6.5 SubcomposeLayout precomposePaused merge（commit 61ef20c4）

## 7. Commit 7 — Enable PausableComposition in prefetch

- [x] 7.1–7.4 isPausableCompositionInPrefetchEnabled = true（commit 10ceaec1）

## 8. 编译验证 (跨平台)

- [x] 8.1 `./gradlew :compose:compileDebugKotlinAndroid` 通过
- [x] 8.2 `./gradlew :core:compileDebugKotlinAndroid` 通过（不应被影响）
- [x] 8.3 `./gradlew :androidApp:assembleDebug` 通过（历史 commit + 本地增量）
- [x] 8.4 iOS：`pod install` + `xcodebuild` + `:compose:compileKotlinIosSimulatorArm64` 通过（日志：`logs/kuikly_xcodebuild_sim.log`、`logs/kuikly_gradle_ios_compose.log`）
- [ ] 8.5 H5：`./gradlew :h5App:jsBrowserDevelopmentWebpack` 通过（prefetch NoOp 无回归；本期低优先级）

## 9. 运行时验证 — Android

> 脚本：`scripts/mobile-test/lazy-prefetch-e2e.ts`；报告示例：`logs/lazy_prefetch_e2e_*.md`

- [x] 9.1 LazyListPrefetchDemoPage（含 CacheWindow / headGap 日志）
- [x] 9.2 kuikly-app-runner 部署 Android demo
- [x] 9.3 预取 OFF baseline
- [x] 9.4 Modifier opt-in ON
- [x] 9.5 反向滚动 + headGap
- [ ] 9.6 RecompositionProfiler（skip：demo 未接入）
- [x] 9.7 全局 flag only
- [x] 9.8 全局 ON + Modifier false 覆盖
- [x] 9.9 重 item + 预取（settledTailGap + schedulePremeasure）
- [x] 9.10 CacheWindow 1000dp（追加项；停稳主判据改为 indexLead，禁止 OR 混判）
- [x] 9.11 预 compose 后划入 viewport：composition 入树仅 1 次（DisposableEffect，对齐官方 prefetch 语义）

## 10. 运行时验证 — iOS Native

> 脚本：`scripts/mobile-test/lazy-prefetch-ios-native.ts`；报告：`logs/lazy_prefetch_ios_native_1779867825851.md`

- [x] 10.1 kuikly-app-runner / Appium 部署 iOS Simulator 并进入 Demo
- [x] 10.2 **与 Android 相同**跨平台用例 9.3–9.11
- [ ] 10.3 Instruments Time Profiler（⏸ 后续补测，本期不阻塞）
- [x] 10.4–10.5 反向滚动 / 9.5 对齐（已并入 9.5 共享用例）
- [x] 10.6 RecompositionProfiler（skip，同 9.6）

## 11. 运行时验证 — iOS JS 动态化（**下期 TODO，本期不支持**）

> **本期明确不做**：JS 动态化模式仍走 NoOp PrefetchScheduler，不纳入 MR 验收范围。

- [ ] 11.1 切换 JS 动态化模式部署 demo — **TODO 下期**
- [ ] 11.2 开 Modifier 仍仅 placed 时 composed（NoOp 路径）— **TODO 下期**
- [ ] 11.3 滚动流畅性 vs baseline — **TODO 下期**

## 12. 自动化测试 — appium 回归

- [x] 12.1 demo 暴露 composed/placed/indexLead 到 UI Text
- [x] 12.2 Android：`scripts/mobile-test/lazy-prefetch-e2e.ts`（替代原 lazy-prefetch-android.py）
- [x] 12.3 iOS Native：`scripts/mobile-test/lazy-prefetch-ios-native.ts`（与 Android 共享 9.3–9.10）
- [x] 12.4 MR 前跑通（Android 全量 + iOS 全量用例；Instruments 不阻塞）

## 13. 业务侧副作用展示 demo (补强 4)

- [ ] 13.1 `PrefetchSideEffectsDemoPage.kt`
- [ ] 13.2 顶部说明 LaunchedEffect 提前触发风险
- [ ] 13.3 录制 30s 滚动+反转视频/GIF
- [ ] 13.4 链入 `.ai/compose-dsl/lazy-list-prefetch.md`

## 14. 文档与归档

- [ ] 14.1 `.ai/compose-dsl/lazy-list-prefetch.md`（含 opt-in、Pausable 前置、LaunchedEffect 迁移）
- [ ] 14.2 `docs/` 官网 LazyList prefetch 段落
- [ ] 14.3 `kuikly-doc-archive-review`
- [ ] 14.4 `openspec verify enable-lazy-list-prefetch`
- [ ] 14.5 MR 描述：列出 **8 个代码 commit**（1/2/2b/3/4/5/6/7）+ 验证截图/视频
