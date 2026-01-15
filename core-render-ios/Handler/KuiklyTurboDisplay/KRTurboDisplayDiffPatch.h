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

/**
 * @brief 第二次 diff 模式枚举
 * - KRSecondDiffModeClassic: 经典模式（旧机制），同步执行完整 diff
 * - KRSecondDiffModeDelayed: 延迟模式（新机制），分阶段执行：事件回放 -> 等待渲染指令 -> 属性更新
 */
typedef enum : NSUInteger {
    KRSecondDiffModeClassic,             // 经典模式：同步执行完整 diff（旧机制）
    KRSecondDiffModeDelayed,             // 延迟模式：分阶段执行 diff（新机制）
} KRSecondDiffMode;

@interface KRTurboDisplayDiffPatch : NSObject

#pragma mark - 全局开关
/**
 * @brief 全局开关：控制第二次 diff 使用经典模式还是延迟模式
 * @note 默认为 KRSecondDiffModeClassic（经典模式）
 */
@property (class, nonatomic, assign) KRSecondDiffMode secondDiffMode;

#pragma mark - TB首屏 Diff（经典模式，保持旧名称）

/**
 * @brief diff 两棵树进行差量更新到渲染器（TB首屏使用，经典模式）
 */
+ (void)diffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                oldNodeTree:(KRTurboDisplayNode * _Nullable)oldNodeTree
                                newNodeTree:(KRTurboDisplayNode *)newNodeTree;

#pragma mark - 第二次 Diff（业务首屏）

/**
 * @brief 经典 Diff（旧机制）：同步执行完整的 diff，包括 Tag 置换、事件回放、属性更新
 * @note 此方法与旧版 KuiklyUI 的 diff 行为完全一致，保持旧名称
 */
+ (void)diffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                oldNodeTree:(KRTurboDisplayNode * _Nullable)oldNodeTree
                                newNodeTree:(KRTurboDisplayNode *)newNodeTree
                                 diffPolicy:(KRFirstScreenDiffPolicy)diffPolicy;

/**
 * @brief 延迟 Diff（新机制）：当前帧执行事件回放，渲染指令延迟到跨端侧渲染指令全部到达后执行
 * @note 分为三个阶段：
 *       阶段1：事件回放 + 事件绑定（不执行 Tag 置换）
 *       阶段2：等待 context 队列渲染指令到达
 *       阶段3：同步执行 Tag 置换 + 属性更新
 */
+ (void)delayedDiffPatchToRenderingWithRenderLayer:(id<KuiklyRenderLayerProtocol>)renderLayer
                                       oldNodeTree:(KRTurboDisplayNode * _Nullable)oldNodeTree
                                       newNodeTree:(KRTurboDisplayNode *)newNodeTree
                                        completion:(dispatch_block_t _Nullable)completion;

/**
 * @brief 保留目标树结构，仅更新目标树属性信息
 * @param targetNodeTree 被更新的目标树
 * @param fromNodeTree 更新的来源树
 * @return 是否有发生更新
 */
+ (BOOL)onlyUpdateWithTargetNodeTree:(KRTurboDisplayNode *)targetNodeTree fromNodeTree:(KRTurboDisplayNode *)fromNodeTree;

@end

NS_ASSUME_NONNULL_END
