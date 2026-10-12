## ADDED Requirements

### Requirement: HarmonyOS self-DSL SHALL expose a public `ohosViewHitTestMode` API with constants
The framework SHALL expose a public self-DSL API named `ohosViewHitTestMode(mode: String)` for HarmonyOS hit-test control. The API MUST emit the native prop key `hit-test-ohos`, and it MUST provide public string constants for `default`, `block`, `transparent`, and `none` so business code does not need to handwrite raw strings.

#### Scenario: Business uses `ohosViewHitTestMode` with public constants
- **GIVEN** a business view is written with the self-DSL on HarmonyOS
- **WHEN** the developer calls `ohosViewHitTestMode(...)` with a framework-provided constant
- **THEN** the framework MUST emit the underlying prop key `hit-test-ohos` with the constant string value

#### Scenario: All supported public constants are available
- **GIVEN** a developer wants to control HarmonyOS hit-test mode
- **WHEN** they use the framework public API
- **THEN** the framework MUST provide constants representing `default`, `block`, `transparent`, and `none`

### Requirement: HarmonyOS KRView and forwardview v1 MUST both support `hit-test-ohos`
HarmonyOS native render MUST support the same `hit-test-ohos` property for both ordinary `KRView`-based views and `forwardview v1`. For `forwardview v1`, the property MUST be consumed in the C++ wrapper layer and SHALL NOT require ArkTS inner views to understand the property.

#### Scenario: Ordinary KRView continues to honor `hit-test-ohos`
- **GIVEN** a normal HarmonyOS view rendered by `KRView`
- **WHEN** the business sets `ohosViewHitTestMode(...)` to any supported constant
- **THEN** the native view MUST apply the corresponding hit-test mode using the existing `KRView` behavior

#### Scenario: forwardview v1 consumes the same property
- **GIVEN** a HarmonyOS `forwardview v1` instance
- **WHEN** the business sets `ohosViewHitTestMode(...)` to any supported constant
- **THEN** `KRForwardArkTSView` MUST apply the corresponding hit-test mode in the C++ wrapper and MUST stop forwarding that property to ArkTS `SetViewProp`

### Requirement: Default and reset behavior MUST preserve each view type's existing default
Applying the public HarmonyOS hit-test API MUST NOT change existing defaults when the property is not set. Removing or resetting the property MUST restore the original default behavior for that view type.

#### Scenario: Ordinary KRView keeps its existing default behavior
- **GIVEN** a normal HarmonyOS view rendered by `KRView`
- **WHEN** `ohosViewHitTestMode(...)` is not set
- **THEN** the view MUST keep the same default hit-test behavior it had before this change

#### Scenario: forwardview v1 resets to transparent
- **GIVEN** a HarmonyOS `forwardview v1` instance previously applied `hit-test-ohos`
- **WHEN** the property is removed or reset
- **THEN** the C++ wrapper MUST restore `ARKUI_HIT_TEST_MODE_TRANSPARENT`

### Requirement: Unsupported platforms MUST ignore the HarmonyOS-only API safely
Android, iOS, Web, and miniApp renderers MUST tolerate the new public API without behavior change. If those platforms do not implement the property, they MUST ignore it safely and SHALL keep their existing behavior unchanged.

#### Scenario: Non-HarmonyOS platforms tolerate the API
- **GIVEN** a business view uses `ohosViewHitTestMode(...)` on Android, iOS, Web, or miniApp
- **WHEN** the property is applied during rendering
- **THEN** the renderer MUST keep its existing behavior and MUST NOT fail because of the property
