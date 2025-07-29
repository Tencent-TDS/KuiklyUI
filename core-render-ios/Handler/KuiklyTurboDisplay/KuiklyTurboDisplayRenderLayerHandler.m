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

#import "KuiklyTurboDisplayRenderLayerHandler.h"
#import "KuiklyRenderLayerHandler.h"
#import "KRTurboDisplayNode.h"
#import "KuiklyRenderUIScheduler.h"
#import "KRTurboDisplayModule.h"
#import "KRTurboDisplayCacheManager.h"
#import "KRTurboDisplayShadow.h"
#import "KRMemoryCacheModule.h"
#import "KRTurboDisplayNodeMethod.h"
#import "KRTurboDisplayDiffPatch.h"
#import "KRLogModule.h"
#import "KRTurboDisplayDiffPatch.h"
#import "KuiklyRenderThreadManager.h"
#import "KRTurboDisplayModule.h"

#define ROOT_VIEW_NAME @"RootView"

@interface KuiklyTurboDisplayRenderLayerHandler()<KuiklyRenderLayerProtocol>
/** 原生渲染器 */
@property (nonatomic, strong) KuiklyRenderLayerHandler *renderLayerHandler;
/** turboDisplay缓存数据 */
@property (nonatomic, strong) KRTurboDisplayCacheData *turboDisplayCacheData;
/** 真视图树 */
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, KRTurboDisplayNode *> *realNodeMap;
/** 真shadow树 */
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, KRTurboDisplayShadow *> *realShadowMap;
/** 真渲染树根节点 */
@property (nonatomic, strong) KRTurboDisplayNode *realRootNode;
/** 处于懒渲染 */
@property (nonatomic, assign) BOOL lazyRendering;
/** 上下文环境参数 */
@property (nonatomic, strong) KuiklyContextParam *contextParam;
/** 缓存Key */
@property (nonatomic, strong) NSString *turboDisplayCacheKey;

/** 下次TurboDisplay首屏 */
@property (nonatomic, strong) KRTurboDisplayNode *nextTurboDisplayRootNode;
/** 标记更新下次TurboDisplay首屏 */
@property (nonatomic, assign) BOOL needUpdateNextTurboDisplayRootNode;

/** needSyncMainQueueOnNextRunLoop  */
@property (nonatomic, assign) BOOL needSyncMainQueueOnNextRunLoop;
/** nextLoopTaskOnMainQueue  */
@property (nonatomic, strong) NSMutableArray<dispatch_block_t> *nextLoopTaskOnMainQueue;

@end

@implementation KuiklyTurboDisplayRenderLayerHandler {
    BOOL _didCloseTurboDisplayRenderingMode;
    NSString *_turboDisplayKey;
    // 关闭自动更新TurboDisplay, 由业务来主动设置首屏更新时机
    BOOL _closeAutoUpdateTurboDisplay;
}

#pragma mark - KuiklyRenderLayerProtocol

- (instancetype)initWithRootView:(UIView *)rootView contextParam:(KuiklyContextParam *)contextParam {
    return [self initWithRootView:rootView contextParam:contextParam turboDisplayKey:@""];
}

- (instancetype)initWithRootView:(UIView *)rootView contextParam:(KuiklyContextParam *)contextParam turboDisplayKey:(nonnull NSString *)turboDisplayKey {
    if (self = [super init]) {
        _turboDisplayKey = turboDisplayKey;
        _contextParam = contextParam;
        _renderLayerHandler = [[KuiklyRenderLayerHandler alloc] initWithRootView:rootView contextParam:contextParam];
        _realNodeMap = [NSMutableDictionary new];
        _realShadowMap = [NSMutableDictionary new];
        _realRootNode = [[KRTurboDisplayNode alloc] initWithTag:KRV_ROOT_VIEW_TAG viewName:ROOT_VIEW_NAME];
        _realNodeMap[KRV_ROOT_VIEW_TAG] = _realRootNode;

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onReceiveSetCurrentUINotification:)
                                                     name:kSetCurrentUIAsFirstScreenForNextLaunchNotificationName object:rootView];
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onReceiveCloseTurboDisplayNotification:)
                                                     name:kCloseTurboDisplayNotificationName object:rootView];
        
        
    }
    return self;
}

