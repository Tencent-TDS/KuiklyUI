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

#import "KRConvertUtil.h"
#import "KRLogModule.h"
#import "KuiklyContextParam.h"
#import "KuiklyRenderCore.h"
#import "KuiklyRenderFrameworkContextHandler.h"
#import "KuiklyRenderLayerHandler.h"
#import "KuiklyRenderThreadManager.h"
#import "KuiklyRenderUIScheduler.h"
#import "NSObject+KR.h"
#import "KuiklyTurboDisplayRenderLayerHandler.h"

// 注：args固定参数个数，不会存在数组访问越界
#define FISRT_ARG args[0]
#define SECOND_ARG args[1]
#define THIRD_ARG args[2]
#define FOUR_ARG args[3]
#define FIVE_ARG args[4]

/** 全局递增的Core实例ID */
static NSInteger gInstanceId = 0;

NSString *const kKuiklyFatalExceptionNotification = @"KuiklyFatalExceptionNotification";

@interface KuiklyRenderCore () <KuiklyRenderUISchedulerDelegate>
/** 渲染层协议的实现者 */
@property (nonatomic, strong) id<KuiklyRenderLayerProtocol> renderLayerHandler;
/** KuiklyKotlin侧协议的实现者 */
@property (nonatomic, strong) id<KuiklyRenderContextProtocol> contextHandler;
/** Core唯一实例ID */
@property (nonatomic, copy) NSString *instanceId;
/** 给KuiklyKotlin侧调用的Native事件注册索引 */
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, KuiklyRenderNativeMethodCallback> *nativeMethodRegistry;
/** UI线程调度器*/
@property (nonatomic, strong) KuiklyRenderUIScheduler *uiScheduler;
/** 上下文环境参数*/
@property (nonatomic, strong) KuiklyContextParam *contextParam;
/** KuiklyRenderCore代理 */
@property (nonatomic, weak) id<KuiklyRenderCoreDelegate> delegate;

@end

@implementation KuiklyRenderCore 

#pragma mark - init

- (instancetype)initWithRootView:(UIView *)rootView
                     contextCode:(NSString *)contextCode
                    contextParam:(KuiklyContextParam *)contextParam
                          params:(NSDictionary *)params
                        delegate:(id<KuiklyRenderCoreDelegate>)delegate {
    if (self = [super init]) {
        _instanceId = [NSString stringWithFormat:@"%ld", (long)++gInstanceId];
        _delegate = delegate;
        _contextParam = contextParam;
        _uiScheduler = [[KuiklyRenderUIScheduler alloc] initWithDelegate:self];
        _renderLayerHandler = [self p_createRenderLayerWithRootView:rootView];
        // 初始化注册Native事件给KuiklyKotlin侧调用
        [self p_initNativeMethodRegisters];
        [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
            [self p_initContextHandlerWithContextCode:contextCode
                                             pageName:contextParam.pageName
                                               params:params];
        }];
    }
    return self;
}

#pragma mark - public
/*
 * @brief 完全初始化Core之后调用
 */
- (void)didInitCore {
    if ([_renderLayerHandler respondsToSelector:@selector(didInit)]) {
        [_renderLayerHandler didInit];
    }
}
/**
 * @brief 通过KuiklyRenderCore发送事件到KuiklyKotlin侧（支持多线程调用, 非主线程时为同步通信）.
 * @param event 事件名
 * @param data 事件对应的参数
 */
- (void)sendWithEvent:(NSString *)event data:(NSDictionary *)data {
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
          [self.contextHandler callWithMethod:KuiklyRenderContextMethodUpdateInstance
                                         args:@[self.instanceId, event, (data ?: @{})]];
    } sync:![NSThread isMainThread]];
}
/**
 * @brief 获取模块对应的实例（仅支持在主线程调用）.
 * @param moduleName 模块名
 * @return module实例
 */
- (id<TDFModuleProtocol>)moduleWithName:(NSString *)moduleName {
    NSAssert([NSThread isMainThread], @"should run on main thread");
    return [self.renderLayerHandler moduleWithName:moduleName];
}

/**
 * @brief 获取tag对应的View实例（仅支持在主线程调用）.
 * @param tag view对应的索引
 * @return view实例
 */
- (id<KuiklyRenderViewExportProtocol> _Nullable)viewWithTag:(NSNumber *)tag {
    NSAssert([NSThread isMainThread], @"should run on main thread");
    return [self.renderLayerHandler viewWithTag:tag];
}
/**
 * @brief Core销毁前调用，用于Core提前发送事件到KuiklyKotlin侧销毁内在资源.
 */
