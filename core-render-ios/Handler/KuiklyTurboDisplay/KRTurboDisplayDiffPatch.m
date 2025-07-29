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

#define SCROLL_VIEW @"KRScrollContentView"

@implementation KRTurboDisplayDiffPatch

static UIView *gBaseView = nil;

+ (void)diffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                oldNodeTree:(KRTurboDisplayNode *)oldNodeTree
                                newNodeTree:(KRTurboDisplayNode *)newNodeTree {
    // 逐层比较，属性和事件key不一样，就删除该节点，如果仅属性值变化就update该属性
    // 能否复用
    if ([self canReuseNode:oldNodeTree newNode:newNodeTree fromUpdateNode:NO]) {
        // 更新渲染视图
        [self updateRenderViewWithCurNode:oldNodeTree newNode:newNodeTree renderLayer:renderLayer hasParent:YES];
        
        NSArray *aChilden = oldNodeTree.children;
        NSArray *bChilden = newNodeTree.children;
        
        if ([oldNodeTree.viewName isEqualToString:SCROLL_VIEW]) { // 可滚动容器节点孩子需要排序
            aChilden = [self sortScrollIndexWithList:aChilden];
            bChilden = [self sortScrollIndexWithList:bChilden];
         }
        
        for (int i = 0; i < MAX(aChilden.count, bChilden.count); i++) {
            KRTurboDisplayNode *oldNode = aChilden.count > i ? aChilden[i] : nil;
            KRTurboDisplayNode *newNode = bChilden.count > i ? bChilden[i] : nil;
            [self diffPatchToRenderingWithRenderLayer:renderLayer oldNodeTree:oldNode newNodeTree:newNode];
        }
    } else {
        [KRLogModule logInfo:[NSString stringWithFormat:@"turbo_display un used with old node:%@ new node:%@", oldNodeTree.viewName, newNodeTree.viewName]];
        // 删除渲染视图
        [self removeRenderViewWithNode:oldNodeTree renderLayer:renderLayer];
        // 新建渲染视图
        [self createRenderViewWithNode:newNodeTree renderLayer:renderLayer];
    }
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
+ (void)createRenderViewWithNode:(KRTurboDisplayNode *)node renderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer {
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
    [self updateRenderViewWithCurNode:nil newNode:node renderLayer:renderLayer hasParent:NO];
    // 递归给子孩子创建渲染
    if (node.hasChild) {
        for (KRTurboDisplayNode *subNode in node.children) {
            [self createRenderViewWithNode:subNode renderLayer:renderLayer];
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
    if (!realShadow) {
        NSString *assertMsg = [NSString stringWithFormat:@"create shadow failed:%@", shadow.viewName];
        NSAssert(NO, assertMsg);
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
        if (targetNodeTree.hasChild && fromNodeTree.hasChild) {
            
            NSArray *aChilden = targetNodeTree.children;
            NSArray *bChilden = fromNodeTree.children;
            if ([targetNodeTree.viewName isEqualToString:SCROLL_VIEW]) { // 可滚动容器节点孩子需要排序
                aChilden = [self sortScrollIndexWithList:aChilden];
                bChilden = [self sortScrollIndexWithList:bChilden];
                if (aChilden.count && bChilden.count >= aChilden.count
                                    && fromNodeTree.renderFrame.size.height > fromNodeTree.renderFrame.size.width) { // 纵向列表 可滚动容器直接替换内容（keep原有节点个数）
                    NSMutableArray *targetChildren = [[[bChilden mutableCopy] subarrayWithRange:NSMakeRange(0, aChilden.count)] mutableCopy];;
                    for (KRTurboDisplayNode *node in targetChildren) {
                         node.parentTag = targetNodeTree.tag;
                    }
                    targetNodeTree.children = targetChildren;
                    return YES;
                }
            }
            
            int fromIndex = 0;
            for (int i = 0; i < aChilden.count; i++) {
                KRTurboDisplayNode *nextTargetNode = aChilden[i];
                KRTurboDisplayNode *nextFromNode = [self nextNodeForUpdateWithChildern:bChilden fromIndex:&fromIndex targetNode:nextTargetNode];
                if (nextFromNode) {
                    if ([self onlyUpdateWithTargetNodeTree:nextTargetNode fromNodeTree:nextFromNode]) {
                         hasUpdate = YES;
                     }
                }
                fromIndex++;
             }
        }
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


@end
