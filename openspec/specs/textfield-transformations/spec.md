## Requirements

### Requirement: InputTransformation filters raw user input
Compose DSL state-based TextField SHALL support `InputTransformation` that transforms or rejects raw user edits before committing them to `TextFieldState`.

#### Scenario: Android max length transformation
- **WHEN** Android user input exceeds `InputTransformation.maxLength(10)`
- **THEN** the extra raw input SHALL be rejected or truncated before state commit

#### Scenario: iOS max length transformation
- **WHEN** iOS user input exceeds `InputTransformation.maxLength(10)`
- **THEN** the extra raw input SHALL be rejected or truncated before state commit

#### Scenario: HarmonyOS max length transformation
- **WHEN** HarmonyOS user input exceeds `InputTransformation.maxLength(10)`
- **THEN** the extra raw input SHALL be rejected or truncated before state commit

#### Scenario: Web max length transformation
- **WHEN** Web user input exceeds `InputTransformation.maxLength(10)`
- **THEN** the extra raw input SHALL be rejected or truncated before state commit

#### Scenario: miniApp max length transformation
- **WHEN** miniApp user input exceeds `InputTransformation.maxLength(10)`
- **THEN** the extra raw input SHALL be rejected or truncated before state commit

#### Scenario: macOS max length transformation
- **WHEN** macOS user input exceeds `InputTransformation.maxLength(10)`
- **THEN** the extra raw input SHALL be rejected or truncated before state commit

### Requirement: OutputTransformation preserves raw state
Compose DSL state-based TextField SHALL support `OutputTransformation` that changes displayed content without changing `TextFieldState.text`.

#### Scenario: Android formatted display preserves raw state
- **WHEN** Android displays `13800138000` as `138 0013 8000`
- **THEN** `TextFieldState.text` SHALL remain `13800138000`

#### Scenario: iOS formatted display preserves raw state
- **WHEN** iOS displays `13800138000` as `138 0013 8000`
- **THEN** `TextFieldState.text` SHALL remain `13800138000`

#### Scenario: HarmonyOS formatted display preserves raw state
- **WHEN** HarmonyOS displays formatted text
- **THEN** `TextFieldState.text` SHALL remain the unformatted raw text

#### Scenario: Web formatted display preserves raw state
- **WHEN** Web displays formatted text
- **THEN** `TextFieldState.text` SHALL remain the unformatted raw text

#### Scenario: miniApp formatted display preserves raw state
- **WHEN** miniApp displays formatted text
- **THEN** `TextFieldState.text` SHALL remain the unformatted raw text

#### Scenario: macOS formatted display preserves raw state
- **WHEN** macOS displays formatted text
- **THEN** `TextFieldState.text` SHALL remain the unformatted raw text

### Requirement: Text post-processor transformation bridge
The system SHALL provide a transformation bridge that maps a Kuikly text post-processor name to output rendering while keeping raw shortcode text in state.

#### Scenario: Android bridge uses existing ImageSpan path
- **WHEN** `TextPostProcessorOutputTransformation("input")` processes `[smile]` on Android
- **THEN** the existing `KRTextFieldView` post-processor path SHALL display an `ImageSpan` while raw state remains `[smile]`

#### Scenario: iOS bridge uses attachment path
- **WHEN** `TextPostProcessorOutputTransformation("input")` processes `[smile]` on iOS
- **THEN** the renderer SHALL display an `NSTextAttachment` and restore raw `[smile]` for callbacks

#### Scenario: HarmonyOS bridge handles unsupported image spans
- **WHEN** `TextPostProcessorOutputTransformation("input")` processes `[smile]` on HarmonyOS before image-span support exists
- **THEN** the renderer SHALL preserve raw text and SHALL NOT corrupt input state

#### Scenario: Web bridge handles display output
- **WHEN** `TextPostProcessorOutputTransformation("input")` processes `[smile]` on Web
- **THEN** the renderer SHALL display transformed content where supported and keep raw text for callbacks

#### Scenario: miniApp bridge handles display output
- **WHEN** `TextPostProcessorOutputTransformation("input")` processes `[smile]` on miniApp
- **THEN** the renderer SHALL preserve raw text and apply display transformation where runtime support permits

