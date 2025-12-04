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

#define KR_ANCHOR_TAG_LEFT 1001
#define KR_ANCHOR_TAG_RIGHT 1002

@interface KRTextSelectionHelper () <UIGestureRecognizerDelegate>

@property (nonatomic, strong) NSArray<KRLabel *> *labels;
@property (nonatomic, weak) UIView *containerView;

@property (nonatomic, strong) KRTextSelectionAnchorView *leftAnchor;
@property (nonatomic, strong) KRTextSelectionAnchorView *rightAnchor;

@property (nonatomic, strong) KRTextMagnifierView *magnifierView;

// Selection state
@property (nonatomic, weak) KRLabel *startLabel;
@property (nonatomic, assign) NSInteger startIndex;
@property (nonatomic, weak) KRLabel *endLabel;
@property (nonatomic, assign) NSInteger endIndex;

@property (nonatomic, strong) UIPanGestureRecognizer *panGesture;

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

- (void)startSelectionWithLabels:(NSArray<KRLabel *> *)labels containerView:(UIView *)containerView {
    [self endSelection]; // Clear previous
    
    self.labels = labels;
    self.containerView = containerView;
    
    // Reset state
    self.startLabel = nil;
    self.endLabel = nil;
    self.startIndex = -1;
    self.endIndex = -1;
}

- (void)selectWordAtPoint:(CGPoint)point {
    if (!self.labels || !self.containerView) return;
    
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
        // 2. Expand to word (simple approximation)
        NSString *text = touchedLabel.textRender.textStorage.string;
        NSRange wordRange = [self rangeOfWordAtIndex:charIndex inString:text];
        
        self.startLabel = touchedLabel;
        self.endLabel = touchedLabel;
        self.startIndex = wordRange.location;
        self.endIndex = wordRange.location + wordRange.length;
        
        [self updateUI];
    }
}

- (void)selectAll {
    if (self.labels.count == 0) return;
    
    self.startLabel = self.labels.firstObject;
    self.startIndex = 0;
    
    self.endLabel = self.labels.lastObject;
    self.endIndex = self.endLabel.textRender.textStorage.length;
    
    [self updateUI];
}

- (void)endSelection {
    [self.leftAnchor removeFromSuperview];
    [self.rightAnchor removeFromSuperview];
    
    for (KRLabel *label in self.labels) {
        label.selectedRange = NSMakeRange(NSNotFound, 0);
    }
    
    self.labels = nil;
    self.containerView = nil;
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
                } else {
                    // Swap: 拖动起始游标超过了结束游标
                    self.startLabel = self.endLabel;
                    self.startIndex = self.endIndex;
                    
                    self.endLabel = closestLabel;
                    self.endIndex = idx;
                    
                    KRTextSelectionAnchorView *temp = self.leftAnchor;
                    self.leftAnchor = self.rightAnchor;
                    self.rightAnchor = temp;
                    selectionChanged = YES;
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
                } else {
                    // Swap: 拖动结束游标超过了起始游标
                    self.endLabel = self.startLabel;
                    self.endIndex = self.startIndex;
                    
                    self.startLabel = closestLabel;
                    self.startIndex = idx;
                    
                    KRTextSelectionAnchorView *temp = self.leftAnchor;
                    self.leftAnchor = self.rightAnchor;
                    self.rightAnchor = temp;
                    selectionChanged = YES;
                }
            }
            
            if (selectionChanged) {
                [self updateUI];
            }
        }
    }
    
    if (gesture.state == UIGestureRecognizerStateEnded || gesture.state == UIGestureRecognizerStateCancelled) {
        [self removeMagnifierView];
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
    
    [string enumerateSubstringsInRange:NSMakeRange(0, string.length) options:NSStringEnumerationByWords usingBlock:^(NSString * _Nullable substring, NSRange substringRange, NSRange enclosingRange, BOOL * _Nonnull stop) {
        if (NSLocationInRange(index, substringRange)) {
            result = substringRange;
            *stop = YES;
        }
    }];
    
    return result;
}

@end

