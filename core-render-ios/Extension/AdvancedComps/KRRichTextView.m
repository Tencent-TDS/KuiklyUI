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

#import "KRRichTextView.h"
#import "KRComponentDefine.h"
#import "KRConvertUtil.h"
#import "KuiklyRenderBridge.h"
#import "NSObject+KR.h"

NSString *const KuiklyIndexAttributeName = @"KuiklyIndexAttributeName";
NSString *const kGradientInfoKeyCSSGradient = @"cssGradient";
NSString *const kGradientInfoKeyFont = @"font";
NSString *const kGradientInfoKeyGlobalRange = @"globalRange";

@interface KRRichTextView()

@property (nonatomic, strong) NSNumber *css_numberOfLines;
@property (nonatomic, strong) NSString *css_lineBreakMode;


@end

@implementation KRRichTextView {
}
@synthesize hr_rootView;

#pragma mark - KuiklyRenderViewExportProtocol

- (instancetype)init {
    if (self = [super init]) {
        self.displaysAsynchronously = NO;
    }
    return self;
}

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_prepareForeReuse {
    KUIKLY_RESET_CSS_COMMON_PROP;
    self.attributedText = nil;
    self.css_numberOfLines = nil;
    self.css_lineBreakMode = nil;
}

+ (id<KuiklyRenderShadowProtocol>)hrv_createShadow {
    return [[KRRichTextShadow alloc] init];
}

- (void)hrv_setShadow:(id<KuiklyRenderShadowProtocol>)shadow {
    KRRichTextShadow * textShadow = (KRRichTextShadow *)shadow;
    self.attributedText = textShadow.attributedString;
}


#pragma mark - set prop

- (void)setCss_numberOfLines:(NSNumber *)css_numberOfLines {
    if (self.css_numberOfLines != css_numberOfLines) {
        _css_numberOfLines = css_numberOfLines;
        self.numberOfLines = [css_numberOfLines unsignedIntValue];
    }
}

- (void)setCss_lineBreakMode:(NSString *)css_lineBreakMode {
    if (self.css_lineBreakMode != css_lineBreakMode) {
        _css_lineBreakMode = css_lineBreakMode;
        self.lineBreakMode = [KRConvertUtil NSLineBreakMode:css_lineBreakMode];
    }
}

#pragma mark - override

- (void)css_onClickTapWithSender:(UIGestureRecognizer *)sender {
    CGPoint location = [sender locationInView:self];
#if TARGET_OS_OSX // [macOS NSWindow is not a subclass of NSView, use contentView
    CGPoint pageLocation = [sender locationInView:self.window.contentView];
#else
    CGPoint pageLocation = [self kr_convertLocalPointToRenderRoot:location];
#endif // macOS]
    KRTextRender * textRender = self.attributedText.hr_textRender;
    NSInteger index = [textRender characterIndexForPoint:location];
    NSNumber *spanIndex = nil;
    if (index >= 0 && index < self.attributedText.length) {
        spanIndex = [self.attributedText attribute:KuiklyIndexAttributeName atIndex:index effectiveRange:nil];
    }
    self.css_click(@{
        @"x": @(location.x),
        @"y": @(location.y),
        @"pageX": @(pageLocation.x),
        @"pageY": @(pageLocation.y),
        @"index": spanIndex?: @(-1),
    });

}

- (void)setBackgroundColor:(UIColor *)backgroundColor
{
    [super setBackgroundColor:backgroundColor];
    // 背景颜色会影响shodow，这里更新下shadow
    [self setCss_boxShadow:self.css_boxShadow];
}

- (void)setCss_boxShadow:(NSString *)css_boxShadow
{
    // 背景色为clear时，会变成textShadow，这里和安卓对齐，统一由textShadow属性来控制
    if (self.backgroundColor != UIColor.clearColor) {
        [super setCss_boxShadow:css_boxShadow];
    }
}

@end

/// KRRichTextShadow
@interface KRRichTextShadow()

@end

