# 长小牛 × 腾讯Kuikly：金融类AI APP三端跨平台架构落地实践

作者：Kuikly

发布时间：2026-10-13

原文链接：https://mp.weixin.qq.com/s/5_SptmZS6NDcksvIO4neRA

当证券业务从单一页面扩展到行情、交易、资讯与 AI 服务，跨端建设要解决的早已不只是“一套代码跑三端”。面对传统H5的体验瓶颈与三端开发成本，**基于KMP的腾讯开源跨端框架Kuikly，凭借原生级的渲染性能、跨端一致性体验和KuiklyAI带来的开发效率提升**，成为破局的关键。长江证券以AI热点复盘为首个试点，在Android、iOS与HarmonyOS三端逐步搭建了覆盖直接调试、宿主验收、独立交付和公共能力复用的Kuikly多业务工程。本文将从技术选型、业务落地、四层架构、统一Bridge、打包交付、AI工程化与真机性能验证等方面，复盘这套体系如何从真实问题中逐步**演进**，并为金融App的跨端建设提供参考。

随着证券行业数字化转型与AI技术加速演进，正在重塑财富管理服务模式，针对传统券商App服务被动、触达低效、内容同质化等问题，**近年来，长江证券以AI原生架构重构移动证券服务，在行业内第三家推出了原生AI App“长小牛”**，覆盖盘前、盘中、盘后及日常场景，推动专业投资服务普惠化。项目以“智能猜你想问”为统一入口，根据客户浏览的资讯、行情、自选和持仓内容生成场景化问题，并通过AI半屏交互嵌入行情、交易、资讯，热点复盘等页面。服务覆盖行情解读、专业研究、持仓分析、交易辅助和个性化跟踪，形成“问题发现—分析解读—服务承接—订阅跟踪—人工服务”的完整链路，为券商AI App建设提供了可复制、可推广的实践路径。

在我们以往开发的项目中，每当有新的功能模块开发，往往是三端各自开发，这种模式在需求少的时候，开发成本还能够让团队接受。但是随着项目业务体系的增长和应用数量的增加，这种模式逐渐出现了开发验收效率低下，三端不一致，维护困难，甚至每个人都要不停切换环境和开发工具等问题，而现有的H5（如热点复盘，条件单，理财商城等页面），他们需要同时兼容Android、iOS和HarmonyOS，并且H5链路又难以同时覆盖原生性能、宿主能力，且动态资源下发受到网络质量约束。

**传统跨端方案把页面写成一份，主要解决了“实现一致”的问题；而性能、调试、验收交付和长期演进，才决定方案能否进入生产。同时，我们也希望将多个业务纳入同一工程开发和构建，再按三端宿主要求分别交付。**

基于这一背景，我们开始评估基于KMP构建的跨平台框架Kuikly。团队没有直接确定技术路线，而是在同一设备、同一网络和相近业务复杂度下，分别构建Kuikly、原生与H5页面，覆盖Android、iOS、HarmonyOS三端，并比较进入耗时、P95长尾、交互期资源占用和卡顿。结果显示，Kuikly的整体体验接近原生，相较H5的多数关键指标得到改善，尤其在HarmonyOS的页面进入和滚动表现上收益明显，因此我们继续推进工程化落地。

把一块复杂业务从既有H5迁到Kuikly，或让Android、iOS、HarmonyOS共用一份页面代码，确实能直接提升开发效率，但这只解决了问题的一半。不同跨端方案在渲染机制、包体积、宿主集成和动态化能力上各有取舍，真正进入生产还要面对工程边界与交付约束。我们的判断是：这不是一次单页面迁移，也不是简单接入一种跨端技术，而是在现有三端App中补上一层新的业务工程。KuiklyDSL是其中最可见的部分，但不独自承担全部工作：普通Kotlin代码负责共享模型、规则与数据编排，Swift、ArkTS等原生代码负责宿主能力与契约实现，KuiklyDSL负责跨端UI结构与交互。最终目标，是形成一套与既有H5开发模式衔接、能够脱离完整宿主直接调试，并具备明确打包规范和桥接准则的多业务跨端工程。

