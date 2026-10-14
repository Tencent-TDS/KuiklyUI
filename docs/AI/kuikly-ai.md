---
title: Kuikly AI 基础工具
category: AI编程
tag:
  - Rules
  - Skills
  - Harness
---

# Kuikly AI 基础工具

> **开源仓库地址**：[https://github.com/Tencent-TDS/KuiklyUI-AI](https://github.com/Tencent-TDS/KuiklyUI-AI)

## 为什么需要 Kuikly AI Harness 工程？

大语言模型为客户端编程带来了极大的生产力红利，但在实际业务开发中，开发者往往面临以下核心挑战：

1. **框架知识断层与幻觉（Hallucinations）**：面对自研响应式机制及模块通信规范时，AI 经常凭经验臆造不存在的 API 或引入错误的第三方依赖。
2. **多端运行环境约束强**：Kuikly 代码需在 Android、iOS、鸿蒙、Web、小程序等多端编译运行。任何平台相关的私有调用、生命周期错位、线程调度不当都会导致编译中断或运行时崩溃。
3. **架构规范难以统一**：在无规则约束的即兴提示词（Vibe Coding）下，不同开发者生成的代码风格混乱，分层模糊，甚至违背团队已有的 Bridge 封装与组件复用约定。

针对以上问题，腾讯 TDS Kuikly 团队开源了 **KuiklyUI-AI**，为开发者构筑起 **Harness Engineering（约束与验证工程体系）**，将框架的最佳实践、API 规格与架构守则转化为 AI 原生可读的 **Rules（规范）** 与 **Skills（技能包）**。

---

## 规范体系：Kuikly Rules

Rules 采用标准 `.mdc` 规范书写，由 AI 编码工具在对话或代码生成时作为项目级上下文自动加载。

| 规则文件 | 适用场景 | 核心覆盖范围 |
| :--- | :--- | :--- |
| **`kuiklyDSL.mdc`** | Kuikly 原生声明式 DSL | 页面容器、`ViewBuilder` 树形结构、响应式变量（`vfor` / `vif` / `vbind`）、事件监听、样式布局与内存规范。 |
| **`kuiklyComposeDSL.mdc`** | Kuikly Compose DSL | 标准 Compose 兼容语法、组件差异处理、重组机制（Recomposition）优化及状态管理最佳实践。 |

### 如何配置使用 Rules

#### 1. 腾讯 CodeBuddy
在项目根目录创建 `.codebuddy/rules/`，并将对应 `.mdc` 文件复制进去：
```text
your-project/
└── .codebuddy/
    └── rules/
        ├── kuiklyDSL.mdc
        └── kuiklyComposeDSL.mdc
```
CodeBuddy 将在每次代码生成与审查时自动匹配并执行该规则。

#### 2. Cursor
直接将规则文件放入 `.cursor/rules/` 目录下（原生支持 `.mdc` 前置元数据）：
```text
your-project/
└── .cursor/
    └── rules/
        └── kuiklyDSL.mdc
```

#### 3. Claude Code / Windsurf / 其他工具
* **Claude Code**：将规则内容拼接到项目根目录的 `CLAUDE.md` 中，或在对话中使用 `@rules/kuiklyDSL.mdc` 引用。
* **Windsurf / 通用助手**：将规则主体作为 System Prompt 或 Custom Instructions 导入即可。

> **进阶提示**：团队可在官方规则后追加团队内部约定（如专用 Bridge 命名、内部 UI 基础库导入规范、网络公共代理拦截等），使 AI 更加深度地契合团队工程标准。

---

## 场景技能包：Kuikly Skills

Skills 是面向特定开发场景的可执行专家级知识包。当开发者在会话中执行诸如“添加一个弹性动画”、“写一个跨端扩展 View”或“优化 Compose 重组耗时”等任务时，AI 工具将主动激活对应技能，分步引导完成。

官方已开放的 Skills 矩阵如下：

| Skill 名称 | 功能定位与使用场景 |
| :--- | :--- |
| **`kuikly-ui-framework`** | **UI 框架基础能力**：指导使用基础组件（`View`、`Text`、`Button`、`List`、`Image`）、内置 Module（`Router`、`Network`、`SP`）、Flexbox 布局与路由导航。 |
| **`kuikly-animation`** | **动画效果构建**：包含声明式动画（`animate` + 响应式变量驱动）与命令式动画（`animateToAttr` + `ViewRef`），精准控制透明度、平移、缩放与尺寸过渡。 |
| **`kuikly-expand-view`** | **原生 UI 扩展开发**：指导如何把平台原生 View（Android View / iOS UIView / 鸿蒙 ArkUI Component）封装并透传给 Kuikly 端使用。 |
| **`kuikly-expand-api`** | **原生 API 扩展开发**：跨平台自定义 Module 开发，实现业务 Native 功能注入以及 Kuikly 与 Native 宿主之间的双向通信。 |
| **`kuikly-network-and-json`** | **网络请求与数据序列化**：指导使用 `NetworkModule` 处理 HTTP 请求、JSON 序列化解析以及二进制流上传下载。 |
| **`kuikly-reactive-observer`** | **响应式状态管理**：规范使用响应式指令（`vfor`、`vif`、`vbind`）与依赖追踪机制，实现精准高效的细粒度 UI 局部刷新。 |
| **`kuikly-coroutines-threading`** | **协程与异步线程调度**：Kuikly 内置轻量协程、`kotlinx.coroutines` 与 `kuiklyx` 库的最佳并发实践，规范后台任务与 UI 主线程更新。 |
| **`kuikly-assets-resource`** | **多端静态资源管理**：指导本地图片、字体、国际化资源的打包命名规则与 `ImageUri` 跨平台加载方式。 |
| **`kuikly-visibility-exposure`** | **节点曝光与可见性监听**：标准化实现 `didAppear`、`didDisappear`、`willAppear`、`willDisappear` 与 `appearPercentage` 曝光上报机制。 |
| **`kuikly-multi-module-config`** | **大型多模块工程治理**：多 Module Kuikly 工程配置规范，避免 `KuiklyCoreEntry` 冲突并实现业务代码解耦。 |
| **`kuikly-compose-interop-dsl`** | **双 DSL 互操作**：在 Compose DSL 页面中无缝嵌入传统 Kuikly DSL 组件（`DeclarativeBaseView` / `ViewContainer`）或调用传统 Module。 |
| **`kuikly-recomposition-analyzer`** | **Compose 重组性能分析**：配合 Recomposition Profiler 诊断重组瓶颈，分析 `profiler_report.json` 并输出性能调优建议。 |

---

## 安装与快速更新

Kuikly Skills 遵循现代 Agent Skills 规范分发，推荐使用标准命令行工具进行一键安装与升级：

### 1. 全局安装 Skills
```bash
npx skills add Tencent-TDS/KuiklyUI-AI/skills
```

### 2. 同步与持续更新
随着 Kuikly 框架迭代，Skills 会持续增加新的特性与补丁。只需在工程目录执行：
```bash
npx skills update Tencent-TDS/KuiklyUI-AI/skills
```

---

## 和全链路的关系

知识库、Rules 和 Skills 处在生产线的前两段：需求规划时提供规范，代码生成时约束写法。视觉稿转码、预览、转码 Agent 和开发 Agent 的整体介绍见 [Kuikly AI](./index.md)。
