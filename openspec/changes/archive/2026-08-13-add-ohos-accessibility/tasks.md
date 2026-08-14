## 1. cpp（`core-render-ohos`）— CAPI 内置组件属性映射

- [x] 1.1 `libohos_render/utils/KRViewUtil.h` 声明 `UpdateNodeAccessibilityRole(node, roleStr)` / `UpdateNodeAccessibilityActions(node, infoStr)` 两个新工具函数
- [x] 1.2 `libohos_render/utils/KRViewUtil.cpp` 实现 `UpdateNodeAccessibilityRole`：按 D3 映射表处理 button/text/image/checkbox/search（`setAttribute(NODE_ACCESSIBILITY_ROLE, ArkUI_NodeType)`），以及 none（`setAttribute(NODE_ACCESSIBILITY_MODE, ARKUI_ACCESSIBILITY_MODE_DISABLED)`）
- [x] 1.3 `libohos_render/utils/KRViewUtil.cpp` 实现 `UpdateNodeAccessibilityActions`：解析 `"clickable longClickable"` 位串（如 `"1 0"`）→ 位或 `ARKUI_ACCESSIBILITY_ACTION_CLICK | ARKUI_ACCESSIBILITY_ACTION_LONG_CLICK` → `setAttribute(NODE_ACCESSIBILITY_ACTIONS)`
- [x] 1.4 `libohos_render/expand/components/base/KRBasePropsHandler.cpp` `SetPropWithoutAnimation` 新增 `accessibilityRole` 分支调 `UpdateNodeAccessibilityRole`
- [x] 1.5 `KRBasePropsHandler.cpp` `SetPropWithoutAnimation` 新增 `accessibilityInfo` 分支调 `UpdateNodeAccessibilityActions`
- [x] 1.6 `KRBasePropsHandler.cpp` `ResetProp` 新增 `accessibilityRole` / `accessibilityInfo` 的 `resetAttribute` 恢复逻辑（同时 reset `NODE_ACCESSIBILITY_MODE` 以覆盖 none 情形）
- [x] 1.7 在 `KRBasePropsHandler.cpp` 顶部的 `kAccessibility*` 常量表补齐 `kAccessibilityRole` / `kAccessibilityInfo` 两个字符串常量（对齐现有 `kAccessibility` 风格）

## 2. cpp（`core-render-ohos`）— announce/focus 方法桥接

- [x] 2.1 `libohos_render/export/IKRRenderViewExport.cpp` `CallMethod` 增加 `accessibilityAnnounce` 拦截：调 `KRArkTSManager::CallArkTSMethod(GetInstanceId(), CallViewMethod, viewTag, "accessibilityAnnounce", params, ...)`
- [x] 2.2 同文件 `CallMethod` 增加 `accessibilityFocus` 拦截：同样桥到 `CallViewMethod` + `"accessibilityFocus"`
- [x] 2.3 `KRForwardArkTSViewV2::CallMethod` 已经通过 `CallViewMethod` 桥接，验证 announce/focus 走它时不会被基类拦截覆盖（若有冲突，改为在基类只拦截 CAPI 组件、ArkTS 转发组件保留原逻辑）

## 3. ets（`core-render-ohos`）— ArkTS 转发组件属性字段

- [x] 3.1 `src/main/ets/components/base/KRBaseViewExport.ets` `KuiklyRenderBaseView` 类新增 `cssAccessibilityText: string | null = null` 字段
- [x] 3.2 同类新增 `cssAccessibilityRole: string | null = null` 字段
- [x] 3.3 同类新增 `cssAccessibilityInfo: string | null = null` 字段（保留原始 `"clickable longClickable"` 串，业务方按需拆解）
- [x] 3.4 `KuiklyRenderBaseView.setProp` 新增 `accessibility` case 赋值 `cssAccessibilityText`
- [x] 3.5 `setProp` 新增 `accessibilityRole` case 赋值 `cssAccessibilityRole`
- [x] 3.6 `setProp` 新增 `accessibilityInfo` case 赋值 `cssAccessibilityInfo`
- [x] 3.7 三个 case 处理完后调用 `this.updateArkUI()`（触发业务方 `@Component` 重新渲染）

## 4. ets（`core-render-ohos`）— announce/focus 方法基类实现

