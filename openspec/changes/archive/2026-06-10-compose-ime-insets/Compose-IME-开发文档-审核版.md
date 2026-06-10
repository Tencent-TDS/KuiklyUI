# Compose IME Insets 能力开发文档（审核版）

> 适用范围：Compose DSL（`compose/` 模块及其三端 renderer 桥接）
> 目标平台：Android、iOS、HarmonyOS
> 不涉及：自研 DSL、Web / 小程序 / macOS

---

## 第一部分：需求范围与能力差距

### 1.1 整体需求目标（Phase 1 ~ Phase 3）

本次需求为 Kuikly Compose DSL 构建完整的 **IME（软键盘）inset 声明式消费链路**，覆盖从基础状态通道到动画联动的全能力栈，最终使输入页能够以与官方 Compose 等价的方式完成键盘规避。

| Phase | 核心目标 | 关键交付物 |
|-------|----------|------------|
| Phase 1 | 建立页面级 IME 状态通道 | `WindowInsets.ime`、`Modifier.imePadding()`、`Scaffold` 默认避让、三端桥接 |
| Phase 2 | 聚焦自动滚动到可视区域 | `BringIntoViewRequester`、Native Scroller 可见区域协议 |
| Phase 3 | 键盘动画逐帧联动与曲线优化 | `duration/curve` 公共 API、`imeNestedScroll` 等价能力、拖拽列表跟手收起 |

### 1.2 与官方 Compose 的能力对比

| 能力项 | 官方 Compose（基线） | Kuikly 当前状态 | Phase 规划 |
|--------|----------------------|-----------------|------------|
| `WindowInsets.ime` | ✅ 系统级 IME inset 源 | ❌ 无页面级 IME 状态 | Phase 1 |
| `Modifier.imePadding()` | ✅ 声明式键盘避让 padding | ❌ 无此 Modifier | Phase 1 |
| `Scaffold` 默认键盘避让 | ✅ `contentWindowInsets` 组合系统栏 + IME | ❌ 仅含系统栏，不含 IME | Phase 1 |
| `BringIntoViewRequester` | ✅ 聚焦自动滚动到可视区 | ❌ 未实现 | Phase 2 |
| `imeNestedScroll` | ✅ 逐帧 IME 动画联动 | ❌ 未实现 | Phase 3 |
| 动画参数 `duration/curve` 公共 API | ✅ 可作为布局动画输入 | ❌ 内部预留，未对外暴露 | Phase 3 |
| `WindowInsets` 基础骨架 | ✅ `union` / `exclude` / `consume` 等 | ✅ 已实现 | 已有 |
| `keyboardHeightChange()` | 组件级回调（非官方标配） | ✅ 输入组件已支持 | 已有 |

### 1.3 差距总结

| 差距维度 | 现状问题 | 期望状态 |
|----------|----------|----------|
| 状态源层级 | 键盘高度只有**输入组件级**回调，页面无统一状态 | 页面级 `WindowInsets.ime`，任何布局均可消费 |
| 声明式能力 | 业务只能手写监听 + 手动算 padding | `Modifier.imePadding()` 声明式消费 |
| 容器默认行为 | `Scaffold` 不会自动避让键盘 | `Scaffold` 默认组合系统栏 + IME inset |
| 聚焦体验 | 输入框被键盘遮挡时不会自动滚动 | 聚焦后自动滚动至可视区域（Phase 2） |
| 动画质量 | 布局变化与键盘动画脱节，可能跳动 | 逐帧联动，边缘贴合（Phase 3） |

---

## 第二部分：可建设项梳理

### 2.1 欠缺且 Kuikly 能做到的部分

以下所有建设项在技术上均可落地，按 Phase 拆分如下：

#### Phase 1 可建设项

