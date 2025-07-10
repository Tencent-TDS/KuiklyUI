# 扩展原生View

``Kuikly``已经封装了常用的View组件，例如``Text``、``Image``和``List``等组件，如果``Kuikly``提供的组件不满足你的需求或者你想复用native已有的UI组件的话，
``Kuikly``允许你将现有的UI组件通过实现一些接口来暴露给``Kuikly``侧使用。

下面我们以``MyImageView``作为例子，来看看将原生的``ImageView``暴露给``Kuikly``使用需要做哪些工作

1. ``Kuikly``侧: 新增ImageView组件，供业务方使用
2. ``Android``侧: 实现``IKuiklyRenderViewExport``接口，完成``android``的``ImageView``暴露给``Kuikly``侧
3. ``iOS``侧: 实现`KuiklyRenderViewExportProtocol`协议，完成`iOS`的`UIImageView`暴露给`Kuikly`侧
4. 鸿蒙侧: 继承`KuiklyRenderBaseView`类，按需实现`call`、`createArkUIView`和`setProp`方法


## Kuikly侧

``Kuikly``组件由这四个部分组成：
1. ``viewName``: 组件对应到原生组件的名字
2. ``Attr``: 组件的属性，用于指定该组件含有哪些属性
3. ``Event``: 用于接收来自原生组件发送的事件
4. ``test``: 组件本身支持的方法，最终实现是在原生侧

我们完成上述的四部分，即可实现一个``Kuikly``侧的组件。

1. 我们首先新建``MyImageView``, 并继承``DeclarativeBaseView``类，继承``DeclarativeBaseView``的时候，需要指定``MyImageView``对应的``Attr``和``Event``类型

```kotlin
class MyImageView : DeclarativeBaseView<MyImageAttr, MyImageEvent>() {

    override fun createAttr(): MyImageAttr {
        return MyImageAttr()
    }

    override fun createEvent(): MyImageEvent {
        return MyImageEvent()
    }

    override fun viewName(): String {
        return "HRImageView"
    }
    
    fun test() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("test", "params")
        }
    }    
}
```

2. 接着在``viewName()``方法中，我们返回了``HRImageView``，表示该``Kuikly``的``MyImageView``对应到原生组件暴露给``Kuikly``侧的名字是``HRImageView``

3. 最后我们在``createAttr``和``createEvent``中，返回了我们在``DeclarativeBaseView``泛型中指定的类型对应的实例

4. ``Attr``表示``Kuikly``侧组件支持的属性集。在这个例子中，我们返回了``MyImageAttr``来表示支持的属性
```kotlin
class MyImageAttr : Attr() {

    /**
     * 设置src
     * @param src 数据源
     * @return this
     */
    fun src(src: String): MyImageAttr {
        "src" with src
        return this
    }
}
// MyImageAttr中，我们编写了src方法，表示MyImageView组件支持src属性，用于设置MyImageView的数据源。
// 在src中方法中，我们将传递进来的属性透传给了原生的组件
```

5. ``Event``表示``Kuikly``侧组件支持的事件。在这个例子中，我们返回了``MyImageEvent``来表示支持的事件
```kotlin
/**
 * MyImageEvent支持loadSuccess事件，表示Image的图片加载上屏时
 * 会触发loadSuccess中的handler闭包调用
 */
 class MyImageEvent : Event() {
    /*
     * 图片成功加载时回调
     * 由原生侧的组件触发
     */
    fun loadSuccess(handler: (LoadSuccessParams) -> Unit) {
        register(LOAD_SUCCESS) {
            handler(LoadSuccessParams.decode(it))
        }
    }
    companion object {
        const val LOAD_SUCCESS = "loadSuccess"
    }
}

data class LoadSuccessParams(
    val src: String
) {
    companion object {
        fun decode(params: Any?): LoadSuccessParams {
            val tempParams = params as? JSONObject ?: JSONObject()
            val src = tempParams.optString("src", "")
            return LoadSuccessParams(src)
        }
    }
}
```

6. 实现``test``方法

```kotlin
class MyImageView : DeclarativeBaseView<MyImageAttr, MyImageEvent>() {
    ...
    fun test() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("test", "params")
        }
    }
    ...  
}
```

**test**为组件自身暴露出去的方法，这种方法真正的实现在原生侧实现的，在这个例子中，如果MyImageView对应到原生的组件为HRImageView，那这个test方法会调用到HRImageView的call("test", "params")方法中

