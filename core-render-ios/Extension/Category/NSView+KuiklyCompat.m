//
//  NSView+KuiklyCompat.m
//

#import "NSView+KuiklyCompat.h"

static NSComparisonResult KuiklyBringToFrontCompare(__kindof NSView *a, __kindof NSView *b, void *context) {
    NSView *target = (__bridge NSView *)context;
    if (a == target && b != target) return NSOrderedDescending;
    if (b == target && a != target) return NSOrderedAscending;
    return NSOrderedSame;
}

@implementation NSView (KuiklyCompat)

- (void)bringSubviewToFront:(NSView *)view {
    if (!view || view.superview != self) { return; }
    [self sortSubviewsUsingFunction:KuiklyBringToFrontCompare context:(__bridge void * _Nullable)(view)];
}

@end


