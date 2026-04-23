# Recomposition Analyzer Skill — Exploration Notes

> 探索阶段备忘录。不是提案、不是最终设计。
> 用来记录：已经对齐了什么 / 还在开放讨论什么 / 疑点和 TODO。

---

## 背景

业务仓库开发者在自己的 KuiklyUI Compose DSL 项目里，跑了重组性能分析工具（Recomposition Profiler），
拿到了两份日志文件（`report.json` + `frames.jsonl`）。希望有一个 skill 能读这些日志 + 业务源码，
自动分析重组问题并给出优化建议。

**业务规模类比腾讯新闻**，意味着几百到上千条重组事件是常态。

---

## 相关资料索引

| 用途 | 路径 |
|------|------|
| Profiler 主技术文档（含 12 规则在第 7 节） | `/Users/qibu/Git/Work/KuiklyUI/.ai/references/recomposition-profiler.md` |
| 用户指南 | `/Users/qibu/Git/Work/KuiklyUI/docs/Compose/recomposition-performance.md` |
| 日志获取命令各平台（已验证） | `openspec/changes/archive/2026-04-03-recomposition-profiler/specs/ai-integration/spec.md` |
| Profiler 源码 | `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/` |
| Filter 实现 | `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/profiler/filter/` |

---

## 已对齐的决策（固化）

### D1. 知识分发方式：内嵌

- skill 自带 `references/`，不依赖业务仓库或 KuiklyUI 仓库的外部文档
- 12 规则、最佳实践、日志格式说明全放 skill 自己的 references 里
- 理由：业务仓库大概率没有 KuiklyUI 源码，且规则是"稳定知识"

### D2. 形态 A / B / C 是一条参数化主流程

- 全量分析（A）、按严重度降序深挖（B）、聚焦某组件（C）共享同一套底层能力
- 不做三条独立流程

### D3. 形态 C 不做"组件名智能匹配"

- 用户想聚焦某页面/组件时，**引导他用 profiler 面板的"重置"按钮**，重新采一段只含目标操作的日志
- skill 不尝试"页面 → 组件名"的推断
- 理由：profiler 自身已有控制能力，skill 不要重复造

### D4. 不处理的情况

- 框架自身 Composable（`androidx.compose.*`、`com.tencent.kuikly.compose.*`）默认已被 profiler filter 掉，skill 不管
- 第三方/外部仓库的组件：skill 找不到源码时只标注"未定位"，不做特殊处理

### D5. 输出形态

- **对话摘要**：高层概览 + TOP N 问题
- **MD 详细报告**：按严重程度降序，每个问题含诊断 / 优化建议 / 源码定位
- 文件命名：`recomp-analysis-YYYYMMDD-HHmm.md`

### D6. 日志获取策略

```
Phase 0 顺序:
  1. 尝试 CWD 下约定目录（如 ./profiler_logs/）
  2. 尝试平台命令（需用户给 BundleID/包名）
     - iOS 模拟器: xcrun simctl get_app_container ... data
     - iOS 真机: xcrun devicectl device copy from ...
     - Android: adb shell run-as <PKG> cat cache/KuiklyProfiler/...
     - HarmonyOS: hdc file recv ...
  3. 失败 → 明确提示: "请跑一次 profiler → 告诉我文件路径"
```

主流程：**先让用户自己跑一遍生成文件，skill 再去获取**。

### D7. 置信度告警

- 当数据不足（如 `totalFrames < 100`）或异常时，skill 先告警并建议重采
- 用户坚持继续 → 分析但标注"置信度低"

### D8. Filter 已是 profiler 能力

- skill 是 filter 的**消费者**：读取 `filteredNames` / `filteredPrefixes` 字段，在报告开头声明"本次排除了哪些组件"
- 可以建议用户下次调整 filter 再采

---

## 关键架构洞察（不要忘）

### 核心原则：读源码是最贵的，必须是最后一步

```
❌ 错误设计: 全量 → 12 规则扫 → 每个都看源码 → 诊断
✅ 正确设计: 分层漏斗, 前几层只用日志, 尽量筛掉"正常重组"
```

### 两阶段分析：report 粗扫 → frames 细挖

