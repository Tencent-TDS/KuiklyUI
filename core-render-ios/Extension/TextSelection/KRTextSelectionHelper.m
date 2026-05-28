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
#import "KRLabel.h"
#import "KRLogModule.h"
#import "KRView+TextSelection.h"

@interface KRTextSelectionHelper ()

@property (nonatomic, strong) NSArray<KRLabel *> *labels;

@property (nonatomic, weak) KRLabel *startLabel;
@property (nonatomic, assign) NSInteger startIndex;
@property (nonatomic, weak) KRLabel *endLabel;
@property (nonatomic, assign) NSInteger endIndex;

// 原始锚点：用户 mouseDown 的位置，drag 期间不变
@property (nonatomic, weak) KRLabel *anchorLabel;
@property (nonatomic, assign) NSInteger anchorIndex;

@property (nonatomic, strong) UIColor *selectionColor;

#if TARGET_OS_OSX
@property (nonatomic, strong) id localEventMonitor;
#endif

@end

@implementation KRTextSelectionHelper

- (instancetype)init {
    if (self = [super init]) {
        self.startIndex = -1;
        self.endIndex = -1;
    }
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] Helper init %p", self]];
    return self;
}

- (void)startSelectionWithLabels:(NSArray<KRLabel *> *)labels containerView:(KRView *)containerView {
    [self endSelection];
    self.labels = labels;
    self.containerView = containerView;
    self.startLabel = nil;
    self.endLabel = nil;
    self.startIndex = -1;
    self.endIndex = -1;
    
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] startSelection labels:%lu container:%@",
                          (unsigned long)labels.count, NSStringFromClass([containerView class])]];
    
#if TARGET_OS_OSX
    [self installMouseMonitor];
#endif
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
                selectionRange = NSMakeRange(charIndex, 1);
                break;
            case KRTextSelectionTypeWord:
                selectionRange = [self rangeOfWordAtIndex:charIndex inString:text];
                break;
            case KRTextSelectionTypeParagraph:
                selectionRange = [self rangeOfParagraphAtIndex:charIndex inString:text];
                break;
            case KRTextSelectionTypeSentence:
                selectionRange = [self rangeOfSentenceAtIndex:charIndex inString:text];
                break;
            default:
                selectionRange = NSMakeRange(charIndex, 1);
                break;
        }
        
        self.startLabel = touchedLabel;
        self.startIndex = selectionRange.location;
        self.endLabel = touchedLabel;
        self.endIndex = selectionRange.location + selectionRange.length;
        
        [self updateUI];
        
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
    
    [self notifyDelegateDidStartSelection];
    
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] selectAll labels:%lu endIndex:%ld",
                          (unsigned long)self.labels.count, (long)self.endIndex]];
}

- (void)endSelection {
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] endSelection begin helper:%p labels:%@ container:%@", self, self.labels ? @"exists" : @"nil", self.containerView ? @"exists" : @"nil"]];
    
#if TARGET_OS_OSX
    [self removeMouseMonitor];
#endif
    
    BOOL hadSelection = (self.labels != nil && self.containerView != nil);
    
    for (KRLabel *label in self.labels) {
        label.selectedRange = NSMakeRange(NSNotFound, 0);
    }
    
    if (self.containerView) {
        [self.containerView kr_setTextSelectionHelper:nil];
    }
    
    self.labels = nil;
    self.containerView = nil;
    
    if (hadSelection) {
        [self notifyDelegateDidCancelSelection];
        [KRLogModule logInfo:@"[TextSelection] endSelection finished (had selection)"];
    } else {
        [KRLogModule logInfo:@"[TextSelection] endSelection finished (no active selection)"];
    }
}

#pragma mark - UI Update

- (void)updateUI {
    if (!self.startLabel || !self.endLabel || self.startIndex < 0 || self.endIndex < 0) {
        [KRLogModule logInfo:@"[TextSelection] updateUI early return - invalid state"];
        return;
    }
    
    BOOL selecting = NO;
    NSInteger assignedCount = 0;
    
    for (KRLabel *label in self.labels) {
        NSRange range = NSMakeRange(NSNotFound, 0);
        
        if (label == self.startLabel && label == self.endLabel) {
            range = NSMakeRange(self.startIndex, self.endIndex - self.startIndex);
            selecting = NO;
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
            assignedCount++;
            [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] updateUI label:%@ range:(%ld,%ld)", label, (long)range.location, (long)range.length]];
        }
        label.selectedRange = range;
    }
    
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] updateUI totalLabels:%lu assigned:%ld start:(%@,%ld) end:(%@,%ld)",
                          (unsigned long)self.labels.count, (long)assignedCount,
                          self.startLabel, (long)self.startIndex,
                          self.endLabel, (long)self.endIndex]];
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
            NSString *text = label.textRender.textStorage.string;
            NSRange range = NSMakeRange(self.startIndex, self.endIndex - self.startIndex);
            if (range.location + range.length <= text.length) {
                [texts addObject:[text substringWithRange:range]];
            }
            break;
        } else if (label == self.startLabel) {
            NSString *text = label.textRender.textStorage.string;
            if (self.startIndex < text.length) {
                [texts addObject:[text substringFromIndex:self.startIndex]];
            }
            collecting = YES;
        } else if (label == self.endLabel) {
            NSString *text = label.textRender.textStorage.string;
            if (self.endIndex <= text.length) {
                [texts addObject:[text substringToIndex:self.endIndex]];
            }
            collecting = NO;
            break;
        } else if (collecting) {
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
    NSInteger startLabelIndex = [self.labels indexOfObject:self.startLabel];
    if (startLabelIndex == NSNotFound) {
        return @[];
    }
    
    if (startLabelIndex > 0) {
        KRLabel *previousLabel = self.labels[startLabelIndex - 1];
        NSString *previousText = previousLabel.textRender.textStorage.string;
        [preContent addObject:previousText ?: @""];
    }
    
    NSString *startLabelText = self.startLabel.textRender.textStorage.string;
    if (self.startIndex > 0 && self.startIndex <= startLabelText.length) {
        [preContent addObject:[startLabelText substringToIndex:self.startIndex]];
    } else {
        [preContent addObject:@""];
    }
    
    return preContent;
}

