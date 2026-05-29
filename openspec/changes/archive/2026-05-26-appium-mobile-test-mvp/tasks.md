## 1. 文档与 OpenSpec

- [x] 1.1 将 `docs/DevGuide/ai-mobile-test-*` 迁入 `openspec/changes/appium-mobile-test-mvp/docs/`
- [x] 1.2 将 `docs/plans/2026-05-06-appium-mobile-test-mvp.md` 迁入 `docs/implementation-plan.md`
- [x] 1.3 合并 `appium-mobile-test-ios-back` 为本 change 子 capability
- [x] 1.4 添加 `docs/README.md` 索引

## 2. SDK — iOS render a11y

- [x] 2.1 Card/容器：debugName + 子节点 → 非 a11y leaf（`UIView+CSS*.m`）
- [x] 2.2 testTag 叶子 → `isAccessibilityElement = YES`（`kr_syncAccessibilityElement`）
- [x] 2.3 可点击 accessibilityInfo → 暴露 a11y 元素

## 3. SDK — Android render a11y

- [x] 3.1 RichText `PLAIN_TEXT_FOR_A11Y` + DEBUG_NAME 门控
- [x] 3.2 testTag / contentDescription 映射

## 4. SDK — Compose

- [x] 4.1 `SemanticsNode` deactivate NPE 修复
- [x] 4.2 `KuiklySemantisHandler`：可点击节点下发 accessibilityInfo

## 5. Demo

- [x] 5.1 `NavigationBar` / `ComposeNavigationBar` 加 `testTag("nav_back")`

## 6. Harness — kuikly-mobile-test skill（独立仓库 MR）

- [x] 6.1 MobileDriver + AppiumMobileDriver + HTTP Server
- [x] 6.2 view-tree / snapshot-normalizer / paths
- [x] 6.3 scenarios: ios-e2e, server-e2e, smoke
- [x] 6.4 SKILL / setup / references（setup.md 渐进式披露）
- [x] 6.5 iOS `back()` via nav_back；E2E 用例 8/9 验证 back

## 7. 验证

- [x] 7.1 harness `npm test`
- [x] 7.2 `ios:e2e` 18/18 PASS
- [x] 7.3 `server:e2e` 16/16 PASS
- [x] 7.4 HTTP `POST /back` 子页返回 Root

## 8. 已知排除

- [x] 8.1 ViewDemoPage iOS 崩溃 — 文档标注避开，不修复 BridgeModule
- [x] 8.2 不接入 Appium `mobile:back` / swipe pop 自动化
