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

#import <Foundation/Foundation.h>
#import "KRTurboDisplayNode.h"
#import "KuiklyRenderLayerProtocol.h"
#import "KRTurboDisplayConfig.h"

NS_ASSUME_NONNULL_BEGIN

typedef enum : NSUInteger {
    KRCacheFirstScreenDiff,              // TB 首屏diff-view，view创建 + 缓存Prop设置 + 设置临时事件callback
    KRRealFirstScreenDiffEventReplay,    // 业务首屏优先执行事件回放
    KRRealFirstScreenDiffPropUpdate,     // 业务首屏事件回放完成，执行真正的diff-view，更新页面首屏。
} KRFirstScreenDiffPolicy;


@interface KRTurboDisplayDiffPatch : NSObject
/** TB 首屏 Diff（Diff-View） */
+ (void)diffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                oldNodeTree:(KRTurboDisplayNode * _Nullable)oldNodeTree
                                newNodeTree:(KRTurboDisplayNode *)newNodeTree;

/** 延迟 Diff（新增 config 参数） */
+ (void)delayedDiffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                       oldNodeTree:(KRTurboDisplayNode * _Nullable)oldNodeTree
                                       newNodeTree:(KRTurboDisplayNode *)newNodeTree
                                        completion:(dispatch_block_t _Nullable)completion;

+ (void)diffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                oldNodeTree:(KRTurboDisplayNode * _Nullable)oldNodeTree
                                newNodeTree:(KRTurboDisplayNode *)newNodeTree
                                 diffPolicy:(KRFirstScreenDiffPolicy)diffPolicy;

/** Diff-DOM（新增 config 参数） */
+ (BOOL)onlyUpdateWithTargetNodeTree:(KRTurboDisplayNode *)targetNodeTree
                        fromNodeTree:(KRTurboDisplayNode *)fromNodeTree
                              config:(KRTurboDisplayConfig *)config;

@end

NS_ASSUME_NONNULL_END
