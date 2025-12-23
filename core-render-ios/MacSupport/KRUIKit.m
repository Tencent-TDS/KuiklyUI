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

// [macOS]

#if TARGET_OS_OSX

#import "KRUIKit.h"
#import <QuartzCore/QuartzCore.h>
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

static NSData *NSImageDataForFileType(NSImage *image, NSBitmapImageFileType fileType, NSDictionary<NSString *, id> *properties) {
    // Try to get existing NSBitmapImageRep if available
    NSBitmapImageRep *imageRep = nil;
    for (NSImageRep *rep in image.representations) {
        if ([rep isKindOfClass:[NSBitmapImageRep class]]) {
            imageRep = (NSBitmapImageRep *)rep;
            break;
        }
    }
    
    // If no NSBitmapImageRep exists, create one by rendering the image
    if (imageRep == nil) {
        // Lock focus to render the image into a bitmap representation
        CGSize imageSize = image.size;
        NSRect imageRect = NSMakeRect(0, 0, imageSize.width, imageSize.height);
        
        // Get the CGImage representation and create NSBitmapImageRep from it
        CGImageRef cgImage = [image CGImageForProposedRect:&imageRect context:nil hints:nil];
        if (cgImage == NULL) {
            return nil;
        }
        
        imageRep = [[NSBitmapImageRep alloc] initWithCGImage:cgImage];
        if (imageRep == nil) {
            return nil;
        }
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

// NSImage category to mimic common UIImage constructors
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

- (nullable CGImageRef)CGImage {
    NSRect imageRect = NSMakeRect(0, 0, self.size.width, self.size.height);
    CGImageRef cgImage = [self CGImageForProposedRect:&imageRect context:nil hints:nil];
    return cgImage;
}

- (CGFloat)scale {
    CGFloat scale = 1;
    NSRect imageRect = NSMakeRect(0, 0, self.size.width, self.size.height);
    NSImageRep *imageRep = [self bestRepresentationForRect:imageRect context:nil hints:nil];
    CGFloat width = imageRep.size.width;
    CGFloat height = imageRep.size.height;
    CGFloat pixelWidth = (CGFloat)imageRep.pixelsWide;
    CGFloat pixelHeight = (CGFloat)imageRep.pixelsHigh;
    if (width > 0 && height > 0) {
        CGFloat widthScale = pixelWidth / width;
        CGFloat heightScale = pixelHeight / height;
        if (widthScale == heightScale && widthScale >= 1) {
            // Protect because there may be `NSImageRepMatchesDevice` (0)
            scale = widthScale;
        }
    }
    
    return scale;
}

@end

@interface UITextPosition ()
@property (nonatomic, assign, readwrite) NSInteger index;
@end

@implementation UITextPosition

+ (instancetype)positionWithIndex:(NSInteger)index {
    UITextPosition *p = [UITextPosition new];
    p.index = index;
    return p;
}

@end

@interface UITextRange ()
@property (nonatomic, strong, readwrite) UITextPosition *start;
@property (nonatomic, strong, readwrite) UITextPosition *end;
@end

@implementation UITextRange

+ (instancetype)rangeWithStart:(UITextPosition *)start end:(UITextPosition *)end {
    UITextRange *r = [UITextRange new];
    r.start = start;
    r.end = end;
    return r;
}

@end

#pragma mark - KRUITextFieldCell

// Custom cell to center text vertically like iOS
// This is the standard approach in macOS to achieve vertical centering in NSTextField
@interface KRUITextFieldCell : NSTextFieldCell
@end

@implementation KRUITextFieldCell

// Helper method to calculate vertically centered rect
// Takes the cell's bounds and adjusts the origin.y to center the text vertically
- (NSRect)adjustedRectForBounds:(NSRect)rect {
    NSSize textSize = [self cellSizeForBounds:rect];
    CGFloat heightDelta = rect.size.height - textSize.height;
    if (heightDelta > 0) {
        rect.size.height = textSize.height;
        rect.origin.y += floor(heightDelta / 2.0);  // floor() for pixel alignment
    }
    return rect;
}

// Called when drawing the text content in non-editing state
// Adjusts the rect to vertically center the displayed text
- (NSRect)drawingRectForBounds:(NSRect)rect {
    return [self adjustedRectForBounds:[super drawingRectForBounds:rect]];
}

// Called when drawing the title/placeholder text
// Adjusts the rect to vertically center the title text
- (NSRect)titleRectForBounds:(NSRect)rect {
    return [self adjustedRectForBounds:[super titleRectForBounds:rect]];
}

// Called when entering edit mode (e.g., user clicks on the text field)
// Adjusts the rect to vertically center the field editor (cursor and text input)
- (void)editWithFrame:(NSRect)rect
               inView:(NSView *)controlView
               editor:(NSText *)textObj
             delegate:(id)delegate
                event:(NSEvent *)event {
    [super editWithFrame:[self adjustedRectForBounds:rect] inView:controlView editor:textObj delegate:delegate event:event];
}

// Called when text selection changes (e.g., user drags to select text)
// Adjusts the rect to vertically center the text selection
- (void)selectWithFrame:(NSRect)rect
                 inView:(NSView *)controlView
                 editor:(NSText *)textObj
               delegate:(id)delegate
                  start:(NSInteger)selStart
                 length:(NSInteger)selLength {
    [super selectWithFrame:[self adjustedRectForBounds:rect] inView:controlView editor:textObj delegate:delegate start:selStart length:selLength];
}

@end

#pragma mark - KRUITextField

@interface KRUITextField ()
@property (nonatomic, strong) NSMutableArray<NSDictionary *> *kr_targets;
@property (nonatomic, copy) NSString *kr_cachedPlaceholder; // Cache for placeholder text
@end

@implementation KRUITextField

+ (Class)cellClass {
    return [KRUITextFieldCell class];
}

- (instancetype)initWithFrame:(NSRect)frameRect {
    if (self = [super initWithFrame:frameRect]) {
        self.bezeled = NO;
        self.bordered = NO;
        self.drawsBackground = NO;
        self.editable = YES;
        self.selectable = YES;
        self.usesSingleLineMode = YES;
        self.delegate = (id<NSTextFieldDelegate>)self;
        self.focusRingType = NSFocusRingTypeNone;
        _kr_targets = [NSMutableArray array];
        
        // Ensure cell is also set to single line mode
        if ([self.cell isKindOfClass:[NSTextFieldCell class]]) {
            ((NSTextFieldCell *)self.cell).usesSingleLineMode = YES;
            ((NSTextFieldCell *)self.cell).wraps = NO;
            ((NSTextFieldCell *)self.cell).scrollable = YES;
        }
    }
    return self;
}

#pragma mark - Property Bridges

- (NSString *)text {
    return self.stringValue;
}

- (void)setText:(NSString *)text {
    self.stringValue = text ?: @"";
}

- (NSAttributedString *)attributedText {
    return self.attributedStringValue;
}

- (void)setAttributedText:(NSAttributedString *)attributedText {
    self.attributedStringValue = attributedText ?: [[NSAttributedString alloc] initWithString:@""];
}

- (NSString *)placeholder {
    // Always return cached value on macOS, because placeholderString gets cleared
    // when placeholderAttributedString is set
    return self.kr_cachedPlaceholder ?: self.placeholderString;
}

- (void)setPlaceholder:(NSString *)placeholder {
    self.kr_cachedPlaceholder = placeholder;
    self.placeholderString = placeholder;
}

- (NSAttributedString *)attributedPlaceholder {
    return self.placeholderAttributedString;
}

- (void)setAttributedPlaceholder:(NSAttributedString *)attributedPlaceholder {
    self.placeholderAttributedString = attributedPlaceholder;
}

- (NSTextAlignment)textAlignment {
    return self.alignment;
}

- (void)setTextAlignment:(NSTextAlignment)textAlignment {
    self.alignment = textAlignment;
}

#pragma mark - UITextInput-like

- (UITextPosition *)beginningOfDocument {
    return [UITextPosition positionWithIndex:0];
}

- (NSTextView *)kr_fieldEditorIfAvailable {
    id editor = [self currentEditor];
    return [editor isKindOfClass:[NSTextView class]] ? (NSTextView *)editor : nil;
}

- (UITextRange *)selectedTextRange {
    NSTextView *editor = [self kr_fieldEditorIfAvailable];
    if (editor) {
        NSRange r = editor.selectedRange;
        return [UITextRange rangeWithStart:[UITextPosition positionWithIndex:(NSInteger)r.location]
                                       end:[UITextPosition positionWithIndex:(NSInteger)(r.location + r.length)]];
    }
    return [UITextRange rangeWithStart:[UITextPosition positionWithIndex:0] end:[UITextPosition positionWithIndex:0]];
}

- (void)setSelectedTextRange:(UITextRange *)selectedTextRange {
    NSTextView *editor = [self kr_fieldEditorIfAvailable];
    if (editor && selectedTextRange) {
        NSInteger loc = selectedTextRange.start.index;
        NSInteger end = selectedTextRange.end.index;
        if (loc < 0) loc = 0;
        if (end < loc) end = loc;
        NSRange r = NSMakeRange((NSUInteger)loc, (NSUInteger)(end - loc));
        editor.selectedRange = r;
    }
}

- (UITextRange *)markedTextRange {
    NSTextView *editor = [self kr_fieldEditorIfAvailable];
    if (editor) {
        NSRange r = editor.markedRange;
        // Check both location and length to match iOS behavior
        // Only return non-nil when there's actual marked text (e.g., IME input)
        if (r.location != NSNotFound && r.length > 0) {
            return [UITextRange rangeWithStart:[UITextPosition positionWithIndex:(NSInteger)r.location]
                                           end:[UITextPosition positionWithIndex:(NSInteger)(r.location + r.length)]];
        }
    }
    return nil;
}

- (UITextPosition *)positionFromPosition:(UITextPosition *)position offset:(NSInteger)offset {
    // Return nil when position is nil to match iOS behavior
    if (!position) {
        return nil;
    }
    NSInteger idx = MAX(0, position.index + offset);
    NSString *s = self.stringValue ?: @"";
    if (idx > (NSInteger)s.length) idx = (NSInteger)s.length;
    return [UITextPosition positionWithIndex:idx];
}

- (UITextRange *)textRangeFromPosition:(UITextPosition *)fromPosition toPosition:(UITextPosition *)toPosition {
    return [UITextRange rangeWithStart:fromPosition end:toPosition];
}

- (NSInteger)offsetFromPosition:(UITextPosition *)from toPosition:(UITextPosition *)to {
    return to.index - from.index;
}

#pragma mark - UIControl-like

- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events {
    if (events & UIControlEventEditingChanged) {
        if (target && action) {
            [_kr_targets addObject:@{ @"t": target, @"a": NSStringFromSelector(action) }];
        }
    }
}

- (void)kr_dispatchEditingChanged {
    for (NSDictionary *entry in _kr_targets) {
        id t = entry[@"t"]; SEL a = NSSelectorFromString(entry[@"a"]);
        if (t && [t respondsToSelector:a]) {
            ((void (*)(id, SEL, id))objc_msgSend)(t, a, self);
        }
    }
}

#pragma mark - NSTextFieldDelegate

- (void)controlTextDidBeginEditing:(NSNotification *)obj {
    id<UITextFieldDelegate> d = (id)self.delegate;
    if ([d respondsToSelector:@selector(textFieldDidBeginEditing:)]) {
        [d textFieldDidBeginEditing:(id)self];
    }
}

- (void)controlTextDidEndEditing:(NSNotification *)obj {
    id<UITextFieldDelegate> d = (id)self.delegate;
    if ([d respondsToSelector:@selector(textFieldDidEndEditing:)]) {
        [d textFieldDidEndEditing:(id)self];
    }
}

- (void)controlTextDidChange:(NSNotification *)obj {
    [self kr_dispatchEditingChanged];
}

- (BOOL)control:(NSControl *)control textView:(NSTextView *)textView shouldChangeTextInRange:(NSRange)affectedCharRange replacementString:(NSString *)replacementString {
    id<UITextFieldDelegate> d = (id)self.delegate;
    if ([d respondsToSelector:@selector(textField:shouldChangeCharactersInRange:replacementString:)]) {
        return [d textField:(id)self shouldChangeCharactersInRange:affectedCharRange replacementString:replacementString];
    }
    return YES;
}

- (BOOL)control:(NSControl *)control textView:(NSTextView *)textView doCommandBySelector:(SEL)commandSelector {
    if (commandSelector == @selector(insertNewline:)) {
        id<UITextFieldDelegate> d = (id)self.delegate;
        BOOL should = YES;
        if ([d respondsToSelector:@selector(textFieldShouldReturn:)]) {
            should = [d textFieldShouldReturn:(id)self];
        }
        if (should) {
            [[self window] makeFirstResponder:nil];
        }
        return YES;
    }
    return NO;
}

@end

#pragma mark - KRUITextView

@interface KRUITextView ()
@property (nonatomic, weak) id<UITextViewDelegate> uiDelegate;
@end

@implementation KRUITextView

- (instancetype)initWithFrame:(NSRect)frameRect {
    if (self = [super initWithFrame:frameRect]) {
        self.delegate = self;
        self.richText = NO;
        self.allowsUndo = YES;
        self.usesFontPanel = NO;
        self.usesRuler = NO;
        self.importsGraphics = NO;
        self.automaticQuoteSubstitutionEnabled = NO;
        self.automaticLinkDetectionEnabled = NO;
        self.automaticDashSubstitutionEnabled = NO;
        self.automaticTextReplacementEnabled = NO;
        self.automaticSpellingCorrectionEnabled = NO;
        
        // Make it behave like iOS UITextView
        [self setDrawsBackground:YES];
        [self setEditable:YES];
        [self setSelectable:YES];
        [self setWantsLayer:YES];
    }
    return self;
}

#pragma mark - Property Bridges

- (NSString *)text {
    return self.string;
}

- (void)setText:(NSString *)text {
    self.string = text ?: @"";
}

- (NSAttributedString *)attributedText {
    return self.attributedString;
}

- (void)setAttributedText:(NSAttributedString *)attributedText {
    [[self textStorage] setAttributedString:attributedText ?: [[NSAttributedString alloc] initWithString:@""]];
}

- (NSTextAlignment)textAlignment {
    return self.alignment;
}

- (void)setTextAlignment:(NSTextAlignment)textAlignment {
    self.alignment = textAlignment;
}

#pragma mark - Override inherited properties

// Override NSView backgroundColor to handle drawsBackground
- (KRUIColor *)backgroundColor {
    return [self drawsBackground] ? [super backgroundColor] : [NSColor clearColor];
}

- (void)setBackgroundColor:(KRUIColor *)backgroundColor {
    if (backgroundColor && ![backgroundColor isEqual:[NSColor clearColor]]) {
        [self setDrawsBackground:YES];
        [super setBackgroundColor:backgroundColor];
    } else {
        [self setDrawsBackground:NO];
    }
}

- (void)setDelegate:(id)delegate {
    // Store UI delegate separately to avoid conflict with NSTextView delegate
    if ([delegate conformsToProtocol:@protocol(UITextViewDelegate)]) {
        _uiDelegate = delegate;
    }
    [super setDelegate:self];
}

#pragma mark - UITextInput-like compatibility

- (UITextPosition *)beginningOfDocument {
    return [UITextPosition positionWithIndex:0];
}

// Override NSText selectedRange for UIKit compatibility
- (NSRange)selectedRange {
    return self.selectedRanges.firstObject ? [self.selectedRanges.firstObject rangeValue] : NSMakeRange(0, 0);
}

- (void)setSelectedRange:(NSRange)selectedRange {
    [self setSelectedRange:selectedRange affinity:NSSelectionAffinityDownstream stillSelecting:NO];
}

- (UITextRange *)selectedTextRange {
    NSRange r = self.selectedRange;
    return [UITextRange rangeWithStart:[UITextPosition positionWithIndex:(NSInteger)r.location]
                                   end:[UITextPosition positionWithIndex:(NSInteger)(r.location + r.length)]];
}

- (void)setSelectedTextRange:(UITextRange *)selectedTextRange {
    if (selectedTextRange) {
        NSInteger loc = selectedTextRange.start.index;
        NSInteger len = selectedTextRange.end.index - loc;
        [self setSelectedRange:NSMakeRange(MAX(0, loc), MAX(0, len))];
    }
}

- (UITextRange *)markedTextRange {
    NSRange r = self.markedRange;
    if (r.location != NSNotFound && r.length > 0) {
        return [UITextRange rangeWithStart:[UITextPosition positionWithIndex:(NSInteger)r.location]
                                       end:[UITextPosition positionWithIndex:(NSInteger)(r.location + r.length)]];
    }
    return nil;
}

- (UITextPosition *)positionFromPosition:(UITextPosition *)position offset:(NSInteger)offset {
    if (!position) {
        return nil;
    }
    NSInteger idx = MAX(0, position.index + offset);
    NSString *s = self.string ?: @"";
    if (idx > (NSInteger)s.length) idx = (NSInteger)s.length;
    return [UITextPosition positionWithIndex:idx];
}

- (UITextRange *)textRangeFromPosition:(UITextPosition *)fromPosition toPosition:(UITextPosition *)toPosition {
    return [UITextRange rangeWithStart:fromPosition end:toPosition];
}

- (NSInteger)offsetFromPosition:(UITextPosition *)from toPosition:(UITextPosition *)to {
    return to.index - from.index;
}

#pragma mark - NSTextViewDelegate

- (void)textDidChange:(NSNotification *)notification {
    if ([_uiDelegate respondsToSelector:@selector(textViewDidChange:)]) {
        [_uiDelegate textViewDidChange:(id)self];
    }
}

- (void)textDidBeginEditing:(NSNotification *)notification {
    if ([_uiDelegate respondsToSelector:@selector(textViewDidBeginEditing:)]) {
        [_uiDelegate textViewDidBeginEditing:(id)self];
    }
}

- (void)textDidEndEditing:(NSNotification *)notification {
    if ([_uiDelegate respondsToSelector:@selector(textViewDidEndEditing:)]) {
        [_uiDelegate textViewDidEndEditing:(id)self];
    }
}

- (BOOL)textView:(NSTextView *)textView shouldChangeTextInRange:(NSRange)affectedCharRange replacementString:(NSString *)replacementString {
    if ([_uiDelegate respondsToSelector:@selector(textView:shouldChangeTextInRange:replacementText:)]) {
        return [_uiDelegate textView:(id)self shouldChangeTextInRange:affectedCharRange replacementText:replacementString];
    }
    return YES;
}

#pragma mark - iOS compatibility methods

- (BOOL)becomeFirstResponder {
    // Do NOT call [super becomeFirstResponder] here.
    // NSTextView's becomeFirstResponder throws an exception when called outside
    // the responder chain context (e.g., from dispatch_async blocks).
    // Using makeFirstResponder: is the correct way to become first responder on macOS.
    return [[self window] makeFirstResponder:self];
}

- (BOOL)resignFirstResponder {
    [[self window] makeFirstResponder:nil];
    return [super resignFirstResponder];
}

- (BOOL)isFirstResponder {
    return [[self window] firstResponder] == self;
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

#pragma mark - NSBezierPath (KRUIKitCompat)

@implementation NSBezierPath (KRUIKitCompat)

- (void)addArcWithCenter:(CGPoint)center
                  radius:(CGFloat)radius
              startAngle:(CGFloat)startAngle
                endAngle:(CGFloat)endAngle
               clockwise:(BOOL)clockwise {
    // [macOS] NSBezierPath uses degrees, UIBezierPath uses radians
    // Also NSBezierPath's angles are counter-clockwise from the positive x-axis
    // Convert radians to degrees and adjust for coordinate system differences
    CGFloat startDegrees = startAngle * 180.0 / M_PI;
    CGFloat endDegrees = endAngle * 180.0 / M_PI;
    
    [self appendBezierPathWithArcWithCenter:center
                                     radius:radius
                                 startAngle:startDegrees
                                   endAngle:endDegrees
                                  clockwise:clockwise];
}

- (void)addLineToPoint:(CGPoint)point {
    [self lineToPoint:point];
}

@end

#pragma mark - NSFont UIKit Compatibility

@implementation NSFont (KRUIKitCompatLineHeight)

- (CGFloat)lineHeight {
    // NSFont doesn't expose lineHeight; synthesize via ascender/descender/leading
    CGFloat h = self.ascender + fabs(self.descender) + self.leading;
    return ceil(h);
}

@end

#pragma mark - KRUIView

// KRUIView implementation - provides macOS-specific extensions
// Note: Most UIKit compatibility is handled by NSView+KRCompat Category
@implementation KRUIView {
@private
    BOOL _hasMouseOver;
    NSTrackingArea *_trackingArea;
    BOOL _mouseDownCanMoveWindow;
}

#pragma mark Initialization

static KRUIView *KRUIViewCommonInit(KRUIView *self) {
    if (self != nil) {
        self.wantsLayer = YES;
        self->_enableFocusRing = YES;
        self->_mouseDownCanMoveWindow = YES;
    }
    return self;
}

- (instancetype)initWithFrame:(NSRect)frameRect {
    return KRUIViewCommonInit([super initWithFrame:frameRect]);
}

- (instancetype)initWithCoder:(NSCoder *)coder {
    return KRUIViewCommonInit([super initWithCoder:coder]);
}

#pragma mark First Responder Handling

- (BOOL)acceptsFirstMouse:(NSEvent *)event {
    if (self.acceptsFirstMouse || [super acceptsFirstMouse:event]) {
        return YES;
    }
    
    // If any KRUIView above has acceptsFirstMouse set, then return YES here
    NSView *view = self;
    while ((view = view.superview)) {
        if ([view isKindOfClass:[KRUIView class]] && [(KRUIView *)view acceptsFirstMouse]) {
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
    
    // didMoveToWindow is handled by NSView+KRCompat swizzling
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
    // Disabled for now to avoid selector warnings
    return NO;
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

#pragma mark Layer Backing

- (BOOL)wantsUpdateLayer {
    return [self respondsToSelector:@selector(displayLayer:)];
}

- (void)updateLayer {
    CALayer *layer = [self layer];
    // backgroundColor is handled by NSView+KRCompat
    if (self.backgroundColor) {
        // updateLayer will be called when the view's current appearance changes.
        // The layer's backgroundColor is a CGColor which is not appearance aware
        // so it has to be reset from the view's NSColor.
        [layer setBackgroundColor:[self.backgroundColor CGColor]];
    }
    [(id<CALayerDelegate>)self displayLayer:layer];
}

@end

#pragma mark - KRPlatformView (AnimationCompat)

@implementation KRPlatformView (AnimationCompat)

+ (void)animateWithDuration:(NSTimeInterval)duration
                      delay:(NSTimeInterval)delay
                    options:(UIViewAnimationOptions)options
                 animations:(void (^)(void))animations
                 completion:(void (^ __nullable)(BOOL finished))completion {
    // 使用 NSAnimationContext
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [NSAnimationContext runAnimationGroup:^(NSAnimationContext *context) {
            context.duration = duration;
            context.allowsImplicitAnimation = YES; // [macOS] 关键：启用隐式动画
            
            // 根据 UIViewAnimationOptions 设置动画曲线
            if (options & UIViewAnimationOptionCurveLinear) {
                context.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionLinear];
            } else if (options & UIViewAnimationOptionCurveEaseIn) {
                context.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseIn];
            } else if (options & UIViewAnimationOptionCurveEaseOut) {
                context.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseOut];
            } else {
                context.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut];
            }
            
            if (animations) {
                animations();
            }
        } completionHandler:^{
            if (completion) {
                completion(YES);
            }
        }];
    });
}