:::tip 注意
View的方法支持异步回调结果，即调用方法时传入回调函数`renderView?.callMethod("test", "params", callback)`，但不支持同步返回结果。
:::

8. 最后我们编写``MyImageView``组件声明式的api方法，供业务侧调用
```kotlin
fun ViewContainer<*, *>.MyImage(init: MyImageView.() -> Unit) {
    addChild(MyImageView(), init)
} 
```

7. 业务侧使用方式
```kotlin
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            MyImage {
                attr {
                    size(176f, 132f)
                    src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/844aa82b.png")
                }
                event {
                    loadSuccess {
                    
                    }
                }
            }
        }
    } 
```

完成上述步骤后, 业务方使用可以使用这个``MyImageView``组件了，但是具体的渲染还需要对应的平台实现，下面我们来看``android``平台和``iOS``平台如何暴露``ImageView``给``Kuikly``侧

## android侧

``android``侧要完成原生``ImageView``暴露给``Kuikly``侧，需要完成以下步骤
1. 新建``HRImageView``并继承原生的``ImageView``, 然后实现``IKuiklyRenderViewExport``接口
2. 实现``IKuiklyRenderViewExport``中的``setProp``方法
3. 实现``IKuiklyRenderViewExport``中的``call``方法
4. 注册``HRImageView``，完成``HRImageView``暴露给``Kuikly``侧

我们完成上述4部分后，即可实现将``HRImageView``组件暴露给``Kuikly``侧

1. 我们首先新建HRImageView，并继承原生的``ImageView``，然后实现``IKuiklyRenderViewExport``接口
```kotlin
open class HRImageView(context: Context) : ImageView(context), IKuiklyRenderViewExport {

    override fun setProp(propKey: String, propValue: Any): Boolean {
       return super.setProp(propKey, propValue)
    }
    
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return super.call(method, params, callback)
    }

}
```

``HRImageView``中实现了``setProp``和``call``方法

1. ``setProp``方法: ``Kuikly``侧组件支持的**属性**和**事件**调用都会走到这个方法。例如上述的**属性**``src``和**事件**``loadSuccess``
2. ``call``方法: ``Kuikly``侧组件支持的**方法**调用都会走到这个方法。例如上述的**方法**``test``

### 实现src属性和loadSuccess事件

前面讲到``Kuikly``的``ImageView``组件，它支持**src属性**和**loadSuccess事件**，在运行的时候会调用到我们新建的``HRImageView``的``setProp``方法，我们来看下如何实现

#### 实现src属性
```kotlin
open class HRImageView(context: Context) : ImageView(context), IKuiklyRenderViewExport {

    private var src = ""
    
    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            "src" -> {
                setSrc(propValue as String)
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }
    
    private fun setSrc(url: String) {
        if (src == url) {
            return
        }
        src = url
        setImageDrawable(null)
        // 加载并设置图片
        val creator = Picasso.get().load(src)
        val target = object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                setImageDrawable(BitmapDrawable(bitmap))
            }
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                setImageDrawable(null)
            }
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }
        }
        creator.into(target)
    }

}
```

当Kuikly侧的MyImageView运行到设置src属性时，对应到端的组件，会走到HRImageView的setProp方法，propKey为属性的名字，即src，而propValue为
属性对应的值。HRImageView识别到propKey为src时，使用Kuikly侧组件传递过来的src来加载Drawable，然后设置给HRImageView。

#### 实现loadSuccess事件
```kotlin
open class HRImageView(context: Context) : ImageView(context), IKuiklyRenderViewExport {

    private var loadSuccessCallback: KuiklyRenderCallback? = null
    
    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            ...
            "loadSuccess" -> {
                loadSuccessCallback = propValue as KuiklyRenderCallback
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }
    
    private fun setSrc(url: String) {
        if (src == url) {
            return
        }
        src = url
        setImageDrawable(null)
        // 加载并设置图片
        val creator = Picasso.get().load(src)
        val target = object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                setImageDrawable(BitmapDrawable(bitmap))
            }
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                setImageDrawable(null)
            }
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }
        }
        creator.into(target)
    }
    
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        loadSuccessCallback?.invoke(mapOf(
            PROP_SRC to src
        ))
    }

}
```

**loadSuccess**表示，原生的ImageView的图片加载成功后，回调该事件给**Kuikly侧的组件，即MyImageView**。
所以我们在**HRImageView**中定义了``private var loadSuccessCallback: KuiklyRenderCallback? = null``变量，并在**setProp**方法中赋值

