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
#import <UIKit/UIKit.h>
#import "KRLabel.h"

NS_ASSUME_NONNULL_BEGIN

/// The helper for text selection.
@interface KRTextSelectionHelper : NSObject

/// The shared instance of the text selection helper.
+ (instancetype)sharedInstance;

/**
 * Start selection session with a list of labels.
 * @param labels The labels that participate in the selection, ordered visually.
 * @param containerView The view that contains all labels (and where anchors will be added).
 */
- (void)startSelectionWithLabels:(NSArray<KRLabel *> *)labels
                   containerView:(UIView *)containerView;

/**
 * Select word at specific point (e.g. from Long Press).
 * @param point Point in containerView coordinates.
 */
- (void)selectWordAtPoint:(CGPoint)point;

/**
 * Select all text across all labels.
 */
- (void)selectAll;

/**
 * End selection and remove anchors.
 */
- (void)endSelection;

/**
 * Check if a point is inside any anchor view's frame.
 * @param point Point in containerView coordinates.
 */
- (BOOL)isPointOnAnchor:(CGPoint)point;

@end

NS_ASSUME_NONNULL_END

