## Context

本设计针对 **Compose DSL 模式**（`ComposeContainer` + `setContent{}`），为 Kuikly Compose 开发者提供运行时重组性能分析能力。

**当前架构状态：**
- Kuikly Compose 基于 Jetpack Compose 1.7.3 魔改版，核心重组流程通过 `ComposeSceneRecomposer` 管理
- `ComposeSceneRecomposer` 封装了 `Recomposer`，通过 `FlushCoroutineDispatcher` 调度重组和副作用
- `ComposeContainer`（继承自 `Pager`）是 Compose DSL 的入口，通过 `ComposeSceneMediator` 桥接场景
- `KuiklyApplier` 负责将 Compose 树变更应用到 `KNode<DeclarativeBaseView>` 节点树
- 已有 `PageEventTrace` / `PageCreateTrace` 机制追踪页面级事件，但不覆盖 Compose 重组维度
- 项目为 KMP 架构，`commonMain` 代码跨 Android/iOS/HarmonyOS/Web/macOS 全平台生效

**关键约束：**
- Compose Runtime 包名为 `androidx.compose.runtime.*`（官方 runtime），业务层为 `com.tencent.kuikly.compose.*`
- 不修改 Compose Compiler 插件，仅在运行时层面实现
- 需考虑性能开销，追踪工具不应显著影响被分析应用的性能

## Goals / Non-Goals

**Goals:**
- 提供 Composable 级别的重组追踪：记录哪些 Composable 发生了重组、重组次数、耗时
- 提供重组原因追踪：识别触发重组的 State 变更来源
- 支持多种输出形态：日志（Log）、UI Overlay（热力图）、结构化数据（JSON）
- 提供编程 API，支持 AI Agent 程序化调用分析工具
- 追踪功能可动态开关，关闭时零性能开销

**Non-Goals:**
- 不修改 Compose Compiler 或生成额外编译产物
- 不覆盖自研 DSL 模式（`Pager` + `body()`）
- 不做持久化存储或远程上报（后续可扩展）
- 不做自动修复或代码建议

## Decisions

### Decision 1: 追踪机制 — 基于 CompositionTracer + CompositionObserver + Snapshot 系统

**选择：** 采用三层追踪机制组合：
1. **CompositionTracer 层（自动追踪）**：通过 `@InternalComposeTracingApi` 的 `CompositionTracer` 接口，接收编译器注入的 `traceEventStart/End` 回调，**自动覆盖所有 @Composable 函数**的重组追踪，无需手动包装
2. **CompositionObserver 层（精确重组原因）**：通过 `@ExperimentalComposeRuntimeApi` 的 `CompositionObserver` + `RecomposeScopeObserver`，利用 `invalidationMap: Map<RecomposeScope, Set<Any>?>` 实现精确的 **scope→State** 重组原因追踪
3. **Snapshot 层（兜底 State 追踪）**：通过 `Snapshot.registerApplyObserver` 监听 State 变更，作为 CompositionObserver 不可用时的 fallback 粗粒度关联

**实际实现（已完成）：**
- `CompositionTracer` 在 `RecompositionTracker` 内部实现，注册到 Compose Runtime 后自动接收所有 Composable 的 traceEventStart/End 回调
- `ProfilerCompositionObserver` 实现 `CompositionObserver`，在 `onBeginComposition(invalidationMap)` 中为每个 invalidated scope 注册 `RecomposeScopeObserver`（通过 `scope.observe()`），维护 activeScopeStack 和 scopeToStatesMap
- `RecompositionTracker.onComposableTraceEnd()` 优先从 `ProfilerCompositionObserver.getCurrentScopeTriggerStates()` 获取精确的触发 State，fallback 到 `currentFrameStateChanges`（帧级别粗粒度）

**替代方案考虑：**
- ~~方案 A：修改 Compose Compiler 插件~~ — 违反 non-goal，且维护成本高
- ~~方案 B：纯 AOP/字节码插桩~~ — KMP 跨平台不适用
- ~~方案 C（初版）：TrackRecomposition 手动包装~~ — 需要开发者手动包装每个 Composable，使用门槛高，已被 CompositionTracer 自动追踪取代
- **方案 D（当前）：CompositionTracer + CompositionObserver** — 完全自动化，零开发者侵入，精确重组原因

