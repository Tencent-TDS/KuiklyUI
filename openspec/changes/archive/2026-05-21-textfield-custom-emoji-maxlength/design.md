## Context

本变更只适用于 **Compose DSL**，目标是把 `TextField` 自定义表情与 `Modifier.maxLength` 的组合行为正式化。当前仓库已经具备以下基础能力：

- `TextFieldState` / `state.edit { replace(...) }` 已经可以基于 raw selection 在光标处或选区上插入短码。
- `Modifier.maxLength` / `Modifier.onLimitChange` 已经通过 `CoreTextField` 下传到 `core/` 与原生渲染层，由各平台按 `LengthLimitType.CHARACTER` / `BYTE` / `VISUAL_WIDTH` 计算真实长度。
- Android 与 iOS 渲染层已经实现 `textInputStateChange`、`selectionChange`、`textLengthBeyondLimit`、`setTextInputState` 等链路，能够在程序化插入后回传 raw text、selection 和 length。
- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextFieldEmojiDemo.kt` 已经是现成的回归入口，覆盖 CHARACTER / BYTE / VISUAL_WIDTH / singleLine 场景。

本次缺少的是更精确的行为约束：当业务通过表情面板在文本中间执行 `replace(selection.start, selection.end, shortCode)` 时，maxlength 必须基于“替换后的 raw text”判断；如果整个短码放不下，不能插入半个 token，也不能破坏光标与前后文。

### NativeBridge / 跨层交互

本变更继续复用现有 NativeBridge 语义，不引入新的桥接数据结构：

- Kotlin 侧通过 `setTextInputState` 把 raw text 与 selection 同步到原生输入框。
- 原生层通过 `textInputStateChange` 回传实际生效的 raw text、selection 和 renderer 计算出的 `length`。
- 原生层在纯光标移动时继续通过 `selectionChange` 回传 raw selection。
- 原生层在超限拒绝插入时继续通过 `textLengthBeyondLimit` 通知 Compose 层。

## Goals / Non-Goals

**Goals:**

- 明确 custom emoji 短码插入与 `Modifier.maxLength` 的组合语义。
- 保证中间插入、选区替换、单行和多行场景都基于替换后的 raw text 做长度校验。
- 保证 custom emoji 以**完整短码为原子单位**参与插入：超限时拒绝整段短码，而不是写入残缺 token。
- 保持 `onLimitChange`、`textLengthBeyondLimit`、`selectionChange` 继续以渲染层实际结果为准。
- 复用现有 `TextFieldEmojiDemo.kt` 作为 Android / iOS 回归矩阵。

**Non-Goals:**

- 不新增新的 emoji 输入 API 或新的长度限制类型。
- 不改造 self-DSL `Input` / `TextArea` 的公开接口。
- 不扩展 HarmonyOS、Web、miniApp、macOS 的同类行为。
- 不重写 `TextPostProcessorOutputTransformation` 或引入新的 inline content 协议。

## Decisions

### Decision 1: 长度判定继续以下沉到渲染层为准

`Modifier.maxLength` 的最终长度判定继续由 Android / iOS 原生渲染层执行，Compose / core 只负责下发约束和消费回调。

**Rationale:**

- `CHARACTER` / `BYTE` / `VISUAL_WIDTH` 三种长度策略已经在渲染层拥有真实实现，尤其涉及 emoji span / attachment 时，Kotlin 层很难保证和原生一致。
- `onLimitChange` 已经依赖 `textInputStateChange.length` 与 `textLengthBeyondLimit`，沿用现有链路改动最小。

**Alternative considered:** 在 `state.edit {}` 或 Compose 层预先计算剩余额度。Rejected，因为这会复制一套与原生不同步的长度规则，容易在 emoji / attachment 场景下出现分歧。

### Decision 2: custom emoji 插入按完整短码原子处理

当业务通过表情面板插入如 `[smile]` 这样的短码时，系统把整段短码视为一次原子编辑。

**Rationale:**

- 写入半截短码会让 `textPostProcessor` 无法稳定渲染，并且破坏 raw text 的可读性与可恢复性。
- 业务视角中“插入一个表情”天然对应一个完整 token，而不是普通字符流截断。

**Alternative considered:** 允许超限时把短码截断到剩余长度。Rejected，因为会产生残缺 token，且不同长度策略下难以定义稳定语义。

### Decision 3: 中间插入与选区替换统一走 replace-after-selection 语义

所有 emoji 面板插入都统一建模为 `replace(selection.start, selection.end, shortCode)`，由现有 selection 同步能力决定替换位置，再由渲染层基于替换后的 raw text 执行长度校验。

**Rationale:**

- 统一中间插入、末尾追加、选区覆盖三种路径，减少额外分支。
- 当前 `TextFieldState` 与 `TextInputState` 已具备 raw selection 能力，无需再引入额外 cursor API。

**Alternative considered:** 对中间插入单独做“先删除选区，再 append”的分支。Rejected，因为会改变前后文语义，并增加 selection 恢复复杂度。

### Decision 4: 超限后的文本与光标以“实际生效结果”回传

无论是允许插入还是拒绝插入，Compose 层都以原生层最终回传的 `textInputStateChange` / `selectionChange` 为准更新状态；如果本次插入被拒绝，前后文与 selection 必须保持在合法 raw index。

**Rationale:**

- 原生层是最终接受或拒绝输入的一方，尤其在 iOS 单行输入与 Android 输入过滤器路径下更是如此。
- 这样可以避免 Compose 侧先假设插入成功，再被原生回滚时出现状态闪烁。

**Alternative considered:** Compose 先乐观更新 state，再等待原生纠正。Rejected，因为在超限与 selection 纠正场景下容易造成瞬时错误状态。

### Decision 5: 复用 `TextFieldEmojiDemo.kt` 作为唯一 demo 入口

不新增新页面，直接在现有 demo 中保留并强化 CHARACTER / BYTE / VISUAL_WIDTH / singleLine 的验证说明。

**Rationale:**

- 现有 demo 已覆盖本次关心的输入方式与长度策略。
- 复用页面可以降低维护成本，也更方便后续回归。

**Alternative considered:** 新建一个独立的 maxlength emoji demo。Rejected，因为会和现有示例重复，增加文档与导航负担。

## File Changes by Module

### `compose/`

- `compose/.../CoreTextField.kt`
  - 保持 `maxLength`、`onLimitChange`、`textInputStateChange`、`selectionChange` 的协作语义。
  - 确保程序化 `replace(selection.start, selection.end, shortCode)` 后以原生实际回传结果更新状态。
- `compose/.../TextFieldMaxLengthModifiers.kt`
  - 保持 `Modifier.maxLength` / `Modifier.onLimitChange` 的公开契约描述与实现一致。

### `core/`

- `core/.../InputView.kt`
- `core/.../TextAreaView.kt`
  - 继续承载 `maxTextLength`、`selectionChange`、`textLengthBeyondLimit`、`setTextInputState` 的桥接语义，不新增新接口。

### `core-render-android/`

- `core-render-android/.../KRTextFieldView.kt`
  - 保证输入过滤器在 custom emoji 中间插入、选区替换时也使用替换后的 raw text 结果做长度判定。
  - 保证超限拒绝后不会破坏前后文和 selection。

### `core-render-ios/`

- `core-render-ios/.../KRTextFieldView.m`
- `core-render-ios/.../KRTextAreaView.m`
  - 保证程序化插入和选区替换都经过统一长度判定。
  - 保证超限时拒绝整段短码并回传合法 selection。

### `demo/`

- `demo/.../TextFieldEmojiDemo.kt`
  - 复用现有页面，覆盖中间插入、选区替换、CHARACTER / BYTE / VISUAL_WIDTH、singleLine 回归说明。

### `docs/`

- `docs/Compose/core-components.md`
  - 补充 custom emoji 与 `Modifier.maxLength` 组合使用时的行为说明。

## Risks / Trade-offs

- **[Risk]** Android 与 iOS 对程序化超限输入的底层处理路径不同 → **Mitigation**：统一以“原生最终接受结果”作为 Compose 状态来源，并在 demo 中覆盖两端回归。
- **[Risk]** 若把短码当普通字符串截断，可能生成残缺 token → **Mitigation**：明确 custom emoji 插入按完整短码原子处理，超限时拒绝整段插入。
- **[Risk]** 纯 Kotlin 预判长度会与原生 span / attachment 口径不一致 → **Mitigation**：继续只让原生渲染层负责最终长度计算。
- **[Risk]** `onLimitChange` 的边界口径易与业务直觉不一致 → **Mitigation**：文档明确 `limit = true` 表示达到或超过上限。

## Migration Plan

1. 保持现有 `Modifier.maxLength`、`Modifier.onLimitChange`、`TextPostProcessorOutputTransformation` API 不变。
2. 在 Android / iOS 渲染层补齐 custom emoji 中间插入与选区替换的 maxlength 处理细节。
3. 复用 `TextFieldEmojiDemo.kt` 验证中间插入、选区替换、singleLine 回归和三种长度策略。
4. 更新 Compose 文档，说明 custom emoji 与 maxlength 的组合语义。
5. 如出现回归，优先回滚各端对 emoji 原子插入的限制逻辑，保留原有 maxLength 与 text post-processor 基础能力。

## Open Questions

- iOS 单行 `UITextField` 在 custom emoji 超限时是否需要与多行 `UITextView` 完全一致地返回 `textLengthBeyondLimit` 节奏？
- `TextFieldEmojiDemo.kt` 中是否需要显式增加“拒绝整段短码插入”的 UI 提示，还是只保留长度计数与红字提示即可？
