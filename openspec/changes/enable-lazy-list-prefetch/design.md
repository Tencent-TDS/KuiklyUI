## Context

**DSL 范围**：本次改动**仅作用于 Compose DSL**（`compose/` 模块），不涉及自研 DSL。

**现状速描**：
- Kuikly Compose vendor 自 CMP `release/1.7`，但 vendor 时把 `LazyLayout.kt` / `LazyListState.kt` / `LazyList.kt` 里的 prefetch 代码全部注释，导致 LazyList 完全没有 idle 预组合能力。
- CMP `release/1.9`（Jetpack Compose 1.8.2）已包含完整的 LazyList prefetch 体系：`PrefetchScheduler`、`LazyLayoutPrefetchState`、`PrefetchHandleProvider`、`LazyListCacheWindowStrategy`、`CacheWindowLogic`、`LazyLayoutCacheWindow`、`NestedPrefetchScope`、`TraversablePrefetchStateNode` 等。
- CMP `release/1.9` 的 `PrefetchScheduler.android.kt` 基于 `Choreographer + view.post`，与 Kuikly 自己的 VSync 帧循环不兼容；`PrefetchExecutor.skiko.kt`（iOS / Desktop / 小程序 / Web 公共 skiko target）则是空 NoOp，引用 youtrack issue `CMP-1265`，截至 `release/1.11` 仍未实现。
- Kuikly 的帧循环：`VsyncModule` 或 `Timer(0, 12ms)` 触发 → `ComposeSceneMediator.renderFrame()` → `vsyncTickConditions.onDisplayLinkTick { scene.render(null, ts) }` → `BaseComposeScene.render(canvas, nanoTime)`，render 内顺序执行 `recomposer.performScheduledTasks` → `frameClock.sendFrame` → `doLayout` → `performScheduledEffects` → `draw(KuiklyCanvas())`。
- Kuikly 的"view = native UIView/Android View"模型：每个 `KNode<T>` 持有 `view: DeclarativeBaseView`，`KuiklyApplier.insertTopDown` 会同步触发 `currentView.insertDomSubView` → `insertSubRenderView` → `createRenderView()`，即 **prefetch 阶段就会创建 native render view**（含 K/N ↔ ObjC/JVM 桥接），这与官方 Android Compose 不同（后者不创建子 View，只在自绘 canvas 上 draw）。
- Kuikly 同时支持 K/Native 和 K/JS 两种 iOS 运行模式：iOS Native 模式编译到 K/Native target，iOS JS 动态化模式编译到 K/JS target。K/JS 上 `Long` 是对象模拟，高频算术性能差 10–100 倍，必须避免 prefetch 的 Long-heavy 热路径在 K/JS 上跑。
- Kuikly 每个 `Pager`（即每个 `ComposeContainer`）绑定一个独立的 `ComposeDispatcher(pagerId)`，跑在自己的协程线程；不同 Pager 之间不共享线程。
- 业务侧约束：Kuikly 内部业务代码大量使用 `LaunchedEffect(Unit)` 做"item 显示"上报，prefetch 启用后会改变这一时机，需要默认保守。

**约束**：
- 必须支持 Kotlin `2.1.21` 构建线，`1.9.22` 构建线本次不涉及。
- 包名约束：所有 vendor 文件从 `androidx.compose.*` 改写为 `com.tencent.kuikly.compose.*`，仅 `androidx.compose.runtime.*` 例外。
- 必须保留 vendor commit 的可审计性：第一次 commit 必须是"纯官方代码 + package rename"，不夹带 Kuikly 修改。
- 不引入新的第三方依赖（仅升级 `org.jetbrains.compose` plugin）。
- Phase 1 不动 `SubcomposeLayout`（保留 Kuikly 既有的 movableContent / Sticky Header / `KNode.lazyItemKey` 等定制）。

## Goals / Non-Goals

