## 1. compose 模块 — 数据模型与核心基础

- [x] 1.1 创建 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/` 包目录结构
- [x] 1.2 实现 `RecompositionEvent.kt` — 定义事件数据模型层次（`RecompositionFrameStartEvent`、`RecompositionFrameEndEvent`、`ComposableRecomposedEvent`（含 `parentName`、`sourceLocation`、`triggerStates`、`reason`、`paramChanges` 字段）、`StateChangedEvent`）
- [x] 1.3 实现 `RecompositionReport.kt` — 定义结构化分析报告数据类，包含 `sessionId`、`durationMs`、`totalRecompositions`、`composables` 聚合列表（含 `paramChangeFrequency`）、`hotspots` 列表、`stateChanges` 时间线，以及 `toJson()` 序列化方法
- [x] 1.4 实现 `RecompositionConfig.kt` — 定义配置数据类，包含 `sampleRate`、`hotspotThreshold`、`includeFrameworkComposables`、`enableStateTracking`、`enableStateIdentity`、`maxEventBufferSize` 等可配置项
- [x] 1.5 实现 `DirtyFlagsParser.kt` — 解析编译器注入的 `$dirty` bitmask，输出 `ParamChangeSummary`（`totalParams`、`changedParams`、`unknownParams`），用于参数级变更检测

## 2. compose 模块 — 追踪引擎核心

- [x] 2.1 实现 `RecompositionTracker.kt` — 追踪引擎核心类，内部实现 `CompositionTracer` 接口接收编译器 traceEventStart/End 回调，维护 trace 栈记录嵌套 Composable 调用，管理事件缓冲区，实现 Snapshot 观察者注册和采样率控制
- [x] 2.2 实现 `ProfilerCompositionObserver.kt` — 实现 `CompositionObserver` 接口，在 `onBeginComposition(invalidationMap)` 中保存精确 scope→State 映射并为每个 invalidated scope 注册 `RecomposeScopeObserver`，维护 `activeScopeStack`，暴露 `getCurrentScopeTriggerStates()` 供 tracker 查询
- [x] 2.3 实现 `StateIdentityRegistry.kt` — 基于 `identityHashCode` 为每个 State 对象分配递增短 ID（`State#1`、`State#2`...），session 内稳定；记录 State→Composable reader 映射；`formatState()` 输出 `State#1(value=3), readers: CounterSection` 格式
- [x] 2.4 修改 `ComposeSceneRecomposer.kt` — 在 `performScheduledTasks()` 前后添加重组帧边界追踪 hook（`onFrameStart` / `onFrameEnd`），注册 CompositionTracer
- [x] 2.5 修改 `BaseComposeScene.kt` — 在 `setContent()` 后通过 `Composition.observe()` 注册 `ProfilerCompositionObserver`，在 `close()` 中 dispose handle，实现 `ProfilerLifecycleListener` 接口响应 profiler 启停
- [x] 2.6 实现框架 Composable 过滤 — 在 `RecompositionTracker` 中维护 `frameworkPrefixes` 列表，根据 `includeFrameworkComposables` 配置在 `onComposableTraceEnd()` 中过滤
- [x] 2.7 实现精确 triggerStates 优先级逻辑 — `onComposableTraceEnd()` 中优先从 `ProfilerCompositionObserver.getCurrentScopeTriggerStates()` 获取精确触发 State，fallback 到 `currentFrameStateChanges`（帧级别粗粒度）
- [x] 2.8 实现参数级变更检测 — `onComposableTraceEnd()` 中调用 `DirtyFlagsParser.parse(dirty1, dirty2)` 解析参数变更，写入 `ComposableRecomposedEvent.paramChanges`，并在 `MutableComposableAccumulator` 中聚合 `paramChangeFrequency`
- [x] 2.9 实现 `stoppedTracker` 快照 — `stop()` 时保存 tracker 引用，使 `getReport()` 在 stop 之后仍能返回完整报告
- [x] 2.10 实现 `stateChangeAccumulator` 容量上限 — `MAX_STATE_CHANGE_ENTRIES = 500`，超限后已有 key 继续更新计数，新 key 拒绝写入，防止内存无限积累

## 3. compose 模块 — 开发者 API（RecompositionProfiler）

