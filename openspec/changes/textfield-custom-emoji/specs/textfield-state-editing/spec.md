## ADDED Requirements

### Requirement: Core text input state payload
The system SHALL define a `TextInputState` payload containing raw text, selection start/end, optional composition start/end, and optional length metadata for communication between Compose DSL, self DSL, core views, and render views.

#### Scenario: Android reports text input state
- **WHEN** an Android `EditText` changes text or selection
- **THEN** the renderer SHALL emit raw text with selection start/end and composition metadata when available

#### Scenario: iOS reports text input state
- **WHEN** an iOS `UITextView` or `UITextField` changes text or selection
- **THEN** the renderer SHALL emit raw text with selection start/end and composition metadata when available

#### Scenario: HarmonyOS reports text input state
- **WHEN** a HarmonyOS text input changes text or selection
- **THEN** the renderer SHALL emit raw text with selection start/end and SHALL report empty composition when composition APIs are unavailable

#### Scenario: Web reports text input state
- **WHEN** a Web input or textarea receives input, selection, or composition events
- **THEN** the renderer SHALL emit raw text with selection start/end and composition metadata when available

#### Scenario: miniApp reports text input state
- **WHEN** a miniApp input receives text changes
- **THEN** the renderer SHALL emit raw text and best-effort selection metadata, with composition empty when unavailable

#### Scenario: macOS reports text input state
- **WHEN** macOS text input changes text or selection
- **THEN** the renderer SHALL emit raw text with selection start/end and composition metadata when available

### Requirement: Atomic text and selection updates
Core input views SHALL support setting raw text and selection atomically so programmatic updates do not move the cursor to the end unless requested.

#### Scenario: Android receives programmatic state
- **WHEN** Kotlin sets `TextInputState(text = "abc", selectionStart = 1, selectionEnd = 1)` on Android
- **THEN** the native input SHALL display `abc` and place the cursor at raw index `1`

#### Scenario: iOS receives programmatic state
- **WHEN** Kotlin sets `TextInputState(text = "abc", selectionStart = 1, selectionEnd = 1)` on iOS
- **THEN** the native input SHALL display `abc` and place the cursor at raw index `1`

#### Scenario: HarmonyOS receives programmatic state
- **WHEN** Kotlin sets `TextInputState` on HarmonyOS
- **THEN** the native input SHALL update text and selection consistently where platform selection APIs permit

#### Scenario: Web receives programmatic state
- **WHEN** Kotlin sets `TextInputState` on Web
- **THEN** the input value and `selectionStart` / `selectionEnd` SHALL match the raw state

#### Scenario: miniApp receives programmatic state
- **WHEN** Kotlin sets `TextInputState` on miniApp
- **THEN** the input SHALL update text and SHALL apply selection when the miniApp runtime exposes selection APIs

#### Scenario: macOS receives programmatic state
- **WHEN** Kotlin sets `TextInputState` on macOS
- **THEN** the native input SHALL update text and selection consistently where platform selection APIs permit

### Requirement: TextFieldValue selection compatibility
Compose value-based TextField APIs SHALL preserve `TextFieldValue.text`, `TextFieldValue.selection`, and `TextFieldValue.composition` when synchronizing with native input views.

#### Scenario: Android TextFieldValue cursor move
- **WHEN** the user taps the middle of a value-based TextField on Android
- **THEN** `onValueChange` SHALL receive a `TextFieldValue` with unchanged text and updated selection

#### Scenario: iOS TextFieldValue cursor move
- **WHEN** the user taps the middle of a value-based TextField on iOS
- **THEN** `onValueChange` SHALL receive a `TextFieldValue` with unchanged text and updated selection

#### Scenario: HarmonyOS TextFieldValue cursor move
- **WHEN** the user changes cursor position on HarmonyOS
- **THEN** `onValueChange` SHALL receive updated selection when the platform reports it

#### Scenario: Web TextFieldValue cursor move
- **WHEN** the user changes cursor position on Web
- **THEN** `onValueChange` SHALL receive updated selection