| 能从 report.json 判断 | 必须看 frames.jsonl |
|---|---|
| 热点识别（高频 Composable）| 同帧父子关系 |
| 总体统计 | 帧耗时峰值 |
| 参数变更频率聚合 | State 连锁重组链路 |
| forced recomposition 是否存在 | 具体在哪几帧、什么触发 |
| scope 分布异常（某 scope 重组 N 次）| 该 scope 具体在哪些帧被触发 |
| State 触发 Composable 的静态关系 | 时序："State X 在 T1 变化后连锁触发 A→B→C" |

**skill 的三阶段漏斗**：
1. 先扫 report.json，产出"嫌疑清单 A"
2. 拉 frames.jsonl：对嫌疑清单 A 细挖 + **frames 本身也能产出新嫌疑**（帧耗时、帧内重组数等）
3. 只对真正可疑的项读源码

---

## 嫌疑分档（修订版）

> 不再按 A/B/C，改为按"需要什么数据源"来分类。

### 维度一：需要什么数据源

| 类别 | 数据源 | 举例 |
|------|--------|------|
| R（report-only）| 只读 report.json 就能定案 | 数据量异常 |
| F（frames-only）| 需读 frames.jsonl | 父子同帧、连锁重组时序 |
| R+F | 组合分析 | scope 分布异常 → report 发现 → frames 溯源 |
| R+F+S | 还要读业务源码 | lambda 稳定性问题 |
| User | 问用户 | 是否动画驱动、业务预期是否合理 |

### 维度二：置信度

- **高置信**：日志 + 源码就能下结论
- **低置信/存疑**：特征模糊、需业务上下文
- **仅陈述**：skill 只摆数据，不下结论

---

## 12 规则逐条审议（用户反馈）

源文档：`/Users/qibu/Git/Work/KuiklyUI/.ai/references/recomposition-profiler.md` 第 234-249 行

| # | 原规则 | 用户反馈 | 处置 |
|---|---|---|---|
| 1 | recompositionsPerSecond > hotspotThreshold | 先不需要，主要关注耗时 | **删除**（耗时更重要）|
| 2 | recompositionCount > totalRecompositions * 0.3 | 没意义 | **删除** |
| 3 | 同一 State 触发 3+ 个 Composable（State 范围过大）| 有必要 | **保留**，R 类 |
| 4 | triggerStates 含 `[forced recomposition]` | 不清楚触发场景 | **存疑**，已查证：不是业务代码直接控制，降级为"标记"不作为独立信号 |
| 5 | avgDurationMs > 16ms | 重要 | **保留**，R 类 + F 类（还得看 frames 找峰值帧）|
| 6 | 父子同帧内均出现 | 在 report 里看不到 | **存疑**，明确标为 F 类 |
| 7 | 短时间内陡增（重组风暴）| 不好识别 | **存疑** |
| 8 | 无 triggerStates 但次数高 | 价值不大 | **删除**（暂） |
| 9 | 相同 composableName 多 scopeKey | 价值不大，且已查证：LazyColumn 多 item 正常场景就这样 | **删除** |
| 10 | paramChanges 频繁变化（lambda 不稳定）| 保留 | **保留**，R+F+S 类 |
| 11 | 框架 Composable 出现在 hotspots | 先不管 | **删除**（默认 filter 掉）|
| 12 | 帧率过低 | 先不管 | **暂缓**，可作为数据健康检查告警|

**修订后保留的规则**（核心只剩 3 条 + 数据健康检查）：

1. **RULE-A**: 存在单次重组耗时 > 阈值（默认 5ms）的组件 → F 类（需 frames 确认峰值）, 高置信
2. **RULE-B**: 某 State 触发多个 Composable → R 类, 需区分是否主题/语言这种合理场景（C 档问题，让用户判断）
3. **RULE-C**: 某组件 paramChanges 高频变化 → R 类筛嫌疑, F 类确认, S 类定位
4. **CHECK-D**: 数据健康检查（帧数 / 帧率）→ 告警是否重采

---

## 用户补充的"重组判别直觉"

### 直觉 1：scopeKey = null 大概率正常（首次组合）

- 源自 profiler 文档 Known Limitations：*"首次组合和参数驱动时 scope=none 是预期行为"*
- skill 策略：scope=null 的事件计入"正常重组清单"，不分析

### 直觉 2：同一组件，区分是 null 次数多还是同 scopeKey 次数多

```
组件 Foo 重组 50 次:
 - 其中 45 次 scope=null     → 大概率正常（LazyColumn 滚动等）
 - 其中 45 次 scope=2048     → 大概率异常（这个实例在被反复触发）
```

