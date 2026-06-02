## Context

Android already implements the intended shape:

- `KuiklyRenderViewBaseDelegator.syncSendEvent(event: String): Boolean` is the host override point.
- Render code consults that policy before deciding whether an event should be dispatched synchronously.
- Default behavior keeps ordinary page events async while making `onBackPressed` synchronous.

Current parity gaps before this change:

- iOS dispatched page events mainly based on thread context, without a delegator-level `syncSendEvent(event)` policy hook.
- iOS `onBackPressedWithCompletion:` still entered the normal event-sending path, but there was no explicit host-controlled sync policy for that event family.
- HarmonyOS initially had render-side `syncSendEvent(event_name)` and default `onBackPressed` sync behavior, but the host side could not explicitly override event sync policy with the same ergonomics as Android.

After the first parity pass, HarmonyOS also exposed a platform-specific white-screen issue during rotation and resize:

- `rootViewSizeDidChanged` may arrive too late if it stays on the ordinary async path.
- Delivering that event after layout has already used the old root size can leave one frame where child views still use stale layout, causing a white flash.
- HarmonyOS therefore needs both a cross-platform host-controlled sync policy and an earlier platform-specific trigger point for the opt-in white-screen mitigation path.

The combined change is therefore:

- Cross-platform policy alignment: host/delegator decides, render layers execute.
- HarmonyOS anti-white-screen extension: business code may opt into synchronous `rootViewSizeDidChanged` handling before layout calculation.

### Platform white-screen mitigation model

| Platform | Trigger point | Why it can affect the same frame | Sync mechanism |
| --- | --- | --- | --- |
| Android | `onSizeChanged` during measure/layout | `remeasureIfNeeded()` forces child re-measure in the same pass | `ConditionVariable` blocks the main thread until the context thread finishes |
| iOS | `setFrame:` while frame updates are being staged | CATransaction commits frame changes together in the same run loop | `dispatch_sync(contextQueue)` blocks the main thread |
| HarmonyOS | `onSizeChange` before layout calculation completes | C-API marks node attributes dirty, then the following layout stage consumes the new values | `DirectRunOnCurThread` executes synchronously on the main thread |

## Goals / Non-Goals

**Goals:**

- Align iOS and HarmonyOS with Android’s policy shape: host/delegator decides, render layers execute.
- Keep `onBackPressed` synchronous by default.
- Keep non-back-press page events asynchronous by default.
- Allow future host code to override sync policy per event without modifying render core.
- Preserve compatibility for existing event dispatch callers.
- On HarmonyOS, make `rootViewSizeDidChanged` opt-in at the host/delegator layer instead of SDK-hardcoded by default.
- On HarmonyOS, trigger the opt-in white-screen mitigation path in `onSizeChange`, before the current layout calculation uses stale size.
- Keep `onAreaChange` as the fallback path when HarmonyOS business code does not opt into synchronous size-change handling.
- Clean up temporary diagnostic logging added during investigation.

**Non-Goals:**

- No Android refactor in this change.
- No wholesale conversion of lifecycle events to synchronous delivery.
- No change to unrelated native callback sync semantics.
- No forced default sync behavior for HarmonyOS `rootViewSizeDidChanged` once host-controlled policy exists.
- No removal of the HarmonyOS fallback path in `onAreaChange`.
- No redesign of DSL mode boundaries; the same renderer behavior applies to self-DSL and Compose DSL.

## Decisions

### Decision 1: Put policy at the host/delegator layer

The sync/async decision belongs to host-facing delegator/controller code, not render core.

Rationale:

- Matches Android semantics.
- Keeps render core policy-free and easier to reason about.
- Lets business code override behavior without forking render internals.

### Decision 2: Keep render/core responsible only for execution

iOS and HarmonyOS render layers receive an explicit `sync` boolean when the host has already decided the policy.

Rationale:

- Prevents render code from hardcoding business decisions.
- Makes execution path explicit and testable.
- Preserves default path helpers for existing callers.

### Decision 3: Default only `onBackPressed` to sync

If host code does not override policy, only `onBackPressed` is synchronous.

Rationale:

- Matches Android baseline.
- Minimizes regression risk for ordinary events.
- Preserves existing async event throughput for lifecycle-style traffic.

### Decision 4: Preserve compatibility wrappers

Existing `sendEvent` / `sendWithEvent:data:` entry points remain available and delegate to the new explicit-sync path.

Rationale:

- Avoids broad caller churn.
- Enables incremental adoption.

### Decision 5: HarmonyOS `rootViewSizeDidChanged` becomes host-controlled, not SDK-hardcoded

HarmonyOS host code must explicitly opt in if `rootViewSizeDidChanged` should be handled synchronously.

Rationale:

- Aligns HarmonyOS with the same host/delegator policy shape used by Android and iOS.
- Avoids making the anti-white-screen path an unconditional SDK behavior change for every existing business page.
- Keeps the original async path for business code that does not know about, or does not need, the optimization.

### Decision 6: Trigger the HarmonyOS anti-white-screen path in `onSizeChange`

