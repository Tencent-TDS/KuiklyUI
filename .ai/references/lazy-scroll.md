# Lazy 组件滚动与 ContentSize 管理机制

> 以下场景读取本文件：`LazyColumn`/`LazyGrid`/`LazyStaggeredGrid` 出现滚动截断、滚动到底部停不住、加载更多不触发、`contentSize` 计算错误、`realContentSize` 为 null、`canScrollForward` 返回值异常、`tryExpandStartSizeNoScroll` 相关问题。

## 概述


KuiklyUI 的 Lazy 组件（LazyList / LazyGrid / LazyStaggeredGrid）运行在原生 ScrollView 之上。Compose 侧需要动态管理 ScrollView 的 `contentSize` 来支持虚拟滚动。核心挑战是：**Compose 无法一次性知道所有 item 的总高度**，需要边滚动边扩容。

## 核心数据结构

### KuiklyScrollInfo（`compose/.../gestures/KuiklyScrollInfo.kt`）

每个 ScrollableState 持有一个实例，关键字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `realContentSize` | `Int?` | 精确的内容总高度（仅当最后一个 item 可见时可计算），null 表示未知 |
| `currentContentSize` | `Int` | 当前设置给原生 ScrollView 的 contentSize |
| `composeOffset` | `Float` | Compose 侧的滚动偏移量（不超出边界） |
| `contentOffset` | `Int` | 原生 ScrollView 的 scroll offset |
| `viewportSize` | `Int` | 可视区域大小 |
| `itemMainSpaceCache` | `HashMap<Any, Int>` | item/行 的主轴尺寸缓存，用于检测高度变化 |
| `cachedTotalItems` | `Int` | 缓存的 item 总数，用于检测数据集变化 |
| `offsetDirty` | `Boolean` | 是否需要校正 ScrollView offset |

### realContentSize 生命周期

```
null (初始/失效)
  │
  ├─ 写入精确值: calculateContentSize() 中 totalContentSize() 返回非 null 时
  │   └─ 条件：最后一个 item 在可视区域内
  │
  └─ 重置为 null:
      ├─ calculateContentSize() 入口（每次重算先清空）
      ├─ resetForNewScrollView()（ScrollView 重绑定）
      ├─ items 数量增加（LazyList:255）
      └─ item/行高度变大且 !isScrollInProgress（LazyList:316, LazyGrid:320, StaggeredGrid:243）
```

## 完整数据流

### 1. 原生 Scroll 事件链

```
Native ScrollView scroll 事件
  │
  ▼
SubcomposeLayout.scroll {} 回调 (SubcomposeLayout.kt:277-324)
  │
  ├─ contentOffset = offset                      // 记录原生 offset
  ├─ calculateAndUpdateContentSize()              // 重新计算 contentSize
  │   ├─ calculateContentSize()
  │   │   ├─ realContentSize = null               // 先重置
  │   │   ├─ totalContentSize()                   // 检查最后一项是否可见
  │   │   │   └─ 最后一项可见 → realContentSize = 精确值
  │   │   └─ 返回 realContentSize 或 扩容后的估算值
  │   └─ currentContentSize = newContentSize
  │       └─ updateContentSizeToRender()          // 同步到原生 ScrollView
  │
  ├─ toButtomDelta = realContentSize - viewport - composeOffset  // ⚠️ 关键读取点
  │   └─ 若 realContentSize == null → toButtomDelta = null → 不做底部限制
  │   └─ 若 realContentSize 有值 → 计算剩余可滚动距离
  │
  ├─ 底部边界检查:
  │   └─ toButtomDelta != null && delta > toButtomDelta:
  │       └─ toButtomDelta <= 0: tryExpandStartSize() + return  // ⚠️ 滚动被截断
  │       └─ toButtomDelta > 0: composeOffset += min(delta, toButtomDelta)
  │
  ├─ kuiklyOnScroll(delta)                        // 触发 Compose 重布局
  │   └─ isScrollingState = true                  // ← isScrollInProgress 变为 true
  │
  └─ tryExpandStartSize(offset, true)             // 反向扩容检查
```

