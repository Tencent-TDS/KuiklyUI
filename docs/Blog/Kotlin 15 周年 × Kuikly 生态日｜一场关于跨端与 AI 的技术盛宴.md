# Kotlin 15 周年 × Kuikly 生态日｜一场关于跨端与 AI 的技术盛宴

作者：Kuikly

发布时间：2026-10-09

原文链接：https://mp.weixin.qq.com/s/Drp1S__M33nUuwbmEZ7nYw

9 月 16 日，「腾讯 Kuikly 生态日 × Kotlin 15 周年」在腾讯总部园区圆满举办。活动聚焦跨平台开发与 AI 融合这一时代命题，多位来自腾讯多业务团队及Kotlin生态的专家轮番登场，围绕 7 大硬核主题，带来了近 4 小时的深度分享。

Kuikly 是腾讯开源的高性能跨端框架，基于 Kotlin Multiplatform 技术，覆盖 Android、iOS、HarmonyOS、H5、微信小程序、Mac 六大平台，支撑业务日活用户超5亿。从 AI 时代的 Kotlin 多平台开发，到 Coding Agent 的可靠性，再到 AI 转码、AI Native 架构、H5 落地等一线实战，本文将带你重温这场技术盛宴的精彩瞬间。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUBUCxrn2PELQy9orm3LwGDaP5btsJqOPf1zF4qdQFIsFvaToC9N3g8lIZvnF3C66n0ZfrvrooCuLtX3EQ0UKDhWNPkReHXPLpU/640?wx_fmt=png&from=appmsg)

## 开场：十五年，从一门语言到一片生态

活动在腾讯大前端委员会会长邱俊的开场致辞中拉开帷幕。致辞从 Kotlin 十五年的成长史说起——从当年的一门新生语言，到如今成熟而富有影响力的跨平台技术，这条路走得扎实而令人敬畏。

回顾 Kuikly 的来时路，从首次业务落地到 GitHub 开源，一步步走向成熟。致辞特别感谢了所有热爱 Kotlin 与 Kuikly 的开发者——正是大家在真实场景中的深度使用与持续反馈，鞭策着它不断完善。

谈及当下，致辞点出了一个关键词：**跨端与 AI 的融合**。这恰是「提效中的提效」。Kuikly 早已不止于一个跨端框架，它在工具链上持续深耕，在 Skills、DSL 插件、Agent 乃至 Harness 等 AI 方向上不断探索；而更精彩的，是各业务团队在真实场景中一个又一个难题的落地与解决。

最后，致辞寄望 Kuikly 作为 Kotlin 生态的重要伙伴继续并肩前行，也期待更多开发者与业务团队加入共建，让技术生态愈发繁荣。

## 范圣佑：AI 时代的 Kotlin 多平台开发体验

Kotlin 资深布道师范圣佑带来了本次直播的首场技术分享，围绕「超越代码生成」这一主题，系统展示了 Kotlin 在 AI 时代的最新演进。

他重点解读了 Kotlin 新版本引入的三项语言特性，并通过代码对比，展示了它们在减少样板代码、优化参数传递、简化集合创建上的实际收益。

- **Kotlin Language Server Protocol：**独立开源项目，支持代码补全、检查与重构，可接入 VS Code 等任意编辑器；

- **官方依赖搜索平台：**提供库的多平台支持、星级与版本信息，让选型更高效；

- **统一工具箱（Alpha）：**集成项目创建、构建、测试与发布等全流程命令；

- **Compose Multiplatform：**补齐多平台 UI 能力，并带来了 **Hot Reload** 新功能，保存即重绘、无需重新编译启动；

- **跨平台 AI Agent 框架：**用 Kotlin 编写，内置工具调用、记忆与 RAG 能力。

分享最后，团队强调：在 AI 辅助开发的大趋势下，Kotlin 的语言演进与工具链，对代码可维护性与人机理解度依然具有核心价值。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUA30VkWD6y6rnRmeQUEsZLcotiaaX5WggQdlt2HuWZERuwrFNPLy0Snc4Y9npb87vDwjljcwKszw2bCfJbP4kQLIzWkJSVRdhKI/640?wx_fmt=png&from=appmsg)

## 孙涛：有了模型之后，如何让 Coding Agent 真正可靠工作？

JetBrains AI 解决方案专家孙涛抛出了一个直击痛点的问题：模型很强，但 **Coding Agent 怎么才能稳定可靠地干活**？

他系统拆解了 Agent 测试评估的三大要素——固定依赖环境、真实工程问题、确定性验收，并介绍了以「任务解决率 + 编译通过率」为核心指标的评估体系。

- 提出了 **Harness** 这一核心概念，主张把自定义工具链、项目索引、RAG 等上下文供给纳入 Agent 综合能力评估；

- 展示了把 IDE 转化为「Agent 优先开发环境」的插件能力；

