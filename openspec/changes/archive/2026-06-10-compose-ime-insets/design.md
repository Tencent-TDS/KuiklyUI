# Compose IME Insets Series — 系列设计文档

> 本文档是 `compose-ime-insets`（2026-06-10）与 `compose-ime-return-to-phase1`（2026-07-27）两个 change 的合并系列设计。
> 配套归档说明：`archive.md`；归档时的 spec 快照：`spec.md`。
> 当前活跃 spec：`openspec/specs/compose-ime-insets/spec.md`。

## 1. 上下文

### 1.1 背景

Kuikly Compose DSL 起步阶段已具备 `WindowInsets` 消费骨架与 `Scaffold` 内容 inset 语义，但仍缺少**页面级 IME 状态通道**。所有键盘相关行为只能由输入组件局部事件驱动，业务需手动监听 `keyboardHeightChange` 并自行算 padding。同时，Android / iOS / HarmonyOS 三端键盘事件分散在输入组件、平台 view 通知、窗口级 `keyboardHeightChange` 等多个不同入口，缺乏统一抽象。

### 1.2 适用范围

- **适用**：Compose DSL（`compose/` 模块与各平台 renderer 桥接）。
- **不适用**：自研 DSL（`core/` 下 `Pager` / `body()` 体系）。

### 1.3 NativeBridge / 跨层通信现状

不新增业务可见的 Native module API。复用现有页面事件通道：

```
平台 renderer 监听窗口级键盘高度
    ↓
renderer 通过现有 sendEvent / sendWithEvent / render.sendEvent 发送给 Kotlin Pager
    ↓
PagerManager 路由给 Pager.onReceivePagerEvent()
    ↓
ComposeContainer.onReceivePagerEvent() 更新 Compose 页面 IME 状态
    ↓
WindowInsets.Companion.ime 与 Modifier.imePadding() 消费该状态
```

这条链路的好处：

- `compose/` 不直接依赖 `core-render-*`；
- 不把 IME 能力绑死在输入组件上；
- 与 `root / window / configuration` 现有事件模式一致。

### 1.4 起点时已具备的基座

- `compose/foundation/layout/WindowInsets.kt`：已实现 `WindowInsets` / `union` / `exclude` / `asPaddingValues`。
- `compose/foundation/layout/WindowInsetsPadding.kt`：已实现 inset 消费与向下游传播。
- `compose/material3/Scaffold.kt`：已支持 `contentWindowInsets`（默认来自 `systemBarsForVisualComponents`）。
- `compose/platform/LocalConfiguration.kt`：已持有页面宽高、`safeAreaInsets`、字体缩放等页面级状态，通过 `mutableStateOf` 驱动 Compose 更新。
- `ComposeContainer.onReceivePagerEvent()`：已接收 `rootViewSizeDidChanged` / `windowSizeDidChanged` / `configurationDidChanged` 三类事件。

### 1.5 起点时存在的缺口

- `keyboardHeightChange` 仍是**输入组件事件**，入口在 `InputView` / `TextAreaView` 与 Compose `Modifier.keyboardHeightChange()`。
- Android 键盘高度来自 `KRKeyboardModule`，但当前主要由 `KRTextFieldView.kt` 局部订阅。
- iOS 键盘事件当前挂在 `KRTextFieldView.m` / `KRTextAreaView.m` 的 `UIKeyboardWillShow/Hide` 通知上。
- HarmonyOS 已有窗口级 `keyboardHeightChange`（`KRWindowInfo.ets` → `KRNativeManager.ets` → `KRKeyboardManager.cpp`），但主要服务输入组件回调。
- Compose 侧没有 `WindowInsets.ime` / `Modifier.imePadding()`；`Scaffold` 默认不把 IME 占用空间纳入内容避让。

## 2. phase1 设计目标（2026-06-10）

### 2.1 Goals

- 建立**页面级 IME 状态源**，把键盘底部占用空间作为窗口状态而非输入组件局部事件。
- 为 Compose DSL 新增 `WindowInsets.ime` 与 `Modifier.imePadding()`。
- 让 `Scaffold` 默认内容 inset 组合**系统栏 + IME**，提升常见输入页的开箱即用性。
- 在 Android / iOS / HarmonyOS 三端补齐页面级桥接路径。
- 保持**增量能力建设**：不破坏已有 `keyboardHeightChange` 业务写法。

### 2.2 Non-Goals

