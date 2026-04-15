# 测试计划：Profiler 自定义过滤功能

## 前置准备

1. 编译运行 Demo App（iOS Simulator 或 Android Emulator 均可）
2. 进入 Demo 首页 → 找到 **「Recomposition Profiler」** 入口
3. 日志过滤关键字：`RCProfiler`（KLog TAG）

---

## 第一部分：核心 API 测试

> 在 `RecompositionProfilerDemoPage` 里的「启动 Profiler」按钮附近临时加入以下调用，跑完后删掉

### TC-01 基础过滤名称

**优先级**：P0

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("CounterSection", "StatusBar")
```

**预期**：
- 日志：`Custom filter updated — names: [CounterSection, StatusBar], prefixes: []`
- 面板里 `CounterSection` 不再出现

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-02 追加语义（不替换）

**优先级**：P1

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("A")
RecompositionProfiler.excludeByName("B")
```

**预期**：
- 第二次日志：`names: [A, B]`（不是只有 `[B]`）

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-03 前缀过滤

**优先级**：P1

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByPrefix("com.tencent.kuikly.demo.pages.compose.Counter")
```

**预期**：
- 日志：`prefixes: [com.tencent.kuikly.demo.pages.compose.Counter]`
- 所有名称含该前缀的 Composable 不出现在面板和日志中

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-04 vararg 与 List 两种调用方式

**优先级**：P1

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("Foo", "Bar")       // vararg
RecompositionProfiler.excludeByName(listOf("Baz"))      // List
```

**预期**：
- 日志中 names 包含 `[Bar, Baz, Foo]`（Set 自动去重排序）
- 两种调用方式均有效

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-05 空白字符串被过滤掉

**优先级**：P1

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("", "  ", "ValidName")
```

**预期**：
- 只有 `ValidName` 进入 excludedNames，空字符串和空格字符串被丢弃
- 日志：`names: [ValidName]`

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-06 幂等（重复添加同名）

**优先级**：P1

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("MyWidget")
RecompositionProfiler.excludeByName("MyWidget")
```

**预期**：
- `excludedNames` 中只有一个 `MyWidget`（Set 语义）
- 两次均输出日志（每次调用都触发，但 Set 内容相同）

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-07 `configure{}` 与 `excludeByName` 不互相覆盖

**优先级**：P0（🔴 关键修复验证）

**操作**：

新建一个简单 filter 对象：
```kotlin
class StaticFilter : ComposableFilter {
    override fun shouldFilter(composableName: String, info: String) =
        composableName == "StaticTarget"
}

RecompositionProfiler.configure {
    customFilters = listOf(StaticFilter())
}
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("DynamicWidget")
```

**预期**：
- `StaticTarget` 不出现在面板和日志中（StaticFilter 依然生效）
- `DynamicWidget` 也不出现（动态规则生效）
- 两者互不覆盖

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-08 清空过滤

**优先级**：P0

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("Foo")
// 确认 Foo 不出现后：
RecompositionProfiler.clearCustomFilters()
```

**预期**：
- 清空时日志：`Custom filter cleared`
- 之后 `Foo` 重新出现在面板中

**追加验证（连续清空）**：
```kotlin
RecompositionProfiler.clearCustomFilters()  // 已空
```
**预期**：无日志输出（空操作不触发日志）

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-09 start 前配置，start 后生效

**优先级**：P0

**操作**：
```kotlin
// 先配置，再启动
RecompositionProfiler.excludeByName("PreStartWidget")
RecompositionProfiler.start()
// 立即触发重组
```

**预期**：
- `PreStartWidget` 从 session 一开始就被过滤，不出现在面板和日志中

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-10 Report 中的过滤快照

**优先级**：P0

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("Widget1")
RecompositionProfiler.excludeByPrefix("com.myapp.")
val report = RecompositionProfiler.getReport(saveToFile = false)
println("names=${report.filteredNames}")
println("prefixes=${report.filteredPrefixes}")
println(report.toJson())
```

