## ADDED Requirements

### Requirement: LazyList prefetch SHALL be disabled by default at the global level

The vendored `ComposeFoundationFlags.isLazyListPrefetchEnabled` SHALL be added (Kuikly-specific flag, not present in upstream CMP) and SHALL default to `false`. When the flag is `false` and no opt-in Modifier is applied, every `LazyColumn` / `LazyRow` SHALL behave identically to the pre-change Kuikly: no prefetch SHALL be scheduled, no native view SHALL be created ahead of viewport entry, and `LaunchedEffect(Unit)` / `DisposableEffect.onAttach` on item content SHALL fire only when the item enters the viewport.

#### Scenario: Vendor commit with default flag does not prefetch

- **WHEN** the change is merged and a `LazyColumn` is used without `Modifier.enableLazyListPrefetch()` and `ComposeFoundationFlags.isLazyListPrefetchEnabled` remains its default value `false`
- **THEN** no `PrefetchRequest` SHALL ever be enqueued, the `LaunchedEffect(Unit)` inside each item SHALL fire only on viewport entry, and observable scroll performance and effect-timing SHALL be equivalent to the pre-change Kuikly baseline.

#### Scenario: Global flag flipped to true acts as kill switch in the on direction

- **WHEN** an operator (or developer) sets `ComposeFoundationFlags.isLazyListPrefetchEnabled = true` at app startup before any `LazyColumn` composes
- **THEN** every subsequent `LazyColumn` / `LazyRow` that does NOT explicitly disable prefetch via `Modifier.enableLazyListPrefetch(enabled = false)` SHALL behave as if `Modifier.enableLazyListPrefetch()` was applied (i.e. prefetch IS enabled).

#### Scenario: Global flag flipped back to false acts as kill switch in the off direction

- **WHEN** `ComposeFoundationFlags.isLazyListPrefetchEnabled` is set back to `false` after some prefetched items already exist in the queue
- **THEN** no new prefetch requests SHALL be enqueued for any LazyList that relies on the global flag (no explicit Modifier override), and existing in-flight prefetch requests MAY complete or be cancelled (implementation choice; the requirement is "no new enqueues").

---

### Requirement: Business code SHALL opt-in single LazyList prefetch via `Modifier.enableLazyListPrefetch()`

A new Kuikly-specific Modifier `Modifier.enableLazyListPrefetch(enabled: Boolean = true)` SHALL be added. Applying this modifier to a `LazyColumn` / `LazyRow` SHALL override the global flag for that single LazyList: passing `enabled = true` enables prefetch regardless of the global flag, and passing `enabled = false` disables prefetch regardless of the global flag.

The effective decision SHALL follow:
```
effectiveEnabled = modifierLevel ?: globalFlag
```
where `modifierLevel` is the value passed to `Modifier.enableLazyListPrefetch()` if the modifier is present, otherwise `null` (fall through to global flag).

#### Scenario: Modifier opt-in overrides default-off global flag

- **WHEN** `ComposeFoundationFlags.isLazyListPrefetchEnabled = false` (default) AND a specific `LazyColumn` applies `Modifier.enableLazyListPrefetch()` (i.e. `enabled = true`)
- **THEN** ONLY this `LazyColumn` SHALL run prefetch; other LazyLists in the same Pager without the modifier SHALL NOT run prefetch.

#### Scenario: Modifier opt-out overrides on global flag

- **WHEN** `ComposeFoundationFlags.isLazyListPrefetchEnabled = true` AND a specific `LazyColumn` applies `Modifier.enableLazyListPrefetch(enabled = false)`
- **THEN** this `LazyColumn` SHALL NOT run prefetch even though the global flag is on; other LazyLists without the modifier SHALL continue running prefetch.

#### Scenario: Multiple Pagers each have their own scheduler

- **WHEN** two `ComposeContainer` instances (two Pagers) are alive simultaneously, each running on its own coroutine dispatcher (`ComposeDispatcher(pagerId)`)
- **THEN** each Pager SHALL have its own `KuiklyPrefetchScheduler` instance owned by its `ComposeSceneMediator`, with no shared state between them, and no locking required (the scheduler's queue is only touched by its own Pager's render thread).

---

### Requirement: LazyList SHALL support idle-time prefetch on Android and iOS Native (when enabled)

