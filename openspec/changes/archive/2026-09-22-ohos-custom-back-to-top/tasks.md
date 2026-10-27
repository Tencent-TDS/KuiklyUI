# Tasks

> 设计依据：`design.md`（D1–D10）；行为契约：`specs/ohos-back-to-top/spec.md`。
> 所有 OHOS 编译验证命令：`cd ohosApp && hvigorw assembleHap --mode module -p module=entry@default -p product=default -p buildMode=debug --no-daemon`（首次需先 `./2.0_ohos_demo_build.sh` 产出 `libshared.so`）。

## 1. core-render-ohos：公共事件基础设施（D3 / D8）

- [x] 1.1 新增 `src/main/cpp/libohos_render/expand/events/common_event/KRCommonEventManager.h`：定义 `CommonEventName` 枚举（首值 `COMMON_EVENT_CLICK_STATUSBAR` → `"usual.event.CLICK_STATUSBAR"`）与单例类骨架；验证：`hvigorw assembleHap` 编译通过
- [x] 1.2 实现 `Subscribe(CommonEventName, const std::string &instanceId)` / `Unsubscribe(...)` 的 `event → std::set<instanceId>` 映射与引用计数（集合由空变非空才订阅、变空才退订）；验证：模拟器实跑，单实例场景日志显示仅 1 次系统订阅，无重复订阅
- [x] 1.3 封装系统调用（`OH_CommonEvent_CreateSubscribeInfo` / `OH_CommonEvent_CreateSubscriber` / `OH_CommonEvent_Subscribe` / `OH_CommonEvent_UnSubscribe` / `OH_CommonEvent_GetEventFromRcvData`），处理返回码并记录失败日志；验证：实现返回码分支（`COMMONEVENT_ERR_OK` 之外打印错误码并释放句柄），失败不崩溃由 5.3 间接覆盖
- [x] 1.4 提供 `IsSubscribed(CommonEventName)` 查询接口（供 D8 fail-open 判定）；验证：`KRBackToTop` 日志中订阅前 `subscribed=0`、订阅后 `subscribed=1`，取值符合预期
- [x] 1.5 回调处理：从 `CommonEvent_RcvData` 取事件名 → 映射为 `CommonEventName` → 切主线程后按 D7 分发；验证：API 24 模拟器点状态栏日志 `dispatch event=usual.event.CLICK_STATUSBAR, registered=1, dispatched=1`（注：线程切换最终用 `KRMainThread::RunOnMainThread`，见 design.md D7）
- [x] 1.6 `src/main/cpp/CMakeLists.txt` 的 `target_link_libraries(kuikly ...)` 增加 `libohcommonevent.so`；验证：`hvigorw assembleHap` 成功，产物 `libkuikly.so` 中 `strings | grep CLICK_STATUSBAR` 命中

## 2. core-render-ohos：实例生命周期与事件分发（D4 / D5 / D6 / D7）

- [x] 2.1 `libohos_render/view/IKRRenderView.h` 增加 `virtual void OnCommonEvent(CommonEventName name) {}`；验证：编译通过，既有子类/测试桩无需改动
- [x] 2.2 `KRRenderView` 增加 `active_` 状态并在 `viewDidAppear` / `viewDidDisappear` 驱动的 `kStateResume` / `kStatePause` 路径上更新；验证：按 Home 退到后台后点状态栏，日志出现 `skip ... instance is not active`
- [x] 2.3 `KRRenderView::Init()` 开头（core 初始化之前）`Subscribe(COMMON_EVENT_CLICK_STATUSBAR, instanceId)`，在 `WillDestroy` / 析构路径 `Unsubscribe`；验证：订阅已在模拟器实测生效（提前到 core 初始化前，见 design.md D5）；**退订的运行期验证待补**（CLI 无法让该 ability 走正常销毁流程）
- [x] 2.4 `KRRenderView::OnCommonEvent` 实现：主线程 → 判 `active_` → 遍历视图树调用 `OnStatusBarClicked()`；验证：后台时点状态栏无回顶且打 skip 日志；回前台后恢复响应
- [x] 2.5 `libohos_render/layer/IKRRenderLayer.h` + `KRRenderLayerHandler.{h,cpp}` 增加只读遍历接口（`ForEachRenderView`，基于 `view_registry_` 快照）；验证：同页 3 个滚动容器全部被遍历到（1 个回调触发 + 2 个默认回顶）

