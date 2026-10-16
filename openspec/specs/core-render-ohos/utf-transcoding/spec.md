# UTF Transcoding

## Purpose

Define HarmonyOS Unicode conversion behavior at renderer and Kotlin/Native boundaries, including explicit lengths, caller-owned buffers, malformed-input semantics, and text offset units.

## Requirements

### Requirement: Lossless conversion of valid Unicode
The HarmonyOS renderer SHALL preserve Unicode scalar values when converting between UTF-8 and UTF-16 or between UTF-8 and UTF-32, including embedded NUL characters supplied with explicit lengths. Empty input SHALL produce empty output.

#### Scenario: HarmonyOS Unicode round trip
- **GIVEN** ASCII, Chinese, supplementary-plane emoji, and embedded NUL text on HarmonyOS
- **WHEN** it is converted from UTF-8 to UTF-16 or UTF-32 and back
- **THEN** the output bytes equal the original input and reported lengths exclude terminators

### Requirement: Malformed input follows Kotlin semantics
HarmonyOS UTF conversion SHALL follow Kotlin/Native UTF-8 semantics. Default decoding SHALL replace each Unicode maximal ill-formed subpart with U+FFFD. Strict decoding SHALL throw `CharacterCodingException`. Default UTF-16 encoding SHALL replace unpaired surrogates with U+FFFD, and the native strict encoding helper SHALL reject malformed UTF-16.

#### Scenario: HarmonyOS default UTF-8 decoding
- **GIVEN** malformed continuation bytes, overlong encodings, truncated sequences, or out-of-range code points
- **WHEN** the renderer or Kotlin string decoder processes them on HarmonyOS without strict validation
- **THEN** each Unicode maximal ill-formed subpart is replaced by one U+FFFD and valid adjacent text is retained

#### Scenario: HarmonyOS strict UTF-8 decoding
- **GIVEN** malformed UTF-8 and a strict Kotlin decode request
- **WHEN** native decoding reports failure
- **THEN** partial native output is discarded and Kotlin throws `CharacterCodingException`

#### Scenario: HarmonyOS default unpaired UTF-16 surrogates
- **GIVEN** isolated high or low surrogate units
- **WHEN** renderer text is encoded as UTF-8 on HarmonyOS
- **THEN** isolated surrogates are replaced with U+FFFD as before

#### Scenario: HarmonyOS strict UTF-16 encoding
- **GIVEN** isolated high or low surrogate units and a strict encoding request
- **WHEN** the native helper processes them
- **THEN** malformed UTF-16 is rejected rather than silently replaced

#### Scenario: HarmonyOS UTF-32 compatibility
- **GIVEN** input previously rejected or accepted by UTF-32 conversion
- **WHEN** the same input is converted on HarmonyOS
- **THEN** success output or exception behavior remains unchanged

### Requirement: Bounded caller-owned decoding
The HarmonyOS native decoding boundary SHALL accept explicit byte length, UTF-16 output capacity, and a strict-mode flag, and write only within caller-owned storage. It MUST reject invalid pointers and insufficient capacity with a failure sentinel. Strict mode MUST reject malformed UTF-8 with the same sentinel; callers MUST discard partial output on failure. Default mode MUST replace malformed input as described above. It SHALL NOT allocate output storage or retain pointers.

#### Scenario: HarmonyOS direct Kotlin output
- **GIVEN** valid UTF-8 and a pinned Kotlin character array with capacity at least the input byte count
- **WHEN** decoding is requested on HarmonyOS
- **THEN** output is written directly into the array and only the returned prefix becomes the Kotlin string

#### Scenario: HarmonyOS invalid decoding request
- **GIVEN** a nonempty input with null storage or output capacity smaller than its byte length
- **WHEN** native decoding is requested on HarmonyOS
- **THEN** it returns a failure sentinel without writing output

#### Scenario: HarmonyOS malformed default decoding
- **GIVEN** malformed UTF-8 read through the Kotlin JSON bridge
- **WHEN** decoding is requested without strict validation
- **THEN** the native boundary writes Kotlin-compatible replacement characters directly into the caller-owned array

#### Scenario: HarmonyOS malformed strict decoding
- **GIVEN** malformed UTF-8 read through the Kotlin JSON bridge
- **WHEN** decoding is requested with `throwOnInvalidSequence = true`
- **THEN** the native boundary reports failure and Kotlin throws `CharacterCodingException`

### Requirement: Text offsets remain UTF-16 based
HarmonyOS paragraph offsets SHALL count UTF-16 code units, not UTF-8 bytes, Unicode scalar values, or graphemes. Existing UTF-16 storage SHALL be reusable without encoding round trips.

#### Scenario: HarmonyOS supplementary text offset
- **GIVEN** a span containing one BMP character and one supplementary-plane emoji
- **WHEN** its paragraph offset is calculated on HarmonyOS
- **THEN** the offset increases by three UTF-16 units

### Requirement: Other platform behavior is unchanged
Android, iOS, macOS, Web, and miniApp text conversion behavior and dependencies SHALL remain unchanged by HarmonyOS conversion optimization.

#### Scenario: HarmonyOS dependency isolation
- **GIVEN** the updated project with native conversion acceleration
- **WHEN** non-HarmonyOS targets are configured or inspected
- **THEN** they do not acquire a simdutf dependency or new conversion calls
