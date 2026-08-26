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

#import "CSSNativeAnimationV2.h"

static CGFloat KRCubicBezierCoordinate(CGFloat t, CGFloat first, CGFloat second) {
    CGFloat oneMinusT = 1.0 - t;
    return 3.0 * oneMinusT * oneMinusT * t * first
        + 3.0 * oneMinusT * t * t * second
        + t * t * t;
}

CGFloat KRNativeAnimationV2Progress(
    NSString *kind,
    NSArray<NSNumber *> *values,
    CGFloat fraction
) {
    fraction = MIN(1.0, MAX(0.0, fraction));
    if (fraction <= 0.0) return 0.0;
    if (fraction >= 1.0 || [kind isEqualToString:@"snap"]) return 1.0;
    if (![kind isEqualToString:@"cubic"] || values.count != 4) return fraction;

    CGFloat lower = 0.0;
    CGFloat upper = 1.0;
    CGFloat parameter = fraction;
    for (NSUInteger iteration = 0; iteration < 16; iteration++) {
        CGFloat x = KRCubicBezierCoordinate(
            parameter,
            values[0].doubleValue,
            values[2].doubleValue
        );
        if (x < fraction) {
            lower = parameter;
        } else {
            upper = parameter;
        }
        parameter = (lower + upper) * 0.5;
    }
    return KRCubicBezierCoordinate(
        parameter,
        values[1].doubleValue,
        values[3].doubleValue
    );
}

NSUInteger KRNativeAnimationV2TransformSampleCount(NSTimeInterval duration) {
    CGFloat framesPerSecond = UIScreen.mainScreen.maximumFramesPerSecond;
    NSUInteger samples = (NSUInteger)ceil(MAX(duration, 0.0) * MAX(framesPerSecond, 60.0));
    return MIN(240, MAX(2, samples));
}

BOOL KRParseNativeAnimationV2(
    NSArray<NSString *> *parts,
    NSString **kind,
    NSArray<NSNumber *> **values
) {
    for (NSString *part in parts) {
        if (![part hasPrefix:@"v2,"]) {
            continue;
        }
        NSArray<NSString *> *payload = [part componentsSeparatedByString:@","];
        if (payload.count < 2) {
            continue;
        }
        if (kind) {
            *kind = payload[1];
        }
        if (values) {
            NSMutableArray<NSNumber *> *parsedValues = [NSMutableArray array];
            for (NSUInteger index = 2; index < payload.count; index++) {
                [parsedValues addObject:@([payload[index] doubleValue])];
            }
            *values = parsedValues;
        }
        return YES;
    }
    return NO;
}

BOOL KRPerformNativeAnimationV2(
    NSString *kind,
    NSArray<NSNumber *> *values,
    NSTimeInterval duration,
    NSTimeInterval delay,
    void (^animations)(void),
    void (^completion)(BOOL finished)
) {
#if TARGET_OS_OSX
    return NO;
#else
    if ([kind isEqualToString:@"snap"]) {
        [UIView animateWithDuration:0
                             delay:delay
                           options:UIViewAnimationOptionAllowUserInteraction
                        animations:animations
                        completion:completion];
        return YES;
    }

    id<UITimingCurveProvider> timingParameters = nil;
    if ([kind isEqualToString:@"cubic"] && values.count == 4) {
        timingParameters = [[UICubicTimingParameters alloc]
            initWithControlPoint1:CGPointMake(values[0].doubleValue, values[1].doubleValue)
                    controlPoint2:CGPointMake(values[2].doubleValue, values[3].doubleValue)];
    }
    if (!timingParameters) {
        return NO;
    }

    UIViewPropertyAnimator *propertyAnimator = [[UIViewPropertyAnimator alloc]
        initWithDuration:duration timingParameters:timingParameters];
    [propertyAnimator addAnimations:animations];
    [propertyAnimator addCompletion:^(UIViewAnimatingPosition finalPosition) {
        if (completion) {
            completion(finalPosition == UIViewAnimatingPositionEnd);
        }
    }];
    if (delay > 0) {
        [propertyAnimator startAnimationAfterDelay:delay];
    } else {
        [propertyAnimator startAnimation];
    }
    return YES;
#endif
}