**Goals:**
- 让 `LazyColumn` / `LazyRow` 在 Android + iOS（Native + JS 动态化）上具备 idle prefetch 能力，单 item 桥接成本由 PausableComposition 跨帧分担。
- 提供官方默认（1-item）和业务可选的 `LazyLayoutCacheWindow` 两套策略，业务侧零代码改动即可享受 1-item 收益。
- 整个 prefetch 代码栈与 CMP `release/1.9` 官方源码保持 1:1 对应（除 package rename 与必要的 expect/actual 桥接），便于后续 cherry-pick 1.11 改进。
- K/JS target（含 iOS JS 动态化、H5、小程序）通过 `expect/actual` 走 `NoOpPrefetchScheduler`，prefetch 静默关闭，零 `Long` 算术开销。
- Profiler 能区分"prefetch 时间"和"主帧时间"，不让 prefetch 拉高现有的帧时长指标。
- 多 Pager 各自子线程场景下，scheduler 实例按 Pager 隔离，零共享、零锁。

**Non-Goals:**
- LazyGrid / LazyStaggeredGrid / Pager（横向翻页）的 prefetch（后续 phase）。
- HarmonyOS 平台的真实 prefetch 调度（保持 NoOp）。
- 自研 DSL 的列表 prefetch（Kuikly `List` / `Scroller`）。
- 调整 Kuikly 自身的 VSync 频率 / 续命策略本身。
- 重写 `SubcomposeLayout`（Kuikly 已有大量定制，本次完全保留）。
- 适配 CMP `release/1.11` 的 trace / 线程安全增强（短期不必要）。

## Decisions

### D1. 基线版本选 CMP `release/1.9`，不选 `release/1.11`

**选择**：以 CMP `origin/release/1.9`（Jetpack Compose 1.8.2，runtime 包含 PausableComposition）作为 vendor 基线。

**理由**：
- CMP `1.11` 强制 Kotlin `2.3.20`，与 Kuikly `2.1.21` 不兼容。强行回 backport 涉及多个 internal API 重命名（`ComposerImpl` → `InternalComposer`、`RecordingApplier.RECOMPOSE_PENDING` 等），风险高、收益小。
- `1.9 → 1.11` 在 prefetch 核心代码 diff ≤ 100 行（实测：`LazyLayoutPrefetchState.kt` 15 行、`PrefetchScheduler.kt` 1 行、`PausableComposition.kt` 69 行），主要是 trace 增强、线程安全 fix、默认 flag 翻转。
- 1.9 的 `ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = false` 默认关，但代码路径完整；只要手动改成 `true`，行为即与 1.11 默认一致。
- 1.9 已经包含 `LazyListCacheWindowStrategy.kt` / `CacheWindowLogic.kt` / `LazyLayoutCacheWindow.kt`，业务侧通过 `rememberLazyListState(cacheWindow = ...)` 即可启用 CacheWindow——无需为 CacheWindow 特地选 1.11。
- 1.9 的 `PrefetchExecutor.skiko.kt`（iOS） 与 1.11 完全一致（同为空 NoOp、引用同一 youtrack issue），所以"iOS 真正可用 scheduler"在 1.11 上仍要 Kuikly 自己写，没有可继承的官方代码。

**备选方案**：
- 备选 A：升 1.11 + Kotlin 2.3。否决，工作量级和兼容性风险都不在本次 scope。
- 备选 B：完全自写 prefetch，不 vendor 官方代码。否决，会失去 1.9 已有的 `CacheWindowLogic`、`NestedPrefetchScope`、`PrefetchHandleProvider` 等大量复杂实现，且后续无法 cherry-pick 官方修复。

### D2. 完全自写 commonMain 的 `KuiklyPrefetchScheduler`，不 vendor 官方 platform 实现

**选择**：把官方 `PrefetchScheduler.android.kt`（Choreographer 实现）和 `PrefetchExecutor.skiko.kt`（空 NoOp）**不 vendor 进来**，由 Kuikly 在 `commonMain` 实现一份统一的 `KuiklyPrefetchScheduler`。然后把官方在 `commonMain` 中的 `@Composable internal expect fun rememberDefaultPrefetchScheduler()` 改造成不再 expect，而是从 `LocalKuiklyPrefetchScheduler` 取实例。

