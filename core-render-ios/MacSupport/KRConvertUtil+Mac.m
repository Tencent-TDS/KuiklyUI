/* macOS minimal implementations for KRConvertUtil to satisfy AppKit build */
#import "KRConvertUtil.h"
#import "RCTUIKit.h"
#import "UIView+CSS.h" // 使用 +css_string:
#import <QuartzCore/QuartzCore.h>

@implementation KRConvertUtil (Mac)

+ (UIColor *)UIColor:(id)json {
    if (!json) { return nil; }
    if ([json isKindOfClass:[NSNumber class]] || [json isKindOfClass:[NSString class]]) {
        NSUInteger argb = [self NSUInteger:json];
        CGFloat a = ((argb >> 24) & 0xFF) / 255.0;
        CGFloat r = ((argb >> 16) & 0xFF) / 255.0;
        CGFloat g = ((argb >> 8) & 0xFF) / 255.0;
        CGFloat b = (argb & 0xFF) / 255.0;
        return [UIColor colorWithRed:r green:g blue:b alpha:a];
    }
    return nil;
}

+ (UIBezierPath *)hr_bezierPathWithRoundedRect:(CGRect)rect
                           topLeftCornerRadius:(CGFloat)topLeftCornerRadius
                           topRightCornerRadius:(CGFloat)topRightCornerRadius
                           bottomLeftCornerRadius:(CGFloat)bottomLeftCornerRadius
                       bottomRightCornerRadius:(CGFloat)bottomRightCornerRadius
{
    // 简化：同半径时使用系统圆角；否则退化为统一最小半径
    CGFloat rx = topLeftCornerRadius;
    CGFloat ry = topLeftCornerRadius;
    if (!(topLeftCornerRadius == topRightCornerRadius &&
          topLeftCornerRadius == bottomLeftCornerRadius &&
          topLeftCornerRadius == bottomRightCornerRadius)) {
        CGFloat minR = MIN(MIN(topLeftCornerRadius, topRightCornerRadius), MIN(bottomLeftCornerRadius, bottomRightCornerRadius));
        rx = ry = MAX(0, minR);
    }
    NSBezierPath *bp = [NSBezierPath bezierPathWithRoundedRect:rect xRadius:rx yRadius:ry];
    return (UIBezierPath *)bp;
}

+ (UIViewAnimationOptions)hr_viewAnimationOptions:(NSString *)value {
    // macOS 下占位返回 0
    return (UIViewAnimationOptions)0;
}

+ (UIViewAnimationCurve)hr_viewAnimationCurve:(NSString *)value {
    NSInteger v = [value integerValue];
    switch (v) {
        case 1: return UIViewAnimationCurveEaseIn;
        case 2: return UIViewAnimationCurveEaseOut;
        case 3: return UIViewAnimationCurveEaseInOut;
        default: return UIViewAnimationCurveLinear;
    }
}

+ (UIUserInterfaceStyle)KRUserInterfaceStyle:(NSString *)style {
    NSString *s = [UIView css_string:style];
    if ([s isEqualToString:@"dark"]) { return UIUserInterfaceStyleDark; }
    if ([s isEqualToString:@"light"]) { return UIUserInterfaceStyleLight; }
    return UIUserInterfaceStyleUnspecified;
}

@end


