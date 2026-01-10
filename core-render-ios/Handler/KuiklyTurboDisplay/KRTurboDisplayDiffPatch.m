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

#import "KRTurboDisplayDiffPatch.h"
#import "KRTurboDisplayShadow.h"
#import "KRLogModule.h"
#import "UIView+CSS.h"
#import "KuiklyRenderThreadManager.h"

#define SCROLL_VIEW @"KRScrollContentView"

@implementation KRTurboDisplayDiffPatch

static UIView *gBaseView = nil;

+ (void)diffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                oldNodeTree:(KRTurboDisplayNode *)oldNodeTree
                                newNodeTree:(KRTurboDisplayNode *)newNodeTree {
    // TB首屏diff，不启用延迟diff，使用默认的全量diff
    [self diffPatchToRenderingWithRenderLayer:renderLayer
                                  oldNodeTree:oldNodeTree
                                  newNodeTree:newNodeTree
                              diffPolicy:KRCacheFirstScreenDiff];
}


// 判断两个节点是否可以复用
+ (BOOL)canReuseNode:(KRTurboDisplayNode *)oldNode newNode:(KRTurboDisplayNode *)newNode fromUpdateNode:(BOOL)fromUpdateNode {
    if (!oldNode || !newNode) {
        return NO;
    }
    
    if (![oldNode.viewName isEqualToString:newNode.viewName]) {
        return NO;
    }
    
    if (oldNode.props.count != newNode.props.count) {
        return NO;
    }
    for (int i = 0; i < newNode.props.count; i++) {
        KRTurboDisplayProp *oldProp = oldNode.props[i];
        KRTurboDisplayProp *newProp = newNode.props[i];
        if (![oldProp.propKey isEqualToString:newProp.propKey]) {
            return NO;
        }
        if (oldProp.propType != newProp.propType) {
            return NO;
        }
        if (oldProp.propType == KRTurboDisplayPropTypeAttr) {
            if (fromUpdateNode && [oldProp.propKey isEqualToString:KR_TURBO_DISPLAY_AUTO_UPDATE_ENABLE]) { // 若有指定，新节点指定可用才可复用
                if (newProp.propValue && ![((NSNumber *)newProp.propValue) intValue]) {
                    return NO;
                }
            }
            
            if (!fromUpdateNode && ![self isBaseAttrKey:oldProp.propKey]) { // 非基础属性的话，如果propValue不一样就无法复用
                if (![self isEqualPropValueWithOldValue:oldProp.propValue newValue:newProp.propValue]) {
                    return NO;
                }
            }
        }
    }
    
  
    NSMutableArray *newNodeCallViewMethods = [NSMutableArray new];
    for (int i = 0; i < newNode.callMethods.count; i++) {
        if (newNode.callMethods[i].type == KRTurboDisplayNodeMethodTypeView) {
            [newNodeCallViewMethods addObject:newNode.callMethods[i]];
        }
    }
    NSMutableArray *oldNodecallViewMethods = [NSMutableArray new];
    for (int i = 0; i < oldNode.callMethods.count; i++) {
        if (oldNode.callMethods[i].type == KRTurboDisplayNodeMethodTypeView) {
            [oldNodecallViewMethods addObject:oldNode.callMethods[i]];
        }
    }
    
    if (newNodeCallViewMethods.count != oldNodecallViewMethods.count) {
        return NO;
    }
    for (int i = 0; i < newNodeCallViewMethods.count; i++) {
        KRTurboDisplayNodeMethod *oldMethod = oldNodecallViewMethods[i];
        KRTurboDisplayNodeMethod *newMethod = newNodeCallViewMethods[i];
        if (![oldMethod.method isEqualToString:newMethod.method]) {
            return NO;
        }
        if (![self isEqualPropValueWithOldValue:oldMethod.params newValue:newMethod.params]) {
            return NO;
        }
    }

    
    return YES;
}

// 判断两个属性值是否相等
+ (BOOL)isEqualPropValueWithOldValue:(id)oldValue newValue:(id)newValue {
    if (oldValue == newValue) {
        return YES;
    }
    if ([oldValue isKindOfClass:[NSString class]]) {
        if (!newValue) return NO;
        return [oldValue isEqualToString:newValue];
    }
    if ([oldValue isKindOfClass:[NSNumber class]]) {
        if (!newValue) return NO;
        return [oldValue isEqual:newValue];
    }
    return oldValue == newValue;
}

