## Context

本次变更聚焦 `core-render-ios` 与 `core-render-ohos` 的 raw touch 处理链路，目标是统一 `touchDown` / `touchMove` / `touchUp` / `touchCancel` 的消费语义。当前实现中，iOS 的 `KRView.m` 通过是否调用 `super` 决定事件是否继续沿 responder chain 上传；HarmonyOS 的 `KRView.cpp` 通过 `StopPropagation(event)` 或父级 `SuperTouchHandler` 标记决定是否继续向父层传播。两端目前都采用“`handled` 且 `stop-propagation=true` 才截断”的逻辑，这意味着 callback 已被调用但事件仍可能继续上传。

这与业务直觉不一致：业务侧通常把 raw touch callback 的触发理解为“当前 View 已经接管了该事件”。在这种预期下，继续把同一 touch 上传给父级会造成日志顺序混乱、父子重复响应、以及跨端行为不一致。

本次设计适用于 **self-DSL 的 raw touch 事件能力**，以及所有最终落到同一 native renderer touch callback 的调用路径。它不引入新的 Kotlin DSL 接口，也不改变 NativeBridge 数据结构；Kotlin 侧仍然通过现有 `touchDown` / `touchMove` / `touchUp` 注册回调，调整发生在 renderer 对“回调被调用后是否继续传播”的判断上。

### File Changes by Module

- **`core-render-ios`**
  - `core-render-ios/Extension/Components/KRView.m`
  - （如头文件注释需要同步，再评估 `core-render-ios/Extension/Components/KRView.h` 与 `core-render-ios/include/KRView.h`）
- **`core-render-ohos`**
  - `core-render-ohos/src/main/cpp/libohos_render/expand/components/view/KRView.cpp`
- **`demo`**
  - 相关触摸验证页（如 `GestureEventCheckDemo.kt` / `OverNativeClickDemo2.kt`）用于补充回归验证

### NativeBridge Interactions

- 无新增或修改的 NativeBridge 协议。
- 无新增跨平台事件字段。
- 本次仅调整 native renderer 本地分发策略。

## Goals / Non-Goals

**Goals:**
- 统一 iOS / HarmonyOS 对 raw touch 的消费判定：**当前 View 的 touch callback 只要被调用，即视为当前 View 已消费本次 touch action。**
- 保证“当前 View 未消费”时事件仍可继续向父层传播，满足兜底处理需求。
- 将“是否继续上抛”的判断从 `stop-propagation` 开关中解耦出来，使 raw touch 的默认语义只由 `handled` 决定。
- 保持业务侧 API 基本不变，避免这次变更扩大到新的属性设计或跨层接口调整。

**Non-Goals:**
- 不重新定义 `click`、长按、pan、gesture recognizer 的消费模型。
- 不修改 Android / Web / miniApp 的触摸分发实现。
- 不重构 `superTouch` 整体机制，只约束它与 raw touch 的交界处遵守同一消费语义。
- 不在本次设计中新增或强化 `stopPropagation` 的业务能力文档。

## Decisions

### Decision 1: `handled` 成为 raw touch 是否停止上抛的唯一判定条件

- **Decision**: 在 iOS 与 HarmonyOS 中，raw touch callback 一旦被当前 View 调用，当前 action 即标记为 `handled=true`，后续不再继续向父层传播。
- **Rationale**:
  - 业务理解最直接：有 callback 且 callback 被调用，就说明当前节点已经接管事件。
  - 跨端容易对齐：iOS 对应“不再调用 `super`”；HarmonyOS 对应“立即本地截断或向父级 super touch handler 写入 stop 标记”。
  - 避免父子节点重复收同一 action，减少调试成本。
- **Alternatives considered**:
  - **保留 `handled && stop-propagation` 双条件**：兼容性高，但语义分裂，业务仍需记忆额外开关。
  - **完全移除 `handled`，仅靠属性显式控制**：不符合“callback 被调用即消费”的自然认知。

**Implementation Note（实际落地的三条路径）**
- 鸿蒙原生触摸事件**默认就会沿视图树自动冒泡**，renderer 只负责“在合适时机喊停”，从不手动把事件传给父节点。因此三端只需决定“何时 `StopPropagation`”，无需关心“如何上抛”。
- HarmonyOS `ProcessTouchEvent` 依据 `super_touch_type_` 走三条分支：
  - **`NONE`**：当前 View 有 raw touch callback 且被调用（`handled=true`）→ 直接 `kuikly::util::StopPropagation(event)`，立即截断本 action 的继续上抛。
  - **`PARENT`**（处于某个 `superTouch` 祖先内）：`handled=true` 时不再要求 `stop_propagation_` 为真，而是直接 `parent_super_touch_handler_->SetStopPropagation(action, true)`，把“这个 action 要拦”的标记写到父容器共享的 `SuperTouchHandler`；真正的 `StopPropagation` 由父链上的 `SELF` 节点执行。
  - **`SELF`**（`superTouch` 容器自身）：新增 `should_stop = handled`，即**容器自身处理了 callback 也会截断**；若子节点此前写入了 stop 标记则同样置 `should_stop=true` 并清除该标记；最终 `should_stop` 为真时 `StopPropagation(event)`。
