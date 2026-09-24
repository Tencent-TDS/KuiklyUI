# LiquidGlass(液态玻璃组件)

:::tip 系统和版本要求

- **系统版本**：iOS 26.0+

- **Kuikly版本**：2.5.0+

:::

`LiquidGlass` 是专门为 iOS 26+ 液态玻璃效果设计的容器组件。其等同于View组件设置 `glassEffectIOS()` 属性。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/LiquidGlassDemoPage.kt)

## 基础用法

```kotlin
import com.tencent.kuikly.core.views.LiquidGlass

LiquidGlass {
    attr {
        height(60f)
        borderRadius(30f)
    }
    Text {
        attr {
            text("液态玻璃效果")
        }
    }
}
```

## 属性

`LiquidGlass` 支持所有[基础属性](basic-attr-event.md#基础属性)，还支持以下专有属性：

### glassEffectInteractive

控制液态玻璃效果是否响应用户交互。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| isInteractive | 是否启用交互效果 | Boolean | false |

### glassEffectTintColor

设置液态玻璃效果的色调颜色。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| color | 色调颜色 | Color | 系统默认 |

### glassEffectStyle

设置液态玻璃效果的样式。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| style | 玻璃效果样式 | GlassEffectStyle | REGULAR |

**GlassEffectStyle 枚举值：**

- `REGULAR` - 标准液态玻璃效果
- `CLEAR` - 清透液态玻璃效果

## 事件

`LiquidGlass` 支持所有[基础事件](basic-attr-event.md#基础事件)。

## 注意事项

### 1. 系统兼容性

使用独立液态玻璃组件时，请做好系统版本检测，确保在 iOS 26+ 系统上使用液态玻璃效果。对于不支持的版本，建议提供降级方案。

```kotlin
import com.tencent.kuikly.core.utils.PlatformUtils

// 推荐做法：检测系统支持
if (PlatformUtils.isLiquidGlassSupported()) {
    LiquidGlass {
        // 液态玻璃实现
    }
} else {
    View {
        attr {
            backgroundColor(Color.GRAY) // 降级方案
        }
        // 相同的子组件
    }
}
```

### 2. 背景设置

使用 `LiquidGlass` 时，避免设置不透明背景色，这会覆盖液态玻璃效果：

```kotlin
LiquidGlass {
    attr {
        // ❌ 错误：会覆盖玻璃效果
        // backgroundColor(Color.RED)
        
        // ✅ 正确：保持透明或不设置
        // backgroundColor(Color.TRANSPARENT)
    }
}
```

## iOS 原生液态玻璃组件

除 `LiquidGlass` 容器外，以下 iOS 原生组件同样内置液态玻璃外观。它们直接使用系统原生控件渲染（`UISlider` / `UISwitch`），仅在 iOS 生效。

### iOSSlider

**属性**

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| `currentProgress(value)` | 当前进度，取值 `0f ~ 1f`，超出范围会抛出异常 | Float |
| `minValue(minValue)` | 最小值，默认 `0.0` | Float |
| `maxValue(maxValue)` | 最大值，默认 `1.0` | Float |
| `thumbColor(color)` | 滑块按钮颜色 | Color |
| `trackColor(color)` | 滑轨背景颜色 | Color |
| `progressColor(color)` | 已填充部分的进度颜色 | Color |
| `continuous(continuous)` | 拖动过程中是否连续回调数值变化 | Boolean |
| `trackThickness(thickness)` | 滑轨粗细 | Float |
| `thumbSize(size)` | 滑块按钮尺寸 | Size |
| `sliderDirection(isHorizontal)` | 方向，true 水平 / false 垂直 | Boolean |

**事件**

| 事件 | 描述 | 回调参数 |
| -- | -- | -- |
| `onValueChanged` | 数值变化 | value: Float |
| `onTouchDown` | 按下 | value / x / y / pageX / pageY |
| `onTouchUp` | 抬起，参数同 `onTouchDown` | 同 `onTouchDown` |

### iOSSwitch

**属性**

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| `enabled(enabled)` | 是否可交互 | Boolean |
| `isOn(isOn)` | 当前开合状态 | Boolean |
| `onColor(color)` | 开启状态下的颜色 | Color |
| `unOnColor(color)` | 关闭状态下的颜色 | Color |
| `thumbColor(color)` | 滑块（圆形滑动部分）颜色 | Color |

**事件**

| 事件 | 描述 | 回调参数 |
| -- | -- | -- |
| `switchOnChanged` | 开关状态变化 | value: Boolean |

**示例**

```kotlin
iOSSlider {
    attr {
        size(200f, 30f)
        currentProgress(0.5f)
        progressColor(Color.BLUE)
    }
}

iOSSwitch {
    attr {
        size(51f, 31f)
        isOn(true)
        onColor(Color.GREEN)
    }
}
```

## 相关组件

- [GlassEffectContainer](./glass-effect-container.md) - 液态玻璃容器组合
- [View](./view.md#glasseffectios) - 基础容器（支持glassEffectIOS属性）
- [iOS 液态玻璃概述](./ios26-liquid-glass.md) - 完整使用指南