// 判断是否为基础属性
+ (BOOL)isBaseAttrKey:(NSString *)propKey {
    if (!gBaseView) {
        gBaseView = [UIView new];
    }
    SEL selector = NSSelectorFromString( [NSString stringWithFormat:@"setCss_%@:", propKey]);
    if ([gBaseView respondsToSelector:selector]) {
        return YES;
    }
    return NO;
}

// 生成渲染视图
+ (void)createRenderViewWithNode:(KRTurboDisplayNode *)node renderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer diffPolicy:(KRFirstScreenDiffPolicy)diffPolicy {
    if (!node) {
        return ;
    }
    if ([node.tag  isEqual: KRV_ROOT_VIEW_TAG]) { // 根节点不需要创建渲染视图
        // 根节点需要同步Module方法调用（因为仅有根节点用来缓存记录module方法调用）
        for (KRTurboDisplayNodeMethod *method in node.callMethods) {
            if (method.type == KRTurboDisplayNodeMethodTypeModule) { // 仅有rootNode才会有这个module调用记录，这里为了统一
                [renderLayer callModuleMethodWithModuleName:method.name method:method.method params:method.params callback:method.callback];
            }
        }
    } else {
        [renderLayer createRenderViewWithTag:node.tag viewName:node.viewName];
    }
    // TB首屏时
    [self updateRenderViewWithCurNode:nil newNode:node renderLayer:renderLayer hasParent:NO diffPolicy:diffPolicy];
    // 递归给子孩子创建渲染
    if (node.hasChild) {
        for (KRTurboDisplayNode *subNode in node.children) {
            [self createRenderViewWithNode:subNode renderLayer:renderLayer diffPolicy:diffPolicy];
        }
    }
}

// 删除渲染视图
+ (void)removeRenderViewWithNode:(KRTurboDisplayNode *)node renderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer {
    if (!node) {
        return ;
    }
    [renderLayer removeRenderViewWithTag:node.tag];
    if (node.hasChild) {
        for (KRTurboDisplayNode *subNode in node.children) {
            [self removeRenderViewWithNode:subNode renderLayer:renderLayer];
        }
    }
}