@implementation KRRichTextShadow {
    NSMutableDictionary<NSString *, id> *_props;
    NSArray<NSDictionary *> * _spans;
    NSMutableAttributedString *_mAttributedString;
    NSMutableArray<NSDictionary *> *_pendingGradients; // 延迟应用的渐变信息（需等待布局完成后获取总尺寸）
}

#pragma mark - KuiklyRenderShadowProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    if (!_props) {
        _props = [[NSMutableDictionary alloc] init];
    }
    _props[propKey] = propValue;
}


- (CGSize)hrv_calculateRenderViewSizeWithConstraintSize:(CGSize)constraintSize {
    _mAttributedString = [self p_buildAttributedString];

    CGFloat height = constraintSize.height > 0 ? constraintSize.height : MAXFLOAT;
    NSInteger numberOfLines = [KRConvertUtil NSInteger:_props[@"numberOfLines"]];
    NSLineBreakMode lineBreakMode = [KRConvertUtil NSLineBreakMode:_props[@"lineBreakMode"]];
    CGFloat lineBreakMargin = [KRConvertUtil CGFloat:_props[@"lineBreakMargin"]];
    CGFloat lineHeight = [KRConvertUtil CGFloat:_props[@"lineHeight"]];
    CGSize fitSize = [KRLabel sizeThatFits:CGSizeMake(constraintSize.width, height) attributedString:_mAttributedString numberOfLines:numberOfLines lineBreakMode:lineBreakMode lineBreakMarin:lineBreakMargin lineHeight:lineHeight];

    // 渐变色延迟应用：需在布局完成后获取总尺寸，才能绘制跨行连续的渐变效果
    if (_pendingGradients.count > 0) {
        for (NSDictionary *gradientInfo in _pendingGradients) {
            NSString *cssGradient = gradientInfo[kGradientInfoKeyCSSGradient];
            UIFont *font = gradientInfo[kGradientInfoKeyFont];
            NSRange globalRange = [gradientInfo[kGradientInfoKeyGlobalRange] rangeValue];

            [TextGradientHandler applyGlobalGradientToAttributedString:_mAttributedString
                                                                 range:globalRange
                                                           cssGradient:cssGradient
                                                                  font:font
                                                        totalLayoutSize:fitSize];
        }

        // 渐变应用后重建 TextRender
        NSTextStorage *textStorage = [[NSTextStorage alloc] initWithAttributedString:_mAttributedString];
        textStorage.hr_hasAttachmentViews = _mAttributedString.hr_hasAttachmentViews;
        KRTextRender *textRender = [[KRTextRender alloc] initWithTextStorage:textStorage lineHeight:lineHeight];
        textRender.lineBreakMargin = lineBreakMargin;
        textRender.maximumNumberOfLines = numberOfLines;
        textRender.lineBreakMode = lineBreakMode;
        
        if (lineBreakMargin > 0 && numberOfLines) {
            textRender.maximumNumberOfLines = 0;
            CGSize newSize = [textRender textSizeWithRenderWidth:constraintSize.width];
            textRender.isBreakLine = !CGSizeEqualToSize(fitSize, newSize);
            textRender.maximumNumberOfLines = numberOfLines;
        }
        _mAttributedString.hr_textRender = textRender;
        _mAttributedString.hr_size = fitSize;
        [_pendingGradients removeAllObjects];
    }

    return fitSize;
}

- (NSString *)hrv_callWithMethod:(NSString *)method params:(NSString *)params {
    if ([method isEqualToString:@"spanRect"]) { // span所在的排版位置坐标
        return [self css_spanRectWithParams:params];
    } else if ([method isEqualToString:@"isLineBreakMargin"]) {
        return [self isLineBreakMargin];
    }
    return @"";
}

- (dispatch_block_t)hrv_taskToMainQueueWhenWillSetShadowToView {
    __weak typeof(self) weakSelf = self;
    NSMutableAttributedString *attrString = _mAttributedString;
    return ^{
        weakSelf.attributedString = attrString;
    };
}

#pragma mark - public

- (NSAttributedString *)buildAttributedString {
    return [self p_buildAttributedString];
}

#pragma mark - private

