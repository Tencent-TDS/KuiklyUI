## Context

KuiklyUI 框架此前只有两条输入框焦点命令：
- `focus()` — 获焦 + 弹起系统键盘
- `blur()` — 失焦 + 收起键盘

业务场景（如搜索页 popup 先获焦占位、用户点击后再弹键盘）需要「保持焦点但键盘不出现」的能力。此前业务只能用 `focus() + hide()` 组合拼凑，存在三个问题：
1. 键盘「先弹起又消失」的视觉闪烁
2. OHOS 端因 `NODE_FOCUS_STATUS=1` 同时获焦+弹键盘的系统限制，完全无法实现
3. `hide()` 在某些时序下会连带失焦，焦点无法保持

本次变更新增一条自包含原生命令 `focusWithoutKeyboard`，三端各自选择最优实现，上层统一语义。

**适用 DSL 模式**：Compose DSL（通过 `SoftwareKeyboardController.hideKeepFocus()`）和自研 DSL（通过 `AutoHeightTextAreaView.focusWithoutKeyboard()`）均可使用，二者通过 `core` 层统一桥接。

**NativeBridge 交互**：上层 → `AutoHeightTextAreaView.focusWithoutKeyboard()` → `renderView.callMethod("focusWithoutKeyboard", "")` → 各端 render 层 `CallMethod` 分发 → 原生实现。命令为自包含语义，不依赖参数，不触发后续 `focus` 命令。

## Goals / Non-Goals

**Goals:**
- 三端（iOS / Android / OHOS）统一提供 `focusWithoutKeyboard` 原生命令，语义为「获焦 + 键盘全程不出现」
- Compose DSL 提供 `SoftwareKeyboardController.hideKeepFocus()` 公开接口
- 自研 DSL 通过 `AutoHeightTextAreaView.focusWithoutKeyboard()` 可用
- 无焦点场景（Compose 焦点异步送达）也能正确工作
- 不产生键盘闪烁、死循环等副作用
- 保护业务自定义 inputView（如日期选择器、数字面板）不被框架 dummy 覆盖

**Non-Goals:**
- 不覆盖 Web / 小程序端（本期仅 iOS / Android / OHOS）
- 不改变 `focus()` 和 `blur()` 的既有语义
- 不为 macOS 单独实现（iOS 代码中 macOS 分支直接 `becomeFirstResponder`）
- 不引入新的自定义组件

## Decisions

### 决策 1：`focusWithoutKeyboard` 作为独立原生命令，而非 `focus()` + `hide()` 组合

**选择**：新增独立命令 `focusWithoutKeyboard`，由原生侧自包含完成「获焦 + 无键盘」。

**理由**：
- 组合方案的根本问题是时序竞态——`focus()` 弹键盘和 `hide()` 收键盘之间存在不可消除的间隙，导致闪烁
- 独立命令让原生侧在一个原子操作内完成获焦和抑制键盘，上层无需关心时序

**备选方案**：
- `focus(keepKeyboard=false)` 参数化 `focus`：语义不如独立命令清晰，且 `focus` 的方法签名变更影响面大
- 上层 `focus() + hide()` 组合：无法消除闪烁，OHOS 不可行

### 决策 2：iOS — dummy inputView (tag=99999) 抑制键盘

**选择**：创建零尺寸 `UIView`（tag=99999）挂到 `inputView` 属性，`becomeFirstResponder` 获焦但系统键盘被 dummy 替代不弹。

**理由**：
- iOS `UITextField.inputView` / `UITextView.inputView` 是系统级抑制键盘的标准机制
- tag=99999 作为框架标识，区分业务自定义 inputView
- 已获焦时挂 dummy + `reloadInputViews` 即可收键盘保留焦点

**备选方案**：
- `inputAccessoryView` 操控：无法完全抑制键盘
- `endEditing` + 延迟 `becomeFirstResponder`：时序不可控，且 OHOS 无法复用思路

