# 沉浸光感（Immersive Light Sense）支持总览

:::tip 系统和版本要求

- **系统版本**：HarmonyOS 6.1.0+（HDS 组件沉浸光感能力自 6.1.0(23) 起提供）

- **设备要求**：沉浸光感为系统材质能力，效果随设备算力自适应（`uiMaterial.isImmersiveMaterialSupported()` 可探测）

- **Kuikly 版本**：2.6.0+

:::

「沉浸光感」是鸿蒙的系统级材质能力（系统材质/点光源/交互形变），**由系统按"生效区域 + 组件白名单"渲染**。Kuikly 以一组原生容器与控件组件接入该能力，业务从 Kuikly attr 出发即可使用，无需感知 ArkTS 细节。

## Kuikly 的支持程度

| 组件 | 底座 | 沉浸光感 | 形态可控度 |
| -- | -- | -- | -- |
| [ImmersiveLightSenseOhos](immersive-light-sense-titlebar.md) 标题栏容器 | HdsNavigation + ContentSlot | ✅ | 标题栏内容为 Kuikly 子视图，完全自绘 |
| [ImmersiveLightSenseNavigationOhos](immersive-light-sense-navigation.md) | HdsNavigation | ✅ | 结构化配置（标题/副标题/菜单/返回键），布局交 HDS 规范 |
| ImmersiveLightSenseNavigationSelfDrawnOhos（原生验证·实验） | 原生 Navigation | ✅（API 26 `systemMaterial`/`scrollEffectOptions` 直连） | 官方示例 20 同款路径，与 HdsNavigation 版对照 |
| [ImmersiveLightSenseTabBarOhos](immersive-light-sense-tabbar.md) 标准版 / 自绘版 | HdsTabs / 原生 Tabs + 自绘 | ✅ 官方光效 / ✅ 节点材质 | 官方光效胶囊固定；自绘版圆角/高度/底色全可控（实验路线） |
| [Slider / Switch / Select](immersive-light-sense-controls.md) | CAPI / ArkTS 转发 | ✅ 官方白名单控件 | 属性面对齐 iOS 液态玻璃 |
| [ImmersiveLightSenseDialogOhos](immersive-light-sense-dialog.md) | ArkTS Modal | ✅ | 背板尺寸/圆角/底色可控 |

## 用户使用沉浸光感的三种方式

### 方式一：专用原生组件（推荐）

直接使用上表中的组件。沉浸光感经 `immersiveMaterial()` 属性开启：

```kotlin
ImmersiveLightSenseTabBarOhos {
    attr {
        items(listOf("首页", "发现", "我的"))
        itemIcons(listOf("house", "star", "person"))
        barInsetX(16f)
        immersiveMaterial(style = ImmersiveStyle.REGULAR)   // ← 沉浸光感开关
    }
    event { tabChanged { index -> } }
}
```

#### immersiveMaterial()

