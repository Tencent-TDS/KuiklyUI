## Why

KuiklyUI 的重组性能分析工具（Recomposition Profiler）已能输出 `report.json`（聚合报告）和 `frames.jsonl`（逐帧详情），但业务开发者面对数百条重组记录，难以快速定位真正需要优化的组件。当前缺少一个能自动解读日志、关联业务源码、给出诊断和优化建议的工具。业务规模类比腾讯新闻，手动分析不可行，需要 AI 辅助。

## What Changes

- 新增 `kuikly-recomposition-analyzer` skill（CodeBuddy Code skill），运行在业务源码仓库
- skill 读取 profiler 输出的 `report.json` + `frames.jsonl`，结合业务源码，自动分析重组问题
- 三阶段漏斗：report 筛查 → frames 深挖 → 源码确认，逐层收窄分析范围
- 可配置阈值（单次耗时、scope 计数、帧事件数等），适配不同业务场景
- 输出对话摘要 + Markdown 详细报告（按严重程度降序）
- 知识内嵌（references/），skill 自带规则和优化方案，不依赖外部仓库
- 同时为 profiler 补充用户操作上下文事件（touch / scroll），辅助 skill 判断重组是否正常

## Capabilities

### New Capabilities

- `recomposition-analyzer-skill`: CodeBuddy Code skill，运行在业务仓库，读取 profiler 日志 + 业务源码，自动诊断重组问题并输出优化建议报告。含三阶段漏斗（report → frames → 源码）、可配置阈值、知识内嵌 references。
- `profiler-context-events`: 为 Recomposition Profiler 新增用户操作上下文事件：touch 事件（touchBegin/touchEnd）和滚动列表事件（firstVisibleItemIndex 变化）。写入 frames.jsonl 为独立 JSONL 行，与现有 frame 行穿插，向后兼容。skill 消费这些事件辅助判断重组是否正常。

### Modified Capabilities

（无）

## Non-goals

- 不处理自研 DSL 的重组问题（仅 Compose DSL）
- 不处理框架自身 Composable 的重组（默认被 profiler filter 掉）
- 不处理外部仓库/第三方库组件的源码定位
- 不做"页面 → 组件名"的智能匹配（引导用户用 profiler 面板重置按钮）
- 不自动修复代码，只给出诊断和优化建议
- 不替代 profiler 自身的功能（skill 是消费者，不是扩展）

## Impact

**新增文件（skill）**：
- `.codebuddy/skills/kuikly-recomposition-analyzer/` 目录下约 10 个文件（SKILL.md + 9 个 reference 文件）
- 不影响任何现有框架代码

**修改文件（profiler context events）**：
- `compose/` 模块：`RecompositionEvent.kt`、`FileOutputStrategy.kt`、`RecompositionProfiler.kt`、`RootNodeOwner.kt`、`LazyListState.kt` / `LazyGridState.kt` / `PagerState.kt`

**影响平台**：Android / iOS / HarmonyOS（profiler 修改影响所有平台，skill 不区分平台）

**影响模块**：`compose/`（profiler 相关代码）