下面介绍的不是某个框架的能力清单，它更像一次从真实约束出发的复盘：我们为什么选择Kuikly，怎样组织DSL代码，为什么工程为四层架构，以及AI热点复盘功能这个首个试点最终让我们学到了什么。

## 为什么是Kuikly：先承认存量工程的边界

我们的三个正式目标平台是Android、iOS和HarmonyOS。它们不是空白工程：登录、交易、行情、权限、埋点、路由、主题，都已经在各自宿主中长期运行。任何跨端方案如果要求这些能力整体重写，代价都会远高于收益。因此一开始就有一个前提：跨端层只能以增量方式进入宿主，原生继续负责自己擅长、也必须负责的部分。

H5是项目当前主要使用的跨平台技术。在需求变化快、展示型内容较多的场景中，H5仍然高效，发布链路成熟，团队也已具备较完整的工具和经验。然而，当页面涉及复杂交互和原生能力协同时，WebView、页面生命周期、JS Bridge以及三端兼容逐渐成为工程负担，并在一定程度上影响用户体验。经过评估，我们最终选择Kuikly，并不是为了给技术栈贴一个新标签，而是看中它与团队Kotlin技术栈的契合度、灵活的KuiklyDSL、接近原生的运行体验、可控的包体积增量、较低的存量改造成本，以及覆盖三端和支持动态化的能力。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUDRT8lCagkVTzXvz2jSjgVgtONGZoKN7LB1ibWFCu2ZtIJz0619ibIkkJ0FkNIyCibJqc3VNicuvCqb3Wsx80eEm2fkAhFPxT7MEqU/640?wx_fmt=png&from=appmsg)

# AI热点复盘：看Kuikly业务页面如何落地

在这里，Kuikly主要承载跨端UI结构与交互，业务模型、规则和逻辑则由普通Kotlin代码实现。

热点复盘被选为首个正式试点，恰恰因为它不简单。它有主页面、技能切换和板块搜索等多个页面，主页面同时处理日期、技能、板块和个股等状态。卡片区域涉及手势和动画，详情区域有时间线与问答展开，关联热股又需要行情跳转和自选协作，再加上深浅主题、曝光与异步数据，这是一块足以检验跨端工程边界的真实业务。

落地时，我们把AI转码拆成四个阶段。第一步，读取H5/Vue源码，建立页面、组件、数据源、Bridge和资源清单。第二步，将官方文档沉淀为本地知识库，生成依赖说明和规则文件。第三步，导入H5/Vue → Kuikly转码Skill：先迁移模型、状态和接口，再生成KuiklyDSL页面结构，最后用Kotlin完成业务逻辑。在热点复盘试点中，按团队内部验收口径，首轮UI还原度接近90%，复杂交互再由人工与AI协同处理。第四步，在Demo中直接编译运行，逐项对齐布局、手势、动画和生命周期。AI首轮输出的目标不是“一次生成即可上线”，而是尽快得到可编译、可运行、可比较的页面骨架。随着项目知识库与官方能力持续沉淀，后续交易条件单及OA业务的AI首轮转码还原率按同一口径接近95%，剩余工作主要集中在人工与AI协同代码审查及复杂交互校准。

在Kuikly业务层，我们采用“轻量分层的组件化页面架构”，而不是严格的MVC或标准MVVM。这样既不必强制引入一层ViewModel，也能避免把所有状态和事件都塞进一个“大页面类”。

UI交互逻辑也应当从“看起来会动”变成“可以说明白”。以下面图示的卡片切换为例，牌堆的拖动偏移、缩放、透明度和动画锁属于UI瞬时状态，因此和牌堆组件放在一起，没有全部塞进页面级状态，这样阅读牌堆效果时，只需要关注一个职责文件，然而这个卡片交互并不是简单地左右切换页面。滑动过程中需要同时完成：

- 顶层卡片跟随手指横向移动；

- 后层卡片根据滑动进度上浮、放大和变亮；

- 超过阈值后，顶层卡片完整飞出；

