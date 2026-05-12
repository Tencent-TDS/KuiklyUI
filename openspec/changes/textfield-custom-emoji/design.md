## Context

This change applies to both DSL modes:

- Compose DSL: primary API alignment target, matching Compose Foundation 1.7.x state-based TextField semantics under Kuikly package names.
- Self DSL: semantic alignment through `TextInputState` and state-change events on `Input` / `TextArea`, not API-shape equivalence with Compose.

Current state:

- Compose `CoreTextField` sends only `value.text` to native input views.
- Compose `onValueChange` reconstructs `TextFieldValue(it.text)` and drops selection/composition.
- Core has `cursorIndex` / `setCursorIndex`, but no selection range, composition range, or atomic text+selection state payload.
- Latest custom emoji commit added `textPostProcessor` on Compose modifiers and self-DSL `InputAttr`, plus Android `EditText` `ImageSpan` rendering in `KRTextFieldView`.
- Current emoji demos append shortcode text to the end because Kotlin cannot observe the current native cursor.

NativeBridge interactions required:

- Kotlin core sends `setTextInputState` to render views with raw text, selection start/end, and composition start/end.
- Kotlin core receives `textInputStateChange` from render views with raw text, selection, composition, and optional length.
- Kotlin core receives `selectionChange` when user moves cursor/selection without text changes.
- Render layers MUST convert platform display coordinates back to raw-text coordinates before firing events.

## Goals / Non-Goals

**Goals:**

- Preserve existing `textPostProcessor` custom emoji behavior and documentation.
- Add raw text + selection + composition synchronization through `TextInputState`.
- Make `TextFieldValue` callbacks include selection/composition.
- Add Kuikly Compose `TextFieldState`, `TextFieldBuffer`, `InputTransformation`, and `OutputTransformation` target APIs.
- Bridge `textPostProcessor` into `OutputTransformation` for custom emoji so existing Android `ImageSpan` logic is reused.
- Support insertion/replacement at current cursor or selected range using official-aligned state editing.
- Define behavior for Android, iOS, HarmonyOS, Web, miniApp, and macOS with explicit degradation for unsupported composition APIs.

**Non-Goals:**

- Do not import or delegate to AndroidX Foundation TextField implementation directly.
- Do not delete `Modifier.textPostProcessor()` or `InputAttr.textPostProcessor()`.
- Do not require full IME composition parity on every platform in the first implementation.
- Do not introduce a full cross-platform inline content replacement for every rich text use case in the first phase.
- Do not change unrelated Text/RichText rendering except for shared post-processor compatibility.

## Decisions

### Decision 1: Introduce `TextInputState` as the core/render payload

Use an internal framework payload rather than exposing `TextFieldState` through the NativeBridge.

Rationale:

- `TextFieldState` is a Compose-layer API with snapshot semantics.
- Self DSL needs a plain state object that can be stored with `observable`.
- Render layers only need raw text, selection, composition, and length metadata.

Alternative considered: use `TextFieldValue` directly in core. Rejected because core must not depend on Compose UI text types and must also support self DSL.

### Decision 2: Keep `textPostProcessor` as a compatibility and rendering backend

The latest custom emoji commit already provides Android runtime behavior and docs. The first transformation implementation will wrap this path rather than replace it.

Rationale:

- Avoids regressing existing demos and Android behavior.
- Allows `OutputTransformation` to become the official-aligned API while still using platform-native spans/attachments.
- Gives iOS/OHOS/Web a clear parity target.

Alternative considered: replace `textPostProcessor` with a new inline content protocol immediately. Rejected due to cross-platform complexity and unnecessary churn.

### Decision 3: Compose DSL gets official-aligned API names under Kuikly packages

Add APIs such as `rememberTextFieldState`, `TextFieldState`, `TextFieldBuffer`, `InputTransformation`, and `OutputTransformation` under `com.tencent.kuikly.compose.*` packages.

Rationale:

- Kuikly Compose cannot mix official AndroidX Foundation rendering implementation.
- Keeping official-like names reduces migration cost for Compose users.
- Kuikly package names preserve module boundaries.

Alternative considered: expose only `TextFieldValue` selection sync. Rejected because it does not cover official state-based APIs or transformation semantics.

### Decision 4: All Kotlin-facing selection indices use raw text coordinates

Even when display uses `ImageSpan`, `NSTextAttachment`, or formatted output, Kotlin state and callbacks use raw shortcode/text coordinates.

Rationale:

