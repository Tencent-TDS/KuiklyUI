# BugFix：Android Pager 切页 / 滑动卡顿

平台：Android　DSL 类型：compose-dsl　Demo 页面：`PagerJankRepro`（已存在，注册名 PagerJankRepro）

---

## REPRO

bug 现象：Kuikly Compose DSL 的 HorizontalPager 在 Android 上切页/滑动卡顿。

复现方式（无需用户手工操作，脚本化）：
- 点击 Tab 切页：Tab 行 y≈289，5 个 tab x = 108/324/540/756/972
- 滑动：`adb shell input swipe 800 1200 300 1200 300`
- 日志：`adb logcat -s System.out | grep PAGER_JANK`

复现结论：**可稳定复现**，详见下方「指标基线」。

---

## DIAGNOSE

### 指标基线（beyond=0，forceRemeasure=true 即官方一致状态，真机 OCE-AN10 / API 31）

| 场景 | scrollToPage 耗时 | 帧数据 |
|---|---|---|
| 点击 Tab 切页 | 222~280ms | — |
| 滑动 6 次 | — | Janky 60.42%，Slow UI 29 帧，P50 20ms，P95/P99 24ms |

注意：`scrollToPage` 返回耗时在 `forceRemeasure=false` 时是**乐观指标**（异步失效，成本推到下一帧），帧数据才是用户感知的真实指标。

### 假设与验证结果（按可能性排序，已用 PROF 插桩验证）

**假设 1：工作发生在主线程** —— ❌ 证伪
- 证据：所有 PROF 日志的 `thread=HRContextQueueHandlerThread`（Kuikly 线程），主线程未出现
- 线程表确认：main=9968，HRContextQueueHandlerThread=10111

**假设 2：切页卡顿由 `scrollToPage` 的 `forceRemeasure=true` 导致** —— ✅ 部分成立
- 证据：`snapToItem forceRemeasure` 258ms，其中 `MeasureAndLayoutDelegate.measureAndLayout` 257ms（占 99.6%）
- 但**官方源码就是 `true`**（androidx androidx-main foundation PagerState.kt:566），Kuikly 与官方逐行一致
- 所以这不是 Kuikly 的 bug，改 false 是「牺牲同步语义换速度」的权衡，非修复

**假设 3：滑动卡顿源于新页全部首次组合、无一命中复用** —— ✅ 成立（当前最强假设）
- 证据：滑动抓到 **11 次 `subcomposeInto INIT`，0 次 `REUSE`**，单次 8~20ms，合计约 147ms
- `INIT` = `setContent`（首次组合），`REUSE` = `setContentWithReuse`
- `reuseContent` 由 `nodeState.forceReuse` 决定，全 INIT 说明 `forceReuse` 恒为 false
- 滑动帧内：多数帧走 `scroll WITHOUT remeasure`（1~5ms，正常），**跨页那一帧走 WITH remeasure（241ms）**，这就是「一卡一卡」的来源

**假设 4：窗口外首次组合是固有成本，beyond 可缓解** —— ⚠️ 部分成立但收益有限
- beyond=0/1/2/3 实测：加大 beyond 只能加宽窗口，窗口外首次组合成本不变
- 修复后（false）beyond=0 与 beyond=1 帧数据无显著差异（Janky 9.32% vs 12.26%），beyond 反而略差

### 关联既有文档

`BugFix/TODO-pager-snapshot-perf.md` §1 记录了「Pager 滑动快速路径被 measure 订阅打穿」，
与本假设 3 的现象吻合（每帧全量 measure、快速路径失效）。该 TODO 标注「待量化验证」，
本次 PROF 数据可作为量化依据。

### 下一步

查 `SubcomposeLayout` 中 `nodeState.forceReuse` 的赋值点，确认为何滑动时恒为 false。
这是比 `forceRemeasure` 更根本的优化点：若 INIT 能转 REUSE，可压缩约 147ms 组合成本，
且不需要改动与官方一致的 `forceRemeasure=true`。

---

## DIAGNOSE（第二轮，修正第一轮判断）

插桩位置：`SubcomposeLayout.subcompose`（按 slotId 打印 REUSE/INIT + 耗时）、
`takeNodeFromReusables` 返回 null 时打印复用池状态。
触发方式：点击 Tab 切页（滑动用 `input swipe` 未稳定触发跨页，待解决）。

### 实测（单次切页，beyond=0）

```text
subcompose slot=tab_2   REUSE  cost=28.9ms     ← 页面根节点命中复用
subcompose MISS slotId=20001 reusableCount=0 precomposedCount=0 children=0
subcompose slot=20001   INIT  cost=9.5ms
subcompose MISS slotId=20002 reusableCount=0 precomposedCount=0 children=1
subcompose slot=20002   INIT  cost=9.6ms
  ...（20003~20010 同模式，6~9ms）
subcompose slot=20010   INIT  cost=5.9ms
```

