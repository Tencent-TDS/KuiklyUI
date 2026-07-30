# 演示页面模块 (demo)

## 概述

本目录包含表格组件的演示页面 `TableDemoPage`，展示组件的各种功能和配置。

## 页面注册

```kotlin
@Page("tableDemo")
class TableDemoPage : Pager()
```

应用启动时默认加载此页面（通过 `KuiklyRenderActivity` 中的 `DEFAULT_PAGE = "tableDemo"`）。

## 演示示例

| Demo | 标题 | 展示功能 |
|------|------|----------|
| 1 | 基础表格 | 多行多列数据展示、斑马纹 |
| 2 | 蓝色主题 | 自定义主题预设、文本对齐 |
| 3 | 深色主题 | TableTheme.DARK 预设 |
| 4 | 大数据量滚动 | 10列 × 50行、横纵双向滚动 |
| 5 | 固定首列 | 固定列 + 滚动联动 |
| 6 | 自定义单元格渲染 | 状态徽章、星星评分 |
| 7 | 事件处理 | 单元格/行/表头点击回调 |
| 8 | 紧凑风格无表头 | headerVisible(false) |
| 9 | 自定义主题 DSL | 紫色主题通过 DSL 构建 |

## 页面结构

```
Scroller (纵向滚动)
├── demoSection("Demo 1: 基础表格")
│   └── Table { ... }
├── demoSection("Demo 2: 蓝色主题")
│   └── Table { ... }
├── ...
└── demoSection("Demo 9: 自定义主题 DSL")
    └── Table { ... }
```

每个 `demoSection` 包含一个标题 Text 和一个 Table 组件，Table 自动占满宽度。

## 相关模块

- [core/](../) — 核心组件
- [dsl/](../dsl/) — DSL 入口
- [model/](../model/) — 数据模型