- 未超过阈值或到达边界时，所有卡片回弹；

- 第一张和最后一张自动调整牌堆展开方向；

- 卡片区域同时支持点击和外层页面纵向滚动。

![](https://mmecoa.qpic.cn/mmecoa_gif/2YTOibpx9RUDmCUKfGIMl59aRicph9sP4JfYwXEuYEmNlFgrBhMVBZ1icQo58BdO8nTUMwGD0bpkDj3guicDeLgGhNxg3u5bcN5USXFDCSztErc/640?wx_fmt=gif&from=appmsg)

![](https://mmecoa.qpic.cn/sz_mmecoa_gif/2YTOibpx9RUBpic1HibcT1f5KxvNYBdzVSIUiaykFcmvejkZEQrhWP5TzqEW99tPSE3OpBjF4jbrMEv19nEPyWATx8XMDAUJOujmdjdBdt2iclaU/640?wx_fmt=gif&from=appmsg)

按照上述要求，我们最终使用真实的多层卡片，而不是用背景图模拟堆叠。每一层都根据同一组响应式状态计算缩放、位移、透明度等属性，使后层卡片能够在拖动过程中连续变化。切换时不能立即替换当前卡片内容，否则旧卡片尚未飞出，UI就会提前显示下一张卡片。因此，交互达到阈值后，顶层卡片先执行飞出动画，后层卡片同步上浮；动画结束后再更新当前卡片、重置各层transform，并解除动画锁。回弹时还要及时隐藏仅用于成功切换的补位层，避免短暂出现多余卡片。下方是部分实现代码。

```
internal fun AiHotReviewPage.finishBlockPan(deltaX: Float) {  if (abs(deltaX) <= BLOCK_SWIPE_THRESHOLD_POINTS) {    reboundBlockCard()    return  }    val direction = if (deltaX < 0f) {    SWIPE_DIRECTION_NEXT  } else {    SWIPE_DIRECTION_PREVIOUS  }  val targetIndex = targetBlockIndex(direction) ?: run {    reboundBlockCard()    return  }  blockStackState.isBlockTransitioning = true  blockStackState.blockDragOffsetX = if (direction == SWIPE_DIRECTION_NEXT) {    -pagerData.pageViewWidth  } else {    pagerData.pageViewWidth  }  schedulePageTimer(BLOCK_CARD_ANIMATION_DURATION_MS) {    viewModel.selectBlock(targetIndex, BlockSelectionSource.SWIPE)    resetStackTransforms()    blockStackState.isBlockTransitioning = false  }}
```

另外，在宿主页面中可以保留多个Kuikly View来提高返回速度，但任意时刻只能有栈顶View处于resumed状态，子页push时根页暂停，pop后当前页先detach，再恢复上一页。如果宿主只转发Activity或ViewController的前后台生命周期，根页实际上不会收到它重新可见的信号。因此，我们将这一规则补充到三端契约中；页面返回时在pageDidAppear中消费一次性结果并刷新，避免重新打开上一页或发起重复请求。

此外，业务层还使用了APNG组件，其解码、播放和本地资源定位仍由宿主实现，本文不再展开。

**热点复盘让我们确认：KuiklyDSL的经验不在于“翻译页面”，而在于先把状态、规则、数据与宿主边界拆清，再让DSL只表达页面。**

业务页面实现后，我们在原生侧对接Bridge，完成Kuikly容器、路由、生命周期管理和各类适配器，并打包到宿主App中验证。随后，新的问题出现了：一份业务代码可以在三个平台编译，却不能自动回答“宿主尚未就绪时如何开发和调试”“测试数据放在哪里”“测试页如何进入真实App、又不进入正式包”。最终采用的四层架构，正是从这些真实调试问题中逐步形成的。

# 四层工程：不是抽象分层，而是调试问题的答案

第一版工程的思路很直接：只有一个业务，Shared中同时放页面、公共组件、Mock和测试按钮；开发完成后再打包到宿主App中。第一个页面确实很快跑了起来，但第二、第三个业务接入后，问题迅速累积：接口层和Bridge能力依赖宿主，业务无法在工程内独立验证；不同业务重复实现相似能力；如果把验证页面和调试代码直接放进业务层，又会污染正式产物并增加包体积。

为解决这三个问题，我们设计了四层工程。Demo是开发调试根节点，聚合多个业务的验证入口，提供导航、测试登录、环境切换、Mock和调试Bridge，使跨端开发无需反复打包到宿主，即可在工程内直接运行和调试；它不是上线产品。Validation是宿主验收层，承载测试页、测试Client、参数输入和原始结果展示，可随对应业务进入真实App，用于验证容器、路由、登录态、Bridge和宿主环境，但不进入正式业务包。Shared是单个业务的正式交付单元，各业务拥有同级且相互独立的Shared，只包含正式页面、状态、数据仓库、资源和注册入口。Common则是各业务共同依赖的底座，承载稳定的跨业务能力；它不单独交付，而是随当前Shared或Validation聚合进最终产物。

当前Validation仍以人工真机联调和宿主验收为主，下一步将补充自动化验收场景。模型解析、状态转换和协议契约由commonTest自动执行；容器、路由、登录态、生命周期和Bridge等依赖宿主能力的测试，则通过host-validation包在三端真机运行，并统一输出结果、日志和截图。这样既能保持正式业务产物纯净，也能逐步把人工验收沉淀为可重复执行的测试模式。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUBDafpGoAK9EE97DDDXPk8hzd4BX2SxZrld0ich70NIpbxRB9He08nibzxVtpomMUODXcsKMbicbwRctV6ibHWdhDj6jzUZzxgD9NI/640?wx_fmt=png&from=appmsg)

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUDRX6u0cAcCrgAPR6GtyKFcjyiaSXhvsBZws7LgoZSMB77p9Hqa6XmoxicxO1fQw48bJh9Q2pdtzDj779TKbEydeuBmcho0NapEk/640?wx_fmt=png&from=appmsg)

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUDSvJVb97O0O3ayexM9uz87iaDWIIAX4LmicCYTaniaNsBuxE93OTIze2aMrLueNeTXbKS92ZjcqJjz7jhLr6EExa853BbbAibnJuE/640?wx_fmt=png&from=appmsg)

