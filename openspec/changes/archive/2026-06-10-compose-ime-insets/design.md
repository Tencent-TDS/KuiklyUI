## Context

### 背景与当前状态

本次改动**只适用于 Compose DSL**。Kuikly 当前已经具备以下基础：

- `compose/foundation/layout/WindowInsets.kt` 已实现 `WindowInsets`、`union`、`exclude`、`asPaddingValues` 等基础能力。
- `compose/foundation/layout/WindowInsetsPadding.kt` 已实现 inset 消费与向下游传播的语义。
- `compose/material3/Scaffold.kt` 已支持 `contentWindowInsets`，默认值来自 `systemBarsForVisualComponents`。
- `compose/platform/LocalConfiguration.kt` 已持有页面宽高、`safeAreaInsets`、字体缩放等页面级状态，并通过 `mutableStateOf` 驱动 Compose 更新。
- `ComposeContainer.onReceivePagerEvent()` 已接收 `rootViewSizeDidChanged`、`windowSizeDidChanged`、`configurationDidChanged` 三类页面级事件。

当前缺口也很明确：

- 现有 `keyboardHeightChange` 仍是**输入组件事件**，入口在 `InputView` / `TextAreaView` 与 Compose 的 `Modifier.keyboardHeightChange()`。
- Android 键盘高度来自 `KRKeyboardModule`，但当前主要由 `KRTextFieldView.kt` 局部订阅。
- iOS 键盘事件目前挂在 `KRTextFieldView.m` / `KRTextAreaView.m` 的 `UIKeyboardWillShow/Hide` 通知上。
- HarmonyOS 虽然已有窗口级 `keyboardHeightChange`（`KRWindowInfo.ets` → `KRNativeManager.ets` → `KRKeyboardManager.cpp`），但当前仍主要服务输入组件回调。
- Compose 侧没有 `WindowInsets.ime`、没有 `Modifier.imePadding()`，`Scaffold` 默认也不会自动把 IME 占用空间纳入内容避让。

这意味着：当前键盘避让只能依赖业务手写监听与手动位移，无法形成页面级、声明式、可复用的 Compose API。

### 适用 DSL

- **适用**：Compose DSL（`compose/` 模块与其平台侧 renderer 桥接）
- **不适用**：自研 DSL（`core/` 下 `Pager` / `body()` 体系）

### NativeBridge / 跨层通信现状

本次设计不会新增业务可见的 Native module API，而是复用现有**页面事件通道**：

1. 平台 renderer 监听窗口级键盘高度变化。
2. renderer 通过现有 `sendEvent` / `sendWithEvent` / `render.sendEvent` 将事件发送给 Kotlin Pager。
3. `PagerManager` 将事件路由给 `Pager.onReceivePagerEvent()`。
4. `ComposeContainer.onReceivePagerEvent()` 更新 Compose 页面的 IME 状态。
5. `WindowInsets.Companion.ime` 与 `Modifier.imePadding()` 消费该状态。

这条链路的优点是：

- 不让 `compose/` 直接依赖 `core-render-*`
- 不把 IME 能力绑死在输入组件上
- 与现有 `root/window/configuration` 事件模式一致，便于统一维护

## Goals / Non-Goals

**Goals:**

- 建立**页面级 IME 状态源**，把键盘底部占用空间作为窗口状态而不是输入组件局部事件。
- 为 Compose DSL 新增 `WindowInsets.ime` 与 `Modifier.imePadding()`。
- 让 `Scaffold` 默认内容 inset 能够组合**系统栏 + IME**，提升常见输入页的开箱即用性。
- 在 Android、iOS、HarmonyOS 三端补齐对应的页面级桥接路径。
- 保持本阶段为**增量能力建设**：不破坏已有 `keyboardHeightChange` 业务写法。

**Non-Goals:**

- 不实现 `BringIntoView`、焦点自动滚动、Native `Scroller` 的可见区域滚动协议。
- 不实现 `imeNestedScroll`、系统逐帧 IME 动画联动、拖拽列表时键盘跟手收起。
- 不把 `curve` / 动画插值能力暴露为 Compose Phase 1 公共 API；动画质量优化延后到 Phase 3。
- 不追求 Web、miniApp、macOS 的等价实现。
- 不移除现有 `Modifier.keyboardHeightChange()` 或输入组件级事件。

## Decisions

### D1：IME 状态通过新的 page-level pager event 进入 Compose，而不是继续复用输入组件事件