#pragma mark - public

- (void)didInit {
    // 读取TurboDisplay渲染指令二进制文件
    double readBeginTime = CFAbsoluteTimeGetCurrent();
    _turboDisplayCacheData = [[KRTurboDisplayCacheManager sharedInstance] nodeWithCachKey:self.turboDisplayCacheKey];
    if ([_turboDisplayCacheData.turboDisplayNode isKindOfClass:[KRTurboDisplayNode class]]) {
        _lazyRendering = YES; // 有TurboDisplay才会进行懒渲染
        KRTurboDisplayModule *module = (KRTurboDisplayModule *)[_renderLayerHandler moduleWithName:NSStringFromClass([KRTurboDisplayModule class])];
        module.firstScreenTurboDisplay = YES;
    }
    double readTurboFileCostTime = (CFAbsoluteTimeGetCurrent() - readBeginTime) * 1000.0;
    KR_WEAK_SELF
    [_uiScheduler performWhenViewDidLoadWithTask:^{
        // 首帧之后去diff两棵树patch差量渲染指令更新到渲染器
        [weakSelf diffPatchToRenderLayer];
    }];
    if (_lazyRendering) {
        [_uiScheduler markViewDidLoad];
        // 渲染TurboDisplay首屏
        double renderBeginTime = CFAbsoluteTimeGetCurrent();
        [self renderTurboDisplayNodeToRenderLayerWithNode:self.turboDisplayCacheData.turboDisplayNode];
        UIView *view = (UIView *)[_renderLayerHandler viewWithTag:self.turboDisplayCacheData.turboDisplayNode.children.firstObject.tag];
        [view.superview layoutIfNeeded]; // 为了触发contentViewDidLoad首屏渲染完成
        double renderCostTime = (CFAbsoluteTimeGetCurrent() - renderBeginTime) * 1000.0f;
        NSString *log = [NSString stringWithFormat:@"page_name:%@ turbo_display render cost_time %.2lfms readTurboFileCostTime: %.2lfms :%d", _contextParam.pageName, renderCostTime, readTurboFileCostTime, _lazyRendering];
        [KRLogModule logInfo:log];
        
    } else {
        [KRLogModule logInfo:[NSString stringWithFormat:@"page:%@ has not turboDisplay file", _contextParam.pageName]];
    }
   
}


#pragma mark - KuiklyRenderLayerProtocol

- (void)createRenderViewWithTag:(NSNumber *)tag
                       viewName:(NSString *)viewName {
    if (_realNodeMap) {
        KRTurboDisplayNode *node =  [[KRTurboDisplayNode alloc] initWithTag:tag viewName:viewName];
        _realNodeMap[tag] = node;
        [self setNeedUpdateNextTurboDisplayRootNode];
        [self addTaskOnNextLoopMainQueueWihTask:^{
            node.addViewMethodDisable = YES;  // only save one frame
        }];
    }
    if (!_lazyRendering) {
        [_renderLayerHandler createRenderViewWithTag:tag viewName:viewName];
    }
}

- (void)removeRenderViewWithTag:(NSNumber *)tag {
    if (_realNodeMap) {
        KRTurboDisplayNode *node = _realNodeMap[tag];
        KRTurboDisplayNode *parentNode = _realNodeMap[node.parentTag];
        [node removeFromParentNode:parentNode];
        [_realNodeMap removeObjectForKey:tag];
    }
    if (!_lazyRendering) {
        [_renderLayerHandler removeRenderViewWithTag:tag];
    }
}

- (void)insertSubRenderViewWithParentTag:(NSNumber *)parentTag
                                childTag:(NSNumber *)childTag
                                 atIndex:(NSInteger)index {
    if (_realNodeMap) {
        KRTurboDisplayNode *parentNode = _realNodeMap[parentTag];
        KRTurboDisplayNode *subNode = _realNodeMap[childTag];
        [parentNode insertSubNode:subNode index:index];
        [self setNeedUpdateNextTurboDisplayRootNode];
    }
    if (!_lazyRendering) {
        [_renderLayerHandler insertSubRenderViewWithParentTag:parentTag childTag:childTag atIndex:index];
    }
    
}

