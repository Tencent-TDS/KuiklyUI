# AI智能转码 + 原生高性能：QQ音乐存量代码迁移Kuikly实践

作者：Kuikly

发布时间：2026-05-13

原文链接：https://mp.weixin.qq.com/s/WDTBnrc-ge19Sa_e7LPojQ

导语 Kuikly 是腾讯开源的高性能跨端开发框架，支持基于 Kotlin 原生开发语言构建 Android、iOS、鸿蒙、Web、小程序及 Mac 多端应用。作为较早接入Kuikly的业务，QQ音乐一直深度使用Kuikly，已累计开发200+页面，并持续迁移存量页面至Kuikly。在AI背景下，我们进一步探索 React 转 Kuikly 智能转码方案，大幅提升跨端页面迁移效率和业务性能表现。

## 一、背景

Kuikly 是 新一代高性能、跨平台、动态化跨端解决方案，目前已在 QQ 音乐团队中得到深度应用。然而，项目中仍遗留了较多 基于 React 开发的 H5页面。团队计划将这些页面统一改造为 Kuikly，但面临着庞大的工作量挑战。

相较于传统 React H5 页面，Kuikly 原生跨端架构具备天生高性能渲染、低延迟加载优势，不仅能统一多端体验，更可从底层解决 H5 页面加载慢、交互卡顿等痛点，这也是团队下决心批量迁移重构的核心原因。

为了破局，我们尝试引入 AI 来辅助完成代码转换。在此过程中，我们联合 Kuikly 团队，将Kuikly核心知识库“喂”给 AI，一起逐步构建了一套自动化的 AI 工作流。通过采用分步骤的渐进式转换策略，我们有效保障了转换过程的准确性、可控性以及可恢复性。

目前，借助这套 AI 转码方案，我们已顺利完成 9 个 H5 页面及 3 个公共组件的 Kuikly 改造。整体转换效果优异，**AI 代码采用率高达 90% 以上**，对于中等复杂度的页面，开发周期（包含代码调整和测试验证）从预计的 3 天以上大幅缩短至 1 天以内，极大地减轻了人工重构的负担。改造完成后，**页面加载耗时降低90%**，性能全面超越原有 React 版本。

QQ音乐乐库页面的转码视频：

为了让这套能力惠及更多团队，我们和Kuikly 团队共建了“核心 Agent + 业务配置”的架构。这使得转换工作流具备了极佳的复用性与可维护性，其他业务团队只需接入专属的业务 Agent 即可快速上手。

## 二、转码架构与流程速览

### 2.1 整体架构

![](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUCAibSSU1Xl5QSuzaZrpic5bje2jdicqJXFY7FCicM7FbvKAEeiaJWAtzdibWp1FXtcjKcOdk6zo7R8ICp4fWOezOKHLpibQaJ888Gyt8/640?wx_fmt=png&from=appmsg)

我们使用插槽式设计，实现了流程逻辑与业务知识的解耦，通过更换底层的“业务 Skill”，其他业务即可快速接入。

### 2.2 转换流程图

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUAkyj8uicDTmqFtE3MluczXrsSa1CwViaLdOfdLYphQkez0V4JQVicSfSPTyu3sm9HbTfu5CI4wicEWo0vDVWJoCxFN3858ewvneng/640?wx_fmt=png&from=appmsg)

整个流程主要分为了4个大步骤：

**1.初始化预处理：**对资源做预处理和缓存检查

其中我们用脚本处理了base64/svg等内联资源来避免token浪费

增加了缓存检查步骤，避免重做

**2.关键字与知识收集：**通过提取关键字实现了精确检索，获得了高密度高准确度的知识

**3.代码生成阶段：**我们通过渐进式转化，保证了每个文件的准确度

**4.验证和交付阶段：**我们通过自检和编译检查保证了代码质量

整个流程中，我们也在关键节点加入了人工干预环节，持久化了关键数据，前置错误风险，以及支持进度识别，以便断点续转。在第3章节会介绍方案的一些演进和思考。

### 2.3 目录结构

