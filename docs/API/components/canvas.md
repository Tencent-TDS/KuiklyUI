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

## 方法

`Canvas`主要通过`CanvasContext`进行图形绘制，`CanvasContext` 绘制接口如下：

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
| x | 圆弧中心点x  | Float |
| y | 圆弧中心点y  | Float |
| radius | 圆弧半径 | Float |
| startAngle | 圆弧起始角（单位弧度，例：PI.toFloat()） | Float |
| endAngle | 圆弧终止角（单位弧度，例：PI.toFloat()） | Float |
| counterclockwise | 圆弧绘制是否逆时针绘制 | Boolean |

### closePath

闭合路径之后图形绘制命令又重新指向到上下文中

### stroke

通过线条来绘制图形轮廓

### strokeStyle

设置笔触的颜色

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| color | 描边颜色  | Color |

### fill

通过填充路径的内容区域生成实心的图形

### fillStyle

设置填充内容区域的颜色

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| color | 描边颜色  | Color |

### fillText

在指定位置填充文本

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| text | 文本  | String |
| x | 文本位置的坐标x  | Float |
| y | 文本位置的坐标y  | Float |

### strokeText

在指定位置填充描边文本

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| text | 描边文本  | String |
| x | 描边位置的坐标x  | Float |
| y | 描边位置的坐标y  | Float |

### lineWidth

设置线条宽度

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| lindeWidth | 线条宽度  | Color |

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

### quadraticCurveTo

创建二次方贝塞尔曲线

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| cpx  | 	贝塞尔控制点的 x 坐标  | Float |
| cpy | 贝塞尔控制点的 y 坐标  | Float |
| x | 结束点的 x 坐标 | Float |
| y | 结束点的 y 坐标 | Float |

### bezierCurveTo

创建三次方贝塞尔曲线

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| cp1x | 第一个贝塞尔控制点的 x 坐标  | Float |
| cp1y | 第一个贝塞尔控制点的 y 坐标  | Float |
| cp2x | 第二个贝塞尔控制点的 x 坐标 | Float |
| cp2y | 第二个贝塞尔控制点的 y 坐标 | Float |
| x | 结束点的 x 坐标 | Float |
| y | 结束点的 y 坐标 | Float |

### clip<Badge text="安卓鸿蒙实现中" type="warn"/>

从原始画布剪切任意形状和尺寸的区域

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