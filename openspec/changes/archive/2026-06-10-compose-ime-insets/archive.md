## 1. 归档范围

本归档覆盖下列两个 change 的设计与回滚过程（按时间顺序）：

| 序号 | Change | 时间 | 性质 | 状态 |
|---|---|---|---|---|
| 1 | `compose-ime-insets` | 2026-06-10 | 首次建立 phase1 基座（`WindowInsets.ime` / `imePadding()` / `Scaffold` 默认 IME inset / 三端 page-level 桥接） | 已落地，spec 进入 `openspec/specs/compose-ime-insets/spec.md` |
| 2 | `compose-ime-return-to-phase1` | 2026-07-27 | 在原基座上叠加了多轮 phase1 之外的能力后，回退到原始 phase1 语义 | 已落地，phase1 边界被进一步收紧 |

## 2. Change 1：compose-ime-insets（2026-06-10）

### 2.1 Why

Kuikly Compose DSL 当时已具备 `WindowInsets` 消费骨架和 `Scaffold` 内容 inset 语义，但仍缺少页面级 IME 状态通道，导致业务只能手动监听键盘高度并自行调整布局。Phase 1 目标：补齐统一的 `ime insets` 底座，让常见输入页能以声明式方式完成键盘规避，而不是继续扩散业务层的临时方案。

### 2.2 What Changes

- 为 Compose DSL 新增页面级 IME inset 能力，统一把键盘底部占用空间注入 `WindowInsets` 体系。
- 新增 `WindowInsets.ime` 与 `Modifier.imePadding()`。
- 调整 `Scaffold` 默认内容 inset 接入策略，组合系统栏与 IME。
- 在 Android / iOS / HarmonyOS renderer 中补齐页面级键盘高度状态接入。
- 新增 demo 验证页面，覆盖底部输入栏和普通表单页两类 phase1 目标场景。

### 2.3 关键 Decisions

- **D1**：IME 状态通过新的 page-level pager event 进入 Compose（建议命名 `imeInsetsDidChanged`），而不是继续复用输入组件事件。
- **D2**：Compose 侧用 `Configuration` 持有 IME 状态，`WindowInsets.ime` 作为只读投影。
- **D3**：`WindowInsets.ime` 与 `Modifier.imePadding()` 作为 phase1 业务主入口。
- **D4**：`ScaffoldDefaults.contentWindowInsets` 改为 `systemBarsForVisualComponents.union(WindowInsets.ime)`，而不是改写 `systemBarsForVisualComponents` 语义。
- **D5**：平台桥接采用"优先复用已有键盘源、补齐页面级挂载点"——Android 复用 `KRKeyboardModule`；iOS 在 `KuiklyRenderView.m` 页面宿主侧新增观察者；HarmonyOS 复用 `KRWindowInfo.ets` 窗口级 `keyboardHeightChange`。
- **D6**：保留旧输入组件事件，phase1 新能力与旧 `keyboardHeightChange` 并存。

### 2.4 Non-Goals（明确不在 phase1 范围）

- 不实现 `BringIntoView`、聚焦自动滚动、原生 `Scroller` 可见区域滚动协议。
- 不实现 `imeNestedScroll`、系统逐帧 IME 动画联动。
- 不追求 Web / miniApp / macOS 的等价键盘规避能力。

### 2.5 关键交付（落地后）

- `compose/.../LocalConfiguration.kt` 新增 `imeBottomDp` / `imeAnimationDuration` / `imeAnimationCurve` 等状态字段。
- `compose/.../WindowInsets.kt` 新增 `WindowInsets.Companion.ime`。
- `compose/.../WindowInsetsPadding.kt` 新增 `Modifier.imePadding()`。
- `compose/.../material3/Scaffold.kt` 默认 `contentWindowInsets` 组合系统栏 + IME。
- `core-render-android/` / `core-render-ios/` / `core-render-ohos/` 三端补齐 page-level IME pager event 桥接。
- `demo/.../ImeInsetDemo.kt`（原 `KeyBoardHeightDemo.kt`）覆盖 `imePadding()` 场景。
- `demo/.../ScaffoldImeInsetDemo.kt`（原 `ScaffoldDemo.kt`）覆盖 `Scaffold` 默认避让场景。