// 更新渲染视图
+ (void)updateRenderViewWithCurNode:(KRTurboDisplayNode *)curNode
                            newNode:(KRTurboDisplayNode *)newNode
                        renderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                          hasParent:(BOOL)hasParent {
    // 无论是否 onlyEventReplay，都需要执行 tag 置换
    if (curNode.tag && newNode.tag && ![newNode.tag isEqual:curNode.tag]) {
        [renderLayer updateViewTagWithCurTag:curNode.tag newTag:newNode.tag];
        curNode.tag = newNode.tag;
    }
    // 同步attr/frame/shadow/insert
    for (int i = 0; i < MAX(curNode.props.count, newNode.props.count) ; i++) {
        KRTurboDisplayProp *curProp = curNode.props.count > i ? curNode.props[i] : nil;
        KRTurboDisplayProp *newProp = newNode.props.count > i ? newNode.props[i] : nil;
        if (newProp.propType == KRTurboDisplayPropTypeAttr) {
            if (![self isEqualPropValueWithOldValue:curProp.propValue newValue:newProp.propValue]) {
                [renderLayer setPropWithTag:newNode.tag propKey:newProp.propKey propValue:newProp.propValue];
            }
        } else if (newProp.propType == KRTurboDisplayPropTypeEvent) {
            if (curProp) {
                [curProp performLazyEventToCallback:newProp.propValue];
            } else {
                [newProp lazyEventIfNeed];
            }
            [renderLayer setPropWithTag:newNode.tag propKey:newProp.propKey propValue:newProp.propValue];
        } else if (newProp.propType == KRTurboDisplayPropTypeFrame) {
            if (curProp.propValue && CGRectEqualToRect([((NSValue *)curProp.propValue) CGRectValue],
                                                       [((NSValue *)newProp.propValue) CGRectValue])) {
                // nothing to do
            } else {
               [renderLayer setRenderViewFrameWithTag:newNode.tag frame:[((NSValue *)newProp.propValue) CGRectValue]];
            }
        } else if (newProp.propType == KRTurboDisplayPropTypeShadow) {
            if (newNode.renderShadow) {
                [renderLayer setShadowWithTag:newNode.tag shadow:newNode.renderShadow];
            } else {
                [self setShadowForViewToRenderLayerWithShadow:(KRTurboDisplayShadow *)newProp.propValue node:newNode renderLayer:renderLayer];
            }
        } else if (newProp.propType == KRTurboDisplayPropTypeInsert) {
            if (!hasParent) {
                [renderLayer insertSubRenderViewWithParentTag:newNode.parentTag childTag:newNode.tag atIndex:[newProp.propValue intValue]];
            }
        }
    }
    // 同步View方法调用
    NSMutableArray *newNodeCallViewMethods = [NSMutableArray new];
    for (int i = 0; i < newNode.callMethods.count; i++) {
        if (newNode.callMethods[i].type == KRTurboDisplayNodeMethodTypeView) {
            [newNodeCallViewMethods addObject:newNode.callMethods[i]];
        }
    }
    NSMutableArray *curNodecallViewMethods = [NSMutableArray new];
    for (int i = 0; i < curNode.callMethods.count; i++) {
        if (curNode.callMethods[i].type == KRTurboDisplayNodeMethodTypeView) {
            [curNodecallViewMethods addObject:curNode.callMethods[i]];
        }
    }
    int fromIndex = 0;
    for (fromIndex = 0; fromIndex < newNodeCallViewMethods.count; fromIndex++) {
        KRTurboDisplayNodeMethod *method = newNodeCallViewMethods[fromIndex];
        KRTurboDisplayNodeMethod *curNodeMethod = curNodecallViewMethods.count > fromIndex ? curNodecallViewMethods[fromIndex] : nil;
        if (!curNodeMethod) {
            break;
        }
        if (![curNodeMethod.method isEqualToString:method.method]
            || ![self isEqualPropValueWithOldValue:curNodeMethod.params newValue:method.params]) {
            break;
        }
    }
    for (; fromIndex < newNodeCallViewMethods.count; fromIndex++) {
        KRTurboDisplayNodeMethod *method = newNodeCallViewMethods[fromIndex];
        [renderLayer callViewMethodWithTag:newNode.tag method:method.method params:method.params callback:method.callback];
    }
}


+ (void)setShadowForViewToRenderLayerWithShadow:(KRTurboDisplayShadow *)shadow
                                           node:(KRTurboDisplayNode *)node
                                    renderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer {
    // create shadow
    id<KuiklyRenderShadowProtocol> realShadow = [NSClassFromString(shadow.viewName) hrv_createShadow];
    
    // 向shadow增加ContextParam
    [renderLayer setContextParamToShadow:realShadow];
    
    if (!realShadow) {
#if !defined(NS_BLOCK_ASSERTIONS)
        NSString *assertMsg = [NSString stringWithFormat:@"create shadow failed:%@", shadow.viewName];
        NSAssert(NO, assertMsg);
#endif
        return ;
    }
    for (KRTurboDisplayProp *prop in shadow.props) {
        [realShadow hrv_setPropWithKey:prop.propKey propValue:prop.propValue];
    }
    [realShadow hrv_calculateRenderViewSizeWithConstraintSize:[((NSValue *)shadow.constraintSize) CGSizeValue]];
    dispatch_block_t task = nil;
     if ([realShadow respondsToSelector:@selector(hrv_taskToMainQueueWhenWillSetShadowToView)]) {
         task = [realShadow hrv_taskToMainQueueWhenWillSetShadowToView];
     }
    if (task) {
        task();
    }
    [renderLayer setShadowWithTag:node.tag shadow:realShadow];
}

+ (NSArray *)sortScrollIndexWithList:(NSArray *)list {
    return  [list sortedArrayUsingComparator:^NSComparisonResult(KRTurboDisplayNode *  _Nonnull obj1, KRTurboDisplayNode *  _Nonnull obj2) {
        if ([obj1.scrollIndex intValue] < [obj2.scrollIndex intValue]) {
            return NSOrderedAscending;
        } else if ([obj1.scrollIndex intValue] > [obj2.scrollIndex intValue]) {
            return NSOrderedDescending;
        }
        CGFloat index1 = obj1.renderFrame.origin.x + obj1.renderFrame.origin.y;
        CGFloat index2 = obj2.renderFrame.origin.x + obj2.renderFrame.origin.y;
        return index1 < index2 ? NSOrderedAscending: (index1 > index2 ? NSOrderedDescending : NSOrderedSame ) ;
    }];
}