**理由：** 方案 D 完全在 `commonMain` 层面实现，跨全平台生效。CompositionTracer 利用编译器已注入的 traceEventStart/End 回调实现零侵入自动追踪；CompositionObserver 提供精确的 scope→State 映射，解决了 Snapshot 观察者的帧级别粗粒度问题。

### Decision 2: 产品形态 — 三种输出模式独立可组合

**选择：** 输出层采用策略模式（`RecompositionOutputStrategy` 接口），三种输出模式可独立启用/组合：

| 模式 | 目标用户 | 实现方式 |
|------|---------|---------|
| **Log 模式** | 开发者调试 | 通过 `KLog` 输出结构化日志，包含组件名、重组次数、耗时、触发 State |
| **UI Overlay 模式** | 开发者直观定位 | 在根 `DivView` 上叠加半透明 Composable overlay，用颜色编码显示重组热度 |
| **JSON API 模式** | AI Agent / 外部工具 | 暴露 `getReport(): RecompositionReport` API，返回结构化数据 |

**替代方案考虑：**
- ~~单一日志模式~~ — 不够直观，AI 难以消费
- ~~仅 UI overlay~~ — 无法程序化调用
- **三模式组合（当前）** — 覆盖所有使用场景，灵活性最高

### Decision 3: 追踪粒度 — CompositionTracer 自动全覆盖 + 框架过滤

**选择：**
1. **自动全覆盖**：通过 `CompositionTracer` 自动接收所有 @Composable 函数的重组通知，无需开发者手动包装
2. **框架 Composable 过滤**：通过 `RecompositionConfig.includeFrameworkComposables` 配置项控制是否包含框架内部 Composable（`androidx.compose.*`、`com.tencent.kuikly.compose.*` 等前缀），默认过滤掉以减少噪音
3. **TrackRecomposition（保留兼容）**：仍保留 `TrackRecomposition(name)` 包装器供特定场景使用，但主要追踪方式已切换为 CompositionTracer 自动追踪

**理由：** CompositionTracer 利用 Compose 编译器已注入的 traceEventStart/End 回调，实现零侵入全覆盖追踪，大幅降低使用门槛。框架过滤机制让开发者可以聚焦业务 Composable 而非被框架内部重组淹没。

### Decision 4: 数据模型 — 与 PageEventTrace 体系对齐

**选择：** 重组事件模型参照已有的 `PageEventTrace` 设计模式，定义 `RecompositionEvent` 类层次：

```
RecompositionEvent (base)
├── RecompositionFrameStartEvent   // 重组帧开始
├── RecompositionFrameEndEvent     // 重组帧结束
├── ComposableRecomposedEvent      // 单个 Composable 重组
│   ├── composableName: String
│   ├── durationMs: Long
│   └── triggerStates: List<String>
└── StateChangedEvent              // State 变更
    ├── stateKey: String
    └── sourceLocation: String?
```

**理由：** 与现有 trace 基础设施风格一致，降低学习成本。

### Decision 5: 性能控制 — 全局开关 + 采样

**选择：**
- 全局 `RecompositionProfiler.isEnabled` 开关，关闭时所有追踪代码短路，接近零开销
- 可选采样率配置（`sampleRate: Float`），在高频重组场景下减少数据量
- Debug 模式下默认关闭，需显式开启

**理由：** 作为开发工具，必须确保默认不影响生产性能。

## File Changes by Module

