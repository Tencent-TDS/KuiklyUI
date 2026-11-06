# iOS 系统栏（UINavigationBar / UIToolbar）

:::tip 版本与运行环境

| 项 | 要求 |
|---|---|
| **KuiklyUI** | **2.30.0+**（含 `UINavigationBar` / `UIToolbar` Kotlin API 与 iOS 侧 `KRUINavigationBar` / `KRUIToolbar` 实现） |
| **平台** | 仅 **iOS** 挂载系统栏；其它端 DSL 为 **no-op**（不创建节点、不报错）。框架内用 `PlatformUtils.isIOS()` 判断，**无**对外 `PlatformUtils.isUIKitChromeSupported()` API |
| **宿主** | 页面在 **`UINavigationController`** 栈中；链接 `core-render-ios/Extension/AdvancedComps`。Demo 宿主只做两件事：普通页 `fd_prefersNavigationBarHidden=YES`（不露出空导航栏），系统栏页（`UINavigationBarDemo` 或 `pageData.iosUIKitNavigationChrome=true`）设 `fd_prefersNavigationBarHidden=NO` 并清空 `UINavigationItem`；显隐与栏内容由 **FDFullscreenPop + KRUINavigationBar/KRUIToolbar** 负责，宿主勿再反复 `setNavigationBarHidden` |

:::

`UINavigationBar` 与 `UIToolbar` 用于把 **UIKit 导航栏 / 工具栏** 接到 Kuikly 页面：业务在 Kotlin 里配置标题、SF Symbol、按钮与点击事件，由系统根据机型与姿态绘制栏的位置与样式。**不是在页面里再画一套自定义 NavBar。**

Kotlin 树上栏节点与正文为**兄弟节点**；栏在布局上不占 flex 占位（`absolutePosition` + 零尺寸），正文需自行预留顶/底安全距离（见下文「内容区布局」）。

## 与液态玻璃（Liquid Glass）的关系

系统栏属于 Apple HIG 中的 **功能层**（浮在内容之上），与 [iOS 液态玻璃效果](./ios26-liquid-glass.md) 的定位一致：

| 能力 | 系统栏（本页） | 内容层 `glassEffectIOS()` / `LiquidGlass` |
|---|---|---|
| 适用层级 | 顶部导航、底部工具栏 | 悬浮按钮、浮层面板等；**不要**给列表/正文大面积铺玻璃 |
| iOS 26+ 外观 | 由 **UIKit `UINavigationBar` / `UIToolbar`** 自动呈现液态玻璃（需宿主用 **iOS 26 SDK（Xcode 26+）** 编译，且未开启 `UIDesignRequiresCompatibility`） | 由 Kuikly 玻璃 API 显式开启 |
| 不透明白底 | 使用 `barBackground(Opaque)` + `barBackgroundColor`（导航栏）；勿指望只设颜色而不设 `Opaque` | 玻璃场景用透明背景 + `glassEffectIOS()` |
| 内容滚过栏下 | 推荐 `Scroller`/`List` 占满屏，内容从栏下穿过；可配 `contentScroll`、滚动边缘效果 | 滚动区本身不用玻璃；栏下需要的是 **scroll edge effect**，不是内容层玻璃 |

:::info 液态玻璃版本（与专题文档一致）

- **系统**：iOS **26.0+**（macOS 26+ 上另有 TabBar 等场景，系统栏组件当前仅 iOS）
- **Kuikly 玻璃属性**：2.5.0+ 见 [ios26-liquid-glass.md](./ios26-liquid-glass.md)
- **系统栏玻璃**：无需在 `UINavigationBar` / `UIToolbar` 上调用 `glassEffectIOS()`；外观跟随系统栏材质

:::

## UINavigationBar

**每页至多一个** `UINavigationBar`。

### 基础结构

```kotlin
import com.tencent.kuikly.core.views.ios.UINavigationBar
import com.tencent.kuikly.core.views.ios.UIBarButtonItemPlacement
import com.tencent.kuikly.core.views.ios.UIBarAxis
import com.tencent.kuikly.core.views.ios.UILargeTitleDisplayMode

UINavigationBar {
    attr {
        tintColor(Color.BLACK)
        // 需要实心白底时务必 Opaque，仅 barBackgroundColor 在 iPhone 上可能无效
        barBackground(UINavigationBarBackgroundStyle.Opaque)
        barScrollEdgeBackground(UINavigationBarBackgroundStyle.Opaque)
        barBackgroundColor(Color.WHITE)
        barScrollEdgeBackgroundColor(Color.WHITE)
    }
    title {
        text {
            attr {
                title("标题")
                largeTitle(UILargeTitleDisplayMode.Never)
            }
        }
    }
    items {
        navbarItems {
            navbarItem {
                attr {
                    symbol("chevron.backward")
                    placement(UIBarButtonItemPlacement.Leading)
                    index(0)
                    axis(UIBarAxis.VerticalPreferred) // iOS 27+ Duo 侧栏等
                }
                event {
                    click { /* 返回 */ }
                }
            }
            navbarItem {
                attr {
                    symbol("square.and.arrow.up")
                    placement(UIBarButtonItemPlacement.Trailing)
                    index(0)
                }
                event {
                    click { }
                }
            }
        }
    }
}
```

### 栏级属性（`attr`）

