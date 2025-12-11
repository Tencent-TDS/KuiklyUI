# 扩展原生API

**Kuikly**允许开发者通过**Module**机制来访问平台的API，以达到复用平台生态能力的目的。下面我们以**打印日志**作为例子，来看**Kuikly**
如何通过**Module**机制来访问平台的API。 将**Native的API**暴露给**Kuikly**使用，需要完成以下工作

1. **Kuikly**侧:
   1. 新建**XXXModule**类并继承**Module**，编写API暴露给业务方使用
   2. 在**Pager**的子类中，注册新创建的**Module**
2. **android**侧:
   1. 新建**XXXModule**类并继承**KuiklyRenderBaseModule**, 编写API的具体实现代码
   2. 将**XXXModule**注册暴露给**Kuikly**侧
3. **iOS**侧:
   <br>
   新建**XXXModule**(类名必须与kuikly侧注册的module名字一致)并继承**KRBaseModule**, 编写API的具体实现代码
4. 鸿蒙侧:
   1. 新建**XXXModule**类，继承**KuiklyRenderBaseModule**，编写API的具体实现代码
   2. 将**XXXModule**注册暴露给**Kuikly**侧


## kuikly侧

1. 我们首先新建``MyLogModule``类，并继承``Module``类, 然后实现``moduleName``方法，返回``MyLogModule``对应的标识名字，用于与Native的Module对应

```kotlin
class MyLogModule : Module() {

    override fun moduleName(): String = "KRMyLogModule"
    
}
```

2. 在实现``MyLogModule``中的方法前，我们先来看其父类``Module``中的``toNative``方法。``toNative``方法用于``Kuikly``侧的``Module``触发``Native``侧对应``Module``的方法调用, 它含有下面5个参数
   1. ``keepCallbackAlive``: ``callback``回调是否常驻，如果为false的话，``callback``被回调一次后，会被销毁掉；如果为true的话，``callback``会一直存在内存中，直到页面销毁
   2. ``methodName``: 调用``Native Module``对应的方法名字
   3. ``param``: 传递给``Native Module``方法的参数，支持基本类型、数组、字符串（特别指出，Json不属于基本类型，需要先序列化为Json字符串）
   4. ``callback``: 用于给``Native Module``将处理结果回调给``Kuikly Module``侧的callback
   5. ``syncCall``: 是否为同步调用。``Kuikly``的代码是运行在一条单独的线程，默认与Native Module是一个异步的通信。如果syncCall指定为true时，可强制``kuikly Module``与``Native Module``同步通信

> 对于``callback``只回调一次的用法，框架提供了4个辅助方法：
> - syncToNativeMethod(methodName, params, null): String // 同步调用Native方法（native侧在子线程执行），传输JSONObject类型参数
> - syncToNativeMethod(methodName, arrayOf(content), null): Any? // 同步调用Native方法（native侧在子线程执行），传输基本类型数组（仅支持String、Int、Float、ByteArray）
> - asyncToNativeMethod(methodName, params, callback) // 异步调用Native方法（native侧在主线程执行），传输JSONObject类型参数，回调JSON字符串
> - asyncToNativeMethod(methodName, arrayOf(content), callback) // 异步调用Native方法（native侧在主线程执行），传输基本类型数组，回调基本类型

#### Native侧支持的数据类型

Module 返回值和 callback 参数支持的类型：
| 平台          | 支持的数据类型                                                                                        |
|:------------|:-----------------------------------------------------------------------------------------------|
| **Android** | `String` `Int` `Long` `Float` `Double` `Boolean` `ByteArray` `Map` `List` `JSONObject`         |
| **iOS**     | `NSString` `NSNumber` `BOOL` `NSData` `NSDictionary` `NSArray`                                 |
| **鸿蒙**      | `String` `Int` `Long` `Float` `Double` `Bool` `ByteArray` `Array` `Map`                        |
| **H5**    | `String` `Int` `Long` `Float` `Double` `Boolean` `Array` `Map` `List` `JSONObject` `JSONArray` |

---

#### Native侧序列化规则

数据从 Native 传递到 Kotlin 时的处理方式：

