# Kuikly Compose 文档结构规划

## 一级结构（5个主要分类）

参考 DevGuide 的结构和主流框架的文档组织方式，采用更宏观的一级分类：

```
/Compose
├── 入门
│   ├── overview.md (已存在：概述)
│   ├── getting-started.md (已存在：快速开始)
│   ├── concepts.md (已存在：核心概念)
│   └── architecture.md (新增：架构详解)
│
├── 开发指南
│   ├── layout.md (从 layout-components.md 拆分：布局)
│   ├── components.md (从 layout-components.md 拆分：组件)
│   ├── state-management.md (新增：状态管理)
│   ├── lists-and-scrolling.md (新增：列表与滚动)
│   ├── modifier.md (新增：Modifier)
│   ├── animation.md (新增：动画)
│   ├── gestures.md (新增：手势)
│   ├── performance.md (新增：性能优化)
│   └── best-practices.md (新增：最佳实践)
│
├── 集成与扩展
│   ├── interop-core.md (已存在，优化：与 Core 集成)
│   ├── using-modules.md (新增：使用 Module)
│   ├── using-router.md (新增：使用 Router)
│   ├── dynamic-ui.md (新增：动态化 UI)
│   ├── extend-native-overview.md (新增：扩展原生组件概述)
│   ├── extend-native-android.md (新增：Android 端扩展)
│   ├── extend-native-ios.md (新增：iOS 端扩展)
│   ├── extend-native-web.md (新增：Web 端扩展)
│   └── extend-native-harmony.md (新增：HarmonyOS 端扩展)
│
├── 多端与迁移
│   ├── multiplatform.md (已存在，优化：多端开发)
│   ├── platform-differences.md (新增：平台差异)
│   ├── platform-specific.md (新增：平台特定能力)
│   ├── for-android-devs.md (新增：Android 开发者指南)
│   ├── for-ios-devs.md (新增：iOS 开发者指南)
│   ├── for-web-devs.md (新增：Web 开发者指南)
│   ├── migration-from-jetpack.md (新增：从 Jetpack Compose 迁移)
│   ├── migration-from-dsl.md (新增：从自研 DSL 迁移)
│   └── compatibility.md (新增：兼容性说明)
│
└── 参考
    ├── components-list.md (新增：组件列表)
    ├── modifier-list.md (新增：Modifier 列表)
    ├── api-differences.md (新增：API 差异)
    ├── official-compose-links.md (已存在：官方文档链接)
    ├── examples.md (新增：示例代码)
    └── faq.md (新增：常见问题)
```

## 详细说明

### 1. 入门（4篇）
- **overview.md** (已存在)：概述，移除 beta 字眼
- **getting-started.md** (已存在)：快速开始，移除 beta 字眼
- **concepts.md** (已存在)：核心概念
- **architecture.md** (新增)：
  - 详细架构图（Compose Runtime → KuiklyApplier → Core → Render）
  - KuiklyApplier 工作原理
  - ComposeContainer 生命周期
  - 渲染流程详解

### 2. 开发指南（9篇）
- **layout.md** (从 layout-components.md 拆分)：
  - Column/Row/Box 等布局组件
  - 布局测量与放置规则
  - 跨端布局注意事项
  
- **components.md** (从 layout-components.md 拆分)：
  - Material3 组件列表
  - 组件使用示例
  - 组件差异说明
  
- **state-management.md** (新增)：
  - remember、mutableStateOf 使用
  - 状态提升模式
  - 副作用处理（LaunchedEffect 等）
  - 与 Jetpack Compose 的差异
  
- **lists-and-scrolling.md** (新增)：
  - LazyColumn/LazyRow 使用
  - 列表性能优化
  - 滚动事件处理
  - 分页、回弹等能力
  
- **modifier.md** (新增)：
  - 常用 Modifier 列表
  - Modifier 链式组合
  - 自定义 Modifier

- **animation.md** (新增)：
  - AnimatedVisibility、AnimatedContent
  - Transition 动画
  - 跨端动画差异
  
