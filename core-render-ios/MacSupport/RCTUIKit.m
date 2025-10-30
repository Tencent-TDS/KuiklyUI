/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// [macOS]

#if TARGET_OS_OSX

#define RCTAssert(...) // TODO

#import "RCTUIKit.h" // [macOS]
#import <QuartzCore/QuartzCore.h> // [macOS] for CATransaction
#import <objc/runtime.h>
#import <CoreImage/CIFilter.h>
#import <CoreImage/CIVector.h>

static char RCTGraphicsContextSizeKey;

//
// semantically equivalent functions
//

// UIGraphics.h

CGContextRef UIGraphicsGetCurrentContext(void)
{
	return [[NSGraphicsContext currentContext] CGContext];
}

NSImage *UIGraphicsGetImageFromCurrentImageContext(void)
{
	NSImage *image = nil;
	NSGraphicsContext *graphicsContext = [NSGraphicsContext currentContext];

	NSValue *sizeValue = objc_getAssociatedObject(graphicsContext, &RCTGraphicsContextSizeKey);
	if (sizeValue != nil) {
		CGImageRef cgImage = CGBitmapContextCreateImage([graphicsContext CGContext]);

		if (cgImage != NULL) {
			NSBitmapImageRep *imageRep = [[NSBitmapImageRep alloc] initWithCGImage:cgImage];
			image = [[NSImage alloc] initWithSize:[sizeValue sizeValue]];
			[image addRepresentation:imageRep];
			CFRelease(cgImage);
		}
	}

	return image;
}

//
// functionally equivalent types
//

// UIImage

CGFloat UIImageGetScale(NSImage *image)
{
  if (image == nil) {
    return 0.0;
  }
  
  RCTAssert(image.representations.count == 1, @"The scale can only be derived if the image has one representation.");

  NSImageRep *imageRep = image.representations.firstObject;
  if (imageRep != nil) {
    NSSize imageSize = image.size;
    NSSize repSize = CGSizeMake(imageRep.pixelsWide, imageRep.pixelsHigh);

    return round(fmax(repSize.width / imageSize.width, repSize.height / imageSize.height));
  }

  return 1.0;
}

CGImageRef __nullable UIImageGetCGImageRef(NSImage *image)
{
  return [image CGImageForProposedRect:NULL context:NULL hints:NULL];
}

static NSData *NSImageDataForFileType(NSImage *image, NSBitmapImageFileType fileType, NSDictionary<NSString *, id> *properties)
{
  RCTAssert(image.representations.count == 1, @"Expected only a single representation since UIImage only supports one.");

  NSBitmapImageRep *imageRep = (NSBitmapImageRep *)image.representations.firstObject;
  if (![imageRep isKindOfClass:[NSBitmapImageRep class]]) {
    RCTAssert([imageRep isKindOfClass:[NSBitmapImageRep class]], @"We need an NSBitmapImageRep to create an image.");
    return nil;
  }

  return [imageRep representationUsingType:fileType properties:properties];
}

NSData *UIImagePNGRepresentation(NSImage *image) {
  return NSImageDataForFileType(image, NSBitmapImageFileTypePNG, @{});
}

NSData *UIImageJPEGRepresentation(NSImage *image, CGFloat compressionQuality) {
  return NSImageDataForFileType(image,
                                NSBitmapImageFileTypeJPEG,
                                @{NSImageCompressionFactor: @(compressionQuality)});
}

// [macOS] NSImage category to mimic common UIImage constructors
@implementation NSImage (KRUIImageCompat)
+ (instancetype)imageWithCGImage:(CGImageRef)cgImage {
  if (!cgImage) { return nil; }
  return [[NSImage alloc] initWithCGImage:cgImage size:NSZeroSize];
}
+ (instancetype)imageWithData:(NSData *)data {
  return [[NSImage alloc] initWithData:data];
}
+ (instancetype)imageWithContentsOfFile:(NSString *)filePath {
  return [[NSImage alloc] initWithContentsOfFile:filePath];
}
@end

// [macOS
NSImage *UIImageResizableImageWithCapInsets(NSImage *image, NSEdgeInsets capInsets, UIImageResizingMode resizingMode)
{
  if (image == nil) {
    return nil;
  }
  
  // For macOS 10.10+, we can use the built-in capInsets property
  if (@available(macOS 10.10, *)) {
    NSImage *resizableImage = [image copy];
    resizableImage.capInsets = capInsets;
    resizableImage.resizingMode = (NSImageResizingMode)resizingMode;
    return resizableImage;
  }
  
  // For older macOS versions, return the original image
  return image;
}
// macOS]

// UIBezierPath
UIBezierPath *UIBezierPathWithRoundedRect(CGRect rect, CGFloat cornerRadius)
{
  return [NSBezierPath bezierPathWithRoundedRect:rect xRadius:cornerRadius yRadius:cornerRadius];
}

void UIBezierPathAppendPath(UIBezierPath *path, UIBezierPath *appendPath)
{
  return [path appendBezierPath:appendPath];
}

//
// substantially different types
//

// UIView


// [macOS] RCTUIView implementation - provides macOS-specific extensions
// Note: Most UIKit compatibility is handled by NSView+KuiklyCompat Category
@implementation RCTUIView
{
@private
  BOOL _clipsToBounds;
  BOOL _hasMouseOver;
  NSTrackingArea *_trackingArea;
  BOOL _mouseDownCanMoveWindow;
}

