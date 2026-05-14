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

#import "KRTextSelectionHelper.h"
#import "KRTextSelectionAnchorView.h"
#import "KRLabel.h"
#import "KRTextMagnifierView.h"
#import "KRScrollView.h"
#import "KRLogModule.h"

#define KR_ANCHOR_TAG_LEFT 1001
#define KR_ANCHOR_TAG_RIGHT 1002


/// KVO context for observing contentOffset changes
static void *KRTextSelectionScrollObserverContext = &KRTextSelectionScrollObserverContext;
/// KVO context for observing containerView frame changes
static void *KRTextSelectionContainerFrameObserverContext = &KRTextSelectionContainerFrameObserverContext;

@interface KRTextSelectionHelper () <UIGestureRecognizerDelegate, UIScrollViewDelegate>

/// The labels that participate in the selection, ordered visually.
@property (nonatomic, strong) NSArray<KRLabel *> *labels;
/// The view that contains all labels (and where anchors will be added).
@property (nonatomic, weak) KRView *containerView;

/// The left anchor view.
@property (nonatomic, strong) KRTextSelectionAnchorView *leftAnchor;
/// The right anchor view.
@property (nonatomic, strong) KRTextSelectionAnchorView *rightAnchor;
/// The magnifier view.
@property (nonatomic, strong) KRTextMagnifierView *magnifierView;

/// The start label.
@property (nonatomic, weak) KRLabel *startLabel;
/// The start index.
@property (nonatomic, assign) NSInteger startIndex;
/// The end label.
@property (nonatomic, weak) KRLabel *endLabel;
/// The end index.
@property (nonatomic, assign) NSInteger endIndex;

/// Scroll observation for anchor position sync
@property (nonatomic, strong) NSHashTable<UIScrollView *> *observedScrollViews;

/// Selection highlight color
@property (nonatomic, strong) UIColor *selectionColor;
/// Cursor/anchor color
@property (nonatomic, strong) UIColor *cursorColor;

/// Flag to track if containerView frame observer is added
@property (nonatomic, assign) BOOL isObservingContainerFrame;

@end

@implementation KRTextSelectionHelper

+ (instancetype)sharedInstance {
    static KRTextSelectionHelper *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[KRTextSelectionHelper alloc] init];
    });
    return instance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        CGFloat anchorWidth = [KRTextSelectionAnchorView defaultAnchorWidth];
        CGFloat anchorHeight = [KRTextSelectionAnchorView defaultAnchorHeight];
        CGRect initialFrame = CGRectMake(0, 0, anchorWidth, anchorHeight);
        
        _leftAnchor = [[KRTextSelectionAnchorView alloc] initWithFrame:initialFrame];
        [_leftAnchor setIsTop:YES];
        _leftAnchor.tag = KR_ANCHOR_TAG_LEFT;
        
        _rightAnchor = [[KRTextSelectionAnchorView alloc] initWithFrame:initialFrame];
        [_rightAnchor setIsTop:NO];
        _rightAnchor.tag = KR_ANCHOR_TAG_RIGHT;
        
        [self setupPanGestureForAnchor:_leftAnchor];
        [self setupPanGestureForAnchor:_rightAnchor];
    }
    return self;
}

- (void)setupPanGestureForAnchor:(KRTextSelectionAnchorView *)anchor {
    UIPanGestureRecognizer *panGesture = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handlePan:)];
    [anchor addGestureRecognizer:panGesture];
    anchor.userInteractionEnabled = YES;
}

- (void)startSelectionWithLabels:(NSArray<KRLabel *> *)labels containerView:(KRView *)containerView {
    [self endSelection]; // Clear previous
    
    self.labels = labels;
    self.containerView = containerView;
    
    // Reset state
    self.startLabel = nil;
    self.endLabel = nil;
    self.startIndex = -1;
    self.endIndex = -1;
    
    // Start observing scroll events to keep anchors in sync
    [self collectAndObserveScrollViews];
    
    // Start observing containerView frame changes (e.g., screen rotation)
    [self observeContainerViewFrame];
    
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] startSelection labels:%lu container:%@",
                          (unsigned long)labels.count, NSStringFromClass([containerView class])]];
}

- (void)selectWordAtPoint:(CGPoint)point {
    [self selectAtPoint:point type:KRTextSelectionTypeWord];
}

