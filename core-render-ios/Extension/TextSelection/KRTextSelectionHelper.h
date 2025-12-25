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

@class KRTextSelectionHelper;

/// Selection type enum (matches DivView.kt SelectionType)
typedef NS_ENUM(NSInteger, KRTextSelectionType) {
    KRTextSelectionTypeCharacter = 0,
    KRTextSelectionTypeWord = 1,
    KRTextSelectionTypeParagraph = 2,
    KRTextSelectionTypeSentence = 3
};

/// Delegate protocol for text selection events
@protocol KRTextSelectionHelperDelegate <NSObject>
@optional
/**
 * Called when text selection starts.
 * @param helper The selection helper instance.
 * @param frame The bounding frame of the selection in container coordinates.
 */
- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didStartWithFrame:(CGRect)frame;

/**
 * Called when text selection changes (anchor dragging).
 * @param helper The selection helper instance.
 * @param frame The bounding frame of the selection in container coordinates.
 */
- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didChangeWithFrame:(CGRect)frame;

/**
 * Called when text selection ends (anchor released).
 * @param helper The selection helper instance.
 * @param frame The bounding frame of the selection in container coordinates.
 */
- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didEndWithFrame:(CGRect)frame;

/**
 * Called when text selection is cancelled.
 * @param helper The selection helper instance.
 */
- (void)textSelectionHelperDidCancel:(KRTextSelectionHelper *)helper;

@end

/// The helper for text selection.
@interface KRTextSelectionHelper : NSObject

/// The delegate for selection events.
@property (nonatomic, weak, nullable) id<KRTextSelectionHelperDelegate> delegate;

/// Create a new instance (for per-container use).
- (instancetype)init;

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
 * Select at specific point with selection type.
 * @param point Point in containerView coordinates.
 * @param type The selection type (character, word, paragraph).
 */
- (void)selectAtPoint:(CGPoint)point type:(KRTextSelectionType)type;

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

/**
 * Get the selected text content from all selected labels.
 * @return An array of selected text strings.
 */
- (NSArray<NSString *> *)getSelectedTexts;

/**
 * Get the bounding frame of the current selection.
 * @return The bounding frame in container coordinates.
 */
- (CGRect)getSelectionFrame;

/**
 * Set the selection highlight color.
 * @param color The background color for text selection.
 */
- (void)setSelectionColor:(UIColor * _Nullable)color;

/**
 * Set the cursor/anchor color.
 * @param color The color for selection anchors.
 */
- (void)setCursorColor:(UIColor * _Nullable)color;

@end

NS_ASSUME_NONNULL_END