| 建设项 | 技术可行性 | 依赖条件 | 实现要点 |
|--------|-----------|----------|----------|
| 页面级 IME 事件通道 | ✅ 高 | 复用现有 pager event 分发链路 | 新增 `imeInsetsDidChanged` 事件常量，payload 含 `height/duration/curve` |
| `Configuration` 存储 IME 状态 | ✅ 高 | `LocalConfiguration` 已持有页面级状态 | 新增 `imeBottomDp`、`imeAnimationDuration`、`imeAnimationCurve` 字段 |
| `WindowInsets.Companion.ime` | ✅ 高 | 依赖上述状态源 | 从 `LocalConfiguration.current` 读取并映射为 `WindowInsets` 只读投影 |
| `Modifier.imePadding()` | ✅ 高 | 复用现有 `windowInsetsPadding()` 机制 | 消费 `WindowInsets.ime` 的 bottom inset，参与现有 consume 链 |
| `Scaffold` 默认组合 IME | ✅ 高 | 依赖 `WindowInsets.ime` | `ScaffoldDefaults.contentWindowInsets = systemBarsForVisualComponents.union(ime)` |
| Android 桥接 | ✅ 高 | 复用 `KRKeyboardModule` 现有 watcher | 在 `KuiklyRenderView.kt` 补充页面级 listener 挂载点 |
| iOS 桥接 | ✅ 高 | 页面宿主层注册键盘通知 | `KuiklyRenderView.m` 监听 `UIKeyboardWillShow/HideNotification` 发送 pager event |
| HarmonyOS 桥接 | ✅ 高 | 复用 `KRWindowInfo` 窗口级来源 | `KRNativeRenderController.ets` 把 `keyboardHeightChange` 接到 pager event 发送 |

#### Phase 2 可建设项

| 建设项 | 技术可行性 | 依赖条件 | 实现要点 |
|--------|-----------|----------|----------|
| `BringIntoViewRequester` 基础机制 | ✅ 中 | Phase 1 IME 状态稳定 | 在 Compose 焦点系统中拦截焦点变化，检测组件是否被 IME 遮挡，触发滚动请求 |
| 父布局滚动响应 | ✅ 中 | 依赖 BringIntoView 请求通道 | `Column` / `LazyColumn` 等容器响应 BringIntoView，计算目标偏移并执行滚动 |
| Native Scroller 可见区域协议 | ✅ 中 | 需打通 Compose → Native 滚动指令 | 当 Compose 侧无法独立完成滚动时，通过 bridge 调用 Native `Scroller` 的滚动 API |

#### Phase 3 可建设项

| 建设项 | 技术可行性 | 依赖条件 | 实现要点 |
|--------|-----------|----------|----------|
| `duration/curve` 公共 API 暴露 | ✅ 高 | Phase 1 已内部存储 | 将 `Configuration` 中的 `imeAnimationDuration`、`imeAnimationCurve` 提升为公共可读属性 |
| 逐帧 IME 动画联动 | ✅ 中 | 需平台侧支持逐帧回调 | Android：监听 `WindowInsetsAnimation`；iOS：切到 `keyboardWillChangeFrame`；HarmonyOS：评估窗口动画回调 |
| 布局与键盘边缘逐帧贴合 | ✅ 中 | 依赖逐帧 IME 高度输入 | Compose 侧使用 `Animatable` 或自定义 `AnimationSpec`，以 `duration/curve` 为输入驱动布局变化 |
| 拖拽列表时键盘跟手收起 | ✅ 中 | 依赖手势系统与 IME 状态联动 | `LazyColumn` 下拉时同步降低 IME inset，形成手势-键盘联动收起效果 |

### 2.2 欠缺但本阶段不建设的部分（暂不规划）

| 建设项 | 暂不建设原因 | 后续评估节点 |
|--------|-------------|--------------|
| Web / 小程序 / macOS 等价实现 | 目标平台外范围，各平台键盘模型差异大 | 暂不入规划 |

---

## 第三部分：开发节奏与验收标准

### 3.1 Phase 1：基础 IME Inset 通道