- 不实现 `BringIntoView`、焦点自动滚动、原生 `Scroller` 可见区域滚动协议。
- 不实现 `imeNestedScroll`、系统逐帧 IME 动画联动、拖拽列表时键盘跟手收起。
- 不把 `curve` / 动画插值能力暴露为 phase1 公共 API；动画质量优化延后到 phase3。
- 不追求 Web / miniApp / macOS 的等价实现。
- 不移除现有 `Modifier.keyboardHeightChange()` 或输入组件级事件。

## 3. phase1 关键 Decisions

### D1：IME 状态通过新 page-level pager event 进入 Compose

**选择**：新增一个页面级 pager event（建议命名 `keyboardInsetsDidChanged` / `imeInsetsDidChanged`），由各平台 renderer 在窗口键盘高度变化时发送；`ComposeContainer` 接收后更新 `Configuration` 内 IME 状态。

**原因**：

- `keyboardHeightChange` 是 View 级事件，页面没有输入框时无法感知 IME，无法支撑 `Scaffold` 等容器级消费。
- `ComposeContainer` 已通过 pager event 持有窗口尺寸、配置变化等页面状态；IME 属同一层级。
- 复用 pager event 可避免 `compose/` 依赖 render module，也避免业务侧额外注册 module callback。

**替代方案**：

- 方案 A：继续用 `Modifier.keyboardHeightChange()` 包装成 `WindowInsets.ime` —— 否，绑定输入组件，无法支撑容器。
- 方案 B：把 IME 高度塞进 `PageData` —— 否，`PageData` 属于设备/页面基础信息，会扩大 core 语义面。
- 方案 C：新增 page-level pager event + `Configuration` 状态（**采用**）—— 与现有窗口事件模型一致，跨层边界清晰。

### D2：Compose 侧用 `Configuration` 持有 IME 状态，`WindowInsets.ime` 作为只读投影

**选择**：在 `compose/platform/LocalConfiguration.kt` 中新增 IME 相关字段与更新方法：

- `imeBottomDp`
- `imeAnimationDuration`
- `imeAnimationCurve`（Phase 1 不对外承诺消费）
- `onImeInsetsChanged(height, duration, curve)`

`WindowInsets.Companion.ime` 不直接创建新跨层对象，而是从 `LocalConfiguration.current` 读取并映射为 `WindowInsets`。

**原因**：

- `Configuration` 已是 Compose 页面级动态配置统一入口（`safeAreaInsets` / `pageViewHeight` / `fontSizeScale` 都在这里更新）。
- `WindowInsets.ime` / `Scaffold` / 业务自定义布局共享同一份来源。
- 对外暴露仍为 `WindowInsets` 与 `Modifier`，业务不直接依赖底层状态结构。

**替代方案**：

- 方案 A：扩展 `WindowInfo` —— 否，`WindowInfo` 当前只承载焦点与容器像素尺寸，加入 IME 会扩大 UI 平台接口面，phase1 无强需求。
- 方案 B：新增独立 `ImeInsetsState` CompositionLocal —— 否，与 `LocalConfiguration` 职责高度重叠。
- 方案 C：放入 `Configuration`（**采用**）—— 与现有页面级状态模型最一致。

### D3：`WindowInsets.ime` 与 `Modifier.imePadding()` 作为 phase1 业务主入口

**选择**：

- `compose/foundation/layout/WindowInsets.kt` 新增 `WindowInsets.Companion.ime`。
- `compose/foundation/layout/WindowInsetsPadding.kt` 新增 `Modifier.imePadding()`。

`imePadding()` 复用现有 `windowInsetsPadding()` 与 inset 消费机制，不另起一套布局逻辑。

**原因**：

- 与官方 Compose 使用习惯一致，业务接入体验直接。
- 复用 `exclude / union / consumeWindowInsets` 语义，避免重复消费系统栏与键盘空间。
- Phase 1 的核心用户价值是"从手动监听改为声明式避让"，这两个 API 就是最小可用闭环。

**替代方案**：

- 方案 A：只暴露 `LocalConfiguration.current.imeBottomDp` —— 否，把业务带回手动算 padding 的旧路径。
- 方案 B：只改 `Scaffold` 默认行为，不暴露 `imePadding()` —— 否，无法覆盖不使用 `Scaffold` 的页面。
- 方案 C：同时提供 `WindowInsets.ime` 与 `imePadding()`（**采用**）—— 既有容器默认能力，又有显式组合能力。

### D4：`ScaffoldDefaults.contentWindowInsets` 改为组合系统栏与 IME

**选择**：保持 `systemBarsForVisualComponents` 仍表示安全区/系统栏；`ScaffoldDefaults.contentWindowInsets` 改为：