### 2. Measure 阶段（高度变化检测）

```
forceRemeasure() → LazyGrid/LazyList/StaggeredGrid measure
  │
  ├─ createLine/createItem 时检查高度:
  │   val oldHeight = itemMainSpaceCache[key]
  │   if ((oldHeight ?: 0) < newHeight && !isScrollInProgress) {
  │       realContentSize = null              // 失效化
  │       tryExpandStartSizeNoScroll()        // 异步扩容（delay 150ms）
  │   }
  │   itemMainSpaceCache[key] = newHeight
  │
  └─ canScrollForward = lastItemIndex != totalCount - 1 || currentMainAxisOffset > maxOffset
```

### 3. ScrollEnd 回调

```
Native scrollEnd 事件 (SubcomposeLayout.kt:268-276)
  │
  └─ kuiklyOnScrollEnd(params)
      └─ isScrollingState = false              // isScrollInProgress 变为 false
```

### 4. tryExpandStartSizeNoScroll 扩容逻辑

```
tryExpandStartSizeNoScroll() (ContentSizeExtensions.kt:243)
  │
  └─ delay(150ms) 后执行:
      ├─ 分支1: offset <= 0 && !isAtTop → 向后扩容 offset
      ├─ 分支2: offset > 0 && isAtTop → 校正 offset
      ├─ 分支3: isAtTop && realContentSize == null && lastItemVisible → 重算 contentSize
      └─ 分支4: canScrollForward && reachBtm → 底部扩容 contentSize
```

## 三个 Lazy 组件的共性

LazyList、LazyGrid、LazyStaggeredGrid 使用**完全相同的模式**检测高度变化并扩容：

```kotlin
// 模式: 高度变大 + 非滚动中 → 失效 realContentSize
if ((oldHeight ?: 0) < newHeight && !state.isScrollInProgress) {
    state.kuiklyInfo.realContentSize = null
    state.tryExpandStartSizeNoScroll()
}
```

因此如果一个组件有 contentSize 相关 bug，其他两个大概率也有，修复时需要考虑通用方案。

### itemMainSpaceCache key 构造差异

| 组件 | key 格式 | 示例 |
|---|---|---|
| LazyList | `itemResult.key` | `DefaultLazyKey(index=5)` |
| LazyGrid | `"line_${index}_${firstItemKey}"` | `line_7_DefaultLazyKey(index=35)` |
| LazyStaggeredGrid | `"item_${index}_${key}"` | `item_3_someKey` |

## scrollEnd 补偿机制

`kuiklyOnScrollEnd()` 末尾统一调用 `tryExpandStartSizeNoScroll()`，补偿滚动期间因 `isScrollInProgress=true` 被跳过的扩容操作。

## 关键文件索引

| 文件 | 关键内容 |
|---|---|
| `compose/.../gestures/KuiklyScrollInfo.kt` | `realContentSize`、`itemMainSpaceCache` 声明 |
| `compose/.../gestures/KuiklyScrollableState.kt` | `isScrollInProgress`、`kuiklyOnScroll`、`kuiklyOnScrollEnd` |
| `compose/.../scroller/ContentSizeExtensions.kt` | `calculateContentSize`、`tryExpandStartSize`、`tryExpandStartSizeNoScroll` |
| `compose/.../scroller/ScrollableStateExtensions.kt` | `kuiklyOnScrollEnd` + scrollEnd 补偿 |
| `compose/.../ui/layout/SubcomposeLayout.kt:268-324` | scroll/scrollEnd 原生回调处理 |
| `compose/.../foundation/lazy/grid/LazyGrid.kt:315-323` | Grid 行高变化检测 |
| `compose/.../foundation/lazy/LazyList.kt:313-319` | List item 高度变化检测 |
| `compose/.../foundation/lazy/staggeredgrid/LazyStaggeredGridMeasure.kt:237-245` | StaggeredGrid item 高度变化检测 |
