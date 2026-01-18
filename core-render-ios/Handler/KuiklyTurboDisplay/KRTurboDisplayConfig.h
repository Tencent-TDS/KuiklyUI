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
    KRDelayedDiffView,      // 禁用延迟 diff（经典模式）
    KRNormalDiffView,      // 禁用延迟 diff（经典模式）
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

/** @brief 自动刷新 默认为 true（启用，使用经典模式）*/
@property (nonatomic, assign) BOOL autoUpdateTurboDisplay;
@property (nonatomic, readonly) BOOL isCloseAutoUpdateTurboDisplay;

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
 * @brief 重置为默认配置
 */
- (void)resetToDefault;

@end

NS_ASSUME_NONNULL_END
