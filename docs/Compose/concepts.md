# 核心概念与架构总览

本页目标：帮你快速建立一张 **「Kuikly 版 Compose 心智图」**，理解 Kuikly 与 Jetpack Compose 在架构上的分工与差异。

### 与 Jetpack Compose 的关系（再强调一次）

- **完全复用**：`androidx.compose.runtime`（State / Snapshot / Recomposer / SideEffect 等）
- **包名改造**：UI / Foundation / Material3 等迁移为 `com.tencent.kuikly.compose.*`
- **渲染栈替换**：不使用 AndroidComposeView / Skia，而是通过 `KuiklyApplier` 接入 Kuikly Core

因此你可以认为：

> **上层是标准 Compose 语义，下层换成了 Kuikly 的跨端渲染与动态化引擎。**

一个简单的概念映射表：

| Jetpack Compose 概念           | Kuikly Compose 中的对应物                         |
| ------------------------------ | ------------------------------------------------- |
| `@Composable` / Composition    | 保持一致，运行在 Kuikly 的 ComposeContainer 中   |
| `Recomposer` / Snapshot        | 直接使用官方实现                                 |
| Layout（Measure/Place）        | 通过 KuiklyApplier 映射到 Kuikly 原子组件布局    |
| Modifier                       | 映射为 Kuikly 的属性与事件（Attr/Event）         |
| `AndroidComposeView` / Skia    | 替换为 Kuikly Core + 各平台 Render                |
| ViewModel / Repository（示例） | 推荐放在 Core / 业务层，通过 Module 暴露给 Compose |

### ComposeContainer：页面容器

在 Kuikly 中，一个 Compose 页面通常继承自 `ComposeContainer`：

- 继承自 Kuikly Core 的 `Pager`，拥有统一的跨端页面生命周期
- 暴露 `setContent(content: @Composable () -> Unit)` 方法，用于设置页面 UI
- 内部会创建 `Composition`，使用 `KuiklyApplier` 作为渲染适配器

典型用法：

```kotlin
class DemoPage : ComposeContainer() {

    override fun onCreate() {
        super.onCreate()
        setContent {
            DemoScreen()
        }
    }
}
```

你可以把它类比为 **“Compose 版 Kuikly 页面基类”**。

### KuiklyApplier：渲染适配器

`KuiklyApplier` 是 Kuikly Compose 中最关键的一层：

- 类型：`AbstractApplier<KNode<DeclarativeBaseView<*, *>>>`
- 职责：接收 Compose 树的 insert / remove / move / update 等操作
- 行为：把这些操作转化为对 Kuikly 原子组件树（KNode）的增量更新

这意味着：

- Compose 负责 **描述 UI** 和 **管理状态/重组**
- Kuikly Core 负责 **跨端渲染** 和 **动态化**

两者通过 `KuiklyApplier` 解耦，形成「双栈分工」：

- 上层：`Composable → Composition → Recomposer`
- 中间：`KuiklyApplier` 做树结构与属性的映射
- 下层：`Kuikly Core → 各平台 Render（Android/iOS/Web/鸿蒙/小程序）`

### 跨端模型：KMP + 原子组件

在工程结构上：

- **commonMain**：放置绝大部分 Compose UI 代码（页面、组件、样式）
- 各平台 `androidMain` / `iosMain` / `webMain` 等：
  - 配置平台入口
  - 嵌入 Kuikly 渲染视图
  - 处理平台特有能力（权限、系统 UI、平台 SDK 等）

在渲染模型上：

- Compose 层只直接接触到 Kuikly Core 提供的「原子组件」抽象（Text、Image、ScrollView、List 等）
- 各端的 `core-render-*` 模块负责把原子组件映射到具体平台视图

> 重要结论：**你的 Compose 代码基本可以认为是「跨端 UI 描述层」，不需要直接处理平台 View 细节。**

### 状态与重组：完全沿用官方语义

因为 Runtime 完全使用官方实现，所以：

- `remember` / `mutableStateOf` / `derivedStateOf` 等用法与 Jetpack Compose 一致
- Snapshot / Recomposer 行为与官方一致
- SideEffect / LaunchedEffect / DisposableEffect 等副作用 API 行为保持一致

当你从 Jetpack Compose 迁移到 Kuikly Compose 时：

- 与「状态管理相关」的大部分代码可以直接复用
- 需要关注的主要是：
  - 包名切换（`androidx` → `com.tencent.kuikly.compose`）
  - 少数未支持或行为有差异的组件 / Modifier（稍后在「布局与组件」中会有差异清单）

### 动态化与 Module 协同（预告）

Kuikly 的一大优势是 **动态化与模块化能力**：

- 通过 Core 的 Module 体系提供网络、存储、埋点、路由等跨端服务
- 通过 DSL / 动态配置下发页面结构与数据

在 Kuikly Compose 中：

- 推荐把业务逻辑和跨端能力封装在 Module 中
- 在 `@Composable` 中调用这些 Module 提供的 API，进行数据加载、导航跳转等操作

更多细节会在单独章节中展开：

- [与 Core 模块协同](./interop-core.md)

### 学习建议

如果你来自 Jetpack Compose 背景：

1. 先快速浏览「快速开始」和本页，了解包名与架构差异
2. 重点关注「布局与组件」「列表与滚动」「多端差异」章节
3. 遇到基础问题，优先查 Jetpack Compose 官方文档

如果你来自 iOS / Web / Flutter / RN 背景：

1. 建议先用 Kuikly Compose 去完成 1~2 个小页面（照抄示例练习）
2. 并行阅读 Jetpack Compose 官方基础教程，熟悉声明式 UI 与状态管理模式
3. 再回来看 Kuikly 在跨端和动态化上的改造，你会更有直观感受


