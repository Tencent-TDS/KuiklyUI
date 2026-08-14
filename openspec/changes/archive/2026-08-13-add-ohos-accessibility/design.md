## Context

### 现状

- Kuikly `core`（KMP）已提供无障碍 API：
  - `Attr.kt` 的 `accessibility(text)` / `accessibilityRole(role)` / `accessibilityInfo(clickable, longClickable)`
  - `DeclarativeBaseView.kt` 的 `accessibilityAnnounce(msg)` / `accessibilityFocus()`
  - `AccessibilityRole` 枚举：`NONE / BUTTON / SEARCH / TEXT / IMAGE / CHECKBOX`
- Android / iOS 端已实现（`core-render-android/**/KRCSSViewExtension.kt`、`core-render-ios/Extension/Category/UIView+CSS.m` 与 `Extension/Components/KRView.m`）
- HarmonyOS 端 `core-render-ohos`：
  - cpp 侧仅有 `accessibility` → `NODE_ACCESSIBILITY_TEXT`（在 `KRBasePropsHandler` 与 `KRViewUtil::UpdateNodeAccessibility`）
  - `accessibilityRole` / `accessibilityInfo` / `accessibilityAnnounce` / `accessibilityFocus` 全库 grep 零命中
  - ets 侧 `KuiklyRenderBaseView.setProp` 只处理 `backgroundColor` / `backgroundImage`，`call` 无实现

### 双渲染路径

HarmonyOS 端有两类组件，走不同的属性/方法通道：

| 组件类别 | 节点创建路径 | 属性通道 | 方法通道 |
|---|---|---|---|
| CAPI 内置组件（KRView / KRImageView …） | cpp `IKRRenderViewExport::CreateNode` → CAPI `createNode(ARKUI_NODE_STACK)` | `KRBasePropsHandler` → CAPI `setAttribute` | cpp `CallMethod` |
| ArkTS 转发组件（`KRForwardArkTSView(V2)`，业务方通过 `viewName()` 注册） | cpp `CreateNode` 通过 `KRArkTSManager::CallArkTSMethod(CreateArkUINode)` 到 ArkTS 侧创建 `ComponentContent`；节点由 `registerNodeCreatedFromArkTS` 标记 | `KRArkTSViewBasePropsHandler`（**全部 return false**）→ `KRForwardArkTSView::SetProp` 桥到 ArkTS `KuiklyRenderBaseView.setProp` → 业务方 `@Component` build 应用修饰器 | cpp `KRForwardArkTSViewV2::CallMethod` 桥到 ArkTS `nativeInstance.callViewMethod` → 业务方 `KuiklyRenderBaseView.call` |

### 关键 SDK 事实（Spike 已跑验）

1. **CAPI 不能修改 ArkTS 创建的节点**：对 `KRForwardArkTSViewV2` 的 `ark_node_` 调 `setAttribute(NODE_ACCESSIBILITY_ROLE, ...)` 返回 `ret=106103`，即 `ARKUI_ERROR_CODE_ARKTS_NODE_NOT_SUPPORTED`。所以属性无法在 cpp 层统一处理，必须 cpp+ets 分别实现。
2. **ArkTS 侧无障碍事件 API**：`@ohos.accessibility.sendAccessibilityEvent(EventInfo)`，`EventInfo` 构造签名 `(type: EventType, bundleName: string, triggerAction: Action)`，`bundleName` 必填，空串会 401；`textAnnouncedForAccessibility` 用于 announce，`customId` 用于 focus 目标定位。
3. **ArkTS 修饰器叠加规则**：`.accessibilityText()` 应用在外层容器；子节点需 `.accessibilityLevel('no')` 或外层 `.accessibilityGroup(true)`，否则读屏优先聚焦到子节点使外层文本失效。
4. **CAPI 有效映射**：
   - `NODE_ACCESSIBILITY_ROLE`(=89): `value[0].u32` = `ArkUI_NodeType`
   - `NODE_ACCESSIBILITY_MODE`(=1421): `value[0].i32` = `ArkUI_AccessibilityMode`
   - `NODE_ACCESSIBILITY_TEXT`(=1409): `.string`
   - `NODE_ACCESSIBILITY_ACTIONS`(=88): `value[0].u32` = `ArkUI_AccessibilityActionType` 位或
5. **`ArkUI_NodeType` 缺失 SEARCH/NONE**：需降级映射（SEARCH → `ARKUI_NODE_TEXT_INPUT`，NONE → 走 `NODE_ACCESSIBILITY_MODE = DISABLED`）

