# Refresh（下拉刷新组件）

用于在ListView 中实现下拉刷新的功能

[组件使用范例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ListViewDemoPage.kt)

## 属性

除了支持所有[基础属性](https://chat.openai.com/chat/basic-attr-event.md#基础属性)，还支持以下属性：

### refreshEnable

是否启用下拉刷新，默认为 `false`

| 属性名           | 描述                          | 类型    |
|---------------| ----------------------------- | ------- |
| refreshEnable | 是否启用下拉刷新 | Boolean |

## 事件

### refreshStateDidChange

监听下拉刷新状态变化事件，回调参数为 `RefreshViewState` 类型

**RefreshViewState**

| 值名       | 描述                     |
| ---------- | ------------------------ |
| IDLE       | 空闲状态，即未下拉       |
| PULLING    | 松开就可以进行刷新的状态 |
| REFRESHING | 正在刷新中的状态         |

### pullingPercentageChanged

监听下拉百分比变化事件，该方法接收类型为``(handler: (percentage01: Float) -> Unit)``闭包，闭包回调参数为 `Float` 类型，范围在0~1之间

| 属性名           | 描述                      | 类型    |
|---------------| ------------------------- | ------- |
| percentage01 | 下拉百分比变化 | Float |

## 方法

### beginRefresh

手动开始下拉刷新, 默认为true

| 参数           | 描述                      | 类型 |
|--------------| ------------------------- |--|
| animated | 是否需要动画 | Boolean |

### endRefresh

结束下拉刷新

| 参数           | 描述                      | 类型 |
|--------------| ------------------------- |--|
| animated | 结束下拉刷新 | Boolean |

### contentInsetWhenEndDrag

当松开手指后，下拉刷新回到初始位置时，列表的内容边距

| 属性名           | 描述                      | 类型 |
|--------------| ------------------------- |--|
| contentInsetTopWhenEndDrag | 边距 | Float |

### refreshState

刷新状态, 接收``RefreshViewState``枚举

**RefreshViewState**

| 枚举值           | 描述                      | 类型 |
|--------------| ------------------------- |--|
| IDLE | 普通闲置状态 | RefreshViewState |
| PULLING | 松开就可以进行刷新的状态 | RefreshViewState |
| REFRESHING | 正在刷新中的状态 | RefreshViewState |