**理由**：
- 官方 Android 实现强依赖 `Choreographer + view.post`，Kuikly 帧循环走的是 K/N 协程线程（不是 Android UI 线程），无法直接复用。
- 官方 skiko 实现是空的，复用没意义。
- Kuikly 已经有完整的"render 在 VSync 回调里被调用 → 当前帧 start time 就是 `nanoTime` 入参 → 帧间隔可推算"的语义，足够实现 `PrefetchScheduler` 接口要求的 `availableTimeNanos()`，且 Android + iOS Native 用同一份代码。
- 把 platform 实现削掉，后续 cherry-pick 官方 prefetch 改进时 conflict 面更小。

**备选方案**：
- 备选 A：保留 vendor android 文件，加 `expect/actual` 包装。否决，需要解决 Choreographer 不存在的问题，且和 Kuikly 帧循环语义不一致。
- 备选 B：在 iosMain / androidMain 各自写 actual。否决，两端逻辑完全相同（都依赖 Kuikly 帧循环），重复且易漂移。

### D3. `KuiklyPrefetchScheduler` 在 `BaseComposeScene.render()` 末尾消费队列

**选择**：在 `BaseComposeScene.render(canvas, nanoTime)` 的 `draw(KuiklyCanvas())` 之后插入 `prefetchScheduler.processRequests(nowNanos = nanoTime, frameIntervalNs = 16_666_667L, isFrameIdle = ...)`。

**理由**：
- Kuikly render 在 Pager 的协程线程跑，prefetch 任务也必须同线程（PausableComposition 不允许跨线程 resume）；在 render() 末尾插就是最直接的"同线程顺序执行"。
- `nanoTime` 是当前帧开始时间，直接用作 `frameStartTimeNs`，配合硬编码 `frameIntervalNs = 16_666_667L`（60fps）算 `nextFrameTimeNs`，无需平台层上报刷新率。
- `availableTimeNanos() = max(0, nextFrameTimeNs - now)`，与官方 `AndroidPrefetchScheduler` 完全一致的语义。
- idle 判定：`isFrameIdle = !vsyncTickConditions.needsToBeProactive && vsyncTickConditions.scheduledRedrawsCount == 0`，等价于官方"最近 2 帧没绘制"（Kuikly 的续命计数刚好表达"接下来还要不要画帧"）。

**备选方案**：
- 备选 A：独立 Timer / coroutine 调度 prefetch。否决，无法保证与 render 同线程，且无法天然拿到帧 deadline。
- 备选 B：跑在 `ComposeSceneRecomposer.recomposeDispatcher` 上。否决，dispatcher 没有"当前帧 idle 剩余"概念，需要自己维护一套帧时序，重复造轮子。

### D4. 帧 idle 时间预算保守为 ~6ms（不是 ~8ms）

**选择**：`processRequests` 入口对 `availableTimeNanos()` 做一次 `if (available < SAFETY_BUDGET_NS) return` 早退，`SAFETY_BUDGET_NS = 2_000_000L`（2ms），即至少留 2ms 不被 prefetch 占用。

**理由**：
- Kuikly 单 item prefetch 包含 K/N ↔ ObjC/JVM 桥接调用，单次桥接抖动可能达到 1–3ms（远高于纯 JVM Compose）。
- 留 2ms buffer 防止 prefetch 跑超时挤占下一帧 draw。
- 这是 Kuikly 相对官方实现额外加的一道保守保护，不影响官方语义（仍然遵守 `availableTimeNanos()` 返回值）。

**备选方案**：
- 备选：完全按官方 `availableTimeNanos()` 推算，不加 safety budget。可选，待 Phase 1 实测确认是否够保守再调。

### D5. `isPausableCompositionInPrefetchEnabled = true`，并允许续帧（最多 2 帧）

