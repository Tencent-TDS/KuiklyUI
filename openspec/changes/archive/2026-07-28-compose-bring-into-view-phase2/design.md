## Context

### 背景与当前状态

本次改动 **只适用于 Compose DSL**。`compose-ime-insets` 已经解决了 phase1 的页面级 IME 状态与 `imePadding()` 问题，但 phase2 仍然缺失：**当输入框获焦且被键盘挡住时，框架还不会自动把目标滚到可视区。**

当前仓库已经具备几块关键基座：

- `compose/foundation/FocusedBounds.kt`
  - 已有 `Modifier.onFocusedBoundsChanged(...)`
  - `FocusedBoundsNode` 会把当前 focused child 的 `LayoutCoordinates` 向父层冒泡
- `compose/foundation/Focusable.kt`
  - `FocusableNode` 已接入焦点事件、`FocusedBoundsNode` 与 pinnable container
  - 源码中已经明确写了 `scrollIntoView()` 的 TODO 和 3 类需要保持可见的场景
- `compose/ui/layout/LayoutCoordinates.kt`
  - 已提供 `boundsInWindow()`、`boundsInRoot()`、`localBoundingBoxOf(...)`、`positionInWindow()` 等坐标能力
- `compose/foundation/gestures/ScrollExtensions.kt`
  - `ScrollableState.animateScrollBy(...)` 已可做像素级平滑滚动
- `compose/foundation/Scroll.kt`
  - `ScrollState` 本身就是 `ScrollableState`
- `compose/foundation/lazy/LazyListState.kt`
  - `LazyListState` 也是 `ScrollableState`，且具备 `layoutInfo`、`animateScrollToItem(...)` 等能力
- `compose/ui/layout/PinnableContainer.kt` 与 `lazy/layout/LazyLayoutPinnableItem.kt`
  - 已有 focused item pinning 基础，可降低 lazy 场景下滚动过程的回收问题
- `compose/platform/LocalConfiguration.kt`
  - phase1 已把 `imeBottomDp` 放入页面级状态，`WindowInsets.ime` 可直接复用

因此，phase2 的核心问题已经不是"有没有底层能力"，而是：

1. 还没有统一的 `BringIntoViewRequester` 公共 API
2. 还没有 scroll 容器的 responder / request 链路
3. 还没有"何时判定被挡住、滚多少、何时停止"的统一策略

### 适用 DSL

- **适用**：Compose DSL（`compose/` 模块与 `demo/`）
- **不适用**：自研 DSL（`core/` 下 `Pager` / `body()` 体系）

### 与跨端链路的关系

phase2 **不新增 renderer / NativeBridge 协议**。它直接复用 phase1 已建成的页面级 `WindowInsets.ime` 状态，把新能力限制在 `compose/` 内闭环：

```text
IME height (phase1 已有)
    ↓
WindowInsets.ime
    ↓
focused target rect + viewport rect + ime inset
    ↓
BringIntoView request
    ↓
ScrollState / LazyListState 响应滚动
```

这意味着首版不需要改 `core-render-android/`、`core-render-ios/`、`core-render-ohos/`，也不需要再扩 page event。

## Goals / Non-Goals

**Goals:**

- 为 Compose DSL 新增与官方 Compose 分层一致的 `BringIntoViewRequester` 能力。
- 让输入组件在获焦且被 IME 遮挡时，自动请求进入可视区。
- 为 `ScrollState` / `verticalScroll` 与 `LazyListState` / `LazyColumn` 建立首版 responder 能力。
- 建立基于 `FocusedBounds`、`LayoutCoordinates`、`WindowInsets.ime` 的可视区判定闭环。
- 在焦点变化、IME 变化、viewport 变化时自动重算，而不是要求业务手动监听。
- 先交付一个 **纯 Compose、纵向容器优先、输入场景优先** 的 MVP。

**Non-Goals:**

- 不实现 `imeNestedScroll`、键盘跟手收起、逐帧 IME animation 或动画曲线精修。
- 不引入 Native `Scroller` bridge 作为首版前置条件。
- 不覆盖横向容器、Grid、Pager、StaggeredGrid、多层嵌套滚动的全量场景。
- 不修改自研 DSL；KuiklyDSL 后续在 Compose DSL 能力稳定后再跟进。
- 不追求 caret 级别的精准可见性；首版只保证 **输入组件整体进入可视区**。
- 不承诺程序化聚焦到"尚未 compose 的离屏 lazy item"也能一步到位滚入视口。