- (NSMutableAttributedString *)p_buildAttributedString {
    NSArray *spans = [KRConvertUtil hr_arrayWithJSONString:_props[@"values"]];
    if (!spans.count) {
        spans = @[_props ? : @{}];
    }
    _spans = spans;
    _pendingGradients = [NSMutableArray new];
    NSString *textPostProcessor = nil;
    NSMutableArray *richAttrArray = [NSMutableArray new];
    UIFont *mainFont = nil;
    for (NSMutableDictionary * span in spans) {
        if (span[@"placeholderWidth"]) { // 属于占位span
            NSAttributedString *placeholderSpanAttributedString = [self p_createPlaceholderSpanAttributedStringWithSpan:span];
            [richAttrArray addObject:placeholderSpanAttributedString];
            continue;
        }

        NSString *text = span[@"value"] ?: span[@"text"];
        if (!text.length) {
            continue;
        }
        NSMutableDictionary *propStyle = [(_props ? : @{}) mutableCopy];
        [propStyle addEntriesFromDictionary:span];

        // 批量解析与字体相关的属性
        UIFont *font = [KRConvertUtil UIFont:propStyle];
        UIColor * color = [UIView css_color:propStyle[@"color"]] ?: [UIColor blackColor];
        NSString *cssGricent = propStyle[@"backgroundImage"];
        BOOL hasGradient = NO;
        if (cssGricent && [cssGricent hasPrefix:@"linear-gradient("]) {
            hasGradient = YES;
        }

        CGFloat letterSpacing = [KRConvertUtil CGFloat:propStyle[@"letterSpacing"]];
        KRTextDecorationLineType textDecoration = [KRConvertUtil KRTextDecorationLineType:propStyle[@"textDecoration"]];
        NSTextAlignment textAlign = [KRConvertUtil NSTextAlignment:propStyle[@"textAlign"]];
        NSNumber *lineHeight = nil;
        NSNumber *lineSpacing = nil;
        NSNumber *paragraphSpacing = propStyle[@"paragraphSpacing"] ? @([KRConvertUtil CGFloat:propStyle[@"paragraphSpacing"]]) : nil;
        if (propStyle[@"lineHeight"]) {
            lineHeight = @([KRConvertUtil CGFloat:propStyle[@"lineHeight"]]);
        } else {
            lineSpacing = @([KRConvertUtil CGFloat:propStyle[@"lineSpacing"]]);
        }
        CGFloat headIndent = [KRConvertUtil CGFloat:propStyle[@"headIndent"]];
        UIColor *strokeColor = [UIView css_color:propStyle[@"strokeColor"]];
        CGFloat strokeWidth = [KRConvertUtil CGFloat:propStyle[@"strokeWidth"]];
        NSInteger spanIndex = [spans indexOfObject:span];

        NSShadow *textShadow = nil;
        NSString *cssTextShadow = propStyle[@"textShadow"];
        if ([cssTextShadow isKindOfClass:[NSString class]] && cssTextShadow.length > 0) {
            CSSBoxShadow *shadow = [[CSSBoxShadow alloc] initWithCSSBoxShadow:cssTextShadow];

            textShadow = [NSShadow new];
            textShadow.shadowColor = shadow.shadowColor;
            textShadow.shadowOffset = CGSizeMake(shadow.offsetX, shadow.offsetY);
            textShadow.shadowBlurRadius = shadow.shadowRadius;
        }
        if (propStyle[@"textPostProcessor"]) {
            textPostProcessor = propStyle[@"textPostProcessor"];
        }

        if (!mainFont) {
            mainFont = font;
        }
        if ([textPostProcessor isKindOfClass:[NSString class]] && textPostProcessor.length) {
            // 代理
            if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(kr_customTextWithText:textPostProcessor:)]) {
                text = [[KuiklyRenderBridge componentExpandHandler] kr_customTextWithText:text textPostProcessor:textPostProcessor];
            }
        }

        // 创建 Span 属性对象
        KRSpanAttributes *spanAttrs = [[KRSpanAttributes alloc] init];
        spanAttrs.text = text;
        spanAttrs.spanIndex = spanIndex;
        spanAttrs.font = font;
        spanAttrs.color = color;
        spanAttrs.hasGradient = hasGradient;
        spanAttrs.cssGradient = cssGricent;
        spanAttrs.letterSpacing = letterSpacing;
        spanAttrs.textDecoration = textDecoration;
        spanAttrs.textAlign = textAlign;
        spanAttrs.lineSpacing = lineSpacing;
        spanAttrs.lineHeight = lineHeight;
        spanAttrs.paragraphSpacing = paragraphSpacing;
        spanAttrs.headIndent = headIndent;
        spanAttrs.strokeColor = strokeColor;
        spanAttrs.strokeWidth = strokeWidth;
        spanAttrs.shadow = textShadow;
        spanAttrs.richAttrArray = richAttrArray;
        // 组合属性，生成这段Span对应的富文本
        NSMutableAttributedString *spanAttrString = [self p_createSpanAttributedStringWithAttributes:spanAttrs];
        if (spanAttrString) {
            [richAttrArray addObject:spanAttrString];
        }
    }

    NSMutableAttributedString *resAttr = [[NSMutableAttributedString alloc] init];
    for (NSAttributedString *attr in richAttrArray) {
        [resAttr appendAttributedString:attr];
    }
    if ([textPostProcessor isKindOfClass:[NSString class]] && textPostProcessor.length) {
        // 代理
        if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(kr_customTextWithAttributedString:font:textPostProcessor:)]) {
            resAttr = [[KuiklyRenderBridge componentExpandHandler] kr_customTextWithAttributedString:resAttr font:mainFont textPostProcessor:textPostProcessor];
        }
    }
    if ([textPostProcessor isKindOfClass:[NSString class]] && textPostProcessor.length) {
        // 代理
        if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_customTextWithAttributedString:textPostProcessor:)]) {
            resAttr = [[KuiklyRenderBridge componentExpandHandler] hr_customTextWithAttributedString:resAttr textPostProcessor:textPostProcessor];
        }
    }
    return resAttr;
}