- (void)willDealloc {
    id<KuiklyRenderContextProtocol> contextHandler = self.contextHandler;
    if ([contextHandler respondsToSelector:@selector(setIsDestroying:)]) {
        // 销毁中，提供让context队列任务中断判断依据，避免列队执行无意义任务耗时影响其他页面任务
        contextHandler.isDestroying = YES;
    }
    NSString *instanceId = [_instanceId copy];
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        [contextHandler callWithMethod:(KuiklyRenderContextMethodDestroyInstance) args:@[instanceId]];
    }];
}
/**
 * @brief 响应kotlin侧闭包
 * @param callbackID GlobalFunctions.createFunction返回的callback id
 * @param data 调用闭包传参
 */
- (void)fireCallbackWithID:(NSString *)callbackID data:(NSDictionary *)data {
    id<KuiklyRenderContextProtocol> contextHandler = self.contextHandler;
    NSString* instanceId = [_instanceId copy];
    [KuiklyRenderThreadManager performOnContextQueueImmediatelyWithBlock:^{
      [contextHandler callWithMethod:(KuiklyRenderContextMethodFireCallback) args:@[ instanceId, callbackID ?: @"", data ?: @{} ]];
    }];
}
/**
 * @brief 同步布局和渲染（在当前线程渲染执行队列中所有任务以实现同步渲染）
 */
- (void)syncFlushAllRenderTasks {
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        [self.uiScheduler performSyncMainQueueTasksBlockIfNeed];
    } sync:YES];
}
/**
 * @brief 执行任务当首屏完成后(优化首屏性能)（仅支持在主线程调用）
 */
- (void)performWhenViewDidLoadWithTask:(dispatch_block_t)task {
    NSAssert([NSThread isMainThread], @"请在主线程调用该接口");
    [self.uiScheduler performWhenViewDidLoadWithTask:task];
}

/**
 * @brief 收到手势响应时调用
 */
- (void)didHitTest {
    if ([_renderLayerHandler respondsToSelector:@selector(didHitTest)]) {
        [_renderLayerHandler didHitTest];
    }
}
#pragma mark - KuiklyRenderUISchedulerDelegate

- (void)willPerformUITasksWithScheduler:(KuiklyRenderUIScheduler *)scheduler {
    // 同步主线程任务前，需要告诉kotlin侧 去 layoutIfNeed, 避免viewFrame设置时机和创建view时机不同步
    [self.contextHandler callWithMethod:KuiklyRenderContextMethodLayoutView args:@[ self.instanceId ]];
}

#pragma mark - private

// 初始化与kotlin侧交互的实现者
- (void)p_initContextHandlerWithContextCode:(NSString *)contextCode
                                   pageName:(NSString *)pageName
                                     params:(NSDictionary * _Nullable)params {
    KR_ASSERT_CONTEXT_HTREAD;
    _contextHandler = [_contextParam.contextMode createContextHandlerWithContextCode:contextCode
                                                                        contextParam:_contextParam];
    if ([_contextHandler respondsToSelector:@selector(setOnExceptionBlock:)]) {
        [_contextHandler setOnExceptionBlock:_onExceptionBlock];
    }
    // KuiklyKotlin侧call native event 时回调该闭包
    KR_WEAK_SELF
    [_contextHandler registerCallNativeWtihCallback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
        KR_STRONG_SELF_RETURN_NIL
        [KuiklyRenderThreadManager assertContextQueue]; // 线程断言，保证仅在Context线程回调
        // 执行KuiklyKotlin侧调用Native侧的事件
        return [strongSelf p_performNativeMethodWithMethod:method args:args];
    }];
    // Native侧调用Kotlin侧事件：CreateInstance, 让Kotlin侧开始创建页面实例
    [_contextHandler callWithMethod:(KuiklyRenderContextMethodCreateInstance) args:@[_instanceId, pageName, (params ?: @{})]];
}

/**
 *  @brief 注册Native事件给Kotlin侧调用.
 *  @param method 具体的Native事件类型
 *  @param callback Kotlin侧调用该Native事件时回调该闭包
 */
- (void)p_registerNativeMethodWithMethod:(KuiklyRenderNativeMethod)method callback:(KuiklyRenderNativeMethodCallback)callback {
    if (!_nativeMethodRegistry) {
        _nativeMethodRegistry = [[NSMutableDictionary alloc] init];
    }
    _nativeMethodRegistry[@(method)] = callback;
}