汇总：1 REUSE + 10 INIT + 10 MISS。

### 修正第一轮的结论

第一轮说「11 次全 INIT、0 次 REUSE」——**不准确**。实际是：

- **页面根（`tab_N`）走 REUSE**，耗时 28.9ms：页面容器本身被复用了
- **页面内 10 个列表项（slotId 20001~20010）全走 INIT**，每项 6~9.5ms，合计约 77ms
- 复用池全程为空：`reusableCount=0 precomposedCount=0`，`children` 从 0 递增到 9（逐项新建）

即：**容器复用了，内容全部重建**。真正开销在「页面内部列表项的首次组合」，
不是「整个页面的首次组合」。定位比第一轮更精确。

### 未拿到的数据

- MAL（measureAndLayout）阶段细分未输出：切页未走 `measureAndLayout(layoutNode, constraints)`
  这个重载，可能走的是无参版 `measureAndLayout()`。需换插桩点。
- 滑动路径未稳定触发（用 `adb shell input swipe` 跨不了页），滑动的数据仍是缺口。

### 下一步（待确认方向）

1. 查 `disposeOrReuseStartingFromIndex` 的调用时机：为什么列表项的 composition 在离开时被
   dispose 而不是进复用池（`reusableCount=0`）
2. 确认列表项 slotId（20001~20010）的 key 策略：`areCompatible` 是否让它们无法互相复用
3. 换插桩点拿到 MAL 阶段细分，区分组合 / 布局 / 原生视图创建

---

## DIAGNOSE（第三轮：复用策略定位）

### 找到的两套复用策略（`SubcomposeLayout.kt`）

| 策略 | `getSlotsToRetain` | `areCompatible` |
|---|---|---|
| `FixedCountSubcomposeSlotReusePolicy`（:1319） | 保留最多 N 个 | 恒 `true`（:1339） |
| `NoOpSubcomposeSlotReusePolicy`（:1342） | **`slotIds.clear()`**（清空） | 恒 `false`（:1347） |
| `LazyLayoutItemReusePolicy`（LazyLayout.kt:139） | 按 contentType 保留上限 7 | contentType 相同才可复用 |

**关键**：`SubcomposeLayoutState` 的无参构造（:499）默认用 `NoOpSubcomposeSlotReusePolicy`
—— 它的 `getSlotsToRetain` 直接 `clear()`，意味着**每次都不保留任何 slot，全部 dispose**。
这与实测的 `reusableCount=0`、`children` 从 0 逐项新建完全吻合。

### 复用失败链路（推断，未最终确认）

`disposeOrReuseStartingFromIndex`（:876）
  → 由 `subcompose` 测量结束后调用（:1063）
  → 内部调 `slotReusePolicy.getSlotsToRetain(reusableSlotIdsSet)`
  → 若 policy 是 NoOp，集合被清空 → 所有 node 走 `composition?.dispose()` + `root.removeAt`
  → 复用池恒为 0 → 下次进入全部走 `createNodeAt` + INIT

### 待确认

- 列表项（slotId 20001~20010）实际用的是哪个 policy？
  页面根 `tab_N` 能 REUSE，说明 Pager 那层 policy 不是 NoOp；
  但列表项全 MISS，可能是内层 LazyColumn 用了不同 policy，或 `factory.getContentType` 不匹配。
- `LazyLayoutItemReusePolicy` 的 `MaxItemsToRetainForReuse = 7`，而列表首屏 10 项 ——
  即使走这个 policy，也有 3 项必然 dispose。这可能解释了部分 INIT。

---

## DIAGNOSE（第四轮：**根因确认**）

### 结论：列表项不是「复用失败」，而是「压根没进复用池，被整棵 dispose」

实测日志（切页 tab0→tab2）：

```text
areCompatible slot=tab_2 reusable=tab_3 typeA=null typeB=null result=true
disposeCurrentNodes disposed=10 (整棵子树 dispose，不进复用池) children=0
getSlotsToRetain before=1 after=1 max=7 types={null=1}
```

三条日志的确证：

1. `areCompatible` **只在页面层触发**（slot=tab_2 / reusable=tab_3），
   列表项（20001~20010）**从未进入** `areCompatible` —— 它们没参与复用判定
2. `disposeCurrentNodes disposed=10` —— 数字与「10 个列表项」精确对应，
   页面节点被 release 时**整棵子树一次性 dispose**
3. `getSlotsToRetain before=1 after=1` —— 复用策略只处理了 **1 个** slot（页面本身），
   列表项完全不在其中

### 根因链路

```
页面切走
  → LayoutNode.onRelease()  (LayoutNode.kt:1473)
  → subcompositionsState?.onRelease() → SubcomposeLayoutState.onRelease()  (:750)
  → disposeCurrentNodes()  (:923)
  → nodeToNodeState.values.forEach { it.composition?.dispose() } + root.removeAll()
  → 页面容器 + 其下 10 个列表项的 composition 全部销毁，复用池不参与

页面切回
  → 页面根 tab_N 命中 REUSE（28.9ms，容器复用成功）
  → 但 10 个列表项已不存在 → 全部 createNodeAt + INIT（各 6~9.5ms，合计约 77ms）
```