- (void)selectAtPoint:(CGPoint)point type:(KRTextSelectionType)type {
    if (!self.labels || !self.containerView) {
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] selectAtPoint failed - labels:%@ container:%@",
                              self.labels ? @"exists" : @"nil", self.containerView ? @"exists" : @"nil"]];
        return;
    }
    
    // 1. Find touched label and index
    KRLabel *touchedLabel = nil;
    NSInteger charIndex = -1;
    
    for (KRLabel *label in self.labels) {
        CGPoint localPoint = [self.containerView convertPoint:point toView:label];
        if (CGRectContainsPoint(label.bounds, localPoint)) {
            charIndex = [label.textRender characterIndexForPoint:localPoint];
            if (charIndex >= 0 && charIndex < label.textRender.textStorage.length) {
                touchedLabel = label;
                break;
            }
        }
    }
    
    if (touchedLabel) {
        NSString *text = touchedLabel.textRender.textStorage.string;
        NSRange selectionRange;
        
        switch (type) {
            case KRTextSelectionTypeCharacter:
                // Select single character
                selectionRange = NSMakeRange(charIndex, 1);
                break;
            case KRTextSelectionTypeWord:
                // Expand to word
                selectionRange = [self rangeOfWordAtIndex:charIndex inString:text];
                break;
            case KRTextSelectionTypeParagraph:
                // Expand to paragraph/line
                selectionRange = [self rangeOfParagraphAtIndex:charIndex inString:text];
                break;
            case KRTextSelectionTypeSentence:
                // Expand to sentence
                selectionRange = [self rangeOfSentenceAtIndex:charIndex inString:text];
                break;
            default:
                selectionRange = NSMakeRange(charIndex, 1);
                break;
        }
        
        self.startLabel = touchedLabel;
        self.endLabel = touchedLabel;
        self.startIndex = selectionRange.location;
        self.endIndex = selectionRange.location + selectionRange.length;
        
        [self updateUI];
        
        // Notify delegate about selection start
        [self notifyDelegateDidStartSelection];
        
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] selectAtPoint:(%.1f,%.1f) type:%ld range:(%ld,%ld)",
                              point.x, point.y, (long)type, (long)selectionRange.location, (long)selectionRange.length]];
    }
}

- (void)selectAll {
    if (self.labels.count == 0) {
        [KRLogModule logInfo:@"[TextSelection] selectAll failed - no labels"];
        return;
    }
    
    self.startLabel = self.labels.firstObject;
    self.startIndex = 0;
    
    self.endLabel = self.labels.lastObject;
    self.endIndex = self.endLabel.textRender.textStorage.length;
    
    [self updateUI];
    
    // Notify delegate about selection start
    [self notifyDelegateDidStartSelection];
    
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] selectAll labels:%lu endIndex:%ld",
                          (unsigned long)self.labels.count, (long)self.endIndex]];
}

- (void)endSelection {
    // Check if there was an active selection before clearing
    BOOL hadSelection = (self.labels != nil && self.containerView != nil);
    if (hadSelection) {
        [KRLogModule logInfo:@"[TextSelection] endSelection"];
    }
    
    // Remove scroll observers first
    [self removeScrollViewObservers];
    
    // Remove containerView frame observer
    [self removeContainerViewFrameObserver];
    
    [self.leftAnchor removeFromSuperview];
    [self.rightAnchor removeFromSuperview];
    
    for (KRLabel *label in self.labels) {
        label.selectedRange = NSMakeRange(NSNotFound, 0);
    }
    
    self.labels = nil;
    self.containerView = nil;
    
    // Notify delegate about selection cancel
    if (hadSelection) {
        [self notifyDelegateDidCancelSelection];
    }
}

/// 游标触摸区域的扩展边距（便于用户触摸）
static const CGFloat kAnchorHitTestPadding = 20.0;

- (BOOL)isPointOnAnchor:(CGPoint)point {
    CGFloat padding = -kAnchorHitTestPadding;
    if (self.leftAnchor.superview && CGRectContainsPoint(CGRectInset(self.leftAnchor.frame, padding, padding), point)) {
        return YES;
    }
    if (self.rightAnchor.superview && CGRectContainsPoint(CGRectInset(self.rightAnchor.frame, padding, padding), point)) {
        return YES;
    }
    return NO;
}

