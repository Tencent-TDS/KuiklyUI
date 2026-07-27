## Context

### 背景与当前状态

`compose-ime-insets` 已经建立了 Compose DSL 的 page-level IME 基座，核心目标是：

- 让 `WindowInsets.ime` 成为页面级窗口状态投影
- 让 `Modifier.imePadding()` 成为声明式键盘避让入口
- 让 `Scaffold` 默认 `contentWindowInsets` 组合系统栏与 IME
- 让 Android、iOS、HarmonyOS 三端都具备页面级键盘高度桥接
- 保留旧 `keyboardHeightChange` 业务链路继续兼容

但随后仓库又叠加了多轮“超出 phase1”的建设，导致当前链路已经变成：

1. 协议层新增 `source` / `animatedHeight`
2. Compose 配置层区分 raw target / projected inset / animation source
3. `WindowInsets.ime` 内部做 `animateFloatAsState` / easing / target 动画投影
4. iOS 引入 `native-progress` / DisplayLink / proxyView 等逐帧能力
5. iOS / HarmonyOS / 部分宿主层把 `imeInsetsDidChanged` 作为同步发送特例
6. demo 文案与调试输出转向 phase3 语义

这些变化提高了动画表达力，但它们共同带来的问题也很明确：

- 当前实现不再是“phase1 基座”，而是混入了多代尝试后的叠加态
- 三端平台的精度和时序策略并不一致，导致设计目标持续漂移
- OpenSpec 中多个活跃 change 彼此覆盖，phase1 边界被持续侵蚀
- 若继续在这套混合态上推进，会让后续回归和对比越来越困难

因此本次设计选择**先回到 phase1**：保留已被证明有价值的 page-level IME 基座和声明式 API，去掉所有本不该提前沉入基线的动画治理层。

### 适用 DSL

- **适用**：Compose DSL（`compose/` 与其平台 renderer 桥接）
- **不适用**：自研 DSL（`core/` 下 `Pager` / `body()` 体系）

## Goals / Non-Goals

**Goals:**

- 让当前 IME 链路重新符合 `compose-ime-insets` 的 phase1 定义
- 保留 `WindowInsets.ime`、`Modifier.imePadding()`、`Scaffold` 默认避让、三端 page-level IME bridge
- 移除所有非 phase1 必需的动画消费、逐帧进度、source 协议、sync-send 依赖
- 给后续 phase2 / phase3 留下一条更干净、可重新评估的基线

**Non-Goals:**

- 不继续优化 IME 动画观感
- 不重做 `native-progress` 或 sync-send 方案的技术评估
- 不在本 change 中引入新的业务入口或新的跨层协议
- 不在本 change 中强推旧业务迁移

## Decisions

### D1：保留 phase1 公开能力，撤销 phase1 之外的默认行为

**选择**：公开给业务的能力面继续保持为：

- `WindowInsets.ime`
- `Modifier.imePadding()`
- `Scaffold` 默认内容避让
- page-level `imeInsetsDidChanged(height, duration, curve)`
- 旧 `keyboardHeightChange` 兼容链路

但默认行为收回到 phase1：只保证**基础规避正确**，不再承诺动画补间、逐帧进度、更高精度时序对齐。

**原因**：

- 这部分是已经形成稳定用户价值的最小闭环
- 继续保留它们可以避免把已完成的 phase1 用户价值一起推翻
- 这样回滚后，业务不会失去声明式 IME 入口，只是失去超出 phase1 的增强行为

### D2：IME page event 协议回退到 `height / duration / curve`

**选择**：

- 保留 `height`
- 保留 `duration`、`curve` 作为 phase1 允许存在的内部元信息
- 移除 `source`
- 移除 `animatedHeight`
- 移除 `fallback/native-progress` source 常量及其相关兼容分支

**原因**：

- `compose-ime-insets` 的 phase1 文档已经明确：`duration / curve` 只是内部预留，不应成为动画主驱动
- `source` 与 `animatedHeight` 属于后续动画治理为了解决多来源、多驱动模式而引入的扩展协议
- 回退到更小的协议面，才能让三端重新对齐到同一基线

### D3：`LocalConfiguration` 回退到单一 IME 当前值模型

**选择**：`LocalConfiguration` 中仅保留 phase1 必需的 IME 状态，例如：

- `imeBottomDp`
- 可选保留内部 `imeAnimationDuration` / `imeAnimationCurve` 存储

移除：

- `imeTargetBottomDp`
- `imeAnimationSource`
- raw / projected 双状态
- `normalizeImeAnimationSource()`
- `onImeAnimationProgressChanged()`
- 基于 source 的分支处理

