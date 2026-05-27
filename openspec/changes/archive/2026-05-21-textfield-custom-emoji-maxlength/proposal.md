## Why

KuiklyUI Compose `TextField` 已经支持自定义表情短码渲染和 `Modifier.maxLength`，但业务仍需要一个明确的能力契约：当用户把光标移动到文本中间或选中一段文本后再插入表情短码时，maxlength 仍必须按最终 raw text 结果生效，不能退化成只对末尾追加生效。现在补这个变更，是为了把“表情插入 + 长度限制 + 光标/选区替换”收敛成一个可验证、可回归的正式变更，并复用现有 `TextFieldEmojiDemo.kt` 作为回归载体。

## What Changes

- 明确 `TextField` 自定义表情插入在使用 `Modifier.maxLength` 时的行为契约：无论是末尾追加、光标中间插入，还是替换选中区间，都必须基于替换后的 raw text 做长度校验。
- 明确长度校验继续由各平台渲染层按 `LengthLimitType.CHARACTER` / `BYTE` / `VISUAL_WIDTH` 计算，避免 Kotlin 层与原生层对 emoji/attachment 长度口径不一致。
- 明确当中间插入表情导致超限时，系统必须拒绝本次完整短码插入，不得写入残缺短码，同时保留未受影响的前后文和有效光标位置。
- 明确 `onLimitChange` / `textLengthBeyondLimit` 在 custom emoji 场景下仍返回渲染层计算出的真实长度与限额状态。
- 复用并补充 `demo/` 中现有 `TextFieldEmojiDemo.kt`，覆盖 CHARACTER / BYTE / VISUAL_WIDTH 以及单行回归场景。

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `textfield-transformations`: 扩展 TextField 变换与长度限制能力，覆盖 custom emoji 在中间插入、选区替换和超限回退时的 maxlength 行为契约。

## Impact

- **受影响 DSL**：本变更只针对 **Compose DSL**；不新增 self-DSL API。
- **受影响模块**：
  - `compose/`：`BasicTextField` / `CoreTextField` 的 `maxLength`、`onLimitChange`、state/edit 插入路径需要遵守新的行为契约。
  - `core/`：继续承载 `LengthLimitType`、`TextInputState`、`textLengthBeyondLimit`、`selectionChange` 等跨层事件语义。
  - `core-render-android/`：长度过滤器与 `KRTextFieldView` 需要保证 custom emoji 插入和中间替换时的长度校验一致。
  - `core-render-ios/`：`KRTextFieldView` / `KRTextAreaView` 需要保证程序化插入、选区替换和超限后的光标恢复符合契约。
  - `demo/`：复用 `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextFieldEmojiDemo.kt` 作为功能与回归示例。
  - `docs/`：需要同步 Compose TextField maxlength 与 custom emoji 的行为说明。
- **受影响平台**：Android、iOS；其他平台不在本次变更范围内。
- **受影响 API/行为**：`Modifier.maxLength`、`Modifier.onLimitChange`、`TextFieldState.edit { replace(...) }`、`TextPostProcessorOutputTransformation` 的组合语义会被正式化。

## Non-goals

- 不新建一套独立于 `maxLength` 之外的 emoji 限制 API。
- 不改变 `TextFieldState` / `TextInputState` 的基础模型，只复用现有选择区与 raw text 同步能力。
- 不扩展到 HarmonyOS、Web、miniApp、macOS 的 custom emoji maxlength 行为。
- 不重做 demo 页面结构，只在现有 `TextFieldEmojiDemo.kt` 上补充和复用回归场景。
