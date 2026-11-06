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

### 3. 圆角

直接设置 `borderRadius` 即可，玻璃外壳、子内容裁剪和 `border` 描边保持同一形状，支持四角独立半径和动态修改：

```kotlin
// 统一圆角：半径取高度的一半即为胶囊形
LiquidGlass {
    attr {
        height(60f)
        borderRadius(30f)
    }
}

// 四角独立圆角（单个角的半径不要超过短边的一半）
LiquidGlass {
    attr {
        height(60f)
        borderRadius(topLeft = 28f, topRight = 6f, bottomLeft = 6f, bottomRight = 28f)
    }
}
```

各平台支持情况：

- **iOS**：玻璃仅在 iOS 26+ 存在，圆角通过 `UICornerConfiguration` 设置，四角独立半径完整支持；子内容按同一形状裁剪。
- **macOS**：四角独立半径需要 **macOS 27+**，macOS 26 上只能统一取左上角的值，详见下方「macOS 四角圆角的版本要求」。

#### macOS 四角圆角的版本要求

**为什么需要 macOS 27**

- 玻璃的形状只由 `NSGlassEffectView` 自身决定。Kuikly 普通 View 表达四角不同圆角用的是 `CAShapeLayer` + `layer.mask`，而 mask 对玻璃无效，无法用来"裁"出四角不同的玻璃。
- macOS 26 的 `NSGlassEffectView` 只提供单一的 `cornerRadius` 属性，系统层面没有表达四角不同半径的能力。
- macOS 27 起 AppKit 新增 `NSViewCornerConfiguration`，`NSGlassEffectView` 会按它的四个角分别生成玻璃形状。Kuikly 在 macOS 27+ 上自动走该能力，业务代码无需改动。
- 该能力同时依赖**编译期**与**运行期**：渲染器必须用 **macOS 27 SDK（Xcode 27+）** 编译，否则相关代码会在预处理阶段被剔除，即使运行在 macOS 27 上也只会按 macOS 26 的方式处理。

| 运行系统 | 四角一致 | 四角不一致 |
|---|---|---|
| macOS 27+（且用 macOS 27 SDK 编译） | ✅ | ✅ 各角按各自半径 |
| macOS 26 | ✅ | ⚠️ 玻璃统一取左上角半径，见下文异常 |

**停留在 macOS 26 时设置四角不同圆角的异常表现**

macOS 26 上玻璃统一取左上角半径，但子内容裁剪与 `border` 描边仍按四个角各自的半径绘制，两者轮廓不重合。以 62 高的条为例：

- **`View + glassEffectIOS()`**
  - 某个角的半径**小于**左上角（如 `borderRadius(31f, 10f, 31f, 10f)` 的右侧两角）：玻璃在该角是 31 的大圆角，而子内容与描边是 10 的小圆角，内容和描边的角会**探出玻璃轮廓**，看起来像玻璃没包住内容。
  - 某个角的半径**大于**左上角（如 `borderRadius(10f, 31f, 10f, 31f)` 的右侧两角）：玻璃在该角只有 10 的小圆角，内容与描边是 31 的大圆角，玻璃的**方角露在内容圆角之外**，与"玻璃外壳直角、四角露出尖角"的问题现象相同。
- **`LiquidGlass` 控件**：玻璃四角统一为左上角半径，`border` 描边仍按四个角各自的半径绘制，**描边与玻璃边缘错位**。

因此需要四角不同圆角的业务，请将 macOS 最低运行版本提升到 27；若必须支持 macOS 26，请对 26 使用四角一致的圆角（可用 `PlatformUtils.getOSVersion()` 区分）。

### 4. boxShadow

系统玻璃自带阴影。iOS / macOS 26+ 上 `LiquidGlass` 的 `boxShadow` 会被框架忽略；26 以下系统玻璃不生效时，`boxShadow` 正常生效。详见 [iOS 液态玻璃效果 · 与 boxShadow 互斥](./ios26-liquid-glass.md)。

### 5. 适用场景

玻璃只用于浮在内容之上的功能层（TabBar、导航栏、工具栏、悬浮按钮、浮层面板），不要用在列表项、卡片、页面背景等内容层元素上。详见 [iOS 液态玻璃效果 · 适用场景](./ios26-liquid-glass.md)。

## 相关组件

- [GlassEffectContainer](./glass-effect-container.md) - 液态玻璃容器组合
- [View](./view.md#glasseffectios) - 基础容器（支持glassEffectIOS属性）
- [iOS 液态玻璃概述](./ios26-liquid-glass.md) - 完整使用指南
