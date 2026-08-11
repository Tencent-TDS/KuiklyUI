## 1. core 模块（编辑态字段）
- [x] 1.1 确认 `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/TextInputState.kt` 含 `coerceToTextBounds` / `hasSameEditingState` / 字符长度计算字段，编译通过
- [x] 1.2 验证 `LengthLimitType` 枚举在 core 层可用（CHARACTER / BYTE / VISUAL_WIDTH）

## 2. compose 模块（Compose 胶水与 API）
- [x] 2.1 确认 `compose/.../foundation/text/CoreTextField.kt` 含 `toTextFieldValue` / `toTextInputState` / `handleNativeEditingStateChange`
- [x] 2.2 确认 `compose/.../foundation/text/input/TextFieldState.kt` 含 `clearText` / `setTextAndSelect` / 编辑态一致性逻辑
- [x] 2.3 确认 `compose/.../material3/TextField.kt` 接入长度限制与 `TextFieldValue` 用例
- [x] 2.4 `compose` 模块 `compileReleaseKotlinAndroid` 编译通过，无新增告警

## 3. core-render-ios 模块（编辑态一致性）
- [x] 3.1 确认 `core-render-ios/Extension/Components/KRTextFieldView.m` 含组合文本 payload 重构 / `p_calculateCharacterLength*` / `p_selectedRange`（编辑态一致性）
- [x] 3.2 确认 `core-render-ios/Extension/Components/KRTextAreaView.m` 含组合文本 payload 重构 / `p_currentTextInputStatePayload` / `p_notifyTextInputStateChangeIfNeeded`
- [x] 3.3 iOS 端 `clang -fobjc-arc` 语法检查两个 `.m` 文件无错误（已由真机 Xcode 完整编译超集覆盖）
- [x] 3.4 真机验证表情候选栏闪烁、连续输入编辑态回写一致（真机运行 TextFieldEmojiDemo 验证）

## 4. demo 模块（验证用，不提交）
- [x] 4.1 确认 `demo/.../compose/TextFieldEmojiDemo.kt` 为 MR107 版本并保留 `ComposeNavigationBar` 包裹
- [x] 4.2 运行 Demo 验证 6 个用例：CHARACTER/BYTE/VISUAL_WIDTH 长度限制、textPostProcessor、`Material3 TextFieldValue`

## 5. 平台验证（Android / iOS）
- [x] 5.1 Android：编译 `:core:compose:demo` 的 `compileReleaseKotlinAndroid` 通过，Demo 跑通表情长度限制与编辑回调
- [x] 5.2 iOS：`.m` 语法检查通过 + 真机回归表情候选栏、编辑态回调（真机运行覆盖）
- [x] 5.3 IDE 静态检查（read_lints）确认 Kotlin 改动文件诊断 0 条

## 6. 提交与收尾
- [x] 6.1 仅 `git add` 非 demo 迁移文件（`core/` / `compose/` / `core-render-ios/`），排除 `TextFieldEmojiDemo.kt` 与无关的 `iosApp/iosApp.xcodeproj/project.pbxproj`
- [x] 6.2 提交完成（commit `fc6f0f8c9`：`feat: sync TextField emoji input keyboard focus handling from ComposeOnKuikly upstream`；demo 与 project.pbxproj 按约定未纳入）
- [x] 6.3 记录 Non-goals 中遗留项：键盘/焦点处理（如 `focusWithoutKeyboard` 免键盘获焦）未在本轮同步，需后续单独补齐