- [x] 3.1 实现 `RecompositionProfiler.kt` — 单例对象，提供 `start()`、`stop()`、`isEnabled`、`configure {}`、`getReport()`、`reset()` 等公开 API，保证幂等和线程安全（`SynchronizedObject`）
- [x] 3.2 实现 `RecompositionOutputStrategy.kt` — 定义输出策略接口，包含 `onFrameComplete(events)` 和 `onReportReady(report)` 方法
- [x] 3.3 实现 `RecompositionProfiler` 的策略管理 — `addOutputStrategy()`、`removeOutputStrategy()` 等 API
- [x] 3.4 实现 `ProfilerLifecycleListener` 机制 — 使 `start()/stop()` 能通知已存在的 Scene 重新注册/取消 CompositionObserver

## 4. compose 模块 — 输出策略实现

- [x] 4.1 实现 `output/LogOutputStrategy.kt` — 结构化重组日志，格式：`RECOMPOSED: Name @File.kt:line (Xms) [parent=<unknown>] params=[no params change] triggers=[...]`，report 输出每个 Composable 的 params 和 state 变更摘要
- [x] 4.2 实现 `output/JsonOutputStrategy.kt` — 将事件数据序列化为 JSON 格式，支持 `report.toJson()` 输出；`frameJsonBuffer` 缓存帧事件供外部消费
- [x] 4.3 实现 `output/OverlayOutputStrategy.kt` — 基于 Compose overlay 层实现 UI 热力图（Experimental）

## 5. demo 模块 — 示例页面

- [x] 5.1 创建 `RecompositionProfilerDemoPage.kt` — 外层 LazyColumn，包含多个测试场景 Section
- [x] 5.2 在 Demo 页面中演示 Start / Stop / Get Report / Reset 交互按钮
- [x] 5.3 在 Demo 页面中展示 `getReport().toJson()` 的 JSON 输出结果
- [x] 5.4 `CounterSection` — 基础计数器，验证 State 变更触发重组
- [x] 5.5 `AutoIncrementSection` — 定时自动递增，验证高频重组采样
- [x] 5.6 `ParentChildDemo` — 父子参数传递场景，验证 `params changed` 检测（`TwoParamChild`、`ThreeParamChild`、`MiddleLayer`、`GrandChild`、`LambdaChild`）
- [x] 5.7 `ScrollSection` — LazyColumn 30 条数据，tap 高亮，验证滚动场景
- [x] 5.8 `ViewModelSection` — StateFlow + collectAsState，验证外部 State 追踪（`ViewModelCounterDisplay`、`ViewModelUserDisplay` 独立订阅互不影响）
- [x] 5.9 `CustomStateSection` — `data class UserProfile` 整体作为 State，验证"State 包对象"模式
- [x] 5.10 `ObjectWithStateSection` — 普通 class `AppCounter` 内部属性为 `mutableStateOf`，验证"对象包 State"模式，`CountDisplay`/`LabelDisplay` 各自独立重组

## 6. State 身份标识

- [x] 6.1 调研 State 标识方案
- [x] 6.2 在 `RecompositionConfig` 中新增 `enableStateIdentity: Boolean = false` 配置项
- [x] 6.3 实现 `StateIdentityRegistry` — 基于 `identityHashCode` 分配稳定短 ID
- [x] 6.4 实现 State→Composable reader 映射 — 在 `onComposableTraceEnd()` 中结合 `getCurrentScopeTriggerStateObjects()` 记录
- [x] 6.5 修改 `ProfilerCompositionObserver.stateToString()` — 开启时输出 `State#1(value=3), readers: CounterSection` 格式
- [x] 6.6 修改 `RecompositionReport` — 报告中 `triggerStates` 包含 State 身份信息
- [x] 6.7 iOS 模拟器验证 — `enableStateIdentity = true`，验证输出包含稳定 ID 和 reader 名称

## 7. 跨平台测试验证