四层跨端工程与三端Native宿主的整体协作关系。多业务Shared同级，Common是共同底座，Demo可脱离宿主直接调试，Validation进入真实宿主验收。

在这套四层结构中，Demo能否顺利仿真宿主环境，取决于Common公共底座是否完整、稳定。

# Common与打包：共享能力必须在交付物里真的存在

Common并不是一开始就设计成一个大而全的工具箱，而是来自几类非常具体的重复：不同业务各自封装一遍实现基本一致的Bridge；同类结果在不同页面中采用不同解析方式；会话过期、签名、加密和错误日志被分别处理；同一个协议变化需要Android、iOS、HarmonyOS各改一遍。继续复制，短期看似更快，长期回归成本却会持续上升。

我们为Common设定了明确门槛：一项能力至少被两个业务复用，或已确定属于跨业务底层能力，才允许下沉；否则仍保留在Shared。接口层面，宿主现有接口依赖Socket，而当前NetworkModule暂不支持该协议。为满足Demo的独立调试需求，我们在后端做了轻量改造，通过HTTPS提供微服务请求，并在Common中统一实现HTTP、签名、加密、Token与解析逻辑。业务层只接触Client、配置和结果数据模型，请求流程大致如下：

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUDbntCxQooywunic2N4bAWbtEWmJskDlnx8jiaeScCHL5HCtVFcVpY5O11lDjFiaYfDhD9LbTIYzFxH8I6mV0gpFhXDnTqTNxNAOc/640?wx_fmt=png&from=appmsg)

Common层的Bridge改造尤其典型。三个宿主已经具备一套面向H5的Jockey解析体系，如果重做完全不同的通信格式，虽然表面上更“干净”，却意味着三端宿主需要再维护一套能力。我们的做法是保留两个外层入口：已有Web/Jockey语义的请求进入webRequest，Kuikly自有能力进入nativeRequest，具体方法在统一请求体中分发。页面不直接接触这些底层入口，而只依赖业务Bridge暴露的Kotlin API。这样既能复用既有能力，也能为新增能力保留清晰边界。