#### Scenario: macOS bridge uses available attachment path
- **WHEN** `TextPostProcessorOutputTransformation("input")` processes `[smile]` on macOS
- **THEN** the renderer SHALL display attachment content where supported and restore raw `[smile]` for callbacks

### Requirement: Emoji insertion uses current selection
Custom emoji insertion SHALL use current raw selection and SHALL replace the selected raw range or insert at the raw cursor position.

#### Scenario: Android emoji replaces selected range
- **WHEN** Android selection covers raw text `abc` and business inserts `[smile]`
- **THEN** raw state SHALL replace `abc` with `[smile]`

#### Scenario: iOS emoji replaces selected range
- **WHEN** iOS selection covers raw text `abc` and business inserts `[smile]`
- **THEN** raw state SHALL replace `abc` with `[smile]`

#### Scenario: HarmonyOS emoji inserts at cursor
- **WHEN** HarmonyOS has a known cursor position and business inserts `[smile]`
- **THEN** raw state SHALL insert `[smile]` at that position

#### Scenario: Web emoji replaces selected range
- **WHEN** Web selection covers raw text `abc` and business inserts `[smile]`
- **THEN** raw state SHALL replace `abc` with `[smile]`

#### Scenario: miniApp emoji uses best-known cursor
- **WHEN** miniApp has a best-known cursor position and business inserts `[smile]`
- **THEN** raw state SHALL insert `[smile]` at that position

#### Scenario: macOS emoji inserts at cursor
- **WHEN** macOS has a known cursor position and business inserts `[smile]`
- **THEN** raw state SHALL insert `[smile]` at that position

### Requirement: Emoji deletion preserves shortcode boundaries
When display output represents a shortcode as a single visual emoji token, deletion of that token SHALL remove the full shortcode from raw state when raw/display mapping is available.

#### Scenario: Android deletes ImageSpan token
- **WHEN** the user deletes a displayed Android `ImageSpan` for `[smile]`
- **THEN** raw state SHALL remove the complete `[smile]` shortcode

#### Scenario: iOS deletes attachment token
- **WHEN** the user deletes an iOS attachment for `[smile]`
- **THEN** raw state SHALL remove the complete `[smile]` shortcode

#### Scenario: HarmonyOS deletes unsupported token as text
- **WHEN** HarmonyOS lacks visual token mapping and user deletes shortcode text
- **THEN** raw state SHALL remain valid text and SHALL NOT emit attachment placeholders

#### Scenario: Web deletes transformed token
- **WHEN** Web has raw/display mapping for a displayed emoji token and the user deletes it
- **THEN** raw state SHALL remove the complete shortcode

#### Scenario: miniApp deletes shortcode text
- **WHEN** miniApp lacks visual token mapping and user deletes shortcode text
- **THEN** raw state SHALL remain valid text and SHALL NOT emit attachment placeholders

#### Scenario: macOS deletes attachment token
- **WHEN** macOS has attachment mapping for `[smile]` and the user deletes it
- **THEN** raw state SHALL remove the complete shortcode

### Requirement: Existing post-processor usage remains compatible
Existing `Modifier.textPostProcessor(processor)` and `InputAttr.textPostProcessor(processor)` usage SHALL remain supported after transformation APIs are introduced.

#### Scenario: Android current Compose emoji demo remains compatible
- **WHEN** `TextFieldEmojiDemo` uses `Modifier.textPostProcessor("input")` on Android
- **THEN** the demo SHALL continue rendering shortcode emoji images

#### Scenario: iOS post-processor compatibility
- **WHEN** existing iOS text rendering uses `textPostProcessor`
- **THEN** the existing text post-processing behavior SHALL remain available

#### Scenario: HarmonyOS post-processor compatibility
- **WHEN** existing HarmonyOS text rendering uses `textPostProcessor`
- **THEN** the existing behavior SHALL remain available or safely no-op where unsupported

#### Scenario: Web post-processor compatibility
- **WHEN** existing Web text rendering uses `textPostProcessor`
- **THEN** the existing behavior SHALL remain available

#### Scenario: miniApp post-processor compatibility
- **WHEN** existing miniApp text rendering uses `textPostProcessor`
- **THEN** the existing behavior SHALL remain available

#### Scenario: macOS post-processor compatibility
- **WHEN** existing macOS text rendering uses `textPostProcessor`
- **THEN** the existing behavior SHALL remain available