`immersiveMaterial()` 是所有沉浸光感组件统一的材质入口，完整参数如下：

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| enable | 材质总开关；`false` 回退普通组件样式 | Boolean | true |
| style | 材质厚薄档位，取值见下方 [ImmersiveStyle 常量](#immersivestyle-常量) | Int | `ImmersiveStyle.REGULAR` |
| interactive | 按压时材质产生弹性形变 | Boolean? | null |
| applyShadow | 使用材质自带阴影（true 时优先于 shadow 通用属性） | Boolean? | null |
| materialColor | 材质层赋色，**必须带透明度**，不透明色会盖死材质 | Color? | null |
| lightEffectColor | 点光源/触点光效颜色（TRANSPARENT 为禁用） | Color? | null |

##### ImmersiveStyle 常量

取值与官方 `uiMaterial.ImmersiveStyle` 一一对应：

| 常量 | 值 | 观感 |
| -- | -- | -- |
| `ImmersiveStyle.ULTRA_THIN` | 0 | 极薄：极强透明感 |
| `ImmersiveStyle.THIN` | 1 | 薄：强透明感 |
| `ImmersiveStyle.REGULAR` | 2 | 标准：常规厚度 |
| `ImmersiveStyle.THICK` | 3 | 厚 |
| `ImmersiveStyle.ULTRA_THICK` | 4 | 极厚 |

### 方式二：Kuikly 自绘标题栏 + 材质开关

不使用原生标题栏组件时，可将自绘标题栏放入 `ImmersiveLightSenseOhos` 容器——子视图经 ContentSlot 挂入 HdsNavigation 标题栏（材质生效区域），照常自绘布局，容器级材质一层生效：

```kotlin
ImmersiveLightSenseOhos {
    attr {
        titleBarHeight(44f)
        immersiveMaterial(style = ImmersiveStyle.REGULAR, materialColor = Color(0x33FFFFFF))
    }
    // Kuikly 子视图 = 标题栏内容，任意布局
    Text { attr { text("自绘标题栏") } }
}
```

### 方式三：能力探测 + 降级

设备不支持或系统关闭材质时组件自动降级为普通样式（不崩页面、只丢材质），业务可主动探测：

```kotlin
// 设备能力（只读）
if (PlatformUtils.isImmersiveMaterialSupported()) { /* 走沉浸光感 UI */ }

// 应用级材质状态基线（MaterialState，系统/用户控制，应用只读）
when (PlatformUtils.getImmersiveMaterialState()) {
    PlatformUtils.ImmersiveMaterialState.ENABLE ->   // 浮层默认带材质，无需逐个设置
    PlatformUtils.ImmersiveMaterialState.DISABLE ->  // 所有材质不生效，回退普通样式
    PlatformUtils.ImmersiveMaterialState.DEFAULT ->  // 仅 Dialog/Toast 等系统浮层默认开
}
```

## 自定义 view 能否使用沉浸光感？

**不能直接使用。** 沉浸光感由系统按白名单区域渲染（Navigation 标题栏、底部 TabBar、Dialog、Slider、Toggle、Select 等），任意 Kuikly 自绘 view 上设置材质**不在生效区域内，不会渲染出官方沉浸光感**。可行做法只有两条：

1. 把自绘内容放进**生效区域**内——即方式二：自绘标题栏内容放入标题栏容器，材质落在系统规定的标题栏 scope；
2. 使用普通节点的 `systemMaterial`（`uiMaterial.ImmersiveMaterial`，如自绘 TabBar 所用）——它能出材质（厚薄档位、点光源、按压形变），**但走的是普通节点材质管线，光效观感弱于 HDS 官方管线**，且是实验性路线。

## 注意事项

0. **鸿蒙专属能力**：沉浸光感为 HarmonyOS 6.1+ 的系统材质能力，**iOS / Android 端不支持**——组件代码可跨端编译，但其他端运行时降级为空视图、无任何光效。iOS 端的对应能力为[液态玻璃](liquid-glass.md)（iOS 26+）。
1. **材质需要背后有内容**：玻璃折射采样的是渲染层背后的内容。材质区域背后若是纯色（尤其纯白），看不出任何效果——页面应有背景图或丰富内容。
2. **开关必须无条件下发**：`if (enable) immersiveMaterial(...)` 的写法会导致"关"状态不下发任何属性，原生侧永远停在开——应使用 `immersiveMaterial(enable = state, ...)`。
3. **全屏挂载与透明底**：原生转发组件全屏挂载时注意透明白底问题（TabBar 需 `contentTransparent(true)`；HdsNavigation 容器各层默认已透明）。
4. **图标受编译期字面量约束**：ArkTS 的 `$r()` 无法从业务传入的任意字符串构造 Resource，item/menu 图标走原生侧符号映射表（`sys.symbol` 名，不带前缀），表外名称回退默认图标。
5. **两条材质管线的取舍**（TabBar 场景）：HdsTabs 版官方光效最强但胶囊外形固定；自绘版外形任意（直角平底/圆角）但光效为普通节点材质。**系统层面二者不可兼得**（官方材质层不参与普通节点裁剪）。
6. **HdsTabs/HdsNavigation 圆角不可设置**：SDK 全部接口无圆角字段，胶囊外形为 HDS 系统规格；需要任意外形请走自绘路线。
7. **`enable` 之外无"反色"能力**：系统材质不自动反色，深色底上业务需自选内容文字颜色。
