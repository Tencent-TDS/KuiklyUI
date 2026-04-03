# AI 自动分析集成方案

> **status: draft** — 尚未实现，计划在独立 change 中完成。

## 背景

现有 Profiler 输出的重组信息（日志、JSON 报告）需要开发者人工翻查分析。
目标是让 AI 读取重组数据、结合业务代码，自动识别性能瓶颈并给出优化建议。

**目标场景（被动分析）**：用户操作完页面，触发 `stop()`，AI 自动获取报告 JSON + 读业务代码 → 输出分析报告。不需要 AI 控制 App 操作。

---

## 核心问题：数据取出通道

AI 分析的前提是拿到结构化的报告 JSON。问题在于 **App 的数据被困在设备 sandbox 里**，不同场景下取出通道差异很大。

### 场景一：iOS/Android 模拟器（⭐ 最可行）

| 取数方式 | 可行性 | 说明 |
|---------|--------|------|
| `FileOutputStrategy` 写文件到 sandbox，AI 直接读 | ✅ | 模拟器权限开放，路径可预测 |
| 读 console log（`./logs/kuikly_console.log`）| ✅ | 现有方案，非结构化 |

**结论**：模拟器场景完全可行，AI 可以自动化整个流程。

### 场景二：真机 + USB 连线

| 平台 | 取数命令 | 可行性 | 说明 |
|------|---------|--------|------|
| iOS 真机 | `xcrun devicectl device copy from --device <id> --domain-type appDataContainer --domain-identifier <bundle> --source Library/Caches/KuiklyProfiler/profiler_report.json /tmp/profiler_report.json` | ✅ | debug 签名即可，无需 root |
| Android 真机/模拟器 | `adb shell run-as <pkg> cat cache/KuiklyProfiler/profiler_report.json` | ✅ | debug 包可用 |
| HarmonyOS | `hdc file recv /data/storage/el2/base/cache/KuiklyProfiler/profiler_report.json /tmp/profiler_report.json` | ⚠️ 待验证 | 路径需确认 |

**结论**：接线场景理论可行，需逐平台验证。

### 场景三：真机 + 无线（WiFi）

无可靠方案，暂不支持。剪贴板有字符上限，HTTP Server 依赖网络环境，均不稳定。

---

## 推荐方案：FileOutputStrategy + Skill

### 数据流

```
用户操作 App
    ↓
RecompositionProfiler.start()
    ↓（每帧有重组时，距上次写入 ≥2s 时批量 append）
    Library/Caches/KuiklyProfiler/profiler_frames.jsonl  ← 逐帧详情
    ↓
RecompositionProfiler.stop() 或 getReport(saveToFile=true)
    ↓（覆盖写）
    Library/Caches/KuiklyProfiler/profiler_report.json   ← 聚合报告
    ↓
模拟器：xcrun simctl get_app_container → 直接读
iOS 真机：xcrun devicectl device copy from → 拉到本地
Android：adb shell run-as <pkg> cat → 输出到本地
鸿蒙：hdc file recv → 拉到本地
    ↓
AI 读取 JSON + 读业务源码 → 分析热点 → 输出优化建议
```

### AI 分析内容

拿到 JSON 后，AI 结合业务代码做以下分析：

1. **热点识别**：`recompositionCount` 最高的 Composable，结合源码看是否合理
2. **触发原因分析**：`triggerStates` 字段，识别哪些 State 变化频率最高
3. **参数变更检测**：`paramChanges` 字段，找出不必要的父组件传参导致的重组
4. **逐帧时序分析**：`profiler_frames.jsonl` 中有时间戳，可分析某段操作期间的重组变化过程
5. **优化建议**：针对具体问题给出 `remember`/`derivedStateOf`/参数稳定化等建议

---

## FileOutputStrategy 设计

### 输出策略架构

`RecompositionProfiler` 内置三类输出策略，均通过 `enableXxx` 配置项开关，用户无需手动注册：

| 策略 | 配置项 | 默认 | 说明 |
|------|--------|------|------|
| `LogOutputStrategy` | `enableLog` | ✅ true | 打印结构化日志，人工查看 |
| `FileOutputStrategy` | `enableFile` | ✅ true | 写文件，供 AI 分析 |
| `OverlayOutputStrategy` | `enableOverlay` | ❌ false | 热点 UI 可视化面板 |

公开 API 极简：

