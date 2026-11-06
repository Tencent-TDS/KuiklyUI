# Kuikly AI：为跨端开发构建 Harness 工程基座，开源首批 Rules 与 Skills

作者：Kuikly

发布时间：2026-04-22

原文链接：https://mp.weixin.qq.com/s/7pqy1Mruko4qyJAzbyCchw

AI 驱动开发是 Kuikly 2026 年的核心方向——我们希望将 AI 能力深度融入研发全链路。围绕这一方向，我们沿着客户端开发的各个环节逐一推进，实践了 Harness 开发并沉淀了工具，相关能力已在司内多条业务线落地，并显著提升了研发效率。现在，我们正式开源首批 AI 能力：Rules 与 Skills，诚邀各位开发者体验与协作，项目地址：[KuiklyUI-AI GitHub 仓库](https://github.com/Tencent-TDS/KuiklyUI-AI)

## 一、背景

过去两年，AI 正在深刻重塑软件工程。越来越多的开发者和研发团队开始将 AI 融入日常开发流程，并在编码效率上取得了显著提升。开发范式也在快速演进：

1. Vibe Coding —— 借助大模型能力，通过 Prompt 描述需求即可让 AI 直接生成代码；

2. Spec Coding —— 通过结构化的需求文档和技术规格，引导 AI 生成更可控、更符合预期的代码。

工程方法论也从 Prompt Engineering（如何写好提示词）演进到Context Engineering（如何管理上下文信息），而最新出现的 **Harness Engineering**（如何构建完整的约束体系）正推动 AI 研发走向可控、可验证的稳定交付。

然而在一线调研中我们发现，当开发者将 AI Coding 工具真正进入客户端开发场景时，新的挑战随之而来：

- **生成代码质量参差**：AI 生成的代码虽能快速产出、体量充足，却常呈现"野路子"特征——可运行但不规范，能实现但不优雅。错误的依赖引入、不存在的 API 调用、混乱的结构组织、不符合框架标准的写法，成为常态。

- **客户端场景天然复杂：**客户端通常面向多平台运行环境，项目中往往长期共存多套架构与技术栈，还要处理复杂的历史债务和组件依赖关系。与此同时，设计稿落地通常需要多轮视觉还原与反复确认，而客户端的运行验证链路又长、反馈又慢。这些恰恰都是当前 AI 最难直接介入的部分，也是研发效率最容易被拖慢的地方。

这些问题都在阻碍 AI 能力在客户端场景下真正走向**工程化落地与规模化应用**。

Kuikly 作为一个轻量、高性能、可动态化的跨平台开发框架，在腾讯内外广受好评，在 AI 大浪潮下，Kuikly 也在积极探索框架的 AI 化路线——不仅仅是让 AI 能写 Kuikly 代码，更要让 AI 写出好的 Kuikly 代码。

## 二、Kuikly AI

### 2.1 出发点：客户端需要 的 AI 工具

客户端在 AI 时代需要准备什么？

当下备受关注的 Harness Engineering 提出了一个核心观点：大模型需要一套工程化的"缰绳"——一个围绕 LLM / Agent 搭建的控制系统，让不确定的模型输出，在确定性的流程中被约束、被校验、被持续管理地完成复杂任务。这套系统通常包含五大方面：

1. 系统提示与上下文注入（Context Injection）

2. 工具与执行环境（Tools & Environment）

3. 控制与编排逻辑（Control & Orchestration）

4. 持久化与状态管理（Persistence & Memory）

5. 观察与验证（Observe & Verify）

如果把这五个方面映射到客户端开发场景，这意味着：

- **系统提示与上下文注入**：要给大模型注入专有知识，通用大模型的训练数据几乎不会覆盖这些专有知识。如果不主动注入一些规范和实践，AI 就只能用"通用写法"去猜，产出的代码自然不规范，需要让它在正确的知识边界内工作。

- **工具与执行环境**：客户端开发场景多样，可能是根据一份新设计稿从零搭建页面，也可能是对存量页面进行功能迭代或体验优化，还可能是将旧框架代码迁移到新框架。不同任务需要的上下文和工具完全不同，因此必须为 AI 提供可感知、可调用的工具能力，而不是让它在“信息黑箱”中盲目生成。

- **控制与编排逻辑**：客户端研发流程长、步骤多，任务拆解和执行顺序尤为重要。AI 只有在合适的工作流里被组织起来，才能真正协助开发，而不是在多个环节中反复返工。

- **持久化与状态管理**：客户端项目生命周期普遍较长，迭代频繁。模块架构、组件依赖关系、历史决策等关键上下文，需要被长期沉淀下来。否则每次新会话开始时，AI 都得重新“理解项目”，很难形成稳定输出。

- **观察与验证**：这是最关键、也是最难的一环。客户端的运行验证链路较长，很多效果问题只有运行后才能暴露。截图虽然能提供一些参考，但终究不是模型能够原生理解的结构化信息。真正有效的做法，是为 AI 提供机器可读的预览与可视化调试能力，让它不仅能“写”，还能“看懂自己写出来的结果”。

从这些角度来看，客户端真正需要的，并不是某一个单点 AI 工具，而是一套覆盖开发全流程的 AI 工程化能力体系。

### 2.2 解 法 ： Kuikly AI 能力 全景

基于上述思路，Kuikly正式推出了 Kuikly AI 项目，以客户端开发的各个流程为出发点，围绕构建完整工程能力体系的各个环节，将框架知识、组件规范、最佳实践和工具链系统化地注入 AI 工作流，框架承担起 AI 适配的责任——使用方只需专注业务本身，即可获得规范、高质量、开箱即用的 Kuikly 开发体验。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUB6mibLQbc7WfSI8yiaG1N5dT8l1spF6kOy8NeSEAXLFjw9ld2AbUHpBSuVTQibyKiaEIZGRn5EN0rME7BN1LgkRmkj1AwbvK9gibxo/640?wx_fmt=png&from=appmsg)

