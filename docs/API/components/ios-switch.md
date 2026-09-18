# iOSSwitch(iOS 原生开关)

iOS 原生 `UISwitch` 封装组件，支持液态玻璃外观。与跨端 [Switch](switch.md) 不同，该组件直接使用系统原生控件渲染。

<Badge text="仅 iOS" type="warn"/>

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### enabled

设置开关是否可交互。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| enabled | true 允许交互 | Boolean |

### isOn

设置开关当前开合状态。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| isOn | true 为开 | Boolean |

### onColor

设置开启状态下的颜色。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 开启状态颜色 | Color |

### unOnColor

设置关闭状态下的颜色。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 关闭状态颜色 | Color |

### thumbColor

设置滑块（圆形滑动部分）颜色。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 滑块颜色 | Color |

## 事件

支持所有[基础事件](basic-attr-event.md#事件)，此外还支持：

### switchOnChanged

开关状态变化回调。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| value | 变化后的开合状态 | Boolean |

## 示例

```kotlin{5-14}
iOSSwitch {
    attr {
        size(51f, 31f)
        isOn(true)
        onColor(Color.GREEN)
        unOnColor(Color.GRAY)
    }
    event {
        switchOnChanged { params ->
            // params.value 为变化后的状态
        }
    }
}
```
