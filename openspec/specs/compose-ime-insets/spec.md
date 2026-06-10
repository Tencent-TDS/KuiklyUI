## ADDED Requirements

### Requirement: Compose DSL SHALL expose page-level IME insets

Compose DSL SHALL expose `WindowInsets.ime` as a page-level window inset source. Its bottom inset MUST reflect the current software keyboard occupied height for the active page window, MUST return to `0` when the keyboard is hidden, and MUST NOT require a `TextField` or `TextArea` to register a component-level `keyboardHeightChange` callback before it can update.

#### Scenario: Android page-level IME inset updates without input callback registration
- **GIVEN** an Android Compose page uses `WindowInsets.ime` or `Modifier.imePadding()` on a container, and no business code has registered a component-level `keyboardHeightChange` callback
- **WHEN** the software keyboard becomes visible or hidden in the current page window
- **THEN** `WindowInsets.ime` SHALL update from the page-level keyboard state, and its bottom inset SHALL reflect the current keyboard height or `0` after dismissal

#### Scenario: iOS page-level IME inset updates without input callback registration
- **GIVEN** an iOS Compose page uses `WindowInsets.ime` or `Modifier.imePadding()` on a container, and no business code has registered a component-level `keyboardHeightChange` callback
- **WHEN** the software keyboard becomes visible or hidden in the current page window
- **THEN** `WindowInsets.ime` SHALL update from the page-level keyboard state, and its bottom inset SHALL reflect the current keyboard height or `0` after dismissal

#### Scenario: HarmonyOS page-level IME inset updates without input callback registration
- **GIVEN** a HarmonyOS Compose page uses `WindowInsets.ime` or `Modifier.imePadding()` on a container, and no business code has registered a component-level `keyboardHeightChange` callback
- **WHEN** the software keyboard becomes visible or hidden in the current page window
- **THEN** `WindowInsets.ime` SHALL update from the page-level keyboard state, and its bottom inset SHALL reflect the current keyboard height or `0` after dismissal

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
