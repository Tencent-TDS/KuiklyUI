# ImmersiveLightSenseNavigationOhos(沉浸光感 HdsNavigation)

:::tip 系统和版本要求

- **系统版本**：HarmonyOS 6.1.0+

- **Kuikly版本**：2.6.0+

:::

官方 HdsNavigation 组件的 Kuikly 封装，**结构化配置**模式：标题/副标题/菜单/返回键由属性下发，**布局与排版完全交给 HdsNavigation 内置规范**，业务不管。支持标题栏滚动渐变模糊（沉浸光感核心视效之一）。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ImmersiveLightSenseNavigationDemoPage.kt)

## 基础用法

```kotlin
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseNavigationOhos

ImmersiveLightSenseNavigationOhos {
    attr {
        absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)  // 全屏
        title("主标题")
        subTitle("副标题")
        menuIcons(listOf("square_and_pencil", "star", "more"))
        menuLabels(listOf("编辑", "收藏", "更多"))
        titleMode(HdsNavigationTitleMode.MINI)
        enableScrollEffect(true)            // 滚动联动
        scrollEffectType(HdsScrollEffectType.GRADIENT_BLUR) // 渐变模糊
        immersiveMaterial(style = ImmersiveStyle.REGULAR)        // 标题栏按钮材质
    }
    event {
        onMenuClick { index -> /* 菜单点击 */ }
    }
}
```

## 属性

| 属性 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| title | 主标题文字 | String | "标题" |
| subTitle | 副标题 | String | "" |
| menuIcons | 菜单图标（sys.symbol 符号名，与 menuLabels 同下标） | List\<String\> | 空 |
| menuLabels | 菜单文字（与 menuIcons 同下标） | List\<String\> | 空 |
| titleMode | 标题模式，取值见下方常量 | Int | `HdsNavigationTitleMode.MINI` |
| hideBackButton | 是否隐藏返回按钮 | Boolean | false |
| enableScrollEffect | 滚动联动开关（内容上滚时标题栏模糊/渐变） | Boolean | false |
| scrollEffectType | 滚动效果类型，取值见下方常量 | Int | `HdsScrollEffectType.GRADIENT_BLUR` |
| immersiveMaterial | 标题栏按钮沉浸光感材质 | 见[总览](immersive-light-sense.md#immersivematerial) | 关 |

### titleMode 常量（HdsNavigationTitleMode）

取值与官方 `HdsNavigationTitleMode` 一一对应：

| 常量 | 值 | 说明 |
| -- | -- | -- |
| `HdsNavigationTitleMode.FREE` | 0 | 自由模式：标题随滚动联动缩放 |
| `HdsNavigationTitleMode.FULL` | 1 | 全屏模式：大标题 |
| `HdsNavigationTitleMode.MINI` | 2 | 迷你模式：常规单行标题（默认） |

### titleMode 常量（HdsNavigationTitleMode）

取值与官方 `HdsNavigationTitleMode` 一一对应：

| 常量 | 值 | 说明 |
| -- | -- | -- |
| `HdsNavigationTitleMode.FREE` | 0 | 自由模式：标题随滚动联动缩放 |
| `HdsNavigationTitleMode.FULL` | 1 | 全屏模式：大标题 |
| `HdsNavigationTitleMode.MINI` | 2 | 迷你模式：常规单行标题（默认） |
| `HdsNavigationTitleMode.MODAL` | 3 | 模态模式：模态页面标题 |

### scrollEffectType 常量（HdsScrollEffectType）

取值与官方 `ScrollEffectType` 一一对应：

| 常量 | 值 | 说明 |
| -- | -- | -- |
| `HdsScrollEffectType.COMMON_BLUR` | 0 | 普通模糊：背景均匀模糊，随透明渐变显隐 |
| `HdsScrollEffectType.GRADUAL_BLUR` | 1 | 渐进模糊：均匀模糊且边界清晰，内容随滚动线性变化 |
| `HdsScrollEffectType.GRADIENT_BLUR` | 2 | 渐变模糊：渐变模糊样式 |
| `HdsScrollEffectType.IMMERSIVE_GRADIENT_BLUR` | 3 | 沉浸式渐变模糊（HdsIosTabs 等官方示例采用） |

## 事件

### onMenuClick

标题栏菜单按钮点击。`index` 为菜单项下标（与 menuIcons/menuLabels 对应）。

## 注意事项

- 组件**全屏挂载**（`absolutePosition` 四边全给），内容区由组件内部承载；
- 菜单图标走原生侧符号映射表（`sys.symbol` 名），表外名称回退默认图标；
- 需要完全自定义标题栏布局时改用[标题栏容器](immersive-light-sense-titlebar.md)（stackBuilder + Kuikly 子视图）。
