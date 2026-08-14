## ADDED Requirements

### Requirement: Accessibility 文本朗读 (`accessibility` prop)

HarmonyOS 端的 CAPI 内置组件与 ArkTS 转发组件，SHALL 支持 kotlin `Attr.accessibility(text)` 设置的字符串作为该节点的无障碍朗读内容，行为与 Android `contentDescription`、iOS `accessibilityLabel` 语义对齐。

#### Scenario: CAPI 内置组件的 accessibility 文本朗读（HarmonyOS）

- **GIVEN** 一个 CAPI 内置组件（如 `View`、`Image`）在 kotlin 侧设置了 `attr { accessibility("头像") }`
- **WHEN** 系统开启屏幕朗读，用户聚焦到该组件
- **THEN** 屏幕朗读器 SHALL 播报 "头像"，而不是组件默认内容

#### Scenario: ArkTS 转发组件的 accessibility 文本朗读（HarmonyOS）

- **GIVEN** 一个 ArkTS 转发组件（业务方通过 `viewName()` 注册）在 kotlin 侧设置了 `attr { accessibility("提交按钮") }`，且业务方 `@Component build()` 里最外层容器已按约定应用 `.accessibilityText(this.renderView.cssAccessibilityText ?? "")` 及配套修饰器
- **WHEN** 用户聚焦到该组件
- **THEN** 屏幕朗读器 SHALL 播报 "提交按钮"

#### Scenario: 属性重置（HarmonyOS）

- **WHEN** kotlin 侧将 `accessibility` 属性设为空串或被 view 复用系统重置
- **THEN** CAPI 组件 SHALL 通过 `resetAttribute(NODE_ACCESSIBILITY_TEXT)` 恢复默认；ArkTS 组件 SHALL 将 `cssAccessibilityText` 设为空，业务方修饰器读到空串时不再覆盖默认朗读


### Requirement: Accessibility 角色声明 (`accessibilityRole` prop)

HarmonyOS 端 SHALL 将 kotlin `AccessibilityRole` 枚举映射为鸿蒙无障碍节点角色（或无障碍模式），映射规则如下：

| kotlin role | HarmonyOS 实现 |
|---|---|
| `BUTTON` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_BUTTON` |
| `TEXT` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_TEXT` |
| `IMAGE` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_IMAGE` |
| `CHECKBOX` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_CHECKBOX` |
| `SEARCH` | `NODE_ACCESSIBILITY_ROLE` = `ARKUI_NODE_TEXT_INPUT`（降级） |
| `NONE` | `NODE_ACCESSIBILITY_MODE` = `ARKUI_ACCESSIBILITY_MODE_DISABLED`（不设 role） |

#### Scenario: 常规角色映射（HarmonyOS，CAPI 内置组件）

- **GIVEN** 一个 CAPI 内置组件设置 `attr { accessibilityRole(AccessibilityRole.BUTTON) }`
- **WHEN** 用户聚焦到该组件
- **THEN** 屏幕朗读器 SHALL 播报 "按钮" 作为角色提示

#### Scenario: SEARCH 降级映射（HarmonyOS）

- **GIVEN** 一个组件设置 `attr { accessibilityRole(AccessibilityRole.SEARCH) }`
- **WHEN** 用户聚焦到该组件
- **THEN** 该组件 SHALL 被识别为文本输入类角色（`ARKUI_NODE_TEXT_INPUT`），与 Android 端 `EditText` 语义对齐
- **AND** 文档 SHALL 明确说明鸿蒙将 SEARCH 视为单行文本输入

#### Scenario: NONE 剔除无障碍树（HarmonyOS）

- **GIVEN** 一个组件设置 `attr { accessibilityRole(AccessibilityRole.NONE) }`
- **WHEN** 用户在屏幕上滑动焦点
- **THEN** 该 CAPI 组件 SHALL 通过 `NODE_ACCESSIBILITY_MODE = ARKUI_ACCESSIBILITY_MODE_DISABLED` 被跳过；ArkTS 转发组件 SHALL 通过 `.accessibilityLevel('no')` 被跳过
- **AND** 语义对齐 Android `IMPORTANT_FOR_ACCESSIBILITY_NO`

#### Scenario: ArkTS 转发组件角色映射（HarmonyOS）