即：**`disposeCurrentNodes` 是「全 dispose」而非「回收进池」，导致每次切回都要重建全部列表项**。

### 与既有文档的关联

`BugFix/TODO-pager-snapshot-perf.md` §1 的「Pager 滑动快速路径被 measure 订阅打穿」是
**另一个**独立问题（measure 自订阅导致每帧全量 measure）。本次根因是
**页面 release 时子树全 dispose**，两者不同，都会造成卡顿，需分别处理。

### 为什么 `reusableCount` 恒为 0

`disposeCurrentNodes` 末尾显式清零（:930-934）：
`nodeToNodeState.clear()` / `slotIdToNode.clear()` / `precomposedCount = 0` /
`reusableCount = 0` / `precomposeMap.clear()`。所以页面被 release 后，
复用池被强制清空，下次进入必然全部新建。

### 修复方向（待评估，未实施）

方向 A：页面 release 时改为「回收进复用池」而非 dispose（即走
`disposeOrReuseStartingFromIndex` 而非 `disposeCurrentNodes`），让列表项可被复用。
- 风险：`onRelease` 是节点彻底释放的语义，改成回收可能违反生命周期契约，需确认上层预期

### ❌ 方向 A 已否决 —— `onRelease` 契约确认，不能改

三份独立证据一致，全 dispose 是**官方设计**，不是 bug：

1. **官方源码逐行一致**：`official-ui-1.10.1-SubcomposeLayout.kt:579`
   ```kotlin
   override fun onRelease() { disposeCurrentNodes() }
   ```
   官方同样是整棵 dispose，Kuikly 无差异。

2. **官方 runtime 明确区分两种命运**（`official-runtime-1.7.6-Composition.kt:1350`）：
   ```kotlin
   if (releasing != null && instance in releasing) instance.onRelease()
   else                                            instance.onDeactivate()
   ```
   `onRelease` = 永久销毁，`onDeactivate` = 暂不激活（可复活）。二者互斥，
   由 runtime 的 `releasing` 集合判定。Pager 页面切走走的是 `onRelease`，
   即 runtime 已判定该节点「永久销毁」。

3. **注释佐证**：
   - `KNode.kt:102` 上方注释：`Node is permanently destroyed — perform full view cleanup`
   - `ViewContainer.kt:250`：`removeDomSubViewForMove` 只移除 flex 节点、保留原生 view，
     「Final render view cleanup happens in KNode.onRelease()」

**改动的后果**：会破坏 movableContent 语义。`MovableContentDemo.kt:831` 明确记录
「完全不在任何 slot → Runtime 调 onRelease 销毁 → 重新出现时状态重置」，
这是**正确行为**。若把 onRelease 改成回收，隐藏再显示的状态重置语义会失效，
且原生 render view 得不到清理（泄漏）。

→ 方向 A 关闭。列表项重建是官方 runtime 语义的必然结果，框架层不能在此处改。

方向 B：加大 `beyondViewportPageCount` 让页面不被 release
- 已验证无效：只会加宽窗口，窗口外仍会 release；且修复后 beyond=0/1 帧数据无显著差异

方向 C：减少列表项首次组合成本（业务侧精简 item 内容）
- 治标，非框架层解法

**注意**：`forceRemeasure` 的 true/false 与本根因是**两个独立问题**。
改 false 能降低切页耗时（258ms→4ms），但列表项重建（约 77ms）依然存在。
且官方源码就是 true，改 false 属权衡而非修复。

---

## FIX_PLAN（草案，待确认）

暂缓。当前 `forceRemeasure=false` 的改动与官方不一致，建议先确认取舍：
- 若接受与官方不一致 → 保留 false，但需在 PR 说明这是权衡、并补旋转/分屏验证
- 若要求与官方一致 → 撤掉该改动，转而攻方向 A（列表项回收进复用池）

## 验收指标（后续修复的验证基准）

采集脚本：`/tmp/metrics.sh`（滑动 + 点击切页两段，均先 reset gfxinfo）
采集前提：插桩全部移除（插桩本身会污染耗时），`beyond=0`，预热全部 5 个 tab

### 基线（forceRemeasure=true，即官方一致状态 = 修复前）

| 场景 | 轮次 | Janky | P50 | P90 | P95 | P99 | Slow UI |
|---|---|---|---|---|---|---|---|
| 滑动（左右各 6 次） | 第1轮 | 13.21% | 10ms | 22ms | 48ms | 250ms | 37 |
| 滑动（左右各 6 次） | 第2轮 | 14.04% | 10ms | 24ms | 44ms | 200ms | 37 |
| 点击切页（跨窗口 0↔2 ×12） | 第1轮 | 8.67% | 10ms | 26ms | 40ms | 93ms | 15 |
| 点击切页（跨窗口 0↔2 ×12） | 第2轮 | 7.73% | 10ms | 23ms | 32ms | 93ms | 14 |