- iOS / macOS 在 `KRView.m` 中对应为：`handled` 为真则**不再调用 `super touches...`**（UIKit responder chain 默认也会继续上抛，不调 super 即截断）。

### Decision 2: `stop-propagation` 在 raw touch 链路中降级为兼容字段，不再决定默认消费语义

- **Decision**: 本次不删除 `stop-propagation` 属性入口，但 raw touch 是否继续上抛不再依赖它。
- **Rationale**:
  - 可以避免扩大接口变更范围，减少 Kotlin Attr 与 demo 的同步成本。
  - 兼容已有调用点，避免本次设计文档演变成属性清理专项。
- **Implementation Note**: `stop_propagation_` 成员变量仍保留，`SetProp` / `ResetProp` 依旧会写入和复位它，但在 `ProcessTouchEvent` 的冒泡判定中**已不再被读取**——即它在 raw touch 路径上当前处于 inert（仅占位兼容）状态。后续如需彻底清理，可放到独立 change。
- **Alternatives considered**:
  - **立即删除属性与相关代码**：实现更干净，但会扩大评审范围，也可能影响尚未回收的调用点。
  - **继续保留属性参与 raw touch 判断**：与本次目标冲突。

### Decision 3: iOS 与 HarmonyOS 使用各自原生机制实现相同语义，而不是强行抽象统一代码

- **Decision**:
  - iOS 在 `KRView.m` 中以“是否调用 `super touches...`”体现是否继续冒泡。
  - HarmonyOS 在 `KRView.cpp` 中以 `StopPropagation(event)` / 父级 `SuperTouchHandler` stop 标记体现是否继续冒泡。
- **Rationale**:
  - 两端原生事件系统不同，统一语义比统一实现形式更重要。
  - 能减少改动面，保持当前平台实现结构稳定。
- **Alternatives considered**:
  - **抽象新的跨端触摸消费层**：成本高，超出当前修正范围。

### Decision 4: `superTouch` 相关分支也必须遵守“handled 优先”的新语义

- **Decision**: HarmonyOS 中 `super_touch_type_ == SELF/PARENT/NONE` 的三条分支，都要确保 raw touch callback 一旦被调用，就不会再把同一 action 继续上传给父层。
- **Rationale**:
  - 如果只修普通分支，不修 `superTouch` 相关分支，会出现同一模块内部两套消费语义。
  - 这类不一致最难排查，尤其会出现在复杂嵌套滚动 / 自定义手势场景。
- **Alternatives considered**:
  - **只修 `NONE` 分支**：改动最小，但行为不完整。

## Risks / Trade-offs

- **[Risk] 旧业务可能依赖“子节点 callback 已触发，但父节点仍能收到 raw touch”** → **Mitigation**：在 demo 中补充回归页，并在变更说明中明确这是行为收敛而非兼容延续。
- **[Risk] HarmonyOS `superTouch` 分支存在额外 stop 标记缓存，调整后可能影响 cancel / up 时序** → **Mitigation**：分别验证 down/move/up/cancel 四类 action，并关注 `SetStopPropagation(action, false)` 的复位时机。
- **[Risk] iOS/macOS 共用 `KRView.m`，修改 UIKit 分支后容易遗漏 macOS 分支** → **Mitigation**：同步调整两个编译分支，保持相同判断模式。
- **[Trade-off] 保留 `stop-propagation` 字段但不再决定 raw touch 消费，会留下短期“字段仍在但语义弱化”的状态** → **Mitigation**：在后续独立 change 中再做属性清理或重新定位用途。

## Migration Plan

1. 在 `KRView.m` 中把 raw touch 四个入口统一收敛为“`handled` 为真则不再调 `super`”。
2. 在 `KRView.cpp` 中把普通分支与 `superTouch` 相关分支统一收敛为“`handled` 为真则截断当前 action 的继续上抛”：
   - `NONE` 分支：`handled` 为真直接 `StopPropagation(event)`；
   - `PARENT` 分支：`handled` 为真即向父 `SuperTouchHandler` 写入 stop 标记（不再要求 `stop_propagation_` 为真）；
   - `SELF` 分支：以 `should_stop = handled` 为基础，叠加子节点 stop 标记，为真时 `StopPropagation(event)`。
3. 保留 `stop-propagation` 属性入口，但从 raw touch 默认消费路径中移除其核心判断作用。
4. 更新 demo / 验证页，覆盖以下场景：
   - 子节点无 touch callback，父节点继续收到事件；
   - 子节点有 callback，父节点不再收到同一 action；
   - `superTouch` 参与时仍保持相同消费语义。
5. 若发现线上/业务依赖旧行为，可回滚到“保留双条件判断”的旧实现；回滚不涉及数据迁移或协议回退。

## Open Questions

- `stop-propagation` 在 raw touch 之外是否还承担其他平台特定语义？如果没有，后续可考虑独立 change 清理。
- HarmonyOS 的 `TryFireSuperTouchCancelEvent` 与 `handled` 之间是否还需要补一层明确的 consumed 注释，以降低后续维护成本？
- 是否需要为该行为补一份更显式的开发文档，说明“raw touch callback 被调用即消费”的跨端约定？
