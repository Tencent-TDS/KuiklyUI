---
name: kuikly-changelog-gen
description: 以 GitHub Release 的 What's Changed 为事实源，生成指定版本的三份变更文档（版本说明 / 变更汇总 / 破坏性与迁移）并落到 docs/ChangeLog/<version>/。当用户说"生成 X.Y.Z 的变更说明"、"生成 2.28.0 的三分文档"、"写一下这个版本的更新日志"时触发。脚本负责 tag、PR 白名单、分桶和文档骨架，AI 负责把开发者修改翻译成用户能做什么 / 怎么用 / 有什么效果，并判定业务可感知的 breaking；格式契约见 docs/ChangeLog/TEMPLATE.md。
license: MIT
compatibility: 需要 git 仓库且配置了 upstream / origin / 官方 GitHub 远端之一（用于补齐缺失 tag）；需要 Python 3。
metadata:
  author: kuikly-changelog
  version: "1.0"
---

# kuikly-changelog-gen

> 本文件是版本三分文档的生成流程规范（skill），随框架仓库维护，位于 `docs/ChangeLog/` 下。
> 与 TEMPLATE.md 一样属于内部规范，不会同步到官网站点（sync 脚本已排除）。

## 触发时机

- 用户说"生成 2.28.0 的变更说明"、"生成这个版本的三分文档"、"写一下 X.Y.Z 的更新日志"
- 发版流程中需要产出变更文档时

---

## 执行流程

### Step 1 — 运行生成脚本（机械部分）

```bash
python3 docs/ChangeLog/generate.py <version> --release-prs <PR1,PR2,...>
```

先从 GitHub Release `<version>` 的 `What's Changed` 提取完整 PR 号列表，再运行脚本。脚本行为：

- 用远端完整 tag 列表确定版本区间（`<prevTag>..<version>`），本地缺失时自动补齐前后两个 tag（upstream → origin → 官方 GitHub）
- 收集区间 commit（subject + 改动路径），按规则分桶：多端 / 单端（Android → iOS → 鸿蒙 → H5 → 小程序）/ 其他
- 使用 `--release-prs` 将 `changelog.md` 候选限制为 Release 实际发布的 PR；不在 `What's Changed` 的本地 commit 不得写入
- Release 中存在但本地 tag 区间找不到的 PR 会输出警告，Skill 必须按 Release 原文手动补入（例如 2.27.0 的 #1715）；Release 同时保留的原提交与 Revert 也不得擅自去重
- 生成 `announcement.md` / `breaking.md` **骨架**（含 `<!-- 待补全 -->` 标记、高风险 commit 清单和用户文档改动提示）
- 写入 `docs/ChangeLog/<version>/`（目录不存在则创建，文件已存在则覆盖）

### Step 2 — 补全 announcement.md（开发者向用户的结果说明）

`announcement.md` 不是 commit 摘要，也不是源码实现说明，而是把开发者的修改翻译成用户能执行的三件事：**你现在能做什么、怎么使用、会得到什么效果**。主数据源仍是 GitHub Release 的 `What's Changed`；关联 PR/commit 的正文、文件列表和 diff 只用于确认意图、用户效果和文档改动，不能脱离来源自行编造。

对每个候选能力点执行以下固定流程：

1. **建立事实卡片**：记录 GitHub `What's Changed` 原文标题、PR 号、关联 PR/commit 正文、实际改动文件和代码证据。
2. **提取开发者意图**：优先使用 PR/commit 中明确写出的用途和用户效果；若开发者已有总结，以该总结作为文案口径，代码只负责核实能否成立。
3. **写结果句**：先写用户最终能做什么或问题得到什么改善，再写必要的使用方式、适用端和限制。只保留业务需要认识的公开 API、属性、事件、组件、Module 或配置项。
4. **去掉实现层**：不要把 KSP、生成注册表、扫描收口、内部类、Native 调用链等实现过程写进正文；除非它本身就是用户必须配置或调用的公开契约。
5. **按固定结构聚类**：同主题 PR 合并为一个能力点，归属两个 DSL 模块（Compose 相关改动 → Compose DSL，其余 → Kuikly DSL）；性能和修复按用户效果概括，不逐条复述 PR。
6. **按 PR 事实决定文档内链**：只有该 PR 的实际变更文件中包含用户文档（`docs/**/*.md` 或 `docs/**/*.mdx`）时，才在对应能力点后提供内链；链接必须指向该 PR 实际修改的文档。只改 `docs/sidebar/zh.ts`、图片或其他配置时不算用户文档；PR 没有修改文档时，禁止因为仓库里存在相关文档而补链。
7. 每条格式：`- **<能力名>（<端>）**：<先写用户效果，再写使用方式/边界；必要时附实际变更文档链接>（#PR）`
8. 补全「性能优化」「Bug 修复」（1~2 句概括 + 指向 changelog）与「升级建议」；无内容的子类整节省略。