- **GIVEN** 一个 ArkTS 转发组件设置 `attr { accessibilityRole(AccessibilityRole.BUTTON) }`，且业务方 `@Component build()` 里已根据 `cssAccessibilityRole` 应用相应 ArkTS 修饰器
- **WHEN** 用户聚焦到该组件
- **THEN** 屏幕朗读器 SHALL 播报该组件的角色


### Requirement: Accessibility 交互动作声明 (`accessibilityInfo` prop)

HarmonyOS 端 SHALL 将 kotlin `Attr.accessibilityInfo(clickable, longClickable)` 序列化的两位标志串（如 `"1 0"`）映射为鸿蒙无障碍动作位掩码 `ARKUI_ACCESSIBILITY_ACTION_CLICK | ARKUI_ACCESSIBILITY_ACTION_LONG_CLICK`。

#### Scenario: 可点击提示（HarmonyOS，CAPI 组件）

- **GIVEN** 一个组件设置 `attr { accessibilityInfo(clickable = true, longClickable = false) }`
- **WHEN** 用户聚焦到该组件
- **THEN** 屏幕朗读器 SHALL 播报可点击操作提示（如 "双击激活"）
- **AND** CAPI 组件通过 `NODE_ACCESSIBILITY_ACTIONS` = `ARKUI_ACCESSIBILITY_ACTION_CLICK` 生效

#### Scenario: 可点击且可长按（HarmonyOS）

- **GIVEN** 一个组件设置 `attr { accessibilityInfo(clickable = true, longClickable = true) }`
- **WHEN** 用户聚焦到该组件
- **THEN** CAPI 组件 SHALL 通过 `NODE_ACCESSIBILITY_ACTIONS` = `ACTION_CLICK | ACTION_LONG_CLICK` 生效
- **AND** ArkTS 转发组件 SHALL 通过业务方 `@Component` 应用等价 ArkTS 修饰器生效


### Requirement: 主动播报文本 (`accessibilityAnnounce` method)

HarmonyOS 端 SHALL 支持通过 kotlin `DeclarativeBaseView.accessibilityAnnounce(msg)` 触发屏幕朗读器主动播报指定文本。行为与 Android `announceForAccessibility` 及 iOS `UIAccessibilityAnnouncementNotification` 对齐。

实现路径：cpp `IKRRenderViewExport::CallMethod` 拦截 → 通过 `KRArkTSManager::CallArkTSMethod(CallViewMethod, ...)` 桥到 ArkTS → ArkTS `KuiklyRenderBaseView.call()` 调用 `@ohos.accessibility.sendAccessibilityEvent`（`type = 'announceForAccessibility'`，`textAnnouncedForAccessibility = msg`，`bundleName` 从 `UIContext.getHostContext().applicationInfo.name` 动态取）。

#### Scenario: 主动播报（HarmonyOS）

- **GIVEN** 系统开启了屏幕朗读
- **WHEN** kotlin 侧调用 `view.accessibilityAnnounce("提交成功")`
- **THEN** 屏幕朗读器 SHALL 播报 "提交成功"，无论该 view 是否处于焦点

#### Scenario: bundleName 兜底（HarmonyOS）

- **GIVEN** ArkTS 侧 `UIContext.getHostContext().applicationInfo.name` 返回空
- **WHEN** kotlin 侧调用 `view.accessibilityAnnounce("测试")`
- **THEN** 系统 SHALL 尝试 `getContext(this).applicationInfo.name` 作为兜底
- **AND** 如果仍然为空，SHALL 记录 `console.error` 并静默 no-op，NOT 抛出未捕获异常


### Requirement: 无障碍焦点跳转 (`accessibilityFocus` method)

HarmonyOS 端 SHALL 支持通过 kotlin `DeclarativeBaseView.accessibilityFocus()` 请求将屏幕朗读器的焦点跳转到调用该方法的 view 上。行为与 Android `TYPE_VIEW_ACCESSIBILITY_FOCUSED` 事件、iOS `UIAccessibilityScreenChangedNotification` 对齐。

实现路径：cpp `IKRRenderViewExport::CallMethod` 拦截 → 桥到 ArkTS → ArkTS `KuiklyRenderBaseView.call()` 调用 `@ohos.accessibility.sendAccessibilityEvent`（`type = 'requestFocusForAccessibility'`，`customId = this.getNodeId()`）。

业务方 ArkTS 组件的最外层容器 MUST 应用 `.id(this.renderView.getNodeId())`，使 `customId` 能定位到目标节点。

#### Scenario: 焦点跳转（HarmonyOS）

