# 发送事件通知

``Kuikly``提供了``NotifyModule``, 用于``Kuikly``页面之间的事件通知、``Kuikly``页面与Native页面的双向通知。下面我们来学习在``Kuikly``中如何监听和发送事件通知

## Kuikly页面事件通知

### 注册事件通知

我们首先在想要监听事件的``Pager``中监听事件, 然后在页面将要销毁时，解注册监听

```kotlin{4,8-11,16}
@Page("1")
internal class NotifyPage : Pager() {
    
    lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).addNotify("test") { data -> 
            // data参数为发送方传递过来的参数
            // 事件处理
        }
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).removeNotify("test", eventCallbackRef)
    }

    override fun body(): ViewBuilder {
        ...
    }

}
```

### 发送事件通知

在前面我们注册了事件名为**test**的通知, 接下来我们在其他地方发送**test**事件通知

```kotlin{7-9}
@Page("secondPage")
internal class SecondPage : Pager() {

    override fun created() {
        super.created()
        setTimeout(500) {
            acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).postNotify("test", JSONObject().apply { 
                put("test", 1)
            })
        }
    }

    override fun body(): ViewBuilder {
        ...
    }

}
```

## Kuikly页面与Native页面通信

``Kuikly``支持``Kuikly``页面与``Native``页面进行双向通信

### Kuikly页面接收Native事件

如果你的``Kuikly``页面想要接收**Native**发送的事件，那么你需要先在``Kuikly``页面先注册事件监听，然后Native发送事件

#### Kuikly页面监听事件

```kotlin{4,8-11,16}
@Page("1")
internal class NotifyPage : Pager() {
    
    lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).addNotify("test") { data -> 
            // data参数为发送方传递过来的参数
            // 事件处理
        }
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).removeNotify("test", eventCallbackRef)
    }

    override fun body(): ViewBuilder {
        ...
    }

}
```

#### Native发送事件

``Kuikly``侧注册事件以后，Native在合适的时机，发送事件通知给``Kuikly``侧

##### iOS侧发送事件

使用``NSNotificationCenter``发送事件

```kotlin
NSDictionary *data = @{ @"test": @1 };
[[NSNotificationCenter defaultCenter] postNotificationName:@"test" object:nil userInfo:data];
```

##### android侧发送事件

使用``Kuikly``提供的扩展方法发送事件

```kotlin
context.sendKuiklyEvent("test", JSONObject().apply{
    put("test", 1)
})
```
##### Ohos侧发送事件
使用鸿蒙``emitter``提供的方法发送事件
```ts
import emitter from '@ohos.events.emitter'

emitter.emit("test", {
          data: {
            'eventName': 'test',  // eventName和emitter.emit的eventId参数要一致
            'stringify': '{"key":"value"}'
          }
        })
```

##### web侧发送事件

使用``Kuikly``提供的扩展方法发送事件

```kotlin
KRNotifyModule.dispatchGlobalEvent("ss", JSONObject().apply{
    put("test", "Kuikly Web Render init")
})
```

### Native接收Kuikly事件

前面我们已经介绍了``Kuikly``如何接收``Native``发送的事件，下面我们来介绍``Native``如何接收``Kuikly``发送的事件

#### Native侧注册监听

##### iOS侧注册监听

使用``NSNotificationCenter``注册监听

```kotlin
[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onReceiveNotication:) name:@"test" object:nil];

- (void)onReceiveNotication:(NSNotification *)notification {
    NSString *eventName = notification.name; // 事件名字
    NSDictionary *userInfo = notification.userInfo; // Kuikly侧传递的参数
}

// 记得解注册
```

##### android侧注册监听

使用``BroadcastReceiver``来注册监听

```kotlin
val kuiklyReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventName = intent.getKuiklyEventName() // 接收到的事件名字
        val data = intent.getKuiklyEventParams() // kuikly侧传递的参数
    }
}

// 注册
context.registerKuiklyBroadcastReceiver(kuiklyReceiver)

// 解注册
context.unregisterKuiklyBroadcastReceiver(kuiklyReceiver)
```
##### Ohos侧注册监听
使用鸿蒙``emitter``提供的方法来注册监听
```ts
import emitter from '@ohos.events.emitter'

let callback = (event: emitter.EventData) => {
    const data = event.data as Record<string, string>;
    const eventName = data.eventName;
    const stringify = data.stringify ?? '{}';
    // The event subscribe was triggered
}
emitter.on("test", callback)
```

##### web侧注册监听
web上会在 NotifyModule构造时自动监听，destroy时解除监听，无需业务处理

#### Kuikly发送事件

``Native``侧监听好事件后, 在``Kuikly``侧, 在合适的时机通过``NotifyModule``发送事件

```kotlin{7-9}
@Page("secondPage")
internal class SecondPage : Pager() {

    override fun created() {
        super.created()
        setTimeout(500) {
            acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).postNotify("test", JSONObject().apply { 
                put("test", 1)
            })
        }
    }

    override fun body(): ViewBuilder {
        ...
    }

}
```

## 下一步

下一节, 我们来学习通过**NetworkModule**来[发送网络请求](network.md)
