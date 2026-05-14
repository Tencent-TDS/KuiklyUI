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

#import "KRTextMagnifierView.h"

// The size of the native iOS magnifier (elliptical).
static const CGFloat kMagnifierWidth = 127.0;
static const CGFloat kMagnifierHeight = 89.0;
static const CGFloat kMagnifierScale = 1.2; // The magnification factor.

static const CGFloat kMagnifierBorderWidth = 1.0;
static const CGFloat kMagnifierShadowRadius = 4;
static const CGFloat kMagnifierShadowOpacity = 0.25;


@interface KRTextMagnifierView ()

/// The image view that displays the magnified content.
@property (nonatomic, strong) UIImageView *contentImageView;

@end

@implementation KRTextMagnifierView

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        // The size of the native iOS magnifier (elliptical): 127x89
        self.frame = CGRectMake(0, 0, kMagnifierWidth, kMagnifierHeight);
        self.backgroundColor = [UIColor clearColor];
        self.userInteractionEnabled = NO;
        
        // Use UIImageView to display the magnified content.
        _contentImageView = [[UIImageView alloc] initWithFrame:self.bounds];
        _contentImageView.contentMode = UIViewContentModeScaleToFill;
        _contentImageView.clipsToBounds = YES;
        
        // Elliptical corner (simulating the native iOS effect).
        _contentImageView.layer.cornerRadius = kMagnifierHeight / 2;
        _contentImageView.layer.masksToBounds = YES;
        
        // Border (simulating the native iOS effect).
        const UIColor *kMagnifierBorderColor = [UIColor colorWithWhite:0.7 alpha:1.0];
        _contentImageView.layer.borderWidth = kMagnifierBorderWidth;
        _contentImageView.layer.borderColor = kMagnifierBorderColor.CGColor;
        
        [self addSubview:_contentImageView];
        
        // Add shadow effect.
        const CGSize kMagnifierShadowOffset = CGSizeMake(0, 2);
        self.layer.shadowColor = [UIColor blackColor].CGColor;
        self.layer.shadowOffset = kMagnifierShadowOffset;
        self.layer.shadowRadius = kMagnifierShadowRadius;
        self.layer.shadowOpacity = kMagnifierShadowOpacity;
    }
    return self;
}

- (void)setTouchPoint:(CGPoint)touchPoint {
    _touchPoint = touchPoint;
    [self updateMagnification];
}

- (void)updateMagnification {
    if (!self.viewToMagnify) return;
    
    // Capture the current touchPoint value before async scheduling.
    CGPoint capturedPoint = self.touchPoint;
    
    // Capture the screenshot asynchronously on the main thread 
    // to avoid recursive calls during `addSubview`.
    dispatch_async(dispatch_get_main_queue(), ^{
        [self captureAndDisplayAtPoint:capturedPoint];
    });
}

- (void)captureAndDisplayAtPoint:(CGPoint)point {
    if (!self.viewToMagnify || !self.superview) return;
    
    UIGraphicsImageRendererFormat *format = [[UIGraphicsImageRendererFormat alloc] init];
    format.scale = [UIScreen mainScreen].scale;
    format.opaque = NO;
    
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:self.bounds.size format:format];

    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        CGContextRef ctx = rendererContext.CGContext;
        
        // Translate and scale to center the point and magnify.
        CGContextTranslateCTM(ctx, self.bounds.size.width / 2, self.bounds.size.height / 2);
        CGContextScaleCTM(ctx, kMagnifierScale, kMagnifierScale);
        CGContextTranslateCTM(ctx, -point.x, -point.y);
        
        // Use drawViewHierarchyInRect
        [self.viewToMagnify drawViewHierarchyInRect:self.viewToMagnify.bounds afterScreenUpdates:NO];
    }];
    
    self.contentImageView.image = image;
}

@end