## Decisions

### D1：首版做"纯 Compose MVP"，不把 Native `Scroller` bridge 作为前置条件

**选择**：phase2 首版只在 `compose/` 内建设请求、判定、响应三层闭环，优先支持 `ScrollState` 与 `LazyListState` 两类纵向容器。

**原因**：

- `WindowInsets.ime`、焦点几何、坐标转换、像素级滚动都已经具备。
- 当前主要缺口在 Compose 内部的 request / responder / policy，而不是平台桥接。
- 先把 80% 常见输入页场景闭环，能最快补齐 Compose DSL 能力面。

**替代方案对比**：

- **方案 A：一开始就做 Native `Scroller` bridge**
  - 否。范围过大，难以同时收敛 API 设计、容器协议和跨端行为；对首版价值不成比例。
- **方案 B：先做纯 Compose MVP（采用）**
  - 是。边界清晰、依赖更少、能快速验证 request / responder 机制是否合理。

### D2：自动行为默认挂在"输入组件"上，而不是所有 `focusable`

**选择**：首版自动 bring-into-view 只默认作用于会触发 IME 的输入组件（如 `CoreTextField` / `TextField` / `BasicTextField` 所在链路）；同时对外暴露通用 `BringIntoViewRequester`，供业务和其他组件手动使用。

**原因**：

- phase2 的目标是解决"输入框被键盘挡住看不到"，不是让所有获得焦点的节点都自动滚动。
- 如果把默认行为挂到所有 `focusable`，按钮、开关、可点击卡片在外接键盘或无键盘场景下也可能触发误滚。
- 公共 API 保留通用性，默认行为保持克制，和官方 Compose 的职责拆分更一致。

**替代方案对比**：

- **方案 A：所有 `focusable` 默认自动滚动**
  - 否。误触发风险高，业务心智也更差。
- **方案 B：只有输入组件默认自动触发，同时保留通用 requester（采用）**
  - 是。既解决核心问题，又不扩大默认副作用。

> 注："默认只作用于输入组件"这一效果在首版**通过路 B 实现**——只在会触发 IME 的输入组件链路（`CoreTextField` / `TextField` / `BasicTextField`）恢复 `FocusableNode` 的 requester 请求并补光标 rect 请求；通用 `focusable`（按钮、开关等）不自动触发。具体机制见 D8。

### D3：采用"Requester + Responder"两层模型，而不是把逻辑直接写死在 `TextField`

**选择**：引入通用 `BringIntoViewRequester` 与 `Modifier.bringIntoViewRequester(...)`；滚动容器提供内部 responder 能力，接收目标节点的 bring-into-view 请求并负责执行滚动。

建议的模块组织：

- `compose/foundation/relocation/BringIntoViewRequester.kt`
  - 定义 requester、默认实现、modifier element
- `compose/foundation/relocation/BringIntoViewResponder.kt`
  - 定义 responder 接口与请求传播协议
- `compose/foundation/relocation/BringIntoViewCalculator.kt`
  - 统一承载可视区判定和目标偏移计算
- `compose/foundation/text/...`
  - 需要接入默认自动触发：`CoreTextField` 新增 `BringIntoViewRequester`，在获焦、点击光标、输入、selection 变化时请求光标矩形（对齐官方 `bringSelectionEndIntoView`），详见 D8
- `compose/foundation/Scroll.kt`
  - `verticalScroll` 容器接入 responder
- `compose/foundation/lazy/...`
  - `LazyColumn` / `LazyListState` 接入 responder

**原因**：

- `BringIntoViewRequester` 是官方 Compose 的能力面，不能只做成 TextField 私有逻辑。
- 让请求层与容器响应层解耦，后续扩到 Grid / Pager / Native bridge 才有演进空间。
- 业务也可以手动对任意节点发起 request，而不是只能依赖输入框默认行为。

**替代方案对比**：

- **方案 A：在 `CoreTextField` 内直接找最近 `ScrollState` 并滚动**
  - 否。强耦合、难扩展、无法对齐官方 API。
