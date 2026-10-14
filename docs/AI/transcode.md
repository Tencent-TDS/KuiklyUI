---
title: 其他框架转码 Kuikly
category: AI编程
tag:
  - 转码
  - React2Kuikly
  - Vue2Kuikly
  - 迁移
---

# 其他框架转码 Kuikly

> **开源项目**：
> * **React2Kuikly**：[https://github.com/Tencent-TDS/React2Kuikly](https://github.com/Tencent-TDS/React2Kuikly)
> * **Vue2Kuikly**：[https://github.com/Tencent-TDS/Vue2Kuikly](https://github.com/Tencent-TDS/Vue2Kuikly)

随着 Kuikly 在腾讯多条大型业务线（如手 Q 证券、QQ 音乐、搜狗输入法、腾讯视频等）的全面铺开，大量存量历史页面（React、React Native、Vue 2、Vue 3、Hippy）面临向 Kuikly 跨端架构迁移的需求。

如果全靠人工“人肉改写”，单个复杂页面通常需要 3~5 天的开发与排期，且极易遗漏边角业务逻辑。Kuikly 团队研发了**基于 AI Agent 的智能转码工作流与工具链**，实现存量代码向 Kuikly Kotlin 代码的高效、高保真迁移。

转码来源不只有 React、Vue 和 Hippy。安卓原生界面、iOS 原生界面和 HTML 页面也可以转成 Kuikly。

---

## 核心设计理念：主从分离与插拔式架构

在大型工程转码中，纯粹通用的规则往往无法满足业务个性化的要求（例如业务方有自研的全局网络库、特有的 JSBridge、统一的 Design Token 规范）。为此，Kuikly 转码体系创新性地提出了 **「主从分离、接口约定、动态发现」** 的架构：

```
                      ┌─────────────────────────────────┐
                      │    AI Agent 转码协调器          │
                      └─────────────────────────────────┘
                                       │
                ┌──────────────────────┴──────────────────────┐
                ▼                                             ▼
  ┌───────────────────────────┐                 ┌───────────────────────────┐
  │     主 Skill (框架通用)    │                 │    从 Skill (业务注入)    │
  │ (如 react2kuikly.zip /   │                 │ (如 react2kuikly-config-  │
  │  vue3tokuikly.zip)        │                 │  skill.zip)               │
  ├───────────────────────────┤                 ├───────────────────────────┤
  │ • 语法树 AST 解析与映射    │ ◄──PATH_CONFIG──┤ • 业务专有 JSBridge 规则  │
  │ • 标准生命周期对齐转换    │   动态契约协议   │ • 内部公共组件库映射      │
  │ • 布局与响应式语义迁移    │                 │ • 资源重命名与业务校验项  │
  │ • 零业务硬编码            │                 │ • 团队独有架构分层约定    │
  └───────────────────────────┘                 └───────────────────────────┘
```

1. **主 Skill 只做通用翻译**：负责 React/Vue 到 Kuikly 的通用语法、状态机、模板与生命周期转换，不掺杂任何特定业务假设。
2. **业务 Skill 外置业务差异**：业务方通过 `PATH_CONFIG` 协议声明自己的规则目录、专属组件对齐表和 Checklist，主 Skill 在运行时动态发现并加载，实现**零硬编码、即插即用**。

---

## 六步迁移流水线

为确保“零遗漏、零创造、编译闭环”，转码流水线严格按照六个阶段有序推进：

```
 ┌───────────────┐     ┌───────────────┐     ┌───────────────┐
 │ 1. 读源项目   │ ──► │ 2. 语义分析   │ ──► │ 3. 知识库加载 │
 │    建立全量清单│     │    依赖拓扑分层│     │    主从规则注入│
 └───────────────┘     └───────────────┘     └───────────────┘
                                                     │
 ┌───────────────┐     ┌───────────────┐             │
 │ 6. 验证修复   │ ◄── │ 5. 资源同步   │ ◄─── 4. 逐文件迁移 ──┘
 │    编译对齐闭环│     │    多端 Assets │     先叶子节点再入口
 └───────────────┘     └───────────────┘
```

1. **读源项目（Inventory Scan）**：
   扫描源工程的入口页面、组件引用拓扑、Pinia/Vuex/Redux 状态、样式表、静态资源及路由配置，生成精确的源项目要素清单。
2. **分析语义与依赖（Semantic & Dependency Analysis）**：
   拒绝凭文件名盲猜，深入分析 `props`、`computed`/`watch`/`hooks`、事件冒泡、条件渲染、列表绑定等真实语义，并按依赖拓扑进行分层排序。
3. **加载转码知识（Knowledge Injection）**：
   载入 Kuikly 官方 Rules 与业务定制从 Skill，建立源框架与目标 Kotlin 语法的对齐字典。
4. **逐文件迁移（Bottom-Up Generation）**：
   按依赖关系**自底向上**推进：**先转数据模型与通用工具 ➔ 再转叶子展示组件 ➔ 接着转复合容器 ➔ 最后转页面入口**。每个关键逻辑均保留 `// Source:` 注释，确保业务逻辑百分之百可追溯。
5. **资源同步（Asset Alignment）**：
   自动识别源项目中的图片、SVG 图标与本地字体，迁移至目标 Kuikly 工程的 `assets/` 目录，并自动生成跨端资源引用常量。
6. **验证修复与编译闭环（Compiler Loop）**：
   驱动编译器进行严格的静态检查。若出现类型不匹配或方法缺失，AI 严格基于目标工程已存在的 API 修复，杜绝臆造 API。

---

## 真实测试效果与评测

在包含 Vue 2、Vue 3 及 React Native 的多个代表性真实项目评测中，转码流水线展现出了极高的稳定度与还原度：

### 1. Vue 3 转码 Kuikly 测评
* **测试环境**：`vue3tokuikly` 主 Skill + `vue3tokuikly-config-skill` 业务 Skill，运行于 Codex + GPT-5.5。
* **效果展现**：Composition API（`ref`、`reactive`、`computed`）被精准映射至 Kuikly 响应式指令与 Compose 状态，复杂列表与条件渲染一次性通过编译。

![Vue3转码Kuikly效果评测](./img/vue3_transcode_demo.png)

### 2. Vue 2 存量移动端项目转码测评
* **测试环境**：开源移动端项目 `Vue2-Mobile`，采用 `vue2kuikly` Skill，运行于 Claude Opus 4.7（1M 上下文）与 GPT-5.5。
* **效果展现**：将 Options API、Vue Router 及 Axios 请求转换为 Kuikly DSL 页面、Router 模块和 Network 模块。

| 转换前：Vue 2 源项目页面 | 转换后：Kuikly 原生渲染效果 |
| :---: | :---: |
| ![Vue2源工程界面](./img/vue2_origin_demo.png) | ![Vue2转码后Kuikly界面](./img/vue2_transcode_demo.png) |

> 经过单次端到端转码，Kuikly 渲染的卡片布局、圆角、阴影、字体层级以及多 Tab 切换均与源 Vue 页面达到 **95% 以上的一致性**。

### 3. React / React Native 转码测评
* **测试环境**：复杂业务 RN 移动端工程，采用 `react2kuikly` 主从 Skill 架构，运行于 Codex + GPT-5.5（高思考模式）。
* **效果展现**：React Hooks（`useState`、`useEffect`、`useCallback`、`useMemo`）被优雅平移为 Kotlin 状态变量与生命周期方法，Flexbox 样式与原生手势交互完整映射。

![React转码Kuikly效果评测](./img/react_transcode_demo.png)

---

## 效能收益总结

综合实际业务工程落地与基准测评数据：

* **首轮有效率 80%+ ~ 95%**：生成的 Kotlin 页面骨架结构完整，布局与逻辑大多能直接运行。
* **开发提效 3 倍以上**：单页面重构平均耗时从过去的 **3 天缩减至 1 天以内**。
* **人工介入聚焦核心交互**：开发人员不再需要耗费精力编写繁复的基础排版与字段绑定代码，只需将精力集中在端侧原生 Bridge 校准和真机差异化测试上。