#pragma mark - UI Update

- (void)updateUI {
    if (!self.startLabel || !self.endLabel || self.startIndex < 0 || self.endIndex < 0) return;
    
    // 1. Update Highlight
    BOOL selecting = NO;
    
    for (KRLabel *label in self.labels) {
        NSRange range = NSMakeRange(NSNotFound, 0);
        
        if (label == self.startLabel && label == self.endLabel) {
            range = NSMakeRange(self.startIndex, self.endIndex - self.startIndex);
            selecting = NO; // Done
        } else if (label == self.startLabel) {
            range = NSMakeRange(self.startIndex, label.textRender.textStorage.length - self.startIndex);
            selecting = YES;
        } else if (label == self.endLabel) {
            range = NSMakeRange(0, self.endIndex);
            selecting = NO;
        } else if (selecting) {
            range = NSMakeRange(0, label.textRender.textStorage.length);
        }
        
        label.selectedRange = range;
    }
    
    // 2. Update Anchors
    [self updateAnchor:self.leftAnchor forLabel:self.startLabel index:self.startIndex isStart:YES];
    [self updateAnchor:self.rightAnchor forLabel:self.endLabel index:self.endIndex isStart:NO];
}

- (void)updateAnchor:(KRTextSelectionAnchorView *)anchor forLabel:(KRLabel *)label index:(NSInteger)index isStart:(BOOL)isStart {
    if (!label || !self.containerView) return;
    
    if (anchor.superview != self.containerView) {
        [self.containerView addSubview:anchor];
    }
    
    // Ensure style matches role
    if (anchor.isTop != isStart) {
        [anchor setIsTop:isStart];
    }
    
    // 获取字符位置的 rect
    // isStart 为 YES 时，获取 index 位置字符的左边缘
    // isStart 为 NO 时，获取 index-1 位置字符的右边缘
    static const CGFloat kCursorRectWidth = 2.0;
    
    CGRect rect;
    CGFloat lineHeight = 0;
    
    if (isStart) {
        // 游标在 index 位置字符之前
        if (index >= label.textRender.textStorage.length && index > 0) {
            // 文本末尾，使用最后一个字符的右边缘
            NSRange lastCharRange = NSMakeRange(index - 1, 1);
            CGRect lastRect = [label.textRender boundingRectForCharacterRange:lastCharRange];
            rect = CGRectMake(CGRectGetMaxX(lastRect), lastRect.origin.y, kCursorRectWidth, lastRect.size.height);
            lineHeight = lastRect.size.height;
        } else {
            NSRange range = NSMakeRange(index, 1);
            CGRect charRect = [label.textRender boundingRectForCharacterRange:range];
            rect = CGRectMake(charRect.origin.x, charRect.origin.y, kCursorRectWidth, charRect.size.height);
            lineHeight = charRect.size.height;
        }
    } else {
        // 游标在 index-1 位置字符之后
        if (index == 0) {
            // 文本开头
            NSRange range = NSMakeRange(0, 1);
            CGRect charRect = [label.textRender boundingRectForCharacterRange:range];
            rect = CGRectMake(charRect.origin.x, charRect.origin.y, kCursorRectWidth, charRect.size.height);
            lineHeight = charRect.size.height;
        } else {
            NSRange range = NSMakeRange(index - 1, 1);
            CGRect charRect = [label.textRender boundingRectForCharacterRange:range];
            rect = CGRectMake(CGRectGetMaxX(charRect), charRect.origin.y, kCursorRectWidth, charRect.size.height);
            lineHeight = charRect.size.height;
        }
    }
    
    // Convert rect to container view
    CGRect globalRect = [label convertRect:rect toView:self.containerView];
    
    // 计算游标 frame
    CGFloat anchorWidth = [KRTextSelectionAnchorView defaultAnchorWidth];
    CGFloat capOffset = [KRTextSelectionAnchorView anchorCapOffset]; // 圆形头部超出文字的偏移量
    
    [anchor setAnchorLineHeight:lineHeight];
    
    if (isStart) {
        // 起始游标：圆形在顶部，竖线向下
        // 圆形 4/5 在文字上方，需要减去 capOffset
        anchor.frame = CGRectMake(globalRect.origin.x - anchorWidth / 2.0,
                                  globalRect.origin.y - capOffset,
                                  anchorWidth,
                                  lineHeight + capOffset);
    } else {
        // 结束游标：竖线在顶部，圆形在底部
        // y 从文字行顶部开始，高度包含行高加上 capOffset
        anchor.frame = CGRectMake(globalRect.origin.x - anchorWidth / 2.0,
                                  globalRect.origin.y,
                                  anchorWidth,
                                  lineHeight + capOffset);
    }
    
    [anchor setNeedsDisplay];
}