接下来是打包交付。Android宿主需要可直接消费的业务AAR，iOS需要同版本的XCFramework与资源Bundle，HarmonyOS需要SO、头文件和资源。正式交付物由Shared与Common聚合而成；宿主验收交付物则由Validation、对应Shared与Common三层聚合而成。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBqsk1iaQCkTagoeJGunKGQfHNaQq9pczwyCBK298RmSypOgQ11rCA9JIcTFc6h04ukF6WdGc4noPWJrV5x0RtiaAuicqq8BkiaJz8/640?wx_fmt=png&from=appmsg)

正式交付与宿主验收交付的组成关系。

在Android打包过程中，我们发现Shared产物无法直接将Common的编译文件与依赖完整合并，容易产生多个AAR。为此，我们构建了专门的交付工程，通过Fused Library聚合工具将多个AAR及其依赖合并为一个AAR，确保每个业务最终只输出一个可供宿主消费的产物。

# AI能力沉淀：让工程规则能被多业务执行

在单页面项目中，AI生成一份能运行的代码，可能已经完成了大部分工作。但在多业务Kuikly工程中，真正困难的是判断代码应该属于哪个业务、哪些能力可以进入公共底座Common、测试代码是否会污染正式产物。

因此，我们沉淀AI能力时，关注的不是“怎样更快写出一个页面”，而是“怎样让新业务按照同一套工程结构快速接入，并且一开始就具备可调试、可验收、可独立交付的能力”。所以我们把AI能力拆成两部分。第一部分是边界规则，回答“什么可以做、什么不能做”。例如：页面不能直接拼Bridge，公共HTTPS不得回退到宿主代理，测试页、Mock和测试Client有自己的模块归属，正式包和验收包必须走不同打包流程。第二部分是可执行的Skills，回答“遇到某类任务应该按什么顺序做”。

为了让规则能被查找、复用和持续更新，我们把AI资产按“索引—规则—Skill—变更证据”分层保存。目录本身就是使用入口：

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUCzkMfwjAIUtXWKnoP4icgDj4mSFbNFvD4icVRrCibhzzJJQ5GHoOy7AAHEgSdwSCWuIctW5wKhunHvfSt1TDaSBG4La8gXOcNkww/640?wx_fmt=png&from=appmsg)

## 项目专项Rules

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUBficXBoFpiakopW8sRQsDMoBvLHmT20zAt3CbAkPGqQKiaSEV7AMuMA5LveTJtP2RFLBSLo8RU6z4Diba1HgibDXqMQxeB5YjAVFT4/640?wx_fmt=png&from=appmsg)

这些沉淀分为两层：底层复用KuiklyUI-AI官方Skills，确保组件、状态、网络、动画和原生扩展方式正确；上层沉淀项目级Skills，解决多业务归属、H5迁移、统一Bridge、直接调试Demo和三端交付问题。下面先列项目中经过真实问题反复验证的典型Skills。

## 项目沉淀的Skills

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUD9meLNoXP5W1TdvzrZPk7uasfzJicgBjEibOPQUMTScfbzyjHXdGdMYwWndeVicEDGjQYBJnTRt0akQY9A4icQvCtgnG02dib2Ribaw/640?wx_fmt=png&from=appmsg)

## 使用到的Kuikly官方Skills

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUBRlv03GsiaIx3icSAovtib5LqrgOicK27N4dhqBcH4AMNWRkDBz2DQBUrTVbK4ANA0T29gjFsGwVDDJjGh8DDRTnlgyQxsKrnkP6k/640?wx_fmt=png&from=appmsg)

# 性能验证与总结

