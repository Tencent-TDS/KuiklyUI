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

#import "KRLabel.h"
#import <pthread.h>
#import <libkern/OSAtomic.h>
#import "KRAsyncDeallocManager.h"
#import <objc/runtime.h>
#import "NSObject+KR.h"

#define KRAssertMainThread() NSAssert(0 != pthread_main_np(), @"This method must be called on the main thread!")
NSString *const KRHighlightAttributeKey = @"KRHighlightAttributeKey";
NSString *const KRBGAttributeKey = @"KRBGAttributeKey";

@interface KRLabel()

@end

@implementation KRLabel



#pragma mark - override

- (NSString *)accessibilityLabel{
    NSString * res = [super accessibilityLabel];
    if (res.length <= 0) {
        return self.attributedText.string;
    }
    return res;
}


- (void)setAttributedText:(NSAttributedString *)attributedText {
    [super setAttributedText:attributedText];
    self.textRender = attributedText.hr_textRender;
    self.attributedText.hr_textRender = self.textRender;
    [self setNeedsDisplay];
}


- (void)drawTextInRect:(CGRect)rect {
    // 使用TextKit绘制文本
    self.textRender.size = rect.size;
    if (self.textRender.lineBreakMargin > 0 && self.textRender.isBreakLine) {
        CGSize size = self.textRender.size;
        UIBezierPath * bezierPath = [UIBezierPath bezierPathWithRect:CGRectMake(size.width - self.textRender.lineBreakMargin, size.height - 10, self.textRender.lineBreakMargin, 10)];
        self.textRender.textContainer.exclusionPaths = @[bezierPath];
    }
    
    [self.textRender drawTextAtPoint:rect.origin isCanceled:nil];

}

- (void)setBackgroundColor:(UIColor *)backgroundColor {
    if (backgroundColor == nil) {
        backgroundColor = [UIColor clearColor];
    }
    [super setBackgroundColor:backgroundColor];
}


#pragma mark - public

+ (CGSize)sizeThatFits:(CGSize)size attributedString:(NSAttributedString *)attString numberOfLines:(NSUInteger)lines lineBreakMode:(NSLineBreakMode)mode{
    return [self sizeThatFits:size attributedString:attString numberOfLines:lines lineBreakMode:mode lineBreakMarin:0];
}

+ (CGSize)sizeThatFits:(CGSize)size attributedString:(NSAttributedString *)attString numberOfLines:(NSUInteger)lines lineBreakMode:(NSLineBreakMode)mode lineBreakMarin:(CGFloat)marin {
    return [self sizeThatFits:size attributedString:attString numberOfLines:lines lineBreakMode:mode lineBreakMarin:0 lineHeight:0];
}

+ (CGSize)sizeThatFits:(CGSize)size attributedString:(NSAttributedString *)attString numberOfLines:(NSUInteger)lines lineBreakMode:(NSLineBreakMode)mode lineBreakMarin:(CGFloat)marin lineHeight:(CGFloat)lineHeight {
    attString = [attString isKindOfClass:[NSAttributedString class]] ? attString : [[NSAttributedString alloc] initWithString:@""];
    NSTextStorage *textStorage = [[NSTextStorage alloc] initWithAttributedString:[attString copy]];
    textStorage.hr_hasAttachmentViews = attString.hr_hasAttachmentViews;
    KRTextRender *textRender = [[KRTextRender alloc] initWithTextStorage:textStorage lineHeight:lineHeight];
    textRender.lineBreakMargin = marin;
    textRender.maximumNumberOfLines = lines;
    textRender.lineBreakMode = mode;
    CGSize fitSize = [textRender textSizeWithRenderWidth:size.width];
    if (marin > 0 && lines) {
        textRender.maximumNumberOfLines = 0;
        CGSize newSize = [textRender textSizeWithRenderWidth:size.width];
        textRender.isBreakLine = !CGSizeEqualToSize(fitSize, newSize);
        textRender.maximumNumberOfLines = lines;//复原
    }
    attString.hr_textRender = textRender;
    attString.hr_size = fitSize;
    
    return fitSize;
}



- (void)dealloc {
   
}