- **AI 问答 Agent**：沉淀知识库、指引文档，并结合代码 RAG，提升问题解答的准确性与深度，降低上手门槛和解决常见使用问题。

- **Kuikly Rules：**给模型注入相关上下文，涵盖开发规范、架构原则、最佳实践等内容，弥补大模型对 Kuikly 的理解缺失，提升输出代码的规范性。

- **Kuikly Skills：**面向特定开发场景的 AI 技能包，让 AI 能像专家一样处理各种复杂任务，实现高质量的智能代码生成。

- **Kuikly MCP：**基于 Model Context Protocol，让 AI 工具能够动态访问 Kuikly 官方文档、组件库索引和开发工具，实时获取知识，告别过时信息。

- **视觉稿转码(Deco)：**通过 Deco 工具将 Figma 设计稿自动转换为 Kuikly 代码，精确高效地还原设计意图，减少页面搭建工作。

- **预览 & UI Inspector：**在 AI 生成代码后即时预览运行效果，并配合 UI Inspector 可视化查看组件树与属性，补齐“观察与验证”这一关键能力。

- **转码 Agent：**支持将 React / Vue / Hippy 等主流框架的存量代码高效迁移为 Kuikly 代码，自动分析代码结构与组件关系，完成语法和 API 的映射转换，帮助业务平滑完成迁移。

- **AI Chat组件：**提供开箱即用的官方 AI Chat 组件，业务团队可快速在 App 内接入完整的 AI 对话能力，支持多模型切换、跨端一致体验和高度自定义 UI，快速赋能此类业务场景。

- **开发 Agent：**串联起从需求规划、代码生成、多端预览到一键验证的完整工作流，将分散的 AI 能力整合为流畅的自动化生产线，让开发者聚焦于业务逻辑本身。

## 三、Kuikly Rules/Skills

首批Kuikly开源的AI能力有 Rules，Skills

### 3.1 Rules

KuiklyDSL 是一套私有 DSL，现阶段大模型的训练语料存在一定欠缺，缺乏规则约束时极易生成不可用的代码；而 Kuikly ComposeDSL 虽然基于 Compose 范式，但在包名导入、组件 API、参数定义等方面与官方 Jetpack Compose 存在部分差异，模型很容易误用。因此，我们围绕两套 DSL 的高频问题，分别提供了对应的 Rules：

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUDy5WGPjCBBp2JkwUnjTl1BjpanqcnSk04XK1oP16TK37eO58YEWzia3lJzrkKw9XCuUlCs07dpSsXibdy9xicIX7rNGbSZL50f1g/640?wx_fmt=png&from=appmsg)

