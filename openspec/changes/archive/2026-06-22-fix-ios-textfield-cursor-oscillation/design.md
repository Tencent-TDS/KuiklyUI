## Context

### 问题现象

Compose TextField 在 iOS 上输入时（尤其中文拼音/日文假名等含 composition 的场景），光标会在 index 0 与当前输入文字末尾之间**反复跳动**，每输入一个字符抖动一次。commit `bacbfeae` 引入了该回归。

### 当前的双向同步架构

Compose TextField 的 iOS 侧存在三条原生 → Compose 的事件回调通道：

```
iOS (UITextField / UITextView)
    │
    ├─ selectionChange        ← textFieldDidChangeSelection:
    │    带完整 payload (text + selectionStart/End)
    │
    ├─ textInputStateChange   ← p_notifyTextInputStateChangeIfNeeded()
    │    带完整 payload
    │
    └─ textDidChange          ← onTextFeildTextChanged: / textViewDidChange:
         仅带 text + length，没有 selection
```

Compose 侧的 `set(value)` 更新区块负责**反向同步**，使用 `shouldSyncToNative` 防环逻辑：

```kotlin
val shouldSyncToNative = !isProcessingNativeEvent ||
    !(lastSyncedTextInputState?.hasSameEditingState(incomingTextInputState) ?: false)
```

### 根因：textDidChange 的 selection 缺失

在拼音输入期间：

1. `markedTextRange` 非空，iOS 侧 `selectionChange` 带正确选区（如 `sel=3`）回调 Compose
2. Compose 标记 `isProcessingNativeEvent=true`，更新 `lastSyncedTextInputState`，调用 `onValueChange` → 重组 → `set(value)` 判定无需同步 → 重置 `isProcessingNativeEvent=false`
3. **同一帧末尾**，iOS 侧 `textDidChange` 回调 Compose，但不带 selection
4. Compose 的 `textDidChange` handler 调用 `onValueChange(TextFieldValue(it.text))`，默认 `selection=TextRange.Zero(0,0)`
5. `set(value)` 收到 incoming `sel=[0,0]`，而 `lastSyncedTextInputState.sel=[3,3]`，`hasSameEditingState=false`
6. `isProcessingNativeEvent=false` → `shouldSyncToNative=true` → **错误地将 `sel=0` 写回原生**

## Goals / Non-Goals

**Goals:**
- 消除 Compose TextField 在 iOS 上每帧光标跳动的现象
- 最小修改原则：仅改动 Compose 层单文件
- 不修改 iOS 原生侧代码
- 不修改自研 DSL 路径

**Non-Goals:**
- 不改变 `textDidChange` 的事件语义（仍保持 text-only 语义）
- 不透传 `isProcessingNativeEvent` 到 `textDidChange` 处理器中，避免增加跨事件时序耦合
- 不改动 Android 平台的 TextField 行为

## Decisions

### 决策 1：在消费者最近的位置补全 selection，而非在源头

**方案对比：**

| 方案 | 改动点 | 表述 | 选型 |
|------|--------|------|------|
| A: 源头补全（iOS 侧） | KRTextFieldView.m / KRTextAreaView.m，在 `textDidChange` payload 中加入 selection | 需要修改两处 .m 文件，且 iOS 侧获取 selection 需要调用 `offsetFromPosition:`，增加隐式依赖 | ❌ |
| B: 消费者层补全（Compose 侧） | CoreTextField.kt 的 `textDidChange` handler 一行修改 | 单文件、单点、与 `lastSyncedTextInputState` 已有架构天然契合 | ✅ |

**选择 B，理由：**
- `lastSyncedTextInputState` 在这个时间点已经被同一帧内前序的 `selectionChange` 或 `textInputStateChange` 正确更新过
- `textDidChange` 的 text-only payload 语义不变，iOS 侧无需感知 Compose 层架构
- 后续若 Android 出现类似问题，修复入口也相同

