# KuiklyUI Harness 工程建设总览

**文档定位**: 整体方案地图，每个模块有独立的详细规划文档
**文档状态**: 进行中
**最后更新**: 2026-04-01

---

## 什么是 Harness 工程？

> Agent = Model + Harness
>
> 模型提供智能，Harness 让智能变得有用。

Harness 是围绕 AI 模型构建的一套系统：知识库、工作流规范、架构约束、自动化工具。它决定了 AI 在你的代码库中能干多少、干得多好。


## 整体架构

```
KuiklyUI Harness
├── 模块 1：知识库管理                             ✅ 已完成（缺维护机制）
├── 模块 2：需求研发工作流                          🔲 未规划
├── 模块 3：Bug 跟进工作流                         ✅ 已完成（缺优化，P1）
├── 模块 4：架构约束与 Lint                        🔲 未规划
├── 模块 5：定期扫描机制（对抗知识库/代码腐烂）         🚧 思路确认（5.1 已确认，5.2 待规划）
├── 模块 6：团队使用要点                           ✅ 已完成
└── 模块 7：对抗评估（Adversarial Review）           🔲 未规划
```

---

## 模块 1：知识库管理

**状态**：✅ 已完成（缺维护机制，见模块 6）
**目标**：让 AI 每次会话都能快速理解 KuiklyUI 的架构、规范和当前状态。

### 设计参考

