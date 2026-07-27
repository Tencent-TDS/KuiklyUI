## Why

`HorizontalPager` / `VerticalPager` 的 `PagerState` 在滑动过渡期与静止态暴露异常状态：`settledPage` 在「手指松开 → 原生 snap 动画未结束」窗口读到中间态 `currentPage` 而跳变（安卓 `2→0→3`、iOS `0→2→0`）；iOS 端 `isScrollInProgress` 在完全静止停留时仍为 `true`，因为初始化同步 / snap 对齐修正 / bounce 回弹触发的非手势 `setContentOffset` 会无条件把 `isScrollingState` 置 true，且这些 scroll 没有配对的 `scrollEnd` 回收。两个状态是业务侧判定「滚动静止」「最终落地页」的核心信号，异常会直接破坏 autoplay、曝光、页签联动等业务逻辑。该问题已在 issue [#1560](https://github.com/Tencent-TDS/KuiklyUI/issues/1560) 中由 OnCall 定位到双端根因，需在 SDK 层修复而非依赖业务 workaround。

## What Changes

- **根因 A（settledPage 跳变）**：`PagerState.settledPage` 的判定从 `if (isScrollInProgress)` 扩展为 `if (isScrollInProgress || isSnapAnimating)`，使「手指松开但原生 snap 未结束」窗口继续使用缓存值 `settledPageState`，避开中间态 `currentPage`。
- **根因 A 配套（isSnapAnimating 可观察化）**：把 `PagerState.isSnapAnimating` 从普通 `var` 改为可观察状态（`mutableStateOf`），使 `derivedStateOf(structuralEqualityPolicy()) { ... }` 在 snap 动画起止时能正确重算 `settledPage`。
- **根因 A 配套（缓存同步）**：在 `clearSnapTrackingAfterAlignment()` 中补充 `settledPageState = currentPage`，确保 snap 真正结束时缓存同步到最终落地页，而非停留在旧值。
- **根因 B（iOS isScrollInProgress 静止卡 true）**：`KuiklyScrollableState.kuiklyOnScroll` 不再无条件置 `isScrollingState = true`，改为只有「原生 `isDragging` 为真」或「本来已处于滚动中」时才维持 true，避免非手势 `setContentOffset`（初始化同步 / 对齐修正 / bounce 回弹）误置滚动状态。
- **回归保护**：保留 `scroll()` 协程路径（`animateScrollToPage` 等程序化动画）对 `isScrollInProgress` 的正确置位（该路径走 `scrollMutex.mutateWith`，与 `kuiklyOnScroll` 路径独立），并在 `animateScrollToPage` 程序化动画窗口维持 `settledPage` 语义不变。

### Non-goals

- **不**改动 Kuikly 帧调度 / VSync / measure 频率策略本身。
- **不**改动 snap 目标页计算逻辑（`kuiklyWillDragEnd` 的 `currentPage + direction` 决策、`relocateSnapTargetByKey`、keep-key settle 等保持原状）。
- **不**改动 `currentPage` 的语义（双端表现已正常）。
- **不**覆盖 LazyList / LazyGrid 的 prefetch、contentSize 双向同步等既有逻辑（仅 `KuiklyScrollableState` 的 `isScrollingState` 置位条件会受影响，需回归但不在本 change 改逻辑）。
- **不**修改 HarmonyOS / Web / 小程序 renderer 代码（根因在 `compose/` 共享 commonMain，修复对齐后这些平台随共享逻辑收敛；OHOS / Web 不作为本期验收平台）。
- **不**改动 `targetPage` 的派生逻辑（其已正确用 `isScrollInProgress` + `programmaticScrollTargetPage` 判定）。
- **不**移除或新增任何对外公开 API 签名（`settledPage` / `isScrollInProgress` 行为收敛到正确语义，签名不变）。

## Capabilities

### New Capabilities
- `pager-state-settled-scroll`: Pager（`PagerState`，即 `HorizontalPager`/`VerticalPager`）在滑动过渡期与静止态暴露的 `settledPage`、`isScrollInProgress` 两个派生状态的正确语义：snap 动画窗口下 `settledPage` 保持缓存值不跳变、snap 结束时缓存同步到最终页；非手势 `setContentOffset` 不误置 `isScrollInProgress`、静止态归 false。

### Modified Capabilities
- 无（`openspec/specs/` 下无既有 pager-state 相关 spec；`vforlazy-scroll-correction-stability` 仅覆盖自研 DSL LazyList，与本 Compose DSL Pager 状态语义无重叠）。

## Impact

- **受影响平台**：
  - **Android**：`settledPage` 过渡期跳变（`2→0→3`）修复；`isScrollInProgress` 因手势 `scrollEnd` 能正常回调，原本仅在过渡窗口异常，修复后过渡窗口也收敛。需回归 `animateScrollToPage` 程序化动画期间的 `isScrollInProgress` 语义。
  - **iOS**：本期重点修复平台。`isScrollInProgress` 静止态卡 true（初始化 / 对齐修正 / bounce）修复；`settledPage` 跳变（`0→2→0`）修复。
  - **HarmonyOS / Web / 小程序 / macOS**：共享 `compose/` commonMain 逻辑，随修复收敛；本期不作为验收平台，但需确认 `kuiklyOnScroll` 的 `isDragging` 兜底在这些平台不会引入回归（无 `isDragging` 时维持「本来已滚动中才置 true」语义）。
- **受影响模块**：
  - `compose/`：核心改动。
    - `compose/.../foundation/pager/PagerState.kt`（`isSnapAnimating` 可观察化、`settledPage` 判定、`clearSnapTrackingAfterAlignment` 缓存同步）。
    - `compose/.../gestures/KuiklyScrollableState.kt`（`kuiklyOnScroll` 的 `isScrollingState` 置位条件）。
  - `compose/.../foundation/drawer/DrawerInternalPagerState.kt`：**不修改**（独立类，issue #1560 未涉及 drawer，避免扩大改动面；见 design 决策 5）。
  - `core/`、`core-render-*`、`demo/`：不修改（可选：在 `demo/` 复现页验证，但非本 change 强约束）。
- **受影响 API（业务可见，行为收敛）**：
  - `PagerState.settledPage`：snap 动画窗口期间返回缓存值（不再跳变），snap 结束返回最终落地页。签名不变。
  - `PagerState.isScrollInProgress`（及所有 `ScrollableState.isScrollInProgress`）：非手势 `setContentOffset` 不再误置 true，静止态归 false。签名不变。
- **依赖**：不引入新依赖。
- **风险**：
  - `KuiklyScrollableState` 被所有 scrollable 容器共用（LazyList / LazyGrid / ScrollState / PullToRefresh / Pager），`kuiklyOnScroll` 置位条件改变需回归：`animateScrollToPage` 程序化动画期间的 `isScrollInProgress`、`targetPage` 语义，以及 PullToRefresh 对 `scrollView?.isDragging` 的依赖。缓解：保留「已处于滚动中则维持 true」与「`isDragging` 为真则置 true」两条兜底，程序化动画走 `scroll()` 协程路径不受影响。
  - `isSnapAnimating` 改为可观察状态后，所有读写点需确认无在非快照上下文（如 measure / draw）中触发额外重组的副作用。缓解：仅在 `derivedStateOf` 读取链路依赖它，与既有 `settledPage` / `targetPage` 派生一致。
