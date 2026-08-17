## Requirements

### Requirement: Compose DSL SHALL expose a bring-into-view request API

Compose DSL SHALL provide a public `BringIntoViewRequester` and `Modifier.bringIntoViewRequester(...)` so that a composed node can request its nearest supported scroll container to move it into the visible viewport. Invoking the request without a supported responder ancestor MUST return safely without crashing.

#### Scenario: Android requester can be attached and invoked safely
- **GIVEN** an Android Compose page attaches `Modifier.bringIntoViewRequester(requester)` to a composed node
- **WHEN** the node invokes `requester.bringIntoView()`
- **THEN** the request SHALL be delivered to the nearest supported responder when one exists, and SHALL return safely without crashing when none exists

#### Scenario: iOS requester can be attached and invoked safely
- **GIVEN** an iOS Compose page attaches `Modifier.bringIntoViewRequester(requester)` to a composed node
- **WHEN** the node invokes `requester.bringIntoView()`
- **THEN** the request SHALL be delivered to the nearest supported responder when one exists, and SHALL return safely without crashing when none exists

#### Scenario: HarmonyOS requester can be attached and invoked safely
- **GIVEN** a HarmonyOS Compose page attaches `Modifier.bringIntoViewRequester(requester)` to a composed node
- **WHEN** the node invokes `requester.bringIntoView()`
- **THEN** the request SHALL be delivered to the nearest supported responder when one exists, and SHALL return safely without crashing when none exists

### Requirement: Focused text inputs SHALL request bring-into-view when obscured by IME

When a Compose text input becomes focused and its bounds intersect the IME-occluded area, the input chain SHALL automatically request bring-into-view for that focused input. If the focused input is already fully visible, the framework MUST NOT start an automatic scroll.

#### Scenario: Android focused bottom input automatically requests visibility
- **GIVEN** an Android Compose page contains a focused text input inside a supported vertical scroll container, and the input becomes partially obscured by `WindowInsets.ime`
- **WHEN** the input gains focus or remains focused while the IME grows to cover it
- **THEN** the framework SHALL automatically issue a bring-into-view request for that focused input

#### Scenario: iOS focused bottom input automatically requests visibility
- **GIVEN** an iOS Compose page contains a focused text input inside a supported vertical scroll container, and the input becomes partially obscured by `WindowInsets.ime`
- **WHEN** the input gains focus or remains focused while the IME grows to cover it
- **THEN** the framework SHALL automatically issue a bring-into-view request for that focused input

#### Scenario: HarmonyOS focused bottom input automatically requests visibility
- **GIVEN** a HarmonyOS Compose page contains a focused text input inside a supported vertical scroll container, and the input becomes partially obscured by `WindowInsets.ime`
- **WHEN** the input gains focus or remains focused while the IME grows to cover it
- **THEN** the framework SHALL automatically issue a bring-into-view request for that focused input

### Requirement: Vertical `LazyListState` containers SHALL fulfill bring-into-view requests for composed focused items

A vertical lazy list backed by `LazyListState` SHALL fulfill bring-into-view requests for focused items that are already composed, using the focused item's current bounds and viewport information to compute the minimum required vertical scroll delta.

#### Scenario: Android lazy list scrolls composed focused input into view
- **GIVEN** an Android Compose page uses `LazyColumn`, and the focused text input belongs to a currently composed item that is partially hidden by the keyboard
- **WHEN** that focused input receives a bring-into-view request
- **THEN** the lazy list SHALL animate vertical scrolling until the focused input is fully visible above the IME area

#### Scenario: iOS lazy list scrolls composed focused input into view
- **GIVEN** an iOS Compose page uses `LazyColumn`, and the focused text input belongs to a currently composed item that is partially hidden by the keyboard
- **WHEN** that focused input receives a bring-into-view request
- **THEN** the lazy list SHALL animate vertical scrolling until the focused input is fully visible above the IME area

#### Scenario: HarmonyOS lazy list scrolls composed focused input into view
- **GIVEN** a HarmonyOS Compose page uses `LazyColumn`, and the focused text input belongs to a currently composed item that is partially hidden by the keyboard
- **WHEN** that focused input receives a bring-into-view request
- **THEN** the lazy list SHALL animate vertical scrolling until the focused input is fully visible above the IME area

### Requirement: Bring-into-view evaluation SHALL rerun when the visible viewport changes

For a currently focused text input, Compose DSL SHALL re-evaluate whether bring-into-view is needed whenever the focused bounds change, the IME inset changes, or the scroll container viewport changes. Re-evaluation MUST avoid infinite scroll loops and MUST NOT scroll when the target is already fully visible.

#### Scenario: Android re-evaluates after keyboard appears after focus
- **GIVEN** an Android Compose page focuses a text input before the keyboard has fully appeared
- **WHEN** `WindowInsets.ime` later increases and causes the focused input to become occluded
- **THEN** Compose DSL SHALL re-evaluate visibility and perform bring-into-view if the focused input is no longer fully visible

#### Scenario: iOS re-evaluates after keyboard appears after focus
- **GIVEN** an iOS Compose page focuses a text input before the keyboard has fully appeared
- **WHEN** `WindowInsets.ime` later increases and causes the focused input to become occluded
- **THEN** Compose DSL SHALL re-evaluate visibility and perform bring-into-view if the focused input is no longer fully visible

#### Scenario: HarmonyOS re-evaluates after keyboard appears after focus
- **GIVEN** a HarmonyOS Compose page focuses a text input before the keyboard has fully appeared
- **WHEN** `WindowInsets.ime` later increases and causes the focused input to become occluded
- **THEN** Compose DSL SHALL re-evaluate visibility and perform bring-into-view if the focused input is no longer fully visible