```kotlin
// 配置并启动（enableLog/enableFile 默认均为 true，无需显式设置）
RecompositionProfiler.configure {
    hotspotThreshold = 5
    enableOverlay = true   // 需要可视面板时开启
}
RecompositionProfiler.start()

// 获取报告（默认同时写文件）
val report = RecompositionProfiler.getReport()           // saveToFile = true（默认）
val report = RecompositionProfiler.getReport(saveToFile = false)  // 只读不写

// 停止（自动 flush 剩余帧 + 写聚合报告）
RecompositionProfiler.stop()
```

### 文件写入方案：FileModule（原生 Module 桥接）

`profiler` 模块无法直接访问原生文件系统（js 动态化模式下 expect/actual 不可用），采用 **KuiklyUI Module 桥接**方案：

```
FileModule（core/src/commonMain/）
  - writeFile(filename, content, callback)   ← 覆盖写
  - appendFile(filename, content, callback)  ← 追加写，末尾自动加换行
  - getFilesDir(callback)                    ← 返回平台的可写目录路径

各平台原生实现：
  - iOS:        Library/Caches/KuiklyProfiler/   （NSCachesDirectory）
  - Android:    context.cacheDir/KuiklyProfiler/
  - HarmonyOS:  /data/storage/el2/base/cache/KuiklyProfiler/

注册位置（SDK 层，宿主 App 无需手动注册）：
  - iOS:        core-render-ios/Extension/Modules/KRFileModule.m
  - Android:    KuiklyRenderViewBaseDelegator.registerModule()
  - HarmonyOS:  ModulesRegisterEntry.registerSDKModules()（待实现）
```

**Module 实例传递机制**：

`FileModule` 需要在 `Page` 上下文中获取，`RecompositionProfiler` 是全局单例。

解法：
1. `ComposeContainer` 在 `viewDidLoad` 时注册 `fileModuleListener`
2. `start()` 触发 `onProfilerStarted`，`ComposeContainer` 调 `getModule<FileModule>()` 并传给 `RecompositionProfiler.setFileModule()`
3. `setFileModule()` 创建 `FileOutputStrategy` 并 `activate()`，清空上次 frames 文件

### 写入文件设计

| 文件 | 写入时机 | 写入方式 | 内容 |
|------|---------|---------|------|
| `profiler_frames.jsonl` | 每次有帧完成时检查，距上次写入 ≥2s 则批量 append | 追加写（`appendFile`） | 逐帧详情：每行一个帧 JSON，含时间戳、各 Composable 触发原因和参数变更 |
| `profiler_report.json` | `stop()` 时 / `getReport(saveToFile=true)` 时 | 覆盖写（`writeFile`） | 聚合报告：热点排名、平均耗时、State 变更统计 |

**帧写入节流实现**：

`FileOutputStrategy.onFrameComplete()` 被每帧调用，采用**惰性时间戳检查**而非独立定时器：

```kotlin
override fun onFrameComplete(events: List<RecompositionEvent>) {
    if (!active) return
    pendingFrames.add(buildFrameJson(events))          // 入缓冲区
    val now = DateTime.currentTimestamp()
    if (now - lastAppendMs >= APPEND_INTERVAL_MS) {    // 距上次写入 ≥2s
        flushPendingFrames()                           // 批量 append
        lastAppendMs = now
    }
}
```

- **优点**：无额外线程/协程，实现简单
- **特点**：页面无重组时不会触发写入（但也没数据需要写）；`stop()` 时的 `deactivate()` 会 flush 剩余帧，不丢数据

**FileModule 异步调用**：`writeFile` / `appendFile` / `getFilesDir` 均为异步方法（`asyncToNativeMethod`），通过 callback 返回结果，不阻塞 UI 线程。

### 各平台写入路径

写入目录统一使用 **Caches 子目录**（而非 Documents/files），原因：
- 不纳入 iCloud 备份，避免用户数据增大
- `xcrun devicectl device copy from --domain-type appDataContainer` 同样可访问
- 多个页面（Pager）共享同一目录，同名文件后写覆盖前写（多页面场景以最后一次 stop 为准）
- 系统磁盘紧张时可能被清理（这对 Profiler 临时数据是可接受的）

| 平台 | 写入路径 | AI 读取命令 |
|------|---------|-------------|
| iOS 模拟器 | `<app_data>/Library/Caches/KuiklyProfiler/` | `cat "$(xcrun simctl get_app_container <sim_id> <bundle_id> data)/Library/Caches/KuiklyProfiler/profiler_report.json"` |
| iOS 真机 | 同上（sandbox） | `xcrun devicectl device copy from --device <device_id> --domain-type appDataContainer --domain-identifier <bundle_id> --source Library/Caches/KuiklyProfiler/profiler_report.json /tmp/profiler_report.json` |
| Android（模拟器/真机） | `/data/data/<pkg>/cache/KuiklyProfiler/` | `adb shell run-as <pkg> cat cache/KuiklyProfiler/profiler_report.json` |
| HarmonyOS | `/data/storage/el2/base/cache/KuiklyProfiler/` | `hdc file recv /data/storage/el2/base/cache/KuiklyProfiler/profiler_report.json /tmp/profiler_report.json` |

