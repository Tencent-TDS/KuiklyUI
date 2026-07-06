## Why

TextField 的编辑态（text + selection + composition）在 Kuikly 的跨层链路中缺乏统一归一化和转换闭环，导致业务通过 `TextFieldState` / `TextFieldValue` 下发的选区信息在原生回流时丢失，光标始终出现在文本末尾。iOS 侧业务代码为规避此问题自行补充光标修正逻辑，进一步触发了 Android 端 `KRTextFieldView.setSelection()` 因非法索引导致的崩溃。根因是 TextFieldState、TextInputState、CoreTextField 在编辑态传递上存在能力缺失。

## What Changes

- **TextInputState 增加边界合法化**：新增 `coerceToTextBounds()`，在 decode 和跨层传递入口处自动裁剪越界 selection/composition
- **CoreTextField 统一编辑态转换**：新增 `toTextFieldValue()` / `toTextInputState()` / `toCompositionRangeOrNull()` / `handleNativeEditingStateChange()`，将原生三个事件回调收口为同一套归一化逻辑
- **TextFieldState 内部一致性收口**：新增 `setTextAndSelect()`，让 `edit{}`、`clearText`、`setTextAndPlaceCursorAtEnd`、`updateFromTextField` 全部走同一个 coerceIn 路径
- **material3.TextField 增加完整编辑态入口**：新增 `TextField(TextFieldValue)` 重载，使 selection/composition 受控写法对业务可见
- **Android Render 防御**：`KRTextFieldView` 重写双参 `setSelection(start, stop)` 并增加 try-catch 兜底，防止非法选区引发崩溃

## Capabilities

### New Capabilities
- `editing-state-normalization`: TextInputState 跨层编辑态的边界合法化与统一归一化入口
- `core-text-field-event-unification`: CoreTextField 原生编辑事件（textInputStateChange / selectionChange / textDidChange）的统一回流与转换
- `android-selection-defense`: Android KRTextFieldView 的选区设置安全防御

### Modified Capabilities
- `text-field-state`: TextFieldState 新增 `setTextAndSelect()` 内部收口方法，`edit{}` / `clearText` / `setTextAndPlaceCursorAtEnd` 路径统一走 coerceIn
- `material3-text-field`: Material3 TextField 新增 `TextFieldValue` 重载入口

## Impact

- **Modules**: `core/` (TextInputState)、`compose/` (TextFieldState, CoreTextField, TextField)、`core-render-android/` (KRTextFieldView)、`demo/` (TextFieldEmojiDemo)
- **Platforms**: 全平台受益（Android crash 直接修复，iOS 光标丢失问题修复，OHOS/Web 编辑态一致性提升）
- **Breaking**: 无。`setTextAndPlaceCursorAtEnd`、`clearText` 行为不变，底层仅增加 coerceIn 保护
- **Non-goals**: 不在此改动中新增选区拖拽手柄、富文本编辑、输入法预上屏 UI 等功能；不修改自研 DSL 的 `TextArea` 路径
