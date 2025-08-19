# Kuikly Web 渲染器模块

本项目为 Kuikly Web 渲染器模块，项目采用 kotlin 实现，引用了kotlin 提供的 stdlib-js 标准库，可以使用浏览器和Javascript相关API，还引用了 kuiklyCore 的相关代码。
关于kotlin/js，可以参考官方文档 https://kotlinlang.org/docs/js-project-setup.html

## 快速上手

项目的目录结构与 core-render-android 类似。不过因为是 Web 平台，所以源代码都存放在 jsMain/com/tencent/kuikly/core/web/render 下。项目入口为 KuiklyRenderView，通过 KuiklyRenderView.init
方法来启动 web-render

这里对常见的一些操作做一下说明：

- 新增 View

所有内置 View 相关逻辑放置在 expand/components 下，View 名称及文件名需要符合规范，以新增 Image 图片处理 View 为例来做说明，

首先我们要实现相应的 View
```kotlin
// 新增 KRImageView Class，因为 Image 需要读取内存缓存模块的数据，因此传入rootView供其调用模块接口，如果不需要此类操作的可以不需要参数
class KRImageView(view: KuiklyRenderView)
// 所有 View 都需要实现 IKuiklyRenderViewExport 接口，注意这里 IKuiklyRenderViewExport 需要传入我们实际渲染的 HTML 元素的类型  
class KRImageView(view: KuiklyRenderView) : IKuiklyRenderViewExport<HTMLImageElement> {}
// 创建真实的 Web 渲染 Image 所使用的 Element
private val image = document.createElement("img")
// 重写接口统一的 View 宿主元素，获取真实的 Web Element
override val ele: HTMLImageElement
    get() = image as HTMLImageElement
// 这里是对 HTML 元素特有的属性或事件进行设置，通用的样式设置在 ktx/KuiklyRenderCSSKtx.kt 中的 Element.setCommonProp 处理，如果
// 新增的 HTML 元素具有自己特殊的属性，以及需要处理事件，则需要在这里进行处理，比如图片需要处理 src 属性，以及 加载成功，获取图片尺寸事件等
override fun setProp(propKey: String, propValue: Any): Boolean {
    SRC -> {
        // 设置图片源
        setSrc(propValue as String)
        true
    }
    LOAD_SUCCESS -> {
        // 保存加载成功回调
        loadSuccessCallback = propValue as KuiklyRenderCallback
        true
    }
    LOAD_RESOLUTION -> {
        // 保存加载分辨率回调
        loadResolutionCallback = propValue as KuiklyRenderCallback
        true
    }
    else -> {
        // 其他统一处理
        super.setProp(propKey, propValue)
    }
}
// 注意这里 kuikly core 中定义的 ImageView 的 loadResolution 事件，在 Web 中并没有，因此我们需要自行封装处理，我们先保存 loadResolution 的回调
// 然后在加载成功的方法里返回对应的数据
imageElement.onload = {
    // 加载成功时，回调实际的图片源内容
    loadSuccessCallback?.invoke(mapOf(
        SRC to realSrc
    ))
    // 加载成功时，回调图片的尺寸数据
    loadResolutionCallback?.invoke(mapOf(
        IMAGE_WIDTH to imageElement.naturalWidth,
        IMAGE_HEIGHT to imageElement.naturalHeight
    ))
}


```
...到这里 ImageView 的能力就基本实现了，其他 View 的实现也可以以此作为参考

另外还有些 View 会提供 API 供 kuikly 侧调用，如果需要提供 API 的，则需要重写 call 方法以兼容 Kuikly 的 API 名称和参数等，我们以 KRListView 为例说明：
```kotlin
// 重写 KRListView 的 call 方法，如果是 HTML 元素没有的 API 名称，则还需要进行封装处理以符合 Kuikly 的 API 格式
override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
    return when(method) {
        METHOD_CONTENT_OFFSET -> setContentOffset(params)
        else -> super.call(method, params, callback)
    }
}
companion object {
    // 设置内容的偏移量，会把List滚到对应的位置
    private const val METHOD_CONTENT_OFFSET = "contentOffset"
}
// 因为 DIV 并没有 contentOffset 方法，因此需要通过 scrollBy 方法来进行模拟

```

