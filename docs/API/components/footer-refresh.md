# FooterRefresh(列表尾部刷新)

用于在ListView 中实现尾部刷新的功能

[组件使用范例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ListViewDemoPage.kt)

## 属性

除了支持所有[基础属性](https://chat.openai.com/chat/basic-attr-event.md#基础属性)，还支持以下属性：

### preloadDistance

设置触发加载时，距离底部的距离

| 属性名          | 描述                           | 类型  |
| --------------- | ------------------------------ | ----- |
| distance | 设置触发加载时，距离底部的距离 | Float |

### minContentSize

设置最小的ContentSize

| 属性名          | 描述     | 类型  |
| --------------- |--------| ----- |
| minContentWidth | 最小内容宽度 | Float |
| minContentHeight | 最小内容高度 | Float |

## 事件

### refreshStateDidChange

监听刷新状态变化事件, 该方法接收类型为``(handler: (state: FooterRefreshState) -> Unit``的闭包, 闭包回调中带有``FooterRefreshState``

**FooterRefreshState**

| 枚举              | 描述     | 类型  |
|-----------------|--------| ----- |
| IDLE | 普通闲置状态 | FooterRefreshState |
| REFRESHING | 正在刷新中的状态 | FooterRefreshState |
| NONE_MORE_DATA | 无更多数据状态（后面不会再次触发刷新状态） | FooterRefreshState |
| FAILURE | 失败状态（一般展示点击重试UI） | FooterRefreshState |

## 方法

### beginRefresh

`()` 手动开始刷新

### endRefresh

结束刷新并设置结束状态

| 参数               | 描述     | 类型  |
|------------------|--------| ----- |
| endState  | 刷新状态   | FooterRefreshEndState |

### resetRefreshState

重置刷新状态

| 参数               | 描述     | 类型  |
|------------------|--------| ----- |
| state  | 重置刷新状态   | FooterRefreshEndState |

### refreshState

设置刷新状态

| 变量名   | 描述    | 类型  |
|-------|-------| ----- |
| state | 刷新状态值 | FooterRefreshEndState |
