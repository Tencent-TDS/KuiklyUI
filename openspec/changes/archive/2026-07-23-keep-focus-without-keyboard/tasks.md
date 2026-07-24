## 1. Core 层 — AutoHeightTextAreaView

- [x] 1.1 新增 `focusWithoutKeyboard()` 方法，通过 `callMethod("focusWithoutKeyboard", "")` 桥接到原生
- [x] 1.2 `blur()` 移除 `keepFocus` 参数，恢复纯失焦语义（仅调用 `callMethod("blur", "")`）
- [x] 1.3 编译验证：`./gradlew :core:compileDebugKotlinAndroid`

## 2. Compose DSL 层 — SoftwareKeyboardController

- [x] 2.1 `SoftwareKeyboardController` 接口新增 `hideKeepFocus()` 方法及文档注释
- [x] 2.2 `PendingAction` 枚举新增 `FOCUS_NO_KEYBOARD` 值
- [x] 2.3 新增 `pendingFocusNoKeyboard` 标记成员变量
- [x] 2.4 `show()` 中清理 `pendingFocusNoKeyboard` 标记
- [x] 2.5 `stopInput()` 中清理 `pendingFocusNoKeyboard` 标记
- [x] 2.6 实现 `hideKeepFocus()`：已有 view 时下发 `FOCUS_NO_KEYBOARD` 命令；无 view 时打标记
- [x] 2.7 `startInput` 分支处理 `pendingFocusNoKeyboard` 标记：替换 `focus()` 为 `focusWithoutKeyboard()`
- [x] 2.8 命令分发新增 `FOCUS_NO_KEYBOARD` 分支：调用 `focusWithoutKeyboard()` 并同步 `activeView`

## 3. Compose DSL 层 — CoreTextField

- [x] 3.1 `inputFocus` 回调加 `if(!hasFocus)` 守卫，避免自激循环
- [x] 3.2 `set(hasFocus)` 移除重复 `focus()` 调用，仅保留注释说明
- [x] 3.3 编译验证：`./gradlew :compose:compileDebugKotlinAndroid`

## 4. iOS 端 — KRTextFieldView.m + KRTextAreaView.m（两文件对齐）

- [x] 4.1 类声明追加 `UIGestureRecognizerDelegate` 协议
- [x] 4.2 新增 `kr_originalInputView` 属性声明
- [x] 4.3 `init` 中添加 `UITapGestureRecognizer` + 设置 delegate
- [x] 4.4 实现 `p_handleRestoreKeyboardTap:` — dummy 在场时调 `css_focus:` 恢复键盘
- [x] 4.5 实现 `gestureRecognizer:shouldReceiveTouch:` — 仅 dummy 在场时返回 YES
- [x] 4.6 重写 `css_focus:` — 清除 dummy + 还原 `kr_originalInputView` + `becomeFirstResponder`；已 firstResponder 但刚清 dummy 时补发 `inputFocus`
- [x] 4.7 重写 `css_blur:` — 异步派发，清除 dummy + 还原 `kr_originalInputView` + `resignFirstResponder`
- [x] 4.8 新增 `css_focusWithoutKeyboard:` — 未获焦挂 dummy + `becomeFirstResponder`；已获焦挂 dummy + `reloadInputViews`
- [x] 4.9 `keyboardWillShow:` 通知在 dummy 在场时过滤 return
- [x] 4.10 `keyboardWillHide:` 通知在 dummy 在场时过滤 return
- [x] 4.11 macOS 分支保持 `becomeFirstResponder` / `resignFirstResponder` 原始逻辑（不挂 dummy）

## 5. Android 端 — KRTextFieldView.kt

- [x] 5.1 新增 `METHOD_FOCUS_WITHOUT_KEYBOARD = "focusWithoutKeyboard"` 常量
- [x] 5.2 `CallMethod` 分发新增 `METHOD_FOCUS_WITHOUT_KEYBOARD -> setFocusWithoutKeyboard()` 分支
- [x] 5.3 实现 `setFocusWithoutKeyboard()` — `requestFocus()` + `post { hideSoftInputFromWindow() }`，不调 `showSoftInput`
- [x] 5.4 编译验证：`./gradlew :androidApp:assembleDebug`

## 6. OHOS 端 — KRTextFieldView.h / .cpp + KRViewUtil.h / .cpp