/**
 * @brief 保留目标树结构，仅更新目标树属性信息
 * @param targetNodeTree 被更新的目标树
 * @param fromNodeTree 更新的来源树
 * @return 是否有发生更新
 */
+ (BOOL)onlyUpdateWithTargetNodeTree:(KRTurboDisplayNode *)targetNodeTree fromNodeTree:(KRTurboDisplayNode *)fromNodeTree {
    BOOL hasUpdate = NO;
    if ([self canReuseNode:targetNodeTree newNode:fromNodeTree fromUpdateNode:YES]) { // 是否同结构节点，才进行更新
        if ([self updateNodeWithTargetNode:targetNodeTree fromNode:fromNodeTree]) {
            hasUpdate = YES;
        }
        if ([self updateChildrenWithTargetNode:targetNodeTree fromNode:fromNodeTree]) {
            hasUpdate = YES;
        }
    } else {
        // 结构不同，全量替换「快照树」此位置节点的内容
        // 过去是这里没有额外的操作，如果不可复用，则此位置的值就不更新了
        [self replaceNodeContentWithTargetNode:targetNodeTree fromNode:fromNodeTree];
        hasUpdate = YES;
    }
    return hasUpdate;
}

+ (KRTurboDisplayNode *)nextNodeForUpdateWithChildern:(NSArray *)fromChildern fromIndex:(int *)fromIndex targetNode:(KRTurboDisplayNode *)targetNode {
    for (int i = *fromIndex; i < fromChildern.count; i++) {
        KRTurboDisplayNode *nextTargetNode = fromChildern[i];
        if ([nextTargetNode.tag isEqual:targetNode.tag]) {
            *fromIndex = i;
            return nextTargetNode;
        }
     }
    return fromChildern.count > (*fromIndex) ? fromChildern[*fromIndex] : nil;;
}

+ (BOOL)updateNodeWithTargetNode:(KRTurboDisplayNode *)node fromNode:(KRTurboDisplayNode *)fromNode {
    BOOL hasUpdate = NO;
    if (node.viewName != fromNode.viewName && ![node.viewName isEqualToString:fromNode.viewName]) {
        node.viewName = fromNode.viewName;
        hasUpdate = YES;
    }
    if (node.props.count == fromNode.props.count) {
        for (int i = 0; i < node.props.count; i++) {
            if ([self updatePropWithTargetProp:node.props[i] fromNode:fromNode.props[i]]) {
                hasUpdate = YES;
            }
        }
    }
    return hasUpdate;
}

+ (BOOL)updatePropWithTargetProp:(KRTurboDisplayProp *)prop fromNode:(KRTurboDisplayProp *)fromProp {
    BOOL hasUpdate = NO;
    
    if (prop.propType != fromProp.propType) {
        prop.propType = fromProp.propType;
        hasUpdate = YES;
    }
    
    if (prop.propKey != prop.propKey && ![prop.propKey isEqualToString:fromProp.propKey]) {
        prop.propKey = fromProp.propKey;
        hasUpdate = YES;
    }
    
    if (![self isEqualPropValueWithOldValue:prop.propValue newValue:fromProp.propValue]) {
        if (prop.propType == KRTurboDisplayPropTypeFrame) {
            
        }
        prop.propValue = fromProp.propValue;
        hasUpdate = YES;
    }
    return hasUpdate;
}


/**
 * @brief 递归更新子节点（支持结构变化）
 * @param targetNode 目标节点（快照树）
 * @param fromNode 来源节点（真实树）
 * @return 是否有发生更新
 */
