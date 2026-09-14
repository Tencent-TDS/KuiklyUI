# 2.17.0 版本说明

> 本版导航：**版本说明** · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)

2.17.0 在 Kuikly DSL 与 Compose DSL 上均有能力新增，并集中修复了多端滚动与手势问题。

## Kuikly DSL

### 能力增加

- **视图快照（iOS / Android）**：支持将页面或指定视图渲染为图片（#1066）。
- **输入框键盘收起控制**：支持自定义发送按钮动作触发的键盘收起行为，业务可以控制键盘收起时机（#1045）。
- **鸿蒙文本首行缩进**：文本支持首行缩进属性（#1182）。
- **H5 文本换行边距事件**：文本换行时提供回调（#1196）。
- **H5 默认 KRCustomPropsHandler**：H5 渲染新增默认的自定义属性处理器，业务可扩展属性处理逻辑（#1175）。

### 性能优化

- **鸿蒙 ArkTS 列表复用**：ArkTS 懒加载列表支持复用 Kuikly 组件，减少滚动时的创建开销（#1159）。

### Bug 修复

修复 iOS 图片中文路径加载、macOS 鼠标手势与文本视图递归、Android 嵌套滚动与多指坐标、鸿蒙 PAGView 自动播放与 Preferences 单例、H5 自定义字体显示等问题，逐条见「变更汇总」。

## Compose DSL

### 能力增加

- **Compose Navigation 支持**：新增导航能力，并提供 NavBackStackEntry 的 ViewModelStore 生命周期管理（#1147、#1184）。
- **Drawer 组件**：新增侧边抽屉布局组件（#1179）。
- **RecompositionProfiler**：新增重组性能分析工具，支持业务自定义过滤规则（#1230、#1252、#1264）。

### 性能优化

- **嵌套列表滚动复用**：LazyList / LazyGrid / Pager 中的 ScrollerView 支持复用，减少滚动时的节点创建开销（#1125）。

### Bug 修复

修复 lazy grid 滚动、pager bounce 触发翻页、ScrollableTabRow 嵌套滚动、pager 包装器 scrollend 展开、协程取消异常等问题，逐条见「变更汇总」。

## 升级建议

本版无破坏性变更，可直接升级。
