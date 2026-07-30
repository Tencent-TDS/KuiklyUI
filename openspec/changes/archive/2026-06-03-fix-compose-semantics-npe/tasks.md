## 1. NodeChain head pointer fix

- [x] 1.1 Modify `NodeChain.removeNode()` to sync `head` property when removed node is the current head
- [x] 1.2 Ensure `head` is reassigned to `child ?: tail` after disconnection

## 2. SemanticsNode null-safety refactor

- [x] 2.1 Change `SemanticsNode(layoutNode, mergingEnabled)` factory return type to nullable (`SemanticsNode?`)
- [x] 2.2 Update `SemanticsOwner.unmergedRootSemanticsNode` to handle nullable return safely
- [x] 2.3 Update `fillOneLayerOfSemanticsWrappers` to use `head()` instead of `has()` for semantics presence check
- [x] 2.4 Ensure all `SemanticsNode()` call sites in `compose/ui/semantics/` handle nullable results

## 3. Cleanup and verification

- [x] 3.1 Remove temporary diagnostic KLog statements added during bug investigation (from `SemanticsNode.kt`, `LayoutNode.kt`, `NodeChain.kt`, `KuiklySemantisHandler.kt`)
- [x] 3.2 Compile `compose` module with `./gradlew :compose:compileDebugKotlinAndroid`
- [ ] 3.3 Run Android demo to verify no regression in semantics tree behavior

## 4. Local Maven publish for integration test

- [x] 4.1 Run `./gradlew :core:publishToMavenLocal :compose:publishToMavenLocal`
- [ ] 4.2 Verify business project can reproduce the crash with the local SNAPSHOT and confirm fix
