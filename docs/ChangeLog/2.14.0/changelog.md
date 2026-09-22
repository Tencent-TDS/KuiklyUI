# 2.14.0 变更说明

## Part 1 总体说明

2.14.0 让滚动与手势在多端上的行为更加一致，文字、阴影、遮罩等视觉细节更贴近设计稿，业务也能自主决定异常兜底策略并在排障时拿到更完整的运行日志。具体如下：

- **滚动与手势行为的收敛**：Android 拖拽结束后回弹状态不再残留，setContentOffset 在各端支持线性动画曲线并对非法曲线参数做校验，iOS 键盘弹起时的动画曲线可与原生保持一致；Compose 侧的滚动定位、列表项布局以及父子组件触摸冲突与惯性停止也一并修正。
- **文字与图形呈现的补齐**：新增 extra bold 与 black 两级字重，支持非填充样式的 boxShadow，鸿蒙画布开启抗锯齿，skew 变换跟随锚点，并补上鸿蒙缺失的遮罩视图。
- **跨端通信与自定义视图联动**：iOS 与鸿蒙的 Module 调用返回值支持更多值类型与 Record 结构，鸿蒙配置变更可以下发到自定义视图，图片适配器补齐了额外的图片参数（iOS、鸿蒙）。
- **异常兜底与调试可见性**：业务可以主动关闭框架默认的异常捕获，Android 新增进程与内核调试日志，协程任务与延迟调用支持取消，并修复了多端若干崩溃与构建问题。

历史提交可见 [2.13.0...2.14.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.13.0...2.14.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- feat: Android kuikly process and core debug log · [#861](https://github.com/Tencent-TDS/KuiklyUI/pull/861)
- fix: Android overScroll not reset when dragging · [#899](https://github.com/Tencent-TDS/KuiklyUI/pull/899)
- feat: support font weight extra bold and black · [#905](https://github.com/Tencent-TDS/KuiklyUI/pull/905)
- fix: skew not follow anchor in android · [#916](https://github.com/Tencent-TDS/KuiklyUI/pull/916)
- docs: add fetchDrawable thread warning · [#918](https://github.com/Tencent-TDS/KuiklyUI/pull/918)
- fix: Android setContentOffest curve param check · [#925](https://github.com/Tencent-TDS/KuiklyUI/pull/925)
- feat: allow users to opt-out the default exception catching mechanism · [#944](https://github.com/Tencent-TDS/KuiklyUI/pull/944)

### iOS

- fix: ios module returnValue support value types · [#762](https://github.com/Tencent-TDS/KuiklyUI/pull/762)
- feat: support native keyboard animation curve sync on iOS · [#888](https://github.com/Tencent-TDS/KuiklyUI/pull/888)
- feat: support for linear animation curve in setContentOffset on iOS · [#926](https://github.com/Tencent-TDS/KuiklyUI/pull/926)
- fix: resolve compilation issue caused by importing YYImage · [#938](https://github.com/Tencent-TDS/KuiklyUI/pull/938)

### 鸿蒙

- fix: Fix Harmony file request issue · [#679](https://github.com/Tencent-TDS/KuiklyUI/pull/679)
- fix: main thread task crash · [#790](https://github.com/Tencent-TDS/KuiklyUI/pull/790)
- fix: enable canvas anti-aliasing on ohos · [#917](https://github.com/Tencent-TDS/KuiklyUI/pull/917)
- feat: setContentOffset support linear animation(Android\ohos) · [#924](https://github.com/Tencent-TDS/KuiklyUI/pull/924)
- feat: ohos callModuleMethod returnvalue type support Record · [#898](https://github.com/Tencent-TDS/KuiklyUI/pull/898)
- feat: pass on configuration update event to custom views · [#872](https://github.com/Tencent-TDS/KuiklyUI/pull/872)
- fix: context handler map crash · [#824](https://github.com/Tencent-TDS/KuiklyUI/pull/824)
- feat: ohos add KRMaskView · [#933](https://github.com/Tencent-TDS/KuiklyUI/pull/933)
- fix: ohos solve complie error · [#946](https://github.com/Tencent-TDS/KuiklyUI/pull/946)

### Compose

- fix: Refactor animateScrollToItem in LazyStaggeredGridState · [#897](https://github.com/Tencent-TDS/KuiklyUI/pull/897)
- fix(compose): Initialize WeakReference.js.kt instance with referent · [#889](https://github.com/Tencent-TDS/KuiklyUI/pull/889)
- feat: android opt touch conflict & add stop fling · [#901](https://github.com/Tencent-TDS/KuiklyUI/pull/901)
- fix: compose implement newDensity on ohos · [#921](https://github.com/Tencent-TDS/KuiklyUI/pull/921)
- fix: compose lazy column item placed incorrect · [#937](https://github.com/Tencent-TDS/KuiklyUI/pull/937)

### 跨端

- fix: solve ohos artifict build error · [#908](https://github.com/Tencent-TDS/KuiklyUI/pull/908)
- docs: update transform docs · [#923](https://github.com/Tencent-TDS/KuiklyUI/pull/923)
- feat(core-coroutines): introduce cancellable Job and cancellable delay; fix demo cancellation · [#707](https://github.com/Tencent-TDS/KuiklyUI/pull/707)

### 多端

- feat: support boxShadow not fill style(android,ohos) · [#909](https://github.com/Tencent-TDS/KuiklyUI/pull/909)
- fix: ohos and ios ImageAdapter add imageParams · [#893](https://github.com/Tencent-TDS/KuiklyUI/pull/893)

### 其他

- docs: update changelog version(2.13.0) · [#894](https://github.com/Tencent-TDS/KuiklyUI/pull/894)
- doc: change the description of the third step in the android-dev docu… · [#794](https://github.com/Tencent-TDS/KuiklyUI/pull/794)
- docs: add macOS alpha support documentation · [#903](https://github.com/Tencent-TDS/KuiklyUI/pull/903)
- feat: update perf docs · [#904](https://github.com/Tencent-TDS/KuiklyUI/pull/904)
- docs: add KuiklyBaseView docs · [#919](https://github.com/Tencent-TDS/KuiklyUI/pull/919)
- docs: add font weight compatibility warning · [#920](https://github.com/Tencent-TDS/KuiklyUI/pull/920)
- feat: artifact optimization guide · [#922](https://github.com/Tencent-TDS/KuiklyUI/pull/922)
- chore: update publish sh and add compatible yaml · [#934](https://github.com/Tencent-TDS/KuiklyUI/pull/934)
- docs: add toImage api docs · [#928](https://github.com/Tencent-TDS/KuiklyUI/pull/928)
- 解决windows快速编译问题 · [#940](https://github.com/Tencent-TDS/KuiklyUI/pull/940)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。