#pragma mark - private

@end
//---------KRTextRender类分割线------------
@interface KRTextRender() <NSLayoutManagerDelegate> {
    CGRect _textBound;
}
@property (nonatomic, strong) KRLayoutManager * layoutManager;
@property (nonatomic, strong) NSTextContainer * textContainer;
@property (nonatomic, strong) NSTextStorage * textStorageOnRender;


@end
@implementation KRTextRender
@synthesize maximumNumberOfLines = _maximumNumberOfLines;

- (instancetype)init{
    if (self = [super init]) {
        _textContainer = [NSTextContainer new];
        _layoutManager = [KRLayoutManager new];
        _layoutManager.delegate = self;
        [_layoutManager addTextContainer:_textContainer];
        _textContainer.lineFragmentPadding = 0;
    }
    return self;
}

- (instancetype)initWithAttributedText:(NSAttributedString *)attributedText{
    if (self = [self initWithTextStorage:[[NSTextStorage alloc] initWithAttributedString:attributedText]]) {
        self.textStorage.hr_hasAttachmentViews = attributedText.hr_hasAttachmentViews;
    }
    return self;
}

- (instancetype)initWithTextStorage:(NSTextStorage *)textStorage lineHeight:(CGFloat)lineHeight {
    if (self = [self init]) {
        self.lineHeight = lineHeight;
        self.textStorage = textStorage;
    }
    return self;
}

- (instancetype)initWithTextStorage:(NSTextStorage *)textStorage{
    return [self initWithTextStorage:textStorage lineHeight:0];
}
#pragma mark - Getter && Setter

- (void)setTextStorage:(NSTextStorage *)textStorage{
    _textStorage = textStorage;
    self.textStorageOnRender = textStorage;
}

- (void)setTextStorageOnRender:(NSTextStorage *)textStorageOnRender{
    if (_textStorageOnRender != textStorageOnRender) {
        if (_textStorageOnRender) {
            [_textStorageOnRender removeLayoutManager:_layoutManager];
        }
        [textStorageOnRender addLayoutManager:_layoutManager];
        _textStorageOnRender = textStorageOnRender;
    }
}

- (void)setSize:(CGSize)size{
    if (isnan(size.width)) {
        size.width = 0;
    }
    if (isnan(size.height)) {
        size.height = 0;
    }
    _size = size;
    if (!CGSizeEqualToSize(_textContainer.size, size)) {
        _textContainer.size = size;
    }
}

- (void)setLineBreakMode:(NSLineBreakMode)lineBreakMode{
    if (_textContainer.lineBreakMode != lineBreakMode) {
        _textContainer.lineBreakMode = lineBreakMode;
    }
}

- (NSUInteger)maximumNumberOfLines{
    return _textContainer.maximumNumberOfLines;
}

- (void)setMaximumNumberOfLines:(NSUInteger)maximumNumberOfLines{
    if (_textContainer.maximumNumberOfLines != maximumNumberOfLines) {
        _textContainer.maximumNumberOfLines = maximumNumberOfLines;
    }
}

#pragma mark - Public

- (NSRange)visibleGlyphRange {
    return [_layoutManager glyphRangeForTextContainer:_textContainer];
}

- (NSRange)visibleCharacterRange {
    return [_layoutManager characterRangeForGlyphRange:[self visibleGlyphRange] actualGlyphRange:nil];
}

- (CGRect)boundingRectForCharacterRange:(NSRange)characterRange {
    NSRange glyphRange = [_layoutManager glyphRangeForCharacterRange:characterRange actualCharacterRange:nil];
    return [self boundingRectForGlyphRange:glyphRange];
}

- (CGRect)boundingRectForGlyphRange:(NSRange)glyphRange {
    return [_layoutManager boundingRectForGlyphRange:glyphRange inTextContainer:_textContainer];
}

- (CGRect)textBound {
    return [_layoutManager usedRectForTextContainer:_textContainer];
}

- (NSInteger)characterIndexForPoint:(CGPoint)point{
    CGFloat distanceToPoint = 1.0;
    NSUInteger index = [_layoutManager characterIndexForPoint:point inTextContainer:_textContainer fractionOfDistanceBetweenInsertionPoints:&distanceToPoint];
    return distanceToPoint < 1 ? index : -1;
}


