# 运行时上下文数据方案

> **status: draft** — 尚未实现，计划在独立 change 中完成（依赖 AI skills 完善后）。

## 背景

当前 profiler 输出只有重组次数/耗时/triggerStates，缺乏**操作行为上下文**，AI 无法判断重组是否合理：

| 场景 | 当前数据 | 缺失信息 | AI 无法判断 |
|------|---------|---------|------------|
| 用户滑动 LazyColumn | `ScrollListItem 重组 48 次` | 滑动了几格 | 每格触发 4 次 vs 1 次，哪个正常？ |
| 动画运行 | `AnimateColorDemo 重组 57 次` | 动画时长 | 300ms 动画 57 次 = 190fps（异常）vs 3s 动画 57 次 = 19fps（正常） |

**目标**：在现有 `profiler_frames.jsonl` 中补充运行时上下文事件，让 AI 能量化判断重组效率。

---

## 方案一：LazyColumn 滚动上下文（12.1）

### 数据格式

在 `profiler_frames.jsonl` 中新增 `scroll_context` 类型事件行（与帧事件穿插）：

```jsonl
{"type":"scroll_context","timestampMs":1775188247602,"lazyListId":"ScrollSection-list","firstVisibleItemIndexFrom":0,"firstVisibleItemIndexTo":8,"visibleItemCount":5}
```

字段说明：
- `lazyListId`：LazyList 实例标识（用于区分页面内多个 LazyList）
- `firstVisibleItemIndexFrom/To`：滑动前后的首个可见 item index
- `visibleItemCount`：当前视口可见的 item 数量（用于判断滑入数量）

### AI 分析逻辑

```
滑动 index 0→8，视口可见 5 个 item，实际滑入 8 个新 item
ScrollListItem 重组 8 次 → 每个新 item 组合 1 次 → ✅ 正常

若 ScrollListItem 重组 40 次 → 每个新 item 重组 5 次 → ⚠️ 异常
（说明 LazyColumn 外层有 State 变化导致全部 item 重组）
```

### 实现方案

在框架层 `LazyColumn`/`LazyRow` 实现中观察 `LazyListState`：

```kotlin
// 伪代码，在 LazyColumn Composable 内部
val listState = rememberLazyListState()
val firstVisible = listState.firstVisibleItemIndex

// 观察变化时写入 Profiler 上下文事件
LaunchedEffect(firstVisible) {
    if (RecompositionProfiler.isEnabled) {
        RecompositionProfiler.recordScrollContext(
            lazyListId = ...,
            indexFrom = prevFirstVisible,
            indexTo = firstVisible,
            visibleCount = listState.layoutInfo.visibleItemsInfo.size
        )
    }
}
```

`recordScrollContext` 写入 `FileOutputStrategy` 的 pending buffer，随帧数据一起 append 到 `profiler_frames.jsonl`。

### 期望判断规则（写入 recomposition-analyzer skill）

```
正常：重组次数 ≤ 滑入新 item 数 × 2
可疑：重组次数 > 滑入新 item 数 × 5（说明有额外触发）
严重：重组次数 ≈ 总 item 数（说明全量重组，通常是 LazyList 外层 State 问题）
```

---

## 方案二：动画上下文（12.2）

### 数据格式

在 `profiler_frames.jsonl` 中新增 `animation_context` 类型事件行：

```jsonl
{"type":"animation_context","timestampMs":1775188247602,"event":"start","animationType":"animateColorAsState","durationMs":300,"label":"background-color"}
{"type":"animation_context","timestampMs":1775188247902,"event":"end","animationType":"animateColorAsState","durationMs":300,"label":"background-color"}
```

### AI 分析逻辑

```
animateColorAsState 动画 300ms，期间触发 18 次重组
18 次 / 300ms ≈ 60fps → ✅ 正常（每帧一次重组）

animateColorAsState 动画 300ms，期间触发 180 次重组
180 次 / 300ms ≈ 600fps → ⚠️ 严重异常（多个 State 在同步变化）
```

### 实现方案

**选项 A（Hook animateTo）**：在框架层 `Animatable.animateTo` 入口/出口处插入记录，成本低但需要修改 Compose Animation 框架代码。

**选项 B（observe animateXxxAsState 的 State 变化频率）**：利用现有 `StateIdentityRegistry`，当某个 State 的变化频率极高（如 60次/s）且值是数值型时，自动标注为"疑似动画驱动"，不需要额外 hook。这是一个**近似方案**，实现成本最低：
- 不需要修改 Animation 框架
- 在 `recomposition-analyzer` skill 里加规则：`stateChanges` 中某 State 在 300ms 内变化 > 10 次 → 标注"疑似动画 State"
- AI 可据此判断该 State 驱动的高频重组是否属于动画范畴

**推荐选项 B 作为 12.2 的实现**：低成本，覆盖大多数场景，无需修改框架。

### 期望判断规则（写入 recomposition-analyzer skill）

```
某 State 在 1s 内变化 > 30 次 → 疑似动画驱动
对应组件的高频重组 → 标注"动画驱动，属正常" 而非热点问题
```

---

## 数据流变更

`profiler_frames.jsonl` 由纯帧事件扩展为**混合事件流**：

```
{"type":"frame","events":[...]}           ← 重组帧（现有）
{"type":"scroll_context",...}             ← 滚动上下文（12.1 新增）
{"type":"animation_context",...}          ← 动画上下文（12.2 新增，选项A）
```

`recomposition-analyzer` skill 需同步更新，增加对这两类事件的解析和分析规则。

---

## 实现优先级

| 任务 | 价值 | 实现成本 | 优先级 |
|------|------|---------|--------|
| 12.1 LazyColumn 滚动上下文 | 高（滑动误判最常见） | 中 | P1 |
| 12.2 动画上下文（选项B，skill 层） | 中（不改框架代码） | 低 | P2 |
| 12.2 动画上下文（选项A，框架层hook） | 高（精确） | 高 | P3 |
