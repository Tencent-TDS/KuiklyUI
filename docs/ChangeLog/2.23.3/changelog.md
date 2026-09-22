# 2.23.3 变更说明

## Part 1 总体说明

2.23.3 让 Pager 翻页与长列表滚动的收尾动作更稳，首屏与图文渲染更加准确，内置组件的禁用态、配色与边界保护在各端表现一致，H5/小程序的输入、超时与右键交互也可以按业务需要控制。具体如下：

- **翻页与长列表的滚动收尾**（iOS、Android、Compose）：修复吸附结束时因弹性动画未收尾导致的硬跳、拖拽结束状态不同步、吸附边缘未对齐与触摸延迟的问题；稳定内容尺寸以消除抽屉边缘留白，并修正视口尺寸单位换算带来的底部回弹越界；提供可显式开启的 LazyList 预取能力，并修复由此引发的鸿蒙 2.0 产物构建失败。
- **首屏与图文渲染**（iOS、Android、鸿蒙）：修掉 TurboDisplay 因同步执行根视图尺寸变更而导致的首屏渲染问题，修正 API 29 图文混排换行时的垂直偏移，鸿蒙侧改用系统点九拉伸能力实现 capInsets，九宫格图片在拉伸区域的显示不再失真。
- **内置组件的跨端一致性**（多端）：ActionSheet 的描述文字在深色与浅色模式下不再共用同一配色，禁用态 CheckBox 不再因点击而切换选中状态，Tabs 在负向滚动偏移下也不再越界抛异常。
- **H5/小程序与 macOS 的可控性**：小程序侧请求超时设置从无效变为真正生效；H5 侧新增右键/长按菜单的全局开关供宿主按需放行，并修复输入框长度限制的处理；macOS 侧的 SwiftPM 集成补齐平台声明后可用。

历史提交可见 [2.23.2...2.23.3](https://github.com/Tencent-TDS/KuiklyUI/compare/2.23.2...2.23.3)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: correct API29 ImageSpan vertical shift on line wrap · [#1420](https://github.com/Tencent-TDS/KuiklyUI/pull/1420)

### iOS

- fix(iOS): TurboDisplay first-screen rendering issue caused by synchronous execution of rootViewSizeChanged · [#1541](https://github.com/Tencent-TDS/KuiklyUI/pull/1541)

### 鸿蒙

- feat(ohos): capInsets 9-patch with OH_Drawing_Lattice · [#1553](https://github.com/Tencent-TDS/KuiklyUI/pull/1553)

### Compose

- fix(ios): avoid pager snap end hard-jump from spring flush miss · [#1538](https://github.com/Tencent-TDS/KuiklyUI/pull/1538)
- fix(android): pager will drag end not sync on android · [#1546](https://github.com/Tencent-TDS/KuiklyUI/pull/1546)
- fix(compose): round viewportSize dp→px conversion to fix bottom overscroll bounce · [#1530](https://github.com/Tencent-TDS/KuiklyUI/pull/1530)
- feat(compose): enable opt-in LazyList prefetch · [#1422](https://github.com/Tencent-TDS/KuiklyUI/pull/1422)
- fix(compose): sync pager snap edge align and touch-defer · [#1563](https://github.com/Tencent-TDS/KuiklyUI/pull/1563)
- fix(Compose): resolve ohos 2.0 product build failure caused by LazyList prefetch · [#1562](https://github.com/Tencent-TDS/KuiklyUI/pull/1562)
- fix(pager): stabilize iOS contentSize to avoid drawer edge gap · [#1567](https://github.com/Tencent-TDS/KuiklyUI/pull/1567)
- feat(iOS): drive Compose on a dedicated context thread · [#1566](https://github.com/Tencent-TDS/KuiklyUI/pull/1566)

### H5

- fix(miniApp): fix req timeout setting invalid bug · [#1543](https://github.com/Tencent-TDS/KuiklyUI/pull/1543)
- Feature/h5 contextmenu switch · [#1558](https://github.com/Tencent-TDS/KuiklyUI/pull/1558)
- Bugfix/h5 maxlength and mouse down · [#1576](https://github.com/Tencent-TDS/KuiklyUI/pull/1576)

### macOS

- fix(swiftpm): add macOS platform support and fix KRUIKit.h not found · [#1552](https://github.com/Tencent-TDS/KuiklyUI/pull/1552)

### 跨端

- fix(core/views): ActionSheet description text uses same color in night mode and light mode · [#1504](https://github.com/Tencent-TDS/KuiklyUI/pull/1504)
- fix(core/views): disabled CheckBox still toggles checked state on click · [#1503](https://github.com/Tencent-TDS/KuiklyUI/pull/1503)
- fix: clamp negative scroll offset index to prevent IndexOutOfBoundsException in TabsView · [#1575](https://github.com/Tencent-TDS/KuiklyUI/pull/1575)

### 多端

- fix(demo): 横竖屏黑底仅作用于 orientation demos · [#1542](https://github.com/Tencent-TDS/KuiklyUI/pull/1542)

### 其他

- chore: update drag demo gesture · [#1544](https://github.com/Tencent-TDS/KuiklyUI/pull/1544)
- docs: add 393dp unified design width best practice guide · [#1467](https://github.com/Tencent-TDS/KuiklyUI/pull/1467)
- docs: add missing limitHeaderBounces param to bouncesEnable API · [#1545](https://github.com/Tencent-TDS/KuiklyUI/pull/1545)
- fix: publish error caused by VersionTest file · [#1568](https://github.com/Tencent-TDS/KuiklyUI/pull/1568)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。