两轮波动：滑动 Janky 13.2~14.0%、P99 200~250ms；切页 Janky 7.7~8.7%、P99 93ms（稳定）。

### ⚠️ 本节作废：P99 / Janky% 不能用于判断卡顿

上面这套（P99 / Janky%）**导致过误判**，已作废，原因：

滑动时只有少数帧跨页，大部分帧很快（10ms），P99 取百分位会把几百 ms 的
极端帧稀释掉。实测出现过 **P99=23ms 但同时存在 550ms 帧** —— 用户明显感到卡，
统计指标却显示"已修复"。

更严重的是，本站曾用"已预热"场景测出最坏帧 65ms 就判定流畅，
掩盖了冷启动下 250~500ms 的真实卡顿。

---

## ✅ 正确的验收指标（现行）

**看最坏帧，不看 P99。** 脚本：`tools/repro/pager-frame-stats.sh`

```bash
zsh tools/repro/pager-frame-stats.sh --reset   # 清空
# 手机上操作（滑动 / 切页）
zsh tools/repro/pager-frame-stats.sh           # 分析
```

输出：最坏帧、慢帧分布、严重帧时间轴、卡顿事件聚类。

### 判定标准

| 最坏帧 | 判定 |
|---|---|
| < 33ms | 流畅（1 帧内） |
| 33~100ms | 轻微卡顿，敏感用户可感知（2~6 帧） |
| > 100ms | 明显卡顿，所有人都能感到 |

### 两个必须遵守的测量约束

1. **必须区分已预热 / 冷启动。** 同一操作冷启动 250ms、已预热仅 65ms。
   只测已预热会得到"已修复"的假象（本站栽过）。测卡顿先 `force-stop` 冷启动，
   或明确标注预热状态。
2. **不要用 `scrollToPage` 返回耗时当指标。** 它只测同步部分，
   异步组合在后续帧，实测 21ms vs 帧上 77ms，系统性乐观。以帧指标为准。

### 当前各场景实测（Debug，beyond=1）

| 场景 | 最坏帧 | 判定 |
|---|---|---|
| 已预热 + 滑动 | 65ms | 轻微 |
| 冷启动 + 切未访问页 | 77ms | 轻微 |
| 冷启动 + 大幅滑动 | 250~500ms | **明显卡（未解决）** |

采集脚本已入库：`tools/repro/pager-frame-stats.sh`（逐帧分析，现行）
另有 `tools/repro/pager-jank-metrics.sh`（滑动+切页两段统计，仅作粗筛，不作判定依据）

---

## 结论（最终）

### 已确认的根因

Pager 切页/滑动卡顿的直接成因：**页面节点跨页抢占 + 复用时走首次组合**。

1. **跨页抢占**：请求 `tab_2` 时池里没有 tab_2（`pool=[tab_4, tab_3]`），
   因 `LazyLayoutItemReusePolicy.areCompatible` 按 contentType 比较而 Pager 页面
   contentType 全为 null → 恒 true → 抢占 tab_3 的节点
2. **首次组合重建**：抢占来的节点走 `setContentWithReuse` → `composeInitial`，
   导致 `remember` 失效、内层 `SubcomposeLayoutState` 全新 → 列表项全部重建

结果：每次切页都在「丢弃别人的内容、重建自己的内容」，永远命中不了复用。
实测 10 个列表项各 6~9.5ms 全部 INIT。

叠加 `scrollToPage` 的 `forceRemeasure`（约 258ms）构成切页尖峰；滑动时跨页帧走
`scroll WITH remeasure`（约 241ms），其余帧 1~5ms，故表现为「一卡一卡」。

### 为什么框架层没有可做的优化

~~在「不缓存、不预加载、不改业务代码」的前提下，四个方向逐一排除~~
**此节已被推翻 —— 见下方「修复（已落地并验证）」。**
「列表项重建」是现象，不是根因；根因是复用策略与组合路径，可在框架层修复。

| 方向 | 结论 | 依据 |
|---|---|---|
| 改 `forceRemeasure` = false | 已按官方一致还原为 true | 官方 `PagerState.kt:566` 就是 true；改 false 是牺牲同步语义的权衡，非修复 |
| 改 `onRelease` 为回收复用 | ❌ 不可行 | 官方 `SubcomposeLayout.kt:579` 同为 `disposeCurrentNodes()`；runtime 用 `releasing` 集合区分 `onRelease`/`onDeactivate`，语义为「永久销毁」，改动会破坏 movableContent 状态重置并泄漏原生 view |
| 加大 `beyondViewportPageCount` | ❌ 无效 | 只加宽窗口，窗口外仍 release；实测修复后 beyond 0/1 帧数据无显著差异（9.32% vs 12.26%） |
| 精简 item 内容 | 治标，属业务侧 | 不在框架层 |

