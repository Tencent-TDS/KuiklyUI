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

## 3. Compatibility and review

- [x] 3.1 Preserve existing `sendEvent` / `sendWithEvent:data:` compatibility entry points.
- [x] 3.2 Keep default sync policy limited to `onBackPressed`.
- [ ] 3.3 Review header / protocol declarations for duplication or style cleanup.
- [ ] 3.4 Run repo-appropriate validation for changed platforms.
- [ ] 3.5 Run `openspec validate --change render-sync-send-event-policy --strict`.
