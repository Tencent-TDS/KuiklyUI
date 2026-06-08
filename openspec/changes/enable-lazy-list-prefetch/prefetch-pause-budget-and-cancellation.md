# LazyList Prefetch：暂停粒度、预算检查与取消（归档前知识沉淀）

> 归档 `enable-lazy-list-prefetch` 时，将本文要点同步到 `.ai/references/`（建议路径：`lazy-list-prefetch-scheduler.md`）。
> 代码锚点：`KuiklyPrefetchScheduler.kt`、`LazyLayoutPrefetchState.kt`（`PrefetchHandleProvider`）、`LazyListPrefetchStrategy.kt`、`BaseComposeScene.render()`。

## 1. 两层「暂停」语义

| 层级 | 粒度 | 行为 |
|------|------|------|
| **调度器** | 一条 `PrefetchRequest`（1-item 策略 ≈ 一个 index） | `execute()` 返回 `true` → 任务留在队首，本帧 `processRequests` 结束 |
| **Request 内部** | compose → apply → nested → measure 等**阶段**；pausable 时还有 **`PausedComposition.resume` 分片** | 阶段门禁 `shouldExecute`；pausable 在每次 `resume` 回调里扣预算并可能 `pauseRequested` |

官方契约（Compose `PrefetchRequest.execute`）：`false` = 做完；`true` = 还有活但本帧 `availableTimeNanos` 不够，下帧再调。

Kuikly 与官方对齐：`availableTimeNanos = max(0, nextFrameTimeNs - now)`（非 idle）；idle 为 `Long.MAX_VALUE`。

## 2. `availableTimeNanos` 何时检查

### 2.1 Scope（帧级，实时）

- `PrefetchRequestScope.availableTimeNanos()`：相对**下一帧 deadline** 的剩余时间（idle 为 `Long.MAX_VALUE`）。
- `processRequests` 入口：**无** Kuikly 自有 2ms 门槛；与官方一致，仅在 `runRequest()` 内用 `availableTimeNanos > 0`。
- `runRequest()`：scope `availableTimeNanos > 0` 才调用 `execute()`；否则 `scheduleForNextFrame`（下一 VSync `needRedraw`）。

### 2.2 Request 内部（本地账本）

每次 `executeRequest()` 开头：

```text
resetAvailableTimeTo(scope.availableTimeNanos())  // 读 scope 一次快照
startBudgetNs = 本地剩余                         // 9.12 超支判据用
```

之后用 `updateElapsedAndAvailableTime()` **扣减本地剩余**；`shouldExecute(本地, 历史平均)` 用于阶段门禁。

**跨帧**：`PausedPrecomposition` 状态保留；**下一帧** `executeRequest` 会 **重新** `resetAvailableTimeTo(scope)`，不是接着上一帧本地毫秒数。

### 2.3 单 item 一次 `execute()` 内的检查点

| 阶段 | Pausable 开 | Pausable 关 |
|------|-------------|-------------|
| **Compose** | 进 `performPausable` 前 1 次 `shouldExecute`；**每个** `composition.resume { }` 回调末尾再检查 | 进 `performFullComposition` 前 1 次；通过则**整段** compose，中间不查 |
| **Apply** | apply 前 1 次 | 同左 |
| **Nested** | resolve 前 `available>0`；nested 执行内部另有逻辑 | 同左 |
| **Measure** | measure 前 1 次；`performMeasure` 内不逐 placeable 查 | 同左 |

Pausable 的 **pause 细粒度**：Compose Runtime 在 `PausedComposition.resume(shouldPause)` 的**安全点**之间推进；prefetch 至少在**每个 `resume` 回合结束**扣时间并可能 `pauseRequested=true`（见 trace `runRequest paused`）。

非 pausable / 单次 `precompose` / 单次 `performMeasure`：**执行过程中不会**再查预算；若一段过长可能触发 9.12 `elapsedNs > startBudgetNs`。

## 3. 单线程与「下帧插不进来」

- Prefetch 在 **`BaseComposeScene.render()` 末尾**、与 recompose/layout/draw **同 Pager 协程线程**顺序执行。
- **不会**在 compose 执行到一半被另一线程抢占；下一帧是**下一次** `render()`，且顺序为：主帧工作 → draw → `processRequests`。
- 「下帧插入」= 下一 VSync 先跑主帧，再在帧末继续同一 request 的 pause 状态。

## 4. 取消（cancel）来源与 trace 标签

| 阶段 | trace 关键字 | 含义 |
|------|----------------|------|
| **Strategy** | `strategy reset cancel index=` | `DefaultLazyListPrefetchStrategy.resetPrefetchState()`：滚动目标 index 变化或 reset |
| **Request 句柄** | `request cancel index=` | `PrefetchHandle.cancel()`：显式取消（strategy 会调 `currentPrefetchHandle?.cancel()`） |
| **Request 无效** | `executeRequest invalid` + `canceled=true` | 执行时发现已 cancel / index 非法 |
| **Scheduler** | `scheduler cancelAll queueSize=` | `KuiklyPrefetchScheduler.cancelAll()`（如 Pager dispose） |
| **CacheWindow** | `cacheWindow cancel index=` | `CacheWindowLogic` 窗口外/ refill 时 `handle.cancel()`（仅 CacheWindow 策略） |

**Pause ≠ Cancel**：`runRequest paused` / `runRequest paused hasMoreWork` 表示预算用尽、**下帧续做**，任务仍在队列。

9.12 E2E 通过 `evaluatePrefetchCancelEvidence(traceLines)` 汇总上表计数（见 `lazy-prefetch-metrics.ts`）。

## 5. 与 9.12 判据的关系

| 尺子 | 用途 |
|------|------|
| `shouldExecute(本地, avg.*)` | 阶段是否**开始** compose/apply/measure |
| `elapsedNs` vs `startBudgetNs` | 单次 execute 内 pausable **总耗时**是否抢主帧 |
| `finalQueuePending` | 停稳后队列是否清空 |
| cancel 统计 | **诊断**：滚动/reset 导致多少预取被撤，**非** 9.12 通过必要条件（除非产品另立规约） |