例如，#1426 应写成“**重复页面检测**：多模块工程中同名 `@Page` 页面会在编译阶段直接失败，帮助尽早发现重复页面，避免运行时路由覆盖（#1426）”，不要写 `@KuiklyModulePages`、KSP、注册表扫描等机制细节；该 PR 没有修改 `docs/**/*.md`，因此不提供多模块文档内链。

### Step 3 — 判定 breaking.md（业务可感知的契约变化）

以完整的 `<prevTag>..<tag>` compare commit 为扫描范围，同时参考 GitHub Release 的 PR 列表；不要因为某个 commit 没进入 `What's Changed` 就跳过 breaking 扫描。先锁定实际变更位置，再判断业务写 Page 时是否会受到影响。实现层变化可以作为扫描证据，但不能单独成为 breaking 条目。

1. **锁定变更位置**：先用 `git log --all --format='%H %s' | grep -E "\(#<PR号>\)$"` 精确匹配该 PR 的 commit（不要只用 `--grep`，会命中正文引用该 PR 号的其他 commit），再用 `git show <hash> --stat` 与 diff 定位文件和符号；无法定位到代码的结论不许写
2. **六维度扫描**：API 签名变更/删除、默认行为变化、路由/@Page/注解契约、宿主头文件导出、构建开关/KSP/Gradle 默认值、废弃转删除
3. **业务视角过滤**：只收录影响属性、事件、内置组件、内置 Module、页面注解等 Page 编写契约的破坏性；Native 机制、core 内部、Compose 内部机制的优化不外露
4. 有命中：写「变化 / 触发条件 / 迁移 / 证据（PR + 代码位置）」；无命中：写「本版无破坏性变更」+ 扫描依据

### Step 4 — 更新总览与侧边栏（机械）

1. 在 `docs/ChangeLog/README.md` 的版本列表顶部插入本版三链接（新版本在最上）。
2. 在 `docs/sidebar/zh.ts` 的 `/ChangeLog` 版本列表中插入本版**抽屉项**（新版本在最上）：

```ts
{
    text: "<version>",
    collapsible: true,
    expanded: false,
    children: [
        { text: "版本说明", link: "/ChangeLog/<version>/announcement.md" },
        { text: "变更汇总", link: "/ChangeLog/<version>/changelog.md" },
        { text: "破坏性与迁移", link: "/ChangeLog/<version>/breaking.md" },
    ],
}
```

抽屉默认收起，用户点击版本号展开后可分别进入三份文档。

### Step 5 — 格式自检

对照 `docs/ChangeLog/TEMPLATE.md` 逐项检查后再交付：

- changelog：三大块顺序（多端 → 单端 Android/iOS/鸿蒙/H5/小程序 → 其他）；条目格式 `- <类型前缀>: <标题> · [#PR](URL)`；空块空端不出节；英文标题原文、无作者信息
- announcement：两个 DSL 模块 + 能力增加 / 性能优化 / Bug 修复 + 升级建议；能力点结果先行、没有实现层堆砌、无 `<!-- -->` 残留；每个文档内链都能在对应 PR 的 `docs/**/*.md(x)` 改动中找到依据
- breaking：业务视角结论 + 扫描依据；无 `<!-- -->` 残留；纯机制变化不写成 breaking
- 符号无「待核」残留；文档链接不指向未被该 PR 修改的页面

---

## 区间生成（多版本）

当用户说"生成 2.24.0 到 2.26.0 的变更说明"这类**区间**需求时：

1. 先从远端 tag 列表确定区间内**所有正式版本**（如 2.24.0、2.25.0、2.26.0；跳过 beta / SNAPSHOT 等非正式 tag）
2. 对每个版本依次执行本 skill 的完整流程（Step 1 ~ Step 5）
3. 全部完成后统一更新一次 README 与 sidebar 的版本列表（按版本倒序插入）

不要只生成区间两端的版本，也不要跨版本合并成一份文档。

## 格式契约

三份文件的格式与生成规则以 **`docs/ChangeLog/TEMPLATE.md`** 为准，脚本与 AI 遵循同一契约，改契约只改 TEMPLATE。

## Guardrails

- changelog 原文照搬：不翻译、不改写、不补全（原文截断的保留截断符），去掉作者信息
- announcement 必须结果先行，回答“用户能做什么 / 怎么用 / 有什么效果”；禁止把实现机制当成能力叙述
- announcement 文档内链必须有 PR 实际修改 `docs/**/*.md(x)` 的证据；无证据不提供链接
- breaking 只收录业务可感知的破坏性
- 符号必须能定位，定位不到标「待核」，交付前清零
- 禁止把未合入主干的 demo 分支写成「框架已支持」
- 不做 `--no-fetch` 之外的手工 git 状态修改；不自动提交（commit）任何改动