- skill 策略：report.json 的 `scopeDistribution` 恰好提供这个数据
- 同一 scope 高次数 = 强嫌疑信号

### 直觉 3：某 scopeKey 级联大量其他组件重组

- 对应原规则 3 的另一个角度：State 提得过高
- skill 策略：R 类发现 + 让用户 review

---

## 开放的 TODO / 已知限制

### TODO-1：用户操作上下文事件（需 profiler 新增，本 skill 是消费者）

**场景**：一次 click 本应 1 次重组，实际 3 次；滑动列表导致 item 重组，是正常还是异常。

**用户要求**：
1. Touch 事件：`touchBegin` / `touchEnd` / `touchCancel`（不要 move，太多）
2. 滚动列表事件：`firstVisibleItemIndex` 变化（LazyList / LazyGrid / Pager 等）

**前缀**：与重组工具日志保持一致（`[RCProfiler]` 或同 sessionId 体系）

**对 skill 的价值**：
- 单 scope 多次重组：如果 touchBegin 和 touchEnd 之间发生了 3 次重组，
  可能 1 次就够了 → 异常
- 滚动导致的 item 重组：如果 firstVisibleItemIndex 变了 → 正常滚动；
  如果没变 → 数据刷新或其他原因，需分析

**实现方案（在 profiler 侧，非 skill 侧）**：

| 事件 | Hook 点 | 方式 |
|------|---------|------|
| touch | `RootNodeOwner.onPointerInput()` (L223-237) | 检测 `PointerEventType.Press/Release/Cancel`，调用 `RecompositionProfiler.recordTouchContext()` |
| scroll | `LazyListState.applyMeasureResult()` (L506-539) | 在 `scrollPosition.updateFromMeasureResult(result)` 前后比较 `firstVisibleItemIndex`，同步回调 `RecompositionProfiler.recordScrollContext()` |

**日志格式**（spec 推荐路径 B：独立 JSONL 行，与 frame 行穿插）：

```jsonl
{"type":"session","sessionId":"rcp-xxx","startTimestampMs":...}
{"type":"frame","events":[...]}
{"type":"touch_context","eventType":"touchBegin","timestampMs":1776678700583,"pointerCount":1}
{"type":"frame","events":[...]}
{"type":"touch_context","eventType":"touchEnd","timestampMs":1776678700620}
{"type":"scroll_context","listId":"feedsList","firstVisibleItemFrom":3,"firstVisibleItemTo":5,"visibleItemCount":7,"timestampMs":1776678700650}
{"type":"frame","events":[...]}
```

**skill 读取端**：按 `type` 字段区分行类型，`frame` → 重组事件，`touch_context` / `scroll_context` → 操作上下文。
旧版 skill 忽略未知 type，向后兼容。

**需改的 profiler 文件**：
- `RecompositionEvent.kt`：新增 `TouchContextEvent` / `ScrollContextEvent`
- `FileOutputStrategy.kt`：新增 `appendContextEvent()` 写独立行
- `RecompositionProfiler.kt`：新增 `recordTouchContext()` / `recordScrollContext()` API
- `RootNodeOwner.kt`：hook onPointerInput
- `LazyListState.kt` / `LazyGridState.kt` / `PagerState.kt`：在 `applyMeasureResult` 内部 hook（同步回调，时序精确）

**方案选择：applyMeasureResult 内部 hook（而非 snapshotFlow）**

理由：
- 时序精确：同步执行，在 layout pass 内即时回调，不滞后
- 对 Snapshot 无额外开销：不引入 read observation
- 对 profiler 无额外影响：不触发 recomposition，不创建 RecomposeScope
- 侵入性略高（需改框架内部代码），但收益值得

实现方式：
```kotlin
// LazyListState.applyMeasureResult() 内部
val oldIndex = scrollPosition.index
scrollPosition.updateFromMeasureResult(result)
if (oldIndex != scrollPosition.index && RecompositionProfiler.isEnabled) {
    RecompositionProfiler.recordScrollContext(
        listId = ..., firstVisibleItemFrom = oldIndex,
        firstVisibleItemTo = scrollPosition.index,
        visibleItemCount = result.visibleItemsInfo.size
    )
}
```