### 决策 2：使用 `lastSyncedTextInputState` 的 selection，而非直接查询原生

**方案对比：**

| 方案 | 行为 | 风险 |
|------|------|------|
| 从 `lastSyncedTextInputState` 继承 | 复用同帧内已知正确的 selection | 低——已在 `selectionChange` handler 中快照 |
| 直接向原生查询（`css_getCursorIndex`） | 引入异步跨层查询 | 中——异步调用可能不在同一帧内完成，且增加额外 Bridge 消息开销 |

### 决策 3：仅当文本完全一致时才继承 selection

```kotlin
val preservedSelection = lastSyncedTextInputState?.let { state ->
    if (state.text == it.text) {
        TextRange(state.selectionStart, state.selectionEnd)
    } else {          // ← 文本变了，不能用旧选区
        TextRange.Zero
    }
} ?: TextRange.Zero
```

当 `textDidChange` 的文本与 `lastSyncedTextInputState` 不同时——说明 text 确实发生了实质性变化→ 此时 `TextRange.Zero` 是正确的（文本变了，旧选区已失效）。

## 架构图：修复前后对比

### 修复前（有 Bug）

```
用户输入 "f"
    │
    ▼
selectionChange: sel=[1,1] → lastSynced={text='f你好', sel=1}
    │                       → isProcessingNativeEvent=true
    │                       → onValueChange → 重组 → set(value) 判定: ⛔不写入
    │                       → isProcessingNativeEvent=false
    │
    ▼
textDidChange: text='f你好'  ← 没有 sel
    │
    ▼
onValueChange(TextFieldValue('f你好', sel=0))   ← sel 默认成了 0
    │
    ▼
set(value): incoming={text='f你好', sel=0}, lastSynced={text='f你好', sel=1}
    → hasSameEditingState = false  (sel 不同)
    → isProcessingNativeEvent = false
    → shouldSyncToNative = true   ← 错误决策!
    → setTextInputState(sel=0)    ← 光标被强制跳到 0
```

### 修复后

```
用户输入 "f"
    │
    ▼
selectionChange: sel=[1,1] → lastSynced={text='f你好', sel=1}  ✅
    │
    ▼
textDidChange: text='f你好'  ← 没有 sel
    │
    ▼
preservedSelection = lastSyncedTextInputState?.let {
    if (it.text == 'f你好') TextRange(1, 1) else TextRange.Zero
}  = TextRange(1, 1)   ← 正确继承

onValueChange(TextFieldValue('f你好', sel=1))  ✅
    │
    ▼
set(value): incoming={text='f你好', sel=1}, lastSynced={text='f你好', sel=1}
    → hasSameEditingState = true
    → shouldSyncToNative = false   ← 正确！
    → 不写回原生，光标不动击
```

## File Changes

| 模块 | 文件 | 改动类型 |
|------|------|----------|
| `compose` | `compose/src/commonMain/kotlin/.../CoreTextField.kt` | 修改 `textDidChange` handler 中 `onValueChange` 调用，补全 selection |

仅此 1 个文件改动。iOS 原生侧（`core-render-ios/Extension/Components/`）和自研 DSL Core 侧（`core/`）无需修改。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 同帧内 `textDidChange` 先于 `selectionChange` 到达时，`lastSyncedTextInputState` 可能为 null 或持有旧帧 state | `?: TextRange.Zero` 兜底行为与修复前一致，不会更差 |
| `lastSyncedTextInputState.text == it.text` 判断在特殊字符（Emoji 等）宽度计算后有 false 匹配 | 成立——Compose 侧 `textDidChange` 收到的 text 就是 iOS 侧 `css_textDidChange` 回调的值，两者基于同一 `self.text`，不会出现不一致 |
| 非 composition 场景下 `textDidChange` 先到，`selectionChange` 后到 | 非 composition 时 `pendingTextInputStateText == it.text`，textDidChange 被 `shouldIgnoreFallback = true` 跳过，进入不到修复逻辑 |