- 介绍了一套实验性开发环境，支持配置 Agent、对比不同 harness 差异并给出优化建议；

- 数据显示，高效接入 IDE 工具可显著降低响应延迟与 Token 消耗。

孙涛特别强调「优先使用官方工具」，并透露将全面转向支持多订阅切换、保留完整运行迹线的新一代 AI 插件，为构建可靠、可观测的 Coding Harness 环境铺路。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUB2gQ8CphnUKhBrpsonbhbUibY9Fib872HjLv9uwZaN2icvoXXP8csp8yria30IzX32ARXkQCPIa2DuJDpqPTRq2ph4y7o1hWgyvK0/640?wx_fmt=png&from=appmsg)

**李翰雄 & 陈振华：为 Kuikly 打造 AI 友好的 D2C & Inspector**

来自 Kuikly 客户端团队的两位专家，围绕「AI 还原设计稿」与「真机 UI 理解」两大难题，带来了 D2C 与 Inspector 工具的建设实践。

**D2C（Design to Code）** 直接读取设计稿结构生成可开发代码。嘉宾指出，还原效果取决于设计输入、工具映射、工程承接等多层因素，团队通过将规则检查前移至转码前，避免「垃圾进、垃圾出」，显著提升生成代码的工程价值。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUDGERu92nXp7ed3g4OujHZiaehPQKCS249DUGo7BUFaN1nicznQNQNELLwd4bQa0RO7SAKAt2jPiaXTJQx6lu9zXmvGv0f66icvCA4/640?wx_fmt=png&from=appmsg)

**Inspector** 则着眼于统一运行时语义。针对组件下沉到原生平台后「身份、可访问性文案、源码位置」等信息丢失的问题，团队提出了「编译期记录调用点 + 运行时采集高层语义」的方案，支撑 AI 精准定位、代码修改与自动化测试，为连接编码、运行与验证搭建了基础能力。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUAvsXN7IbqDlaq8dQTWuetbAEQWrWZX9bDIggoyIlh7YHUjk3xjXxhdoFpTIfmvM878csOHSDWvKBWfuRgeK5uic9aPwRZaU32U/640?wx_fmt=png&from=appmsg)

## 王振宇：Native2Kuikly AI 转码实践

腾讯自选股客户端王振宇带来了一个工程实践案例——用 AI 把存量 Native 代码迁移到跨端框架。

面对存量代码规模大、文档缺失与零异常上线的高质量要求，团队构建了**分阶段 AI 转移流水线**，遵循「上下文胶囊化隔离、子任务独立重跑、断点续传、顺序依赖」四大原则，配合多阶段调度执行的工作流，让迁移过程可控、可维护。

分享中令人印象深刻的是转码可用率的显著跃升——通过补充结构图、视觉对齐与逐项收口，团队将首轮转码质量大幅提升，剩余问题再辅以人工编码修补。

他同时强调，核心难点并非「代码生成」本身，而是「持续稳定交付」——这也与前面孙涛关于可靠性的判断形成了呼应。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUCIgribmPSibic19JKObabV3QMHCfszABrr6Crrm1nt3HNLZ1iciamQyBUz5gEmpBeibtqVqFsE24ml4aYwUOAPiayvA9x9GicQUMvpS18/640?wx_fmt=png&from=appmsg)

## 陈超铭：QQ 浏览器 AI Native 实践及演进

QQ 浏览器客户端Tech leader 陈超铭回顾了团队从早期试水跨平台到支撑鸿蒙版本的演进历程。面对海量存量代码的重写挑战，团队在多方对比后选定「KMP + KuiklyUI」作为下一代架构，以复用既有原生能力。

在 AI 落地上，团队没有照搬通用流程，而是自研了标准化工作流，并设立多重架构门禁，配合统一的契约声明，实现了鸿蒙、iOS、安卓三端 API 复用与稳定迭代。

落地成效同样亮眼：

- 三端代码共享率大幅提升，沉淀了一批可复用组件；

- 相当比例的常规需求可由 AI 生成，简单需求实现「一句话交付」；

- 通过增加编译验证、上下文收集等确定性流程，AI 交互轮次大幅压缩，交付稳定可控。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUDUJIbrwEGA45NW8Mc5sd2873aLyb3hYTkuQvrCQDPcOysfYiatYkXIFuvqId8sZT8dcFBCqicDJhia5LvxcUC95mBgkktKaxBgkc/640?wx_fmt=png&from=appmsg)

## 谢易成：Kuikly H5 在 QQ 游戏中心的落地与演进

QQ 增值前端开发专家谢易成分享了 Kuikly H5 化的实战经验。团队通过重构渲染链路，使千节点渲染耗时显著降低；针对打开黑屏问题，引入动态资源管理方案实现资源独立发布与预加载，冷启动与二次启动体验均大幅优化，基本消除黑屏。

