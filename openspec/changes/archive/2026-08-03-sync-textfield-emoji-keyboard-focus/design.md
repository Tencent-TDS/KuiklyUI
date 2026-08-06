## Context

`KuiklyUI` 的 Compose DSL 层（`compose/` + `core/`）与 `ComposeOnKuikly` 仓库存在 TextField 能力代差。后者在 MR107/108/110（作者 valoxbwang）中落地了表情输入长度计量、`textPostProcessor` 文本后处理、编辑态事件统一。上游 `KuiklyUI` 当前缺少这些实现，表现为表情长度统计不准、编辑回调时序不一致。

本变更把上述改动同步进 `KuiklyUI`，对齐两个仓库的 TextField 编辑态行为。DSL 模式为 **Compose DSL**（`ComposeContainer` + `setContent{}`）；自研 DSL 不受影响。

## Goals / Non-Goals

**Goals:**
- 在 `core/` + `compose/` 落地长度计量（`LengthLimitType`）、`textPostProcessor`、编辑态事件统一（字段 + 一致性逻辑）。
- 在 `core-render-ios` 同步编辑态一致性改动（组合文本 payload 重构）。
- 更新 `demo/.../compose/TextFieldEmojiDemo.kt` 为 MR107 版本以验证能力（不纳入提交）。

**Non-Goals:**
- 不包含键盘/焦点处理相关能力（如 `focusWithoutKeyboard` 免键盘获焦）——该部分未在本轮同步。
- 不包含自研 DSL 的对应改动。

## Decisions

### 决策 1：以 ComposeOnKuikly 的 MR107/108/110 为单一同步源
**选择**：直接以 `ComposeOnKuikly` 对应提交的文件内容覆盖 `KuiklyUI`，而非逐行 reinterpret。
**理由**：两仓库同源于 Kuikly，API 形态一致，整文件同步可避免遗漏。
**备选**：逐函数 cherry-pick——成本高且易漏字段（如 `coerceToTextBounds`）。否决。

### 决策 2：Demo 保留 `ComposeNavigationBar` 包裹
**选择**：用 MR107 版 `TextFieldEmojiDemo.kt` 覆盖后，保留 KuiklyUI 的 `ComposeNavigationBar { }` 包裹。
**理由**：KuiklyUI 全部 76 个 Compose Demo 均用该包裹，去掉会丢失导航栏、与项目不一致（MR107 在 ComposeOnKuikly 中去掉了包裹，但那是该仓库结构，不适用 KuiklyUI）。
**备选**：完全照搬 ComposeOnKuikly 版本——与 KuiklyUI 其它 Demo 不一致。否决。

## File Changes (按模块分组)

**core/**
- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/TextInputState.kt`：`coerceToTextBounds`、`hasSameEditingState`、字符长度计算字段。

**compose/**
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/CoreTextField.kt`：`toTextFieldValue` / `toTextInputState` / `handleNativeEditingStateChange`。
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/input/TextFieldState.kt`：`clearText` / `setTextAndSelect` / 编辑态一致性。
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/material3/TextField.kt`：Material3 适配（`TextFieldValue` 用例、长度限制接入）。

**core-render-ios/**
- `core-render-ios/Extension/Components/KRTextFieldView.m`：组合文本 payload 重构 / `p_calculateCharacterLength*` / `p_selectedRange`（编辑态一致性）。
- `core-render-ios/Extension/Components/KRTextAreaView.m`：组合文本 payload 重构 / `p_currentTextInputStatePayload` / `p_notifyTextInputStateChangeIfNeeded`（编辑态一致性）。

**demo/**（验证用，不提交）
- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextFieldEmojiDemo.kt`：MR107 版本 + `ComposeNavigationBar` 包裹。

## Risks / Trade-offs

- **[风险] 编辑态字段不一致**：`TextInputState` 新增字段若与 KuiklyUI 既有字段语义冲突 → 缓解：编译期 `compileReleaseKotlinAndroid` 已通过，运行时需 `TextFieldEmojiDemo` 多用例回归。
- **[风险] iOS 组合文本 payload 重构回归**：`p_currentTextInputStatePayload` / `p_notifyTextInputStateChangeIfNeeded` 改动可能影响编辑态回写 → 缓解：需真机回归表情候选栏与连续输入场景。

## Migration Plan

1. 提交非 demo 迁移文件（`core/` / `compose/` / `core-render-ios/`；见 tasks）。
2. 编译校验：`:core`/`:compose`/`:demo` 的 `compileReleaseKotlinAndroid` + iOS `.m` 的 `clang -fobjc-arc` 语法检查（已通过）。
3. 用 `TextFieldEmojiDemo.kt` 做功能回归（表情长度、文本后处理、编辑回调）。
4. 回滚：若回归失败，`git revert` 本次提交即可，无迁移脚本/数据变更，回滚成本低。

## Open Questions

- `core-render-ios` 的编辑态一致性改动（组合文本 payload）是否需补充 HarmonyOS 端（`core-render-ohos`）的对应实现？