- **GIVEN** 屏幕朗读焦点当前在页面某个位置，一个 ArkTS 转发组件 `yellowView` 的最外层容器已应用 `.id(this.renderView.getNodeId())`
- **WHEN** kotlin 侧调用 `yellowView.accessibilityFocus()`
- **THEN** 屏幕朗读器的焦点框 SHALL 跳转到 `yellowView`

#### Scenario: 焦点跳转失败（HarmonyOS）

- **GIVEN** 业务方遗漏了 `.id(this.renderView.getNodeId())` 修饰器
- **WHEN** kotlin 侧调用 `view.accessibilityFocus()`
- **THEN** `accessibility.sendAccessibilityEvent` SHALL 触发但焦点不会跳转（`customId` 匹配不到目标）
- **AND** 系统 NOT 抛出未捕获异常，仅日志记录


### Requirement: 双渲染路径覆盖

HarmonyOS 端的无障碍属性/方法实现 SHALL 同时覆盖 CAPI 内置组件（`KRView`、`KRImageView` 等，走 `KRBasePropsHandler`）与 ArkTS 转发组件（`KRForwardArkTSView(V2)`，走 `KRArkTSViewBasePropsHandler` no-op 后桥到 ArkTS），两条渲染路径的行为等价。

#### Scenario: CAPI 组件属性通过 cpp 生效（HarmonyOS）

- **GIVEN** 一个 CAPI 内置组件
- **WHEN** kotlin 设置 `accessibilityRole` / `accessibilityInfo`
- **THEN** cpp `KRBasePropsHandler::SetPropWithoutAnimation` SHALL 调用 CAPI `setAttribute` 生效

#### Scenario: ArkTS 组件属性通过 ets 生效（HarmonyOS）

- **GIVEN** 一个 ArkTS 转发组件
- **WHEN** kotlin 设置 `accessibilityRole` / `accessibilityInfo`
- **THEN** 属性 SHALL 通过 `SetViewProp` 桥到 ArkTS `KuiklyRenderBaseView.setProp`，写入 `cssAccessibilityRole` / `cssAccessibilityInfo` 字段
- **AND** 业务方 `@Component build()` 从 `this.renderView` 读取对应字段并应用 ArkTS 修饰器
- **AND** cpp 侧不得对该组件的 `ark_node_` 直接调 `setAttribute(NODE_ACCESSIBILITY_*)`（会返回 `ARKUI_ERROR_CODE_ARKTS_NODE_NOT_SUPPORTED = 106103`）

#### Scenario: announce/focus 两类组件行为一致（HarmonyOS）

- **GIVEN** 一个 CAPI 内置组件 A 和一个 ArkTS 转发组件 B
- **WHEN** 分别调用 `A.accessibilityAnnounce("hi")` 与 `B.accessibilityAnnounce("hi")`
- **THEN** 两者 SHALL 通过同一条 ArkTS 桥路径最终调用 `@ohos.accessibility.sendAccessibilityEvent`，行为等价


### Requirement: 文档补齐

`docs/API/components/basic-attr-event.md` SHALL 补充 `accessibility`、`accessibilityRole`、`accessibilityInfo`、`accessibilityAnnounce`、`accessibilityFocus` 五项 API 的完整说明，含跨端支持矩阵（Android / iOS / HarmonyOS）与鸿蒙特有的 role 映射规则；`docs/DevGuide/` 下 SHALL 新增一份"鸿蒙自定义 ArkTS 组件无障碍接入指南"。

#### Scenario: API 文档包含跨端差异说明

- **WHEN** 开发者查阅 `basic-attr-event.md` 中的 `accessibilityRole` 章节
- **THEN** 文档 SHALL 展示 role 枚举在 Android / iOS / HarmonyOS 三端的映射对照表
- **AND** 明确说明鸿蒙 SEARCH → `TEXT_INPUT`、NONE → `MODE_DISABLED` 的降级规则

#### Scenario: 业务方接入指南可复制粘贴

- **WHEN** 业务方阅读 `docs/DevGuide/ohos-custom-accessibility.md`
- **THEN** 文档 SHALL 提供一个完整的 `@Component build()` 代码片段作为模板，展示如何应用 `.id()` / `.accessibilityText()` / `.accessibilityLevel()` / `.accessibilityGroup()`
- **AND** 说明为什么子节点需要 `.accessibilityLevel('no')`
