# 沉浸光感控件（Slider / Switch / Select）

:::tip 系统和版本要求

- **系统版本**：HarmonyOS 6.1.0+

- **Kuikly版本**：2.6.0+

:::

沉浸光感**官方白名单控件**的原生封装——Slider、Switch、Select 可全页面生效沉浸光感。属性面对齐 Kuikly 的 iOS 液态玻璃控件（iOSSlider/iOSSwitch），业务可近乎无缝切换。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ImmersiveLightSenseNativeControlsDemoPage.kt)

## Slider（ImmersiveLightSenseSliderOhos）

```kotlin
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseSliderOhos

ImmersiveLightSenseSliderOhos {
    attr {
        width(300f); height(40f)
        value(50f); min(0f); max(100f)
        progressColor(Color(0xFF007DFF))   // 进度色
        trackColor(Color(0x33808080))      // 轨道色
        thumbColor(Color(0xFFFFFFFF))      // 滑块色
        trackThickness(6f)                 // 轨道粗细
        immersiveMaterial(style = ImmersiveStyle.THICK, interactive = true)  // interactive=按压弹性形变
    }
    event { valueChanged { v -> } }
}
```

| 属性 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| value | 当前值 | Float | 0 |
| min / max | 值域 | Float | 0 / 100 |
| progressColor | 已滑过部分颜色 | Color | — |
| trackColor | 轨道底色 | Color | — |
| thumbColor | 滑块颜色 | Color | — |
| trackThickness | 轨道粗细（vp） | Float | — |
| immersiveMaterial | 沉浸光感材质 | 见[总览](immersive-light-sense.md#immersivematerial) | 关 |

事件：`valueChanged` 值变化回调。

## Switch（ImmersiveLightSenseSwitchOhos）

```kotlin
ImmersiveLightSenseSwitchOhos {
    attr {
        size(51f, 31f)
        isOn(true)
        onColor(Color(0xFF007DFF))      // 开启态轨道色
        thumbColor(Color(0xFFFFFFFF))   // 圆点色
        immersiveMaterial(style = ImmersiveStyle.REGULAR)
    }
    event { switchChanged { on -> } }
}
```

| 属性 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| isOn | 开关状态 | Boolean | false |
| onColor | 开启态轨道颜色 | Color | — |
| thumbColor | 圆点颜色 | Color | — |
| immersiveMaterial | 沉浸光感材质 | 见[总览](immersive-light-sense.md#immersivematerial) | 关 |

事件：`switchChanged` 开关变化回调。（ArkUI Toggle 不支持 unOnColor，故不开放关闭态轨道色。）

## Select（ImmersiveLightSenseSelectOhos）

```kotlin
ImmersiveLightSenseSelectOhos {
    attr {
        width(150f); height(40f)
        options(listOf("低", "中", "高"))
        selectedIndex(1)
        immersiveMaterial(style = ImmersiveStyle.REGULAR)
    }
    event { selectChanged { index, value -> } }
}
```

| 属性 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| options | 选项列表 | List\<String\> | 空 |
| selectedIndex | 当前选中索引 | Int | 0 |
| immersiveMaterial | 沉浸光感材质 | 见[总览](immersive-light-sense.md#immersivematerial) | 关 |

事件：`selectChanged` 选中变化回调（index + value）。

## 注意事项

- 三个控件均为官方白名单控件，材质**不限于特定区域**、全页面生效；
- 低版本系统/不支持设备自动降级为普通控件（不崩页面、只丢材质）；
- 布局属性（size/margin/position 等）照常从通用 attr 下发。