## 修复（已落地并验证）

### 改动（2 处，均在框架层）

1. `LazyLayout` 新增 `strictSlotReuse` 参数；`LazyLayoutItemReusePolicy` 严格模式下
   `areCompatible` 只按 slotId 精确匹配。`LazyLayoutPager` 传 `true`
2. `SubcomposeLayoutState.subcomposeInto`：composition 仍有效
   （`existing != null && !isDisposed`）时走增量 `setContent`，
   而非 `setContentWithReuse`（后者触发 `composeInitial` 重建整棵子树）

**不是保活、不是预加载**：`beyondViewportPageCount` 仍为 0，页面切走仍从视口移除，
只是让它复用自己那份 composition 而不是抢占别人的重建。

### 实测效果

Debug（beyond=0，真机 OCE-AN10）：

| 场景 | 指标 | 修复前 | 修复后 |
|---|---|---|---|
| 切页 | scrollToPage | 222~280ms | 30~48ms |
| 切页 | P99 / Janky | 93ms / 7.7~8.7% | 25ms / 5.39% |
| 滑动 | P99 / Janky | 200~250ms / 13~14% | 53~57ms / 9.5~10% |

Release（真实用户感知，2 轮稳定）：

| 场景 | 指标 | 修复前 | 修复后 |
|---|---|---|---|
| 切页 | scrollToPage | 81~118ms | 12~36ms |
| 滑动 | Janky | 6.64% | 4.10% / 3.85% |
| 滑动 | P95 / P99 | 29ms / 109ms | 16ms / 34ms、27ms |

### 回归验证（均已实际执行）

- 各 tab 内容正确渲染且互不相同（内容像素 11625 / 11649，hash 不同）
- 刷新（替换数据集）生效：日志 `feedVersion=1`，截图差异 13963 像素
  → 增量 `setContent` 正确处理数据变化
- 增删 tab（pageCount 5→4）无崩溃，切页正常
- 列表内滚动（LazyColumn）：Janky 7.42%、P99 25ms、无崩溃/异常
  → `subcomposeInto` 改动未破坏列表项复用

### 提交

`d7611e861 perf(pager): 修复切页时页面节点跨页抢占导致内容整棵重建`
（分支 `codex/pager-jank-force-remeasure`，已推送）

### 遗留风险

1. **旋转/分屏未验证**（设备无法真正旋转）
2. **iOS / 鸿蒙未实测** —— 改动在 `commonMain`，理论上影响所有平台，**须三端回归**
3. 早期曾用 `strictSlotReuse` 精确匹配复用，但实测 `areCompatible` 在 Pager 场景从不调用（beyond=1 与 =4 均为 0 次），已移除；当前不含该项改动，LazyColumn 复用策略未变

### 遗留未覆盖项

1. **旋转/分屏**：用 `wm`/`settings` 未能真正旋转该设备（`ROTATION_9` 异常），未验证
2. **滑动路径的插桩数据**：`adb shell input swipe` 未稳定触发跨页，滑动只有帧数据、
   没有像切页那样的 slotId 级证据
3. **三端一致性**：`disposeCurrentNodes` 在 commonMain，但只在 Android 实测，
   iOS/鸿蒙未验证
4. **MAL 阶段细分**：切页未走 `measureAndLayout(layoutNode, constraints)` 重载，
   「组合 / 布局 / 原生视图创建」的比例未拆开

### 另一个独立问题（未处理）

`BugFix/TODO-pager-snapshot-perf.md` §1 曾记录「Pager 滑动快速路径被 measure 订阅打穿」，
与本问题不同。该问题后续已**实测确认在当前代码上未发生**（详见该文档顶部状态与
「量化验证结果」），故不再作为独立遗留项。

---

## 2026-11-02 追加：后续两轮优化与根因修正

本文档上文的部分归因（触摸阻塞等待 Kuikly 线程 / provider 打翻 item scope）
已被后续实测**修正**，以本节为准：

1. **JVM 模式下 Kuikly 逻辑与 UI 同在主线程**，不存在跨线程等待；
   当时 trace 里的空白窗口是 Compose DSL 指针处理不打 trace 所致。
2. **滑动**：doLayout 逐次实测首次建新页 62.3ms、之后 9.8→2.5→1.1ms ——
   滑动本身流畅，卡顿仅在首次进入新页那一帧；成本为跨桥建视图 0.018ms/节点
   × 304 + 文本排版 0.947ms/次（StaticLayout.build 为主体）。
3. **点击切页**：`scrollToPage` 当帧 skipBeyondCompose 只建目标页，但 finally
   复位后紧邻下一帧立即以完整 beyond=1 再测量，相邻页组合成本压在切页后
   那一帧（约 62ms）。修复为走 deferBeyondRestoreOnIdle（静止 600ms 补建），
   点击切页最坏帧 37.4ms → 约 20ms（-46%），>33ms 帧清零，优于官方同场景
   （39.4~63.0ms）。提交 `88876f176`。