除了官方提供的通用规则外，使用方也可以进一步补充自身项目的架构约定、目录规范和最佳实践，让 AI 更贴合具体工程约束，真正做到“在你的工程里按你的方式工作”。

### 3.2 Skills

Kuikly Skills 作为给 AI Agent（CodeBuddy / Cursor / Claude 等）使用的技能库，核心目标是让 AI 在 Kuikly 项目中面对复杂开发流程时，拥有明确的规范和指导，助力更高效的完成各类需求任务。

目前，Kuikly Skills 已覆盖 11 不同技能，涵盖 Kuikly 开发的各个步骤：框架集成、UI构建、基础开发能力、能力扩展能方面。

随着技能体系的不断完善，AI 在 Kuikly 项目中的定位将不再局限于代码生成，而是进一步成长为能够参与完整开发流程的工程助手。未来，我们也会持续完善性能优化、组件市场等方向的能力布局。

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUC7DpEzvOp0PKEJSgBbhes53SpMsKgwgnibW9kQcLX4CIqhwtD8JtREXoStnicy5v5E3GRfHNbrl5sxFpEX0MqFAN7h6Uv1a24JM/640?wx_fmt=png&from=appmsg)

安装方式也很简单。打开你的终端，输入：

```
// 安装npx skills add Tencent-TDS/KuiklyUI-AI/skills// 更新 npx skills update Tencent-TDS/KuiklyUI-AI/skills
```

使用示例

```
"用 Kuikly 创建一个 Todo 列表页面""帮我给 xx 视图出现时添加一个动画"
```

### 3.3 接入建议

从目前已接入的业务反馈来看，无论是在已有工程上迭代新功能、搭建常见页面布局，还是从零构建新的 Kuikly 页面，Rules 和 Skills 都有效地增强了模型对 Kuikly 框架的理解。开发者普遍反馈，在引入 Rules 和 Skills 之后，AI 生成代码的规范度和可用性都有明显提升，整体开发过程也更顺畅。很多以往需要反复修正的框架细节问题，可以在生成阶段就被提前规避，最终往往只需要少量样式或交互层面的微调，就能达到预期效果。

因此我们建议 **Rules和Skills 全部使用，**以获得最佳体验**。**

## 四、后续规划

Rules 和 Skills 是 Kuikly AI 的第一步，但远不是终点。接下来，我们将沿着"让 AI 更懂客户端"这条主线，持续释放更多工程化能力， MCP 服务、视觉稿转码、预览 & Inspector、转码 Agent、AI Chat 组件、开发Agent等能力将会重点推进，并也将陆续与大家见面。

把繁琐交给Kuikly AI，把创造力留给自己～

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUAgayZh02jtaQyBhw52oSx6OtDtxC3n0qkIngWiaoPZGoE50hD2IpDibCoCIBanZHTLpQVr7FAYbXRzv9icq6HYpXFLJ1ricpvF2f0/640?wx_fmt=png&from=appmsg)

## 五、关于Kuikly

当前 Kuikly 已经开源，欢迎有兴趣的开发者或团队访问以下链接，查看仓库与文档，也欢迎大家 Star、Watch 并亲身体验。

👉 [Github 仓库](https://github.com/Tencent-TDS/KuiklyUI)| [官方文档](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical13)

Kuikly框架属于腾讯端服务联盟（tds.qq.com）的重要成员，欢迎关注及了解更多信息：

腾讯端服务官网：[https://tds.qq.com/](https://tds.qq.com/?utm_source=artical13)

TDS Framework官网：[https://framework.tds.qq.com/](https://framework.tds.qq.com/?utm_source=artical13)

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUCjBjOAWIXZtVZxTY2AiaN6V31ErsQYKwQcvaJJZNoaykvZIE7TA5HCB2C7debOkby3oMoEJvnWBx2JLaGDT2zPGIjTPH1EzbFk/640?wx_fmt=png&from=appmsg)
