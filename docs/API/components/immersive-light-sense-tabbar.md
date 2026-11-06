# ImmersiveLightSenseTabBarOhos(沉浸光感 TabBar)

:::tip 系统和版本要求

- **系统版本**：HarmonyOS 6.1.0+

- **Kuikly版本**：2.6.0+

:::

沉浸光感 TabBar 提供两种实现，按业务场景选用：

| | 标准版（HdsTabs） | 自绘版（SelfDrawn·实验） |
| -- | -- | -- |
| 组件 | `ImmersiveLightSenseTabBarOhos` | `ImmersiveLightSenseTabBarSelfDrawnOhos` |
| 光效材质 | **官方管线（最强）** | 普通节点材质（`ImmersiveMaterial`，含点光源/形变） |
| bar 外形 | **胶囊固定，圆角不可设置** | **圆角/高度/底色全可控**（0=直角平底） |
| item 定制 | `items` + `itemIcons`（符号映射表） | `items`（图标原生侧固定符号表） |
| 切换交互 | 官方（点击/滑动/系统切换动画） | 点击 + **bar 上 PanGesture 拖动切换** |
| 定位 | **推荐** | 实验路线（外形强诉求时用） |

> 两条路线的材质管线在系统层面不可兼得：官方材质层（胶囊外形）不参与普通节点裁剪，"官方光效 + 任意外形"无法同时成立。

[标准版使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ImmersiveLightSenseTabBarDemoPage.kt) / [自绘版使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ImmersiveLightSenseTabBarSelfDrawnDemoPage.kt)（两版可在 Demo 页 12017 实时切换对照）

## 标准版（ImmersiveLightSenseTabBarOhos）

官方 HdsTabs 封装——官方光效与材质管线，item 图标/文字/数量由业务下发，布局排版交给 HdsTabs 内置规范。

```kotlin
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseTabBarOhos

ImmersiveLightSenseTabBarOhos {
    attr {
        // 全屏挂载：item 内容视图全屏 + 页签栏悬浮底部
        absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)
        contentTransparent(true)       // 全屏挂载必须：内置页底色会盖住整页
        items(listOf("首页", "发现", "消息", "我的"))
        itemIcons(listOf("house", "star", "message", "person"))
        selectedIndex(0)
        barInsetX(16f)                 // 页签栏左右内缩
        barBottomOffset(48f)           // 页签栏抬离页底
        immersiveMaterial(style = ImmersiveStyle.REGULAR)
    }
    event {
        tabChanged { index -> /* 切页 */ }
    }
}
```

### 属性

| 属性 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| items | 页签文字列表（数量即页签数） | List\<String\> | 空 |
| itemIcons | 页签图标（sys.symbol 符号名，与 items 同下标） | List\<String\> | 空 |
| selectedIndex | 当前选中索引（受控） | Int | 0 |
| barInsetX | 页签栏左右内缩（vp） | Float | 16 |
| barBottomOffset | 页签栏底部抬离（vp） | Float | 16 |
| contentTransparent | 内置内容区底色透明开关（全屏挂载传 true） | Boolean | false |
| immersiveMaterial | 官方沉浸光感材质 | 见[总览](immersive-light-sense.md#immersivematerial) | 关 |

### 事件

**tabChanged**：页签切换（点击 / 页签栏上滑动 / 内容区滑动），参数为新页签索引。

### 注意事项

- **胶囊外形系统固定，圆角不可设置**——SDK 无对应字段（HDS 系统规格）。需要任意外形请用下方自绘版；
- 全屏挂载必须传 `contentTransparent(true)`，否则内置逐页换色底色会盖住整页（含 Kuikly 控制内容）。

## 自绘版（ImmersiveLightSenseTabBarSelfDrawnOhos·实验）

原生 Tabs 提供切换承载，bar 外形与 item 内容完全自绘；材质走原生 `Tabs.barFloatingStyle`（API 26）的 `uiMaterial.ImmersiveMaterial`（含点光源 `lightEffect` 与按压形变 `interactive`）。

```kotlin
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseTabBarSelfDrawnOhos

ImmersiveLightSenseTabBarSelfDrawnOhos {
    attr {
        absolutePosition(left = 0f, top = 0f, right = 0f, bottom = 0f)
        items(listOf("首页", "发现", "消息", "我的", "设置"))
        selectedIndex(0)
        pillRadius(0f)          // 0=直角平底 bar（对齐业务自绘 tabbar 外形）
        pillHeight(52f)
        barInsetX(16f)
        barBottomOffset(48f)
        immersiveMaterial(enable = true, style = ImmersiveStyle.THIN)
    }
    event {
        tabChanged { index -> }
    }
}
```

### 属性

| 属性 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| items | 页签文字列表 | List\<String\> | 空 |
| selectedIndex | 当前选中索引（受控） | Int | 0 |
| pillRadius | **整条 bar 的圆角**（vp，作用于底板层；0=直角平底） | Float | 0 |
| pillHeight | bar 高度（vp） | Float | 52 |
| pillBackgroundColor | bar 底板背景色（默认透明，让背景透进 bar 区域供材质折射） | Color | 透明 |
| barInsetX | bar 左右内缩（vp） | Float | 16 |
| barBottomOffset | bar 底部抬离（vp） | Float | 48 |
| immersiveMaterial | 节点材质 | 见[总览](immersive-light-sense.md#immersivematerial) | 关 |

### 事件

**tabChanged**：页签切换。切换可由**点击 item**或**bar 上横向拖动（PanGesture）**触发——拖动结束时按"位移 ÷ 槽宽"四舍五入决定切几页；tap 与 pan 由系统手势仲裁自动区分。内容区不响应滑动切换（手势归业务内容）。

### 注意事项

- 实验路线，不在生态主线内，与标准版并存按场景选用；
- 材质需要背后有内容（背景图），纯白/纯色背景上折射不可见；
- 选中反馈为图标/文字变色（无背景色块，对齐原生观感）；
- item 图标由原生侧固定符号表提供（不支持 itemIcons 定制，与标准版不同）。