// 判断事件是否需要同步调用
- (BOOL)p_shouldSyncCallWithWithMethod:(KuiklyRenderNativeMethod)method args:(NSArray *)args {
    if (method == KuiklyRenderNativeMethodCallModuleMethod) {
        return [FIVE_ARG isKindOfClass:[NSNumber class]] ? [FIVE_ARG boolValue] : NO;  //
    }
    return method == KuiklyRenderNativeMethodCalculateRenderViewSize ||
           method == KuiklyRenderNativeMethodCreateShadow ||
           method == KuiklyRenderNativeMethodRemoveShadow ||
           method == KuiklyRenderNativeMethodSetShadowForView ||
           method == KuiklyRenderNativeMethodSetShadowProp ||
           method == KuiklyRenderNativeMethodSetTimeout ||
           method == KuiklyRenderNativeMethodCallShadowMethod ||
           method == KuiklyRenderNativeMethodFireFatalException ||
           method == KuiklyRenderNativeMethodSyncFlushUI ||
           method == KuiklyRenderNativeMethodCallTDFModuleMethod;
}

// 执行KuiklyKotlin侧调用Native侧的事件
- (id)p_performNativeMethodWithMethod:(KuiklyRenderNativeMethod)method args:(NSArray *)args {
    // _nativeMethodRegistry读安全（因nativeMethodRegistry初始化后再无写入）
    KuiklyRenderNativeMethodCallback methodCallback = _nativeMethodRegistry[@(method)];
    if (methodCallback) {
        [KuiklyRenderThreadManager assertContextQueue];
        // 如果是moduleMethod的话，直接回调，在内部切线程
        if ([self p_shouldSyncCallWithWithMethod:method args:args]) {
            return methodCallback(method, args);
        } else {
            [self.uiScheduler addTaskToMainQueueWithTask:^{  // 异步批量处理
              methodCallback(method, args);
            }];
        }
    }
    return nil;
}

// 初始化注册给Kotlin侧调用的Native事件接口
- (void)p_initNativeMethodRegisters {
    // 注册view相关事件
    [self p_initViewMethodRegisters];
    // 注册module相关事件
    [self p_initModuleMethodRegisters];
    // 注册shadow相关事件
    [self p_initShadowMethodRegisters];
}

// 注册view相关事件
- (void)p_initViewMethodRegisters {
    KR_WEAK_SELF
    NSString *instanceId = self.instanceId;
    // 注册创建RenderView事件
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodCreateRenderView
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      // 注：args固定参数个数，不会存在数组访问越界
                                      [weakSelf.renderLayerHandler createRenderViewWithTag:FISRT_ARG viewName:SECOND_ARG];
                                      return nil;
                                  }];
    // 注册删除RenderView事件
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodRemoveRenderView
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      [weakSelf.renderLayerHandler removeRenderViewWithTag:FISRT_ARG];
                                      return nil;
                                  }];
    // 注册插入子RenderView事件
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodInsertSubRenderView
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      [weakSelf.renderLayerHandler insertSubRenderViewWithParentTag:FISRT_ARG
                                                                                           childTag:SECOND_ARG
                                                                                            atIndex:[THIRD_ARG intValue]];
                                      return nil;
                                  }];
    // 注册RenderView属性设置事件
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodSetViewProp
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      BOOL isEvent = [FOUR_ARG isKindOfClass:[NSNumber class]] ? [FOUR_ARG boolValue] : NO;
                                      id propValue = THIRD_ARG;
                                      if (isEvent) {
                                          NSString *tag = FISRT_ARG;
                                          BOOL sync = [FIVE_ARG isKindOfClass:[NSNumber class]] ? [FIVE_ARG boolValue] : NO;  // sync fire
                                          propValue = ^(id _Nullable result) {
                                              
                                              KR_STRONG_SELF_RETURN_IF_NIL
                                              BOOL shouldSync = sync;
                                              if (!sync && [result isKindOfClass:[NSDictionary class]] && result[KR_SYNC_CALLBACK_KEY]) {
                                                  shouldSync = [result[KR_SYNC_CALLBACK_KEY] boolValue];
                                               }
                                              // 正在主线程执行任务产生的同步事件->异步
                                              if (shouldSync && strongSelf.uiScheduler.performingMainQueueTask) {
                                                  shouldSync = NO;
                                              }
                                              [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
                                                  [weakSelf.contextHandler callWithMethod:KuiklyRenderContextMethodFireViewEvent
                                                                                     args:@[instanceId, tag, SECOND_ARG, result ?: @{}]];
                                                  if (shouldSync) {
                                                      [weakSelf.uiScheduler performSyncMainQueueTasksBlockIfNeed];
                                                  }
                                               } sync:shouldSync];
                                        };
                                      }
                                      [weakSelf.renderLayerHandler setPropWithTag:FISRT_ARG propKey:SECOND_ARG propValue:propValue];
                                      return nil;
                                  }];
    // 注册设置RenderView位置事件
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodSetRenderViewFrame
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      CGRect frame = [KRConvertUtil toSafeRect:CGRectMake([SECOND_ARG doubleValue],
                                                                                          [THIRD_ARG doubleValue],
                                                                                          [FOUR_ARG doubleValue],
                                                                                          [FIVE_ARG doubleValue])];
                                      [weakSelf.renderLayerHandler setRenderViewFrameWithTag:FISRT_ARG frame:frame];
                                      return nil;
                                  }];
    // 注册测量RenderView尺寸事件
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodCalculateRenderViewSize
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      CGSize size = [weakSelf.renderLayerHandler
                                                     calculateRenderViewSizeWithTag:FISRT_ARG
                                                                     constraintSize:CGSizeMake([SECOND_ARG doubleValue], [THIRD_ARG doubleValue])];
                                       return [KRConvertUtil sizeStrWithSize:size];
                                  }];
    // 注册调用RenderView方法事件
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodCallViewMethod
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      NSString *callbackId = FOUR_ARG;
                                      [weakSelf.renderLayerHandler
                                          callViewMethodWithTag:FISRT_ARG
                                                         method:SECOND_ARG
                                                         params:THIRD_ARG
                                                       callback:^(id _Nullable result) {
                                                         if ([callbackId isKindOfClass:[NSString class]] && callbackId.length) {
                                                             [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
                                                                 [weakSelf.contextHandler      callWithMethod:KuiklyRenderContextMethodFireCallback
                                                                               args:@[instanceId, callbackId, result ?: @{}]];
                                                             }];
                                                         }
                                                       }];
                                       return nil;
                                  }];
}