**选择**：把 vendor 来的 `ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled` 默认值改为 `true`；在 `KuiklyPrefetchScheduler.processRequests` 末尾，如果队列还有 task 且 `vsyncTickConditions.needsToBeProactive == true`，调用 `vsyncTickConditions.needRedraw()` 续帧；用 `MAX_CONTINUATION_FRAMES = 2` 限制单次连续续帧上限，避免无限唤醒 VSync。

**理由**：
- Kuikly 单 item prefetch 桥接成本高，单帧 ~6ms idle 预算可能塞不下一个复杂 item；PausableComposition 让单 item 的 compose 可以跨帧分块完成。
- `needsToBeProactive == true` 意味着用户正在滚动，VSync 反正在跑，续帧不耗额外电；用户停手后不续帧，等下次操作时再继续，符合官方 `Choreographer.postFrameCallback` 的语义。
- 队列在默认 1-item 策略下天然有界（最多 1 个 task），即使在 CacheWindow 策略下也有界（被 ahead window 大小约束），2 帧连续续帧上限足够，超过则放弃这一轮 prefetch 到下次滚动事件。

**备选方案**：
- 备选 A：保持 `isPausableCompositionInPrefetchEnabled = false`。否决，会让单 item 桥接成本无法跨帧。
- 备选 B：完全不续帧。否决，单帧 ~6ms 在 Kuikly 上不够稳。

### D6. K/JS target 用 `expect/actual` 走 `NoOpPrefetchScheduler`；`prefetchState` 在 K/JS 上为 `null`

**选择**：在 `commonMain` 定义 `internal expect fun createDefaultKuiklyPrefetchScheduler(): PrefetchScheduler`；`androidMain` / `iosMain`（K/Native）actual 返回 `KuiklyPrefetchScheduler` 实例；`jsMain`（K/JS，覆盖 iOS JS 动态化 + H5 + 小程序）actual 返回 `NoOpPrefetchScheduler { schedulePrefetch { /* 空 */ } }`。同时在 Kuikly 改动的 `LazyListState` 中通过 `expect val isPrefetchSupported: Boolean` 控制 `prefetchState` 是否真创建：K/JS 上为 `null`，K/Native 上为 `LazyLayoutPrefetchState(...)`。

**理由**：
- 单层 `NoOpScheduler` 已经避免主要的 `Long` 算术热路径（`schedulePrefetch` 不会真的执行）；
- 双层（`prefetchState = null`）进一步保证 `LazyListPrefetchScope.onScroll` 早 return，连 `calculateIndexToPrefetch` 这类 `Int` 计算也省掉。
- "`prefetchState` 可空"刚好是官方 1.9 已支持的合法形态（`LazyLayout(prefetchState = null, ...)`），不破坏官方语义。
- K/JS 编译产物里 vendor 来的 `LazyLayoutPrefetchState` / `PrefetchHandleProvider` 等代码仍会被打包（dead code），但运行时永远进不去 hot path，开销可忽略。

**备选方案**：
- 备选 A：在 `commonMain` 用 runtime 判断 `if (isJsRuntime) ...`。否决，无法在编译期消除热路径。
- 备选 B：把 vendor 代码再做一次 `expect/actual` 拆分，把 Long-heavy 部分单独抽到 platform 文件。否决，会大幅破坏 vendor 代码与官方源码的对应关系。

### D7. `LocalKuiklyPrefetchScheduler` 由 `ComposeSceneMediator` 持有并通过 `CompositionLocalProvider` 注入

**选择**：每个 `ComposeSceneMediator`（对应一个 Pager）在 init 时通过 `createDefaultKuiklyPrefetchScheduler()` 拿到自己的 scheduler 实例；`ComposeContainer.ProvideContainerCompositionLocals` 中加入 `LocalKuiklyPrefetchScheduler provides mediator!!.prefetchScheduler`；vendor 的 `rememberDefaultPrefetchScheduler()` 改造为 `LocalKuiklyPrefetchScheduler.current`。`ComposeSceneMediator.dispose()` 同步调用 `prefetchScheduler.cancelAll()` 清空队列，避免持有已 dispose 的 `SubcomposeLayoutState` 引用。