Once a `LazyColumn` / `LazyRow` is enabled via the Modifier or global flag, the underlying `LazyLayout` + `LazyListState` SHALL schedule pre-composition and pre-measurement of upcoming items into a `KuiklyPrefetchScheduler`. The scheduler SHALL consume queued prefetch requests within the idle window of the current frame on platforms compiled to Kotlin/Native (Android and iOS Native), so that those items are fully composed, measured, and have their native render views created before they enter the viewport.

#### Scenario: Default 1-item prefetch runs on Android while scrolling, after opt-in

- **WHEN** the user scrolls a `LazyColumn` that uses `rememberLazyListState()` AND has `Modifier.enableLazyListPrefetch()` applied, on Android (Kotlin/Native target)
- **THEN** the immediate next off-screen item in the scroll direction SHALL be pre-composed and pre-measured during the same frame's idle window, and its underlying `KuiklyView` / native Android view SHALL be created before it scrolls into the viewport, with no observable frame drop attributable to that item's first appearance.

#### Scenario: Default 1-item prefetch runs on iOS Native while scrolling, after opt-in

- **WHEN** the user scrolls a `LazyColumn` that uses `rememberLazyListState()` AND has `Modifier.enableLazyListPrefetch()` applied, on iOS Native (Kotlin/Native target)
- **THEN** the immediate next off-screen item in the scroll direction SHALL be pre-composed and pre-measured during the same frame's idle window, and its underlying `KuiklyView` / native UIView SHALL be created before it scrolls into the viewport.

#### Scenario: Prefetch cancels when scroll direction reverses

- **WHEN** an opted-in `LazyColumn` has a scheduled prefetch for item `N + 1` (while scrolling forward) but the user reverses scroll direction before `N + 1` becomes visible
- **THEN** the scheduler SHALL cancel the pending prefetch for `N + 1` (via `resetPrefetchState()`), and SHALL schedule a new prefetch in the new scroll direction.

---

### Requirement: Business code SHALL be able to opt into `LazyLayoutCacheWindow` prefetch strategy

Business code SHALL be able to call `rememberLazyListState(cacheWindow = LazyLayoutCacheWindow(ahead = ..., behind = ...))` to switch the prefetch strategy from the default 1-item behavior to a window-based behavior that prefetches multiple items ahead of the viewport and retains multiple items behind it without disposing. CacheWindow SHALL only take effect if prefetch is enabled (via `Modifier.enableLazyListPrefetch()` or the global flag); otherwise CacheWindow SHALL be inert.

#### Scenario: CacheWindow prefetches multiple items ahead on Android (when enabled)

- **WHEN** `rememberLazyListState(cacheWindow = LazyLayoutCacheWindow(ahead = 500.dp, behind = 500.dp))` is used in a `LazyColumn` that ALSO has `Modifier.enableLazyListPrefetch()` applied, on Android, and the user scrolls forward
- **THEN** all items whose offset falls within `500.dp` past the viewport bottom edge SHALL be progressively scheduled for prefetch (composed, measured, native view created) across one or more frame idle windows, until either the window is fully populated or the scheduler runs out of frame budget.

#### Scenario: CacheWindow retains items behind the viewport (when enabled)

- **WHEN** the user scrolls past items that are now within the configured `behind` window AND prefetch is enabled on this LazyList
- **THEN** those items SHALL NOT be disposed by `LazyLayout`'s normal reuse logic until they fall outside the behind window.

#### Scenario: CacheWindow is inert when prefetch is not enabled

- **WHEN** `rememberLazyListState(cacheWindow = ...)` is used but neither `Modifier.enableLazyListPrefetch()` nor the global flag is set to `true`
- **THEN** no prefetch SHALL be scheduled; `cacheWindow` parameter SHALL effectively be ignored at runtime (the LazyList behaves as if `cacheWindow = null`).

---

### Requirement: Prefetch SHALL be a no-op on Kotlin/JS targets

On targets compiled to Kotlin/JS (covering H5, mini-program, and iOS JS dynamic-loading mode), the `expect/actual` factory `createDefaultKuiklyPrefetchScheduler()` SHALL return a `NoOpPrefetchScheduler` whose `schedulePrefetch` method does nothing. Additionally, `LazyListState.prefetchState` SHALL be `null` on Kotlin/JS targets (gated by an `expect val isPrefetchSupported: Boolean = false`), so that the `LazyListPrefetchStrategy.onScroll` hot path is entirely skipped and no `Long` arithmetic is exercised at runtime.