```
systemBarsForVisualComponents.union(WindowInsets.ime)
```

**原因**：

- IME 不是 system bars，直接改写 `systemBarsForVisualComponents` 会污染既有语义。
- `Scaffold` 的默认内容避让才是增强对象，而不是所有 `systemBarsForVisualComponents` 调用点。
- 这种设计风险可控：其他直接使用系统栏 inset 的调用点行为不变。

**替代方案**：

- 方案 A：让 `systemBarsForVisualComponents` 直接包含 IME —— 否，语义不准确，可能引发 `safeAreaInsets` 调用点行为偏差。
- 方案 B：只让业务手动传 `contentWindowInsets = WindowInsets.ime` —— 否，不能解决默认体验问题。
- 方案 C：只增强 `ScaffoldDefaults.contentWindowInsets`（**采用**）—— 兼顾默认行为和风险控制。

### D5：平台桥接采用"优先复用已有键盘源、补齐页面级挂载点"

**Android**：

- 复用 `core-render-android/.../KRKeyboardModule.kt` 现有 watcher。
- 新增页面级 listener 挂载点，优先放在 `KuiklyRenderView.kt` 或其 Compose 页面宿主链路。
- payload 至少包含 `height / duration`；`curve` 可选保留为 0。

**iOS**：

- 不再依赖 `KRTextFieldView.m` / `KRTextAreaView.m` 局部注册通知。
- 在 `core-render-ios/View/KuiklyRenderView.m` 页面宿主侧新增键盘通知 observer，统一发送 pager event。
- Phase 1 延续 `UIKeyboardWillShowNotification` / `UIKeyboardWillHideNotification` 粗粒度模型。

**HarmonyOS**：

- 复用已有窗口级来源：`KRWindowInfo.ets` 的 `window.on('keyboardHeightChange')`。
- 沿用 `KRNativeManager.ets` / `KRKeyboardManager.cpp` 全局窗口键盘通知链路。
- 新增 renderer → pager event 接线，让 Compose 页面直接获得 page-level IME 状态。
- `KRNativeRenderController.ets` 需在 `avoidAreaListener` 新增 `TYPE_KEYBOARD` 分支，从 `params.area.bottomRect.height` 取值并调用 `notifyImeInsetsChanged`；`KRWindowInfo.ets` 需新增 `getCurrentKeyboardHeight()` 通过 `getWindowAvoidArea(TYPE_KEYBOARD)` 实时获取，供 `KRNativeRenderController` 初始化和 `onPageShow` 补发使用。

**共同约束**：payload 最小集合 `height / duration / curve`；phase1 规范只要求 `height` 正确生效，`duration/curve` 作为向后兼容与 phase3 预留。

### D6：保留旧输入组件事件

现有 `InputView.keyboardHeightChange()`、`TextAreaView.keyboardHeightChange()`、Compose `Modifier.keyboardHeightChange()` 全部保留；phase1 新能力与旧能力并存。

**原因**：

- 现网业务可能已有手工键盘联动逻辑。
- Phase 1 目标是补齐声明式通道，不是强行迁移旧写法。
- 业务可渐进迁移：简单输入页优先切 `imePadding()`，复杂动画场景继续用旧事件。

## 4. phase1 计划文件变更

### 4.1 `core/`

- `core/.../Pager.kt`
  - 新增 IME page event 常量与 payload key 约定。
  - 维持事件统一分发语义。

### 4.2 `compose/`

- `compose/.../ComposeContainer.kt`：接收新的 IME pager event；调用 `Configuration.onImeInsetsChanged()`。
- `compose/.../platform/LocalConfiguration.kt`：新增 IME 状态字段与更新方法。
- `compose/.../foundation/layout/WindowInsets.kt`：新增 `WindowInsets.Companion.ime`。
- `compose/.../foundation/layout/WindowInsetsPadding.kt`：新增 `Modifier.imePadding()`。
- `compose/.../material3/Scaffold.kt`：调整默认 `contentWindowInsets` 组合策略。
- `compose/.../material3/internal/SystemBarsDefaultInsets.kt`（如有需要）：新增仅供 `Scaffold` 使用的组合 helper，避免污染 `systemBarsForVisualComponents` 语义。

### 4.3 `core-render-android/`

- `core-render-android/.../expand/module/KRKeyboardModule.kt`：复用 watcher，补充页面级监听接入说明。
- `core-render-android/.../KuiklyRenderView.kt`：新增 IME 事件常量与事件发送逻辑；生命周期内注册/释放页面级键盘 listener。