**理由**：
- 满足"多 Pager 各自子线程"约束：每个 Pager 一个 scheduler，调用方（LazyList 测量）和消费方（render() 末尾）天然在同一线程，无锁。
- `BaseComposeScene` 已经在 `setContent` 时通过 Recomposer 触发 composable 跑，CompositionLocal 注入是干净的方式，不需要把 scheduler 反向暴露到 Compose 内部。
- `cancelAll()` 是必需的兜底：`ComposeSceneRecomposer.cancel()` 已经处理了 dispatcher 残留 task；scheduler 也要同步清空。

**备选方案**：
- 备选 A：scheduler 放在 `BaseComposeScene`。否决，`BaseComposeScene` 是 abstract，让具体 Mediator 持有更合适。
- 备选 B：单例。否决，多 Pager 共享会破坏线程隔离。

### D8. Prefetch 默认**全局关闭**，业务通过 Modifier opt-in；启用时使用 1-item 策略

**选择**：
- 新增全局 flag `ComposeFoundationFlags.isLazyListPrefetchEnabled: Boolean`，**默认 `false`**（即默认所有 LazyList 都不开 prefetch）。
- 新增 Kuikly 自创 `Modifier.enableLazyListPrefetch(enabled: Boolean = true)`，业务侧给单个 `LazyColumn` / `LazyRow` 显式启用 prefetch。
- 启用后，默认策略仍为 1-item（`LazyListPrefetchStrategy()`，与官方 1.9/1.11 默认一致）；业务侧仍可通过 `rememberLazyListState(cacheWindow = ...)` 升级到 CacheWindow。
- Phase 1.5 不动这个默认（全局仍 `false`），后续根据 Phase 1 灰度结果再评估是否调高。

**理由**：
- 业务侧 `LaunchedEffect(Unit)` 做曝光埋点的代码量未知；vendor commit 后默认开任何 prefetch 都会改变 effect 时机，破坏面大。最稳的做法是"业务自己挑哪些 list 开"。
- 全局 flag 是 kill switch：若 Phase 1 上线后发现某个角落问题，运维侧把 flag 改回 `false` 即可一键全关，不需要回滚代码。
- Modifier API 比 `rememberLazyListState` 参数更自然：业务在使用 LazyList 时往往最早写 modifier（`.fillMaxSize()` 等），加一个 `.enableLazyListPrefetch()` 比改 state 工厂参数视觉负担更小，也更显眼。

**实现路径（具体细节由 Commit 4 决定，候选）**：
- 候选 A：`Modifier.enableLazyListPrefetch()` 通过 `ModifierLocalProvider` 提供一个 `ModifierLocal<Boolean>`；LazyList 内部用 `ModifierLocalConsumer` 读取，传给 LazyListState。
- 候选 B：`Modifier.enableLazyListPrefetch()` 是一个 `ModifierNodeElement<LazyListPrefetchEnabledNode>`，Node 持有 boolean，LazyList 内部通过遍历 modifier chain 读取。
- 候选 C：`Modifier.enableLazyListPrefetch()` 用 `CompositionLocal` 提供 override，但需要包裹 LazyList 内容；不推荐（破坏 Modifier 语义）。
- 推荐**候选 A**，与官方 Compose modifier+state 协作模式一致。

**判定优先级**：
```
effectiveEnabled =
    modifierLevel    (Modifier.enableLazyListPrefetch(true/false) 显式设置)
      ?: globalFlag  (ComposeFoundationFlags.isLazyListPrefetchEnabled)
```

也就是说，Modifier 显式 `enabled=false` 可以**覆盖**全局 flag = true（业务可以选择性禁用某个 list）；Modifier 不存在时就回退全局 flag。