- [x] 7.1 iOS 模拟器验证 — 日志输出正常、CompositionTracer 自动追踪生效、CompositionObserver 精确 triggerStates 生效、框架过滤生效、params changed 检测生效、stop 后 getReport 正常、enableStateIdentity 生效、LazyColumn 滚动重组日志正常（Snapshot.registerApplyObserver flush 方案）。重复输出修复后重新验证通过：每帧只输出一次，无重复 Frame block。
- [x] 7.2 Android 平台验证 — JVM 模式下验证通过，日志 tag 为 `RecompositionProfiler`，`State(prev=x, now=y)` 格式正确，CounterSection/AutoIncrementSection/ParentChildDemo 全部正常。同时发现并修复**重复输出 bug**：Android 上 `onFrameEnd` 和 `Snapshot.registerApplyObserver` 两条 flush 路径对同一帧都会触发，导致每帧被输出两次（2 套 Frame block + 1 个多余 END）。修复方案：`flushCurrentFrameEvents()` flush 后立即从 `events` 缓冲区删除已输出的事件（从 FrameStart 到末尾），第二次调用时找不到 FrameStart 直接返回空，彻底防止重复。同时删除了已无调用的 `getCurrentFrameEvents()` 方法。
- [x] 7.3 HarmonyOS 平台验证 — 模拟器（HarmonyOS 5.0.2 API 14）验证通过。完整流程：`2.0_ohos_demo_build.sh` 生成 libshared.so → DevEco Studio 配置签名（用户一次性操作）→ CLI 全程接管（hvigorw 构建、hdc 安装、hilog 抓日志）。日志 tag 为 `A01234/RecompositionProfiler`，所有功能与 iOS/Android 一致：prev→now 格式、readers、params changed、无重复帧均正常。
- [~] 7.4 性能影响验证 — Profiler OFF 时 `RecompositionProfiler.isEnabled` 为 false，`BaseComposeScene.render()` 里 tracker 引用不获取，`CompositionTracer.isTraceInProgress()` 返回 false，编译器注入的 traceEventStart/End 均短路，理论零开销。代码层面已验证，不做 benchmark。
- [~] 7.5 线程安全验证 — `RecompositionProfiler` 用 `SynchronizedObject` 保护 start/stop/configure，`render()` 里直接读 tracker（单线程 Compose render 线程，无并发风险）。设计说明已在注释中记录，不做额外压测。

## 8. 待优化问题（测试完成后评估）

- [~] 8.1 **首次 Composition 盲区** — 已调研，存在根本性 Runtime API 限制：
  - `CompositionObserver.onBeginComposition` 只在有待重组 scope 时才被调用，纯 initial composition（页面首次加载）不触发此回调，因此 `hasPreciseScopeMapping=false`，traceEventStart/End 回调也不在 precise 路径内
  - LazyColumn 滑入新 item 看似是 initial composition，实际是 LazyColumn 内部 scroll/itemProvider state 变化触发的 recomposition，scope 在 `invalidationMap` 中，无法区分
  - `[initial]` 标记在现有 Compose Runtime API 下无法可靠实现，暂不处理
- [x] 8.2 **State 定位体验** — 删除无意义的 `State#N` 序号，改为 `State(prev=x, now=y), readers: Name` 格式；`StateIdentityRegistry` 重构为只记录 prevValue + readers，删除 ID 分配逻辑；同步删除 `enableStateIdentity` 配置项（功能现已默认常驻）。
  - **prev→now 时序问题修复**：精确路径（CompositionObserver）下 `traceEventEnd` 在 apply 之后执行，此时 registry prevValue 已被 `updateLastSeenValue` 覆盖。解决方案：在 apply callback 中，`formatState()` 在 `updateLastSeenValue` 之前调用（此时 prevValue 是正确的旧值），将格式化结果缓存到 `stateChangeCache`（`Map<identityHashCode, String>`），`traceEventEnd` 精确路径查此 cache 获取正确的 prev→now 字符串。Cache miss 时降级调 `formatState`。每帧结束时清空 cache。
  - 验证结果：`State(prev=1, now=2), readers: CounterSection`、`State(prev=Bob, now=Alice), readers: ParentChildDemo` 等输出正确
- [x] 8.3 **性能小优化** — `events.removeAt(0)` 改为 `ArrayDeque` + `removeFirst()`，O(n) → O(1)
- [x] 8.4 **内存小优化** — `JsonOutputStrategy` 新增 `maxFrameJsonBufferSize` 参数（默认 1000），超限丢弃最旧条目
- [x] 8.5 **重复代码** — 新建 `ProfilerJsonUtils.kt` 提取公共 `escapeJson()`，`RecompositionReport` 和 `JsonOutputStrategy` 统一引用
- [x] 8.6 **参数校验** — `maxEventBufferSize` 校验范围改为 `1..1_000_000`，防止 OOM

## 9. Code Review 遗留问题