static RCTUIView *RCTUIViewCommonInit(RCTUIView *self)
{
  if (self != nil) {
    self.wantsLayer = YES;
    self->_enableFocusRing = YES;
    self->_mouseDownCanMoveWindow = YES;
  }
  return self;
}

- (instancetype)initWithFrame:(NSRect)frameRect
{
  return RCTUIViewCommonInit([super initWithFrame:frameRect]);
}

- (instancetype)initWithCoder:(NSCoder *)coder
{
  return RCTUIViewCommonInit([super initWithCoder:coder]);
}

- (BOOL)acceptsFirstMouse:(NSEvent *)event
{
  if (self.acceptsFirstMouse || [super acceptsFirstMouse:event]) {
    return YES;
  }

  // If any RCTUIView view above has acceptsFirstMouse set, then return YES here.
  NSView *view = self;
  while ((view = view.superview)) {
    if ([view isKindOfClass:[RCTUIView class]] && [(RCTUIView *)view acceptsFirstMouse]) {
      return YES;
    }
  }

  return NO;
}

- (BOOL)acceptsFirstResponder
{
  return [self canBecomeFirstResponder];
}

- (BOOL)isFirstResponder {
  return [[self window] firstResponder] == self;
}

- (void)viewDidMoveToWindow
{
  // Subscribe to view bounds changed notification so that the view can be notified when a
  // scroll event occurs either due to trackpad/gesture based scrolling or a scrollwheel event
  // both of which would not cause the mouseExited to be invoked.

  if ([self window] == nil) {
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:NSViewBoundsDidChangeNotification
                                                  object:nil];
  }
  else if ([[self enclosingScrollView] contentView] != nil) {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(viewBoundsChanged:)
                                                 name:NSViewBoundsDidChangeNotification
                                               object:[[self enclosingScrollView] contentView]];
  }

    // TODO
//  [self reactViewDidMoveToWindow]; // [macOS] Github#1412

  // [macOS] didMoveToWindow is handled by NSView+KuiklyCompat swizzling
  [super viewDidMoveToWindow];
}

// TODO
//- (void)viewBoundsChanged:(NSNotification*)__unused inNotif
//{
//  // When an enclosing scrollview is scrolled using the scrollWheel or trackpad,
//  // the mouseExited: event does not get called on the view where mouseEntered: was previously called.
//  // This creates an unnatural pairing of mouse enter and exit events and can cause problems.
//  // We therefore explicitly check for this here and handle them by calling the appropriate callbacks.
//
//  if (!_hasMouseOver && self.onMouseEnter)
//  {
//    NSPoint locationInWindow = [[self window] mouseLocationOutsideOfEventStream];
//    NSPoint locationInView = [self convertPoint:locationInWindow fromView:nil];
//
//    if (NSPointInRect(locationInView, [self bounds]))
//    {
//      _hasMouseOver = YES;
//
//      [self sendMouseEventWithBlock:self.onMouseEnter
//                       locationInfo:[self locationInfoFromDraggingLocation:locationInWindow]
//                      modifierFlags:0
//                     additionalData:nil];
//    }
//  }
//  else if (_hasMouseOver && self.onMouseLeave)
//  {
//    NSPoint locationInWindow = [[self window] mouseLocationOutsideOfEventStream];
//    NSPoint locationInView = [self convertPoint:locationInWindow fromView:nil];
//
//    if (!NSPointInRect(locationInView, [self bounds]))
//    {
//      _hasMouseOver = NO;
//
//      [self sendMouseEventWithBlock:self.onMouseLeave
//                       locationInfo:[self locationInfoFromDraggingLocation:locationInWindow]
//                      modifierFlags:0
//                     additionalData:nil];
//    }
//  }
//}

// TODO
- (BOOL)hasMouseHoverEvent
{
  // [macOS] M1：先统一关闭 hover 追踪，避免选择器告警
  return NO;
}

- (NSDictionary*)locationInfoFromDraggingLocation:(NSPoint)locationInWindow
{
  NSPoint locationInView = [self convertPoint:locationInWindow fromView:nil];

  return @{@"screenX": @(locationInWindow.x),
           @"screenY": @(locationInWindow.y),
           @"clientX": @(locationInView.x),
           @"clientY": @(locationInView.y)
           };
}

- (NSDictionary*)locationInfoFromEvent:(NSEvent*)event
{
  NSPoint locationInWindow = event.locationInWindow;
  NSPoint locationInView = [self convertPoint:locationInWindow fromView:nil];

  return @{@"screenX": @(locationInWindow.x),
           @"screenY": @(locationInWindow.y),
           @"clientX": @(locationInView.x),
           @"clientY": @(locationInView.y)
           };
}

- (void)mouseEntered:(NSEvent *)event
{
  _hasMouseOver = YES;
    // TODO
//  [self sendMouseEventWithBlock:self.onMouseEnter
//                   locationInfo:[self locationInfoFromEvent:event]
//                  modifierFlags:event.modifierFlags
//                 additionalData:nil];
}

