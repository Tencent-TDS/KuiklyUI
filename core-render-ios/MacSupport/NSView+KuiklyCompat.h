//
//  NSView+KuiklyCompat.h
//

#import <AppKit/AppKit.h>

#define UIEvent NSEvent

NS_ASSUME_NONNULL_BEGIN

@interface NSView (KuiklyCompat)

@property (nonatomic, assign) CGFloat alpha;
@property (nonatomic) CGAffineTransform transform;
@property (nonatomic, copy) NSColor *backgroundColor;
@property (nonatomic, getter=isUserInteractionEnabled) BOOL userInteractionEnabled;

#pragma mark -

- (void)bringSubviewToFront:(NSView *)view;
- (void)insertSubview:(NSView *)view atIndex:(NSInteger)index;
- (void)layoutIfNeeded;
- (void)layoutSubviews;

#pragma mark -

- (void)setIsAccessibilityElement:(BOOL)isAccessibilityElement;

#pragma mark -

- (NSView *)hitTest:(CGPoint)point withEvent:(UIEvent *_Nullable)event;

@end

NS_ASSUME_NONNULL_END
