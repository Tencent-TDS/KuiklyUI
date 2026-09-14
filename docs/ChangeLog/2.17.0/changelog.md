# 2.17.0 变更汇总

> 本版导航：[版本说明](./announcement.md) · **变更汇总** · [破坏性与迁移](./breaking.md)

## 多端

- feat(navigation): add Compose Navigation support · [#1147](https://github.com/Tencent-TDS/KuiklyUI/pull/1147)
- feat(navigation): NavBackStackEntry lifecycle with ViewModelStore · [#1184](https://github.com/Tencent-TDS/KuiklyUI/pull/1184)
- feat: compose add Drawer component · [#1179](https://github.com/Tencent-TDS/KuiklyUI/pull/1179)
- feat: support ScrollerView reuse in nested LazyList/LazyGrid/Pager via ReusableComposeNode · [#1125](https://github.com/Tencent-TDS/KuiklyUI/pull/1125)
- feat(compose): add RecompositionProfiler for Compose performance analysis · [#1230](https://github.com/Tencent-TDS/KuiklyUI/pull/1230)
- feat(compose): add business custom filter API for RecompositionProfiler · [#1252](https://github.com/Tencent-TDS/KuiklyUI/pull/1252)
- feat(profiler): fix scope loss when framework composables are filtered · [#1264](https://github.com/Tencent-TDS/KuiklyUI/pull/1264)
- feat: support custom keyboard dismissal control for the send button action · [#1045](https://github.com/Tencent-TDS/KuiklyUI/pull/1045)（Android / iOS / 鸿蒙）
- feat: ios and android support view snapshot · [#1066](https://github.com/Tencent-TDS/KuiklyUI/pull/1066)（iOS / Android）
- fix: lazy grid scroll behavior and remove JS-incompatible API · [#1198](https://github.com/Tencent-TDS/KuiklyUI/pull/1198)
- fix(compose): fix pager guard allowing bounce-direction fling to trigger page change · [#1238](https://github.com/Tencent-TDS/KuiklyUI/pull/1238)
- fix(compose): fix ScrollableTabRow nestedScroll & bounceEnable not working · [#1186](https://github.com/Tencent-TDS/KuiklyUI/pull/1186)
- fix: skip scrollend expansion for pager wrappers · [#1229](https://github.com/Tencent-TDS/KuiklyUI/pull/1229)
- fix: use drainSafely() to avoid CancellationException on cancel · [#1191](https://github.com/Tencent-TDS/KuiklyUI/pull/1191)

## 单端

### Android

- fix(android): allow parent ViewPager scroll when inner RecyclerView disabled · [#1254](https://github.com/Tencent-TDS/KuiklyUI/pull/1254)
- fix: bypass touch when scrollEnabled is false · [#1240](https://github.com/Tencent-TDS/KuiklyUI/pull/1240)
- fix(list): correct multi-touch pointer coordinates in scroll handlers · [#1236](https://github.com/Tencent-TDS/KuiklyUI/pull/1236)
- fix(list): use VelocityTracker in nested scroll · [#1199](https://github.com/Tencent-TDS/KuiklyUI/pull/1199)

### iOS

- fix: iOS support loading images with Chinese characters in the file path · [#1259](https://github.com/Tencent-TDS/KuiklyUI/pull/1259)
- fix: macOS gesture pointerId and state machine for mouse events · [#1251](https://github.com/Tencent-TDS/KuiklyUI/pull/1251)
- fix: resolve infinite recursion in KRUITextView resignFirstResponder on macOS · [#1225](https://github.com/Tencent-TDS/KuiklyUI/pull/1225)
- fix(ios): preserve JSON key order in HTTP POST body to fix signature mismatch · [#1203](https://github.com/Tencent-TDS/KuiklyUI/pull/1203)
- fix: update the acquisition timing of iOS ActivityHeight and add viewWithTag docs description · [#1142](https://github.com/Tencent-TDS/KuiklyUI/pull/1142)

### 鸿蒙

- feat(ohos): head indent support · [#1182](https://github.com/Tencent-TDS/KuiklyUI/pull/1182)
- Reuseable kuikly component · [#1159](https://github.com/Tencent-TDS/KuiklyUI/pull/1159)
- fix: OHOS PAGView autoPlay not working and setProgress not synced · [#1177](https://github.com/Tencent-TDS/KuiklyUI/pull/1177)
- fix: update ohos KROhPreferences instantiation to singleton pattern · [#1224](https://github.com/Tencent-TDS/KuiklyUI/pull/1224)

### H5

- add default KRCustomPropsHandler in h5 web render · [#1175](https://github.com/Tencent-TDS/KuiklyUI/pull/1175)
- feat: h5 support text onLineBreakMargin event · [#1196](https://github.com/Tencent-TDS/KuiklyUI/pull/1196)
- fix: h5 text with custom font display not full · [#1153](https://github.com/Tencent-TDS/KuiklyUI/pull/1153)

## 其他

### 文档

- docs: add app foreground/background trigger notes for pageDidAppear and pageDidDisappear · [#857](https://github.com/Tencent-TDS/KuiklyUI/pull/857)
- docs: update Kuikly roadmap titles · [#1271](https://github.com/Tencent-TDS/KuiklyUI/pull/1271)
- docs: add Kuikly 2026 open source roadmap · [#1263](https://github.com/Tencent-TDS/KuiklyUI/pull/1263)
- docs: resolve docs compile error · [#1250](https://github.com/Tencent-TDS/KuiklyUI/pull/1250)
- docs: add PAGAdapter description and update pag.md · [#1235](https://github.com/Tencent-TDS/KuiklyUI/pull/1235)
- docs: add guide for getting component size and position · [#1205](https://github.com/Tencent-TDS/KuiklyUI/pull/1205)
- docs: fix broken links, add badges and macOS app instructions to README · [#1204](https://github.com/Tencent-TDS/KuiklyUI/pull/1204)
- Revert "docs: add AI harness infrastructure for KuiklyUI team" · [#1207](https://github.com/Tencent-TDS/KuiklyUI/pull/1207)
- docs(ohos): update docs · [#1174](https://github.com/Tencent-TDS/KuiklyUI/pull/1174)
- docs(android): disabling system scaling via getDisplayMetrics · [#1171](https://github.com/Tencent-TDS/KuiklyUI/pull/1171)
- docs: update compile notes · [#1172](https://github.com/Tencent-TDS/KuiklyUI/pull/1172)
- docs: update refresh enable notes · [#1165](https://github.com/Tencent-TDS/KuiklyUI/pull/1165)
- docs: update changelog · [#1168](https://github.com/Tencent-TDS/KuiklyUI/pull/1168)
- docs: add compile and version notes · [#1071](https://github.com/Tencent-TDS/KuiklyUI/pull/1071)
- docs: update pagelist pageIndexDidChange notes · [#1162](https://github.com/Tencent-TDS/KuiklyUI/pull/1162)
- docs: resolve compile error by failing to access image · [#1161](https://github.com/Tencent-TDS/KuiklyUI/pull/1161)
- docs(ios): update configuration screenshot · [#1154](https://github.com/Tencent-TDS/KuiklyUI/pull/1154)
- docs(ios): update configuration screenshot（missing image) · [#1156](https://github.com/Tencent-TDS/KuiklyUI/pull/1156)

### 工程与示例

- feat: add sample solution for embedding Kuikly pages into native pages at the view granularity · [#1228](https://github.com/Tencent-TDS/KuiklyUI/pull/1228)
- feat: iOS add a dedicated demo page for TurboDisplay mechanism · [#1261](https://github.com/Tencent-TDS/KuiklyUI/pull/1261)
- fix: remove playTimeDidChanged from ComposeVideoDemo to avoid confusion · [#1169](https://github.com/Tencent-TDS/KuiklyUI/pull/1169)
- chore: update maven plugin source · [#1279](https://github.com/Tencent-TDS/KuiklyUI/pull/1279)
- chore: bump kuikly-harness to v2026.04.15 · [#1255](https://github.com/Tencent-TDS/KuiklyUI/pull/1255)
- chore: bump kuikly-harness to v2026.04.13 · [#1246](https://github.com/Tencent-TDS/KuiklyUI/pull/1246)
- chore: bump kuikly-harness to v2026.04.07 · [#1231](https://github.com/Tencent-TDS/KuiklyUI/pull/1231)
- chore: bump kuikly-harness to v2026.04.03.8 · [#1208](https://github.com/Tencent-TDS/KuiklyUI/pull/1208)
- chore: update apk qrcode · [#1239](https://github.com/Tencent-TDS/KuiklyUI/pull/1239)