**预期**：
- `report.filteredNames == ["Widget1"]`
- `report.filteredPrefixes == ["com.myapp."]`
- JSON 包含：
  ```json
  "filteredNames": ["Widget1"],
  "filteredPrefixes": ["com.myapp."]
  ```

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-11 `saveToFile=false` 不触发日志输出

**优先级**：P1

**操作**：
```kotlin
RecompositionProfiler.start()
// 第一次：静默获取
RecompositionProfiler.getReport(saveToFile = false)
// 第二次：正常获取
RecompositionProfiler.getReport(saveToFile = true)
```

**预期**：
- 第一次：logcat 中没有 `RCProfiler` 报告输出
- 第二次：logcat 有报告汇总输出（`=== Recomposition Report ===`）

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

## 第二部分：Overlay 交互测试

> 启动前配置 `enableOverlay = true`：
> ```kotlin
> RecompositionProfiler.configure { enableOverlay = true }
> RecompositionProfiler.start()
> ```

### TC-12 「过滤」按钮出现

**优先级**：P0

**操作**：
1. 展开 Overlay 面板（点击右下角悬浮按钮）
2. 触发几次重组让热点列表有内容

**预期**：
- 每个热点行右侧有「过滤」按钮

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-13 点击「过滤」后视觉反馈

**优先级**：P0

**操作**：
1. 点击某热点（如 `CounterSection`）的「过滤」按钮

**预期**：
- 按钮变为「已过滤」灰色禁用态（不可再次点击）
- 该行名称和次数文字变灰
- 日志：`Custom filter updated — names: [CounterSection], prefixes: []`

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-14 过滤后新数据不再累积

**优先级**：P0

**操作**：
1. 过滤 `CounterSection`
2. 继续触发重组（点击「+」按钮或其他操作）
3. 等待约 1 秒面板刷新

**预期**：
- 面板中 `CounterSection` 的计数不再增加
- 日志中不出现 `RECOMPOSED: CounterSection`

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-15 「清空过滤」按钮

**优先级**：P0

**操作**：
1. 先过滤至少一个热点
2. 点击控制栏「清空过滤」按钮

**预期**：
- 日志：`Custom filter cleared`
- 下一帧后，已过滤的组件重新开始出现在面板中

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-16 无过滤时点击「清空过滤」

**优先级**：P2

**操作**：
1. 确保当前没有自定义过滤规则
2. 点击「清空过滤」按钮

**预期**：
- 无任何日志输出（幂等，空操作）
- 无 crash

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-17 「过滤」与「重置」配合

**优先级**：P1

**操作**：
1. 过滤几个热点
2. 点击「重置」按钮

**预期**：
- 面板数据清空（计数归零）✓
- 过滤规则**保留**（重置只清数据，不清配置）
- 再触发重组，被过滤的组件仍不出现在面板中

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-18 控制栏在窄屏不截断

**优先级**：P2

**操作**：
1. 在较窄的机型（或缩小模拟器宽度）打开 Overlay 面板

**预期**：
- 4 个按钮（暂停/重置/报告/清空过滤）可以完整显示，不被截断或遮挡

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

## 第三部分：边界与稳定性测试

### TC-19 运行中动态修改过滤规则

**优先级**：P1

**操作**：
```kotlin
// Profiler 已经 start()
RecompositionProfiler.excludeByName("W1")
// 等 1 秒
RecompositionProfiler.excludeByName("W2")
// 等 1 秒
RecompositionProfiler.clearCustomFilters()
// 等 1 秒
RecompositionProfiler.excludeByPrefix("com.test.")
```

**预期**：
- 每次操作不 crash
- 每次有规则变更时输出对应日志
- 每次变更立即在下一帧生效

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-20 stop 后过滤规则保留，再次 start 仍生效

**优先级**：P1

