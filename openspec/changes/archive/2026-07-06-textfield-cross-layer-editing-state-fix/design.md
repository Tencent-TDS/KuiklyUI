## Context

Kuikly Compose DSL 的 TextField 编辑态数据（text + selection + composition）在跨层链路中经历以下流转：

```
业务层 (TextFieldState / TextFieldValue)
    ↓ set(value)
Compose DSL (CoreTextField)
    ↓ setTextInputState(TextInputState)
Core 层 (TextInputState 序列化)
    ↓ Bridge JSON
Render 层 (Android/iOS/OHOS 原生 View)
    ↓ 原生回调（三事件）
Compose DSL (onValueChange → TextFieldValue)
    ↓
业务层 (UI 更新)
```

当前每个环节都可能产生 selection/composition 的丢失或越界，具体表现为：

1. **下发侧**：`CoreTextField.set(value)` 将 `TextFieldValue` 手动拼装为 `TextInputState`，无边界合法化
2. **回流侧**：`textInputStateChange`、`selectionChange`、`textDidChange` 三个回调各自独立组装 `TextFieldValue`，同一份原生数据被三条不同路径拼成三个可能不同的结果
3. **防御侧**：Android `KRTextFieldView.setSelection()` 无双参重写，非法索引直通系统 API

### 三个原生事件的关系

`textInputStateChange` 和 `selectionChange` 是互斥的——输入和选区移动不可能在同一时刻发生。二者都可以和 `textDidChange` 共存：

- `textInputStateChange` 发生后几乎必然跟随 `textDidChange`，由 `pendingTextInputStateText` skip 机制去重
- `selectionChange` 发生时文本不变，可以检查 `lastSyncedTextInputState.text == it.text` 来决策是否需要 fallback
- `textDidChange` 是后备兜底——如果前两个事件没有带回过新的 selection/composition，就由它从 `lastSyncedTextInputState` 恢复编辑态。如果 lastSynced 文本也变了、无法匹配，fallback 到 `TextRange(it.text.length)`（文末），保证有文字时不会错误地回退到位置 0

## Goals / Non-Goals

**Goals:**
- 建立编辑态跨层传递的统一归一化入口，保证 text/selection/composition 始终合法
- 统一 CoreTextField 原生事件回流逻辑，消除三事件各自拼装导致的选区丢失
- 补全 Android Render 层双参 setSelection 防御，杜绝非法选区引发崩溃
- 补全 `material3.TextField` 的 `TextFieldValue` 入口，使完整编辑态对业务可见

**Non-Goals:**
- 不新增选区拖拽手柄、富文本编辑、输入法预上屏 UI
- 不修改自研 DSL 的 `TextArea` 路径
- 不在 TextFieldState 上新增便利性 API（如 replaceSelection / moveCursorTo 等），仅保留修复所需的最小集

## Decisions

### Decision 1：在 TextInputState 上增加 coerceToTextBounds() 而非在各调用点分散裁剪

**选择**：在 `TextInputState` 上提供统一的 `coerceToTextBounds()` 方法，并在 `decode()` 和 `CoreTextField` 回流入口处调用。

**理由**：
- 单点归一化，避免每个调用方各自实现裁剪逻辑导致遗漏
- `decode()` 时归一化保证从 JSON 反序列化后即合法
- `coerceToTextBounds()` 内部判断是否真的需要裁剪，不做无谓的 copy

**替代方案考虑**：
- 在各 Render 层裁剪 → 不可行，`core/` 层无 render 依赖
- 只在 CoreTextField 中裁 → 不够，TextInputState 还可能被其他路径构造

### Decision 2：CoreTextField 原生事件统一走 handleNativeEditingStateChange()

**选择**：抽取 `handleNativeEditingStateChange(nativeState, shouldMarkPendingText)` 作为 `textInputStateChange`、`selectionChange` 的统一入口；`textDidChange` fallback 走 `lastSyncedTextInputState` 恢复编辑态。

**理由**：
- 消除三事件各自拼装 `TextFieldValue` 的重复代码
- `textDidChange` fallback 时如果上次同步的 `lastSyncedTextInputState` 文本一致，则复用其 selection/composition，避免掉到 `TextRange.Zero`

**替代方案考虑**：
- 让原生层三事件合并为单一回调 → 需要修改全部 Render 层，跨平台改动量大且不必要

### Decision 3：TextFieldState 内部一致性通过 setTextAndSelect() 收口

**选择**：新增 `setTextAndSelect(text, selection, composition)` 作为统一的写入入口，`edit{}`（通过 `applyBuffer`）、`clearText`、`setTextAndPlaceCursorAtEnd`、`updateFromTextField` 全部走这个路径。

**理由**：
- 所有写入 text/selection/composition 的路径共享同一个 coerceIn 逻辑
- 不影响已有的 `edit{}` API 语义
- `selection` 和 `composition` 参数不再被直接赋值，而是经过 coerceIn 到 `text.length`

**替代方案考虑**：
- 在 each setter 的 `private set` 中做 coerceIn → `mutableStateOf` 代理不支持，且会破坏 Kotlin property delegate 的行为预期

### Decision 4：material3.TextField 增加 TextFieldValue 重载而非替换 String 重载

**选择**：新增一个独立的重载，保留原有 `TextField(value: String, onValueChange: (String) -> Unit)`。

**理由**：
- 向下兼容，现有 String 用法不变
- `TextFieldValue` 重载让需要完整编辑态的业务主动选择

### Decision 5：KRTextFieldView 双参 setSelection + try-catch 防御

**选择**：重写 `setSelection(start, stop)`，coerceIn 到 editableText/text 长度内，外层包裹 try-catch，异常时 fallback 到单参 `setSelection(safeStop)`。

**理由**：
- 双参 setSelection 在历史路径中被直接调用（无防御），是 crash 的直接起点
- try-catch 捕获系统 API 内部可能的异常（如 IndexOutOfBoundsException）

## Risks / Trade-offs

- [Risk] `coerceToTextBounds()` 在 decode 时自动裁剪，可能掩盖 Render 层传回非法值的问题 → [Mitigation] 仅在越界时才 copy 新实例，不改数据时不引入开销；如需排查 Render 层问题可通过日志观察
- [Risk] `textDidChange` fallback 依赖 `lastSyncedTextInputState`，若文本变了、无法复用历史编辑态 → [Mitigation] fallback 到 `TextRange(it.text.length)`（文末），有文字时不会错误地回到位置 0；与 textInputStateChange 互斥的 selectionChange 也会在文本不变时复用 edit 态
- [Trade-off] `handleNativeEditingStateChange` 内调用 `coerceToTextBounds()`，每次原生回流增加一次轻量检查 → 开销可忽略（O(1) 的 Int 比较）

## Migration Plan

无迁移需求，所有改动向后兼容。`setTextAndPlaceCursorAtEnd` 和 `clearText` 行为不变。