- **gestures.md** (新增)：
  - 手势处理
  - 拖拽、滑动等
  
- **performance.md** (新增)：
  - 重组优化
  - 列表性能
  - 内存管理
  
- **best-practices.md** (新增)：
  - 代码组织
  - 组件设计
  - 状态管理最佳实践

### 3. 集成与扩展（9篇）
- **interop-core.md** (已存在，优化)：
  - 保持现有内容，补充更多示例
  
- **using-modules.md** (新增)：
  - 如何在 Compose 中使用 Network、Storage 等 Module
  - 示例代码
  
- **using-router.md** (新增)：
  - 路由跳转
  - 参数传递
  - 页面返回
  
- **dynamic-ui.md** (新增)：
  - 动态化 UI 实现
  - 服务端驱动 UI
  - 配置化页面

#### 3.1 与 Core 集成（4篇）
- **extend-native-overview.md** (新增)：
  - 扩展原生组件的整体思路
  - 架构说明
  - 与自研 DSL 扩展的对比
  
- **extend-native-android.md** (新增)：
  - Android 端如何扩展原生组件
  - 示例：扩展一个自定义 View
  - 属性映射、事件映射
  
- **extend-native-ios.md** (新增)：
  - iOS 端如何扩展原生组件
  - 示例：扩展一个自定义 UIView
  
- **extend-native-web.md** (新增)：
  - Web 端如何扩展原生组件
  - 示例：扩展一个自定义 DOM 元素
  
- **extend-native-harmony.md** (新增)：
  - HarmonyOS 端如何扩展原生组件
  - 示例：扩展一个自定义 ArkUI 组件

#### 3.2 扩展原生组件（5篇）⭐ 重点
- **extend-native-overview.md** (新增)：
  - 扩展原生组件的整体思路
  - 架构说明
  - 与自研 DSL 扩展的对比
  
- **extend-native-android.md** (新增)：
  - Android 端如何扩展原生组件
  - 示例：扩展一个自定义 View
  - 属性映射、事件映射
  
- **extend-native-ios.md** (新增)：
  - iOS 端如何扩展原生组件
  - 示例：扩展一个自定义 UIView
  
- **extend-native-web.md** (新增)：
  - Web 端如何扩展原生组件
  - 示例：扩展一个自定义 DOM 元素
  
- **extend-native-harmony.md** (新增)：
  - HarmonyOS 端如何扩展原生组件
  - 示例：扩展一个自定义 ArkUI 组件

### 4. 多端与迁移（9篇）
- **multiplatform.md** (已存在，优化)：
  - 保持现有内容，补充更多细节
  
- **platform-differences.md** (新增)：
  - 详细的平台差异清单
  - 按组件/能力维度分类
  - 已知问题与规避建议
  
- **platform-specific.md** (新增)：
  - 各平台特定能力使用
  - 平台条件编译
  - 平台特定 API 调用

#### 4.1 多端开发（3篇）
- **multiplatform.md** (已存在，优化)：
  - 保持现有内容，补充更多细节
  
- **platform-differences.md** (新增)：
  - 详细的平台差异清单
  - 按组件/能力维度分类
  - 已知问题与规避建议
  
- **platform-specific.md** (新增)：
  - 各平台特定能力使用
  - 平台条件编译
  - 平台特定 API 调用

#### 4.2 开发者角色指南（3篇）⭐ 重点
- **for-android-devs.md** (新增)：
  - Android 开发者快速上手
  - 与 Jetpack Compose 的对比
  - 迁移路径
  
- **for-ios-devs.md** (新增)：
  - iOS 开发者快速上手
  - SwiftUI 对比
  - 常见概念映射
  
- **for-web-devs.md** (新增)：
  - Web 开发者快速上手
  - React/Vue 对比
  - 前端概念映射

#### 4.3 迁移指南（3篇）
- **migration-from-jetpack.md** (新增)：
  - 从 Jetpack Compose 迁移到 Kuikly Compose
  - 包名替换清单
  - 代码迁移步骤
  - 常见问题
  
