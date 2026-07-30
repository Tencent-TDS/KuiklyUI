# iOSSegmentedControl(iOS分段控件)

:::tip 系统和版本要求

- **系统版本**：iOS 26.0+ 自动支持液态玻璃效果，低版本系统上使用标准UISegmentedControl外观。

- **Kuikly版本**：2.5.0+

:::

`iOSSegmentedControl` 是基于 iOS 原生 `UISegmentedControl` 实现的分段控件组件，在 iOS 26+ 系统上自动具备液态玻璃视觉效果。该组件提供了完整的原生分段选择功能，支持多选项切换、自定义标题和选择事件处理。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/LiquidGlassDemoPage.kt)

## 基础用法

```kotlin
import com.tencent.kuikly.core.views.ios.SegmentedControlIOS

private var selectedIndex by observable(0)

SegmentedControlIOS {
    attr {
        height(40f)
        width(200f)
        titles(listOf("选项1", "选项2", "选项3"))
        selectedIndex(selectedIndex)
    }
    event {
        onValueChanged { params ->
            selectedIndex = params.index
            println("选中选项: ${params.index + 1}")
        }
    }
}
```

## 属性

`iOSSegmentedControl` 支持所有[基础属性](basic-attr-event.md#基础属性)，还支持以下专有属性：

### titles

设置分段控制器的标题数组。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| titles | 分段标题列表 | List\<String\> | 空列表 |

#### 示例

```kotlin
SegmentedControlIOS {
    attr {
        titles(listOf("全部", "进行中", "已完成"))
    }
}
```

### selectedIndex

设置当前选中的分段索引。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| index | 选中索引（0-based） | Int | 0 |

#### 示例

```kotlin
SegmentedControlIOS {
    attr {
        selectedIndex(1) // 选中第二个选项
    }
}
```

## 事件

### onValueChanged

分段选择变化时触发的事件。

#### ValueChangedParams

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| index | 当前选中索引 | Int |

## 相关组件

- [Switch](./switch.md) - 支持液态玻璃效果的开关组件
- [Slider](./slider.md) - 支持液态玻璃效果的滑块组件
- [Tabs](./tabs.md) - 通用标签页组件
- [iOS 液态玻璃概述](./ios26-liquid-glass.md) - 液态玻璃效果详解
