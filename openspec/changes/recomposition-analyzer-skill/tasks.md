## 1. Profiler 上下文事件（compose 模块）

- [x] 1.1 新增 `TouchContextEvent` 和 `ScrollContextEvent` 到 `RecompositionEvent.kt`
- [x] 1.2 在 `RecompositionProfiler.kt` 新增 `recordTouchContext()` 和 `recordScrollContext()` 公开 API（含 isEnabled 门控）
- [x] 1.3 在 `FileOutputStrategy.kt` 新增 `appendContextEvent()` 方法，写独立 JSONL 行到 pendingFrames
- [x] 1.4 在 `RootNodeOwner.onPointerInput()` hook touch 事件（Press/Release/Cancel），调用 recordTouchContext
- [x] 1.5 在 `LazyListState.applyMeasureResult()` hook scroll 事件，比较 firstVisibleItemIndex 前后值，调用 recordScrollContext
- [x] 1.6 在 `LazyGridState` 和 `PagerState` 同理 hook scroll 事件
- [x] 1.7 编译验证：`./gradlew :compose:compileDebugKotlinAndroid`

## 2. Recomposition Analyzer Skill 骨架

- [ ] 2.1 创建 `.codebuddy/skills/kuikly-recomposition-analyzer/SKILL.md`（触发词、简介、主工作流）
- [ ] 2.2 创建 `.codebuddy/skills/kuikly-recomposition-analyzer/config.md`（可配置阈值表）
- [ ] 2.3 创建 `.codebuddy/skills/kuikly-recomposition-analyzer/workflows/main.md`（主参数化流程 Phase 0-4）

## 3. Skill References 文件

- [ ] 3.1 创建 `references/log-format.md`（report.json / frames.jsonl 字段说明 + 上下文事件格式）
- [ ] 3.2 创建 `references/log-retrieval.md`（各平台 adb/xcrun/hdc 命令）
- [ ] 3.3 创建 `references/detection-rules.md`（精简后的规则：scope 高频、paramChanges、State 广播 + 帧级检查）
- [ ] 3.4 创建 `references/optimization-patterns.md`（每条规则对应的优化方案 + Compose 写法样例）
- [ ] 3.5 创建 `references/known-limitations.md`（EventContext 缺失、paramChanges 索引无参数名等）
- [ ] 3.6 创建 `references/report-template.md`（MD 报告模板）

## 4. Profiler 上下文事件测试用例（Android 为主，iOS/HarmonyOS 交叉验证）

### 4.1 Touch 上下文事件

- [ ] 4.1.1 单指点击：touchBegin + touchEnd 成对出现在 frames.jsonl，pointerCount=1
- [ ] 4.1.2 单指长按后移出屏幕：touchBegin + touchCancel 成对出现
- [ ] 4.1.3 双指缩放：touchBegin pointerCount=2
- [ ] 4.1.4 Move 事件不记录：滑动过程中 frames.jsonl 不出现 touch_context eventType=touchMove
- [ ] 4.1.5 Profiler 未启用时零事件：禁用 profiler 后操作，frames.jsonl 无 touch_context 行
- [ ] 4.1.6 touch 事件与重组帧的时序：touchBegin 后的下一帧应包含重组事件（如果有）

### 4.2 Scroll 上下文事件

- [ ] 4.2.1 LazyColumn 滚动：scroll_context 记录 firstVisibleItemFrom/To 变化，visibleItemCount 正确
- [ ] 4.2.2 LazyRow 水平滚动：同上
- [ ] 4.2.3 LazyGrid 滚动：firstVisibleItemIndex 变化时记录 scroll_context
- [ ] 4.2.4 Pager 翻页：currentPage 变化时记录 scroll_context
- [ ] 4.2.5 未滚动不记录：LazyColumn 内容未变化时不产生 scroll_context
- [ ] 4.2.6 listId 区分多个列表：同一页面有 2 个 LazyColumn 时，scroll_context 的 listId 不同
- [ ] 4.2.7 快速滚动：多次 firstVisibleItemIndex 变化产生多条 scroll_context
- [ ] 4.2.8 Profiler 未启用时零事件：禁用 profiler 后滚动，frames.jsonl 无 scroll_context 行

