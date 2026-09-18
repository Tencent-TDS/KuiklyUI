# Canvas 与自定义绘制

Kuikly Compose 支持通过 `Canvas` 组件与绘制类 Modifier 进行自定义绘制，绘制逻辑运行在 **DrawScope** 作用域内。

## Canvas 组件

在指定区域内执行画布绘制。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| modifier <Badge text="必需" type="warn"/> | 尺寸策略，**必须指定尺寸** | Modifier |
| contentDescription <Badge text="非必需" type="warn"/> | 无障碍服务使用的描述文本 | String |
| onDraw <Badge text="必需" type="warn"/> | 绘制逻辑 | DrawScope.() -> Unit |

:::: warning 使用限制
- `modifier` 必须指定尺寸（`Modifier.size()`、`Modifier.fillMaxSize()`、`ColumnScope.weight()` 等）；父容器为包裹内容时只能使用精确尺寸。
- `onDraw` 在**绘制阶段**执行，其中不能调用 `@Composable` 函数，否则会在运行时抛异常。
::::

**示例**

```kotlin
Canvas(modifier = Modifier.size(200.dp)) {
    drawRect(color = Color.Red)
    drawLine(
        color = Color.Blue,
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = 4f
    )
}
```

## 绘制 Modifier

### Modifier.drawBehind

在组件内容**背后**绘制。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| onDraw | 绘制逻辑 | DrawScope.() -> Unit |

### Modifier.drawWithContent

在组件内容绘制的**同一层**绘制，可通过 `drawContent()` 控制原内容与自定义内容的先后顺序。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| onDraw | 绘制逻辑 | ContentDrawScope.() -> Unit |

```kotlin
Text(
    text = "带下划线的文本",
    modifier = Modifier.drawWithContent {
        drawContent()
        drawLine(
            color = Color.Black,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1f
        )
    }
)
```

### Modifier.drawWithCache

带缓存的绘制，用于在多次绘制之间复用对象（如 `Brush`、`Path`），避免每次重组都重新创建。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| onBuildDrawCache | 构建绘制缓存并返回 DrawResult | CacheDrawScope.() -> DrawResult |

### Modifier.paint

使用 `Painter` 绘制内容。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| painter <Badge text="必需" type="warn"/> | 绘制器 | Painter |
| sizeToIntrinsics <Badge text="非必需" type="warn"/> | 是否按 Painter 固有尺寸测量 | Boolean |
| alignment <Badge text="非必需" type="warn"/> | 对齐方式 | Alignment |
| contentScale <Badge text="非必需" type="warn"/> | 缩放模式 | ContentScale |

## DrawScope

绘制作用域，提供当前绘制环境信息与绘制方法。

**环境信息**

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| `size` | 当前绘制区域尺寸 | Size |
| `center` | 当前绘制区域中心点 | Offset |
| `layoutDirection` | 布局方向 | LayoutDirection |
| `drawContext` | 绘制上下文 | DrawContext |

**变换与裁剪**

| 方法 | 说明 |
| -- | -- |
| `inset` | 收缩绘制区域 |
| `translate` | 平移 |
| `rotate` / `rotateRad` | 旋转（角度 / 弧度） |
| `scale` | 缩放 |
| `clipRect` / `clipPath` | 按矩形 / 路径裁剪 |
| `withTransform` | 组合多个变换 |
| `drawIntoCanvas` | 直接操作底层 `Canvas` |

**绘制方法**

| 方法 | 说明 |
| -- | -- |
| `drawLine` | 绘制线段（支持 `Color` 与 `Brush` 两个重载） |
| `drawRect` | 绘制矩形（支持 `Color` 与 `Brush` 两个重载） |
| `drawOutline` | 按 `Outline` 绘制 |
| `draw` | 以指定尺寸与变换绘制 Painter 内容 |

:::: warning 当前支持范围
Kuikly 的 `DrawScope` 绘制方法当前**未开放** `pathEffect`、`colorFilter`、`blendMode` 参数（源码中对应参数处于注释状态）。依赖描边特效、颜色滤镜或混合模式的绘制逻辑需要改用其他方式实现。
::::
