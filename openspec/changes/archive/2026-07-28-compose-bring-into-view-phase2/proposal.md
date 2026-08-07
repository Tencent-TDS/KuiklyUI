## Why

`compose-ime-insets` 已经补齐了页面级 IME inset 与 `imePadding()`，解决了“键盘占了多少空间、页面如何基础避让”的问题，但还没有解决“当前获焦输入框是否真正看得到”的问题。现在推进 phase2，是为了把 Compose DSL 的输入体验补到和官方 Compose 同一分层：当输入框获焦且被键盘遮挡时，框架自动把目标滚入可视区，业务不再手动监听键盘高度和计算滚动偏移。

## What Changes

- 为 `compose/` 新增 `BringIntoViewRequester` 能力与 `Modifier.bringIntoViewRequester(...)` 注册方式，建立通用的“目标进入可视区”请求通道。
- 为 `compose/` 的焦点链路补齐自动触发逻辑：输入类组件在获得焦点后，若自身处于 IME 遮挡区内，可自动发起 bring-into-view 请求。
- 为 `compose/` 的常见纵向滚动容器补齐响应能力，首版覆盖 `ScrollState` / `verticalScroll` 与 `LazyListState` / `LazyColumn`，在接收到请求后计算目标偏移并执行滚动。
- 建立基于 `FocusedBounds`、`LayoutCoordinates` 与 `WindowInsets.ime` 的可视区判定闭环，在焦点变化、IME 高度变化、容器尺寸变化时重新评估是否需要滚动。
- 新增 demo 验证页面，覆盖长表单、`LazyColumn`、键盘已显示时切换焦点、无遮挡不误滚等典型场景。
- 明确本次 phase2 首版是 **纯 Compose MVP**：不引入 Native `Scroller` bridge`、不实现 `imeNestedScroll`、不处理逐帧键盘动画联动。

## Capabilities

### New Capabilities
- `compose-bring-into-view`: 为 `compose/` 提供通用 bring-into-view 请求能力、输入焦点自动滚入可视区能力，以及 `ScrollState` / `LazyListState` 容器的首版响应能力。

### Modified Capabilities
- None.

## Impact

- **受影响平台**：Android、iOS、HarmonyOS；Web、miniApp、macOS 不在本轮范围内。
- **受影响模块**：`compose/`、`demo/`、`openspec/`。
- **受影响 API**：Compose 侧新增 `BringIntoViewRequester`、`Modifier.bringIntoViewRequester(...)`，并为输入组件补齐默认焦点联动行为。
- **受影响系统**：Compose 焦点系统、几何坐标换算、滚动容器滚动策略，以及 IME 可视区判定链路。
- **外部依赖**：复用现有 `compose-ime-insets` 页面级 IME 状态，不新增 renderer 侧协议作为首版前置条件。

## Non-goals

- 本阶段不实现 `imeNestedScroll`、键盘跟手收起、逐帧 IME animation 或动画曲线精修。
- 本阶段不引入 Native `Scroller` 可视区协议；仅在纯 Compose 容器可闭环的前提下完成首版。
- 本阶段不扩展到横向容器、Grid、Pager、StaggeredGrid、多层嵌套滚动的全量覆盖。
- 本阶段不修改自研 DSL 的输入框自动滚动能力；先以 Compose DSL 对齐官方 Compose 为目标。
- 本阶段不要求业务手动感知 IME 高度、手动计算目标位置或手动触发默认输入焦点滚动。