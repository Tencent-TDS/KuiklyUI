## Why

Kuikly Compose DSL 目前已经具备 `WindowInsets` 的消费骨架和 `Scaffold` 的内容 inset 语义，但仍缺少页面级 IME 状态通道，导致业务只能手动监听键盘高度并自行调整布局。现在推进 Phase 1，是为了先补齐统一的 `ime insets` 底座，让常见输入页能够以声明式方式完成键盘规避，而不是继续扩散业务层的临时方案。

## What Changes

- 为 Compose DSL 新增页面级 IME inset 能力，支持将键盘底部占用空间统一注入到 `WindowInsets` 体系。
- 为 Compose DSL 新增面向业务的基础 API，包括 `WindowInsets.ime` 与 `Modifier.imePadding()`。
- 调整 `Scaffold` 默认内容 inset 的接入方式，使其具备组合系统栏与 IME inset 的能力。
- 在 Android、iOS、HarmonyOS renderer 中补齐页面级键盘高度状态接入，避免继续依赖输入组件局部事件。
- 增加 demo 验证页面，覆盖底部输入栏和普通表单页两类 Phase 1 目标场景。

## Capabilities

### New Capabilities
- `compose-ime-insets`: 为 `compose/` 提供页面级 IME inset 状态、声明式 `imePadding` 接口，以及 `Scaffold` 对键盘避让的基础支持。

### Modified Capabilities
- None.

## Impact

- **受影响平台**：Android、iOS、HarmonyOS；Web、miniApp、macOS 本阶段不纳入实现范围。
- **受影响模块**：`compose/`、`core-render-android/`、`core-render-ios/`、`core-render-ohos/`、`demo/`。
- **受影响 API**：Compose 侧新增 `WindowInsets.ime`、`Modifier.imePadding()`，并调整 `Scaffold` 的默认内容 inset 组合语义。
- **受影响系统**：页面级键盘状态分发链路、Compose 窗口配置状态、Material3 容器默认避让行为。

## Non-goals

- 本阶段不实现 `BringIntoView`、聚焦自动滚动、原生 `Scroller` 的可见区域滚动协议。
- 本阶段不实现官方 Compose `imeNestedScroll` 或系统级逐帧 IME 动画联动。
- 本阶段不追求 Web、miniApp、macOS 的等价键盘规避能力。