| 属性 | 说明 |
|---|---|
| `tintColor` | 按钮与强调色 |
| `visibility` | 是否显示导航栏 |
| `leadingInset` / `trailingInset` | 按钮组与栏边缘额外间距（pt），折叠屏横屏避让遮挡时可增大 |
| `contentScroll` / `contentScrollNativeRef` | 绑定主滚动 `ViewRef`，用于栏与滚动联动（minimization 等） |
| `barBackground` / `barScrollEdgeBackground` | `Default` / `Opaque` / `Transparent` |
| `barBackgroundColor` / `barScrollEdgeBackgroundColor` | 与 appearance 配合的背景色 |
| `barShadowColor` | 底部分隔线/阴影色，可与背景同色以减弱缝 |
| `barStyle` / `barTranslucent` | 对应 UIKit `UIBarStyle` / 半透明 |
| `hideScrollEdgeEffects` | 是否隐藏滚动到边缘时的系统效果 |
| `navigationBarMinimizationBehavior` | **iOS 27+**，`Automatic` / `Never` / `OnScrollDown` / `OnScrollUp` |
| `navigationBarMinimizationSafeAreaAdjustment` | **iOS 27+**，是否由系统自动调整内容 safe area |
| `navigationBarMinimizationRestoration` | **iOS 27+**，栏恢复行为 |

### 标题

- `title { text { attr { … } } }`：系统字符串标题（`title`、`largeTitle`、`titleFontSize`、`titleColor` 等）。
- `title { titleContent { … } }`：自定义标题区域（Kuikly 子树挂到 `titleView`）。

多套标题或按钮请用 **`vif` / `velse` 整段切换** `navbarItems` 或 `title`，避免只改单个属性导致旧状态残留。

### 按钮

- `navbarItem`：`symbol` / `src` / `title`、`placement`（`Leading` / `Trailing`）、`index`、`itemGroup`、`axis`、`tintColor`、`visibility`。
- `navbarItemContent`：自定义 `UIBarButtonItem.customView` 区域。

## UIToolbar

**每页至多一个** `UIToolbar`。写法与导航栏对称：`attr` + `items`。

```kotlin
import com.tencent.kuikly.core.views.ios.UIToolbar
import com.tencent.kuikly.core.views.ios.UIToolbarItemKind

UIToolbar {
    attr {
        tintColor(Color.BLACK)
        leadingInset(0f)
        trailingInset(0f)
    }
    items {
        toolbarItem {
            attr {
                kind(UIToolbarItemKind.FlexibleSpace)
                index(0)
            }
        }
        toolbarItem {
            attr {
                kind(UIToolbarItemKind.Item)
                symbol("trash")
                index(1)
                axis(UIBarAxis.VerticalPreferred)
            }
            event {
                click { }
            }
        }
        toolbarItem {
            attr {
                kind(UIToolbarItemKind.Item)
                symbol("square.and.pencil")
                index(2)
            }
            event {
                click { }
            }
        }
    }
}
```

### 工具栏项 `kind`

| `UIToolbarItemKind` | 说明 |
|---|---|
| `Item` | 普通按钮（symbol / src / title） |
| `FlexibleSpace` | 弹性空白 |
| `FixedSpace` | 固定宽度空白（配合 `spaceWidth`） |

`toolbarItemContent` 可挂载自定义按钮内容，属性与 `navbarItem` 类似（`index`、`visibility`、`axis` 等）。

## 内容区布局

系统栏不替页面预留 flex 高度，正文需自己处理：

1. **顶栏**：正文顶距一般为 `pageData.statusBarHeight + 44`（与 UIKit 导航栏占位一致）；**不要**在外层容器与 `Scroller` 内重复叠两层相同顶距。折叠屏 Duo 侧置系统栏时，以 `pageData.hingeStatus`、`safeAreaInsets` 为准（见 [page-data.md](../../DevGuide/page-data.md)）。
2. **底栏**：有 `UIToolbar` 时，滚动区 `paddingBottom` 需包含 **Home Indicator**（`safeAreaInsets.bottom`）及工具栏高度（约 49pt）。
3. **左右**：宽屏或 Duo 用 `safeAreaInsets.left/right` 做 `padding`，再在中间 `maxWidth` + `alignSelfCenter()` 做阅读限宽。

示例页：`UIChromeReaderPage`（`UIChromeReader`）、`FoldChrome` / `FoldAppNav`（折叠屏配图）。

## 折叠屏与 iOS 27+

- **`leadingInset` / `trailingInset`**：横屏遇到 `reservedRegions` 遮挡时，让按钮避开铰链/摄像头区域。
- **`UIBarAxis.VerticalPreferred`**：iOS **27.1+** 竖向边栏场景下，系统更倾向竖排按钮组（需系统 SDK 支持）。
- **`hingeStatus` / `reservedRegions`**：KuiklyUI **2.29.0+** 起在 iPhone Duo 等设备上提供；与系统栏 API **2.30.0+** 配合使用。

## 示例与源码

| 说明 | 路由 / 文件 |
|---|---|
| 全量开关与 minimization、contentScroll | `UINavigationBarDemo` → `UINavigationBarDemoPage.kt` |
| 阅读器：导航 + 底栏 + 限宽正文 | `UIChromeReader` / `UIChromeChannel` / `UIChromeAlbum` |
| 折叠屏：通用 Nav 封装 | `demo/.../foldguide/FoldChrome.kt` |
| iOS 原生实现 | `core-render-ios/Extension/AdvancedComps/UINavigationBar/`、`.../UIToolbar/` |

## 相关文档

- [page-data.md](../../DevGuide/page-data.md) — `safeAreaInsets`、`hingeStatus`、`reservedRegions`