- (NSArray<NSString *> *)getPostSelectionContent {
    if (!self.endLabel || self.endIndex < 0) {
        return @[];
    }
    
    NSMutableArray<NSString *> *postContent = [NSMutableArray array];
    NSInteger endLabelIndex = [self.labels indexOfObject:self.endLabel];
    if (endLabelIndex == NSNotFound) {
        return @[];
    }
    
    NSString *endLabelText = self.endLabel.textRender.textStorage.string;
    if (self.endIndex < endLabelText.length) {
        [postContent addObject:[endLabelText substringFromIndex:self.endIndex]];
    } else {
        [postContent addObject:@""];
    }
    
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
    for (KRLabel *label in self.labels) {
        label.selectionColor = color;
    }
}

#pragma mark - Helpers

- (NSInteger)insertionIndexForPoint:(CGPoint)point inLabel:(KRLabel *)label {
    if (!CGSizeEqualToSize(label.textRender.size, label.bounds.size)) {
        label.textRender.size = label.bounds.size;
    }
    
    NSLayoutManager *lm = label.textRender.layoutManager;
    NSTextContainer *tc = label.textRender.textContainer;
    NSUInteger textLength = label.textRender.textStorage.length;
    
    if (textLength == 0) {
        return 0;
    }
    
    CGPoint adjustedPoint = point;
    if (adjustedPoint.x < 0) {
        adjustedPoint.x = 0;
    } else if (adjustedPoint.x > label.bounds.size.width) {
        adjustedPoint.x = label.bounds.size.width;
    }
    
    CGFloat fraction = 0;
    NSUInteger index = [lm characterIndexForPoint:adjustedPoint inTextContainer:tc fractionOfDistanceBetweenInsertionPoints:&fraction];
    
    if (index >= textLength) {
        return textLength;
    }
    
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

- (NSRange)rangeOfWordAtIndex:(NSInteger)index inString:(NSString *)string {
    if (index < 0 || index >= string.length) return NSMakeRange(0, 0);
    
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
    
    if (!found) {
        result = NSMakeRange(0, string.length);
    }
    
    return result;
}

- (CGFloat)distanceFromPoint:(CGPoint)p toRect:(CGRect)rect {
    CGFloat dx = MAX(CGRectGetMinX(rect) - p.x, 0);
    if (p.x > CGRectGetMaxX(rect)) dx = p.x - CGRectGetMaxX(rect);
    
    CGFloat dy = MAX(CGRectGetMinY(rect) - p.y, 0);
    if (p.y > CGRectGetMaxY(rect)) dy = p.y - CGRectGetMaxY(rect);
    
    return sqrt(dx*dx + dy*dy);
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

#pragma mark - macOS Mouse Events

- (void)mouseDown:(NSEvent *)event inLabel:(KRLabel *)label localPoint:(NSPoint)localPoint {
    if (!self.labels || ![self.labels containsObject:label]) {
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] mouseDown rejected - labels:%@ contains:%d", self.labels ? @"exists" : @"nil", (int)[self.labels containsObject:label]]];
        return;
    }
    
    NSInteger idx = [self insertionIndexForPoint:localPoint inLabel:label];
    
    if (event.clickCount == 2) {
        NSString *text = label.textRender.textStorage.string;
        NSRange wordRange = [self rangeOfWordAtIndex:idx inString:text];
        self.startLabel = label;
        self.startIndex = wordRange.location;
        self.endLabel = label;
        self.endIndex = wordRange.location + wordRange.length;
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] mouseDown doubleClick label:%@ idx:%ld wordRange:(%ld,%ld)", label, (long)idx, (long)wordRange.location, (long)wordRange.length]];
    } else if (event.clickCount >= 3) {
        self.startLabel = label;
        self.startIndex = 0;
        self.endLabel = label;
        self.endIndex = label.textRender.textStorage.length;
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] mouseDown tripleClick label:%@ idx:%ld fullLength:%ld", label, (long)idx, (long)self.endIndex]];
    } else {
        self.startLabel = label;
        self.startIndex = idx;
        self.endLabel = label;
        self.endIndex = idx;
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] mouseDown singleClick label:%@ idx:%ld", label, (long)idx]];
    }
    
    // 保存原始锚点
    self.anchorLabel = self.startLabel;
    self.anchorIndex = self.startIndex;
    
    [self updateUI];
    [self notifyDelegateDidStartSelection];
}

