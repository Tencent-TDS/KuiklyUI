# KuiklyUI Harness AI 编程工程建设规划

**文档状态**: 草案（待确认）  
**最后更新**: 2026-03-30  
**目标**: 构建支持多 AI 工具的 Harness 工程体系，提升 KuiklyUI 开发效率

---
## lint相关
- [ ] **建设 lint 规则体系**
  - 基于 OpenAI 2.4 节（规范架构与品味）
  - Kotlin/KMP 生态适配
  - 模块依赖检查
  - API 使用规范

- **结论**: 通过 lint 强制执行不变量
- **行动**: 建设 lint 规则体系 + error 处理知识库


## 自动bug修复 自动扫描

- [ ] **实现 AI 自动修复 bug 能力**
  - 基于 OpenAI 2.6 节
  - Bug 重现 → 修复 → 验证流程
  - 需要完善的工具和测试基础


## 需求工作流

### 决策 1: 需求开发工作流 ⭐ 已确认

**决策**: 采用 OpenSpec 作为需求开发的工作流

**背景**: 
- 精读 "Effective harnesses for long-running agents" 文章后确认
- OpenSpec 是文件驱动的方法，与 OpenAI 渐进式披露理念一致
- 支持计划与执行分离，符合 Harness Engineering 核心思想

**实施方案**:
1. 产品需求以 OpenSpec 文件形式定义
2. AI 读取 OpenSpec 后生成任务列表
3. 按任务列表逐项实现
4. 每个任务完成后更新状态

**与 Anthropic 双 Agent 模式的关系**:
- Anthropic 提出 Initializer + Coding Agent 分离，每次重新开新会话
- OpenSpec 方案：文件驱动，保持单一 Agent 模式
- **决策**: 暂缓采用双 Agent 模式，先用 OpenSpec 实践，后续根据效果再决定


- [ ] **制定 KuiklyUI 的 Research → Plan → Implement 工作流**
  - 适配三阶段提示词到 KuiklyUI 场景
  - 定义每个阶段的输出格式
  - 设计进度压缩和会话切换机制


### 思考项 2: OpenSpec 与 Sprint 合约机制的借鉴

**来源**: Anthropic 文章

**Anthropic 机制**:
- 规划器不指定很细的实现细节
- 生成器提议：要构建什么 + 如何验证成功
- 评估器审核：确保生成器构建正确的东西
- 两者对抗协商，达成一致后开始编码

**OpenSpec 借鉴思考**:
- [ ] OpenSpec 是否应增加"验收标准"章节？
- [ ] AI 实现功能后如何自检是否符合标准？
- [ ] 是否需要引入"生成器-评估器"对抗机制到 OpenSpec 工作流？
- [ ] 如何平衡规划的细节程度？（太细容易错，太粗容易偏）

---

