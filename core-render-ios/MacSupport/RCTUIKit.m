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

#if TARGET_OS_OSX

#define RCTAssert(...) // TODO

#import "RCTUIKit.h" // [macOS]
#import <QuartzCore/QuartzCore.h> // [macOS] for CATransaction
#import <objc/runtime.h>
#import <objc/message.h>
#import <CoreImage/CIFilter.h>
#import <CoreImage/CIVector.h>

static char RCTGraphicsContextSizeKey;

#pragma mark - Graphics Context Functions

CGContextRef UIGraphicsGetCurrentContext(void) {
    return [[NSGraphicsContext currentContext] CGContext];
}

NSImage *UIGraphicsGetImageFromCurrentImageContext(void) {
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

#pragma mark - Image Functions

CGFloat UIImageGetScale(NSImage *image) {
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

CGImageRef __nullable UIImageGetCGImageRef(NSImage *image) {
    return [image CGImageForProposedRect:NULL context:NULL hints:NULL];
}

static NSData *NSImageDataForFileType(NSImage *image, NSBitmapImageFileType fileType, NSDictionary<NSString *, id> *properties) {
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

#pragma mark - NSImage (KRUIImageCompat)

// [macOS] NSImage category to mimic common UIImage constructors
@implementation NSImage (KRUIImageCompat)

+ (instancetype)imageWithCGImage:(CGImageRef)cgImage {
    if (!cgImage) {
        return nil;
    }
    return [[NSImage alloc] initWithCGImage:cgImage size:NSZeroSize];
}

+ (instancetype)imageWithData:(NSData *)data {
    return [[NSImage alloc] initWithData:data];
}

+ (instancetype)imageWithContentsOfFile:(NSString *)filePath {
    return [[NSImage alloc] initWithContentsOfFile:filePath];
}

@end


NSImage *UIImageResizableImageWithCapInsets(NSImage *image, NSEdgeInsets capInsets, UIImageResizingMode resizingMode) {
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


#pragma mark - UIBezierPath Functions

UIBezierPath *UIBezierPathWithRoundedRect(CGRect rect, CGFloat cornerRadius) {
    return [NSBezierPath bezierPathWithRoundedRect:rect xRadius:cornerRadius yRadius:cornerRadius];
}

void UIBezierPathAppendPath(UIBezierPath *path, UIBezierPath *appendPath) {
    return [path appendBezierPath:appendPath];
}

#pragma mark - NSFont UIKit Compatibility

// Provide UIKit-like lineHeight on NSFont so call sites can use font.lineHeight
@interface NSFont (KRUIKitCompatLineHeight)
- (CGFloat)lineHeight;
@end

@implementation NSFont (KRUIKitCompatLineHeight)
- (CGFloat)lineHeight {
    // NSFont doesn't expose lineHeight; synthesize via ascender/descender/leading
    CGFloat h = self.ascender + fabs(self.descender) + self.leading;
    return ceil(h);
}
@end

#pragma mark - RCTUIView

// [macOS] RCTUIView implementation - provides macOS-specific extensions
// Note: Most UIKit compatibility is handled by NSView+KRCompat Category
@implementation RCTUIView {
@private
    BOOL _hasMouseOver;
    NSTrackingArea *_trackingArea;
    BOOL _mouseDownCanMoveWindow;
}

#pragma mark Initialization

static RCTUIView *RCTUIViewCommonInit(RCTUIView *self) {
    if (self != nil) {
        self.wantsLayer = YES;
        self->_enableFocusRing = YES;
        self->_mouseDownCanMoveWindow = YES;
    }
    return self;
}

- (instancetype)initWithFrame:(NSRect)frameRect {
    return RCTUIViewCommonInit([super initWithFrame:frameRect]);
}

- (instancetype)initWithCoder:(NSCoder *)coder {
    return RCTUIViewCommonInit([super initWithCoder:coder]);
}

#pragma mark First Responder Handling

- (BOOL)acceptsFirstMouse:(NSEvent *)event {
    if (self.acceptsFirstMouse || [super acceptsFirstMouse:event]) {
        return YES;
    }
    
    // If any RCTUIView above has acceptsFirstMouse set, then return YES here
    NSView *view = self;
    while ((view = view.superview)) {
        if ([view isKindOfClass:[RCTUIView class]] && [(RCTUIView *)view acceptsFirstMouse]) {
            return YES;
        }
    }
    
    return NO;
}

- (BOOL)acceptsFirstResponder {
    return [self canBecomeFirstResponder];
}

- (BOOL)isFirstResponder {
    return [[self window] firstResponder] == self;
}

- (BOOL)canBecomeFirstResponder {
    return [super acceptsFirstResponder];
}

- (BOOL)becomeFirstResponder {
    return [[self window] makeFirstResponder:self];
}

#pragma mark View Lifecycle

- (void)viewDidMoveToWindow {
    // Subscribe to view bounds changed notification so that the view can be notified when a
    // scroll event occurs either due to trackpad/gesture based scrolling or a scrollwheel event
    // both of which would not cause the mouseExited to be invoked
    
    if ([self window] == nil) {
        [[NSNotificationCenter defaultCenter] removeObserver:self
                                                        name:NSViewBoundsDidChangeNotification
                                                      object:nil];
    } else if ([[self enclosingScrollView] contentView] != nil) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(viewBoundsChanged:)
                                                     name:NSViewBoundsDidChangeNotification
                                                   object:[[self enclosingScrollView] contentView]];
    }
    
    // TODO: Implement reactViewDidMoveToWindow if needed
    // [macOS] didMoveToWindow is handled by NSView+KRCompat swizzling
    [super viewDidMoveToWindow];
}

- (void)viewBoundsChanged:(NSNotification *)__unused inNotif {
    // TODO: Implement mouse hover tracking logic when needed
    // When an enclosing scrollview is scrolled using the scrollWheel or trackpad,
    // the mouseExited: event does not get called on the view where mouseEntered: was previously called.
    // This creates an unnatural pairing of mouse enter and exit events and can cause problems.
    // We therefore explicitly check for this here and handle them by calling the appropriate callbacks.
}

#pragma mark Mouse Event Handling

- (BOOL)hasMouseHoverEvent {
    // [macOS] Disabled for now to avoid selector warnings
    return NO;
}

- (NSDictionary *)locationInfoFromDraggingLocation:(NSPoint)locationInWindow {
    NSPoint locationInView = [self convertPoint:locationInWindow fromView:nil];
    
    return @{
        @"screenX": @(locationInWindow.x),
        @"screenY": @(locationInWindow.y),
        @"clientX": @(locationInView.x),
        @"clientY": @(locationInView.y)
    };
}

- (NSDictionary *)locationInfoFromEvent:(NSEvent *)event {
    NSPoint locationInWindow = event.locationInWindow;
    NSPoint locationInView = [self convertPoint:locationInWindow fromView:nil];
    
    return @{
        @"screenX": @(locationInWindow.x),
        @"screenY": @(locationInWindow.y),
        @"clientX": @(locationInView.x),
        @"clientY": @(locationInView.y)
    };
}

- (void)mouseEntered:(NSEvent *)event {
    _hasMouseOver = YES;
    // TODO: Implement mouse enter event callback when needed
}

- (void)mouseExited:(NSEvent *)event {
    _hasMouseOver = NO;
    // TODO: Implement mouse leave event callback when needed
}

- (void)updateTrackingAreas {
    BOOL hasMouseHoverEvent = [self hasMouseHoverEvent];
    BOOL wouldRecreateIdenticalTrackingArea = hasMouseHoverEvent && _trackingArea && NSEqualRects(self.bounds, [_trackingArea rect]);
    
    if (!wouldRecreateIdenticalTrackingArea) {
        if (_trackingArea) {
            [self removeTrackingArea:_trackingArea];
        }
        
        if (hasMouseHoverEvent) {
            _trackingArea = [[NSTrackingArea alloc] initWithRect:self.bounds
                                                         options:NSTrackingActiveAlways | NSTrackingMouseEnteredAndExited
                                                           owner:self
                                                        userInfo:nil];
            [self addTrackingArea:_trackingArea];
        }
    }
    
    [super updateTrackingAreas];
}

#pragma mark Properties

- (BOOL)mouseDownCanMoveWindow {
    return _mouseDownCanMoveWindow;
}

- (void)setMouseDownCanMoveWindow:(BOOL)mouseDownCanMoveWindow {
    _mouseDownCanMoveWindow = mouseDownCanMoveWindow;
}

- (BOOL)isFlipped {
    return YES;
}

- (CGFloat)alpha {
    return self.alphaValue;
}

- (void)setAlpha:(CGFloat)alpha {
    self.alphaValue = alpha;
}

- (CGAffineTransform)transform {
    return self.layer.affineTransform;
}

- (void)setTransform:(CGAffineTransform)transform {
    self.layer.affineTransform = transform;
}

#pragma mark Hit Testing

- (NSView *)hitTest:(NSPoint)point {
    // IMPORTANT: point is passed in superview coordinates by macOS, but expected to be passed in local coordinates
    NSView *superview = [self superview];
    NSPoint pointInSelf = superview != nil ? [self convertPoint:point fromView:superview] : point;
    return [self hitTest:pointInSelf withEvent:nil];
}

#pragma mark Layer Backing

- (BOOL)wantsUpdateLayer {
    return [self respondsToSelector:@selector(displayLayer:)];
}

- (void)updateLayer {
    CALayer *layer = [self layer];
    // [macOS] backgroundColor is handled by NSView+KRCompat
    if (self.backgroundColor) {
        // updateLayer will be called when the view's current appearance changes.
        // The layer's backgroundColor is a CGColor which is not appearance aware
        // so it has to be reset from the view's NSColor.
        [layer setBackgroundColor:[self.backgroundColor CGColor]];
    }
    [(id<CALayerDelegate>)self displayLayer:layer];
}

- (void)drawRect:(CGRect)rect {
    // [macOS] backgroundColor is handled by NSView+KRCompat
    [super drawRect:rect];
}

- (void)layout {
    [super layout];
    // [macOS] layoutSubviews is handled by NSView+KRCompat
}

#pragma mark Layout Methods

- (void)setNeedsLayout {
    self.needsLayout = YES;
}

- (void)setNeedsDisplay {
    self.needsDisplay = YES;
}

#pragma mark Cursor

// We purposely don't use RCTCursor for the parameter type here because it would introduce an import cycle:
// RCTUIKit > RCTCursor > RCTConvert > RCTUIKit
- (void)setCursor:(NSInteger)cursor {
    // This method is required to be defined due to [RCTVirtualTextViewManager view] returning a RCTUIView.
}

@end

#pragma mark - RCTPlatformView (AnimationCompat)

@implementation RCTPlatformView (AnimationCompat)

+ (void)animateWithDuration:(NSTimeInterval)duration
                      delay:(NSTimeInterval)delay
                    options:(UIViewAnimationOptions)options
                 animations:(void (^)(void))animations
                 completion:(void (^ __nullable)(BOOL finished))completion {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [CATransaction begin];
        [CATransaction setAnimationDuration:duration];
        if (animations) {
            animations();
        }
        [CATransaction commit];
        if (completion) {
            completion(YES);
        }
    });
}

