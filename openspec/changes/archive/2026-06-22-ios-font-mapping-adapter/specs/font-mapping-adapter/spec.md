## ADDED Requirements

### Requirement: Font Mapping Adapter Protocol Extension
The `KuiklyFontProtocol` SHALL provide an optional method that allows business to map a `fontFamily` string to a concrete `UIFont` instance.

**iOS only.** The method signature SHALL be:

```
- (nullable UIFont *)hr_fontWithFontFamily:(NSString *)fontFamily
                                  fontSize:(CGFloat)fontSize
                                fontWeight:(UIFontWeight)fontWeight
                             contextParams:(nullable KuiklyContextParam *)contextParam;
```

- `fontFamily` SHALL be the exact string set from the Kotlin side (potentially a business alias, not matching the font's real PostScript Name).
- `contextParam.resourceFolderUrl` SHALL be available for constructing font file paths when provided.
- The method SHALL return `nil` to fall through to the existing loading chain.

#### Scenario: Business returns a valid UIFont
- **WHEN** `hr_fontWithFontFamily:` is called with fontFamily = "CustomSans", fontSize = 16, contextParam provided with a valid `resourceFolderUrl`
- **THEN** the implementation SHALL locate the font file at the constructed path, register it, extract the PostScript Name via `CGFontCopyPostScriptName`, create a `[UIFont fontWithName:realPostScriptName size:16]`, and return the `UIFont` instance

#### Scenario: Business chooses to ignore the adapter (return nil)
- **WHEN** `hr_fontWithFontFamily:` returns `nil`
- **THEN** `KRConvertUtil +UIFont:` SHALL continue to the next priority step (try `hr_loadCustomFont:` + `[UIFont fontWithName:fontFamily size:fontSize]`)

#### Scenario: Business has not implemented the optional method
- **WHEN** the font handler does not respond to the `hr_fontWithFontFamily:` selector
- **THEN** the call SHALL be forwarded to `nil` and skipped, behaving exactly as before the change

### Requirement: `KRConvertUtil +UIFont:` Priority Chain Update
The `+UIFont:` method in `KRConvertUtil.m` SHALL adopt the following priority order for resolving a `UIFont` when `fontFamily` is non-empty:

1. `[UIFont fontWithName:fontFamily size:fontSize]` (static registration check — exact PostScript Name match)
2. `[KRFontModule hr_fontWithFontFamily:...]` (new adapter — business returns UIFont directly)
3. `[KRFontModule hr_loadCustomFont:...]` + `[UIFont fontWithName:fontFamily size:fontSize]` (legacy: register file + retry with fontFamily)
4. `[[KuiklyRenderBridge componentExpandHandler] hr_fontWithFontFamily:...]` (old compatibility layer)
5. System default font fallback

#### Scenario: Static registration matches first
- **WHEN** `fontFamily = "PingFangSC-Regular"` and the font is already available via `[UIFont fontWithName:size:]`
- **THEN** the method returns immediately at priority 1, never reaching the adapter or any fallback

#### Scenario: Adapter resolves a previously unknown alias
- **WHEN** `fontFamily = "BrandFont"` (a business alias), font is NOT registered, adapter returns a `UIFont`
- **THEN** the adapter's `UIFont` is returned, priorities 3–5 are skipped

#### Scenario: Adapter returns nil, legacy path succeeds
- **WHEN** adapter returns nil, `hr_loadCustomFont:` successfully registers the font file, and `[UIFont fontWithName:fontFamily size:fontSize]` finds a match (legacy scenario where fontFamily coincidentally matches PostScript Name)
- **THEN** the legacy path returns the `UIFont`

#### Scenario: All custom paths fail
- **WHEN** fontFamily is non-empty, adapter returns nil, `hr_loadCustomFont:` fails or returns NO
- **THEN** the method SHALL fall through to the system default font (consider italic, fontWeight)

### Requirement: ContextParam Injection for Normal Layer Handler
The `KuiklyRenderLayerHandler` SHALL inject `_contextParam` into every shadow created by `p_createShadowHandlerWithTag:viewName:`.

This SHALL be done by calling `[shadow hrv_setPropWithKey:@"contextParam" propValue:_contextParam]` directly, NOT via `[self setContextParamToShadow:shadow]`, to avoid a main-thread assertion failure — shadow creation runs on the context thread.

#### Scenario: Normal LayerHandler creates a shadow
- **WHEN** `p_createShadowHandlerWithTag:` creates a new shadow
- **THEN** the shadow's props SHALL contain a key-value pair `@"contextParam"` → `_contextParam` immediately after creation, before any `setShadowProp` calls arrive from Kotlin

#### Scenario: TurboDisplay creates a shadow via normal LayerHandler (not restoration path)
- **WHEN** TurboDisplay delegates to the real `_renderLayerHandler` for `createShadow:viewName:`
- **THEN** the created shadow SHALL also receive `contextParam` injection, identical to the normal path

#### Scenario: shadow's contextParam is available during layout measurement
- **WHEN** the shadow's `hrv_setPropWithKey:propValue:` is later called for font properties, and `KRConvertUtil +UIFont:` reads `json[@"contextParam"]`
- **THEN** the `contextParam` SHALL be non-nil and contain the correct `resourceFolderUrl`
