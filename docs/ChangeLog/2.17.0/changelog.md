# 2.17.0 变更说明

## Part 1 总体说明

2.17.0 让 Compose 侧第一次具备完整的路由与调试手段，给出了 Kuikly 页面嵌入原生页面的落地方案，并把列表滚动中几处长期存在的抖动与错乱收口。具体如下：

- **Compose 导航与重组分析**：新增 Compose Navigation，`NavBackStackEntry` 补齐生命周期并挂上 ViewModelStore，页面状态在返回栈中得以保留；新增 Drawer 组件；新增 RecompositionProfiler 重组分析工具并支持业务自定义过滤规则，嵌套列表中的 ScrollerView 通过 `ReusableComposeNode` 复用。
- **原生页面嵌入与视图快照**：提供以 View 粒度把 Kuikly 页面嵌入 Native 页面的示例方案；iOS、Android 支持视图快照；多端组件支持复用，发送按钮可自定义键盘收起行为。
- **滚动与多指触控收口**：修复嵌套滚动的速度追踪与多指触控坐标、`scrollEnabled` 关闭后仍透传触摸、Pager 滚动结束误判；iOS 修掉 POST 请求体 JSON 键序被打乱导致的签名校验失败与中文路径图片加载不出来，macOS 修掉鼠标手势 ID 与状态机异常以及文本视图失焦时的无限递归，鸿蒙修掉 PAGView 自动播放与进度不同步。