**关键参数获取方法**：

```bash
# 获取已连接 iOS 真机的 Device ID
xcrun devicectl list devices

# 获取 Bundle ID（从项目配置）
grep PRODUCT_BUNDLE_IDENTIFIER iosApp/iosApp.xcodeproj/project.pbxproj | head -3

# 获取 iOS 模拟器列表（找 Booted 的那个）
xcrun simctl list devices | grep Booted
```

**多 App / 多页面场景说明**：
- 同一 App 内多个页面（Pager）均调用 `stop()` 时，写入同一文件路径，**后写覆盖前写**，AI 分析的是最后一次 stop 的报告
- `profiler_frames.jsonl` 每次 `start()` 时自动清空，不会累积多个 session 的数据
- 多个不同 App 各自有独立 sandbox，不会互相覆盖

---

## 验证进展

### ✅ iOS 模拟器（2026-04-01 验证通过）

1. `FileModule` 实现：`KRFileModule.m` 通过 `dispatch_async` + `KR_CALLBACK_KEY` 异步写入 Caches/KuiklyProfiler/ 目录
2. 写入路径：`<simctl data container>/Library/Caches/KuiklyProfiler/profiler_test.json`
3. AI 读取命令验证通过：
   ```bash
   APP_DATA=$(xcrun simctl get_app_container <SIM_ID> <BUNDLE_ID> data)
   cat "$APP_DATA/Library/Caches/KuiklyProfiler/profiler_test.json"
   ```
4. 文件内容正确读取 ✅

> 注：`get_app_container` 返回的路径包含动态 UUID，每次 App 数据清除后会变，但 `xcrun simctl get_app_container` 命令始终能正确找到当前路径。

### ✅ iOS 真机（2026-04-01 验证通过）

1. 设备：iPhone，Device ID `EC6A270C-CCE2-575B-8B0F-8E811361DADB`（通过 `xcrun devicectl list devices` 获取）
2. Bundle ID：`com.tencent.kuiklycore.demo.luoyibu1`（通过 `grep PRODUCT_BUNDLE_IDENTIFIER project.pbxproj` 获取）
3. 文件写入验证通过，读取命令：
   ```bash
   xcrun devicectl device copy from \
     --device EC6A270C-CCE2-575B-8B0F-8E811361DADB \
     --domain-type appDataContainer \
     --domain-identifier com.tencent.kuiklycore.demo.luoyibu1 \
     --source Library/Caches/KuiklyProfiler/profiler_test.json \
     /tmp/profiler_test.json
   ```
4. 文件内容 `{"test":"hello from KuiklyUI"}` 正确读取 ✅
5. **注意**：首次安装需在 iPhone「设置 → 通用 → VPN与设备管理」信任开发者证书（团队 ID：W87D9D2E4L）

### ✅ Android 模拟器（2026-04-01 验证通过）

1. `KRFileModule.kt` 写入 `context.cacheDir/KuiklyProfiler/`，后台线程异步写文件
2. 注册位置：SDK 层 `KuiklyRenderViewBaseDelegator.registerModule()`，宿主 App 无需手动注册
3. 读取命令验证通过：
   ```bash
   adb shell run-as com.tencent.kuikly.android.demo \
     cat cache/KuiklyProfiler/profiler_test.json
   ```
4. 文件内容 `{"test":"hello from KuiklyUI"}` 正确读取 ✅

### ⏳ HarmonyOS 待验证

- 实现：`/data/storage/el2/base/cache/KuiklyProfiler/`（待补充 HarmonyOS 原生实现）
- 读取命令：`hdc file recv /data/storage/el2/base/cache/KuiklyProfiler/profiler_report.json /tmp/profiler_report.json`

---

## recomposition-analyzer Skill 完整设计

### 触发方式

用户主动触发，关键词：「分析重组」「看看 profiler 报告」「哪里重组太多」「分析性能」等。

### 工作流（六步，刚性顺序执行）

#### 第一步：确认平台 + 拉取文件

询问用户：
- 平台：iOS 模拟器 / iOS 真机 / Android 模拟器 / Android 真机 / HarmonyOS
- 获取对应参数（SIM_ID / Device ID / Bundle ID / Package Name）