- (void)mouseExited:(NSEvent *)event
{
  _hasMouseOver = NO;
    // TODO
//  [self sendMouseEventWithBlock:self.onMouseLeave
//                   locationInfo:[self locationInfoFromEvent:event]
//                  modifierFlags:event.modifierFlags
//                 additionalData:nil];
}

// TODO
//- (void)sendMouseEventWithBlock:(RCTDirectEventBlock)block
//                   locationInfo:(NSDictionary*)locationInfo
//                  modifierFlags:(NSEventModifierFlags)modifierFlags
//                 additionalData:(NSDictionary* __nullable)additionalData
//{
//  if (block == nil) {
//    return;
//  }
//
//  NSMutableDictionary *body = [NSMutableDictionary new];
//
//  if (modifierFlags & NSEventModifierFlagShift) {
//    body[@"shiftKey"] = @YES;
//  }
//  if (modifierFlags & NSEventModifierFlagControl) {
//    body[@"ctrlKey"] = @YES;
//  }
//  if (modifierFlags & NSEventModifierFlagOption) {
//    body[@"altKey"] = @YES;
//  }
//  if (modifierFlags & NSEventModifierFlagCommand) {
//    body[@"metaKey"] = @YES;
//  }
//
//  if (locationInfo) {
//    [body addEntriesFromDictionary:locationInfo];
//  }
//
//  if (additionalData) {
//    [body addEntriesFromDictionary:additionalData];
//  }
//
//  block(body);
//}

- (void)updateTrackingAreas
{
  BOOL hasMouseHoverEvent = [self hasMouseHoverEvent];
  BOOL wouldRecreateIdenticalTrackingArea = hasMouseHoverEvent && _trackingArea && NSEqualRects(self.bounds, [_trackingArea rect]);

  if (!wouldRecreateIdenticalTrackingArea) {
    if (_trackingArea) {
      [self removeTrackingArea:_trackingArea];
    }

    if (hasMouseHoverEvent) {
      _trackingArea = [[NSTrackingArea alloc] initWithRect:self.bounds
                                                   options:NSTrackingActiveAlways|NSTrackingMouseEnteredAndExited
                                                     owner:self
                                                  userInfo:nil];
      [self addTrackingArea:_trackingArea];
    }
  }

  [super updateTrackingAreas];
}

- (BOOL)mouseDownCanMoveWindow{
	return _mouseDownCanMoveWindow;
}

- (void)setMouseDownCanMoveWindow:(BOOL)mouseDownCanMoveWindow{
	_mouseDownCanMoveWindow = mouseDownCanMoveWindow;
}

- (BOOL)isFlipped
{
  return YES;
}

- (CGFloat)alpha
{
  return self.alphaValue;
}

- (void)setAlpha:(CGFloat)alpha
{
  self.alphaValue = alpha;
}

- (CGAffineTransform)transform
{
  return self.layer.affineTransform;
}

- (void)setTransform:(CGAffineTransform)transform
{
  self.layer.affineTransform = transform;
}

- (NSView *)hitTest:(NSPoint)point
{
  // IMPORTANT point is passed in super coordinates by OSX, but expected to be passed in local coordinates
  NSView *superview = [self superview];
  NSPoint pointInSelf = superview != nil ? [self convertPoint:point fromView:superview] : point;
  return [self hitTest:pointInSelf withEvent:nil];
}

- (BOOL)wantsUpdateLayer
{
  return [self respondsToSelector:@selector(displayLayer:)];
}

- (void)updateLayer
{
  CALayer *layer = [self layer];
  // [macOS] backgroundColor is handled by NSView+KuiklyCompat
  if (self.backgroundColor) {
    // updateLayer will be called when the view's current appearance changes.
    // The layer's backgroundColor is a CGColor which is not appearance aware
    // so it has to be reset from the view's NSColor.
    [layer setBackgroundColor:[self.backgroundColor CGColor]];
  }
  [(id<CALayerDelegate>)self displayLayer:layer];
}

- (void)drawRect:(CGRect)rect
{
  // [macOS] backgroundColor is handled by NSView+KuiklyCompat
  [super drawRect:rect];
}

- (void)layout
{
  [super layout];
  // [macOS] layoutSubviews is handled by NSView+KuiklyCompat
}

- (BOOL)canBecomeFirstResponder
{
  return [super acceptsFirstResponder];
}

- (BOOL)becomeFirstResponder
{
  return [[self window] makeFirstResponder:self];
}

// [macOS] setNeedsLayout: wrapper for NSView.needsLayout
- (void)setNeedsLayout
{
  self.needsLayout = YES;
}

// [macOS] setNeedsDisplay: wrapper for NSView.needsDisplay
- (void)setNeedsDisplay
{
  self.needsDisplay = YES;
}

@synthesize clipsToBounds = _clipsToBounds;

// We purposely don't use RCTCursor for the parameter type here because it would introduce an import cycle:
// RCTUIKit > RCTCursor > RCTConvert > RCTUIKit
- (void)setCursor:(NSInteger)cursor
{
  // This method is required to be defined due to [RCTVirtualTextViewManager view] returning a RCTUIView.
}

@end

@implementation RCTPlatformView (AnimationCompat)
+ (void)animateWithDuration:(NSTimeInterval)duration
                      delay:(NSTimeInterval)delay
                    options:(UIViewAnimationOptions)options
                 animations:(void (^)(void))animations
                 completion:(void (^ __nullable)(BOOL finished))completion
{
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
    [CATransaction begin];
    [CATransaction setAnimationDuration:duration];
    if (animations) { animations(); }
    [CATransaction commit];
    if (completion) { completion(YES); }
  });
}

