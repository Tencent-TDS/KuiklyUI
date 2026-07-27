## Context

**DSL 模式**：本 change 仅适用于 **Compose DSL**（`compose/` 模块）。不涉及自研 DSL（`core/` 的 `Pager`/`body()`）。

Kuikly Compose 的 Pager 是**双层状态**模型：Compose 层 `PagerState` 负责逻辑页码与 measure，Native `ScrollerView` 负责真实滚动与 spring 动画。滑动事件链路（见 `.ai/references/pager-compose-native-architecture.md` §2）：

```
用户拖拽 → Native ScrollerView 更新 contentOffset
  → SubcomposeLayout scroll 回调 → kuiklyOnScroll → isScrollingState=true
  → 用户松手 → kuiklyWillDragEnd → markSnapAnimationStarted → setContentOffset(animated=true)
  → native snap 动画运行（与 Compose recomposition / measure 并行）
  → scrollEnd（仅触摸手势结束回调）→ kuiklyOnScrollEnd → isScrollingState=false
```

当前两个派生状态在该链路上有缺陷（issue #1560）：

1. **`settledPage`**（`PagerState.kt:1168-1174`，`DrawerInternalPagerState.kt:390-392`）：
   ```kotlin
   val settledPage by derivedStateOf(structuralEqualityPolicy()) {
       if (isScrollInProgress) settledPageState else this.currentPage
   }
   ```
   手指松手那一刻 `isScrollInProgress` 已变 false，但 native snap 动画仍在运行，期间对齐修正逻辑反复改写 `currentPage`（可能短暂为 0 或旧值）。`settledPage` 走 `else` 读 `currentPage` → 跳变（安卓 `2→0→3`、iOS `0→2→0`）。`isSnapAnimating`（`PagerState.kt:422`）当前是普通 `var`，**无法驱动 `derivedStateOf` 重算**。

2. **`isScrollInProgress`**（iOS 静止卡 true）：`scrollEnd` 仅触摸手势结束回调（`SubcomposeLayout.kt:310` 注释明确）。但 `scroll` 事件在非手势 `setContentOffset`（初始化同步 / snap 对齐修正 / bounce 回弹）时也触发，而 `KuiklyScrollableState.kuiklyOnScroll`（`KuiklyScrollableState.kt:74-81`）**无条件** `isScrollingState.value = true`：
   ```kotlin
   fun kuiklyOnScroll(pixels: Float): Float {
       isScrollingState.value = true   // ← 非手势 scroll 也强制置 true
       ...
   }
   ```
   这些非手势 scroll 没有配对 `scrollEnd` → `isScrollInProgress` 永久卡 true。iOS 进入页面跑一次对齐修正 `setContentOffset`，故**初始状态即为 true**。

**NativeBridge / 跨平台通信**：本 change 不引入新的 NativeBridge 交互。`kuiklyInfo.isDragging`（`KuiklyScrollInfo.kt:99`，`mutableStateOf(false)`）已是既有可观察字段，由 `scroll` / `dragEnd` 回调从 native 同步（`SubcomposeLayout.kt:321,346`：`kuiklyInfo.isDragging = kuiklyInfo.scrollView?.isDragging ?: false`），修复直接复用，不新增 bridge 字段。

**约束**：
- `compose/` 是纯 KMP 模块，禁止依赖 `core-render-*`；修复在 `compose/` commonMain 完成，各 renderer 不动。
- `KuiklyScrollableState` 被所有 scrollable 容器共用（LazyList / LazyGrid / ScrollState / PullToRefresh / Pager），`kuiklyOnScroll` 改动需全局回归。
- `PagerState.kt` 含 Kuikly 独有逻辑（`DefaultPagerState.Saver` bridge state、`RecompositionProfiler`），**禁止整文件覆盖**，只做最小 hunk 修改（见架构文档 §6）。

## Goals / Non-Goals

**Goals:**
- `settledPage` 在「手指松开 → native snap 未结束」窗口返回缓存值 `settledPageState`，不跳变；snap 结束返回最终落地页。
- `isSnapAnimating` 可观察化，使 `settledPage` 的 `derivedStateOf` 在 snap 起止时重算。
- `clearSnapTrackingAfterAlignment()` 同步 `settledPageState = currentPage`，确保 snap 结束缓存正确。
- iOS（及所有平台）`isScrollInProgress` 静止态归 false；非手势 `setContentOffset` 不误置 true。
- 保留 touch 拖拽、`animateScrollToPage` 程序化动画期间 `isScrollInProgress` 正确为 true。
- `PagerState` 与 `DrawerInternalPagerState` 同步修复（同构）。

