# ComposeView生命周期

在``Kuikly``中，组合组件**ComposeView**的生命周期如下:

1. **created**: ``ComposeView``已经创建, 此方法会在``body``方法前调用
2. **viewWillLoad**: ``ComposeView``的UI组件树即将创建, 此方法会在``body``方法前调用
3. **viewDidLoad**: ``ComposeView``的UI组件树已经创建好, 此方法会在``body``方法之后调用
4. **viewDidLayout**: ``ComposeView``的UI组件树已经测量完毕，可以在此方法执行一些依赖组件大小的操作，例如开始启动动画
5. **viewWillUnload**: ``ComposeView``即将被移除
6. **viewDidUnload**: ``ComposeView``已经被移除
7. **viewDestroyed**: ``ComposeView``已经被销毁

## 下一步

了解了组合组件``ComposeView``的生命周期后, 下一步, 我们来看在``Kuikly``中如何[执行延迟任务](set-timeout.md)