**原因**：

- phase1 的 `WindowInsets.ime` 只需要一个可直接投影的页面级高度值
- 双状态模型本质上是为动画投影服务，不属于 phase1 基线
- 移除 source 分支后，配置层职责会重新变得单纯且易于验证

### D4：`WindowInsets.ime` 直接投影当前 IME 高度，不做内部补动画

**选择**：`WindowInsets.Companion.ime` 回退为直接读取 `LocalConfiguration.current` 的当前 IME 底部高度。

明确撤销：

- `animateFloatAsState`
- `tween(...)`
- easing 映射
- target-driven 本地补间
- `native-progress` 优先消费
- `SideEffect` 驱动的投影值回写

**原因**：

- phase1 只承诺“状态正确优先”的页面级避让，不承诺动画质量
- 内部补动画会把 `duration / curve` 从预留字段变成默认行为，偏离原始设计
- 去掉动画投影后，`imePadding()` 与 `Scaffold` 会回到最直接、最可解释的消费语义

### D5：三端 renderer 保留 page-level 高度桥接，但不再为 IME 做 sync-send 特例

**选择**：

- Android：保留 page-level 键盘高度监听与 `imeInsetsDidChanged` 发送，但移除 `source` 与 IME sync 特例
- iOS：保留页面宿主键盘通知与 page event 发送，但移除 `native-progress`、DisplayLink、proxyView、`animatedHeight/source` 以及 IME sync 特例
- HarmonyOS：保留窗口级 keyboardHeight → page event 链路，但移除 `source` 与 IME sync 特例
- demo host：撤回 `imeInsetsDidChanged` 的同步发送声明

**原因**：

- phase1 的桥接重点是“页面级高度状态能到 Compose”，而不是“事件必须同步逐帧送达”
- sync-send 特例与 native-progress 都属于后续治理层，不应成为基线成立条件
- 去掉这些特例后，平台差异会明显减少，验收口径也更单纯

### D6：Demo 回到 phase1 验证口径

**选择**：保留 `KeyboardHeightDemo` 与 `ScaffoldDemo` 两个场景，但文案和调试展示只围绕：

- `imePadding()` 是否把底部输入栏抬离键盘
- `Scaffold` 默认内容避让是否生效
- 旧 `keyboardHeightChange` 是否仍兼容

移除：

- `IME Sync Curve`
- `Native Progress`
- `phase3 提前映射`
- `source / target / projected / animatedHeight` 调试展示

**原因**：

- demo 的职责是定义验收口径，而不是延续试验性语义
- 如果 demo 继续暴露 phase3 字段，会误导后续实现继续朝增强方向膨胀

### D7：OpenSpec 层将后续 IME 动画 change 视为 superseded 候选

**选择**：本 change 不直接删除其他 change，但在实施时应评估以下 change 的处理方式：

- `compose-ime-animation-polish`
- `compose-ime-linear-animation-mode`
- `native-ime-animation-progress`
- `ios-ime-sync-send-event`
- `compose-ime-sync-curve-animation`

它们应被明确标记为 superseded / withdrawn / archived，至少不再作为“当前要继续推进的基线方向”。

**原因**：

- 否则 OpenSpec 会同时保留多个互相冲突的 IME 目标状态
- 草案的价值不仅是描述代码回滚，也要把文档语义重新收口

## Planned File Changes by Module

### `core/`

- `core/.../Pager.kt`
  - 回退 IME payload key 到 `height / duration / curve`
  - 移除 `source` / `animatedHeight` 常量

### `compose/`

- `compose/.../ComposeContainer.kt`
  - 只解析 phase1 payload
  - 移除 `source` / `animatedHeight` 相关处理
- `compose/.../platform/LocalConfiguration.kt`
  - 回退到单一 IME 当前值状态模型
- `compose/.../foundation/layout/WindowInsets.kt`
  - 回退 `WindowInsets.ime` 到直接投影当前高度
- `compose/.../foundation/layout/WindowInsetsPadding.kt`
  - 仅确认 `imePadding()` 保持 phase1 消费语义，无需新增变更
- `compose/.../material3/Scaffold.kt`
  - 仅确认默认 `contentWindowInsets` 仍组合 system bars + IME，无需撤销 phase1 基线

### `core-render-android/`

- `core-render-android/.../KuiklyRenderView.kt`
  - 保留 page-level keyboard event
  - 移除 `source` 透传与 IME sync 特例

### `core-render-ios/`

- `core-render-ios/View/KuiklyRenderView.m`
  - 保留页面宿主键盘通知 → page event
  - 移除 `native-progress` 逐帧链路、DisplayLink、proxyView、`source / animatedHeight` 发送、IME sync 特例依赖