#### Scenario: iOS JS dynamic-loading scrolls without invoking prefetch hot path

- **WHEN** the user scrolls a `LazyColumn` inside a Kuikly Pager running in iOS JS dynamic-loading mode (Kotlin/JS target)
- **THEN** no prefetch request SHALL be enqueued, no `availableTimeNanos` arithmetic SHALL execute on `Long` values, and observable behavior SHALL be identical to the pre-change Kuikly (items are composed/measured/native-view-created lazily, on demand, in the same frame they become visible).

#### Scenario: H5 and mini-program scrolls without invoking prefetch hot path

- **WHEN** the user scrolls a `LazyColumn` in a Kuikly Pager running in H5 or mini-program mode (Kotlin/JS target)
- **THEN** no prefetch SHALL occur and no performance regression SHALL be observable compared to the pre-change baseline.

---

### Requirement: `KuiklyPrefetchScheduler` SHALL run on the same coroutine thread as `BaseComposeScene.render()` and SHALL be invoked at the tail of `render()`

The scheduler SHALL be invoked from `BaseComposeScene.render(canvas, nanoTime)` after `draw(KuiklyCanvas())`, on the same Pager-bound coroutine dispatcher (`ComposeDispatcher(pagerId)`). The scheduler SHALL receive the frame's `nanoTime` as the frame start time, SHALL use a hard-coded `frameIntervalNs = 16_666_667L` (60fps), SHALL compute `nextFrameTimeNs = nanoTime + frameIntervalNs`, and SHALL expose `availableTimeNanos() = max(0, nextFrameTimeNs - System.nanoTime())` per the official `PrefetchRequestScope` contract.

#### Scenario: Prefetch runs after draw, in the same render() call

- **WHEN** `BaseComposeScene.render()` is invoked by `ComposeSceneMediator.renderFrame()` during a VSync tick
- **THEN** the order of operations SHALL be: `performScheduledTasks()` → `frameClock.sendFrame()` → `doLayout()` → `performScheduledEffects()` → input handling → `snapshotInvalidationTracker.onDraw()` → `draw(KuiklyCanvas())` → `prefetchScheduler.processRequests(nanoTime, 16_666_667L, isFrameIdle)`.

#### Scenario: `availableTimeNanos` returns frame-based deadline when not idle

- **WHEN** `processRequests` runs while `vsyncTickConditions.needsToBeProactive == true` (user is actively interacting)
- **THEN** the active `PrefetchRequestScope.availableTimeNanos()` SHALL return `max(0, (nanoTime + 16_666_667L) - System.nanoTime())`, matching the official `AndroidPrefetchScheduler` semantics for non-idle frames.

#### Scenario: `availableTimeNanos` returns `Long.MAX_VALUE` when idle

- **WHEN** `processRequests` runs and `nanoTime > lastFrameDrawNanoTime + 2 * frameIntervalNs` (aligned with `AndroidPrefetchScheduler` idle detection)
- **THEN** `isFrameIdle` SHALL be `true` and `availableTimeNanos()` SHALL return `Long.MAX_VALUE`, allowing the scheduler to fully drain the queue without per-frame time pressure.

#### Scenario: Per-request budget gate matches official `runRequest`

- **WHEN** `runRequest()` is invoked and `availableTimeNanos() <= 0` while `isFrameIdle == false`
- **THEN** the scheduler SHALL NOT call `execute()` this frame, SHALL return `scheduleForNextFrame = true`, and `BaseComposeScene` SHALL call `needRedraw()` for the next VSync (equivalent to official `Choreographer.postFrameCallback`).

---

### Requirement: PausableComposition SHALL be enabled for prefetch after build-chain and SubcomposeLayout prerequisites

Kuikly SHALL enable prefetch-time PausableComposition by setting `ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled` to `true` (Commit 7), matching CMP `release/1.11` default behavior. This SHALL only take effect after:

1. **Commit 2b**: Gradle wrapper ≥ **8.2**, AGP ≥ **8.2.2**, `org.jetbrains.compose` plugin **1.9.3**, and `compose.runtime` **1.9.3** (minimum compatible stack per design D10; not latest Gradle/AGP).
2. **Commit 6**: Kuikly `SubcomposeLayout` exposes a real `createPausedPrecomposition()` / `precomposePaused()` implementation (not the Commit 3 `EagerPausedPrecomposition` stub).