+ (void)animateWithDuration:(NSTimeInterval)duration
                      delay:(NSTimeInterval)delay
     usingSpringWithDamping:(CGFloat)damping
      initialSpringVelocity:(CGFloat)velocity
                    options:(UIViewAnimationOptions)options
                 animations:(void (^)(void))animations
                 completion:(void (^ __nullable)(BOOL finished))completion {
    // [macOS] 使用 NSAnimationContext + spring timing，这是 macOS 上最可靠的方案
    // 虽然不如 iOS 的 spring 动画物理准确，但在 NSAnimationContext 框架下最兼容
    
    // Spring 动画阻尼分级阈值
    static const CGFloat kHighDampingThreshold = 0.85;  // 高阻尼阈值：几乎无振荡
    static const CGFloat kMediumDampingThreshold = 0.6; // 中等阻尼阈值：轻微弹性
    static const CGFloat kLowDampingThreshold = 0.8;    // 低阻尼时延长动画的阈值
    
    // 动画时长调整参数
    static const CGFloat kDurationMultiplier = 0.5;     // 低阻尼时的时长延长系数
    
    // Cubic Bezier 控制点 - 中等阻尼
    static const CGFloat kMediumDampingCP1X = 0.25;    // 控制点1 X：加速度起始
    static const CGFloat kMediumDampingCP1Y = 0.1;     // 控制点1 Y：平缓起始
    static const CGFloat kMediumDampingCP2X = 0.25;    // 控制点2 X：减速度起始
    static const CGFloat kMediumDampingCP2Y = 1.0;     // 控制点2 Y：平滑结束
    
    // Cubic Bezier 控制点 - 低阻尼（弹性效果）
    static const CGFloat kLowDampingCP1X = 0.3;        // 控制点1 X：快速加速
    static const CGFloat kLowDampingCP1Y = 0.0;        // 控制点1 Y：从零开始
    static const CGFloat kLowDampingCP2X = 0.2;        // 控制点2 X：提前到达峰值
    static const CGFloat kOvershootMultiplier = 0.8;   // 过冲幅度系数（增大以增强过冲）
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [NSAnimationContext runAnimationGroup:^(NSAnimationContext *context) {
            // Spring 参数映射：
            // - iOS damping: 0 = 无阻尼（强烈震荡），1 = 完全阻尼（无震荡）
            // - 通过调整 duration 和 timing function 的控制点来模拟物理弹簧效果
            
            // 低阻尼时延长动画时间，给振荡留出时间
            CGFloat adjustedDuration = duration;
            if (damping < kLowDampingThreshold) {
                // 阻尼越小，动画时长延长越多，以显示完整的振荡过程
                adjustedDuration = duration * (1.0 + (1.0 - damping) * kDurationMultiplier);
            }
            context.duration = adjustedDuration;
            context.allowsImplicitAnimation = YES;
            
            // 根据阻尼值分级设置 timing function
            if (damping >= kHighDampingThreshold) {
                // 高阻尼（0.85-1.0）：平滑过渡，接近线性减速，无振荡
                context.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseOut];
                
            } else if (damping >= kMediumDampingThreshold) {
                // 中等阻尼（0.6-0.85）：使用 cubic bezier 模拟轻微弹性
                // 曲线稍微超过终点后快速回归，产生微弱的弹簧感
                context.timingFunction = [CAMediaTimingFunction functionWithControlPoints:
                    kMediumDampingCP1X :kMediumDampingCP1Y :kMediumDampingCP2X :kMediumDampingCP2Y];
                
            } else {
                // 低阻尼（0.0-0.6）：明显的弹性曲线，有过冲效果
                // CP2Y > 1.0 会产生过冲（超过目标值），模拟弹簧振荡
                CGFloat overshoot = (1.0 - damping) * kOvershootMultiplier; // 阻尼越小，过冲越大
                CGFloat cp2y = 1.0 + overshoot; // 过冲量：控制点2的Y值超过1.0
                
                context.timingFunction = [CAMediaTimingFunction functionWithControlPoints:
                    kLowDampingCP1X :kLowDampingCP1Y :kLowDampingCP2X :cp2y];
            }
            
            if (animations) {
                animations();
            }
        } completionHandler:^{
            if (completion) {
                completion(YES);
            }
        }];
    });
}

