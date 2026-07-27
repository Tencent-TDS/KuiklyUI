## MODIFIED Requirements

### Requirement: Compose IME phase1 baseline SHALL NOT require synchronous dispatch opt-in

The `compose-ime-insets` phase1 baseline SHALL remain correct under the existing ordinary page-event dispatch path. Host, delegator, or controller code MUST NOT be required to mark `imeInsetsDidChanged` as a synchronous page event in order for `WindowInsets.ime`, `Modifier.imePadding()`, or `Scaffold` default IME avoidance to work correctly. Any generic sync-send mechanism MAY continue to exist independently, but it SHALL NOT be part of the phase1 capability contract.

#### Scenario: Android phase1 IME avoidance works without sync-send opt-in
- **GIVEN** Android host code does not mark `imeInsetsDidChanged` as a synchronous event
- **WHEN** a Compose page relies on `WindowInsets.ime`, `imePadding()`, or `Scaffold`
- **THEN** phase1 keyboard avoidance SHALL still work correctly through the normal page-event dispatch path

#### Scenario: iOS phase1 IME avoidance works without sync-send opt-in
- **GIVEN** iOS host or delegator code does not mark `imeInsetsDidChanged` as a synchronous event
- **WHEN** a Compose page relies on `WindowInsets.ime`, `imePadding()`, or `Scaffold`
- **THEN** phase1 keyboard avoidance SHALL still work correctly through the normal page-event dispatch path

#### Scenario: HarmonyOS phase1 IME avoidance works without sync-send opt-in
- **GIVEN** HarmonyOS host or controller code does not mark `imeInsetsDidChanged` as a synchronous event
- **WHEN** a Compose page relies on `WindowInsets.ime`, `imePadding()`, or `Scaffold`
- **THEN** phase1 keyboard avoidance SHALL still work correctly through the normal page-event dispatch path
