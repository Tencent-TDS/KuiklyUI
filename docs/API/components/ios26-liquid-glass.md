# iOS 液态玻璃效果

:::tip 系统和版本要求

- **系统版本**：iOS 26.0+

- **Kuikly版本**：2.5.0+

:::

iOS 26 引入了全新的液态玻璃（Liquid Glass）视觉效果，为用户界面带来了更加生动和现代的外观。 Kuikly基于系统原生API适配了这一特性。

- **系统原生**：使用iOS原生实现，确保最佳性能与效果
- **自动降级**：在不支持的设备和系统版本上自动忽略或降级为普通容器

## 适用场景（Apple 官方指引）

以下引用自 Apple Human Interface Guidelines《Materials》与《Adopting Liquid Glass》：

> Liquid Glass forms a distinct functional layer for controls and navigation elements — like tab bars and sidebars — that floats above the content layer, establishing a clear visual hierarchy between functional elements and content.

> **Don't use Liquid Glass in the content layer.** Liquid Glass works best when it provides a clear distinction between interactive elements and content, and including it in the content layer can result in unnecessary complexity and a confusing visual hierarchy. Instead, use standard materials for elements in the content layer, such as app backgrounds.

> An exception to this is for controls in the content layer with a transient interactive element like sliders and toggles.

> **Use Liquid Glass effects sparingly.** … Limit these effects to the most important functional elements in your app.

据此，Kuikly 中玻璃的使用建议：

| 场景 | 是否适用 | 说明                                             |
|---|---|------------------------------------------------|
| 底部 TabBar、顶部导航栏、工具栏 | ✅ | 典型的「浮在内容之上的功能层」，内容从其下方滚过                       |
| 悬浮按钮、浮动操作条、浮层面板（侧边栏 / 弹层） | ✅ | 功能层元素；面板内可放列表，玻璃加在面板外层 View 上                  |
| Slider / Switch 等交互控件 | ✅ | 官方例外：交互时呈现玻璃外观（见下文 Switch / Slider）            |
| 列表项、卡片、页面背景等内容区元素 | ❌ | 属于内容层，应使用普通背景或半透明背景                            |
| `Scroller` / `List` 本身 | ❌ | 滚动区是内容层；提供的是 scroll edge effect 而非玻璃，可按列表用属性控制 |
| 大面积、多处同时使用 | ❌ | 官方要求克制使用，只用于最重要的功能元素                           |

另外，玻璃需要背后有可折射的内容才能看出质感：深色或纯色的大面积背景上，玻璃看起来接近纯色填充。

## 基础使用方法

### 1. 属性使用方式

通过 `glassEffectIOS()` 和 `glassEffectContainerIOS()` 属性为 `View` 及基于 View 的容器组件（如 Button）添加液态玻璃效果，`Scroller` / `List` 等滚动组件不支持（见下文「支持的组件范围」）：

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

```

> **交互态（按压反馈）的平台要求**：`glassEffectInteractive(true)` 与 `glassEffectIOS(interactive = true)` 在 iOS 26+ 生效；macOS 上对应的系统能力 `NSGlassEffectView.effectIsInteractive` 从 **macOS 27** 才提供，且渲染器需用 **macOS 27 SDK（Xcode 27+）** 编译，macOS 26 上玻璃没有按压态。

```kotlin
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
Compose DSL 提供等价的 Modifier 入口：`Modifier.liquidGlass()` 与 `Modifier.liquidGlassContainer(spacing)`，详见 [Kuikly 扩展 Modifier - 液态玻璃](../../Compose/kuikly-modifiers.md)。
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

### TabBar（原生 UITabBar 扩展组件）

iOS 26 起系统 `UITabBar` 自带液态玻璃效果，无需设置任何玻璃属性。Kuikly **核心库未内置** TabBar 组件，Demo 中提供了一个封装原生 `UITabBar` 的扩展组件 `TabbarIOS`，业务可直接拷贝使用：

| 端 | 文件 | 说明 |
|---|---|---|
| Kotlin | `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/base/iOSTabbar.kt` | 声明 `TabbarIOS` 组件（`viewName = "KRTabbarView"`）及其属性、事件 |
| iOS | `iosApp/iosApp/KuiklyRenderExpand/Views/KRTabbarView.h/.m` | 原生实现，内部持有 `UITabBar`；放入宿主工程即可，Kuikly 按类名自动创建 |

**使用方式**

```kotlin
Scroller {
    attr { flex(1f) }
    // 页面内容：需从 TabBar 下方滚过，玻璃才能折射出内容
}
TabbarIOS {
    ref { ctx.tabbarRef = it }
    attr {
        absolutePosition(left = 0f, right = 0f, bottom = 0f) // 浮在内容之上
        height(80f)
        items(
            listOf(
                TabbarIOSItem("首页", homeIcon, homeSelectedIcon),
                TabbarIOSItem("我的", profileIcon, profileSelectedIcon)
            )
        )
        selectedIndex(0)
    }
    event {
        onTabSelected { params ->
            // params.index：选中的 tab 下标
        }
    }
}

// 设置角标，传空字符串移除
ctx.tabbarRef?.view?.setBadge(index = 1, badge = "99+")
```

**生效条件**

