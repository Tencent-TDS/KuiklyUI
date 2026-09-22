# 2.15.0 变更说明

## Part 1 总体说明

2.15.0 把 H5 端的图片、动画与交互补全到接近原生的水平，修掉了多端上影响文字排版与渲染正确性的问题，页面事件也新增了可追踪能力。具体如下：

- **输入框与富文本排版**：Android 重新设置输入内容时行高会按最新文本重算，富文本表现与 Android 保持一致，文本的最大长度限制支持区分计数类型。
- **H5 端的加载与交互**：图片只在加载成功后才显示，PAG 动画支持直接加载本地资源，拖拽不再误触发点击、文本可以选中，嵌套分页列表与向原生发送通知也都打通。
- **多端渲染与平台适配**：修掉宿主全局替换 layerClass 导致文字不可见、转屏后嵌套滚动出现底部空白、动画期间误报滚动事件等问题；鸿蒙侧修复阴影复用与页面默认样式并支持跟随系统字体；Compose 侧修复缩放为 0 的异常与未滚动时内容尺寸不更新。
- **页面事件可追踪**：新增页面事件埋点，页面加载与交互过程可以在业务侧被观测和统计。

历史提交可见 [2.14.0...2.15.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.14.0...2.15.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: reconfig the lineHeight when setInputText · [#948](https://github.com/Tencent-TDS/KuiklyUI/pull/948)
- fix: android rich text consistency · [#970](https://github.com/Tencent-TDS/KuiklyUI/pull/970)
- fix: core context scheduler issue · [#990](https://github.com/Tencent-TDS/KuiklyUI/pull/990)

### iOS

- fix: resolve text invisible issue when host overrides layerClass globally on iOS · [#955](https://github.com/Tencent-TDS/KuiklyUI/pull/955)
- fix(iOS): fix blank area at bottom when nested scroll with orientation change · [#958](https://github.com/Tencent-TDS/KuiklyUI/pull/958)
- fix: ignore erroneous scroll event during animation when setContentSize is called · [#961](https://github.com/Tencent-TDS/KuiklyUI/pull/961)

### 鸿蒙

- Shadow reuse issue · [#951](https://github.com/Tencent-TDS/KuiklyUI/pull/951)
- fix: activity default style issue · [#976](https://github.com/Tencent-TDS/KuiklyUI/pull/976)
- feat: ohos support system font · [#992](https://github.com/Tencent-TDS/KuiklyUI/pull/992)

### Compose

- fix: update scroll content size when scroll size changed without scrolling · [#930](https://github.com/Tencent-TDS/KuiklyUI/pull/930)
- fix: exception when scale to 0f · [#960](https://github.com/Tencent-TDS/KuiklyUI/pull/960)

### H5

- (H5) Show image after successful load · [#950](https://github.com/Tencent-TDS/KuiklyUI/pull/950)
- feat: h5 pag support asset path · [#963](https://github.com/Tencent-TDS/KuiklyUI/pull/963)
- feat: h5 not click for drag && text selection · [#980](https://github.com/Tencent-TDS/KuiklyUI/pull/980)
- feat: h5 support notify to native · [#986](https://github.com/Tencent-TDS/KuiklyUI/pull/986)
- fix: h5 nest pagelist · [#985](https://github.com/Tencent-TDS/KuiklyUI/pull/985)

### 跨端

- feat: trace page events · [#993](https://github.com/Tencent-TDS/KuiklyUI/pull/993)

### 多端

- feat: max length limit type · [#977](https://github.com/Tencent-TDS/KuiklyUI/pull/977)

### 其他

- 优化 Ohos APP 运行说明，明确区分 Mac 和 Windows 操作步骤 · [#941](https://github.com/Tencent-TDS/KuiklyUI/pull/941)
- fix: update core-gradle-plugin for web · [#962](https://github.com/Tencent-TDS/KuiklyUI/pull/962)
- docs: update iOS assets notes · [#969](https://github.com/Tencent-TDS/KuiklyUI/pull/969)
- chore: add tencent maven for settings.gradle · [#972](https://github.com/Tencent-TDS/KuiklyUI/pull/972)
- feat: update kuikly plugin · [#973](https://github.com/Tencent-TDS/KuiklyUI/pull/973)
- fix: add nestedScroll attr and backgroundLinearGradient attr use notes · [#952](https://github.com/Tencent-TDS/KuiklyUI/pull/952)
- fix: adjust iOS.md imageAdapter description format · [#978](https://github.com/Tencent-TDS/KuiklyUI/pull/978)
- fix: supplement KuiklyRenderViewDelegator use description in iOS.md · [#979](https://github.com/Tencent-TDS/KuiklyUI/pull/979)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

## 发布状态

2.15.0 已被标记为 deprecated，不建议作为新项目或生产基线；建议直接升级到 2.15.1 或更高版本。
