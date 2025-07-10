# Tabs(标签栏)

选项卡切换组件（与[PageList分页列表组件](./page-list.md)配套使用）

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/TabsExamplePage.kt)

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

## 事件

支持所有[Scroller事件](./scroller.md#事件)