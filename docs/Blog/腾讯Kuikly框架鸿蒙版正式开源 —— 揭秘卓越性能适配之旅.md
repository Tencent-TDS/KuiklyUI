# 腾讯Kuikly框架鸿蒙版正式开源 —— 揭秘卓越性能适配之旅

作者：TDF端框架助手

发布时间：2025-06-03

原文链接：https://mp.weixin.qq.com/s/KsEqVO_Zh73xSlUWOrvZrQ

## 一 背景

Kuikly 是腾讯广泛使用的跨端开发框架，基于 Kotlin Multiplatform 技术构建，为开发者提供了技术栈更统一的跨端开发体验，由**腾讯大前端领域 Oteam（公司级）**推出。在Android、iOS开源基础上，本次开源**鸿蒙平台支持**和**Compose DSL支持**，进一步提升业务多端适配和鸿蒙开发效率。目前Kuikly鸿蒙版已接入腾讯多款业务，开发并上架鸿蒙App，如QQ浏览器、腾讯新闻、搜狗输入法、全民K歌、自选股等。

## 二 鸿蒙效果适配方案

Kuikly以高性能、动态化为框架核心目标。在鸿蒙Next系统推出后，Kuikly较早投入适配工作，得益于轻量渲染架构的设计很快完成初版。经过持续的迭代和打磨优化，Kuikly鸿蒙版完整适配，并取得了原生性能表现。

实测数据如下：

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdqbblWfiaEUOwBmbT1GYicia9iajgDRuSJxtMibyiczVcjHiagtf9T1u4ecE7A/640?wx_fmt=other&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

（测试场景是一个较复杂的Feed流场景，机型均为华为Mate60）

- 在鸿蒙平台上，Kuiky打开页面速度比RN快6倍:

![图片](https://mmbiz.qpic.cn/sz_mmbiz_gif/j3gficicyOvasnhWPhxbgksvficRlCFMObd8h2hc23KX75xQEdcr4xhrjyE3ibLFmq0V9MJGS8wIcibZbzhnrN55ELQ/640?wx_fmt=gif&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

- Kuikly鸿蒙版对齐Android版高性能表现，与原生打开速度基本一致:

![图片](https://mmbiz.qpic.cn/sz_mmbiz_gif/j3gficicyOvasnhWPhxbgksvficRlCFMObdzQ74icWmoFso0ltSwwicibvqxT7IE7zAw4p1pJMZrSVLiaMUVZLWkbo0xQ/640?wx_fmt=gif&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

### 三 总体适配方案

#### 1、Kuikly架构回顾和优势

Kuikly是一个一码多端、追求极致性能、动态化、原生体验的开发框架，技术上以Kotlin Multiplatform为依托，解决了过去业界跨端框架普遍存在的性能、体验跟原生不一致以及开发生态问题；设计上最大程度将逻辑实现在Kotlin跨端侧，使Native侧的逻辑极致轻量。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdjBeu01mwp6Y8kUogqvDgzeLcRLo86EnVWhrEibY7KhgsblUzsSddiavA/640?wx_fmt=other&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

## Kuikly包括两部分：

- KuiklyUI：支持业务使用自研DSL和Compose DSL进行UI跨端开发，采用轻量、原生渲染方式，支持页面级动态化。

- KuiklyBase：支持UI和KMP逻辑全面跨端的基础能力和设施，包括丰富的跨端组件，完善的调试、构建、发布、监控配套工具链，稳定性监控能力等。

## Kuikly框架优势：

- 一码五端，支持Android、iOS、鸿蒙、Web、小程序5个平台（Web、小程序Q2开源）。

- 原生级性能体验：得益于 KMP 跨平台能力，Kuikly 将 Kotlin 代码编译成各个平台原生产物，从而获得接近原生平台的执行性能。

- Kotlin 语言驱动，纯原生开发工具链：复用原生 IDE( Android Studio / VS Code ) 和原生性能分析工具，从业务代码到框架代码层，使用统一技术栈完成开发，调试和性能分析，从而实现框架开发技术栈自闭环。

- 声明+响应式 DSL：自研声明式 + 响应式 DSL，提升 UI 开发效率。同时，ComposeDSL本次也同步开源Beta版本。

- 支持页面级动态化：按需使用内置和动态化模式，稳定、高性能，在Android上动态化模式采用平台产物，性能几乎零损耗，即便在中低端机仍有接近原生表现。

- 轻量稳定易维护：框架整体设计精巧、无复杂外部依赖，框架稳定性、可控性和维护性较高。

#### 2、总体适配方案概述

Kuikly鸿蒙适配是一项复杂、系统化工作，同时为了达到高性能、原生渲染、动态化等适配目标，进行了持续的探索和优化。其核心适配工作包括：对接鸿蒙UI系统，封装原子组件，对接事件系统，优化和解决性能及稳定性问题；Kotlin跨端层逻辑编译为鸿蒙上可高效运行的Native产物，探索Kotlin JS和Kotlin Native在鸿蒙平台上的适配落地及其性能优化；打通跨端层和鸿蒙原生层的相互调用通道，并驱动框架和App整体工作起来；建设调试插件以及Crash监控等开发及运营阶段所需的基础设施。

限于本文篇幅无法一一介绍，本文将选取部分核心工作进行展开。

### 四 渲染层的适配

#### 1、ArkUI上渲染指令映射问题

## a）原始问题：如何用声明式接口映射渲染指令

在Android、iOS平台上，系统都提供了命令式的UI接口，这种命令式UI接口非常符合Kuikly的渲染层抽象，可以等价直调系统接口操作原生控件。然而鸿蒙平台的用户界面是通过声明式的 ArkUI 来编写的，UI组件由数据和UI描述组成，UI更新只能通过修改其绑定的数据来实现。渲染层怎样驱动声明式的ArkUI成为了鸿蒙版适配的第一个问题。

## b）初步解决方案：统一Builder入口，全量属性绑定

经过一系列的探索尝试，我们全新设计了鸿蒙版的渲染层方案：

**组件属性的更新：**在渲染层维护一个渲染节点的数据抽象，渲染节点通过 ArkUI 的装饰器与 ArkUI 组件绑定，这样对渲染节点属性的增、删、改就能触发UI属性的更新；

**UI树的描述和更新：**用到了一个关键的接口ForEach（循环渲染），这个接口可以绑定一个数组类型的数据，以此创建子UI组件。我们把渲染节点的数据抽象关联为渲染节点树，以节点的children作为数据，配合递归调用ArkUI的builder方法，就实现了对整个UI树的描述和增、删、改。

## c）新的问题：声明式接口映射方案性能不理想

通过ArkUI声明式UI实现功能后，我们也很快输出了第一个版本，但是性能评测结果不及预期，究其原因有：

1. 因为组件外层被设置属性后，会导致组件层级实际被加倍，这会影响性能表现；

2. 由于无法按需绑定属性，只能统一绑定所有属性，修改一个对象可能触发全量属性更新；

3. 通过复用组件帮助也不大，因为准备复用所需要的调用耗时会成为瓶颈；

4. Kotlin JS转鸿蒙字节码模式效率不高。

## d）终极解决方案：采用命令式CAPI实现指令的映射

再回顾上面Android、iOS的实现示例来，可见处理渲染指令最自然的方式是用命令式接口。经过和鸿蒙系统侧的沟通，我们拿到尝鲜版的API，经过调研发现它的性能优势比较明显：C风格的API其实更适合Kuikly的场景，同时C的效率更佳并且和Kotlin Native可以直调、无额外跨VM/语言调用开销。除了早期CAPI版本存在一些Bug和功能缺失等问题，CAPI方案也在开发难度、复杂性上也更高，但为了追求极致性能表现，我们最终决定采用CAPI方案。

新旧方案渲染层级变化：

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdLXCXeLicUNMLd31PUsBD7nicL0dv5YVhx5WX8oaIRtxdsPxiaI04KowYQ/640?wx_fmt=other&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

#### 2、C节点和ArkTS组件嵌套问题

实际业务使用场景中，普遍存在自定义控件和Kuikly内置控件混用嵌套的情况，也就是C节点和ArkTS组件混合嵌套的情况。由于ArkTS组件都是声明式的，早期没有方案动态为其添加C的子节点，因此无法直接解决混合嵌套问题。

#### 解决：影子节点无痛兼容现有组件，内容插槽方案兜底

CAPI命令式接口可以为C节点添加C的子节点，也可以在C节点上挂载ArkTS节点作为子节点。因此我们设计了一种绕开对ArkTS节点进行操作的方法，以解决嵌套问题。

我们首先增加一个C的影子节点，它和业务自定义组件大小是等大的，我们会把基础属性自动设置给影子节点，同时也会"抄送"给业务自定义组件；此后当需要为业务自定义组件挂载子节点的时候，我们只需要把子节点挂载在影子节点上即可，这样视觉上就能实现符合预期的效果。

这个方式可以完美解决嵌套问题，其好处是可以零修改无痛兼容业务现有组件和第三方组件，但一点不足的是，这事实上把本该是父子关系的自定义节点及其子节点，变为了兄弟节点，当业务需要对自定义节点进行一些特别操作，如transform的时候就容易出现UI异常。

在鸿蒙API 12版本推出后，我们进一步增加了对自定义节点内容插槽的支持，当业务有特殊需要的时候可以按照规范在自定义组件中增加内容插槽，走这个模式的时候我们将不会为其生成影子节点，而子节点我们也会挂到插槽上，这样UI树就能和预期保持一致。

#### 3、文本渲染性能的优化

## a）首个实现方案：文本测量和渲染分离方案

由于我们最终采用的是CAPI，所以这里不再赘述基于ArkTS接口的文本渲染方案。

在CAPI版本中，我们最早探索了文本测量和渲染分离的方案，即在测量阶段，对文本测量结果仅仅使用其大小信息用于整体布局，渲染阶段直接使用鸿蒙的文本控件进行渲染。

但我们通过性能分析发现，在一个List滚动场景中，文本的测量占了父容器（List）布局耗时超过50%。这显然不能令人满意，我们需要探索复用文本布局阶段的结果，避免在上屏时候二次布局。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdf6hIov6kDGzY5ghXCUreymMeTurwoqXIFbn8a6RKaVH39cFjEibPJiaQ/640?wx_fmt=png&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

## b）初步优化方案：部分复用布局产物

在鸿蒙API 12版本推出后，支持了新的文本渲染能力，使我们能够实现在对文本布局后，除了使用文本的大小信息外，也可以保留布局中间产物，这为我们文字渲染性能优化提供了新的思路。对此我们也进行了尝试和验证，发现性能有较大提升，但仍存在二次布局问题。

## c）最终方案：利用系统提供的文本绘制能力按需绘制

于是我们转向直接利用系统文本绘制接口进行直调绘制方案，在上一个方案基础上，更进一步复用了布局结果。通过分析可以确认，这个方案彻底解决了文本重布局问题。综合对比上面几个方案，上屏阶段的文本布局耗时：

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdg8THlEFib1tEAyXT7FvYjaY8iaCmy5HmCza9TM0PJBLXFicgkSIRWffnQ/640?wx_fmt=other&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

系统API按需绘制方案取得了预期的性能表现。同时，我们也补齐了accessibility、局部背景色、子串区域点击回调等支持，对齐系统文本控件能力。

#### 4、其他典型性能和稳定性问题

## a）高频节点创建和释放性能问题

在长列表或者其他极端场景下，高频的节点创建和释放方法调用耗时，在渲染流程中也是可观的开销，因此为了追求极致性能，我们引入了节点复用机制。

但在节点复用中，也遇到了节点属性无法重置、重置Crash等问题。为了尽量高的节点复用率和稳定性，我们设计了以下复用策略：在节点维度采取了白名单机制，把一些高频节点纳入可复用白名单；在节点属性维度采用黑名单机制，把一些无法重置的属性排除掉，不进入节点复用池。

## b）稳定性优化

使用CAPI方案后，在开发难度和复杂性方面都变得更高，适配过程中也遇到很多稳定性问题。这些问题的定位和解决成本都比较高。

比如早期版本XComponent在高频快速的创建和销毁场景下我们就遇到高概率Crash问题；还有些系统接口比较容易出现误用导致Crash，比如系统日志打印接口、节点ID的读取接口等。这些问题在在迭代过程中都得到了有效的解决。经过多个业务一年多的线上验证，Crash率维持在较低的水平，稳定性上已经得到充分保证。

### 五 KuiklyBase适配鸿蒙平台

为了支持Kuikly以及业务对鸿蒙的适配，KuiklyBase增加了对鸿蒙的支持，包括Kotlin Native、跨端组件以及各种基础设施都实现了对鸿蒙的支持。

#### 1、Kotlin Native鸿蒙适配

在适配的早期，我们快速尝试了Kotlin JS模式，发现它的性能是不太能满足我们的要求的：

1. 通过Kotlin官方benchmark对比可见Native性能是JS性能的950%，性能缺口较大，且无法依赖系统AOT来弥补；

2. 鸿蒙下使用Kotlin JS模式的限制较多，比如缺少多线程能力，无法做到真正的并发；JS逻辑需要运行在worker中，内存隔离对性能影响比较大；Kotlin转换到JS后数值运算效率差等。这些限制和问题在Kotlin Native模式中都不存在。

## 解决方案：

Kotlin Native适配到鸿蒙平台的核心工作有编译期和运行时两部分，以对象的创建为例子帮助加深对这两部分的理解：

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdaPfW1F7iaBZcLtbPZR16ibQicX1W1uFGXHHl2d24ImKgeqzicHhTBkYT6A/640?wx_fmt=other&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

## 编译期的适配

Kotlin 1.9.x 使用的LLVM 11，Kotlin 2.1升级到LLVM 16，但是鸿蒙平台能够支持的版本在LLVM 12 ~ 15，苹果和鸿蒙都是基于公共版本的LLVM进行修改，增加了自己的特性优化。苹果相对好的点在于公共版本的LLVM中包含有苹果的target，所以鸿蒙版本的LLVM既可以支持iOS，又可以支持鸿蒙平台。

- 常规方案：常规的Kotlin适配思路是分别使用鸿蒙和苹果的LLVM进行编译，这种方案的好处是修改简单，且不存在兼容性问题。缺点是由于Kotlin本身不支持多LLVM架构，导致鸿蒙平台的Kotlin和iOS平台要进行分别编译，需要依赖不同的Kotlin版本。

- KuiklyBase方案：在第一步Kotlin IR转LLVM IR时采用苹果的LLVM 11，在LLVM IR生成可执行文件时使用鸿蒙的LLVM 12，这样既可以满足诉求，Kotlin本身也无需进行架构调整。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdHWqnfZqAnpa72WejsYCOmLgQdMJib9EMbq1hsicZrZMo7wgaZ5v2uEZg/640?wx_fmt=other&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

## 运行时的适配和优化

为Kotlin Native增加鸿蒙平台的互操作文件，对接系统API，同时调整运行时中涉及到架构、平台的判断等逻辑，使其实现对鸿蒙平台的支持。

完成初步适配后，通过Kotlin官方的Benchmark进行评测对比，发现鸿蒙上的耗时是同等性能的iOS设备上2.48倍。为此，我们针对鸿蒙平台进行一系列的优化，包括内联优化、ThreadLocal优化、协程性能优化等。优化后，鸿蒙Kotlin Native性能有了倍数级别的提升，满足了产品落地的要求。另外，关于这部分的的适配和优化未来也会通过另一篇文章进行更详细介绍。

#### 2、调试效率优化：通过插件让鸿蒙IDE支持Kotlin的调试

在鸿蒙IDE中使用Kotlin Native后，有一个很大的痛点是调试问题，通过使用Kotlin官方LLDB插件可以实现调试，但它的变量查看效率很低，基本不可用状态。

为了提高效率使其变得可用，我们通过对Kotlin的调试插件以及配套运行时进行了优化，增加了调试信息获取的复用与缓存，以及调用合并，信息预加载等优化，实现了40倍的速度提升，使插件从基本不可用状态变得基本可满足日常开发调试需求。

在调试变得可用后，进一步遇到的问题是调试能力的使用门槛高，需要给鸿蒙IDE进行一系列的手工设置，打断点也需要先在文件系统找到源码文件并拖拽到鸿蒙IDE打开后才能进行断点设置，或者通过命令行设置（这种方式门槛更高）。

于是我们为鸿蒙IDE开发了一个用于自动化调试流程的插件，自动给IDE进行调试相关设置并关联产物代码，用户只需要通过切换到Kuikly面板，就能看到项目中的SO产物，以及它所关联的文件，可以在这个面板双击打开文件，无需另外手工拖进鸿蒙IDE，极大提高了日常调试的效率。未来待这个工具在内部得到更广泛验证后，我们将会将其发布到鸿蒙IDE的插件市场，回馈社区。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdVNyvJHEMTsHsE1lN64d3Cfv32ibsSadBjw1icneP9dMtQbKcrCBPbStg/640?wx_fmt=other&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)

### 六 ComposeDSL Beta版本发布

基于 Kuikly 的核心架构与通用渲染层，我们进一步拓展了对标准 Compose DSL 的支持，目前 Beta 版本已发布。Kuikly对Compose DSL的支持，可以进一步降低客户端开发上手成本，其核心特点是：

**1.更多平台支持：**通过复用 Kuikly 的通用渲染层，Kuikly Compose 能够在支持标准 Compose DSL 语法的同时，无缝覆盖主流平台，包括 Android、iOS、鸿蒙、Web，以及国内常见的小程序平台，极大提升了应用的可达性。

**2.动态化能力：**在 Kuikly 跨端框架层基础上扩展对 Compose DSL 的支持，使 Kuikly Compose 天然具备了 Kuikly 现有的动态化能力，包括热更新、动态下发等特性。

**3.原生体验：**不同于官方 Compose 的自渲染方式，Kuikly Compose 保留了 Kuikly 的原生渲染优势，确保在各个平台上实现高性能、原生级的 UI 体验。

关于ComposeDSL支持详情，可查阅[官方文档](https://kuikly.tds.qq.com/ComposeDSL/overview.html)

#### 与ovCompose的差异

两个框架在渲染方式、动态化支持、性能表现与多端适配层面各具优势。为满足业务场景的差异化需求，腾讯大前端Oteam同时进行两个方案探索。

- 原生渲染方案 KuiklyUI ：侧重于静态化+动态化双运行模式，采用轻量原生渲染保持原生UI体验并具备高度一致性，并基于原生组件映射的方式支持Compose API，支持H5和小程序（6月底推出）。

- 自渲染方案 ovCompose：专注于全面对齐 Compose Multiplatform 标准API，采用自渲染方式实现鸿蒙平台的适配，确保三端高度一致性。针对 iOS 上较多的存量业务，提出了多模态渲染方案解决UI混排问题。

### 七 技术展望

1.鸿蒙系统快速演进中，我们将继续保持关注，始终保持对鸿蒙的良好适配

2.当前Kotlin Native的GC算法仍有着不小的改进空间，后续还会追赶对齐Java的GC性能

3.当前仍然主要以鸿蒙IDE来进行主要的调试，后续考虑扩展Android Studio的KMP插件，或者为Android Studio增加一个支持鸿蒙的插件，对齐Android、iOS在Android Studio上的开发调试体验。

期待社区的优秀开发者能一起参与进来，共同打造一套：一码多端、极致易用、动态灵活的全平台高性能开发框架。

🚀 立即体验 Kuikly，加入开源社区。

👉 [GitHub 仓库](https://github.com/Tencent-TDS/KuiklyUI) | 📚 [官方文档](https://kuikly.tds.qq.com/%E7%AE%80%E4%BB%8B/arch.html)

欢迎关注

Kuikly框架属于腾讯端服务联盟成员，欢迎关注及了解更多信息：

腾讯端服务官网：

[https://tds.qq.com/](https://tds.qq.com/)

TDS Framework官网：

[https://framework.tds.qq.com/](https://framework.tds.qq.com/)

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasnhWPhxbgksvficRlCFMObdArSemgz1jy1e4P3rMTicDSbhBXcISZXK6bFicpB1uEcnibseMeghibHQWw/640?wx_fmt=other&from=appmsg&wxfrom=5&wx_lazy=1&tp=webp)