+ (void)animateKeyframesWithDuration:(NSTimeInterval)duration
                                delay:(NSTimeInterval)delay
                              options:(UIViewKeyframeAnimationOptions)options
                           animations:(void (^)(void))animations
                           completion:(void (^ __nullable)(BOOL finished))completion {
    // 修复关键帧动画：重置 keyframe blocks 数组，准备收集新的关键帧
    g_keyframeBlocks = [NSMutableArray array];
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        // [macOS] 执行 animations block 收集所有关键帧（通过 addKeyframeWithRelativeStartTime 添加）
        if (animations) {
            animations();
        }
        
        // [macOS] 如果没有关键帧，直接调用 completion
        if (g_keyframeBlocks.count == 0) {
            if (completion) {
                completion(YES);
            }
            return;
        }
        
        // [macOS] 使用 NSAnimationContext 执行关键帧动画
        NSArray<void (^)(void)> *keyframes = [g_keyframeBlocks copy];
        g_keyframeBlocks = nil; // Clear for next animation
        
        [NSAnimationContext runAnimationGroup:^(NSAnimationContext *context) {
            context.duration = duration;
            context.allowsImplicitAnimation = YES; // [macOS] 关键：启用隐式动画
            
            // Execute all keyframe animations
            for (void (^keyframeBlock)(void) in keyframes) {
                if (keyframeBlock) {
                    keyframeBlock();
                }
            }
        } completionHandler:^{
            if (completion) {
                completion(YES);
            }
        }];
    });
}