- **方案 B：Requester + Responder（采用）**
  - 是。结构更通用，也更适合后续 KuiklyDSL 复用同一思路。

### D4：可视区判定以"目标 rect、容器 viewport、IME 遮挡边界"三者关系为准

**选择**：以目标节点整体 rect 是否落入"当前可见窗口"来判定是否需要滚动，计算逻辑统一下沉到 calculator：

```text
visibleTop = containerViewportTop
visibleBottom = min(containerViewportBottom, windowBottom - imeBottomPx)

if targetBottom > visibleBottom:
    scrollDownBy(targetBottom - visibleBottom + extraMargin)
else if targetTop < visibleTop:
    scrollUpBy(targetTop - visibleTop - extraMargin)
else:
    no-op
```

其中：

- `target rect` 来源于 `FocusedBounds` / `LayoutCoordinates`
- `container viewport` 来源于容器自身 layout coordinates / layoutInfo
- `imeBottomPx` 来源于 `WindowInsets.ime`
- `extraMargin` 作为小安全边距，避免输入框贴边

**原因**：

- phase2 要解决的是"看得到"，本质是几何关系判断，不是单纯的"有焦点就滚"。
- 使用统一公式，可以同时复用到 `ScrollState` 和 `LazyListState`。
- 只保证组件整体可见，比 caret 级方案简单且足够覆盖首版表单场景。

**替代方案对比**：

- **方案 A：只要 focus 就固定滚一个常量距离**
  - 否。不同布局、不同键盘高度下会明显不准。
- **方案 B：按目标 rect 与可视区重叠关系计算（采用）**
  - 是。可解释性强，也更可测试。

### D5：触发源必须同时覆盖 focus、focused bounds、IME 和 viewport 变化

**选择**：在以下时机重算 bring-into-view：

- 输入组件 **新获得焦点**
- focused target 的 `LayoutCoordinates` **发生变化**
- `WindowInsets.ime` **发生变化**
- scroll 容器 viewport / layout size **发生变化**

**原因**：

- 只监听 focus 不够。常见顺序是：用户先点输入框 → 焦点先到 → 键盘随后弹起 → 这时才发生遮挡。
- 只监听 IME 也不够。列表滚动、内容重排、横竖屏或安全区变化同样会改变可视区。
- `FocusedBounds` 已经能提供焦点几何变化的冒泡链路，正好适合做统一重算源。

**替代方案对比**：

- **方案 A：只在 `onFocusChanged` 里滚一次**
  - 否。键盘后弹、容器重排等场景会漏判。
- **方案 B：多触发源统一重算（采用）**
  - 是。更符合真实交互时序。

### D6：`ScrollState` 与 `LazyListState` 首版都走"像素位移优先"

> **修订（2026-07-28）**：`ScrollState` 支持已移除。Kuikly Compose 的 `ScrollState`/`verticalScroll` 容器不具备手势滚动能力（无 `Modifier.scrollable` 实现），且全仓库业务 demo 零使用，phase2 范围收敛为仅 `LazyListState`/`LazyColumn`。`Scroll.kt` 相关改动（responder 安装、`verticalScroll`/`horizontalScroll` 公开函数、`ScrollStateBringIntoViewResponderNode`）已整体回退，下文 ScrollState 部分仅作历史决策记录保留。
>
> 同期两项缺陷修复（保留在 `BringIntoViewResponder.kt`）：
> 1. **路 A 可视区定义**：viewport shrink 检测改为「IME 裁剪后的有效可视高度」（`min(容器底, 窗口底 − imeBottomPx) − 容器顶`），修复覆盖模式（容器不 resize）下键盘弹起不触发补偿的问题。官方 `onRemeasured` 语义的环境泛化，判定公式与触发条件与官方一致。
> 2. **坐标 clip 策略统一**：路 B 的 target bounds 从 `boundsInRoot()`（受视口 clip 裁剪）改为 `localBoundingBoxOf(clipBounds=false)` 换算，修复部分露出的输入框被误判为"已完全可见"而双路 no-op 的问题。

**选择**：

- `ScrollState`：直接使用 `animateScrollBy(delta)`
- `LazyListState`：对已 compose 的 focused item，同样优先使用 `animateScrollBy(delta)`；仅在后续扩展 programmatic/offscreen 场景时再考虑 index-based 补偿