#pragma mark - Gesture

- (void)handlePan:(UIPanGestureRecognizer *)gesture {
    if (gesture.state == UIGestureRecognizerStateChanged) {
        CGPoint point = [gesture locationInView:self.containerView];
        
        // Find closest label - 遍历所有 label，找到距离最近的
        KRLabel *closestLabel = nil;
        CGFloat minDistance = MAXFLOAT;
        
        for (KRLabel *label in self.labels) {
            CGPoint localPoint = [self.containerView convertPoint:point toView:label];
            
            // 计算点到 label bounds 的距离
            CGFloat dist = [self distanceFromPoint:localPoint toRect:label.bounds];
            
            // 如果点在 bounds 内，距离为 0
            if (CGRectContainsPoint(label.bounds, localPoint)) {
                dist = 0;
            }
            
            if (dist < minDistance) {
                minDistance = dist;
                closestLabel = label;
                
                // 如果点正好在某个 label 内部，直接选择它
                if (dist == 0) {
                    break;
                }
            }
        }
        
        if (closestLabel) {
            CGPoint localPoint = [self.containerView convertPoint:point toView:closestLabel];
            NSInteger idx = [self insertionIndexForPoint:localPoint inLabel:closestLabel];
            
            // 无论选区是否变化，都更新放大镜位置
            [self showMagnifierViewWithTargetLabel:closestLabel point:point];
            
            BOOL selectionChanged = NO;
            
            if (gesture.view == self.leftAnchor) {
                // Dragging Start
                NSComparisonResult cmp = [self comparePositionLabel:closestLabel index:idx withLabel:self.endLabel index:self.endIndex];
                
                if (cmp == NSOrderedAscending) {
                    if (self.startLabel != closestLabel || self.startIndex != idx) {
                        self.startLabel = closestLabel;
                        self.startIndex = idx;
                        selectionChanged = YES;
                    }
                } else if (cmp == NSOrderedDescending) {
                    // Swap: 拖动起始游标超过了结束游标
                    self.startLabel = self.endLabel;
                    self.startIndex = self.endIndex;
                    
                    self.endLabel = closestLabel;
                    self.endIndex = idx;
                    
                    KRTextSelectionAnchorView *temp = self.leftAnchor;
                    self.leftAnchor = self.rightAnchor;
                    self.rightAnchor = temp;
                    selectionChanged = YES;
                } else {
                    // same, ignore
                }
                
            } else {
                // Dragging End
                NSComparisonResult cmp = [self comparePositionLabel:closestLabel index:idx withLabel:self.startLabel index:self.startIndex];
                
                if (cmp == NSOrderedDescending) {
                    if (self.endLabel != closestLabel || self.endIndex != idx) {
                        self.endLabel = closestLabel;
                        self.endIndex = idx;
                        selectionChanged = YES;
                    }
                } else if (cmp == NSOrderedAscending) {
                    // Swap: 拖动结束游标超过了起始游标
                    self.endLabel = self.startLabel;
                    self.endIndex = self.startIndex;
                    
                    self.startLabel = closestLabel;
                    self.startIndex = idx;
                    
                    KRTextSelectionAnchorView *temp = self.leftAnchor;
                    self.leftAnchor = self.rightAnchor;
                    self.rightAnchor = temp;
                    selectionChanged = YES;
                } else {
                    // same, ignore
                }
            }
            
            if (selectionChanged) {
                [self updateUI];
                // Notify delegate about selection change
                [self notifyDelegateDidChangeSelection];
            }
        }
    }
    
    if (gesture.state == UIGestureRecognizerStateEnded || gesture.state == UIGestureRecognizerStateCancelled) {
        [self removeMagnifierView];
        // Notify delegate about selection end
        [self notifyDelegateDidEndSelection];
        
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] panGesture ended startIdx:%ld endIdx:%ld",
                              (long)self.startIndex, (long)self.endIndex]];
    }
}

