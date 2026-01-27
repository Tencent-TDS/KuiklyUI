# 优化方案：支持预加载 Prefetch

> 优先级：P0  
> 状态：待实现

---

## 问题背景

快滑动时复用率仅 8.7%，根本原因是时序问题：measure 阶段需要新 slot 时，旧 slot 还没释放到复用池。

---

## 核心原理

提前 `precompose` 即将可见的 item，这样在滚动到该位置时，slot 已经准备好了。

---

## 关键 API

来自 `SubcomposeLayout.kt`：

```kotlin
fun precompose(
    slotId: Any?,
    content: @Composable () -> Unit,
): PrecomposedSlotHandle {
    // ...
    val node = precomposeMap.getOrPut(slotId) {
        val reusedNode = takeNodeFromReusables(slotId)
        if (reusedNode != null) {
            // 复用已有 slot
        } else {
            createNodeAt(root.foldedChildren.size).also {
                precomposedCount++
            }
        }
    }
    subcompose(node, slotId, content)
    // ...
}
```

---

## 实现方式

1. 利用 `SubcomposeLayoutState.precompose()` API
2. 在滚动监听中，提前 precompose 下 N 个 item

---

## 预期效果

- 首次滑动不容易白屏，因为 slot 已经预创建好了
- 预加载的 item 在 `precomposeMap` 中等待使用

---

## 优势

- API 已存在，实现相对简单
- 直接解决「首次滑动白屏」问题
