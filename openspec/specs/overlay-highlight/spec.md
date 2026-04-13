# Overlay Highlight — Composable 重组热点可视化

## 背景

现有 Profiler 通过日志输出重组信息，适合离线分析。
本功能在运行时 UI 上直观标记重组热点，帮助开发者在调试阶段快速定位问题组件，无需翻查日志。

## 目标

- 悬浮圆形按钮，可拖拽，实时显示当前帧重组次数
- 点击展开居中面板，展示重组次数最高的 5 个 Composable 热点（按实例区分）
- 面板顶部有暂停/继续、重置、输出报告三个按钮
- 不污染现有追踪数据（Overlay 自身重组不计入）
- 天然覆盖业务 Dialog/Popup（利用 KuiklyUI 插槽系统）
- Profiler OFF 时零开销

---

## UI 设计

### 收起态（悬浮圆形按钮）

```
              ╭───╮
              │🔥12│  ← 当前帧重组次数（红色）
              ╰───╯    0 次时绿色，暂停时显示 ⏸
```

- 默认位置：右下角
- 可拖拽到任意位置
- 直径约 48dp

### 展开态（居中面板）

```
┌─────────────────────────────────┐
│  重组热点                    ✕  │
│  [▶ 继续] [↺ 重置] [📋 报告]   │
│ ─────────────────────────────── │
│  CounterSection @Demo.kt:100     │
│  总 47 次  最近1分钟 12 次       │
│ ─────────────────────────────── │
│  AutoIncrementSection @Demo.kt:245│
│  总 31 次  最近1分钟 8 次        │
│ ─────────────────────────────── │
│  ScrollListItem @Demo.kt:386     │
│  总 18 次  最近1分钟 5 次        │
│ ─────────────────────────────── │
│  （最多展示 5 条，按总次数降序）  │
└─────────────────────────────────┘
```

- 点击条目展开 triggers 详情
- 点击遮罩 / ✕ / 返回收起，缩放动画回原位置

---

## 热点统计口径

### 实例 Key 方案 — RecomposeScope 验证结果

#### 编译器源码分析（compose-multiplatform-core）

编译器中存在**两个不同的 `sourceKey` 函数**：

| 函数 | 文件 | 计算方式 | 用于 |
|------|------|----------|------|
| `IrElement.sourceKey()` | ComposableFunctionBodyTransformer:2002 | `hash(fqName + startOffset + endOffset)` | `startReplaceGroup` 等内部 group |
| `IrSimpleFunction.sourceKey()` | AbstractComposeLowering:1208 | `hash(fqName + JVM signature)` | **`traceEventStart`** |

**关键发现**：`traceEventStart` 和 `startRestartGroup` 都使用 `irFunctionSourceKey()`（函数定义级别 key），
即 `hash(函数名 + JVM 签名)`。`info` 字符串中的行号也是**函数定义行**，不是调用行。

因此 `traceEventStart` 的 key 和 info **完全无法区分不同调用位置的同名组件**。

#### traceKey 验证（2026-03-25）

- `traceKey`（`traceEventStart` 的第一个 Int 参数）是编译器按**函数定义位置**生成的静态 hash
- LazyColumn 里所有 ScrollListItem 的 traceKey **完全相同**（`-825905589`）
- 不同调用位置的 ActionButton（line 237 vs line 435）traceKey **也完全相同**（`828366394`）
- `info` 字符串的行号始终指向函数定义行（line 559），不是调用行
- 结论：`name + traceKey` 与按函数名聚合**完全等价**，无法区分任何实例

#### RecomposeScope 验证（2026-03-25）✅

**验证 1：不同调用位置的同名组件**

修改 Demo 让 ActionButton 在不同 Section 都触发重组，验证结果：

| 调用位置 | scopeHash | 稳定性 |
|---------|-----------|--------|
| CounterSection 内的 ActionButton (line 237) | `1034559832` | ✅ 多次重组 hash 一致 |
| ViewModelSection 内的 ActionButton (line 435) | `1034584960` | ✅ 多次重组 hash 一致 |

不同调用位置 → 不同 scope hash，同一实例多次重组 → hash 稳定。

**验证 2：LazyColumn item 的 sub-composition**

CompositionObserver 会传递到 SubcomposeLayout 创建的 sub-composition（文档确认）。
LazyColumn item 滚动后被 State invalidate 时，每个 item 有独立的 scope：

| item | scopeHash | 说明 |
|------|-----------|------|
| ScrollListItem #1 | `959204992` | 独立 scope ✅ |
| ScrollListItem #2 | `959209024` | 独立 scope ✅ |
| ScrollListItem #3 | `959212840` | 独立 scope ✅ |

#### scopeKey 语义修正（2026-04-02）

之前结论"所有二次重组都有 scope hash"**不准确**，需要修正。

