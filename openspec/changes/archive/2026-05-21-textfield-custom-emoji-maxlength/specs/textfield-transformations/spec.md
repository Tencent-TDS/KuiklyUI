## ADDED Requirements

### Requirement: Custom emoji insertion SHALL respect renderer-backed maxLength
When Compose `TextField` combines custom emoji rendering with `Modifier.maxLength`, the system SHALL evaluate length against the raw text after applying `replace(selection.start, selection.end, shortCode)` and SHALL use the active renderer strategy for `CHARACTER`, `BYTE`, or `VISUAL_WIDTH`.

#### Scenario: Android middle insertion within remaining CHARACTER quota
- **WHEN** Android `TextField` selection is in the middle of the raw text and business inserts `[smile]` through `replace(selection.start, selection.end, shortCode)` with remaining `CHARACTER` quota available
- **THEN** the renderer SHALL insert the full shortcode at that raw selection and SHALL report the accepted length through `textInputStateChange.length`

#### Scenario: Android middle insertion exceeds remaining BYTE quota
- **WHEN** Android `TextField` selection is in the middle of the raw text and inserting `[smile]` would exceed the remaining `BYTE` quota
- **THEN** the renderer SHALL reject the full shortcode insertion, SHALL keep the unaffected raw prefix and suffix unchanged, and SHALL emit `textLengthBeyondLimit`

#### Scenario: Android selected-range replace uses VISUAL_WIDTH result
- **WHEN** Android `TextField` replaces a selected raw range with `[smile]` under `VISUAL_WIDTH` mode
- **THEN** the renderer SHALL evaluate the resulting raw text with the visual-width strategy before deciding whether the replacement is accepted

#### Scenario: iOS single-line middle insertion exceeds remaining BYTE quota
- **WHEN** iOS single-line `TextField` selection is in the middle of the raw text and inserting `[smile]` would exceed the remaining `BYTE` quota
- **THEN** the renderer SHALL reject the full shortcode insertion, SHALL keep the raw text unchanged, and SHALL return a valid raw selection

#### Scenario: iOS selected-range replace within CHARACTER quota
- **WHEN** iOS `TextField` or `TextArea` replaces a selected raw range with `[smile]` and the resulting raw text stays within `CHARACTER` quota
- **THEN** the renderer SHALL accept the full shortcode replacement and SHALL return the accepted raw text and selection through `textInputStateChange`

### Requirement: Custom emoji shortcode insertion SHALL be atomic under maxLength
When a business action inserts a custom emoji shortcode such as `[smile]`, the system SHALL treat the shortcode as one atomic edit and SHALL NOT write a partial shortcode token when maxLength rejects the edit.

#### Scenario: Android over-limit insertion does not write partial token
- **WHEN** Android `TextField` receives a shortcode insertion that cannot fully fit in the remaining maxLength budget
- **THEN** the raw text SHALL NOT contain a partial token such as `[smi` and the previous raw text SHALL remain intact

#### Scenario: iOS over-limit insertion does not write partial token
- **WHEN** iOS `TextField` or `TextArea` receives a shortcode insertion that cannot fully fit in the remaining maxLength budget
- **THEN** the raw text SHALL NOT contain a partial token and the previous raw text SHALL remain intact

### Requirement: Limit callbacks SHALL reflect renderer-computed emoji length
`Modifier.onLimitChange` and `textLengthBeyondLimit` SHALL continue to report the renderer-computed length result for custom emoji input, rather than a Kotlin-side string-length estimate.

#### Scenario: Android reports renderer-computed custom emoji length
- **WHEN** Android accepts or rejects a custom emoji insertion under `Modifier.maxLength`
- **THEN** `onLimitChange` SHALL use the length returned from renderer callbacks and `limit = true` SHALL indicate that the length has reached or exceeded the configured limit

#### Scenario: iOS reports renderer-computed custom emoji length
- **WHEN** iOS accepts or rejects a custom emoji insertion under `Modifier.maxLength`
- **THEN** `onLimitChange` SHALL use the length returned from renderer callbacks and `limit = true` SHALL indicate that the length has reached or exceeded the configured limit

### Requirement: TextField emoji demo SHALL cover maxLength-limited selection edits
The Compose demo for custom emoji input SHALL reuse `TextFieldEmojiDemo.kt` and SHALL include regression coverage for middle insertion, selected-range replacement, and single-line behavior under maxLength.

#### Scenario: Demo covers Android selection-aware emoji limit cases
- **WHEN** developers open `TextFieldEmojiDemo.kt` for Android verification
- **THEN** the demo SHALL contain cases that exercise middle insertion, selected-range replacement, and CHARACTER / BYTE / VISUAL_WIDTH length modes

#### Scenario: Demo covers iOS single-line regression case
- **WHEN** developers open `TextFieldEmojiDemo.kt` for iOS verification
- **THEN** the demo SHALL contain a single-line case used to validate custom emoji insertion, limit rejection, and cursor recovery
