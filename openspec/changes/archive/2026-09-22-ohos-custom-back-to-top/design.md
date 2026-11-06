# Design

## Context

背景与动机见 `proposal.md`。以下是影响方案选择的现状与约束（均已核对代码与 SDK）：

**实例模型**
- 每个 Kuikly 页面在 OHOS 渲染层对应一个 `KRRenderView`，持有 `instanceId`，登记在 `KRRenderManager`（`std::unordered_map<std::string, std::shared_ptr<KRRenderView>>` + `KRSpinLock`，`GetRenderView(instanceId)` 任意线程可查）。
- 实例创建由 ArkTS `createNativeRoot(contentHandle, instanceId)` 触发；销毁分两步：`DestroyRenderView`（通知 `WillDestroy`）→ `DestroyRenderViewCallBack`（真正从表中 erase）。
- `KRRenderView::SendEvent` 已在 `viewDidAppear` / `viewDidDisappear` 上驱动 `KRInitState::kStateResume` / `kStatePause`（`core/KRRenderCore.h:29` 的枚举），活跃状态无需新建机制。

**滚动容器**
- OHOS 渲染层只有 `KRScrollerView` 一类滚动容器（`ARKUI_NODE_SCROLL`），且自研 DSL 的 `Scroller` 与 `List` 都注册到该类（`expand/components/ComponentsRegisterEntry.h:70`）。
- 视图注册表 `KRRenderLayerHandler::view_registry_`（tag → view）目前**没有对外遍历接口**，需要新增。

**ArkUI 能力**
- `NODE_SCROLL_BACK_TO_TOP = 1002021`，`@since 15`；API ≥ 18 时纵向滚动容器默认值为 1（即默认开启）。Kuikly 从未设置过该属性——已在模拟器实测：点击状态栏时 `Scroller` 从 offset 690 自行滚回 0，且业务无任何感知。
- 公共事件 NDK：`BasicServicesKit/oh_commonevent.h`，`@library libohcommonevent.so`，`@since 12`，syscap `SystemCapability.Notification.CommonEvent`；三架构（arm64-v8a / x86_64 / arm）so 齐全。已在 API 21 模拟器实测：第三方 App 订阅 `usual.event.CLICK_STATUSBAR` 不抛异常且能收到回调（`data.parameters` 为空）。
- 版本门控已有范式：`KRScrollerView::IsFlingSpeedLimitApiAvailable()` 用 `OH_GetSdkApiVersion() >= 18` 判断。

**线程模型**
- ArkUI 节点/属性操作要求在渲染主线程（相关入口均调 `KREnsureMainThread()`）；公共事件回调所在线程**不保证**是渲染主线程。
- Kotlin 侧回调沿用既有链路：`KRRenderCallback` → `KRRenderCore::CallKotlinMethod` → `callKotlin_`（由 Kotlin 通过 `com_tencent_kuikly_SetCallKotlin` 注入），该链路已在主线程触发。

## Goals / Non-Goals

**Goals**
- 在 OHOS 上把"点击状态栏回顶"的控制权收归框架：既能被业务拦截，也能在没有业务介入时保持默认回顶。
- 只暴露一个可扩展的常量位（当前仅"状态栏点击"），后续新增系统事件时无需改动分发骨架。
- 对 Kotlin/业务侧零 API 变更（复用既有 `scrollToTop`）。

**Non-Goals（设计层面）**
- 不改造既有 scroll/scrollEnd/dragBegin 等事件链路及其线程模型。
- 不为 ArkTS 侧滚动容器（`KRForwardArkTSView` 承载的容器）提供回顶。
- 不做通用"系统事件总线"抽象（如支持任意事件名透传、事件参数解析、优先级/拦截链）。
- 不引入回顶动画可配置项（时长/曲线/部分回顶）。

## Decisions

### D1 事件订阅放在 C++ 渲染层，而非 ArkTS 宿主
- **选择**：在 `core-render-ohos` 的 C++ 层用公共事件 NDK 直接订阅。
- **为什么**：只有渲染层能按 `instanceId` 精确定位 `KRRenderView` 并访问其视图树；且 ArkTS 宿主不是 Kuikly 框架的必备件（业务可自行组织页面容器），把能力放在框架层才能保证行为一致。
- **备选**：宿主 ArkTS 订阅后经 `sendEvent` 下发。放弃原因：无法定位具体渲染实例、需要业务自行接入、且会引入 ArkTS↔native 的额外一跳。

### D2 一律关闭系统属性，默认回顶由渲染层自实现（方案 B）
- **选择**：所有由本渲染层创建的滚动容器统一设置 `NODE_SCROLL_BACK_TO_TOP = 0`。
- **为什么**：行为可预期、不依赖系统默认值随 API 版本变化（API<18 默认 0、≥18 默认 1），且能让"未注册回调"和"已注册回调"两种情形都由框架显式决定。
- **备选**：仅在业务注册了 `scrollToTop` 回调时才关闭属性（保留系统动画与物理特性）。放弃原因：行为随"是否注册回调"分叉、框架对默认路径无控制权，且与"感知事件"的需求不一致。
- **代价**：默认回顶动画由系统实现变为框架实现（见 Risks）。

