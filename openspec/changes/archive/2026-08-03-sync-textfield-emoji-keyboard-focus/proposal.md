## Why

`ComposeOnKuikly` 仓库（作者 valoxbwang）在 MR107/108/110 中对 Compose DSL 的 TextField 做了多次能力增强：表情输入的长度计量（`LengthLimitType` 字符/字节/可见宽度）、`textPostProcessor` 文本后处理、编辑态事件统一（`textInputStateChange`/`textDidChange`/`selectionChange` 时序与一致性）。这些能力尚未同步进上游 `KuiklyUI`，导致 KuiklyUI 的 Compose TextField 在表情输入、长度限制、编辑态回调等场景下行为落后、存在光标跳动/选区丢失等已知问题。

本变更将以上 MR 的代码改动（core / compose / iOS 渲染层）同步到 KuiklyUI，使两个仓库的 TextField 能力对齐。

## What Changes

- **新增表情/长度计量能力**：`LengthLimitType`（CHARACTER / BYTE / VISUAL_WIDTH）与 `maxLength(...)` / `onLimitChange(...)`，按不同计量维度限制输入长度。
- **新增文本后处理**：`textPostProcessor` 与 `TextPostProcessorOutputTransformation`，支持在提交前对文本做转换。
- **编辑态事件统一**：`TextInputState` 增加 `coerceToTextBounds` / `hasSameEditingState` 等字段；`CoreTextField` 增加 `toTextFieldValue` / `toTextInputState` / `handleNativeEditingStateChange`；iOS 渲染层增加 `p_calculateCharacterLength*`、`p_selectedRange` 等一致性实现。
- **Demo**：`demo/.../compose/TextFieldEmojiDemo.kt` 更新为 MR107 版本（6 个用例），保留 KuiklyUI 的 `ComposeNavigationBar` 包裹。

**BREAKING**：无公开 API 删除；`TextFieldState` / `CoreTextField` 等内部编辑态契约有调整，属实现层一致化，不影响对外调用方。

## Capabilities

### New Capabilities
- `textfield-emoji-editing-state`: 覆盖 Compose DSL TextField 的表情长度计量、文本后处理、编辑态事件统一，对齐 ComposeOnKuikly 的 MR107/108/110 能力。

## Impact

- **受影响平台**：Android / iOS（本次迁移覆盖）。
- **受影响模块**：
  - `core/` — `TextInputState.kt`（编辑态字段）
  - `compose/` — `CoreTextField.kt`、`TextFieldState.kt`、`material3/TextField.kt`（Compose 胶水与 API）
  - `core-render-ios/` — `KRTextFieldView.m`、`KRTextAreaView.m`
  - `demo/` — `TextFieldEmojiDemo.kt`
- **依赖/约束**：`core/` 与 `compose/` 为纯 KMP 模块，禁止依赖 `core-render-*`；Compose 包名除 `androidx.compose.runtime.*` 外一律用 `com.tencent.kuikly.compose.*`。

## Non-goals

- **不包含 Demo 提交**：`TextFieldEmojiDemo.kt` 仅作为验证用途，按约定不纳入本次提交。
- **不修改 KuiklyDSL（自研 DSL）**：仅同步 Compose DSL 相关路径。
- **不包含键盘/焦点处理相关能力**：如 `focusWithoutKeyboard` 免键盘获焦，该部分未在本轮同步。