**原因**：

- 两者都实现了 `ScrollableState`，已有统一的像素级滚动能力。
- 对真实"点击输入框获焦"的场景，目标节点本来就已经 compose，像素位移最自然。
- 一开始就引入 item index 推导，会把 MVP 拉入更多列表结构与 key/index 协议复杂度。

**替代方案对比**：

- **方案 A：Lazy 一上来只做 `animateScrollToItem(index)`**
  - 否。自动聚焦时并不天然持有 index，而且会丢失精细偏移控制。
- **方案 B：优先像素位移（采用）**
  - 是。实现更直接，体验也更平滑。

### D7：为避免抖动，自动滚动需要串行化和去重

**选择**：在 responder 层维护一次仅有一个 active request 的串行策略，并加入以下保护：

- 当目标已经 fully visible 时直接 no-op
- 当本次 `delta` 小于阈值时直接忽略
- 新请求到来时取消旧动画，只保留最新目标
- 滚动完成后再次检查，若仍未可见再补一次，避免持续循环

**原因**：

- 滚动会引起 bounds 改变；bounds 改变又可能重新触发请求，如果没有去重就会抖动。
- 键盘弹起过程中 IME 高度可能连续变化，如果每次都启动独立动画，也会造成"来回抢滚"。

**替代方案对比**：

- **方案 A：每次事件都无条件起一次动画**
  - 否。很容易抖动、重入、抢滚。
- **方案 B：串行化 + 去重 + 阈值（采用）**
  - 是。更容易得到稳定闭环。

### D8：对齐官方——路 A 和路 B 同时接入，共用 `ContentInViewNode`

**背景**：经核实官方 Compose 源码（`compose-multiplatform-core/compose/foundation/foundation/...`），官方 TextField 的自动滚动机制如下：

- **路 B 是获焦滚动的主触发源**。官方 `FocusableNode` 内部持有 `BringIntoViewRequester`，获焦时调用 `bringIntoViewRequester.bringIntoView()` 请求整个 focusable bounds。官方 `CoreTextField` 还额外创建自己的 `BringIntoViewRequester`，在获焦、点击光标、输入、selection 变化时调用 `bringSelectionEndIntoView` 请求光标矩形。
- **路 A 是 viewport 缩小时的焦点保持机制**。官方滚动容器同时安装 `BringIntoViewResponderNode`（B 入口）和 `FocusedBoundsObserverNode`（A 入口），二者共用同一个 `ContentInViewNode`。A 回调只记录 `focusedChild`，**不立即滚动**；仅在 viewport 因键盘等原因缩小时，判断"焦点项在旧 viewport 完全可见、在新 viewport 被裁剪"才启动补偿滚动。
- **A 和 B 不是二选一**，而是两个入口共用同一套滚动计算与动画。B 优先处理请求队列；无 B 请求时才使用 A 跟踪的 focused child。

当前 Kuikly 骨架存在的问题：

1. `FocusableNode` 里获焦时的 `bringIntoViewRequester.bringIntoView()` 被注释掉（`Focusable.kt:228-233`），路 B 的默认触发未启用。
2. `CoreTextField` 没有自己的 `BringIntoViewRequester`，不支持光标矩形请求。
3. 路 A 的 `onFocusedBoundsChanged` 收到焦点坐标后立即 `scheduleRequest()`，与官方"只记录、viewport shrink 时才滚"的语义不一致。
4. 路 A 和路 B 共写同一个 `focusedCoordinates`，存在双写竞态。
5. `ContentInViewNode.kt` 基本是空壳（只有常量定义），没有实现。
6. 滚动容器（`Scroll.kt` / `LazyList.kt`）没有安装 responder 或 observer。

**选择**：MVP 同时接入路 A 和路 B，对齐官方 `ContentInViewNode` 架构。

- **路 B 作为获焦、光标移动、输入时的主触发源**：
  - 恢复 `FocusableNode` 获焦时的 `bringIntoViewRequester.bringIntoView()`（请求整个 focusable bounds）。
  - `CoreTextField` 新增 `BringIntoViewRequester`，在获焦、点击光标、输入、selection 变化时请求光标矩形（对齐官方 `bringSelectionEndIntoView`）。
  - 但只在输入组件链路默认启用，不在所有 `focusable`（按钮、开关等）上默认启用（对齐 D2）。