+ (void)animateWithDuration:(NSTimeInterval)duration
                      delay:(NSTimeInterval)delay
     usingSpringWithDamping:(CGFloat)damping
      initialSpringVelocity:(CGFloat)velocity
                    options:(UIViewAnimationOptions)options
                 animations:(void (^)(void))animations
                 completion:(void (^ __nullable)(BOOL finished))completion {
    // Simplified: treat as normal animation
    [self animateWithDuration:duration delay:delay options:options animations:animations completion:completion];
}

+ (void)animateKeyframesWithDuration:(NSTimeInterval)duration
                                delay:(NSTimeInterval)delay
                              options:(UIViewKeyframeAnimationOptions)options
                           animations:(void (^)(void))animations
                           completion:(void (^ __nullable)(BOOL finished))completion {
    [self animateWithDuration:duration delay:delay options:(UIViewAnimationOptions)options animations:animations completion:completion];
}

static NSMutableArray<void (^)(void)> *g_keyframeBlocks;

+ (void)addKeyframeWithRelativeStartTime:(double)frameStartTime
                        relativeDuration:(double)frameDuration
                               animations:(void (^)(void))animations {
    if (!g_keyframeBlocks) {
        g_keyframeBlocks = [NSMutableArray array];
    }
    if (animations) {
        [g_keyframeBlocks addObject:[animations copy]];
    }
}

