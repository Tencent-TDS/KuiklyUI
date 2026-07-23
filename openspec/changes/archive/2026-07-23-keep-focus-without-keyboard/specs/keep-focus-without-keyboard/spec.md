## ADDED Requirements

### Requirement: focusWithoutKeyboard 原生命令
系统 SHALL 在 iOS / Android / OHOS 三端 render 层提供 `focusWithoutKeyboard` 原生命令，语义为「让输入框获取焦点，但软键盘全程不出现」。该命令为自包含命令——原生侧自行判断当前是否已获焦并统一处理为「获焦 + 无键盘」，调用方无需在命令前后额外下发 `focus` 或 `hide`。

#### Scenario: iOS — 未获焦时调用 focusWithoutKeyboard
- **WHEN** iOS 输入框（UITextField 或 UITextView）未获焦时收到 `focusWithoutKeyboard` 命令
- **THEN** 系统 SHALL 挂载 dummy inputView (tag=99999) 并调用 `becomeFirstResponder`，输入框获得焦点（光标可见），系统键盘 SHALL 不弹出

#### Scenario: iOS — 已获焦时调用 focusWithoutKeyboard
- **WHEN** iOS 输入框已获焦且系统键盘可见时收到 `focusWithoutKeyboard` 命令
- **THEN** 系统 SHALL 挂载 dummy inputView (tag=99999) 并调用 `reloadInputViews`，输入框保持焦点，系统键盘 SHALL 收起

#### Scenario: Android — 调用 focusWithoutKeyboard
- **WHEN** Android 输入框收到 `focusWithoutKeyboard` 命令
- **THEN** 系统 SHALL 调用 `requestFocus()` 获取焦点，并在下一帧调用 `hideSoftInputFromWindow()` 收起键盘；系统 SHALL NOT 调用 `showSoftInput`

#### Scenario: OHOS — 调用 focusWithoutKeyboard
- **WHEN** OHOS 输入框收到 `focusWithoutKeyboard` 命令
- **THEN** 系统 SHALL 设置 `NODE_TEXT_INPUT_ENABLE_KEYBOARD_ON_FOCUS=0`，执行 Blur 后在下一帧 refocus（`NODE_FOCUS_STATUS=1`），输入框获得焦点且系统键盘 SHALL 不弹出

#### Scenario: OHOS — IME 拆卸尾部 blur 不上抛
- **WHEN** OHOS `focusWithoutKeyboard` 引发的 refocus 后 150ms 内收到 IME 拆卸的尾部 blur 事件
- **THEN** 系统 SHALL 抑制该 blur 事件不上抛给业务层，并执行补充 refocus 确保焦点保持

#### Scenario: OHOS — 真实失焦正常上抛
- **WHEN** OHOS 输入框在 `focusWithoutKeyboard` 流程完成且超过 150ms 窗口后收到 blur 事件
- **THEN** 系统 SHALL 正常上抛 blur 事件给业务层

### Requirement: iOS 业务自定义 inputView 保护
iOS 端在执行 `focusWithoutKeyboard` 挂载 dummy inputView 前，SHALL 检查当前 `inputView` 是否为业务自定义（非 nil 且 tag ≠ 99999），若是则 SHALL 备份到 `kr_originalInputView` 属性。恢复系统键盘（`focus` 命令或 `blur` 命令）时 SHALL 还原 `kr_originalInputView`。

#### Scenario: iOS — 业务有自定义 inputView 时执行 focusWithoutKeyboard
- **WHEN** iOS 输入框已设置业务自定义 inputView（如日期选择器），收到 `focusWithoutKeyboard` 命令
- **THEN** 系统 SHALL 将业务 inputView 备份到 `kr_originalInputView`，然后挂载 dummy inputView 获焦

#### Scenario: iOS — 从 focusWithoutKeyboard 恢复系统键盘
- **WHEN** iOS 输入框处于 dummy inputView 在场状态，收到 `focus` 命令
- **THEN** 系统 SHALL 清除 dummy inputView，还原 `kr_originalInputView`，调用 `reloadInputViews` 恢复系统键盘

#### Scenario: iOS — blur 清除 dummy inputView
- **WHEN** iOS 输入框处于 dummy inputView 在场状态，收到 `blur` 命令
- **THEN** 系统 SHALL 清除 dummy inputView，还原 `kr_originalInputView`，然后 `resignFirstResponder` 失焦

### Requirement: iOS 键盘通知过滤
iOS 端在 dummy inputView (tag=99999) 在场时，SHALL 过滤 `keyboardWillShow` 和 `keyboardWillHide` 系统通知，不转发给业务层。

#### Scenario: iOS — dummy 在场时键盘通知被过滤
- **WHEN** iOS 输入框的 `inputView.tag == 99999` 时系统派发 `keyboardWillShow` 或 `keyboardWillHide` 通知
- **THEN** 系统 SHALL 不调用 `css_keyboardHeightChange` 回调，不将键盘高度变化转发给业务层

#### Scenario: iOS — dummy 不在场时键盘通知正常转发
- **WHEN** iOS 输入框的 `inputView` 为 nil 或 tag ≠ 99999 时系统派发键盘通知
- **THEN** 系统 SHALL 正常调用 `css_keyboardHeightChange` 回调转发键盘高度变化

### Requirement: iOS 免键盘获焦态点击恢复键盘
iOS 端在 dummy inputView 在场时，SHALL 注册手势委托（`UIGestureRecognizerDelegate`），仅在该状态下识别点击手势，用户点击输入框时恢复系统键盘。普通态 SHALL 完全交由 UITextField / UITextView 内部手势处理。

