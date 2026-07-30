# iOS framework模式堆栈翻译

## 获取crash堆栈

### 方案一：通过bugly的错误列表查看

该方案需要业务自行捕获异常，并通过bugly的自定义错误接口，自行上传异常信息；示例代码如下：

```objc
# 注册异常通知
#import "KuiklyRenderCore.h"
[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleKuiklyException:) name:kKuiklyFatalExceptionNotification object:nil];

# 接收通知，并上报
- (void)handleKuiklyException:(NSNotification *)noti {
    if (noti.userInfo && noti.userInfo[@"exception"]) {
        NSString *exceptionString = noti.userInfo[@"exception"];
        NSArray *components = [exceptionString componentsSeparatedByString:@"\n"];
        NSString *exceptionName = [components firstObject];

        NSArray<NSString *> *callStackArray = [components subarrayWithRange:NSMakeRange(1, components.count - 1)];
        NSLog(@"xxxx %@, %@", exceptionName, callStackArray);
        // bugly上报示例
        [Bugly reportExceptionWithCategory:7 name:exceptionName reason:exceptionName callStack:callStackArray extraInfo:@{} terminateApp:YES];
    }
}
```



### 方案二：获取苹果的官方crash文件

苹果官方大体有3种方式获取用户crash，可查阅官方文档：