**选择**：新增一个页面级 pager event（建议命名为 `keyboardInsetsDidChanged` 或 `imeInsetsDidChanged`），由各平台 renderer 在窗口键盘高度变化时发送；`ComposeContainer` 接收后更新 `Configuration` 内的 IME 状态。

**原因**：

- `keyboardHeightChange` 当前是 View 级事件，只在输入组件挂载后才有意义，不符合 `WindowInsets` 的窗口级语义。
- `ComposeContainer` 已经通过 pager event 持有窗口尺寸、配置变化等页面状态；IME 属于同一层级的问题。
- 复用 pager event 可以避免 `compose/` 依赖 render module，也避免在业务侧额外注册 module callback。

**替代方案对比**：

- **方案 A：继续使用 `Modifier.keyboardHeightChange()` 包装成 `WindowInsets.ime`**
  - 否。事件绑定在输入组件上，页面中没有输入框时无法感知 IME 状态，也无法支撑 `Scaffold` 这类容器级消费。
- **方案 B：把 IME 高度直接塞进 `PageData`**
  - 否。`PageData` 以设备/页面基础信息为主，IME 属于瞬时窗口状态；塞入 `PageData` 会扩大 core 语义面，且与 Compose 层直接消费并不完全贴合。
- **方案 C：新增 page-level pager event + `Configuration` 状态（采用）**
  - 是。与现有窗口事件模型一致，跨层边界清晰。

### D2：Compose 侧用 `Configuration` 持有 IME 状态，`WindowInsets.ime` 作为其只读投影

**选择**：在 `compose/platform/LocalConfiguration.kt` 中新增 IME 相关状态字段与更新方法，例如：

- `imeBottomDp`
- `imeAnimationDuration`
- `imeAnimationCurve`（先存储，Phase 1 不对外承诺消费）
- `onImeInsetsChanged(height, duration, curve)`

`WindowInsets.Companion.ime` 不直接创建新的跨层对象，而是从 `LocalConfiguration.current` 读取状态并映射为 `WindowInsets`。

**原因**：

- `Configuration` 已经是 Compose 页面级动态配置的统一入口，现有 `safeAreaInsets` / `pageViewHeight` / `fontSizeScale` 都在这里更新。
- 把 IME 状态保持在 `Configuration`，可以让 `WindowInsets.ime`、`Scaffold`、业务自定义布局共享同一份来源。
- 对外暴露的依旧是 `WindowInsets` 与 `Modifier`，不会让业务直接依赖底层状态结构。

**替代方案对比**：

- **方案 A：扩展 `WindowInfo`**
  - 暂不采用。Kuikly 当前 `WindowInfo` 只承载焦点与容器像素尺寸，加入 IME 会扩大 UI 平台接口面，但 Phase 1 暂无直接消费者必须依赖它。
- **方案 B：新增独立 `ImeInsetsState` CompositionLocal**
  - 暂不采用。可行，但与现有 `LocalConfiguration` 职责高度重叠，会增加维护入口。
- **方案 C：放入 `Configuration`（采用）**
  - 与现有页面级状态模型最一致。

### D3：`WindowInsets.ime` 与 `Modifier.imePadding()` 作为 Phase 1 的业务主入口

**选择**：

- 在 `compose/foundation/layout/WindowInsets.kt` 中新增 `WindowInsets.Companion.ime`
- 在 `compose/foundation/layout/WindowInsetsPadding.kt` 中新增 `Modifier.imePadding()`

`imePadding()` 复用现有 `windowInsetsPadding()` 与 inset 消费机制，不另起一套布局逻辑。

**原因**：

- 这与官方 Compose 的使用习惯一致，能直接提升业务接入体验。
- 复用现有 `exclude/union/consumeWindowInsets` 语义，可避免重复消费系统栏与键盘空间。
- Phase 1 的主要用户价值是“从手动监听改为声明式避让”，这两个 API 就是最小可用闭环。

**替代方案对比**：

- **方案 A：只暴露 `LocalConfiguration.current.imeBottomDp`**
  - 否。会把业务重新带回手动算 padding 的路径，失去 Compose insets 体系的统一性。
- **方案 B：只改 `Scaffold` 默认行为，不暴露 `imePadding()`**
  - 否。无法覆盖不使用 `Scaffold` 的页面。
- **方案 C：同时提供 `WindowInsets.ime` 与 `imePadding()`（采用）**
  - 既有容器默认能力，又有显式组合能力。

