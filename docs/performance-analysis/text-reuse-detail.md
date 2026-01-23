# 快滑动复用率低 - 详细代码分析

> 本文档是 [text-reuse-analysis.md](./text-reuse-analysis.md) 的详细补充

---

## 复用机制的关键流程

```
滚动事件 → onScroll() → forceRemeasure() → measure() → placeChildren() → disposeOrReuseStartingFromIndex()
```

---

## 关键代码位置

### 1. 复用池回收时机

**文件**：`compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/layout/SubcomposeLayout.kt`（约 1000 行）

```kotlin
fun createMeasurePolicy(...): MeasurePolicy {
    return object : LayoutNode.NoIntrinsicsMeasurePolicy(...) {
        override fun MeasureScope.measure(...): MeasureResult {
            currentIndex = 0
            val result = scope.block(constraints)  // ← measure 阶段：subcompose 新 item
            val indexAfterMeasure = currentIndex
            return createMeasureResult(result) {
                currentIndex = indexAfterMeasure
                result.placeChildren()
                checkOffScreenNode(result)
                disposeOrReuseStartingFromIndex(currentIndex)  // ← placeChildren 阶段：释放旧 slot
            }
        }
    }
}
```

### 2. 滚动触发 remeasure

**文件**：`compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/LazyListState.kt`（第 385-423 行）

```kotlin
internal fun onScroll(distance: Float): Float {
    scrollToBeConsumed += distance
    if (abs(scrollToBeConsumed) > 0.5f) {
        val intDelta = scrollToBeConsumed.fastRoundToInt()
        // 尝试不触发 remeasure 的优化
        val scrolledWithoutRemeasure = layoutInfo.tryToApplyScrollWithoutRemeasure(...)
        
        if (scrolledWithoutRemeasure) {
            // 小滑动：只需要 re-placement
            placementScopeInvalidator.invalidateScope()
        } else {
            // 大滑动：需要完整 remeasure
            remeasurement?.forceRemeasure()  // ← 快滑动时走这里
        }
    }
}
```

---

## 问题本质：时序问题

### 执行顺序图示

```
                    一次 measure pass 的执行顺序
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌─────────────────────────────────────────────────────────────────┐
│  measure 阶段                                                    │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  1. subcompose(新 item 18) → 需要 slot → 复用池空 → 新建    ││
│  │  2. subcompose(新 item 19) → 需要 slot → 复用池空 → 新建    ││
│  │  3. subcompose(新 item 20) → 需要 slot → 复用池空 → 新建    ││
│  │  ...                                                        ││
│  │  N. subcompose(新 item 30) → 需要 slot → 复用池空 → 新建    ││
│  └─────────────────────────────────────────────────────────────┘│
│                           ↓                                      │
│                    此时旧 item 0~17 还在使用中！                  │
│                    它们还没有被释放到复用池                       │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│  placeChildren 阶段                                              │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  disposeOrReuseStartingFromIndex()                          ││
│  │  → 释放旧 item 0~17 进复用池                                ││
│  │  → 但已经太晚了！新 item 都已经创建完了                      ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 慢滑动 vs 快滑动对比

```
【慢滑动】每帧滑动 50px，只需要 0~1 个新 item
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Frame 1: 滑出 item 0 → 进复用池
Frame 2: 需要 item 18 → 从复用池取 item 0 的 slot → ✅ 复用成功
Frame 3: 滑出 item 1 → 进复用池  
Frame 4: 需要 item 19 → 从复用池取 item 1 的 slot → ✅ 复用成功
...

【快滑动】一帧滑动 2000px，需要 10+ 个新 item
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Frame 1:
  measure 阶段:
    - 需要 item 18, 19, 20, 21, 22... → 复用池是空的！
    - 旧 item 0~17 还没释放 → 全部新建 ❌
  placeChildren 阶段:
    - 释放 item 0~17 进复用池 → 太晚了，上面都新建完了
```

| 场景 | 每次滑动 delta | subcompose 新 item 数量 | 复用情况 |
|------|---------------|------------------------|---------| 
| 慢滑动 | 小（如 10px） | 0-1 个 | 上一帧释放的 slot 足够复用 |
| 快滑动 | 大（如 500px）| 10+ 个 | 本帧需要大量新 slot，但旧 slot 还在 measure 中 |

---

## 测试数据

### 复用率对比

| 指标 | 慢滑动 | 快滑动 |
|------|--------|--------|
| 总 Recompose 次数 | 1000 次 | 625 次 |
| 不同 viewRef 数量 | 425 个 | 575 个 |
| 复用次数 | 575 次 | 50 次 |
| **复用率** | **100%** | **8.7%** |

### 慢滑动详细数据

| 滑动数据 | 值 |
|---------|-----|
| 滑动涉及的 carIndex 范围 | 18 ~ 57（共 40 行）|
| 每行 Text 数量 | 25 个 |
| 总 Recompose 次数 | 40 × 25 = 1000 次 ✅ |

### viewRef 复用分布（慢滑动）

| 复用情况 | viewRef 数量 | 说明 |
|---------|-------------|------|
| 重组 2 次 | 275 个 | 被复用 1 次 |
| 重组 3 次 | 150 个 | 被复用 2 次 |

---

## 复用池容量限制

```kotlin
// LazyLayout.kt
MaxItemsToRetainForReuse = 7  // 每种 contentType 最多保留 7 个 slot
```

- 屏幕上同时显示约 13 行
- 理论最大需要 13 + 7 = 20 行的 viewRef = 500 个
- **但主要问题不是容量，而是时序**

---

## 可能的优化方向

1. **提前释放 slot**：在 measure 阶段开始时，先把不可见的 slot 释放到复用池
2. **预取优化**：提前 precompose 即将可见的 item（已被注释掉的 prefetch 机制）
3. **分批 measure**：避免一次 measure pass 中 subcompose 太多新 item

---

## 首屏日志分析（无重复问题）

首屏各阶段日志数量一致，每个组件只执行 1 次：

- bindViewRef: 330 次
- Recompose: 325 次
- ComposeMeasure: 324 次
- NativeMeasure: 323 次
- Layout: 323 次

---

## 嵌套 LazyRow 问题详解

### 原因分析

1. **contentType 设置问题**：原来的 contentType 使用动态值 `"${item.type}"`，应该使用固定值
2. **LazyRow 嵌套问题**：每个 LazyRow 是独立的 SubcomposeLayout，有自己独立的复用池。当外层 LazyColumn 行滚出屏幕时，整个 LazyRow 被回收，其内部的复用池也被丢弃

### 修改方案

```kotlin
// 修改前：LazyRow
LazyRow(
    modifier = Modifier.fillMaxWidth().height(itemHeight.dp),
    contentPadding = PaddingValues(horizontal = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    itemsIndexed(
        items = cardListData,
        key = { index, item -> "${item.id}" },
        contentType = { index, item -> "RankingItem" }
    ) { itemIndex, item ->
        RankingItemView2(...)
    }
}

// 修改后：普通 Row
Row(
    modifier = Modifier.fillMaxWidth().height(itemHeight.dp).padding(horizontal = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    cardListData.take(5).forEachIndexed { itemIndex, item ->
        RankingItemView2(...)
    }
}
```

### 验证结果

- 修改前：每个 viewRef 只重组 1 次（550 个 viewRef，550 次重组）
- 修改后：部分 viewRef 重组 2 次（575 个 viewRef，625 次重组）— 说明复用生效