- (CGSize)textSizeWithRenderWidth:(CGFloat)renderWidth{
    if (!_textStorageOnRender)  return CGSizeZero;
    _textContainer.size = CGSizeMake(renderWidth, MAXFLOAT);
    CGSize textSize = [self textBound].size;
    CGSize res = CGSizeMake(ceil(textSize.width), ceil(textSize.height));
    return  res;
}
#pragma mark -  draw text


- (void)drawTextAtPoint:(CGPoint)point isCanceled:(BOOL (^)(void))isCanceled{
    NSRange glyphRange = [_layoutManager glyphRangeForTextContainer:_textContainer];
    // drawing text
    [_layoutManager enumerateLineFragmentsForGlyphRange:glyphRange usingBlock:^(CGRect rect, CGRect usedRect, NSTextContainer * _Nonnull textContainer, NSRange glyphRange, BOOL * _Nonnull stop) {
        [self->_layoutManager drawBackgroundForGlyphRange:glyphRange atPoint:point];
        if (isCanceled && isCanceled()) {*stop = YES; return ;};
        [self->_layoutManager drawGlyphsForGlyphRange:glyphRange atPoint:point];
        if (isCanceled && isCanceled()) {*stop = YES; return ;};
    }];
}

- (void)dealloc{
    [[KRAsyncDeallocManager shareManager] asyncDeallocWithObject:_textStorageOnRender];
    if (_textStorage != _textStorageOnRender) {
        [[KRAsyncDeallocManager shareManager] asyncDeallocWithObject:_textStorage];
    }
    [[KRAsyncDeallocManager shareManager] asyncDeallocWithObject:_layoutManager];
    [[KRAsyncDeallocManager shareManager] asyncDeallocWithObject:_textContainer];

}

#pragma mark - layout manager delegate
- (BOOL)layoutManager:(NSLayoutManager *)layoutManager shouldSetLineFragmentRect:(inout CGRect *)lineFragmentRect lineFragmentUsedRect:(inout CGRect *)lineFragmentUsedRect baselineOffset:(inout CGFloat *)baselineOffset inTextContainer:(NSTextContainer *)textContainer forGlyphRange:(NSRange)glyphRange {
    
    if (_lineHeight > 0) {
        UIFont *font;
        NSParagraphStyle *style;
        NSArray *attrsList = [self attributesListForGlyphRange:glyphRange layoutManager:layoutManager];
        [self getFont:&font paragraphStyle:&style fromAttibutesList:attrsList];

        if (![font isKindOfClass:[UIFont class]]) {
            return NO;
        }

        UIFont *defaultFont = [self systemDefaultFontForFont:font];

        CGRect rect = *lineFragmentRect;
        CGRect usedRect = *lineFragmentUsedRect;
        
        CGFloat textLineHeight = _lineHeight;
        CGFloat fixedBaseLineOffset = [self.class baseLineOffsetForLineHeight:textLineHeight font:defaultFont];
        
        rect.size.height = textLineHeight;
        usedRect.size.height = MAX(textLineHeight, usedRect.size.height);
        
        *lineFragmentRect = rect;
        *lineFragmentUsedRect = usedRect;
        *baselineOffset = fixedBaseLineOffset;
    }
    
    return YES;
}

+ (CGFloat)lineHeightForFont:(UIFont *)font paragraphStyle:(NSParagraphStyle *)style  {
    CGFloat lineHeight = font.lineHeight;
    if (!style) {
        return lineHeight;
    }
    if (style.lineHeightMultiple > 0) {
        lineHeight *= style.lineHeightMultiple;
    }
    if (style.minimumLineHeight > 0) {
        lineHeight = MAX(style.minimumLineHeight, lineHeight);
    }
    if (style.maximumLineHeight > 0) {
        lineHeight = MIN(style.maximumLineHeight, lineHeight);
    }
    return lineHeight;
}


+ (CGFloat)baseLineOffsetForLineHeight:(CGFloat)lineHeight font:(UIFont *)font {
    CGFloat baseLine = lineHeight + font.descender / 2;
    return baseLine;
}

