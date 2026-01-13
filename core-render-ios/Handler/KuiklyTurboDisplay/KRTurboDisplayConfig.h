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

/**
 * @brief Diff-DOM 模式枚举
 * - KRDiffDOMModeLegacy: 旧模式，不支持结构变化（仅更新属性，不处理节点增删）
 * - KRDiffDOMModeStructureAware: 新模式，支持结构变化（属性更新 + 节点增删）
 */
typedef NS_ENUM(NSUInteger, KRDiffDOMMode) {
    KRDiffDOMModeLegacy,            // 旧模式：不支持结构变化
    KRDiffDOMModeStructureAware,    // 新模式：支持结构变化
};

/**
 * @brief 延迟 Diff 模式枚举
 * - KRDelayedDiffModeDisabled: 禁用延迟 diff（经典模式）
 * - KRDelayedDiffModeEnabled: 启用延迟 diff（分阶段执行：事件回放 -> 属性更新）
 */
typedef NS_ENUM(NSUInteger, KRDelayedDiffMode) {
    KRDelayedDiffModeDisabled,      // 禁用延迟 diff（经典模式）
    KRDelayedDiffModeEnabled,       // 启用延迟 diff（新机制）
};

/**
 * @brief TurboDisplay 全局配置类
 * @discussion 用于配置 TurboDisplay 的各种开关和参数
 *             业务可在 KuiklyRenderViewController 初始化时配置
 */
@interface KRTurboDisplayConfig : NSObject

#pragma mark - 单例

/**
 * @brief 获取全局配置单例
 */
+ (instancetype)sharedConfig;

#pragma mark - Diff-DOM 配置

/**
 * @brief Diff-DOM 模式
 * @note 默认为 KRDiffDOMModeStructureAware（新模式，支持结构变化）
 */
@property (nonatomic, assign) KRDiffDOMMode diffDOMMode;

/**
 * @brief 是否启用 Diff-DOM 结构变化支持
 * @note 便捷属性，等价于 diffDOMMode == KRDiffDOMModeStructureAware
 */
@property (nonatomic, readonly) BOOL isDiffDOMStructureAwareEnabled;

#pragma mark - 延迟 Diff 配置

/**
 * @brief 延迟 Diff 模式
 * @note 默认为 KRDelayedDiffModeDisabled（禁用，使用经典模式）
 */
@property (nonatomic, assign) KRDelayedDiffMode delayedDiffMode;

/**
 * @brief 是否启用延迟 Diff
 * @note 便捷属性，等价于 delayedDiffMode == KRDelayedDiffModeEnabled
 */
@property (nonatomic, readonly) BOOL isDelayedDiffEnabled;

#pragma mark - 便捷配置方法

/**
 * @brief 启用 Diff-DOM 结构变化支持
 */
- (void)enableDiffDOMStructureAware;

/**
 * @brief 禁用 Diff-DOM 结构变化支持（使用旧模式）
 */
- (void)disableDiffDOMStructureAware;

/**
 * @brief 启用延迟 Diff
 */
- (void)enableDelayedDiff;

/**
 * @brief 禁用延迟 Diff（使用经典模式）
 */
- (void)disableDelayedDiff;

/**
 * @brief 重置为默认配置
 */
- (void)resetToDefault;

@end

NS_ASSUME_NONNULL_END
