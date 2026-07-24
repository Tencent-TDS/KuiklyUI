## Why

业务需要「让输入框获取焦点但键盘不弹出」的能力（如搜索页 popup 先获焦占位、用户点击后再弹键盘），但框架此前只有 `focus()`（获焦+弹键盘）和 `blur()`（失焦+收键盘）两条命令。业务被迫用 `focus() + hide()` 组合拼凑，导致键盘「先弹起又消失」的闪烁；OHOS 端更因「获焦即弹键盘」的系统限制完全无法实现。本次新增一条自包含原生命令 `focusWithoutKeyboard`，三端统一语义，键盘全程不出现。

## What Changes

- **新增 `focusWithoutKeyboard` 原生命令**：iOS / Android / OHOS 三端 render 层各自实现「获焦 + 无键盘」语义，作为一条自包含命令，调用方无需再下发 `focus`
- **iOS 端**：通过 dummy inputView (tag=99999) 抑制系统键盘，`becomeFirstResponder` 获焦；新增 `kr_originalInputView` 备份/还原业务自定义 inputView；手势委托在 dummy 在场时恢复键盘；键盘通知在 dummy 在场时过滤防死循环
- **Android 端**：`requestFocus()` + `hideSoftInputFromWindow()`，不调用 `showSoftInput`
- **OHOS 端**：利用 `NODE_TEXT_INPUT_ENABLE_KEYBOARD_ON_FOCUS` 属性 + blur→refocus 状态机绕过「获焦即弹键盘」限制
- **Compose DSL 层**：`SoftwareKeyboardController` 新增 `hideKeepFocus()` 接口；新增 `FOCUS_NO_KEYBOARD` 命令和 `pendingFocusNoKeyboard` 标记处理无焦点异步场景
- **core 层**：`AutoHeightTextAreaView` 新增 `focusWithoutKeyboard()` 方法，`blur()` 恢复纯失焦语义（移除 keepFocus 参数）
- **Compose 层**：`CoreTextField` 的 `inputFocus` 回调加 `if(!hasFocus)` 守卫防自激循环，`set(hasFocus)` 移除重复 `focus()` 调用
- **Demo**：新增 `HideKeyboardTestDemo` 测试页面

## Non-goals

- 不修改 Web / 小程序端的输入框行为（本期仅覆盖 iOS / Android / OHOS）
- 不改变 `focus()`（获焦+弹键盘）和 `blur()`（纯失焦）的既有语义
- 不为 macOS 单独实现 `focusWithoutKeyboard`（iOS 实现中 macOS 分支直接 `becomeFirstResponder`，不挂 dummy）
- 不引入新的自定义组件，仅扩展原生输入框能力

## Capabilities

### New Capabilities
- `keep-focus-without-keyboard`: 输入框「保持焦点但关闭键盘」能力——新增 `focusWithoutKeyboard` 原生命令及 Compose DSL `hideKeepFocus()` 接口，覆盖 iOS / Android / OHOS 三端

### Modified Capabilities
- `textfield-state-editing`: `blur()` 语义变更——移除 `keepFocus` 参数，恢复纯失焦；`CoreTextField` 的 `inputFocus` 回调增加去重守卫

## Impact

**受影响模块**：
| 模块 | 改动 |
|------|------|
| `core` | `AutoHeightTextAreaView.kt` — 新增 `focusWithoutKeyboard()`，`blur()` 移除 keepFocus |
| `compose` | `SoftwareKeyboardController.kt` — 新增 `hideKeepFocus()` 接口 + `FOCUS_NO_KEYBOARD` 命令；`CoreTextField.kt` — inputFocus 守卫 + 去重 |
| `core-render-ios` | `KRTextFieldView.m` + `KRTextAreaView.m` — dummy inputView 方案、手势委托、键盘通知过滤 |
| `core-render-android` | `KRTextFieldView.kt` — `setFocusWithoutKeyboard()` + 常量注册 |
| `core-render-ohos` | `KRTextFieldView.h/.cpp` — blur→refocus 状态机；`KRViewUtil.h/.cpp` — `UpdateInputNodeFocusAndKeyBoardStatus()` |
| `demo` | `HideKeyboardTestDemo.kt` — 测试页面 |

**受影响平台**：iOS / Android / HarmonyOS

**API 变更**：
- 新增公开 API：`SoftwareKeyboardController.hideKeepFocus()`
- 新增 core 方法：`AutoHeightTextAreaView.focusWithoutKeyboard()`
- 新增原生命令：`focusWithoutKeyboard`（三端 render 层）
- **BREAKING**（内部）：`AutoHeightTextAreaView.blur()` 移除 `keepFocus` 参数（仅框架内部调用，无业务直接调用）

**风险**：
- iOS dummy inputView 与业务自定义 inputView 的交互需充分测试（日期选择器/数字面板等场景）
- OHOS blur→refocus 状态机依赖 150ms 时间窗口，极端性能场景可能误判