- 设备系统为 iOS 26+，且**宿主 App** 使用 Xcode 26+（iOS 26 SDK）编译。TabBar 的玻璃由 UIKit 自动提供，与 Kuikly 渲染器的编译 SDK 无关。
- 宿主 App 的 Info.plist 未设置 `UIDesignRequiresCompatibility = YES`（该开关会让系统组件保持旧版外观）。
- 不要给 `TabbarIOS` 设置 `backgroundColor`，否则会遮挡系统玻璃。
- 图标需为 Kuikly 本地资源路径，原生侧从 App Bundle 加载并缩放到 25×25。
- 该组件只在 iOS 上可用，其他平台需要自行提供对应实现，或用普通 View 降级。

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

### 4. 支持的组件范围

| 组件 | `glassEffectIOS()` | `glassEffectContainerIOS()` |
|---|---|---|
| `View` 及基于 View 的容器（Button 等） | ✅ | ✅ |
| `Scroller` / `List` / `PageList` 等滚动组件 | ❌ | ❌ |
| 其他非 View 容器 | ❌ | ❌ |

在不支持的组件上调用，编译能通过但没有效果，DEBUG 下控制台会输出 `[Kuikly] glassEffectXxx is not supported on ...` 告警。滚动区属于 Apple 规范中的内容层，不应使用玻璃；如果浮层面板里需要列表，把玻璃加在面板外层 `View` 上，列表作为子视图放在里面：

```kotlin
View {
    attr {
        borderRadius(16f)
        glassEffectIOS()      // 玻璃加在浮层面板上
    }
    List {
        attr { flex(1f) }     // 列表在玻璃内部滚动
        // 列表内容
    }
}
```

### 5. 圆角

在玻璃组件上直接设置 `borderRadius` 即可。玻璃外壳、子内容裁剪和 `border` 描边保持同一形状，支持四角独立半径和动态修改，不需要额外代码：

```kotlin
// 统一圆角：半径取高度的一半即为胶囊形
View {
    attr {
        size(300f, 62f)
        borderRadius(31f)
        glassEffectIOS()
    }
}

// 四角独立圆角（单个角的半径不要超过短边的一半，超出部分会被压缩）
View {
    attr {
        size(300f, 62f)
        borderRadius(topLeft = 28f, topRight = 6f, bottomLeft = 6f, bottomRight = 28f)
        glassEffectIOS()
    }
}

// 动态修改：响应式变量变化后，玻璃形状和描边同步更新
View {
    attr {
        size(300f, 62f)
        borderRadius(if (ctx.expanded) 31f else 0f)
        glassEffectIOS()
    }
}
```

`LiquidGlass` 控件的用法相同。各平台支持情况：

- **iOS**：玻璃仅在 iOS 26+ 存在，圆角通过 `UICornerConfiguration` 设置，四角独立半径完整支持。
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

macOS 26 上玻璃统一取左上角半径，而子内容裁剪与 `border` 描边仍按四个角各自的半径绘制，两者轮廓不重合（`View + glassEffectIOS()` 与 `LiquidGlass` 控件行为一致）。以 62 高的条为例：

- 某个角的半径**小于**左上角（如 `borderRadius(31f, 10f, 31f, 10f)` 的右侧两角）：玻璃在该角是 31 的大圆角，而子内容与描边是 10 的小圆角，内容和描边的角会**探出玻璃轮廓**，看起来像玻璃没包住内容。
- 某个角的半径**大于**左上角（如 `borderRadius(10f, 31f, 10f, 31f)` 的右侧两角）：玻璃在该角只有 10 的小圆角，内容与描边是 31 的大圆角，玻璃的**方角露在内容圆角之外**，与"玻璃外壳直角、四角露出尖角"的问题现象相同。

因此需要四角不同圆角的业务，请将 macOS 最低运行版本提升到 27；若必须支持 macOS 26，请对 26 使用四角一致的圆角（可用 `PlatformUtils.getOSVersion()` 区分）。

### 6. 与 boxShadow 互斥

系统玻璃自带阴影，**玻璃生效时框架会忽略 `boxShadow`**，开启玻璃的节点上不需要设置 `boxShadow`：

| 场景 | boxShadow |
|---|---|
| iOS / macOS 26+，`glassEffectIOS()`（enable = true）或 `LiquidGlass` 控件 | 被忽略（不下发到原生） |
| `glassEffectIOS(enable = false)`、`glassEffectContainerIOS()` | 正常生效 |
| iOS / macOS 26 以下、Android / 鸿蒙 / H5 等玻璃不生效的环境 | 正常生效 |

- 动态开关玻璃时框架会自动处理：开启玻璃时清除已生效的阴影，关闭玻璃时按当前圆角恢复 `boxShadow`。

## 相关组件

- [LiquidGlass组件](./liquid-glass.md) - 基础液态玻璃容器
- [GlassEffectContainer组件](./glass-effect-container.md) - 液态玻璃容器组合
- [Slider组件](./slider.md) - 支持液态玻璃效果的滑块组件（enableGlassEffect属性）
- [Switch组件](./switch.md) - 支持液态玻璃效果的开关组件（enableGlassEffect属性）
- [iOSSegmentedControl组件](./ios-segmented-control.md) - 支持液态玻璃的iOS原生分段控制器
- [View组件](./view.md) - 基础容器组件（支持glassEffectIOS属性）
- [Button组件](./button.md) - 按钮组件（支持glassEffectIOS属性）
- [Scroller 组件](./scroller.md) - `hideTopScrollEdgeEffect` / `hideBottomScrollEdgeEffect`

## 示例代码

完整的使用示例可以参考：
[LiquidGlassDemoPage](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/LiquidGlassDemoPage.kt)