+ (BOOL)updateChildrenWithTargetNode:(KRTurboDisplayNode *)targetNode fromNode:(KRTurboDisplayNode *)fromNode {
    BOOL hasUpdate = NO;
    
    NSMutableArray *targetChildren = targetNode.children;   // 快照树当前节点的子节点数组
    NSArray *fromChildren = fromNode.children;              // 真实树当前节点的子节点数组
    
    // 可滚动容器的操作和原来保持不变
    BOOL isScrollView = [targetNode.viewName isEqualToString:SCROLL_VIEW];
    if (isScrollView) {
        targetChildren = [[self sortScrollIndexWithList:targetChildren] mutableCopy];
        fromChildren = [self sortScrollIndexWithList:fromChildren];
        
        // 纵向列表可滚动容器直接替换内容（保持原有节点个数）这里的逻辑还存在问题 需要复核
        if (targetChildren.count && fromChildren.count >= targetChildren.count
            && fromNode.renderFrame.size.height > fromNode.renderFrame.size.width) {
            NSMutableArray *newChildren = [[[fromChildren mutableCopy] subarrayWithRange:NSMakeRange(0, targetChildren.count)] mutableCopy];;
            for (KRTurboDisplayNode *node in newChildren) {
                 node.parentTag = targetNode.tag;
            }
            targetNode.children = newChildren;
            return YES;
        }
    }
    
    /// 阶段2 基于真实树（新树）更新快照树（旧树）当前节点的子树
    // 构建快照树子节点的tag索引
    NSMutableDictionary<NSNumber *, KRTurboDisplayNode *> *targetChildrenMap = [NSMutableDictionary new];
    for (KRTurboDisplayNode *child in targetChildren) {
        if (child.tag) {
            targetChildrenMap[child.tag] = child;
        }
    }
    
    // 按照真实树的顺序重建快照树的children数组
    NSMutableArray *newTargetChildren = [NSMutableArray new];
    
    for (KRTurboDisplayNode *fromChild in fromChildren) {
        if (!fromChild.tag) {
            continue;
        }
        
        KRTurboDisplayNode *existingTargetChild = targetChildrenMap[fromChild.tag];
        if (existingTargetChild) {
            // 情况1：有新有旧 - 递归更新匹配的节点
            if ([self onlyUpdateWithTargetNodeTree:existingTargetChild fromNodeTree:fromChild]) {
                hasUpdate = YES;
            }
            [newTargetChildren addObject:existingTargetChild];  // 按照位置依次的加入
            [targetChildrenMap removeObjectForKey:fromChild.tag];
        } else {
            // 情况2：有新无旧 - 从真实树deepCopy节点，插入到对应位置
            KRTurboDisplayNode *newNode = [fromChild deepCopy];
            newNode.parentTag = targetNode.tag;
            [newTargetChildren addObject:newNode];
            hasUpdate = YES;
        }
    }
    
    // 处理情况3：无新有旧 - 删除快照树中存在但真实树中不存在的节点
    // targetChildrenMap中剩余的节点就是需要删除的,这些节点不会被加入newTargetChildren
    if (targetChildrenMap.count > 0) {
        hasUpdate = YES;
    }
    
    targetNode.children = newTargetChildren;
    return hasUpdate;
}

/**
 * @brief 全量替换节点内容（节点不可复用时，执行此操作）
 * @param targetNode 目标节点（快照树）
 * @param fromNode 来源节点（真实树）
 */
+ (void)replaceNodeContentWithTargetNode:(KRTurboDisplayNode *)targetNode fromNode:(KRTurboDisplayNode *)fromNode {
    NSNumber *originParentTag = targetNode.parentTag;
    
//    targetNode.tag = [fromNode.tag copy];   // 这个我觉得暂时是没有必要的，因为快照树和真实树在比对节点时tag一定是一致的
    targetNode.viewName = [fromNode.viewName copy];
    targetNode.parentTag = originParentTag;     // 保持原有的父子关系
    
    // 复制props
    [targetNode.props removeAllObjects];
    for (KRTurboDisplayProp *prop in fromNode.props) {
        [targetNode.props addObject:[prop deepCopy]];
    }
    
    // 复制callMethods
    [targetNode.callMethods removeAllObjects];
    for (KRTurboDisplayNodeMethod *method in fromNode.callMethods) {
        [targetNode.callMethods addObject:[method deepCopy]];
    }
    
    // 复制children
    [targetNode.children removeAllObjects];
    for (KRTurboDisplayNode *child in fromNode.children) {
        KRTurboDisplayNode *copyChild = [child deepCopy];
        copyChild.parentTag = targetNode.tag;
        [targetNode.children addObject:copyChild];
    }
    
}