- (void)setPropWithTag:(NSNumber *)tag propKey:(NSString *)propKey propValue:(id)propValue {
    if (_realNodeMap) {
        KRTurboDisplayNode *node = _realNodeMap[tag];
        [node setPropWithKey:propKey propValue:propValue];
        [self setNeedUpdateNextTurboDisplayRootNode];
    }
    if (!_lazyRendering) {
        [_renderLayerHandler setPropWithTag:tag propKey:propKey propValue:propValue];
    }
}

- (void)setShadowWithTag:(NSNumber *)tag shadow:(id<KuiklyRenderShadowProtocol>)shadow {
    [self setNeedUpdateNextTurboDisplayRootNode];
    if (!_lazyRendering) {
        [_renderLayerHandler setShadowWithTag:tag shadow:shadow];
    }
}

- (void)setRenderViewFrameWithTag:(NSNumber *)tag frame:(CGRect)frame {
    if (_realNodeMap) {
        KRTurboDisplayNode *node = _realNodeMap[tag];
        [node setFrame:frame];
        [self setNeedUpdateNextTurboDisplayRootNode];
    }
    if (!_lazyRendering) {
        [_renderLayerHandler setRenderViewFrameWithTag:tag frame:frame];
    }
}

- (CGSize)calculateRenderViewSizeWithTag:(NSNumber *)tag constraintSize:(CGSize)constraintSize {
    if (_realShadowMap) {
        KRTurboDisplayShadow *shadow = _realShadowMap[tag];
        [shadow calculateWithConstraintSize:constraintSize];
    }
    return [_renderLayerHandler calculateRenderViewSizeWithTag:tag constraintSize:constraintSize];
}

- (void)callViewMethodWithTag:(NSNumber *)tag
                       method:(NSString *)method
                       params:(NSString * _Nullable)params
                     callback:(KuiklyRenderCallback _Nullable)callback {
    if (_realNodeMap) {
        KRTurboDisplayNode *node = _realNodeMap[tag];
        if (!node.addViewMethodDisable) {  // only save one frame
            [node addViewMethodWithMethod:method params:params callback:callback];
        }
    }
    if (!_lazyRendering) {
        [_renderLayerHandler callViewMethodWithTag:tag method:method params:params callback:callback];
    }
}

- (NSString * _Nullable)callModuleMethodWithModuleName:(NSString *)moduleName
                                                method:(NSString *)method
                                                params:(NSString * _Nullable)params
                                              callback:(KuiklyRenderCallback _Nullable)callback {
    if ([moduleName isEqualToString:NSStringFromClass([KRMemoryCacheModule class])]) {
        [_realRootNode addModuleMethodWithModuleName:moduleName method:method params:params callback:callback];
    }
    return [_renderLayerHandler callModuleMethodWithModuleName:moduleName method:method params:params callback:callback];
}

- (NSString * _Nullable)callTDFModuleMethodWithModuleName:(NSString *)moduleName
                                                   method:(NSString *)method
                                                   params:(NSString * _Nullable)params
                                           succCallbackId:(NSString *)succCallbackId
                                          errorCallbackId:(NSString *)errorCallbackId {
    return [_renderLayerHandler callTDFModuleMethodWithModuleName:moduleName
                                                           method:method
                                                           params:params
                                                   succCallbackId:succCallbackId
                                                  errorCallbackId:errorCallbackId];
}

/****  shadow 相关 ***/
- (void)createShadowWithTag:(NSNumber *)tag
                   viewName:(NSString *)viewName {
    if (_realShadowMap) {
        _realShadowMap[tag] = [[KRTurboDisplayShadow alloc] initWithTag:tag viewName:viewName];
    }
    [_renderLayerHandler createShadowWithTag:tag viewName:viewName];
}

- (void)removeShadowWithTag:(NSNumber *)tag {
    if (_realShadowMap) {
        [_realShadowMap removeObjectForKey:tag];
    }
    [_renderLayerHandler removeShadowWithTag:tag];
}

- (void)setShadowPropWithTag:(NSNumber *)tag propKey:(NSString *)propKey propValue:(id)propValue {
    if (_realShadowMap) {
        KRTurboDisplayShadow *shadow = _realShadowMap[tag];
        [shadow setPropWithKey:propKey propValue:propValue];
    }
    [_renderLayerHandler setShadowPropWithTag:tag propKey:propKey propValue:propValue];
}

