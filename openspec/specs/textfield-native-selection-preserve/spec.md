## ADDED Requirements

### Requirement: Compose TextField textDidChange callback SHALL preserve native cursor position

When the Compose DSL `CoreTextField` receives a `textDidChange` callback from the iOS native layer — which carries only `text` without `selection` information — it SHALL preserve the correct cursor/selection position by inheriting from the last-synced native editing state (`lastSyncedTextInputState`), rather than defaulting to `TextRange.Zero` (position 0).

#### Scenario: Pinyin composition (marked text) on single-line TextField
- **WHEN** the user is typing Chinese pinyin into a Compose single-line TextField on iOS, and the native layer fires `textDidChange` (with text only) after a `selectionChange` (with correct selection) has already been processed in the same frame
- **THEN** the `CoreTextField` SHALL use the selection from `lastSyncedTextInputState` (which was updated by `selectionChange`) for the `onValueChange` call, instead of defaulting to `TextRange.Zero`
- **AND** the native cursor SHALL remain at the correct position (not jump to index 0)

#### Scenario: Pinyin composition (marked text) on multi-line TextField
- **WHEN** the user is typing Chinese pinyin into a Compose multi-line TextField (BasicTextField with maxLines > 1) on iOS
- **THEN** the behavior SHALL be identical to the single-line scenario — the cursor SHALL NOT jump to index 0

#### Scenario: textDidChange text differs from lastSyncedTextInputState
- **WHEN** the `textDidChange` callback carries a `text` value that differs from the text in `lastSyncedTextInputState`
- **THEN** the `CoreTextField` SHALL fall back to `TextRange.Zero` for the `onValueChange` call, because the text has changed and the old selection is no longer valid

#### Scenario: Non-composition input (direct character insertion)
- **WHEN** the user types a single character directly (no pinyin/marked text), and the native layer fires both `textInputStateChange` (with text + selection) and `textDidChange` (text only)
- **THEN** the `textDidChange` callback SHALL be skipped via the existing `pendingTextInputStateText` deduplication mechanism, and the fix logic SHALL NOT be reached

#### Scenario: Self-DSL (Pager + body()) text input
- **WHEN** the user is typing into a TextField under the self-DSL path (not Compose DSL)
- **THEN** the fix SHALL NOT apply, because the self-DSL path uses `InputView.didSetProp` / `TextAreaView.didSetProp` with a different `shouldSyncToNative` logic that already handles this correctly
