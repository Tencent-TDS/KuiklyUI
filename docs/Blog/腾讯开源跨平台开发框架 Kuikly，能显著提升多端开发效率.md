# 腾讯开源跨平台开发框架 Kuikly，能显著提升多端开发效率

作者：TDF端框架助手

发布时间：2025-04-29

原文链接：https://mp.weixin.qq.com/s/56vXqvTuigT4H3OB1eD96g

Kuikly 是腾讯广泛使用的跨端开发框架，基于 Kotlin Multiplatform 技术构建，为开发者了提供技术栈更统一的跨端开发体验。已在 QQ、腾讯新闻、QQ 音乐、搜狗输入法、QQ 浏览器等业务中广泛使用，显著提升了多端开发效率。近期 Kuikly 已在 [GitHub 开源](https://github.com/Tencent-TDS/KuiklyUI)。那么，Kuikly 相对其他框架优势是什么？它的核心设计理念和技术原理是什么？现在，让我们一起探索 Kuikly 的奥秘。

## 背景

跨平台开发始终是大前端领域持续探索的重要方向，尤其是随着 Harmony Next 的推出，业务对跨平台开发诉求更加强烈。在新的需求场景下，业务希望在更多的场景进行更全面的跨平台开发，这对跨平台框架能力也提出了更高的要求。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsNGOBybqSRJuB0H9Jjo89mukiaO1nelT0AnbhXG0HInjV2CcRhwECNnQ/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

在跨端框架中，跨端能力、性能表现和动态化能力，作为开发框架的三个重要维度，一直比较难达到理想的平衡效果。

上图是从动态化和性能两个视角来看当前流行跨端框架在这三者的平衡情况。可以看到，动态语言跨端方案，如类 RN 框架、Web 容器类方案，能做动态化但性能较低。基于 AOT 编译的自绘跨端方案，如 Flutter、JetBrains Compose（以下简称JB Compose），能达到接近原生的性能但又不能动态化。这些局限，导致很难完美匹配业务诉求。

此外，类RN框架需要跨越 JS <-> 虚拟机 <-> C++ <-> Native 多重技术栈，技术复杂，语言之间也需要多重转换，也影响着性能。Flutter / JB Compose 基于自绘，包体积增量较大，同时自绘引擎初始化也影响其加载性能。

在此技术背景下，Kuikly试图突破现有框架的局限，在跨端、性能、动态化三者之间寻求更优的平衡，目标打造一套去中间层的纯原生跨平台动态化方案，具备原生级性能体验和原生技术栈开发体验。

### 能力和特性优势

Kuikly 已在腾讯内部大规模应用，目前覆盖 15+ 款 APP 、落地 500+ 业务页面，日均 PV 达亿级。部分业务在鸿蒙端完全采用 Kuikly 进行开发，进而复用到Android 和 iOS ，显著提升了跨端开发效率。

在深入解析 Kuikly 的技术原理之前，让我们先简单了解其在真实业务场景中的特性优势。

## 一码五端，支持鸿蒙平台

已支持 Android、iOS、鸿蒙、Web、小程序五个平台，其中已开源 Android、iOS 平台，计划 5 月份开源鸿蒙支持，Q2 开源 Web 和小程序支持。

## 原生级性能体验

得益于 KMP 跨平台能力，Kuikly 将 Kotlin 代码编译成各个平台原生产物（ .aar/framework等），从而获得接近原生平台的执行性能。

## 与原生性能对比(高中低端机)

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsf01Ee30HVwz31kLPl4CSELicFjg8v8mbsSBqDvKeFmBPnYnYTnNBNJw/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

- Kuikly 开发的页面首屏耗时，与原生基本一致；

- 在内存方面，由于 Kuikly 基于 KMP 编译及原生渲染，无额外引擎引入，内存增量与原生相差不大。

Kuikly测试数据基于复杂 Feeds 流 Demo 测试，Demo代码已包含在开源代码中

## 与其他框架性能对比

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsvpfb06dyBJ7p5oVjX0MxibzEUnoONMIBsRrQOLjpZBbdPwCFZ8y0N0Q/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

从整体看，Kuikly 在启动和内存上都具有更好的表现。

## 实际效果一览

通过与原生录屏对比，可以看到 Kuikly 和原生在体验上几乎没有差别。

Android：

从左至右分别是：原生、Kuikly-内置和Kuikly-动态化。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_gif/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsXXOfUHAMPP85qI6PeZDBR31DnkwwsThNfnnRSTHAHDjMteIFyPdIfA/640?wx_fmt=gif&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

iOS：

从左至右分别是：原生、Kuikly-内置和Kuikly-动态化。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_gif/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsllsRIpSich4Jvt0lqkIu0G9E8YWGkuJLFgfjTgT2uicPjmWnlFgjz7aw/640?wx_fmt=gif&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

## Kotlin 语言驱动，纯原生开发工具链

Kuikly 使用 Kotlin 作为开发语言，使用原生 IDE( Android Studio / VS Code ) 和原生性能分析工具，从业务代码到框架代码层，使用统一技术栈完成开发，调试和性能分析，从而实现框架开发技术栈自闭环。

各框架开发模式对比如下：

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsib7KpL3gTmqa9qj6yiciaIMdE1eyL0CpMg4LDXUZxJq6IkWia1g68BSWOA/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

## 声明+响应式 DSL

自研声明式 + 响应式 DSL，提升 UI 开发效率。同时，ComposeDSL 正在支持中，计划 Q2 正式开源。

## 支持页面级动态化

Kuikly 支持内置和动态化按需切换，具有页面维度更新、无 hook 稳定性高等优势。动态化模式性能表现上，Android 动态化采用平台产物，性能几乎没有损耗，即便在中低端机仍有接近原生表现。

## 轻量稳定易维护

框架整体设计精巧、无复杂外部依赖，框架稳定性、可控性和维护性较高。各流行框架安装包增量大小对比：

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRs24CwUE2mZAcngJQ9dyM4Nic86CrWicuMav7yMpFozob0a3NW2NByAs3w/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

### 实现原理和架构

一直以来，团队不断进行各种跨端语言的探索，随着 JetBrains 推出 KMM（Kotlin Multiplatform Mobile，现更名为 KMP）及不断完善 ，我们看到了更优的解决方案：

- Kotlin 是 Android 官方开发语言，天然兼容 Android 工具链和生态，终端开发者上手成本低。

- KMP 的跨平台编译能力将 Kotlin 代码编译为各平台原生产物（如 Android 的 JVM/ART 字节码、iOS 的 Native 二进制），可以支持多端高性能运行。

- 另外，KMP 也可以将 Kotlin 编译成 JS/Wasm 等产物，可以实现代码动态更新能力。

这些技术特性与我们需求较为契合，最终成为了 Kuikly 的核心技术基石。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsf9xELcolrJfyufkQttwxrQibRX57srhyjWmzo1zHl7ibY1wapma58gFQ/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

#### 架构图概览

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRs1ic5R3xZTLQK164hf1f8XUwNx60r3bcUYTbibXwTPLuhoI1AkGGcza1A/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

## 跨端 Core层（核心逻辑）：

- DSL 驱动：包含自研的声明+响应式 Kuikly DSL 与标准的 Compose DSL（建设中）；

- BuildTree：高性能的 DSL 组件树映射生成 Native UI 树的核心逻辑，包括2棵树映射方案、精细化的 O(1) Diff 更新算法、渲染指令生成等逻辑；

- UI 统一控件：核心是保证多平台一致性，在跨端层重新实现了大量的复杂高阶组件（如ListView、ViewPager、Waterfall等），少量 Native 原子组件则通过统一接口层直接映射到原生；

- 布局引擎：通过成熟的 yoga 布局引擎来支持流行的 FlexBox 布局

## Native 渲染层（特点是轻量化）：

- 统一的平台接口层：保证 Native 各平台能力的接口统一；

- 原生UI映射：最小化的原子渲染组件，仅 Text、Image、Input、ScrollView 等少量组件放到 Native 层；

- API 实现模块：供各平台扩展统一 API 提供给跨平台层使用；

Core 与 Render 层之间的通信，规避了 KMP 的 actual/expect 直接依赖调用方式，采用callKotlin/callNative 的指令通信方案，实现了 Kotlin 跨端层与 Native 渲染层之间的编译隔离，从而支持跨平台层（Core与业务代码）动态化更新能力。

**KuiklyBase 基础设施：**鸿蒙 Kotlin/Native 适配以支持鸿蒙高性能运行，以及调试、质量监控、发布、组件生态等配套基础设施。

### 关键技术点解析

#### 两棵树渲染原理

典型的渲染流程包括4个步骤：创建 ViewTree、测量、布局、绘制。为了保留纯原生渲染的体验，绘制这一步是放到了 Native 层，前三步都放在平台无关的 Kotlin 层。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsOzsy6ylCia8gQ4ASwdxUHymmT7XtrV3wz0YLcCY6EY6ichAfhNRRFiaag/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

与 Flutter、RN 等基于虚拟 Dom 的三棵树方案相比，Kuikly 采用跨平台 DSL 树直接映射生成 Native 渲染树的方案，实现了更轻量的渲染机制，进一步提升性能表现。

## Kuikly的两棵树渲染方案

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsOa3ZYH1lUaDY6CCCUvybQ9GBicIYJ2ic9seZx225R4EyGIyicDBuew0Rw/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

有了两棵树直调的渲染方案，接下来就是设计渲染指令，实现跨端和原生两棵树打通。如下图所示，不同平台 UI 均可以抽象增删改相关指令，通过调用各平台实现的这些抽象渲染接口，即可构建各端一致 UI 渲染。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRs37zp5s9wbW6icPWmUPAWPMia6ApJgy6BL2TTEM0XibnRAEnXNHqxOficqQ/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

#### 高一致性原生渲染方案

跨端框架的渲染主要有两种实现方式：原生（Native 控件）渲染，自绘渲染。前者的代表有 RN 框架，后者的代表有 Flutter 和 Compose。一般来说，原生渲染有更好的开发工具链支持、更贴近原生的交互体验以及更小的安装包占用；而自绘有着更高的跨端一致性，但交互体验无法完全对齐原生。综合考虑，Kuikly 选择了原生渲染。

在过去，原生渲染的原生控件多端行为不一致问题，是跨端开发的一大痛点。Kuikly 在设计之初，就考虑了这个问题，通过轻原生层的设计，原生 UI 仅提供最少量的原子组件，大量高阶组件则通过拼积木的方式，在 Kotlin 跨平台层实现，从而实现组件逻辑的高一致性。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsMod1SAgjoN3QZb31cQDTvtJAmYjS0wF83HjaicHCby05yH4XwxfD2dw/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

#### 声明式、响应式 DSL

业界的Compose/Swift 等现代声明式、响应式 DSL，它们在开发效率、代码可维护性、运行时性能等方面有着显著优势，Kuikly 借鉴了这套优秀理念，利用 Kotlin 语言的独特优势实现现代化开发体验：

- 通过高阶函数和类型安全构建器，实现优雅的声明式语法

- 基于属性委托机制，构建自动化的响应式更新系统

这种设计使得数据（ Model ）变更能够自动触发视图（ View ）更新，同时保持代码的简洁性和可维护性。以下是一个典型Kuikly页面示例：

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsaFYuzTljYtfEvs0BSFv7RAJy1wBIcYweQJurOgXSia06Jmg7Jjdtc3g/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

Kuikly 起步较早，Jetpack Compose（以下简称 Compose）的普及度不足、依赖要求较高，因此选择自研 DSL 。但随着 Compose 日趋广泛的使用，以及面向 AI 编程、适配业界生态的需要，Kuikly 当前也完成对 Compose DSL 的支持，近期也会逐步开源。

Kuikly 支持 Compose DSL 的整体思路是复用官方 Compose 的组件、布局、动画、事件等高阶能力，仅把渲染层从 Skia 自绘切换到 Kuikly 的跨平台渲染层（原生 View 绘制）。这样既保留了绝大部分原生 Compose 的 API 与能力，也能利用 Kuikly 的跨端渲染层，快速适配到鸿蒙、WebRender、小程序等其他平台；同时也能天然复用 Kuikly 的动态化能力，实现 Compose DSL 语法的动态化更新。

#### 极致渲染性能优化

除了前面讲述的渲染流程，Kuikly 在渲染指令量级、渲染节点量级、渲染指令直出等方面也做了较多细致的优化，以达到更加极致的渲染效果，以下是两个典型优化策略：

## 精准 Diff

在 UI 框架中，Diff（差异对比）是指通过算法比较新旧组件树的差异，找出需要更新的最小变更集，从而避免全量渲染，提升性能。Kuikly 通过重写Kotlin逻辑语句封装 Patch 指令（条件语句：if/else if/else，循环语句：for, switch语句：when），实现精准 Diff 能力，满足业务极致性能场景需求。

## UI 扁平化设计

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsk4ThRcr8KZc7yfYmlp3IicyscKl8Nz6cNAKJXuqUoeMyQ6hVPjtZKGw/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

UI 层级过深会导致测量/布局耗时增加、渲染性能下降，甚至引发卡顿。Kuikly吸收 RN 思路，结合自身现有2棵树设计，实现仅可视节点上屏：

- 第一棵树（原型树）引入可渲染节点接口，由子类重写实现是否需要渲染

- 对于仅布局的 View 节点重写接口返回为非渲染节点，避免被渲染映射原生控件

- 原型树同步渲染过程中，剔除非渲染节点，仅保留可渲染节点，维护 RenderTree 再进行控件1:1映射

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsM56nwgvdPnFkEfGOkX9f9gdSc1VEyA9YzDpJa75tFbcgOiaUR9DNicYg/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

#### 动态化

在设计之初，Kuikly 就考虑了动态化使用场景，通过全面的解耦设计，使得业务代码可以独立编译为各平台的可执行产物。即跨端代码与平台代码既可直调，也可以解耦动态调用，实现既可内置，又可以动态化的按需动态化能力。Android 端动态化采用平台产物，达到接近原生执行效率；iOS、鸿蒙动态化产物采用 JS，与其它类 RN 框架在同一水平，甚至更好（由于更轻量的DSL 设计）。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsStRzLojic3kktveOjhC36yAOaxgKWibrQ1wr0coIxAS3Ieqp6zibibicQWw/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

## KuiklyBase 基础设施

KuiklyBase 是在打造 Kuikly 过程中沉淀下来的一套基于 Kotlin 的跨平台开发的基础能力和设施，包括：

- 鸿蒙 Kotlin/Native 适配：扩展Kotlin编译鸿蒙二进制产物，以支持Kuikly在鸿蒙高性能运行（计划5月开源）；

- 跨三端的基础组件：扩展常用基础组件支持鸿蒙端，持续丰富组件生态（计划5月开源）；

- 完善的开发工具链：覆盖从插件脚手架到调试、构建、发布，监控等研发全流程配套工具；

- 堆栈捕获和上报能力：能够提高开发阶段的堆栈还原效率，也使线上crash问题能够得到有效的监控和精准定位

期望这套 KuiklyBase 跨端基础设施，能够帮助开发者更高效地专注于 Kuikly 应用开发，提升整体研发效率。

### 计划

## 近期计划

Kuikly已有能力正在逐步开源，近期计划开源项：

- 鸿蒙：5月开源

- H5、小程序平台：Q2 开源

- Compose DSL 支持：Q2 开源

## 未来计划

跨平台开发技术需要不断进化，以适应日新月异的终端形态和业务需求。Kuikly 作为腾讯开源的面向客户端开发的跨平台开发框架，会始终紧跟技术发展趋势。未来，Kuikly 将持续进化，为开发者带来技术栈更一致的跨端开发体验。

- 框架能力进一步提升

- 配套工具完善

- 更多平台支持，如Windows/Mac桌面端

- 建设国际化能力

- 社区生态建设

- 建设社区组件平台

- 建设社区交流氛围

期待社区的优秀开发者能一起参与进来，共同打造一套：一码多端、极致易用、动态灵活的全平台高性能开发框架。

🚀 立即体验 Kuikly，加入开源社区。

👉 [GitHub 仓库](https://github.com/Tencent-TDS/KuiklyUI) | 📚 [官方文档](https://kuikly.tds.qq.com/%E7%AE%80%E4%BB%8B/arch.html)

## 欢迎关注

Kuikly框架属于腾讯端服务联盟成员，欢迎关注及了解更多信息：

腾讯端服务官网：[https://tds.qq.com/](https://tds.qq.com/)

TDS Framework官网：[https://framework.tds.qq.com/](https://framework.tds.qq.com/)

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvattxeD0NI9Qb2EgJkk0EWRsGvuvKp6ticsppgicWLaialjYnIqsoRPWO6ZBZX2C8DVEfnfQQUmy8Ut2A/640?wx_fmt=other&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)