**操作**：
```kotlin
RecompositionProfiler.excludeByName("MyWidget")
RecompositionProfiler.start()
// 确认 MyWidget 被过滤
RecompositionProfiler.stop()
RecompositionProfiler.start()   // 重新启动新 session
// 触发重组
```

**预期**：
- 新 session 中 `MyWidget` 依然被过滤，不出现在面板和日志中

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-21 `configure{}` 多次调用不丢失动态过滤规则

**优先级**：P1（🔴 关键修复验证）

**操作**：
```kotlin
RecompositionProfiler.start()
RecompositionProfiler.excludeByName("DynWidget")
// 只修改其他配置，不涉及 customFilters：
RecompositionProfiler.configure { hotspotThreshold = 20 }
// 触发重组
```

**预期**：
- `DynWidget` 依然被排除（`configure` 不覆盖动态过滤 Set）
- 日志中不出现 `RECOMPOSED: DynWidget`

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

### TC-22 大量规则下性能无明显劣化

**优先级**：P2

**操作**：
```kotlin
val names = (1..200).map { "Widget$it" }
RecompositionProfiler.start()
RecompositionProfiler.excludeByName(names)
// 持续触发重组约 30 秒
```

**预期**：
- 无明显卡顿（帧率无明显下降）
- 日志输出正常
- 无 OOM 或 crash

**结果**：`[ ] PASS  [ ] FAIL  [ ] SKIP`

**备注**：

---

## 汇总表

| 用例 | 优先级 | 分类 | 状态 |
|------|-------|------|------|
| TC-01 基础过滤名称 | P0 | API | ✅ PASS |
| TC-02 追加语义 | P1 | API | ✅ PASS |
| TC-03 前缀过滤 | P1 | API | ✅ PASS |
| TC-04 vararg / List | P1 | API | ✅ PASS |
| TC-05 空白字符串 | P1 | API | ✅ PASS |
| TC-06 幂等 | P1 | API | ✅ PASS |
| TC-07 configure 不覆盖 excludeByName | P0 | API | ✅ PASS |
| TC-08 清空过滤 | P0 | API | ✅ PASS |
| TC-09 start 前配置生效 | P0 | API | ✅ PASS |
| TC-10 Report 过滤快照 | P0 | API | ✅ PASS |
| TC-11 saveToFile=false 无日志 | P1 | API | ✅ PASS |
| TC-12 「过滤」按钮出现 | P0 | Overlay | ✅ PASS |
| TC-13 点击过滤视觉反馈 | P0 | Overlay | ✅ PASS |
| TC-14 过滤后新数据不累积 | P0 | Overlay | ✅ PASS |
| TC-15 「清空过滤」按钮 | P0 | Overlay | ✅ PASS |
| TC-16 无过滤时清空幂等 | P2 | Overlay | ✅ PASS |
| TC-17 过滤与重置配合 | P1 | Overlay | ✅ PASS |
| TC-18 窄屏不截断 | P2 | Overlay | ⏭️ SKIP |
| TC-19 动态修改规则 | P1 | 稳定性 | ✅ PASS |
| TC-20 stop/start 规则保留 | P1 | 稳定性 | ✅ PASS |
| TC-21 configure 多次不丢规则 | P1 | 稳定性 | ✅ PASS |
| TC-22 大量规则性能 | P2 | 稳定性 | ✅ PASS |

**P0 共 9 项（必测）｜P1 共 9 项（应测）｜P2 共 4 项（可选）**

---

## 预期日志格式速查

```
# 添加过滤
I/RCProfiler: Custom filter updated — names: [WidgetA, WidgetB], prefixes: [com.myapp.]

# 清空过滤
I/RCProfiler: Custom filter cleared

# 报告 JSON（片段）
{
  "filteredNames": ["WidgetA", "WidgetB"],
  "filteredPrefixes": ["com.myapp."],
  ...
}
```
