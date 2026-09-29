# Proposal: OHOS 自定义实现状态栏点击回顶（back-to-top）

## Why

HarmonyOS API 15 起 ArkUI 提供"点击状态栏让滚动容器回到顶部"的能力（`NODE_SCROLL_BACK_TO_TOP`，API ≥ 18 时纵向滚动容器默认值为 1）。Kuikly 的 OHOS 渲染层从未设置过该属性，导致：

- 业务**无法拦截、也无法替换**该行为：系统在渲染层完全不感知的情况下滚动，页面状态（分页、曝光、联动定位）会出现与 iOS 不一致的表现；
- 三端语义不对齐：iOS 早有 `scrollToTop` 回调 + `scrollViewShouldScrollToTop:` 语义，Kotlin 侧 `ScrollerView.scrollToTop{}` / `ScrollerEventConst.SCROLL_TO_TOP` / Compose `Modifier.scrollToTop` 都是跨平台 API，**唯独 OHOS 是缺口**。

本 change 关闭系统默认能力，改由渲染层自行实现回顶，补齐 OHOS 能力并让三端行为一致。

## What Changes

- **关闭系统默认回顶（行为变更）**：`KRScrollerView` 在创建 Scroll 节点后设置 `NODE_SCROLL_BACK_TO_TOP = 0`（仅 API ≥ 15 设置，低版本跳过）。关闭后默认回顶行为由本 change 自行实现，业务侧无需改动。
- 新增 C++ 公共事件单例 `KRCommonEventManager`（`core-render-ohos/src/main/cpp/libohos_render/expand/events/`），对外提供：
  - `Subscribe(CommonEventName name, const std::string &instanceId)`
  - `Unsubscribe(CommonEventName name, const std::string &instanceId)`

  内部维护 `event name -> set<instanceId>` 映射与引用计数：首个订阅者时向系统发起订阅，最后一个退订者移除后向系统退订，避免空订阅常驻。底层使用 `BasicServicesKit/oh_commonevent.h`（`OH_CommonEvent_CreateSubscribeInfo` / `OH_CommonEvent_CreateSubscriber` / `OH_CommonEvent_Subscribe` / `OH_CommonEvent_UnSubscribe` / `OH_CommonEvent_GetEventFromRcvData`），需链接 `libohcommonevent.so`。
- 新增 `CommonEventName` 枚举，首值 `COMMON_EVENT_CLICK_STATUSBAR`（映射系统事件名 `usual.event.CLICK_STATUSBAR`）。
- `IKRRenderView` 新增 `OnCommonEvent(CommonEventName name)` 虚函数；`KRRenderView` 提供实现：公共事件回调按事件名取出 instanceId，通过 `KRRenderManager::GetInstance().GetRenderView(instanceId)` 找到实例后逐个下发，查不到的 instanceId（已销毁/未创建）直接跳过。
- `KRRenderView` 维护自身 active 状态：由 `viewDidAppear` / `viewDidDisappear` 事件（现有 `DispatchInitState(kStateResume/kStatePause)` 路径）驱动，`OnCommonEvent` 仅在 active 时处理，非活跃实例忽略事件。
- `KRRenderView::OnCommonEvent` 在 active 时遍历本实例的视图树，对滚动容器调用 `OnStatusBarClicked()`。当前 OHOS 渲染层只有 `KRScrollerView` 一类滚动容器（`Scroller` 与 `List` 均注册到该类），因此遍历时仅需命中它。
- `KRScrollerView::OnStatusBarClicked()`：语义对齐 iOS `scrollViewShouldScrollToTop:`——
  - 业务已注册 `scrollToTop` 回调：触发回调，**渲染层不回顶**（与 iOS 返回 `NO` 一致，交业务自行处理）；
  - 未注册回调：渲染层自行滚动到顶部（补回被关闭的系统默认行为）。
- `KRScrollerView` 支持 Kotlin 侧已存在的 `scrollToTop` 事件 prop（`ScrollerEventConst.SCROLL_TO_TOP`），命名与 iOS/Android 保持一致。

## Non-goals

- 不修改 iOS / Android / Web / 小程序 的回顶实现；iOS 的 `scrollViewShouldScrollToTop:` 仅作为语义参考。
- 不覆盖 ArkTS 侧的滚动容器（`KRForwardArkTSView` 及其承载的上层 ArkTS List/Grid 等），它们不在 C++ 视图树内，本期不纳入分发范围。
- 不新增 Kotlin 侧公共 API：`scrollToTop` 事件与 Compose `Modifier.scrollToTop` 已存在，本次只在 OHOS 渲染层补齐实现。
- 不引入可配置的回顶动画参数（时长/曲线/部分回顶），默认行为对齐系统：滚到 offset 0。
- 不做多实例共享订阅以外的优化（如事件去抖、批量分发合并）。

## Capabilities

### New Capabilities

- `ohos-back-to-top`: OHOS 渲染层的状态栏点击回顶能力——系统默认能力关闭策略、公共事件订阅与引用计数生命周期、实例 active 过滤、`Scroller`/`List` 回顶分发，以及 `scrollToTop` 回调语义（有回调交业务、无回调走默认回顶）。

### Modified Capabilities

- 无。`openspec/specs/` 下现有 capability 均不涉及滚动容器的回顶行为，本次不修改任何既有 spec。

## Impact

- **受影响平台**：HarmonyOS（`NODE_SCROLL_BACK_TO_TOP` 需 API ≥ 15，公共事件订阅已在本机 API 21 模拟器实测可收到 `usual.event.CLICK_STATUSBAR`）。其余平台仅作为行为对齐参考，无代码改动。
- **受影响模块**：
  - `core-render-ohos`（主要）：新增 `KRCommonEventManager` / `CommonEventName`；`IKRRenderView` / `KRRenderView` 增加 `OnCommonEvent` 与 active 状态；`KRScrollerView` 增加 `NODE_SCROLL_BACK_TO_TOP` 关闭、`scrollToTop` prop 与 `OnStatusBarClicked()`；`KRRenderLayerHandler` 需要新增只读的视图遍历接口（现有 `view_registry_` 未对外暴露）。
  - `core`：无改动（`scrollToTop` 事件已定义）。
  - `demo`：补 OHOS 回顶验证页面（分别覆盖"注册 `scrollToTop` 回调"与"未注册"两种用例，含 `Scroller` 与 `List`）。
- **依赖与构建**：`core-render-ohos` 的 CMake 需新增链接 `libohcommonevent.so`（SDK 自带，arm64-v8a / x86_64 / arm 均有）。
- **风险**：
  - 公共事件订阅在不同系统版本上可能受权限策略限制（本机 API 21 可用，低版本/真机需回归）；
  - 关闭系统属性后，任何未被遍历覆盖到的滚动容器（如 ArkTS 侧容器）将失去回顶能力，需在文档中说明；
  - 遍历视图树在滚动容器数量大时会有额外开销，需控制为仅在事件到达时执行。
