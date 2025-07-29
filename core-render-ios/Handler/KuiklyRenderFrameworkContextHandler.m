/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "KuiklyRenderFrameworkContextHandler.h"
#import "KRMultiDelegateProxy.h"
#import "KuiklyRenderThreadManager.h"
#import "KRConvertUtil.h"
#import <objc/runtime.h>
#import "KRLogModule.h"
#define MAX_FRAMEWORK_NAME_LENGTH 100


#define KRSafeObject(object) object?:@""
#define KRSafeArrayIndex(array, index)  ((index < array.count)?array[index] : nil)

@protocol KRKuiklyKotlinCoreEntryDelegate

@required
- (id _Nullable)callNativeMethodId:(int32_t)methodId arg0:(id _Nullable)arg0 arg1:(id _Nullable)arg1 arg2:(id _Nullable)arg2 arg3:(id _Nullable)arg3 arg4:(id _Nullable)arg4 arg5:(id _Nullable)arg5;

@end

@protocol KuiklyCoreEntryCompanionProtocol <NSObject>

- (BOOL)isPageExistPageName:(NSString *)pageNam;

@end

@protocol KuiklyKotlinCoreEntryProtocol <NSObject>

@property id<KRKuiklyKotlinCoreEntryDelegate> hrCoreDelegate;
@property (class, readonly, getter=companion) id<KuiklyCoreEntryCompanionProtocol> companion;
- (void)callKotlinMethodMethodId:(int32_t)methodId arg0:(id _Nullable)arg0 arg1:(id _Nullable)arg1 arg2:(id _Nullable)arg2 arg3:(id _Nullable)arg3 arg4:(id _Nullable)arg4 arg5:(id _Nullable)arg5;

@end

@interface KuiklyRenderFrameworkContextHandler()<KRKuiklyKotlinCoreEntryDelegate>

@property (nonatomic, strong) KuiklyRenderNativeMethodCallback nativeCallback;
@property (nonatomic, strong) id<KuiklyKotlinCoreEntryProtocol> coreEntryInstance;
@property (nonatomic, strong) KuiklyContextParam *contextParam;

@end

@implementation KuiklyRenderFrameworkContextHandler {
 
}

@synthesize isDestroying;

#pragma mark - KuiklyRenderContextProtocol

+ (void)initialize {
 
    

//    class_addProtocol([KuiklyRenderFrameworkContextHandler class], NSProtocolFromString(@"SharedKuiklyKotlinCoreEntryDelegate"));
}


/*
 * @brief 初始化Kotlin侧代码执行环境
 * @param contextCode 环境代码（framework名）
 * @return 返回context实例
 */
- (instancetype)initWithContext:(NSString *)contextCode contextParam:(KuiklyContextParam *)contextParam {
    if (self = [super init]) {
        _coreEntryInstance = [[[[self class] entryClassWithFrameworkName:contextCode] alloc] init];
        NSAssert([_coreEntryInstance respondsToSelector:@selector(callKotlinMethodMethodId:arg0:arg1:arg2:arg3:arg4:arg5:)], @"entry未实现该方法，请check下entry文件");
        _coreEntryInstance.hrCoreDelegate = (id<KRKuiklyKotlinCoreEntryDelegate>)self;
        _contextParam = contextParam;
    }
    return self;
}
/*
 * @brief Native侧调用Kotlin侧方法接口
 * @param method Kotlin侧的方法
 * @param args 方法参数
 */
- (void)callWithMethod:(KuiklyRenderContextMethod)method args:(NSArray *)args {
    KR_ASSERT_CONTEXT_HTREAD;
    if (self.isDestroying &&
        (method == KuiklyRenderContextMethodFireCallback || method == KuiklyRenderContextMethodLayoutView)) {
        return ;
    }
    NSMutableArray * arguments = [[NSMutableArray alloc] initWithCapacity:6];
    for (id arg in args) {
        id ele = [KRConvertUtil nativeObjectToKotlinObject:arg];
        if (ele) {
            [arguments addObject:ele];
        }
    }
    [_coreEntryInstance callKotlinMethodMethodId:(int32_t)method
                                            arg0:KRSafeArrayIndex(arguments, 0)
                                            arg1:KRSafeArrayIndex(arguments, 1)
                                            arg2:KRSafeArrayIndex(arguments, 2)
                                            arg3:KRSafeArrayIndex(arguments, 3)
                                            arg4:KRSafeArrayIndex(arguments, 4)
                                            arg5:KRSafeArrayIndex(arguments, 5)];
}
/*
 * @brief Kotlin侧用Native侧方法时接口回调
 * @param callback kotlin侧调用native侧方法时回调闭包
 */
- (void)registerCallNativeWtihCallback:(KuiklyRenderNativeMethodCallback)callback {
    _nativeCallback = callback;
}

