# Kuikly Compose DSL 正式发布：六端原生渲染的标准 Compose 实践

作者：Kuikly

发布时间：2026-06-03

原文链接：https://mp.weixin.qq.com/s/i5y6Mc2t9NyNhUQpB6TvNA

| 导语 Kuikly Compose DSL在 Kuikly 跨端框架的原生渲染引擎之上，实现了对标准 Jetpack Compose API 的支持，覆盖 Android、iOS、HarmonyOS、H5、微信小程序、Mac 六大平台。本文从背景与动机、架构设计、核心对接问题与实现、业务实践、未来方向与快速上手五个部分，介绍 Kuikly Compose DSL的设计思路、关键实现、落地情况与后续规划，并重点展开节点树、布局坐标系、手势与滑动等方向的对接工作。

## 一、背景与动机

[Kuikly](https://github.com/Tencent-TDS/KuiklyUI) 是腾讯开源的跨平台 UI 框架，基于 Kotlin Multiplatform、原生渲染路线，已在腾讯内部 **30+** App、**5 亿+** 日活场景验证；开源后，**快手、方正证券、MiniMax** 等 **20 余家** 外部团队已接入 Kuikly（含自研 DSL 与 Compose DSL）。

Jetpack Compose 已成为 Android 官方推荐的 UI 开发框架，Compose Multiplatform 也在持续向更多平台延伸。与此同时，主流 AI 大模型编写 Compose 代码的能力已相当成熟，从布局到动画，生成的代码基本可以直接运行。一个自然的问题由此浮现：Kuikly 能否也支持 Compose？

这就是 Kuikly Compose DSL 要解决的问题。在 Kuikly 上接 Compose，难的不是跑通 Demo，而是让 Compose Runtime 与六端原生渲染语义对齐——节点树、布局、手势、滑动等每一环都有独立的工程挑战。我们的目标是：

- **标准 API**：兼容 ≥ 95% 的 Jetpack Compose API，开发者无需学习新语法

- **AI 友好**：采用标准 Compose API，主流 AI 工具生成的代码开箱即用，无需额外适配

- **动态化**：继承 Kuikly 已有的动态化能力，支持热更新与动态下发

- **多平台覆盖**：一套代码覆盖 6 个平台

- **原生渲染**：复用 Kuikly 的原生渲染引擎，保持各平台原生体验

## 二、架构设计

### 2.1 整体思路

首先要思考的是：在已有 Compose Multiplatform 的情况下，Kuikly Compose DSL 能给开发者带来哪些**新的核心价值**？

- **平台覆盖**：额外支持 HarmonyOS 和小程序平台，覆盖当前国内业务的特殊诉求，这是 CMP 目前尚未覆盖的

- **动态化**：完整继承 Kuikly 的动态化能力，支持业务页面热更新与动态下发，这在追求灵活发版的场景下是不可替代的

- **原生渲染体系**：CMP 采用 Skia 自绘，而 Kuikly Compose DSL 可以直接复用现有成熟的原生渲染体系，拥有与原生系统完全一致的使用体验，例如滑动手感、输入框、动画、无障碍等

明确了核心价值之后，问题就回到了实现上——在 Kuikly 上支持 Compose，**从哪一层入手，开始对接 Kuikly？**

我们的做法是：完整复用 Compose 的 Runtime、高阶组件（LazyColumn、Pager、动画系统、布局引擎等）、手势系统等高阶能力，在原子组件层将渲染对接到 Kuikly 的原生渲染引擎。这样既保证了 API 与官方 Compose 高度一致，又复用了 Kuikly 已有的跨端渲染能力。

### 2.2 三层架构

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUD9OFBnn5LzlId17JuoGxjowMLMpTz71aZ7KAR61PaibkmvoBvtoV3RjxmsBD2dXUAZiah8KmhGdKqxAJhqTPdkIqX359gcN8qOQ/640?wx_fmt=png&from=appmsg)

如图所示，架构分为三层：最上层完整保留 Compose Runtime、布局引擎、动画、手势等核心逻辑，以及 LazyColumn、Pager、Material3 等高阶组件——开发者写的 `@Composable` 函数与标准 Jetpack Compose 完全一致，无需改动。最下层复用 Kuikly 已有的跨端渲染引擎，覆盖 6 个平台。

中间的适配层是 Kuikly Compose DSL自研的核心部分，负责解决两套渲染体系之间的语义差异：将 Compose 的节点树对接到 Kuikly 的原生视图树、处理坐标系映射、桥接手势事件、对接原生滑动组件。后文选取的几项对接工作，基本都发生在这里。