- **路 A 作为 viewport / IME 缩小时的焦点保持补偿**：
  - `onFocusedBoundsChanged` 改为只记录 `focusedChild`，不立即 `scheduleRequest()`。
  - 在 viewport / IME 可视区缩小时，判断"原本可见现在被裁剪"才触发滚动。
- **A 和 B 统一收口到 `ContentInViewNode`**：
  - 实现空壳 `ContentInViewNode.kt`，同时作为 A 的 observer 入口和 B 的 responder 入口。
  - 维护 `focusedChild`（A 跟踪）和请求队列（B 一次性请求），消除 `focusedCoordinates` 双写。
  - B 请求优先；无 B 请求时才使用 A 跟踪的 focused child。

**原因**：

- 官方源码已给出明确模板，照着做比"自己造一套简化版 A"更稳。
- 路 B 的 requester 骨架 Kuikly 已写好（`BringIntoViewRequester.kt` / `BringIntoViewResponder.kt`），恢复 FocusableNode 的请求和 CoreTextField 的光标请求是对齐官方的一步。
- 双写竞态本来就要解决，不如一次性按官方 `ContentInViewNode` 架构收口。
- 后续支持"长文本打字时光标跟随滚动"可以直接复用路 B 的光标 rect 请求，不需要二次重构。

**由此带来的调整**：

- `Focusable.kt` 里注释掉的 `bringIntoViewRequester.bringIntoView()` **恢复启用**，但限制为只在输入组件链路生效。
- `CoreTextField` **需要改动**：新增 `BringIntoViewRequester`，实现 `bringSelectionEndIntoView`。
- 路 A 的 `onFocusedBoundsChanged` **改为官方语义**：只记录 `focusedChild`，viewport shrink 时才滚。
- `ContentInViewNode.kt` 从空壳实现为统一 content-in-view 节点。
- 原 D8 的"遗留设计异味（双写竞态）"**已由 `ContentInViewNode` 统一收口解决**，不再遗留。

**替代方案对比**：

- **方案 A：MVP 只走路 A（原 D8）**
  - 否。与官方实现不一致（官方获焦滚动走 B）；不支持光标跟随；路 A 的"收到焦点立即滚"偏离官方语义。
- **方案 B：MVP 同时接 A + B，对齐官方 `ContentInViewNode`（采用）**
  - 是。架构与官方一致，后续光标跟随、长文本打字都能直接扩展，双写竞态一次性消除。

## Planned File Changes by Module

### `compose/`

- `compose/.../foundation/relocation/BringIntoViewRequester.kt`
  - 新增 requester 定义、默认实现、modifier API
- `compose/.../foundation/relocation/BringIntoViewResponder.kt`
  - 新增 responder 协议、请求传播与取消机制
- `compose/.../foundation/relocation/BringIntoViewCalculator.kt`
  - 新增遮挡判定与滚动偏移计算
- ~~`compose/.../foundation/Scroll.kt`~~（**2026-07-28 已回退**：ScrollState 场景移出 phase2 范围）
  - ~~为 `verticalScroll` 容器接入 responder~~
- `compose/.../foundation/lazy/LazyListState.kt`
  - 暴露 responder 所需的 viewport 与 scroll 能力
- `compose/.../foundation/lazy/...`
  - 在 `LazyColumn` / lazy 布局接入 responder 与 focused item 绑定
- `compose/.../foundation/Focusable.kt`
  - 恢复被注释的 `bringIntoViewRequester.bringIntoView()`：获焦时请求整个 focusable bounds（对齐官方 `FocusableNode.onFocusEvent`）
  - 但只在输入组件链路生效，不在所有 focusable 上默认启用
- `compose/.../foundation/text/CoreTextField.kt`
  - 新增 `BringIntoViewRequester`，在获焦时调用 `bringSelectionEndIntoView`（对齐官方）
  - 在点击光标、输入、selection 变化时请求光标矩形
- `compose/.../foundation/text/TextFieldGestureModifiers.kt`
  - 挂接 `bringIntoViewRequester(requester)` modifier