**阶段目标**：建立页面级 IME 状态，提供 `WindowInsets.ime`、`imePadding()`、`Scaffold` 默认避让，解决"有没有"的问题。

#### 模块级改动清单

| 模块 | 文件 | 关键改动 |
|------|------|----------|
| `core/` | `Pager.kt` | 新增 IME page event 常量 `imeInsetsDidChanged`，定义 payload key：`height`、`duration`、`curve` |
| `compose/` | `ComposeContainer.kt` | 接收 IME pager event，调用 `Configuration.onImeInsetsChanged()` |
| `compose/` | `LocalConfiguration.kt` | 新增 `imeBottomDp`、`imeAnimationDuration`、`imeAnimationCurve` 及更新方法 |
| `compose/` | `WindowInsets.kt` | 新增 `WindowInsets.Companion.ime`，从 `LocalConfiguration` 读取状态 |
| `compose/` | `WindowInsetsPadding.kt` | 新增 `Modifier.imePadding()`，复用 `windowInsetsPadding()` 消费 `WindowInsets.ime` |
| `compose/` | `Scaffold.kt` | 调整 `ScaffoldDefaults.contentWindowInsets` 为 `systemBarsForVisualComponents.union(WindowInsets.ime)` |
| `core-render-android/` | `KuiklyRenderView.kt` | 注册页面级键盘高度 listener，变化时发送 IME pager event |
| `core-render-ios/` | `KuiklyRenderView.m` | 注册 `UIKeyboardWillShow/HideNotification`，发送 IME pager event |
| `core-render-ohos/` | `KRNativeRenderController.ets` | 复用 `window.on('keyboardHeightChange')`，发送 IME pager event |
| `demo/` | `KeyboardHeightDemo.kt` | 底部输入栏 + `imePadding()` 场景 |
| `demo/` | `ScaffoldDemo.kt` | 普通表单页 + `Scaffold` 默认避让场景 |

#### 阶段完成后达到的效果

- 任何 Compose 页面均可使用 `WindowInsets.ime` 获取当前键盘高度，无需输入组件挂载。
- 业务可在任意容器上使用 `Modifier.imePadding()` 实现声明式键盘避让。
- 使用 `Scaffold` 的页面默认自动避让键盘，开箱即用。
- 三端（Android / iOS / HarmonyOS）行为一致，高度同步准确。
- 旧有 `keyboardHeightChange()` 业务写法不受影响，可渐进迁移。

#### 验收方式

| 场景 | 操作步骤 | 预期现象 | 通过标准 |
|------|----------|----------|----------|
| 底部输入栏 | 进入 `KeyboardHeightDemo` → 点击输入框唤起键盘 | 底部输入区随键盘上移，不被遮挡；键盘收起后恢复 | 三端均通过 |
| 表单页避让 | 进入 `ScaffoldDemo` → 点击靠下的输入框 | `Scaffold` 内容区自动避让键盘，底部表单仍可见 | 三端均通过 |
| 旧 callback 兼容 | 运行原有 `keyboardHeightChange` 相关 demo | 旧输入组件回调继续生效，无中断 | 无 regression |
| 状态源独立性 | 页面中无输入框时唤起键盘（如外部触发） | `WindowInsets.ime` 仍能正确更新 | 高度正确 |
| 生命周期安全 | 反复进入/退出页面，多次唤起/收起键盘 | 无重复通知、无内存泄漏、无高度残留 | 稳定运行 |

---

### 3.2 Phase 2：聚焦自动滚动（BringIntoView）

**阶段目标**：当输入框获取焦点且被键盘遮挡时，自动滚动到可视区域，解决"看得到"的问题。

#### 模块级改动清单