### 2.3 适配层不是「胶水层」

标准 Compose 默认将 `Applier` 接到 Android `View` 或 Skia `Canvas`——框架内置了统一的渲染语义。Kuikly 则是另一套跨六端、支持动态化的原生体系，布局单位、Bridge 调用、滚动容器行为均与 Compose 假设不同。

因此 Kuikly Compose DSL不能停留在「换一个 Applier 实现」：必须在 Runtime 之下重建节点同步、坐标映射、事件桥接、滑动对接等一整套语义。下文选取的若干代表性对接问题，都绕不开这层工作。

这里也解释下架构中两个关键决策：为什么选择原生渲染，以及为什么大量复用 Compose 上层。

### 2.4 为什么选择原生渲染

最初 Kuikly 选择原生渲染而非 Skia 自绘，核心考量有以下几点：

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUABp7lv7JBVwJzlqnrkTrwrrp89fPHfiasYBngMorm3xwB7HKlUYEGWVYUfZ2jYjn0IFcYcX38xSYariaHk28BEwe2KRmjRIwicPM/640?wx_fmt=png&from=appmsg)

注：安装包增量数据基于包含全部 Sample Demo 的 shared 模块测量。

### 2.5 为何大量复用 Compose 上层

大量复用上层的核心原因有两点：

**标准兼容**：大量复用 Compose 的成熟能力，意味着能够完整保留状态管理、重组机制、布局引擎、动画系统、手势处理等核心行为——开发者积累的 Compose 开发经验可以直接迁移，存量代码迁移成本极低，内部已有业务将 50+ 标准 Compose 页面完成了一次性迁移。

**AI 友好**：主流 AI 大模型的训练数据中包含了大量标准 Compose 代码，使用标准 API 意味着 AI 生成的代码可以直接在 Kuikly Compose DSL上运行，无需二次适配。这一点在实践中效果显著——我们用 AI 生成了 73 个功能 Demo，全部零修改编译运行。

Kuikly Compose DSL 覆盖了绝大多数常用 Compose API，涵盖以下模块：

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUDzJuKDPq9YnL7VxcAxMN1icrUSjvo97E8hPRbEwc73qOgeclzBXQDs30r5tPGFQ2JgibLvz5iaV2AicrHSHFrtnyC1bI4n5EkElnc/640?wx_fmt=png&from=appmsg)

## 三、核心对接问题与实现

架构方案确定之后，适配层有一系列对接问题，需要在框架建设阶段逐一攻克。下文先展开其中几项：

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUA7ZVdMlE10y5nFUibQGGicOJnMzBcMOzjHqiaGb8ZficeDJO9L1qgpGnV5Gkbg342Mxyu1DCWFarDV9oJ1PVWdCCOoh5VykX31KBw/640?wx_fmt=png&from=appmsg)

下文按表中所列顺序，对这几项展开说明。

### 3.1 节点树对接：KuiklyApplier

Compose 重组时会不断增删改节点，改动通过 `Applier` 交给渲染后端。Android、Skia 都有官方实现。Kuikly 用的是 `DeclarativeBaseView` 和跨端 Bridge，需要自己实现 Applier。常规的 insert/remove/move 接到原生树上，相对直接，但也有 `movableContentOf`、`SubcomposeLayout` 等特殊场景。

在 `movableContentOf`、`SubcomposeLayout` 这些场景下，Applier 收到的是 remove/insert，Compose 语义却是 move/reinsert，`LayoutNode` 实例不能直接销毁。Kuikly 每个节点连着 renderView 和 pager 的 nativeRef，不能走普通删除的完整销毁流程。`KNode` 通过 `removeChildForMove`、`reinsertChild` 先从 flex 树摘掉、保留原生 View，再插入时挂回 viewMap。

最终，`KuiklyApplier` 继承 `AbstractApplier<KNode>`，把常规增删和 move/reinsert 等语义，统一收敛到 `insertTopDown`、`insertBottomUp`、`remove`、`move` 四类回调里。每个 `KNode` 一侧绑定 `LayoutNode` 做布局，一侧绑定 `DeclarativeBaseView` 做渲染，保持 1:1 对应。下一节布局对接也沿用这一关系，不会为 Modifier 链额外膨胀 View 层级。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUAFGkp47kXTYNbYPOvJcV4VLORz38qlxkTA7KFEZiaeibKnFxImiaWPRreXez4dnic4wHZekmicCaYF55C3NR8aZjDIkstrZiciaNpCjc/640?wx_fmt=png&from=appmsg)

