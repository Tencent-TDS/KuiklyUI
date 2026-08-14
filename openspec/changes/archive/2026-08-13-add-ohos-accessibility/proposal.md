## Why

Kuikly `core` 层已经暴露 `accessibility` / `accessibilityRole` / `accessibilityInfo` 三个属性与 `accessibilityAnnounce` / `accessibilityFocus` 两个方法（Android、iOS 端均已实现），但 HarmonyOS 端 `core-render-ohos` 里除 `accessibility` 文本外，其余属性与两个方法**完全没有实现**。这导致业务方在鸿蒙上无法为界面提供有意义的无障碍语义（角色、可点击提示、主动播报、焦点跳转），影响残障用户可访问性与合规性。文档中 `accessibility` 一节也缺失对完整能力与各端差异的说明。

## What Changes

- **cpp（CAPI 组件）** `core-render-ohos/src/main/cpp/libohos_render/expand/components/base/KRBasePropsHandler.*`：
  - 新增 `accessibilityRole` prop 分发：映射到 `NODE_ACCESSIBILITY_ROLE`（`ArkUI_NodeType`），特殊值 `none` 走 `NODE_ACCESSIBILITY_MODE = ARKUI_ACCESSIBILITY_MODE_DISABLED`
  - 新增 `accessibilityInfo` prop 分发：解析 `"clickable longClickable"` 位串 → 映射到 `NODE_ACCESSIBILITY_ACTIONS`（`ArkUI_AccessibilityActionType` 位或）
  - 沿用现有 `accessibility` → `NODE_ACCESSIBILITY_TEXT` 映射，不改动
- **cpp（通用能力）** `core-render-ohos/src/main/cpp/libohos_render/export/IKRRenderViewExport.cpp`：
  - `CallMethod` 拦截 `accessibilityAnnounce` / `accessibilityFocus`，通过 `KRArkTSManager::CallArkTSMethod(CallViewMethod, ...)` 桥接到 ArkTS 侧统一实现（不区分内置/转发 view）
- **ets（ArkTS 转发组件）** `core-render-ohos/src/main/ets/components/base/KRBaseViewExport.ets`：
  - `KuiklyRenderBaseView` 新增 `cssAccessibilityText / cssAccessibilityRole / cssAccessibilityInfo` 字段与对应 `setProp` 分支
  - 新增 `call()` 基类实现，拦截 `accessibilityAnnounce` / `accessibilityFocus`，通过 `@ohos.accessibility.sendAccessibilityEvent` 触发（`bundleName` 从 `getUIContext().getHostContext().applicationInfo.name` 动态获取）
  - 提供 SDK 侧工具方法 / 修饰器扩展函数，业务方在自己 `@Component build()` 里应用一次即可
- **业务方 ArkTS 组件（约定）**：`.id(this.renderView.getNodeId())` + `.accessibilityText(this.renderView.cssAccessibilityText ?? '')` + `.accessibilityLevel('yes')` + `.accessibilityGroup(true)`，且子节点需 `.accessibilityLevel('no')` 防止抢焦点（文档指导）
- **文档** `docs/API/components/basic-attr-event.md`：补齐 `accessibility` / `accessibilityRole` / `accessibilityInfo` / `accessibilityAnnounce` / `accessibilityFocus` 五项说明，标注各端支持差异与 role 枚举的鸿蒙映射表；`docs/DevGuide/` 补充"鸿蒙自定义 ArkTS 组件如何接入无障碍"指引
- **Demo** `demo/.../CustomViewExamplePage.kt` + `demo/.../AccessibilityTestPage.kt` + `ohosApp/.../KRMyDemoCustomView.ets`：加入示例，作为业务方参考模板
- **Android / iOS Demo 侧对齐**：`androidApp/.../KRMyDemoCustomView.kt` 与 `iosApp/.../KRMyDemoCustomView.h/.m` 补齐（历史遗留：`viewName() = "KRMyDemoCustomView"` 三端只有鸿蒙有原生实现），以保证 `AccessibilityTestPage` D 组"自定义 View 作为焦点容器"用例在三端可跑通；两端 A11y 属性直接走各自 render 层的通用通道，无需额外适配代码

## Capabilities

### New Capabilities

- `ohos-accessibility`: HarmonyOS 端的无障碍属性/方法实现规范，覆盖 CAPI 内置组件与 ArkTS 转发组件两条渲染路径

### Modified Capabilities

（无。当前 openspec/specs/ 下无既有的 accessibility spec，此次为纯新增。）

## Impact

- **平台**：仅 HarmonyOS（Android / iOS 已实现且行为对齐，此次是向 HarmonyOS 端补齐）
- **模块**：
  - `core-render-ohos` cpp 侧（KRBasePropsHandler、IKRRenderViewExport 的 CallMethod、KRViewUtil）
  - `core-render-ohos` ets 侧（KuiklyRenderBaseView 基类）
  - `docs/` 文档
  - `demo/` 与 `ohosApp/` 示例
- **不改动**：`core/` KMP core 层（现有 `Attr.accessibility*` / `DeclarativeBaseView.accessibility*` 已足够）、`core-render-android`、`core-render-ios`、`core-render-web`
- **对业务方**：接入自定义 ArkTS 转发组件的业务方**需要**在自己 `@Component build()` 里增加 4~5 行修饰器（SDK 侧无法基类统一注入，ArkTS 语法限制）；使用 Kuikly 内置组件的业务方**零改动**
- **不做的事（Non-goals）**：
  - 不新增 kotlin `core` 层 API（`accessibilityHint`、`AccessibilityRole.LINK` 等更多角色，留待后续独立 change）
  - 不实现 `AccessibilityProvider`（`ArkUI_AccessibilityProvider` 只在 `ARKUI_NODE_CUSTOM` 上可用，Kuikly 节点走的是 `ARKUI_NODE_STACK` 等标准节点，路径不通）
  - 不改动现有 `KRArkTSViewBasePropsHandler` 的 no-op 语义（Spike 0 已证 `setAttribute` 会返回 106103 `ARKUI_ERROR_CODE_ARKTS_NODE_NOT_SUPPORTED`，属 SDK 硬性限制）
  - 不涉及非无障碍焦点（`OH_ArkUI_FocusRequest` / `NODE_FOCUS_STATUS` 是输入焦点，语义不同）
