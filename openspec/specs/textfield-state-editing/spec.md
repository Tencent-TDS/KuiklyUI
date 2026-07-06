## Requirements

### Requirement: TextInputState SHALL normalize out-of-bounds selection and composition
The `TextInputState` data class SHALL provide a `coerceToTextBounds()` method that clips `selectionStart`, `selectionEnd`, `compositionStart`, and `compositionEnd` to the valid range `[0, text.length]`. The method SHALL return the same instance when no values are out of bounds, and a new copy only when correction is needed.

#### Scenario: selection exceeds text length
- **WHEN** a `TextInputState` is constructed or decoded with `selectionStart = 10` and `selectionEnd = 10` but `text.length = 5`
- **THEN** `coerceToTextBounds()` SHALL return a copy with `selectionStart = 5` and `selectionEnd = 5`

#### Scenario: composition out of bounds
- **WHEN** a `TextInputState` has `compositionStart = -1` and `compositionEnd = 100` with `text.length = 10`
- **THEN** `coerceToTextBounds()` SHALL return a copy with `compositionStart = 0` and `compositionEnd = 10`

#### Scenario: all values already in bounds
- **WHEN** all selection and composition values are within `[0, text.length]`
- **THEN** `coerceToTextBounds()` SHALL return the same instance without copying

#### Scenario: no composition present
- **WHEN** `compositionStart` and `compositionEnd` are both `NO_COMPOSITION` (-1)
- **THEN** `coerceToTextBounds()` SHALL preserve them as `NO_COMPOSITION` without modification

### Requirement: TextInputState.decode SHALL apply coerceToTextBounds
The `TextInputState.decode()` factory method SHALL call `coerceToTextBounds()` on the constructed instance before returning, ensuring any JSON-deserialized state is immediately normalized.

#### Scenario: decode from JSON with out-of-bounds indices
- **WHEN** `TextInputState.decode()` receives a JSONObject with `selectionStart = 100` for a text of length 3
- **THEN** the returned `TextInputState` SHALL have `selectionStart` clipped to 3

### Requirement: CoreTextField SHALL unify native editing event handling
The Compose DSL `CoreTextField` SHALL provide a private `handleNativeEditingStateChange(nativeState: TextInputState, shouldMarkPendingText: Boolean)` method that normalizes the native state via `coerceToTextBounds()`, marks the processing flag, updates `lastSyncedTextInputState`, and calls `onValueChange` with a fully populated `TextFieldValue` containing text, selection, and composition.

#### Scenario: textInputStateChange triggers unified handler
- **WHEN** the native layer fires `textInputStateChange` with text, selection, and composition data
- **THEN** `handleNativeEditingStateChange` SHALL be called with `shouldMarkPendingText = true`
- **AND** `pendingTextInputStateText` SHALL be set for `textDidChange` deduplication

#### Scenario: selectionChange triggers unified handler
- **WHEN** the native layer fires `selectionChange` with updated selection but no text change
- **THEN** `handleNativeEditingStateChange` SHALL be called with `shouldMarkPendingText = false`
- **AND** `onValueChange` SHALL receive the updated selection without overwriting text

### Requirement: CoreTextField toTextFieldValue SHALL produce complete editing state
The `CoreTextField` SHALL provide `TextInputState.toTextFieldValue()` that normalizes the state via `coerceToTextBounds()` and returns a `TextFieldValue` with `text`, `selection`, and `composition` (as `TextRange?`).

#### Scenario: converting normalized TextInputState with composition
- **WHEN** a `TextInputState` with valid text, selection, and composition is converted
- **THEN** the resulting `TextFieldValue` SHALL have `selection = TextRange(selectionStart, selectionEnd)` and `composition = TextRange(compositionStart, compositionEnd)`

#### Scenario: converting TextInputState without composition
- **WHEN** a `TextInputState` has `compositionStart = -1` (NO_COMPOSITION)
- **THEN** the resulting `TextFieldValue.composition` SHALL be `null`

### Requirement: CoreTextField toTextInputState SHALL normalize before returning
The `CoreTextField` SHALL provide `TextFieldValue.toTextInputState()` that maps `selection` and `composition` from `TextFieldValue` to `TextInputState` and calls `coerceToTextBounds()` before returning, ensuring downward state is always legal.

#### Scenario: TextFieldValue with TextRange.Zero selection
- **WHEN** `TextFieldValue("hello", selection = TextRange.Zero)` is converted
- **THEN** the resulting `TextInputState` SHALL have `selectionStart = 0, selectionEnd = 0` and be bounded by `text.length`

### Requirement: textDidChange fallback SHALL preserve editing state
When the `textDidChange` callback fires and is not deduplicated by `pendingTextInputStateText`, `CoreTextField` SHALL attempt to recover selection and composition from `lastSyncedTextInputState` if its text matches the current text. If text differs, fallback to `TextRange(text.length)` (end of text) rather than `TextRange.Zero` (position 0).

#### Scenario: textDidChange text matches last synced state
- **WHEN** `textDidChange` fires with text equal to `lastSyncedTextInputState.text`
- **THEN** the `onValueChange` call SHALL include `selection` from `lastSyncedTextInputState` and `composition` if present

#### Scenario: textDidChange text differs from last synced state
- **WHEN** `textDidChange` fires with text different from `lastSyncedTextInputState.text`
- **THEN** the `onValueChange` call SHALL fall back to `TextRange(text.length)` (end of text) with null composition

