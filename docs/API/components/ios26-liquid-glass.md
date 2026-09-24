# iOS 液态玻璃效果

:::tip 系统和版本要求

- **系统版本**：iOS 26.0+

- **Kuikly版本**：2.5.0+

:::

iOS 26 引入了全新的液态玻璃（Liquid Glass）视觉效果，为用户界面带来了更加生动和现代的外观。 Kuikly基于系统原生API适配了这一特性。

- **系统原生**：使用iOS原生实现，确保最佳性能与效果
- **自动降级**：在不支持的设备和系统版本上自动忽略或降级为普通容器

## 基础使用方法

### 1. 属性使用方式

通过 `glassEffectIOS()` 和 `glassEffectContainerIOS()` 属性为任意继承自ViewContainer的组件添加液态玻璃效果（包括View、Button等容器类组件）：

```kotlin
View {
    attr {
        size(300f, 60f)
        borderRadius(30f)
        glassEffectIOS() // 启用液态玻璃效果
    }
}

View {
    attr {
        size(300f, 60f)
        borderRadius(30f)
        // 实现多个液态玻璃组件的融合效果
        glassEffectContainerIOS()
    }
}
```

### 2. 独立组件使用方式

为了支持更加灵活的使用方式，Kuikly也同时提供了独立的液态玻璃组件：

```kotlin
// 基础液态玻璃容器
LiquidGlass {
    attr {
        height(60f)
        borderRadius(30f)
        glassEffectInteractive(true)
    }
}

// 液态玻璃容器组合
GlassEffectContainer {
    attr {
        spacing(15f)
        flexDirectionRow()
    }
    // 液态玻璃子组件会产生融合效果
}
```

## 液态玻璃组件清单

Kuikly 的液态玻璃能力分为「属性入口」与「内置组件」两部分，全部入口如下：

| 入口 | 类型 | 启用方式 | 说明 |
| -- | -- | -- | -- |
| 任意容器组件 | 属性 | `glassEffectIOS()` | 为任意继承自 ViewContainer 的组件（View、Button 等）添加液态玻璃效果 |
| 任意容器组件 | 属性 | `glassEffectContainerIOS(spacing)` | 为容器添加融合效果，子元素相互靠近时产生视觉融合 |
| [LiquidGlass](./liquid-glass.md) | 独立组件 | 无需额外设置 | 基础液态玻璃容器，等同于 View + `glassEffectIOS()` |
| [GlassEffectContainer](./glass-effect-container.md) | 独立组件 | 无需额外设置 | 液态玻璃容器组合，等同于 View + `glassEffectContainerIOS()` |
| [Switch](./switch.md#enableglasseffect) | 内置组件 | `enableGlassEffect(true)` | iOS 26+ 自动使用原生液态玻璃开关 |
| [Slider](./slider.md#enableglasseffect) | 内置组件 | `enableGlassEffect(true)` | iOS 26+ 自动使用原生液态玻璃滑块 |
| [iOSSegmentedControl](./ios-segmented-control.md) | iOS 原生组件 | 无需额外设置 | iOS 原生分段控件，自动具备液态玻璃外观 |

属性入口的完整参数说明见 [View 组件 - glassEffectIOS](./view.md#glasseffectios)。

::::tip Compose 侧
Compose DSL 提供等价的 Modifier 入口：`Modifier.liquidGlass()` 与 `Modifier.liquidGlassContainer(spacing)`，详见 [Compose 核心能力 - 液态玻璃](../../Compose/kuikly-modifiers.md)。
::::

以下是各内置组件的启用示例：

### Switch 组件

```kotlin
Switch {
    attr {
        enableGlassEffect(true) // 启用液态玻璃效果
        isOn(true)
        onColor(Color.GREEN)
    }
}
```

### Slider 组件

```kotlin
Slider {
    attr {
        enableGlassEffect(true) // 启用液态玻璃效果
        currentProgress(0.5f)
    }
}
```

### SegmentedControl 组件

```kotlin
// iOS原生组件自动支持液态玻璃
SegmentedControlIOS {
    attr {
        titles(listOf("选项1", "选项2", "选项3"))
        selectedIndex(0)
    }
}
```

## 最佳实践

### 1. 使用原则

- 避免嵌套液态玻璃组件
- 仅在交互层使用液态玻璃效果

### 2. 背景设置

使用液态玻璃效果时，确保背景设置正确：

```kotlin
View {
    attr {
        glassEffectIOS()
        glassEffectTintColor(Color.RED) // ✅
        // backgroundColor(Color.RED) // ❌ 会覆盖玻璃效果
    }
}
```

### 3. 兼容性处理

推荐使用 `PlatformUtils.isLiquidGlassSupported()` 来检测当前设备是否支持液态玻璃效果，并始终提供降级方案：

```kotlin
View {
    attr {
        if (PlatformUtils.isLiquidGlassSupported()) {
            glassEffectIOS()
            backgroundColor(Color.TRANSPARENT)
        } else {
            backgroundColor(Color(0x80000000)) // 半透明黑色降级
        }
    }
}
```

## 相关组件

- [LiquidGlass组件](./liquid-glass.md) - 基础液态玻璃容器
- [GlassEffectContainer组件](./glass-effect-container.md) - 液态玻璃容器组合
- [Slider组件](./slider.md) - 支持液态玻璃效果的滑块组件（enableGlassEffect属性）
- [Switch组件](./switch.md) - 支持液态玻璃效果的开关组件（enableGlassEffect属性）
- [iOSSegmentedControl组件](./ios-segmented-control.md) - 支持液态玻璃的iOS原生分段控制器
- [View组件](./view.md) - 基础容器组件（支持glassEffectIOS属性）
- [Button组件](./button.md) - 按钮组件（支持glassEffectIOS属性）

## 示例代码

完整的使用示例可以参考：
[LiquidGlassDemoPage](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/LiquidGlassDemoPage.kt)
