# 适配鸿蒙操作系统！！腾讯游戏直播SDK基于Kuikly三端一码开发实践

作者：Kuikly

发布时间：2025-07-28

原文链接：https://mp.weixin.qq.com/s/joIAZr8_28UnR7XhPGZ62Q

随着鸿蒙Next的发布，越来越多的APP开始推进鸿蒙化适配，产品与技术团队也启动了游戏电竞直播SDK的鸿蒙适配规划。投入足够资源通常能达成适配目标，但仅实现技术目标的意义相对单薄 —— 如何进一步升华价值、让鸿蒙适配效益最大化？本文将介绍基于Kuikly框架的鸿蒙跨端适配方案，实现SDK业务代码100%跨三端跨多游戏App复用，相比各端独立开发，可节省 50% 以上人力，显著提升开发效率。

其中Kuikly是腾讯广泛使用的跨端开发框架，提供了使用Kotlin语言开发Android、iOS、鸿蒙、Web、小程序跨端应用能力，也是腾讯端服务联盟的重要成员。

### 适配工作

#### 现状

游戏电竞直播SDK是一个有着很长历史的电竞赛事直播基础组件，在CODM，QQ飞车，极品飞车，元梦之星，AOV等游戏APP中都有使用。游戏电竞直播SDK在这些游戏APP上的产品形态非常相似。为提升跨多端、跨多APP的研发效率，我们也在力争做到同一份代码尽可能复用到不同游戏APP中。

#### 新增工作

预计新增鸿蒙Next平台后引入的工作量增量来源占比

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHzicibibSP7oxut5MUIZAyvt60yh3jc4E2nvEbALiccrKyXw8HUTz5Ll5yg7Q/640?wx_fmt=png&from=appmsg&randomid=dg9ip08z&tp=webp&wxfrom=5&wx_lazy=1)

预估适配工作导致的人力开销增幅

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7CXXo8ibGPEGJpictVPLCDf3jxPwKOEySibSy4pU2CsMwXTXHyibQ6gE8JXDvicm5yPToU5PicicK8dchP7g/640?wx_fmt=png&from=appmsg&randomid=8gpx5n8k&tp=webp&wxfrom=5&wx_lazy=1)

新增鸿蒙版本的开发，在适配阶段需要投入较大成本对齐安卓/iOS能力，同时后期也需要增加一套鸿蒙人力持续开发迭代。在此背景下，团队需要思考如何选择适配方案，以降低适配成本、甚至放大适配价值。

### 适配目标

从研发效率提升为出发点，团队设定了适配目标：既要低成本适配鸿蒙系统，又要让适配工作价值最大化。

团队主要通过技术方案升级和运营手段相辅达成目标。

#### 方案设计

游戏电竞直播SDK和Kuikly技术团队合作快两年，Android和iOS在多个项目(CODM、QQ飞车、极品飞车)已经基于Kuikly实现了业务跨端，跨业务线开发。针对鸿蒙系统，Kuikly同样是较早完成适配，并在浏览器、K歌等多业务完成验证打磨，因此这次鸿蒙适配也是基于这个司内优秀跨端框架作为基础搭建。

对Kuikly感兴趣的可以看下这两篇文章：

[腾讯开源跨平台开发框架 Kuikly，能显著提升多端开发效率](https://mp.weixin.qq.com/s?__biz=MzA3NTYzODYzMg==&mid=2653580946&idx=1&sn=2e0c6a69c24ea76b580587f524fe6c0f&scene=21#wechat_redirect)

[腾讯Kuikly框架鸿蒙版正式开源 —— 揭秘卓越性能适配之旅](https://mp.weixin.qq.com/s?__biz=MzA3NTYzODYzMg==&mid=2653581123&idx=1&sn=7155516e0e24c005e2ad6c29cd09511c&poc_token=HCzHgWijK-faB-EnmZu_-68x6UFtniC4w5OlzSC0&scene=21#wechat_redirect)

#### 补齐鸿蒙开发能力

对比Android，鸿蒙的三方文档相对较少。为了确保后面制定技术方案不会出现较大纰漏，还是稳扎稳打的抽时间学习了鸿蒙开发基础。ArkTs，自定义组件，声明式布局开发，多线程，多进程开发等基础能力都需要提前补齐。

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHzic4iaouJEmXfc4I2S6KhSKjOib8E8Vbrh3ojqvXb9gUOFso5hibDtIVeyibQ/640?wx_fmt=png&from=appmsg&randomid=z8yal0j7&tp=webp&wxfrom=5&wx_lazy=1)

如上图是鸿蒙入门的基础知识点，只需掌握基础的，更进阶的编程要点在开发过程中通过官方文档，元宝AI搜索基本都可解决。为验证学习效果，基于鸿蒙UI框架花费了约1d的时间简单搭建一个电竞直播SDK主体框架，目的是验证学习效果是否达标，并且基于这个做组内分享，让团队人员也能够基于熟悉的项目快速入门鸿蒙开发，方便后期对齐技术方案。

#### 方案对比

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHzicZEbYvtQBbju9wM35NBdDibEfFcQjAppyR4bsEoTiauzsGpsnBdgf0c6g/640?wx_fmt=png&from=appmsg&randomid=m1vvijys&tp=webp&wxfrom=5&wx_lazy=1)

