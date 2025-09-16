# 组件自定义属性

如果想要扩展组件的属性，可以通过自定义通道传输至宿主侧，对属性进行相对应的处理，实现组件的自定义属性扩展。

## Kuikly
首先需要在 `Kuikly` 跨端侧
1. 声明自定义扩展属性
> 注：属性值只能使用基本数据类型
```kotlin
// 声明了 myProp 的属性
fun Attr.myProp(value: String) {
    "myProp" with value
}
```
2. 在组件中使用自定义属性

```kotlin
// 在需要使用自定义属性的组件中，调用定义的 myProp 方法
View {
    attr {
        ...
        myProp("value")
        ...            
    }
}
```

然后需要在各端实现自定义属性的处理

## 安卓
宿主端实现自定义属性Handler，可以处理自定义属性

IKuiklyRenderViewExport是kuikly ui组件向外暴露的接口
```kotlin
class ViewPropExternalHandler : IKuiklyRenderViewPropExternalHandler {
    override fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean {
        return when (propKey) {
            "myProp" -> {
                ...
                true
            }
            else -> false
        }
    }

    override fun resetViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String
    ): Boolean {
        return when (propKey) {
            "myProp" -> {
                ...
                true
            }
            else -> false
        }
    }

}

```

注册实现的自定义属性Handler
```kotlin
// KuiklyRenderActivity.kt
    override fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerViewExternalPropHandler(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            viewPropExternalHandlerExport(ViewPropExternalHandler())
        }
    }
```


## iOS
因为在Kuikly侧设置的属性名是myProp，所以到了宿主侧，kuikly会通过反射寻找css_myProp属性，调用setCss_myProp方法设置属性，因此通过声明一个UIView的分类来扩展属性，参考如下实现：

```object-c
// UIView+MyProp.h

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIView (MyProp)
@property (nonatomic, strong, nullable) NSString *css_myProp;
@end

NS_ASSUME_NONNULL_END
```

```object-c
// UIView+MyProp.m

#import "UIView+MyProp.h"
#import <objc/runtime.h>

@implementation UIView (MyProp)
- (NSString *)css_myProp {
    return objc_getAssociatedObject(self, @selector(css_myProp));
}

- (void)setCss_myProp:(NSString *)css_myProp {
    if (css_myProp) {
        ...
    } else {
        ...
    }
}
@end

```

## 鸿蒙
添加kuikly头文件至CMake
```CMAKE
include_directories(
        ...
        ${NATIVERENDER_ROOT_PATH}/../../../oh_modules/@kuikly-open/render/include
        ...
)
```

arkui_handle view对应的ohos capi的handle，类型为ArkUI_NodeHandle
```C++
#include <string>
#include "Kuikly/Kuikly.h"

bool ViewPropHandler(void* arkui_handle, const char* propKey, KRAnyData propValue) {
    if (strcmp(propKey, "myProp") == 0) {
        if (KRAnyDataIsString(propValue)) {
            // propValueStr为Kuikly传递传递过来的字符串，KRAnyData暂时支持String和Int
            std::string propValueStr(KRAnyDataGetString(propValue));
            ...
            return true;
        }
    }
    return true;
}

bool ViewResetPropHandler(void* arkui_handle, const char* propKey) {
    if (strcmp(propKey, "myProp") == 0) {
        ...
        return true;
    }
    return false;
}
```

