# 2.18.0 变更说明

## Part 1 总体说明

2.18.0 补齐了桌面端的指针与键盘交互，收敛了滚动、弹层、键盘这些高频场景的稳定性问题，同时让小程序可以直接使用微信原生能力、让重组性能问题有线索可查。具体如下：

- **桌面端指针与键盘交互**（macOS）：悬停可以驱动组件状态变化并切换光标形态，输入框聚焦期间鼠标按下仍正常派发，回车键能正常触发提交。
- **滚动与弹层的稳定性**（Android、iOS）：嵌套滚动锁在回弹场景生效，Pager 的滚动状态被收敛，图层属性正确落到承载视图；Android 键盘监听的并发崩溃也一并修复，并补上基于 Dialog 的全屏弹层实现。
- **小程序原生能力接入**（小程序）：可以直接用 DSL 使用微信原生组件与对应能力；折叠屏展开与收起时 SliderPage 子项跟随页面项尺寸重新布局，不再错位。
- **重组分析的可观测性**（多端）：重组分析器会连同触摸与滚动上下文一起记录，定位多余重组有了直接线索。

历史提交可见 [2.17.0...2.18.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.17.0...2.18.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: android keyboard listener concurrent crash · [#1282](https://github.com/Tencent-TDS/KuiklyUI/pull/1282)

### iOS

- fix: iOS nested scroll lock work for spring branch · [#1328](https://github.com/Tencent-TDS/KuiklyUI/pull/1328)

### Compose

- feat(profiler): add touch/scroll context events for recomposition analyzer · [#1286](https://github.com/Tencent-TDS/KuiklyUI/pull/1286)
- fix: stabilize iOS pager and nested scroll · [#1315](https://github.com/Tencent-TDS/KuiklyUI/pull/1315)
- feat(android): implement Dialog-based ModalView with full-screen · [#1284](https://github.com/Tencent-TDS/KuiklyUI/pull/1284)
- fix(compose): apply layer props to owner view · [#1338](https://github.com/Tencent-TDS/KuiklyUI/pull/1338)

### H5

- fix(h5): fix longPress state param · [#1330](https://github.com/Tencent-TDS/KuiklyUI/pull/1330)

### macOS

- feat: Macos support hover state with cursor change · [#1280](https://github.com/Tencent-TDS/KuiklyUI/pull/1280)
- fix: Macos enter key support commit effect · [#1314](https://github.com/Tencent-TDS/KuiklyUI/pull/1314)
- fix: Macos mouseDown invalid when TextField get focus · [#1313](https://github.com/Tencent-TDS/KuiklyUI/pull/1313)

### 跨端

- fix(ohos): make SliderPage child items respond to dynamic pageItem size (foldable screen fix) · [#1321](https://github.com/Tencent-TDS/KuiklyUI/pull/1321)

### 多端

- feat(miniApp): add wx component and api wrapper · [#1277](https://github.com/Tencent-TDS/KuiklyUI/pull/1277)

### 其他

- docs: update changelog for 2.17.0 version · [#1281](https://github.com/Tencent-TDS/KuiklyUI/pull/1281)
- chore: bump kuikly-harness to v2026.04.23.2 · [#1298](https://github.com/Tencent-TDS/KuiklyUI/pull/1298)
- Bugfix/h5 rich text2 · [#1322](https://github.com/Tencent-TDS/KuiklyUI/pull/1322)
- Bugfix/h5 pag copy bugs · [#1334](https://github.com/Tencent-TDS/KuiklyUI/pull/1334)
- chore: update codeowners · [#1332](https://github.com/Tencent-TDS/KuiklyUI/pull/1332)
- Bugfix/h5 input and image · [#1336](https://github.com/Tencent-TDS/KuiklyUI/pull/1336)
- fix(ohos): resolve ohos har build errors caused by core-wx · [#1339](https://github.com/Tencent-TDS/KuiklyUI/pull/1339)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。
