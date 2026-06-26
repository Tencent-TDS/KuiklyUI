# ComposeAllSample 鸿蒙滑动白屏优化总结

> 场景：鸿蒙设备上进入 `ComposeAllSample`（Demo案例-Compose语法），快速/慢速滑动 LazyColumn 时出现视口内大块空白（白屏）。
>
> 状态：**框架层优化已验收**；`ComposeAllSample.kt` **保持 main 原版未改**，流畅度仍明显改善（说明收益主要来自框架，而非 Demo 页减负）。

---

## 1. 问题本质

Kuikly Compose 的 `LazyColumn` **不是** ArkUI 原生 List，而是 **双引擎滚动**：

```
ArkUI ScrollerView 位移
  → Native onScroll 桥接到 Kotlin
  → SubcomposeLayout 同步 composeOffset
  → LazyListState.kuiklyOnScroll → remeasure / subcompose 新 item
  → 创建/更新原生 DivView + Text（KRRichTextView 绘制）
```

白屏不是 crash，而是 **滚动链路过长 + 回调过频**，新 item 来不及绘制，视口短暂露出背景色 `#F5F5F5`。

`KuiklyScrollTrace` 日志证实：瓶颈在 **每次 onScroll 都触发 calcSize / expand / 桥接**（一次 fling 可达 300+ 次 Kuikly 业务回调），而非 hilog 打印本身。

---

## 2. 修改项生效度排名

按 **对流畅度 / 白屏的实测贡献** 排序（★★★★★ 最高）。排名依据：`KuiklyScrollTrace` 前后对比 + 恢复 `ComposeAllSample.kt` 后仍可流畅的交叉验证。

| 排名 | 生效度 | 修改项 | 文件 | 日志 / 现象依据 |
|:---:|--------|--------|------|-----------------|
| **#1** | ★★★★★ | **expand 空转去除**：`tryExpandStartSize` 仅在双端 offset 真正不同步时执行 | `ContentSizeExtensions.kt` | `expand` 总量 **944 → 0**；此前 `kuiklyScroll=0` 时仍 expand 36 次/手势 |
| **#2** | ★★★★★ | **calc/expand 与 `kuiklyOnScroll` 绑定**：只有 LazyList 真实滚动后才 calc + expand | `SubcomposeLayout.kt` | 快滑 `calcSize` **170 → 38**（`kuiklyScroll=14~37`） |
| **#3** | ★★★★☆ | **contentSize 去重**：`lastAppliedContentSize` 避免重复 `setFrame` | `KuiklyScrollInfo.kt` | `dedup` 数百次 vs `setFrame` 个位数；抑制原生 contentView relayout 风暴 |
| **#4** | ★★★★☆ | **RichText 排版就绪时继续绘制**：主线程任务中 typography 已就绪则不 Skip | `KRRichTextView.cpp` | `OnForegroundDraw Skip` **恒为 0**；直接消除新 item 文字白块 |
| **#5** | ★★★★☆ | **滚动中 calc 节流**：`calculateAndUpdateContentSizeIfNeeded` 仅近底 / 未知真实高度时更新 | `ContentSizeExtensions.kt` | 长滑中间段不再每帧读 frame；`calc/scroll` **1.57x → 1.13x** |
| **#6** | ★★★☆☆ | **Fling 态 Native 量化 2vp**：快滑加大位移阈值 | `KRScrollerView.cpp` | `fireToBridge` **173 → 90**（同场景快滑）；`fireSkipped` 提升至 ~23–35% |
| **#7** | ★★★☆☆ | **Compose sub-pixel 过滤**：`< 0.5px` 位移累积，不驱动 remeasure | `SubcomposeLayout.kt` | 与 LazyListState 对齐；挡鸿蒙高频小数 onScroll |
| **#8** | ★★★☆☆ | **触底边界 defer**：`pendingBottomExpand` 标记，scrollEnd 统一扩容 | `SubcomposeLayout.kt` + `KuiklyScrollInfo.kt` | 消除 `toButtomDelta<=0` 每帧 calc（earlyRet 手势中 calc 虚高主因） |
| **#9** | ★★★☆☆ | **scrollEnd 统一收尾**：`finalizeNativeScrollSync` 一次 calc + offset 校正 | `SubcomposeLayout.kt` + `ContentSizeExtensions.kt` | 保证手势结束双端 offset / contentSize 最终一致 |
| **#10** | ★★☆☆☆ | **慢拖 Native 量化 0.5vp** + scrollStop `force` flush | `KRScrollerView.cpp` | 慢滑仍 ~1:1 桥接，但消除 sub-pixel 噪声；stop 时补齐尾差 |
| **#11** | ★★☆☆☆ | **语义树 debounce + 无障碍关闭时跳过** | `RootNodeOwner.kt` | 滚动中少遍历语义树；Demo 默认 `debugUIInspector=true` 时收益有限 |
| **#12** | ★☆☆☆☆ | **OHOS expand delay 缩短**（25→16ms，settle 150→80ms） | `ContentSizeExtensions.kt` | 停手后空白窗口略缩短；难单独量化 |
| — | （诊断） | `KuiklyScrollTrace` 分层计数 | `KuiklyScrollTrace.kt` | 非性能优化；`ENABLED=false` 默认关闭 |
| — | （未采用） | **ComposeAllSample 页面减负** | `ComposeAllSample.kt` | 见 §3；**未合入**，恢复 main 后仍流畅 |