**来源**: [HumanLayer - Advanced Context Engineering for Coding Agents](https://www.humanlayer.dev/blog/advanced-context-engineering) (2025-08-29)

### 13.2 Research → Plan → Implement 工作流 ⭐⭐⭐ 高度适用

**结论**: 采用三阶段工作流：研究（Research）→ 规划（Plan）→ 实现（Implement），每个阶段都有明确的提示词和输出要求。

**出处**: HumanLayer 团队在 BAML 30 万行 Rust 代码库上的实践经验

**参考提示词**:

**1. 研究阶段（Research）**
- 理解代码库、相关文件、信息流动
- 识别问题潜在原因
- 输出：研究文档（research.md）
- 参考: https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/research_codebase.md

**2. 规划阶段（Plan）**
- 概述修复问题的确切步骤
- 明确需要编辑的文件和方式
- 定义每个阶段的测试/验证步骤
- 输出：实施计划（plan.md）
- 参考: https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/create_plan.md

**3. 实现阶段（Implement）**
- 逐步执行计划
- 复杂工作在每阶段验证后压缩回计划文件
- 输出：代码变更 + 更新后的计划
- 参考: https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/implement_plan.md

**关键原则**:
- 人类审查应集中在高杠杆部分（Research 和 Plan）
- 代码审查（Implement）相对低杠杆
- Research 中的一行错误可能导致数千行错误代码
- Plan 中的一行错误可能导致数百行错误代码



## 待明确
- [ ] **是否采用 Anthropic 双 Agent 模式**
  - 背景：Anthropic 文章提出 Initializer + Coding Agent 分离，每次重新开新会话
  - 当前方案：OpenSpec 文件驱动，已满足计划与执行分离的理念
  - 疑问：是否需要每次重新开新会话？还是保持单一 Agent 模式？
  - 决策：暂缓，先用 OpenSpec 实践，后续根据实际效果再决定

### 13.3 Markdown 文件管理机制

**结论**: HumanLayer 团队开发了特定的 Markdown 文件管理方法（称为 "thoughts tool"），用于在 AI 会话间共享和更新文档。

**出处**: "How we manage/share markdown files... ask 'thoughts tool' how it works"

**待研究**:
- [ ] 研究 humanlayer/humanlayer 仓库中的 Markdown 管理机制
- [ ] 了解 "thoughts tool" 的具体实现
- [ ] 评估是否适用于 KuiklyUI 的工作流

**相关资源**:
- 仓库: https://github.com/humanlayer/humanlayer
- 研究示例: https://github.com/ai-that-works/ai-that-works/tree/main/2025-08-05-advanced-context-engineering-for-coding-agents/thoughts/shared/research
- 计划示例: https://github.com/ai-that-works/ai-that-works/tree/main/2025-08-05-advanced-context-engineering-for-coding-agents/thoughts/shared/plans


## 评估机制

### 决策 2: 评估器机制 ⭐ 待确认 (P1)

**背景**: 精读 Anthropic "Harness design for long-running apps" 后

**核心发现**: 生成器-评估器分离架构（GAN 启发）显著提升质量
- 生成器倾向于自我肯定，独立评估器可调校得更客观
- 评估标准具象化（将主观判断转为可评分标准）
- Sprint 合约机制（生成器与评估器协商完成标准）

**待决策**:
- [ ] 是否在 KuiklyUI 引入 Code Review Agent 概念？
- [ ] 评估标准如何定义（架构合规、代码质量、测试覆盖）？
- [ ] 成本考量（Anthropic 实验 $200 vs $9，需轻量级实现）

**详细分析**: 见 [research/anthropic-harness-design-long-running-apps-detailed.md](./research/anthropic-harness-design-long-running-apps-detailed.md)



## 团队ai共识：

### 思考项 1: 上下文焦虑与会话管理

**来源**: Anthropic 文章

**关键发现**:
- AI 存在"上下文焦虑"：接近上下文限制时提前结束工作
- 解决方案：上下文重置（开启新会话）+ 结构化交接文件
- **但**: Opus 4.6 不再需要上下文重置，新压缩技术足够好

**待思考问题**:
- [ ] KuiklyUI 开发场景是否需要长会话？
- [ ] 如果需要，什么时候触发新会话？
- [ ] 如何设计状态交接文件格式？
- [ ] OpenSpec 文件驱动是否已部分解决此问题？



### 11.5 迭代式改进与反馈循环

**结论**: 当 AI 遇到困难时，应识别缺少什么（工具、防护栏、文档），并让 AI 自己编写修复。

**出处**: "When the agent struggles, we treat it as a signal: identify what is missing — tools, guardrails, documentation — and feed it back into the repository, always by having Codex itself write the fix."

**落实方案**:
1. **人工发现 + AI 修复**: 团队在日常开发中记录 AI 困难点，定期让 AI 生成修复
2. **自动化收集**: 在 AI 开发过程中自动记录错误和重试，分析模式
3. **定期回顾**: 每周/每月回顾 AI 开发痛点，更新 Harness

**关键**: 需要团队共识和规范，主动人工发现是主要方式


### 12.2 子 Agent 作为上下文防火墙 ⭐⭐⭐ 高度适用

**结论**: 子 Agent 最有效的用途是隔离上下文，防止主 Agent 的上下文被污染。通过子 Agent 执行搜索/查找/总结任务，父 Agent 保持干净的上下文窗口直接开始工作。

**出处**: "Sub-agents are about context control... use a fresh context window for lookup/search/summarize, enabling the parent to start working directly without polluting its context window with Glob/Grep/Read calls"

**KuiklyUI 实践建议**:
- 使用子 Agent 进行代码库搜索和文件查找
- 使用子 Agent 生成研究文档和总结
- 主 Agent 专注于实现，保持上下文干净

**效果评估**: ⭐⭐⭐ 高 - 上下文隔离效果显著


**来源**: [HumanLayer - Advanced Context Engineering for Coding Agents](https://www.humanlayer.dev/blog/advanced-context-engineering) (2025-08-29)

### 13.1 频繁有意压缩（Frequent Intentional Compaction）⭐⭐⭐ 高度适用

**结论**: 当上下文占用达到 40-60%，或感觉 AI 变蠢时，应主动压缩上下文到文件并开启新会话。

**出处**: "Keep utilization in the 40-60% range... designing your entire development process around context management"

**团队提示词（Prompt）**:
```
把我们到目前为止做的所有事情写到 progress.md，确保记录：
- 最终目标
- 我们采用的方法
- 已完成的步骤
- 我们正在处理的当前失败
```

**KuiklyUI 实践建议**:
- 监控上下文利用率，保持在 40-60% 范围
- 当 AI 响应质量下降时触发压缩
- 使用结构化文件记录进度（progress.md）
- 压缩后开启新会话继续工作
