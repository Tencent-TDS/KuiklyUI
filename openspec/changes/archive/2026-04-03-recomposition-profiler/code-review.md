# Recomposition Profiler — 代码 Review 文档

## 整体架构

```
RecompositionProfiler (公开 API 入口)
    │
    ├── RecompositionTracker (核心引擎)
    │       ├── CompositionTracer ─────────────── 接收编译器注入回调
    │       ├── ProfilerCompositionObserver ────── 精确 scope→state 映射
    │       ├── StateIdentityRegistry ─────────── State 身份 ID 注册
    │       ├── Snapshot.registerApplyObserver ─── State 变更监听
    │       └── OutputStrategy[] ──────────────── 输出管道
    │
    └── RecompositionConfig (配置)

数据模型层：RecompositionEvent → RecompositionReport
输出层：LogOutputStrategy / JsonOutputStrategy / OverlayOutputStrategy
```

文件路径：`compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/`

---

## 文件逐一说明

### 1. `RecompositionProfiler.kt` — 公开 API 入口（Singleton）

**职责**：对外暴露的唯一门面，管理生命周期、线程安全、配置分发。

**关键设计**：
- `object` 单例，所有公开方法都通过 `SynchronizedObject` 加锁
- `start()` 时做两件事：① 创建 `RecompositionTracker` 并调 `Composer.setTracer(...)` 注册全局 CompositionTracer；② 通知所有 `ProfilerLifecycleListener`（即 BaseComposeScene）注册 CompositionObserver
- `stop()` 时将 tracer 替换为 `noOpTracer`（`isTraceInProgress()` 返回 false），编译器注入的 `traceEventStart/End` 调用会被 Compose Runtime 短路跳过，**零开销**
- `enabledState` 是 Compose State，供 UI 响应式感知 profiler 启停状态（如 Demo 页面的按钮文字切换）
- `ProfilerLifecycleListener` 是 internal 接口，只供 `BaseComposeScene` 实现，外部不可见

**需关注**：`lifecycleListeners` 是普通 `MutableSet`，`addLifecycleListener` 时若 profiler 已启用会立即回调 `onProfilerStarted`，需保证注册时机在 Composition 建立之前。

---

### 2. `RecompositionTracker.kt` — 核心追踪引擎

**职责**：接收所有追踪数据、维护状态、生成报告。整个模块最复杂的类。

**关键机制**（按数据流顺序）：

**① CompositionTracer 回调路径**（覆盖所有 Composable，零侵入）：
```
编译器注入 traceEventStart(key, dirty1, dirty2, info)
  → onComposableTraceStart() → 压栈 TraceEntry（含 dirty1/dirty2）
编译器注入 traceEventEnd()
  → onComposableTraceEnd() → 弹栈，计算耗时、解析 dirty、查询 triggerStates、生成事件
```

**② triggerStates 来源优先级**：
- 优先使用 `ProfilerCompositionObserver.getCurrentScopeTriggerStates()`（精确，scope 级别）
- 降级到 `currentFrameStateChanges`（粗粒度，帧级别）
- 由 `hasPreciseScopeMapping` flag 控制切换

**③ 参数变更检测**：
- 调 `DirtyFlagsParser.parse(dirty1, dirty2)` 解析编译器 `$dirty` bitmask
- 结合 `triggerStates` 和 `parentInfo` 判断 `RecompositionReason`

**④ Snapshot 观察者**：`registerSnapshotObserver()` 注册 `Snapshot.registerApplyObserver`，State 值变更时累积到 `currentFrameStateChanges` 和 `stateChangeAccumulator`

**⑤ 框架 Composable 过滤**：`frameworkPrefixes` 列表，`includeFrameworkComposables=false` 时过滤 `androidx.compose.*` 和 `com.tencent.kuikly.compose.runtime/ui/foundation` 等包名

**⑥ 采样控制**：`shouldSampleFrame()` 按 `sampleRate` 随机决定当前帧是否采样，不采样帧直接跳过所有处理

**内部累积器**：
- `MutableComposableAccumulator`：按 Composable name 聚合重组次数、耗时、triggerStates、paramChangeFrequency
- `MutableStateChangeAccumulator`：按 stateKey 聚合 State 变更次数

**需关注**：
- 类注释说"非线程安全，由 `RecompositionProfiler` 负责同步"，但实际上 `compositionTracer` 的回调（`traceEventStart/End`）发生在 Compose 的重组线程，而 `onFrameStart/End` 可能来自另一个调用路径——需确认这两条调用链是否在同一线程
- `events` 缓冲区溢出时 `removeAt(0)` 是 O(n) 操作，高频重组下可考虑换成 `ArrayDeque`