### 决策 3：iOS — `kr_originalInputView` 备份还原业务 inputView

**选择**：挂 dummy 前检查当前 `inputView` 是否为业务自定义（非 nil 且 tag≠99999），若是则备份到 `kr_originalInputView`；恢复键盘时还原。

**理由**：业务可能已设置自定义 inputView（日期选择器、数字面板等），框架 dummy 不能覆盖业务配置。

### 决策 4：iOS — 键盘通知在 dummy 在场时过滤

**选择**：`keyboardWillShow` / `keyboardWillHide` 在 `inputView.tag == 99999` 时直接 return。

**理由**：挂/卸 dummy inputView 并 `reloadInputViews` 时系统会派发键盘通知，若转发给业务会被误判为「用户收起键盘」→ 关闭 popup → 重建 → 再次 `focusWithoutKeyboard` → 死循环。

### 决策 5：iOS — 手势委托恢复键盘

**选择**：添加 `UITapGestureRecognizer` + `UIGestureRecognizerDelegate`，`shouldReceiveTouch:` 仅在 dummy 在场时返回 YES，点击时调用 `css_focus:` 清除 dummy 恢复系统键盘。

**理由**：用户在免键盘获焦态下点击输入框，预期恢复系统键盘开始输入，而非保持无键盘态。

### 决策 6：Android — `requestFocus()` + `hideSoftInputFromWindow()`

**选择**：`isFocusable = true` → `requestFocus()` → `post { hideSoftInputFromWindow() }`，不调用 `showSoftInput`。

**理由**：Android 的 `requestFocus` 和 `showSoftInput` 是分离的 API，只要不主动 `showSoftInput` 即可获焦不弹键盘。`post` 确保 `requestFocus` 生效后再 hide。

### 决策 7：OHOS — blur→refocus 状态机

**选择**：
1. `FocusWithoutKeyBoard()` 先写 `NODE_TEXT_INPUT_ENABLE_KEYBOARD_ON_FOCUS=0`（关闭获焦即弹键盘），置 `pending_refocus_after_blur_=true`，调 `Blur()`
2. `OnInputBlur()` 三分支：① pending → `ScheduleRefocus(arm=true)` ② awaiting + <150ms → `ScheduleRefocus(arm=false)` ③ 其他 → 正常上抛 blur
3. `ScheduleRefocus` 在下一帧调 `UpdateInputNodeFocusStatus(1)` 重聚焦

**理由**：
- OHOS `NODE_FOCUS_STATUS=1` 会同时获焦+弹键盘，无法分离
- `NODE_TEXT_INPUT_ENABLE_KEYBOARD_ON_FOCUS=0` 关闭获焦即弹键盘，但需要先 blur 再 refocus 才能生效
- IME 拆卸会派发尾部 blur，用 150ms 窗口 + `awaiting_teardown_blur_` 标记区分「IME 拆卸噪声 blur」和「真实失焦」

**备选方案**：
- 固定定时器延迟 refocus：时序不稳定，极端性能下可能误判
- 不 refocus，仅设属性：`NODE_TEXT_INPUT_ENABLE_KEYBOARD_ON_FOCUS` 对已聚焦节点不生效，必须 blur→refocus

### 决策 8：OHOS — `Focus()` 已聚焦分支复用 pending refocus 流程

**选择**：`Focus()` 中检查 `is_node_focused_`，若已聚焦则走 `pending_refocus_after_blur_ = true` + `Blur()` 流程。

**理由**：已聚焦状态下直接 `NODE_FOCUS_STATUS=1` 是 no-op，键盘不会弹起。复用 blur→refocus 流程确保焦点迁移稳定、键盘弹起。

### 决策 9：Compose DSL — `pendingFocusNoKeyboard` 标记处理异步焦点

