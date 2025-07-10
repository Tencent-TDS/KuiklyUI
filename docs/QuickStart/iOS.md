# 现有iOS工程接入

:::tip 注意
在此之前请确保已经完成**KMP侧 Kuikly**的接入，如还未完成，请移步[KMP跨端工程接入](./common.md)
:::

完成**Kuikly KMP**侧的配置后, 我们还需要将**Kuikly**渲染器和适配器接入到宿主平台中，此文档适用于您想把Kuikly渲染器接入到您现有的iOS工程中。下面我们来看下，如何在现有iOS工程中接入Kuikly渲染器。

我们先新建一个名为**KuiklyTest**新工程并假设这个工程是你现有的iOS工程

<div align="center">
<img src="./img/new_ios_project.png">
</div>

## 添加Kuikly iOS 渲染器依赖

1. 添加``kuikly ios render``, 在你的工程的podfile添加以下代码

```ruby{1,9}
source 'https://cdn.cocoapods.org/'

platform :ios, '14.1'

target 'KuiklyTest' do

inhibit_all_warnings!

pod 'OpenKuiklyIOSRender', 'KUIKLY_RENDER_VERSION'
end
```
:::tip 提示
* KUIKLY_RENDER_VERSION 需要替换为实际的 Kuikly 版本号，在这里[查看最新版本](../ChangeLog/changelog.md)
* 版本号需要和[KMP跨端工程](common.md)保持一致
:::

2. 执行``pod install --repo-update``安装依赖

## 实现Kuikly承载容器

在你现有的iOS工程中，新建KuiklyRenderViewController, 作为``Kuikly``页面的容器。

具体实现代码，请参考源码工程iOSApp模块的``KuiklyRenderViewController``类。

```objc
#import <UIKit/UIKit.h>
NS_ASSUME_NONNULL_BEGIN

@interface KuiklyRenderViewController : UIViewController

/*
 * @brief 创建实例对应的初始化方法.
 * @param pageName 页面名 （对应的值为kotlin侧页面注解 @Page("xxxx")中的xxx名）
 * @param params 页面对应的参数（kotlin侧可通过pageData.params获取）
 * @return 返回KuiklyRenderViewController实例
 */
- (instancetype)initWithPageName:(NSString *)pageName pageData:(NSDictionary *)pageData;
@end

NS_ASSUME_NONNULL_END
```

```objc
#import "KuiklyRenderViewController.h"
#import <OpenKuiklyIOSRender/KuiklyRenderViewControllerBaseDelegator.h>
#import <OpenKuiklyIOSRender/KuiklyRenderContextProtocol.h>

#define HRWeakSelf __weak typeof(self) weakSelf = self;
@interface KuiklyRenderViewController()<KuiklyRenderViewControllerBaseDelegatorDelegate>

@property (nonatomic, strong) KuiklyRenderViewControllerBaseDelegator *delegator;

@end

@implementation KuiklyRenderViewController {
    NSDictionary *_pageData;
}

- (instancetype)initWithPageName:(NSString *)pageName pageData:(NSDictionary *)pageData {
    if (self = [super init]) {
        // 存储页面数据
        pageData = [self p_mergeExtParamsWithOriditalParam:pageData];
        _pageData = pageData;
        // 实例化Kuikly委托者类
        _delegator = [[KuiklyRenderViewControllerDelegator alloc] initWithPageName:pageName pageData:pageData];
        _delegator.delegate = self;
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor whiteColor];
    // 通知Kuikly页面ViewDidLoad
    [_delegator viewDidLoadWithView:self.view];
    [self.navigationController setNavigationBarHidden:YES animated:NO];

}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    // 通知Kuikly页面viewDidLayoutSubviews
    [_delegator viewDidLayoutSubviews];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    // 通知Kuikly页面viewWillAppear
    [_delegator viewWillAppear];
    [self.navigationController setNavigationBarHidden:YES animated:NO];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    // 通知Kuikly页面viewDidAppear
    [_delegator viewDidAppear];
    [self.navigationController setNavigationBarHidden:YES animated:NO];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    // 通知Kuikly页面viewWillDisappear
    [_delegator viewWillDisappear];
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    // 通知Kuikly页面viewDidDisappear
    [_delegator viewDidDisappear];
}

#pragma mark - private

- (NSDictionary *)p_mergeExtParamsWithOriditalParam:(NSDictionary *)pageParam {
    NSMutableDictionary *mParam = [(pageParam ?: @{}) mutableCopy];
 
    return mParam;
}

#pragma mark - KuiklyRenderViewControllerDelegatorDelegate

// 创建等待加载视图
- (UIView *)createLoadingView {
    UIView *loadingView = [[UIView alloc] init];
    loadingView.backgroundColor = [UIColor whiteColor];
    return loadingView;
}

// 创建加载错误视图
- (UIView *)createErrorView {
    UIView *errorView = [[UIView alloc] init];
    errorView.backgroundColor = [UIColor whiteColor];
    return errorView;
}

// 设置业务代码编译成的framework名字
- (void)fetchContextCodeWithPageName:(NSString *)pageName resultCallback:(KuiklyContextCodeCallback)callback {
    if (callback) {
        // 返回对应framework名字
        callback(@"shared", nil);
    }
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}


@end
```