---

### 3. `DirtyFlagsParser.kt` — 编译器 `$dirty` bitmask 解析器

**职责**：纯函数工具，将 Compose 编译器传入的 `dirty1/dirty2` 整型解析为参数变更摘要。

**核心原理**（来自编译器源码 `ComposableFunctionBodyTransformer.kt`）：
- 每个参数占 **3 bits**，bit 0 保留，slot N 起始位 = `N * 3 + 1`
- 每个 Int 最多容纳 10 个参数槽（`SLOTS_PER_INT = 10`）
- 编码：`000`=UNUSED, `001`=SAME, `010`=DIFFERENT, `011`=STATIC, `100`=UNKNOWN
- `dirty2 = -1`（`0xFFFFFFFF`）是哨兵值，表示函数参数 ≤ 10 个，没有第二个 dirty 变量，**必须忽略**

**`parse()` 流程**：
1. `dirty1 == 0` 直接返回 null（无有效数据）
2. `parseSlotsFromInt(dirty1)` 解析 10 个槽
3. `dirty2 != -1 && dirty2 != 0` 时继续解析 dirty2
4. `trimTrailingUnused()` 裁剪尾部 `UNUSED(000)` 填充槽，得到真实参数数量

**需关注**：`trimTrailingUnused` 依赖"参数后面的槽全为 UNUSED"这个假设。编译器实际上会把未使用的 bit 置 0，这个假设成立。但 UNKNOWN(100) 参数如果出现在末尾不会被裁剪，这是正确行为。

---

### 4. `ProfilerCompositionObserver.kt` — 精确重组原因追踪

**职责**：利用 Compose Runtime 的 `CompositionObserver` API，获取每次重组中精确的 scope→triggerState 映射。

**数据流**：
```
onBeginComposition(invalidationMap)
  → 保存 scopeToStatesMap（scope → Set<State>）
  → 为每个被 invalidate 的 scope 注册 RecomposeScopeObserver

ScopeObserver.onBeginScopeComposition(scope) → activeScopeStack.push(scope)
CompositionTracer.traceEventEnd() → tracker 调 getCurrentScopeTriggerStates()
  → 取 activeScopeStack.top()，查 scopeToStatesMap，返回触发 states 列表
ScopeObserver.onEndScopeComposition(scope) → activeScopeStack.pop()

onEndComposition() → 清理所有临时数据，dispose 所有 handle
```

**两个查询方法**：
- `getCurrentScopeTriggerStates()` → 返回 `List<String>`（格式化后的 state 标识），供日志输出
- `getCurrentScopeTriggerStateObjects()` → 返回 `Set<Any>`（原始 state 对象），供 `StateIdentityRegistry.recordReader()` 建立 reader 映射

**triggers 的精确语义**：

返回值是 **"该 scope 依赖的 State 中，本帧 Snapshot.apply 时值发生变化的那些"**，即 `invalidationMap[scope]` 的内容。

这是 scope 粒度而非 Composable 函数粒度，有一个重要限制：**当同一帧内多个 State 同时变化（在同一个 Snapshot.apply 批次中被 commit），凡是该 scope 依赖的 State 都会出现在列表中**。

以 Demo 中的 `ParentChildDemo` 为例：点击 "Name Only" 时，按钮 lambda 里 `clickCount++` 和 `userName = ...` 是在同一次事件处理中执行的，它们被**同一个 Snapshot.apply 批次** commit——即使用户的意图只是改 `userName`，`clickCount` 也会出现在 triggers 里。

**为什么即使 scope 没有直接依赖 State 也会显示 triggers？**
子组件（如 `ThreeParamChild`）本身的 scope 不直接读取 State，但其父 scope 被 invalidate 后整个父函数重新执行，子组件的 scope 也可能出现在 `invalidationMap` 中，携带了父 scope 的 invalidation 原因。

**Compose Runtime 实现层面的证据**（`Composition.kt`）：
```kotlin
// 只有当 observer 被设置时，才记录具体触发 State（否则直接标记为 ScopeInvalidated）
if (observer == null && instance !is DerivedState<*>) {
    invalidations.set(scope, ScopeInvalidated)  // 不记录具体 State
} else {
    invalidations.add(scope, instance)  // 精确记录触发 State
}
```
`invalidationMap[scope]` 里的 `Set<Any>` = **本次 apply 批次中发生变化的、且被该 scope 读取的所有 State**。Compose 无法区分"哪个 State 才是真正触发该 scope 重组的那一个"——只要同批次变化且被该 scope 读取，都会出现。

