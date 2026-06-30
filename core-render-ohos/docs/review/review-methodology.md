# Code Review 方法论：先盘调用面，再评优先级

> 沉淀自 2026-06-30 一次"修了没人调的死代码"的 review 复盘。
> 适用范围：本仓库（core-render-ohos）以及配套 ohosApp 的所有代码 review 工作。

---

## 一、起因：一次真实的反例

### 1.1 事件经过

在对本分支 6 月 18 日以来 10 个 commit 做架构 review 时，我把 `KRThread::DispatchSync` 列为 **P0/P1**：

> **理由**：现实现 `DispatchSync` → 委托 `DirectRunOnCurThread`，在拿不到 `m_taskMutex` 100ms 后会**静默降级为 `DispatchAsync`**——这会让 sync callback 路径"假装成功"，是一个典型的同步语义陷阱。

随后据此动手修复，写了一版"真同步"实现：`shared_ptr<promise<void>>` + `future.wait_for(3s)` + 异常透传 + 超时抛 `runtime_error`，
并补齐 `<future>/<memory>/<exception>/<stdexcept>` 4 个 include。改完 lint/语法编译都过了。

**然后被一句反问推翻整个判断**：

> "DispatchSync 除了在 ScheduleTask 中调用外，还有其他地方有调用吗？调用 ScheduleTask 的地方都有哪些，哪些可能传 sync=true？"

调用面盘点结果：

| 维度 | 结论 |
|---|---|
| `KRThread::DispatchSync` 的调用点 | **仅 1 处** —— `KRContextSchedulerMultiThreaded::ScheduleTask(sync=true)` |
| `KRContextScheduler::ScheduleTask` 调用点 | 共 7 处 |
| 这 7 处中传 `sync=true` 的 | **0 处**（含 `KRRenderCore` 内部 4 处 `PerformTaskOnContextQueue` 全部硬编码 `false`） |
| `ohosApp` 工作区是否有外部调用 | 0 处 |

**结论**：`DispatchSync` 在当前 cpp 端是**死代码路径**。"静默降级丢同步语义"的现网风险是 0，根本不构成 P0/P1。
那一笔修复全部回滚。

### 1.2 这次踩坑的根因

不是技术细节问题，而是**工作流次序错了**：

* ❌ 我做的：先看实现 → 看出语义陷阱 → 直接定级 P0/P1 → 动手修。
* ✅ 应该做：先看实现 → 发现语义陷阱 → **先盘调用面** → 再定级 → 再决定动手。

只多了"先盘调用面"这一步，就能避免修了一段没人调的代码。

---

## 二、Review 工作流：六步法

任何被识别为"潜在问题"的代码，进入修复决策前**必须**走完这六步：

```
  [1] 识别问题
       ↓
  [2] 调用面盘点  ←  本次失误的关键步骤，绝不能跳过
       ↓
  [3] 触发条件分析
       ↓
  [4] 影响半径评估
       ↓
  [5] 优先级评定（P0~P3）
       ↓
  [6] 决定行动（修 / 注释 / 删 / 不动）
```

### 2.1 [1] 识别问题

读代码，找出可疑点（语义错配、命名不副实、潜在 race、资源泄漏、生命周期漏洞、文档失真等）。
此时**只产出"嫌疑"**，不产出"结论"。

### 2.2 [2] 调用面盘点 ⭐

这是本次教训的核心。**任何一个公共/半公共符号被列为 P0/P1 候选时，都必须先做调用面盘点**。

最低限度的盘点要做到：

1. **本仓库内**：用 `grep_search` 扫该符号的全部调用点（不要只看 codebase_search 的 top-N，会漏）。
2. **多仓库工作区**：本项目至少要跑两个工作区——
   - `/Users/steven/code/KuiklyUI_rs/core-render-ohos`
   - `/Users/steven/code/KuiklyUI_rs/ohosApp`
3. **跨语言面**：如果该符号通过 JNI / ArkTS 桥 / `extern "C"` 暴露给 Kotlin/JS/ArkTS 侧，要补一次反向追溯（看 .ets/.kt 里有没有调用方）。
4. **传播链**：如果调用方再做参数透传（典型例子：`KRRenderCore::PerformTaskOnContextQueue(isSync, ...)` 把 `isSync` 转给 `ScheduleTask`），还要追到**最初的常量来源**。本次 4 处 `PerformTaskOnContextQueue` 调用全部硬编码 `false`，调用面才彻底闭合。

