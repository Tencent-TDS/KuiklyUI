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
