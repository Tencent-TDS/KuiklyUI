# OpenClaw给我发来了它写的第一个Kuikly apk

作者：Kuikly

发布时间：2026-04-13

原文链接：https://mp.weixin.qq.com/s/ntyst7R7wxn4aPT4hD376g

导语：给龙虾热再添一把火，是时候尝试让小龙虾自主开发App了

## 这些App，都是腾讯云上的OpenClaw用Kuikly框架自己写的。

![](https://mmecoa.qpic.cn/sz_mmecoa_jpg/2YTOibpx9RUDxlz1icXmmSI4I80UrBG2YtGEEpWQUHGaO3uiccoKpht3OBoQjCY3EUialM1Q948FibCutrrqNDOLibzyMWoDyibHS7BabxiaB6lzNOo/640?wx_fmt=jpeg&from=appmsg)

![](https://mmecoa.qpic.cn/sz_mmecoa_jpg/2YTOibpx9RUDPPKjtYg83cIQF77UQubciaavumnGmHtvK1wia3VUBibibeNyibC0soFHicCYXoEickvd3RB4AibdHcwRraT6Iwz0sLlA5uZHvRG6YWTA/640?wx_fmt=jpeg&from=appmsg)

### 一、为什么要做这个？

**作为一名多年的客户端开发，看着电脑上同时打开的Xcode、Android Studio、DevEco-Studio，总觉得有一种说不出的落伍感。虽然各家IDE都在卷AI辅助能力，但相比于一个CodeBuddy就能搞定的前端和后台场景来说，客户端开发仍然太像"古法编程"（虽然可能也不完全是个坏事...）。**

2026年的趋势已经无比清晰——未来的软件、框架、工具，甚至是服务，一定不仅是给人用的，更多的是面向AI Agent。从这个角度回头看客户端领域，问题就很明显了：从工程创建到编译调试，整条链路都不够CLI化、不够结构化，没法像前端npm生态那样被AI轻松打通。

拿Kuikly来说，技术能力可谓业界领先——真正的原生渲染加KMP，一套代码跑Android、iOS、鸿蒙等6端，还支持动态化。但以前严重依赖Android Studio插件来创建项目，尽管Kuikly 在IDE场景已经提供了丰富的工具，取得了不错的效果，但整个流程锁在 GUI 交互里，AI Agent 无法通过标准接口直接调用。

作为腾讯内部深度使用Kuikly的业务方，一直在使用Kuikly团队提供的Skills等AI基建，所以我决定自己上手实践一下。

## 二、背后的原理

一句话来说，就是给了龙虾一个创建App的SKILL——**「Kuikly App Builder」**。

![](https://mmecoa.qpic.cn/sz_mmecoa_jpg/2YTOibpx9RUBDIHbUGOVCGRkUsiaIEQmib3ZHrRe19M3MMl7L2eLXqpaCsvOTl4OyWOBnXXdh4WK5ZibP7ib2Mh3BW7iauS4FMdo37D2b2ImJ7QxM/640?wx_fmt=jpeg&from=appmsg)

这个SKILL里，我们主要干了三件事。

**第一，把项目创建、配置和构建过程完全CLI化。**以前Kuikly只提供IDE插件来生成模版项目，现在一条命令就能创建完整的Kuikly跨端工程——shared模块、Android/iOS/鸿蒙/H5等宿主App，一步到位。

**第二，让所有命令输出结构化JSON。**编译失败时，错误不再是几百行Gradle日志，而是被解析成精确到文件、行号和修复建议的结构化数据。AI拿到这些，可以直接定位、修改、重编译——自动修复闭环，不需要人介入。

![](https://mmecoa.qpic.cn/mmecoa_jpg/2YTOibpx9RUCBmTFRib5iaI1FhyLxT7Z1C3txPYw7UvAyOR0pBicVjWlJqxjCGnPs7XWOPk9VAIHsRxruna8BKZyBOMOIGna4hrzvDuMW7dsRaY/640?wx_fmt=jpeg&from=appmsg)

**第三，也是最重要的，把Kuikly的编程知识"喂"给AI。**我们把组件API、常见错误、版本兼容信息整理成Skills文件，塞进AI的工作记忆。其中有一条特别值得一提的规则——"禁止凭记忆写代码"。AI极容易拿React或Flutter的经验来猜Kuikly的API，猜错了就编不过。所以SKILL强制它先查官方文档和示例，查完再动手写。具体指令如下图所示：

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBFpIQqzwVRzp87FbQGra0YrgvDhvzHPU1o4L4gFRSeDDardrDwOiad8OibW8ShVy7mNq0FVJd93Gic5icFB6tVrgZkrOJwtiaI6t1g/640?wx_fmt=png&from=appmsg)

上述用到的 Kuikly-AI Skills，正是腾讯官方最近开源的Kuikly AI技能库，地址是「[https://github.com/Tencent-TDS/KuiklyUI-AI](https://github.com/Tencent-TDS/KuiklyUI-AI)」，也可以访问《[Kuikly AI编程指南](https://kuikly.tds.qq.com/AI/?utm_source=artical12)》获取到更详细的资料和规划。

## 效果怎么样？

从开头的截图相信大家也能有所感受，其实很多情况下已经**超出了我的预期**。它不仅可以自行完成页面搭建，甚至在逻辑以及网络请求等方面也能妥善处理。比如开篇图中的股票市场App，它并非一个静态Demo，而是一个有真实网络请求的跨平台应用，龙虾自动找到了免费可用的服务，我在过程中所做的仅仅是给它提供了一个真实可用的API Key。

同时，再生成apk后，我们可以再结合腾讯司内的云真机平台，如下图所示，实现龙虾自动部署，云端运行，在线体验：

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUDf94W2QJWshUaAmRy2w6cxfqEJYgQwAre4BpQicej6VSgS8xfuJEhfKyKsJIeicz1TquJ9OEWbWxL43icyGFWBRf0OPeCmz2icUS8/640?wx_fmt=png&from=appmsg)

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBklGzA8q50rgjIVw38I2bVgoBeB4nQwCnrda6UicCq6pIE1fb2NOl2Y9hONfkScpQbOq9mTvW40fyvulBm0xObNRTzyibib2WG9Y/640?wx_fmt=png&from=appmsg)

不过，整个流程目前还有两个明显的不足：

一方面有时会受限于龙虾的云端运行环境，没法编译iOS，Kuikly一码多端的能力没完全发挥出来。

其次，从软件工程角度看仅实现了最小闭环，现在更像是放养龙虾自由发挥，缺少从需求到验收的完整控制。这也正是当下 Harness engineering 所做的事情，龙虾开发Kuikly，同样需要更多的harness探索。

## 三、一点思考

最后，这次实践最大的收获，其实不是龙虾做出了几个Kuikly APK。而是验证了两点：

**其一，如果你的工具或框架不能被AI Agent调用，你可能正在失去一半未来的用户。**

把工具CLI化、输出结构化、知识文档化——这三步不只适用于Kuikly，对公司内任何框架和工具都一样。

## 其二，回到龙虾本身，我们可能远远还未充分发掘其潜在可能。

尽管目前生成的APK更多还是原型和Demo级别，但路径已经通了。后续持续给它叠加更丰富的技能——bug调试、性能分析、开发范式管理——这只小龙虾的开发能力会不断成长。相信用不了多久，它便可以达到甚至超越一个"跨平台App开发专家"的水平。

## 四、欢迎体验

「**Kuikly App Builder**」已上架[ClawHub](https://clawhub.ai/)，直接搜索「Kuikly」关键词即可找到，欢迎转发给你的龙虾，让它也来试试～

## 具体操作办法，就两步：

先访问[ClawHub](https://clawhub.ai/)或[SkillHub-专为中国用户优化的Skills社区](https://skillhub.cn/)技能市场搜索Kuikly，直接安装Skill，（或将其下载到本地导入你的WorkBuddy等Claw Agent）；

然后直接给你的小龙虾说：

"使用 kuikly-app-builder skill 实现一个打卡App，可以实现打卡记录的功能"

"使用 kuikly-app-builder skill 帮我写个漂亮的计算器App"

"使用 kuikly-app-builder skill 帮我写股票App" ...

友情提醒：

* 当前工具仍为Beta版本，由于测试场景覆盖有限，可能还会遇到一些bad case，欢迎给我们反馈；

* 尽量选用coding能力较强的模型，有时模型不会主动发文件或截图，可能需要多聊几次；

* 由于开发App所需的工具较多，首次体验时需要安装一些必要工具，可以让龙虾自主完成，如下图所示：

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUC87MqqaGibTV8bul3CuCFMqEKyufNdeCEruqApO8H2w5BiaXlGR4XyNws8WUNia8FeZj09zUmPe2RlUnqCmu9IHW9jJibc7PytliaQ/640?wx_fmt=png&from=appmsg)

最后，欢迎评论一起交流，分享你用小龙虾+Kuikly做出了什么有趣的App~

## 五、关于Kuikly

当前Kuikly已经开源，有兴趣和有需要的产品，可以通过以下方式访问 Kuikly 仓库和文档，欢迎Star、Watch与体验：

👉 [Github 仓库](https://github.com/Tencent-TDS/KuiklyUI) | [官方文档](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical12)

Kuikly框架属于腾讯端服务联盟（tds.qq.com）的重要成员，欢迎关注及了解更多信息：

腾讯端服务官网：[https://tds.qq.com/](https://tds.qq.com/?utm_source=artical12)

TDS Framework官网： [https://framework.tds.qq.com/](https://framework.tds.qq.com/?utm_source=artical12)

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBDF0cBrsOgKaMV6KXicFuoYU37JrzA9jvicQ7RLvcWObM9dyzibI8ulTob8Co8rVSGWqXUw8ib5TTCuaaQPRDfYaB1TTzhtOdmG0U/640?wx_fmt=png&from=appmsg)