**Non-Goals:**
- 不改 snap 目标页计算逻辑（`kuiklyWillDragEnd` 决策、`relocateSnapTargetByKey`、keep-key settle）。
- 不改 `currentPage` / `targetPage` 语义。
- 不改帧调度 / VSync / measure 频率。
- 不改 HarmonyOS / Web / 小程序 renderer（共享 commonMain 收敛，不作验收平台）。
- 不新增 / 不移除对外 API 签名。

## Decisions

### 决策 1：`isSnapAnimating` 改为可观察状态

**选择**：把 `PagerState.isSnapAnimating`（`PagerState.kt:422`）从 `internal var isSnapAnimating = false` 改为：
```kotlin
internal var isSnapAnimating by mutableStateOf(false)
```
**理由**：`settledPage` 与 `targetPage` 都是 `derivedStateOf(structuralEqualityPolicy())`，其输入必须是可观察状态才能在变更时重算。当前 `isSnapAnimating` 是普通 `var`，`derivedStateOf` 读它时不会建立依赖，导致 snap 起止时 `settledPage` 不重算。只改 `PagerState`（`HorizontalPager`/`VerticalPager` 所用），不改 `DrawerInternalPagerState`——后者是独立的抽屉分页状态，issue #1560 未涉及，避免扩大改动面（见决策 5）。

**备选**：
- *A. 用单独的 `mutableStateOf` 包装类字段*：引入 `private val isSnapAnimatingState = mutableStateOf(false)` + 属性委托。等价但多一个字段，无额外收益。
- *B. 不改 `isSnapAnimating`，改用 `isScrollInProgress` 一个信号*：不可行——手指松手后 `isScrollInProgress` 已 false，但 snap 仍运行，正是这个窗口需要覆盖；且 `isScrollInProgress` 翻 false 与 `markSnapAnimationStarted` 不在同一原子操作，松手→snap 开始之间存在时序窗口仍可能读到中间态 `currentPage`。
- *C. 用 `derivedStateOf` 直接组合 `isScrollInProgress || isSnapAnimating` 作为新派生态*：可行但 `isSnapAnimating` 仍需可观察才能进入派生，本质同决策 1。

**采用委托 `by mutableStateOf`**（决策 1 主体），最小改动，与文件内既有 `mutableStateOf` / `mutableIntStateOf` 风格一致（如 `settledPageState`、`programmaticScrollTargetPage`）。

### 决策 2：`settledPage` 判定加入 `isSnapAnimating`

**选择**：`PagerState.kt:1168-1174`：
```kotlin
val settledPage by derivedStateOf(structuralEqualityPolicy()) {
    if (isScrollInProgress || isSnapAnimating) {
        settledPageState
    } else {
        this.currentPage
    }
}
```
**理由**：snap 动画窗口（`isScrollInProgress=false && isSnapAnimating=true`）期间 `currentPage` 可能被对齐修正改写为中间态，此时应返回缓存值 `settledPageState`。`isSnapAnimating` 现可观察（决策 1），`derivedStateOf` 会正确重算。

**备选**：
- *A. 用 `targetPage` 作 snap 窗口的返回值*：`targetPage` 在 snap 窗口已能算出最终目标页，但 `targetPage` 在程序化动画走 `programmaticScrollTargetPage` 分支、手势走 fraction 分支，语义与「已 settle 的页」不同；`settledPage` 文档语义是「animation/scroll settles 时更新的页」，用缓存 `settledPageState` 更贴合且无歧义。

### 决策 3：`clearSnapTrackingAfterAlignment()` 同步缓存

**选择**：`PagerState.kt:1019` 的 `clearSnapTrackingAfterAlignment()` 开头补充：
```kotlin
private fun clearSnapTrackingAfterAlignment() {
    settledPageState = currentPage   // ← 新增：snap 真正结束时缓存同步到最终页
    isSnapAnimating = false
    // ... 其余清零逻辑不变
}
```
**理由**：`settledPageState` 仅在 `scroll()` 入口更新（`PagerState.kt:1433-1435`：`if (!isScrollInProgress) settledPageState = currentPage`）。snap 窗口期间 `settledPage` 返回旧缓存（决策 2），snap 结束 `isSnapAnimating` 翻 false 后 `settledPage` 走 `else` 读 `currentPage`——但若此时 `currentPage` 因 measure 时序尚未更新到最终页，仍有瞬时旧值风险。在 `clearSnapTrackingAfterAlignment` 显式把缓存同步到 `currentPage`（此刻已是最终落地页），保证 `isSnapAnimating` 翻 false 后 `settledPageState` 与 `currentPage` 一致，无跳变。

