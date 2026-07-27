## ADDED Requirements

### Requirement: settledPage SHALL NOT change during the native snap settle window

After the user lifts the finger, Kuikly drives the native `setContentOffset(animated=true)` snap animation. During this window the Compose `currentPage` may be transiently rewritten by the alignment-correction logic (`alignScrollViewOffset` / `alignComposePositionToNativeBoundaryIfNeeded`) and can briefly read 0 or a stale value. The `settledPage` derived state MUST keep returning the cached `settledPageState` for the entire window where `isScrollInProgress` is false but `isSnapAnimating` is true, so that observers do not see an intermediate jump.

#### Scenario: Android settledPage does not jump to 0 during snap
- **GIVEN** an Android `HorizontalPager` with `pageCount = 6` resting at page 2
- **WHEN** the user drags from page 2 toward page 3, lifts the finger, and the native snap animation begins settling toward page 3
- **THEN** `settledPage` SHALL remain 2 throughout the snap window (finger lifted, `isSnapAnimating == true`, `isScrollInProgress == false`)
- **AND** `settledPage` SHALL transition directly to 3 once the snap settles, without an intermediate 0

#### Scenario: iOS settledPage does not jump during snap
- **GIVEN** an iOS `HorizontalPager` with `pageCount = 6` resting at page 2
- **WHEN** the user drags from page 2 toward page 3, lifts the finger, and the native snap animation begins settling toward page 3
- **THEN** `settledPage` SHALL remain 2 throughout the snap window (`isSnapAnimating == true`, `isScrollInProgress == false`)
- **AND** `settledPage` SHALL transition directly to 3 once the snap settles, without an intermediate 0 or a `0 → 2 → 0` sequence

### Requirement: settledPageState SHALL sync to the final landing page when snap tracking clears

When the snap settle completes and snap tracking is cleared (`clearSnapTrackingAfterAlignment`), the cached `settledPageState` MUST be synchronized to the current `currentPage` (the final landing page). This guarantees that the next time `settledPage` is observed (after `isSnapAnimating` flips to false), it reflects the real landing page rather than a stale value.

#### Scenario: Android settledPage reflects final page after snap clears
- **GIVEN** an Android `HorizontalPager` whose native snap toward page 3 has just finished
- **WHEN** `clearSnapTrackingAfterAlignment()` runs and `isSnapAnimating` becomes false
- **THEN** `settledPageState` SHALL equal `currentPage` (3)
- **AND** `settledPage` SHALL read 3 at rest

#### Scenario: iOS settledPage reflects final page after snap clears
- **GIVEN** an iOS `HorizontalPager` whose native snap toward page 3 has just finished
- **WHEN** `clearSnapTrackingAfterAlignment()` runs and `isSnapAnimating` becomes false
- **THEN** `settledPageState` SHALL equal `currentPage` (3)
- **AND** `settledPage` SHALL read 3 at rest

### Requirement: isSnapAnimating SHALL be observable so derived settledPage recomputes

`isSnapAnimating` MUST be backed by an observable Compose state (e.g. `mutableStateOf`) so that `derivedStateOf(structuralEqualityPolicy())` computing `settledPage` recomputes when the snap animation starts and ends. A plain `var` cannot drive `derivedStateOf` and would leave `settledPage` stale.

#### Scenario: Android settledPage recomputes when snap animation toggles
- **GIVEN** an Android `HorizontalPager` observing `settledPage` via a `derivedStateOf` reader (e.g. `LaunchedEffect(settledPage)`)
- **WHEN** `isSnapAnimating` flips from false to true (snap starts) and later from true to false (snap ends)
- **THEN** the `settledPage` `derivedStateOf` SHALL recompute on each flip
- **AND** observers of `settledPage` SHALL be notified of the recomputed value

#### Scenario: iOS settledPage recomputes when snap animation toggles
- **GIVEN** an iOS `HorizontalPager` observing `settledPage` via a `derivedStateOf` reader
- **WHEN** `isSnapAnimating` flips from false to true and later from true to false
- **THEN** the `settledPage` `derivedStateOf` SHALL recompute on each flip
- **AND** observers of `settledPage` SHALL be notified of the recomputed value

### Requirement: isScrollInProgress SHALL be false when the pager is at rest

`isScrollInProgress` MUST be false whenever the pager is completely at rest, including the initial mount and after non-gesture `setContentOffset` operations (initial sync, snap alignment correction, bounce rebound). Non-gesture `setContentOffset` MUST NOT set `isScrollInProgress` to true. Because native `scrollEnd` is only delivered for touch-gesture endings, scroll events originating from non-gesture `setContentOffset` MUST NOT unconditionally force the scrolling state to true.