如上图中三个分支节点代表三种实现方案。

![](https://mmecoa.qpic.cn/mmecoa_png/tnZGrhTk4deiaBClV6LNZdO1SY5CkhRO5wosEbMO5CeJpzvgSN7509Ku2tqUwBFqso49dd1wgdxu4NxNMraAB7Q/640?wx_fmt=png&from=appmsg)

游戏电竞直播SDK的Android，iOS已经深度使用Kuikly组件，稳定性，混合开发特性，性能，包体积等方面都有较好实测体验。

对比方案后选择“KuiklyUI实现业务，KuiklyBase实现逻辑组件”的方案更加合适。

#### 整体架构设计

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHzic7RhhbOeonkYAW34XRhlSIQ3GHWqpC8sDgbF4smHMLOC0CmTqP0hISw/640?wx_fmt=png&from=appmsg&randomid=6qo3woka&tp=webp&wxfrom=5&wx_lazy=1)

原生层的开发是避免不了的工作，这里我们也选择将通用逻辑做跨端处理，使用了KMP的开发范式。主体思想即“求大同，存小异”。

我们将“大同”的部分规划到用Kotlin开发，实现跨端调用。“小异”部份用特定的平台层编程语言开发。

#### 页面层级设计

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYicsZNF9KWGsWwQE6O6GgveNJ5PUwCF57OS6Nwh6icnVoRuXzRIT2NHpw/640?wx_fmt=png&from=appmsg&randomid=9nynfywn&wxfrom=5&wx_lazy=1&tp=webp)

如图中可见，最底层只有一个很”薄“的鸿蒙原生页面，这个页面是一个Entry装饰的page组件，不是Ability。

其他所有可见的页面都是Kuikly化。

#### Kuikly层业务框架设计

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYevYRvCz9ApEn2QwAzwicJXr7mkHLp5RsqN9GJ0DYW27yl66YLcl59eA/640?wx_fmt=png&from=appmsg&randomid=ua949x3m&wxfrom=5&wx_lazy=1&tp=webp)

如上图所示是电竞直播SDK业务层整体框架设计，其中页面路由很有特点，不是基于Kuikly的官方的路由组件，而是自己封装的基于KuiklyNativeView实现的页面路由，由于Kuikly很灵活，可以让开发人员把任意原生View按照Kuikly协议映射为Kuikly可以布局使用的KuiklyView，这里简称为KuiklyNativeView。

### 原生组件Kuikly化

目标是将游戏电竞直播业务实现100%跨端，必须将之前原生层的业务模块嵌入到Kuikly中。基于Kuikly的自定义组件接口协议，我们封装了一些必要组件到Kuikly使用。

#### 原生View映射为KuiklyNativeView

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYBdz4r9JVMQheh7606m5HNRYhljcomeedCm3ATCicbD6vLvaicnbyV3SQ/640?wx_fmt=png&from=appmsg&randomid=2qgvu2o8&wxfrom=5&wx_lazy=1&tp=webp)

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYEWyOBzcwaRKMwELqibRVtW1evyMJJXnPh5zvkVX9DjkMVL4hibicibxj5w/640?wx_fmt=png&from=appmsg&randomid=b2jvvt4j&wxfrom=5&wx_lazy=1&tp=webp)

如上是将一个KuiklyPage 嵌入到一个鸿蒙原生的View中，相当于业务方实现了以View为单位的最小粒度去使用KuiklyPage，并且还按照KuiklyPage规范，封装成了一个KuiklyNativeView，让KuiklyPage里面可以套娃方式嵌套使用KuiklyPage。效果类似Android的Frament组件。这种使用方式是Kuikly官方没有支持的特性，在Android上已经通过Fragment实现同等效果，积累了经验并且验证了稳定性，在鸿蒙上直接再次复刻了能力。这个特性让电竞直播SDK主页面可以自由嵌套多个任意大小，位置，层级的KuiklyPage完成需求开发，也为后续说的需求特性模块化共享留下技术支撑。

#### 原生全屏透明弹窗映射为Kuikly页面容器