- (void)performCallback:(NSString *)callbackId instanceId:(NSString *)instanceId result:(id _Nullable)result {
    KR_WEAK_SELF
    if ([callbackId isKindOfClass:[NSString class]] && callbackId.length) {
        [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
            [weakSelf.contextHandler callWithMethod:KuiklyRenderContextMethodFireCallback args:@[instanceId, callbackId, result ?: @{}]];
        }];
    }
}

// 注册module相关事件
- (void)p_initModuleMethodRegisters {
    KR_WEAK_SELF
    NSString *instanceId = self.instanceId;
    // 注册kotin调用Module方法回调
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodCallModuleMethod
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                    NSString *callbackId = FOUR_ARG;
                                    return [weakSelf.renderLayerHandler callModuleMethodWithModuleName:FISRT_ARG
                                                                                                method:SECOND_ARG
                                                                                                params:THIRD_ARG
                                                                                              callback:^(id _Nullable result) {
                                               [weakSelf performCallback:callbackId instanceId:instanceId result:result];
                                           }];
    }];
    // 注册kotin调用TDFModule方法回调
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodCallTDFModuleMethod
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                    NSString *moduleName = FISRT_ARG;
                                    BOOL sync = [FIVE_ARG boolValue];
                                    __block NSString *result = nil;
                                    dispatch_block_t task = ^{
                                        NSDictionary *cbDic = [FOUR_ARG hr_stringToDictionary];
                                        result = [weakSelf.renderLayerHandler callTDFModuleMethodWithModuleName:moduleName
                                                                                                         method:SECOND_ARG
                                                                                                         params:THIRD_ARG
                                                                                                 succCallbackId:cbDic[@"succ"]
                                                                                                errorCallbackId:cbDic[@"error"]];
                                    };
                                    if (sync) {
                                        task();
                                    } else {
                                        [KuiklyRenderThreadManager performOnModuleQueueWithTDFModuleName:moduleName task:task];
                                    };
                                    return result;
                                }];
}