### 4.3 JSONL 格式兼容性

- [ ] 4.3.1 新旧行类型穿插：frames.jsonl 中 frame / touch_context / scroll_context 行可正确穿插，读取端按 type 字段区分
- [ ] 4.3.2 旧版 skill 读取新版日志：忽略 touch_context / scroll_context 行，不报错
- [ ] 4.3.3 report.json 不受影响：上下文事件不影响 report.json 的结构和内容

### 4.4 性能验证

- [ ] 4.4.1 touch hook 开销：启用 profiler 后点击操作无明显卡顿（touch 回调逻辑应极轻量）
- [ ] 4.4.2 scroll hook 开销：启用 profiler 后滚动操作帧率无明显下降（applyMeasureResult 内部同步回调应 < 0.1ms）
- [ ] 4.4.3 禁用 profiler 时零开销：确认 isEnabled=false 时 hook 点无额外计算

### 4.5 iOS 交叉验证

- [ ] 4.5.1 iOS 模拟器：单指点击产生 touchBegin + touchEnd
- [ ] 4.5.2 iOS 模拟器：LazyColumn 滚动产生 scroll_context
- [ ] 4.5.3 iOS 真机：touch + scroll 事件验证（需信任开发者证书）

### 4.6 HarmonyOS 交叉验证

- [ ] 4.6.1 HarmonyOS 模拟器/真机：touch + scroll 事件验证（如 HDC 可用）

## 5. Skill 集成测试用例

### 5.1 日志获取（Phase 0）

- [ ] 5.1.1 Android 自动获取：adb 连接模拟器，skill 自动拉取 report.json + frames.jsonl
- [ ] 5.1.2 iOS 自动获取：xcrun simctl 从模拟器拉取
- [ ] 5.1.3 无设备时引导：未连接设备时 skill 输出操作指引
- [ ] 5.1.4 用户指定路径：用户手动提供文件路径时 skill 正确读取

### 5.2 Report 筛查（Phase 2）

- [ ] 5.2.1 noScope 归正常：全 noScope 组件列入"正常重组清单"，不触发后续分析
- [ ] 5.2.2 scope 高频检测：scopeDistribution 中某 key 计数 > scopeCountThreshold 的组件标记为嫌疑
- [ ] 5.2.3 paramChanges 高频检测：paramChangeFrequency 中 #N 变化率 > paramChangeRateThreshold 的组件标记为嫌疑
- [ ] 5.2.4 State 广播检测：triggerStates 中 readers 数 > stateReadersThreshold 的 State 标记为广播
- [ ] 5.2.5 数据不足告警：totalFrames < minFramesThreshold 时输出告警
- [ ] 5.2.6 阈值可覆盖：用户指定 scopeCountThreshold=10 时，使用自定义值

### 5.3 Frames 深挖（Phase 3）

- [ ] 5.3.1 帧耗时检测：单帧 durationMs > frameDurationThreshold 时输出帧级卡顿告警
- [ ] 5.3.2 帧事件数检测：单帧 composable 事件数 > frameEventThreshold 时输出帧级热点告警
- [ ] 5.3.3 单组件耗时检测：单次重组 durationMs > durationThreshold 时标记 RULE-A 嫌疑
- [ ] 5.3.4 级联检测：同帧内多个组件被同一 State 触发时输出级联嫌疑

### 5.4 源码关联（Phase 3 S 类）

- [ ] 5.4.1 正常定位：sourceLocation 为 "CounterSection.kt:221" 时，Glob 找到文件并读取对应行
- [ ] 5.4.2 找不到源码：Glob 无结果时标注"源码未定位"并继续
- [ ] 5.4.3 同名文件冲突：Glob 匹配多个同名文件时，用行号校验或标注歧义

### 5.5 输出报告（Phase 4）

- [ ] 5.5.1 Markdown 报告生成：包含数据概览、正常重组清单、问题诊断、过滤配置声明
- [ ] 5.5.2 按严重度降序：问题诊断段中，耗时高 × 次数多的排前面
- [ ] 5.5.3 对话摘要：输出 TOP 3 问题的高层概览

