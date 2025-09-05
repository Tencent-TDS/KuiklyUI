# iOS 26 液态玻璃效果

:::tip 系统要求
iOS 26.0+ 支持。在低版本系统上会自动降级到普通背景效果。
:::

iOS 26 引入了全新的液态玻璃（Liquid Glass）视觉效果，为用户界面带来了更加生动和现代的外观。Kuikly 框架对此特性提供了完整的支持，让开发者能够轻松为跨平台应用添加这一令人惊艳的视觉效果。

## 特性概述

液态玻璃效果具有以下特点：

- **玻璃质感**：实时动态地弯曲和汇聚光线，形成玻璃般的质感
- **动态响应**：能够响应用户交互，提供视觉反馈
- **系统原生**：使用iOS原生实现，确保最佳性能和系统一致性
- **自动降级**：在不支持的设备上自动降级为普通背景

## 支持检测

使用 `PlatformUtils.isLiquidGlassSupported()` 来检测当前设备是否支持液态玻璃效果：

```kotlin
if (PlatformUtils.isLiquidGlassSupported()) {
    // 使用液态玻璃效果
    attr {
        glassEffectIOS()
    }
} else {
    // 降级为普通背景
    attr {
        backgroundColor(Color.GRAY)
    }
}
```

## 基础使用方法

### 1. ViewContainer组件中使用

通过 `glassEffectIOS()` 属性为任意继承自ViewContainer的组件添加液态玻璃效果（包括View、Button、各种布局组件等）：

```kotlin
View {
    attr {
        size(300f, 60f)
        borderRadius(30f)
        glassEffectIOS() // 启用液态玻璃效果
        backgroundColor(Color.TRANSPARENT) // 必须设置为透明
    }
    Text {
        attr {
            text("液态玻璃效果")
            alignSelfCenter()
        }
    }
}
```

### 2. 使用专用组件

Kuikly 提供了专门的液态玻璃组件：

```kotlin
// 基础液态玻璃容器
LiquidGlass {
    attr {
        height(60f)
        borderRadius(30f)
        glassEffectInteractive(true)
    }
    Text {
        attr {
            text("专用组件")
            alignSelfCenter()
        }
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

## 样式配置

### Regular 样式（默认）

标准的液态玻璃效果，适用于大多数场景：

```kotlin
View {
    attr {
        glassEffectIOS() // 默认为Regular样式
        // 或者显式指定
        glassEffectIOS(style = GlassEffectStyle.REGULAR)
    }
}
```

### Clear 样式

更加透明的效果，适用于需要更强背景穿透的场景：

```kotlin
View {
    attr {
        glassEffectIOS(style = GlassEffectStyle.CLEAR)
    }
}
```

### 自定义颜色

可以为液态玻璃效果添加色彩倾向：

```kotlin
View {
    attr {
        glassEffectIOS(
            style = GlassEffectStyle.REGULAR,
            tintColor = Color.BLUE,
            interactive = true
        )
    }
}
```

## 交互支持

液态玻璃效果支持交互反馈：

```kotlin
Button {
    attr {
        glassEffectIOS(interactive = true) // 启用交互效果
        titleAttr {
            text("可交互按钮")
        }
    }
    event {
        click {
            // 点击时会有视觉反馈
        }
    }
}
```

## iOS 原生组件集成

Kuikly 提供了支持液态玻璃效果的iOS原生组件：

### Switch 组件

```kotlin
Switch {
    attr {
        enableGlassEffect(true) // 启用液态玻璃效果
        isOn(true)
        onColor(Color.GREEN)
    }
}

// 注意：iOS原生组件已集成到标准组件中
// 使用 enableGlassEffect 属性启用液态玻璃效果
```

### Slider 组件

```kotlin
Slider {
    attr {
        enableGlassEffect(true) // 启用液态玻璃效果
        currentProgress(0.5f)
    }
}

// 注意：iOS原生组件已集成到标准组件中
// 使用 enableGlassEffect 属性启用液态玻璃效果
```

### SegmentedControl 组件

```kotlin
SegmentedControlIOS {
    attr {
        titles(listOf("选项1", "选项2", "选项3"))
        selectedIndex(0)
        // iOS原生组件自动支持液态玻璃
    }
}
```

## 组合效果

使用 `GlassEffectContainer` 创建融合液态玻璃效果：

```kotlin
GlassEffectContainer {
    attr {
        spacing(10f) // 元素间距
        flexDirectionRow()
    }
    
    LiquidGlass {
        attr {
            flex(1f)
            height(60f)
            borderRadius(30f)
        }
        Text { 
            attr { 
                text("左侧") 
                alignSelfCenter()
            } 
        }
    }
    
    LiquidGlass {
        attr {
            flex(1f)
            height(60f)
            borderRadius(30f)
            glassEffectTintColor(Color.YELLOW)
        }
        Text { 
            attr { 
                text("右侧") 
                alignSelfCenter()
            } 
        }
    }
}
```

## 最佳实践

### 1. 背景设置

使用液态玻璃效果时，确保背景设置正确：

```kotlin
View {
    attr {
        glassEffectIOS()
        backgroundColor(Color.TRANSPARENT) // ✅ 推荐
        // backgroundColor(Color.RED) // ❌ 会覆盖玻璃效果
    }
}
```

### 2. 兼容性处理

始终提供降级方案：

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

### 3. 性能考虑

- 仅在交互层使用液态玻璃，避免在内容层使用
- 避免嵌套液态玻璃组件

## 适用范围说明

`glassEffectIOS` 和 `glassEffectContainerIOS` 属性可用于**所有继承自 `ViewContainer` 的组件**，包括但不限于：

- View组件
- Button组件  
- 其他容器类组件

这些属性在 `ViewContainer` 基类中实现，因此对所有子类组件都可用。

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