### 4.4 `core-render-ios/`

- `core-render-ios/View/KuiklyRenderView.m`：新增键盘通知 observer 注册/释放与 pager event 发送逻辑。
- `core-render-ios/View/KuiklyRenderView.h`：补充声明（如有需要）。

### 4.5 `core-render-ohos/`

- `core-render-ohos/src/main/ets/foundation/KRWindowInfo.ets`：继续作为窗口级 keyboardHeight 来源；新增 `getCurrentKeyboardHeight()`。
- `core-render-ohos/src/main/ets/KRNativeRenderController.ets`：新增 IME pager event 常量与发送逻辑；在 `avoidAreaListener` 新增 `TYPE_KEYBOARD` 分支。
- `core-render-ohos/src/main/ets/manager/KRNativeManager.ets`：复用现有键盘高度上报链路。
- `core-render-ohos/src/main/cpp/libohos_render/manager/KRKeyboardManager.*`：最小补充（如需支持 `curve` 或更明确分发）。

### 4.6 `demo/`

- 新增 `ImeInsetDemo.kt`（覆盖底部输入栏 + `Modifier.imePadding()`）。
- 新增 `ScaffoldImeInsetDemo.kt`（覆盖普通表单页 + `Scaffold` 默认避让）。

## 5. phase1 风险与缓解

- **iOS 仍用 `WillShow/WillHide`，无法覆盖浮动键盘或交互式 frame 变化**  
  缓解：phase1 只承诺"基础规避"，spec 中明确不等价于逐帧 IME animation；phase3 再评估 `keyboardWillChangeFrame`。
- **Android / OHOS 动画信息精度不一致**  
  缓解：phase1 只把 `height` 作为行为契约，`duration/curve` 作为兼容字段保留但不形成强承诺。
- **`Scaffold` 默认加入 IME 后，少量依赖旧行为的页面可能出现双重避让**  
  缓解：依赖现有 consumed insets 机制，并在 demo / 文档中强调若业务已手动处理键盘，应显式传入自定义 `contentWindowInsets`。
- **页面级 listener 生命周期不当可能造成泄漏或重复通知**  
  缓解：统一挂载在 `KuiklyRenderView` / `KRNativeRenderController` 等页面宿主对象上，销毁时成对移除 observer。
- **若把 IME 状态放进过多公共接口，会扩大后续兼容成本**  
  缓解：phase1 仅公开 `WindowInsets.ime` 与 `imePadding()`，其他元数据只内部存储。

## 6. 回退到 phase1 的设计（2026-07-27）

### 6.1 Why

phase1 落地后，仓库叠加了多轮 phase1 之外的能力（`compose-ime-animation-polish`、`compose-ime-linear-animation-mode`、`native-ime-animation-progress`、`ios-ime-sync-send-event`、`compose-ime-sync-curve-animation`），共同带来：

1. 协议层新增 `source / animatedHeight`。
2. Compose 配置层区分 raw target / projected inset / animation source。
3. `WindowInsets.ime` 内部做 `animateFloatAsState` / easing / target 动画投影。
4. iOS 引入 `native-progress` / DisplayLink / proxyView 等逐帧能力。
5. iOS / HarmonyOS / 部分宿主层把 `imeInsetsDidChanged` 作为同步发送特例。
6. Demo 文案与调试输出转向 phase3 语义。

这些变化使实现不再是"phase1 基座"，而是混入了多代尝试后的叠加态；继续推进会让回归与对比越来越困难。本 change 目标是先回退到 phase1 边界。

### 6.2 关键 Decisions（回退）

- **D1（回退）**：保留 phase1 公开能力，撤销 phase1 之外默认行为。  
  公开给业务的能力面继续保持为 `WindowInsets.ime` / `Modifier.imePadding()` / `Scaffold` 默认内容避让 / `imeInsetsDidChanged(height, duration, curve)` / 旧 `keyboardHeightChange` 兼容链路；默认行为收回到 phase1：只保证基础规避正确，不再承诺动画补间、逐帧进度、更高精度时序对齐。