**备选方案**：
- 备选 A：Phase 1 默认全局 1-item。否决，业务 `LaunchedEffect` 破坏面不可控。
- 备选 B：仅在 `rememberLazyListState` 加 `prefetchEnabled: Boolean` 参数。否决，用户明确要求用 Modifier，且 Modifier 设计更优雅。
- 备选 C：完全不提供 opt-in，业务无法启用 prefetch。否决，prefetch 能力归零，本 change 失去价值。

### D9. `RecompositionProfiler` 上报 prefetch 实际耗时（不再固定 0）

**选择**：把 `BaseComposeScene.render()` 中的 `tracker?.onFrameEnd(0)` 改为 `tracker?.onFrameEnd(prefetchSpentNs)`，其中 `prefetchSpentNs` 由 `KuiklyPrefetchScheduler.processRequests` 在结束时返回，表示本帧 prefetch 消耗的总时间。

**理由**：
- 当前 `onFrameEnd(0)` 在 prefetch 启用后会让"帧时长"指标看起来稳定，但真实的主帧耗时被掩盖。
- 上报 `prefetchSpentNs` 让 Profiler / 业务可以单独看 prefetch 时间，定位"prefetch 太重"问题。
- 修改面极小，对既有 Profiler API 兼容。

**备选方案**：
- 备选：暂不上报，等 Phase 1 上线后再改。可选，但建议同步做以避免误判。

### D10. 构建链最小升级：Gradle 8.2 + AGP 8.2.2 + Compose 1.9.3（不追最新）

**选择**：Commit 2b 按**兼容表允许的最低版本**升级，而非一次性跳到 Gradle 8.12 / AGP 9.0：

| 组件 | 当前 | 目标（最低） | 依据 |
|------|------|-------------|------|
| Gradle wrapper | 7.6.3 | **8.2** | `org.jetbrains.compose` 1.9.3 在 Gradle 8.0/8.2/8.3 上可配置（本地实测）；AGP 8.2.x 要求 Gradle ≥ 8.2 |
| Android Gradle Plugin | 7.4.2 | **8.2.2** | [Kotlin 2.1.21 兼容表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) 最低 AGP；AGP 7.4.x 不支持 Gradle 8 |
| `org.jetbrains.compose` plugin | 1.7.3 | **1.9.3** | 公开 Maven 1.9 线最新稳定版（无 1.9.4）；与 CMP `release/1.9` vendor 基线一致 |
| `compose.runtime` / `runtime-saveable` | 1.7.3 (BOM) | **1.9.3** | PausableComposition API 在 runtime 1.9；随 plugin BOM 或显式 override |
| `collection-internal` | 1.9.3 | 1.9.3 | Commit 2 已完成 |

**不选更高版本的理由**：
- Gradle 8.12 / AGP 8.7+ / AGP 9.0 会引入更多迁移面（JDK、namespace、R8、KMP 插件行为），与 prefetch 目标无关。
- Kotlin 2.1.21 兼容表上界是 Gradle 8.12.1 / AGP 9.0，本次只取**下界**。

**Commit 顺序约束**：
```
Commit 2  (collection 1.9.3)     ✅ 已完成
Commit 2b (Gradle/AGP/plugin/runtime) → 必须在 Commit 6/7 之前
Commit 4–5 (LazyList + Scheduler)     → 可在 2b 之前做基础 prefetch（Pausable=false）
Commit 6  (SubcomposeLayout.precomposePaused) → 依赖 2b runtime 1.9
Commit 7  (开 isPausableCompositionInPrefetchEnabled) → 依赖 Commit 6
```

**备选方案**：
- 备选 A：Gradle 8.0 + AGP 8.0。否决，AGP 8.0 不在 Kotlin 2.1.21 官方兼容表内，且 AGP 8.2.2 才是表内最低。
- 备选 B：保持 Gradle 7.6.3，仅 override runtime 1.9.3 JAR。否决，实测 compose plugin 1.8+ 在 7.6.3 上 `InvalidProtocolBufferException`；且 Profiler runtime API 漂移需整体升级。