在工程效率上，通过依赖分析实施按需构建，整体构建耗时大幅缩短；活动页产物体积也得到明显压缩。后续团队还将深入对比 WebAssembly 与 JS 在大数据计算及编解码上的性能差异。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUAnEibn0cMMWT1ahQibwuNjiaZMC8cC3SiazupOXS6PxVBO3nlVhKuocrO49iaJeTudSFTAIzVicRCsIB9njmB7TsjzhdIpLkkmf6qYA/640?wx_fmt=png&from=appmsg)

## 霍丙乾：Kuiklybase Kotlin 的昨天、今天和明天

腾讯视频跨端框架负责人霍丙乾为整场直播压轴，回顾了 Kuiklybase Kotlin 的版本演进之路。从为适配鸿蒙而发布的早期版本，到如今面临的多生态兼容挑战，团队已完成底层架构的统一整合，并在内部验证通过。

面对升级这一「势在必行」的选择，团队给出了务实的应对策略：

- 建立**无缝切换、一键回退、小流量验证**的平滑升级机制；

- 分阶段交付：先确保业务无感切换与 API 兼容，再逐步实施性能优化；

- 坚持「减少定制包袱、依赖官方优化、将定制贡献上游」的长期思路。

一个直观的「性能红利」案例：底层编译链路的耗时从小时级优化至分钟级。分享强调，升级落地只是开始，长期维护机制的构建才是可持续的关键。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUBg3S9PibS5oa7j6JO56lAXTnHcxvGHfKFic8MyHzzU0SiaDrS8kuWppbqIFOXQma5Ziamdm92HricQBFs12gliaUwH6NT5ibDvdVg24U/640?wx_fmt=png&from=appmsg)

## 线下互动：技术之外，热度不减

本次活动不止于线上的深度分享，线下同样精彩。我们在商业 A 区设置了现场互动区，参与者可在多个点位自由参与互动，现场人气火爆。Kotlin 与 Kuikly 的专家们也专程到场，与大家面对面交流跨端技术，答疑解惑；Kotlin 吉祥物 Kodee 更是成为全场的焦点，与伙伴们欢乐互动、合影留念，为这场技术盛会增添了不少轻松与活力。

![](https://mmecoa.qpic.cn/sz_mmecoa_jpg/2YTOibpx9RUDENZl03eRv5bVZAZX7QGKdQnoZzF0sTrMtdG1aVn1w5UtD0kfibtgjN97k8DYLm6Fk1TAp0flbfAcp3hw3LXib1xNVjGMpBqibSA/640?wx_fmt=jpeg&from=appmsg)

![](https://mmecoa.qpic.cn/mmecoa_jpg/2YTOibpx9RUDDlEwUOLeJZiaFZcns7ve3t1W1zwv3CY9xBsx1fr81PNiccygg0Jwu8bUgfJhJrw6VMyYg7D64gEY1IOgOQUFRyQEAjSj90oOas/640?wx_fmt=jpeg&from=appmsg)

## 写在最后

近四个小时的线上直播，7 大主题层层递进：从语言与工具链的演进，到 Coding Agent 的可靠性，再到 D2C、Inspector、AI 转码、AI Native、H5 落地、版本升级……线下互动区同样热度不减，让这场生态日真正做到了「线上见技术、线下见温度」。

一条清晰的脉络贯穿始终——**跨端与 AI 正在加速融合，而 Kuikly 正在这条赛道上扎实生长**。正如多位嘉宾反复强调的：AI 的价值不在于「能生成代码」，而在于「能稳定交付」。这也正是腾讯 Kuikly 生态接下来要持续回答的命题。

感谢每一位分享嘉宾的倾囊相授，也感谢线上线下每一位伙伴的参与互动。技术长路，我们下次再见。

## 关于Kuikly

当前Kuikly已经提供开源版本，有兴趣和有需要的产品，可以通过以下方式访问Kuikly仓库和文档，欢迎Star、Watch与体验：

👉[Kuikly Github](https://github.com/Tencent-TDS/KuiklyUI) | 📚[Kuikly 文档](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical23)

Kuikly框架属于腾讯端服务联盟（tds.qq.com）的重要成员，欢迎关注及了解更多信息：

- 腾讯端服务官网： [TDS 腾讯端服务](https://tds.qq.com/?utm_source=artical23)

- Kuikly官网：[Kuikly · 一码多端 极致易用 动态灵活的全平台高性能开发框架](https://framework.tds.qq.com/?utm_source=artical23)

- 「腾讯端服务」服务号

- 「Kuikly」公众号

- 在线咨询

![图片](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBGtBUvicf8N1oNs8yupK3bWO15bxfUcsqrQT2PPZ8huPlXJyJkjWRlpOtJhgVwaqQUf20Bxmtt6oPeQy3gKFIwmcgP7qBvmNFc/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=10005&wx_lazy=1#imgIndex=14)