/// 延迟diff实现，仅仅只是针对第二次的diff-view
+ (void)delayedDiffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                       oldNodeTree:(KRTurboDisplayNode *)oldNodeTree
                                       newNodeTree:(KRTurboDisplayNode *)newNodeTree {
    // 阶段1：当前帧执行 Tag 置换 + 事件回放 + 事件绑定
    [self diffPatchToRenderingWithRenderLayer:renderLayer
                                  oldNodeTree:oldNodeTree
                                  newNodeTree:newNodeTree
                              diffPolicy:KRRealFirstScreenDiffEventReplay];
    
    // 阶段2：在 Kuikly 线程队列末尾添加任务，等待跨端侧渲染指令全部到达后执行延迟渲染
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        // 阶段3：回到主线程执行延迟的渲染指令
        dispatch_async(dispatch_get_main_queue(), ^{
            [self diffPatchToRenderingWithRenderLayer:renderLayer
                                          oldNodeTree:oldNodeTree
                                          newNodeTree:newNodeTree
                                           diffPolicy:KRRealFirstScreenDiffPropUpdate];
        });
    }];
}


#pragma mark - Internal Diff Implementation

+ (void)diffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                oldNodeTree:(KRTurboDisplayNode *)oldNodeTree
                                newNodeTree:(KRTurboDisplayNode *)newNodeTree
                                 diffPolicy:(KRFirstScreenDiffPolicy)diffPolicy {
    // 逐层比较，属性和事件key不一样，就删除该节点，如果仅属性值变化就update该属性
    // 能否复用
    if ([self canReuseNode:oldNodeTree newNode:newNodeTree fromUpdateNode:NO]) {
        // 更新渲染视图
        [self updateRenderViewWithCurNode:oldNodeTree newNode:newNodeTree renderLayer:renderLayer hasParent:YES diffPolicy:diffPolicy];
        
        NSArray *aChilden = oldNodeTree.children;
        NSArray *bChilden = newNodeTree.children;
        
        if ([oldNodeTree.viewName isEqualToString:SCROLL_VIEW]) { // 可滚动容器节点孩子需要排序
            aChilden = [self sortScrollIndexWithList:aChilden];
            bChilden = [self sortScrollIndexWithList:bChilden];
         }
        
        for (int i = 0; i < MAX(aChilden.count, bChilden.count); i++) {
            KRTurboDisplayNode *oldNode = aChilden.count > i ? aChilden[i] : nil;
            KRTurboDisplayNode *newNode = bChilden.count > i ? bChilden[i] : nil;
            [self diffPatchToRenderingWithRenderLayer:renderLayer oldNodeTree:oldNode newNodeTree:newNode diffPolicy:diffPolicy];
        }
    } else {
        // 仅事件回放模式下，不执行删除和创建操作
        if (diffPolicy == KRRealFirstScreenDiffEventReplay) {
            return;
        }
        [KRLogModule logInfo:[NSString stringWithFormat:@"turbo_display un used with old node:%@ new node:%@", oldNodeTree.viewName, newNodeTree.viewName]];
        // 删除渲染视图
        [self removeRenderViewWithNode:oldNodeTree renderLayer:renderLayer];
        // 新建渲染视图
        [self createRenderViewWithNode:newNodeTree renderLayer:renderLayer diffPolicy:diffPolicy];
    }
}

