## MODIFIED Requirements

### Requirement: Compose DSL SHALL expose phase1 page-level IME insets as direct page state projection

Compose DSL SHALL continue to expose `WindowInsets.ime` as the page-level software keyboard inset source for the active page window. In the phase1 baseline, its effective bottom inset MUST reflect the current page-level IME height state used by Compose layout, MUST return to `0` when the keyboard is hidden, and MUST NOT depend on framework-internal target/projection dual state, non-linear local tweening, or native-progress-specific consumption semantics in order to satisfy the capability.

#### Scenario: Android phase1 IME inset works without animation projection helpers
- **GIVEN** an Android Compose page uses `WindowInsets.ime` or `Modifier.imePadding()`
- **WHEN** the software keyboard becomes visible or hidden for the current page window
- **THEN** the effective bottom inset SHALL update from the page-level IME height state without requiring target/projection dual state, `native-progress`, or local easing-driven animation projection

#### Scenario: iOS phase1 IME inset works without animation projection helpers
- **GIVEN** an iOS Compose page uses `WindowInsets.ime` or `Modifier.imePadding()`
- **WHEN** the software keyboard becomes visible or hidden for the current page window
- **THEN** the effective bottom inset SHALL update from the page-level IME height state without requiring target/projection dual state, `native-progress`, or local easing-driven animation projection

#### Scenario: HarmonyOS phase1 IME inset works without animation projection helpers
- **GIVEN** a HarmonyOS Compose page uses `WindowInsets.ime` or `Modifier.imePadding()`
- **WHEN** the software keyboard becomes visible or hidden for the current page window
- **THEN** the effective bottom inset SHALL update from the page-level IME height state without requiring target/projection dual state, `native-progress`, or local easing-driven animation projection

### Requirement: Compose phase1 IME event consumption SHALL remain compatible with `height / duration / curve` only

The phase1 `compose-ime-insets` capability SHALL remain correct when the page-level IME event payload contains only `height`, `duration`, and `curve`. `duration` and `curve` MAY continue to exist as internal reserved metadata, but the capability MUST NOT require `source`, `animatedHeight`, or source-specific fallback/native-progress branches in order to preserve correct `WindowInsets.ime`, `imePadding()`, and `Scaffold` behavior.

#### Scenario: Android phase1 IME behavior remains correct without `source` and `animatedHeight`
- **GIVEN** an Android renderer sends page-level IME events with `height`, `duration`, and `curve`
- **WHEN** a Compose page consumes `WindowInsets.ime`, `imePadding()`, or `Scaffold`
- **THEN** phase1 keyboard avoidance SHALL work correctly without requiring `source` or `animatedHeight` fields

#### Scenario: iOS phase1 IME behavior remains correct without `source` and `animatedHeight`
- **GIVEN** an iOS renderer sends page-level IME events with `height`, `duration`, and `curve`
- **WHEN** a Compose page consumes `WindowInsets.ime`, `imePadding()`, or `Scaffold`
- **THEN** phase1 keyboard avoidance SHALL work correctly without requiring `source` or `animatedHeight` fields

#### Scenario: HarmonyOS phase1 IME behavior remains correct without `source` and `animatedHeight`
- **GIVEN** a HarmonyOS renderer sends page-level IME events with `height`, `duration`, and `curve`
- **WHEN** a Compose page consumes `WindowInsets.ime`, `imePadding()`, or `Scaffold`
- **THEN** phase1 keyboard avoidance SHALL work correctly without requiring `source` or `animatedHeight` fields
