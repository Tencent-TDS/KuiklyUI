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
#import "KRTurboDisplayStateRestorableProtocol.h"

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
/** 根视图（弱引用，用于通知来源验证） */
@property (nonatomic, weak) UIView *rootView;

@end

@implementation KuiklyTurboDisplayRenderLayerHandler {
    BOOL _didCloseTurboDisplayRenderingMode;
    NSString *_turboDisplayKey;
    BOOL _closeAutoUpdateTurboDisplay;      // 关闭自动更新TurboDisplay, 由业务来主动设置首屏更新时机
    NSString *_extraCacheContent;           // 额外缓存内容（JSON字符串）
}

#pragma mark - KuiklyRenderLayerProtocol

- (instancetype)initWithRootView:(UIView *)rootView contextParam:(KuiklyContextParam *)contextParam {
    return [self initWithRootView:rootView contextParam:contextParam turboDisplayKey:@""];
}

- (instancetype)initWithRootView:(UIView *)rootView contextParam:(KuiklyContextParam *)contextParam turboDisplayKey:(nonnull NSString *)turboDisplayKey {
    if (self = [super init]) {
        _turboDisplayKey = turboDisplayKey;
        _contextParam = contextParam;
        _rootView = rootView;
        _renderLayerHandler = [[KuiklyRenderLayerHandler alloc] initWithRootView:rootView contextParam:contextParam];
        _realNodeMap = [NSMutableDictionary new];
        _realShadowMap = [NSMutableDictionary new];
        _realRootNode = [[KRTurboDisplayNode alloc] initWithTag:KRV_ROOT_VIEW_TAG viewName:ROOT_VIEW_NAME];
        _realNodeMap[KRV_ROOT_VIEW_TAG] = _realRootNode;
        _closeAutoUpdateTurboDisplay = [[KRTurboDisplayConfig sharedConfig] isCloseAutoUpdateTurboDisplay];
        
        // 新增：在init时机，读取业务自定义的缓存内容；只读取不执行删除
        _extraCacheContent = [[KRTurboDisplayCacheManager sharedInstance] extraCacheContentWithCacheKey:self.turboDisplayCacheKey];
        
        // 更新 TurboDisplayModuleMethod 强制刷新TurboDispla缓存
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onReceiveSetCurrentUINotification:)
                                                     name:kSetCurrentUIAsFirstScreenForNextLaunchNotificationName object:rootView];
        // TurboDisplayModuleMethod 强制关闭本地TurboDispla缓存
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onReceiveCloseTurboDisplayNotification:)
                                                     name:kCloseTurboDisplayNotificationName object:rootView];
        // 新增：TurboDisplayModuleMethod 强制清除当前已有的TurboDispla缓存
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onReceiveClearCurrentPageCacheNotification:)
                                                     name:kClearCurrentPageCacheNotificationName object:rootView];
    }
    return self;
}

#pragma mark - public

