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

NS_ASSUME_NONNULL_BEGIN

/*** @brief Diff-DOM 模式枚举 */
typedef NS_ENUM(NSUInteger, KRDiffDOMMode) {
    KRNormalDiffDOM,            // 旧模式：不支持结构变化
    KRStructureAwareDiffDOM,    // 新模式：支持结构变化
};

/*** @brief Diff-View 模式枚举 */
typedef NS_ENUM(NSUInteger, KRDiffViewMode) {
    KRDelayedDiffView,      // 启用延迟 diff（经典模式）
    KRNormalDiffView,       // 不使用延迟 diff（经典模式）
};

/*** @brief 首屏 Diff 执行时机模式枚举 */
typedef NS_ENUM(NSUInteger, KRDiffExecuteMode) {
    KRNormalExecuteDiff,         // 系统默认时机：业务首帧渲染完成后立即执行（默认）
    KRSuspendExecuteDiff,        // 挂起diff：didInit静态挂起，由业务executeTurboDisplayDiff触发执行
};

/**
 * @brief TurboDisplay 全局配置类
 * 用于配置 TurboDisplay 的各种开关和参数
 * 业务可在 KuiklyRenderViewController 初始化时配置
 */
@interface KRTurboDisplayConfig : NSObject <NSCopying>

/** @brief Diff-DOM 模式 默认为 KRDiffDOMModeStructureAware（新模式，支持结构变化）*/
@property (nonatomic, assign) KRDiffDOMMode diffDOMMode;
@property (nonatomic, readonly) BOOL isStructureAwareDiffDOMEnabled;

/** @brief 延迟 Diff 模式 默认为 KRDelayedDiffModeDisabled（禁用，使用经典模式）*/
@property (nonatomic, assign) KRDiffViewMode diffViewMode;
@property (nonatomic, readonly) BOOL isDelayedDiffEnabled;

/** @brief 首屏Diff执行时机模式 默认为 KRNormalExecuteDiff（系统默认时机）
 *  @note 挂起模式下Diff在didInit静态挂起（早于一切Kotlin渲染与UI批次，免疫sync事件插队竞态；
 *        Config在init传入零时序依赖，framework/JS动态化两种加载模式下均可靠），
 *        数据就绪后由Kotlin侧executeTurboDisplayDiff()触发接管；无超时自动执行（业务忘调则页面停留缓存态） */
@property (nonatomic, assign) KRDiffExecuteMode diffExecuteMode;
@property (nonatomic, readonly) BOOL isSuspendDiffEnabled;

/** @brief 自动刷新 默认为 true（启用，使用经典模式）*/
@property (nonatomic, assign) BOOL autoUpdateTurboDisplay;
@property (nonatomic, readonly) BOOL isCloseAutoUpdateTurboDisplay;

/**
 * @brief 真实树持久更新开关 默认为 YES（启用）
 * @note 开启后：真实树会持续更新，支持 diff-DOM、强制刷新缓存，但有性能开销
 *       关闭后：性能更好，但失去 diff-DOM 和强制刷新缓存的能力
 *       特殊：TB 首屏懒加载期间无论开关状态如何都会更新真实树
 */
@property (nonatomic, assign) BOOL persistentRealTree;
@property (nonatomic, readonly) BOOL isPersistentRealTreeEnabled;

#pragma mark - 便捷配置方法

/**
 * @brief 启用 Diff-DOM 结构变化支持
 */
- (void)enableStructureAwareDiffDOM;

/**
 * @brief 禁用 Diff-DOM 结构变化支持（使用旧模式）
 */
- (void)disableStructureAwareDiffDOM;

/**
 * @brief 启用延迟 Diff
 */
- (void)enableDelayedDiff;

/**
 * @brief 禁用延迟 Diff（使用经典模式）
 */
- (void)disableDelayedDiff;

/**
 * @brief 启用自动更新
 */
- (void)enableAutoUpdateTurboDisplay;

/**
 * @brief 禁用自动更新
 */
- (void)closeAutoUpdateTurboDisplay;

/**
 * @brief 启用真实树持久更新（默认）
 */
- (void)enablePersistentRealTree;

/**
 * @brief 禁用真实树持久更新
 */
- (void)disablePersistentRealTree;

/**
 * @brief 启用挂起Diff（didInit静态挂起，由业务executeTurboDisplayDiff触发执行，无超时自动执行）
 */
- (void)enableSuspendDiff;

/**
 * @brief 禁用挂起Diff（恢复系统默认时机）
 */
- (void)disableSuspendDiff;

/**
 * @brief 重置为默认配置
 */
- (void)resetToDefault;

@end

NS_ASSUME_NONNULL_END
