# DSL 模块 (dsl)

## 概述

本目录包含表格组件的 DSL（领域特定语言）实现，提供简洁的声明式语法快速创建表格。

## 入口方法

```kotlin
fun ViewContainer<*, *>.Table(init: TableBuilder.() -> Unit)
```

在任意 Kuikly page 的 `body()` 中调用 `Table { ... }` 即可创建表格。

## 构建器类

### TableBuilder
表格构建器，收集 DSL 中的列定义、行数据、主题、事件等配置。

**列方法：**
| 方法 | 说明 |
|------|------|
| `column(key, title, width) { ... }` | 定义单列 |
| `columns(vararg cols)` | 批量传入 ColumnDef |

**数据方法：**
| 方法 | 说明 |
|------|------|
| `row(vararg values)` | 添加一行 |
| `rows(data)` | 批量设置行数据 |
| `data(tableData)` | 从 TableData 填充 |

**配置方法：**
| 方法 | 说明 |
|------|------|
| `theme(theme)` | 设置预设主题 |
| `theme { ... }` | DSL 自定义主题 |
| `headerVisible(bool)` | 表头可见性 |
| `fixedColumns(n)` | 固定列数 |
| `maxHeight(h)` | 最大高度 |
| `scrollEnabled(bool)` | 是否可滚动 |

**事件方法：**
| 方法 | 说明 |
|------|------|
| `onCellClick { row, col, value -> }` | 单元格点击 |
| `onRowClick { row -> }` | 行点击 |
| `onHeaderClick { col, key -> }` | 表头点击 |
| `onScroll { x, y -> }` | 滚动事件 |

### ColumnBuilder
列构建器，在 `column {}` 块内配置列属性。

### ThemeBuilder
主题构建器，在 `theme {}` 块内自定义主题配置。

## 使用示例

```kotlin
import com.kuikly.table.dsl.Table

// 基础用法
Table {
    column("name", "姓名", 120f)
    column("age", "年龄", 80f)
    row("张三", "28")
    row("李四", "32")
}

// 完整配置
Table {
    column("id", "ID", 60f)
    column("name", "姓名", 120f) {
        align(TextAlign.LEFT)
    }
    column("status", "状态", 100f) {
        align(TextAlign.CENTER)
        customRenderer { cellData, _, _ ->
            // 自定义渲染逻辑
        }
    }
    
    row("001", "张三", "在线")
    
    theme(TableTheme.BLUE)
    fixedColumns(1)
    maxHeight(400f)
    
    onCellClick { row, col, value ->
        println("[$row, $col] = $value")
    }
}
```

## 相关模块

- [core/](../) — 核心组件
- [model/](../model/) — 数据模型
