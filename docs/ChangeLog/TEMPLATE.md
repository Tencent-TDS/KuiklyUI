# 版本三分文档模板与生成规则

> 本文供作者与自动化 Skill 使用，是生成 `docs/ChangeLog/<version>/` 三份文件的契约，非对外内容。

每个版本在 `docs/ChangeLog/<version>/` 下生成三份文件（`<version>` 与 Git tag 一致），并在总览 [README.md](./README.md) 的版本列表顶部插入本版条目。

## 一、数据来源

- 一级数据源：本地 git 的版本区间 commit（`git log --no-merges <prevTag>..<tag>`，squash merge 的 commit subject 即 What's Changed 原文，含类型前缀与 PR 号）。
- 二级校验：GitHub Release `<tag>` 的 What's Changed 列表（与本地 commit 互相印证）；代码库（凡写入文档的符号必须能在代码中定位，定位不到标「待核」，禁止编造）。

## 二、导出过程

### 第 1 步：分桶（机械规则）

三大块：多端 / 单端 / 其他。

**多端**：改动涉及跨端通用模块或多个端的变更。

- 跨端通用模块：`core/`、`compose/`、`core-ksp/`、`core-annotations/`
- 同时改动 2 个及以上端 render 层的跨端能力（条文里点名涉及端）

**单端**：只影响单端的变更，按 Android → iOS → 鸿蒙 → H5 → 小程序 顺序排列：

| 改动路径 | 端 |
|---|---|
| `core-render-android/`、`androidApp/` | Android |
| `core-render-ios/`、`iosApp/` | iOS |
| `core-render-ohos/`、`ohosApp/` | 鸿蒙 |
| `core-render-web/`、`h5App/` | H5 |
| `miniApp/` | 小程序 |

**其他**：文档（`docs/`）、chore、CI、发版杂项、示例（demo），内部按「文档 / 工程与示例」排列。

空块与空端不出节。

### 第 2 步：changelog.md（机械搬运）

每条照搬 commit subject / What's Changed 原文一行：`<类型前缀>: <标题> · [#<PR号>](<PR URL>)`，按三大块分组输出。标题不翻译、不改写、不补全（原文截断的保留截断符），去掉作者信息，仅把裸 URL 转为 Markdown 链接。

### 第 3 步：announcement.md（两个 DSL 模块 + 聚类成文）

固定结构（顺序固定，有内容才出节）：

| 模块 | 子类 | 收纳范围 |
|---|---|---|
| Kuikly DSL | 能力增加 | 内置组件 / 内置 Module / 属性 / 事件 / 注解 / 宿主配置项 |
| Kuikly DSL | 性能优化 | 启动 / 渲染 / 内存优化 |
| Kuikly DSL | Bug 修复 | 修复类，1~2 句概括 + 指向 changelog |
| Compose DSL | 能力增加 | Compose 组件 / API / 工具 |
| Compose DSL | 性能优化 | 重组 / 滚动 / 渲染优化 |
| Compose DSL | Bug 修复 | 修复类，同上 |
| 升级建议 | — | 由 breaking 结论回填 |

写作规则：

1. 「能力增加」用列表：每条 = 粗体能力名 + 1~2 句说明 + PR 号；端专属的在名称后点名端。
2. 「性能优化」「Bug 修复」无内容则整节省略；Bug 修复不逐条列，概括修复范围并指向 changelog。
3. 「升级建议」由 breaking 结论回填；无则写「本版无破坏性变更，可直接升级」。
4. 能力点来源：`feat` 类条目 + 有叙事价值的机制变化；纯内部重构、构建脚本、文档类不进。

### 第 4 步：breaking.md（业务视角扫描）

判定标准：**只看业务写 Page 时能用的能力面**（属性 / 事件 / 内置组件 / 内置 Module / 页面注解）是否发生破坏性变化。Native 机制、core 内部、Compose 内部机制的优化不外露。

扫描动作（保证不漏，对每个 PR 的代码 diff 执行）：