- `core-render-ios/View/KuiklyRenderView.h`
  - 如包含相关声明，则同步收口
- `core-render-ios/Extension/...`
  - 若文档或示例中把 IME 当作 sync-send 典型场景，需同步回退

### `core-render-ohos/`

- `core-render-ohos/.../KRNativeRenderController.ets`
  - 保留 page-level IME 事件桥接
  - 移除 `source` 透传和 IME sync 特例
  - `avoidAreaListener` 新增 `TYPE_KEYBOARD` 分支：键盘弹起/收起时从 `params.area.bottomRect.height` 取值并调用 `notifyImeInsetsChanged`（此前遗漏导致 `WindowInsets.ime` 恒为 0）
- `core-render-ohos/.../foundation/KRWindowInfo.ets`
  - 新增 `getCurrentKeyboardHeight()` 方法：通过 `getWindowAvoidArea(TYPE_KEYBOARD)` 实时获取当前键盘高度，供 `KRNativeRenderController` 初始化和 `onPageShow` 补发使用（此前方法不存在导致编译失败 + 回切后状态丢失）
- `ohosApp/.../KuiklyViewDelegate.ets`
  - 回退 demo host 中 `imeInsetsDidChanged` 的同步声明

### `iosApp/`

- `iosApp/.../KuiklyRenderViewController.m`
  - 回退 demo host 中 `imeInsetsDidChanged` 的同步声明

### `demo/`

- `demo/.../KeyboardHeightDemo.kt`
  - 回退到 phase1 文案与展示
- `demo/.../ScaffoldDemo.kt`
  - 回退到 phase1 文案与展示
  - 布局调整为 `Box(weight=1f, BottomCenter)` 包裹三个 TextField，避免 `Spacer(weight=1f)` 在小屏设备上把表单推出可视区
- `demo/.../ImeCompareDemo.kt`（新增）
  - Tab 对比 demo：Tab1 用 `keyboardHeightChange` 旧方式避让，Tab2 用 `WindowInsets.ime` + `imePadding` 新方式避让，用于验证新方式是否等价替换旧方式

### `androidApp/`

- `androidApp/.../OfficialComposeCompareActivity.kt`（新增）
  - 官方 Jetpack Compose 对照 demo，含 `imePadding` 和 `Scaffold` 两个 Tab，用于与 Kuikly Compose 的 `KeyboardHeightDemo` / `ScaffoldDemo` 做体感对比

### `openspec/`

- 为本次回滚新增 change 草案
- 后续在真正实施时，处理与其冲突的 IME animation / sync change 状态

## Risks / Trade-offs

- **[风险]** 回到 phase1 后，键盘弹出/收起的视觉过渡会重新变得更“硬”  
  **缓解**：这是有意收口；当前目标是先恢复基线一致性，而不是继续维持混合态动画优化。

- **[风险]** 已基于 `source / animatedHeight` 或 demo 调试字段排查问题的同学会失去这些辅助信息  
  **缓解**：保留最小必要日志与 phase1 验证页，后续如需继续做动画路线，可在新的独立 change 中重建。

- **[风险]** 若不同时处理 OpenSpec 中的冲突 change，文档层会继续混乱  
  **缓解**：将 superseded / withdrawn 处理纳入实施任务。

- **[风险]** iOS 从复杂链路回退到 phase1 粗粒度模型后，某些边界场景（如交互式 frame 变化）支持度降低  
  **缓解**：phase1 本就不承诺这类场景，spec 中明确回到“基础规避”即可。

## Migration Plan

1. 先以本草案定义“只保留 phase1”的明确边界与验收口径。
2. 再按模块拆分实施回滚：协议层 → Compose 状态层 → renderer → demo。
3. 在实施收口后，统一处理冲突的 IME animation / sync changes。
4. 最后重新做三端 phase1 验证：输入栏避让、`Scaffold` 避让、旧 callback 兼容。

## Open Questions

- 在实施时，`duration / curve` 是否保留在 `LocalConfiguration` 作为纯内部预留字段，还是进一步一起裁掉？当前建议：**保留存储，但不参与默认行为**。
- iOS 是否严格回到 `WillShow / WillHide`，还是允许保留 `WillChangeFrame` 但只发送 phase1 payload？当前建议：**允许保留更稳定的宿主监听方式，但对 Compose 侧只输出 phase1 语义**。
- 对已存在的后续 IME change，是采用 `withdrawn` 还是 `superseded by compose-ime-return-to-phase1` 更清晰？当前建议：**用 superseded 更利于追溯演进历史**。