When business code returns `true` from `syncSendEvent(rootViewSizeDidChanged)`, HarmonyOS performs the size-change handling early in `onSizeChange` rather than waiting for `onAreaChange`.

Rationale:

- `onAreaChange` may fire after the frame has already laid out children using the old root size.
- `onSizeChange` runs early enough for synchronous Kotlin layout and C-API dirty marking to affect the current frame.
- One policy hook controls both “should this event be synchronous?” and “should HarmonyOS take the early trigger path?”.

### Decision 7: Keep `onAreaChange` as a full fallback path on HarmonyOS

`onAreaChange` continues to detect and process unhandled size changes.

Rationale:

- Preserves correctness if business code leaves `rootViewSizeDidChanged` on the original async path.
- Protects against edge cases where `onSizeChange` does not execute or does not fully cover the transition.
- Avoids introducing a second business-facing configuration switch.

### Decision 8: Keep the C++ HarmonyOS fallback sync policy available

`KRRenderView::syncSendEvent` may still retain `rootViewSizeDidChanged` as a fallback sync rule even though ArkTS now owns the business-facing decision.

Rationale:

- The business-facing policy is decided in ArkTS and forwarded through explicit sync plumbing.
- Keeping the render-side fallback does not interfere with the explicit ArkTS path.
- It still serves as a protective fallback when native code reaches render-side helpers without the explicit ArkTS override path.

## Target State

### iOS

- `KuiklyRenderViewControllerBaseDelegator` exposes `-syncSendEvent:`.
- Delegator `sendWithEvent:data:` computes `sync` from host policy and forwards `sendWithEvent:data:sync:`.
- `KuiklyRenderView` and `KuiklyRenderCore` accept explicit `sync`.
- `KuiklyRenderCore` uses the explicit sync flag to drive synchronous context execution and UI flush when required.
- `onBackPressedWithCompletion:` goes through the same policy-controlled event path.

### HarmonyOS

- ArkTS controller exposes `syncSendEvent(event: string): boolean` for host override.
- ArkTS controller offers `sendEventSync(...)` / explicit-sync dispatch plumbing.
- NAPI exposes `sendEventSync`.
- `KRRenderView` and `KRRenderCore` accept explicit sync overrides while keeping compatibility helpers.
- Default host policy returns `true` only for `onBackPressed`.
- If business code overrides `syncSendEvent(rootViewSizeDidChanged)` to return `true`, `onSizeChange` performs early synchronous size-change handling to avoid white screen.
- If business code does not opt in, `onAreaChange` keeps the original async size-change path.
- Demo/business-facing ArkTS code can override `syncSendEvent(event)` to show how to enable the HarmonyOS white-screen mitigation.

## Validation Plan

- Verify `onBackPressed` still uses synchronous dispatch on iOS and HarmonyOS by default.
- Verify ordinary events remain async by default.
- Verify host override points can force sync dispatch for other events without render-core changes.
- Verify existing non-explicit callers still compile and route through compatibility wrappers.
- Verify HarmonyOS `rootViewSizeDidChanged` remains async by default when host code does not override policy.
- Verify HarmonyOS white-screen mitigation works only when business code opts in through `syncSendEvent(rootViewSizeDidChanged)`.
- Verify HarmonyOS `onAreaChange` still handles size changes correctly when the opt-in path is disabled.
- Verify temporary `[SyncDiag]` investigation logs are removed.

## File Changes

### iOS

- `core-render-ios/Extension/KuiklyRenderViewControllerBaseDelegator.h`
- `core-render-ios/Extension/KuiklyRenderViewControllerBaseDelegator.m`
- `core-render-ios/View/KuiklyRenderView.h`
- `core-render-ios/View/KuiklyRenderView.m`
- `core-render-ios/Core/KuiklyRenderCore.h`
- `core-render-ios/Core/KuiklyRenderCore.m`

### HarmonyOS

- `core-render-ohos/src/main/ets/IKuiklyRenderView.ets`
- `core-render-ohos/src/main/ets/KRNativeRenderController.ets`
- `core-render-ohos/src/main/cpp/types/index.d.ts`
- `core-render-ohos/src/main/cpp/napi_init.cpp`
- `core-render-ohos/src/main/cpp/libohos_render/view/IKRRenderView.h`
- `core-render-ohos/src/main/cpp/libohos_render/view/KRRenderView.h`
- `core-render-ohos/src/main/cpp/libohos_render/view/KRRenderView.cpp`
- `core-render-ohos/src/main/cpp/libohos_render/core/KRRenderCore.h`
- `core-render-ohos/src/main/cpp/libohos_render/core/KRRenderCore.cpp`
- `core-render-ohos/src/main/cpp/libohos_render/scheduler/KRContextScheduler.cpp`
- `core-render-ohos/src/main/cpp/libohos_render/scheduler/KRUIScheduler.cpp`
- `core-render-ohos/src/main/cpp/libohos_render/foundation/thread/KRThread.h`

### Demo / host example

- `ohosApp/entry/src/main/ets/kuikly/KuiklyViewDelegate.ets`