**选择**：`hideKeepFocus()` 在无 `activeView`/`pendingView` 时打 `pendingFocusNoKeyboard` 标记，`startInput` 收到 pendingView 时把默认 `focus()` 替换为 `focusWithoutKeyboard()`。`show()` / `stopInput()` 清理标记。

**理由**：Compose 焦点系统是异步的——`requestFocus()` → `onFocusChanged` 回调 → `startInput()` 之间有延迟。无焦点时 `hideKeepFocus()` 拿不到 view，只能打标记等焦点送达。

### 决策 10：Compose DSL — `inputFocus` 回调加 `if(!hasFocus)` 守卫

**选择**：`CoreTextField` 的 `inputFocus` 回调中 `focusRequester.requestFocus()` 加 `if(!hasFocus)` 守卫；`set(hasFocus)` 移除重复 `focus()` 调用。

**理由**：原生侧 `focusWithoutKeyboard` 已完成获焦，若 `inputFocus` 回调无守卫地回请 Compose 聚焦，会触发 Compose 焦点系统再次下发原生 `focus`，形成自激循环。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| iOS dummy inputView 与业务自定义 inputView 交互异常 | `kr_originalInputView` 备份还原 + tag=99999 精确识别，`css_focus:` / `css_blur:` 均检查 tag 后才清理 |
| OHOS 150ms 时间窗口在极端性能设备上误判 | 窗口仅用于区分 IME 拆卸尾部 blur，超窗口走正常上举路径不会卡死；`pending_refocus_after_blur_` 在 `OnInputFocus` 中防御性复位 |
| iOS 键盘通知过滤过度，遗漏真实键盘事件 | 仅在 dummy 在场（tag==99999）时过滤，普通态完全透传；dummy 清除后通知恢复正常 |
| Compose `pendingFocusNoKeyboard` 标记残留污染后续输入 | `show()` / `stopInput()` 均清理标记；`startInput` 消费后即清零 |
| `blur()` 移除 keepFocus 参数为 BREAKING 变更 | 仅框架内部调用，无业务直接调用 `AutoHeightTextAreaView.blur(keepFocus)` |
| OHOS `KRViewUtil.cpp` 中 `SetArkUIImageSourceSize` 被注释 | 非本次特性改动，需单独确认是否为调试残留 |

## File Changes (by module)

### core
- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/AutoHeightTextAreaView.kt` — 新增 `focusWithoutKeyboard()`，`blur()` 移除 keepFocus 参数

### compose
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/platform/SoftwareKeyboardController.kt` — 新增 `hideKeepFocus()` 接口 + `FOCUS_NO_KEYBOARD` 枚举 + `pendingFocusNoKeyboard` 标记
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/CoreTextField.kt` — `inputFocus` 加 `if(!hasFocus)` 守卫，`set(hasFocus)` 移除重复 `focus()`

### core-render-ios
- `core-render-ios/Extension/Components/KRTextFieldView.m` — dummy inputView 方案、手势委托、键盘通知过滤
- `core-render-ios/Extension/Components/KRTextAreaView.m` — 同上（两文件对齐）

### core-render-android
- `core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/component/KRTextFieldView.kt` — `setFocusWithoutKeyboard()` + `METHOD_FOCUS_WITHOUT_KEYBOARD` 常量

### core-render-ohos
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextFieldView.h` — 新增成员变量和方法声明
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextFieldView.cpp` — `FocusWithoutKeyBoard()` + `ScheduleRefocus()` + `OnInputBlur()` 三分支状态机 + `Focus()` 已聚焦分支
- `core-render-ohos/src/main/cpp/libohos_render/utils/KRViewUtil.h` — `UpdateInputNodeFocusAndKeyBoardStatus()` 声明
- `core-render-ohos/src/main/cpp/libohos_render/utils/KRViewUtil.cpp` — `UpdateInputNodeFocusAndKeyBoardStatus()` 实现

### demo
- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/HideKeyboardTestDemo.kt` — 测试页面
