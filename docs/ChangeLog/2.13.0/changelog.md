# 2.13.0 变更说明

## Part 1 总体说明

2.13.0 提升了绘制与变换的还原度，改善了多端滚动与翻页的手感，并补齐了鸿蒙端的性能数据采集与更多设备形态，具体如下：

- **绘制与变换的还原度**（Android、iOS、鸿蒙）：字重为常规时的字形绘制、边框与圆角、负缩放、斜切变换、富文本渐变色以及多张图片并发加载等显示异常被逐一修正。
- **滚动与翻页的手感**（Android、H5、Compose、跨端）：嵌套滚动不再抖动，翻页不再因微小速度而反向回弹，H5 上列表嵌套翻页容器的滚轮冲突解除，无限滚动的传送机制与列表子项的层叠顺序也更稳定。
- **性能数据与设备形态覆盖**（鸿蒙）：可以取到主线程帧率与内存占用数据，平板与二合一设备类型纳入支持。
- **更多端与 Compose 的接入完善**（小程序、macOS、Compose）：提供小程序与 Kuikly 混编的示例，修复 macOS 上的多个问题，Compose 的输入组件支持权重分配，并发场景下的互斥锁崩溃被消除。

历史提交可见 [2.12.1...2.13.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.12.1...2.13.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix(android): optimize font weight normal drawing · [#854](https://github.com/Tencent-TDS/KuiklyUI/pull/854)
- fix: avoid nestScroll shake when sub list pos change · [#860](https://github.com/Tencent-TDS/KuiklyUI/pull/860)
- chore: change dependency  to implementation for dynamicanimation library · [#879](https://github.com/Tencent-TDS/KuiklyUI/pull/879)
- fix: pager ignore small velocity sometime reverse · [#890](https://github.com/Tencent-TDS/KuiklyUI/pull/890)
- chore: downgrade core-ktx for compatibility for android renderer on k… · [#891](https://github.com/Tencent-TDS/KuiklyUI/pull/891)

### iOS

- refactor: optimize parameter format for specific component on iOS · [#863](https://github.com/Tencent-TDS/KuiklyUI/pull/863)
- fix: negative scaling factor issue · [#867](https://github.com/Tencent-TDS/KuiklyUI/pull/867)
- fix: ios abnormal image loading between multiple KRImageViews · [#812](https://github.com/Tencent-TDS/KuiklyUI/pull/812)
- fix: ios Border and BorderRadius abnormal effect · [#848](https://github.com/Tencent-TDS/KuiklyUI/pull/848)

### 鸿蒙

- feat: ohos performance fps(mainFps) and memory(pss) api · [#806](https://github.com/Tencent-TDS/KuiklyUI/pull/806)
- fix: skew transformation issue · [#870](https://github.com/Tencent-TDS/KuiklyUI/pull/870)
- chore: ohosApp add tablet and 2in1 deviceTypes · [#873](https://github.com/Tencent-TDS/KuiklyUI/pull/873)
- fix: version info in pageData · [#886](https://github.com/Tencent-TDS/KuiklyUI/pull/886)

### Compose

- fix: avoid mutex locked crash · [#846](https://github.com/Tencent-TDS/KuiklyUI/pull/846)
- feat: add scroll max velcity sepped limit for compose dsl · [#847](https://github.com/Tencent-TDS/KuiklyUI/pull/847)
- feat: optimize infinite scroll teleport mechanism · [#859](https://github.com/Tencent-TDS/KuiklyUI/pull/859)
- fix: compose TextField support weight · [#827](https://github.com/Tencent-TDS/KuiklyUI/pull/827)

### H5

- feat: fix web wheel conflict for ListView nested in PageList · [#880](https://github.com/Tencent-TDS/KuiklyUI/pull/880)

### 小程序

- feat: add miniapp work with kuikly example · [#853](https://github.com/Tencent-TDS/KuiklyUI/pull/853)

### 跨端

- fix: unstable overlapping order of list items · [#877](https://github.com/Tencent-TDS/KuiklyUI/pull/877)
- fix: reduce using blur on android · [#882](https://github.com/Tencent-TDS/KuiklyUI/pull/882)

### 其他

- add contribute wall · [#849](https://github.com/Tencent-TDS/KuiklyUI/pull/849)
- fix: resolve multiple issues on macOS · [#850](https://github.com/Tencent-TDS/KuiklyUI/pull/850)
- docs: update changelog to 2.12.0 · [#840](https://github.com/Tencent-TDS/KuiklyUI/pull/840)
- docs: add diffUpdate documentation to observableList · [#772](https://github.com/Tencent-TDS/KuiklyUI/pull/772)
- fix: ios Richtext linegradientColor abnormal effect · [#838](https://github.com/Tencent-TDS/KuiklyUI/pull/838)
- add community info · [#862](https://github.com/Tencent-TDS/KuiklyUI/pull/862)
- docs: update community sharing title · [#871](https://github.com/Tencent-TDS/KuiklyUI/pull/871)
- chore: add dragItemListDemoPage sample · [#789](https://github.com/Tencent-TDS/KuiklyUI/pull/789)
- feat: update compose docs · [#875](https://github.com/Tencent-TDS/KuiklyUI/pull/875)
- docs: modify dragItemListDemo url · [#876](https://github.com/Tencent-TDS/KuiklyUI/pull/876)
- docs: update BlurView docs · [#881](https://github.com/Tencent-TDS/KuiklyUI/pull/881)
- chore: add skew demo page · [#883](https://github.com/Tencent-TDS/KuiklyUI/pull/883)
- docs: android proguard rule · [#885](https://github.com/Tencent-TDS/KuiklyUI/pull/885)
- fix: docs solve vue template error · [#887](https://github.com/Tencent-TDS/KuiklyUI/pull/887)

---

## Part 3 破坏性变更

## 条件性行为变化

### Android 渲染层不再向外透出 dynamicanimation 依赖

- 结果：Android 渲染层的 `androidx.dynamicanimation` 依赖由 api 改为 implementation，不再随渲染层传递给宿主工程。
- 影响：宿主工程若在自身代码中直接引用该库的类且未自行声明依赖，升级后会出现编译期找不到符号；仅通过 Kuikly 能力使用弹性动画的业务不受影响。
- 迁移：在宿主工程的 Gradle 依赖中自行声明该库（版本可按宿主需要选择）。
- 说明文档：[Android 快速开始](../../QuickStart/android.md)