/// get system default font of size
- (UIFont *)systemDefaultFontForFont:(UIFont *)font {
    return [UIFont systemFontOfSize:font.pointSize];
}


- (NSArray<NSDictionary *> *)attributesListForGlyphRange:(NSRange)glyphRange layoutManager:(NSLayoutManager *)layoutManager {

    // exclude the line break. System doesn't calucate the line rect with it.
    if (glyphRange.length > 1) {
        NSGlyphProperty property = [layoutManager propertyForGlyphAtIndex:glyphRange.location + glyphRange.length - 1];
        if (property & NSGlyphPropertyControlCharacter) {
            glyphRange = NSMakeRange(glyphRange.location, glyphRange.length - 1);
        }
    }

    
    NSTextStorage *textStorage = layoutManager.textStorage;
    NSRange targetRange = [layoutManager characterRangeForGlyphRange:glyphRange actualGlyphRange:nil];
    NSMutableArray *dicts = [NSMutableArray arrayWithCapacity:2];

    NSInteger last = -1;
    NSRange effectRange = NSMakeRange(targetRange.location, 0);

    while (effectRange.location + effectRange.length < targetRange.location + targetRange.length) {
        NSInteger current = effectRange.location + effectRange.length;
        // if effectRange didn't advanced, we manuly add 1 to avoid infinate loop.
        if (current <= last) {
            current += 1;
        }
        NSDictionary *attributes = [textStorage attributesAtIndex:current effectiveRange:&effectRange];
        if (attributes) {
            [dicts addObject:attributes];
        }
        last = current;
    }

    return dicts;
}

- (void)getFont:(UIFont **)returnFont paragraphStyle:(NSParagraphStyle **)returnStyle fromAttibutesList:(NSArray<NSDictionary *> *)attributesList {

    if (attributesList.count == 0) {
        return;
    }

    UIFont *findedFont = nil;
    NSParagraphStyle *findedStyle = nil;
    CGFloat lastHeight = -CGFLOAT_MAX;

    // find the attributes with max line height
    for (NSInteger i = 0; i < attributesList.count; i++) {
        NSDictionary *attrs = attributesList[i];

        NSParagraphStyle *style = attrs[NSParagraphStyleAttributeName];
        UIFont *font = attrs[NSFontAttributeName];

        if ([font isKindOfClass:[UIFont class]] &&
            (!style || [style isKindOfClass:[NSParagraphStyle class]]) ) {

            CGFloat height = [self.class lineHeightForFont:font paragraphStyle:style];
            if (height > lastHeight) {
                lastHeight = height;
                findedFont = font;
                findedStyle = style;
            }
        }
    }

    *returnFont = findedFont;
    *returnStyle = findedStyle;
}


@end

//------KRLayoutManager类分割线-----

@implementation KRLayoutManager{
    CGPoint _drawAtPoint;
}

- (void)drawBackgroundForGlyphRange:(NSRange)glyphsToShow atPoint:(CGPoint)origin {
    _drawAtPoint = origin;
    [super drawBackgroundForGlyphRange:glyphsToShow atPoint:origin];
    _drawAtPoint = CGPointZero;
}
- (void)dealloc{
#if DEBUG
    
    

#endif
}

@end
     

@interface KRTextAttachment ()
@property (nonatomic, assign) NSRange range;
@property (nonatomic, assign) CGPoint position;
@end

@implementation KRTextAttachment
- (void)setSize:(CGSize)size {
    _size = size;
    self.bounds = CGRectMake(0, _baseline, _size.width, _size.height);
}

- (void)setBounds:(CGRect)bounds {
    [super setBounds:bounds];
    _size = bounds.size;
}

- (void)setBaseline:(CGFloat)baseline {
    _baseline = baseline;
    self.bounds = CGRectMake(0, _baseline, _size.width, _size.height);
}

- (void)setImage:(UIImage *)image {
    [super setImage:image];
    if (_size.width == 0 && _size.height == 0 ) {
        self.size = image.size;
    }
}