- **D2（回退）**：IME page event 协议回退到 `height / duration / curve`；移除 `source` / `animatedHeight` / `fallback / native-progress` source 常量。
- **D3（回退）**：`LocalConfiguration` 回退到单一 IME 当前值模型；移除 `imeTargetBottomDp` / `imeAnimationSource` / raw / projected 双状态 / `normalizeImeAnimationSource()` / `onImeAnimationProgressChanged()`。
- **D4（回退）**：`WindowInsets.ime` 直接投影当前 IME 高度；撤销 `animateFloatAsState` / `tween(...)` / easing 映射 / target-driven 本地补间 / `native-progress` 优先消费 / `SideEffect` 驱动的投影值回写。
- **D5（回退）**：三端 renderer 保留 page-level 高度桥接，但不再为 IME 做 sync-send 特例。  
  - Android：移除 `source` 与 IME sync 特例。  
  - iOS：移除 `native-progress`、DisplayLink、proxyView、`animatedHeight/source` 发送、IME sync 特例依赖。  
  - HarmonyOS：移除 `source` 透传与 IME sync 特例。  
  - demo host：撤回 `imeInsetsDidChanged` 的同步发送声明。
- **D6（回退）**：Demo 回到 phase1 验证口径。`ImeInsetDemo` 与 `ScaffoldImeInsetDemo` 保留场景，但文案和调试展示只围绕 `imePadding()` 是否把底部输入栏抬离键盘、`Scaffold` 默认内容避让是否生效、旧 `keyboardHeightChange` 是否仍兼容；移除 `IME Sync Curve` / `Native Progress` / `phase3 提前映射` / `source / target / projected / animatedHeight` 调试展示。
- **D7（回退）**：OpenSpec 层将后续 IME 动画 change 视为 superseded / withdrawn 候选：`compose-ime-animation-polish`、`compose-ime-linear-animation-mode`、`native-ime-animation-progress`、`ios-ime-sync-send-event`、`compose-ime-sync-curve-animation`。

### 6.3 回退后的 Non-Goals

- 不重新定义 phase2 / phase3 路线图。
- 不引入新的动画能力、逐帧进度能力或更高精度的键盘联动。
- 不移除 phase1 公开业务入口。
- 不修改自研 DSL / Web / miniApp / macOS 键盘能力边界。

### 6.4 回退后的风险

- 回到 phase1 后，键盘弹出/收起的视觉过渡会重新变得更"硬"。  
  缓解：这是有意收口；当前目标是先恢复基线一致性，不是维持混合态动画优化。
- 已基于 `source / animatedHeight` 或 demo 调试字段排查问题的同学会失去这些辅助信息。  
  缓解：保留最小必要日志与 phase1 验证页，后续如需继续做动画路线，可在新的独立 change 中重建。
- 若不同时处理 OpenSpec 中的冲突 change，文档层会继续混乱。  
  缓解：将 superseded / withdrawn 处理纳入实施任务（已纳入本归档 §6.2 D7）。
- iOS 从复杂链路回退到 phase1 粗粒度模型后，某些边界场景（如交互式 frame 变化）支持度降低。  
  缓解：phase1 本就不承诺这类场景，spec 明确回到"基础规避"即可。

## 7. 跨端数据流（phase1 + 回退后最终状态）

```
┌──────────────────┐
│ 平台原生键盘事件  │   Android: KRKeyboardModule + page-level listener
│                  │   iOS:     UIKeyboardWillShow/Hide on KuiklyRenderView
│                  │   OHOS:    KRWindowInfo.ets keyboardHeightChange
└────────┬─────────┘
         │  height / duration / curve
         ↓
┌──────────────────┐
│ Page Event       │   imeInsetsDidChanged
│  PagerManager    │
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│ ComposeContainer │
│ onReceivePagerEvent
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│ LocalConfiguration
│ .imeBottomDp     │   ← 单一当前值，不做双状态 / 投影 / 补间
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│ WindowInsets.ime │   ← 直接投影 imeBottomDp
└────────┬─────────┘
         │
         ↓
┌──────────────────┐
│ Modifier.imePadding()
│ Scaffold default │
│ contentWindowInsets
└──────────────────┘
```

## 8. 后续 phase2 入口

在 phase1 页面级 IME 基座之上，下一步是 **输入框获焦且被键盘遮挡时，框架自动把目标滚入可视区**：

- 详细设计：`openspec/changes/compose-bring-into-view-phase2/`
- 关键能力：
  - 新增 `BringIntoViewRequester` 与 `Modifier.bringIntoViewRequester(...)`。
  - 输入组件获焦后自动请求进入可视区。
  - `ScrollState / verticalScroll` 与 `LazyListState / LazyColumn` 接入 responder。
  - 基于 `FocusedBounds` / `LayoutCoordinates` / `WindowInsets.ime` 的可视区判定闭环。
- 阶段定位：纯 Compose MVP，不引入 Native `Scroller` bridge / `imeNestedScroll` / 逐帧键盘动画联动。