## 实现Kuikly适配器（必须实现部分）

``Kuikly``框架为了灵活和可拓展性，通过适配器的设计模式，将一些功能的具体实现委托给宿主App实现。

``Kuikly``为iOS宿主工程提供了以下适配器

1. **图片加载适配器**: 用于给Kuikly的Image组件实现图片下载解码能力。**宿主侧必须实现**
2. **页面路由适配器**: 用于实现跳转到``Kuikly``容器。**宿主侧必须实现**
3. **日志适配器**: 用于给Kuikly框架和Kuikly业务实现日志打印。**推荐宿主侧实现**
4. **异常适配器**: 当Kuikly业务执行逻辑出错时，决定如何处理异常。**推荐宿主侧实现**
5. **颜色值转换适配器**: Kuikly框架对颜色值的处理，默认只处理十六进制的颜色值。**宿主按需实现**
6. **APNG图片加载适配器**: 用于给Kuikly提供APNG图片加载的能力。**宿主按需实现（使用APNG组件时必须实现）**

### 实现图片加载适配器

具体实现代码，请参考源码工程iOSApp模块的``KuiklyRenderComponentExpandHandler``类。

```objc
// .h
#import <Foundation/Foundation.h>
#import <OpenKuiklyIOSRender/KuiklyRenderBridge.h>

NS_ASSUME_NONNULL_BEGIN

/*
 * @brief 需要接入方自定义实现图片加载
 */
@interface KuiklyRenderComponentExpandHandler : NSObject<KuiklyRenderComponentExpandProtocol>


NS_ASSUME_NONNULL_END

@end
```

```objc
// .m
#import "KuiklyRenderComponentExpandHandler.h"
#import <SDWebImage/UIImageView+WebCache.h>

@implementation KuiklyRenderComponentExpandHandler

+ (void)load {
    // 注册自定义实现
    [KuiklyRenderBridge registerComponentExpandHandler:[self new]];
}

/*
 * 自定义实现设置图片
 * @param url 设置的图片url，如果url为nil，则是取消图片设置，需要view.image = nil
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 */
- (BOOL)hr_setImageWithUrl:(NSString *)url forImageView:(UIImageView *)imageView {
    // 图片下载我们使用
    [imageView sd_setImageWithURL:[NSURL URLWithString:url]];
    return YES;
}
/*
...

@end
```

### 实现页面路由适配器

具体实现代码，请参考源码工程iOSApp模块的``KRRouterHandler``类。

```objc
#import <Foundation/Foundation.h>
#import <OpenKuiklyIOSRender/KRRouterModule.h>
NS_ASSUME_NONNULL_BEGIN

@interface KRRouterHandler : NSObject<KRRouterProtocol>

@end

NS_ASSUME_NONNULL_END
```

```objc
#import "KRRouterHandler.h"
#import "KuiklyRenderViewController.h"

@implementation KRRouterHandler

// 注册适配器
+ (void)load {
    [KRRouterModule registerRouterHandler:[self new]];
}

// 打开页面
- (void)openPageWithName:(NSString *)pageName pageData:(NSDictionary *)pageData controller:(UIViewController *)controller {
    KuiklyRenderViewController *renderViewController = [[KuiklyRenderViewController alloc] initWithPageName:pageName pageData:pageData];
    [controller.navigationController pushViewController:renderViewController animated:YES];
}

// 关闭页面
- (void)closePage:(UIViewController *)controller {
    [controller.navigationController popViewControllerAnimated:YES];
}

@end
```