### `compose` 模块（核心改动）
| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `compose/src/commonMain/.../profiler/RecompositionProfiler.kt` | 新增 ✅ | 主入口 API，控制追踪启停、配置、获取报告；含 ProfilerLifecycleListener 机制 |
| `compose/src/commonMain/.../profiler/RecompositionTracker.kt` | 新增 ✅ | 追踪引擎核心，基于 CompositionTracer 接收编译器回调，维护事件缓冲区和统计聚合 |
| `compose/src/commonMain/.../profiler/ProfilerCompositionObserver.kt` | 新增 ✅ | CompositionObserver + RecomposeScopeObserver 实现，提供精确 scope→State 重组原因追踪 |
| `compose/src/commonMain/.../profiler/RecompositionEvent.kt` | 新增 ✅ | 事件数据模型（含 ComposableRecomposedEvent.parentName 字段） |
| `compose/src/commonMain/.../profiler/RecompositionReport.kt` | 新增 ✅ | 结构化分析报告 |
| `compose/src/commonMain/.../profiler/RecompositionConfig.kt` | 新增 ✅ | 配置数据类，含 `includeFrameworkComposables` 框架过滤选项 |
| `compose/src/commonMain/.../profiler/RecompositionOutputStrategy.kt` | 新增 ✅ | 输出策略接口 |
| `compose/src/commonMain/.../profiler/output/LogOutputStrategy.kt` | 新增 ✅ | 日志输出策略 |
| `compose/src/commonMain/.../profiler/output/OverlayOutputStrategy.kt` | 新增 ✅ | UI Overlay 输出策略 |
| `compose/src/commonMain/.../profiler/output/JsonOutputStrategy.kt` | 新增 ✅ | JSON/API 输出策略 |
| `compose/src/commonMain/.../profiler/TrackRecomposition.kt` | 新增 ✅ | 保留的手动追踪包装器（已非主要追踪方式） |
| `compose/src/commonMain/.../ComposeContainer.kt` | 修改 ✅ | 注入 Profiler CompositionLocal |
| `compose/src/commonMain/.../ui/scene/ComposeSceneRecomposer.kt` | 修改 ✅ | 添加重组帧追踪 hook |
| `compose/src/commonMain/.../ui/scene/BaseComposeScene.kt` | 修改 ✅ | setContent 后注册 CompositionObserver，close 中 dispose |

### `core` 模块（可选扩展）
| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `core/src/commonMain/.../pager/PageCreateTrace.kt` | 未修改 | 暂未扩展（可选） |

### `demo` 模块
| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `demo/src/commonMain/.../compose/RecompositionProfilerDemoPage.kt` | 新增 ✅ | 重组分析工具示例页面 |

## Risks / Trade-offs

**[Risk] Compose Runtime 内部/实验性 API 不稳定** → `CompositionTracer`（`@InternalComposeTracingApi`）、`CompositionObserver`（`@ExperimentalComposeRuntimeApi`）和 `Snapshot.registerApplyObserver` 可能在 Compose 版本升级时变化。**Mitigation**: KuiklyUI 锁定 Runtime 1.7.3，短期内不受影响；通过适配层封装 API 调用，升级时只需修改适配层。

**[Risk] 追踪开销影响性能分析准确性** → 追踪代码本身会引入耗时，可能影响测量精度。**Mitigation**: 追踪逻辑尽量轻量（仅记录时间戳和标识符），采样模式进一步降低开销。State 身份追踪（readObserver + identityHashCode）通过 `enableStateIdentity` 开关控制，默认关闭。

**[Risk] Composable 名称获取困难** → Compose Runtime 在 release 模式下可能混淆函数名。**Mitigation**: CompositionTracer 的 `info` 参数由编译器注入，包含函数名 + 源码位置（如 `"CounterSection (Demo.kt:195)"`），release 模式下需验证是否保留。

**[Risk] UI Overlay 在某些平台可能存在渲染差异** → overlay 在不同平台渲染器的表现可能不一致。**Mitigation**: Overlay 模式标记为 Experimental，优先保证 Log 和 JSON 模式的跨平台一致性。

## Open Questions

1. ~~**Snapshot 观察的粒度问题**~~ → **已解决**：通过 `CompositionObserver.onBeginComposition(invalidationMap)` + `RecomposeScopeObserver` 实现精确的 scope→State 映射，Snapshot 观察者仅作为 fallback。
2. ~~**UI Overlay 的交互形式**~~ → **已实现**：悬浮 FAB（颜色编码重组热度）+ 点击展开热点面板（列表 + 暂停/重置/打印报告操作）。
3. ~~**AI Agent 调用协议**~~ → **已实现**：`recomposition-analyzer` skill（`.claude/skills/recomposition-analyzer`），通过文件拉取 + 六步分析工作流实现 AI 程序化分析；12 组可疑项识别规则已定义。后续上下文增强（滚动/动画事件）移到独立 change。
4. ~~**State 源码位置标识**~~ → **已决定并实现**：`StateIdentityRegistry` 记录 prev/now value + readers Composable 名称，输出格式为 `State(prev=x, now=y), readers: CounterSection`。`enableStateIdentity` 配置项已删除，功能默认常驻（由 `enableStateTracking` 统一控制）。
