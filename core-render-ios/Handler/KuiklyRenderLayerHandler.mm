/*
* Tencent is pleased to support the open source community by making KuiklyUI
* available.
* Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#import "TDFBaseModule.h"
#import "TDFConvert.h"
#import "TDFModuleProtocol.h"
#import "TDFNativeMethod.h"
#import <pthread.h>
#import "KRConvertUtil.h"
#import "KRLogModule.h"
#import "KuiklyBridgeDelegator.h"
#import "KuiklyRenderLayerHandler.h"
#import "KuiklyRenderModuleExportProtocol.h"
/*
 *  渲染层协议的实现者(渲染器)
 */
@implementation KuiklyRenderLayerHandler {
    /** 渲染层的rootView */
    __weak UIView* _rootView;
    /** 上下文环境参数 */
    KuiklyContextParam *_contextParam;
    /** renderView的索引Map */
    NSMutableDictionary<NSNumber *, id<KuiklyRenderViewExportProtocol>> *_renderViewRegistry;
    /** shadow的索引Map */
    NSMutableDictionary<NSNumber *, id<KuiklyRenderShadowProtocol>> *_shadowRegistry;
    /** module的索引Map */
    NSMutableDictionary<NSString *, id<TDFModuleProtocol>>* _moduleRegistry;
    /** renderView的复用队列 */
    NSMutableDictionary<NSString *, NSMutableArray<id<KuiklyRenderViewExportProtocol>> *> *_renderViewReuseQueue;
    /** 用于访问module的读写锁 */
    pthread_rwlock_t _moduleRWLock;
}

#pragma mark - KuiklyRenderLayerProtocol

Class _Nullable KRClassFromString(NSString *aClassName) {
    return NSClassFromString(aClassName)
           ?: NSClassFromString([aClassName stringByReplacingCharactersInRange:NSMakeRange(0, 1) withString:@"K"]);
}

- (instancetype)initWithRootView:(UIView *)rootView contextParam:(KuiklyContextParam *)contextParam {
    if (self = [super init]) {
        _rootView = rootView;
        _contextParam = contextParam;
        _renderViewRegistry = [[NSMutableDictionary alloc] init];
        _renderViewReuseQueue = [[NSMutableDictionary alloc] init];
        _moduleRegistry = [[NSMutableDictionary alloc] init];
        pthread_rwlock_init(&_moduleRWLock, NULL);
    }
    return self;
}

- (void)didInit {
    // nothing to do
}

- (void)createRenderViewWithTag:(NSNumber *)tag viewName:(NSString *)viewName {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    NSAssert(![self p_renderViewHandlerWithTag:tag], @"tag should be nil");
    [self p_createRenderViewHandlerWithTag:tag viewName:viewName];
}

- (void)insertSubRenderViewWithParentTag:(NSNumber *)parentTag childTag:(NSNumber *)childTag atIndex:(NSInteger)index {
    BOOL isRootViewTag = [parentTag isEqual:KRV_ROOT_VIEW_TAG];
    NSAssert(isRootViewTag || [self p_renderViewHandlerWithTag:parentTag], @"parentTag can't be nil");
    NSAssert([self p_renderViewHandlerWithTag:childTag], @"childTag can't be nil");
    UIView *parentView = (UIView *)[self p_renderViewHandlerWithTag:parentTag];
    UIView *childView = (UIView *)[self p_renderViewHandlerWithTag:childTag];
    if (index > parentView.subviews.count || index == -1) {
        index = parentView.subviews.count;
    }
    UIView *parentRenderView = isRootViewTag ? _rootView : parentView;
    [((id<KuiklyRenderViewExportProtocol>)parentRenderView) hrv_insertSubview:childView atIndex:index];
}

- (void)setPropWithTag:(NSNumber *)tag propKey:(NSString *)propKey propValue:(id)propValue {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    [[self p_renderViewHandlerWithTag:tag] hrv_setPropWithKey:propKey propValue:propValue];
}