- `compose/.../foundation/FocusedBounds.kt`
  - 路 A 改为官方语义：`onFocusedBoundsChanged` 只记录 `focusedChild`，不立即 `scheduleRequest()`
  - 在 viewport / IME 可视区缩小时，判断"原本可见现在被裁剪"才触发滚动
- `compose/.../foundation/gestures/ContentInViewNode.kt`
  - 从空壳实现为统一 content-in-view 节点：同时作为 A 的 observer 入口和 B 的 responder 入口
  - 维护 `focusedChild`（A 跟踪）和请求队列（B 一次性请求），消除 `focusedCoordinates` 双写

### `demo/`

- `demo/.../BringIntoViewDemo.kt`
  - 新增长表单验证页
- `demo/.../LazyColumnBringIntoViewDemo.kt` 或复用同页多 section
  - 验证 lazy list 场景
- `demo/.../ImeInsetDemo.kt` / `ScaffoldImeInsetDemo.kt`
  - 如有必要，补入口或说明 phase1 与 phase2 的差异

### `openspec/`

- `openspec/changes/compose-bring-into-view-phase2/`
  - proposal / design / specs / tasks

## Skeleton Exit Criteria And Next Steps

### 什么算"骨架完成"

本方案中的"骨架"不是指最终功能可交付，而是指以下三层已经建立并能独立工作：

1. **请求层**：`BringIntoViewRequester` 与 `Modifier.bringIntoViewRequester(...)` 已存在，request 能找到最近 responder
2. **判定层**：能基于 focused rect、viewport rect、`WindowInsets.ime` 计算出是否需要滚动以及滚动方向 / delta
3. **响应层**：responder 已具备最小可用的请求串行化、取消旧请求、阈值过滤与滚动后复检机制

也就是说，骨架阶段重点验证的是"**链路和分层成立**"，而不是"所有容器和输入场景都已接完"。

### 骨架完成后的验收口径

骨架完成后，建议先做一次阶段性验收，再进入具体容器接入：

- **链路验收**：requester 在有 responder 时能把 request 向上送达，在无 responder 时能安全返回
- **几何验收**：计算器能正确区分无遮挡、底部被 IME 遮挡、顶部越界三类情况，并给出稳定 delta
- **稳定性验收**：请求串行化与去重机制能避免明显抖动、死循环、重复补偿
- **分层验收**：确认 requester / responder / calculator 的职责边界清晰，再继续接具体容器和默认输入行为

### 骨架完成后还要继续做什么

骨架完成并通过阶段验收后，后续工作按下面顺序推进最稳：

```text
基础骨架完成
    ↓
骨架阶段验收
    ↓
实现 ContentInViewNode（A+B 统一入口）
    ↓
接入 ScrollState / verticalScroll（安装 responder + observer）
    ↓
恢复 FocusableNode 路 B 请求 + CoreTextField 光标 rect 请求
    ↓
修正路 A 语义（viewport shrink 时才滚）
    ↓
接入 LazyListState / LazyColumn
    ↓
补 demo 与三端回归
    ↓
整理 phase2 扩展 backlog
```

其中每一步的目标分别是：

- **实现 `ContentInViewNode`**：建立 A+B 统一入口，消除 `focusedCoordinates` 双写竞态
- **接入 `ScrollState` / `verticalScroll`**：先拿下最简单、最可控的长表单场景，验证骨架确实能落到真实容器
- **恢复 `FocusableNode` 路 B + `CoreTextField` 光标请求**：让输入框获焦时通过路 B 自动触发滚动，并支持光标跟随
- **修正路 A 语义**：让 viewport / IME 缩小时的焦点保持补偿与官方一致
- **接入 `LazyListState` / `LazyColumn`**：补齐高频列表输入场景，验证 focused item pinning 与 viewport 计算在 lazy 模型下是否稳定
- **补 demo 与三端回归**：把"看得到"的用户可见收益固定成可复测的场景，避免后续改动破坏
- **整理 phase2 扩展 backlog**：把本期明确不做、但后续高概率要做的内容收敛成下一阶段输入

### 每个阶段完成后应交付什么

为了避免后续只说"做完了"却没有统一的阶段产物，建议把每个阶段的最小交付物也固定下来：