```kotlin
override fun setProp(propKey: String, propValue: Any): Boolean {
    return when (propKey) {
        ...
        "loadSuccess" -> {
            loadSuccessCallback = propValue as KuiklyRenderCallback
            true
        }
        else -> super.setProp(propKey, propValue)
    }
}
```

最后重写``HRImageView``的**setImageDrawable**，在此方法中触发**loadSuccessCallback**回调

### 实现call方法

在Kuikly侧的ImageView含有一个test方法，该方法实现为:
```kotlin
fun test() {
    performTaskWhenRenderViewDidLoad {
        renderView?.callMethod("test", "params")
    }
}
```

我们看到``renderView?.callMethod``方法传递了test方法名字和一个params字符串，这个调用会对应到HRImageView的call方法, 即
```kotlin
open class HRImageView(context: Context) : ImageView(context), IKuiklyRenderViewExport {
    
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when(method) {
            "test" -> callTestMethod(params)
            else -> super.call(method, params, callback)
        }
    }

    private fun callTestMethod(params: String?) {
        Log.d("HRImageView", "callTestMethod: $params")
    }
}
```
在上述的代码中，我们重写**HRImageView**的侧``call``方法，识别到方法名字为"test"时, 调用``callTestMethod``方法，以此来响应**Kuikly侧的ImageView.test方法的调用**

### 注册HRImageView到Kuikly中

原生侧完成HRImageView的编写后，还需要注册暴露给Kuikly侧，指定这个UI组件对应Kuikly侧组件的名字。我们在实现了``KuiklyRenderViewDelegatorDelegate``接口的类中重写registerExternalRenderView方法，
然后调用renderViewExport完成HRImageView的注册暴露

```kotlin
    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            renderViewExport("HRImageView", { context ->
                HRImageView(context)
            })
        }
    }
```

:::tip 注意
"HRImageView"为Kuikly中ImageView.getName返回的字符串
:::
## iOS侧

``iOS``侧要完成原生``UIImageView``暴露给``Kuikly``侧，需要完成以下步骤
1. 新建``HRImageView``并继承原生的``UIImageView``, 然后实现``KuiklyRenderViewExportProtocol``协议
2. 实现``KuiklyRenderViewExportProtocol``中的``hrv_setPropWithKey``方法
3. 实现``KuiklyRenderViewExportProtocol``中的``hrv_callWithMethod``方法

我们完成上述3部分后，即可实现将``HRImageView``组件暴露给``Kuikly``侧

我们首先新建``HRImageView``并继承原生的``UIImageView``, 然后实现``KuiklyRenderViewExportProtocol``协议

:::tip 注意
类名必须与Kuikly侧的ImageView.viewName()返回的值一样
:::

```objc
#import <UIKIt/UIKit.h>
#import "KuiklyRenderViewExportProtocol.h"

NS_ASSUME_NONNULL_BEGIN
/*
 * @brief 暴露给Kotlin侧调用的Image组件
 */
@interface HRImageView : UIImageView<KuiklyRenderViewExportProtocol>

@end

NS_ASSUME_NONNULL_END
```

### 实现hrv_setPropWithKey方法

接下来，我们实现**hrv_setPropWithKey**，并且调用KUIKLY_SET_CSS_COMMON_PROP宏。

```kotlin
#import "HRImageView.h"
#import "KRComponentDefine.h"
/*
 * @brief 暴露给Kotlin侧调用的Image组件
 */
@interface HRImageView()

@end

@implementation HRImageView

@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

...

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

...

@end
```

#### 实现src属性

当``Kuikly``侧的``ImageView``调用到``ImageAttr``的src方法时，会调用到iOS侧的``HRImageView``的``hrv_setPropWithKey``方法。
然后**KUIKLY_SET_CSS_COMMON_PROP**会使用运行时，调用``HRImageView``中匹配``setCss_propKey``的方法。以``src``属性作为例子，会调用
到``HRImageView``的**setCss_src**方法。因此，我们在``HRImageView``中新增**setCss_src**方法，以响应``Kuikly``侧的调用

```objc
#import "HRImageView.h"
#import "HRComponentDefine.h"
/*
 * @brief 暴露给Kotlin侧调用的Image组件
 */
@interface HRImageView()

@end

@implementation HRImageView

@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

...

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)setCss_src:(NSString *)css_src {
    1. css_src为kuikly传递过来的参数
    2. 使用css_src去下载图片得到一个UIImage
    3. 最后将UIImage设置给UIImageVIew
}

...

@end
```