- (void)setShadowWithTag:(NSNumber *)tag shadow:(id<KuiklyRenderShadowProtocol>)shadow {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    [[self p_renderViewHandlerWithTag:tag] hrv_setShadow:shadow];
}

- (void)setRenderViewFrameWithTag:(NSNumber *)tag frame:(CGRect)frame {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    id<KuiklyRenderViewExportProtocol> viewHandler = [self p_renderViewHandlerWithTag:tag];
    [viewHandler hrv_setPropWithKey:@"frame" propValue:[NSValue valueWithCGRect:frame]];
}

- (void)removeRenderViewWithTag:(NSNumber *)tag {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    [self p_removeViewWithTag:tag];
}

- (CGSize)calculateRenderViewSizeWithTag:(NSNumber *)tag constraintSize:(CGSize)constraintSize {
    id<KuiklyRenderShadowProtocol> shadow = [self p_shadowHandlerWithTag:tag];
    return [shadow hrv_calculateRenderViewSizeWithConstraintSize:constraintSize];
}

- (void)callViewMethodWithTag:(NSNumber *)tag
                       method:(NSString *)method
                       params:(NSString * _Nullable)params
                     callback:(KuiklyRenderCallback _Nullable)callback {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    id<KuiklyRenderViewExportProtocol> view = [self p_renderViewHandlerWithTag:tag];
    [view hrv_callWithMethod:method params:params callback:callback];
}

- (NSString* _Nullable)callModuleMethodWithModuleName:(NSString *)moduleName
                                               method:(NSString *)method
                                               params:(NSString *_Nullable)params
                                             callback:(KuiklyRenderCallback _Nullable)callback {
    id<KuiklyRenderModuleExportProtocol> moduleHandler =
        (id<KuiklyRenderModuleExportProtocol>)[self p_moduleHandlerWithModuleName:moduleName];
    if ([moduleHandler respondsToSelector:@selector(hrv_callWithMethod:params:callback:)]) {
        return [moduleHandler hrv_callWithMethod:method params:params callback:callback];
    }
    return nil;
}

- (NSString* _Nullable)callTDFModuleMethodWithModuleName:(NSString *)moduleName
                                                  method:(NSString *)method
                                                  params:(NSString *_Nullable)params
                                          succCallbackId:(NSString *)succCallbackId
                                         errorCallbackId:(NSString *)errorCallbackId {
    TDFBaseModule *moduleHandler = (TDFBaseModule *)[self p_moduleHandlerWithModuleName:moduleName];
    TDFNativeMethod *tdfMethod = moduleHandler.methodsByName[method ?: @""];
    NSAssert(tdfMethod, @"没找到%@类的%@方法，请注意是否有通过TDF_EXPORT_METHOD导出", moduleName, method);
    NSMutableArray *arrayParams = [NSMutableArray arrayWithArray:[KRConvertUtil hr_arrayWithJSONString:params]];
    if (succCallbackId.length > 0) {
        [arrayParams addObject:@([succCallbackId intValue])];
    }
    if (errorCallbackId.length > 0) {
        [arrayParams addObject:@([errorCallbackId intValue])];
    }
    id result = [tdfMethod invokeWithModule:moduleHandler arguments:arrayParams];
    return [KRConvertUtil hr_dictionaryToJSON:@{@"result" : result ?: @""}];
}

- (void)createShadowWithTag:(NSNumber *)tag viewName:(NSString *)viewName {
    [self p_createShadowHandlerWithTag:tag viewName:viewName];
}

- (void)removeShadowWithTag:(NSNumber *)tag {
    [self p_removeShadowWithTag:tag];
}

- (void)setShadowPropWithTag:(NSNumber *)tag propKey:(NSString *)propKey propValue:(id)propValue {
    [[self p_shadowHandlerWithTag:tag] hrv_setPropWithKey:propKey propValue:propValue];
}

- (id<KuiklyRenderShadowProtocol>)shadowWithTag:(NSNumber *)tag {
    return [self p_shadowHandlerWithTag:tag];
}

