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

NS_ASSUME_NONNULL_BEGIN

@interface KRTurboDisplayDiffPatch : NSObject
/**
 * @brief diff 两棵树进行差量更新到渲染器
 */
+ (void)diffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                oldNodeTree:(KRTurboDisplayNode * _Nullable)oldNodeTree
                                newNodeTree:(KRTurboDisplayNode *)newNodeTree;

/**
 * @brief 保留目标树结构，仅更新目标树属性信息
 * @param targetNodeTree 被更新的目标树
 * @param fromNodeTree 更新的来源树
 * @return 是否有发生更新
 */
+ (BOOL)onlyUpdateWithTargetNodeTree:(KRTurboDisplayNode *)targetNodeTree fromNodeTree:(KRTurboDisplayNode *)fromNodeTree;

@end

NS_ASSUME_NONNULL_END