- (nullable NSMutableAttributedString *)p_createSpanAttributedStringWithAttributes:(KRSpanAttributes *)attrs {
    NSMutableAttributedString *attributedString = [[NSMutableAttributedString alloc] initWithString:attrs.text attributes:@{}];
    NSRange range = NSMakeRange(0, attributedString.length);

    // 设置字体
    if (attrs.font) {
        [attributedString addAttribute:NSFontAttributeName value:attrs.font range:range];
    }
    
    // 渐变色：先用纯色占位，记录渐变信息到 _pendingGradients，布局完成后统一应用
    if (attrs.hasGradient && attrs.cssGradient && attrs.font) {
        [attributedString addAttribute:NSForegroundColorAttributeName value:attrs.color range:range];

        NSUInteger currentLength = 0;
        for (NSAttributedString *attr in attrs.richAttrArray) {
            currentLength += attr.length;
        }

        NSDictionary *gradientInfo = @{
            kGradientInfoKeyCSSGradient: attrs.cssGradient,
            kGradientInfoKeyFont: attrs.font,
            kGradientInfoKeyGlobalRange: [NSValue valueWithRange:NSMakeRange(currentLength, attrs.text.length)]
        };
        [_pendingGradients addObject:gradientInfo];
    } else {
        [attributedString addAttribute:NSForegroundColorAttributeName value:attrs.color range:range];
    }

    // 强制使用LTR文本方向
    [attributedString addAttribute:NSWritingDirectionAttributeName value:@[@(NSWritingDirectionLeftToRight | NSWritingDirectionOverride)] range:range];

    if (attrs.letterSpacing) {
        [attributedString addAttribute:NSKernAttributeName value:@(attrs.letterSpacing) range:range];
    }

    if (attrs.textDecoration == KRTextDecorationLineTypeUnderline) {
        [attributedString addAttribute:NSUnderlineStyleAttributeName value:@(NSUnderlineStyleSingle) range:range];
    }
    if (attrs.textDecoration == KRTextDecorationLineTypeStrikethrough) {
        [attributedString addAttribute:NSStrikethroughStyleAttributeName value:@(NSUnderlineStyleSingle) range:range];
    }

    [self p_applyTextAttributeWithAttr:attributedString
                            textAliment:attrs.textAlign
                           lineSpacing:attrs.lineSpacing
                      paragraphSpacing:attrs.paragraphSpacing
                            lineHeight:attrs.lineHeight
                                 range:range
                              fontSize:attrs.font.pointSize
                            headIndent:attrs.headIndent
                                  font:attrs.font];

    if (attrs.strokeColor) {
        [attributedString addAttribute:NSStrokeColorAttributeName value:attrs.strokeColor range:range];
        NSNumber *width = _strokeAndFill ? @(-attrs.strokeWidth) : @(attrs.strokeWidth);
        [attributedString addAttribute:NSStrokeWidthAttributeName value:width range:range];
    }

    [attributedString addAttribute:KuiklyIndexAttributeName value:@(attrs.spanIndex) range:range];

    if (attrs.shadow) {
        [attributedString addAttribute:NSShadowAttributeName value:attrs.shadow range:range];
    }

    return attributedString;
}

