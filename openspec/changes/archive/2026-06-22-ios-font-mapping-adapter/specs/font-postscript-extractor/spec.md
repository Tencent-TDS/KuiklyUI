## ADDED Requirements

### Requirement: PostScript Name Extraction from Font File
The system SHALL be able to extract the real PostScript Name from a font file (`.ttf`, `.otf`, etc.) using `CGFontCopyPostScriptName` before or during the font registration process.

**iOS only.** This SHALL be done via CoreText APIs:

```
CGDataProviderRef provider = CGDataProviderCreateWithURL(fontURL);
CGFontRef cgFont = CGFontCreateWithDataProvider(provider);
NSString *postScriptName = (__bridge_transfer NSString *)CGFontCopyPostScriptName(cgFont);
```

#### Scenario: Valid font file with embedded PostScript Name
- **WHEN** a valid `.ttf` or `.otf` font file is read via `CGDataProviderCreateWithURL`
- **THEN** `CGFontCreateWithDataProvider` SHALL succeed, and `CGFontCopyPostScriptName` SHALL return a non-nil `NSString` representing the font's real PostScript Name (e.g., `"CustomSans-Regular"`, `"PingFangSC-Medium"`)

#### Scenario: Font file is malformed or invalid
- **WHEN** the font file is corrupt, not a valid font format, or the URL points to a non-existent file
- **THEN** `CGFontCreateWithDataProvider` SHALL return `NULL`, and the method SHALL return `nil` without crashing

### Requirement: Registration Returns Extracted PostScript Name
`registerFontAtLocalURL:` SHALL be refactored from returning `BOOL` to returning `NSString *` (nullable).

- Return value SHALL be the extracted PostScript Name on success
- Return value SHALL be `nil` on any failure (font file not found, registration failed, etc.)

#### Scenario: Successful registration with PostScript Name
- **WHEN** `registerFontAtLocalURL:` registers a valid font
- **THEN** the returned `NSString *` SHALL equal the result of `CGFontCopyPostScriptName` called on the same font file
- **AND** `[UIFont fontWithName:returnedString size:12]` SHALL return a non-nil `UIFont`

#### Scenario: Registration failure
- **WHEN** `CTFontManagerRegisterFontsForURL` fails (e.g., font already registered with same URL, or scope permission denied)
- **THEN** the method SHALL return `nil` and NOT throw/raise any exceptions

### Requirement: Cached Font Check Before Re-registration
Before calling `CTFontManagerRegisterFontsForURL`, the system SHALL check if the extracted PostScript Name is already available via `[UIFont fontWithName:postScriptName size:12]`.

#### Scenario: Font already registered
- **WHEN** `[UIFont fontWithName:extractedName size:12]` returns non-nil (font was registered in a previous call)
- **THEN** `CTFontManagerRegisterFontsForURL` SHALL NOT be called again; the extracted PostScript Name SHALL be returned directly

#### Scenario: Font newly registered
- **WHEN** `[UIFont fontWithName:extractedName size:12]` returns nil (font not yet in system)
- **THEN** `CTFontManagerRegisterFontsForURL` SHALL be called, and on success the PostScript Name SHALL be returned