## 3. core-render-ohos：滚动容器回顶（D2 / D4 / D9 / D10）

- [x] 3.1 `libohos_render/export/IKRRenderViewExport.h` 增加 `virtual void OnStatusBarClicked() {}` 默认空实现；验证：编译通过
- [x] 3.2 `KRScrollerView` 关闭系统内置回顶（`NODE_SCROLL_BACK_TO_TOP = 0`），门控条件为 `OH_GetSdkApiVersion() >= 15 && KRCommonEventManager::IsSubscribed(CLICK_STATUSBAR)`（D8），并在 `DidMoveToParentView` 幂等兜底；验证：日志 `disable system back-to-top: node=0x..., result=0`，点状态栏后系统不再自动滚动（改动前基线 690 → 0）
- [x] 3.3 `KRScrollerView::SetProp` 支持 `scrollToTop` 事件 prop（复用 `ScrollerEventConst.SCROLL_TO_TOP` / iOS 同名 prop），存 `KRRenderCallback` 并在 `ResetProp` 中清理；验证：Kotlin 侧 `scrollToTop {}` 注册后回调计数按次递增
- [x] 3.4 `KRScrollerView::OnStatusBarClicked()`：已注册回调 → 触发回调且不滚动；未注册 → 滚动到 offset 0（对齐 iOS `scrollViewShouldScrollToTop:`）；验证：见 5.1

## 4. demo：验证页面

- [x] 4.1 新增 OHOS 回顶验证页（`demo/.../OhosBackToTopDemoPage.kt`，`@Page("OhosBackToTopPage")`）：滚到中部后点状态栏应回到顶部
- [x] 4.2 同页增加"已注册 `scrollToTop` 回调"用例（页面显示回调触发次数，且不自动回顶）
- [x] 4.3 增加 List 用例（同一 C++ 容器类）：点状态栏可回顶
- [x] 4.4 混合用例即同一页面内三者共存，行为分化已实测（见 5.1/5.4）

## 5. 平台验证（HarmonyOS 为主，其余为回归）

- [x] 5.1 HarmonyOS 模拟器端到端（HarmonyOS 6.1 / API 24 emulator，`hdc` + `uitest`）：① 无回调 Scroller 178.43 → 点状态栏 → **0.0**；② 有回调 Scroller 181.10 → 点状态栏 → **保持 181.10**、`callbackCount` 1→2；③ List 66.62 → 点状态栏 → **≈0**；④ 切后台点状态栏 → `skip ... not active`，回前台后恢复响应
- [ ] 5.2 低版本降级：需要 API < 15 的设备/镜像才能实测；当前由 `IsScrollBackToTopApiAvailable()` 门控 + 代码审查保证（未跑）
- [x] 5.3 fail-open：**间接验证**——在订阅生效前创建容器的中间版本上，属性未被关闭，系统默认回顶仍生效（即"订阅失败 → 保持系统能力"这条路径的行为符合预期）；未构造订阅返回错误码的强制场景
- [x] 5.4 多容器：同页 3 个滚动容器（2 Scroller + 1 List）全部被遍历到，混合行为（1 个回调接管 + 2 个默认回顶）分别正确
- [x] 5.5 iOS 回归：本次改动未触及 `core-render-ios`（`git diff` 范围仅 OHOS + demo），iOS 行为由代码范围保证不变
- [x] 5.6 Android 回归：同上，未触及 Android 渲染层

## 6. 收尾

- [x] 6.1 清理调试日志与临时开关；验证：`git diff --stat` 仅含预期文件，`KRBackToTop` 仅保留一行有效日志，无遗留调试输出
- [ ] 6.2 判断是否需要同步文档（`docs/` 与 `.ai/`），按 doc-archive-review 结论更新 OHOS 回顶限制说明（ArkTS 侧容器不覆盖、API 版本要求）；验证：产出清单或确认无需改动
- [ ] 6.3 验收通过后归档 change（`openspec archive ohos-custom-back-to-top`）并确认 `openspec/specs/ohos-back-to-top/spec.md` 已生成；验证：`openspec list --specs` 中出现新 capability