### 2.6 任务清单（执行结果）

```
[x] 1.1 在 core/Pager.kt 中新增 page-level IME 事件常量与 payload key
[x] 1.2 沿用现有 pager event 分发链路，不影响 root/window/configuration 事件
[x] 2.1 LocalConfiguration 新增 IME 状态字段与 onImeInsetsChanged()
[x] 2.2 ComposeContainer 接收 IME pager event 并更新页面级 IME 状态
[x] 2.3 WindowInsets 新增 WindowInsets.ime
[x] 2.4 WindowInsetsPadding 新增 Modifier.imePadding()
[x] 2.5 Scaffold 默认 contentWindowInsets 组合 system bars + IME
[x] 3.1 Android 复用 KRKeyboardModule，补齐页面级 listener
[x] 3.2 iOS 页面宿主层补齐键盘通知 observer
[x] 3.3 HarmonyOS 把窗口级 keyboardHeightChange 接到 IME pager event
[x] 3.4 三端 listener 生命周期与释放验证
[x] 4.1 demo 覆盖底部输入栏 + Modifier.imePadding() 场景
[x] 4.2 demo 覆盖 Scaffold 默认避让场景
[x] 4.3 旧 keyboardHeightChange 业务写法兼容性验证
[ ] 5.1 Android 三端验证
[ ] 5.2 iOS 三端验证
[ ] 5.3 HarmonyOS 三端验证
```

## 3. Change 2：compose-ime-return-to-phase1（2026-07-27）

### 3.1 Why

phase1 基座落地后，仓库又叠加了多轮超出 phase1 范围的能力：

- `compose-ime-animation-polish`
- `compose-ime-linear-animation-mode`
- `native-ime-animation-progress`
- `ios-ime-sync-send-event`
- `compose-ime-sync-curve-animation`

这些 change 共同引入了：动画投影、`native-progress`、`source / animatedHeight` 协议扩展、IME 事件同步发送特例、iOS DisplayLink / proxyView 逐帧能力，导致当前实现语义明显偏离 phase1 原始边界。

本 change 的目的不是重新设计 phase2/phase3，而是先把当前实现重新收敛到 phase1 合约：

- 保留页面级 IME inset 基座与声明式 API；
- 移除所有 phase1 之外的动画治理与同步派发依赖；
- 降低复杂度、减少跨端分叉；
- 为后续是否重启动画优化留出干净基线。

### 3.2 What Changes

- 将 Compose IME 消费链路回退到 phase1 语义：`WindowInsets.ime` 直接投影页面级 IME 当前高度，不再由框架内部执行目标值补间、非线性 easing 或 `native-progress` 优先消费。
- 将 IME page event 协议收敛回 phase1 范围：保留 `height / duration / curve`，撤销 `source / animatedHeight` 及相关常量、兼容分支、调试语义。
- 撤销三端 renderer 和宿主层对 `imeInsetsDidChanged` 的同步发送特例。
- 保留 `WindowInsets.ime` / `Modifier.imePadding()` / `Scaffold` 默认内容避让 / 旧 `keyboardHeightChange` 兼容性。
- 回滚 demo 页面中的 phase3 文案、调试字段和"sync curve / native-progress / phase3 提前映射"说明。
- OpenSpec 层面：phase1 之外的 IME 动画 change 视为 superseded / withdrawn，不再作为当前基线继续推进。

### 3.3 关键 Decisions

