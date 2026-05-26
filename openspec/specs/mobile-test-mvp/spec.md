# mobile-test-mvp Specification

## Purpose
TBD - created by archiving change appium-mobile-test-mvp. Update Purpose after archive.
## Requirements
### Requirement: MobileDriver SHALL abstract Appium for AI clients

The mobile-test tool MUST expose a `MobileDriver` interface (start/stop session, getSnapshot/getViewTree, tap, input, scroll, back, waitFor, assert*, screenshot) and MUST NOT require AI clients to call Appium WebDriver APIs directly.

#### Scenario: Scenario script uses driver only

- **GIVEN** a test scenario under harness `scenarios/`
- **WHEN** the scenario runs against a booted simulator
- **THEN** it uses `AppiumMobileDriver` implementing `MobileDriver`
- **AND** does not import `webdriverio` session methods outside the driver implementation

### Requirement: HTTP server SHALL expose curl-friendly test API

The tool MUST provide an HTTP server with endpoints including `/status`, `/start-session`, `/view-tree`, `/tap`, `/input`, `/scroll`, `/back`, `/assert-visible`, `/assert-text`, `/restart-app`, `/stop-session`.

#### Scenario: AI curl workflow

- **GIVEN** Appium and HTTP server running
- **WHEN** client POSTs `/start-session` with required platform fields
- **AND** GETs `/view-tree?visible=true`
- **AND** POSTs `/tap` with selector
- **THEN** each response is JSON `{ok: true, ...}` or `{ok: false, error: "..."}`

### Requirement: view-tree SHALL expose Kuikly semantics for automation

The normalized view-tree text MUST include Kuikly view class names (when debugUIInspector enabled), testTag, text, bounds, and platform raw type where available.

#### Scenario: Compose demo with debugUIInspector

- **GIVEN** a debug demo page with `debugUIInspector() = true` and `Modifier.testTag`
- **WHEN** client fetches view-tree
- **THEN** nodes show class names and testTag values usable as selectors

### Requirement: Evidence SHALL write under checkout logs directory

Runtime snapshots, screenshots, and scenario reports MUST resolve to `<checkout-root>/logs/` where checkout root contains `settings.gradle.kts` (main clone or git worktree).

#### Scenario: Worktree execution

- **GIVEN** tool run from a git worktree checkout
- **WHEN** E2E completes
- **THEN** reports appear under that worktree's `logs/` directory

### Requirement: Tool distribution SHALL live in kuikly-harness not SDK

The mobile-test implementation MUST reside in kuikly-harness `.agents/skills/kuikly-mobile-test/` and MUST NOT ship inside KuiklyUI SDK release artifacts.

#### Scenario: SDK repo layout

- **GIVEN** KuiklyUI SDK repository
- **WHEN** inspecting published SDK modules
- **THEN** there is no `tools/mobile-test/` package in the SDK tree

### Requirement: SDK SHALL expose testTag to native accessibility trees

Kuikly render layers on iOS and Android MUST map `testTag` to platform accessibility identifiers such that Appium can locate elements when combined with appropriate a11y element exposure rules.

#### Scenario: iOS leaf testTag

- **GIVEN** a leaf native view with `css_testTag` set (e.g. nav_back Image)
- **WHEN** XCUITest page source is captured
- **THEN** the node appears as an accessibility element with identifier or name containing the testTag