4. **R8**：本文档上文曾将 R8 列入"无收益"是误判 —— 当时 release 根本未开启
   `isMinifyEnabled`。本轮开启后首屏 747→666ms（-11%），配套新增
   `androidApp/proguard-rules.pro`（composeDSL 整包保留，否则部分页面白屏）。
   提交 `9bb6ff27a`。

### 修复项 ↔ 收益对照表（从多少 → 多少）

**读表说明：**

- **主指标**以文档「✅ 正确的验收指标」为准：**最坏帧**优先；辅指标含 `scrollToPage` 同步耗时、Janky/P99（粗筛，易稀释尖峰）。
- **设备 / 包**：未单独注明时为 **OCE-AN10**、`PagerJankRepro`；fix6 栈 release 复测见 **`0e27e2102`**（`logs/pager_e2e_rerun_20261102.log`）。
- **「未单拆 A/B」**：与其它改动同栈发布，没有单独 revert 对比；**修复后**列为 fix6 正确协议下的实测，**不能**当作该项单独贡献的精确 delta。

| # | 修复项 | 提交 / 代码 | 解决的问题 | 主指标（修复前 → 修复后） | 测量条件 | 证据层级 |
|---|--------|-------------|------------|---------------------------|----------|----------|
| 0 | （问题基线） | — | 跨页抢占 + 整树重建 | 切页 `scrollToPage` **222~280ms**；滑动 Janky **~60%**（早期 gfxinfo） | Debug，beyond=0，forceRemeasure=true | 文档 §DIAGNOSE 基线 |
| 1 | **`subcomposeInto` 有效 composition 走增量 `setContent`** | `d7611e861` | 切页抢占后 `composeInitial` 重建列表 | Debug 切页 `scrollToPage` **222~280 → 30~48ms**；滑动 P99/Janky **200~250ms / 13~14% → 53~57ms / ~10%** | beyond=0，真机 | **集成**（§修复 实测表） |
| 1b | 同上 | 同上 | Release 用户感知 | 切页 `scrollToPage` **81~118 → 12~36ms**；滑动 Janky **6.64% → ~4%**；P95/P99 **29/109 → 16/34ms** | Release，2 轮 | **集成** |
| 2 | **`deferBeyondRestoreOnIdle` + 切页当帧 `skipBeyondCompose`** | `88876f176` / `0d9f8a8ed` | 切页下一帧立刻 full beyond=1，~**62ms** 压在切页后一帧 | 点击切页**最坏帧** **37.4 → ~20ms**（**-46%**）；**>33ms 帧清零**；优于官方同场景 **39.4~63ms** | Debug，beyond=1，Perfetto/帧统计 | **集成**（§2026-11-02 §3） |
| 3 | **滑动热路径：滚动只刷新时间戳，不每帧 cancel/launch 协程** | `0d9f8a8ed` | MOVE 高频 Job 分配 | **未单拆帧 delta**；属 fix6 栈一部分 | 插桩 + Perfetto 定性 | **静态 / 定性** |
| 4 | **defer 期间保留已组合 adjacent 页（`isComposedLastMeasure`）** | `0e27e2102` | defer 时 beyond=0 把已组合邻页移出组合 | **单拆 A/B**（`pager-e2e-protocol.sh`，OCE-AN10 release）：预热滑动最坏 **150→73ms**，≥100ms **2→0**；Janky **5.69%→3.32%**，P99 **81→23ms**；Tab 最坏 **77→69ms**；冷启动 Perfetto **71.9→66.7ms**（均无 ≥100ms） | 2026-11-02；WITH=`0e27e2102`；WITHOUT=关 retain 本地 APK | **真实 E2E A/B**（`logs/pager_ab4_*`） |
| 5 | **`requestScrollToPage` 与 `scrollToPage` 同 defer 路径** | `0212a78dd` | API 行为不一致 | **未单独量化**；与 #2 同机制 | 代码审查 | **静态** |
| 6 | **`ViewContainer.indexOfRenderChild` / `collectRenderChildren`** | `0d9f8a8ed` | 插入 render 时重复 `renderChildren()` 分配 | **无帧时间 A/B**；粗算单页建树少 **~数百 KB 短生命周期堆**（N≈300） | 静态估算 | **未实测 Allocation** |
| 7 | **androidApp Release R8 + proguard** | `9bb6ff27a` | release 未开 minify | **首屏 747 → 666ms**（**-11%**） | Release 启动 A/B | **集成**（非 Pager 内滑动） |
| 8 | **OHOS JS 脚本缓存 key（路径+内容哈希）+ stale 清理** | `efb52dd0c`（**仅 fix6 发版线**，本分支无代码） | 同名/同 mtime 误命中旧 JS | **非 Pager**；fix6 审阅项 | fix6 分支 | **L0 / 静态** |
| 9 | **滚动静默检测 `DateTime.nanoTime()`（1.8.21）** | `0e27e2102` | `TimeSource.Monotonic` 需 ExperimentalTime | **无运行时收益**；**1.8.21 `:compose:compileReleaseKotlinAndroid` PASS** | CI 同款 sed + 本地编译 | **编译门禁** |
| 10 | **`ViewContainer.forEachRenderChild` → private** | `0e27e2102` | 对外 API 面 | **无性能数字** | — | **API** |

