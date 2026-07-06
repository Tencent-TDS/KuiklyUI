## ADDED Requirements

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

## MODIFIED Requirements

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
