# BackPressModule

返回键模块，用于通知宿主侧是否消费返回键按下事件。

<Badge text="Android" type="warn"/> <Badge text="iOS" type="warn"/> <Badge text="鸿蒙" type="warn"/>

:::tip 与 BackPressHandler 的区别
页面级返回键拦截请使用 `BackPressHandler`（详见[接入系统返回键](../../DevGuide/back-press-handler.md)）。框架在收到返回键事件时，会按「是否注册了 `BackPressCallback`」自动调用本模块的 `backHandle` 通知宿主，业务一般无需手动调用。
:::

## backHandle方法

同步通知宿主是否消费本次返回键事件。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| isConsumed <Badge text="必需" type="warn"/> | 是否消费返回键事件，true 为已消费 | Boolean |

**示例**

```kotlin
// Pager 内注册返回键回调，框架会自动调用 backHandle(true) 通知宿主已消费
getBackPressHandler().addCallback(object : BackPressCallback() {
    override fun handleOnBackPressed() {
        // 页面自行处理返回逻辑
    }
})
```
