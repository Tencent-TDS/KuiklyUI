## Why

Android already exposes a delegator-driven `syncSendEvent(event)` policy so host code can decide which page events must be delivered synchronously to Kotlin. iOS previously lacked the same policy hook, and HarmonyOS initially relied on render-side defaults instead of a host/delegator override point. This created platform drift around `onBackPressed` and future page events that may require synchronous dispatch.

After the initial parity work, HarmonyOS also proved that `rootViewSizeDidChanged` needs a business-controlled synchronous path during rotation or resize to prevent a one-frame white screen. If that event is delivered asynchronously, Kotlin may still lay out children with the old root size for the current frame. The change therefore needs to describe both layers together: cross-platform sync policy parity, and the HarmonyOS white-screen mitigation built on top of that policy.

## What Changes

- Add iOS delegator-driven `syncSendEvent(event)` policy, with default `onBackPressed` synchronous behavior.
- Add explicit `sendWithEvent(..., sync)` plumbing on iOS render view/core so render layers execute sync policy instead of deciding it themselves.
- Add HarmonyOS host/delegator-facing `syncSendEvent(event)` policy in ArkTS and explicit `sendEventSync` plumbing through ArkTS, NAPI, render view, and render core.
- Keep `onBackPressed` as the default synchronous event on all three platforms unless host code overrides the policy.
- Preserve existing asynchronous behavior for ordinary page events when no override is provided.
- On HarmonyOS, change the default `syncSendEvent` policy for `rootViewSizeDidChanged` from SDK-hardcoded sync back to host-controlled behavior.
- On HarmonyOS, move the white-screen mitigation trigger to `onSizeChange` so business code can opt into early synchronous rendering before layout calculation.
- Keep `onAreaChange` as the fallback path on HarmonyOS when business code does not opt into synchronous `rootViewSizeDidChanged` handling.
- Provide a host/delegator override pattern so business code can enable white-screen mitigation by overriding `syncSendEvent(event)` for `rootViewSizeDidChanged`.

## Capabilities

### New Capabilities
- `render-sync-send-event-policy`: Host/delegator-controlled synchronous page-event dispatch across iOS and HarmonyOS, aligned with Android semantics.

### Modified Capabilities
- `render-sync-send-event-policy`: HarmonyOS `rootViewSizeDidChanged` handling now uses the same host-controlled policy shape, and allows business code to opt into early synchronous rendering to avoid white screen during resize/rotation.

## Impact

- Modules impacted:
  - `core-render-ios/`: delegator, render view, render core.
  - `core-render-ohos/`: ArkTS controller, NAPI bridge, render view, render core, type declarations, scheduler/log cleanup.
  - `ohosApp/`: demo delegator override example.
- Affected APIs:
  - iOS adds delegator `syncSendEvent:` and explicit `sendWithEvent:data:sync:` plumbing.
  - HarmonyOS adds ArkTS `sendEventSync(...)` and host `syncSendEvent(event)` override point.
  - HarmonyOS business code can override `syncSendEvent(event)` for `rootViewSizeDidChanged` to enable the anti-white-screen path.
- Affected platforms: iOS and HarmonyOS for parity work; HarmonyOS additionally changes the `rootViewSizeDidChanged` default policy to host-controlled behavior; Android acts as the semantic baseline.
- Breaking behavior note:
  - HarmonyOS no longer enables synchronous `rootViewSizeDidChanged` by default in SDK policy. Business code must explicitly override `syncSendEvent(event)` to turn that optimization on.

## Non-goals

- Do not change Android behavior in this change.
- Do not make all page events synchronous by default.
- Do not move event policy decisions into render core.
- Do not redesign back-press module semantics beyond aligning dispatch timing.
- Do not force HarmonyOS `rootViewSizeDidChanged` to stay synchronous by default inside SDK once host/delegator control exists.
- Do not remove the HarmonyOS fallback path in `onAreaChange`.
