## Why

Android already exposes a delegator-driven `syncSendEvent(event)` policy so host code can decide which page events must be delivered synchronously to Kotlin. Today iOS lacks the same policy hook, and HarmonyOS still relies mostly on render-side defaults instead of a host/delegator override point. This creates platform drift around `onBackPressed` and other future page events that may require synchronous dispatch.

## What Changes

- Add iOS delegator-driven `syncSendEvent(event)` policy, with default `onBackPressed` synchronous behavior.
- Add explicit `sendWithEvent(..., sync)` plumbing on iOS render view/core so render layers execute sync policy instead of deciding it themselves.
- Add HarmonyOS host/delegator-facing `syncSendEvent(event)` policy in ArkTS and explicit `sendEventSync` plumbing through ArkTS, NAPI, render view, and render core.
- Keep `onBackPressed` as the default synchronous event on all three platforms unless host code overrides the policy.
- Preserve existing asynchronous behavior for ordinary page events when no override is provided.

## Capabilities

### New Capabilities
- `render-sync-send-event-policy`: Host/delegator-controlled synchronous page-event dispatch across iOS and HarmonyOS, aligned with Android semantics.

### Modified Capabilities
- None.

## Impact

- Modules impacted:
  - `core-render-ios/`: delegator, render view, render core.
  - `core-render-ohos/`: ArkTS controller, NAPI bridge, render view, render core, type declarations.
- Affected APIs:
  - iOS adds delegator `syncSendEvent:` and explicit `sendWithEvent:data:sync:` plumbing.
  - HarmonyOS adds ArkTS `sendEventSync(...)` and host `syncSendEvent(event)` override point.
- Affected platforms: iOS and HarmonyOS for new parity work; Android acts as the semantic baseline.

## Non-goals

- Do not change Android behavior in this change.
- Do not make all page events synchronous by default.
- Do not move event policy decisions into render core.
- Do not redesign back-press module semantics beyond aligning dispatch timing.