#pragma mark - KRCallNativeDelegate

- (id _Nullable)callNativeMethodId:(int32_t)methodId arg0:(id _Nullable)arg0 arg1:(id _Nullable)arg1 arg2:(id _Nullable)arg2 arg3:(id _Nullable)arg3 arg4:(id _Nullable)arg4 arg5:(id _Nullable)arg5 {
    if (_nativeCallback) {
        id result = _nativeCallback(methodId, @[KRSafeObject(arg1),
                                           KRSafeObject(arg2),
                                           KRSafeObject(arg3),
                                           KRSafeObject(arg4),
                                           KRSafeObject(arg5)]);
        return [KRConvertUtil nativeObjectToKotlinObject:result];
    }
    return nil;
}
/*
 * @brief 做销毁前的清理工作
 */
- (void)willDestroy {
    _coreEntryInstance.hrCoreDelegate = nil;
    _coreEntryInstance = nil;
    _nativeCallback = nil;
}

- (void)dealloc {
    
}

#pragma mark - public
/*
 * @brief 对应contextCode是否framework模式
 */
+ (BOOL)isFrameworkWithContextCode:(NSString *)contextCode {
    if (contextCode.length && contextCode.length < MAX_FRAMEWORK_NAME_LENGTH) {
        if (![self entryClassWithFrameworkName:contextCode]) {
            [KRLogModule logError:[NSString stringWithFormat:@"framework:%@ 名字错误，找不到对应KuiklyKotlinCoreEntry", contextCode ?: @""]];
        }
        return YES;
    }
    return NO;
}
/*
 * @brief 判断PageName是否存在于当前Framework中
 * @param pageName 对应Kotin测@Page注解名字
 * @param frameworkName 编译出来的framework名字
 * @return 是否存在该页面
 */
+ (BOOL)isPageExistWithPageName:(NSString *)pageName frameworkName:(NSString *)frameworkName {
    id<KuiklyKotlinCoreEntryProtocol> coreEntryClass = (id<KuiklyKotlinCoreEntryProtocol>)[self entryClassWithFrameworkName:frameworkName];
    NSAssert(coreEntryClass, @"framework名字错误，找不到对应KuiklyKotlinCoreEntry");
    if (!coreEntryClass) {
        [KRLogModule logError:[NSString stringWithFormat:@"framework:%@ 名字错误，找不到对应KuiklyKotlinCoreEntry", frameworkName ?: @""]];
    }
    if ([coreEntryClass respondsToSelector:@selector(companion)]) {
        id <KuiklyCoreEntryCompanionProtocol> companion = [coreEntryClass performSelector:@selector(companion)];
        if ([companion respondsToSelector:@selector(isPageExistPageName:)]) {
            return  [companion isPageExistPageName:pageName];
        }
    }
#if DEBUG
    assert(NO); //
#endif
    [KRLogModule logError:@"companion类属性不存在或者isPageExistPageName不存在, 请升级最新Core版本或反馈联系SDK"];
    return NO;
}


+ (Class _Nullable)entryClassWithFrameworkName:(NSString *)frameworkName {
    NSString *firstChar = [[frameworkName uppercaseString] substringWithRange:NSMakeRange(0, 1)];
    // 首字母大写后的framework名
    frameworkName = [NSString stringWithFormat:@"%@%@", firstChar, [frameworkName substringFromIndex:1]];
    NSString *entryClassName = [NSString stringWithFormat:@"%@KuiklyCoreEntry", frameworkName];
    if (!NSClassFromString(entryClassName)) {
        entryClassName = [NSString stringWithFormat:@"%@KuiklyCoreEntry",  [self removeLowerCaseCharacters:frameworkName]];
    }
    // 动态遵循协议，以便运行时设置其代理
    NSString *entryDelegateName = [NSString stringWithFormat:@"%@Delegate", entryClassName];
    Protocol *entryDelegateProtocol = NSProtocolFromString(entryDelegateName);
    if (!entryDelegateProtocol) {
        entryDelegateProtocol = objc_allocateProtocol(entryDelegateName.UTF8String);
        objc_registerProtocol(entryDelegateProtocol);
    }
    if (entryDelegateProtocol && !class_conformsToProtocol(self, entryDelegateProtocol)) {
        class_addProtocol(self, entryDelegateProtocol);
    }
    return NSClassFromString(entryClassName);
}

+ (NSString *)removeLowerCaseCharacters:(NSString *)input {
    NSMutableString *result = [[NSMutableString alloc] init];
    NSUInteger length = input.length;

    for (NSUInteger i = 0; i < length; i++) {
        unichar character = [input characterAtIndex:i];
        if (![[NSCharacterSet lowercaseLetterCharacterSet] characterIsMember:character]) {
            [result appendFormat:@"%C", character];
        }
    }
    return result;
}


@end