- **migration-from-dsl.md** (新增)：
  - 从自研 DSL 迁移到 Compose
  - 迁移策略
  - 混用方案
  
- **compatibility.md** (新增)：
  - 与 Jetpack Compose 的兼容性说明
  - 版本对应关系
  - 不兼容的 API 清单

### 5. 参考（6篇）
- **components-list.md** (新增)：
  - 所有支持的组件列表
  - 快速索引
  - 链接到详细文档
  
- **modifier-list.md** (新增)：
  - 所有支持的 Modifier 列表
  - 快速索引
  
- **api-differences.md** (新增)：
  - 与官方 Jetpack Compose API 的差异清单
  - 不支持的 API
  - 行为差异说明

- **components-list.md** (新增)：
  - 所有支持的组件列表
  - 快速索引
  - 链接到详细文档
  
- **modifier-list.md** (新增)：
  - 所有支持的 Modifier 列表
  - 快速索引
  
- **api-differences.md** (新增)：
  - 与官方 Jetpack Compose API 的差异清单
  - 不支持的 API
  - 行为差异说明

- **official-compose-links.md** (已存在)：
  - 保持现状
  
- **examples.md** (新增)：
  - 示例代码集合
  - 常见场景示例
  - 链接到 demo 项目
  
- **faq.md** (新增)：
  - 常见问题解答
  - 故障排查

## 侧边栏配置建议

```typescript
"/Compose": [
    {
        text: "入门",
        prefix: "/Compose",
        collapsible: true,
        children: [
            "overview.md",
            "getting-started.md",
            "concepts.md",
            "architecture.md"
        ]
    },
    {
        text: "开发指南",
        prefix: "/Compose",
        collapsible: true,
        children: [
            "layout.md",
            "components.md",
            "state-management.md",
            "lists-and-scrolling.md",
            "modifier.md",
            "animation.md",
            "gestures.md",
            "performance.md",
            "best-practices.md"
        ]
    },
    {
        text: "集成与扩展",
        prefix: "/Compose",
        collapsible: true,
        children: [
            "interop-core.md",
            "using-modules.md",
            "using-router.md",
            "dynamic-ui.md",
            "extend-native-overview.md",
            "extend-native-android.md",
            "extend-native-ios.md",
            "extend-native-web.md",
            "extend-native-harmony.md"
        ]
    },
    {
        text: "多端与迁移",
        prefix: "/Compose",
        collapsible: true,
        children: [
            "multiplatform.md",
            "platform-differences.md",
            "platform-specific.md",
            "for-android-devs.md",
            "for-ios-devs.md",
            "for-web-devs.md",
            "migration-from-jetpack.md",
            "migration-from-dsl.md",
            "compatibility.md"
        ]
    },
    {
        text: "参考",
        prefix: "/Compose",
        collapsible: true,
        children: [
            "components-list.md",
            "modifier-list.md",
            "api-differences.md",
            "official-compose-links.md",
            "examples.md",
            "faq.md"
        ]
    }
]
```

## 优先级建议

### 第一阶段（核心文档）
1. ✅ 入门（已存在，只需优化）
2. ⭐ 集成与扩展 → 扩展原生组件（重点，用户最关心）
3. ⭐ 多端与迁移 → 开发者角色指南（重点，降低上手门槛）
4. 开发指南 → 基础部分（拆分现有 layout-components.md）

### 第二阶段（完善内容）
1. 进阶指南
2. 与 Core 集成（优化现有）
3. 多端开发（优化现有）
4. 迁移指南

### 第三阶段（补充细节）
1. API 参考
2. 资源与参考
3. FAQ

## 注意事项

1. **移除所有 beta 字眼**：所有文档中不再出现 beta、实验性等字眼
2. **保持与官方文档的链接**：在相关章节顶部提供 Jetpack Compose 官方文档链接
3. **示例代码**：每个章节都要有实际可运行的示例代码
4. **跨端差异标注**：明确标注哪些能力在哪些平台有差异
5. **渐进式学习路径**：从概述 → 快速开始 → 基础 → 进阶，形成清晰的学习路径