#### Scenario: iOS — dummy 在场时点击恢复键盘
- **WHEN** iOS 输入框处于 dummy inputView 在场状态，用户点击输入框
- **THEN** 系统 SHALL 清除 dummy inputView，还原 `kr_originalInputView`，恢复系统键盘

#### Scenario: iOS — 普通态手势委托不干扰
- **WHEN** iOS 输入框不在 dummy inputView 状态，用户点击输入框
- **THEN** 自定义 tap 手势 SHALL 不参与识别，完全由 UITextField / UITextView 内部手势处理获焦和光标定位

### Requirement: Compose DSL hideKeepFocus 接口
Compose DSL SHALL 在 `SoftwareKeyboardController` 接口上提供 `hideKeepFocus()` 方法，语义为「收起软键盘但保留/获取焦点」。

#### Scenario: Compose — 已有焦点时调用 hideKeepFocus
- **WHEN** 输入框已获焦（`activeView` 存在），业务调用 `keyboardController.hideKeepFocus()`
- **THEN** 系统 SHALL 向该 view 下发 `FOCUS_NO_KEYBOARD` 命令，调用 `focusWithoutKeyboard()`，键盘收起且焦点保持

#### Scenario: Compose — 无焦点时调用 hideKeepFocus
- **WHEN** 输入框未获焦（`activeView` 和 `pendingView` 均为 null），业务先调用 `focusRequester.requestFocus()` 再调用 `hideKeepFocus()`
- **THEN** 系统 SHALL 设置 `pendingFocusNoKeyboard` 标记；当焦点异步送达触发 `startInput` 时，SHALL 把默认 `focus()` 替换为 `focusWithoutKeyboard()`，键盘全程不出现

#### Scenario: Compose — show 清除 pendingFocusNoKeyboard 标记
- **WHEN** `pendingFocusNoKeyboard` 标记为 true 时，业务调用 `keyboardController.show()`
- **THEN** 系统 SHALL 清除 `pendingFocusNoKeyboard` 标记，并正常下发 `SHOW_KEYBOARD` 命令

#### Scenario: Compose — stopInput 清除 pendingFocusNoKeyboard 标记
- **WHEN** `pendingFocusNoKeyboard` 标记为 true 时，输入框失焦触发 `stopInput`
- **THEN** 系统 SHALL 清除 `pendingFocusNoKeyboard` 标记，避免残留污染后续输入框

### Requirement: Compose inputFocus 回调去重守卫
Compose DSL 的 `CoreTextField` 在 `inputFocus` 回调中 SHALL 仅在当前未聚焦时回请 Compose 焦点（`if(!hasFocus) focusRequester.requestFocus()`），避免「原生获焦 → 回请 Compose 聚焦 → 再触发原生 focus」的自激循环。

#### Scenario: Compose — 已聚焦时 inputFocus 回调不回请
- **WHEN** `CoreTextField` 已处于聚焦状态（`hasFocus == true`），原生侧触发 `inputFocus` 回调
- **THEN** 系统 SHALL NOT 调用 `focusRequester.requestFocus()`，避免自激循环

#### Scenario: Compose — 未聚焦时 inputFocus 回调正常回请
- **WHEN** `CoreTextField` 未聚焦（`hasFocus == false`），原生侧触发 `inputFocus` 回调
- **THEN** 系统 SHALL 调用 `focusRequester.requestFocus()` 回请 Compose 焦点

### Requirement: Core 层 focusWithoutKeyboard 方法
`AutoHeightTextAreaView` SHALL 提供 `focusWithoutKeyboard()` 方法，通过 `callMethod("focusWithoutKeyboard", "")` 桥接到各端原生实现。

#### Scenario: Core — focusWithoutKeyboard 桥接到原生
- **WHEN** 业务调用 `AutoHeightTextAreaView.focusWithoutKeyboard()`
- **THEN** 系统 SHALL 在 renderView 加载完成后调用 `renderView.callMethod("focusWithoutKeyboard", "")`

### Requirement: OHOS Focus 已聚焦分支复用 refocus 流程
OHOS 端 `Focus()` 方法在节点已聚焦（`is_node_focused_ == true`）时，SHALL 复用 `pending_refocus_after_blur_` + `Blur()` 的 refocus 流程，确保焦点迁移稳定且键盘弹起。

#### Scenario: OHOS — 已聚焦时调用 Focus
- **WHEN** OHOS 输入框已聚焦（`is_node_focused_ == true`），收到 `focus` 命令
- **THEN** 系统 SHALL 设置 `pending_refocus_after_blur_ = true` 并调用 `Blur()`，由 `OnInputBlur` 触发 refocus 确保键盘弹起

### Requirement: OHOS UpdateInputNodeFocusAndKeyBoardStatus 工具函数
`KRViewUtil` SHALL 提供 `UpdateInputNodeFocusAndKeyBoardStatus(node, status)` 函数，封装写入 `NODE_TEXT_INPUT_ENABLE_KEYBOARD_ON_FOCUS` 属性。

#### Scenario: OHOS — 写入 ENABLE_KEYBOARD_ON_FOCUS 属性
- **WHEN** 调用 `UpdateInputNodeFocusAndKeyBoardStatus(node, 0)`
- **THEN** 系统 SHALL 设置节点的 `NODE_TEXT_INPUT_ENABLE_KEYBOARD_ON_FOCUS` 属性为 0，关闭获焦即弹键盘