static NSMutableArray<void (^)(void)> *g_keyframeBlocks;

+ (void)addKeyframeWithRelativeStartTime:(double)frameStartTime
                        relativeDuration:(double)frameDuration
                               animations:(void (^)(void))animations {
    // [macOS] Store keyframe blocks to be executed in animateKeyframesWithDuration
    if (!g_keyframeBlocks) {
        g_keyframeBlocks = [NSMutableArray array];
    }
    if (animations) {
        [g_keyframeBlocks addObject:[animations copy]];
    }
}

static UIViewAnimationCurve g_currentAnimationCurve = UIViewAnimationCurveEaseInOut;

+ (void)setAnimationCurve:(UIViewAnimationCurve)curve {
    // [macOS] Store animation curve to be used in keyframe animations
    g_currentAnimationCurve = curve;
    
    // Set timing function for current CATransaction if one is active
    NSString *timingFunction = kCAMediaTimingFunctionEaseInEaseOut;
    switch (curve) {
        case UIViewAnimationCurveLinear:
            timingFunction = kCAMediaTimingFunctionLinear;
            break;
        case UIViewAnimationCurveEaseIn:
            timingFunction = kCAMediaTimingFunctionEaseIn;
            break;
        case UIViewAnimationCurveEaseOut:
            timingFunction = kCAMediaTimingFunctionEaseOut;
            break;
        case UIViewAnimationCurveEaseInOut:
        default:
            timingFunction = kCAMediaTimingFunctionEaseInEaseOut;
            break;
    }
    [CATransaction setAnimationTimingFunction:[CAMediaTimingFunction functionWithName:timingFunction]];
}

