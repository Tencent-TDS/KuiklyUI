## Why

Kuikly `RichText` 目前只支持 `Span.click`，业务无法对单个文字 `Span` 或 `ImageSpan` 绑定长按交互，只能退化到整个 `RichText` 级别处理，无法精确识别被长按的内容。补齐 `Span.longPress` 后，富文本可以支持更自然的链接菜单、词条浮层、上下文操作等交互，并与现有的 span 点击能力形成一致的事件模型。

## What Changes

- 为 Kuikly DSL 富文本 `Span` 增加 `longPress` 能力，使业务可以声明式监听单个 span 的长按事件。
- 为富文本事件分发链路增加 span 级长按拦截和派发能力，保证 `RichText.longPress` 与 `Span.longPress` 的优先级和回调行为可预测。
- 为基础长按事件参数补齐承载原始命中信息的能力，使 span index 等附加数据可以从渲染层透传到 `core` 公共层。
- 为 Android、iOS、HarmonyOS 富文本渲染层补齐 span 长按命中与 index 回传逻辑，优先复用现有 span 点击命中实现。
- 明确定义 span 长按的手势语义：在 `start` 阶段锁定首个命中的 span，并将同一轮长按手势的后续状态派发给该 span。
- 首版覆盖 Android、iOS、HarmonyOS 三端；Web、miniApp、macOS 暂不在本次交付范围内。
- 预计不引入破坏性变更。

## Non-goals

- 不重新设计 `RichText` 全部事件系统，也不将 click/longPress 统一重构为全新的手势抽象层。
- 不在本次变更中扩展文本选择、系统菜单、复制粘贴等原生富文本交互能力。
- 不要求首版同时完成 miniApp、macOS 等所有剩余平台的渲染实现。
- 不修改 Compose DSL 专有组件或引入与 `compose/` 模块耦合的新接口。

## Capabilities

### New Capabilities

- `span-longpress`：覆盖 `core` 中富文本 span 长按 DSL 与事件模型，以及 `core-render-android` / `core-render-ios` / `core-render-ohos` 中的 span 长按命中与事件回传行为。

### Modified Capabilities

- 无。

## Impact

- **影响平台**：Android、iOS、HarmonyOS 为首版目标平台；Web、miniApp、macOS 本次不在首版交付范围内。
- **影响模块**：
  - `core`：增加 `Span.longPress` DSL、span 长按分发逻辑、长按参数扩展。
  - `core-render-android`：补齐富文本 span 长按命中与 index 回传。
  - `core-render-ios`：补齐富文本 span 长按命中与 index 回传。
  - `core-render-ohos`：补齐富文本 span 长按命中与 index 回传。
  - `compose`：不受影响。
  - `demo`：可能补充示例页或验证场景，用于覆盖 span 点击与长按共存行为。
- **API 影响**：新增非破坏性的 `Span.longPress` 相关 DSL 与事件能力。
- **依赖影响**：预计不新增第三方依赖。