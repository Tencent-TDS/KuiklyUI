# 使用Module调用Native方法

在``Kuikly``开发中, 经常会有需要调用平台API的诉求, ``Kuikly``是一个跨端的UI框架，本身不具备平台相关的能力，但是``Kuikly``提供了``Module``机制，方便你调用平台的API。

## 什么是Module

``Module``可以理解为平台API的统一接口模块，里面包含着对**Native API的统一定义**，**具体实现由各个平台实现**。``Kuikly``内置了一些核心的``Module``:

1. **NotifyModule**: 通知模块, 可实现``Kuikly``页面与``Kuikly``页面，``Kuikly``页面与``Native``页面的双向通知
2. **MemoryCacheModule**: 内存缓存模块, 可实现内存级别的缓存
3. **SharedPreferencesModule**: 轻量级键值对磁盘缓存模块

## 如何获取Module实例

### 在Pager中获取Module

``Kuikly``的``Pager``在初始化的过程中会初始化``Pager``注册的``Module``，我们可以在``Pager``初始化完成以后, 获取``Module``，以下是获取``MemoryCacheModule``的示例

```kotlin{6,9}
internal class HelloWorldPage : Pager() {

    override fun created() {
        super.created()
        // 1. 通过acquireModule<T>(moduleName)获取Module, 如果找不到Module的话会抛异常
        val cacheModule = acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)
        
        // 2. getModule<T>(moduleName)获取Module, 如果找不到Module的话返回null
        val cacheModule1 = getModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)
    }

}
```

### 在组合组件中获取Module

如果你想在组合组件中获取``Module``, 你可以这样获取:

```kotlin
class TestComposeView : ComposeView<ComposeAttr, ComposeEvent>() {

    override fun created() {
        super.created()
        // 1. 通过acquireModule<T>(moduleName)获取Module, 如果找不到Module的话会抛异常
        val cacheModule = acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)

        // 2. getModule<T>(moduleName)获取Module, 如果找不到Module的话返回null
        val cacheModule1 = getModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)
    }
}
```

### 其他地方获取Module

如果你可能会想在非``Pager``类和非组合组件中获取``Module``, 你获取当前的``Pager``示例，通过``Pager``来获取``Module``

```kotlin{4-6}
class Test {

    fun setValue(v: Int) {
        val cacheModule = PagerManager.getCurrentPager()
            .acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)
        cacheModule.setObject("test", v)
    }
}
```

## 下一步

在了解完如何获取``Module``示例后, 下一步，我们来看如何使用``RouterModule``来[打开和关闭Kuikly页面](open-and-close-page.md)