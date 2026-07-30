# 布局组件

本页说明 Kuikly Compose 中布局组件的支持情况与使用注意事项。  
基础用法和官方保持一致，请**优先查阅 Jetpack Compose 官方文档**。

> 官方文档（推荐阅读）：[Layouts in Compose](https://developer.android.com/develop/ui/compose/layouts/basics)

## 支持的布局组件
Kuikly 当前重点支持以下布局组件，并与 Jetpack Compose 对齐：

### 基础布局容器

- **Column** - 垂直排列子元素
- **Row** - 水平排列子元素
- **Box** - 层叠排列子元素
- **BoxWithConstraints** - 带约束的层叠布局

### 自定义布局

- **Layout** - 完全自定义测量与放置逻辑的布局容器，适合特殊排列需求  

### 常见布局相关 Modifier

- 尺寸控制  
  - `size` / `width` / `height`  
  - `fillMaxWidth` / `fillMaxHeight` / `fillMaxSize`  
  - `requiredSize` / `requiredWidth` / `requiredHeight`（强制尺寸）  
  - `defaultMinSize`、`heightIn` 、`widthIn`等（设置最小/范围尺寸）
- 间距与位置  
  - `padding`（内边距）  
  - `offset` / `absoluteOffset`（位移）  
- 权重与占比  
  - `weight`（配合 `Row` / `Column` 控制主轴空间占比）  
  - `aspectRatio`（固定宽高比）
- 对齐与包装  
  - `wrapContentSize` / `wrapContentWidth` / `wrapContentHeight`  
  - `align` / `alignBy` / `alignByBaseline`（配合 `Box` / `Row` 使用）  
  - `matchParentSize`（子项填满父容器）

### 流式布局

- **FlowRow** - 流式水平布局，自动换行
- **FlowColumn** - 流式垂直布局，自动换列

### 辅助组件

- **Spacer** - 空白占位符，用于在布局中占据固定或可伸缩空间
- **HorizontalDivider / VerticalDivider** - 水平 / 垂直分割线，常用于列表、区块之间的分隔

## 自定义布局示例

```kotlin
@Composable
fun TwoStack(
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            top()
            bottom()
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOf { it.width }
        val height = placeables.sumOf { it.height }
        layout(width, height) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height
            }
        }
    }
}
```

## 内外边距示例

```kotlin
@Composable
fun MarginPaddingExample() {
    Column {
        // 1. 外边距：padding 在 background 之前，相当于父容器的 margin
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.Yellow)
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("外边距示例")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 内边距：padding 在 background 之后，挤压内部内容
        Box(
            modifier = Modifier
                .background(Color.Yellow)
                .padding(16.dp)
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.background(Color.White)) {
                Text("内边距示例")
            }
        }
    }
}
```

## 更多代码示例

以下 Demo 展示了布局组件与布局相关能力的典型用法，可在开源仓库中查看完整代码：

- [`BoxWithConstraintsDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/BoxWithConstraintsDemo.kt)：`BoxWithConstraints` 响应式布局示例  
- [`FlowRowDemo1.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/FlowRowDemo1.kt)：`FlowRow` 流式行布局示例  
- [`FlowColumnDemo1.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/FlowColumnDemo1.kt)：`FlowColumn` 流式列布局示例  
- [`MarginPaddingTestDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/MarginPaddingTestDemo.kt)：Compose 内外边距、边框写法示例

## 注意事项

- 自定义布局时，保持测量/放置逻辑在组合内，避免阻塞。
- Modifier 顺序会直接影响布局效果（例如 padding / background / size 的先后关系），建议结合官方文档与实际渲染结果理解。
