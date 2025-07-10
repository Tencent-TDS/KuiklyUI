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


## Pager中监听页面事件

在``Pager``中，你可以通过重写``onReceivePagerEvent``方法来监听来自Native的事件

```kotlin
@Page("1")
internal class HelloWorldPage : Pager() {

    ...
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

   ...
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