### 3.2 布局系统对接

最容易想到的做法是：布局结束后，把每个节点的 `(x, y, w, h)` 直接写给 Kuikly View 的 `frame`。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUBWRRu7DibRJibKRMt9dVtibRoO3utW6MOqWk6RricjNLapnuia1egAxAxOq0kJAoNhv7fyVtjaUw6sF4pic0DDaeKFxwblOj1HcecVI/640?wx_fmt=png&from=appmsg)

但实际情况比这复杂——`Modifier` 链会在 LayoutNode 内部产生多层嵌套坐标系，而 Kuikly View 只有一个 frame，该用哪层的值并不直观。

考虑以下代码：

```
Box(    Modifier.padding(10.dp).size(50.dp).background(Color.Blue))
```

`background` 绑定在 inner 坐标系，绘制区域是 50×50；加上 `padding` 后，outer 边界已是 70×70。右侧示意即 70×70 外框内的 50×50 蓝块。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUArHqC1UmRZDj0TtJ0cI1ibAEQVT9LVf5k8Jic503lrlrfnFXwNBtxqHarR1daszGczgfz0WLibic5bfZecS81Jwibndtagx2Wr439E/640?wx_fmt=png&from=appmsg)

一个 LayoutNode 内部有多层坐标系，但 Kuikly View 只有一个 frame。常见直觉方案都会踩坑：

- **选 outer 作为 frame**（不满足）：绘制尺寸错误。View 设为 70×70，`background` 也会铺满 70×70，但绘制只需 50×50。

- **选 inner 作为 frame**（不满足）：子节点错位。View 设为 50×50，绘制尺寸对了，但 padding 偏移丢失，子节点会错位。

- **每个 Modifier 创建独立 View**（不满足）：View 数量。3 个 Modifier 对应 3 个嵌套 View，复杂页面 View 层级爆炸，性能差。

理清上述矛盾后，约定如下：

## 绘制坐标系决定 Size，相对位置决定 Position。

- View 的 **size** 取第一个绘制 Modifier（background border shadow）绑定的坐标系尺寸

- View 的 **position** 由子 View 的绘制坐标系相对父 View 的绘制坐标系逐层累加 offset 得出

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUADUj1wxruktcicCvgw3PjsdbPHcqZcoxHiaM8WzP9LmuKJ4XicXLf2eV3IvOPVT04AXfnticxSwtoiba4CzJfyDWWEMbj3K9XumyxI/640?wx_fmt=png&from=appmsg)

这样既保证绘制属性与 View 尺寸匹配、子节点位置不丢失 padding 偏移，又维持 1 个 LayoutNode 对应 1 个 View，没有额外层级开销。

### 3.3 手势系统桥接

Kuikly 页面里常见原生列表（`ScrollView`）与 Compose 区域混排。触摸事件从原生系统进来，要先桥接进 Compose 的 `PointerInput` 管线；更棘手的是**同一视图树上的优先级**：点的是 Compose 区域，滑动的却是外层原生列表；或者 Compose 节点没绑手势，原生列表本该滚动，却因为事件被提前拦截而滑不动。

Compose 通过 `pointerInput` / `detectDragGestures` 等 API 识别拖拽、点击等手势。Kuikly 需要在「Compose 消费」和「原生滚动接管」之间双向协调：

- Compose 检测器消费了移动（例如可拖拽组件），通过 `preventTouch` 通知原生容器不要再处理这条触摸序列，避免外层列表抢滑动。

- 原生列表开始滚动、或系统手势接管时，把原生 `cancel` 映射给 Compose，让检测器及时退出，避免 Compose 占着事件却不处理，导致原生侧也滑不动。

## 事件管线

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUCNA8ZP0daGglXUMdWNyeDIKUOWfCEEX6OKbQ8GJK6enN31aibxbibmQXaVRhy30G2ibk4Qz7lLXUpHQIoZrX9UgWWpSEZmNn0oPY/640?wx_fmt=png&from=appmsg)

原生触摸到达 Compose 手势系统，需经以下处理：

1. **原生触摸捕获**：`SuperTouchManager` 在 Kuikly 容器上注册监听，将 down move up / cancel 捕获到 Kotlin 层

2. **坐标转换**：将 Kuikly 密度无关坐标乘以 `pageDensity`，转为 Compose 像素坐标

3. **事件合成**：`SyntheticEventSender` 补全缺失的合成事件（如多指场景下的 Press/Release），保证事件流完整

4. **命中测试与分发**：进入 Compose 官方的 `PointerInputEventProcessor` 与 `HitPathTracker` 三阶段分发