// 注册shadow相关事件
- (void)p_initShadowMethodRegisters {
    KR_WEAK_SELF
    NSString *instanceId = self.instanceId;
    // 注册创建Sahdow回调
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodCreateShadow
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      [weakSelf.renderLayerHandler createShadowWithTag:FISRT_ARG viewName:SECOND_ARG];
                                      return nil;
                                  }];
    // 注册删除Sahdow回调
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodRemoveShadow
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      [weakSelf.renderLayerHandler removeShadowWithTag:FISRT_ARG];
                                      return nil;
                                  }];
    // 注册设置Sahdow属性
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodSetShadowProp
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      [weakSelf.renderLayerHandler setShadowPropWithTag:FISRT_ARG propKey:SECOND_ARG propValue:THIRD_ARG];
                                      return nil;
                                  }];

    // 设置shadow for view
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodSetShadowForView
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      id<KuiklyRenderShadowProtocol> shadow = [weakSelf.renderLayerHandler shadowWithTag:FISRT_ARG];
                                      dispatch_block_t task = nil;
                                       if ([shadow respondsToSelector:@selector(hrv_taskToMainQueueWhenWillSetShadowToView)]) {
                                           task = [shadow hrv_taskToMainQueueWhenWillSetShadowToView];
                                       }
                                       [weakSelf.uiScheduler addTaskToMainQueueWithTask:^{
                                           if (task) {
                                               task();
                                           }
                                           [weakSelf.renderLayerHandler setShadowWithTag:FISRT_ARG shadow:shadow];
                                       }];
                                       return nil;
                                  }];

    // 设置延时能力回调
    [self p_registerNativeMethodWithMethod:(KuiklyRenderNativeMethodSetTimeout)
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                       [KuiklyRenderThreadManager performOnContextQueueWithTask:^{
                                           [weakSelf.contextHandler callWithMethod:KuiklyRenderContextMethodFireCallback
                                                                              args:@[instanceId, SECOND_ARG]];
                                       } delay:[FISRT_ARG doubleValue] / 1000.0f];
                                       return nil;
                                  }];
    // 注册调用Shadow方法接口
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodCallShadowMethod
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      return [weakSelf.renderLayerHandler callShadowMethodWithTag:FISRT_ARG
                                                                                           method:SECOND_ARG
                                                                                           params:THIRD_ARG];
                                  }];
    // 注册同步上屏UI接口
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodSyncFlushUI
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      [weakSelf.uiScheduler performSyncMainQueueTasksBlockIfNeed];
                                      return nil;
                                  }];
    // 注册错误异常接口回调
    [self p_registerNativeMethodWithMethod:KuiklyRenderNativeMethodFireFatalException
                                  callback:^id _Nullable(KuiklyRenderNativeMethod method, NSArray *_Nonnull args) {
                                      NSString *exception = args.count > 1 ? args[0] : @"";
                                      if (exception.length > 0) {
                                          
                                          [KRLogModule logError:[NSString stringWithFormat:@"[Kuikly Exception], %@", exception]];
                                          [[NSNotificationCenter defaultCenter] postNotificationName:kKuiklyFatalExceptionNotification
                                                                                              object:nil
                                                                                            userInfo:@{@"exception" : exception,
                                                                                                          @"pageName" : weakSelf.contextParam.pageName ?: @""
                                                                       }];
                                          NSArray *components = [exception componentsSeparatedByString:@"\n"];
                                          NSString *exceptionName = [components firstObject];
                                          NSString *stackStr = [exception substringFromIndex:exceptionName.length + @"\n".length];
                                          if (weakSelf.onExceptionBlock) {
                                              weakSelf.onExceptionBlock(exceptionName, stackStr, weakSelf.contextParam.contextMode.modeId);
                                          }
                                       }
                                       return nil;
                                  }];
}

- (id<KuiklyRenderLayerProtocol>)p_createRenderLayerWithRootView:(UIView *)rootView {
    NSString * turboDisplayKey = nil; // 是否为TurboDisplay AOT渲染模式
    if ([_delegate respondsToSelector:@selector(turboDisplayKey)]) {
        turboDisplayKey = [_delegate turboDisplayKey];
    }
    if (turboDisplayKey.length) {
        KuiklyTurboDisplayRenderLayerHandler *handler = [[KuiklyTurboDisplayRenderLayerHandler alloc] 
                                                         initWithRootView:rootView
                                                         contextParam:_contextParam
                                                         turboDisplayKey:turboDisplayKey];
        handler.uiScheduler = _uiScheduler;
        return handler;
    }
    return [[KuiklyRenderLayerHandler alloc] initWithRootView:rootView contextParam:_contextParam];
}


#pragma mark - dealloc

- (void)dealloc {
    id<KuiklyRenderLayerProtocol> renderLayerHandler = _renderLayerHandler;
    [KuiklyRenderThreadManager performOnMainQueueWithTask:^{
        if ([renderLayerHandler respondsToSelector:@selector(willDealloc)]) {
            [renderLayerHandler willDealloc];
        }
    } sync:NO];
    id contextHandler = _contextHandler;
    
    // 指定线程和时序释放
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        [contextHandler willDestroy];
        [KuiklyRenderThreadManager performOnMainQueueWithTask:^{
            [renderLayerHandler description];
        } delay:0.5];
    }];
}

@end
