# Canvas(自绘画布)

结合`CanvasContext`提供绘制图形的接口，对齐 H5 Canvas 能力，可用于绘制直线、曲线、矩形、圆形、文本等

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/CanvasTestPage.kt)

以下是`Canvas`的构造代码，初始化 `Canvas`注册一个渲染回调函数，通过回调的`CanvasContext`绘制一条直线

```kotlin
 Canvas ({
   attr {
     absolutePosition(0f, 0f, 0f, 0f)
   }
 }) { context, width, height ->
     context.beginPath()
     context.strokeStyle(Color.RED)
     context.lineWidth(2.0f)
     context.moveTo(0f, 0f)
     context.lineTo(width, height)
     context.stroke()
    }
```

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)


# CanvasContext(画布上下文)

`Canvas`主要通过`CanvasContext`进行图形绘制，`CanvasContext` 绘制接口如下：

## 路径操作

### beginPath

新建一条路径，生成之后，图形绘制命令被指向到路径上生成路径

### moveTo

将笔触移动到指定的坐标x以及y上

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | 指定的坐标x  | Float |
| y | 指定的坐标y  | Float |

### lineTo

绘制一条从当前位置到指定x以及y位置的直线

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | 移动到指定的坐标x  | Float |
| y | 移动指定的坐标y  | Float |

### arc

创建弧/曲线（用于创建圆或部分圆）

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| centerX | 圆弧中心点x  | Float |
| centerY | 圆弧中心点y  | Float |
| radius | 圆弧半径 | Float |
| startAngle | 圆弧起始角（单位弧度，例：PI.toFloat()） | Float |
| endAngle | 圆弧终止角（单位弧度，例：PI.toFloat()） | Float |
| counterclockwise | 圆弧绘制是否逆时针绘制 | Boolean |

### closePath

闭合路径之后图形绘制命令又重新指向到上下文中

### quadraticCurveTo

创建二次方贝塞尔曲线

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| controlPointX  | 	贝塞尔控制点的 x 坐标  | Float |
| controlPointY | 贝塞尔控制点的 y 坐标  | Float |
| pointX | 结束点的 x 坐标 | Float |
| pointY | 结束点的 y 坐标 | Float |

### bezierCurveTo

创建三次方贝塞尔曲线

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| controlPoint1X | 第一个贝塞尔控制点的 x 坐标  | Float |
| controlPoint1Y | 第一个贝塞尔控制点的 y 坐标  | Float |
| controlPoint2X | 第二个贝塞尔控制点的 x 坐标 | Float |
| controlPoint2Y | 第二个贝塞尔控制点的 y 坐标 | Float |
| pointX | 结束点的 x 坐标 | Float |
| pointY | 结束点的 y 坐标 | Float |

## 描边与填充

### stroke

通过线条来绘制图形轮廓

### strokeStyle

设置笔触的颜色或渐变

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| color | 描边颜色  | Color |

或

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| linearGradient | 线性渐变对象  | CanvasLinearGradient |

### fill

通过填充路径的内容区域生成实心的图形

### fillStyle

设置填充内容区域的颜色或渐变

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| color | 填充颜色  | Color |

或

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| linearGradient | 线性渐变对象  | CanvasLinearGradient |

## 线条样式

### lineWidth

设置线条宽度（默认为0f）

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| width | 线条宽度  | Float |

### setLineDash

设置虚线样式，如果要切换至实线模式，将参数设置为空数组。

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| intervals | 线和间隙的交替长度  | List\<Float\> |

### lineCapRound

设置线条末端的样式为圆形的

### lineCapButt

设置线条末端的样式为平直的

### lineCapSquare

设置线条末端的样式为方形的

## 渐变

### createLinearGradient

创建线性渐变对象

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x0 | 起始点的 x 坐标 | Float |
| y0 | 起始点的 y 坐标 | Float |
| x1 | 结束点的 x 坐标 | Float |
| y1 | 结束点的 y 坐标 | Float |

该方法返回一个`CanvasLinearGradient`对象，该对象可以调用`addColorStop`方法向渐变中添加一个颜色停点。不同颜色停点之间会形成线性渐变。

**addColorStop**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| stopIn01 | 颜色停点的位置，范围在 0 到 1 之间 | Float |
| color | 颜色停点的颜色 | Color |

`CanvasLinearGradient`对象添加完颜色停点之后即可作为`fillStyle`和`strokeStyle`的参数应用于填充渐变和描边渐变。

### createRadialGradient<Badge text="实验性API，仅iOS支持" type="warn"/>

创建径向渐变

