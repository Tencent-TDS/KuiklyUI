# 需求文档

## 引言

本文档针对 Compose DSL 中 `BasicTextWithNoInlinContent` 组件的 **applyTextStyle** 相关耗时进行深入分析，并提出优化方案。

### 问题背景

根据 iOS Profile 日志分析，在 900 个 Text 组件场景下，`BasicTextWithNoInlinContent` 的 `ReusableComposeNode` 更新回调总耗时约 **36ms**，其中：
- `applyTextStyle` 调用耗时 **21ms**（58.3%）
- `applyMaxLines` 调用耗时 **7ms**（19.4%）
- `text()` 设置耗时 **3ms**（8.3%）
- `applyOverflow` 调用耗时 **3ms**（8.3%）
- `applySoftWrap` 调用耗时 **2ms**（5.6%）

### 核心问题

`applyTextStyle` 函数在 **每次创建 Text 组件时都会无条件设置所有文本样式属性**，即使这些属性使用的是默认值。这导致：

1. **大量冗余的桥接调用**：每个文本组件都要设置 fontWeight、fontStyle、textAlign、textIndent、shadow 等属性
2. **每次属性设置都触发 Native 桥接**：`setProp` → `BridgeManager.callNativeMethod` → OC 原生调用
3. **对比 Kuikly DSL**：Kuikly DSL 只在用户显式设置属性时才触发桥接调用

---

## 需求

### 需求 1：减少默认值属性的冗余设置

**用户故事：** 作为一名 Kuikly Compose DSL 开发者，我希望 Text 组件在首次创建时仅设置非默认值的样式属性，以便减少不必要的 Native 桥接调用开销。

#### 验收标准

1. WHEN Text 组件首次创建 AND fontWeight 未指定（为 null 或默认值） THEN 系统 SHALL 跳过 `fontWeightNormal()` 的调用
2. WHEN Text 组件首次创建 AND fontStyle 未指定（为 null 或默认值） THEN 系统 SHALL 跳过 `fontStyleNormal()` 的调用
3. WHEN Text 组件首次创建 AND textAlign 为默认值（TextAlign.Start 或 null） THEN 系统 SHALL 跳过 `textAlignLeft()` 的调用
4. WHEN Text 组件首次创建 AND textIndent 为默认值（无缩进） THEN 系统 SHALL 跳过 `firstLineHeadIndent()` 的调用
5. WHEN Text 组件首次创建 AND shadow 为 null THEN 系统 SHALL 跳过 `textShadow()` 的调用
6. WHEN Text 组件首次创建 AND letterSpacing 未指定 THEN 系统 SHALL 跳过 `letterSpacing()` 的调用

---

### 需求 2：优化文本样式的增量更新机制

**用户故事：** 作为一名 Kuikly Compose DSL 开发者，我希望 Text 组件在样式更新时只设置发生变化的属性，以便减少重复的 Native 桥接调用。

#### 验收标准

1. WHEN TextStyle 发生变化 AND 只有 color 属性改变 THEN 系统 SHALL 仅调用 `color()` 方法而不重新设置其他属性
2. WHEN TextStyle 发生变化 AND 只有 fontSize 属性改变 THEN 系统 SHALL 仅调用 `fontSize()` 方法而不重新设置其他属性
3. IF 当前样式与上次应用的样式相同 THEN 系统 SHALL 跳过整个 `applyTextStyle` 调用

---

### 需求 3：合并多个属性设置为批量调用

**用户故事：** 作为一名 Kuikly Compose DSL 开发者，我希望多个文本属性能够批量设置到 Native 层，以便减少 Kotlin/Native 与 OC 之间的桥接调用次数。

#### 验收标准

1. WHEN Text 组件首次创建 AND 需要设置多个样式属性 THEN 系统 SHALL 将这些属性打包为一个批量调用发送到 Native 层
2. IF 当前实现无法支持批量调用 THEN 系统 SHALL 提供配置选项允许开发者在性能敏感场景下启用批量模式

---

### 需求 4：避免 ReusableComposeNode 中的重复样式应用

**用户故事：** 作为一名 Kuikly Compose DSL 开发者，我希望 `ReusableComposeNode` 的 update 块中不会重复应用相同的样式，以便减少首次渲染的耗时。

#### 验收标准

1. WHEN ReusableComposeNode 创建 AND style 参数被设置 THEN 系统 SHALL 确保 `applyTextStyle` 只被调用一次
2. WHEN ReusableComposeNode 创建 AND softWrap/overflow/maxLines 参数被设置 THEN 系统 SHALL 确保这些属性的设置不与 `applyTextStyle` 中的设置重复