### 约束（DSL 与依赖）

- 本 change 只涉及**自研 DSL** 侧的属性/方法透传实现。Compose DSL（`compose/`）不受影响——Compose 也是通过 core 层最终发到 render，行为对等。
- `core-render-ohos` 依赖 `core`（common API），不反向依赖。本 change **不改 `core` 层任何代码**。
- NativeBridge 交互：无新增跨语言方法枚举——announce/focus 复用已有的 `KRNativeCallArkTSMethod::CallViewMethod`；属性透传复用 `SetViewProp`。

## Goals / Non-Goals

**Goals:**

- 使 HarmonyOS 端行为与 Android / iOS 对齐，实现全部 5 项无障碍能力（3 属性 + 2 方法）
- 覆盖 CAPI 内置组件与 ArkTS 转发组件两条渲染路径，不遗漏
- 提供业务方接入的最小心智负担：使用 Kuikly 内置组件零改动；使用 ArkTS 转发自定义组件仅需在自己 `@Component build()` 加 4~5 行修饰器
- 补齐文档，标注跨端差异与鸿蒙特有的 role 映射规则
- 提供 demo 示例作为业务方参考

**Non-Goals:**

- 不改动 `core/` KMP core 层公共 API
- 不新增 kotlin 侧 `accessibilityHint` / `LINK` 等更多角色（未来独立 change）
- 不实现 `ArkUI_AccessibilityProvider`（仅 `ARKUI_NODE_CUSTOM` 支持，Kuikly 节点结构不匹配）
- 不改动 `KRArkTSViewBasePropsHandler` 的 no-op 语义（SDK 硬性限制）
- 不涉及输入焦点（`NODE_FOCUS_STATUS` / `OH_ArkUI_FocusRequest`）
- 不为业务方"基类统一注入无障碍修饰器"（ArkTS `ComponentContent` 嵌套/cpp wrap Stack 均破坏节点树，Spike 已确认技术不可行）

## Decisions

### D1：属性通道 —— cpp + ets 双侧实现（不复用）

**决策**：`accessibilityRole` / `accessibilityInfo` 的 setProp 在 cpp `KRBasePropsHandler`（覆盖 CAPI 组件）与 ets `KuiklyRenderBaseView`（覆盖 ArkTS 转发组件）**分别实现**。

**替代方案**：
- ~~方案 A：cpp 层统一 setAttribute~~ —— 被 Spike 0 否决：SDK 返回 106103 `ARKUI_ERROR_CODE_ARKTS_NODE_NOT_SUPPORTED`
- ~~方案 B：ets 基类统一注入修饰器~~ —— `ComponentContent` 不支持嵌套；cpp 侧套一层 Stack 会破坏节点树的父子关系、hitTest 与 frame 语义
- **方案 C：双侧实现（选定）** —— 与现有 `backgroundColor` / `backgroundImage` 的处理模式一致（cpp 侧对内置 view 处理，ArkTS 转发 view 走 `SetViewProp` 到 ets），符合既有架构

**代价**：业务方 ArkTS 组件需要在自己 `@Component` 里加 `.accessibilityText(this.renderView.cssAccessibilityText)` 等修饰器。通过文档 + demo 引导，且这与业务方已经在做的 `.backgroundColor(this.renderView.cssBackgroundColor)` 是同一心智模型。

### D2：方法通道 —— cpp `CallMethod` 拦截 + 桥到 ArkTS 统一实现

**决策**：`accessibilityAnnounce` / `accessibilityFocus` 在 cpp `IKRRenderViewExport::CallMethod` 拦截，通过 `KRArkTSManager::CallArkTSMethod(CallViewMethod, ...)` 桥到 ArkTS 侧 view 的 `call()` 统一处理。CAPI 内置组件与 ArkTS 转发组件复用同一份 ArkTS 实现。

**替代方案**：
- ~~纯 cpp 实现~~：CAPI 无 announce 接口（`OH_ArkUI_SendAccessibilityAsyncEvent` 需 `AccessibilityProvider`，只支持 `ARKUI_NODE_CUSTOM`）；无 "请求无障碍焦点" 接口（`OH_ArkUI_FocusRequest` 是输入焦点）
- ~~ets 独立 Module 承接 announce~~：announce 虽是页面级 API，但 kotlin core 定义在 `DeclarativeBaseView` 上（view 方法而非 module 方法），走 view 通道更契合现有 kotlin 语义