/// TurboDisplay机制 入口函数
- (void)didInit {
    // 1.TB缓存读取
    double readBeginTime = CFAbsoluteTimeGetCurrent();
    _turboDisplayCacheData = [[KRTurboDisplayCacheManager sharedInstance] nodeWithCachKey:self.turboDisplayCacheKey];
    if ([_turboDisplayCacheData.turboDisplayNode isKindOfClass:[KRTurboDisplayNode class]]) {
        _lazyRendering = YES;                                               // 存在TB缓存，更新懒渲染标志
        _turboDisplayCacheData.extraCacheContent = _extraCacheContent;      // 业务自定义缓存，与TB缓存存储于同一对象
        KRTurboDisplayModule *module = (KRTurboDisplayModule *)[_renderLayerHandler moduleWithName:NSStringFromClass([KRTurboDisplayModule class])];
        module.firstScreenTurboDisplay = YES;
    }
    double readTurboFileCostTime = (CFAbsoluteTimeGetCurrent() - readBeginTime) * 1000.0;
    
    // 2.注入业务真实首屏diff任务，viewDidLoad时机后执行第二次diff-View
    KR_WEAK_SELF
    [_uiScheduler performWhenViewDidLoadWithTask:^{
        // 首帧之后去diff两棵树patch差量渲染指令更新到渲染器
        [weakSelf diffPatchToRenderLayer];
    }];
    
    // 3.懒加载TB首屏，在viewDidLoad时机之前，通过第一次diff-view执行TB首屏渲染
    if (_lazyRendering) {
        [_uiScheduler markViewDidLoad];
        double renderBeginTime = CFAbsoluteTimeGetCurrent();
        
        [self renderTurboDisplayNodeToRenderLayerWithNode:self.turboDisplayCacheData.turboDisplayNode];     // 加载TB首屏
        [self applyExtraCacheContentToViews];       // 应用额外缓存内容到对应View（如ListView的offset）
        
        UIView *view = (UIView *)[_renderLayerHandler viewWithTag:self.turboDisplayCacheData.turboDisplayNode.children.firstObject.tag];
        [view.superview layoutIfNeeded];            // 为了触发contentViewDidLoad首屏渲染完成
        
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
    
    /// ToDo项：valo view粒度的结构变化捕捉的开关，要在这里就先给识别出来，然后在这里就直接设置给TurboDisplayNode
    if (_realNodeMap) {
        KRTurboDisplayNode *node = _realNodeMap[tag];
        [node setPropWithKey:propKey propValue:propValue];
        [self setNeedUpdateNextTurboDisplayRootNode];
    }
    if (!_lazyRendering) {
        [_renderLayerHandler setPropWithTag:tag propKey:propKey propValue:propValue];
    }
}

/*
 * 新增：向shadow注入ContextParam
 */
- (void)setContextParamToShadow:(id<KuiklyRenderShadowProtocol>)shadow {
    [_renderLayerHandler setContextParamToShadow:shadow];
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
    // 收到手势，关闭自动更新
    if (_nextTurboDisplayRootNode) {
        // 关闭自动更新之前，执行一次Diff-DOM
        [self updateNextTurboDisplayRootNodeIfNeed];
        
        _closeAutoUpdateTurboDisplay = YES;
        [[KRTurboDisplayConfig sharedConfig] enableCloseAutoUpdateTurboDisplay];
        
        _nextTurboDisplayRootNode = nil;
    }
}

#pragma mark - notification
// 强制刷新TB缓存
- (void)onReceiveSetCurrentUINotification:(NSNotification *)notification {
    
    // 验证通知来源是否为当前实例关联的 rootView
    if (notification.object != _rootView) {
        return;
    }
    
    if (!_realRootNode) {
        return;
    }
    // 业务手动强制刷新，与自动刷新相斥，因此默认执行自动刷新关闭
    _closeAutoUpdateTurboDisplay = YES;
    [[KRTurboDisplayConfig sharedConfig] enableCloseAutoUpdateTurboDisplay];
    
    
    // 获取额外缓存内容
    NSString *extraCacheContent = nil;
    if (notification.userInfo && [notification.userInfo isKindOfClass:[NSDictionary class]]) {
        NSString *paramStr = notification.userInfo[@"param"];
        NSDictionary *dict = [paramStr hr_stringToDictionary];
        id content = dict[@"extraCacheContent"];
        
        if ([content isKindOfClass:[NSString class]] && [(NSString *)content length] > 0) {
            extraCacheContent = (NSString *)content;
        }
    }
    
    // 优化强刷缓存的逻辑：从直接全量deepcopy真实树 优化为 使用diff-DOM 基于「真实树」捕捉结构变化更新「快照树」
    KRTurboDisplayNode *nodeToCache = nil;
    if (_nextTurboDisplayRootNode) {
        [KRTurboDisplayDiffPatch onlyUpdateWithTargetNodeTree:_nextTurboDisplayRootNode fromNodeTree:_realRootNode];
        nodeToCache = [_nextTurboDisplayRootNode deepCopy];
    } else {
        // 如果快照树不存在，则直接deepCopy真实树
        nodeToCache = [_realRootNode deepCopy];
    }
    
    // 【重要】TB 缓存和 extra 缓存必须原子性写入，避免异步写入的竞态条件
    // 场景：用户在 ScrollEnd 后立即杀死 App，可能导致只写入了 extra 缓存而 TB 缓存未写入
    // 后果：下次启动时 extra 缓存的 index 与 TB 缓存不匹配，导致 scrollToPosition 越界崩溃
    [[KRTurboDisplayCacheManager sharedInstance] cacheWithViewNode:nodeToCache 
                                                          cacheKey:self.turboDisplayCacheKey 
                                                 extraCacheContent:extraCacheContent];
    
}

- (void)onReceiveCloseTurboDisplayNotification:(NSNotification *)notification {
    // 验证通知来源
    if (notification.object != _rootView) {
        return;
    }
    [[KRTurboDisplayCacheManager sharedInstance] removeCacheWithKey:self.turboDisplayCacheKey];
    self.turboDisplayCacheData = nil;
}

- (void)onReceiveClearCurrentPageCacheNotification:(NSNotification *)notification {
    // 验证通知来源
    if (notification.object != _rootView) {
        return;
    }
    [[KRTurboDisplayCacheManager sharedInstance] removeCacheWithKey:self.turboDisplayCacheKey];
    self.turboDisplayCacheData = nil;
    _nextTurboDisplayRootNode = nil;
    
    // 关闭自动更新
    _closeAutoUpdateTurboDisplay = NO;
    [[KRTurboDisplayConfig sharedConfig] disableCloseAutoUpdateTurboDisplay];
}

#pragma mark - TurboDisplay rendering
// TurboDisplay首屏渲染到渲染器
- (void)renderTurboDisplayNodeToRenderLayerWithNode:(KRTurboDisplayNode *)node {
    if (!node) {
        return ;
    }
    // 第一次diff-view，执行默认diff，非延迟diff，作用是上屏TB首屏
    [KRTurboDisplayDiffPatch diffPatchToRenderingWithRenderLayer:_renderLayerHandler oldNodeTree:nil newNodeTree:node];
}

/**
 * @brief 应用额外缓存内容到对应的View（恢复状态）
 * extraCacheContent 格式：{ "tag": { "viewName": "xxx", "propKey": propValue, ... } }
 * 注意：extraCacheContent 中的 tag 是业务原始 tag（正数），需要格式化为负数才能匹配 TB 首屏的 View
 */
- (void)applyExtraCacheContentToViews {
    if (!_extraCacheContent.length) {
        return;
    }
    
    @try {
        NSDictionary *extraDict = [_extraCacheContent hr_stringToDictionary];
        if (![extraDict isKindOfClass:[NSDictionary class]]) {
            return;
        }
        
        for (NSString *tagStr in extraDict.allKeys) {
            NSDictionary *viewProps = extraDict[tagStr];
            if (![viewProps isKindOfClass:[NSDictionary class]]) {
                continue;
            }
            
            // 业务原始 tag（正数）需要格式化为负数，与 TB 缓存的 tag 格式保持一致
            NSInteger originalTag = [tagStr integerValue];
            NSNumber *formattedTag;
            if (originalTag >= 0) {
                formattedTag = @(-(originalTag + 2));
            } else {
                formattedTag = @(originalTag);
            }
            
            UIView *view = (UIView *)[_renderLayerHandler viewWithTag:formattedTag];
            if (!view) {
                continue;
            }
            
            if ([view conformsToProtocol:@protocol(KRTurboDisplayStateRestorableProtocol)]) {
                NSMutableDictionary *props = [viewProps mutableCopy];
                [props removeObjectForKey:@"viewName"];
                [(id<KRTurboDisplayStateRestorableProtocol>)view applyTurboDisplayExtraCacheContent:props];
            } else {
                [KRLogModule logInfo:[NSString stringWithFormat:@"【TurboDisplay-恢复状态】view 未实现 KRTurboDisplayStateRestorableProtocol 协议"]];
            }
        }
    } @catch (NSException *exception) {
        [KRLogModule logError:[NSString stringWithFormat:@"applyExtraCacheContentToViews error: %@", exception]];
    }
}


#pragma mark - diff to rendering
// diff两棵树patch差量渲染指令更新到渲染器 （第二次diff-view执行）
- (void)diffPatchToRenderLayer {
    // 自动刷新任务注册，开启后续的diff-DOM
    if (_realRootNode && !_nextTurboDisplayRootNode) {
        _nextTurboDisplayRootNode = [_realRootNode deepCopy];
        [self setNeedUpdateNextTurboDisplayRootNode];
    }
    
    // 执行业务首屏的 diff-view，根据开关选择模式
    if (self.turboDisplayCacheData.turboDisplayNode && _realRootNode) {
        KRSecondDiffMode mode = [KRTurboDisplayDiffPatch secondDiffMode];
                
        if (mode == KRSecondDiffModeDelayed) {
            // 延迟diff
            [KRTurboDisplayDiffPatch delayedDiffPatchToRenderingWithRenderLayer:_renderLayerHandler
                                                                    oldNodeTree:self.turboDisplayCacheData.turboDisplayNode
                                                                    newNodeTree:_realRootNode
                                                                     completion:^{
                // 回调关闭懒加载标志
                self->_lazyRendering = NO;
                [self rewriteTurboDisplayRootNodeIfNeed];
                self.turboDisplayCacheData = nil;
            }];
        } else {
            // 经典模式（旧机制）：使用带 diffPolicy 的方法，传入 KRCacheFirstScreenDiff 表示全量 diff
            [KRTurboDisplayDiffPatch diffPatchToRenderingWithRenderLayer:_renderLayerHandler
                                                             oldNodeTree:self.turboDisplayCacheData.turboDisplayNode
                                                             newNodeTree:_realRootNode
                                                              diffPolicy:KRCacheFirstScreenDiff];
            // 同步关闭懒加载标志
            _lazyRendering = NO;
            [self rewriteTurboDisplayRootNodeIfNeed];
            self.turboDisplayCacheData = nil;
        }
    } else {
        // 兜底
        _lazyRendering = NO;
        [self rewriteTurboDisplayRootNodeIfNeed];
        self.turboDisplayCacheData = nil;
    }
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

- (NSString *)extraCacheContent {
    return _extraCacheContent;
}

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
