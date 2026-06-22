## 1. Compose / Core bridge alignment

- [x] 1.1 Audit `CoreTextField` and state-edit insertion flow so emoji panel actions always use `replace(selection.start, selection.end, shortCode)` with the latest raw selection.
- [x] 1.2 Align `Modifier.maxLength` / `Modifier.onLimitChange` handling with renderer-backed length results, including the accepted/rejected state returned by `textInputStateChange` and `textLengthBeyondLimit`.
- [x] 1.3 Confirm no new core API is required; if needed, only make minimal bridge-side adjustments around `selectionChange`, `textLengthBeyondLimit`, and `setTextInputState` semantics.

## 2. Android renderer

- [x] 2.1 Update `core-render-android/.../KRTextFieldView.kt` so middle insertion and selected-range replacement are validated against the post-replacement raw text under `CHARACTER`, `BYTE`, and `VISUAL_WIDTH`.
- [x] 2.2 Ensure over-limit custom emoji insertion rejects the full shortcode atomically, without writing a partial token or corrupting the unaffected prefix/suffix.
- [x] 2.3 Verify Android callbacks keep reporting consistent accepted length, limit state, and selection after accepted or rejected emoji insertion.

## 3. iOS renderer

- [x] 3.1 Update `core-render-ios/.../KRTextFieldView.m` so single-line programmatic emoji insertion checks the full shortcode against maxLength before commit.
- [x] 3.2 Update `core-render-ios/.../KRTextAreaView.m` so selected-range emoji replacement either accepts the full shortcode or rejects it atomically while preserving valid raw selection.
- [x] 3.3 Align iOS `textInputStateChange`, `selectionChange`, and `textLengthBeyondLimit` callbacks for accepted vs rejected emoji insertion cases.

## 4. Demo and docs

- [x] 4.1 Reuse `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextFieldEmojiDemo.kt` to cover middle insertion, selected-range replacement, and maxLength rejection under CHARACTER / BYTE / VISUAL_WIDTH.
- [x] 4.2 Keep and polish the existing single-line regression card so iOS cursor recovery and over-limit rejection are easy to verify.
- [x] 4.3 Update `docs/Compose/core-components.md` to document custom emoji + `Modifier.maxLength` behavior and the `onLimitChange` callback semantics.

## 5. Validation

- [x] 5.1 Manually verify Android end insertion, middle insertion, selected-range replacement, and over-limit emoji rejection in `TextFieldEmojiDemo`.
- [x] 5.2 Manually verify iOS single-line and multi-line custom emoji maxLength behavior, including cursor recovery after rejected insertion.
- [x] 5.3 Run a HarmonyOS smoke verification to confirm this change does not introduce compile or runtime regressions outside the scoped platforms.
- [x] 5.4 Run module-level build checks needed for this change, including at least `./gradlew :compose:compileDebugKotlinAndroid` and the relevant renderer/app verification steps.
- [x] 5.5 Run `openspec validate --changes textfield-custom-emoji-maxlength --strict` before starting or finishing implementation.