@end

#pragma mark - KRUIDocumentView

// Flipped container view for NSScrollView to match iOS coordinate system
@interface KRUIDocumentView : NSView
@end

@implementation KRUIDocumentView

- (BOOL)isFlipped {
    // Return YES to use top-left origin coordinate system like iOS
    // This matches UIKit's coordinate system where (0,0) is top-left
    return YES;
}

// Accept first responder to enable keyboard/mouse interaction
- (BOOL)acceptsFirstResponder {
    return YES;
}

@end

#pragma mark - KRUIScrollView

@implementation KRUIScrollView {
    BOOL _isInitializing; // Flag to avoid intercepting internal subview additions during init
}

#pragma mark Initialization and Deallocation

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self name:NSViewBoundsDidChangeNotification object:self.contentView];
}

- (instancetype)initWithFrame:(CGRect)frame {
    _isInitializing = YES; // Mark as initializing to avoid intercepting internal addSubview calls
    
    if (self = [super initWithFrame:frame]) {
        self.scrollEnabled = YES;
        self.drawsBackground = NO;
        self.contentView.postsBoundsChangedNotifications = YES;
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(kr_contentViewBoundsDidChange:)
                                                     name:NSViewBoundsDidChangeNotification
                                                   object:self.contentView];
        
        // Setup custom documentView after super init completes
        [self kr_setupDocumentView];
    }
    
    _isInitializing = NO; // Init complete
    return self;
}

// Setup the document view container
- (void)kr_setupDocumentView {
    // Create a flipped container view as documentView to hold subviews
    // Use a flipped view to match iOS coordinate system (origin at top-left)
    // Start with a minimal size - it will be resized when content is added
    KRUIDocumentView *documentView = [[KRUIDocumentView alloc] initWithFrame:CGRectMake(0, 0, 1, 1)];
    [documentView setWantsLayer:YES];
    // Ensure documentView accepts mouse events
    documentView.layerContentsRedrawPolicy = NSViewLayerContentsRedrawDuringViewResize;
    self.documentView = documentView;
}


#pragma mark Subview Management

// Override to add subviews to documentView instead of NSScrollView itself
- (void)addSubview:(NSView *)view {
    // During initialization, don't intercept internal NSScrollView subview additions
    if (_isInitializing) {
        [super addSubview:view];
        return;
    }
    
    // After initialization, add content views to documentView
    if ([self.documentView isKindOfClass:[KRUIDocumentView class]]) {
        [self.documentView addSubview:view];
    } else {
        [super addSubview:view];
    }
}

- (void)insertSubview:(NSView *)view atIndex:(NSInteger)index {
    if (!view) { return; }
    
    // During initialization, don't intercept internal NSScrollView subview additions
    if (_isInitializing) {
        [super addSubview:view];
        return;
    }
    
    // After initialization, add content views to documentView
    if ([self.documentView isKindOfClass:[KRUIDocumentView class]]) {
        NSArray<__kindof NSView *> *subviews = self.documentView.subviews;
        if (index < 0) { index = 0; }
        if ((NSUInteger)index >= subviews.count) {
            [self.documentView addSubview:view];
        } else {
            [self.documentView addSubview:view positioned:NSWindowBelow relativeTo:subviews[index]];
        }
    } else {
        [super addSubview:view];
    }
}

// Override subviews to return documentView's subviews for UIKit compatibility
- (NSArray<__kindof NSView *> *)subviews {
    // After initialization, return documentView's subviews for UIKit compatibility
    if (!_isInitializing && self.documentView && [self.documentView isKindOfClass:[KRUIDocumentView class]]) {
        return self.documentView.subviews;
    }
    return [super subviews];
}

#pragma mark Event Handling

// Override hitTest to ensure proper event routing to subviews
- (NSView *)hitTest:(NSPoint)point {
    NSPoint pointInSelf = [self convertPoint:point fromView:self.superview];
    // First check if the point is within our bounds
    if (![self mouse:pointInSelf inRect:self.bounds]) {
        return nil;
    }
    
    // Only do custom hit testing if we have our custom documentView
    if ([self.documentView isKindOfClass:[KRUIDocumentView class]]) {
        // Convert point to documentView's coordinate space and check subviews
        NSView *hitView = [self.documentView hitTest:[self.documentView convertPoint:point fromView:self.superview]];
        
        // If a subview was hit, return it
        if (hitView && hitView != self.documentView) {
            return hitView;
        }
    }
    
    // Otherwise, use default behavior or return self for scrolling
    return [super hitTest:point];
}


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

// bounces property bridge
- (BOOL)bounces {
    return self.horizontalScrollElasticity != NSScrollElasticityNone ||
           self.verticalScrollElasticity != NSScrollElasticityNone;
}

