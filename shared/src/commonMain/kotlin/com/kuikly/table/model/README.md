# 数据模型模块 (model)

## 概述

本目录包含表格组件的数据模型定义，采用 Kotlin `data class` 实现不可变数据结构。

## 模型清单

### ColumnDef — 列定义

```kotlin
data class ColumnDef(
    val key: String,                           // 列唯一标识
    val title: String,                         // 表头文本
    val width: Float = 100f,                   // 列宽
    val minWidth: Float = 50f,                 // 最小宽度
    val align: TextAlign = TextAlign.LEFT,     // 单元格对齐
    val headerAlign: TextAlign = TextAlign.CENTER, // 表头对齐
    val fixed: Boolean = false,                // 是否为固定列
    val customRenderer: CellRendererScope? = null  // 自定义渲染器
)
```

### CellData — 单元格数据

```kotlin
data class CellData(
    val rowIndex: Int,           // 行索引
    val colIndex: Int,           // 列索引
    val value: String,           // 单元格值
    val columnKey: String = "",  // 所属列的 key
    val extra: Map<String, Any>? = null  // 扩展数据
)
```

### TableData — 表格聚合数据

```kotlin
data class TableData(
    val columns: List<ColumnDef>,
    val rows: List<List<String>>,
    val headerVisible: Boolean = true,
    val fixedColumns: Int = 0
)
```

提供 `getCellData(rowIndex, colIndex)` 方法安全获取单元格数据。

### TableTheme — 主题配置

```kotlin
data class TableTheme(
    // 表头样式 (6项)
    headerBackgroundColor, headerTextColor, headerFontSize, headerFontBold, headerHeight,
    // 表体样式 (6项)
    rowHeight, rowBackgroundColor, rowAlternateColor, cellTextColor, cellFontSize, cellPaddingHorizontal, cellPaddingVertical,
    // 边框样式 (5项)
    borderColor, borderWidth, showRowBorder, showColumnBorder, showOuterBorder,
    // 斑马纹
    stripedRows: Boolean = true
)
```

内置 4 套预设主题：
| 预设 | 说明 |
|------|------|
| `DEFAULT` | 默认浅灰主题 |
| `DARK` | 深色主题 |
| `COMPACT` | 紧凑主题（较小行高/字号） |
| `BLUE` | 蓝色主题 |

### TextAlign — 文本对齐

```kotlin
enum class TextAlign(val value: String) {
    LEFT("left"), CENTER("center"), RIGHT("right")
}
```

提供 `toFlexAlign()` 扩展方法映射到 Kuikly 的 `FlexAlign` 枚举。

### CellRendererScope — 自定义渲染器类型

```kotlin
typealias CellRendererScope = ViewBuilder.(cellData: CellData, rowIndex: Int, colIndex: Int) -> Unit
```

## 相关模块

- [core/](../) — 核心组件
- [dsl/](../dsl/) — DSL 入口（使用这些模型构建表格）
- [demo/](../demo/) — 演示页面
