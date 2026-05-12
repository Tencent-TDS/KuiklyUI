## 1. Core Module

- [x] 1.1 Add `TextInputState` data model with raw text, selection, composition, and length metadata.
- [x] 1.2 Add `setTextInputState` and `getTextInputState` methods to `InputView`.
- [x] 1.3 Add `setTextInputState` and `getTextInputState` methods to `TextAreaView`.
- [x] 1.4 Add `setTextInputState` and `getTextInputState` methods to `AutoHeightTextAreaView`.
- [x] 1.5 Add `textInputStateChange` event support to `InputEvent` without breaking `textDidChange`.
- [x] 1.6 Add `textInputStateChange` event support to `TextAreaEvent` without breaking `textDidChange`.
- [x] 1.7 Add `selectionChange` event support for cursor/selection-only changes.
- [x] 1.8 Keep `cursorIndex`, `setCursorIndex`, and `textDidChange` compatibility paths intact.

## 2. Compose Module

- [x] 2.1 Update `CoreTextField` to send `TextFieldValue.text`, `selection`, and `composition` through `TextInputState`.
- [x] 2.2 Update `CoreTextField` to convert native `TextInputState` callbacks into full `TextFieldValue` callbacks.
- [x] 2.3 Add loop prevention for programmatic state updates in `CoreTextField`.
- [x] 2.4 Add `TextFieldState` and `rememberTextFieldState` under Kuikly Compose packages.
- [x] 2.5 Add `TextFieldBuffer` with insert, replace, delete, append, selectAll, placeCursorAtEnd, and revert support.
- [x] 2.6 Add state-based `BasicTextField(state = ...)` overload wired to existing `CoreTextField`.
- [x] 2.7 Add `InputTransformation` with `then` and `maxLength` support.
- [ ] 2.8 Add `OutputTransformation` display-only pipeline.
- [x] 2.9 Add `TextPostProcessorOutputTransformation` bridge that reuses existing `textPostProcessor` semantics.
- [x] 2.10 Preserve `Modifier.textPostProcessor(processor)` as compatibility API.

## 3. Android Renderer

- [x] 3.1 Extend `KRTextFieldView` call handling for `setTextInputState` and `getTextInputState`.
- [x] 3.2 Emit `textInputStateChange` with text, selection start/end, length, and best-effort composition metadata.
- [x] 3.3 Emit `selectionChange` from `onSelectionChanged` when text is unchanged.
- [x] 3.4 Preserve current `textPostProcessor` and `applyEmojiSpans` behavior for existing emoji demos.
- [x] 3.5 Ensure programmatic text+selection updates do not force cursor to text end.
- [ ] 3.6 Add raw/display mapping tests around `ImageSpan` deletion and cursor movement.

## 4. iOS Renderer

- [ ] 4.1 Add `css_setTextInputState` and `css_getTextInputState` to `KRTextAreaView`.
- [ ] 4.2 Add `css_setTextInputState` and `css_getTextInputState` to `KRTextFieldView`.
- [ ] 4.3 Emit `textInputStateChange` and `selectionChange` from iOS text delegates.
- [ ] 4.4 Reuse `p_outputText` and cursor mapping so attachment display returns raw shortcode text.
- [ ] 4.5 Add input-view `textPostProcessor` application with `NSTextAttachment` and `KRTextAttachmentStringProtocol`.
- [ ] 4.6 Guard `_ignoreTextDidChanged` paths against recursive callback loops.

## 5. HarmonyOS Renderer

- [ ] 5.1 Add state payload method handling to HarmonyOS text input and text area views.
- [ ] 5.2 Emit text and selection state changes using ArkUI selection APIs where available.
- [ ] 5.3 Represent unsupported composition as empty composition metadata.
- [ ] 5.4 Verify existing text input behavior remains unchanged when new state APIs are unused.

## 6. Web and miniApp Renderers

- [ ] 6.1 Add Web input/textarea state payload support using `selectionStart` and `selectionEnd`.
- [ ] 6.2 Add Web composition event handling for composition metadata.
- [ ] 6.3 Add miniApp best-effort state payload support for text and selection.
- [ ] 6.4 Preserve existing Web and miniApp text input behavior when state APIs are unused.

## 7. Demo and Documentation

- [x] 7.1 Update `TextFieldEmojiDemo` to show current `textPostProcessor` usage and target state/edit usage.
- [x] 7.2 Update `EmojiTextInputDemo` to show self DSL `TextInputState` usage when available.
- [ ] 7.3 Add Compose demo for `TextFieldValue` selection synchronization.
- [x] 7.4 Add Compose demo for `TextFieldState.edit {}` inserting custom emoji at cursor.
- [ ] 7.5 Add Compose demo for `InputTransformation.maxLength` and digit-only filtering.
- [ ] 7.6 Add Compose demo for `OutputTransformation` phone formatting.
- [x] 7.7 Update text post-processor docs with compatibility and transformation-bridge guidance.
- [x] 7.8 Add migration notes from append-style emoji input to state/edit insertion.

## 8. Validation

- [x] 8.1 Run `./gradlew :core:compileDebugKotlinAndroid`.
- [x] 8.2 Run `./gradlew :compose:compileDebugKotlinAndroid`.
- [x] 8.3 Run `./gradlew :androidApp:assembleDebug` and verify Android emoji demos.
- [ ] 8.4 Verify Android cursor insert, selected-range replace, emoji delete, paste, and Chinese IME cases manually.
- [ ] 8.5 Build and verify iOS TextArea cursor insert, selected-range replace, emoji delete, paste, and Chinese IME cases.
- [ ] 8.6 Build and verify iOS TextField single-line state synchronization.
- [ ] 8.7 Build and verify HarmonyOS text input state synchronization and graceful composition degradation.
- [ ] 8.8 Run Web development build and verify text/selection state synchronization.
- [ ] 8.9 Verify old `textDidChange`, `cursorIndex`, `setCursorIndex`, and `textPostProcessor` usages remain compatible.
- [ ] 8.10 Run `openspec validate --change textfield-custom-emoji --strict` before implementation completion.
