## ADDED Requirements

### Requirement: Skill 触发与日志获取

skill SHALL 在用户提及"重组分析"、"recomp 报告"、"重组优化"、"卡顿分析"等关键词时触发。

skill SHALL 按以下优先级获取日志文件：
1. 当前工作目录下约定位置（如 `./profiler_logs/`）
2. 通过平台命令从设备拉取（需用户提供 BundleID/包名）
3. 失败时提示用户手动提供路径

skill SHALL 支持以下平台的日志获取命令：
- iOS 模拟器：`xcrun simctl get_app_container`
- iOS 真机：`xcrun devicectl device copy from`
- Android：`adb shell run-as <PKG> cat cache/KuiklyProfiler/...`
- HarmonyOS：`hdc file recv`

#### Scenario: 自动获取 Android 日志
- **WHEN** skill 被触发且检测到 adb 连接的 Android 设备
- **THEN** skill SHALL 使用 `adb shell run-as <PKG> cat cache/KuiklyProfiler/profiler_report.json` 拉取报告

#### Scenario: 无法获取日志时引导用户
- **WHEN** skill 无法自动获取日志文件
- **THEN** skill SHALL 输出明确的操作指引："请跑一次 profiler 生成文件，然后告诉我文件路径"

### Requirement: 三阶段漏斗分析

skill SHALL 按三阶段漏斗分析重组问题：Phase 2（report 筛查）→ Phase 3（frames 深挖）→ Phase 3（源码确认），逐层收窄分析范围。

#### Scenario: report 筛查产出嫌疑清单
- **WHEN** skill 读取 `report.json` 完成
- **THEN** skill SHALL 输出嫌疑清单，包含：scope 高频组件、paramChanges 高频组件、State 广播组件

#### Scenario: frames 深挖确认嫌疑并发现新问题
- **WHEN** skill 对嫌疑项拉取 `frames.jsonl` 分析
- **THEN** skill SHALL 同时产出：对 report 嫌疑的确认结果 + frames 本身发现的新嫌疑（帧耗时 > 阈值、帧事件数 > 阈值、单组件耗时 > 阈值）

#### Scenario: 源码确认仅对真正可疑项执行
- **WHEN** 嫌疑项经过 frames 确认后仍可疑
- **THEN** skill SHALL 读取对应 `sourceLocation` 的业务源码，生成诊断和优化建议

### Requirement: noScope 组件直接归为正常

skill SHALL 将 `noScopeRecompositions == recompositionCount` 的组件直接归入"正常重组清单"，不深入分析。

#### Scenario: 全 noScope 组件归入正常清单
- **WHEN** 某组件的 `scopeDistribution` 为空（全部重组无 scope）
- **THEN** skill SHALL 将该组件列入"正常重组清单"，不触发后续分析步骤

### Requirement: 具体 scope 高频触发检测

skill SHALL 遍历每个组件的 `scopeDistribution`，找出某 scopeKey 计数超过 `scopeCountThreshold`（默认 5）的组件，标记为高优先级嫌疑。

#### Scenario: scope 高频触发
- **WHEN** 组件 `ViewModelSection` 的 `scopeDistribution` 为 `{"66116068": 4}` 且 `noScopeRecompositions = 0`
- **THEN** skill SHALL 标记该组件为嫌疑（scope 66116068 被触发 4 次，接近阈值）

### Requirement: 可配置阈值

skill SHALL 支持以下可配置阈值，默认值如下：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `scopeCountThreshold` | 5 | 某 scopeKey 重组次数超过此值 → 嫌疑 |
| `durationThreshold` | 5 | 单次重组耗时超过此值(ms) → 嫌疑 |
| `frameEventThreshold` | 50 | 单帧事件数超过此值 → 帧级热点 |
| `frameDurationThreshold` | 16 | 单帧耗时超过此值(ms) → 帧级卡顿 |
| `paramChangeRateThreshold` | 0.9 | 参数变化率超过此值 → lambda 不稳定 |
| `stateReadersThreshold` | 3 | State readers 数超过此值 → State 广播 |
| `minFramesThreshold` | 30 | 总帧数低于此值 → 数据不足告警 |

#### Scenario: 用户自定义阈值
- **WHEN** 用户通过 skill 参数指定 `scopeCountThreshold=10`
- **THEN** skill SHALL 使用 10 作为 scope 高频阈值，覆盖默认值

### Requirement: 数据健康检查

skill SHALL 在分析开始时检查数据是否充分。当 `totalFrames < minFramesThreshold` 时，SHALL 输出告警并建议用户重新采集。

#### Scenario: 帧数不足
- **WHEN** report.json 显示 `totalFrames = 15` 且 `minFramesThreshold = 30`
- **THEN** skill SHALL 输出"数据量偏少，建议采集至少 30 秒再分析"，并询问用户是否继续

### Requirement: 输出报告

skill SHALL 输出两种产物：
1. **对话摘要**：高层概览 + TOP 3 问题
2. **Markdown 详细报告**：`recomp-analysis-YYYYMMDD-HHmm.md`，按严重程度降序排列，包含"正常重组清单"段

#### Scenario: 生成 Markdown 报告
- **WHEN** 分析完成
- **THEN** skill SHALL 生成包含以下段的报告文件：数据概览、正常重组清单、问题诊断（按严重度降序，每项含诊断/优化建议/源码定位）、过滤配置声明

### Requirement: 形态 C 通过 profiler 重置实现

当用户想聚焦某页面/组件时，skill SHALL 引导用户使用 profiler 面板的"重置"按钮，重新采集一段只含目标操作的日志。

#### Scenario: 用户请求聚焦分析
- **WHEN** 用户说"我只想看 XX 页"
- **THEN** skill SHALL 回复引导文本："请在 profiler 面板点'重置'，进入 XX 页操作一遍，然后让我分析"

### Requirement: 源码定位

skill SHALL 使用 `sourceLocation`（格式 `文件名.kt:行号`）通过 `Glob "**/<文件名>.kt"` 定位源码文件，再读取对应行。

#### Scenario: 定位到源码
- **WHEN** 嫌疑组件的 `sourceLocation` 为 `CounterSection.kt:221`
- **THEN** skill SHALL 通过 Glob 搜索 `**/CounterSection.kt`，读取第 221 行附近代码

#### Scenario: 找不到源码
- **WHEN** Glob 搜索无结果或匹配到多个不同路径的文件
- **THEN** skill SHALL 标注"源码未定位"并继续分析其他项