+ (void)animateWithDuration:(NSTimeInterval)duration
                      delay:(NSTimeInterval)delay
     usingSpringWithDamping:(CGFloat)damping
      initialSpringVelocity:(CGFloat)velocity
                    options:(UIViewAnimationOptions)options
                 animations:(void (^)(void))animations
                 completion:(void (^ __nullable)(BOOL finished))completion
{
  // 简化：与普通动画同处理
  [self animateWithDuration:duration delay:delay options:options animations:animations completion:completion];
}

+ (void)animateKeyframesWithDuration:(NSTimeInterval)duration
                                delay:(NSTimeInterval)delay
                              options:(UIViewKeyframeAnimationOptions)options
                           animations:(void (^)(void))animations
                           completion:(void (^ __nullable)(BOOL finished))completion
{
  [self animateWithDuration:duration delay:delay options:(UIViewAnimationOptions)options animations:animations completion:completion];
}

static NSMutableArray<void (^)(void)> *g_keyframeBlocks;

+ (void)addKeyframeWithRelativeStartTime:(double)frameStartTime
                        relativeDuration:(double)frameDuration
                               animations:(void (^)(void))animations
{
  if (!g_keyframeBlocks) { g_keyframeBlocks = [NSMutableArray array]; }
  if (animations) { [g_keyframeBlocks addObject:[animations copy]]; }
}

+ (void)setAnimationCurve:(UIViewAnimationCurve)curve
{
  // 最小兼容：不做额外处理
}
@end

// RCTUIScrollView

@implementation RCTUIScrollView

- (void)dealloc
{
  [[NSNotificationCenter defaultCenter] removeObserver:self name:NSViewBoundsDidChangeNotification object:self.contentView];
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    self.scrollEnabled = YES;
    self.drawsBackground = NO;
    self.contentView.postsBoundsChangedNotifications = YES;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(rct_contentViewBoundsDidChange:)
                                                 name:NSViewBoundsDidChangeNotification
                                               object:self.contentView];
  }
  
  return self;
}

// [macOS] Bridge UIView layout methods for UIScrollView subclasses
// Note: NSView+KuiklyCompat provides layoutSubviews (which calls layout), but we need
// the reverse direction for RCTUIScrollView: when NSView's layout is called, we need
// to call the UIKit-style layoutSubviews so subclasses like KRScrollView can override it.
// Lifecycle methods (didMoveToSuperview, didMoveToWindow) are handled by NSView+KuiklyCompat swizzling.

- (void)layout
{
  [super layout];
  // Bridge NSView's layout to UIView's layoutSubviews
  if (self.window != nil) {
    [self layoutSubviews];
  }
}

- (void)layoutSubviews
{
  // Subclasses (like KRScrollView) override this for UIView-style layout
}
// macOS]

- (void)setEnableFocusRing:(BOOL)enableFocusRing {
  if (_enableFocusRing != enableFocusRing) {
    _enableFocusRing = enableFocusRing;
  }

  if (enableFocusRing) {
    // NSTextView has no focus ring by default so let's use the standard Aqua focus ring.
    [self setFocusRingType:NSFocusRingTypeExterior];
  } else {
    [self setFocusRingType:NSFocusRingTypeNone];
  }
}

// UIScrollView properties missing from NSScrollView
- (CGPoint)contentOffset
{
  return self.documentVisibleRect.origin;
}

- (void)setContentOffset:(CGPoint)contentOffset
{
  [self.documentView scrollPoint:contentOffset];
}

- (void)setContentOffset:(CGPoint)contentOffset animated:(BOOL)animated
{
    if (animated) {
        [NSAnimationContext runAnimationGroup:^(NSAnimationContext *context) {
            context.duration = 0.3; // Set the duration of the animation
            [self.documentView.animator scrollPoint:contentOffset];
        } completionHandler:^{
            if ([self.delegate respondsToSelector:@selector(scrollViewDidEndScrollingAnimation:)]) {
                [self.delegate scrollViewDidEndScrollingAnimation:(UIScrollView *)self];
            }
        }];
    } else {
        [self.documentView scrollPoint:contentOffset];
    }
}

- (UIEdgeInsets)contentInset
{
  return super.contentInsets;
}

- (void)setContentInset:(UIEdgeInsets)insets
{
  super.contentInsets = insets;
}

- (CGSize)contentSize
{
  return self.documentView.frame.size;
}

- (void)setContentSize:(CGSize)contentSize
{
  CGRect frame = self.documentView.frame;
  frame.size = contentSize;
  self.documentView.frame = frame;
}

- (BOOL)showsHorizontalScrollIndicator
{
	return self.hasHorizontalScroller;
}

- (void)setShowsHorizontalScrollIndicator:(BOOL)show
{
	self.hasHorizontalScroller = show;
}

- (BOOL)showsVerticalScrollIndicator
{
	return self.hasVerticalScroller;
}

- (void)setShowsVerticalScrollIndicator:(BOOL)show
{
	self.hasVerticalScroller = show;
}

- (UIEdgeInsets)scrollIndicatorInsets
{
	return self.scrollerInsets;
}