+ (void)setAnimationCurve:(UIViewAnimationCurve)curve {
    // Minimal compatibility: no additional processing
}

@end

#pragma mark - RCTUIScrollView

@implementation RCTUIScrollView

#pragma mark Initialization and Deallocation

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self name:NSViewBoundsDidChangeNotification object:self.contentView];
}

- (instancetype)initWithFrame:(CGRect)frame {
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

#pragma mark Layout Bridge

// [macOS] Bridge UIView layout methods for UIScrollView subclasses
// Note: NSView+KRCompat provides layoutSubviews (which calls layout), but we need
// the reverse direction for RCTUIScrollView: when NSView's layout is called, we need
// to call the UIKit-style layoutSubviews so subclasses like KRScrollView can override it.
// Lifecycle methods (didMoveToSuperview, didMoveToWindow) are handled by NSView+KRCompat swizzling.

- (void)layout {
    [super layout];
    // Bridge NSView's layout to UIView's layoutSubviews
    if (self.window != nil) {
        [self layoutSubviews];
    }
}

- (void)layoutSubviews {
    // Subclasses (like KRScrollView) override this for UIView-style layout
}
// macOS]

#pragma mark Focus Ring

- (void)setEnableFocusRing:(BOOL)enableFocusRing {
    if (_enableFocusRing != enableFocusRing) {
        _enableFocusRing = enableFocusRing;
    }
    
    if (enableFocusRing) {
        // NSTextView has no focus ring by default so let's use the standard Aqua focus ring
        [self setFocusRingType:NSFocusRingTypeExterior];
    } else {
        [self setFocusRingType:NSFocusRingTypeNone];
    }
}

#pragma mark UIScrollView Property Bridges

- (CGPoint)contentOffset {
    return self.documentVisibleRect.origin;
}

- (void)setContentOffset:(CGPoint)contentOffset {
    [self.documentView scrollPoint:contentOffset];
}

- (void)setContentOffset:(CGPoint)contentOffset animated:(BOOL)animated {
    if (animated) {
        [NSAnimationContext runAnimationGroup:^(NSAnimationContext *context) {
            context.duration = 0.3;
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

- (UIEdgeInsets)contentInset {
    return super.contentInsets;
}

- (void)setContentInset:(UIEdgeInsets)insets {
    super.contentInsets = insets;
}

- (CGSize)contentSize {
    return self.documentView.frame.size;
}

- (void)setContentSize:(CGSize)contentSize {
    CGRect frame = self.documentView.frame;
    frame.size = contentSize;
    self.documentView.frame = frame;
}

- (BOOL)showsHorizontalScrollIndicator {
    return self.hasHorizontalScroller;
}

- (void)setShowsHorizontalScrollIndicator:(BOOL)show {
    self.hasHorizontalScroller = show;
}

- (BOOL)showsVerticalScrollIndicator {
    return self.hasVerticalScroller;
}

- (void)setShowsVerticalScrollIndicator:(BOOL)show {
    self.hasVerticalScroller = show;
}

- (UIEdgeInsets)scrollIndicatorInsets {
    return self.scrollerInsets;
}

- (void)setScrollIndicatorInsets:(UIEdgeInsets)insets {
    self.scrollerInsets = insets;
}

- (CGFloat)zoomScale {
    return self.magnification;
}

- (void)setZoomScale:(CGFloat)zoomScale {
    self.magnification = zoomScale;
}

- (CGFloat)maximumZoomScale {
    return self.maxMagnification;
}

- (void)setMaximumZoomScale:(CGFloat)maximumZoomScale {
    self.maxMagnification = maximumZoomScale;
}

- (CGFloat)minimumZoomScale {
    return self.minMagnification;
}

- (void)setMinimumZoomScale:(CGFloat)minimumZoomScale {
    self.minMagnification = minimumZoomScale;
}

- (BOOL)alwaysBounceHorizontal {
    return self.horizontalScrollElasticity != NSScrollElasticityNone;
}

- (void)setAlwaysBounceHorizontal:(BOOL)alwaysBounceHorizontal {
    self.horizontalScrollElasticity = alwaysBounceHorizontal ? NSScrollElasticityAllowed : NSScrollElasticityNone;
}

- (BOOL)alwaysBounceVertical {
    return self.verticalScrollElasticity != NSScrollElasticityNone;
}

- (void)setAlwaysBounceVertical:(BOOL)alwaysBounceVertical {
    self.verticalScrollElasticity = alwaysBounceVertical ? NSScrollElasticityAllowed : NSScrollElasticityNone;
}

// [macOS] bounces property bridge
- (BOOL)bounces {
    return self.horizontalScrollElasticity != NSScrollElasticityNone ||
           self.verticalScrollElasticity != NSScrollElasticityNone;
}

- (void)setBounces:(BOOL)bounces {
    NSScrollElasticity elasticity = bounces ? NSScrollElasticityAllowed : NSScrollElasticityNone;
    self.horizontalScrollElasticity = elasticity;
    self.verticalScrollElasticity = elasticity;
}

// [macOS] pagingEnabled property bridge
- (BOOL)pagingEnabled {
    static char kPagingEnabledKey;
    NSNumber *value = objc_getAssociatedObject(self, &kPagingEnabledKey);
    return value ? [value boolValue] : NO;
}

- (void)setPagingEnabled:(BOOL)pagingEnabled {
    static char kPagingEnabledKey;
    objc_setAssociatedObject(self, &kPagingEnabledKey, @(pagingEnabled), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

#pragma mark Mouse Location Tracking

// [macOS] Store last scroll event for mouse location tracking
- (NSEvent *)rct_lastScrollEvent {
    static char kLastScrollEventKey;
    return objc_getAssociatedObject(self, &kLastScrollEventKey);
}

- (void)rct_setLastScrollEvent:(NSEvent *)event {
    static char kLastScrollEventKey;
    objc_setAssociatedObject(self, &kLastScrollEventKey, event, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

// [macOS] Get current mouse location in given view (simulates touch location)
- (CGPoint)rct_mouseLocationInView:(UIView *)view {
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

#pragma mark - RCTUIScrollView (DelegateBridge)

@implementation RCTUIScrollView (DelegateBridge)

#pragma mark State Properties

// [macOS] Public readonly properties for UIScrollView compatibility
- (BOOL)isDragging {
    return [self rct_isDragging];
}

- (BOOL)isDecelerating {
    return [self rct_isDecelerating];
}
// macOS]

#pragma mark Internal State Tracking

// [macOS] Internal state tracking using associated objects
- (BOOL)rct_isDragging {
    static char kDraggingKey;
    NSNumber *v = objc_getAssociatedObject(self, &kDraggingKey);
    return v.boolValue;
}

- (void)rct_setDragging:(BOOL)dragging {
    static char kDraggingKey;
    objc_setAssociatedObject(self, &kDraggingKey, @(dragging), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)rct_isDecelerating {
    static char kDeceleratingKey;
    NSNumber *v = objc_getAssociatedObject(self, &kDeceleratingKey);
    return v.boolValue;
}

- (void)rct_setDecelerating:(BOOL)decelerating {
    static char kDeceleratingKey;
    objc_setAssociatedObject(self, &kDeceleratingKey, @(decelerating), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}
// macOS]

#pragma mark Scroll Notifications

- (void)rct_contentViewBoundsDidChange:(NSNotification *)__unused note {
    if ([self.delegate respondsToSelector:@selector(scrollViewDidScroll:)]) {
        [self.delegate scrollViewDidScroll:(UIScrollView *)self];
    }
}

#pragma mark Paging Support

// [macOS] Snap to nearest page boundary when pagingEnabled is YES
- (void)rct_snapToNearestPage {
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

#pragma mark Scroll Wheel Handling

- (void)scrollWheel:(NSEvent *)event {
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
        CGPoint target = self.contentOffset;
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

#pragma mark - View Helper Functions

BOOL RCTUIViewSetClipsToBounds(RCTPlatformView *view) {
    // NSViews are always clipped to bounds
    BOOL clipsToBounds = YES;
    
    // But see if UIView overrides that behavior
    if ([view respondsToSelector:@selector(clipsToBounds)]) {
        clipsToBounds = [(id)view clipsToBounds];
    }
    
    return clipsToBounds;
}

#pragma mark - RCTClipView

@implementation RCTClipView

- (instancetype)initWithFrame:(NSRect)frameRect {
    if (self = [super initWithFrame:frameRect]) {
        self.constrainScrolling = NO;
        self.drawsBackground = NO;
    }
    
    return self;
}

- (NSRect)constrainBoundsRect:(NSRect)proposedBounds {
    if (self.constrainScrolling) {
        return NSMakeRect(0, 0, 0, 0);
    }
    
    return [super constrainBoundsRect:proposedBounds];
}

@end

#pragma mark - RCTUISlider

@implementation RCTUISlider

- (void)setValue:(float)value animated:(__unused BOOL)animated {
    self.animator.floatValue = value;
}

@end

#pragma mark - RCTUILabel

@implementation RCTUILabel

- (instancetype)initWithFrame:(NSRect)frameRect {
    if (self = [super initWithFrame:frameRect]) {
        [self setBezeled:NO];
        [self setDrawsBackground:NO];
        [self setEditable:NO];
        [self setSelectable:NO];
        [self setWantsLayer:YES];
    }
    
    return self;
}

- (void)setText:(NSString *)text {
    [self setStringValue:text];
}

// Bridge UILabel.attributedText <-> NSTextField.attributedStringValue
- (NSAttributedString *)attributedText {
    return [self attributedStringValue];
}

- (void)setAttributedText:(NSAttributedString *)attributedText {
    [self setAttributedStringValue:attributedText ?: [[NSAttributedString alloc] initWithString:@""]];
}

// Bridge iOS UILabel drawing pipeline to call drawTextInRect: if implemented by subclass (e.g. KRLabel)
- (void)drawRect:(NSRect)dirtyRect {
    SEL drawTextSel = @selector(drawTextInRect:);
    if ([self respondsToSelector:drawTextSel]) {
        void (*msgSend)(id, SEL, CGRect) = (void (*)(id, SEL, CGRect))objc_msgSend;
        msgSend(self, drawTextSel, self.bounds);
        return;
    }
    [super drawRect:dirtyRect];
}

// [macOS] Use iOS-like flipped coordinates for consistency with UIKit drawing
- (BOOL)isFlipped {
    return YES;
}

@end

#pragma mark - RCTUISwitch

@implementation RCTUISwitch

- (BOOL)isOn {
    return self.state == NSControlStateValueOn;
}

- (void)setOn:(BOOL)on {
    [self setOn:on animated:NO];
}

- (void)setOn:(BOOL)on animated:(BOOL)animated {
    self.state = on ? NSControlStateValueOn : NSControlStateValueOff;
}

@end

#pragma mark - NSValue (KUCGGeometryCompat)

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

// NSValue class methods to provide UIKit-like factory methods
@implementation NSValue (KRUIKitCompatFactory)

+ (instancetype)valueWithCGSize:(CGSize)size {
    return [NSValue valueWithBytes:&size objCType:@encode(CGSize)];
}

+ (instancetype)valueWithCGRect:(CGRect)rect {
    return [NSValue valueWithBytes:&rect objCType:@encode(CGRect)];
}

+ (instancetype)valueWithCGPoint:(CGPoint)point {
    return [NSValue valueWithBytes:&point objCType:@encode(CGPoint)];
}

@end

#pragma mark - RCTUIActivityIndicatorView

@interface RCTUIActivityIndicatorView ()
@property (nonatomic, readwrite, getter=isAnimating) BOOL animating;
@end

@implementation RCTUIActivityIndicatorView

#pragma mark Initialization

- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.displayedWhenStopped = NO;
        self.style = NSProgressIndicatorStyleSpinning;
    }
    return self;
}

#pragma mark Animation Control

- (void)startAnimating {
    // `wantsLayer` gets reset after the animation is stopped. We have to
    // reset it in order for CALayer filters to take effect.
    [self setWantsLayer:YES];
    [self startAnimation:self];
}

- (void)stopAnimating {
    [self stopAnimation:self];
}

- (void)startAnimation:(id)sender {
    [super startAnimation:sender];
    self.animating = YES;
}

- (void)stopAnimation:(id)sender {
    [super stopAnimation:sender];
    self.animating = NO;
}

#pragma mark Style and Appearance

- (void)setActivityIndicatorViewStyle:(UIActivityIndicatorViewStyle)activityIndicatorViewStyle {
    _activityIndicatorViewStyle = activityIndicatorViewStyle;
    
    switch (activityIndicatorViewStyle) {
        case UIActivityIndicatorViewStyleLarge:
            if (@available(macOS 11.0, *)) {
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

- (void)setColor:(RCTUIColor *)color {
    if (_color != color) {
        _color = color;
        [self setNeedsDisplay:YES];
    }
}

- (void)updateLayer {
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

#pragma mark Visibility

- (void)setHidesWhenStopped:(BOOL)hidesWhenStopped {
    self.displayedWhenStopped = !hidesWhenStopped;
}

- (BOOL)hidesWhenStopped {
    return !self.displayedWhenStopped;
}

- (void)setHidden:(BOOL)hidden {
    if ([self hidesWhenStopped] && ![self isAnimating]) {
        [super setHidden:YES];
    } else {
        [super setHidden:hidden];
    }
}

#pragma mark Layout

// layoutSubviews compatibility for iOS behavior
- (void)layoutSubviews {
    // On macOS, NSProgressIndicator doesn't need explicit layoutSubviews call
    // Provide empty implementation for iOS compatibility
}

- (void)setFrame:(NSRect)frame {
    NSRect oldFrame = self.frame;
    [super setFrame:frame];
    
    // Trigger layoutSubviews when frame changes to maintain iOS behavior
    if (!NSEqualRects(oldFrame, frame)) {
        [self layoutSubviews];
    }
}

- (void)setBounds:(NSRect)bounds {
    NSRect oldBounds = self.bounds;
    [super setBounds:bounds];
    
    // Trigger layoutSubviews when bounds change to maintain iOS behavior
    if (!NSEqualRects(oldBounds, bounds)) {
        [self layoutSubviews];
    }
}

@end

#pragma mark - RCTUIImageView

@implementation RCTUIImageView {
    CALayer *_tintingLayer;
}

#pragma mark Initialization

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self setLayer:[[CALayer alloc] init]];
        [self setWantsLayer:YES];
    }
    
    return self;
}

// initWithImage: compatibility for iOS UIImageView behavior
- (instancetype)initWithImage:(UIImage *)image {
    if (self = [self initWithFrame:CGRectZero]) {
        [self setImage:image];
        if (image) {
            [self setFrame:CGRectMake(0, 0, image.size.width, image.size.height)];
        }
    }
    return self;
}

#pragma mark Content Mode

- (void)setContentMode:(UIViewContentMode)contentMode {
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

#pragma mark Image

- (UIImage *)image {
    return [[self layer] contents];
}

- (void)setImage:(UIImage *)image {
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

@end

#pragma mark - RCTUIGraphicsImageRendererFormat

@implementation RCTUIGraphicsImageRendererFormat

+ (nonnull instancetype)defaultFormat {
    RCTUIGraphicsImageRendererFormat *format = [RCTUIGraphicsImageRendererFormat new];
    return format;
}

@end

#pragma mark - RCTUIGraphicsImageRenderer

@implementation RCTUIGraphicsImageRenderer {
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