- (NSAttributedString *)p_createPlaceholderSpanAttributedStringWithSpan:(NSMutableDictionary *)span {
    KRRichTextAttachment *attachment = [[KRRichTextAttachment alloc] init];
    CGFloat height = [span[@"placeholderHeight"] doubleValue];
    CGFloat width = [span[@"placeholderWidth"] doubleValue];
    NSMutableDictionary *propStyle = [(_props ? : @{}) mutableCopy];
    [propStyle addEntriesFromDictionary:span];
    if (!propStyle[@"fontSize"]) {
        for (NSDictionary * inSpan in _spans) {
            if (inSpan[@"fontSize"]) {
                [propStyle addEntriesFromDictionary:inSpan];
                break;
            }
        }
    }
    UIFont *font = [KRConvertUtil UIFont:propStyle];

    CGFloat lineHeight = [KRConvertUtil CGFloat:propStyle[@"lineHeight"]];
    if (lineHeight > 0) {
        attachment.offsetY = - font.descender;
    } else {
        attachment.offsetY = ( height - font.capHeight ) / 2.0;
    }

    attachment.bounds = CGRectMake(0, -attachment.offsetY, width, height);
    if ([span isKindOfClass:[NSMutableDictionary class]]) {
        ((NSMutableDictionary *)span)[@"attachment"] = attachment;
    }

    NSAttributedString *attrString = [NSAttributedString attributedStringWithAttachment:attachment];
    NSMutableAttributedString *mutableAttrString = [[NSMutableAttributedString alloc] initWithAttributedString:attrString];
    [mutableAttrString kr_addAttribute:NSWritingDirectionAttributeName value:@[@(NSWritingDirectionLeftToRight | NSWritingDirectionOverride)] range:NSMakeRange(0, mutableAttrString.length)];
    return mutableAttrString;
}


- (void)p_applyTextAttributeWithAttr:(NSMutableAttributedString *)attributedString
                         textAliment:(NSTextAlignment)textAliment
                         lineSpacing:(NSNumber *)lineSpacing
                    paragraphSpacing: (NSNumber *)paragraphSpacing
                          lineHeight:(NSNumber *)lineHeight
                               range:(NSRange)range
                            fontSize:(CGFloat)fontSize
                          headIndent:(CGFloat)headIndent
                                font:(UIFont *)font {
    NSMutableParagraphStyle *style  = [[NSMutableParagraphStyle alloc] init];
    style.alignment = textAliment;
    // 强制使用LTR文本方向，确保文本始终从左到右显示
    style.baseWritingDirection = NSWritingDirectionLeftToRight;
    if (lineSpacing) {
         style.lineSpacing = ceil([lineSpacing floatValue]) ;
    }
    if (lineHeight) {
        style.maximumLineHeight = [lineHeight floatValue];
        style.minimumLineHeight = [lineHeight floatValue];
        CGFloat baselineOffset = ([lineHeight floatValue]  - font.pointSize) / 2;
        [attributedString addAttribute:NSBaselineOffsetAttributeName value:@(baselineOffset) range:range];
    }
    if (paragraphSpacing) {
        style.paragraphSpacing = ceil([paragraphSpacing floatValue]) ;
    }
    if (headIndent) {
        style.firstLineHeadIndent = headIndent;
    }
    [attributedString addAttribute:NSParagraphStyleAttributeName value:style range:range];
}