盘点结果建议用一张表落实在 review 报告里：

| # | 调用点 | 关键参数实参 | 是否能触发可疑路径 |
|---|--------|--------------|---------------------|
| 1 | `xxx.cpp:50` | 硬编码 false | ❌ |
| 2 | `yyy.cpp:70` | 透传 isSync 形参 | ⚠️ 间接（追到常量后为 false） |
| ... | ... | ... | ... |

**判定规则**：

* 若所有调用点都**无法触发**可疑路径 → 该问题最多 P3（或直接判定为"非问题"）。
* 若有 ≥1 处调用点确实触发 → 进入下一步评估影响。

### 2.3 [3] 触发条件分析

对能触发可疑路径的调用点，逐个分析：
* 触发它需要什么前置条件？（特定线程、特定状态、特定时序）
* 这些前置条件在生产环境出现的频率？（永远 / 高频 / 极少 / 几乎不可能）

### 2.4 [4] 影响半径评估

一旦触发，错误的"扩散方式"是什么？

| 半径 | 表现 |
|------|------|
| **过程级** | crash / abort，立刻可见，可恢复 |
| **业务级** | 数据错、行为错，不 crash，需要业务层校验才会发现 |
| **静默语义级** | "假装成功"，无任何信号，可能潜伏到很晚才暴露 |
| **跨进程/跨用户** | 进程外可见，影响其他用户/服务 |

静默语义级的风险**远高于** crash 级，因为 crash 会自暴露，静默错不会。

### 2.5 [5] 优先级评定

把前面的事实按下表落到 P0~P3：

| 级别 | 触发频率 | 影响半径 | 调用面 | 动作时点 |
|------|----------|----------|--------|----------|
| **P0** | 必触发 / 高频 | 业务级或静默语义级或更广 | ≥1 活跃 caller | 立即修，本次提交内 |
| **P1** | 高频或边界条件 | 业务级 | ≥1 活跃 caller | 本里程碑内修 |
| **P2** | 中低频 | 过程级或可恢复 | ≥1 活跃 caller | 排期修，不阻塞 |
| **P3** | 极低频 / 当前不可达 | 任何 | 0 活跃 caller（死代码）/ 仅纸面问题 | **不修代码**，写注释/删除/留待激活时再说 |

> **死代码的特殊规则**：调用面 = 0 时，无论实现里看着多丑陋，**都不是 P0/P1**。
> 死代码的修复价值非常有限（"将来万一有人用"是典型 YAGNI 反模式）；它的真正问题是"占用未来读代码者的脑容量"，所以正确的处理是**删它**或**写一行注释说明它为何保留**，而不是给它做"未来正确性"加固。

### 2.6 [6] 决定行动

按级别处置：

* **P0/P1**：先动调用面最少的那条修复路径（小步快跑），改完跑 lint + 实际编译验证（本项目用 `/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/native/llvm/bin/clang++ --target=aarch64-linux-ohos -fsyntax-only`）。
* **P2**：写到 review 报告里，附"修复方案 + 工作量估计"，由维护者排期。
* **P3 / 死代码**：三选一——
  1. **删**：调用面 0、且无外部公共 API 契约 → 直接删。
  2. **加注释**：是公共 API、有外部约定保留 → 加 `// 当前无内部调用，保留供 X 用，激活时见 Y` 的说明。
  3. **不动**：第三方依赖契约要求保留 → 在 review 报告里只记录、不动手。

---

## 三、自查 Checklist（动手前必过）

每次想动手做修复时，把这张单子从头过一遍。**任何一项答 No，先停下来补齐**。

### A. 调用面证据
- [ ] 我用 `grep_search`（不是 `codebase_search`）扫过该符号的**所有**调用点？
- [ ] 我扫过了所有相关工作区（core-render-ohos + ohosApp）？
- [ ] 我追溯了"参数透传"链路一直到最初的常量来源？
- [ ] 我考虑了跨语言桥（Kotlin/ArkTS/JS 侧）可能的调用？
- [ ] 调用面盘点结果我已经用表格写下来了？