| 类目         | 序列化方式 | 涉及类型                                                 |
|:-----------|:---:|:-----------------------------------------------------|
| **基础类型**   | ✅ 直接透传 | `String` `Int` `Float` `Double` `Boolean` `NSNumber` |
| **二进制数据**  | ✅ 直接透传 | `ByteArray` `NSData`                                 |
| **JSON数据** | 📦 JSON字符串 | `JSONObject` `JSONArray`                             |
| **集合类型**   | 📦 JSON字符串 | `Map` `List` `NSDictionary` `NSArray` `Array`        |
| **特殊规则**   | ✅ 直接透传 | Array 中包含二进制元素（`ByteArray`/`NSData`）时                |

:::tip 注意
- syncToNativeMethod和asyncToNativeMethod，传入参数params是JSONObject且序列化为jSON字符串传至Native侧，
序列化过程不支持对ByteArray二进制数据进行处理。因此请选择params为Any的syncToNativeMethod/asyncToNativeMethod方法传输二进制参数
- callback中解析回传至Koltin侧二进制数据，可参考示例：
```koltin
if (data is ByteArray) {
   val byteData = data.decodeToString()
   // ...
}
```
 :::

3. 接着我们新增``log``方法，让业务方能够打印日志。
```kotlin
class MyLogModule : Module() {

    /**
     * 打印日志
     * @param content 日志内容
     */
    fun log(content: String) {
        toNative(
            false,
            "log",
            content,
            null,
            false
        )
    }

    override fun moduleName(): String = "KRMyLogModule"

}
```
在``log``方法中，我们调用了``toNative``方法来完成对**Native Module**的调用。这个``log``方法是没有返回值的。但是实际业务场景中，往往是有需要返回值的需求，那
``module``中的api如何获取原生侧的返回值呢？

### 获取返回值

``Kuikly``调用原生API时，可以有两种方式获取原生侧的返回值
1. 异步获取返回值: 这种方式是在调用``toNative``方法时，传递``CallbackFn``参数，让原生侧将结果已json字符串的形式传递给``CallbackFn``
2. 同步获取: 这种方式是在``Kuikly``当前线程(非UI线程)中调用原生侧的API方法，原生侧的API方法将结果以String的格式返回

```kotlin
class MyLogModule : Module() {

    /**
     * 打印日志
     * @param content 内容
     * @param callbackFn 结果回调
     */
    fun logWithCallback(content: String, callbackFn: CallbackFn) {
        toNative(
            false,
            "logWithCallback",
            content,
            callbackFn,
            false
        )
    }

    /**
     * 同步调用打印日志
     * @param content
     */
    fun syncLog(content: String): String {
        return toNative(
            false,
            "syncLog",
            content,
            null,
            true
        ).toString()
    }

    override fun moduleName(): String = "KRMyLogModule"

}
```

4. 实现完``Kuikly``侧的module后，下面我们在``Pager``的子类中重写``createExternalModules``注册``MyLogModule``

```kotlin
internal class TestPage : Pager() {
    override fun body(): ViewBuilder {
    }

    override fun createExternalModules(): Map<String, Module>? {
        return mapOf(
            "KRMyLogModule" to MyLogModule()
        )
    }
}
```

5. 最后我们来看业务是如何使用``MyLogModule``

```kotlin
internal class TestPage : Pager() {

    override fun created() {
        super.created()
        
        val myLogModule = acquireModule<MyLogModule>("KRMyLogModule") // 调用acquireModule并传入module名字获取module
        myLogModule.log("test log") // 调用log打印日志
        myLogModule.logWithCallback("log with callback") { // 异步调用含有返回值的log方法
            val reslt = it // 原生侧返回的JSONObject对象
        }
        val result = myLogModule.syncLog("sync log") // 同步调用含有返回值的log方法
    }
}
```

6. 以上就是``Kuikly``侧的需要完成的工作，剩下的就是原生侧实现``Kuikly``侧定义的方法

## android侧

1. 在接入``Kuikly``的android宿主工程中新建``KRMyLogModule``类，然后继承``KuiklyRenderBaseModule``，并重写其``call``方法（``call``方法有两个实现，**根据Module传输的数据类型，选择重写其中之一**）

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {
    // 传输基本类型、数组、字符串
    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {

    }

    // 传输Json（会被序列化为Json字符串）
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
       
    }
}
```

``Kuikly``的``MyLogModule``的``toNative``方法最终会调用原生对应的``Module``的``call``方法，也就是``KRMyLogModule``中的``call``方法。

在``Kuikly``的``MyLogModule``中定义了三个方法，下面我们来看在android侧如何实现这三个方法

### log方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            else -> super.call(method, params, callback)
        }
    }

    private fun log(content: String) {
        Log.d("test", content)
    }

}
```