`getCurrentScopeKey()` 返回的是 `activeScopeStack` 顶部 scope 的 hashCode，即**当前帧中最近的失效祖先 scope**，而非"当前 Composable 自己的 scope"。

**`scopeKey` 为 null 的三种情况：**

| 场景 | 原因 |
|------|------|
| **首次 Composition** | `invalidationMap` 为空，`onBeginScopeComposition` 不被调用，`activeScopeStack` 始终空 |
| **参数驱动重组，且祖先链上没有 scope 在 `invalidationMap` 中** | 比如整个调用链都是参数传递驱动，没有 State 直接失效的 scope |
| **LazyColumn subcomposition（`CompositionObserver` 不覆盖的情况）** | 某些 subcomposition 路径下 observer 未传递 |

**`scopeKey` 非 null 的情况：**

当前帧 `invalidationMap` 里有祖先 scope 时，`activeScopeStack` 有值，`scopeKey` 为该祖先 scope 的 hashCode。同一祖先 scope 驱动的所有子 Composable 共享相同的 `scopeKey`，但加上 `composableName` 前缀后可以区分。

**实际验证（2026-04-02）：**

```
ActionButton scopeKey=null       parent=<anonymous>        ← 祖先不在 invalidationMap
ActionButton scopeKey=668715544  parent=<anonymous>        ← 某祖先在 invalidationMap
ActionButton scopeKey=668723248  parent=AutoIncrementSection ← AutoIncrementSection scope 失效
CountDisplay scopeKey=1098411336 parent=ObjectWithStateSection ← OWS scope 失效
LabelDisplay scopeKey=1098411336 parent=ObjectWithStateSection ← 同一祖先 scope，key 相同但 name 不同
```

#### 最终 Key 策略（2026-04-02 修订）

**统一按函数名聚合，key = `composableName`。**

曾尝试用 `composableName + scopeKey` 区分实例，但存在根本性问题：
- `scopeKey` 语义是"最近的失效祖先 scope"，State 失效驱动时有值，参数驱动时为 null
- 不同驱动方式下行为不一致：有些组件有 `#N` 序号、有些没有
- 用户体验差，反而让人困惑

**设计决策**：Overlay 定位是「快速直觉感知」工具，函数名粒度完全够用。实例级细节通过日志的 RECOMPOSED 输出查看（日志包含 parent 信息，可判断是哪个实例）。

### 计数维度

- **总次数**：会话开始到现在的累积，`reset()` 清零
- **最近1分钟**：滑动时间窗口，实时衰减

---

## 层级方案：利用 KuiklyUI 插槽系统

### 关键发现

KuiklyUI 的 Dialog/Popup 不走 Android WindowManager，而是通过 `SlotProvider` 插槽堆栈实现，**全部在同一 Activity ViewGroup 内渲染，后渲染的自动在最顶层**。

相关代码：
- `SlotProvider.kt` — 插槽注册与管理
- `ComposeContainer.ProvideContainerCompositionLocals()` — 按顺序渲染所有插槽

### 实现方案

在 `ComposeContainer` 插槽渲染的**末尾**加一个固定的 Profiler Overlay 插槽，业务所有 Dialog/Popup 都在普通插槽里，Overlay 永远最后渲染 = 永远在最顶层。

```kotlin
// ComposeContainer.ProvideContainerCompositionLocals() 末尾追加
@Composable
private fun ProvideContainerCompositionLocals(content: @Composable () -> Unit) {
    // ... 现有插槽渲染 ...
    slots.forEach { it.render() }  // 业务 Dialog/Popup

    // Profiler Overlay — 始终最后渲染，覆盖所有业务弹层
    if (RecompositionProfiler.isOverlayEnabled) {
        ProfilerOverlaySlot()
    }
}
```

**优点**：
- 无需 Android 权限
- 跨平台一致（iOS/鸿蒙同样走 SlotProvider 体系）
- 无额外 Box 层级，不影响业务布局树

---

## Overlay 自身状态过滤

Overlay 读取 Profiler 数据时会触发自身重组，不应计入追踪。

过滤策略：`RecompositionTracker.isFrameworkComposable()` 中追加 `overlayPrefixes` 列表，
匹配 `com.tencent.kuikly.compose.profiler.` 包下所有 Composable，始终过滤。

---

## UI 刷新架构

### 核心难题

在 `render()` 内部写 Compose State 无法正确触发 recomposition：
- `render()` 被 `postponeInvalidation { }` 包裹，`isInvalidationDisabled = true`
- 在此期间写 Compose State（`mutableStateOf`/`mutableIntStateOf`），apply 通知被 postpone
- `postponeInvalidation` 结束时的 `invalidateIfNeeded()` 检查已完成，写入的变化不会调度新帧

### 解决方案

数据写入和 UI 刷新分离到不同时间点：