- (void)mouseDraggedToPoint:(NSPoint)containerPoint {
    if (!self.labels || !self.containerView) {
        [KRLogModule logInfo:@"[TextSelection] mouseDragged rejected - no labels or container"];
        return;
    }
    
    KRLabel *closest = nil;
    CGFloat minDistance = MAXFLOAT;
    
    for (KRLabel *label in self.labels) {
        CGPoint localPoint = [self.containerView convertPoint:containerPoint toView:label];
        CGFloat dist = [self distanceFromPoint:localPoint toRect:label.bounds];
        if (CGRectContainsPoint(label.bounds, localPoint)) {
            dist = 0;
        }
        if (dist < minDistance) {
            minDistance = dist;
            closest = label;
            if (dist == 0) break;
        }
    }
    
    if (!closest) {
        [KRLogModule logInfo:@"[TextSelection] mouseDragged no closest label found"];
        return;
    }
    
    CGPoint local = [self.containerView convertPoint:containerPoint toView:closest];
    NSInteger idx = [self insertionIndexForPoint:local inLabel:closest];
    
    // 始终和原始锚点比较，而非和当前 start 比较
    NSComparisonResult cmp = [self comparePositionLabel:closest index:idx
                                              withLabel:self.anchorLabel index:self.anchorIndex];
    if (cmp == NSOrderedDescending || cmp == NSOrderedSame) {
        // 拖拽在锚点之后（向下）→ anchor 是 start，拖拽位置是 end
        self.startLabel = self.anchorLabel;
        self.startIndex = self.anchorIndex;
        self.endLabel = closest;
        self.endIndex = idx;
    } else {
        // 拖拽在锚点之前（向上）→ 拖拽位置是 start，anchor 是 end
        self.startLabel = closest;
        self.startIndex = idx;
        self.endLabel = self.anchorLabel;
        self.endIndex = self.anchorIndex;
    }
    
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] mouseDragged closest:%@ idx:%ld start:(%@,%ld) end:(%@,%ld)",
                          closest, (long)idx, self.startLabel, (long)self.startIndex, self.endLabel, (long)self.endIndex]];
    
    [self updateUI];
    [self notifyDelegateDidChangeSelection];
}

- (void)mouseUp {
    [KRLogModule logInfo:@"[TextSelection] mouseUp"];
    [self notifyDelegateDidEndSelection];
}

#pragma mark - macOS Global Mouse Monitor

#if TARGET_OS_OSX

- (void)installMouseMonitor {
    [self removeMouseMonitor];
    __weak typeof(self) weakSelf = self;
    self.localEventMonitor = [NSEvent addLocalMonitorForEventsMatchingMask:NSEventMaskLeftMouseDown handler:^NSEvent *(NSEvent *event) {
        [weakSelf handleGlobalMouseDown:event];
        return event;
    }];
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] installMouseMonitor helper:%p", self]];
}

- (void)removeMouseMonitor {
    if (self.localEventMonitor) {
        [NSEvent removeMonitor:self.localEventMonitor];
        self.localEventMonitor = nil;
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] removeMouseMonitor helper:%p", self]];
    }
}

- (void)handleGlobalMouseDown:(NSEvent *)event {
    if (!self.labels || self.labels.count == 0) {
        [KRLogModule logInfo:@"[TextSelection] handleGlobalMouseDown early return - no labels"];
        return;
    }
    
    NSWindow *window = event.window;
    if (!window) {
        [KRLogModule logInfo:@"[TextSelection] handleGlobalMouseDown early return - no window"];
        return;
    }
    
    NSPoint windowPoint = event.locationInWindow;
    NSView *hitView = [window.contentView hitTest:windowPoint];
    
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] handleGlobalMouseDown hitView:%@ windowPoint:(%.1f,%.1f)", hitView, windowPoint.x, windowPoint.y]];
    
    // Check if hit view is one of the selected labels or inside one
    BOOL hitSelectedLabel = NO;
    NSView *current = hitView;
    while (current) {
        if ([current isKindOfClass:[KRLabel class]]) {
            for (KRLabel *label in self.labels) {
                if (label == current && label.selectedRange.length > 0) {
                    hitSelectedLabel = YES;
                    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] handleGlobalMouseDown hit selected label:%@ range:(%ld,%ld)", label, (long)label.selectedRange.location, (long)label.selectedRange.length]];
                    break;
                }
            }
        }
        if (hitSelectedLabel) break;
        current = current.superview;
    }
    
    if (!hitSelectedLabel) {
        [KRLogModule logInfo:@"[TextSelection] handleGlobalMouseDown miss selected labels -> endSelection"];
        [self endSelection];
    }
}

#endif

@end