**位置选择**：放在 `clearSnapTrackingAfterAlignment` 而非 `scrollEnd`——因为 snap 结束不一定经由 `scrollEnd`（iOS 非手势 snap 无 `scrollEnd`），而 `clearSnapTrackingAfterAlignment` 是所有 snap 收尾的统一出口（`alignScrollViewOffset` 决策树多个分支都会调用，见架构文档 §4.3）。

### 决策 4：`kuiklyOnScroll` 的 `isScrollingState` 置位条件

**选择**：`KuiklyScrollableState.kt:74-81`：
```kotlin
fun kuiklyOnScroll(pixels: Float): Float {
    // 仅在「原生正在拖拽」或「本来已处于滚动中」时维持 true，
    // 避免非手势 setContentOffset（初始化同步 / snap 对齐修正 / bounce 回弹）误置滚动状态
    if (kuiklyInfo.isDragging || isScrollingState.value) {
        isScrollingState.value = true
    }
    if (pixels.isNaN()) return 0f
    val delta = onDelta(pixels)
    isLastScrollForwardState.value = delta > 0
    isLastScrollBackwardState.value = delta < 0
    return delta
}
```
**理由**：
- `kuiklyInfo.isDragging`（来自 native `scrollView?.isDragging`）为真 → 真实触摸拖拽，应置 true。
- `isScrollingState.value` 已为真 → 程序化动画（`scroll()` 协程路径已在入口置 true）或正在进行的滚动，维持 true 不被非手势 scroll 中断误清。
- 两者皆否 → 非手势 `setContentOffset`（初始化 / 对齐 / bounce），**不置 true**。

**为何不直接 `false`**：程序化 `animateScrollToPage` 走 `scroll()` → `scrollMutex.mutateWith` → 入口 `isScrollingState.value = true`，期间 native 也会发 `scroll` 事件回调 `kuiklyOnScroll`，若强制不置 true 不影响（已 true）；但若某平台程序化动画不经 `scroll()` 协程而纯靠 native `setContentOffset`，`isScrollingState` 不会被入口置位——此时靠「本来已滚动中」无法覆盖。权衡后用 `isDragging || alreadyScrolling` 双兜底，覆盖最广且不误置静止态。

**备选**：
- *A. 直接 `isScrollingState.value = kuiklyInfo.isDragging`*：会清掉程序化动画中靠 `scroll()` 入口置的 true（若 native scroll 事件在 isDragging=false 时到达），破坏 `animateScrollToPage` 语义。否决。
- *B. 新增 `isProgrammaticScrolling` 标志区分手势 / 程序化*：侵入大，且 `scroll()` 协程路径已自带 `isScrollingState` 生命周期管理，无需重复。否决。
- *C. 在 `scrollEnd` 之外为非手势 snap 补 `kuiklyOnScrollEnd`*：需在 `alignScrollViewOffset` / bounce 各出口补回调，散落易漏；且 iOS bounce 回弹是 native 行为，Compose 侧难精确 hook 结束时机。决策 4 从源头不误置更稳健。

**回归范围**：`KuiklyScrollableState` 共用于 LazyList / LazyGrid / ScrollState / PullToRefresh / Pager。PullToRefresh 依赖 `scrollView?.isDragging`（即 `kuiklyInfo.isDragging`）判定拖拽——决策 4 在 `isDragging=true` 时仍置 true，PullToRefresh 不受影响。LazyList 静止态 `isScrollInProgress` 收敛为 false（修正前可能卡 true，与 Pager 同根因）。

### 决策 5：仅修改 `PagerState`，不改 `DrawerInternalPagerState`

**选择**：本 change 只改 `PagerState`（决策 1/2/3），**不**同步修改 `DrawerInternalPagerState`。

**理由**：
- `DrawerInternalPagerState` 是独立的 `abstract class ... : ScrollableState`，**不继承 `PagerState`**，有自己单独的 `isSnapAnimating`（`DrawerInternalPagerState.kt:249`）与 `settledPage`（`DrawerInternalPagerState.kt:390`），二者字段互不共享。改 `PagerState` 不会影响 drawer。
- issue #1560 与 OnCall 报告仅涉及 `HorizontalPager`（走 `PagerState`），未报告 drawer 有 `settledPage` / `isScrollInProgress` 问题。drawer 是抽屉分页，业务场景与 `HorizontalPager` 不同，未必依赖 `settledPage`。
- 避免扩大改动面与回归面：drawer 的 snap 行为是否与 `PagerState` 完全同构未经核实，贸然同构修改可能引入未经验证的行为变化。