附带修复：在 `RecompositionTracker.registerSnapshotObserver()` 中过滤掉
`layoutInfoState`（`neverEqualPolicy`），避免每次滚动产生大量无意义的
state change 记录消耗配额。

**当前 skill 处理**：写进 known limitations，等 profiler 侧实现后再对接。

### TODO-2：真实案例库建设

- 用户当前没有具体案例
- 规则先上 MVP，随着使用积累案例，迭代更新 references/

### TODO-3：严重度排序算法细节

- 按"耗时 × 次数 × 嫌疑权重"排序
- 权重先拍脑袋，跑几份真实报告后调

### TODO-4：OverlayOutputStrategy 的重置按钮交互如何引导

- 形态 C 依赖用户会用"重置"按钮
- SKILL.md 里写明引导文本 + 可能要加一张面板截图指路

---

## skill 结构草案（待定）

```
.codebuddy/skills/kuikly-recomposition-analyzer/
├── SKILL.md                        ← 触发词、简介、主工作流
├── config.md                       ← 可配置阈值（见下）
├── references/
│   ├── log-format.md               ← report.json / frames.jsonl 字段说明
│   ├── log-retrieval.md            ← 各平台命令
│   ├── detection-rules.md          ← 精简后的规则 + R/F/S 分类
│   ├── optimization-patterns.md    ← 每条规则对应的优化方案
│   ├── known-limitations.md        ← EventContext 缺失等
│   └── report-template.md          ← MD 报告模板
└── workflows/
    └── main.md                     ← 主参数化流程
```

### 可配置阈值（config.md）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `scopeCountThreshold` | 5 | 某具体 scopeKey 重组次数超过此值 → 嫌疑 |
| `durationThreshold` | 5 | 单次重组耗时超过此值(ms) → RULE-A 嫌疑 |
| `frameEventThreshold` | 50 | 单帧 composable 事件数超过此值 → CHECK-E 帧级热点 |
| `frameDurationThreshold` | 16 | 单帧耗时超过此值(ms) → 帧级卡顿 |
| `paramChangeRateThreshold` | 0.9 | 参数变化率超过此值(0-1) → RULE-C lambda 不稳定 |
| `stateReadersThreshold` | 3 | State 的 readers 数超过此值 → RULE-B State 广播 |
| `minFramesThreshold` | 30 | 总帧数低于此值 → CHECK-D 数据不足告警 |

所有阈值在 SKILL.md 或 skill 调用时可通过参数覆盖。

**"规则"和"优化方案"为什么拆开**：
规则是判别（what is wrong），优化方案是建议（how to fix），两者独立演化。
规则稳定，优化方案可以随 Compose 升级、新 API 出现而扩展。

---

## 主流程草图（修订版 v4）

> v3 → v4 修正：
> 1. noScope 组件直接归为正常，不需要额外 frames 验证。
>    之前的"noScope 也可能异常"是我犯了事实性错误后的过度修正。
> 2. 筛选维度：关注有具体 scope 且计数 > 阈值的组件。
> 3. 新增 CHECK-E：单帧事件数阈值。