> 注：实验性API，仅iOS支持

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x0 | 开始圆的圆心 x 坐标 | Float |
| y0 | 开始圆的圆心 y 坐标 | Float |
| r0 | 开始圆的半径 | Float |
| x1 | 结束圆的圆心 x 坐标 | Float |
| y1 | 结束圆的圆心 y 坐标 | Float |
| r1 | 结束圆的半径 | Float |
| alpha | 整体透明度，一般为1f，取值范围 [0f, 1f] | Float |
| colors | 渐变中的颜色数组（可变参数） | Color |

## 文本

### textAlign

设置文本对齐方式

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| textAlign | 文本对齐方式  | TextAlign |

**TextAlign 枚举值：**

| 值  | 描述     |
|:----|:-------|
| LEFT | 左对齐 |
| CENTER | 居中对齐 |
| RIGHT | 右对齐 |

### font

设置文本样式

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| style | 字体样式（可选，默认 NORMAL） | FontStyle |
| weight | 字体粗细（可选，默认 NORMAL） | FontWeight |
| size | 字体大小（可选，默认 15f） | Float |
| family | 字体名称（可选，默认为空） | String |

**FontStyle 枚举值：**

| 值  | 描述     |
|:----|:-------|
| NORMAL | 正常 |
| ITALIC | 斜体 |

**FontWeight 枚举值：**

| 值  | 描述     |
|:----|:-------|
| NORMAL | 正常（400） |
| MEDIUM | 中等（500） |
| SEMISOLID | 半粗（600） |
| BOLD | 粗体（700） |

### measureText

测量文本尺寸，返回 `TextMetrics` 对象

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| value | 要测量的文本 | String |

**TextMetrics 属性：**

| 属性  | 描述     | 类型 |
|:----|:-------|:--|
| width | 文本宽度 | Float |
| actualBoundingBoxLeft | 文本左边界距离 | Float |
| actualBoundingBoxRight | 文本右边界距离 | Float |
| actualBoundingBoxAscent | 文本基线上方高度 | Float |
| actualBoundingBoxDescent | 文本基线下方高度 | Float |

### fillText

在指定位置绘制填充文本

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| text | 文本  | String |
| x | 文本位置的坐标x  | Float |
| y | 文本位置的坐标y  | Float |

### strokeText

在指定位置绘制描边文本

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| text | 描边文本  | String |
| x | 描边位置的坐标x  | Float |
| y | 描边位置的坐标y  | Float |

## 图像绘制

### drawImage

在画布上绘制图像，支持三种重载形式：

**基础绘制：**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| image | 图像引用 | ImageRef |
| dx | 目标 x 坐标 | Float |
| dy | 目标 y 坐标 | Float |

**指定尺寸绘制：**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| image | 图像引用 | ImageRef |
| dx | 目标 x 坐标 | Float |
| dy | 目标 y 坐标 | Float |
| dWidth | 目标宽度 | Float |
| dHeight | 目标高度 | Float |

**裁剪绘制：**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| image | 图像引用 | ImageRef |
| sx | 源图像裁剪区域 x 坐标 | Float |
| sy | 源图像裁剪区域 y 坐标 | Float |
| sWidth | 源图像裁剪区域宽度 | Float |
| sHeight | 源图像裁剪区域高度 | Float |
| dx | 目标 x 坐标 | Float |
| dy | 目标 y 坐标 | Float |
| dWidth | 目标宽度 | Float |
| dHeight | 目标高度 | Float |

## 状态管理

### save

保存当前绑定的状态（包括变换矩阵、裁剪区域、样式等）

### saveLayer

保存当前状态并创建一个新的图层

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | 图层起始 x 坐标 | Float |
| y | 图层起始 y 坐标 | Float |
| width | 图层宽度 | Float |
| height | 图层高度 | Float |

### restore

恢复之前保存的状态

## 裁剪

### clip

从原始画布剪切任意形状和尺寸的区域

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| intersect | 是否使用交集模式（默认为 true） | Boolean |

### clipPathIntersect

使用交集模式裁剪路径（等同于 `clip(true)`）

### clipPathDifference

使用差集模式裁剪路径（等同于 `clip(false)`）

## 变换

### translate

平移画布原点

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | x 方向平移距离 | Float |
| y | y 方向平移距离 | Float |

### scale

缩放画布

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | x 方向缩放比例 | Float |
| y | y 方向缩放比例 | Float |

### rotate

旋转画布

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| angle | 旋转角度（弧度） | Float |

### skew

倾斜画布

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | x 方向倾斜角度 | Float |
| y | y 方向倾斜角度 | Float |

### transform

应用变换矩阵

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| array | 3x3 变换矩阵（9个元素的 FloatArray） | FloatArray |