- [x] 4.1 `KuiklyRenderBaseView` 从 `abstract call(...)` 改为可默认实现（去掉 abstract 修饰），提供基类分发；业务方可 override 但需 `super.call(...)` 兜底
- [x] 4.2 基类 `call()` 增加 `accessibilityAnnounce` 分支：`import accessibility from '@ohos.accessibility'`，构造 `EventInfo('announceForAccessibility', bundleName, 'common')`，赋 `textAnnouncedForAccessibility = params`，调 `accessibility.sendAccessibilityEvent(ev)`；`bundleName` 从 `this.getUIContext()?.getHostContext()?.applicationInfo?.name` 取
- [x] 4.3 基类 `call()` 增加 `accessibilityFocus` 分支：构造 `EventInfo('requestFocusForAccessibility', bundleName, 'common')`，赋 `customId = this.getNodeId()`，调用同样 API
- [x] 4.4 两个分支的 catch 块 log `console.error`，NOT 抛未捕获异常；`bundleName` 空时先兜底 `getContext(this).applicationInfo.name`，仍空则日志后 no-op
- [x] 4.5 更新既有子类（如 `KRMyDemoCustomView`）的 `call()` 实现：改为先 `super.call(method, params, callback)` 再处理自定义 method，避免破坏已有业务

## 5. Demo 与业务方 ArkTS 组件示例

- [x] 5.1 新建 `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/AccessibilityTestPage.kt`（`@Page("AccessibilityTestPage")`）：A 组 4 例覆盖 `accessibility` 文本（CAPI/响应式/ArkTS/空串 reset）；B 组 8 例覆盖 `accessibilityRole` 全枚举（含 SEARCH 降级、NONE 剔除、NONE↔BUTTON 切换的 MODE 重置、ArkTS role）；C 组 3 例覆盖 `accessibilityInfo`
- [x] 5.2 同文件 D/E 组共 7 例覆盖 `accessibilityAnnounce`（CAPI/ArkTS/长文本/连续时序）与 `accessibilityFocus`（CAPI/ArkTS/跨视口），通过 `ViewRef<DivView>` / `ViewRef<MyDemoCustomView>` 拿引用；同时在 `catalog/ExampleIndexPage.kt` 加入口
- [x] 5.3 `ohosApp/entry/src/main/ets/kuikly/components/KRMyDemoCustomView.ets`：`@Component` 外层 Stack 应用 `.id(this.renderView.getNodeId())` + `.accessibilityText(this.renderView.cssAccessibilityText ?? "")` + `.accessibilityLevel("yes")` + `.accessibilityGroup(true)`
- [x] 5.4 同文件子 `Text` / `Button` 加 `.accessibilityLevel('no')`（防抢焦点，作为业务方参考）
- [x] 5.5 同文件 `setProp` 里删除 spike 期间加的临时 `console.info` 日志，保留 case 分支即可

### 5A. Android/iOS Demo 侧 `KRMyDemoCustomView` 原生补齐

历史上 `KRMyDemoCustomView`（kotlin 侧 `viewName() = "KRMyDemoCustomView"`）只有鸿蒙端原生实现，Android/iOS 未覆盖。为了让 `AccessibilityTestPage` D 组「自定义 View 作为焦点容器」的用例三端可跑通，本次一并补齐。

- [x] 5A.1 `androidApp/src/main/java/com/tencent/kuikly/android/demo/KRMyDemoCustomView.kt`：继承 `KRView`（`FrameLayout` 派生），实现黄底黑边 + 居中 `TextView` + `Tap Me` `Button` + 承载 kuikly 子节点；`setProp` 处理 `message` / `onMyViewTapped`
- [x] 5A.2 `androidApp/src/main/java/com/tencent/kuikly/android/demo/ContextCodeHandler.kt` `registerExternalRenderView` 注册 `renderViewExport(KRMyDemoCustomView.VIEW_NAME, ...)`
- [x] 5A.3 `iosApp/iosApp/KuiklyRenderExpand/Views/KRMyDemoCustomView.h/.m`：继承 `KRView`（复用其 `hrv_callWithMethod:` 中 `accessibilityAnnounce` / `accessibilityFocus` 实现），黄底黑边 + `UILabel` + `UIButton`；`hrv_setPropWithKey:` 走 `KUIKLY_SET_CSS_COMMON_PROP`，`setCss_message:` / `setCss_onMyViewTapped:` 处理自定义属性；`didAddSubview:` 保证 kuikly 子节点始终位于装饰层之上
- [x] 5A.4 `iosApp/iosApp.xcodeproj/project.pbxproj`：在 `Views` group、`PBXBuildFile` / `PBXFileReference` / `PBXSourcesBuildPhase` 段落各加入 `KRMyDemoCustomView.h/.m` 引用（对齐 `KRTabbarView` 的 6 处占位）
- [x] 5A.5 A11y 属性通道说明：Android 走 `KRCSSViewExtension.setCommonProp` 的 `accessibility` / `accessibilityRole` / `accessibilityInfo` 分支（自动生效），iOS 走 `UIView+CSS.m` 的 `css_accessibility*` setter（自动生效）；两端均不需在 `KRMyDemoCustomView` 内重写 A11y 逻辑，与鸿蒙侧 ArkTS 转发 View 手动应用修饰器的做法不同（因鸿蒙 CAPI 属性通道无法进入 ArkTS 节点）