在``call``方法中，我们通过``method``参数来识别``log``方法，然后调用我们定义的私有方法``log``，并将``Kuikly``侧传递过来的``content``参数传递给``log``方法

### logWithCallback方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            "logWithCallback" -> logWithCallback(params ?: "", callback)
            else -> super.call(method, params, callback)
        }
    }

    private fun logWithCallback(content: String, callback: KuiklyRenderCallback?) {
        Log.d("test", content) // 1. 打印日志
        callback?.invoke(mapOf(
            "result" to 1
        )) // 2. callback对应kuikly module侧方法的callbackFn, 此处将数据存放到map中并传递给kuikly侧
    }
    ...
}
```

### syncLog方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            "logWithCallback" -> logWithCallback(params ?: "", callback)
            "syncLog" -> syncLog(params ?: "")
            else -> super.call(method, params, callback)
        }
    }
    
    private fun syncLog(content: String): String {
        Log.d("test", content) // 1. 打印日志
        return "success" // 2. 将字符串同步返回给kuikly侧
    }
    ...
}
```

2. 原生侧实现完API后，我们将``KRMyLogModule``注册暴露到``Kuikly``中，与``Kuikly``侧的``MyLogModule``对应起来。在实现了``KuiklyRenderViewDelegatorDelegate``接口
类中重写``registerExternalModule``方法，注册``KRMyLogModule``

:::tip 注意
注册的名字必须与``Kuikly moudle``侧注册的名字一样
::::

```kotlin
    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            ...
            moduleExport("KRMyLogModule") {
                KRMyLogModule()
            }
        }
    }
```

## iOS侧

1. 在接入``Kuikly``的iOS宿主工程中新建``KRMyLogModule``类，然后继承``KRBaseModule``

:::tip 注意
iOS原生侧的Module创建是在运行时根据``Kuikly``注册``module``的名字来动态创建的，因此类名必须与``Kuikly``侧注册``module``的名字保持一致
:::

```objc
// .h
#import <Foundation/Foundation.h>
#import "KRBaseModule.h"

NS_ASSUME_NONNULL_BEGIN

@interface KRMyLogModule : KRBaseModule

@end

NS_ASSUME_NONNULL_END

// .m
#import "KRMyLogModule.h"

@implementation KRMyLogModule

@end
```

下面我们看如何实现``Kuikly``侧定义的API

### log方法

```objc
#import "KRMyLogModule.h"

@implementation KRMyLogModule

-(void)log:(NSDictionary *)args {
    NSString *content = args[HR_PARAM_KEY]; // 获取log内容
    NSLog(@"log:%@", content);
}

@end
```

方法名字保持与``Kuikly``侧的log方法名字一致，并且参数固定为``NSDictionary``类型。

``Kuikly``侧传递过来的参数从``args``字典中提取, 例如

```objc
NSString *content = args[HR_PARAM_KEY]; // 获取log内容
```

:::tip 注意
iOS侧的Module中的方法名字必须与kuikly侧toNative方法传递的方法名字一致，这样才能在运行时找到并调用方法
:::

### logWithCallback方法

```kotlin
- (void)logWithCallback:(NSDictionary *)args {
    NSString *content = args[HR_PARAM_KEY]; // 1.获取log内容
    NSLog(@"log:%@", content); // 2.打印日志
    
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY]; // 3.获取kuikly侧传递的callbackFn
    callback(@{
        @"result": @1
    }); // 4.回调给kuikly侧
    
}
```

``Kuikly``侧的``CallbackFn``我们可以从args字典中拿到

```kotlin
KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
```

### syncLog方法

```kotlin
- (id)syncLog:(NSDictionary *)args {
    NSString *content = args[HR_PARAM_KEY]; // 1.获取log内容
    NSLog(@"log:%@", content); // 2.打印日志
    
    return @"success"; // 3.同步返回给kuikly侧
}
```
## 鸿蒙侧