1. API 签名变更 / 删除（对外符号）
2. 默认行为变化（业务可感知）
3. 路由 / @Page / 注解契约
4. 宿主头文件 / 对外 C / ObjC / ArkTS 导出（业务集成会用到的）
5. 构建开关 / KSP / Gradle 选项默认值
6. 废弃转删除

命中且业务可感知：写条目（变化 / 触发条件 / 迁移 / 证据）。
命中但业务不感知（纯机制）：不列为破坏性。
全部未命中：写「本版无破坏性变更」+ 扫描依据（compare 链接、commit 数量、业务能力面核对结果）。
注意：Release 列表不标注破坏性（如 2.27.0 的 #1426），判定必须结合代码 diff。

### 第 5 步：更新总览（机械）

在 [README.md](./README.md) 版本列表顶部插入本版三链接。

## 三、三份文件骨架

### announcement.md

```markdown
# <version> 版本说明

> 本版导航：[版本说明](./announcement.md) · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)

<总述：1 句，本版核心方向；纯修复版写「本版以稳定性修复为主」>

## Kuikly DSL

### 能力增加

- **<能力名>（<端>）**：<1~2 句说明>（#<PR号>）。

### 性能优化

- **<优化名>**：<1~2 句说明>（#<PR号>）。

### Bug 修复

<1~2 句概括修复范围，指向 changelog>

## Compose DSL

### 能力增加

- **<能力名>**：<1~2 句说明>（#<PR号>）。

### 性能优化

- **<优化名>**：<1~2 句说明>（#<PR号>）。

### Bug 修复

<1~2 句概括修复范围，指向 changelog>

## 升级建议

<有需注意项时 1 句 + 指向 breaking；无则写「本版无破坏性变更，可直接升级」>
```

两个 DSL 模块下均无内容的子类整节省略。

### changelog.md

```markdown
# <version> 变更汇总

> 本版导航：[版本说明](./announcement.md) · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)

## 多端

- <类型前缀>: <标题> · [#<PR号>](<PR URL>)

## 单端

### Android

- <同上>

### iOS

- <同上>

### 鸿蒙

- <同上>

### H5

- <同上>

### 小程序

- <同上>

## 其他

### 文档

- <同上>

### 工程与示例

- <同上>
```

三大块固定顺序：多端 → 单端（Android / iOS / 鸿蒙 / H5 / 小程序）→ 其他；空端、空小节整节省略。

### breaking.md

```markdown
# <version> 破坏性与迁移

> 本版导航：[版本说明](./announcement.md) · [变更汇总](./changelog.md) · [破坏性与迁移](./breaking.md)

## 条件性破坏性

### #<PR号> <业务视角一句话结论>

- 变化：<旧行为 → 新行为，用业务能懂的话>
- 触发条件：<什么情况下会命中>
- 迁移：<迁移步骤>
- 证据：#<PR号>

## 扫描依据

<compare 链接> 共 N 个 commit。按业务能力面（属性 / 事件 / 内置组件 / 内置 Module / 页面注解）核对：<结果>。
```

无破坏性版本：省略「条件性破坏性」小节，正文写「本版无破坏性变更」，保留扫描依据。

## 四、生成方式

- 脚本（机械部分）：`python3 docs/ChangeLog/generate.py <version>` —— 拉 tag、分桶、完整生成 changelog.md、生成 announcement/breaking 骨架。
- Skill（AI 调度）：`kuikly-changelog-gen` —— 调用脚本后补全 announcement 的能力点描述、判定 breaking、更新总览、格式自检。

## 五、写作红线

- announcement 与 changelog 相辅相成：announcement 按两个 DSL 模块写叙事概览，changelog 按三大块列全量事实，两者禁止长段重复。
- breaking 只收录业务可感知的破坏性，无则写「无 + 扫描依据」。
- 符号必须能定位，定位不到标「待核」，合入前清零。
- 禁止把未合入主干的 demo 分支写成「框架已支持」。