**技术选型**：
- announce：`accessibility.EventInfo('announceForAccessibility', bundleName, 'common')`，`textAnnouncedForAccessibility` = kotlin 侧传入的 msg
- focus：`accessibility.EventInfo('requestFocusForAccessibility', bundleName, 'common')`，`customId` = `this.getNodeId()`
- `bundleName` 从 `this.getUIContext()?.getHostContext()?.applicationInfo?.name` 动态取

### D3：`AccessibilityRole` → ohos 映射表

| kotlin role | ohos 实现 |
|---|---|
| `BUTTON` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_BUTTON`(9) |
| `TEXT` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_TEXT`(1) |
| `IMAGE` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_IMAGE`(4) |
| `CHECKBOX` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_CHECKBOX`(11) |
| `SEARCH` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_TEXT_INPUT`(7)（降级，与安卓 `EditText` 语义对齐）|
| `NONE` | `NODE_ACCESSIBILITY_MODE` = `ARKUI_ACCESSIBILITY_MODE_DISABLED`(2)（不设 role，改设 mode）|

**理由**：`ArkUI_NodeType` 枚举无 SEARCH/NONE 值。SEARCH 降级到 `TEXT_INPUT` 与安卓端 `EditText::class.java.name` 语义等价（都是"可编辑单行文本"）。NONE 走 mode 通道对齐安卓 `IMPORTANT_FOR_ACCESSIBILITY_NO`（把节点从无障碍树中剔除）。

**ets 侧对应**：kotlin role 字符串直接透传到 ets，业务方在 `@Component` 里根据字符串应用相应 ArkTS `.accessibilityLevel(...)` / 修饰器；SDK 提供工具函数 `applyAccessibilityRole(role: string)` 供业务方使用。

### D4：cpp `CallMethod` 桥接 —— 复用而非新增枚举

**决策**：announce/focus 复用已有的 `KRNativeCallArkTSMethod::CallViewMethod`（=6），不新增 `KRNativeCallArkTSMethod` 枚举值。

**理由**：ArkTS 转发 view 的 `CallMethod` 已经通过 `CallViewMethod` 桥接（见 `KRForwardArkTSViewV2::CallMethod`）。CAPI 内置 view 只需在基类 `IKRRenderViewExport::CallMethod` 里对这两个 method 名添加拦截并同样走 `CallViewMethod` 即可，无跨语言协议变动。

### D5：文件级职责划分

**cpp 侧改动**（仅 `core-render-ohos`）：

| 文件 | 改动 |
|---|---|
| `libohos_render/expand/components/base/KRBasePropsHandler.cpp` | `SetPropWithoutAnimation`：新增 `accessibilityRole` / `accessibilityInfo` 两个分支；`ResetProp`：新增对应重置逻辑 |
| `libohos_render/utils/KRViewUtil.h` / `.cpp` | 新增 `UpdateNodeAccessibilityRole(node, roleStr)` / `UpdateNodeAccessibilityActions(node, infoStr)` 工具函数，包含 role 缺口降级与 mode 分派逻辑 |
| `libohos_render/export/IKRRenderViewExport.cpp` | `CallMethod`：新增 `accessibilityAnnounce` / `accessibilityFocus` 拦截 → `KRArkTSManager::CallArkTSMethod(CallViewMethod, ...)` |

**ets 侧改动**（仅 `core-render-ohos`）：

| 文件 | 改动 |
|---|---|
| `src/main/ets/components/base/KRBaseViewExport.ets` | `KuiklyRenderBaseView`：新增 3 个 `cssAccessibility*` 字段；`setProp` 增加 3 个 case；`call()` 基类新增 announce/focus 分支，调用 `@ohos.accessibility.sendAccessibilityEvent` |
| `src/main/ets/utils/`（新文件）`KRAccessibilityHelper.ets` | 导出业务方修饰器辅助函数（如 `applyKuiklyAccessibility(renderView)`），封装 `.id()` / `.accessibilityText()` / `.accessibilityLevel()` / `.accessibilityGroup()` 组合 |

**文档改动**：

| 文件 | 改动 |
|---|---|
| `docs/API/components/basic-attr-event.md` | 扩展 `accessibility方法` 章节；新增 `accessibilityRole`、`accessibilityInfo`、`accessibilityAnnounce`、`accessibilityFocus` 四小节；补跨端支持矩阵与鸿蒙 role 映射表 |
| `docs/DevGuide/`（新文件） `ohos-custom-accessibility.md` | 详解鸿蒙自定义 ArkTS 组件的无障碍接入模板 |

**Demo 改动**：

| 文件 | 改动 |
|---|---|
| `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/CustomViewExamplePage.kt` | 加 accessibility 属性与 announce/focus 触发按钮 |
| `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/AccessibilityTestPage.kt`（新文件） | A11y 三端测试页，覆盖属性/method/自定义 View 焦点容器等 20+ 用例 |
| `ohosApp/entry/src/main/ets/kuikly/components/KRMyDemoCustomView.ets` | 加字段、setProp 分派、`@Component` 应用修饰器（作为业务方参考模板） |
| `androidApp/src/main/java/com/tencent/kuikly/android/demo/KRMyDemoCustomView.kt`（新文件） | Android 端原生实现：黄底黑边 + 居中文字 + Tap Me + 子节点插槽；A11y 走 `KRView` 基类通用通道 |
| `androidApp/src/main/java/com/tencent/kuikly/android/demo/ContextCodeHandler.kt` | `registerExternalRenderView` 注册 `KRMyDemoCustomView.VIEW_NAME` |
| `iosApp/iosApp/KuiklyRenderExpand/Views/KRMyDemoCustomView.h/.m`（新文件） | iOS 端原生实现：继承 `KRView`（复用 `hrv_callWithMethod:` 的 announce/focus 桥接）；A11y 走 `UIView+CSS` 通用通道 |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | 加入上述 `.h/.m` 文件（`Views` group / BuildFile / FileReference / SourcesBuildPhase） |

## Risks / Trade-offs

- **[R1] 业务方 ArkTS 组件遗漏修饰器 → 无障碍语义丢失**：文档 + demo + 业务方 code review 兜底；无法在 SDK 侧强制。
- **[R2] `SEARCH` role 降级到 `TEXT_INPUT`，读屏播报可能与安卓/iOS 不完全一致**：文档明示"鸿蒙将 SEARCH 视为单行文本输入"；实用差异不大。
- **[R3] `bundleName` 通过 `getUIContext()?.getHostContext()?.applicationInfo?.name` 获取，若在某些边界场景（页面初始化未完成）取不到 → announce/focus 失败**：`bundleName` 空则回退取 `getContext(this).applicationInfo.name`；仍然失败则 `console.error` 日志后 no-op，不抛异常。
- **[R4] `accessibilityLevel('no')` 需要业务方主动应用到子节点**：文档强调；提供 helper 函数 `applyKuiklyAccessibilityChild()` 供业务方在子节点批量调用。
- **[R5] `NODE_ACCESSIBILITY_MODE` / `NODE_ACCESSIBILITY_ACTIONS` 常量的 SDK 最低版本兼容**：需在 cpp 编译期通过 `__attribute__((__availability__(ohos, introduced=...)))` 或运行期 API version 检查降级；对当前 Kuikly 目标 API 版本（≥ 12）都可用。
- **[R6] Spike 临时代码与 proposal 实现代码可能有微妙差异**：实现阶段需以 spec.md 的 Requirements 为准，不可直接照抄 spike 代码；实现完成后清理 spike 分支。

## Migration Plan

- **无破坏性变更**。全部为新增能力，现有代码不受影响。
- 部署顺序：cpp 与 ets 侧改动必须同一版本发布（cpp 侧 CallMethod 桥接依赖 ets 侧 base call 实现）。
- 回滚策略：如线上出现无障碍相关闪退，可通过 revert 本 change 恢复到当前行为（仅 `accessibility` 文本生效，其他 no-op）。

## Open Questions

- **Q1**：`AccessibilityRole.NONE` 的 ets 侧实现如何呼应 cpp 的 `MODE_DISABLED`？→ 建议 ets 侧检查 `cssAccessibilityRole === 'none'` 时应用 `.accessibilityLevel('no')`，语义对齐。写实现时按此办。
- **Q2**：`KRAccessibilityHelper.ets` 里的 helper 应该以扩展函数还是普通函数导出？→ ArkTS 不支持真正的扩展函数在 `@Component` 里直接链式调用，只能用普通函数返回配置对象，或让业务方手动逐行调用。建议**逐行手动调用 + 文档示例**，最简单可维护。
- **Q3**：是否需要给 core 层 `accessibilityInfo` 序列化格式加防御性校验？（当前 `"0 0"` / `"1 1"` 位串通过位置解析）→ 现有安卓/iOS 已用此格式且线上稳定；不改。