- [x] 9.1 **C1：`DirtyFlagsParser` 未显式处理 legacy `-1` sentinel** — `dirty1=-1` 时依赖副作用返回 null，应改为 `if (dirty1 == 0 || dirty1 == -1) return null`（`DirtyFlagsParser.kt`）
- [~] 9.2 **I1：`PARENT_RECOMPOSITION` reason** — 不需要实现。`paramChanges` 已包含参数变更信息（`params changed: [#0]` + `triggers=[]`），用户可直接判断是父级重组导致，reason 枚举值增加一个仅重复此信息。`RecompositionReason` 只保留 `STATE_CHANGE` / `UNKNOWN` 两个值。
- [~] 9.3 **I2：`enableStateIdentity` 配置项** — 不重新引入开关。8.2 重构后 `StateIdentityRegistry` 改为轻量常驻（仅记录 prevValue + readers），开销可忽略，无需按需关闭。文档对齐：6.2/6.3/6.5/6.7 相关条目已过时。
- [x] 9.4 **I4：`INITIAL_COMPOSITION` 死代码枚举值** — 从 `RecompositionReason` 中删除，同步删除 `LogOutputStrategy` 里 `initialMarker` 相关代码
- [x] 9.5 **I5：`StateChangedEvent` 死代码** — 删除 class 定义，删除 `LogOutputStrategy` / `JsonOutputStrategy` 中对应的 `when` 分支和 import
- [x] 9.6 **C2：`composableAccumulator` / `stateIdentityRegistry` 无上限** — 加注释说明：key 为函数名（非实例），条目数有自然上限，不会无限增长

## 10. 后续功能规划

- [x] 10.1 **Overlay 热点高亮** — 对重组次数超过阈值（默认 5 次）的 Composable 显示红色边框 + 计数，点击展示组件名和 triggers；Overlay 自身重组需过滤。方案细节见 `specs/overlay-highlight/spec.md`，待参考官方 Android Studio 插件体验后最终决策。
- [x] 10.2 **补充官网文档** — 完善 `docs/Compose/recomposition-performance.md`，内容包括：工具背景与使用场景、快速开始（start/stop/getReport API 示例）、配置项说明（sampleRate/hotspotThreshold/enableOverlay 等）、输出格式解读（日志格式、JSON 报告字段说明）、Overlay 热点面板使用说明、进阶用法（自定义 OutputStrategy、State 身份追踪）。

## 12. 运行时上下文数据（让 AI 能判断重组是否合理）

背景：当前 profiler 只有重组次数/耗时/triggerStates，缺乏操作行为上下文，AI 无法判断
"ScrollListItem 重组 48 次"是正常还是异常（取决于用户滑动了几格），
"AnimateColorDemo 重组 57 次"是正常还是异常（取决于动画时长）。

方案见 specs/runtime-context/spec.md（待创建）。

- [ ] ~~12.1 **LazyColumn 滚动上下文**~~ → **延后**：依赖 `recomposition-analyzer` skill 可用后再实现，移到独立 change。
- [ ] ~~12.2 **动画上下文**~~ → **延后**：同上，移到独立 change。


- [x] 11.0 **验证各平台文件读取通道**（iOS 模拟器 ✅、iOS 真机 ✅、Android 模拟器 ✅；HarmonyOS 待验证）
- [x] 11.1 **新建 FileModule + FileOutputStrategy** — `FileModule` 提供 `writeFile`/`appendFile`/`getFilesDir`；iOS/Android 原生实现写入 `Caches/KuiklyProfiler/`；`FileOutputStrategy` 每 2s 批量 append 帧数据到 `profiler_frames.jsonl`，`stop()`/`getReport(saveToFile=true)` 写 `profiler_report.json`；`RecompositionConfig` 新增 `enableLog`/`enableFile`（默认 true）；`start()` 自动装配 `LogOutputStrategy` 和 `FileOutputStrategy`；`JsonOutputStrategy` 已删除（职责合并入 `FileOutputStrategy`）；HarmonyOS 原生实现待补充
- [x] 11.2 **recomposition-analyzer Skill（初版占位）** — 已创建 `.claude/skills/recomposition-analyzer`，包含基础的文件拉取命令和简单分析框架；需在 11.3 中升级为完整版
- [x] 11.3 **recomposition-analyzer Skill（完整版）** — 重写 skill，实现完整的六步分析工作流：
  1. 用户声明平台（iOS模拟器/真机/Android/HarmonyOS）和设备类型，自动拉取 `profiler_report.json` + `profiler_frames.jsonl`，按时间戳存到 `/tmp/kuikly_profiler/`
  2. 基础报告解析（概览：总帧数、总重组次数、时长）
  3. 11条可疑项识别（热点绝对排名、相对占比、突发重组、paramChanges不稳定、State粒度粗、maxDurationMs偶发卡顿、avgDurationMs超帧、UNKNOWN reason占比、集合/var字段参数、Lambda无remember、LazyColumn无key）
  4. 深度分析（grep热点组件源码 + frames辅助验证，能从代码判断的跳过frames）
  5. 输出中文分析报告（概览 + 可疑项 + 每条问题的结论 + 修复方案）
  6. 询问下一步：直接修复→改代码→提示用户重跑profiler→拉新文件（时间戳命名）→对比前后→确认修复→检测remote是否工蜂→发MR