电竞直播SDK大厅业务大部分需要撑满全屏，还有一种非全屏特殊场景，即在游戏的Unity页面上以非全屏浮层方式呈现活动页面。类似那种广告弹窗等。由于鸿蒙上实现透明页面需要Ablity层级的组件，但是这种组件对于游戏电竞直播SDK而言太“重”了，因此采用了弹窗方式。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYxN6MOZMibTF9xr2Y6uwK0aMR7DwlJdLn9iaWacryZmibictbicLRN69kMvg/640?wx_fmt=png&from=appmsg&randomid=bzvkw6gi&wxfrom=5&wx_lazy=1&tp=webp)

如上图展示的层级关系，用鸿蒙原生自定义全屏透明弹窗作为容器承载KuiklyNativeView，如此就可以让Kuiky业务自己决定显示哪些内容到游戏画面上，并且可以控制透明区域的大小位置。

#### 播放器组件Kuikly化

游戏电竞直播SDK核心玩法之一是游戏赛事直播，播放器会被重度使用。在Android和iOS老项目上，我们采用了混合开发，播放页面依然使用原生容器Activity/ViewController实现，然后再在上面安装Kuikly的插槽。这种方案缺陷很多，例如原生和Kuikly之间的事件，方法双向调用很繁琐，需要做很多胶水层代码，桥接层很重。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYPROiag5OmzQdXRqRe69okhhX8sufVicXqCicx4zYvVauuqqdxcKC4fbXw/640?wx_fmt=png&from=appmsg&randomid=ay67funh&wxfrom=5&wx_lazy=1&tp=webp)

本次鸿蒙适配过程中，直接反向思维，将必要的原生组件嵌入到Kuikly页面中。如上图所示，我们参照KuiklyNativeView的实现思路，将播放器所在的View直接封装到Kuikly中，实现一个KuiklyPlayerView。这样播放器的生命周期，层级，大小，位置都交由宿主KuiklyPager管理。

#### 弹幕&WebView组件Kuikly化

弹幕也是重要的运营工具，为了方便业务闭环运营，参照播放器一样封装映射为Kuikly组件使用。

### 需求特性模块化共享

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHzic8aRwn4yYu0HC49INEE4oyYVKrahoBGCEdibM8rRtTzMdGSvcNttUcjA/640?wx_fmt=png&from=appmsg&randomid=1e8dpalf&tp=webp&wxfrom=5&wx_lazy=1)

如上图所以，游戏电竞直播SDK是一个强运营特性的SDK，经常出现A游戏实验不错的运营玩法会铺开到其他其他游戏中使用。这个时候玩法特性按需放到不同游戏的游戏电竞直播SDK中就很有必要了。如下介绍特性跨产品线配置化实现方式

#### 传统实现方式

![图A 图B](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYQ3c4nHggjb9bhKCQctLI57IFa9PJLQL3C5bcnvqjf05dS16v2IGIhg/640?wx_fmt=png&from=appmsg&randomid=l78gr6oc&wxfrom=5&wx_lazy=1&tp=webp)

图A 图B

如上图A所示，当ABCD四个特性代码都寄存在同一个Activity时候，第一个研发人员可能会保持各个特性实现尽量做到低耦合，但是代码经手多人后，这种“弱耦合”的代码最终会演变为“强耦合”，甚至成为一团乱麻的债务。

#### 弱耦合实现方式

如图B所示，宿主页面是一个独立全屏的KuiklyPager，然后ABCD四个特性UI界面也各自是独立的KuiklyPager，由前文提到的KuiklyNativeView能力，使得KuiklyPager可以套娃使用KuiklyPager。

KuiklyPager有一个特点，即两个KuiklyPager之间的数据共享成本很高，无法直接通过静态变量传递数据，官方手段是通过启动参数共享数据，或者通过事件总线共享数据和事件。并且两个KuiklyPager之间无法通过持有对方的引用调用接口。这种高门槛的数据共享，禁止对象互相持有的特性，刚好天然实现了KuikyPager之间的弱耦合特性。研发人员想制造毛球代码都很难。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYPuib3Xnojo52u7JEGvotVXxvSDicduQDMMBfqramSR8YicqfZt1ENNQNA/640?wx_fmt=png&from=appmsg&randomid=lae3wxwn&wxfrom=5&wx_lazy=1&tp=webp)

如上是Kuikly官方文档提到的Pager的生命周期，由于每个特性独占一个KuiklyPager，需求特性所需要的基本要素都满足了，自然很少会去耦合其他模块的代码。且其轻量化的底层设计也使得资源额外开销可以忽略。

既然ABCD之间和耦合很弱，那么之间的交互关系怎么完成呢？用到了业界很成熟的方案，即Scema跳转。电竞直播SDK的Schema路由跳转经历了1.0版本的初步引入使用，展示了其友好的运营可配置化特性，2.0版本进一步加强了Schema配置管理端的建设，降低了Schema暴增后的管理维护难度。