### 实现日志适配器
该适配器Kuikly有默认实现，非必须实现, 业务可根据实际使用需求来决定是否实现。
具体实现代码，请参考源码工程core-render-ios模块的``KuiklyLogHandler``类。
```objc
@interface KuiklyLogHandler : NSObject<KuiklyLogProtocol>

@end

@implementation KuiklyLogHandler

- (BOOL)asyncLogEnable {
    return NO;
}

- (void)logInfo:(NSString *)message {
    NSLog(@"%@", message);
}

- (void)logDebug:(NSString *)message {
#if DEBUG
    NSLog(@"%@", message);
#endif
}

- (void)logError:(NSString *)message {
    NSLog(@"%@", message);
}

@end
```

### 实现异常适配器
该适配器非必须实现, 业务可根据实际使用需求来决定是否实现。
如需实现，需重写`KuiklyRenderViewControllerBaseDelegatorDelegate`协议的`onUnhandledException:stack:mode:`方法，具体实现代码，请参考源码工程iOSApp模块的``KuiklyRenderViewController``类。

```objc
@implementation KuiklyRenderViewController

- (void)onUnhandledException:(NSString *)exReason stack:(NSString *)callstackStr mode:(KuiklyContextMode)mode
{
    // 处理异常或异常上报
}

@end
```

其他按需实现适配器示例参考[实现适配器（按需实现部分）](#实现适配器-按需实现部分)

## 链接Kuikly业务代码

Kuikly业务代码，在iOS平台上会被编译成``.framework``产物，下一步，我们将编译好的Kuikly业务代码``.framework``链接到你的工程。
``.framework``的集成，你可以选择远程pod接入或者本地pod接入。这里为了方便，我只演示本地pod接入。

我们先前在``Kuikly``[KMP跨端工程接入](common.md)中已经新建了``Kuikly``业务工程，然后我们将这个业务工程的业务代码编译成的``.framework``链接到我们的现有iOS工程

```ruby{10}
source 'https://cdn.cocoapods.org/'

platform :ios, '14.1'

target 'KuiklyTest' do

inhibit_all_warnings!

pod 'OpenKuiklyIOSRender', 'KUIKLY_RENDER_VERSION'
pod 'shared', :path => '/Users/XXX/workspace/TestKuikly/shared' # 本地存放Kuikly业务代码工程路径
end

```

重新执行``pod install``安装依赖

## 编写TestPage验证

1. 完成上述步骤后, 我们便完成了Kuikly的接入。下面我们在shared模块中编写TestPage，验证是否接入成功。我们在之前[KMP跨端工程接入](common.md)中新建的工程中的
``shared``模块中新建``TestPage``, 编写业务代码。新增的页面的名字为test

```kotlin
@Page("test")
class TestPage : Pager(){
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    fontSize(18f)
                    text("Hello Kuikly")
                    color(Color.GREEN)
                }
            }
        }
    }
}
```

2. 接着我们初始化``KuiklyRenderViewController``，并将``test``作为``pageName``传入容器中, 指定跳转到我们刚新建的``TestPage``中

```objc
- (void)pushKRView {
    KuiklyRenderViewController *vc = [[KuiklyRenderViewController alloc] initWithPageName:@"test" pageData:nil];
        [self.navigationController pushViewController:vc animated:YES];
}
```

3. 当手机出现以下界面时, 说明接入已经成功接入Kuikly

<div align="center">
<img src="./img/hello_kuikly_ios.png" style="width: 30%; border: 1px gray solid">
</div>

## 实现适配器（按需实现部分）
### 实现颜色值转换适配器

通过该适配器自定义颜色转换逻辑，业务可根据实际使用需求来决定是否实现
具体实现代码，请参考源码工程iOSApp模块的``KuiklyRenderComponentExpandHandler``类。

```objc
@implementation KuiklyRenderComponentExpandHandler
/*
 * 自定义实现设置颜色值
 * @param value 设置的颜色值
 * @return 完成自定义处理的颜色对象
 */
- (UIColor *)hr_colorWithValue:(NSString *)value {
    return nil;
}
```

### 实现APNG图片加载适配器

通过该适配器加载并显示APNG图片，当业务需要使用APNG组件时必须实现该适配器
具体实现代码，请参考源码工程iOSApp模块的``KRAPNGViewHandler``类。

```objc
@interface KRAPNGViewHandler : SDAnimatedImageView<APNGImageViewProtocol>

@end

@implementation KRAPNGViewHandler

+ (void)load {
    [KRAPNGView registerAPNGViewCreator:^id<APNGImageViewProtocol> _Nonnull(CGRect frame) {
        KRAPNGViewHandler *apngView = [[KRAPNGViewHandler alloc] initWithFrame:frame];
        return apngView;
    }];
}

@end
```