**取舍**：若未来发现 drawer 也有同类 bug，可单独提 change 修复。本 change 保持最小改动，对应 issue。

## File Changes (grouped by module)

### `compose/`（核心改动，全部 commonMain）

| 文件 | 改动 |
|------|------|
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/pager/PagerState.kt` | 决策 1：`isSnapAnimating` 改 `by mutableStateOf(false)`（~L422）。决策 2：`settledPage` 判定加 `\|\| isSnapAnimating`（~L1168-1174）。决策 3：`clearSnapTrackingAfterAlignment()` 开头加 `settledPageState = currentPage`（~L1019）。 |
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/gestures/KuiklyScrollableState.kt` | 决策 4：`kuiklyOnScroll` 的 `isScrollingState.value = true` 改为 `if (kuiklyInfo.isDragging \|\| isScrollingState.value) { isScrollingState.value = true }`（~L74-81）。 |

### `compose/` — 不修改（刻意收敛）

| 文件 | 不改原因 |
|------|---------|
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/drawer/DrawerInternalPagerState.kt` | 决策 5：独立类，issue 未涉及，避免扩大改动面。 |

### `core/`、`core-render-*`、`core-annotations/`、`core-ksp/`、`demo/`

不修改。（可选：用 `demo/` 既有 `HorizontalPagerDemo3` 验证，但不在本 change 改 demo 代码。）

## Risks / Trade-offs

- **[风险] `KuiklyScrollableState` 共用导致 LazyList / PullToRefresh 回归** → 缓解：决策 4 保留 `isDragging || alreadyScrolling` 双兜底；PullToRefresh 依赖的 `isDragging` 路径不变；LazyList 静止态 `isScrollInProgress` 收敛为 false 是正确方向（同根因修正）。tasks 中列入回归验证。
- **[风险] `isSnapAnimating` 改可观察后在 measure / draw 中触发额外重组** → 缓解：仅 `settledPage` / `targetPage` 的 `derivedStateOf` 读取它，与既有派生链路一致；`structuralEqualityPolicy()` 保证值未变时不重组。`isSnapAnimating` 写入点（`markSnapAnimationStarted` / `clearSnapTrackingAfterAlignment` / `clearSnapAnimationState`）均在主线程 UI 路径，无并发。
- **[风险] `clearSnapTrackingAfterAlignment` 同步 `settledPageState = currentPage` 时 `currentPage` 暂未更新到最终页** → 缓解：`clearSnapTrackingAfterAlignment` 在 align 决策树末端调用，此刻 `currentPage` 已是对齐后的最终落地页（参见架构文档 §4.3 决策树，`positionCorrupted` / `itemsChangedDuringSnap` 等分支已 re-base 完成）；即使有极端时序，决策 2 的 `isSnapAnimating=true` 窗口仍返回缓存，下一次 `derivedStateOf` 重算会读到正确值。
- **[风险] iOS bounce 回弹期间 `isScrollInProgress` 应否为 true 存在语义争议** → 决策：bounce 回弹是 native 驱动的非手势动画，`isDragging=false`，按决策 4 不置 true。这与「静止态归 false」目标一致；业务若需区分 bounce，应观察 `currentPageOffsetFraction` 而非 `isScrollInProgress`。文档化此取舍。
- **[权衡] 未为非手势 snap 补 `scrollEnd` 回调（决策 4 选 C 否决）** → 从源头不误置比在各出口补回调更稳健、更少散落点；代价是 `isScrollInProgress` 在非手势 snap / bounce 期间为 false（符合目标语义）。

## Migration Plan

- 纯 SDK 内部行为收敛，无业务 API 签名变更，无数据迁移。
- 发布：打 tag 后 CI 自动发版（见架构文档 §6），无需本地 `publishToMavenLocal`。
- 灰度：建议先在 demo（`HorizontalPagerDemo3` / `VerticalPagerDemo`）双端验证 `settledPage` / `isScrollInProgress` 序列，再宿主 mavenLocal 集成回归 LazyList / PullToRefresh。
- 回滚：revert 三个文件 hunk 即可恢复原行为（`settledPage` 跳变 / iOS 卡 true 回来），无残留状态。

## Open Questions

- 是否需要在 `demo/` 新增一个专门的 `settledPage` / `isScrollInProgress` 观测页（类似 OnCall 报告中的最小示例）用于长期回归？本期倾向复用 `HorizontalPagerDemo3` + 临时日志，不新增 demo 页，避免引入与 change 无关的 demo 改动。
