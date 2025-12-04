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

#import "KRTextSelectionAnchorView.h"

/// 游标圆形头部的直径
static const CGFloat kAnchorCapDiameter = 14.0;
/// 游标竖线的宽度
static const CGFloat kAnchorLineWidth = 2.0;
/// 圆形头部与文字行高重叠的比例（1/5 重叠，4/5 在外面）
static const CGFloat kAnchorCapOverlapRatio = 0.2;

@implementation KRTextSelectionAnchorView {
    CGFloat _lineHeight;
    BOOL _isTop;
    UIColor *_color;
}

#pragma mark - Class Methods

+ (CGFloat)anchorCapDiameter {
    return kAnchorCapDiameter;
}

+ (CGFloat)anchorCapOffset {
    // 圆形头部超出文字的偏移量 = 直径 * (1 - 重叠比例)
    return kAnchorCapDiameter * (1.0 - kAnchorCapOverlapRatio);
}

+ (CGFloat)defaultAnchorWidth {
    return kAnchorCapDiameter;
}

+ (CGFloat)defaultAnchorHeight {
    return kAnchorCapDiameter;
}

#pragma mark - Lifecycle

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        self.backgroundColor = [UIColor clearColor];
        self.userInteractionEnabled = NO;
        _color = [UIColor colorWithRed:0x00/255.0 green:0x99/255.0 blue:0xff/255.0 alpha:1.0];
    }
    return self;
}

#pragma mark - Public Methods

- (void)setIsTop:(BOOL)isTop {
    _isTop = isTop;
    [self setNeedsDisplay];
}

- (BOOL)isTop {
    return _isTop;
}

- (void)setAnchorLineHeight:(CGFloat)height {
    if (_lineHeight != height) {
        _lineHeight = height;
        [self setNeedsDisplay];
    }
}

- (void)setColor:(UIColor *)color {
    _color = color;
    [self setNeedsDisplay];
}

#pragma mark - Drawing

- (void)drawRect:(CGRect)rect {
    CGContextRef context = UIGraphicsGetCurrentContext();
    if (!context) return;
    
    [_color set];
    CGContextSetLineWidth(context, kAnchorLineWidth);
    
    CGFloat capDiameter = kAnchorCapDiameter;
    CGFloat capOverlap = capDiameter * kAnchorCapOverlapRatio; // 与文字重叠的部分
    CGFloat capOffset = capDiameter - capOverlap;              // 超出文字的部分
    CGFloat centerX = self.bounds.size.width / 2.0;
    CGFloat capOriginX = (self.bounds.size.width - capDiameter) / 2.0;
    
    if (_isTop) {
        // 起始游标：圆形在顶部，竖线向下延伸
        // 绘制圆形头部（4/5 在文字上方）
        CGRect capRect = CGRectMake(capOriginX, 0, capDiameter, capDiameter);
        CGContextAddEllipseInRect(context, capRect);
        CGContextFillPath(context);
        
        // 绘制竖线（从圆形底部 1/5 处向下延伸）
        CGContextMoveToPoint(context, centerX, capOffset);
        CGContextAddLineToPoint(context, centerX, _lineHeight + capOffset);
        CGContextStrokePath(context);
        
    } else {
        // 结束游标：竖线在顶部，圆形在底部
        // 绘制竖线
        CGContextMoveToPoint(context, centerX, 0);
        CGContextAddLineToPoint(context, centerX, _lineHeight);
        CGContextStrokePath(context);
        
        // 绘制圆形头部（4/5 在文字下方，只有 1/5 与文字重叠）
        CGRect capRect = CGRectMake(capOriginX, _lineHeight - capOverlap, capDiameter, capDiameter);
        CGContextAddEllipseInRect(context, capRect);
        CGContextFillPath(context);
    }
}

@end