- (NSString*)callShadowMethodWithTag:(NSNumber *)tag method:(NSString *)method params:(NSString *)params {
    return [[self p_shadowHandlerWithTag:tag] hrv_callWithMethod:method params:params];
}

- (id<TDFModuleProtocol>)moduleWithName:(NSString *)moduleName {
    NSAssert([NSThread isMainThread], @"should call on sub thread");
    return [self p_moduleHandlerWithModuleName:moduleName];
}

- (id<KuiklyRenderViewExportProtocol>)viewWithTag:(NSNumber *)tag {
    NSAssert([NSThread isMainThread], @"should call on sub thread");
    return [self p_renderViewHandlerWithTag:tag];
}

- (void)updateViewTagWithCurTag:(NSNumber *)curTag newTag:(NSNumber *)newTag {
   id<KuiklyRenderViewExportProtocol> renderView = [self viewWithTag:curTag];
    if (renderView) {
        [_renderViewRegistry removeObjectForKey:curTag];
        _renderViewRegistry[newTag] = renderView;
    } else {
        NSAssert(renderView, @"update RenderView is nil");
    }
}

- (void)willDealloc {
    // nothing to do
}

#pragma mark - private

- (id<KuiklyRenderViewExportProtocol>)p_renderViewHandlerWithTag:(NSNumber *)tag {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    return _renderViewRegistry[tag];
}

- (id<TDFModuleProtocol>)p_moduleHandlerWithModuleName:(NSString *)moduleName {
    id<TDFModuleProtocol> moduleHandler = [self p_moduleWithName:moduleName];
    if (!moduleHandler) {
        /** 开始写操作 */
        pthread_rwlock_wrlock(&_moduleRWLock);  // 写锁进入时，其他写和读都被阻塞
        if (!_moduleRegistry[moduleName]) {     // 避免重复写
            moduleHandler = [[KRClassFromString(moduleName) alloc] init];
            Class tdfModuleClass = TDGGetModuleClass(moduleName);
            NSAssert(!(tdfModuleClass && moduleHandler && [moduleHandler class] != tdfModuleClass),
                    @"TDF导出的模块名，已经存在同名的Class对象，tdf导出类会被覆盖: %@", moduleName ?: @"");
            // 支持tdf通用Module
            if (!moduleHandler) {
                moduleHandler = [[tdfModuleClass alloc] init];
            }
            KuiklyBridgeDelegator *delegator = [[KuiklyBridgeDelegator alloc] initWithRootView:_rootView];
            if ([moduleHandler respondsToSelector:@selector(setDelegate:)]) {
                [moduleHandler performSelector:@selector(setDelegate:) withObject:delegator];
            }
            if ([moduleHandler respondsToSelector:@selector(setHr_rootView:)]) {
                [moduleHandler performSelector:@selector(setHr_rootView:) withObject:_rootView];
            }
            if ([moduleHandler respondsToSelector:@selector(setHr_contextParam:)]) {
                [moduleHandler performSelector:@selector(setHr_contextParam:) withObject:_contextParam];
            }
            if (moduleHandler) {
               _moduleRegistry[moduleName] = moduleHandler;
            } else {
                NSString *reason = [NSString stringWithFormat:@"创建Module失败 ModuleName:%@ 找不到对应Module实现", moduleName];
                [KRLogModule logError:reason];
                NSAssert(NO, reason);
            }
        }
        pthread_rwlock_unlock(&_moduleRWLock);
        // 校验一次，保证同一个handler
        moduleHandler = [self p_moduleWithName:moduleName];
    }
    return moduleHandler;
}

- (void)p_createShadowHandlerWithTag:(NSNumber*)tag viewName:(NSString*)viewName {
    if (!_shadowRegistry) {
        _shadowRegistry = [[NSMutableDictionary alloc] init];
    }
    NSAssert(!_shadowRegistry[tag], @"shadow did created");
    _shadowRegistry[tag] = [KRClassFromString(viewName) hrv_createShadow];
}