- (void)setScrollIndicatorInsets:(UIEdgeInsets)insets
{
	self.scrollerInsets = insets;
}

- (CGFloat)zoomScale
{
  return self.magnification;
}

- (void)setZoomScale:(CGFloat)zoomScale
{
  self.magnification = zoomScale;
}

- (CGFloat)maximumZoomScale
{
  return self.maxMagnification;
}

- (void)setMaximumZoomScale:(CGFloat)maximumZoomScale
{
  self.maxMagnification = maximumZoomScale;
}

- (CGFloat)minimumZoomScale
{
  return self.minMagnification;
}

- (void)setMinimumZoomScale:(CGFloat)minimumZoomScale
{
  self.minMagnification = minimumZoomScale;
}


- (BOOL)alwaysBounceHorizontal
{
  return self.horizontalScrollElasticity != NSScrollElasticityNone;
}

- (void)setAlwaysBounceHorizontal:(BOOL)alwaysBounceHorizontal
{
  self.horizontalScrollElasticity = alwaysBounceHorizontal ? NSScrollElasticityAllowed : NSScrollElasticityNone;
}

- (BOOL)alwaysBounceVertical
{
  return self.verticalScrollElasticity != NSScrollElasticityNone;
}

- (void)setAlwaysBounceVertical:(BOOL)alwaysBounceVertical
{
  self.verticalScrollElasticity = alwaysBounceVertical ? NSScrollElasticityAllowed : NSScrollElasticityNone;
}

// [macOS] bounces property bridge
- (BOOL)bounces
{
  return self.horizontalScrollElasticity != NSScrollElasticityNone || 
         self.verticalScrollElasticity != NSScrollElasticityNone;
}

- (void)setBounces:(BOOL)bounces
{
  NSScrollElasticity elasticity = bounces ? NSScrollElasticityAllowed : NSScrollElasticityNone;
  self.horizontalScrollElasticity = elasticity;
  self.verticalScrollElasticity = elasticity;
}

// [macOS] pagingEnabled property bridge
- (BOOL)pagingEnabled
{
  static char kPagingEnabledKey;
  NSNumber *value = objc_getAssociatedObject(self, &kPagingEnabledKey);
  return value ? [value boolValue] : NO;
}