#pragma mark css - method
/*
 * 返回span所在的文本排版坐标
 */
- (NSString *)css_spanRectWithParams:(NSString *)params {
    if (!_mAttributedString) { // 文本还未排版，调用无效
        return @"";
    }
    NSInteger spanIndex = [params intValue];
    if (spanIndex < _spans.count ) {
        KRRichTextAttachment *attachment = _spans[spanIndex][@"attachment"];

        // 检查attachment是否在可见范围内
        NSInteger numberOfLines = [KRConvertUtil NSInteger:_props[@"numberOfLines"]];
        NSLayoutManager *layoutManager = _mAttributedString.hr_textRender.layoutManager;
        NSTextContainer *textContainer = _mAttributedString.hr_textRender.textContainer;

        if (numberOfLines > 0 && layoutManager && textContainer) {
            // 获取attachment对应的字形索引
            NSUInteger glyphIndex = [layoutManager glyphIndexForCharacterAtIndex:attachment.charIndex];
            // 获取截断的字形范围
            NSRange truncatedGlyphRange = [layoutManager truncatedGlyphRangeInLineFragmentForGlyphAtIndex:glyphIndex];

            // 如果有截断
            if (truncatedGlyphRange.location != NSNotFound && truncatedGlyphRange.length > 0) {
                // 判断attachment是否在截断范围内
                if (glyphIndex >= truncatedGlyphRange.location) {
                    return @"";
                }
            }
        }

        CGRect frame = [_mAttributedString.hr_textRender boundingRectForCharacterRange:NSMakeRange(attachment.charIndex, 1)];
        CGFloat offsetY = (CGRectGetHeight(frame) - attachment.bounds.size.height) / 2.0;
        return [NSString stringWithFormat:@"%.2lf %.2lf %.2lf %.2lf", CGRectGetMinX(frame), CGRectGetMinY(frame) + offsetY, attachment.bounds.size.width , attachment.bounds.size.height];
    }
    return @"";

}

- (NSString *)isLineBreakMargin {
    return _mAttributedString.hr_textRender.isBreakLine ? @"1" : @"0";
}


- (void)dealloc {

}

@end




@implementation KRRichTextAttachment


- (UIImage *)imageForBounds:(CGRect)imageBounds textContainer:(NSTextContainer *)textContainer characterIndex:(NSUInteger)charIndex {
    return nil;
}



- (CGRect)attachmentBoundsForTextContainer:(NSTextContainer *)textContainer proposedLineFragment:(CGRect)lineFrag glyphPosition:(CGPoint)position characterIndex:(NSUInteger)charIndex {
    _charIndex = charIndex;
    return CGRectMake(0, -self.offsetY, self.bounds.size.width, self.bounds.size.height);
}

@end

// Span属性参数对象实现
@implementation KRSpanAttributes

@end

// 文本渐变色处理类
@implementation TextGradientHandler

/// 将渐变色应用到富文本指定范围，使用总布局尺寸确保多行渐变连续
+ (void)applyGlobalGradientToAttributedString:(NSMutableAttributedString *)attributedString
                                         range:(NSRange)range
                                   cssGradient:(NSString *)cssGradient
                                          font:(UIFont *)font
                                totalLayoutSize:(CGSize)totalLayoutSize {
    CSSGradientInfo *gradientInfo = [self parseGradient:cssGradient];
    if (!gradientInfo) {
        return;
    }

    UIImage *gradientImage = [self createGradientImageWithInfo:gradientInfo size:totalLayoutSize];
    if (!gradientImage) {
        return;
    }

    UIColor *patternColor = [UIColor colorWithPatternImage:gradientImage];
    [attributedString addAttribute:NSForegroundColorAttributeName
                             value:patternColor
                             range:range];
}


