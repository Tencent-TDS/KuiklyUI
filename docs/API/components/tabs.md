# Tabs(标签栏)

选项卡切换组件（与[PageList分页列表组件](./page-list.md)配套使用）

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/TabsExamplePage.kt)

::::warning 使用注意
- Tabs **必须设置高度**（`attr { height(50f) }`），未设置时会在运行时抛错。
- Tabs 只支持横向排版。
- 小程序端指示条动画曲线与其余平台不同（对齐 scroll-view 约 300ms 的 ease-out 曲线），其余平台为线性曲线。
::::

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### scrollParams

更新scroller滚动信息使得tabs组件'指示条'同步滚动。该参数必须设置，才能让tabs组件正常使用，该参数来自PageList等Scroller容器组件中监听scroll事件的[ScrollParams参数](./scroller.md#scroll)

### defaultInitIndex

首次默认初始化的tabs组件对应index

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| index | 初始化index | Int |

### indicatorInTabItem

生成可滚动的指示条，配合scrollParams同步滚动

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| creator | 指示条View| ViewContainer<*, *>.() -> Unit |

### indicatorAlignCenter

指示条居中滚动

### indicatorAlignAspectRatio

指示条按比例滚动（默认行为）

> 当使用该模式时，如果关联列表同时设置了默认项（如`defaultPageIndex`）并且希望首屏首次渲染时Tabs也能正确联动到对应的default item，需要将关联List/PageList的`firstContentLoadMaxIndex`设置为列表总数量，避免首屏分批加载导致初始位置关联不准确。

## TabItem

`TabItem` 为 Tabs 下的子项容器，通过回调参数 `newState` 暴露选中态，业务可基于 `newState.selected` 响应选中变化。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| newState | TabItem 状态，含 `selected` 选中态 | TabItemView.ItemState |

**属性**：支持所有[基础属性](basic-attr-event.md#基础属性)与[布局属性](basic-attr-event.md#布局属性)，无自有属性。

**事件**：支持所有[基础事件](basic-attr-event.md#事件)，无自有事件。

:::: tip 说明
未添加任何 TabItem 时，Tabs 的指示条会自动隐藏。
::::

**示例**

```kotlin{2-12}
Tabs {
    attr {
        height(50f)
        scrollParams(scrollParams)
    }
    TabItem { newState ->
        attr {
            backgroundColor(if (newState.selected) Color.BLUE else Color.WHITE)
        }
        Text {
            attr {
                text(if (newState.selected) "已选中" else "未选中")
            }
        }
    }
}
```

## 事件

支持所有[Scroller事件](./scroller.md#事件)