#pragma mark - Magnifier Views

/// 放大镜视图的尺寸
static const CGFloat kMagnifierViewSize = 80.0;
/// 放大镜距离触摸点的垂直偏移
static const CGFloat kMagnifierVerticalOffset = 60.0;

- (void)showMagnifierViewWithTargetLabel:(KRLabel *)label point:(CGPoint)point {
    if (!self.magnifierView) {
        CGRect magnifierFrame = CGRectMake(0, 0, kMagnifierViewSize, kMagnifierViewSize);
        self.magnifierView = [[KRTextMagnifierView alloc] initWithFrame:magnifierFrame];
    }
    self.magnifierView.viewToMagnify = self.containerView;
    
    UIWindow *window = label.window;
    if (window && self.magnifierView.superview != window) {
        [window addSubview:self.magnifierView];
    }
    
    // 更新放大镜位置（显示在触摸点上方）
    CGPoint windowPoint = [self.containerView convertPoint:point toView:window];
    self.magnifierView.center = CGPointMake(windowPoint.x, windowPoint.y - kMagnifierVerticalOffset);
    self.magnifierView.touchPoint = point;
}

- (void)removeMagnifierView {
    [self.magnifierView removeFromSuperview];
    self.magnifierView = nil;
}

- (CGFloat)distanceFromPoint:(CGPoint)p toRect:(CGRect)rect {
    CGFloat dx = MAX(CGRectGetMinX(rect) - p.x, 0);
    if (p.x > CGRectGetMaxX(rect)) dx = p.x - CGRectGetMaxX(rect);
    
    CGFloat dy = MAX(CGRectGetMinY(rect) - p.y, 0);
    if (p.y > CGRectGetMaxY(rect)) dy = p.y - CGRectGetMaxY(rect);
    
    return sqrt(dx*dx + dy*dy);
}

- (NSInteger)insertionIndexForPoint:(CGPoint)point inLabel:(KRLabel *)label {
    // 确保 textRender.size 与 label 的实际大小一致
    // textRender.size 只在 drawTextInRect: 时设置，可能不是最新值
    if (!CGSizeEqualToSize(label.textRender.size, label.bounds.size)) {
        label.textRender.size = label.bounds.size;
    }
    
    NSLayoutManager *lm = label.textRender.layoutManager;
    NSTextContainer *tc = label.textRender.textContainer;
    NSUInteger textLength = label.textRender.textStorage.length;
    
    if (textLength == 0) {
        return 0;
    }
    
    // 只限制 x 坐标在 label 宽度范围内，y 坐标不限制
    // 让 characterIndexForPoint: 自己决定返回哪个字符
    CGPoint adjustedPoint = point;
    if (adjustedPoint.x < 0) {
        adjustedPoint.x = 0;
    } else if (adjustedPoint.x > label.bounds.size.width) {
        adjustedPoint.x = label.bounds.size.width;
    }
    
    CGFloat fraction = 0;
    NSUInteger index = [lm characterIndexForPoint:adjustedPoint inTextContainer:tc fractionOfDistanceBetweenInsertionPoints:&fraction];
    
    // 边界检查
    if (index >= textLength) {
        return textLength;
    }
    
    // 根据 fraction 决定插入点在字符前还是后
    if (fraction > 0.5) {
        return MIN(index + 1, textLength);
    }
    return index;
}

- (NSComparisonResult)comparePositionLabel:(KRLabel *)l1 index:(NSInteger)idx1 withLabel:(KRLabel *)l2 index:(NSInteger)idx2 {
    if (l1 == l2) {
        if (idx1 < idx2) return NSOrderedAscending;
        if (idx1 > idx2) return NSOrderedDescending;
        return NSOrderedSame;
    }
    
    NSInteger i1 = [self.labels indexOfObject:l1];
    NSInteger i2 = [self.labels indexOfObject:l2];
    
    if (i1 < i2) return NSOrderedAscending;
    if (i1 > i2) return NSOrderedDescending;
    return NSOrderedSame;
}

#pragma mark - Helpers