这是 Compose Runtime API 的设计限制，无法从 `invalidationMap` 进一步细分。**精确到参数级别需结合 `paramChanges`（编译器 `$dirty` bitmask）判断**——`params changed: [#1] (1/3)` 才是最准确的信息。

**null/空值语义**：
- 返回 `null` → scope 不在 invalidationMap，是初次组合或子 scope
- 返回 `listOf("[forced recomposition]")` → key 存在但 value 为 null，强制重组
- 返回正常列表 → 有明确 State 触发

**需关注**：
- 每次 `onBeginComposition` 都会 dispose 上次的 `scopeObserverHandles` 再重建，若同一帧内有多个 Composition 实例（KuiklyUI 通常只有一个）需注意
- `ScopeObserver.onScopeDisposed` 时从 `scopeToStatesMap` 移除，防止 scope 对象内存泄漏

---

### 5. `StateIdentityRegistry.kt` — State 身份 ID 注册表

**职责**：为 State 对象分配会话内稳定的短 ID，并记录 reader Composable，提升 State 标识可读性。

**核心机制**：
- 用 `identityHashCode`（对象内存地址哈希，非 `equals`）作为 key，规避 State 对象没有自定义 `equals` 的问题
- ID 从 1 开始递增，格式 `State#1`、`State#2`...
- `formatState()` 输出：`State#1(value=3), readers: CounterSection, StatusBar`
- `extractValue()` 从 `MutableState.toString()` 中提取 `(value=...)` 部分

**需关注**：
- `identityHashCode` 在极少情况下可能碰撞（不同对象相同 hash），会导致两个不同 State 共享同一个 ID。实际概率极低，对 profiler 场景可接受
- `enableStateIdentity = false` 时此类完全不使用，不产生任何开销
- `formatState()` 的 value 提取依赖 `state.toString()` 输出格式为 `MutableState(value=...)@xxx`（Compose 内置格式）。若 State 持有的是 `data class`，Kotlin 自动生成 `toString()` 可正确展示；若是普通 class 无自定义 `toString()`，则 value 部分显示为空（`State#N` 无括号），**不崩溃，graceful degradation**

---

### 6. `RecompositionEvent.kt` — 事件模型

**职责**：定义追踪事件的数据类型，sealed class 确保穷举处理。

**四种事件**：
| 事件 | 触发时机 | 关键字段 |
|------|---------|---------|
| `RecompositionFrameStartEvent` | 帧开始 | `frameId` |
| `RecompositionFrameEndEvent` | 帧结束 | `frameId`, `durationMs`, `recomposedCount` |
| `ComposableRecomposedEvent` | 单个 Composable 重组完成 | `composableName`, `triggerStates`, `reason`, `paramChanges` |
| `StateChangedEvent` | MutableState 值变更 | `stateKey`, `sourceInfo` |

**`RecompositionReason` 枚举**：
- `STATE_CHANGE`：`triggerStates` 非空，有 State 直接 invalidate
- `PARENT_RECOMPOSITION`：父组件重组传入新参数（当 triggerStates 为空且有 parent 且 `paramChanges.hasChanges`）
- `UNKNOWN`：初次组合或无法判断

**需关注**：由于 `CompositionObserver.invalidationMap` 实际上也会包含子组件 scope（当父组件被 invalidate 时子组件同样在 map 中），`PARENT_RECOMPOSITION` 在当前实现中几乎不会触发。这是一个已知的设计 trade-off，不影响 `STATE_CHANGE` 的准确性。

---

### 7. `RecompositionReport.kt` — 报告模型

**职责**：汇总统计数据的数据类 + 内置 JSON 序列化。

**`ComposableStats` 字段**：
- `parentTriggeredCount`：因父组件重组而触发的次数
- `paramChangeFrequency`：各参数位置变更频率 map（`paramIndex → count`），JSON 输出格式：`"#0": 5, "#1": 3`

**JSON 序列化**：手写 `buildString` 拼接，避免依赖 kotlinx.serialization，跨平台兼容。

**需关注**：`escapeJson` 方法在 `RecompositionReport.kt` 和 `JsonOutputStrategy.kt` 各有一份实现，存在重复代码，可提取到 common util。

---

### 8. `RecompositionConfig.kt` — 配置

**职责**：不可变配置数据类 + DSL 构建器。

**配置项**：
| 配置 | 默认值 | 说明 |
|-----|--------|------|
| `sampleRate` | 1.0f | 帧采样率，降低可减少性能开销 |
| `hotspotThreshold` | 10 | 每秒重组次数超过此值标记为热点 |
| `maxEventBufferSize` | 10000 | 事件环形缓冲区上限 |
| `enableStateTracking` | true | 是否监听 Snapshot |
| `includeFrameworkComposables` | false | 是否包含框架内部 Composable |
| `enableStateIdentity` | false | 是否启用 State 身份 ID |