拉取两个文件，按时间戳存本地，支持多次验证对比：
```
/tmp/kuikly_profiler/
  report_<YYYYMMDD_HHmmss>.json
  frames_<YYYYMMDD_HHmmss>.jsonl
```

#### 第二步：基础概览

从 `profiler_report.json` 提取：
- 会话时长 `durationMs`
- 总帧数 `totalFrames`
- 总重组次数 `totalRecompositions`
- 平均每帧重组次数 = totalRecompositions / totalFrames

#### 第三步：11 条可疑项识别

逐条过筛，标记 ✅ 正常 / ⚠️ 可疑 / 🔴 问题：

| # | 维度 | 判断条件 | 数据来源 |
|---|------|---------|---------|
| 1 | 热点绝对排名 | `recompositionCount` 最高的前 5 个 | `hotspots` |
| 2 | 相对占比异常 | 单组件 count / totalRecompositions > 30% | `composables` |
| 3 | 短时突发重组 | frames 里某组件在 1s 内出现 > 10 帧 | `frames.jsonl` |
| 4 | 父传参不稳定 | `paramChanges` 非空且 `triggerStates` 为空或少 | `composables` |
| 5 | State 粒度粗 | 同一 State 的 `readers` 包含 3+ 个不同组件 | `stateChanges` |
| 6 | 偶发卡顿 | `maxDurationMs` > `avgDurationMs` × 5 | `composables` |
| 7 | 持续超帧 | `avgDurationMs` > 8ms（超出半帧预算） | `composables` |
| 8 | 大量未知原因 | `reason=UNKNOWN` 占该组件总重组 > 50% | `composables` |
| 9 | 集合/不稳定类参数 | 源码参数类型为 `List`/`Map` 或含 `var` 字段的类 | 源码 |
| 10 | Lambda 无 remember | 源码中函数类型参数未被 `remember` 包裹 | 源码 |
| 11 | LazyColumn 无 key | 源码中 `LazyColumn { items(...) }` 没有 `key = {}` | 源码 |

**注**：维度 9/10/11 需要 grep 源码验证；frames 仅在维度 3 和辅助验证时使用，能从代码直接判断则跳过 frames。

#### 第四步：深度分析

对每个 ⚠️/🔴 项：
1. 在项目中 grep 定位对应 Composable 源码
2. 阅读代码逻辑，结合 `triggerStates`/`paramChanges`/时序 给出具体诊断
3. 如无法从代码直接判断，读 `frames.jsonl` 中该组件的相关帧辅助验证

#### 第五步：输出中文分析报告

格式：
```
## 重组性能分析报告

### 概览
- 分析时长：Xs，总帧数：N，总重组：M次，平均每帧：X.X次

### 发现的问题（共 N 条）

#### 🔴 问题1：<ComponentName> 高频重组（N次，占比X%）
- **现象**：...
- **根因**：<代码片段 + 说明>
- **修复方案**：
  ```kotlin
  // 修改前
  // 修改后
  ```

#### ⚠️ 问题2：...

### 总结
- 修复优先级排序
- 预期收益
```

#### 第六步：询问下一步

提供三个选项：
1. **立即修复**：按报告逐条改代码 → 提示用户重跑 Profiler（start→操作→stop）→ 拉新文件（新时间戳）→ 与上次报告对比（recompositionCount 变化对比） → 确认修复效果 → 检测 `git remote` 是否工蜂 → 发 MR
2. **先看看**：用户自行评估，随时可再次触发 skill
3. **部分修复**：选择某几条修复

**MR 流程**：
```bash
# 检测是否工蜂
git remote get-url origin
# 包含 git.woa.com / tgit.io → 工蜂，直接建分支
git checkout -b fix/recomposition-<component>-<date>
git push origin fix/recomposition-<component>-<date>
# 调用 mcp__gongfeng__create_merge_request
```

---

## 实现优先级

1. ✅ **P0（验证）**：iOS 模拟器、iOS 真机、Android 模拟器文件读取通道均已验证通过
2. ✅ **P1（实现）**：`FileOutputStrategy` 已实现，`stop()` 自动写报告，每 2s 批量写帧数据
3. ⏳ **P2（Skill 完整版）**：`recomposition-analyzer` skill 升级为六步完整工作流（见 tasks.md 11.3）
4. ⏳ **P3（HarmonyOS）**：补充 HarmonyOS FileModule 原生实现 + 验证
5. 📋 **P4（远期）**：MCP Server 封装分析能力，支持对比分析

---

## 与 tasks.md 的关联

待实现任务见 `tasks.md` Section 11。