```
Phase 0  获取日志                (自动 → 引导 → 问用户)
   │
Phase 1  解析 report.json + Overview
   │      - 统计、过滤配置声明
   │      - CHECK-D 数据健康检查
   │
Phase 2  纯 report 筛查（不读源码）
   │
   │    ┌── Step 2a: 分流
   │    │
   │    │   对每个组件, 看 scopeDistribution + noScopeRecompositions:
   │    │
   │    │   · 有具体 scopeKey 且计数 > scopeCountThreshold（默认 5）
   │    │     → 高优先级嫌疑（某实例被反复触发）
   │    │
   │    │   · 有具体 scopeKey 但计数低
   │    │     → 低优先级，待观察
   │    │
   │    │   · 全部 noScope（scopeDistribution 为空）
   │    │     → 直接归入"正常重组清单"，不深入分析
   │    │       （noScope = 首次组合/无状态保持，大概率正常）
   │    │
   │    ├── Step 2b: 标记嫌疑（R 类能看到的）
   │    │
   │    │   ① 具体 scope 高频触发                    [直觉 2 修正版]
   │    │     scopeDistribution 里某 scopeKey 计数 > scopeCountThreshold
   │    │     → "某实例被反复触发", 强嫌疑
   │    │     （不看比例, 看绝对次数）
   │    │
   │    │   ② paramChanges 高频                   (RULE-C)
   │    │     paramChangeFrequency 里某 #N 变化率 ≈ 100%
   │    │     → lambda/参数不稳定信号
   │    │
   │    │   ③ State 广播                          (RULE-B)
   │    │     triggerStates 里出现 readers: A, B, C (多个)
   │    │     → R 类能看到, 但需用户判断是否主题这种合理场景
   │    │
   │    └── Step 2c: 标记"需 F 类确认的嫌疑"
   │                                             [直觉 3 落地]
   │        这些 report 看不到, 但值得去 frames 查:
   │        · 级联重组链 (某 scope 重组 → 带出其他组件)
   │        · 父子同帧
   │        · 短时间重组陡增
   │
Phase 3  拉 frames.jsonl 深挖
   │    ┌── 对 Phase 2 嫌疑项确认时序
   │    │   · 对 Step 2b 嫌疑: 确认具体时机模式
   │    │   · 对 Step 2c 嫌疑: 检测级联链/父子同帧
   │    │
   │    ├── frames 本身产出新嫌疑（R 阶段看不到的）
   │    │   · RULE-A: 单帧内某组件耗时 > durationThreshold（默认 5ms）
   │    │   · CHECK-E: 单帧 composable 事件数 > frameEventThreshold（默认 50）
   │    │     → 帧级热点告警
   │    │   · 帧耗时 > frameDurationThreshold（默认 16ms）
   │    │     → 帧级卡顿告警
   │    │
   │    ├── 读业务源码（S 类 only, 比如 paramChanges）
   │    └── 生成诊断 + 优化建议
   │
Phase 4  输出
        - 对话: 概览 + TOP 3
        - MD: 完整报告（按严重度降序, 附"正常重组清单"）
```

### 三条直觉的落地映射（v4）

| 直觉 | 落地位置 | 数据依据 |
|------|----------|----------|
| 1. scope=null 大概率正常 | Step 2a | noScopeRecompositions == recompositionCount → 直接归正常清单 |
| 2. 关注具体 scope 高频 | Step 2c ① | scopeDistribution 里 scopeKey 的绝对计数 > 阈值 |
| 3. 某 scope 级联带动其他组件 | Step 2d + Phase 3 | frames.jsonl 帧内事件 |

---

## 真实数据验证记录（复杂场景）

### 验证 1：noScope 组件直接归正常

**数据**：RenderFeedsItem (16次重组, 全 noScope)，分布在 ~15 个不同帧
→ LazyColumn 滚动出新 item 的首次组合，正常行为

**结论**：noScope 直接归正常清单，不需要额外 frames 验证。
之前我犯的事实性错误（说"16次都在同一帧"）不构成推翻分类逻辑的理由。

### 验证 2：具体 scope 高频是强嫌疑信号

CustomStateSection: scopeDistribution = {"263889481": 3}, noScope=1
→ scope 263889481 被触发了 3 次，值得分析

ViewModelSection: scopeDistribution = {"66116068": 4}, noScope=0
→ scope 66116068 被触发了 4 次，且全有 scope，强嫌疑

### 验证 3：单帧事件数是帧级热点信号

Frame #25: 81ms, 337 事件 → 明显的帧级热点
Frame #1: 148ms, 72 事件 → 首次组合，耗时但可能是正常的

### 验证 4：paramChangeFrequency #1 = 100% 是 lambda 不稳定信号

WithTheme: #1 变了 60/60 次 → lambda 参数每次重建
Divider: #1 变了 23/23 次 → 同上

---

## 下次继续的切入点

1. 用户读完 `recomposition-profiler.md` 第 7 节后，我们对齐最终保留的规则列表
2. 用户补充真实案例，我们反推判别特征
3. 确定 skill 目录结构落地 → 开始写 SKILL.md 和 references/
4. 是否要转成正式 OpenSpec 提案（proposal.md / tasks.md）

---

## 决策变更记录

