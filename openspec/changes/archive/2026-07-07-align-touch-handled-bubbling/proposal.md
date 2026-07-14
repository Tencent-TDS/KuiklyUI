## Why

当前 `KRView.m` 与 `KRView.cpp` 对 raw touch 的“已处理后是否继续向父层上抛”判断语义并不一致，且都把 `stop-propagation` 作为额外开关叠加在 `handled` 判断之后，导致事件是否继续上传不是由“当前 View 是否已消费”直接决定。对于业务侧来说，这会让 iOS 与 HarmonyOS 的 `touchDown` / `touchMove` / `touchUp` 行为难以预测，也让调试日志中的父子响应顺序与消费语义不稳定。

现在需要把这条语义收敛为更直观的规则：**当前 View 的 raw touch callback 只要被调用，就视为当前 View 已消费该 touch；只有当前 View 未消费时，事件才允许继续向父层上抛。**

## What Changes

- 统一 iOS 与 HarmonyOS raw touch 的消费语义：`touchDown` / `touchMove` / `touchUp` / `touchCancel` 回调只要被当前 View 触发，即视为已消费。
- 调整 `core-render-ios` 中 `KRView.m` 的冒泡判断，使是否调用 `super` 仅由“当前事件是否已被当前 View 处理”决定，而不再额外依赖 `stop-propagation`。
- 调整 `core-render-ohos` 中 `KRView.cpp` 的 `ProcessTouchEvent` 判断，使是否调用 `StopPropagation(event)` 或向父级 super touch 传递 stop 标记仅由 `handled` 决定，而不再额外依赖 `stop_propagation_`。
- 明确本次变更只收敛 raw touch 语义，不扩展新的触摸属性设计；`stop-propagation` 可保留兼容入口，但不再作为这次行为设计的核心控制条件。
- 补充 demo / 测试用例，覆盖“当前节点无 touch callback 时继续上抛”和“当前节点 callback 被调用后父节点不再收到 raw touch”两类场景。

## Capabilities

### New Capabilities
- `touch-handled-bubbling`: 定义 iOS / HarmonyOS renderer 在 raw touch 回调被当前节点处理后如何停止继续向父层传播的统一行为。

### Modified Capabilities
- None.

## Impact

- **Affected platforms**: iOS, HarmonyOS（macOS 跟随 `KRView.m` 逻辑一并对齐）；Android / Web / miniApp 不在本次范围内。
- **Impacted modules**: `core-render-ios`, `core-render-ohos`, `demo`。
- **Affected code**: `core-render-ios/Extension/Components/KRView.m`, `core-render-ohos/src/main/cpp/libohos_render/expand/components/view/KRView.cpp`, 相关 demo 页面与验证日志。
- **API / behavior impact**: raw touch 的默认消费语义会更严格；此前依赖“callback 已触发但仍继续上传给父节点”的页面行为将被修正。

## Non-goals

- 不新增跨平台触摸属性，也不扩大 `stop-propagation` 的平台能力设计。
- 不修改 Android / Web / miniApp 的触摸事件分发模型。
- 不重构 `click`、手势识别器、`superTouch` 的整体架构；仅在本次设计中约束其与 raw touch 的边界语义。