前三步为 Kuikly Compose DSL 补齐的桥接，第四步复用 Compose 自身机制。

### 3.4 滑动组件对接

选用原生滑动，是为了保留各端系统级手感；但 Compose 的 `LazyColumn` / `LazyRow` 只对可见项布局，原生 `ScrollView` 却需要可预估的**内容总高度**、稳定的 **contentOffset**，还要支持 `scrollToItem`——两套机制并不天然契合。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBQ6ACR2IdkIgLM0OHFmWkcRXBGUCLTS3ybI8bbQ2OKgYFJ4YcnapaqngTq0TExMyIavNicuwQVWHUdiayAtDLRZXo8jck4FprrY/640?wx_fmt=png&from=appmsg)

若强行让原生容器按「当前已布局高度」滚动，会出现总高度不足、回弹异常、`scrollToItem` 目标错位等问题。自绘滚动虽可统一模型，却难以在各端复刻原生惯性与嵌套滚动行为——这也是坚持原生渲染之后，必须单独攻克的对接问题。

**思路**：在保持 Lazy 语义的前提下，对原生容器做**动态扩容**——随用户滚动逐步扩展可滚动范围，并对已滚动偏移做 **Offset 补偿**，使可见区域与 Compose 布局结果一致；`scrollToItem` 等操作则触发目标区域重布局后再驱动原生滚动。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUC56SWzrtwy0B8sYWMKaWH7l3lwJDQ7rPwzVCNaSmEPBFuKaWCPcicm1Nd1efCS7WElCXAkO5sscWAxSicmIe2otCcMomicguZBAs/640?wx_fmt=png&from=appmsg)

该方案已在新闻、地图等业务的信息流与列表场景中验证，兼顾 Lazy 性能与原生滑动手感。

## 四、业务实践

Kuikly Compose DSL 已在腾讯新闻、新微视、腾讯地图（鸿蒙）、ima、部分创新 App 等多条业务线落地。开源后，**快手、网易邮箱、MiniMax、方正证券、螃蟹账号、微购相册、海尔H-work**等 **10 余家** 外部团队也已接入。

## 典型业务案例：

- **新微视**：以 AI 为主导的研发模式，整 App 采用全 Kuikly Compose DSL 方案建设，从主要功能到核心页面链路均基于 Compose 落地

- **腾讯新闻**：多个核心业务模块以 Compose 实现，一套代码覆盖 Android / iOS / 鸿蒙三端

- **腾讯地图（鸿蒙）**：鸿蒙端既有标准 Compose 页面整体迁移至 Kuikly Compose DSL，验证标准 API 兼容性与迁移可行性

## 五、未来方向与快速上手

### 未来方向

- **工具链持续完善**：[重组性能调试工具](https://kuikly.tds.qq.com/Compose/recomposition-performance.html) 已随官网文档开放；实时预览、Hot Reload、性能分析等能力仍在建设中，持续提升开发体验

- **AI 工具建设**：围绕标准 Compose API 的天然 AI 友好特性，探索从代码生成到页面搭建的 AI 辅助开发工具链

- **持续优化**：渲染链路优化、首屏性能优化

### 快速上手

Kuikly Compose DSL 已在 GitHub 开源，欢迎试用：

- **Kuikly官网**：[https://kuikly.tds.qq.com/](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical14)

- **GitHub**：[https://github.com/Tencent-TDS/KuiklyUI](https://github.com/Tencent-TDS/KuiklyUI)

- **快速开始**：参见仓库 README 中的 Getting Started 指引

如果你正在寻找一套标准 Compose API + 原生渲染 + 多平台覆盖的跨端方案，欢迎 Star、试用，并通过 Issue 或社区群反馈使用体验。我们也欢迎社区贡献——无论是组件适配、平台扩展还是文档完善。

Kuikly框架属于腾讯端服务联盟（tds.qq.com）的重要成员，欢迎关注及了解更多信息：

- 腾讯端服务官网： [https://tds.qq.com](https://tds.qq.com/?utm_source=artical14)

- TDS Framework官网： [https://framework.tds.qq.com/](https://framework.tds.qq.com/?utm_source=artical14)

![图片](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUDArBfiaIK9o3c5JCzS0QCr1gMoNniaB2GOfLzl6d2l8DNmF3y1kFuBsQsWtAzVhu19t6OWuKHrviaUcqDt0ic1VDVh0PhD4ia6tbyo/640?wx_fmt=png&from=appmsg&wxfrom=10005&wx_lazy=1&tp=webp#imgIndex=11)