1. 在接入``Kuikly``的鸿蒙宿主工程（ArkTS）中新建``KRMyLogModule``类，继承``KuiklyRenderBaseModule``，并重写其``call``方法

```ts
export class KRMyLogModule extends KuiklyRenderBaseModule {
    // 定义模块名（注册时用到，全局唯一）
    static readonly MODULE_NAME = "KRMyLogModule";

    // 是否同步模式（同步模式的module运行在kuikly线程，支持同步调用和异步调用； 异步模式的module运行在ui线程，只支持异步调用）
    syncMode(): boolean {
        return true;
    }

    call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {

    }
    ...
}
```

``Kuikly``的``MyLogModule``的``toNative``方法最终会调用原生对应的``Module``的``call``方法，也就是``KRMyLogModule``中的``call``方法。

在``Kuikly``的``MyLogModule``中定义了三个方法，下面我们来看在鸿蒙侧如何实现这三个方法

### log方法

```ts
export class KRMyLogModule extends KuiklyRenderBaseModule {
    static readonly MODULE_NAME = "KRMyLogModule";
  
    syncMode(): boolean {
        return true;
    }

    call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
        // 分发响应
        switch (method) {
            case 'log':
                this.log(params as string);
                return null;
        }
        return null
    }

    private log(content: string) {
        console.log(`log: ${content}`);
    }
    ...
}
```

在``call``方法中，我们通过``method``参数来识别``log``方法，然后调用我们定义的私有方法``log``，并将``Kuikly``侧传递过来的``content``参数传递给``log``方法

### logWithCallback方法

```ts
export class KRMyLogModule extends KuiklyRenderBaseModule {
    static readonly MODULE_NAME = "KRMyLogModule";
  
    syncMode(): boolean {
        return true;
    }

    call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
        // 分发响应
        switch (method) {
            case 'log':
                this.log(params as string);
                return null;
            case 'logWithCallback':
                this.logWithCallback(params as string, callback);
                return null;
        }
        return null
    }

    private logWithCallback(content: string, callback: KuiklyRenderCallback | null) {
        console.log("log:" + content);
        // 异步返回结果
        callback?.({
            "result": 1
        });
    }
    ...
}
```

调用`KRMyLogModule`的`logWithCallback`方法时，我们传入`KuiklyRenderCallback`，在执行完相关操作后调用该`callback`异步返回结果。

### syncLog方法

```ts
export class KRMyLogModule extends KuiklyRenderBaseModule {
    static readonly MODULE_NAME = "KRMyLogModule";
  
    syncMode(): boolean {
        return true;
    }

    call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
        // 分发响应
        switch (method) {
            case 'log':
                this.log(params as string);
                return null;
            case 'logWithCallback':
                this.logWithCallback(params as string, callback);
                return null;
            case 'syncLog':
                // 同步返回结果
                return this.syncLog(params as string);
        }
        return null
    }

    private syncLog(content: string) {
        console.log("log:" + content);
        return "success";
    }
    ...
}
```
同步调用`KRMyLogModule`的方法时，直接在`call`函数中返回函数结果。

2. 原生侧实现完API后，我们将``KRMyLogModule``注册暴露到``Kuikly``中，与``Kuikly``侧的``MyLogModule``对应起来。在实现了``IKuiklyViewDelegate``接口
类中重写``getCustomRenderModuleCreatorRegisterMap``方法，注册``KRMyLogModule``

:::tip 注意
注册的名字必须与``Kuikly moudle``侧注册的名字一样
::::

```ts
export class KuiklyViewDelegate extends IKuiklyViewDelegate {
    ...
    getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
        const map: Map<string, KRRenderModuleExportCreator> = new Map();
        // 注册自定义module
        map.set(KRMyLogModule.MODULE_NAME, () => new KRMyLogModule())
        return map;
    }
}
```

### 鸿蒙CModule
如果业务希望在鸿蒙C侧实现扩展原生API，可以通过以下方式进行扩展
1. 接口定义

- kuikly.h: module的构造和注册API
- KRAnyData.h:  KRAnyData设置和读取API


