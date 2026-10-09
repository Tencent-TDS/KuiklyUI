# 2.15.3 变更说明

## Part 1 总体说明

2.15.3 让滚动在自定义缩放与嵌套场景下的边界表现更精确，修掉了多端渐变、文本与动效绘制的若干错误，同时补齐了常用的端能力，构建与混淆配置也更完整。具体如下：

- **滚动与边界绘制精度**：Android 自定义 density 下的 1px 边缘缺失被修复，嵌套滚动不再产生多余回弹，无弹簧需求时改用平滑滚动；scrollToTop 事件可被业务拦截，Compose 滚动组件也支持回到顶部，超出边界的重绘范围得到优化。
- **渐变、文本与动效绘制**：修掉 iOS 画布渐变色失效与渐变扩散异常、macOS 渐变色复用错误、Compose DSL 在 iOS 上的颜色异常；GIF 切换资源前停止动画并复位到首帧，文字阴影的属性下发次数明显减少，鸿蒙图标字体渲染同步优化。
- **端能力与工程兼容**：鸿蒙新增本地存储 Module，新增 32 位 MD5 摘要接口；补齐 ProGuard 规则并解决 Kotlin 模块元数据冲突，同时修复鸿蒙冷启动失败、macOS 与鸿蒙编译错误等问题。

历史提交可见 [2.15.2...2.15.3](https://github.com/Tencent-TDS/KuiklyUI/compare/2.15.2...2.15.3)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: android fix 1px edge miss when using custom density · [#1027](https://github.com/Tencent-TDS/KuiklyUI/pull/1027)
- feat: android avoid overscroll when nest scroll · [#1042](https://github.com/Tencent-TDS/KuiklyUI/pull/1042)
- fix: add proguard rule · [#1070](https://github.com/Tencent-TDS/KuiklyUI/pull/1070)
- fix: IKuiklyRenderCoreScheduler backward compatibility · [#1073](https://github.com/Tencent-TDS/KuiklyUI/pull/1073)
- feat: optimize android over-bounds redraw · [#1065](https://github.com/Tencent-TDS/KuiklyUI/pull/1065)
- fix: add proguard rule · [#1082](https://github.com/Tencent-TDS/KuiklyUI/pull/1082)
- fix: android use smoothScrollBy when no spring need · [#1084](https://github.com/Tencent-TDS/KuiklyUI/pull/1084)
- feat(android): support scrollToTop event interception for android · [#1088](https://github.com/Tencent-TDS/KuiklyUI/pull/1088)
- fix: stop GIF animation before src change and reset to first frame on start · [#1093](https://github.com/Tencent-TDS/KuiklyUI/pull/1093)

### iOS

- fix: ios canvasView gradient color not work · [#474](https://github.com/Tencent-TDS/KuiklyUI/pull/474)
- fix: iOS synchronize set safeAreaInset · [#1007](https://github.com/Tencent-TDS/KuiklyUI/pull/1007)
- fix: iOS LineGradientGradientcolor diffusion abnormaly · [#1068](https://github.com/Tencent-TDS/KuiklyUI/pull/1068)

### 鸿蒙

- feat: icon font optimization · [#1031](https://github.com/Tencent-TDS/KuiklyUI/pull/1031)
- feat: ohos add KROhSharedPreferencesModule · [#998](https://github.com/Tencent-TDS/KuiklyUI/pull/998)
- fix: ohos compile error caused by KRRenderValue in KROhSharedPreferencesModule · [#1046](https://github.com/Tencent-TDS/KuiklyUI/pull/1046)
- fix: cold-start failure in OH_ArkUI_NodeContent_RegisterCallback · [#1052](https://github.com/Tencent-TDS/KuiklyUI/pull/1052)

### Compose

- fix: adjust scaffold snackbarHost order · [#1030](https://github.com/Tencent-TDS/KuiklyUI/pull/1030)
- feat: compose textfield maxLength api · [#1037](https://github.com/Tencent-TDS/KuiklyUI/pull/1037)
- fix: ComposeDSL color abnormal effect on iOS · [#1026](https://github.com/Tencent-TDS/KuiklyUI/pull/1026)
- feat(perf): reduce setProp calls for text shadow rendering · [#1043](https://github.com/Tencent-TDS/KuiklyUI/pull/1043)
- fix: compensate Modifier padding delta in realContentSize calculation · [#1078](https://github.com/Tencent-TDS/KuiklyUI/pull/1078)
- fix: `META-INF/**.kotlin_module` name conflicts · [#1087](https://github.com/Tencent-TDS/KuiklyUI/pull/1087)
- feat(compose): add scrollToTop support for Compose scrollable components · [#1090](https://github.com/Tencent-TDS/KuiklyUI/pull/1090)
- chore: remove useless gradle files · [#1094](https://github.com/Tencent-TDS/KuiklyUI/pull/1094)
- refactor: replace extProps HashMap with dedicated RenderProperties class for node render attributes · [#1101](https://github.com/Tencent-TDS/KuiklyUI/pull/1101)

### H5

- feat:prevent select text for h5 · [#1051](https://github.com/Tencent-TDS/KuiklyUI/pull/1051)
- fix: fix miniapp matchmedia · [#1081](https://github.com/Tencent-TDS/KuiklyUI/pull/1081)

### macOS

- fix: MacOS KRScrollerView setFrame compile error · [#1036](https://github.com/Tencent-TDS/KuiklyUI/pull/1036)
- fix: macos abnormal gradientColor reuse · [#1096](https://github.com/Tencent-TDS/KuiklyUI/pull/1096)

### 多端

- feat: add md5With32() API for 32-bit MD5 support · [#1018](https://github.com/Tencent-TDS/KuiklyUI/pull/1018)

### 其他

- docs: update ohos compile plguin notes · [#1035](https://github.com/Tencent-TDS/KuiklyUI/pull/1035)
- fix: Android ImageView reuse drawable modidy problem · [#1034](https://github.com/Tencent-TDS/KuiklyUI/pull/1034)
- docs: correct docs link · [#1044](https://github.com/Tencent-TDS/KuiklyUI/pull/1044)
- docs(compose): add FAQ for pointerInput gesture event issues · [#1074](https://github.com/Tencent-TDS/KuiklyUI/pull/1074)
- docs: add thread execution info and performance notes for Module methods · [#1098](https://github.com/Tencent-TDS/KuiklyUI/pull/1098)
- feat: ios add kuikly compatible with swift description · [#1099](https://github.com/Tencent-TDS/KuiklyUI/pull/1099)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。
