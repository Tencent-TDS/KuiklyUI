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
#import "KRLabel.h"
#import "KRView.h"

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
- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didStartWithFrame:(CGRect)frame;
- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didChangeWithFrame:(CGRect)frame;
- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didEndWithFrame:(CGRect)frame;
- (void)textSelectionHelperDidCancel:(KRTextSelectionHelper *)helper;
@end

@interface KRTextSelectionHelper : NSObject

@property (nonatomic, weak, nullable) id<KRTextSelectionHelperDelegate> delegate;
@property (nonatomic, weak, nullable) KRView *containerView;

- (instancetype)init;

- (void)startSelectionWithLabels:(NSArray<KRLabel *> *)labels
                   containerView:(KRView *)containerView;

- (void)selectWordAtPoint:(CGPoint)point;
- (void)selectAtPoint:(CGPoint)point type:(KRTextSelectionType)type;
- (void)selectAll;
- (void)endSelection;

- (NSArray<NSString *> *)getSelectedTexts;
- (NSArray<NSString *> *)getPreSelectionContent;
- (NSArray<NSString *> *)getPostSelectionContent;
- (CGRect)getSelectionFrame;

- (void)setSelectionColor:(UIColor * _Nullable)color;

#pragma mark - macOS Mouse Events

#if TARGET_OS_OSX
- (void)mouseDown:(NSEvent *)event inLabel:(KRLabel *)label localPoint:(NSPoint)localPoint;
- (void)mouseDraggedToPoint:(NSPoint)containerPoint;
- (void)mouseUp;
#endif

@end

NS_ASSUME_NONNULL_END
