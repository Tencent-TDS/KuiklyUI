# mobile-test-ios-back Specification

## Purpose
TBD - created by archiving change appium-mobile-test-mvp. Update Purpose after archive.
## Requirements
### Requirement: iOS back SHALL tap nav_back testTag

The mobile-test driver on iOS MUST implement `back()` by tapping an element with `testTag` `nav_back`, and MUST NOT invoke Appium system back (`mobile:back` or equivalent UIKit pop).

#### Scenario: Compose demo page with ComposeNavigationBar

- **GIVEN** an iOS session on a Compose demo page that uses `ComposeNavigationBar`
- **AND** the page was opened via Kuikly navigation from a parent page
- **WHEN** the client calls `MobileDriver.back()` or `POST /back`
- **THEN** the driver taps the element with testTag `nav_back`
- **AND** the app returns to the previous page without crashing

#### Scenario: Self DSL demo page with NavigationBar

- **GIVEN** an iOS session on a Self DSL demo page that uses demo `NavigationBar`
- **AND** the back arrow has testTag `nav_back`
- **WHEN** the client calls `back()`
- **THEN** the driver taps `nav_back`
- **AND** the app returns to the previous page

#### Scenario: nav_back not present

- **GIVEN** an iOS session on a page without `nav_back` testTag
- **WHEN** the client calls `back()`
- **THEN** the driver returns an error indicating `nav_back` was not found
- **AND** the error message suggests adding testTag or using `restartApp()`

### Requirement: Android back SHALL remain system back

On Android, `back()` MUST continue to use the system Back key via Appium `driver.back()`.

#### Scenario: Android page navigation

- **GIVEN** an Android session with an active page stack
- **WHEN** the client calls `back()`
- **THEN** the driver invokes Appium system back
- **AND** the app performs default back behavior (pop or BackHandler consume)

### Requirement: HTTP server SHALL expose POST /back

The mobile-test HTTP server MUST provide `POST /back` that delegates to `MobileDriver.back()` for the active session.

#### Scenario: iOS back via HTTP

- **GIVEN** an active iOS mobile-test session
- **WHEN** the client sends `POST /back` with empty body
- **THEN** the server responds `{"ok": true}` after tapping `nav_back`
- **OR** responds `{"ok": false, "error": "..."}` if `nav_back` is missing

### Requirement: iOS nav_back SHALL be exposed in accessibility tree

When demo navigation bars set `testTag("nav_back")` on the back control, the iOS render layer MUST expose that leaf as an accessibility element so XCUITest and `MobileDriver.back()` can find it.

#### Scenario: nav_back visible after render fix

- **GIVEN** an iOS debug build with nav_back testTag on NavigationBar Image
- **WHEN** page source is captured on a sub-page
- **THEN** an accessible node with name or identifier containing `nav_back` exists