| 模块 | 文件 | 关键改动 |
|------|------|----------|
| `compose/` | 新增 `BringIntoViewRequester.kt` | 实现 `BringIntoViewRequester` 接口，焦点组件通过 `Modifier.bringIntoViewRequester()` 注册 |
| `compose/` | 焦点管理拦截点 | 在焦点变化回调中检测组件是否被 IME inset 遮挡，若被遮挡则发起 BringIntoView 请求 |
| `compose/` | `Column` / `LazyColumn` 等容器 | 响应 BringIntoView 请求，计算目标组件相对容器的偏移量，执行平滑滚动 |
| `compose/` | `Scroller` bridge 扩展 | 若 Compose 侧容器无法独立完成滚动，增加向 Native `Scroller` 发送滚动指令的通道 |
| `demo/` | 新增 `BringIntoViewDemo.kt` | 长表单页，底部输入框聚焦后自动滚动至键盘上方 |

#### 阶段完成后达到的效果

- 输入框获取焦点时，若其底部位于 `WindowInsets.ime` 遮挡区域内，父布局自动滚动使其完全可见。
- 滚动过程有基本动画，非突兀跳转。
- 在 `Column`、`LazyColumn` 等常见容器中均可生效。
- 不依赖业务手动监听键盘高度并计算滚动偏移。

#### 验收方式

| 场景 | 操作步骤 | 预期现象 | 通过标准 |
|------|----------|----------|----------|
| 长表单聚焦滚动 | 进入 `BringIntoViewDemo` → 滚动到底部 → 点击最下方输入框 | 页面自动上滚，输入框完整显示在键盘上方 | 三端均通过 |
| 列表内聚焦滚动 | 在 `LazyColumn` 中间某行点击输入框 | 列表自动滚动，该行滚动至可视区 | 三端均通过 |
| 键盘已显示时切换焦点 | 键盘已唤起 → 点击更下方的输入框 | 页面再次滚动，新焦点组件可见 | 连续触发正确 |
| 无遮挡时不滚动 | 点击页面顶部输入框（键盘不遮挡） | 页面保持不动 | 不误触发 |

---

### 3.3 Phase 3：IME 动画联动与曲线优化

**阶段目标**：键盘弹出/收起过程与 Compose 布局变化形成逐帧联动，提升动画质量，解决"动得顺"的问题。

#### 模块级改动清单

| 模块 | 文件 | 关键改动 |
|------|------|----------|
| `compose/` | `LocalConfiguration.kt` | 将 `imeAnimationDuration`、`imeAnimationCurve` 提升为公共属性 |
| `compose/` | `WindowInsets.kt` | 如需要，新增 `WindowInsets.imeAnimation` 相关查询接口 |
| `compose/` | 新增 `ImeNestedScrollConnection.kt` | 实现与官方 `imeNestedScroll` 等价的 NestedScrollConnection，拦截列表手势与 IME 变化 |
| `core-render-android/` | `KuiklyRenderView.kt` | 接入 `WindowInsetsAnimation` 监听，逐帧上报 IME 高度变化，附带 `duration/curve` |
| `core-render-ios/` | `KuiklyRenderView.m` | 评估从 `UIKeyboardWillShow/Hide` 切换到 `keyboardWillChangeFrame`，逐帧上报 |
| `core-render-ohos/` | `KRNativeRenderController.ets` | 评估 HarmonyOS 窗口动画回调能力，补齐逐帧上报 |
| `compose/` | `Modifier.imePadding()` 动画化 | 使用 `Animatable` 接收逐帧高度输入，以平台提供的 `duration/curve` 驱动 padding 变化 |
| `demo/` | 新增 `ImeAnimationDemo.kt` | 展示逐帧联动效果：布局边缘与键盘边缘始终保持贴合 |
| `demo/` | 新增 `ImeNestedScrollDemo.kt` | 展示列表拖拽时键盘跟手收起效果 |

#### 阶段完成后达到的效果

- 键盘弹出/收起时，使用 `imePadding()` 的容器高度变化与键盘动画同步，无跳动或延迟。
- 业务可读取 `duration/curve` 参数，用于自定义布局动画。
- 在可滚动列表中拖拽时，键盘可跟手收起，形成连贯的手势体验。
- 三端动画质量达到可接受水准，iOS 不再因 `WillShow/Hide` 粗粒度模型导致边缘不贴合。