- Business state remains serializable and deterministic.
- Existing Android emoji adapter preserves shortcode text in `Editable`.
- iOS can use `KRTextAttachmentStringProtocol` and `p_outputText` to restore raw text.

Alternative considered: expose display coordinates. Rejected because display coordinates vary by platform and transformation.

### Decision 5: Implement in layers with value-based API first

The safest path is `TextInputState` + `TextFieldValue` selection sync first, then state API and transformations.

Rationale:

- Cursor insertion for emoji can be validated before full transformation support.
- Existing value-based demos continue to work.
- Later `TextFieldState` can be layered over the same core/render state payload.

## Implementation Status (Updated 2026-05-08)

Based on commit `da232115` (feat: compose TextField support custom emoji), the implementation is partially complete with the following status:

### ✅ Completed (Phase 1)

**Core Module:**
- `TextInputState` data model with raw text, selection, composition, and length metadata
- `setTextInputState` and `getTextInputState` methods for `InputView`, `TextAreaView`, `AutoHeightTextAreaView`
- `textInputStateChange` event support in `InputEvent` and `TextAreaEvent`
- `selectionChange` event support for cursor/selection-only changes

**Compose Module:**
- `CoreTextField` sends `TextFieldValue.text`, `selection`, and `composition` through `TextInputState`
- `CoreTextField` converts native `TextInputState` callbacks into full `TextFieldValue` callbacks
- Loop prevention for programmatic state updates in `CoreTextField`
- `TextFieldState` and `rememberTextFieldState` under Kuikly Compose packages
- `TextFieldBuffer` with insert, replace, delete, append, selectAll, placeCursorAtEnd, and revert support
- State-based `BasicTextField(state = ...)` overload wired to existing `CoreTextField`
- `InputTransformation` with `then` and `maxLength` support
- `TextPostProcessorOutputTransformation` bridge that reuses existing `textPostProcessor` semantics
- Preserved `Modifier.textPostProcessor(processor)` as compatibility API

**Android Renderer:**
- `KRTextFieldView` supports `setTextInputState` and `getTextInputState`
- `KRTextFieldView` emits `textInputStateChange` with text, selection, length, and composition metadata
- `KRTextFieldView` emits `selectionChange` from `onSelectionChanged`
- `KRTextFieldView` preserves `textPostProcessor` and `applyEmojiSpans` behavior
- Programmatic text+selection updates do not force cursor to text end

**Demo and Documentation:**
- `TextFieldEmojiDemo` shows `textPostProcessor` usage and state/edit usage
- `EmojiTextInputDemo` shows self DSL `TextInputState` usage
- Compose demo for `TextFieldState.edit {}` inserting custom emoji at cursor
- Text post-processor docs with compatibility and transformation-bridge guidance
- Migration notes from append-style emoji input to state/edit insertion

### ⏳ Pending (Future Phases)

**Android Renderer:**
- Raw/display mapping tests around `ImageSpan` deletion and cursor movement (Task 3.6)

**iOS Renderer:** (All tasks 4.1-4.6 pending)
- `css_setTextInputState` and `css_getTextInputState` for `KRTextAreaView` and `KRTextFieldView`
- Emit `textInputStateChange` and `selectionChange` from iOS text delegates
- Reuse `p_outputText` and cursor mapping for attachment display
- Add input-view `textPostProcessor` application with `NSTextAttachment`
- Guard `_ignoreTextDidChanged` paths against recursive callback loops

**HarmonyOS Renderer:** (All tasks 5.1-5.4 pending)
- State payload method handling for text input and text area views
- Emit text and selection state changes using ArkUI selection APIs
- Represent unsupported composition as empty composition metadata

**Web and miniApp Renderers:** (All tasks 6.1-6.4 pending)
- Web input/textarea state payload support using `selectionStart` and `selectionEnd`
- Web composition event handling for composition metadata
- miniApp best-effort state payload support

**Additional Compose Demos:** (Tasks 7.3, 7.5, 7.6 pending)
- Compose demo for `TextFieldValue` selection synchronization
- Compose demo for `InputTransformation.maxLength` and digit-only filtering
- Compose demo for `OutputTransformation` phone formatting

**Validation:** (Most tasks 8.4-8.10 pending)
- Manual verification of Android cursor insert, selected-range replace, emoji delete, paste, and Chinese IME cases
- iOS build and verification
- HarmonyOS build and verification
- Web development build and verification
- Compatibility verification for old APIs
- `openspec validate --change textfield-custom-emoji --strict` before completion

## File Changes by Module

### `compose/`

- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/CoreTextField.kt`
  - Send and receive `TextInputState`.
  - Preserve `TextFieldValue.selection` and `composition`.
  - Add state-based overload wiring.
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/*.kt`
  - Add `TextFieldState`, `TextFieldBuffer`, `InputTransformation`, `OutputTransformation`, and helpers.
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/extension/ModifierSetProp.kt`
  - Keep `textPostProcessor` compatibility.

### `core/`

- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/InputView.kt`
- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/TextAreaView.kt`
- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/AutoHeightTextAreaView.kt`
  - Add `TextInputState` methods and events.
  - Keep old `textDidChange`, `cursorIndex`, and `setCursorIndex`.
- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/TextInputState.kt`
  - New file: `TextInputState` data model.

### `core-render-android/`

- `core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/component/KRTextFieldView.kt`
  - Reuse existing `textPostProcessor` and `applyEmojiSpans`.
  - Add selection range callbacks and state payload support.
  - Prevent programmatic set loops.

### `core-render-ios/` (Pending)

- `core-render-ios/Extension/Components/KRTextAreaView.m`
- `core-render-ios/Extension/Components/KRTextFieldView.m`
  - Add state payload methods/events.
  - Add post-processor support for input views via `NSTextAttachment` where needed.
  - Use raw/display cursor mapping.

### `core-render-ohos/` (Pending), `core-render-web/` (Pending)

- Add text/selection/composition payload support where platform APIs allow.
- Document composition limitations when unavailable.

### `demo/`

- `TextFieldEmojiDemo.kt` - Updated with `textPostProcessor` and state/edit usage.
- `EmojiTextInputDemo.kt` - Shows self DSL `TextInputState` usage.
- `ComposeAllSample.kt` - Added Demo entry.

### `docs/`

- `docs/API/components/input.md` - Updated with `textPostProcessor` and `TextInputState` API.
- `docs/API/components/text-area.md` - Updated with state API.
- `docs/API/components/text.md` - Added `textPostProcessor` documentation.
- `docs/DevGuide/text-post-processor-guide.md` - New guide for text post-processor usage and migration.

## Risks / Trade-offs

- [Risk] Platform composition APIs differ or are unavailable → Mitigation: represent unsupported composition as `null` / `-1` and avoid destructive transformations during active composition.
- [Risk] Raw/display offset mapping around emoji attachments is error-prone → Mitigation: start with shortcode-preserving spans/attachments and add focused tests for insert, delete, selection, and paste.
- [Risk] Programmatic state updates can trigger callback loops → Mitigation: use render-side ignore flags and Compose-side last-state comparison.
- [Risk] New state APIs may diverge from official Compose details → Mitigation: match Compose 1.7.x names and semantics where possible; document Kuikly-specific differences.
- [Risk] Self DSL users may confuse `TextInputState` with official `TextFieldState` → Mitigation: keep naming explicit and document it as core/render payload semantics.
- [Risk] iOS/HarmonyOS/Web/miniApp platforms not yet implemented → Mitigation: Phase 1 focuses on Android + Compose; other platforms can be added in future phases while keeping API compatibility.

## Migration Plan

1. Keep existing `textPostProcessor` APIs and demos working.
2. Add `TextInputState` alongside old `textDidChange` APIs.
3. Update Compose value-based `TextFieldValue` path to synchronize selection.
4. Add `TextFieldState` and `edit {}` APIs on top of the same payload.
5. Add `OutputTransformation` wrapper for current `textPostProcessor` custom emoji.
6. Update demos and docs to show both current compatibility usage and recommended final usage.
7. Rollback strategy: disable new state-based overloads and keep existing value-based text + `textPostProcessor` path unchanged.
8. Future phases: implement iOS/HarmonyOS/Web/miniApp renderers following the same `TextInputState` payload protocol.

## Open Questions

- Which platforms must provide full composition range in the first implementation versus explicitly returning `null`?
  - **Answer (2026-05-08):** Android provides composition metadata; iOS/HarmonyOS/Web/miniApp will return `null`/`-1` for composition in future phases.
- Should `TextAreaView` self DSL expose `TextInputState` as public stable API or experimental API first?
- Should the bridge wrapper be named `TextPostProcessorOutputTransformation`, `KuiklyTextPostProcessorTransformation`, or a shorter API name?
  - **Answer (2026-05-08):** Named `TextPostProcessorOutputTransformation`.
- Should deletion of a displayed emoji token delete the complete shortcode by default on all platforms, or only where platform span/attachment metadata can map it safely?
  - **Answer (2026-05-08):** Android handles this via `applyEmojiSpans`; other platforms need investigation in future phases.
