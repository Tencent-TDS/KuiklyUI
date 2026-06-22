## Why

iOS 平台的 Compose TextField 在输入时（尤其拼音/多语言场景）光标会在 index 0 与当前输入文字末尾之间反复跳动，严重影响输入体验。该问题由 commit `bacbfeae`（`feat(TextField): TextField maxLength support limit emoji (#1385)`）引入——该提交新增了 `selectionChange` / `textInputStateChange` 两条原生到 Compose 的回流链路，但 `textDidChange` 回调的 `onValueChange` 没有携带 selection 信息，导致 Compose 反向同步时将 `selection=0` 写回原生层，覆盖了正确光标位置。

## What Changes

- **修复**：在 `CoreTextField.kt` 的 `textDidChange` 事件处理器中，当调用 `onValueChange(TextFieldValue(...))` 时，若 `lastSyncedTextInputState` 的文本与新文本一致，则沿用其 selection 而非默认的 `TextRange.Zero`。
- 修复仅限于 Compose DSL 路径的单文件修改，自研 DSL（`Pager + body()`）不受影响，iOS 原生侧（`KRTextFieldView.m` / `KRTextAreaView.m`）无需修改。

## Capabilities

### New Capabilities
- `textfield-native-selection-preserve`: 当 Compose 层的 `textDidChange` 回调不含 selection 信息时，从 `lastSyncedTextInputState` 快照中继承正确的选区，避免错误地往原生层写入 `selection=0`。

### Modified Capabilities
- （无修改项。现有 `textfield` 相关规格的行为不变，修复仅涉及内部实现细节。）

## Non-goals

- 不修改自研 DSL（`Pager` / `body()`）路径。自研 DSL 的 `InputView.didSetProp` / `TextAreaView.didSetProp` 已有 `shouldSyncToNative` 防环逻辑，经观察日志确认无误。
- 不修改 iOS 原生侧代码（`KRTextFieldView.m` / `KRTextAreaView.m`）。
- 不改动 Android 平台的 TextField 行为。

## Impact

| 维度 | 说明 |
|------|------|
| 受影响平台 | **iOS**（仅 iOS 存在该回归） |
| 受影响模块 | **compose** — `CoreTextField.kt`（单文件修改） |
| 影响范围 | Compose DSL 下所有 TextField / BasicTextField 组件，包括单行和多行 |
| 风险 | 低。仅在 `textDidChange` 被 `pendingTextInputStateText` dedup 判定为不匹配时才生效，且只复用已经在同帧内被正确更新的 selection |
