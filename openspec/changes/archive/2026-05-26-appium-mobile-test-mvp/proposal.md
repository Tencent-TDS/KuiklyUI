## Why

KuiklyUI 需要 AI 可驱动的移动端测试闭环：Agent 通过结构化 UI 状态（view-tree）观察页面，用语义 selector 操作 App，并落盘证据。编译/安装/日志由 `kuikly-app-runner` 负责；缺的是跨 iOS/Android 的 UI 自动化层与 SDK 侧 a11y/testTag 暴露。

## What Changes

### SDK（KuiklyUI worktree）

- **core-render-ios / core-render-android**：testTag、a11y 树暴露（Card 子树、RichText 文本、nav_back 叶子节点等）
- **compose/**：`SemanticsNode` 稳定性；Compose 语义 → 原生 a11y 同步
- **demo/**：导航栏 `testTag("nav_back")`；测试页 `debugUIInspector` / testTag

### 工具（kuikly-harness，独立 MR）

- **`kuikly-mobile-test` skill**：MobileDriver + Appium HTTP Server + E2E 场景
- 从 SDK `tools/mobile-test/` 迁出，经 `.claude/skills/` symlink 使用

### 文档

- 原分散在 `docs/DevGuide/ai-mobile-test-*` 与 `docs/plans/` 的文档 **全部迁入** 本 change 的 [`docs/`](docs/README.md)

## Capabilities

### New Capabilities

- `mobile-test-mvp`：Appium 驱动的跨端 UI 自动化（view-tree、selector、断言、HTTP API、E2E）
- `mobile-test-ios-back`：iOS 通过 `nav_back` testTag + render a11y 暴露实现 `back()`

### Modified Capabilities

- （无现有 main spec 修改；SDK 改动为本 MVP 交付的一部分，见 `docs/progress.md`）

## Impact

| 模块 | 变更 |
|------|------|
| `core-render-ios` | a11y identifier、testTag 叶子暴露、Card 容器非 leaf |
| `core-render-android` | RichText a11y text、DEBUG_NAME 门控 |
| `compose/` | Semantics、a11y 同步 |
| `demo/` | nav_back、测试 testTag |
| harness | mobile-test skill（独立仓库） |

## Non-goals

- mock 登录/接口、网络抓包、TAPD 集成（见 `docs/loop.md` P1+）
- 修复 ViewDemoPage iOS BridgeModule 崩溃（文档标注避开）
- Appium `mobile:back` / iOS 左滑 pop 自动化
- 将 skill 代码合入 KuiklyUI SDK 主仓

## Reference

详细设计、计划、踩坑与进展见 [docs/README.md](docs/README.md)。