// 更新渲染视图（核心修改：新增 diffPolicy 参数）
+ (void)updateRenderViewWithCurNode:(KRTurboDisplayNode *)curNode
                            newNode:(KRTurboDisplayNode *)newNode
                        renderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                          hasParent:(BOOL)hasParent
                    diffPolicy:(KRFirstScreenDiffPolicy)diffPolicy {
    
    // ========== 阶段1：Tag 置换（必须最先执行）==========
    if (curNode.tag && newNode.tag && ![newNode.tag isEqual:curNode.tag]) {
        [renderLayer updateViewTagWithCurTag:curNode.tag newTag:newNode.tag];
        curNode.tag = newNode.tag;
    }
    
    // ========== 阶段2：遍历属性，根据 diffPolicy 决定执行内容 ==========
    for (int i = 0; i < MAX(curNode.props.count, newNode.props.count) ; i++) {
        KRTurboDisplayProp *curProp = curNode.props.count > i ? curNode.props[i] : nil;
        KRTurboDisplayProp *newProp = newNode.props.count > i ? newNode.props[i] : nil;
        
        if (newProp.propType == KRTurboDisplayPropTypeEvent) {
            // 事件处理：分为事件回放和事件绑定两部分
            switch (diffPolicy) {
                case KRCacheFirstScreenDiff:
                    // 构建临时callback
                    if (curProp == nil) {
                        [newProp lazyEventIfNeed];
                    }
                    [renderLayer setPropWithTag:newNode.tag propKey:newProp.propKey propValue:newProp.propValue];
                    break;
                case KRRealFirstScreenDiffEventReplay:
                    if (curProp && curProp.lazyEventCallbackResults.count > 0) {
                        KREventReplayPolicy policy = [KRTurboDisplayProp replayPolicyForEventKey:newProp.propKey];
                        [curProp performLazyEventToCallback:newProp.propValue withPolicy:policy];
                    }
                    [renderLayer setPropWithTag:newNode.tag propKey:newProp.propKey propValue:newProp.propValue];
                    break;
                case KRRealFirstScreenDiffPropUpdate:
                    break;
            }
        } else if (diffPolicy == KRCacheFirstScreenDiff || diffPolicy == KRRealFirstScreenDiffPropUpdate) {
            // 非事件类型的属性：仅在 TB首屏 或 属性更新阶段 执行
            if (newProp.propType == KRTurboDisplayPropTypeAttr) {
                if (![self isEqualPropValueWithOldValue:curProp.propValue newValue:newProp.propValue]) {
                    [renderLayer setPropWithTag:newNode.tag propKey:newProp.propKey propValue:newProp.propValue];
                }
            } else if (newProp.propType == KRTurboDisplayPropTypeFrame) {
                if (curProp.propValue && CGRectEqualToRect([((NSValue *)curProp.propValue) CGRectValue],
                                                           [((NSValue *)newProp.propValue) CGRectValue])) {
                    // nothing to do
                } else {
                   [renderLayer setRenderViewFrameWithTag:newNode.tag frame:[((NSValue *)newProp.propValue) CGRectValue]];
                }
            } else if (newProp.propType == KRTurboDisplayPropTypeShadow) {
                if (newNode.renderShadow) {
                    [renderLayer setShadowWithTag:newNode.tag shadow:newNode.renderShadow];
                } else {
                    [self setShadowForViewToRenderLayerWithShadow:(KRTurboDisplayShadow *)newProp.propValue node:newNode renderLayer:renderLayer];
                }
            } else if (newProp.propType == KRTurboDisplayPropTypeInsert) {
                if (!hasParent) {
                    [renderLayer insertSubRenderViewWithParentTag:newNode.parentTag childTag:newNode.tag atIndex:[newProp.propValue intValue]];
                }
            }
        }
    }
    
    // ========== 阶段3：View 方法调用（仅在非事件回放模式下执行）==========
    if (diffPolicy == KRRealFirstScreenDiffEventReplay) {
        return;
    }
    
    // 同步View方法调用
    NSMutableArray *newNodeCallViewMethods = [NSMutableArray new];
    for (int i = 0; i < newNode.callMethods.count; i++) {
        if (newNode.callMethods[i].type == KRTurboDisplayNodeMethodTypeView) {
            [newNodeCallViewMethods addObject:newNode.callMethods[i]];
        }
    }
    NSMutableArray *curNodecallViewMethods = [NSMutableArray new];
    for (int i = 0; i < curNode.callMethods.count; i++) {
        if (curNode.callMethods[i].type == KRTurboDisplayNodeMethodTypeView) {
            [curNodecallViewMethods addObject:curNode.callMethods[i]];
        }
    }
    int fromIndex = 0;
    for (fromIndex = 0; fromIndex < newNodeCallViewMethods.count; fromIndex++) {
        KRTurboDisplayNodeMethod *method = newNodeCallViewMethods[fromIndex];
        KRTurboDisplayNodeMethod *curNodeMethod = curNodecallViewMethods.count > fromIndex ? curNodecallViewMethods[fromIndex] : nil;
        if (!curNodeMethod) {
            break;
        }
        if (![curNodeMethod.method isEqualToString:method.method]
            || ![self isEqualPropValueWithOldValue:curNodeMethod.params newValue:method.params]) {
            break;
        }
    }
    for (; fromIndex < newNodeCallViewMethods.count; fromIndex++) {
        KRTurboDisplayNodeMethod *method = newNodeCallViewMethods[fromIndex];
        [renderLayer callViewMethodWithTag:newNode.tag method:method.method params:method.params callback:method.callback];
    }
}


@end