## 6. 文档

- [x] 6.1 `docs/API/components/basic-attr-event.md` 扩展现有 `accessibility方法` 一节：说明它对应 ArkTS `accessibilityText` / Android `contentDescription` / iOS `accessibilityLabel`，跨端一致
- [x] 6.2 新增 `accessibilityRole方法` 一节：完整枚举列表 + 三端映射表（Android → 类名，iOS → `accessibilityTraits`，HarmonyOS → `ArkUI_NodeType` / `ArkUI_AccessibilityMode`），明确 SEARCH/NONE 的降级规则
- [x] 6.3 新增 `accessibilityInfo方法` 一节：`clickable` / `longClickable` 参数说明 + 三端映射
- [x] 6.4 新增 `accessibilityAnnounce方法`（DeclarativeBaseView 上的实例方法）与 `accessibilityFocus方法` 两节：说明各端底层 API
- [x] 6.5 新增 `docs/DevGuide/ohos-custom-accessibility.md`：给业务方 ArkTS 组件接入的最小模板（含 `.id() + .accessibilityText() + .accessibilityLevel() + .accessibilityGroup()` 组合 + 子节点 `.accessibilityLevel('no')` 说明）+ 完整可复制的 `@Component build()` 示例
- [x] 6.6 检查 `docs/API/components/basic-attr-event.md` 顶部或对应位置是否需要增补跨端支持矩阵行（已在每个 accessibility 小节内嵌三端映射表，无需额外顶部矩阵）

## 7. 验证

> 实际验证方式：不再使用 `CustomViewExamplePage` + HiLog，改为通过新建的 `AccessibilityTestPage`（A/B/C/D/E 组共 20+ 用例）在鸿蒙真机上逐项走查，观察屏幕朗读器播报结果。详见 [demo/.../AccessibilityTestPage.kt](../../../demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/AccessibilityTestPage.kt)。

- [x] 7.1 鸿蒙真机开启屏幕朗读，进入 `AccessibilityTestPage`
- [x] 7.2 CAPI 组件通过 A 组（accessibility 文本）+ B 组（role 全枚举）用例验证：观察播报文本/角色符合预期
- [x] 7.3 ArkTS 转发组件 `MyDemoCustom` 通过 B8 用例验证：读屏播报"我是ArkTS按钮 ... 按钮"（`.accessibilityText/.accessibilityRole/.accessibilityLevel/.accessibilityGroup` + 基类 `resolveArkUIAccessibilityRole` 全部生效）
- [x] 7.4 D 组 announce 用例验证：修复 CAPI view 未注册导致的静默失效（`KRNativeInstance.callViewMethod` 兜底 + cpp 侧改传 nodeId），D1~D4 均正常播报
- [x] 7.5 E 组 focus 用例验证：CAPI focus 走全局兜底后焦点跳转正常
- [x] 7.6 关闭 → 重开屏幕朗读回归；C 组 accessibilityInfo 通过 `NODE_ACCESSIBILITY_DESCRIPTION` + 资源本地化播报"双击激活/双击并长按"（对齐 iOS `accessibilityHint`）

## 8. Spike 分支清理与 PR

- [x] 8.1 清理 `../KuiklyUI-a11y-spike` worktree 中所有 `SPIKE-A11Y` 标记的临时代码
- [x] 8.2 有未提交改动先 `git stash`；删除 worktree：`git worktree remove ../KuiklyUI-a11y-spike && git branch -D a11y-spike`
- [x] 8.3 将 spike 结论中的关键事实（如 106103 错误码、`getHostContext` 取 bundleName、修饰器组合规则）以内联注释形式保留在实现代码里，避免下次踩同样的坑
- [x] 8.4 提交前跑一遍 `openspec validate add-ohos-accessibility --strict`；跑 `kuikly-doc-archive-review` 确认 docs/.ai 是否需再同步（归档流程会自动触发 doc-archive-review）
- [x] 8.5 按 Angular Convention 提交：commit `58259c563` — `feat(ohos): localize accessibility action hints via string resources`（本地化提示、CAPI announce/focus 兜底、AccessibilityRole 映射、Android/iOS CustomView 补齐等改动一并纳入）
