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

#import "KRTurboDisplayConfig.h"

@implementation KRTurboDisplayConfig

#pragma mark - 单例

+ (instancetype)sharedConfig {
    static KRTurboDisplayConfig *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[KRTurboDisplayConfig alloc] init];
    });
    return instance;
}

- (instancetype)init {
    if (self = [super init]) {
        [self resetToDefault];
    }
    return self;
}

#pragma mark - 便捷属性

- (BOOL)isDiffDOMStructureAwareEnabled {
    return _diffDOMMode == KRDiffDOMModeStructureAware;
}

- (BOOL)isDelayedDiffEnabled {
    return _delayedDiffMode == KRDelayedDiffModeEnabled;
}

#pragma mark - 便捷配置方法

- (void)enableDiffDOMStructureAware {
    _diffDOMMode = KRDiffDOMModeStructureAware;
}

- (void)disableDiffDOMStructureAware {
    _diffDOMMode = KRDiffDOMModeLegacy;
}

- (void)enableDelayedDiff {
    _delayedDiffMode = KRDelayedDiffModeEnabled;
}

- (void)disableDelayedDiff {
    _delayedDiffMode = KRDelayedDiffModeDisabled;
}

- (void)resetToDefault {
    // 默认配置：
    // - Diff-DOM：启用结构变化支持（新模式）
    // - 延迟 Diff：禁用（经典模式）
    _diffDOMMode = KRDiffDOMModeStructureAware;
    _delayedDiffMode = KRDelayedDiffModeDisabled;
}

@end
