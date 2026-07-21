# touch-handled-bubbling Specification

## Purpose
TBD - created by archiving change align-touch-handled-bubbling. Update Purpose after archive.
## Requirements
### Requirement: Current-view raw touch callback consumes the current action
The system SHALL treat a raw touch action as consumed by the current view once the current view invokes its own raw touch callback for that action.

#### Scenario: iOS current view handles touchDown
- **WHEN** an iOS `KRView` receives `touchesBegan` and invokes the current view's `touchDown` callback
- **THEN** the renderer SHALL treat the `down` action as handled by the current view
- **AND** the renderer SHALL NOT continue propagating that same raw touch action to parent responders

#### Scenario: HarmonyOS current view handles touchDown
- **WHEN** a HarmonyOS `KRView` receives a touch `down` event and invokes the current view's `touchDown` callback
- **THEN** the renderer SHALL treat the `down` action as handled by the current view
- **AND** the renderer SHALL NOT continue propagating that same raw touch action to parent views

#### Scenario: Current view handles move/up/cancel
- **WHEN** the current renderer view invokes its own `touchMove`, `touchUp`, or `touchCancel` callback for the current action
- **THEN** that action SHALL be treated as handled by the current view
- **AND** that same action SHALL NOT continue propagating upward on either iOS or HarmonyOS

### Requirement: Unhandled raw touch actions continue to bubble upward
The system SHALL continue propagating a raw touch action upward only when the current view does not handle that action.

#### Scenario: iOS current view has no matching raw touch callback
- **WHEN** an iOS `KRView` receives a raw touch action and the current view does not invoke a matching raw touch callback for that action
- **THEN** the renderer SHALL allow the action to continue through the parent responder chain

#### Scenario: HarmonyOS current view has no matching raw touch callback
- **WHEN** a HarmonyOS `KRView` receives a raw touch action and the current view does not invoke a matching raw touch callback for that action
- **THEN** the renderer SHALL allow the action to continue propagating to parent views

### Requirement: Raw touch bubbling SHALL NOT depend on stop-propagation once the current action is handled
The system SHALL determine raw touch bubbling from the handled state of the current action, instead of requiring `stop-propagation` to be enabled.

#### Scenario: iOS handled action ignores stop-propagation for bubbling decision
- **WHEN** an iOS current view invokes its raw touch callback for an action
- **THEN** the renderer SHALL stop upward raw touch propagation for that action regardless of whether `stop-propagation` is set

#### Scenario: HarmonyOS handled action ignores stop-propagation for bubbling decision
- **WHEN** a HarmonyOS current view invokes its raw touch callback for an action
- **THEN** the renderer SHALL stop upward raw touch propagation for that action regardless of whether `stop-propagation` is set

### Requirement: Super-touch assisted paths preserve the same handled-first semantics on HarmonyOS
The system SHALL apply the same handled-first bubbling rule to HarmonyOS paths that coordinate with `SuperTouchHandler`.

#### Scenario: HarmonyOS parent super-touch path receives unhandled action
- **WHEN** a HarmonyOS child view participates in a parent `SuperTouchHandler` chain and does not invoke a matching raw touch callback for the current action
- **THEN** the renderer SHALL continue allowing the action to reach the parent path

#### Scenario: HarmonyOS parent super-touch path receives handled action
- **WHEN** a HarmonyOS child view participates in a parent `SuperTouchHandler` chain and invokes a matching raw touch callback for the current action
- **THEN** the renderer SHALL mark that action as stopped for the parent super-touch path
- **AND** the parent path SHALL NOT process that same raw touch action again

