# 腾讯Kuikly框架进一步开源，新增支持Web，开启一码五端新体验！

作者：腾讯程序员

发布时间：2025-08-20

原文链接：https://mp.weixin.qq.com/s/Ylo97APs2LhorlB7hTNwUw

![](https://mmbiz.qpic.cn/sz_mmbiz_gif/j3gficicyOvasVeMDmWoZ2zyN8iaSc6XWYj79H3xfgvsqK9TDxOBlcUa6W0EE5KBdxacd2Ql6QBmuhBJKIUS4PSZQ/640?wx_fmt=gif&from=appmsg)

文/QQ游戏中心谢易成

### 一、背景

Kuikly是腾讯广泛应用的跨端开发框架，基于Kotlin Multiplatform技术构建，为开发者提供了技术栈更统一的跨端开发体验，由腾讯大前端领域 Oteam（公司级）推出。目前已有20+业务深度使用，页面数1000+，日活用户超5亿，满足了这些业务在众多场景下的各类复杂需求（[应用场景案例](https://kuikly.tds.qq.com/Introduction/application_cases.html?utm_source=artical4)）。Kuikly 作为腾讯端服务联盟(tds.qq.com)的重要成员，将持续推动跨端开发的技术创新和生态建设。

本次在Android、iOS、鸿蒙开源基础上，将新增开源Web版，支持**H5**和**微信小程序**，进一步扩展多端适配场景。Kuikly适配的H5和微信小程序已接入腾讯多款业务，如搜狗输入法、鹅毛市集、QQ小游戏等。

### 二、效果展示

Kuikly Web版在H5和微信小程序上已经实现了绝大多数核心组件能力，运行效果如下：

![](https://mmecoa.qpic.cn/mmecoa_gif/tnZGrhTk4ddQjFApmV3g76BEQaIAAIhcQEcgRyiceQFTAicxAPrXiaIjYY2oudPicbru9cia2iazF1olEibqHJbtMxNqw/640?wx_fmt=gif&from=appmsg)

### 三、主流方案对比

Kuikly是基于客户端技术栈设计，在支持Android、iOS、鸿蒙高性能跨端的基础上，拓展支持H5和小程序，以达到更多端的复用。这与一些业界跨端框架定位是类似的，如 Flutter、Compose Multiplatform 等。

我们从其中挑选了两个框架，从多个维度与它们对比在H5与微信小程序场景下的差异。

#### 1 产物大小

在H5平台上，三个框架编译产物大小差别很大，Kuikly包体积优势明显。

- 业界基于终端技术栈的跨端方案，都是通过自绘引擎，通过 WASM 技术运行在浏览器上，编译后产物体积很大。

- Kuikly Web使用DOM渲染方案，不依赖第三方产物，产物远小于其他框架，只有463KB。

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvaupA5G4yLjr4iceV0ALdpbuNcZ6dSm1HSJ67mmDaAdxmqGzv6rXcdQAGHRnSkylO4EepTUAla7tHoQ/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

#### 2 页面加载速度

我们在iOS，Android和PC浏览器环境进行性能测试(运行Hello World Demo)，Kuikly在三个浏览器环境下加载速度都是最快的。

## 2.1 iOS加载速度对比

![](https://mmecoa.qpic.cn/mmecoa_gif/tnZGrhTk4ddQjFApmV3g76BEQaIAAIhcHGW7IeW9I4ndIDs3e6n3q3n6cXCI6OqYYmXRbicRJXtS6pyEuZCWZQg/640?wx_fmt=gif&from=appmsg)

## 2.2 Android加载速度对比

![](https://mmecoa.qpic.cn/mmecoa_gif/tnZGrhTk4ddQjFApmV3g76BEQaIAAIhcriaNs8kbWOTakvUUQgfyHTflgzfRBO3ib64Ab8lgg8QpNxsBicUkwDBTg/640?wx_fmt=gif&from=appmsg)

## 2.3 PC 性能数据对比

在MacBook Pro M4Pro 电脑的Chrome浏览器(138.0.7204.158)上，我们使用开发者工具上进行了详细的性能测试。测出Kuikly的FCP耗时仅为87.76ms，不到其他框架的一半。

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvaupA5G4yLjr4iceV0ALdpbuNI7OHbnWY21hWSzc9y7lvQroCNNo2ezTvUCveHrZVW9d3FS8eaZpiaCw/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

#### 3 其他优势

在H5平台上与主流跨端框架对比，Kuikly还具有以下优势：

1. 开发体验: Android Studio 完善的开发支持。

2. 代码调试: 可直接调试JS或通过SourceMap调试Kotlin。

3. SEO友好: 采用DOM渲染，传统的SEO优化都可以生效。

4. 兼容性好: 仅依赖ES6和CSS3特性，大部分设备都支持。

5. 生态复用: 编译产物是JS，采用DOM渲染方案，可通过Kuikly自定义扩展复用React等H5生态库。

#### 4 微信小程序支持

主流的基于终端技术栈的跨端框架，缺少官方微信小程序运行方案支持，**Kuikly Web版微信小程序**的出现填补了这部分空白。

### 四、整体技术方案

#### 1.Kuikly的架构设计回顾

简单回归一下Kuikly的整体架构，跨端Core层处理框架核心逻辑，Render层负责不同平台渲染。新平台接入Kuikly需要实现自己的Render层。

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvaupA5G4yLjr4iceV0ALdpbuNEuzoaPC4eazPtd96JY9j2cw687y5pGq8AIBwlzviaAyiakWUw8xzibnjw/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

#### 2.Kuikly Web版本整体方案设计

在进行Kuikly Web版H5和微信小程序适配工作时，我们发现许多代码可以共用，因此我们抽象了一个**Web容器运行时**作为适配层，这个适配层依赖抽象的**DOM API**、**KuiklyWindow**、**KuiklyDocument**，实现了绝大部分Render逻辑。

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvaupA5G4yLjr4iceV0ALdpbuNxZGmjiau9Mibc1NiaGmR1BaUQY3yfYkYoGRPE2RKV4VTo9bWdHxp5kWtg/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

## 2.1 Web容器运行时

通过抽象核心接口构建Web容器运行时，我们实现了以下能力：

- 将Kuikly的UI操作转换为标准DOM操作

- 为差异化模块（动画/列表/文本测量等）提供扩展接口

- 支持JS宿主通过实现Web容器运行时接口，接入Kuikly

## 2.2 H5运行时

浏览器提供了标准的DOM，Window，Document。Kuikly适配H5时只需实现动画，滚动列表，文本测量等少部分 Web容器运行时拓展接口。

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvaupA5G4yLjr4iceV0ALdpbuNiaYuUItBlnNoS7FL4IiaPXfh5mt1GRT58dnS460XGZzHibG6j3ZClNEsg/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

## 2.3 微信小程序运行时

在适配微信小程序之前，我们调研了目前支持微信小程序的跨端框架。这些框架基本都是基于前端技术，在微信小程序上基本采用编译时或者运行时方案，最终都是数据驱动模板完成UI渲染。如下图：

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvaupA5G4yLjr4iceV0ALdpbuN7emFL8GV7EQYH8817PB7npmLHW0mrHDWrvxd2bO52hicfdyJbWibapkQ/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

我们借鉴了业界主流小程序框架Tarojs和Kbone的思路，结合Kuikly框架的特点，通过实现Web容器运行时接口，提供轻量级DOM和拓展接口实现，仅实现Kuikly需要的能力，并做了一系列针对Kuikly渲染流程的优化。如下图：

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvaupA5G4yLjr4iceV0ALdpbuNSYdQJXresicPBNnYtrnZnic8ia4ZEavicHzmthl4oicIlcftuSIicFpXU0Eg/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

目前Kuikly适配微信小程序的方案在性能上仍有不少优化空间，后续我们会探索编译Kuikly产物为WASM, 使用预编译等方式优化Kuikly在微信小程序平台的体验。

### 五、技术展望

1.继续对Kuikly Web版进行性能优化，使用预编译进一步提升小程序性能，同时减少编译产物大小。

2.探索使用WASM提升计算密集型任务的执行效率，优化Kuikly Web版的使用体验

3.扩大Kuikly Web版支持范围，下半年将开源Electron环境的适配

🚀 立即体验 Kuikly，加入开源社区。

👉 [Github 仓库](https://github.com/Tencent-TDS/KuiklyUI) | 📚 [https://kuikly.tds.qq.com](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical4)

Kuikly框架属于腾讯端服务联盟重要成员，欢迎关注及了解更多信息：

腾讯端服务官网：

[https://tds.qq.com/](https://tds.qq.com/)

TDS Framework官网：

[https://framework.tds.qq.com/](https://framework.tds.qq.com/)

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvaupA5G4yLjr4iceV0ALdpbuNNkEoTpH5icTKcWsYSbahT1I82bNnnf3Gq4dowt3Um3T8H39hZVST9tg/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=5&wx_lazy=1)

![](https://mmbiz.qpic.cn/sz_mmbiz_gif/j3gficicyOvasVeMDmWoZ2zyN8iaSc6XWYjZ7Hx6Udjjk2BGLzC9ahJq7ibxDd1RGA0c9NYZc1husEsvb3tY4FcWPQ/640?wx_fmt=gif&from=appmsg)

![](https://mmbiz.qpic.cn/sz_mmbiz_png/j3gficicyOvasVeMDmWoZ2zyN8iaSc6XWYj5q5PQEOc5ibURPb03vnRibrxC3UR8xzdyATfiawTYRV2vJvBnAIcE1FeQ/640?wx_fmt=png&from=appmsg)