三份真机报告覆盖Android、iOS和HarmonyOS。以下只比较同一平台从H5到Kuikly的相对变化，不做三端绝对值排名。Kuikly在多数核心运行时指标上均有改善：HarmonyOS的页面进入、CPU和内存表现提升明显；Android的滚动CPU、帧耗时和内存稳定改善；iOS的完整页面进程组内存与受控滚动连续性明显改善。三端核心数据如下。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBkrbKe7T8BlwTfymR9rNEIJxQR8gYlZp388vxibqExJjicdb6Vu8tPAGfuhqCSXkeibynBxSXW8s5aP5uj1CWAl2Qcg4DLokjJus/640?wx_fmt=png&from=appmsg)

说明：三份报告均采用同平台、同设备、同操作序列的H5与Kuikly对照，完整PSS按各报告可归属的页面进程组或全应用口径取稳定态数据。不同平台采集工具不同，因此不做绝对值横向排名。

综合三端结果，Kuikly的价值已经从“页面一致”扩展到可量化的运行效率。HarmonyOS平均进入耗时下降20.7%、滚动CPU下降86.2%、稳定态PSS下降28.5%；Android P95进入耗时下降7.5%、滚动CPU下降6.6%、P95帧耗时下降28.6%，稳定态完整PSS下降约36.1%；iOS完整页面进程组内存下降约59.4%，受控滚动Hitch从8次降至1次。总体来看，Kuikly在多数关键指标上优于H5，但不同平台的收益分布并不相同，后续仍需持续补齐薄弱项。

## 跨端的价值，不是让三端看起来一样，而是让三端在同一套边界内稳定协作。

回头看，这一阶段最重要的成果不是简单增加了一套跨端页面，而是形成了一套面向多业务、可直接调试、可进入三端宿主验收的工程体系。KuiklyDSL统一了业务表达，Demo让页面无需等待宿主集成即可运行调试，Shared与Common保证正式交付边界清晰。热点复盘的三端实测进一步表明，这套体系不仅提升了开发效率，也在内存、CPU、帧耗时和交互连续性等多数关键指标上优于H5；后续我们将继续补齐iOS首屏、低端设备、冷热启动、可隔离包体与长时运行验证，把已经验证有效的能力沉淀为更多业务的默认工程能力。

# 写在最后

长江证券的这次实践说明，跨端建设的真正价值，不只是让三端共用一份代码，而是把业务边界、调试方式、宿主契约、验收机制和交付形态同时拉齐。只有当这些能力被沉淀为可复用的工程底座，新业务才能以更低成本进入三端，并在性能、体验和长期维护之间取得平衡。

从AI热点复盘到后续条件单和OA业务，Kuikly已从单点试验演进为可复用的多业务体系。接下来，我们将继续完善自动化验收、性能基线和长时稳定性验证，让这套工程能力在更多金融业务中稳定复用。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUDyq9538OFAyibNPajkwqia7A4B4wfv4fAhLiaiatIqrEZSrjGhW3YWsib4I46VDfuMxkQxhLQicaAM22ALorNNu9hkq2861jndvt7I8/640?wx_fmt=png&from=appmsg)

长小牛App

# 关于Kuikly

当前Kuikly已经提供开源版本，有兴趣和有需要的产品，可以通过以下方式访问Kuikly仓库和文档，欢迎Star、Watch与体验：

👉[Kuikly Github](https://github.com/Tencent-TDS/KuiklyUI) | 📚[Kuikly 文档](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical24)

Kuikly框架属于腾讯端服务联盟（tds.qq.com）的重要成员，欢迎关注及了解更多信息：

- 腾讯端服务官网： [TDS 腾讯端服务](https://tds.qq.com/?utm_source=artical24)

- Kuikly官网：[Kuikly · 一码多端 极致易用 动态灵活的全平台高性能开发框架](https://framework.tds.qq.com/?utm_source=artical24)

- 「腾讯端服务」服务号

- 「Kuikly」公众号

- 在线咨询

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBGtBUvicf8N1oNs8yupK3bWO15bxfUcsqrQT2PPZ8huPlXJyJkjWRlpOtJhgVwaqQUf20Bxmtt6oPeQy3gKFIwmcgP7qBvmNFc/640?wx_fmt=png&from=appmsg)
