# 2.17.0 版本说明

> 本版导航：**版本说明** · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)

2.17.0 扩展了 Kuikly 页面与原生宿主的组合方式，补充了 Compose 导航、抽屉和重组分析能力，并改善了多端输入、截图与滚动体验。

## Kuikly DSL

### 能力增加

- **以 View 粒度嵌入 Kuikly 页面（Android / iOS / 鸿蒙）**：可以把 Kuikly 页面作为子 View 嵌入原生 Activity、ViewController 或 ArkUI 列表/容器，用于卡片、Banner、瀑布流等混合页面；View 模式下由宿主负责尺寸和生命周期转发。接入方式见 [Android 工程接入](../../QuickStart/android.md)、[iOS 工程接入](../../QuickStart/iOS.md) 和 [鸿蒙工程接入](../../QuickStart/harmony.md)（#1228）。
- **视图截图（iOS / Android）**：可以将当前 View 转换为图片，并按 `CACHE_KEY`、`DATA_URI` 或 `FILE` 获取结果，适合分享、预览和缓存复用场景。使用方式见[基础属性、事件和方法](../../API/components/basic-attr-event.md)（#1066）。
- **输入框 IME Action 键盘控制（Android / iOS / 鸿蒙）**：通过 `autoHideKeyboardOnImeAction` 或 `Modifier.autoHideKeyboardOnImeAction`，业务可以明确控制点击 Send、Go、Search、Done 等键盘操作后是否收起软键盘；未显式设置时各端默认行为不同，需要跨端一致时建议主动设置。使用方式见 [Input 组件](../../API/components/input.md) 和 [Compose 核心组件](../../Compose/core-components.md)（#1045）。
- **鸿蒙文本首行缩进**：`Text` 和富文本可以使用 `firstLineHeadIndent` 设置首行缩进，后续行保持正常对齐（#1182）。
- **H5 DOM class 扩展**：H5 页面可以通过 `cssClass` 给 `View`、`Text` 等节点附加或动态更新 CSS class，复用宿主已有样式。使用方式见 [cssClass 使用说明](../../DevGuide/h5-css-class.md)（#1175）。
- **H5 文本折行边距事件**：H5 文本在折行或省略号场景下可以通过 `onLineBreakMargin` 感知预留边距相关变化，用于配合“更多”等交互（#1196）。

### 性能优化

- **鸿蒙列表组件复用**：鸿蒙列表滚动场景支持复用 Kuikly 组件，减少列表项反复创建带来的开销（#1159）。

### Bug 修复

本版修复了 iOS 图片中文路径加载、macOS 鼠标手势与文本视图递归、Android 嵌套滚动和多指坐标、鸿蒙 PAGView 播放与偏好设置、H5 自定义字体显示等问题，详情见「变更汇总」对应端的条目。

## Compose DSL

### 能力增加

- **Compose Navigation**：可以使用 `NavHost`、`NavHostController` 和导航 DSL 声明页面路由，支持参数传递、嵌套导航图、返回栈操作和页面切换动画；使用方式见[导航组件](../../Compose/navigation.md)（#1147）。
- **页面级生命周期与 ViewModel 管理**：每个 `NavBackStackEntry` 可以承载独立的生命周期和 `ViewModelStore`，页面从导航栈移除时可自动完成对应状态清理；使用方式见[导航组件](../../Compose/navigation.md)和[Compose 核心组件](../../Compose/core-components.md)（#1184）。
- **Navigation Drawer**：新增 `ModalNavigationDrawer` 和 `DismissibleNavigationDrawer`，支持抽屉状态、手势开关、遮罩关闭及内容推开效果；当前 Semantics、RTL 和 Permanent Drawer 等部分能力仍在建设中。使用方式见[Compose 核心组件](../../Compose/core-components.md)（#1179）。
- **RecompositionProfiler**：调试阶段可以采集 Composable 的重组次数、耗时、State 触发和参数变化，并通过日志、文件报告或悬浮面板定位重组热点；支持按名称或包名前缀过滤基础组件。使用方式见[重组性能分析工具](../../Compose/recomposition-performance.md)（#1230、#1252、#1264）。

### 性能优化

- **嵌套列表滚动复用**：`LazyList`、`LazyGrid` 和 `Pager` 的嵌套滚动场景支持复用列表节点，减少滚动过程中的节点创建和回收开销（#1125）。

### Bug 修复

本版修复了 Pager 反向 fling 误触发翻页、`ScrollableTabRow` 嵌套滚动、Pager wrapper 的 `scrollend` 展开以及协程取消异常等问题，详情见「变更汇总」的多端条目。

## 升级建议

本版无业务可感知的破坏性变更，可直接升级；完整扫描结论见[破坏性与迁移](./breaking.md)。
