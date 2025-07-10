# NotifyModule

用于页面内或者页面之间发送通知

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