1. 有crash设备，直接连接crash设备的情况下，xcode获取crash.log文件
2. 联系用户，通过用户手机分享，[参考](https://developer.apple.com/documentation/xcode/acquiring-crash-reports-and-diagnostic-logs#Locate-crash-reports-and-memory-logs-on-the-device) 
3. xcode登录发布账号，在Window->Organizer->crash里面查看



## 符号表获取

**宿主以静态库方式集成Kuikly产物**（推荐）

静态库集成时，会直接编译到app的产物中去，因此直接使用app的dSYM符号表文件

**宿主以动态库方式集成Kuikly产物**

动态库集成时，Kuikly产物会单独生成符号表文件，可在`build/cocoapods/framework`目录下获取该dSYM文件。构建流水线将该文件归档，方便后续直接从流水线获取该符号表文件。
<br>Bugly已经支持业务在流水线自动上报符号表文件能力，在查看异常时，直接给出翻译后的堆栈。



**修改静态库与动态库集成方式**

```
// build.gradle.kts
kotlin {
    cocoapods {
        framework {
            baseName = "shared"
            isStatic = false			// here
        }
    }
}

```



## 堆栈翻译示例

**注意：** 上传到bugly后，bugly有符号表的情况下，可以直接翻译。
<br>如果是自行获取到crash文件的情况，也可以自行通过苹果的atos工具，进行翻译。

<br>在某些低Kotlin版本场景下，可能会遇到过栈顶的堆栈有偏移4的情况，可酌情尝试自行翻译栈顶的堆栈：



**kotlin 代码(Greeting.kt):**

```kotlin
package com.luoyibu.kmmdemo

class Greeting {
    private val platform: Platform = getPlatform()

    fun greet(): String {
        val list = listOf<Int>(1, 2, 3, 4)
        val a = list[5]
        return "Hello, ${platform.name}!"
    }
}
```

可以看到该代码在第8行（val a = list[5]）处有一个数组越界问题，会产生crash。我们通过该代码来模拟触发crash情况



**crash原始堆栈**

通过编译iOS的release版本app，运行后触发crash，我们可以获取到原始的iOS crash文件，对应堆栈如下

```
Exception Type:  EXC_CRASH (SIGABRT)
Exception Codes: 0x0000000000000000, 0x0000000000000000
Triggered by Thread:  0

Application Specific Information:
abort() called


Last Exception Backtrace:
0   iosApp                        	       0x100edacb4 ThrowArrayIndexOutOfBoundsException + 156
1   iosApp                        	       0x100eec0fc kfun:com.luoyibu.kmmdemo.IOSPlatform#<init>(){} + 0
2   iosApp                        	       0x100eefa20 objc2kotlin_kfun:com.luoyibu.kmmdemo.Greeting#greet(){}kotlin.String + 212
3   iosApp                        	       0x100ece870 closure #1 in iOSApp.body.getter + 60
4   SwiftUI                       	       0x1b0f75b74 0x1b0084000 + 15670132
5   iosApp                        	       0x100ece944 protocol witness for App.body.getter in conformance iOSApp + 148
6   SwiftUI                       	       0x1b06c0ff0 0x1b0084000 + 6541296
7   SwiftUI                       	       0x1b00ceda8 0x1b0084000 + 306600
8   SwiftUI                       	       0x1b02bd7bc 0x1b0084000 + 2332604
9   SwiftUI                       	       0x1b00c27a0 0x1b0084000 + 255904
10  SwiftUI                       	       0x1b0113af8 0x1b0084000 + 588536
11  AttributeGraph                	       0x1cf7b16f4 AG::Graph::UpdateStack::update() + 520
12  AttributeGraph                	       0x1cf7b0f44 AG::Graph::update_attribute(AG::data::ptr<AG::Node>, unsigned int) + 424
13  AttributeGraph                	       0x1cf7b2088 AG::Graph::input_value_ref_slow(AG::data::ptr<AG::Node>, AG::AttributeID, unsigned int, AGSwiftMetadata const*, unsigned char&, long) + 420
14  AttributeGraph                	       0x1cf7af71c AGGraphGetValue + 212
15  SwiftUI                       	       0x1b010a72c 0x1b0084000 + 550700
16  SwiftUI                       	       0x1b00c2778 0x1b0084000 + 255864
17  SwiftUI                       	       0x1b0113af8 0x1b0084000 + 588536
18  AttributeGraph                	       0x1cf7b16f4 AG::Graph::UpdateStack::update() + 520
19  AttributeGraph                	       0x1cf7b0f44 AG::Graph::update_attribute(AG::data::ptr<AG::Node>, unsigned int) + 424
20  AttributeGraph                	       0x1cf7b2088 AG::Graph::input_value_ref_slow(AG::data::ptr<AG::Node>, AG::AttributeID, unsigned int, AGSwiftMetadata const*, unsigned char&, long) + 420
21  AttributeGraph                	       0x1cf7af71c AGGraphGetValue + 212
22  SwiftUI                       	       0x1b02bfce0 0x1b0084000 + 2342112
23  SwiftUI                       	       0x1b01623cc 0x1b0084000 + 910284
24  SwiftUI                       	       0x1b01751a8 0x1b0084000 + 987560
25  AttributeGraph                	       0x1cf7b16f4 AG::Graph::UpdateStack::update() + 520
26  AttributeGraph                	       0x1cf7b0f44 AG::Graph::update_attribute(AG::data::ptr<AG::Node>, unsigned int) + 424
27  AttributeGraph                	       0x1cf7af558 AG::Graph::value_ref(AG::AttributeID, AGSwiftMetadata const*, unsigned char&) + 192
28  AttributeGraph                	       0x1cf7af764 AGGraphGetValue + 284
29  SwiftUI                       	       0x1b029ba00 0x1b0084000 + 2193920
30  SwiftUI                       	       0x1b1641bf4 0x1b0084000 + 22797300
31  SwiftUI                       	       0x1b008ef04 0x1b0084000 + 44804
32  SwiftUI                       	       0x1b01a6e4c 0x1b0084000 + 1191500
33  UIKitCore                     	       0x1aea079e4 +[UIScene _sceneForFBSScene:create:withSession:connectionOptions:] + 1036
34  UIKitCore                     	       0x1aea9e1f8 -[UIApplication _connectUISceneFromFBSScene:transitionContext:] + 888
35  UIKitCore                     	       0x1aea9dcd0 -[UIApplication workspace:didCreateScene:withTransitionContext:completion:] + 372
36  UIKitCore                     	       0x1aea9daf0 -[UIApplicationSceneClientAgent scene:didInitializeWithEvent:completion:] + 288
37  FrontBoardServices            	       0x1c243ce90 -[FBSScene _callOutQueue_agent_didCreateWithTransitionContext:completion:] + 344
38  FrontBoardServices            	       0x1c247c8f8 __92-[FBSWorkspaceScenesClient createSceneWithIdentity:parameters:transitionContext:completion:]_block_invoke.78 + 120
39  FrontBoardServices            	       0x1c2440c24 -[FBSWorkspace _calloutQueue_executeCalloutFromSource:withBlock:] + 168
40  FrontBoardServices            	       0x1c247c530 __92-[FBSWorkspaceScenesClient createSceneWithIdentity:parameters:transitionContext:completion:]_block_invoke + 360
41  libdispatch.dylib             	       0x1b3cdcf88 _dispatch_client_callout + 20
42  libdispatch.dylib             	       0x1b3ce0a08 _dispatch_block_invoke_direct + 264
43  FrontBoardServices            	       0x1c244ad40 __FBSSERIALQUEUE_IS_CALLING_OUT_TO_A_BLOCK__ + 52
44  FrontBoardServices            	       0x1c244a8dc -[FBSSerialQueue _targetQueue_performNextIfPossible] + 220
45  FrontBoardServices            	       0x1c244d184 -[FBSSerialQueue _performNextFromRunLoopSource] + 28
46  CoreFoundation                	       0x1ac790f24 __CFRUNLOOP_IS_CALLING_OUT_TO_A_SOURCE0_PERFORM_FUNCTION__ + 28
47  CoreFoundation                	       0x1ac79d2fc __CFRunLoopDoSource0 + 176
48  CoreFoundation                	       0x1ac721220 __CFRunLoopDoSources0 + 340
49  CoreFoundation                	       0x1ac736b7c __CFRunLoopRun + 836
50  CoreFoundation                	       0x1ac73beb0 CFRunLoopRunSpecific + 612
51  GraphicsServices              	       0x1e6931368 GSEventRunModal + 164
52  UIKitCore                     	       0x1aec31668 -[UIApplication _run] + 888
53  UIKitCore                     	       0x1aec312cc UIApplicationMain + 340
54  SwiftUI                       	       0x1b0252244 0x1b0084000 + 1892932
55  SwiftUI                       	       0x1b01b3278 0x1b0084000 + 1241720
56  SwiftUI                       	       0x1b019c62c 0x1b0084000 + 1148460
57  iosApp                        	       0x100ece9cc main + 36
58  dyld                          	       0x1cb034960 start + 2528

Thread 0 name:   Dispatch queue: com.apple.main-thread
Thread 0 Crashed:
0   libsystem_kernel.dylib        	       0x1ea1df674 __pthread_kill + 8
1   libsystem_pthread.dylib       	       0x1fa9431ac pthread_kill + 268
2   libsystem_c.dylib             	       0x1b3d40c8c abort + 180
3   iosApp                        	       0x100f0593c konan::abort() + 12
4   iosApp                        	       0x100f0c4a0 (anonymous namespace)::terminateWithUnhandledException(ObjHeader*)::$_1::operator()() const + 36
5   iosApp                        	       0x100f0c3d0 void (anonymous namespace)::$_0::operator()<(anonymous namespace)::terminateWithUnhandledException(ObjHeader*)::$_1>((anonymous namespace)::terminateWithUnhandledException(ObjHeader*)::$_1) + 60
6   iosApp                        	       0x100f0c234 (anonymous namespace)::terminateWithUnhandledException(ObjHeader*) + 12
7   iosApp                        	       0x100f0c214 (anonymous namespace)::processUnhandledException(ObjHeader*) + 2732
8   iosApp                        	       0x100f0d1ec kotlin::ProcessUnhandledException(ObjHeader*) + 96
9   iosApp                        	       0x100f0e9a0 Kotlin_ObjCExport_trapOnUndeclaredException + 28
10  iosApp                        	       0x100eefaa4 objc2kotlin_kfun:com.luoyibu.kmmdemo.Greeting#greet(){}kotlin.String + 344
11  iosApp                        	       0x100ece870 closure #1 in iOSApp.body.getter + 60
12  SwiftUI                       	       0x1b0f75b74 0x1b0084000 + 15670132
13  iosApp                        	       0x100ece944 protocol witness for App.body.getter in conformance iOSApp + 148
14  SwiftUI                       	       0x1b06c0ff0 0x1b0084000 + 6541296
15  SwiftUI                       	       0x1b00ceda8 0x1b0084000 + 306600
16  SwiftUI                       	       0x1b02bd7bc 0x1b0084000 + 2332604
17  SwiftUI                       	       0x1b00c27a0 0x1b0084000 + 255904
18  SwiftUI                       	       0x1b0113af8 0x1b0084000 + 588536
19  AttributeGraph                	       0x1cf7b16f4 AG::Graph::UpdateStack::update() + 520
20  AttributeGraph                	       0x1cf7b0f44 AG::Graph::update_attribute(AG::data::ptr<AG::Node>, unsigned int) + 424
21  AttributeGraph                	       0x1cf7b2088 AG::Graph::input_value_ref_slow(AG::data::ptr<AG::Node>, AG::AttributeID, unsigned int, AGSwiftMetadata const*, unsigned char&, long) + 420
22  AttributeGraph                	       0x1cf7af71c AGGraphGetValue + 212
23  SwiftUI                       	       0x1b010a72c 0x1b0084000 + 550700
24  SwiftUI                       	       0x1b00c2778 0x1b0084000 + 255864
25  SwiftUI                       	       0x1b0113af8 0x1b0084000 + 588536
26  AttributeGraph                	       0x1cf7b16f4 AG::Graph::UpdateStack::update() + 520
27  AttributeGraph                	       0x1cf7b0f44 AG::Graph::update_attribute(AG::data::ptr<AG::Node>, unsigned int) + 424
28  AttributeGraph                	       0x1cf7b2088 AG::Graph::input_value_ref_slow(AG::data::ptr<AG::Node>, AG::AttributeID, unsigned int, AGSwiftMetadata const*, unsigned char&, long) + 420
29  AttributeGraph                	       0x1cf7af71c AGGraphGetValue + 212
30  SwiftUI                       	       0x1b02bfce0 0x1b0084000 + 2342112
31  SwiftUI                       	       0x1b01623cc 0x1b0084000 + 910284
32  SwiftUI                       	       0x1b01751a8 0x1b0084000 + 987560
33  AttributeGraph                	       0x1cf7b16f4 AG::Graph::UpdateStack::update() + 520
34  AttributeGraph                	       0x1cf7b0f44 AG::Graph::update_attribute(AG::data::ptr<AG::Node>, unsigned int) + 424
35  AttributeGraph                	       0x1cf7af558 AG::Graph::value_ref(AG::AttributeID, AGSwiftMetadata const*, unsigned char&) + 192
36  AttributeGraph                	       0x1cf7af764 AGGraphGetValue + 284
37  SwiftUI                       	       0x1b029ba00 0x1b0084000 + 2193920
38  SwiftUI                       	       0x1b1641bf4 0x1b0084000 + 22797300
39  SwiftUI                       	       0x1b008ef04 0x1b0084000 + 44804
40  SwiftUI                       	       0x1b01a6e4c 0x1b0084000 + 1191500
41  UIKitCore                     	       0x1aea079e4 +[UIScene _sceneForFBSScene:create:withSession:connectionOptions:] + 1036
42  UIKitCore                     	       0x1aea9e1f8 -[UIApplication _connectUISceneFromFBSScene:transitionContext:] + 888
43  UIKitCore                     	       0x1aea9dcd0 -[UIApplication workspace:didCreateScene:withTransitionContext:completion:] + 372
44  UIKitCore                     	       0x1aea9daf0 -[UIApplicationSceneClientAgent scene:didInitializeWithEvent:completion:] + 288
45  FrontBoardServices            	       0x1c243ce90 -[FBSScene _callOutQueue_agent_didCreateWithTransitionContext:completion:] + 344
46  FrontBoardServices            	       0x1c247c8f8 __92-[FBSWorkspaceScenesClient createSceneWithIdentity:parameters:transitionContext:completion:]_block_invoke.78 + 120
47  FrontBoardServices            	       0x1c2440c24 -[FBSWorkspace _calloutQueue_executeCalloutFromSource:withBlock:] + 168
48  FrontBoardServices            	       0x1c247c530 __92-[FBSWorkspaceScenesClient createSceneWithIdentity:parameters:transitionContext:completion:]_block_invoke + 360
49  libdispatch.dylib             	       0x1b3cdcf88 _dispatch_client_callout + 20
50  libdispatch.dylib             	       0x1b3ce0a08 _dispatch_block_invoke_direct + 264
51  FrontBoardServices            	       0x1c244ad40 __FBSSERIALQUEUE_IS_CALLING_OUT_TO_A_BLOCK__ + 52
52  FrontBoardServices            	       0x1c244a8dc -[FBSSerialQueue _targetQueue_performNextIfPossible] + 220
53  FrontBoardServices            	       0x1c244d184 -[FBSSerialQueue _performNextFromRunLoopSource] + 28
54  CoreFoundation                	       0x1ac790f24 __CFRUNLOOP_IS_CALLING_OUT_TO_A_SOURCE0_PERFORM_FUNCTION__ + 28
55  CoreFoundation                	       0x1ac79d2fc __CFRunLoopDoSource0 + 176
56  CoreFoundation                	       0x1ac721220 __CFRunLoopDoSources0 + 340
57  CoreFoundation                	       0x1ac736b7c __CFRunLoopRun + 836
58  CoreFoundation                	       0x1ac73beb0 CFRunLoopRunSpecific + 612
59  GraphicsServices              	       0x1e6931368 GSEventRunModal + 164
60  UIKitCore                     	       0x1aec31668 -[UIApplication _run] + 888
61  UIKitCore                     	       0x1aec312cc UIApplicationMain + 340
62  SwiftUI                       	       0x1b0252244 0x1b0084000 + 1892932
63  SwiftUI                       	       0x1b01b3278 0x1b0084000 + 1241720
64  SwiftUI                       	       0x1b019c62c 0x1b0084000 + 1148460
65  iosApp                        	       0x100ece9cc main + 36
66  dyld                          	       0x1cb034960 start + 2528
```

可以看到最终的crash原因是`terminateWithUnhandledException`，exception信息以及thread 0里面，都可以看到具体引发exception的kotlin代码详情：

```
# exception info 部分
Last Exception Backtrace:
0   iosApp                        	       0x100edacb4 ThrowArrayIndexOutOfBoundsException + 156
1   iosApp                        	       0x100eec0fc kfun:com.luoyibu.kmmdemo.IOSPlatform#<init>(){} + 0
2   iosApp                        	       0x100eefa20 objc2kotlin_kfun:com.luoyibu.kmmdemo.Greeting#greet(){}kotlin.String + 212
3   iosApp                        	       0x100ece870 closure #1 in iOSApp.body.getter + 60
```

```
# thread 0 部分
10  iosApp                        	       0x100eefaa4 objc2kotlin_kfun:com.luoyibu.kmmdemo.Greeting#greet(){}kotlin.String + 344
11  iosApp                        	       0x100ece870 closure #1 in iOSApp.body.getter + 60
```

可以看到2部分的信息，都指向了`com.luoyibu.kmmdemo.Greeting#greet()`这个函数，不过exception info的栈顶会多一个`kfun:com.luoyibu.kmmdemo.IOSPlatform#<init>(){}`比较奇怪，我们分别通过iosApp的符号表（如果是编译成静态库就用主app的，动态库就用单独的framework的符号表，推荐用静态库）来翻译一下

**翻译后**

```
# exception info 部分
Last Exception Backtrace:
0   iosApp                        	       ThrowArrayIndexOutOfBoundsException (in iosApp) (RuntimeUtils.kt:25)
1   iosApp                        	       kfun:com.luoyibu.kmmdemo.IOSPlatform#<init>(){} (in iosApp) (Platform.kt:0)
2   iosApp                        	       objc2kotlin_kfun:com.luoyibu.kmmdemo.Greeting#greet(){}kotlin.String (in iosApp) (/<compiler-generated>:1)
3   iosApp                        	       closure #1 in iOSApp.body.getter (in iosApp) (iOSApp.swift:7)
```

```
# thread 0 部分
10  iosApp                        	       objc2kotlin_kfun:com.luoyibu.kmmdemo.Greeting#greet(){}kotlin.String (in iosApp) (/<compiler-generated>:1)
11  iosApp                        	       closure #1 in iOSApp.body.getter (in iosApp) (iOSApp.swift:7)
```

问题：

	1. exception这里，栈顶是`kfun:com.luoyibu.kmmdemo.IOSPlatform#<init>(){}`，并不是引发crash的实际代码
	1. `objc2kotlin_kfun:com.luoyibu.kmmdemo.Greeting#greet(){}`  这块问题代码的对应堆栈，没有对应到具体的代码行数



**解决方案：**

怀疑exception这里的栈顶 `0x100eec0fc kfun:com.luoyibu.kmmdemo.IOSPlatform#<init>(){} + 0`实际是crash的下一行指令的地址，所以拿这个地址减去4来尝试翻译：

```
➜  sym xcrun atos -o iosApp.app.dSYM/Contents/Resources/DWARF/iosApp -arch arm64 -l 0x100ec8000 0x100eec0f9
kfun:com.luoyibu.kmmdemo.Greeting#greet(){}kotlin.String (in iosApp) (Greeting.kt:8)
```

可以看到，减去4后，翻译出来的是实际crash的代码，并有代码行数: `Greeting.kt:8`