Until both prerequisites land, `isPausableCompositionInPrefetchEnabled` SHALL remain `false` and prefetch SHALL use `performFullComposition` / ordinary `precompose()`.

#### Scenario: A complex item's prefetch composition can span multiple frames

- **WHEN** Commit 2b and Commit 6 are complete, a opted-in `LazyColumn` item's composable body takes longer than the current frame's `availableTimeNanos`, and `isPausableCompositionInPrefetchEnabled == true`
- **THEN** the partial composition SHALL pause at a safe checkpoint, and on subsequent frames the composition SHALL resume from the same checkpoint until complete (via real `PausedPrecomposition.resume`, not a one-shot eager stub).

#### Scenario: Pausable flag remains off until prerequisites are met

- **WHEN** Commit 2b or Commit 6 is not yet merged
- **THEN** `isPausableCompositionInPrefetchEnabled` SHALL be `false`, and prefetch (if enabled via Modifier/global flag) SHALL use the non-pausable `performFullComposition` path only.

---

### Requirement: Prefetch SHALL request the next frame while work remains (aligned with official `scheduleForNextFrame`)

When `processRequests` returns `scheduleForNextFrame == true` (paused request, or `availableTimeNanos() <= 0`), `BaseComposeScene` SHALL call `vsyncTickConditions.needRedraw()` so the next VSync re-enters prefetch—equivalent to official `Choreographer.postFrameCallback`. There SHALL be no `MAX_CONTINUATION_FRAMES` cap.

#### Scenario: Continuation after pause or zero budget

- **WHEN** `runRequest()` returns `hasMoreWork == true` OR `availableTimeNanos() <= 0` before `execute()`
- **THEN** `processRequests` SHALL return `scheduleForNextFrame = true` and `needRedraw()` SHALL be called for the next frame.

#### Scenario: No continuation when queue is drained

- **WHEN** all requests finish (`execute()` returned `false`) and the queue is empty
- **THEN** `scheduleForNextFrame` SHALL be `false` and `needRedraw()` SHALL NOT be called solely for prefetch.

---

### Requirement: Scheduler SHALL clear its queue on `ComposeSceneMediator.dispose()`

`ComposeSceneMediator.dispose()` SHALL call `prefetchScheduler.cancelAll()` before `scene.close()`. `cancelAll()` SHALL cancel every `PrefetchHandle` in the queue and clear all internal state, ensuring no `SubcomposeLayoutState` reference or other Composition-related object survives past the Pager's lifecycle.

#### Scenario: Disposing a Pager mid-prefetch does not leak

- **WHEN** a `ComposeContainer.pageWillDestroy()` is invoked while the prefetch queue contains active requests
- **THEN** `ComposeSceneMediator.dispose()` SHALL clear the queue (via `cancelAll()`), the underlying `SubcomposeLayoutState` references SHALL be released, and no `PrefetchHandle` SHALL survive into the next event loop iteration.

---

### Requirement: `RecompositionProfiler` SHALL report actual prefetch time per frame

`BaseComposeScene.render()` SHALL pass the actual prefetch time spent in this frame (returned by `KuiklyPrefetchScheduler.processRequests`) to `tracker.onFrameEnd(prefetchSpentNs)`, instead of the current hard-coded `0`. This allows the Profiler / business diagnostics to distinguish prefetch overhead from main-frame work.

#### Scenario: Profiler records prefetch time separately from main-frame time

- **WHEN** the Profiler is enabled and `BaseComposeScene.render()` runs a frame during which prefetch processed one or more requests
- **THEN** `tracker.onFrameEnd(prefetchSpentNs)` SHALL be called with `prefetchSpentNs` equal to the wall-clock time consumed by `prefetchScheduler.processRequests` (in nanoseconds), and the Profiler's frame record SHALL expose this value separately from total frame time.

#### Scenario: Profiler records zero prefetch time when no prefetch ran

- **WHEN** `processRequests` is invoked but the queue is empty (or the safety budget is not met)
- **THEN** `prefetchSpentNs` SHALL be `0` (or near-zero, accounting only for the early-return overhead), and the Profiler SHALL record `0` for that frame's prefetch.