历史提交可见 [2.16.0...2.17.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.16.0...2.17.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix(list): use VelocityTracker in nested scroll · [#1199](https://github.com/Tencent-TDS/KuiklyUI/pull/1199)
- fix(list): correct multi-touch pointer coordinates in scroll handlers · [#1236](https://github.com/Tencent-TDS/KuiklyUI/pull/1236)
- fix: bypass touch when scrollEnabled is false · [#1240](https://github.com/Tencent-TDS/KuiklyUI/pull/1240)
- fix(android): allow parent ViewPager scroll when inner RecyclerView d… · [#1254](https://github.com/Tencent-TDS/KuiklyUI/pull/1254)

### iOS

- fix: update the acquisition timing of iOS ActivityHeight  and add viewWithTag docs description · [#1142](https://github.com/Tencent-TDS/KuiklyUI/pull/1142)
- fix(ios): preserve JSON key order in HTTP POST body to fix signature mismatch · [#1203](https://github.com/Tencent-TDS/KuiklyUI/pull/1203)
- fix: iOS support loading images with Chinese characters in the file path · [#1259](https://github.com/Tencent-TDS/KuiklyUI/pull/1259)
- feat: iOS add a dedicated demo page for TurboDisplay mechanism · [#1261](https://github.com/Tencent-TDS/KuiklyUI/pull/1261)

### 鸿蒙

- Reuseable kuikly component · [#1159](https://github.com/Tencent-TDS/KuiklyUI/pull/1159)
- fix: update ohos KROhPreferences instantiation to singleton pattern · [#1224](https://github.com/Tencent-TDS/KuiklyUI/pull/1224)
- fix: OHOS PAGView autoPlay not working and setProgress not synced · [#1177](https://github.com/Tencent-TDS/KuiklyUI/pull/1177)
- feat(ohos): head indent support · [#1182](https://github.com/Tencent-TDS/KuiklyUI/pull/1182)

### Compose

- feat(navigation): add Compose Navigation support · [#1147](https://github.com/Tencent-TDS/KuiklyUI/pull/1147)
- feat: support ScrollerView reuse in nested LazyList/LazyGrid/Pager via ReusableComposeNode · [#1125](https://github.com/Tencent-TDS/KuiklyUI/pull/1125)
- fix(compose): fix ScrollableTabRow nestedScroll&bounceEnable · [#1186](https://github.com/Tencent-TDS/KuiklyUI/pull/1186)
- feat: compose add Drawer component · [#1179](https://github.com/Tencent-TDS/KuiklyUI/pull/1179)
- fix: use drainSafely() to avoid CancellationException on cancel · [#1191](https://github.com/Tencent-TDS/KuiklyUI/pull/1191)
- feat(navigation):  NavBackStackEntry lifecycle with ViewModelStore · [#1184](https://github.com/Tencent-TDS/KuiklyUI/pull/1184)
- fix: lazy grid scroll behavior and remove JS-incompatible API · [#1198](https://github.com/Tencent-TDS/KuiklyUI/pull/1198)
- feat: support custom keyboard dismissal control for the send button action · [#1045](https://github.com/Tencent-TDS/KuiklyUI/pull/1045)
- feat: h5 support text onLineBreakMargin event · [#1196](https://github.com/Tencent-TDS/KuiklyUI/pull/1196)
- fix: skip scrollend expansion for pager wrappers · [#1229](https://github.com/Tencent-TDS/KuiklyUI/pull/1229)
- fix(compose): fix pager guard allowing bounce-direction fling to trigger page change · [#1238](https://github.com/Tencent-TDS/KuiklyUI/pull/1238)
- feat(compose): add RecompositionProfiler for Compose performance analysis · [#1230](https://github.com/Tencent-TDS/KuiklyUI/pull/1230)
- feat(compose): add business custom filter API for RecompositionProfiler · [#1252](https://github.com/Tencent-TDS/KuiklyUI/pull/1252)
- feat(profiler): fix scope loss when framework composables are filtered · [#1264](https://github.com/Tencent-TDS/KuiklyUI/pull/1264)

### H5

- fix: h5 text with custom font display not full · [#1153](https://github.com/Tencent-TDS/KuiklyUI/pull/1153)
- feat: add default KRCustomPropsHandler in h5 web render · [#1175](https://github.com/Tencent-TDS/KuiklyUI/pull/1175)

### macOS

- fix: remove infinite recursion in KRUITextView resignFirstResponder on macOS · [#1225](https://github.com/Tencent-TDS/KuiklyUI/pull/1225)
- fix: macOS gesture pointerId and state machine for mouse events · [#1251](https://github.com/Tencent-TDS/KuiklyUI/pull/1251)

### 多端

- feat: ios and android support view snapshot · [#1066](https://github.com/Tencent-TDS/KuiklyUI/pull/1066)
- feat: add sample solution for embedding Kuikly pages into native pages at the view granularity · [#1228](https://github.com/Tencent-TDS/KuiklyUI/pull/1228)

### 其他

- docs(ios): update configuration screenshot · [#1154](https://github.com/Tencent-TDS/KuiklyUI/pull/1154)
- docs(ios): update configuration screenshot（missing image) · [#1156](https://github.com/Tencent-TDS/KuiklyUI/pull/1156)
- docs: resolve compile error by failing to access image · [#1161](https://github.com/Tencent-TDS/KuiklyUI/pull/1161)
- docs: update pagelist pageIndexDidChange notes · [#1162](https://github.com/Tencent-TDS/KuiklyUI/pull/1162)
- docs: add compile and version notes · [#1071](https://github.com/Tencent-TDS/KuiklyUI/pull/1071)
- fix: remove playTimeDidChanged from ComposeVideoDemo to avoid confusion · [#1169](https://github.com/Tencent-TDS/KuiklyUI/pull/1169)
- docs: update changelog · [#1168](https://github.com/Tencent-TDS/KuiklyUI/pull/1168)
- docs: update refresh enable notes · [#1165](https://github.com/Tencent-TDS/KuiklyUI/pull/1165)
- docs: update compile notes · [#1172](https://github.com/Tencent-TDS/KuiklyUI/pull/1172)
- docs(android): disabling system scaling via getDisplayMetrics · [#1171](https://github.com/Tencent-TDS/KuiklyUI/pull/1171)
- docs(ohos): update docs · [#1174](https://github.com/Tencent-TDS/KuiklyUI/pull/1174)
- docs: add AI harness infrastructure for KuiklyUI team · [#1202](https://github.com/Tencent-TDS/KuiklyUI/pull/1202)
- docs: fix broken links, add badges and macOS app instructions to README · [#1204](https://github.com/Tencent-TDS/KuiklyUI/pull/1204)
- Revert "docs: add AI harness infrastructure for KuiklyUI team" · [#1207](https://github.com/Tencent-TDS/KuiklyUI/pull/1207)
- chore: bump kuikly-harness to v2026.04.03.8 · [#1208](https://github.com/Tencent-TDS/KuiklyUI/pull/1208)
- docs: add guide for getting component size and position · [#1205](https://github.com/Tencent-TDS/KuiklyUI/pull/1205)
- chore: bump kuikly-harness to v2026.04.07 · [#1231](https://github.com/Tencent-TDS/KuiklyUI/pull/1231)
- chore: update apk qrcode · [#1239](https://github.com/Tencent-TDS/KuiklyUI/pull/1239)
- docs: add PAGAdapter description and update pag.md · [#1235](https://github.com/Tencent-TDS/KuiklyUI/pull/1235)
- chore: bump kuikly-harness to v2026.04.13 · [#1246](https://github.com/Tencent-TDS/KuiklyUI/pull/1246)
- fix: resolve docs compile error · [#1250](https://github.com/Tencent-TDS/KuiklyUI/pull/1250)
- chore: bump kuikly-harness to v2026.04.15 · [#1255](https://github.com/Tencent-TDS/KuiklyUI/pull/1255)
- docs: add Kuikly 2026 open source roadmap · [#1263](https://github.com/Tencent-TDS/KuiklyUI/pull/1263)
- docs: update Kuikly roadmap titles · [#1271](https://github.com/Tencent-TDS/KuiklyUI/pull/1271)
- chore: update maven plugin source · [#1279](https://github.com/Tencent-TDS/KuiklyUI/pull/1279)
- docs: update API/Components/Pager.md pageDidAppear and pageDidDisappear description · [#857](https://github.com/Tencent-TDS/KuiklyUI/pull/857)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。