### 生效度分级说明

| 等级 | 含义 |
|------|------|
| ★★★★★ | 日志有数量级变化，或直接导致白块消失；**必须合入** |
| ★★★★☆ | 显著减少重操作 / 原生 relayout；**强烈建议合入** |
| ★★★☆☆ | 明显减少回调或边界 case 浪费；**建议合入** |
| ★★☆☆☆ | 有收益但难单独量化，或仅特定场景；**可合入** |
| ★☆☆☆☆ | 边际优化；**可选** |
| 未采用 | 业务页可选实践，**非框架必需**（本次验证已排除） |

---

## 3. 已合入修改详情（按排名）

### #1–#2 滚动同步：「只在真正滚动时做重活」

**优化前**：每次 Native `onScroll`（~60fps）都执行 `calculateAndUpdateContentSize` + `tryExpandStartSize` + 可能 `kuiklyOnScroll`。

**优化后**：

```
Native onScroll
  ├─ [L1] 位移量化（0.5vp / fling 2vp）           → 减桥接
  ├─ [L2] Compose sub-pixel 过滤（< 0.5px）       → 减 remeasure
  ├─ [L3] earlyReturn（顶边界 / ignoreOffset）     → 不驱动 LazyList
  └─ [L4] kuiklyOnScroll 成功后
         ├─ calculateAndUpdateContentSizeIfNeeded()
         └─ tryExpandStartSize()（#1 条件守卫）
scrollEnd → finalizeNativeScrollSync()            → 一次收尾
```

```kotlin
// SubcomposeLayout.kt — 仅真实滚动后同步
scrollableState.kuiklyOnScroll(scrollDelta.toFloat())
scrollableState.calculateAndUpdateContentSizeIfNeeded()
scrollableState.tryExpandStartSize(offset, true)
```

```kotlin
// ContentSizeExtensions.kt — expand 空转去除（#1）
val needsTopExpand = offset <= 0 && !atTopSync && kuiklyInfo.offsetDirty
val needsScrollViewPullBack = offset > 0 && atTopSync
if (!needsTopExpand && !needsScrollViewPullBack) return
```

---

### #3 contentSize 去重（`KuiklyScrollInfo.kt`）

```kotlin
private var lastAppliedContentSize: Int = -1

fun updateContentSizeToRender() {
    if (currentContentSize == lastAppliedContentSize) return
    lastAppliedContentSize = currentContentSize
    scrollView?.contentView?.setFrameToRenderView(createContentFrame())
}
```

---

### #4 RichText 绘制（`KRRichTextView.cpp`）

```cpp
if (rootView->IsPerformMainTasking()) {
    if (richTextShadow == nullptr || richTextShadow->MainThreadTypographyHandle() == nullptr) {
        // 排版未就绪 → 下一帧 markDirty
        return;
    }
    // typography 就绪 → 继续绘制，不 Skip
}
```

---

### #5–#9 calc 节流与 scrollEnd 收尾（`ContentSizeExtensions.kt`）

```kotlin
internal fun ScrollableState.calculateAndUpdateContentSizeIfNeeded(force: Boolean = false) {
    if (force || kuiklyInfo.nearScrollBottom() || kuiklyInfo.realContentSize == null) {
        calculateAndUpdateContentSize()
    }
}

internal fun ScrollableState.finalizeNativeScrollSync(offset: Int) {
    calculateAndUpdateContentSize()
    if (kuiklyInfo.pendingBottomExpand) {
        kuiklyInfo.pendingBottomExpand = false
    }
    tryExpandStartSize(offset, isScrolling = false)
}
```

```kotlin
// SubcomposeLayout.kt — 触底 defer（#8）
if (toButtomDelta.toInt() <= 0) {
    kuiklyInfo.pendingBottomExpand = true
    return@scroll
}
```

---

### #6–#10 Native 滚动量化（`KRScrollerView.cpp`）

```cpp
constexpr float kMinScrollOffsetDelta = 0.5f;
constexpr float kFlingScrollOffsetDelta = 2.0f;
const float minDelta = (current_scroll_state_ == ARKUI_SCROLL_STATE_FLING)
                           ? kFlingScrollOffsetDelta
                           : kMinScrollOffsetDelta;
```