- (NSRange)rangeOfWordAtIndex:(NSInteger)index inString:(NSString *)string {
    if (index < 0 || index >= string.length) return NSMakeRange(0, 0);
    
    // Simple whitespace based
    __block NSRange result = NSMakeRange(index, 1);
    
    [string enumerateSubstringsInRange:NSMakeRange(0, string.length)
                               options:NSStringEnumerationByWords
                            usingBlock:^(NSString * _Nullable substring,
                                         NSRange substringRange,
                                         NSRange enclosingRange,
                                         BOOL * _Nonnull stop) {
        if (NSLocationInRange(index, substringRange)) {
            result = substringRange;
            *stop = YES;
        }
    }];
    
    return result;
}

- (NSRange)rangeOfParagraphAtIndex:(NSInteger)index inString:(NSString *)string {
    if (index < 0 || index >= string.length) return NSMakeRange(0, string.length);
    
    // Find paragraph boundaries (line breaks)
    __block NSRange result = NSMakeRange(0, string.length);
    
    [string enumerateSubstringsInRange:NSMakeRange(0, string.length)
                               options:NSStringEnumerationByParagraphs
                            usingBlock:^(NSString * _Nullable substring,
                                         NSRange substringRange,
                                         NSRange enclosingRange,
                                         BOOL * _Nonnull stop) {
        if (NSLocationInRange(index, substringRange) || 
            (index == substringRange.location + substringRange.length && index == string.length)) {
            result = substringRange;
            *stop = YES;
        }
    }];
    
    return result;
}

- (NSRange)rangeOfSentenceAtIndex:(NSInteger)index inString:(NSString *)string {
    if (index < 0 || index >= string.length) return NSMakeRange(0, string.length);
    
    // Find sentence boundaries using NSStringEnumerationBySentences
    __block NSRange result = NSMakeRange(0, string.length);
    __block BOOL found = NO;
    
    [string enumerateSubstringsInRange:NSMakeRange(0, string.length)
                               options:NSStringEnumerationBySentences
                            usingBlock:^(NSString * _Nullable substring,
                                         NSRange substringRange,
                                         NSRange enclosingRange,
                                         BOOL * _Nonnull stop) {
        if (NSLocationInRange(index, substringRange) || 
            (index == substringRange.location + substringRange.length && index == string.length)) {
            result = substringRange;
            found = YES;
            *stop = YES;
        }
    }];
    
    // If no sentence boundary found (e.g., text without punctuation), return entire string
    if (!found) {
        result = NSMakeRange(0, string.length);
    }
    
    return result;
}

#pragma mark - Public Methods

- (NSArray<NSString *> *)getSelectedTexts {
    if (!self.startLabel || !self.endLabel || self.startIndex < 0 || self.endIndex < 0) {
        return @[];
    }
    
    NSMutableArray<NSString *> *texts = [NSMutableArray array];
    BOOL collecting = NO;
    
    for (KRLabel *label in self.labels) {
        if (label == self.startLabel && label == self.endLabel) {
            // Single label selection
            NSString *text = label.textRender.textStorage.string;
            NSRange range = NSMakeRange(self.startIndex, self.endIndex - self.startIndex);
            if (range.location + range.length <= text.length) {
                [texts addObject:[text substringWithRange:range]];
            }
            break;
        } else if (label == self.startLabel) {
            // Start of multi-label selection
            NSString *text = label.textRender.textStorage.string;
            if (self.startIndex < text.length) {
                [texts addObject:[text substringFromIndex:self.startIndex]];
            }
            collecting = YES;
        } else if (label == self.endLabel) {
            // End of multi-label selection
            NSString *text = label.textRender.textStorage.string;
            if (self.endIndex <= text.length) {
                [texts addObject:[text substringToIndex:self.endIndex]];
            }
            collecting = NO;
            break;
        } else if (collecting) {
            // Middle labels - select all text
            NSString *text = label.textRender.textStorage.string;
            if (text.length > 0) {
                [texts addObject:text];
            }
        }
    }
    
    return texts;
}

