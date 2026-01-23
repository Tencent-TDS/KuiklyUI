# 优化方案：支持 Lazy 组件复用

> 优先级：P0  
> 状态：待验证

---

## 问题背景

当 `LazyColumn` + `LazyRow` 嵌套时，Text 的 LayoutNode 无法复用。每个 LazyRow 有独立的复用池，外层行滚出时整个复用池被丢弃。

---

## 核心发现

**LazyRow 被复用时，内部 Text 节点也会被复用！**

### 复用链路

```
LazyRow 被复用
    ↓
LazyRow 的 composition 被保留（existing != null）
    ↓
调用 setContentWithReuse(composable)
    ↓
LazyRow 内部的 SubcomposeLayoutState 也被保留
    ↓
LazyRow 内部的复用池也被保留！
    ↓
Text 节点可以被复用 ✓
```

### 状态保留情况

| 状态 | 是否保留 |
|------|----------|
| LazyRow 的 `ReusableComposition` | ✅ 保留 |
| LazyRow 的 `SubcomposeLayoutState` | ✅ 保留 |
| LazyRow 内部的复用池 | ✅ 保留 |
| LazyRow 内部的 Text 节点 | ✅ 可复用 |

---

## 实现思路

1. 确保 LazyRow/LazyColumn 使用 `ReusableComposeNode` 而非 `ComposeNode`
2. 设置合适的 `contentType` 让外层正确复用

---

## 代码分析

### 1. 从复用池取出时 (`takeNodeFromReusables`)

```kotlin
private fun takeNodeFromReusables(slotId: Any?): LayoutNode? {
    // ...
    val nodeState = nodeToNodeState[node]!!
    nodeState.forceReuse = true   // ⬅️ 标记需要复用
    nodeState.forceRecompose = true
    return node
}
```

### 2. 复用时调用 `setContentWithReuse`

```kotlin
private fun subcomposeInto(...): ReusableComposition =
    if (existing == null || existing.isDisposed) {
        createSubcomposition(container, parent)  // 新建
    } else {
        existing  // ⬅️ 复用已有的 ReusableComposition！
    }.apply {
        if (!reuseContent) {
            setContent(composable)
        } else {
            setContentWithReuse(composable)  // ⬅️ 带复用地设置内容！
        }
    }
```

---

## 待验证点

- [ ] 实际运行时复用链路是否畅通
- [ ] 复用后内部状态是否正确
- [ ] 滚动位置是否需要重置

---

## 注意事项

| 问题 | 说明 |
|------|------|
| **首次大块滑动** | LazyRow 首次进入复用池前，内部复用池可能还没积累足够的 slot |
| **大块滑动时序问题** | 内层 LazyRow 仍然有「先取后放」的时序问题 |
| **LazyRow 内容差异大** | 如果不同行的 LazyRow 内容差异很大，复用效果可能不理想 |

---

## 结论

> **✅ 让 LazyRow 支持复用，可以有效解决嵌套 Lazy 的复用问题！**
>
> 关键是：LazyRow 被复用时，它内部的 `SubcomposeLayoutState` 和复用池都会保留，所以内部的 Text 节点也能被复用。
>
> 这比「全局共享复用池」的方案更简单可行。
