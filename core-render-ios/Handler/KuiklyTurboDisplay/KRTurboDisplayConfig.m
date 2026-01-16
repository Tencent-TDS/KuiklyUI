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

- (instancetype)init {
    if (self = [super init]) {
        [self resetToDefault];
    }
    return self;
}

- (void)resetToDefault {
    _diffDOMMode = KRNormalDiffDOM;       // 使用 非结构捕捉的 diff-DOM
    _diffViewMode = KRNormalDiffView;     // 使用 普通diff（View），非延迟diff（View）
    _autoUpdateTurboDisplay = YES;        // 开启自动刷新，会自动执行 diff-DOM
}

- (id)copyWithZone:(NSZone *)zone {
    KRTurboDisplayConfig *copy = [[KRTurboDisplayConfig allocWithZone:zone] init];
    copy.diffDOMMode = self.diffDOMMode;
    copy.diffViewMode = self.diffViewMode;
    copy.autoUpdateTurboDisplay = self.autoUpdateTurboDisplay;
    return copy;
}

#pragma mark - 开关状态读取

- (BOOL)isStructureAwareDiffDOMEnabled {
    return _diffDOMMode == KRStructureAwareDiffDOM;
}

- (BOOL)isDelayedDiffEnabled {
    return _diffViewMode == KRDelayedDiffView;
}

- (BOOL)isCloseAutoUpdateTurboDisplay {
    return _autoUpdateTurboDisplay == NO;
}

#pragma mark - 开关 getter/setter

- (void)enableStructureAwareDiffDOM {
    _diffDOMMode = KRStructureAwareDiffDOM;
}

- (void)disableStructureAwareDiffDOM {
    _diffDOMMode = KRNormalDiffDOM;
}

- (void)enableDelayedDiff {
    _diffViewMode = KRDelayedDiffView;
}

- (void)disableDelayedDiff {
    _diffViewMode = KRNormalDiffView;
}

- (void)enableAutoUpdateTurboDisplay {
    _autoUpdateTurboDisplay = YES;
}

- (void)closeAutoUpdateTurboDisplay {
    _autoUpdateTurboDisplay = NO;
}


@end