#### 实现loadSuccess事件

**loadSuccess**回调的实现步骤与src相似。

```objc
#import "HRImageView.h"
#import "HRComponentDefine.h"
/*
 * @brief 暴露给Kotlin侧调用的Image组件
 */
@interface HRImageView()

/** 图片加载成功回调事件 */
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_loadSuccess;

@end

@implementation HRImageView

@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

...

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)setCss_loadSuccess:(KuiklyRenderCallback)css_loadSuccess {
    if (_css_loadSuccess != css_loadSuccess) {
        _css_loadSuccess = css_loadSuccess;
        if (css_loadSuccess && self.image) {
            [self p_fireLoadSuccessEvent];
        }
    }
}

- (void)setImage:(UIImage *)image {
    if (image && self.image != image) {
        [self p_fireLoadSuccessEvent];
    }
    [super setImage:image];
}

-(void)p_fireLoadSuccessEvent {
    if (_css_loadSuccess) {
        _css_loadSuccess( @{@"src" : self.css_src ?: @"" } );
    }
}

...

@end
```

### 实现hrv_callWithMethod方法

在实现完属性和回调后，我们来看看如何实现``Kuikly``侧的``test``方法调用。

```objc
#import "HRImageView.h"
#import "HRComponentDefine.h"
/*
 * @brief 暴露给Kotlin侧调用的Image组件
 */
@interface HRImageView()

@end

@implementation HRImageView

@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

...

- (NSString * _Nullable)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
    return nil;
}

...

@end
```

当``Kuikly``侧的``ImageView``调用test方法时，会调用到iOS侧的``HRImageView``的``hrv_callWithMethod``方法。
然后**KUIKLY_CALL_CSS_METHOD**会使用运行时，调用``HRImageView``中匹配``css_method``的方法。以``test``方法作为例子，会调用
到``HRImageView``的**css_test**方法。因此，我们在``HRImageView``中新增**css_test**方法，以响应``Kuikly``侧的调用

```objc
#import "HRImageView.h"
#import "HRComponentDefine.h"
/*
 * @brief 暴露给Kotlin侧调用的Image组件
 */
@interface HRImageView()

@end

@implementation HRImageView

@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

...

- (NSString * _Nullable)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
    return nil;
}

- (void)css_test {
    // 实现test方法
}

...

@end
```

完成以上步骤后，即可在iOS侧扩展``Kuikly``组件

:::tip 提示
iOS侧的kuikly组件是通过运行时暴露给Kuikly侧，因此无需手动注册
:::

## 鸿蒙侧

鸿蒙侧要完成原生``Image``暴露给``Kuikly``侧，需要完成以下步骤
1. 新建``HRImageView``并继承``KuiklyRenderBaseView``类
2. 重写``KuiklyRenderBaseView``中的``setProp``方法
3. 实现``KuiklyRenderBaseView``中的``call``方法
4. 实现``KuiklyRenderBaseView``中的``createArkUIView``方法
5. 注册``HRImageView``，将其暴露给``Kuikly``侧

我们完成上述5部分后，即可实现将``HRImageView``组件暴露给``Kuikly``侧

我们首先新建`HRImageView`，继承``KuiklyRenderBaseView``类
```ts
@Observed
export class HRImageView extends KuiklyRenderBaseView {
    static readonly VIEW_NAME = 'HRImageView';
    ...
}
```

### 重写setProp方法

前面讲到``Kuikly``的``MyImageView``组件，它支持**src属性**和**loadSuccess事件**，在运行的时候会调用到我们新建的``HRImageView``的``setProp``方法，我们来看下如何实现

#### 实现src属性
```ts
@Observed
export class HRImageView extends KuiklyRenderBaseView {
    static readonly VIEW_NAME = 'HRImageView';
    cssSrc: string | null = null;
    image: PixelMap | undefined = undefined;

    setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
        switch (propKey) {
            case 'src':
                this.setSrc(propValue as string);
                return true;
            default:
                return super.setProp(propKey, propValue);
        }
    }

    setSrc(url: string): void {
        1. 将this.cssSrc设为kuikly传递过来的参数
        2. 使用this.cssSrc去下载图片得到一个PixelMap
        3. 最后将PixelMap设置给this.image
    }
}
```