- [A Complete Guide to AGENTS.md](https://www.aihero.dev/a-complete-guide-to-agents-md) — 渐进式披露、< 60 行原则、少即是多
- [Writing a Good CLAUDE.md](https://www.humanlayer.dev/blog/writing-a-good-claude-md) — 触发条件写法、只放代码管不了的东西
- [Harness Engineering](https://openai.com/zh-Hans-CN/index/harness-engineering/)（OpenAI）— 知识库组织形式参考（我们采用了渐进式披露思路，但结构有所不同）

### 核心原则

- **少即是多**：根目录 `AGENTS.md` 只放普遍适用内容，控制在合理范围内
- **渐进式披露**：`AGENTS.md` 是地图 + 路由表，细节放在 `.ai/` 子文档中；子文档前 5 行写场景说明，AI 按需读取
- **只放代码管不了的东西**：能用 lint 强制的规范，不要放进 AGENTS.md
- **主动触发**：每份子文档前 5 行明确写「以下场景读取本文件」，AI 先读前 5 行判断相关性再决定是否读完整内容

> 多工具兼容策略详见：[附录 A：多工具上下文兼容](#附录-a多工具上下文兼容)

### 已实施的知识库结构

```
AGENTS.md                          # 根目录入口（模块速查 + 依赖边界 + 知识库索引）
CLAUDE.md -> AGENTS.md             # 软链接，Claude Code 兼容
.ai/
├── architecture/
│   └── AGENTS.md                  # 三层架构、模块结构、依赖关系、核心类
├── coding-standards/
│   └── AGENTS.md                  # 版权头、包名规则、KR 命名、日志规范
├── self-dsl/
│   └── AGENTS.md                  # 自研 DSL 开发指南（组件 API 索引到官网文档）
├── compose-dsl/
│   └── AGENTS.md                  # Compose DSL 开发指南（包名规则、架构原理）
└── references/
    ├── AGENTS.md                  # references 路由表
    ├── common-errors.md           # 错误路由表
    ├── errors/
    │   ├── self-dsl-errors.md     # 自研 DSL 专属错误
    │   ├── compose-errors.md      # Compose DSL 专属错误
    │   └── kmp-errors.md          # KMP 通用错误
    ├── native-bridge.md           # 原生扩展开发指南
    ├── native-bridge-internals.md # 通信原理深度参考
    ├── lazy-scroll.md             # LazyList/Grid 滚动机制
    ├── nested-scroll.md           # 嵌套滚动实现原理
    └── publish.md                 # 发布管理
```

---

## 模块 2：开发工作流（需求研发）

**状态**：🔲 未规划（AI 汇总，待 review 和改进）

**目标**：用标准化流程管理 AI 驱动的功能开发，避免 AI 一次性乱来。

使用openspec


---

## 模块 3：Bug 跟进工作流

**状态**：✅ 已完成（缺优化，P1）
**目标**：通过系统性根因分析 + 日志自我验证，让 AI 主导从复现到修复的完整流程，人工只在关键决策点介入。

> 详细工作流文档：[.ai/team/workflows-debug.md](./workflows-debug.md)

### 核心思路

- **让 AI 寻找根因**：用 `systematic-debugging` skill 驱动系统性分析，禁止在根因确认前尝试修复
- **日志自我验证**：AI 主动在关键节点添加诊断日志，通过读取日志验证自己的假设，而不是凭猜测直接修改代码
- **Skills 自动化编译运行**：`kuikly-app-runner` skill 承担编译/部署/日志采集，无需人工操作 IDE
- **人工介入 4 个决策点**：描述问题 / 操作复现 / 确认根因和修复方案 / Review 代码

### TODO

- [ ] 沉淀为 `kuikly-debug` Skill，实现一句话启动完整流程
- [ ] 探索自动化测试，替代「开发者操作复现」步骤
- [ ] 编写上下文管理指南（AI 变慢变蠢时的处理方式）

## 模块 4：架构约束与 Lint

**状态**：🔲 未规划（AI 汇总，待 review 和改进）

**目标**：用确定性工具（而非提示词）强制执行架构规则，防止 AI 写出违反设计的代码。

**核心原则**：不要让 LLM 做 Linter 的工作。

### KuiklyUI 核心约束（待完善具体规则）

```
架构层面
├── 模块依赖规则：Render 层 / Core 层独立，禁止反向依赖
├── 平台代码规范：expect/actual 使用规范
└── API 可见性规范：internal vs public 使用边界

代码质量层面
├── KMP 最佳实践（禁止平台特定 API 进入公共模块）
├── 性能反模式检测（如不必要的对象创建）
└── 废弃 API 使用检查
```

### 实现方式

| 约束类型 | 工具 | 优先级 |
|---------|------|--------|
| 模块依赖检查 | ArchUnit（Kotlin 生态） | P0 |
| 代码风格 | ktlint / detekt | P0 |
| 自定义架构规则 | detekt 自定义规则 | P1 |
| CI 集成 | GitHub Actions | P1 |

### Claude Code Hook 集成

在 AI 停止工作前自动运行 lint，确保每次 AI 修改后代码都符合规范：

```json
{
  "hooks": {
    "Stop": ["./scripts/lint-check.sh"]
  }
}
```

### 待完成

- [ ] 定义 KuiklyUI 模块依赖规则（具体边界）
- [ ] 配置 detekt + ktlint 基础规则集
- [ ] 实现核心自定义 lint 规则（模块依赖、API 可见性）
- [ ] 配置 Claude Code Stop Hook 自动运行 lint

---

## 模块 5：定期扫描机制

**状态**：🚧 思路确认（5.1 知识库维护机制思路已确认，待启动；5.2 代码扫描未规划）

**为什么需要做这个？**

> "Codex 会复现代码库中已存在的模式（包括不均衡的模式）→ 漂移"
> — [OpenAI Harness Engineering](https://openai.com/zh-Hans-CN/index/harness-engineering/)

> "定期运行的 Agent，发现文档不一致或架构约束违规，对抗熵增和腐烂"
> — [Martin Fowler: Harness Engineering](https://martinfowler.com/articles/exploring-gen-ai/harness-engineering.html)

AI 辅助开发中存在两种腐烂：
1. **知识库腐烂**：代码变了但文档没跟上，AI 拿着过时的知识库写错代码
2. **代码腐烂**：AI 复现了已有的坏模式，架构约束被逐渐侵蚀

---

### 5.1 知识库健康度扫描（初步构思，待启动）

**核心思路**：
- 不依赖定期人工扫描，改为**依赖 PR 触发**（代码变更时检查相关文档是否需要同步）
- 日常 AI 开发时，**AI 主动识别沉淀机会**（遇到文档未覆盖的知识点时主动提示）
- **执行方式**：AI 自动扫描识别问题 → 生成 PR → 人工只负责审批

**触发时机**：
- PR 合入时：涉及 API 变更、新增组件、目录结构调整
- AI 开发过程中：遇到需要多次重试才解决的问题、文档未覆盖的场景
- 手动触发：发现 AI 频繁出错同一类问题时

**AI 扫描检查项**：

| 检查项 | 说明 |
|--------|------|
| 文档准确性 | API 名称、目录结构、编译命令是否仍然正确 |
| 知识空白 | AI 遇到但文档未覆盖的问题，是否需要新增文档 |
| 链接有效性 | `.ai/` 文档中的官网文档链接、demo 路径是否仍然有效 |
| 场景说明质量 | 各文档前 5 行的场景说明是否足够精准 |

**待明确**：
- [ ] PR 触发的具体规则（哪些类型的 PR 需要触发知识库检查）
- [ ] AI 主动提示的时机和方式（如何嵌入日常开发流程）
- [ ] 文档更新的审核机制（谁来 Review、合入标准）
- [ ] 与代码同步策略（API 变更时如何自动触发文档更新）

---

### 5.2 代码层面扫描（待实施）

**目标**：检测架构违规、废弃代码，当前阶段暂未实施。

| 扫描类型 | 内容 | 频率 | 工具 |
|---------|------|------|------|
| 架构约束违规 | lint 规则之外的结构性问题 | 迭代结束时 | ArchUnit |
| 重复代码 | 相似代码块检测 | 月度 | PMD CPD |
| 废弃代码 | 未被引用的 public 方法 | 月度 | 待定 |
public API 与文档对比

**待完成**：
- [ ] 配置 ArchUnit 结构性测试
- [ ] 评估文档-代码一致性扫描工具

---

## 模块 7：对抗评估（Adversarial Review）

**状态**：🔲 未规划（AI 汇总，待 review 和改进）
**目标**：通过 AI 子 Agent 机制对代码产出进行质量评估，形成生成-评估的反馈闭环，减少人工 Review 负担。

### 核心思路

> "将前端生成与前端评分分离，可以创建反馈循环，推动生成器产生更强的输出。"
> "代码审查和 QA 在此扮演与设计评估器相同的结构角色。"
>
> — [Anthropic - Harness Design for Long-running Apps](https://www.anthropic.com/engineering/harness-design-long-running-apps)

**可落地的方向**：

| 方向 | 说明 | 优先级 |
|------|------|--------|
| **Code Review Agent** | AI 实现修复后，另起一个 Agent 做代码审查，输出问题列表 | P1 |
| **架构合规检查 Agent** | 检查 AI 修改是否违反模块边界、API 可见性等约束 | P1 |
| **Sprint 合约机制** | 实现前让 AI 先提方案 + 验收标准，实现后自检是否达标 | P2 |
| **生成器-评估器双 Agent** | 独立评估器 Agent 调校为怀疑论者，比让生成器自评更可靠 | P2 |

### 调研参考

- [Anthropic Harness Design 精读笔记](../../harness-engineer/research/anthropic-harness-design-long-running-apps-detailed.md) — 生成器-评估器架构详解
- 旧规划文档「评估器机制」章节 — 成本分析与待决策项

### 待明确

- [ ] 是否引入 Code Review Agent？评审标准如何定义
- [ ] 成本评估（Anthropic 实验中评估器成本显著高于生成器）
- [ ] 与模块 4（Lint）的边界：确定性规则交给 Lint，主观判断交给 Agent



| 决策 | 选项 | 阻塞项 |
|------|------|-------|
| 是否引入评估器机制 | 生成器-评估器双 Agent vs 单 Agent | 成本评估 |

## 模块 6：团队使用要点

**状态**：✅ 已完成
**目标**：让团队成员快速上手 AI 辅助开发，建立共同的工作习惯和经验积累机制。

> 详细文档：[.ai/team/getting-started.md](./getting-started.md)（工具清单、AI 工作流、经验总结）
> Bug 跟进工作流：[.ai/team/workflows-debug.md](./workflows-debug.md)

---

## 知识库建设 TODO

### 已完成
- [x] 确定多工具兼容策略（`AGENTS.md` 主文件，`CLAUDE.md` 软链接）
- [x] 编写根目录 `AGENTS.md` 初稿
- [x] 建立 `.ai/` 目录结构
- [x] 迁移 `architecture.md` + `project-structure.md` → `.ai/architecture/AGENTS.md`
- [x] 迁移并精简 `coding-standards.md` → `.ai/coding-standards/AGENTS.md`
- [x] 迁移 `lazy-scroll-contentsize.md` → `.ai/references/lazy-scroll.md`
- [x] 迁移 `nested-scroll.md` → `.ai/references/nested-scroll.md`
- [x] 建立 `.ai/references/AGENTS.md` 子目录索引

### 待完成
- [x] 迁移 `patterns/self-dsl.md` → `.ai/self-dsl/AGENTS.md`
- [x] 迁移 `patterns/compose-dsl.md` → `.ai/compose-dsl/AGENTS.md`
- [x] 迁移 `patterns/native-bridge.md` → `.ai/references/native-bridge.md`
- [x] 旧 `.ai/index.md` 核心原则提炼到规划文档，原文件可删
- [x] 旧 `.ai/architecture.md` 多版本构建说明迁移到 `AGENTS.md`，原文件可删
- [x] 建立 `.ai/references/common-errors.md`（整合 kuikly.mdc 错误章节）
- [x] 创建 `CLAUDE.md` 软链接（`ln -s AGENTS.md CLAUDE.md`）
- [ ] 将 coding-standards 关键规则（版权头、包名）内联到 AGENTS.md（待评估）

### 待调研 TODO
- [ ] 调研 CodeX Plugin / Claude Code 对抗性审查机制，评估是否引入 KuiklyUI code review 流程

## 知识库维护规范

新增或修改 `.ai/` 下的文档时，每份文档**前 5 行必须包含场景说明**（`> 以下场景读取本文件：...`），确保：

- 写**触发条件**，不写功能摘要——描述什么情况下需要读这个文件
- 列举具体触发关键词：类名、API 名、报错症状
- 覆盖同义词和症状描述——用户不一定用技术术语
- 至少列出 3 个触发场景
- 禁止写流程摘要（"第一步...第二步..."）

---

*文档版本: v1.0-draft*
*创建时间: 2026-03-31*
*替代文档: KUIKLY_HARNESS_PLAN.md（旧版，保留供参考）*

---

## 附录 A：多工具上下文兼容

各工具读取的上下文入口文件：

| 工具 | 读取文件 | 说明 |
|------|---------|------|
| **Claude Code** | `CLAUDE.md` | 软链接到 `AGENTS.md` |
| **OpenCode** | `AGENTS.md` | 无 AGENTS.md 时回退读 `CLAUDE.md` |
| **CodeBuddy** | `CODEBUDDY.md` | 无 CODEBUDDY.md 时回退读 `AGENTS.md` |
| **Cursor** | `AGENTS.md` | 支持根目录及子目录 |

**实现**：`AGENTS.md` 为真实文件，`CLAUDE.md` 为软链接（`ln -s AGENTS.md CLAUDE.md`）。

---

## 附录 B：多工具 Skills 兼容

各工具 project-level skills 的加载路径：

| 工具 | 路径 |
|------|------|
| Claude Code | `.claude/skills/<name>/SKILL.md` |
| OpenCode | `.claude/skills/<name>/SKILL.md`（兼容 Claude 路径）|
| Cursor | `.agents/skills/<name>/SKILL.md` 或 `.cursor/skills/` |
| CodeBuddy | `.codebuddy/skills/<name>/SKILL.md` |

**统一方案**：以 `.agents/skills/` 为真实目录，其余通过软链接指向：
```bash
.agents/skills/          ← 真实 skills 目录（已创建）
.claude/skills/    -> ../.agents/skills/   ← Claude Code / OpenCode 兼容（已创建）
.codebuddy/skills/ -> ../.agents/skills/   ← CodeBuddy 兼容（已创建）
# Cursor 同时支持 .agents/skills/，无需额外软链接
```

**已迁入的 skills**：
- `systematic-debugging` — Bug 跟进工作流核心 skill
- `kuikly-app-runner` — KuiklyUI 各平台编译运行工具