#### Scenario: miniApp TextFieldValue cursor move
- **WHEN** the user changes cursor position on miniApp
- **THEN** `onValueChange` SHALL receive updated selection when the runtime reports it

#### Scenario: macOS TextFieldValue cursor move
- **WHEN** the user changes cursor position on macOS
- **THEN** `onValueChange` SHALL receive updated selection when the platform reports it

### Requirement: State-based Compose TextField editing
Compose DSL SHALL provide a Kuikly package `TextFieldState` API with `rememberTextFieldState` and `edit {}` operations for insert, replace, delete, append, cursor placement, and selection updates.

#### Scenario: Android state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on Android
- **THEN** the shortcode SHALL be inserted at the current raw cursor position

#### Scenario: iOS state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on iOS
- **THEN** the shortcode SHALL be inserted at the current raw cursor position

#### Scenario: HarmonyOS state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on HarmonyOS
- **THEN** the shortcode SHALL be inserted at the current raw cursor position when selection is available

#### Scenario: Web state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on Web
- **THEN** the shortcode SHALL be inserted at the current raw cursor position

#### Scenario: miniApp state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on miniApp
- **THEN** the shortcode SHALL be inserted at the best-known raw cursor position

#### Scenario: macOS state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on macOS
- **THEN** the shortcode SHALL be inserted at the current raw cursor position when selection is available

### Requirement: Self DSL text input state binding
Self DSL `Input` and `TextArea` SHALL expose semantic state binding through `TextInputState` methods/events while keeping existing `textDidChange`, `cursorIndex`, and `setCursorIndex` APIs compatible.

#### Scenario: Android self DSL state binding
- **WHEN** self DSL binds `TextInputState` to an Android input
- **THEN** text and selection SHALL stay synchronized through state-change events

#### Scenario: iOS self DSL state binding
- **WHEN** self DSL binds `TextInputState` to an iOS input
- **THEN** text and selection SHALL stay synchronized through state-change events

#### Scenario: HarmonyOS self DSL state binding
- **WHEN** self DSL binds `TextInputState` to a HarmonyOS input
- **THEN** text and available selection SHALL stay synchronized through state-change events

#### Scenario: Web self DSL state binding
- **WHEN** self DSL binds `TextInputState` to a Web input
- **THEN** text and selection SHALL stay synchronized through state-change events

#### Scenario: miniApp self DSL state binding
- **WHEN** self DSL binds `TextInputState` to a miniApp input
- **THEN** text and available selection SHALL stay synchronized through state-change events

#### Scenario: macOS self DSL state binding
- **WHEN** self DSL binds `TextInputState` to a macOS input
- **THEN** text and available selection SHALL stay synchronized through state-change events

### Requirement: Backward compatibility for existing text input APIs
Existing text-only TextField, `textDidChange`, `cursorIndex`, `setCursorIndex`, `Modifier.textPostProcessor`, and `InputAttr.textPostProcessor` behavior SHALL remain compatible.

#### Scenario: Android existing emoji demo remains valid
- **WHEN** the existing Android emoji demo uses `Modifier.textPostProcessor("input")`
- **THEN** shortcode rendering through `ImageSpan` SHALL continue to work

#### Scenario: iOS existing text input remains valid
- **WHEN** an existing iOS text input uses only `text` and `textDidChange`
- **THEN** text input behavior SHALL remain unchanged

#### Scenario: HarmonyOS existing text input remains valid
- **WHEN** an existing HarmonyOS text input uses only `text` and `textDidChange`
- **THEN** text input behavior SHALL remain unchanged

#### Scenario: Web existing text input remains valid
- **WHEN** an existing Web text input uses only `text` and `textDidChange`
- **THEN** text input behavior SHALL remain unchanged

#### Scenario: miniApp existing text input remains valid
- **WHEN** an existing miniApp text input uses only `text` and `textDidChange`
- **THEN** text input behavior SHALL remain unchanged

#### Scenario: macOS existing text input remains valid
- **WHEN** an existing macOS text input uses only `text` and `textDidChange`
- **THEN** text input behavior SHALL remain unchanged
