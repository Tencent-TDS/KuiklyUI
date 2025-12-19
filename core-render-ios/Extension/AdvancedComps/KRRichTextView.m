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
    NSMutableDictionary<NSString *, id> *_props; // context thread used
    NSArray<NSDictionary *> * _spans; // context thread used
    NSMutableAttributedString *_mAttributedString; // context thread used
    NSMutableArray<NSDictionary *> *_pendingGradients; // 待应用的Richtext渐变信息
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

    // 全局渐变延迟应用机制：
    // 渐变色需要基于完整的文本布局尺寸绘制，必须在 sizeThatFits 完成后才能获取准确的总尺寸
    if (_pendingGradients.count > 0) {
        // 获取整个富文本的总布局尺寸（包含所有行）
        CGSize totalLayoutSize = _mAttributedString.hr_textRender.size;
        // 异常检查
        if (totalLayoutSize.width <= 0 || totalLayoutSize.height <= 0) {
            totalLayoutSize = fitSize;
        }

        // 遍历所有待应用的渐变 span
        for (NSDictionary *gradientInfo in _pendingGradients) {
            NSString *cssGradient = gradientInfo[@"cssGradient"];
            UIFont *font = gradientInfo[@"font"];
            NSRange globalRange = [gradientInfo[@"globalRange"] rangeValue];

            // 应用全局渐变：使用总布局尺寸创建渐变图片，确保多行文本渐变连续
            [TextGradientHandler applyGlobalGradientToAttributedString:_mAttributedString
                                                                 range:globalRange
                                                           cssGradient:cssGradient
                                                                  font:font
                                                        totalLayoutSize:totalLayoutSize];
        }

        // 重建 TextRender：原 textStorage 是副本，需用渐变修改后的内容重建
        // PatternColor 只改颜色不影响字形尺寸，fitSize 应保持不变
        NSTextStorage *textStorage = [[NSTextStorage alloc] initWithAttributedString:_mAttributedString];
        textStorage.hr_hasAttachmentViews = _mAttributedString.hr_hasAttachmentViews;
        KRTextRender *textRender = [[KRTextRender alloc] initWithTextStorage:textStorage lineHeight:lineHeight];
        textRender.lineBreakMargin = lineBreakMargin;
        textRender.maximumNumberOfLines = numberOfLines;
        textRender.lineBreakMode = lineBreakMode;
        textRender.size = fitSize;
        // 此次布局仅为构建 LayoutManager 字形位置，供 characterIndexForPoint 等方法使用
        CGSize newFitSize = [textRender textSizeWithRenderWidth:constraintSize.width];
        
        // 防御性检查：若尺寸变化说明渐变图片尺寸不匹配，回退为黑色
        if (!CGSizeEqualToSize(fitSize, newFitSize)) {
            for (NSDictionary *gradientInfo in _pendingGradients) {
                NSRange range = [gradientInfo[@"globalRange"] rangeValue];
                [_mAttributedString addAttribute:NSForegroundColorAttributeName value:[UIColor blackColor] range:range];
            }
        }
        
        if (lineBreakMargin > 0 && numberOfLines) {
            textRender.maximumNumberOfLines = 0;
            CGSize newSize = [textRender textSizeWithRenderWidth:constraintSize.width];
            textRender.isBreakLine = !CGSizeEqualToSize(fitSize, newSize);        // 始终使用旧的fitsize，因为渐变色应用后尺寸发生变化，也是回退至使用纯色的时候
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
    // 初始化待处理的渐变列表（用于延迟应用）
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
        // 解析颜色：支持渐变色（backgroundImage）和纯色（color）
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
    [attributedString addAttribute:NSFontAttributeName value:attrs.font ?: [NSNull null] range:range];

    // 渐变色延迟应用机制：
    // 1. 检测到渐变时，先使用临时颜色占位，避免黑色文字闪现
    // 2. 记录渐变信息到 _pendingGradients，包含各段文字所在的位置（globalRange）
    // 3. 等待布局完成后，在 hrv_calculateRenderViewSizeWithConstraintSize 中统一应用
    if (attrs.hasGradient && attrs.cssGradient) {
        [attributedString addAttribute:NSForegroundColorAttributeName value:attrs.color range:range];

        // 计算当前 span 在整个富文本中的全局起始位置
        NSUInteger currentLength = 0;
        for (NSAttributedString *attr in attrs.richAttrArray) {
            currentLength += attr.length;
        }

        // 记录渐变信息：CSS 字符串、字体、全局范围
        NSDictionary *gradientInfo = @{
            @"cssGradient": attrs.cssGradient,
            @"font": attrs.font,
            @"globalRange": [NSValue valueWithRange:NSMakeRange(currentLength, attrs.text.length)]
        };
        [_pendingGradients addObject:gradientInfo];
    } else {
        // 应用普通颜色
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

// 文本渐变色绘制实现类
@implementation TextGradientHandler

// 全局渐变实现：用于多行文本的连续渐变效果
// 渐变范围基于整个文本布局的总尺寸，确保跨行渐变的连续性
+ (void)applyGlobalGradientToAttributedString:(NSMutableAttributedString *)attributedString
                                         range:(NSRange)range
                                   cssGradient:(NSString *)cssGradient
                                          font:(UIFont *)font
                                totalLayoutSize:(CGSize)totalLayoutSize {
    CSSGradientInfo *gradientInfo = [self parseGradient:cssGradient];
    if (!gradientInfo) {
        return;
    }

    // 使用整个布局的总尺寸创建渐变图片
    UIImage *gradientImage = [self createGradientImageWithInfo:gradientInfo size:totalLayoutSize];

    if (!gradientImage) {
        return;
    }

    // 将渐变图片设置为文字的填充颜色（PatternColor）
    UIColor *patternColor = [UIColor colorWithPatternImage:gradientImage];
    [attributedString addAttribute:NSForegroundColorAttributeName
                             value:patternColor
                             range:range];
}


// 解析 CSS 渐变字符串，提取方向、颜色、位置信息
+ (CSSGradientInfo *)parseGradient:(NSString *)cssGradient {
    NSString *lineargradientPrefix = @"linear-gradient(";
    if (![cssGradient hasPrefix:lineargradientPrefix]) {
        return nil;
    }
    // 解析格式：linear-gradient(180deg, #FF0000 0%, #0000FF 100%)
    NSString *content = [cssGradient substringWithRange:NSMakeRange(lineargradientPrefix.length, cssGradient.length - lineargradientPrefix.length - 1)];
    NSArray<NSString *>* splits = [content componentsSeparatedByString:@","];


    CSSGradientInfo *info = [CSSGradientInfo new];
    info.direction = [splits.firstObject intValue];
    info.colors = [NSMutableArray array];
    info.locations = [NSMutableArray array];

    for (int i = 1; i < splits.count; i++) {
        NSString *colorStopStr = splits[i];
        NSArray<NSString *> *colorAndStop = [colorStopStr componentsSeparatedByString:@" "];
        UIColor *color = [UIView css_color:colorAndStop.firstObject];
        if (!color) {
            color = [UIColor blackColor];
        }
        [info.colors addObject:color];
        CGFloat location = [colorAndStop.lastObject doubleValue];
        [info.locations addObject:@(location)];

    }

    return info;
}

// 创建渐变图片
+ (UIImage *)createGradientImageWithInfo:(CSSGradientInfo *)info size:(CGSize)size {
    if (size.width <= 0 || size.height <= 0) {
        return nil;
    }
    
#if TARGET_OS_OSX // [macOS
    KRUIGraphicsImageRenderer *renderer = [[KRUIGraphicsImageRenderer alloc] initWithSize:size];
    UIImage *image = [renderer imageWithActions:^(KRUIGraphicsImageRendererContext *rendererContext) {
        CGContextRef context = [rendererContext CGContext];
#else
    UIGraphicsImageRendererFormat *format = [[UIGraphicsImageRendererFormat alloc] init];
    format.scale = [UIScreen mainScreen].scale;
    format.opaque = NO;

    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size format:format];

    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext *rendererContext) {
        CGContextRef context = rendererContext.CGContext;
#endif // macOS]
        // 转换颜色为 CGColor
        NSMutableArray *cgColors = [NSMutableArray array];
        CGFloat locations[info.locations.count];

        for (int i = 0; i < info.locations.count; i++) {
            [cgColors addObject:(__bridge id)(info.colors[i].CGColor)];
            locations[i] = [info.locations[i] floatValue];
        }
        // 创建线性渐变
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        CGGradientRef gradient = CGGradientCreateWithColors(colorSpace, (__bridge CFArrayRef)cgColors, locations);

        // 根据方向计算起点和终点
        CAGradientLayer *gradientLayer = [CAGradientLayer layer];
        gradientLayer.bounds = CGRectMake(0, 0, size.width, size.height);

        [KRConvertUtil hr_setStartPointAndEndPointWithLayer:gradientLayer direction:info.direction];
        CGPoint startPoint = CGPointMake(gradientLayer.startPoint.x * size.width,
                                        gradientLayer.startPoint.y * size.height);
        CGPoint endPoint = CGPointMake(gradientLayer.endPoint.x * size.width,
                                      gradientLayer.endPoint.y * size.height);

        // 绘制渐变到上下文
        CGContextDrawLinearGradient(context, gradient, startPoint, endPoint, 0);

        CGGradientRelease(gradient);
        CGColorSpaceRelease(colorSpace);
    }];

    return image;
}


@end


@implementation CSSGradientInfo

@end
