---
name: kuikly-changelog-gen
description: 生成指定版本的三份变更文档（版本说明 / 变更汇总 / 破坏性与迁移）并落到 docs/ChangeLog/<version>/。当用户说"生成 X.Y.Z 的变更说明"、"生成 2.28.0 的三分文档"、"写一下这个版本的更新日志"时触发。脚本负责机械部分（拉 tag、分桶、changelog 完整生成），AI 负责语义部分（announcement 能力点描述、breaking 破坏性判定），格式契约见 docs/ChangeLog/TEMPLATE.md。
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
python3 docs/ChangeLog/generate.py <version>
```

脚本行为：

- 用远端完整 tag 列表确定版本区间（`<prevTag>..<version>`），本地缺失时自动补齐前后两个 tag（upstream → origin → 官方 GitHub）
- 收集区间 commit（subject + 改动路径），按规则分桶：多端 / 单端（Android → iOS → 鸿蒙 → H5 → 小程序）/ 其他
- **完整生成 `changelog.md`**（原文照搬，含 revert 去重）
- 生成 `announcement.md` / `breaking.md` **骨架**（含 `<!-- 待补全 -->` 标记与高风险 commit 清单）
- 写入 `docs/ChangeLog/<version>/`（目录不存在则创建，文件已存在则覆盖）

### Step 2 — 补全 announcement.md（AI 语义部分）

读取骨架中的 `<!-- 改动路径 -->` 注释与 feat 条目，按 TEMPLATE 第 3 步规则处理：

- 按能力点聚类（同主题合并为一个能力点，不逐条罗列 PR）
- 每条格式：`- **<能力名>（<端>）**：<1~2 句业务视角说明>（#PR）`
- 归属两个 DSL 模块：Compose 相关改动 → Compose DSL，其余 → Kuikly DSL
- 补全「性能优化」「Bug 修复」（1~2 句概括 + 指向 changelog）与「升级建议」；无内容的子类整节省略
- 关键符号需在代码中核实（可定位），定位不到标「待核」

### Step 3 — 判定 breaking.md（AI 语义部分）

对骨架列出的高风险 commit 逐一核对：

- 六维度扫描：API 签名变更/删除、默认行为变化、路由/@Page/注解契约、宿主头文件导出、构建开关/KSP/Gradle 默认值、废弃转删除
- **业务视角过滤**：只收录影响"属性 / 事件 / 内置组件 / 内置 Module / 页面注解"的破坏性；Native 机制、core 内部、Compose 内部机制的优化不外露
- 有命中：写「变化 / 触发条件 / 迁移 / 证据」；无命中：写「本版无破坏性变更」+ 保留扫描依据
- 高风险 commit 的判定必须看代码 diff，不能只看标题

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

- changelog：三大块顺序（多端 → 单端 Android/iOS/鸿蒙/H5/小程序 → 其他）；条目格式 `- <类型前缀>: <标题> · [#PR](URL)`；空块空端不出节
- announcement：两个 DSL 模块 + 能力增加 / 性能优化 / Bug 修复 + 升级建议；无 `<!-- -->` 残留
- breaking：业务视角结论 + 扫描依据；无 `<!-- -->` 残留
- 符号无「待核」残留

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
- announcement 能力点用业务视角描述；breaking 只收录业务可感知的破坏性
- 符号必须能定位，定位不到标「待核」，交付前清零
- 禁止把未合入主干的 demo 分支写成「框架已支持」
- 不做 `--no-fetch` 之外的手工 git 状态修改；不自动提交（commit）任何改动