**需关注**：`maxEventBufferSize` 只校验 `> 0`，没有上限，传入极大值可能导致 OOM。

---

### 9. `RecompositionOutputStrategy.kt` — 输出策略接口

**职责**：策略模式接口，两个方法均可选实现。`onReportReady` 有默认空实现，策略只需关心自己感兴趣的事件类型。

> ~~`LocalRecompositionProfiler.kt`~~ — 已删除。原本提供 `LocalRecompositionProfilerEnabled` CompositionLocal，但框架内部无任何消费者（死代码）。业务直接通过 `RecompositionProfiler.start()/stop()` 全局控制即可，可在任意时机调用（Composition 建立前后均可），框架内部的 `ProfilerLifecycleListener` 机制会自动处理时序。

---

### 10. `output/LogOutputStrategy.kt` — 日志输出

**职责**：将追踪数据格式化为可读日志，帧事件走 `KLog.d`，报告走 `KLog.i`。

`buildParamChangeString()` 格式：`params changed: [#0, #2] (2/3)`，只在 `hasChanges` 时输出。

**需关注**：帧日志使用 `indent` 计数表达嵌套层级，但由于 `traceEventStart/End` 的调用顺序和 `FrameStart` 的插入时机，实际嵌套层级可能不完全准确。

---

### 11. `output/JsonOutputStrategy.kt` — JSON 输出

**职责**：结构化 JSON，面向 AI Agent 或外部工具消费。`logFrameEvents = false` 时不打日志，只缓存到 `frameJsonBuffer`，可通过 `getFrameJsonBuffer()` 取出。

**需关注**：`frameJsonBuffer` 没有容量上限，长时间运行可能积累大量数据，建议配合 `clearFrameJsonBuffer()` 定期清理。

---

### 12. `output/OverlayOutputStrategy.kt` — UI Overlay（实验性）

**职责**：在 App 界面上叠加实时重组热度可视化，颜色编码：绿→黄→橙→红。

**实现方式**：持有 `mutableStateMapOf<String, Int>` 作为响应式计数器，帧完成时更新，Compose 自动重组 `OverlayContent()`。

**需关注**：`OverlayContent()` 需要手动插入到 Composition 树中，目前属于实验性功能，使用门槛较高。

---

### 13. `LocalRecompositionProfiler.kt` — CompositionLocal

**职责**：提供 `LocalRecompositionProfilerEnabled` CompositionLocal，供 Composable 树内部感知 profiler 启停状态。

---

## Review 要点汇总

| 分类 | 问题 | 严重度 |
|------|------|--------|
| 线程安全 | `RecompositionTracker` 非线程安全，但 `compositionTracer` 回调（`traceEventStart/End`）与 `onFrameStart/End` 均在同一帧调用栈内同步执行（`frameClock.sendFrame` 内部），**实际无跨线程风险** | 无问题 |
| 性能 | `events.removeAt(0)` O(n)，高频重组下建议改 `ArrayDeque` | 低 |
| 内存 | `frameJsonBuffer` 无上限，长时运行需注意 | 低 |
| 重复代码 | `escapeJson` 在 `RecompositionReport` 和 `JsonOutputStrategy` 各一份 | 低 |
| 参数校验 | `maxEventBufferSize` 无上限校验 | 低 |
| 实验性功能 | `OverlayOutputStrategy` 需手动注入到 Composition 树，使用门槛较高 | 设计讨论 |
| **State 定位** | **非参数更新场景（Composable 自身读取 State 触发重组）下，triggers 只能显示全局 `State#N` ID，无法直接对应到源码中的变量名，定位体验较差。参数更新场景可通过 `params changed: [#N]` 精确定位，但自身 State 读取场景缺乏有效手段。候选方案：① 提供 `rememberProfiledState("name") { mutableStateOf(...) }` 显式命名 API；② Composable 内局部 State 重新编号。待测试完成后评估优化** | 待优化 |
| **首次 Composition 盲区** | **Profiler 只追踪 recomposition，不追踪 initial composition。LazyColumn 滑入新 item 走的是首次组合路径（`Composition.composeContent`），不经过 `onFrameStart/End` 帧回调，`currentFrameSampled` 始终为 false，导致 `traceEventStart` 直接跳过，无日志输出。优化方向：不区分首次/非首次，统一输出执行日志，首次组合加 `[initial]` 标记区分即可。实现思路：以 `CompositionObserver.onBeginComposition/onEndComposition` 作为帧边界替代 `onFrameStart/End`，两种路径都能覆盖。待测试完成后实现** | 待优化 |