### D3 单例 + 引用计数订阅模型
- **选择**：`KRCommonEventManager` 单例，内部 `std::map<CommonEventName, std::set<std::string /*instanceId*/>>` + 互斥保护；`Subscribe` 时插入，集合由空变非空才向系统订阅；`Unsubscribe` 时移除，集合变空才向系统退订。
- **为什么**：系统订阅是进程级资源，NDK 回调不携带实例信息，最终仍要在 C++ 侧按实例集合分发；集中管理可避免 N 个实例 N 次系统订阅。
- **备选**：每个实例各自订阅。放弃原因：重复占用系统资源，且回调中无法区分实例，仍需同样的映射表。

### D4 分发采用"遍历视图树 + 虚函数默认空实现"
- **选择**：在 `IKRRenderViewExport` 增加 `OnStatusBarClicked()`（默认空实现），`KRScrollerView` 覆写；`KRRenderView` 遍历本实例全部视图并对每个视图调用该方法。
- **为什么**：避免 `dynamic_cast` 类型判断；后续新增滚动容器（如瀑布流）只需覆写同一虚函数，分发侧不用改。
- **代价**：一次事件需要遍历全部视图（事件频率极低，可接受）。

### D5 实例生命周期与订阅/退订挂钩
- **选择**：`KRRenderView::Init()` **开头**（`core_` 初始化之前）就 `Subscribe(COMMON_EVENT_CLICK_STATUSBAR, instanceId)`；在实例销毁回调（`DestroyRenderViewCallBack` / `WillDestroy` 路径）时 `Unsubscribe`，析构函数兜底。
- **为什么**：与实例存活期一致，避免"页面不可见"导致的频繁订阅/退订抖动；是否响应由 active 状态决定（D6），两个维度正交。
- **实现说明（实测得出）**：页面视图树会在 `core_->DidInit()` 过程中被同步创建（`createInstance` → Kotlin 构建 body → native `createRenderView`）。当前把订阅放在 `Init()` 开头，使事件通道在首帧前就绪；`DidMoveToParentView` 会再幂等设置一次滚动容器的回顶属性，覆盖后续新增容器。原方案中"订阅必须先于容器创建"的强约束随 D8 修订一并消失。
- **备选**：每次 `viewDidAppear` 订阅 / `viewDidDisappear` 退订。放弃原因：前后台切换频繁时反复订阅退订，且订阅是有系统开销的 IPC 操作。

### D6 仅活跃实例响应，复用既有状态源
- **选择**：`KRRenderView` 依据既有 `kStateResume` / `kStatePause`（由 `viewDidAppear` / `viewDidDisappear` 驱动）维护 `active_`，`OnCommonEvent` 时先判活跃。
- **为什么**：不新增状态源，避免与页面生命周期不一致。

### D7 公共事件回调统一切回渲染主线程
- **选择**：回调中先通过 **`KRMainThread::RunOnMainThread`** 切到主线程，再做实例查找、视图遍历与节点操作。
- **为什么**：ArkUI 节点非线程安全；主线程执行也避免了后续给 `KRScrollerView` 引入额外锁。
- **实现约束（实测得出）**：公共事件回调运行在 **FFRT worker 线程**（fault log 中线程名 `OS_FFRT_2_0`），既不是主线程也不是 kuikly worker 线程。`KRContextScheduler::ScheduleTaskOnMainThread` 对"第三方线程调用"会直接 `__assert_fail`（`scheduler/KRContextScheduler.cpp:188`），因此**不能**用它做这里的线程切换；`KRMainThread::RunOnMainThread` 内部在非主线程时走 `uv_async_send`，是唯一安全入口。
- **备选**：在回调线程直接处理并加锁。放弃原因：节点操作依然必须在主线程，锁只增加复杂度。

### D8 订阅失败的处理（评审后修订：不做 fail-open 门控）
- **选择（修订后）**：滚动容器**无条件**关闭 `NODE_SCROLL_BACK_TO_TOP`（仅按系统 API ≥ 15 门控）；`OH_CommonEvent_Subscribe` 返回失败（权限/系统限制/异常返回码）视为系统级问题，只记录错误日志，不让上层行为依赖订阅结果。
- **为什么**：该接口返回成功即可认为订阅生效；失败属于设备/系统策略问题，为其引入"查询订阅状态 → 门控关属性 → 订阅成功后回补已存在容器"的机制收益过低，且顺带消除了"订阅必须先于容器创建"的强时序约束。
- **已知代价（显式接受）**：订阅失败的设备上点击状态栏不会回顶，仅错误日志可见。
- **留档（原方案，已移除）**：fail-open——只在订阅成功后关闭属性、失败时保持系统默认回顶，配套接口 `KRCommonEventManager::IsSubscribed()` 与容器侧门控均已删除。

### D9 DSL 适用范围
- **选择**：自研 DSL 与 Compose DSL **同时覆盖**，无需 DSL 专属改动。
- **为什么**：两种 DSL 的滚动容器在 OHOS 渲染层最终都落到同一个 `KRScrollerView`；`scrollToTop` 事件（自研 DSL `ScrollerView.scrollToTop{}`）与 Compose `Modifier.scrollToTop` 走的也是同一条事件 prop 通道。