View 实现之后，还需要将 View 注册到 rootView 中，在 web-render expand 目录下的 KuiklyRenderViewDelegator 中完成新 View 的注册
```kotlin
// KuiklyRenderViewDelegator.kt 的 registerRenderView 方法，将 KRImageView 注册
// 这里因为 ImageView 需要调用模块，因此将 rootView 作为参数传入，不需要的 View 可以不传入此参数
renderView?.let {
    renderViewExport(KRImageView.VIEW_NAME, {
        KRImageView(it)
    })
}
```
至此新增一个内置 View 就已经完成了


- 新增 Module

新增内置 Module 与新增 View 有些类似。所有 Module 相关逻辑放置在 expand/module 下，Module 名称及文件名需要符合规范，以 KRLogModule 为例说明：

首先实现相应 Module

```kotlin
// 新增 KRLogModule Class，所有 Module 都需要继承 KuiklyRenderBaseModule 类
class KRLogModule : KuiklyRenderBaseModule() {}
// 然后重写 call 方法，需要实现的接口都在这里实现即可
override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
    when(method) {
        METHOD_LOG_INFO -> logInfo(params)
        METHOD_LOG_DEBUG -> logDebug(params)
        METHOD_LOG_ERROR -> logError(params)
        else -> super.call(method, params, callback)
    }
    return null
}

companion object {
    const val MODULE_NAME = "KRLogModule"
    private const val METHOD_LOG_INFO = "logInfo"
    private const val METHOD_LOG_DEBUG = "logDebug"
    private const val METHOD_LOG_ERROR = "logError"
}
```

然后再将 Module 注册到 rootView 中，在 web-render expand 目录下的 KuiklyRenderViewDelegator 中完成新 Module 的注册

```kotlin
// KuiklyRenderViewDelegator.kt 的 registerModule 方法，将 KRLogModule 注册
moduleExport(KRLogModule.MODULE_NAME) {
    KRLogModule()
}
```

至此新增一个内置 Module 就已经完成了

- 外部 View 和 Module

外部 View 和 Module 的实现是在宿主 App 中进行的，通过 delegate 完成注册，注册时机是在 KuiklyRenderViewDelegator 的 registerExternalXXX 方法中，在内置 View 和 Module 注册之后
```kotlin
// KuiklyRenderViewDelegator.kt 的 registerExternalModule 方法，注册代理传入的外部 Module
// 代理给外部，让宿主工程可以暴露自己的 module
delegate.registerExternalModule(this)
// KuiklyRenderViewDelegator.kt 的 registerExternalRenderView 方法，注册代理传入的外部 View
// 代理给外部，让宿主工程可以暴露自己的 view
delegate.registerExternalRenderView(this)

// KuiklyRenderViewDelegator.kt 的 registerViewExternalPropHandler 方法，注册代理传入的外部 propHandler
// 代理给外部，让宿主工程可以暴露自己的自定义属性处理器
delegate.registerViewExternalPropHandler(kuiklyRenderExport)
```

## 构建配置

本项目目前仅配置了浏览器的构建产物，相关配置在build.gradle.kts中。具体配置说明配置文件有注释说明。如果需要使用新的依赖，可以修改配置文件进行引入。
本项目通常不需要单独构建，而是在宿主App项目中构建查看效果。具体可以参考 webApp 的说明文档。

如果需要查看项目效果，可以执行以下脚本
- Dev版
```shell
./gradlew :core-render-web:jsBrowserDevelopmentRun
## 简写
./gradlew :core-render-web:jsRun
```
- Release版
```shell
./gradlew :core-render-web:jsBrowserProductionRun
## 简写
./gradlew :core-render-web:jsRunProduction

```

构建结果在对应 render 目录下的 build 目录中，Dev 版本在 developmentExecutable 目录，Release 版在 distributions 目录