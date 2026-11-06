# NotifyModule

用于页面内或者页面之间发送通知

::: warning 重要：务必在页面销毁时解除监听
调用 `addNotify` 后，**必须**在 `pageWillDestroy` 中调用 `removeNotify` 解除监听，否则会出现以下问题：

- **所有平台**：回调闭包会持有 Pager 实例引用，页面无法被 GC，造成内存泄漏；
- **H5 / 小程序平台**（事件会在同一 JS 运行时的所有活跃 Kuikly 页面间共享，包括 SPA 中被缓存但未销毁的页面）：
    - 已销毁页面的回调仍会被其他页面的 `postNotify` 触发；
    - 回调中如果访问已销毁页面的视图/状态，可能引发异常或出现"幽灵页面"收到事件的现象。

即使页面销毁时框架会调用 `KRNotifyModule.onDestroy()` 兜底清理，仍强烈建议业务侧显式配对调用 `addNotify` / `removeNotify`，以保证时序确定、避免泄漏。
:::

## removeNotify方法

移除事件监听, 防止内存泄漏

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| eventName <Badge text="必需" type="warn"/> | 事件名字  | String |
| callbackRef <Badge text="必需" type="warn"/> | 事件监听回调引用  | CallbackRef |

## addNotify方法

添加事件监听方法, 该方法返回``CallbackRef``引用, 使用者需缓存这个引用，然后在不需要的时候解除事件监听

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| eventName <Badge text="必需" type="warn"/> | 事件名字  | String |
| crossProcess <Badge text="非必需" type="warn"/> <Badge text="仅 Android" type="warn"/> | 是否为跨进程通知，默认 false  | Boolean |
| cb <Badge text="必需" type="warn"/> | 事件回调闭包  | CallbackFn |

**示例**

```kotlin{4,8-10,18}
@Page("test")
class NotifyTestPage : Pager() {
    
    lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).addNotify("test") { data -> 
            // 事件回调闭包
        }
    }
    override fun body(): ViewBuilder {
        TODO("Not yet implemented")
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).removeNotify("test", eventCallbackRef)
    }

}
```

## postNotify方法

发送通知方法

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| eventName <Badge text="必需" type="warn"/> | 事件名字  | String |
| eventData <Badge text="必需" type="warn"/> | 事件参数  | JSONObject |
| crossProcess <Badge text="非必需" type="warn"/> <Badge text="仅 Android" type="warn"/> | 是否为跨进程通知，默认 false  | Boolean |