### Requirement: KRTextFieldView SHALL defensively guard setSelection(int)
The Android `KRTextFieldView` SHALL override `setSelection(index: Int)` to coerce the index into `[0, text?.length]` and wrap the call in try-catch, logging errors rather than crashing.

#### Scenario: setSelection with negative index
- **WHEN** `setSelection(-1)` is called on an `EditText` with text length 10
- **THEN** the method SHALL coerce the index to 0 and call `super.setSelection(0)` without throwing

#### Scenario: setSelection with index exceeding text length
- **WHEN** `setSelection(100)` is called on an empty `EditText`
- **THEN** the method SHALL coerce the index to 0 and call `super.setSelection(0)` without throwing

#### Scenario: setSelection with valid index
- **WHEN** `setSelection(5)` is called on an `EditText` with text length 10
- **THEN** the method SHALL pass 5 directly to `super.setSelection(5)`

#### Scenario: super.setSelection throws exception
- **WHEN** `super.setSelection()` throws any exception
- **THEN** the method SHALL catch it via `runCatching` and log the error without crashing

### Requirement: KRTextFieldView SHALL defensively guard setSelection(int, int)
The Android `KRTextFieldView` SHALL override `setSelection(start: Int, stop: Int)` to coerce both start and stop into `[0, text?.length]`, wrap in try-catch, and fall back to single-parameter `setSelection(stop)` on failure.

#### Scenario: setSelection with negative start
- **WHEN** `setSelection(-5, 3)` is called
- **THEN** `safeStart` SHALL be coerced to 0 and `super.setSelection(0, 3)` called

#### Scenario: setSelection with both indices out of bounds
- **WHEN** `setSelection(100, 200)` is called on text of length 10
- **THEN** both SHALL be coerced to 10 and `super.setSelection(10, 10)` called

#### Scenario: super.setSelection(int, int) throws exception
- **WHEN** `super.setSelection(int, int)` throws any exception
- **THEN** the method SHALL catch it, log the error, and attempt fallback via `super.setSelection(safeStop)`

### Requirement: Material3 TextField SHALL support TextFieldValue overload
The Kuikly Compose `material3.TextField` composable SHALL provide an overload accepting `value: TextFieldValue` and `onValueChange: (TextFieldValue) -> Unit`, enabling full control over text, selection, and composition state from business code.

#### Scenario: TextFieldValue cursor positioning
- **WHEN** business code sets `TextField(TextFieldValue("hello", selection = TextRange(2, 2)))`
- **THEN** the text field SHALL display "hello" with the cursor at raw index 2

#### Scenario: TextFieldValue selection range
- **WHEN** business code sets `TextField(TextFieldValue("hello world", selection = TextRange(0, 5)))`
- **THEN** the text field SHALL display "hello world" with characters 0-4 selected

#### Scenario: TextFieldValue placeholder via text property
- **WHEN** the `DecorationBox` renders and accesses `value.text`
- **THEN** it SHALL display the correct text from `TextFieldValue.text`

#### Scenario: Backward compatibility with String overload
- **WHEN** business code uses the existing `TextField(value: String, onValueChange: (String) -> Unit)` overload
- **THEN** behavior SHALL remain unchanged

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
Compose DSL SHALL provide a Kuikly package `TextFieldState` API with `rememberTextFieldState` and `edit {}` operations for insert, replace, delete, append, cursor placement, and selection updates. The `TextFieldState` SHALL use a private `applyBuffer` method and a public `setTextAndSelect` method to ensure all text/selection/composition mutations are normalized via `coerceIn(0, text.length)`.

#### Scenario: Android state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on Android
- **THEN** the shortcode SHALL be inserted at the current raw cursor position
- **AND** the resulting selection and composition SHALL be coerced to `[0, text.length]`

#### Scenario: iOS state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on iOS
- **THEN** the shortcode SHALL be inserted at the current raw cursor position
- **AND** the resulting selection and composition SHALL be coerced to `[0, text.length]`

#### Scenario: HarmonyOS state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on HarmonyOS
- **THEN** the shortcode SHALL be inserted at the current raw cursor position when selection is available
- **AND** the resulting selection and composition SHALL be coerced to `[0, text.length]`

#### Scenario: Web state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on Web
- **THEN** the shortcode SHALL be inserted at the current raw cursor position
- **AND** the resulting selection and composition SHALL be coerced to `[0, text.length]`

#### Scenario: miniApp state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on miniApp
- **THEN** the shortcode SHALL be inserted at the best-known raw cursor position
- **AND** the resulting selection and composition SHALL be coerced to `[0, text.length]`

#### Scenario: macOS state edit inserts at cursor
- **WHEN** business code calls `state.edit { insert(selection.start, "[smile]") }` on macOS
- **THEN** the shortcode SHALL be inserted at the current raw cursor position when selection is available
- **AND** the resulting selection and composition SHALL be coerced to `[0, text.length]`

#### Scenario: setTextAndSelect normalizes out-of-bounds selection
- **WHEN** `state.setTextAndSelect("ab", selection = TextRange(5, 10))` is called
- **THEN** `state.selection` SHALL be `TextRange(2, 2)` (clipped to text length)

#### Scenario: clearText resets editing state
- **WHEN** `state.clearText()` is called
- **THEN** `state.text` SHALL be `""`, `state.selection` SHALL be `TextRange.Zero`, and `state.composition` SHALL be `null`

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