**当前问题分析**：

在 `BasicTextWithNoInlinContent` 的 `ReusableComposeNode` update 块中，存在以下重复设置：

```kotlin
// set(style) 中会调用 applyTextStyle，其中包含 applyTextAlign
set(style) {
    withTextView {
        applyTextStyle(finalStyle, density)  // 内部调用了 applyTextAlign
    }
}

// 后续又单独设置了 overflow，而 applyOverflow 也会设置 textOverFlow 属性
set(overflow) {
    if (softWrap) {
        withTextView {
            applyOverflow(overflow)  // 可能与 applySoftWrap 设置冲突
        }
    }
}

// applySoftWrap 也会设置 textOverFlow 属性
set(softWrap) {
    withTextView {
        applySoftWrap(softWrap)  // 内部调用 textOverFlowWordWrapping() 或 textOverFlowClip()
    }
}
```

---

### 需求 5：优化 applyTextStyle 中各子函数的条件判断

**用户故事：** 作为一名 Kuikly Compose DSL 开发者，我希望 `applyTextStyle` 的各个子函数能够在内部判断是否需要设置属性，以便在源头避免不必要的桥接调用。

#### 验收标准

1. WHEN `applyFontWeight(null)` 被调用 THEN 系统 SHALL 直接返回而不调用任何 Native 方法
2. WHEN `applyFontStyle(null)` 被调用 THEN 系统 SHALL 直接返回而不调用任何 Native 方法
3. WHEN `applyTextAlign(null)` 或 `applyTextAlign(TextAlign.Unspecified)` 被调用 THEN 系统 SHALL 直接返回而不调用任何 Native 方法
4. WHEN `applyTextIndent(null)` 或 `applyTextIndent(TextIndent.None)` 被调用 THEN 系统 SHALL 直接返回而不调用任何 Native 方法
5. WHEN `applyShadow(null)` 被调用 THEN 系统 SHALL 直接返回而不调用任何 Native 方法

---

## 性能瓶颈详细分析

### Profile 数据解读

根据 iOSProfileLog2.md 的数据，`applyTextStyle` (21ms) 的耗时分布如下：

| 子函数 | 耗时 | 调用路径 |
|--------|------|----------|
| `applyTextIndent` → `firstLineHeadIndent()` | 4ms | `setProp` → `BridgeManager.callNativeMethod` → Native |
| `color()` | 4ms | `setProp` → `BridgeManager.callNativeMethod` → Native |
| `applyFontStyle` → `fontStyleNormal()` | 3ms | `setProp` → `BridgeManager.callNativeMethod` → Native |
| `applyTextAlign` → `textAlignLeft()` | 3ms | `setProp` → `BridgeManager.callNativeMethod` → Native |
| `applyShadow` → `textShadow()` | 2ms | `setProp` → `BridgeManager.callNativeMethod` → Native |
| `letterSpacing()` | 2ms | `setProp` → `BridgeManager.callNativeMethod` → Native |
| `applyFontWeight` → `fontWeightNormal()` | 2ms | `setProp` → `BridgeManager.callNativeMethod` → Native |
| `fontSize()` | 1ms | `setProp` → `BridgeManager.callNativeMethod` → Native |

### 优化潜力评估

假设 900 个 Text 组件都使用默认样式（如测试场景），优化后可跳过的调用：

| 属性 | 当前行为 | 优化后行为 | 节省耗时 |
|------|---------|-----------|---------|
| fontWeight | 每次调用 `fontWeightNormal()` | 跳过（null 默认值） | ~2ms |
| fontStyle | 每次调用 `fontStyleNormal()` | 跳过（null 默认值） | ~3ms |
| textAlign | 每次调用 `textAlignLeft()` | 跳过（默认左对齐） | ~3ms |
| textIndent | 每次调用 `firstLineHeadIndent(0f)` | 跳过（无缩进） | ~4ms |
| shadow | 每次调用 `textShadow(0,0,0,Transparent)` | 跳过（null） | ~2ms |
| letterSpacing | 每次调用 `letterSpacing(0)` | 跳过（未指定） | ~2ms |

**预估优化效果**：可节省约 **14-16ms**（占 applyTextStyle 耗时的 67-76%）

---

## 相关文件

- `/compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/text/BasicText.kt` - BasicTextWithNoInlinContent 实现
- `/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/jank/performance-analysis/compose-vs-kuikly-text.md` - 性能对比文档
- `/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/jank/iOSProfileLog2.md` - iOS Profile 日志