- (NSString * _Nullable)callShadowMethodWithTag:(NSNumber *)tag method:(NSString * _Nonnull)method
                                         params:(NSString * _Nullable)params {
    if (_realShadowMap) {
        KRTurboDisplayShadow *shadow = _realShadowMap[tag];
        [shadow addMethodWithName:method params:params];
    }
    return [_renderLayerHandler callShadowMethodWithTag:tag method:method params:params];
}

- (id<KuiklyRenderShadowProtocol>)shadowWithTag:(NSNumber *)tag {
    id shadow = [_renderLayerHandler shadowWithTag:tag];
    if (_realShadowMap) {
        KRTurboDisplayShadow *viewShadow = [_realShadowMap[tag] deepCopy];
        KR_WEAK_SELF;
        [_uiScheduler addTaskToMainQueueWithTask:^{
            if (weakSelf.realNodeMap) {
                KRTurboDisplayNode *node = weakSelf.realNodeMap[tag];
                [node setShadow:viewShadow];
                node.renderShadow = shadow;
            }
        }];
    }
    return shadow;
   
}

- (id<TDFModuleProtocol>)moduleWithName:(NSString *)moduleName {
    return [_renderLayerHandler moduleWithName:moduleName];
}

- (id<KuiklyRenderViewExportProtocol>)viewWithTag:(NSNumber *)tag {
    return [_renderLayerHandler viewWithTag:tag];
}

- (void)updateViewTagWithCurTag:(NSNumber *)curTag newTag:(NSNumber *)newTag {
    [_renderLayerHandler updateViewTagWithCurTag:curTag newTag:newTag];
}

- (void)willDealloc {
    if (!_nextTurboDisplayRootNode) {
        [self rewriteTurboDisplayRootNodeIfNeed];
    }
    [self updateNextTurboDisplayRootNodeIfNeed];
}

/**
 * @brief 收到手势响应时调用
 */
- (void)didHitTest {
    // 收到手势，不在自动更新
    if (_nextTurboDisplayRootNode) {
        [self updateNextTurboDisplayRootNodeIfNeed];
        _closeAutoUpdateTurboDisplay = YES;
        _nextTurboDisplayRootNode = nil;
    }
}

#pragma mark - notification

- (void)onReceiveSetCurrentUINotification:(NSNotification *)notification {
    if (!_realRootNode) {
        return ;
    }
    KRTurboDisplayNode *node = _realRootNode;
    _closeAutoUpdateTurboDisplay = YES;// 已经被主动更新了，所以关闭
    [[KRTurboDisplayCacheManager sharedInstance] cacheWithViewNode:[node deepCopy] cacheKey:self.turboDisplayCacheKey];
}

- (void)onReceiveCloseTurboDisplayNotification:(NSNotification *)notification {
    [[KRTurboDisplayCacheManager sharedInstance] removeCacheWithKey:self.turboDisplayCacheKey];
    self.turboDisplayCacheData = nil;
}


#pragma mark - TurboDisplay rendering
// TurboDisplay首屏渲染到渲染器
- (void)renderTurboDisplayNodeToRenderLayerWithNode:(KRTurboDisplayNode *)node {
    if (!node) {
        return ;
    }
    [KRTurboDisplayDiffPatch diffPatchToRenderingWithRenderLayer:_renderLayerHandler oldNodeTree:nil newNodeTree:node];
}


#pragma mark - diff to rendering

// diff两棵树patch差量渲染指令更新到渲染器
- (void)diffPatchToRenderLayer {
    if (_realRootNode && !_nextTurboDisplayRootNode) {
        // 真首屏节点保持一份原型作为更新目标树,该树作为下次启动首屏
        _nextTurboDisplayRootNode = [_realRootNode deepCopy];
        [self setNeedUpdateNextTurboDisplayRootNode];
    }
    
    if (self.turboDisplayCacheData.turboDisplayNode && _realRootNode) { // 动静结合diff上屏
        [KRTurboDisplayDiffPatch diffPatchToRenderingWithRenderLayer:_renderLayerHandler
                                                         oldNodeTree:self.turboDisplayCacheData.turboDisplayNode
                                                         newNodeTree:_realRootNode];
    }
    _lazyRendering = NO;
    // 证明成功可以回写，如果文件不存在的话
    [self rewriteTurboDisplayRootNodeIfNeed];
    self.turboDisplayCacheData = nil;
}