/// 解析 CSS 渐变字符串，格式：linear-gradient(180deg, #FF0000 0%, #0000FF 100%)
+ (CSSGradientInfo *)parseGradient:(NSString *)cssGradient {
    NSString *lineargradientPrefix = @"linear-gradient(";
    if (![cssGradient hasPrefix:lineargradientPrefix] || cssGradient.length <= lineargradientPrefix.length) {
        return nil;
    }
    NSString *content = [cssGradient substringWithRange:NSMakeRange(lineargradientPrefix.length, cssGradient.length - lineargradientPrefix.length - 1)];
    NSArray<NSString *>* splits = [content componentsSeparatedByString:@","];
    
    if (splits.count < 3) {
        return nil;
    }

    CSSGradientInfo *info = [CSSGradientInfo new];
    info.direction = [splits.firstObject intValue];
    info.colors = [NSMutableArray array];
    info.locations = [NSMutableArray array];

    for (int i = 1; i < splits.count; i++) {
        NSString *colorStopStr = splits[i];
        NSArray<NSString *> *colorAndStop = [colorStopStr componentsSeparatedByString:@" "];
        UIColor *color = [UIView css_color:colorAndStop.firstObject];
        if (!color) {
            continue;
        }
        [info.colors addObject:color];
        CGFloat location = [colorAndStop.lastObject doubleValue];
        [info.locations addObject:@(location)];
    }
    
    if (info.colors.count < 2) {
        return nil;
    }

    return info;
}

// 根据渐变信息创建渐变图片
+ (UIImage *)createGradientImageWithInfo:(CSSGradientInfo *)info size:(CGSize)size {
    if (size.width <= 0 || size.height <= 0) {
        return nil;
    }
    
    // 在 Block 外部准备所有数据，避免 Block 内部访问 UIKit 对象导致的线程问题
    
    NSMutableArray *cgColors = [NSMutableArray arrayWithCapacity:info.colors.count];
    for (UIColor *color in info.colors) {
        [cgColors addObject:(__bridge id)(color.CGColor)];
    }
    
    NSUInteger locationsCount = info.locations.count;
    CGFloat *locations = (CGFloat *)malloc(sizeof(CGFloat) * locationsCount);
    if (!locations) {
        return nil;
    }
    for (NSUInteger i = 0; i < locationsCount; i++) {
        locations[i] = [info.locations[i] floatValue];
    }
    
    // 通过 CAGradientLayer 计算渐变方向对应的起点和终点
    CAGradientLayer *tempLayer = [CAGradientLayer layer];
    tempLayer.bounds = CGRectMake(0, 0, size.width, size.height);
    [KRConvertUtil hr_setStartPointAndEndPointWithLayer:tempLayer direction:info.direction];
    CGPoint startPoint = CGPointMake(tempLayer.startPoint.x * size.width,
                                     tempLayer.startPoint.y * size.height);
    CGPoint endPoint = CGPointMake(tempLayer.endPoint.x * size.width,
                                   tempLayer.endPoint.y * size.height);
    
#if TARGET_OS_OSX // [macOS
    KRUIGraphicsImageRenderer *renderer = [[KRUIGraphicsImageRenderer alloc] initWithSize:size];
    UIImage *image = [renderer imageWithActions:^(KRUIGraphicsImageRendererContext *rendererContext) {
        CGContextRef context = [rendererContext CGContext];
        CGContextTranslateCTM(context, 0, size.height);
        CGContextScaleCTM(context, 1.0, -1.0);
#else
    UIGraphicsImageRendererFormat *format = [[UIGraphicsImageRendererFormat alloc] init];
    format.scale = [UIScreen mainScreen].scale;
    format.opaque = NO;
    
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size format:format];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext *rendererContext) {
        CGContextRef context = rendererContext.CGContext;
#endif // macOS]
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        CGGradientRef gradient = CGGradientCreateWithColors(colorSpace, (__bridge CFArrayRef)cgColors, locations);
        CGGradientDrawingOptions options = kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation;
        CGContextDrawLinearGradient(context, gradient, startPoint, endPoint, options);
        CGGradientRelease(gradient);
        CGColorSpaceRelease(colorSpace);
    }];

    free(locations);
    return image;
}



@end


@implementation CSSGradientInfo

@end
