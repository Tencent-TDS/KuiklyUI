# ImmersiveLightSenseOhos(沉浸光感标题栏容器)

:::tip 系统和版本要求

- **系统版本**：HarmonyOS 6.1.0+

- **Kuikly版本**：2.6.0+

:::

基于官方 HdsNavigation 的沉浸光感标题栏容器。**标题栏内容为 Kuikly 子视图**（经 ContentSlot 挂入 HdsNavigation 标题栏的 stackBuilder 槽，落在系统规定的材质生效区域内），布局完全由业务自绘。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ImmersiveLightSenseOhosDemoPage.kt)

## 基础用法

```kotlin
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseOhos

ImmersiveLightSenseOhos {
    attr {
        titleBarHeight(44f)
        // 容器级材质：整个标题栏区域一层玻璃底
        immersiveMaterial(style = ImmersiveStyle.REGULAR)
    }
    // 子视图即标题栏内容（Kuikly 自绘，任意布局）
    Text { attr { text("标题") } }
}
```

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)（布局/定位/背景等照常），还支持以下专有属性：

### titleBarHeight

标题栏高度（vp）。必须提供，内容布局依赖该值。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| height | 标题栏高度 | Float | 0 |

### contentBackgroundColor

标题栏内容层（ContentSlot 承载层）背景色。默认完全透明（白底会挡住页面背景、毁掉材质折射观感）。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| color | 内容层背景色 | Color | 透明 |

### immersiveMaterial

标题栏区域沉浸光感材质，参数见[总览](immersive-light-sense.md#immersivematerial)。注意材质**设在标题栏内承载子视图的层上**（不在容器节点上），表现为"整个标题栏区域一层玻璃底"；子视图可再各自调用覆盖。

## 注意事项

- 标题栏自动避让安全区（位于状态栏之下）；
- 返回按钮默认隐藏（内容全自绘，业务需要返回键请用 [ImmersiveLightSenseNavigationOhos](immersive-light-sense-navigation.md)）；
- 材质背后需要有页面内容（背景图等），纯色背景上材质不可见。
