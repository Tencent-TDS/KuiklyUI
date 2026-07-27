# Chart（跨端图表）

`Chart` 是基于 Kuikly `Canvas` 实现的声明式跨端图表组件。一份 Kotlin 代码可在 Android、iOS、macOS、HarmonyOS、Web 和小程序上运行，无需接入平台图表 SDK。

组件内置以下能力：

- 折线图、分组柱状图和面积图，可在同一坐标系混合展示；
- 自动计算易读的 Y 轴范围和刻度，也可指定固定范围；
- 坐标轴、网格线、图例、数值标签和空状态；
- 通过点击或滑动选中分类，展示十字线和 Tooltip；
- `null` 数据断点、正负值、单点、常量数据和空数据的安全处理；
- 使用纯 Kotlin 布局引擎计算刻度、坐标与命中区域，所有平台保持一致。

[完整示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ChartExamplePage.kt)

## 快速开始

```kotlin
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.views.chart.Chart

Chart {
    attr {
        size(360f, 260f)
        data {
            categories("Mon", "Tue", "Wed", "Thu", "Fri")

            bar("Orders", Color(0xFF00A870L)) {
                values(120f, 168f, 142f, 210f, 232f)
                showValues = true
            }

            line("Revenue", Color(0xFF0052D9L)) {
                values(92f, 126f, 118f, 168f, 182f)
                lineWidth = 2.5f
            }
        }
    }
    event {
        selectionChanged { selection ->
            // selection.index、selection.category、selection.values
        }
    }
}
```

> `Chart` 必须设置明确的宽度和高度。组件会在初始化时校验尺寸，避免零尺寸 Canvas 静默不渲染。

## 数据 DSL

### categories

设置 X 轴分类：

```kotlin
categories("Jan", "Feb", "Mar")
```

如果系列的数据数量多于分类数量，组件会自动使用从 `1` 开始的序号补全标签。

### line

添加折线系列：

```kotlin
line("Temperature", Color(0xFFE34D59L)) {
    values(18f, 21f, null, 26f)
    lineWidth = 2f
    showPoints = true
    showValues = false
}
```

`null` 表示数据缺失。折线和面积不会跨过缺失点连接，Tooltip 中显示为 `--`。

### bar

添加柱状系列。多个柱状系列会自动在分类带内分组排列：

```kotlin
bar("2025", Color(0xFF0052D9L)) { values(12f, 18f, 21f) }
bar("2026", Color(0xFF00A870L)) { values(16f, 22f, 25f) }
```

### area

添加面积系列。面积以 `0` 或当前 Y 轴中最接近 `0` 的边界作为基线：

```kotlin
area("Net change", Color(0xFF7B61FFL)) {
    values(-12f, 8f, 24f, null, 18f)
}
```

可通过 `areaOpacity(0.2f)` 调整填充透明度。

## 属性

除所有[基础属性](basic-attr-event.md#基础属性)外，`ChartAttr` 提供以下配置：

| 方法 | 默认值 | 说明 |
|:---|:---|:---|
| `data(ChartData)` | 空数据 | 直接传入不可变数据模型 |
| `data { ... }` | 空数据 | 使用声明式 DSL 构造数据 |
| `insets(ChartInsets)` | `48,18,16,38` | 绘图区为坐标标签保留的边距 |
| `legend(position)` | `TOP` | `NONE`、`TOP` 或 `BOTTOM` |
| `interactive(enabled)` | `true` | 是否响应点击和滑动选点 |
| `tooltip(visible)` | `true` | 选中时是否展示 Tooltip |
| `gridLines(visible)` | `true` | 是否绘制水平网格线 |
| `includeZero(include)` | `false` | 自动范围是否强制包含 0；柱状图和面积图始终包含 0 |
| `yTickCount(count)` | `5` | 期望 Y 轴刻度数，范围 2～10 |
| `yRange(min, max)` | 自动 | 固定 Y 轴范围 |
| `autoYRange()` | — | 恢复自动范围 |
| `labelFontSize(size)` | `11` | 坐标轴和 Tooltip 字号 |
| `legendFontSize(size)` | `11` | 图例字号 |
| `emptyText(text)` | `No data` | 空数据文案 |
| `colors(...)` | 内置中性色 | 背景、坐标轴、网格、文字和 Tooltip 颜色 |
| `areaOpacity(opacity)` | `0.18` | 面积填充透明度 |
| `yLabelFormatter { ... }` | 智能缩写 | 自定义 Y 轴标签 |
| `valueLabelFormatter { ... }` | 智能缩写 | 自定义系列数值标签 |
| `tooltipValueFormatter { ... }` | 智能缩写 | 自定义 Tooltip 数值 |

图例会根据实际换行数动态增加绘图区的上/下边距，不需要调用方重复预留。

## 事件

### selectionChanged

用户点击或在图表上滑动到新的分类时触发：

```kotlin
event {
    selectionChanged { selection ->
        val index = selection.index
        val label = selection.category
        selection.values.forEach { item ->
            // item.seriesName、item.value、item.color
        }
    }
    selectionCleared {
        // 用户移出绘图区、十字线消失时清空业务侧选中状态
    }
}
```

`ChartSelection.values` 保持系列声明顺序，便于业务渲染自定义详情面板。

## 动态更新

`ChartData` 是不可变模型。父组件中的响应式状态改变后，重新执行 `data(newData)` 即可触发 Canvas 重绘：

```kotlin
Chart {
    attr {
        size(360f, 240f)
        data(currentChartData)
    }
}
```

## 边界行为

- 非有限数值（`NaN`、正负无穷）按缺失数据处理；
- 空数据展示 `emptyText`，不会产生无效坐标；
- 全部数值相同时自动扩展上下界，避免除零；
- 固定范围的 `min > max` 时会自动交换；
- X 轴分类超过 8 个时自动抽样标签，但所有数据点仍会绘制；
- 图表本身不滚动或缩放，适合仪表盘和业务卡片；大量数据建议先在业务层采样。

## 平台说明

图表只依赖 `commonMain` 中的 `Canvas`、手势和响应式能力，不包含平台分支。各平台会复用 Kuikly 对应的 Canvas Renderer；颜色、间距和数据布局由同一套纯 Kotlin 布局引擎计算。