```
<项目根目录>/├── react2kuikly/│   └── [页面名称]/│       └── cache/                         # 进度与知识缓存目录（随时可恢复）│           ├── progress.md                # 进度跟踪文件│           ├── project-keywords.md        # 业务关键字集合│           ├── knowledge-cache.md         # 统一转化知识库│           └── conversion-plan.md         # 转换计划与代码分析报告│└── $OUTPUT_DIR/                           # 代码输出目录（根据业务自定义）    ├── [PageName]Pager.kt                 # 页面入口    └── composedview/    └── models/      └── viewmodel/.codebuddy/skills/react2kuikly/ # 主 Skill 本体（核心引擎，无需修改）├── SKILL.md # Skill 主入口├── scripts/ # 自动化脚本工具集 (缓存检查、进度更新等)└── references/ # 框架知识与转换SOP文档
```

## 三、关键方案演进和思考

### 3.1 智能关键字提取与按需知识加载

在早期探索中，我们习惯性地将所有框架文档和业务规则“全量注入” Prompt。这不仅导致 Token 消耗急剧膨胀，更致命的是引发了 AI的“注意力稀释”，导致生成的代码经常出现错误。

所以，这里采取了“智能扫描 + 按需加载”的精细化策略：

- **精准扫描**：在转换前置阶段扫描源码，精确识别当前页面实际用到的组件和业务场景。

- **动态组装**：除了全量加载基础框架知识以确保底座准确外，对于组件文档和业务规则文件则严格执行“按需加载”。这样大幅降低了 Token 消耗，也让 AI 的注意力高度聚焦，显著提升了代码生成的准确率。

### 3.2 内联资源预处理

另一个踩过的坑是 React 项目中常见的内联资源。最初直接转换时，包含大量 base64 编码图片或超长 SVG 节点的文件成功率极低。这些大段的无意义字符白白占用了宝贵的上下文窗口，还严重干扰了 AI 对核心业务逻辑的理解。

为此，我们在核心流程前置了专门的预处理脚本（步骤 2）。它会自动识别并提取所有内联资源，将它们替换为精简的本地文件引用。同时，脚本还支持跨目录的别名路径（如 @/）追踪，并顺手抹平了一些框架级的细微差异，比如自动完成 SVG 的 color 到 tintColor 转换。

### 3.3 渐进式转换策略

对于大型复杂页面，传统的“一键生成”模式极易触发模型的注意力衰退，导致严重的幻觉。为此，我设计并落地了渐进式转化架构。核心思路是将迁移过程从“黑盒生成”转变为“分而治之”。

## 方案对比：

#### 一次性全量生成

- **核心思想**: 将知识库和全部React源代码一起交给AI，一次性生成所有Kuikly文件

- **优点**: 流程极简，开发成本低，理论上下文完整

- **缺点**: 上下文窗口硬限制，注意力衰减，可控性为零，错误恢复代价极高

#### 分步骤渐进式生成（✅ 最终采用）

- **核心思想**: 先分析结构，再逐文件转换，最后架构整理

- **优点**: Token消耗可控，业务逻辑准确，工具调用稳定，错误恢复成本低

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUCiaiaEv93amJjywv9LorepiawJCXh0FibreibRLibGV8G8icsqYicSGdCZTr7rMpW1PKWw5Xupf1Uaictq0uNicm5dvj5rsI8YqX5h2pyDY/640?wx_fmt=png&from=appmsg)

这里我们评估了两种方案后，最终采用渐进式转换策略**（先逐文件完整转换，后架构整理）**

### 3.4 进度持久化与断点续传

为了进一步提升方案的工程化程度，我们引入了过程存档与断点续转机制。

#### （1） 核心问题：AI Agent对话的"失忆"和"失控"

在复杂 AI 转码任务中，一旦出错往往需要全量重做，这在大型工程中是不可接受的。我们需要将关键节点的产物（如关键字提取结果、转换计划）进行持久化存档。

#### （2）设计方案

采用进度记录系统和关键节点存档询问机制两套互机制。

进度记录系统:

每个页面转换任务有一个独立的进度文件react2kuikly/[页面名]/cache/progress.md，支持两级粒度:

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUBAZSykIw3MQzTf9Zia2IzI9z4K6y0NjP9HG4mhibWfTOjhTHe33VyAouvbWwH6UGkDPdicStWibGiaicLiaAjwjslicMjicdccj7WYvRbI/640?wx_fmt=png&from=appmsg)

### 3.5 双重质量保障

#### 转换中检查

- 每个文件转换后立即执行单文件逻辑验证

- 立即执行业务 Checklist 检查并修正

#### 转换后检查

- 全局逻辑功能完整性验证

- 编译检查（循环修正直到通过）

### 3.6 可插拔业务 Skill 设计

考虑到想把这个方案让更多业务能直接复用，我们也和Kuikly团队共建了这套方案，让核心 Skill 与业务规则完全解耦：

```
react2kuikly（核心）    └── 调用 ──▶ qqmusic-react2kuikly（业务 Skill）    └── 调用 ──▶ 其他业务 Skill（可自行接入）
```

业务 Skill 通过PATH_CONFIG 声明资源路径，核心脚本动态解析，无需修改核心代码即可接入新业务。

## 四、转换效果

#### 项目成果：

目前已通过 AI 转码方案完成了 9 个 React 页面及 3 个公共组件的 Kuikly 改造。对于中等复杂度的页面，对于中等复杂度的页面，开发周期（包含代码调整和测试验证）**从预计的 3 天以上大幅缩短至 1 天**以内，大幅减少了人工重构的工作量。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUDWLMtqwSQuh6F8qUBG4Ps1xYtozvFKuLotbS1dQuZfxECRRrriclHAiaZNwUVS7reO8Qa0QO3pNIYdE9mUoFzncklqJf91QU4l8/640?wx_fmt=png&from=appmsg)

除研发效率大幅提升外，业务体验也实现质的升级：依托 Kuikly 原生高性能跨端渲染能力，**改造后页面加载耗时降低90%，性能全面超越原有 React 版本**，彻底摆脱 H5 容器性能瓶颈，在安卓、iOS、鸿蒙多端均实现一致的高性能体验。

![](https://mmecoa.qpic.cn/mmecoa_png/2YTOibpx9RUDTsfE40HDy4TG7KszoBYSJlH2Cs7vmw159y3aicnDt9I6feuJdic8HHSeBcQRfcubDdOtDXZwrG5XQ485a8m0tOGKJSJ1xfm8us/640?wx_fmt=png&from=appmsg)

## 五、后续规划

后续我们将持续拓展 AI 转码框架能力，除已成熟落地的 React、Vue 框架转译外，目前已探索借助AI把**原生应用的代码向 Kuikly 转码，**并取得显著效果。我们将持续完善多前端框架、原生应用的规则适配，打造通用化跨端迁移 AI 解决方案。

同时，整套 AI 转码方案已筹备开源，后续将持续沉淀技术成果、对外分享实践经验，回馈开源社区，敬请期待！

## 六、关于Kuikly

当前Kuikly已经开源，有兴趣和有需要的产品，可以通过以下方式访问 Kuikly 仓库和文档，欢迎Star、Watch与体验：

👉[Kuikly Github 仓库](https://github.com/Tencent-TDS/KuiklyUI) | 📚[Kuikly 官网](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical13) | [Kuikly AI 仓库](https://kuikly.tds.qq.com/AI/)

Kuikly框架属于腾讯端服务联盟（tds.qq.com）的重要成员，欢迎关注及了解更多信息：

- 腾讯端服务官网： [https://tds.qq.com](https://tds.qq.com/?utm_source=artical13)

- TDS Framework官网： [https://framework.tds.qq.com/](https://framework.tds.qq.com/?utm_source=artical13)

![图片](https://mmecoa.qpic.cn/sz_mmecoa_png/2YTOibpx9RUDArBfiaIK9o3c5JCzS0QCr1gMoNniaB2GOfLzl6d2l8DNmF3y1kFuBsQsWtAzVhu19t6OWuKHrviaUcqDt0ic1VDVh0PhD4ia6tbyo/640?wx_fmt=png&from=appmsg&tp=webp&wxfrom=10005&wx_lazy=1#imgIndex=11)