- (void)setBounces:(BOOL)bounces {
    NSScrollElasticity elasticity = bounces ? NSScrollElasticityAllowed : NSScrollElasticityNone;
    self.horizontalScrollElasticity = elasticity;
    self.verticalScrollElasticity = elasticity;
}

// pagingEnabled property bridge
- (BOOL)pagingEnabled {
    static char kPagingEnabledKey;
    NSNumber *value = objc_getAssociatedObject(self, &kPagingEnabledKey);
    return value ? [value boolValue] : NO;
}

- (void)setPagingEnabled:(BOOL)pagingEnabled {
    static char kPagingEnabledKey;
    objc_setAssociatedObject(self, &kPagingEnabledKey, @(pagingEnabled), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

// scrollEnabled property bridge
- (BOOL)isScrollEnabled {
    static char kScrollEnabledKey;
    NSNumber *value = objc_getAssociatedObject(self, &kScrollEnabledKey);
    return value ? [value boolValue] : YES; // Default is YES
}

- (void)setScrollEnabled:(BOOL)scrollEnabled {
    static char kScrollEnabledKey;
    objc_setAssociatedObject(self, &kScrollEnabledKey, @(scrollEnabled), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}


#pragma mark Mouse Location Tracking

// Store last scroll event for mouse location tracking
- (NSEvent *)kr_lastScrollEvent {
    static char kLastScrollEventKey;
    return objc_getAssociatedObject(self, &kLastScrollEventKey);
}

- (void)kr_setLastScrollEvent:(NSEvent *)event {
    static char kLastScrollEventKey;
    objc_setAssociatedObject(self, &kLastScrollEventKey, event, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

// Get current mouse location in given view (simulates touch location)
- (CGPoint)kr_mouseLocationInView:(UIView *)view {
    NSEvent *lastEvent = [self kr_lastScrollEvent];
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

@end

#pragma mark - KRUIScrollView (DelegateBridge)

@implementation KRUIScrollView (DelegateBridge)

#pragma mark State Properties

// Public readonly properties for UIScrollView compatibility
- (BOOL)isDragging {
    return [self kr_isDragging];
}

- (BOOL)isDecelerating {
    return [self kr_isDecelerating];
}

#pragma mark Internal State Tracking

// Internal state tracking using associated objects
- (BOOL)kr_isDragging {
    static char kDraggingKey;
    NSNumber *v = objc_getAssociatedObject(self, &kDraggingKey);
    return v.boolValue;
}

- (void)kr_setDragging:(BOOL)dragging {
    static char kDraggingKey;
    objc_setAssociatedObject(self, &kDraggingKey, @(dragging), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)kr_isDecelerating {
    static char kDeceleratingKey;
    NSNumber *v = objc_getAssociatedObject(self, &kDeceleratingKey);
    return v.boolValue;
}

- (void)kr_setDecelerating:(BOOL)decelerating {
    static char kDeceleratingKey;
    objc_setAssociatedObject(self, &kDeceleratingKey, @(decelerating), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

#pragma mark Scroll Notifications

- (void)kr_contentViewBoundsDidChange:(NSNotification *)__unused note {
    if ([self.delegate respondsToSelector:@selector(scrollViewDidScroll:)]) {
        [self.delegate scrollViewDidScroll:(UIScrollView *)self];
    }
}

#pragma mark Paging Support

// Snap to nearest page boundary when pagingEnabled is YES
- (void)kr_snapToNearestPage {
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

#pragma mark Scroll Wheel Handling

- (void)scrollWheel:(NSEvent *)event {
    // Check if scrolling is enabled
    if (!self.isScrollEnabled) {
        // If scrolling is disabled, pass the event to the next responder
        [self.nextResponder scrollWheel:event];
        return;
    }
    
    // Store event for mouse location tracking
    [self kr_setLastScrollEvent:event];
    
    // Begin dragging
    if (![self kr_isDragging] && event.phase != NSEventPhaseNone) {
        [self kr_setDragging:YES];
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
    if ([self kr_isDragging] && event.momentumPhase == NSEventPhaseBegan) {
        if ([self.delegate respondsToSelector:@selector(scrollViewDidEndDragging:willDecelerate:)]) {
            [self.delegate scrollViewDidEndDragging:(UIScrollView *)self willDecelerate:YES];
        }
        [self kr_setDragging:NO];
        [self kr_setDecelerating:YES];
    }
    
    // No momentum: end dragging without decelerate
    if ([self kr_isDragging] && event.phase == NSEventPhaseEnded && event.momentumPhase == NSEventPhaseNone) {
        if ([self.delegate respondsToSelector:@selector(scrollViewDidEndDragging:willDecelerate:)]) {
            [self.delegate scrollViewDidEndDragging:(UIScrollView *)self willDecelerate:NO];
        }
        [self kr_setDragging:NO];
        
        // Apply paging snap after drag ends without momentum
        if (self.pagingEnabled) {
            [self kr_snapToNearestPage];
        }
    }
    
    // Momentum end: deceleration done
    if ([self kr_isDecelerating] && event.momentumPhase == NSEventPhaseEnded) {
        if ([self.delegate respondsToSelector:@selector(scrollViewDidEndDecelerating:)]) {
            [self.delegate scrollViewDidEndDecelerating:(UIScrollView *)self];
        }
        [self kr_setDecelerating:NO];
        
        // Apply paging snap after momentum ends
        if (self.pagingEnabled) {
            [self kr_snapToNearestPage];
        }
    }
}

@end

#pragma mark - View Helper Functions

BOOL KRUIViewSetClipsToBounds(KRPlatformView *view) {
    // NSViews are always clipped to bounds
    BOOL clipsToBounds = YES;
    
    // But see if UIView overrides that behavior
    if ([view respondsToSelector:@selector(clipsToBounds)]) {
        clipsToBounds = [(id)view clipsToBounds];
    }
    
    return clipsToBounds;
}

#pragma mark - KRClipView

@implementation KRClipView

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

#pragma mark - KRUISlider

@interface KRUISlider ()
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, NSMutableArray<NSDictionary *> *> *eventHandlers;
@property (nonatomic, readwrite) BOOL pressed;
@end

@implementation KRUISlider

// Override synthesize to use NSSlider properties
@dynamic value;
@dynamic minimumValue;
@dynamic maximumValue;

- (instancetype)initWithFrame:(NSRect)frameRect {
    if (self = [super initWithFrame:frameRect]) {
        _eventHandlers = [NSMutableDictionary new]; // [macOS]
        _pressed = NO; // [macOS]
        
        // [macOS] Set up action to track value changes
        [super setTarget:self];
        [super setAction:@selector(kr_sliderValueChanged:)];
    }
    return self;
}

#pragma mark - Property Bridges

// [macOS] Bridge float value to NSSlider's double value
- (float)value {
    return (float)self.doubleValue;
}

- (void)setValue:(float)value {
    self.doubleValue = (double)value;
}

- (void)setValue:(float)value animated:(__unused BOOL)animated {
    // [macOS] Animate slider value change
    if (animated) {
        [NSAnimationContext runAnimationGroup:^(NSAnimationContext *context) {
            context.duration = 0.3;
            self.animator.doubleValue = (double)value;
        } completionHandler:nil];
    } else {
        self.doubleValue = (double)value;
    }
}

- (float)minimumValue {
    return (float)self.minValue;
}

- (void)setMinimumValue:(float)minimumValue {
    self.minValue = (double)minimumValue;
}

- (float)maximumValue {
    return (float)self.maxValue;
}

- (void)setMaximumValue:(float)maximumValue {
    self.maxValue = (double)maximumValue;
}

// [macOS] thumbTintColor - limited support on NSSlider
- (void)setThumbTintColor:(NSColor *)thumbTintColor {
    _thumbTintColor = thumbTintColor;
    // Note: NSSlider doesn't directly support thumb tint color
    // Subclasses may override rendering to use this property
}

#pragma mark - UIControl-like Target-Action Support

// [macOS] Add target-action for UIControlEvents
- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events {
    if (!target || !action) return;
    
    NSNumber *eventsKey = @(events);
    NSMutableArray *handlers = self.eventHandlers[eventsKey];
    if (!handlers) {
        handlers = [NSMutableArray new];
        self.eventHandlers[eventsKey] = handlers;
    }
    
    [handlers addObject:@{
        @"target": target,
        @"action": NSStringFromSelector(action)
    }];
}

// [macOS] Internal action handler for slider value changes
- (void)kr_sliderValueChanged:(id)sender {
    // Invoke all registered handlers for UIControlEventValueChanged
    [self kr_invokeHandlersForEvent:UIControlEventValueChanged];
}

// [macOS] Helper to invoke event handlers
- (void)kr_invokeHandlersForEvent:(UIControlEvents)event {
    NSArray *handlers = self.eventHandlers[@(event)];
    for (NSDictionary *handler in handlers) {
        id target = handler[@"target"];
        SEL action = NSSelectorFromString(handler[@"action"]);
        
        if (target && [target respondsToSelector:action]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [target performSelector:action withObject:self];
#pragma clang diagnostic pop
        }
    }
}

#pragma mark - Mouse Events for Touch Simulation

// [macOS] Track mouse down/up to simulate iOS touch events
- (void)mouseDown:(NSEvent *)event {
    [super mouseDown:event];
    self.pressed = YES;
    [self kr_invokeHandlersForEvent:UIControlEventTouchDown];
}

- (void)mouseUp:(NSEvent *)event {
    [super mouseUp:event];
    
    // [macOS] Check if mouse is inside or outside bounds
    NSPoint locationInView = [self convertPoint:event.locationInWindow fromView:nil];
    BOOL isInside = NSPointInRect(locationInView, self.bounds);
    
    self.pressed = NO;
    
    if (isInside) {
        [self kr_invokeHandlersForEvent:UIControlEventTouchUpInside];
    } else {
        [self kr_invokeHandlersForEvent:UIControlEventTouchUpOutside];
    }
}

#pragma mark - Track and Thumb Customization

// Override points for subclasses to customize track rendering
- (CGRect)trackRectForBounds:(CGRect)bounds {
    // NSSlider doesn't expose public API for track rect
    // Provide a reasonable approximation based on bounds and control size
    CGRect trackRect = bounds;
    
    // Adjust for typical NSSlider track dimensions
    if (self.vertical) {
        // Vertical slider: track is typically narrower, centered horizontally
        CGFloat trackWidth = 4.0; // Default track width
        trackRect.origin.x = CGRectGetMidX(bounds) - trackWidth * 0.5;
        trackRect.size.width = trackWidth;
        // Height stays the same as bounds
    } else {
        // Horizontal slider: track is typically shorter in height, centered vertically
        CGFloat trackHeight = 4.0; // Default track height
        trackRect.origin.y = CGRectGetMidY(bounds) - trackHeight * 0.5;
        trackRect.size.height = trackHeight;
        // Width stays the same as bounds
    }
    
    return trackRect;
}

// [macOS] Override points for subclasses to customize thumb rendering
- (CGRect)thumbRectForBounds:(CGRect)bounds trackRect:(CGRect)rect value:(float)value {
    // NSSlider doesn't directly expose thumb rect calculation
    // Approximate thumb position based on value and track rect
    
    CGFloat range = self.maximumValue - self.minimumValue;
    if (range <= 0) {
        return CGRectZero;
    }
    
    CGFloat normalizedValue = (value - self.minimumValue) / range;
    
    // Estimate thumb size (NSSlider default is approximately 15x15)
    CGFloat thumbWidth = 15.0;
    CGFloat thumbHeight = 15.0;
    
    // Calculate thumb position along track
    CGFloat thumbX = rect.origin.x + (rect.size.width - thumbWidth) * normalizedValue;
    CGFloat thumbY = rect.origin.y + (rect.size.height - thumbHeight) * 0.5;
    
    return CGRectMake(thumbX, thumbY, thumbWidth, thumbHeight);
}

@end

#pragma mark - KRUILabel

@implementation KRUILabel

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

// Bridge iOS UILabel drawing pipeline to call drawTextInRect: if implemented by subclass
- (void)drawRect:(NSRect)dirtyRect {
    // Check if subclass overrides drawTextInRect: (compare method implementations)
    if ([self methodForSelector:@selector(drawTextInRect:)] != 
        [KRUILabel instanceMethodForSelector:@selector(drawTextInRect:)]) {
        // Subclass has custom drawing, call it with full bounds to match iOS UILabel behavior
        [self drawTextInRect:self.bounds];
        return;
    }
    // Use default NSTextField drawing
    [super drawRect:dirtyRect];
}

// Base implementation for subclasses to override
- (void)drawTextInRect:(CGRect)rect {
    // Subclasses (e.g. KRLabel) should override this method to perform custom text drawing
}

// Use iOS-like flipped coordinates for consistency with UIKit drawing
- (BOOL)isFlipped {
    return YES;
}

// Bridge textAlignment <-> alignment
- (NSTextAlignment)textAlignment {
    return self.alignment;
}

- (void)setTextAlignment:(NSTextAlignment)textAlignment {
    self.alignment = textAlignment;
}

@end

#pragma mark - KRUISegmentedControl

@interface KRUISegmentedControl ()
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, NSMutableArray<NSDictionary *> *> *eventHandlers;
@end

@implementation KRUISegmentedControl

- (instancetype)initWithFrame:(NSRect)frameRect {
    if (self = [super initWithFrame:frameRect]) {
        _eventHandlers = [NSMutableDictionary new];
        
        // Configure NSSegmentedControl
        self.segmentStyle = NSSegmentStyleRounded;
        self.trackingMode = NSSegmentSwitchTrackingSelectOne;
        
        // Set up action to track segment changes
        [super setTarget:self];
        [super setAction:@selector(kr_segmentedControlValueChanged:)];
    }
    return self;
}

- (instancetype)initWithItems:(NSArray *)items {
    if (self = [self initWithFrame:NSZeroRect]) {
        // Add segments for each item
        for (NSUInteger i = 0; i < items.count; i++) {
            id item = items[i];
            if ([item isKindOfClass:[NSString class]]) {
                [self insertSegmentWithTitle:item atIndex:i animated:NO];
            } else if ([item isKindOfClass:[NSImage class]]) {
                [self setImage:item forSegment:i];
            }
        }
    }
    return self;
}

#pragma mark - Property Bridges

// Bridge iOS selectedSegmentIndex to NSSegmentedControl's selectedSegment
- (NSInteger)selectedSegmentIndex {
    return self.selectedSegment;
}

- (void)setSelectedSegmentIndex:(NSInteger)selectedSegmentIndex {
    self.selectedSegment = selectedSegmentIndex;
}

// Bridge iOS numberOfSegments to NSSegmentedControl's segmentCount
- (NSInteger)numberOfSegments {
    return self.segmentCount;
}

#pragma mark - Segment Management

// Bridge iOS insertSegmentWithTitle:atIndex:animated: to NSSegmentedControl
- (void)insertSegmentWithTitle:(NSString *)title atIndex:(NSUInteger)segment animated:(BOOL)animated {
    // NSSegmentedControl doesn't support animation, ignore the animated parameter
    
    // Ensure we have enough segments
    if (segment > self.segmentCount) {
        self.segmentCount = segment + 1;
    } else if (segment == self.segmentCount) {
        self.segmentCount = self.segmentCount + 1;
    }
    
    [self setLabel:title forSegment:segment];
    
    // Auto-size the segment to fit its content
    if (@available(macOS 10.13, *)) {
        [self setWidth:0 forSegment:segment]; // 0 means auto-size
    }
}

- (void)removeAllSegments {
    self.segmentCount = 0;
}

#pragma mark - UIControl-like Target-Action Support

- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events {
    if (!target || !action) return;
    
    NSNumber *eventsKey = @(events);
    NSMutableArray *handlers = self.eventHandlers[eventsKey];
    if (!handlers) {
        handlers = [NSMutableArray new];
        self.eventHandlers[eventsKey] = handlers;
    }
    
    [handlers addObject:@{
        @"target": target,
        @"action": NSStringFromSelector(action)
    }];
}

// Internal action handler for segment changes
- (void)kr_segmentedControlValueChanged:(id)sender {
    // Invoke all registered handlers for UIControlEventValueChanged
    [self kr_invokeHandlersForEvent:UIControlEventValueChanged];
}

// Helper to invoke event handlers
- (void)kr_invokeHandlersForEvent:(UIControlEvents)event {
    NSArray *handlers = self.eventHandlers[@(event)];
    for (NSDictionary *handler in handlers) {
        id target = handler[@"target"];
        SEL action = NSSelectorFromString(handler[@"action"]);
        
        if (target && [target respondsToSelector:action]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [target performSelector:action withObject:self];
#pragma clang diagnostic pop
        }
    }
}

@end


#pragma mark - KRUISwitch

@interface KRUISwitch ()
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, NSMutableArray<NSDictionary *> *> *eventHandlers;
@end

@implementation KRUISwitch

// Color properties: LIMITED support on NSButton-based switch
// thumbTintColor has no visual effect (cannot customize thumb on NSButton)
@synthesize onTintColor = _onTintColor;
@synthesize thumbTintColor = _thumbTintColor;
@synthesize tintColor = _tintColor;

- (void)setOnTintColor:(KRUIColor *)onTintColor {
    _onTintColor = onTintColor;
    // Try to apply contentTintColor on macOS 10.14+
    // This has limited visual effect on NSButtonTypeSwitch
    if (@available(macOS 10.14, *)) {
        if (onTintColor && self.state == NSControlStateValueOn) {
            self.contentTintColor = onTintColor;
        }
    }
}

- (void)setTintColor:(KRUIColor *)tintColor {
    _tintColor = tintColor;
    // contentTintColor affects the overall tint when available
    if (@available(macOS 10.14, *)) {
        if (tintColor) {
            self.contentTintColor = tintColor;
        }
    }
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _eventHandlers = [NSMutableDictionary new];
        
        // Configure NSButton as a switch-style control
        // On macOS 10.15+, NSButtonTypeSwitch renders as a modern switch
        // On earlier versions, it renders as a checkbox
        [self setButtonType:NSButtonTypeSwitch];
        self.title = @""; // No text label by default
        
        [super setTarget:self];
        [super setAction:@selector(switchValueChanged:)];
    }
    return self;
}

- (BOOL)isOn {
    return self.state == NSControlStateValueOn;
}

- (void)setOn:(BOOL)on {
    [self setOn:on animated:NO];
}

- (void)setOn:(BOOL)on animated:(BOOL)animated {
    self.state = on ? NSControlStateValueOn : NSControlStateValueOff;
    
    // Update contentTintColor based on state (macOS 10.14+)
    if (@available(macOS 10.14, *)) {
        if (on && _onTintColor) {
            self.contentTintColor = _onTintColor;
        } else if (!on && _tintColor) {
            self.contentTintColor = _tintColor;
        }
    }
}

// UIControl-like target-action support
- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events {
    if (!target || !action) return;
    
    NSNumber *eventsKey = @(events);
    NSMutableArray *handlers = self.eventHandlers[eventsKey];
    if (!handlers) {
        handlers = [NSMutableArray new];
        self.eventHandlers[eventsKey] = handlers;
    }
    
    [handlers addObject:@{
        @"target": target,
        @"action": NSStringFromSelector(action)
    }];
}

- (void)switchValueChanged:(id)sender {
    // Invoke all registered handlers for UIControlEventValueChanged
    NSArray *handlers = self.eventHandlers[@(UIControlEventValueChanged)];
    for (NSDictionary *handler in handlers) {
        id target = handler[@"target"];
        SEL action = NSSelectorFromString(handler[@"action"]);
        
        if (target && [target respondsToSelector:action]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [target performSelector:action withObject:self];
#pragma clang diagnostic pop
        }
    }
}

@end

#pragma mark - NSValue (KRUIKitCompatFactory)

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

#pragma mark - KRUIActivityIndicatorView

@interface KRUIActivityIndicatorView ()
@property (nonatomic, readwrite, getter=isAnimating) BOOL animating;
@end

@implementation KRUIActivityIndicatorView

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

- (void)setColor:(KRUIColor *)color {
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

#pragma mark - KRUIImageView

@implementation KRUIImageView {
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

#pragma mark - KRUIGraphicsImageRendererFormat

@implementation KRUIGraphicsImageRendererFormat

+ (nonnull instancetype)defaultFormat {
    KRUIGraphicsImageRendererFormat *format = [KRUIGraphicsImageRendererFormat new];
    return format;
}

@end

#pragma mark - KRUIGraphicsImageRenderer

@implementation KRUIGraphicsImageRenderer {
    CGSize _size;
    KRUIGraphicsImageRendererFormat *_format;
}

- (nonnull instancetype)initWithSize:(CGSize)size {
    if (self = [super init]) {
        self->_size = size;
    }
    return self;
}

- (nonnull instancetype)initWithSize:(CGSize)size format:(nonnull KRUIGraphicsImageRendererFormat *)format {
    if (self = [super init]) {
        self->_size = size;
        self->_format = format;
    }
    return self;
}

- (nonnull NSImage *)imageWithActions:(NS_NOESCAPE KRUIGraphicsImageDrawingActions)actions {
    NSImage *image = [NSImage imageWithSize:_size
                                    flipped:NO
                             drawingHandler:^BOOL(NSRect dstRect) {
        KRUIGraphicsImageRendererContext *context = [NSGraphicsContext currentContext];
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