**fix6 栈 release 端到端（#1~#6 等已合入，`0e27e2102`，正确 gfxinfo 协议 + Perfetto）：**

| 场景 | 指标 | 相对「问题基线 #0」 | 本轮实测 |
|------|------|---------------------|----------|
| 预热后滑动 | **最坏帧** | 跨页 remeasure **~241ms** 级 → **73ms** | §真机 E2E 表 |
| 预热后 Tab 切页 | **最坏帧** | 切页后一帧 **~62ms** 级 → **77ms**（含 Tab 动画路径） | 同上 |
| 冷启动 + 2 次 swipe | **最坏帧**（Perfetto） | 冷启动大幅滑 **250~500ms** 档仍可能存在（未在本轮脚本覆盖） | **~68ms**，≥100ms=0 |
| 预热滑动（粗筛） | Janky / P99 | 早期 **~60%** → 本轮 **~6% / 57ms** | `pager-jank-metrics` |

**仍明显未解决 / 未在本表承诺优化：**

- **冷启动 + 大幅滑动**首次进新页：历史 Debug 仍记录 **250~500ms** 最坏档（§当前各场景实测）；fix6 冷启动脚本只滑 2 次，**不能**代表大幅冷滑已消失。
- **旋转 / 分屏、iOS / 鸿蒙 Pager 帧**：未测。

#### A/B #4 保留已组合页（2026-11-02，单变量）

方法：仅改 `PagerMeasurePolicy` 中 `retainedPageWindow` / `isRetainedPage`（WITHOUT = 全 0/null，等同 defer 但不保留）；其余 fix6 栈不变。脚本：`zsh tools/repro/pager-e2e-protocol.sh`。

| 场景 | WITHOUT retain | WITH retain（合入） | Δ |
|------|----------------|---------------------|---|
| 预热滑动最坏帧 | **150ms**（≥100ms **2** 帧） | **73ms**（**0**） | 滑动尖峰与严重帧下降 |
| 预热滑动 Janky / P99 | **5.69% / 81ms** | **3.32% / 23ms** | 粗筛一致向好 |
| Tab 切页最坏帧 | 77ms | **69ms** | 小幅 |
| 冷启动 2 swipe（Perfetto） | 71.9ms | **66.7ms** | 小幅 |

日志：`logs/pager_ab4_WITHOUT_RETAIN.log`、`logs/pager_ab4_WITH_RETAIN_v2.log`。

### 上文"遗留未覆盖项"的更新

- 「滑动路径的插桩数据」：已补齐（doLayout 逐次耗时、约束序列、miss 原因分布）
- 「MAL 阶段细分」：已拆分 —— 文本排版为主体（0.947ms/次），
  建视图 0.018ms/节点，构造 spans 0.044ms/次
- 「旋转/分屏」「iOS/鸿蒙未实测」：仍未验证，维持为遗留项

### 验证工具（随分支交付）

`tools/repro/` 下：`pager-cold-frame-check.sh`（冷启动滑动，**Perfetto**，优先于 gfxinfo）、
`pager-frame-stats.sh`（直方图最坏帧；**须预热后 reset 再测**，勿 reset 在进页前）、
`pager-jank-metrics.sh`（滑动/切页两段，内置正确 reset 时机）、
`pager-vs-official.sh`（切页 A/B 官方对照）、`pager-flat-smoke.sh`
（跨页面冒烟，改布局结构类改动必跑）、`pager-vs-official-launch.sh`（首屏 A/B）。

### CodeReview / Goal 跟进（2026-11-02）

**P2：** `requestScrollToPage` 已与 `scrollToPage` 对齐（`skipBeyondCompose` + finally `deferBeyondUntilIdle`）。

**CodeReview 跟进（审阅修复，2026-11-02）：**

