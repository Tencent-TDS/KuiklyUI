## Why

当前 Compose IME 链路已经在 `compose-ime-insets` 的 page-level IME 基座之上，继续叠加了多轮超出 phase1 范围的建设：`compose-ime-animation-polish`、`compose-ime-linear-animation-mode`、`native-ime-animation-progress`、`ios-ime-sync-send-event`、`compose-ime-sync-curve-animation`。这些 change 分别引入了动画投影、`native-progress`、`source / animatedHeight` 协议扩展、以及 IME 事件同步发送特例，使当前实现语义明显偏离了最初约定的 phase1 边界。

现在需要新增一份“回退到 phase1”草案，明确把 IME 能力重新收敛到 `compose-ime-insets` 的原始目标：**保留页面级 IME inset 基座与声明式 API，移除所有 phase1 之外的动画治理与同步派发依赖**。这份 change 的目的不是重新设计 phase2/phase3，而是先让现有实现重新符合已确认的 phase1 合约，降低复杂度、减少跨端分叉，并为后续是否重启动画优化留出干净基线。

## What Changes

- 将 Compose IME 消费链路回退到 phase1 语义：`WindowInsets.ime` 直接投影页面级 IME 当前高度，不再由框架内部执行目标值补间、非线性 easing 或 `native-progress` 优先消费。
- 将 IME page event 协议收敛回 phase1 范围：保留 `height`、`duration`、`curve`，撤销 `source`、`animatedHeight` 及其相关常量、兼容分支与调试语义。
- 撤销三端 renderer 和宿主层对 `imeInsetsDidChanged` 的同步发送特例，使 phase1 基线重新建立在普通 page event 派发语义之上，而不是依赖 sync-send policy。
- 保留 `WindowInsets.ime`、`Modifier.imePadding()`、`Scaffold` 默认内容避让，以及旧 `keyboardHeightChange` callback 的兼容性，不回退 phase1 已建立的用户可见能力。
- 回滚 demo 页面中的 phase3 文案、调试字段和“sync curve / native-progress / phase3 提前映射”说明，使 demo 重新只验证 phase1 场景。
- 在 OpenSpec 层面明确：后续 IME 动画治理 change 视为**超出本次基线范围**，在真正实施代码回滚时需要被标记为 superseded、withdrawn 或至少不再作为当前落地目标继续推进。

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `compose-ime-insets`: 收紧到原始 phase1 合约，明确页面级 IME 状态只承诺基础避让，不承诺内部动画投影、逐帧进度驱动或同步事件派发。
- `render-sync-send-event-policy`: 明确 `compose-ime-insets` phase1 基线不依赖 `imeInsetsDidChanged` 的同步发送特例；是否支持 sync-send 仍可作为独立机制存在，但不再属于 phase1 的必需条件。

## Impact

- **受影响平台**：Android、iOS、HarmonyOS；Web、miniApp、macOS 不在本轮范围内。
- **受影响模块**：`core/`、`compose/`、`core-render-android/`、`core-render-ios/`、`core-render-ohos/`、`iosApp/`、`ohosApp/`、`demo/`、`openspec/`。
- **受影响 API / 协议**：业务侧公开 API 保持 `WindowInsets.ime`、`Modifier.imePadding()`、`Scaffold` 不变；内部 page event 协议从扩展态回退到 phase1 payload 语义。
- **受影响 change 范围**：本草案会与 `compose-ime-animation-polish`、`compose-ime-linear-animation-mode`、`native-ime-animation-progress`、`ios-ime-sync-send-event`、`compose-ime-sync-curve-animation` 形成显式 scope 冲突，需要在真正实施时决定归档或撤销策略。

## Non-goals

- 不在本草案中重新定义新的 phase2 / phase3 路线图。
- 不在本草案中实现新的动画能力、逐帧进度能力或更高精度的键盘联动。
- 不移除 phase1 已公开的业务入口：`WindowInsets.ime`、`Modifier.imePadding()`、`Scaffold` 默认 IME 避让。
- 不修改自研 DSL、Web、miniApp、macOS 的键盘能力边界。
- 不在本草案中直接实施代码回滚；本 change 只负责把回滚目标、边界与验收口径定义清楚。
