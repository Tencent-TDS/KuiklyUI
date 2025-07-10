# Scroller(滚动容器)

用于展示不确定高度的内容，可以将一系列不确定高度的子组件装到一个确定高度的容器中，使用者可通过上下或左右滚动操作查看组件宽高之外的内容。

[组件使用范例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/kit_demo/DeclarativeDemo/ScrollViewExamplePage.kt)

## 属性

除了支持所有[基础属性](basic-attr-event.md#基础属性)，还支持以下属性：

### scrollEnable

是否允许滑动内容

| 参数           | 描述                                                         | 类型          |
|--------------| ------------------------------------------------------------ | ------------- |
| scrollEnable | 是否允许滑动内容，默认`true`                                 | Boolean       |

### bouncesEnable

是否开启回弹效果

| 参数                | 描述                                                         | 类型          |
| --------------------- | ------------------------------------------------------------ | ------------- |
| bouncesEnable         | 是否开启回弹效果，默认 `true`                                | Boolean       |

### showScrollerIndicator

是否显示滚动条

| 参数                | 描述                                                         | 类型          |
| --------------------- | ------------------------------------------------------------ | ------------- |
| showScrollerIndicator | 是否显示滚动条，默认 `true`                                  | Boolean       |

### flexDirection

指定排版方向

| 参数                | 描述                                                         | 类型          |
| --------------------- | ------------------------------------------------------------ | ------------- |
| flexDirection         | 指定排版方向，`FlexDirection.COLUMN`为垂直方向，`FlexDirection.Row`为水平方向，默认为垂直方向 | FlexDirection |

### pagingEnable

是否开启分页

| 参数                | 描述 | 类型          |
| --------------------- |--| ------------- |
| flexDirection         | 是否开启分页 | Boolean |

## 事件

除了支持所有[基础事件](basic-attr-event.md#基础事件)，还支持以下事件：

### scroll

监听滚动事件，该方法接收一个闭包回调, 回调中的参数为 ``ScrollParams``

**ScrollParams**

| 参数     | 描述        | 类型      |
|--------|-----------|---------|
| offsetX | 列表当前横轴偏移量 | Float   |
| offsetY | 列表当前纵轴偏移量 | Float   |
| contentWidth | 列表当前内容总宽度 | Float   |
| contentHeight | 列表当前内容总高度 | Float   |
| viewWidth | 列表View宽度 | Float   |
| viewHeight | 列表View高度 | Float   |
| isDragging | 当前是否处于拖拽列表滚动中 | Boolean |

### scrollEnd

滚动结束时事件, 该方法接收一个闭包回调, 回调中的参数为 ``ScrollParams``

**ScrollParams**

| 参数     | 描述        | 类型      |
|--------|-----------|---------|
| offsetX | 列表当前横轴偏移量 | Float   |
| offsetY | 列表当前纵轴偏移量 | Float   |
| contentWidth | 列表当前内容总宽度 | Float   |
| contentHeight | 列表当前内容总高度 | Float   |
| viewWidth | 列表View宽度 | Float   |
| viewHeight | 列表View高度 | Float   |
| isDragging | 当前是否处于拖拽列表滚动中 | Boolean |

### dragBegin

用户开始拖拽事件, 该方法接收一个闭包回调, 回调中的参数为 ``ScrollParams``

**ScrollParams**

| 参数     | 描述        | 类型      |
|--------|-----------|---------|
| offsetX | 列表当前横轴偏移量 | Float   |
| offsetY | 列表当前纵轴偏移量 | Float   |
| contentWidth | 列表当前内容总宽度 | Float   |
| contentHeight | 列表当前内容总高度 | Float   |
| viewWidth | 列表View宽度 | Float   |
| viewHeight | 列表View高度 | Float   |
| isDragging | 当前是否处于拖拽列表滚动中 | Boolean |

### dragEnd

用户停止拖拽事件, 该方法接收一个闭包回调, 回调中的参数为 ``ScrollParams``

**ScrollParams**

| 参数     | 描述        | 类型      |
|--------|-----------|---------|
| offsetX | 列表当前横轴偏移量 | Float   |
| offsetY | 列表当前纵轴偏移量 | Float   |
| contentWidth | 列表当前内容总宽度 | Float   |
| contentHeight | 列表当前内容总高度 | Float   |
| viewWidth | 列表View宽度 | Float   |
| viewHeight | 列表View高度 | Float   |
| isDragging | 当前是否处于拖拽列表滚动中 | Boolean |

### contentSizeChanged

组件 Size 发生变化事件, 该方法接收一个闭包回调, 回调中的参数如下

| 参数     | 描述   | 类型      |
|--------|------|---------|
| width | 组件宽度 | Float   |
| height | 组件高度 | Float   |

## 方法

### setContentOffset

设置`Scroller`滚动到某个具体坐标偏移值(offset)的位置。

| 参数      | 描述                      | 类型   |
|---------|-------------------------| ---------- |
| offsetX | 滚动到x轴的偏移量               | Float|
| offsetY | 滚动到y轴的偏移量               | Float|
| animated | 滚动到y轴的偏移量, 默认为false     | Boolean|
| springAnimation | 是否使用spring动画滚动, 默认为null | SpringAnimation|

### setContentInset

设置`Scroller`内容边距

| 参数      | 描述                  | 类型      |
|---------|---------------------|---------|
| top | 上边距                 | Float   |
| left | 左边距                 | Float   |
| bottom | 下边距                 | Float   |
| right | 右边距                 | Float   |
| animated | 滑动过程是否使用动画, 默认false | Boolean |

### setContentInsetWhenEndDrag

设置 OverScroll 时停留的内容边距

| 参数      | 描述                  | 类型      |
|---------|---------------------|---------|
| top | 上边距                 | Float   |
| left | 左边距                 | Float   |
| bottom | 下边距                 | Float   |
| right | 右边距                 | Float   |
