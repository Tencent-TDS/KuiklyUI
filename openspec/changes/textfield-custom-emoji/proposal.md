## Why

KuiklyUI Compose TextField currently only synchronizes text, so business code cannot observe cursor/selection changes or insert custom emoji shortcode at the user-selected position. The latest custom emoji commit has proven the `textPostProcessor` display path on Android, but it remains append-only and lacks official Compose-style state, selection, and transformation semantics.

## What Changes

- Add a cross-layer text input state contract for raw text, selection, and composition synchronization.
- Extend Compose DSL TextField toward official Compose 1.7.x state-based usage with `TextFieldValue` compatibility and a `TextFieldState`/`edit {}` target API.
- Bridge the existing `textPostProcessor` custom emoji implementation into the target `OutputTransformation` path instead of replacing it immediately.
- Preserve existing `Modifier.textPostProcessor()` and `InputAttr.textPostProcessor()` behavior as compatibility and platform-rendering backends.
- Enable custom emoji insertion at the current cursor or selected range rather than only appending shortcode text.
- Add renderer support expectations for Android, iOS, HarmonyOS, Web, miniApp, and macOS where platform capability permits.
- Add demos and documentation that distinguish current append-style usage from the final official-aligned usage.

## Capabilities

### New Capabilities
- `textfield-state-editing`: Covers TextField raw text, selection, composition, state synchronization, and programmatic editing semantics across Compose DSL, core, and renderers.
- `textfield-transformations`: Covers input filtering, output-only display transformation, and bridging existing `textPostProcessor` custom emoji rendering into the transformation pipeline.

### Modified Capabilities
- None.

## Impact

- Modules impacted:
  - `compose/`: `CoreTextField`, `BasicTextField` overloads, TextField state/buffer/transformation APIs, modifier compatibility.
  - `core/`: `TextInputState`, text input state methods/events for `InputView`, `TextAreaView`, and `AutoHeightTextAreaView`.
  - `core-render-android/`: preserve and adapt current `KRTextFieldView` `textPostProcessor` / `ImageSpan` behavior; add selection/composition state synchronization.
  - `core-render-ios/`: add equivalent TextField/TextArea state synchronization and shortcode-to-attachment display support.
  - `core-render-ohos/`, `core-render-web/`: add text/selection/composition event and state setting support as platform APIs allow.
  - `demo/`: update current emoji demos and add official-aligned state/edit demos.
  - `docs/`: update text post-processor guidance and add migration guidance.
- Affected APIs:
  - Existing value-based `BasicTextField` remains compatible.
  - Existing `textPostProcessor` remains supported.
  - New official-aligned APIs are added under Kuikly Compose packages, not by importing AndroidX Foundation directly.
- Affected platforms: Android, iOS, HarmonyOS, Web, miniApp, and macOS.

## Non-goals

- Do not remove or break the latest `textPostProcessor` custom emoji commit.
- Do not directly depend on official AndroidX Foundation TextField implementation inside KuiklyUI Compose.
- Do not require all platforms to support full IME composition semantics in the first implementation; unsupported platforms may report `composition = null`.
- Do not replace every rich text/input span mechanism with a new inline content system in the first phase.
- Do not change unrelated Text, RichText, layout, or keyboard action behavior except where required for TextField state synchronization.
