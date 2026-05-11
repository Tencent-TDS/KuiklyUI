# Recomposition Analyzer Skill — Implementation Handoff Prompt

## 如何使用

在新会话中发送：

```
请先阅读 /Users/qibu/.local/share/opencode/worktree/25ff8ae844627a433869ad4b0766ecb19edb9c2d/lucky-falcon/openspec/changes/recomposition-analyzer-skill/handoff-prompt.md 的完整内容，然后执行 /openspec-apply-change 开始实施 recomposition-analyzer-skill 这个变更。
```

---

## 项目背景

KuiklyUI 是腾讯开源的 Kotlin Multiplatform 跨端 UI 框架，支持 Android、iOS、HarmonyOS、Web、小程序。其中 Compose DSL 层（`compose/` 模块）有一个 Recomposition Profiler 工具，输出两个日志文件：

- **report.json**：聚合报告，包含每个 Composable 的重组次数、耗时、scopeDistribution、paramChangeFrequency、triggerStates（含 readers）等
- **frames.jsonl**：每帧详情，每行一个 JSON 对象（type=frame），包含 frameId、timestamp、duration、events（composable_recomposed 列表）

现在要做两件事：

1. **Profiler 侧**：在 frames.jsonl 中新增用户操作上下文事件（touch 和 scroll），帮助判断重组是否由用户操作触发
2. **Skill 侧**：创建 `kuikly-recomposition-analyzer` skill，运行在业务源码仓库，读取 profiler 日志 + 业务源码，三阶段漏斗分析重组性能问题

---

## OpenSpec 变更位置

所有 artifact 在：
```
/Users/qibu/.local/share/opencode/worktree/25ff8ae844627a433869ad4b0766ecb19edb9c2d/lucky-falcon/openspec/changes/recomposition-analyzer-skill/
```

包含：
- `proposal.md` — 变更提案
- `design.md` — 8 个关键决策（D1-D8）
- `specs/recomposition-analyzer-skill/spec.md` — Skill 侧规格（9 条 ADDED）
- `specs/profiler-context-events/spec.md` — Profiler 侧规格（6 条 ADDED）
- `tasks.md` — 实施任务（6 组）
- `exploration-notes.md` — 探索阶段的完整决策记录

---

## 核心设计决策速查

### 三阶段漏斗（D1）
1. **Stage 1 — report 筛查**：读 report.json，用阈值过滤出可疑组件
2. **Stage 2 — frames 深挖**：读 frames.jsonl，确认/补充嫌疑
3. **Stage 3 — 源码确认**：读业务源码，给出具体优化建议

### noScope 直接归正常（D3 补充）
report.json 中 `noScopeRecompositions` 字段记录的是首次组合（first composition），不是重组，**直接归为正常**，不需要额外 frames 验证。

### 可配置阈值（D4）
```
scopeCountThreshold=5     — scope 重组次数超过此值才关注
durationThreshold=5ms     — 单次重组耗时超过此值才关注
frameEventThreshold=50    — 单帧事件数超过此值才关注
frameDurationThreshold=16ms — 帧耗时超过此值才关注
paramChangeRateThreshold=0.9 — 参数变化率超过此值视为 lambda 不稳定
stateReadersThreshold=3   — State 的 readers 数超过此值视为广播
minFramesThreshold=30     — 帧数据少于此值给出健康检查警告
```

### 三条检测规则

| 规则 | 信号 | 来源 | 逻辑 |
|------|------|------|------|
| RULE-A | 单次重组耗时长 | report.duration | duration > durationThreshold |
| RULE-B | State 广播 | report.triggerStates + readers | readers 数 > stateReadersThreshold |
| RULE-C | Lambda 不稳定 | report.paramChangeFrequency | #N 参数变化率 > paramChangeRateThreshold |

### Frames 独立数据源（D2）
frames.jsonl 不仅用于确认 report 嫌疑，还能**独立产生新嫌疑**：
- 单帧事件数过多（> frameEventThreshold）
- 帧耗时长（> frameDurationThreshold）
- 连续帧中出现同一 scopeKey

### 上下文事件格式（D7/D8）
在 frames.jsonl 中插入独立行，与 frame 行交错：