### 迭代规范化

很多事遵从二八法则，通过投入少量精力制定规范化的运营迭代流程，就可以解决很多技术上需要投入巨大资源才能做到的事情。很多历史债务是由于迭代期间逐步积累的，我们已经实现了Kuikly业务的弱耦合开发规范，接下来看下更多规范制定。

#### 基建能力原子化开发

Kuikly框架是一个跨平台UI框架，一些系统接口能力还是需要原生提供桥接层代码实现。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYOu2mu2ZKnXxicdrEmHGIbdcSurWZxeO19QSwz1bkh1iaz0PslAVXSdAg/640?wx_fmt=png&from=appmsg&randomid=rjbdx127&wxfrom=5&wx_lazy=1&tp=webp)

如上图是遵从惯性思维实现原生层的API给Kuikly业务层使用，提前写了一堆if-else代码，导致原生层很“厚重”，绑定业务的原生API也很难复用到其他业务特性上。

![图片](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvauuEr2pwP4DH8OOXmicu92eYm0uwZtfzVwXibSNmwgpcrnmicABf4zZ3jaVKOchByFyeNFawml61Uibcg/640?wx_fmt=png&from=appmsg&randomid=smuhinkn&wxfrom=5&wx_lazy=1&tp=webp)

优化的设计如上图，原生尽可能提供必要的原子接口，让原生层做得更“轻薄”。

#### 业务Kuikly规范化

基于前面提到的种种特性，业务尽可能提升到Kuikly层实现，原生只提供必要的原子接口即可。再配合Schema配置管理端，流水线发布规范，需求评审流程规范等。这套流程已经在游戏电竞直播SDK上稳定运行1年多，鸿蒙项目继续复刻这套研发流程。

### 初步效果

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHziceQnIkjXxBs3YzoXVH212gUTIMBMktt6HT91OQ1mmiaoLO1wGHWAYNAw/640?wx_fmt=png&from=appmsg&randomid=b9kvc88y&tp=webp&wxfrom=5&wx_lazy=1)

本次鸿蒙平台适配，所有业务代码通过Kuikly跨端为基础，补齐基建能力，流程规范，进而实现了业务代码100%跨三端、跨多App共享。**得益于Kuikly框架对鸿蒙平台适配的功能完善度、高性能、动态化和稳定性等各维度优异表现，也帮助我们快速完成初版适配，并在性能和产品体验上达到了预期效果。**

鸿蒙适配不仅为鸿蒙，更是为了其他项目更加稳定高效运行做的努力。

鸿蒙端CODM电台大厅

![图片](https://mmbiz.qpic.cn/mmbiz_jpg/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHzicWKxXzTlC8H0m0oFtn5xDMuF28MCjGYjZ3g1nfprGNIldBam8GoF4eQ/640?wx_fmt=other&from=appmsg&randomid=jovwysx3&tp=webp&wxfrom=5&wx_lazy=1)

Android端CODM电台大厅

![图片](https://mmbiz.qpic.cn/mmbiz_jpg/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHzicoMmnZrUwpqDic1RkTsl9Hz12pNFUdySH2V2n7taZk7DHw5oLoFE0aZw/640?wx_fmt=other&from=appmsg&randomid=w0n6whpc&tp=webp&wxfrom=5&wx_lazy=1)

iOS端CODM电台大厅

![图片](https://mmbiz.qpic.cn/mmbiz_jpg/ibrUhU3BKx7AhXrHWsibJGdA3U71wbwHzic2bduaYZMT2nbyVQT69fFV3c0v2V4vy20fImT3ANibl1Sazt7NZhnuDg/640?wx_fmt=other&from=appmsg&randomid=j3gxocvk&tp=webp&wxfrom=5&wx_lazy=1)

## 关于Kuikly

当前Kuikly已经开源，有兴趣和有需要的产品，可以通过以下方式了解详情或接入体验：

- 进入腾讯端服务联盟（[tds.qq.com](https://tds.qq.com/)）主页访问Kuikly官网（[framework.tds.qq.com](https://framework.tds.qq.com/)）。

- 通过以下Kuikly文档和仓库，快速体验：

[官方文档](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical3)

[Github仓库](https://github.com/Tencent-TDS/KuiklyUI)

- 关注和联系他们

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7CXXo8ibGPEGJpictVPLCDf3jN8EjP2hUEJkt4aLZU5CAZ41mTqvdoOTkXvwSkSzTZcKGfL8GsslQhQ/640?wx_fmt=png&from=appmsg&randomid=mus016ue&tp=webp&wxfrom=5&wx_lazy=1)
