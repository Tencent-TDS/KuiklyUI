## 1. iOS policy alignment

- [x] 1.1 Add `-syncSendEvent:` to `KuiklyRenderViewControllerBaseDelegator`.
- [x] 1.2 Let delegator `sendWithEvent:data:` derive sync policy before forwarding.
- [x] 1.3 Add `sendWithEvent:data:sync:` to `KuiklyRenderView`.
- [x] 1.4 Add `sendWithEvent:data:sync:` to `KuiklyRenderCore`.
- [x] 1.5 Make `onBackPressedWithCompletion:` use the policy-controlled event path.
- [ ] 1.6 Build/verify iOS render event dispatch behavior.

## 2. HarmonyOS host/delegator parity

- [x] 2.1 Add ArkTS host override point `syncSendEvent(event)`.
- [x] 2.2 Add ArkTS explicit dispatch helper `sendEventSync(...)`.
- [x] 2.3 Add NAPI `sendEventSync` bridge.
- [x] 2.4 Add explicit-sync overloads to `IKRRenderView`, `KRRenderView`, and `KRRenderCore`.
- [x] 2.5 Route compatibility helpers through host policy / explicit-sync plumbing.
- [ ] 2.6 Build/verify HarmonyOS render event dispatch behavior.

## 3. HarmonyOS root-size white-screen mitigation

- [x] 3.1 Change HarmonyOS default `syncSendEvent` policy so `rootViewSizeDidChanged` is no longer SDK-hardcoded sync by default.
- [x] 3.2 Make `KRNativeRenderController.ets` trigger early synchronous size handling in `onSizeChange` only when `syncSendEvent(KRRootViewSizeDidChangedEventKey)` returns `true`.
- [x] 3.3 Keep `onAreaChange` as the full fallback path when the HarmonyOS opt-in sync path is disabled.
- [x] 3.4 Provide a business/demo override example for `syncSendEvent(rootViewSizeDidChanged)`.
- [x] 3.5 Remove temporary HarmonyOS diagnostic logs added during investigation.

## 4. Compatibility and review

- [x] 4.1 Preserve existing `sendEvent` / `sendWithEvent:data:` compatibility entry points.
- [x] 4.2 Keep default sync policy limited to `onBackPressed` unless host code opts in for another event.
- [ ] 4.3 Review header / protocol declarations for duplication or style cleanup.
- [ ] 4.4 Run repo-appropriate validation for changed platforms.
- [ ] 4.5 Run `openspec validate --change render-sync-send-event-policy --strict`.