`OnScrollStop` 时 `FireOnScrollEvent(event, true)` 强制 flush 最终 offset。

---

### #11 语义树（`RootNodeOwner.kt`）

```kotlin
override fun onSemanticsChange() {
    if (!isSemanticsRunnnng) return
    semanticsDebounceJob?.cancel()
    semanticsDebounceJob = semanticsCoroutineScope.launch {
        delay(100)
        semanticsKuiklyHandler.onSemanticsChange(semanticsOwner)
    }
}
```

---

## 4. 未采用的页面层优化（可选参考）

以下改动曾验证有效，但 **`ComposeAllSample.kt` 已恢复 `main` 原版**，未纳入最终合入范围。业务列表可参考，**非白屏根因修复**。

| 项 | 改法 | 预估生效度 | 说明 |
|----|------|-----------|------|
| 关 UI Inspector | `debugUIInspector()` 默认 `false` | ★★☆☆☆ | 减调试 overlay；Demo 当前仍为 `true` |
| stable key | `items(..., key = { it.pageName })` | ★★☆☆☆ | 减 slot 重建 |
| 去 Card shadow | `Row + background` 替代 `Card` | ★★☆☆☆ | 减离屏阴影 |
| 固定 item 高度 | `.height(72.dp)` | ★★★☆☆ | 提升 `noRemeasure` 比例；对慢滑白屏有帮助 |

---

## 5. 日志验收数据

诊断：`KuiklyScrollTrace`（`ENABLED=true`，`hilog | grep KuiklyScrollTrace`）

### 5.1 框架优化前后（ComposeAllSample 有页面改动时期）

| 指标 | 优化前（18 次手势） | 优化后（15 次手势） |
|------|---------------------|---------------------|
| expand 总量 | **944** | **0** |
| calc / kuiklyScroll | 1.57x | 1.13x |

### 5.2 典型快速 fling

| 指标 | 优化前 | 框架优化后 |
|------|--------|------------|
| fireToBridge | 173 | 90 |
| kuiklyScroll | 14 | 37 |
| calcSize | **170** | **38** |
| expand | **170** | **0** |

### 5.3 恒成立项

- `OnForegroundDraw Skip`：**0**
- `setFrame` 极少，`dedup` 占绝大多数
- 恢复 `ComposeAllSample.kt` 后：**仍流畅** → 排名 #1–#11 框架改动可独立生效

---

## 6. 已合入文件清单

```
compose/.../SubcomposeLayout.kt                       # #2 #7 #8 #9
compose/.../ContentSizeExtensions.kt                  # #1 #5 #9 #12
compose/.../KuiklyScrollInfo.kt                       # #3 #8
compose/.../RootNodeOwner.kt                          # #11
compose/.../KuiklyScrollTrace.kt                      # 诊断（默认关）
core-render-ohos/.../KRScrollerView.cpp/.h            # #6 #10
core-render-ohos/.../KRRichTextView.cpp               # #4
```

**未修改**：`demo/.../ComposeAllSample.kt`（保持 `main`）

---

## 7. 可复用经验

1. **先查「同步频率」再查「单帧绘制」**：双引擎列表的白屏多为回调风暴，不是 GPU 慢。
2. **用分层计数定位空转**：`fireToBridge` / `kuiklyScroll` / `calc` / `expand` / `remeasure` 分开统计。
3. **去重 > 节流 > 延后**：`lastAppliedContentSize`（#3）成本低收益高；calc 绑定滚动（#2）次之；scrollEnd 收尾（#9）保底一致性。
4. **框架优化可独立于业务页**：本次恢复 Demo 原版后仍流畅，说明 #1–#11 是通用收益。
5. **业务页优化（§4）是锦上添花**：固定高度、stable key 对慢滑 remeasure 仍有价值，但不替代框架改动。

---

## 8. 后续可选

| 优先级 | 方向 | 关联排名 |
|--------|------|----------|
| 中 | 慢滑 remeasure 根因（item 高度稳定性） | 对标 §4 固定高度 |
| 中 | 语义同步全局开关（列表页默认关） | #11 增强 |
| 低 | iOS / Android 对齐 fling 2vp 策略 | #6 跨端 |
| 低 | MR 拆分：仅框架层一个 PR | — |

---

## 9. 复现与验证

**进入 ComposeAllSample（鸿蒙）**：

1. 冷启动 App → 「Kuikly页面路由」
2. 点击 **「Demo案例-Compose语法」**（须 `router.pushUrl`，勿用 `aa start --ps pageName` 冷启动）

**验收**：S1 快速 fling ×3、S2 匀速滑、S3 边界来回、S4 静止后 fling；视口无大块空白，`OnForegroundDraw Skip = 0`。