#### Scenario: iOS isScrollInProgress is false on initial mount at rest
- **GIVEN** an iOS `HorizontalPager` that runs an alignment-correction `setContentOffset` during initial layout
- **WHEN** initial layout completes and the pager is resting at the initial page with no finger down
- **THEN** `isScrollInProgress` SHALL be false
- **AND** it SHALL NOT be true immediately after mount

#### Scenario: iOS isScrollInProgress is false after bounce rebound settles
- **GIVEN** an iOS `HorizontalPager` that overscrolls past an edge and triggers a native bounce rebound via `setContentOffset`
- **WHEN** the bounce rebound animation finishes with no finger down
- **THEN** `isScrollInProgress` SHALL be false at rest
- **AND** it SHALL NOT be stuck at true after the rebound

#### Scenario: Android isScrollInProgress is false after snap alignment correction
- **GIVEN** an Android `HorizontalPager` whose snap alignment correction triggers a non-gesture `setContentOffset`
- **WHEN** the correction completes and the pager is at rest with no finger down
- **THEN** `isScrollInProgress` SHALL be false at rest

### Requirement: isScrollInProgress SHALL remain true during touch-driven scrolling and programmatic animated scroll

The fix to gate `isScrollInProgress` MUST NOT regress touch-driven scrolling or programmatic animated scrolling (`animateScrollToPage`). Touch-driven scroll MUST keep `isScrollInProgress` true for the duration of the gesture. Programmatic animated scroll (driven through the `scroll()` coroutine / `scrollMutex` path) MUST keep `isScrollInProgress` true for the duration of the animation.

#### Scenario: Android isScrollInProgress true during touch drag
- **GIVEN** an Android `HorizontalPager` at rest
- **WHEN** the user touches and drags the pager
- **THEN** `isScrollInProgress` SHALL be true for the entire duration of the drag
- **AND** `isScrollInProgress` SHALL become false once the touch gesture ends and settling completes

#### Scenario: iOS isScrollInProgress true during touch drag
- **GIVEN** an iOS `HorizontalPager` at rest
- **WHEN** the user touches and drags the pager
- **THEN** `isScrollInProgress` SHALL be true for the entire duration of the drag
- **AND** `isScrollInProgress` SHALL become false once the touch gesture ends and settling completes

#### Scenario: Android isScrollInProgress true during animateScrollToPage
- **GIVEN** an Android `HorizontalPager` at rest on page 0
- **WHEN** `pagerState.animateScrollToPage(3)` is invoked
- **THEN** `isScrollInProgress` SHALL be true for the duration of the animated scroll
- **AND** `isScrollInProgress` SHALL become false when the animated scroll completes

#### Scenario: iOS isScrollInProgress true during animateScrollToPage
- **GIVEN** an iOS `HorizontalPager` at rest on page 0
- **WHEN** `pagerState.animateScrollToPage(3)` is invoked
- **THEN** `isScrollInProgress` SHALL be true for the duration of the animated scroll
- **AND** `isScrollInProgress` SHALL become false when the animated scroll completes

### Requirement: The scrolling-state gating SHALL apply to all scrollable containers sharing KuiklyScrollableState

Because `KuiklyScrollableState` is shared by LazyList, LazyGrid, ScrollState, PullToRefresh, and Pager, the `kuiklyOnScroll` scrolling-state gating change MUST preserve correct `isScrollInProgress` semantics for these containers. Non-gesture `setContentOffset` on any of these containers MUST NOT leave `isScrollInProgress` stuck true; touch-driven and programmatic scrolling MUST keep it true.

#### Scenario: LazyList isScrollInProgress false at rest after non-gesture setContentOffset
- **GIVEN** a `LazyColumn` (Android or iOS) resting at the top
- **WHEN** a non-gesture `setContentOffset` (e.g. layout-driven alignment) fires and completes with no finger down
- **THEN** `isScrollInProgress` of the list's scroll state SHALL be false at rest

#### Scenario: PullToRefresh still detects dragging via isDragging
- **GIVEN** a `PullToRefresh` container (Android or iOS) sharing `KuiklyScrollableState`
- **WHEN** the user drags down to pull-to-refresh
- **THEN** the container's drag detection (which relies on `scrollView?.isDragging`) SHALL continue to work
- **AND** `isScrollInProgress` SHALL be true during the drag and false at rest afterward
