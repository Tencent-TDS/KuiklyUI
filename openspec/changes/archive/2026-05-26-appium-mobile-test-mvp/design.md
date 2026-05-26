## Context

AI 移动端测试采用「observe → think → act」：HTTP Server / MobileDriver 获取 view-tree，用 testTag/text selector 操作，断言后落盘证据。编译部署依赖 `kuikly-app-runner`；自动化后端选用 Appium（见 `docs/framework-decision.md`）。

工具实现位于 **kuikly-harness**（`kuikly-mobile-test` skill），SDK 侧只保留 a11y/testTag render 修复。

## Goals / Non-Goals

**Goals:**

- Android + iOS 模拟器上跑通 E2E（tap、input、scroll、assert、back）
- view-tree 含 Kuikly 类名（debugUIInspector）与 testTag
- 证据写入 checkout `./logs/`
- iOS `back()` 走 `nav_back` → `RouterModule.closePage()`

**Non-goals:** 见 `proposal.md`；P1 mock/网络/平台集成见 `docs/loop.md`。

## Architecture

```text
AI / scenarios / curl
  ↓
HTTP Server (MobileTestServer)  或  scenarios/*.ts
  ↓
MobileDriver
  ↓
AppiumMobileDriver → Appium → UiAutomator2 / XCUITest
  ↓
KuiklyUI App (demo)

编译/安装/日志: kuikly-app-runner（独立 skill）
```

## Key Decisions

### 1. Appium 作为 P0 后端

跨端统一 WebDriver API；底层仍用 UiAutomator2/XCUITest。详见 `docs/framework-decision.md`。

### 2. 工具不随 SDK 发布

`tools/mobile-test` 迁入 **kuikly-harness**，KuiklyUI 只保留 render/demo 改动与本 OpenSpec 文档。

### 3. Selector 统一

view-tree 的 `testTag` 对应 HTTP `accessibilityId` 或 `testTag`（等价）。iOS 对 Card/Button 用 `identifier CONTAINS` + xpath `@name` fallback。

### 4. iOS 返回

- **禁止** Appium 系统 back（UIKit 裸 pop，曾崩溃）
- **实现** tap `nav_back` testTag
- **Render 修复**（2026-05-26）：叶子节点设 `testTag` 时 `isAccessibilityElement = YES`（`UIView+CSSDebug kr_syncAccessibilityElement`）

### 5. 证据落盘

`paths.ts` 向上找 `settings.gradle.kts`（含 worktree），日志 → `<checkout>/logs/`。

## SDK Render 要点

| 问题 | 修复位置 | 说明 |
|------|----------|------|
| iOS Card 挡住子树 | `UIView+CSS*.m` | debugName + 有子节点 → 非 a11y leaf |
| Android RichText 无 text | `KRRichTextView` + delegate | DEBUG_NAME 门控下写 `info.text` |
| iOS nav_back 不可见 | `UIView+CSSDebug.m` | testTag 叶子 → a11y 元素 |
| Compose 可点击无 a11yInfo | `KuiklySemantisHandler.kt` | clickable 时仍下发 accessibilityInfo |

## Verification

| 套件 | 结果（2026-05-26） |
|------|-------------------|
| harness `npm test` | 单元测试 PASS |
| harness `ios:e2e` | 18/18 |
| harness `server:e2e` | 16/16 |
| HTTP `POST /back` | TextViewDemo 子页返回 Root OK |

## Reference Docs

全部在 [`docs/`](docs/README.md)：`loop.md`、`implementation-plan.md`、`ios-e2e-pitfalls.md`、`progress.md` 等。
