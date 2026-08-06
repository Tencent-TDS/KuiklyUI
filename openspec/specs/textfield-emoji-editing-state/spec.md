## ADDED Requirements

### Requirement: 表情/文本长度计量（LengthLimitType）
Compose DSL 的 TextField SHALL 支持按 `LengthLimitType`（CHARACTER / BYTE / VISUAL_WIDTH）三种维度限制输入长度，并在超出时通过 `onLimitChange` 回调通知。

#### Scenario: 按字符数限制（Android）
- **WHEN** 业务为 TextField 配置 `maxLength(10, LengthLimitType.CHARACTER)` 并输入超过 10 个字符
- **THEN** 系统 SHALL 截断至 10 个字符并触发 `onLimitChange(true)`

#### Scenario: 按字节数限制（Android）
- **WHEN** 业务配置 `maxLength(20, LengthLimitType.BYTE)` 且输入含多字节表情
- **THEN** 系统 SHALL 以 UTF-8 字节数计量，超出 20 字节时截断并回调

#### Scenario: 按可见宽度限制（Android/iOS）
- **WHEN** 业务配置 `maxLength(100, LengthLimitType.VISUAL_WIDTH)` 且累计字形宽度超限
- **THEN** 系统 SHALL 按字形可见宽度截断输入

### Requirement: 文本后处理（textPostProcessor）
Compose DSL 的 TextField SHALL 支持 `textPostProcessor` 与 `TextPostProcessorOutputTransformation`，在文本提交前按其规则转换。

#### Scenario: 提交前转换（Android/iOS）
- **WHEN** 业务设置 `textPostProcessor` 并在输入提交/回填时
- **THEN** 系统 SHALL 先经 `TextPostProcessorOutputTransformation` 转换文本，再更新编辑态

### Requirement: 编辑态事件统一
Compose DSL 的 `TextInputState` / `CoreTextField` SHALL 统一 `textInputStateChange` / `textDidChange` / `selectionChange` 三个原生事件的时序与一致性，避免光标跳动与选区丢失。

#### Scenario: 原生编辑态变更一致回写（Android/iOS）
- **WHEN** 原生层通过 `handleNativeEditingStateChange` 上报编辑态（文本/选区/组合态）
- **THEN** 系统 SHALL 经 `toTextFieldValue` / `toTextInputState` 一致映射到 Compose 状态，并保证 `coerceToTextBounds` 不越界

#### Scenario: 文本/选区不丢失（Android/iOS）
- **WHEN** 拼音输入中间态或快速连续输入触发多次原生回调
- **THEN** 系统 SHALL 以 `hasSameEditingState` 去重，避免中间态误上屏或光标跳变
