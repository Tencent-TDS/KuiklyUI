# 入门指南

本页帮助初学者了解应该阅读哪些文档来入门 Kuikly Compose，根据你的背景找到最适合的学习路径。

## 前置知识

在开始学习 Kuikly Compose 之前，建议先掌握以下基础知识：

### Kotlin
Kuikly Compose 使用 Kotlin 编写，你需要熟悉 Kotlin 的基础语法：
- [Kotlin 官方文档](https://kotlinlang.org/docs/home.html)

### Kotlin Multiplatform (KMP)
Kuikly Compose 基于 KMP 实现跨端，了解 KMP 有助于理解项目结构：
- [Kotlin Multiplatform 官方文档](https://kotlinlang.org/docs/multiplatform/kmp-overview.html)

**重点了解**：
- commonMain / platformMain 目录结构
- expect / actual 机制
- 平台特定实现

## Jetpack Compose 官方文档

Kuikly Compose 与官方 Jetpack Compose API 高度一致，**强烈建议先学习官方 Compose 文档**。

### Compose 入门必读

这些是理解 Compose 的核心概念，必须掌握：

1. **[Compose 编程思想](https://developer.android.com/develop/ui/compose/mental-model?hl=zh-cn)**
   - 理解声明式 UI 的思维方式
   - 了解重组（Recomposition）机制
   - 掌握状态驱动 UI 的理念

2. **[生命周期](https://developer.android.com/develop/ui/compose/lifecycle?hl=zh-cn)**
   - 理解 Compose 的生命周期
   - 了解初始组合、重组、离开组合
   - 掌握生命周期与状态的关系

3. **[状态管理](https://developer.android.com/develop/ui/compose/state)**
   - `remember`、`mutableStateOf` 的使用
   - 状态提升
   - 状态在重组中的行为

4. **[副作用](https://developer.android.com/develop/ui/compose/side-effects?hl=zh-cn)**
   - `LaunchedEffect`、`DisposableEffect` 的使用
   - 副作用的最佳实践
   - 理解副作用在重组中的行为

5. **[布局基础知识](https://developer.android.com/develop/ui/compose/layouts/basics)**
   - Column、Row、Box 的使用
   - Modifier 的链式组合
   - 布局测量与放置

### 推荐阅读

这些文档帮助你更深入理解 Compose，建议按需阅读：

1. **[为什么采用 Compose](https://developer.android.com/develop/ui/compose/why-adopt?hl=zh-cn)**
   - 了解 Compose 的优势
   - 理解与传统 View 系统的区别

2. **[界面架构](https://developer.android.com/develop/ui/compose/architecture)**
   - Compose 的架构设计
   - 生命周期
   - 副作用处理

3. **[CompositionLocal](https://developer.android.com/develop/ui/compose/compositionlocal?hl=zh-cn)**
   - 理解 CompositionLocal 的作用
   - 如何使用 CompositionLocal 传递隐式依赖
   - CompositionLocal 的最佳实践

4. **[列表](https://developer.android.com/develop/ui/compose/lists)**
   - LazyColumn、LazyRow 的使用
   - 列表性能优化

5. **[动画](https://developer.android.com/develop/ui/compose/animation/choose-api?hl=zh-cn)**
   - 动画 API 的使用
