# 页面事件

在``Kuikly``中, ``Pager``除了作为容器承载者, 还负责接收并分发**页面事件**, 这些事件通常是**Native**发送过来的事件。

## Native发送事件

Native侧可以通过Kuikly提供的API来给Kuikly页面发送页面事件

### android侧发送事件

在Kuikly容器中, 调用``KuiklyRenderViewDelegator.sendEvet``发送页面事件给Kuikly

```kotlin
kuiklyRenderViewDelegator.sendEvent("test", mapOf(
    "test" to 1
))
```

### iOS侧发送事件

在Kuikly容器中，调用``KuiklyRenderViewControllerDelegator.sendWithEvent``发送页面事件给Kuikly

```objc
@property (nonatomic, strong) KuiklyRenderViewControllerDelegator *delegator;

...
// 在合适的时机, 调用
[self.delegator sendWithEvent:"test" data:@{}]
```

### 鸿蒙侧发送事件

在鸿蒙侧中，可以在`KTNative`入口类中传入`onControllerReadyCallback`回调获得`KTNativeRenderController`，然后就可以调用`controller.sendEvent`方法发送页面事件给Kuikly

```ts
private controller: KTNativeRenderController | null = null

...
// 在合适的时机, 调用
controller.sendEvent("test", {"key": "value"})
```


**同步发送事件的使用场景**

默认情况下，Native 发送给 `Pager` 的事件通常是异步到达的。

如果某些事件需要与当前原生布局或上屏时机保持一致，可以在 Native 容器中实现 `syncSendEvent` 接口，指定事件为同步发送，避免事件晚一帧生效。

典型场景：页面旋转、分屏或宿主容器尺寸变化时，`rootViewSizeDidChanged` 如果异步发送，可能导致页面内容更新晚一帧，出现短暂白屏。

在鸿蒙侧，`rootViewSizeDidChanged` 会优先尝试在 `onSizeChange` 中走提前同步处理，`onAreaChange` 继续保留为兜底路径；两者会复用相同的尺寸变化判断和 `viewRect` 更新逻辑，避免重复处理分叉。

### 示例代码

以下示例表示：当 Native 发送 `rootViewSizeDidChanged` 时，改为同步发送给 Kuikly。

**Android**

```kotlin
// ContextCodeHandler.kt
val delegate = object : KuiklyRenderViewBaseDelegatorDelegate {
    override fun syncSendEvent(event: String): Boolean {
        return event == "rootViewSizeDidChanged"
    }
}
```

**iOS**

```objective-c
// KuiklyRenderViewController.m
- (BOOL)syncSendEvent:(NSString *)event {
    return [event isEqualToString:@"rootViewSizeDidChanged"];
}
```

**鸿蒙**

```ts
// KuiklyViewDelegate.ets
syncSendEvent(event: string): boolean {
  return event === 'rootViewSizeDidChanged'
}
```

## Pager中监听页面事件

在``Pager``中，你可以通过重写``onReceivePagerEvent``方法来监听来自Native的事件

```kotlin
@Page("1")
internal class HelloWorldPage : Pager() {

    // ...
    override fun onReceivePagerEvent(pagerEvent: String, eventData: JSONObject) {
        super.onReceivePagerEvent(pagerEvent, eventData)
        // pagerEvent: 事件名字
        // eventData: 事件数据
    }
}
```

## ComposeView中监听页面事件

除了在``Pager``中你可以监听到页面事件外，你还可以在[组合组件ComposeView](compose-view.md)中监听页面事件，具体监听代码如下

```kotlin
// 1. 实现IPagerEventObserver接口
class TestComposeView : ComposeView<ComposeAttr, ComposeEvent>(), IPagerEventObserver {

    override fun viewDidLoad() {
        super.viewDidLoad()
        getPager().addPagerEventObserver(this) // 2. 监听页面事件
    }

    override fun onPagerEvent(pagerEvent: String, eventData: JSONObject) {
        // 3. 处理页面事件
    }

    override fun viewDidUnload() {
        super.viewDidUnload()
        getPager().removePagerEventObserver(this) // 4. 取消页面事件监听, 防止内存泄漏
    }
}
```

## 下一步

下一步我们接着学习``Kuikly``中[组件属性attr](attr.md)