- (void)setView:(UIView *)view {
    _view = view;
    if (_size.width == 0 && _size.height == 0 ) {
        self.size = view.frame.size;
    }
}


- (nullable UIImage *)imageForBounds:(CGRect)imageBounds textContainer:(nullable NSTextContainer *)textContainer characterIndex:(NSUInteger)charIndex {
    _position = CGPointMake(imageBounds.origin.x, imageBounds.origin.y - _size.height);
    return self.image;
}

- (CGRect)attachmentBoundsForTextContainer:(NSTextContainer *)textContainer proposedLineFragment:(CGRect)lineFrag glyphPosition:(CGPoint)position characterIndex:(NSUInteger)charIndex {
    if (_verticalAlignment == KRAttachmentAlignmentBaseline || self.bounds.origin.y > 0) {
        
        return CGRectMake(self.bounds.origin.x, self.bounds.origin.y - 2, self.bounds.size.width, self.bounds.size.height);
    }
    CGFloat offset = 0;
    UIFont *font = [textContainer.layoutManager.textStorage attribute:NSFontAttributeName atIndex:charIndex effectiveRange:nil];
    if (!font) {
        return self.bounds;
    }
    //    CGFloat pointSize = font.pointSize;
    //    CGFloat mid = font.descender + font.capHeight;
    //    CGFloat dd = font.descender ;
    //    CGFloat dd2 = font.ascender ;
    switch (_verticalAlignment) {
            //        case KRAttachmentAlignmentBaseline:
            //            offset = (font.capHeight - _size.height)/2;
            //            break;
        case KRAttachmentAlignmentCenter:
        {
            offset = (_size.height - font.capHeight)/2;
        }
            break;
        case KRAttachmentAlignmentBottom:
            offset = _size.height-font.pointSize + 2;
        default:
            break;
    }
    return CGRectMake(0, -offset, _size.width, _size.height);
}


@end

@implementation KRTextAttachment (Display)

- (void)setFrame:(CGRect)frame {
    _view.frame = frame;
}

- (void)addToSuperView:(UIView *)superView {
    if (_view) {
        [superView addSubview:_view];
    }
}
- (void)removeFromSuperView:(UIView *)superView {
    if (_view.superview == superView) {
        [_view removeFromSuperview];
    }
}

@end

@implementation NSAttributedString (KRTextAttachment)
- (BOOL)hr_hasAttachmentViews{
    NSNumber * value = objc_getAssociatedObject(self, @selector(hr_hasAttachmentViews));
    return [value boolValue];
}


- (void)setHr_hasAttachmentViews:(BOOL)hr_hasAttachmentViews{
    objc_setAssociatedObject(self, @selector(hr_hasAttachmentViews), @(hr_hasAttachmentViews), OBJC_ASSOCIATION_RETAIN);
}

- (NSArray<KRTextAttachment *> *)hr_viewAttachments{
    if (self.hr_hasAttachmentViews) {
        NSMutableArray *res = [NSMutableArray array];
        [self enumerateAttribute:NSAttachmentAttributeName inRange:NSMakeRange(0, self.length) options:kNilOptions usingBlock:^(KRTextAttachment *attribute, NSRange range, BOOL *stop) {
            if (attribute && [attribute isKindOfClass:[KRTextAttachment class]] && (attribute.view)) {
                attribute.range = range;
                [res addObject:attribute];
            }
        }];
        return res.count ? res : nil;
    }
    return nil;
}

@end

@implementation NSAttributedString(MIJAsync)
-(KRTextRender *)hr_textRender{
    return objc_getAssociatedObject(self, @selector(hr_textRender));
}

- (void)setHr_textRender:(KRTextRender *)hr_textRender{
    objc_setAssociatedObject(self, @selector(hr_textRender), hr_textRender, OBJC_ASSOCIATION_RETAIN);
}
- (CGSize)hr_size{
    return [objc_getAssociatedObject(self, @selector(hr_size)) CGSizeValue];
}

- (void)setHr_size:(CGSize)hr_size{
    objc_setAssociatedObject(self, @selector(hr_size), [NSValue valueWithCGSize:hr_size], OBJC_ASSOCIATION_RETAIN);
}
@end


