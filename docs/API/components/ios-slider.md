# iOSSlider(iOS 原生滑块)

iOS 原生 `UISlider` 封装组件，支持液态玻璃外观。与跨端 [Slider](slider.md) 不同，该组件直接使用系统原生控件渲染。

<Badge text="仅 iOS" type="warn"/>

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### currentProgress

设置滑块当前进度值，取值范围 `0f ~ 1f`，超出范围会抛出异常。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| value | 当前进度值 | Float |

### minValue

设置滑块最小值，默认 `0.0`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| minValue | 最小值 | Float |

### maxValue

设置滑块最大值，默认 `1.0`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| maxValue | 最大值 | Float |

### thumbColor

设置滑块按钮颜色。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 按钮颜色 | Color |

### trackColor

设置滑轨背景颜色。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 滑轨颜色 | Color |

### progressColor

设置已填充部分的进度颜色。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 进度颜色 | Color |

### continuous

设置拖动过程中是否连续回调数值变化，默认由系统决定。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| continuous | true 连续回调，false 离散回调 | Boolean |

### trackThickness

设置滑轨粗细。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| thickness | 滑轨粗细 | Float |

### thumbSize

设置滑块按钮尺寸。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| size | 按钮尺寸 | Size |

### sliderDirection

设置滑块方向。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| isHorizontal | true 水平方向，false 垂直方向 | Boolean |

## 事件

支持所有[基础事件](basic-attr-event.md#事件)，此外还支持：

### onValueChanged

滑块数值变化回调。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| value | 当前数值 | Float |

### onTouchDown

按下滑块回调。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| value | 当前数值 | Float |
| x | 触点相对组件横坐标 | Float |
| y | 触点相对组件纵坐标 | Float |
| pageX | 触点相对页面横坐标 | Float |
| pageY | 触点相对页面纵坐标 | Float |

### onTouchUp

抬起滑块回调，参数与 `onTouchDown` 一致。

## 示例

```kotlin{5-18}
iOSSlider {
    attr {
        size(200f, 30f)
        currentProgress(0.5f)
        minValue(0f)
        maxValue(1f)
        progressColor(Color.BLUE)
        trackColor(Color.GRAY)
        continuous(true)
    }
    event {
        onValueChanged { params ->
            // params.value 为当前数值
        }
    }
}
```
