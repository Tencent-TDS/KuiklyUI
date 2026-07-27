## Context

Kuikly 当前的富文本交互能力属于 **self-DSL** 范畴：业务通过 `core` 中的 `RichTextView`、`TextSpan`、`ImageSpan` 声明富文本节点，渲染侧再由各平台 renderer 负责命中测试与事件回传。现状中，`Span.click` 已有一条定制的分发链路，但 `longPress` 仅支持 `RichText` 组件级事件，不支持 span 级命中与回调。

当前已确认的实现特点如下：

- `core` 中 `RichTextView` / `RichTextEvent` / `ISpan` 只为 span click 定制了分发抽象，没有 `Span.longPress` 对应的 DSL 和拦截入口。
- `core` 中基础事件系统已经有 `EventName.LONG_PRESS` 与 `LongPressParams`，因此组件级长按无需新增事件类型。
- Android 与 iOS 渲染层都已有 span click 命中能力：在点击时根据坐标换算字符 offset，再回传 span index。
- HarmonyOS 已存在明确的富文本实现入口，例如 `core-render-ohos/.../richtext/KRRichTextView.cpp` 与 `KRRichTextShadow.cpp`，因此本次变更将其纳入首版实现范围。
- `LongPressParams` 当前缺少类似 `ClickParams.params` 的原始透传字段，公共层无法取得渲染侧附加的 span index。

因此，这次变更不是简单增加一个 DSL 方法，而是一个横跨 `core`、`core-render-android`、`core-render-ios`、`core-render-ohos` 的事件模型扩展。

**DSL 适用范围**：本次仅适用于 Kuikly **self-DSL** 富文本能力，不涉及 Compose DSL 的 `Text` / `AnnotatedString` API。

**NativeBridge 交互**：本次不新增独立的 Native Module 或新的桥接通道，而是复用现有视图事件回调链路。各 renderer 需要在派发 `LONG_PRESS` 事件时，将 span index 注入原始事件 payload；`core` 再在 `LongPressParams` 中保留该原始参数，并由 `RichTextEvent` 的 span 拦截逻辑消费。

## Goals / Non-Goals

**Goals:**

- 为 `TextSpan` 提供 `longPress` DSL，使业务可为单个富文本 span 注册长按回调。
- 为富文本长按建立与现有 span click 对齐的公共层分发机制。
- 为 `LongPressParams` 增加原始参数透传能力，以承载 span index 等平台侧命中结果。
- 在 Android、iOS、HarmonyOS 三端补齐 span 长按命中与回传，优先复用 click 的既有命中算法。
- 明确长按语义：`start` 阶段命中并锁定首个 span，后续 `move/end/cancel` 都派发给该锁定目标，避免移动过程中目标抖动。
- 保持 `RichText.longPress` 兼容：未命中 span 时仍走组件级长按。

**Non-Goals:**

- 不重构整个 `RichText` 事件体系为通用手势框架。
- 不在本次变更中引入文本选择、复制菜单、上下文菜单等额外交互。
- 不要求首版同时交付 Web、miniApp、macOS 的 span 长按渲染支持。
- 不修改 Compose DSL API，也不让 `compose/` 依赖任何 renderer 模块。
- 不改变现有 span click 的行为和协议。

## Decisions

### D1. 公共层沿用 click 的定制分发模式，平行增加 span longPress 能力

**决策**：在 `core` 的 `RichTextView` 中为 `TextSpan` 增加 `longPress(handler: (LongPressParams) -> Unit)`，并在 `ISpan` / `RichTextAttr` / `RichTextEvent` 中平行补齐长按的注册、拦截和分发入口，而不是趁机重写为全新的手势抽象。

**原因**：

- 现有 span click 已有稳定链路，平行扩展风险最小。
- 业务想要的是 `Span.longPress` 能力，而不是一套新的手势框架。
- 与 click 结构保持镜像，有助于跨平台 renderer 直接复用现有思路。

**替代方案**：

- **统一改造成手势枚举驱动的 span 事件模型**：长期抽象更干净，但改造面过大，且会触碰现有 click 逻辑，不适合作为首版。

### D2. `LongPressParams` 增加 `params: Any?` 原始透传字段，而不是直接增加 `index`

**决策**：修改 `core` 中的 `LongPressParams`，增加与 `ClickParams` 对齐的原始透传字段 `params: Any?`，在 decode 时保留 renderer 回传的原始 payload。

**原因**：

- span index 只是当前首个使用方，直接增加 `index` 会把富文本命中概念泄漏到所有长按事件。
- 保留原始 payload 后，公共层可以在富文本中读取 `index`，未来若有其他长按附加信息也可继续复用。
- 该方案与 click 参数模型一致，认知成本最低。

**替代方案**：

- **给 `LongPressParams` 增加显式 `index` 字段**：实现简单，但模型污染更重，也不利于未来扩展。

### D3. span longPress 在 `start` 阶段锁定首个命中的 `activeSpanIndex`

**决策**：当 `RichText` 收到 `longPress state=start` 时，先做一次命中判断：

- 若命中某个 span，则将其记录为 `activeSpanIndex`，并将当前事件派发给该 span；
- 若未命中 span，则走 `RichText.longPress` 组件级事件。

在同一轮长按手势中，`move/end/cancel` 不重新 hit-test，而是直接派发给 `activeSpanIndex` 对应的 span；在 `end/cancel` 后清空该状态。

**原因**：

- 长按目标应在手势开始时确定，避免手指轻微移动导致 span 来回切换。
- Android / iOS / HarmonyOS 更容易对齐这一语义。
- 业务对长按生命周期的理解更稳定，状态更容易维护。