### 5.6 上下文事件消费（touch/scroll 可用时）

- [ ] 5.6.1 touch 上下文辅助判断：touchBegin~touchEnd 之间某 scope 重组 3 次 → 标注"一次点击触发 3 次重组，疑似可优化"
- [ ] 5.6.2 scroll 上下文辅助判断：scroll_context 显示 firstVisibleItemIndex 变化 + item 重组次数 ≈ 滑入数量 → 归入正常
- [ ] 5.6.3 scroll 上下文异常判断：scroll_context 显示 index 未变 + item 重组 → 标注"非滚动导致的重组，需分析"

### 5.7 形态 C 验证

- [ ] 5.7.1 用户请求聚焦分析时，skill 输出"请在 profiler 面板点'重置'"的引导文本

## 6. 端到端测试用例（基于 RecompositionProfilerDemoPage）

> 以下用例在 Android 模拟器上执行，使用 demo app 的 RecompositionProfilerDemoPage 页面。
> 每个用例包含：操作步骤 → 预期日志输出 → skill 预期分析结果。

### 6.1 E2E: 手动计数器 — 正常重组

- [ ] 6.1.1 操作：Start Profiler → 点击 CounterSection 的 "+1" 按钮 3 次 → Stop Profiler → Get Report
  - 预期日志：CounterSection 重组 3 次，triggerStates 含 count State 变化记录
  - 预期 skill 分析：CounterSection 有具体 scope 且重组 3 次（< scopeCountThreshold=5），标记为低优先级；triggerStates 显示 count State 正常驱动重组；归入"基本正常，无需优化"

### 6.2 E2E: 自动递增 — 高频重组热点

- [ ] 6.2.1 操作：Start Profiler → 点击 AutoIncrementSection 的 "Start" → 等待 3 秒 → 点击 "Stop" → Stop Profiler → Get Report
  - 预期日志：AutoIncrementSection 重组约 30 次（100ms/次），isHotspot=true
  - 预期 skill 分析：AutoIncrementSection 某具体 scope 重组次数远超 scopeCountThreshold，标记为高优先级嫌疑；triggerStates 显示 autoCount State 驱动；skill 应输出"每 100ms 递增一次导致持续重组，如无实时显示需求，建议降低更新频率"

### 6.3 E2E: 参数变化检测 — Lambda 不稳定

- [ ] 6.3.1 操作：Start Profiler → 点击 ParentChildDemo 的 "Name Only" 按钮 5 次 → Stop Profiler → Get Report
  - 预期日志：LambdaChild 重组 5 次，paramChanges 中 #1 参数（label 或 onClick）每次都变化
  - 预期 skill 分析：LambdaChild 的 paramChangeFrequency 中 #1 变化率 100%，触发 RULE-C；skill 读取源码 LambdaChild 声明，确认 #1 参数是否为 lambda；如为 lambda 且未 remember，建议"使用 remember 稳定 lambda 引用"

### 6.4 E2E: 父子同帧重组

- [ ] 6.4.1 操作：Start Profiler → 点击 ParentChildDemo 的 "Both" 按钮 3 次 → Stop Profiler → Get Report
  - 预期日志：MiddleLayer 和 GrandChild 在同帧内均出现重组
  - 预期 skill 分析：skill 从 frames.jsonl 发现 MiddleLayer + GrandChild 同帧出现；如果 MiddleLayer 的 name 参数变化导致 GrandChild 的 displayName 也变化，标注"父子组件参数耦合，如子组件独立逻辑较多建议拆分"

### 6.5 E2E: 滚动列表 — Scroll 上下文辅助判断

- [ ] 6.5.1 操作：Start Profiler → 滚动 ScrollSection 的 LazyColumn 向下滑动 5-10 格 → Stop Profiler → Get Report
  - 预期日志：ScrollListItem 重组多次；scroll_context 记录 firstVisibleItemIndex 变化（如 0→5→10）；ScrollListItem 的 noScopeRecompositions 占多数
  - 预期 skill 分析：ScrollListItem 全 noScope 或 noScope 占比高 → 归入正常重组清单；scroll_context 显示 index 确实变化 → 确认是滚动导致的正常 item 首次组合；skill 不应将此标记为问题