- (void)setPagingEnabled:(BOOL)pagingEnabled
{
  static char kPagingEnabledKey;
  objc_setAssociatedObject(self, &kPagingEnabledKey, @(pagingEnabled), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

// [macOS] Store last scroll event for mouse location tracking
- (NSEvent *)rct_lastScrollEvent
{
  static char kLastScrollEventKey;
  return objc_getAssociatedObject(self, &kLastScrollEventKey);
}

- (void)rct_setLastScrollEvent:(NSEvent *)event
{
  static char kLastScrollEventKey;
  objc_setAssociatedObject(self, &kLastScrollEventKey, event, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

// [macOS] Get current mouse location in given view (simulates touch location)
- (CGPoint)rct_mouseLocationInView:(UIView *)view
{
  NSEvent *lastEvent = [self rct_lastScrollEvent];
  NSPoint locationInWindow;
  
  if (lastEvent) {
    locationInWindow = lastEvent.locationInWindow;
  } else {
    // Fallback: get current mouse position outside of event stream
    locationInWindow = [[self window] mouseLocationOutsideOfEventStream];
  }
  
  if (view && self.window) {
    NSPoint locationInView = [view convertPoint:locationInWindow fromView:nil];
    return NSPointToCGPoint(locationInView);
  }
  
  return CGPointZero;
}
// macOS]

@end

#pragma mark - RCTUIScrollView (Delegate Bridge)

@implementation RCTUIScrollView (DelegateBridge)

// [macOS] Public readonly properties for UIScrollView compatibility
- (BOOL)isDragging
{
  return [self rct_isDragging];
}

- (BOOL)isDecelerating
{
  return [self rct_isDecelerating];
}
// macOS]

// [macOS] Internal state tracking using associated objects
- (BOOL)rct_isDragging
{
  static char kDraggingKey;
  NSNumber *v = objc_getAssociatedObject(self, &kDraggingKey);
  return v.boolValue;
}

- (void)rct_setDragging:(BOOL)dragging
{
  static char kDraggingKey;
  objc_setAssociatedObject(self, &kDraggingKey, @(dragging), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)rct_isDecelerating
{
  static char kDeceleratingKey;
  NSNumber *v = objc_getAssociatedObject(self, &kDeceleratingKey);
  return v.boolValue;
}

- (void)rct_setDecelerating:(BOOL)decelerating
{
  static char kDeceleratingKey;
  objc_setAssociatedObject(self, &kDeceleratingKey, @(decelerating), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}
// macOS]

- (void)rct_contentViewBoundsDidChange:(NSNotification *)__unused note
{
  if ([self.delegate respondsToSelector:@selector(scrollViewDidScroll:)]) {
    [self.delegate scrollViewDidScroll:(UIScrollView *)self];
  }
}

// [macOS] Snap to nearest page boundary when pagingEnabled is YES
- (void)rct_snapToNearestPage
{
  CGRect bounds = self.bounds;
  CGPoint currentOffset = self.contentOffset;
  CGSize pageSize = bounds.size;
  
  // Calculate the page index for both directions
  CGFloat pageX = round(currentOffset.x / pageSize.width);
  CGFloat pageY = round(currentOffset.y / pageSize.height);
  
  // Calculate target offset aligned to page boundaries
  CGPoint targetOffset = CGPointMake(
    pageX * pageSize.width,
    pageY * pageSize.height
  );
  
  // Clamp to valid scroll range
  CGSize contentSize = self.contentSize;
  UIEdgeInsets contentInset = self.contentInset;
  
  CGFloat maxOffsetX = MAX(0, contentSize.width - pageSize.width + contentInset.right);
  CGFloat maxOffsetY = MAX(0, contentSize.height - pageSize.height + contentInset.bottom);
  
  targetOffset.x = MAX(-contentInset.left, MIN(targetOffset.x, maxOffsetX));
  targetOffset.y = MAX(-contentInset.top, MIN(targetOffset.y, maxOffsetY));
  
  // Animate to target page if different from current offset
  if (!CGPointEqualToPoint(targetOffset, currentOffset)) {
    [self setContentOffset:targetOffset animated:YES];
  }
}
// macOS]

- (void)scrollWheel:(NSEvent *)event
{
  // [macOS] Store event for mouse location tracking
  [self rct_setLastScrollEvent:event];
  // macOS]
  
  // Begin dragging
  if (![self rct_isDragging] && event.phase != NSEventPhaseNone) {
    [self rct_setDragging:YES];
    if ([self.delegate respondsToSelector:@selector(scrollViewWillBeginDragging:)]) {
      [self.delegate scrollViewWillBeginDragging:(UIScrollView *)self];
    }
  }

  // Will end dragging: provide velocity and targetContentOffset approximation
  if (event.phase == NSEventPhaseEnded && event.momentumPhase == NSEventPhaseNone) {
    CGPoint velocity = CGPointMake((CGFloat)event.scrollingDeltaX, (CGFloat)event.scrollingDeltaY);
    CGPoint target = self.contentOffset; // best-effort current offset
    if ([self.delegate respondsToSelector:@selector(scrollViewWillEndDragging:withVelocity:targetContentOffset:)]) {
      [self.delegate scrollViewWillEndDragging:(UIScrollView *)self withVelocity:velocity targetContentOffset:&target];
    }
    // If delegate adjusted target, honor it
    if (!CGPointEqualToPoint(target, self.contentOffset)) {
      [self setContentOffset:target animated:NO];
    }
  }

  // Pass event to super to actually scroll
  [super scrollWheel:event];

  // Momentum begin: decelerate
  if ([self rct_isDragging] && event.momentumPhase == NSEventPhaseBegan) {
    if ([self.delegate respondsToSelector:@selector(scrollViewDidEndDragging:willDecelerate:)]) {
      [self.delegate scrollViewDidEndDragging:(UIScrollView *)self willDecelerate:YES];
    }
    [self rct_setDragging:NO];
    [self rct_setDecelerating:YES];
  }

  // No momentum: end dragging without decelerate
  if ([self rct_isDragging] && event.phase == NSEventPhaseEnded && event.momentumPhase == NSEventPhaseNone) {
    if ([self.delegate respondsToSelector:@selector(scrollViewDidEndDragging:willDecelerate:)]) {
      [self.delegate scrollViewDidEndDragging:(UIScrollView *)self willDecelerate:NO];
    }
    [self rct_setDragging:NO];
    
    // [macOS] Apply paging snap after drag ends without momentum
    if (self.pagingEnabled) {
      [self rct_snapToNearestPage];
    }
    // macOS]
  }

  // Momentum end: deceleration done
  if ([self rct_isDecelerating] && event.momentumPhase == NSEventPhaseEnded) {
    if ([self.delegate respondsToSelector:@selector(scrollViewDidEndDecelerating:)]) {
      [self.delegate scrollViewDidEndDecelerating:(UIScrollView *)self];
    }
    [self rct_setDecelerating:NO];
    
    // [macOS] Apply paging snap after momentum ends
    if (self.pagingEnabled) {
      [self rct_snapToNearestPage];
    }
    // macOS]
  }
}

@end

BOOL RCTUIViewSetClipsToBounds(RCTPlatformView *view)
{
  // NSViews are always clipped to bounds
  BOOL clipsToBounds = YES;

  // But see if UIView overrides that behavior
  if ([view respondsToSelector:@selector(clipsToBounds)])
  {
    clipsToBounds = [(id)view clipsToBounds];
  }

  return clipsToBounds;
}

@implementation RCTClipView

- (instancetype)initWithFrame:(NSRect)frameRect
{
   if (self = [super initWithFrame:frameRect]) {
    self.constrainScrolling = NO;
    self.drawsBackground = NO;
  }
  
  return self;
}

- (NSRect)constrainBoundsRect:(NSRect)proposedBounds
{
  if (self.constrainScrolling) {
    return NSMakeRect(0, 0, 0, 0);
  }
  
  return [super constrainBoundsRect:proposedBounds];
}

@end

// RCTUISlider

@implementation RCTUISlider {}

- (void)setValue:(float)value animated:(__unused BOOL)animated
{
  self.animator.floatValue = value;
}

@end


// RCTUILabel

@implementation RCTUILabel {}

- (instancetype)initWithFrame:(NSRect)frameRect
{
  if (self = [super initWithFrame:frameRect]) {
    [self setBezeled:NO];
    [self setDrawsBackground:NO];
    [self setEditable:NO];
    [self setSelectable:NO];
    [self setWantsLayer:YES];
  }
  
  return self;
}

- (void)setText:(NSString *)text
{
  [self setStringValue:text];
}

@end

@implementation RCTUISwitch

- (BOOL)isOn
{
	return self.state == NSControlStateValueOn;
}

- (void)setOn:(BOOL)on
{
	[self setOn:on animated:NO];
}

- (void)setOn:(BOOL)on animated:(BOOL)animated {
	self.state = on ? NSControlStateValueOn : NSControlStateValueOff;
}

@end

// [macOS] NSValue CGSizeValue shim to match UIKit API surface
// [macOS] NSValue geometry compatibility - map iOS CGXxxValue to macOS native methods
@implementation NSValue (KUCGGeometryCompat)

- (CGPoint)CGPointValue {
  // On macOS, CGPoint is NSPoint, use native pointValue
  return [self pointValue];
}

- (CGSize)CGSizeValue {
  // On macOS, CGSize is NSSize, use native sizeValue
  return [self sizeValue];
}

- (CGRect)CGRectValue {
  // On macOS, CGRect is NSRect, use native rectValue
  return [self rectValue];
}

@end
// macOS]

// RCTUIActivityIndicatorView

@interface RCTUIActivityIndicatorView ()
@property (nonatomic, readwrite, getter=isAnimating) BOOL animating;
@end

@implementation RCTUIActivityIndicatorView {}

- (instancetype)initWithFrame:(CGRect)frame
{
  if ((self = [super initWithFrame:frame])) {
    self.displayedWhenStopped = NO;
    self.style = NSProgressIndicatorStyleSpinning;
  }
  return self;
}

- (void)startAnimating
{
  // `wantsLayer` gets reset after the animation is stopped. We have to
  // reset it in order for CALayer filters to take effect.
  [self setWantsLayer:YES];
  [self startAnimation:self];
}

- (void)stopAnimating
{
  [self stopAnimation:self];
}

- (void)startAnimation:(id)sender
{
  [super startAnimation:sender];
  self.animating = YES;
}

- (void)stopAnimation:(id)sender
{
  [super stopAnimation:sender];
  self.animating = NO;
}

- (void)setActivityIndicatorViewStyle:(UIActivityIndicatorViewStyle)activityIndicatorViewStyle
{
  _activityIndicatorViewStyle = activityIndicatorViewStyle;
  
  switch (activityIndicatorViewStyle) {
    case UIActivityIndicatorViewStyleLarge:
      if (@available(macos 11.0, *)) {
        self.controlSize = NSControlSizeLarge;
      } else {
        self.controlSize = NSControlSizeRegular;
      }
      break;
    case UIActivityIndicatorViewStyleMedium:
      self.controlSize = NSControlSizeRegular;
      break;
    default:
      break;
  }
}

- (void)setColor:(RCTUIColor*)color
{
  if (_color != color) {
    _color = color;
    [self setNeedsDisplay:YES];
  }
}

- (void)updateLayer
{
  [super updateLayer];
  if (_color != nil) {
    CGFloat r, g, b, a;
    [[_color colorUsingColorSpace:[NSColorSpace genericRGBColorSpace]] getRed:&r green:&g blue:&b alpha:&a];

    CIFilter *colorPoly = [CIFilter filterWithName:@"CIColorPolynomial"];
    [colorPoly setDefaults];
    
    CIVector *redVector = [CIVector vectorWithX:r Y:0 Z:0 W:0];
    CIVector *greenVector = [CIVector vectorWithX:g Y:0 Z:0 W:0];
    CIVector *blueVector = [CIVector vectorWithX:b Y:0 Z:0 W:0];
    [colorPoly setValue:redVector forKey:@"inputRedCoefficients"];
    [colorPoly setValue:greenVector forKey:@"inputGreenCoefficients"];
    [colorPoly setValue:blueVector forKey:@"inputBlueCoefficients"];
    
    [[self layer] setFilters:@[colorPoly]];
  } else {
    [[self layer] setFilters:nil];
  }
}

- (void)setHidesWhenStopped:(BOOL)hidesWhenStopped
{
  self.displayedWhenStopped = !hidesWhenStopped;
}

- (BOOL)hidesWhenStopped
{
  return !self.displayedWhenStopped;
}

- (void)setHidden:(BOOL)hidden
{
  if ([self hidesWhenStopped] && ![self isAnimating]) {
    [super setHidden:YES];
  } else {
    [super setHidden:hidden];
  }
}

// [macOS] 添加 layoutSubviews 方法实现，以兼容 iOS UIImageView 的行为
- (void)layoutSubviews
{
  // 在 macOS 上，NSImageView 不需要显式的 layoutSubviews 调用
  // 但为了兼容 iOS 代码，我们提供一个空实现
  // 如果需要，可以在这里添加特定的布局逻辑
}

// [macOS] 重写 setFrame 方法，确保在布局变化时触发相应的回调
- (void)setFrame:(NSRect)frame
{
  NSRect oldFrame = self.frame;
  [super setFrame:frame];
  
  // 当 frame 发生变化时，触发 layoutSubviews 以保持与 iOS 行为一致
  if (!NSEqualRects(oldFrame, frame)) {
    [self layoutSubviews];
  }
}

// [macOS] 重写 setBounds 方法，确保在 bounds 变化时触发相应的回调
- (void)setBounds:(NSRect)bounds
{
  NSRect oldBounds = self.bounds;
  [super setBounds:bounds];
  
  // 当 bounds 发生变化时，触发 layoutSubviews 以保持与 iOS 行为一致
  if (!NSEqualRects(oldBounds, bounds)) {
    [self layoutSubviews];
  }
}

@end

// RCTUIImageView

@implementation RCTUIImageView {
  CALayer *_tintingLayer;
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    [self setLayer:[[CALayer alloc] init]];
    [self setWantsLayer:YES];
  }
  
  return self;
}

// [macOS] 添加 initWithImage: 方法实现，以兼容 iOS UIImageView 的行为
- (instancetype)initWithImage:(UIImage *)image {
  if (self = [self initWithFrame:CGRectZero]) {
    [self setImage:image];
    if (image) {
      [self setFrame:CGRectMake(0, 0, image.size.width, image.size.height)];
    }
  }
  return self;
}
// macOS]

- (BOOL)clipsToBounds
{
  return [[self layer] masksToBounds];
}

- (void)setClipsToBounds:(BOOL)clipsToBounds
{
  [[self layer] setMasksToBounds:clipsToBounds];
}

- (void)setContentMode:(UIViewContentMode)contentMode
{
  _contentMode = contentMode;
  
  CALayer *layer = [self layer];
  switch (contentMode) {
    case UIViewContentModeScaleAspectFill:
      [layer setContentsGravity:kCAGravityResizeAspectFill];
      break;
      
    case UIViewContentModeScaleAspectFit:
      [layer setContentsGravity:kCAGravityResizeAspect];
      break;
      
    case UIViewContentModeScaleToFill:
      [layer setContentsGravity:kCAGravityResize];
      break;
      
    case UIViewContentModeCenter:
      [layer setContentsGravity:kCAGravityCenter];
      break;
    
    default:
      break;
  }
}

- (UIImage *)image
{
  return [[self layer] contents];
}

- (void)setImage:(UIImage *)image
{
  CALayer *layer = [self layer];
  
  if ([layer contents] != image || [layer backgroundColor] != nil) {
    if (_tintColor) {
      if (!_tintingLayer) {
        _tintingLayer = [CALayer new];
        [_tintingLayer setFrame:self.bounds];
        [_tintingLayer setAutoresizingMask:kCALayerWidthSizable | kCALayerHeightSizable];
        [_tintingLayer setZPosition:1.0];
        CIFilter *sourceInCompositingFilter = [CIFilter filterWithName:@"CISourceInCompositing"];
        [sourceInCompositingFilter setDefaults];
        [_tintingLayer setCompositingFilter:sourceInCompositingFilter];
        [layer addSublayer:_tintingLayer];
      }
      [_tintingLayer setBackgroundColor:_tintColor.CGColor];
    } else {
      [_tintingLayer removeFromSuperlayer];
      _tintingLayer = nil;
    }
    
    if (image != nil && [image resizingMode] == NSImageResizingModeTile) {
      [layer setContents:nil];
      [layer setBackgroundColor:[NSColor colorWithPatternImage:image].CGColor];
    } else {
      [layer setContents:image];
      [layer setBackgroundColor:nil];
    }
  }
}

// [macOS] 添加 insertSubview:atIndex: 方法实现，以兼容 iOS UIImageView 的行为
- (void)insertSubview:(NSView *)view atIndex:(NSInteger)index
{
  NSArray<__kindof NSView *> *subviews = self.subviews;
  if ((NSUInteger)index == subviews.count) {
    [self addSubview:view];
  } else {
    [self addSubview:view positioned:NSWindowBelow relativeTo:subviews[index]];
  }
}

@end

@implementation RCTUIGraphicsImageRendererFormat

+ (nonnull instancetype)defaultFormat {
    RCTUIGraphicsImageRendererFormat *format = [RCTUIGraphicsImageRendererFormat new];
    return format;
}

@end

@implementation RCTUIGraphicsImageRenderer
{
    CGSize _size;
    RCTUIGraphicsImageRendererFormat *_format;
}

- (nonnull instancetype)initWithSize:(CGSize)size {
    if (self = [super init]) {
        self->_size = size;
    }
    return self;
}

- (nonnull instancetype)initWithSize:(CGSize)size format:(nonnull RCTUIGraphicsImageRendererFormat *)format {
    if (self = [super init]) {
        self->_size = size;
        self->_format = format;
    }
    return self;
}

- (nonnull NSImage *)imageWithActions:(NS_NOESCAPE RCTUIGraphicsImageDrawingActions)actions {

    NSImage *image = [NSImage imageWithSize:_size
                                    flipped:YES
                             drawingHandler:^BOOL(NSRect dstRect) {
        
        RCTUIGraphicsImageRendererContext *context = [NSGraphicsContext currentContext];
        if (self->_format.opaque) {
            CGContextSetAlpha([context CGContext], 1.0);
        }
        actions(context);
        return YES;
    }];
    return image;
}

@end

#endif
