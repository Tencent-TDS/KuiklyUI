## ADDED Requirements

### Requirement: Host-controlled sync policy for page events
The system SHALL allow host/delegator code to decide whether a page event is dispatched synchronously, instead of forcing that decision to live in render core.

#### Scenario: iOS host decides sync policy
- **WHEN** iOS host code sends a page event through `KuiklyRenderViewControllerBaseDelegator`
- **THEN** the delegator SHALL determine whether the event is synchronous through `syncSendEvent:` before forwarding to render view/core

#### Scenario: HarmonyOS host decides sync policy
- **WHEN** HarmonyOS host code sends a page event through `KRNativeRenderController`
- **THEN** the controller SHALL determine whether the event is synchronous through `syncSendEvent(event)` before forwarding to native render code

### Requirement: Default back-press synchronous behavior
The system SHALL keep `onBackPressed` synchronous by default unless host code explicitly overrides the policy.

#### Scenario: iOS default back press
- **GIVEN** host code does not override `syncSendEvent:`
- **WHEN** `onBackPressed` is sent on iOS
- **THEN** the event SHALL be dispatched synchronously

#### Scenario: HarmonyOS default back press
- **GIVEN** host code does not override `syncSendEvent(event)`
- **WHEN** `onBackPressed` is sent on HarmonyOS
- **THEN** the event SHALL be dispatched synchronously

### Requirement: Default async behavior for ordinary page events
The system SHALL keep ordinary page events asynchronous by default unless host code explicitly requests synchronous delivery.

#### Scenario: iOS ordinary event remains async
- **GIVEN** host code does not override `syncSendEvent:`
- **WHEN** iOS sends an event other than `onBackPressed`
- **THEN** the event SHALL use the asynchronous dispatch path

#### Scenario: HarmonyOS ordinary event remains async
- **GIVEN** host code does not override `syncSendEvent(event)`
- **WHEN** HarmonyOS sends an event other than `onBackPressed`
- **THEN** the event SHALL use the asynchronous dispatch path

### Requirement: Render layers execute explicit sync policy
Once host/delegator code decides sync policy, render view and render core SHALL execute that policy without re-owning the business decision.

#### Scenario: iOS explicit sync is forwarded to render core
- **WHEN** iOS delegator decides an event must be synchronous
- **THEN** `KuiklyRenderView` and `KuiklyRenderCore` SHALL receive an explicit sync flag and execute synchronous dispatch

#### Scenario: HarmonyOS explicit sync is forwarded to render core
- **WHEN** HarmonyOS host controller decides an event must be synchronous
- **THEN** ArkTS, NAPI, `KRRenderView`, and `KRRenderCore` SHALL forward an explicit sync flag and execute synchronous dispatch

### Requirement: HarmonyOS default sync policy for root size change remains opt-in
HarmonyOS SHALL not treat `rootViewSizeDidChanged` as synchronous by default once host/delegator policy is available.

#### Scenario: HarmonyOS default root size change stays async
- **GIVEN** host code does not override `syncSendEvent`
- **WHEN** `rootViewSizeDidChanged` is sent on HarmonyOS
- **THEN** the event SHALL use the asynchronous dispatch path

### Requirement: HarmonyOS may trigger early synchronous size handling when business code opts in
When business code overrides `syncSendEvent` to return `true` for `rootViewSizeDidChanged`, HarmonyOS SHALL execute the size-change handling early enough to affect the current layout pass.

#### Scenario: Business enables synchronous root size change handling
- **GIVEN** business code overrides `syncSendEvent` to return `true` for `rootViewSizeDidChanged`
- **WHEN** the root component size changes on HarmonyOS
- **THEN** `onSizeChange` SHALL trigger synchronous Kotlin layout and native attribute updates before the current layout calculation completes

#### Scenario: Business does not enable synchronous root size change handling
- **GIVEN** business code does not override `syncSendEvent` for `rootViewSizeDidChanged`
- **WHEN** the root component size changes on HarmonyOS
- **THEN** `onSizeChange` SHALL NOT take the early synchronous handling path
- **AND** the original async handling path SHALL remain responsible for processing the size change

### Requirement: HarmonyOS size-change fallback remains available
HarmonyOS SHALL keep `onAreaChange` as a fallback path for root size changes so correctness does not depend on the early synchronous path.

#### Scenario: Fallback is skipped after early synchronous handling already covered the size change
- **GIVEN** business code enables synchronous handling for `rootViewSizeDidChanged`
- **WHEN** `onAreaChange` runs after `onSizeChange` has already updated the tracked size
- **THEN** HarmonyOS SHALL detect that no additional size-change processing is needed and SHALL skip redundant work

#### Scenario: Fallback handles root size change when early synchronous handling is disabled
- **GIVEN** business code does not enable synchronous handling for `rootViewSizeDidChanged`
- **WHEN** `onAreaChange` receives the new size on HarmonyOS
- **THEN** HarmonyOS SHALL process the size change through the original fallback path

### Requirement: Compatibility entry points remain available
Existing event-sending entry points SHALL remain available so current callers are not forced to adopt the explicit-sync APIs immediately.

#### Scenario: iOS compatibility wrapper remains valid
- **WHEN** existing iOS code calls `sendWithEvent:data:`
- **THEN** the call SHALL remain valid and SHALL route through the new policy-aware path

#### Scenario: HarmonyOS compatibility wrapper remains valid
- **WHEN** existing HarmonyOS code calls `sendEvent(event, data)`
- **THEN** the call SHALL remain valid and SHALL route through the new policy-aware path