### D4：`ScaffoldDefaults.contentWindowInsets` 改为组合系统栏与 IME，而不是修改 `systemBarsForVisualComponents` 语义

**选择**：保持 `systemBarsForVisualComponents` 仍表示安全区/系统栏；`ScaffoldDefaults.contentWindowInsets` 改为更接近：

- `systemBarsForVisualComponents.union(WindowInsets.ime)`

**原因**：

- IME 不是 system bars，直接改写 `systemBarsForVisualComponents` 会污染既有语义。
- `Scaffold` 的默认内容避让才是我们要增强的对象，而不是所有调用 `systemBarsForVisualComponents` 的地方。
- 这种设计更容易控制风险：其他直接使用系统栏 inset 的调用点行为不变。

**替代方案对比**：

- **方案 A：让 `systemBarsForVisualComponents` 直接包含 IME**
  - 否。语义不准确，可能引发与现有 `safeAreaInsets` 调用点的行为偏差。
- **方案 B：只让业务手动传 `contentWindowInsets = WindowInsets.ime`**
  - 否。不能解决默认体验问题。
- **方案 C：只增强 `ScaffoldDefaults.contentWindowInsets`（采用）**
  - 兼顾默认行为和风险控制。

### D5：平台桥接采用“优先复用已有键盘源、补齐页面级挂载点”的策略

**Android**

- 复用 `core-render-android/.../KRKeyboardModule.kt` 现有 watcher。
- 新增页面级 listener 挂载点，优先放在 `KuiklyRenderView.kt` 或其 Compose 页面宿主链路，而不是 `KRTextFieldView.kt`。
- renderer 在高度变化时发送新的 pager event，payload 至少包含：`height`、`duration`；`curve` 可选保留为 0。

**iOS**

- 不再依赖 `KRTextFieldView.m` / `KRTextAreaView.m` 局部注册通知作为 Compose IME 主来源。
- 在 `core-render-ios/View/KuiklyRenderView.m` 所在页面宿主侧新增键盘通知观察者，统一发送 pager event。
- Phase 1 延续 `UIKeyboardWillShowNotification` / `UIKeyboardWillHideNotification` 的粗粒度模型；未来若做 Phase 3，可再评估切到 `keyboardWillChangeFrame`。

**HarmonyOS**

- 复用已有窗口级来源：`KRWindowInfo.ets` 的 `window.on('keyboardHeightChange')`。
- 继续沿用 `KRNativeManager.ets` / `KRKeyboardManager.cpp` 的全局窗口键盘通知链路。
- 新增 renderer → pager event 的接线，使 Compose 页面能够直接获得 page-level IME 状态，而不是仅供输入组件回调消费。

**共同约束**：payload 最小集合为 `height`、`duration`、`curve`；Phase 1 规范只要求 `height` 正确生效，`duration/curve` 以向后兼容和为 Phase 3 预留为主。

### D6：保留旧输入组件事件，避免破坏已有业务场景

**选择**：现有 `InputView.keyboardHeightChange()`、`TextAreaView.keyboardHeightChange()`、Compose `Modifier.keyboardHeightChange()` 全部保留；Phase 1 新能力与其并存。

**原因**：

- 现网业务可能已有手工键盘联动逻辑。
- Phase 1 的目标是补齐声明式通道，而不是强行迁移所有旧写法。
- 业务可以渐进迁移：简单输入页优先切 `imePadding()`，复杂动画场景继续使用旧事件直到 Phase 3 成熟。

## Planned File Changes by Module

### `core/`

- `core/.../Pager.kt`
  - 新增 IME page event 常量
  - 维持对事件的统一分发语义，必要时补注释与 payload key 常量

### `compose/`

- `compose/.../ComposeContainer.kt`
  - 接收新的 IME pager event
  - 调用 `Configuration.onImeInsetsChanged()` 更新页面状态
- `compose/.../platform/LocalConfiguration.kt`
  - 新增 IME 状态字段与更新方法
- `compose/.../foundation/layout/WindowInsets.kt`
  - 新增 `WindowInsets.Companion.ime`
- `compose/.../foundation/layout/WindowInsetsPadding.kt`
  - 新增 `Modifier.imePadding()`
- `compose/.../material3/Scaffold.kt`
  - 调整默认 `contentWindowInsets` 组合策略