- [x] 6.1 `KRTextFieldView.h` 新增 `#include <chrono>`
- [x] 6.2 `KRTextFieldView.h` 新增成员变量：`pending_refocus_after_blur_`、`awaiting_teardown_blur_`、`last_refocus_ts_`、`is_node_focused_`
- [x] 6.3 `KRTextFieldView.h` 新增方法声明：`FocusWithoutKeyBoard()`、`ScheduleRefocus(bool)`、`UpdateInputNodeFocusAndKeyBoardStatus(int)`
- [x] 6.4 `KRTextFieldView.cpp` 新增 `kMethodFocusWithoutKeyboard` 常量
- [x] 6.5 `KRTextFieldView.cpp` `CallMethod` 新增 `focusWithoutKeyboard` 分支
- [x] 6.6 实现 `FocusWithoutKeyBoard()` — 设 `ENABLE_KEYBOARD_ON_FOCUS=0` + `pending_refocus_after_blur_=true` + `Blur()`
- [x] 6.7 实现 `ScheduleRefocus(arm)` — next-loop 调度 `UpdateInputNodeFocusStatus(1)`，arm 控制 `awaiting_teardown_blur_`
- [x] 6.8 `OnInputFocus` 维护 `is_node_focused_=true` + 防御性复位 `pending_refocus_after_blur_`
- [x] 6.9 `OnInputBlur` 实现三分支状态机：pending refocus / awaiting teardown <150ms / 正常上抛
- [x] 6.10 `Focus()` 已聚焦分支复用 pending refocus 流程
- [x] 6.11 `KRViewUtil.h` 新增 `UpdateInputNodeFocusAndKeyBoardStatus()` 声明
- [x] 6.12 `KRViewUtil.cpp` 实现 `UpdateInputNodeFocusAndKeyBoardStatus()` — 写入 `NODE_TEXT_INPUT_ENABLE_KEYBOARD_ON_FOCUS`
- [x] 6.13 编译验证：`./2.0_ohos_demo_build.sh`

## 7. Demo — HideKeyboardTestDemo

- [x] 7.1 新增 `HideKeyboardTestDemo.kt` 测试页面，包含输入框 + hide() / hideKeepFocus() / show() 三个按钮
- [x] 7.2 通过 `@Page("223399")` 注解自动注册 Demo 页面入口

## 8. 平台测试（iOS / Android / OHOS 三端均已验证）

> 测试页面：`HideKeyboardTestDemo`（Page ID: 223399）
> 测试方法：手动点击输入框获焦（键盘弹起），再依次点击按钮，观察键盘显隐和焦点状态

### 链路 1：手动获焦 → hide

- [x] 8.1 iOS — 点击输入框获焦（键盘弹起）→ 点击 `hide()` → 验证键盘收起且焦点丢失
- [x] 8.2 Android — 点击输入框获焦（键盘弹起）→ 点击 `hide()` → 验证键盘收起且焦点丢失
- [x] 8.3 OHOS — 点击输入框获焦（键盘弹起）→ 点击 `hide()` → 验证键盘收起且焦点丢失

### 链路 2：手动获焦 → hideKeepFocus → focus

- [x] 8.4 iOS — 点击输入框获焦（键盘弹起）→ 点击 `hideKeepFocus()`（键盘收起、光标保留）→ 点击 `show()` 或点击输入框 → 验证键盘重新弹起
- [x] 8.5 Android — 点击输入框获焦（键盘弹起）→ 点击 `hideKeepFocus()`（键盘收起、光标保留）→ 点击 `show()` 或点击输入框 → 验证键盘重新弹起
- [x] 8.6 OHOS — 点击输入框获焦（键盘弹起）→ 点击 `hideKeepFocus()`（键盘收起、光标保留）→ 点击 `show()` 或点击输入框 → 验证键盘重新弹起

### 链路 3：手动获焦 → hideKeepFocus → hide

- [x] 8.7 iOS — 点击输入框获焦（键盘弹起）→ 点击 `hideKeepFocus()`（键盘收起、光标保留）→ 点击 `hide()` → 验证焦点丢失
- [x] 8.8 Android — 点击输入框获焦（键盘弹起）→ 点击 `hideKeepFocus()`（键盘收起、光标保留）→ 点击 `hide()` → 验证焦点丢失
- [x] 8.9 OHOS — 点击输入框获焦（键盘弹起）→ 点击 `hideKeepFocus()`（键盘收起、光标保留）→ 点击 `hide()` → 验证焦点丢失

## 9. 清理与收尾

- [x] 9.1 确认 `KRViewUtil.cpp` 中 `SetArkUIImageSourceSize` 的 `setAttribute` 注释是否为调试残留，若是则恢复
- [x] 9.2 确认 `iosApp/iosApp.xcodeproj/project.pbxproj` 改动为 Demo 注册，无多余配置
- [x] 9.3 确认 `ohosApp/build-profile.json5` 等构建配置改动合理