### D10 NativeBridge 交互
- **选择**：**不新增** NativeBridge/Module 能力，不新增 Kotlin 公共 API。
- **数据流**：Kotlin 注册回调 → 既有事件 prop 通道（`KRRenderNativeMethodSetViewProp` / `SetEvent`）→ 渲染层保存 `KRRenderCallback`；状态栏点击发生时，由渲染层经既有回调链路（`KuiklyRenderContextMethodFireCallback` / `callKotlin_`）回传，签名与 iOS 一致（无参数）。

## 文件变更（按模块）

**core-render-ohos（主要）**
| 文件 | 变更 |
|---|---|
| `src/main/cpp/libohos_render/expand/events/common_event/KRCommonEventManager.{h,cpp}` | 新增：单例、`CommonEventName` 枚举、`Subscribe`/`Unsubscribe`、`event → set<instanceId>` 映射与引用计数、系统订阅/退订封装、`IsSubscribed(name)` 查询 |
| `src/main/cpp/libohos_render/view/IKRRenderView.h` | 新增：`virtual void OnCommonEvent(CommonEventName name)`（默认空实现，保证子类/测试桩不受影响） |
| `src/main/cpp/libohos_render/view/KRRenderView.{h,cpp}` | 新增：`active_` 状态维护、`OnCommonEvent` 实现（查实例→判活跃→遍历视图分发）、实例初始化订阅 / 销毁退订 |
| `src/main/cpp/libohos_render/export/IKRRenderViewExport.h` | 新增：`virtual void OnStatusBarClicked() {}`（默认空实现） |
| `src/main/cpp/libohos_render/expand/components/scroller/KRScrollerView.{h,cpp}` | 新增：`NODE_SCROLL_BACK_TO_TOP` 关闭（API ≥ 15 且订阅成功时）、`scrollToTop` 事件 prop 处理、`OnStatusBarClicked()` 覆写（有回调→回调；无回调→滚到顶部） |
| `src/main/cpp/libohos_render/layer/KRRenderLayerHandler.{h,cpp}`、`layer/IKRRenderLayer.h` | 新增：只读遍历接口（如 `ForEachRenderView(const std::function<void(const std::shared_ptr<IKRRenderViewExport>&)>&)`），供 `KRRenderView` 遍历视图树 |
| `src/main/cpp/CMakeLists.txt` | 新增链接 `libohcommonevent.so` |

**core**：无改动（`ScrollerView.scrollToTop{}` 与 `ScrollerEventConst.SCROLL_TO_TOP` 已存在）。

**demo**：新增 OHOS 回顶验证页面（`Scroller` 与 `List` 各一，覆盖"注册回调 / 未注册回调"两种用例）。

## Risks / Trade-offs

- [关闭系统属性后，未被遍历覆盖的滚动容器失去回顶] → 分发覆盖 C++ 视图树内全部滚动容器；ArkTS 侧容器在文档与 spec 中显式声明不在范围内；后续如需支持，可在 ArkTS 层补独立实现。
- [自实现回顶与系统默认动画/物理特性存在体验差异] → 选择直接滚动至 offset 0（与 iOS 默认回顶一致）；如需过渡动画，后续以独立 change 引入可配置项，不污染本次行为契约。
- [公共事件订阅在部分系统/真机可能受限] → 仅记录错误日志；订阅失败时点击状态栏不回顶（D8 修订后接受的代价），不做 fail-open 门控。
- [公共事件参数为空，无法区分是哪个容器/窗口被点击] → 按"实例内全部滚动容器"统一处理；多 Pager/多窗口同时活跃时所有活跃实例都会回顶，与 iOS 单窗口语义不同，记录为已知行为（见 Open Questions）。
- [遍历视图树的开销] → 仅在事件到达时遍历一次；后续若容器规模显著增长，可在渲染层维护滚动容器索引（不改变 spec 行为）。

## Migration Plan

- 无数据迁移、无 API 破坏性变更；业务代码不需要改动即可在 OHOS 上获得"可拦截的回顶"能力。
- 灰度/回滚：回滚即去掉属性设置与事件订阅（revert 本 change），系统默认行为自动恢复；无残留状态（订阅随实例销毁退订）。
- 发布前回归清单（真机 + 低版本系统）：Scroller 回顶、List 回顶、注册 `scrollToTop` 回调时回调被触发且不自动回顶、页面不可见时不响应、同页面多容器、API < 15 设备滚动行为不变、订阅失败场景不崩溃且回顶可用。

## Open Questions

- 多实例同时活跃（多 Pager / 分屏）时是否需要只响应前台窗口实例？当前按"全部活跃实例"处理，不改变 spec 与方案，后续可加过滤。
- `scrollToTop` 回调是否需要在回调中携带参数（如当前 offset）？当前与 iOS 保持一致（无参数），如业务有诉求可在后续 change 中扩展（属于 spec 变更）。