- (void)rewriteTurboDisplayRootNodeIfNeed {
    NSData *turboDisplayNodeData = self.turboDisplayCacheData.turboDisplayNodeData;
    if (turboDisplayNodeData) { // 说明原来缓存文件没异常发生，回写作为兜底
        if (![[KRTurboDisplayCacheManager sharedInstance] hasNodeWithCacheKey:self.turboDisplayCacheKey]) {
            [[KRTurboDisplayCacheManager sharedInstance] cacheWithViewNodeData:turboDisplayNodeData cacheKey:self.turboDisplayCacheKey];
        }
    }
}

// 标记更新TurboDisplay首屏， 限频(0.5s内最多一次)
- (void)setNeedUpdateNextTurboDisplayRootNode {
    if (!_needUpdateNextTurboDisplayRootNode) {
        _needUpdateNextTurboDisplayRootNode = YES;
        KR_WEAK_SELF
        [KuiklyRenderThreadManager performOnMainQueueWithTask:^{
            [weakSelf updateNextTurboDisplayRootNodeIfNeed];
        } delay:0.5];
    }
}

// 添加任务到下一个runloop统一执行
- (void)addTaskOnNextLoopMainQueueWihTask:(dispatch_block_t)task {
    if (!_needSyncMainQueueOnNextRunLoop) {
        _needSyncMainQueueOnNextRunLoop = YES;
        if (!_nextLoopTaskOnMainQueue) {
            _nextLoopTaskOnMainQueue = [NSMutableArray new];
        }
        [_nextLoopTaskOnMainQueue addObject:task];
        dispatch_async(dispatch_get_main_queue(), ^{
            self.needSyncMainQueueOnNextRunLoop = NO;
            NSMutableArray *queue = self.nextLoopTaskOnMainQueue;
            self.nextLoopTaskOnMainQueue = nil;
            for (dispatch_block_t block  in queue) {
                block();
            }
        });
    }
}

// 更新TurboDisplay首屏
- (void)updateNextTurboDisplayRootNodeIfNeed {
    if (!self.needUpdateNextTurboDisplayRootNode) {
        return ;
    }
    assert([NSThread isMainThread]);
    self.needUpdateNextTurboDisplayRootNode = NO;
    if (_closeAutoUpdateTurboDisplay) {
        return ;
    }
    if (_realRootNode && _nextTurboDisplayRootNode) {
        // 限制更新频率，0.5s一次&&delloc兜底更新
        double beginTime = CFAbsoluteTimeGetCurrent();
        BOOL didUpdated =  [KRTurboDisplayDiffPatch onlyUpdateWithTargetNodeTree:_nextTurboDisplayRootNode fromNodeTree:_realRootNode];
        double deepCopyCostTime = 0;
        if (didUpdated) {
            // copy后异步线程缓存到磁盘持久化
            double beginTime = CFAbsoluteTimeGetCurrent();
            [[KRTurboDisplayCacheManager sharedInstance] cacheWithViewNode:[_nextTurboDisplayRootNode deepCopy] cacheKey:self.turboDisplayCacheKey];
            deepCopyCostTime = (CFAbsoluteTimeGetCurrent() - beginTime) * 1000.0;
        }
        double endTime = CFAbsoluteTimeGetCurrent();
        NSString *log = [NSString stringWithFormat:@"updateNextTurboDisplayRootNode: %.2lfms deepCopyCostTime:%.2lf didUpdated:%d page:%@",(endTime - beginTime) * 1000.0, deepCopyCostTime, didUpdated, _contextParam.pageName];
        [KRLogModule logInfo:log];
        
      
    }
}


#pragma mark - getter

- (NSString *)turboDisplayCacheKey {
    if (!_turboDisplayCacheKey) {
        _turboDisplayCacheKey = [[KRTurboDisplayCacheManager sharedInstance] cacheKeyWithTurboDisplayKey:_turboDisplayKey 
                                                                                                pageName:_contextParam.pageName];
    }
    return _turboDisplayCacheKey;
}



#pragma mark - delloc

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end
