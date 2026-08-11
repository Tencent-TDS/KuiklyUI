## ADDED Requirements

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

### Requirement: Compose DSL SHALL provide imePadding based on unconsumed IME insets

Compose DSL SHALL provide `Modifier.imePadding()`. This modifier MUST add bottom padding equal to the unconsumed bottom inset of `WindowInsets.ime`, and MUST participate in the existing insets consumption chain so descendants do not re-apply the same IME space.

#### Scenario: Android imePadding moves bottom input content above the keyboard
- **GIVEN** an Android Compose page has a bottom-aligned input bar that uses `Modifier.imePadding()`
- **WHEN** the software keyboard becomes visible
- **THEN** the input bar SHALL be laid out above the keyboard area, and descendant inset consumers SHALL observe the IME inset as consumed for the already-padded space

#### Scenario: iOS imePadding moves bottom input content above the keyboard
- **GIVEN** an iOS Compose page has a bottom-aligned input bar that uses `Modifier.imePadding()`
- **WHEN** the software keyboard becomes visible
- **THEN** the input bar SHALL be laid out above the keyboard area, and descendant inset consumers SHALL observe the IME inset as consumed for the already-padded space

#### Scenario: HarmonyOS imePadding moves bottom input content above the keyboard
- **GIVEN** a HarmonyOS Compose page has a bottom-aligned input bar that uses `Modifier.imePadding()`
- **WHEN** the software keyboard becomes visible
- **THEN** the input bar SHALL be laid out above the keyboard area, and descendant inset consumers SHALL observe the IME inset as consumed for the already-padded space

### Requirement: Material3 Scaffold SHALL include IME in its default content window insets

`ScaffoldDefaults.contentWindowInsets` SHALL include both the existing visual system bar insets and the current IME inset. When the keyboard is visible, the default `Scaffold` content padding MUST avoid the keyboard for content areas that rely on default `contentWindowInsets`, while preserving the existing system bar padding behavior.

#### Scenario: Android Scaffold default content avoids keyboard
- **GIVEN** an Android Compose page uses `Scaffold` with its default `contentWindowInsets`, and its content contains a bottom input area or form field region that applies the provided `PaddingValues`
- **WHEN** the software keyboard becomes visible
- **THEN** the content area SHALL receive bottom padding that includes the current IME inset in addition to the existing visual system bar inset semantics

#### Scenario: iOS Scaffold default content avoids keyboard
- **GIVEN** an iOS Compose page uses `Scaffold` with its default `contentWindowInsets`, and its content contains a bottom input area or form field region that applies the provided `PaddingValues`
- **WHEN** the software keyboard becomes visible
- **THEN** the content area SHALL receive bottom padding that includes the current IME inset in addition to the existing visual system bar inset semantics

#### Scenario: HarmonyOS Scaffold default content avoids keyboard
- **GIVEN** a HarmonyOS Compose page uses `Scaffold` with its default `contentWindowInsets`, and its content contains a bottom input area or form field region that applies the provided `PaddingValues`
- **WHEN** the software keyboard becomes visible
- **THEN** the content area SHALL receive bottom padding that includes the current IME inset in addition to the existing visual system bar inset semantics

### Requirement: Existing component-level keyboard callbacks SHALL remain available

The existing component-level `keyboardHeightChange` callback behavior on input components SHALL remain available after this change. Adding page-level IME insets MUST NOT remove or invalidate existing business code that still depends on manual keyboard height callbacks.

#### Scenario: Android legacy keyboard callback remains functional
- **GIVEN** an Android page still uses input-component `keyboardHeightChange` for manual keyboard coordination
- **WHEN** the keyboard height changes after the new page-level IME inset support is introduced
- **THEN** the existing callback SHALL continue to receive keyboard height updates usable by business code

#### Scenario: iOS legacy keyboard callback remains functional
- **GIVEN** an iOS page still uses input-component `keyboardHeightChange` for manual keyboard coordination
- **WHEN** the keyboard height changes after the new page-level IME inset support is introduced
- **THEN** the existing callback SHALL continue to receive keyboard height updates usable by business code

#### Scenario: HarmonyOS legacy keyboard callback remains functional
- **GIVEN** a HarmonyOS page still uses input-component `keyboardHeightChange` for manual keyboard coordination
- **WHEN** the keyboard height changes after the new page-level IME inset support is introduced
- **THEN** the existing callback SHALL continue to receive keyboard height updates usable by business code

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