- **D1**：保留 phase1 公开能力，撤销 phase1 之外的默认行为。
- **D2**：IME page event 协议回退到 `height / duration / curve`；移除 `source` / `animatedHeight` / `native-progress` 常量。
- **D3**：`LocalConfiguration` 回退到单一 IME 当前值模型；移除 `imeTargetBottomDp` / `imeAnimationSource` / raw / projected 双状态。
- **D4**：`WindowInsets.ime` 直接投影当前 IME 高度；撤销 `animateFloatAsState` / `tween` / easing 映射 / `native-progress` 优先消费 / `SideEffect` 投影回写。
- **D5**：三端 renderer 保留 page-level 高度桥接，但不再为 IME 做 sync-send 特例。
- **D6**：Demo 回到 phase1 验证口径；移除 `IME Sync Curve` / `Native Progress` / `phase3 提前映射` 调试展示。
- **D7**：OpenSpec 层将后续 IME 动画 change 视为 superseded / withdrawn 候选。

### 3.4 Non-Goals

- 不重新定义 phase2 / phase3 路线图。
- 不引入新的动画能力、逐帧进度能力或更高精度的键盘联动。
- 不移除 phase1 公开业务入口。
- 不修改自研 DSL / Web / miniApp / macOS 的键盘能力边界。
- 不在本 change 中直接实施代码回滚；本 change 只负责把回滚目标、边界与验收口径定义清楚。

### 3.5 与 phase1 的差异点（回退生效后）

| 维度 | phase1（已落地）| 回退后变更 |
|---|---|---|
| `WindowInsets.ime` | 投影 `LocalConfiguration.imeBottomDp` 当前值 | 移除内部 `animateFloatAsState` / `tween` / easing 投影 |
| `LocalConfiguration` | 单一 `imeBottomDp` + 内部 `duration/curve` 预留 | 移除 `imeTargetBottomDp` / `imeAnimationSource` / `normalizeImeAnimationSource` / `onImeAnimationProgressChanged` |
| IME page event payload | `height / duration / curve` | 移除 `source` / `animatedHeight` |
| iOS renderer | 页面宿主键盘通知 → page event | 移除 `native-progress` / DisplayLink / proxyView / `source / animatedHeight` 发送 / IME sync 特例 |
| Android / OHOS renderer | 复用 page-level keyboard event | 移除 `source` 透传与 IME sync 特例 |
| Demo | phase1 验证口径 | 移除 `IME Sync Curve` / `Native Progress` / `phase3 提前映射` 等调试展示 |

### 3.6 任务清单（执行结果）

```
[x] 1.1 确认 compose-ime-return-to-phase1 与 compose-ime-insets phase1 边界一致
[x] 1.2 明确与 5 个 IME 动画 / sync change 的 superseded / withdrawn 处理策略
[x] 2.1 core/Pager.kt 移除 IME_SOURCE / IME_ANIMATED_HEIGHT 常量
[x] 2.2 ComposeContainer 回退到只解析 phase1 payload
[x] 2.3 LocalConfiguration 移除 target / projected / source 相关状态
[x] 2.4 WindowInsets 移除内部 IME 补动画与 native-progress 优先消费
[x] 2.5 确认 imePadding() / Scaffold 默认避让保持 phase1 行为
[x] 3.1 Android 宿主层移除 source 透传与 IME sync 特例
[x] 3.2 iOS renderer 移除 native-progress / DisplayLink / proxyView 等
[x] 3.3 HarmonyOS renderer 移除 source 透传与 IME sync 特例
[x] 3.4 iosApp / ohosApp demo host 回退 imeInsetsDidChanged 同步声明
[x] 3.5 确认三端 page-level keyboard height bridge 仍保留
[x] 4.1 ImeInsetDemo 回退到 phase1 验证文案与展示字段
[x] 4.2 ScaffoldImeInsetDemo 回退到 phase1 验证文案与展示字段
[x] 4.3 移除 demo 中 sync curve / native-progress / phase3 提前映射等说明
[x] 5.1 Android 三端 phase1 验证
[x] 5.2 iOS 三端 phase1 验证
[x] 5.3 HarmonyOS 三端 phase1 验证
[x] 5.4 确认 phase1 行为不再依赖 source / animatedHeight / sync-send / native-progress
```

## 4. 两个 Change 共同覆盖的最终 Spec

