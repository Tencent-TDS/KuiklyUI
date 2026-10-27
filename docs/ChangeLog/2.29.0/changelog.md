# 2.29.0 变更说明

## Part 1 总体说明

2.29.0 版本为 iPhone Duo 折叠屏提供了铰链与系统避让区域适，：页面内可获取铰链状态，以及折缝区、遮挡区的位置、尺寸与生效状态；系统区域变化后，页面可以重新计算布局避让。适配了 Xcode 27 的 iOS / macOS 工程要求；扩展了 iOS TurboDisplay 的差异更新能力；修正了富文本、阴影和 H5 交互问题。具体如下：

- **iPhone Duo 折叠屏适配**（iOS）：提供了从系统信息接入到页面布局避让的完整链路，重点包括：
  - **适配机制**：通过 `hingeStatus` 获取合上、半开、全开状态，通过 `reservedRegions` 获取折缝区（`DIVISION`）和遮挡区（`OCCLUSION`）的坐标、尺寸与生效状态；这些数据会随 `rootViewSizeDidChanged` 在折叠、展开和旋转后更新。详细字段与用法见[页面数据](../../DevGuide/page-data.md)。
  - **组件使用方式**：`PagerData` 提供了安全区、铰链和避让区域数据供 `SafeArea`、导航栏以及普通 `View` / `Text` 等组件消费，把折缝与遮挡区域转化为页面布局边距。
  - **Demo 参考**：新增 `ReservedRegionDemo`，展示安全区、铰链状态和避让区域，区分生效与未生效的折缝，并用颜色标出系统区域；同时演示导航栏、顶部/左右/底部内容如何根据折缝和遮挡区域自动留出空间，为旋转、折叠/展开后的页面重排提供完整参考。
- **功能扩展与修复**：
  - 扩展 iOS TurboDisplay 的差异更新的手动控制：用户可通过 `TurboDisplayModule` 手动触发真实视图接管缓存首屏，避免默认时机过早执行；
  - 新增文本两端对齐，Span 局部字体设置能够正确保留，Surface 阴影改为正确的密度换算，Android 阴影不再被视图边界截断；H5 修复单行省略号、展开动画自动收起；Kotlin 1.9.22 下 JS 代码转换问题，鸿蒙线程运行时也得到加固。
- **Xcode 27 工程接入**（iOS、macOS）：Xcode 27 及以上版本要求 iOS 最低部署目标为 15.0、macOS 最低部署目标为 12.0。

历史提交可见 [2.28.0...2.29.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.28.0...2.29.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: preserve Android shadows beyond view bounds · [#1760](https://github.com/Tencent-TDS/KuiklyUI/pull/1760)

### iOS

- fix(iOS): keep text alignment when rebuilding attributedText in KRTextAreaView · [#1745](https://github.com/Tencent-TDS/KuiklyUI/pull/1745)
- fix(ios): implicit animation issue during border scaling · [#1749](https://github.com/Tencent-TDS/KuiklyUI/pull/1749)
- feat(ios): TurboDisplay support diff executation controlled by custom · [#1744](https://github.com/Tencent-TDS/KuiklyUI/pull/1744)
- feat(ios): support reserved regions and hinge status on iOS 27.1 · [#1765](https://github.com/Tencent-TDS/KuiklyUI/pull/1765)

### 鸿蒙

- fix(ohos): raise KRThread worker stack to 8MB · [#1778](https://github.com/Tencent-TDS/KuiklyUI/pull/1778)
- chore(ohos): delete performance invalid code · [#1788](https://github.com/Tencent-TDS/KuiklyUI/pull/1788)

### Compose

- fix(compose): serialize SpanStyle.fontFamily on rich text spans · [#1775](https://github.com/Tencent-TDS/KuiklyUI/pull/1775)
- feat(text): add justify align mode · [#1763](https://github.com/Tencent-TDS/KuiklyUI/pull/1763)
- fix: Surface shadowElevation double dp-to-px conversion · [#1785](https://github.com/Tencent-TDS/KuiklyUI/pull/1785)

### H5

- fix(web): 修复单行 textOverFlowTail 不显示省略号 · [#1747](https://github.com/Tencent-TDS/KuiklyUI/pull/1747)
- fix(h5): fix expand anim auto collapse bug · [#1748](https://github.com/Tencent-TDS/KuiklyUI/pull/1748)
- fix(h5): fix h5 jsValueToKotlin outlinedJsCode error for kotlin 1.9.22 · [#1754](https://github.com/Tencent-TDS/KuiklyUI/pull/1754)

### 多端

- fix: adapt iOS and macOS builds for Xcode 27 minimum deployment targets · [#1753](https://github.com/Tencent-TDS/KuiklyUI/pull/1753)

### 其他

- docs: update env-setup ide version notes · [#1751](https://github.com/Tencent-TDS/KuiklyUI/pull/1751)
- feat(demo): add chat bubble demo(左滑删除) · [#1758](https://github.com/Tencent-TDS/KuiklyUI/pull/1758)
- docs: fix page-data error · [#1777](https://github.com/Tencent-TDS/KuiklyUI/pull/1777)

---

## Part 3 破坏性变更

## 条件性破坏性

### Xcode 27 最低部署目标提升

- 结果：使用 Xcode 27 及以上版本时，iOS 最低部署目标提升为 15.0，macOS 最低部署目标提升为 12.0。
- 影响：宿主 App target 或 Pods 中仍使用更低 Deployment Target 的 iOS / macOS 工程，升级后无法按新 SDK 构建。
- 迁移：将宿主 App target 与 Pods 中各 target 的 iOS Deployment Target 调整为 15.0 或更高，macOS Deployment Target 调整为 12.0 或更高。
- 说明文档：[iOS 接入](../../QuickStart/iOS.md)

### Compose `Surface.shadowElevation` 行为修正

- 结果：`Surface.shadowElevation` 从此前的双重 dp-to-px 换算修正为正确的密度换算，阴影大小与 `Box(Modifier.shadow(...))` 对齐。
- 影响：在 density 大于 1 的设备上，使用 `Surface` 及其内部组件阴影的页面，升级后阴影会比 2.28.0 及之前版本更小，这是修复过度放大的结果。
- 迁移：对使用 `Surface(shadowElevation = ...)` 的页面，以及 `Card`、`Button`、`Snackbar` 等相关组件进行视觉回归；无需修改调用方式。
- 说明文档：[Compose 基础组件](../../Compose/core-components.md)