#### 验收方式

| 场景 | 操作步骤 | 预期现象 | 通过标准 |
|------|----------|----------|----------|
| 逐帧贴合 | 进入 `ImeAnimationDemo` → 唤起键盘 | 慢放录屏：布局底部与键盘顶部边缘全程贴合，无跳变 | 人眼无明显跳动 |
| 动画参数可读 | 代码中读取 `LocalConfiguration.current.imeAnimationDuration` | 返回正值（如 300ms），与平台实际动画一致 | 数值正确 |
| 列表拖拽收键盘 | 进入 `ImeNestedScrollDemo` → 键盘唤起 → 向下拖拽列表 | 键盘随拖拽进度逐渐收起，手指抬起后键盘完成剩余动画 | 手势联动自然 |
| 快速切换 | 连续快速点击输入框唤起/收起键盘 | 动画不冲突、不叠加、最终高度正确 | 无异常 |
| 三端对比 | 同一场景在三端分别运行 | 动画曲线有平台差异，但最终体验相当 | 三端均通过 |

---

## 附录：受影响模块与平台（全 Phase）

| 模块 | 平台 | Phase 1 改动 | Phase 2 改动 | Phase 3 改动 |
|------|------|--------------|--------------|--------------|
| `compose/` | Android / iOS / HarmonyOS | 新增 API + 调整 Scaffold 默认行为 | 新增 BringIntoView 机制 | 新增动画联动 + 手势联动 |
| `core-render-android/` | Android | 新增页面级桥接 | 无 | 接入 WindowInsetsAnimation |
| `core-render-ios/` | iOS | 新增页面级桥接 | 无 | 评估切到 keyboardWillChangeFrame |
| `core-render-ohos/` | HarmonyOS | 复用已有链路，补 pager event | 无 | 评估逐帧动画回调 |
| `demo/` | 三端 | 新增验证页面 | 新增 BringIntoViewDemo | 新增 ImeAnimationDemo / ImeNestedScrollDemo |
| `core/` | 三端 | 新增事件常量 | 无 | 无 |

---

## 附录：风险评估与缓解（全 Phase）

| 风险 | 影响阶段 | 具体影响 | 缓解措施 |
|------|----------|----------|----------|
| iOS `WillShow/WillHide` 无法覆盖浮动键盘 | Phase 1 | 部分场景高度不精确 | Phase 1 明确只承诺"基础规避"；Phase 3 评估切到 `keyboardWillChangeFrame` |
| 三端 `duration/curve` 精度不一致 | Phase 1 ~ 3 | 动画参数不可靠 | Phase 1 只把 `height` 作为行为契约；Phase 3 以 Android `WindowInsetsAnimation` 为基准对齐其他平台 |
| `Scaffold` 默认加 IME 后旧页面双重避让 | Phase 1 | 少量页面布局异常 | 依赖 consumed insets 机制；业务可显式传入自定义 `contentWindowInsets` 覆盖默认行为 |
| 页面级 listener 生命周期泄漏 | Phase 1 | 重复通知或内存泄漏 | 统一挂载在页面宿主对象，销毁时成对移除 observer |
| BringIntoView 与业务自定义滚动冲突 | Phase 2 | 双重滚动或滚动方向矛盾 | BringIntoView 请求加入优先级队列，业务显式滚动优先级更高；提供开关禁用默认行为 |
| 逐帧动画性能开销 | Phase 3 | 低端机型掉帧 | 提供降级开关：关闭逐帧联动，回退到 Phase 1 的即时变化模式 |
| iOS `keyboardWillChangeFrame` 与旧通知共存 | Phase 3 | 通知重复或冲突 | 彻底替换旧通知，不再共存；充分回归验证 |