当前活跃 spec 见 `openspec/specs/compose-ime-insets/spec.md`，归档时快照见本目录 `spec.md`，核心 ADDED / MODIFIED 需求：

### 4.1 核心 Requirements

- **Compose DSL SHALL expose phase1 page-level IME insets as direct page state projection**  
  `WindowInsets.ime` 是页面级 IME inset 源；其底部 inset 必须反映当前 page-level IME 高度状态；当键盘隐藏时必须返回 `0`；不得依赖框架内部 target/projection 双状态、非线性本地补间或 `native-progress` 专属消费语义。

- **Compose DSL SHALL provide imePadding based on unconsumed IME insets**  
  `Modifier.imePadding()` 添加等于 `WindowInsets.ime` 未消费底部 inset 的底部 padding，并参与现有 inset 消费链。

- **Material3 Scaffold SHALL include IME in its default content window insets**  
  `ScaffoldDefaults.contentWindowInsets` 同时包含视觉系统栏 inset 与当前 IME inset。

- **Existing component-level keyboard callbacks SHALL remain available**  
  旧 `keyboardHeightChange` 行为保留；新增 page-level IME insets 不得破坏已有业务写法。

- **Compose phase1 IME event consumption SHALL remain compatible with `height / duration / curve` only**  
  phase1 能力在 payload 只含 `height / duration / curve` 时仍能正确工作；`duration / curve` 可作为内部预留元信息，但不得要求 `source` / `animatedHeight`。

- **Compose IME phase1 baseline SHALL NOT require synchronous dispatch opt-in**（影响 `render-sync-send-event-policy` capability）  
  phase1 基线在普通 page-event 分发路径下应保持正确；host / delegator / controller 不得为 `imeInsetsDidChanged` 标记为同步事件以满足 phase1 capability。

### 4.2 受影响平台

- Android、iOS、HarmonyOS  
- Web、miniApp、macOS 不在本轮范围。

### 4.3 受影响模块

- `core/`：IME page event 常量与 payload key。
- `compose/`：`ComposeContainer` / `LocalConfiguration` / `WindowInsets` / `WindowInsetsPadding` / `Scaffold`。
- `core-render-android/` / `core-render-ios/` / `core-render-ohos/`：page-level 键盘桥接。
- `demo/`：`ImeInsetDemo` / `ScaffoldImeInsetDemo`。

## 5. Superseded 关系

下列 change 视为本系列的后续（已被回退到 phase1 基线）：

- `compose-ime-animation-polish`
- `compose-ime-linear-animation-mode`
- `native-ime-animation-progress`
- `ios-ime-sync-send-event`
- `compose-ime-sync-curve-animation`

它们均不再作为当前要继续推进的基线方向；若后续要重启动画优化路线，应作为独立 change 重新评估与提案。

## 6. 后续 phase2 入口

- `compose-bring-into-view-phase2`：在 phase1 页面级 IME 基座之上，新增"输入框获焦且被键盘遮挡时，框架自动把目标滚入可视区"的能力。
- 详细设计见 `openspec/changes/compose-bring-into-view-phase2/`。

## 7. 元信息

- 归档目录：`openspec/changes/archive/2026-06-10-compose-ime-insets/`
- 归档创建时间：2026-06-10（首个 change 落地），2026-07-27（回退 change 落地）
- 关联活跃 spec：`openspec/specs/compose-ime-insets/spec.md`
- 关联 draft spec（已并入活跃 spec）：`openspec/changes/archive/2026-07-27-compose-ime-return-to-phase1/specs/render-sync-send-event-policy/spec.md`（`render-sync-send-event-policy` 在 `openspec/specs/` 下尚未建立独立 spec，实施时按本归档中 §4.1 / §4.2 描述纳入 `compose-ime-insets` 主 spec 的 MODIFIED 需求）

---

*本归档将原 2026-06-10 与 2026-07-27 两个 change 合并为单目录下的 series 文档：1 份 design（见 `design.md`）+ 1 份 archive（本文）+ 1 份 spec（见 `spec.md`）+ 1 份 `.openspec.yaml` 元信息。*