```c
/**
 * 注册自定义模块(V2)
 * @param moduleName 模块名称
 * @param onConstruct Module构造时调用的方法
 * @param onDestruct Module析构时调用的方法
 * @param onCallMethod 模块的call method实现
 * @param reserved 保留字段
 */
void KRRenderModuleRegisterV2(const char *moduleName,
                            KRRenderModuleOnConstruct onConstruct,
                            KRRenderModuleOnDestruct  onDestruct,
                            KRRenderModuleCallMethodV2  onCallMethod,
                            void *reserved);

/**
 * Module的CallMethod调用(新)
 * @param moduleInstance 模块实例，这是KRRenderModuleOnConstruct的返回值
 * @param moduleName 模块实例，这是KRRenderModuleOnConstruct的返回值
 * @param sync bool 是否同步
 * @param method 调用的模块方法
 * @param context 回调的上下文，可为nullptr，有值的时候业务可通过KRRenderModuleDoCallback回调数据给kotlin调用方
 * @return KRAnyData
 * @note 返回值KRAnyData由框架Destroy
 */
typedef KRAnyData (*KRRenderModuleCallMethodV2)(const void* moduleInstance, const char* moduleName, int sync, const char *method, KRAnyData param, KRRenderModuleCallbackContext context);

```

2. 使用方式
- module实现
```c
static void* ExampleModuleOnConstruct(const char *moduleName){
    return nullptr;
}

static ExampleModuleOnDestruct(const void* moduleInstance){
    // since nullptr was returned in ExampleModuleOnConstruct,
    // we don't need to do anything here
}

static KRAnyData ExampleModuleOnCallMethod(const void* moduleInstance,
    const char* moduleName,
    int sync,
    const char *method,
    KRAnyData param,
    KRRenderModuleCallbackContext context){
    
    if(context){
        // Do some work and callback later.
        // For the sake of simplicity, a thread is used here to illustrate the async behavior,
        // which might probably not be the best practice.
        std::thread([context] { 
            char* result = "{\"key\":\"value\"}";
            KRRenderModuleDoCallback(context, result);
        }).detach();
    }
    
    // 传参的值可以根据使用 KRAnyDataIsXXX 和 KRAnyDataGetXXX 判断和取值。

    std::string resultString(method ? method: "");
    resultString.append(" handled.");
    return KRAnyDataCreateString(resultString.c_str());
}

```

- module注册

推荐在`InitKuikly`过程中 实现`module`的注册
```c
KRRenderModuleRegisterV2("MyExampleCModule", &ExampleModuleOnConstruct, &ExampleModuleOnDestruct, &ExampleModuleOnCallMethod, nullptr);

static int adapterRegistered = false;
static napi_value InitKuikly(napi_env env, napi_callback_info info) {
    if(!adapterRegistered){
        registerExampleCModule();
        
        /*
            。。。。
        */ 
        
        adapterRegistered = true;
    }
    
    auto api = libshared_symbols();
    int handler = api->kotlin.root.initKuikly();
    napi_value result;
    napi_create_int32(env, handler, &result);
    return result;
}

```

- 使用注意事项
1. 作为返回值的的 `KRAnyData` 由框架做释放 不需要 额外调用 `KRAnyDataDestroy`
2. Ets Module和C Module同时存在时优先调用C Module


## H5侧
1. 在接入``Kuikly``的H5宿主工程中新建``KRMyLogModule``类，然后继承``KuiklyRenderBaseModule``，并重写其``call``方法（``call``方法有两个实现，**根据Module传输的数据类型，选择重写其中之一**）

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {
    // 传输基本类型、数组、字符串
    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {

    }

    // 传输Json（会被序列化为Json字符串）
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
       
    }
}
```

``Kuikly``的``MyLogModule``的``toNative``方法最终会调用原生对应的``Module``的``call``方法，也就是``KRMyLogModule``中的``call``方法。

在``Kuikly``的``MyLogModule``中定义了三个方法，下面我们来看在H5侧如何实现这三个方法

### log方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            else -> super.call(method, params, callback)
        }
    }

    private fun log(content: String) {
        Log.d("test", content)
    }

}
```

在``call``方法中，我们通过``method``参数来识别``log``方法，然后调用我们定义的私有方法``log``，并将``Kuikly``侧传递过来的``content``参数传递给``log``方法