- (NSArray<NSString *> *)getPreSelectionContent {
    if (!self.startLabel || self.startIndex < 0) {
        return @[];
    }
    
    NSMutableArray<NSString *> *preContent = [NSMutableArray array];
    
    // Find index of startLabel
    NSInteger startLabelIndex = [self.labels indexOfObject:self.startLabel];
    if (startLabelIndex == NSNotFound) {
        return @[];
    }
    
    // Add previous label's text if exists
    if (startLabelIndex > 0) {
        KRLabel *previousLabel = self.labels[startLabelIndex - 1];
        NSString *previousText = previousLabel.textRender.textStorage.string;
        [preContent addObject:previousText ?: @""];
    }
    
    // Add text before selection in start label
    // According to requirement b): if selection starts at index 0 (covers from beginning), this should be ""
    NSString *startLabelText = self.startLabel.textRender.textStorage.string;
    if (self.startIndex > 0 && self.startIndex <= startLabelText.length) {
        [preContent addObject:[startLabelText substringToIndex:self.startIndex]];
    } else {
        // Selection starts at beginning, so preContent's last element is ""
        [preContent addObject:@""];
    }
    
    return preContent;
}

- (NSArray<NSString *> *)getPostSelectionContent {
    if (!self.endLabel || self.endIndex < 0) {
        return @[];
    }
    
    NSMutableArray<NSString *> *postContent = [NSMutableArray array];
    
    // Find index of endLabel
    NSInteger endLabelIndex = [self.labels indexOfObject:self.endLabel];
    if (endLabelIndex == NSNotFound) {
        return @[];
    }
    
    // Add text after selection in end label
    // According to requirement b): if selection ends at end of text (covers to end), this should be ""
    NSString *endLabelText = self.endLabel.textRender.textStorage.string;
    if (self.endIndex < endLabelText.length) {
        [postContent addObject:[endLabelText substringFromIndex:self.endIndex]];
    } else {
        // Selection ends at end, so postContent's first element is ""
        [postContent addObject:@""];
    }
    
    // Add next label's text if exists
    if (endLabelIndex < self.labels.count - 1) {
        KRLabel *nextLabel = self.labels[endLabelIndex + 1];
        NSString *nextText = nextLabel.textRender.textStorage.string;
        [postContent addObject:nextText ?: @""];
    }
    
    return postContent;
}

- (CGRect)getSelectionFrame {
    if (!self.startLabel || !self.endLabel || !self.containerView) {
        return CGRectZero;
    }
    
    CGRect unionRect = CGRectZero;
    BOOL firstRect = YES;
    BOOL selecting = NO;
    
    for (KRLabel *label in self.labels) {
        NSRange range = NSMakeRange(NSNotFound, 0);
        
        if (label == self.startLabel && label == self.endLabel) {
            range = NSMakeRange(self.startIndex, self.endIndex - self.startIndex);
        } else if (label == self.startLabel) {
            range = NSMakeRange(self.startIndex, label.textRender.textStorage.length - self.startIndex);
            selecting = YES;
        } else if (label == self.endLabel) {
            range = NSMakeRange(0, self.endIndex);
            selecting = NO;
        } else if (selecting) {
            range = NSMakeRange(0, label.textRender.textStorage.length);
        }
        
        if (range.location != NSNotFound && range.length > 0) {
            CGRect labelRect = [label.textRender boundingRectForCharacterRange:range];
            CGRect containerRect = [label convertRect:labelRect toView:self.containerView];
            
            if (firstRect) {
                unionRect = containerRect;
                firstRect = NO;
            } else {
                unionRect = CGRectUnion(unionRect, containerRect);
            }
        }
        
        if (label == self.endLabel) {
            break;
        }
    }
    
    return unionRect;
}

- (void)setSelectionColor:(UIColor *)color {
    _selectionColor = color;
    // Apply to all labels
    for (KRLabel *label in self.labels) {
        label.selectionColor = color;
    }
}

- (void)setCursorColor:(UIColor *)color {
    _cursorColor = color;
    // Apply to anchors
    [self.leftAnchor setColor:color];
    [self.rightAnchor setColor:color];
}

#pragma mark - Delegate Notifications

- (void)notifyDelegateDidStartSelection {
    if ([self.delegate respondsToSelector:@selector(textSelectionHelper:didStartWithFrame:)]) {
        CGRect frame = [self getSelectionFrame];
        [self.delegate textSelectionHelper:self didStartWithFrame:frame];
    }
}