- **defer beyond 语义**：滑动/切页关键帧仍把 `resolveBeyondCount` 压到 0，但**不会**把上一帧已在窗口内组合好的相邻页移出组合；只有**新进入**测量窗口的 beyond 页推迟到静止 600ms 后补建（`PagerMeasure` + `isComposedLastMeasure`）。相邻页上的 `remember` / `LaunchedEffect` 在来回滑时不再因 defer 被反复 dispose。
- **仍存在的权衡**：首次进入从未组合过的 beyond 页，组合成本仍会落在拖动帧或静止后的补建帧上；`beyondViewportPageCount>0` 时全局生效，无单独开关。
- **Kotlin 1.8.21**：滚动静默检测改用 `DateTime.nanoTime()`，不再依赖 `kotlin.time.TimeSource`（避免 1.8.21 `@ExperimentalTime` 编译失败）。本地已跑 `KUIKLY_KOTLIN_VERSION=1.8.21` + CI 同款 Volatile sed → `:compose:compileReleaseKotlinAndroid` **BUILD SUCCESSFUL**（`logs/compile_1821_android.log`）。
- **OHOS JS 缓存**（**不在 `codex/pager-jank-squash`**）：fix6 上见 `KTJsScriptCache.h` / `efb52dd0c`。
- **core**：`ViewContainer.forEachRenderChild` 改为 `private`，对外 API 不扩大。
- **`indexOfRenderChild`（core）**：语义与 `renderChildren().indexOf(target)` 相同（展开顺序、引用匹配、未找到 -1），**不改变布局/复用行为**；在 `insertSubRenderView` **单节点插入**热路径上避免每次分配整份拍平列表（建树阶段累计约 **O(N²) 量级的短生命周期引用槽**，N≈300 页量级粗算 **~数百 KB 临时堆**，**非常驻**；未做 Allocation 实测）。**不是** Pager 卡顿的主因修复，只是配合首次进页建原生视图链路的微优化。多节点同批插入仍 `collectRenderChildren()` 快照一次，避免插入过程中下标漂移。

### 真机 E2E 与 gfxinfo 采数（2026-11-02，OCE-AN10，release，`0e27e2102`）

**包名**：`com.tencent.kuikly.android.demo`（`pager-*` 脚本默认）。完整日志：`logs/pager_e2e_rerun_20261102.log`。

**关键结论（避免误判）：**

1. **`pager-frame-stats.sh` 必须「预热 → `dumpsys gfxinfo reset` → 只做目标动作」**，与 `pager-jank-metrics.sh` 第 14–16 行一致。若 **reset 写在 force-stop / 进页之前**，直方图会把 **进页 + Tab 预热** 算进「滑动」，出现 **550ms / 700ms 虚假尖峰** —— **不是 fix6 或 review 改动引入的回归**。
2. **正确协议下**（见日志 §2–§4）：预热后 **仅滑动** 最坏 **73ms**、**仅 Tab 切页** 最坏 **77ms**、**≥100ms 均为 0**；`pager-jank-metrics` 同会话滑动 P99 **57ms**、Janky **~6%**，与直方图一致。
3. **冷启动滑动**用 **`pager-cold-frame-check.sh`（Perfetto doFrame）**，文档注明 gfxinfo 在部分 Kuikly 自绘场景不可靠；本轮 **最坏 ~68ms**，**≥100ms = 0**。
4. **错误对照**（日志 §5，故意 reset 在进页前）：最坏 **550ms** —— 仅用于证明采数顺序问题，**不能当产品指标**。

| 场景 | 协议 | 最坏帧 | ≥100ms | 依据 |
|------|------|--------|--------|------|
| 滑动 6 轮 | 预热后 reset | **73ms** | 0 | `pager-frame-stats` + jank-metrics A |
| Tab 0↔2 ×12 | 预热后 reset | **77ms** | 0 | `pager-frame-stats` + jank-metrics B |
| 冷启动 + 2 swipe | Perfetto | **~68ms** | 0 | `pager-cold-frame-check.sh` |
| 滑动（错误 reset 顺序） | reset 在进页前 | 550ms（虚假） | 1 | 日志 §5，勿采用 |

**Android repro（release + `com.tencent.kuikly.android.demo`，真机 OCE-AN10）：**

| 脚本 | 结果 |
|------|------|
| `pager-flat-smoke.sh` | ✅ |
| `pager-regression-visual.sh` | ✅（需 `pageData {"autoRegression":"1"}`，`PagerJankRepro` 已接） |
| `pager-reuse-visual.sh` | ✅ PASS |

**iOS：** `:core` / `:compose` `compileKotlinIosSimulatorArm64` ✅（静态，未跑 iosApp 打开 `PagerJankRepro`）。

**鸿蒙：** `:demo:linkSharedDebugSharedOhosArm64` ❌。分支 HEAD 报 `imakuikly.shared/Chip`；本机 demo 包名/visibility 修复后链接阶段 `NullPointerException`（`logs/kuikly_ohos_libshared16.log`）。`hdc list targets` 仅 `127.0.0.1:5555`，HAP 未装。**建议与 pager 核心改动分 MR**，单独收 OHOS demo KN 链接。

**工作区 demo 改动：** 主要为鸿蒙链接试修（`ModifierKuikly` → `pages.modifier`、`imakuikly`/`composedemo` 包名修正等），与 pager 验收正交；Android 已 `assembleRelease` 验证通过。

