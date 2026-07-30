# 核心组件模块 (core)

## 概述

本目录包含 KuiklyTableUI 表格组件的核心实现文件，是整个表格组件的心脏。

## 文件说明

| 文件 | 说明 |
|------|------|
| `KuiklyTableView.kt` | 表格主组件，继承 `ComposeView<TableAttr, TableEvent>`，实现表格的完整渲染逻辑 |
| `TableAttr.kt` | 表格属性定义，继承 `ComposeAttr`，包含列定义、行数据、主题、固定列等配置 |
| `TableEvent.kt` | 表格事件定义，继承 `ComposeEvent`，支持单元格点击、行点击、表头点击、滚动事件 |

## 架构

```
ComposeView<TableAttr, TableEvent>
    └── KuiklyTableView
            ├── createAttr() → TableAttr     # 属性初始化
            ├── createEvent() → TableEvent   # 事件初始化
            └── body() → ViewBuilder         # UI 渲染
                    ├── renderNormalLayout()         # 普通布局（所有列一起滚动）
                    └── renderFixedColumnsLayout()   # 固定列布局（左固定 + 右滚动）
```

## 渲染模式

### 普通布局 (renderNormalLayout)
- 外层 Scroller（横向滚动）
  - 内容容器 View（纵向布局）
    - 表头行
    - 内层 Scroller（纵向滚动）
      - 表体行

### 固定列布局 (renderFixedColumnsLayout)
- 外层 View（横向布局）
  - 左侧固定面板
    - 固定列表头
    - 同步滚动 Scroller（含 fixedColumns 列数据）
  - 右侧滚动面板
    - 滚列表头
    - 同步滚动 Scroller（含 scrollableColumns 列数据）

## 边框实现

由于 Kuikly 框架中 `borderBottom`/`borderRight` 等单侧边框方法在某些版本不可用，本组件使用 `addBorderBottom()`/`addBorderRight()` 辅助函数，通过绝对定位 View + border 实现单侧描边效果。

## 日志

渲染过程通过 `TableLog` 记录关键事件：
- 渲染开始/完成
- 渲染异常（含完整堆栈）
- 异常时自动显示错误提示 UI

## 相关模块

- [model/](../model/) — 数据模型定义
- [dsl/](../dsl/) — DSL 语法入口
- [log/](../log/) — 日志系统
- [demo/](../demo/) — 演示页面