- (void)p_removeShadowWithTag:(NSNumber*)tag {
    [_shadowRegistry removeObjectForKey:tag];
}

- (id<KuiklyRenderShadowProtocol>)p_shadowHandlerWithTag:(NSNumber*)tag {
    id<KuiklyRenderShadowProtocol> shadow = _shadowRegistry[tag];
    NSAssert(shadow, @"shadow can't be nil");
    return shadow;
}

- (id<KuiklyRenderViewExportProtocol>)p_createRenderViewHandlerWithTag:(NSNumber *)tag viewName:(NSString *)viewName {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    id<KuiklyRenderViewExportProtocol> renderViewHandler = _renderViewRegistry[tag];
    if (!renderViewHandler) {
        renderViewHandler = [self p_popRenderViewHandlerFromReuseQueueWithTag:tag viewName:viewName];
    }
    if (!renderViewHandler) {
        renderViewHandler = [[KRClassFromString(viewName) alloc] init];
    }
    if ([renderViewHandler isKindOfClass:[UIView class]] &&
        [renderViewHandler conformsToProtocol:@protocol(KuiklyRenderViewExportProtocol)]) {
        _renderViewRegistry[tag] = renderViewHandler;
        if ([renderViewHandler respondsToSelector:@selector(setHr_rootView:)]) {
            [renderViewHandler performSelector:@selector(setHr_rootView:) withObject:_rootView];
        }
    } else {
        NSAssert(NO, ([NSString stringWithFormat:@"创建View组件失败 ViewName:%@ 找不到对应View实现", viewName]));
    }
    return renderViewHandler;
}

- (id<KuiklyRenderViewExportProtocol>)p_popRenderViewHandlerFromReuseQueueWithTag:(NSNumber *)tag viewName:(NSString *)viewName {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    NSMutableArray* reuseQueue = _renderViewReuseQueue[viewName];
    if (reuseQueue.count) {
        id<KuiklyRenderViewExportProtocol> viewHandler = [reuseQueue lastObject];
        [reuseQueue removeLastObject];
        return viewHandler;
    }
    return nil;
}

- (void)p_pushRenderViewHandlerToReuseQueueWithWithViewHandlder:(id<KuiklyRenderViewExportProtocol>)viewHandler {
    NSAssert([NSThread isMainThread], @"should call on main thread");
    NSAssert(viewHandler, @"viewHandler can't be nil");
    NSString *viewName = NSStringFromClass([viewHandler class]);
    NSMutableArray *reuseQueue = _renderViewReuseQueue[viewName];
    if (!reuseQueue) {
        reuseQueue = [[NSMutableArray alloc] init];
        _renderViewReuseQueue[viewName] = reuseQueue;
    }
    [reuseQueue addObject:viewHandler];
}

- (void)p_removeViewWithTag:(NSNumber *)tag {
    id<KuiklyRenderViewExportProtocol> renderViewHandler = [self p_renderViewHandlerWithTag:tag];
#if DEBUG
    assert(renderViewHandler);  // renderViewHandler不存在
#endif
    [renderViewHandler hrv_removeFromSuperview];
    if ([renderViewHandler respondsToSelector:@selector(hrv_prepareForeReuse)]
        && !(((UIView *)renderViewHandler).kr_reuseDisable)) {
        // 放进复用队列
        [renderViewHandler hrv_prepareForeReuse];
        [self p_pushRenderViewHandlerToReuseQueueWithWithViewHandlder:renderViewHandler];
    }
    [_renderViewRegistry removeObjectForKey:tag];
}

- (id<TDFModuleProtocol>)p_moduleWithName:(NSString *)moduleName {
    id<TDFModuleProtocol> res = nil;
    pthread_rwlock_rdlock(&_moduleRWLock);
    res = _moduleRegistry[moduleName];
    pthread_rwlock_unlock(&_moduleRWLock);
    return res;
}

@end