| 阶段 | 最小交付物 | 目的 |
|---|---|---|
| 骨架阶段 | 一段 request/responder 验证录屏或 GIF + 一份骨架验收结论 | 证明链路与分层成立 |
| `ContentInViewNode` 阶段 | A+B 统一入口实现 + 双写竞态消除验证 | 证明架构对齐官方 |
| `ScrollState` 阶段 | 一个长表单 demo + 一条底部输入框自动可见录屏 | 证明 MVP 已进入真实容器 |
| 路 B 触发 + 光标请求阶段 | `FocusableNode` requester 恢复 + `CoreTextField` 光标 rect 请求 + 获焦自动滚动录屏 | 证明输入框获焦通过路 B 自动滚入可视区 |
| 路 A 语义修正阶段 | viewport shrink 时焦点保持补偿录屏 + 不误滚验证 | 证明路 A 与官方语义一致 |
| `LazyListState` 阶段 | 一个 `LazyColumn` demo + 一条键盘已弹出时切换焦点的录屏 | 证明 lazy 场景不只是理论可行 |
| 回归阶段 | Android / iOS / HarmonyOS 三端回归结论 | 证明跨端行为对齐到"最终可见" |

### 建议的阶段验收材料

骨架之后，每一阶段都尽量至少留下下面三类材料，避免口头评审时信息丢失：

1. **一条最短录屏**：只展示本阶段新增能力，不把其他未稳定因素混进来
2. **一段结论文字**：说明"这阶段已经解决了什么、还没解决什么"
3. **一条已知限制**：明确当前阶段不承诺覆盖的边界，防止误用

这样做的价值是：

- 后续接力的人不需要重新猜"上一阶段到底验证到了哪一步"
- 评审时能快速对齐"当前是骨架通过，还是体验通过，还是三端通过"
- 如果后面需要回退，也能知道回退后还剩下哪些被证明过的能力

### 骨架之后的文档化 backlog

phase2 首版完成后，文档中应继续保留以下后续方向，避免后续讨论重新发散：