宿主端注册实现的自定义属性Handler
```c++
static napi_value InitKuikly(napi_env env, napi_callback_info info) {
...

    KRRenderViewSetExternalPropHandler(*ViewPropHandler, *ViewResetPropHandler);

    // 位于api->kotlin.root.initKuikly()之前;
 
    ...
}
```
自定义View需要单独对Kuikly传来的参数进行设置，参考：[重写setprop方法](./expand-native-ui.md#重写setprop方法)

## H5
宿主端实现自定义属性Handler，可以处理自定义属性

IKuiklyRenderViewExport是kuikly ui组件向外暴露的接口
```kotlin
class ViewPropExternalHandler : IKuiklyRenderViewPropExternalHandler {
    override fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean {
        return when (propKey) {
            "myProp" -> {
                ...
                true
            }
            else -> false
        }
    }

    override fun resetViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String
    ): Boolean {
        return when (propKey) {
            "myProp" -> {
                ...
                true
            }
            else -> false
        }
    }

}

```

注册实现的自定义属性Handler
```kotlin
// KuiklyRenderActivity.kt
    override fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerViewExternalPropHandler(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            viewPropExternalHandlerExport(ViewPropExternalHandler())
        }
    }
```


## 微信小程序
宿主端实现自定义属性Handler，可以处理自定义属性

IKuiklyRenderViewExport是kuikly ui组件向外暴露的接口
```kotlin
class ViewPropExternalHandler : IKuiklyRenderViewPropExternalHandler {
    override fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean {
        return when (propKey) {
            "myProp" -> {
                ...
                true
            }
            else -> false
        }
    }

    override fun resetViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String
    ): Boolean {
        return when (propKey) {
            "myProp" -> {
                ...
                true
            }
            else -> false
        }
    }

}

```

注册实现的自定义属性Handler
```kotlin
// KuiklyRenderActivity.kt
    override fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerViewExternalPropHandler(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            viewPropExternalHandlerExport(ViewPropExternalHandler())
        }
    }
```

 website/docs/开发文档/notify.md +1-1
 
        })
```

##### web侧发送事件
##### H5侧发送事件

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
 website/docs/开发文档/miniapp-dev.md  0 → 100644 +56-0
 
# 微信小程序平台开发方式

``Kuikly``在微信小程序平台上，可以编译成**js**产物运行在微信上。

## 快速开始

```shell
# 运行 shared 项目 dev server 服务器，没有安装 npm 包则先 npm install 安装一下依赖
npm run serve
#  构建 shared 项目 Debug 版
./gradlew :shared:packLocalJsBundleDebug
```

然后构建 miniApp 项目
```shell
#  运行 miniApp 服务器 Debug 版
./gradlew :miniApp:jsMiniAppDevelopmentWebpack
```

构建 release 版本
```shell
# 首先构建业务 Bundle
./gradlew :demo:packLocalJSBundleRelease

# 然后构建 miniApp
./gradlew :miniApp:jsMiniAppProductionWebpack
```


使用微信小程序开发者工具打开miniApp下的dist目录，根据你的实际页面，修改app.json里面的pages数组和在pages里新建对应的页面
```javascript
// 例如demo里存在router的Page, 就需要在app.json的pages数组里添加 "pages/router/index", 同时在pages的目录里新建router目录补充和pages/index目录一样的内容

// pages/index/index.js内容
var render = require('../../lib/miniApp.js')
render.renderView({
    // 这里的pageName是最高优先级，如果没配置，会去拿微信小程序启动参数里的page_name，如果都没有会报错
    // 建议微信小程序的第一个页面必须配置pageName
    // pageName: "router",
    statusBarHeight: 0 // 如果要全屏，需要把状态栏高度设置为0
})
```

## 本地静态资源

demo里面的src/commonMain/assets下的文件，需要复制到dist/assets目录
```shell
// 复制业务的assets文件到微信小程序目录
./gradlew :miniApp:copyAssets
```

## 页面配置

微信小程序的壳工程中, 每个页面里都会调用render.renderView, 支持传递两个参数
1. pageName页面名称, 这里如果配置了会忽略微信小程序启动的时候传递的page_name参数
2. statusBarHeight状态栏高度, 默认会使用系统的状态了高度, 设置为0可以全屏   

在微信小程序开发者工具，可以配置启动参数，指定启动的页面和其他配置

例如配置 page_name=SafeAreaExamplePage&testParam=123

## 项目说明

项目入口在 Main.kt 的 main 方法中，其中 KuiklyRenderViewDelegator 用于注册外部自定义 View 和 Module 及 PropHandler， 
宿主侧可以在此实现自定义的View，Module并注册到KuiklyRenderViewDelegator中。
