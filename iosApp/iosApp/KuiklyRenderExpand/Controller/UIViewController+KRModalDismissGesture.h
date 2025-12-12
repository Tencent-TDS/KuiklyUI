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

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Category for UIViewController to support modal dismiss gesture (swipe right to dismiss)
 */
@interface UIViewController (KRModalDismissGesture)

/**
 * Setup modal dismiss pan gesture
 * Call this method in viewDidLoad to enable swipe-to-dismiss gesture
 */
- (void)kr_setupModalDismissGesture;

/**
 * Remove modal dismiss pan gesture
 * Call this method when you want to disable the gesture
 */
- (void)kr_removeModalDismissGesture;

/**
 * Check if modal dismiss gesture is enabled
 */
- (BOOL)kr_isModalDismissGestureEnabled;

/**
 * Enable or disable modal dismiss gesture
 */
- (void)kr_setModalDismissGestureEnabled:(BOOL)enabled;

@end

NS_ASSUME_NONNULL_END