### logWithCallback方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            "logWithCallback" -> logWithCallback(params ?: "", callback)
            else -> super.call(method, params, callback)
        }
    }

    private fun logWithCallback(content: String, callback: KuiklyRenderCallback?) {
        Log.d("test", content) // 1. 打印日志
        callback?.invoke(mapOf(
            "result" to "1",
            // 这里可以使用H5宿主提供的方法和属性，做你自己想做的事情 
            "locationHref" to window.location.href
        )) // 2. callback对应kuikly module侧方法的callbackFn, 此处将数据存放到map中并传递给kuikly侧
    }
    ...
}
```

### syncLog方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            "logWithCallback" -> logWithCallback(params ?: "", callback)
            "syncLog" -> syncLog(params ?: "")
            else -> super.call(method, params, callback)
        }
    }
    
    private fun syncLog(content: String): String {
        Log.d("test", content) // 1. 打印日志
        return "success" // 2. 将字符串同步返回给kuikly侧
    }
    ...
}
```

2. 原生侧实现完API后，我们将``KRMyLogModule``注册暴露到``Kuikly``中，与``Kuikly``侧的``MyLogModule``对应起来。在实现了``KuiklyRenderViewDelegatorDelegate``接口
   类中重写``registerExternalModule``方法，注册``KRMyLogModule``

:::tip 注意
注册的名字必须与``Kuikly moudle``侧注册的名字一样
::::

```kotlin
    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            ...
            moduleExport("KRMyLogModule") {
                KRMyLogModule()
            }
        }
    }
```
## 微信小程序侧
1. 在接入``Kuikly``的微信小程序宿主工程中新建``KRMyLogModule``类，然后继承``KuiklyRenderBaseModule``，并重写其``call``方法（``call``方法有两个实现，**根据Module传输的数据类型，选择重写其中之一**）

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {
    // 传输基本类型、数组、字符串
    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {

    }

    // 传输Json（会被序列化为Json字符串）
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
       
    }
}
```

``Kuikly``的``MyLogModule``的``toNative``方法最终会调用原生对应的``Module``的``call``方法，也就是``KRMyLogModule``中的``call``方法。

kuikly提供了几个变量，方便大家拓展微信小程序的Native接口
1. ``NativeApi.plat``就是全局的``wx``对象, ``NativeApi.plat.showToast`` = ``wx.showToast``
2. ``NativeApi.globalThis``, 就是微信小程序全局的``global``

在``Kuikly``的``MyLogModule``中定义了三个方法，下面我们来看在微信小程序侧如何实现这三个方法

### log方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            else -> super.call(method, params, callback)
        }
    }

    private fun log(content: String) {
        Log.d("test", content)
    }

}
```

在``call``方法中，我们通过``method``参数来识别``log``方法，然后调用我们定义的私有方法``log``，并将``Kuikly``侧传递过来的``content``参数传递给``log``方法

### logWithCallback方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            "logWithCallback" -> logWithCallback(params ?: "", callback)
            else -> super.call(method, params, callback)
        }
    }

    private fun logWithCallback(content: String, callback: KuiklyRenderCallback?) {
        Log.d("test", content) // 1. 打印日志
        callback?.invoke(mapOf(
            "result" to "1",
            "platform" to NativeApi.plat.getSystemInfoSync().platform
        )) // 2. callback对应kuikly module侧方法的callbackFn, 此处将数据存放到map中并传递给kuikly侧
    }
    ...
}
```

### syncLog方法

```kotlin
class KRMyLogModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "log" -> log(params ?: "")
            "logWithCallback" -> logWithCallback(params ?: "", callback)
            "syncLog" -> syncLog(params ?: "")
            else -> super.call(method, params, callback)
        }
    }
    
    private fun syncLog(content: String): String {
        Log.d("test", content) // 1. 打印日志
        return "success" // 2. 将字符串同步返回给kuikly侧
    }
    ...
}
```

2. 原生侧实现完API后，我们将``KRMyLogModule``注册暴露到``Kuikly``中，与``Kuikly``侧的``MyLogModule``对应起来。在实现了``KuiklyRenderViewDelegatorDelegate``接口
   类中重写``registerExternalModule``方法，注册``KRMyLogModule``

:::tip 注意
注册的名字必须与``Kuikly moudle``侧注册的名字一样
::::

```kotlin
    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            ...
            moduleExport("KRMyLogModule") {
                KRMyLogModule()
            }
        }
    }
```