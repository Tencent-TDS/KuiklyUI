## 1. Fix Implementation (Compose DSL)

- [x] 1.1 在 `CoreTextField.kt` 的 `textDidChange` handler 中，将 `onValueChange(TextFieldValue(it.text))` 替换为继承 `lastSyncedTextInputState` 的 selection 的版本
- [x] 1.2 编译验证：`./gradlew :compose:compileDebugKotlinAndroid` 通过

## 2. iOS 回归验证（需真机）

- [x] 2.1 在 iOS 真机上打开 Compose TextField Demo 页面，输入中英文混合文本，验证光标不再跳动
- [x] 2.2 验证拼音组词（输入 "f你好"、"发反反复复"）光标稳定在末尾
- [x] 2.3 验证多行 BasicTextField（maxLines > 1）同样正常

## 3. 兼容性验证（需真机）

- [x] 3.1 验证普通英文输入不出现 regression
- [x] 3.2 验证 input type=password、email、number 等键盘类型正常
- [x] 3.3 验证粘贴、删除操作光标正常
- [x] 3.4 验证 maxLength 截断场景下光标行为正常
