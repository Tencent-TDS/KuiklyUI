## Context

KuiklyUI Compose DSL 的 Recomposition Profiler 已能输出 `report.json`（聚合报告）和 `frames.jsonl`（逐帧详情），并提供 Overlay 面板实时查看。但业务开发者面对数百条重组记录时，缺乏自动化的分析工具来快速定位需要优化的组件。

本 change 包含两部分：
1. **recomposition-analyzer skill**：CodeBuddy Code skill，运行在业务仓库，读取 profiler 日志 + 业务源码，自动诊断重组问题
2. **profiler context events**：为 profiler 补充 touch / scroll 上下文事件，辅助 skill 判断重组是否正常

仅涉及 **Compose DSL**，不涉及自研 DSL。

## Goals / Non-Goals

**Goals:**
- 业务开发者跑完 profiler 后，通过 skill 一键获取重组问题诊断 + 优化建议
- 三阶段漏斗（report → frames → 源码）逐层收窄，避免全量读源码
- 可配置阈值，适配不同业务场景
- skill 知识内嵌，不依赖外部仓库
- profiler 新增 touch / scroll 上下文事件，写入 frames.jsonl
- 修复 `layoutInfoState`（neverEqualPolicy）对 state change 统计的污染

**Non-Goals:**
- 不自动修复代码
- 不处理自研 DSL
- 不处理框架自身 Composable / 外部仓库组件
- 不做"页面 → 组件名"智能匹配
- 动画上下文事件不在本次实现（延后）

## Decisions

### D1: 三阶段漏斗（非全量扫描）

**选择**：report 筛查 → frames 深挖 → 源码确认，逐层收窄

**理由**：业务规模类比腾讯新闻，几百上千条重组记录全量读源码不可行。每层用更贵的数据源，但只处理上一层筛出的子集。

**替代方案**：12 规则全量扫 + 每个都读源码 → 丢弃，太慢且浪费

### D2: frames 是独立数据源（非单向确认工具）

**选择**：frames 阶段既能确认 report 嫌疑，也能自己产出新嫌疑（帧耗时 > 16ms、帧事件数 > 50、单组件耗时 > 5ms）

**理由**：report.json 只有聚合数据，帧级信息（帧耗时、帧内事件数、同帧组件组合）只能从 frames.jsonl 获取

### D3: 筛选维度是"某具体 scope 的绝对计数"（非比例）

**选择**：遍历 `scopeDistribution`，找出计数 > `scopeCountThreshold` 的 scopeKey

**理由**：noScope 占比高不代表正常，占比低不代表异常。关键是有没有某个具体 scope 被反复触发。noScope 组件直接归入正常清单。

### D4: 所有阈值可配置

**选择**：skill 自带 `config.md`，列出默认阈值，调用时可参数覆盖

| 配置项 | 默认值 |
|--------|--------|
| `scopeCountThreshold` | 5 |
| `durationThreshold` | 5ms |
| `frameEventThreshold` | 50 |
| `frameDurationThreshold` | 16ms |
| `paramChangeRateThreshold` | 0.9 |
| `stateReadersThreshold` | 3 |
| `minFramesThreshold` | 30 |

**理由**：不同业务场景对"什么算问题"的容忍度不同，硬编码不合理

### D5: 形态 C 靠 profiler "重置"按钮（非智能匹配）

**选择**：用户想聚焦某页面时，引导他用 profiler 面板的"重置"按钮，重新采一段只含目标操作的日志

**替代方案**：skill 做"页面名 → 组件名"推断 → 丢弃，不可靠且复杂

### D6: 知识内嵌（非外链）

**选择**：skill 自带 `references/` 目录，包含规则、优化方案、日志格式、各平台获取命令

**替代方案**：运行时去读 KuiklyUI 仓库 → 丢弃，业务仓库大概率没有 KuiklyUI 源码

### D7: scroll 上下文用 applyMeasureResult 内部 hook（非 snapshotFlow）

**选择**：在 `LazyListState.applyMeasureResult()` 内部，`scrollPosition.updateFromMeasureResult()` 前后比较 `firstVisibleItemIndex`，同步回调

**理由**：
- 时序精确：同步执行，在 layout pass 内即时回调
- 对 Snapshot 无额外开销
- 对 profiler 无额外影响

**替代方案**：snapshotFlow → 不选，时序略滞后（apply 后异步）

### D8: 上下文事件写入独立 JSONL 行（非嵌入帧事件）

**选择**：touch / scroll 上下文事件写为 `{"type":"touch_context",...}` / `{"type":"scroll_context",...}` 独立行，与 `{"type":"frame",...}` 穿插

**理由**：
- 语义正确：上下文事件不属于"帧"概念
- 向后兼容：旧读取端按 `type` 字段过滤，忽略未知类型
- 实现简单：`FileOutputStrategy` 新增 `appendContextEvent()` 方法

## Risks / Trade-offs

**[Risk] scopeCountThreshold 等阈值为拍脑袋值** → 先用默认值上线，积累真实数据后调整。阈值可配置，业务可自行覆盖。

**[Risk] paramChanges 索引号（#1）无法映射到参数名** → 必须读源码按参数声明顺序对照。skill 读源码时需按 sourceLocation 定位函数声明，按参数顺序匹配。

**[Risk] sourceLocation 只有文件名无包路径** → `Glob "**/xxx.kt"` 可能匹配多个同名文件。用行号做二次校验。同名文件冲突概率不高但存在。

**[Risk] frames.jsonl 同帧事件同毫秒，时序不精确** → 级联检测只能做到"同帧同 State 触发一组组件"，无法区分先后顺序。对 skill 诊断影响有限。

**[Risk] applyMeasureResult hook 侵入性** → 修改 LazyListState 内部代码。通过 `RecompositionProfiler.isEnabled` 门控，未启用时零开销。

## Open Questions

- 动画上下文事件的具体实现时机（本次延后，用 skill 层的"疑似动画 State"规则替代）
- 真实案例库尚未建设，判别规则的准确性需在使用中迭代