- **默认行为开关**：是否给输入组件默认自动 bring-into-view 提供 opt-out
- **离屏 focus 能力**：程序化 focus 到尚未 compose 的 lazy item 时，是否补 index / key 级协议
- **嵌套滚动协商**：多父滚动容器时，request 应先由谁消费、消费多少
- **更精细的可见性目标**：是否从"组件整体可见"继续演进到 caret / selection 级可见
- **Native `Scroller` bridge`**：当纯 Compose 容器闭环不足时，再评估是否需要下沉到 Native 协议层

## Risks / Trade-offs

- **[重复滚动抖动]** 焦点变化、bounds 变化、IME 变化会连续触发请求，若每次都直接滚动，页面会抖动。→ **缓解**：请求串行化、阈值过滤、滚动后复检但限制补偿次数。
- **[键盘动画期间反复抢滚]** 键盘弹出过程中可视区会持续收缩，自动滚动可能被多次打断。→ **缓解**：以"最新请求覆盖旧请求"为准，并把 phase2 目标限制为"最终可见"，不追求逐帧贴边。
- **[多父滚动容器责任不清]** 嵌套 scroll 场景里，不容易判定到底该谁滚。→ **缓解**：首版只承诺最近的纵向 scroll parent，nested scroll 全量支持放到后续阶段。
- **[Lazy item 回收导致目标丢失]** 自动滚动过程中，focused item 可能被 lazy 回收。→ **缓解**：复用现有 pinnable container / pinned item 机制，在焦点存续期间尽量固定目标节点。
- **[程序化 focus 到离屏 item]** 若目标节点还未 compose，就拿不到稳定坐标。→ **缓解**：首版不承诺该场景；后续若需要，再补 index/key 级协议。
- **[业务自定义滚动冲突]** 业务自己也在滚动时，默认自动滚动可能抢控制权。→ **缓解**：请求统一走 scroll mutex；必要时为输入默认行为提供 opt-out 开关。
- **[只保证组件整体可见，不保证 caret 精准对齐]** 首版体验会略弱于 editor 级精细方案。→ **缓解**：先保证"看得到"，caret/selection 级对齐放到后续增强。

## Migration Plan

phase2 骨架的三块（requester 节点 / responder 基类 / calculator）已各自写好，但**彼此未连、也未接到任何真实容器与输入框**，且路 B 的默认触发被注释、路 A 语义偏离官方。首版的核心工作是补齐接线并对齐官方 `ContentInViewNode` 架构：

- **缺口①：`ContentInViewNode.kt` 是空壳**
  当前只有常量定义。需实现为统一 content-in-view 节点：同时作为 A 的 `FocusedBoundsObserverNode` 入口和 B 的 `BringIntoViewResponderNode` 入口；维护 `focusedChild`（A 跟踪）和请求队列（B 一次性请求）；B 请求优先，无 B 请求时才使用 A 跟踪的 focused child。
- **缺口②：容器缺少 responder / observer 实例**
  `verticalScroll` / `LazyColumn` 里没有任何 `BringIntoViewResponderNode` 子类或 `FocusedBoundsObserverNode`。需在滚动容器同时安装两者，共用同一个 `ContentInViewNode`（对齐官方 `Scrollable.kt:323-335` 的 `delegate` 模式）。
- **缺口③：路 B 默认触发被注释**
  `Focusable.kt:228-233` 的 `bringIntoViewRequester.bringIntoView()` 被注释。需恢复，但限制为只在输入组件链路生效。
- **缺口④：`CoreTextField` 没有光标 rect 请求**
  需新增 `BringIntoViewRequester`，在获焦、点击光标、输入、selection 变化时请求光标矩形（对齐官方 `bringSelectionEndIntoView`）。
- **缺口⑤：路 A 语义需要修正**
  `BringIntoViewResponderNode.onFocusedBoundsChanged` 当前收到焦点坐标后立即 `scheduleRequest()`。需改为只记录 `focusedChild`，在 viewport / IME 可视区缩小时才判断"原本可见现在被裁剪"再滚。
- **缺口⑥：`imeBottomPx` 没人传入**
  `ContentInViewNode` 的 `imeBottomPx` 由构造 / `update(...)` 传入。容器 modifier 工厂需读取 `WindowInsets.ime` 的 bottom 并传入，IME 变化时 recompose → `update` → 重算。

补线顺序：

1. 在 `openspec` 中冻结 phase2 能力边界与验收口径（D8 已修订为对齐官方）。
2. 实现 `ContentInViewNode.kt`（缺口①），作为 A+B 统一入口。
3. 补 `ScrollState` 版 `ContentInViewNode` 子类，在 `verticalScroll` 同时安装 responder + observer（缺口②⑥），用长表单页验证最小闭环。
4. 恢复 `FocusableNode` 的路 B 请求（缺口③），在 `CoreTextField` 接入光标 rect 请求（缺口④），修正路 A 语义（缺口⑤）。
5. 把 `BringIntoViewDemo` 注册进 `ComposeAllSample`，验证 `ScrollState` 场景（第一个可见里程碑，Android 单端即可）。
6. 补 `LazyListState` 版 `ContentInViewNode` 子类（`viewportRect` 用 `layoutInfo`），验证 `LazyColumn` 场景。
7. 三端回归（Android → iOS → HarmonyOS）。

**回滚策略：**

- 若自动滚动造成明显抖动或业务兼容问题，可先关闭输入组件默认自动触发，仅保留公共 `BringIntoViewRequester` API。
- 若 Lazy 场景稳定性不足，可先只发布 `ScrollState` 支持，把 `LazyListState` 回退为后续增量。
- 因为 phase2 首版不改 renderer 协议，所以回滚只需局限在 `compose/` 与 demo。

## Open Questions

- ~~路 A（`onFocusedBoundsChanged`）与路 B（`bringChildIntoView`）都会写入同一个 `focusedCoordinates`，是否要统一为单一来源？~~ → **已由 D8 修订解决**：`ContentInViewNode` 统一收口，A 只记录 `focusedChild`，B 走独立请求队列，不再共写 `focusedCoordinates`。
- 是否需要在首版就提供业务可配置的自动行为开关，例如 `TextFieldDefaults.autoBringIntoView` 或 modifier 级 opt-out？
- `LazyListState` 首版是否要额外暴露更明确的 viewport helper，避免 responder 直接依赖过多内部实现？
- 是否要为 bring-into-view 引入统一的最小安全边距常量，并开放业务覆盖？
- phase2 稳定后，KuiklyDSL 是否直接复用同一套 request / responder 分层，还是在 core 层抽象公共协议？
