## 1. core — TextInputState 编辑态归一化

- [x] 1.1 在 `TextInputState` 上新增 `coerceToTextBounds()` 方法，将 selectionStart/End 和 compositionStart/End 裁剪到 `[0, text.length]`
- [x] 1.2 在 `TextInputState.decode()` 返回前调用 `coerceToTextBounds()`，确保 JSON 反序列化后自动归一化
- [x] 1.3 验证 `hasSameEditingState` 保持语义不变（顺序与字段均和旧版一致）

## 2. compose — TextFieldState 内部一致性收口

- [x] 2.1 新增 `setTextAndSelect(text, selection, composition)` 作为 text/selection/composition 统一写入入口，每次写入都执行 `coerceIn(0, text.length)`
- [x] 2.2 新增 `applyBuffer(buffer)` 私有方法，让 `edit{}` 走 `setTextAndSelect` 而非直接写字段
- [x] 2.3 将 `setTextAndPlaceCursorAtEnd`、`clearText`、`updateFromTextField` 改为走 `setTextAndSelect`

## 3. compose — CoreTextField 统一原生事件回流

- [x] 3.1 新增 `toCompositionRangeOrNull()` / `toTextFieldValue()` / `toTextInputState()` 三个私有转换函数
- [x] 3.2 新增 `handleNativeEditingStateChange(nativeState, shouldMarkPendingText)` 统一入口
- [x] 3.3 将 `textInputStateChange` 和 `selectionChange` 回调改为走 `handleNativeEditingStateChange`
- [x] 3.4 将 `textDidChange` fallback 改为从 `lastSyncedTextInputState` 恢复 selection + composition
- [x] 3.5 将 `set(value)` 受控写回改为走 `toTextInputState()` 统一转换

## 4. compose — material3.TextField 完整编辑态入口

- [x] 4.1 新增 `TextField(TextFieldValue)` 重载，参数包含 `onValueChange: (TextFieldValue) -> Unit`
- [x] 4.2 保持原有 `TextField(value: String)` 重载不变，向后兼容

## 5. core-render-android — KRTextFieldView 选区防御

- [x] 5.1 重写 `setSelection(index: Int)`，添加 `coerceIn(0, textLength)` + try-catch
- [x] 5.2 重写 `setSelection(start: Int, stop: Int)`，双参各自合法化 + try-catch + fallback 到单参
- [x] 5.3 验证 Android 端在非法选区传入时不再崩溃

## 6. demo — 验证场景

- [x] 6.1 在 `TextFieldEmojiDemo` 新增 `Material3SelectionCard`，验证 `TextField(TextFieldValue)` 选区插入与替换
- [x] 6.2 补上 `TextFieldValue.replaceSelection` 私有 helper，统一 demo 中的选区替换语义

## 7. 编译验证

- [x] 7.1 KuiklyUI `./gradlew :core:compileDebugKotlinAndroid :compose:compileDebugKotlinAndroid :demo:compileDebugKotlinAndroid` 通过
- [x] 7.2 ComposeOnKuikly `./gradlew :core:compileDebugKotlinAndroid :compose:compileDebugKotlinAndroid :demo:compileDebugKotlinAndroid` 通过

## 8. 代码注释（Review 可读性）

- [x] 8.1 `TextInputState.coerceToTextBounds()` — 标注为编辑态统一归一化入口，关联 CoreTextField
- [x] 8.2 `TextFieldState.setTextAndSelect()` — 标注为唯一写入收口，所有写路径均收敛于此
- [x] 8.3 `CoreTextField` 顶部转换函数块 — 标注 TextInputState ↔ TextFieldValue 映射收口
- [x] 8.4 `CoreTextField.handleNativeEditingStateChange()` — 标注三事件统一处理入口
- [x] 8.5 `CoreTextField` 三事件注册处 — 标注事件分工与 fallback 联动逻辑
- [x] 8.6 `CoreTextField.set(value)` — 标注下发路径的归一化与防回环
- [x] 8.7 `material3.TextField(TextFieldValue)` — 标注完整受控入口与底层链路
- [x] 8.8 `KRTextFieldView.setSelection()` — 标注与 CoreTextField 归一化形成双层保障