- (void)notifyDelegateDidChangeSelection {
    if ([self.delegate respondsToSelector:@selector(textSelectionHelper:didChangeWithFrame:)]) {
        CGRect frame = [self getSelectionFrame];
        [self.delegate textSelectionHelper:self didChangeWithFrame:frame];
    }
}

- (void)notifyDelegateDidEndSelection {
    if ([self.delegate respondsToSelector:@selector(textSelectionHelper:didEndWithFrame:)]) {
        CGRect frame = [self getSelectionFrame];
        [self.delegate textSelectionHelper:self didEndWithFrame:frame];
    }
}

- (void)notifyDelegateDidCancelSelection {
    if ([self.delegate respondsToSelector:@selector(textSelectionHelperDidCancel:)]) {
        [self.delegate textSelectionHelperDidCancel:self];
    }
}

#pragma mark - Scroll Observation

/// Collect all UIScrollView ancestors of the labels and start observing their scroll events
- (void)collectAndObserveScrollViews {
    if (!self.labels || self.labels.count == 0) return;
    
    [self removeScrollViewObservers]; // Clear previous observers
    self.observedScrollViews = [NSHashTable weakObjectsHashTable];
    
    // Traverse view hierarchy for each label and collect all UIScrollView ancestors
    for (KRLabel *label in self.labels) {
        UIView *view = label.superview;
        while (view) {
            if ([view isKindOfClass:[UIScrollView class]]) {
                UIScrollView *scrollView = (UIScrollView *)view;
                
                // Avoid duplicate observation
                if (![self.observedScrollViews containsObject:scrollView]) {
                    [self.observedScrollViews addObject:scrollView];
                    
                    // Use KRScrollView's delegate mechanism if available, otherwise use KVO
                    if ([scrollView isKindOfClass:[KRScrollView class]]) {
                        [(KRScrollView *)scrollView addScrollViewDelegate:self];
                    } else {
                        [scrollView addObserver:self
                                     forKeyPath:@"contentOffset"
                                        options:NSKeyValueObservingOptionNew
                                        context:KRTextSelectionScrollObserverContext];
                    }
                }
            }
            view = view.superview;
        }
    }
}

/// Remove all scroll observers
- (void)removeScrollViewObservers {
    if (!self.observedScrollViews) return;
    
    for (UIScrollView *scrollView in self.observedScrollViews) {
        if ([scrollView isKindOfClass:[KRScrollView class]]) {
            [(KRScrollView *)scrollView removeScrollViewDelegate:self];
        } else {
            @try {
                [scrollView removeObserver:self forKeyPath:@"contentOffset" context:KRTextSelectionScrollObserverContext];
            } @catch (NSException *exception) {
                // Observer was already removed or never added
            }
        }
    }
    
    self.observedScrollViews = nil;
}

#pragma mark - UIScrollViewDelegate (for KRScrollView)

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    // Update anchor positions when scroll content changes
    [self updateUI];
}

#pragma mark - KVO (for generic UIScrollView)

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary<NSKeyValueChangeKey,id> *)change
                       context:(void *)context {
    if (context == KRTextSelectionScrollObserverContext) {
        // Update anchor positions when scroll content changes
        [self updateUI];
    } else if (context == KRTextSelectionContainerFrameObserverContext) {
        // Clear selection when containerView bounds changes (e.g., screen rotation)
        [self.containerView kr_cleanupTextSelection];
    } else {
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
}

#pragma mark - Container Frame Observation

/// Start observing containerView frame changes
- (void)observeContainerViewFrame {
    if (!self.containerView) return;
    if (self.isObservingContainerFrame) return; // Already observing
    
    [self.containerView addObserver:self
                         forKeyPath:@"layer.bounds"
                            options:NSKeyValueObservingOptionNew
                            context:KRTextSelectionContainerFrameObserverContext];
    self.isObservingContainerFrame = YES;
}

/// Remove containerView frame observer
- (void)removeContainerViewFrameObserver {
    if (!self.containerView || !self.isObservingContainerFrame) return;
    @try {
        [self.containerView removeObserver:self
                                forKeyPath:@"layer.bounds"
                                   context:KRTextSelectionContainerFrameObserverContext];
    } @catch (NSException *exception) {
        // Observer was already removed or never added
    }
    self.isObservingContainerFrame = NO;
}

@end