当Kuikly侧的`MyImageView`运行到设置`src`属性时，对应到端的组件，会走到`HRImageView`的`setProp`方法，`propKey`为属性的名字，即`src`，而`propValue`为属性对应的值。`HRImageView`识别到`propKey`为`src`时，使用kuikly侧组件传递过来的`src`来加载`PixelMap`。

#### 实现loadSuccess事件
```ts{13-15}
@Observed
export class HRImageView extends KuiklyRenderBaseView {
    static readonly VIEW_NAME = 'HRImageView';
    src: string | null = null;
    image: PixelMap | undefined = undefined;
    loadSuccessCallback: KuiklyRenderCallback | null = null;

    setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
        switch (propKey) {
            case 'src':
                this.setSrc(propValue as string);
                return true;
            case 'loadSuccess':
                this.setLoadSuccessCallback(propValue as KuiklyRenderCallback);
                return true;
            default:
                return super.setProp(propKey, propValue);
        }
    }

    setSrc(url: string): void {
        1. 将this.src设为kuikly传递过来的参数
        2. 使用this.src去下载图片得到一个PixelMap
        3. 最后将PixelMap设置给this.image
    }

    setLoadSuccessCallback(callback: KuiklyRenderCallback): void {
        if (this.loadSuccessCallback != callback) {
            this.loadSuccessCallback = callback;
            if (this.loadSuccessCallback != null && this.image) {
                this.fileLoadSuccess();
            }
        }
    }

    setImage(image: PixelMap): void {
        if (image && this.image != image) {
            this.image = image;
            this.fileLoadSuccess();
        }
    }

    fileLoadSuccess(): void {
        if (this.loadSuccessCallback) {
            this.loadSuccessCallback({
                "src": this.src
            });
        }
    }
}
```

`loadSuccess`表示，原生的`PixelMap`图片加载成功后，回调该事件给Kuikly侧的组件，即`MyImageView`。所以我们在`HRImageView`中定义了``loadSuccessCallback: KuiklyRenderCallback | null = null;``变量，并在`setProp`方法中赋值，并在`PixelMap`成功加载时调用`loadSuccessCallback`

### 实现call方法

在Kuikly侧的`ImageView`含有一个`test`方法，该方法实现为:
```kotlin
fun test() {
    performTaskWhenRenderViewDidLoad {
        renderView?.callMethod("test", "params")
    }
}
```

我们看到``renderView?.callMethod``方法传递了`test`方法名字和一个`params`字符串，这个调用会对应到`HRImageView`的`call`方法, 即
```ts
@Observed
export class HRImageView extends KuiklyRenderBaseView {
    ...
    call(method: string, params: KTAny, callback: KuiklyRenderCallback | null): void {
        switch (method) {
            case 'test':
                this.callTestMethod(params as string);
                return;
        }
    }

    callTestMethod(params: string) {
        console.log(`HRImageView callTestMethod: ${params}`);
    }
    ...
}
```
在上述的代码中，我们实现`KuiklyRenderBaseView`的``call``方法，识别到方法名字为"test"时, 调用``callTestMethod``方法，以此来响应Kuikly侧的`ImageView.test`方法的调用。

### 实现createArkUIView方法

`createArkUIView`方法用于创建组件实际的ArkUI视图，返回`ComponentContent`实例。

```ts
@Observed
export class HRImageView extends KuiklyRenderBaseView {
    ...
    createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
        const uiContext = this.getUIContext() as UIContext
        return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createMyImageView), this)
    }
    ...
}

@Builder
function createMyImageView(view: KuiklyRenderBaseView) {
  // 构造你的UI,如Column
  Column(){
    Image((view as HRImageView).image)
      .width('100%')
      .height('100%')
  }.width('100%').height('100%')
}
```

### 注册HRImageView到Kuikly中

原生侧完成HRImageView的编写后，还需要注册暴露给Kuikly侧，指定这个UI组件对应Kuikly侧组件的名字。我们在实现了``IKuiklyViewDelegate``接口的类中重写getCustomRenderViewCreatorRegisterMap方法:

```ts
export class KuiklyViewDelegate extends IKuiklyViewDelegate {
    ...
    getCustomRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator> {
        const map: Map<string, KRRenderViewExportCreator> = new Map();
        map.set(HRImageView.VIEW_NAME, () => new HRImageView());
        return map;
    }
}
```

:::tip 注意
HRImageView.VIEW_NAME需要与Kuikly侧的组件名相同，为"HRImageView"
:::