### 6.6 E2E: 滚动列表点击 — Touch 上下文辅助判断

- [ ] 6.6.1 操作：Start Profiler → 点击 ScrollSection 中某个 item → 点击另一个 item → Stop Profiler → Get Report
  - 预期日志：touchBegin + touchEnd 事件；2 个 ScrollListItem 因 selectedIndex 变化而重组
  - 预期 skill 分析：touch 上下文显示用户执行了 2 次点击；2 个 item 重组是 selectedIndex State 驱动的正常行为；skill 不应标记为异常

### 6.7 E2E: ViewModel 精准重组 — State 隔离

- [ ] 6.7.1 操作：Start Profiler → 点击 ViewModelSection 的 "+1" 5 次 → 点击 "-> Bob" → Stop Profiler → Get Report
  - 预期日志：ViewModelCounterDisplay 重组 5+ 次（counter 驱动），ViewModelUserDisplay 重组 1 次（userName 驱动），两者互不触发
  - 预期 skill 分析：triggerStates 的 readers 字段分别只包含对应组件；skill 确认 State 隔离良好，无广播问题；输出"ViewModel 多 StateFlow 方案实现了精准重组"

### 6.8 E2E: Data Class State — 粗粒度更新

- [ ] 6.8.1 操作：Start Profiler → 点击 CustomStateSection 的 "Rename" 3 次 → Stop Profiler → Get Report
  - 预期日志：CustomStateSection 重组 3 次（profile 整体替换），ProfileNameCard 重组 3 次（name 变），ProfileLevelCard 和 ProfileScoreCard 应 skip
  - 预期 skill 分析：CustomStateSection 每次 profile 变化都重组（data class 整体替换）；如果 ProfileLevelCard/ProfileScoreCard 未 skip 且出现在重组列表中 → 标记"子组件未正确 skip，检查 data class 参数是否被不稳定对象包裹"

### 6.9 E2E: Object State — 细粒度更新

- [ ] 6.9.1 操作：Start Profiler → 点击 ObjectWithStateSection 的 "Count++" 3 次 → Stop Profiler → Get Report
  - 预期日志：CountDisplay 重组 3 次（count 变），LabelDisplay 不重组（label 未变），ObjectWithStateSection 重组 3 次
  - 预期 skill 分析：对比 6.8 场景，skill 应指出"Object 持有独立 MutableState 属性比 data class 整体替换更细粒度，子组件 skip 率更高"

### 6.10 E2E: Filter 配置 — 消费者视角

- [ ] 6.10.1 操作：Start Profiler → 点击 "Exclude CounterSection" → 点击 CounterSection 的 "+1" 3 次 → Stop Profiler → Get Report
  - 预期日志：report.json 的 filteredNames 包含 "CounterSection"；CounterSection 不出现在 composables 列表中
  - 预期 skill 分析：skill 在 Overview 段声明"本次分析排除了 CounterSection"；如用户问"CounterSection 怎么没出现"，skill 解释过滤配置

### 6.11 E2E: 数据不足告警

- [ ] 6.11.1 操作：Start Profiler → 立即 Stop Profiler（不操作任何组件）→ Get Report
  - 预期日志：totalFrames 很少（< 30），totalRecompositions 接近 0
  - 预期 skill 分析：skill 输出告警"数据量不足，建议采集至少 30 秒再分析"；询问用户是否继续

### 6.12 E2E: 完整流程 — 全量分析 + 报告

- [ ] 6.12.1 操作：Start Profiler → 依次操作 CounterSection+1、AutoIncrement 3 秒、滚动 LazyColumn、点击列表项、ViewModel +1、CustomState Rename → Stop Profiler → Get Report → 运行 skill 全量分析
  - 预期：skill 输出完整 Markdown 报告，包含：
    - 数据概览（sessionDuration、totalFrames、totalRecompositions）
    - 正常重组清单（全 noScope 组件如 ScrollListItem 的滚动重组）
    - 问题诊断（按严重度降序，AutoIncrement 高频排首位）
    - 每个问题含：命中规则、源码定位、诊断描述、优化建议
    - 过滤配置声明