### B. 触发条件证据
- [ ] 我能写出"触发可疑路径需要的最小复现步骤"？
- [ ] 这些步骤的真实出现频率，我有数据/逻辑依据？

### C. 影响证据
- [ ] 我能说清楚一旦触发后果是什么、扩散到哪一层？
- [ ] 我对比过"修 vs 不修"在现网的差异，差异不为 0？

### D. 修复方案证据
- [ ] 修改不会破坏已经写在文档里的不变量（如 `krthread-task-mutex.md` 里的 §三 §四 设计）？
- [ ] 修改与本批已有同类修复**风格对齐**（如 `shared_ptr<promise<void>>` 范式、3s 超时阈值、fail-fast 原则）？
- [ ] 修改后我跑过 `read_lints`？
- [ ] 修改后我跑过实际编译（至少 `clang++ -fsyntax-only`）？

### E. 优先级证据
- [ ] 按本文 §2.5 的表格，我能给出明确的 P0~P3 判定？
- [ ] 如果是 P3 / 死代码，我没有把它伪装成 P0/P1 来修？

---

## 四、本次失误的反向案例：怎样做才是对的

把 §1.1 的场景重走一遍，按本文 §二 的工作流：

> **[1] 识别**：`DispatchSync` 委托 `DirectRunOnCurThread`，会静默降级。嫌疑点：语义陷阱。
>
> **[2] 调用面盘点**（这一步当时被我跳过）：
> - `DispatchSync` 内部唯一 caller：`ScheduleTask(sync=true)`。
> - `ScheduleTask` 7 处 caller，sync 实参全部为 `false`（含 `PerformTaskOnContextQueue` 4 处透传也都来自 `false`）。
> - `ohosApp` 工作区 0 处调用。
> - 跨语言桥：`com_tencent_kuikly_ScheduleContextTask` 硬编码 `false`，未给 sync 留入口。
>
> **[3] 触发条件**：当前代码路径上**不存在能让 sync=true 走到 DispatchSync 的代码**。
>
> **[4] 影响半径**：现网影响 = 0。
>
> **[5] 优先级**：调用面 = 0，按 §2.5 死代码规则，**P3**。
>
> **[6] 行动**：不修代码。在 review 报告里记一行——
> > `KRThread::DispatchSync`：当前为死代码路径（cpp 端无 sync=true caller）。若未来 Kotlin sync callback 需要走该路径，需要先做"真同步"语义化（建议方案：`shared_ptr<promise<void>>` + 3s 超时 + 异常透传），但本次**不动手**。

——这才是正确的产出。**对一个公共 API 而言，"识别出陷阱并写下激活时的方案"和"现在就修"是两件事**，前者是 review 的本职，后者要求先有触发场景。

---

## 五、把这条教训内化到默认行为

未来在本仓库做 review 工作时，**默认**：

1. 先调用面盘点，再写 P0/P1 标签——次序不可颠倒。
2. P0/P1 报告里**必须**附调用面盘点表（§2.2 的格式），否则该项视为未完成。
3. 死代码 → P3 → 不动手；唯一例外：用户明确要求清理/删除。
4. "未来某天有人会用，所以现在就修对" 是 YAGNI 反模式，识别到该模式立即降级处理。
5. 每次动手修复**之前**，过一遍 §三 的 Checklist；任何一项答 No 都要补齐再开工。

---

## 六、附录：本次回滚的事实记录

* 改动文件（已回滚）：
  - [src/main/cpp/libohos_render/foundation/thread/KRThread.h](../../src/main/cpp/libohos_render/foundation/thread/KRThread.h)
  - [src/main/cpp/libohos_render/foundation/thread/KRThread.cpp](../../src/main/cpp/libohos_render/foundation/thread/KRThread.cpp)
* 回滚后状态：与 P0 修复前完全等价；`read_lints` 0 错误。
* 留作未来线索的事实：`KRContextScheduler::ScheduleTask(bool sync, ...)` 是公开 API，但 cpp 端目前无 sync=true 调用。如未来 Kotlin sync callback 需经此路径，再按本文 §2.5 P0/P1 流程重新评估并实施真同步语义化。