| 日期 | 变更 | 原因 |
|------|------|------|
| 初始 | A/B/C 档分级 | 按"日志可信度"分 |
| 修订 1 | A 档 "同 scopeKey 1 帧内多次" 删除 | 查证：LazyColumn 多 item 场景合法 |
| 修订 2 | A 档 "强制重组、级联 State" 降级为存疑 | 用户质疑 + 查证：有合法场景 |
| 修订 3 | 分档维度改为 R/F/R+F/R+F+S/User | 用户指出"区分看报告/帧/源码" 更实用 |
| 修订 4 | 12 规则精简到 3 条核心 + 1 项检查 | 用户逐条审议 |
| 修订 5 | 形态 C 不做名字匹配，改引导用"重置"按钮 | 用户提议，利用 profiler 已有能力 |
| 修订 6 | 三条直觉显式落地到 Step 2a/2c/2d | 审计发现直觉 2 逻辑不显式、直觉 3 被错误并入 RULE-B |
| 修订 7 | "noScope 占比高 → 归正常"删除；改用"具体 scope 计数 > 阈值"筛嫌疑 | 用户纠正：应关注带 scope 的次数，不看比例 |
| 修订 8 | 全 noScope 组件不直接归正常，需 frames 验证帧分布 | 真实数据验证：RenderFeedsItem 16次 noScope 分布在 15 帧（正常），而非集中在 1 帧（异常），证明不能只看聚合数字 |
| 修订 9 | 新增 CHECK-E：单帧事件数 > 阈值 → 帧级热点告警 | 复杂场景 Frame #25 有 337 事件/81ms，是强信号 |
| 修订 10 | 撤回修订 8：noScope 组件直接归正常，不需 frames 验证 | 用户纠正：之前的事实错误不构成推翻分类逻辑的理由，noScope = 首次组合 = 大概率正常 |
| 修订 11 | frames 阶段也能产出新嫌疑（帧耗时、帧事件数等），不只是确认 report 嫌疑 | 用户指出：两阶段不是单向漏斗，frames 是独立数据源 |
| 修订 12 | RULE-A 阈值从 16ms 改为 5ms，所有阈值可配置 | 用户要求：16ms 太高，5ms 更实际；阈值不应硬编码 |
| 修订 13 | frames 阶段也能产出新嫌疑（帧耗时、帧事件数等） | 用户指出：frames 是独立数据源，不只是确认 report 嫌疑 |
| 修订 14 | scroll 上下文事件用 applyMeasureResult 内部 hook（非 snapshotFlow） | 用户选择：时序精确性优先，侵入性可接受 |

---

## 6 疑问真相对齐（基于简单+复杂场景）

### ① scopeDistribution 里 null 怎么表示？

不在 scopeDistribution 的 key 里。用独立字段 `noScopeRecompositions` 表示。
scopeDistribution 里只有数字 key（scope 的 hashCode）。

**skill 判别代码**：
```
有 scope 重组数 = recompositionCount - noScopeRecompositions
有 scope 重组数 > 阈值 → 嫌疑
```

### ② 筛选维度：不看比例，看绝对次数

用户明确指出：不管 noScope 占比多高，只要某具体 scopeKey 的重组次数 > 阈值，就值得分析。

**skill 逻辑**：
- 遍历 scopeDistribution，找出计数 > 阈值（如 > 5）的 scopeKey
- 这些 scope 就是"被反复触发的实例"，强嫌疑

### ③ triggerStates 可读性

三种格式：
- 可读+有 readers: `"State(prev=10, now=11), readers: ViewModelCounterDisplay, ViewModelSection"`
- 半可读（有值无 readers）: `"State(now=true)"`
- 匿名/框架级: `"State(prev=LazyListMeasureResult@xxx, now=...@yyy)"`

skill 可以从 `readers: A, B, C` 提取 State→Composable 传播关系，无法从 State 值反推源码变量名。

### ④ frames.jsonl 同帧事件时序

timestampMs 精确到毫秒，但同帧内多个事件经常同毫秒，**严格因果顺序不可靠**。
级联检测只能做到"同帧同 State 触发了一组组件"。

### ⑤ sourceLocation 格式

`文件名.kt:行号`，无包路径。skill 用 `Glob "**/xxx.kt"` 定位文件。
同名文件冲突风险存在（尤其 `MainActivity.kt`），需行号二次校验。

### ⑥ paramChanges 格式

逐事件: `{"totalParams": 3, "changedParams": [0, 1], "unknownParams": []}`
聚合: `{"#0": 2, "#1": 24, "#2": 1}`

索引号（#1）无法直接映射到源码参数名，需读源码按参数声明顺序对。
`#1` 在 Compose 中通常是 lambda/callback 参数，100% 变化率 = lambda 不稳定。