### D11. `SubcomposeLayout.precomposePaused` 必须 vendor 进 Kuikly（Commit 6）

**选择**：从 CMP `release/1.9` 的 `SubcomposeLayout.kt` merge `createPausedPrecomposition` / `LayoutNodeSubcompositionsState.precomposePaused` 及 `PausedPrecomposition` 接口到 Kuikly 自研 `SubcomposeLayout.kt`，**保留** Kuikly 既有定制（movableContent、`KNode.lazyItemKey`、Sticky Header、Scroller 集成等）；删除 Commit 3 的 `EagerPausedPrecomposition` 桩。

**理由**：
- `createPausedPrecomposition` 在 **compose.ui**（Kuikly 本地 vendor），不在 Maven runtime 里；仅升 runtime 1.9 不够。
- Commit 3 的 Eager 桩只保证编译，无跨帧 Pausable 收益。
- D8 的「不改造 SubcomposeLayout」指**不破坏** Kuikly 定制，不是禁止 merge 官方 pausable API。

**备选方案**：
- 备选：永久保留 Eager 桩 + 只开 flag。否决，无 Pausable 价值，Kuikly 桥接成本高时仍会单帧超时。

## Risks / Trade-offs

- **R1**: 业务 `LaunchedEffect(Unit)` 在 prefetched item 上提前触发，可能导致曝光埋点上报错位、网络请求过早发出 → **缓解**：默认全局 flag = `false`（vendor 后全部 LazyList 行为与改动前一致）+ 业务侧通过 `Modifier.enableLazyListPrefetch()` 逐 list opt-in + 同步发布业务迁移指南（提示使用 onPlaced 或 visibility-aware API 做曝光）+ 全局 flag 保留 kill switch 用于线上灰度回滚。
- **R2**: 单 item 桥接成本可能超出帧 idle 预算，prefetch 反而拖累主帧 → **缓解**：D4 的 safety budget + D5 的 PausableComposition + D5 的 2 帧续帧上限三重保护；同时 D9 让 Profiler 能识别问题。
- **R3**: K/JS target 上 NoOp 兜底，但 vendor 来的 Long-heavy 代码仍会被打包到 bundle → **缓解**：JS 上 bundle size 增加可接受（KB 级别）；运行时不进入热路径 → 性能零影响。如果未来 bundle size 成为问题，可以再用 `expect/actual` 进一步切分。
- **R4**: `SubcomposeLayout` Kuikly 定制与 prefetch / pausable 路径可能存在边界 case 冲突 → **缓解**：Commit 6 采用 merge 而非覆盖整文件；保留 movableContent / Sticky Header 等定制；Phase 1 验证重点 review pausable + 重用交互；`cancelAll` + dispose cleanup 兜底。
- **R5**: PausableComposition 在 K/Native 上的实际线程行为没有 K/N 上的官方测试覆盖（CMP 1.9 的 pause runtime 测试用例都跑在 JVM/iOS Native simulator 上）→ **缓解**：本次仅启用 prefetch 路径的 PausableComposition（不用于其他 SubcomposeLayout 用法），且 Kuikly 单 Pager 单线程的特性正好规避了 1.11 增强的线程安全场景；如 K/Native 上发现问题，可临时把 flag 切回 false。
- **R6**: vendor commit 一次性引入 ~3000 行代码，review 压力大 → **缓解**：commit 严格遵循"纯 vendor + package rename"，diff 可以与 `~/Git/Work/compose-multiplatform-core` 的 `release/1.9` 直接对照；commit message 记录 `git sha` + `sed` 命令，可重放。
- **R7**: Compose plugin `1.7.3 → 1.9.3` + Gradle 8.2 + AGP 8.2.2 带来构建链与 runtime 变化（strong skipping、Profiler API、`RecomposeScopeObserver` 等）→ **缓解**：独立 Commit 2b 便于回滚；2b 内含 `ProfilerCompositionObserver` runtime 1.9 适配；灰度期先全关 prefetch 验证构建链，再开 Modifier opt-in。