**替代方案**：

- **只给 `start` 派发 span，后续状态回到 `RichText`**：实现更小，但生命周期语义不完整。
- **每次 `move` 重新命中并切换 span**：会造成抖动与跨端不一致，不采用。

### D4. span longPress 优先于 `RichText.longPress`

**决策**：若 `start` 阶段命中 span，则整轮长按手势优先派发给该 span，不再同时向 `RichText` 本身派发重复的长按事件；只有未命中 span 时，才触发 `RichText.longPress`。

**原因**：

- 保持目标唯一，避免业务同时收到 span 级和组件级重复回调。
- 与 span click 的“命中则消费”思路一致。

**替代方案**：

- **span 与 RichText 同时回调**：会提高业务冲突概率，也让事件优先级不可预测。

### D5. Android / iOS 复用现有 span click 命中算法

**决策**：

- Android 继续使用当前点击链路里的“坐标 → 字符 offset → span index”算法，并在长按回调构造参数时填入 index。
- iOS 继续使用 `characterIndexForPoint` + 富文本属性中的 index 进行命中。

**原因**：

- click 链路已经证明这些命中方式可用。
- 复用现有实现能减少每端新增逻辑的不确定性。

**替代方案**：

- **重新发明新的 span 命中算法**：没有必要，且会增加各端行为差异。

### D6. HarmonyOS 纳入首版实现，并复用现有富文本组件入口

**决策**：在 spec 中要求 Android、iOS、HarmonyOS 三端首版行为一致；HarmonyOS 侧以现有 `KRRichTextView.cpp` 与 `KRRichTextShadow.cpp` 为实现入口，补齐 span 长按命中、index 回传和与点击一致的 span 标识映射。

**原因**：

- 当前仓库里已经存在明确的 HarmonyOS 富文本组件与 shadow 层实现，不是从零起步。
- 用户明确要求鸿蒙也要支持，本次 change 需要反映真实交付范围。
- 将 HarmonyOS 一并纳入首版，可以保证 `Span.longPress` 在 Kuikly 主要渲染端上的能力一致性。

**替代方案**：

- **仅在文档里预留 HarmonyOS 扩展点，延后实现**：与本次需求不符，也会继续造成平台能力割裂。

## Risks / Trade-offs

- **[Risk] `LongPressParams` 是基础事件模型，改动后需要兼容所有现有长按调用方** → **Mitigation**：只新增可选 `params` 字段，不修改现有字段语义，确保旧逻辑不受影响。
- **[Risk] span 与 `RichText` 长按优先级处理不当会引发重复回调** → **Mitigation**：在 spec 中明确“span 优先、未命中才回退到 `RichText`”并补回归用例。
- **[Risk] `activeSpanIndex` 生命周期管理错误会导致 `move/end` 丢失或串台** → **Mitigation**：将其生命周期限定在单轮长按手势内，并在 `end/cancel` 后统一清空。
- **[Risk] HarmonyOS 富文本命中链路与其他平台实现细节不同，可能出现 index 映射偏差** → **Mitigation**：明确要求 `KRRichTextView.cpp` 与 `KRRichTextShadow.cpp` 共享与点击一致的 span 标识，并补充端上回归验证。

## Migration Plan

- **发布方式**：新增非破坏性能力，无需迁移脚本或配置开关；随 `core` 与对应 renderer 正常发布。
- **回滚方式**：若后续需要回滚，可直接回退本次 change 对 `Span.longPress`、`LongPressParams.params` 与 renderer 命中扩展的修改，不影响现有 span click 与 `RichText.longPress`。
- **验证策略**：
  1. `core` 单元测试覆盖 span 长按分发、未命中回退、`activeSpanIndex` 锁定与清理。
  2. Android/iOS/HarmonyOS 分别验证命中 span 时回调 index 正确，未命中时回退到组件级长按。
  3. 验证 span click 与 span longPress 共存时互不影响。

## Open Questions

- `ImageSpan` 是否在首版与 `TextSpan` 一起开放 `longPress`，还是仅在内部接口上预留能力？
- demo 是否需要新增独立示例页，还是在已有富文本示例中补充长按验证场景即可？
- HarmonyOS 侧 span 命中结果最终由 `view` 层直接回传，还是需要 `shadow` 层额外承担索引映射缓存职责？

## 受影响文件（按模块分组）

- **core**
  - `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/RichTextView.kt`
    - 增加 `TextSpan.longPress` 与相关 span 长按分发逻辑。
    - 为 `ISpan`、`RichTextAttr`、`RichTextEvent` 平行补齐长按注册与拦截入口。
    - 管理 `activeSpanIndex` 的锁定与清理。
  - `core/src/commonMain/kotlin/com/tencent/kuikly/core/base/event/EventParams.kt`
    - 为 `LongPressParams` 增加 `params` 原始透传字段。

- **core-render-android**
  - `core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/component/KRRichTextView.kt`
    - 复用点击命中算法，补齐长按 span index 回传。

- **core-render-ios**
  - `core-render-ios/Extension/AdvancedComps/KRRichTextView.m`
    - 复用字符命中逻辑，补齐长按 span index 回传。

- **core-render-ohos**
  - `core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.cpp`
    - 复用现有富文本命中基础，补齐长按 span index 回传。
  - `core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextShadow.cpp`
    - 校验或补齐与点击一致的 span 索引映射和数据透传。

- **demo**
  - 视验证方案决定是否补充富文本 span 长按示例或回归页。