**Touch 事件**：
```json
{"type":"touch_context","action":"touchBegin|touchEnd|touchCancel","timestamp":1234567890}
```

**Scroll 事件**：
```json
{"type":"scroll_context","firstVisibleItemIndex":5,"timestamp":1234567890}
```

### Profiler 侧 Hook 点

**Touch**：`RootNodeOwner.onPointerInput()` (L223-237)
- 在事件分发前后记录 touchBegin/touchEnd/touchCancel
- 仅记录这三类，不记录 move

**Scroll**：`LazyListState.applyMeasureResult()` (L506-539)
- 比较 apply 前后的 firstVisibleItemIndex
- 变化时写入 scroll_context 事件
- 需在 RecompositionProfiler 中新增 `recordScrollContext()` API

### 模式 C 实现（D5）
不靠 skill 推断"页面→组件"映射，而是引导用户使用 profiler 面板的 Reset 按钮，只捕获目标操作的重组数据。

---

## 关键源码文件

| 文件 | 用途 |
|------|------|
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/RecompositionProfiler.kt` | Profiler 入口，addOutputStrategy 在 L548-552 |
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/RecompositionTracker.kt` | Tracker 核心，apply observer 在 L670 |
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/RecompositionEvent.kt` | 3 种事件类型（frame_start, frame_end, composable_recomposed） |
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/output/FileOutputStrategy.kt` | JSONL 写入，appendEventJson 在 L157-199 |
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/node/RootNodeOwner.kt` | onPointerInput 在 L223-237（touch hook 点） |
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/foundation/lazy/LazyListState.kt` | firstVisibleItemIndex 在 L142，applyMeasureResult 在 L506-539（scroll hook 点） |
| `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/ProfilerCompositionObserver.kt` | 强制重组检测在 L150-155 |

---

## 真实日志示例

| 文件 | 场景 |
|------|------|
| `profiler_report_simple.json` | Demo app，简单场景 |
| `profiler_frames_simple.jsonl` | 44 帧 |
| `profiler_report_complex.json` | 新闻 app，77 composables，892 recompositions |
| `profiler_frames_complex.jsonl` | 57 帧，1715 events |

路径前缀：`/Users/qibu/.local/share/opencode/worktree/25ff8ae844627a433869ad4b0766ecb19edb9c2d/lucky-falcon/`

---

## 任务分组概览

| 组 | 内容 | 任务数 |
|----|------|--------|
| Group 1 | Profiler 上下文事件实现 | 7 |
| Group 2 | Skill 骨架 | 3 |
| Group 3 | Skill references 文档 | 6 |
| Group 4 | Profiler 上下文事件测试 | 20 |
| Group 5 | Skill 集成测试 | 13 |
| Group 6 | E2E 测试（基于 DemoPage） | 12 |

---

## 注意事项

1. **两种 DSL 不可混用**：本变更仅涉及 Compose DSL（`compose/` 模块），不涉及自研 DSL
2. **边界规则**：`compose/` 是纯 KMP 模块，禁止依赖 `core-render-*`
3. **sourceLocation 格式**：report.json 中的 sourceLocation 只有 `FileName.kt:lineNumber`，没有包路径，skill 需要用 Glob 在业务源码中定位
4. **scopeDistribution 的 key**：是 scopeKey 的 hashCode，不是 scopeKey 本身
5. **paramChangeFrequency 的 key**：是参数索引 `#N`，无法映射到参数名，skill 只能提示"第 N 个参数"
6. **frames.jsonl 时间戳精度**：同一毫秒内多帧的时间戳可能相同，不能严格按时间排序
7. **neverEqualPolicy**：`layoutInfoState` 使用 neverEqualPolicy 会导致大量 state change 记录，但这属于框架内部行为，本次变更不处理
8. **编译验证**：修改 `compose/` 模块后用 `./gradlew :compose:compileDebugKotlinAndroid` 验证

---

## 知识库索引

开始编码前务必查阅：
- Compose DSL：`.ai/compose-dsl/AGENTS.md`
- 编码规范：`.ai/coding-standards/AGENTS.md`
- Recomposition Profiler 详细文档：`.ai/references/recomposition-profiler.md`
