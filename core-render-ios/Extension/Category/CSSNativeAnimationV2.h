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

#import "UIView+CSS.h"

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT BOOL KRParseNativeAnimationV2(
    NSArray<NSString *> *parts,
    NSString * _Nullable * _Nullable kind,
    NSArray<NSNumber *> * _Nullable * _Nullable values
);

FOUNDATION_EXPORT CGFloat KRNativeAnimationV2Progress(
    NSString *kind,
    NSArray<NSNumber *> *values,
    CGFloat fraction
);

FOUNDATION_EXPORT NSUInteger KRNativeAnimationV2TransformSampleCount(NSTimeInterval duration);

FOUNDATION_EXPORT BOOL KRPerformNativeAnimationV2(
    NSString *kind,
    NSArray<NSNumber *> *values,
    NSTimeInterval duration,
    NSTimeInterval delay,
    void (^animations)(void),
    void (^ _Nullable completion)(BOOL finished)
);

NS_ASSUME_NONNULL_END