- `compose/.../material3/internal/SystemBarsDefaultInsets.kt`
  - 如有需要，新增仅供 `Scaffold` 使用的组合 helper，避免污染原有 `systemBarsForVisualComponents` 语义

### `core-render-android/`

- `core-render-android/.../expand/module/KRKeyboardModule.kt`
  - 复用 watcher，补充页面级监听接入说明
- `core-render-android/.../KuiklyRenderView.kt`
  - 新增 IME 事件常量与事件发送逻辑
  - 生命周期内注册/释放页面级键盘 listener
- 如需最小化耦合，可新增一个 Android 侧 pager keyboard dispatcher 辅助类

### `core-render-ios/`

- `core-render-ios/View/KuiklyRenderView.m`
  - 新增键盘通知 observer 的注册/释放与 pager event 发送逻辑
- `core-render-ios/View/KuiklyRenderView.h`
  - 如需对外声明常量或生命周期接口，补充声明
- 如需降低 `KuiklyRenderView.m` 复杂度，可新增轻量 keyboard observer/helper 文件

### `core-render-ohos/`

- `core-render-ohos/src/main/ets/foundation/KRWindowInfo.ets`
  - 继续作为窗口级 keyboardHeight 来源
- `core-render-ohos/src/main/ets/KRNativeRenderController.ets`
  - 新增 IME pager event 常量与发送逻辑
- `core-render-ohos/src/main/ets/manager/KRNativeManager.ets`
  - 复用现有键盘高度上报链路，必要时补注释/透传字段
- `core-render-ohos/src/main/cpp/libohos_render/manager/KRKeyboardManager.*`
  - 如需支持额外 payload（如 curve）或更明确的页面级分发，可做最小补充

### `demo/`

- 新增或更新 Compose demo 页面，至少覆盖：
  - 底部输入栏 + `imePadding()`
  - 普通表单页 + `Scaffold` 默认避让

## Risks / Trade-offs

- **[风险] iOS 仍使用 `WillShow/WillHide`，无法覆盖浮动键盘或交互式 frame 变化** → **缓解**：Phase 1 只承诺“基础规避”，在 spec 中明确不等价于逐帧 IME animation；Phase 3 再评估 `keyboardWillChangeFrame`。
- **[风险] Android / OHOS 动画信息精度不一致** → **缓解**：Phase 1 只把 `height` 作为行为契约，`duration/curve` 作为兼容字段保留但不形成强承诺。
- **[风险] `Scaffold` 默认加入 IME 后，少量依赖旧行为的页面可能出现双重避让** → **缓解**：依赖现有 consumed insets 机制，并在 demo / 文档中强调若业务已手动处理键盘，应显式传入自定义 `contentWindowInsets`。
- **[风险] 页面级 listener 生命周期处理不当可能造成泄漏或重复通知** → **缓解**：统一挂载在 `KuiklyRenderView` / `KRNativeRenderController` 等页面宿主对象上，在销毁时成对移除 observer。
- **[风险] 若把 IME 状态放进过多公共接口，会扩大后续兼容成本** → **缓解**：Phase 1 仅公开 `WindowInsets.ime` 与 `imePadding()`，其他元数据只在内部存储。

## Migration Plan

1. 先在三端 renderer 增加 IME pager event 的发送能力，但不改业务 API。
2. Compose 侧接入 `Configuration` 状态与 `WindowInsets.ime`，确保新增能力是纯增量。
3. 新增 `Modifier.imePadding()`，并调整 `ScaffoldDefaults.contentWindowInsets`。
4. 通过 demo 验证两类主场景：底部输入栏、普通表单页。
5. 若出现回归，可先回退 `ScaffoldDefaults.contentWindowInsets` 的默认组合策略，同时保留 `WindowInsets.ime` / `imePadding()` 供业务显式启用。

## Open Questions

- 是否需要在 Phase 1 就把 `curve` 透传到 Compose 公共 API，还是只在内部保存等待 Phase 3 使用？当前建议：**内部保存，不对外承诺**。
- Android 页面级 listener 最合适的挂载点是否直接放在 `KuiklyRenderView.kt`，还是拆出独立 dispatcher 以降低视图类体积？当前建议：**先在宿主层接通，若代码膨胀再抽辅助类**。
- `ScaffoldDefaults.contentWindowInsets` 是否需要立即覆盖所有 Material3 页面，还是先仅增强 Compose demo 验证后再放开？当前建议：**按默认语义直接增强，但在任务中加入回归验证**。
