## 1. core

- [x] 1.1 在 `core/src/commonMain/kotlin/com/tencent/kuikly/core/base/event/EventParams.kt` 为 `LongPressParams` 增加原始 `params` 透传字段，并保持现有长按字段语义不变。
- [x] 1.2 在 `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/RichTextView.kt` 为 `TextSpan` 增加 `longPress` DSL 注册入口，并补齐对应的 span 长按 handler 存储。
- [x] 1.3 在 `RichTextView.kt` 中为 `ISpan`、`RichTextAttr`、`RichTextEvent` 平行补齐 span 长按拦截与派发逻辑，保持与现有 span click 链路结构一致。
- [x] 1.4 在 `RichTextView.kt` 中实现 `activeSpanIndex` 的生命周期管理：`start` 阶段锁定命中 span，`move/end/cancel` 复用锁定目标，`end/cancel` 后清空。
- [x] 1.5 为 `core` 增加单元测试，覆盖 span 命中分发、未命中回退到 `RichText.longPress`、以及 `activeSpanIndex` 锁定与清理行为。

## 2. core-render-android

- [x] 2.1 在 `core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/component/KRRichTextView.kt` 复用现有 span click 命中逻辑，为长按事件构造并回传 span index。
- [x] 2.2 验证 Android 长按 `start/move/end/cancel` 回调都能携带 `core` 所需的原始 payload，并与 `activeSpanIndex` 语义对齐。
- [x] 2.3 为 Android 富文本长按补充回归测试或最小验证用例，确认 span click 与 span longPress 共存时互不影响。

## 3. core-render-ios

- [x] 3.1 在 `core-render-ios/Extension/AdvancedComps/KRRichTextView.m` 复用现有字符命中逻辑，为长按事件回传 span index。
- [x] 3.2 校验 iOS 长按状态流与 `activeSpanIndex` 语义一致，避免 `move` 过程中重新切换 span 目标。
- [x] 3.3 补充 iOS 富文本长按验证，确认命中 span 时优先派发给 `Span.longPress`，未命中时回退到 `RichText.longPress`。

## 4. core-render-ohos

- [x] 4.1 在 `core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.cpp` 复用现有富文本命中基础，为长按事件构造并回传 span index。
- [x] 4.2 在 `core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextShadow.cpp` 校验或补齐 span 索引映射所需的数据透传，确保长按与点击使用一致的 span 标识。
- [x] 验证 HarmonyOS 长按 `start/move/end/cancel` 回调与 `activeSpanIndex` 语义一致，避免 `move` 过程中重新切换目标。
- [x] 为 HarmonyOS 富文本长按补充回归测试或最小验证用例，确认 span click 与 span longPress 共存时互不影响。

## 5. demo 与联调验证

- [x] 5.1 评估是否需要在 `demo` 中补充富文本 span 长按示例；若需要，新增最小示例以覆盖 span click 与 span longPress 共存场景。
- [x] 5.2 在 Android demo 上手动验证：命中 span 时只触发对应 `Span.longPress`，未命中时回退到 `RichText.longPress`。
- [x] 5.3 在 iOS demo 上手动验证：长按 `start` 锁定目标 span，后续 `move/end/cancel` 不切换目标。
- [x] 5.4 在 HarmonyOS demo 或最小验证页上手动验证：命中 span 时只触发对应 `Span.longPress`，未命中时回退到 `RichText.longPress`。
- [x] 5.5 汇总 Android/iOS/HarmonyOS 联调结论，确认首版交付范围与遗留问题。