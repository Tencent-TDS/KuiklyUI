## Context

Android already implements the intended shape:

- `KuiklyRenderViewBaseDelegator.syncSendEvent(event: String): Boolean` is the host override point.
- Render code consults that policy before deciding whether an event should be dispatched synchronously.
- Default behavior keeps ordinary page events async while making `onBackPressed` synchronous.

Current parity gaps before this change:

- iOS dispatched page events mainly based on thread context, without a delegator-level `syncSendEvent(event)` policy hook.
- iOS `onBackPressedWithCompletion:` still entered the normal event-sending path, but there was no explicit host-controlled sync policy for that event family.
- HarmonyOS already had render-side `syncSendEvent(event_name)` and default `onBackPressed` sync behavior, but the host side could not explicitly override event sync policy with the same ergonomics as Android.

## Goals / Non-Goals

**Goals:**

- Align iOS and HarmonyOS with Android’s policy shape: host/delegator decides, render layers execute.
- Keep `onBackPressed` synchronous by default.
- Keep non-back-press page events asynchronous by default.
- Allow future host code to override sync policy per event without modifying render core.
- Preserve compatibility for existing event dispatch callers.

**Non-Goals:**

- No Android refactor in this change.
- No wholesale conversion of lifecycle events to synchronous delivery.
- No change to unrelated native callback sync semantics.

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

## Validation Plan

- Verify `onBackPressed` still uses synchronous dispatch on iOS and HarmonyOS by default.
- Verify ordinary events remain async by default.
- Verify host override points can force sync dispatch for other events without render-core changes.
- Verify existing non-explicit callers still compile and route through compatibility wrappers.

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