1. **数据写入**（`onFrameComplete`）：只写普通 Map（`instanceCounts`），发生在 apply callback 或 `onFrameEnd` 里，不涉及 Compose State
2. **UI 刷新**（`flushIfNeeded`）：在 `render()` 的 `postponeInvalidation` **之后**调用（此时 `isInvalidationDisabled = false`），写 `dataVersion`（`mutableIntStateOf`）+ 调用 `invalidateIfNeeded()` 通知原生层调度新帧
3. **Composable 订阅**：`ProfilerOverlaySlot` 在顶层读 `strategy.dataVersion` 建立订阅，`dataVersion` 变化触发重组，重组时从普通 Map 读取最新数据

```
onFrameComplete (apply callback / onFrameEnd)
  → 写 instanceCounts (普通 Map), hasPendingUpdate = true

render() {
    postponeInvalidation {
        // ... 正常渲染 ...
    }
    // postponeInvalidation 之后，isInvalidationDisabled = false
    tracker.notifyOverlayIfNeeded()
      → OverlayOutputStrategy.flushIfNeeded()
        → if (hasPendingUpdate) { dataVersion++; hasPendingUpdate = false }
    invalidateIfNeeded()  // 通知原生层调度新帧
}
```

### Sub-composition 采样修复

LazyColumn item 的首次组合发生在 `onFrameEnd` 之后的 apply callback 路径，
此时 `currentFrameSampled` 已被重置为 false，导致 `traceEventStart` 被跳过。

修复：在 apply observer 开头检测 `currentFrameSampled == false && changedObjects.isNotEmpty()` 时，
主动恢复采样（`currentFrameSampled = true`）并添加新的 `FrameStartEvent`，
确保 sub-composition 的重组也能被正确捕获。

---

## 热点统计口径（已实现）

### 外部 FAB 计数

累计总重组次数，所有非匿名、非框架 Composable 都计入（含首次组合和参数驱动重组）。

### 展开面板热点列表

- **按函数名聚合**，key = `composableName`，同名多实例合并统计
- 过滤 `<anonymous>` 匿名 lambda 和框架内部 Composable（`rememberAsyncImagePainter`、`painterResource`、`collectAsState`、`viewModel` 等）
- 按总次数降序，最多展示 `overlayTopCount` 条（默认 5）
- 每条显示：名称（左）、重组次数（右）、源码声明位置（第二行灰色小字）

**为什么不做实例级区分**：Compose Runtime API 的根本限制——不同驱动方式（State 失效 vs 参数驱动）下实例 key 的可用性不一致，强行区分会导致有些组件有 `#N` 序号、有些没有，行为不统一反而让用户困惑。Overlay 定位是快速直觉感知，函数名粒度够用。实例级细节看日志的 RECOMPOSED 输出（含 parent 信息）。

---

## 实际实现的文件变更

| 文件 | 变更 |
|------|------|
| `RecompositionConfig.kt` | 新增 `enableOverlay`（默认 false）、`overlayTopCount`（默认 5） |
| `RecompositionEvent.kt` | `ComposableRecomposedEvent` 新增 `scopeKey: Int?` |
| `ProfilerCompositionObserver.kt` | 新增 `getCurrentScopeKey()` |
| `RecompositionTracker.kt` | event 填入 scopeKey；`overlayPrefixes` 过滤；`notifyOverlayIfNeeded()`；apply observer 恢复采样 |
| `RecompositionOutputStrategy.kt` | 无新增接口（`onFrameTick` 已删除） |
| `OverlayOutputStrategy.kt` | 完全重写：纯数据容器 + `flushIfNeeded()` 写 `dataVersion` |
| `ProfilerOverlay.kt` | 新建：悬浮按钮（可拖拽、边界限制）+ 展开面板 |
| `RecompositionProfiler.kt` | `overlayStrategyState`（Compose State 驱动显隐）；`start()` 自动创建 OverlayOutputStrategy |
| `ComposeContainer.kt` | 末尾插入 `ProfilerOverlaySlot` |
| `BaseComposeScene.kt` | `render()` 末尾调用 `notifyOverlayIfNeeded()` + `invalidateIfNeeded()` |
| `LogOutputStrategy.kt` | 去掉 `=== Recomposition Frame ===` 行 |

---

## 已知限制

- **函数名粒度**：热点列表按函数名聚合，同名多实例合并统计。实例级细节看日志
- **`scopeKey` 不一致性（Compose Runtime 硬限制）**：State 失效驱动时有祖先 scope key，参数驱动时为 null；traceKey/sourceLocation/parentName 均无法可靠区分不同调用点的同名实例，统一按函数名聚合是最合理的选择
- **`sourceLocation` 是声明位置**：编译器 `info` 字符串的行号是函数定义行，不是调用行
- 数据是全局单例（`RecompositionProfiler`），多页面跳转时数据累积；可通过 Reset 按钮清空
- Profiler OFF 时零开销（`CompositionTracer.isTraceInProgress()` 返回 false，编译器注入的 trace 调用被跳过）
