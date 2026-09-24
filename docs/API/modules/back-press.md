# BackPressModule

返回键模块，用于通知宿主侧是否消费返回键按下事件。

<Badge text="Android" type="warn"/> <Badge text="鸿蒙" type="warn"/>

:::tip 与 BackPressHandler 的区别
页面级返回键拦截使用 `BackPressHandler`（详见「返回键处理」章节）；本模块用于在拦截回调中把「是否消费」的结果**同步**回传给宿主。
:::

## backHandle方法

同步通知宿主是否消费本次返回键事件。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| isConsumed <Badge text="必需" type="warn"/> | 是否消费返回键事件，true 为已消费 | Boolean |

**示例**

```kotlin
getPager().getBackPressHandler().addCallback {
    // 返回 true 表示页面自行处理返回逻辑，并同步通知宿主
    acquireModule<BackPressModule>(BackPressModule.MODULE_NAME).backHandle(true)
    true
}
```
